package com.example.server

import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.util.Log
import com.example.features.camera.CameraStreamManager
import com.example.features.lyrics.LyricsStylePreset
import com.example.model.*
import com.example.presentation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class PresentationWebServer(private val presentationServer: PresentationServer) {
    private var serverSocket: ServerSocket? = null
    private var wifiLock: WifiManager.WifiLock? = null
    @Volatile private var isRunning = false
    
    private val wsClients = ConcurrentHashMap<Socket, OutputStream>()
    
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("PresentationWebServer", "Caught coroutine exception in PresentationWebServer", throwable)
    }
    
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)
    
    var activePort: Int = 8080
        private set

    fun start(context: Context? = null) {
        if (isRunning) return
        
        if (context != null) {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                wifiLock = wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "PresentationWebServer:WifiLock")?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
            } catch (e: Throwable) {
                Log.e("PresentationWebServer", "Failed to acquire WifiLock", e)
            }
        }

        if (scope.coroutineContext[Job]?.isActive != true) {
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)
        }

        scope.launch {
            startServerLoop(activePort)
        }

        // Start presentation state observer to broadcast updates to all connected WebSockets
        scope.launch {
            presentationServer.state.collectLatest {
                broadcastCurrentState()
            }
        }
    }

    fun restartOnPort(port: Int) {
        scope.launch {
            try {
                stopInternal()
                delay(300)
                activePort = port
                startServerLoop(port)
            } catch (e: Throwable) {
                Log.e("PresentationWebServer", "Error restarting server on port $port", e)
            }
        }
    }

    private fun startServerLoop(port: Int) {
        try {
            activePort = port
            val ss = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0")).apply {
                reuseAddress = true
            }
            serverSocket = ss
            isRunning = true
            Log.i("PresentationWebServer", "Presentation Web Server successfully listening on 0.0.0.0:$port")

            while (isRunning && !ss.isClosed) {
                try {
                    val clientSocket = ss.accept()
                    clientSocket.soTimeout = 0 // Keep alive for WebSocket / MJPEG
                    clientSocket.tcpNoDelay = true
                    
                    scope.launch {
                        handleClient(clientSocket)
                    }
                } catch (e: Throwable) {
                    if (!isRunning || ss.isClosed) break
                    Log.w("PresentationWebServer", "Error accepting client connection: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            Log.e("PresentationWebServer", "Error starting ServerSocket on port $port", e)
            if (port == 8080 && isRunning) {
                Log.i("PresentationWebServer", "Retrying on fallback port 8081...")
                startServerLoop(8081)
            }
        }
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            val input = BufferedInputStream(socket.getInputStream())
            val output = BufferedOutputStream(socket.getOutputStream())

            // Read HTTP request line and headers
            val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
            val requestLine = reader.readLine() ?: run {
                socket.close()
                return@withContext
            }

            val headers = mutableMapOf<String, String>()
            var line: String? = reader.readLine()
            while (!line.isNullOrEmpty()) {
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    val key = line.substring(0, colonIdx).trim().lowercase()
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
                line = reader.readLine()
            }

            val parts = requestLine.split(" ")
            val method = if (parts.isNotEmpty()) parts[0].uppercase() else "GET"
            val fullUri = if (parts.size > 1) parts[1] else "/"

            if (method == "OPTIONS") {
                sendCorsOptionsResponse(output)
                socket.close()
                return@withContext
            }

            val questionIdx = fullUri.indexOf('?')
            val path = if (questionIdx >= 0) fullUri.substring(0, questionIdx) else fullUri
            val query = if (questionIdx >= 0) fullUri.substring(questionIdx + 1) else ""

            val isUpgrade = headers["upgrade"]?.equals("websocket", ignoreCase = true) == true
            val isWebSocket = path.startsWith("/ws") || isUpgrade

            if (isWebSocket && isUpgrade) {
                val clientKey = headers["sec-websocket-key"]
                if (clientKey != null) {
                    val handshakeSuccess = handleWebSocketHandshake(clientKey, output)
                    if (handshakeSuccess) {
                        wsClients[socket] = output
                        sendCurrentStateToClient(socket, output)
                        handleWebSocketFrames(socket, input)
                    }
                } else {
                    sendHttpResponse(output, 400, "Bad Request", "Missing Sec-WebSocket-Key".toByteArray(), "text/plain")
                    socket.close()
                }
            } else {
                when {
                    path == "/ping" -> {
                        sendHttpResponse(output, 200, "OK", "OK".toByteArray(), "text/plain")
                        socket.close()
                    }
                    path == "/health" -> {
                        val json = "{\"status\":\"ok\",\"port\":$activePort}"
                        sendHttpResponse(output, 200, "OK", json.toByteArray(), "application/json")
                        socket.close()
                    }
                    path == "/api/state" -> {
                        val json = buildStateJson()
                        sendHttpResponse(output, 200, "OK", json.toByteArray(Charsets.UTF_8), "application/json; charset=UTF-8")
                        socket.close()
                    }
                    path == "/camera/stream" -> {
                        serveCameraStream(socket, output)
                        // Stream keeps running until socket closes
                    }
                    path == "/media" -> {
                        val uriParam = extractQueryParam(query, "uri")
                        if (!uriParam.isNullOrEmpty()) {
                            serveMedia(uriParam, output)
                        } else {
                            sendHttpResponse(output, 400, "Bad Request", "Missing uri parameter".toByteArray(), "text/plain")
                        }
                        socket.close()
                    }
                    path == "/favicon.ico" -> {
                        sendHttpResponse(output, 204, "No Content", ByteArray(0), "image/x-icon")
                        socket.close()
                    }
                    path == "/stage" || path == "/stage-monitor" || path == "/stage-display" -> {
                        val html = getStageMonitorHtml()
                        sendHttpResponse(output, 200, "OK", html.toByteArray(Charsets.UTF_8), "text/html; charset=UTF-8")
                        socket.close()
                    }
                    path == "/obs" || path == "/transparent" -> {
                        val html = getTransparentViewerHtml()
                        sendHttpResponse(output, 200, "OK", html.toByteArray(Charsets.UTF_8), "text/html; charset=UTF-8")
                        socket.close()
                    }
                    else -> {
                        // Serve Live Presentation Screen HTML
                        val html = getPresentationViewerHtml()
                        sendHttpResponse(output, 200, "OK", html.toByteArray(Charsets.UTF_8), "text/html; charset=UTF-8")
                        socket.close()
                    }
                }
            }
        } catch (e: Throwable) {
            Log.d("PresentationWebServer", "Client handled: ${e.message}")
        } finally {
            wsClients.remove(socket)
            try { socket.close() } catch (_: Throwable) {}
        }
    }

    private suspend fun serveCameraStream(socket: Socket, output: OutputStream) = withContext(Dispatchers.IO) {
        try {
            val responseHeader = StringBuilder()
                .append("HTTP/1.1 200 OK\r\n")
                .append("Content-Type: multipart/x-mixed-replace; boundary=frame\r\n")
                .append("Cache-Control: no-cache, no-store, must-revalidate\r\n")
                .append("Pragma: no-cache\r\n")
                .append("Access-Control-Allow-Origin: *\r\n")
                .append("Connection: close\r\n\r\n")
                .toString()

            output.write(responseHeader.toByteArray(Charsets.US_ASCII))
            output.flush()

            while (isRunning && !socket.isClosed) {
                val frame = CameraStreamManager.latestFrame.value
                if (frame != null && frame.isNotEmpty()) {
                    val frameHeader = "--frame\r\n" +
                            "Content-Type: image/jpeg\r\n" +
                            "Content-Length: ${frame.size}\r\n\r\n"
                    output.write(frameHeader.toByteArray(Charsets.US_ASCII))
                    output.write(frame)
                    output.write("\r\n".toByteArray(Charsets.US_ASCII))
                    output.flush()
                }
                delay(40) // ~25 FPS
            }
        } catch (_: Throwable) {
            // Client closed connection
        }
    }

    private fun extractQueryParam(query: String, paramName: String): String? {
        if (query.isEmpty()) return null
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = pair.substring(0, idx)
                if (key == paramName) {
                    val rawValue = pair.substring(idx + 1)
                    return try {
                        URLDecoder.decode(rawValue, "UTF-8")
                    } catch (_: Throwable) {
                        rawValue
                    }
                }
            }
        }
        return null
    }

    private fun serveMedia(rawUri: String, output: OutputStream) {
        try {
            val uri = Uri.parse(rawUri)
            val scheme = uri.scheme
            val context = presentationServer.context
            
            val inputStream: InputStream? = when {
                scheme == "content" -> context?.contentResolver?.openInputStream(uri)
                scheme == "file" -> {
                    val path = uri.path
                    if (path != null) FileInputStream(File(path)) else null
                }
                rawUri.startsWith("/") -> FileInputStream(File(rawUri))
                else -> context?.contentResolver?.openInputStream(uri)
            }

            if (inputStream == null) {
                sendHttpResponse(output, 404, "Not Found", "Media stream not found".toByteArray(), "text/plain")
                return
            }

            val mimeType = getMimeType(rawUri, uri, context)
            
            inputStream.use { input ->
                val bytes = input.readBytes()
                sendHttpResponse(output, 200, "OK", bytes, mimeType)
            }
        } catch (e: Throwable) {
            Log.e("PresentationWebServer", "Error serving media: $rawUri", e)
            sendHttpResponse(output, 500, "Internal Server Error", "Error reading media".toByteArray(), "text/plain")
        }
    }

    private fun getMimeType(rawUri: String, uri: Uri, context: Context?): String {
        val crType = try { context?.contentResolver?.getType(uri) } catch (_: Throwable) { null }
        if (!crType.isNullOrEmpty()) return crType

        val lower = rawUri.lowercase()
        return when {
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".mp4") -> "video/mp4"
            lower.endsWith(".webm") -> "video/webm"
            lower.endsWith(".mov") -> "video/quicktime"
            lower.endsWith(".mkv") -> "video/x-matroska"
            lower.endsWith(".mp3") -> "audio/mpeg"
            lower.endsWith(".wav") -> "audio/wav"
            else -> "image/png"
        }
    }

    private fun handleWebSocketHandshake(clientKey: String, output: OutputStream): Boolean {
        return try {
            val guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
            val sha1 = MessageDigest.getInstance("SHA-1").digest((clientKey.trim() + guid).toByteArray(Charsets.UTF_8))
            val acceptKey = android.util.Base64.encodeToString(sha1, android.util.Base64.NO_WRAP)
            val response = "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: $acceptKey\r\n\r\n"
            output.write(response.toByteArray(Charsets.US_ASCII))
            output.flush()
            true
        } catch (e: Throwable) {
            Log.e("PresentationWebServer", "WebSocket Handshake error", e)
            false
        }
    }

    private fun handleWebSocketFrames(socket: Socket, input: InputStream) {
        try {
            while (isRunning && !socket.isClosed) {
                val b0 = input.read()
                if (b0 == -1) break
                
                val opcode = b0 and 0x0F
                val b1 = input.read()
                if (b1 == -1) break
                
                val isMasked = (b1 and 0x80) != 0
                var payloadLen = (b1 and 0x7F).toLong()
                
                if (payloadLen == 126L) {
                    val b2 = input.read()
                    val b3 = input.read()
                    if (b2 == -1 || b3 == -1) break
                    payloadLen = ((b2 shl 8) or b3).toLong()
                } else if (payloadLen == 127L) {
                    var len = 0L
                    for (i in 0 until 8) {
                        val b = input.read()
                        if (b == -1) return
                        len = (len shl 8) or (b.toLong() and 0xFF)
                    }
                    payloadLen = len
                }
                
                val mask = ByteArray(4)
                if (isMasked) {
                    var readMask = 0
                    while (readMask < 4) {
                        val r = input.read(mask, readMask, 4 - readMask)
                        if (r == -1) return
                        readMask += r
                    }
                }
                
                val targetSize = payloadLen.toInt().coerceAtMost(65536)
                val payload = ByteArray(targetSize)
                var readBytes = 0
                while (readBytes < targetSize) {
                    val r = input.read(payload, readBytes, targetSize - readBytes)
                    if (r == -1) return
                    readBytes += r
                }

                if (isMasked) {
                    for (i in 0 until targetSize) {
                        payload[i] = (payload[i].toInt() xor mask[i % 4].toInt()).toByte()
                    }
                }

                if (opcode == 0x8) {
                    break
                } else if (opcode == 0x9) {
                    val out = wsClients[socket] ?: socket.getOutputStream()
                    synchronized(out) {
                        out.write(0x8A)
                        out.write(0)
                        out.flush()
                    }
                } else if (opcode == 0x1) {
                    // WebSocket Text Message received (Remote commands)
                    try {
                        val textMsg = String(payload, Charsets.UTF_8)
                        val cmdJson = JSONObject(textMsg)
                        executeRemoteCommand(cmdJson)
                    } catch (_: Throwable) {}
                }
            }
        } catch (_: Throwable) {}
    }

    private fun executeRemoteCommand(cmdJson: JSONObject) {
        val action = cmdJson.optString("action")
        when (action) {
            "next" -> presentationServer.nextSlide()
            "prev" -> presentationServer.previousSlide()
            "jump" -> {
                val slide = cmdJson.optInt("slide", 0)
                presentationServer.setSlideIndex(slide)
            }
            "black" -> presentationServer.black()
            "clear" -> presentationServer.clear()
            "toggle_video" -> presentationServer.toggleVideoPlayback()
            "go_song" -> {
                val songId = cmdJson.optString("songId")
                val slide = cmdJson.optInt("slide", 0)
                val song = presentationServer.songsLibrary.find { it.id == songId }
                if (song != null) {
                    presentationServer.go(song)
                    presentationServer.setSlideIndex(slide)
                }
            }
            "go_bible" -> {
                val bibleId = cmdJson.optString("bibleId")
                val verse = cmdJson.optInt("verse", 0)
                val bible = presentationServer.sampleBiblePassages.find { it.id == bibleId }
                if (bible != null) {
                    presentationServer.go(bible)
                    presentationServer.setSlideIndex(verse)
                }
            }
            "go_media" -> {
                val mediaId = cmdJson.optString("mediaId")
                val media = presentationServer.mediaLibrary.find { it.id == mediaId }
                if (media != null) {
                    presentationServer.go(media)
                }
            }
            "go_custom" -> {
                val title = cmdJson.optString("title")
                val text = cmdJson.optString("text")
                val type = cmdJson.optString("type", "LYRICS")
                if (text.isNotEmpty()) {
                    presentationServer.goCustomText(title, text, type)
                }
            }
            "set_bg" -> {
                val bgType = cmdJson.optString("bgType")
                val url = cmdJson.optString("url")
                when (bgType.uppercase()) {
                    "NONE" -> {
                        presentationServer.setBackgroundImage(null)
                        presentationServer.setBackgroundVideo(null)
                        presentationServer.setBackgroundCamera(false)
                    }
                    "CAMERA" -> presentationServer.setBackgroundCamera(true)
                    "IP_CAMERA" -> presentationServer.setBackgroundIpCamera(url)
                    "VIDEO" -> presentationServer.setBackgroundVideo(url)
                    "IMAGE" -> presentationServer.setBackgroundImage(url)
                }
            }
            "toggle_split_screen" -> {
                presentationServer.toggleSplitScreen()
            }
            "toggle_lyrics_mode" -> {
                presentationServer.toggleLyricsDisplayMode()
            }
            "set_lyrics_mode" -> {
                val modeStr = cmdJson.optString("mode")
                if (modeStr.equals("PER_BARIS", ignoreCase = true) || modeStr.equals("LINE", ignoreCase = true)) {
                    presentationServer.setLyricsDisplayMode(LyricsDisplayMode.PER_BARIS)
                } else {
                    presentationServer.setLyricsDisplayMode(LyricsDisplayMode.PER_BAIT)
                }
            }
            "start_timer" -> presentationServer.startSermonTimer()
            "pause_timer" -> presentationServer.pauseSermonTimer()
            "toggle_timer" -> presentationServer.toggleSermonTimer()
            "reset_timer" -> presentationServer.resetSermonTimer()
            "set_timer_minutes" -> {
                val mins = cmdJson.optInt("minutes", 30)
                presentationServer.setSermonTimerDuration(mins)
            }
            "add_timer_minutes" -> {
                val mins = cmdJson.optInt("minutes", 5)
                presentationServer.addSermonTimerMinutes(mins)
            }
            "send_stage_alert" -> {
                val msg = cmdJson.optString("message", "")
                presentationServer.sendStageAlert(msg)
            }
            "clear_stage_alert" -> presentationServer.clearStageAlert()
            "set_bg_media" -> {
                val mediaId = cmdJson.optString("mediaId")
                val media = presentationServer.mediaLibrary.find { it.id == mediaId }
                when (media) {
                    is IpCameraContent -> presentationServer.setBackgroundIpCamera(media.streamUrl)
                    is CameraContent -> presentationServer.setBackgroundCamera(true)
                    is ImageContent -> presentationServer.setBackgroundImage(media.uri)
                    is VideoContent -> presentationServer.setBackgroundVideo(media.uri)
                    else -> {}
                }
            }
            "add_droidcam" -> {
                val title = cmdJson.optString("title")
                val ip = cmdJson.optString("ip")
                val port = cmdJson.optString("port", "4747")
                if (ip.isNotEmpty()) {
                    presentationServer.addDroidCamMedia(title, ip, port)
                }
            }
            "add_song" -> {
                val title = cmdJson.optString("title")
                val text = cmdJson.optString("text")
                if (title.isNotEmpty() && text.isNotEmpty()) {
                    val slides = text.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }
                    presentationServer.addCustomSong(title, if (slides.isEmpty()) listOf(text.trim()) else slides)
                }
            }
            "update_style" -> {
                val fontSize = cmdJson.optInt("fontSize", presentationServer.state.value.fontSizeSp)
                val posStr = cmdJson.optString("position")
                val pos = when (posStr.uppercase()) {
                    "LOWER_THIRD" -> TextDisplayPosition.LOWER_THIRD
                    "TOP" -> TextDisplayPosition.TOP_BANNER
                    "BOTTOM" -> TextDisplayPosition.BOTTOM_CENTER
                    "LEFT" -> TextDisplayPosition.LEFT_CENTER
                    else -> TextDisplayPosition.CENTER
                }
                val alignStr = cmdJson.optString("align")
                val align = when (alignStr.uppercase()) {
                    "LEFT" -> TextAlignmentOption.LEFT
                    "RIGHT" -> TextAlignmentOption.RIGHT
                    else -> TextAlignmentOption.CENTER
                }
                val colorHex = cmdJson.optString("color", "#FFFFFF")
                val colorLong = try {
                    android.graphics.Color.parseColor(colorHex).toLong() and 0xFFFFFFFFL
                } catch (_: Throwable) {
                    0xFFFFFFFFL
                }
                val bold = cmdJson.optBoolean("isBold", presentationServer.state.value.isTextBold)
                val shadow = cmdJson.optBoolean("isShadow", presentationServer.state.value.isTextShadowEnabled)
                val alpha = cmdJson.optDouble("alpha", presentationServer.state.value.textBackgroundAlpha.toDouble()).toFloat()
                presentationServer.updateLiveTextSettings(
                    fontSizeSp = fontSize,
                    textPosition = pos,
                    textColorRgb = colorLong,
                    textAlignment = align,
                    textBackgroundAlpha = alpha,
                    isTextBold = bold,
                    isTextShadowEnabled = shadow
                )
            }
            "set_preset" -> {
                val presetStr = cmdJson.optString("preset")
                when (presetStr.uppercase()) {
                    "WORSHIP" -> presentationServer.setStylePreset(LyricsStylePreset.WORSHIP)
                    "PRAISE", "MODERN" -> presentationServer.setStylePreset(LyricsStylePreset.MODERN)
                    "SERMON", "CLASSIC" -> presentationServer.setStylePreset(LyricsStylePreset.CLASSIC)
                    "MINIMALIST", "MINIMAL" -> presentationServer.setStylePreset(LyricsStylePreset.MINIMAL)
                }
            }
        }
        broadcastCurrentState()
    }

    private fun sendHttpResponse(
        output: OutputStream,
        statusCode: Int,
        statusText: String,
        body: ByteArray,
        contentType: String
    ) {
        try {
            val responseHeader = StringBuilder()
                .append("HTTP/1.1 $statusCode $statusText\r\n")
                .append("Content-Type: $contentType\r\n")
                .append("Content-Length: ${body.size}\r\n")
                .append("Access-Control-Allow-Origin: *\r\n")
                .append("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n")
                .append("Access-Control-Allow-Headers: *\r\n")
                .append("Connection: close\r\n\r\n")
                .toString()
            
            output.write(responseHeader.toByteArray(Charsets.US_ASCII))
            if (body.isNotEmpty()) {
                output.write(body)
            }
            output.flush()
        } catch (_: Throwable) {}
    }

    private fun sendCorsOptionsResponse(output: OutputStream) {
        try {
            val header = "HTTP/1.1 204 No Content\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                    "Access-Control-Allow-Headers: *\r\n" +
                    "Content-Length: 0\r\n" +
                    "Connection: close\r\n\r\n"
            output.write(header.toByteArray(Charsets.US_ASCII))
            output.flush()
        } catch (_: Throwable) {}
    }

    private fun broadcastCurrentState() {
        if (wsClients.isEmpty()) return
        val jsonString = buildStateJson()
        val deadSockets = mutableListOf<Socket>()
        
        wsClients.forEach { (socket, output) ->
            try {
                sendWebSocketTextFrame(output, jsonString)
            } catch (e: Throwable) {
                deadSockets.add(socket)
            }
        }
        
        deadSockets.forEach { socket ->
            wsClients.remove(socket)
            try { socket.close() } catch (_: Throwable) {}
        }
    }

    private fun sendCurrentStateToClient(socket: Socket, output: OutputStream) {
        try {
            val jsonString = buildStateJson()
            sendWebSocketTextFrame(output, jsonString)
        } catch (e: Throwable) {
            wsClients.remove(socket)
            try { socket.close() } catch (_: Throwable) {}
        }
    }

    private fun sendWebSocketTextFrame(output: OutputStream, message: String) {
        val payload = message.toByteArray(Charsets.UTF_8)
        val length = payload.size
        
        synchronized(output) {
            output.write(0x81)
            when {
                length <= 125 -> {
                    output.write(length)
                }
                length <= 65535 -> {
                    output.write(126)
                    output.write((length shr 8) and 0xFF)
                    output.write(length and 0xFF)
                }
                else -> {
                    output.write(127)
                    for (i in 7 downTo 0) {
                        output.write(((length.toLong() shr (8 * i)) and 0xFF).toInt())
                    }
                }
            }
            output.write(payload)
            output.flush()
        }
    }

    private fun formatMediaUrl(uriString: String?): String {
        if (uriString.isNullOrEmpty()) return ""
        if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
            return uriString
        }
        return try {
            "/media?uri=" + URLEncoder.encode(uriString, "UTF-8")
        } catch (_: Throwable) {
            "/media?uri=$uriString"
        }
    }

    private fun buildStateJson(): String {
        return try {
            val state = presentationServer.state.value
            val json = JSONObject()
            
            val status = state.status
            json.put("status", status.name)
            
            var contentType = "NONE"
            var text = ""
            var title = ""
            var mediaUrl = ""
            var totalSlides = 0
            var slideIndex = state.currentSlideIndex
            val slidesArray = JSONArray()

            when (val content = state.currentContent) {
                is LyricsContent -> {
                    contentType = "LYRICS"
                    title = content.title
                    val effectiveSlides = content.getEffectiveSlides(state.lyricsDisplayMode)
                    totalSlides = effectiveSlides.size
                    effectiveSlides.forEach { slidesArray.put(it) }
                    if (effectiveSlides.isNotEmpty()) {
                        val index = slideIndex.coerceIn(0, effectiveSlides.size - 1)
                        text = effectiveSlides[index]
                    }
                }
                is BibleContent -> {
                    contentType = "BIBLE"
                    title = content.title
                    totalSlides = content.verses.size
                    content.verses.forEach { slidesArray.put(it) }
                    if (content.verses.isNotEmpty()) {
                        val index = slideIndex.coerceIn(0, content.verses.size - 1)
                        text = content.verses[index]
                    }
                }
                is ImageContent -> {
                    contentType = "IMAGE"
                    title = content.title
                    mediaUrl = formatMediaUrl(content.uri)
                    slidesArray.put(content.title)
                    totalSlides = 1
                }
                is VideoContent -> {
                    contentType = "VIDEO"
                    title = content.title
                    mediaUrl = formatMediaUrl(content.uri)
                    slidesArray.put(content.title)
                    totalSlides = 1
                }
                is PowerPointContent -> {
                    contentType = "POWERPOINT"
                    title = content.title
                    totalSlides = content.slides.size
                    content.slides.forEachIndexed { i, _ -> slidesArray.put("Slide ${i + 1}") }
                    if (content.slides.isNotEmpty()) {
                        val index = slideIndex.coerceIn(0, content.slides.size - 1)
                        mediaUrl = formatMediaUrl(content.slides[index])
                    }
                }
                is IpCameraContent -> {
                    contentType = "IP_CAMERA"
                    title = content.title
                    mediaUrl = content.streamUrl
                    slidesArray.put(content.title)
                    totalSlides = 1
                }
                is CameraContent -> {
                    contentType = "CAMERA"
                    title = content.title
                    mediaUrl = "/camera/stream"
                    slidesArray.put(content.title)
                    totalSlides = 1
                }
                null -> {
                    contentType = "NONE"
                }
            }

            // Next Slide Text Preview (Confidence Monitor)
            var nextText = ""
            if (slideIndex + 1 < totalSlides && slidesArray.length() > slideIndex + 1) {
                nextText = slidesArray.optString(slideIndex + 1, "")
            }

            json.put("contentType", contentType)
            json.put("text", text)
            json.put("nextText", nextText)
            json.put("title", title)
            json.put("slideIndex", slideIndex)
            json.put("totalSlides", totalSlides)
            json.put("slides", slidesArray)
            json.put("mediaUrl", mediaUrl)
            
            // Background Configuration
            json.put("backgroundType", state.backgroundType.name)
            val bgMediaUrl = when (state.backgroundType) {
                BackgroundType.IMAGE -> formatMediaUrl(state.backgroundImageUri)
                BackgroundType.VIDEO -> formatMediaUrl(state.backgroundVideoUri)
                BackgroundType.IP_CAMERA -> state.backgroundVideoUri ?: ""
                BackgroundType.CAMERA -> "/camera/stream"
                else -> ""
            }
            json.put("backgroundMediaUrl", bgMediaUrl)
            json.put("isVideoPlaying", state.isVideoPlaying)

            // Text Styles & Flexible Positioning
            json.put("fontSize", state.fontSizeSp)
            json.put("position", state.textPosition.name)
            
            val hexColor = String.format("#%06X", (0xFFFFFF and state.textColorRgb.toInt()))
            json.put("textColor", hexColor)
            
            val textAlign = when (state.textAlignment.name) {
                "LEFT" -> "left"
                "RIGHT" -> "right"
                else -> "center"
            }
            json.put("textAlign", textAlign)
            
            json.put("bgAlpha", state.textBackgroundAlpha)
            json.put("isBold", state.isTextBold)
            json.put("isShadowEnabled", state.isTextShadowEnabled)
            json.put("textVerticalPercent", state.textVerticalPercent)
            json.put("textHorizontalPercent", state.textHorizontalPercent)
            json.put("textBoxWidthPercent", state.textBoxWidthPercent)
            json.put("textBoxCornerRadiusDp", state.textBoxCornerRadiusDp)
            json.put("textBoxPaddingDp", state.textBoxPaddingDp)
            json.put("textLineHeightMultiplier", state.textLineHeightMultiplier)
            json.put("isTextUppercase", state.isTextUppercase)
            json.put("textBoxBorderEnabled", state.textBoxBorderEnabled)

            // Lyrics Display Mode
            json.put("lyricsDisplayMode", state.lyricsDisplayMode.name)

            // Sermon Timer & Stage Monitor
            json.put("sermonTimerRunning", state.sermonTimerRunning)
            json.put("sermonTimerRemainingSeconds", state.sermonTimerRemainingSeconds)
            json.put("sermonTimerTotalSeconds", state.sermonTimerTotalSeconds)
            json.put("sermonTimerMode", state.sermonTimerMode.name)
            json.put("stageAlertMessage", state.stageAlertMessage ?: "")
            json.put("isStageAlertActive", state.isStageAlertActive)

            // Split Screen Feature
            json.put("isSplitScreenEnabled", state.isSplitScreenEnabled)
            json.put("splitRatioCamPercent", state.splitRatioCamPercent)
            json.put("splitScreenSide", state.splitScreenSide.name)
            val splitCamUrl = when {
                !state.splitCameraStreamUrl.isNullOrBlank() -> state.splitCameraStreamUrl
                state.backgroundType == BackgroundType.IP_CAMERA && !state.backgroundVideoUri.isNullOrBlank() -> state.backgroundVideoUri
                else -> "/camera/stream"
            }
            json.put("splitCameraStreamUrl", splitCamUrl)
            
            json.toString()
        } catch (e: Throwable) {
            Log.e("PresentationWebServer", "Error building state JSON", e)
            "{\"status\":\"IDLE\",\"contentType\":\"NONE\",\"text\":\"\",\"title\":\"\",\"mediaUrl\":\"\"}"
        }
    }

    private fun stopInternal() {
        isRunning = false
        wsClients.forEach { (socket, _) ->
            try { socket.close() } catch (_: Throwable) {}
        }
        wsClients.clear()
        try {
            serverSocket?.close()
        } catch (_: Throwable) {}
        serverSocket = null
    }

    fun stop() {
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
            wifiLock = null
            stopInternal()
            scope.cancel()
        } catch (e: Throwable) {
            Log.e("PresentationWebServer", "Error stopping web server", e)
        }
    }

    private fun getPresentationViewerHtml(): String {
        return """<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
    <title>Church Presentation Live Screen</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        html, body {
            width: 100vw;
            height: 100vh;
            overflow: hidden;
            background-color: #000000;
            color: #ffffff;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            user-select: none;
            -webkit-user-select: none;
        }

        #screen-root {
            position: relative;
            width: 100%;
            height: 100%;
            display: flex;
            overflow: hidden;
            background-color: #000000;
        }

        /* DUAL SPLIT-SCREEN CONTAINER */
        #split-container {
            display: none;
            width: 100%;
            height: 100%;
            flex-direction: row;
            background-color: #05050A;
            position: relative;
            z-index: 5;
        }

        /* Split Screen Panes */
        .split-pane {
            position: relative;
            height: 100%;
            overflow: hidden;
            display: flex;
            justify-content: center;
            align-items: center;
            background-color: #000000;
            transition: width 0.3s ease;
        }

        #split-cam-pane {
            background-color: #080811;
            box-shadow: inset 0 0 20px rgba(0,0,0,0.8);
            position: relative;
        }

        #split-cam-feed {
            width: 100%;
            height: 100%;
            object-fit: cover;
            background-color: #05050A;
        }

        .cam-badge {
            position: absolute;
            top: 16px;
            left: 16px;
            background: rgba(220, 38, 38, 0.85);
            backdrop-filter: blur(8px);
            color: #ffffff;
            font-size: 11px;
            font-weight: 800;
            letter-spacing: 1px;
            padding: 4px 10px;
            border-radius: 20px;
            display: flex;
            align-items: center;
            gap: 6px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.4);
            z-index: 10;
        }

        .cam-badge-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background-color: #ffffff;
            animation: pulse 1.5s infinite;
        }

        @keyframes pulse {
            0% { opacity: 1; transform: scale(1); }
            50% { opacity: 0.4; transform: scale(1.2); }
            100% { opacity: 1; transform: scale(1); }
        }

        /* Split Screen Divider */
        #split-divider {
            width: 3px;
            height: 100%;
            background: linear-gradient(180deg, rgba(208,188,255,0.4) 0%, rgba(56,189,248,0.6) 50%, rgba(208,188,255,0.4) 100%);
            box-shadow: 0 0 10px rgba(56,189,248,0.5);
            z-index: 10;
        }

        #split-content-pane {
            flex: 1;
            position: relative;
        }

        /* NORMAL FULL-SCREEN CONTAINER */
        #single-container {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            display: block;
        }

        /* LAYER 1: Background Layer */
        .bg-layer {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            z-index: 1;
            overflow: hidden;
            background: linear-gradient(135deg, #0F172A 0%, #020617 100%);
            transition: opacity 0.3s ease;
        }

        .bg-image {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            object-fit: cover;
            display: none;
        }

        .bg-video {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            object-fit: cover;
            display: none;
        }

        /* LAYER 2: Media Presentation Layer (Images, Videos, PowerPoint Slides, Live Camera) */
        .media-layer {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            z-index: 2;
            display: flex;
            justify-content: center;
            align-items: center;
            pointer-events: none;
        }

        .media-image, .media-ppt {
            max-width: 100%;
            max-height: 100%;
            width: 100%;
            height: 100%;
            object-fit: contain;
            display: none;
        }

        .media-video {
            max-width: 100%;
            max-height: 100%;
            width: 100%;
            height: 100%;
            object-fit: contain;
            display: none;
        }

        /* LAYER 3: Text / Lyrics / Bible Overlay Layer */
        .overlay-layer {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            z-index: 3;
            pointer-events: none;
        }

        .text-box {
            position: absolute;
            background-color: rgba(0, 0, 0, 0.45);
            border-radius: 12px;
            padding: 24px 36px;
            text-align: center;
            transition: all 0.2s ease-in-out;
            display: none;
            box-sizing: border-box;
        }

        .title-badge {
            display: inline-block;
            background-color: #D0BCFF;
            color: #381E72;
            font-size: 14px;
            font-weight: 700;
            padding: 4px 14px;
            border-radius: 6px;
            margin-bottom: 12px;
            letter-spacing: 0.5px;
            text-transform: uppercase;
        }

        .content-text {
            color: #ffffff;
            font-size: 38px;
            font-weight: 700;
            white-space: pre-wrap;
            line-height: 1.35;
            word-break: break-word;
            text-shadow: 2px 2px 6px rgba(0, 0, 0, 0.85);
        }

        /* Standby / Idle Welcome Watermark */
        #idle-banner {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            z-index: 4;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            text-align: center;
            background: linear-gradient(135deg, #0F172A 0%, #020617 100%);
            transition: opacity 0.4s ease;
        }

        #idle-banner h1 {
            font-size: 28px;
            font-weight: 600;
            color: rgba(255, 255, 255, 0.85);
            letter-spacing: 1px;
            margin-bottom: 8px;
        }

        #idle-banner p {
            font-size: 14px;
            color: rgba(255, 255, 255, 0.45);
            letter-spacing: 0.5px;
        }

        /* Blackout Overlay */
        #blackout-layer {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            z-index: 999;
            background-color: #000000;
            display: none;
        }

        /* Connection & Fullscreen Helper Bar */
        #status-bar {
            position: fixed;
            bottom: 12px;
            right: 14px;
            z-index: 1000;
            display: flex;
            align-items: center;
            gap: 10px;
            opacity: 0.7;
            transition: opacity 0.2s;
        }
        #status-bar:hover {
            opacity: 1;
        }

        .status-dot {
            width: 10px;
            height: 10px;
            border-radius: 50%;
            background-color: #ef4444;
            box-shadow: 0 0 6px rgba(239, 68, 68, 0.6);
            transition: all 0.3s;
        }
        .status-dot.connected {
            background-color: #10b981;
            box-shadow: 0 0 8px rgba(16, 185, 129, 0.8);
        }
        .status-dot.polling {
            background-color: #f59e0b;
            box-shadow: 0 0 8px rgba(245, 158, 11, 0.8);
        }

        .fs-btn {
            background: rgba(0,0,0,0.5);
            border: 1px solid rgba(255,255,255,0.2);
            color: #fff;
            padding: 4px 10px;
            border-radius: 4px;
            font-size: 11px;
            cursor: pointer;
            outline: none;
        }
    </style>
</head>
<body>
    <div id="screen-root">
        <!-- Blackout layer -->
        <div id="blackout-layer"></div>

        <!-- Normal Single Screen Container -->
        <div id="single-container">
            <div id="bg-layer" class="bg-layer">
                <img id="bg-image" class="bg-image" alt="Background">
                <video id="bg-video" class="bg-video" autoplay loop muted playsinline></video>
            </div>

            <div id="media-layer" class="media-layer">
                <img id="media-image" class="media-image" alt="Presentation Image">
                <img id="media-ppt" class="media-ppt" alt="PowerPoint Slide">
                <video id="media-video" class="media-video" autoplay playsinline></video>
            </div>

            <div id="overlay-layer" class="overlay-layer">
                <div id="text-box" class="text-box">
                    <div id="title-badge" class="title-badge"></div>
                    <div id="content-text" class="content-text"></div>
                </div>
            </div>
        </div>

        <!-- Split Screen Dual Container (30:70 / 70:30 Live Cam + Sermon Presentation) -->
        <div id="split-container">
            <!-- Cam Pane -->
            <div id="split-cam-pane" class="split-pane">
                <div class="cam-badge">
                    <div class="cam-badge-dot"></div>
                    <span>LIVE CAM</span>
                </div>
                <img id="split-cam-feed" alt="Live Camera Stream">
            </div>

            <!-- Divider -->
            <div id="split-divider"></div>

            <!-- Material / Sermon / Lyrics Pane -->
            <div id="split-content-pane" class="split-pane">
                <div id="split-bg-layer" class="bg-layer">
                    <img id="split-bg-image" class="bg-image" alt="Split Background">
                    <video id="split-bg-video" class="bg-video" autoplay loop muted playsinline></video>
                </div>

                <div id="split-media-layer" class="media-layer">
                    <img id="split-media-image" class="media-image" alt="Split Image">
                    <img id="split-media-ppt" class="media-ppt" alt="Split PowerPoint">
                    <video id="split-media-video" class="media-video" autoplay playsinline></video>
                </div>

                <div id="split-overlay-layer" class="overlay-layer">
                    <div id="split-text-box" class="text-box">
                        <div id="split-title-badge" class="title-badge"></div>
                        <div id="split-content-text" class="content-text"></div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Standby Idle Welcome Banner -->
        <div id="idle-banner">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="rgba(208, 188, 255, 0.8)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="margin-bottom: 12px;">
                <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
                <line x1="8" y1="21" x2="16" y2="21"></line>
                <line x1="12" y1="17" x2="12" y2="21"></line>
            </svg>
            <h1>Church Presentation System</h1>
            <p>Live Web Display • Ready to Present</p>
        </div>
    </div>

    <!-- Status & Fullscreen toggle -->
    <div id="status-bar">
        <button class="fs-btn" onclick="toggleFullScreen()">Fullscreen (F11)</button>
        <div id="status-indicator" class="status-dot" title="Koneksi Live Server"></div>
    </div>

    <script>
        const blackoutLayer = document.getElementById('blackout-layer');
        const singleContainer = document.getElementById('single-container');
        const splitContainer = document.getElementById('split-container');
        
        const splitCamPane = document.getElementById('split-cam-pane');
        const splitContentPane = document.getElementById('split-content-pane');
        const splitCamFeed = document.getElementById('split-cam-feed');
        
        // Single elements
        const bgImage = document.getElementById('bg-image');
        const bgVideo = document.getElementById('bg-video');
        const mediaImage = document.getElementById('media-image');
        const mediaPpt = document.getElementById('media-ppt');
        const mediaVideo = document.getElementById('media-video');
        const textBox = document.getElementById('text-box');
        const titleBadge = document.getElementById('title-badge');
        const contentText = document.getElementById('content-text');
        
        // Split elements
        const splitBgImage = document.getElementById('split-bg-image');
        const splitBgVideo = document.getElementById('split-bg-video');
        const splitMediaImage = document.getElementById('split-media-image');
        const splitMediaPpt = document.getElementById('split-media-ppt');
        const splitMediaVideo = document.getElementById('split-media-video');
        const splitTextBox = document.getElementById('split-text-box');
        const splitTitleBadge = document.getElementById('split-title-badge');
        const splitContentText = document.getElementById('split-content-text');
        
        const idleBanner = document.getElementById('idle-banner');
        const statusIndicator = document.getElementById('status-indicator');

        let ws = null;
        let isWsConnected = false;
        let pollTimer = null;

        function connectWebSocket() {
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const wsUrl = protocol + '//' + window.location.host + '/ws';
            
            try {
                ws = new WebSocket(wsUrl);
            } catch (e) {
                startPolling();
                return;
            }

            ws.onopen = function() {
                isWsConnected = true;
                statusIndicator.className = 'status-dot connected';
                statusIndicator.title = 'Live Real-time WebSocket Connected';
                stopPolling();
            };

            ws.onmessage = function(event) {
                try {
                    const state = JSON.parse(event.data);
                    renderPresentation(state);
                } catch (err) {}
            };

            ws.onclose = function() {
                isWsConnected = false;
                statusIndicator.className = 'status-dot polling';
                statusIndicator.title = 'Reconnecting WebSocket (Polling Active)';
                startPolling();
                setTimeout(connectWebSocket, 2000);
            };

            ws.onerror = function() {
                isWsConnected = false;
                try { ws.close(); } catch(e) {}
            };
        }

        function fetchStateHttp() {
            fetch('/api/state')
                .then(res => res.json())
                .then(data => {
                    if (!isWsConnected) {
                        statusIndicator.className = 'status-dot polling';
                    }
                    renderPresentation(data);
                })
                .catch(err => {
                    statusIndicator.className = 'status-dot';
                });
        }

        function startPolling() {
            if (!pollTimer) {
                fetchStateHttp();
                pollTimer = setInterval(fetchStateHttp, 1500);
            }
        }

        function stopPolling() {
            if (pollTimer) {
                clearInterval(pollTimer);
                pollTimer = null;
            }
        }

        function renderPresentation(data) {
            if (!data) return;

            // 1. BLACKOUT MODE
            if (data.status === 'BLACK') {
                blackoutLayer.style.display = 'block';
                return;
            } else {
                blackoutLayer.style.display = 'none';
            }

            const isSplit = Boolean(data.isSplitScreenEnabled);
            
            if (isSplit) {
                singleContainer.style.display = 'none';
                splitContainer.style.display = 'flex';
                
                // Configure Split Ratio & Side
                const camRatio = data.splitRatioCamPercent || 30;
                const side = data.splitScreenSide || 'CAM_LEFT_CONTENT_RIGHT';
                
                if (side === 'CAM_LEFT_CONTENT_RIGHT') {
                    splitContainer.style.flexDirection = 'row';
                    splitCamPane.style.width = camRatio + '%';
                    splitContentPane.style.width = (100 - camRatio) + '%';
                } else {
                    splitContainer.style.flexDirection = 'row-reverse';
                    splitCamPane.style.width = camRatio + '%';
                    splitContentPane.style.width = (100 - camRatio) + '%';
                }

                // Split Camera Feed
                const camUrl = data.splitCameraStreamUrl || '/camera/stream';
                if (splitCamFeed.src !== camUrl && !splitCamFeed.src.endsWith(camUrl)) {
                    splitCamFeed.src = camUrl;
                }

                // Render Content inside Split Material Pane
                renderLayers(data, {
                    bgImage: splitBgImage,
                    bgVideo: splitBgVideo,
                    mediaImage: splitMediaImage,
                    mediaPpt: splitMediaPpt,
                    mediaVideo: splitMediaVideo,
                    textBox: splitTextBox,
                    titleBadge: splitTitleBadge,
                    contentText: splitContentText
                });

            } else {
                splitContainer.style.display = 'none';
                singleContainer.style.display = 'block';
                
                renderLayers(data, {
                    bgImage: bgImage,
                    bgVideo: bgVideo,
                    mediaImage: mediaImage,
                    mediaPpt: mediaPpt,
                    mediaVideo: mediaVideo,
                    textBox: textBox,
                    titleBadge: titleBadge,
                    contentText: contentText
                });
            }

            // Standby / Idle Banner
            const hasText = Boolean(data.text && data.text.trim() !== '');
            const hasActiveMedia = Boolean(data.mediaUrl && data.mediaUrl.trim() !== '');
            const hasBg = Boolean(data.backgroundType && data.backgroundType !== 'NONE');
            const isPresenting = isSplit || hasActiveMedia || (hasText && data.status !== 'CLEAR') || hasBg;
            
            if (isPresenting || data.status === 'CLEAR') {
                idleBanner.style.opacity = '0';
                setTimeout(() => {
                    if (idleBanner.style.opacity === '0') idleBanner.style.display = 'none';
                }, 300);
            } else {
                idleBanner.style.display = 'flex';
                idleBanner.style.opacity = '1';
            }
        }

        function renderLayers(data, targets) {
            // Background
            const bgType = data.backgroundType || 'NONE';
            const bgUrl = data.backgroundMediaUrl || '';

            if ((bgType === 'IMAGE' || bgType === 'CAMERA' || bgType === 'IP_CAMERA') && bgUrl) {
                if (targets.bgImage.src !== bgUrl && !targets.bgImage.src.endsWith(bgUrl)) {
                    targets.bgImage.src = bgUrl;
                }
                targets.bgImage.style.display = 'block';
                targets.bgVideo.style.display = 'none';
                targets.bgVideo.pause();
            } else if (bgType === 'VIDEO' && bgUrl) {
                if (targets.bgVideo.src !== bgUrl && !targets.bgVideo.src.endsWith(bgUrl)) {
                    targets.bgVideo.src = bgUrl;
                    targets.bgVideo.play().catch(() => {});
                }
                targets.bgVideo.style.display = 'block';
                targets.bgImage.style.display = 'none';
            } else {
                targets.bgImage.style.display = 'none';
                targets.bgVideo.style.display = 'none';
                targets.bgVideo.pause();
            }

            // Media
            const mediaUrl = data.mediaUrl || '';
            const status = data.status || 'IDLE';

            if ((status === 'IMAGE' || status === 'CAMERA' || status === 'IP_CAMERA') && mediaUrl) {
                if (targets.mediaImage.src !== mediaUrl && !targets.mediaImage.src.endsWith(mediaUrl)) {
                    targets.mediaImage.src = mediaUrl;
                }
                targets.mediaImage.style.display = 'block';
                targets.mediaPpt.style.display = 'none';
                targets.mediaVideo.style.display = 'none';
                targets.mediaVideo.pause();
            } else if (status === 'POWERPOINT' && mediaUrl) {
                if (targets.mediaPpt.src !== mediaUrl && !targets.mediaPpt.src.endsWith(mediaUrl)) {
                    targets.mediaPpt.src = mediaUrl;
                }
                targets.mediaPpt.style.display = 'block';
                targets.mediaImage.style.display = 'none';
                targets.mediaVideo.style.display = 'none';
                targets.mediaVideo.pause();
            } else if (status === 'VIDEO' && mediaUrl) {
                if (targets.mediaVideo.src !== mediaUrl && !targets.mediaVideo.src.endsWith(mediaUrl)) {
                    targets.mediaVideo.src = mediaUrl;
                    targets.mediaVideo.play().catch(() => {});
                }
                targets.mediaVideo.style.display = 'block';
                targets.mediaImage.style.display = 'none';
                targets.mediaPpt.style.display = 'none';
            } else {
                targets.mediaImage.style.display = 'none';
                targets.mediaPpt.style.display = 'none';
                targets.mediaVideo.style.display = 'none';
                targets.mediaVideo.pause();
            }

            // Text Overlay
            const hasText = Boolean(data.text && data.text.trim() !== '');
            const isTextVisible = (status !== 'CLEAR') && hasText;

            if (isTextVisible) {
                targets.textBox.style.display = 'block';
                targets.contentText.innerText = data.text;

                if (data.title && data.title.trim() !== '') {
                    targets.titleBadge.style.display = 'inline-block';
                    targets.titleBadge.innerText = data.title;
                } else {
                    targets.titleBadge.style.display = 'none';
                }

                // Typography & Formatting
                targets.contentText.style.fontSize = (data.fontSize || 38) + 'px';
                targets.contentText.style.color = data.textColor || '#ffffff';
                targets.contentText.style.textAlign = data.textAlign || 'center';
                targets.contentText.style.fontWeight = data.isBold ? 'bold' : 'normal';
                targets.contentText.style.textTransform = data.isTextUppercase ? 'uppercase' : 'none';
                targets.contentText.style.lineHeight = (data.textLineHeightMultiplier || 1.35);
                
                if (data.isShadowEnabled) {
                    targets.contentText.style.textShadow = '2px 2px 8px rgba(0,0,0,0.95)';
                } else {
                    targets.contentText.style.textShadow = 'none';
                }

                const bgAlpha = data.bgAlpha !== undefined ? data.bgAlpha : 0.45;
                targets.textBox.style.backgroundColor = 'rgba(0, 0, 0, ' + bgAlpha + ')';
                targets.textBox.style.borderRadius = (data.textBoxCornerRadiusDp !== undefined ? data.textBoxCornerRadiusDp : 12) + 'px';
                targets.textBox.style.padding = (data.textBoxPaddingDp !== undefined ? data.textBoxPaddingDp : 20) + 'px ' + ((data.textBoxPaddingDp || 20) + 12) + 'px';
                
                if (data.textBoxBorderEnabled) {
                    targets.textBox.style.border = '1.5px solid rgba(208, 188, 255, 0.6)';
                    targets.textBox.style.boxShadow = '0 0 15px rgba(208, 188, 255, 0.2)';
                } else {
                    targets.textBox.style.border = 'none';
                    targets.textBox.style.boxShadow = 'none';
                }

                // Positioning
                applyFlexibleTextPosition(targets.textBox, data);
            } else {
                targets.textBox.style.display = 'none';
            }
        }

        function applyFlexibleTextPosition(box, data) {
            const pos = data.position || 'CENTER';

            // Reset positioning rules
            box.style.top = 'auto';
            box.style.bottom = 'auto';
            box.style.left = 'auto';
            box.style.right = 'auto';
            box.style.transform = 'none';
            box.style.width = 'auto';
            box.style.maxWidth = '92%';

            switch (pos) {
                case 'CENTER':
                    box.style.top = '50%';
                    box.style.left = '50%';
                    box.style.transform = 'translate(-50%, -50%)';
                    box.style.maxWidth = (data.textBoxWidthPercent || 90) + '%';
                    break;
                case 'LOWER_THIRD':
                    box.style.bottom = '32px';
                    box.style.left = '50%';
                    box.style.transform = 'translateX(-50%)';
                    box.style.width = (data.textBoxWidthPercent || 95) + '%';
                    box.style.maxWidth = '98%';
                    break;
                case 'BOTTOM_CENTER':
                    box.style.bottom = '36px';
                    box.style.left = '50%';
                    box.style.transform = 'translateX(-50%)';
                    box.style.maxWidth = (data.textBoxWidthPercent || 90) + '%';
                    break;
                case 'TOP_BANNER':
                    box.style.top = '32px';
                    box.style.left = '50%';
                    box.style.transform = 'translateX(-50%)';
                    box.style.width = (data.textBoxWidthPercent || 95) + '%';
                    box.style.maxWidth = '98%';
                    break;
                case 'LEFT_CENTER':
                    box.style.top = '50%';
                    box.style.left = '36px';
                    box.style.transform = 'translateY(-50%)';
                    box.style.maxWidth = (data.textBoxWidthPercent || 75) + '%';
                    break;
                case 'RIGHT_CENTER':
                    box.style.top = '50%';
                    box.style.right = '36px';
                    box.style.transform = 'translateY(-50%)';
                    box.style.maxWidth = (data.textBoxWidthPercent || 75) + '%';
                    break;
                case 'TOP_LEFT':
                    box.style.top = '32px';
                    box.style.left = '36px';
                    box.style.maxWidth = (data.textBoxWidthPercent || 75) + '%';
                    break;
                case 'TOP_RIGHT':
                    box.style.top = '32px';
                    box.style.right = '36px';
                    box.style.maxWidth = (data.textBoxWidthPercent || 75) + '%';
                    break;
                case 'BOTTOM_LEFT':
                    box.style.bottom = '32px';
                    box.style.left = '36px';
                    box.style.maxWidth = (data.textBoxWidthPercent || 75) + '%';
                    break;
                case 'BOTTOM_RIGHT':
                    box.style.bottom = '32px';
                    box.style.right = '36px';
                    box.style.maxWidth = (data.textBoxWidthPercent || 75) + '%';
                    break;
                case 'CUSTOM':
                default:
                    const vPct = (data.textVerticalPercent !== undefined) ? data.textVerticalPercent : 50;
                    const hPct = (data.textHorizontalPercent !== undefined) ? data.textHorizontalPercent : 50;
                    const wPct = (data.textBoxWidthPercent !== undefined) ? data.textBoxWidthPercent : 85;
                    box.style.top = vPct + '%';
                    box.style.left = hPct + '%';
                    box.style.transform = 'translate(-50%, -50%)';
                    box.style.width = wPct + '%';
                    box.style.maxWidth = '98%';
                    break;
            }
        }

        function toggleFullScreen() {
            if (!document.fullscreenElement) {
                document.documentElement.requestFullscreen().catch(err => {});
            } else {
                if (document.exitFullscreen) {
                    document.exitFullscreen().catch(err => {});
                }
            }
        }

        document.addEventListener('dblclick', toggleFullScreen);

        // Start connection
        fetchStateHttp();
        connectWebSocket();
    </script>
</body>
</html>"""
    }

    private fun getStageMonitorHtml(): String {
        return """<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
    <title>Stage Confidence Monitor</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        html, body {
            width: 100vw;
            height: 100vh;
            overflow: hidden;
            background-color: #0A0A0C;
            color: #ffffff;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            user-select: none;
        }

        #stage-root {
            display: flex;
            flex-direction: column;
            width: 100%;
            height: 100%;
            padding: 16px;
            gap: 12px;
        }

        /* TOP BAR: REALTIME CLOCK & SERMON TIMER */
        #top-bar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: #18181B;
            border-radius: 12px;
            padding: 12px 20px;
            border: 1px solid #27272A;
        }

        .bar-section {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .bar-label {
            font-size: 11px;
            font-weight: 700;
            letter-spacing: 1px;
            color: #A1A1AA;
            text-transform: uppercase;
        }

        #realtime-clock {
            font-size: 28px;
            font-weight: 900;
            color: #FACC15;
            letter-spacing: 1px;
            font-variant-numeric: tabular-nums;
        }

        #sermon-timer-box {
            text-align: right;
        }

        #timer-status-badge {
            font-size: 11px;
            font-weight: 800;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin-bottom: 2px;
        }

        #sermon-timer {
            font-size: 36px;
            font-weight: 900;
            letter-spacing: 1px;
            font-variant-numeric: tabular-nums;
            line-height: 1;
        }

        .timer-normal { color: #10B981; }
        .timer-warning { color: #F59E0B; }
        .timer-overtime { color: #EF4444; animation: pulse 1s infinite; }
        .timer-paused { color: #94A3B8; }

        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.4; }
        }

        /* STAGE ALERT BANNER */
        #alert-banner {
            display: none;
            background: #DC2626;
            border: 3px solid #FDE047;
            border-radius: 10px;
            padding: 12px 18px;
            text-align: center;
            font-size: 22px;
            font-weight: 900;
            color: #ffffff;
            box-shadow: 0 0 20px rgba(220, 38, 38, 0.6);
            animation: pulse 1.5s infinite;
        }

        /* MAIN CURRENT SLIDE BOX */
        #current-slide-card {
            flex: 1.6;
            background: #18181B;
            border: 3px solid #3B82F6;
            border-radius: 14px;
            padding: 24px;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            text-align: center;
            position: relative;
            box-shadow: 0 8px 24px rgba(0,0,0,0.5);
        }

        #current-meta-header {
            position: absolute;
            top: 14px;
            left: 20px;
            right: 20px;
            display: flex;
            justify-content: space-between;
            font-size: 13px;
            font-weight: 700;
            color: #60A5FA;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        #current-slide-text {
            font-size: 42px;
            font-weight: 800;
            line-height: 1.35;
            color: #FFFFFF;
            max-width: 95%;
            word-wrap: break-word;
            white-space: pre-line;
        }

        /* NEXT SLIDE CUE CARD */
        #next-slide-card {
            flex: 0.8;
            background: #121214;
            border: 1px solid #3F3F46;
            border-radius: 12px;
            padding: 14px 20px;
            display: flex;
            flex-direction: column;
            justify-content: flex-start;
        }

        #next-meta-header {
            font-size: 12px;
            font-weight: 800;
            color: #FBBF24;
            letter-spacing: 0.5px;
            margin-bottom: 6px;
        }

        #next-slide-text {
            font-size: 22px;
            font-weight: 600;
            line-height: 1.35;
            color: #D4D4D8;
            white-space: pre-line;
        }

        /* FULLSCREEN BUTTON */
        #btn-fs {
            position: fixed;
            bottom: 12px;
            right: 12px;
            background: rgba(255,255,255,0.1);
            color: #fff;
            border: 1px solid #444;
            border-radius: 20px;
            padding: 6px 12px;
            font-size: 11px;
            cursor: pointer;
            z-index: 100;
        }
    </style>
</head>
<body>
    <div id="stage-root">
        <!-- Top Bar -->
        <div id="top-bar">
            <div class="bar-section">
                <div>
                    <div class="bar-label">🕒 REALTIME CLOCK</div>
                    <div id="realtime-clock">--:--:--</div>
                </div>
            </div>

            <div id="sermon-timer-box">
                <div id="timer-status-badge" class="timer-normal">⏱️ KHOTBAH (READY)</div>
                <div id="sermon-timer" class="timer-normal">30:00</div>
            </div>
        </div>

        <!-- Flash Alert -->
        <div id="alert-banner">📢 PESAN OPERATOR: <span id="alert-text"></span></div>

        <!-- Main Slide -->
        <div id="current-slide-card">
            <div id="current-meta-header">
                <span id="current-title">▶ LIVE PRESENTATION</span>
                <span id="current-slide-num">SLIDE 1</span>
            </div>
            <div id="current-slide-text">MENUNGGU TAYANGAN...</div>
        </div>

        <!-- Next Slide Preview -->
        <div id="next-slide-card">
            <div id="next-meta-header">⏭️ BERIKUTNYA (NEXT CUE):</div>
            <div id="next-slide-text">[AKHIR LAGU / MATERI]</div>
        </div>
    </div>

    <button id="btn-fs" onclick="toggleFullScreen()">⛶ Layar Penuh</button>

    <script>
        let ws = null;
        let lastState = null;
        let clientTimerSecs = 1800;
        let clientTimerRunning = false;

        function updateRealtimeClock() {
            const now = new Date();
            const hrs = String(now.getHours()).padStart(2, '0');
            const mins = String(now.getMinutes()).padStart(2, '0');
            const secs = String(now.getSeconds()).padStart(2, '0');
            document.getElementById('realtime-clock').innerText = hrs + ':' + mins + ':' + secs;
        }
        setInterval(updateRealtimeClock, 1000);
        updateRealtimeClock();

        function renderState(state) {
            lastState = state;
            if (!state) return;

            // 1. Text & Slide Info
            const currentTitle = document.getElementById('current-title');
            const currentSlideNum = document.getElementById('current-slide-num');
            const currentText = document.getElementById('current-slide-text');
            const nextText = document.getElementById('next-slide-text');

            if (state.status === 'BLACK') {
                currentText.innerText = '[ LAYAR HITAM / BLACKOUT ]';
                currentTitle.innerText = 'STATUS: BLACKOUT';
            } else if (state.status === 'CLEAR') {
                currentText.innerText = '';
                currentTitle.innerText = 'STATUS: CLEAR';
            } else {
                currentText.innerText = state.text || state.title || 'STAGE MONITOR READY';
                currentTitle.innerText = '▶ ' + (state.title || 'LIVE CONTENT');
            }

            const total = state.totalSlides || 0;
            const currentIdx = (state.slideIndex || 0) + 1;
            currentSlideNum.innerText = total > 0 ? ('SLIDE ' + currentIdx + ' / ' + total) : '';

            nextText.innerText = state.nextText ? state.nextText : '[AKHIR MATERI / SLIDE TERAKHIR]';

            // 2. Sermon Timer
            clientTimerRunning = !!state.sermonTimerRunning;
            clientTimerSecs = state.sermonTimerRemainingSeconds !== undefined ? state.sermonTimerRemainingSeconds : 1800;
            renderTimerUi();

            // 3. Stage Alert
            const alertBanner = document.getElementById('alert-banner');
            const alertText = document.getElementById('alert-text');
            if (state.isStageAlertActive && state.stageAlertMessage) {
                alertText.innerText = state.stageAlertMessage;
                alertBanner.style.display = 'block';
            } else {
                alertBanner.style.display = 'none';
            }
        }

        function renderTimerUi() {
            const timerEl = document.getElementById('sermon-timer');
            const badgeEl = document.getElementById('timer-status-badge');
            
            const isOvertime = clientTimerSecs < 0;
            const absSecs = Math.abs(clientTimerSecs);
            const hrs = Math.floor(absSecs / 3600);
            const mins = Math.floor((absSecs % 3600) / 60);
            const secs = absSecs % 60;

            let formatted = '';
            if (hrs > 0) {
                formatted = (isOvertime ? '+' : '') + String(hrs).padStart(2, '0') + ':' + String(mins).padStart(2, '0') + ':' + String(secs).padStart(2, '0');
            } else {
                formatted = (isOvertime ? '+' : '') + String(mins).padStart(2, '0') + ':' + String(secs).padStart(2, '0');
            }

            timerEl.innerText = formatted;

            let colorClass = 'timer-normal';
            let badgeText = clientTimerRunning ? '⏱️ KHOTBAH (LIVE)' : '⏸️ KHOTBAH (PAUSED)';

            if (isOvertime) {
                colorClass = 'timer-overtime';
                badgeText = '⚠️ OVERTIME';
            } else if (clientTimerSecs <= 300) {
                colorClass = 'timer-warning';
                badgeText = clientTimerRunning ? '⏱️ SISA < 5 MENIT' : '⏸️ PAUSED';
            } else if (!clientTimerRunning) {
                colorClass = 'timer-paused';
            }

            timerEl.className = colorClass;
            badgeEl.className = colorClass;
            badgeEl.innerText = badgeText;
        }

        // Local 1-second ticker for smooth timer if WS has slight delay
        setInterval(() => {
            if (clientTimerRunning) {
                clientTimerSecs--;
                renderTimerUi();
            }
        }, 1000);

        function connectWebSocket() {
            const loc = window.location;
            const wsProtocol = loc.protocol === 'https:' ? 'wss:' : 'ws:';
            const wsUrl = wsProtocol + '//' + loc.host + '/ws';
            ws = new WebSocket(wsUrl);

            ws.onmessage = function(event) {
                try {
                    const data = JSON.parse(event.data);
                    renderState(data);
                } catch(e) {}
            };

            ws.onclose = function() {
                setTimeout(connectWebSocket, 1500);
            };

            ws.onerror = function() {
                try { ws.close(); } catch(e) {}
            };
        }

        function fetchStateHttp() {
            fetch('/api/state')
                .then(res => res.json())
                .then(data => renderState(data))
                .catch(err => {});
        }

        function toggleFullScreen() {
            if (!document.fullscreenElement) {
                document.documentElement.requestFullscreen().catch(err => {});
            } else {
                if (document.exitFullscreen) {
                    document.exitFullscreen().catch(err => {});
                }
            }
        }

        fetchStateHttp();
        connectWebSocket();
    </script>
</body>
</html>"""
    }

    private fun getTransparentViewerHtml(): String {
        return """<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>OBS Transparent Output</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        html, body {
            width: 100vw;
            height: 100vh;
            overflow: hidden;
            background: transparent !important;
            color: #ffffff;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            user-select: none;
        }

        #screen-root {
            position: relative;
            width: 100%;
            height: 100%;
            display: flex;
            background: transparent;
        }

        .text-overlay {
            position: absolute;
            transition: all 0.25s ease-out;
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 10;
        }

        .text-box {
            box-sizing: border-box;
            word-wrap: break-word;
            white-space: pre-line;
            text-shadow: 0 2px 8px rgba(0,0,0,0.9), 0 0 20px rgba(0,0,0,0.7);
        }
    </style>
</head>
<body>
    <div id="screen-root">
        <div id="text-container" class="text-overlay">
            <div id="text-box" class="text-box"></div>
        </div>
    </div>
    <script>
        function renderState(state) {
            const box = document.getElementById('text-box');
            const container = document.getElementById('text-container');
            if (state.status === 'BLACK' || state.status === 'CLEAR' || !state.text) {
                box.style.display = 'none';
                return;
            }
            box.style.display = 'block';
            box.innerText = state.text;
            box.style.fontSize = (state.fontSize || 32) + 'px';
            box.style.color = state.textColor || '#FFFFFF';
            box.style.fontWeight = state.isBold ? '800' : '500';
            box.style.textAlign = state.textAlign || 'center';
            box.style.backgroundColor = 'rgba(0, 0, 0, ' + (state.bgAlpha !== undefined ? state.bgAlpha : 0.4) + ')';
            box.style.borderRadius = (state.textBoxCornerRadiusDp || 12) + 'px';
            box.style.padding = (state.textBoxPaddingDp || 16) + 'px';
            box.style.width = (state.textBoxWidthPercent || 90) + '%';
            
            container.style.top = (state.textVerticalPercent || 50) + '%';
            container.style.left = (state.textHorizontalPercent || 50) + '%';
            container.style.transform = 'translate(-' + (state.textHorizontalPercent || 50) + '%, -' + (state.textVerticalPercent || 50) + '%)';
            container.style.width = '100%';
        }

        function connectWebSocket() {
            const loc = window.location;
            const ws = new WebSocket((loc.protocol === 'https:' ? 'wss:' : 'ws:') + '//' + loc.host + '/ws');
            ws.onmessage = (e) => {
                try { renderState(JSON.parse(e.data)); } catch(err) {}
            };
            ws.onclose = () => setTimeout(connectWebSocket, 1500);
        }
        fetch('/api/state').then(r => r.json()).then(renderState).catch(() => {});
        connectWebSocket();
    </script>
</body>
</html>"""
    }
}


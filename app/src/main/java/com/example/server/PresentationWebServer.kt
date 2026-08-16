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
                    totalSlides = content.slides.size
                    content.slides.forEach { slidesArray.put(it) }
                    if (content.slides.isNotEmpty()) {
                        val index = slideIndex.coerceIn(0, content.slides.size - 1)
                        text = content.slides[index]
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

            // Text Styles & Positioning
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

        /* LAYER 1: Background Layer */
        #bg-layer {
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

        #bg-image {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            object-fit: cover;
            display: none;
        }

        #bg-video {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            object-fit: cover;
            display: none;
        }

        /* LAYER 2: Media Presentation Layer (Images, Videos, PowerPoint Slides, Live Camera) */
        #media-layer {
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

        #media-image, #media-ppt {
            max-width: 100%;
            max-height: 100%;
            width: 100%;
            height: 100%;
            object-fit: contain;
            display: none;
        }

        #media-video {
            max-width: 100%;
            max-height: 100%;
            width: 100%;
            height: 100%;
            object-fit: contain;
            display: none;
        }

        /* LAYER 3: Text / Lyrics / Bible Overlay Layer */
        #overlay-layer {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            z-index: 3;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 32px;
            pointer-events: none;
            transition: all 0.25s ease-out;
        }

        #text-box {
            background-color: rgba(0, 0, 0, 0.45);
            border-radius: 12px;
            padding: 24px 36px;
            text-align: center;
            max-width: 95%;
            transition: all 0.2s ease-in-out;
            display: none;
        }

        #title-badge {
            display: inline-block;
            background-color: #D0BCFF;
            color: #381E72;
            font-size: 15px;
            font-weight: 700;
            padding: 4px 14px;
            border-radius: 6px;
            margin-bottom: 14px;
            letter-spacing: 0.5px;
            text-transform: uppercase;
        }

        #content-text {
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

        <!-- Background layer -->
        <div id="bg-layer">
            <img id="bg-image" alt="Background">
            <video id="bg-video" autoplay loop muted playsinline></video>
        </div>

        <!-- Media presentation layer -->
        <div id="media-layer">
            <img id="media-image" alt="Presentation Image">
            <img id="media-ppt" alt="PowerPoint Slide">
            <video id="media-video" autoplay playsinline></video>
        </div>

        <!-- Lyrics & Bible overlay layer -->
        <div id="overlay-layer">
            <div id="text-box">
                <div id="title-badge"></div>
                <div id="content-text"></div>
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
        const bgLayer = document.getElementById('bg-layer');
        const bgImage = document.getElementById('bg-image');
        const bgVideo = document.getElementById('bg-video');
        
        const mediaImage = document.getElementById('media-image');
        const mediaPpt = document.getElementById('media-ppt');
        const mediaVideo = document.getElementById('media-video');
        
        const overlayLayer = document.getElementById('overlay-layer');
        const textBox = document.getElementById('text-box');
        const titleBadge = document.getElementById('title-badge');
        const contentText = document.getElementById('content-text');
        
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

            // 2. BACKGROUND LAYER
            const bgType = data.backgroundType || 'NONE';
            const bgUrl = data.backgroundMediaUrl || '';

            if ((bgType === 'IMAGE' || bgType === 'CAMERA') && bgUrl) {
                if (bgImage.src !== bgUrl && !bgImage.src.endsWith(bgUrl)) {
                    bgImage.src = bgUrl;
                }
                bgImage.style.display = 'block';
                bgVideo.style.display = 'none';
                bgVideo.pause();
            } else if (bgType === 'IP_CAMERA' && bgUrl) {
                // IP Camera (DroidCam) stream is MJPEG
                if (bgImage.src !== bgUrl && !bgImage.src.endsWith(bgUrl)) {
                    bgImage.src = bgUrl;
                }
                bgImage.style.display = 'block';
                bgVideo.style.display = 'none';
                bgVideo.pause();
            } else if (bgType === 'VIDEO' && bgUrl) {
                if (bgVideo.src !== bgUrl && !bgVideo.src.endsWith(bgUrl)) {
                    bgVideo.src = bgUrl;
                    bgVideo.play().catch(() => {});
                }
                bgVideo.style.display = 'block';
                bgImage.style.display = 'none';
            } else {
                bgImage.style.display = 'none';
                bgVideo.style.display = 'none';
                bgVideo.pause();
            }

            // 3. MEDIA CONTENT LAYER
            const contentType = data.contentType || 'NONE';
            const mediaUrl = data.mediaUrl || '';
            const status = data.status || 'IDLE';

            let hasActiveMedia = false;

            if ((status === 'IMAGE' || status === 'CAMERA') && mediaUrl) {
                hasActiveMedia = true;
                if (mediaImage.src !== mediaUrl && !mediaImage.src.endsWith(mediaUrl)) {
                    mediaImage.src = mediaUrl;
                }
                mediaImage.style.display = 'block';
                mediaPpt.style.display = 'none';
                mediaVideo.style.display = 'none';
                mediaVideo.pause();
            } else if (status === 'IP_CAMERA' && mediaUrl) {
                hasActiveMedia = true;
                if (mediaImage.src !== mediaUrl && !mediaImage.src.endsWith(mediaUrl)) {
                    mediaImage.src = mediaUrl;
                }
                mediaImage.style.display = 'block';
                mediaPpt.style.display = 'none';
                mediaVideo.style.display = 'none';
            } else if (status === 'POWERPOINT' && mediaUrl) {
                hasActiveMedia = true;
                if (mediaPpt.src !== mediaUrl && !mediaPpt.src.endsWith(mediaUrl)) {
                    mediaPpt.src = mediaUrl;
                }
                mediaPpt.style.display = 'block';
                mediaImage.style.display = 'none';
                mediaVideo.style.display = 'none';
                mediaVideo.pause();
            } else if (status === 'VIDEO' && mediaUrl) {
                hasActiveMedia = true;
                if (mediaVideo.src !== mediaUrl && !mediaVideo.src.endsWith(mediaUrl)) {
                    mediaVideo.src = mediaUrl;
                    mediaVideo.play().catch(() => {});
                }
                mediaVideo.style.display = 'block';
                mediaImage.style.display = 'none';
                mediaPpt.style.display = 'none';
            } else {
                mediaImage.style.display = 'none';
                mediaPpt.style.display = 'none';
                mediaVideo.style.display = 'none';
                mediaVideo.pause();
            }

            // 4. LYRICS / BIBLE TEXT OVERLAY LAYER
            const hasText = Boolean(data.text && data.text.trim() !== '');
            const isTextVisible = (status !== 'CLEAR') && hasText;

            if (isTextVisible) {
                textBox.style.display = 'block';
                contentText.innerText = data.text;

                if (data.title && data.title.trim() !== '') {
                    titleBadge.style.display = 'inline-block';
                    titleBadge.innerText = data.title;
                } else {
                    titleBadge.style.display = 'none';
                }

                // Apply text styling
                contentText.style.fontSize = (data.fontSize || 38) + 'px';
                contentText.style.color = data.textColor || '#ffffff';
                contentText.style.textAlign = data.textAlign || 'center';
                contentText.style.fontWeight = data.isBold ? 'bold' : 'normal';
                
                if (data.isShadowEnabled) {
                    contentText.style.textShadow = '2px 2px 8px rgba(0,0,0,0.9)';
                } else {
                    contentText.style.textShadow = 'none';
                }

                const bgAlpha = data.bgAlpha !== undefined ? data.bgAlpha : 0.45;
                textBox.style.backgroundColor = 'rgba(0, 0, 0, ' + bgAlpha + ')';

                // Positioning
                applyPositioning(data.position || 'CENTER');
            } else {
                textBox.style.display = 'none';
            }

            // 5. STANDBY / IDLE BANNER
            const hasAnyContent = hasActiveMedia || isTextVisible || (bgType !== 'NONE');
            if (hasAnyContent || status === 'CLEAR') {
                idleBanner.style.opacity = '0';
                setTimeout(() => {
                    if (idleBanner.style.opacity === '0') idleBanner.style.display = 'none';
                }, 300);
            } else {
                idleBanner.style.display = 'flex';
                idleBanner.style.opacity = '1';
            }
        }

        function applyPositioning(pos) {
            overlayLayer.style.justifyContent = 'center';
            overlayLayer.style.alignItems = 'center';
            overlayLayer.style.padding = '32px';
            textBox.style.width = 'auto';
            textBox.style.maxWidth = '95%';

            if (pos === 'LOWER_THIRD' || pos === 'BOTTOM_CENTER') {
                overlayLayer.style.justifyContent = 'center';
                overlayLayer.style.alignItems = 'flex-end';
                overlayLayer.style.padding = '0 32px 36px 32px';
                textBox.style.width = '100%';
                textBox.style.maxWidth = '100%';
            } else if (pos === 'TOP_BANNER') {
                overlayLayer.style.justifyContent = 'center';
                overlayLayer.style.alignItems = 'flex-start';
                overlayLayer.style.padding = '36px 32px 0 32px';
                textBox.style.width = '100%';
                textBox.style.maxWidth = '100%';
            } else if (pos === 'LEFT_CENTER') {
                overlayLayer.style.justifyContent = 'flex-start';
                overlayLayer.style.alignItems = 'center';
                overlayLayer.style.padding = '32px';
                textBox.style.width = '80%';
                textBox.style.maxWidth = '80%';
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
}

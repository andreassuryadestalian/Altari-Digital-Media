package com.example.features.video

import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUri: String,
    isPlaying: Boolean = true,
    isLooping: Boolean = true,
    isMuted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cleanUri = videoUri.trim()
    val isMediaFile = cleanUri.lowercase().endsWith(".mp4") ||
            cleanUri.lowercase().endsWith(".m3u8") ||
            cleanUri.lowercase().endsWith(".webm") ||
            cleanUri.lowercase().endsWith(".mkv")

    val isIpCamera = !isMediaFile && (
            cleanUri.startsWith("http://") ||
            cleanUri.startsWith("https://") ||
            cleanUri.contains(":4747") ||
            cleanUri.contains("/video") ||
            cleanUri.contains("/mjpegfeed") ||
            cleanUri.contains("/camera/stream")
    )

    if (isIpCamera) {
        IpCameraStreamPlayer(
            streamUrl = cleanUri,
            modifier = modifier
        )
    } else {
        LocalVideoPlayer(
            videoUri = cleanUri,
            isPlaying = isPlaying,
            isLooping = isLooping,
            isMuted = isMuted,
            modifier = modifier
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun LocalVideoPlayer(
    videoUri: String,
    isPlaying: Boolean,
    isLooping: Boolean,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            try {
                val mediaItem = MediaItem.fromUri(Uri.parse(videoUri))
                setMediaItem(mediaItem)
                repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                volume = if (isMuted) 0f else 1f
                prepare()
                playWhenReady = isPlaying
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
        onDispose { }
    }

    DisposableEffect(videoUri) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
fun IpCameraStreamPlayer(
    streamUrl: String,
    modifier: Modifier = Modifier
) {
    var activeStreamUrl by remember(streamUrl) { mutableStateOf(streamUrl) }
    var hasError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var retryCount by remember { mutableIntStateOf(0) }

    val formattedUrl = remember(activeStreamUrl) {
        var url = activeStreamUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        url
    }

    val targetUrl = remember(formattedUrl) {
        when {
            formattedUrl.contains("/mjpegfeed") || formattedUrl.contains("/video") || formattedUrl.contains("/camera/stream") -> formattedUrl
            formattedUrl.endsWith("/") -> "${formattedUrl}video"
            else -> "$formattedUrl/video"
        }
    }

    val baseUrl = remember(targetUrl) {
        if (targetUrl.contains("/")) {
            targetUrl.substringBeforeLast("/") + "/"
        } else {
            targetUrl
        }
    }

    LaunchedEffect(targetUrl, retryCount) {
        isLoading = true
        hasError = false
        // Allow time for the MJPEG frame header to connect
        kotlinx.coroutines.delay(1800)
        if (isLoading) {
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        key(targetUrl, retryCount) {
            AndroidView(
                factory = { ctx ->
                    try {
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                allowContentAccess = true
                                allowFileAccess = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                mediaPlaybackRequiresUserGesture = false
                                cacheMode = WebSettings.LOAD_NO_CACHE
                            }
                            setBackgroundColor(android.graphics.Color.BLACK)

                            addJavascriptInterface(object {
                                @android.webkit.JavascriptInterface
                                fun onImageLoaded() {
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        hasError = false
                                        isLoading = false
                                    }
                                }

                                @android.webkit.JavascriptInterface
                                fun onImageError() {
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        hasError = true
                                        isLoading = false
                                    }
                                }
                            }, "AndroidBridge")

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    hasError = false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    val reqUrl = request?.url?.toString() ?: ""
                                    if (request?.isForMainFrame == true || reqUrl.contains("4747") || reqUrl == targetUrl) {
                                        hasError = true
                                        isLoading = false
                                    }
                                }

                                override fun onReceivedHttpError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    errorResponse: WebResourceResponse?
                                ) {
                                    val reqUrl = request?.url?.toString() ?: ""
                                    if (request?.isForMainFrame == true || reqUrl == targetUrl) {
                                        hasError = true
                                        isLoading = false
                                    }
                                }

                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?
                                ) {
                                    handler?.proceed()
                                }
                            }

                            val htmlContent = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                <style>
                                    * { margin: 0; padding: 0; box-sizing: border-box; }
                                    html, body {
                                        width: 100%; height: 100%;
                                        background-color: #000;
                                        display: flex; align-items: center; justify-content: center;
                                        overflow: hidden;
                                    }
                                    img {
                                        width: 100%; height: 100%;
                                        object-fit: contain;
                                        display: block;
                                    }
                                </style>
                                </head>
                                <body>
                                    <img id="camFeed" src="$targetUrl"
                                         onload="if(window.AndroidBridge) window.AndroidBridge.onImageLoaded();"
                                         onerror="if(window.AndroidBridge) window.AndroidBridge.onImageError();" />
                                </body>
                                </html>
                            """.trimIndent()

                            // Use baseUrl matching targetUrl origin to avoid Chromium Private Network Access blocking
                            loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "UTF-8", null)
                        }
                    } catch (e: Throwable) {
                        android.util.Log.e("IpCameraStreamPlayer", "Error initializing WebView", e)
                        hasError = true
                        isLoading = false
                        android.widget.TextView(ctx).apply {
                            text = "WebView tidak tersedia: ${e.localizedMessage}"
                            setTextColor(android.graphics.Color.RED)
                        }
                    }
                },
                onRelease = { view ->
                    try {
                        if (view is WebView) {
                            view.stopLoading()
                            view.destroy()
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading Indicator Overlay
        if (isLoading && !hasError) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFD0BCFF),
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                    Column {
                        Text(
                            text = "🎥 Menghubungkan Kamera...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = targetUrl,
                            color = Color(0xFFD0BCFF),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Error Diagnosis & Troubleshooting Overlay
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF18181B).copy(alpha = 0.95f))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.widthIn(max = 460.dp)
                ) {
                    Text(
                        text = "⚠️ Gagal Membuka Live Feed Kamera",
                        color = Color(0xFFEF4444),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        color = Color(0xFF27272A),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = targetUrl,
                            color = Color(0xFFD0BCFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF27272A), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text("💡 Panduan Solusi Cepat:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("1. Pastikan kedua HP terhubung ke Wi-Fi / Hotspot yang SAMA.", color = Color.LightGray, fontSize = 11.sp)
                        Text("2. Buka DroidCam di HP kamera dan pastikan status 'STARTED'.", color = Color.LightGray, fontSize = 11.sp)
                        Text("3. Pastikan IP dan Port sesuai (default port DroidCam: 4747).", color = Color.LightGray, fontSize = 11.sp)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                retryCount++
                                hasError = false
                                isLoading = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🔄 Muat Ulang", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        if (!targetUrl.contains("/mjpegfeed")) {
                            Button(
                                onClick = {
                                    activeStreamUrl = targetUrl.replace("/video", "/mjpegfeed")
                                    retryCount++
                                    hasError = false
                                    isLoading = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Coba /mjpegfeed", color = Color(0xFFD0BCFF), fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    activeStreamUrl = targetUrl.replace("/mjpegfeed", "/video")
                                    retryCount++
                                    hasError = false
                                    isLoading = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Coba /video", color = Color(0xFFD0BCFF), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

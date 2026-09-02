package com.example.features.video

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    val isIpCamera = (videoUri.startsWith("http") && !videoUri.lowercase().endsWith(".mp4") && !videoUri.lowercase().endsWith(".m3u8") && !videoUri.lowercase().endsWith(".webm"))
    if (isIpCamera) {
        IpCameraStreamPlayer(
            streamUrl = videoUri,
            modifier = modifier
        )
    } else {
        LocalVideoPlayer(
            videoUri = videoUri,
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
    var hasError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var retryCount by remember { mutableIntStateOf(0) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    val formattedUrl = remember(streamUrl) {
        var url = streamUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        url
    }

    LaunchedEffect(formattedUrl, retryCount) {
        isLoading = true
        hasError = false
        kotlinx.coroutines.delay(1200)
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        key(formattedUrl, retryCount) {
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
                                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                mediaPlaybackRequiresUserGesture = false
                            }
                            setBackgroundColor(android.graphics.Color.BLACK)

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
                                    if (request?.isForMainFrame == true) {
                                        hasError = true
                                        isLoading = false
                                    }
                                }
                            }

                            val targetUrl = if (formattedUrl.contains("/mjpegfeed") || formattedUrl.contains("/video")) {
                                formattedUrl
                            } else {
                                if (formattedUrl.endsWith("/")) "${formattedUrl}video" else "$formattedUrl/video"
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
                                        object-fit: cover;
                                        display: block;
                                    }
                                </style>
                                </head>
                                <body>
                                    <img src="$targetUrl" onerror="this.onerror=null; this.src='$formattedUrl';" />
                                </body>
                                </html>
                            """.trimIndent()

                            loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                            webViewInstance = this
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
                update = { view ->
                    if (view is WebView) {
                        webViewInstance = view
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading Overlay
        if (isLoading && !hasError) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎥 Menghubungkan ke DroidCam...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = streamUrl,
                        color = Color(0xFFD0BCFF),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Error Diagnosis & Troubleshooting Overlay
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1B24))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.widthIn(max = 480.dp)
                ) {
                    Text(
                        text = "⚠️ Gagal Terhubung ke DroidCam HP",
                        color = Color(0xFFEF4444),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        color = Color(0xFF2D2A37),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "URL Stream: $streamUrl",
                            color = Color(0xFFD0BCFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "💡 Panduan Solusi Koneksi DroidCam:",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2D2A37), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text("1. Pastikan HP dan Perangkat ini terhubung ke WI-FI / HOTSPOT yang SAMA.", color = Color.LightGray, fontSize = 11.sp)
                        Text("2. Buka aplikasi DroidCam di HP dan tekan 'START'.", color = Color.LightGray, fontSize = 11.sp)
                        Text("3. Samakan IP Address yang tertera di layar HP (contoh: 192.168.1.50).", color = Color.LightGray, fontSize = 11.sp)
                        Text("4. Coba ubah format feed di tab Settings ke /video atau /mjpegfeed.", color = Color.LightGray, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                retryCount++
                                hasError = false
                                isLoading = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("🔄 Hubungkan Ulang", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}


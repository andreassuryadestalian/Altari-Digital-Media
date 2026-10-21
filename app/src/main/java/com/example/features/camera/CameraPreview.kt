package com.example.features.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.features.display.HeadlessCameraRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

object CameraCaptureExecutor {
    val executor = Executors.newSingleThreadExecutor()
}

/**
 * Headless camera capture engine that runs in the background of the app.
 * It continuously captures camera frames using CameraX ImageAnalysis and feeds them
 * into [CameraStreamManager] for both local Compose rendering and MJPEG network streaming.
 */
@Composable
fun CameraCaptureHost(enabled: Boolean = true) {
    val context = LocalContext.current

    if (!enabled) {
        LaunchedEffect(Unit) {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                } else {
                    withContext(Dispatchers.IO) {
                        cameraProviderFuture.get().unbindAll()
                    }
                }
                CameraStreamManager.clearCurrentFrames()
            } catch (_: Throwable) {}
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val useFrontCamera by CameraStreamManager.useFrontCamera.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            CameraStreamManager.setCameraStateMessage("Izin kamera ditolak. Berikan izin di Pengaturan.")
        }
    }

    // Auto-prompt permission ONLY when camera is actively enabled by the operator
    LaunchedEffect(hasCameraPermission, enabled) {
        if (enabled && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
                CameraStreamManager.clearCurrentFrames()
            } catch (_: Throwable) {}
        }
    }

    if (!hasCameraPermission) return

    LaunchedEffect(useFrontCamera, hasCameraPermission, enabled) {
        if (!enabled) return@LaunchedEffect
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraProvider = withContext(Dispatchers.IO) {
                cameraProviderFuture.get()
            }

            val preferredSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            val cameraSelector = when {
                cameraProvider.hasCamera(preferredSelector) -> preferredSelector
                cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                else -> null
            }

            if (cameraSelector == null) {
                CameraStreamManager.setCameraStateMessage("Tidak ada kamera fisik terdeteksi")
                return@LaunchedEffect
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setTargetResolution(android.util.Size(1280, 720))
                .build()

            imageAnalysis.setAnalyzer(CameraCaptureExecutor.executor) { imageProxy ->
                try {
                    val bitmap = imageProxy.toBitmap()
                    CameraStreamManager.onNewFrame(bitmap, imageProxy.imageInfo.rotationDegrees)
                } catch (e: Throwable) {
                    e.printStackTrace()
                } finally {
                    imageProxy.close()
                }
            }

            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageAnalysis
            )

            CameraStreamManager.setCameraStateMessage(null)

            camera.cameraInfo.cameraState.observe(lifecycleOwner) { state ->
                if (state.type == CameraState.Type.CLOSED || state.type == CameraState.Type.CLOSING) {
                    state.error?.let { err ->
                        CameraStreamManager.setCameraStateMessage("Status kamera: error ${err.code}")
                    }
                }
            }
        } catch (e: Throwable) {
            CameraStreamManager.setCameraStateMessage("Gagal memulai kamera: ${e.localizedMessage}")
        }
    }
}

/**
 * Visual Camera Composable used in settings or preview boxes.
 * Seamlessly integrates with the continuous [CameraCaptureHost] background stream.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    useFrontCamera: Boolean = false
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(useFrontCamera) {
        CameraStreamManager.setUseFrontCamera(useFrontCamera)
    }

    val cameraMsg by CameraStreamManager.cameraStateMessage.collectAsState()
    val currentFrame by CameraStreamManager.latestImageBitmap.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!hasCameraPermission) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VideocamOff,
                    contentDescription = null,
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Izin Kamera Diperlukan",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Izinkan", fontSize = 10.sp, color = Color(0xFFD0BCFF))
                }
            }
        } else if (currentFrame != null) {
            HeadlessCameraRenderer()
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFD0BCFF),
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = cameraMsg ?: "Menghubungkan Kamera...",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

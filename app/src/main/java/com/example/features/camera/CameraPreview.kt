package com.example.features.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    useFrontCamera: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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

    var cameraStateMessage by remember { mutableStateOf<String?>(null) }
    var activeCameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                activeCameraProvider?.unbindAll()
            } catch (_: Throwable) {}
            try {
                cameraExecutor.shutdown()
            } catch (_: Throwable) {}
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF1E1B2E))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideocamOff,
                    contentDescription = null,
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Izin Kamera Diperlukan",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Berikan izin kamera untuk menampilkan live feed",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Izinkan Kamera", fontSize = 11.sp, color = Color(0xFFD0BCFF))
                }
            }
        }
        return
    }

    if (cameraStateMessage != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E24))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = cameraStateMessage ?: "Kamera Tidak Tersedia",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { cameraStateMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2B38)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Muat Ulang", fontSize = 10.sp, color = Color.White)
                }
            }
        }
        return
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        activeCameraProvider = cameraProvider

                        val preferredSelector = if (useFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        // Determine available camera selector without crashing if one is missing
                        val cameraSelector = when {
                            cameraProvider.hasCamera(preferredSelector) -> preferredSelector
                            cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                            else -> null
                        }

                        if (cameraSelector == null) {
                            cameraStateMessage = "Tidak ada hardware kamera yang terdeteksi"
                            return@addListener
                        }

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
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
                            preview,
                            imageAnalysis
                        )

                        // Observe camera state to stop retry loops on fatal error or device disabled
                        camera.cameraInfo.cameraState.observe(lifecycleOwner) { state ->
                            if (state.type == CameraState.Type.CLOSED || state.type == CameraState.Type.CLOSING) {
                                val err = state.error
                                if (err != null) {
                                    when (err.code) {
                                        CameraState.ERROR_CAMERA_DISABLED,
                                        CameraState.ERROR_CAMERA_IN_USE,
                                        CameraState.ERROR_MAX_CAMERAS_IN_USE,
                                        CameraState.ERROR_CAMERA_FATAL_ERROR,
                                        CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> {
                                            try {
                                                cameraProvider.unbindAll()
                                            } catch (_: Throwable) {}
                                            cameraStateMessage = "Kamera tidak dapat diakses (${err.code})"
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        cameraStateMessage = "Kamera tidak dapat dimulai: ${e.localizedMessage ?: "Error"}"
                    }
                }, ContextCompat.getMainExecutor(ctx))
            } catch (e: Throwable) {
                cameraStateMessage = "Gagal inisialisasi kamera: ${e.localizedMessage ?: "Error"}"
            }

            previewView
        },
        modifier = modifier.fillMaxSize()
    )
}



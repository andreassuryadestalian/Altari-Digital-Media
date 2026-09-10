package com.example.features.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream

object CameraStreamManager {
    private val _latestFrame = MutableStateFlow<ByteArray?>(null)
    val latestFrame: StateFlow<ByteArray?> = _latestFrame.asStateFlow()

    private val _latestImageBitmap = MutableStateFlow<ImageBitmap?>(null)
    val latestImageBitmap: StateFlow<ImageBitmap?> = _latestImageBitmap.asStateFlow()

    private val _useFrontCamera = MutableStateFlow(false)
    val useFrontCamera: StateFlow<Boolean> = _useFrontCamera.asStateFlow()

    private val _cameraStateMessage = MutableStateFlow<String?>(null)
    val cameraStateMessage: StateFlow<String?> = _cameraStateMessage.asStateFlow()

    @Volatile
    private var lastFrameTime = 0L

    fun setUseFrontCamera(front: Boolean) {
        _useFrontCamera.value = front
    }

    fun setCameraStateMessage(msg: String?) {
        _cameraStateMessage.value = msg
    }

    fun onNewFrame(bitmap: Bitmap, rotationDegrees: Int = 0) {
        val now = System.currentTimeMillis()
        // Throttle to ~30 fps (33ms) to ensure high performance and low latency
        if (now - lastFrameTime < 33) return
        lastFrameTime = now

        try {
            val finalBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            // Expose for Compose rendering without decoding overhead
            _latestImageBitmap.value = finalBitmap.asImageBitmap()
            _cameraStateMessage.value = null

            val stream = ByteArrayOutputStream()
            // Compress JPEG at 75% quality for fast network streaming
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
            _latestFrame.value = stream.toByteArray()

            // NOTE: Do NOT call finalBitmap.recycle() here because finalBitmap.asImageBitmap()
            // is actively referenced by Compose for rendering. Android native GC reclaims bitmaps safely.
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

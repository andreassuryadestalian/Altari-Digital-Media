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

    @Volatile
    private var lastFrameTime = 0L

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

            val stream = ByteArrayOutputStream()
            // Compress JPEG at 75% quality for fast network streaming
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
            _latestFrame.value = stream.toByteArray()

            if (finalBitmap != bitmap) {
                finalBitmap.recycle()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

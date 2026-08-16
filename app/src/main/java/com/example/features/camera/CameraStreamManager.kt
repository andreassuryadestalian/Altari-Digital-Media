package com.example.features.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream

object CameraStreamManager {
    private val _latestFrame = MutableStateFlow<ByteArray?>(null)
    val latestFrame: StateFlow<ByteArray?> = _latestFrame.asStateFlow()

    @Volatile
    private var lastFrameTime = 0L

    fun onNewFrame(bitmap: Bitmap, rotationDegrees: Int = 0) {
        val now = System.currentTimeMillis()
        // Throttle to ~25 fps (40ms) to ensure high performance and low latency
        if (now - lastFrameTime < 40) return
        lastFrameTime = now

        try {
            val finalBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

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

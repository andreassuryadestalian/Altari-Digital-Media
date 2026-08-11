package com.example.features.camera

interface CameraSource {
    fun start()
    fun stop()
    fun pause()
    fun resume()
    fun getFrame(): Any // Represents a frame/texture
}

// AndroidCameraSource using CameraX would implement this
class AndroidCameraSource : CameraSource {
    override fun start() {}
    override fun stop() {}
    override fun pause() {}
    override fun resume() {}
    override fun getFrame(): Any { return Any() }
}

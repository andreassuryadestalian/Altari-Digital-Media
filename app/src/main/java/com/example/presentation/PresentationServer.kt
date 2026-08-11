package com.example.presentation

import com.example.features.lyrics.LyricsStylePreset
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PresentationServer : PresentationEngine {
    private val _state = MutableStateFlow(PresentationState())
    override val state: StateFlow<PresentationState> = _state.asStateFlow()

    override fun go(content: PresentationContent) {
        val newStatus = when (content) {
            is LyricsContent -> PresentationStatus.LYRICS
            is BibleContent -> PresentationStatus.BIBLE
            is VideoContent -> PresentationStatus.VIDEO
            is ImageContent -> PresentationStatus.IMAGE
            is CameraContent -> PresentationStatus.CAMERA
            is IpCameraContent -> PresentationStatus.IP_CAMERA
            is PowerPointContent -> PresentationStatus.POWERPOINT
        }

        _state.update {
            it.copy(
                currentContent = content,
                currentSlideIndex = 0,
                status = newStatus,
                isVideoPlaying = true
            )
        }
    }

    fun setSlideIndex(index: Int) {
        val currentContent = _state.value.currentContent
        val maxIndex = when (currentContent) {
            is LyricsContent -> currentContent.slides.size - 1
            is BibleContent -> currentContent.verses.size - 1
            is PowerPointContent -> currentContent.slides.size - 1
            else -> 0
        }
        if (maxIndex >= 0) {
            val clamped = index.coerceIn(0, maxIndex)
            _state.update { it.copy(currentSlideIndex = clamped) }
        }
    }

    override fun nextSlide() {
        val currentContent = _state.value.currentContent
        val currentIndex = _state.value.currentSlideIndex
        val maxIndex = when (currentContent) {
            is LyricsContent -> currentContent.slides.size - 1
            is BibleContent -> currentContent.verses.size - 1
            is PowerPointContent -> currentContent.slides.size - 1
            else -> 0
        }
        if (currentIndex < maxIndex) {
            _state.update { it.copy(currentSlideIndex = currentIndex + 1) }
        }
    }

    override fun previousSlide() {
        val currentIndex = _state.value.currentSlideIndex
        if (currentIndex > 0) {
            _state.update { it.copy(currentSlideIndex = currentIndex - 1) }
        }
    }

    override fun clear() {
        _state.update {
            if (it.status == PresentationStatus.CLEAR) {
                // Toggle clear off back to content
                val status = when (it.currentContent) {
                    is LyricsContent -> PresentationStatus.LYRICS
                    is BibleContent -> PresentationStatus.BIBLE
                    is VideoContent -> PresentationStatus.VIDEO
                    is ImageContent -> PresentationStatus.IMAGE
                    is CameraContent -> PresentationStatus.CAMERA
                    is IpCameraContent -> PresentationStatus.IP_CAMERA
                    is PowerPointContent -> PresentationStatus.POWERPOINT
                    null -> PresentationStatus.IDLE
                }
                it.copy(status = status)
            } else {
                it.copy(status = PresentationStatus.CLEAR)
            }
        }
    }

    override fun black() {
        _state.update {
            if (it.status == PresentationStatus.BLACK) {
                val status = when (it.currentContent) {
                    is LyricsContent -> PresentationStatus.LYRICS
                    is BibleContent -> PresentationStatus.BIBLE
                    is VideoContent -> PresentationStatus.VIDEO
                    is ImageContent -> PresentationStatus.IMAGE
                    is CameraContent -> PresentationStatus.CAMERA
                    is IpCameraContent -> PresentationStatus.IP_CAMERA
                    is PowerPointContent -> PresentationStatus.POWERPOINT
                    null -> PresentationStatus.IDLE
                }
                it.copy(status = status)
            } else {
                it.copy(status = PresentationStatus.BLACK)
            }
        }
    }

    fun setBackgroundImage(uri: String?) {
        _state.update {
            if (uri.isNullOrEmpty()) {
                it.copy(backgroundType = BackgroundType.NONE, backgroundImageUri = null)
            } else {
                it.copy(backgroundType = BackgroundType.IMAGE, backgroundImageUri = uri)
            }
        }
    }

    fun setBackgroundVideo(uri: String?) {
        _state.update {
            if (uri.isNullOrEmpty()) {
                it.copy(backgroundType = BackgroundType.NONE, backgroundVideoUri = null)
            } else if (uri.startsWith("http://") || uri.startsWith("https://")) {
                it.copy(backgroundType = BackgroundType.IP_CAMERA, backgroundVideoUri = uri)
            } else {
                it.copy(backgroundType = BackgroundType.VIDEO, backgroundVideoUri = uri)
            }
        }
    }

    fun setBackgroundIpCamera(streamUrl: String?) {
        setBackgroundVideo(streamUrl)
    }

    fun setBackgroundCamera(enabled: Boolean) {
        _state.update {
            if (enabled) {
                it.copy(backgroundType = BackgroundType.CAMERA)
            } else {
                it.copy(backgroundType = BackgroundType.NONE)
            }
        }
    }

    fun setStylePreset(preset: LyricsStylePreset) {
        _state.update { it.copy(stylePreset = preset) }
    }

    fun toggleVideoPlayback() {
        _state.update { it.copy(isVideoPlaying = !it.isVideoPlaying) }
    }

    override fun selectDisplay(displayId: String) {
        _state.update { it.copy(activeDisplayId = displayId) }
    }
}

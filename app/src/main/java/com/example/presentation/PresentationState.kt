package com.example.presentation

import com.example.features.lyrics.LyricsStylePreset
import com.example.model.PresentationContent

enum class PresentationStatus {
    IDLE, LYRICS, BIBLE, POWERPOINT, IMAGE, VIDEO, CAMERA, IP_CAMERA, BLACK, CLEAR
}

enum class BackgroundType {
    NONE, IMAGE, VIDEO, CAMERA, IP_CAMERA
}

data class PresentationState(
    val currentContent: PresentationContent? = null,
    val nextContent: PresentationContent? = null,
    val currentSlideIndex: Int = 0,
    val status: PresentationStatus = PresentationStatus.IDLE,
    val backgroundType: BackgroundType = BackgroundType.NONE,
    val backgroundImageUri: String? = null,
    val backgroundVideoUri: String? = null,
    val stylePreset: LyricsStylePreset = LyricsStylePreset.WORSHIP,
    val isVideoPlaying: Boolean = true,
    val activeDisplayId: String? = null
)

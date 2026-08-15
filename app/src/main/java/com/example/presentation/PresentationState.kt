package com.example.presentation

import androidx.compose.ui.text.style.TextAlign
import com.example.features.lyrics.LyricsStylePreset
import com.example.model.PresentationContent

enum class PresentationStatus {
    IDLE, LYRICS, BIBLE, POWERPOINT, IMAGE, VIDEO, CAMERA, IP_CAMERA, BLACK, CLEAR
}

enum class BackgroundType {
    NONE, IMAGE, VIDEO, CAMERA, IP_CAMERA
}

enum class TextDisplayPosition(val label: String) {
    CENTER("Tengah Layar"),
    LOWER_THIRD("Lower Third (Bawah)"),
    BOTTOM_CENTER("Bawah Tengah"),
    TOP_BANNER("Atas Layar"),
    LEFT_CENTER("Kiri Tengah")
}

enum class TextAlignmentOption(val label: String, val value: TextAlign) {
    CENTER("Rata Tengah", TextAlign.Center),
    LEFT("Rata Kiri", TextAlign.Start),
    RIGHT("Rata Kanan", TextAlign.End)
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
    val activeDisplayId: String? = null,
    val fontSizeSp: Int = 32,
    val textPosition: TextDisplayPosition = TextDisplayPosition.CENTER,
    val textColorRgb: Long = 0xFFFFFFFF,
    val textAlignment: TextAlignmentOption = TextAlignmentOption.CENTER,
    val textBackgroundAlpha: Float = 0.35f,
    val isTextBold: Boolean = true,
    val isTextShadowEnabled: Boolean = true
)


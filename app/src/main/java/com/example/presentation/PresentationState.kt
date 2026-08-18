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
    TOP_BANNER("Banner Atas"),
    LEFT_CENTER("Kiri Tengah"),
    RIGHT_CENTER("Kanan Tengah"),
    TOP_LEFT("Kiri Atas"),
    TOP_RIGHT("Kanan Atas"),
    BOTTOM_LEFT("Kiri Bawah"),
    BOTTOM_RIGHT("Kanan Bawah"),
    CUSTOM("Posisi Kustom (Slider)")
}

enum class TextAlignmentOption(val label: String, val value: TextAlign) {
    CENTER("Rata Tengah", TextAlign.Center),
    LEFT("Rata Kiri", TextAlign.Start),
    RIGHT("Rata Kanan", TextAlign.End)
}

enum class SplitScreenSide(val label: String) {
    CAM_LEFT_CONTENT_RIGHT("Kamera Kiri (30%) | Materi Kanan (70%)"),
    CONTENT_LEFT_CAM_RIGHT("Materi Kiri (70%) | Kamera Kanan (30%)")
}

enum class LyricsDisplayMode(val label: String, val shortLabel: String, val description: String) {
    PER_BAIT("Per Bait (Stanza / Verse)", "Per Bait", "Menampilkan 1 bait penuh (verse / chorus) per slide"),
    PER_BARIS("Per Baris (Line by Line)", "Per Baris", "Memecah lirik menjadi 1 baris per slide (ideal untuk Lower Third)")
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
    
    // Lyrics Presentation Mode (Per Bait / Per Baris)
    val lyricsDisplayMode: LyricsDisplayMode = LyricsDisplayMode.PER_BAIT,
    
    // Live Typography & Flexible Text Positioning
    val fontSizeSp: Int = 32,
    val textPosition: TextDisplayPosition = TextDisplayPosition.CENTER,
    val textColorRgb: Long = 0xFFFFFFFF,
    val textAlignment: TextAlignmentOption = TextAlignmentOption.CENTER,
    val textBackgroundAlpha: Float = 0.35f,
    val isTextBold: Boolean = true,
    val isTextShadowEnabled: Boolean = true,
    
    // Advanced Flexible Text Placement
    val textVerticalPercent: Int = 50, // 0% (top) to 100% (bottom)
    val textHorizontalPercent: Int = 50, // 0% (left) to 100% (right)
    val textBoxWidthPercent: Int = 90, // 30% to 100%
    val textBoxCornerRadiusDp: Int = 12,
    val textBoxPaddingDp: Int = 16,
    val textLineHeightMultiplier: Float = 1.35f,
    val isTextUppercase: Boolean = false,
    val textBoxBorderEnabled: Boolean = false,

    // Split Screen Feature (2 Screen Live Web / Live Presentation)
    val isSplitScreenEnabled: Boolean = false,
    val splitRatioCamPercent: Int = 30, // Default 30% Live Cam : 70% Sermon
    val splitScreenSide: SplitScreenSide = SplitScreenSide.CAM_LEFT_CONTENT_RIGHT,
    val splitCameraStreamUrl: String? = null,
    val splitCameraSourceType: BackgroundType = BackgroundType.IP_CAMERA
)



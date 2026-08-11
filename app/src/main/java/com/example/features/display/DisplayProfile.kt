package com.example.features.display

enum class DisplayProfileType(val label: String, val description: String) {
    MAIN_PROJECTOR("Main Projector", "Full graphics, video background & lyrics"),
    STAGE_MONITOR("Stage Confidence Monitor", "High-contrast text + Next Slide preview for worship team"),
    FOYER_SCREEN("Foyer / Lobby TV", "Info slides, announcements & church banner"),
    LOWER_THIRD("Lower Third Livestream", "Clean lower-third overlay for video mix")
}

data class DisplayProfile(
    val id: String,
    val name: String,
    val profileType: DisplayProfileType,
    val displayId: Int,
    val isEnabled: Boolean = true
)

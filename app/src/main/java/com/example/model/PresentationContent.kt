package com.example.model

import com.example.presentation.LyricsDisplayMode

sealed interface PresentationContent {
    val id: String
    val title: String
}

data class LyricsContent(
    override val id: String,
    override val title: String,
    val slides: List<String>
) : PresentationContent {

    /**
     * Returns effective slides based on display mode (PER_BAIT or PER_BARIS).
     */
    fun getEffectiveSlides(mode: LyricsDisplayMode): List<String> {
        return when (mode) {
            LyricsDisplayMode.PER_BAIT -> if (slides.isEmpty()) listOf("") else slides
            LyricsDisplayMode.PER_BARIS -> getLineByLineSlides()
        }
    }

    /**
     * Splits lyric verses into individual line slides, excluding section headers like [Chorus] or Verse 1.
     */
    fun getLineByLineSlides(): List<String> {
        val result = mutableListOf<String>()
        for (slide in slides) {
            val lines = slide.lines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isNotBlank() && !isSectionHeaderOnly(trimmed)) {
                    result.add(trimmed)
                }
            }
        }
        return if (result.isEmpty()) (if (slides.isEmpty()) listOf("") else slides) else result
    }

    private fun isSectionHeaderOnly(line: String): Boolean {
        val upper = line.uppercase().trim()
        if (upper.startsWith("[") && upper.endsWith("]")) return true
        val headerKeywords = listOf("VERSE", "CHORUS", "BRIDGE", "INTRO", "OUTRO", "REFRAIN", "PRE-CHORUS", "TAG", "ENDING", "BAIT", "REFF")
        return headerKeywords.any { upper == it || upper.matches(Regex("^(VERSE|BAIT|CHORUS|REFF|BRIDGE)\\s*\\d*$", RegexOption.IGNORE_CASE)) }
    }
}

data class ImageContent(
    override val id: String,
    override val title: String,
    val uri: String
) : PresentationContent

data class VideoContent(
    override val id: String,
    override val title: String,
    val uri: String
) : PresentationContent

data class PowerPointContent(
    override val id: String,
    override val title: String,
    val slides: List<String> // URIs to converted slide images
) : PresentationContent

data class BibleContent(
    override val id: String,
    override val title: String, // e.g. "John 3:16-17 (NIV)"
    val bookAndChapter: String, // e.g. "John 3"
    val verses: List<String> // e.g. ["16 For God so loved the world...", "17 For God did not send..."]
) : PresentationContent

data class CameraContent(
    override val id: String,
    override val title: String,
    val cameraId: String
) : PresentationContent

data class IpCameraContent(
    override val id: String,
    override val title: String, // e.g. "DroidCam HP (192.168.1.50)"
    val streamUrl: String       // e.g. "http://192.168.1.50:4747/video"
) : PresentationContent

package com.example.features.lyrics

object LyricsParser {

    /**
     * Parses raw lyrics text into a list of slide strings.
     * Looks for section markers (VERSE, CHORUS, etc.) or double line breaks.
     */
    fun parse(rawText: String): List<String> {
        if (rawText.isBlank()) return emptyList()

        val normalized = rawText.replace("\r\n", "\n").trim()
        
        // Check if there are explicit section headers like VERSE, CHORUS, [VERSE], etc.
        val lines = normalized.lines()
        val slides = mutableListOf<String>()
        var currentChunk = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()
            val isHeader = isSectionHeader(trimmed)

            if (isHeader) {
                if (currentChunk.isNotBlank()) {
                    slides.add(currentChunk.toString().trim())
                    currentChunk = StringBuilder()
                }
                currentChunk.append(trimmed).append("\n")
            } else if (trimmed.isEmpty()) {
                if (currentChunk.isNotBlank()) {
                    slides.add(currentChunk.toString().trim())
                    currentChunk = StringBuilder()
                }
            } else {
                currentChunk.append(line).append("\n")
            }
        }

        if (currentChunk.isNotBlank()) {
            slides.add(currentChunk.toString().trim())
        }

        // Filter out empty slides
        return slides.filter { it.isNotBlank() }
    }

    private fun isSectionHeader(line: String): Boolean {
        val upper = line.uppercase().trim()
        if (upper.startsWith("[") && upper.endsWith("]")) return true
        val keywords = listOf("VERSE", "CHORUS", "BRIDGE", "INTRO", "OUTRO", "REFRAIN", "PRE-CHORUS", "TAG", "ENDING")
        return keywords.any { upper.startsWith(it) }
    }
}

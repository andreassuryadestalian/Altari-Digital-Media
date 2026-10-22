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

    /**
     * Parses a text file that may contain multiple songs separated by delimiters
     * like "===", "---", "[SONG]", or lines starting with "Judul:" / "Title:".
     */
    fun parseMultiSongTxt(rawText: String, defaultTitle: String): List<com.example.model.LyricsContent> {
        val normalized = rawText.replace("\r\n", "\n").trim()
        if (normalized.isBlank()) return emptyList()

        val delimiterRegex = Regex("(?m)^[=\\-]{3,}\\s*$|(?m)^\\[(?:SONG|LAGU)\\]\\s*$")
        val splits = normalized.split(delimiterRegex).map { it.trim() }.filter { it.isNotBlank() }

        if (splits.size > 1) {
            val results = mutableListOf<com.example.model.LyricsContent>()
            splits.forEachIndexed { index, songBlock ->
                val lines = songBlock.lines()
                var title = ""
                val bodyLines = mutableListOf<String>()

                for (line in lines) {
                    val trimmed = line.trim()
                    if (title.isEmpty() && (trimmed.startsWith("Title:", ignoreCase = true) ||
                                trimmed.startsWith("Judul:", ignoreCase = true))) {
                        title = trimmed.substringAfter(':').trim()
                    } else if (title.isEmpty() && trimmed.startsWith("# ")) {
                        title = trimmed.removePrefix("# ").trim()
                    } else {
                        bodyLines.add(line)
                    }
                }

                if (title.isEmpty()) {
                    title = lines.firstOrNull { it.isNotBlank() }?.trim() ?: "Lagu ${index + 1}"
                }

                val slides = parse(bodyLines.joinToString("\n"))
                results.add(
                    com.example.model.LyricsContent(
                        id = "${System.currentTimeMillis()}_$index",
                        title = title,
                        slides = if (slides.isEmpty()) listOf(songBlock) else slides
                    )
                )
            }
            return results
        }

        // Single song
        val lines = normalized.lines()
        var extractedTitle = defaultTitle
        val bodyLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (extractedTitle == defaultTitle && (trimmed.startsWith("Title:", ignoreCase = true) ||
                        trimmed.startsWith("Judul:", ignoreCase = true))) {
                extractedTitle = trimmed.substringAfter(':').trim()
            } else if (extractedTitle == defaultTitle && trimmed.startsWith("# ")) {
                extractedTitle = trimmed.removePrefix("# ").trim()
            } else {
                bodyLines.add(line)
            }
        }

        val slides = parse(bodyLines.joinToString("\n"))
        return listOf(
            com.example.model.LyricsContent(
                id = System.currentTimeMillis().toString(),
                title = extractedTitle,
                slides = if (slides.isEmpty()) listOf(normalized) else slides
            )
        )
    }

    /**
     * Parses JSON string into a list of LyricsContent objects.
     * Supports:
     * - JSON Array of songs: [ { "title": "...", "slides": [...] }, ... ]
     * - JSON Object with songs list: { "songs": [ ... ] }
     * - Single song JSON Object: { "title": "...", "slides": [...] }
     */
    fun parseJsonSongs(jsonString: String, fallbackTitle: String): List<com.example.model.LyricsContent> {
        val trimmed = jsonString.trim()
        val results = mutableListOf<com.example.model.LyricsContent>()

        try {
            if (trimmed.startsWith("[")) {
                val array = org.json.JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    parseSingleJsonSong(item, "Lagu ${i + 1}")?.let { results.add(it) }
                }
            } else if (trimmed.startsWith("{")) {
                val obj = org.json.JSONObject(trimmed)
                when {
                    obj.has("songs") -> {
                        val array = obj.getJSONArray("songs")
                        for (i in 0 until array.length()) {
                            val item = array.optJSONObject(i) ?: continue
                            parseSingleJsonSong(item, "Lagu ${i + 1}")?.let { results.add(it) }
                        }
                    }
                    obj.has("items") -> {
                        val array = obj.getJSONArray("items")
                        for (i in 0 until array.length()) {
                            val item = array.optJSONObject(i) ?: continue
                            parseSingleJsonSong(item, "Lagu ${i + 1}")?.let { results.add(it) }
                        }
                    }
                    else -> {
                        parseSingleJsonSong(obj, fallbackTitle)?.let { results.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results
    }

    private fun parseSingleJsonSong(obj: org.json.JSONObject, defaultTitle: String): com.example.model.LyricsContent? {
        val title = obj.optString("title").ifBlank {
            obj.optString("name").ifBlank {
                obj.optString("judul", defaultTitle)
            }
        }

        val slides = mutableListOf<String>()

        // 1. Check array of slides/verses/stanzas
        val arrayKey = when {
            obj.has("slides") -> "slides"
            obj.has("verses") -> "verses"
            obj.has("stanzas") -> "stanzas"
            obj.has("lyrics_slides") -> "lyrics_slides"
            else -> null
        }

        if (arrayKey != null) {
            val arr = obj.optJSONArray(arrayKey)
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.opt(i)
                    when (item) {
                        is String -> if (item.isNotBlank()) slides.add(item.trim())
                        is org.json.JSONObject -> {
                            val text = item.optString("text").ifBlank {
                                item.optString("content").ifBlank {
                                    item.optString("lyrics", "")
                                }
                            }
                            if (text.isNotBlank()) slides.add(text.trim())
                        }
                    }
                }
            }
        }

        // 2. Check single string of lyrics if slides array was empty
        if (slides.isEmpty()) {
            val rawLyrics = obj.optString("lyrics").ifBlank {
                obj.optString("content").ifBlank {
                    obj.optString("text", "")
                }
            }
            if (rawLyrics.isNotBlank()) {
                slides.addAll(parse(rawLyrics))
            }
        }

        if (slides.isEmpty()) {
            slides.add("Tanpa lirik")
        }

        return com.example.model.LyricsContent(
            id = "${System.currentTimeMillis()}_${(100..999).random()}",
            title = title,
            slides = slides
        )
    }
}

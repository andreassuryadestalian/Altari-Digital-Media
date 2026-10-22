package com.example.features.lyrics

import android.util.Log
import com.example.model.LyricsContent
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiLyricSearch"

data class GeminiSongResult(
    val title: String,
    val artist: String = "",
    val songwriter: String = "",
    val album: String = "",
    val releaseYear: String = "",
    val slides: List<String>
) {
    fun toLyricsContent(): LyricsContent {
        val fullTitle = buildString {
            append(title.trim())
            if (artist.isNotBlank() && !title.contains(artist, ignoreCase = true)) {
                append(" - ").append(artist.trim())
            }
        }
        return LyricsContent(
            id = "ai_${System.currentTimeMillis()}_${(100..999).random()}",
            title = fullTitle,
            slides = if (slides.isEmpty()) listOf("Lirik tidak tersedia") else slides
        )
    }
}

sealed interface SearchAiState {
    data object Idle : SearchAiState
    data object Loading : SearchAiState
    data class Success(val results: List<GeminiSongResult>) : SearchAiState
    data class Error(val message: String) : SearchAiState
}

class GeminiLyricSearchService {

    private val searchSchema = Schema.obj(
        properties = mapOf(
            "songs" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(description = "Judul lagu"),
                        "artist" to Schema.string(description = "Penyanyi atau artis lagu", nullable = true),
                        "songwriter" to Schema.string(description = "Pencipta atau penulis lirik lagu", nullable = true),
                        "album" to Schema.string(description = "Nama album lagu jika diketahui", nullable = true),
                        "releaseYear" to Schema.string(description = "Tahun rilis atau era rilis lagu", nullable = true),
                        "slides" to Schema.array(
                            items = Schema.string(description = "Satu bait lirik (Verse/Chorus/Bridge) per slide proyektor"),
                            description = "Daftar bait lirik lengkap per slide"
                        )
                    ),
                    optionalProperties = listOf("artist", "songwriter", "album", "releaseYear")
                ),
                description = "Daftar lagu yang sesuai dengan pencarian"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = searchSchema
            }
        )
    }

    suspend fun searchSong(
        title: String,
        artist: String = "",
        songwriter: String = "",
        album: String = "",
        releaseYear: String = ""
    ): List<GeminiSongResult> {
        val prompt = buildString {
            appendLine("Anda adalah asisten pencari lirik lagu ibadah gereja dan rohani Kristen/Katolik/hymn/praise&worship serta lagu umum.")
            appendLine("Tugas Anda: Cari lirik lagu lengkap berdasarkan identitas berikut:")
            if (title.isNotBlank()) appendLine("- Judul Lagu: $title")
            if (artist.isNotBlank()) appendLine("- Penyanyi / Artis: $artist")
            if (songwriter.isNotBlank()) appendLine("- Pencipta / Komposer: $songwriter")
            if (album.isNotBlank()) appendLine("- Album: $album")
            if (releaseYear.isNotBlank()) appendLine("- Perilis / Tahun: $releaseYear")
            appendLine()
            appendLine("Panduan Penulisan Lirik:")
            appendLine("1. Berikan lirik lengkap dari bait pertama hingga selesai (Verse 1, Verse 2, Reff / Chorus, Bridge, Ending) tanpa disingkat.")
            appendLine("2. Setiap elemen dalam array 'slides' harus berisi satu bait yang rapi (2-6 baris) yang siap ditayangkan langsung di proyektor LCD ibadah.")
            appendLine("3. Jika ada variasi lagu, berikan versi yang paling akurat sesuai identitas penyanyi atau pencipta yang dimasukkan.")
        }

        val response = withTimeout(60_000L) {
            model.generateContent(prompt)
        }

        val jsonString = response.text ?: throw IllegalStateException("Model tidak mengembalikan respon teks.")
        Log.d(TAG, "Raw Gemini response: $jsonString")

        val root = JSONObject(jsonString)
        val songsArray = root.optJSONArray("songs") ?: return emptyList()
        val results = mutableListOf<GeminiSongResult>()

        for (i in 0 until songsArray.length()) {
            val item = songsArray.getJSONObject(i)
            val songTitle = item.optString("title", title)
            val songArtist = item.optString("artist", artist)
            val songSongwriter = item.optString("songwriter", songwriter)
            val songAlbum = item.optString("album", album)
            val songYear = item.optString("releaseYear", releaseYear)

            val slidesArray = item.optJSONArray("slides")
            val slides = mutableListOf<String>()
            if (slidesArray != null) {
                for (j in 0 until slidesArray.length()) {
                    val slideText = slidesArray.getString(j).trim()
                    if (slideText.isNotBlank()) {
                        slides.add(slideText)
                    }
                }
            }

            if (slides.isEmpty()) {
                slides.add("Lirik tidak tersedia")
            }

            results.add(
                GeminiSongResult(
                    title = songTitle,
                    artist = songArtist,
                    songwriter = songSongwriter,
                    album = songAlbum,
                    releaseYear = songYear,
                    slides = slides
                )
            )
        }

        return results
    }
}

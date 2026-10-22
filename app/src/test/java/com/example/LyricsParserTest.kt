package com.example

import com.example.data.PresetSongbook
import com.example.features.lyrics.LyricsParser
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LyricsParserTest {

    @Test
    fun testParseMultiSongTxtWithDelimiters() {
        val rawText = """
            Judul: Lagu Pertama
            Bait 1
            Ini baris pertama lagu satu
            
            Reff
            Ini reff lagu satu
            ===
            Judul: Lagu Kedua
            Verse 1
            Ini lagu kedua
        """.trimIndent()

        val songs = LyricsParser.parseMultiSongTxt(rawText, "Default")
        assertEquals(2, songs.size)
        assertEquals("Lagu Pertama", songs[0].title)
        assertTrue(songs[0].slides.isNotEmpty())
        assertEquals("Lagu Kedua", songs[1].title)
    }

    @Test
    fun testParseJsonArray() {
        val json = """
            [
              {
                "title": "Amazing Grace",
                "slides": [
                  "Amazing grace how sweet the sound",
                  "That saved a wretch like me"
                ]
              },
              {
                "title": "Sentuh Hatiku",
                "lyrics": "Betapa 'ku mencintai\nSegala yang t'lah Kau beri\n\nReff:\nSentuh hatiku"
              }
            ]
        """.trimIndent()

        val songs = LyricsParser.parseJsonSongs(json, "Fallback")
        assertEquals(2, songs.size)
        assertEquals("Amazing Grace", songs[0].title)
        assertEquals(2, songs[0].slides.size)
        assertEquals("Sentuh Hatiku", songs[1].title)
        assertTrue(songs[1].slides.size >= 2)
    }

    @Test
    fun testParseJsonObjectWithSongsList() {
        val json = """
            {
              "songs": [
                {
                  "title": "KJ 400 - Kudaki Jalan Mulia",
                  "verses": [
                    "Kudaki jalan mulia",
                    "Ya Tuhan angkat jiwaku"
                  ]
                }
              ]
            }
        """.trimIndent()

        val songs = LyricsParser.parseJsonSongs(json, "Fallback")
        assertEquals(1, songs.size)
        assertEquals("KJ 400 - Kudaki Jalan Mulia", songs[0].title)
        assertEquals(2, songs[0].slides.size)
    }

    @Test
    fun testPresetSongbookValidity() {
        val presets = PresetSongbook.songs
        assertTrue("Preset songs should have at least 15 songs", presets.size >= 15)
        for (song in presets) {
            assertTrue("Song id should not be blank", song.id.isNotBlank())
            assertTrue("Song title should not be blank", song.title.isNotBlank())
            assertTrue("Song category should not be blank", song.category.isNotBlank())
            assertTrue("Song slides should not be empty", song.slides.isNotEmpty())
        }
    }
}

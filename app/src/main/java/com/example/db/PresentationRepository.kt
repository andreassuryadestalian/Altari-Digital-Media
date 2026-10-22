package com.example.db

import com.example.model.BibleContent
import com.example.model.LyricsContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PresentationRepository(private val database: AppDatabase) {

    val allLyrics: Flow<List<LyricsContent>> = database.lyricDao().getAllLyrics().map { list ->
        list.map { it.toModel() }
    }

    val allBibleVerses: Flow<List<BibleContent>> = database.bibleDao().getAllBibleVerses().map { list ->
        list.map { it.toModel() }
    }

    suspend fun insertLyric(lyric: LyricsContent) {
        database.lyricDao().insertLyric(LyricEntity.fromModel(lyric))
    }

    suspend fun insertLyrics(lyrics: List<LyricsContent>) {
        database.lyricDao().insertLyrics(lyrics.map { LyricEntity.fromModel(it) })
    }

    suspend fun deleteLyric(id: String) {
        database.lyricDao().deleteLyricById(id)
    }

    suspend fun insertBibleVerse(bibleVerse: BibleContent) {
        database.bibleDao().insertBibleVerse(BibleEntity.fromModel(bibleVerse))
    }

    suspend fun deleteBibleVerse(id: String) {
        database.bibleDao().deleteBibleVerseById(id)
    }

    val allPlaylists: Flow<List<com.example.model.ServicePlaylist>> = database.playlistDao().getAllPlaylists().map { list ->
        list.map { it.toModel() }
    }

    suspend fun insertPlaylist(playlist: com.example.model.ServicePlaylist, orderIndex: Int = 0) {
        database.playlistDao().insertPlaylist(PlaylistEntity.fromModel(playlist, orderIndex))
    }

    suspend fun deletePlaylist(id: String) {
        database.playlistDao().deletePlaylistById(id)
    }
}

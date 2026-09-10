package com.example.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricDao {
    @Query("SELECT * FROM lyrics ORDER BY title ASC")
    fun getAllLyrics(): Flow<List<LyricEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyric(lyric: LyricEntity)

    @Query("DELETE FROM lyrics WHERE id = :id")
    suspend fun deleteLyricById(id: String)
}

@Dao
interface BibleDao {
    @Query("SELECT * FROM bible_verses ORDER BY title ASC")
    fun getAllBibleVerses(): Flow<List<BibleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBibleVerse(bibleVerse: BibleEntity)

    @Query("DELETE FROM bible_verses WHERE id = :id")
    suspend fun deleteBibleVerseById(id: String)
}

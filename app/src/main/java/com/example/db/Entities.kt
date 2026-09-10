package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.BibleContent
import com.example.model.LyricsContent

@Entity(tableName = "lyrics")
data class LyricEntity(
    @PrimaryKey val id: String,
    val title: String,
    val slides: List<String>
) {
    fun toModel() = LyricsContent(
        id = id,
        title = title,
        slides = slides
    )

    companion object {
        fun fromModel(model: LyricsContent) = LyricEntity(
            id = model.id,
            title = model.title,
            slides = model.slides
        )
    }
}

@Entity(tableName = "bible_verses")
data class BibleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val bookAndChapter: String,
    val verses: List<String>
) {
    fun toModel() = BibleContent(
        id = id,
        title = title,
        bookAndChapter = bookAndChapter,
        verses = verses
    )

    companion object {
        fun fromModel(model: BibleContent) = BibleEntity(
            id = model.id,
            title = model.title,
            bookAndChapter = model.bookAndChapter,
            verses = model.verses
        )
    }
}

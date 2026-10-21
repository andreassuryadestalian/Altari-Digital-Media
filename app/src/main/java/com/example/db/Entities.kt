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

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val itemsJson: String,
    val orderIndex: Int = 0
) {
    fun toModel(): com.example.model.ServicePlaylist {
        val list = androidx.compose.runtime.mutableStateListOf<com.example.model.PresentationContent>()
        list.addAll(Converters.deserializeContentList(itemsJson))
        return com.example.model.ServicePlaylist(
            id = id,
            name = name,
            items = list
        )
    }

    companion object {
        fun fromModel(model: com.example.model.ServicePlaylist, orderIndex: Int = 0) = PlaylistEntity(
            id = model.id,
            name = model.name,
            itemsJson = Converters.serializeContentList(model.items.toList()),
            orderIndex = orderIndex
        )
    }
}

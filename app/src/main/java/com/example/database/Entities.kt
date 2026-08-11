package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey val id: String,
    val type: String, // SONG, IMAGE, VIDEO, POWERPOINT
    val title: String,
    val content: String, // JSON slides for SONG/POWERPOINT, URI for IMAGE/VIDEO
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_item")
data class PlaylistItemEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val mediaId: String,
    val orderIndex: Int
)

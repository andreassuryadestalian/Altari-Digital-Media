package com.example.model

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf

data class ServicePlaylist(
    val id: String,
    var name: String,
    val items: SnapshotStateList<PresentationContent> = mutableStateListOf()
)

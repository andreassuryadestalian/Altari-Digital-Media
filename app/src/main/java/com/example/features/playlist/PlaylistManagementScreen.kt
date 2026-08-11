package com.example.features.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistManagementScreen(
    playlists: List<ServicePlaylist>,
    activePlaylistId: String,
    onSelectActivePlaylist: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRemoveItem: (playlistId: String, index: Int) -> Unit,
    onMoveItem: (playlistId: String, fromIndex: Int, toIndex: Int) -> Unit,
    onSelectForPreview: (PresentationContent) -> Unit,
    onSelectForGo: (PresentationContent) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newPlaylistNameInput by remember { mutableStateOf("") }
    var renameInput by remember { mutableStateOf("") }

    val activePlaylist = playlists.find { it.id == activePlaylistId } ?: playlists.firstOrNull()

    // Dialog Tambah Playlist Baru
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Color(0xFF25232A),
            title = {
                Text("➕ Tambah Service Playlist Baru", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Contoh: Ibadah Minggu Sore, Ibadah Pemuda, Gladi Bersama, dll.", color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = newPlaylistNameInput,
                        onValueChange = { newPlaylistNameInput = it },
                        label = { Text("Nama Playlist", color = Color.Gray) },
                        placeholder = { Text("mis. Ibadah Pemuda / Youth") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD0BCFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newPlaylistNameInput.trim()
                        if (name.isNotEmpty()) {
                            onCreatePlaylist(name)
                            newPlaylistNameInput = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Buat Playlist", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    // Dialog Edit Nama Playlist
    if (showRenameDialog && activePlaylist != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = Color(0xFF25232A),
            title = {
                Text("✏️ Edit Nama Playlist", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Nama Playlist Baru", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD0BCFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newName = renameInput.trim()
                        if (newName.isNotEmpty()) {
                            onRenamePlaylist(activePlaylist.id, newName)
                            showRenameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Simpan", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SERVICE ORDER PLAYLISTS",
                    color = Color(0xFFE6E1E9),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Kelola beberapa susunan acara ibadah & playlist lagu",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("➕ Playlist Baru", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Multi-Playlist Tabs Selector Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(playlists) { playlist ->
                val isSelected = playlist.id == activePlaylist?.id
                Surface(
                    color = if (isSelected) Color(0xFF381E72) else Color(0xFF25232A),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F)
                    ),
                    modifier = Modifier.clickable { onSelectActivePlaylist(playlist.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "📋 ${playlist.name}",
                            color = if (isSelected) Color.White else Color.LightGray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )

                        Surface(
                            color = if (isSelected) Color(0xFF10B981) else Color(0xFF49454F),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${playlist.items.size}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activePlaylist == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF25232A), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Tidak ada playlist. Klik '+ Playlist Baru' untuk membuat.", color = Color.Gray)
            }
        } else {
            // Active Playlist Toolbar Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF25232A)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFF10B981),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ACTIVE CONSOLE PLAYLIST",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = activePlaylist.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                renameInput = activePlaylist.name
                                showRenameDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49454F)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("✏️ Edit Nama", fontSize = 11.sp, color = Color.White)
                        }

                        if (playlists.size > 1) {
                            Button(
                                onClick = { onDeletePlaylist(activePlaylist.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("🗑️ Hapus", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val playlistItems = activePlaylist.items

            if (playlistItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF25232A), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Playlist ini masih kosong.", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tambahkan lagu dari tab SONGS, ayat dari tab BIBLE, atau video/kamera dari tab MEDIA.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(playlistItems) { index, item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF25232A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(Color(0xFF381E72), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (index + 1 < 10) "0${index + 1}" else "${index + 1}",
                                            color = Color(0xFFD0BCFF),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val typeLabel = when (item) {
                                            is LyricsContent -> "🎵 Song • Lyrics"
                                            is VideoContent -> "🎥 Media • Video"
                                            is ImageContent -> "🖼️ Media • Image"
                                            is BibleContent -> "📖 Scripture • Bible"
                                            is CameraContent -> "📷 Hardware Camera"
                                            is IpCameraContent -> "📱 DroidCam / IP Stream"
                                            is PowerPointContent -> "📊 Presentation Slides"
                                        }
                                        Text(typeLabel, color = Color.Gray, fontSize = 11.sp)
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Move Up / Down
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                onMoveItem(activePlaylist.id, index, index - 1)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("⬆️", fontSize = 12.sp, color = if (index > 0) Color.White else Color.DarkGray)
                                    }

                                    IconButton(
                                        onClick = {
                                            if (index < playlistItems.size - 1) {
                                                onMoveItem(activePlaylist.id, index, index + 1)
                                            }
                                        },
                                        enabled = index < playlistItems.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("⬇️", fontSize = 12.sp, color = if (index < playlistItems.size - 1) Color.White else Color.DarkGray)
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Button(
                                        onClick = { onSelectForPreview(item) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49454F)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Preview", color = Color.White, fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = { onSelectForGo(item) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("GO LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }

                                    IconButton(
                                        onClick = { onRemoveItem(activePlaylist.id, index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("✕", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

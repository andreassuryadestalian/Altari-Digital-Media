package com.example.features.lyrics

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LyricsContent
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsManagementScreen(
    songsList: List<LyricsContent>,
    onAddSong: (LyricsContent) -> Unit,
    onSelectForPreview: (LyricsContent) -> Unit,
    onSelectForGo: (LyricsContent) -> Unit,
    onAddToPlaylist: ((LyricsContent) -> Unit)? = null
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingSong by remember { mutableStateOf<LyricsContent?>(null) }

    // TXT File Picker
    val txtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                inputStream?.close()

                val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Imported Song"
                val slides = LyricsParser.parse(content)
                val newSong = LyricsContent(
                    id = System.currentTimeMillis().toString(),
                    title = fileName,
                    slides = if (slides.isEmpty()) listOf(content) else slides
                )
                onAddSong(newSong)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val filteredSongs = remember(searchQuery, songsList) {
        if (searchQuery.isBlank()) songsList
        else songsList.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LYRICS LIBRARY",
                color = Color(0xFFE6E1E9),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { txtLauncher.launch("text/plain") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72))
                ) {
                    Text("Import TXT", color = Color(0xFFD0BCFF))
                }

                Button(
                    onClick = {
                        editingSong = LyricsContent(
                            id = System.currentTimeMillis().toString(),
                            title = "New Song",
                            slides = listOf("Verse 1\nEnter lyrics here...")
                        )
                        showEditDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF))
                ) {
                    Text("+ Add Song", color = Color(0xFF381E72), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search songs...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFD0BCFF),
                unfocusedBorderColor = Color(0xFF49454F),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Songs List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredSongs) { song ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF25232A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${song.slides.size} Slides",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            onAddToPlaylist?.let { addFn ->
                                Button(
                                    onClick = { addFn(song) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72))
                                ) {
                                    Text("+ Playlist", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    editingSong = song
                                    showEditDialog = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD0BCFF))
                            ) {
                                Text("Edit")
                            }

                            Button(
                                onClick = { onSelectForPreview(song) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49454F))
                            ) {
                                Text("Preview", color = Color.White)
                            }

                            Button(
                                onClick = { onSelectForGo(song) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("GO LIVE", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Song Dialog
    if (showEditDialog && editingSong != null) {
        val song = editingSong!!
        var title by remember { mutableStateOf(song.title) }
        var rawLyrics by remember { mutableStateOf(song.slides.joinToString("\n\n")) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Song Lyrics", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = rawLyrics,
                        onValueChange = { rawLyrics = it },
                        label = { Text("Lyrics (Sections separated by blank lines)", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedSlides = LyricsParser.parse(rawLyrics)
                        val updated = song.copy(
                            title = title,
                            slides = if (parsedSlides.isEmpty()) listOf(rawLyrics) else parsedSlides
                        )
                        onAddSong(updated)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF))
                ) {
                    Text("Save", color = Color(0xFF381E72), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF2B2930)
        )
    }
}

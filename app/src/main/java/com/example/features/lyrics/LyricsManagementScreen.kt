package com.example.features.lyrics

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.db.PresentationRepository
import com.example.model.LyricsContent
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsManagementScreen(
    repository: PresentationRepository,
    songsList: List<LyricsContent>,
    onAddSong: (LyricsContent) -> Unit,
    onDeleteSong: ((LyricsContent) -> Unit)? = null,
    onSelectForPreview: (LyricsContent) -> Unit,
    onSelectForGo: (LyricsContent) -> Unit,
    onAddToPlaylist: ((LyricsContent) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingSong by remember { mutableStateOf<LyricsContent?>(null) }
    var songToDelete by remember { mutableStateOf<LyricsContent?>(null) }
    var showPresetSongbookDialog by remember { mutableStateOf(false) }
    var showGeminiSearchDialog by remember { mutableStateOf(false) }

    // Multi-format File Picker (.txt & .json)
    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                inputStream?.close()

                val rawName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Lagu"
                val isJson = rawName.endsWith(".json", ignoreCase = true) ||
                        content.trimStart().startsWith("{") ||
                        content.trimStart().startsWith("[")

                val baseTitle = if (rawName.contains('.')) rawName.substringBeforeLast('.') else rawName
                val importedSongs: List<LyricsContent> = if (isJson) {
                    LyricsParser.parseJsonSongs(content, baseTitle)
                } else {
                    LyricsParser.parseMultiSongTxt(content, baseTitle)
                }

                if (importedSongs.isNotEmpty()) {
                    coroutineScope.launch {
                        repository.insertLyrics(importedSongs)
                        val formatTag = if (isJson) "JSON" else "TXT"
                        Toast.makeText(
                            context,
                            "✅ Berhasil mengimpor ${importedSongs.size} lagu ($formatTag)!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(context, "File kosong atau format tidak didukung", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Gagal mengimpor file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val filteredSongs = remember(searchQuery, songsList) {
        if (searchQuery.isBlank()) songsList
        else songsList.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    var showSmartPasteDialog by remember { mutableStateOf(false) }

    val isCompact = LocalConfiguration.current.screenWidthDp < 600

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .padding(16.dp)
    ) {
        if (isCompact) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "LYRICS LIBRARY",
                    color = Color(0xFFE6E1E9),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showGeminiSearchDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                    ) {
                        Text("✨ Cari Lirik AI", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showPresetSongbookDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text("📚 Preset Songbook", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            fileImportLauncher.launch(
                                arrayOf("text/plain", "application/json", "application/octet-stream", "*/*")
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72))
                    ) {
                        Text("📥 Import TXT / JSON", color = Color(0xFFD0BCFF))
                    }

                    Button(
                        onClick = { showSmartPasteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                    ) {
                        Text("📝 Smart Paste", color = Color.White, fontWeight = FontWeight.Bold)
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
        } else {
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
                        onClick = { showGeminiSearchDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                    ) {
                        Text("✨ Cari Lirik AI", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showPresetSongbookDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text("📚 Preset Songbook", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            fileImportLauncher.launch(
                                arrayOf("text/plain", "application/json", "application/octet-stream", "*/*")
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72))
                    ) {
                        Text("📥 Import TXT / JSON", color = Color(0xFFD0BCFF))
                    }

                    Button(
                        onClick = { showSmartPasteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                    ) {
                        Text("📝 Smart Paste", color = Color.White, fontWeight = FontWeight.Bold)
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
                    if (isCompact) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${song.slides.size} Slides",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }

                                Button(
                                    onClick = { onSelectForGo(song) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("GO LIVE ▶", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                onAddToPlaylist?.let { addFn ->
                                    Button(
                                        onClick = { addFn(song) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("+ Playlist", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        editingSong = song
                                        showEditDialog = true
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD0BCFF)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Edit", fontSize = 11.sp)
                                }

                                onDeleteSong?.let { delFn ->
                                    OutlinedButton(
                                        onClick = { songToDelete = song },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Hapus", color = Color(0xFFEF4444), fontSize = 11.sp)
                                    }
                                }

                                Button(
                                    onClick = { onSelectForPreview(song) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49454F)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Preview", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
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

                                onDeleteSong?.let { delFn ->
                                    OutlinedButton(
                                        onClick = { songToDelete = song },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                                    ) {
                                        Text("Hapus", color = Color(0xFFEF4444))
                                    }
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

    // Delete Confirmation Dialog
    if (songToDelete != null) {
        val song = songToDelete!!
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = { Text("Hapus Lagu", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus lagu \"${song.title}\" dari database offline?", color = Color(0xFFD4D4D8)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSong?.invoke(song)
                        songToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Hapus Permanen", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) {
                    Text("Batal", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF2B2930)
        )
    }

    // Smart Paste Dialog
    if (showSmartPasteDialog) {
        var title by remember { mutableStateOf("") }
        var rawLyrics by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSmartPasteDialog = false },
            title = { Text("📝 Smart Paste Lagu", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Paste seluruh teks lagu dari Google atau dokumen lain. Sistem akan otomatis memecahnya per bait (dengan mendeteksi baris kosong atau kata kunci Verse/Chorus).",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Judul Lagu", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = rawLyrics,
                        onValueChange = { rawLyrics = it },
                        label = { Text("Lirik (Paste di sini)", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
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
                        val newSong = LyricsContent(
                            id = System.currentTimeMillis().toString(),
                            title = title.ifBlank { "Untitled Song" },
                            slides = if (parsedSlides.isEmpty()) listOf(rawLyrics) else parsedSlides
                        )
                        onAddSong(newSong)
                        showSmartPasteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                ) {
                    Text("Auto-Split & Simpan", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmartPasteDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            },
        )
    }

    // Preset Songbook Dialog
    if (showPresetSongbookDialog) {
        PresetSongbookDialog(
            existingSongs = songsList,
            onDismissRequest = { showPresetSongbookDialog = false },
            onImportSingleSong = { song ->
                coroutineScope.launch {
                    repository.insertLyric(song)
                }
            },
            onImportMultipleSongs = { songs ->
                coroutineScope.launch {
                    repository.insertLyrics(songs)
                }
            },
            onSelectForPreview = { song ->
                onSelectForPreview(song)
                showPresetSongbookDialog = false
            },
            onSelectForGo = { song ->
                onSelectForGo(song)
                showPresetSongbookDialog = false
            }
        )
    }

    // Gemini AI Lyric Search Dialog
    if (showGeminiSearchDialog) {
        GeminiLyricSearchDialog(
            existingSongs = songsList,
            onDismissRequest = { showGeminiSearchDialog = false },
            onSaveToLibrary = { song ->
                coroutineScope.launch {
                    repository.insertLyric(song)
                }
            },
            onSelectForPreview = { song ->
                onSelectForPreview(song)
                showGeminiSearchDialog = false
            },
            onSelectForGo = { song ->
                onSelectForGo(song)
                showGeminiSearchDialog = false
            }
        )
    }
}

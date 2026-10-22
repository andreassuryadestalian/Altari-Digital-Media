package com.example.features.lyrics

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.LyricsContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiLyricSearchDialog(
    existingSongs: List<LyricsContent>,
    onDismissRequest: () -> Unit,
    onSaveToLibrary: (LyricsContent) -> Unit,
    onSelectForPreview: (LyricsContent) -> Unit,
    onSelectForGo: (LyricsContent) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val searchService = remember { GeminiLyricSearchService() }

    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var songwriter by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var releaseYear by remember { mutableStateOf("") }

    var searchState by remember { mutableStateOf<SearchAiState>(SearchAiState.Idle) }
    var expandedSongIndex by remember { mutableIntStateOf(0) }

    fun performSearch() {
        if (title.isBlank()) {
            Toast.makeText(context, "Silakan isi judul lagu terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        coroutineScope.launch {
            searchState = SearchAiState.Loading
            try {
                val results = searchService.searchSong(
                    title = title,
                    artist = artist,
                    songwriter = songwriter,
                    album = album,
                    releaseYear = releaseYear
                )
                if (results.isEmpty()) {
                    searchState = SearchAiState.Error("Gemini tidak menemukan lirik untuk lagu ini. Coba periksa kembali judul atau nama artis.")
                } else {
                    searchState = SearchAiState.Success(results)
                }
            } catch (e: TimeoutCancellationException) {
                searchState = SearchAiState.Error("Pencarian lirik melebihi batas waktu (timeout). Periksa koneksi internet Anda.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                searchState = SearchAiState.Error(e.localizedMessage ?: "Terjadi kesalahan saat mencari lirik.")
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E24)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with AI Gradient Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✨ Cari Lirik dengan AI Gemini",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF6D28D9),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Gemini 3.8",
                                    color = Color(0xFFE9D5FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Ketik identitas lagu. AI akan mencari lirik lengkap & membaginya menjadi slide siap proyektor untuk disimpan ke pustaka offline.",
                            color = Color(0xFFA1A1AA),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    IconButton(onClick = onDismissRequest) {
                        Text("✕", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Identity Input Form Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF26262E)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Identitas Lagu:",
                                color = Color(0xFFE4E4E7),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Title (Required)
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Judul Lagu * (Wajib)", color = Color(0xFFA1A1AA)) },
                                placeholder = { Text("contoh: Sentuh Hatiku, Kudaki Jalan Mulia...", color = Color.DarkGray) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF8B5CF6),
                                    unfocusedBorderColor = Color(0xFF3F3F46),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            // Artist & Songwriter
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = artist,
                                    onValueChange = { artist = it },
                                    label = { Text("Penyanyi / Artis", color = Color(0xFFA1A1AA)) },
                                    placeholder = { Text("contoh: Sari Simorangkir", color = Color.DarkGray) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF8B5CF6),
                                        unfocusedBorderColor = Color(0xFF3F3F46),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                OutlinedTextField(
                                    value = songwriter,
                                    onValueChange = { songwriter = it },
                                    label = { Text("Pencipta / Komposer", color = Color(0xFFA1A1AA)) },
                                    placeholder = { Text("contoh: Jonathan Prawira", color = Color.DarkGray) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF8B5CF6),
                                        unfocusedBorderColor = Color(0xFF3F3F46),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }

                            // Album & Release Year
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = album,
                                    onValueChange = { album = it },
                                    label = { Text("Album", color = Color(0xFFA1A1AA)) },
                                    placeholder = { Text("contoh: Sentuh Hatiku", color = Color.DarkGray) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF8B5CF6),
                                        unfocusedBorderColor = Color(0xFF3F3F46),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                OutlinedTextField(
                                    value = releaseYear,
                                    onValueChange = { releaseYear = it },
                                    label = { Text("Tahun / Perilis", color = Color(0xFFA1A1AA)) },
                                    placeholder = { Text("contoh: 2008 / Maranatha", color = Color.DarkGray) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF8B5CF6),
                                        unfocusedBorderColor = Color(0xFF3F3F46),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Search Button
                            Button(
                                onClick = { performSearch() },
                                enabled = title.isNotBlank() && searchState !is SearchAiState.Loading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF7C3AED),
                                    disabledContainerColor = Color(0xFF4C1D95).copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (searchState is SearchAiState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sedang mencari lirik dengan Gemini AI...", color = Color.White, fontSize = 13.sp)
                                } else {
                                    Text(
                                        "🔍 Cari Lirik dengan Gemini AI",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Status: Error
                    if (searchState is SearchAiState.Error) {
                        val errorMsg = (searchState as SearchAiState.Error).message
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "⚠️ Gagal Mendapatkan Lirik",
                                    color = Color(0xFFFCA5A5),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = errorMsg,
                                    color = Color(0xFFFECACA),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { performSearch() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Coba Cari Lagi", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Status: Results
                    if (searchState is SearchAiState.Success) {
                        val results = (searchState as SearchAiState.Success).results
                        Text(
                            text = "🎉 Ditemukan ${results.size} Hasil Lagu dari Gemini AI:",
                            color = Color(0xFF6EE7B7),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        results.forEachIndexed { index, song ->
                            val isSaved = existingSongs.any {
                                it.title.equals(song.title, ignoreCase = true) ||
                                (song.artist.isNotBlank() && it.title.contains(song.title, ignoreCase = true) && it.title.contains(song.artist, ignoreCase = true))
                            }
                            val isExpanded = expandedSongIndex == index

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF26262E)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = if (isSaved) Color(0xFF10B981) else Color(0xFF8B5CF6).copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    // Row 1: Title, AI Badge, Saved Indicator
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = song.title,
                                                    color = Color.White,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (isSaved) {
                                                    Surface(
                                                        color = Color(0xFF065F46),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "✓ Tersimpan Offline",
                                                            color = Color(0xFF6EE7B7),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Identity metadata chips
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState())
                                                    .padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (song.artist.isNotBlank()) {
                                                    Text("🎤 ${song.artist}", color = Color(0xFF93C5FD), fontSize = 11.sp)
                                                    Text("•", color = Color.Gray, fontSize = 11.sp)
                                                }
                                                if (song.songwriter.isNotBlank()) {
                                                    Text("✍️ ${song.songwriter}", color = Color(0xFFFDE047), fontSize = 11.sp)
                                                    Text("•", color = Color.Gray, fontSize = 11.sp)
                                                }
                                                if (song.album.isNotBlank()) {
                                                    Text("💿 ${song.album}", color = Color(0xFFA7F3D0), fontSize = 11.sp)
                                                    Text("•", color = Color.Gray, fontSize = 11.sp)
                                                }
                                                if (song.releaseYear.isNotBlank()) {
                                                    Text("📅 ${song.releaseYear}", color = Color(0xFFE9D5FF), fontSize = 11.sp)
                                                    Text("•", color = Color.Gray, fontSize = 11.sp)
                                                }
                                                Text("📑 ${song.slides.size} Slide", color = Color(0xFFD4D4D8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Slide Preview
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF18181B), RoundedCornerShape(10.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Pratinjau Slide Lirik (Siap Tayang):",
                                            color = Color(0xFFA1A1AA),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        val displayedSlides = if (isExpanded) song.slides else song.slides.take(2)
                                        for ((slideIdx, slideText) in displayedSlides.withIndex()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF26262E), RoundedCornerShape(6.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Text(
                                                    text = "Slide ${slideIdx + 1}:",
                                                    color = Color(0xFF818CF8),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.width(60.dp)
                                                )
                                                Text(
                                                    text = slideText,
                                                    color = Color(0xFFE4E4E7),
                                                    fontSize = 12.sp,
                                                    lineHeight = 16.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        if (song.slides.size > 2) {
                                            TextButton(
                                                onClick = { expandedSongIndex = if (isExpanded) -1 else index },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(
                                                    text = if (isExpanded) "Sembunyikan Sebagian ▲" else "Lihat Semua ${song.slides.size} Slide ▼",
                                                    color = Color(0xFF93C5FD),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Action Buttons Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Save to offline library
                                        Button(
                                            onClick = {
                                                val content = song.toLyricsContent()
                                                onSaveToLibrary(content)
                                                Toast.makeText(
                                                    context,
                                                    "✅ '${song.title}' berhasil disimpan ke library offline!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSaved) Color(0xFF047857) else Color(0xFF10B981)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (isSaved) "✓ Simpan Lagi ke Library" else "💾 Simpan ke Library Offline",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Preview
                                        FilledTonalButton(
                                            onClick = { onSelectForPreview(song.toLyricsContent()) },
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = Color(0xFF3F3F46),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("👁️ Preview", fontSize = 12.sp)
                                        }

                                        // Go Live
                                        Button(
                                            onClick = { onSelectForGo(song.toLyricsContent()) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("▶ GO LIVE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
}

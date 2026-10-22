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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PresetSong
import com.example.data.PresetSongbook
import com.example.model.LyricsContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSongbookDialog(
    existingSongs: List<LyricsContent>,
    onDismissRequest: () -> Unit,
    onImportSingleSong: (LyricsContent) -> Unit,
    onImportMultipleSongs: (List<LyricsContent>) -> Unit,
    onSelectForPreview: (LyricsContent) -> Unit,
    onSelectForGo: (LyricsContent) -> Unit
) {
    val context = LocalContext.current
    val allPresetSongs: List<PresetSong> = remember { PresetSongbook.songs }
    val categories = listOf("Semua", "Kidung Jemaat", "NKB / PKJ", "Praise & Worship")
    var selectedCategory by remember { mutableStateOf("Semua") }
    var searchQuery by remember { mutableStateOf("") }
    var expandedSongId by remember { mutableStateOf<String?>(null) }

    val filteredSongs: List<PresetSong> = remember(selectedCategory, searchQuery) {
        allPresetSongs.filter { song ->
            val matchCategory = if (selectedCategory == "Semua") true else song.category.equals(selectedCategory, ignoreCase = true)
            val matchSearch = if (searchQuery.isBlank()) true else {
                song.title.contains(searchQuery, ignoreCase = true) ||
                song.category.contains(searchQuery, ignoreCase = true) ||
                song.slides.any { it.contains(searchQuery, ignoreCase = true) }
            }
            matchCategory && matchSearch
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
                // Header with Title & Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📚 Buku Lagu Gereja Offline",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bank lirik lagu rohani & himne bawaan siap pakai tanpa internet",
                            color = Color(0xFFA1A1AA),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = onDismissRequest) {
                        Text("✕", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari lagu (contoh: KJ 400, Sentuh Hatiku, atau lirik)...", color = Color.Gray, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF818CF8),
                        unfocusedBorderColor = Color(0xFF3F3F46),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF818CF8)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Filter Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val count = if (category == "Semua") allPresetSongs.size else allPresetSongs.count { it.category.equals(category, ignoreCase = true) }
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text("$category ($count)", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4F46E5),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF27272A),
                                labelColor = Color(0xFFD4D4D8)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bulk Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF27272A), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Menampilkan ${filteredSongs.size} lagu",
                        color = Color(0xFFD4D4D8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Button(
                        onClick = {
                            val toImport = filteredSongs.map { it.toLyricsContent() }
                            onImportMultipleSongs(toImport)
                            Toast.makeText(
                                context,
                                "✅ Berhasil mengimpor ${toImport.size} lagu ke Pustaka!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "⬇️ Impor Semua (${filteredSongs.size})",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Song List
                if (filteredSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tidak ada lagu yang sesuai dengan pencarian", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredSongs, key = { it.id }) { preset ->
                            val isSaved = existingSongs.any {
                                it.title.equals(preset.title, ignoreCase = true)
                            }
                            val isExpanded = expandedSongId == preset.id

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF26262E)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = if (isSaved) Color(0xFF059669).copy(alpha = 0.5f) else Color(0xFF3F3F46),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    // Row 1: Title, Category badge, Saved indicator
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
                                                    text = preset.title,
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (isSaved) {
                                                    Surface(
                                                        color = Color(0xFF065F46),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "✓ Tersimpan",
                                                            color = Color(0xFF6EE7B7),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                val catColor = when (preset.category) {
                                                    "Kidung Jemaat" -> Color(0xFF818CF8)
                                                    "NKB / PKJ" -> Color(0xFF2DD4BF)
                                                    else -> Color(0xFFFBBF24)
                                                }
                                                Text(
                                                    text = preset.category,
                                                    color = catColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text("•", color = Color.Gray, fontSize = 11.sp)
                                                Text("${preset.slides.size} Bait/Slide", color = Color(0xFFA1A1AA), fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Verse preview snippet or expanded verses
                                    if (!isExpanded) {
                                        val firstVerseSnippet = preset.slides.firstOrNull()?.lines()?.take(2)?.joinToString(" / ") ?: ""
                                        Text(
                                            text = "\"$firstVerseSnippet...\"",
                                            color = Color(0xFFA1A1AA),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF18181B), RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            for ((idx, slide) in preset.slides.withIndex()) {
                                                Text(
                                                    text = "Bait ${idx + 1}:",
                                                    color = Color(0xFF818CF8),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = slide,
                                                    color = Color(0xFFE4E4E7),
                                                    fontSize = 12.sp,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action buttons
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { expandedSongId = if (isExpanded) null else preset.id },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (isExpanded) "Sembunyikan ▲" else "Lihat Semua Bait ▼",
                                                color = Color(0xFF93C5FD),
                                                fontSize = 11.sp
                                            )
                                        }

                                        FilledTonalButton(
                                            onClick = { onSelectForPreview(preset.toLyricsContent()) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = Color(0xFF3F3F46),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("👁️ Preview", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = { onSelectForGo(preset.toLyricsContent()) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("▶ GO LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                onImportSingleSong(preset.toLyricsContent())
                                                Toast.makeText(
                                                    context,
                                                    "✅ '${preset.title}' ditambahkan ke Pustaka!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSaved) Color(0xFF047857) else Color(0xFF4F46E5)
                                            ),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (isSaved) "+ Impor Lagi" else "+ Simpan ke Pustaka",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
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

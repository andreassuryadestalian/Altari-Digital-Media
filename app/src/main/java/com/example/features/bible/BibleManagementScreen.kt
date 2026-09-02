package com.example.features.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.model.BibleContent
import com.example.model.PresentationContent
import com.example.api.BibleApiService
import com.example.api.KjvApiService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleManagementScreen(
    onSelectForPreview: (PresentationContent) -> Unit,
    onSelectForGo: (PresentationContent) -> Unit,
    onAddToPlaylist: ((PresentationContent) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val bibleApi = remember { BibleApiService.create() }
    val kjvApi = remember { KjvApiService.create() }

    var searchQuery by remember { mutableStateOf("") }
    
    // API Fetch states
    var inputBook by remember { mutableStateOf("Yohanes") }
    var inputChapter by remember { mutableStateOf("3") }
    var selectedVersion by remember { mutableStateOf("TB") } // TB or KJV
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var passages by remember {
        mutableStateOf(
            listOf(
                BibleContent(
                    id = "b1",
                    title = "Yohanes 3:16-17 (TB)",
                    bookAndChapter = "Yohanes 3",
                    verses = listOf(
                        "16 Karena begitu besar kasih Allah akan dunia ini, sehingga Ia telah mengaruniakan Anak-Nya yang tunggal, supaya setiap orang yang percaya kepada-Nya tidak binasa, melainkan beroleh hidup yang kekal.",
                        "17 Sebab Allah mengutus Anak-Nya ke dalam dunia bukan untuk menghakimi dunia, melainkan untuk menyelamatkannya oleh Dia."
                    )
                ),
                BibleContent(
                    id = "b2",
                    title = "John 3:16 (KJV)",
                    bookAndChapter = "John 3",
                    verses = listOf(
                        "16 For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life."
                    )
                )
            )
        )
    }

    var selectedPassage by remember { mutableStateOf<BibleContent?>(passages.firstOrNull()) }

    val filteredPassages = passages.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.verses.any { verse -> verse.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // --- API Search Box ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2A37)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔍 Ambil Ayat dari Alkitab (Otomatis)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    Row(
                        modifier = Modifier.background(Color(0xFF1C1B1F), RoundedCornerShape(8.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TB (Indo)",
                            color = if (selectedVersion == "TB") Color.White else Color.Gray,
                            fontWeight = if (selectedVersion == "TB") FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clickable { selectedVersion = "TB" }
                                .background(if (selectedVersion == "TB") Color(0xFF381E72) else Color.Transparent, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        Text(
                            text = "KJV (Eng)",
                            color = if (selectedVersion == "KJV") Color.White else Color.Gray,
                            fontWeight = if (selectedVersion == "KJV") FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clickable { selectedVersion = "KJV" }
                                .background(if (selectedVersion == "KJV") Color(0xFF381E72) else Color.Transparent, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = inputBook,
                        onValueChange = { inputBook = it },
                        label = { Text("Kitab (cth: Yohanes / John)", color = Color.Gray, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = inputChapter,
                        onValueChange = { inputChapter = it },
                        label = { Text("Pasal (cth: 3)", color = Color.Gray, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.width(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Button(
                        onClick = {
                            if (inputBook.isNotBlank() && inputChapter.isNotBlank()) {
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    try {
                                        val chapterInt = inputChapter.toIntOrNull() ?: 1
                                        if (selectedVersion == "TB") {
                                            val response = bibleApi.getChapter(inputBook, chapterInt)
                                            val data = response.data
                                            if (data?.verses != null && data.verses.isNotEmpty()) {
                                                val validVerses = data.verses.filter { it.type == "content" }
                                                val mappedVerses = validVerses.map { "${it.verse} ${it.content}" }
                                                val newContent = BibleContent(
                                                    id = "api_${System.currentTimeMillis()}",
                                                    title = "${data.book?.name ?: inputBook} ${data.book?.chapter ?: inputChapter} (TB)",
                                                    bookAndChapter = "${data.book?.name ?: inputBook} ${data.book?.chapter ?: inputChapter}",
                                                    verses = mappedVerses
                                                )
                                                passages = listOf(newContent) + passages
                                                selectedPassage = newContent
                                                searchQuery = "" // Reset search
                                            } else {
                                                errorMessage = "Ayat tidak ditemukan."
                                            }
                                        } else {
                                            // Fetch KJV
                                            val query = "$inputBook+$chapterInt"
                                            val response = kjvApi.getPassage(query)
                                            if (response.verses != null && response.verses.isNotEmpty()) {
                                                val mappedVerses = response.verses.map { "${it.verse} ${it.text.trim()}" }
                                                val ref = response.reference ?: "$inputBook $chapterInt"
                                                val newContent = BibleContent(
                                                    id = "api_${System.currentTimeMillis()}",
                                                    title = "$ref (KJV)",
                                                    bookAndChapter = ref,
                                                    verses = mappedVerses
                                                )
                                                passages = listOf(newContent) + passages
                                                selectedPassage = newContent
                                                searchQuery = ""
                                            } else {
                                                errorMessage = "Ayat tidak ditemukan."
                                            }
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Gagal mengambil data: ${e.localizedMessage}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Ambil", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(errorMessage!!, color = Color.Red, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Filter Search Bar ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari ayat yang sudah tersimpan...", color = Color.Gray, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFD0BCFF),
                unfocusedBorderColor = Color(0xFF49454F),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: Passage List
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF25232A)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "📖 Daftar Ayat Disimpan",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredPassages) { passage ->
                            val isSelected = passage.id == selectedPassage?.id
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPassage = passage }
                                    .background(
                                        if (isSelected) Color(0xFF381E72) else Color(0xFF1C1B1F),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFFD0BCFF) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = passage.title,
                                        color = if (isSelected) Color(0xFFD0BCFF) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = passage.verses.firstOrNull() ?: "",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right Column: Verses Preview & Direct Trigger
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF25232A)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
            ) {
                if (selectedPassage != null) {
                    val passage = selectedPassage!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = passage.title,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${passage.verses.size} Ayat",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                onAddToPlaylist?.let { addFn ->
                                    Button(
                                        onClick = { addFn(passage) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72))
                                    ) {
                                        Text("+ Playlist", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                Button(
                                    onClick = { onSelectForPreview(passage) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49454F))
                                ) {
                                    Text("Preview", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { onSelectForGo(passage) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Text("GO LIVE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(passage.verses) { idx, verse ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectForGo(passage)
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = verse,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Pilih atau cari ayat untuk melihat isi", color = Color.Gray)
                    }
                }
            }
        }
    }
}

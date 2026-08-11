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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleManagementScreen(
    onSelectForPreview: (PresentationContent) -> Unit,
    onSelectForGo: (PresentationContent) -> Unit,
    onAddToPlaylist: ((PresentationContent) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTranslation by remember { mutableStateOf("NIV") }

    val samplePassages = remember {
        listOf(
            BibleContent(
                id = "b1",
                title = "John 3:16-17 ($selectedTranslation)",
                bookAndChapter = "John 3",
                verses = listOf(
                    "16 For God so loved the world that he gave his one and only Son, that whoever believes in him shall not perish but have eternal life.",
                    "17 For God did not send his Son into the world to condemn the world, but to save the world through him."
                )
            ),
            BibleContent(
                id = "b2",
                title = "Psalm 23:1-6 ($selectedTranslation)",
                bookAndChapter = "Psalm 23",
                verses = listOf(
                    "1 The LORD is my shepherd, I lack nothing.",
                    "2 He makes me lie down in green pastures, he leads me beside quiet waters,",
                    "3 He refreshes my soul. He guides me along the right paths for his name's sake.",
                    "4 Even though I walk through the darkest valley, I will fear no evil, for you are with me.",
                    "5 You prepare a table before me in the presence of my enemies. You anoint my head with oil; my cup overflows.",
                    "6 Surely your goodness and love will follow me all the days of my life, and I will dwell in the house of the LORD forever."
                )
            ),
            BibleContent(
                id = "b3",
                title = "Genesis 1:1-3 ($selectedTranslation)",
                bookAndChapter = "Genesis 1",
                verses = listOf(
                    "1 In the beginning God created the heavens and the earth.",
                    "2 Now the earth was formless and empty, darkness was over the surface of the deep, and the Spirit of God was hovering over the waters.",
                    "3 And God said, 'Let there be light,' and there was light."
                )
            ),
            BibleContent(
                id = "b4",
                title = "Philippians 4:13 ($selectedTranslation)",
                bookAndChapter = "Philippians 4",
                verses = listOf(
                    "13 I can do all this through him who gives me strength."
                )
            ),
            BibleContent(
                id = "b5",
                title = "Yohanes 14:6 (TB)",
                bookAndChapter = "Yohanes 14",
                verses = listOf(
                    "6 Kata Yesus kepadanya: 'Akulah jalan dan kebenaran dan hidup. Tidak ada seorangpun yang datang kepada Bapa, kalau tidak melalui Aku.'"
                )
            )
        )
    }

    var selectedPassage by remember { mutableStateOf<BibleContent?>(samplePassages.firstOrNull()) }

    val filteredPassages = samplePassages.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
                it.verses.any { v -> v.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BIBLE & SCRIPTURE ENGINE",
                    color = Color(0xFFE6E1E9),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Instant Scripture projection for sermons & scripture readings",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            // Translation Switcher
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("NIV", "TB", "KJV", "ESV").forEach { trans ->
                    FilterChip(
                        selected = selectedTranslation == trans,
                        onClick = { selectedTranslation = trans },
                        label = { Text(trans, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF381E72),
                            selectedLabelColor = Color(0xFFD0BCFF)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search passage or keyword (e.g. John 3, Psalm 23, shepherd...)", color = Color.Gray, fontSize = 13.sp) },
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
                        text = "Scripture Bookmarks",
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
                                    text = "${passage.verses.size} Verses Ready",
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
                        Text("Select a Bible passage to view verses", color = Color.Gray)
                    }
                }
            }
        }
    }
}

package com.example.features.powerpoint

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.PowerPointContent
import com.example.model.PresentationContent
import kotlinx.coroutines.launch

@Composable
fun PowerPointManagementScreen(
    pptList: List<PowerPointContent>,
    onAddPowerPoint: (PowerPointContent) -> Unit,
    onSelectForPreview: (PresentationContent) -> Unit,
    onSelectForGo: (PresentationContent) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isConverting by remember { mutableStateOf(false) }
    var selectedPPT by remember { mutableStateOf<PowerPointContent?>(pptList.firstOrNull()) }

    val pptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            coroutineScope.launch {
                isConverting = true
                val conversionService = LocalPowerPointConversionService()
                val slideImageUris = conversionService.convert(context, fileUri)
                val fileName = fileUri.lastPathSegment?.substringAfterLast('/') ?: "Presentation.pptx"

                val newPPT = PowerPointContent(
                    id = System.currentTimeMillis().toString(),
                    title = fileName,
                    slides = slideImageUris
                )
                onAddPowerPoint(newPPT)
                selectedPPT = newPPT
                isConverting = false
            }
        }
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
            Column {
                Text(
                    text = "POWERPOINT & SLIDES ENGINE",
                    color = Color(0xFFE6E1E9),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Import PPT, PPTX, or PDF presentation slides",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = { pptPickerLauncher.launch("*/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF)),
                enabled = !isConverting
            ) {
                if (isConverting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF381E72))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Converting...", color = Color(0xFF381E72))
                } else {
                    Text("+ Import PPT / PDF", color = Color(0xFF381E72), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedPPT != null) {
            val ppt = selectedPPT!!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Deck: ${ppt.title} (${ppt.slides.size} Slides)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSelectForPreview(ppt) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49454F))
                    ) {
                        Text("Preview Deck")
                    }

                    Button(
                        onClick = { onSelectForGo(ppt) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("GO LIVE", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Slides Grid Renderer
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(ppt.slides) { idx, slideUri ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF25232A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectForPreview(ppt)
                            }
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(Color.Black)
                            ) {
                                AsyncImage(
                                    model = slideUri,
                                    contentDescription = "Slide ${idx + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .align(Alignment.TopStart)
                                ) {
                                    Text("Slide ${idx + 1}", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF25232A), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No presentation deck loaded. Click '+ Import PPT / PDF' to start.", color = Color.Gray)
            }
        }
    }
}

package com.example.features.media

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.features.video.VideoPlayer
import com.example.model.CameraContent
import com.example.model.ImageContent
import com.example.model.IpCameraContent
import com.example.model.PresentationContent
import com.example.model.VideoContent

@Composable
fun MediaManagementScreen(
    mediaList: List<PresentationContent>,
    onAddMedia: (PresentationContent) -> Unit,
    onSelectForPreview: (PresentationContent) -> Unit,
    onSelectForGo: (PresentationContent) -> Unit,
    onSetBackgroundVideo: (String) -> Unit,
    onSetBackgroundImage: (String) -> Unit,
    onSetBackgroundCamera: (Boolean) -> Unit
) {
    var showDroidCamDialog by remember { mutableStateOf(false) }
    var droidCamIpInput by remember { mutableStateOf("192.168.1.50") }
    var droidCamPortInput by remember { mutableStateOf("4747") }

    // Pickers
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val name = it.lastPathSegment?.substringAfterLast('/') ?: "Image Asset"
            onAddMedia(ImageContent(id = System.currentTimeMillis().toString(), title = name, uri = it.toString()))
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val name = it.lastPathSegment?.substringAfterLast('/') ?: "Video Asset"
            onAddMedia(VideoContent(id = System.currentTimeMillis().toString(), title = name, uri = it.toString()))
        }
    }

    if (showDroidCamDialog) {
        var selectedPath by remember { mutableStateOf("/video") }

        AlertDialog(
            onDismissRequest = { showDroidCamDialog = false },
            containerColor = Color(0xFF25232A),
            title = {
                Text("📱 Tambah DroidCam / IP Kamera HP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Masukkan alamat IP & Port dari aplikasi DroidCam di HP kamera:", color = Color.Gray, fontSize = 12.sp)

                    OutlinedTextField(
                        value = droidCamIpInput,
                        onValueChange = { droidCamIpInput = it },
                        label = { Text("IP Address HP (Wi-Fi)", color = Color.Gray) },
                        placeholder = { Text("192.168.1.50") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = droidCamPortInput,
                        onValueChange = { droidCamPortInput = it },
                        label = { Text("Port DroidCam", color = Color.Gray) },
                        placeholder = { Text("4747") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Format Feed Stream:", color = Color.Gray, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedPath == "/video",
                            onClick = { selectedPath = "/video" },
                            label = { Text("/video", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF381E72),
                                selectedLabelColor = Color(0xFFD0BCFF)
                            )
                        )
                        FilterChip(
                            selected = selectedPath == "/mjpegfeed",
                            onClick = { selectedPath = "/mjpegfeed" },
                            label = { Text("/mjpegfeed", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF381E72),
                                selectedLabelColor = Color(0xFFD0BCFF)
                            )
                        )
                    }

                    Text(
                        "💡 Tips: Pastikan HP dan aplikasi ini terhubung di jaringan Wi-Fi / Hotspot yang SAMA.",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ip = droidCamIpInput.trim()
                        val port = droidCamPortInput.trim()
                        if (ip.isNotEmpty()) {
                            val streamUrl = "http://$ip:$port$selectedPath"
                            onAddMedia(
                                IpCameraContent(
                                    id = System.currentTimeMillis().toString(),
                                    title = "DroidCam HP ($ip)",
                                    streamUrl = streamUrl
                                )
                            )
                            showDroidCamDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Simpan Kamera", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDroidCamDialog = false }) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MEDIA & BACKGROUND ENGINE",
                color = Color(0xFFE6E1E9),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { imageLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72))
                ) {
                    Text("+ Image", color = Color(0xFFD0BCFF))
                }

                Button(
                    onClick = { videoLauncher.launch("video/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72))
                ) {
                    Text("+ Video", color = Color(0xFFD0BCFF))
                }

                Button(
                    onClick = { showDroidCamDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72))
                ) {
                    Text("+ DroidCam", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val cameraMedia = CameraContent(
                            id = System.currentTimeMillis().toString(),
                            title = "Kamera Lokal Feed",
                            cameraId = "0"
                        )
                        onAddMedia(cameraMedia)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("+ Cam Lokal", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(mediaList) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF25232A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(Color.Black, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            when (item) {
                                is ImageContent -> {
                                    AsyncImage(
                                        model = item.uri,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                is VideoContent -> {
                                    Text("🎥 VIDEO", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold)
                                }
                                is CameraContent -> {
                                    Text("📷 KAMERA LOKAL", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                }
                                is IpCameraContent -> {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        VideoPlayer(
                                            videoUri = item.streamUrl,
                                            isPlaying = true,
                                            isLooping = true,
                                            isMuted = true,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(4.dp)
                                                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("📱 DROIDCAM", color = Color(0xFFD0BCFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = item.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { onSelectForGo(item) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("GO LIVE", fontSize = 11.sp, color = Color.White)
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            OutlinedButton(
                                onClick = {
                                    when (item) {
                                        is ImageContent -> onSetBackgroundImage(item.uri)
                                        is VideoContent -> onSetBackgroundVideo(item.uri)
                                        is CameraContent -> onSetBackgroundCamera(true)
                                        is IpCameraContent -> onSetBackgroundVideo(item.streamUrl)
                                        else -> {}
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Set BG", fontSize = 11.sp, color = Color(0xFFD0BCFF))
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.features.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.features.camera.CameraPreview
import com.example.features.display.DisplayProfileType
import com.example.features.display.DisplayRenderer
import com.example.features.lyrics.LyricsStylePreset
import com.example.model.CameraContent
import com.example.model.IpCameraContent
import com.example.presentation.PresentationServer
import com.example.server.getLocalIpAddress
import com.example.server.getLocalIpInfo

@Composable
fun SettingsScreen(
    server: PresentationServer,
    onSetPreset: (LyricsStylePreset) -> Unit,
    onClearBackground: () -> Unit
) {
    val context = LocalContext.current
    val displayRenderer = remember { DisplayRenderer(context, server.state) }
    var detectedDisplays by remember { mutableStateOf<List<Display>>(emptyList()) }
    var connectedDisplayProfiles by remember { mutableStateOf<Map<Int, DisplayProfileType>>(emptyMap()) }

    fun refreshDisplays() {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        detectedDisplays = dm.displays.filter { it.displayId != Display.DEFAULT_DISPLAY }
    }

    LaunchedEffect(Unit) {
        refreshDisplays()
        displayRenderer.setOnDisplayChangeListener {
            refreshDisplays()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SYSTEM SETTINGS & MULTI-DISPLAY ROUTING",
            color = Color(0xFFE6E1E9),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        // CARD 0: Live Web Server Status & Connection Guide
        LiveServerStatusCard(server = server)

        // CARD 1: Live Display & Typography Settings
        LiveTextSettingsCard(server = server, onSetPreset = onSetPreset)

        // CARD 2: External Display Output Card (Phase 9 & 10)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF25232A)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Multi-Display Routing & Profiles",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Route live presentation output to Main Projector, Stage Monitor, or Lower Thirds",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (detectedDisplays.isEmpty()) {
                    Text("No external displays detected.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        detectedDisplays.forEach { display ->
                            val isConnected = displayRenderer.isDisplayConnected(display.displayId)
                            val selectedProfile = connectedDisplayProfiles[display.displayId] ?: DisplayProfileType.MAIN_PROJECTOR

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1C1B1F), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = display.name ?: "Display #${display.displayId}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "ID: ${display.displayId} • ${display.width}x${display.height}",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }

                                    if (isConnected) {
                                        Button(
                                            onClick = {
                                                displayRenderer.disconnectDisplay(display.displayId)
                                                connectedDisplayProfiles = connectedDisplayProfiles - display.displayId
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                        ) {
                                            Text("Disconnect", color = Color.White)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                displayRenderer.connectDisplay(display, selectedProfile)
                                                connectedDisplayProfiles = connectedDisplayProfiles + (display.displayId to selectedProfile)
                                                server.selectDisplay(display.displayId.toString())
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                        ) {
                                            Text("Connect Profile", color = Color.White)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Routing Profile:", color = Color.LightGray, fontSize = 12.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    DisplayProfileType.values().forEach { profile ->
                                        val isProfileSelected = selectedProfile == profile
                                        FilterChip(
                                            selected = isProfileSelected,
                                            onClick = {
                                                connectedDisplayProfiles = connectedDisplayProfiles + (display.displayId to profile)
                                                if (isConnected) {
                                                    displayRenderer.connectDisplay(display, profile)
                                                }
                                            },
                                            label = { Text(profile.label, fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF381E72),
                                                selectedLabelColor = Color(0xFFD0BCFF)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Camera Configuration & Hardware Input Card
        var selectedCameraFacing by remember { mutableStateOf(false) } // false = Back, true = Front
        var droidCamIp by remember { mutableStateOf("192.168.1.50") }
        var droidCamPort by remember { mutableStateOf("4747") }
        var selectedStreamPath by remember { mutableStateOf("/video") }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF25232A)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📱 DroidCam & Wireless IP Camera Settings",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Connect smartphone camera via DroidCam app (WiFi/IP Stream)",
                            color = Color(0xFFD0BCFF),
                            fontSize = 12.sp
                        )
                    }

                    Surface(
                        color = Color(0xFF10B981),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "DROIDCAM READY",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = droidCamIp,
                        onValueChange = { droidCamIp = it },
                        label = { Text("Phone IP Address", fontSize = 11.sp, color = Color.Gray) },
                        placeholder = { Text("192.168.1.50") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = droidCamPort,
                        onValueChange = { droidCamPort = it },
                        label = { Text("Port", fontSize = 11.sp, color = Color.Gray) },
                        placeholder = { Text("4747") },
                        modifier = Modifier.weight(0.8f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("DroidCam Feed Path:", color = Color.LightGray, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedStreamPath == "/video",
                        onClick = { selectedStreamPath = "/video" },
                        label = { Text("/video (HD Stream)", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF381E72),
                            selectedLabelColor = Color(0xFFD0BCFF)
                        )
                    )
                    FilterChip(
                        selected = selectedStreamPath == "/mjpegfeed",
                        onClick = { selectedStreamPath = "/mjpegfeed" },
                        label = { Text("/mjpegfeed (MJPEG)", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF381E72),
                            selectedLabelColor = Color(0xFFD0BCFF)
                        )
                    )
                }

                val fullDroidCamUrl = "http://${droidCamIp.trim()}:${droidCamPort.trim()}$selectedStreamPath"

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            server.go(
                                IpCameraContent(
                                    id = "droidcam_${System.currentTimeMillis()}",
                                    title = "DroidCam HP ($droidCamIp)",
                                    streamUrl = fullDroidCamUrl
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("▶ GO LIVE DROIDCAM", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            server.setBackgroundVideo(fullDroidCamUrl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set as BG Stream", fontSize = 11.sp, color = Color(0xFFD0BCFF))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = Color(0xFF49454F))

                Spacer(modifier = Modifier.height(12.dp))

                // Built-in & USB Camera Section
                Text(
                    text = "📷 Kamera Lokal & Capture Card USB (HDMI)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Camera Test Preview Box
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(95.dp)
                            .background(Color.Black, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CameraPreview(useFrontCamera = selectedCameraFacing)
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Pilih Kamera HP:", color = Color.LightGray, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = !selectedCameraFacing,
                                onClick = { selectedCameraFacing = false },
                                label = { Text("Kamera Belakang", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF381E72),
                                    selectedLabelColor = Color(0xFFD0BCFF)
                                )
                            )
                            FilterChip(
                                selected = selectedCameraFacing,
                                onClick = { selectedCameraFacing = true },
                                label = { Text("Depan", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF381E72),
                                    selectedLabelColor = Color(0xFFD0BCFF)
                                )
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    server.go(
                                        CameraContent(
                                            id = "cam_live",
                                            title = "Kamera Lokal",
                                            cameraId = if (selectedCameraFacing) "1" else "0"
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("GO LIVE", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val currentCamBg = server.state.value.backgroundType == com.example.presentation.BackgroundType.CAMERA
                                    server.setBackgroundCamera(!currentCamBg)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Overlay BG", fontSize = 11.sp, color = Color(0xFFD0BCFF))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "💡Cara Menghubungkan DroidCam:\n1. Buka DroidCam di HP HP/Kamera lain (harus 1 jaringan Wi-Fi dengan tablet/HP ini).\n2. Masukkan IP yang tertera di DroidCam (contoh: 192.168.1.50) dan Port (4747).\n3. Klik 'GO LIVE DROIDCAM' atau 'Set as BG Stream'.\n4. Di menu MEDIA, Anda juga bisa klik tombol '+ DroidCam' untuk menyimpan preset kamera HP.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Background Controller Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF25232A)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Background Engine",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onClearBackground,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("Clear Active Background")
                }
            }
        }

        // Server Status & System Optimization Card (Phase 10)
        val optimizationEngine = remember { SystemOptimizationEngine(context) }
        val healthState by optimizationEngine.healthState.collectAsState()

        LaunchedEffect(detectedDisplays) {
            optimizationEngine.checkSystemHealth(detectedDisplays.size)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF25232A)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "System Performance & Diagnostics (Phase 10)",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Memory management, auto-recovery & video renderer health",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Heap Memory: ${healthState.memoryUsageMb} MB", color = Color.White, fontSize = 13.sp)
                        Text("Status: ${healthState.statusMessage}", color = Color(0xFF10B981), fontSize = 12.sp)
                    }

                    Button(
                        onClick = { optimizationEngine.runMemoryOptimization() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72))
                    ) {
                        Text("Optimize Memory", color = Color(0xFFD0BCFF), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF10B981), RoundedCornerShape(5.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Server Status: Active (Port 8080) • WS Event Sync Active", color = Color.LightGray, fontSize = 13.sp)
                }
            }
        }
}
}

@Composable
fun LiveTextSettingsCard(
    server: PresentationServer,
    onSetPreset: (LyricsStylePreset) -> Unit
) {
    val presState by server.state.collectAsState()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF25232A)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🎨 Pengaturan Tampilan Live & Font",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Atur ukuran font, tata letak posisi, warna, dan transparansi layar live",
                        color = Color(0xFFD0BCFF),
                        fontSize = 12.sp
                    )
                }


                Column(horizontalAlignment = Alignment.End) {
                    val context = LocalContext.current
                    var ipAddress by remember { mutableStateOf("Memuat IP...") }
                    val port = server.webServer.activePort
                    
                    LaunchedEffect(port) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val info = getLocalIpInfo()
                            val primary = info.primaryIp
                            ipAddress = if (primary.isNotEmpty()) "http://$primary:$port" else "http://localhost:$port"
                        }
                    }
                    
                    Surface(
                        color = Color(0xFF10B981),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = "Live Web: $ipAddress",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    Surface(
                        color = Color(0xFF381E72),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                        text = "${presState.fontSizeSp} SP • ${presState.textPosition.label}",
                        color = Color(0xFFD0BCFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
                }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Preview Window
            Text("LIVE TEXT PREVIEW:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color.Black, RoundedCornerShape(8.dp))
            ) {
                com.example.features.display.PresentationFrameRenderer(
                    state = presState.copy(
                        currentContent = com.example.model.LyricsContent(
                            id = "preview_sample",
                            title = "Preview Lirik",
                            slides = listOf("Sebab Tuhan Maha Besar\nDan Sangat Terpuji Di Kota Allah Kita")
                        ),
                        status = com.example.presentation.PresentationStatus.LYRICS
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. UKURAN FONT (FONT SIZE)
            Text("1. Ukuran Font (Font Size):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (presState.fontSizeSp > 16) {
                            server.updateLiveTextSettings(fontSizeSp = presState.fontSizeSp - 2)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                    modifier = Modifier.width(48.dp)
                ) {
                    Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Text(
                    text = "${presState.fontSizeSp} SP",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.width(55.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Button(
                    onClick = {
                        if (presState.fontSizeSp < 72) {
                            server.updateLiveTextSettings(fontSizeSp = presState.fontSizeSp + 2)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                    modifier = Modifier.width(48.dp)
                ) {
                    Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.width(4.dp))

                val sizeOptions = listOf(20, 26, 32, 40, 48)
                sizeOptions.forEach { size ->
                    FilterChip(
                        selected = presState.fontSizeSp == size,
                        onClick = { server.updateLiveTextSettings(fontSizeSp = size) },
                        label = { Text("${size}sp", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF10B981),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. LETAK / POSISI FONT SAAT LIVE (TEXT POSITION ON SCREEN)
            Text("2. Letak / Posisi Teks Pada Layar Live:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                com.example.presentation.TextDisplayPosition.values().forEach { pos ->
                    FilterChip(
                        selected = presState.textPosition == pos,
                        onClick = { server.updateLiveTextSettings(textPosition = pos) },
                        label = { Text(pos.label, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF381E72),
                            selectedLabelColor = Color(0xFFD0BCFF)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. RATA TEKS (TEXT ALIGNMENT)
            Text("3. Rata Teks (Text Alignment):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                com.example.presentation.TextAlignmentOption.values().forEach { alignOpt ->
                    FilterChip(
                        selected = presState.textAlignment == alignOpt,
                        onClick = { server.updateLiveTextSettings(textAlignment = alignOpt) },
                        label = { Text(alignOpt.label, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF381E72),
                            selectedLabelColor = Color(0xFFD0BCFF)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. WARNA FONT & GAYA (TEXT COLOR & STYLE)
            Text("4. Warna Font & Bayangan:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val colorList = listOf(
                    "Putih" to 0xFFFFFFFF,
                    "Kuning" to 0xFFFACC15,
                    "Ungu" to 0xFFD0BCFF,
                    "Hijau" to 0xFF10B981,
                    "Cyan" to 0xFF06B6D4,
                    "Orange" to 0xFFF97316
                )

                colorList.forEach { (name, hex) ->
                    FilterChip(
                        selected = presState.textColorRgb == hex,
                        onClick = { server.updateLiveTextSettings(textColorRgb = hex) },
                        label = { Text(name, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(hex),
                            selectedLabelColor = if (hex == 0xFFFFFFFF || hex == 0xFFFACC15 || hex == 0xFFD0BCFF) Color.Black else Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = presState.isTextBold,
                    onClick = { server.updateLiveTextSettings(isTextBold = !presState.isTextBold) },
                    label = { Text(if (presState.isTextBold) "✓ Teks Tebal (Bold)" else "Teks Normal", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF381E72),
                        selectedLabelColor = Color(0xFFD0BCFF)
                    )
                )

                FilterChip(
                    selected = presState.isTextShadowEnabled,
                    onClick = { server.updateLiveTextSettings(isTextShadowEnabled = !presState.isTextShadowEnabled) },
                    label = { Text(if (presState.isTextShadowEnabled) "✓ Bayangan Teks (Shadow)" else "Tanpa Bayangan", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF381E72),
                        selectedLabelColor = Color(0xFFD0BCFF)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. KEGELAPAN OVERLAY LATAR (BACKGROUND DIM)
            Text("5. Transparansi Latar Teks (Overlay Dimming):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val alphaOptions = listOf(
                    "0% (Bebas)" to 0.0f,
                    "35% (Sedang)" to 0.35f,
                    "65% (Gelap)" to 0.65f,
                    "85% (Penuh)" to 0.85f
                )

                alphaOptions.forEach { (label, alpha) ->
                    FilterChip(
                        selected = Math.abs(presState.textBackgroundAlpha - alpha) < 0.05f,
                        onClick = { server.updateLiveTextSettings(textBackgroundAlpha = alpha) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF381E72),
                            selectedLabelColor = Color(0xFFD0BCFF)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PRESET CEPAT
            Text("PRESET CEPAT:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LyricsStylePreset.values().forEach { preset ->
                    Button(
                        onClick = {
                            onSetPreset(preset)
                            server.setStylePreset(preset)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(preset.presetName, fontSize = 10.sp, color = Color(0xFFD0BCFF))
                    }
                }
            }
        }
    }
}

@Composable
fun LiveServerStatusCard(server: PresentationServer) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var ipInfo by remember { mutableStateOf<com.example.server.IpInfo?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val port = server.webServer.activePort

    LaunchedEffect(port, refreshKey) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            ipInfo = getLocalIpInfo(context)
        }
    }

    val info = ipInfo
    val wifiUrl = if (info != null && info.wifiIps.isNotEmpty()) "http://${info.wifiIps.first()}:$port" else if (info != null && info.primaryIp.isNotEmpty() && info.primaryIp != "localhost" && info.primaryIp != "127.0.0.1") "http://${info.primaryIp}:$port" else "http://192.168.1.x:$port"
    val hotspotUrl = if (info != null && info.hotspotIps.isNotEmpty()) "http://${info.hotspotIps.first()}:$port" else "http://192.168.43.1:$port"
    val localUrl = "http://localhost:$port"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Status Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🌐", fontSize = 20.sp)
                    Column {
                        Text(
                            text = "Server Live Broadcast & Web Stream",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Akses tayangan live di Browser, OBS, HP lain, atau Laptop",
                            color = Color(0xFFD0BCFF),
                            fontSize = 12.sp
                        )
                    }
                }

                Surface(
                    color = Color(0xFF10B981),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "● Online (Port $port)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PORT SWITCHER & RESTART
            Surface(
                color = Color(0xFF252136),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ganti Port Server:",
                        color = Color(0xFFE6E1E9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(8080, 8081, 8888, 5000).forEach { p ->
                            val isSelected = p == port
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (!isSelected) {
                                        server.webServer.restartOnPort(p)
                                        Toast.makeText(context, "Mengubah server ke Port $p...", Toast.LENGTH_SHORT).show()
                                        refreshKey++
                                    }
                                },
                                label = { Text("$p", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFD0BCFF),
                                    selectedLabelColor = Color(0xFF381E72),
                                    containerColor = Color(0xFF1C1B1F),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. OPEN ON SAME PHONE (LOCALHOST)
            Surface(
                color = Color(0xFF2D283E),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📱", fontSize = 16.sp)
                            Text("1. Browser Di HP Ini (Lokal):", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(4.dp)) {
                            Text("SAMA HP", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = localUrl,
                        color = Color(0xFF10B981),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(localUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Throwable) {
                                    Toast.makeText(context, "Gagal membuka browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🚀 Buka di Browser HP Ini", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(localUrl))
                                Toast.makeText(context, "URL Lokal disalin!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("📋 Salin", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. WI-FI NETWORK (HP LAIN / LAPTOP / OBS STUDIO)
            Surface(
                color = Color(0xFF2D283E),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("💻", fontSize = 16.sp)
                            Text("2. HP Lain / Laptop / OBS Studio (Wi-Fi):", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Surface(color = Color(0xFF381E72), shape = RoundedCornerShape(4.dp)) {
                            Text("WI-FI SAMA", color = Color(0xFFD0BCFF), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = wifiUrl,
                        color = Color(0xFF38BDF8),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (info != null && info.wifiIps.size > 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Opsi Alamat IP Wi-Fi Alternatif: " + info.wifiIps.drop(1).joinToString(", ") { "http://$it:$port" },
                            color = Color(0xFF93C5FD),
                            fontSize = 11.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(wifiUrl))
                            Toast.makeText(context, "URL Wi-Fi disalin!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📋 Salin URL Wi-Fi ($wifiUrl)", fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. WEB REMOTE CONTROLLER
            val remoteUrl = if (info != null && info.wifiIps.isNotEmpty()) "http://${info.wifiIps.first()}:$port/remote" else "$localUrl/remote"
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📱", fontSize = 16.sp)
                            Text("3. Web Remote Controller (Kontrol Jarak Jauh):", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(4.dp)) {
                            Text("WEB REMOTE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = remoteUrl,
                        color = Color(0xFF34D399),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Buka link ini di browser HP/tablet/laptop apa saja untuk mengontrol slide (Next, Prev, Blackout, Clear) secara nirkabel via Wi-Fi!",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(remoteUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Throwable) {
                                    Toast.makeText(context, "Gagal membuka browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🚀 Buka Web Remote", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(remoteUrl))
                                Toast.makeText(context, "URL Web Remote disalin!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("📋 Salin Link Remote", fontSize = 11.sp)
                        }
                    }
                }
            }

            if (info != null && info.hotspotIps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                // 4. HOTSPOT HP (TETHERING)
                Surface(
                    color = Color(0xFF2D283E),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📡", fontSize = 16.sp)
                            Text("4. Hotspot HP (Tethering):", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = hotspotUrl, color = Color(0xFFFACC15), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(hotspotUrl))
                                Toast.makeText(context, "URL Hotspot disalin!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📋 Salin URL Hotspot", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TROUBLESHOOTING BANNER
            Surface(
                color = Color(0xFF3B1D1D),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("⚠️", fontSize = 16.sp)
                        Text("Solusi Jika Tetap Tidak Bisa Dihubungi dari HP Lain:", color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = "1. Isolasi AP (AP Isolation) di Router Wi-Fi: Banyak router Wi-Fi Publik / Gereja mengaktifkan 'AP Isolation' yang melarang HP bertukar data dengan HP lain meskipun terhubung ke Wi-Fi yang sama.\n" +
                               "2. Solusi Terbaik Tanpa Router: Nyalakan Hotspot Seluler di HP ini, lalu hubungkan HP pengakses/Laptop ke Hotspot HP ini. Gunakan URL Hotspot ($hotspotUrl).\n" +
                               "3. Coba Ganti Port Server: Jika Port $port diblokir firewall lokal HP, klik tombol 'Ganti Port Server' ke 8081 atau 8888 di atas.\n" +
                               "4. Aplikasi Utama Aman & Stabil: Penanganan WebSocket dan Server telah dilengkapi isolasi error (supervisorScope) sehingga tidak akan terjadi Force Close lagi.",
                        color = Color(0xFFFECACA),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

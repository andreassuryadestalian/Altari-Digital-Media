package com.example.features.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.presentation.LyricsDisplayMode
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

        // CARD 1.5: Split Screen (2-Screen 30:70 Live Cam & Sermon Mode)
        SplitScreenSettingsCard(server = server)

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
fun SplitScreenSettingsCard(server: PresentationServer) {
    val presState by server.state.collectAsState()
    var customStreamUrl by remember(presState.splitCameraStreamUrl) {
        mutableStateOf(presState.splitCameraStreamUrl ?: "http://192.168.1.50:4747/video")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "📺 Dual Split-Screen (30:70 Live Cam & Materi)",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (presState.isSplitScreenEnabled) {
                            Surface(
                                color = Color(0xFF10B981),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Tampilkan 2 layar live berdampingan: Live Kamera Pembicara (30%) + Materi Khotbah/Lirik (70%)",
                        color = Color(0xFFD0BCFF),
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = presState.isSplitScreenEnabled,
                    onCheckedChange = { isEnabled ->
                        server.updateSplitScreenSettings(isEnabled = isEnabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFD0BCFF),
                        checkedTrackColor = Color(0xFF381E72),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF25232A)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Mini Visual Preview of Split Screen
            Text("PREVIEW SPLIT SCREEN LAYOUT:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                if (presState.isSplitScreenEnabled) {
                    val camPercent = presState.splitRatioCamPercent.coerceIn(15, 85)
                    val contentPercent = 100 - camPercent

                    Row(modifier = Modifier.fillMaxSize()) {
                        if (presState.splitScreenSide == com.example.presentation.SplitScreenSide.CAM_LEFT_CONTENT_RIGHT) {
                            // Cam Left
                            Box(
                                modifier = Modifier
                                    .weight(camPercent / 100f)
                                    .fillMaxHeight()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔴 LIVE CAM", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("${camPercent}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFF38BDF8))
                            )

                            // Content Right
                            Box(
                                modifier = Modifier
                                    .weight(contentPercent / 100f)
                                    .fillMaxHeight()
                                    .background(Color(0xFF090D16), RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📖 MATERI KHOTBAH / LIRIK", color = Color(0xFFD0BCFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("${contentPercent}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        } else {
                            // Content Left
                            Box(
                                modifier = Modifier
                                    .weight(contentPercent / 100f)
                                    .fillMaxHeight()
                                    .background(Color(0xFF090D16), RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📖 MATERI KHOTBAH / LIRIK", color = Color(0xFFD0BCFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("${contentPercent}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFF38BDF8))
                            )

                            // Cam Right
                            Box(
                                modifier = Modifier
                                    .weight(camPercent / 100f)
                                    .fillMaxHeight()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔴 LIVE CAM", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("${camPercent}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Mode Split Screen Nonaktif (Layar Live Single 100% Penuh)",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. RASIO SPLIT PRESET CHIPS
            Text("1. Rasio Pembagian Layar (Live Cam : Materi):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val ratioPresets = listOf(
                    "30:70 (Standar)" to 30,
                    "70:30" to 70,
                    "50:50" to 50,
                    "40:60" to 40,
                    "25:75" to 25
                )

                ratioPresets.forEach { (label, ratio) ->
                    FilterChip(
                        selected = presState.splitRatioCamPercent == ratio,
                        onClick = { server.updateSplitScreenSettings(ratioCamPercent = ratio) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF381E72),
                            selectedLabelColor = Color(0xFFD0BCFF)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fine tuning slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Kamera: ${presState.splitRatioCamPercent}%", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(85.dp))
                Slider(
                    value = presState.splitRatioCamPercent.toFloat(),
                    onValueChange = { server.updateSplitScreenSettings(ratioCamPercent = it.toInt()) },
                    valueRange = 15f..85f,
                    steps = 13,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF381E72),
                        inactiveTrackColor = Color(0xFF25232A)
                    )
                )
                Text("Materi: ${100 - presState.splitRatioCamPercent}%", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(85.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. POSISI LAYAR (SIDE ORIENTATION)
            Text("2. Posisi Tata Letak Layar (Kanan / Kiri):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = presState.splitScreenSide == com.example.presentation.SplitScreenSide.CAM_LEFT_CONTENT_RIGHT,
                    onClick = {
                        server.updateSplitScreenSettings(side = com.example.presentation.SplitScreenSide.CAM_LEFT_CONTENT_RIGHT)
                    },
                    label = { Text("👈 Kamera Kiri (30%) | Materi Kanan (70%)", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF381E72),
                        selectedLabelColor = Color(0xFFD0BCFF)
                    )
                )

                FilterChip(
                    selected = presState.splitScreenSide == com.example.presentation.SplitScreenSide.CONTENT_LEFT_CAM_RIGHT,
                    onClick = {
                        server.updateSplitScreenSettings(side = com.example.presentation.SplitScreenSide.CONTENT_LEFT_CAM_RIGHT)
                    },
                    label = { Text("Materi Kiri (70%) | Kamera Kanan (30%) 👉", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF381E72),
                        selectedLabelColor = Color(0xFFD0BCFF)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. SUMBER KAMERA LIVE (LIVE CAMERA SOURCE)
            Text("3. Sumber Kamera Live Cam:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = presState.splitCameraSourceType == com.example.presentation.BackgroundType.IP_CAMERA,
                    onClick = {
                        server.updateSplitScreenSettings(
                            sourceType = com.example.presentation.BackgroundType.IP_CAMERA,
                            cameraStreamUrl = customStreamUrl
                        )
                    },
                    label = { Text("📱 DroidCam / Wireless IP Camera", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF381E72),
                        selectedLabelColor = Color(0xFFD0BCFF)
                    )
                )

                FilterChip(
                    selected = presState.splitCameraSourceType == com.example.presentation.BackgroundType.CAMERA,
                    onClick = {
                        server.updateSplitScreenSettings(
                            sourceType = com.example.presentation.BackgroundType.CAMERA,
                            cameraStreamUrl = "/camera/stream"
                        )
                    },
                    label = { Text("📷 Kamera Internal / USB Cam", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF381E72),
                        selectedLabelColor = Color(0xFFD0BCFF)
                    )
                )
            }

            if (presState.splitCameraSourceType == com.example.presentation.BackgroundType.IP_CAMERA) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customStreamUrl,
                        onValueChange = {
                            customStreamUrl = it
                            server.updateSplitScreenSettings(cameraStreamUrl = it)
                        },
                        label = { Text("URL Stream IP Camera / DroidCam", fontSize = 11.sp, color = Color.Gray) },
                        placeholder = { Text("http://192.168.1.50:4747/video") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Button(
                        onClick = {
                            server.updateSplitScreenSettings(
                                isEnabled = true,
                                sourceType = com.example.presentation.BackgroundType.IP_CAMERA,
                                cameraStreamUrl = customStreamUrl
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Simpan & Live", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎨 Pengaturan Posisi & Tampilan Teks Live Screen",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Fleksibel atur posisi teks, ukuran font, perataan, warna, border, dan tata letak lirik",
                        color = Color(0xFFD0BCFF),
                        fontSize = 12.sp
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

            Spacer(modifier = Modifier.height(12.dp))

            // Live Preview Window
            Text("LIVE TEXT PREVIEW (TAMPILAN NYATA):", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
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

            Spacer(modifier = Modifier.height(16.dp))

            // FORMAT PENAMPIL LIRIK (PER BAIT ATAU PER BARIS)
            Text("Format Penampilan Lirik (Lyrics Display Mode):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("Pilih bagaimana lirik lagu dipecah dan ditampilkan pada layar tayangan / proyektor / live stream.", color = Color.Gray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Option 1: PER BAIT
                val isBait = presState.lyricsDisplayMode == LyricsDisplayMode.PER_BAIT
                Surface(
                    color = if (isBait) Color(0xFF381E72) else Color(0xFF25232A),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, if (isBait) Color(0xFFD0BCFF) else Color(0xFF383545)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { server.setLyricsDisplayMode(LyricsDisplayMode.PER_BAIT) }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            RadioButton(
                                selected = isBait,
                                onClick = { server.setLyricsDisplayMode(LyricsDisplayMode.PER_BAIT) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFD0BCFF))
                            )
                            Text("Per Bait (Verse)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Menampilkan bait penuh (verse / chorus) dalam 1 slide. Standar untuk proyektor jemaat.",
                            color = Color(0xFFE2E8F0),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                // Option 2: PER BARIS
                val isBaris = presState.lyricsDisplayMode == LyricsDisplayMode.PER_BARIS
                Surface(
                    color = if (isBaris) Color(0xFF0F766E) else Color(0xFF25232A),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, if (isBaris) Color(0xFF2DD4BF) else Color(0xFF383545)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { server.setLyricsDisplayMode(LyricsDisplayMode.PER_BARIS) }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            RadioButton(
                                selected = isBaris,
                                onClick = { server.setLyricsDisplayMode(LyricsDisplayMode.PER_BARIS) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2DD4BF))
                            )
                            Text("Per Baris (1 Line)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Memecah lirik 1 baris per slide secara otomatis. Sangat rapi untuk Lower-Third & Live Stream!",
                            color = Color(0xFFE2E8F0),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. LETAK / POSISI TEKS FLEKSIBEL (FLEXIBLE POSITIONING PRESETS & FREE MODE)
            Text("1. Letak / Posisi Teks Pada Layar Live:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            
            // Grid of Presets
            val positionsRow1 = listOf(
                com.example.presentation.TextDisplayPosition.LOWER_THIRD,
                com.example.presentation.TextDisplayPosition.CENTER,
                com.example.presentation.TextDisplayPosition.BOTTOM_CENTER,
                com.example.presentation.TextDisplayPosition.TOP_BANNER
            )
            val positionsRow2 = listOf(
                com.example.presentation.TextDisplayPosition.LEFT_CENTER,
                com.example.presentation.TextDisplayPosition.RIGHT_CENTER,
                com.example.presentation.TextDisplayPosition.TOP_LEFT,
                com.example.presentation.TextDisplayPosition.TOP_RIGHT
            )
            val positionsRow3 = listOf(
                com.example.presentation.TextDisplayPosition.BOTTOM_LEFT,
                com.example.presentation.TextDisplayPosition.BOTTOM_RIGHT,
                com.example.presentation.TextDisplayPosition.CUSTOM
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                positionsRow1.forEach { pos ->
                    FilterChip(
                        selected = presState.textPosition == pos,
                        onClick = { server.updateLiveTextSettings(textPosition = pos) },
                        label = { Text(pos.label, fontSize = 9.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF381E72),
                            selectedLabelColor = Color(0xFFD0BCFF)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                positionsRow2.forEach { pos ->
                    FilterChip(
                        selected = presState.textPosition == pos,
                        onClick = { server.updateLiveTextSettings(textPosition = pos) },
                        label = { Text(pos.label, fontSize = 9.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF381E72),
                            selectedLabelColor = Color(0xFFD0BCFF)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                positionsRow3.forEach { pos ->
                    FilterChip(
                        selected = presState.textPosition == pos,
                        onClick = { server.updateLiveTextSettings(textPosition = pos) },
                        label = { Text(pos.label, fontSize = 9.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF10B981),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // If Custom Positioning is selected or fine-tuning sliders
            if (presState.textPosition == com.example.presentation.TextDisplayPosition.CUSTOM) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFF1C1B1F),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("📐 Pengaturan Koordinat Bebas (Custom Coordinates):", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        
                        // Vertical Position Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Posisi Vertikal (Atas - Bawah): ${presState.textVerticalPercent}%", color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Slider(
                                value = presState.textVerticalPercent.toFloat(),
                                onValueChange = { server.updateLiveTextSettings(textVerticalPercent = it.toInt()) },
                                valueRange = 5f..95f,
                                modifier = Modifier.weight(1.2f),
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFD0BCFF), activeTrackColor = Color(0xFF381E72))
                            )
                        }

                        // Horizontal Position Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Posisi Horizontal (Kiri - Kanan): ${presState.textHorizontalPercent}%", color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Slider(
                                value = presState.textHorizontalPercent.toFloat(),
                                onValueChange = { server.updateLiveTextSettings(textHorizontalPercent = it.toInt()) },
                                valueRange = 5f..95f,
                                modifier = Modifier.weight(1.2f),
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFD0BCFF), activeTrackColor = Color(0xFF381E72))
                            )
                        }

                        // Width Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Lebar Kotak Teks: ${presState.textBoxWidthPercent}%", color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Slider(
                                value = presState.textBoxWidthPercent.toFloat(),
                                onValueChange = { server.updateLiveTextSettings(textBoxWidthPercent = it.toInt()) },
                                valueRange = 30f..100f,
                                modifier = Modifier.weight(1.2f),
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFD0BCFF), activeTrackColor = Color(0xFF381E72))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. UKURAN FONT (FONT SIZE)
            Text("2. Ukuran Font (Font Size):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

            // 4. WARNA FONT & GAYA TEKS
            Text("4. Warna Font & Gaya Teks:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

            // Toggles: Bold, Shadow, Uppercase, Border
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = presState.isTextBold,
                    onClick = { server.updateLiveTextSettings(isTextBold = !presState.isTextBold) },
                    label = { Text(if (presState.isTextBold) "✓ Bold" else "Normal", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF381E72),
                        selectedLabelColor = Color(0xFFD0BCFF)
                    )
                )

                FilterChip(
                    selected = presState.isTextShadowEnabled,
                    onClick = { server.updateLiveTextSettings(isTextShadowEnabled = !presState.isTextShadowEnabled) },
                    label = { Text(if (presState.isTextShadowEnabled) "✓ Shadow" else "No Shadow", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF381E72),
                        selectedLabelColor = Color(0xFFD0BCFF)
                    )
                )

                FilterChip(
                    selected = presState.isTextUppercase,
                    onClick = { server.updateLiveTextSettings(isTextUppercase = !presState.isTextUppercase) },
                    label = { Text(if (presState.isTextUppercase) "✓ KAPITAL" else "Aa Normal", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF381E72),
                        selectedLabelColor = Color(0xFFD0BCFF)
                    )
                )

                FilterChip(
                    selected = presState.textBoxBorderEnabled,
                    onClick = { server.updateLiveTextSettings(textBoxBorderEnabled = !presState.textBoxBorderEnabled) },
                    label = { Text(if (presState.textBoxBorderEnabled) "✓ Border" else "No Border", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF381E72),
                        selectedLabelColor = Color(0xFFD0BCFF)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. JARAK ANTAR BARIS (LINE HEIGHT) & KOTAK TEKS (CORNER / PADDING / DIM)
            Text("5. Pengaturan Spasi Antar Baris & Kotak Teks:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Spasi Baris:", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically))
                val lineHeights = listOf("Rapat (1.1x)" to 1.1f, "Normal (1.35x)" to 1.35f, "Longgar (1.6x)" to 1.6f)
                lineHeights.forEach { (label, lh) ->
                    FilterChip(
                        selected = Math.abs(presState.textLineHeightMultiplier - lh) < 0.05f,
                        onClick = { server.updateLiveTextSettings(textLineHeightMultiplier = lh) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF381E72),
                            selectedLabelColor = Color(0xFFD0BCFF)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Transparency
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Latar Kotak:", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically))
                val alphaOptions = listOf(
                    "0% (Bebas)" to 0.0f,
                    "35% (Sedang)" to 0.35f,
                    "65% (Gelap)" to 0.65f,
                    "85% (Solid)" to 0.85f
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

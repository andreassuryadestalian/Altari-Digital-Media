package com.example.features.settings

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.features.camera.CameraPreview
import com.example.features.display.DisplayProfileType
import com.example.features.display.DisplayRenderer
import com.example.features.lyrics.LyricsStylePreset
import com.example.model.CameraContent
import com.example.model.IpCameraContent
import com.example.presentation.PresentationServer

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
        detectedDisplays = dm.displays.toList()
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SYSTEM SETTINGS & MULTI-DISPLAY ROUTING",
            color = Color(0xFFE6E1E9),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        // External Display Output Card (Phase 9 & 10)
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

        // Preset Selector Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF25232A)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Lyrics Style Presets",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select text rendering style for external projection",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LyricsStylePreset.values().forEach { preset ->
                        Button(
                            onClick = { onSetPreset(preset) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(preset.presetName, fontSize = 11.sp, color = Color(0xFFD0BCFF))
                        }
                    }
                }
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


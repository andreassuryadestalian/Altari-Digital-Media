package com.example.features.stage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.features.dashboard.*
import com.example.features.display.StageMonitorRenderer
import com.example.presentation.PresentationServer
import com.example.presentation.PresentationState
import com.example.presentation.TimerMode
import com.example.server.getLocalIpAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun StageMonitorScreen(server: PresentationServer) {
    val presentationState by server.state.collectAsState()
    var alertInputText by remember { mutableStateOf("") }
    val localIp = remember(server) { getLocalIpAddress(server.context) }
    val stageUrl = "http://$localIp:${server.webServer.activePort}/stage"

    var currentTime by remember {
        mutableStateOf(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    val remainingSecs = presentationState.sermonTimerRemainingSeconds
    val isOvertime = remainingSecs < 0
    val absSecs = abs(remainingSecs)
    val timerHours = absSecs / 3600
    val timerMinutes = (absSecs % 3600) / 60
    val timerSeconds = absSecs % 60
    val formattedTimer = if (timerHours > 0) {
        String.format(Locale.US, "%s%02d:%02d:%02d", if (isOvertime) "+" else "", timerHours, timerMinutes, timerSeconds)
    } else {
        String.format(Locale.US, "%s%02d:%02d", if (isOvertime) "+" else "", timerMinutes, timerSeconds)
    }

    val timerColor = when {
        !presentationState.sermonTimerRunning && remainingSecs == presentationState.sermonTimerTotalSeconds -> Color(0xFF94A3B8)
        isOvertime -> Color(0xFFEF4444)
        remainingSecs <= 300 -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgMain)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Live Stage Confidence Monitor Preview
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BgPanel),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("⏱️", fontSize = 18.sp)
                            Text("MONITOR PANGGUNG (CONFIDENCE MONITOR)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Surface(
                            color = Emerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("LIVE FEED", color = EmeraldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Embedded Live Renderer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, BorderDark, RoundedCornerShape(8.dp))
                    ) {
                        StageMonitorRenderer(state = presentationState)
                    }
                }
            }
        }

        // Section 2: Sermon Timer Controller & Realtime Clock
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BgPanel),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⏱️ KONTROL TIMER KHOTBAH & JAM REALTIME", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    // Timer Display & Realtime Clock Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Realtime Clock Box
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("JAM REALTIME", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(currentTime, color = Color(0xFFFACC15), fontSize = 24.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        // Sermon Countdown Timer Box
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = when {
                                        isOvertime -> "⚠️ WAKTU HABIS (OVERTIME)"
                                        presentationState.sermonTimerRunning -> "⏱️ TIMER SEDANG BERJALAN"
                                        else -> "⏸️ TIMER BERHENTI (PAUSED)"
                                    },
                                    color = timerColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(formattedTimer, color = timerColor, fontSize = 28.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    // Main Action Buttons: Start/Pause, Reset, +5m, -5m
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { server.toggleSermonTimer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (presentationState.sermonTimerRunning) Color(0xFFF59E0B) else Color(0xFF10B981)
                            ),
                            modifier = Modifier.weight(1.2f).height(46.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (presentationState.sermonTimerRunning) "⏸️ PAUSE" else "▶️ START TIMER",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = { server.resetSermonTimer() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F3F46)),
                            modifier = Modifier.weight(0.9f).height(46.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🔄 RESET", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { server.addSermonTimerMinutes(5) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            modifier = Modifier.weight(0.9f).height(46.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+5 MENIT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    // Preset Durations
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Pilih Durasi Khotbah:", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf(15, 20, 30, 45, 60)
                            presets.forEach { mins ->
                                val isSelected = presentationState.sermonTimerTotalSeconds == mins * 60
                                Button(
                                    onClick = {
                                        server.setSermonTimerDuration(mins)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) PrimaryDark else Color(0xFF27272A)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text(
                                        "$mins Min",
                                        color = if (isSelected) Primary else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Mode Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Mode Tampilan Timer:", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TimerMode.values().forEach { mode ->
                                val isSelected = presentationState.sermonTimerMode == mode
                                Button(
                                    onClick = { server.setStageTimerMode(mode) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFF0D9488) else Color(0xFF27272A)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text(
                                        text = when(mode) {
                                            TimerMode.COUNTDOWN -> "⏳ Mundur"
                                            TimerMode.COUNT_UP -> "⏱️ Maju"
                                            TimerMode.CLOCK_ONLY -> "🕒 Jam Saja"
                                        },
                                        color = if (isSelected) Color(0xFF99F6E4) else Color.White,
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

        // Section 3: Pesan Kilat Operator ke Panggung (Stage Alerts)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BgPanel),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📢 PESAN KILAT KE PANGGUNG (STAGE FLASH MESSAGE)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Kirim pesan langsung ke monitor panggung/preacher tanpa terlihat oleh jemaat.", color = Color.Gray, fontSize = 11.sp)

                    // Active Alert Indicator
                    if (presentationState.isStageAlertActive && !presentationState.stageAlertMessage.isNullOrBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                    Text("📢", fontSize = 14.sp)
                                    Text("Sedang Tayang di Panggung: \"${presentationState.stageAlertMessage}\"", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { server.clearStageAlert() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Hapus", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Input field & Send Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = alertInputText,
                            onValueChange = { alertInputText = it },
                            placeholder = { Text("Ketik pesan kilat untuk panggung...", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = BorderDark
                            )
                        )

                        Button(
                            onClick = {
                                if (alertInputText.isNotBlank()) {
                                    server.sendStageAlert(alertInputText)
                                    alertInputText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("KIRIM 📢", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Quick Preset Flash Messages
                    Text("Pesan Cepat Siap Pakai:", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val quickMessages = listOf(
                            "⏱️ Sisa 5 Menit",
                            "⏱️ Sisa 2 Menit",
                            "🙏 Mohon Simpulkan Khotbah",
                            "🎤 Sesi Doa / Altar Call",
                            "🚗 Mobil B 1234 CD Harap Dipindahkan",
                            "🔋 Mic 1 Baterai Lemah"
                        )
                        items(quickMessages) { msg ->
                            Surface(
                                color = Color(0xFF27272A),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.clickable {
                                    server.sendStageAlert(msg)
                                }
                            ) {
                                Text(
                                    text = msg,
                                    color = Color(0xFFD0BCFF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Web Stage Monitor Link
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("📱", fontSize = 16.sp)
                        Text("AKSES MONITOR PANGGUNG NIRKABEL (WEB DISPLAY)", color = Color(0xFFC7D2FE), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        "Buka link di bawah ini pada tablet, iPad, laptop, atau smart TV panggung yang terhubung di jaringan Wi-Fi gereja:",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF312E81)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stageUrl,
                            color = Color(0xFFFDE047),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

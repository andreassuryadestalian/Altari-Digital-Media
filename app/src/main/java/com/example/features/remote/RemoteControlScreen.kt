package com.example.features.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LyricsContent
import com.example.model.PowerPointContent
import com.example.presentation.PresentationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteControlScreen(
    remoteEngine: RemoteServerEngine
) {
    val currentPin by remoteEngine.pairingPin.collectAsState()
    var inputPin by remember { mutableStateOf("") }
    var sessionToken by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val eventFlow = remoteEngine.eventFlow.collectAsState(initial = null)
    var currentState by remember { mutableStateOf(eventFlow.value?.let { if (it is RemoteEvent.StateUpdated) it.state else null }) }

    LaunchedEffect(eventFlow.value) {
        when (val event = eventFlow.value) {
            is RemoteEvent.StateUpdated -> currentState = event.state
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
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
                    text = "REMOTE CONTROL",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Mobile / Tablet Wireless Handheld Controller",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            // Connection Badge
            Box(
                modifier = Modifier
                    .background(
                        if (sessionToken != null) Color(0xFF10B981) else Color(0xFFEF4444),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (sessionToken != null) "PAIRED & CONNECTED" else "DISCONNECTED",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (sessionToken == null) {
            // UNPAIRED STATE - ENTER PIN SCREEN
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Pair Remote Controller",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter the 4-digit PIN displayed on the main Church Presentation Console settings screen.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    // PIN Info box for easy testing
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2B2930), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Server PIN Code: $currentPin",
                            color = Color(0xFFD0BCFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedTextField(
                        value = inputPin,
                        onValueChange = { if (it.length <= 4) inputPin = it },
                        label = { Text("4-Digit PIN Code", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.width(200.dp)
                    )

                    errorMessage?.let { err ->
                        Text(err, color = Color.Red, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val token = remoteEngine.authenticatePairing(inputPin)
                            if (token != null) {
                                sessionToken = token
                                errorMessage = null
                            } else {
                                errorMessage = "Invalid PIN. Please try again."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF)),
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Text("Connect Remote", color = Color(0xFF381E72), fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // PAIRED STATE - CONTROL SURFACE
            val token = sessionToken!!
            val state = currentState

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live Screen Feedback Monitor Card
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
                            Text(
                                text = "LIVE STATUS",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .background(
                                        when (state?.status) {
                                            PresentationStatus.BLACK -> Color.DarkGray
                                            PresentationStatus.CLEAR -> Color.Blue
                                            PresentationStatus.IDLE -> Color.Gray
                                            else -> Color(0xFF10B981)
                                        },
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = state?.status?.name ?: "UNKNOWN",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val title = state?.currentContent?.title ?: "No Live Presentation"
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Current Slide Line
                        val currentSlideText = when (val c = state?.currentContent) {
                            is LyricsContent -> {
                                val idx = state.currentSlideIndex.coerceIn(0, (c.slides.size - 1).coerceAtLeast(0))
                                c.slides.getOrNull(idx) ?: ""
                            }
                            is PowerPointContent -> "Slide ${state.currentSlideIndex + 1} of ${c.slides.size}"
                            else -> "Live Content Active"
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = currentSlideText.ifBlank { "Screen Clear" },
                                color = Color(0xFFD0BCFF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Quick Navigation Grid
                val slideCount = when (val c = state?.currentContent) {
                    is LyricsContent -> c.slides.size
                    is PowerPointContent -> c.slides.size
                    else -> 0
                }

                if (slideCount > 0) {
                    Text(
                        text = "SLIDE SELECTOR",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(slideCount) { idx ->
                            val isSelected = state?.currentSlideIndex == idx
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1.5f)
                                    .background(
                                        if (isSelected) Color(0xFF381E72) else Color(0xFF2B2930),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFFD0BCFF) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        remoteEngine.sendCommand(token, RemoteCommand.JumpToSlide(idx))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "0${idx + 1}",
                                    color = if (isSelected) Color(0xFFD0BCFF) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Big Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { remoteEngine.sendCommand(token, RemoteCommand.PreviousSlide) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49454F)),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("◀ PREV", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { remoteEngine.sendCommand(token, RemoteCommand.NextSlide) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("NEXT ▶", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { remoteEngine.sendCommand(token, RemoteCommand.BlackOut) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("BLACK", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { remoteEngine.sendCommand(token, RemoteCommand.ClearDisplay) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("CLEAR", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

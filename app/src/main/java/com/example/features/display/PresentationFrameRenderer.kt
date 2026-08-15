package com.example.features.display

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.features.camera.CameraPreview
import com.example.features.lyrics.LyricsStylePreset
import com.example.features.video.VideoPlayer
import com.example.model.*
import com.example.presentation.BackgroundType
import com.example.presentation.PresentationState
import com.example.presentation.PresentationStatus

import com.example.features.display.DisplayProfileType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PresentationFrameRenderer(
    state: PresentationState,
    profileType: DisplayProfileType = DisplayProfileType.MAIN_PROJECTOR,
    modifier: Modifier = Modifier
) {
    if (state.status == PresentationStatus.BLACK) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        )
        return
    }

    if (profileType == DisplayProfileType.STAGE_MONITOR) {
        // Specialized Stage Confidence Monitor layout for worship team/speaker
        StageMonitorRenderer(state = state, modifier = modifier)
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // LAYER 1: Background
        when (state.backgroundType) {
            BackgroundType.IMAGE -> {
                state.backgroundImageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Background Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            BackgroundType.VIDEO -> {
                state.backgroundVideoUri?.let { uri ->
                    VideoPlayer(
                        videoUri = uri,
                        isPlaying = state.isVideoPlaying,
                        isLooping = true,
                        isMuted = true
                    )
                }
            }
            BackgroundType.CAMERA -> {
                CameraPreview()
            }
            BackgroundType.IP_CAMERA -> {
                state.backgroundVideoUri?.let { uri ->
                    VideoPlayer(
                        videoUri = uri,
                        isPlaying = true,
                        isLooping = true,
                        isMuted = true
                    )
                }
            }
            BackgroundType.NONE -> {
                // Default dark gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
                            )
                        )
                )
            }
        }

        // LAYER 2: Overlay / Main Content (If not CLEAR)
        if (state.status != PresentationStatus.CLEAR) {
            val content = state.currentContent

            val effectivePosition = if (profileType == DisplayProfileType.LOWER_THIRD) {
                com.example.presentation.TextDisplayPosition.LOWER_THIRD
            } else {
                state.textPosition
            }

            val liveTextStyle = androidx.compose.ui.text.TextStyle(
                color = Color(state.textColorRgb),
                fontSize = state.fontSizeSp.sp,
                fontWeight = if (state.isTextBold) FontWeight.Bold else FontWeight.Normal,
                fontFamily = state.stylePreset.fontFamily,
                textAlign = state.textAlignment.value,
                lineHeight = (state.fontSizeSp * 1.3f).sp,
                shadow = if (state.isTextShadowEnabled) {
                    androidx.compose.ui.graphics.Shadow(
                        color = Color.Black,
                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                        blurRadius = 4f
                    )
                } else null
            )

            when (content) {
                is LyricsContent -> {
                    val slides = content.slides
                    if (slides.isNotEmpty()) {
                        val index = state.currentSlideIndex.coerceIn(0, slides.size - 1)
                        val slideText = slides[index]

                        when (effectivePosition) {
                            com.example.presentation.TextDisplayPosition.LOWER_THIRD -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.BottomStart
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = state.textBackgroundAlpha.coerceAtLeast(0.6f)), RoundedCornerShape(8.dp))
                                            .padding(16.dp)
                                    ) {
                                        Text(text = slideText, style = liveTextStyle, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                            com.example.presentation.TextDisplayPosition.BOTTOM_CENTER -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = state.textBackgroundAlpha.coerceAtLeast(0.5f)), RoundedCornerShape(8.dp))
                                            .padding(16.dp)
                                    ) {
                                        Text(text = slideText, style = liveTextStyle, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                            com.example.presentation.TextDisplayPosition.TOP_BANNER -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = state.textBackgroundAlpha.coerceAtLeast(0.5f)), RoundedCornerShape(8.dp))
                                            .padding(16.dp)
                                    ) {
                                        Text(text = slideText, style = liveTextStyle, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                            com.example.presentation.TextDisplayPosition.LEFT_CENTER -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .background(Color.Black.copy(alpha = state.textBackgroundAlpha), RoundedCornerShape(8.dp))
                                            .padding(20.dp)
                                    ) {
                                        Text(text = slideText, style = liveTextStyle, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                            com.example.presentation.TextDisplayPosition.CENTER -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = state.textBackgroundAlpha))
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = slideText, style = liveTextStyle)
                                }
                            }
                        }
                    }
                }
                is com.example.model.BibleContent -> {
                    val verses = content.verses
                    if (verses.isNotEmpty()) {
                        val index = state.currentSlideIndex.coerceIn(0, verses.size - 1)
                        val verseText = verses[index]

                        when (effectivePosition) {
                            com.example.presentation.TextDisplayPosition.LOWER_THIRD,
                            com.example.presentation.TextDisplayPosition.BOTTOM_CENTER -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = state.textBackgroundAlpha.coerceAtLeast(0.65f)), RoundedCornerShape(8.dp))
                                            .padding(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFD0BCFF), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = content.title,
                                                color = Color(0xFF381E72),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = verseText, style = liveTextStyle, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                            com.example.presentation.TextDisplayPosition.TOP_BANNER -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = state.textBackgroundAlpha.coerceAtLeast(0.65f)), RoundedCornerShape(8.dp))
                                            .padding(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFD0BCFF), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = content.title,
                                                color = Color(0xFF381E72),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = verseText, style = liveTextStyle, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = state.textBackgroundAlpha))
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFD0BCFF), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = content.title,
                                                color = Color(0xFF381E72),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(text = verseText, style = liveTextStyle)
                                    }
                                }
                            }
                        }
                    }
                }
                is ImageContent -> {
                    AsyncImage(
                        model = content.uri,
                        contentDescription = content.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is VideoContent -> {
                    VideoPlayer(
                        videoUri = content.uri,
                        isPlaying = state.isVideoPlaying,
                        isLooping = false,
                        isMuted = false
                    )
                }
                is CameraContent -> {
                    CameraPreview()
                }
                is IpCameraContent -> {
                    VideoPlayer(
                        videoUri = content.streamUrl,
                        isPlaying = true,
                        isLooping = true,
                        isMuted = false
                    )
                }
                is PowerPointContent -> {
                    val slides = content.slides
                    if (slides.isNotEmpty()) {
                        val index = state.currentSlideIndex.coerceIn(0, slides.size - 1)
                        AsyncImage(
                            model = slides[index],
                            contentDescription = "PowerPoint Slide",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                null -> {
                    // Idle default banner
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Church Presentation System",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StageMonitorRenderer(
    state: PresentationState,
    modifier: Modifier = Modifier
) {
    val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    val currentSlideText = when (val c = state.currentContent) {
        is LyricsContent -> {
            val idx = state.currentSlideIndex.coerceIn(0, (c.slides.size - 1).coerceAtLeast(0))
            c.slides.getOrNull(idx) ?: ""
        }
        is PowerPointContent -> "Slide ${state.currentSlideIndex + 1} of ${c.slides.size}"
        else -> state.currentContent?.title ?: "STAGE CONFIDENCE MONITOR"
    }

    val nextSlideText = when (val c = state.currentContent) {
        is LyricsContent -> {
            val nextIdx = state.currentSlideIndex + 1
            if (nextIdx < c.slides.size) c.slides[nextIdx] else "[END OF SONG]"
        }
        is PowerPointContent -> {
            val nextIdx = state.currentSlideIndex + 1
            if (nextIdx < c.slides.size) "Slide ${nextIdx + 1}" else "[END OF DECK]"
        }
        else -> ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Header info bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("STAGE MONITOR", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("TIME: $currentTime", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Current Slide Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f)
                .background(Color(0xFF121212), RoundedCornerShape(8.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentSlideText,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Next Slide Preview Box
        if (nextSlideText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.8f)
                    .background(Color(0xFF1E1E24), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text("NEXT SLIDE:", color = Color.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = nextSlideText,
                        color = Color.LightGray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

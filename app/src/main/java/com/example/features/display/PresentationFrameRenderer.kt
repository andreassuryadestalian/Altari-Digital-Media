package com.example.features.display

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
        StageMonitorRenderer(state = state, modifier = modifier)
        return
    }

    if (state.isSplitScreenEnabled) {
        // Dual Split Screen (30:70 / 70:30 Live Cam + Sermon Presentation)
        val camWeight = (state.splitRatioCamPercent.coerceIn(15, 85) / 100f)
        val contentWeight = 1f - camWeight

        Row(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF05050A))
        ) {
            if (state.splitScreenSide == com.example.presentation.SplitScreenSide.CAM_LEFT_CONTENT_RIGHT) {
                // Live Cam on Left
                Box(
                    modifier = Modifier
                        .weight(camWeight)
                        .fillMaxHeight()
                        .background(Color.Black)
                ) {
                    SplitCameraFeedView(state)
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(Brush.verticalGradient(listOf(Color(0xFFD0BCFF), Color(0xFF38BDF8), Color(0xFFD0BCFF))))
                )

                // Sermon Material on Right
                Box(
                    modifier = Modifier
                        .weight(contentWeight)
                        .fillMaxHeight()
                ) {
                    SinglePresentationContent(state = state, profileType = profileType)
                }
            } else {
                // Sermon Material on Left
                Box(
                    modifier = Modifier
                        .weight(contentWeight)
                        .fillMaxHeight()
                ) {
                    SinglePresentationContent(state = state, profileType = profileType)
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(Brush.verticalGradient(listOf(Color(0xFFD0BCFF), Color(0xFF38BDF8), Color(0xFFD0BCFF))))
                )

                // Live Cam on Right
                Box(
                    modifier = Modifier
                        .weight(camWeight)
                        .fillMaxHeight()
                        .background(Color.Black)
                ) {
                    SplitCameraFeedView(state)
                }
            }
        }
    } else {
        // Normal Single Presentation Screen
        SinglePresentationContent(state = state, profileType = profileType, modifier = modifier)
    }
}

@Composable
private fun SplitCameraFeedView(state: PresentationState) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (state.splitCameraSourceType == BackgroundType.IP_CAMERA && !state.splitCameraStreamUrl.isNullOrBlank()) {
            VideoPlayer(
                videoUri = state.splitCameraStreamUrl,
                isPlaying = true,
                isLooping = true,
                isMuted = true
            )
        } else if (state.backgroundType == BackgroundType.IP_CAMERA && !state.backgroundVideoUri.isNullOrBlank()) {
            VideoPlayer(
                videoUri = state.backgroundVideoUri,
                isPlaying = true,
                isLooping = true,
                isMuted = true
            )
        } else {
            CameraPreview()
        }

        // Live Cam Tag
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color(0xFFDC2626).copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color.White, RoundedCornerShape(3.dp))
                )
                Text(
                    text = "LIVE CAM",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun SinglePresentationContent(
    state: PresentationState,
    profileType: DisplayProfileType = DisplayProfileType.MAIN_PROJECTOR,
    modifier: Modifier = Modifier
) {
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
                lineHeight = (state.fontSizeSp * state.textLineHeightMultiplier).sp,
                shadow = if (state.isTextShadowEnabled) {
                    androidx.compose.ui.graphics.Shadow(
                        color = Color.Black,
                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                        blurRadius = 6f
                    )
                } else null
            )

            when (content) {
                is LyricsContent -> {
                    val slides = content.slides
                    if (slides.isNotEmpty()) {
                        val index = state.currentSlideIndex.coerceIn(0, slides.size - 1)
                        val slideText = if (state.isTextUppercase) slides[index].uppercase() else slides[index]

                        RenderFlexibleTextBox(
                            text = slideText,
                            title = null,
                            position = effectivePosition,
                            state = state,
                            textStyle = liveTextStyle
                        )
                    }
                }
                is com.example.model.BibleContent -> {
                    val verses = content.verses
                    if (verses.isNotEmpty()) {
                        val index = state.currentSlideIndex.coerceIn(0, verses.size - 1)
                        val verseText = if (state.isTextUppercase) verses[index].uppercase() else verses[index]

                        RenderFlexibleTextBox(
                            text = verseText,
                            title = content.title,
                            position = effectivePosition,
                            state = state,
                            textStyle = liveTextStyle
                        )
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
private fun RenderFlexibleTextBox(
    text: String,
    title: String?,
    position: com.example.presentation.TextDisplayPosition,
    state: PresentationState,
    textStyle: androidx.compose.ui.text.TextStyle
) {
    val boxShape = RoundedCornerShape(state.textBoxCornerRadiusDp.dp)
    val boxPadding = state.textBoxPaddingDp.dp
    val borderStroke = if (state.textBoxBorderEnabled) {
        androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD0BCFF).copy(alpha = 0.7f))
    } else null

    val (alignment, widthFraction) = when (position) {
        com.example.presentation.TextDisplayPosition.CENTER -> Alignment.Center to (state.textBoxWidthPercent / 100f)
        com.example.presentation.TextDisplayPosition.LOWER_THIRD -> Alignment.BottomCenter to (state.textBoxWidthPercent / 100f).coerceAtLeast(0.92f)
        com.example.presentation.TextDisplayPosition.BOTTOM_CENTER -> Alignment.BottomCenter to (state.textBoxWidthPercent / 100f)
        com.example.presentation.TextDisplayPosition.TOP_BANNER -> Alignment.TopCenter to (state.textBoxWidthPercent / 100f).coerceAtLeast(0.92f)
        com.example.presentation.TextDisplayPosition.LEFT_CENTER -> Alignment.CenterStart to (state.textBoxWidthPercent / 100f).coerceAtMost(0.85f)
        com.example.presentation.TextDisplayPosition.RIGHT_CENTER -> Alignment.CenterEnd to (state.textBoxWidthPercent / 100f).coerceAtMost(0.85f)
        com.example.presentation.TextDisplayPosition.TOP_LEFT -> Alignment.TopStart to (state.textBoxWidthPercent / 100f).coerceAtMost(0.85f)
        com.example.presentation.TextDisplayPosition.TOP_RIGHT -> Alignment.TopEnd to (state.textBoxWidthPercent / 100f).coerceAtMost(0.85f)
        com.example.presentation.TextDisplayPosition.BOTTOM_LEFT -> Alignment.BottomStart to (state.textBoxWidthPercent / 100f).coerceAtMost(0.85f)
        com.example.presentation.TextDisplayPosition.BOTTOM_RIGHT -> Alignment.BottomEnd to (state.textBoxWidthPercent / 100f).coerceAtMost(0.85f)
        com.example.presentation.TextDisplayPosition.CUSTOM -> {
            val biasX = ((state.textHorizontalPercent - 50) / 50f).coerceIn(-1f, 1f)
            val biasY = ((state.textVerticalPercent - 50) / 50f).coerceIn(-1f, 1f)
            androidx.compose.ui.BiasAlignment(biasX, biasY) to (state.textBoxWidthPercent / 100f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .then(
                    if (borderStroke != null) Modifier.border(borderStroke, boxShape) else Modifier
                )
                .background(Color.Black.copy(alpha = state.textBackgroundAlpha), boxShape)
                .padding(boxPadding),
            horizontalAlignment = when (state.textAlignment) {
                com.example.presentation.TextAlignmentOption.LEFT -> Alignment.Start
                com.example.presentation.TextAlignmentOption.RIGHT -> Alignment.End
                else -> Alignment.CenterHorizontally
            }
        ) {
            if (!title.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFD0BCFF), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = title,
                        color = Color(0xFF381E72),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = text,
                style = textStyle,
                modifier = Modifier.fillMaxWidth()
            )
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

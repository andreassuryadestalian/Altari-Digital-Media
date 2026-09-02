package com.example.features.display

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.togetherWith
import kotlin.math.abs
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

    Box(modifier = modifier.fillMaxSize()) {
        if (state.isSplitScreenEnabled) {
            // Dual Split Screen (30:70 / 70:30 Live Cam + Sermon Presentation)
            val camWeight = (state.splitRatioCamPercent.coerceIn(15, 85) / 100f)
            val contentWeight = 1f - camWeight

            Row(
                modifier = Modifier
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
            SinglePresentationContent(state = state, profileType = profileType, modifier = Modifier.fillMaxSize())
        }

        // Ticker Overlay
        if (state.isTickerVisible && !state.tickerText.isNullOrBlank() && profileType != DisplayProfileType.STAGE_MONITOR) {
            TickerBar(
                text = state.tickerText,
                backgroundColorRgb = state.tickerBackgroundColorRgb,
                textColorRgb = state.tickerTextColorRgb,
                speed = state.tickerSpeed,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TickerBar(
    text: String,
    backgroundColorRgb: Long,
    textColorRgb: Long,
    speed: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(backgroundColorRgb))
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = Color(textColorRgb),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    velocity = speed.dp,
                    initialDelayMillis = 0
                )
                .padding(horizontal = 16.dp)
        )
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
            HeadlessCameraRenderer()
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
                HeadlessCameraRenderer()
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
                    val slides = content.getEffectiveSlides(state.lyricsDisplayMode)
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
                    HeadlessCameraRenderer()
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
                            text = "Altari Digital",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
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
            androidx.compose.animation.AnimatedContent(
                targetState = text,
                transitionSpec = {
                    androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) togetherWith
                    androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
                },
                label = "TextAnimation"
            ) { targetText ->
                Text(
                    text = targetText,
                    style = textStyle,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


@Composable
fun StageMonitorRenderer(
    state: PresentationState,
    modifier: Modifier = Modifier
) {
    var currentTime by remember {
        mutableStateOf(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    val currentSlideText = when (val c = state.currentContent) {
        is LyricsContent -> {
            val slides = c.getEffectiveSlides(state.lyricsDisplayMode)
            val idx = state.currentSlideIndex.coerceIn(0, (slides.size - 1).coerceAtLeast(0))
            slides.getOrNull(idx) ?: ""
        }
        is BibleContent -> {
            val verses = c.verses
            val idx = state.currentSlideIndex.coerceIn(0, (verses.size - 1).coerceAtLeast(0))
            verses.getOrNull(idx) ?: ""
        }
        is PowerPointContent -> "Slide ${state.currentSlideIndex + 1} of ${c.slides.size}"
        else -> state.currentContent?.title ?: "STAGE CONFIDENCE MONITOR"
    }

    val nextSlideText = when (val c = state.currentContent) {
        is LyricsContent -> {
            val slides = c.getEffectiveSlides(state.lyricsDisplayMode)
            val nextIdx = state.currentSlideIndex + 1
            if (nextIdx < slides.size) slides[nextIdx] else "[AKHIR LAGU / END OF SONG]"
        }
        is BibleContent -> {
            val nextIdx = state.currentSlideIndex + 1
            if (nextIdx < c.verses.size) c.verses[nextIdx] else "[AKHIR BACAAN]"
        }
        is PowerPointContent -> {
            val nextIdx = state.currentSlideIndex + 1
            if (nextIdx < c.slides.size) "Slide ${nextIdx + 1}" else "[END OF DECK]"
        }
        else -> ""
    }

    // Format Sermon Timer
    val remainingSecs = state.sermonTimerRemainingSeconds
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
        !state.sermonTimerRunning && remainingSecs == state.sermonTimerTotalSeconds -> Color(0xFF94A3B8)
        isOvertime -> Color(0xFFEF4444) // Overtime RED
        remainingSecs <= 300 -> Color(0xFFF59E0B) // <= 5 min AMBER WARNING
        else -> Color(0xFF10B981) // Normal GREEN
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F12))
            .padding(12.dp)
    ) {
        // Top Bar: Clock, Sermon Timer & Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF18181B), RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Realtime Clock
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🕒", fontSize = 16.sp)
                Column {
                    Text("REALTIME CLOCK", color = Color(0xFF9CA3AF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(currentTime, color = Color(0xFFFACC15), fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
            }

            // Sermon Countdown Timer Widget
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = when {
                                isOvertime -> "⚠️ OVERTIME"
                                state.sermonTimerRunning -> "⏱️ KHOTBAH (LIVE)"
                                else -> "⏸️ KHOTBAH (PAUSED)"
                            },
                            color = timerColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = formattedTimer,
                        color = timerColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )
                }
            }
        }

        // Operator Flash Alert Banner
        if (state.isStageAlertActive && !state.stageAlertMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFDC2626), RoundedCornerShape(8.dp))
                    .border(2.dp, Color(0xFFFDE047), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📢 PESAN OPERATOR: ${state.stageAlertMessage}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Current Slide Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.4f)
                .background(Color(0xFF1E1E24), RoundedCornerShape(8.dp))
                .border(2.dp, Color(0xFF3B82F6), RoundedCornerShape(8.dp))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "▶ TAMPIL SAAT INI (${state.currentContent?.title ?: "No Content"})",
                        color = Color(0xFF60A5FA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SLIDE ${state.currentSlideIndex + 1}",
                        color = Color(0xFF60A5FA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentSlideText,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Next Slide Preview Box
        if (nextSlideText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f)
                    .background(Color(0xFF141416), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("⏭️ BERIKUTNYA (NEXT CUE):", color = Color(0xFFFBBF24), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = nextSlideText,
                        color = Color(0xFFD4D4D8),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HeadlessCameraRenderer() {
    val latestBitmap by com.example.features.camera.CameraStreamManager.latestImageBitmap.collectAsState()
    latestBitmap?.let { bmp ->
        androidx.compose.foundation.Image(
            bitmap = bmp,
            contentDescription = "Live Camera",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } ?: Box(modifier = Modifier.fillMaxSize().background(Color.Black))
}

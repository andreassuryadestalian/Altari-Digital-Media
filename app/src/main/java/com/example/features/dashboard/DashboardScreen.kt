package com.example.features.dashboard

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.features.bible.BibleManagementScreen
import com.example.features.display.PresentationFrameRenderer
import com.example.features.video.VideoPlayer
import com.example.features.lyrics.LyricsManagementScreen
import com.example.features.media.MediaManagementScreen
import com.example.features.playlist.PlaylistManagementScreen
import com.example.features.powerpoint.PowerPointManagementScreen
import com.example.features.settings.SettingsScreen
import com.example.features.stage.StageMonitorScreen
import com.example.model.*
import com.example.presentation.BackgroundType
import com.example.presentation.LyricsDisplayMode
import com.example.presentation.PresentationServer
import com.example.presentation.PresentationState
import com.example.presentation.PresentationStatus
import com.example.features.lyrics.LyricsStylePreset
import com.example.server.getLocalIpAddress

// Theme colors - Modern Studio Dark Palette
val BgMain = Color(0xFF0F172A)
val BgTopBar = Color(0xFF1E293B)
val BgPanel = Color(0xFF1E293B)
val BgSubtle = Color(0xFF0B1120)
val BorderDark = Color(0xFF334155)
val BorderLight = Color(0xFF475569)
val TextMain = Color(0xFFF8FAFC)
val TextMuted = Color(0xFF94A3B8)
val TextDim = Color(0xFF64748B)
val Emerald = Color(0xFF10B981)
val EmeraldLight = Color(0xFF34D399)
val EmeraldDark = Color(0xFF064E3B)
val Primary = Color(0xFFA78BFA)
val PrimaryDark = Color(0xFF3B1E72)
val Warning = Color(0xFFF59E0B)
val Danger = Color(0xFFEF4444)

enum class NavigationTab(val label: String, val icon: String) {
    DASHBOARD("CONSOLE", "📺"),
    STAGE("STAGE MONITOR", "⏱️"),
    LYRICS("SONGS", "🎵"),
    BIBLE("BIBLE", "📖"),
    POWERPOINT("SLIDES", "📊"),
    MEDIA("MEDIA", "🎥"),
    PLAYLIST("SERVICE ORDER", "📋"),
    SETTINGS("SETTINGS", "⚙️")
}

@Composable
fun DashboardScreen(server: PresentationServer) {
    val presentationState by server.state.collectAsState()
    var selectedTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Master state stored in Controller
    var previewContent by remember { mutableStateOf<PresentationContent?>(null) }
    var previewSlideIndex by remember { mutableStateOf(0) }

    // Library items state initialized from server single source of truth
    val songsLibrary = remember {
        mutableStateListOf<LyricsContent>().apply {
            addAll(server.songsLibrary)
        }
    }

    val mediaLibrary = remember {
        mutableStateListOf<PresentationContent>().apply {
            addAll(server.mediaLibrary)
        }
    }

    val pptLibrary = remember {
        mutableStateListOf<PowerPointContent>()
    }

    val playlists = remember {
        mutableStateListOf<ServicePlaylist>(
            ServicePlaylist(
                id = "p1",
                name = "Ibadah Minggu Pagi",
                items = mutableStateListOf<PresentationContent>().apply {
                    if (songsLibrary.isNotEmpty()) add(songsLibrary[0])
                    if (songsLibrary.size > 1) add(songsLibrary[1])
                }
            ),
            ServicePlaylist(
                id = "p2",
                name = "Ibadah Pemuda / Youth",
                items = mutableStateListOf<PresentationContent>().apply {
                    if (songsLibrary.size > 2) add(songsLibrary[2])
                }
            ),
            ServicePlaylist(
                id = "p3",
                name = "Ibadah Minggu Sore",
                items = mutableStateListOf()
            )
        )
    }

    var activePlaylistId by remember { mutableStateOf("p1") }
    val activePlaylist = playlists.find { it.id == activePlaylistId } ?: playlists.firstOrNull()

    var showDroidCamDialog by remember { mutableStateOf(false) }

    if (showDroidCamDialog) {
        AddDroidCamDialog(
            onDismiss = { showDroidCamDialog = false },
            onAdd = { newMedia ->
                mediaLibrary.add(newMedia)
                server.mediaLibrary.add(newMedia)
            }
        )
    }

    // Default preview item initialization
    LaunchedEffect(Unit) {
        if (previewContent == null && songsLibrary.isNotEmpty()) {
            previewContent = songsLibrary[0]
        }
    }

    // Synchronize preview selection with live state when live presentation changes
    LaunchedEffect(presentationState.currentContent, presentationState.currentSlideIndex) {
        val currentLive = presentationState.currentContent
        if (currentLive != null) {
            if (previewContent?.id == currentLive.id) {
                previewSlideIndex = presentationState.currentSlideIndex
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        TopBar(
            server = server,
            presentationState = presentationState,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            playlists = playlists,
            activePlaylistId = activePlaylistId,
            onSelectActivePlaylist = { activePlaylistId = it }
        )

        when (selectedTab) {
            NavigationTab.DASHBOARD -> {
                if (isLandscape) {
                    LandscapeOperatorConsole(
                        server = server,
                        presentationState = presentationState,
                        previewContent = previewContent,
                        previewSlideIndex = previewSlideIndex,
                        songsLibrary = songsLibrary,
                        playlists = playlists,
                        activePlaylistId = activePlaylistId,
                        onSelectActivePlaylist = { activePlaylistId = it },
                        mediaLibrary = mediaLibrary,
                        onOpenAddDroidCam = { showDroidCamDialog = true },
                        onSelectPreviewContent = { content ->
                            previewContent = content
                            previewSlideIndex = 0
                        },
                        onSelectPreviewSlide = { previewSlideIndex = it },
                        onGoLive = {
                            previewContent?.let { content ->
                                server.goSlide(content, previewSlideIndex)
                            }
                        }
                    )
                } else {
                    PortraitOperatorConsole(
                        server = server,
                        presentationState = presentationState,
                        previewContent = previewContent,
                        previewSlideIndex = previewSlideIndex,
                        songsLibrary = songsLibrary,
                        playlists = playlists,
                        activePlaylistId = activePlaylistId,
                        onSelectActivePlaylist = { activePlaylistId = it },
                        mediaLibrary = mediaLibrary,
                        onOpenAddDroidCam = { showDroidCamDialog = true },
                        onSelectPreviewContent = { content ->
                            previewContent = content
                            previewSlideIndex = 0
                        },
                        onSelectPreviewSlide = { previewSlideIndex = it },
                        onGoLive = {
                            previewContent?.let { content ->
                                server.goSlide(content, previewSlideIndex)
                            }
                        }
                    )
                }
            }
            NavigationTab.STAGE -> {
                StageMonitorScreen(server = server)
            }
            NavigationTab.LYRICS -> {
                LyricsManagementScreen(
                    songsList = songsLibrary,
                    onAddSong = { newSong ->
                        val index = songsLibrary.indexOfFirst { it.id == newSong.id }
                        if (index >= 0) {
                            songsLibrary[index] = newSong
                            val sIdx = server.songsLibrary.indexOfFirst { it.id == newSong.id }
                            if (sIdx >= 0) server.songsLibrary[sIdx] = newSong else server.songsLibrary.add(newSong)
                        } else {
                            songsLibrary.add(newSong)
                            server.songsLibrary.add(newSong)
                        }
                    },
                    onSelectForPreview = {
                        previewContent = it
                        previewSlideIndex = 0
                        selectedTab = NavigationTab.DASHBOARD
                    },
                    onSelectForGo = {
                        previewContent = it
                        previewSlideIndex = 0
                        server.go(it)
                        selectedTab = NavigationTab.DASHBOARD
                    },
                    onAddToPlaylist = { song ->
                        activePlaylist?.items?.add(song)
                    }
                )
            }
            NavigationTab.BIBLE -> {
                BibleManagementScreen(
                    onSelectForPreview = {
                        previewContent = it
                        previewSlideIndex = 0
                        selectedTab = NavigationTab.DASHBOARD
                    },
                    onSelectForGo = {
                        previewContent = it
                        previewSlideIndex = 0
                        server.go(it)
                        selectedTab = NavigationTab.DASHBOARD
                    },
                    onAddToPlaylist = { passage ->
                        activePlaylist?.items?.add(passage)
                    }
                )
            }
            NavigationTab.POWERPOINT -> {
                PowerPointManagementScreen(
                    pptList = pptLibrary,
                    onAddPowerPoint = { pptLibrary.add(it) },
                    onSelectForPreview = {
                        previewContent = it
                        previewSlideIndex = 0
                        selectedTab = NavigationTab.DASHBOARD
                    },
                    onSelectForGo = {
                        previewContent = it
                        previewSlideIndex = 0
                        server.go(it)
                        selectedTab = NavigationTab.DASHBOARD
                    }
                )
            }
            NavigationTab.MEDIA -> {
                MediaManagementScreen(
                    mediaList = mediaLibrary,
                    onAddMedia = {
                        mediaLibrary.add(it)
                        server.mediaLibrary.add(it)
                    },
                    onDeleteMedia = { item ->
                        mediaLibrary.remove(item)
                        server.mediaLibrary.remove(item)
                        if (previewContent?.id == item.id) {
                            previewContent = null
                        }
                    },
                    onSelectForPreview = {
                        previewContent = it
                        selectedTab = NavigationTab.DASHBOARD
                    },
                    onSelectForGo = {
                        previewContent = it
                        server.go(it)
                        selectedTab = NavigationTab.DASHBOARD
                    },
                    onSetBackgroundVideo = { server.setBackgroundVideo(it) },
                    onSetBackgroundImage = { server.setBackgroundImage(it) },
                    onSetBackgroundCamera = { server.setBackgroundCamera(it) }
                )
            }
            NavigationTab.PLAYLIST -> {
                PlaylistManagementScreen(
                    playlists = playlists,
                    activePlaylistId = activePlaylistId,
                    onSelectActivePlaylist = { activePlaylistId = it },
                    onCreatePlaylist = { name ->
                        val newId = "p_${System.currentTimeMillis()}"
                        playlists.add(ServicePlaylist(id = newId, name = name, items = mutableStateListOf()))
                        activePlaylistId = newId
                    },
                    onRenamePlaylist = { id, newName ->
                        playlists.find { it.id == id }?.name = newName
                    },
                    onDeletePlaylist = { id ->
                        if (playlists.size > 1) {
                            playlists.removeAll { it.id == id }
                            if (activePlaylistId == id) {
                                activePlaylistId = playlists.first().id
                            }
                        }
                    },
                    onRemoveItem = { playlistId, index ->
                        playlists.find { it.id == playlistId }?.items?.let { items ->
                            if (index in items.indices) items.removeAt(index)
                        }
                    },
                    onMoveItem = { playlistId, fromIdx, toIdx ->
                        playlists.find { it.id == playlistId }?.items?.let { items ->
                            if (fromIdx in items.indices && toIdx in items.indices) {
                                val item = items.removeAt(fromIdx)
                                items.add(toIdx, item)
                            }
                        }
                    },
                    onSelectForPreview = {
                        previewContent = it
                        previewSlideIndex = 0
                        selectedTab = NavigationTab.DASHBOARD
                    },
                    onSelectForGo = {
                        previewContent = it
                        previewSlideIndex = 0
                        server.go(it)
                        selectedTab = NavigationTab.DASHBOARD
                    }
                )
            }
            NavigationTab.SETTINGS -> {
                SettingsScreen(
                    server = server,
                    onSetPreset = { server.setStylePreset(it) },
                    onClearBackground = {
                        server.setBackgroundImage(null)
                        server.setBackgroundVideo(null)
                        server.setBackgroundCamera(false)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    server: PresentationServer,
    presentationState: PresentationState,
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    playlists: List<ServicePlaylist>,
    activePlaylistId: String,
    onSelectActivePlaylist: (String) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showTickerDialog by remember { mutableStateOf(false) }
    var showWebRemoteDialog by remember { mutableStateOf(false) }
    val activePlaylist = playlists.find { it.id == activePlaylistId } ?: playlists.firstOrNull()
    val localIp = remember(server) { getLocalIpAddress(server.context) }
    val port = server.webServer.activePort

    if (showTickerDialog) {
        TickerDialog(
            server = server,
            presentationState = presentationState,
            onDismiss = { showTickerDialog = false }
        )
    }

    if (showWebRemoteDialog) {
        WebRemoteDialog(
            server = server,
            onDismiss = { showWebRemoteDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgTopBar)
    ) {
        // Status Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.altari_digital_icon_1787294223180),
                    contentDescription = "Altari Digital Logo",
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Emerald, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ALTARI DIGITAL",
                            color = TextMain,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "LIVE WEB DISPLAY: http://$localIp:$port",
                        color = EmeraldLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Ticker Quick Action Pill
                Surface(
                    color = if (presentationState.isTickerVisible) Color(0xFFDC2626) else Color(0xFF27272A),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable { showTickerDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (presentationState.isTickerVisible) "🏃 Ticker ON" else "🏃 Ticker OFF",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Sermon Timer Quick Action Pill
                val timerSecs = presentationState.sermonTimerRemainingSeconds
                val isTimerOvertime = timerSecs < 0
                val absSecs = kotlin.math.abs(timerSecs)
                val tMins = (absSecs % 3600) / 60
                val tSecs = absSecs % 60
                val formattedTimerStr = String.format(java.util.Locale.US, "%s%02d:%02d", if (isTimerOvertime) "+" else "", tMins, tSecs)
                val timerBadgeColor = when {
                    isTimerOvertime -> Color(0xFFDC2626)
                    timerSecs <= 300 -> Color(0xFFD97706)
                    presentationState.sermonTimerRunning -> Color(0xFF059669)
                    else -> Color(0xFF27272A)
                }

                Surface(
                    color = timerBadgeColor,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable { onTabSelected(NavigationTab.STAGE) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (presentationState.sermonTimerRunning) "⏱️ $formattedTimerStr" else "⏸️ $formattedTimerStr",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Split Screen Quick Toggle Pill
                Surface(
                    color = if (presentationState.isSplitScreenEnabled) Color(0xFF7C3AED) else Color(0xFF27272A),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable { server.toggleSplitScreen() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (presentationState.isSplitScreenEnabled) "🔲 Split ON (${presentationState.splitRatioCamPercent}:${100 - presentationState.splitRatioCamPercent})" else "🔲 Split OFF",
                            color = if (presentationState.isSplitScreenEnabled) Color(0xFFD0BCFF) else Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Web Remote Quick Action Pill
                Surface(
                    color = Color(0xFF0D9488),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable { showWebRemoteDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "📱 Remote",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box {
                    Surface(
                        color = Color(0xFF381E72),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { dropdownExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "📋 ${activePlaylist?.name ?: "PLAYLIST"}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("▾", color = Color(0xFFD0BCFF), fontSize = 11.sp)
                        }
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(Color(0xFF25232A))
                    ) {
                    playlists.forEach { pl ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = pl.name,
                                    color = if (pl.id == activePlaylistId) Color(0xFFD0BCFF) else Color.White,
                                    fontWeight = if (pl.id == activePlaylistId) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            onClick = {
                                onSelectActivePlaylist(pl.id)
                                dropdownExpanded = false
                            }
                        )
                    }
                    HorizontalDivider(color = Color(0xFF49454F))
                    DropdownMenuItem(
                        text = {
                            Text("⚙️ Kelola Semua Playlist...", color = Color(0xFFD0BCFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        },
                        onClick = {
                            onTabSelected(NavigationTab.PLAYLIST)
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }
    }

        // Scrollable Navigation Bar (Zero Overlap!)
        ScrollableTabRow(
            selectedTabIndex = NavigationTab.values().indexOf(selectedTab),
            containerColor = BgTopBar,
            contentColor = Primary,
            edgePadding = 12.dp,
            divider = {}
        ) {
            NavigationTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(tab.icon, fontSize = 12.sp)
                            Text(
                                text = tab.label,
                                color = if (isSelected) Primary else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                )
            }
        }
    }
}

/**
 * Professional Widescreen Operator Console for Landscape Mode (ProPresenter Style)
 */
@Composable
fun LandscapeOperatorConsole(
    server: PresentationServer,
    presentationState: PresentationState,
    previewContent: PresentationContent?,
    previewSlideIndex: Int,
    songsLibrary: List<LyricsContent>,
    playlists: List<ServicePlaylist>,
    activePlaylistId: String,
    onSelectActivePlaylist: (String) -> Unit,
    mediaLibrary: List<PresentationContent>,
    onOpenAddDroidCam: () -> Unit,
    onSelectPreviewContent: (PresentationContent) -> Unit,
    onSelectPreviewSlide: (Int) -> Unit,
    onGoLive: () -> Unit
) {
    val activePlaylist = playlists.find { it.id == activePlaylistId } ?: playlists.firstOrNull()
    val activePlaylistItems = activePlaylist?.items ?: emptyList()

    var consoleLeftTab by remember { mutableIntStateOf(0) } // 0: Order, 1: Media/BG, 2: Songs

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // PANEL 1: LEFT SIDE - Service Order & Media Picker
        Card(
            colors = CardDefaults.cardColors(containerColor = BgPanel),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Segmented Tab Selector for Panel 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1B24), RoundedCornerShape(6.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val tabs = listOf("📋 Order", "🎬 Media/BG", "🎵 Songs")
                    tabs.forEachIndexed { idx, label ->
                        val isSelected = consoleLeftTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) Color(0xFF381E72) else Color.Transparent)
                                .clickable { consoleLeftTab = idx },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFFD0BCFF) else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (consoleLeftTab == 1) { // MEDIA & BACKGROUND CONSOLE
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Live Background Monitor Badge
                        Surface(
                            color = when (presentationState.backgroundType) {
                                BackgroundType.IP_CAMERA -> Color(0xFF381E72)
                                BackgroundType.VIDEO -> Color(0xFF1E3A8A)
                                BackgroundType.IMAGE -> Color(0xFF065F46)
                                BackgroundType.CAMERA -> Color(0xFF831843)
                                BackgroundType.NONE -> Color(0xFF1F2937)
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ACTIVE BACKGROUND", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = when (presentationState.backgroundType) {
                                            BackgroundType.IP_CAMERA -> "📱 DroidCam Stream (${presentationState.backgroundVideoUri ?: ""})"
                                            BackgroundType.VIDEO -> "🎥 Video Background"
                                            BackgroundType.IMAGE -> "🖼 Image Background"
                                            BackgroundType.CAMERA -> "📷 Local Camera Overlay"
                                            BackgroundType.NONE -> "⚪ Default Dark Gradient"
                                        },
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (presentationState.backgroundType != BackgroundType.NONE) {
                                    Surface(
                                        color = Color.Red.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.clickable {
                                            server.setBackgroundVideo(null)
                                            server.setBackgroundImage(null)
                                            server.setBackgroundCamera(false)
                                        }
                                    ) {
                                        Text(
                                            "❌ CLEAR",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Header & Add DroidCam Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("QUICK MEDIA LIST", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                            Surface(
                                color = Color(0xFF10B981),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.clickable { onOpenAddDroidCam() }
                            ) {
                                Text(
                                    "+ DroidCam",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Media Items List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(mediaLibrary) { media ->
                                val isCurrentBg = when (media) {
                                    is IpCameraContent -> presentationState.backgroundType == BackgroundType.IP_CAMERA && presentationState.backgroundVideoUri == media.streamUrl
                                    is VideoContent -> presentationState.backgroundType == BackgroundType.VIDEO && presentationState.backgroundVideoUri == media.uri
                                    is ImageContent -> presentationState.backgroundType == BackgroundType.IMAGE && presentationState.backgroundImageUri == media.uri
                                    is CameraContent -> presentationState.backgroundType == BackgroundType.CAMERA
                                    else -> false
                                }

                                Surface(
                                    color = if (isCurrentBg) Color(0xFF381E72) else Color(0xFF1C1B1F),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isCurrentBg) Color(0xFFD0BCFF) else BorderDark
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = when (media) {
                                                    is IpCameraContent -> "📱 ${media.title}"
                                                    is VideoContent -> "🎥 ${media.title}"
                                                    is ImageContent -> "🖼 ${media.title}"
                                                    is CameraContent -> "📷 ${media.title}"
                                                    else -> media.title
                                                },
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )

                                            if (isCurrentBg) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFF10B981), RoundedCornerShape(3.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text("BG LIVE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Action 1: SET BG
                                            Button(
                                                onClick = {
                                                    when (media) {
                                                        is IpCameraContent -> server.setBackgroundIpCamera(media.streamUrl)
                                                        is VideoContent -> server.setBackgroundVideo(media.uri)
                                                        is ImageContent -> server.setBackgroundImage(media.uri)
                                                        is CameraContent -> server.setBackgroundCamera(true)
                                                        else -> {}
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp).weight(1.1f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text("🖼 SET BG", color = Color(0xFFD0BCFF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // Action 2: PREVIEW
                                            Button(
                                                onClick = { onSelectPreviewContent(media) },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp).weight(1f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text("👁 PREV", color = Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // Action 3: GO LIVE MEDIA
                                            Button(
                                                onClick = { server.go(media) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp).weight(1f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text("▶ LIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (consoleLeftTab == 2) { // SONGS
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("QUICK SONG LIBRARY", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(songsLibrary) { song ->
                                Surface(
                                    color = Color(0xFF1C1B1F),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectPreviewContent(song) }
                                ) {
                                    Text(
                                        text = song.title,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                } else { // PLAYLIST / SERVICE ORDER
                    Column(modifier = Modifier.fillMaxSize()) {
                        var plDropdown by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SERVICE ORDER",
                                color = Primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Box {
                                Surface(
                                    color = Color(0xFF381E72),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.clickable { plDropdown = true }
                                ) {
                                    Text(
                                        text = "${activePlaylist?.name} ▾",
                                        color = Color(0xFFD0BCFF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .widthIn(max = 130.dp)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = plDropdown,
                                    onDismissRequest = { plDropdown = false },
                                    modifier = Modifier.background(Color(0xFF25232A))
                                ) {
                                    playlists.forEach { pl ->
                                        DropdownMenuItem(
                                            text = { Text(pl.name, color = Color.White, fontSize = 12.sp) },
                                            onClick = {
                                                onSelectActivePlaylist(pl.id)
                                                plDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(activePlaylistItems) { index, item ->
                                val isPreviewed = item.id == previewContent?.id
                                val isLive = item.id == presentationState.currentContent?.id

                                Surface(
                                    color = when {
                                        isLive -> Color(0xFF064E3B)
                                        isPreviewed -> PrimaryDark
                                        else -> Color(0xFF1C1B1F)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        when {
                                            isLive -> Emerald
                                            isPreviewed -> Primary
                                            else -> Color.Transparent
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectPreviewContent(item) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${index + 1}. ${item.title}",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isLive) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Emerald, RoundedCornerShape(3.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text("LIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // PANEL 2: MIDDLE - Interactive Slide Cue Matrix
        Card(
            colors = CardDefaults.cardColors(containerColor = BgPanel),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = previewContent?.title ?: "No Item Selected",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap slide to preview • Tap GO LIVE to project",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (previewContent is LyricsContent) {
                            Surface(
                                color = if (presentationState.lyricsDisplayMode == LyricsDisplayMode.PER_BARIS) Color(0xFF0D9488) else Color(0xFF381E72),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.clickable { server.toggleLyricsDisplayMode() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (presentationState.lyricsDisplayMode == LyricsDisplayMode.PER_BARIS) "🎶 Per Baris" else "🎶 Per Bait",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("⇄", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                }
                            }
                        }

                        Surface(
                            color = PrimaryDark,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "PREVIEW READY",
                                color = Primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val content = previewContent
                if (content is LyricsContent) {
                    val effectiveSlides = content.getEffectiveSlides(presentationState.lyricsDisplayMode)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 130.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(effectiveSlides) { idx, slide ->
                            val isPreviewed = idx == previewSlideIndex && content.id == previewContent.id
                            val isLiveContent = content.id == presentationState.currentContent?.id
                            val isLiveSlide = idx == presentationState.currentSlideIndex && isLiveContent

                            Box(
                                modifier = Modifier
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            isLiveSlide -> Color(0xFF064E3B)
                                            isPreviewed -> PrimaryDark
                                            else -> Color.Black.copy(alpha = 0.4f)
                                        }
                                    )
                                    .border(
                                        2.dp,
                                        when {
                                            isLiveSlide -> Emerald
                                            isPreviewed -> Primary
                                            else -> BorderDark
                                        },
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        onSelectPreviewSlide(idx)
                                        if (isLiveContent) {
                                            server.setSlideIndex(idx)
                                        }
                                    }
                                    .padding(8.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Slide ${idx + 1}",
                                            color = if (isLiveSlide) EmeraldLight else Primary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isLiveSlide) {
                                            Text("LIVE", color = Emerald, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = slide,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else if (content is BibleContent) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(content.verses) { idx, verse ->
                            val isPreviewed = idx == previewSlideIndex && content.id == previewContent.id
                            val isLiveContent = content.id == presentationState.currentContent?.id
                            val isLiveVerse = idx == presentationState.currentSlideIndex && isLiveContent

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        when {
                                            isLiveVerse -> Color(0xFF064E3B)
                                            isPreviewed -> PrimaryDark
                                            else -> Color.Black.copy(alpha = 0.4f)
                                        },
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            isLiveVerse -> Emerald
                                            isPreviewed -> Primary
                                            else -> BorderDark
                                        },
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        onSelectPreviewSlide(idx)
                                        if (isLiveContent) {
                                            server.setSlideIndex(idx)
                                        }
                                    }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Verse ${idx + 1}",
                                        color = if (isLiveVerse) EmeraldLight else Primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = verse,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                } else if (content is IpCameraContent) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            VideoPlayer(
                                videoUri = content.streamUrl,
                                isPlaying = true,
                                isLooping = true,
                                isMuted = true,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("📱 DROIDCAM LIVE STREAM", color = Color(0xFFD0BCFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { server.go(content) },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("▶ GO LIVE MEDIA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { server.setBackgroundIpCamera(content.streamUrl) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("🖼 SET BG STREAM", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                } else if (content is VideoContent) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                        ) {
                            VideoPlayer(
                                videoUri = content.uri,
                                isPlaying = true,
                                isLooping = true,
                                isMuted = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { server.go(content) },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("▶ GO LIVE VIDEO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { server.setBackgroundVideo(content.uri) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("🖼 SET BG VIDEO", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                } else if (content is ImageContent) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = content.uri,
                                contentDescription = content.title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { server.go(content) },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("▶ GO LIVE IMAGE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { server.setBackgroundImage(content.uri) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("🖼 SET BG IMAGE", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select playlist item or song to view slides", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        }

        // PANEL 3: RIGHT SIDE - Dual Output Monitors & Action Center
        Column(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // PREVIEW MONITOR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color.Black, RoundedCornerShape(8.dp))
                    .border(1.dp, Primary, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(PrimaryDark, RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("PREVIEW", color = Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(previewContent?.title ?: "None", color = Color.White, fontSize = 11.sp, maxLines = 1)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (val c = previewContent) {
                            is LyricsContent -> c.getEffectiveSlides(presentationState.lyricsDisplayMode).getOrNull(previewSlideIndex) ?: ""
                            is BibleContent -> c.verses.getOrNull(previewSlideIndex) ?: ""
                            is IpCameraContent -> "📱 DroidCam Stream: ${c.streamUrl}"
                            is VideoContent -> "🎥 Video: ${c.title}"
                            is ImageContent -> "🖼 Image: ${c.title}"
                            is CameraContent -> "📷 Local Camera Feed"
                            else -> "Ready"
                        },
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        maxLines = 3
                    )
                }
            }

            // PROGRAM LIVE MONITOR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.Black, RoundedCornerShape(8.dp))
                    .border(2.dp, Emerald, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                PresentationFrameRenderer(state = presentationState)
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .background(Emerald, RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("PROGRAM (LIVE)", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ACTION CENTER (Spacious, Un-cluttered Buttons)
            Card(
                colors = CardDefaults.cardColors(containerColor = BgPanel),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Big Prominent GO LIVE Button
                    Button(
                        onClick = onGoLive,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("GO LIVE ▶", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { server.previousSlide() },
                            colors = ButtonDefaults.buttonColors(containerColor = BorderDark),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("◀ PREV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { server.nextSlide() },
                            colors = ButtonDefaults.buttonColors(containerColor = BorderDark),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("NEXT ▶", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { server.black() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (presentationState.status == PresentationStatus.BLACK) Color.Red else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("◼ BLACK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { server.clear() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (presentationState.status == PresentationStatus.CLEAR) Color.Red else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("◻ CLEAR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Split Screen Toggle Button
                    Button(
                        onClick = { server.toggleSplitScreen() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (presentationState.isSplitScreenEnabled) Color(0xFF7C3AED) else Color(0xFF1E293B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (presentationState.isSplitScreenEnabled) "🔲 SPLIT SCREEN: ON" else "🔲 SPLIT SCREEN: OFF",
                            color = if (presentationState.isSplitScreenEnabled) Color(0xFFD0BCFF) else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Lyrics Mode Toggle
                        Button(
                            onClick = { server.toggleLyricsDisplayMode() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (presentationState.lyricsDisplayMode == LyricsDisplayMode.PER_BARIS) Color(0xFF0D9488) else Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (presentationState.lyricsDisplayMode == LyricsDisplayMode.PER_BARIS) "🎶 LIRIK: BARIS" else "🎶 LIRIK: BAIT",
                                color = if (presentationState.lyricsDisplayMode == LyricsDisplayMode.PER_BARIS) Color(0xFF99F6E4) else Color.LightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Camera Overlay Toggle
                        Button(
                            onClick = {
                                val isCamActive = presentationState.backgroundType == BackgroundType.CAMERA
                                server.setBackgroundCamera(!isCamActive)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (presentationState.backgroundType == BackgroundType.CAMERA) PrimaryDark else Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (presentationState.backgroundType == BackgroundType.CAMERA) "📷 CAM: ON" else "📷 CAM: OFF",
                                color = if (presentationState.backgroundType == BackgroundType.CAMERA) Primary else Color.LightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern Clean Portrait Operator Console
 */
@Composable
fun PortraitOperatorConsole(
    server: PresentationServer,
    presentationState: PresentationState,
    previewContent: PresentationContent?,
    previewSlideIndex: Int,
    songsLibrary: List<LyricsContent>,
    playlists: List<ServicePlaylist>,
    activePlaylistId: String,
    onSelectActivePlaylist: (String) -> Unit,
    mediaLibrary: List<PresentationContent>,
    onOpenAddDroidCam: () -> Unit,
    onSelectPreviewContent: (PresentationContent) -> Unit,
    onSelectPreviewSlide: (Int) -> Unit,
    onGoLive: () -> Unit
) {
    val activePlaylist = playlists.find { it.id == activePlaylistId } ?: playlists.firstOrNull()
    val activePlaylistItems = activePlaylist?.items ?: emptyList()

    var workspaceTab by remember { mutableIntStateOf(0) } // 0: Order, 1: Songs, 2: Media/BG
    var songSearchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        // SECTION 1: DUAL MONITORS (Preview & Program Output)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // PREVIEW MONITOR BOX
            Card(
                colors = CardDefaults.cardColors(containerColor = BgPanel),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.5.dp, Primary.copy(alpha = 0.6f)),
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = PrimaryDark,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "PREVIEW",
                                color = Primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = previewContent?.title ?: "No Item",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false).padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(BgSubtle, RoundedCornerShape(6.dp))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewText = when (val c = previewContent) {
                            is LyricsContent -> {
                                val slides = c.getEffectiveSlides(presentationState.lyricsDisplayMode)
                                slides.getOrNull(previewSlideIndex) ?: slides.firstOrNull() ?: ""
                            }
                            is BibleContent -> c.verses.getOrNull(previewSlideIndex) ?: c.verses.firstOrNull() ?: ""
                            is PowerPointContent -> "📊 Slide #${previewSlideIndex + 1}"
                            is VideoContent -> "🎬 Video: ${c.title}"
                            is ImageContent -> "🖼️ Gambar: ${c.title}"
                            is IpCameraContent -> "📷 Feed DroidCam: ${c.streamUrl}"
                            else -> "Pilih item dari playlist/library di bawah"
                        }
                        Text(
                            text = previewText,
                            color = TextMain,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // PROGRAM (LIVE) MONITOR BOX
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(2.dp, Emerald),
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    PresentationFrameRenderer(state = presentationState)

                    Surface(
                        color = Emerald,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text(
                            "LIVE PROGRAM",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // SECTION 2: MASTER LIVE ACTION DESK
        Card(
            colors = CardDefaults.cardColors(containerColor = BgPanel),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // GO LIVE BUTTON
                Button(
                    onClick = onGoLive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("▶ TAMPILKAN KE PROJECTOR (GO LIVE)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }

                // PRIMARY NAVIGATION & MUTE ACTIONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { server.previousSlide() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("◀ PREV", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { server.nextSlide() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("NEXT ▶", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { server.black() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (presentationState.status == PresentationStatus.BLACK) Danger else Color(0xFF1E293B)
                        ),
                        border = BorderStroke(1.dp, if (presentationState.status == PresentationStatus.BLACK) Danger else BorderDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text(
                            "◼ BLACK",
                            color = if (presentationState.status == PresentationStatus.BLACK) Color.White else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { server.clear() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (presentationState.status == PresentationStatus.CLEAR) Danger else Color(0xFF1E293B)
                        ),
                        border = BorderStroke(1.dp, if (presentationState.status == PresentationStatus.CLEAR) Danger else BorderDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text(
                            "◻ CLEAR",
                            color = if (presentationState.status == PresentationStatus.CLEAR) Color.White else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // QUICK STREAM TOGGLES
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Split Screen Toggle
                    Surface(
                        color = if (presentationState.isSplitScreenEnabled) PrimaryDark else Color(0xFF0F172A),
                        border = BorderStroke(1.dp, if (presentationState.isSplitScreenEnabled) Primary else BorderDark),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { server.toggleSplitScreen() }
                    ) {
                        Text(
                            text = if (presentationState.isSplitScreenEnabled) "🔲 Split: ON" else "🔲 Split: OFF",
                            color = if (presentationState.isSplitScreenEnabled) Primary else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }

                    // Lyrics 1 Baris / 1 Bait Toggle
                    Surface(
                        color = if (presentationState.lyricsDisplayMode == LyricsDisplayMode.PER_BARIS) Color(0xFF0D9488) else Color(0xFF0F172A),
                        border = BorderStroke(1.dp, if (presentationState.lyricsDisplayMode == LyricsDisplayMode.PER_BARIS) Color(0xFF2DD4BF) else BorderDark),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { server.toggleLyricsDisplayMode() }
                    ) {
                        Text(
                            text = if (presentationState.lyricsDisplayMode == LyricsDisplayMode.PER_BARIS) "🎶 1 Baris" else "🎶 1 Bait",
                            color = if (presentationState.lyricsDisplayMode == LyricsDisplayMode.PER_BARIS) Color.White else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }

                    // Camera Overlay Toggle
                    val isCam = presentationState.backgroundType == BackgroundType.CAMERA
                    Surface(
                        color = if (isCam) Color(0xFF831843) else Color(0xFF0F172A),
                        border = BorderStroke(1.dp, if (isCam) Color(0xFFF472B6) else BorderDark),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { server.setBackgroundCamera(!isCam) }
                    ) {
                        Text(
                            text = if (isCam) "📷 Cam: ON" else "📷 Cam: OFF",
                            color = if (isCam) Color.White else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // SECTION 3: WORKSPACE HUB (Segmented Tabs & Content Picker)
        Card(
            colors = CardDefaults.cardColors(containerColor = BgPanel),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Workspace Segmented Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgSubtle, RoundedCornerShape(8.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf("📋 Order Ibadah", "🎵 Lagu / Songs", "🎬 Media & BG")
                    tabs.forEachIndexed { index, label ->
                        val isSelected = workspaceTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) PrimaryDark else Color.Transparent)
                                .clickable { workspaceTab = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Primary else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                when (workspaceTab) {
                    // TAB 0: ORDER IBADAH (ACTIVE SERVICE PLAYLIST & SLIDE EXPANDER)
                    0 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Active Playlist Header & Dropdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Susunan: ${activePlaylist?.name}",
                                    color = TextMain,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "${activePlaylistItems.size} Item",
                                    color = TextDim,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                itemsIndexed(activePlaylistItems) { index, item ->
                                    val isItemPreviewed = previewContent?.id == item.id
                                    val isItemLive = presentationState.currentContent?.id == item.id

                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = when {
                                                isItemLive -> EmeraldDark
                                                isItemPreviewed -> PrimaryDark
                                                else -> BgSubtle
                                            }
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            when {
                                                isItemLive -> Emerald
                                                isItemPreviewed -> Primary
                                                else -> BorderDark
                                            }
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onSelectPreviewContent(item)
                                                onSelectPreviewSlide(0)
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        "${index + 1}.",
                                                        color = if (isItemLive) EmeraldLight else Primary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        item.title,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                if (isItemLive) {
                                                    Surface(
                                                        color = Emerald,
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            "LIVE",
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Black,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // If previewed or live, show clickable verse chips!
                                            if (isItemPreviewed || isItemLive) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                val slides = when (item) {
                                                    is LyricsContent -> item.getEffectiveSlides(presentationState.lyricsDisplayMode)
                                                    is BibleContent -> item.verses
                                                    else -> emptyList()
                                                }

                                                if (slides.isNotEmpty()) {
                                                    LazyRow(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        itemsIndexed(slides) { sIdx, slideText ->
                                                            val isSlideSelected = sIdx == previewSlideIndex && isItemPreviewed
                                                            val isSlideLive = sIdx == presentationState.currentSlideIndex && isItemLive

                                                            Surface(
                                                                color = when {
                                                                    isSlideLive -> Emerald
                                                                    isSlideSelected -> Primary
                                                                    else -> Color(0xFF334155)
                                                                },
                                                                shape = RoundedCornerShape(4.dp),
                                                                modifier = Modifier.clickable {
                                                                    onSelectPreviewSlide(sIdx)
                                                                    if (isItemLive) {
                                                                        server.setSlideIndex(sIdx)
                                                                    }
                                                                }
                                                            ) {
                                                                Text(
                                                                    text = "Slide ${sIdx + 1}",
                                                                    color = if (isSlideLive || isSlideSelected) Color.Black else Color.White,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 1: LAGU / SONG LIBRARY
                    1 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            OutlinedTextField(
                                value = songSearchQuery,
                                onValueChange = { songSearchQuery = it },
                                placeholder = { Text("🔍 Cari judul lagu / lirik...", color = TextDim, fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = BorderDark
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val filteredSongs = songsLibrary.filter {
                                it.title.contains(songSearchQuery, ignoreCase = true) ||
                                it.slides.any { s -> s.contains(songSearchQuery, ignoreCase = true) }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(filteredSongs) { song ->
                                    val isSelected = previewContent?.id == song.id
                                    Surface(
                                        color = if (isSelected) PrimaryDark else BgSubtle,
                                        border = BorderStroke(1.dp, if (isSelected) Primary else BorderDark),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onSelectPreviewContent(song)
                                                onSelectPreviewSlide(0)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(song.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(
                                                    song.slides.firstOrNull() ?: "",
                                                    color = TextDim,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Surface(
                                                color = Color(0xFF334155),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "Preview 👁️",
                                                    color = Primary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 2: MEDIA & BACKGROUND HUB
                    2 -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Active Background Status & Clear
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = when (presentationState.backgroundType) {
                                        BackgroundType.IP_CAMERA -> PrimaryDark
                                        BackgroundType.VIDEO -> Color(0xFF1E3A8A)
                                        BackgroundType.IMAGE -> Color(0xFF065F46)
                                        BackgroundType.CAMERA -> Color(0xFF831843)
                                        BackgroundType.NONE -> BgSubtle
                                    }
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("BACKGROUND AKTIF", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = when (presentationState.backgroundType) {
                                                BackgroundType.IP_CAMERA -> "📱 DroidCam IP Kamera"
                                                BackgroundType.VIDEO -> "🎬 Video Loop Aktif"
                                                BackgroundType.IMAGE -> "🖼️ Background Gambar"
                                                BackgroundType.CAMERA -> "📷 Kamera HP (Device Cam)"
                                                BackgroundType.NONE -> "⬛ Background Hitam Polos"
                                            },
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (presentationState.backgroundType != BackgroundType.NONE) {
                                        Button(
                                            onClick = { server.clearBackground() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Danger),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(30.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Clear BG", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Add DroidCam Button
                            Button(
                                onClick = onOpenAddDroidCam,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                modifier = Modifier.fillMaxWidth().height(38.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("📱 + Sambungkan DroidCam HP Kamera", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Media Items List
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(mediaLibrary) { media ->
                                    Surface(
                                        color = BgSubtle,
                                        border = BorderStroke(1.dp, BorderDark),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(media.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text(
                                                    when (media) {
                                                        is VideoContent -> "Video Motion"
                                                        is ImageContent -> "Still Image"
                                                        is IpCameraContent -> "IP Camera Feed"
                                                        else -> "Media"
                                                    },
                                                    color = TextDim,
                                                    fontSize = 9.sp
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Button(
                                                    onClick = {
                                                        when (media) {
                                                            is VideoContent -> server.setBackgroundVideo(media.uri)
                                                            is ImageContent -> server.setBackgroundImage(media.uri)
                                                            is IpCameraContent -> server.setBackgroundIpCamera(media.streamUrl)
                                                            else -> {}
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier.height(30.dp),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Set BG", color = Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryPanel(
    modifier: Modifier = Modifier,
    library: List<PresentationContent>,
    onSelect: (PresentationContent) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(BgPanel, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text("Song Library", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn {
            items(library) { item ->
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item) }
                        .padding(vertical = 6.dp)
                )
                HorizontalDivider(color = BorderDark.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
fun PlaylistPanel(
    modifier: Modifier = Modifier,
    playlist: List<PresentationContent>,
    playlists: List<ServicePlaylist>,
    activePlaylistId: String,
    onSelectActivePlaylist: (String) -> Unit,
    onSelect: (PresentationContent) -> Unit
) {
    val activePlaylist = playlists.find { it.id == activePlaylistId } ?: playlists.firstOrNull()
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(BgPanel, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Service Order", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

            Box {
                Text(
                    text = "${activePlaylist?.name} ▾",
                    color = Primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(max = 100.dp)
                        .clickable { dropdownExpanded = true }
                )

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    playlists.forEach { pl ->
                        DropdownMenuItem(
                            text = { Text(pl.name, color = Color.White, fontSize = 11.sp) },
                            onClick = {
                                onSelectActivePlaylist(pl.id)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn {
            itemsIndexed(playlist) { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("0${index + 1}. ", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(item.title, color = Color.White, fontSize = 12.sp)
                }
                HorizontalDivider(color = BorderDark.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
fun BottomControls(
    server: PresentationServer,
    presentationState: PresentationState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGo: () -> Unit,
    onBlack: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgPanel)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ControlButton(text = "PREV", icon = "◀", onClick = onPrevious, modifier = Modifier.weight(1f))
            ControlButton(text = "NEXT", icon = "▶", onClick = onNext, modifier = Modifier.weight(1f))
            ControlButton(
                text = "BLACK",
                icon = "◼",
                onClick = onBlack,
                modifier = Modifier.weight(1f),
                containerColor = if (presentationState.status == PresentationStatus.BLACK) Danger else BorderDark
            )
            ControlButton(
                text = "CLEAR",
                icon = "◻",
                onClick = onClear,
                modifier = Modifier.weight(1f),
                containerColor = if (presentationState.status == PresentationStatus.CLEAR) Danger else BorderDark
            )
        }

        Button(
            onClick = onGo,
            colors = ButtonDefaults.buttonColors(containerColor = Emerald),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("GO LIVE ▶", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun ControlButton(
    text: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = BorderDark
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(44.dp),
        contentPadding = PaddingValues(2.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(icon, fontSize = 14.sp, color = TextMain)
            Text(text, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMain)
        }
    }
}

@Composable
fun AddDroidCamDialog(
    onDismiss: () -> Unit,
    onAdd: (IpCameraContent) -> Unit
) {
    var droidCamIpInput by remember { mutableStateOf("192.168.1.50") }
    var droidCamPortInput by remember { mutableStateOf("4747") }
    var selectedPath by remember { mutableStateOf("/video") }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                        onAdd(
                            IpCameraContent(
                                id = System.currentTimeMillis().toString(),
                                title = "DroidCam HP ($ip)",
                                streamUrl = streamUrl
                            )
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Simpan Kamera", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color.Gray)
            }
        }
    )
}

@Composable
fun TickerDialog(
    server: PresentationServer,
    presentationState: PresentationState,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(presentationState.tickerText ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pengumuman Darurat (Ticker)")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Teks ini akan berjalan di bagian bawah layar presentasi.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Pesan Pengumuman") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    placeholder = { Text("Contoh: Pemilik mobil B 1234 CD harap memindahkan kendaraannya...") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        server.showTicker(text)
                    } else {
                        server.hideTicker()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald)
            ) {
                Text(if (text.isNotBlank()) "Tampilkan Ticker" else "Sembunyikan Ticker")
            }
        },
        dismissButton = {
            if (presentationState.isTickerVisible) {
                Button(
                    onClick = {
                        server.hideTicker()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Matikan Ticker", color = Color.White)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Batal")
                }
            }
        }
    )
}

@Composable
fun WebRemoteDialog(
    server: PresentationServer,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val localIp = remember(server) { getLocalIpAddress(server.context) }
    val port = server.webServer.activePort
    val remoteUrl = "http://$localIp:$port/remote"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📱 Web Remote Controller")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = Color(0xFF0D9488).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF0D9488)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "🔒 Hak Akses Terbatas (Media & Kamera)",
                            color = Color(0xFF2DD4BF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Remote web khusus untuk kru multimedia/kamera mengganti Media Background dan Kamera Live via Wi-Fi. Full control lirik lagu, ayat Alkitab, dan urutan ibadah tetap aman di konsol utama ini.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("ALAMAT URL REMOTE WEB:", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = remoteUrl,
                            color = Color(0xFF34D399),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(remoteUrl))
                            Toast.makeText(context, "URL Web Remote disalin!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📋 Salin URL", fontSize = 11.sp, color = Color.White)
                    }

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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🚀 Buka Web", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = Color(0xFF94A3B8))
            }
        }
    )
}


package com.example.presentation

import android.content.Context
import com.example.features.lyrics.LyricsStylePreset
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

class PresentationServer(val context: Context? = null) : PresentationEngine {
    private val _state = MutableStateFlow(PresentationState())
    override val state: StateFlow<PresentationState> = _state.asStateFlow()
    val webServer = com.example.server.PresentationWebServer(this)

    // Shared in-memory library for Web Remote & Android App
    val songsLibrary = mutableListOf(
        LyricsContent("s1", "Engkau Baik (Worship)", listOf("Verse 1\nKu datang ke hadirat-Mu\nDengan hati penuh syukur", "Chorus\nEngkau baik\nSelamanya baik\nKasih setia-Mu kekal")),
        LyricsContent("s2", "Amazing Grace", listOf("Verse 1\nAmazing grace, how sweet the sound\nThat saved a wretch like me", "Verse 2\nI once was lost, but now am found\nWas blind, but now I see", "Verse 3\nThrough many dangers, toils and snares\nI have already come")),
        LyricsContent("s3", "How Great Thou Art", listOf("Verse 1\nO Lord my God, when I in awesome wonder\nConsider all the worlds Thy hands have made", "Chorus\nThen sings my soul, my Savior God, to Thee\nHow great Thou art, how great Thou art", "Verse 2\nWhen Christ shall come, with shout of acclamation\nAnd take me home, what joy shall fill my heart")),
        LyricsContent("s4", "Kebaikan-Mu Lebih Dari Hidup", listOf("Verse\nSebab kasih setia-Mu lebih dari hidup\nBibirku akan memegahkan Engkau", "Chorus\nSeumur hidupku kupuji nama-Mu\nKunaikkan tanganku demi nama-Mu")),
        LyricsContent("s5", "10,000 Reasons (Bless The Lord)", listOf("Chorus\nBless the Lord, O my soul, O my soul\nWorship His holy name\nSing like never before, O my soul\nI'll worship Your holy name", "Verse 1\nThe sun comes up, it's a new day dawning\nIt's time to sing Your song again"))
    )

    val mediaLibrary = mutableListOf<PresentationContent>(
        IpCameraContent("droid1", "DroidCam HP (Wireless Stream)", "http://192.168.1.50:4747/video"),
        CameraContent("cam1", "Kamera HP Utama (Local Cam)", "0")
    )

    val sampleBiblePassages = listOf(
        BibleContent(
            id = "b1",
            title = "Yohanes 3:16-17 (TB)",
            bookAndChapter = "Yohanes 3",
            verses = listOf(
                "16 Karena begitu besar kasih Allah akan dunia ini, sehingga Ia telah mengaruniakan Anak-Nya yang tunggal, supaya setiap orang yang percaya kepada-Nya tidak binasa, melainkan beroleh hidup yang kekal.",
                "17 Sebab Allah mengutus Anak-Nya ke dalam dunia bukan untuk menghakimi dunia, melainkan untuk menyelamatkannya oleh Dia."
            )
        ),
        BibleContent(
            id = "b2",
            title = "Mazmur 23:1-6 (TB)",
            bookAndChapter = "Mazmur 23",
            verses = listOf(
                "1 TUHAN adalah gembalaku, takkan kekurangan aku.",
                "2 Ia membaringkan aku di padang yang berumput hijau, Ia membimbing aku ke air yang tenang;",
                "3 Ia menyegarkan jiwaku. Ia menuntun aku di jalan yang benar oleh karena nama-Nya.",
                "4 Sekalipun aku berjalan dalam lembah kekelaman, aku tidak takut bahaya, sebab Engkau besertaku; gada-Mu dan tongkat-Mu, itulah yang menghibur aku.",
                "5 Engkau menyediakan hidangan bagiku, di hadapan lawanku; Engkau mengurapi kepalaku dengan minyak; pialaku penuh melimpah.",
                "6 Kebajikan dan kemurahan belaka akan mengikuti aku, seumur hidupku; dan aku akan diam dalam rumah TUHAN sepanjang masa."
            )
        ),
        BibleContent(
            id = "b3",
            title = "Filipi 4:13 (TB)",
            bookAndChapter = "Filipi 4",
            verses = listOf(
                "13 Segala perkara dapat kutanggung di dalam Dia yang memberi kekuatan kepadaku."
            )
        ),
        BibleContent(
            id = "b4",
            title = "Yohanes 14:6 (TB)",
            bookAndChapter = "Yohanes 14",
            verses = listOf(
                "6 Kata Yesus kepadanya: 'Akulah jalan dan kebenaran dan hidup. Tidak ada seorangpun yang datang kepada Bapa, kalau tidak melalui Aku.'"
            )
        ),
        BibleContent(
            id = "b5",
            title = "Mazmur 91:1-4 (TB)",
            bookAndChapter = "Mazmur 91",
            verses = listOf(
                "1 Orang yang duduk dalam lindungan Yang Mahatinggi dan bermalam dalam naungan Yang Mahakuasa",
                "2 akan berkata kepada TUHAN: 'Tempat perlindunganku dan kubu pertahananku, Allahku, pada-Mu aku percaya.'",
                "3 Sungguh, Dialah yang akan melepaskan engkau dari jerat penangkap burung, dari penyakit sampar yang busuk.",
                "4 Dengan kepak-Nya Ia akan menudungi engkau, di bawah sayap-Nya engkau akan berlindung, kesetiaan-Nya ialah perisai dan pagar tembok."
            )
        )
    )

    init {
        webServer.start(context)
    }

    override fun go(content: PresentationContent) {
        val newStatus = when (content) {
            is LyricsContent -> PresentationStatus.LYRICS
            is BibleContent -> PresentationStatus.BIBLE
            is VideoContent -> PresentationStatus.VIDEO
            is ImageContent -> PresentationStatus.IMAGE
            is CameraContent -> PresentationStatus.CAMERA
            is IpCameraContent -> PresentationStatus.IP_CAMERA
            is PowerPointContent -> PresentationStatus.POWERPOINT
        }

        _state.update {
            it.copy(
                currentContent = content,
                currentSlideIndex = 0,
                status = newStatus,
                isVideoPlaying = true
            )
        }
    }

    fun setSlideIndex(index: Int) {
        val currentContent = _state.value.currentContent
        val maxIndex = when (currentContent) {
            is LyricsContent -> currentContent.slides.size - 1
            is BibleContent -> currentContent.verses.size - 1
            is PowerPointContent -> currentContent.slides.size - 1
            else -> 0
        }
        if (maxIndex >= 0) {
            val clamped = index.coerceIn(0, maxIndex)
            _state.update { it.copy(currentSlideIndex = clamped) }
        }
    }

    override fun nextSlide() {
        val currentContent = _state.value.currentContent
        val currentIndex = _state.value.currentSlideIndex
        val maxIndex = when (currentContent) {
            is LyricsContent -> currentContent.slides.size - 1
            is BibleContent -> currentContent.verses.size - 1
            is PowerPointContent -> currentContent.slides.size - 1
            else -> 0
        }
        if (currentIndex < maxIndex) {
            _state.update { it.copy(currentSlideIndex = currentIndex + 1) }
        }
    }

    override fun previousSlide() {
        val currentIndex = _state.value.currentSlideIndex
        if (currentIndex > 0) {
            _state.update { it.copy(currentSlideIndex = currentIndex - 1) }
        }
    }

    override fun clear() {
        _state.update {
            if (it.status == PresentationStatus.CLEAR) {
                val status = when (it.currentContent) {
                    is LyricsContent -> PresentationStatus.LYRICS
                    is BibleContent -> PresentationStatus.BIBLE
                    is VideoContent -> PresentationStatus.VIDEO
                    is ImageContent -> PresentationStatus.IMAGE
                    is CameraContent -> PresentationStatus.CAMERA
                    is IpCameraContent -> PresentationStatus.IP_CAMERA
                    is PowerPointContent -> PresentationStatus.POWERPOINT
                    null -> PresentationStatus.IDLE
                }
                it.copy(status = status)
            } else {
                it.copy(status = PresentationStatus.CLEAR)
            }
        }
    }

    override fun black() {
        _state.update {
            if (it.status == PresentationStatus.BLACK) {
                val status = when (it.currentContent) {
                    is LyricsContent -> PresentationStatus.LYRICS
                    is BibleContent -> PresentationStatus.BIBLE
                    is VideoContent -> PresentationStatus.VIDEO
                    is ImageContent -> PresentationStatus.IMAGE
                    is CameraContent -> PresentationStatus.CAMERA
                    is IpCameraContent -> PresentationStatus.IP_CAMERA
                    is PowerPointContent -> PresentationStatus.POWERPOINT
                    null -> PresentationStatus.IDLE
                }
                it.copy(status = status)
            } else {
                it.copy(status = PresentationStatus.BLACK)
            }
        }
    }

    fun setBackgroundImage(uri: String?) {
        _state.update {
            if (uri.isNullOrEmpty()) {
                it.copy(backgroundType = BackgroundType.NONE, backgroundImageUri = null)
            } else {
                it.copy(backgroundType = BackgroundType.IMAGE, backgroundImageUri = uri)
            }
        }
    }

    fun setBackgroundVideo(uri: String?) {
        _state.update {
            if (uri.isNullOrEmpty()) {
                it.copy(backgroundType = BackgroundType.NONE, backgroundVideoUri = null)
            } else if (uri.startsWith("http://") || uri.startsWith("https://")) {
                it.copy(backgroundType = BackgroundType.IP_CAMERA, backgroundVideoUri = uri)
            } else {
                it.copy(backgroundType = BackgroundType.VIDEO, backgroundVideoUri = uri)
            }
        }
    }

    fun setBackgroundIpCamera(streamUrl: String?) {
        setBackgroundVideo(streamUrl)
    }

    fun setBackgroundCamera(enabled: Boolean) {
        _state.update {
            if (enabled) {
                it.copy(backgroundType = BackgroundType.CAMERA)
            } else {
                it.copy(backgroundType = BackgroundType.NONE)
            }
        }
    }

    fun setStylePreset(preset: LyricsStylePreset) {
        _state.update {
            it.copy(
                stylePreset = preset,
                fontSizeSp = preset.fontSize.value.toInt(),
                isTextBold = preset.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold || preset.fontWeight == androidx.compose.ui.text.font.FontWeight.ExtraBold,
                textColorRgb = preset.textColor.value.toLong(),
                textPosition = if (preset.isLowerThird) TextDisplayPosition.LOWER_THIRD else TextDisplayPosition.CENTER
            )
        }
    }

    fun updateLiveTextSettings(
        fontSizeSp: Int = _state.value.fontSizeSp,
        textPosition: TextDisplayPosition = _state.value.textPosition,
        textColorRgb: Long = _state.value.textColorRgb,
        textAlignment: TextAlignmentOption = _state.value.textAlignment,
        textBackgroundAlpha: Float = _state.value.textBackgroundAlpha,
        isTextBold: Boolean = _state.value.isTextBold,
        isTextShadowEnabled: Boolean = _state.value.isTextShadowEnabled
    ) {
        _state.update {
            it.copy(
                fontSizeSp = fontSizeSp,
                textPosition = textPosition,
                textColorRgb = textColorRgb,
                textAlignment = textAlignment,
                textBackgroundAlpha = textBackgroundAlpha,
                isTextBold = isTextBold,
                isTextShadowEnabled = isTextShadowEnabled
            )
        }
    }

    fun toggleVideoPlayback() {
        _state.update { it.copy(isVideoPlaying = !it.isVideoPlaying) }
    }

    override fun selectDisplay(displayId: String) {
        _state.update { it.copy(activeDisplayId = displayId) }
    }

    fun addCustomSong(title: String, slidesText: List<String>): LyricsContent {
        val newSong = LyricsContent(
            id = "s_" + System.currentTimeMillis(),
            title = title,
            slides = slidesText
        )
        songsLibrary.add(0, newSong)
        return newSong
    }

    fun addDroidCamMedia(title: String, ip: String, port: String): IpCameraContent {
        val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://").removeSuffix("/")
        val cleanPort = port.trim().ifEmpty { "4747" }
        val streamUrl = "http://$cleanIp:$cleanPort/video"
        val media = IpCameraContent(
            id = "droid_" + System.currentTimeMillis(),
            title = title.ifEmpty { "DroidCam ($cleanIp)" },
            streamUrl = streamUrl
        )
        mediaLibrary.add(media)
        return media
    }

    fun goCustomText(title: String, text: String, type: String = "LYRICS") {
        val slides = text.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }
        val finalSlides = if (slides.isEmpty()) listOf(text.trim()) else slides
        if (type == "BIBLE") {
            val bible = BibleContent(
                id = "custom_b_" + System.currentTimeMillis(),
                title = title.ifEmpty { "Kitab Suci" },
                bookAndChapter = title,
                verses = finalSlides
            )
            go(bible)
        } else {
            val lyrics = LyricsContent(
                id = "custom_s_" + System.currentTimeMillis(),
                title = title.ifEmpty { "Quick Presentation" },
                slides = finalSlides
            )
            go(lyrics)
        }
    }

    fun buildLibraryJson(): JSONObject {
        val json = JSONObject()
        
        val songsArr = JSONArray()
        songsLibrary.forEach { song ->
            val obj = JSONObject()
            obj.put("id", song.id)
            obj.put("title", song.title)
            val slides = JSONArray()
            song.slides.forEach { slides.put(it) }
            obj.put("slides", slides)
            songsArr.put(obj)
        }
        json.put("songs", songsArr)

        val bibleArr = JSONArray()
        sampleBiblePassages.forEach { passage ->
            val obj = JSONObject()
            obj.put("id", passage.id)
            obj.put("title", passage.title)
            obj.put("bookAndChapter", passage.bookAndChapter)
            val verses = JSONArray()
            passage.verses.forEach { verses.put(it) }
            obj.put("verses", verses)
            bibleArr.put(obj)
        }
        json.put("bible", bibleArr)

        val mediaArr = JSONArray()
        mediaLibrary.forEach { media ->
            val obj = JSONObject()
            obj.put("id", media.id)
            obj.put("title", media.title)
            when (media) {
                is IpCameraContent -> {
                    obj.put("type", "IP_CAMERA")
                    obj.put("url", media.streamUrl)
                }
                is CameraContent -> {
                    obj.put("type", "CAMERA")
                    obj.put("url", "/camera/stream")
                }
                is ImageContent -> {
                    obj.put("type", "IMAGE")
                    obj.put("url", media.uri)
                }
                is VideoContent -> {
                    obj.put("type", "VIDEO")
                    obj.put("url", media.uri)
                }
                else -> {
                    obj.put("type", "OTHER")
                    obj.put("url", "")
                }
            }
            mediaArr.put(obj)
        }
        json.put("media", mediaArr)

        return json
    }
}


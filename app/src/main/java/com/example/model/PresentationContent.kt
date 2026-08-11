package com.example.model

sealed interface PresentationContent {
    val id: String
    val title: String
}

data class LyricsContent(
    override val id: String,
    override val title: String,
    val slides: List<String>
) : PresentationContent

data class ImageContent(
    override val id: String,
    override val title: String,
    val uri: String
) : PresentationContent

data class VideoContent(
    override val id: String,
    override val title: String,
    val uri: String
) : PresentationContent

data class PowerPointContent(
    override val id: String,
    override val title: String,
    val slides: List<String> // URIs to converted slide images
) : PresentationContent

data class BibleContent(
    override val id: String,
    override val title: String, // e.g. "John 3:16-17 (NIV)"
    val bookAndChapter: String, // e.g. "John 3"
    val verses: List<String> // e.g. ["16 For God so loved the world...", "17 For God did not send..."]
) : PresentationContent

data class CameraContent(
    override val id: String,
    override val title: String,
    val cameraId: String
) : PresentationContent

data class IpCameraContent(
    override val id: String,
    override val title: String, // e.g. "DroidCam HP (192.168.1.50)"
    val streamUrl: String       // e.g. "http://192.168.1.50:4747/video"
) : PresentationContent

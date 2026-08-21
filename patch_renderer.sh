sed -i 's/CameraPreview()/HeadlessCameraRenderer()/g' app/src/main/java/com/example/features/display/PresentationFrameRenderer.kt
cat << 'INNER_EOF' >> app/src/main/java/com/example/features/display/PresentationFrameRenderer.kt

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
INNER_EOF

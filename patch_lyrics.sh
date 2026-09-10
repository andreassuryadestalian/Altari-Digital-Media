sed -i 's|import androidx.compose.ui.unit.sp|import androidx.compose.ui.unit.sp\nimport androidx.lifecycle.compose.collectAsStateWithLifecycle\nimport com.example.db.PresentationRepository|g' app/src/main/java/com/example/features/lyrics/LyricsManagementScreen.kt

sed -i 's/fun LyricsManagementScreen(/fun LyricsManagementScreen(\n    repository: PresentationRepository,/g' app/src/main/java/com/example/features/lyrics/LyricsManagementScreen.kt

sed -i '/var selectedPassage by remember { mutableStateOf<BibleContent?>(passages.firstOrNull()) }/d' app/src/main/java/com/example/features/bible/BibleManagementScreen.kt

sed -i 's/import androidx.compose.ui.platform.LocalContext/import androidx.compose.ui.platform.LocalContext\nimport kotlinx.coroutines.launch/g' app/src/main/java/com/example/features/dashboard/DashboardScreen.kt

sed -i 's/LyricsManagementScreen(/LyricsManagementScreen(\n                    repository = repository,/g' app/src/main/java/com/example/features/dashboard/DashboardScreen.kt

sed -i 's|import androidx.compose.ui.unit.sp|import androidx.compose.ui.unit.sp\nimport androidx.lifecycle.compose.collectAsStateWithLifecycle\nimport com.example.db.PresentationRepository|g' app/src/main/java/com/example/features/bible/BibleManagementScreen.kt

sed -i 's/fun BibleManagementScreen(/fun BibleManagementScreen(\n    repository: PresentationRepository,/g' app/src/main/java/com/example/features/bible/BibleManagementScreen.kt

sed -i '/var passages by remember {/,/    }/c\    val passages by repository.allBibleVerses.collectAsStateWithLifecycle(initialValue = emptyList())\n\n    var selectedPassage by remember { mutableStateOf<BibleContent?>(null) }' app/src/main/java/com/example/features/bible/BibleManagementScreen.kt


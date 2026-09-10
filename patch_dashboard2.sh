sed -i '/val songsLibrary = remember {/,/    }/c\    val dbSongs by repository.allLyrics.collectAsStateWithLifecycle(initialValue = emptyList())\n\n    LaunchedEffect(dbSongs) {\n        server.songsLibrary.clear()\n        server.songsLibrary.addAll(dbSongs)\n    }' app/src/main/java/com/example/features/dashboard/DashboardScreen.kt

sed -i 's/songsList = songsLibrary,/songsList = dbSongs,/g' app/src/main/java/com/example/features/dashboard/DashboardScreen.kt


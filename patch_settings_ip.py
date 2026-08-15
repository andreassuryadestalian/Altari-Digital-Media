with open("app/src/main/java/com/example/features/settings/SettingsScreen.kt", "r") as f:
    content = f.read()

target_import = "import com.example.server.getLocalIpAddress"
replacement_import = "import com.example.server.getLocalIpAddress\nimport com.example.server.getLocalIpInfo"
if target_import in content:
    content = content.replace(target_import, replacement_import)

target_col = """                Column(horizontalAlignment = Alignment.End) {
                    val context = LocalContext.current
                    var ipAddress by remember { mutableStateOf("Memuat IP...") }
                    
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val ip = getLocalIpAddress()
                            ipAddress = if (ip.isNotEmpty()) "http://$ip:8080" else "Koneksi lokal tidak ditemukan"
                        }
                    }"""

replacement_col = """                Column(horizontalAlignment = Alignment.End) {
                    val context = LocalContext.current
                    var ipAddress by remember { mutableStateOf("Memuat IP...") }
                    
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val info = getLocalIpInfo()
                            val primary = info.primaryIp
                            ipAddress = if (primary.isNotEmpty()) "http://$primary:8080" else "http://localhost:8080"
                        }
                    }"""

if target_col in content:
    content = content.replace(target_col, replacement_col)
    with open("app/src/main/java/com/example/features/settings/SettingsScreen.kt", "w") as f:
        f.write(content)
    print("Patched SettingsScreen.kt successfully!")
else:
    print("target_col not found!")

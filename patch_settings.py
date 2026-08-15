import re

with open("app/src/main/java/com/example/features/settings/SettingsScreen.kt", "r") as f:
    content = f.read()

target = "import com.example.model.IpCameraContent\nimport com.example.presentation.PresentationServer"
replacement = "import com.example.model.IpCameraContent\nimport com.example.presentation.PresentationServer\nimport com.example.server.getLocalIpAddress"
if target in content:
    content = content.replace(target, replacement)

target2 = "                Surface(\n                    color = Color(0xFF381E72),\n                    shape = RoundedCornerShape(4.dp)\n                ) {\n                    Text("
replacement2 = """
                Column(horizontalAlignment = Alignment.End) {
                    val context = LocalContext.current
                    var ipAddress by remember { mutableStateOf("Memuat IP...") }
                    
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.Dispatchers.IO.invoke {
                            val ip = getLocalIpAddress()
                            ipAddress = if (ip.isNotEmpty()) "http://$ip:8080" else "Koneksi lokal tidak ditemukan"
                        }
                    }
                    
                    Surface(
                        color = Color(0xFF10B981),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = "Live Web: $ipAddress",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    Surface(
                        color = Color(0xFF381E72),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("""
if target2 in content:
    content = content.replace(target2, replacement2)
else:
    print("Target 2 not found!")

with open("app/src/main/java/com/example/features/settings/SettingsScreen.kt", "w") as f:
    f.write(content)

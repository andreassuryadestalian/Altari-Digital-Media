with open("app/src/main/java/com/example/presentation/PresentationServer.kt", "r") as f:
    content = f.read()

target = "class PresentationServer : PresentationEngine {\n    private val _state = MutableStateFlow(PresentationState())\n    override val state: StateFlow<PresentationState> = _state.asStateFlow()"
replacement = "class PresentationServer : PresentationEngine {\n    private val _state = MutableStateFlow(PresentationState())\n    override val state: StateFlow<PresentationState> = _state.asStateFlow()\n    private val webServer = com.example.server.PresentationWebServer(this)\n\n    init {\n        webServer.start()\n    }"

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/presentation/PresentationServer.kt", "w") as f:
        f.write(content)
else:
    print("Target not found!")

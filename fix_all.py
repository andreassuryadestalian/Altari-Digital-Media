import re

# 1. Patch PresentationWebServer.kt
p_path = "app/src/main/java/com/example/server/PresentationWebServer.kt"
with open(p_path, "r") as f:
    p_code = f.read()

p_code = p_code.replace(
    'val serverEngine = embeddedServer(CIO, port = port) {',
    'val serverEngine = embeddedServer(CIO, port = port, host = "0.0.0.0") {'
)
with open(p_path, "w") as f:
    f.write(p_code)
print("Patched PresentationWebServer.kt")

# 2. Patch DisplayRenderer.kt
d_path = "app/src/main/java/com/example/features/display/DisplayRenderer.kt"
with open(d_path, "r") as f:
    d_code = f.read()

old_connect = """    fun connectDisplay(display: Display, profileType: DisplayProfileType = DisplayProfileType.MAIN_PROJECTOR) {
        disconnectDisplay(display.displayId)
        displayProfiles[display.displayId] = profileType
        val presentation = object : Presentation(context, display) {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                val composeView = ComposeView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setContent {
                        val state by presentationStateFlow.collectAsState()
                        PresentationFrameRenderer(
                            state = state,
                            profileType = profileType
                        )
                    }
                }
                setContentView(composeView)
            }
        }
        try {
            presentation.show()
            activePresentations[display.displayId] = presentation
        } catch (e: Exception) {
            Log.e("DisplayRenderer", "Failed to launch presentation on display ${display.displayId}", e)
        }
    }"""

new_connect = """    fun connectDisplay(display: Display, profileType: DisplayProfileType = DisplayProfileType.MAIN_PROJECTOR) {
        if (display.displayId == Display.DEFAULT_DISPLAY) {
            Log.w("DisplayRenderer", "Cannot open Presentation on DEFAULT_DISPLAY (Main Screen)")
            return
        }
        disconnectDisplay(display.displayId)
        displayProfiles[display.displayId] = profileType
        try {
            val presentation = object : Presentation(context, display) {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    try {
                        val composeView = ComposeView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setContent {
                                val state by presentationStateFlow.collectAsState()
                                PresentationFrameRenderer(
                                    state = state,
                                    profileType = profileType
                                )
                            }
                        }
                        setContentView(composeView)
                    } catch (e: Throwable) {
                        Log.e("DisplayRenderer", "Error inside Presentation onCreate", e)
                    }
                }
            }
            presentation.show()
            activePresentations[display.displayId] = presentation
        } catch (e: Throwable) {
            Log.e("DisplayRenderer", "Failed to launch presentation on display ${display.displayId}", e)
        }
    }"""

if old_connect in d_code:
    d_code = d_code.replace(old_connect, new_connect)
    with open(d_path, "w") as f:
        f.write(d_code)
    print("Patched DisplayRenderer.kt")
else:
    print("WARNING: old_connect not found in DisplayRenderer.kt")

# 3. Patch SettingsScreen.kt
s_path = "app/src/main/java/com/example/features/settings/SettingsScreen.kt"
with open(s_path, "r") as f:
    s_code = f.read()

s_code = s_code.replace(
    'detectedDisplays = dm.displays.toList()',
    'detectedDisplays = dm.displays.filter { it.displayId != Display.DEFAULT_DISPLAY }'
)
with open(s_path, "w") as f:
    f.write(s_code)
print("Patched SettingsScreen.kt")


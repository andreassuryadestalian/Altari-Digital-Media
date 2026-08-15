with open("app/src/main/java/com/example/features/settings/SettingsScreen.kt", "r") as f:
    content = f.read()

target = """                    Surface(
                        color = Color(0xFF381E72),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${presState.fontSizeSp} SP • ${presState.textPosition.label}""""

replacement = """                    Surface(
                        color = Color(0xFF381E72),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${presState.fontSizeSp} SP • ${presState.textPosition.label}"""

if target in content:
    # Need to close the Column
    # The structure was:
    # Column {
    #   Surface { ... }
    #   Surface { Text(...) }
    # }
    # Wait, the Text() has closing braces below it.
    pass

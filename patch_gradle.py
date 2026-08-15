import re

with open("gradle/libs.versions.toml", "r") as f:
    content = f.read()

if "ktor =" not in content:
    content = content.replace("[versions]\n", "[versions]\nktor = \"2.3.12\"\n")
    content = content.replace("[libraries]\n", "[libraries]\nktor-server-core = { group = \"io.ktor\", name = \"ktor-server-core\", version.ref = \"ktor\" }\nktor-server-cio = { group = \"io.ktor\", name = \"ktor-server-cio\", version.ref = \"ktor\" }\nktor-server-cors = { group = \"io.ktor\", name = \"ktor-server-cors\", version.ref = \"ktor\" }\nktor-server-websockets = { group = \"io.ktor\", name = \"ktor-server-websockets\", version.ref = \"ktor\" }\nktor-server-html-builder = { group = \"io.ktor\", name = \"ktor-server-html-builder\", version.ref = \"ktor\" }\n")

with open("gradle/libs.versions.toml", "w") as f:
    f.write(content)

with open("app/build.gradle.kts", "r") as f:
    app_content = f.read()

if "ktor-server-core" not in app_content:
    app_content = app_content.replace("dependencies {", "dependencies {\n  implementation(libs.ktor.server.core)\n  implementation(libs.ktor.server.cio)\n  implementation(libs.ktor.server.cors)\n  implementation(libs.ktor.server.websockets)\n  implementation(libs.ktor.server.html.builder)\n")

with open("app/build.gradle.kts", "w") as f:
    f.write(app_content)

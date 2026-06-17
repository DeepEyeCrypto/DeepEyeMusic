import re

file_path = "app/src/main/java/com/deepeye/musicpro/ui/player/NowPlayingScreen.kt"
with open(file_path, "r") as f:
    code = f.read()

# 1. Remove `if (!isInPipMode) {` (around line 214) and its closing brace.
# To do this safely, we will replace the exact lines.
code = code.replace("        if (!isInPipMode) {\n            // Refined Clear Glass Mesh Background\n            Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {",
                    "        // Refined Clear Glass Mesh Background\n        Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {")

# The closing brace for `if (!isInPipMode)` is right after `AudioNowPlayingLayout(...) }` and before `if (showDspSheet) {`.
code = code.replace("                }\n            }\n        }\n\n    if (showDspSheet) {",
                    "                }\n            }\n\n    if (showDspSheet) {")

# 2. Update VideoNowPlayingLayout
code = code.replace("val isFullscreen = LocalFullscreenMode.current.isFullscreen",
                    "val isFullscreen = LocalFullscreenMode.current.isFullscreen || com.deepeye.musicpro.ui.LocalPipMode.current")

# 3. Update AudioNowPlayingLayout
audio_patch = """
    val headerColor = if (finalBgColor.luminance() > 0.5f) Color.Black else Color.White
    val isInPipMode = com.deepeye.musicpro.ui.LocalPipMode.current
    
    Column(
"""
code = code.replace("    val headerColor = if (finalBgColor.luminance() > 0.5f) Color.Black else Color.White\n    \n    Column(", audio_patch)

code = code.replace("        // Header\n        Row(", "        // Header\n        if (!isInPipMode) {\n        Row(")
code = code.replace("            }\n        }\n\n        // Pager Artwork Area\n        Box(", "            }\n        }\n        }\n\n        // Pager Artwork Area\n        Box(")

code = code.replace("        // 3. Info & Context Area\n        Column(", "        // 3. Info & Context Area\n        if (!isInPipMode) {\n        Column(")
code = code.replace("        }\n    }\n}\n\nprivate fun VideoNowPlayingLayout", "        }\n        }\n    }\n}\n\nprivate fun VideoNowPlayingLayout")


with open(file_path, "w") as f:
    f.write(code)

print("NowPlayingScreen patched!")

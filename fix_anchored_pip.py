import re

file_path = "app/src/main/java/com/deepeye/musicpro/ui/player/AnchoredMiniPlayer.kt"
with open(file_path, "r") as f:
    code = f.read()

# Add LocalPipMode import if not present
if "import com.deepeye.musicpro.ui.LocalPipMode" not in code:
    code = code.replace("import com.deepeye.musicpro.ui.player.NowPlayingScreen", "import com.deepeye.musicpro.ui.LocalPipMode\nimport com.deepeye.musicpro.ui.player.NowPlayingScreen")

# In the offset modifier, check Pip mode
replacement = """                .offset {
                    val isInPipMode = com.deepeye.musicpro.ui.LocalPipMode.current
                    val yOffset = if (isInPipMode) 0f else (draggableState.offset.takeIf { !it.isNaN() } ?: collapsedAnchor)
                    IntOffset(0, yOffset.roundToInt())
                }"""
code = re.sub(r'                \.offset \{\n                    val yOffset = draggableState\.offset\.takeIf \{ !it\.isNaN\(\) \} \?: collapsedAnchor\n                    IntOffset\(0, yOffset\.roundToInt\(\)\)\n                \}', replacement, code)

with open(file_path, "w") as f:
    f.write(code)

print("AnchoredMiniPlayer patched")

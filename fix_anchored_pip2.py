import re

file_path = "app/src/main/java/com/deepeye/musicpro/ui/player/AnchoredMiniPlayer.kt"
with open(file_path, "r") as f:
    code = f.read()

# Hoist the pip mode check
replacement_offset = """                .offset {
                    val yOffset = if (isInPipMode) 0f else (draggableState.offset.takeIf { !it.isNaN() } ?: collapsedAnchor)
                    IntOffset(0, yOffset.roundToInt())
                }"""

# Find where to inject isInPipMode = LocalPipMode.current
# We can just put it at the beginning of the composable
if "val isInPipMode = com.deepeye.musicpro.ui.LocalPipMode.current" not in code:
    code = code.replace("    val density = LocalDensity.current", "    val density = LocalDensity.current\n    val isInPipMode = com.deepeye.musicpro.ui.LocalPipMode.current")

code = re.sub(r'                \.offset \{\n                    val isInPipMode = com\.deepeye\.musicpro\.ui\.LocalPipMode\.current\n                    val yOffset = if \(isInPipMode\) 0f else \(draggableState\.offset\.takeIf \{ !it\.isNaN\(\) \} \?: collapsedAnchor\)\n                    IntOffset\(0, yOffset\.roundToInt\(\)\)\n                \}', replacement_offset, code)

with open(file_path, "w") as f:
    f.write(code)

print("AnchoredMiniPlayer patched correctly")

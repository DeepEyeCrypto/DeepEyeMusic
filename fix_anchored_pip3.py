import re

file_path = "app/src/main/java/com/deepeye/musicpro/ui/player/AnchoredMiniPlayer.kt"
with open(file_path, "r") as f:
    code = f.read()

# Inject the val
if "val isInPipMode = com.deepeye.musicpro.ui.LocalPipMode.current" not in code:
    code = code.replace(") {\n    BoxWithConstraints(", ") {\n    val isInPipMode = com.deepeye.musicpro.ui.LocalPipMode.current\n    BoxWithConstraints(")

with open(file_path, "w") as f:
    f.write(code)

print("AnchoredMiniPlayer patched correctly")

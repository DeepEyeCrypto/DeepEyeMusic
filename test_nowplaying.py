import re

file_path = "app/src/main/java/com/deepeye/musicpro/ui/player/NowPlayingScreen.kt"
with open(file_path, "r") as f:
    code = f.read()

lines = code.split('\n')
for i, line in enumerate(lines):
    if "if (!isInPipMode) {" in line:
        print(f"Line {i+1}: {line}")


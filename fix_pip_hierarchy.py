import re

file_path = "app/src/main/java/com/deepeye/musicpro/ui/DeepEyeMusicApp.kt"
with open(file_path, "r") as f:
    code = f.read()

# Revert my previous Box patch
code = re.sub(r'if \(isInPipMode && playerState\.currentItem != null\) \{.*?\}\n        \}', '', code, flags=re.DOTALL)

# Let's change the condition for AnchoredMiniPlayer to just `if (playerState.currentItem != null)`
code = code.replace('if (playerState.currentItem != null && !isInPipMode) {', 'if (playerState.currentItem != null) {')

with open(file_path, "w") as f:
    f.write(code)

print("Hierarchy patched")

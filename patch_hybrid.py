import re

with open("app/src/main/java/com/deepeye/musicpro/ui/components/HybridPlayerCard.kt", "r") as f:
    content = f.read()

# 1. Add isLocked
content = re.sub(
    r'(var videoScale by remember \{ mutableFloatStateOf\(1f\) \})',
    r'\1\n    var isLocked by remember { mutableStateOf(false) }',
    content, count=1
)

# 2. Update pointerInput
content = re.sub(
    r'pointerInput\(isFullscreen\) \{\n\s*if \(!isFullscreen\) return@pointerInput',
    r'pointerInput(isFullscreen, isLocked) {\n                            if (!isFullscreen || isLocked) return@pointerInput',
    content, count=1
)

# 3. Replace Fullscreen Exit Button with Lock Button
old_exit_btn = '''                    // Close / Exit Fullscreen Button Overlay (top right)
                    if (isFullscreen && !isInPipMode) {
                        AnimatedVisibility(
                            visible = controlsVisible && !isInPipMode,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Box(
                                modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                IconButton(
                                    onClick = { fullscreenMode.exit() },
                                    modifier =
                                    Modifier
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FullscreenExit,
                                        contentDescription = "Exit Fullscreen",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }
                    }'''

new_lock_btn = '''                    // Top Left Lock/Unlock Button
                    if (isFullscreen && !isInPipMode) {
                        AnimatedVisibility(
                            visible = isLocked || controlsVisible,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.TopStart,
                            ) {
                                IconButton(
                                    onClick = {
                                        isLocked = !isLocked
                                        if (!isLocked) resetTimer()
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                                ) {
                                    Icon(
                                        imageVector = if (isLocked) androidx.compose.material.icons.filled.Lock else androidx.compose.material.icons.filled.LockOpen,
                                        contentDescription = if (isLocked) "Unlock" else "Lock",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }
                    }'''
if old_exit_btn in content:
    content = content.replace(old_exit_btn, new_lock_btn)
else:
    print("Could not find Fullscreen Exit button block")

# 4. Hide tap zones when locked
old_tap_zones = '''                    // GESTURE SKIP ZONES + OVERLAYS: Hidden in PiP for clean view
                    if (!isInPipMode) {'''
new_tap_zones = '''                    // GESTURE SKIP ZONES + OVERLAYS: Hidden in PiP for clean view
                    if (!isInPipMode && !isLocked) {'''
if old_tap_zones in content:
    content = content.replace(old_tap_zones, new_tap_zones)
else:
    print("Could not find GESTURE SKIP ZONES block")

# 5. Hide center play when locked
old_center = 'visible = controlsVisible && !isPlaying && !isInPipMode,'
new_center = 'visible = controlsVisible && !isPlaying && !isInPipMode && !isLocked,'
if old_center in content:
    content = content.replace(old_center, new_center)
else:
    print("Could not find center play visibility")

# 6. Hide bottom controls when locked
old_bottom = 'visible = controlsVisible && !isInPipMode,'
new_bottom = 'visible = controlsVisible && !isInPipMode && !isLocked,'
if old_bottom in content:
    content = content.replace(old_bottom, new_bottom)
else:
    print("Could not find bottom controls visibility")

# 7. Also handle the tap outside when locked to show lock button
# Actually, the user can just tap the ExoPlayer View if they want to show controls. But ExoPlayer view doesn't consume taps, they fall back to the Box. The Box has no tap handler. 
# Wait, when !isLocked, ExoPlayer view taps go to... where?
# In the Left/Right Tap zones, there is a catch-all that triggers controlsVisible = !controlsVisible on single tap.
# But when isLocked is true, the Tap zones are hidden! So single taps fall through to the Box, which does nothing. So we need a Box to catch taps and show the unlock button!
# Let's add a pointerInput to the main Box when isLocked is true.

content_new = []
for line in content.split('\\n'):
    content_new.append(line)
    if "if (!isInPipMode && !isLocked) {" in line:
        pass # just a marker

# Actually, a better way to handle taps when locked is:
tap_catch_box = """
                    // Catch taps when locked to show the unlock button
                    if (isLocked) {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                                    controlsVisible = true
                                    resetTimer()
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        )
                    }
"""

if new_tap_zones in content:
    content = content.replace(new_tap_zones, tap_catch_box + new_tap_zones)
else:
    print("Could not insert tap_catch_box")

with open("app/src/main/java/com/deepeye/musicpro/ui/components/HybridPlayerCard.kt", "w") as f:
    f.write(content)

print("Patch applied successfully.")

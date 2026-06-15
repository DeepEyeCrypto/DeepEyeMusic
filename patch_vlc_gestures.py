import re

with open('app/src/main/java/com/deepeye/musicpro/ui/components/HybridPlayerCard.kt', 'r') as f:
    content = f.read()

# 1. Add VLC state variables and audioManager near the top of HybridPlayerCard
vlc_state_code = """
    // VLC Gesture OSD States
    var volumeOsd by remember { mutableStateOf<Int?>(null) }
    var brightnessOsd by remember { mutableStateOf<Int?>(null) }
    var seekOsd by remember { mutableStateOf<Long?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
    val activity = context as? android.app.Activity
"""

content = re.sub(
    r'(val context = androidx\.compose\.ui\.platform\.LocalContext\.current)',
    r'\1\n' + vlc_state_code,
    content,
    count=1
)

# 2. Replace the simple vertical drag with full VLC gestures
old_gesture = """                        // Vertical drag overlay for fullscreen exit — uses Final pass
                        // so child tap zones (which use Main pass) get priority.
                        // Only consumes clearly vertical drags; taps and horizontal
                        // movements pass through uninterrupted.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(isFullscreen) {
                                    if (!isFullscreen) return@pointerInput
                                    awaitEachGesture {
                                        val down = awaitFirstDown(
                                            requireUnconsumed = false,
                                            pass = PointerEventPass.Final
                                        )
                                        val slopThreshold = viewConfiguration.touchSlop
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Final)
                                            val change = event.changes.firstOrNull { it.id == down.id }
                                            if (change == null || !change.pressed) break
                                            val dy = change.position.y - down.position.y
                                            val dx = change.position.x - down.position.x
                                            // Only consume if clearly vertical (2:1 ratio)
                                            if (kotlin.math.abs(dy) > slopThreshold * 2 &&
                                                kotlin.math.abs(dy) > kotlin.math.abs(dx) * 2f
                                            ) {
                                                if (dy > 50f) {
                                                    fullscreenMode.exit()
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }
                        )"""

new_gesture = """                        // VLC-style gesture overlay — only rendered in fullscreen
                        // Uses Final pass so child tap zones (Main pass) get priority.
                        // Only consumes actual drags; taps pass through uninterrupted.
                        if (isFullscreen) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    var initialBrightness = 0f
                                    var initialVolume = 0
                                    var initialSeek = 0L
                                    var maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                                    var dragDirection: Int? = null // 1: Horizontal (Seek), 2: Vertical Left (Brightness), 3: Vertical Right (Volume), 4: Exit
                                    
                                    awaitEachGesture {
                                        val down = awaitFirstDown(
                                            requireUnconsumed = false,
                                            pass = PointerEventPass.Final
                                        )
                                        val screenWidth = size.width
                                        val screenHeight = size.height
                                        val slopThreshold = viewConfiguration.touchSlop
                                        
                                        initialBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
                                        if (initialBrightness < 0f) initialBrightness = 0.5f // fallback
                                        initialVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                                        initialSeek = player.currentPosition
                                        dragDirection = null
                                        isDragging = false

                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Final)
                                            val change = event.changes.firstOrNull { it.id == down.id }
                                            if (change == null || !change.pressed) {
                                                if (isDragging) {
                                                    // Apply Seek if it was a seek drag
                                                    seekOsd?.let { 
                                                        player.seekTo(it) 
                                                        onSeekTo(it)
                                                    }
                                                }
                                                // Reset states on pointer up
                                                isDragging = false
                                                brightnessOsd = null
                                                volumeOsd = null
                                                seekOsd = null
                                                dragDirection = null
                                                break
                                            }
                                            
                                            val dy = change.position.y - down.position.y
                                            val dx = change.position.x - down.position.x
                                            
                                            // Determine direction if not locked yet
                                            if (dragDirection == null) {
                                                if (kotlin.math.abs(dy) > slopThreshold * 2 || kotlin.math.abs(dx) > slopThreshold * 2) {
                                                    isDragging = true
                                                    if (kotlin.math.abs(dy) > kotlin.math.abs(dx) * 1.5f) {
                                                        // Vertical drag
                                                        if (dy > 50f && down.position.y < screenHeight * 0.2f) {
                                                            // Pull down from top -> Exit Fullscreen
                                                            dragDirection = 4
                                                        } else if (down.position.x < screenWidth / 2) {
                                                            // Left side vertical -> Brightness
                                                            dragDirection = 2
                                                        } else {
                                                            // Right side vertical -> Volume
                                                            dragDirection = 3
                                                        }
                                                    } else {
                                                        // Horizontal drag -> Seek
                                                        dragDirection = 1
                                                    }
                                                }
                                            }

                                            // Apply Gesture
                                            if (dragDirection != null) {
                                                change.consume() // Consume drag so children don't process it as scroll
                                                resetTimer()
                                                when (dragDirection) {
                                                    1 -> { // Seek
                                                        val seekDeltaMs = ((dx / screenWidth) * 90000f).toLong() // Max 90s swipe per screen width
                                                        val targetSeek = (initialSeek + seekDeltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
                                                        seekOsd = targetSeek
                                                    }
                                                    2 -> { // Brightness
                                                        val deltaB = -(dy / screenHeight) * 1.5f // Negative because up is minus Y
                                                        val newBrightness = (initialBrightness + deltaB).coerceIn(0.01f, 1f)
                                                        activity?.window?.attributes = activity?.window?.attributes?.apply {
                                                            screenBrightness = newBrightness
                                                        }
                                                        brightnessOsd = (newBrightness * 100).toInt()
                                                    }
                                                    3 -> { // Volume
                                                        val deltaV = -(dy / screenHeight) * maxVolume * 1.5f
                                                        val newVolume = (initialVolume + deltaV).toInt().coerceIn(0, maxVolume)
                                                        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume, 0)
                                                        volumeOsd = ((newVolume.toFloat() / maxVolume) * 100).toInt()
                                                    }
                                                    4 -> { // Exit
                                                        if (dy > 50f) {
                                                            fullscreenMode.exit()
                                                            break
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                        )
                        }"""

content = content.replace(old_gesture, new_gesture)

# 3. Add the VLC OSD Overlay
vlc_osd_code = """
                        // VLC GESTURE OSD OVERLAY
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isDragging && (brightnessOsd != null || volumeOsd != null || seekOsd != null),
                            enter = androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.fadeOut(),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            androidx.compose.material3.Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (brightnessOsd != null) {
                                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = Color.White) // Fallback icon
                                        Spacer(Modifier.width(8.dp))
                                        Text(text = "${brightnessOsd}%", color = Color.White, fontWeight = FontWeight.FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                    } else if (volumeOsd != null) {
                                        Icon(imageVector = if (volumeOsd!! == 0) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color.White)
                                        Spacer(Modifier.width(8.dp))
                                        Text(text = "${volumeOsd}%", color = Color.White, fontWeight = FontWeight.FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                    } else if (seekOsd != null) {
                                        val delta = seekOsd!! - player.currentPosition
                                        val sign = if (delta > 0) "+" else ""
                                        Text(text = "$sign${delta / 1000}s", color = Color.White, fontWeight = FontWeight.FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                        Spacer(Modifier.width(8.dp))
                                        Text(text = "[${com.deepeye.musicpro.core.utils.TimeFormatter.formatDuration(seekOsd!!)}]", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        }
"""

content = re.sub(
    r'(// BOTTOM GLASSMORPHIC QUICK MEDIA CONTROL PANEL OVERLAY)',
    vlc_osd_code + r'\n                        \1',
    content,
    count=1
)

with open('app/src/main/java/com/deepeye/musicpro/ui/components/HybridPlayerCard.kt', 'w') as f:
    f.write(content)

import re

with open("app/src/main/java/com/deepeye/musicpro/ui/player/NowPlayingScreen.kt", "r") as f:
    content = f.read()

# 1. Remove showTasteDetailDialog
content = content.replace("    var showTasteDetailDialog by remember { mutableStateOf(false) }\n", "")

# 2. Add movableContentOf and remove spacerRect/rootRect logic
old_box = """    val density = androidx.compose.ui.platform.LocalDensity.current
    var rootRect by remember { mutableStateOf(Rect.Zero) }
    var spacerRect by remember { mutableStateOf(Rect.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                rootRect = Rect(
                    offset = it.positionInWindow(),
                    size = Size(it.size.width.toFloat(), it.size.height.toFloat())
                )
            }
    ) {"""

new_box = """    val playerContent = remember {
        movableContentOf<Modifier> { modifier ->
            val innerItem = playerState.currentItem
            val innerIsVideo = innerItem is MediaItem.Remote && playerState.isVideo
            if (innerIsVideo) {
                com.deepeye.musicpro.ui.components.HybridPlayerCard(
                    item = innerItem,
                    player = viewModel.player,
                    isVideo = true,
                    isLoading = playerState.isLoading,
                    isPlaying = playerState.isPlaying,
                    playbackPosition = playerState.position,
                    modifier = modifier,
                    onTogglePlayPause = { viewModel.togglePlayPause() }
                )
            } else {
                Box(
                    modifier = modifier.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (innerItem != null) {
                        AsyncImage(
                            model = innerItem.artworkUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .aspectRatio(1f)
                                .blur(36.dp)
                                .alpha(0.65f)
                                .graphicsLayer { translationY = 15f },
                            contentScale = ContentScale.Crop
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .aspectRatio(1f)
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = innerItem?.artworkUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        NowPlayingVisualizerOverlay(
                            fftData = fftData,
                            dominantColor = dominantColor,
                            isPlaying = playerState.isPlaying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {"""
content = content.replace(old_box, new_box)

# 3. Change ArtworkOrVideoSection
old_artwork = """            @Composable
            fun ArtworkOrVideoSection(modifier: Modifier = Modifier) {
                Spacer(
                    modifier = modifier.onGloballyPositioned {
                        spacerRect = Rect(
                            offset = it.positionInWindow(),
                            size = Size(it.size.width.toFloat(), it.size.height.toFloat())
                        )
                    }
                )
            }"""
new_artwork = """            @Composable
            fun ArtworkOrVideoSection(modifier: Modifier = Modifier) {
                playerContent(modifier)
            }"""
content = content.replace(old_artwork, new_artwork)

# 4. TrackMetadataSection
old_track = """            @Composable
            fun TrackMetadataSection() {
                val feedback by viewModel.currentSongFeedback.collectAsStateWithLifecycle()
                val tasteProfile by viewModel.tasteProfile.collectAsStateWithLifecycle()
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = playerState.currentItem?.title ?: "No Track Playing",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = playerState.currentItem?.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    val item = playerState.currentItem
                    if (item != null) {
                        val tags = remember(item, feedback, tasteProfile) {
                            val list = mutableListOf<String>()
                            if (feedback?.liked == true) {
                                list.add("Liked Song")
                            }
                            
                            val isFavArtist = tasteProfile.favoriteArtists.any { it.equals(item.artist, ignoreCase = true) }
                            if (isFavArtist) {
                                list.add("Favorite Artist")
                            }
                            
                            val titleLower = item.title.lowercase()
                            val detectedLanguage = when {
                                titleLower.contains("hindi") -> "Hindi"
                                titleLower.contains("punjabi") -> "Punjabi"
                                titleLower.contains("bhojpuri") -> "Bhojpuri"
                                titleLower.contains("tamil") -> "Tamil"
                                titleLower.contains("telugu") -> "Telugu"
                                titleLower.contains("english") -> "English"
                                titleLower.contains("haryanvi") -> "Haryanvi"
                                titleLower.contains("bengali") -> "Bengali"
                                titleLower.contains("korean") -> "Korean"
                                else -> ""
                            }
                            if (detectedLanguage.isNotEmpty() && tasteProfile.preferredLanguages.contains(detectedLanguage)) {
                                list.add(detectedLanguage)
                            }
                            
                            if (list.isEmpty()) {
                                list.add("Personalized Mix")
                            }
                            list
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showTasteDetailDialog = true }
                        ) {
                            tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = tag.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }"""
new_track = """            @Composable
            fun TrackMetadataSection() {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = playerState.currentItem?.title ?: "No Track Playing",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = playerState.currentItem?.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }"""
content = content.replace(old_track, new_track)

# 5. Remove long press on Autoplay
old_autoplay = """                                .combinedClickable(
                                    onClick = { viewModel.toggleAutoplay() },
                                    onLongClick = {
                                        if (playerState.autoplayEnabled) {
                                            showTasteDetailDialog = true
                                        }
                                    }
                                )
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = "Autoplay Mode",
                                    tint = if (playerState.autoplayEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (playerState.autoplayEnabled) "AUTOPLAY: TASTE" else "AUTOPLAY OFF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (playerState.autoplayEnabled) Color.White else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }"""
new_autoplay = """                                .clickable { viewModel.toggleAutoplay() }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = "Autoplay Mode",
                                    tint = if (playerState.autoplayEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (playerState.autoplayEnabled) "AUTOPLAY ON" else "AUTOPLAY OFF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (playerState.autoplayEnabled) Color.White else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }"""
content = content.replace(old_autoplay, new_autoplay)

# 6. Change else branch
old_else = """        } else {
            // Fullscreen or PiP mode background
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        }"""
new_else = """        } else {
            // Fullscreen or PiP mode background
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                playerContent(Modifier.fillMaxSize())
            }
        }"""
content = content.replace(old_else, new_else)

# 7. Delete the rest from overlay down to the end, leaving only the outer Box's brace
import sys
idx = content.find("        // --- THE PLAYER CARD OVERLAY ---")
if idx == -1:
    print("Overlay not found!")
    sys.exit(1)

content = content[:idx] + "    }\n}\n"

with open("app/src/main/java/com/deepeye/musicpro/ui/player/NowPlayingScreen.kt", "w") as f:
    f.write(content)

print("Modifications applied successfully.")

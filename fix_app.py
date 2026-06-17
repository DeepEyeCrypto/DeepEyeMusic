import re

file_path = "app/src/main/java/com/deepeye/musicpro/ui/DeepEyeMusicApp.kt"
with open(file_path, "r") as f:
    code = f.read()

# Replace the AnchoredMiniPlayer block with the new PiP logic
old_block = r"if \(playerState\.currentItem != null && !isInPipMode\) \{.*?AnchoredMiniPlayer\(.*?\n        \}"
new_block = """if (playerState.currentItem != null && !isInPipMode) {
            androidx.activity.compose.BackHandler(
                enabled = sheetState.anchor == com.deepeye.musicpro.ui.player.MiniSheetAnchor.EXPANDED ||
                    sheetState.anchor == com.deepeye.musicpro.ui.player.MiniSheetAnchor.HALF_EXPANDED
            ) {
                sheetViewModel.collapse()
            }

            val exactDockHeight = if (isBottomBar && showBottomBar) {
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 100.dp
            } else {
                0.dp
            }
            
            com.deepeye.musicpro.ui.player.AnchoredMiniPlayer(
                playerState = playerState,
                sheetState = sheetState,
                onTogglePlayPause = { playerController.togglePlayPause() },
                onSeekTo = { playerController.seekTo(it) },
                onNext = { playerController.next() },
                onPrevious = { playerController.previous() },
                onExpand = { sheetViewModel.expand() },
                onCollapse = { sheetViewModel.collapse() },
                bottomPadding = exactDockHeight,
                playerController = playerController,
            )
        }

        if (isInPipMode && playerState.currentItem != null) {
            Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
                com.deepeye.musicpro.ui.components.HybridPlayerCard(
                    item = playerState.currentItem!!,
                    player = playerController.player,
                    isVideo = playerState.isVideo,
                    isLoading = playerState.isLoading,
                    isPlaying = playerState.isPlaying,
                    playbackPosition = playerState.position,
                    onTogglePlayPause = { playerController.togglePlayPause() },
                    onSeekTo = { playerController.seekTo(it) }
                )
            }
        }"""

code = re.sub(r'if \(playerState\.currentItem != null && !isInPipMode\) \{.*?\)\n        \}', new_block, code, flags=re.DOTALL)

with open(file_path, "w") as f:
    f.write(code)

print("DeepEyeMusicApp patched successfully")

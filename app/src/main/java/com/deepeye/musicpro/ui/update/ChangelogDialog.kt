package com.deepeye.musicpro.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.deepeye.musicpro.updates.ChangelogEntry

// Brand colors
private val TealGlow = Color(0xFF00D2FF)
private val PurpleGlow = Color(0xFF7C4DFF)
private val TextPrimary = Color(0xFFE8E8E8)
private val TextSecondary = Color(0xFFB0B0B0)
private val TextMuted = Color(0xFF808080)
private val SurfaceDark = Color(0xFF121218)
private val SurfaceCard = Color(0xFF1A1A24)

@Composable
fun ChangelogDialog(
    entries: List<ChangelogEntry>,
    onDismiss: () -> Unit,
    onLater: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties =
        DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier =
            Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(SurfaceCard, SurfaceDark),
                    ),
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(TealGlow.copy(alpha = 0.25f), PurpleGlow.copy(alpha = 0.15f)),
                    ),
                    RoundedCornerShape(28.dp),
                ),
        ) {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                // ── Header ──
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.NewReleases,
                            contentDescription = null,
                            tint = TealGlow,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "What's New",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                            Text(
                                "Updated features and fixes",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                        }
                    }
                    IconButton(onClick = onLater) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Scrollable Changelog ──
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(entries) { entry ->
                        ChangelogReleaseCard(entry)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Action Buttons ──
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onLater,
                        shape = RoundedCornerShape(14.dp),
                        colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary,
                        ),
                    ) {
                        Text("Later")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors =
                        ButtonDefaults.buttonColors(
                            containerColor = TealGlow,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Text("Got it", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ChangelogReleaseCard(entry: ChangelogEntry) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (entry.highlight) {
                    TealGlow.copy(alpha = 0.08f)
                } else {
                    Color.White.copy(alpha = 0.03f)
                },
            )
            .border(
                1.dp,
                if (entry.highlight) {
                    TealGlow.copy(alpha = 0.2f)
                } else {
                    Color.White.copy(alpha = 0.06f)
                },
                RoundedCornerShape(18.dp),
            )
            .padding(14.dp),
    ) {
        // Version + Title row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                entry.versionName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TealGlow,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                entry.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
        }

        if (entry.releaseDate.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                entry.releaseDate,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }

        Spacer(Modifier.height(10.dp))

        // Bullet items
        entry.items.forEach { bullet ->
            Row(
                Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text("•", color = TealGlow, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                Text(
                    bullet,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

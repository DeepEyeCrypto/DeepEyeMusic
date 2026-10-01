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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.deepeye.musicpro.ui.theme.ContentBounds
import com.deepeye.musicpro.updates.ChangelogEntry
import com.deepeye.musicpro.ui.theme.ProvideAppZoom

// Brand colors
private val TealGlow = Color(0xFF00D2FF)
private val PurpleGlow = Color(0xFF7C4DFF)
private val TextPrimary = Color(0xFFE8E8E8)
private val TextSecondary = Color(0xFFB0B0B0)
private val TextMuted = Color(0xFF808080)
private val SurfaceDark = Color(0xFF121218)
private val SurfaceCard = Color(0xFF1A1A24)

/**
 * Gap kept between the card and the top/bottom edges of the dialog window.
 *
 * Half of this lands on each edge, so the card reads as floating rather than as
 * a full-bleed sheet, and the rounded corners are never clipped by the bezel.
 */
private val DialogVerticalMargin = 32.dp

/**
 * Floor for [DialogVerticalMargin]-adjusted card height, so a very short
 * viewport (split-screen, or a landscape phone in a cramped multi-window slot)
 * degrades to "a small scrollable card" rather than a negative or zero height.
 */
private val MinDialogCardHeight = 200.dp

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
        // Dialogs get their own window and therefore their own density; without
        // this the card renders at system scale while the screen behind it is
        // zoomed. See ProvideAppZoom for the measured evidence.
        ProvideAppZoom {
        // The card is clamped to the viewport, which is what makes the action
        // row reachable.
        //
        // `weight(1f, fill = false)` on the body alone does NOT do this. A Column
        // with an unbounded max height measures a `fill = false` weighted child at
        // its own natural height, so the body grew to fit every changelog entry
        // and the `heightIn(max = 420.dp)` that used to sit on it was itself
        // larger than a 360dp-tall landscape viewport (720px / density 2.0).
        // Nothing ever engaged the scroll, and "Later" / "Got it" ended up ~240dp
        // below the fold, unclickable.
        //
        // Bounding the card gives the Column a finite budget, so the weighted body
        // is measured against the space the header and buttons leave behind and
        // scrolls within it. Title and buttons stay pinned because they are
        // non-weighted siblings.
        //
        // `usePlatformDefaultWidth = false` makes the dialog window MATCH_PARENT,
        // so the window height is the screen height. LocalConfiguration is
        // per-context and is unaffected by [ProvideAppZoom]'s density override,
        // so it still reports the true unzoomed height here.
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val dialogMaxHeight =
            (screenHeight - DialogVerticalMargin).coerceAtLeast(MinDialogCardHeight)
        // Outer Box exists solely to centre the capped dialog.
        //
        // `usePlatformDefaultWidth = false` makes the Dialog window fill the
        // screen and places its content at the *top start*. Once the card stops
        // stretching to 92% of the viewport it would otherwise sit hard against
        // the left edge with all of its margin on one side only.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier =
            Modifier
                // Bounded, not raw `fillMaxWidth(0.92f)`. A percentage of an
                // ~890dp landscape viewport is still ~820dp, so the dialog ran
                // nearly the full width of the screen and every release-note
                // line stretched to a single short sentence 800dp wide.
                //
                // `widthIn` first, then the percentage fill: this way the
                // percentage is applied to the *capped* width, so the dialog is
                // 92% of 560dp on a wide viewport and 92% of the screen on a
                // narrow one. Reversing the order would let the percentage
                // override the cap.
                .widthIn(max = ContentBounds.dialog)
                .fillMaxWidth(0.92f)
                // Must precede `.clip`/`.background` so the clamp bounds the
                // card, not just its painted edges.
                .heightIn(max = dialogMaxHeight)
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
                    // `weight(1f, fill = false)` — take only what the header and
                    // the action row leave over, and no more than that.
                    //
                    // The `heightIn(max = 420.dp)` that used to sit here is gone.
                    // 420.dp is *larger than the entire landscape viewport* this
                    // dialog ships on (360dp = 720px / density 2.0), so it never
                    // clamped anything; the body simply grew to fit every entry
                    // and pushed "Later" / "Got it" roughly 240dp below the fold,
                    // where they could not be tapped. A hardcoded dp cap cannot be
                    // right across portrait, landscape and split-screen anyway.
                    //
                    // Weighting is the structural form of the same idea: the
                    // siblings above and below are measured first, so the body is
                    // measured against the leftover budget, bounded by the card's
                    // own `heightIn(max = dialogMaxHeight)`. A LazyColumn already
                    // clamps to the constraint it is measured with, so no explicit
                    // cap is needed here.
                    modifier = Modifier.weight(1f, fill = false),
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

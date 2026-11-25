package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.GraphicsQuality
import com.example.engine.SeatSnapshot
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.LocalTablePalette
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TablePalette
import com.example.ui.theme.TextPrimaryLight

/**
 * Rich Casino Felt & Mahogany Rail Game Table Background.
 * Dynamically adapts texture density to GraphicsQuality for low-RAM devices.
 */
@Composable
fun LuxuryTableSurface(
    palette: TablePalette = LocalTablePalette.current,
    graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Outer mahogany / obsidian rail
            drawRect(color = palette.railOuter)

            // Main baize felt with warm overhead chandelier radial spotlight
            val inset = 8.dp.toPx()
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(palette.feltCenter, palette.feltMid, palette.feltEdge),
                    center = Offset(w * 0.5f, h * 0.42f),
                    radius = maxOf(w, h) * 0.72f
                ),
                topLeft = Offset(inset, inset),
                size = Size(w - inset * 2, h - inset * 2),
                cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
            )

            // Subtle woven baize felt micro-stipple on Medium/High/Ultra
            if (graphicsQuality != GraphicsQuality.LOW) {
                val step = if (graphicsQuality == GraphicsQuality.ULTRA) 24.dp.toPx() else 36.dp.toPx()
                var y = inset + 20f
                while (y < h - inset) {
                    var x = inset + 20f
                    while (x < w - inset) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.024f),
                            radius = 1.2f,
                            center = Offset(x, y)
                        )
                        x += step
                    }
                    y += step
                }
            }

            // Inlaid brass double casino table trim line
            val trim1 = 16.dp.toPx()
            drawRoundRect(
                color = palette.lineGold,
                topLeft = Offset(trim1, trim1),
                size = Size(w - trim1 * 2, h - trim1 * 2),
                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                style = Stroke(width = 1.3.dp.toPx())
            )

            // Center playing arena subtle medallion ring
            drawCircle(
                color = palette.lineGold.copy(alpha = 0.22f),
                radius = minOf(w, h) * 0.25f,
                center = Offset(w * 0.5f, h * 0.44f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        content()
    }
}

/** Animated player seat badge showing avatar initial, name, cards remaining, score/bid, dealer chip, & glowing turn ring */
@Composable
fun PlayerSeatBadge(
    seat: SeatSnapshot,
    compact: Boolean = false,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "turn_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse_alpha"
    )
    val borderCol = when {
        seat.isActiveTurn -> ChampagneGold.copy(alpha = if (reducedMotion) 1f else pulseAlpha)
        seat.isEliminated -> Color(0xFF475750)
        else -> Color.White.copy(alpha = 0.18f)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(ObsidianNight.copy(alpha = if (seat.isEliminated) 0.52f else 0.78f))
            .border(if (seat.isActiveTurn) 1.8.dp else 1.dp, borderCol, RoundedCornerShape(24.dp))
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Avatar circle with dealer crown badge
        Box(
            modifier = Modifier
                .size(if (compact) 26.dp else 30.dp)
                .clip(CircleShape)
                .background(
                    if (seat.teamId == 0) Brush.linearGradient(listOf(BurnishedGold, Color(0xFF8C6921)))
                    else Brush.linearGradient(listOf(Color(0xFF39564C), Color(0xFF1A2E27)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = seat.name.take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (seat.teamId == 0) ObsidianNight else TextPrimaryLight
                )
            )
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = seat.name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (seat.isActiveTurn) ChampagneGold else TextPrimaryLight
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (seat.isDealer) {
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(BurnishedGold)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text("D", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = ObsidianNight)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${seat.cardCount}🃏",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFB7C7BF))
                )
                if (seat.statusBadge.isNotEmpty()) {
                    Text(
                        text = seat.statusBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (seat.isActiveTurn) SuccessEmerald else ChampagneGold,
                            fontWeight = FontWeight.Bold
                        )
                    )
                } else if (seat.totalScore != 0.0) {
                    val fmt = if (seat.totalScore % 1.0 == 0.0) seat.totalScore.toInt().toString() else String.format("%.1f", seat.totalScore)
                    Text(
                        text = "$fmt pts",
                        style = MaterialTheme.typography.labelSmall.copy(color = ChampagneGold)
                    )
                }
            }
        }
    }
}

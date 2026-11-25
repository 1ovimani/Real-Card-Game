package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.model.Card
import com.example.core.model.CardBackStyle
import com.example.core.model.Rank
import com.example.core.model.Suit
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ForestSuit
import com.example.ui.theme.IvoryStock
import com.example.ui.theme.IvoryWarm
import com.example.ui.theme.OnyxInk
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.RubyCrimson
import com.example.ui.theme.SapphireSuit

/**
 * World-Class Realistic Playing Card Renderer.
 * - Custom vector Bezier paths for Spades, Hearts, Diamonds, Clubs (zero emoji substitutes!)
 * - Authentic symmetrical pip layouts (2–10), Ace grand medallion, ornate royal court emblems (J/Q/K)
 * - 4-Color Suit accessibility option (Sapphire Diamonds, Forest Clubs)
 * - Smooth 3D Y-axis flip rotation, tactile lift on selection/playable hint, multi-layer shadows
 * - 4 luxury card back styles including the ornate Royal Filigree artwork
 */
@Composable
fun RealisticPlayingCard(
    card: Card?,
    faceUp: Boolean = true,
    width: Dp = 74.dp,
    height: Dp = 108.dp,
    isPlayable: Boolean = true,
    isSelected: Boolean = false,
    isHinted: Boolean = false,
    colorBlindSuits: Boolean = false,
    cardBackId: String = CardBackStyle.ROYAL_FILIGREE.id,
    reducedMotion: Boolean = false,
    rotationDeg: Float = 0f,
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val liftY by animateDpAsState(
        targetValue = when {
            isSelected -> (-14).dp
            isHinted -> (-6).dp
            else -> 0.dp
        },
        animationSpec = if (reducedMotion) spring(stiffness = Spring.StiffnessHigh) else spring(dampingRatio = 0.68f, stiffness = 420f),
        label = "card_lift"
    )
    val flipAngle by animateFloatAsState(
        targetValue = if (faceUp) 0f else 180f,
        animationSpec = if (reducedMotion) spring(stiffness = Spring.StiffnessHigh) else spring(dampingRatio = 0.78f, stiffness = 320f),
        label = "card_flip"
    )

    val cornerRadius = (width.value * 0.11f).coerceIn(5f, 11f).dp
    val shape = RoundedCornerShape(cornerRadius)
    val showFront = flipAngle <= 90f && card != null
    val desc = if (showFront && card != null) "${card.rank.nameEn} of ${card.suit.nameEn}" else "Face-down playing card"

    val borderColor = when {
        isSelected -> ChampagneGold
        isHinted -> Color(0xFF4CE89F)
        isPlayable && showFront -> Color(0xFFD5C9B0)
        else -> Color(0xFF8B8477)
    }
    val borderWidth = if (isSelected || isHinted) 2.2.dp else 0.9.dp

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .offset(y = liftY)
            .size(width = width, height = height)
            .graphicsLayer {
                rotationZ = rotationDeg
                rotationY = flipAngle
                cameraDistance = 14f * density
                alpha = if (!showFront || isPlayable) 1f else 0.56f
            }
            .shadow(
                elevation = if (isSelected) 12.dp else 5.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.55f),
                spotColor = Color.Black.copy(alpha = 0.65f)
            )
            .clip(shape)
            .border(borderWidth, borderColor, shape)
            .semantics { contentDescription = desc }
            .let { m -> if (testTag != null) m.testTag(testTag) else m }
            .let { m -> if (onClick != null) m.clickable(enabled = isPlayable || !faceUp) { onClick() } else m }
    ) {
        if (showFront && card != null) {
            CardFrontCanvas(
                card = card,
                colorBlindSuits = colorBlindSuits,
                textMeasurer = textMeasurer
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                CardBackArtwork(cardBackId = cardBackId)
            }
        }
    }
}

@Composable
private fun CardFrontCanvas(
    card: Card,
    colorBlindSuits: Boolean,
    textMeasurer: TextMeasurer
) {
    val suitColor = suitInkColor(card.suit, colorBlindSuits)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Realistic ivory linen card paper stock with warm vignette sheen
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFFDF8), IvoryStock, IvoryWarm),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // Subtle inner linen hairline frame for ultra-crisp casino finish
        val inset = w * 0.045f
        drawRoundRect(
            color = Color(0x18B59B65),
            topLeft = Offset(inset, inset),
            size = Size(w - inset * 2, h - inset * 2),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
            style = Stroke(width = 1.2f)
        )

        // 2. Corner Rank & Mini Suit Index (Top-Left + Mirrored Bottom-Right)
        val rankFontSize = (w * 0.195f / density).coerceIn(9f, 19f).sp
        val rankLayout = textMeasurer.measure(
            text = card.rank.label,
            style = TextStyle(
                fontFamily = PlayfairDisplayFamily,
                fontWeight = FontWeight.Bold,
                fontSize = rankFontSize,
                color = suitColor
            )
        )
        val indexX = w * 0.095f
        val indexY = h * 0.045f
        drawText(rankLayout, topLeft = Offset(indexX, indexY))

        val miniPipSize = w * 0.145f
        val miniPipCenterX = indexX + rankLayout.size.width / 2f
        val miniPipCenterY = indexY + rankLayout.size.height + miniPipSize * 0.48f
        drawVectorSuit(card.suit, Offset(miniPipCenterX, miniPipCenterY), miniPipSize, suitColor)

        // Mirrored bottom-right index
        rotate(degrees = 180f, pivot = Offset(w / 2f, h / 2f)) {
            drawText(rankLayout, topLeft = Offset(indexX, indexY))
            drawVectorSuit(card.suit, Offset(miniPipCenterX, miniPipCenterY), miniPipSize, suitColor)
        }

        // 3. Center Artwork: Ace Medallion / Court Royal Crest / Symmetrical Pip Field
        when (card.rank) {
            Rank.ACE -> drawAceCenterpiece(card.suit, w, h, suitColor)
            Rank.JACK, Rank.QUEEN, Rank.KING -> drawRoyalCourtCard(card, w, h, suitColor)
            else -> drawNumberPips(card, w, h, suitColor)
        }
    }
}

private fun DrawScope.drawAceCenterpiece(suit: Suit, w: Float, h: Float, color: Color) {
    val cx = w / 2f
    val cy = h / 2f
    // Subtle gold halo ring behind Ace emblem
    drawCircle(
        color = BurnishedGold.copy(alpha = 0.22f),
        radius = w * 0.28f,
        center = Offset(cx, cy),
        style = Stroke(width = w * 0.022f)
    )
    drawCircle(
        color = BurnishedGold.copy(alpha = 0.08f),
        radius = w * 0.24f,
        center = Offset(cx, cy)
    )
    drawVectorSuit(suit, Offset(cx, cy), w * 0.44f, color)
}

private fun DrawScope.drawRoyalCourtCard(card: Card, w: Float, h: Float, suitColor: Color) {
    val frameLeft = w * 0.23f
    val frameTop = h * 0.17f
    val frameW = w * 0.54f
    val frameH = h * 0.66f
    val cx = w / 2f
    val cy = h / 2f

    // Ornate royal velvet inset panel
    val panelTint = if (card.suit.isRed) Color(0x16C61E2B) else Color(0x14162436)
    drawRoundRect(
        color = panelTint,
        topLeft = Offset(frameLeft, frameTop),
        size = Size(frameW, frameH),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
    )
    drawRoundRect(
        brush = Brush.linearGradient(listOf(BurnishedGold, ChampagneGold, BurnishedGold)),
        topLeft = Offset(frameLeft, frameTop),
        size = Size(frameW, frameH),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
        style = Stroke(width = (w * 0.024f).coerceAtLeast(1.4f))
    )

    // Geometric diamond tapestry lines inside court panel
    drawLine(
        color = BurnishedGold.copy(alpha = 0.35f),
        start = Offset(cx, frameTop + frameH * 0.08f),
        end = Offset(frameLeft + frameW * 0.88f, cy),
        strokeWidth = 1f
    )
    drawLine(
        color = BurnishedGold.copy(alpha = 0.35f),
        start = Offset(frameLeft + frameW * 0.88f, cy),
        end = Offset(cx, frameTop + frameH * 0.92f),
        strokeWidth = 1f
    )
    drawLine(
        color = BurnishedGold.copy(alpha = 0.35f),
        start = Offset(cx, frameTop + frameH * 0.92f),
        end = Offset(frameLeft + frameW * 0.12f, cy),
        strokeWidth = 1f
    )
    drawLine(
        color = BurnishedGold.copy(alpha = 0.35f),
        start = Offset(frameLeft + frameW * 0.12f, cy),
        end = Offset(cx, frameTop + frameH * 0.08f),
        strokeWidth = 1f
    )

    // Crown / Royal Crest silhouette at top of court panel
    val crownW = frameW * 0.56f
    val crownH = frameH * 0.18f
    val crownTop = frameTop + frameH * 0.11f
    val crownPath = Path().apply {
        moveTo(cx - crownW / 2f, crownTop + crownH)
        lineTo(cx - crownW * 0.55f, crownTop + crownH * 0.25f)
        lineTo(cx - crownW * 0.22f, crownTop + crownH * 0.62f)
        lineTo(cx, crownTop)
        lineTo(cx + crownW * 0.22f, crownTop + crownH * 0.62f)
        lineTo(cx + crownW * 0.55f, crownTop + crownH * 0.25f)
        lineTo(cx + crownW / 2f, crownTop + crownH)
        close()
    }
    val crownColor = when (card.rank) {
        Rank.KING -> BurnishedGold
        Rank.QUEEN -> Color(0xFFE0B450)
        else -> Color(0xFFB7995B)
    }
    drawPath(crownPath, color = crownColor)

    // Central Suit Medallion
    drawVectorSuit(card.suit, Offset(cx, cy + frameH * 0.03f), w * 0.29f, suitColor)

    // Mirrored mini crown at bottom
    rotate(180f, pivot = Offset(cx, cy)) {
        drawPath(crownPath, color = crownColor.copy(alpha = 0.65f))
    }
}

private fun DrawScope.drawNumberPips(card: Card, w: Float, h: Float, color: Color) {
    // Authentic symmetrical casino playing card pip coordinates (normalized 0..1)
    val left = 0.33f
    val mid = 0.50f
    val right = 0.67f
    val top = 0.22f
    val upperMid = 0.36f
    val center = 0.50f
    val lowerMid = 0.64f
    val bottom = 0.78f

    // Pair of (x, y, inverted)
    val coords: List<Triple<Float, Float, Boolean>> = when (card.rank) {
        Rank.TWO -> listOf(Triple(mid, top, false), Triple(mid, bottom, true))
        Rank.THREE -> listOf(Triple(mid, top, false), Triple(mid, center, false), Triple(mid, bottom, true))
        Rank.FOUR -> listOf(
            Triple(left, top, false), Triple(right, top, false),
            Triple(left, bottom, true), Triple(right, bottom, true)
        )
        Rank.FIVE -> listOf(
            Triple(left, top, false), Triple(right, top, false),
            Triple(mid, center, false),
            Triple(left, bottom, true), Triple(right, bottom, true)
        )
        Rank.SIX -> listOf(
            Triple(left, top, false), Triple(right, top, false),
            Triple(left, center, false), Triple(right, center, false),
            Triple(left, bottom, true), Triple(right, bottom, true)
        )
        Rank.SEVEN -> listOf(
            Triple(left, top, false), Triple(right, top, false),
            Triple(mid, 0.34f, false),
            Triple(left, center, false), Triple(right, center, false),
            Triple(left, bottom, true), Triple(right, bottom, true)
        )
        Rank.EIGHT -> listOf(
            Triple(left, top, false), Triple(right, top, false),
            Triple(mid, 0.34f, false),
            Triple(left, center, false), Triple(right, center, false),
            Triple(mid, 0.66f, true),
            Triple(left, bottom, true), Triple(right, bottom, true)
        )
        Rank.NINE -> listOf(
            Triple(left, top, false), Triple(right, top, false),
            Triple(left, upperMid, false), Triple(right, upperMid, false),
            Triple(mid, center, false),
            Triple(left, lowerMid, true), Triple(right, lowerMid, true),
            Triple(left, bottom, true), Triple(right, bottom, true)
        )
        Rank.TEN -> listOf(
            Triple(left, top, false), Triple(right, top, false),
            Triple(mid, 0.29f, false),
            Triple(left, upperMid, false), Triple(right, upperMid, false),
            Triple(left, lowerMid, true), Triple(right, lowerMid, true),
            Triple(mid, 0.71f, true),
            Triple(left, bottom, true), Triple(right, bottom, true)
        )
        else -> listOf(Triple(mid, center, false))
    }

    val pipSize = w * if (card.rank.value >= 9) 0.165f else 0.19f
    for ((nx, ny, inverted) in coords) {
        val pos = Offset(w * nx, h * ny)
        if (inverted) {
            rotate(180f, pivot = pos) {
                drawVectorSuit(card.suit, pos, pipSize, color)
            }
        } else {
            drawVectorSuit(card.suit, pos, pipSize, color)
        }
    }
}

/** Crisp vector Bezier paths for all 4 classic playing card suits */
fun DrawScope.drawVectorSuit(suit: Suit, center: Offset, size: Float, color: Color) {
    val r = size / 2f
    val cx = center.x
    val cy = center.y

    when (suit) {
        Suit.HEARTS -> {
            val path = Path().apply {
                moveTo(cx, cy + r * 0.92f)
                cubicTo(cx - r * 1.15f, cy + r * 0.18f, cx - r * 1.08f, cy - r * 0.92f, cx - r * 0.45f, cy - r * 0.92f)
                cubicTo(cx - r * 0.12f, cy - r * 0.92f, cx, cy - r * 0.56f, cx, cy - r * 0.36f)
                cubicTo(cx, cy - r * 0.56f, cx + r * 0.12f, cy - r * 0.92f, cx + r * 0.45f, cy - r * 0.92f)
                cubicTo(cx + r * 1.08f, cy - r * 0.92f, cx + r * 1.15f, cy + r * 0.18f, cx, cy + r * 0.92f)
                close()
            }
            drawPath(path, color = color)
        }
        Suit.DIAMONDS -> {
            val path = Path().apply {
                moveTo(cx, cy - r * 0.96f)
                quadraticTo(cx + r * 0.25f, cy - r * 0.22f, cx + r * 0.80f, cy)
                quadraticTo(cx + r * 0.25f, cy + r * 0.22f, cx, cy + r * 0.96f)
                quadraticTo(cx - r * 0.25f, cy + r * 0.22f, cx - r * 0.80f, cy)
                quadraticTo(cx - r * 0.25f, cy - r * 0.22f, cx, cy - r * 0.96f)
                close()
            }
            drawPath(path, color = color)
        }
        Suit.SPADES -> {
            val path = Path().apply {
                moveTo(cx, cy - r * 0.95f)
                cubicTo(cx + r * 1.12f, cy - r * 0.05f, cx + r * 1.02f, cy + r * 0.72f, cx + r * 0.38f, cy + r * 0.62f)
                cubicTo(cx + r * 0.16f, cy + r * 0.58f, cx + r * 0.12f, cy + r * 0.36f, cx + r * 0.12f, cy + r * 0.36f)
                lineTo(cx + r * 0.36f, cy + r * 0.94f)
                lineTo(cx - r * 0.36f, cy + r * 0.94f)
                lineTo(cx - r * 0.12f, cy + r * 0.36f)
                cubicTo(cx - r * 0.12f, cy + r * 0.36f, cx - r * 0.16f, cy + r * 0.58f, cx - r * 0.38f, cy + r * 0.62f)
                cubicTo(cx - r * 1.02f, cy + r * 0.72f, cx - r * 1.12f, cy - r * 0.05f, cx, cy - r * 0.95f)
                close()
            }
            drawPath(path, color = color)
        }
        Suit.CLUBS -> {
            // Classic 3-lobe clover + flared pedestal stem
            val lobeR = r * 0.43f
            drawCircle(color = color, radius = lobeR, center = Offset(cx, cy - r * 0.42f))
            drawCircle(color = color, radius = lobeR, center = Offset(cx - r * 0.46f, cy + r * 0.12f))
            drawCircle(color = color, radius = lobeR, center = Offset(cx + r * 0.46f, cy + r * 0.12f))
            val stem = Path().apply {
                moveTo(cx - r * 0.12f, cy + r * 0.12f)
                lineTo(cx + r * 0.12f, cy + r * 0.12f)
                lineTo(cx + r * 0.34f, cy + r * 0.92f)
                lineTo(cx - r * 0.34f, cy + r * 0.92f)
                close()
            }
            drawPath(stem, color = color)
        }
    }
}

fun suitInkColor(suit: Suit, colorBlindSuits: Boolean): Color = when (suit) {
    Suit.HEARTS -> RubyCrimson
    Suit.DIAMONDS -> if (colorBlindSuits) SapphireSuit else RubyCrimson
    Suit.CLUBS -> if (colorBlindSuits) ForestSuit else OnyxInk
    Suit.SPADES -> OnyxInk
}

@Composable
fun CardBackArtwork(cardBackId: String) {
    when (cardBackId) {
        CardBackStyle.ROYAL_FILIGREE.id -> {
            Box(modifier = Modifier.fillMaxSize().background(IvoryStock).padding(2.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.img_card_back_royal),
                    contentDescription = "Royal lotus card back",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(5.dp))
                )
            }
        }
        else -> {
            val (bgStart, bgEnd, accent) = when (cardBackId) {
                CardBackStyle.CRIMSON_DAMASK.id -> Triple(Color(0xFF7A1422), Color(0xFF3E0810), ChampagneGold)
                CardBackStyle.EMERALD_JAMDANI.id -> Triple(Color(0xFF0E4B3A), Color(0xFF06231B), BurnishedGold)
                else -> Triple(Color(0xFF1F2329), Color(0xFF0B0D10), ChampagneGold)
            }
            Canvas(modifier = Modifier.fillMaxSize().background(IvoryStock)) {
                val pad = size.width * 0.06f
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(bgStart, bgEnd)),
                    topLeft = Offset(pad, pad),
                    size = Size(size.width - pad * 2, size.height - pad * 2),
                    cornerRadius = CornerRadius(size.width * 0.07f, size.width * 0.07f)
                )
                val inner = pad * 1.8f
                drawRoundRect(
                    color = accent.copy(alpha = 0.78f),
                    topLeft = Offset(inner, inner),
                    size = Size(size.width - inner * 2, size.height - inner * 2),
                    cornerRadius = CornerRadius(size.width * 0.04f, size.width * 0.04f),
                    style = Stroke(width = 1.4f)
                )
                // Jamdani / Lattice Diamond Filigree Grid
                val cx = size.width / 2f
                val cy = size.height / 2f
                val rx = size.width * 0.24f
                val ry = size.height * 0.22f
                val diamond = Path().apply {
                    moveTo(cx, cy - ry)
                    lineTo(cx + rx, cy)
                    lineTo(cx, cy + ry)
                    lineTo(cx - rx, cy)
                    close()
                }
                drawPath(diamond, color = accent.copy(alpha = 0.22f))
                drawPath(diamond, color = accent.copy(alpha = 0.85f), style = Stroke(width = 1.5f))
                drawCircle(color = accent, radius = size.width * 0.065f, center = Offset(cx, cy))
            }
        }
    }
}

/** Compact vector suit icon composable for Trump indicators & suit pickers */
@Composable
fun SuitVectorBadge(
    suit: Suit,
    size: Dp = 20.dp,
    colorBlindSuits: Boolean = false,
    tintOverride: Color? = null,
    modifier: Modifier = Modifier
) {
    val color = tintOverride ?: suitInkColor(suit, colorBlindSuits)
    Canvas(modifier = modifier.size(size)) {
        drawVectorSuit(suit, Offset(this.size.width / 2f, this.size.height / 2f), this.size.minDimension * 0.92f, color)
    }
}

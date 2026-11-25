package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.SalonChipBadge
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldVelvet
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextSecondaryLight
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val strings = LocalAppStrings.current
    var visible by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.78f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(650),
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(1350L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(EmeraldVelvet, EmeraldDeep, ObsidianNight)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .scale(scale)
                .alpha(alpha)
        ) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .border(2.dp, BurnishedGold, RoundedCornerShape(30.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_wcm_logo_fg),
                    contentDescription = "World Card Master emblem",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = strings.appTitle,
                style = MaterialTheme.typography.displayMedium.copy(
                    color = ChampagneGold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = strings.appSubtitle,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryLight)
            )
            Spacer(Modifier.height(18.dp))
            SalonChipBadge(text = strings.offlineReadyBadge, accent = SuccessEmerald)
        }
    }
}

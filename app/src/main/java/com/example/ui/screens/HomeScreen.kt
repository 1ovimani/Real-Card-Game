package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.model.AppLanguage
import com.example.core.model.GameCatalog
import com.example.core.model.GameCategory
import com.example.core.model.GameInfo
import com.example.core.storage.PlayerProfileEntity
import com.example.core.storage.SavedGameEntity
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.components.SalonChipBadge
import com.example.ui.components.SalonSectionHeader
import com.example.ui.components.VelvetSecondaryButton
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldElevated
import com.example.ui.theme.EmeraldVelvet
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.viewmodel.MainTab

@Composable
fun HomeScreen(
    language: AppLanguage,
    profile: PlayerProfileEntity,
    activeSave: SavedGameEntity?,
    favoriteIds: Set<String>,
    onPlayGame: (String) -> Unit,
    onResumeGame: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleLanguage: () -> Unit,
    onNavigateTab: (MainTab, String?) -> Unit,
    onOpenManual: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val isBn = language == AppLanguage.BANGLA

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(EmeraldDeep, ObsidianNight))),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Header: Brand + Instant Bangla/English Language Toggle + Chips/Stats Pill
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.appTitle,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = ChampagneGold,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = strings.appSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Instant Language Switch Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(EmeraldElevated.copy(alpha = 0.8f))
                            .border(1.dp, BurnishedGold.copy(alpha = 0.7f), RoundedCornerShape(50))
                            .testTag("home_language_toggle")
                            .clickable { onToggleLanguage() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Translate, contentDescription = "Switch Language", tint = ChampagneGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (isBn) "EN" else "বাংলা",
                                style = MaterialTheme.typography.labelMedium.copy(color = ChampagneGold, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Virtual Chips & Career Stats Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatPill(
                    label = strings.virtualChips,
                    value = "${profile.virtualChips}",
                    accent = ChampagneGold,
                    modifier = Modifier.weight(1.2f)
                )
                StatPill(
                    label = strings.winsLabel,
                    value = "${profile.totalWins}",
                    accent = SuccessEmerald,
                    modifier = Modifier.weight(0.9f)
                )
                StatPill(
                    label = strings.gamesPlayedLabel,
                    value = "${profile.totalGamesPlayed}",
                    accent = Color(0xFF8EC5FC),
                    modifier = Modifier.weight(0.9f)
                )
            }
        }

        // Flagship Hero Salon Card (Quick Play Twenty-Nine + Continue Saved Game)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(216.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.2.dp, BurnishedGold.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_salon),
                    contentDescription = "Grand Card Salon table",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    EmeraldDeep.copy(alpha = 0.78f),
                                    ObsidianNight.copy(alpha = 0.94f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SalonChipBadge(text = strings.offlineReadyBadge, accent = SuccessEmerald)
                        SalonChipBadge(
                            text = if (isBn) "১২টি সম্পূর্ণ খেলা" else "12 Full Engines",
                            accent = ChampagneGold
                        )
                    }

                    Column {
                        Text(
                            text = if (isBn) "টোয়েন্টি-নাইন (২৯) • গ্র্যান্ড আসর" else "Twenty-Nine (29) • Grand Table",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = ChampagneGold,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (isBn) "গোপন রং, জোড়া, ডাবল/রি-ডাবল ও স্মার্ট পার্টনারশিপ এআই।" else "Hidden Trump, Pair/Marriage, Double & Redouble with authentic partnership AI.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimaryLight.copy(alpha = 0.88f)),
                            maxLines = 2
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GoldPrimaryButton(
                                text = strings.quickPlay + " 29",
                                icon = Icons.Filled.PlayArrow,
                                onClick = { onPlayGame(GameCatalog.GAME_TWENTY_NINE) },
                                testTag = "home_quick_play_29",
                                modifier = Modifier.weight(1f)
                            )
                            if (activeSave != null) {
                                VelvetSecondaryButton(
                                    text = strings.resumeMatch,
                                    icon = Icons.Filled.Replay,
                                    onClick = onResumeGame,
                                    testTag = "home_resume_game",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Continue Active Saved Game Banner (if present)
        if (activeSave != null) {
            item {
                LuxuryGlassCard(
                    borderColor = BurnishedGold.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("continue_saved_card")
                        .clickable { onResumeGame() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(BurnishedGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Replay, contentDescription = null, tint = ChampagneGold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = strings.continueGame,
                                    style = MaterialTheme.typography.labelMedium.copy(color = ChampagneGold)
                                )
                                Text(
                                    text = if (isBn) activeSave.summaryBn else activeSave.summaryEn,
                                    style = MaterialTheme.typography.titleMedium.copy(color = TextPrimaryLight),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(Icons.Filled.PlayArrow, contentDescription = strings.resumeMatch, tint = ChampagneGold)
                    }
                }
            }
        }

        // Quick Action Tiles: Multiplayer / Solitaire / Manual / Favorites
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickShortcutTile(
                    icon = Icons.Filled.Wifi,
                    label = strings.navMultiplayer,
                    sub = if (isBn) "ওয়াই-ফাই / ব্লুটুথ" else "Wi-Fi & BT",
                    onClick = { onNavigateTab(MainTab.MULTIPLAYER, null) },
                    testTag = "home_shortcut_multiplayer",
                    modifier = Modifier.weight(1f)
                )
                QuickShortcutTile(
                    icon = Icons.Filled.Casino,
                    label = strings.solitaireSection,
                    sub = if (isBn) "একক খেলা" else "Klondike",
                    onClick = { onPlayGame(GameCatalog.GAME_KLONDIKE) },
                    testTag = "home_shortcut_solitaire",
                    modifier = Modifier.weight(1f)
                )
                QuickShortcutTile(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = strings.navManual,
                    sub = if (isBn) "বাংলা / EN" else "Rules Guide",
                    onClick = { onNavigateTab(MainTab.MANUAL, null) },
                    testTag = "home_shortcut_manual",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // BANGLADESH CLASSICS DEDICATED FEATURED SECTION
        item {
            SalonSectionHeader(
                title = strings.bangladeshClassics,
                subtitle = if (isBn) "২৯, হাজারী, কল ব্রিজ, কোর্ট পিস, তিন পাত্তি, ভাবী" else "29, Hazari, Call Bridge, Call Break, Court Piece, Teen Patti, Bhabhi",
                actionText = if (isBn) "সব দেখুন" else "See All",
                onActionClick = { onNavigateTab(MainTab.LIBRARY, GameCategory.BANGLADESH_CLASSICS.id) }
            )
            Spacer(Modifier.height(8.dp))

            // Heritage Cultural Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, BurnishedGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    .clickable { onPlayGame(GameCatalog.GAME_HAZARI) }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_bangladesh_banner),
                    contentDescription = "Bangladesh Heritage Card Classics",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(ObsidianNight.copy(alpha = 0.88f), Color.Transparent)))
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(16.dp)
                ) {
                    SalonChipBadge(text = if (isBn) "বাংলাদেশের ঐতিহ্য" else "Bangladesh Priority", accent = ChampagneGold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isBn) "হাজারী (১০০০) খেলুন" else "Play Authentic Hazari (1000)",
                        style = MaterialTheme.typography.titleLarge.copy(color = TextPrimaryLight)
                    )
                    Text(
                        text = if (isBn) "১৩ তাসের ৩-৩-৩-৪ সেট • ত্রয়ী ও রঙিন রান" else "13-card 3-3-3-4 sets • Troy & Color Run",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(GameCatalog.bangladeshClassics, key = { it.id }) { game ->
                    FeaturedGameCard(
                        game = game,
                        isBn = isBn,
                        isFavorite = favoriteIds.contains(game.id),
                        onPlay = { onPlayGame(game.id) },
                        onToggleFav = { onToggleFavorite(game.id) },
                        onRules = { onOpenManual(game.id) }
                    )
                }
            }
        }

        // WORLD CLASSICS SECTION
        item {
            SalonSectionHeader(
                title = strings.worldGames,
                subtitle = if (isBn) "স্পেডস, হার্টস, ক্রেজি এইটস, ব্ল্যাকজ্যাক, সলিটায়ার" else "Spades, Hearts, Crazy Eights, Blackjack 21, Klondike Solitaire",
                actionText = if (isBn) "লাইব্রেরি" else "Browse All",
                onActionClick = { onNavigateTab(MainTab.LIBRARY, null) }
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(GameCatalog.worldClassics + GameCatalog.solitaireGames, key = { it.id }) { game ->
                    FeaturedGameCard(
                        game = game,
                        isBn = isBn,
                        isFavorite = favoriteIds.contains(game.id),
                        onPlay = { onPlayGame(game.id) },
                        onToggleFav = { onToggleFavorite(game.id) },
                        onRules = { onOpenManual(game.id) }
                    )
                }
            }
        }

        // Favorites strip (if user starred any)
        if (favoriteIds.isNotEmpty()) {
            item {
                val favGames = GameCatalog.allGames.filter { favoriteIds.contains(it.id) }
                SalonSectionHeader(title = strings.favorites)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(favGames, key = { "fav_${it.id}" }) { game ->
                        FeaturedGameCard(
                            game = game,
                            isBn = isBn,
                            isFavorite = true,
                            onPlay = { onPlayGame(game.id) },
                            onToggleFav = { onToggleFavorite(game.id) },
                            onRules = { onOpenManual(game.id) }
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(14.dp)) }
    }
}

@Composable
private fun StatPill(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    LuxuryGlassCard(modifier = modifier, borderColor = accent.copy(alpha = 0.35f)) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryLight), maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(color = accent, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun QuickShortcutTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sub: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    LuxuryGlassCard(
        modifier = modifier
            .testTag(testTag)
            .clickable { onClick() },
        borderColor = ObsidianBorder
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EmeraldVelvet),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = ChampagneGold, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium.copy(color = TextPrimaryLight, fontWeight = FontWeight.SemiBold), maxLines = 1)
            Text(sub, style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryLight), maxLines = 1)
        }
    }
}

@Composable
private fun FeaturedGameCard(
    game: GameInfo,
    isBn: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFav: () -> Unit,
    onRules: () -> Unit
) {
    val strings = LocalAppStrings.current
    LuxuryGlassCard(
        borderColor = if (game.isBangladeshClassic) BurnishedGold.copy(alpha = 0.5f) else ObsidianBorder,
        modifier = Modifier
            .width(214.dp)
            .testTag("game_card_${game.id}")
            .clickable { onPlay() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SalonChipBadge(
                    text = if (isBn) game.countryTagBn else game.countryTagEn,
                    accent = if (game.isBangladeshClassic) ChampagneGold else Color(0xFF8EC5FC)
                )
                IconButton(
                    onClick = onToggleFav,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFF6B7A) else TextSecondaryLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isBn) game.nameBn else game.nameEn,
                style = MaterialTheme.typography.titleLarge.copy(color = TextPrimaryLight),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (game.isTeamGame) Icons.Filled.Groups else Icons.Filled.Public,
                    contentDescription = null,
                    tint = BurnishedGold,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${game.recommendedPlayers}P • ${game.deckType.size} ${strings.deckCards}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryLight)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isBn) game.manual.objectiveBn else game.manual.objectiveEn,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldPrimaryButton(
                    text = strings.playNow,
                    onClick = onPlay,
                    testTag = "play_btn_${game.id}",
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onRules,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldVelvet)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = strings.rulePreview, tint = ChampagneGold, modifier = Modifier.size(19.dp))
                }
            }
        }
    }
}

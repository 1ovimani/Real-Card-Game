package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.model.AiDifficulty
import com.example.core.model.AppLanguage
import com.example.core.model.AppSettings
import com.example.core.model.CardBackStyle
import com.example.core.model.GameCatalog
import com.example.core.model.GraphicsQuality
import com.example.core.model.MusicMood
import com.example.core.storage.MatchHistoryEntity
import com.example.core.storage.PlayerProfileEntity
import com.example.ui.components.CardBackArtwork
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.components.SalonChipBadge
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldElevated
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TablePalettes
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight

@Composable
fun SettingsScreen(
    settings: AppSettings,
    profile: PlayerProfileEntity,
    matchHistory: List<MatchHistoryEntity>,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit
) {
    val strings = LocalAppStrings.current
    val isBn = settings.language == AppLanguage.BANGLA

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(EmeraldDeep, ObsidianNight))),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineLarge.copy(color = ChampagneGold, fontWeight = FontWeight.Bold)
            )
        }

        // 1. LANGUAGE SWITCHER (Instant English / বাংলা)
        item {
            LuxuryGlassCard(borderColor = BurnishedGold.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.settingsLanguage, style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            val selected = settings.language == lang
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateSettings { it.copy(language = lang) } },
                                label = { Text(lang.label, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BurnishedGold,
                                    selectedLabelColor = EmeraldDeep
                                ),
                                modifier = Modifier.testTag("lang_chip_${lang.code}")
                            )
                        }
                    }
                }
            }
        }

        // 2. GAME TABLE THEME STUDIO (8 Luxury Themes)
        item {
            LuxuryGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.settingsTableTheme, style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(TablePalettes, key = { it.id }) { pal ->
                            val selected = settings.tableThemeId == pal.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(104.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) EmeraldElevated else ObsidianNight)
                                    .border(if (selected) 2.dp else 1.dp, if (selected) ChampagneGold else ObsidianBorder, RoundedCornerShape(14.dp))
                                    .clickable { onUpdateSettings { it.copy(tableThemeId = pal.id) } }
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Brush.radialGradient(listOf(pal.feltCenter, pal.feltEdge)))
                                        .border(1.5.dp, pal.railAccent, RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = if (isBn) pal.nameBn else pal.nameEn,
                                    style = MaterialTheme.typography.labelSmall.copy(color = if (selected) ChampagneGold else TextPrimaryLight),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. CARD BACK STUDIO
        item {
            LuxuryGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.settingsCardBack, style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(CardBackStyle.entries, key = { it.id }) { style ->
                            val selected = settings.cardBackId == style.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(88.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(if (selected) 2.dp else 1.dp, if (selected) ChampagneGold else ObsidianBorder, RoundedCornerShape(12.dp))
                                    .clickable { onUpdateSettings { it.copy(cardBackId = style.id) } }
                                    .padding(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 58.dp, height = 84.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                ) {
                                    CardBackArtwork(cardBackId = style.id)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (isBn) style.nameBn else style.nameEn,
                                    style = MaterialTheme.typography.labelSmall.copy(color = if (selected) ChampagneGold else TextSecondaryLight),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. AUDIO & MUSIC MIXER
        item {
            LuxuryGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.settingsAudio, style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    SettingSwitchRow(strings.muteAll, settings.muteAll) { v -> onUpdateSettings { it.copy(muteAll = v) } }
                    SettingSliderRow(strings.masterVolume, settings.masterVolume) { v -> onUpdateSettings { it.copy(masterVolume = v) } }
                    SettingSliderRow(strings.musicVolume, settings.musicVolume) { v -> onUpdateSettings { it.copy(musicVolume = v) } }
                    SettingSliderRow(strings.sfxVolume, settings.sfxVolume) { v -> onUpdateSettings { it.copy(sfxVolume = v) } }
                    SettingSwitchRow(strings.bgMusic, settings.musicEnabled) { v -> onUpdateSettings { it.copy(musicEnabled = v) } }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(MusicMood.entries) { mood ->
                            FilterChip(
                                selected = settings.musicMood == mood,
                                onClick = { onUpdateSettings { it.copy(musicMood = mood) } },
                                label = { Text(if (isBn) mood.nameBn else mood.nameEn, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BurnishedGold, selectedLabelColor = EmeraldDeep)
                            )
                        }
                    }
                    SettingSwitchRow(strings.cardSounds, settings.cardSoundsEnabled) { v -> onUpdateSettings { it.copy(cardSoundsEnabled = v) } }
                    SettingSwitchRow(strings.vibration, settings.vibrationEnabled) { v -> onUpdateSettings { it.copy(vibrationEnabled = v) } }
                }
            }
        }

        // 5. GRAPHICS & GAMEPLAY
        item {
            LuxuryGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.settingsGraphics, style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(GraphicsQuality.entries) { q ->
                            FilterChip(
                                selected = settings.graphicsQuality == q,
                                onClick = { onUpdateSettings { it.copy(graphicsQuality = q) } },
                                label = { Text(if (isBn) q.nameBn else q.nameEn) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BurnishedGold, selectedLabelColor = EmeraldDeep)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(strings.settingsGameplay, style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(AiDifficulty.entries) { d ->
                            FilterChip(
                                selected = settings.defaultAiDifficulty == d,
                                onClick = { onUpdateSettings { it.copy(defaultAiDifficulty = d) } },
                                label = { Text(if (isBn) d.nameBn else d.nameEn) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BurnishedGold, selectedLabelColor = EmeraldDeep)
                            )
                        }
                    }
                    SettingSwitchRow(strings.autoSort, settings.autoSortCards) { v -> onUpdateSettings { it.copy(autoSortCards = v) } }
                    SettingSwitchRow(strings.showHints, settings.showHints) { v -> onUpdateSettings { it.copy(showHints = v) } }
                    SettingSwitchRow(strings.confirmQuit, settings.confirmBeforeQuit) { v -> onUpdateSettings { it.copy(confirmBeforeQuit = v) } }
                }
            }
        }

        // 6. ACCESSIBILITY
        item {
            LuxuryGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.settingsAccessibility, style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    SettingSwitchRow(strings.largeCards, settings.largeCards) { v -> onUpdateSettings { it.copy(largeCards = v) } }
                    SettingSwitchRow(strings.highContrast, settings.highContrast) { v -> onUpdateSettings { it.copy(highContrast = v) } }
                    SettingSwitchRow(strings.colorBlindSuits, settings.colorBlindSuits) { v -> onUpdateSettings { it.copy(colorBlindSuits = v) } }
                    SettingSwitchRow(strings.reducedMotion, settings.reducedMotion) { v -> onUpdateSettings { it.copy(reducedMotion = v) } }
                }
            }
        }

        // 7. STATISTICS & RECENT MATCH HISTORY
        item {
            LuxuryGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.settingsStats, style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SalonChipBadge("${strings.virtualChips}: ${profile.virtualChips}", ChampagneGold)
                        SalonChipBadge("${strings.winsLabel}: ${profile.totalWins}/${profile.totalGamesPlayed}", SuccessEmerald)
                    }
                    if (matchHistory.isEmpty()) {
                        Text(
                            text = if (isBn) "এখনো কোনো ম্যাচ সম্পন্ন হয়নি।" else "Complete any match to record your salon history.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight
                        )
                    } else {
                        matchHistory.take(8).forEach { entry ->
                            val g = GameCatalog.byId(entry.gameId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianNight)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(if (isBn) g.nameBn else g.nameEn, style = MaterialTheme.typography.labelLarge, color = TextPrimaryLight)
                                    Text(entry.scoreSummary, style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)
                                }
                                Text(
                                    text = if (entry.playerWon) "WIN" else "PLAYED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (entry.playerWon) SuccessEmerald else ErrorCrimson,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 8. ABOUT, LICENSES & PRIVACY
        item {
            LuxuryGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("World Card Master v1.0", style = MaterialTheme.typography.titleSmall, color = ChampagneGold)
                    Text(
                        text = "100% Offline-First • Original Synthesized Card Audio • Local Room SQLite Storage • Bundled Open-License Google Fonts (Playfair Display, Plus Jakarta Sans, Hind Siliguri). Zero personal data leaves your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimaryLight), modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EmeraldDeep,
                checkedTrackColor = BurnishedGold,
                uncheckedThumbColor = TextSecondaryLight,
                uncheckedTrackColor = ObsidianNight
            )
        )
    }
}

@Composable
private fun SettingSliderRow(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(thumbColor = ChampagneGold, activeTrackColor = BurnishedGold)
        )
    }
}

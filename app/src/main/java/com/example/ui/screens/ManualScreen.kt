package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.model.AppLanguage
import com.example.core.model.GameCatalog
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.components.SalonChipBadge
import com.example.ui.components.VelvetSecondaryButton
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight

@Composable
fun ManualScreen(
    language: AppLanguage,
    selectedGameId: String,
    onSelectGame: (String) -> Unit,
    onToggleLanguage: () -> Unit,
    onPlayGame: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val isBn = language == AppLanguage.BANGLA
    val game = GameCatalog.byId(selectedGameId)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(EmeraldDeep, ObsidianNight))),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.manualTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(color = ChampagneGold, fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                VelvetSecondaryButton(
                    text = if (isBn) "English" else "বাংলা",
                    icon = Icons.Filled.Translate,
                    onClick = onToggleLanguage
                )
            }
        }

        // Game selector pills
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GameCatalog.allGames, key = { it.id }) { g ->
                    FilterChip(
                        selected = g.id == game.id,
                        onClick = { onSelectGame(g.id) },
                        label = { Text(if (isBn) g.nameBn else g.nameEn) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BurnishedGold,
                            selectedLabelColor = EmeraldDeep
                        )
                    )
                }
            }
        }

        // Game Fact Sheet Header
        item {
            LuxuryGlassCard(borderColor = BurnishedGold.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SalonChipBadge(text = if (isBn) game.origin.labelBn else game.origin.labelEn, accent = ChampagneGold)
                        SalonChipBadge(text = "${game.recommendedPlayers}P • ${game.deckType.size} ${strings.deckCards}", accent = SuccessEmerald)
                    }
                    Text(
                        text = if (isBn) "${game.nameBn} (${game.nameEn})" else "${game.nameEn} (${game.nameBn})",
                        style = MaterialTheme.typography.headlineMedium.copy(color = TextPrimaryLight, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Also known as: ${game.altNames}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryLight)
                    )
                    GoldPrimaryButton(
                        text = "${strings.playNow}: ${if (isBn) game.nameBn else game.nameEn}",
                        icon = Icons.Filled.PlayArrow,
                        onClick = { onPlayGame(game.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item { ManualSectionCard(strings.manualIntro, if (isBn) game.manual.introBn else game.manual.introEn) }
        item { ManualSectionCard(strings.manualObjective, if (isBn) game.manual.objectiveBn else game.manual.objectiveEn) }
        item { ManualSectionCard(strings.manualDeck, if (isBn) game.manual.deckDealBn else game.manual.deckDealEn) }
        item { ManualSectionCard(strings.manualRules, if (isBn) game.manual.rulesBn else game.manual.rulesEn) }
        item { ManualSectionCard(strings.manualScoring, if (isBn) game.manual.scoringBn else game.manual.scoringEn) }
        item { ManualSectionCard(strings.manualStrategy, if (isBn) game.manual.strategyBn else game.manual.strategyEn, accent = SuccessEmerald) }
        item { ManualSectionCard(strings.manualFaq, if (isBn) game.manual.faqBn else game.manual.faqEn) }

        // Research Provenance & Rule Profiles
        item {
            LuxuryGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(strings.manualSource, style = MaterialTheme.typography.titleSmall, color = ChampagneGold)
                    Text("Source: ${game.sourceReference}", style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)
                    Text("Rule Version: ${game.ruleVersion} • Confidence: ${game.confidenceLevel}", style = MaterialTheme.typography.labelSmall, color = SuccessEmerald)
                    Spacer(Modifier.height(4.dp))
                    game.ruleProfiles.forEach { prof ->
                        Text(
                            text = "• ${if (isBn) prof.nameBn else prof.nameEn}: ${if (isBn) prof.descBn else prof.descEn}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimaryLight
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualSectionCard(title: String, body: String, accent: Color = ChampagneGold) {
    LuxuryGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(color = accent, fontWeight = FontWeight.Bold))
            Text(body, style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimaryLight))
        }
    }
}

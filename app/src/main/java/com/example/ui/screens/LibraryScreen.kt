package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.core.model.GameCatalog
import com.example.core.model.GameCategory
import com.example.core.model.GameInfo
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.components.SalonChipBadge
import com.example.ui.components.VelvetSecondaryButton
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldElevated
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight

@Composable
fun LibraryScreen(
    language: AppLanguage,
    initialCategory: String?,
    defaultDifficulty: AiDifficulty,
    favoriteIds: Set<String>,
    onStartGame: (gameId: String, profileId: String, difficulty: AiDifficulty) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenManual: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val isBn = language == AppLanguage.BANGLA

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var playerFilter by remember { mutableStateOf<Int?>(null) } // null=all, 1=solo, 4=4P
    var sortAlphabetical by remember { mutableStateOf(false) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var setupDialogGame by remember { mutableStateOf<GameInfo?>(null) }

    val filteredGames = remember(query, selectedCategory, playerFilter, sortAlphabetical, favoritesOnly, favoriteIds, isBn) {
        GameCatalog.allGames
            .filter { g ->
                if (selectedCategory != null && g.category.id != selectedCategory) return@filter false
                if (favoritesOnly && !favoriteIds.contains(g.id)) return@filter false
                if (playerFilter != null && playerFilter !in g.minPlayers..g.maxPlayers) return@filter false
                if (query.isNotBlank()) {
                    val q = query.trim().lowercase()
                    g.nameEn.lowercase().contains(q) ||
                        g.nameBn.lowercase().contains(q) ||
                        g.altNames.lowercase().contains(q) ||
                        g.countryTagEn.lowercase().contains(q)
                } else true
            }
            .let { list ->
                if (sortAlphabetical) list.sortedBy { if (isBn) it.nameBn else it.nameEn } else list
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(EmeraldDeep, ObsidianNight)))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isBn) "গেম লাইব্রেরি (${filteredGames.size})" else "Game Library (${filteredGames.size})",
                style = MaterialTheme.typography.headlineLarge.copy(color = ChampagneGold)
            )
            FilterChip(
                selected = sortAlphabetical,
                onClick = { sortAlphabetical = !sortAlphabetical },
                label = { Text(if (sortAlphabetical) strings.sortAZ else strings.sortPopular) },
                leadingIcon = { Icon(Icons.Filled.SortByAlpha, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BurnishedGold,
                    selectedLabelColor = EmeraldDeep
                )
            )
        }

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(strings.searchGamesHint, color = TextSecondaryLight) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ChampagneGold) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextSecondaryLight)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BurnishedGold,
                unfocusedBorderColor = ObsidianBorder,
                focusedContainerColor = ObsidianCard,
                unfocusedContainerColor = ObsidianCard,
                focusedTextColor = TextPrimaryLight,
                unfocusedTextColor = TextPrimaryLight
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("library_search_input")
        )

        Spacer(Modifier.height(10.dp))

        // Category Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCategory == null && !favoritesOnly,
                    onClick = { selectedCategory = null; favoritesOnly = false },
                    label = { Text(strings.allCategories) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BurnishedGold, selectedLabelColor = EmeraldDeep)
                )
            }
            item {
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = !favoritesOnly; if (favoritesOnly) selectedCategory = null },
                    label = { Text(strings.favorites) },
                    leadingIcon = { Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BurnishedGold, selectedLabelColor = EmeraldDeep)
                )
            }
            items(GameCategory.entries) { cat ->
                FilterChip(
                    selected = selectedCategory == cat.id,
                    onClick = { selectedCategory = if (selectedCategory == cat.id) null else cat.id; favoritesOnly = false },
                    label = { Text(if (isBn) cat.nameBn else cat.nameEn) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BurnishedGold, selectedLabelColor = EmeraldDeep)
                )
            }
        }

        // Player-count filters
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            val counts = listOf(null to if (isBn) "সব খেলোয়াড়" else "Any Players", 1 to "1P Solo", 4 to "4P Table")
            items(counts) { (cnt, lbl) ->
                FilterChip(
                    selected = playerFilter == cnt,
                    onClick = { playerFilter = cnt },
                    label = { Text(lbl, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldElevated, selectedLabelColor = ChampagneGold)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(filteredGames, key = { it.id }) { game ->
                LibraryGameRowCard(
                    game = game,
                    isBn = isBn,
                    isFav = favoriteIds.contains(game.id),
                    onConfigurePlay = { setupDialogGame = game },
                    onToggleFav = { onToggleFavorite(game.id) },
                    onOpenRules = { onOpenManual(game.id) }
                )
            }
        }
    }

    // Pre-Match Setup Dialog (Rule Profile + AI Difficulty Selector)
    setupDialogGame?.let { game ->
        var chosenProfile by remember(game.id) { mutableStateOf(game.ruleProfiles.first().id) }
        var chosenDiff by remember(game.id) { mutableStateOf(defaultDifficulty) }

        AlertDialog(
            onDismissRequest = { setupDialogGame = null },
            containerColor = ObsidianCard,
            titleContentColor = ChampagneGold,
            title = {
                Column {
                    Text(if (isBn) game.nameBn else game.nameEn, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = if (isBn) game.origin.labelBn else game.origin.labelEn,
                        style = MaterialTheme.typography.labelSmall.copy(color = SuccessEmerald)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings.selectRuleProfile, style = MaterialTheme.typography.titleSmall.copy(color = ChampagneGold))
                    game.ruleProfiles.forEach { profile ->
                        val selected = chosenProfile == profile.id
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) EmeraldElevated else ObsidianNight)
                                .border(1.dp, if (selected) BurnishedGold else ObsidianBorder, RoundedCornerShape(12.dp))
                                .clickable { chosenProfile = profile.id }
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (isBn) profile.nameBn else profile.nameEn,
                                style = MaterialTheme.typography.labelLarge.copy(color = if (selected) ChampagneGold else TextPrimaryLight)
                            )
                            Text(
                                text = if (isBn) profile.descBn else profile.descEn,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                            )
                        }
                    }

                    if (game.recommendedPlayers > 1) {
                        Text(strings.selectDifficulty, style = MaterialTheme.typography.titleSmall.copy(color = ChampagneGold))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(AiDifficulty.entries) { diff ->
                                FilterChip(
                                    selected = chosenDiff == diff,
                                    onClick = { chosenDiff = diff },
                                    label = { Text(if (isBn) diff.nameBn else diff.nameEn) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BurnishedGold,
                                        selectedLabelColor = EmeraldDeep
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                GoldPrimaryButton(
                    text = strings.startSoloMatch,
                    icon = Icons.Filled.PlayArrow,
                    testTag = "dialog_start_match_btn",
                    onClick = {
                        setupDialogGame = null
                        onStartGame(game.id, chosenProfile, chosenDiff)
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { setupDialogGame = null }) {
                    Text(strings.cancel, color = TextSecondaryLight)
                }
            }
        )
    }
}

@Composable
private fun LibraryGameRowCard(
    game: GameInfo,
    isBn: Boolean,
    isFav: Boolean,
    onConfigurePlay: () -> Unit,
    onToggleFav: () -> Unit,
    onOpenRules: () -> Unit
) {
    val strings = LocalAppStrings.current
    LuxuryGlassCard(
        borderColor = if (game.isBangladeshClassic) BurnishedGold.copy(alpha = 0.5f) else ObsidianBorder,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConfigurePlay() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SalonChipBadge(
                        text = if (isBn) game.origin.labelBn else game.origin.labelEn,
                        accent = if (game.isBangladeshClassic) ChampagneGold else Color(0xFF8EC5FC)
                    )
                    SalonChipBadge(
                        text = "${game.recommendedPlayers}P • ${game.deckType.size}",
                        accent = SuccessEmerald
                    )
                }
                IconButton(onClick = onToggleFav, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Toggle favorite",
                        tint = if (isFav) Color(0xFFFF6B7A) else TextSecondaryLight
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isBn) "${game.nameBn} (${game.nameEn})" else game.nameEn,
                style = MaterialTheme.typography.titleLarge.copy(color = TextPrimaryLight, fontWeight = FontWeight.Bold)
            )
            Text(
                text = if (isBn) game.manual.introBn else game.manual.introEn,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight),
                maxLines = 2
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoldPrimaryButton(
                    text = strings.playNow,
                    icon = Icons.Filled.PlayArrow,
                    onClick = onConfigurePlay,
                    testTag = "lib_play_${game.id}",
                    modifier = Modifier.weight(1.2f)
                )
                VelvetSecondaryButton(
                    text = strings.rulePreview,
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    onClick = onOpenRules,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AppLanguage
import com.example.core.model.AppSettings
import com.example.core.model.Card
import com.example.core.model.GameCatalog
import com.example.core.model.Suit
import com.example.engine.GameMove
import com.example.engine.HazariTableData
import com.example.engine.SolitaireTableData
import com.example.engine.TablePhase
import com.example.engine.UniversalTableState
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.components.LuxuryTableSurface
import com.example.ui.components.PlayerSeatBadge
import com.example.ui.components.RealisticPlayingCard
import com.example.ui.components.SalonChipBadge
import com.example.ui.components.SuitVectorBadge
import com.example.ui.components.VelvetSecondaryButton
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldElevated
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalTablePalette
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.WarningAmber

@Composable
fun GameTableScreen(
    state: UniversalTableState,
    settings: AppSettings,
    isPaused: Boolean,
    onMove: (GameMove) -> Unit,
    onToggleMute: () -> Unit,
    onSetPaused: (Boolean) -> Unit,
    onExitTable: () -> Unit
) {
    val strings = LocalAppStrings.current
    val palette = LocalTablePalette.current
    val isBn = settings.language == AppLanguage.BANGLA
    val gameInfo = remember(state.gameId) { GameCatalog.byId(state.gameId) }

    var showQuitDialog by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }
    var showHintActive by remember { mutableStateOf(false) }
    var hazariSelectedIdx by remember { mutableStateOf<Int?>(null) }

    BackHandler {
        if (settings.confirmBeforeQuit) showQuitDialog = true else onExitTable()
    }

    LuxuryTableSurface(
        palette = palette,
        graphicsQuality = settings.graphicsQuality
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // 1. TOP HUD BAR
            TableTopHudBar(
                gameTitle = if (isBn) gameInfo.nameBn else gameInfo.nameEn,
                state = state,
                settings = settings,
                hintEnabled = showHintActive,
                onBack = { if (settings.confirmBeforeQuit) showQuitDialog = true else onExitTable() },
                onToggleHint = { showHintActive = !showHintActive },
                onToggleMute = onToggleMute,
                onOpenRules = { showRulesDialog = true },
                onPause = { onSetPaused(true) }
            )

            // 2. LIVE STATUS BANNER
            val banner = if (isBn) state.bannerMessageBn else state.bannerMessageEn
            if (banner.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianNight.copy(alpha = 0.72f))
                        .border(0.8.dp, BurnishedGold.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = banner,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ChampagneGold,
                            fontWeight = FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }

            // 3. MAIN GAME-SPECIFIC ARENA
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.solitaireData != null -> {
                        SolitaireTableArena(
                            data = state.solitaireData,
                            playableIds = state.playableCardIds,
                            hintCardId = if (showHintActive) state.hintCardId else null,
                            settings = settings,
                            onMove = onMove
                        )
                    }
                    state.hazariData != null -> {
                        HazariTableArena(
                            state = state,
                            data = state.hazariData,
                            settings = settings,
                            selectedIdx = hazariSelectedIdx,
                            onSelectCard = { idx ->
                                val prev = hazariSelectedIdx
                                if (prev == null) {
                                    hazariSelectedIdx = idx
                                } else if (prev == idx) {
                                    hazariSelectedIdx = null
                                } else {
                                    onMove(GameMove.HazariSwap(prev, idx))
                                    hazariSelectedIdx = null
                                }
                            },
                            onMove = {
                                hazariSelectedIdx = null
                                onMove(it)
                            }
                        )
                    }
                    state.gameId == GameCatalog.GAME_BLACKJACK -> {
                        BlackjackTableArena(state = state, settings = settings, onMove = onMove)
                    }
                    state.gameId == GameCatalog.GAME_TEEN_PATTI -> {
                        TeenPattiTableArena(state = state, settings = settings, onMove = onMove)
                    }
                    else -> {
                        FourPlayerCardArena(
                            state = state,
                            settings = settings,
                            showHint = showHintActive && settings.showHints,
                            onMove = { m ->
                                showHintActive = false
                                onMove(m)
                            }
                        )
                    }
                }
            }
        }

        // ROUND OVER / MATCH OVER MODAL OVERLAY
        if (state.phase == TablePhase.ROUND_OVER || state.phase == TablePhase.MATCH_OVER) {
            RoundSummaryOverlay(
                state = state,
                isBn = isBn,
                onNext = { onMove(GameMove.NextRound) },
                onBackLobby = onExitTable
            )
        }

        // PAUSE OVERLAY
        if (isPaused) {
            AlertDialog(
                onDismissRequest = { onSetPaused(false) },
                containerColor = ObsidianCard,
                title = { Text(strings.pauseGame, color = ChampagneGold) },
                text = { Text(if (isBn) gameInfo.nameBn else gameInfo.nameEn, color = TextPrimaryLight) },
                confirmButton = {
                    GoldPrimaryButton(text = strings.resumeGame, onClick = { onSetPaused(false) })
                },
                dismissButton = {
                    TextButton(onClick = { onSetPaused(false); onExitTable() }) {
                        Text(strings.quitAndSave, color = TextSecondaryLight)
                    }
                }
            )
        }

        // QUIT CONFIRMATION DIALOG
        if (showQuitDialog) {
            AlertDialog(
                onDismissRequest = { showQuitDialog = false },
                containerColor = ObsidianCard,
                title = { Text(strings.quitConfirmTitle, color = ChampagneGold) },
                text = { Text(strings.quitConfirmMsg, color = TextSecondaryLight) },
                confirmButton = {
                    GoldPrimaryButton(
                        text = strings.quitAndSave,
                        testTag = "confirm_quit_table",
                        onClick = { showQuitDialog = false; onExitTable() }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showQuitDialog = false }) {
                        Text(strings.cancel, color = TextSecondaryLight)
                    }
                }
            )
        }

        // IN-GAME RULEBOOK MODAL
        if (showRulesDialog) {
            AlertDialog(
                onDismissRequest = { showRulesDialog = false },
                containerColor = ObsidianCard,
                title = { Text(if (isBn) gameInfo.nameBn else gameInfo.nameEn, color = ChampagneGold) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(strings.manualObjective, style = MaterialTheme.typography.titleSmall, color = ChampagneGold)
                        Text(if (isBn) gameInfo.manual.objectiveBn else gameInfo.manual.objectiveEn, style = MaterialTheme.typography.bodySmall, color = TextPrimaryLight)
                        Text(strings.manualRules, style = MaterialTheme.typography.titleSmall, color = ChampagneGold)
                        Text(if (isBn) gameInfo.manual.rulesBn else gameInfo.manual.rulesEn, style = MaterialTheme.typography.bodySmall, color = TextPrimaryLight)
                        Text(strings.manualScoring, style = MaterialTheme.typography.titleSmall, color = ChampagneGold)
                        Text(if (isBn) gameInfo.manual.scoringBn else gameInfo.manual.scoringEn, style = MaterialTheme.typography.bodySmall, color = TextPrimaryLight)
                    }
                },
                confirmButton = {
                    GoldPrimaryButton(text = strings.resumeGame, onClick = { showRulesDialog = false })
                }
            )
        }
    }
}

@Composable
private fun TableTopHudBar(
    gameTitle: String,
    state: UniversalTableState,
    settings: AppSettings,
    hintEnabled: Boolean,
    onBack: () -> Unit,
    onToggleHint: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenRules: () -> Unit,
    onPause: () -> Unit
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianNight.copy(alpha = 0.78f))
            .border(0.8.dp, ObsidianBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp).testTag("table_back_btn")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
            }
            Column {
                Text(
                    text = gameTitle,
                    style = MaterialTheme.typography.titleSmall.copy(color = ChampagneGold, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${strings.roundLabel} ${state.roundNumber}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryLight)
                    )
                    if (state.teamScores != null) {
                        Text(
                            text = "Team ${state.teamScores.first} : ${state.teamScores.second}",
                            style = MaterialTheme.typography.labelSmall.copy(color = SuccessEmerald, fontWeight = FontWeight.Bold)
                        )
                    } else if (state.virtualPot > 0) {
                        Text(
                            text = "Pot ${state.virtualPot}",
                            style = MaterialTheme.typography.labelSmall.copy(color = ChampagneGold, fontWeight = FontWeight.Bold)
                        )
                    }
                    // Trump badge
                    if (state.trumpSuit != null || state.activeWildSuit != null) {
                        val s = state.trumpSuit ?: state.activeWildSuit
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(EmeraldElevated)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            if (state.isTrumpHidden) {
                                Text(strings.hiddenTrump, fontSize = 9.sp, color = WarningAmber, fontWeight = FontWeight.Bold)
                            } else if (s != null) {
                                SuitVectorBadge(
                                    suit = s,
                                    size = 12.dp,
                                    colorBlindSuits = settings.colorBlindSuits,
                                    tintOverride = if (!s.isRed && !settings.colorBlindSuits) Color.White else null
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (settings.showHints && state.hintCardId != null) {
                IconButton(onClick = onToggleHint, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Filled.Lightbulb,
                        contentDescription = strings.hintLabel,
                        tint = if (hintEnabled) SuccessEmerald else TextSecondaryLight,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            IconButton(onClick = onOpenRules, modifier = Modifier.size(38.dp).testTag("table_rules_btn")) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = strings.rulesShortcut, tint = ChampagneGold, modifier = Modifier.size(19.dp))
            }
            IconButton(onClick = onToggleMute, modifier = Modifier.size(38.dp)) {
                Icon(
                    if (settings.muteAll) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = "Audio",
                    tint = if (settings.muteAll) ErrorCrimson else TextPrimaryLight,
                    modifier = Modifier.size(19.dp)
                )
            }
            IconButton(onClick = onPause, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Filled.Pause, contentDescription = strings.pauseGame, tint = TextPrimaryLight, modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun FourPlayerCardArena(
    state: UniversalTableState,
    settings: AppSettings,
    showHint: Boolean,
    onMove: (GameMove) -> Unit
) {
    val strings = LocalAppStrings.current
    val isBn = settings.language == AppLanguage.BANGLA
    val cardW = if (settings.largeCards) 76.dp else 66.dp
    val cardH = if (settings.largeCards) 110.dp else 96.dp

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // NORTH SEAT
        Box(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), contentAlignment = Alignment.Center) {
            state.seats.getOrNull(2)?.let { seat ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PlayerSeatBadge(seat = seat, compact = true, reducedMotion = settings.reducedMotion)
                    if (state.teamRoundPoints != null && state.gameId == GameCatalog.GAME_TWENTY_NINE) {
                        Spacer(Modifier.height(3.dp))
                        SalonChipBadge(
                            text = "Pts: You ${state.teamRoundPoints.first} • Opp ${state.teamRoundPoints.second}" +
                                if (state.currentBid > 0) " • Bid ${state.currentBid}" else "",
                            accent = ChampagneGold
                        )
                    }
                }
            }
        }

        // MIDDLE ROW: WEST SEAT | CENTER TRICK / DISCARD ARENA | EAST SEAT
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            state.seats.getOrNull(1)?.let {
                PlayerSeatBadge(seat = it, compact = true, reducedMotion = settings.reducedMotion)
            }

            // CENTER ARENA
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.topDiscardCard != null) {
                    // Crazy Eights Draw + Discard layout
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RealisticPlayingCard(
                                card = null,
                                faceUp = false,
                                width = 62.dp,
                                height = 90.dp,
                                cardBackId = settings.cardBackId,
                                onClick = { if (state.activeSeat == 0 && state.phase == TablePhase.PLAYING) onMove(GameMove.DrawCard) },
                                testTag = "draw_pile_card"
                            )
                            Text("${state.drawPileCount}", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RealisticPlayingCard(
                                card = state.topDiscardCard,
                                faceUp = true,
                                width = 68.dp,
                                height = 98.dp,
                                colorBlindSuits = settings.colorBlindSuits
                            )
                            state.activeWildSuit?.let { s ->
                                Spacer(Modifier.height(4.dp))
                                SuitVectorBadge(suit = s, size = 16.dp, colorBlindSuits = settings.colorBlindSuits)
                            }
                        }
                    }
                } else {
                    // 4-Seat Trick Diamond Layout
                    Box(modifier = Modifier.size(185.dp), contentAlignment = Alignment.Center) {
                        state.centerCards.forEach { played ->
                            val (ox, oy, rot) = when (played.seatIndex) {
                                2 -> Triple(0.dp, (-34).dp, -3f)
                                1 -> Triple((-44).dp, 0.dp, -7f)
                                3 -> Triple(44.dp, 0.dp, 7f)
                                else -> Triple(0.dp, 34.dp, 2f)
                            }
                            RealisticPlayingCard(
                                card = played.card,
                                faceUp = played.faceUp,
                                width = 62.dp,
                                height = 90.dp,
                                colorBlindSuits = settings.colorBlindSuits,
                                cardBackId = settings.cardBackId,
                                reducedMotion = settings.reducedMotion,
                                rotationDeg = rot,
                                modifier = Modifier.offset(x = ox, y = oy)
                            )
                        }
                    }
                }
            }

            state.seats.getOrNull(3)?.let {
                PlayerSeatBadge(seat = it, compact = true, reducedMotion = settings.reducedMotion)
            }
        }

        // CONTEXTUAL ACTION BAR (Bidding / Select Trump / Double / Reveal Trump / Pair / Wild Suit)
        ContextualActionStrip(state = state, settings = settings, isBn = isBn, onMove = onMove)

        // SOUTH HUMAN SEAT + FANNED HAND
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            state.seats.getOrNull(0)?.let { me ->
                PlayerSeatBadge(seat = me, compact = true, reducedMotion = settings.reducedMotion)
            }
            Spacer(Modifier.height(8.dp))

            HumanCardFan(
                cards = state.humanHand,
                playableIds = state.playableCardIds,
                hintCardId = if (showHint) state.hintCardId else null,
                cardW = cardW,
                cardH = cardH,
                settings = settings,
                onTapCard = { card -> onMove(GameMove.PlayCard(card.id)) }
            )
        }
    }
}

@Composable
private fun ContextualActionStrip(
    state: UniversalTableState,
    settings: AppSettings,
    isBn: Boolean,
    onMove: (GameMove) -> Unit
) {
    val strings = LocalAppStrings.current
    when {
        state.phase == TablePhase.BIDDING && state.activeSeat == 0 && state.bidOptions.isNotEmpty() -> {
            LuxuryGlassCard(borderColor = BurnishedGold, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(strings.bidPhase, style = MaterialTheme.typography.labelLarge, color = ChampagneGold)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.canPassBid) {
                            item {
                                VelvetSecondaryButton(
                                    text = strings.passBid,
                                    testTag = "bid_pass_btn",
                                    onClick = { onMove(GameMove.PlaceBid(-1)) }
                                )
                            }
                        }
                        items(state.bidOptions) { bidVal ->
                            GoldPrimaryButton(
                                text = if (bidVal == 0) "NIL" else "$bidVal",
                                testTag = "bid_val_$bidVal",
                                onClick = { onMove(GameMove.PlaceBid(bidVal)) }
                            )
                        }
                    }
                }
            }
        }
        (state.phase == TablePhase.SELECT_TRUMP || state.phase == TablePhase.PICK_WILD_SUIT) && state.activeSeat == 0 -> {
            LuxuryGlassCard(borderColor = BurnishedGold, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (state.phase == TablePhase.PICK_WILD_SUIT) strings.pickWildSuit else strings.selectTrump,
                        style = MaterialTheme.typography.titleSmall,
                        color = ChampagneGold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Suit.entries.forEach { suit ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFAF6EC))
                                    .border(1.5.dp, BurnishedGold, RoundedCornerShape(12.dp))
                                    .testTag("pick_suit_${suit.code}")
                                    .clickable {
                                        if (state.phase == TablePhase.PICK_WILD_SUIT) onMove(GameMove.PickWildSuit(suit))
                                        else onMove(GameMove.SelectTrump(suit))
                                    }
                                    .padding(vertical = 10.dp)
                            ) {
                                SuitVectorBadge(suit = suit, size = 24.dp, colorBlindSuits = settings.colorBlindSuits)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = if (isBn) suit.nameBn else suit.nameEn,
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF14181F), fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
        state.phase == TablePhase.DOUBLE_DECISION && state.activeSeat == 0 -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.canCallDouble) {
                    GoldPrimaryButton(text = strings.callDouble, onClick = { onMove(GameMove.CallDouble) }, modifier = Modifier.weight(1f))
                }
                if (state.canCallRedouble) {
                    GoldPrimaryButton(text = strings.callRedouble, onClick = { onMove(GameMove.CallRedouble) }, modifier = Modifier.weight(1f))
                }
                VelvetSecondaryButton(text = strings.continuePlay, onClick = { onMove(GameMove.ContinueNormal) }, modifier = Modifier.weight(1f))
            }
        }
        state.phase == TablePhase.PLAYING && state.activeSeat == 0 && (state.canRevealTrump || state.canDeclarePair || (state.topDiscardCard != null && state.playableCardIds.isEmpty())) -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                if (state.canRevealTrump) {
                    GoldPrimaryButton(
                        text = strings.revealTrump,
                        icon = Icons.Filled.Visibility,
                        testTag = "reveal_trump_btn",
                        onClick = { onMove(GameMove.RevealTrump) }
                    )
                }
                if (state.canDeclarePair) {
                    GoldPrimaryButton(
                        text = strings.declarePair,
                        icon = Icons.Filled.AutoAwesome,
                        testTag = "declare_pair_btn",
                        onClick = { onMove(GameMove.DeclarePair) }
                    )
                }
                if (state.topDiscardCard != null && state.playableCardIds.isEmpty()) {
                    GoldPrimaryButton(
                        text = strings.drawCard,
                        testTag = "crazy8_draw_btn",
                        onClick = { onMove(GameMove.DrawCard) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HumanCardFan(
    cards: List<Card>,
    playableIds: Set<String>,
    hintCardId: String?,
    cardW: Dp,
    cardH: Dp,
    settings: AppSettings,
    onTapCard: (Card) -> Unit
) {
    if (cards.isEmpty()) return
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val availWidth = maxWidth - 12.dp
        val count = cards.size
        val step = if (count <= 1) 0.dp else ((availWidth - cardW) / (count - 1)).coerceAtMost(cardW * 0.68f).coerceAtLeast(26.dp)
        val totalW = cardW + step * (count - 1)

        Box(
            modifier = Modifier
                .width(totalW)
                .height(cardH + 18.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            cards.forEachIndexed { idx, card ->
                val isPlayable = playableIds.contains(card.id)
                val isHinted = hintCardId == card.id
                val mid = (count - 1) / 2f
                val fanRot = if (settings.reducedMotion) 0f else (idx - mid) * 1.8f
                RealisticPlayingCard(
                    card = card,
                    faceUp = true,
                    width = cardW,
                    height = cardH,
                    isPlayable = isPlayable || playableIds.isEmpty(),
                    isHinted = isHinted,
                    colorBlindSuits = settings.colorBlindSuits,
                    cardBackId = settings.cardBackId,
                    reducedMotion = settings.reducedMotion,
                    rotationDeg = fanRot,
                    testTag = "hand_card_${card.id}",
                    onClick = if (isPlayable) ({ onTapCard(card) }) else null,
                    modifier = Modifier.offset(x = step * idx)
                )
            }
        }
    }
}

@Composable
private fun HazariTableArena(
    state: UniversalTableState,
    data: HazariTableData,
    settings: AppSettings,
    selectedIdx: Int?,
    onSelectCard: (Int) -> Unit,
    onMove: (GameMove) -> Unit
) {
    val strings = LocalAppStrings.current
    val isBn = settings.language == AppLanguage.BANGLA

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Scoreboard strip for all 4 players
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.seats.forEach { seat ->
                Box(modifier = Modifier.weight(1f)) {
                    PlayerSeatBadge(seat = seat, compact = true, reducedMotion = settings.reducedMotion)
                }
            }
        }

        if (state.phase == TablePhase.GROUP_ARRANGING) {
            if (!data.isValidOrder) {
                Text(
                    text = strings.hazariGroupWarning,
                    style = MaterialTheme.typography.labelMedium.copy(color = ErrorCrimson, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // 4 Interactive Hazari Group Trays (3-3-3-4)
            data.groups.forEach { grp ->
                val baseOffset = when (grp.groupIndex) {
                    0 -> 0; 1 -> 3; 2 -> 6; else -> 9
                }
                LuxuryGlassCard(
                    borderColor = if (data.isValidOrder) BurnishedGold.copy(alpha = 0.5f) else ErrorCrimson,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Set ${grp.groupIndex + 1}: ${if (isBn) grp.rankNameBn else grp.rankNameEn}",
                                style = MaterialTheme.typography.titleSmall.copy(color = ChampagneGold)
                            )
                            SalonChipBadge(text = "${grp.points} pts", accent = SuccessEmerald)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            grp.cards.forEachIndexed { localIdx, card ->
                                val globalIdx = baseOffset + localIdx
                                RealisticPlayingCard(
                                    card = card,
                                    faceUp = true,
                                    width = 62.dp,
                                    height = 88.dp,
                                    isSelected = selectedIdx == globalIdx,
                                    colorBlindSuits = settings.colorBlindSuits,
                                    reducedMotion = settings.reducedMotion,
                                    testTag = "hazari_card_$globalIdx",
                                    onClick = { onSelectCard(globalIdx) }
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VelvetSecondaryButton(
                    text = strings.autoArrange,
                    icon = Icons.Filled.AutoAwesome,
                    testTag = "hazari_auto_arrange",
                    onClick = { onMove(GameMove.HazariAutoArrange) },
                    modifier = Modifier.weight(1f)
                )
                GoldPrimaryButton(
                    text = strings.confirmGroups,
                    icon = Icons.Filled.PlayArrow,
                    enabled = data.isValidOrder,
                    testTag = "hazari_confirm_btn",
                    onClick = { onMove(GameMove.HazariConfirmGroups) },
                    modifier = Modifier.weight(1.2f)
                )
            }
        } else {
            // Showdown Reveal Stage (all 4 seats' current Set side-by-side)
            Text(
                text = "Showdown — Set ${(data.stageIndex + 1).coerceIn(1, 4)} / 4",
                style = MaterialTheme.typography.titleMedium.copy(color = ChampagneGold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            state.seats.forEach { seat ->
                val cards = data.stageCardsBySeat[seat.seatIndex].orEmpty()
                val wonStage = data.stageWinnerSeat == seat.seatIndex
                LuxuryGlassCard(
                    borderColor = if (wonStage) SuccessEmerald else ObsidianBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.width(86.dp)) {
                            Text(seat.name, style = MaterialTheme.typography.labelLarge, color = if (wonStage) SuccessEmerald else TextPrimaryLight)
                            Text("+${seat.roundScore} pts", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            cards.forEach { c ->
                                RealisticPlayingCard(
                                    card = c,
                                    faceUp = true,
                                    width = 52.dp,
                                    height = 76.dp,
                                    colorBlindSuits = settings.colorBlindSuits
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeenPattiTableArena(
    state: UniversalTableState,
    settings: AppSettings,
    onMove: (GameMove) -> Unit
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Opponents row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            state.seats.filter { it.seatIndex != 0 }.forEach { seat ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PlayerSeatBadge(seat = seat, compact = true, reducedMotion = settings.reducedMotion)
                    if (seat.revealedCards.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            seat.revealedCards.forEach { c ->
                                RealisticPlayingCard(card = c, faceUp = true, width = 38.dp, height = 56.dp, colorBlindSuits = settings.colorBlindSuits)
                            }
                        }
                    }
                }
            }
        }

        // Virtual Chip Pot Centerpiece
        LuxuryGlassCard(borderColor = BurnishedGold, modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(strings.virtualChips, style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)
                Text("POT: ${state.virtualPot}", style = MaterialTheme.typography.headlineMedium.copy(color = ChampagneGold, fontWeight = FontWeight.Bold))
                Text("${strings.virtualChips}: ${state.virtualChips}", style = MaterialTheme.typography.labelSmall, color = SuccessEmerald)
            }
        }

        // Player's 3 cards + actions
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.humanHand.forEach { card ->
                    RealisticPlayingCard(
                        card = card,
                        faceUp = state.humanIsSeen || state.phase == TablePhase.ROUND_OVER,
                        width = 82.dp,
                        height = 118.dp,
                        cardBackId = settings.cardBackId,
                        colorBlindSuits = settings.colorBlindSuits,
                        reducedMotion = settings.reducedMotion
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            if (state.phase == TablePhase.PLAYING && state.activeSeat == 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!state.humanIsSeen) {
                        VelvetSecondaryButton(
                            text = strings.seeCards,
                            testTag = "tp_see_btn",
                            onClick = { onMove(GameMove.TeenPattiSee) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    GoldPrimaryButton(
                        text = "${if (state.humanIsSeen) strings.chaalBet else strings.blindBet} (${state.currentStake})",
                        testTag = "tp_bet_btn",
                        onClick = { onMove(GameMove.TeenPattiBet(raise = false)) },
                        modifier = Modifier.weight(1.2f)
                    )
                    if (state.canShowdown) {
                        GoldPrimaryButton(
                            text = strings.showCards,
                            onClick = { onMove(GameMove.TeenPattiShow) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    VelvetSecondaryButton(
                        text = strings.packFold,
                        onClick = { onMove(GameMove.TeenPattiPack) },
                        modifier = Modifier.weight(0.85f)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(strings.virtualChipsNote, style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)
        }
    }
}

@Composable
private fun BlackjackTableArena(
    state: UniversalTableState,
    settings: AppSettings,
    onMove: (GameMove) -> Unit
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dealer Hand
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SalonChipBadge(text = "${strings.dealer}: ${state.blackjackDealerTotal}", accent = ChampagneGold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.blackjackDealerCards.forEachIndexed { idx, card ->
                    RealisticPlayingCard(
                        card = card,
                        faceUp = !(idx == 1 && state.blackjackDealerHidden),
                        width = 76.dp,
                        height = 110.dp,
                        cardBackId = settings.cardBackId,
                        colorBlindSuits = settings.colorBlindSuits,
                        reducedMotion = settings.reducedMotion
                    )
                }
            }
        }

        // Virtual Chips Bet Banner
        SalonChipBadge(
            text = "Bet: ${state.virtualPot} • ${strings.virtualChips}: ${state.virtualChips}",
            accent = SuccessEmerald
        )

        // Player Hand + Hit / Stand / Double
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SalonChipBadge(text = "You: ${state.blackjackPlayerTotal}", accent = ChampagneGold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.humanHand.forEach { card ->
                    RealisticPlayingCard(
                        card = card,
                        faceUp = true,
                        width = 78.dp,
                        height = 112.dp,
                        colorBlindSuits = settings.colorBlindSuits,
                        reducedMotion = settings.reducedMotion
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            if (state.phase == TablePhase.PLAYING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GoldPrimaryButton(
                        text = strings.hit,
                        testTag = "bj_hit_btn",
                        onClick = { onMove(GameMove.BlackjackHit) },
                        modifier = Modifier.weight(1f)
                    )
                    VelvetSecondaryButton(
                        text = strings.stand,
                        testTag = "bj_stand_btn",
                        onClick = { onMove(GameMove.BlackjackStand) },
                        modifier = Modifier.weight(1f)
                    )
                    if (state.canBlackjackDouble) {
                        VelvetSecondaryButton(
                            text = strings.doubleDown,
                            onClick = { onMove(GameMove.BlackjackDouble) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SolitaireTableArena(
    data: SolitaireTableData,
    playableIds: Set<String>,
    hintCardId: String?,
    settings: AppSettings,
    onMove: (GameMove) -> Unit
) {
    val strings = LocalAppStrings.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val colW = ((maxWidth - 24.dp) / 7).coerceIn(44.dp, 76.dp)
        val colH = (colW.value * 1.44f).dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Controls row: Score, Moves, Undo, Auto-Finish
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SalonChipBadge(text = "${strings.score}: ${data.score} • Moves: ${data.movesCount}", accent = ChampagneGold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (data.canUndo) {
                        VelvetSecondaryButton(
                            text = strings.undoMove,
                            icon = Icons.AutoMirrored.Filled.Undo,
                            onClick = { onMove(GameMove.SolitaireUndo) }
                        )
                    }
                    if (data.canAutoFinish) {
                        GoldPrimaryButton(
                            text = strings.autoComplete,
                            icon = Icons.Filled.AutoAwesome,
                            onClick = { onMove(GameMove.SolitaireAutoFinish) }
                        )
                    }
                }
            }

            // Stock + Waste + 4 Foundations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Stock pile
                    Box(
                        modifier = Modifier
                            .size(colW, colH)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, BurnishedGold.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .testTag("solitaire_stock_pile")
                            .clickable { onMove(GameMove.SolitaireDrawStock) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (data.stockCount > 0) {
                            RealisticPlayingCard(
                                card = null, faceUp = false, width = colW, height = colH,
                                cardBackId = settings.cardBackId
                            )
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reset stock", tint = ChampagneGold)
                        }
                    }

                    // Waste top card
                    val wTop = data.waste.lastOrNull()
                    if (wTop != null) {
                        RealisticPlayingCard(
                            card = wTop,
                            faceUp = true,
                            width = colW,
                            height = colH,
                            isPlayable = playableIds.contains(wTop.id),
                            isHinted = hintCardId == wTop.id,
                            colorBlindSuits = settings.colorBlindSuits,
                            onClick = { onMove(GameMove.SolitaireTap(wTop.id)) }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(colW, colH)
                                .border(0.8.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
                        )
                    }
                }

                // 4 Foundations
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    data.foundations.forEachIndexed { idx, pile ->
                        val top = pile.lastOrNull()
                        if (top != null) {
                            RealisticPlayingCard(
                                card = top, faceUp = true, width = colW, height = colH,
                                colorBlindSuits = settings.colorBlindSuits
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(colW, colH)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(EmeraldDeep.copy(alpha = 0.45f))
                                    .border(1.dp, BurnishedGold.copy(alpha = 0.35f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", color = BurnishedGold.copy(alpha = 0.65f), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 7 Tableau Columns
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.tableau.forEachIndexed { colIdx, col ->
                    val stackH = if (col.cards.isEmpty()) colH else colH + (col.cards.size * 22).dp
                    Box(modifier = Modifier.width(colW).height(stackH)) {
                        if (col.cards.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(colW, colH)
                                    .border(0.8.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
                            )
                        } else {
                            var yOff = 0.dp
                            col.cards.forEachIndexed { cardIdx, card ->
                                val isFaceUp = cardIdx >= col.faceUpStartIndex
                                val stepY = if (isFaceUp) 22.dp else 9.dp
                                RealisticPlayingCard(
                                    card = card,
                                    faceUp = isFaceUp,
                                    width = colW,
                                    height = colH,
                                    isPlayable = !isFaceUp || playableIds.contains(card.id),
                                    isHinted = isFaceUp && hintCardId == card.id,
                                    cardBackId = settings.cardBackId,
                                    colorBlindSuits = settings.colorBlindSuits,
                                    reducedMotion = settings.reducedMotion,
                                    onClick = if (isFaceUp) ({ onMove(GameMove.SolitaireTap(card.id)) }) else null,
                                    modifier = Modifier.offset(y = yOff)
                                )
                                yOff += stepY
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundSummaryOverlay(
    state: UniversalTableState,
    isBn: Boolean,
    onNext: () -> Unit,
    onBackLobby: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isMatch = state.phase == TablePhase.MATCH_OVER
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        LuxuryGlassCard(borderColor = BurnishedGold, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = when {
                        isMatch && state.matchWinnerIsHuman == true -> strings.youWin
                        isMatch && state.matchWinnerIsHuman == false -> strings.opponentWins
                        else -> "${strings.roundLabel} ${state.roundNumber}"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = if (state.matchWinnerIsHuman == false) ErrorCrimson else ChampagneGold,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = if (isBn) state.roundResultSummaryBn.ifBlank { state.bannerMessageBn }
                    else state.roundResultSummaryEn.ifBlank { state.bannerMessageEn },
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimaryLight),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                GoldPrimaryButton(
                    text = if (isMatch) strings.playAgain else strings.nextRound,
                    icon = Icons.Filled.PlayArrow,
                    testTag = "round_over_next_btn",
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth()
                )
                VelvetSecondaryButton(
                    text = strings.backToLobby,
                    onClick = onBackLobby,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

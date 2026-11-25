package com.example.engine

import com.example.core.audio.CardSfx
import com.example.core.model.AiDifficulty
import com.example.core.model.Card
import com.example.core.model.GameCatalog
import com.example.core.model.Suit

enum class TablePhase {
    BIDDING,
    SELECT_TRUMP,
    DOUBLE_DECISION,
    GROUP_ARRANGING,
    PLAYING,
    PICK_WILD_SUIT,
    ROUND_OVER,
    MATCH_OVER
}

sealed class GameMove {
    data class PlayCard(val cardId: String) : GameMove()
    data class PlaceBid(val bid: Int) : GameMove() // -1 = Pass
    data class SelectTrump(val suit: Suit) : GameMove()
    data object RevealTrump : GameMove()
    data object DeclarePair : GameMove()
    data object CallDouble : GameMove()
    data object CallRedouble : GameMove()
    data object ContinueNormal : GameMove()
    data class HazariSwap(val idxA: Int, val idxB: Int) : GameMove()
    data object HazariAutoArrange : GameMove()
    data object HazariConfirmGroups : GameMove()
    data object BlackjackHit : GameMove()
    data object BlackjackStand : GameMove()
    data object BlackjackDouble : GameMove()
    data object TeenPattiSee : GameMove()
    data class TeenPattiBet(val raise: Boolean = false) : GameMove()
    data object TeenPattiPack : GameMove()
    data object TeenPattiShow : GameMove()
    data class PickWildSuit(val suit: Suit) : GameMove()
    data object DrawCard : GameMove()
    data class SolitaireTap(val cardId: String) : GameMove()
    data object SolitaireDrawStock : GameMove()
    data object SolitaireUndo : GameMove()
    data object SolitaireAutoFinish : GameMove()
    data object NextRound : GameMove()
}

data class SeatSnapshot(
    val seatIndex: Int, // 0 = Human (South), 1 = West, 2 = North, 3 = East
    val name: String,
    val cardCount: Int,
    val totalScore: Double = 0.0,
    val roundScore: Int = 0,
    val tricksWon: Int = 0,
    val bid: Int? = null,
    val isDealer: Boolean = false,
    val isActiveTurn: Boolean = false,
    val teamId: Int = seatIndex % 2,
    val statusBadge: String = "",
    val isEliminated: Boolean = false,
    val revealedCards: List<Card> = emptyList()
)

data class PlayedTableCard(
    val seatIndex: Int,
    val card: Card,
    val faceUp: Boolean = true
)

data class HazariGroupInfo(
    val groupIndex: Int,
    val cards: List<Card>,
    val rankNameEn: String,
    val rankNameBn: String,
    val points: Int
)

data class HazariTableData(
    val groups: List<HazariGroupInfo>,
    val isValidOrder: Boolean,
    val stageIndex: Int, // 0..3 during reveal showdown
    val stageWinnerSeat: Int? = null,
    val stageCardsBySeat: Map<Int, List<Card>> = emptyMap(),
    val targetScore: Int = 1000
)

data class SolitaireColumn(
    val cards: List<Card>,
    val faceUpStartIndex: Int
)

data class SolitaireTableData(
    val tableau: List<SolitaireColumn>,
    val foundations: List<List<Card>>, // 4 piles
    val stockCount: Int,
    val waste: List<Card>,
    val score: Int,
    val movesCount: Int,
    val canUndo: Boolean,
    val canAutoFinish: Boolean,
    val drawCount: Int = 1
)

data class RoundHistoryEntry(
    val roundNumber: Int,
    val summaryEn: String,
    val summaryBn: String
)

data class UniversalTableState(
    val gameId: String,
    val ruleProfileId: String,
    val difficulty: AiDifficulty,
    val phase: TablePhase,
    val roundNumber: Int = 1,
    val activeSeat: Int = 0,
    val dealerSeat: Int = 0,
    val seats: List<SeatSnapshot> = emptyList(),
    val humanHand: List<Card> = emptyList(),
    val playableCardIds: Set<String> = emptySet(),
    val hintCardId: String? = null,
    val centerCards: List<PlayedTableCard> = emptyList(),
    val topDiscardCard: Card? = null,
    val drawPileCount: Int = 0,
    val trumpSuit: Suit? = null,
    val isTrumpHidden: Boolean = false,
    val activeWildSuit: Suit? = null,
    val currentBid: Int = 0,
    val bidWinnerSeat: Int? = null,
    val bidOptions: List<Int> = emptyList(),
    val canPassBid: Boolean = false,
    val canRevealTrump: Boolean = false,
    val canDeclarePair: Boolean = false,
    val canCallDouble: Boolean = false,
    val canCallRedouble: Boolean = false,
    val doubleMultiplier: Int = 1,
    val teamScores: Pair<Int, Int>? = null, // (Your Team, Opponent Team)
    val teamRoundPoints: Pair<Int, Int>? = null,
    val virtualPot: Long = 0L,
    val currentStake: Long = 0L,
    val virtualChips: Long = 5000L,
    val humanIsSeen: Boolean = false,
    val canShowdown: Boolean = false,
    val blackjackDealerCards: List<Card> = emptyList(),
    val blackjackDealerHidden: Boolean = false,
    val blackjackPlayerTotal: Int = 0,
    val blackjackDealerTotal: Int = 0,
    val canBlackjackDouble: Boolean = false,
    val hazariData: HazariTableData? = null,
    val solitaireData: SolitaireTableData? = null,
    val bannerMessageEn: String = "",
    val bannerMessageBn: String = "",
    val roundResultSummaryEn: String = "",
    val roundResultSummaryBn: String = "",
    val matchWinnerIsHuman: Boolean? = null,
    val roundHistory: List<RoundHistoryEntry> = emptyList(),
    val lastSfx: CardSfx = CardSfx.CARD_PLACE
)

data class MoveResult(
    val accepted: Boolean,
    val state: UniversalTableState,
    val sfx: CardSfx = CardSfx.CARD_PLACE,
    val chipDelta: Long = 0L,
    val errorReason: String? = null
)

interface CardGameEngine {
    val gameId: String
    fun initializeGame(
        ruleProfileId: String,
        difficulty: AiDifficulty,
        playerName: String,
        virtualChips: Long = 5000L
    ): UniversalTableState

    fun getState(): UniversalTableState
    fun applyMove(move: GameMove): MoveResult
    fun computeAiMove(): GameMove?
    fun serializeState(): String
    fun restoreState(json: String): UniversalTableState?
}

object EngineFactory {
    fun create(gameId: String): CardGameEngine = when (gameId) {
        GameCatalog.GAME_TWENTY_NINE -> TwentyNineEngine()
        GameCatalog.GAME_HAZARI -> HazariEngine()
        GameCatalog.GAME_CALL_BRIDGE -> CallBridgeEngine(isCallBreak = false)
        GameCatalog.GAME_CALL_BREAK -> CallBridgeEngine(isCallBreak = true)
        GameCatalog.GAME_COURT_PIECE -> CourtPieceEngine()
        GameCatalog.GAME_TEEN_PATTI -> TeenPattiEngine()
        GameCatalog.GAME_BHABHI -> BhabhiEngine()
        GameCatalog.GAME_SPADES -> SpadesEngine()
        GameCatalog.GAME_HEARTS -> HeartsEngine()
        GameCatalog.GAME_CRAZY_EIGHTS -> CrazyEightsEngine()
        GameCatalog.GAME_BLACKJACK -> BlackjackEngine()
        GameCatalog.GAME_KLONDIKE -> KlondikeSolitaireEngine()
        else -> TwentyNineEngine()
    }
}

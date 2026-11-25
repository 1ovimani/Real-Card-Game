package com.example.engine

import com.example.core.audio.CardSfx
import com.example.core.model.AiDifficulty
import com.example.core.model.Card
import com.example.core.model.DeckFactory
import com.example.core.model.DeckType
import com.example.core.model.GameCatalog
import com.example.core.model.Rank
import com.example.core.model.Suit
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * Authentic Call Bridge & Call Break Engine (distinct rule profiles!):
 * - Call Bridge: Min bid 2, Spades always trump, strict must-beat/over-trump rule, 5 rounds.
 * - Call Break: Min bid 1, Spades always trump, strict must-beat rule, 8-call bonus (13.0 pts), 5 rounds.
 */
class CallBridgeEngine(private val isCallBreak: Boolean) : CardGameEngine {
    override val gameId: String = if (isCallBreak) GameCatalog.GAME_CALL_BREAK else GameCatalog.GAME_CALL_BRIDGE

    private var ruleProfileId = if (isCallBreak) "classic_call_break" else "bd_call_bridge_5r"
    private var difficulty = AiDifficulty.NORMAL
    private var names = arrayOf("You", "Hasan", "Nadia", "Imran")

    private var phase = TablePhase.BIDDING
    private var roundNumber = 1
    private val totalRounds = 5
    private var dealerSeat = 0
    private var activeSeat = 1

    private var hands = Array(4) { mutableListOf<Card>() }
    private var bids = IntArray(4) { -1 }
    private var tricksWon = IntArray(4)
    private var totalScores = DoubleArray(4)
    private var currentTrick = mutableListOf<PlayedTableCard>()
    private var tricksPlayed = 0
    private var history = mutableListOf<RoundHistoryEntry>()
    private var bannerEn = ""
    private var bannerBn = ""
    private var roundSummaryEn = ""
    private var roundSummaryBn = ""

    private val minBid: Int get() = if (isCallBreak) 1 else 2

    override fun initializeGame(
        ruleProfileId: String,
        difficulty: AiDifficulty,
        playerName: String,
        virtualChips: Long
    ): UniversalTableState {
        this.ruleProfileId = ruleProfileId
        this.difficulty = difficulty
        this.names = arrayOf(playerName.ifBlank { "You" }, "Hasan", "Nadia", "Imran")
        this.roundNumber = 1
        this.dealerSeat = 0
        this.totalScores = DoubleArray(4)
        this.history.clear()
        startRound()
        return getState()
    }

    private fun startRound() {
        val deck = DeckFactory.shuffledDeck(DeckType.STANDARD_52)
        for (i in 0..3) {
            hands[i] = deck.subList(i * 13, (i + 1) * 13).toMutableList()
            hands[i].sortWith(compareBy<Card> { it.suit.ordinal }.thenByDescending { it.rank.value })
        }
        bids = IntArray(4) { -1 }
        tricksWon = IntArray(4)
        currentTrick.clear()
        tricksPlayed = 0
        phase = TablePhase.BIDDING
        activeSeat = (dealerSeat + 1) % 4
        bannerEn = "Round $roundNumber/$totalRounds: Spades (♠) are Trump. Place your call ($minBid–8)."
        bannerBn = "রাউন্ড $roundNumber/$totalRounds: ইস্কাবন (♠) রং। আপনার কল দিন ($minBid–8)।"
    }

    override fun getState(): UniversalTableState {
        val playable = if (phase == TablePhase.PLAYING && activeSeat == 0) {
            legalCards(0).map { it.id }.toSet()
        } else emptySet()

        val seats = (0..3).map { i ->
            SeatSnapshot(
                seatIndex = i,
                name = names[i],
                cardCount = hands[i].size,
                totalScore = ((totalScores[i] * 10.0).roundToInt() / 10.0),
                tricksWon = tricksWon[i],
                bid = bids[i].takeIf { it > 0 },
                isDealer = dealerSeat == i,
                isActiveTurn = activeSeat == i && phase != TablePhase.ROUND_OVER && phase != TablePhase.MATCH_OVER,
                statusBadge = if (bids[i] > 0) "${tricksWon[i]}/${bids[i]}" else ""
            )
        }

        val winnerHuman = if (phase == TablePhase.MATCH_OVER) {
            totalScores[0] >= (totalScores.maxOrNull() ?: 0.0)
        } else null

        return UniversalTableState(
            gameId = gameId,
            ruleProfileId = ruleProfileId,
            difficulty = difficulty,
            phase = phase,
            roundNumber = roundNumber,
            activeSeat = activeSeat,
            dealerSeat = dealerSeat,
            seats = seats,
            humanHand = hands[0].toList(),
            playableCardIds = playable,
            hintCardId = if (phase == TablePhase.PLAYING && activeSeat == 0) chooseAiCard(0)?.id else null,
            centerCards = currentTrick.toList(),
            trumpSuit = Suit.SPADES,
            isTrumpHidden = false,
            bidOptions = if (phase == TablePhase.BIDDING && activeSeat == 0) (minBid..8).toList() else emptyList(),
            canPassBid = false,
            bannerMessageEn = bannerEn,
            bannerMessageBn = bannerBn,
            roundResultSummaryEn = roundSummaryEn,
            roundResultSummaryBn = roundSummaryBn,
            matchWinnerIsHuman = winnerHuman,
            roundHistory = history.toList()
        )
    }

    /** Authentic Call Bridge / Call Break strict must-beat & over-trump rule */
    fun legalCards(seat: Int): List<Card> {
        val hand = hands[seat]
        if (hand.isEmpty() || currentTrick.isEmpty()) return hand
        val ledSuit = currentTrick.first().card.suit
        val trickWinnerCard = winningCardOf(currentTrick)
        val sameSuit = hand.filter { it.suit == ledSuit }

        if (sameSuit.isNotEmpty()) {
            // If current winner is still of ledSuit, player MUST beat it if able!
            if (trickWinnerCard.suit == ledSuit) {
                val higher = sameSuit.filter { it.rank.value > trickWinnerCard.rank.value }
                if (higher.isNotEmpty()) return higher
            }
            return sameSuit
        }

        // Void in led suit: MUST play a winning Spade if able!
        val spades = hand.filter { it.suit == Suit.SPADES }
        if (spades.isNotEmpty()) {
            val beatingSpades = if (trickWinnerCard.suit == Suit.SPADES) {
                spades.filter { it.rank.value > trickWinnerCard.rank.value }
            } else {
                spades
            }
            if (beatingSpades.isNotEmpty()) return beatingSpades
        }
        return hand
    }

    private fun winningCardOf(trick: List<PlayedTableCard>): Card {
        val ledSuit = trick.first().card.suit
        var best = trick.first().card
        for (p in trick.drop(1)) {
            val c = p.card
            val beats = when {
                c.suit == Suit.SPADES && best.suit != Suit.SPADES -> true
                c.suit != Suit.SPADES && best.suit == Suit.SPADES -> false
                c.suit == best.suit -> c.rank.value > best.rank.value
                else -> false
            }
            if (beats) best = c
        }
        return best
    }

    override fun applyMove(move: GameMove): MoveResult {
        return when (move) {
            is GameMove.PlaceBid -> {
                if (phase != TablePhase.BIDDING || move.bid !in minBid..8) return MoveResult(false, getState())
                bids[activeSeat] = move.bid
                bannerEn = "${names[activeSeat]} called ${move.bid}."
                bannerBn = "${names[activeSeat]} ${move.bid} কল দিয়েছে।"
                if (bids.all { it > 0 }) {
                    phase = TablePhase.PLAYING
                    activeSeat = (dealerSeat + 1) % 4
                    bannerEn = "Calls locked! ${names[activeSeat]} leads."
                    bannerBn = "কল সম্পন্ন! ${names[activeSeat]} প্রথম চাল দিচ্ছে।"
                } else {
                    activeSeat = (activeSeat + 1) % 4
                }
                MoveResult(true, getState(), CardSfx.BID_ANNOUNCE)
            }
            is GameMove.PlayCard -> {
                if (phase != TablePhase.PLAYING) return MoveResult(false, getState())
                val card = legalCards(activeSeat).firstOrNull { it.id == move.cardId }
                    ?: return MoveResult(false, getState(), errorReason = "Must follow suit & beat if able")
                hands[activeSeat].remove(card)
                currentTrick.add(PlayedTableCard(activeSeat, card))
                if (currentTrick.size < 4) {
                    activeSeat = (activeSeat + 1) % 4
                    return MoveResult(true, getState(), CardSfx.CARD_PLACE)
                }
                val winCard = winningCardOf(currentTrick)
                val winner = currentTrick.first { it.card.id == winCard.id }.seatIndex
                tricksWon[winner]++
                tricksPlayed++
                currentTrick.clear()
                bannerEn = "${names[winner]} takes trick $tricksPlayed (${tricksWon[winner]}/${bids[winner]})."
                bannerBn = "${names[winner]} $tricksPlayed নং হাত জিতেছে (${tricksWon[winner]}/${bids[winner]})।"
                if (tricksPlayed >= 13) {
                    scoreRound()
                    val sfx = if (phase == TablePhase.MATCH_OVER) {
                        if (totalScores[0] >= (totalScores.maxOrNull() ?: 0.0)) CardSfx.MATCH_WIN else CardSfx.MATCH_LOSS
                    } else CardSfx.ROUND_WIN
                    return MoveResult(true, getState(), sfx)
                }
                activeSeat = winner
                MoveResult(true, getState(), CardSfx.TRICK_WIN)
            }
            is GameMove.NextRound -> {
                if (phase == TablePhase.MATCH_OVER) {
                    totalScores = DoubleArray(4)
                    roundNumber = 1
                    history.clear()
                } else {
                    roundNumber++
                    dealerSeat = (dealerSeat + 1) % 4
                }
                startRound()
                MoveResult(true, getState(), CardSfx.CARD_SHUFFLE)
            }
            else -> MoveResult(false, getState())
        }
    }

    private fun scoreRound() {
        for (i in 0..3) {
            val bid = bids[i]
            val won = tricksWon[i]
            val delta = if (won >= bid) {
                if (isCallBreak && bid >= 8) 13.0 else bid + (won - bid) * 0.1
            } else {
                -bid.toDouble()
            }
            totalScores[i] = ((totalScores[i] + delta) * 10.0).roundToInt() / 10.0
        }
        val sbEn = (0..3).joinToString(" | ") { "${names[it]}: ${totalScores[it]}" }
        roundSummaryEn = "Round $roundNumber complete — $sbEn"
        roundSummaryBn = "রাউন্ড $roundNumber সমাপ্ত — $sbEn"
        history.add(RoundHistoryEntry(roundNumber, roundSummaryEn, roundSummaryBn))
        phase = if (roundNumber >= totalRounds) TablePhase.MATCH_OVER else TablePhase.ROUND_OVER
    }

    override fun computeAiMove(): GameMove? {
        if (activeSeat == 0) return null
        return when (phase) {
            TablePhase.BIDDING -> {
                val h = hands[activeSeat]
                val est = h.count { it.rank == Rank.ACE } +
                    h.count { it.rank == Rank.KING } * 0.65 +
                    (h.count { it.suit == Suit.SPADES } - 2).coerceAtLeast(0) * 0.75
                val bid = est.roundToInt().coerceIn(minBid, 7)
                GameMove.PlaceBid(bid)
            }
            TablePhase.PLAYING -> chooseAiCard(activeSeat)?.let { GameMove.PlayCard(it.id) }
            else -> null
        }
    }

    private fun chooseAiCard(seat: Int): Card? {
        val legal = legalCards(seat)
        if (legal.isEmpty()) return null
        if (legal.size == 1) return legal.first()
        if (difficulty == AiDifficulty.BEGINNER) return legal.random()

        val needMore = tricksWon[seat] < bids[seat]
        if (currentTrick.isEmpty()) {
            return if (needMore) {
                legal.firstOrNull { it.suit != Suit.SPADES && it.rank == Rank.ACE }
                    ?: legal.maxByOrNull { it.rank.value }
            } else {
                legal.minByOrNull { it.rank.value + if (it.suit == Suit.SPADES) 20 else 0 }
            }
        }
        val winners = legal.filter { c ->
            winningCardOf(currentTrick + PlayedTableCard(seat, c)).id == c.id
        }
        return if (needMore && winners.isNotEmpty()) {
            winners.minByOrNull { it.rank.value + if (it.suit == Suit.SPADES) 20 else 0 }
        } else {
            legal.minByOrNull { it.rank.value + if (it.suit == Suit.SPADES) 20 else 0 }
        }
    }

    override fun serializeState(): String {
        val r = JSONObject()
        r.put("phase", phase.name)
        r.put("round", roundNumber)
        r.put("dealer", dealerSeat)
        r.put("active", activeSeat)
        r.put("tricksPlayed", tricksPlayed)
        r.put("bids", JSONArray(bids.toList()))
        r.put("tricks", JSONArray(tricksWon.toList()))
        r.put("scores", JSONArray(totalScores.toList()))
        r.put("hands", JSONArray(hands.map { h -> JSONArray(h.map { it.id }) }))
        r.put("trick", JSONArray(currentTrick.map { JSONObject().put("s", it.seatIndex).put("c", it.card.id) }))
        return r.toString()
    }

    override fun restoreState(json: String): UniversalTableState? {
        return try {
            val r = JSONObject(json)
            phase = TablePhase.valueOf(r.getString("phase"))
            roundNumber = r.getInt("round")
            dealerSeat = r.getInt("dealer")
            activeSeat = r.getInt("active")
            tricksPlayed = r.getInt("tricksPlayed")
            val bA = r.getJSONArray("bids")
            val tA = r.getJSONArray("tricks")
            val sA = r.getJSONArray("scores")
            val hA = r.getJSONArray("hands")
            for (i in 0..3) {
                bids[i] = bA.getInt(i)
                tricksWon[i] = tA.getInt(i)
                totalScores[i] = sA.getDouble(i)
                hands[i] = mutableListOf()
                val sub = hA.getJSONArray(i)
                for (j in 0 until sub.length()) Card.fromId(sub.getString(j))?.let { hands[i].add(it) }
            }
            currentTrick.clear()
            val trA = r.getJSONArray("trick")
            for (i in 0 until trA.length()) {
                val o = trA.getJSONObject(i)
                Card.fromId(o.getString("c"))?.let { currentTrick.add(PlayedTableCard(o.getInt("s"), it)) }
            }
            getState()
        } catch (_: Throwable) { null }
    }
}

/**
 * Authentic South Asian Court Piece / Rung (কোর্ট পিস / রং) Engine.
 * - 4 Players, 2 Teams (0 & 2 vs 1 & 3), 52-card deck
 * - Authentic 5-4-4 deal: 5 cards dealt -> Trump selector picks Rung -> remaining 8 cards dealt
 * - First team to 7 tricks wins hand; first 7 consecutive tricks = COURT (2 match pts)! Match to 5.
 */
class CourtPieceEngine : CardGameEngine {
    override val gameId: String = GameCatalog.GAME_COURT_PIECE

    private var ruleProfileId = "single_sir"
    private var difficulty = AiDifficulty.NORMAL
    private var names = arrayOf("You", "Zayed", "Arman (Partner)", "Farhan")

    private var phase = TablePhase.SELECT_TRUMP
    private var roundNumber = 1
    private var dealerSeat = 0
    private var trumpCallerSeat = 0
    private var activeSeat = 0
    private var trumpSuit: Suit? = null

    private var hands = Array(4) { mutableListOf<Card>() }
    private var reserve = Array(4) { mutableListOf<Card>() }
    private var currentTrick = mutableListOf<PlayedTableCard>()
    private var teamTricks = intArrayOf(0, 0)
    private var teamMatchScore = intArrayOf(0, 0)
    private var history = mutableListOf<RoundHistoryEntry>()
    private var bannerEn = ""
    private var bannerBn = ""
    private var roundSummaryEn = ""
    private var roundSummaryBn = ""

    override fun initializeGame(
        ruleProfileId: String,
        difficulty: AiDifficulty,
        playerName: String,
        virtualChips: Long
    ): UniversalTableState {
        this.ruleProfileId = ruleProfileId
        this.difficulty = difficulty
        this.names = arrayOf(playerName.ifBlank { "You" }, "Zayed", "Arman (Partner)", "Farhan")
        this.roundNumber = 1
        this.dealerSeat = 3
        this.teamMatchScore = intArrayOf(0, 0)
        this.history.clear()
        startRound()
        return getState()
    }

    private fun startRound() {
        val deck = DeckFactory.shuffledDeck(DeckType.STANDARD_52)
        for (i in 0..3) {
            hands[i] = deck.subList(i * 5, i * 5 + 5).toMutableList()
            reserve[i] = deck.subList(20 + i * 8, 20 + i * 8 + 8).toMutableList()
            hands[i].sortWith(compareBy<Card> { it.suit.ordinal }.thenByDescending { it.rank.value })
        }
        trumpCallerSeat = (dealerSeat + 1) % 4
        activeSeat = trumpCallerSeat
        trumpSuit = null
        currentTrick.clear()
        teamTricks = intArrayOf(0, 0)
        phase = TablePhase.SELECT_TRUMP
        bannerEn = "5 cards dealt! ${names[trumpCallerSeat]} selects Rung (Trump Suit)."
        bannerBn = "প্রথম ৫টি তাস দেওয়া হয়েছে! ${names[trumpCallerSeat]} রং নির্বাচন করছে।"
    }

    override fun getState(): UniversalTableState {
        val playable = if (phase == TablePhase.PLAYING && activeSeat == 0) legalCards(0).map { it.id }.toSet() else emptySet()
        val seats = (0..3).map { i ->
            val t = i % 2
            SeatSnapshot(
                seatIndex = i,
                name = names[i],
                cardCount = hands[i].size,
                totalScore = teamMatchScore[t].toDouble(),
                tricksWon = teamTricks[t],
                isDealer = dealerSeat == i,
                isActiveTurn = activeSeat == i && phase != TablePhase.ROUND_OVER && phase != TablePhase.MATCH_OVER,
                teamId = t
            )
        }
        return UniversalTableState(
            gameId = gameId,
            ruleProfileId = ruleProfileId,
            difficulty = difficulty,
            phase = phase,
            roundNumber = roundNumber,
            activeSeat = activeSeat,
            dealerSeat = dealerSeat,
            seats = seats,
            humanHand = hands[0].toList(),
            playableCardIds = playable,
            hintCardId = if (phase == TablePhase.PLAYING && activeSeat == 0) pickAiCard(0)?.id else null,
            centerCards = currentTrick.toList(),
            trumpSuit = trumpSuit,
            isTrumpHidden = false,
            teamScores = teamMatchScore[0] to teamMatchScore[1],
            teamRoundPoints = teamTricks[0] to teamTricks[1],
            bannerMessageEn = bannerEn,
            bannerMessageBn = bannerBn,
            roundResultSummaryEn = roundSummaryEn,
            roundResultSummaryBn = roundSummaryBn,
            matchWinnerIsHuman = if (phase == TablePhase.MATCH_OVER) teamMatchScore[0] >= 5 else null,
            roundHistory = history.toList()
        )
    }

    private fun legalCards(seat: Int): List<Card> {
        val h = hands[seat]
        if (currentTrick.isEmpty()) return h
        val led = currentTrick.first().card.suit
        val same = h.filter { it.suit == led }
        return if (same.isNotEmpty()) same else h
    }

    private fun trickWinner(trick: List<PlayedTableCard>): Int {
        val led = trick.first().card.suit
        val t = trumpSuit
        var best = trick.first()
        for (p in trick.drop(1)) {
            val c = p.card
            val b = best.card
            val beats = when {
                t != null && c.suit == t && b.suit != t -> true
                t != null && c.suit != t && b.suit == t -> false
                c.suit == b.suit -> c.rank.value > b.rank.value
                else -> false
            }
            if (beats) best = p
        }
        return best.seatIndex
    }

    override fun applyMove(move: GameMove): MoveResult {
        return when (move) {
            is GameMove.SelectTrump -> {
                if (phase != TablePhase.SELECT_TRUMP) return MoveResult(false, getState())
                trumpSuit = move.suit
                for (i in 0..3) {
                    hands[i].addAll(reserve[i])
                    reserve[i].clear()
                    hands[i].sortWith(compareBy<Card> { it.suit.ordinal }.thenByDescending { it.rank.value })
                }
                phase = TablePhase.PLAYING
                activeSeat = trumpCallerSeat
                bannerEn = "Rung is ${move.suit.nameEn} ${move.suit.symbol}! Remaining 8 cards dealt. First to 7 tricks wins."
                bannerBn = "রং হলো ${move.suit.nameBn} ${move.suit.symbol}! বাকি ৮ তাস দেওয়া হয়েছে। ৭ হাতে জয়।"
                MoveResult(true, getState(), CardSfx.TRUMP_REVEAL)
            }
            is GameMove.PlayCard -> {
                if (phase != TablePhase.PLAYING) return MoveResult(false, getState())
                val card = legalCards(activeSeat).firstOrNull { it.id == move.cardId } ?: return MoveResult(false, getState())
                hands[activeSeat].remove(card)
                currentTrick.add(PlayedTableCard(activeSeat, card))
                if (currentTrick.size < 4) {
                    activeSeat = (activeSeat + 1) % 4
                    return MoveResult(true, getState(), CardSfx.CARD_PLACE)
                }
                val winner = trickWinner(currentTrick)
                val winTeam = winner % 2
                teamTricks[winTeam]++
                currentTrick.clear()
                bannerEn = "${names[winner]} won the trick! Score: ${teamTricks[0]}–${teamTricks[1]}"
                bannerBn = "${names[winner]} হাত জিতেছে! স্কোর: ${teamTricks[0]}–${teamTricks[1]}"

                if (teamTricks[0] >= 7 || teamTricks[1] >= 7) {
                    val winningTeam = if (teamTricks[0] >= 7) 0 else 1
                    val isCourt = teamTricks[1 - winningTeam] == 0
                    val pts = if (isCourt) 2 else 1
                    teamMatchScore[winningTeam] += pts
                    val teamStrEn = if (winningTeam == 0) "Your Team" else "Opponents"
                    val teamStrBn = if (winningTeam == 0) "আপনার দল" else "প্রতিপক্ষ"
                    val courtNoteEn = if (isCourt) " — COURT SWEEP (+2)!" else " (+1)"
                    val courtNoteBn = if (isCourt) " — কোর্ট সুইপ (+২)!" else " (+১)"
                    roundSummaryEn = "$teamStrEn won ${teamTricks[winningTeam]}–${teamTricks[1 - winningTeam]}$courtNoteEn"
                    roundSummaryBn = "$teamStrBn জিতেছে ${teamTricks[winningTeam]}–${teamTricks[1 - winningTeam]}$courtNoteBn"
                    history.add(RoundHistoryEntry(roundNumber, roundSummaryEn, roundSummaryBn))
                    phase = if (teamMatchScore[0] >= 5 || teamMatchScore[1] >= 5) TablePhase.MATCH_OVER else TablePhase.ROUND_OVER
                    return MoveResult(true, getState(), if (winningTeam == 0) CardSfx.ROUND_WIN else CardSfx.TRICK_WIN)
                }
                activeSeat = winner
                MoveResult(true, getState(), CardSfx.TRICK_WIN)
            }
            is GameMove.NextRound -> {
                if (phase == TablePhase.MATCH_OVER) {
                    teamMatchScore = intArrayOf(0, 0)
                    roundNumber = 1
                    history.clear()
                } else {
                    roundNumber++
                    dealerSeat = (dealerSeat + 1) % 4
                }
                startRound()
                MoveResult(true, getState(), CardSfx.CARD_SHUFFLE)
            }
            else -> MoveResult(false, getState())
        }
    }

    override fun computeAiMove(): GameMove? {
        if (activeSeat == 0) return null
        return when (phase) {
            TablePhase.SELECT_TRUMP -> {
                val best = Suit.entries.maxByOrNull { s ->
                    hands[activeSeat].filter { it.suit == s }.sumOf { it.rank.value }
                } ?: Suit.SPADES
                GameMove.SelectTrump(best)
            }
            TablePhase.PLAYING -> pickAiCard(activeSeat)?.let { GameMove.PlayCard(it.id) }
            else -> null
        }
    }

    private fun pickAiCard(seat: Int): Card? {
        val legal = legalCards(seat)
        if (legal.isEmpty()) return null
        if (currentTrick.isEmpty()) return legal.maxByOrNull { it.rank.value }
        val partner = (seat + 2) % 4
        if (trickWinner(currentTrick) == partner) return legal.minByOrNull { it.rank.value }
        val winners = legal.filter { trickWinner(currentTrick + PlayedTableCard(seat, it)) == seat }
        return winners.minByOrNull { it.rank.value + if (it.suit == trumpSuit) 20 else 0 }
            ?: legal.minByOrNull { it.rank.value + if (it.suit == trumpSuit) 20 else 0 }
    }

    override fun serializeState(): String {
        val r = JSONObject()
        r.put("phase", phase.name)
        r.put("round", roundNumber)
        r.put("dealer", dealerSeat)
        r.put("caller", trumpCallerSeat)
        r.put("active", activeSeat)
        r.put("trump", trumpSuit?.code ?: "")
        r.put("t0", teamTricks[0]); r.put("t1", teamTricks[1])
        r.put("m0", teamMatchScore[0]); r.put("m1", teamMatchScore[1])
        r.put("hands", JSONArray(hands.map { h -> JSONArray(h.map { it.id }) }))
        r.put("reserve", JSONArray(reserve.map { h -> JSONArray(h.map { it.id }) }))
        return r.toString()
    }

    override fun restoreState(json: String): UniversalTableState? {
        return try {
            val r = JSONObject(json)
            phase = TablePhase.valueOf(r.getString("phase"))
            roundNumber = r.getInt("round")
            dealerSeat = r.getInt("dealer")
            trumpCallerSeat = r.getInt("caller")
            activeSeat = r.getInt("active")
            trumpSuit = r.optString("trump").takeIf { it.isNotEmpty() }?.let { Suit.fromCode(it) }
            teamTricks = intArrayOf(r.getInt("t0"), r.getInt("t1"))
            teamMatchScore = intArrayOf(r.getInt("m0"), r.getInt("m1"))
            val hA = r.getJSONArray("hands")
            val rA = r.getJSONArray("reserve")
            for (i in 0..3) {
                hands[i] = mutableListOf()
                val sub = hA.getJSONArray(i)
                for (j in 0 until sub.length()) Card.fromId(sub.getString(j))?.let { hands[i].add(it) }
                reserve[i] = mutableListOf()
                val subR = rA.getJSONArray(i)
                for (j in 0 until subR.length()) Card.fromId(subR.getString(j))?.let { reserve[i].add(it) }
            }
            currentTrick.clear()
            getState()
        } catch (_: Throwable) { null }
    }
}

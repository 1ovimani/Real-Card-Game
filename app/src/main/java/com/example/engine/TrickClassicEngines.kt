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
 * Authentic Partnership Spades Engine:
 * - 4 Players (2 Teams: 0+2 vs 1+3), 52 cards, 13 each
 * - Bids 0 (Nil) to 13, Spades broken rule, +100/-100 Nil, 10 bags = -100, target 500
 */
class SpadesEngine : CardGameEngine {
    override val gameId: String = GameCatalog.GAME_SPADES
    private var ruleProfileId = "standard_500"
    private var difficulty = AiDifficulty.NORMAL
    private var names = arrayOf("You", "Elena", "Marcus (Partner)", "Viktor")

    private var phase = TablePhase.BIDDING
    private var roundNumber = 1
    private var dealerSeat = 0
    private var activeSeat = 1
    private var hands = Array(4) { mutableListOf<Card>() }
    private var bids = IntArray(4) { -1 }
    private var tricksWon = IntArray(4)
    private var teamScores = intArrayOf(0, 0)
    private var teamBags = intArrayOf(0, 0)
    private var spadesBroken = false
    private var currentTrick = mutableListOf<PlayedTableCard>()
    private var tricksPlayed = 0
    private var history = mutableListOf<RoundHistoryEntry>()
    private var bannerEn = ""
    private var bannerBn = ""
    private var roundSummaryEn = ""
    private var roundSummaryBn = ""

    override fun initializeGame(ruleProfileId: String, difficulty: AiDifficulty, playerName: String, virtualChips: Long): UniversalTableState {
        this.ruleProfileId = ruleProfileId
        this.difficulty = difficulty
        this.names = arrayOf(playerName.ifBlank { "You" }, "Elena", "Marcus (Partner)", "Viktor")
        roundNumber = 1; dealerSeat = 0; teamScores = intArrayOf(0, 0); teamBags = intArrayOf(0, 0); history.clear()
        startRound()
        return getState()
    }

    private fun startRound() {
        val deck = DeckFactory.shuffledDeck(DeckType.STANDARD_52)
        for (i in 0..3) {
            hands[i] = deck.subList(i * 13, (i + 1) * 13).toMutableList()
            hands[i].sortWith(compareBy<Card> { it.suit.ordinal }.thenByDescending { it.rank.value })
        }
        bids = IntArray(4) { -1 }; tricksWon = IntArray(4); spadesBroken = false
        currentTrick.clear(); tricksPlayed = 0; phase = TablePhase.BIDDING
        activeSeat = (dealerSeat + 1) % 4
        bannerEn = "Round $roundNumber: Bid 0 (Nil) to 13 tricks. First team to 500 wins."
        bannerBn = "রাউন্ড $roundNumber: ০ (নিল) থেকে ১৩ পর্যন্ত ডাক দিন। ৫০০ পয়েন্টে জয়।"
    }

    override fun getState(): UniversalTableState {
        val playable = if (phase == TablePhase.PLAYING && activeSeat == 0) legalCards(0).map { it.id }.toSet() else emptySet()
        val seats = (0..3).map { i ->
            SeatSnapshot(
                seatIndex = i, name = names[i], cardCount = hands[i].size,
                totalScore = teamScores[i % 2].toDouble(), tricksWon = tricksWon[i],
                bid = bids[i].takeIf { it >= 0 }, isDealer = dealerSeat == i,
                isActiveTurn = activeSeat == i && phase != TablePhase.ROUND_OVER && phase != TablePhase.MATCH_OVER,
                teamId = i % 2, statusBadge = if (bids[i] == 0) "NIL" else if (bids[i] > 0) "${tricksWon[i]}/${bids[i]}" else ""
            )
        }
        return UniversalTableState(
            gameId = gameId, ruleProfileId = ruleProfileId, difficulty = difficulty, phase = phase,
            roundNumber = roundNumber, activeSeat = activeSeat, dealerSeat = dealerSeat, seats = seats,
            humanHand = hands[0].toList(), playableCardIds = playable,
            hintCardId = if (phase == TablePhase.PLAYING && activeSeat == 0) pickAiCard(0)?.id else null,
            centerCards = currentTrick.toList(), trumpSuit = Suit.SPADES, isTrumpHidden = false,
            bidOptions = if (phase == TablePhase.BIDDING && activeSeat == 0) (0..10).toList() else emptyList(),
            teamScores = teamScores[0] to teamScores[1],
            teamRoundPoints = (tricksWon[0] + tricksWon[2]) to (tricksWon[1] + tricksWon[3]),
            bannerMessageEn = bannerEn, bannerMessageBn = bannerBn,
            roundResultSummaryEn = roundSummaryEn, roundResultSummaryBn = roundSummaryBn,
            matchWinnerIsHuman = if (phase == TablePhase.MATCH_OVER) teamScores[0] >= teamScores[1] else null,
            roundHistory = history.toList()
        )
    }

    private fun legalCards(seat: Int): List<Card> {
        val h = hands[seat]
        if (currentTrick.isEmpty()) {
            if (!spadesBroken) {
                val nonSpades = h.filter { it.suit != Suit.SPADES }
                if (nonSpades.isNotEmpty()) return nonSpades
            }
            return h
        }
        val led = currentTrick.first().card.suit
        val same = h.filter { it.suit == led }
        return if (same.isNotEmpty()) same else h
    }

    private fun trickWinner(trick: List<PlayedTableCard>): Int {
        var best = trick.first()
        for (p in trick.drop(1)) {
            val c = p.card; val b = best.card
            val beats = (c.suit == Suit.SPADES && b.suit != Suit.SPADES) ||
                (c.suit == b.suit && c.rank.value > b.rank.value)
            if (beats) best = p
        }
        return best.seatIndex
    }

    override fun applyMove(move: GameMove): MoveResult = when (move) {
        is GameMove.PlaceBid -> {
            if (phase != TablePhase.BIDDING || move.bid !in 0..13) MoveResult(false, getState())
            else {
                bids[activeSeat] = move.bid
                bannerEn = "${names[activeSeat]} bid ${if (move.bid == 0) "NIL" else move.bid}."
                bannerBn = "${names[activeSeat]} ${if (move.bid == 0) "নিল" else move.bid} ডাক দিয়েছে।"
                if (bids.all { it >= 0 }) {
                    phase = TablePhase.PLAYING
                    activeSeat = (dealerSeat + 1) % 4
                } else activeSeat = (activeSeat + 1) % 4
                MoveResult(true, getState(), CardSfx.BID_ANNOUNCE)
            }
        }
        is GameMove.PlayCard -> {
            val card = legalCards(activeSeat).firstOrNull { it.id == move.cardId } ?: return MoveResult(false, getState())
            hands[activeSeat].remove(card)
            if (card.suit == Suit.SPADES) spadesBroken = true
            currentTrick.add(PlayedTableCard(activeSeat, card))
            if (currentTrick.size < 4) {
                activeSeat = (activeSeat + 1) % 4
                MoveResult(true, getState(), CardSfx.CARD_PLACE)
            } else {
                val winner = trickWinner(currentTrick)
                tricksWon[winner]++
                tricksPlayed++
                currentTrick.clear()
                bannerEn = "${names[winner]} takes trick $tricksPlayed."
                bannerBn = "${names[winner]} $tricksPlayed নং হাত জিতেছে।"
                if (tricksPlayed >= 13) {
                    scoreSpadesRound()
                    MoveResult(true, getState(), if (phase == TablePhase.MATCH_OVER && teamScores[0] >= teamScores[1]) CardSfx.MATCH_WIN else CardSfx.ROUND_WIN)
                } else {
                    activeSeat = winner
                    MoveResult(true, getState(), CardSfx.TRICK_WIN)
                }
            }
        }
        is GameMove.NextRound -> {
            if (phase == TablePhase.MATCH_OVER) { teamScores = intArrayOf(0, 0); teamBags = intArrayOf(0, 0); roundNumber = 1; history.clear() }
            else { roundNumber++; dealerSeat = (dealerSeat + 1) % 4 }
            startRound(); MoveResult(true, getState(), CardSfx.CARD_SHUFFLE)
        }
        else -> MoveResult(false, getState())
    }

    private fun scoreSpadesRound() {
        for (team in 0..1) {
            val sA = team; val sB = team + 2
            var delta = 0
            // Nil bonuses
            for (s in listOf(sA, sB)) {
                if (bids[s] == 0) delta += if (tricksWon[s] == 0) 100 else -100
            }
            val teamBid = (if (bids[sA] > 0) bids[sA] else 0) + (if (bids[sB] > 0) bids[sB] else 0)
            val teamWon = (if (bids[sA] > 0) tricksWon[sA] else 0) + (if (bids[sB] > 0) tricksWon[sB] else 0)
            if (teamBid > 0) {
                if (teamWon >= teamBid) {
                    val bags = teamWon - teamBid
                    delta += teamBid * 10 + bags
                    teamBags[team] += bags
                    if (teamBags[team] >= 10) {
                        teamBags[team] -= 10
                        delta -= 100
                    }
                } else {
                    delta -= teamBid * 10
                }
            }
            teamScores[team] += delta
        }
        roundSummaryEn = "Team Score: You ${teamScores[0]} (${teamBags[0]} bags) vs Opp ${teamScores[1]} (${teamBags[1]} bags)"
        roundSummaryBn = "দলগত স্কোর: আপনি ${teamScores[0]} বনাম প্রতিপক্ষ ${teamScores[1]}"
        history.add(RoundHistoryEntry(roundNumber, roundSummaryEn, roundSummaryBn))
        phase = if (teamScores[0] >= 500 || teamScores[1] >= 500) TablePhase.MATCH_OVER else TablePhase.ROUND_OVER
    }

    override fun computeAiMove(): GameMove? {
        if (activeSeat == 0) return null
        return when (phase) {
            TablePhase.BIDDING -> {
                val h = hands[activeSeat]
                val est = h.count { it.rank == Rank.ACE } + h.count { it.rank == Rank.KING } * 0.6 +
                    h.count { it.suit == Suit.SPADES && it.rank.value >= 11 } * 0.7
                GameMove.PlaceBid(est.roundToInt().coerceIn(1, 7))
            }
            TablePhase.PLAYING -> pickAiCard(activeSeat)?.let { GameMove.PlayCard(it.id) }
            else -> null
        }
    }

    private fun pickAiCard(seat: Int): Card? {
        val legal = legalCards(seat)
        if (legal.isEmpty()) return null
        if (bids[seat] == 0) return legal.minByOrNull { it.rank.value }
        if (currentTrick.isEmpty()) return legal.maxByOrNull { it.rank.value }
        val partner = (seat + 2) % 4
        if (trickWinner(currentTrick) == partner) return legal.minByOrNull { it.rank.value }
        val win = legal.filter { trickWinner(currentTrick + PlayedTableCard(seat, it)) == seat }
        return win.minByOrNull { it.rank.value + if (it.suit == Suit.SPADES) 20 else 0 }
            ?: legal.minByOrNull { it.rank.value + if (it.suit == Suit.SPADES) 20 else 0 }
    }

    override fun serializeState(): String = JSONObject().apply {
        put("phase", phase.name); put("round", roundNumber); put("active", activeSeat)
        put("t0", teamScores[0]); put("t1", teamScores[1]); put("b0", teamBags[0]); put("b1", teamBags[1])
        put("spadesBroken", spadesBroken); put("tricksPlayed", tricksPlayed)
        put("bids", JSONArray(bids.toList())); put("won", JSONArray(tricksWon.toList()))
        put("hands", JSONArray(hands.map { h -> JSONArray(h.map { it.id }) }))
    }.toString()

    override fun restoreState(json: String): UniversalTableState? = try {
        val r = JSONObject(json)
        phase = TablePhase.valueOf(r.getString("phase"))
        roundNumber = r.getInt("round"); activeSeat = r.getInt("active")
        teamScores = intArrayOf(r.getInt("t0"), r.getInt("t1"))
        teamBags = intArrayOf(r.getInt("b0"), r.getInt("b1"))
        spadesBroken = r.getBoolean("spadesBroken"); tricksPlayed = r.getInt("tricksPlayed")
        val bA = r.getJSONArray("bids"); val wA = r.getJSONArray("won"); val hA = r.getJSONArray("hands")
        for (i in 0..3) {
            bids[i] = bA.getInt(i); tricksWon[i] = wA.getInt(i); hands[i] = mutableListOf()
            val sub = hA.getJSONArray(i)
            for (j in 0 until sub.length()) Card.fromId(sub.getString(j))?.let { hands[i].add(it) }
        }
        currentTrick.clear(); getState()
    } catch (_: Throwable) { null }
}

/**
 * Authentic Hearts (Black Lady) Engine:
 * - 2♣ leads Trick 1, no penalty cards on Trick 1, Hearts breaking rule
 * - Hearts = 1 pt, Q♠ = 13 pts, Shoot the Moon (26 -> 0 for shooter, +26 others), lowest score at 100 wins!
 */
class HeartsEngine : CardGameEngine {
    override val gameId: String = GameCatalog.GAME_HEARTS
    private var ruleProfileId = "classic_hearts"
    private var difficulty = AiDifficulty.NORMAL
    private var names = arrayOf("You", "Clara", "Arthur", "Maya")

    private var phase = TablePhase.PLAYING
    private var roundNumber = 1
    private var activeSeat = 0
    private var hands = Array(4) { mutableListOf<Card>() }
    private var roundPts = IntArray(4)
    private var totalPts = IntArray(4)
    private var heartsBroken = false
    private var currentTrick = mutableListOf<PlayedTableCard>()
    private var tricksPlayed = 0
    private var history = mutableListOf<RoundHistoryEntry>()
    private var bannerEn = ""
    private var bannerBn = ""
    private var roundSummaryEn = ""
    private var roundSummaryBn = ""

    override fun initializeGame(ruleProfileId: String, difficulty: AiDifficulty, playerName: String, virtualChips: Long): UniversalTableState {
        this.ruleProfileId = ruleProfileId; this.difficulty = difficulty
        this.names = arrayOf(playerName.ifBlank { "You" }, "Clara", "Arthur", "Maya")
        roundNumber = 1; totalPts = IntArray(4); history.clear()
        startRound(); return getState()
    }

    private fun startRound() {
        val deck = DeckFactory.shuffledDeck(DeckType.STANDARD_52)
        for (i in 0..3) {
            hands[i] = deck.subList(i * 13, (i + 1) * 13).toMutableList()
            hands[i].sortWith(compareBy<Card> { it.suit.ordinal }.thenBy { it.rank.value })
        }
        roundPts = IntArray(4); heartsBroken = false; currentTrick.clear(); tricksPlayed = 0
        phase = TablePhase.PLAYING
        activeSeat = (0..3).firstOrNull { s -> hands[s].any { it.suit == Suit.CLUBS && it.rank == Rank.TWO } } ?: 0
        bannerEn = "Round $roundNumber: 2♣ leads! Avoid Hearts (♥) & Queen of Spades (Q♠)."
        bannerBn = "রাউন্ড $roundNumber: 2♣ দিয়ে শুরু! হরতন ও ইস্কাবনের বিবি (Q♠) এড়িয়ে চলুন।"
    }

    override fun getState(): UniversalTableState {
        val playable = if (phase == TablePhase.PLAYING && activeSeat == 0) legalCards(0).map { it.id }.toSet() else emptySet()
        val seats = (0..3).map { i ->
            SeatSnapshot(
                seatIndex = i, name = names[i], cardCount = hands[i].size,
                totalScore = totalPts[i].toDouble(), roundScore = roundPts[i],
                isActiveTurn = activeSeat == i && phase == TablePhase.PLAYING,
                statusBadge = "+${roundPts[i]} pts"
            )
        }
        return UniversalTableState(
            gameId = gameId, ruleProfileId = ruleProfileId, difficulty = difficulty, phase = phase,
            roundNumber = roundNumber, activeSeat = activeSeat, seats = seats,
            humanHand = hands[0].toList(), playableCardIds = playable,
            hintCardId = if (phase == TablePhase.PLAYING && activeSeat == 0) pickAiCard(0)?.id else null,
            centerCards = currentTrick.toList(),
            bannerMessageEn = bannerEn, bannerMessageBn = bannerBn,
            roundResultSummaryEn = roundSummaryEn, roundResultSummaryBn = roundSummaryBn,
            matchWinnerIsHuman = if (phase == TablePhase.MATCH_OVER) totalPts[0] <= (totalPts.minOrNull() ?: 0) else null,
            roundHistory = history.toList()
        )
    }

    private fun isPenaltyCard(c: Card) = c.suit == Suit.HEARTS || (c.suit == Suit.SPADES && c.rank == Rank.QUEEN)

    private fun legalCards(seat: Int): List<Card> {
        val h = hands[seat]
        if (tricksPlayed == 0 && currentTrick.isEmpty()) {
            val twoClubs = h.filter { it.suit == Suit.CLUBS && it.rank == Rank.TWO }
            if (twoClubs.isNotEmpty()) return twoClubs
        }
        if (currentTrick.isEmpty()) {
            if (!heartsBroken) {
                val nonH = h.filter { it.suit != Suit.HEARTS }
                if (nonH.isNotEmpty()) return nonH
            }
            return h
        }
        val led = currentTrick.first().card.suit
        val same = h.filter { it.suit == led }
        if (same.isNotEmpty()) return same
        if (tricksPlayed == 0) {
            val safe = h.filter { !isPenaltyCard(it) }
            if (safe.isNotEmpty()) return safe
        }
        return h
    }

    override fun applyMove(move: GameMove): MoveResult = when (move) {
        is GameMove.PlayCard -> {
            val card = legalCards(activeSeat).firstOrNull { it.id == move.cardId } ?: return MoveResult(false, getState())
            hands[activeSeat].remove(card)
            if (card.suit == Suit.HEARTS) heartsBroken = true
            currentTrick.add(PlayedTableCard(activeSeat, card))
            if (currentTrick.size < 4) {
                activeSeat = (activeSeat + 1) % 4
                MoveResult(true, getState(), CardSfx.CARD_PLACE)
            } else {
                val led = currentTrick.first().card.suit
                val winner = currentTrick.filter { it.card.suit == led }.maxBy { it.card.rank.value }.seatIndex
                val pts = currentTrick.sumOf { c ->
                    when {
                        c.card.suit == Suit.HEARTS -> 1
                        c.card.suit == Suit.SPADES && c.card.rank == Rank.QUEEN -> 13
                        else -> 0
                    }
                }
                roundPts[winner] += pts
                tricksPlayed++
                currentTrick.clear()
                bannerEn = "${names[winner]} took the trick${if (pts > 0) " (+$pts penalty)" else ""}."
                bannerBn = "${names[winner]} হাত নিয়েছে${if (pts > 0) " (+$pts পেনাল্টি)" else ""}।"
                if (tricksPlayed >= 13) {
                    val moonShooter = (0..3).firstOrNull { roundPts[it] == 26 }
                    if (moonShooter != null) {
                        for (i in 0..3) if (i != moonShooter) totalPts[i] += 26
                        roundSummaryEn = "${names[moonShooter]} SHOT THE MOON! Everyone else +26!"
                        roundSummaryBn = "${names[moonShooter]} শ্যুট দ্য মুন করেছে! অন্য সবার +২৬!"
                    } else {
                        for (i in 0..3) totalPts[i] += roundPts[i]
                        roundSummaryEn = (0..3).joinToString(" | ") { "${names[it]}: ${totalPts[it]}" }
                        roundSummaryBn = roundSummaryEn
                    }
                    history.add(RoundHistoryEntry(roundNumber, roundSummaryEn, roundSummaryBn))
                    phase = if (totalPts.any { it >= 100 }) TablePhase.MATCH_OVER else TablePhase.ROUND_OVER
                    MoveResult(true, getState(), CardSfx.ROUND_WIN)
                } else {
                    activeSeat = winner
                    MoveResult(true, getState(), CardSfx.TRICK_WIN)
                }
            }
        }
        is GameMove.NextRound -> {
            if (phase == TablePhase.MATCH_OVER) { totalPts = IntArray(4); roundNumber = 1; history.clear() }
            else roundNumber++
            startRound(); MoveResult(true, getState(), CardSfx.CARD_SHUFFLE)
        }
        else -> MoveResult(false, getState())
    }

    override fun computeAiMove(): GameMove? =
        if (activeSeat == 0 || phase != TablePhase.PLAYING) null else pickAiCard(activeSeat)?.let { GameMove.PlayCard(it.id) }

    private fun pickAiCard(seat: Int): Card? {
        val legal = legalCards(seat)
        if (legal.isEmpty()) return null
        if (currentTrick.isEmpty()) return legal.minByOrNull { it.rank.value + if (it.suit == Suit.HEARTS) 15 else 0 }
        val led = currentTrick.first().card.suit
        if (legal.first().suit != led) {
            // Void! Dump Q♠ or highest Heart immediately
            return legal.firstOrNull { it.suit == Suit.SPADES && it.rank == Rank.QUEEN }
                ?: legal.filter { it.suit == Suit.HEARTS }.maxByOrNull { it.rank.value }
                ?: legal.maxByOrNull { it.rank.value }
        }
        val highestLed = currentTrick.filter { it.card.suit == led }.maxOf { it.card.rank.value }
        val duck = legal.filter { it.rank.value < highestLed }.maxByOrNull { it.rank.value }
        return duck ?: legal.minByOrNull { it.rank.value }
    }

    override fun serializeState(): String = JSONObject().apply {
        put("phase", phase.name); put("round", roundNumber); put("active", activeSeat)
        put("heartsBroken", heartsBroken); put("tricksPlayed", tricksPlayed)
        put("rPts", JSONArray(roundPts.toList())); put("tPts", JSONArray(totalPts.toList()))
        put("hands", JSONArray(hands.map { h -> JSONArray(h.map { it.id }) }))
    }.toString()

    override fun restoreState(json: String): UniversalTableState? = try {
        val r = JSONObject(json)
        phase = TablePhase.valueOf(r.getString("phase"))
        roundNumber = r.getInt("round"); activeSeat = r.getInt("active")
        heartsBroken = r.getBoolean("heartsBroken"); tricksPlayed = r.getInt("tricksPlayed")
        val rp = r.getJSONArray("rPts"); val tp = r.getJSONArray("tPts"); val hA = r.getJSONArray("hands")
        for (i in 0..3) {
            roundPts[i] = rp.getInt(i); totalPts[i] = tp.getInt(i); hands[i] = mutableListOf()
            val sub = hA.getJSONArray(i)
            for (j in 0 until sub.length()) Card.fromId(sub.getString(j))?.let { hands[i].add(it) }
        }
        currentTrick.clear(); getState()
    } catch (_: Throwable) { null }
}

/**
 * Authentic South Asian Bhabhi / Getaway (ভাবী / গেটঅ্যাওয়ে) Engine!
 * - Ace of Spades opens; follow led suit.
 * - Throwing an off-suit card ("Thulla") immediately ends the trick & forces the player who played
 *   the highest card of the led suit to PICK UP all cards on the table!
 * - Empty your hand to Get Away; last player holding cards is the Bhabhi!
 */
class BhabhiEngine : CardGameEngine {
    override val gameId: String = GameCatalog.GAME_BHABHI
    private var ruleProfileId = "classic_bhabhi"
    private var difficulty = AiDifficulty.NORMAL
    private var names = arrayOf("You", "Tanim", "Sumi", "Rony")

    private var phase = TablePhase.PLAYING
    private var activeSeat = 0
    private var hands = Array(4) { mutableListOf<Card>() }
    private var gotAway = BooleanArray(4)
    private var currentTrick = mutableListOf<PlayedTableCard>()
    private var isFirstTrick = true
    private var bannerEn = ""
    private var bannerBn = ""
    private var summaryEn = ""
    private var summaryBn = ""

    override fun initializeGame(ruleProfileId: String, difficulty: AiDifficulty, playerName: String, virtualChips: Long): UniversalTableState {
        this.ruleProfileId = ruleProfileId; this.difficulty = difficulty
        this.names = arrayOf(playerName.ifBlank { "You" }, "Tanim", "Sumi", "Rony")
        startGame(); return getState()
    }

    private fun startGame() {
        val deck = DeckFactory.shuffledDeck(DeckType.STANDARD_52)
        for (i in 0..3) {
            hands[i] = deck.subList(i * 13, (i + 1) * 13).toMutableList()
            sortHand(hands[i])
        }
        gotAway = BooleanArray(4); currentTrick.clear(); isFirstTrick = true; phase = TablePhase.PLAYING
        activeSeat = (0..3).firstOrNull { s -> hands[s].any { it.suit == Suit.SPADES && it.rank == Rank.ACE } } ?: 0
        bannerEn = "${names[activeSeat]} holds Ace of Spades (A♠) and opens! Empty your hand to Get Away!"
        bannerBn = "${names[activeSeat]}-এর হাতে A♠! প্রথম চাল দিন — সবার আগে তাস শেষ করুন!"
    }

    private fun sortHand(h: MutableList<Card>) {
        h.sortWith(compareBy<Card> { it.suit.ordinal }.thenByDescending { it.rank.value })
    }

    override fun getState(): UniversalTableState {
        val playable = if (phase == TablePhase.PLAYING && activeSeat == 0) legalCards(0).map { it.id }.toSet() else emptySet()
        val seats = (0..3).map { i ->
            SeatSnapshot(
                seatIndex = i, name = names[i], cardCount = hands[i].size,
                isActiveTurn = activeSeat == i && phase == TablePhase.PLAYING,
                isEliminated = gotAway[i],
                statusBadge = if (gotAway[i]) "SAFE ✓" else "${hands[i].size} cards"
            )
        }
        return UniversalTableState(
            gameId = gameId, ruleProfileId = ruleProfileId, difficulty = difficulty, phase = phase,
            activeSeat = activeSeat, seats = seats, humanHand = hands[0].toList(),
            playableCardIds = playable,
            hintCardId = if (phase == TablePhase.PLAYING && activeSeat == 0) pickAiCard(0)?.id else null,
            centerCards = currentTrick.toList(),
            bannerMessageEn = bannerEn, bannerMessageBn = bannerBn,
            roundResultSummaryEn = summaryEn, roundResultSummaryBn = summaryBn,
            matchWinnerIsHuman = if (phase == TablePhase.MATCH_OVER) gotAway[0] else null
        )
    }

    private fun legalCards(seat: Int): List<Card> {
        val h = hands[seat]
        if (isFirstTrick && currentTrick.isEmpty()) {
            val aceSpades = h.filter { it.suit == Suit.SPADES && it.rank == Rank.ACE }
            if (aceSpades.isNotEmpty()) return aceSpades
        }
        if (currentTrick.isEmpty()) return h
        val led = currentTrick.first().card.suit
        val same = h.filter { it.suit == led }
        return if (same.isNotEmpty()) same else h
    }

    private fun activeSeatsRemaining(): List<Int> = (0..3).filter { !gotAway[it] && hands[it].isNotEmpty() }

    private fun nextActiveSeat(from: Int): Int {
        var cur = (from + 1) % 4
        repeat(4) {
            if (!gotAway[cur] && hands[cur].isNotEmpty()) return cur
            cur = (cur + 1) % 4
        }
        return from
    }

    override fun applyMove(move: GameMove): MoveResult = when (move) {
        is GameMove.PlayCard -> {
            if (phase != TablePhase.PLAYING) MoveResult(false, getState())
            else {
                val card = legalCards(activeSeat).firstOrNull { it.id == move.cardId } ?: return MoveResult(false, getState())
                val ledSuit = currentTrick.firstOrNull()?.card?.suit ?: card.suit
                hands[activeSeat].remove(card)
                currentTrick.add(PlayedTableCard(activeSeat, card))

                val isThulla = currentTrick.size > 1 && card.suit != ledSuit
                val activePlayersCount = (0..3).count { !gotAway[it] }

                if (isThulla && !isFirstTrick) {
                    // THULLA! Highest card of led suit picks up EVERY card in currentTrick!
                    val victim = currentTrick.filter { it.card.suit == ledSuit }.maxBy { it.card.rank.value }.seatIndex
                    val pile = currentTrick.map { it.card }
                    hands[victim].addAll(pile)
                    sortHand(hands[victim])
                    gotAway[victim] = false
                    bannerEn = "THULLA! ${names[activeSeat]} dropped off-suit — ${names[victim]} picks up ${pile.size} cards!"
                    bannerBn = "থুল্লা! ${names[activeSeat]} ভিন্ন রং দিয়েছে — ${names[victim]} ${pile.size}টি তাস তুলে নিলো!"
                    currentTrick.clear()
                    updateGotAwayStatus()
                    if (checkBhabhiOver()) return MoveResult(true, getState(), if (gotAway[0]) CardSfx.MATCH_WIN else CardSfx.MATCH_LOSS)
                    activeSeat = victim
                    MoveResult(true, getState(), CardSfx.TRUMP_REVEAL)
                } else if (currentTrick.size >= activePlayersCount) {
                    // Clean trick discarded!
                    val highestLed = currentTrick.filter { it.card.suit == ledSuit }.maxBy { it.card.rank.value }.seatIndex
                    isFirstTrick = false
                    currentTrick.clear()
                    updateGotAwayStatus()
                    if (checkBhabhiOver()) return MoveResult(true, getState(), if (gotAway[0]) CardSfx.MATCH_WIN else CardSfx.MATCH_LOSS)
                    activeSeat = if (!gotAway[highestLed] && hands[highestLed].isNotEmpty()) highestLed else nextActiveSeat(highestLed)
                    bannerEn = "Trick cleared! ${names[activeSeat]} leads next."
                    bannerBn = "হাত বাতিল হয়েছে! ${names[activeSeat]} পরবর্তী চাল দিচ্ছে।"
                    MoveResult(true, getState(), CardSfx.CARD_COLLECT)
                } else {
                    activeSeat = nextActiveSeat(activeSeat)
                    MoveResult(true, getState(), CardSfx.CARD_PLACE)
                }
            }
        }
        is GameMove.NextRound -> { startGame(); MoveResult(true, getState(), CardSfx.CARD_SHUFFLE) }
        else -> MoveResult(false, getState())
    }

    private fun updateGotAwayStatus() {
        for (i in 0..3) {
            if (hands[i].isEmpty()) gotAway[i] = true
        }
    }

    private fun checkBhabhiOver(): Boolean {
        val left = activeSeatsRemaining()
        if (left.size <= 1) {
            val loser = left.firstOrNull() ?: 0
            phase = TablePhase.MATCH_OVER
            summaryEn = if (gotAway[0]) "You Got Away safely! ${names[loser]} is the Bhabhi!" else "You were left holding cards — Bhabhi!"
            summaryBn = if (gotAway[0]) "আপনি নিরাপদে গেটঅ্যাওয়ে করেছেন! ${names[loser]} হয়েছে ভাবী!" else "আপনার হাতে তাস রয়ে গেছে — ভাবী!"
            return true
        }
        return false
    }

    override fun computeAiMove(): GameMove? =
        if (activeSeat == 0 || phase != TablePhase.PLAYING) null else pickAiCard(activeSeat)?.let { GameMove.PlayCard(it.id) }

    private fun pickAiCard(seat: Int): Card? {
        val legal = legalCards(seat)
        if (legal.isEmpty()) return null
        if (currentTrick.isEmpty()) return legal.minByOrNull { it.rank.value }
        val led = currentTrick.first().card.suit
        if (legal.first().suit != led) {
            // Throw highest card as a lethal Thulla!
            return legal.maxByOrNull { it.rank.value }
        }
        // Shed highest safe card if everyone likely follows, else play low
        return if (isFirstTrick) legal.maxByOrNull { it.rank.value } else legal.minByOrNull { it.rank.value }
    }

    override fun serializeState(): String = JSONObject().apply {
        put("phase", phase.name); put("active", activeSeat); put("first", isFirstTrick)
        put("gotAway", JSONArray(gotAway.toList()))
        put("hands", JSONArray(hands.map { h -> JSONArray(h.map { it.id }) }))
    }.toString()

    override fun restoreState(json: String): UniversalTableState? = try {
        val r = JSONObject(json)
        phase = TablePhase.valueOf(r.getString("phase")); activeSeat = r.getInt("active")
        isFirstTrick = r.getBoolean("first")
        val gA = r.getJSONArray("gotAway"); val hA = r.getJSONArray("hands")
        for (i in 0..3) {
            gotAway[i] = gA.getBoolean(i); hands[i] = mutableListOf()
            val sub = hA.getJSONArray(i)
            for (j in 0 until sub.length()) Card.fromId(sub.getString(j))?.let { hands[i].add(it) }
        }
        currentTrick.clear(); getState()
    } catch (_: Throwable) { null }
}

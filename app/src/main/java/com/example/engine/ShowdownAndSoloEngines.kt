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

/**
 * Authentic Teen Patti Engine (Entertainment Virtual Chips Only):
 * - 4 Players, 3 cards each, Blind vs Seen betting, Chaal, Pack, Showdown
 * - Uses authentic 3-card evaluator (Trail > Pure Sequence > Sequence > Color > Pair > High Card)
 */
class TeenPattiEngine : CardGameEngine {
    override val gameId: String = GameCatalog.GAME_TEEN_PATTI
    private var ruleProfileId = "classic_virtual"
    private var difficulty = AiDifficulty.NORMAL
    private var names = arrayOf("You", "Arif", "Priya", "Kabir")

    private var phase = TablePhase.PLAYING
    private var activeSeat = 0
    private var hands = Array(4) { mutableListOf<Card>() }
    private var packed = BooleanArray(4)
    private var seen = BooleanArray(4)
    private var pot = 0L
    private var currentBlindStake = 20L
    private var playerChips = 5000L
    private var roundCount = 1
    private var turnsTaken = 0
    private var bannerEn = ""
    private var bannerBn = ""
    private var summaryEn = ""
    private var summaryBn = ""
    private var winnerIsHuman: Boolean? = null

    override fun initializeGame(ruleProfileId: String, difficulty: AiDifficulty, playerName: String, virtualChips: Long): UniversalTableState {
        this.ruleProfileId = ruleProfileId; this.difficulty = difficulty
        this.names = arrayOf(playerName.ifBlank { "You" }, "Arif", "Priya", "Kabir")
        this.playerChips = virtualChips.coerceAtLeast(200L)
        this.roundCount = 1
        dealHand()
        return getState()
    }

    private fun dealHand() {
        val deck = DeckFactory.shuffledDeck(DeckType.STANDARD_52)
        for (i in 0..3) hands[i] = deck.subList(i * 3, i * 3 + 3).toMutableList()
        packed = BooleanArray(4)
        seen = BooleanArray(4)
        currentBlindStake = 20L
        pot = 80L // 20 boot each
        playerChips = (playerChips - 20L).coerceAtLeast(0L)
        activeSeat = 0
        turnsTaken = 0
        phase = TablePhase.PLAYING
        winnerIsHuman = null
        summaryEn = ""; summaryBn = ""
        bannerEn = "Boot placed! Play Blind (20) or tap See Cards to view your hand."
        bannerBn = "বুট জমা হয়েছে! ব্লাইন্ড (২০) খেলুন অথবা 'তাস দেখুন' চাপুন।"
    }

    private fun activePlayers(): List<Int> = (0..3).filter { !packed[it] }

    override fun getState(): UniversalTableState {
        val revealAll = phase == TablePhase.ROUND_OVER
        val seats = (0..3).map { i ->
            SeatSnapshot(
                seatIndex = i, name = names[i], cardCount = if (packed[i]) 0 else 3,
                isActiveTurn = activeSeat == i && phase == TablePhase.PLAYING,
                isEliminated = packed[i],
                statusBadge = when {
                    packed[i] -> "PACKED"
                    seen[i] -> "SEEN"
                    else -> "BLIND"
                },
                revealedCards = if (revealAll && !packed[i]) hands[i].toList() else emptyList()
            )
        }
        val myCost = if (seen[0]) currentBlindStake * 2 else currentBlindStake
        return UniversalTableState(
            gameId = gameId, ruleProfileId = ruleProfileId, difficulty = difficulty, phase = phase,
            roundNumber = roundCount, activeSeat = activeSeat, seats = seats,
            humanHand = hands[0].toList(),
            virtualPot = pot, currentStake = myCost, virtualChips = playerChips,
            humanIsSeen = seen[0],
            canShowdown = phase == TablePhase.PLAYING && activeSeat == 0 && activePlayers().size == 2 && seen[0],
            bannerMessageEn = bannerEn, bannerMessageBn = bannerBn,
            roundResultSummaryEn = summaryEn, roundResultSummaryBn = summaryBn,
            matchWinnerIsHuman = winnerIsHuman
        )
    }

    override fun applyMove(move: GameMove): MoveResult = when (move) {
        is GameMove.TeenPattiSee -> {
            seen[0] = true
            val ev = HazariEngine.evaluate3CardHand(hands[0])
            bannerEn = "Your Hand: ${ev.nameEn}! Chaal is now ${currentBlindStake * 2}."
            bannerBn = "আপনার তাস: ${ev.nameBn}! এখন চাল ${currentBlindStake * 2}।"
            MoveResult(true, getState(), CardSfx.CARD_FLIP)
        }
        is GameMove.TeenPattiBet -> {
            val seat = activeSeat
            val mult = if (seen[seat]) 2L else 1L
            if (move.raise && currentBlindStake < 160L) currentBlindStake *= 2L
            val bet = currentBlindStake * mult
            pot += bet
            var delta = 0L
            if (seat == 0) {
                playerChips = (playerChips - bet).coerceAtLeast(0L)
                delta = -bet
            }
            turnsTaken++
            bannerEn = "${names[seat]} played ${if (seen[seat]) "Chaal" else "Blind"} ($bet)."
            bannerBn = "${names[seat]} ${if (seen[seat]) "চাল" else "ব্লাইন্ড"} দিয়েছে ($bet)।"
            if (turnsTaken >= 12 || pot >= 1000L) {
                resolveShowdown()
            } else {
                advanceTurn()
                MoveResult(true, getState(), CardSfx.CARD_PLACE, chipDelta = delta)
            }
        }
        is GameMove.TeenPattiPack -> {
            packed[activeSeat] = true
            bannerEn = "${names[activeSeat]} packed."
            bannerBn = "${names[activeSeat]} প্যাক করেছে।"
            val rem = activePlayers()
            if (rem.size == 1) {
                finishWinner(rem.first())
            } else {
                advanceTurn()
                MoveResult(true, getState(), CardSfx.CARD_COLLECT)
            }
        }
        is GameMove.TeenPattiShow -> resolveShowdown()
        is GameMove.NextRound -> {
            roundCount++; dealHand()
            MoveResult(true, getState(), CardSfx.CARD_SHUFFLE)
        }
        else -> MoveResult(false, getState())
    }

    private fun advanceTurn() {
        var next = (activeSeat + 1) % 4
        while (packed[next] && next != activeSeat) next = (next + 1) % 4
        activeSeat = next
    }

    private fun resolveShowdown(): MoveResult {
        val rem = activePlayers()
        val winner = rem.maxWith(Comparator { a, b ->
            HazariEngine.evaluate3CardHand(hands[a]).compareTo(HazariEngine.evaluate3CardHand(hands[b]))
        })
        return finishWinner(winner)
    }

    private fun finishWinner(winner: Int): MoveResult {
        phase = TablePhase.ROUND_OVER
        winnerIsHuman = winner == 0
        val winEval = HazariEngine.evaluate3CardHand(hands[winner])
        val delta = if (winner == 0) pot else 0L
        if (winner == 0) playerChips += pot
        summaryEn = "${names[winner]} wins $pot virtual chips with ${winEval.nameEn}!"
        summaryBn = "${names[winner]} ${winEval.nameBn} দিয়ে $pot ভার্চুয়াল চিপস জিতেছে!"
        bannerEn = summaryEn; bannerBn = summaryBn
        return MoveResult(true, getState(), if (winner == 0) CardSfx.MATCH_WIN else CardSfx.MATCH_LOSS, chipDelta = delta)
    }

    override fun computeAiMove(): GameMove? {
        if (activeSeat == 0 || phase != TablePhase.PLAYING) return null
        val seat = activeSeat
        val ev = HazariEngine.evaluate3CardHand(hands[seat])
        seen[seat] = true
        val active = activePlayers()
        if (active.size == 2 && turnsTaken >= 5) return GameMove.TeenPattiShow
        return when {
            ev.category >= 4 -> GameMove.TeenPattiBet(raise = currentBlindStake < 80L)
            ev.category >= 2 -> GameMove.TeenPattiBet(raise = false)
            ev.tieBreakers.firstOrNull().let { (it ?: 0) >= 12 } && turnsTaken < 6 -> GameMove.TeenPattiBet(false)
            else -> GameMove.TeenPattiPack
        }
    }

    override fun serializeState(): String = JSONObject().apply {
        put("pot", pot); put("stake", currentBlindStake); put("chips", playerChips); put("round", roundCount)
    }.toString()

    override fun restoreState(json: String): UniversalTableState? = try {
        val r = JSONObject(json)
        playerChips = r.optLong("chips", 5000L); roundCount = r.optInt("round", 1)
        dealHand(); getState()
    } catch (_: Throwable) { null }
}

/**
 * Authentic Crazy Eights Engine:
 * - 4 Players, 5 cards each, match top discard suit/rank OR play any 8 (Wild Suit Selector)
 */
class CrazyEightsEngine : CardGameEngine {
    override val gameId: String = GameCatalog.GAME_CRAZY_EIGHTS
    private var ruleProfileId = "classic_crazy_8"
    private var difficulty = AiDifficulty.NORMAL
    private var names = arrayOf("You", "Leo", "Zara", "Noah")

    private var phase = TablePhase.PLAYING
    private var activeSeat = 0
    private var hands = Array(4) { mutableListOf<Card>() }
    private var drawPile = mutableListOf<Card>()
    private var discardPile = mutableListOf<Card>()
    private var activeSuit = Suit.HEARTS
    private var bannerEn = ""
    private var bannerBn = ""
    private var summaryEn = ""
    private var summaryBn = ""

    override fun initializeGame(ruleProfileId: String, difficulty: AiDifficulty, playerName: String, virtualChips: Long): UniversalTableState {
        this.ruleProfileId = ruleProfileId; this.difficulty = difficulty
        this.names = arrayOf(playerName.ifBlank { "You" }, "Leo", "Zara", "Noah")
        startRound(); return getState()
    }

    private fun startRound() {
        val deck = DeckFactory.shuffledDeck(DeckType.STANDARD_52).toMutableList()
        for (i in 0..3) {
            hands[i] = deck.take(5).toMutableList()
            repeat(5) { deck.removeAt(0) }
        }
        discardPile.clear()
        val starter = deck.removeAt(0)
        discardPile.add(starter)
        activeSuit = starter.suit
        drawPile = deck
        activeSeat = 0
        phase = TablePhase.PLAYING
        bannerEn = "Match ${activeSuit.nameEn} ${activeSuit.symbol} or rank ${starter.rank.label}, or play an 8!"
        bannerBn = "${activeSuit.nameBn} ${activeSuit.symbol} বা ${starter.rank.label} মিলিয়ে খেলুন, অথবা ৮ (ওয়াইল্ড) দিন!"
    }

    private fun legalCards(seat: Int): List<Card> {
        val top = discardPile.lastOrNull() ?: return hands[seat]
        return hands[seat].filter { it.rank == Rank.EIGHT || it.suit == activeSuit || it.rank == top.rank }
    }

    override fun getState(): UniversalTableState {
        val playable = if (phase == TablePhase.PLAYING && activeSeat == 0) legalCards(0).map { it.id }.toSet() else emptySet()
        val seats = (0..3).map { i ->
            SeatSnapshot(
                seatIndex = i, name = names[i], cardCount = hands[i].size,
                isActiveTurn = activeSeat == i && phase == TablePhase.PLAYING
            )
        }
        return UniversalTableState(
            gameId = gameId, ruleProfileId = ruleProfileId, difficulty = difficulty, phase = phase,
            activeSeat = activeSeat, seats = seats, humanHand = hands[0].toList(),
            playableCardIds = playable, hintCardId = playable.firstOrNull(),
            topDiscardCard = discardPile.lastOrNull(), drawPileCount = drawPile.size,
            activeWildSuit = activeSuit,
            bannerMessageEn = bannerEn, bannerMessageBn = bannerBn,
            roundResultSummaryEn = summaryEn, roundResultSummaryBn = summaryBn,
            matchWinnerIsHuman = if (phase == TablePhase.MATCH_OVER) hands[0].isEmpty() else null
        )
    }

    override fun applyMove(move: GameMove): MoveResult = when (move) {
        is GameMove.PlayCard -> {
            val card = legalCards(activeSeat).firstOrNull { it.id == move.cardId } ?: return MoveResult(false, getState())
            hands[activeSeat].remove(card)
            discardPile.add(card)
            if (hands[activeSeat].isEmpty()) {
                phase = TablePhase.MATCH_OVER
                summaryEn = "${names[activeSeat]} shed all cards and WON!"
                summaryBn = "${names[activeSeat]} সব তাস শেষ করে জিতেছে!"
                return MoveResult(true, getState(), if (activeSeat == 0) CardSfx.MATCH_WIN else CardSfx.MATCH_LOSS)
            }
            if (card.rank == Rank.EIGHT) {
                if (activeSeat == 0) {
                    phase = TablePhase.PICK_WILD_SUIT
                    bannerEn = "Crazy 8! Pick the new active suit!"
                    bannerBn = "ক্রেজি ৮! নতুন রং বেছে নিন!"
                    MoveResult(true, getState(), CardSfx.TRUMP_REVEAL)
                } else {
                    activeSuit = Suit.entries.maxByOrNull { s -> hands[activeSeat].count { it.suit == s } } ?: Suit.HEARTS
                    bannerEn = "${names[activeSeat]} played 8 and switched suit to ${activeSuit.nameEn} ${activeSuit.symbol}!"
                    bannerBn = "${names[activeSeat]} ৮ খেলে রং ${activeSuit.nameBn} ${activeSuit.symbol} করেছে!"
                    activeSeat = (activeSeat + 1) % 4
                    MoveResult(true, getState(), CardSfx.TRUMP_REVEAL)
                }
            } else {
                activeSuit = card.suit
                bannerEn = "${names[activeSeat]} played ${card.rank.label}${card.suit.symbol}."
                bannerBn = "${names[activeSeat]} ${card.rank.label}${card.suit.symbol} খেলেছে।"
                activeSeat = (activeSeat + 1) % 4
                MoveResult(true, getState(), CardSfx.CARD_PLACE)
            }
        }
        is GameMove.PickWildSuit -> {
            if (phase != TablePhase.PICK_WILD_SUIT) return MoveResult(false, getState())
            activeSuit = move.suit
            phase = TablePhase.PLAYING
            bannerEn = "Active suit is now ${activeSuit.nameEn} ${activeSuit.symbol}!"
            bannerBn = "নতুন রং হলো ${activeSuit.nameBn} ${activeSuit.symbol}!"
            activeSeat = 1
            MoveResult(true, getState(), CardSfx.BID_ANNOUNCE)
        }
        is GameMove.DrawCard -> {
            if (drawPile.isEmpty() && discardPile.size > 1) {
                val top = discardPile.removeAt(discardPile.lastIndex)
                drawPile.addAll(discardPile.shuffled())
                discardPile.clear()
                discardPile.add(top)
            }
            if (drawPile.isNotEmpty()) {
                val drawn = drawPile.removeAt(0)
                hands[activeSeat].add(drawn)
                bannerEn = "${names[activeSeat]} drew a card."
                bannerBn = "${names[activeSeat]} ১টি তাস তুলেছে।"
            }
            activeSeat = (activeSeat + 1) % 4
            MoveResult(true, getState(), CardSfx.CARD_DEAL)
        }
        is GameMove.NextRound -> { startRound(); MoveResult(true, getState(), CardSfx.CARD_SHUFFLE) }
        else -> MoveResult(false, getState())
    }

    override fun computeAiMove(): GameMove? {
        if (activeSeat == 0 || phase != TablePhase.PLAYING) return null
        val legal = legalCards(activeSeat)
        if (legal.isEmpty()) return GameMove.DrawCard
        val nonEight = legal.filter { it.rank != Rank.EIGHT }.maxByOrNull { it.rank.value }
        return GameMove.PlayCard((nonEight ?: legal.first()).id)
    }

    override fun serializeState(): String = JSONObject().apply {
        put("active", activeSeat); put("suit", activeSuit.code)
        put("hands", JSONArray(hands.map { h -> JSONArray(h.map { it.id }) }))
        put("discard", JSONArray(discardPile.map { it.id }))
        put("draw", JSONArray(drawPile.map { it.id }))
    }.toString()

    override fun restoreState(json: String): UniversalTableState? = try {
        val r = JSONObject(json)
        activeSeat = r.getInt("active"); activeSuit = Suit.fromCode(r.getString("suit"))
        val hA = r.getJSONArray("hands")
        for (i in 0..3) {
            hands[i] = mutableListOf()
            val sub = hA.getJSONArray(i)
            for (j in 0 until sub.length()) Card.fromId(sub.getString(j))?.let { hands[i].add(it) }
        }
        discardPile.clear(); val dA = r.getJSONArray("discard")
        for (i in 0 until dA.length()) Card.fromId(dA.getString(i))?.let { discardPile.add(it) }
        drawPile.clear(); val drA = r.getJSONArray("draw")
        for (i in 0 until drA.length()) Card.fromId(drA.getString(i))?.let { drawPile.add(it) }
        phase = TablePhase.PLAYING; getState()
    } catch (_: Throwable) { null }
}

/**
 * Authentic Blackjack 21 Engine (Virtual Chips Only):
 * - Hit, Stand, Double Down, 3:2 Natural Blackjack, Dealer stands on 17
 */
class BlackjackEngine : CardGameEngine {
    override val gameId: String = GameCatalog.GAME_BLACKJACK
    private var ruleProfileId = "classic_21"
    private var difficulty = AiDifficulty.NORMAL

    private var deck = mutableListOf<Card>()
    private var playerHand = mutableListOf<Card>()
    private var dealerHand = mutableListOf<Card>()
    private var phase = TablePhase.PLAYING
    private var bet = 100L
    private var chips = 5000L
    private var dealerHidden = true
    private var bannerEn = ""
    private var bannerBn = ""
    private var summaryEn = ""
    private var summaryBn = ""
    private var humanWon: Boolean? = null

    override fun initializeGame(ruleProfileId: String, difficulty: AiDifficulty, playerName: String, virtualChips: Long): UniversalTableState {
        this.ruleProfileId = ruleProfileId; this.difficulty = difficulty
        this.chips = virtualChips.coerceAtLeast(200L)
        dealNewHand(); return getState()
    }

    private fun handValue(cards: List<Card>): Int {
        var total = cards.sumOf { it.blackjackValue }
        var aces = cards.count { it.rank == Rank.ACE }
        while (total > 21 && aces > 0) {
            total -= 10; aces--
        }
        return total
    }

    private fun dealNewHand() {
        if (deck.size < 14) deck = DeckFactory.shuffledDeck(DeckType.STANDARD_52).toMutableList()
        bet = 100L.coerceAtMost(chips)
        chips -= bet
        playerHand = mutableListOf(deck.removeAt(0), deck.removeAt(0))
        dealerHand = mutableListOf(deck.removeAt(0), deck.removeAt(0))
        dealerHidden = true
        phase = TablePhase.PLAYING
        humanWon = null; summaryEn = ""; summaryBn = ""

        if (handValue(playerHand) == 21) {
            dealerHidden = false
            val payout = (bet * 5) / 2 // 3:2 natural blackjack
            chips += payout
            phase = TablePhase.ROUND_OVER
            humanWon = true
            summaryEn = "BLACKJACK! Natural 21 pays 3:2 (+${payout - bet} virtual chips)!"
            summaryBn = "ব্ল্যাকজ্যাক! ন্যাচারাল ২১-এ ৩:২ বোনাস (+${payout - bet} চিপস)!"
            bannerEn = summaryEn; bannerBn = summaryBn
        } else {
            bannerEn = "Your hand: ${handValue(playerHand)}. Hit, Stand, or Double Down?"
            bannerBn = "আপনার পয়েন্ট: ${handValue(playerHand)}। কার্ড নিন, থামুন বা ডাবল করুন।"
        }
    }

    override fun getState(): UniversalTableState {
        val visibleDealerVal = if (dealerHidden && dealerHand.isNotEmpty()) dealerHand.first().blackjackValue else handValue(dealerHand)
        return UniversalTableState(
            gameId = gameId, ruleProfileId = ruleProfileId, difficulty = difficulty, phase = phase,
            activeSeat = 0, humanHand = playerHand.toList(),
            blackjackDealerCards = dealerHand.toList(), blackjackDealerHidden = dealerHidden,
            blackjackPlayerTotal = handValue(playerHand), blackjackDealerTotal = visibleDealerVal,
            canBlackjackDouble = phase == TablePhase.PLAYING && playerHand.size == 2 && chips >= bet,
            virtualPot = bet, virtualChips = chips,
            bannerMessageEn = bannerEn, bannerMessageBn = bannerBn,
            roundResultSummaryEn = summaryEn, roundResultSummaryBn = summaryBn,
            matchWinnerIsHuman = humanWon
        )
    }

    override fun applyMove(move: GameMove): MoveResult = when (move) {
        is GameMove.BlackjackHit -> {
            playerHand.add(deck.removeAt(0))
            val total = handValue(playerHand)
            if (total > 21) {
                dealerHidden = false; phase = TablePhase.ROUND_OVER; humanWon = false
                summaryEn = "Bust with $total! Dealer wins."
                summaryBn = "$total পয়েন্টে বাস্ট! ডিলার জিতেছে।"
                bannerEn = summaryEn; bannerBn = summaryBn
                MoveResult(true, getState(), CardSfx.MATCH_LOSS, chipDelta = -bet)
            } else if (total == 21) {
                settleDealer()
            } else {
                bannerEn = "Your hand: $total. Hit or Stand?"
                bannerBn = "আপনার পয়েন্ট: $total। কার্ড নেবেন না থামবেন?"
                MoveResult(true, getState(), CardSfx.CARD_DEAL)
            }
        }
        is GameMove.BlackjackDouble -> {
            if (playerHand.size == 2 && chips >= bet) {
                chips -= bet; bet *= 2
                playerHand.add(deck.removeAt(0))
                if (handValue(playerHand) > 21) {
                    dealerHidden = false; phase = TablePhase.ROUND_OVER; humanWon = false
                    summaryEn = "Doubled & Bust (${handValue(playerHand)})!"
                    summaryBn = "ডাবল করে বাস্ট (${handValue(playerHand)})!"
                    bannerEn = summaryEn; bannerBn = summaryBn
                    MoveResult(true, getState(), CardSfx.MATCH_LOSS, chipDelta = -bet)
                } else settleDealer()
            } else MoveResult(false, getState())
        }
        is GameMove.BlackjackStand -> settleDealer()
        is GameMove.NextRound -> { dealNewHand(); MoveResult(true, getState(), CardSfx.CARD_SHUFFLE) }
        else -> MoveResult(false, getState())
    }

    private fun settleDealer(): MoveResult {
        dealerHidden = false
        while (handValue(dealerHand) < 17) dealerHand.add(deck.removeAt(0))
        val pVal = handValue(playerHand); val dVal = handValue(dealerHand)
        phase = TablePhase.ROUND_OVER
        return when {
            dVal > 21 || pVal > dVal -> {
                chips += bet * 2; humanWon = true
                summaryEn = "You WIN ($pVal vs Dealer $dVal)! +$bet chips"
                summaryBn = "আপনি জিতেছেন ($pVal বনাম ডিলার $dVal)! +$bet চিপস"
                bannerEn = summaryEn; bannerBn = summaryBn
                MoveResult(true, getState(), CardSfx.MATCH_WIN, chipDelta = bet)
            }
            pVal == dVal -> {
                chips += bet; humanWon = null
                summaryEn = "Push ($pVal vs $dVal). Bet returned."
                summaryBn = "ড্র ($pVal বনাম $dVal)। চিপস ফেরত।"
                bannerEn = summaryEn; bannerBn = summaryBn
                MoveResult(true, getState(), CardSfx.TRICK_WIN, chipDelta = 0L)
            }
            else -> {
                humanWon = false
                summaryEn = "Dealer wins ($dVal vs $pVal)."
                summaryBn = "ডিলার জিতেছে ($dVal বনাম $pVal)।"
                bannerEn = summaryEn; bannerBn = summaryBn
                MoveResult(true, getState(), CardSfx.MATCH_LOSS, chipDelta = -bet)
            }
        }
    }

    override fun computeAiMove(): GameMove? = null
    override fun serializeState(): String = JSONObject().put("chips", chips).toString()
    override fun restoreState(json: String): UniversalTableState? {
        chips = JSONObject(json).optLong("chips", 5000L)
        dealNewHand(); return getState()
    }
}

/**
 * Authentic Klondike Solitaire Engine:
 * - 7 Tableau columns, 4 Foundation piles (A->K by suit), Stock & Waste (Draw 1 or Draw 3)
 * - Smart single-tap auto-routing (Foundations first, then alternating-color Tableau, Kings to empty column)
 * - Full Undo Stack + Auto-Finish when all tableau cards are face-up!
 */
class KlondikeSolitaireEngine : CardGameEngine {
    override val gameId: String = GameCatalog.GAME_KLONDIKE
    private var ruleProfileId = "draw_1"
    private var difficulty = AiDifficulty.NORMAL
    private val drawCount: Int get() = if (ruleProfileId == "draw_3") 3 else 1

    private var tableau = MutableList(7) { mutableListOf<Card>() }
    private var faceUpIdx = IntArray(7)
    private var foundations = MutableList(4) { mutableListOf<Card>() }
    private var stock = mutableListOf<Card>()
    private var waste = mutableListOf<Card>()
    private var score = 0
    private var moves = 0
    private var phase = TablePhase.PLAYING
    private var bannerEn = ""
    private var bannerBn = ""
    private val undoStack = ArrayDeque<String>()

    override fun initializeGame(ruleProfileId: String, difficulty: AiDifficulty, playerName: String, virtualChips: Long): UniversalTableState {
        this.ruleProfileId = ruleProfileId; this.difficulty = difficulty
        dealNewGame(); return getState()
    }

    private fun dealNewGame() {
        val deck = DeckFactory.shuffledDeck(DeckType.STANDARD_52).toMutableList()
        tableau = MutableList(7) { mutableListOf() }
        faceUpIdx = IntArray(7)
        for (col in 0..6) {
            for (k in 0..col) tableau[col].add(deck.removeAt(0))
            faceUpIdx[col] = col
        }
        foundations = MutableList(4) { mutableListOf() }
        stock = deck
        waste = mutableListOf()
        score = 0; moves = 0; undoStack.clear(); phase = TablePhase.PLAYING
        bannerEn = "Tap any highlighted card to move it, or tap Stock to draw."
        bannerBn = "যেকোনো খোলা তাসে ট্যাপ করে সরান অথবা ডেক থেকে তাস তুলুন।"
    }

    private fun canAutoFinish(): Boolean =
        stock.isEmpty() && waste.isEmpty() && (0..6).all { c -> tableau[c].isEmpty() || faceUpIdx[c] == 0 }

    override fun getState(): UniversalTableState {
        val cols = (0..6).map { c -> SolitaireColumn(tableau[c].toList(), faceUpIdx[c]) }
        val playable = computeMovableCardIds()
        return UniversalTableState(
            gameId = gameId, ruleProfileId = ruleProfileId, difficulty = difficulty, phase = phase,
            activeSeat = 0, playableCardIds = playable, hintCardId = playable.firstOrNull(),
            solitaireData = SolitaireTableData(
                tableau = cols,
                foundations = foundations.map { it.toList() },
                stockCount = stock.size,
                waste = waste.takeLast(3),
                score = score,
                movesCount = moves,
                canUndo = undoStack.isNotEmpty(),
                canAutoFinish = phase == TablePhase.PLAYING && canAutoFinish(),
                drawCount = drawCount
            ),
            bannerMessageEn = bannerEn, bannerMessageBn = bannerBn,
            roundResultSummaryEn = if (phase == TablePhase.MATCH_OVER) "Solitaire Solved! Score: $score in $moves moves." else "",
            roundResultSummaryBn = if (phase == TablePhase.MATCH_OVER) "সলিটায়ার সম্পূর্ণ! স্কোর: $score ($moves চালে)।" else "",
            matchWinnerIsHuman = if (phase == TablePhase.MATCH_OVER) true else null
        )
    }

    private fun canPlaceOnFoundation(card: Card, fIdx: Int): Boolean {
        val pile = foundations[fIdx]
        return if (pile.isEmpty()) card.rank == Rank.ACE
        else pile.last().suit == card.suit && card.rank.value == pile.last().rank.value + 1
    }

    private fun canPlaceOnTableau(card: Card, colIdx: Int): Boolean {
        val col = tableau[colIdx]
        return if (col.isEmpty()) card.rank == Rank.KING
        else col.last().suit.isRed != card.suit.isRed && card.rank.value == col.last().rank.value - 1
    }

    private fun computeMovableCardIds(): Set<String> {
        val out = mutableSetOf<String>()
        waste.lastOrNull()?.let { w ->
            if ((0..3).any { canPlaceOnFoundation(w, it) } || (0..6).any { canPlaceOnTableau(w, it) }) out.add(w.id)
        }
        for (c in 0..6) {
            val col = tableau[c]
            for (idx in faceUpIdx[c] until col.size) {
                val card = col[idx]
                val isTop = idx == col.lastIndex
                if (isTop && (0..3).any { canPlaceOnFoundation(card, it) }) out.add(card.id)
                if ((0..6).any { dest -> dest != c && canPlaceOnTableau(card, dest) }) out.add(card.id)
            }
        }
        return out
    }

    override fun applyMove(move: GameMove): MoveResult = when (move) {
        is GameMove.SolitaireDrawStock -> {
            pushUndo()
            if (stock.isEmpty()) {
                if (waste.isNotEmpty()) {
                    stock.addAll(waste.reversed())
                    waste.clear()
                    moves++
                }
            } else {
                repeat(drawCount.coerceAtMost(stock.size)) {
                    waste.add(stock.removeAt(0))
                }
                moves++
            }
            MoveResult(true, getState(), CardSfx.CARD_FLIP)
        }
        is GameMove.SolitaireTap -> handleTapCard(move.cardId)
        is GameMove.SolitaireUndo -> {
            val prev = undoStack.removeLastOrNull()
            if (prev != null) {
                restoreState(prev)
                MoveResult(true, getState(), CardSfx.UI_CLICK)
            } else MoveResult(false, getState())
        }
        is GameMove.SolitaireAutoFinish -> {
            if (!canAutoFinish()) MoveResult(false, getState())
            else {
                pushUndo()
                var progressed = true
                while (progressed) {
                    progressed = false
                    for (c in 0..6) {
                        val top = tableau[c].lastOrNull() ?: continue
                        val fIdx = (0..3).firstOrNull { canPlaceOnFoundation(top, it) }
                        if (fIdx != null) {
                            tableau[c].removeAt(tableau[c].lastIndex)
                            foundations[fIdx].add(top)
                            score += 10; moves++
                            progressed = true
                        }
                    }
                }
                checkWin()
                MoveResult(true, getState(), CardSfx.MATCH_WIN)
            }
        }
        is GameMove.NextRound -> { dealNewGame(); MoveResult(true, getState(), CardSfx.CARD_SHUFFLE) }
        else -> MoveResult(false, getState())
    }

    private fun handleTapCard(cardId: String): MoveResult {
        // 1. Check Waste top card
        val wTop = waste.lastOrNull()
        if (wTop != null && wTop.id == cardId) {
            val fIdx = (0..3).firstOrNull { canPlaceOnFoundation(wTop, it) }
            if (fIdx != null) {
                pushUndo(); waste.removeAt(waste.lastIndex); foundations[fIdx].add(wTop)
                score += 10; moves++; checkWin()
                return MoveResult(true, getState(), if (phase == TablePhase.MATCH_OVER) CardSfx.MATCH_WIN else CardSfx.CARD_PLACE)
            }
            val tIdx = (0..6).firstOrNull { canPlaceOnTableau(wTop, it) }
            if (tIdx != null) {
                pushUndo(); waste.removeAt(waste.lastIndex); tableau[tIdx].add(wTop)
                score += 5; moves++
                return MoveResult(true, getState(), CardSfx.CARD_PLACE)
            }
            return MoveResult(false, getState())
        }

        // 2. Check Tableau cards (supports moving a full face-up stack!)
        for (c in 0..6) {
            val col = tableau[c]
            val idx = col.indexOfFirst { it.id == cardId }
            if (idx >= faceUpIdx[c] && idx in col.indices) {
                val movingCard = col[idx]
                // Single top card -> Foundation first
                if (idx == col.lastIndex) {
                    val fIdx = (0..3).firstOrNull { canPlaceOnFoundation(movingCard, it) }
                    if (fIdx != null) {
                        pushUndo()
                        col.removeAt(col.lastIndex)
                        foundations[fIdx].add(movingCard)
                        if (col.isNotEmpty() && faceUpIdx[c] >= col.size) {
                            faceUpIdx[c] = col.lastIndex; score += 5
                        }
                        score += 10; moves++; checkWin()
                        return MoveResult(true, getState(), if (phase == TablePhase.MATCH_OVER) CardSfx.MATCH_WIN else CardSfx.CARD_PLACE)
                    }
                }
                // Move card or sub-stack to another Tableau column
                val dest = (0..6).firstOrNull { d -> d != c && canPlaceOnTableau(movingCard, d) }
                if (dest != null) {
                    pushUndo()
                    val movingStack = col.subList(idx, col.size).toList()
                    repeat(movingStack.size) { col.removeAt(col.lastIndex) }
                    tableau[dest].addAll(movingStack)
                    if (col.isNotEmpty() && faceUpIdx[c] >= col.size) {
                        faceUpIdx[c] = col.lastIndex; score += 5
                    }
                    moves++
                    return MoveResult(true, getState(), CardSfx.CARD_PLACE)
                }
            }
        }
        return MoveResult(false, getState())
    }

    private fun checkWin() {
        if (foundations.sumOf { it.size } == 52) {
            phase = TablePhase.MATCH_OVER
            score += 100
            bannerEn = "Congratulations! Klondike Solitaire completed!"
            bannerBn = "অভিনন্দন! সলিটায়ার সফলভাবে সম্পন্ন হয়েছে!"
        }
    }

    private fun pushUndo() {
        if (undoStack.size >= 25) undoStack.removeFirst()
        undoStack.addLast(serializeState())
    }

    override fun computeAiMove(): GameMove? = null

    override fun serializeState(): String = JSONObject().apply {
        put("profile", ruleProfileId); put("score", score); put("moves", moves)
        put("faceUp", JSONArray(faceUpIdx.toList()))
        put("tableau", JSONArray(tableau.map { c -> JSONArray(c.map { it.id }) }))
        put("foundations", JSONArray(foundations.map { f -> JSONArray(f.map { it.id }) }))
        put("stock", JSONArray(stock.map { it.id }))
        put("waste", JSONArray(waste.map { it.id }))
    }.toString()

    override fun restoreState(json: String): UniversalTableState? = try {
        val r = JSONObject(json)
        ruleProfileId = r.optString("profile", "draw_1")
        score = r.optInt("score", 0); moves = r.optInt("moves", 0)
        val fu = r.getJSONArray("faceUp")
        val tb = r.getJSONArray("tableau")
        for (i in 0..6) {
            faceUpIdx[i] = fu.getInt(i); tableau[i] = mutableListOf()
            val sub = tb.getJSONArray(i)
            for (j in 0 until sub.length()) Card.fromId(sub.getString(j))?.let { tableau[i].add(it) }
        }
        val fn = r.getJSONArray("foundations")
        for (i in 0..3) {
            foundations[i] = mutableListOf()
            val sub = fn.getJSONArray(i)
            for (j in 0 until sub.length()) Card.fromId(sub.getString(j))?.let { foundations[i].add(it) }
        }
        stock = mutableListOf(); val st = r.getJSONArray("stock")
        for (i in 0 until st.length()) Card.fromId(st.getString(i))?.let { stock.add(it) }
        waste = mutableListOf(); val ws = r.getJSONArray("waste")
        for (i in 0 until ws.length()) Card.fromId(ws.getString(i))?.let { waste.add(it) }
        phase = if (foundations.sumOf { it.size } == 52) TablePhase.MATCH_OVER else TablePhase.PLAYING
        getState()
    } catch (_: Throwable) { null }
}

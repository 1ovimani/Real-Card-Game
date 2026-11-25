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
 * Authentic Bangladeshi Twenty-Nine (29) Game Engine.
 * - 4 Players, 2 Teams (Team 0: South[0] & North[2], Team 1: West[1] & East[3])
 * - 32-Card Piquet Deck (7..A)
 * - Exact Ranking: J(3) > 9(2) > A(1) > 10(1) > K(0) > Q(0) > 8(0) > 7(0) = 28 + 1 last trick = 29 pts
 * - Authentic 4 + 4 deal: Bidding (16..28) -> Hidden Trump Selection -> Remaining 4 cards dealt
 * - Double (2x) & Redouble (4x)
 * - Hidden Trump Reveal when void in led suit
 * - Marriage / Pair (K + Q of Trump) adjusting target by ±4 when enabled by profile
 */
class TwentyNineEngine : CardGameEngine {
    override val gameId: String = GameCatalog.GAME_TWENTY_NINE

    private var ruleProfileId: String = "classic_dhaka"
    private var difficulty: AiDifficulty = AiDifficulty.NORMAL
    private var names = arrayOf("You", "Rahim", "Partner", "Karim")

    private var phase: TablePhase = TablePhase.BIDDING
    private var roundNumber = 1
    private var dealerSeat = 0
    private var activeSeat = 1

    private var hands = Array(4) { mutableListOf<Card>() }
    private var reserveCards = Array(4) { mutableListOf<Card>() } // 2nd batch of 4 cards before trump chosen

    // Bidding state
    private var highestBid = 15
    private var bidWinnerSeat: Int? = null
    private var passedSeats = BooleanArray(4)
    private var biddingOrderCount = 0

    // Trump & Double state
    private var trumpSuit: Suit? = null
    private var isTrumpHidden = true
    private var trumpRevealedThisTrickBySeat: Int? = null
    private var doubleMultiplier = 1 // 1 = normal, 2 = double, 4 = redouble
    private var pairDeclared = false
    private var effectiveBidTarget = 16

    // Trick play state
    private var currentTrick = mutableListOf<PlayedTableCard>()
    private var trickLeaderSeat = 1
    private var tricksPlayed = 0
    private var teamRoundPoints = intArrayOf(0, 0)
    private var seatTricks = IntArray(4)
    private var teamMatchScores = intArrayOf(0, 0) // Race to +6 or -6
    private var history = mutableListOf<RoundHistoryEntry>()
    private var bannerEn = ""
    private var bannerBn = ""
    private var roundSummaryEn = ""
    private var roundSummaryBn = ""

    private val pairRuleEnabled: Boolean
        get() = ruleProfileId != "strict_29"

    override fun initializeGame(
        ruleProfileId: String,
        difficulty: AiDifficulty,
        playerName: String,
        virtualChips: Long
    ): UniversalTableState {
        this.ruleProfileId = ruleProfileId
        this.difficulty = difficulty
        this.names = arrayOf(playerName.ifBlank { "You" }, "Rahim", "Tariq (Partner)", "Karim")
        this.roundNumber = 1
        this.dealerSeat = 0
        this.teamMatchScores = intArrayOf(0, 0)
        this.history.clear()
        startNewRound()
        return getState()
    }

    private fun startNewRound() {
        val deck = DeckFactory.shuffledDeck(DeckType.PIQUET_32)
        for (i in 0..3) {
            hands[i] = deck.subList(i * 4, i * 4 + 4).toMutableList()
            reserveCards[i] = deck.subList(16 + i * 4, 16 + i * 4 + 4).toMutableList()
            sortHand(hands[i])
        }
        highestBid = 15
        bidWinnerSeat = null
        passedSeats = BooleanArray(4)
        biddingOrderCount = 0
        trumpSuit = null
        isTrumpHidden = true
        trumpRevealedThisTrickBySeat = null
        doubleMultiplier = 1
        pairDeclared = false
        effectiveBidTarget = 16
        currentTrick.clear()
        tricksPlayed = 0
        teamRoundPoints = intArrayOf(0, 0)
        seatTricks = IntArray(4)
        roundSummaryEn = ""
        roundSummaryBn = ""

        phase = TablePhase.BIDDING
        activeSeat = (dealerSeat + 1) % 4
        trickLeaderSeat = activeSeat
        bannerEn = "Round $roundNumber: 4 cards dealt. Place your bid (16–28) or Pass."
        bannerBn = "রাউন্ড $roundNumber: ৪টি করে তাস দেওয়া হয়েছে। ১৬–২৮ ডাক দিন বা পাস করুন।"
    }

    override fun getState(): UniversalTableState {
        val playable = if (phase == TablePhase.PLAYING && activeSeat == 0) {
            legalCardsForSeat(0).map { it.id }.toSet()
        } else emptySet()

        val canReveal = phase == TablePhase.PLAYING && activeSeat == 0 && canSeatRevealTrump(0)
        val canPair = phase == TablePhase.PLAYING && activeSeat == 0 && canSeatDeclarePair(0)

        val bidOpts = if (phase == TablePhase.BIDDING && activeSeat == 0) {
            val minNext = (highestBid + 1).coerceAtLeast(16)
            (minNext..28).toList()
        } else emptyList()

        val hintId = if (phase == TablePhase.PLAYING && activeSeat == 0) {
            chooseSmartCard(0)?.id
        } else null

        val seats = (0..3).map { i ->
            val team = i % 2
            val badge = buildString {
                if (bidWinnerSeat == i) append("Bid ${effectiveBidTarget} ")
                if (passedSeats[i] && phase == TablePhase.BIDDING) append("Pass")
            }.trim()
            SeatSnapshot(
                seatIndex = i,
                name = names[i],
                cardCount = hands[i].size,
                totalScore = teamMatchScores[team].toDouble(),
                roundScore = teamRoundPoints[team],
                tricksWon = seatTricks[i],
                bid = if (bidWinnerSeat == i) effectiveBidTarget else null,
                isDealer = dealerSeat == i,
                isActiveTurn = activeSeat == i && phase != TablePhase.ROUND_OVER && phase != TablePhase.MATCH_OVER,
                teamId = team,
                statusBadge = badge
            )
        }

        val matchWinnerHuman = if (phase == TablePhase.MATCH_OVER) {
            teamMatchScores[0] >= 6 || teamMatchScores[1] <= -6
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
            hintCardId = hintId,
            centerCards = currentTrick.toList(),
            trumpSuit = trumpSuit,
            isTrumpHidden = isTrumpHidden,
            currentBid = if (highestBid >= 16) effectiveBidTarget else 0,
            bidWinnerSeat = bidWinnerSeat,
            bidOptions = bidOpts,
            canPassBid = phase == TablePhase.BIDDING && activeSeat == 0 && !(bidWinnerSeat == null && passedSeats.count { it } == 3),
            canRevealTrump = canReveal,
            canDeclarePair = canPair,
            canCallDouble = phase == TablePhase.DOUBLE_DECISION && activeSeat == 0 && doubleMultiplier == 1,
            canCallRedouble = phase == TablePhase.DOUBLE_DECISION && activeSeat == 0 && doubleMultiplier == 2,
            doubleMultiplier = doubleMultiplier,
            teamScores = teamMatchScores[0] to teamMatchScores[1],
            teamRoundPoints = teamRoundPoints[0] to teamRoundPoints[1],
            bannerMessageEn = bannerEn,
            bannerMessageBn = bannerBn,
            roundResultSummaryEn = roundSummaryEn,
            roundResultSummaryBn = roundSummaryBn,
            matchWinnerIsHuman = matchWinnerHuman,
            roundHistory = history.toList()
        )
    }

    override fun applyMove(move: GameMove): MoveResult {
        return when (move) {
            is GameMove.PlaceBid -> handleBid(move.bid)
            is GameMove.SelectTrump -> handleSelectTrump(move.suit)
            is GameMove.CallDouble -> handleDoubleDecision(2)
            is GameMove.CallRedouble -> handleDoubleDecision(4)
            is GameMove.ContinueNormal -> handleDoubleDecision(doubleMultiplier)
            is GameMove.RevealTrump -> handleRevealTrump()
            is GameMove.DeclarePair -> handleDeclarePair()
            is GameMove.PlayCard -> handlePlayCard(move.cardId)
            is GameMove.NextRound -> {
                if (phase == TablePhase.MATCH_OVER) {
                    teamMatchScores = intArrayOf(0, 0)
                    roundNumber = 1
                    history.clear()
                } else {
                    roundNumber++
                    dealerSeat = (dealerSeat + 1) % 4
                }
                startNewRound()
                MoveResult(true, getState(), CardSfx.CARD_SHUFFLE)
            }
            else -> MoveResult(false, getState(), errorReason = "Unsupported move for Twenty-Nine")
        }
    }

    private fun handleBid(bid: Int): MoveResult {
        if (phase != TablePhase.BIDDING) return MoveResult(false, getState(), errorReason = "Not bidding phase")
        val seat = activeSeat
        if (bid == -1) {
            // Forced minimum 16 if everyone else passed
            if (bidWinnerSeat == null && passedSeats.count { it } == 3) {
                highestBid = 16
                effectiveBidTarget = 16
                bidWinnerSeat = seat
                bannerEn = "${names[seat]} takes forced bid of 16."
                bannerBn = "${names[seat]} বাধ্যতামূলক ১৬ ডাক নিয়েছে।"
                finishBidding()
                return MoveResult(true, getState(), CardSfx.BID_ANNOUNCE)
            }
            passedSeats[seat] = true
            bannerEn = "${names[seat]} passed."
            bannerBn = "${names[seat]} পাস করেছে।"
        } else {
            val minAllowed = (highestBid + 1).coerceAtLeast(16)
            if (bid !in minAllowed..28) {
                return MoveResult(false, getState(), errorReason = "Bid must be between $minAllowed and 28")
            }
            highestBid = bid
            effectiveBidTarget = bid
            bidWinnerSeat = seat
            bannerEn = "${names[seat]} bids $bid!"
            bannerBn = "${names[seat]} $bid ডাক দিয়েছে!"
        }

        biddingOrderCount++
        val activeBidders = (0..3).filter { !passedSeats[it] }
        if (activeBidders.size == 1 && bidWinnerSeat != null) {
            finishBidding()
            return MoveResult(true, getState(), CardSfx.BID_ANNOUNCE)
        }
        if (highestBid == 28 && bidWinnerSeat != null) {
            finishBidding()
            return MoveResult(true, getState(), CardSfx.BID_ANNOUNCE)
        }
        // Advance to next non-passed seat
        var next = (seat + 1) % 4
        while (passedSeats[next] && next != seat) {
            next = (next + 1) % 4
        }
        if (next == bidWinnerSeat && activeBidders.size == 1) {
            finishBidding()
        } else {
            activeSeat = next
        }
        return MoveResult(true, getState(), CardSfx.BID_ANNOUNCE)
    }

    private fun finishBidding() {
        val winner = bidWinnerSeat ?: ((dealerSeat + 1) % 4)
        bidWinnerSeat = winner
        effectiveBidTarget = highestBid.coerceAtLeast(16)
        phase = TablePhase.SELECT_TRUMP
        activeSeat = winner
        bannerEn = "${names[winner]} won the bid ($effectiveBidTarget). Choosing hidden trump..."
        bannerBn = "${names[winner]} $effectiveBidTarget ডাকে জিতেছে। গোপন রং বাছা হচ্ছে..."
    }

    private fun handleSelectTrump(suit: Suit): MoveResult {
        if (phase != TablePhase.SELECT_TRUMP) return MoveResult(false, getState(), errorReason = "Not trump selection phase")
        trumpSuit = suit
        isTrumpHidden = true

        // Deal remaining 4 cards to every player (8 cards total)
        for (i in 0..3) {
            hands[i].addAll(reserveCards[i])
            reserveCards[i].clear()
            sortHand(hands[i])
        }

        val bidderTeam = (bidWinnerSeat ?: 0) % 2
        // Offer Double to opposing team! If human is on defending team, ask human directly.
        if (bidderTeam == 1) {
            phase = TablePhase.DOUBLE_DECISION
            activeSeat = 0
            doubleMultiplier = 1
            bannerEn = "Hidden trump set! All 8 cards dealt. Call Double (2×) or Play Normal?"
            bannerBn = "গোপন রং নির্ধারিত! ৮ তাস দেওয়া হয়েছে। ডাবল (২×) করবেন না সাধারণ খেলবেন?"
        } else {
            // Check if defending AI wants to Double
            val defenderStrong = evaluateDefenderDoubleStrength()
            if (defenderStrong) {
                doubleMultiplier = 2
                phase = TablePhase.DOUBLE_DECISION
                activeSeat = 0
                bannerEn = "Opponents called DOUBLE (2×)! Call Redouble (4×) or accept?"
                bannerBn = "প্রতিপক্ষ ডাবল (২×) ডেকেছে! রি-ডাবল (৪×) করবেন?"
            } else {
                startTrickPlay()
            }
        }
        return MoveResult(true, getState(), CardSfx.CARD_DEAL)
    }

    private fun handleDoubleDecision(chosenMult: Int): MoveResult {
        if (phase != TablePhase.DOUBLE_DECISION) return MoveResult(false, getState())
        doubleMultiplier = chosenMult.coerceIn(1, 4)
        startTrickPlay()
        val sfx = if (doubleMultiplier > 1) CardSfx.BID_ANNOUNCE else CardSfx.UI_CLICK
        return MoveResult(true, getState(), sfx)
    }

    private fun startTrickPlay() {
        phase = TablePhase.PLAYING
        trickLeaderSeat = (dealerSeat + 1) % 4
        activeSeat = trickLeaderSeat
        val multTag = when (doubleMultiplier) {
            2 -> " [DOUBLE 2×]"
            4 -> " [REDOUBLE 4×]"
            else -> ""
        }
        bannerEn = "Target $effectiveBidTarget$multTag — ${names[activeSeat]} leads Trick 1."
        bannerBn = "লক্ষ্য $effectiveBidTarget$multTag — ${names[activeSeat]} ১ম হাতের চাল দিচ্ছে।"
    }

    private fun canSeatRevealTrump(seat: Int): Boolean {
        if (!isTrumpHidden || trumpSuit == null) return false
        if (currentTrick.isEmpty()) return false // Cannot reveal when leading
        val ledSuit = currentTrick.first().card.suit
        return hands[seat].none { it.suit == ledSuit }
    }

    private fun canSeatDeclarePair(seat: Int): Boolean {
        if (!pairRuleEnabled || pairDeclared || isTrumpHidden) return false
        val t = trumpSuit ?: return false
        val hasK = hands[seat].any { it.suit == t && it.rank == Rank.KING }
        val hasQ = hands[seat].any { it.suit == t && it.rank == Rank.QUEEN }
        return hasK && hasQ
    }

    private fun handleRevealTrump(): MoveResult {
        if (phase != TablePhase.PLAYING || !canSeatRevealTrump(activeSeat)) {
            return MoveResult(false, getState(), errorReason = "Cannot reveal trump right now")
        }
        isTrumpHidden = false
        trumpRevealedThisTrickBySeat = activeSeat
        val t = trumpSuit!!
        bannerEn = "${names[activeSeat]} revealed Trump: ${t.nameEn} ${t.symbol}!"
        bannerBn = "${names[activeSeat]} গোপন রং খুলেছে: ${t.nameBn} ${t.symbol}!"
        return MoveResult(true, getState(), CardSfx.TRUMP_REVEAL)
    }

    private fun handleDeclarePair(): MoveResult {
        if (phase != TablePhase.PLAYING || !canSeatDeclarePair(activeSeat)) {
            return MoveResult(false, getState(), errorReason = "Cannot declare Pair")
        }
        pairDeclared = true
        val bidderTeam = (bidWinnerSeat ?: 0) % 2
        val declarerTeam = activeSeat % 2
        if (declarerTeam == bidderTeam) {
            effectiveBidTarget = (effectiveBidTarget - 4).coerceAtLeast(16)
            bannerEn = "${names[activeSeat]} declared TRUMP PAIR (K+Q)! Target drops to $effectiveBidTarget!"
            bannerBn = "${names[activeSeat]} রঙের জোড়া (K+Q) দেখিয়েছে! লক্ষ্য কমে $effectiveBidTarget হলো!"
        } else {
            effectiveBidTarget = (effectiveBidTarget + 4).coerceAtMost(28)
            bannerEn = "${names[activeSeat]} declared DEFENDER PAIR (K+Q)! Target rises to $effectiveBidTarget!"
            bannerBn = "${names[activeSeat]} বিপক্ষের জোড়া (K+Q) দেখিয়েছে! লক্ষ্য বেড়ে $effectiveBidTarget হলো!"
        }
        return MoveResult(true, getState(), CardSfx.TRUMP_REVEAL)
    }

    fun legalCardsForSeat(seat: Int): List<Card> {
        val hand = hands[seat]
        if (hand.isEmpty()) return emptyList()
        if (currentTrick.isEmpty()) return hand

        val ledSuit = currentTrick.first().card.suit
        val sameSuit = hand.filter { it.suit == ledSuit }
        if (sameSuit.isNotEmpty()) return sameSuit

        // If this player JUST revealed trump on this trick, authentic 29 requires playing a trump card if held!
        if (trumpRevealedThisTrickBySeat == seat && trumpSuit != null) {
            val trumps = hand.filter { it.suit == trumpSuit }
            if (trumps.isNotEmpty()) return trumps
        }
        return hand
    }

    private fun handlePlayCard(cardId: String): MoveResult {
        if (phase != TablePhase.PLAYING) return MoveResult(false, getState(), errorReason = "Not playing phase")
        val seat = activeSeat
        val legal = legalCardsForSeat(seat)
        val card = legal.firstOrNull { it.id == cardId }
            ?: return MoveResult(false, getState(), errorReason = "Illegal move: must follow suit")

        hands[seat].remove(card)
        currentTrick.add(PlayedTableCard(seat, card))

        if (currentTrick.size < 4) {
            activeSeat = (seat + 1) % 4
            return MoveResult(true, getState(), CardSfx.CARD_PLACE)
        }

        // Complete trick!
        val winnerSeat = computeTrickWinner(currentTrick)
        val trickPts = currentTrick.sumOf { it.card.twentyNinePoints }
        tricksPlayed++
        val lastTrickBonus = if (tricksPlayed == 8) 1 else 0
        val totalAward = trickPts + lastTrickBonus
        val winnerTeam = winnerSeat % 2
        teamRoundPoints[winnerTeam] += totalAward
        seatTricks[winnerSeat]++

        val bonusNote = if (lastTrickBonus > 0) " (+1 last trick)" else ""
        bannerEn = "${names[winnerSeat]} wins Trick $tricksPlayed (+$totalAward pts$bonusNote)."
        bannerBn = "${names[winnerSeat]} $tricksPlayed নং হাত জিতেছে (+$totalAward পয়েন্ট)।"

        currentTrick.clear()
        trumpRevealedThisTrickBySeat = null

        if (tricksPlayed >= 8) {
            concludeRound()
            val humanWonRound = ((bidWinnerSeat ?: 0) % 2 == 0 && teamRoundPoints[0] >= effectiveBidTarget) ||
                ((bidWinnerSeat ?: 0) % 2 == 1 && teamRoundPoints[1] < effectiveBidTarget)
            val sfx = if (phase == TablePhase.MATCH_OVER) {
                if (teamMatchScores[0] >= 6 || teamMatchScores[1] <= -6) CardSfx.MATCH_WIN else CardSfx.MATCH_LOSS
            } else {
                if (humanWonRound) CardSfx.ROUND_WIN else CardSfx.TRICK_WIN
            }
            return MoveResult(true, getState(), sfx)
        }

        trickLeaderSeat = winnerSeat
        activeSeat = winnerSeat
        return MoveResult(true, getState(), CardSfx.TRICK_WIN)
    }

    private fun computeTrickWinner(trick: List<PlayedTableCard>): Int {
        val ledSuit = trick.first().card.suit
        var best = trick.first()
        for (played in trick.drop(1)) {
            val c = played.card
            val b = best.card
            val cIsTrump = !isTrumpHidden && trumpSuit != null && c.suit == trumpSuit
            val bIsTrump = !isTrumpHidden && trumpSuit != null && b.suit == trumpSuit
            val beats = when {
                cIsTrump && !bIsTrump -> true
                !cIsTrump && bIsTrump -> false
                cIsTrump && bIsTrump -> c.twentyNineOrder > b.twentyNineOrder
                c.suit == ledSuit && b.suit == ledSuit -> c.twentyNineOrder > b.twentyNineOrder
                else -> false
            }
            if (beats) best = played
        }
        return best.seatIndex
    }

    private fun concludeRound() {
        val bidderTeam = (bidWinnerSeat ?: 0) % 2
        val bidderPts = teamRoundPoints[bidderTeam]
        val succeeded = bidderPts >= effectiveBidTarget
        val delta = doubleMultiplier

        if (succeeded) {
            teamMatchScores[bidderTeam] += delta
        } else {
            teamMatchScores[bidderTeam] -= delta
        }

        val teamNameEn = if (bidderTeam == 0) "Your Team" else "Opponents"
        val teamNameBn = if (bidderTeam == 0) "আপনার দল" else "প্রতিপক্ষ"
        val verdictEn = if (succeeded) "MADE bid $effectiveBidTarget with $bidderPts pts (+$delta)" else "FAILED bid $effectiveBidTarget with $bidderPts pts (-$delta)"
        val verdictBn = if (succeeded) "$bidderPts পয়েন্ট তুলে $effectiveBidTarget ডাক পূরণ করেছে (+$delta)" else "$bidderPts পয়েন্ট তুলে $effectiveBidTarget ডাকে ব্যর্থ হয়েছে (-$delta)"

        roundSummaryEn = "$teamNameEn $verdictEn | Match: You ${teamMatchScores[0]} vs Opp ${teamMatchScores[1]}"
        roundSummaryBn = "$teamNameBn $verdictBn | ম্যাচ: আপনি ${teamMatchScores[0]} বনাম প্রতিপক্ষ ${teamMatchScores[1]}"
        history.add(RoundHistoryEntry(roundNumber, roundSummaryEn, roundSummaryBn))

        phase = if (teamMatchScores[0] >= 6 || teamMatchScores[1] >= 6 || teamMatchScores[0] <= -6 || teamMatchScores[1] <= -6) {
            TablePhase.MATCH_OVER
        } else {
            TablePhase.ROUND_OVER
        }
    }

    override fun computeAiMove(): GameMove? {
        if (activeSeat == 0) return null
        return when (phase) {
            TablePhase.BIDDING -> computeAiBid(activeSeat)
            TablePhase.SELECT_TRUMP -> GameMove.SelectTrump(pickBestTrumpSuit(activeSeat))
            TablePhase.PLAYING -> {
                if (canSeatDeclarePair(activeSeat)) return GameMove.DeclarePair
                if (canSeatRevealTrump(activeSeat) && shouldAiRevealTrump(activeSeat)) {
                    return GameMove.RevealTrump
                }
                val card = chooseSmartCard(activeSeat) ?: return null
                GameMove.PlayCard(card.id)
            }
            else -> null
        }
    }

    private fun computeAiBid(seat: Int): GameMove {
        val hand = hands[seat]
        val bestSuit = Suit.entries.maxByOrNull { s -> suitBidScore(hand, s) } ?: Suit.SPADES
        val score = suitBidScore(hand, bestSuit)
        val skillBias = when (difficulty) {
            AiDifficulty.BEGINNER -> -1
            AiDifficulty.EASY -> 0
            AiDifficulty.NORMAL -> 0
            AiDifficulty.HARD -> 1
            AiDifficulty.EXPERT, AiDifficulty.MASTER -> 1
        }
        val maxWilling = (14 + score / 2 + skillBias).coerceIn(15, 23)
        val nextMin = (highestBid + 1).coerceAtLeast(16)
        return if (maxWilling >= nextMin) {
            GameMove.PlaceBid(nextMin)
        } else if (bidWinnerSeat == null && passedSeats.count { it } == 3) {
            GameMove.PlaceBid(16)
        } else {
            GameMove.PlaceBid(-1)
        }
    }

    private fun suitBidScore(hand: List<Card>, suit: Suit): Int {
        var score = 0
        val inSuit = hand.filter { it.suit == suit }
        score += inSuit.size * 2
        for (c in inSuit) {
            score += when (c.rank) {
                Rank.JACK -> 5
                Rank.NINE -> 3
                Rank.ACE -> 2
                Rank.TEN -> 1
                else -> 0
            }
        }
        // Side jacks/aces
        score += hand.count { it.suit != suit && (it.rank == Rank.JACK || it.rank == Rank.ACE) }
        return score
    }

    private fun pickBestTrumpSuit(seat: Int): Suit {
        val hand = hands[seat]
        return Suit.entries.maxByOrNull { s -> suitBidScore(hand, s) } ?: Suit.SPADES
    }

    private fun evaluateDefenderDoubleStrength(): Boolean {
        val def1 = suitBidScore(hands[1], pickBestTrumpSuit(1))
        val def3 = suitBidScore(hands[3], pickBestTrumpSuit(3))
        return (def1 + def3) >= 24 && highestBid >= 20
    }

    private fun shouldAiRevealTrump(seat: Int): Boolean {
        val trickPts = currentTrick.sumOf { it.card.twentyNinePoints }
        val partnerSeat = (seat + 2) % 4
        val currentWinner = computeTrickWinner(currentTrick)
        if (currentWinner == partnerSeat && trickPts < 3) return false
        return trickPts >= 2 || difficulty.level >= AiDifficulty.NORMAL.level
    }

    private fun chooseSmartCard(seat: Int): Card? {
        val legal = legalCardsForSeat(seat)
        if (legal.isEmpty()) return null
        if (legal.size == 1) return legal.first()
        if (difficulty == AiDifficulty.BEGINNER) return legal.random()

        val partnerSeat = (seat + 2) % 4
        if (currentTrick.isEmpty()) {
            // Lead strong non-trump Jack or safe zero card
            val highLead = legal.filter { it.suit != trumpSuit && it.rank == Rank.JACK }
            if (highLead.isNotEmpty()) return highLead.first()
            return legal.minByOrNull { it.twentyNinePoints * 10 + it.twentyNineOrder }
        }

        val currentWinner = computeTrickWinner(currentTrick)
        val partnerWinning = currentWinner == partnerSeat
        val ledSuit = currentTrick.first().card.suit

        if (partnerWinning && currentTrick.size == 3) {
            // Feed highest point card (J/9/A/10) to partner!
            return legal.maxByOrNull { it.twentyNinePoints * 10 - (if (it.suit == trumpSuit) 20 else 0) }
        }

        // Try to win trick cheaply
        val winningCards = legal.filter { candidate ->
            val sim = currentTrick + PlayedTableCard(seat, candidate)
            computeTrickWinner(sim) == seat
        }
        val trickValue = currentTrick.sumOf { it.card.twentyNinePoints }
        if (winningCards.isNotEmpty() && (!partnerWinning || trickValue >= 2)) {
            return winningCards.minByOrNull {
                (if (it.suit == trumpSuit && ledSuit != trumpSuit) 10 else 0) + it.twentyNineOrder
            }
        }

        // Cannot win cleanly — shed lowest-value non-trump card
        return legal.minByOrNull {
            (if (!isTrumpHidden && it.suit == trumpSuit) 50 else 0) + it.twentyNinePoints * 15 + it.twentyNineOrder
        }
    }

    private fun sortHand(list: MutableList<Card>) {
        list.sortWith(compareBy<Card> { it.suit.ordinal }.thenByDescending { it.twentyNineOrder })
    }

    override fun serializeState(): String {
        val root = JSONObject()
        root.put("profile", ruleProfileId)
        root.put("diff", difficulty.level)
        root.put("phase", phase.name)
        root.put("round", roundNumber)
        root.put("dealer", dealerSeat)
        root.put("active", activeSeat)
        root.put("highestBid", highestBid)
        root.put("effectiveBid", effectiveBidTarget)
        root.put("bidWinner", bidWinnerSeat ?: -1)
        root.put("trump", trumpSuit?.code ?: "")
        root.put("trumpHidden", isTrumpHidden)
        root.put("doubleMult", doubleMultiplier)
        root.put("pairDeclared", pairDeclared)
        root.put("tricksPlayed", tricksPlayed)
        root.put("t0Round", teamRoundPoints[0])
        root.put("t1Round", teamRoundPoints[1])
        root.put("t0Match", teamMatchScores[0])
        root.put("t1Match", teamMatchScores[1])
        root.put("names", JSONArray(names.toList()))
        root.put("hands", JSONArray(hands.map { h -> JSONArray(h.map { it.id }) }))
        root.put("reserves", JSONArray(reserveCards.map { h -> JSONArray(h.map { it.id }) }))
        root.put("trick", JSONArray(currentTrick.map { JSONObject().put("s", it.seatIndex).put("c", it.card.id) }))
        root.put("passed", JSONArray(passedSeats.toList()))
        return root.toString()
    }

    override fun restoreState(json: String): UniversalTableState? {
        return try {
            val r = JSONObject(json)
            ruleProfileId = r.optString("profile", "classic_dhaka")
            difficulty = AiDifficulty.fromLevel(r.optInt("diff", 3))
            phase = TablePhase.valueOf(r.optString("phase", TablePhase.BIDDING.name))
            roundNumber = r.optInt("round", 1)
            dealerSeat = r.optInt("dealer", 0)
            activeSeat = r.optInt("active", 0)
            highestBid = r.optInt("highestBid", 15)
            effectiveBidTarget = r.optInt("effectiveBid", 16)
            bidWinnerSeat = r.optInt("bidWinner", -1).takeIf { it >= 0 }
            trumpSuit = r.optString("trump", "").takeIf { it.isNotEmpty() }?.let { Suit.fromCode(it) }
            isTrumpHidden = r.optBoolean("trumpHidden", true)
            doubleMultiplier = r.optInt("doubleMult", 1)
            pairDeclared = r.optBoolean("pairDeclared", false)
            tricksPlayed = r.optInt("tricksPlayed", 0)
            teamRoundPoints = intArrayOf(r.optInt("t0Round", 0), r.optInt("t1Round", 0))
            teamMatchScores = intArrayOf(r.optInt("t0Match", 0), r.optInt("t1Match", 0))
            val nArr = r.optJSONArray("names")
            if (nArr != null && nArr.length() == 4) {
                for (i in 0..3) names[i] = nArr.getString(i)
            }
            val hArr = r.getJSONArray("hands")
            val resArr = r.getJSONArray("reserves")
            for (i in 0..3) {
                hands[i] = mutableListOf()
                val sub = hArr.getJSONArray(i)
                for (j in 0 until sub.length()) Card.fromId(sub.getString(j))?.let { hands[i].add(it) }
                reserveCards[i] = mutableListOf()
                val subR = resArr.getJSONArray(i)
                for (j in 0 until subR.length()) Card.fromId(subR.getString(j))?.let { reserveCards[i].add(it) }
            }
            currentTrick.clear()
            val tArr = r.getJSONArray("trick")
            for (i in 0 until tArr.length()) {
                val obj = tArr.getJSONObject(i)
                Card.fromId(obj.getString("c"))?.let { currentTrick.add(PlayedTableCard(obj.getInt("s"), it)) }
            }
            val pArr = r.optJSONArray("passed")
            if (pArr != null && pArr.length() == 4) {
                for (i in 0..3) passedSeats[i] = pArr.getBoolean(i)
            }
            bannerEn = "Restored Twenty-Nine — Round $roundNumber"
            bannerBn = "২৯ খেলা পুনরুদ্ধার করা হয়েছে — রাউন্ড $roundNumber"
            getState()
        } catch (_: Throwable) {
            null
        }
    }
}

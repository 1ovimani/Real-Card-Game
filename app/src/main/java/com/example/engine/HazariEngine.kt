package com.example.engine

import com.example.core.audio.CardSfx
import com.example.core.model.AiDifficulty
import com.example.core.model.Card
import com.example.core.model.DeckFactory
import com.example.core.model.DeckType
import com.example.core.model.GameCatalog
import com.example.core.model.Rank
import org.json.JSONArray
import org.json.JSONObject

/**
 * Authentic Bangladeshi Hazari (1000) Game Engine.
 * - 4 Players, 52-Card Deck, 13 Cards each
 * - 4 Groups per player: [3 cards, 3 cards, 3 cards, 4 cards]
 * - Strict Descending Strength Rule: Group 1 >= Group 2 >= Group 3
 * - Hand Hierarchy: Troy (6) > Color Run (5) > Run (4) > Color (3) > Pair (2) > Indi (1)
 *   with authentic A-K-Q > A-2-3 > K-Q-J sequence ordering!
 * - Scoring: 10, J, Q, K, A = 10 pts; 2..9 = 5 pts (360 pts per deal). First to 1000 wins!
 */
class HazariEngine : CardGameEngine {
    override val gameId: String = GameCatalog.GAME_HAZARI

    private var ruleProfileId = "standard_1000"
    private var difficulty = AiDifficulty.NORMAL
    private var names = arrayOf("You", "Nabil", "Rafi", "Sadia")
    private val targetScore: Int get() = if (ruleProfileId == "quick_500") 500 else 1000

    private var phase = TablePhase.GROUP_ARRANGING
    private var roundNumber = 1
    private var dealerSeat = 0
    private var totalScores = IntArray(4)
    private var roundScores = IntArray(4)

    // 13 cards per seat ordered as [0..2]=Group1, [3..5]=Group2, [6..8]=Group3, [9..12]=Group4
    private var seatHands = Array(4) { mutableListOf<Card>() }
    private var revealStage = -1 // -1 while arranging, 0..3 during 4-stage showdown
    private var lastStageWinner: Int? = null
    private var lastStageCards = mapOf<Int, List<Card>>()
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
        this.names = arrayOf(playerName.ifBlank { "You" }, "Nabil", "Rafi", "Sadia")
        this.roundNumber = 1
        this.dealerSeat = 0
        this.totalScores = IntArray(4)
        this.history.clear()
        dealNewRound()
        return getState()
    }

    private fun dealNewRound() {
        val deck = DeckFactory.shuffledDeck(DeckType.STANDARD_52)
        roundScores = IntArray(4)
        revealStage = -1
        lastStageWinner = null
        lastStageCards = emptyMap()
        roundSummaryEn = ""
        roundSummaryBn = ""

        for (s in 0..3) {
            val raw = deck.subList(s * 13, (s + 1) * 13)
            seatHands[s] = smartArrange13Cards(raw).toMutableList()
        }
        phase = TablePhase.GROUP_ARRANGING
        bannerEn = "Round $roundNumber: Tap two cards to swap sets or tap Lock Groups to showdown!"
        bannerBn = "রাউন্ড $roundNumber: দুটি তাসে ট্যাপ করে অদলবদল করুন অথবা সেট নিশ্চিত করুন!"
    }

    override fun getState(): UniversalTableState {
        val humanGroups = buildGroupInfos(seatHands[0])
        val validOrder = isValidHazariArrangement(seatHands[0])
        val seats = (0..3).map { i ->
            SeatSnapshot(
                seatIndex = i,
                name = names[i],
                cardCount = if (phase == TablePhase.GROUP_ARRANGING) 13 else (13 - ((revealStage + 1) * 3).coerceAtMost(13)),
                totalScore = totalScores[i].toDouble(),
                roundScore = roundScores[i],
                isDealer = dealerSeat == i,
                isActiveTurn = (phase == TablePhase.GROUP_ARRANGING && i == 0) || (lastStageWinner == i),
                statusBadge = if (lastStageWinner == i) "+Pts" else "",
                revealedCards = lastStageCards[i].orEmpty()
            )
        }
        val winnerIsHuman = if (phase == TablePhase.MATCH_OVER) {
            val best = totalScores.maxOrNull() ?: 0
            totalScores[0] >= best
        } else null

        return UniversalTableState(
            gameId = gameId,
            ruleProfileId = ruleProfileId,
            difficulty = difficulty,
            phase = phase,
            roundNumber = roundNumber,
            activeSeat = if (phase == TablePhase.GROUP_ARRANGING) 0 else (lastStageWinner ?: 0),
            dealerSeat = dealerSeat,
            seats = seats,
            humanHand = seatHands[0].toList(),
            hazariData = HazariTableData(
                groups = humanGroups,
                isValidOrder = validOrder,
                stageIndex = revealStage,
                stageWinnerSeat = lastStageWinner,
                stageCardsBySeat = lastStageCards,
                targetScore = targetScore
            ),
            bannerMessageEn = bannerEn,
            bannerMessageBn = bannerBn,
            roundResultSummaryEn = roundSummaryEn,
            roundResultSummaryBn = roundSummaryBn,
            matchWinnerIsHuman = winnerIsHuman,
            roundHistory = history.toList()
        )
    }

    override fun applyMove(move: GameMove): MoveResult {
        return when (move) {
            is GameMove.HazariSwap -> {
                if (phase != TablePhase.GROUP_ARRANGING) return MoveResult(false, getState())
                val h = seatHands[0]
                if (move.idxA in h.indices && move.idxB in h.indices && move.idxA != move.idxB) {
                    val tmp = h[move.idxA]
                    h[move.idxA] = h[move.idxB]
                    h[move.idxB] = tmp
                    return MoveResult(true, getState(), CardSfx.CARD_FLIP)
                }
                MoveResult(false, getState())
            }
            is GameMove.HazariAutoArrange -> {
                if (phase != TablePhase.GROUP_ARRANGING) return MoveResult(false, getState())
                seatHands[0] = smartArrange13Cards(seatHands[0]).toMutableList()
                bannerEn = "Auto-arranged into optimal valid Hazari groups!"
                bannerBn = "সেরা বৈধ হাজারী সেটে সাজানো হয়েছে!"
                MoveResult(true, getState(), CardSfx.CARD_SHUFFLE)
            }
            is GameMove.HazariConfirmGroups -> {
                if (phase != TablePhase.GROUP_ARRANGING) return MoveResult(false, getState())
                if (!isValidHazariArrangement(seatHands[0])) {
                    bannerEn = "Invalid order: Group 1 must be ≥ Group 2 ≥ Group 3!"
                    bannerBn = "ভুল ক্রম: ১ম সেট ≥ ২য় সেট ≥ ৩য় সেট হতে হবে!"
                    return MoveResult(false, getState(), CardSfx.UI_CLICK, errorReason = "Group strength must descend")
                }
                phase = TablePhase.PLAYING
                revealStage = -1
                return advanceShowdownStage()
            }
            is GameMove.ContinueNormal -> {
                if (phase == TablePhase.PLAYING) advanceShowdownStage() else MoveResult(false, getState())
            }
            is GameMove.NextRound -> {
                if (phase == TablePhase.MATCH_OVER) {
                    totalScores = IntArray(4)
                    roundNumber = 1
                    history.clear()
                } else {
                    roundNumber++
                    dealerSeat = (dealerSeat + 1) % 4
                }
                dealNewRound()
                MoveResult(true, getState(), CardSfx.CARD_SHUFFLE)
            }
            else -> MoveResult(false, getState())
        }
    }

    private fun advanceShowdownStage(): MoveResult {
        revealStage++
        if (revealStage <= 2) {
            val start = revealStage * 3
            val stageMap = (0..3).associateWith { s -> seatHands[s].subList(start, start + 3).toList() }
            lastStageCards = stageMap
            val winner = (0..3).maxWith(Comparator { a, b ->
                evaluate3CardHand(stageMap[a]!!).compareTo(evaluate3CardHand(stageMap[b]!!))
            })
            val pts = stageMap.values.flatten().sumOf { it.hazariPoints }
            roundScores[winner] += pts
            lastStageWinner = winner
            val winEval = evaluate3CardHand(stageMap[winner]!!)
            bannerEn = "Set ${revealStage + 1}: ${names[winner]} wins with ${winEval.nameEn} (+$pts pts)!"
            bannerBn = "সেট ${revealStage + 1}: ${names[winner]} ${winEval.nameBn} দিয়ে জিতেছে (+$pts পয়েন্ট)!"
            return MoveResult(true, getState(), CardSfx.TRICK_WIN)
        } else {
            // Stage 3: Final 4-card closing throw (highest single cards determine capture of 4 tricks)
            val stageMap = (0..3).associateWith { s ->
                seatHands[s].subList(9, 13).sortedByDescending { it.rank.value }
            }
            lastStageCards = stageMap
            val stagePtsBySeat = IntArray(4)
            for (k in 0..3) {
                val winnerK = (0..3).maxByOrNull { s -> stageMap[s]!![k].rank.value } ?: 0
                val trickPts = (0..3).sumOf { s -> stageMap[s]!![k].hazariPoints }
                stagePtsBySeat[winnerK] += trickPts
                roundScores[winnerK] += trickPts
            }
            val topClosing = (0..3).maxByOrNull { stagePtsBySeat[it] } ?: 0
            lastStageWinner = topClosing

            for (s in 0..3) totalScores[s] += roundScores[s]
            val roundTop = (0..3).maxByOrNull { roundScores[it] } ?: 0
            roundSummaryEn = "Round $roundNumber: You +${roundScores[0]} (Total ${totalScores[0]}/$targetScore) | Leader: ${names[totalScores.indices.maxByOrNull { totalScores[it] } ?: 0]} (${totalScores.maxOrNull()})"
            roundSummaryBn = "রাউন্ড $roundNumber: আপনি +${roundScores[0]} (মোট ${totalScores[0]}/$targetScore) | শীর্ষে: ${names[totalScores.indices.maxByOrNull { totalScores[it] } ?: 0]} (${totalScores.maxOrNull()})"
            history.add(RoundHistoryEntry(roundNumber, roundSummaryEn, roundSummaryBn))

            val matchOver = totalScores.any { it >= targetScore }
            phase = if (matchOver) TablePhase.MATCH_OVER else TablePhase.ROUND_OVER
            bannerEn = "Set 4 complete! ${names[roundTop]} led the round with ${roundScores[roundTop]} pts."
            bannerBn = "৪র্থ সেট শেষ! ${names[roundTop]} এই রাউন্ডে সর্বোচ্চ ${roundScores[roundTop]} পয়েন্ট তুলেছে।"
            val sfx = if (matchOver) {
                if (totalScores[0] >= (totalScores.maxOrNull() ?: 0)) CardSfx.MATCH_WIN else CardSfx.MATCH_LOSS
            } else CardSfx.ROUND_WIN
            return MoveResult(true, getState(), sfx)
        }
    }

    override fun computeAiMove(): GameMove? {
        // Auto-advance reveal stages cleanly during Playing phase
        return if (phase == TablePhase.PLAYING) GameMove.ContinueNormal else null
    }

    private fun buildGroupInfos(hand: List<Card>): List<HazariGroupInfo> {
        if (hand.size < 13) return emptyList()
        val g1 = hand.subList(0, 3)
        val g2 = hand.subList(3, 6)
        val g3 = hand.subList(6, 9)
        val g4 = hand.subList(9, 13)
        val e1 = evaluate3CardHand(g1)
        val e2 = evaluate3CardHand(g2)
        val e3 = evaluate3CardHand(g3)
        return listOf(
            HazariGroupInfo(0, g1, e1.nameEn, e1.nameBn, g1.sumOf { it.hazariPoints }),
            HazariGroupInfo(1, g2, e2.nameEn, e2.nameBn, g2.sumOf { it.hazariPoints }),
            HazariGroupInfo(2, g3, e3.nameEn, e3.nameBn, g3.sumOf { it.hazariPoints }),
            HazariGroupInfo(3, g4, "4-Card Closing", "৪-তাস শেষ সেট", g4.sumOf { it.hazariPoints })
        )
    }

    companion object {
        data class HandEval(
            val category: Int, // 6=Troy, 5=ColorRun, 4=Run, 3=Color, 2=Pair, 1=Indi
            val tieBreakers: List<Int>,
            val nameEn: String,
            val nameBn: String
        ) : Comparable<HandEval> {
            override fun compareTo(other: HandEval): Int {
                if (category != other.category) return category.compareTo(other.category)
                for (i in 0 until minOf(tieBreakers.size, other.tieBreakers.size)) {
                    if (tieBreakers[i] != other.tieBreakers[i]) return tieBreakers[i].compareTo(other.tieBreakers[i])
                }
                return 0
            }
        }

        /** Authentic Bangladeshi Hazari & Teen Patti 3-card evaluator (A-K-Q highest, A-2-3 2nd highest!) */
        fun evaluate3CardHand(cards: List<Card>): HandEval {
            require(cards.size == 3)
            val sorted = cards.sortedByDescending { it.rank.value }
            val r0 = sorted[0].rank.value
            val r1 = sorted[1].rank.value
            val r2 = sorted[2].rank.value
            val sameSuit = sorted[0].suit == sorted[1].suit && sorted[1].suit == sorted[2].suit

            // 1. Troy (Three of a Kind)
            if (r0 == r1 && r1 == r2) {
                return HandEval(6, listOf(r0), "Troy (${sorted[0].rank.label}s)", "ত্রয়ী (${sorted[0].rank.label})")
            }

            // Check Sequence: normal consecutive OR A-2-3 (14, 3, 2)
            val isNormalSeq = (r0 == r1 + 1 && r1 == r2 + 1)
            val isA23 = (r0 == 14 && r1 == 3 && r2 == 2)
            val isSeq = isNormalSeq || isA23
            // Sequence strength: A-K-Q -> 15, A-2-3 -> 14, K-Q-J -> 13 ... 4-3-2 -> 4
            val seqRank = when {
                r0 == 14 && r1 == 13 && r2 == 12 -> 15
                isA23 -> 14
                else -> r0
            }

            // 2. Color Run (Pure Sequence / Straight Flush)
            if (sameSuit && isSeq) {
                return HandEval(5, listOf(seqRank), "Color Run", "রঙিন রান")
            }
            // 3. Run (Sequence / Straight)
            if (isSeq) {
                return HandEval(4, listOf(seqRank), "Run", "রান")
            }
            // 4. Color (Flush)
            if (sameSuit) {
                return HandEval(3, listOf(r0, r1, r2), "Color", "রং")
            }
            // 5. Pair
            if (r0 == r1 || r1 == r2) {
                val pairVal = if (r0 == r1) r0 else r1
                val kicker = if (r0 == r1) r2 else r0
                return HandEval(2, listOf(pairVal, kicker), "Pair", "জোড়া")
            }
            // 6. Indi (High Card)
            return HandEval(1, listOf(r0, r1, r2), "Indi (${sorted[0].rank.label})", "ইন্ডি (${sorted[0].rank.label})")
        }

        fun isValidHazariArrangement(hand: List<Card>): Boolean {
            if (hand.size != 13) return false
            val e1 = evaluate3CardHand(hand.subList(0, 3))
            val e2 = evaluate3CardHand(hand.subList(3, 6))
            val e3 = evaluate3CardHand(hand.subList(6, 9))
            return e1 >= e2 && e2 >= e3
        }

        /** Greedily extracts top 3 strongest disjoint 3-card hands so Group1 >= Group2 >= Group3 is guaranteed */
        fun smartArrange13Cards(cards: List<Card>): List<Card> {
            val pool = cards.toMutableList()
            val groups = mutableListOf<List<Card>>()
            repeat(3) {
                var bestTriple: List<Card>? = null
                var bestEval: HandEval? = null
                val n = pool.size
                for (i in 0 until n - 2) {
                    for (j in i + 1 until n - 1) {
                        for (k in j + 1 until n) {
                            val triple = listOf(pool[i], pool[j], pool[k])
                            val ev = evaluate3CardHand(triple)
                            if (bestEval == null || ev > bestEval!!) {
                                bestEval = ev
                                bestTriple = triple
                            }
                        }
                    }
                }
                val chosen = bestTriple!!
                groups.add(chosen.sortedByDescending { it.rank.value })
                pool.removeAll(chosen.toSet())
            }
            groups.sortByDescending { evaluate3CardHand(it) }
            val closing4 = pool.sortedByDescending { it.rank.value }
            return groups[0] + groups[1] + groups[2] + closing4
        }
    }

    override fun serializeState(): String {
        val root = JSONObject()
        root.put("profile", ruleProfileId)
        root.put("diff", difficulty.level)
        root.put("phase", phase.name)
        root.put("round", roundNumber)
        root.put("dealer", dealerSeat)
        root.put("stage", revealStage)
        root.put("totals", JSONArray(totalScores.toList()))
        root.put("rounds", JSONArray(roundScores.toList()))
        root.put("hands", JSONArray(seatHands.map { h -> JSONArray(h.map { it.id }) }))
        return root.toString()
    }

    override fun restoreState(json: String): UniversalTableState? {
        return try {
            val r = JSONObject(json)
            ruleProfileId = r.optString("profile", "standard_1000")
            difficulty = AiDifficulty.fromLevel(r.optInt("diff", 3))
            phase = TablePhase.valueOf(r.optString("phase", TablePhase.GROUP_ARRANGING.name))
            roundNumber = r.optInt("round", 1)
            dealerSeat = r.optInt("dealer", 0)
            revealStage = r.optInt("stage", -1)
            val tot = r.getJSONArray("totals")
            val rnd = r.getJSONArray("rounds")
            val hArr = r.getJSONArray("hands")
            for (i in 0..3) {
                totalScores[i] = tot.getInt(i)
                roundScores[i] = rnd.getInt(i)
                seatHands[i] = mutableListOf()
                val sub = hArr.getJSONArray(i)
                for (j in 0 until sub.length()) Card.fromId(sub.getString(j))?.let { seatHands[i].add(it) }
            }
            bannerEn = "Restored Hazari — Round $roundNumber"
            bannerBn = "হাজারী পুনরুদ্ধার করা হয়েছে — রাউন্ড $roundNumber"
            getState()
        } catch (_: Throwable) {
            null
        }
    }
}

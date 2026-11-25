package com.example

import com.example.core.model.AiDifficulty
import com.example.core.model.Card
import com.example.core.model.DeckFactory
import com.example.core.model.DeckType
import com.example.core.model.GameCatalog
import com.example.core.model.Rank
import com.example.core.model.Suit
import com.example.engine.BlackjackEngine
import com.example.engine.CallBridgeEngine
import com.example.engine.EngineFactory
import com.example.engine.GameMove
import com.example.engine.HazariEngine
import com.example.engine.KlondikeSolitaireEngine
import com.example.engine.TablePhase
import com.example.engine.TwentyNineEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GameEnginesUnitTest {

    @Test
    fun twentyNine_deckHas32CardsAnd28CardPointsPlus1LastTrickEquals29() {
        val deck = DeckFactory.createDeck(DeckType.PIQUET_32)
        assertEquals(32, deck.size)
        val totalCardPoints = deck.sumOf { it.twentyNinePoints }
        assertEquals(28, totalCardPoints) // + 1 last trick point = 29!
    }

    @Test
    fun twentyNine_biddingDeals4Then8AndCompletesFullRound() {
        val engine = TwentyNineEngine()
        var state = engine.initializeGame("classic_dhaka", AiDifficulty.NORMAL, "Tester")
        assertEquals(TablePhase.BIDDING, state.phase)
        assertEquals(4, state.humanHand.size)

        // Drive bidding & trump selection to playing phase
        var safety = 0
        while (state.phase != TablePhase.PLAYING && safety++ < 40) {
            val move = if (state.activeSeat == 0) {
                when (state.phase) {
                    TablePhase.BIDDING -> if (state.bidOptions.isNotEmpty()) GameMove.PlaceBid(state.bidOptions.first()) else GameMove.PlaceBid(-1)
                    TablePhase.SELECT_TRUMP -> GameMove.SelectTrump(Suit.SPADES)
                    TablePhase.DOUBLE_DECISION -> GameMove.ContinueNormal
                    else -> break
                }
            } else engine.computeAiMove() ?: break
            val res = engine.applyMove(move)
            assertTrue(res.accepted)
            state = res.state
        }

        assertEquals(TablePhase.PLAYING, state.phase)
        assertEquals(8, state.humanHand.size)
        assertTrue(state.isTrumpHidden)
        assertNotNull(state.trumpSuit)

        // Play all 8 tricks (32 plays)
        var plays = 0
        while (state.phase == TablePhase.PLAYING && plays++ < 60) {
            val move = if (state.activeSeat == 0) {
                val cardId = state.playableCardIds.first()
                GameMove.PlayCard(cardId)
            } else {
                engine.computeAiMove()!!
            }
            val res = engine.applyMove(move)
            assertTrue("Move rejected: ${res.errorReason}", res.accepted)
            state = res.state
        }

        assertTrue(state.phase == TablePhase.ROUND_OVER || state.phase == TablePhase.MATCH_OVER)
        val totalRoundPts = (state.teamRoundPoints?.first ?: 0) + (state.teamRoundPoints?.second ?: 0)
        assertEquals(29, totalRoundPts)
    }

    @Test
    fun hazari_deckPointsEqual360AndHandRankingIsAuthentic() {
        val deck = DeckFactory.createDeck(DeckType.STANDARD_52)
        assertEquals(52, deck.size)
        assertEquals(360, deck.sumOf { it.hazariPoints })

        val troy = HazariEngine.evaluate3CardHand(listOf(Card(Suit.SPADES, Rank.SEVEN), Card(Suit.HEARTS, Rank.SEVEN), Card(Suit.CLUBS, Rank.SEVEN)))
        val colorRunAKQ = HazariEngine.evaluate3CardHand(listOf(Card(Suit.HEARTS, Rank.ACE), Card(Suit.HEARTS, Rank.KING), Card(Suit.HEARTS, Rank.QUEEN)))
        val colorRunA23 = HazariEngine.evaluate3CardHand(listOf(Card(Suit.SPADES, Rank.ACE), Card(Suit.SPADES, Rank.TWO), Card(Suit.SPADES, Rank.THREE)))
        val colorRunKQJ = HazariEngine.evaluate3CardHand(listOf(Card(Suit.DIAMONDS, Rank.KING), Card(Suit.DIAMONDS, Rank.QUEEN), Card(Suit.DIAMONDS, Rank.JACK)))
        val run = HazariEngine.evaluate3CardHand(listOf(Card(Suit.SPADES, Rank.FIVE), Card(Suit.HEARTS, Rank.SIX), Card(Suit.CLUBS, Rank.SEVEN)))
        val color = HazariEngine.evaluate3CardHand(listOf(Card(Suit.CLUBS, Rank.ACE), Card(Suit.CLUBS, Rank.NINE), Card(Suit.CLUBS, Rank.FOUR)))
        val pair = HazariEngine.evaluate3CardHand(listOf(Card(Suit.SPADES, Rank.KING), Card(Suit.HEARTS, Rank.KING), Card(Suit.CLUBS, Rank.TWO)))
        val indi = HazariEngine.evaluate3CardHand(listOf(Card(Suit.SPADES, Rank.ACE), Card(Suit.HEARTS, Rank.TEN), Card(Suit.CLUBS, Rank.THREE)))

        assertTrue(troy > colorRunAKQ)
        assertTrue(colorRunAKQ > colorRunA23)
        assertTrue(colorRunA23 > colorRunKQJ)
        assertTrue(colorRunKQJ > run)
        assertTrue(run > color)
        assertTrue(color > pair)
        assertTrue(pair > indi)
    }

    @Test
    fun hazari_smartArrangeProducesValidDescendingSetsAndFullShowdownScores360() {
        val engine = HazariEngine()
        var state = engine.initializeGame("standard_1000", AiDifficulty.HARD, "Tester")
        assertEquals(TablePhase.GROUP_ARRANGING, state.phase)
        assertTrue(state.hazariData!!.isValidOrder)

        // Lock groups & step through all 4 showdown stages
        state = engine.applyMove(GameMove.HazariConfirmGroups).state
        var steps = 0
        while (state.phase == TablePhase.PLAYING && steps++ < 10) {
            state = engine.applyMove(GameMove.ContinueNormal).state
        }
        assertTrue(state.phase == TablePhase.ROUND_OVER || state.phase == TablePhase.MATCH_OVER)
        assertEquals(360, state.seats.sumOf { it.roundScore })
    }

    @Test
    fun callBridge_completesRoundWith13TricksAndValidScores() {
        val engine = CallBridgeEngine(isCallBreak = false)
        var state = engine.initializeGame("bd_call_bridge_5r", AiDifficulty.NORMAL, "Tester")
        var safety = 0
        while (state.phase == TablePhase.BIDDING && safety++ < 10) {
            val move = if (state.activeSeat == 0) GameMove.PlaceBid(3) else engine.computeAiMove()!!
            state = engine.applyMove(move).state
        }
        assertEquals(TablePhase.PLAYING, state.phase)
        var plays = 0
        while (state.phase == TablePhase.PLAYING && plays++ < 60) {
            val move = if (state.activeSeat == 0) GameMove.PlayCard(state.playableCardIds.first()) else engine.computeAiMove()!!
            val res = engine.applyMove(move)
            assertTrue(res.accepted)
            state = res.state
        }
        assertEquals(TablePhase.ROUND_OVER, state.phase)
        assertEquals(13, state.seats.sumOf { it.tricksWon })
    }

    @Test
    fun all12CatalogGamesInitializeAndSerializeCleanly() {
        assertEquals(12, GameCatalog.allGames.size)
        for (info in GameCatalog.allGames) {
            val eng = EngineFactory.create(info.id)
            val st = eng.initializeGame(info.ruleProfiles.first().id, AiDifficulty.NORMAL, "Player")
            assertEquals(info.id, st.gameId)
            val saved = eng.serializeState()
            assertFalse(saved.isBlank())
            val restored = EngineFactory.create(info.id).restoreState(saved)
            assertNotNull("Failed to restore ${info.id}", restored)
        }
    }

    @Test
    fun blackjackAndSolitaireBasicPlayMovesWork() {
        val bj = BlackjackEngine()
        var bState = bj.initializeGame("classic_21", AiDifficulty.NORMAL, "Tester", 5000L)
        if (bState.phase == TablePhase.PLAYING) {
            bState = bj.applyMove(GameMove.BlackjackStand).state
        }
        assertEquals(TablePhase.ROUND_OVER, bState.phase)

        val sol = KlondikeSolitaireEngine()
        var sState = sol.initializeGame("draw_1", AiDifficulty.NORMAL, "Tester")
        assertEquals(24, sState.solitaireData!!.stockCount)
        sState = sol.applyMove(GameMove.SolitaireDrawStock).state
        val afterDraw = sState.solitaireData!!
        assertEquals(23, afterDraw.stockCount)
        assertTrue(afterDraw.canUndo)
    }
}

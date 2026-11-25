package com.example.core.model

import kotlin.random.Random

enum class Suit(val symbol: String, val code: String, val nameEn: String, val nameBn: String, val isRed: Boolean) {
    SPADES("♠", "S", "Spades", "ইস্কাবন", false),
    HEARTS("♥", "H", "Hearts", "হরতন", true),
    DIAMONDS("♦", "D", "Diamonds", "রুইতন", true),
    CLUBS("♣", "C", "Clubs", "চিড়িতন", false);

    companion object {
        fun fromCode(code: String): Suit = entries.firstOrNull { it.code == code } ?: SPADES
    }
}

enum class Rank(val value: Int, val label: String, val nameEn: String, val nameBn: String) {
    TWO(2, "2", "Two", "দুই"),
    THREE(3, "3", "Three", "তিন"),
    FOUR(4, "4", "Four", "চার"),
    FIVE(5, "5", "Five", "পাঁচ"),
    SIX(6, "6", "Six", "ছয়"),
    SEVEN(7, "7", "Seven", "সাত"),
    EIGHT(8, "8", "Eight", "আট"),
    NINE(9, "9", "Nine", "নয়"),
    TEN(10, "10", "Ten", "দশ"),
    JACK(11, "J", "Jack", "গোলাম"),
    QUEEN(12, "Q", "Queen", "বিবি"),
    KING(13, "K", "King", "সাহেব"),
    ACE(14, "A", "Ace", "টেক্কা");

    companion object {
        fun fromValue(v: Int): Rank = entries.firstOrNull { it.value == v } ?: ACE
    }
}

data class Card(
    val suit: Suit,
    val rank: Rank,
    val deckIndex: Int = 0
) {
    val id: String get() = "${rank.label}${suit.code}_$deckIndex"

    val shortCode: String get() = "${rank.label}${suit.code}"

    /** Authentic Twenty-Nine (29) point values: J=3, 9=2, A=1, 10=1, others=0 */
    val twentyNinePoints: Int
        get() = when (rank) {
            Rank.JACK -> 3
            Rank.NINE -> 2
            Rank.ACE -> 1
            Rank.TEN -> 1
            else -> 0
        }

    /** Authentic Twenty-Nine (29) rank strength: J(7) > 9(6) > A(5) > 10(4) > K(3) > Q(2) > 8(1) > 7(0) */
    val twentyNineOrder: Int
        get() = when (rank) {
            Rank.JACK -> 7
            Rank.NINE -> 6
            Rank.ACE -> 5
            Rank.TEN -> 4
            Rank.KING -> 3
            Rank.QUEEN -> 2
            Rank.EIGHT -> 1
            Rank.SEVEN -> 0
            else -> -1
        }

    /** Authentic Bangladeshi Hazari (1000) point values: 10,J,Q,K,A = 10 pts; 2..9 = 5 pts */
    val hazariPoints: Int
        get() = if (rank.value >= 10) 10 else 5

    /** Blackjack numeric value (Ace counted as 11 initially, reduced to 1 as needed) */
    val blackjackValue: Int
        get() = when (rank) {
            Rank.ACE -> 11
            Rank.KING, Rank.QUEEN, Rank.JACK -> 10
            else -> rank.value
        }

    companion object {
        fun fromId(id: String): Card? {
            val parts = id.split("_")
            if (parts.isEmpty()) return null
            val raw = parts[0]
            if (raw.length < 2) return null
            val suitCode = raw.takeLast(1)
            val rankLabel = raw.dropLast(1)
            val suit = Suit.entries.firstOrNull { it.code == suitCode } ?: return null
            val rank = Rank.entries.firstOrNull { it.label == rankLabel } ?: return null
            val idx = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return Card(suit, rank, idx)
        }
    }
}

enum class DeckType(val size: Int) {
    STANDARD_52(52),
    PIQUET_32(32),     // 7..A (Twenty-Nine, Skat)
    RUSSIAN_36(36),    // 6..A (Durak)
    ITALIAN_40(40),    // A..7, J, Q, K (Briscola, Scopa)
    PINOCHLE_48(48),   // Double 9..A
    DOUBLE_104(104)    // Double 52-card deck
}

object DeckFactory {
    fun createDeck(type: DeckType): List<Card> = when (type) {
        DeckType.STANDARD_52 -> Suit.entries.flatMap { s -> Rank.entries.map { r -> Card(s, r, 0) } }
        DeckType.PIQUET_32 -> {
            val ranks = listOf(Rank.SEVEN, Rank.EIGHT, Rank.NINE, Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING, Rank.ACE)
            Suit.entries.flatMap { s -> ranks.map { r -> Card(s, r, 0) } }
        }
        DeckType.RUSSIAN_36 -> {
            val ranks = Rank.entries.filter { it.value >= 6 }
            Suit.entries.flatMap { s -> ranks.map { r -> Card(s, r, 0) } }
        }
        DeckType.ITALIAN_40 -> {
            val ranks = Rank.entries.filter { it.value <= 7 || it.value >= 11 }
            Suit.entries.flatMap { s -> ranks.map { r -> Card(s, r, 0) } }
        }
        DeckType.PINOCHLE_48 -> {
            val ranks = Rank.entries.filter { it.value >= 9 }
            (0..1).flatMap { copy -> Suit.entries.flatMap { s -> ranks.map { r -> Card(s, r, copy) } } }
        }
        DeckType.DOUBLE_104 -> {
            (0..1).flatMap { copy -> Suit.entries.flatMap { s -> Rank.entries.map { r -> Card(s, r, copy) } } }
        }
    }

    fun shuffledDeck(type: DeckType, random: Random = Random.Default): List<Card> =
        createDeck(type).shuffled(random)
}

enum class AppLanguage(val code: String, val label: String) {
    ENGLISH("en", "English"),
    BANGLA("bn", "বাংলা")
}

enum class AiDifficulty(val level: Int, val nameEn: String, val nameBn: String) {
    BEGINNER(1, "Beginner", "নতুন"),
    EASY(2, "Easy", "সহজ"),
    NORMAL(3, "Normal", "সাধারণ"),
    HARD(4, "Hard", "কঠিন"),
    EXPERT(5, "Expert", "দক্ষ"),
    MASTER(6, "Master", "মাস্টার");

    companion object {
        fun fromLevel(lvl: Int): AiDifficulty = entries.firstOrNull { it.level == lvl } ?: NORMAL
    }
}

enum class GraphicsQuality(val id: String, val nameEn: String, val nameBn: String) {
    LOW("low", "Low (Battery)", "লো (ব্যাটারি সেভার)"),
    MEDIUM("medium", "Medium", "মিডিয়াম"),
    HIGH("high", "High", "হাই"),
    ULTRA("ultra", "Ultra Luxury", "আল্ট্রা লাক্সারি");

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: HIGH
    }
}

enum class CardBackStyle(val id: String, val nameEn: String, val nameBn: String) {
    ROYAL_FILIGREE("royal_filigree", "Royal Lotus Filigree", "রয়েল পদ্ম নকশা"),
    CRIMSON_DAMASK("crimson_damask", "Crimson Damask", "লাল রাজকীয় নকশা"),
    EMERALD_JAMDANI("emerald_jamdani", "Emerald Jamdani", "সবুজ জামদানি নকশা"),
    OBSIDIAN_GOLD("obsidian_gold", "Obsidian Gold", "কালো সোনালি নকশা");

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: ROYAL_FILIGREE
    }
}

enum class MusicMood(val id: String, val nameEn: String, val nameBn: String) {
    CASINO_AMBIENCE("casino", "Luxury Casino Lounge", "লাক্সারি লাউঞ্জ"),
    CLASSIC_CARD_ROOM("card_room", "Classic Card Room", "ক্লাসিক কার্ড রুম"),
    TRADITIONAL_SITAR("traditional", "Bengali Heritage Drone", "ঐতিহ্যবাহী সুর"),
    MINIMAL_AMBIENT("minimal", "Minimal Calm Ambient", "শান্ত পরিবেশ");

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: CASINO_AMBIENCE
    }
}

enum class GameCategory(val id: String, val nameEn: String, val nameBn: String) {
    BANGLADESH_CLASSICS("bangladesh", "Bangladesh Classics", "বাংলাদেশের ক্লাসিক"),
    TRICK_TAKING("trick_taking", "Trick-Taking", "হাত জেতার খেলা"),
    CASINO_POKER("casino", "Casino & Showdown", "ক্যাসিনো ও শোডাউন"),
    SHEDDING("shedding", "Shedding Games", "তাস ছাড়ার খেলা"),
    SOLITAIRE("solitaire", "Solitaire", "সলিটায়ার")
}

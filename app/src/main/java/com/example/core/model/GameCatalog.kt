package com.example.core.model

enum class OriginClassification(val labelEn: String, val labelBn: String) {
    BANGLADESH_HERITAGE("Bangladesh & Bengal Heritage", "বাংলাদেশ ও বাংলার ঐতিহ্য"),
    WIDELY_PLAYED_BD("Widely Played in Bangladesh", "বাংলাদেশে অত্যন্ত জনপ্রিয়"),
    SOUTH_ASIAN_CLASSIC("South Asian Classic", "দক্ষিণ এশীয় ক্লাসিক"),
    INTERNATIONAL_CLASSIC("International Classic", "আন্তর্জাতিক ক্লাসিক")
}

data class RuleProfile(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val descEn: String,
    val descBn: String
)

data class BilingualManual(
    val introEn: String,
    val introBn: String,
    val objectiveEn: String,
    val objectiveBn: String,
    val deckDealEn: String,
    val deckDealBn: String,
    val rulesEn: String,
    val rulesBn: String,
    val scoringEn: String,
    val scoringBn: String,
    val strategyEn: String,
    val strategyBn: String,
    val faqEn: String,
    val faqBn: String
)

data class GameInfo(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val altNames: String,
    val category: GameCategory,
    val isBangladeshClassic: Boolean,
    val origin: OriginClassification,
    val countryTagEn: String,
    val countryTagBn: String,
    val minPlayers: Int,
    val maxPlayers: Int,
    val recommendedPlayers: Int,
    val isTeamGame: Boolean,
    val deckType: DeckType,
    val cardsDealtPerPlayer: Int,
    val supportsLocalMultiplayer: Boolean,
    val ruleProfiles: List<RuleProfile>,
    val sourceReference: String,
    val ruleVersion: String,
    val confidenceLevel: String,
    val manual: BilingualManual
)

object GameCatalog {
    const val GAME_TWENTY_NINE = "twenty_nine"
    const val GAME_HAZARI = "hazari"
    const val GAME_CALL_BRIDGE = "call_bridge"
    const val GAME_CALL_BREAK = "call_break"
    const val GAME_COURT_PIECE = "court_piece"
    const val GAME_TEEN_PATTI = "teen_patti"
    const val GAME_BHABHI = "bhabhi"
    const val GAME_SPADES = "spades"
    const val GAME_HEARTS = "hearts"
    const val GAME_CRAZY_EIGHTS = "crazy_eights"
    const val GAME_BLACKJACK = "blackjack"
    const val GAME_KLONDIKE = "klondike"

    val allGames: List<GameInfo> = listOf(
        GameInfo(
            id = GAME_TWENTY_NINE,
            nameEn = "Twenty-Nine (29)",
            nameBn = "টোয়েন্টি-নাইন (২৯)",
            altNames = "29, 29 Double, 29 Redouble, Pair 29",
            category = GameCategory.BANGLADESH_CLASSICS,
            isBangladeshClassic = true,
            origin = OriginClassification.BANGLADESH_HERITAGE,
            countryTagEn = "Bangladesh & Bengal",
            countryTagBn = "বাংলাদেশ ও বাংলা",
            minPlayers = 4,
            maxPlayers = 4,
            recommendedPlayers = 4,
            isTeamGame = true,
            deckType = DeckType.PIQUET_32,
            cardsDealtPerPlayer = 8,
            supportsLocalMultiplayer = true,
            ruleProfiles = listOf(
                RuleProfile(
                    id = "classic_dhaka",
                    nameEn = "Classic Dhaka 29 (Pair + Double/Redouble)",
                    nameBn = "ক্লাসিক ঢাকা ২৯ (জোড়া + ডাবল/রি-ডাবল)",
                    descEn = "Standard 16–28 bidding, hidden trump, Double/Redouble, and K+Q Trump Pair (±4).",
                    descBn = "১৬–২৮ ডাক, গোপন রং, ডাবল/রি-ডাবল এবং রঙের সাহেব-বিবি জোড়া (±৪)।"
                ),
                RuleProfile(
                    id = "strict_29",
                    nameEn = "Tournament 29 (No Pair)",
                    nameBn = "টুর্নামেন্ট ২৯ (জোড়া ছাড়া)",
                    descEn = "Pure 29 rules with Hidden Trump & Double/Redouble, without Marriage/Pair bonus.",
                    descBn = "গোপন রং ও ডাবল/রি-ডাবলসহ শুদ্ধ ২৯ নিয়ম, জোড়া বোনাস ছাড়া।"
                ),
                RuleProfile(
                    id = "high_stakes_double",
                    nameEn = "29 Double & Redouble Match",
                    nameBn = "২৯ ডাবল ও রি-ডাবল ম্যাচ",
                    descEn = "Fast high-stakes 29 where Double (2×) and Redouble (4×) decide the +6/-6 match rapidly.",
                    descBn = "দ্রুতগতির ২৯ যেখানে ডাবল (২×) ও রি-ডাবল (৪×) দ্রুত ম্যাচের ফলাফল নির্ধারণ করে।"
                )
            ),
            sourceReference = "Pagat.com Jass/29 Archive & Bangladeshi Regional Card Club Conventions",
            ruleVersion = "29-BD-v2.1",
            confidenceLevel = "High (Verified Regional Standard)",
            manual = BilingualManual(
                introEn = "Twenty-Nine (29) is Bangladesh's most iconic 4-player partnership trick-taking card game from the European Jass family, deeply rooted in Dhaka, Chattogram, Sylhet, and Bengal card clubs.",
                introBn = "টোয়েন্টি-নাইন (২৯) বাংলাদেশের সবচেয়ে জনপ্রিয় ৪ জনের দলগত তাস খেলা। দু'জন করে দুটি দল একে অপরের মুখোমুখি বসে খেলে।",
                objectiveEn = "Bid the number of points (16–28) your partnership can capture in 8 tricks, set a hidden trump suit, and meet or exceed your bid to win game points toward +6.",
                objectiveBn = "৮ হাতে আপনার দল কত পয়েন্ট (১৬ থেকে ২৮) তুলতে পারবে তার ডাক দিন, গোপন রং নির্ধারণ করুন এবং ডাক পূরণ করে +৬ গেম পয়েন্টে পৌঁছান।",
                deckDealEn = "32-card deck (7, 8, 9, 10, J, Q, K, A of each suit). 4 cards are dealt first for bidding & hidden trump selection, followed by 4 more cards (8 cards total per player).",
                deckDealBn = "৩২টি তাসের ডেক (৭, ৮, ৯, ১০, গোলাম, বিবি, সাহেব, টেক্কা)। প্রথমে প্রত্যেককে ৪টি করে তাস দেওয়া হয় ডাক ও গোপন রং বাছার জন্য, এরপর আরও ৪টি করে মোট ৮টি তাস দেওয়া হয়।",
                rulesEn = "Card strength: J (3 pts) > 9 (2 pts) > A (1 pt) > 10 (1 pt) > K, Q, 8, 7 (0 pts). Total = 28 card points + 1 point for winning the 8th (last) trick = 29 points. Players must follow the led suit. If void in the led suit, a player may tap 'Reveal Trump' before playing; once revealed, they must play a trump if held. If holding K+Q of trump after reveal, 'Declare Pair' reduces bidder target by 4 (or raises by 4 for defenders).",
                rulesBn = "তাসের মান ও পয়েন্ট: গোলাম/J (৩) > ৯ (২) > টেক্কা/A (১) > ১০ (১) > K, Q, ৮, ৭ (০)। মোট ২৮ পয়েন্ট + শেষ হাতের ১ পয়েন্ট = ২৯ পয়েন্ট। চাল দেওয়া রঙের তাস থাকলে অবশ্যই তা খেলতে হবে। না থাকলে 'রং খুলুন' চেপে গোপন রং প্রকাশ করে কাটতে পারবেন। রং খোলার পর কারো হাতে রঙের সাহেব-বিবি (K+Q) থাকলে 'জোড়া' দেখালে ডাকের লক্ষ্য ±৪ পরিবর্তিত হয়।",
                scoringEn = "Meeting bid earns +1 game point (+2 on Double, +4 on Redouble). Failing deducts the same. First team to +6 (or pushing opponents to -6) wins the match.",
                scoringBn = "ডাক পূরণ করলে +১ গেম পয়েন্ট (ডাবলে +২, রি-ডাবলে +৪)। ব্যর্থ হলে সমপরিমাণ কাটা যাবে। যে দল আগে +৬ পয়েন্টে পৌঁছায় তারা ম্যাচ জিতে নেয়।",
                strategyEn = "Bid 17–19 on a strong J+9 suit with side Aces. Save your 9 of trump to capture an opponent's Jack or protect your own Jack. Count all 8 trumps carefully once revealed.",
                strategyBn = "একই রঙে গোলাম ও ৯ এবং পাশে টেক্কা থাকলে ১৭–১৯ ডাক নিরাপদ। রং খোলার পর মোট কয়টি রঙের তাস পড়েছে তা হিসাব রাখুন এবং নিজের পার্টনারের জয়ের হাতে পয়েন্ট তাস দিন।",
                faqEn = "Q: Why can't I trump before revealing?\nA: Because trump stays hidden until a player who cannot follow suit formally reveals it!\nQ: When can I declare Pair?\nA: On your turn immediately after trump has been revealed, if you still hold both K and Q of trump.",
                faqBn = "প্রশ্ন: রং না খুলে কাটা যাবে কি?\nউত্তর: না, চাল দেওয়া রঙের তাস না থাকলে আগে 'রং খুলুন' চাপতে হবে।\nপ্রশ্ন: জোড়া কখন দেখানো যায়?\nউত্তর: রং প্রকাশ হওয়ার পর আপনার চালের সময় হাতে রঙের K ও Q দুটিই থাকলে।"
            )
        ),
        GameInfo(
            id = GAME_HAZARI,
            nameEn = "Hazari (1000)",
            nameBn = "হাজারী (১০০০)",
            altNames = "1000 Points, Bangladeshi Hazari",
            category = GameCategory.BANGLADESH_CLASSICS,
            isBangladeshClassic = true,
            origin = OriginClassification.BANGLADESH_HERITAGE,
            countryTagEn = "Bangladesh",
            countryTagBn = "বাংলাদেশ",
            minPlayers = 4,
            maxPlayers = 4,
            recommendedPlayers = 4,
            isTeamGame = false,
            deckType = DeckType.STANDARD_52,
            cardsDealtPerPlayer = 13,
            supportsLocalMultiplayer = true,
            ruleProfiles = listOf(
                RuleProfile(
                    id = "standard_1000",
                    nameEn = "Classic 1000 Target",
                    nameBn = "ক্লাসিক ১০০০ পয়েন্ট ম্যাচ",
                    descEn = "Standard 13-card 3-3-3-4 grouping, 10/5 card points (360 per deal), race to 1000.",
                    descBn = "১৩ তাসকে ৩-৩-৩-৪ সেটে সাজিয়ে খেলা, ১০/৫ পয়েন্ট গণনা, প্রথমে ১০০০ পয়েন্টে জয়।"
                ),
                RuleProfile(
                    id = "quick_500",
                    nameEn = "Quick 500 Sprint",
                    nameBn = "দ্রুত ৫০০ পয়েন্ট ম্যাচ",
                    descEn = "Identical authentic Hazari grouping and scoring with a faster 500-point finish.",
                    descBn = "একই হাজারী নিয়ম ও পয়েন্ট গণনা, তবে ৫০০ পয়েন্টেই দ্রুত ম্যাচ সমাপ্ত।"
                )
            ),
            sourceReference = "Pagat.com Hazari Documentation & Traditional Bangladeshi Rules",
            ruleVersion = "HZ-BD-v2.0",
            confidenceLevel = "High (Verified Regional Standard)",
            manual = BilingualManual(
                introEn = "Hazari (literally 'One Thousand') is a celebrated Bangladeshi 4-player card-arranging and comparison game combining Teen Patti hand rankings with trick point capture.",
                introBn = "হাজারী বাংলাদেশের অত্যন্ত জনপ্রিয় ৪ জনের তাস খেলা, যেখানে ১৩টি তাসকে ৪টি শক্তিশালী সেটে সাজিয়ে প্রতিপক্ষের সাথে লড়াই করে ১০০০ পয়েন্ট অর্জন করতে হয়।",
                objectiveEn = "Arrange your 13 cards into three 3-card hands and one 4-card closing hand in descending order of strength to capture the highest card points and reach 1000 points first.",
                objectiveBn = "আপনার ১৩টি তাসকে শক্তি অনুযায়ী বড় থেকে ছোট ক্রমে তিনটি ৩-তাসের সেট এবং একটি ৪-তাসের শেষ সেটে সাজিয়ে সর্বাধিক পয়েন্ট সংগ্রহ করে ১০০০ পয়েন্টে পৌঁছান।",
                deckDealEn = "Standard 52-card deck; all 52 cards are dealt equally — 13 cards to each of the 4 players.",
                deckDealBn = "সম্পূর্ণ ৫২টি তাসের ডেক। ৪ জন খেলোয়াড়কে সমান ১৩টি করে তাস বণ্টন করা হয়।",
                rulesEn = "Hand Rankings (highest to lowest): 1. Troy (Three of a Kind, AAA highest), 2. Color Run (Straight Flush: A-K-Q > A-2-3 > K-Q-J...), 3. Run (Straight), 4. Color (Flush), 5. Pair, 6. Indi (High Card). Your Group 1 MUST be stronger than or equal to Group 2, and Group 2 ≥ Group 3. All 4 players reveal Group 1 simultaneously — the strongest group captures all 12 cards! Repeat for Group 2 and Group 3; Group 4 compares highest single cards.",
                rulesBn = "সেটের শক্তির ক্রম (সর্বোচ্চ থেকে সর্বনিম্ন): ১. ত্রয়ী/Troy (একই মানের ৩টি তাস), ২. রঙিন রান/Color Run (একই রঙের ক্রম: A-K-Q > A-2-3 > K-Q-J...), ৩. রান/Run (ভিন্ন রঙের ক্রম), ৪. রং/Color (একই রঙের ৩ তাস), ৫. জোড়া/Pair, ৬. ইন্ডি/High Card। ১ম সেট ≥ ২য় সেট ≥ ৩য় সেট হতে হবে। প্রতিটি রাউন্ডে সেরা সেট টেবিলের সব তাসের পয়েন্ট জিতে নেয়।",
                scoringEn = "Cards 10, J, Q, K, A are worth 10 points each; cards 2 through 9 are worth 5 points each. Total deck value = 360 points per round. First to 1000 points wins!",
                scoringBn = "১০, J, Q, K, A প্রতিটি তাস ১০ পয়েন্ট করে এবং ২ থেকে ৯ প্রতিটি ৫ পয়েন্ট করে। প্রতি ডিলের মোট পয়েন্ট ৩৬০। যে আগে ১০০০ পয়েন্ট করবেন তিনিই বিজয়ী!",
                strategyEn = "Don't waste all 10-point cards in a weak Group 3! If your Group 1 can't beat a Troy, build two rock-solid mid groups (Color Run + Run/Color) to sweep Rounds 2 & 3.",
                strategyBn = "সব ১০-পয়েন্টের তাস দুর্বল সেটে দেবেন না। শক্তিশালী ১ম ও ২য় সেট গড়ে বড় পয়েন্টের হাতগুলো দখল করুন। Auto-Arrange বাটন ব্যবহার করে সেরা বিন্যাস দেখে নিতে পারেন।",
                faqEn = "Q: Is A-2-3 valid in Hazari?\nA: Yes! A-K-Q is the highest sequence and A-2-3 is the second highest sequence (beating K-Q-J).",
                faqBn = "প্রশ্ন: হাজারীতে কি A-2-3 রান বৈধ?\nউত্তর: হ্যাঁ! A-K-Q সর্বোচ্চ রান এবং A-2-3 দ্বিতীয় সর্বোচ্চ রান হিসেবে গণ্য হয়।"
            )
        ),
        GameInfo(
            id = GAME_CALL_BRIDGE,
            nameEn = "Call Bridge",
            nameBn = "কল ব্রিজ",
            altNames = "Bangladeshi Call Bridge",
            category = GameCategory.BANGLADESH_CLASSICS,
            isBangladeshClassic = true,
            origin = OriginClassification.WIDELY_PLAYED_BD,
            countryTagEn = "Bangladesh & South Asia",
            countryTagBn = "বাংলাদেশ ও দক্ষিণ এশিয়া",
            minPlayers = 4,
            maxPlayers = 4,
            recommendedPlayers = 4,
            isTeamGame = false,
            deckType = DeckType.STANDARD_52,
            cardsDealtPerPlayer = 13,
            supportsLocalMultiplayer = true,
            ruleProfiles = listOf(
                RuleProfile(
                    id = "bd_call_bridge_5r",
                    nameEn = "Standard 5-Round Call Bridge",
                    nameBn = "স্ট্যান্ডার্ড ৫-রাউন্ড কল ব্রিজ",
                    descEn = "Spades permanently trump, minimum call 2, must beat trick leader if possible, +0.1 per overtrick.",
                    descBn = "ইস্কাবন সর্বদা রং, সর্বনিম্ন কল ২, সম্ভব হলে বড় তাস চালতে হবে, অতিরিক্ত হাতে +০.১।"
                )
            ),
            sourceReference = "South Asian Spades/Call Bridge Rule Compendium",
            ruleVersion = "CB-BD-v1.5",
            confidenceLevel = "High (Verified Regional Standard)",
            manual = BilingualManual(
                introEn = "Call Bridge is a fast-paced 4-player individual trick-taking staple across Bangladesh, where Spades (ইস্কাবন) are permanently trump.",
                introBn = "কল ব্রিজ বাংলাদেশের প্রতিটি আড্ডার অতি পরিচিত ৪ জনের তাস খেলা, যেখানে ইস্কাবন (♠) সব সময় রং হিসেবে কাজ করে।",
                objectiveEn = "Accurately call how many tricks (2–8) you will win out of 13, fulfill your call across 5 rounds, and finish with the highest cumulative score.",
                objectiveBn = "১৩ হাতের মধ্যে আপনি কত হাত (২–৮) জিতবেন তা অনুমান করে কল দিন এবং ৫ রাউন্ড শেষে সর্বোচ্চ স্কোর অর্জন করুন।",
                deckDealEn = "52-card deck, 13 cards dealt to each of the 4 players.",
                deckDealBn = "৫২টি তাসের ডেক, ৪ জনকে ১৩টি করে তাস দেওয়া হয়।",
                rulesEn = "Spades are always trump. You MUST follow the led suit, and if you hold a card higher than the current winning card of that suit, you MUST play a higher card (over-play rule). If void in the led suit, you MUST play a winning Spade if able.",
                rulesBn = "ইস্কাবন সর্বদা রং। চাল দেওয়া রঙের তাস থাকলে অবশ্যই খেলতে হবে এবং টেবিলের সর্বোচ্চ তাসের চেয়ে বড় তাস থাকলে বড়টিই দিতে হবে। সেই রং না থাকলে ইস্কাবন দিয়ে কাটতে হবে।",
                scoringEn = "Meeting or exceeding your call gives `call + 0.1 × overtricks`. Missing your call deducts `-call` points.",
                scoringBn = "কল পূরণ করলে `কল + ০.১ × অতিরিক্ত হাত` পয়েন্ট পাবেন। কল পূরণ করতে ব্যর্থ হলে `-কল` পয়েন্ট কাটা যাবে।",
                strategyEn = "Count your top Spades and side Aces conservatively. Over-calling by even 1 trick causes a massive negative swing.",
                strategyBn = "নিশ্চিত টেক্কা, সাহেব ও বড় ইস্কাবন গুনে নিরাপদ কল দিন। ১ হাত কম হলেই পুরো কল মাইনাস হয়ে যায়।",
                faqEn = "Q: Why are some of my cards dimmed?\nA: Call Bridge enforces the strict must-beat rule — only legal moves that follow suit and beat the current high card (when possible) are enabled.",
                faqBn = "প্রশ্ন: কিছু তাস ঝাপসা দেখায় কেন?\nউত্তর: কল ব্রিজের নিয়ম অনুযায়ী বড় তাস থাকলে বড়টিই চালতে হয়, তাই কেবল বৈধ তাসগুলো উজ্জ্বল থাকে।"
            )
        ),
        GameInfo(
            id = GAME_CALL_BREAK,
            nameEn = "Call Break",
            nameBn = "কল ব্রেক",
            altNames = "Lakadi, Ghochi",
            category = GameCategory.BANGLADESH_CLASSICS,
            isBangladeshClassic = true,
            origin = OriginClassification.SOUTH_ASIAN_CLASSIC,
            countryTagEn = "Bangladesh, Nepal & India",
            countryTagBn = "বাংলাদেশ, নেপাল ও ভারত",
            minPlayers = 4,
            maxPlayers = 4,
            recommendedPlayers = 4,
            isTeamGame = false,
            deckType = DeckType.STANDARD_52,
            cardsDealtPerPlayer = 13,
            supportsLocalMultiplayer = true,
            ruleProfiles = listOf(
                RuleProfile(
                    id = "classic_call_break",
                    nameEn = "5-Round Call Break (1–8 Bid + 8-Call Bonus)",
                    nameBn = "৫-রাউন্ড কল ব্রেক (১–৮ কল + ৮-কল বোনাস)",
                    descEn = "Minimum call 1, 8+ call wins 13 points bonus, strict over-trump enforcement.",
                    descBn = "সর্বনিম্ন কল ১, ৮ কল পূরণ করলে ১৩ পয়েন্ট বোনাস, কঠোর ওভার-ট্রাম্প নিয়ম।"
                )
            ),
            sourceReference = "Pagat.com Call Break / Lakadi Standard",
            ruleVersion = "CBK-SA-v1.4",
            confidenceLevel = "High (Verified Regional Standard)",
            manual = BilingualManual(
                introEn = "Call Break is the South Asian sister game to Call Bridge, allowing single-trick calls (1–8) and awarding an iconic 13-point instant bonus for completing an 8-trick call.",
                introBn = "কল ব্রেক দক্ষিণ এশিয়ার অত্যন্ত জনপ্রিয় খেলা, যেখানে সর্বনিম্ন ১ কল দেওয়া যায় এবং ৮ কল সফলভাবে পূরণ করলে বিশেষ ১৩ পয়েন্ট বোনাস পাওয়া যায়।",
                objectiveEn = "Win at least as many tricks as your call each round over a 5-round tournament.",
                objectiveBn = "৫ রাউন্ডের টুর্নামেন্টে প্রতি রাউন্ডে নিজের কল পূরণ করে সর্বোচ্চ পয়েন্ট অর্জন করুন।",
                deckDealEn = "Standard 52-card deck, 13 cards dealt to each player.",
                deckDealBn = "৫২ তাসের ডেক, প্রত্যেকের জন্য ১৩টি তাস।",
                rulesEn = "Spades are always trump. Follow suit and beat the current highest card if possible. If void, play a winning Spade if held.",
                rulesBn = "ইস্কাবন সর্বদা রং। চাল দেওয়া রঙের বড় তাস থাকলে তা খেলতে হবে, না থাকলে ইস্কাবন দিয়ে কাটতে হবে।",
                scoringEn = "Success = Call + (0.1 × Extra Tricks). Completing a bid of 8 awards a special 13.0 bonus! Failure = -Call.",
                scoringBn = "সফল হলে কল + (০.১ × অতিরিক্ত হাত)। ৮ কল সফল করলে ১৩.০ বোনাস! ব্যর্থ হলে -কল।",
                strategyEn = "Track which suits opponents are void in early so you don't lose your King to a low Spade cut.",
                strategyBn = "কোন প্রতিপক্ষের কোন রং শেষ হয়েছে তা লক্ষ্য রাখুন, যাতে আপনার সাহেব ছোট ইস্কাবনে কাটা না পড়ে।",
                faqEn = "Q: How does Call Break differ from Call Bridge?\nA: Call Break allows bidding 1 and includes the 8-call 13-point bonus profile.",
                faqBn = "প্রশ্ন: কল ব্রিজের সাথে কল ব্রেকের পার্থক্য কী?\nউত্তর: কল ব্রেকে ১ কল দেওয়া যায় এবং ৮ কল পূরণে ১৩ পয়েন্ট বোনাস থাকে।"
            )
        ),
        GameInfo(
            id = GAME_COURT_PIECE,
            nameEn = "Court Piece / Rung",
            nameBn = "কোর্ট পিস / রং",
            altNames = "Rang, Hokm, Coat Piece",
            category = GameCategory.BANGLADESH_CLASSICS,
            isBangladeshClassic = true,
            origin = OriginClassification.SOUTH_ASIAN_CLASSIC,
            countryTagEn = "Bangladesh, India & Pakistan",
            countryTagBn = "বাংলাদেশ ও দক্ষিণ এশিয়া",
            minPlayers = 4,
            maxPlayers = 4,
            recommendedPlayers = 4,
            isTeamGame = true,
            deckType = DeckType.STANDARD_52,
            cardsDealtPerPlayer = 13,
            supportsLocalMultiplayer = true,
            ruleProfiles = listOf(
                RuleProfile(
                    id = "single_sir",
                    nameEn = "Single Sir (Classic 7 Tricks)",
                    nameBn = "সিঙ্গেল সির (ক্লাসিক ৭ হাত)",
                    descEn = "5-4-4 deal; Trump selector calls Rung after 5 cards; first team to 7 tricks wins (first 7 straight = Court!).",
                    descBn = "৫-৪-৪ বণ্টন; প্রথম ৫ তাসে রং নির্বাচন; ৭ হাতে জয় (টানা প্রথম ৭ হাতে কোর্ট!)।"
                )
            ),
            sourceReference = "Pagat.com Court Piece / Rang Archive",
            ruleVersion = "CP-SA-v1.3",
            confidenceLevel = "High (Verified Regional Standard)",
            manual = BilingualManual(
                introEn = "Court Piece (also called Rung / Rang) is a thrilling 4-player partnership game famous for its dramatic 5-card trump call and 'Court' sweep.",
                introBn = "কোর্ট পিস বা রং ৪ জনের দলগত খেলা, যেখানে প্রথম ৫টি তাস দেখেই রং ঘোষণা করতে হয় এবং টানা প্রথম ৭ হাত জিতলে 'কোর্ট' হয়।",
                objectiveEn = "Together with your partner across the table, win 7 of the 13 tricks — or capture the first 7 consecutive tricks for a prestigious Court (2 match points).",
                objectiveBn = "আপনার পার্টনারের সাথে মিলে ১৩ হাতের মধ্যে ৭ হাত জিতুন — আর টানা প্রথম ৭ হাত জিততে পারলে ২ পয়েন্টের 'কোর্ট' জিতে নিন।",
                deckDealEn = "52-card deck dealt in authentic 5-4-4 batches: 5 cards first to pick Trump (Rung), then 4 + 4 cards to reach 13 each.",
                deckDealBn = "৫২ তাসের ডেক ৫-৪-৪ ধাপে বণ্টন করা হয়: প্রথমে ৫ তাস দেখে রং বাছা হয়, তারপর আরও ৮ তাস দেওয়া হয়।",
                rulesEn = "The trump caller leads the first trick. Players must follow the led suit; if void, they may play trump or discard. Highest card of led suit wins unless trumped.",
                rulesBn = "যিনি রং ডাকেন তিনিই প্রথম চাল দেন। চাল দেওয়া রং থাকলে তা খেলতে হবে, না থাকলে রং দিয়ে কাটা বা অন্য তাস দেওয়া যাবে।",
                scoringEn = "Winning 7+ tricks scores 1 match point; winning the first 7 consecutive tricks scores a Court (2 match points). First team to 5 match points wins!",
                scoringBn = "৭ হাত জিতলে ১ ম্যাচ পয়েন্ট; টানা প্রথম ৭ হাত জিতলে 'কোর্ট' (২ পয়েন্ট)। আগে ৫ পয়েন্টে ম্যাচ জয়।",
                strategyEn = "Choose a suit where you have both high honors (A, K) and length (at least 2-3 cards in your opening 5).",
                strategyBn = "প্রথম ৫ তাসে যে রঙে টেক্কা বা সাহেবসহ একাধিক তাস আছে সেটিকে রং হিসেবে বাছাই করুন।",
                faqEn = "Q: What is a Court?\nA: Winning the very first 7 tricks in a row before opponents win even a single trick!",
                faqBn = "প্রশ্ন: কোর্ট কী?\nউত্তর: প্রতিপক্ষ একটি হাতও জেতার আগে টানা প্রথম ৭টি হাত জিতে নেওয়াই কোর্ট।"
            )
        ),
        GameInfo(
            id = GAME_TEEN_PATTI,
            nameEn = "Teen Patti",
            nameBn = "তিন পাত্তি",
            altNames = "Flush, Three Cards",
            category = GameCategory.BANGLADESH_CLASSICS,
            isBangladeshClassic = true,
            origin = OriginClassification.SOUTH_ASIAN_CLASSIC,
            countryTagEn = "Bangladesh & South Asia",
            countryTagBn = "বাংলাদেশ ও দক্ষিণ এশিয়া",
            minPlayers = 4,
            maxPlayers = 4,
            recommendedPlayers = 4,
            isTeamGame = false,
            deckType = DeckType.STANDARD_52,
            cardsDealtPerPlayer = 3,
            supportsLocalMultiplayer = true,
            ruleProfiles = listOf(
                RuleProfile(
                    id = "classic_virtual",
                    nameEn = "Classic Teen Patti (Virtual Chips)",
                    nameBn = "ক্লাসিক তিন পাত্তি (ভার্চুয়াল চিপস)",
                    descEn = "Authentic Blind & Seen betting, Trail > Pure Sequence > Sequence > Color > Pair > High Card. Virtual entertainment chips only.",
                    descBn = "ব্লাইন্ড ও সিন চাল, ট্রেইল > পিওর সিকোয়েন্স > সিকোয়েন্স > কালার > পেয়ার। শুধুমাত্র ভার্চুয়াল চিপস।"
                )
            ),
            sourceReference = "Pagat.com Teen Patti / Three Card Brag Variant",
            ruleVersion = "TP-SA-v1.2",
            confidenceLevel = "High (Verified Standard)",
            manual = BilingualManual(
                introEn = "Teen Patti is South Asia's classic 3-card showdown played here strictly with free entertainment virtual chips — zero real-money wagering.",
                introBn = "তিন পাত্তি দক্ষিণ এশিয়ার জনপ্রিয় ৩-তাসের খেলা। এখানে শুধুমাত্র বিনোদনের ভার্চুয়াল চিপস ব্যবহার করা হয়।",
                objectiveEn = "Win the virtual chip pot either by holding the strongest 3-card hand at Showdown or by outlasting opponents who Pack.",
                objectiveBn = "শোডাউনে সেরা ৩ তাসের কম্বিনেশন দেখিয়ে অথবা প্রতিপক্ষ প্যাক করলে পটের ভার্চুয়াল চিপস জিতে নিন।",
                deckDealEn = "52-card deck; 3 face-down cards dealt to each player after placing the boot ante.",
                deckDealBn = "৫২ তাসের ডেক; বুট অ্যান্টের পর প্রত্যেককে ৩টি করে উল্টো তাস দেওয়া হয়।",
                rulesEn = "Play Blind (betting 1× current stake without viewing cards) or tap 'See Cards' to play Seen (betting 2× blind stake). When 2 players remain, tap 'Showdown' to compare hands! Hand Ranks: 1. Trail (Three of a Kind) > 2. Pure Sequence (A-K-Q, A-2-3...) > 3. Sequence > 4. Color (Flush) > 5. Pair > 6. High Card.",
                rulesBn = "তাস না দেখে 'ব্লাইন্ড চাল' দিন অথবা 'তাস দেখুন' চেপে 'চাল দিন' (২ গুণ)। ২ জন বাকি থাকলে 'শো করুন' চেপে সেরা হাত তুলনা করুন। মানক্রম: ১. ট্রেইল (তিনটি একই) > ২. পিওর সিকোয়েন্স > ৩. সিকোয়েন্স > ৪. কালার > ৫. পেয়ার > ৬. হাই কার্ড।",
                scoringEn = "Winner collects the entire virtual chip pot; chips persist locally across sessions.",
                scoringBn = "বিজয়ী পুরো ভার্চুয়াল চিপ পট জিতে নেয় এবং চিপস লোকাল ডাটাবেসে সংরক্ষিত থাকে।",
                strategyEn = "Playing Blind for 1–2 rounds keeps your stake cost halved while pressuring Seen players.",
                strategyBn = "শুরুর ১–২ রাউন্ড ব্লাইন্ড খেললে খরচ কম থাকে এবং সিন খেলোয়াড়দের ওপর চাপ তৈরি হয়।",
                faqEn = "Q: Does this use real money?\nA: Never! All chips are 100% free virtual entertainment points.",
                faqBn = "প্রশ্ন: এখানে কি আসল টাকা লাগে?\nউত্তর: একদমই না! সব চিপস সম্পূর্ণ বিনামূল্যে বিনোদনের ভার্চুয়াল পয়েন্ট।"
            )
        ),
        GameInfo(
            id = GAME_BHABHI,
            nameEn = "Bhabhi / Getaway",
            nameBn = "ভাবী / গেটঅ্যাওয়ে",
            altNames = "Bhabhi Thulla, Kazhutha, Getaway",
            category = GameCategory.BANGLADESH_CLASSICS,
            isBangladeshClassic = true,
            origin = OriginClassification.SOUTH_ASIAN_CLASSIC,
            countryTagEn = "Bangladesh, India & Pakistan",
            countryTagBn = "বাংলাদেশ ও দক্ষিণ এশিয়া",
            minPlayers = 4,
            maxPlayers = 4,
            recommendedPlayers = 4,
            isTeamGame = false,
            deckType = DeckType.STANDARD_52,
            cardsDealtPerPlayer = 13,
            supportsLocalMultiplayer = true,
            ruleProfiles = listOf(
                RuleProfile(
                    id = "classic_bhabhi",
                    nameEn = "Classic Bhabhi Thulla",
                    nameBn = "ক্লাসিক ভাবী থুল্লা",
                    descEn = "Ace of Spades opens, Thulla forces highest led-suit player to pick up the pile, avoid being last!",
                    descBn = "ইস্কাবনের টেক্কায় শুরু, থুল্লা পড়লে সর্বোচ্চ তাসধারীকে সব তাস তুলতে হয়!"
                )
            ),
            sourceReference = "Pagat.com Bhabhi / Getaway Rules",
            ruleVersion = "BH-SA-v1.1",
            confidenceLevel = "High (Verified Regional Standard)",
            manual = BilingualManual(
                introEn = "Bhabhi (Getaway) is a hilarious, tense shedding card game beloved across Bangladesh and South Asia where the goal is to 'Get Away' by emptying your hand before anyone else is left holding cards.",
                introBn = "ভাবী বা গেটঅ্যাওয়ে অত্যন্ত মজার তাস ছাড়ার খেলা, যেখানে সবার আগে নিজের হাতের সব তাস খালি করে 'গেটঅ্যাওয়ে' করতে হয়।",
                objectiveEn = "Empty your entire hand of cards so you are NOT the final player remaining (the 'Bhabhi').",
                objectiveBn = "হাতের সব তাস শেষ করুন যাতে আপনি সবার শেষে তাস হাতে থেকে 'ভাবী' না হয়ে যান।",
                deckDealEn = "All 52 cards dealt evenly (13 each to 4 players). The holder of the Ace of Spades starts the game.",
                deckDealBn = "৫২টি তাস সমানভাবে ৪ জনকে (১৩টি করে) দেওয়া হয়। যার হাতে ইস্কাবনের টেক্কা (A♠) আছে সে খেলা শুরু করে।",
                rulesEn = "Players must follow the led suit. If all active players follow suit, the trick is discarded cleanly and the highest card leads next. However, if ANY player cannot follow suit, they throw a 'Thulla' (any off-suit card, usually a high card!) — play stops immediately and whoever played the highest card of the led suit MUST pick up all cards on the table into their hand!",
                rulesBn = "চাল দেওয়া রঙের তাস থাকলে অবশ্যই খেলতে হবে। সবাই একই রং দিলে তাসগুলো বাতিল হয়। কিন্তু কেউ সেই রং না থাকলে অন্য রঙের তাস ('থুল্লা') দিলে তখনই হাত থেমে যায় এবং চাল দেওয়া রঙের সবচেয়ে বড় তাস যিনি দিয়েছেন তাকে টেবিলের সব তাস হাতে তুলে নিতে হয়!",
                scoringEn = "First players to empty their hands Get Away safely. The last player remaining with cards loses as Bhabhi!",
                scoringBn = "যাদের তাস শেষ হয়ে যায় তারা নিরাপদে বের হয়ে যায়। সবার শেষে যার হাতে তাস থাকে সে 'ভাবী' হিসেবে পরাজিত হয়।",
                strategyEn = "Shed your Aces and Kings safely on early tricks before opponents run void in a suit and hit you with a Thulla!",
                strategyBn = "প্রতিপক্ষের কোনো রং শেষ হওয়ার আগেই আপনার বড় টেক্কা-সাহেব ছেড়ে দিন, নইলে বড় থুল্লা খেতে হবে!",
                faqEn = "Q: What happens when a Thulla is played?\nA: The trick ends immediately and the player with the highest card of the led suit picks up every card played in that trick.",
                faqBn = "প্রশ্ন: থুল্লা পড়লে কী হয়?\nউত্তর: হাত সাথে সাথে থেমে যায় এবং চাল দেওয়া রঙের সর্বোচ্চ তাসধারীকে টেবিলের সব তাস তুলতে হয়।"
            )
        ),
        GameInfo(
            id = GAME_SPADES,
            nameEn = "Spades",
            nameBn = "স্পেডস",
            altNames = "Partnership Spades",
            category = GameCategory.TRICK_TAKING,
            isBangladeshClassic = false,
            origin = OriginClassification.INTERNATIONAL_CLASSIC,
            countryTagEn = "USA & Worldwide",
            countryTagBn = "আন্তর্জাতিক ক্লাসিক",
            minPlayers = 4,
            maxPlayers = 4,
            recommendedPlayers = 4,
            isTeamGame = true,
            deckType = DeckType.STANDARD_52,
            cardsDealtPerPlayer = 13,
            supportsLocalMultiplayer = true,
            ruleProfiles = listOf(
                RuleProfile(
                    id = "standard_500",
                    nameEn = "Classic 500 Spades (Nil + Bags)",
                    nameBn = "ক্লাসিক ৫০০ স্পেডস (নিল + ব্যাগস)",
                    descEn = "Partnership bidding with Nil (+100/-100), 10-bag penalty (-100), Spades breaking rule.",
                    descBn = "দলগত ডাক, নিল বোনাস (+১০০/-১০০), ১০ ব্যাগে -১০০ পেনাল্টি, ৫০০ পয়েন্টে জয়।"
                )
            ),
            sourceReference = "Pagat.com Standard Partnership Spades",
            ruleVersion = "SP-INT-v1.3",
            confidenceLevel = "High (Verified Standard)",
            manual = BilingualManual(
                introEn = "Spades is one of the world's premier 4-player partnership trick-taking classics, featuring Nil bids and Sandbag penalties.",
                introBn = "স্পেডস বিশ্বজুড়ে জনপ্রিয় ৪ জনের দলগত তাস খেলা, যেখানে ইস্কাবন স্থায়ী রং এবং নিল (০) ডাকের রোমাঞ্চকর নিয়ম আছে।",
                objectiveEn = "Combine with your partner to make your team bid and be the first team to reach 500 points.",
                objectiveBn = "পার্টনারের সাথে মিলে দলগত ডাক পূরণ করে সবার আগে ৫০০ পয়েন্ট অর্জন করুন।",
                deckDealEn = "52-card deck, 13 cards dealt to each player.",
                deckDealBn = "৫২ তাসের ডেক, প্রত্যেকের জন্য ১৩টি তাস।",
                rulesEn = "Each player bids 0 (Nil) to 13 tricks. Spades are trump and cannot be led until 'broken' (played on a void) or if you only hold Spades.",
                rulesBn = "প্রত্যেকে ০ (নিল) থেকে ১৩ পর্যন্ত ডাক দেয়। ইস্কাবন রং হিসেবে ব্যবহার না হওয়া পর্যন্ত প্রথম চালে ইস্কাবন খেলা যায় না।",
                scoringEn = "Made team bid = 10 × bid + 1 per overtrick (bag). Accumulating 10 bags costs -100! Making Nil awards +100 (failing costs -100).",
                scoringBn = "ডাক পূরণ করলে ১০ × ডাক + প্রতি অতিরিক্ত হাতে ১ ব্যাগ। ১০ ব্যাগ জমলে -১০০ কাটা যায়। নিল সফল হলে +১০০।",
                strategyEn = "Bid Nil only when you lack the Ace/King of Spades and have low cards in long suits.",
                strategyBn = "হাতে বড় ইস্কাবন না থাকলে এবং ছোট তাস বেশি থাকলে তবেই নিল ডাক দিন।",
                faqEn = "Q: What is a Bag?\nA: Each trick won above your team's bid is a bag (+1 pt), but 10 bags trigger a -100 sandbag penalty.",
                faqBn = "প্রশ্ন: ব্যাগ কী?\nউত্তর: ডাকের চেয়ে বেশি হাত জিতলে তা ব্যাগ হিসেবে জমা হয়; ১০টি ব্যাগ হলে -১০০ পেনাল্টি হয়।"
            )
        ),
        GameInfo(
            id = GAME_HEARTS,
            nameEn = "Hearts",
            nameBn = "হার্টস",
            altNames = "Black Lady",
            category = GameCategory.TRICK_TAKING,
            isBangladeshClassic = false,
            origin = OriginClassification.INTERNATIONAL_CLASSIC,
            countryTagEn = "International Classic",
            countryTagBn = "আন্তর্জাতিক ক্লাসিক",
            minPlayers = 4,
            maxPlayers = 4,
            recommendedPlayers = 4,
            isTeamGame = false,
            deckType = DeckType.STANDARD_52,
            cardsDealtPerPlayer = 13,
            supportsLocalMultiplayer = true,
            ruleProfiles = listOf(
                RuleProfile(
                    id = "classic_hearts",
                    nameEn = "Classic Hearts (to 100 pts)",
                    nameBn = "ক্লাসিক হার্টস (১০০ পয়েন্ট)",
                    descEn = "Avoid Hearts (1 pt) and Queen of Spades (13 pts), or Shoot the Moon for 26!",
                    descBn = "হরতন (১ পয়েন্ট) ও ইস্কাবনের বিবি (১৩ পয়েন্ট) এড়িয়ে চলুন, অথবা সব ২৬টি নিয়ে শ্যুট দ্য মুন করুন!"
                )
            ),
            sourceReference = "Pagat.com Black Lady Hearts Standard",
            ruleVersion = "HT-INT-v1.2",
            confidenceLevel = "High (Verified Standard)",
            manual = BilingualManual(
                introEn = "Hearts is the classic trick-avoidance card game where taking penalty cards hurts your score — unless you boldly capture every single one!",
                introBn = "হার্টস একটি বুদ্ধিদীপ্ত তাস খেলা যেখানে পেনাল্টি তাস (হরতন ও ইস্কাবনের বিবি) এড়িয়ে কম স্কোর ধরে রাখতে হয়।",
                objectiveEn = "Have the LOWEST total score when any player reaches 100 penalty points.",
                objectiveBn = "কেউ ১০০ পয়েন্টে পৌঁছালে যার স্কোর সবচেয়ে কম থাকবে তিনিই জয়ী।",
                deckDealEn = "52-card deck, 13 cards each. 2 of Clubs always leads the first trick.",
                deckDealBn = "৫২ তাসের ডেক, ১৩টি করে তাস। চিড়িতনের ২ (2♣) দিয়ে প্রথম চাল শুরু হয়।",
                rulesEn = "No points (Hearts or Q♠) may be played on Trick 1. Hearts cannot be led until broken. Follow led suit whenever possible.",
                rulesBn = "প্রথম হাতে কোনো হরতন বা Q♠ দেওয়া যাবে না। হরতন একবার ভাঙার আগে প্রথম চালে হরতন খেলা যাবে না।",
                scoringEn = "Each Heart = 1 pt, Queen of Spades (Q♠) = 13 pts. Capturing all 26 points ('Shoot the Moon') gives 0 to you and +26 to all opponents!",
                scoringBn = "প্রতি হরতন ১ পয়েন্ট, ইস্কাবনের বিবি ১৩ পয়েন্ট। একা সব ২৬ পয়েন্ট নিলে আপনার ০ এবং অন্য সবার +২৬ পয়েন্ট যোগ হবে!",
                strategyEn = "Void yourself in a short suit early so you can dump the Queen of Spades onto an unsuspecting opponent.",
                strategyBn = "দ্রুত একটি রং খালি করে নিন যাতে প্রতিপক্ষের হাতে ইস্কাবনের বিবি (Q♠) গছিয়ে দিতে পারেন।",
                faqEn = "Q: Is lower score better in Hearts?\nA: Yes! Hearts is a trick-avoidance game — lowest score wins.",
                faqBn = "প্রশ্ন: হার্টসে কি কম পয়েন্ট ভালো?\nউত্তর: হ্যাঁ! এখানে সবচেয়ে কম পয়েন্টধারী বিজয়ী হয়।"
            )
        ),
        GameInfo(
            id = GAME_CRAZY_EIGHTS,
            nameEn = "Crazy Eights",
            nameBn = "ক্রেজি এইটস",
            altNames = "Switch, Last Card",
            category = GameCategory.SHEDDING,
            isBangladeshClassic = false,
            origin = OriginClassification.INTERNATIONAL_CLASSIC,
            countryTagEn = "International Classic",
            countryTagBn = "আন্তর্জাতিক ক্লাসিক",
            minPlayers = 2,
            maxPlayers = 4,
            recommendedPlayers = 4,
            isTeamGame = false,
            deckType = DeckType.STANDARD_52,
            cardsDealtPerPlayer = 5,
            supportsLocalMultiplayer = true,
            ruleProfiles = listOf(
                RuleProfile(
                    id = "classic_crazy_8",
                    nameEn = "Classic 4-Player Crazy Eights",
                    nameBn = "ক্লাসিক ৪ জনের ক্রেজি এইটস",
                    descEn = "Match suit or rank, 8 is Wild to switch active suit, draw when unable to play.",
                    descBn = "রং বা সংখ্যা মেলান, ৮ হলো ওয়াইল্ড কার্ড যা দিয়ে রং বদলানো যায়।"
                )
            ),
            sourceReference = "Pagat.com Crazy Eights Rules",
            ruleVersion = "C8-INT-v1.1",
            confidenceLevel = "High (Verified Standard)",
            manual = BilingualManual(
                introEn = "Crazy Eights is the fast, joyful shedding card classic that inspired modern color-matching card games.",
                introBn = "ক্রেজি এইটস দ্রুত ও আনন্দময় তাস ছাড়ার খেলা, যেখানে ৮ নম্বর তাস দিয়ে যেকোনো সময় রং পরিবর্তন করা যায়।",
                objectiveEn = "Be the first player to discard every card in your hand.",
                objectiveBn = "সবার আগে নিজের হাতের সব তাস শেষ করুন।",
                deckDealEn = "52-card deck; 5 cards dealt to each player, 1 face-up starter card, rest form the draw pile.",
                deckDealBn = "৫২ তাসের ডেক; প্রত্যেককে ৫টি করে তাস এবং টেবিলে ১টি খোলা তাস দেওয়া হয়।",
                rulesEn = "Play a card matching either the active suit or rank of the top discard card — or play any Eight (8) at any time and pick the new active suit! If you have no playable card, draw from the deck.",
                rulesBn = "টেবিলের উপরের তাসের রং বা সংখ্যা মিলিয়ে তাস ছাড়ুন, অথবা যেকোনো সময় ৮ (ওয়াইল্ড) খেলে নতুন রং বেছে নিন। বৈধ তাস না থাকলে ডেক থেকে তাস তুলুন।",
                scoringEn = "Winner earns points from opponents' remaining hands: 8 = 50 pts, K/Q/J/10 = 10 pts, Ace = 1 pt, others = face value.",
                scoringBn = "বিজয়ী প্রতিপক্ষের বাকি তাস থেকে পয়েন্ট পায়: ৮ = ৫০, K/Q/J/১০ = ১০, A = ১, বাকিগুলো সংখ্যার মান।",
                strategyEn = "Hold onto your Eights for emergencies and shed high-value face cards early.",
                strategyBn = "জরুরি মুহূর্তের জন্য ৮ জমিয়ে রাখুন এবং আগে বড় মানের তাস ছেড়ে দিন।",
                faqEn = "Q: Can I play an 8 on anything?\nA: Yes! Eights are always wild regardless of the current suit.",
                faqBn = "প্রশ্ন: ৮ কি যেকোনো তাসের ওপর খেলা যায়?\nউত্তর: হ্যাঁ, ৮ সব সময়ই ওয়াইল্ড কার্ড।"
            )
        ),
        GameInfo(
            id = GAME_BLACKJACK,
            nameEn = "Blackjack 21",
            nameBn = "ব্ল্যাকজ্যাক ২১",
            altNames = "Twenty-One, Pontoon",
            category = GameCategory.CASINO_POKER,
            isBangladeshClassic = false,
            origin = OriginClassification.INTERNATIONAL_CLASSIC,
            countryTagEn = "International Casino Classic",
            countryTagBn = "আন্তর্জাতিক ক্যাসিনো ক্লাসিক",
            minPlayers = 1,
            maxPlayers = 1,
            recommendedPlayers = 1,
            isTeamGame = false,
            deckType = DeckType.STANDARD_52,
            cardsDealtPerPlayer = 2,
            supportsLocalMultiplayer = false,
            ruleProfiles = listOf(
                RuleProfile(
                    id = "classic_21",
                    nameEn = "Grand Salon 21 (3:2 Natural)",
                    nameBn = "গ্র্যান্ড স্যালন ২১ (৩:২ ব্ল্যাকজ্যাক)",
                    descEn = "Dealer stands on all 17s, Hit, Stand, Double Down, 3:2 Blackjack payout with virtual chips.",
                    descBn = "ডিলার ১৭-তে থামে, হিট, স্ট্যান্ড, ডাবল ডাউন, ভার্চুয়াল চিপসে ৩:২ ব্ল্যাকজ্যাক পেআউট।"
                )
            ),
            sourceReference = "Standard International Casino Blackjack Rules",
            ruleVersion = "BJ-INT-v1.1",
            confidenceLevel = "High (Verified Standard)",
            manual = BilingualManual(
                introEn = "Blackjack 21 pits your strategy directly against the house dealer using free virtual chips — reach 21 without going bust!",
                introBn = "ব্ল্যাকজ্যাক ২১ হলো ডিলারের বিপক্ষে কৌশলগত ২১ পয়েন্টের খেলা। এখানে সম্পূর্ণ বিনামূল্যের ভার্চুয়াল চিপস ব্যবহৃত হয়।",
                objectiveEn = "Beat the dealer's hand total by getting closer to 21 without exceeding 21.",
                objectiveBn = "২১ অতিক্রম না করে ডিলারের চেয়ে বেশি পয়েন্ট তৈরি করুন।",
                deckDealEn = "52-card deck; you get 2 face-up cards, dealer gets 1 face-up and 1 hidden hole card.",
                deckDealBn = "৫২ তাসের ডেক; আপনার ২টি খোলা তাস এবং ডিলারের ১টি খোলা ও ১টি গোপন তাস।",
                rulesEn = "Number cards = face value, J/Q/K = 10, Ace = 11 or 1. Choose Hit (draw), Stand (hold), or Double Down (2× bet + 1 final card). Dealer draws until reaching 17+.",
                rulesBn = "সংখ্যা তাস = নিজ মান, J/Q/K = ১০, টেক্কা = ১১ বা ১। কার্ড নিন (Hit), থামুন (Stand), অথবা ডাবল করুন। ডিলার ১৭ বা বেশি না হওয়া পর্যন্ত তাস তোলে।",
                scoringEn = "Natural 21 pays 3:2 virtual chips; regular win pays 1:1; tie pushes your bet back.",
                scoringBn = "ন্যাচারাল ২১-এ ৩:২ ভার্চুয়াল চিপস; সাধারণ জয়ে ১:১; ড্র হলে চিপস ফেরত।",
                strategyEn = "Stand on 17+, and consider Standing on 13–16 when the dealer shows a weak 2–6 upcard.",
                strategyBn = "হাতে ১৭+ থাকলে থেমে যান; ডিলারের খোলা তাস ২–৬ হলে ১৩–১৬ তেই থামা নিরাপদ।",
                faqEn = "Q: What happens if I hold an Ace and go over 21?\nA: The Ace automatically adjusts from 11 down to 1 so you don't bust.",
                faqBn = "প্রশ্ন: টেক্কা থাকলে ২১-এর বেশি হলে কী হয়?\nউত্তর: টেক্কা স্বয়ংক্রিয়ভাবে ১১ থেকে ১ হয়ে যায়।"
            )
        ),
        GameInfo(
            id = GAME_KLONDIKE,
            nameEn = "Klondike Solitaire",
            nameBn = "ক্লোনডাইক সলিটায়ার",
            altNames = "Patience, Classic Solitaire",
            category = GameCategory.SOLITAIRE,
            isBangladeshClassic = false,
            origin = OriginClassification.INTERNATIONAL_CLASSIC,
            countryTagEn = "Worldwide Solo Classic",
            countryTagBn = "বিশ্বখ্যাত একক খেলা",
            minPlayers = 1,
            maxPlayers = 1,
            recommendedPlayers = 1,
            isTeamGame = false,
            deckType = DeckType.STANDARD_52,
            cardsDealtPerPlayer = 28,
            supportsLocalMultiplayer = false,
            ruleProfiles = listOf(
                RuleProfile(
                    id = "draw_1",
                    nameEn = "Classic Draw 1",
                    nameBn = "ক্লাসিক ড্র ১",
                    descEn = "Turn 1 card at a time from stock with unlimited passes, undo, hints, and auto-finish.",
                    descBn = "একবার করে ১টি তাস তুলুন, আনডু, হিন্ট ও অটো-ফিনিশ সুবিধাসহ।"
                ),
                RuleProfile(
                    id = "draw_3",
                    nameEn = "Challenge Draw 3",
                    nameBn = "চ্যালেঞ্জ ড্র ৩",
                    descEn = "Classic Vegas-style 3-card stock turn for seasoned Solitaire players.",
                    descBn = "অভিজ্ঞ খেলোয়াড়দের জন্য একসাথে ৩টি করে তাস তোলার চ্যালেঞ্জ মোড।"
                )
            ),
            sourceReference = "Standard Klondike Patience Rules",
            ruleVersion = "KL-INT-v1.2",
            confidenceLevel = "High (Verified Standard)",
            manual = BilingualManual(
                introEn = "Klondike Solitaire is the timeless single-player patience game with smooth tap-to-move intelligence, undo history, and instant auto-complete.",
                introBn = "ক্লোনডাইক সলিটায়ার বিশ্বজুড়ে পরিচিত একক তাস খেলা, যেখানে স্মার্ট ট্যাপ-টু-মুভ, আনডু এবং অটো-ফিনিশ সুবিধা রয়েছে।",
                objectiveEn = "Build all 4 Foundation piles up by suit from Ace (A) to King (K).",
                objectiveBn = "চারটি ফাউন্ডেশনে প্রতি রঙের টেক্কা (A) থেকে সাহেব (K) পর্যন্ত ক্রমানুসারে সাজান।",
                deckDealEn = "52-card deck: 7 tableau columns (1 to 7 cards, top card face-up) + 24 cards in the Stock pile.",
                deckDealBn = "৫২ তাসের ডেক: ৭টি টেবিল কলামে (১ থেকে ৭টি তাস) এবং বাকি ২৪টি স্টক ডেকে।",
                rulesEn = "Stack cards on the Tableau in descending rank with alternating colors (Red on Black / Black on Red). Only Kings fill empty tableau columns. Tap any highlighted card or stack to move it automatically to its best legal target!",
                rulesBn = "টেবিল কলামে বড় থেকে ছোট ক্রমে বিপরীত রঙের তাস (লালের ওপর কালো) সাজান। ফাঁকা কলামে কেবল সাহেব (K) বসবে। যেকোনো তাসে ট্যাপ করলেই তা সেরা বৈধ স্থানে সরে যাবে।",
                scoringEn = "+10 per Foundation card, +5 per revealed Tableau card, +5 Waste to Tableau, plus speed bonus on victory!",
                scoringBn = "ফাউন্ডেশনে +১০, নতুন তাস উল্টালে +৫, এবং দ্রুত শেষ করলে বোনাস স্কোর।",
                strategyEn = "Always prioritize uncovering hidden face-down cards in the tallest tableau columns before drawing from stock.",
                strategyBn = "ডেক থেকে তাস তোলার আগে বড় কলামের উল্টো তাসগুলো খোলার চেষ্টা করুন।",
                faqEn = "Q: When does Auto-Finish appear?\nA: As soon as all hidden tableau cards are turned face-up, you can tap Auto-Finish to sweep all cards to the Foundations!",
                faqBn = "প্রশ্ন: অটো-ফিনিশ কখন চালু হয়?\nউত্তর: টেবিলের সব উল্টো তাস খোলা হয়ে গেলেই এক চাপে সব তাস ফাউন্ডেশনে সাজানো যায়।"
            )
        )
    )

    fun byId(id: String): GameInfo = allGames.firstOrNull { it.id == id } ?: allGames.first()

    val bangladeshClassics: List<GameInfo> get() = allGames.filter { it.isBangladeshClassic }
    val worldClassics: List<GameInfo> get() = allGames.filter { !it.isBangladeshClassic && it.category != GameCategory.SOLITAIRE }
    val solitaireGames: List<GameInfo> get() = allGames.filter { it.category == GameCategory.SOLITAIRE }
}

# World Card Master — Asset & License Inventory

All assets included in World Card Master are original, synthesized locally, or bundled from verified open-source font releases. No copyrighted commercial artwork, audio, or music is included.

## 1. Typography (Bundled Static TTF Fonts)
- **Playfair Display** (`res/font/playfair_display.ttf`) — SIL Open Font License (OFL 1.1), Google Fonts
- **Plus Jakarta Sans** (`res/font/plus_jakarta_sans.ttf`) — SIL Open Font License (OFL 1.1), Google Fonts
- **Hind Siliguri** (`res/font/hind_siliguri.ttf`, Bengali Unicode) — SIL Open Font License (OFL 1.1), Google Fonts

## 2. Visual Artwork & Playing Cards
- **Playing Card Faces & Vector Suits**: 100% original procedural Android Jetpack Compose Canvas vector geometry (`RealisticCard.kt`)
- **Table Surfaces & Felt Lighting**: 100% original procedural Canvas shaders (`TableSurface.kt`)
- **App Emblem & Salon Banners** (`ic_wcm_logo_fg.png`, `img_hero_salon.jpg`, `img_card_back_royal.jpg`, `img_bangladesh_banner.jpg`): Original generated project artwork created exclusively for World Card Master

## 3. Sound Effects & Background Music
- **Card SFX & Musical Cues** (`CardAudioEngine.kt`): 100% original procedural PCM WAV synthesis generated at runtime into app cache via Android `SoundPool`
- **Ambient Background Music**: 100% original synthesized harmonic chord progressions via Android `AudioTrack`

## 4. Open-Source Libraries
- AndroidX Core, Lifecycle, Activity Compose, Jetpack Compose, Material 3, Room Persistence, Kotlin Coroutines — Apache License 2.0

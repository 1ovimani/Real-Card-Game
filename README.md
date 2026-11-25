# World Card Master

Offline-first luxury Android card gaming platform built with Kotlin, Jetpack Compose, Material 3, Room SQLite, custom Canvas playing card rendering, synthesized studio card audio, English & বাংলা localization, and local Wi-Fi / Hotspot / Bluetooth Classic RFCOMM multiplayer.

## Included Playable Games (12 Engines)
- **Bangladesh Classics:** Twenty-Nine (29) with Hidden Trump/Pair/Double/Redouble, Hazari (1000), Call Bridge, Call Break, Court Piece / Rung, Teen Patti (Virtual Chips), Bhabhi / Getaway
- **International Classics:** Spades, Hearts, Crazy Eights, Blackjack 21 (Virtual Chips), Klondike Solitaire

---

## Build APK Automatically with GitHub Actions

1. Push this project to a GitHub repository (using AI Studio's **Push to GitHub** or `git push`).
2. Open your repository on GitHub and click the **Actions** tab.
3. The **Build World Card Master APK** workflow runs automatically on every push to `main`/`master` (or click **Run workflow** manually).
4. Once green, open the workflow run and download **`WorldCardMaster-debug-apk`** from **Artifacts**, unzip it, and install `WorldCardMaster-debug.apk` on your Android phone.

### Optional: Signed Release APK & Play Store AAB on GitHub
Add these three repository secrets under **Settings → Secrets and variables → Actions**:
- `KEYSTORE_BASE64` — Base64-encoded `.jks` upload keystore (alias `upload`)
- `STORE_PASSWORD` — Keystore password
- `KEY_PASSWORD` — Key password

When those secrets are present, GitHub Actions automatically builds and uploads both `WorldCardMaster-release.apk` and `WorldCardMaster-release.aab`.

---

## GitHub দিয়ে APK বিল্ড করার নিয়ম (বাংলা)

১. AI Studio থেকে প্রজেক্টটি **GitHub-এ পুশ** করুন।  
২. আপনার GitHub রিপোজিটরির **Actions** ট্যাবে যান।  
৩. **Build World Card Master APK** ওয়ার্কফ্লোটি স্বয়ংক্রিয়ভাবে চলবে (অথবা **Run workflow** বাটনে ক্লিক করে চালাতে পারবেন)।  
৪. বিল্ড শেষ হলে নিচের **Artifacts** অংশ থেকে **`WorldCardMaster-debug-apk`** ডাউনলোড ও আনজিপ করে আপনার Android ফোনে ইনস্টল করুন।

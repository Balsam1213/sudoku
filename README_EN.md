# Sudoku

[中文](README.md) | English

A fully offline Android Sudoku game built from scratch with Kotlin + Jetpack Compose (Material 3). No Internet permission, no login, no tracking.

## Features

- **Six difficulty levels**: Beginner / Easy / Medium / Hard / Expert / Extreme (25/35/45/54/58/60+ empty cells)
- **Guaranteed puzzle quality**: a hand-written bitmask + MRV backtracking solver verifies that the puzzle still has a unique solution after every cell is removed — every puzzle has exactly one solution
- **Complete gameplay**: pencil notes (conflicting candidates are rejected automatically), undo/redo, erase, hints, instant conflict highlighting, row/column/box and same-digit highlighting
- **Lightning mode**: lock a digit and fill every matching cell one by one; advances to the next digit automatically. Enabled by default for every new game
- **Daily challenge**: difficulty adapts to how you usually play; the same puzzle for everyone on a given day (date-seeded generation); complete every day of a month to earn that month's trophy
- **Achievements**: 72 achievements in 13 groups (per-difficulty milestones, pencil/hints/cumulative days/daily challenges, etc.), progress bars for cumulative ones, and non-blocking banner + sound when unlocked mid-game
- **Stats**: best/average time and win rate per difficulty, current & longest streak, total active days, last 50 games
- **Share**: share the puzzle as an image during a game, or share the completed board and your result on the win screen
- **Extras**: light/dark/system themes, optional 3-mistakes loss rule, sound & haptic feedback, auto-saved games you can resume anytime

## Technical Highlights

- Kotlin + Jetpack Compose (BOM) / Material 3, single Activity, MVVM
- Room (records / achievements / daily challenges / month trophies, with a v1→v2 migration) + DataStore (settings and game saves)
- Pure-Kotlin Sudoku generation & solving engine with unit tests covering the unique-solution guarantee
- R8 code & resource shrinking; release APK is about 1.4 MB
- Minimum Android 8.0 (API 26)

## Build & Run

```bash
./gradlew installDebug        # Build and install the debug build (device/emulator required)
./gradlew testDebugUnitTest   # Run unit tests (unique-solution guarantee, etc.)
./gradlew assembleRelease     # Build the release package
```

Requirements: JDK 17+, Android SDK (configure `sdk.dir` in `local.properties`; this file is not committed — create it after cloning).

## About Signing

`keystore/` and `keystore.properties` (the release signing key) are **intentionally not committed**. To build a release after cloning, create your own key and a `keystore.properties` file in the project root:

```properties
storeFile=keystore/your-key.jks
storePassword=your-password
keyAlias=your-alias
keyPassword=your-password
```

Without a key the release build produces an unsigned package; debug builds are unaffected.

## Project Structure

```
app/src/main/java/com/balsam/sudoku/
├── game/          # Sudoku engine (generator, solver, rules)
├── data/          # Room database and DataStore repositories
├── achievements/  # Achievement definitions and evaluation engine
├── ui/
│   ├── menu/      # Main menu (daily challenge, difficulty selection)
│   ├── play/      # Game screen (board, keypad, lightning mode)
│   ├── stats/     # Statistics
│   ├── achievements/ # Achievements page
│   ├── settings/  # Settings
│   ├── common/    # Share image, sound, formatting utilities
│   └── theme/     # Theming
└── MainActivity.kt
```

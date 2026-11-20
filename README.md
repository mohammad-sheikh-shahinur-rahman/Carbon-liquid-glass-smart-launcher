# Carbon

**Carbon** (`com.itamadersomajinc.Carbon`) is a native Android launcher built with Kotlin, Jetpack Compose, and Material 3 featuring a **LiquidGlass** UI/UX.

## Highlights

- **LiquidGlass Visual System**: Frosted translucent glass capsules, cards, sheets, and specular rim borders layered over an atmospheric plum/slate/teal LiquidGlass backdrop (plus additional curated wallpapers and live/system wallpaper support).
- **App Library & Quick Scrubber**: LiquidGlass `App Library` search bar with `Cancel`, colorful circular app icons with indented hairline dividers, and a right-edge A–Z scrubber with a frosted circular glass lens indicator.
- **Customizable Home & Dock**: Configurable home layouts, live clock & Open-Meteo weather card, media now-playing card, notification badge count, customizable gestures, and glass opacity controls.
- **Native Android Widgets & Shortcuts**: Built-in `AppWidgetHost` integration, app launcher shortcuts, Essentials quick bar, and a hidden App Vault.
- **Privacy & System Integrations**: Accessibility service for one-tap screen lock, Notification Listener for unread counts and media session controls, and zero-permission Photo Picker for custom wallpapers.

## Project Configuration

- **App Name**: `Carbon`
- **Application ID**: `com.itamadersomajinc.Carbon`
- **Tech Stack**: Kotlin, Jetpack Compose, Material 3, Coroutines & StateFlow, Gradle Kotlin DSL (`.gradle.kts`)
- **Min SDK**: 26 | **Compile / Target SDK**: 36

## Build

```bash
gradle :app:assembleDebug
```

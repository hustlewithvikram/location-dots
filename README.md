<div align="center">

# Location Dots

**Every place leaves a dot.**

A passive Android timeline that automatically turns your everyday movement into a visual history of places, journeys, and time.

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Status](https://img.shields.io/badge/Status-Pre--release%20Hardening-blue)](#status)

<img src="assets/location-dots-hero-banner.svg" alt="Location Dots hero banner" width="100%" />

</div>

## What is Location Dots?

Location Dots records your location in the background and turns it into a simple, continuous timeline.

You don't manually create places or log trips. The app is designed to quietly build your history while you live normally.

```text
🏠 Home
   │
   ●
   │
🏢 Office
   │
   ●
   │
☕ Cafe
   │
   ●
   │
🏋️ Gym
   │
   ●
   │
🏠 Home
```

> **Install once. Live normally. Your timeline builds itself.**

## Features

- **Automatic location timeline** — builds your day without manual logging.
- **Place detection** — turns location data into meaningful places and visits.
- **Journey tracking** — connects places into travel segments.
- **Infinite history** — scroll through days, weeks, months, and eventually years.
- **Day separators** — clearly separates one day from the next.
- **Visit details** — arrival time, departure time, and time spent at a place.
- **Travel details** — distance, travel duration, route maps, and travel-mode detection.
- **Place editing** — rename or correct places when the app gets them wrong.
- **Pause tracking** — stop recording whenever you want.
- **Local-first storage** — location history stays in the app database by default.

## The timeline

The timeline is the main interface. It is designed to feel like **scrolling through your physical life**, rather than browsing a conventional map.

```text
                    01 OCTOBER
                         │
                         ●
                    🏠 Home
                    08:42
                         │
                         │ 🚗 31 min
                         │
                         ●
                    🏢 Office
                    09:13
                         │
                         │ 8h 24m
                         │
                         ●
                    ☕ Cafe
                    17:37
                         │
                         │ 🚗 14 min
                         │
                         ●
                    🏋️ Gym
                    18:02
                         │
                         ●
                    🏠 Home
                    20:11

              · · · · · · · · · · ·

                   30 SEPTEMBER

              · · · · · · · · · · ·
```

## Permissions

Location Dots currently uses foreground location permission and a foreground location service. Approximate location is supported.

| Permission | Purpose |
| --- | --- |
| `ACCESS_FINE_LOCATION` | Precise location when granted |
| `ACCESS_COARSE_LOCATION` | Approximate location |
| `FOREGROUND_SERVICE` | Keep the tracking service running |
| `FOREGROUND_SERVICE_LOCATION` | Declare location use by the foreground service |

The app does not currently request `ACCESS_BACKGROUND_LOCATION`.

## Privacy

Location history can reveal sensitive information about where someone lives, works, exercises, and travels. Privacy is therefore a core part of the product.

- **Local-first** location storage.
- **No account required** for the initial version.
- **No cloud upload by default.**
- No selling or sharing of location history.
- Pause tracking whenever needed.
- Delete individual history or all history.
- Export your data.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Kotlin Coroutines / Flow
- WorkManager
- Fused Location Provider
- Activity Recognition
- Foreground Service

## Current functionality

- [x] Location permission onboarding
- [x] Foreground location tracking
- [x] Automatic place detection
- [x] Visits and journeys
- [x] Travel distance and movement mode
- [x] Persisted timeline with pagination
- [x] Place history and visit insights
- [x] Journey route maps
- [x] Timeline search
- [x] Places map overview
- [x] Personal insights and statistics
- [x] Today timeline summary
- [x] Local place renaming

## Status

**Pre-release hardening**

The core prototype is implemented; the current milestone is focused on reliability, edge cases, and release validation:

> **Install → grant permission → live your day → come back and see your day as a timeline.**

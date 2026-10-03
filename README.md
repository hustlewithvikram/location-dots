<div align="center">

# Location Dots

### **Every place leaves a dot.**

A privacy-focused Android app that quietly records where you go and turns your day into a simple timeline of **places, stops, and journeys**.

[![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Material 3](https://img.shields.io/badge/Material%203-6750A4)](https://m3.material.io/)
[![Status](https://img.shields.io/badge/Status-Pre--release-3F51B5)](#status)

<img src="assets/location-dots-hero-banner.svg" alt="Location Dots" width="100%" />

</div>

---

## What is Location Dots?

Location Dots is a **local-first location history app** for Android.

Instead of showing you a stream of GPS coordinates, it tries to turn your movement into something human-readable:

**You went somewhere → stayed there → left → travelled → arrived somewhere else.**

That becomes:

```text
📍 Place
   ↓
🕐 Visit
   ↓
🚶 Journey
   ↓
📍 Next place
```

Everything is designed around your personal timeline rather than a raw map full of points.

## How it works

You don't have to manually create every stop.

1. **Location Dots records location updates** while tracking is enabled.
2. **Your location history is saved locally** on the device.
3. The app looks for periods where you appear to have stayed in one area.
4. A sufficiently long stop becomes a **visit**.
5. Nearby visits can be recognized as the same **place**.
6. Movement between places becomes a **journey**.
7. The result appears in your **Timeline, Places, Maps, Search, and Insights**.

> **Turn it on → go about your day → come back and see where your day took you.**

### When does a stop become a place?

Location Dots deliberately waits before creating a dot. A stop currently needs to look like a real stay rather than a quick pause.

The important rule is simple:

**You need to remain in an area for about 8 minutes before it can become a visit.**

The app also considers the accuracy and movement of the recorded locations, so a single inaccurate GPS reading doesn't immediately create a place.

The exact detection rules live in the app and may change as the prototype improves.

## What you can do

| Timeline | Places | Insights | Settings |
|:---:|:---:|:---:|:---:|
| See your day | See saved places | Understand your activity | Control tracking |
| Visits & journeys | View place history | Distance & time | Map appearance |
| Route previews | Rename places | Movement summaries | Privacy & data |
| Search history | Explore on map | — | Backup & restore |

### Highlights

- **Automatic places** — stops can become recognizable places without manual input.
- **Journeys** — see movement between places with route and distance information.
- **Personal timeline** — browse your history by day.
- **Maps** — explore your places and routes visually.
- **Search** — find places and timeline history quickly.
- **Insights** — see useful summaries from your recorded activity.
- **Backup & restore** — export your local data and import it later.
- **Local data control** — clear your stored history whenever you want.
- **Themes & appearance** — choose system, light, or dark mode and customize map presentation.

## Privacy

Location history is personal.

Location Dots is built around keeping that history **on your device**.

- Your location history is stored locally.
- There is currently no account or cloud location-history service.
- Nothing is uploaded as part of normal location tracking.
- You decide when to export or share your data.
- You can delete your local history from the app.
- Maps use online map resources, so map display requires an internet connection.

> **Local-first doesn't mean offline-only.** Your history stays local, while the map can use the internet to display map data.

## What you need

### Android

- **Android 8.0 (API 26) or newer**
- Location permission
- Location services enabled
- Internet connection for online maps

### For reliable background tracking

Android may restrict apps that are heavily optimized for battery life. If tracking stops unexpectedly, check the app's **battery optimization** setting in Location Dots' diagnostics.

Location Dots uses an Android foreground service for continuous tracking, so you can leave the app and continue using your phone normally.

## Maps

Location Dots uses **MapLibre** with **OpenFreeMap**.

No Google Maps API key is required.

Available styles:

**Liberty · Bright · Positron · Dark · Fiord**

More details: [Maps setup →](MAPS_SETUP.md)

## Backup & restore

Your local history can be exported as a JSON backup from Settings.

The app lets you:

**Export → save the backup somewhere safe → import it later → review what will be restored → confirm.**

This is useful when moving to another device or keeping a personal copy of your history.

## For developers

The project is a Kotlin Android application using:

- Jetpack Compose + Material 3
- Room
- Kotlin Coroutines / Flow
- Google Play Services Location
- MapLibre Native
- OpenFreeMap

The code is organized around **UI → domain logic → local data**, with the location tracker running as an Android foreground service.

If you want the implementation details, see:

- [Architecture](ARCHITECTURE.md)
- [Maps setup](MAPS_SETUP.md)

### Repository structure

```text
app/
├── core/       → Android/platform helpers
├── data/       → Local database and repositories
├── domain/     → Places, journeys and business logic
├── feature/    → App screens
├── service/    → Background location tracking
└── ui/         → Shared UI, theme and maps
```

## Building

Clone the repository and open it in Android Studio.

For automated APK builds:

- [Debug workflow](.github/workflows/build-debug-apk.yml)
- [Release workflow](.github/workflows/build-release-apk.yml)

Release builds use code/resource shrinking and can use the project's release signing configuration when it is provided.

## Status

### **Pre-release**

The core experience is in place and the project is currently focused on reliability, edge cases, and release polish.

**Working today**

- [x] Onboarding
- [x] Location tracking
- [x] Automatic visits and places
- [x] Journey detection
- [x] Timeline
- [x] Places and place history
- [x] Journey details and routes
- [x] Search
- [x] Insights
- [x] Map customization
- [x] Backup & restore
- [x] Local data deletion
- [x] Diagnostics
- [x] Debug/release APK workflows

---

<div align="center">

### **Location Dots**
*Your day, turned into dots.*

</div>

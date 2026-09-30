# Location Dots

> **Every place leaves a dot.**

Location Dots is an Android app that automatically records where you go and turns your movements into a simple, continuous timeline.

Install it once, grant the required permissions, and go about your day. Location Dots builds the timeline automatically.

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

## What it does

- Automatically records your location history.
- Detects meaningful places and visits.
- Connects places into journeys.
- Builds an infinite chronological timeline.
- Separates each day with a clear date divider.
- Shows when you arrived, left, and how long you stayed.
- Shows travel duration, distance, and eventually travel mode.
- Lets you browse your history by simply scrolling through time.
- Keeps your location history local by default.

## Timeline

The main screen is the entire experience: a continuous vertical timeline of your movements.

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

The app should feel like **scrolling through your physical life**, rather than browsing a conventional map.

## Permissions

Location Dots requires Android permissions necessary for passive location tracking:

- **Precise location** — to record accurate locations.
- **Approximate location** — supported when precise location is not granted, with reduced accuracy.
- **Background location** — required so tracking can continue when the app is not open.
- **Physical activity recognition** — used to help detect movement and reduce unnecessary location updates.
- **Notifications** — required for the foreground tracking service and important tracking status on supported Android versions.

Permissions should be requested only when needed and explained clearly to the user.

## Privacy

Location data is highly sensitive. Location Dots is designed around privacy from the beginning.

- Local-first location storage.
- No account required for the initial version.
- No location data uploaded by default.
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

## MVP Features

- [ ] Automatic background location tracking
- [ ] Permission onboarding
- [ ] Location timeline
- [ ] Infinite scrolling history
- [ ] Day separators
- [ ] Place detection
- [ ] Visit duration
- [ ] Travel segments
- [ ] Distance and travel duration
- [ ] Pause/resume tracking
- [ ] Edit place names
- [ ] Delete location history
- [ ] Export location history
- [ ] Privacy/settings screen

## Future Features

- Day / week / month / year replay
- Search your location history
- Frequently visited places
- Frequent routes
- Travel mode detection
- Mini route maps
- Personal location statistics
- Encrypted backup
- Optional sync across devices

## Status

**Early development — MVP**

The first goal is simple:

> **Install → grant permission → live your day → come back and see your day as a timeline.**

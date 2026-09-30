# Location Dots

> **Every place leaves a dot.**

Location Dots is a privacy-first Android app that automatically turns your physical movements into a continuous visual timeline.

Install it, grant the required permissions, and live normally. Location Dots detects meaningful places and journeys and builds the timeline for you.

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

## Product vision

Location Dots is **not another conventional map or GPS tracker**.

The primary interface is an **infinite vertical timeline** representing the user's physical journey through time.

The core principle is:

> **Install once. Live normally. Your timeline builds itself.**

The app should require almost no interaction after setup.

---

## Core experience

Location Dots converts raw location signals into meaningful timeline events:

```text
Location samples
       ↓
Noise filtering
       ↓
Movement detection
       ↓
Stay detection
       ↓
Visit clustering
       ↓
Place resolution
       ↓
Timeline events
```

Instead of thousands of GPS points, the user sees something understandable:

```text
08:42  🏠 Home
09:13  🏢 Office
17:37  ☕ Cafe
18:02  🏋️ Gym
20:11  🏠 Home
```

---

# Timeline UI

The timeline is the identity of Location Dots.

```text
                         01 OCTOBER
                              │
                              ●
                         🏠 Home
                         08:42
                              │
                              │
                         🚗 31 min
                              │
                              ●
                         🏢 Office
                         09:13
                              │
                              │
                              │
                         8h 24m
                              │
                              ●
                         ☕ Cafe
                         17:37
                              │
                              │
                         🚗 14 min
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

                              ●
                         🏠 Home
                         08:31
                              │
                              ●
                         🏢 Office
                         09:22
```

### Timeline principles

- The timeline is one continuous scrollable history.
- Days are separated with a distinct dotted divider.
- Significant places become dots/events.
- Travel is represented by connecting segments.
- Raw GPS points are never exposed as the primary UI.
- A sticky date indicator can show which day is currently visible.
- Long-term history should remain easy to browse.

### Travel segments

A journey between places can show:

```text
🏢 Office
17:42

│
│ 🚗 5.8 km · 21 min
│
●

☕ Cafe
18:03
```

Travel mode may eventually include walking, bicycle, vehicle, transit, and unknown.

---

# MVP

The first version should stay focused on the core promise.

### Required

- [ ] Android app
- [ ] Location permission onboarding
- [ ] Background location tracking
- [ ] Activity/movement detection
- [ ] Stay/visit detection
- [ ] Meaningful place/event creation
- [ ] Local Room database
- [ ] Infinite chronological timeline
- [ ] Day separators
- [ ] Current-day indicator
- [ ] Basic place names
- [ ] Travel segments
- [ ] Timeline event details
- [ ] Pause/resume tracking
- [ ] Delete location history
- [ ] Export location history

### Not required for v0.1

- Cloud sync
- Accounts
- Social features
- AI assistant
- Public profiles
- Web dashboard
- Complex analytics

**The first milestone is:**

> Install → grant permission → move around normally → reopen the app → see the day automatically reconstructed as a timeline.

---

# Android stack

Location Dots should be Android-first.

Recommended stack:

- **Kotlin**
- **Jetpack Compose**
- **Material 3**
- **Room**
- **Kotlin Coroutines / Flow**
- **WorkManager**
- **Fused Location Provider**
- **Activity Recognition**
- **Foreground Service**

Suggested architecture:

```text
                    ┌─────────────────┐
                    │  Compose UI     │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ ViewModel       │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ Timeline        │
                    │ Repository      │
                    └────────┬────────┘
                             │
                 ┌───────────┴───────────┐
                 ▼                       ▼
          ┌─────────────┐        ┌─────────────┐
          │ Room        │        │ Location    │
          │ Database    │        │ Engine      │
          └─────────────┘        └──────┬──────┘
                                        │
                              ┌─────────┴─────────┐
                              ▼                   ▼
                       Fused Location      Activity Recognition
```

---

# Suggested project structure

```text
app/src/main/java/com/locationdots/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── entity/
│   │   └── LocationDotsDatabase.kt
│   ├── repository/
│   └── preferences/
│
├── location/
│   ├── LocationCollector.kt
│   ├── ActivityDetector.kt
│   ├── JourneyProcessor.kt
│   ├── VisitDetector.kt
│   └── PlaceResolver.kt
│
├── timeline/
│   ├── TimelineViewModel.kt
│   ├── TimelineScreen.kt
│   ├── TimelineItem.kt
│   ├── DaySeparator.kt
│   └── TravelSegment.kt
│
├── settings/
│   ├── SettingsScreen.kt
│   └── PrivacyScreen.kt
│
└── MainActivity.kt
```

This structure can evolve as the project grows. Keep the location engine, data layer, and timeline UI separated.

---

# Data model

Initial model:

```text
Day
 ├── id
 ├── date
 └── events

TimelineEvent
 ├── id
 ├── timestamp
 ├── type
 ├── placeId
 ├── latitude
 ├── longitude
 ├── duration
 ├── distance
 └── travelMode

Place
 ├── id
 ├── name
 ├── category
 ├── latitude
 ├── longitude
 └── radius
```

Potential event types:

```kotlin
enum class TimelineEventType {
    VISIT,
    TRAVEL,
    DAY_SEPARATOR
}
```

Potential travel modes:

```kotlin
enum class TravelMode {
    WALKING,
    BICYCLE,
    VEHICLE,
    TRANSIT,
    UNKNOWN
}
```

---

# Battery strategy

Continuous high-accuracy GPS would make the app unusable.

Location collection must be adaptive:

```text
                    Device state
                         │
              ┌──────────┴──────────┐
              │                     │
            STILL                 MOVING
              │                     │
        Low-power signals      Higher accuracy
              │                     │
              └──────────┬──────────┘
                         ▼
                  Journey engine
                         │
                         ▼
                  Detect destination
                         │
                         ▼
                  Return to low power
```

The app should:

- Prefer battery-efficient location signals.
- Use activity recognition to detect movement.
- Increase sampling only when movement is likely.
- Reduce expensive location requests while stationary.
- Batch processing where practical.
- Avoid unnecessary background wakeups.

A beautiful timeline that destroys battery life is a failed product.

---

# Privacy

Location history can reveal where someone lives, works, exercises, eats, and travels. Privacy is therefore a core product requirement.

### Default principles

- Local-first storage.
- No account required for the MVP.
- No cloud upload by default.
- No selling location data.
- Clear background-location explanation.
- User-controlled tracking.
- Easy pause.
- Easy deletion.
- Full history deletion.
- Data export.

Optional encrypted backup can be considered later.

---

# Development roadmap

## Phase 0 — Project setup

- [ ] Android project
- [ ] Kotlin
- [ ] Jetpack Compose
- [ ] Material 3
- [ ] Room
- [ ] Dependency setup
- [ ] Basic navigation
- [ ] CI/build verification

## Phase 1 — Timeline prototype

Build the UI using fake data before connecting GPS.

- [ ] Infinite vertical timeline
- [ ] Location dots
- [ ] Day separators
- [ ] Visit cards
- [ ] Travel segments
- [ ] Sticky date indicator
- [ ] Timeline animations
- [ ] Dark/light themes

## Phase 2 — Location collection

- [ ] Foreground location
- [ ] Background location
- [ ] Permission flow
- [ ] Location service
- [ ] Activity recognition
- [ ] Room persistence
- [ ] Basic movement detection

## Phase 3 — Journey engine

- [ ] Noise filtering
- [ ] Stay detection
- [ ] Visit creation
- [ ] Travel segment creation
- [ ] Place clustering
- [ ] Duplicate visit prevention
- [ ] Day grouping

## Phase 4 — Real places

- [ ] Reverse geocoding
- [ ] Place names
- [ ] Place categories
- [ ] User rename
- [ ] User correction
- [ ] Remember known places

## Phase 5 — Timeline polish

- [ ] Animated current position
- [ ] Expandable journeys
- [ ] Mini route maps
- [ ] Travel statistics
- [ ] Smooth long-range scrolling
- [ ] Timeline search
- [ ] Date jumping

## Phase 6 — Long-term features

Possible future features:

- [ ] Day replay
- [ ] Week replay
- [ ] Month/year view
- [ ] Most visited places
- [ ] Frequent routes
- [ ] Time spent by place
- [ ] Travel history
- [ ] Personal location insights
- [ ] Encrypted backup
- [ ] Data import/export

---

# Product principles

### 1. Passive first

The user should not have to maintain the timeline.

### 2. Timeline first

The timeline is the primary product, not a secondary view.

### 3. Meaning over raw data

Thousands of coordinates should become a handful of understandable events.

### 4. Privacy by default

Location data belongs to the user.

### 5. Battery matters

Background tracking must be engineered carefully.

### 6. No unnecessary AI

Do not add AI just because it sounds impressive. Use deterministic processing first and introduce ML/AI only where it provides a real improvement.

### 7. The app gets better automatically

The more history it collects, the more useful the timeline becomes.

---

# Visual identity

The central visual element is the dot and line:

```text
●
│
●
│
●
│
●
```

Potential tagline:

> **Every place leaves a dot.**

The visual system should remain clean, minimal, and calm. The timeline—not a map—should be the dominant visual element.

---

# Repository rules

Keep the repository:

- Clean
- Modular
- Documented
- Testable
- Privacy-conscious
- Battery-conscious
- Android-first

Never commit:

- API keys
- Signing keys
- Keystores
- User location data
- Production secrets
- Generated local databases

---

# Status

**Planning / MVP development**

Location Dots is currently focused on proving one idea extremely well:

> **Can a phone automatically turn someone's ordinary day into a beautiful, useful timeline of places?**

Everything else comes after that.

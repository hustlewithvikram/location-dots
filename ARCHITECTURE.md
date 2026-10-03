# Location Dots — How it works

This page explains the parts of Location Dots that are useful to understand when working on the project.

## The big picture

Location Dots takes raw location updates and turns them into a timeline:

```text
📍 Location updates
        ↓
   Save locally
        ↓
 Find meaningful stops
        ↓
   Create places
        ↓
 Find movement between them
        ↓
 Timeline + Maps + Insights
```

The important idea is that **location collection and timeline generation are separate**.

The tracker collects data first. The app then decides what that data means.

## Main parts

### Location tracking

The Android foreground service keeps collecting location updates while tracking is enabled.

It uses Google's location services and saves the received points locally.

This is what allows tracking to continue while Location Dots is not the screen currently open.

### Local storage

Location history is stored on the device using **Room**.

The app keeps three main kinds of information:

- **Location points** — recorded positions.
- **Places** — locations recognized as meaningful stops.
- **Timeline events** — visits and journeys shown to the user.

This keeps the raw data available while allowing the timeline to be rebuilt when the detection logic changes.

### Visit detection

The journey processor looks through recorded points and searches for clusters where the user appears to have stayed.

A stop currently needs:

- at least **5 usable location points**;
- around **8 minutes** of dwell time;
- locations that remain reasonably close together;
- mostly stationary movement.

If the cluster looks like a genuine stop, it becomes a **visit**.

The exact thresholds are implementation details and can change as detection improves.

### Place recognition

A visit does not always create a brand-new place.

The app calculates the center of the visit and checks whether a saved place is nearby.

If one is found, the visit is associated with that place.

Otherwise, a new place is created.

This is what allows repeated visits to places such as **Home, Office, Gym, or a favourite café** to build history around the same dot.

### Journey detection

When the app finds two visits with movement between them, it can create a journey.

Journeys contain:

- start and end places when available;
- start and end times;
- travelled distance;
- the recorded route;
- an estimated movement mode.

The current mode estimate is intentionally simple:

- slower movement → walking;
- medium movement → cycling;
- faster movement → vehicle.

It is an estimate, not a promise of exact transport recognition.

## What the user sees

The processing above feeds the main parts of the app:

```text
                  ┌── Timeline
                  ├── Places
Location data ────┼── Journey details
                  ├── Search
                  └── Insights
```

### Timeline

Shows visits and journeys in chronological order, grouped by day.

### Places

Shows recognized places on a map and lets the user open their history or rename them.

### Journey details

Shows the route, distance, duration, and estimated movement mode.

### Search

Lets the user find stored timeline/place information without manually browsing every day.

### Insights

Uses the stored visits and journeys to produce summaries about movement and time.

## App structure

The source code is grouped by responsibility:

```text
core/       → Android/platform helpers
data/       → Room database and repositories
domain/     → Location, places, journeys and business rules
feature/    → User-facing screens and ViewModels
service/    → Background location tracking
ui/         → Shared Compose UI and maps
logging/    → Diagnostics
```

A typical change should stay in the layer that owns it.

For example:

**Change how a visit is detected?** → `domain/journey`

**Change how places are stored?** → `data`

**Change the Places screen?** → `feature/map`

**Change a shared map component?** → `ui/components`

## Maps

Maps are provided by **MapLibre Native** using OpenFreeMap styles.

The shared map component handles:

- place markers;
- route lines;
- camera positioning;
- interactive maps;
- compact map previews.

Map configuration is documented separately in [MAPS_SETUP.md](MAPS_SETUP.md).

## Backup and restore

The app can export its local history to JSON.

The flow is:

```text
Local data
    ↓
Export JSON
    ↓
User chooses where to save it

Backup file
    ↓
Import
    ↓
Preview
    ↓
User confirms
    ↓
Restore local data
```

## Privacy boundary

Normal location tracking stores history locally.

There is currently no account or cloud location-history backend.

The map is different: it uses online map resources, so an internet connection may be needed to display map tiles/styles.

## In short

If you only remember one thing about the architecture:

> **Location Dots collects raw location data first, then turns it into meaningful places and journeys, and finally presents those results through the UI.**

That separation is what makes the location algorithm, storage, and UI easier to improve independently.

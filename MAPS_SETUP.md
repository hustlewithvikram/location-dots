# Maps setup

Location Dots uses **MapLibre Native** with **OpenFreeMap**. The application does not use the Google Maps SDK and does not require a Google Maps API key.

## Runtime configuration

The map implementation lives in:

```text
app/src/main/java/com/locationdots/app/ui/components/LocationMap.kt
```

The currently available OpenFreeMap styles are:

| App setting | Style URL |
|---|---|
| Liberty | `https://tiles.openfreemap.org/styles/liberty` |
| Bright | `https://tiles.openfreemap.org/styles/bright` |
| Positron | `https://tiles.openfreemap.org/styles/positron` |
| Dark | `https://tiles.openfreemap.org/styles/dark` |
| Fiord | `https://tiles.openfreemap.org/styles/fiord` |

`liberty` is the default.

## What the map component supports

`LocationMap` is shared by timeline, places, and journey-related UI and supports:

- interactive and non-interactive modes;
- route-line rendering;
- place-marker rendering;
- automatic camera fitting;
- map click callbacks;
- fullscreen place-map presentation;
- lifecycle-aware MapView handling.

The timeline uses compact, non-interactive maps for visit/journey previews. The Places screen provides an interactive overview and fullscreen map.

## Attribution

OpenFreeMap uses OpenStreetMap-based data. MapLibre's attribution UI is used by the application.

## Network behavior

Maps are the primary online dependency of the current app. Location history itself is stored locally in Room.

If the map provider is unavailable, location collection and local history do not depend on the map renderer to persist data.

## Production note

OpenFreeMap's public instance is useful for development and prototypes, but it does not provide an SLA.

If the project later needs guaranteed availability or higher-scale tile delivery, the map layer is intentionally isolated in `LocationMap.kt`. The provider/style URLs can therefore be replaced with another MapLibre-compatible source without redesigning the location database or domain model.

## Development

No map API key or `secrets.properties` map entry is required.

A normal Android build is sufficient to compile the map integration.

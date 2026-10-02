# Maps setup

Location Dots uses MapLibre Native with OpenFreeMap.

No Google Maps SDK, Google Maps API key, or map API secret is required.

## Map style

The app uses:

`https://tiles.openfreemap.org/styles/liberty`

OpenFreeMap provides the vector map style and OpenStreetMap-based data. Attribution is handled by MapLibre's built-in map attribution UI.

## Production note

OpenFreeMap's public instance is free and supports commercial use, but it does not provide an SLA. If Location Dots grows enough to need guaranteed availability, the map layer is isolated in `ui/components/LocationMap.kt` so the style URL can be switched to a self-hosted or commercial MapLibre-compatible provider without changing the app's location data model.

## Development

No `secrets.properties` entry is needed for maps.

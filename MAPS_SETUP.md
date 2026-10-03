# Maps

Location Dots uses **MapLibre Native** and **OpenFreeMap** to display maps.

You do not need a Google Maps API key.

## Where maps are used

Maps appear in:

- the Places overview;
- place history;
- journey details;
- timeline previews.

The shared map component is:

```text
app/src/main/java/com/locationdots/app/ui/components/LocationMap.kt
```

## Available styles

Users can choose from:

| Style | Look |
|---|---|
| **Liberty** | General-purpose map |
| **Bright** | Light and clean |
| **Positron** | Minimal light map |
| **Dark** | Dark map |
| **Fiord** | Darker blue-toned map |

**Liberty** is the default.

## Internet requirement

The location history itself is stored locally, but map styles and map data come from the internet.

So:

- tracking and local history do not depend on the map being visible;
- maps may not render correctly without an internet connection.

## Changing the map provider

The map implementation is kept in one shared component, so the provider can be changed later without redesigning the location-history system.

This is useful if the project eventually needs:

- a dedicated tile provider;
- self-hosted maps;
- higher availability;
- different map styling.

## Development

No map API key or special map secret is required.

Open the project in Android Studio and build normally.

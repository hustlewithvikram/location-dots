# Google Maps setup

Location Dots uses Google Maps Compose 8.6.0 for the journey route map.

1. Create or select a Google Cloud project.
2. Enable Maps SDK for Android.
3. Create an API key and restrict it to the Android app package and signing certificate.
4. Create `secrets.properties` in the repository root. This file is ignored by Git.
5. Add:

```properties
MAPS_API_KEY=YOUR_REAL_KEY
```

The app reads the key through the Google Secrets Gradle Plugin and exposes it to the manifest as `MAPS_API_KEY`.

If `secrets.properties` is absent, `local.defaults.properties` provides a build-time placeholder. The map itself requires a valid key at runtime.

References:
- https://developers.google.com/maps/documentation/android-sdk/get-api-key
- https://developers.google.com/maps/documentation/android-sdk/maps-compose

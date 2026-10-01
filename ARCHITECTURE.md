# Location Dots — Architecture

Step 2 establishes the application boundaries before real location tracking is implemented.

## Layers

- **core** — platform-facing primitives: location provider contract, permissions, notifications, navigation, common results.
- **data** — Room persistence, DAOs, entities, mappers, and repository implementations.
- **domain** — app concepts and business contracts. This layer does not depend on Android UI.
- **feature** — screen-level presentation. Timeline currently has its ViewModel boundary.
- **service** — Android service entry points for tracking, journey processing, and notifications.

## Data flow

`GPS/provider → LocationProvider → LocationRepository → Room`

`Room → Repository → Domain models → TimelineViewModel → Compose UI`

Journey processing will later sit between collected location points and meaningful timeline events:

`Location points → JourneyProcessor → Visits/Journeys → Timeline`

## Planned feature packages

`feature/onboarding`
`feature/timeline`
`feature/map`
`feature/place`
`feature/search`
`feature/insights`
`feature/settings`
`feature/tracking`
`feature/appearance`
`feature/privacy`
`feature/data`
`feature/photos`
`feature/profile`

No real background location collection is enabled in Step 2. The service and provider boundaries are intentionally present so the implementation can be added without restructuring the project.

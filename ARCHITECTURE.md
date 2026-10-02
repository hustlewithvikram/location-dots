# Location Dots — Architecture

The application uses clear boundaries between platform services, persistence, domain processing, and Compose presentation.

## Layers

- **core** — platform-facing primitives: location provider contract, permissions, notifications, navigation, common results.
- **data** — Room persistence, DAOs, entities, mappers, and repository implementations.
- **domain** — app concepts and business contracts. This layer does not depend on Android UI.
- **feature** — screen-level presentation. Timeline currently has its ViewModel boundary.
- **service** — Android service entry points for tracking, journey processing, and notifications.

## Data flow

`GPS/provider → LocationProvider → LocationRepository → Room`

`Room → Repository → Domain models → TimelineViewModel → Compose UI`

Journey processing sits between collected location points and meaningful timeline events:

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

Background location collection is implemented through the foreground location service and persists location points locally before journey/place processing.

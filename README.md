# Street Viewer (Android)

A personal GPS tracker for Android. It records where you move, uploads the track to a small
self-hosted backend, and shows each day's route on a map along with the places you stayed and
the streets you've covered on foot, either by walking or running.

The backend lives in a separate repo: **[Street_viewer_api](https://github.com/DanielOfRivia/Street_viewer_api)**.

## Features

- **Background tracking**: a foreground service records a GPS fix every 30 seconds (and only
  after moving 10 m). Fixes with accuracy worse than 50 m are dropped.
- **Battery-aware**: with the optional *Physical activity* permission, GPS switches off while
  you're stationary and comes back when you start moving (Activity Recognition Transition API).
- **Offline-first**: points are stored on-device (Room) and uploaded hourly by WorkManager,
  or on demand with *Sync now*. Synced points are kept locally for 30 days.
- **Day map**: pick any day to see its track, direction arrows, and the places you stayed
  (detected server-side, with address and businesses at that spot).
- **Street coverage**: streets you've walked are coloured on the map. Street geometry comes
  from OpenStreetMap via the public [Overpass API](https://overpass-api.de/). Driving and
  other fast travel are ignored. The speed limit is hardcoded and set to 15 km/h by default.
- **Survives reboots**: if tracking was on, it restarts after boot, or posts a notification
  to resume it when Android doesn't allow a location service to start in the background.

## Requirements

- Android Studio (recent stable; AGP 9.x, Kotlin 2.2)
- An Android 10+ device or emulator (minSdk 29) with Google Play services
- A Google Maps SDK for Android API key
- A running instance of the [backend](https://github.com/DanielOfRivia/Street_viewer_api)

## Setup

1. Clone the repo and open it in Android Studio.
2. Create `local.properties` in the project root (it's gitignored; Android Studio usually
   creates it with `sdk.dir` already set) and add:

   ```properties
   # Google Maps SDK for Android key (restrict it to this app's package + signing SHA-1)
   MAPS_API_KEY=your-maps-sdk-key

   # Backend base URL for debug builds (trailing slash required).
   # Default when omitted: http://10.0.2.2:8080/ (the host machine, from the emulator).
   BASE_URL_DEBUG=https://your-tunnel.ngrok-free.app/

   # Must equal the backend's API_KEY; sent as the X-API-Key header.
   NGROK_SECURE_API_KEY=your-backend-api-key

   # Optional: backend URL for release builds.
   # BASE_URL_RELEASE=https://api.example.com/
   ```

3. Start the backend (see its README), then run the `app` configuration.
4. In the app, tap **Start** and grant location, notifications and (optionally) physical
   activity.

To reach a backend on your computer from a physical phone, expose it with a tunnel such as
[ngrok](https://ngrok.com/) and put the tunnel URL in `BASE_URL_DEBUG`.

## Permissions

| Permission | Why |
|---|---|
| Fine / coarse location | Recording the track (foreground service, while-in-use) |
| Notifications | The ongoing "tracking" notification |
| Physical activity | *Optional.* Turns GPS off while stationary to save battery |
| Boot completed | Resuming tracking after a restart |

Background location permission is deliberately not requested.

## Project structure

```
app/src/main/java/.../street_viewer_v2/
├── data/      Room database, Retrofit API, Overpass client, repository implementations
├── di/        Hilt modules
├── domain/    Models, repository interfaces, geometry and track-processing logic
├── service/   Tracking foreground service, sync worker, activity monitor, boot receiver
└── ui/        Compose screens: tracking controls and the day map
```

## Tests

```bash
./gradlew testDebugUnitTest          # JVM unit tests
./gradlew connectedDebugAndroidTest  # instrumented tests (needs a device or emulator)
```

## License

[MIT](LICENSE)

# Drinkly apps

Native apps for the Drinkly bottle:

- `android/`: Kotlin + Jetpack Compose (min. Android 8.0, API 26)
- `ios/`: Swift + SwiftUI (min. iOS 17)
- `index(2).html`: the web view

Both apps have the same four tabs:

| Tab | Content |
| --- | --- |
| Today | Today's intake, hydration status (shown by color), streak, bottle connection and battery |
| Activity | 7/30-day chart, average, best day, goal days, longest streak, recent drinks |
| Reminders | Reminder plans (off / every 3 h / 2 h / 1 h), active hours, "only when I'm behind" |
| Profile | Name, daily goal (with a weight-based suggestion), bottle, backend address |

The hydration status compares today's intake with the share of the goal that should be done by now,
spread evenly over the active hours: blue = on track, amber = slightly behind, orange = behind, green = goal reached.

## Backend

The apps use the Quarkus backend in `../backend` (`GET/POST /entries`, `GET /entries/{user}`,
`DELETE /entries/{id}` and the `/events` stream for live updates). Start it with `./mvnw quarkus:dev`.

Default backend address (can be changed in Profile → Server):

- Android emulator: `http://10.0.2.2:8080`
- iOS simulator: `http://localhost:8080`
- Real phone: `http://<your computer's LAN IP>:8080`

## Bottle

The app receives the bottle's data over Bluetooth and forwards every sip to the backend.
Until the bottle's Bluetooth (GATT) profile is defined, a simulated bottle is used
(`SimulatedBottle`; Profile → Bottle → "Simulate a 250 ml sip"). A real implementation only needs to
implement the `BottleConnection` interface/protocol.

## Run

**Android:** open `android/` in Android Studio, or run `./gradlew installDebug`.

**iOS:** open `ios/Drinkly.xcodeproj` in Xcode 16 or newer, pick your team under
Signing & Capabilities, and run it.

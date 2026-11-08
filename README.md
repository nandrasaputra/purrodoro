# Purrodoro

Purrodoro is a Pomodoro focus timer with tasks, habits, focus history, and
soundscapes. This repository contains the Android app, an iOS port, and
supporting tools and backend migrations.

## Projects

- [`purrodoro-android/`](purrodoro-android/) — Kotlin and Jetpack Compose app.
- [`purrodoro-ios/`](purrodoro-ios/) — SwiftUI port; see [port status](purrodoro-ios/STATUS.md).
- [`tools/soundscape-admin/`](tools/soundscape-admin/) — local-only catalog administration.
- [`supabase/migrations/`](supabase/migrations/) — database schema and catalog migrations used by supported cloud features.

## Android development

Install Android Studio, an Android SDK and a compatible JDK, then from
`purrodoro-android/` run:

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew installDebug   # requires a connected device or emulator
```

The current Android app stores task and history data locally. See the
[Android release readiness notes](purrodoro-android/docs/release-readiness.md).

## iOS development

On macOS with Xcode and an installed iOS simulator, from `purrodoro-ios/`:

```sh
xcodebuild -scheme Purrodoro -destination 'platform=iOS Simulator,name=iPhone 17' build
xcodebuild test -scheme Purrodoro -destination 'platform=iOS Simulator,name=iPhone 17' -only-testing:PurrodoroTests
```

Simulator availability and cloud credentials depend on your local setup. iOS
features and release readiness can differ from Android; consult the port status.

## More information

- [Product reference](docs/PRODUCT.md)
- [Cloud asset management](docs/CLOUD_ASSET_MANAGEMENT.md)
- [Android privacy policy](docs/PRIVACY_POLICY.md)
- [Account deletion information](docs/DELETE_ACCOUNT.md)

## Licensing and assets

No project license has been specified yet. Do not assume permission to reuse or
redistribute the code, branding, audio, or other assets; the owner must confirm
the applicable terms and any third-party attribution requirements. The bundled
Nunito fonts have their own [SIL Open Font License](LICENSES/Nunito-OFL.txt).

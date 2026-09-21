# FL-Converter
A remake of FLP for FLM that I made before, but even more improved and with added new elements.

## Stack
Kotlin Multiplatform with Compose Multiplatform, targeting Android, iOS and Desktop (Windows, macOS, Linux). No web target.

## Layout
- `composeApp/src/commonMain` shared UI, view model and conversion domain
- `composeApp/src/androidMain` Android entry point and file access
- `composeApp/src/desktopMain` Desktop entry point and file access
- `composeApp/src/iosMain` iOS entry point and file access
- `iosApp` SwiftUI host sources for the Xcode project

## Run
- Desktop: `gradle :composeApp:run`
- Android: `gradle :composeApp:installDebug`
- iOS (macOS only): `brew install xcodegen`, then `cd iosApp && xcodegen generate` and open `iosApp.xcodeproj`

Requires JDK 17 or newer and the Android SDK for the Android target.

## CI
`.github/workflows/build.yml` builds on every push and pull request:
- Windows `.msi` and `.exe`, macOS `.dmg`, Linux `.deb`
- Android debug `.apk`
- iOS unsigned `.ipa` (must be re-signed to install on a device)

Everything is uploaded as workflow artifacts. There is no release publishing.

## Icon
The source is `assets/icon.svg`. The generated PNG, ICO, ICNS, Android and iOS variants live in `composeApp/icons`, `composeApp/src/androidMain/res`, `composeApp/src/desktopMain/resources` and `iosApp/iosApp/Assets.xcassets`.

## Status
The FLP codec reads and writes the FLP container (header and data chunk). The FLM codec is a stub in `FlmCodec`, so conversions currently report that FLM support is not implemented.

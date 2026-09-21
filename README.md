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

### Android signing
Without a proper signing key the APK is debug-signed and Google Play Protect is likely to warn about it. To sign the release APK:

```
keytool -genkeypair -v -keystore release.jks -alias flconverter -keyalg RSA -keysize 4096 -validity 10000
base64 -w 0 release.jks
```

Add these repository secrets: `ANDROID_KEYSTORE_BASE64` (the base64 output), `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. Keep `release.jks` backed up and never commit it: every future update must be signed with the same key.

With the secrets set, the workflow builds a signed release APK, verifies the signature with `apksigner`, fails if the debug certificate is used or unexpected permissions appear, and uploads a SHA-256 checksum next to the APK.

## Icon
The source is `assets/icon.svg`. The generated PNG, ICO, ICNS, Android and iOS variants live in `composeApp/icons`, `composeApp/src/androidMain/res`, `composeApp/src/desktopMain/resources` and `iosApp/iosApp/Assets.xcassets`.

## How conversion works
FL Converter transfers the notes between the two formats. Instruments, mixer and effects live in project data that only the target application understands, so they are never invented:

- **With a base project** (recommended): pick a project of the target format. The result is that project with its notes and playlist replaced. Instruments, mixer, effects and audio are kept byte for byte.
- **Without a base project**: a built-in default is used. FLP gets one empty sampler channel per source channel, named after it. FLM gets a single instrument channel, and every source channel is merged into it. Assign the instruments in the target app afterwards.

Other rules:
- Source channels can be included or excluded before converting.
- Each source channel with notes is mapped, in order, to a note-capable channel of the base. When the base has fewer channels, the extra source channels are merged into the last one and the app reports it.
- Overlapping clips that end up on the same FLM channel are merged into one clip.
- FLM clips become FLP patterns with playlist items, and FLP playlist items become FLM clips.
- Tempo is copied. Note timing is exact at 96 ticks per beat. Velocity is quantized to 0-127.

## Status
Both directions run, with and without a base project, and were checked against real projects: every note is preserved (position, length and key) when a converted file is read back. Files were not opened in FL Studio or FL Studio Mobile, so please test the output and report any problem.

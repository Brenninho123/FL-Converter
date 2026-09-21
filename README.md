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

## Status
Both formats can be read: the FLP codec reads the container and its notes (tempo, patterns, notes) and the FLM codec reads tempo, channels, clips and notes. Writing is not implemented yet: FLP output from FLM notes and any FLM output are reported as unsupported instead of producing a file.

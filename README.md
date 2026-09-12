# Ethic QR Scanner

Ethic QR Scanner is an original Android QR and barcode scanner built around a
simple rule: decoding a code must not silently trigger an external action.

It scans using the device camera, decodes locally, and presents the result for
the person to review. HTTP(S) links require a separate choice before Android is
asked to open a browser.

## Privacy commitments

- No account, analytics, advertising, network permission, or embedded web view.
- The scanner opens directly; Android requests camera access when the scanner
  starts because the camera is its primary function.
- A photo can be selected through Android's system picker; it is decoded in
  memory without storage permission and is not saved by the app.
- Frames are decoded in memory and are not saved or transmitted.
- No scan history is included in version 1.0.0.
- Copying a result uses Android's local clipboard service; the system may expose
  clipboard data to the current keyboard or other system components according
  to the device's own privacy rules.

## Provenance and licensing

This is an independent implementation. It does not include source code, visual
assets, brand, text, screenshots or icons from SECUSO's Privacy Friendly QR
Scanner or another QR scanner.

Application code is GPL-3.0-or-later. It uses CameraX (AndroidX) and ZXing Core
for local decoding; dependency versions and their licenses must be reviewed
again before any release or F-Droid submission.

The name **Ethic QR Scanner** is provisional. A basic web search found no exact
application-name collision, but that is not a trademark clearance.

## Build from source

This repository includes its Gradle wrapper and can be built without relying on
another project in the workspace:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
./gradlew :app:assembleRelease :app:lintVitalRelease
```

The release APK is unsigned by design; F-Droid builds and signs its own
artifacts from the published source and metadata.

## Status

The source is prepared for public review. A local build passing does not imply
F-Droid acceptance; maintainers must review licensing, dependencies, metadata
and reproducibility.

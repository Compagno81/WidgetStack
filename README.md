# Widget Stack

Android stacks of live third-party widgets. Recovered from the owner-supplied
1.14 Android Studio archive; new work is reconstruction, not historical 1.18 source.

See [requirements](docs/WidgetStack-Requirements-and-Acceptance-Criteria.md),
[recovery provenance](docs/SOURCE-RECOVERY.md), and
[candidate notes](docs/RELEASE-CANDIDATE.md).

Custom sizing uses the largest aspect-preserving fit within each home-screen
copy. When prompted, tap **Tap to fit this stack** once on the home screen; tap
again after resizing that copy. Auto needs no calibration. See
[custom sizing details](docs/CUSTOM-FITTING-1.23.md).

## Build

Use JDK 17, Android SDK 35, Build Tools 35.0.0, and the included Gradle 8.11.1
wrapper. Configure the SDK through ANDROID_HOME or an ignored local.properties.
Run `./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleRelease`.
For a test APK, restore your historical testing key locally from the supplied
archive into signing/testing-debug.keystore and run `./gradlew :app:assembleDebug`.
Keys are excluded from Git. A clean checkout can compile and test without that
key; debug APK signing requires it. Do not generate a replacement and assume it
will upgrade an existing installation.

Every tagged release includes an APK, matching source, commit, checksum and
validation notes. Configure owner signing secrets as described in the candidate
notes before running the release workflow. No validated new release is claimed
until its required checks pass; physical Pixel/HyperOS testing remains necessary.

The source retains private Android API integration and its compatibility risks.
Original project documentation is under docs/recovered-1.14/README.md.

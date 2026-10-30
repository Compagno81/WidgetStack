# 1.21 reconstructed test candidate — Auto layout correction

The 1.20 APK did not resolve the owner's MyFitnessPal screenshot: in Auto mode,
provider content still had margins on all four sides within the launcher border.

The renderer used launcher option sizes as fixed display bounds even for Auto.
Launcher options can differ from the size actually measured by the launcher.
Auto now embeds the live provider RemoteViews directly in the match-parent content
area, allowing Android's parent layout to determine display bounds. No fixed-size
scaling frame is added for fully automatic entries with no legacy percentage.
Provider content remains live; its controls and responsive layout are preserved.
Navigation's reserved space is still managed by the existing outer stack layout.

A production-renderer regression measures a 900 × 720 px launcher area while its
options use the smaller fallback dimensions. It failed on API 31 and 35 before
the change because provider bounds remained smaller. The new path must reach the
measured content bounds. The test uses a controllable MATCH_PARENT provider,
not a copy of MyFitnessPal, and is not a physical-device confirmation.

Custom dimensions and historical percentage settings retain their separate
aspect-preserving fit path. They still depend on launcher-reported sizes; this
release does not claim to solve reporting discrepancies for custom sizing.
Provider-internal padding is preserved. Fixed-size provider internals can still
limit native Auto layouts; content is not heuristically cropped or distorted.

Validation: 197 tests passed, with no failures/errors/skips. The production-renderer Auto regression now passes on API 31 and 35. Debug APK compilation and lint passed (zero errors, 969 warnings). No physical Pixel/HyperOS or
MyFitnessPal run has been performed here. This is a test candidate, not a claim
that the owner's screenshot has been resolved on the device.

The APK uses the same recovered testing signing key as 1.19/1.20; signing keys
are excluded from source. Reconstructed from supplied 1.14 source. Each release
has an APK, source ZIP, source commit, checksums and validation notes on GitHub.

Build: JDK 17, Gradle 8.11.1, Android SDK 35 / Build Tools 35.0.0. Local test
runner cache/proxy/trust settings are external to source. The automated signed
release workflow requires owner secrets ANDROID_KEYSTORE_BASE64,
ANDROID_STORE_PASSWORD, ANDROID_KEY_ALIAS and ANDROID_KEY_PASSWORD.

APK SHA-256: `d99cf76ba739d2f0cb81ec1cb37c3d5d4e200fe7179d47398b694a846baa601c`.

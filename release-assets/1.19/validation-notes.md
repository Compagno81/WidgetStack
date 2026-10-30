# 1.19 reconstructed candidate — not a validated release

Reconstructed from the supplied 1.14 archive. This is not the original source of
1.18, and that binary has not been supplied for comparison.

Changes: independent width/height pixels and Auto, 50 px sliders, exact fields,
non-interactive single-provider sizing preview with draggable bounds, draft Apply/
Cancel, density-aware provider options and uniform fit allowing enlargement.
Historical percentage sizes remain active until dimensions are applied.

Stored pixel values remain physical pixels if screen density changes; Android
provider dimensions are rounded to dp at the current density. Explicit values
are limited to 1–4000 px, with zero stored internally only as Auto.

Validation: 193 unit/Robolectric tests passed on API 31 and 35; debug APK compilation and lint passed (zero errors, 969 warnings). The APK signature and version metadata were verified. Lint warnings remain, including legacy UI/resource issues; this is not a store-ready release. No emulator/instrumentation or physical-device run was performed. Pixel and HyperOS home-screen behavior, scaled input,
provider responsive layouts, contraction and upgrade compatibility require
physical-device checks. Existing private API compatibility risks remain.

Every tagged release must include an APK, source ZIP, source commit, checksums
and validation notes. The workflow requires owner-controlled signing secrets:
ANDROID_KEYSTORE_BASE64, ANDROID_STORE_PASSWORD, ANDROID_KEY_ALIAS and
ANDROID_KEY_PASSWORD. Reuse the intended signing identity to retain upgrade
compatibility. Never put those values or keys into source control. The historical
testing key is only in the owner-supplied archive, not in Git.

Workflow dispatch creates a downloadable build artifact; a v-prefixed tag creates
a GitHub prerelease with matching assets. Do not tag a candidate as a validated
release until its required acceptance checks pass.

APK SHA-256: `bc8a9fd3c49cd3492168ab19211aed627d7e25564a4ebf9691a8dab1098befca`.

The test APK uses the historical testing key recovered locally from the supplied
archive. Its certificate SHA-256 is
`eb7cc6e833e7221ea3fab1fde4e64b2a2c21e57765a8d48b1cd651d3e44ae925`.
Compatibility with the unsupplied 1.18 APK cannot be confirmed.

Home rendering uses launcher-provided size variants where available. Launchers
that provide only legacy min/max options necessarily use those estimates.

Build environment: JDK 17, Gradle 8.11.1, Android SDK 35 / Build Tools 35.0.0.
The sandbox required a local Gradle init script to move Robolectric caches to a
writable directory and use the environment proxy/certificate trust. No such
machine-specific settings are committed.

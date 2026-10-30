# 1.20 reconstructed test candidate

Fixes a reproduced fitting defect: a provider may return a fixed-size or
wrap-content root smaller than the requested custom layout rectangle. Version
1.19 fitted that rectangle and could leave avoidable space around the provider.
This version measures the returned root at the requested constraints and fits
its complete bounds uniformly to the available area, allowing enlargement and
reduction. Custom provider size options and saved per-widget dimensions remain
unchanged. No descendant-content detection, padding removal or cropping is used.
If measurement requires a real attached host or fails, the requested-size fitting
path remains the fallback rather than replacing working content with an error.

Regression: a 120 × 80 dp provider root requested inside 200 × 100 dp and
placed in 600 × 400 dp must use a 5× scale and fill both axes. The regression
failed on API 31 and 35 before the change. Existing MATCH_PARENT-root fitting
and independent dimensions remain covered.

Validation: 195 tests passed, zero failures or skips, including the new regression on API 31 and 35. Debug APK compilation and lint passed, with zero lint errors and 969 warnings. No emulator/instrumented or physical-device test was performed. Device confirmation of
the reported screenshot is still required. Internal padding in provider content
is preserved. Launchers that supply only legacy min/max size options still use
those estimates; no actual device compatibility claim is made by unit tests.

This is reconstructed work based on recovered 1.14 source, not historical 1.18
source. The test APK is signed with the same recovered testing key as 1.19;
release signing keys remain excluded from Git. All new releases include a test
APK, matching source ZIP, source commit, checksums and these validation notes.

Build: JDK 17, Gradle 8.11.1, SDK 35 / Build Tools 35.0.0. Local-only test runner
settings route cache writes and network/certificate trust through the managed
environment; they are not committed to the app project.

The release workflow requires owner-controlled GitHub secrets:
ANDROID_KEYSTORE_BASE64, ANDROID_STORE_PASSWORD, ANDROID_KEY_ALIAS,
ANDROID_KEY_PASSWORD. Retain the intended signing identity for upgrades.

APK SHA-256: `6481197716aaa848a6959fc1264051aa4f52d67262cd1ae1e84c907fc367d2b1`.

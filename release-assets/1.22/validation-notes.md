# 1.22 reconstructed test candidate — remove hidden legacy sizing

The owner reports that 1.21 still leaves four-sided gaps around MyFitnessPal
with both dimensions on Auto. The device report remains the acceptance case.

Further testing found a distinct product defect: old logicalSizePercent data
could remain at 150 while the current width and height controls both displayed
Auto. 1.21 used that hidden value to select the fixed fitting wrapper. A new
production-host regression failed on API 31 and 35 with that saved state.

Auto now means both dimensions are automatic regardless of historical percent
records. Provider size options also ignore the obsolete multiplier. Legacy
records remain preserved for recovery, but do not affect rendering. The editor
explains the transition. Explicit dimensions and aspect-preserving custom fitting
remain available, with their existing launcher-option limitations.

Validation: 205 unit/framework tests passed, with no failures/errors/skips. This
includes actual provider bounds within AppWidgetHostView, 64 rail geometry
combinations across API 31/35, resizing without republishing, attached-window
edge taps, and the new legacy-settings regression. Debug APK, instrumentation
APK and lint builds passed (no lint errors; existing warnings remain).

Android runtime validation: two instrumented tests passed on Android 12/API 31,
using a separately installed provider. One host runs in a separate UID/process.
Across six resize cases, tests assert actual provider screen bounds, exact
background pixels at the edge, and PendingIntent delivery from real edge taps.
The cross-UID case carries a retained 150% legacy value with both axes on Auto.
The same case fails on the old 1.21 APK. An old-build capture measured 80 × 60
inside a 400 × 300 host. Logs, geometry and screenshots are committed under
`docs/evidence-fit-1.22/android31/`.

The host used for these checks is a controlled framework host, and the provider
has a MATCH_PARENT root. This does not prove arbitrary fixed provider internals
or incorrect OEM host constraints can be stretched. No assertion is relaxed to
make a failing product case pass; setup/lifecycle issues were corrected and
screenshot sampling waits for a stable composed frame.

No physical HyperOS/MyFitnessPal pass is claimed. Provider internals are not
cropped. A fixture/host pass alone does not establish that the owner's exact
provider and launcher combination is resolved.

Signing: same recovered testing key as 1.19–1.21, excluded from Git. This is
reconstruction from supplied 1.14 source, not historical 1.18 source. Each release
includes its APK, matching source ZIP, source commit, checksum and these notes.
Toolchain: JDK 17, Gradle 8.11.1, SDK 35 / Build Tools 35.0.0.

The signed-release workflow requires owner GitHub secrets ANDROID_KEYSTORE_BASE64,
ANDROID_STORE_PASSWORD, ANDROID_KEY_ALIAS and ANDROID_KEY_PASSWORD.

APK SHA-256: `935d585b834378ec894c0865393ad13179ef07075a8ee47f5ee0c52146382337`.

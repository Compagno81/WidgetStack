# 1.23 reconstructed test candidate — fit custom sizes to measured bounds

The owner confirms Auto fills the available space, while changing width,
height, or both introduces gaps across widget providers. The custom fitting
path used launcher option hints as its target instead of the actual laid-out
area. The preview design is unchanged.

Custom sizing now uses a per-placement physical measurement. On the home
screen, tap **Tap to fit this stack** when prompted. This supplies the actual
area through Android's click bounds, then displays the interactive provider.
Width/height edits reuse that measurement. Resizing a copy asks for another
tap; each copy is independent. Auto needs no tap. There is no persistent
overlay, provider cropping, or distortion. Mismatched aspect ratios can leave
space on one pair of sides; at least one pair reaches the content edges.

An automatic RemoteViews measurement attempt was rejected by Android's remote
method restrictions, so this candidate makes the calibration step explicit.
See docs/CUSTOM-FITTING-1.23.md for behavior and lifecycle details.

Validation: 219 unit/framework tests passed on API 31/35 with no failures,
errors or skips. New checks cover per-placement measurements, stale/empty tap
rejection, option/density changes, 72 actual custom layout combinations at mdpi
and xhdpi, and 16 attached-host edge taps while enlarging/reducing. Debug APK,
instrumentation APK and lint builds passed: no lint errors, 975 warnings.

Four real Android 12/API 31 instrumentation tests passed. The same custom-size
regression fails on the old 1.22 APK (80 × 60 inside 400 × 300) and passes on
1.23 (400 × 300). Another case checks all 18 combinations of width-only,
height-only and both dimensions across three host shapes, including exact edge
pixels and real PendingIntent edge taps. Auto passes three shapes in an
external host and three more in-process. Source logs, screenshots and geometry
are under docs/evidence-fit-1.23/. Harness checks wait for input focus and
completed layout before injecting taps or sampling positions; exact pixel and
PendingIntent assertions remain required.

No physical HyperOS or MyFitnessPal pass is claimed. Android runtime evidence
uses a controlled host and separately installed provider. Keep issue #1 open
until the owner confirms custom sizing on the phone.

Signing: same recovered testing key as 1.19–1.22, excluded from Git. Source is
reconstructed from the supplied 1.14 archive. The release includes its APK,
matching source ZIP, source commit, checksum and these notes. Toolchain: JDK 17,
Gradle 8.11.1, SDK 35 / Build Tools 35.0.0.

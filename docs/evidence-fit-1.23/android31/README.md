# Android 12 custom fitting verification

The controlled host and provider are separately installed in the fixture APK;
the stack runs under another UID. The fixture host measures its laid-out views
on its UI thread. Its option hints deliberately remain 80 × 60 dp while its
actual size is 400 × 300, then 300 × 400 and 500 × 200 physical pixels at mdpi.

The same matching-aspect regression fails on the old 1.22 APK: expected width
400, actual width 80. The old screenshot shows an 80 × 60 provider inside the
400 × 300 host. The new APK passes after a real tap on its full-size calibration
surface: the provider measures 400 × 300. Pixel bounding boxes are in
comparison-pixels.json; the old screenshot's composed color differs during the
window transition, so its observed color is recorded rather than relabeled as
the fixture's original background color.

The custom axes test passes all 18 combinations of three host shapes and six
logical sizes: width-only 200/800 px, height-only 150/600 px, and both 200 × 150
or 800 × 600 px. It asserts transformed provider bounds, centered placement,
aspect-preserving fitting, exact blue background pixels near the visible edge,
and PendingIntent delivery from an injected tap there. Resize notifications
invalidate the measurement and a real tap captures the new area. No synthetic
measurement Intent is used in these runtime cases.

Auto passes three shapes in the external host and three more in a host inside
the stack process. The external Auto case also retains obsolete 150% sizing
data. Both Auto tests check exact edge pixels and PendingIntent taps. All four
positive instrumentation logs end in OK (1 test).

The harness waits for the external host to regain input focus after Back and
for pending layout to finish before sampling screen coordinates. Background
pixel probes avoid text glyphs. These checks retain exact color assertions and
real touch delivery; a fixture setup or focus failure is not counted as a pass.

Build/install the fixture and instrumentation APKs, grant BIND_APPWIDGET to
com.kevin.widgetstack and com.kevin.widgetfixture with adb appwidget grantbind,
and run these methods with the platform InstrumentationTestRunner:

- CrossUidControlsTest#testCustomMatchingAspectUsesEntireActualArea
- CrossUidControlsTest#testCustomDimensionsFitActualBoundsAcrossUids
- CrossUidControlsTest#testAutoActualBoundsAndEdgeTapFromAnotherUid
- WidgetIntegrationTest#testAutoUsesActualLauncherBoundsAndRetainsEdgeClicksAfterResize

Each class is in com.kevin.widgetstack. The first method runs against the old
1.22 APK as a negative control using the new instrumentation APK. The other
methods run against 1.23. APK signing certificates match, enabling upgrade and
test downgrade without changing the recovered testing key.

This is a controlled framework host with a MATCH_PARENT provider root, not a
physical HyperOS/MyFitnessPal verification. Keep repository issue #1 open for
that device acceptance case.

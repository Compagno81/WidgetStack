# Agenda gesture investigation, 14 November 2026

Baseline: WidgetStack 1.42, source commit 98a3fe91485686a885046b3d8fc61ca4efa4a70b.

## Reported device behavior

The user confirmed 1.42 restored custom-widget loading. On Xiaomi Android 17/API 37, Trace Calendar agenda does not move at all in a directly placed WidgetStack; launcher swipe actions take over. The list has more content to scroll. Expanding or collapsing the bar makes no difference. The agenda inside WidgetStack inside Widget Canvas scrolls successfully. The user did not change the canvas to obtain this result.

## Reproducible tests

Run `./gradlew :app:testDebugUnitTest --tests '*AgendaGestureTest*'`.

The debug-only fixture uses Trace Calendar 1.15's calendar_widget layout, with IDs renamed to avoid fixture collisions. Rows are 100 synthetic native RemoteCollectionItems, with click fill-in intents and a pending-intent template. These are not private calendar records or the complete Trace Calendar APK. Every experiment parcels the RemoteViews before application and sends DOWN, MOVE and UP events through an attached AppWidgetHostView. Each confirms native list binding and actual overflow, then checks the visible row position. It does not call setSelection to simulate a swipe.

24 tests passed, zero failures/errors/skips, on Robolectric Android APIs 31 and 35:

- Direct agenda: scrolls.
- Normal navigation rail: scrolls.
- Overlapping rail: scrolls.
- Corner arrows: scrolls.
- Contracted corner control: scrolls.
- Expanded auto-contract bar: scrolls.
- 75% visual scale: scrolls.
- Explicit 400 x 500 logical pixel size: scrolls.
- Extra plain outer FrameLayout: scrolls. This is a structural probe, not the actual Canvas implementation.
- A synthetic parent that intercepts vertical MOVE events blocks scrolling without host protection.
- In that same parent, host protection on DOWN after finding a nested AdapterView prevents takeover and permits scrolling.
- The actual Stack placeholder is not detected as containing an AdapterView; the populated Stack is.

The last three experiments demonstrate a possible mechanism. They do not prove Xiaomi caches scrollability, uses the same detection algorithm, or fails to protect the child. A simulated parent deliberately stealing a gesture is not a reproduction of Xiaomi's complete implementation.

## Reference and limits

AOSP Launcher3 scans the child hierarchy for AdapterView during layout and requests that its parent not intercept on DOWN when classified scrollable:
https://android.googlesource.com/platform/packages/apps/Launcher3/+/6df1743d23595bc9b800c9e81fcaae085ad47951/src/com/android/launcher3/widget/LauncherAppWidgetHostView.java

The harness models that protection boundary in a small test host. It does not run Launcher3, Xiaomi's launcher, or Android 17. The initial-placeholder/populated-list transition makes stale host classification a hypothesis, not a finding about Xiaomi. The fixture does not cover numeric resource-ID collisions between separately built APKs, provider-specific data, every size setting or launcher lifecycle transitions. Canvas source was not recovered from connected records.

No production change or new APK was issued. The exact device failure and a verified fix remain unresolved. Future changes should reproduce or instrument the host classification/touch boundary before treating a marker ListView, wrapper replacement or lifecycle refresh as a solution; those could suppress normal launcher gestures unnecessarily.

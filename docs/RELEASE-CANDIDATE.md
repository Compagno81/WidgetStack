# 1.32 — single-layout switching and read-only rendering

This returns to normal navigation work. It does not claim that configuration
access was broken or that pausing widgets fixes calendar switching.

Home-screen publication now sends one RemoteViews layout for the selected
widget with a fresh root identity, without an outer responsive-size map.
Provider layout selection still uses placement size hints, and Auto axes follow
the actual parent. Size-option changes republish the selected layout. This is a
simpler launcher-facing update path, not a reproduced API 37 launcher fix. The
responsive-wrapper identity test passed on API 31/35 before this simplification;
it did not reproduce the phone's fresh=false observation.

A separate concrete bug was reproduced: rendering removed configured children
when getAppWidgetInfo returned null, then wrote the reduced list into the shared
saved definition. Under a forced missing-metadata condition, a two-widget stack
became empty on both API 31 and 35. This can disable navigation. Rendering now
retains configured IDs. Explicit removal callbacks and user edits still handle
actual removals. Provider-info changes also release the tracking flag so later
sync can retry. This prevents further rendering-time deletion; it cannot restore
entries already removed by older builds.

Regression checks include size-option publications, fresh identity through
parceling, twenty alternating serialized agenda layouts with exactly one ListView
and clickable next/previous controls, and preserved stack membership/selection
when metadata is missing. These layout fixtures do not load real calendar rows
or reproduce native agenda switching on Xiaomi Android 17/API 37.

Fresh startup is active again. Existing explicit pause state is preserved; if
1.31 left stacks paused, tap Resume stacks in the app. Pause remains optional.
Saved dimensions, manual scale and navigation styles remain available.

Full API 31/35 framework suite, APK build and lint are required. The native API
37 issue remains open pending phone validation. Same recovered testing key.

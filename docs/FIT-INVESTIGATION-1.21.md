# Continued fitting investigation after 1.21

The owner's MyFitnessPal screenshot was not verified fixed by prior unit tests.
The 1.21 build removes the fixed option-sized wrapper for fully automatic entries.
This follow-up adds stronger checks without changing production app behavior.

## Framework host checks

`LauncherBoundsTest` uses AppWidgetHostView and production RemoteViews:

- Auto fills actual measured bounds across five shapes without republishing.
- All four rail edges, overlap on/off, contracted/expanded, and corner/bar modes
  reserve only the expected navigation space and leave the provider filling the
  remainder (32 combinations on each of API 31 and 35).
- Real MotionEvents at the provider's right edge deliver its PendingIntent after
  three host resizes. The test host is attached to an activity window; a detached
  host cannot execute queued click delivery and is not a valid touch test.

All 203 unit/framework tests passed with zero failures, errors or skips, including
these new cases, on 2026-10-30. Debug lint completed with no errors. These are
Robolectric framework checks, not a physical-device MyFitnessPal result.

## Android runtime checks

Two new instrumentation cases exercise actual binding, external providers,
screenshot edge pixels and pointer injection while host bounds deliberately
exceed reported options. One uses an in-process AppWidgetHostView; the other
uses a separately installed fixture launcher (different UID/process).

The fixture exposes bounded test-only geometry/resize calls for instrumentation.
This interface is confined to the fixture APK and is not part of the shipped app.

A software Android 12/API 31 emulator was provisioned, because no attached phone
or /dev/kvm is available. App, fixture and instrumentation APKs compiled and
installed. Runtime results are pending. The emulator is AOSP, not HyperOS or
Pixel Launcher, and the provider is a controllable fixture, not MyFitnessPal.

## Remaining boundary

Physical confirmation for the reported MyFitnessPal case remains necessary.
Explicit custom dimensions still use the separate fitting path and its
launcher-option limitations; passing Auto tests does not establish custom sizing.
No universal visual crop of provider internals is introduced.

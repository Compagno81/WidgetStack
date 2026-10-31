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

## Follow-up after owner confirmed 1.21 still has gaps

A retained 150% legacy field with both current axes on Auto reproduces a small
provider through the fixed wrapper on 1.21. `LauncherBoundsTest` now follows the
actual provider descendant through ancestor transforms, rather than accepting a
full-sized outer wrapper as proof of fill. The legacy case failed on both API
levels before the fix. Ignoring that obsolete multiplier gives 205 passing
unit/framework tests. See `RELEASE-CANDIDATE.md` for the 1.22 candidate.

The external fixture's geometry report likewise follows the real provider leaf's
visible screen bounds. Test focus and Back injection use the correct cross-app
mechanisms. Runtime runs remain incomplete until a clean end-to-end result is
recorded; infrastructure crashes and interrupted runs are not passing evidence.

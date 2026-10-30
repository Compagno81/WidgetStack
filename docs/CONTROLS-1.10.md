# Home-screen control delivery, 1.10

The user's Pixel still had inert home-screen controls in 1.9 although the configuration preview worked. Preview clicks use local handlers and do not verify PendingIntent delivery. This release changes the actual home-screen delivery path.

## Changes

- Explicit immutable navigation and expansion PendingIntents now target `StackControlReceiver`, a non-exported, filterless component. A launcher invokes it through the app-created PendingIntent capability. It does not depend on the exported AppWidgetProvider's lifecycle intent filter.
- Pin completion uses the same dedicated endpoint, with an explicit mutable PendingIntent so Android can supply the new home ID.
- Removed the artificial `goAsync()` hold that kept navigation broadcasts open for 2.7–8 seconds while waiting for auto-contraction. Each receiver call finishes after applying navigation and publishing. The foreground host retains the contraction timer.
- Older publications addressed to StackProvider remain compatible. A separate legacy filter matches their custom actions and URI scheme; lifecycle updates retain their own filter.
- Widget ownership checks, independent selection per placement, third-party content interactions and saved definitions are retained.

## Verification

163 automated executions pass on API 31 and 35 with zero failures/errors. Android lint passes with errors enabled. The ten new checks apply a parcelled production RemoteViews publication, tap its buttons/dots/contracted strip or square, allow manifest receiver dispatch, and assert the stored home selection/expanded state. They cover every edge, overlap and buttons-only modes, twenty rapid taps, independent copies, legacy publications and rejection of saved IDs as home IDs. They do not manually call the control receiver.

The two endpoint-contract executions failed against the old manifest/delivery path and pass after the change. Those failures demonstrate the endpoint regression check, not reproduction of the user's physical-device failure. A global real ViewAnimator shadow prevents cached RemoteViews method handles from referring to incompatible test shadows when suites run together.

The instrumentation suite includes `CrossUidControlsTest`: a separately installed fixture APK acts as an AppWidgetHost in another UID. It injects touchscreen events into actual published controls and checks selection after arrows, dots, settings changes and corner expansion. It requires binding grants for both APKs. The software-emulator attempt failed at Android package/activity service startup before this test could run. Its infrastructure output is included in the evidence; no successful emulator or physical execution is claimed.

## Practical limits

Physical Pixel and Xiaomi stock-launcher verification remains required. A passing Robolectric test cannot establish that the reported phone issue has been resolved. No live device logs from the reported failure were available, so the exported-filter ambiguity and broadcast hold are addressed without claiming a confirmed sole root cause.

Install as an update using the existing development certificate, then open Widget Stack once to republish current controls for existing home-screen copies. Saved stacks and provider widget setup are preserved.

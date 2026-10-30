# Validation: version 1.3

On 21 October 2026, all 24 automated tests passed with no failures or errors after a clean build. Android lint passed with errors enabled. The user app, instrumentation APK and separate fixture APK compile using JDK 17, Android SDK 35, AGP 8.9.2 and Gradle 8.11.1. The final user APK is assembled after those checks pass and its signature and version are verified separately. Raw results are in `evidence-1.3/`.

## Current automated coverage

| Checks | Executions |
| --- | ---: |
| Stack state: independent stacks, selection, reordering, removal, navigation wrapping, single/empty stacks and settings bounds | 10 |
| Native bar layout: four edges, configurable thickness/opacity, quarter-length end controls and full content with hidden bar | 2 |
| Hosted widget size options: correct axis and responsive size lists | 2 |
| Dot and previous/next PendingIntent routing | 2 |
| Both directions of the animation layouts, disabled animation and ordinary updates | 2 |
| Settings persistence and loading older stack data | 2 |
| Visual editor: live controls, Apply, Cancel and native graphics rendering | 2 |
| Total | 24 |

The 14 Android UI executions use Robolectric 4.14.1 against Android 12/API 31 and Android 15/API 35 framework runtimes. These are simulated framework tests, not device or stock-launcher tests. Robolectric's default ViewAnimator shadow omits animations; the animation test delegates its selection/current-view methods to the original Android ViewAnimator implementation and verifies that the incoming animation is started. The native editor render was inspected for clipping and readable controls. This establishes layout and animation setup, not physical rendering smoothness.

The instrumentation suite has been updated for the new direct dot controls and wrapping end buttons, and compiles. It has not been executed on an emulator or physical device for 1.3. Historical 1.2 device results remain in `VALIDATION-1.2.md` and `evidence/`; they are not claimed as a new device run for 1.3.

## Physical checks still required

- On the user's Pixel: update 1.2 to 1.3, retain stacks, open configuration, place the bar on each edge and vary thickness/opacity.
- Tap every dot and both end buttons, including first/last wrapping and after reordering. Test independent settings on two stacks.
- Enable slide animation and inspect both directions. The effect slides in the new widget; the outgoing widget is replaced immediately. Disable it and confirm instant switching.
- Check real calendar scrolling and event taps, third-party provider setup and refreshes, widget resizing and launcher rotation/restart.
- Repeat on the actual Xiaomi HyperOS stock launcher and check background/autostart policy and native gesture navigation.

No physical Pixel, Xiaomi or WhatsApp setup result is claimed. The existing RemoteViews compatibility bridge still depends on Android and OEM behaviour. A large number of widgets reduces the size of individual dot targets; the previous/next controls remain quarter-length. Smaller thickness choices also reduce touch-target width.

## Reproduce

```sh
./gradlew :app:clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest :fixture:assembleDebug
./gradlew :app:assembleDebug
```

The existing development signing key is included and retained from 1.2, so this build can update 1.2 in place. A physical update test remains on the checklist above.

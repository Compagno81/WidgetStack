# Validation: version 1.2

## Passed in this environment

- Android application, instrumentation APK and separate fixture APK compile with JDK 17, Android SDK 35, AGP 8.9.2 and Gradle 8.11.1.
- Seven JUnit model tests pass with no failures or errors.
- All 15 platform instrumentation tests pass together on Android 15 / API 35, AOSP x86_64, in 357.638 seconds.
- Android lint passes with errors enabled. Warnings about the documented private API bridge, English-only strings and minor UI recommendations remain.
- The final APK is rebuilt after the complete device run and its signature is verified. Its code, resources, manifest and assets exactly match the tested APK; only archive/signing metadata differs. Raw evidence is in `docs/evidence/`.

| Device checks | Passed tests |
| --- | ---: |
| RemoteViews nested traversal, collection traversal and owner-reference recursion guard | 3 |
| App-group picker, sizes, provider preview layout and adding back into the editor | 2 |
| External provider setup acceptance and cancellation with ID cleanup | 2 |
| Original external widget content and PendingIntent delivery | 1 |
| Forty-row calendar collection, finger swipe and row click delivery | 1 |
| Independent stacks, selection, sizing, order identity and deletion | 1 |
| Four-level nested weather-style layout and reopening its stack editor | 1 |
| Provider refresh reaching visible content | 1 |
| Optional rail visibility and child width restoration | 1 |
| 20% rail opacity and indicator selection | 1 |
| Ten-widget rail finger scrolling and retaining its position after a refresh | 1 |
| Total | 15 |

The fixture is a separately installed app with its own Android UID. The tests bind real child AppWidget IDs and a real outer stack, then render the outer RemoteViews in another AppWidgetHostView. They do not replace hosted widgets with screenshots or mock providers. The owner-reference regression uses a simulated framework-style owner field; it is not a reproduction of Google Weather's crash.

## Test environment limits

The emulator has no hardware acceleration. Its system services initially timed out during first boot. The test VM uses a system timeout multiplier of 20 and a longer watchdog timeout. Root was used only to adjust that VM property; the application runs under its ordinary app UID and does not require root. Automatic tests use a shell widget-binding grant. Android's normal user binding-consent dialog is therefore a physical-device check.

The tested outer host is an instrumented AppWidgetHostView, not the Pixel or Xiaomi stock launcher. No physical Pixel, Google Weather app or Xiaomi device result is claimed. The app's private API bridge remains dependent on Android and OEM framework behaviour. DrawInstructions-only RemoteViews cannot be embedded in the classic XML stack hierarchy.

## Physical-device checks still required

| Check | Expected result |
| --- | --- |
| Google Weather on Pixel 9 / Android 17 | Add, complete setup, reopen editor and interact without force-closing |
| Real calendar apps | Vertical agenda scrolling and event taps retain their behaviour |
| Android binding approval | Approve and cancel the system prompt cleanly |
| Stock launcher | Add multiple stacks, resize and restore them after rotation/restart |
| Native gesture navigation | Home, Back and Recents continue to work normally |
| Background and reboot | Provider updates resume, subject to battery/autostart policy |
| Xiaomi HyperOS stock launcher | All the above on the actual target HyperOS version |

## Reproduce developer checks

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest :fixture:assembleDebug
adb install -r fixture/build/outputs/apk/debug/fixture-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell appwidget grantbind --package com.kevin.widgetstack --user 0
adb shell am instrument -w -r com.kevin.widgetstack.test/android.test.InstrumentationTestRunner
adb shell appwidget revokebind --package com.kevin.widgetstack --user 0
```

The fixture and shell grant are for developer tests only. Normal users install only Widget Stack. Version 1.0 must be uninstalled before installing 1.1 because its development signing key was lost during the workspace reset. This removes the old stacks. Versions 1.1 and 1.2 share the included disposable test key, so 1.2 can update 1.1.

For a failing real widget, capture logs immediately after reproduction:

```sh
adb logcat -d -s WidgetStack AndroidRuntime
```

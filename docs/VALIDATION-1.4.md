# Validation: version 1.4

On 29 October 2026, all 39 automated tests passed with zero failures or errors. Android lint passed with errors enabled. The app and instrumentation APK compile with JDK 17, Android SDK 35, AGP 8.9.2 and Gradle 8.11.1. The final user APK is assembled after the checks pass, then its signature, version and visibility declarations are checked. Raw results are in `evidence-1.4/`.

| Suite | Executions | Coverage |
| --- | ---: | --- |
| StackStateTest | 10 | Order, selection, wrapping, independent stacks and appearance bounds |
| WidgetSearchTest | 3 | Spaced app names, punctuation, package names, partial matches and non-English labels |
| BarNavigationTest | 22 | Four edges, quarter-length normal controls, half-length buttons-only controls, 8 dp compact layouts, reapplying modes, click intents, sizing, persistence, editor Apply/Cancel and animation setup |
| WidgetPickerDiscoveryTest | 4 | Synthetic installed provider descriptors, excluding the host app, spaced app-name and widget-label filtering, manual refresh and resume-time discovery |
| Total | 39 | No failures or errors |

Android UI tests use Robolectric 4.14.1 with Android 12/API 31 and Android 15/API 35 runtimes. These are simulated framework executions, not stock-launcher tests. Native graphics editor renders were inspected, including the buttons-only preview. The animation test delegates selection methods to Android's original ViewAnimator implementation because the default Robolectric shadow omits animation starts.

Duolingo and MyFitnessPal are represented only by synthetic metadata in picker tests. Their actual APKs were not installed or tested. Android's package visibility filtering is not reproduced by that fixture. The compiled manifest includes both explicit package declarations and launcher/widget intent visibility, addressing a plausible cause of the user's missing providers. Actual availability still needs checking on the phone. Profile policy, app/component enablement, provider implementation and OEM behaviour can also affect visibility.

The platform instrumentation suite compiles, but was not executed on an emulator or physical device for 1.4. Historical results for 1.2 and 1.3 remain in their respective validation documents and evidence folders.

## Physical checks

- Update the existing app, retain stacks, enable Buttons only and set the thickness to 8, 12 or 16 dp on each edge. Each arrow should fill half the bar and switch in the correct direction. Very thin bars have correspondingly narrow touch targets.
- Turn Buttons only off and confirm dots and settings gear return. Use the app's stack list to edit while the gear is hidden.
- Open Duolingo and MyFitnessPal once, reopen or refresh the widget picker, and check their entries. Search with and without spaces. If absent, check whether the stock launcher lists their widgets.
- Add the actual app widgets and check setup, interaction, refreshing and resizing. Check calendar scrolling and optional slide appearance with the user's Pixel or Xiaomi HyperOS stock launcher.
- Check background/autostart and native gesture navigation on the target HyperOS version.

No physical Pixel, Xiaomi, Duolingo or MyFitnessPal compatibility result is claimed. The existing RemoteViews bridge still depends on Android and OEM behaviour, including whether a provider uses an embeddable RemoteViews representation.

## Reproduce

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
./gradlew :app:assembleDebug
```

The testing signing key is retained from 1.3, so the APK can update that build in place.

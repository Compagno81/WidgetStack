# Validation: version 1.5

On 29 October 2026, all 49 automated tests passed with zero failures or errors. Android lint passed with errors enabled. The app and instrumentation APK compile against Android SDK 35 using JDK 17, AGP 8.9.2 and Gradle 8.11.1. The final user APK is assembled after those checks pass, then its version and signing certificate are verified. Evidence is in `evidence-1.5/`.

| Suite | Executions |
| --- | ---: |
| Stack state and appearance bounds | 10 |
| Search normalization | 3 |
| Existing navigation, sizing, persistence, visual editor and animation setup | 22 |
| Synthetic widget discovery, search and refresh | 4 |
| Optional overlap geometry, hosted sizes, touch routing, persistence and slide layering | 10 |
| Total | 49 |

Framework UI tests use Robolectric 4.14.1 with Android 12/API 31 and Android 15/API 35 runtimes. They are simulated framework executions, not physical launcher tests. The overlap checks cover all four edges, normal/buttons-only modes, 8 and 48 dp thickness, full-size content and responsive size options. Native editor output with overlap enabled was visually inspected.

Touch routing is exercised through an attached Android View hierarchy with a synthetic interactive Button in the widget area. Taps and movement outside the bar reach that view; taps on the bar arrows send the navigation broadcast without clicking the view underneath. This validates composition and input routing, not interaction with every third-party widget. Animation checks use the original Android ViewAnimator methods because Robolectric's default shadow omits animation starts, and confirm that only the widget area animates while the bar remains stationary.

The instrumentation suite compiles but was not run on an emulator or physical device for 1.5. Physical Pixel and Xiaomi checks remain required: enable/disable overlap, inspect real widget sizing and refreshes, use controls near the covered edge, scroll a real calendar outside the bar, and inspect animations. The bar intentionally receives touches within its covered area, including with low opacity. Existing stacks default to overlap off.

Duolingo/MyFitnessPal availability and OEM/private RemoteViews compatibility remain unverified on the user's phone. Historical results are retained in the older validation documents and evidence folders.

## Reproduce

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
./gradlew :app:assembleDebug
```

The development signing key is retained from 1.4, so this APK can update that build in place.

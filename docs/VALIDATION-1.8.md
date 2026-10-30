# Validation: version 1.8

On 29 October 2026, all 101 automated tests passed with zero failures or errors, Android lint passed with errors enabled, and the instrumentation APK compiled. Raw evidence is in `evidence-1.8/`. The final validation run covers state, navigation, overlap, discovery, the grouped editor, automatic contraction, and the live preview. The app targets Android SDK 35 with JDK 17, AGP 8.9.2 and Gradle 8.11.1.

| Suite | Executions |
| --- | ---: |
| State and bounds | 10 |
| Search | 3 |
| Existing navigation/editor/animation | 22 |
| Overlap | 10 |
| Discovery | 4 |
| Auto-contract and grouped configuration | 12 |
| Live content preview, touch shield, local navigation and refresh lifecycle | 8 |
| Delay validation, bar motion, interruption and preview timers | 12 |
| Corner square geometry, animation, touch routing, settings and preview | 10 |
| Launcher reapply ID collisions, actions, cache loss and async updates | 10 |
| Total | 101 |

## Coverage

The pre-existing 49 tests cover state/order, search, bar geometry, touch routing, overlap, hosted sizing, persistence, Apply/Cancel and widget slide setup. New checks cover the 1999/2000 ms contraction-start deadline and 240 ms animation completion, deadline reset after another navigation tap, independent stack timers, provider refreshes that do not extend the deadline, disabled/hidden cases, contracted geometry on all edges, EXPAND-only pending intents, size options, contracted-width bounds, dependent settings, grouped drafts and legacy defaults.

Live-preview tests use synthetic cached RemoteViews through the production rendering bridge. They verify original content, blocked provider touch dispatch and broadcast actions, disabled descendant accessibility/focus, home-size measurement, local preview navigation, explicit loading placeholders, provider snapshot refresh and listener removal on detach. Native editor and preview renders were inspected. No installed third-party app compatibility is inferred from synthetic content.

Framework UI tests run with Robolectric 4.14.1 against Android 12/API 31 and Android 15/API 35. They are not physical launcher tests. The instrumentation suite compiles but has not been executed on a device for this version. Historical results remain in older validation documents.

New animation tests use Android's real ViewAnimator implementation in place of Robolectric's simplified shadow. They assert animations actually start, sample transforms at mid-animation, verify every edge/end and overlap mode, and confirm only the bar animates. Corner routing tests confirm provider controls remain tappable on the rest of the selected edge. Delay tests cover defaults/persistence, long arithmetic, invalid input blocking, reset and interruption, and attached preview timing.

## Reapply regression

Six checks failed against version 1.7 before the fix on API 31 and 35. They reproduced stale outer bar width/opacity and stale dot lists when hosted provider IDs numerically collided with our IDs. Android resolves RemoteViews action IDs against the existing view tree during reapply. Previously, bar actions ran before the old provider content was removed, so they could affect that content rather than our bar.

Expanded, contracted and corner shells now start by clearing the content container and adding a clean placeholder. Bar actions then resolve against our own controls. Original provider content is embedded after bar actions, preserving its interactions. The placeholder also handles a lost provider snapshot without leaving the previous widget tree attached.

Ten regression executions now cover repeated direct reapply, updated outer geometry/dots and click intents, original provider clicks, contracted expansion, lost snapshots and asynchronous reapply. These use the actual framework RemoteViews update path rather than Robolectric's AppWidgetHostView shadow, which always reinflates. Deliberate before-fix failures are included as reapply-before.xml/log; final TEST XML files show passing results.

The app republishes saved stack settings when opened. Refresh widgets also resets transient bar state and reconciles child dimensions.

## Physical checks still required

- Install 1.8 over the existing app and open it once. Check existing stack bar taps and saved setting updates without restarting the launcher, including after hosted content refreshes.
- Enable auto-contract, choose expanded/contracted sizes and check contraction after the configured delay (minimum two seconds), expansion-only on the first strip tap, and delay reset after each navigation tap.
- Check strip sliding, corner contraction to either end, square-only hit targets, and expansion on Pixel and Xiaomi launchers. In non-overlap mode, provider content reflow is a discrete size change even while the bar animation is smooth.
- Check timing during sleep/wake, process restart and Xiaomi battery/autostart restrictions. A main-thread handler and short navigation-broadcast lifetime hold are used; there is no real-time scheduling guarantee under OS suspension.
- Verify widget interaction, scrolling, resizing and refreshing in both overlap modes. Non-overlap mode changes the widget's available width/height when the bar changes size; layout changes can affect provider view state.
- Compare the preview with the actual Pixel/Xiaomi stack at the same size. The preview reuses real cached content and rendering but launcher padding, corner treatment, theme and timing can differ. Preview bar taps browse locally; provider controls are protected by the preview touch shield.
- Check real provider configuration and compatibility, including Duolingo/MyFitnessPal. Android/OEM private RemoteViews differences remain a compatibility dependency.

## Reproduce

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
./gradlew :app:assembleDebug
```

The development signing key is retained from 1.7, so this APK can update it in place.

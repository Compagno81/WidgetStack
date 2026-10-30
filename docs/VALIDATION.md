# Validation: version 1.14

All 185 automated unit and Android framework UI executions passed with zero failures or errors. Android lint passed, and a forced non-incremental APK assembly completed successfully. New coverage verifies per-widget logical-size persistence and provider option scaling on Android API 31 and 35. The framework tests use synthetic widget content; MyFitnessPal's layout response still needs to be checked on the Pixel.

---

# Validation: version 1.11

All 179 automated executions pass with zero failures/errors; lint passes, and instrumentation/fixture APKs compile. This release targets the launcher Edit route, deletion, and an unreachable small contracted corner. See RECOVERY-1.11.md and evidence-1.11 for before/after checks. Framework test results do not establish physical-device success. No new emulator/physical run is claimed.

---

# Validation: version 1.10

On 30 October 2026, all 163 automated tests passed with zero failures or errors. Android lint passed with errors enabled, the instrumentation and separate fixture APKs compiled, and the user APK was built afterward with versionCode 11/versionName 1.10 and the unchanged development certificate. Raw results are in `evidence-1.10/`.

The ten additional tests exercise parcelled home publications through manifest receiver dispatch, rather than the preview's local handlers. See `CONTROLS-1.10.md`. This does not establish physical Pixel or Xiaomi success. The API 35 software emulator did not reach stable package/activity service availability: installations and bind grants failed during startup, and instrumentation could not start. No application assertion from that attempt passed or failed. `evidence-1.10/emulator-attempt.json` contains the results. The separate-UID test is compiled and provided for a stable emulator or physical device.

---

# Validation: version 1.9

On 29 October 2026, all 153 automated tests passed with zero failures or errors, Android lint passed with errors enabled, and the instrumentation APK compiled. Raw evidence is in `evidence-1.9/`. The final validation run covers state, navigation, overlap, discovery, the grouped editor, automatic contraction, and the live preview. The app targets Android SDK 35 with JDK 17, AGP 8.9.2 and Gradle 8.11.1.

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
| Saved-definition persistence, migration and placement semantics | 22 |
| Saved-stack editor, chooser, binding flow, pin callback, copies, sizing and native layout | 26 |
| Public fresh-root protocol, parcel round trip and ownership fallback | 4 |
| Total | 153 |

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

## Saved stacks and control compatibility

Saved-stack store tests cover creating definitions before placement, all control fields, atomic/idempotent legacy migration, preserved child IDs and setup, linked settings, independent selections, reassignment, placement deletion, guarded definition deletion, missing placements, malformed legacy data and invalid assignments.

Framework flow tests cover editing and previewing unplaced definitions, Android binding-permission handoff for a provider chosen through the picker, launcher chooser result IDs, cancellation, creation through the chooser, mutable explicit pin callbacks with system-filled IDs, receiver navigation, multiple-copy size hints, removal of every home copy without deleting real child bindings, and loading/refreshing live saved previews. Native manager and chooser views were rendered on API 31 and 35 and inspected. Providers in these tests are synthetic; physical third-party setup and launcher confirmation dialogs remain unverified.

The fresh-layout protocol uses the public RemoteViews(package, layout, rootId) constructor, not a private reapply flag. A preference-backed generation counter prevents reuse across app restarts. Tests serialize it through Parcel, verify the host's canRecycleView decision is false on both API 31 and 35, and additionally prove direct reapply rejects a different root on API 35. A provider-list fallback test keeps ownership validation strict when individual provider information is unavailable. The prior ordered-reapply protections remain covered by their regression suite.

The user reported controls still unresponsive on-device after 1.8. This report does not claim the 1.9 safeguards resolve that physical issue. Local bounded troubleshooting events record received expand/navigation intents, ownership decisions and attempted publication; no widget content, names or analytics are collected.

## Physical checks still required

- Install 1.9 over the existing app and open it once. Verify migrated saved definitions, add from both the launcher chooser and the in-app pin action, reuse a saved definition twice, edit controls, and remove a copy while retaining the saved definition. Check existing stack bar taps and saved setting updates without restarting the launcher, including after hosted content refreshes.
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

The development signing key is retained from 1.8, so this APK can update it in place.

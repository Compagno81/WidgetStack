# Widget Stack

A small Android app for home-screen widget stacks. Requires Android 12 or newer.

## Install and use

If version 1.0 is installed, uninstall it first. The development signing key changed after a workspace reset, and uninstalling removes the old stacks.

1. Install `WidgetStack-1.14-debug.apk` and open Widget Stack.
2. Tap **Create saved stack**. Add actual installed widgets, approve Android's binding prompt, and complete each provider's setup when requested.
3. Rename the saved stack by tapping its name. Use **Customize navigation bar** to configure its controls. Edits are saved as you go; Done closes the editor.
4. Long-press the home screen, add **Widget Stack** from the launcher's widget picker, then choose a saved stack.
5. Each copy starts with the saved starting widget. Dot and arrow taps navigate that copy independently. Editing its widget list or controls updates all copies of the same saved stack.
6. Scroll and tap within the displayed home-screen widget normally. Resize each copy with the launcher's handles. Use the in-widget gear or the app's Edit button to edit the saved definition. The launcher's Edit/reconfigure action opens the current stack editor; Use a different saved stack opens selection explicitly.
7. Remove a copy through the launcher without deleting the saved stack. Delete a saved stack from the app after confirmation. Its home-screen copies become unassigned slots where you can choose another stack.

Existing home-screen stacks are migrated atomically into saved definitions with their original widget IDs, names, setup, order and control preferences. No widget binding is cloned or reset. The saved definition and its home-screen placement are separate records. See `docs/SAVED-STACKS-1.9.md`.

The widget picker groups widgets by app and accessible profile, supports app, package and widget-name searches, and shows provider previews with declared or approximate sizes. Spaces and punctuation in searches are ignored. It refreshes when reopened and has a Refresh menu action. App visibility includes launcher apps and explicit declarations for Duolingo and MyFitnessPal. Those apps’ actual widget availability still requires a physical-device check. Apps that do not supply a preview are labelled accordingly.

The bar defaults to the right edge, 20% opacity and 36 dp thickness. Each end button occupies 25% of its length; the middle 50% contains the widget dots and settings gear. Each stack has independent settings. The visual editor allows 8–80 dp thickness and 0–100% opacity. Previous and next wrap through the stack in its current order. With many widgets, dot targets become smaller; the end buttons retain their quarter-length areas. **Buttons only** removes the dots and gear and gives each arrow half the bar. Use the app’s stack list to edit when that mode is enabled. **Overlap widget** places the bar over the selected edge while the widget keeps its full size. The bar receives touches on the covered edge. In **Behaviour**, optional **Auto-contract** contracts the bar after a delay entered in whole seconds, starting at 2. Choose an **Edge strip** or **Corner square**, and either the top/left or bottom/right end of the selected edge. Expansion and contraction animate over 240 ms. The square sits inside a corner tap target of at least 48 dp, centred inward so launcher rounding cannot hide a small square. Only this corner target intercepts touches. Tap the narrow strip to expand it without switching widgets. Contracted thickness is configurable from 4 dp up to just below the expanded size. See `docs/NAVIGATION-1.7.md`.

The settings preview uses the selected widget's actual cached RemoteViews and the same rendering path and current content-size options as a home-screen copy. An unplaced stack uses a labelled sample size; when several copies exist, the largest provides representative preview dimensions. Provider updates refresh an attached preview. Preview bar taps browse the draft locally; a touch shield blocks widget content actions, scrolling and long presses while stack controls remain active. If content has not loaded, an explicit placeholder is shown. Launcher padding, rounding, theme treatment and scale can differ, so a pixel-identical stock-launcher match is not claimed.

The optional animation slides the incoming widget into view when a dot or end button changes the selection. It is a tap-triggered slide-in effect, with the outgoing widget replaced immediately. It does not enable swipe navigation. Scrolling and tapping inside hosted widgets retain their normal handling. The bar is part of the stack, not a screen overlay.

A low-priority Android notification accompanies the background widget host. Grant notification permission if you want it visible in the notification shade. If your device restricts background activity, adjust its battery settings if widget updates stop. Keep your existing launcher and gesture navigation.

## Status

This is an installable debug test build, not a store release. Version 1.13 has 181 automated state and Android framework UI checks, including all bar positions, sizing, click intents, persistence, settings cancellation and animation setup. See `docs/VALIDATION.md` for the precise results and limitations. Physical-device visual and interaction checks are still required, particularly slide appearance, hosted calendar scrolling and event taps, resizing, widget refreshes and background operation.

The implementation preserves original widget RemoteViews, PendingIntents and collection views. Compatibility with every third-party widget or OEM Android build is not guaranteed. Unsupported rendering shows an error inside the stack. Android 15+ DrawInstructions-only widgets, OEM-only widgets that are not exposed as Android AppWidgets, provider-specific URI access, and vendor changes to non-public APIs need particular attention.

The APK is signed with a development key. A locally rebuilt APK with another key requires uninstalling the supplied build first, which removes its stacks. Versions 1.2 through 1.14 use the same disposable testing key as 1.1, so they can update those builds without removing stacks. The original 1.0 key was lost during a workspace reset; uninstall 1.0 before installing 1.1. This clears the old stacks.

## Build in Android Studio

Open this folder. Use JDK 17, Android SDK Platform 35 and Build Tools 35.0.0. Android Studio can install the missing SDK packages. The Gradle wrapper is included.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

The app has one runtime library, Android Hidden API Bypass 6.1. It uses native Android views and Java. It has no network permission, analytics, overlay, accessibility service, launcher replacement, lock-screen feature or widget drawer. `fixture` is a separate developer test APK and is not included in the user app.

## Source reference and licence

Inspected Widgets Anywhere at commit `259a2c7dc6df2e3317bcde2e77c36c4e8275f2ab`:
https://github.com/zacharee/LockscreenWidgets

Relevant MIT compatibility logic is adapted in `RemoteViewsBridge` and `CollectionProxyService`. Both upstream MIT notices are retained in `licenses/`, along with the Apache 2.0 licence for Hidden API Bypass, and included in the APK assets. See `NOTICE` and `docs/IMPLEMENTATION.md`.

The previous 1.8 reapply fix remains. Version 1.9 also gives each published layout a durable, distinct root ID using the public Android 12+ RemoteViews constructor, so the framework rejects reuse of stale controls. Provider-owned ID lists are used as an ownership fallback. Actual resolution of the reported unresponsive-control issue on the user's device remains unverified. The updated app no longer exposes diagnostic controls on its main screen.


Version 1.10 routes navigation, expansion and pin completion through a dedicated, non-exported receiver with no intent filters. Broadcasts finish promptly; auto-contraction remains owned by the foreground host's timer. Old widget publications targeting the provider still work. The regression suite now taps parcelled production RemoteViews and verifies changes through manifest receiver dispatch. This verifies more than the configuration preview, which uses local click handlers. See `docs/CONTROLS-1.10.md` for evidence and device-test limits.


Version 1.11 removes Add to home, allows confirmed deletion of placed saved stacks, and routes launcher Edit to the existing editor without reselecting or resetting the starting widget. The editor offers Use a different saved stack. Opening Edit or using Refresh widgets resets transient bar state and republishes expanded controls while retaining that copy's selected widget. Contracted corner visuals retain their configured size but are centred inside a tap target of at least 48 dp. The content touch shield remains exclusively in the configuration preview. See `docs/RECOVERY-1.11.md`.


Version 1.12 refreshes the app screens with a modern Android visual system: a restrained indigo palette, rounded surfaces, clearer text hierarchy, and separated primary and secondary actions. The home screen presents saved stacks as cards and keeps device troubleshooting out of the main flow.


Version 1.13 replaces the widget reorder text with compact up/down arrow controls, retaining spoken labels for accessibility. Compact action labels stay on one line, and the configuration page clips within its system-bar insets so the title clears the status bar. See `docs/RECOVERY-1.13.md`.

Version 1.14 adds a per-widget requested layout size setting from 100% to 150%. The host scales the requested provider dimensions up, selects its responsive RemoteViews layout, and scales that layout down to the actual slot. Apps decide whether to expose a richer layout at the larger size, so results are provider-specific. Existing widgets remain at 100% by default. The setting is in each widget row in stack configuration.

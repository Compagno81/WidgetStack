# Implementation notes

## Investigation and decision

An AppWidgetHostView is an ordinary View subclass used by launchers and other widget hosts. It is not one of the classes permitted in a home-screen RemoteViews hierarchy. An ordinary third-party app cannot place a live custom host view or custom touch listener inside a launcher widget using supported public APIs alone.

RemoteViews does support nested RemoteViews through addView, and ListView collections support vertical scrolling. The reference project already uses that distinction: it hosts real widget IDs, retrieves the providers' original RemoteViews, embeds them in its own home-screen widget, and adapts collection services and caches. This app follows that design. It does not use screenshots, floating windows, a replacement launcher or an accessibility overlay.

Sources:
- https://developer.android.com/reference/android/widget/RemoteViews
- https://developer.android.com/develop/ui/views/appwidgets/host
- https://developer.android.com/develop/ui/views/appwidgets/collections
- https://github.com/zacharee/LockscreenWidgets/blob/259a2c7dc6df2e3317bcde2e77c36c4e8275f2ab/app/src/main/java/tk/zwander/lockscreenwidgets/appwidget/WidgetStackProvider.kt
- https://github.com/zacharee/LockscreenWidgets/blob/259a2c7dc6df2e3317bcde2e77c36c4e8275f2ab/app/src/main/java/tk/zwander/common/appwidget/RemoteViewsProxyService.kt

## Upstream inspection

Both upstream licences are MIT, copyright Zachary Wander (2020 and 2023). The inspected project includes lock-screen frames, overlays, a drawer, Samsung tiles, Compose, native code, Bugsnag, Tasker, Shizuku and many UI libraries. Its current build also uses a modified API 37 android.jar.

This project reuses only the widget-stack technique and adapted collection compatibility logic. The upstream lock-screen services, overlay permissions, drawer, telemetry, native libraries, Tasker integration, Shizuku and Compose dependencies are excluded. It builds against the unmodified public API 35 SDK. The runtime dependency is Hidden API Bypass 6.1, the version used by the inspected upstream project.

## Responsibilities

| Component | Purpose |
| --- | --- |
| StackProvider | Independent outer home-screen widget instances and selection broadcasts |
| StackHost | Real child widget IDs, system binding and live update listeners |
| RemoteViewsBridge | Size variant selection, nesting, collection action adaptation and Android 15 collection cache transfer |
| CollectionProxyService | Binds legacy collection services with the original child widget identity |
| StackRenderer | Original widget content plus a configurable edge bar and optional tap-triggered slide-in |
| ConfigureActivity | Binding approval, provider setup, reordering and removal |
| WidgetPickerActivity / WidgetCatalog | Accessible-profile widget discovery, refreshed searchable app groups, size labels and provider previews |
| HostService | Keeps the widget host listening without polling or overlays |
| StackStore / StackState | Durable per-stack order, selection and navigation appearance |

Each child belongs to one stack. Selection is stored by child ID so reordering does not silently select another widget. Removing a stack releases its child IDs. Size changes are passed to all children, subtracting bar thickness along the selected axis only while the bar is visible. Child PendingIntents remain intact. Direct dot and end-button PendingIntents handle navigation; the content area has no added gesture interception.

Non-public API exemptions are limited to the widget, collection and service-dispatch interfaces used here. Failure is handled with a visible stack placeholder and log output. This remains an Android compatibility dependency: compiling successfully cannot prove it works on every Android/HyperOS release. The targeted lint suppression for the non-public RemoteViews method records this deliberate implementation choice.

No HOME intent filter is declared, and the app does not request changes to system navigation. Native gesture navigation should remain with the stock launcher, but that needs confirmation on the target Xiaomi build.

## Limits

- Independent sizes for different pages are not exposed. The launcher resizes the shared stack space and all child widgets receive that space.
- The rail switches pages on tap. Android RemoteViews provides no general swipe callback to a widget provider. Using StackView to add swiping would compete with vertical scrolling inside hosted widgets, and horizontal widget gestures are not part of the standard app-widget gesture model.
- Android 15+ DrawInstructions-only RemoteViews cannot be nested in classic XML RemoteViews. Such providers need device-specific investigation rather than a claim of universal support.
- Battery management can stop the live host. Existing rendered content remains with the launcher; opening the app or tapping a page attempts to resume updates.
- Android widget IDs are device-specific. Backup and device transfer of stack state are disabled.

## Version 1.6 changes

Navigation settings share a single draft under Appearance, Behaviour and Animation tabs. Visibility is consolidated into Appearance. Auto-contract is optional and defaults off. Its dependent contracted-width control appears only when enabled, and is constrained to 4 dp through expanded width minus 1 dp.

BarController maintains one cancellable main-looper deadline per expanded bar. EXPAND and SELECT actions reset the deadline to 2000 ms; provider refreshes do not. A contracted strip emits EXPAND only. On expiry, hosted size options and the rendered widget update. The existing foreground host maintains the listener process, and navigation broadcasts briefly hold goAsync for the deadline. Service/process restart starts contracted and reconciles hosted sizes. There are no polling loops or periodic alarms. Ordinary widget touches cannot be observed by the provider, so only navigation taps reset the delay. Contraction/expansion changes layout without a bar-resize animation.

StackPreview uses the production RemoteViews renderer, the cached real widget snapshot and current outer widget size options. Its AppWidgetHostView is scaled to fit the preview. As of 1.7, a parent touch interceptor shields all provider content while handling local bar navigation without changing home-screen selection. Provider descendants cannot receive preview gestures, keyboard focus or accessibility actions. Attached previews subscribe to snapshots and collection-data notifications and detach their listeners cleanly. No new child widget IDs or host listeners are allocated for previews. An explicit placeholder is used while the provider snapshot is unavailable. Exact launcher padding, corners, theme and rendering timing are outside the preview's control.

## Version 1.5 changes

The per-stack Overlap widget option defaults off. When enabled, a RemoteViews FrameLayout holds a full-size widget area followed by the bar as its last sibling. The bar uses edge gravity and is clickable within its own bounds, so the underlying widget is covered only at that edge. The root adds no touch interception outside the bar. This composition stays inside the home-screen widget and requests no new permissions.

Hosted size options retain the outer width and height in overlap mode, including responsive size lists. Disabling overlap restores the previous size reservation. Both buttons-only and dot modes, all four edges, opacity and tap-triggered slide layouts support overlapping composition. The bar remains outside the animated widget area. The visual editor previews the full widget area with the bar drawn over it, and Apply/Cancel follow the existing draft-and-save flow.

## Version 1.4 changes

The bar supports an independent per-stack buttons-only mode. Its middle section is hidden, so previous and next each occupy half its length. The app's stack list remains the configuration route when the gear is hidden. The thickness range is now 8–80 dp, with control glyphs scaled to the chosen thickness. Existing thickness, opacity, position and animation preferences are retained.

Package visibility now declares normal launcher apps in addition to widget receivers, and names Duolingo and MyFitnessPal explicitly. Android's AppWidgetService filters provider results by calling-app package visibility; these declarations address that path without QUERY_ALL_PACKAGES. Discovery merges the public manager results for accessible profiles and excludes this app's own provider. Search also matches package and widget labels and ignores spaces/punctuation. The list refreshes on resume and through a Refresh menu action.

Synthetic provider metadata and Android framework UI tests cover discovery, spaced-name searches and refreshing after providers become available. These do not reproduce the user's installed app versions, Xiaomi launcher or package visibility enforcement; actual Duolingo/MyFitnessPal availability must be checked on the phone. If an app remains absent, open it once, refresh the list, and check whether its widgets are present in the stock launcher's widget picker.

Relevant public API documentation:
- https://developer.android.com/training/package-visibility/declaring
- https://developer.android.com/reference/android/appwidget/AppWidgetManager#getInstalledProvidersForProfile(android.os.UserHandle)

## Version 1.3 changes

The native bar can sit on any edge, with 24–80 dp thickness and 0–100% opacity. Each end receives 25% of the bar length and switches to the previous or next widget, wrapping through the configured order. Weighted dots and a gear share the middle 50%. The live visual editor applies changes together and supports cancellation. All settings are stored per stack with backward-compatible defaults.

An optional ViewFlipper layout provides a 180 ms incoming slide for explicit page changes. It is never started for ordinary content updates. Side bars use vertical entry and top/bottom bars use horizontal entry; previous reverses the direction. The outgoing view is replaced immediately, so this is an entry effect rather than a continuous scroll animation. It adds no touch handler to widget content. Pixel and HyperOS launchers still need physical validation of the effect. See `NAVIGATION-1.3.md` and `VALIDATION.md`.

The indicator ListView from 1.2 has been replaced by direct weighted dot controls. With many pages their touch targets shrink; the quarter-length end controls remain available.

## Version 1.2 changes

Each stack can hide or show its right-side navigation strip. Hidden strips reserve no width, and child widget size options are updated so the widget uses the full stack width. The setting is persisted per stack and defaults to visible for existing stacks. Instrumentation covers both the view visibility and child width changes.

## Version 1.1 fixes

RemoteViews action traversal excludes static and synthetic owner fields and uses identity-based cycle detection with a depth bound. Framework actions can contain an enclosing RemoteViews reference; following it backwards can otherwise recurse indefinitely. Host callbacks also handle malformed or unsupported snapshots without bringing down the editor.

Android 15 collection caches are merged using their ID and URI maps. The earlier implementation expected an addMapping method that is absent on the tested SDK 35 framework. Legacy collection binding selects exact compatible service-dispatch overloads, adapts integer/long flags, and publishes its interface before its binder. If a cold connection initially returns no data, a single deferred refresh is scheduled after connection. It does not rebuild on every service connection.

The picker groups widgets by app and profile, searches app and widget names, shows declared grid dimensions or explicitly approximate dimensions, and reports minimum size in dp. Preview order is Android 15 generated RemoteViews, previewLayout, previewImage, then an app icon and an explicit missing-preview label. Preview content is not interactive. Actual stack content retains the original widget interactions.

Picker preview sources: https://developer.android.com/develop/ui/views/appwidgets/previews and https://developer.android.com/reference/android/appwidget/AppWidgetProviderInfo.

In 1.2, the indicator ListView retained its scroll position across provider refreshes; 1.3 replaces it with direct dot controls. Transparent white preview images receive a contrasting background, and the search layout respects the keyboard inset.


Version 1.7 adds per-stack delay seconds (minimum 2), strip/corner shrink modes, endpoint preference, and 240 ms launcher-side XML bar animations. See NAVIGATION-1.7.md for geometry, timer handling and non-overlap reflow limitations.


Version 1.8 clears hosted content before all shell actions to prevent resource-ID collisions during synchronous and asynchronous launcher reapply. A clean placeholder is installed even without a provider snapshot, and content is embedded last. Opening the app republishes saved stack settings; manual refresh resets transient bar state and sizes. See REAPPLY-1.8.md.


Version 1.9 separates saved definitions from home-screen placements, retains child bindings through migration/removal, adds StackChooserActivity and explicit pin callbacks, and aggregates size options for linked copies. Home publications use distinct durable root IDs through the public RemoteViews constructor as an additional stale-control safeguard. Bounded local receipt/publication diagnostics are user-copyable. See SAVED-STACKS-1.9.md and VALIDATION.md for protocol coverage and physical limitations.

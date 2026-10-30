# Saved stacks in 1.9

## User flow

Create saved stack in the app, choose and configure real installed widgets, then customize the control bar. Naming, order, starting widget and all bar preferences belong to the saved definition and persist without a home-screen placement. Widget and name edits are saved immediately; navigation-bar drafts use Apply/Cancel. Backing out retains the saved stack. A cancelled or failed provider binding is released rather than added.

When Widget Stack is added through the launcher's picker, StackChooserActivity lists saved stacks and assigns the chosen definition to the supplied home-screen AppWidget ID. It returns RESULT_OK with that same ID only after storing the assignment and requesting its first publication. Cancel leaves the placement unassigned. Create saved stack in this chooser opens the same editor; Done assigns it to the original pending placement. Native reconfigure opens the chooser again.

Add to home in the app uses requestPinAppWidget. Android does not automatically launch provider configuration for that route, so an explicit mutable broadcast PendingIntent carries the saved ID while the system fills in the new placement ID. A cancelled request creates no placement. If a definition was deleted before the callback, no invalid assignment is stored; the unassigned widget can open the chooser. Launchers without pin support receive manual picker instructions.

## Records and ownership

Saved definitions use negative app-local IDs starting at -2. These are not Android AppWidget IDs and are never sent as widget placements to AppWidgetManager. Definitions own the original positive third-party child AppWidget IDs and their completed provider configuration. Placement records map positive launcher IDs to one definition and hold per-copy selection. Navigation never changes the starting widget of the saved definition or another copy's selection.

The same saved stack placed twice shares widget configuration, provider data and control style. It does not allocate or attempt to clone third-party widget setup. Each copy renders the original provider RemoteViews at its own dimensions and receives its own navigation PendingIntents and auto-contract timers. Removing a copy removes only its mapping; deleting a definition with remaining copies is blocked. Deleting an unused definition releases its child bindings.

Legacy per-placement JSON is converted in one SharedPreferences commit: the same child IDs and configuration become a saved definition, while the existing home ID becomes a reference with its previous selection. Migration is idempotent and does not allocate or delete provider bindings. Corrupt legacy entries are retained rather than discarded. Reopening the app prunes mappings no longer present in the provider's authoritative AppWidget ID list, retaining the saved definitions.

## Size and preview behavior

One provider binding has one set of size options. All bound copies contribute their adjusted min/max dimensions and responsive SizeF entries to that binding's options. Modern responsive providers can supply variants for each size; older providers that only interpret min/max may produce different results when copies have very different dimensions. This requires real launcher/provider testing.

Unplaced stacks use a 240 by 160 dp sample. If placed, the largest copy supplies representative preview dimensions. The same renderer, live provider snapshots, protected provider surface and local navigation controls are used. Preview actions never invoke third-party PendingIntents.

## Unresponsive-control investigation

The 1.8 clear-before-controls ordering remains. Each actual home-screen publication now receives a durable distinct root view ID using the public API 31+ RemoteViews constructor. The framework host's canRecycleView decision rejects stale root identities and the host inflates the new hierarchy. Provider-owned widget IDs are an ownership fallback when getAppWidgetInfo is unavailable. Copy troubleshooting info records the device/API, app version, saved/copy counts and recent receipt/publication events. It stores no widget contents or labels and has no network access. Physical resolution of the user's reported freeze is not claimed from framework tests.

References: https://developer.android.com/reference/android/appwidget/AppWidgetManager#requestPinAppWidget(android.content.ComponentName,android.os.Bundle,android.app.PendingIntent), https://developer.android.com/reference/android/widget/RemoteViews#RemoteViews(java.lang.String,int,int).

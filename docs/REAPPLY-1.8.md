# Stale home-screen controls: 1.8

The failure is in RemoteViews update ordering, independent of battery restrictions. Different installed apps can assign the same numeric resource IDs. When the launcher reuses an existing stack view, actions such as bar width, opacity, dot replacement and PendingIntent assignment search that existing tree. With provider content appearing before the bar in the tree, a matching provider ID can receive those actions. The previous provider subtree was only removed later. Fresh inflation, such as after restarting the launcher, can temporarily hide the issue.

All shell updates now clear the content container first, before any control lookups, and install an explicit clean placeholder. They apply shell settings and handlers next, then insert the actual provider RemoteViews. This ordering also applies to async action preparation. Missing snapshots keep the placeholder instead of stale provider content. Normal home-screen widget interactions are preserved. The config preview continues to shield provider touches and permits only local stack controls.

Opening the app republishes saved settings and sizes; Refresh widgets clears transient animation/timer state before reconciling sizes and content. Version 1.8 retains the existing development signing key and preferences.

The six original regression executions failed before the fix, and ten executions pass after adding async and lost-cache coverage on Android 12 and Android 15. All evidence is in evidence-1.8. Pixel/HyperOS launcher behavior still requires a physical check. No attached phone is available here.

Framework reference: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/appwidget/AppWidgetHostView.java and https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/widget/RemoteViews.java.

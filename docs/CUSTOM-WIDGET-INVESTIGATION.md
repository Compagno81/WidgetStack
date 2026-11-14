# Custom widget loading investigation, 14 November 2026

Reported against 1.41: App Grid and Canvas fail to load inside the stack while other providers work.

Inspected AppGrid main commit 31ba9128b69746cdb865ab58dadf61f940d184c3. GridWidget uses responsive RemoteViews with native RemoteCollectionItems, bitmap row icons, stable row IDs and pending-intent templates.

Added a regression probe for two responsive bitmap-backed collections, including source parceling, combined publication parceling, native adapter binding, row image and text loading, and a navigation reapply. The new API 35 probe passes. All 13 RetainedNavigationTest cases pass across APIs 31 and 35. This does not reproduce or establish the cause of the reported device failure. It does not test the complete AppGrid APK, Canvas, or the user's launcher.

No production fix or new APK has been issued. Next evidence: use Copy troubleshooting report on WidgetStack's main screen after reproducing the failure; capture the visible failure message and identify Canvas's app/package. Phone model Xiaomi 17 does not establish Android API level. Canvas source was not found in connected repositories or filename search.

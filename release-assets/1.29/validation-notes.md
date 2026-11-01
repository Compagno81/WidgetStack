# 1.29 — independent navigation publication after an agenda

The owner reports that agendas continue to scroll and respond to taps, and the
configuration button works, while next/previous controls stop visibly switching
widgets. Several agendas expose this; merely having scrolling is not sufficient.

Android 15's AppWidgetManager can route all subsequent updates through a shared
collection conversion executor after a legacy list has been posted, including
updates with no collection. A blocked conversion can leave the last published
agenda interactive while subsequent selected widgets never reach the launcher.
The new framework regression deliberately blocks that executor: the public
update queues, while the new publication path delivers two later widget updates.
This establishes the queue dependency, not a physical Xiaomi reproduction.

For API 35+, Widget Stack now submits its already-prepared RemoteViews directly
to the widget system service, retaining the launcher's asynchronous adapter
binding and the bridge's collection item caches. Earlier Android versions retain
the public update path. This uses private Android APIs, like the existing widget
bridge; OEM/version compatibility remains a test requirement. Failures are
recorded rather than silently reentering the blocked conversion queue.

Additional tests verify that adapters with identical view IDs retain separate
child identities and leave original provider snapshots unchanged. Navigation
selection transitions are now in the troubleshooting report. The main screen
has Copy troubleshooting report, and About reads the actual installed version
instead of showing the stale hardcoded 1.22 label.

The preceding 1.28 child-ID and connection fixes remain in place. Validation
includes the full API 31/35 framework suite, APK build and lint. Physical Google,
Xiaomi and Business Calendar 2 agenda switching on HyperOS remains unverified.
This is a test candidate, not a claim that the phone failure is confirmed fixed.

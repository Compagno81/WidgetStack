# 1.28 — agenda service identity and collection connection handling

The owner reports Business Calendar 2 Agenda freezes navigation, while a custom
scrollable grid works. Scrolling alone is not the cause.

Collection service intents now explicitly carry the original child widget ID.
Widget Stack captures RemoteViews before the original host applies them, so
Android's usual adapter-binding ID injection has not happened. The proxy must
supply the child ID, rather than omit it or use the outer stack ID. Forwarding
clones the original intent and preserves its component, data and other extras.
This repairs an Android adapter contract defect; Business Calendar 2 has not
been run here, so its exact phone failure is not claimed as reproduced.

Connection metadata requests no longer wait for binding. Until connected, the
proxy returns temporary empty metadata and requests a refresh when ready.
Connection callbacks use a dedicated thread. The collection service runs in a
separate process, and readiness is forwarded to the main host process, which
owns current preferences and RemoteViews snapshots. It notifies data changes
and republishes the selected widget to avoid missing an early readiness event.

Regression tests on API 31/35 check nonblocking disconnected metadata, callback
progress with the main looper paused, connected row counts, process isolation,
and child-ID injection despite a different outer stack ID. Build and lint are
required. Full device reproduction and Business Calendar 2/HyperOS acceptance
remain pending. Same recovered testing signing key as previous candidates.

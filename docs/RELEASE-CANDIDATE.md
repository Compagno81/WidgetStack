# 1.31 — recovery controls, starts with stacks paused

The owner remains unable to control stacked agendas after 1.30. This build is
for recovering app control, not a claim that calendar switching has been fixed.

On the first launch of this recovery version, all stacks are paused. Opening
Widget Stack replaces home-screen stacks with a plain text placeholder that
opens the app. It starts no collection adapters or agenda-loading service.
A fresh public widget client publishes this non-collection placeholder to avoid
reusing a client that previously submitted legacy agendas.

Saved definitions, child widget bindings, placement references, selections,
width/height and manual scale are retained. While paused, host listening,
tracking, provider-size updates, collection refreshes and queued publications
are suppressed. Old navigation buttons cannot change the saved selection.
Paused app startup does not prune placement records. Refresh widgets also
stays paused. The service does not restart itself while paused.

The main screen has Pause stacks / Resume stacks. Pause can be used at any
time. Resuming is explicit, persists across app restarts, and may bring back
the unresolved agenda problem. Install and open the app, then leave stacks
paused while managing saved configurations. Do not clear app storage.

Tests on API 31/35 cover pause initialization, preserving an explicit resume,
preserving child identities, placements and sizes/scaling, blocking host work,
and safe placeholder interaction. Build and lint are required. Native API 37
launcher recovery remains unverified. Same recovered testing signing key.

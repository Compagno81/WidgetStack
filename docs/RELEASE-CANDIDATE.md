# Widget Stack 1.42 reconstructed compatibility candidate

On Android 16 (API 36) and newer, use the established single-selected-widget
publication path from 1.40. This removes 1.41's retained ViewFlipper/page wrappers
and hidden sibling widgets from publications on these platforms. Both normal
refreshes and navigation use the compatibility path, and any retained snapshot
tracking is cleared. The troubleshooting report identifies this mode.

This is a mitigation for the reported Android 17/API 37 regression: a stack in
Widget Canvas fails to inflate, and App Grid displays its heading without its
collection rows. No settings changes or widget recreation are required by this
update. Existing sizing, alignment, visibility profiles, Tasker and controls remain.

Tradeoff: switching widgets and bar updates can reset scroll position in
compatibility mode. The original scroll-retention goal remains unresolved on
Android 17. Android 12–15 retain the 1.41 behavior and existing collection fallbacks.

The precise platform/launcher failure has not been reproduced. The user's report
records successful publication submission, which does not establish successful
host inflation. Its additive-hierarchy warnings do not identify the failing apps.

Framework tests run on API 31 and 35. Additional tests cover the API eligibility
policy, clearing retained state on a newer API, and parceling/applying the
single-page native collection publication. Changing an SDK value in a policy test
is not Android 17 framework validation. Physical-device verification remains open.

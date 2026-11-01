# 1.26 — adjustable corner-arrow navigation

Navigation bar settings now include Corner arrow navigation and an arrow-size
slider (16–80 dp). The selected edge determines placement and direction:
top/bottom use left and right corner arrows; left/right use up and down arrows.
Each arrow directly selects the previous or next widget, wrapping at the ends.
The pair overlays the content without reducing the widget area. Auto-contract
is bypassed in this mode so the arrows stay available. Visibility and opacity
follow the existing bar settings. Tap targets remain at least 48 dp.

The editor preview supports the same corner placement and direct navigation.
The option defaults off and persists per saved stack, together with size and
selected edge. Existing navigation modes remain available.

Validation covers API 31/35 layout across four edges and three sizes, full-area
content, clickable controls, persistence and the existing regression suite.
Physical launcher testing of this new mode remains pending. Signed with the
same recovered testing key as previous candidates.

# Custom sizing and actual placement bounds

The latest owner report separates two paths: Auto uses the usable home-screen
area, while custom width, custom height, or both can produce four-sided gaps.
This is a general host sizing problem, not specific to MyFitnessPal. The preview
redesign is deferred.

The native Auto path follows parent layout. Custom fitting instead needs a
numeric target size for the provider's logical frame and its uniform scale.
Previously that target came from AppWidgetManager options. Those are launcher
hints in dp, not a measurement of the laid-out host; fitting to a smaller hint
centers a smaller result inside the true host.

For custom sizing, a new placement first shows **Tap to fit this stack** on the
home screen. Its root fills the actual host. A real tap supplies that root's
source bounds through an explicit mutable PendingIntent. Widget Stack retains
only width and height, associated with that placement and the current launcher
options, orientation and density. It then publishes the original interactive
provider, using the actual measured area for aspect-preserving fitting. The
provider's contents are not cropped. There is no overlay in the steady state.

Changing a child width or height reuses this measurement. Each home-screen copy
has its own measurement. A launcher resize invalidates the affected copy; the
next custom-sized display asks for another tap. Auto needs no tap. Measurements
survive process restarts; deleting a placement removes its measurement. Stale
calibration publications cannot overwrite a newer measurement. Unknown, empty
or implausibly large bounds are ignored.

Android RemoteViews cannot execute a custom layout callback in the launcher.
The attempted automatic checked-state callback is unavailable: generic
setChecked is not an allowed remote method, and the dedicated setter suppresses
state-change responses. The explicit calibration tap makes this limitation
visible instead of silently fitting to unreliable hints.

A fit preserves aspect ratio, so mismatched ratios can leave space on one pair
of sides. At least one pair must reach the usable content edges. When the
provider and area have the same ratio, all four edges must align. Navigation
space is reserved using the existing bar rules.

Validation results and Android runtime evidence are recorded in the release
candidate notes. A controlled-host pass does not establish a physical HyperOS
pass for every third-party provider.

# Preview responsiveness (1.36)

The sizing editor now holds the provider snapshot captured when it opens. Global host/provider updates do not automatically rebuild it; **Refresh preview snapshot** explicitly captures the latest layout. Opening the editor no longer writes provider size options. Dimension edits still request provider layouts, and Apply/Cancel preserve the existing save/restore behavior.

Rapid scale/alignment/size changes are debounced for 120 ms. Closing the dialog cancels its pending render. AppWidgetHostView inflation uses a bounded background executor instead of inflating provider layouts synchronously on the UI thread. Preview collections can still load rows asynchronously; provider collection content is not a frozen screenshot.

This addresses a concrete repeated-work path in 1.35: every global preview notification immediately reapplied the preview on the UI thread. It does not establish a reproduction or confirmed fix of the reported Xiaomi near-hang after calibration. Measurement, alignment and the common preview/live embedding routine remain available. Business Calendar compatibility remains unresolved.

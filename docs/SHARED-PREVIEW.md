# Shared preview dimensions (1.38)

Launcher calibration and preview instance selection now live in **Edit saved stack → Navigation controls**. They are removed from Resize widget. Choose an instance, then **Match preview to launcher instance**. Go home and tap the measurement panel to return to configuration; the displayed preview dimensions refresh using that instance's measured bounds.

**Set preview dimensions** accepts width and height in screen pixels. **Use instance size** removes the manual override. These are editor settings only: they do not resize the launcher placement or change provider dimensions. A manual override is cleared when a different instance is selected or a measurement tap returns.

The navigation preview, navigation settings dialog, and Resize widget now read the same viewport. The navigation preview shows the full stack size; Resize widget shows the widget content area after space occupied by the navigation bar is deducted. Overlapping controls and corner arrows reserve no extra content space. Each whole preview is scaled down to fit the editor, so its aspect ratio follows the shared dimensions.

Navigation settings previews now also copy each child's existing dimensions, scale and alignment. Existing older measurement panels that return to a resize draft remain compatible. Native Xiaomi launcher behavior still requires device verification.

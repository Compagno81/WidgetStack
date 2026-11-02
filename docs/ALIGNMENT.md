# Scaling alignment and preview (1.35)

Resize widget now offers nine alignment choices combining left/centre/right and top/centre/bottom. Centre remains the default for existing widgets. Each child saves its own alignment. Alignment anchors the transformed widget bounds, including cropping when enlarged.

The editor uses the same RemoteViews embedding routine as the launcher, inside a viewport for one selected placement. Multiple copies have a preview instance selector. Width/height fields and sliders replace dragging the preview edges. The preview is reduced as a whole to fit the editor; widget zoom is applied inside that viewport exactly once.

Use **Measure selected launcher instance**, go home and tap its measurement panel. From 1.37 the tap returns directly to Resize widget, restoring the unsaved settings and using the actual launcher bounds. Provider refreshes can replace the panel before it is tapped; repeat the measurement if necessary.

The dimension label distinguishes measured launcher bounds from estimated launcher options. Estimates can differ from actual launcher allocation; measured bounds are invalidated on size/configuration changes. Navigation-bar state, provider updates, collection loading and launcher-specific rendering can still differ. This does not guarantee pixel equality on every launcher, or fix the Business Calendar issue. Preview actions remain blocked.

In 1.36, preview refresh is explicit and widget inflation is asynchronous; see PREVIEW-RESPONSIVENESS.md.

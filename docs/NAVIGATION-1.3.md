# Navigation changes in 1.3

Each stack stores its own bar position, thickness, opacity, visibility and slide setting. Existing stacks retain the right edge, 36 dp thickness, 20% opacity and visible bar. Animation defaults off.

The visual editor is available in stack configuration through the preview or Customize navigation bar. Position choices are top, bottom, left and right. Thickness ranges from 24 to 80 dp. Opacity ranges from 0 to 100%. Apply commits the changes together; Cancel leaves the saved settings intact.

Each end button occupies exactly one quarter of the bar's length. The middle half contains the dots and a settings gear. Each widget has a direct selection dot. Previous and next follow the configured widget order and wrap at either end. With fewer than two widgets, the end buttons are disabled. The bar fits all dots into its middle section, so a large number of widgets makes individual dot targets smaller; end buttons remain available. The bar occupies widget space along its selected edge; hosted size options are reduced along the correct axis, including responsive size lists.

The optional transition slides the incoming widget into view for 180 ms. Side bars use vertical entry; top and bottom bars use horizontal entry. Previous reverses the entry direction. This is a slide-in effect triggered by a tap, not dragging or continuous scrolling, and the outgoing widget is replaced immediately. Ordinary provider refreshes and configuration changes do not request an animation. A stock RemoteViews ViewFlipper supplies the effect and installs no touch interceptor over the hosted widget. Appearance and timing still need physical launcher testing.

The application continues to use native Android widgets, the stock launcher and Android gesture navigation. The testing signing key from 1.2 is retained, so 1.3 can update 1.2 in place.

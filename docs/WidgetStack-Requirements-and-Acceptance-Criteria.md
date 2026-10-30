# Widget Stack: consolidated product requirements and acceptance criteria

Version: 1.0  
Date: 30 October 2026  
Purpose: implementation, recovery, regression testing and release acceptance.

This document consolidates the user's requested behavior, including changes not yet delivered. It describes the target product, not a claim that every feature currently works. Requirements marked conditional need a working home-screen proof before they can be advertised. Numerical defaults described as proposed are implementation decisions, not previously confirmed user preferences.

## 1. Product objective and boundaries

Build a lightweight Android application dedicated to creating and managing stacks of actual widgets from other installed applications. Users prepare named stacks inside the app and choose a saved stack when placing Widget Stack from their launcher's widget picker.

A stack displays one selected widget at a time, with configurable navigation controls. It must preserve the hosted widget's normal interaction and responsive layout behavior. It must work with Pixel and Xiaomi HyperOS stock launchers while retaining native Android gesture navigation.

**Acceptance criteria**

- A user can create a saved stack, add at least two third-party widgets, place it on the home screen, switch between its widgets and use their normal actions.
- Two or more separately configured stacks can coexist on the home screen without changing each other's selection or controls.
- The product contains no lock-screen functionality, floating overlays, widget drawers or unrelated tools.
- Using it does not require replacing the launcher or disabling native Android navigation.
- A screenshot or bitmap-only imitation does not qualify as an interactive hosted widget.

## 2. Engineering investigation and upstream reuse

Reference project: https://github.com/zacharee/LockscreenWidgets

Inspect the source, licence, dependencies and widget-hosting implementation. Reuse only relevant functionality where feasible. Record Android restrictions on composing third-party widgets inside a launcher widget, including configuration activities, collection views, click actions, size options, updates, gesture handling and animations.

**Acceptance criteria**

- The repository records the upstream revision reviewed, code reused, required licence/attribution notices and relevant dependencies.
- Unused lock-screen, drawer and overlay components are excluded.
- An architecture note explains how live provider content and interactions reach the launcher, and identifies any non-public APIs or compatibility risks.
- Unsupported behavior has an explicit fallback; working behavior in the activity preview alone is insufficient evidence of home-screen support.
- Technical limitations are verified against the implementation and supported Android versions before making compatibility claims.

## 3. Saved stacks and placement

### STK-01: Manage stacks inside the app

Create, name, edit and delete saved stack definitions before placing them on the home screen. Each saved stack contains its widget entries, their order, starting widget, per-widget emulated dimensions and navigation settings.

**Acceptance criteria**

- A stack can be created and edited without placing it on the home screen.
- Closing and reopening the app preserves all saved stack data.
- Delete actually removes the selected saved stack and does not delete an unrelated stack.
- Deletion warns when a stack has placed instances and handles those instances predictably. Proposed behavior: show a safe “Stack deleted” state until the user removes or reconfigures the launcher widget.
- Removing a hosted entry or stack releases widget-host resources when they are no longer used.
- The per-stack configuration screen contains no “Add to home screen” button.

### STK-02: Choose a saved stack during launcher placement

Adding Widget Stack from the launcher's widget picker opens a chooser of existing saved stacks, with a route to create one if none exists.

**Acceptance criteria**

- Selecting a saved stack creates a working home-screen instance with its configured widgets, controls and starting selection.
- Cancelling placement does not leave an unwanted stack instance or allocated provider ID.
- Saved definitions and placed instances are represented distinctly in storage.
- Proposed copy semantics: instances share the saved definition and receive saved edits; each instance has an independent current selection and expanded/contracted state.
- Selecting a starting widget affects new instances without unexpectedly moving an existing instance to another widget.
- Reusing a definition does not make provider updates or navigation in one instance corrupt another.

### STK-03: Reliable editing and persistence

**Acceptance criteria**

- Both the launcher's “Edit” action and the stack's configuration control open the correct configuration.
- Applying edits updates existing home-screen instances promptly, without force-closing the app or launcher.
- Reboot, app process recreation and launcher restart retain definitions, widget bindings and settings.
- Saving one stack does not overwrite another stack or create accidental duplicates.

## 4. Widget discovery, binding and setup

### WGT-01: Android-style widget picker

Replace the plain text list with a modern picker organized by application, showing recognizable names, available sizes and visual examples.

**Acceptance criteria**

- Each provider entry shows its app identity, widget name, size information and a preview where available.
- Provider-supplied previews are preferred; unavailable previews have an honest placeholder.
- Size information distinguishes a provider's advertised size from this app's custom emulated dimensions.
- Eligible installed providers are discoverable, including Duolingo and MyFitnessPal when those apps expose widgets for the current user/profile.
- Filtering does not silently exclude a provider because of an unusual initial size or optional setup activity.
- Missing or restricted providers are explained where detectable, rather than presented as a successful add.
- Search by app/widget name is a recommended usability enhancement.

### WGT-02: Binding and provider configuration

**Acceptance criteria**

- Adding a widget uses the required Android binding and permission flow.
- A provider requiring setup opens its configuration activity and stores the result.
- A “Set up” action reopens supported provider configuration for an existing entry.
- Google Weather can be added or fail gracefully without crashing the stack configuration.
- WhatsApp's available configurable widget is included in the physical-device setup test.
- Cancelling, backing out, or encountering a missing/broken configuration activity does not crash the app or leave an unusable blank entry.
- Provider-specific restrictions and absent configuration capabilities are communicated accurately.

### WGT-03: Order, removal and selection

**Acceptance criteria**

- Each entry has accessible up-arrow and down-arrow controls, using arrow icons rather than wrapping “Move up/down” text.
- First/last entries disable or omit invalid reorder actions.
- A user can also reorder by holding a widget title and dragging it to another row.
- Removing an entry updates dots, selection and stored order.
- Removing the selected entry selects a valid remaining entry; removing the last entry shows a useful empty state.
- Setup and Remove actions fit on narrow screens without splitting words.
- A widget title can select the starting widget for new instances.

## 5. Live widget behavior

### LIVE-01: Interaction and updates

**Acceptance criteria**

- Exactly one provider widget is visible and interactive in each stack.
- Buttons, links and supported collection/list scrolling behave as they do in a direct host, subject to documented provider restrictions.
- Calendar list scrolling scrolls the calendar rather than changing the stack.
- Updates appear without reopening configuration, including after switching away and back.
- Input coordinates remain aligned with visible controls after scaling, resizing or navigation-bar changes.
- Provider uninstall, unavailable content or update failure shows a recoverable state instead of crashing the stack.
- The configuration preview's input-blocking layer is never applied to the home-screen widget.

### LIVE-02: Responsive home-screen resizing

**Acceptance criteria**

- Resizing a placed stack changes its available display area.
- With automatic dimensions, the provider receives suitable size information and can choose its responsive layout.
- With explicit emulated dimensions, resizing changes the fit into the real area without overwriting those explicit dimensions.
- Rotation and launcher size changes recompute layout and controls without requiring launcher restart.

## 6. Navigation controls

### NAV-01: Position, appearance and visibility

Support top, bottom, left and right placement, adjustable bar thickness, opacity and an option to hide the bar. Initial behavior is a visible right-side strip at approximately 20% opacity; auto-collapse is off by default.

**Acceptance criteria**

- Configuration includes a visual preview of all four placements.
- Thickness can be made narrow enough for short/narrow widgets, without breaking the layout.
- Opacity changes are reflected in preview and on the home screen.
- Hiding the bar removes its visual footprint and touch interception.
- Proposed hidden-bar behavior: retain the selected widget; configuration remains reachable through the launcher.
- Accessible control labels do not depend on orientation, symbols or color alone.

### NAV-02: Dots and previous/next controls

Provide dots for each entry and previous/next buttons. In the combined mode, the leading 25% and trailing 25% of the rail are navigation buttons; the middle 50% contains the indicators.

**Acceptance criteria**

- Tapping any visible dot selects the corresponding widget.
- Tapping either end button moves to the previous or next widget.
- Vertical bars use up/down arrows; horizontal bars use left/right arrows.
- A buttons-only mode omits indicators for compact stacks.
- A single-widget stack does not show misleading active navigation controls.
- If many indicators cannot fit, the UI keeps them usable without confusing indicator overflow scrolling with stack navigation.
- Proposed endpoint behavior: wrap from last to first and first to last, consistently across controls and preview.
- Repeated navigation works on the actual launcher, not only in the preview.

### NAV-03: Optional overlap

**Acceptance criteria**

- Overlap off reserves space for the bar and fits the provider into the remaining content area.
- Overlap on places controls over the provider and lets provider content use the full content area.
- Only the visible control hit region intercepts taps.
- Changing side, thickness, collapse mode or overlap updates both content bounds and hit regions correctly.
- Hidden or contracted portions do not leave invisible areas blocking the provider.

### NAV-04: Optional automatic contraction

Support an adjustable inactivity delay in seconds, starting at 2 seconds, followed by smooth contraction.

Modes:
1. Contract the bar to a configurable thickness.
2. Contract to a square at either end/corner, tapped to expand.

**Acceptance criteria**

- Auto-contraction can be disabled.
- Delay is entered/displayed in seconds; values below 2 are rejected or clamped with clear feedback.
- Contracted thickness is independently configurable and constrained to a valid size.
- Square mode offers both ends, with a visual indication of the resulting corner. Proposed mapping: top/bottom ends for vertical bars; left/right ends for horizontal bars.
- Tapping the contracted affordance expands the controls without also activating an obscured provider action.
- Interacting with stack controls resets the inactivity timer; active input is not interrupted by contraction.
- Expanded and contracted geometry, visibility and touch regions remain synchronized.
- The transition is visibly smooth where the launcher rendering path supports it, and respects system animation settings.
- If smooth home-screen animation cannot be implemented reliably, document that limitation and seek acceptance of a stable fallback; a preview-only animation does not fulfill this criterion.
- Returning from configuration, app process death or launcher recreation cannot leave controls permanently unresponsive.

## 7. Swipe navigation: requested but feasibility-dependent

Earlier requests included swiping horizontally or vertically across the widget to change the selected entry, or using the bar as the swipe surface. The clarification was that a swipe should change the hosted widget, not merely scroll an overflowing bar. The user subsequently accepted button-based alternatives, so swipe must remain a separately tracked request, not be presented as already delivered or silently assumed mandatory for the button-based release.

**Conditional acceptance criteria**

- Demonstrate real home-screen support before enabling or advertising swipe navigation.
- If supported, configuration offers mutually exclusive swipe surfaces: widget content or navigation bar; an Off option remains available.
- Content-swipe mode offers horizontal or vertical direction.
- Bar swipes change the selected entry; they are not just indicator-list scrolling.
- Provider controls have priority: a scrollable calendar/list scrolls internally rather than changing the stack.
- Taps, links, long presses where supported, and Android/launcher gestures remain functional.
- If gesture ownership cannot be resolved safely, keep swipe disabled for that surface and explain the limitation.
- A preview-only gesture implementation does not count as delivery.

## 8. Per-widget emulated layout size

### SIZE-01: Emulated provider dimensions, not cosmetic zoom

The user controls the width and height reported to each provider, independently of the space occupied by the stack on the home screen. The provider can then choose a compact or richer responsive layout. The resulting layout is fitted into the real stack area.

**Acceptance criteria**

- Width and height are independently configurable for each widget entry.
- Each axis supports Auto and an explicit value.
- Changing one entry's dimensions does not change another entry's dimensions.
- The provider receives consistent width/height options, including size variants supported by the implementation.
- MyFitnessPal can choose different layouts when the requested dimensions cross its own breakpoints, where the provider supports that behavior.
- No particular button or chart is guaranteed: providers control their own content and breakpoints.
- Changing dimensions updates the provider request, rather than only scaling an unchanged layout.
- There is no arbitrary blank-widget failure above the previous 100% setting.
- The old percentage control is replaced by clear dimension controls; existing data is migrated or reset transparently.

### SIZE-02: Friendly dimension controls

**Acceptance criteria**

- Width and height each have a slider with 50 px increments and an editable numeric field.
- Users can type values between slider increments.
- Each axis has an Auto action, clearly distinguished from an explicit zero-sized widget.
- Displayed units are px; conversion to Android logical sizing units is consistent and density-aware.
- The pixel convention is documented, including how saved settings behave if screen density changes.
- Invalid, empty, negative or excessive values do not crash or silently blank the widget.
- Supported bounds are visible and consistent across sliders, fields and drag controls.
- Apply saves; Cancel restores saved provider options and discards draft values.

### SIZE-03: Single-widget live sizing editor

**Acceptance criteria**

- The sizing editor shows only the widget being edited, with enough room to inspect it while changing dimensions.
- A light blue bounding box marks the requested layout.
- Dragging left/right edges changes width; top/bottom edges changes height; corners change both.
- Width and height values update during dragging and stay synchronized with fields and sliders.
- Proposed drag snapping: 50 px, matching sliders; exact typed values remain supported.
- The provider preview updates during editing, with bounded/debounced requests to avoid excessive work.
- The editor distinguishes requested dimensions from the smaller on-screen preview scale.
- The editor remains usable in portrait, landscape, small screens and with the keyboard visible.
- The provider itself is non-interactive in this editor; drag handles and sizing controls remain interactive.

### SIZE-04: Fill the available area while preserving aspect ratio

**Latest outstanding fix.** Custom-sized widgets must not remain unnecessarily small and centered with avoidable margins on all four sides.

Define the usable content rectangle after outer layout padding and any non-overlapping navigation space are accounted for. Fit the requested provider layout into that rectangle using one uniform scale:

`scale = min(availableWidth / logicalWidth, availableHeight / logicalHeight)`

**Acceptance criteria**

- Scale can be greater than or less than 1; small requested layouts are enlarged when space permits.
- Aspect ratio is preserved; no horizontal/vertical distortion or cropping is introduced by the stack.
- At least one pair of opposite layout edges reaches the usable rectangle's corresponding edges, within rounding tolerance.
- Letterboxing is permitted on only the other axis when aspect ratios differ.
- Matching aspect ratios fill both dimensions.
- Example: requested 600 × 400 in an available 300 × 300 area renders at 300 × 200.
- Example: requested 200 × 100 in an available 600 × 400 area renders at 600 × 300.
- The fit uses the launcher's actual available content size, not a stale or unrelated size estimate.
- Resize, rotation, bar overlap and bar contraction recompute the correct fit.
- Provider buttons still respond at their displayed positions after scaling.
- Provider-internal blank padding is distinguished from padding introduced by this app; do not crop provider content heuristically to hide internal padding.
- Changing fit does not replace the explicitly requested dimensions with the real home-screen dimensions.
- The home-screen screenshot's excessive-margin case is reproduced and verified fixed on a physical device.

## 9. Preview behavior and configuration design

### UI-01: Faithful but safe previews

**Acceptance criteria**

- The main stack preview uses actual available provider content and the same layout-selection, fitting and bar geometry rules as the home screen.
- Preview taps on provider content do not launch apps, modify data or trigger provider actions.
- Stack dots, previous/next controls, expansion controls and settings remain interactive.
- The input blocker covers only provider content and exists only in configuration.
- Preview limitations are explicit when real launcher dimensions or provider content are unavailable.
- Leaving the editor restores the correct saved provider size options.

### UI-02: Consolidated modern Android interface

Use the current stable Material design guidance supported by the selected Android UI toolkit. Keep the interface focused on stacks rather than diagnostic machinery.

Suggested organization:
- Stacks.
- Stack configuration: preview, name and widget order.
- Navigation: position, style, overlap, visibility and contraction.
- Individual widget: provider setup and emulated dimensions.
- App settings: purchase/support, licences and app version.

**Acceptance criteria**

- Related settings are grouped with clear labels and short explanations.
- Modern Android typography, spacing, colors, surfaces and light/dark themes are consistent.
- Arrow icons replace lengthy reorder button labels.
- Buttons do not wrap into broken words or overflow at narrow screen widths and enlarged font settings.
- Status/navigation bar insets prevent headings or controls colliding with system UI.
- Touch targets and accessibility labels remain usable, including for narrow visual rails.
- User-facing copy contains no Xiaomi-specific instructions or routine troubleshooting controls.
- Platform-specific diagnostics may remain in developer documentation or development builds.
- Configuration does not require repeatedly scrolling away from a preview to understand the effect of a setting.

## 10. Reliability, compatibility and resource use

**Acceptance criteria**

- Home-screen navigation is tested independently of activity preview navigation.
- Edits apply without force-stopping either application or launcher.
- Unique action identities prevent controls for different stacks/instances from colliding.
- Process recreation does not leave stale click actions, obsolete configuration or orphaned timers.
- Pixel stock launcher and Xiaomi HyperOS stock launcher receive physical-device acceptance tests.
- Tested Android, launcher, device and provider versions are recorded; untested combinations are not claimed verified.
- Native gesture navigation remains enabled throughout compatibility testing.
- No unnecessary persistent service, overlay permission or frequent polling is introduced.
- Idle/background CPU, memory, update behavior and battery impact are measured on representative devices and regressions investigated.
- Failure of one provider does not make the entire configuration or other stacks unusable.

## 11. Commercial release and monetisation

These are requested future product requirements, not a claim of an existing billing implementation.

### PAY-01: Free and paid tiers

Free tier: one stack with at most two widgets. Paid tier: removal of those product limits.

**Acceptance criteria**

- Limit checks cover creation, adding widgets and any copy/import/restore paths.
- A clear upgrade explanation appears before an action exceeding the free limits.
- Existing saved work is not silently deleted if entitlement changes.
- Paid entitlement survives restart and can be restored through the purchase platform.
- Cancellation, pending purchase, failed purchase, refund and offline entitlement states are handled predictably.
- Paid removes artificial stack/widget limits; device/platform constraints still apply.
- Proposed clarification: the free limit applies to one saved definition and one placed instance, preventing repeated placement from bypassing the intended limit. Confirm this commercial rule before billing implementation.

### PAY-02: Support/donate and store readiness

**Acceptance criteria**

- A voluntary support/donate option is distinguishable from the purchase that unlocks features.
- Billing and donation mechanisms are checked against store policy at implementation/release time.
- The final packaging decision, one app with an upgrade or separate editions, is documented before release. Proposed default: one app with an upgrade.
- Store assets, privacy disclosures, third-party notices and purchase wording accurately describe actual behavior.
- Release builds remove routine troubleshooting UI and device-brand-specific copy.
- Release signing credentials are controlled by the owner and preserved securely, separately from public source.

## 12. Source control, recovery and delivery

### SRC-01: Durable, owner-accessible repository

**Acceptance criteria**

- All source, resources, build configuration, tests, licences and requirements are committed to a Git repository accessible to the owner.
- Every meaningful development milestone is committed and pushed; the remote commit is verified.
- A temporary workspace or download link is never the only copy of source.
- Until the remote is available, a verified full Git bundle is saved durably after each source milestone.
- No credentials, signing keys, machine-local SDK paths, caches or generated build directories are committed.
- Signing material is backed up separately and documented sufficiently to preserve upgrade compatibility.
- Issues and release notes track known defects, unsupported behavior and unfinished requests.

### SRC-02: Honest reconstruction

Recovery baseline: preserved source 1.14 and preserved APK 1.18. Later source changes require reconstruction.

**Acceptance criteria**

- Recovered original source and reconstructed changes have distinct, honest commits.
- Reconstructed source is not tagged or described as the original source used for a historical APK.
- Reconstructed features are checked against requirements and the preserved APK where feasible.
- Missing evidence is recorded; decompilation or screenshots alone are not treated as proof of equivalence.
- The next release is built from the recovered/reconstructed repository with a new version identity.

### REL-01: Repeatable builds and durable releases

**Acceptance criteria**

- The Android Studio project opens and builds from a clean checkout using documented toolchain versions.
- A Gradle wrapper and reproducible dependency/build instructions are included.
- Each APK has a version name/code, source commit, release tag, checksum and matching source archive.
- APK upgrades preserve data and use the intended signing identity.
- Each release has a durable owner-accessible location, such as repository release assets, containing the APK, source and validation notes.
- Any convenient chat download is supplementary to durable storage.
- The delivered APK is the exact build tested and described in release notes.
- Compilation success, automated tests, emulator tests and physical-device tests are reported separately.

## 13. Verification plan and release gates

### Automated tests

Cover saved-stack persistence/deletion, selection and reorder boundaries, per-instance isolation, settings serialization/migration, navigation action identity, size-option generation, density conversion, fit calculations, preview input isolation, editor cancellation and provider configuration failures.

**Pass condition:** all applicable tests pass on the release commit; failures are fixed rather than ignored or removed simply to obtain a green run. Any intentionally skipped check has a documented reason.

### Build and static checks

Run clean release-candidate compilation, unit tests and applicable Android lint checks. Verify package/version metadata, signing and APK checksum.

**Pass condition:** no unexplained build failures or release-blocking diagnostics; a clean checkout can reproduce the documented build process.

### Emulator/instrumented tests

Exercise real RemoteViews inflation, binding with test providers, responsive layout breakpoints, configuration result handling, actual input targets and update behavior. Use controllable test providers rather than relying solely on third-party account data.

**Pass condition:** automated and manual launcher-host scenarios work, including custom sizes larger and smaller than available bounds.

### Physical-device acceptance matrix

| Scenario | Pixel stock launcher | Xiaomi HyperOS stock launcher |
| --- | --- | --- |
| Add saved stack through launcher; edit through launcher | Required | Required |
| Multiple stacks and repeated navigation | Required | Required |
| Google Weather add/configuration | Where available | Where available |
| WhatsApp setup; Duolingo/MyFitnessPal discovery | Where installed | Where installed |
| Calendar internal scrolling and provider buttons | Required | Required |
| All rail sides, dots/buttons-only, overlap and hide | Required | Required |
| Delay, contraction modes and expansion | Required | Required |
| Custom dimensions, drag editor and aspect-preserving fit | Required | Required |
| Preview blocks provider actions; home screen permits them | Required | Required |
| Reboot, process death, resize and app upgrade | Required | Required |
| Native gesture navigation and idle behavior | Required | Required |

A provider-specific failure must be recorded with app/device versions and reproduction steps. A feature is not verified merely because another provider works.

### Regression blockers

Do not declare the next functional release ready while any of these remains reproducible:
- Navigation controls work in preview but not on the home screen.
- Settings require launcher force-close to appear.
- Deleting a stack does nothing.
- Provider configuration force-closes the app.
- Preview interaction blocking leaks onto the home screen.
- Valid custom dimensions cause a blank widget.
- The stack adds avoidable margins on all four sides instead of fitting to the available area.
- Source or release assets exist only in temporary storage.

## 14. Current status and outstanding work

This is a requirements baseline, not a new implementation or test report.

| Area | Status at preparation |
| --- | --- |
| Original saved source | Version 1.14 recovered and committed locally |
| Latest preserved binary | Version 1.18 APK recovered |
| Changes after source 1.14 | Require reconstruction and validation in the repository |
| Independent dimensions, pixel controls and drag preview | Reported in 1.18; source reconstruction and regression verification outstanding |
| Aspect-preserving enlargement to available bounds | Latest requested fix; not yet delivered |
| Remote source repository | Not yet established/verified in the available work |
| Durable Git recovery checkpoint | Created; does not by itself complete remote repository setup |
| Swipe navigation and launcher animations | Require explicit feasibility evidence; do not claim delivered |
| Monetisation and Play Store release | Future work requiring implementation and release-time policy review |
| Pixel/HyperOS acceptance | Requires recorded physical-device results for the next candidate |

## 15. Definition of done

A release is ready when its included requirements pass their acceptance criteria; the regression blockers are resolved; the exact tested APK and corresponding committed source are durably available to the owner; and release notes distinguish verified behavior, conditional capabilities and remaining physical-device checks.

The full product is complete only when all mandatory requirements are met and the owner explicitly accepts any documented platform limitation or deferred commercial feature. A build passing compilation alone does not meet this definition.


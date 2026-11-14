# 1.41: Retain widget views during navigation

Base: 1.40-reconstructed, source 6f094d64630df5c19087c25d0e1eccfe43fd5eed.
The owner-supplied 1.40 APK matched its archived SHA-256 exactly:
68ca39328ca0a03dd764081954cc556573d28e536a3f00a268c33f1248018e83.

## Changes

The launcher receives a stable outer layout containing separately scoped controls
and a ViewFlipper of retained provider pages. Stable child identities allow the
framework to reuse each provider view on full-snapshot reapply. Controls are
recreated separately, preserving the protection against foreign resource-ID
collisions and stale click bindings. Enabled switching animation uses a fade
between retained pages; bar expansion/contraction keeps its existing animation.

Android merges partial widget updates into the full stored snapshot before
notifying the launcher. This implementation therefore publishes complete,
reconstructible snapshots and verifies full reapply, not only partial actions.
Unchanged provider snapshots omit imperative scroll/selection commands so a
provider's initial scroll position is not reapplied during navigation. New
provider snapshots retain those commands. Dynamic subtrees with clear/add actions
receive stable child identities. Custom dimensions, scale, alignment, profile
filtering and corner-arrow settings remain in use.

Navigation no longer republishes size options to every provider. Auto-contract
uses stable expanded-bar provider size hints, while actual content padding can
change with the controls. Starting the already-running foreground service no
longer queues another full refresh of every stack.

## Limits and compatibility paths

- Collection retention requires Android 15/API 35 or newer. API 31 framework
  adapters require actions at the outer host level and fail inside these nested
  pages. Older Android versions retain the prior single-widget collection path.
- Actual provider updates, configuration/size/profile changes, launcher recreation
  and compatibility fallback may reset state. This is not persistent storage of
  scroll coordinates across launcher/process restarts.
- If provider code appends dynamic views without clearing its container, that
  page is recreated instead of risking duplicated content. Other pages can stay.
- Data notifications use unscoped view IDs. If notification targeting is ambiguous,
  the selected widget returns to the single-widget path before the data refresh.
  Notifications for hidden widgets are deferred until they are selected. This
  avoids refreshing a different calendar with the same numeric resource ID.
- Keeping pages attached increases launcher memory. If publication exceeds a
  platform limit, the active widget falls back to the former single-widget path.
- Real AppGrid/MyFitnessPal/calendar widgets on the user's launcher still require
  device confirmation. No physical-device validation is claimed.

## Required checks

- Twenty alternating full snapshots preserve both ListView objects, adapters and
  distinct nonzero scroll positions, through Parcel serialization, on API 31/35.
- Bar expansion/contraction, custom sizing and corner arrows preserve list state.
- API 35 native collection rows retain position despite an initial provider scroll
  command. API 31 uses a working collection fallback rather than blank content.
- Existing stale-control and foreign-ID collision regressions remain green.
- Repeated service starts do not schedule another stack refresh.
- Run the complete framework suite, lint, build, APK version and signature checks.

Validation: 316 tests passed, zero failures/errors; lint and APK assembly passed.
APK signature matches 1.40. Source: 3d1940f332b54c781eefa6b870237f87fbfe1224.

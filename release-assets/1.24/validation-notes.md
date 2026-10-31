# 1.24 — independent width and height

Changing height now leaves width alone. Auto uses MATCH_PARENT on that axis;
custom dimensions use physical pixel layout sizes. The provider lays out its
contents in that rectangle. No uniform scale transform is applied to custom
widgets, and no calibration tap is required. Dimensions larger than the slot
are centered and clipped rather than shrinking the other axis.

This corrects the resizing contract from 1.23. That version fitted a logical
rectangle uniformly into the host, coupling the two axes. Its passing fitting
tests did not establish the independent-axis behavior the owner expects.

The resize editor keeps explicit pixel values exact rather than displaying a
rounded dp-to-pixel conversion (for example 850 becoming 852). Auto provider
layout hints can still be approximate; the actual Auto axis follows its parent.
Third-party providers retain control over their internal layouts.

Validation: full framework suite on API 31/35, APK build and lint. New regression
covers a 1060 × 865 host at 420 dpi, changing only height to 864, 850 and 1 while
asserting that width remains 1060. Existing custom geometry checks now verify
independent physical sizes at mdpi/xhdpi and touch delivery within visible bounds.
Android instrumentation cases have been updated for independent dimensions;
the 1.24 instrumentation cases have not yet been run on a device. Historical
1.23 emulator evidence validates the previous fitting behavior, not this change.
Physical MyFitnessPal/HyperOS acceptance remains open.

Signed with the same recovered testing key, excluded from Git. APK and matching
source ZIP accompany this test release.

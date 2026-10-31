# Android runtime fitting evidence

The old-build failure is an intentional negative control. Both new-build logs end
with `OK (1 test)`; each test covers three host shapes and real pointer delivery.

The old screenshot/geometry capture shows an 80 × 60 provider inside a 400 × 300
host with an obsolete percentage still present alongside Auto axes. Widget IDs
can differ between separate runs; these are controlled fixture tests, not a copy
of the owner's MyFitnessPal provider.

Final screenshots have no `-initial` or `-failure` suffix. Initial frames are
retained to explain why the test waits for a stable compositor frame; pixel
assertions remain exact rather than accepting a color tolerance.

The fixture launcher runs under its own UID/process. A separate in-process host
test also passes. These results do not establish Pixel Launcher, HyperOS or
MyFitnessPal physical-device acceptance. See summary.json and release notes.

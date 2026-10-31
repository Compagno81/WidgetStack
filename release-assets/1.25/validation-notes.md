# 1.25 — manual scale per stacked widget

The Resize widget dialog now has a Scale slider from 25% to 300%, a live
percentage label, a preview, and Reset scale to 100%. Apply saves it for the
selected widget; Cancel discards the draft. Scaling enlarges or shrinks both
axes proportionally around the center. Enlarged edges are clipped by the stack.
Width/height still control layout independently. Auto dimensions support scale.

The new setting is separate from obsolete logicalSizePercent data, so old
settings cannot unexpectedly enable zoom. It persists by child widget identity
and defaults to 100%. Provider actions retain native transformed touch handling.

Validation: framework tests on API 31/35, including independent persistence,
legacy default behavior, transforms and provider taps at 50/100/150/300%; APK
build and lint. Physical HyperOS/MyFitnessPal and emulator checks of this new
version have not been performed. Same recovered testing signing key.

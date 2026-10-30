# Widget Stack recovery

Android application for stacks of interactive third-party widgets.

## Current checkpoint

This repository currently preserves the supplied requirements, not application
source. It contains no buildable Android application or verified implementation.

See [requirements and acceptance criteria](docs/WidgetStack-Requirements-and-Acceptance-Criteria.md).

The requirements report recovered version 1.14 source and a preserved version
1.18 APK. Neither artifact was available in this workspace when this checkpoint
was created. Those historical recovery claims have not been independently verified.

## Next work

1. Obtain the recovered source or full Git bundle, and the preserved APK.
2. Import recovered source in a separate commit recording its provenance.
3. Reconstruct later changes separately, without claiming historical equivalence.
4. Verify independent provider width/height settings and implement SIZE-04:
   aspect-preserving fit, including enlargement, using actual content bounds.
5. Run the applicable build and acceptance checks and record untested device cases.
6. Push milestones to an owner-accessible GitHub repository and verify remote SHAs.

No GitHub remote is configured yet. A local bundle is a recovery aid, not proof
of durable remote storage. Keep signing keys and credentials outside the repository.

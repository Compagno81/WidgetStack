# WidgetStack maintenance

- Canonical repository: https://github.com/Compagno81/WidgetStack. Check remote branches and release tags before claiming that source is missing or reconstructing an APK.
- Check the APK checksum against the release-artifacts branch and identify its matching source tag before changing a reported release.
- Preserve newer sizing, alignment, visibility-profile, Tasker and navigation features. Do not substitute an older recovery baseline.
- Commit and push source milestones. Every delivered build needs a matching source commit/tag, APK checksum, source archive, validation notes and persistent download. Never make scratch storage the only copy.
- Keep signing material and machine-specific build configuration out of Git. Verify the update certificate against the previous APK.
- Test native RemoteViews apply/reapply and parceling on supported framework levels. A plain partial-update test is insufficient: Android sends a merged full snapshot to the launcher.
- Do not claim physical-device or provider compatibility from framework tests. Record fallbacks and any unfinished device verification in the release notes.

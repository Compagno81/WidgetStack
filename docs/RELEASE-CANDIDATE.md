# Widget Stack 1.41 reconstructed candidate

Navigation retains widget pages rather than replacing the selected provider on
 every tap. The bar can expand/collapse independently; repeated foreground-service
starts no longer queue a full refresh. Existing sizing, profiles and corner arrows
remain. Switching animation uses a fade between retained pages.

Collection retention targets Android 15 and later. Older Android versions keep
the existing collection rendering path. Real provider updates, incompatible
provider hierarchies, resource-ID notification collisions, launcher restarts and
platform memory limits may still reset a page. See NAVIGATION-STATE-1.41.md.

Automated validation results and exact source/APK hashes accompany the release.
Physical testing with the user's launcher and real providers remains outstanding.

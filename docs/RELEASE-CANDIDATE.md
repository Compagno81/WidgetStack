# 1.30 — row-only refreshes and Android 17 publication compatibility

The owner's 1.29 report identifies Xiaomi Android 17/API 37. Agenda interaction
continues to work; switching widgets stops visibly responding. The log records
a received navigation tap, a changed selection and many publication submissions
for two home-screen copies. Submitting an update does not prove launcher apply.

Two issues are corrected:

- Collection readiness and provider data-change callbacks no longer schedule
  full stack publications. They request row refreshes only, and only for copies
  currently showing that child. Old readiness events cannot refresh a different
  agenda just because its view IDs overlap. Avoiding root reconstruction removes
  a feedback path that can replace adapters and trigger further readiness events.
- The private Android 15 publication workaround is restricted to API 35. API 36,
  37 and future versions use the Android platform publication path. The API 35
  regression result did not establish private-API compatibility on API 37.

The report now distinguishes collection row requests from provider layout
updates and publication submissions. It includes active selection/count for
each placement and the publication path. No widget contents are included.

Regression tests repeatedly deliver readiness events with two copies showing
different children: no root publications may be queued, only the active copy
may receive row notifications, and selection must remain unchanged. A policy
test checks API 37 selects the standard path. Existing API 31/35 tests, APK build
and lint remain required. These are not native Android 17 tests; physical agenda
switching on the reported Xiaomi device remains unverified.

Same recovered testing key, with matching source and APK ZIP provided. The
1.29 direct-publication experiment is no longer used on this owner's API level.

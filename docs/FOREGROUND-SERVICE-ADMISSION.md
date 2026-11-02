# Foreground-service admission (1.39)

The supplied Xiaomi crash report names Widget Canvas (`com.kevin.widgetcanvas.HostService.onCreate`, line 24), not Widget Stack. Android rejected `startForeground()` with `ForegroundServiceStartNotAllowedException`. Closing all recent tasks may lead to a background restart, but that trigger is not proven by the trace.

Widget Stack had the same unguarded lifecycle call. Its existing catch around `startForegroundService()` cannot catch exceptions thrown later inside service creation. Stack now handles denied foreground promotion and foreground permission denial, stops the rejected service, and returns START_NOT_STICKY if a start callback follows. A rejected service does not start the host listener or tear down a listener owned by another caller. Permitted foreground starts keep their prior behavior.

Tests inject both denial exceptions during service creation and verify a clean, non-sticky stop; a normal promotion is also checked. Xiaomi close-all behavior remains unverified. Reopen Widget Stack to resume hosting if Android refuses a background start.

This change applies to Widget Stack only and does not fix the Canvas package identified by the screenshot. Canvas source was not available through the current repository connection at implementation time.

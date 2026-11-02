# Measurement return (1.37)

After choosing **Measure selected launcher instance**, go home and tap the measurement panel. The tap opens that stack's configuration directly and reopens Resize widget for the same child and instance. Width, height, scale and alignment are carried as an unsaved draft. Apply saves them; Cancel leaves saved settings unchanged.

The panel uses a direct activity PendingIntent rather than launching an activity from a broadcast receiver, which recent Android versions restrict. Existing measurement token, ownership, source-bounds and size-signature validation still protects calibration. The configuration screen returns even if a launcher omits usable bounds; the preview continues to label the size as estimated in that case.

Native Xiaomi launcher return behavior still needs device verification. No stacks or widget bindings are removed.

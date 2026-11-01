package com.kevin.widgetstack;

import android.app.Application;
import android.util.Log;
import org.lsposed.hiddenapibypass.HiddenApiBypass;

public final class StackApplication extends Application {
    static boolean compatibility;
    @Override public void onCreate() {
        super.onCreate();
        try {
            compatibility = HiddenApiBypass.addHiddenApiExemptions(
                "Landroid/widget/RemoteViews", "Landroid/appwidget/AppWidgetHost",
                "Landroid/appwidget/AppWidgetManager", "Landroid/content/Context",
                "Lcom/android/internal/widget/IRemoteViewsFactory", "Lcom/android/internal/appwidget/IAppWidgetService");
        } catch (Throwable e) { Log.e("WidgetStack", "Widget compatibility unavailable", e); }
    }
}

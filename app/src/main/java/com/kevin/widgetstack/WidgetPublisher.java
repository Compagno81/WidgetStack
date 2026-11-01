package com.kevin.widgetstack;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.os.Build;
import android.widget.RemoteViews;
import java.lang.reflect.*;

/** Keep interactive legacy adapters out of Android 15's shared conversion queue. */
final class WidgetPublisher {
 static boolean usesDirectPublication(int api){return api==35;}
 static void update(Context c,int id,RemoteViews views){
  AppWidgetManager manager=AppWidgetManager.getInstance(c);
  if(!usesDirectPublication(Build.VERSION.SDK_INT)){manager.updateAppWidget(id,views);return;}
  try{
   Field field=AppWidgetManager.class.getDeclaredField("mService");field.setAccessible(true);Object service=field.get(manager);
   if(service==null){manager.updateAppWidget(id,views);return;}
   // The bridge has already prepared nested collection adapters and item caches.
   // Let the launcher bind legacy adapters asynchronously, rather than letting
   // a slow agenda block every subsequent publication in the shared executor.
   Class<?> contract=Class.forName("com.android.internal.appwidget.IAppWidgetService");
   contract.getMethod("updateAppWidgetIds",String.class,int[].class,RemoteViews.class).invoke(service,c.getPackageName(),new int[]{id},views);
  }catch(InvocationTargetException e){throw new IllegalStateException("Widget publication failed",e.getCause());}
   catch(ReflectiveOperationException e){throw new IllegalStateException("Direct widget publication unavailable",e);}
 }
}

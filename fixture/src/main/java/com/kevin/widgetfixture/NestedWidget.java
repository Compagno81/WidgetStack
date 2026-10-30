package com.kevin.widgetfixture;
import android.appwidget.*;import android.content.*;import android.widget.*;
public final class NestedWidget extends AppWidgetProvider {
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids){RemoteViews inner=new RemoteViews(c.getPackageName(),R.layout.plain);inner.setTextViewText(R.id.fixture_plain,"Nested weather "+id);for(int depth=0;depth<4;depth++){RemoteViews parent=new RemoteViews(c.getPackageName(),R.layout.nested);parent.addView(R.id.fixture_nested,inner);inner=parent;}m.updateAppWidget(id,inner);}}
}

package com.kevin.widgetfixture;
import android.app.*;import android.appwidget.*;import android.content.*;import android.os.Bundle;import android.widget.*;
public class PlainWidget extends AppWidgetProvider {
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids){RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.plain);v.setTextViewText(R.id.fixture_plain,"External widget "+id);v.setOnClickPendingIntent(R.id.fixture_plain,PendingIntent.getActivity(c,id,new Intent(c,OpenedActivity.class).putExtra("row",-1),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));m.updateAppWidget(id,v);}}
 @Override public void onReceive(Context c,Intent i){super.onReceive(c,i);if("com.kevin.widgetfixture.REFRESH".equals(i.getAction())){AppWidgetManager m=AppWidgetManager.getInstance(c);for(int id:m.getAppWidgetIds(new ComponentName(c,PlainWidget.class))){RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.plain);v.setTextViewText(R.id.fixture_plain,"Updated external widget "+id);m.updateAppWidget(id,v);}}}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle b){onUpdate(c,m,new int[]{id});}
}

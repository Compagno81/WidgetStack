package com.kevin.widgetfixture;
import android.app.*;import android.appwidget.*;import android.content.*;import android.net.Uri;import android.widget.*;
public class ListWidget extends AppWidgetProvider {
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids){RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.list);Intent service=new Intent(c,RowsService.class).setData(Uri.parse("fixture://rows/"+id));v.setRemoteAdapter(R.id.fixture_list,service);v.setPendingIntentTemplate(R.id.fixture_list,PendingIntent.getActivity(c,id,new Intent(c,OpenedActivity.class),PendingIntent.FLAG_MUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));m.updateAppWidget(id,v);}}
}

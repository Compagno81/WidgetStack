package com.kevin.widgetfixture;
import android.content.*;import android.widget.*;
public class RowsService extends RemoteViewsService {
 @Override public RemoteViewsFactory onGetViewFactory(Intent i){return new RemoteViewsFactory(){public void onCreate(){}public void onDataSetChanged(){}public void onDestroy(){}public int getCount(){return 40;}public RemoteViews getViewAt(int p){RemoteViews v=new RemoteViews(getPackageName(),R.layout.row);v.setTextViewText(R.id.fixture_row,"Calendar event "+p);v.setOnClickFillInIntent(R.id.fixture_row,new Intent().putExtra("row",p));return v;}public RemoteViews getLoadingView(){return null;}public int getViewTypeCount(){return 1;}public long getItemId(int p){return p;}public boolean hasStableIds(){return true;}};}
}

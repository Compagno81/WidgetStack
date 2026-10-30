package com.kevin.widgetfixture;
import android.content.*;import android.database.Cursor;import android.net.Uri;import android.os.Bundle;
/** Test fixture only. Records external PendingIntent delivery across UID boundaries. */
public final class EventsProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 @Override public Bundle call(String method,String arg,Bundle extras){
  if("allocate_home".equals(method)){
   android.appwidget.AppWidgetHost host=new android.appwidget.AppWidgetHost(getContext(),7777);
   int id=host.allocateAppWidgetId();
   boolean bound=android.appwidget.AppWidgetManager.getInstance(getContext()).bindAppWidgetIdIfAllowed(id,new ComponentName("com.kevin.widgetstack","com.kevin.widgetstack.StackProvider"));
   Bundle result=new Bundle();result.putInt("id",bound?id:-1);return result;
  }
  if("delete_home".equals(method)){new android.appwidget.AppWidgetHost(getContext(),7777).deleteAppWidgetId(extras.getInt("id"));return Bundle.EMPTY;}
  android.content.SharedPreferences p=getContext().getSharedPreferences("events",0);
  if("reset".equals(method))p.edit().putInt("row",-999).putInt("config_id",-1).commit();
  if("record".equals(method))p.edit().putInt("row",extras.getInt("row")).commit();
  if(method.startsWith("config_")){FixtureConfigureActivity a=FixtureConfigureActivity.current;if(a!=null)new android.os.Handler(android.os.Looper.getMainLooper()).post(()->a.complete("config_accept".equals(method)));}
  Bundle b=new Bundle();b.putInt("config_id",p.getInt("config_id",-1));b.putInt("row",p.getInt("row",-999));return b;
 }
 public Cursor query(Uri u,String[] p,String s,String[] a,String sort){return null;}public String getType(Uri u){return null;}public Uri insert(Uri u,ContentValues v){return null;}public int delete(Uri u,String s,String[] a){return 0;}public int update(Uri u,ContentValues v,String s,String[] a){return 0;}
}

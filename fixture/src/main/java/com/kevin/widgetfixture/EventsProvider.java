package com.kevin.widgetfixture;
import android.content.*;import android.database.Cursor;import android.net.Uri;import android.os.Bundle;
/** Test fixture only. Records external PendingIntent delivery across UID boundaries. */
public final class EventsProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 @Override public Bundle call(String method,String arg,Bundle extras){
  if("launcher_bounds".equals(method)||"launcher_resize".equals(method)){
   Bundle result=new Bundle();LauncherActivity activity=LauncherActivity.current;if(activity==null)return result;
   java.util.concurrent.CountDownLatch ready=new java.util.concurrent.CountDownLatch(1);
   new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
    try{
     android.appwidget.AppWidgetHostView widget=activity.widget;
     if("launcher_resize".equals(method)&&extras!=null)widget.setLayoutParams(new android.widget.FrameLayout.LayoutParams(extras.getInt("width"),extras.getInt("height"),android.view.Gravity.CENTER));
     int[] xy=new int[2];widget.getLocationOnScreen(xy);result.putInt("left",xy[0]);result.putInt("top",xy[1]);result.putInt("width",widget.getWidth());result.putInt("height",widget.getHeight());
     int id=activity.getResources().getIdentifier("content","id","com.kevin.widgetstack");android.view.View area=widget.findViewById(id);
     if(area instanceof android.view.ViewGroup&&((android.view.ViewGroup)area).getChildCount()>0){android.view.View child=((android.view.ViewGroup)area).getChildAt(0);result.putInt("provider_width",child.getWidth());result.putInt("provider_height",child.getHeight());result.putInt("provider_left",child.getLeft());result.putInt("provider_top",child.getTop());}
    }finally{ready.countDown();}
   });
   try{if(!ready.await(10,java.util.concurrent.TimeUnit.SECONDS))return new Bundle();}catch(InterruptedException e){Thread.currentThread().interrupt();return new Bundle();}
   return result;
  }
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

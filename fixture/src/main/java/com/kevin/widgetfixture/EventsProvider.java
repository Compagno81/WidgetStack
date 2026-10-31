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
     android.appwidget.AppWidgetHostView widget=activity.widget;result.putInt("home_id",activity.homeId);
     if("launcher_resize".equals(method)&&extras!=null)widget.setLayoutParams(new android.widget.FrameLayout.LayoutParams(extras.getInt("width"),extras.getInt("height"),android.view.Gravity.CENTER));
     int[] xy=new int[2];widget.getLocationOnScreen(xy);result.putInt("left",xy[0]);result.putInt("top",xy[1]);result.putInt("width",widget.getWidth());result.putInt("height",widget.getHeight());
     android.view.View child=findProvider(widget);
     if(child!=null){android.graphics.Rect visible=new android.graphics.Rect();if(child.getGlobalVisibleRect(visible)){result.putInt("provider_width",visible.width());result.putInt("provider_height",visible.height());result.putInt("provider_left",visible.left-xy[0]);result.putInt("provider_top",visible.top-xy[1]);}}

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
 private android.view.View findProvider(android.view.View root){
  if(root instanceof android.widget.TextView){String text=((android.widget.TextView)root).getText().toString();if(text.startsWith("External widget")||text.startsWith("Updated external widget"))return root;}
  if(root instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)root;for(int i=0;i<group.getChildCount();i++){android.view.View found=findProvider(group.getChildAt(i));if(found!=null)return found;}}
  return null;
 }
 public Cursor query(Uri u,String[] p,String s,String[] a,String sort){return null;}public String getType(Uri u){return null;}public Uri insert(Uri u,ContentValues v){return null;}public int delete(Uri u,String s,String[] a){return 0;}public int update(Uri u,ContentValues v,String s,String[] a){return 0;}
}

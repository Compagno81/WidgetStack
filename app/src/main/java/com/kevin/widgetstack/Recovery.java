package com.kevin.widgetstack;
import android.app.PendingIntent;
import android.content.*;
import android.widget.RemoteViews;

/** Reversible pause. Does not edit saved stacks, selections or child bindings. */
final class Recovery {
 private static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("recovery",0);}
 static void initialize(Context c){if(!prefs(c).contains("initialized"))prefs(c).edit().putBoolean("initialized",true).putBoolean("paused",false).commit();}
 static boolean paused(Context c){return prefs(c).getBoolean("paused",false);}
 static RemoteViews view(Context c,int id){
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.stack_placeholder);
  v.setTextViewText(R.id.empty,"Widget stacks paused\nTap to open Widget Stack");
  Intent open=new Intent(c,MainActivity.class).setData(android.net.Uri.parse("widgetstack://recovery/"+id));
  v.setOnClickPendingIntent(R.id.empty,PendingIntent.getActivity(c,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));return v;
 }
 static void publish(Context c,int id){
  if(!StackRenderer.owns(c,id))return;
  // Use a fresh public client which has never submitted legacy collections.
  // A plain recovery view must not wait behind this client's prior agendas.
  Context clean=c.createConfigurationContext(new android.content.res.Configuration(c.getResources().getConfiguration()));
  android.appwidget.AppWidgetManager.getInstance(clean).updateAppWidget(id,view(c,id));
 }
 static void pause(Context c){
  prefs(c).edit().putBoolean("initialized",true).putBoolean("paused",true).commit();StackHost.get(c).stop();
  for(StackState s:new StackStore(c).all())if(s.id>=0){BarController.cancel(s.id);publish(c,s.id);}
  c.stopService(new Intent(c,HostService.class));
 }
 static void resume(Context c){
  prefs(c).edit().putBoolean("paused",false).commit();HostService.ensure(c);StackHost.get(c).sync();
  for(StackState s:new StackStore(c).all())if(s.id>=0){StackRenderer.updateSizes(c,s.id);StackRenderer.update(c,s.id);}
 }
}

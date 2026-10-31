package com.kevin.widgetstack;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Explicit PendingIntent endpoint. No exported intent-filter or lifecycle dispatch. */
public final class StackControlReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){handle(context,intent);}

 static boolean handle(Context c,Intent i){
  String action=i.getAction();
  if(MeasuredViewport.ACTION.equals(action)){MeasuredViewport.receive(c,i);return true;}
  if(StackProvider.PINNED.equals(action)){
   SavedStacks.place(c,i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1),i.getIntExtra(SavedStacks.EXTRA_SAVED_ID,-1));return true;
  }
  boolean select=StackProvider.SELECT.equals(action);
  if(!select&&!StackProvider.EXPAND.equals(action))return false;
  int id=i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
  boolean own=StackRenderer.owns(c,id);
  StackDiagnostics.note(c,select?"navigation received":"expand received",id,"owned="+own);
  if(!own)return true;
  StackStore store=new StackStore(c);store.migrateLegacy();StackState state=store.get(id);
  if(state==null){StackDiagnostics.note(c,"navigation ignored",id,"no saved assignment");return true;}
  int before=state.selected,direction=i.getIntExtra("direction",0);
  if(select){
   if(direction!=0)state.step(direction);else state.select(i.getIntExtra("child",-1));
   store.selectOnHome(id,state.selected);
  }
  // A receiver must finish promptly. Keeping it pending until contraction blocks
  // subsequent launcher broadcasts on real devices. The foreground host owns timers.
  HostService.ensure(c);
  boolean expanding=BarController.expand(c,id);
  StackHost.get(c).sync();StackRenderer.updateSizes(c,id);
  StackRenderer.updateBarMotion(c,id,before==state.selected?0:direction!=0?direction:1,expanding?1:0);
  return true;
 }
}

package com.kevin.widgetstack;

import android.content.*;
import java.util.*;

/** Opt-in public automation endpoint; positions are one-based. */
public final class TaskerReceiver extends BroadcastReceiver {
 public static final String ACTION="com.kevin.widgetstack.SET_POSITION";
 static boolean enabled(Context c){return c.getSharedPreferences("automation",0).getBoolean("enabled",false);}
 static void enable(Context c,boolean value){c.getSharedPreferences("automation",0).edit().putBoolean("enabled",value).apply();}
 static List<Integer> select(Context c,Intent i){
  List<Integer> changed=new ArrayList<>();
  if(!enabled(c)||!ACTION.equals(i.getAction()))return changed;
  // Tasker's Send Intent fields arrive as strings; also accept typed integer extras.
  int position=number(i,"position"),instance=number(i,"instance_id"),saved=number(i,"stack_id");
  if(position<1||i.hasExtra("instance_id")==i.hasExtra("stack_id"))return changed;
  StackStore store=new StackStore(c);
  List<Integer> targets=i.hasExtra("instance_id")?Collections.singletonList(instance):store.placements(saved);
  for(int id:targets){
   if(id<0||!store.isPlacement(id))continue;
   StackState state=store.get(id);
   if(state==null||position>state.children.size())continue;
   store.selectOnHome(id,state.children.get(position-1));changed.add(id);
  }
  return changed;
 }
 private static int number(Intent i,String key){
  Object value=i.getExtras()==null?null:i.getExtras().get(key);
  try{return Integer.parseInt(String.valueOf(value));}catch(NumberFormatException e){return Integer.MIN_VALUE;}
 }
 @Override public void onReceive(Context c,Intent i){
  for(int id:select(c,i)){
   StackDiagnostics.note(c,"automation selection",id,"position="+number(i,"position"));
   if(Recovery.paused(c))continue;
   StackHost.get(c).sync();StackRenderer.updateSizes(c,id);StackRenderer.update(c,id);
  }
 }
 static String instructions(Context c){
  StringBuilder text=new StringBuilder("Enable Tasker control to allow other apps to select widget positions. No plug-in is required.\n\nTasker → Task → + → System → Send Intent\nAction: "+ACTION+"\nPackage: com.kevin.widgetstack\nClass: com.kevin.widgetstack.TaskerReceiver\nTarget: Broadcast Receiver\nExtra: position:2\nExtra: instance_id:123\n\nReplace 123 with an instance ID below. Alternatively use stack_id instead of instance_id to control all copies of a saved stack. Use exactly one target extra. Positions start at 1 and follow the widget order in Edit. Invalid positions are ignored. Paused stacks retain the selection until resumed. Re-adding a widget gives it a new instance ID.\n");
  StackStore store=new StackStore(c);
  for(StackState s:store.saved())text.append("\n").append(s.name).append(" — stack_id:").append(s.id).append("\nInstance IDs: ").append(store.placements(s.id)).append('\n');
  return text.toString();
 }
}

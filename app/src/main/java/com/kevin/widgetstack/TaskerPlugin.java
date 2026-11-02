package com.kevin.widgetstack;

import android.content.Intent;
import android.os.Bundle;

/** Locale/Tasker action plug-in wire format. Only primitive configuration is persisted. */
final class TaskerPlugin {
 static final String EDIT="com.twofortyfouram.locale.intent.action.EDIT_SETTING";
 static final String FIRE="com.twofortyfouram.locale.intent.action.FIRE_SETTING";
 static final String BUNDLE="com.twofortyfouram.locale.intent.extra.BUNDLE";
 static final String BLURB="com.twofortyfouram.locale.intent.extra.BLURB";
 static Intent command(Intent input){
  if(input==null||!FIRE.equals(input.getAction()))return null;
  Bundle b=input.getBundleExtra(BUNDLE);
  if(b==null||b.getInt("version",0)!=1||b.getInt("position",0)<1)return null;
  boolean instance=b.containsKey("instance_id"),stack=b.containsKey("stack_id");
  if(instance==stack)return null;
  String key=instance?"instance_id":"stack_id";
  return new Intent(TaskerReceiver.ACTION).putExtra("position",b.getInt("position")).putExtra(key,b.getInt(key));
 }
 static Bundle configuration(String key,int id,int position){
  Bundle b=new Bundle();b.putInt("version",1);b.putInt(key,id);b.putInt("position",position);return b;
 }
}

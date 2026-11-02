package com.kevin.widgetstack;
import android.content.*;

public final class TaskerPluginReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent input){
  Intent command=TaskerPlugin.command(input);
  if(command!=null)new TaskerReceiver().onReceive(c,command);
 }
}

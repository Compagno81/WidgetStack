package com.kevin.widgetstack;

import android.content.*;
import android.os.Build;
import org.json.JSONArray;

/** Local, bounded troubleshooting events. No widget contents, app labels or network access. */
final class StackDiagnostics {
    static synchronized void note(Context c,String event,int id,String detail){
        try{android.content.SharedPreferences p=c.getSharedPreferences("troubleshooting",0);JSONArray old=new JSONArray(p.getString("events","[]")),out=new JSONArray();for(int i=Math.max(0,old.length()-19);i<old.length();i++)out.put(old.getString(i));out.put(java.time.Instant.now()+" "+event+" #"+id+" "+detail);p.edit().putString("events",out.toString()).apply();}catch(Exception ignored){}
    }
    static String report(Context c){
        String version="";try{version=c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName;}catch(Exception ignored){}
        StackStore store=new StackStore(c);int copies=0;for(StackState s:store.saved())copies+=store.placements(s.id).size();
        StringBuilder text=new StringBuilder("Widget Stack "+version+"\n"+Build.MANUFACTURER+" "+Build.MODEL+" · Android "+Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")\nSaved stacks: "+store.saved().size()+" · Home-screen copies: "+copies+"\n");
        text.append("Stacks paused: ").append(Recovery.paused(c)).append('\n');
        text.append("Publication path: ").append(WidgetPublisher.usesDirectPublication(Build.VERSION.SDK_INT)?"direct API 35":"Android platform").append('\n');
        for(StackState definition:store.saved())for(int home:store.placements(definition.id)){
            StackState placement=store.get(home);if(placement!=null)text.append("Home #").append(home).append(" selected=").append(placement.selected).append(" widgets=").append(placement.children.size()).append('\n');
        }
        try{JSONArray events=new JSONArray(c.getSharedPreferences("troubleshooting",0).getString("events","[]"));for(int i=0;i<events.length();i++)text.append(events.getString(i)).append('\n');}catch(Exception ignored){}
        return text.toString();
    }
}

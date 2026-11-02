package com.kevin.widgetstack;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.graphics.Rect;
import android.net.Uri;
import android.os.*;
import android.util.SizeF;
import android.widget.RemoteViews;
import java.util.*;

/** Per-placement physical bounds. Launcher option sizes are hints, not layout measurements. */
final class MeasuredViewport {
    static final String ACTION="com.kevin.widgetstack.MEASURE_VIEWPORT";
    private static final String PREFS="measured_viewports",TOKEN="viewport_token",SIGNATURE="viewport_signature";
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    private static String key(int id,String suffix){return id+"_"+suffix;}
    private static String signature(Context c,int id){
        Bundle b=AppWidgetManager.getInstance(c).getAppWidgetOptions(id);
        StringBuilder s=new StringBuilder();
        for(String k:new String[]{AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT})s.append(b.getInt(k,-1)).append(':');
        s.append((Object)b.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES));
        s.append(':').append(c.getResources().getConfiguration().orientation).append(':').append(c.getResources().getDisplayMetrics().densityDpi);
        return s.toString();
    }
    static boolean has(Context c,int id){
        if(id<0)return false;SharedPreferences p=prefs(c);
        return p.getInt(key(id,"width"),0)>0&&p.getInt(key(id,"height"),0)>0&&signature(c,id).equals(p.getString(key(id,"measured_signature"),null));
    }
    static Bundle options(Context c,int id,Bundle reported){
        if(!has(c,id))return reported;
        SharedPreferences p=prefs(c);float density=c.getResources().getDisplayMetrics().density;
        float w=p.getInt(key(id,"width"),0)/density,h=p.getInt(key(id,"height"),0)/density;
        Bundle out=new Bundle(reported);
        for(String k:new String[]{AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH})out.putInt(k,Math.max(1,Math.round(w)));
        for(String k:new String[]{AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT})out.putInt(k,Math.max(1,Math.round(h)));
        ArrayList<SizeF> sizes=new ArrayList<>();sizes.add(new SizeF(w,h));out.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,sizes);
        return out;
    }
    static boolean needed(Context c,StackState s){return s.id>=0&&(s.widthPx(s.selected)>0||s.heightPx(s.selected)>0)&&!has(c,s.id);}
    static RemoteViews probe(Context c,int id){return probe(c,id,null);}
    static RemoteViews probe(Context c,int id,Bundle editor){
        String sig=signature(c,id);SharedPreferences p=prefs(c);int token=p.getInt(key(id,"token"),-1);
        if(token<0||!sig.equals(p.getString(key(id,"pending_signature"),null)))token=StackRenderer.nextRootId(c);
        p.edit().putInt(key(id,"token"),token).putString(key(id,"pending_signature"),sig).commit();
        Intent i=new Intent(c,editor==null?StackControlReceiver.class:ConfigureActivity.class).setAction(ACTION).setData(Uri.parse("widgetstack://measure/"+id+"/"+token)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra(TOKEN,token).putExtra(SIGNATURE,sig);
        // Mutable solely so the launcher can attach the clicked root bounds.
        // The destination is an explicit, non-exported component. Editor taps
        // launch the activity directly, avoiding a background broadcast trampoline.
        if(editor!=null)i.putExtra("resize_draft",editor).putExtra(SavedStacks.EXTRA_SAVED_ID,new StackStore(c).definitionId(id)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent callback=editor==null?PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE):PendingIntent.getActivity(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
        RemoteViews root=new RemoteViews(c.getPackageName(),R.layout.viewport_probe,token);
        root.setContentDescription(R.id.viewport_probe_status,"Tap to fit this stack");
        root.setOnClickPendingIntent(R.id.viewport_probe_status,callback);
        return root;
    }
    private static boolean current(Context c,int id,int token){return prefs(c).getInt(key(id,"token"),-1)==token;}
    static void receive(Context c,Intent i){
        int id=i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1),token=i.getIntExtra(TOKEN,-1);
        boolean own=StackRenderer.owns(c,id),active=current(c,id,token),same=signature(c,id).equals(i.getStringExtra(SIGNATURE));
        Rect received=i.getSourceBounds();
        StackDiagnostics.note(c,"fit tap",id,"owned="+own+" current="+active+" sameSize="+same+" bounds="+(received==null?"missing":received.width()+"x"+received.height()));
        if(!own||!active||!same)return;
        Rect bounds=i.getSourceBounds();
        if(bounds!=null&&bounds.width()>0&&bounds.height()>0&&bounds.width()<=20000&&bounds.height()<=20000){
            prefs(c).edit().putInt(key(id,"width"),bounds.width()).putInt(key(id,"height"),bounds.height()).putString(key(id,"measured_signature"),signature(c,id)).commit();
            stop(c,id);StackDiagnostics.note(c,"viewport measured",id,bounds.width()+"x"+bounds.height());
            if(new StackStore(c).get(id)!=null){HostService.ensure(c);StackHost.get(c).sync();StackRenderer.updateSizes(c,id);StackRenderer.update(c,id);}
        }
    }
    static void stop(Context c,int id){prefs(c).edit().remove(key(id,"token")).remove(key(id,"pending_signature")).commit();}
    static void invalidate(Context c,int id){stop(c,id);prefs(c).edit().remove(key(id,"width")).remove(key(id,"height")).remove(key(id,"measured_signature")).commit();}
    static void invalidateAll(Context c){prefs(c).edit().clear().commit();}
}

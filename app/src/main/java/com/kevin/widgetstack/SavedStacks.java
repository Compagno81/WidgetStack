package com.kevin.widgetstack;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import java.util.*;

/** Operations shared by the saved-stack editor, chooser and pin callback. */
final class SavedStacks {
    static final String EXTRA_SAVED_ID="saved_stack_id";
    static Intent edit(Context c,int saved){return new Intent(c,ConfigureActivity.class).setData(Uri.parse("widgetstack://saved/"+saved)).putExtra(EXTRA_SAVED_ID,saved);}
    static void changed(Context c,int saved){
        StackStore store=new StackStore(c);StackHost.get(c).sync();
        List<Integer> homes=store.placements(saved);Map<Integer,Boolean> motion=new HashMap<>();
        for(int id:homes)motion.put(id,BarController.expand(c,id));
        StackRenderer.updateSizes(c,saved);
        for(int id:homes)StackRenderer.updateBarMotion(c,id,0,motion.get(id)?1:0);
        HostService.ensure(c);
    }
    static boolean place(Context c,int home,int saved){
        if(!StackRenderer.owns(c,home))return false;StackStore store=new StackStore(c);
        int old=store.definitionId(home);if(!store.assign(home,saved))return false;
        BarController.cancel(home);StackHost.get(c).sync();StackRenderer.updateSizes(c,home);StackRenderer.update(c,home);
        if(old!=home&&old!=saved)StackRenderer.updateSizes(c,old);HostService.ensure(c);return true;
    }
    static PendingIntent pinCallback(Context c,int saved){
        Intent i=new Intent(c,StackControlReceiver.class).setAction(StackProvider.PINNED).setData(Uri.parse("widgetstack://pin/"+saved)).putExtra(EXTRA_SAVED_ID,saved);
        // The launcher fills in the new home-screen ID. The component is explicit.
        return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
    }
    static void recoverControls(Context c,int home){
        if(!StackRenderer.owns(c,home)||new StackStore(c).get(home)==null)return;
        BarController.cancel(home);BarController.expand(c,home);
        StackHost.get(c).sync();StackRenderer.updateSizes(c,home);
        StackRenderer.updateBarMotion(c,home,0,0);HostService.ensure(c);
    }
    static boolean delete(Context c,int saved){
        StackStore store=new StackStore(c);StackState state=store.get(saved);
        if(!store.isSaved(saved)||state==null)return false;
        List<Integer> homes=store.placements(saved);if(!store.deleteSaved(saved))return false;
        for(int home:homes)BarController.cancel(home);
        Set<Integer> retained=new HashSet<>();for(StackState other:store.all())retained.addAll(other.children);
        StackHost host=StackHost.get(c);host.sync();
        for(int child:state.children)if(!retained.contains(child))host.deleteAppWidgetId(child);
        for(int home:homes)StackRenderer.update(c,home);
        if(store.all().isEmpty())c.stopService(new Intent(c,HostService.class));
        return true;
    }
    static void reconcile(Context c){
        StackStore store=new StackStore(c);store.migrateLegacy();Set<Integer> active=new HashSet<>();
        for(int id:AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,StackProvider.class)))active.add(id);
        store.prunePlacements(active);
    }
}

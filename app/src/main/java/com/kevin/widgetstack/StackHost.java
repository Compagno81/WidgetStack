package com.kevin.widgetstack;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class StackHost extends AppWidgetHost {
    static final int HOST_ID = 4107;
    private static StackHost instance;
    final Context context;
    final Map<Integer, RemoteViews> views = new HashMap<>();
    final Set<Integer> tracked = new HashSet<>();
    final Handler handler = new Handler(Looper.getMainLooper());
    boolean listening;
    final Set<Runnable> previewListeners=new HashSet<>();
    final Set<Integer> queued = new HashSet<>();
    static synchronized StackHost get(Context c) {
        if(instance==null) instance=new StackHost(c.getApplicationContext()); return instance;
    }
    private StackHost(Context c) { super(c, HOST_ID);context=c; }
    void sync() {
        if(Recovery.paused(context))return;
        Set<Integer> wanted=new HashSet<>();
        for(StackState s:new StackStore(context).all()) wanted.addAll(s.children);
        for(int id:new HashSet<>(tracked)) if(!wanted.contains(id)) { tracked.remove(id);views.remove(id); }
        for(int id:wanted) if(!tracked.contains(id)) track(id);
        if(!listening) try { startListening();listening=true; } catch(RuntimeException e) { Log.e("WidgetStack","Host start failed",e); }
    }
    void track(int id) {
        if(Recovery.paused(context))return;
        AppWidgetProviderInfo info=AppWidgetManager.getInstance(context).getAppWidgetInfo(id);
        if(info==null) return;
        tracked.add(id);
        try {
            Class<?> listener=Class.forName("android.appwidget.AppWidgetHost$AppWidgetHostListener");
            Object proxy=Proxy.newProxyInstance(listener.getClassLoader(),new Class<?>[]{listener},(p,m,a)->{
                try { switch(m.getName()) {
                    case "updateAppWidget": capture(id,(RemoteViews)a[0]);break;
                    case "onViewDataChanged":
                        collectionChanged(id,(Integer)a[0],"provider data"); notifyPreviews(); break;
                    case "onUpdateProviderInfo": tracked.remove(id);views.remove(id); refresh(id);break;
                    case "updateAppWidgetDeferred": refresh(id);break;
                    case "toString": return "StackListener("+id+")";
                    case "hashCode": return System.identityHashCode(p);
                    case "equals": return p==a[0];
                    default: break;
                }
                }catch(RuntimeException|LinkageError|StackOverflowError e){Log.e("WidgetStack","Host callback failed",e);}
                return null;
            });
            Method m=AppWidgetHost.class.getDeclaredMethod("setListener",int.class,listener);
            m.setAccessible(true);m.invoke(this,id,proxy);
        } catch(Exception e) {
            Log.w("WidgetStack","Using host view compatibility fallback",e);
            createView(context,id,info);
        }
    }
    private void refresh(int id) {
        // createView retrieves the current system snapshot, then restore our data-change listener.
        AppWidgetProviderInfo info=AppWidgetManager.getInstance(context).getAppWidgetInfo(id);
        if(info!=null) { createView(context,id,info);handler.post(()->track(id)); }
    }
    private void capture(int id,RemoteViews rv) {
        if(Recovery.paused(context))return;
        try{if(rv!=null) views.put(id,new RemoteViews(rv)); else views.remove(id);}
        catch(RuntimeException|LinkageError|StackOverflowError e){views.remove(id);Log.e("WidgetStack","Invalid widget update",e);}
        StackDiagnostics.note(context,"provider layout received",id,rv==null?"empty":"snapshot");
        for(StackState s:new StackStore(context).all()) if(s.id>=0&&s.selected==id) schedule(s.id);
        notifyPreviews();
    }
    private void notifyPreviews(){handler.post(()->{for(Runnable observer:new HashSet<>(previewListeners))observer.run();});}
    void collectionChanged(int child,int view,String reason){
        if(Recovery.paused(context))return;
        if(child<0||view==0)return;
        for(StackState s:new StackStore(context).all())if(s.id>=0&&s.selected==child&&StackRenderer.owns(context,s.id)){
            AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(s.id,view);
            StackDiagnostics.note(context,"collection rows requested",s.id,"child="+child+" view="+view+" reason="+reason);
        }
        // Data readiness must not rebuild the root: replacing an adapter can
        // start another readiness event and perpetually discard incoming updates.
    }
    void schedule(int stack) {
        if(Recovery.paused(context))return;
        if(!queued.add(stack)) return;
        handler.postDelayed(()->{queued.remove(stack);StackRenderer.update(context,stack);},120);
    }
    @Override protected AppWidgetHostView onCreateView(Context c,int id,AppWidgetProviderInfo info) {
        return new AppWidgetHostView(c) { @Override public void updateAppWidget(RemoteViews rv) { capture(id,rv); } };
    }
    @Override public void onAppWidgetRemoved(int id) {
        views.remove(id);tracked.remove(id);
        StackStore store=new StackStore(context);
        for(StackState s:store.all()) if(s.children.contains(id)) { s.remove(id);store.put(s);if(s.id>=0)schedule(s.id); }
    }
    @Override public void deleteAppWidgetId(int id) { super.deleteAppWidgetId(id);tracked.remove(id);views.remove(id); }
    void stop() { try { stopListening(); }catch(RuntimeException ignored){}listening=false; }
}

package com.kevin.widgetstack;

import android.appwidget.AppWidgetManager;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/** Routes legacy collection services using the child widget's real host identity.
 * Concept adapted from Widgets Anywhere's MIT RemoteViewsProxyService. */
public final class CollectionProxyService extends RemoteViewsService {
    static final String READY="com.kevin.widgetstack.COLLECTION_READY";
    private HandlerThread connectionThread;
    private Handler connectionHandler;
    @Override public void onCreate(){
        super.onCreate();connectionThread=new HandlerThread("WidgetCollectionConnections");connectionThread.start();connectionHandler=new Handler(connectionThread.getLooper());
    }
    @Override public void onDestroy(){super.onDestroy();if(connectionThread!=null)connectionThread.quitSafely();}
    static Intent providerIntent(int child,Intent original){
        // Captured RemoteViews have not been applied in their original host.
        // Supply the ID that RemoteViews would normally add during adapter binding.
        return original==null?null:new Intent(original).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,child);
    }
    static Intent intent(Context c,int child,Intent original,int viewId) {
        return new Intent(c,CollectionProxyService.class)
            .putExtra("child",child).putExtra("original",providerIntent(child,original)).putExtra("view_id",viewId)
            .setData(new Uri.Builder().scheme("widgetstack").authority("collection")
                .appendPath(Integer.toString(child)).appendPath(Integer.toString(viewId))
                .appendPath(original.toUri(0)).build());
    }
    @Override public RemoteViewsFactory onGetViewFactory(Intent i) {
        return new Factory(i.getIntExtra("child",-1),i.getIntExtra("view_id",0),i.getParcelableExtra("original"));
    }
    private final class Factory implements RemoteViewsFactory {
        final int child,viewId;final Intent original;
        volatile Object remote;
        volatile Class<?> factoryInterface;
        volatile boolean bound,servedEmpty;
        final AtomicBoolean refreshPosted=new AtomicBoolean();
        final Handler handler=connectionHandler;
        final ServiceConnection connection=new ServiceConnection(){
            @Override public void onServiceConnected(ComponentName n,IBinder binder) {
                try {
                    Class<?> stub=Class.forName("com.android.internal.widget.IRemoteViewsFactory$Stub");
                    factoryInterface=Class.forName("com.android.internal.widget.IRemoteViewsFactory");
                    remote=stub.getMethod("asInterface",IBinder.class).invoke(null,binder);
                }catch(Exception e){Log.e("WidgetStack","Collection connection failed",e);}
                refreshIfNeeded();
            }
            @Override public void onServiceDisconnected(ComponentName n){remote=null;refreshPosted.set(false);}
            @Override public void onNullBinding(ComponentName n){}
        };
        Factory(int id,int view,Intent i){child=id;viewId=view;original=providerIntent(id,i);}
        @Override public void onCreate() {
            if(bound)return;
            if(original==null||child<0){return;}
            try {
                Method dispatcher=null;
                for(Method m:Context.class.getDeclaredMethods())if(m.getName().equals("getServiceDispatcher")&&m.getParameterCount()==3&&m.getParameterTypes()[0]==ServiceConnection.class&&m.getParameterTypes()[1]==Handler.class){dispatcher=m;break;}
                if(dispatcher==null)throw new NoSuchMethodException("getServiceDispatcher");
                dispatcher.setAccessible(true);
                Object flags=dispatcher.getParameterTypes()[2]==long.class?(Object)Long.valueOf(0):Integer.valueOf(0);
                Object conn=dispatcher.invoke(CollectionProxyService.this,connection,handler,flags);
                Method bind=null;
                for(Method m:AppWidgetManager.class.getDeclaredMethods())if(m.getName().equals("bindRemoteViewsService")&&m.getParameterCount()==5&&m.getParameterTypes()[0]==Context.class&&m.getParameterTypes()[1]==int.class&&m.getParameterTypes()[2]==Intent.class){bind=m;break;}
                if(bind==null)throw new NoSuchMethodException("bindRemoteViewsService");
                bind.setAccessible(true);
                Object bindFlags=bind.getParameterTypes()[4]==long.class?(Object)Long.valueOf(Context.BIND_AUTO_CREATE):Integer.valueOf(Context.BIND_AUTO_CREATE);
                bound=Boolean.TRUE.equals(bind.invoke(AppWidgetManager.getInstance(CollectionProxyService.this),CollectionProxyService.this,child,original,conn,bindFlags));
            }catch(Exception e){Log.e("WidgetStack","Collection binding failed",e);}
        }
        private void refreshIfNeeded(){
            if(!servedEmpty||remote==null||!refreshPosted.compareAndSet(false,true))return;
            handler.postDelayed(()->{
                // The collection process has no host snapshots and must never
                // render a stack or read cached cross-process preferences.
                sendBroadcast(new Intent(CollectionProxyService.this,StackControlReceiver.class)
                    .setAction(READY).putExtra("child",child).putExtra("view_id",viewId));
            },100);
        }
        private Object call(String method,Class<?>[] types,Object...args) {
            // Never wait for a service connection in a metadata request.
            // Serve a temporary empty collection and notify the host when ready.
            Object current=remote;if(current==null)return null;
            try{return factoryInterface.getMethod(method,types).invoke(current,args);}
            catch(Exception e){Log.w("WidgetStack","Collection call failed: "+method,e);return null;}
        }
        private Object call(String name){return call(name,new Class<?>[0]);}
        @Override public void onDataSetChanged(){call("onDataSetChanged");}
        @Override public int getCount(){Object v=call("getCount");if(v==null){servedEmpty=true;refreshIfNeeded();return 0;}return(Integer)v;}
        @Override public RemoteViews getViewAt(int position){return (RemoteViews)call("getViewAt",new Class<?>[]{int.class},position);}
        @Override public RemoteViews getLoadingView(){return (RemoteViews)call("getLoadingView");}
        @Override public int getViewTypeCount(){Object v=call("getViewTypeCount");return v==null?1:Math.max(1,(Integer)v);}
        @Override public long getItemId(int position){Object v=call("getItemId",new Class<?>[]{int.class},position);return v==null?position:(Long)v;}
        @Override public boolean hasStableIds(){return Boolean.TRUE.equals(call("hasStableIds"));}
        @Override public void onDestroy(){if(bound)try{unbindService(connection);}catch(IllegalArgumentException ignored){}bound=false;remote=null;}
    }
}

package com.kevin.widgetstack;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.HashMap;
import java.util.Map;

/** Main-thread timers only. Frames run inside the launcher's ViewFlipper, without polling. */
final class BarController {
    static final long ANIMATION_MS=240;
    private static final Handler handler=new Handler(Looper.getMainLooper());
    private static final Map<Integer,Expansion> expanded=new HashMap<>();
    private static final class Expansion implements Runnable {
        final Context context;final int id;final long until;
        long animationEnd;boolean collapsing;
        final Runnable settle;
        Expansion(Context c,StackState s,boolean animate){
            context=c.getApplicationContext();id=s.id;until=SystemClock.uptimeMillis()+s.contractDelayMillis();
            animationEnd=animate?SystemClock.uptimeMillis()+ANIMATION_MS:0;
            settle=()->{if(expanded.get(id)!=this)return;if(collapsing)expanded.remove(id);else animationEnd=0;refresh();};
        }
        void refresh(){if(StackRenderer.owns(context,id)){StackRenderer.updateSizes(context,id);StackRenderer.update(context,id);}}
        @Override public void run(){
            if(expanded.get(id)!=this)return;
            collapsing=true;animationEnd=SystemClock.uptimeMillis()+ANIMATION_MS;
            StackRenderer.updateBarMotion(context,id,0,-1);
            handler.postDelayed(settle,ANIMATION_MS);
        }
    }
    static boolean contracted(StackState s){
        Expansion e=expanded.get(s.id);
        return s.showRail&&s.autoContract&&(e==null||(e.collapsing&&SystemClock.uptimeMillis()>=e.animationEnd));
    }
    static boolean animating(int id){Expansion e=expanded.get(id);return e!=null&&SystemClock.uptimeMillis()<e.animationEnd;}
    static int width(StackState s){return contracted(s)?s.contractedWidth:s.railWidth;}
    /** Returns whether this tap expands a contracted or currently contracting strip. */
    static boolean expand(Context c,int id){
        StackState s=new StackStore(c).get(id);Expansion old=expanded.get(id);
        boolean animate=s!=null&&(contracted(s)||(old!=null&&old.collapsing));
        cancel(id);
        if(s==null||!s.showRail||!s.autoContract)return false;
        Expansion e=new Expansion(c,s,animate);expanded.put(id,e);handler.postDelayed(e,s.contractDelayMillis());
        if(animate)handler.postDelayed(e.settle,ANIMATION_MS);
        return animate;
    }
    static void cancel(int id){Expansion old=expanded.remove(id);if(old!=null){handler.removeCallbacks(old);handler.removeCallbacks(old.settle);}}
}

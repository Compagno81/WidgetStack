package com.kevin.widgetstack;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;

/** A provider callback can arrive while the launcher is drawing the newly selected page. */
final class PageSlide {
    private static final Handler handler=new Handler(Looper.getMainLooper());
    private static final SparseArray<PageSlide> active=new SparseArray<>();
    private boolean refreshNeeded;
    private final Runnable finish;

    private PageSlide(Context c,int id){
        finish=()->{
            active.remove(id);
            if(refreshNeeded)StackRenderer.update(c,id);
        };
    }

    static int layout(StackState s,int direction){
        if(direction==0)return 0;
        boolean horizontal=s.pairedBars?!s.topBottomPair:s.horizontalRail();
        if(horizontal)return direction>0?R.layout.slide_page_right:R.layout.slide_page_left;
        return direction>0?R.layout.slide_page_bottom:R.layout.slide_page_top;
    }

    static void started(Context c,int id){
        PageSlide old=active.get(id);
        if(old!=null)handler.removeCallbacks(old.finish);
        PageSlide slide=new PageSlide(c.getApplicationContext(),id);
        active.put(id,slide);
        // Allow for launcher delivery plus the 280 ms layout animation.
        handler.postDelayed(slide.finish,420);
    }

    static boolean deferRefresh(int id){
        PageSlide slide=active.get(id);
        if(slide==null)return false;
        slide.refreshNeeded=true;
        return true;
    }
}

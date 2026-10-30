package com.kevin.widgetstack;

import android.appwidget.AppWidgetManager;
import android.os.Bundle;
import android.util.SizeF;
import java.util.ArrayList;

/** Stored pixels are physical display pixels; provider options and layout use dp. */
final class WidgetSizing {
    static float fit(float width,float height,float logicalWidth,float logicalHeight) {
        if(width<=0||height<=0||logicalWidth<=0||logicalHeight<=0)throw new IllegalArgumentException("Dimensions must be positive");
        return Math.min(width/logicalWidth,height/logicalHeight);
    }
    /** Measure the provider root as FrameLayout will measure it in the launcher.
     * This honors fixed/wrap-content roots without inspecting or cropping their
     * descendants. MATCH_PARENT roots keep the full requested logical size.
     */
    static SizeF rootSize(android.content.Context context,android.widget.RemoteViews provider,SizeF requested) {
        float density=context.getResources().getDisplayMetrics().density;
        try {
            android.widget.FrameLayout parent=new android.widget.FrameLayout(context);
            android.view.View root=provider.apply(context,parent);
            parent.addView(root);
            parent.measure(android.view.View.MeasureSpec.makeMeasureSpec(Math.max(1,Math.round(requested.getWidth()*density)),android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(Math.max(1,Math.round(requested.getHeight()*density)),android.view.View.MeasureSpec.EXACTLY));
            int width=root.getMeasuredWidth(),height=root.getMeasuredHeight();
            if(width>0&&height>0)return new SizeF(width/density,height/density);
        }catch(RuntimeException|LinkageError e){
            // Some providers depend on a real host or attached collection view.
            // Preserve the established requested-size path in that case.
            android.util.Log.w("WidgetStack","Provider root measurement unavailable; using requested size",e);
        }
        return requested;
    }
    static Bundle options(Bundle actual,StackState state,int child,float density) {
        Bundle result=StackRenderer.logicalOptions(actual,state.logicalSizePercent(child));
        int width=state.widthPx(child),height=state.heightPx(child);
        if(width>0)for(String key:new String[]{AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH})result.putInt(key,Math.max(1,Math.round(width/density)));
        if(height>0)for(String key:new String[]{AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT})result.putInt(key,Math.max(1,Math.round(height/density)));
        ArrayList<SizeF> sizes=result.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
        if(sizes!=null){ArrayList<SizeF> requested=new ArrayList<>();for(SizeF size:sizes){SizeF next=new SizeF(width>0?Math.max(1,Math.round(width/density)):size.getWidth(),height>0?Math.max(1,Math.round(height/density)):size.getHeight());if(!requested.contains(next))requested.add(next);}result.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,requested);}
        return result;
    }
}

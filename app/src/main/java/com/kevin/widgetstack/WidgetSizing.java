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

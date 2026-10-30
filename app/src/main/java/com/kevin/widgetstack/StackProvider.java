package com.kevin.widgetstack;

import android.appwidget.*;
import android.content.*;
import android.os.Bundle;

public final class StackProvider extends AppWidgetProvider {
    static final String PINNED="com.kevin.widgetstack.PINNED";
    static final String EXPAND="com.kevin.widgetstack.EXPAND";
    static final String SELECT="com.kevin.widgetstack.SELECT";
    @Override public void onReceive(Context c,Intent i){
        // Older launcher publications still target this provider.
        if(StackControlReceiver.handle(c,i))return;
        super.onReceive(c,i);
    }
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){
        new StackStore(c).migrateLegacy();
        StackHost.get(c).sync();for(int id:ids){StackRenderer.updateSizes(c,id);StackRenderer.update(c,id);}HostService.ensure(c);
    }
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){StackRenderer.updateSizes(c,id);StackHost.get(c).sync();StackRenderer.update(c,id);}
    @Override public void onDeleted(Context c,int[] ids){
        StackStore store=new StackStore(c);store.migrateLegacy();
        for(int id:ids){BarController.cancel(id);int saved=store.definitionId(id);store.delete(id);if(saved!=id)StackRenderer.updateSizes(c,saved);}
        if(store.all().isEmpty())c.stopService(new Intent(c,HostService.class));
    }
}

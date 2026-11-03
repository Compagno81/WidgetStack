package com.kevin.widgetstack;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SizeF;
import android.view.View;
import android.widget.RemoteViews;
import java.util.ArrayList;

final class StackRenderer {
    static final int RAIL_DP=36;
    static boolean owns(Context c,int id){
        if(id<0)return false;
        AppWidgetManager manager=AppWidgetManager.getInstance(c);ComponentName provider=new ComponentName(c,StackProvider.class);
        try{AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);if(info!=null)return provider.equals(info.provider);}catch(RuntimeException ignored){}
        try{for(int own:manager.getAppWidgetIds(provider))if(own==id)return true;}catch(RuntimeException ignored){}
        return false;
    }
    static Intent editIntent(Context c,int id){
        StackStore store=new StackStore(c);int saved=store.definitionId(id);
        if(store.isSaved(saved))return SavedStacks.edit(c,saved).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
        Class<?> screen=store.get(id)==null?StackChooserActivity.class:ConfigureActivity.class;
        return new Intent(c,screen).setData(Uri.parse("widgetstack://edit/"+id)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
    }
    static Bundle optionsFor(Context c,int id){
        AppWidgetManager m=AppWidgetManager.getInstance(c);if(id>=0)return MeasuredViewport.options(c,id,m.getAppWidgetOptions(id));
        Bundle best=null;long area=-1;
        for(int home:new StackStore(c).placements(id))if(owns(c,home)){
            Bundle b=MeasuredViewport.options(c,home,m.getAppWidgetOptions(home));long candidate=(long)b.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,240)*b.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,160);
            if(candidate>area){best=b;area=candidate;}
        }
        if(best!=null)return best;
        Bundle b=new Bundle();b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,240);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,240);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,160);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,160);return b;
    }
    /** One provider binding can serve several placements; publish all their supported sizes. */
    static Bundle hostedOptions(Context c,int id){
        StackStore store=new StackStore(c);int saved=store.definitionId(id);StackState definition=store.get(saved);
        if(definition==null)return new Bundle();java.util.List<Bundle> candidates=new ArrayList<>();
        if(store.isSaved(saved))for(int home:store.placements(saved))if(owns(c,home))candidates.add(childOptions(optionsFor(c,home),store.get(home)));
        if(candidates.isEmpty())candidates.add(childOptions(optionsFor(c,id),definition));
        Bundle out=new Bundle(candidates.get(0));ArrayList<SizeF> sizes=new ArrayList<>();
        for(Bundle b:candidates){
            for(String key:new String[]{AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT})out.putInt(key,Math.min(out.getInt(key),b.getInt(key)));
            for(String key:new String[]{AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT})out.putInt(key,Math.max(out.getInt(key),b.getInt(key)));
            ArrayList<SizeF> own=b.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
            if(own==null){own=new ArrayList<>();own.add(new SizeF(b.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),b.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)));own.add(new SizeF(b.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH),b.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)));}
            for(SizeF size:own)if(!sizes.contains(size))sizes.add(size);
        }
        out.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,sizes);return out;
    }

    static Bundle childOptions(Bundle outer, boolean showRail) {
        StackState s=new StackState(-1);s.showRail=showRail;return childOptions(outer,s);
    }
    static Bundle childOptions(Bundle outer,StackState s) {return childOptions(outer,s,null);}
    static Bundle childOptions(Bundle outer,StackState s,Boolean forceContracted) {
        Bundle b=new Bundle(outer);s.normalize();
        b.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
        int width=forceContracted==null?BarController.width(s):forceContracted?s.contractedWidth:s.railWidth;
        boolean compact=forceContracted==null?BarController.contracted(s):forceContracted;
        boolean reserve=s.showRail&&!s.cornerArrows&&!s.overlapBar&&!(compact&&s.contractToCorner);
        int w=reserve&&!s.horizontalRail()?width:0,h=reserve&&s.horizontalRail()?width:0;
        for(String k:new String[]{AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH})b.putInt(k,Math.max(32,outer.getInt(k,240)-w));
        for(String k:new String[]{AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT})b.putInt(k,Math.max(32,outer.getInt(k,160)-h));
        ArrayList<SizeF> sizes=outer.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
        if(sizes!=null){ArrayList<SizeF> out=new ArrayList<>();for(SizeF size:sizes)out.add(new SizeF(Math.max(32,size.getWidth()-w),Math.max(32,size.getHeight()-h)));b.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,out);}
        return b;
    }
    /** Ask the provider to choose a roomier responsive layout without changing the launcher slot. */
    static Bundle logicalOptions(Bundle actual,int percent) {
        Bundle b=new Bundle(actual);float factor=Math.max(100,Math.min(150,percent))/100f;
        for(String key:new String[]{AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT})
            if(b.containsKey(key))b.putInt(key,Math.max(1,Math.round(b.getInt(key)*factor)));
        ArrayList<SizeF> sizes=actual.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
        if(sizes!=null){ArrayList<SizeF> scaled=new ArrayList<>();for(SizeF size:sizes)scaled.add(new SizeF(size.getWidth()*factor,size.getHeight()*factor));b.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,scaled);}
        return b;
    }
    static SizeF displaySize(Bundle options,boolean landscape){
        return new SizeF(Math.max(1,options.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH:AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,240)),Math.max(1,options.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT:AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,160)));
    }

    static void updateSizes(Context c,int stack) {
        if(Recovery.paused(c))return;
        StackState s=new StackStore(c).get(stack);if(s==null)return;
        AppWidgetManager m=AppWidgetManager.getInstance(c);
        for(int child:s.children) {
            Bundle b=WidgetSizing.options(hostedOptions(c,stack),s,child,c.getResources().getDisplayMetrics().density);
            Bundle old=m.getAppWidgetOptions(child);
            boolean same=true;
            for(String k:new String[]{AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,AppWidgetManager.OPTION_APPWIDGET_SIZES})
                if(!java.util.Objects.equals(old.get(k),b.get(k)))same=false;
            if(!same)try{m.updateAppWidgetOptions(child,b);}catch(RuntimeException e){Log.w("WidgetStack","Widget size update failed",e);}
        }
    }
    static RemoteViews shell(Context c,StackState s){return shell(c,s,0);}
    static RemoteViews shell(Context c,StackState s,int direction){return shell(c,s,direction,null);}
    static RemoteViews shell(Context c,StackState s,int direction,Boolean forceContracted){
        return shell(c,s,direction,forceContracted,0);
    }
    static RemoteViews shell(Context c,StackState s,int direction,Boolean forceContracted,int barMotion){return shell(c,s,direction,forceContracted,barMotion,View.NO_ID);}
    private static RemoteViews shell(Context c,StackState s,int direction,Boolean forceContracted,int barMotion,int rootId){
        s.normalize();
        if(s.cornerArrows&&s.showRail){
            int[] corners={R.layout.corner_arrows_0,R.layout.corner_arrows_1,R.layout.corner_arrows_2,R.layout.corner_arrows_3};
            RemoteViews root=baseShell(c,corners[s.railEdge],rootId);
            for(int button:new int[]{R.id.previous,R.id.next}){
                root.setTextViewText(button,button==R.id.previous?(s.horizontalRail()?"←":"↑"):(s.horizontalRail()?"→":"↓"));
                root.setContentDescription(button,button==R.id.previous?"Previous widget":"Next widget");
                root.setTextViewTextSize(button,android.util.TypedValue.COMPLEX_UNIT_DIP,s.cornerArrowSize*.7f);
                root.setViewLayoutWidth(button,Math.max(48,s.cornerArrowSize),android.util.TypedValue.COMPLEX_UNIT_DIP);
                root.setViewLayoutHeight(button,Math.max(48,s.cornerArrowSize),android.util.TypedValue.COMPLEX_UNIT_DIP);
                root.setFloat(button,"setAlpha",s.railOpacity/100f);
                root.setBoolean(button,"setEnabled",s.children.size()>1);
                root.setOnClickPendingIntent(button,navigation(c,s.id,-1,button==R.id.previous?-1:1));
            }
            root.setOnClickPendingIntent(R.id.empty,PendingIntent.getActivity(c,s.id,editIntent(c,s.id),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
            return root;
        }
        int[][] layouts={{R.layout.stack_top,R.layout.stack_top_next,R.layout.stack_top_previous},{R.layout.stack_bottom,R.layout.stack_bottom_next,R.layout.stack_bottom_previous},{R.layout.stack_left,R.layout.stack_left_next,R.layout.stack_left_previous},{R.layout.stack,R.layout.stack_next,R.layout.stack_previous}};
        if(s.overlapBar)layouts=new int[][]{{R.layout.stack_top_overlap,R.layout.stack_top_next_overlap,R.layout.stack_top_previous_overlap},{R.layout.stack_bottom_overlap,R.layout.stack_bottom_next_overlap,R.layout.stack_bottom_previous_overlap},{R.layout.stack_left_overlap,R.layout.stack_left_next_overlap,R.layout.stack_left_previous_overlap},{R.layout.stack_overlap,R.layout.stack_next_overlap,R.layout.stack_previous_overlap}};
        if(barMotion==0&&(forceContracted!=null?forceContracted:BarController.contracted(s)))return contractedShell(c,s,rootId);
        boolean animate=s.animateSwitch&&direction!=0;
        int[][] barLayouts={{R.layout.stack_top_bar_expand,R.layout.stack_bottom_bar_expand,R.layout.stack_left_bar_expand,R.layout.stack_bar_expand},{R.layout.stack_top_overlap_bar_expand,R.layout.stack_bottom_overlap_bar_expand,R.layout.stack_left_overlap_bar_expand,R.layout.stack_overlap_bar_expand},{R.layout.stack_top_bar_contract,R.layout.stack_bottom_bar_contract,R.layout.stack_left_bar_contract,R.layout.stack_bar_contract},{R.layout.stack_top_overlap_bar_contract,R.layout.stack_bottom_overlap_bar_contract,R.layout.stack_left_overlap_bar_contract,R.layout.stack_overlap_bar_contract}};
        if(s.contractToCorner){int[][] cornerLayouts={{R.layout.stack_top_bar_expand_corner_0,R.layout.stack_bottom_bar_expand_corner_0,R.layout.stack_left_bar_expand_corner_0,R.layout.stack_bar_expand_corner_0},{R.layout.stack_top_overlap_bar_expand_corner_0,R.layout.stack_bottom_overlap_bar_expand_corner_0,R.layout.stack_left_overlap_bar_expand_corner_0,R.layout.stack_overlap_bar_expand_corner_0},{R.layout.stack_top_bar_contract_corner_0,R.layout.stack_bottom_bar_contract_corner_0,R.layout.stack_left_bar_contract_corner_0,R.layout.stack_bar_contract_corner_0},{R.layout.stack_top_overlap_bar_contract_corner_0,R.layout.stack_bottom_overlap_bar_contract_corner_0,R.layout.stack_left_overlap_bar_contract_corner_0,R.layout.stack_overlap_bar_contract_corner_0},{R.layout.stack_top_bar_expand_corner_1,R.layout.stack_bottom_bar_expand_corner_1,R.layout.stack_left_bar_expand_corner_1,R.layout.stack_bar_expand_corner_1},{R.layout.stack_top_overlap_bar_expand_corner_1,R.layout.stack_bottom_overlap_bar_expand_corner_1,R.layout.stack_left_overlap_bar_expand_corner_1,R.layout.stack_overlap_bar_expand_corner_1},{R.layout.stack_top_bar_contract_corner_1,R.layout.stack_bottom_bar_contract_corner_1,R.layout.stack_left_bar_contract_corner_1,R.layout.stack_bar_contract_corner_1},{R.layout.stack_top_overlap_bar_contract_corner_1,R.layout.stack_bottom_overlap_bar_contract_corner_1,R.layout.stack_left_overlap_bar_contract_corner_1,R.layout.stack_overlap_bar_contract_corner_1}};barLayouts=new int[][]{cornerLayouts[s.cornerEnd*4],cornerLayouts[s.cornerEnd*4+1],cornerLayouts[s.cornerEnd*4+2],cornerLayouts[s.cornerEnd*4+3]};}
        boolean moving=barMotion!=0&&s.showRail&&s.autoContract;
        RemoteViews root=baseShell(c,moving?barLayouts[(barMotion<0?2:0)+(s.overlapBar?1:0)][s.railEdge]:layouts[s.railEdge][animate?(direction<0?2:1):0],rootId);
        if(moving){
            int unit=android.util.TypedValue.COMPLEX_UNIT_DIP;
            if(s.horizontalRail()){root.setViewLayoutHeight(R.id.rail_frame,s.railWidth,unit);root.setViewLayoutHeight(R.id.compact_rail,s.contractedWidth,unit);}
            else{root.setViewLayoutWidth(R.id.rail_frame,s.railWidth,unit);root.setViewLayoutWidth(R.id.compact_rail,s.contractedWidth,unit);}
            if(s.contractToCorner){root.setViewLayoutWidth(R.id.compact_rail,s.contractedWidth,unit);root.setViewLayoutHeight(R.id.compact_rail,s.contractedWidth,unit);}
            if(s.contractToCorner){
                cornerMark(root,s);float inset=(Math.max(48,s.contractedWidth)-s.contractedWidth)/2f;
                for(int margin:new int[]{RemoteViews.MARGIN_LEFT,RemoteViews.MARGIN_TOP,RemoteViews.MARGIN_RIGHT,RemoteViews.MARGIN_BOTTOM})root.setViewLayoutMargin(R.id.compact_rail,margin,inset,unit);
            }
            root.setFloat(R.id.rail_frame,"setAlpha",s.railOpacity/100f);
            root.setOnClickPendingIntent(R.id.compact_rail,expandIntent(c,s.id));
            root.setDisplayedChild(R.id.bar_transition,1);
        }
        if(animate&&!moving)root.setDisplayedChild(R.id.transition,1);
        root.setViewVisibility(R.id.rail,s.showRail?View.VISIBLE:View.GONE);
        root.setFloat(R.id.rail,"setAlpha",moving?1f:s.railOpacity/100f);
        root.setViewVisibility(R.id.rail_middle,s.buttonsOnly?View.GONE:View.VISIBLE);
        float iconSize=Math.min(24,s.railWidth*.7f);
        for(int button:new int[]{R.id.previous,R.id.next,R.id.edit})root.setTextViewTextSize(button,android.util.TypedValue.COMPLEX_UNIT_DIP,iconSize);
        if(!moving){if(s.horizontalRail())root.setViewLayoutHeight(R.id.rail,s.railWidth,android.util.TypedValue.COMPLEX_UNIT_DIP);
        else root.setViewLayoutWidth(R.id.rail,s.railWidth,android.util.TypedValue.COMPLEX_UNIT_DIP);}
        PendingIntent edit=PendingIntent.getActivity(c,s.id,editIntent(c,s.id),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        root.setOnClickPendingIntent(R.id.edit,edit);root.setOnClickPendingIntent(R.id.empty,edit);
        root.removeAllViews(R.id.indicators);
        for(int child:s.buttonsOnly?java.util.Collections.<Integer>emptyList():s.children){
            RemoteViews dot=new RemoteViews(c.getPackageName(),s.horizontalRail()?R.layout.indicator_horizontal:R.layout.indicator);
            dot.setTextViewText(R.id.dot,child==s.selected?"●":"○");
            dot.setTextViewTextSize(R.id.dot,android.util.TypedValue.COMPLEX_UNIT_DIP,Math.min(16,s.railWidth*.7f));
            dot.setContentDescription(R.id.dot,"Widget "+(s.children.indexOf(child)+1)+(child==s.selected?", selected":""));
            dot.setOnClickPendingIntent(R.id.dot,navigation(c,s.id,child,0));root.addView(R.id.indicators,dot);
        }
        root.setTextViewText(R.id.previous,s.horizontalRail()?"‹":"↑");root.setTextViewText(R.id.next,s.horizontalRail()?"›":"↓");
        root.setOnClickPendingIntent(R.id.previous,navigation(c,s.id,-1,-1));root.setOnClickPendingIntent(R.id.next,navigation(c,s.id,-1,1));
        root.setBoolean(R.id.previous,"setEnabled",s.children.size()>1);root.setBoolean(R.id.next,"setEnabled",s.children.size()>1);
        return root;
    }
    /**
     * These must be the FIRST actions, on both apply and reapply. Installed apps
     * can use the same numeric IDs as our controls. Remove the previous provider
     * subtree before any bar lookup, then supply a clean loading placeholder.
     * embed() replaces this placeholder after all shell actions have run.
     */
    private static RemoteViews baseShell(Context c,int layout,int rootId){
        RemoteViews root=rootId==View.NO_ID?new RemoteViews(c.getPackageName(),layout):new RemoteViews(c.getPackageName(),layout,rootId);
        root.removeAllViews(R.id.content);
        root.addView(R.id.content,new RemoteViews(c.getPackageName(),R.layout.stack_placeholder));
        return root;
    }
    private static RemoteViews contractedShell(Context c,StackState s,int rootId){
        int[] normal={R.layout.stack_top_contracted,R.layout.stack_bottom_contracted,R.layout.stack_left_contracted,R.layout.stack_contracted};
        int[] overlap={R.layout.stack_top_overlap_contracted,R.layout.stack_bottom_overlap_contracted,R.layout.stack_left_overlap_contracted,R.layout.stack_overlap_contracted};
        int[][] corners={{R.layout.stack_top_corner_0,R.layout.stack_bottom_corner_0,R.layout.stack_left_corner_0,R.layout.stack_corner_0},{R.layout.stack_top_corner_1,R.layout.stack_bottom_corner_1,R.layout.stack_left_corner_1,R.layout.stack_corner_1}};
        RemoteViews root=baseShell(c,s.contractToCorner?corners[s.cornerEnd][s.railEdge]:(s.overlapBar?overlap:normal)[s.railEdge],rootId);
        if(s.contractToCorner){root.setViewLayoutWidth(R.id.rail,s.contractedWidth,android.util.TypedValue.COMPLEX_UNIT_DIP);root.setViewLayoutHeight(R.id.rail,s.contractedWidth,android.util.TypedValue.COMPLEX_UNIT_DIP);}
        if(s.horizontalRail())root.setViewLayoutHeight(R.id.rail,s.contractedWidth,android.util.TypedValue.COMPLEX_UNIT_DIP);
        else root.setViewLayoutWidth(R.id.rail,s.contractedWidth,android.util.TypedValue.COMPLEX_UNIT_DIP);
        root.setFloat(R.id.rail,"setAlpha",s.railOpacity/100f);
        if(s.contractToCorner){
            int target=Math.max(48,s.contractedWidth);root.setViewLayoutWidth(R.id.corner_touch,target,android.util.TypedValue.COMPLEX_UNIT_DIP);root.setViewLayoutHeight(R.id.corner_touch,target,android.util.TypedValue.COMPLEX_UNIT_DIP);
            cornerMark(root,s);root.setOnClickPendingIntent(R.id.corner_touch,expandIntent(c,s.id));
        }
        root.setOnClickPendingIntent(R.id.rail,expandIntent(c,s.id));
        root.setOnClickPendingIntent(R.id.empty,PendingIntent.getActivity(c,s.id,editIntent(c,s.id),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        return root;
    }
    private static void cornerMark(RemoteViews root,StackState s){root.setTextViewTextSize(R.id.expand_mark,android.util.TypedValue.COMPLEX_UNIT_DIP,Math.min(18,s.contractedWidth*.65f));root.setViewVisibility(R.id.expand_mark,s.contractedWidth>=12?View.VISIBLE:View.GONE);}
    private static PendingIntent expandIntent(Context c,int id){
        Intent i=new Intent(c,StackControlReceiver.class).setAction(StackProvider.EXPAND).setData(Uri.parse("widgetstack://expand/"+id)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
        return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static PendingIntent navigation(Context c,int stack,int child,int direction){
        Intent i=new Intent(c,StackControlReceiver.class).setAction(StackProvider.SELECT).setData(Uri.parse("widgetstack://select/"+stack+"/"+child+"/"+direction)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,stack).putExtra("child",child).putExtra("direction",direction);
        return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static void update(Context c,int id){update(c,id,0);}
    static void update(Context c,int id,int direction){
        if(Recovery.paused(c)){Recovery.publish(c,id);return;}
        if(BarController.animating(id))return; // settle callback incorporates the newest snapshot
        updateBarMotion(c,id,direction,0);
    }
    static void updateBarMotion(Context c,int id,int direction,int barMotion){
        if(Recovery.paused(c)){Recovery.publish(c,id);return;}
        if(!owns(c,id))return;
        StackStore store=new StackStore(c);StackState s=store.get(id);
        if(s==null){MeasuredViewport.stop(c,id);s=new StackState(id);RemoteViews empty=shell(c,s,0,null,0,nextRootId(c));empty.setTextViewText(R.id.empty,"Tap to choose a saved stack");try{boolean fresh=empty.getViewId()!=View.NO_ID;WidgetPublisher.update(c,id,empty);StackDiagnostics.note(c,"publication submitted",id,"choose saved stack; fresh="+fresh);}catch(RuntimeException e){Log.w("WidgetStack","Unable to show stack chooser",e);}return;}
        AppWidgetManager m=AppWidgetManager.getInstance(c);
        // Provider metadata can be temporarily unavailable. Rendering must not
        // remove configured children or disable navigation by rewriting the stack.
        RemoteViews root=renderForHome(c,s,direction,barMotion);
        try{boolean fresh=root.getViewId()!=View.NO_ID;WidgetPublisher.update(c,id,root);if(root.getLayoutId()!=R.layout.viewport_probe)MeasuredViewport.stop(c,id);StackDiagnostics.note(c,"publication submitted",id,"selected="+s.selected+" path="+(WidgetPublisher.usesDirectPublication(android.os.Build.VERSION.SDK_INT)?"direct35":"platform")+" fresh="+fresh);}catch(RuntimeException e){
            Log.e("WidgetStack","Launcher rejected widget update",e);StackDiagnostics.note(c,"publish failed",id,e.getClass().getSimpleName());
            RemoteViews fallback=shell(c,s,0,null,0,nextRootId(c));fallback.setTextViewText(R.id.empty,"Widget update was too large or unsupported.\nTap here to edit.");
            MeasuredViewport.stop(c,id);try{WidgetPublisher.update(c,id,fallback);}catch(RuntimeException ignored){}
        }
    }
    static RemoteViews renderPreview(Context c,StackState s,Boolean compact,int motion){return render(c,s,0,compact,motion,View.NO_ID,PreviewViewport.options(c,s.id));}
    static RemoteViews render(Context c,StackState s,int direction,Boolean forceContracted){return render(c,s,direction,forceContracted,0);}
    static RemoteViews render(Context c,StackState s,int direction,Boolean forceContracted,int barMotion){return render(c,s,direction,forceContracted,barMotion,View.NO_ID);}
    /** A durable root ID makes the launcher inflate each publication instead of reusing stale controls. */
    static synchronized int nextRootId(Context c){
        android.content.SharedPreferences prefs=c.getSharedPreferences("render_generation",Context.MODE_PRIVATE);
        int id=prefs.getInt("root",1000000)%0x00fffffe+1;prefs.edit().putInt("root",id).commit();return id;
    }
    static RemoteViews renderForHome(Context c,StackState s,int direction,int barMotion){
        // A placement has one active provider. Give the actual launcher-visible
        // publication a fresh identity, without another size-variant envelope.
        // Native MATCH_PARENT axes follow the slot; option changes republish it.
        return render(c,s,direction,null,barMotion,nextRootId(c));
    }
    private static RemoteViews render(Context c,StackState s,int direction,Boolean forceContracted,int barMotion,int rootId){return render(c,s,direction,forceContracted,barMotion,rootId,null);}
    private static RemoteViews render(Context c,StackState s,int direction,Boolean forceContracted,int barMotion,int rootId,Bundle exactOptions){
        s=VisibilityProfiles.filtered(c,s);
        AppWidgetManager m=AppWidgetManager.getInstance(c);
        RemoteViews root=shell(c,s,direction,forceContracted,barMotion,rootId);
        if(s.children.isEmpty()&&new VisibilityProfiles(c).active(new StackStore(c).definitionId(s.id))!=0)root.setTextViewText(R.id.empty,"No widgets enabled in this profile.\nTap to configure.");
        RemoteViews source=StackHost.get(c).views.get(s.selected);
        if(source!=null){
            Bundle actual=childOptions(exactOptions==null?optionsFor(c,s.id):exactOptions,s,forceContracted);
            Bundle b=WidgetSizing.options(actual,s,s.selected,c.getResources().getDisplayMetrics().density);
            boolean landscape=c.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
            SizeF available=displaySize(actual,landscape),logical=displaySize(b,landscape);
            try{
                if(s.widthPx(s.selected)==0&&s.heightPx(s.selected)==0&&s.visualScalePercent(s.selected)==100)
                    RemoteViewsBridge.embedAuto(c,root,source,s.selected,logical);
                else RemoteViewsBridge.embedAligned(c,root,source,s.selected,logical,s.widthPx(s.selected),s.heightPx(s.selected),s.visualScalePercent(s.selected),s.alignment(s.selected),available);
            }
            catch(Exception|LinkageError|StackOverflowError e){
                Log.e("WidgetStack","Cannot embed widget "+s.selected,e);
                root=shell(c,s,0,forceContracted,0,rootId);root.setTextViewText(R.id.empty,"This widget could not be displayed.\nTap here to edit the stack.");
            }
        }else if(!s.children.isEmpty())root.setTextViewText(R.id.empty,"Waiting for widget…\nTap to edit or refresh");
        return root;
    }

}

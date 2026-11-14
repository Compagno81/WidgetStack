package com.kevin.widgetstack;

import android.content.Context;
import android.os.Bundle;
import android.util.SizeF;
import android.widget.RemoteViews;
import java.util.*;

/** Launcher-owned pages survive control-only publications. No scroll coordinates
 * are guessed: the original ListView/GridView instances remain attached. */
final class RetainedWidgets {
    private static final Map<Integer,List<Integer>> published=new HashMap<>();
    private static final Map<Integer,Map<Integer,RemoteViews>> snapshots=new HashMap<>();
    // Retention was validated through API 35. API 37 reports missing native
    // collection rows and failed inflation when another widget hosts a stack.
    // Use the established single-page publication on newer platform versions
    // until the nested apply/conversion path has device coverage.
    static boolean supportedOn(int api){return api>=31&&api<=35;}
    static void forget(int id){published.remove(id);snapshots.remove(id);}
    static boolean contains(int id){return published.containsKey(id);}
    static void clear(){published.clear();snapshots.clear();}
    // Host data notifications use findViewById, not a page-scoped lookup. Foreign
    // apps may reuse collection IDs. Never notify the wrong retained adapter.
    static boolean canNotify(Context c,int home,int child,int view){
        List<Integer> pages=published.get(home);
        if(pages==null)return false;
        if(pages.isEmpty()||pages.get(0)!=child)return false;
        for(java.lang.reflect.Field field:R.id.class.getFields())try{if(field.getInt(null)==view)return false;}catch(IllegalAccessException ignored){}
        for(int candidate:pages){
            RemoteViews source=StackHost.get(c).views.get(candidate);
            if(source==null)continue;
            try{if(RemoteViewsBridge.collectionIds(c,source,candidate).contains(view))return candidate==child;}
            catch(ReflectiveOperationException e){return false;}
        }
        return false;
    }

    static RemoteViews full(Context c,StackState original) throws ReflectiveOperationException {return full(c,original,0);}
    static RemoteViews full(Context c,StackState original,int motion) throws ReflectiveOperationException {
        if(!supportedOn(android.os.Build.VERSION.SDK_INT))throw new UnsupportedOperationException("Single-page compatibility mode on API "+android.os.Build.VERSION.SDK_INT);
        StackState s=VisibilityProfiles.filtered(c,original);
        RemoteViews root=controls(c,s,motion);
        root.removeAllViews(R.id.retained_pages);
        boolean landscape=c.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        Bundle actual=StackRenderer.childOptions(StackRenderer.optionsFor(c,s.id),s,s.autoContract&&!s.cornerArrows?Boolean.FALSE:null);
        SizeF available=StackRenderer.displaySize(actual,landscape);
        for(int child:s.children){
            RemoteViews page=new RemoteViews(c.getPackageName(),R.layout.retained_page);
            RemoteViews source=StackHost.get(c).views.get(child);
            if(source==null){
                RemoteViews loading=new RemoteViews(c.getPackageName(),R.layout.stack_placeholder);
                loading.setTextViewText(R.id.empty,"Waiting for widget…");page.removeAllViews(R.id.content);page.addStableView(R.id.content,loading,child);
            }else{
                SizeF logical=StackRenderer.displaySize(WidgetSizing.options(actual,s,child,c.getResources().getDisplayMetrics().density),landscape);
                boolean unchanged=snapshots.containsKey(s.id)&&snapshots.get(s.id).get(child)==source;
                boolean recyclable=true;
                try{source=RemoteViewsBridge.retainedCopy(c,source,logical,unchanged);}
                catch(IllegalArgumentException e){
                    // An additive provider hierarchy cannot safely be replayed.
                    // Refresh just this page instead of duplicating its children.
                    recyclable=false;
                    page=new RemoteViews(c.getPackageName(),R.layout.retained_page,StackRenderer.nextRootId(c));
                    StackDiagnostics.note(c,"page retention unavailable",s.id,"child="+child+" "+e.getMessage());
                }
                if(android.os.Build.VERSION.SDK_INT<35&&!RemoteViewsBridge.collectionIds(c,source,child).isEmpty())
                    throw new UnsupportedOperationException("Nested collection retention requires Android 15 or later");
                if(s.widthPx(child)==0&&s.heightPx(child)==0&&s.visualScalePercent(child)==100)
                    RemoteViewsBridge.embedAuto(c,page,source,child,logical);
                else RemoteViewsBridge.embedAligned(c,page,source,child,logical,s.widthPx(child),s.heightPx(child),s.visualScalePercent(child),s.alignment(child),available);
                if(recyclable)RemoteViewsBridge.makeRetained(page);
            }
            root.addStableView(R.id.retained_pages,page,child);
        }
        root.setDisplayedChild(R.id.retained_pages,Math.max(0,s.children.indexOf(s.selected)));
        return root;
    }
    static void remember(Context c,StackState s){
        published.put(s.id,new ArrayList<>(VisibilityProfiles.filtered(c,s).children));
        Map<Integer,RemoteViews> copy=new HashMap<>();for(int child:s.children)copy.put(child,StackHost.get(c).views.get(child));snapshots.put(s.id,copy);
    }
    static boolean navigate(Context c,StackState original,int motion){
        StackState s=VisibilityProfiles.filtered(c,original);
        if(!supportedOn(android.os.Build.VERSION.SDK_INT)){forget(s.id);return false;}
        if(s.children.isEmpty()||!s.children.equals(published.get(s.id)))return false;
        try{
            RemoteViews root=full(c,s,motion);WidgetPublisher.update(c,s.id,root);remember(c,s);return true;
        }catch(ReflectiveOperationException e){forget(s.id);return false;}
    }
    static RemoteViews controls(Context c,StackState s,int motion){
        RemoteViews root=new RemoteViews(c.getPackageName(),s.animateSwitch?R.layout.stack_retained_animated:R.layout.stack_retained);
        // A new shell keeps the previous stale-control workaround isolated from
        // provider pages. Its empty content area is transparent and not clickable.
        RemoteViews controls=StackRenderer.shell(c,s,0,null,motion);
        controls.removeAllViews(R.id.content);
        root.removeAllViews(R.id.retained_controls);
        root.addView(R.id.retained_controls,controls);
        boolean compact=motion<0||motion==0&&BarController.contracted(s);
        boolean reserve=s.showRail&&!s.cornerArrows&&!s.overlapBar&&!(compact&&s.contractToCorner);
        int thickness=Math.round((compact?s.contractedWidth:s.railWidth)*c.getResources().getDisplayMetrics().density);
        root.setViewPadding(R.id.retained_pages,reserve&&s.railEdge==StackState.LEFT?thickness:0,
                reserve&&s.railEdge==StackState.TOP?thickness:0,reserve&&s.railEdge==StackState.RIGHT?thickness:0,
                reserve&&s.railEdge==StackState.BOTTOM?thickness:0);
        root.setDisplayedChild(R.id.retained_pages,Math.max(0,s.children.indexOf(s.selected)));
        return root;
    }
}

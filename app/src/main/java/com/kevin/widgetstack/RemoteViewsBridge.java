package com.kevin.widgetstack;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.SizeF;
import android.util.SparseArray;
import android.widget.RemoteViews;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapted from Widgets Anywhere's WidgetStackProvider (MIT, Zachary Wander).
 * The host is a real AppWidgetHost. The launcher renders the provider's original
 * RemoteViews, retaining PendingIntents and collection scrolling, rather than a bitmap.
 * Android does not expose a supported nested AppWidgetHostView in RemoteViews.
 */
@android.annotation.SuppressLint({"PrivateApi", "BlockedPrivateApi"})
final class RemoteViewsBridge {
    static Object field(Object target,String name) throws ReflectiveOperationException {
        Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);
    }
    static List<Object> actions(RemoteViews v) throws ReflectiveOperationException {
        @SuppressWarnings("unchecked") List<Object> a=(List<Object>)field(v,"mActions");return a;
    }
    static RemoteViews select(Context c,RemoteViews source,SizeF size) throws ReflectiveOperationException {
        Method m=RemoteViews.class.getDeclaredMethod("getRemoteViewsToApply",Context.class,SizeF.class);
        m.setAccessible(true);return new RemoteViews((RemoteViews)m.invoke(source,c,size));
    }
    static List<Object> prepare(Context c,RemoteViews v,int child) throws ReflectiveOperationException {
        return prepare(c,v,child,Collections.newSetFromMap(new IdentityHashMap<>()),0);
    }
    private static List<Object> prepare(Context c,RemoteViews v,int child,Set<RemoteViews> visited,int depth) throws ReflectiveOperationException {
        List<Object> collections=new ArrayList<>();
        if(depth>32||!visited.add(v))return collections;
        List<Object> aa=actions(v);
        if(aa==null)return collections;
        for(Object action:aa) {
            String name=action.getClass().getSimpleName();
            if(name.contains("SetRemoteViewsAdapterIntent")) {
                Field f;
                try{f=action.getClass().getDeclaredField("mIntent");}catch(NoSuchFieldException e){f=action.getClass().getDeclaredField("intent");}
                f.setAccessible(true);Intent original=(Intent)f.get(action);
                if(original!=null&&(original.getComponent()==null||!original.getComponent().getClassName().equals(CollectionProxyService.class.getName())))
                    f.set(action,CollectionProxyService.intent(c,child,original,actionViewId(action)));
            }
            if(name.contains("SetRemoteViewsAdapterIntent")||name.contains("SetRemoteCollectionItemListAdapterAction"))collections.add(action);
            for(Field f:action.getClass().getDeclaredFields())if(f.getType()==RemoteViews.class&&!f.isSynthetic()&&!Modifier.isStatic(f.getModifiers())&&!f.getName().startsWith("this$")){
                f.setAccessible(true);RemoteViews nested=(RemoteViews)f.get(action);
                if(nested!=null)collections.addAll(prepare(c,nested,child,visited,depth+1));
            }
        }
        return collections;
    }
    static void embed(Context c,RemoteViews outer,RemoteViews source,int child,SizeF size) throws ReflectiveOperationException {
        embed(c,outer,source,child,size,1f);
    }
    static void embed(Context c,RemoteViews outer,RemoteViews source,int child,SizeF size,float scale) throws ReflectiveOperationException {
        embedFitted(c,outer,source,child,size,new SizeF(size.getWidth()*scale,size.getHeight()*scale));
    }
    static void embedAuto(Context c,RemoteViews outer,RemoteViews source,int child,SizeF size) throws ReflectiveOperationException {
        embedWithSizing(c,outer,source,child,size,null);
    }
    static void embedFitted(Context c,RemoteViews outer,RemoteViews source,int child,SizeF size,SizeF available) throws ReflectiveOperationException {
        embedWithSizing(c,outer,source,child,size,available);
    }
    /** Each explicit axis is a physical layout size; Auto follows its actual parent. */
    static void embedSized(Context c,RemoteViews outer,RemoteViews source,int child,SizeF requested,int widthPx,int heightPx) throws ReflectiveOperationException {
        RemoteViews chosen=select(c,new RemoteViews(source),requested);
        List<Object> collections=prepare(c,chosen,child);
        if(Build.VERSION.SDK_INT>=35){copyCache(source,outer);copyCache(chosen,outer);}
        RemoteViews frame=new RemoteViews(c.getPackageName(),R.layout.widget_scale_frame);
        int unit=android.util.TypedValue.COMPLEX_UNIT_PX;
        frame.setViewLayoutWidth(R.id.widget_scale_container,widthPx>0?widthPx:android.view.ViewGroup.LayoutParams.MATCH_PARENT,unit);
        frame.setViewLayoutHeight(R.id.widget_scale_container,heightPx>0?heightPx:android.view.ViewGroup.LayoutParams.MATCH_PARENT,unit);
        frame.addView(R.id.widget_scale_container,chosen);
        outer.removeAllViews(R.id.content);outer.addView(R.id.content,frame);
        List<Object> dest=actions(outer);if(dest!=null)dest.addAll(collections);
    }
    private static void embedWithSizing(Context c,RemoteViews outer,RemoteViews source,int child,SizeF size,SizeF available) throws ReflectiveOperationException {
        RemoteViews chosen=select(c,new RemoteViews(source),size);
        List<Object> collections=prepare(c,chosen,child);
        if(Build.VERSION.SDK_INT>=35){copyCache(source,outer);copyCache(chosen,outer);}
        outer.removeAllViews(R.id.content);
        if(available==null){
            // Auto uses the actual parent layout in the launcher. Options are
            // provider hints, not authoritative pixel bounds for the slot.
            outer.addView(R.id.content,chosen);
        }else{
        SizeF bounds=WidgetSizing.rootSize(c,chosen,size);
        float scale=WidgetSizing.fit(available.getWidth(),available.getHeight(),bounds.getWidth(),bounds.getHeight());
        // Fit the provider root, which may be smaller than the requested layout.
        // The outer match-parent frame centers the logical-size frame; Android's
        // View transforms also map descendant touch coordinates back to that frame.
        RemoteViews frame=new RemoteViews(c.getPackageName(),R.layout.widget_scale_frame);
        int unit=android.util.TypedValue.COMPLEX_UNIT_DIP;
        frame.setViewLayoutWidth(R.id.widget_scale_container,bounds.getWidth(),unit);
        frame.setViewLayoutHeight(R.id.widget_scale_container,bounds.getHeight(),unit);
        frame.setFloat(R.id.widget_scale_container,"setScaleX",scale);
        frame.setFloat(R.id.widget_scale_container,"setScaleY",scale);
        frame.addView(R.id.widget_scale_container,chosen);outer.addView(R.id.content,frame);
        }
        // Android's collection conversion scans root actions on some OS releases.
        // Repeating only collection adapter actions at the root makes them discoverable.
        List<Object> dest=actions(outer);if(dest!=null)dest.addAll(collections);
    }
    private static void copyCache(RemoteViews from,RemoteViews to) throws ReflectiveOperationException {
        Object src=field(from,"mCollectionCache"),dst=field(to,"mCollectionCache");
        @SuppressWarnings("unchecked") SparseArray<String> ids=(SparseArray<String>)field(src,"mIdToUriMapping");
        @SuppressWarnings("unchecked") Map<String,Object> items=(Map<String,Object>)field(src,"mUriToCollectionMapping");
        @SuppressWarnings("unchecked") SparseArray<String> destIds=(SparseArray<String>)field(dst,"mIdToUriMapping");
        @SuppressWarnings("unchecked") Map<String,Object> destItems=(Map<String,Object>)field(dst,"mUriToCollectionMapping");
        for(int i=0;i<ids.size();i++){String uri=ids.valueAt(i);destIds.put(ids.keyAt(i),uri);}
        destItems.putAll(items);
    }
    private static int actionViewId(Object action) {
        for(Class<?> type=action.getClass();type!=null;type=type.getSuperclass())
            for(String name:new String[]{"mViewId","viewId"})try{
                Field f=type.getDeclaredField(name);f.setAccessible(true);return f.getInt(action);
            }catch(ReflectiveOperationException ignored){}
        return 0;
    }
}

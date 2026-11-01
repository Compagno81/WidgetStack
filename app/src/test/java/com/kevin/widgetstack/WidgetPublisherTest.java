package com.kevin.widgetstack;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.robolectric.util.ReflectionHelpers.ClassParameter.from;
import static org.junit.Assert.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.Executor;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,application=Application.class)
public class WidgetPublisherTest {
 @Test @Config(sdk={31,35}) public void switchingCollectionsWithIdenticalViewIdsKeepsChildBindingsSeparate()throws Exception{
  Context c=RuntimeEnvironment.getApplication();RemoteViews source=new RemoteViews(c.getPackageName(),R.layout.stack_placeholder);
  Intent service=new Intent().setComponent(new ComponentName("fixture.calendar","Rows"));source.setRemoteAdapter(R.id.empty,service);
  RemoteViews a=new RemoteViews(c.getPackageName(),R.layout.stack),b=new RemoteViews(c.getPackageName(),R.layout.stack);
  RemoteViewsBridge.embedAuto(c,a,source,11,new android.util.SizeF(400,300));
  RemoteViewsBridge.embedAuto(c,b,source,22,new android.util.SizeF(400,300));
  Intent one=adapterIntent(a),two=adapterIntent(b);
  assertEquals(11,one.getIntExtra("child",-1));assertEquals(22,two.getIntExtra("child",-1));assertNotEquals(one.getData(),two.getData());
  assertEquals("Original provider snapshot must not be rewritten",service.getComponent(),adapterIntent(source).getComponent());
 }
 private Intent adapterIntent(RemoteViews views)throws Exception{
  for(Object action:RemoteViewsBridge.actions(views))if(action.getClass().getSimpleName().contains("SetRemoteViewsAdapterIntent")){
   for(String name:new String[]{"mIntent","intent"})try{Field f=action.getClass().getDeclaredField(name);f.setAccessible(true);return(Intent)f.get(action);}catch(NoSuchFieldException ignored){}
  }
  throw new AssertionError("Missing collection adapter");
 }
 @Test public void nextWidgetCanPublishEvenWhenCollectionConversionQueueIsBlocked()throws Exception{
  Context c=RuntimeEnvironment.getApplication();AppWidgetManager manager=AppWidgetManager.getInstance(c);
  Class<?> flags=Class.forName("android.appwidget.flags.Flags"),features=Class.forName("android.appwidget.flags.FeatureFlags"),contract=Class.forName("com.android.internal.appwidget.IAppWidgetService");
  Object oldFlags=ReflectionHelpers.getStaticField(flags,"FEATURE_FLAGS"),oldExecutor=ReflectionHelpers.getStaticField(AppWidgetManager.class,"sUpdateExecutor"),oldService=ReflectionHelpers.getField(manager,"mService");
  List<Runnable> blocked=new ArrayList<>();List<Integer> published=new ArrayList<>();
  try{
   ReflectionHelpers.setStaticField(flags,"FEATURE_FLAGS",Proxy.newProxyInstance(features.getClassLoader(),new Class[]{features},(p,m,a)->m.getName().equals("remoteAdapterConversion")));
   ReflectionHelpers.setStaticField(AppWidgetManager.class,"sUpdateExecutor",(Executor)blocked::add);
   ReflectionHelpers.setField(manager,"mHasPostedLegacyLists",true);
   ReflectionHelpers.setField(manager,"mService",Proxy.newProxyInstance(contract.getClassLoader(),new Class[]{contract},(p,m,a)->{if(m.getName().equals("updateAppWidgetIds"))published.add(((int[])a[1])[0]);return null;}));
   RemoteViews next=new RemoteViews(c.getPackageName(),R.layout.stack_placeholder);
   // Reproduce framework queueing: even a non-scrollable next widget waits
   // once this manager has posted a legacy agenda. Do not execute that queue.
   Shadow.directlyOn(manager,AppWidgetManager.class,"updateAppWidget",from(int[].class,new int[]{701}),from(RemoteViews.class,next));
   assertEquals(1,blocked.size());assertTrue(published.isEmpty());
   WidgetPublisher.update(c,702,next);WidgetPublisher.update(c,703,next);
   assertEquals(Arrays.asList(702,703),published);assertEquals(1,blocked.size());
  }finally{ReflectionHelpers.setStaticField(flags,"FEATURE_FLAGS",oldFlags);ReflectionHelpers.setStaticField(AppWidgetManager.class,"sUpdateExecutor",oldExecutor);ReflectionHelpers.setField(manager,"mService",oldService);ReflectionHelpers.setField(manager,"mHasPostedLegacyLists",false);}
 }
}

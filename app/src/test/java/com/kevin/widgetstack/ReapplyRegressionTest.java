package com.kevin.widgetstack;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Exercise RemoteViews.reapply, unlike the always-reinflate host-view test shadow. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class,qualifiers="mdpi")
public class ReapplyRegressionTest {
 private Context c(){return RuntimeEnvironment.getApplication();}
 @Before public void reset(){org.robolectric.util.ReflectionHelpers.setStaticField(StackHost.class,"instance",null);}
 private StackState state(){
  StackState s=new StackState(909);s.add(10);s.add(20);
  // A separate APK's numeric resource IDs may equal ours. Same-layout synthetic
  // content gives deterministic collisions for every bar ID, including indicators.
  RemoteViews child=new RemoteViews(c().getPackageName(),R.layout.stack);
  child.setTextViewText(R.id.empty,"Provider action");
  child.setOnClickPendingIntent(R.id.empty,PendingIntent.getBroadcast(c(),88,new Intent("provider.action").setPackage(c().getPackageName()),PendingIntent.FLAG_IMMUTABLE));
  StackHost.get(c()).views.put(10,child);StackHost.get(c()).views.put(20,child);
  return s;
 }
 private View layout(View v){v.measure(View.MeasureSpec.makeMeasureSpec(320,1073741824),View.MeasureSpec.makeMeasureSpec(240,1073741824));v.layout(0,0,320,240);return v;}
 private View rail(View v){return ((ViewGroup)v).getChildAt(1);}
 @Test public void reapplyUpdatesTheActualBarDespiteCollidingProviderIds(){
  StackState s=state();View v=layout(StackRenderer.render(c(),s,0,false).apply(c(),new FrameLayout(c())));
  assertEquals(36,rail(v).getWidth());
  for(int width:new int[]{64,12,48}){
   s.railWidth=width;s.railOpacity=73;s.buttonsOnly=true;
   StackRenderer.render(c(),s,0,false).reapply(c(),v);layout(v);
   assertEquals("Update must target the outer bar",width,rail(v).getWidth());assertEquals(.73f,rail(v).getAlpha(),.001f);assertEquals(View.GONE,rail(v).findViewById(R.id.rail_middle).getVisibility());
  }
 }
 @Test public void reapplyReplacesOuterDotsAndPreservesBothSetsOfClickActions(){
  StackState s=state();View v=layout(StackRenderer.render(c(),s,0,false).apply(c(),new FrameLayout(c())));
  s.remove(20);s.add(30);s.add(40);
  StackRenderer.render(c(),s,0,false).reapply(c(),v);layout(v);
  LinearLayout dots=rail(v).findViewById(R.id.indicators);assertEquals(3,dots.getChildCount());
  int before=shadowOf((Application)c()).getBroadcastIntents().size();dots.getChildAt(2).performClick();Intent sent=shadowOf((Application)c()).getBroadcastIntents().get(before);assertEquals(StackProvider.SELECT,sent.getAction());assertEquals(40,sent.getIntExtra("child",-1));
  ViewGroup content=((ViewGroup)((ViewGroup)v).getChildAt(0)).findViewById(R.id.content);View provider=content.getChildAt(0).findViewById(R.id.empty);before=shadowOf((Application)c()).getBroadcastIntents().size();provider.performClick();assertEquals("provider.action",shadowOf((Application)c()).getBroadcastIntents().get(before).getAction());
 }
 @Test public void repeatedContractedRefreshesKeepExpansionAndThicknessCurrent(){
  StackState s=state();s.autoContract=true;s.contractedWidth=6;
  View v=layout(StackRenderer.render(c(),s,0,true).apply(c(),new FrameLayout(c())));
  s.contractedWidth=18;s.railOpacity=65;StackRenderer.render(c(),s,0,true).reapply(c(),v);layout(v);assertEquals(18,rail(v).getWidth());assertEquals(.65f,rail(v).getAlpha(),.001f);
  int before=shadowOf((Application)c()).getBroadcastIntents().size();rail(v).performClick();assertEquals(StackProvider.EXPAND,shadowOf((Application)c()).getBroadcastIntents().get(before).getAction());
 }
 @Test public void losingACachedSnapshotRestoresThePlaceholderAndKeepsTheBarUsable(){
  StackState s=state();View v=layout(StackRenderer.render(c(),s,0,false).apply(c(),new FrameLayout(c())));
  StackHost.get(c()).views.clear();s.railWidth=60;s.buttonsOnly=true;
  StackRenderer.render(c(),s,0,false).reapply(c(),v);layout(v);assertEquals(60,rail(v).getWidth());
  ViewGroup content=((ViewGroup)((ViewGroup)v).getChildAt(0)).findViewById(R.id.content);
  TextView placeholder=content.findViewById(R.id.empty);assertNotNull(placeholder);assertTrue(placeholder.getText().toString().startsWith("Waiting for widget"));assertEquals(1,content.getChildCount());
 }
 @Test public void asynchronousReapplyAlsoClearsCollisionsBeforeResolvingBarActions() throws Exception {
  StackState s=state();View v=layout(StackRenderer.render(c(),s,0,false).apply(c(),new FrameLayout(c())));s.railWidth=56;s.railOpacity=81;
  RemoteViews next=StackRenderer.render(c(),s,0,false);int[] applied={0};Throwable[] error={null};
  Class<?> listener=Class.forName("android.widget.RemoteViews$OnViewAppliedListener");
  Object callback=java.lang.reflect.Proxy.newProxyInstance(listener.getClassLoader(),new Class<?>[]{listener},(proxy,method,args)->{if(method.getName().equals("onViewApplied"))applied[0]++;if(method.getName().equals("onError"))error[0]=(Throwable)args[0];return null;});
  java.lang.reflect.Method method=RemoteViews.class.getDeclaredMethod("reapplyAsync",Context.class,View.class,java.util.concurrent.Executor.class,listener);method.setAccessible(true);method.invoke(next,c(),v,(java.util.concurrent.Executor)Runnable::run,callback);
  shadowOf(Looper.getMainLooper()).idle();assertNull(error[0]);assertEquals(1,applied[0]);layout(v);assertEquals(56,rail(v).getWidth());assertEquals(.81f,rail(v).getAlpha(),.001f);
 }

}

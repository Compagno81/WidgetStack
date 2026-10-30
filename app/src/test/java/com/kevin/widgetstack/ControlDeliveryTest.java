package com.kevin.widgetstack;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Exercise parcelled production controls through manifest receiver dispatch. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class)
public class ControlDeliveryTest {
 Context c;StackStore store;int saved;
 @Before public void setup() throws Exception {
  c=RuntimeEnvironment.getApplication();store=new StackStore(c);
  org.robolectric.util.ReflectionHelpers.setStaticField(StackHost.class,"instance",null);
  bind(901,StackProvider.class.getName());bind(902,StackProvider.class.getName());
  for(int id:new int[]{11,22,33})bind(id,"com.kevin.widgetstack.TestChild");
  StackState s=store.createSaved("Buttons");s.add(11);s.add(22);s.add(33);saved=s.id;store.put(s);
  assertTrue(SavedStacks.place(c,901,saved));assertTrue(SavedStacks.place(c,902,saved));
 }
 @After public void cleanup(){BarController.cancel(901);BarController.cancel(902);}
 void bind(int id,String cls){
  AppWidgetProviderInfo info=new AppWidgetProviderInfo();info.provider=new ComponentName(c,cls);
  shadowOf(AppWidgetManager.getInstance(c)).addInstalledProvider(info);
  shadowOf(AppWidgetManager.getInstance(c)).setAllowedToBindAppWidgets(true);
  assertTrue(AppWidgetManager.getInstance(c).bindAppWidgetIdIfAllowed(id,info.provider));
 }
 View published(int id){
  RemoteViews original=StackRenderer.renderForHome(c,store.get(id),0,0);
  Parcel parcel=Parcel.obtain();original.writeToParcel(parcel,0);parcel.setDataPosition(0);
  RemoteViews transmitted=RemoteViews.CREATOR.createFromParcel(parcel);parcel.recycle();
  return transmitted.apply(c,new FrameLayout(c));
 }
 void tap(View view){assertTrue(view.performClick());shadowOf(Looper.getMainLooper()).idle();}
 @Test public void controlsUsePrivateFilterlessEndpoint() throws Exception {
  ActivityInfo info=c.getPackageManager().getReceiverInfo(new ComponentName(c,StackControlReceiver.class),0);
  assertFalse(info.exported);
  tap(published(901).findViewById(R.id.next));
  Intent sent=shadowOf((Application)c).getBroadcastIntents().get(shadowOf((Application)c).getBroadcastIntents().size()-1);
  assertEquals(new ComponentName(c,StackControlReceiver.class),sent.getComponent());
  assertEquals(22,store.get(901).selected);
 }
 @Test public void nextPreviousAndDotsChangeHomeSelectionOnEveryEdgeAndMode(){
  for(int edge=0;edge<4;edge++)for(boolean buttons:new boolean[]{false,true})for(boolean overlap:new boolean[]{false,true}){
   StackState s=store.get(saved);s.railEdge=edge;s.buttonsOnly=buttons;s.overlapBar=overlap;store.put(s);store.selectOnHome(901,11);
   View root=published(901);tap(root.findViewById(R.id.previous));assertEquals(33,store.get(901).selected);
   tap(published(901).findViewById(R.id.next));assertEquals(11,store.get(901).selected);
   if(!buttons){LinearLayout dots=published(901).findViewById(R.id.indicators);tap(dots.getChildAt(1));assertEquals(22,store.get(901).selected);}
   assertEquals(11,store.get(902).selected);assertEquals(11,store.get(saved).selected);
  }
 }
 @Test public void rapidTapsOnOnePublicationAreAllApplied(){
  View next=published(901).findViewById(R.id.next);
  for(int n=0;n<20;n++)tap(next);
  assertEquals(33,store.get(901).selected);
 }
 @Test public void contractedStripAndCornerExpandThenNavigate(){
  for(boolean corner:new boolean[]{false,true}){
   BarController.cancel(901);StackState s=store.get(saved);s.autoContract=true;s.contractToCorner=corner;store.put(s);store.selectOnHome(901,11);
   assertTrue(BarController.contracted(store.get(901)));
   tap(published(901).findViewById(R.id.rail));assertFalse(BarController.contracted(store.get(901)));
   tap(published(901).findViewById(R.id.next));assertEquals(22,store.get(901).selected);
  }
 }
 @Test public void oldPublishedIntentsStillNavigateAndForeignIdsAreRejected(){
  Intent legacy=new Intent(c,StackProvider.class).setAction(StackProvider.SELECT).setData(android.net.Uri.parse("widgetstack://select/901/-1/1")).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,901).putExtra("direction",1);
  c.sendBroadcast(legacy);shadowOf(Looper.getMainLooper()).idle();assertEquals(22,store.get(901).selected);
  Intent invalid=new Intent(c,StackControlReceiver.class).setAction(StackProvider.SELECT).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,saved).putExtra("direction",1);
  c.sendBroadcast(invalid);shadowOf(Looper.getMainLooper()).idle();assertEquals(11,store.get(saved).selected);
 }
}

package com.kevin.widgetstack;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Rect;
import android.net.Uri;
import android.os.*;
import android.test.ActivityInstrumentationTestCase2;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;

/** Real OS delivery: fixture APK hosts the stack in its own process and UID. */
@SuppressWarnings("deprecation")
public final class CrossUidControlsTest extends ActivityInstrumentationTestCase2<MainActivity> {
 private static final Uri EVENTS=Uri.parse("content://com.kevin.widgetfixture.events");
 private Context c;private int home=-1,saved=-1,a=-1,b=-1;
 public CrossUidControlsTest(){super(MainActivity.class);}
 private interface Ready {boolean check();}
 private void await(String message,Ready ready) throws Exception {
  long end=SystemClock.elapsedRealtime()+60000;
  while(SystemClock.elapsedRealtime()<end){if(ready.check())return;Thread.sleep(100);}fail(message);
 }
 private AccessibilityNodeInfo node(AccessibilityNodeInfo root,String description){
  if(root==null)return null;
  if(root.getContentDescription()!=null&&root.getContentDescription().toString().startsWith(description))return root;
  for(int n=0;n<root.getChildCount();n++){AccessibilityNodeInfo found=node(root.getChild(n),description);if(found!=null)return found;}return null;
 }
 private AccessibilityNodeInfo node(String description){return node(getInstrumentation().getUiAutomation().getRootInActiveWindow(),description);}
 private void tap(String description) throws Exception {
  await("Missing external-host control: "+description,()->node(description)!=null);
  Rect bounds=new Rect();node(description).getBoundsInScreen(bounds);assertFalse(bounds.isEmpty());
  long down=SystemClock.uptimeMillis();
  for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){
   MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,bounds.centerX(),bounds.centerY(),0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);
   assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(e,true));e.recycle();
  }
 }
 private int child(String provider){int id=StackHost.get(c).allocateAppWidgetId();assertTrue(AppWidgetManager.getInstance(c).bindAppWidgetIdIfAllowed(id,new ComponentName("com.kevin.widgetfixture","com.kevin.widgetfixture."+provider)));return id;}
 public void testTouchDeliveryFromAnotherUid() throws Exception {
  c=getActivity();
  getInstrumentation().runOnMainSync(()->{a=child("PlainWidget");b=child("NestedWidget");});
  home=c.getContentResolver().call(EVENTS,"allocate_home",null,null).getInt("id",-1);assertTrue("Grant bind permission to the fixture",home>=0);
  getInstrumentation().runOnMainSync(()->{
   StackStore store=new StackStore(c);StackState state=store.createSaved("Cross UID test");saved=state.id;state.add(a);state.add(b);store.put(state);assertTrue(SavedStacks.place(c,home,saved));
  });
  c.startActivity(new Intent().setComponent(new ComponentName("com.kevin.widgetfixture","com.kevin.widgetfixture.LauncherActivity")).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,home).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  tap("Next widget");await("Next tap did not select second child",()->new StackStore(c).get(home).selected==b);
  tap("Previous widget");await("Previous tap did not select first child",()->new StackStore(c).get(home).selected==a);
  tap("Widget 2");await("Dot tap did not select second child",()->new StackStore(c).get(home).selected==b);
  // Publish changed settings while the external host stays alive, then tap again.
  getInstrumentation().runOnMainSync(()->{
   StackStore store=new StackStore(c);StackState state=store.get(saved);state.railEdge=StackState.TOP;state.overlapBar=true;state.buttonsOnly=true;store.put(state);SavedStacks.changed(c,saved);
  });
  tap("Next widget");await("Buttons after settings publication did not switch",()->new StackStore(c).get(home).selected==a);
  getInstrumentation().runOnMainSync(()->{
   StackStore store=new StackStore(c);StackState state=store.get(saved);state.autoContract=true;state.contractToCorner=true;state.contractedWidth=24;state.contractDelaySeconds=4;store.put(state);BarController.cancel(home);StackRenderer.update(c,home);
  });
  tap("Expand navigation bar");
  tap("Next widget");await("Expanded bar did not switch",()->new StackStore(c).get(home).selected==b);
 }
 @Override protected void tearDown() throws Exception {
  if(c!=null){
   if(home>=0){Bundle extras=new Bundle();extras.putInt("id",home);c.getContentResolver().call(EVENTS,"delete_home",null,extras);getInstrumentation().runOnMainSync(()->new StackProvider().onDeleted(c,new int[]{home}));}
   getInstrumentation().runOnMainSync(()->{if(saved<=-2)new StackStore(c).delete(saved);for(int id:new int[]{a,b})if(id>=0)StackHost.get(c).deleteAppWidgetId(id);});
  }
  super.tearDown();
 }
}

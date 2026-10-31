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
 public void testAutoActualBoundsAndEdgeTapFromAnotherUid() throws Exception {
  c=getActivity();getInstrumentation().runOnMainSync(()->a=child("PlainWidget"));
  home=c.getContentResolver().call(EVENTS,"allocate_home",null,null).getInt("id",-1);assertTrue(home>=0);
  getInstrumentation().runOnMainSync(()->{
   StackStore store=new StackStore(c);StackState state=store.createSaved("Auto actual bounds");saved=state.id;state.add(a);state.showRail=false;state.setLogicalSizePercent(a,150);store.put(state);
   Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,80);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,80);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,60);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,60);
   AppWidgetManager.getInstance(c).updateAppWidgetOptions(home,options);assertTrue(SavedStacks.place(c,home,saved));
  });
  c.startActivity(new Intent().setComponent(new ComponentName("com.kevin.widgetfixture","com.kevin.widgetfixture.LauncherActivity")).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,home).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  for(int[] size:new int[][]{{400,300},{300,400},{500,200}}){
   await("External launcher must start",()->launcherBounds().getInt("width")>0);
   await("External provider must be visible",()->hasText(getInstrumentation().getUiAutomation().getRootInActiveWindow(),"External widget"));
   Bundle request=new Bundle();request.putInt("width",size[0]);request.putInt("height",size[1]);c.getContentResolver().call(EVENTS,"launcher_resize",null,request);
   await("Provider must fill external host despite stale options",()->{Bundle geometry=launcherBounds();return geometry.getInt("width")==size[0]&&geometry.getInt("height")==size[1]&&geometry.getInt("provider_width")==size[0]&&geometry.getInt("provider_height")==size[1]&&geometry.getInt("provider_left")==0&&geometry.getInt("provider_top")==0;});
   getInstrumentation().getUiAutomation().waitForIdle(100,10000);
   Bundle geometry=launcherBounds();int x=geometry.getInt("left")+size[0]-4,y=geometry.getInt("top")+size[1]/2;
   android.graphics.Bitmap image=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);assertEquals("External provider background reaches right edge",0xffb9d9ec,image.getPixel(x,y));
   java.io.File directory=c.getExternalFilesDir("fit-evidence");assertNotNull(directory);directory.mkdirs();
   try(java.io.FileOutputStream file=new java.io.FileOutputStream(new java.io.File(directory,"cross-uid-"+size[0]+"x"+size[1]+".png"))){assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,file));}
   image.recycle();
   c.getContentResolver().call(EVENTS,"reset",null,null);long now=SystemClock.uptimeMillis();
   for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent event=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(event,true));event.recycle();}
   await("Visible edge tap must deliver provider PendingIntent across UIDs",()->c.getContentResolver().call(EVENTS,"read",null,null).getInt("row",-999)==-1);
   long keyTime=SystemClock.uptimeMillis();for(int action:new int[]{KeyEvent.ACTION_DOWN,KeyEvent.ACTION_UP})assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(new KeyEvent(keyTime,SystemClock.uptimeMillis(),action,KeyEvent.KEYCODE_BACK,0),true));
  }
 }
 private boolean hasText(AccessibilityNodeInfo node,String prefix){if(node==null)return false;if(node.getText()!=null&&node.getText().toString().startsWith(prefix))return true;for(int i=0;i<node.getChildCount();i++)if(hasText(node.getChild(i),prefix))return true;return false;}
 private Bundle launcherBounds(){Bundle result=c.getContentResolver().call(EVENTS,"launcher_bounds",null,null);return result==null?new Bundle():result;}
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

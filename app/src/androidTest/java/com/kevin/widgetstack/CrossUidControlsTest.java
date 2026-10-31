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
  c.startActivity(new Intent().setComponent(new ComponentName("com.kevin.widgetfixture","com.kevin.widgetfixture.LauncherActivity")).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,home).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
  for(int[] size:new int[][]{{400,300},{300,400},{500,200}}){
   await("External launcher must bind this test widget ID",()->{Bundle geometry=launcherBounds();return geometry.getInt("home_id",-1)==home&&geometry.getInt("width")>0;});
   Bundle request=new Bundle();request.putInt("width",size[0]);request.putInt("height",size[1]);c.getContentResolver().call(EVENTS,"launcher_resize",null,request);
   await("Provider must fill external host despite stale options",()->{Bundle geometry=launcherBounds();return geometry.getInt("width")==size[0]&&geometry.getInt("height")==size[1]&&geometry.getInt("provider_width")==size[0]&&geometry.getInt("provider_height")==size[1]&&geometry.getInt("provider_left")==0&&geometry.getInt("provider_top")==0;});
   getInstrumentation().getUiAutomation().waitForIdle(100,10000);
   Bundle geometry=launcherBounds();int x=geometry.getInt("left")+size[0]-4,y=geometry.getInt("top")+size[1]/2;
   FitScreenshot.assertPixel(getInstrumentation().getUiAutomation(),c,"cross-uid-"+size[0]+"x"+size[1],x,y,0xffb9d9ec);
   c.getContentResolver().call(EVENTS,"reset",null,null);long now=SystemClock.uptimeMillis();
   for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent event=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(event,true));event.recycle();}
   await("Visible edge tap must deliver provider PendingIntent across UIDs",()->c.getContentResolver().call(EVENTS,"read",null,null).getInt("row",-999)==-1);
   long keyTime=SystemClock.uptimeMillis();for(int action:new int[]{KeyEvent.ACTION_DOWN,KeyEvent.ACTION_UP})assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(new KeyEvent(keyTime,SystemClock.uptimeMillis(),action,KeyEvent.KEYCODE_BACK,0),true));
  }
 }
 public void testCustomMatchingAspectUsesEntireActualArea() throws Exception {
  c=getActivity();getInstrumentation().runOnMainSync(()->a=child("PlainWidget"));
  home=c.getContentResolver().call(EVENTS,"allocate_home",null,null).getInt("id",-1);assertTrue(home>=0);
  getInstrumentation().runOnMainSync(()->{
   StackStore store=new StackStore(c);StackState state=store.createSaved("Custom regression");saved=state.id;state.add(a);state.showRail=false;state.setDimensions(a,200,150);store.put(state);
   Bundle hints=new Bundle();hints.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,80);hints.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,80);hints.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,60);hints.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,60);
   AppWidgetManager.getInstance(c).updateAppWidgetOptions(home,hints);assertTrue(SavedStacks.place(c,home,saved));
  });
  c.startActivity(new Intent().setComponent(new ComponentName("com.kevin.widgetfixture","com.kevin.widgetfixture.LauncherActivity")).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,home).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
  await("Correct external host must attach",()->launcherBounds().getInt("home_id",-1)==home);
  // Works with the old APK too: it displays a provider immediately; the new
  // build first measures its full-size calibration surface with a real tap.
  await("Provider must appear",()->{
   Bundle g=launcherBounds();if(g.getInt("provider_width")>0)return true;
   long now=SystemClock.uptimeMillis();for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent e=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,g.getInt("left")+200,g.getInt("top")+150,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);getInstrumentation().getUiAutomation().injectInputEvent(e,true);e.recycle();}return false;
  });
  Bundle geometry=launcherBounds();String version=c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName;
  android.util.Log.i("CUSTOM_FIT","matching-aspect version="+version+" geometry="+geometry);
  android.graphics.Bitmap frame=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(frame);try{FitScreenshot.save(c,"matching-aspect-"+version,frame);}finally{frame.recycle();}
  assertEquals("custom width must fill real host: "+geometry,400,geometry.getInt("provider_width"));
  assertEquals("custom height must fill real host: "+geometry,300,geometry.getInt("provider_height"));
  assertEquals(0,geometry.getInt("provider_left"));assertEquals(0,geometry.getInt("provider_top"));
 }
 public void testCustomDimensionsFitActualBoundsAcrossUids() throws Exception {
  c=getActivity();getInstrumentation().runOnMainSync(()->a=child("PlainWidget"));
  home=c.getContentResolver().call(EVENTS,"allocate_home",null,null).getInt("id",-1);assertTrue(home>=0);
  getInstrumentation().runOnMainSync(()->{
   StackStore store=new StackStore(c);StackState state=store.createSaved("Custom actual bounds");saved=state.id;state.add(a);state.showRail=false;state.setDimensions(a,200,150);store.put(state);
   Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,80);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,80);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,60);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,60);
   AppWidgetManager.getInstance(c).updateAppWidgetOptions(home,options);assertTrue(SavedStacks.place(c,home,saved));
  });
  c.startActivity(new Intent().setComponent(new ComponentName("com.kevin.widgetfixture","com.kevin.widgetfixture.LauncherActivity")).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,home).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
  calibrateViewport();
  for(int[] size:new int[][]{{400,300},{300,400},{500,200}}){
   Bundle request=new Bundle();request.putInt("width",size[0]);request.putInt("height",size[1]);c.getContentResolver().call(EVENTS,"launcher_resize",null,request);
   await("Launcher size change must be laid out",()->{Bundle g=launcherBounds();return g.getInt("home_id",-1)==home&&g.getInt("width")==size[0]&&g.getInt("height")==size[1];});
   getInstrumentation().runOnMainSync(()->new StackProvider().onAppWidgetOptionsChanged(c,AppWidgetManager.getInstance(c),home,AppWidgetManager.getInstance(c).getAppWidgetOptions(home)));
   calibrateViewport();
   for(int[] dimensions:new int[][]{{200,0},{0,150},{200,150},{800,0},{0,600},{800,600}}){
    getInstrumentation().runOnMainSync(()->{StackStore store=new StackStore(c);StackState state=store.get(saved);state.setDimensions(a,dimensions[0],dimensions[1]);store.put(state);SavedStacks.changed(c,saved);});
    float logicalW=dimensions[0]==0?size[0]:dimensions[0],logicalH=dimensions[1]==0?size[1]:dimensions[1];
    float scale=Math.min(size[0]/logicalW,size[1]/logicalH);int w=Math.round(logicalW*scale),h=Math.round(logicalH*scale),left=(size[0]-w)/2,top=(size[1]-h)/2;
    String label="custom-"+size[0]+"x"+size[1]+"-"+dimensions[0]+"x"+dimensions[1];
    await(label+" must reach an edge and preserve aspect ratio",()->{Bundle g=launcherBounds();return Math.abs(g.getInt("provider_width")-w)<=1&&Math.abs(g.getInt("provider_height")-h)<=1&&Math.abs(g.getInt("provider_left")-left)<=1&&Math.abs(g.getInt("provider_top")-top)<=1;});
    await("External host must regain input focus",()->launcherBounds().getBoolean("focused"));
    getInstrumentation().getUiAutomation().waitForIdle(100,10000);
    Bundle g=launcherBounds();assertTrue("Provider click listener must survive fitting",g.getBoolean("provider_clickable"));assertTrue(g.getBoolean("provider_enabled"));int x=g.getInt("left")+left+w-4,y=g.getInt("top")+top+Math.min(8,h/4);
    FitScreenshot.assertPixel(getInstrumentation().getUiAutomation(),c,label,x,y,0xffb9d9ec);
    android.util.Log.i("CUSTOM_FIT",label+" actual="+g);
    c.getContentResolver().call(EVENTS,"reset",null,null);long now=SystemClock.uptimeMillis();
    for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent event=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(event,true));event.recycle();}
    await("Scaled edge tap must deliver provider PendingIntent",()->c.getContentResolver().call(EVENTS,"read",null,null).getInt("row",-999)==-1);
    long keyTime=SystemClock.uptimeMillis();for(int action:new int[]{KeyEvent.ACTION_DOWN,KeyEvent.ACTION_UP})assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(new KeyEvent(keyTime,SystemClock.uptimeMillis(),action,KeyEvent.KEYCODE_BACK,0),true));
   }
  }
 }
 private void calibrateViewport() throws Exception {
  await("Calibration view must be visible",()->{Bundle g=launcherBounds();return g.getInt("home_id",-1)==home&&g.getInt("width")>0;});
  long end=SystemClock.elapsedRealtime()+30000;
  while(!MeasuredViewport.has(c,home)&&SystemClock.elapsedRealtime()<end){
   Bundle g=launcherBounds();long now=SystemClock.uptimeMillis();
   for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent e=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,g.getInt("left")+g.getInt("width")/2,g.getInt("top")+g.getInt("height")/2,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);getInstrumentation().getUiAutomation().injectInputEvent(e,true);e.recycle();}
   Thread.sleep(500);
  }
  assertTrue("Launcher tap must deliver its actual bounds across UIDs",MeasuredViewport.has(c,home));
 }
 private Bundle launcherBounds(){Bundle result=c.getContentResolver().call(EVENTS,"launcher_bounds",null,null);return result==null?new Bundle():result;}
 public void testTouchDeliveryFromAnotherUid() throws Exception {
  c=getActivity();
  getInstrumentation().runOnMainSync(()->{a=child("PlainWidget");b=child("NestedWidget");});
  home=c.getContentResolver().call(EVENTS,"allocate_home",null,null).getInt("id",-1);assertTrue("Grant bind permission to the fixture",home>=0);
  getInstrumentation().runOnMainSync(()->{
   StackStore store=new StackStore(c);StackState state=store.createSaved("Cross UID test");saved=state.id;state.add(a);state.add(b);store.put(state);assertTrue(SavedStacks.place(c,home,saved));
  });
  c.startActivity(new Intent().setComponent(new ComponentName("com.kevin.widgetfixture","com.kevin.widgetfixture.LauncherActivity")).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,home).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
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

package com.kevin.widgetstack;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Device integration tests. Install :fixture and grant bind permission with adb
 * shell appwidget grantbind --package com.kevin.widgetstack --user 0 first.
 * The fixture is a separately installed APK/UID, not a mock widget provider.
 */
@SuppressWarnings("deprecation")
public class WidgetIntegrationTest extends ActivityInstrumentationTestCase2<MainActivity> {
 MainActivity activity;Context c;AppWidgetManager manager;AppWidgetHost launcher;
 ArrayList<Integer> outerIds=new ArrayList<>();
 public WidgetIntegrationTest(){super(MainActivity.class);}
 @Override protected void setUp() throws Exception {
  super.setUp();setActivityIntent(new Intent(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));activity=getActivity();c=activity;manager=AppWidgetManager.getInstance(c);
  getInstrumentation().runOnMainSync(()->{launcher=new AppWidgetHost(c,9998);launcher.startListening();});
 }
 private int allocate(String provider){
  final int[] result={-1};getInstrumentation().runOnMainSync(()->{
   int id=StackHost.get(c).allocateAppWidgetId();
   assertTrue("Binding must be granted by test setup",manager.bindAppWidgetIdIfAllowed(id,new ComponentName("com.kevin.widgetfixture","com.kevin.widgetfixture."+provider)));
   result[0]=id;
  });return result[0];
 }
 private int stack(int...children){
  final int[] out={-1};getInstrumentation().runOnMainSync(()->{
   int id=launcher.allocateAppWidgetId();assertTrue(manager.bindAppWidgetIdIfAllowed(id,new ComponentName(c,StackProvider.class)));
   outerIds.add(id);StackState s=new StackState(id);for(int child:children)s.add(child);new StackStore(c).put(s);
   Bundle size=new Bundle();size.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,400);size.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,400);size.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,300);size.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,300);manager.updateAppWidgetOptions(id,size);
   StackHost.get(c).sync();StackRenderer.updateSizes(c,id);StackRenderer.update(c,id);out[0]=id;
  });return out[0];
 }
 private AppWidgetHostView display(int id){
  final AppWidgetHostView[] v={null};getInstrumentation().runOnMainSync(()->{
   v[0]=launcher.createView(c,id,manager.getAppWidgetInfo(id));v[0].setPadding(0,0,0,0);FrameLayout frame=new FrameLayout(c);frame.addView(v[0],new FrameLayout.LayoutParams(Ui.dp(activity,400),Ui.dp(activity,300),Gravity.CENTER));activity.setContentView(frame);
  });return v[0];
 }
 private interface Ready{boolean check();}
 private void await(String message,Ready r) throws Exception {
  long end=SystemClock.elapsedRealtime()+60000;while(SystemClock.elapsedRealtime()<end){final boolean[] ok={false};getInstrumentation().runOnMainSync(()->ok[0]=r.check());if(ok[0])return;Thread.sleep(125);}fail(message);
 }
 private TextView findText(View v,String prefix){
  if(v instanceof TextView&&((TextView)v).getText().toString().startsWith(prefix))return(TextView)v;
  if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView t=findText(((ViewGroup)v).getChildAt(i),prefix);if(t!=null)return t;}return null;
 }
 private ListView findList(View v){
  if(v instanceof ListView&&v.getId()!=R.id.indicators)return(ListView)v;
  if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){ListView t=findList(((ViewGroup)v).getChildAt(i));if(t!=null)return t;}return null;
 }
 public void testExternalContentsAndPendingIntent() throws Exception {
  int child=allocate("PlainWidget"),id=stack(child);AppWidgetHostView view=display(id);
  await("Original external widget content should render",()->findText(view,"External widget")!=null);
  resetEvents();
  getInstrumentation().runOnMainSync(()->findText(view,"External widget").performClick());
  await("Original widget PendingIntent should open external app",()->eventRow()==-1);
  pressBack();
 }
 public void testAutoUsesActualLauncherBoundsAndRetainsEdgeClicksAfterResize() throws Exception {
  int child=allocate("PlainWidget"),id=stack(child);
  getInstrumentation().runOnMainSync(()->{
   StackState state=new StackStore(c).get(id);state.showRail=false;new StackStore(c).put(state);
   Bundle reported=new Bundle();reported.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,200);reported.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,200);reported.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,150);reported.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,150);
   manager.updateAppWidgetOptions(id,reported);StackRenderer.updateSizes(c,id);StackRenderer.update(c,id);
  });
  AppWidgetHostView view=display(id);
  await("External provider should render",()->findText(view,"External widget")!=null);
  for(int[] dimensions:new int[][]{{400,300},{300,400},{400,200}}){
   getInstrumentation().runOnMainSync(()->view.setLayoutParams(new FrameLayout.LayoutParams(Ui.dp(activity,dimensions[0]),Ui.dp(activity,dimensions[1]),Gravity.CENTER)));
   await("Auto provider must fill actual launcher area despite smaller size options",()->{
    TextView provider=findText(view,"External widget");View area=view.findViewById(R.id.content);
    return provider!=null&&view.getWidth()==Ui.dp(activity,dimensions[0])&&view.getHeight()==Ui.dp(activity,dimensions[1])&&provider.getWidth()==area.getWidth()&&provider.getHeight()==area.getHeight()&&provider.getWidth()==view.getWidth()&&provider.getHeight()==view.getHeight();
   });
   final int[] xy=new int[2],wh=new int[2];getInstrumentation().runOnMainSync(()->{TextView provider=findText(view,"External widget");provider.getLocationOnScreen(xy);wh[0]=provider.getWidth();wh[1]=provider.getHeight();});
   getInstrumentation().waitForIdleSync();
   android.graphics.Bitmap image=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull("Screenshot must be available",image);
   int pixel=image.getPixel(xy[0]+wh[0]-4,xy[1]+wh[1]/2);
   java.io.File directory=c.getExternalFilesDir("fit-evidence");assertNotNull(directory);directory.mkdirs();
   try(java.io.FileOutputStream file=new java.io.FileOutputStream(new java.io.File(directory,"in-process-"+dimensions[0]+"x"+dimensions[1]+".png"))){assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,file));}
   image.recycle();
   assertEquals("Provider background must reach the visible right edge",0xffb9d9ec,pixel);
   resetEvents();long now=SystemClock.uptimeMillis();
   MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,xy[0]+wh[0]-4,xy[1]+wh[1]/2f,0),up=MotionEvent.obtain(now,now+80,MotionEvent.ACTION_UP,xy[0]+wh[0]-4,xy[1]+wh[1]/2f,0);
   down.setSource(InputDevice.SOURCE_TOUCHSCREEN);up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
   assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(down,true));assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(up,true));down.recycle();up.recycle();
   await("A tap at the displayed edge must open the external provider",()->eventRow()==-1);
   pressBack();getInstrumentation().waitForIdleSync();
   // MainActivity deliberately rebuilds its normal UI in onResume. Reattach
   // the test host after returning from the external provider activity.
   getInstrumentation().runOnMainSync(()->activity.setContentView((View)view.getParent()));
   await("Test host should regain focus",view::hasWindowFocus);
  }
 }
 public void testCalendarStyleCollectionScrollAndClick() throws Exception {
  int child=allocate("ListWidget"),id=stack(child);AppWidgetHostView view=display(id);
  await("External collection should contain forty events",()->findList(view)!=null&&findList(view).getCount()==40);
  await("Calendar list should be laid out and focused",()->findList(view).getWidth()>0&&findList(view).getHeight()>100&&view.hasWindowFocus());
  int[] xy=new int[2];final int[] wh=new int[2];
  getInstrumentation().runOnMainSync(()->{ListView l=findList(view);l.getLocationOnScreen(xy);wh[0]=l.getWidth();wh[1]=l.getHeight();});
  TouchUtils.drag(this,xy[0]+wh[0]/2f,xy[0]+wh[0]/2f,xy[1]+wh[1]-30,xy[1]+30,12);
  await("A finger swipe should scroll the collection",()->findList(view).getFirstVisiblePosition()>0);
  getInstrumentation().runOnMainSync(()->{findList(view).smoothScrollBy(0,0);findList(view).setSelection(30);});
  await("External collection should scroll",()->findList(view).getFirstVisiblePosition()>=29);
  await("Scrolled external rows should load",()->findText(view,"Calendar event 30")!=null);
  resetEvents();
  getInstrumentation().runOnMainSync(()->{ListView l=findList(view);int pos=l.getFirstVisiblePosition();l.performItemClick(l.getChildAt(0),pos,l.getItemIdAtPosition(pos));});
  await("Collection row PendingIntent should open external app with row",()->eventRow()>=29);
  pressBack();
 }
 public void testIndependentSelectionSizingAndPlacementDeletion() throws Exception {
  int a=allocate("PlainWidget"),b=allocate("PlainWidget"),first=stack(a,b),d=allocate("PlainWidget"),second=stack(d);
  getInstrumentation().runOnMainSync(()->new StackProvider().onReceive(c,new Intent(c,StackProvider.class).setAction(StackProvider.SELECT).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,first).putExtra("child",b)));
  assertEquals(b,new StackStore(c).get(first).selected);assertEquals(d,new StackStore(c).get(second).selected);
  assertEquals(364,manager.getAppWidgetOptions(b).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));
  StackState saved=new StackStore(c).get(first);saved.move(1,0);new StackStore(c).put(saved);assertEquals(b,new StackStore(c).get(first).selected);
  new StackStore(c).migrateLegacy();int savedId=new StackStore(c).definitionId(first);
  getInstrumentation().runOnMainSync(()->new StackProvider().onDeleted(c,new int[]{first}));
  assertNull(new StackStore(c).get(first));assertNotNull(new StackStore(c).get(savedId));assertNotNull(manager.getAppWidgetInfo(a));assertNotNull(manager.getAppWidgetInfo(b));assertNotNull(manager.getAppWidgetInfo(d));
 }
 public void testRailOpacityAndSelectionTap() throws Exception {
  int a=allocate("PlainWidget"),b=allocate("PlainWidget"),id=stack(a,b);AppWidgetHostView view=display(id);
  await("Rail should load indicators",()->view.findViewById(R.id.indicators) instanceof LinearLayout&&((LinearLayout)view.findViewById(R.id.indicators)).getChildCount()==2);
  assertEquals(.2f,view.findViewById(R.id.rail).getAlpha(),.001f);
  getInstrumentation().runOnMainSync(()->{LinearLayout l=view.findViewById(R.id.indicators);l.getChildAt(1).performClick();});
  await("Tapping rail should select second widget",()->new StackStore(c).get(id).selected==b);
 }
 public void testNavigationStripCanBeHiddenAndRestored() throws Exception {
  int child=allocate("PlainWidget"),id=stack(child);AppWidgetHostView view=display(id);
  await("Navigation strip should initially be visible",()->view.findViewById(R.id.rail)!=null&&view.findViewById(R.id.rail).getVisibility()==View.VISIBLE);
  getInstrumentation().runOnMainSync(()->{StackState s=new StackStore(c).get(id);s.showRail=false;new StackStore(c).put(s);StackRenderer.updateSizes(c,id);StackRenderer.update(c,id);});
  await("Hidden strip should leave the full stack area to the child",()->view.findViewById(R.id.rail).getVisibility()==View.GONE&&manager.getAppWidgetOptions(child).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)==400);
  getInstrumentation().runOnMainSync(()->{StackState s=new StackStore(c).get(id);s.showRail=true;new StackStore(c).put(s);StackRenderer.updateSizes(c,id);StackRenderer.update(c,id);});
  await("Restoring the strip should restore its width reservation",()->view.findViewById(R.id.rail).getVisibility()==View.VISIBLE&&manager.getAppWidgetOptions(child).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)==364);
 }
 public void testRailEndButtonsWrapAndDotsSurviveProviderRefresh() throws Exception {
  int a=allocate("PlainWidget"),b=allocate("PlainWidget"),id=stack(a,b);AppWidgetHostView view=display(id);
  await("Two dots should load",()->view.findViewById(R.id.indicators) instanceof LinearLayout&&((LinearLayout)view.findViewById(R.id.indicators)).getChildCount()==2);
  getInstrumentation().runOnMainSync(()->view.findViewById(R.id.previous).performClick());
  await("Previous should wrap to last widget",()->new StackStore(c).get(id).selected==b);
  getInstrumentation().runOnMainSync(()->view.findViewById(R.id.next).performClick());
  await("Next should wrap to first widget",()->new StackStore(c).get(id).selected==a);
  c.sendBroadcast(new Intent("com.kevin.widgetfixture.REFRESH").setComponent(new ComponentName("com.kevin.widgetfixture","com.kevin.widgetfixture.PlainWidget")));
  await("Provider should refresh its content",()->findText(view,"Updated external widget")!=null);
  getInstrumentation().runOnMainSync(()->((LinearLayout)view.findViewById(R.id.indicators)).getChildAt(1).performClick());
  await("Dots should still select after refresh",()->new StackStore(c).get(id).selected==b);
 }
 private void pressBack(){
  long now=SystemClock.uptimeMillis();
  for(int action:new int[]{KeyEvent.ACTION_DOWN,KeyEvent.ACTION_UP})assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(new KeyEvent(now,SystemClock.uptimeMillis(),action,KeyEvent.KEYCODE_BACK,0),true));
 }
 private static final Uri EVENTS=Uri.parse("content://com.kevin.widgetfixture.events");
 private void resetEvents(){c.getContentResolver().call(EVENTS,"reset",null,null);}
 private int eventRow(){return c.getContentResolver().call(EVENTS,"read",null,null).getInt("row",-999);}
 public void testNestedWeatherLayoutAndEditor() throws Exception {
  int child=allocate("NestedWidget"),id=stack(child);AppWidgetHostView view=display(id);
  await("Nested provider content should render",()->findText(view,"Nested weather")!=null);
  Intent i=StackRenderer.editIntent(c,id);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  Instrumentation.ActivityMonitor monitor=getInstrumentation().addMonitor(ConfigureActivity.class.getName(),null,false);
  c.startActivity(i);Activity editor=monitor.waitForActivityWithTimeout(20000);assertNotNull(editor);
  getInstrumentation().runOnMainSync(editor::finish);getInstrumentation().removeMonitor(monitor);
 }
 public void testPickerGroupsSizesAndPreview() throws Exception {
  Instrumentation.ActivityMonitor monitor=getInstrumentation().addMonitor(WidgetPickerActivity.class.getName(),null,false);
  c.startActivity(new Intent(c,WidgetPickerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  Activity picker=monitor.waitForActivityWithTimeout(20000);assertNotNull(picker);
  getInstrumentation().runOnMainSync(()->findSearch(picker.getWindow().getDecorView()).setText("Widget test fixture"));
  await("Picker groups should load",()->findText(picker.getWindow().getDecorView(),"Widget test fixture")!=null);
  // Perform group click on its list row, since the text itself does not own the listener.
  getInstrumentation().runOnMainSync(()->{findSearch(picker.getWindow().getDecorView()).setText("Widget test fixture");ExpandableListView l=findExpandable(picker.getWindow().getDecorView());for(int i=0;i<l.getExpandableListAdapter().getGroupCount();i++)l.expandGroup(i);});
  await("Picker should show sizes",()->findText(picker.getWindow().getDecorView(),"2 × 2")!=null);
  await("Picker should show provider preview layout",()->findText(picker.getWindow().getDecorView(),"External widget")!=null);
  getInstrumentation().runOnMainSync(picker::finish);getInstrumentation().removeMonitor(monitor);
 }
 private EditText findSearch(View v){if(v instanceof EditText)return(EditText)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){EditText e=findSearch(((ViewGroup)v).getChildAt(i));if(e!=null)return e;}return null;}
 private ExpandableListView findExpandable(View v){if(v instanceof ExpandableListView)return(ExpandableListView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){ExpandableListView l=findExpandable(((ViewGroup)v).getChildAt(i));if(l!=null)return l;}return null;}
 public void testAddingFromPreviewPickerReturnsToEditor() throws Exception {
  int id=stack();Instrumentation.ActivityMonitor editorMonitor=getInstrumentation().addMonitor(ConfigureActivity.class.getName(),null,false);
  c.startActivity(StackRenderer.editIntent(c,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Activity editor=editorMonitor.waitForActivityWithTimeout(20000);assertNotNull(editor);
  Instrumentation.ActivityMonitor pickerMonitor=getInstrumentation().addMonitor(WidgetPickerActivity.class.getName(),null,false);
  getInstrumentation().runOnMainSync(()->findText(editor.getWindow().getDecorView(),"Add widget").performClick());
  Activity picker=pickerMonitor.waitForActivityWithTimeout(20000);assertNotNull(picker);
  getInstrumentation().runOnMainSync(()->{findSearch(picker.getWindow().getDecorView()).setText("Widget test fixture");ExpandableListView l=findExpandable(picker.getWindow().getDecorView());for(int i=0;i<l.getExpandableListAdapter().getGroupCount();i++)l.expandGroup(i);});
  await("Nested weather card should appear",()->findText(picker.getWindow().getDecorView(),"Nested weather")!=null);
  getInstrumentation().runOnMainSync(()->{TextView label=findText(picker.getWindow().getDecorView(),"Nested weather");((View)label.getParent()).performClick();});
  await("Selected widget should be added",()->new StackStore(c).get(id)!=null&&new StackStore(c).get(id).children.size()==1);
  await("Editor should remain usable",()->findText(editor.getWindow().getDecorView(),"Done")!=null);
  getInstrumentation().runOnMainSync(()->findText(editor.getWindow().getDecorView(),"Done").performClick());
  getInstrumentation().removeMonitor(editorMonitor);getInstrumentation().removeMonitor(pickerMonitor);
 }
 public void testProviderConfigurationAcceptsAndReturnsToEditor() throws Exception {checkConfiguration(true);}
 public void testProviderConfigurationCancelReleasesWidget() throws Exception {checkConfiguration(false);}
 private void checkConfiguration(boolean accepted) throws Exception {
  resetEvents();int id=stack();Instrumentation.ActivityMonitor editorMonitor=getInstrumentation().addMonitor(ConfigureActivity.class.getName(),null,false);
  c.startActivity(StackRenderer.editIntent(c,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Activity editor=editorMonitor.waitForActivityWithTimeout(20000);assertNotNull(editor);
  Instrumentation.ActivityMonitor pickerMonitor=getInstrumentation().addMonitor(WidgetPickerActivity.class.getName(),null,false);
  getInstrumentation().runOnMainSync(()->findText(editor.getWindow().getDecorView(),"Add widget").performClick());
  Activity picker=pickerMonitor.waitForActivityWithTimeout(20000);assertNotNull(picker);
  getInstrumentation().runOnMainSync(()->{findSearch(picker.getWindow().getDecorView()).setText("Widget test fixture");ExpandableListView l=findExpandable(picker.getWindow().getDecorView());for(int i=0;i<l.getExpandableListAdapter().getGroupCount();i++)l.expandGroup(i);});
  await("Configurable widget card should appear",()->findText(picker.getWindow().getDecorView(),"Configurable widget")!=null);
  getInstrumentation().runOnMainSync(()->((View)findText(picker.getWindow().getDecorView(),"Configurable widget").getParent()).performClick());
  await("External provider setup must open",()->c.getContentResolver().call(EVENTS,"read",null,null).getInt("config_id",-1)>0);
  int child=c.getContentResolver().call(EVENTS,"read",null,null).getInt("config_id",-1);
  c.getContentResolver().call(EVENTS,accepted?"config_accept":"config_cancel",null,null);
  if(accepted)await("Configured widget should be added",()->new StackStore(c).get(id).children.contains(child));
  else{await("Canceled widget must be deleted",()->manager.getAppWidgetInfo(child)==null);assertTrue(new StackStore(c).get(id).children.isEmpty());}
  await("Editor should return after provider setup",()->editor.hasWindowFocus());
  getInstrumentation().runOnMainSync(()->findText(editor.getWindow().getDecorView(),"Done").performClick());
  getInstrumentation().removeMonitor(editorMonitor);getInstrumentation().removeMonitor(pickerMonitor);
 }
 public void testProviderUpdatesReachStack() throws Exception {
  int child=allocate("PlainWidget"),id=stack(child);AppWidgetHostView view=display(id);
  await("Initial provider update should arrive",()->findText(view,"External widget")!=null);
  c.sendBroadcast(new Intent("com.kevin.widgetfixture.REFRESH").setComponent(new ComponentName("com.kevin.widgetfixture","com.kevin.widgetfixture.PlainWidget")));
  await("Later provider update should arrive",()->findText(view,"Updated external widget")!=null);
 }
 @Override protected void tearDown() throws Exception {
  if(c!=null)getInstrumentation().runOnMainSync(()->{new StackProvider().onDeleted(c,outerIds.stream().mapToInt(Integer::intValue).toArray());for(int id:outerIds)launcher.deleteAppWidgetId(id);StackStore store=new StackStore(c);for(StackState s:store.saved())if(store.placements(s.id).isEmpty()){store.delete(s.id);for(int child:s.children)StackHost.get(c).deleteAppWidgetId(child);}launcher.stopListening();});
  super.tearDown();
 }
}

package com.kevin.widgetstack;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class,qualifiers="mdpi")
public class HomeRecoveryTest {
 Context c;StackStore store;StackState saved;
 @Before public void setup() throws Exception {
  c=RuntimeEnvironment.getApplication();store=new StackStore(c);
  org.robolectric.util.ReflectionHelpers.setStaticField(StackHost.class,"instance",null);
  bind(901,StackProvider.class.getName());bind(11,"com.kevin.widgetstack.TestProvider");bind(22,"com.kevin.widgetstack.TestProvider");
  saved=store.createSaved("Recovery test");saved.add(11);saved.add(22);store.put(saved);assertTrue(SavedStacks.place(c,901,saved.id));
 }
 void bind(int id,String cls) throws Exception {
  AppWidgetProviderInfo info=new AppWidgetProviderInfo();info.provider=new ComponentName(c,cls);info.label="Test widget";
  ActivityInfo activity=new ActivityInfo();activity.applicationInfo=c.getApplicationInfo();activity.packageName=c.getPackageName();activity.name=cls;activity.nonLocalizedLabel="Test widget";
  for(java.lang.reflect.Field f:AppWidgetProviderInfo.class.getDeclaredFields())if(f.getType()==ActivityInfo.class){f.setAccessible(true);f.set(info,activity);}
  shadowOf(AppWidgetManager.getInstance(c)).addInstalledProvider(info);shadowOf(AppWidgetManager.getInstance(c)).setAllowedToBindAppWidgets(true);
  assertTrue(AppWidgetManager.getInstance(c).bindAppWidgetIdIfAllowed(id,info.provider));
 }
 @After public void cleanup(){BarController.cancel(901);}
 TextView text(View v,String title){if(v instanceof TextView&&title.contentEquals(((TextView)v).getText()))return(TextView)v;if(v instanceof ViewGroup)for(int n=0;n<((ViewGroup)v).getChildCount();n++){TextView result=text(((ViewGroup)v).getChildAt(n),title);if(result!=null)return result;}return null;}
 Button button(View v,String title){if(v instanceof Button&&title.contentEquals(((Button)v).getText()))return(Button)v;if(v instanceof ViewGroup)for(int n=0;n<((ViewGroup)v).getChildCount();n++){Button result=button(((ViewGroup)v).getChildAt(n),title);if(result!=null)return result;}return null;}
 @Test public void appHasNoPinButtonAndDeleteWorksForPlacedStacks(){
  MainActivity main=Robolectric.buildActivity(MainActivity.class).setup().get();View page=main.getWindow().getDecorView();
  assertNull(text(page,"Add to home"));assertNotNull(text(page,"Delete"));text(page,"Delete").performClick();
  AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);dialog.getButton(-1).performClick();shadowOf(Looper.getMainLooper()).idle();
  assertNull(store.get(saved.id));assertNull(store.get(901));assertFalse(store.isPlacement(901));assertNull(text(main.getWindow().getDecorView(),"Recovery test"));
  assertNotNull(AppWidgetManager.getInstance(c).getAppWidgetInfo(901)); // launcher still owns its slot
 }
 @Test public void launcherEditOpensCurrentEditorAndOffersExplicitSwitch(){
  StackChooserActivity chooser=Robolectric.buildActivity(StackChooserActivity.class,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,901)).setup().get();
  var launch=shadowOf(chooser).getNextStartedActivityForResult();assertNotNull("Home-screen Edit must open the current stack editor",launch);
  assertEquals(ConfigureActivity.class.getName(),launch.intent.getComponent().getClassName());assertEquals(saved.id,launch.intent.getIntExtra(SavedStacks.EXTRA_SAVED_ID,-1));
  ConfigureActivity editor=Robolectric.buildActivity(ConfigureActivity.class,launch.intent).setup().get();assertNotNull(text(editor.getWindow().getDecorView(),"Customize navigation bar"));
  TextView replace=text(editor.getWindow().getDecorView(),"Use a different saved stack");assertNotNull(replace);replace.performClick();
  Intent select=shadowOf(editor).getNextStartedActivityForResult().intent;assertEquals(StackChooserActivity.class.getName(),select.getComponent().getClassName());
  StackChooserActivity explicit=Robolectric.buildActivity(StackChooserActivity.class,select).setup().get();assertNotNull(text(explicit.getWindow().getDecorView(),"Choose a saved stack"));
 }
 @Test public void reorderArrowsStayCompactAndRemainAccessible(){
  ConfigureActivity editor=Robolectric.buildActivity(ConfigureActivity.class,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,901)).setup().get();
  View page=editor.getWindow().getDecorView();Button up=button(page,"↑"),down=button(page,"↓"),remove=button(page,"Remove");
  assertNotNull(up);assertNotNull(down);assertNotNull(remove);assertNull(text(page,"Move up"));assertNull(text(page,"Move down"));
  assertEquals("Move up",up.getContentDescription());assertEquals("Move down",down.getContentDescription());
  assertEquals(1,up.getMaxLines());assertEquals(1,down.getMaxLines());assertEquals(1,remove.getMaxLines());
 }
 View layout(View root){root.measure(View.MeasureSpec.makeMeasureSpec(320,1073741824),View.MeasureSpec.makeMeasureSpec(240,1073741824));root.layout(0,0,320,240);return root;}
 Rect bounds(View root,View view){Rect r=new Rect(0,0,view.getWidth(),view.getHeight());((ViewGroup)root).offsetDescendantRectToMyCoords(view,r);return r;}
 @Test public void smallCornerVisualSurvivesTwentyEightDpLauncherRounding(){
  for(int edge=0;edge<4;edge++)for(int end=0;end<2;end++)for(int size:new int[]{4,8,12}){
   StackState s=store.get(901);s.autoContract=true;s.contractToCorner=true;s.railEdge=edge;s.cornerEnd=end;s.contractedWidth=size;store.put(s);
   View root=layout(StackRenderer.shell(c,s,0,true).apply(c,new FrameLayout(c)));Rect visual=bounds(root,root.findViewById(R.id.rail));
   boolean left=edge==StackState.LEFT||(s.horizontalRail()&&end==0),top=edge==StackState.TOP||(!s.horizontalRail()&&end==0);
   float x=left?visual.left:320-visual.right,y=top?visual.top:240-visual.bottom;
   assertTrue("The small square must stay inside a 28dp rounded corner",Math.pow(Math.max(0,28-x),2)+Math.pow(Math.max(0,28-y),2)<=28*28);
   assertEquals(size,visual.width());assertEquals(size,visual.height());
  }
 }
 @Test public void openingEditorResetsStaleExpansionWithoutChangingTheSelectedWidget(){
  StackState s=store.get(saved.id);s.autoContract=true;s.contractToCorner=true;store.put(s);store.selectOnHome(901,22);BarController.cancel(901);
  ConfigureActivity editor=Robolectric.buildActivity(ConfigureActivity.class,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,901)).setup().get();
  assertFalse("Opening home Edit recovers the bar without reselecting the stack",BarController.contracted(store.get(901)));assertEquals(22,store.get(901).selected);
  text(editor.getWindow().getDecorView(),"Done").performClick();assertEquals(Activity.RESULT_OK,shadowOf(editor).getResultCode());assertEquals(22,store.get(901).selected);assertEquals(11,store.get(saved.id).selected);
 }
 @Test public void cancelDeleteKeepsTheSavedStackAndItsPlacements(){
  MainActivity main=Robolectric.buildActivity(MainActivity.class).setup().get();text(main.getWindow().getDecorView(),"Delete").performClick();
  ShadowAlertDialog.getLatestAlertDialog().getButton(-2).performClick();shadowOf(Looper.getMainLooper()).idle();
  assertNotNull(store.get(saved.id));assertEquals(saved.id,store.definitionId(901));
 }
 @Test public void deleteRetainsAnUnrelatedStackAndResetsEveryDeletedCopy() throws Exception {
  bind(902,StackProvider.class.getName());assertTrue(SavedStacks.place(c,902,saved.id));
  StackState other=store.createSaved("Retained stack");store.put(other);
  assertTrue(SavedStacks.delete(c,saved.id));assertNull(store.get(901));assertNull(store.get(902));assertNotNull(store.get(other.id));
  assertTrue(store.placements(saved.id).isEmpty());assertTrue(store.saved().size()==1);assertFalse(SavedStacks.delete(c,saved.id));
 }
 @Test public void refreshRecoversTheContractedBarWithoutResettingSelection(){
  StackState state=store.get(saved.id);state.autoContract=true;state.contractToCorner=true;store.put(state);store.selectOnHome(901,22);BarController.cancel(901);
  MainActivity main=Robolectric.buildActivity(MainActivity.class).setup().get();text(main.getWindow().getDecorView(),"Refresh widgets").performClick();
  assertFalse(BarController.contracted(store.get(901)));assertEquals(22,store.get(901).selected);
 }
 @Test public void largeCornerSquaresKeepTheirConfiguredSizeAndTarget(){
  for(int edge=0;edge<4;edge++)for(int end=0;end<2;end++)for(int size:new int[]{48,60,79}){
   StackState state=store.get(901);state.autoContract=true;state.contractToCorner=true;state.railEdge=edge;state.cornerEnd=end;state.railWidth=80;state.contractedWidth=size;
   View root=layout(StackRenderer.shell(c,state,0,true).apply(c,new FrameLayout(c)));
   View target=root.findViewById(R.id.corner_touch),visual=root.findViewById(R.id.rail);
   assertEquals(size,target.getWidth());assertEquals(size,target.getHeight());assertEquals(size,visual.getWidth());assertEquals(size,visual.getHeight());
  }
 }

}

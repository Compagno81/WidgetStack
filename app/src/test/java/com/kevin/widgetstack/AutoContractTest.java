package com.kevin.widgetstack;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.time.Duration;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class,qualifiers="mdpi")
public class AutoContractTest {
 private Context c(){return RuntimeEnvironment.getApplication();}
 private StackState state(int id){StackState s=new StackState(id);s.add(10);s.add(20);s.autoContract=true;s.railWidth=36;s.contractedWidth=6;new StackStore(c()).put(s);return s;}
 @After public void clean(){for(int id:new int[]{901,902})BarController.cancel(id);}
 private void advance(long ms){shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms));}
 @Test public void contractsAtTwoSecondsAndASecondTapResetsTheDeadline(){
  StackState s=state(901);assertTrue(BarController.contracted(s));BarController.expand(c(),s.id);assertEquals(36,BarController.width(s));advance(1999);assertFalse(BarController.contracted(s));BarController.expand(c(),s.id);advance(1);assertFalse(BarController.contracted(s));advance(1999);assertFalse(BarController.contracted(s));assertTrue(BarController.animating(s.id));advance(240);assertTrue(BarController.contracted(s));assertEquals(6,BarController.width(s));
 }
 @Test public void stacksHaveIndependentTimersAndOrdinaryRenderingDoesNotExtendThem(){
  StackState a=state(901),b=state(902);BarController.expand(c(),a.id);advance(1000);BarController.expand(c(),b.id);StackRenderer.shell(c(),a);StackRenderer.childOptions(new Bundle(),a);advance(1240);assertTrue(BarController.contracted(a));assertFalse(BarController.contracted(b));advance(1000);assertTrue(BarController.contracted(b));
 }
 @Test public void disabledOrHiddenBarsDoNotAutoContractAndDeletedSettingsCancelTheTimer(){
  StackState s=state(901);s.autoContract=false;new StackStore(c()).put(s);BarController.expand(c(),s.id);advance(2000);assertFalse(BarController.contracted(s));
  s.autoContract=true;s.showRail=false;new StackStore(c()).put(s);BarController.expand(c(),s.id);assertFalse(BarController.contracted(s));s.showRail=true;new StackStore(c()).put(s);BarController.expand(c(),s.id);new StackStore(c()).delete(s.id);BarController.cancel(s.id);advance(2000);assertNull(new StackStore(c()).get(s.id));
 }
 @Test public void contractedShellUsesConfiguredThicknessAndOnlyExpandsWithoutSelecting(){
  for(int edge=0;edge<4;edge++)for(boolean overlay:new boolean[]{false,true}){
   StackState s=state(901);s.railEdge=edge;s.overlapBar=overlay;new StackStore(c()).put(s);View v=StackRenderer.shell(c(),s).apply(c(),new FrameLayout(c()));v.measure(View.MeasureSpec.makeMeasureSpec(320,1073741824),View.MeasureSpec.makeMeasureSpec(240,1073741824));v.layout(0,0,320,240);
   assertEquals(6,s.horizontalRail()?v.findViewById(R.id.rail).getHeight():v.findViewById(R.id.rail).getWidth());assertNull(v.findViewById(R.id.next));int before=shadowOf((Application)c()).getBroadcastIntents().size();v.findViewById(R.id.rail).performClick();Intent sent=shadowOf((Application)c()).getBroadcastIntents().get(before);assertEquals(StackProvider.EXPAND,sent.getAction());assertEquals(s.id,sent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1));assertEquals(10,new StackStore(c()).get(s.id).selected);
   Bundle size=new Bundle();size.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,320);size.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,240);Bundle child=StackRenderer.childOptions(size,s);assertEquals(overlay||s.horizontalRail()?320:314,child.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));assertEquals(overlay||!s.horizontalRail()?240:234,child.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
  }
 }
 private <T extends View>T find(View v,Class<T> type,String name){if(type.isInstance(v)&&name.equals(String.valueOf(v.getContentDescription())))return type.cast(v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){T r=find(((ViewGroup)v).getChildAt(i),type,name);if(r!=null)return r;}return null;}
 @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
 @Test public void groupedEditorShowsDependentSizeAndCommitsOrCancelsTheWholeDraft() throws Exception {
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();StackState s=new StackState(901);BarEditor.show(a,s,()->{});AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();View decor=d.getWindow().getDecorView();RadioGroup tabs=find(decor,RadioGroup.class,"Settings section");assertEquals(3,tabs.getChildCount());assertEquals(View.GONE,find(decor,LinearLayout.class,"Contracted size settings").getVisibility());((RadioButton)tabs.getChildAt(1)).setChecked(true);find(decor,Switch.class,"Auto-contract").setChecked(true);assertEquals(View.VISIBLE,find(decor,LinearLayout.class,"Contracted size settings").getVisibility());find(decor,SeekBar.class,"Contracted bar thickness").setProgress(2);
  decor.measure(View.MeasureSpec.makeMeasureSpec(360,1073741824),View.MeasureSpec.makeMeasureSpec(740,1073741824));decor.layout(0,0,360,740);android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(360,740,android.graphics.Bitmap.Config.ARGB_8888);decor.draw(new android.graphics.Canvas(bitmap));java.io.File image=new java.io.File("build/reports/behaviour-api"+Build.VERSION.SDK_INT+".png");image.getParentFile().mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(image)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
  assertFalse(s.autoContract);d.getButton(-1).performClick();shadowOf(Looper.getMainLooper()).idle();assertTrue(s.autoContract);assertEquals(6,s.contractedWidth);new StackStore(c()).put(s);assertTrue(new StackStore(c()).get(s.id).autoContract);
  BarEditor.show(a,s,()->{});d=ShadowAlertDialog.getLatestAlertDialog();decor=d.getWindow().getDecorView();find(decor,Switch.class,"Auto-contract").setChecked(false);d.getButton(-2).performClick();shadowOf(Looper.getMainLooper()).idle();assertTrue(s.autoContract);
 }
 @Test public void contractedWidthStaysSmallerThanExpandedAndLegacyPreferencesDefaultOff(){
  StackState s=new StackState(901);s.railWidth=8;s.contractedWidth=80;s.normalize();assertEquals(7,s.contractedWidth);s.contractedWidth=0;s.normalize();assertEquals(4,s.contractedWidth);
  c().getSharedPreferences("stacks",0).edit().putString("stack_902","{\"railWidth\":36}").commit();assertFalse(new StackStore(c()).get(902).autoContract);assertEquals(8,new StackStore(c()).get(902).contractedWidth);
 }
}

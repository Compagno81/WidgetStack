package com.kevin.widgetstack;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.view.animation.*;
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
public class BarMotionTest {
 private Context c(){return RuntimeEnvironment.getApplication();}
 private StackState state(){StackState s=new StackState(903);s.autoContract=true;s.contractedWidth=6;s.add(10);s.add(20);return s;}
 private void save(StackState s){new StackStore(c()).put(s);}
 private void advance(long ms){shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms));}
 @After public void clean(){BarController.cancel(903);}
 @Test public void customDelayStartsContractionAtTheConfiguredSecondThenSettles(){
  StackState s=state();s.contractDelaySeconds=5;save(s);assertTrue(BarController.expand(c(),s.id));advance(240);assertFalse(BarController.animating(s.id));advance(4759);assertFalse(BarController.contracted(s));assertFalse(BarController.animating(s.id));advance(1);assertTrue(BarController.animating(s.id));assertEquals(36,BarController.width(s));advance(239);assertFalse(BarController.contracted(s));advance(1);assertTrue(BarController.contracted(s));assertEquals(6,BarController.width(s));
 }
 @Test public void navigationResetsDelayAndTapDuringContractionCancelsOldCompletion(){
  StackState s=state();save(s);BarController.expand(c(),s.id);advance(1000);assertFalse("Already expanded taps must not repeat expansion",BarController.expand(c(),s.id));advance(1999);assertFalse(BarController.animating(s.id));advance(1);assertTrue(BarController.animating(s.id));advance(120);assertTrue(BarController.expand(c(),s.id));advance(120);assertFalse(BarController.contracted(s));advance(120);assertFalse(BarController.animating(s.id));advance(1760);assertTrue(BarController.animating(s.id));advance(240);assertTrue(BarController.contracted(s));
 }
 @Test public void delayPersistsDefaultsToTwoAndUsesLongArithmetic(){
  StackState s=state();s.contractDelaySeconds=17;save(s);assertEquals(17,new StackStore(c()).get(s.id).contractDelaySeconds);
  c().getSharedPreferences("stacks",0).edit().putString("stack_903","{\"autoContract\":true}").commit();assertEquals(2,new StackStore(c()).get(s.id).contractDelaySeconds);
  s.contractDelaySeconds=-1;s.normalize();assertEquals(2,s.contractDelaySeconds);s.contractDelaySeconds=Integer.MAX_VALUE;assertEquals(2147483647000L,s.contractDelayMillis());
 }
 @Config(shadows=BarNavigationTest.FrameworkViewAnimator.class)
 @Test public void transitionsAnimateOnlyTheBarAndAnchorAtEveryEdgeInBothLayoutModes(){
  for(int edge=0;edge<4;edge++)for(boolean overlap:new boolean[]{false,true})for(int motion:new int[]{-1,1}){
   StackState s=state();s.railEdge=edge;s.overlapBar=overlap;
   View v=StackRenderer.shell(c(),s,0,false,motion).apply(c(),new FrameLayout(c()));v.measure(View.MeasureSpec.makeMeasureSpec(320,1073741824),View.MeasureSpec.makeMeasureSpec(240,1073741824));v.layout(0,0,320,240);
   View frame=v.findViewById(R.id.rail_frame),strip=v.findViewById(R.id.compact_rail);ViewFlipper flipper=v.findViewById(R.id.bar_transition);
   assertEquals(36,s.horizontalRail()?frame.getHeight():frame.getWidth());assertEquals(6,s.horizontalRail()?strip.getHeight():strip.getWidth());assertEquals(.2f,frame.getAlpha(),.001f);assertEquals(1,flipper.getDisplayedChild());assertFalse(flipper.isAutoStart());assertNull(v.findViewById(R.id.content).getAnimation());
   Animation animation=motion>0?flipper.getInAnimation():flipper.getOutAnimation();assertNotNull(animation);assertEquals(240,animation.getDuration());
   assertNotNull(motion>0?flipper.getCurrentView().getAnimation():flipper.getChildAt(0).getAnimation());
   animation.initialize(frame.getWidth(),frame.getHeight(),frame.getWidth(),frame.getHeight());animation.setStartTime(1000);
   Transformation t=new Transformation();animation.getTransformation(1120,t);float[] matrix=new float[9];t.getMatrix().getValues(matrix);float shift=s.horizontalRail()?matrix[android.graphics.Matrix.MTRANS_Y]:matrix[android.graphics.Matrix.MTRANS_X];assertEquals(edge==StackState.TOP||edge==StackState.LEFT?-18f:18f,shift,.1f);
   int before=shadowOf((Application)c()).getBroadcastIntents().size();assertTrue(strip.performClick());assertEquals(StackProvider.EXPAND,shadowOf((Application)c()).getBroadcastIntents().get(before).getAction());
   assertEquals(overlap?320:s.horizontalRail()?320:284,v.findViewById(R.id.widget_area).getWidth());assertEquals(overlap?240:s.horizontalRail()?204:240,v.findViewById(R.id.widget_area).getHeight());
  }
 }
 private <T extends View>T find(View v,Class<T> type,String label){if(type.isInstance(v)&&label.equals(String.valueOf(v.getContentDescription())))return type.cast(v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){T r=find(((ViewGroup)v).getChildAt(i),type,label);if(r!=null)return r;}return null;}
 @Test public void editorRejectsInvalidSecondsWithoutClosingAndCancelPreservesDelay(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();StackState s=state();int[] saves={0};BarEditor.show(a,s,()->saves[0]++);AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();EditText delay=find(d.getWindow().getDecorView(),EditText.class,"Auto-contract delay in seconds");
  for(String bad:new String[]{"1","","999999999999999"}){delay.setText(bad);d.getButton(-1).performClick();assertTrue(d.isShowing());assertNotNull(delay.getError());assertEquals(0,saves[0]);assertEquals(2,s.contractDelaySeconds);}
  delay.setText("7");d.getButton(-1).performClick();assertFalse(d.isShowing());assertEquals(1,saves[0]);assertEquals(7,s.contractDelaySeconds);save(s);assertEquals(7,new StackStore(c()).get(s.id).contractDelaySeconds);
  BarEditor.show(a,s,()->saves[0]++);d=ShadowAlertDialog.getLatestAlertDialog();find(d.getWindow().getDecorView(),EditText.class,"Auto-contract delay in seconds").setText("20");d.getButton(-2).performClick();advance(0);assertEquals(7,s.contractDelaySeconds);assertEquals(1,saves[0]);
 }
 @Test public void previewUsesCustomDelayAndTheProductionExpansionLayout(){
  StackState s=state();s.contractDelaySeconds=4;StackPreview p=new StackPreview(c(),s);
  var activity=Robolectric.buildActivity(Activity.class).create().start().resume();activity.get().setContentView(p);activity.visible();
  assertTrue(p.contracted);p.demonstrateExpansion();
  assertNotNull(p.getChildAt(0).findViewById(R.id.bar_transition));advance(240);
  assertNull(p.getChildAt(0).findViewById(R.id.bar_transition));
  // Native frame rendering may advance Robolectric's clock. Exact boundaries are
  // covered by the controller tests; this checks that the preview uses four seconds.
  advance(2100);assertFalse("A custom four-second delay must remain expanded past two seconds",p.contracted);
  advance(2000);assertTrue(p.contracted);assertNull(p.getChildAt(0).findViewById(R.id.bar_transition));
 }
}

package com.kevin.widgetstack;

import android.app.Activity;
import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.SizeF;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowPendingIntent;
import org.robolectric.shadows.ShadowView;
import java.util.ArrayList;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class,qualifiers="mdpi")
public class BarNavigationTest {
 private Context context(){return RuntimeEnvironment.getApplication();}
 private StackState state(){StackState s=new StackState(987);s.add(10);s.add(20);s.add(30);s.select(20);return s;}
 private View apply(StackState s,int direction){
  View v=StackRenderer.shell(context(),s,direction).apply(context(),new FrameLayout(context()));
  v.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(240,View.MeasureSpec.EXACTLY));v.layout(0,0,320,240);return v;
 }
 @Test public void allEdgesReserveThicknessAndKeepQuarterEndButtons(){
  for(int edge=0;edge<4;edge++){
   StackState s=state();s.railEdge=edge;s.railWidth=48;s.railOpacity=65;
   View v=apply(s,0),rail=v.findViewById(R.id.rail),area=v.findViewById(R.id.widget_area);
   assertEquals(.65f,rail.getAlpha(),.001f);
   assertEquals(48,s.horizontalRail()?rail.getHeight():rail.getWidth());
   assertEquals(s.horizontalRail()?192:272,s.horizontalRail()?area.getHeight():area.getWidth());
   int length=s.horizontalRail()?rail.getWidth():rail.getHeight();
   for(int id:new int[]{R.id.previous,R.id.next})assertEquals(length/4,s.horizontalRail()?v.findViewById(id).getWidth():v.findViewById(id).getHeight());
   if(edge==StackState.TOP)assertEquals(0,rail.getTop());
   if(edge==StackState.BOTTOM)assertEquals(240,rail.getBottom());
   if(edge==StackState.LEFT)assertEquals(0,rail.getLeft());
   if(edge==StackState.RIGHT)assertEquals(320,rail.getRight());
  }
 }
 @Test public void hiddenBarRestoresFullContentAndSizes(){
  StackState s=state();s.showRail=false;
  for(int edge=0;edge<4;edge++){
   s.railEdge=edge;View v=apply(s,0);assertEquals(View.GONE,v.findViewById(R.id.rail).getVisibility());
   assertEquals(320,v.findViewById(R.id.widget_area).getWidth());assertEquals(240,v.findViewById(R.id.widget_area).getHeight());
   Bundle b=StackRenderer.childOptions(options(),s);assertEquals(320,b.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));assertEquals(240,b.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
  }
 }
 private Bundle options(){Bundle b=new Bundle();b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,320);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,400);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,240);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,300);ArrayList<SizeF> sizes=new ArrayList<>();sizes.add(new SizeF(320,240));b.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,sizes);return b;}
 @Test public void resizeOptionsSubtractOnlyOccupiedAxisIncludingResponsiveSizes(){
  for(int edge=0;edge<4;edge++){
   StackState s=state();s.railEdge=edge;s.railWidth=48;Bundle b=StackRenderer.childOptions(options(),s);
   int w=s.horizontalRail()?320:272,h=s.horizontalRail()?192:240;
   assertEquals(w,b.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));assertEquals(h,b.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
   assertEquals(new SizeF(w,h),b.<SizeF>getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES).get(0));
   assertEquals(320,options().getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));
  }
 }
 @Test public void everyDotAndEndButtonHasItsOwnCorrectNavigationIntent(){
  StackState s=state();
  for(int edge=0;edge<4;edge++){
   s.railEdge=edge;View v=apply(s,0);LinearLayout dots=v.findViewById(R.id.indicators);assertEquals(3,dots.getChildCount());
   for(int i=0;i<3;i++){
    int before=shadowOf((Application)context()).getBroadcastIntents().size();assertTrue(dots.getChildAt(i).performClick());
    Intent sent=shadowOf((Application)context()).getBroadcastIntents().get(before);assertEquals((int)s.children.get(i),sent.getIntExtra("child",-1));assertEquals(s.id,sent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1));
   }
   for(int id:new int[]{R.id.previous,R.id.next}){
    int before=shadowOf((Application)context()).getBroadcastIntents().size();v.findViewById(id).performClick();Intent sent=shadowOf((Application)context()).getBroadcastIntents().get(before);assertEquals(id==R.id.previous?-1:1,sent.getIntExtra("direction",0));
   }
  }
 }
 // The built-in Robolectric ViewAnimator shadow changes visibility without starting animations.
 // Use Android's original ViewAnimator methods for this animation contract test.
 @org.robolectric.annotation.Implements(android.widget.ViewAnimator.class)
 public static class FrameworkViewAnimator extends org.robolectric.shadows.ShadowViewAnimator {
  @org.robolectric.annotation.RealObject private ViewAnimator real;
  @org.robolectric.annotation.Implementation protected void setDisplayedChild(int index){org.robolectric.shadow.api.Shadow.directlyOn(real,ViewAnimator.class,"setDisplayedChild",org.robolectric.util.ReflectionHelpers.ClassParameter.from(int.class,index));}
  @org.robolectric.annotation.Implementation protected int getDisplayedChild(){return org.robolectric.shadow.api.Shadow.directlyOn(real,ViewAnimator.class,"getDisplayedChild");}
  @org.robolectric.annotation.Implementation protected View getCurrentView(){return org.robolectric.shadow.api.Shadow.directlyOn(real,ViewAnimator.class,"getCurrentView");}
 }
 @Config(shadows=FrameworkViewAnimator.class)
 @Test public void animationLayoutsApplyBothDirectionsAndLeaveWidgetContentInteractive(){
  StackState s=state();s.animateSwitch=true;
  for(int edge=0;edge<4;edge++)for(int direction:new int[]{-1,1}){
   s.railEdge=edge;View v=apply(s,direction);ViewFlipper flipper=v.findViewById(R.id.transition);assertNotNull(flipper);assertEquals(1,flipper.getDisplayedChild());assertNotNull(flipper.getInAnimation());assertNotNull("Switch should start an incoming animation",flipper.getCurrentView().getAnimation());assertFalse(flipper.isAutoStart());assertNotNull(v.findViewById(R.id.content));
  }
  assertNull(apply(s,0).findViewById(R.id.transition));s.animateSwitch=false;assertNull(apply(s,1).findViewById(R.id.transition));
 }
 @Test public void storePersistsSettingsAndLoadsOldStacksWithDefaults(){
  Context c=context();StackStore store=new StackStore(c);StackState s=state();s.railEdge=0;s.railWidth=64;s.railOpacity=72;s.animateSwitch=true;s.buttonsOnly=true;store.put(s);StackState read=store.get(s.id);
  assertEquals(0,read.railEdge);assertEquals(64,read.railWidth);assertEquals(72,read.railOpacity);assertTrue(read.animateSwitch);assertTrue(read.buttonsOnly);assertEquals(s.children,read.children);
  c.getSharedPreferences("stacks",0).edit().putString("stack_988","{\"children\":[10],\"selected\":10}").commit();read=store.get(988);assertEquals(StackState.RIGHT,read.railEdge);assertEquals(36,read.railWidth);assertEquals(20,read.railOpacity);assertFalse(read.animateSwitch);assertFalse(read.buttonsOnly);assertFalse(read.overlapBar);
 }
 @Test public void compactButtonsOnlyBarUsesTwoEqualAreasOnEveryEdge(){
  for(int edge=0;edge<4;edge++){
   StackState s=state();s.railEdge=edge;s.railWidth=8;s.buttonsOnly=true;View v=apply(s,0);
   v.measure(View.MeasureSpec.makeMeasureSpec(100,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(64,View.MeasureSpec.EXACTLY));v.layout(0,0,100,64);
   View rail=v.findViewById(R.id.rail);assertEquals(8,s.horizontalRail()?rail.getHeight():rail.getWidth());assertEquals(View.GONE,v.findViewById(R.id.rail_middle).getVisibility());
   assertEquals(0,((LinearLayout)v.findViewById(R.id.indicators)).getChildCount());
   for(int id:new int[]{R.id.previous,R.id.next}){
    View button=v.findViewById(id);assertEquals((s.horizontalRail()?100:64)/2,s.horizontalRail()?button.getWidth():button.getHeight());assertTrue(((TextView)button).getTextSize()<=8);
   }
  }
 }
 @Test public void switchingBarModeRestoresDotsWhenRemoteViewsAreReapplied(){
  StackState s=state();View v=apply(s,0);assertEquals(3,((LinearLayout)v.findViewById(R.id.indicators)).getChildCount());
  s.buttonsOnly=true;StackRenderer.shell(context(),s).reapply(context(),v);assertEquals(View.GONE,v.findViewById(R.id.rail_middle).getVisibility());assertEquals(0,((LinearLayout)v.findViewById(R.id.indicators)).getChildCount());
  s.buttonsOnly=false;StackRenderer.shell(context(),s).reapply(context(),v);assertEquals(View.VISIBLE,v.findViewById(R.id.rail_middle).getVisibility());assertEquals(3,((LinearLayout)v.findViewById(R.id.indicators)).getChildCount());
 }
 @Test public void buttonsOnlyNavigationKeepsTheStackIdentityAndDirection(){
  StackState s=state();s.buttonsOnly=true;View v=apply(s,0);
  for(int id:new int[]{R.id.previous,R.id.next}){
   int before=shadowOf((Application)context()).getBroadcastIntents().size();assertTrue(v.findViewById(id).performClick());Intent sent=shadowOf((Application)context()).getBroadcastIntents().get(before);
   assertEquals(s.id,sent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1));assertEquals(id==R.id.previous?-1:1,sent.getIntExtra("direction",0));
  }
 }
 @Test public void minimumThicknessAndButtonsOnlyCanBeAppliedAndPersisted(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();StackState s=state();BarEditor.show(a,s,()->{});
  android.app.AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();View decor=dialog.getWindow().getDecorView();find(decor,SeekBar.class,"Bar thickness").setProgress(0);find(decor,Switch.class,"Buttons only").setChecked(true);find(decor,Switch.class,"Overlap widget").setChecked(true);
  assertEquals(36,s.railWidth);assertFalse(s.buttonsOnly);dialog.getButton(-1).performClick();shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(8,s.railWidth);assertTrue(s.buttonsOnly);
  StackStore store=new StackStore(context());store.put(s);assertEquals(8,store.get(s.id).railWidth);assertTrue(store.get(s.id).buttonsOnly);
 }
 private <T extends View>T find(View v,Class<T> type,String description){if(type.isInstance(v)&&(description==null||description.equals(String.valueOf(v.getContentDescription()))))return type.cast(v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){T result=find(((ViewGroup)v).getChildAt(i),type,description);if(result!=null)return result;}return null;}
 @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
 @Test public void visualEditorAppliesTogetherAndCancelLeavesSettingsAlone() throws Exception {
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();StackState s=state();final boolean[] saved={false};BarEditor.show(a,s,()->saved[0]=true);
  android.app.AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();View decor=dialog.getWindow().getDecorView();
  find(decor,SeekBar.class,"Bar thickness").setProgress(72);find(decor,SeekBar.class,"Bar opacity").setProgress(75);find(decor,Switch.class,"Slide animation").setChecked(true);find(decor,Switch.class,"Buttons only").setChecked(true);find(decor,Switch.class,"Overlap widget").setChecked(true);
  RadioGroup edges=find(decor,RadioGroup.class,"Bar position");((RadioButton)edges.getChildAt(0)).setChecked(true);
  decor.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(740,View.MeasureSpec.EXACTLY));decor.layout(0,0,360,740);
  android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(360,740,android.graphics.Bitmap.Config.ARGB_8888);decor.draw(new android.graphics.Canvas(image));
  java.io.File file=new java.io.File("build/reports/bar-editor-api"+android.os.Build.VERSION.SDK_INT+".png");file.getParentFile().mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
  assertEquals(36,s.railWidth);assertFalse(saved[0]);dialog.getButton(-1).performClick();shadowOf(android.os.Looper.getMainLooper()).idle();
  assertTrue(saved[0]);assertEquals(80,s.railWidth);assertEquals(75,s.railOpacity);assertEquals(StackState.TOP,s.railEdge);assertTrue(s.animateSwitch);assertTrue(s.buttonsOnly);assertTrue(s.overlapBar);
  BarEditor.show(a,s,()->saved[0]=false);dialog=ShadowAlertDialog.getLatestAlertDialog();find(dialog.getWindow().getDecorView(),SeekBar.class,"Bar opacity").setProgress(0);dialog.getButton(-2).performClick();shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(75,s.railOpacity);assertTrue(saved[0]);
 }
}

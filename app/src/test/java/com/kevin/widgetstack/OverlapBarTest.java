package com.kevin.widgetstack;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.os.*;
import android.util.SizeF;
import android.view.*;
import android.widget.*;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class,qualifiers="mdpi")
public class OverlapBarTest {
 private Context context(){return RuntimeEnvironment.getApplication();}
 private StackState state(){StackState s=new StackState(987);s.add(10);s.add(20);s.overlapBar=true;return s;}
 private void layout(View v){v.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(240,View.MeasureSpec.EXACTLY));v.layout(0,0,320,240);}
 private View apply(StackState s,int direction){View v=StackRenderer.shell(context(),s,direction).apply(context(),new FrameLayout(context()));layout(v);return v;}
 @Test public void overlapUsesFullWidgetAreaAndDrawsBarAboveItOnEveryEdge(){
  for(int edge=0;edge<4;edge++)for(boolean buttons:new boolean[]{false,true})for(int width:new int[]{8,48}){
   StackState s=state();s.railEdge=edge;s.railWidth=width;s.buttonsOnly=buttons;View v=apply(s,0),rail=v.findViewById(R.id.rail),area=v.findViewById(R.id.widget_area);
   assertTrue(v instanceof FrameLayout);assertEquals(320,area.getWidth());assertEquals(240,area.getHeight());assertEquals(0,area.getLeft());assertEquals(0,area.getTop());
   assertSame(rail,((ViewGroup)v).getChildAt(((ViewGroup)v).getChildCount()-1));assertTrue(rail.isClickable());assertFalse(v.isClickable());
   assertEquals(width,s.horizontalRail()?rail.getHeight():rail.getWidth());assertEquals(.2f,rail.getAlpha(),.001f);
   if(edge==StackState.TOP)assertEquals(0,rail.getTop());if(edge==StackState.BOTTOM)assertEquals(240,rail.getBottom());if(edge==StackState.LEFT)assertEquals(0,rail.getLeft());if(edge==StackState.RIGHT)assertEquals(320,rail.getRight());
   s.showRail=false;v=apply(s,0);assertEquals(View.GONE,v.findViewById(R.id.rail).getVisibility());assertEquals(320,v.findViewById(R.id.widget_area).getWidth());assertEquals(240,v.findViewById(R.id.widget_area).getHeight());
  }
 }
 @Test public void overlapPreservesAllHostedWidgetDimensionsAndResponsiveSizes(){
  Bundle outer=new Bundle();outer.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,320);outer.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,400);outer.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,240);outer.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,300);
  ArrayList<SizeF> sizes=new ArrayList<>();sizes.add(new SizeF(320,240));sizes.add(new SizeF(400,300));outer.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,sizes);
  for(int edge=0;edge<4;edge++){
   StackState s=state();s.railEdge=edge;s.railWidth=48;Bundle options=StackRenderer.childOptions(outer,s);
   for(String key:new String[]{AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT})assertEquals(outer.getInt(key),options.getInt(key));
   assertEquals(sizes,options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES));
   s.overlapBar=false;options=StackRenderer.childOptions(outer,s);assertEquals(s.horizontalRail()?320:272,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));assertEquals(s.horizontalRail()?192:240,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
  }
 }
 private void touch(View v,int action,float x,float y){MotionEvent e=MotionEvent.obtain(0,SystemClock.uptimeMillis(),action,x,y,0);assertTrue(v.dispatchTouchEvent(e));e.recycle();if(action==MotionEvent.ACTION_UP)shadowOf(Looper.getMainLooper()).idle();}
 @Test public void onlyTheCoveredEdgeConsumesTouchesAndArrowsKeepTheirNavigationIntents(){
  Activity host=Robolectric.buildActivity(Activity.class).setup().get();
  for(int edge=0;edge<4;edge++)for(boolean buttons:new boolean[]{false,true}){
   StackState s=state();s.railEdge=edge;s.buttonsOnly=buttons;s.railWidth=48;View v=apply(s,0);ViewGroup content=v.findViewById(R.id.content);content.removeAllViews();Button widget=new Button(context());final int[] clicks={0},moves={0};widget.setOnClickListener(w->clicks[0]++);widget.setOnTouchListener((w,event)->{if(event.getAction()==MotionEvent.ACTION_MOVE)moves[0]++;return false;});content.addView(widget,new FrameLayout.LayoutParams(-1,-1));host.setContentView(v);layout(v);
   int before=shadowOf((Application)context()).getBroadcastIntents().size();touch(v,MotionEvent.ACTION_DOWN,160,120);touch(v,MotionEvent.ACTION_MOVE,160,125);touch(v,MotionEvent.ACTION_UP,160,125);assertEquals(1,clicks[0]);assertTrue(moves[0]>0);assertEquals(before,shadowOf((Application)context()).getBroadcastIntents().size());
   View rail=v.findViewById(R.id.rail);
   for(int id:new int[]{R.id.previous,R.id.next}){
    View button=v.findViewById(id);float x=rail.getLeft()+button.getLeft()+button.getWidth()/2f,y=rail.getTop()+button.getTop()+button.getHeight()/2f;
    before=shadowOf((Application)context()).getBroadcastIntents().size();touch(v,MotionEvent.ACTION_DOWN,x,y);touch(v,MotionEvent.ACTION_UP,x,y);
    assertEquals(1,clicks[0]);Intent sent=shadowOf((Application)context()).getBroadcastIntents().get(before);assertEquals(s.id,sent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1));assertEquals(id==R.id.previous?-1:1,sent.getIntExtra("direction",0));
   }
  }
 }
 private Switch overlapSwitch(View v){if(v instanceof Switch&&"Overlap widget".equals(String.valueOf(v.getContentDescription())))return(Switch)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){Switch result=overlapSwitch(((ViewGroup)v).getChildAt(i));if(result!=null)return result;}return null;}
 @Test public void overlapSettingAppliesAndPersistsWhileCancelRetainsItsPreviousValue(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();StackState s=new StackState(98);assertFalse(s.overlapBar);BarEditor.show(a,s,()->{});AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();overlapSwitch(dialog.getWindow().getDecorView()).setChecked(true);assertFalse(s.overlapBar);dialog.getButton(-1).performClick();shadowOf(Looper.getMainLooper()).idle();assertTrue(s.overlapBar);
  StackStore store=new StackStore(context());store.put(s);assertTrue(store.get(s.id).overlapBar);
  BarEditor.show(a,s,()->{});dialog=ShadowAlertDialog.getLatestAlertDialog();Switch toggle=overlapSwitch(dialog.getWindow().getDecorView());assertTrue(toggle.isChecked());toggle.setChecked(false);dialog.getButton(-2).performClick();shadowOf(Looper.getMainLooper()).idle();assertTrue(s.overlapBar);assertTrue(store.get(s.id).overlapBar);
 }
 @Config(shadows=BarNavigationTest.FrameworkViewAnimator.class)
 @Test public void slideAnimationStaysBelowTheStationaryOverlappingBar(){
  StackState s=state();s.animateSwitch=true;
  for(int edge=0;edge<4;edge++)for(int direction:new int[]{-1,1}){
   s.railEdge=edge;View v=apply(s,direction);ViewFlipper flipper=v.findViewById(R.id.transition);assertEquals(1,flipper.getDisplayedChild());assertNotNull(flipper.getCurrentView().getAnimation());assertEquals(320,v.findViewById(R.id.widget_area).getWidth());assertEquals(240,v.findViewById(R.id.widget_area).getHeight());assertNull(v.findViewById(R.id.rail).getAnimation());assertSame(v.findViewById(R.id.rail),((ViewGroup)v).getChildAt(1));
  }
 }
}

package com.kevin.widgetstack;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.RectF;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Layout the production RemoteViews inside the framework launcher host, rather
 * than asserting requested LayoutParams or precomputed scale values alone. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class)
public class LauncherBoundsTest {
 Context c;
 @Before public void setup(){c=RuntimeEnvironment.getApplication();}
 @After public void cleanup(){StackHost.get(c).views.remove(12);}
 private StackState state(){StackState s=new StackState(9898);s.add(12);s.showRail=false;return s;}
 private RemoteViews provider(){return new RemoteViews(c.getPackageName(),R.layout.stack_placeholder);}
 private AppWidgetHostView host(StackState s,Boolean compact){
  AppWidgetHostView host=new AppWidgetHostView(c);host.setPadding(0,0,0,0);
  host.updateAppWidget(StackRenderer.render(c,s,0,compact));return host;
 }
 private void layout(View v,int width,int height){
  v.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));v.layout(0,0,width,height);
 }
 private void fillsContent(AppWidgetHostView host){
  ViewGroup content=host.findViewById(R.id.content);assertNotNull(content);assertEquals(1,content.getChildCount());
  View provider=host.findViewById(R.id.empty);assertNotNull(provider);
  RectF visible=new RectF(0,0,provider.getWidth(),provider.getHeight());View node=provider;
  while(node!=content){ViewGroup parent=(ViewGroup)node.getParent();node.getMatrix().mapRect(visible);visible.offset(node.getLeft()-parent.getScrollX(),node.getTop()-parent.getScrollY());node=parent;}
  assertEquals("provider left",0f,visible.left,1f);assertEquals("provider top",0f,visible.top,1f);
  assertEquals("provider right",content.getWidth(),visible.right,1f);assertEquals("provider bottom",content.getHeight(),visible.bottom,1f);

 }
 @Test public void autoReflowsOnActualHostResizeWithoutRepublishing(){
  StackHost.get(c).views.put(12,provider());StackState state=state();AppWidgetHostView host=host(state,false);
  for(int[] size:new int[][]{{900,720},{720,900},{1200,400},{400,1200},{333,197}}){layout(host,size[0],size[1]);fillsContent(host);}
 }
 @Test public void autoDoesNotKeepUsingAnInvisibleLegacyPercentage(){
  StackHost.get(c).views.put(12,provider());StackState state=state();state.setLogicalSizePercent(12,150);
  assertEquals(0,state.widthPx(12));assertEquals(0,state.heightPx(12));
  AppWidgetHostView host=host(state,false);layout(host,900,720);fillsContent(host);
 }
 @Test public void allRailEdgesOverlapAndContractionLeaveNoExtraContentGaps(){
  StackHost.get(c).views.put(12,provider());
  for(int edge=0;edge<4;edge++)for(boolean overlap:new boolean[]{false,true})for(boolean compact:new boolean[]{false,true})for(boolean corner:new boolean[]{false,true}){
   StackState state=state();state.showRail=true;state.railEdge=edge;state.overlapBar=overlap;state.autoContract=true;state.contractToCorner=corner;
   AppWidgetHostView host=host(state,compact);layout(host,900,720);fillsContent(host);
   View content=host.findViewById(R.id.content);int reserve=Math.round((compact?state.contractedWidth:state.railWidth)*c.getResources().getDisplayMetrics().density);
   boolean reserved=!overlap&&!(compact&&corner);
   assertEquals("content width edge="+edge+" compact="+compact+" corner="+corner,900-(reserved&&!state.horizontalRail()?reserve:0),content.getWidth());
   assertEquals("content height",720-(reserved&&state.horizontalRail()?reserve:0),content.getHeight());
  }
 }
 @Test public void tapAtVisibleProviderEdgeSurvivesHostResize(){
  RemoteViews provider=provider();String action="widgetstack.test.PROVIDER_TAP";
  provider.setOnClickPendingIntent(R.id.empty,PendingIntent.getBroadcast(c,712,new Intent(action).setPackage(c.getPackageName()),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));
  StackHost.get(c).views.put(12,provider);AppWidgetHostView host=host(state(),false);
  Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
  activity.setContentView(host);Shadows.shadowOf(Looper.getMainLooper()).idle();
  for(int[] size:new int[][]{{900,720},{1200,400},{400,1200}}){
   layout(host,size[0],size[1]);int before=count(action);long now=SystemClock.uptimeMillis();
   MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,size[0]-2,size[1]/2f,0),up=MotionEvent.obtain(now,now+50,MotionEvent.ACTION_UP,size[0]-2,size[1]/2f,0);
   host.dispatchTouchEvent(down);host.dispatchTouchEvent(up);down.recycle();up.recycle();Shadows.shadowOf(Looper.getMainLooper()).idle();
   assertEquals("visible edge must remain clickable after resizing",before+1,count(action));
  }
 }
 private int count(String action){int n=0;for(Intent i:Shadows.shadowOf((Application)c).getBroadcastIntents())if(action.equals(i.getAction()))n++;return n;}
}

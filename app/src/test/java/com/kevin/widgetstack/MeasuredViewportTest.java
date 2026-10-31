package com.kevin.widgetstack;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class,qualifiers="mdpi")
public class MeasuredViewportTest {
 Context c;AppWidgetManager manager;
 @Before public void setup(){
  c=RuntimeEnvironment.getApplication();manager=AppWidgetManager.getInstance(c);MeasuredViewport.invalidateAll(c);
  AppWidgetProviderInfo info=new AppWidgetProviderInfo();info.provider=new ComponentName(c,StackProvider.class);
  shadowOf(manager).addInstalledProvider(info);shadowOf(manager).setAllowedToBindAppWidgets(true);
  for(int id:new int[]{701,702}){assertTrue(manager.bindAppWidgetIdIfAllowed(id,info.provider));manager.updateAppWidgetOptions(id,hints());}
 }
 @After public void cleanup(){MeasuredViewport.invalidateAll(c);StackHost.get(c).views.remove(12);}
 Bundle hints(){Bundle b=new Bundle();b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,80);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,80);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,60);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,60);return b;}
 Intent request(int id){
  RemoteViews probe=MeasuredViewport.probe(c,id);View view=probe.apply(c,new FrameLayout(c));assertTrue(view.findViewById(R.id.viewport_probe_status).performClick());
  java.util.List<Intent> sent=shadowOf((Application)c).getBroadcastIntents();return new Intent(sent.get(sent.size()-1));
 }
 void measure(int id,int width,int height){Intent i=request(id);i.setSourceBounds(new Rect(25,50,25+width,50+height));MeasuredViewport.receive(c,i);assertTrue(MeasuredViewport.has(c,id));}
 @Test public void cachesActualBoundsSeparatelyForEachPlacement(){
  measure(701,400,300);measure(702,300,500);
  assertEquals(400,StackRenderer.optionsFor(c,701).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));
  assertEquals(300,StackRenderer.optionsFor(c,701).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
  assertEquals(300,StackRenderer.optionsFor(c,702).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));
  assertEquals(500,StackRenderer.optionsFor(c,702).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
  assertEquals(80,manager.getAppWidgetOptions(701).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));
 }
 @Test public void ignoresZeroAndStaleMeasurementsAndInvalidatesChangedOptions(){
  Intent first=request(701);first.setSourceBounds(new Rect());MeasuredViewport.receive(c,first);assertFalse(MeasuredViewport.has(c,701));
  assertEquals("Pending publications must retain a usable tap",first.getData(),request(701).getData());MeasuredViewport.invalidate(c,701);
  Intent current=request(701);first.setSourceBounds(new Rect(0,0,400,300));MeasuredViewport.receive(c,first);assertFalse(MeasuredViewport.has(c,701));
  current.setSourceBounds(new Rect(0,0,400,300));MeasuredViewport.receive(c,current);assertTrue(MeasuredViewport.has(c,701));
  Bundle changed=hints();changed.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,90);manager.updateAppWidgetOptions(701,changed);assertFalse(MeasuredViewport.has(c,701));
  MeasuredViewport.receive(c,current);assertFalse(MeasuredViewport.has(c,701));
 }
 @Test public void onlyPlacedCustomSizingNeedsCalibrationAndEditsReuseIt(){
  StackState state=new StackState(701);state.add(12);state.setLogicalSizePercent(12,150);assertFalse(MeasuredViewport.needed(c,state));
  state.setDimensions(12,200,0);assertTrue(MeasuredViewport.needed(c,state));
  measure(701,400,300);assertFalse(MeasuredViewport.needed(c,state));
  state.setDimensions(12,0,150);assertFalse(MeasuredViewport.needed(c,state));
  state.setDimensions(12,800,600);assertFalse(MeasuredViewport.needed(c,state));
  MeasuredViewport.invalidate(c,701);assertTrue(MeasuredViewport.needed(c,state));
  state.setDimensions(12,0,0);assertFalse(MeasuredViewport.needed(c,state));
  StackState draft=new StackState(-2);draft.add(12);draft.setDimensions(12,200,150);assertFalse(MeasuredViewport.needed(c,draft));
 }
 @Test @Config(qualifiers="xhdpi") public void measuredPixelsBecomeDensityAwareProviderOptions(){
  measure(701,600,400);Bundle actual=StackRenderer.optionsFor(c,701);
  assertEquals(300,actual.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));
  assertEquals(200,actual.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
  StackState state=new StackState(701);state.add(12);state.setDimensions(12,800,0);
  Bundle logical=WidgetSizing.options(actual,state,12,c.getResources().getDisplayMetrics().density);
  assertEquals(400,logical.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));assertEquals(200,logical.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
  MeasuredViewport.invalidate(c,701);assertFalse(MeasuredViewport.has(c,701));
 }
 @Test public void enlargedAndReducedVisibleEdgesRemainClickableInAttachedHost(){
  String action="widgetstack.test.SCALED_EDGE";RemoteViews provider=new RemoteViews(c.getPackageName(),R.layout.stack_placeholder);
  provider.setOnClickPendingIntent(R.id.empty,PendingIntent.getBroadcast(c,812,new Intent(action).setPackage(c.getPackageName()),PendingIntent.FLAG_IMMUTABLE));StackHost.get(c).views.put(12,provider);
  measure(701,400,300);Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
  for(int[] dims:new int[][]{{200,150},{800,600}}){
   StackState state=new StackState(701);state.add(12);state.showRail=false;state.setDimensions(12,dims[0],dims[1]);
   AppWidgetHostView host=new AppWidgetHostView(c);host.setPadding(0,0,0,0);host.updateAppWidget(StackRenderer.render(c,state,0,false));activity.setContentView(host);shadowOf(Looper.getMainLooper()).idle();
   host.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.EXACTLY));host.layout(0,0,400,300);
   int w=Math.min(400,dims[0]),h=Math.min(300,dims[1]),l=(400-w)/2,t=(300-h)/2;
   for(int[] point:new int[][]{{l+w-4,t+8},{l+4,t+h/2},{l+w/2,t+h-4},{l+w/2,t+4}}){
    int before=count(action);long now=SystemClock.uptimeMillis();for(int a:new int[]{android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP}){android.view.MotionEvent event=android.view.MotionEvent.obtain(now,now+40,a,point[0],point[1],0);assertTrue(host.dispatchTouchEvent(event));event.recycle();}shadowOf(Looper.getMainLooper()).idle();assertEquals(before+1,count(action));
   }
  }
 }
 int count(String action){int n=0;for(Intent i:shadowOf((Application)c).getBroadcastIntents())if(action.equals(i.getAction()))n++;return n;}
 @Test @Config(qualifiers="xhdpi") public void customDimensionsFitPhysicalBoundsAtHighDensity(){widthOnlyHeightOnlyAndBothReachAnEdgeAtActualBounds();}
 @Test @Config(qualifiers="420dpi") public void changingHeightByOnePixelKeepsActualAutoWidth(){
  StackHost.get(c).views.put(12,new RemoteViews(c.getPackageName(),R.layout.stack_placeholder));
  for(int height:new int[]{0,864,850,1}){
   StackState state=new StackState(701);state.add(12);state.showRail=false;state.setDimensions(12,0,height);
   AppWidgetHostView host=new AppWidgetHostView(c);host.setPadding(0,0,0,0);host.addView(StackRenderer.renderForHome(c,state,0,0).apply(c,host));
   host.measure(View.MeasureSpec.makeMeasureSpec(1060,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(865,View.MeasureSpec.EXACTLY));host.layout(0,0,1060,865);
   View provider=host.findViewById(R.id.empty);assertNotNull(provider);assertEquals(1060,provider.getWidth());assertEquals(height==0?865:height,provider.getHeight());
  }
 }
 @Test public void widthOnlyHeightOnlyAndBothReachAnEdgeAtActualBounds(){
  StackHost.get(c).views.put(12,new RemoteViews(c.getPackageName(),R.layout.stack_placeholder));
  for(int[] area:new int[][]{{400,300},{300,400},{500,200}}){
   measure(701,area[0],area[1]);
   for(int[] dims:new int[][]{{200,0},{0,150},{200,150},{800,0},{0,600},{800,600}}){
    StackState state=new StackState(701);state.add(12);state.showRail=false;state.setDimensions(12,dims[0],dims[1]);
    AppWidgetHostView host=new AppWidgetHostView(c);host.setPadding(0,0,0,0);host.updateAppWidget(StackRenderer.render(c,state,0,false));
    host.measure(View.MeasureSpec.makeMeasureSpec(area[0],View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(area[1],View.MeasureSpec.EXACTLY));host.layout(0,0,area[0],area[1]);
    View provider=host.findViewById(R.id.empty),node=provider;assertNotNull(provider);RectF bounds=new RectF(0,0,provider.getWidth(),provider.getHeight());
    while(node!=host){ViewGroup parent=(ViewGroup)node.getParent();node.getMatrix().mapRect(bounds);bounds.offset(node.getLeft()-parent.getScrollX(),node.getTop()-parent.getScrollY());node=parent;}
    float w=dims[0]==0?area[0]:dims[0],h=dims[1]==0?area[1]:dims[1],scale=1;
    assertEquals(w*scale,bounds.width(),1f);assertEquals(h*scale,bounds.height(),1f);
    assertEquals(area[0]/2f,bounds.centerX(),1f);assertEquals(area[1]/2f,bounds.centerY(),1f);
    assertEquals("No uniform scaling",1f,host.findViewById(R.id.widget_scale_container).getScaleX(),0f);
   }
  }
 }
}

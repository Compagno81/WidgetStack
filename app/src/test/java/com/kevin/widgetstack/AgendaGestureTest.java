package com.kevin.widgetstack;
import android.app.*;import android.appwidget.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;
/** Dispatch real MotionEvents through parcelled widget layouts, rather than setSelection. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class,qualifiers="mdpi")
public class AgendaGestureTest {
 Context c;StackState s;RemoteViews agenda;
 @Before public void setup(){c=RuntimeEnvironment.getApplication();ReflectionHelpers.setStaticField(StackHost.class,"instance",null);s=new StackState(900);s.add(11);
  agenda=new RemoteViews(c.getPackageName(),R.layout.trace_agenda_fixture);agenda.setViewVisibility(R.id.trace_widget_image,View.GONE);agenda.setViewVisibility(R.id.trace_widget_empty,View.GONE);agenda.setViewVisibility(R.id.trace_widget_profile,View.GONE);
  RemoteViews.RemoteCollectionItems.Builder items=new RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(1);
  for(int i=0;i<100;i++){RemoteViews row=new RemoteViews("android",android.R.layout.simple_list_item_1);row.setTextViewText(android.R.id.text1,"Event "+i);row.setOnClickFillInIntent(android.R.id.text1,new Intent().putExtra("event",i));items.addItem(i,row);}agenda.setPendingIntentTemplate(R.id.trace_widget_list,PendingIntent.getActivity(c,77,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE));agenda.setRemoteAdapter(R.id.trace_widget_list,items.build());agenda.setScrollPosition(R.id.trace_widget_list,0);StackHost.get(c).views.put(11,agenda);
 }
 void layout(View root){root.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY));root.layout(0,0,400,400);}
 void touch(View root,long start,long time,int action,float x,float y){MotionEvent e=MotionEvent.obtain(start,time,action,x,y,0);root.dispatchTouchEvent(e);e.recycle();}
 static class GestureWorkspace extends FrameLayout {
  float down;int intercepted;
  GestureWorkspace(Context c){super(c);}
  @Override public boolean onInterceptTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN)down=e.getY();if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&Math.abs(e.getY()-down)>8){intercepted++;return true;}return false;}
  @Override public boolean onTouchEvent(MotionEvent e){return true;}
 }
 // Models AOSP Launcher3's DOWN-time protection, not Xiaomi's implementation.
 static class ScrollAwareHost extends AppWidgetHostView {
  final boolean protect;ScrollAwareHost(Context c,boolean p){super(c);protect=p;}
  @Override public boolean onInterceptTouchEvent(MotionEvent e){if(protect&&e.getActionMasked()==MotionEvent.ACTION_DOWN&&containsList(this)&&getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);return false;}
 }
 static boolean containsList(View v){if(v instanceof AdapterView)return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(containsList(((ViewGroup)v).getChildAt(i)))return true;return false;}
 static class Outcome {int first,intercepted;Outcome(int f,int i){first=f;intercepted=i;}}
 void check(RemoteViews rv,boolean outerFrame)throws Exception{assertTrue("Gesture must scroll agenda",experiment(rv,outerFrame,false,false).first>0);}
 Outcome experiment(RemoteViews rv,boolean outerFrame,boolean intercept,boolean protect)throws Exception{
  Parcel p=Parcel.obtain();rv.writeToParcel(p,0);p.setDataPosition(0);rv=RemoteViews.CREATOR.createFromParcel(p);p.recycle();
  Activity activity=Robolectric.buildActivity(Activity.class).setup().get();AppWidgetHostView host=new ScrollAwareHost(activity,protect);View root=rv.apply(c,host);host.addView(root);GestureWorkspace workspace=new GestureWorkspace(activity);
  if(intercept){workspace.addView(host);activity.setContentView(workspace);}else if(outerFrame){FrameLayout frame=new FrameLayout(activity);frame.addView(host);activity.setContentView(frame);}else activity.setContentView(host);
  layout(intercept?workspace:host);Shadows.shadowOf(Looper.getMainLooper()).idle();layout(intercept?workspace:host);assertTrue("A recursive host scan finds the embedded list",containsList(host));
  ListView list=root.findViewById(R.id.trace_widget_list);assertNotNull(list.getAdapter());assertEquals(100,list.getCount());assertTrue("List must overflow",list.canScrollVertically(1));
  android.graphics.Rect bounds=new android.graphics.Rect();list.getGlobalVisibleRect(bounds);int[] hostLoc=new int[2];host.getLocationOnScreen(hostLoc);float x=bounds.centerX()-hostLoc[0],y=bounds.bottom-hostLoc[1]-20;long start=SystemClock.uptimeMillis();
  View receiver=intercept?workspace:host;touch(receiver,start,start,MotionEvent.ACTION_DOWN,x,y);
  for(int i=1;i<=10;i++)touch(receiver,start,start+i*20,MotionEvent.ACTION_MOVE,x,y-i*20);
  touch(receiver,start,start+220,MotionEvent.ACTION_UP,x,y-200);layout(host);
  Outcome result=new Outcome(list.getFirstVisiblePosition(),workspace.intercepted);activity.finish();return result;
 }
 void check()throws Exception{check(StackRenderer.renderForHome(c,s,0,0),false);}
 @Test public void parentCanStealTheSwipeWhenHostDoesNotProtectItsList()throws Exception{Outcome r=experiment(StackRenderer.renderForHome(c,s,0,0),false,true,false);assertEquals(0,r.first);assertTrue(r.intercepted>0);}
 @Test public void hostRecognisingTheListPreventsParentGestureTakeover()throws Exception{Outcome r=experiment(StackRenderer.renderForHome(c,s,0,0),false,true,true);assertTrue(r.first>0);assertEquals(0,r.intercepted);}
 @Test public void initialPlaceholderAndPopulatedStackHaveDifferentScrollClassification()throws Exception{assertFalse(containsList(new RemoteViews(c.getPackageName(),R.layout.stack).apply(c,new AppWidgetHostView(c))));assertTrue(containsList(StackRenderer.renderForHome(c,s,0,0).apply(c,new AppWidgetHostView(c))));}
 @Test public void directAgenda()throws Exception{check(agenda,false);}
 @Test public void normalRail()throws Exception{check();}
 @Test public void overlappingRail()throws Exception{s.overlapBar=true;check();}
 @Test public void cornerArrows()throws Exception{s.cornerArrows=true;check();}
 @Test public void contractedCorner()throws Exception{s.autoContract=true;s.contractToCorner=true;check();}
 @Test public void expandedAutoContract()throws Exception{s.autoContract=true;s.contractToCorner=true;check(StackRenderer.render(c,s,0,false),false);}
 @Test public void scaledWidget()throws Exception{s.setVisualScalePercent(11,75);check();}
 @Test public void explicitSize()throws Exception{s.setDimensions(11,400,500);check();}
 @Test public void extraHostContainer()throws Exception{check(StackRenderer.renderForHome(c,s,0,0),true);}
}

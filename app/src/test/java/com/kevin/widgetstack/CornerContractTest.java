package com.kevin.widgetstack;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.view.animation.*;
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
public class CornerContractTest {
 private Context c(){return RuntimeEnvironment.getApplication();}
 private StackState state(int edge,int end){StackState s=new StackState(904);s.autoContract=true;s.contractToCorner=true;s.cornerEnd=end;s.railEdge=edge;s.contractedWidth=24;return s;}
 private View layout(View v){v.measure(View.MeasureSpec.makeMeasureSpec(320,1073741824),View.MeasureSpec.makeMeasureSpec(240,1073741824));v.layout(0,0,320,240);return v;}
 @Test public void everyCornerIsSquareAndLeavesTheRestOfTheWidgetFullSize(){
  for(int edge=0;edge<4;edge++)for(int end=0;end<2;end++)for(boolean overlap:new boolean[]{false,true}){
   StackState s=state(edge,end);s.overlapBar=overlap;View v=layout(StackRenderer.shell(c(),s,0,true).apply(c(),new FrameLayout(c()))),rail=v.findViewById(R.id.rail);assertEquals(24,rail.getWidth());assertEquals(24,rail.getHeight());
   boolean left=edge==StackState.LEFT||(s.horizontalRail()&&end==0),top=edge==StackState.TOP||(!s.horizontalRail()&&end==0);View target=v.findViewById(R.id.corner_touch);assertEquals(48,target.getWidth());assertEquals(48,target.getHeight());assertEquals(left?0:272,target.getLeft());assertEquals(top?0:192,target.getTop());assertEquals(12,rail.getLeft());assertEquals(12,rail.getTop());assertEquals(320,v.findViewById(R.id.widget_area).getWidth());assertEquals(240,v.findViewById(R.id.widget_area).getHeight());
   Bundle b=new Bundle();b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,320);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,240);Bundle child=StackRenderer.childOptions(b,s,true);assertEquals(320,child.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));assertEquals(240,child.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
   int before=shadowOf((Application)c()).getBroadcastIntents().size();rail.performClick();assertEquals(StackProvider.EXPAND,shadowOf((Application)c()).getBroadcastIntents().get(before).getAction());
  }
 }
 @Config(shadows=BarNavigationTest.FrameworkViewAnimator.class)
 @Test public void cornerAnimationShrinksAlongTheBarTowardsChosenEnd(){
  for(int edge=0;edge<4;edge++)for(int end=0;end<2;end++)for(boolean overlap:new boolean[]{false,true})for(int motion:new int[]{-1,1}){
   StackState s=state(edge,end);s.overlapBar=overlap;View v=layout(StackRenderer.shell(c(),s,0,false,motion).apply(c(),new FrameLayout(c())));View compact=v.findViewById(R.id.compact_rail);assertEquals(24,compact.getWidth());assertEquals(24,compact.getHeight());ViewFlipper flipper=v.findViewById(R.id.bar_transition);Animation animation=motion>0?flipper.getInAnimation():flipper.getOutAnimation();assertTrue(animation instanceof ScaleAnimation);animation.initialize(s.horizontalRail()?320:36,s.horizontalRail()?36:240,320,240);animation.setStartTime(1000);Transformation t=new Transformation();animation.getTransformation(1120,t);float[] matrix=new float[9];t.getMatrix().getValues(matrix);assertEquals(s.horizontalRail()?.5f:1f,matrix[0],.001f);assertEquals(s.horizontalRail()?1f:.5f,matrix[4],.001f);assertEquals(end==0?0:s.horizontalRail()?160:120,s.horizontalRail()?matrix[2]:matrix[5],.01f);assertNull(v.findViewById(R.id.content).getAnimation());
  }
 }
 private void tap(View v,float x,float y){for(int action:new int[]{0,1}){MotionEvent e=MotionEvent.obtain(0,SystemClock.uptimeMillis(),action,x,y,0);assertTrue(v.dispatchTouchEvent(e));e.recycle();}shadowOf(Looper.getMainLooper()).idle();}
 @Test public void onlyTheSquareCapturesTouchesWhileTheRestOfItsEdgeRemainsUsable(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();
  for(int edge=0;edge<4;edge++)for(int end=0;end<2;end++){
   StackState s=state(edge,end);View v=StackRenderer.shell(c(),s,0,true).apply(c(),new FrameLayout(c()));ViewGroup content=v.findViewById(R.id.content);content.removeAllViews();Button provider=new Button(c());int[] clicks={0};provider.setOnClickListener(w->clicks[0]++);content.addView(provider,new FrameLayout.LayoutParams(-1,-1));a.setContentView(v);layout(v);
   tap(v,s.horizontalRail()?160:edge==StackState.LEFT?12:308,s.horizontalRail()?edge==StackState.TOP?12:228:120);assertEquals(1,clicks[0]);View target=v.findViewById(R.id.corner_touch);int before=shadowOf((Application)c()).getBroadcastIntents().size();tap(v,target.getLeft()+40,target.getTop()+24);assertEquals(1,clicks[0]);assertEquals(before+1,shadowOf((Application)c()).getBroadcastIntents().size());assertEquals(StackProvider.EXPAND,shadowOf((Application)c()).getBroadcastIntents().get(before).getAction());
  }
 }
 private RadioGroup group(View v,String name){if(v instanceof RadioGroup&&name.equals(v.getContentDescription()))return(RadioGroup)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){RadioGroup r=group(((ViewGroup)v).getChildAt(i),name);if(r!=null)return r;}return null;}
 @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
 @Test public void modeAndCornerApplyTogetherPersistAndDefaultToTheExistingStrip() throws Exception {
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();StackState s=state(3,0);s.contractToCorner=false;BarEditor.show(a,s,()->{});AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();View decor=d.getWindow().getDecorView();((RadioButton)group(decor,"Shrink mode").getChildAt(1)).setChecked(true);((RadioButton)group(decor,"Square corner").getChildAt(1)).setChecked(true);assertFalse(s.contractToCorner);
  ((RadioButton)group(decor,"Settings section").getChildAt(1)).setChecked(true);decor.measure(View.MeasureSpec.makeMeasureSpec(400,1073741824),View.MeasureSpec.makeMeasureSpec(900,1073741824));decor.layout(0,0,400,900);android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(400,900,android.graphics.Bitmap.Config.ARGB_8888);decor.draw(new android.graphics.Canvas(bitmap));java.io.File image=new java.io.File("build/reports/corner-editor-api"+Build.VERSION.SDK_INT+".png");image.getParentFile().mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(image)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
  d.getButton(-1).performClick();assertTrue(s.contractToCorner);assertEquals(1,s.cornerEnd);new StackStore(c()).put(s);assertTrue(new StackStore(c()).get(s.id).contractToCorner);assertEquals(1,new StackStore(c()).get(s.id).cornerEnd);
  c().getSharedPreferences("stacks",0).edit().putString("stack_904","{\"autoContract\":true}").commit();s=new StackStore(c()).get(904);assertFalse(s.contractToCorner);assertEquals(0,s.cornerEnd);
 }
 @Test public void previewInterceptsOnlyTheActualContractedCorner(){
  for(int edge=0;edge<4;edge++)for(int end=0;end<2;end++){
   StackState s=state(edge,end);StackPreview p=new StackPreview(c(),s);layout(p);View host=p.getChildAt(0);float scale=host.getScaleX(),ox=host.getTranslationX(),oy=host.getTranslationY();float x=s.horizontalRail()?end==0?12:228:edge==StackState.LEFT?12:228,y=s.horizontalRail()?edge==StackState.TOP?12:148:end==0?12:148;
   MotionEvent e=MotionEvent.obtain(0,0,0,ox+x*scale,oy+y*scale,0);assertTrue(p.onInterceptTouchEvent(e));assertTrue((Boolean)org.robolectric.util.ReflectionHelpers.getField(p,"barGesture"));e.recycle();x=s.horizontalRail()?120:x;y=s.horizontalRail()?y:80;e=MotionEvent.obtain(0,0,0,ox+x*scale,oy+y*scale,0);assertTrue(p.onInterceptTouchEvent(e));assertFalse((Boolean)org.robolectric.util.ReflectionHelpers.getField(p,"barGesture"));e.recycle();
  }
 }
}

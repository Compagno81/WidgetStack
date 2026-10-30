package com.kevin.widgetstack;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.content.pm.*;
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
public class LivePreviewTest {
 @Before public void resetHost(){org.robolectric.util.ReflectionHelpers.setStaticField(StackHost.class,"instance",null);}
 private Context c(){return RuntimeEnvironment.getApplication();}
 private StackState setup() throws Exception {
  AppWidgetManager m=AppWidgetManager.getInstance(c());AppWidgetProviderInfo info=new AppWidgetProviderInfo();info.provider=new ComponentName(c(),StackProvider.class);info.label="Stack";info.widgetCategory=1;ApplicationInfo app=c().getApplicationInfo();ActivityInfo activity=new ActivityInfo();activity.applicationInfo=app;activity.packageName=c().getPackageName();activity.name=StackProvider.class.getName();
  for(java.lang.reflect.Field field:AppWidgetProviderInfo.class.getDeclaredFields())if(field.getType()==ActivityInfo.class){field.setAccessible(true);field.set(info,activity);}
  shadowOf(m).addInstalledProvider(info);shadowOf(m).setAllowedToBindAppWidgets(true);assertTrue(m.bindAppWidgetIdIfAllowed(700,info.provider));
  Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,320);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,160);m.updateAppWidgetOptions(700,options);
  StackState s=new StackState(700);s.add(10);s.add(20);new StackStore(c()).put(s);
  RemoteViews rv=new RemoteViews(c().getPackageName(),R.layout.stack);rv.setTextViewText(R.id.empty,"Live widget marker");rv.setOnClickPendingIntent(R.id.empty,PendingIntent.getBroadcast(c(),55,new Intent("preview.provider.action").setPackage(c().getPackageName()),PendingIntent.FLAG_IMMUTABLE));StackHost.get(c()).views.put(10,rv);return s;
 }
 private TextView text(View v,String value){if(v instanceof TextView&&value.contentEquals(((TextView)v).getText()))return(TextView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView r=text(((ViewGroup)v).getChildAt(i),value);if(r!=null)return r;}return null;}
 private StackPreview preview(StackState s){StackPreview p=new StackPreview(c(),s);p.measure(View.MeasureSpec.makeMeasureSpec(320,1073741824),View.MeasureSpec.makeMeasureSpec(160,1073741824));p.layout(0,0,320,160);shadowOf(Looper.getMainLooper()).idle();return p;}
 @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
 @Test public void previewRendersCachedWidgetButBlocksProviderTouchesAndAccessibility() throws Exception {
  StackState s=setup();StackPreview p=preview(s);android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(320,160,android.graphics.Bitmap.Config.ARGB_8888);p.draw(new android.graphics.Canvas(bitmap));java.io.File image=new java.io.File("build/reports/live-preview-api"+Build.VERSION.SDK_INT+".png");image.getParentFile().mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(image)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
  TextView marker=text(p,"Live widget marker");assertNotNull("Original cached RemoteViews should be visible in preview",marker);int before=shadowOf((Application)c()).getBroadcastIntents().size();int[] providerTouches={0};marker.setOnTouchListener((view,event)->{providerTouches[0]++;return false;});
  for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP}){MotionEvent e=MotionEvent.obtain(0,SystemClock.uptimeMillis(),action,100,80,0);assertTrue(p.dispatchTouchEvent(e));e.recycle();}
  shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1000));assertEquals(0,providerTouches[0]);assertEquals(before,shadowOf((Application)c()).getBroadcastIntents().size());assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,p.getChildAt(0).getImportantForAccessibility());assertEquals(ViewGroup.FOCUS_BLOCK_DESCENDANTS,((ViewGroup)p.getChildAt(0)).getDescendantFocusability());assertEquals(10,new StackStore(c()).get(s.id).selected);
 }
 @Test public void draftBarNavigationStaysLocalAndUsesTheSameLayoutAndDimensions() throws Exception {
  StackState s=setup();StackPreview p=preview(s);View host=p.getChildAt(0);assertEquals(320,host.getWidth());assertEquals(160,host.getHeight());float scale=host.getScaleX();float x=host.getTranslationX()+(320-s.railWidth/2f)*scale,y=host.getTranslationY()+144*scale;
  for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent e=MotionEvent.obtain(0,SystemClock.uptimeMillis(),action,x,y,0);assertTrue(p.dispatchTouchEvent(e));e.recycle();}
  assertEquals(20,s.selected);assertEquals("Preview navigation must not select the home-screen widget",10,new StackStore(c()).get(s.id).selected);
 }
 @Test public void noSnapshotShowsAnExplicitPlaceholderRatherThanInventedWidgetContent(){
  StackState s=new StackState(711);StackPreview p=preview(s);assertNotNull(text(p,"Add a widget for a live preview"));
 }
 @Test public void attachedPreviewRefreshesOnProviderSnapshotsAndDetachesCleanly() throws Exception {
  StackState s=setup();var controller=Robolectric.buildActivity(Activity.class).create().start().resume();Activity a=controller.get();StackPreview p=preview(s);a.setContentView(p);controller.visible();shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(32));assertNotNull(text(p,"Live widget marker"));
  assertFalse("Preview should subscribe to updates",StackHost.get(c()).previewListeners.isEmpty());
  RemoteViews changed=new RemoteViews(c().getPackageName(),R.layout.stack);changed.setTextViewText(R.id.empty,"Refreshed widget marker");
  org.robolectric.util.ReflectionHelpers.callInstanceMethod(StackHost.get(c()),"capture",org.robolectric.util.ReflectionHelpers.ClassParameter.from(int.class,10),org.robolectric.util.ReflectionHelpers.ClassParameter.from(RemoteViews.class,changed));shadowOf(Looper.getMainLooper()).idle();assertNotNull(text(p,"Refreshed widget marker"));
  a.setContentView(new FrameLayout(a));assertTrue(StackHost.get(c()).previewListeners.isEmpty());
 }
}

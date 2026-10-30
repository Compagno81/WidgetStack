package com.kevin.widgetstack;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class)
public class FreshControlsTest {
 @Test public void successivePublicationsSurviveParcelAndCannotReuseStaleRoot() throws Exception {
  Context c=RuntimeEnvironment.getApplication();StackState state=new StackState(900);RemoteViews first=StackRenderer.renderForHome(c,state,0,0);View old=first.apply(c,new FrameLayout(c));RemoteViews second=StackRenderer.renderForHome(c,state,0,0);assertTrue(first.getViewId()>0);assertNotEquals(first.getViewId(),second.getViewId());
  Parcel p=Parcel.obtain();second.writeToParcel(p,0);p.setDataPosition(0);RemoteViews copy=RemoteViews.CREATOR.createFromParcel(p);p.recycle();assertEquals(second.getViewId(),copy.getViewId());
  java.lang.reflect.Method recycle=RemoteViews.class.getDeclaredMethod("canRecycleView",View.class);recycle.setAccessible(true);assertEquals(Boolean.FALSE,recycle.invoke(copy,old));
  if(Build.VERSION.SDK_INT>=35)try{copy.reapply(c,old);fail("API 35 must also reject direct reapply to a different root");}catch(RuntimeException expected){}
  View fresh=copy.apply(c,new FrameLayout(c));assertEquals(copy.getViewId(),fresh.getId());assertNotNull(fresh.findViewById(R.id.previous));
 }
 @Implements(AppWidgetManager.class)
 public static class ProviderOnlyManager extends org.robolectric.shadows.ShadowAppWidgetManager {
  @Implementation protected AppWidgetProviderInfo getAppWidgetInfo(int id){return null;}
  @Implementation protected int[] getAppWidgetIds(ComponentName provider){return provider.getClassName().equals(StackProvider.class.getName())?new int[]{900}:new int[0];}
 }
 @Test @Config(shadows=ProviderOnlyManager.class)
 public void providerIdListProvidesOwnershipFallbackWithoutAcceptingOtherIds(){Context c=RuntimeEnvironment.getApplication();assertTrue(StackRenderer.owns(c,900));assertFalse(StackRenderer.owns(c,901));assertFalse(StackRenderer.owns(c,-2));}
}

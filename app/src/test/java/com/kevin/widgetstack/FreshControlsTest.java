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
 @Test public void responsiveLauncherSizesDoNotHidePublicationIdentity()throws Exception{
  Context c=RuntimeEnvironment.getApplication();AppWidgetManager manager=AppWidgetManager.getInstance(c);AppWidgetProviderInfo info=new AppWidgetProviderInfo();info.provider=new ComponentName(c,StackProvider.class);Shadows.shadowOf(manager).addInstalledProvider(info);Shadows.shadowOf(manager).setAllowedToBindAppWidgets(true);assertTrue(manager.bindAppWidgetIdIfAllowed(900,info.provider));
  Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,400);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,300);
  java.util.ArrayList<android.util.SizeF> sizes=new java.util.ArrayList<>();sizes.add(new android.util.SizeF(400,300));sizes.add(new android.util.SizeF(300,400));options.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,sizes);manager.updateAppWidgetOptions(900,options);
  StackState state=new StackState(900);RemoteViews one=StackRenderer.renderForHome(c,state,0,0),two=StackRenderer.renderForHome(c,state,0,0);
  assertTrue("Launcher-visible root must have an identity even with size options",one.getViewId()>0);assertNotEquals(one.getViewId(),two.getViewId());
  View old=one.apply(c,new FrameLayout(c));java.lang.reflect.Method recycle=RemoteViews.class.getDeclaredMethod("canRecycleView",View.class);recycle.setAccessible(true);assertEquals(Boolean.FALSE,recycle.invoke(two,old));
 }
 @Test public void twentyAgendaSelectionsSerializeOneActiveListAndFreshControls()throws Exception{
  Context c=RuntimeEnvironment.getApplication();org.robolectric.util.ReflectionHelpers.setStaticField(StackHost.class,"instance",null);
  AppWidgetManager manager=AppWidgetManager.getInstance(c);AppWidgetProviderInfo info=new AppWidgetProviderInfo();info.provider=new ComponentName(c,StackProvider.class);Shadows.shadowOf(manager).addInstalledProvider(info);Shadows.shadowOf(manager).setAllowedToBindAppWidgets(true);assertTrue(manager.bindAppWidgetIdIfAllowed(900,info.provider));
  Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,400);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,300);java.util.ArrayList<android.util.SizeF> sizes=new java.util.ArrayList<>();sizes.add(new android.util.SizeF(400,300));sizes.add(new android.util.SizeF(300,400));options.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,sizes);manager.updateAppWidgetOptions(900,options);
  StackState state=new StackState(900);state.add(11);state.add(22);
  for(int id:new int[]{11,22}){
   RemoteViews agenda=new RemoteViews("android",android.R.layout.list_content);agenda.setContentDescription(android.R.id.list,"Agenda "+id);agenda.setRemoteAdapter(android.R.id.list,new Intent().setComponent(new ComponentName("fixture.calendar","Rows")).setData(android.net.Uri.parse("fixture://agenda/"+id)));StackHost.get(c).views.put(id,agenda);
  }
  int previous=-1;
  for(int n=0;n<20;n++){
   state.select(n%2==0?11:22);RemoteViews views=StackRenderer.renderForHome(c,state,0,0);assertTrue(views.getViewId()>0);assertNotEquals(previous,views.getViewId());previous=views.getViewId();
   Parcel parcel=Parcel.obtain();views.writeToParcel(parcel,0);parcel.setDataPosition(0);RemoteViews copy=RemoteViews.CREATOR.createFromParcel(parcel);parcel.recycle();assertEquals(views.getViewId(),copy.getViewId());
   View root=copy.apply(c,new FrameLayout(c));assertEquals(1,countLists(root));assertEquals("Agenda "+state.selected,root.findViewById(android.R.id.list).getContentDescription());assertTrue(root.findViewById(R.id.next).isClickable());assertTrue(root.findViewById(R.id.previous).isClickable());assertEquals(1,((ViewGroup)root.findViewById(R.id.content)).getChildCount());
  }
 }
 private int countLists(View view){int n=view instanceof ListView?1:0;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)n+=countLists(((ViewGroup)view).getChildAt(i));return n;}
 @Test @Config(shadows=ProviderOnlyManager.class) public void temporaryMissingProviderInfoMustNotDeleteStackMembers(){
  Context c=RuntimeEnvironment.getApplication();org.robolectric.util.ReflectionHelpers.setStaticField(StackHost.class,"instance",null);AppWidgetManager manager=AppWidgetManager.getInstance(c);
  for(int id:new int[]{900,11,22}){
   AppWidgetProviderInfo info=new AppWidgetProviderInfo();info.provider=new ComponentName(c,id==900?StackProvider.class.getName():"fixture.Calendar"+id);Shadows.shadowOf(manager).addInstalledProvider(info);Shadows.shadowOf(manager).setAllowedToBindAppWidgets(true);assertTrue(manager.bindAppWidgetIdIfAllowed(id,info.provider));
  }
  StackStore store=new StackStore(c);StackState saved=store.createSaved("Calendar navigation");saved.add(11);saved.add(22);store.put(saved);store.assign(900,saved.id);
  StackHost.get(c).views.put(11,new RemoteViews(c.getPackageName(),R.layout.stack_placeholder));
  StackRenderer.update(c,900);
  assertEquals("Rendering must not edit the shared saved stack",2,store.get(saved.id).children.size());
  assertEquals(2,store.get(900).children.size());assertEquals(11,store.get(900).selected);
 }
 @Implements(AppWidgetManager.class)
 public static class ProviderOnlyManager extends org.robolectric.shadows.ShadowAppWidgetManager {
  @Implementation protected AppWidgetProviderInfo getAppWidgetInfo(int id){return null;}
  @Implementation protected int[] getAppWidgetIds(ComponentName provider){return provider.getClassName().equals(StackProvider.class.getName())?new int[]{900}:new int[0];}
 }
 @Test @Config(shadows=ProviderOnlyManager.class)
 public void providerIdListProvidesOwnershipFallbackWithoutAcceptingOtherIds(){Context c=RuntimeEnvironment.getApplication();assertTrue(StackRenderer.owns(c,900));assertFalse(StackRenderer.owns(c,901));assertFalse(StackRenderer.owns(c,-2));}
}

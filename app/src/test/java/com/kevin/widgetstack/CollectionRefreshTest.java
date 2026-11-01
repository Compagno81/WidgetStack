package com.kevin.widgetstack;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.robolectric.Shadows.shadowOf;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class)
public class CollectionRefreshTest {
 @Test public void repeatedReadinessOnlyRefreshesActiveRowsWithoutRootPublication(){
  Context c=RuntimeEnvironment.getApplication();ReflectionHelpers.setStaticField(StackHost.class,"instance",null);StackStore store=new StackStore(c);
  AppWidgetManager manager=AppWidgetManager.getInstance(c);AppWidgetProviderInfo info=new AppWidgetProviderInfo();info.provider=new ComponentName(c,StackProvider.class);shadowOf(manager).addInstalledProvider(info);shadowOf(manager).setAllowedToBindAppWidgets(true);
  for(int id:new int[]{901,902})assertTrue(manager.bindAppWidgetIdIfAllowed(id,info.provider));
  StackState s=store.createSaved("Agendas");s.add(11);s.add(22);store.put(s);store.assign(901,s.id);store.assign(902,s.id);store.selectOnHome(901,11);store.selectOnHome(902,22);
  for(int n=0;n<10;n++)StackControlReceiver.handle(c,new Intent(CollectionProxyService.READY).putExtra("child",11).putExtra("view_id",123));
  assertTrue("Readiness cannot enqueue another whole-stack update",StackHost.get(c).queued.isEmpty());
  shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(5));
  assertEquals(11,store.get(901).selected);assertEquals(22,store.get(902).selected);
  String report=StackDiagnostics.report(c);assertTrue(report.contains("collection rows requested #901"));assertFalse("Other copy displays a different agenda",report.contains("collection rows requested #902"));assertFalse(report.contains("publication submitted"));
 }
 @Test public void untestedFutureAndroidVersionsUsePlatformPublication(){
  assertTrue(WidgetPublisher.usesDirectPublication(35));
  for(int api:new int[]{31,34,36,37,38})assertFalse(WidgetPublisher.usesDirectPublication(api));
 }
}

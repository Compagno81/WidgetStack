package com.kevin.widgetstack;
import android.app.Application;
import android.appwidget.*;
import android.content.*;
import android.widget.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.robolectric.Shadows.shadowOf;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class)
public class RecoveryTest {
 @Before public void resetHost(){org.robolectric.util.ReflectionHelpers.setStaticField(StackHost.class,"instance",null);}
 @Test public void firstRecoveryLaunchPausesAndInitializationDoesNotOverrideChoice(){
  Context c=RuntimeEnvironment.getApplication();Recovery.initialize(c);assertTrue(Recovery.paused(c));
  c.getSharedPreferences("recovery",0).edit().putBoolean("paused",false).commit();Recovery.initialize(c);assertFalse(Recovery.paused(c));
 }
 @Test public void pausedUpdatesDoNotDeleteChildrenOrLoadCollectionViews(){
  Context base=RuntimeEnvironment.getApplication();
  // Robolectric stores widget IDs per manager instance rather than in a shared
  // system service. Reuse its manager for the data-preservation assertions.
  Context c=new ContextWrapper(base){@Override public Context createConfigurationContext(android.content.res.Configuration config){return base;}};
  AppWidgetManager manager=AppWidgetManager.getInstance(c);AppWidgetProviderInfo info=new AppWidgetProviderInfo();info.provider=new ComponentName(c,StackProvider.class);shadowOf(manager).addInstalledProvider(info);shadowOf(manager).setAllowedToBindAppWidgets(true);assertTrue(manager.bindAppWidgetIdIfAllowed(901,info.provider));
  StackStore store=new StackStore(c);StackState saved=store.createSaved("Agendas");saved.add(11);saved.add(22);store.put(saved);store.assign(901,saved.id);
  Recovery.initialize(c);StackRenderer.update(c,901);StackRenderer.updateSizes(c,901);StackHost.get(c).sync();StackHost.get(c).collectionChanged(11,123,"ready");StackHost.get(c).schedule(901);
  StackControlReceiver.handle(c,new Intent(StackProvider.SELECT).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,901).putExtra("direction",1));
  assertEquals(2,store.get(saved.id).children.size());assertEquals(2,store.get(901).children.size());assertEquals(11,store.get(901).selected);assertTrue(StackHost.get(c).queued.isEmpty());assertTrue(StackHost.get(c).tracked.isEmpty());
  android.view.View root=Recovery.view(c,901).apply(c,new FrameLayout(c));TextView label=root.findViewById(R.id.empty);assertTrue(label.getText().toString().contains("paused"));assertTrue(label.performClick());
 }
 @Test public void pausePreservesDimensionsScaleAndSavedPlacement(){
  Context c=RuntimeEnvironment.getApplication();StackStore store=new StackStore(c);StackState s=store.createSaved("Keep me");s.add(11);s.setDimensions(11,0,850);s.setVisualScalePercent(11,130);store.put(s);store.assign(902,s.id);
  Recovery.pause(c);assertTrue(Recovery.paused(c));assertEquals(850,store.get(s.id).heightPx(11));assertEquals(130,store.get(s.id).visualScalePercent(11));assertEquals(s.id,store.definitionId(902));
 }
}

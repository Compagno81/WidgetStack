package com.kevin.widgetstack;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class)
public class TaskerPluginTest {
 @Test public void taskerCanDiscoverEditorAndReceiver(){
  Context c=RuntimeEnvironment.getApplication();
  assertFalse(c.getPackageManager().queryIntentActivities(new Intent(TaskerPlugin.EDIT).setPackage(c.getPackageName()),0).isEmpty());
  assertFalse(c.getPackageManager().queryBroadcastReceivers(new Intent(TaskerPlugin.FIRE).setPackage(c.getPackageName()),0).isEmpty());
 }
 @Test public void pluginBundleSelectsOnlyConfiguredInstance(){
  Context c=RuntimeEnvironment.getApplication();StackStore store=new StackStore(c);StackState s=store.createSaved("Work");s.add(11);s.add(22);store.put(s);store.assign(101,s.id);store.assign(102,s.id);TaskerReceiver.enable(c,true);
  Intent fire=new Intent(TaskerPlugin.FIRE).putExtra(TaskerPlugin.BUNDLE,TaskerPlugin.configuration("instance_id",101,2));
  assertEquals(1,TaskerReceiver.select(c,TaskerPlugin.command(fire)).size());assertEquals(22,store.get(101).selected);assertEquals(11,store.get(102).selected);
 }
 @Test public void malformedConfigurationCannotBecomeACommand(){
  assertNull(TaskerPlugin.command(new Intent(TaskerPlugin.FIRE)));
  assertNull(TaskerPlugin.command(new Intent("other")));
  Bundle b=TaskerPlugin.configuration("stack_id",-2,0);assertNull(TaskerPlugin.command(new Intent(TaskerPlugin.FIRE).putExtra(TaskerPlugin.BUNDLE,b)));
  b.putInt("position",1);b.putInt("instance_id",101);assertNull(TaskerPlugin.command(new Intent(TaskerPlugin.FIRE).putExtra(TaskerPlugin.BUNDLE,b)));
 }
 @Test public void editorRestoresSelectionAndReturnsLocaleBundle(){
  Context c=RuntimeEnvironment.getApplication();StackStore store=new StackStore(c);StackState s=store.createSaved("Work");s.add(11);s.add(22);store.put(s);store.assign(101,s.id);
  Intent edit=new Intent(TaskerPlugin.EDIT).putExtra(TaskerPlugin.BUNDLE,TaskerPlugin.configuration("instance_id",101,2));
  TaskerPluginActivity a=Robolectric.buildActivity(TaskerPluginActivity.class,edit).setup().get();
  assertEquals(1,a.position.getSelectedItemPosition());a.save();
  assertEquals(Activity.RESULT_OK,shadowOf(a).getResultCode());
  Bundle result=shadowOf(a).getResultIntent().getBundleExtra(TaskerPlugin.BUNDLE);assertEquals(101,result.getInt("instance_id"));assertEquals(2,result.getInt("position"));
 }
 @Test public void cancellingAnEmptyEditorDoesNotCreateAnAction(){
  TaskerPluginActivity a=Robolectric.buildActivity(TaskerPluginActivity.class,new Intent(TaskerPlugin.EDIT)).setup().get();
  assertFalse(a.save.isEnabled());a.finish();assertEquals(Activity.RESULT_CANCELED,shadowOf(a).getResultCode());
 }
}

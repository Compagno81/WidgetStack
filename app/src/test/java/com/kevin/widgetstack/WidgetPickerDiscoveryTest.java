package com.kevin.widgetstack;

import android.app.Application;
import android.appwidget.*;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Synthetic installed widget descriptors exercise discovery and the actual picker UI. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class)
public class WidgetPickerDiscoveryTest {
 private AppWidgetManager manager(){return AppWidgetManager.getInstance(RuntimeEnvironment.getApplication());}
 private AppWidgetProviderInfo provider(String pkg,String app,String label) throws Exception {
  AppWidgetProviderInfo p=new AppWidgetProviderInfo();p.provider=new ComponentName(pkg,pkg+".TestWidget");p.label=label;p.widgetCategory=AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN;
  ApplicationInfo application=new ApplicationInfo();application.packageName=pkg;application.nonLocalizedLabel=app;application.uid=10001;
  ActivityInfo activity=new ActivityInfo();activity.packageName=pkg;activity.name=p.provider.getClassName();activity.nonLocalizedLabel=label;activity.applicationInfo=application;
  for(java.lang.reflect.Field field:AppWidgetProviderInfo.class.getDeclaredFields())if(field.getType()==ActivityInfo.class){field.setAccessible(true);field.set(p,activity);}
  return p;
 }
 private <T extends View>T find(View v,Class<T> type){if(type.isInstance(v))return type.cast(v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){T result=find(((ViewGroup)v).getChildAt(i),type);if(result!=null)return result;}return null;}
 @Test public void pickerFindsSpacedNamesAndWidgetLabelsWithoutDuplicateProviders() throws Exception {
  shadowOf(manager()).addInstalledProvider(provider("com.duolingo","Duolingo","Streak"));
  shadowOf(manager()).addInstalledProvider(provider("com.myfitnesspal.android","MyFitnessPal","Calories Consumed"));
  shadowOf(manager()).addInstalledProvider(provider(RuntimeEnvironment.getApplication().getPackageName(),"Widget Stack","Stack"));
  assertEquals(2,WidgetCatalog.installed(RuntimeEnvironment.getApplication()).size());
  var controller=Robolectric.buildActivity(WidgetPickerActivity.class).setup();View decor=controller.get().getWindow().getDecorView();
  EditText search=find(decor,EditText.class);ExpandableListView list=find(decor,ExpandableListView.class);
  assertEquals(2,list.getExpandableListAdapter().getGroupCount());
  for(String query:new String[]{"Duo Lingo","My Fitness Pal","Calories Consumed","com.duolingo"}){search.setText(query);assertEquals(query,1,list.getExpandableListAdapter().getGroupCount());}
  search.setText("Not installed");assertEquals(0,list.getExpandableListAdapter().getGroupCount());controller.pause().stop().destroy();
 }
 @Test public void refreshAndResumeDiscoverNewlyAvailableWidgets() throws Exception {
  var controller=Robolectric.buildActivity(WidgetPickerActivity.class).setup();View decor=controller.get().getWindow().getDecorView();ExpandableListView list=find(decor,ExpandableListView.class);
  assertEquals(0,list.getExpandableListAdapter().getGroupCount());
  shadowOf(manager()).addInstalledProvider(provider("com.duolingo","Duolingo","Streak"));
  Toolbar toolbar=find(decor,Toolbar.class);assertTrue(toolbar.getMenu().performIdentifierAction(toolbar.getMenu().getItem(0).getItemId(),0));assertEquals(1,list.getExpandableListAdapter().getGroupCount());
  shadowOf(manager()).addInstalledProvider(provider("com.myfitnesspal.android","MyFitnessPal","Calories Consumed"));
  controller.pause().resume();assertEquals(2,list.getExpandableListAdapter().getGroupCount());controller.pause().stop().destroy();
 }
}

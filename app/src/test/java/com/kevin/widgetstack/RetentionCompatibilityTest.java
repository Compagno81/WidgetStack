package com.kevin.widgetstack;

import android.app.Application;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,application=Application.class)
public class RetentionCompatibilityTest {
 @Test public void unvalidatedPlatformVersionsUseCompatibilityMode(){
  assertTrue(RetainedWidgets.supportedOn(31));assertTrue(RetainedWidgets.supportedOn(35));
  assertFalse(RetainedWidgets.supportedOn(36));assertFalse(RetainedWidgets.supportedOn(37));assertFalse(RetainedWidgets.supportedOn(38));
 }
 @Test public void newerPlatformCannotReusePreviouslyRetainedPublication()throws Exception{
  Context c=RuntimeEnvironment.getApplication();StackState s=new StackState(900);s.add(11);RetainedWidgets.remember(c,s);
  int sdk=Build.VERSION.SDK_INT;
  try{
   ReflectionHelpers.setStaticField(Build.VERSION.class,"SDK_INT",37);
   assertFalse(RetainedWidgets.navigate(c,s,0));assertFalse(RetainedWidgets.contains(s.id));
   assertThrows(UnsupportedOperationException.class,()->RetainedWidgets.full(c,s));
  }finally{ReflectionHelpers.setStaticField(Build.VERSION.class,"SDK_INT",sdk);RetainedWidgets.clear();}
 }
 @Test public void compatibilityPublicationLoadsOnlySelectedCollectionAfterParceling()throws Exception{
  Context c=RuntimeEnvironment.getApplication();ReflectionHelpers.setStaticField(StackHost.class,"instance",null);
  StackState s=new StackState(900);s.add(11);s.add(22);
  for(int child:s.children){RemoteViews rv=new RemoteViews("android",android.R.layout.list_content);
   RemoteViews.RemoteCollectionItems.Builder rows=new RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(1);
   for(int n=0;n<20;n++){RemoteViews row=new RemoteViews("android",android.R.layout.simple_list_item_1);row.setTextViewText(android.R.id.text1,"Child "+child+" row "+n);rows.addItem(n,row);}
   rv.setRemoteAdapter(android.R.id.list,rows.build());StackHost.get(c).views.put(child,rv);
  }
  int previous=-1;
  for(int child:new int[]{11,22,11}){
   s.select(child);RemoteViews rv=StackRenderer.renderForHome(c,s,0,0);assertNotEquals(previous,rv.getViewId());previous=rv.getViewId();
   Parcel p=Parcel.obtain();rv.writeToParcel(p,0);p.setDataPosition(0);rv=RemoteViews.CREATOR.createFromParcel(p);p.recycle();
   View root=rv.apply(c,new AppWidgetHostView(c));assertNull(root.findViewById(R.id.retained_pages));
   ListView list=root.findViewById(android.R.id.list);assertNotNull(list.getAdapter());assertEquals(20,list.getCount());
   TextView row=list.getAdapter().getView(0,null,list).findViewById(android.R.id.text1);assertEquals("Child "+child+" row 0",row.getText().toString());
   assertTrue(root.findViewById(R.id.next).isClickable());
  }
 }
}

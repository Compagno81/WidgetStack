package com.kevin.widgetstack;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.os.Bundle;
import android.util.SizeF;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class)
public class IndependentSizingTest {
 @Test public void enlargesAndShrinksWithoutDistortion(){
  assertEquals(.5f,WidgetSizing.fit(300,300,600,400),.0001f);
  assertEquals(3f,WidgetSizing.fit(600,400,200,100),.0001f);
  assertEquals(2f,WidgetSizing.fit(600,400,300,200),.0001f);
 }
 @Test public void explicitAxisIsDensityAwareAndAutoKeepsResponsiveVariants(){
  Bundle actual=new Bundle();actual.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,120);actual.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,200);actual.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,80);actual.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,160);
  ArrayList<SizeF> sizes=new ArrayList<>();sizes.add(new SizeF(120,160));sizes.add(new SizeF(200,80));actual.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,sizes);
  StackState state=new StackState(-2);state.add(11);state.add(22);state.setDimensions(11,600,0);
  Bundle requested=WidgetSizing.options(actual,state,11,2);
  assertEquals(300,requested.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));assertEquals(300,requested.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH));assertEquals(80,requested.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
  ArrayList<SizeF> variants=requested.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);assertEquals(new SizeF(300,160),variants.get(0));assertEquals(new SizeF(300,80),variants.get(1));
  assertEquals(120,actual.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));assertEquals(120,WidgetSizing.options(actual,state,22,2).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));
 }
 @Test public void fittedRemoteViewsInflateAtRequestedBoundsAndEnlarge() throws Exception {
  android.content.Context c=RuntimeEnvironment.getApplication();StackState state=new StackState(-2);state.showRail=false;
  android.widget.RemoteViews root=StackRenderer.shell(c,state);
  android.widget.RemoteViews provider=new android.widget.RemoteViews(c.getPackageName(),R.layout.stack_placeholder);
  RemoteViewsBridge.embedFitted(c,root,provider,11,new SizeF(200,100),new SizeF(600,400));
  android.view.View view=root.apply(c,new android.widget.FrameLayout(c));
  android.view.View scaled=view.findViewById(R.id.widget_scale_container);
  assertNotNull(scaled);assertEquals(3f,scaled.getScaleX(),.0001f);assertEquals(3f,scaled.getScaleY(),.0001f);
  assertEquals(Math.round(200*c.getResources().getDisplayMetrics().density),scaled.getLayoutParams().width);
  assertEquals(Math.round(100*c.getResources().getDisplayMetrics().density),scaled.getLayoutParams().height);
 }
 @Test public void fixedProviderRootFillsAvailableWidthInsteadOfScalingEmptyRequestedSpace() throws Exception {
  android.content.Context c=RuntimeEnvironment.getApplication();StackState state=new StackState(-2);state.showRail=false;
  android.widget.RemoteViews root=StackRenderer.shell(c,state);
  android.widget.RemoteViews provider=new android.widget.RemoteViews(c.getPackageName(),R.layout.stack_placeholder);
  provider.setViewLayoutWidth(R.id.empty,120,android.util.TypedValue.COMPLEX_UNIT_DIP);
  provider.setViewLayoutHeight(R.id.empty,80,android.util.TypedValue.COMPLEX_UNIT_DIP);
  RemoteViewsBridge.embedFitted(c,root,provider,11,new SizeF(200,100),new SizeF(600,400));
  android.view.View view=root.apply(c,new android.widget.FrameLayout(c));
  android.view.View scaled=view.findViewById(R.id.widget_scale_container);
  float density=c.getResources().getDisplayMetrics().density;
  assertEquals(5f,scaled.getScaleX(),.0001f);
  assertEquals(Math.round(120*density),scaled.getLayoutParams().width);
  assertEquals(Math.round(80*density),scaled.getLayoutParams().height);
  assertEquals(600*density,scaled.getLayoutParams().width*scaled.getScaleX(),1f);
  assertEquals(400*density,scaled.getLayoutParams().height*scaled.getScaleY(),1f);
 }
 @Test public void dimensionsSurvivePersistenceReorderAndIndependentPlacements(){
  StackStore store=new StackStore(RuntimeEnvironment.getApplication());StackState saved=store.createSaved("Sizing");saved.add(11);saved.add(22);saved.setDimensions(11,600,400);saved.setDimensions(22,175,0);store.put(saved);store.assign(100,saved.id);store.assign(200,saved.id);
  StackState copy=store.get(saved.id);copy.move(0,1);store.put(copy);
  assertEquals(600,store.get(100).widthPx(11));assertEquals(400,store.get(200).heightPx(11));assertEquals(175,copy.widthPx(22));assertEquals(0,copy.heightPx(22));
  copy.remove(11);copy.add(11);assertEquals(0,copy.widthPx(11));
 }
}

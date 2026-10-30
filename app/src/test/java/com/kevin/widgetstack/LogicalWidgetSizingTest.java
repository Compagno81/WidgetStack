package com.kevin.widgetstack;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.os.Bundle;
import android.util.SizeF;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class)
public class LogicalWidgetSizingTest {
 @Test public void requestsRoomierProviderOptionsWhileKeepingActualOptionsUnchanged(){
  Bundle actual=new Bundle();actual.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,120);actual.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,180);actual.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,80);actual.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,140);ArrayList<SizeF> sizes=new ArrayList<>();sizes.add(new SizeF(120,140));sizes.add(new SizeF(180,80));actual.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,sizes);
  Bundle logical=StackRenderer.logicalOptions(actual,125);assertEquals(120,actual.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));assertEquals(150,logical.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));assertEquals(225,logical.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH));assertEquals(100,logical.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));assertEquals(175,logical.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));ArrayList<SizeF> requested=logical.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);assertEquals(new SizeF(150,175),requested.get(0));assertEquals(180,StackRenderer.logicalOptions(actual,100).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH));
 }
}

package com.kevin.widgetfixture;

import android.app.Activity;
import android.appwidget.*;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;

/** Separately installed test launcher. Delivers widget clicks from another UID. */
public final class LauncherActivity extends Activity {
 private AppWidgetHost host;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  host=new AppWidgetHost(this,7777);host.startListening();
  int id=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
  AppWidgetHostView view=host.createView(this,id,AppWidgetManager.getInstance(this).getAppWidgetInfo(id));
  view.setPadding(0,0,0,0);
  FrameLayout frame=new FrameLayout(this);
  frame.addView(view,new FrameLayout.LayoutParams(400,300,Gravity.CENTER));setContentView(frame);
 }
 @Override public void onDestroy(){if(host!=null)host.stopListening();super.onDestroy();}
}

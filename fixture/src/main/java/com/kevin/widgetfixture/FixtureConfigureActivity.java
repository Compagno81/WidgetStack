package com.kevin.widgetfixture;
import android.app.*;import android.appwidget.*;import android.content.*;import android.os.*;import android.widget.*;
/** Test-only provider setup screen. Test control travels through fixture EventsProvider. */
public final class FixtureConfigureActivity extends Activity {
 static volatile FixtureConfigureActivity current;
 @Override public void onCreate(Bundle b){super.onCreate(b);setResult(RESULT_CANCELED);current=this;getSharedPreferences("events",0).edit().putInt("config_id",getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1)).commit();Button accept=new Button(this);accept.setText("Finish setup");accept.setOnClickListener(v->complete(true));setContentView(accept);}
 void complete(boolean accepted){int id=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);if(accepted)setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id));finish();}
 @Override protected void onDestroy(){if(current==this)current=null;super.onDestroy();}
}

package com.kevin.widgetfixture;
import android.app.*;import android.content.Intent;import android.os.*;import android.net.Uri;import android.view.Gravity;import android.widget.*;
public class OpenedActivity extends Activity {
 @Override public void onCreate(Bundle b){super.onCreate(b);show(getIntent());}
 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);show(i);}
 private void show(Intent i){int row=i.getIntExtra("row",-99);Bundle b=new Bundle();b.putInt("row",row);getContentResolver().call(Uri.parse("content://com.kevin.widgetfixture.events"),"record",null,b);TextView t=new TextView(this);t.setText("Opened row "+row);t.setTextSize(24);t.setGravity(Gravity.CENTER);setContentView(t);}
}

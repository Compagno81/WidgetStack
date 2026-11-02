package com.kevin.widgetstack;

import android.app.Activity;
import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.util.*;

/** Native action editor discovered by Tasker through EDIT_SETTING. */
public final class TaskerPluginActivity extends Activity {
 final List<Target> targets=new ArrayList<>();
 Spinner target,position;Button save;StackStore store;Bundle previous;
 static final class Target {
  final String key,label;final int id,saved;
  Target(String k,int i,int s,String l){key=k;id=i;saved=s;label=l;}
  @Override public String toString(){return label;}
 }
 @Override public void onCreate(Bundle state){
  super.onCreate(state);setResult(RESULT_CANCELED);store=new StackStore(this);
  previous=state!=null?state:getIntent().getBundleExtra(TaskerPlugin.BUNDLE);
  LinearLayout root=Ui.page(this,"Widget Stack · Tasker action");
  Ui.text(this,root,"Select which stack copies to control, then the widget position to show. Positions follow the order in Edit saved stack.",15);
  Switch enabled=new Switch(this);enabled.setText("Enable Tasker control (allows external automation)");enabled.setChecked(TaskerReceiver.enabled(this));enabled.setOnCheckedChangeListener((v,on)->TaskerReceiver.enable(this,on));root.addView(enabled);
  for(StackState s:store.saved()){
   targets.add(new Target("stack_id",s.id,s.id,s.name+" · all copies"));
   for(int id:store.placements(s.id))targets.add(new Target("instance_id",id,s.id,s.name+" · instance "+id));
  }
  int selected=0;
  if(previous!=null){String key=previous.containsKey("instance_id")?"instance_id":"stack_id";int id=previous.getInt(key,Integer.MIN_VALUE);boolean found=false;
   for(int n=0;n<targets.size();n++)if(targets.get(n).key.equals(key)&&targets.get(n).id==id){selected=n;found=true;break;}
   if(!found){targets.add(0,new Target(key,id,Integer.MIN_VALUE,"Unavailable target · choose another"));selected=0;}
  }
  Ui.text(this,root,"Stack or instance",17);target=new Spinner(this);target.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,targets));root.addView(target);
  Ui.text(this,root,"Widget position",17);position=new Spinner(this);root.addView(position);
  Ui.text(this,root,"For an instance ID, open that widget’s configuration. All copies includes Canvas-hosted copies. Re-adding a widget creates a new instance ID.",14);
  save=Ui.primaryButton(this,root,"Save action",this::save);
  Ui.button(this,root,"Cancel",this::finish);
  target.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int n,long id){positions(n);}});
  target.setSelection(selected);positions(selected);
 }
 void positions(int index){
  List<String> labels=new ArrayList<>();StackState s=index>=0&&index<targets.size()?store.get(targets.get(index).saved):null;
  if(s!=null)for(int n=0;n<s.children.size();n++){
   AppWidgetProviderInfo info=AppWidgetManager.getInstance(this).getAppWidgetInfo(s.children.get(n));
   labels.add((n+1)+" · "+(info==null?"Widget":info.loadLabel(getPackageManager())));
  }
  position.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));save.setEnabled(!labels.isEmpty());
  if(previous!=null&&index>=0&&index<targets.size()){
   Target t=targets.get(index);if(previous.containsKey(t.key)&&previous.getInt(t.key)==t.id){int p=previous.getInt("position",1)-1;if(p>=0&&p<labels.size())position.setSelection(p);}
  }
 }
 void save(){
  int n=target.getSelectedItemPosition(),p=position.getSelectedItemPosition();if(n<0||n>=targets.size()||p<0)return;
  Target t=targets.get(n);Bundle b=TaskerPlugin.configuration(t.key,t.id,p+1);
  Intent result=new Intent().putExtra(TaskerPlugin.BUNDLE,b).putExtra(TaskerPlugin.BLURB,t.label+" → position "+(p+1));
  setResult(RESULT_OK,result);finish();
 }
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);int n=target.getSelectedItemPosition();if(n>=0&&n<targets.size()){Target t=targets.get(n);out.putAll(TaskerPlugin.configuration(t.key,t.id,position.getSelectedItemPosition()+1));}}
}

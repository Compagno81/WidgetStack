package com.kevin.widgetstack;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
import java.util.*;

public final class ProfilePluginActivity extends Activity {
 Spinner stack,profile;List<StackState> stacks;List<Integer> ids=new ArrayList<>();List<String> names=new ArrayList<>();Bundle previous;
 @Override public void onCreate(Bundle state){super.onCreate(state);setResult(RESULT_CANCELED);previous=state!=null?state:getIntent().getBundleExtra(TaskerPlugin.BUNDLE);LinearLayout root=Ui.page(this,"Tasker · Activate widget profile");Ui.text(this,root,"Choose a saved stack and its widget visibility profile. Manage profiles inside that stack's configuration.",15);
  Switch enabled=new Switch(this);enabled.setText("Enable Tasker control");enabled.setChecked(TaskerReceiver.enabled(this));enabled.setOnCheckedChangeListener((v,on)->TaskerReceiver.enable(this,on));root.addView(enabled);
  stacks=new StackStore(this).saved();List<String> labels=new ArrayList<>();int selected=0;for(int n=0;n<stacks.size();n++){labels.add(stacks.get(n).name);if(previous!=null&&previous.getInt("stack_id",-1)==stacks.get(n).id)selected=n;}
  if(previous!=null&&stacks.stream().noneMatch(s->s.id==previous.getInt("stack_id",-1))){stacks.add(0,new StackState(-1));labels.add(0,"Unavailable stack — choose another");selected=0;}
  stack=new Spinner(this);stack.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));root.addView(stack);profile=new Spinner(this);root.addView(profile);
  stack.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int n,long id){profiles();}});stack.setSelection(selected);profiles();
  Ui.primaryButton(this,root,"Save action",()->{int n=stack.getSelectedItemPosition(),index=profile.getSelectedItemPosition();if(n<0||n>=stacks.size()||index<0)return;int saved=stacks.get(n).id,id=ids.get(index);VisibilityProfiles profiles=new VisibilityProfiles(this);if(!new StackStore(this).isSaved(saved)||(id!=0&&!profiles.forStack(saved).containsKey(id))){Toast.makeText(this,"Choose an available stack and profile",Toast.LENGTH_SHORT).show();return;}setResult(RESULT_OK,new Intent().putExtra(TaskerPlugin.BUNDLE,TaskerPlugin.profileConfiguration(saved,id)).putExtra(TaskerPlugin.BLURB,stacks.get(n).name+" → "+names.get(index)));finish();});Ui.button(this,root,"Cancel",this::finish);
 }
 void profiles(){ids.clear();names.clear();ids.add(0);names.add("Show all widgets");int n=stack.getSelectedItemPosition();if(n<0||n>=stacks.size())return;int saved=stacks.get(n).id;for(Map.Entry<Integer,String> e:new VisibilityProfiles(this).forStack(saved).entrySet()){ids.add(e.getKey());names.add(e.getValue());}int wanted=previous!=null&&previous.getInt("stack_id",-1)==saved?previous.getInt("profile_id",0):0;if(!ids.contains(wanted)){ids.add(wanted);names.add("Unavailable profile — choose another");}profile.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));profile.setSelection(ids.indexOf(wanted));}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);int n=stack.getSelectedItemPosition(),p=profile.getSelectedItemPosition();if(n>=0&&p>=0)out.putAll(TaskerPlugin.profileConfiguration(stacks.get(n).id,ids.get(p)));}
}

package com.kevin.widgetstack;
import android.app.*;
import android.os.Bundle;
import android.widget.*;
import java.util.*;

public final class ProfilesActivity extends Activity {
 int saved;
 @Override public void onCreate(Bundle b){super.onCreate(b);saved=getIntent().getIntExtra(SavedStacks.EXTRA_SAVED_ID,-1);if(!new StackStore(this).isSaved(saved)){finish();return;}render();}
 void render(){VisibilityProfiles profiles=new VisibilityProfiles(this);LinearLayout root=Ui.page(this,"Widget visibility profiles");Ui.text(this,root,"Active: "+profiles.activeName(saved),18);Ui.text(this,root,"Each profile chooses which widgets appear inside this saved stack. Its copies share the active profile and keep their own selection. Hidden widgets keep their configuration. New widgets must be checked in existing profiles.",14);
  Ui.button(this,root,"Show all widgets",()->{profiles.activate(saved,0);render();});
  Ui.primaryButton(this,root,"Create profile",()->name(null,0));
  for(Map.Entry<Integer,String> entry:profiles.forStack(saved).entrySet()){int id=entry.getKey();LinearLayout card=Ui.card(this,root);Ui.text(this,card,entry.getValue()+(profiles.active(saved)==id?" · active":""),18);Ui.text(this,card,"Tasker profile_id: "+id,13);
   Ui.button(this,card,"Choose visible widgets",()->members(id));Ui.button(this,card,"Activate",()->{profiles.activate(saved,id);render();});Ui.button(this,card,"Rename",()->name(entry.getValue(),id));Ui.button(this,card,"Delete",()->new AlertDialog.Builder(this).setMessage("Delete this profile? Deleting the active profile shows all widgets.").setPositiveButton("Delete",(d,w)->{profiles.delete(id);render();}).setNegativeButton("Cancel",null).show());
  }
 }
 void name(String previous,int id){EditText field=new EditText(this);field.setSingleLine(true);if(previous!=null)field.setText(previous);AlertDialog dialog=new AlertDialog.Builder(this).setTitle(id==0?"Create profile":"Rename profile").setView(field).setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();dialog.setOnShowListener(d->dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(v->{String text=field.getText().toString().trim();if(text.isEmpty()){field.setError("Enter a profile name");return;}VisibilityProfiles profiles=new VisibilityProfiles(this);if(id==0){int created=profiles.create(saved,text);dialog.dismiss();render();members(created);}else{profiles.rename(id,text);dialog.dismiss();render();}}));dialog.show();}
 void members(int id){VisibilityProfiles profiles=new VisibilityProfiles(this);List<Integer> children=new StackStore(this).get(saved).children;String[] names=new String[children.size()];boolean[] selected=new boolean[children.size()];Set<String> members=profiles.members(id);for(int n=0;n<children.size();n++){int child=children.get(n);android.appwidget.AppWidgetProviderInfo info=android.appwidget.AppWidgetManager.getInstance(this).getAppWidgetInfo(child);names[n]=(n+1)+". "+(info==null?"Widget #"+child:info.loadLabel(getPackageManager()));selected[n]=members.contains(String.valueOf(child));}
  new AlertDialog.Builder(this).setTitle("Visible widgets · "+profiles.all().get(id)).setMultiChoiceItems(names,selected,(d,n,on)->selected[n]=on).setPositiveButton("Save",(d,w)->{Set<String> visible=new HashSet<>();for(int n=0;n<children.size();n++)if(selected[n])visible.add(String.valueOf(children.get(n)));profiles.members(id,visible);render();}).setNegativeButton("Cancel",null).show();
 }
}

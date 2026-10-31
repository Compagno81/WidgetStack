package com.kevin.widgetstack;

import android.app.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.widget.*;

public final class MainActivity extends Activity {
 @Override public void onCreate(Bundle b){super.onCreate(b);}
 @Override public void onResume(){super.onResume();SavedStacks.reconcile(this);refresh(false);render();}
 private void refresh(boolean reset){
  StackStore store=new StackStore(this);StackHost host=StackHost.get(this);host.sync();HostService.ensure(this);
  for(StackState s:store.saved()){
   if(reset){for(int child:s.children)host.track(child);for(int home:store.placements(s.id))SavedStacks.recoverControls(this,home);}
   StackRenderer.updateSizes(this,s.id);for(int home:store.placements(s.id))StackRenderer.update(this,home);
  }
 }
 void render(){
  StackStore store=new StackStore(this);LinearLayout root=Ui.page(this,"Widget Stack");
  Ui.text(this,root,"Build a stack of your favorite widgets, then place it on your home screen.",16);
  Ui.primaryButton(this,root,"Create saved stack",()->{StackState s=store.createSaved(null);startActivity(SavedStacks.edit(this,s.id));});
  Ui.section(this,root,"Your stacks");
  if(store.saved().isEmpty()){
   LinearLayout empty=Ui.card(this,root);Ui.text(this,empty,"No stacks yet",18);
   Ui.text(this,empty,"Create a stack and add widgets from apps installed on your phone.",14);
  }
  for(StackState s:store.saved()){
   LinearLayout card=Ui.card(this,root);
   TextView name=Ui.text(this,card,s.name,19);name.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));name.setOnClickListener(v->startActivity(SavedStacks.edit(this,s.id)));
   int copies=store.placements(s.id).size();Ui.text(this,card,s.children.size()+" widgets  ·  "+copies+" home-screen "+(copies==1?"copy":"copies"),14);
   LinearLayout row=Ui.row(this,card);
   Ui.compactButton(this,row,"Edit",()->startActivity(SavedStacks.edit(this,s.id)));
   Ui.compactButton(this,row,"Delete",()->{
    new AlertDialog.Builder(this).setTitle("Delete "+s.name+"?").setMessage("This deletes the saved stack and its configured widgets. Its home-screen copies will ask you to choose another saved stack.").setPositiveButton("Delete",(d,w)->{SavedStacks.delete(this,s.id);render();}).setNegativeButton("Cancel",null).show();
   });
  }
  Ui.text(this,root,"Home-screen copies share their widgets and settings. Each copy can show a different widget.",14);
  Ui.tonalButton(this,root,"Refresh widgets",()->{refresh(true);Toast.makeText(this,"Widgets refreshed",Toast.LENGTH_SHORT).show();});
  Ui.tonalButton(this,root,"About",()->new AlertDialog.Builder(this).setTitle("Widget Stack 1.22-reconstructed").setMessage("Home-screen widget stacks. Android 12 or newer.\n\nUses widget-stacking concepts and compatibility code adapted from Widgets Anywhere, © 2020/2023 Zachary Wander, MIT licence.\n\nAndroid Hidden API Bypass 6.1, Apache 2.0. Full licences are included in this app's assets and the source package.").setNeutralButton("App settings",(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())))).setPositiveButton("Done",null).show());
 }
}

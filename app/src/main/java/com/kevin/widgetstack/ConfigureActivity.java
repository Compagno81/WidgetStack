package com.kevin.widgetstack;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class ConfigureActivity extends Activity {
 private static final int BIND=51,CONFIGURE=52,RECONFIGURE=53,PICK=54,SWITCH_STACK=55;
 int stackId=-1,pending=-1; boolean existing,done,externalFlow;
 StackStore store;StackHost host;int homeId=-1;
 @Override public void onCreate(Bundle b){
  super.onCreate(b);setResult(RESULT_CANCELED);store=new StackStore(this);host=StackHost.get(this);
  store.migrateLegacy();homeId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
  int requested=getIntent().getIntExtra(SavedStacks.EXTRA_SAVED_ID,-1);
  if(store.isSaved(requested))stackId=requested;
  else if(StackRenderer.owns(this,homeId)&&store.get(homeId)!=null)stackId=store.definitionId(homeId);
  else{finish();return;}
  existing=true;
  if(b!=null){pending=b.getInt("pending",-1);externalFlow=b.getBoolean("externalFlow");}
  if(MeasuredViewport.ACTION.equals(getIntent().getAction()))MeasuredViewport.receive(this,getIntent());
  host.sync();HostService.ensure(this);
  if(homeId>=0)SavedStacks.recoverControls(this,homeId);
  else for(int home:store.placements(stackId))SavedStacks.recoverControls(this,home);
  render();
  Bundle draft=getIntent().getBundleExtra("resize_draft");
  if(draft!=null){StackState current=state();int child=draft.getInt("child",-1);if(current!=null&&current.children.contains(child))WidgetSizeEditor.show(this,current,child,()->{SavedStacks.changed(this,stackId);render();});}
 }
 @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putInt("pending",pending);b.putBoolean("existing",existing);b.putBoolean("externalFlow",externalFlow);}
 private StackState state(){return store.get(stackId);}
 private void changed(StackState s){store.put(s);SavedStacks.changed(this,stackId);render();}
 void render(){
  StackState s=state();if(s==null)return;
  LinearLayout root=Ui.page(this,"Edit saved stack");
  LinearLayout identity=Ui.card(this,root);
  TextView stackName=Ui.text(this,identity,s.name,20);stackName.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));stackName.setOnClickListener(v->{EditText name=new EditText(this);name.setText(s.name);new AlertDialog.Builder(this).setTitle("Stack name").setView(name).setPositiveButton("Save",(d,w)->{s.name=name.getText().toString().trim();if(s.name.isEmpty())s.name="Saved stack";changed(s);}).setNegativeButton("Cancel",null).show();});
  Ui.text(this,identity,"Changes apply to all home-screen copies. Tap the name to rename.",14);
  Ui.text(this,identity,store.placements(stackId).isEmpty()?"Preview uses a sample size until placed on your home screen.":"Resize each copy with your home-screen widget handles.",13);
  if(homeId>=0)Ui.text(this,identity,"Tasker instance_id: "+homeId+" · stack_id: "+stackId,14);
  if(homeId>=0&&StackRenderer.owns(this,homeId))Ui.button(this,root,"Use a different saved stack",()->startActivityForResult(new Intent(this,StackChooserActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,homeId).putExtra(StackChooserActivity.EXTRA_CHOOSE,true),SWITCH_STACK));
  Ui.section(this,root,"Navigation controls");
  LinearLayout nav=Ui.card(this,root);
  BarEditor.Preview preview=new BarEditor.Preview(this,s);nav.addView(preview,new LinearLayout.LayoutParams(-1,Ui.dp(this,190)));
  preview.setOnClickListener(v->BarEditor.show(this,s,()->changed(s)));
  Ui.tonalButton(this,nav,"Customize navigation bar",()->BarEditor.show(this,s,()->changed(s)));
  Ui.text(this,nav,"Tap a dot to choose a widget, or use the end buttons to move between widgets. Scrolling inside a widget stays with that widget.",13);
  Ui.section(this,root,"Widgets");
  Ui.text(this,root,"Tap a widget name to choose the starting widget for new copies. Resize each widget independently to request a compact or roomy provider layout. The result fits the stack without changing its proportions.",14);
  if(s.children.isEmpty()){LinearLayout empty=Ui.card(this,root);Ui.text(this,empty,"Add your first widget",17);}
  AppWidgetManager manager=AppWidgetManager.getInstance(this);
  for(int index=0;index<s.children.size();index++){
   final int pos=index,child=s.children.get(index);
   AppWidgetProviderInfo info=manager.getAppWidgetInfo(child);
   LinearLayout row=Ui.card(this,root);
   String label=info==null?"Unavailable widget":info.loadLabel(getPackageManager());
   TextView title=Ui.text(this,row,(s.selected==child?"● ":"○ ")+(index+1)+". "+label,17);
   title.setOnClickListener(v->{s.select(child);changed(s);});
   title.setOnLongClickListener(v->{v.startDragAndDrop(ClipData.newPlainText("widgetstack-order",Integer.toString(child)),new View.DragShadowBuilder(v),Integer.valueOf(child),0);return true;});
   row.setOnDragListener((v,e)->{
    if(e.getAction()==DragEvent.ACTION_DRAG_STARTED)return e.getLocalState() instanceof Integer;
    if(e.getAction()==DragEvent.ACTION_DROP&&e.getLocalState() instanceof Integer){int from=s.children.indexOf((Integer)e.getLocalState());s.move(from,pos);changed(s);return true;}
    return true;
   });
   LinearLayout controls=Ui.row(this,row);
   Button moveUp=Ui.compactButton(this,controls,"↑",()->{s.move(pos,pos-1);changed(s);});moveUp.setTextSize(22);moveUp.setContentDescription("Move up");moveUp.setEnabled(pos>0);
   Button moveDown=Ui.compactButton(this,controls,"↓",()->{s.move(pos,pos+1);changed(s);});moveDown.setTextSize(22);moveDown.setContentDescription("Move down");moveDown.setEnabled(pos<s.children.size()-1);
   if(info!=null&&info.configure!=null)Ui.compactButton(this,controls,"Set up",()->{
    try{host.startAppWidgetConfigureActivityForResult(this,child,0,RECONFIGURE,null);}catch(Exception e){message("This widget has no available setup screen.");}
   });
   Ui.compactButton(this,controls,"Remove",()->new AlertDialog.Builder(this).setMessage("Remove this widget from the stack?").setPositiveButton("Remove",(d,w)->{s.remove(child);store.put(s);host.deleteAppWidgetId(child);changed(s);}).setNegativeButton("Cancel",null).show());
   Ui.text(this,row,"Layout: "+(s.widthPx(child)==0?"Auto":s.widthPx(child)+" px")+" × "+(s.heightPx(child)==0?"Auto":s.heightPx(child)+" px"),14);
   if(s.logicalSizePercent(child)!=100)Ui.text(this,row,"Legacy percentage sizing is no longer used. Auto follows the launcher; set dimensions for a custom layout.",13);
   Ui.tonalButton(this,row,"Resize widget",()->WidgetSizeEditor.show(this,s,child,()->changed(s)));

  }
  Ui.button(this,root,"Add widget",this::pick).setEnabled(!externalFlow);
  Ui.text(this,root,"Reorder using the arrows, or hold a widget title and drag it onto another row.",14);
  Ui.primaryButton(this,root,"Done",()->{done=true;Intent result=new Intent().putExtra(SavedStacks.EXTRA_SAVED_ID,stackId);if(homeId>=0)result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,homeId);setResult(RESULT_OK,result);SavedStacks.changed(this,stackId);finish();});
 }
 private void pick(){
  startActivityForResult(new Intent(this,WidgetPickerActivity.class),PICK);
 }
 private void bind(AppWidgetProviderInfo info){
  if(pending!=-1)return;
  pending=host.allocateAppWidgetId();externalFlow=true;
  Bundle options=StackRenderer.hostedOptions(this,stackId);
  try{
   if(AppWidgetManager.getInstance(this).bindAppWidgetIdIfAllowed(pending,info.getProfile(),info.provider,options))configurePending();
   else startActivityForResult(new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,pending).putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,info.provider).putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE,info.getProfile()).putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS,options),BIND);
  }catch(Exception e){cancelPending();message("Android could not bind this widget: "+e.getClass().getSimpleName());}
 }
 private void configurePending(){
  AppWidgetProviderInfo info=AppWidgetManager.getInstance(this).getAppWidgetInfo(pending);
  if(info==null){cancelPending();return;}
  if(info.configure!=null)try{host.startAppWidgetConfigureActivityForResult(this,pending,0,CONFIGURE,null);}catch(Exception e){cancelPending();message("This widget's setup screen could not be opened.");}
  else completePending();
 }
 private void completePending(){
  if(pending<0)return;StackState s=state();if(s==null){cancelPending();return;}
  s.add(pending);pending=-1;externalFlow=false;changed(s);
 }
 private void cancelPending(){if(pending>=0)host.deleteAppWidgetId(pending);pending=-1;externalFlow=false;render();}
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(request==SWITCH_STACK){if(result==RESULT_OK){setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,homeId));finish();}}
  else if(request==PICK){if(result==RESULT_OK&&data!=null){AppWidgetProviderInfo info=data.getParcelableExtra("provider");if(info!=null)bind(info);}}
  else if(request==BIND){if(result==RESULT_OK)configurePending();else cancelPending();}
  else if(request==CONFIGURE){if(result==RESULT_OK)completePending();else cancelPending();}
  else if(request==RECONFIGURE){SavedStacks.changed(this,stackId);render();}
 }
 private void message(String text){new AlertDialog.Builder(this).setMessage(text).setPositiveButton("OK",null).show();}
 @Override protected void onDestroy(){
  if(isFinishing()&&pending>=0&&host!=null)host.deleteAppWidgetId(pending);
  super.onDestroy();
 }
}

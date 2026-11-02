package com.kevin.widgetstack;
import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.os.Bundle;
import android.util.SizeF;
import android.widget.*;
import java.util.*;

/** Shared editor viewport; never writes launcher/provider sizing options. */
final class PreviewViewport {
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("preview_viewport",0);}
 static int instance(Context c,int saved){List<Integer> ids=new StackStore(c).placements(saved);int chosen=prefs(c).getInt(saved+"_instance",-1);return ids.contains(chosen)?chosen:ids.isEmpty()?saved:ids.get(0);}
 static void choose(Context c,int saved,int id){prefs(c).edit().putInt(saved+"_instance",id).remove(saved+"_width").remove(saved+"_height").apply();}
 static void manual(Context c,int saved,int w,int h){prefs(c).edit().putInt(saved+"_width",w).putInt(saved+"_height",h).apply();}
 static Bundle options(Context c,int saved){
  int definition=new StackStore(c).definitionId(saved);Bundle b=new Bundle(StackRenderer.optionsFor(c,instance(c,definition)));int w=prefs(c).getInt(definition+"_width",0),h=prefs(c).getInt(definition+"_height",0);
  if(w>0&&h>0){float d=c.getResources().getDisplayMetrics().density;for(String k:new String[]{AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH})b.putInt(k,Math.max(1,Math.round(w/d)));for(String k:new String[]{AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT})b.putInt(k,Math.max(1,Math.round(h/d)));ArrayList<SizeF> sizes=new ArrayList<>();sizes.add(new SizeF(w/d,h/d));b.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,sizes);}return b;
 }
 static String label(Context c,int saved){SizeF size=StackRenderer.displaySize(options(c,saved),c.getResources().getConfiguration().orientation==2);float d=c.getResources().getDisplayMetrics().density;boolean custom=prefs(c).getInt(saved+"_width",0)>0;return "Preview: "+Math.round(size.getWidth()*d)+" × "+Math.round(size.getHeight()*d)+" px · "+(custom?"manual":MeasuredViewport.has(c,instance(c,saved))?"measured":"estimated");}
 static void controls(Activity a,LinearLayout root,int saved,Runnable changed){
  TextView label=Ui.text(a,root,label(a,saved),14);List<Integer> ids=new StackStore(a).placements(saved);
  if(!ids.isEmpty()){Spinner spinner=new Spinner(a);List<String> names=new ArrayList<>();for(int id:ids)names.add("Launcher instance "+id);spinner.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,names));spinner.setSelection(ids.indexOf(instance(a,saved)));root.addView(spinner);spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,android.view.View v,int n,long id){if(instance(a,saved)!=ids.get(n)){choose(a,saved,ids.get(n));label.setText(label(a,saved));changed.run();}}});}
  Ui.tonalButton(a,root,"Set preview dimensions",()->{LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);EditText w=new EditText(a),h=new EditText(a);w.setHint("Width (screen pixels)");h.setHint("Height (screen pixels)");w.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);h.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);SizeF size=StackRenderer.displaySize(options(a,saved),a.getResources().getConfiguration().orientation==2);float d=a.getResources().getDisplayMetrics().density;w.setText(""+Math.round(size.getWidth()*d));h.setText(""+Math.round(size.getHeight()*d));form.addView(w);form.addView(h);AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Preview dimensions").setView(form).setNegativeButton("Cancel",null).setNeutralButton("Use instance size",(x,y)->{choose(a,saved,instance(a,saved));label.setText(label(a,saved));changed.run();}).setPositiveButton("Apply",null).create();dialog.setOnShowListener(x->dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(v->{try{int width=Integer.parseInt(w.getText().toString()),height=Integer.parseInt(h.getText().toString());if(width<1||height<1||width>20000||height>20000)throw new NumberFormatException();manual(a,saved,width,height);label.setText(label(a,saved));changed.run();dialog.dismiss();}catch(NumberFormatException e){w.setError("Enter dimensions from 1 to 20000 px");}}));dialog.show();});
  Ui.tonalButton(a,root,"Match preview to launcher instance",()->{int id=instance(a,saved);if(id<0){Toast.makeText(a,"Place the stack on your launcher first.",Toast.LENGTH_LONG).show();return;}Bundle back=new Bundle();back.putBoolean("navigation_measure",true);try{WidgetPublisher.update(a,id,MeasuredViewport.probe(a,id,back));Toast.makeText(a,"Go home and tap the measurement panel to return to configuration.",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(a,"Could not show measurement panel.",Toast.LENGTH_LONG).show();}});
 }
}

package com.kevin.widgetstack;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.*;

/** Grouped native settings share one draft and a preview of the production widget layout. */
final class BarEditor {
 static void show(Activity a,StackState target,Runnable saved){
  StackState draft=new StackState(target.id);draft.railEdge=target.railEdge;draft.railWidth=target.railWidth;draft.railOpacity=target.railOpacity;draft.showRail=target.showRail;draft.animateSwitch=target.animateSwitch;draft.buttonsOnly=target.buttonsOnly;draft.overlapBar=target.overlapBar;draft.autoContract=target.autoContract;draft.contractedWidth=target.contractedWidth;draft.contractDelaySeconds=target.contractDelaySeconds;draft.contractToCorner=target.contractToCorner;draft.cornerEnd=target.cornerEnd;
  for(int id:target.children)draft.add(id);draft.select(target.selected);
  LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(Ui.dp(a,16),Ui.dp(a,8),Ui.dp(a,16),Ui.dp(a,8));
  Preview preview=new Preview(a,draft);root.addView(preview,new LinearLayout.LayoutParams(-1,Ui.dp(a,160)));preview.setOnClickListener(v->preview.demonstrateExpansion());
  TextView previewHint=new TextView(a);previewHint.setText("Live preview. Only stack controls respond; widget content is protected.");previewHint.setTextSize(12);root.addView(previewHint);
  RadioGroup tabs=new RadioGroup(a);tabs.setOrientation(LinearLayout.HORIZONTAL);tabs.setContentDescription("Settings section");root.addView(tabs);
  ScrollView scroll=new ScrollView(a);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout pages=new LinearLayout(a);pages.setOrientation(LinearLayout.VERTICAL);scroll.addView(pages);
  LinearLayout appearance=panel(a,pages),behaviour=panel(a,pages),animationPanel=panel(a,pages);LinearLayout[] sections={appearance,behaviour,animationPanel};
  String[] names={"Appearance","Behaviour","Animation"};
  for(int i=0;i<names.length;i++){
   RadioButton tab=choice(a,names[i]);tabs.addView(tab,new RadioGroup.LayoutParams(0,Ui.dp(a,48),1));final int section=i;
   tab.setOnCheckedChangeListener((button,on)->{if(on){for(int n=0;n<sections.length;n++)sections[n].setVisibility(n==section?View.VISIBLE:View.GONE);scroll.scrollTo(0,0);}});
  }
  ((RadioButton)tabs.getChildAt(0)).setChecked(true);
  Switch visible=toggle(a,appearance,"Show navigation bar",draft.showRail);
  Ui.text(a,appearance,"Position",16);RadioGroup edges=new RadioGroup(a);edges.setContentDescription("Bar position");edges.setOrientation(LinearLayout.HORIZONTAL);appearance.addView(edges);
  String[] labels={"Top","Bottom","Left","Right"};
  for(int i=0;i<labels.length;i++){RadioButton b=choice(a,labels[i]);edges.addView(b,new RadioGroup.LayoutParams(0,Ui.dp(a,48),1));final int edge=i;b.setChecked(i==draft.railEdge);b.setOnCheckedChangeListener((v,on)->{if(on){draft.railEdge=edge;preview.refresh();}});}
  TextView width=Ui.text(a,appearance,"",16);SeekBar thickness=slider(a,appearance,"Bar thickness",72,draft.railWidth-8);
  TextView alpha=Ui.text(a,appearance,"",16);SeekBar opacity=slider(a,appearance,"Bar opacity",100,draft.railOpacity);
  Switch buttons=toggle(a,behaviour,"Buttons only",draft.buttonsOnly),overlap=toggle(a,behaviour,"Overlap widget",draft.overlapBar),automatic=toggle(a,behaviour,"Auto-contract",draft.autoContract);
  LinearLayout compact=panel(a,behaviour);compact.setContentDescription("Contracted size settings");TextView compactLabel=Ui.text(a,compact,"",16);SeekBar compactSize=slider(a,compact,"Contracted bar thickness",draft.railWidth-5,draft.contractedWidth-4);
  Ui.text(a,compact,"Shrink to",16);RadioGroup shrink=new RadioGroup(a);shrink.setContentDescription("Shrink mode");shrink.setOrientation(LinearLayout.HORIZONTAL);compact.addView(shrink);
  RadioButton strip=choice(a,"Edge strip"),square=choice(a,"Corner square");shrink.addView(strip,new RadioGroup.LayoutParams(0,Ui.dp(a,48),1));shrink.addView(square,new RadioGroup.LayoutParams(0,Ui.dp(a,48),1));strip.setChecked(!draft.contractToCorner);square.setChecked(draft.contractToCorner);
  LinearLayout corners=panel(a,compact);corners.setContentDescription("Corner settings");Ui.text(a,corners,"Corner on the selected edge",16);RadioGroup ends=new RadioGroup(a);ends.setContentDescription("Square corner");ends.setOrientation(LinearLayout.HORIZONTAL);corners.addView(ends);
  RadioButton first=choice(a,"Top / left"),last=choice(a,"Bottom / right");ends.addView(first,new RadioGroup.LayoutParams(0,Ui.dp(a,48),1));ends.addView(last,new RadioGroup.LayoutParams(0,Ui.dp(a,48),1));first.setChecked(draft.cornerEnd==0);last.setChecked(draft.cornerEnd==1);
  corners.setVisibility(draft.contractToCorner?View.VISIBLE:View.GONE);
  strip.setOnCheckedChangeListener((v,on)->{if(on){draft.contractToCorner=false;corners.setVisibility(View.GONE);compactLabel.setText("Contracted bar thickness: "+draft.contractedWidth+" dp");preview.contracted=true;preview.refresh();}});
  square.setOnCheckedChangeListener((v,on)->{if(on){draft.contractToCorner=true;corners.setVisibility(View.VISIBLE);compactLabel.setText("Corner square size: "+draft.contractedWidth+" dp");preview.contracted=true;preview.refresh();}});
  first.setOnCheckedChangeListener((v,on)->{if(on){draft.cornerEnd=0;preview.contracted=true;preview.refresh();}});last.setOnCheckedChangeListener((v,on)->{if(on){draft.cornerEnd=1;preview.contracted=true;preview.refresh();}});
  Ui.text(a,compact,"Delay in seconds (minimum 2)",16);
  EditText delay=new EditText(a);delay.setContentDescription("Auto-contract delay in seconds");delay.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);delay.setSingleLine(true);delay.setText(String.valueOf(draft.contractDelaySeconds));compact.addView(delay,new LinearLayout.LayoutParams(-1,-2));
  delay.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){try{int seconds=Integer.parseInt(s.toString());if(seconds<2)throw new NumberFormatException();draft.contractDelaySeconds=seconds;delay.setError(null);}catch(NumberFormatException e){delay.setError("Enter whole seconds, 2 or more");}}public void afterTextChanged(android.text.Editable s){}});
  Ui.text(a,compact,"Tap the contracted strip to expand. Each navigation tap restarts the delay. Both shrink modes animate. The corner square sits inside a 48 dp corner tap target to keep it visible and easy to expand. Its visual size remains adjustable.",14);
  Ui.text(a,behaviour,"Buttons only gives each arrow half the bar. Overlap keeps the widget full size; the bar receives touches on the covered edge.",14);
  Switch animation=toggle(a,animationPanel,"Slide animation",draft.animateSwitch);animation.setText("Slide when switching widgets");Ui.text(a,animationPanel,"Slides the incoming widget when a dot or arrow changes the selection.",14);
  final boolean[] refreshing={false};Runnable refresh=()->{
   if(refreshing[0])return;refreshing[0]=true;draft.normalize();width.setText("Expanded bar thickness: "+draft.railWidth+" dp");alpha.setText("Opacity: "+draft.railOpacity+"%");compactLabel.setText((draft.contractToCorner?"Corner square size: ":"Contracted bar thickness: ")+draft.contractedWidth+" dp");compactSize.setMax(draft.railWidth-5);compactSize.setProgress(draft.contractedWidth-4);compact.setVisibility(draft.autoContract?View.VISIBLE:View.GONE);preview.refresh();refreshing[0]=false;
  };
  thickness.setOnSeekBarChangeListener(listener(value->{draft.railWidth=value+8;preview.contracted=false;refresh.run();}));opacity.setOnSeekBarChangeListener(listener(value->{draft.railOpacity=value;refresh.run();}));
  compactSize.setOnSeekBarChangeListener(listener(value->{if(!refreshing[0]){draft.contractedWidth=value+4;preview.contracted=true;refresh.run();}}));
  visible.setOnCheckedChangeListener((v,on)->{draft.showRail=on;refresh.run();});buttons.setOnCheckedChangeListener((v,on)->{draft.buttonsOnly=on;refresh.run();});overlap.setOnCheckedChangeListener((v,on)->{draft.overlapBar=on;refresh.run();});automatic.setOnCheckedChangeListener((v,on)->{draft.autoContract=on;preview.contracted=on;refresh.run();});animation.setOnCheckedChangeListener((v,on)->draft.animateSwitch=on);refresh.run();
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Navigation bar").setView(root).setPositiveButton("Apply",null).setNegativeButton("Cancel",null).create();
  dialog.setOnShowListener(d->{int height=Math.round(a.getResources().getDisplayMetrics().heightPixels*.85f);dialog.getWindow().setLayout(-1,height);});dialog.show();
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   if(draft.autoContract){try{int seconds=Integer.parseInt(delay.getText().toString());if(seconds<2)throw new NumberFormatException();draft.contractDelaySeconds=seconds;}catch(NumberFormatException e){delay.setError("Enter whole seconds, 2 or more");delay.requestFocus();return;}}
   target.railEdge=draft.railEdge;target.railWidth=draft.railWidth;target.railOpacity=draft.railOpacity;target.showRail=draft.showRail;target.animateSwitch=draft.animateSwitch;target.buttonsOnly=draft.buttonsOnly;target.overlapBar=draft.overlapBar;target.autoContract=draft.autoContract;target.contractedWidth=draft.contractedWidth;target.contractDelaySeconds=draft.contractDelaySeconds;target.contractToCorner=draft.contractToCorner;target.cornerEnd=draft.cornerEnd;saved.run();dialog.dismiss();
  });
 }
 private static LinearLayout panel(Activity a,LinearLayout parent){LinearLayout p=new LinearLayout(a);p.setOrientation(LinearLayout.VERTICAL);parent.addView(p,new LinearLayout.LayoutParams(-1,-2));return p;}
 private static RadioButton choice(Activity a,String label){RadioButton b=new RadioButton(a);b.setId(View.generateViewId());b.setText(label);b.setTextSize(12);b.setButtonDrawable(null);b.setGravity(android.view.Gravity.CENTER);b.setMinWidth(0);b.setMinimumWidth(0);b.setBackgroundResource(android.R.drawable.btn_default);b.setTextColor(new android.content.res.ColorStateList(new int[][]{{android.R.attr.state_checked},{}},new int[]{0xffffffff,Ui.TEXT}));b.setBackgroundTintList(new android.content.res.ColorStateList(new int[][]{{android.R.attr.state_checked},{}},new int[]{Ui.PRIMARY,Ui.SURFACE_TONAL}));b.setPadding(0,0,0,0);return b;}
 private static Switch toggle(Activity a,LinearLayout parent,String label,boolean checked){Switch s=new Switch(a);s.setText(label);s.setContentDescription(label);s.setTextSize(15);s.setChecked(checked);parent.addView(s,new LinearLayout.LayoutParams(-1,Ui.dp(a,48)));return s;}
 private static SeekBar slider(Activity a,LinearLayout parent,String label,int max,int value){SeekBar s=new SeekBar(a);s.setContentDescription(label);s.setMax(max);s.setProgress(value);parent.addView(s,new LinearLayout.LayoutParams(-1,-2));return s;}
 private interface Value{void changed(int n);}
 private static SeekBar.OnSeekBarChangeListener listener(Value value){return new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int n,boolean user){value.changed(n);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}};}
 static final class Preview extends StackPreview {Preview(Context c,StackState s){super(c,s);}}
}

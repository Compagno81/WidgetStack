package com.kevin.widgetstack;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

/** Called by the launcher when adding or reconfiguring a home-screen placement. */
public final class StackChooserActivity extends Activity {
    private static final int CREATE=61,EDIT=62;
    static final String EXTRA_CHOOSE="choose_different_stack";
    private boolean editing;
    int home=-1,pendingSaved=-1;
    @Override public void onCreate(Bundle b){
        super.onCreate(b);setResult(RESULT_CANCELED);
        home=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
        if(!StackRenderer.owns(this,home)){finish();return;}
        StackStore store=new StackStore(this);store.migrateLegacy();
        if(b!=null){pendingSaved=b.getInt("pendingSaved",-1);editing=b.getBoolean("editing",false);}
        else if(store.isPlacement(home)&&!getIntent().getBooleanExtra(EXTRA_CHOOSE,false)){
            editing=true;startActivityForResult(SavedStacks.edit(this,store.definitionId(home)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,home),EDIT);
        }
    }
    @Override protected void onResume(){super.onResume();if(!isFinishing()&&!editing)render();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putInt("pendingSaved",pendingSaved);b.putBoolean("editing",editing);}
    void render(){
        StackStore store=new StackStore(this);LinearLayout root=Ui.page(this,"Choose a saved stack");
        Ui.text(this,root,"Choose which saved stack to show here. Widgets and control settings are shared with its other copies; navigation is independent.",16);
        if(store.saved().isEmpty())Ui.text(this,root,"No saved stacks yet. Create one and choose its widgets and controls.",16);
        for(StackState s:store.saved()){
            String current=store.definitionId(home)==s.id?" · Currently used here":"";
            Ui.button(this,root,s.name+" · "+s.children.size()+" widgets"+current,()->choose(s.id));
        }
        Ui.button(this,root,"Create saved stack",()->{StackState s=store.createSaved(null);pendingSaved=s.id;startActivityForResult(SavedStacks.edit(this,s.id),CREATE);});
        Ui.button(this,root,"Cancel",this::finish);
    }
    void choose(int saved){
        if(!SavedStacks.place(this,home,saved)){Toast.makeText(this,"That stack is no longer available. Choose another.",Toast.LENGTH_SHORT).show();render();return;}
        setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,home));finish();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==EDIT){setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,home));finish();return;}if(request==CREATE&&result==RESULT_OK&&new StackStore(this).isSaved(pendingSaved))choose(pendingSaved);}
}

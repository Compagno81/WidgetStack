package com.kevin.widgetstack;

import android.app.*;
import android.appwidget.*;
import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;

/** Draft-only sizing with provider actions blocked; dismissal restores saved options. */
final class WidgetSizeEditor {
    static void show(Activity activity,StackState saved,int child,Runnable applied) {
        Session session=new Session(activity,saved,child,applied);session.show();
    }
    private static final class Session {
        final Activity activity;
        final StackState saved,draft;
        final int child;
        final Runnable applied;
        final Handler handler=new Handler(Looper.getMainLooper());
        final Dialog dialog;
        final SizingPreview preview;
        final EditText[] fields=new EditText[2];
        final SeekBar[] sliders=new SeekBar[2];
        final TextView dimensions;
        boolean syncing,closed;
        final Runnable refresh=this::requestProvider;
        final Runnable observer=()->renderPreview();
        Session(Activity activity,StackState saved,int child,Runnable applied) {
            this.activity=activity;this.saved=saved;this.child=child;this.applied=applied;
            draft=new StackState(saved.id);draft.add(child);draft.select(child);draft.showRail=false;
            draft.setDimensions(child,saved.widthPx(child),saved.heightPx(child));
            dialog=new Dialog(activity);dialog.setTitle("Resize widget");
            ScrollView scroll=new ScrollView(activity);LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);int padding=Ui.dp(activity,16);root.setPadding(padding,padding,padding,padding);scroll.addView(root);
            Ui.text(activity,root,"Resize widget",24);
            Ui.text(activity,root,"Width and height resize independently. Auto fills that axis of the stack. Larger sizes are clipped to the stack. Values are screen pixels, from 1 to 4000; sliders and dragging use 50 px steps.",14);
            dimensions=Ui.text(activity,root,"",14);
            preview=new SizingPreview(activity,this);root.addView(preview,new LinearLayout.LayoutParams(-1,Ui.dp(activity,260)));
            Ui.text(activity,root,"Drag the blue edges or corners. Provider actions are disabled in this preview.",13);
            axis(root,0,"Width");axis(root,1,"Height");
            LinearLayout buttons=Ui.row(activity,root);
            Ui.compactButton(activity,buttons,"Cancel",dialog::dismiss);
            Ui.compactButton(activity,buttons,"Apply",()->{
                if(!valid())return;
                // Apply to the newest definition so navigation or provider callbacks
                // cannot overwrite unrelated edits made while the dialog was open.
                StackState latest=new StackStore(activity).get(saved.id);
                if(latest==null||!latest.children.contains(child)){dialog.dismiss();return;}
                latest.setDimensions(child,draft.widthPx(child),draft.heightPx(child));
                new StackStore(activity).put(latest);
                saved.setDimensions(child,draft.widthPx(child),draft.heightPx(child));
                dialog.dismiss();applied.run();
            });
            dialog.setContentView(scroll);
            dialog.setOnDismissListener(d->{closed=true;handler.removeCallbacks(refresh);StackHost.get(activity).previewListeners.remove(observer);StackRenderer.updateSizes(activity,saved.id);});
            sync();
        }
        void show(){
            dialog.show();dialog.getWindow().setLayout(-1,-1);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            StackHost.get(activity).previewListeners.add(observer);requestProvider();
        }
        void axis(LinearLayout root,int axis,String label){
            Ui.text(activity,root,label+" (px)",17);
            LinearLayout row=Ui.row(activity,root);
            EditText field=new EditText(activity);fields[axis]=field;field.setSingleLine(true);field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);field.setHint("Auto");field.setContentDescription(label+" in pixels, blank for Auto");row.addView(field,new LinearLayout.LayoutParams(0,-2,2));
            Ui.compactButton(activity,row,"Auto",()->{set(axis,0);sync();schedule();});
            SeekBar slider=new SeekBar(activity);sliders[axis]=slider;slider.setMax(StackState.MAX_SIZE_PX/50);slider.setContentDescription(label+" in 50 pixel steps; zero is Auto");root.addView(slider,new LinearLayout.LayoutParams(-1,Ui.dp(activity,48)));
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(SeekBar bar,int progress,boolean user){if(user){set(axis,progress*50);sync();schedule();}}
                public void onStartTrackingTouch(SeekBar bar){}
                public void onStopTrackingTouch(SeekBar bar){}
            });
            field.addTextChangedListener(new TextWatcher(){
                public void beforeTextChanged(CharSequence s,int st,int count,int after){}
                public void onTextChanged(CharSequence s,int st,int before,int count){
                    if(syncing)return;
                    Integer value=parse(field);if(value==null)return;
                    set(axis,value);syncing=true;slider.setProgress(Math.round(value/50f));syncing=false;schedule();
                }
                public void afterTextChanged(Editable e){}
            });
        }
        Integer parse(EditText field){
            String text=field.getText().toString().trim();
            if(text.isEmpty()){field.setError(null);return 0;}
            try{int n=Integer.parseInt(text);if(n>=1&&n<=StackState.MAX_SIZE_PX){field.setError(null);return n;}}catch(NumberFormatException ignored){}
            field.setError("Enter 1–4000 px, or use Auto");return null;
        }
        boolean valid(){boolean w=parse(fields[0])!=null,h=parse(fields[1])!=null;return w&&h;}
        void set(int axis,int value){draft.setDimensions(child,axis==0?value:draft.widthPx(child),axis==1?value:draft.heightPx(child));}
        void sync(){syncing=true;for(int i=0;i<2;i++){
            if(fields[i]==null)continue;int value=i==0?draft.widthPx(child):draft.heightPx(child);
            fields[i].setText(value==0?"":Integer.toString(value));fields[i].setError(null);sliders[i].setProgress(Math.round(value/50f));
        }syncing=false;}
        void schedule(){handler.removeCallbacks(refresh);handler.postDelayed(refresh,180);renderPreview();}
        android.util.SizeF logicalSize(){
            android.os.Bundle actual=StackRenderer.hostedOptions(activity,saved.id);
            return StackRenderer.displaySize(WidgetSizing.options(actual,draft,child,activity.getResources().getDisplayMetrics().density),activity.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE);
        }
        void requestProvider(){
            if(closed)return;
            try{AppWidgetManager.getInstance(activity).updateAppWidgetOptions(child,WidgetSizing.options(StackRenderer.hostedOptions(activity,saved.id),draft,child,activity.getResources().getDisplayMetrics().density));}
            catch(RuntimeException e){dimensions.setText("Provider could not update its layout. Try Auto or reopen setup.");}
            renderPreview();
        }
        void renderPreview(){if(!closed)preview.refresh();}
    }
    private static final class SizingPreview extends FrameLayout {
        final Session session;
        final AppWidgetHostView host;
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF bounds=new RectF();
        float logicalWidth=240,logicalHeight=160,scale=1,startX,startY,startWidth,startHeight,startScale;
        int edges;
        SizingPreview(Context c,Session session){
            super(c);this.session=session;setWillNotDraw(false);setBackgroundColor(0xfff0f5fa);
            host=new AppWidgetHostView(c);host.setPadding(0,0,0,0);host.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);host.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);addView(host);
            setContentDescription("Non-interactive widget preview. Use width and height fields for accessible sizing.");
        }
        void refresh(){
            android.util.SizeF size=session.logicalSize();float density=getResources().getDisplayMetrics().density;
            logicalWidth=session.draft.widthPx(session.child)>0?session.draft.widthPx(session.child):size.getWidth()*density;logicalHeight=session.draft.heightPx(session.child)>0?session.draft.heightPx(session.child):size.getHeight()*density;
            android.widget.RemoteViews source=StackHost.get(getContext()).views.get(session.child);
            if(source!=null)try{host.updateAppWidget(RemoteViewsBridge.select(getContext(),source,size));}catch(Exception e){host.updateAppWidget(null);}
            session.dimensions.setText(Math.round(logicalWidth)+" × "+Math.round(logicalHeight)+" px requested; preview fits the editor.");
            requestLayout();invalidate();
        }
        @Override protected void onMeasure(int ws,int hs){
            setMeasuredDimension(MeasureSpec.getSize(ws),MeasureSpec.getSize(hs));
            host.measure(MeasureSpec.makeMeasureSpec(Math.max(1,Math.round(logicalWidth)),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,Math.round(logicalHeight)),MeasureSpec.EXACTLY));
        }
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
            float margin=24*getResources().getDisplayMetrics().density;
            scale=Math.min(1,WidgetSizing.fit(Math.max(1,getWidth()-margin*2),Math.max(1,getHeight()-margin*2),logicalWidth,logicalHeight));
            float left=(getWidth()-logicalWidth*scale)/2,top=(getHeight()-logicalHeight*scale)/2;
            host.layout(0,0,Math.round(logicalWidth),Math.round(logicalHeight));host.setPivotX(0);host.setPivotY(0);host.setScaleX(scale);host.setScaleY(scale);host.setTranslationX(left);host.setTranslationY(top);
            bounds.set(left,top,left+logicalWidth*scale,top+logicalHeight*scale);
        }
        @Override protected void dispatchDraw(Canvas canvas){
            super.dispatchDraw(canvas);paint.setColor(0xff61b9f4);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3*getResources().getDisplayMetrics().density);canvas.drawRect(bounds,paint);
            paint.setStyle(Paint.Style.FILL);float radius=5*getResources().getDisplayMetrics().density;
            for(float x:new float[]{bounds.left,bounds.centerX(),bounds.right})for(float y:new float[]{bounds.top,bounds.centerY(),bounds.bottom})if(x!=bounds.centerX()||y!=bounds.centerY())canvas.drawCircle(x,y,radius,paint);
        }
        @Override public boolean onInterceptTouchEvent(MotionEvent event){return true;}
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()==MotionEvent.ACTION_DOWN){
                float hit=24*getResources().getDisplayMetrics().density;edges=0;
                if(e.getY()>=bounds.top-hit&&e.getY()<=bounds.bottom+hit){if(Math.abs(e.getX()-bounds.left)<hit)edges|=1;else if(Math.abs(e.getX()-bounds.right)<hit)edges|=2;}
                if(e.getX()>=bounds.left-hit&&e.getX()<=bounds.right+hit){if(Math.abs(e.getY()-bounds.top)<hit)edges|=4;else if(Math.abs(e.getY()-bounds.bottom)<hit)edges|=8;}
                startX=e.getX();startY=e.getY();startWidth=logicalWidth;startHeight=logicalHeight;startScale=scale;
                if(edges!=0)getParent().requestDisallowInterceptTouchEvent(true);
            }else if(e.getAction()==MotionEvent.ACTION_MOVE&&edges!=0){
                if((edges&3)!=0)session.set(0,snap(startWidth+(e.getX()-startX)/startScale*((edges&1)!=0?-1:1)));
                if((edges&12)!=0)session.set(1,snap(startHeight+(e.getY()-startY)/startScale*((edges&4)!=0?-1:1)));
                session.sync();session.schedule();
            }else if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){edges=0;getParent().requestDisallowInterceptTouchEvent(false);if(e.getAction()==MotionEvent.ACTION_UP)performClick();}
            return true;
        }
        int snap(float value){return Math.max(50,Math.min(StackState.MAX_SIZE_PX,Math.round(value/50)*50));}
        @Override public boolean performClick(){super.performClick();return true;}
    }
}

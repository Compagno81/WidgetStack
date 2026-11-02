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
    static final class Session {
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
        int previewId;
        final Runnable refresh=this::requestProvider;
        android.widget.RemoteViews snapshot;
        final Runnable previewRender=this::refreshPreviewNow;
        Session(Activity activity,StackState saved,int child,Runnable applied) {
            this.activity=activity;this.saved=saved;this.child=child;this.applied=applied;snapshot=StackHost.get(activity).views.get(child);
            previewId=PreviewViewport.instance(activity,saved.id);
            draft=new StackState(saved.id);draft.add(child);draft.select(child);draft.showRail=false;
            draft.setDimensions(child,saved.widthPx(child),saved.heightPx(child));
            draft.setVisualScalePercent(child,saved.visualScalePercent(child));draft.setAlignment(child,saved.alignment(child));
            Bundle restored=activity.getIntent().getBundleExtra("resize_draft");
            if(restored!=null&&restored.getInt("child",-1)==child){draft.setDimensions(child,restored.getInt("width"),restored.getInt("height"));draft.setVisualScalePercent(child,restored.getInt("scale",100));draft.setAlignment(child,restored.getInt("alignment",4));previewId=restored.getInt("instance",previewId);activity.getIntent().removeExtra("resize_draft");}
            dialog=new Dialog(activity);dialog.setTitle("Resize widget");
            ScrollView scroll=new ScrollView(activity);LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);int padding=Ui.dp(activity,16);root.setPadding(padding,padding,padding,padding);scroll.addView(root);
            Ui.text(activity,root,"Resize widget",24);
            Ui.text(activity,root,"Width and height resize independently. Auto fills that axis of the stack. Larger sizes are clipped to the stack. Values are screen pixels, from 1 to 4000; sliders use 50 px steps.",14);
            dimensions=Ui.text(activity,root,"",14);
            preview=new SizingPreview(activity,this);root.addView(preview,new LinearLayout.LayoutParams(-1,Ui.dp(activity,260)));
            Ui.text(activity,root,"Preview shows the widget area for the selected instance. Use the controls below to resize. Provider actions are disabled.",13);
            Ui.tonalButton(activity,root,"Refresh preview snapshot",()->{snapshot=StackHost.get(activity).views.get(child);renderPreview();});
            Ui.text(activity,root,"Preview holds a snapshot while you edit. Refresh it to show newer provider content.",13);
            final TextView zoomLabel=Ui.text(activity,root,"Scale: "+draft.visualScalePercent(child)+"%",17);
            Ui.text(activity,root,"Zoom this widget proportionally. 100% is normal; enlarged edges may be clipped.",14);
            SeekBar zoom=new SeekBar(activity);zoom.setMax(275);zoom.setProgress(draft.visualScalePercent(child)-25);zoom.setContentDescription("Widget scale, 25 to 300 percent");root.addView(zoom,new LinearLayout.LayoutParams(-1,Ui.dp(activity,48)));
            zoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(SeekBar bar,int value,boolean user){draft.setVisualScalePercent(child,value+25);zoomLabel.setText("Scale: "+draft.visualScalePercent(child)+"%");renderPreview();}
                public void onStartTrackingTouch(SeekBar bar){} public void onStopTrackingTouch(SeekBar bar){}
            });
            Ui.compactButton(activity,Ui.row(activity,root),"Reset scale to 100%",()->zoom.setProgress(75));
            Ui.text(activity,root,"Alignment",17);
            Spinner alignment=new Spinner(activity);alignment.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,new String[]{"Top left","Top centre","Top right","Centre left","Centre","Centre right","Bottom left","Bottom centre","Bottom right"}));alignment.setSelection(draft.alignment(child));root.addView(alignment);
            alignment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int n,long id){draft.setAlignment(child,n);renderPreview();}});
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
                latest.setVisualScalePercent(child,draft.visualScalePercent(child));latest.setAlignment(child,draft.alignment(child));saved.setAlignment(child,draft.alignment(child));
                saved.setVisualScalePercent(child,draft.visualScalePercent(child));
                new StackStore(activity).put(latest);
                saved.setDimensions(child,draft.widthPx(child),draft.heightPx(child));
                dialog.dismiss();applied.run();
            });
            dialog.setContentView(scroll);
            dialog.setOnDismissListener(d->{closed=true;handler.removeCallbacks(refresh);handler.removeCallbacks(previewRender);StackRenderer.updateSizes(activity,saved.id);});
            sync();
        }
        Bundle returnDraft(){Bundle b=new Bundle();b.putInt("child",child);b.putInt("instance",previewId);b.putInt("width",draft.widthPx(child));b.putInt("height",draft.heightPx(child));b.putInt("scale",draft.visualScalePercent(child));b.putInt("alignment",draft.alignment(child));return b;}
        void show(){
            dialog.show();dialog.getWindow().setLayout(-1,-1);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            renderPreview();
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
        android.os.Bundle viewportOptions(){return StackRenderer.childOptions(PreviewViewport.options(activity,saved.id),saved);}

        android.util.SizeF logicalSize(){
            android.os.Bundle actual=viewportOptions();
            return StackRenderer.displaySize(WidgetSizing.options(actual,draft,child,activity.getResources().getDisplayMetrics().density),activity.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE);
        }
        void requestProvider(){
            if(closed)return;
            try{AppWidgetManager.getInstance(activity).updateAppWidgetOptions(child,WidgetSizing.options(StackRenderer.hostedOptions(activity,saved.id),draft,child,activity.getResources().getDisplayMetrics().density));}
            catch(RuntimeException e){dimensions.setText("Provider could not update its layout. Try Auto or reopen setup.");}
            renderPreview();
        }
        void refreshPreviewNow(){if(!closed)preview.refresh();}
        void renderPreview(){if(!closed){handler.removeCallbacks(previewRender);handler.postDelayed(previewRender,120);}}
    }
    static final java.util.concurrent.Executor PREVIEW_EXECUTOR=new java.util.concurrent.ThreadPoolExecutor(1,1,30,java.util.concurrent.TimeUnit.SECONDS,new java.util.concurrent.ArrayBlockingQueue<>(1),r->{Thread t=new Thread(r,"WidgetSizingPreview");t.setDaemon(true);return t;},new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy());
    static final class SizingPreview extends FrameLayout {
        final Session session;
        final AppWidgetHostView host;
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF bounds=new RectF();
        float logicalWidth=240,logicalHeight=160,scale=1,startX,startY,startWidth,startHeight,startScale;
        int edges;
        SizingPreview(Context c,Session session){
            super(c);this.session=session;setWillNotDraw(false);setBackgroundColor(0xfff0f5fa);
            host=new AppWidgetHostView(c);host.setExecutor(PREVIEW_EXECUTOR);host.setPadding(0,0,0,0);host.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);host.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);addView(host);
            setContentDescription("Non-interactive widget preview. Use width and height fields for accessible sizing.");
        }
        void refresh(){
            android.util.SizeF size=session.logicalSize();float density=getResources().getDisplayMetrics().density;
            android.util.SizeF viewport=StackRenderer.displaySize(session.viewportOptions(),getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE);
            logicalWidth=viewport.getWidth()*density;logicalHeight=viewport.getHeight()*density;
            android.widget.RemoteViews source=session.snapshot;
            if(source!=null)try{
                android.widget.RemoteViews frame=new android.widget.RemoteViews(getContext().getPackageName(),R.layout.sizing_preview);
                RemoteViewsBridge.embedAligned(getContext(),frame,source,session.child,size,session.draft.widthPx(session.child),session.draft.heightPx(session.child),session.draft.visualScalePercent(session.child),session.draft.alignment(session.child),viewport);
                host.updateAppWidget(frame);
            }catch(Exception e){host.updateAppWidget(null);}
            else host.updateAppWidget(null);
            session.dimensions.setText(Math.round(logicalWidth)+" × "+Math.round(logicalHeight)+" px widget area · "+(MeasuredViewport.has(getContext(),session.previewId)?"measured launcher size":"shared preview size")+". Preview reduced to fit.");
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
            return true;
        }
        int snap(float value){return Math.max(50,Math.min(StackState.MAX_SIZE_PX,Math.round(value/50)*50));}
        @Override public boolean performClick(){super.performClick();return true;}
    }
}

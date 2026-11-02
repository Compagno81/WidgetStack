package com.kevin.widgetstack;

import android.appwidget.*;
import android.content.Context;
import android.os.*;
import android.view.*;
import android.widget.*;

/** The production RemoteViews, scaled to fit, behind a touch shield. Only local stack controls act. */
class StackPreview extends FrameLayout {
    final StackState state;
    boolean contracted;
    private final AppWidgetHostView host;
    private int homeWidth,homeHeight,generation;
    private float scale=1,offsetX,offsetY;
    private boolean barGesture,observing;
    private int barMotion;
    private long animationUntil;
    private final Runnable observer=()->{if(SystemClock.uptimeMillis()>=animationUntil)refresh();};
    StackPreview(Context c,StackState s){
        super(c);state=s;contracted=s.autoContract;
        host=new AppWidgetHostView(c);host.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);host.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);host.setPadding(0,0,0,0);addView(host);
        setContentDescription("Widget preview using current home-screen dimensions. Preview only. Widget content is not interactive; stack navigation controls remain active.");
        refresh();
    }
    void refresh(){
        state.normalize();AppWidgetManager manager=AppWidgetManager.getInstance(getContext());Bundle options=PreviewViewport.options(getContext(),state.id);
        boolean landscape=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        float density=getResources().getDisplayMetrics().density;
        homeWidth=Math.round(Math.max(1,options.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH:AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,240))*density);
        homeHeight=Math.round(Math.max(1,options.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT:AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,160))*density);
        boolean compact=state.autoContract&&state.showRail&&contracted;
        AppWidgetProviderInfo own=state.id>=0?manager.getAppWidgetInfo(state.id):null;
        boolean available=StackRenderer.owns(getContext(),state.id)||new StackStore(getContext()).isSaved(state.id);
        boolean live=available&&!state.children.isEmpty()&&StackHost.get(getContext()).views.containsKey(state.selected);
        if(own!=null)host.setAppWidget(state.id,own);host.setPadding(0,0,0,0);
        android.widget.RemoteViews views=live?StackRenderer.renderPreview(getContext(),state,compact,barMotion):StackRenderer.shell(getContext(),state,0,compact,barMotion);
        if(!live){views.setTextViewText(R.id.empty,state.children.isEmpty()?"Add a widget for a live preview":"Waiting for the selected widget to load");views.setOnClickPendingIntent(R.id.empty,null);}
        host.updateAppWidget(views);requestLayout();invalidate();
    }
    void demonstrateExpansion(){
        boolean animate=state.autoContract&&(contracted||barMotion<0);int token=++generation;
        contracted=false;barMotion=animate?1:0;animationUntil=animate?SystemClock.uptimeMillis()+BarController.ANIMATION_MS:0;refresh();
        if(animate)postDelayed(()->{if(token==generation){barMotion=0;animationUntil=0;refresh();}},BarController.ANIMATION_MS);
        if(state.autoContract)postDelayed(()->{
            if(token!=generation||!state.autoContract)return;
            barMotion=-1;animationUntil=SystemClock.uptimeMillis()+BarController.ANIMATION_MS;refresh();
            postDelayed(()->{if(token==generation){contracted=true;barMotion=0;animationUntil=0;refresh();}},BarController.ANIMATION_MS);
        },state.contractDelayMillis());
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(StackRenderer.owns(getContext(),state.id)||new StackStore(getContext()).isSaved(state.id)){StackHost.get(getContext()).previewListeners.add(observer);observing=true;}}
    @Override protected void onDetachedFromWindow(){generation++;if(observing){StackHost.get(getContext()).previewListeners.remove(observer);observing=false;}super.onDetachedFromWindow();}
    @Override protected void onMeasure(int ws,int hs){
        int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);setMeasuredDimension(w,h);
        host.measure(MeasureSpec.makeMeasureSpec(homeWidth,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(homeHeight,MeasureSpec.EXACTLY));
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        host.layout(0,0,homeWidth,homeHeight);float margin=4*getResources().getDisplayMetrics().density;
        scale=Math.max(.01f,Math.min((getWidth()-2*margin)/homeWidth,(getHeight()-2*margin)/homeHeight));offsetX=(getWidth()-homeWidth*scale)/2;offsetY=(getHeight()-homeHeight*scale)/2;
        host.setPivotX(0);host.setPivotY(0);host.setScaleX(scale);host.setScaleY(scale);host.setTranslationX(offsetX);host.setTranslationY(offsetY);
    }
    private boolean onBar(float x,float y){
        if(!state.showRail||x<0||y<0||x>homeWidth||y>homeHeight)return false;
        float density=getResources().getDisplayMetrics().density;
        if(state.cornerArrows){
            float size=Math.max(48,state.cornerArrowSize)*density;
            boolean end=state.horizontalRail()?(x<size||x>=homeWidth-size):(y<size||y>=homeHeight-size);
            return end&&(state.railEdge==StackState.TOP?y<size:state.railEdge==StackState.BOTTOM?y>=homeHeight-size:state.railEdge==StackState.LEFT?x<size:x>=homeWidth-size);
        }
        if(state.autoContract&&contracted&&state.contractToCorner){
            float target=Math.max(48,state.contractedWidth)*density;boolean left=state.railEdge==StackState.LEFT||(state.horizontalRail()&&state.cornerEnd==0),top=state.railEdge==StackState.TOP||(!state.horizontalRail()&&state.cornerEnd==0);
            return (left?x<target:x>=homeWidth-target)&&(top?y<target:y>=homeHeight-target);
        }
        float width=(state.autoContract&&contracted?state.contractedWidth:state.railWidth)*density;
        switch(state.railEdge){case StackState.TOP:return y<width;case StackState.BOTTOM:return y>=homeHeight-width;case StackState.LEFT:return x<width;default:return x>=homeWidth-width;}
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent event){
        if(event.getAction()==MotionEvent.ACTION_DOWN)barGesture=onBar((event.getX()-offsetX)/scale,(event.getY()-offsetY)/scale);
        return true; // Shield all hosted content, including clicks, scrolls and long presses.
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(!barGesture)return true;
        if(event.getAction()==MotionEvent.ACTION_UP){
            float x=(event.getX()-offsetX)/scale,y=(event.getY()-offsetY)/scale;
            if(onBar(x,y)){
                if(state.cornerArrows){state.step((state.horizontalRail()?x/homeWidth:y/homeHeight)<.5f?-1:1);refresh();return true;}
                if(!state.autoContract||!contracted){
                    float fraction=state.horizontalRail()?x/homeWidth:y/homeHeight;
                    if(state.buttonsOnly)state.step(fraction<.5f?-1:1);
                    else if(fraction<.25f)state.step(-1);else if(fraction>.75f)state.step(1);
                    else if(fraction<.65f&&!state.children.isEmpty())state.select(state.children.get(Math.min(state.children.size()-1,(int)((fraction-.25f)/.4f*state.children.size()))));
                    else performClick();
                }
                demonstrateExpansion();
            }
            barGesture=false;
        }else if(event.getAction()==MotionEvent.ACTION_CANCEL)barGesture=false;
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}

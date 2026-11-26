package com.kevin.widgetstack;

import android.app.Application;
import android.content.Context;
import android.os.Parcel;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35}, application=Application.class, qualifiers="mdpi")
public class PairedBarsTest {
    @Test public void bothEdgesResizeAndFadeAfterParcelAndReapply(){
        Context c=RuntimeEnvironment.getApplication();
        StackState state=new StackState(1);
        state.add(10);
        state.add(20);
        state.pairedBars=true;
        state.autoContract=true;
        state.hiddenOpacity=0;
        for(boolean horizontal:new boolean[]{false,true}){
            state.topBottomPair=horizontal;
            FrameLayout parent=new FrameLayout(c);
            View root=StackRenderer.shell(c,state,0,false).apply(c,parent);
            for(boolean compact:new boolean[]{true,false}){
                RemoteViews shell=StackRenderer.shell(c,state,0,compact);
                Parcel parcel=Parcel.obtain();
                try{
                    shell.writeToParcel(parcel,0);
                    parcel.setDataPosition(0);
                    RemoteViews.CREATOR.createFromParcel(parcel).reapply(c,root);
                }finally{parcel.recycle();}
                int width=compact?state.contractedWidth:state.railWidth;
                root.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.EXACTLY));
                root.layout(0,0,400,300);
                View previous=root.findViewById(R.id.previous),next=root.findViewById(R.id.next);
                assertEquals(width,horizontal?previous.getHeight():previous.getWidth());
                assertEquals(horizontal?400:300,horizontal?previous.getWidth():previous.getHeight());
                assertEquals(horizontal?300-width:400-width,horizontal?next.getTop():next.getLeft());
                assertEquals(compact?0f:state.railOpacity/100f,next.getAlpha(),.001f);
                assertTrue(next.hasOnClickListeners());
                assertEquals(horizontal?400:400-2*width,root.findViewById(R.id.content).getWidth());
                assertEquals(horizontal?300-2*width:300,root.findViewById(R.id.content).getHeight());
            }
        }
    }
}

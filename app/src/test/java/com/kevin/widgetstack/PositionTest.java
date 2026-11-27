package com.kevin.widgetstack;

import android.app.Application;
import android.content.Context;
import android.os.Parcel;
import android.util.SizeF;
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
@Config(sdk={31,35},application=Application.class)
public class PositionTest {
    @Test public void explicitOffsetsReplaceOldAnchorAndSurviveMergedUpdate() throws Exception {
        Context c=RuntimeEnvironment.getApplication();
        StackState state=new StackState(-2);
        state.add(11);state.add(22);
        state.setAlignment(11,8);
        state.setPosition(11,-32,19);
        state=StackStore.copy(state);
        assertEquals(4,state.alignment(11));
        assertFalse(state.shifted(22));
        SizeF slot=new SizeF(200,140);
        RemoteViews provider=new RemoteViews("android",android.R.layout.simple_list_item_1);
        RemoteViews outer=new RemoteViews(c.getPackageName(),R.layout.sizing_preview);
        RemoteViewsBridge.embedPositioned(c,outer,provider,11,slot,state,slot);
        View root=outer.apply(c,new FrameLayout(c));
        View inner=root.findViewById(R.id.widget_scale_container);
        assertEquals(-32,inner.getTranslationX(),0f);
        assertEquals(19,inner.getTranslationY(),0f);
        state.setPosition(11,14,-7);
        RemoteViewsBridge.embedPositioned(c,outer,provider,11,slot,state,slot);
        Parcel parcel=Parcel.obtain();
        try{
            outer.writeToParcel(parcel,0);parcel.setDataPosition(0);
            RemoteViews.CREATOR.createFromParcel(parcel).reapply(c,root);
        }finally{parcel.recycle();}
        inner=root.findViewById(R.id.widget_scale_container);
        assertEquals(14,inner.getTranslationX(),0f);
        assertEquals(-7,inner.getTranslationY(),0f);
    }
}

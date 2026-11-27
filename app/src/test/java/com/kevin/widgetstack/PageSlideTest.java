package com.kevin.widgetstack;

import android.app.Application;
import android.content.Context;
import android.os.Parcel;
import android.util.SizeF;
import android.view.View;
import android.view.ViewGroup;
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
public class PageSlideTest {
    @Test public void slideWrapsOneLivePageAfterParcelAndReapply() throws Exception {
        Context c=RuntimeEnvironment.getApplication();
        StackState s=new StackState(-2);
        s.add(11);s.pairedBars=true;
        for(boolean vertical:new boolean[]{true,false})for(int direction:new int[]{1,-1}){
            s.topBottomPair=vertical;
            RemoteViews shell=StackRenderer.shell(c,s,0);
            RemoteViews widget=new RemoteViews("android",android.R.layout.simple_list_item_1);
            RemoteViewsBridge.embedWithSizing(c,shell,widget,11,new SizeF(200,100),null,PageSlide.layout(s,direction));
            Parcel parcel=Parcel.obtain();
            try{
                shell.writeToParcel(parcel,0);parcel.setDataPosition(0);
                RemoteViews copy=RemoteViews.CREATOR.createFromParcel(parcel);
                View root=copy.apply(c,new FrameLayout(c));
                copy.reapply(c,root);
                ViewGroup page=root.findViewById(R.id.sliding_page);
                assertEquals(1,page.getChildCount());
                assertNotNull(page.getLayoutAnimation());
                assertNotNull(page.findViewById(android.R.id.text1));
                assertNull(root.findViewById(R.id.transition));
            }finally{parcel.recycle();}
        }
        assertEquals(0,PageSlide.layout(s,0));
    }
}

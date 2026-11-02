package com.kevin.widgetstack;
import android.app.Application;
import android.content.Context;
import android.util.SizeF;
import android.view.View;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class)
public class AlignmentTest {
 @Test public void transformedEdgesStayAnchoredForShrinkAndEnlargement() throws Exception {
  Context c=RuntimeEnvironment.getApplication();float d=c.getResources().getDisplayMetrics().density;
  for(int percent:new int[]{50,150})for(int a=0;a<9;a++){
   RemoteViews root=new RemoteViews(c.getPackageName(),R.layout.sizing_preview);
   RemoteViewsBridge.embedAligned(c,root,new RemoteViews("android",android.R.layout.simple_list_item_1),11,new SizeF(200/d,100/d),200,100,percent,a,new SizeF(400/d,300/d));
   View v=root.apply(c,new FrameLayout(c));v.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.EXACTLY));v.layout(0,0,400,300);
   View child=v.findViewById(R.id.widget_scale_container);float scale=percent/100f;
   float x=child.getLeft()+child.getTranslationX()+child.getPivotX()*(1-scale),y=child.getTop()+child.getTranslationY()+child.getPivotY()*(1-scale);
   assertEquals((400-200*scale)*(a%3)/2f,x,.01f);assertEquals((300-100*scale)*(a/3)/2f,y,.01f);
  }
 }
 @Test public void alignmentSurvivesStorageAndIsIndependentPerWidget(){Context c=RuntimeEnvironment.getApplication();StackStore store=new StackStore(c);StackState s=store.createSaved("Test");s.add(11);s.add(22);s.setAlignment(11,8);store.put(s);assertEquals(8,store.get(s.id).alignment(11));assertEquals(4,store.get(s.id).alignment(22));}
}

package com.kevin.widgetstack;
import android.app.Application;
import android.content.Context;
import android.view.*;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class,qualifiers="mdpi")
public class CornerArrowsTest {
 @Test public void cornersOverlayFullContentAtEveryEdgeAndSize(){
  Context c=RuntimeEnvironment.getApplication();
  for(int edge=0;edge<4;edge++)for(int size:new int[]{16,32,80}){
   StackState s=new StackState(-2);s.add(11);s.add(22);s.cornerArrows=true;s.railEdge=edge;s.cornerArrowSize=size;s.autoContract=true;
   View root=StackRenderer.shell(c,s).apply(c,new FrameLayout(c));
   root.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.EXACTLY));root.layout(0,0,400,300);
   assertEquals(400,root.findViewById(R.id.content).getWidth());assertEquals(300,root.findViewById(R.id.content).getHeight());
   TextView previous=root.findViewById(R.id.previous),next=root.findViewById(R.id.next);
   assertEquals(Math.max(48,size),previous.getWidth());assertEquals(Math.max(48,size),next.getHeight());
   assertEquals(s.horizontalRail()?"←":"↑",previous.getText().toString());assertEquals(s.horizontalRail()?"→":"↓",next.getText().toString());
   if(s.horizontalRail()){assertEquals(0,previous.getLeft());assertEquals(400,next.getRight());}else{assertEquals(0,previous.getTop());assertEquals(300,next.getBottom());}
   assertFalse(BarController.contracted(s));assertTrue(previous.performClick());assertTrue(next.performClick());
  }
 }
 @Test public void choiceAndSizeSurviveSave(){
  StackStore store=new StackStore(RuntimeEnvironment.getApplication());StackState s=store.createSaved("Corner arrows");s.cornerArrows=true;s.cornerArrowSize=64;s.railEdge=StackState.TOP;store.put(s);
  StackState restored=store.get(s.id);assertTrue(restored.cornerArrows);assertEquals(64,restored.cornerArrowSize);assertEquals(StackState.TOP,restored.railEdge);
 }
}

package com.kevin.widgetstack;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class,qualifiers="mdpi")
public class PreviewViewportTest {
 @Test public void manualViewportIsSharedAndDoesNotChangeLauncherOrWidgetDimensions(){
  Context c=RuntimeEnvironment.getApplication();StackStore store=new StackStore(c);StackState s=store.createSaved("Shared");s.add(11);s.setDimensions(11,200,150);store.put(s);store.assign(701,s.id);store.assign(702,s.id);
  PreviewViewport.choose(c,s.id,702);PreviewViewport.manual(c,s.id,800,600);
  assertEquals(702,PreviewViewport.instance(c,s.id));
  android.util.SizeF size=StackRenderer.displaySize(PreviewViewport.options(c,s.id),false);assertEquals(800,size.getWidth(),0);assertEquals(600,size.getHeight(),0);
  Bundle content=StackRenderer.childOptions(PreviewViewport.options(c,s.id),s);
  android.util.SizeF slot=StackRenderer.displaySize(content,false);assertEquals(800-s.railWidth,slot.getWidth(),0);
  assertEquals(200,store.get(s.id).widthPx(11));assertEquals(150,store.get(s.id).heightPx(11));
  assertFalse(PreviewViewport.label(c,s.id).isEmpty());assertTrue(PreviewViewport.label(c,s.id).contains("800 × 600"));
  PreviewViewport.choose(c,s.id,701);assertEquals(701,PreviewViewport.instance(c,s.id));assertEquals(0,PreviewViewport.prefs(c).getInt(s.id+"_width",0));
 }
 @Test public void deletedPreviewInstanceFallsBackToRemainingCopy(){Context c=RuntimeEnvironment.getApplication();StackStore store=new StackStore(c);StackState s=store.createSaved("Fallback");store.assign(701,s.id);store.assign(702,s.id);PreviewViewport.choose(c,s.id,702);store.delete(702);assertEquals(701,PreviewViewport.instance(c,s.id));}
}

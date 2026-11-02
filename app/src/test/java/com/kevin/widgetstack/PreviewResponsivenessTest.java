package com.kevin.widgetstack;
import android.app.*;
import android.os.Looper;
import android.widget.RemoteViews;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class)
public class PreviewResponsivenessTest {
 @Test public void providerStormDoesNotReplaceEditingSnapshotOrRegisterLiveObserver(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();StackStore store=new StackStore(a);StackState s=store.createSaved("Test");s.add(11);store.put(s);
  StackHost host=StackHost.get(a);RemoteViews first=new RemoteViews("android",android.R.layout.simple_list_item_1);host.views.put(11,first);int observers=host.previewListeners.size();
  WidgetSizeEditor.Session editor=new WidgetSizeEditor.Session(a,s,11,()->{});editor.show();assertEquals(observers,host.previewListeners.size());
  for(int n=0;n<100;n++){host.views.put(11,new RemoteViews("android",android.R.layout.simple_list_item_1));for(Runnable listener:new java.util.ArrayList<>(host.previewListeners))listener.run();}
  assertSame(first,editor.snapshot);
  for(int n=0;n<100;n++)editor.renderPreview();assertTrue(editor.handler.hasCallbacks(editor.previewRender));
  editor.dialog.dismiss();org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(editor.handler.hasCallbacks(editor.previewRender));editor.renderPreview();assertFalse(editor.handler.hasCallbacks(editor.previewRender));
 }
}

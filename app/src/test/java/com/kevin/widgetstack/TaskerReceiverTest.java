package com.kevin.widgetstack;
import android.app.Application;
import android.content.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class)
public class TaskerReceiverTest {
 Context c;StackStore store;int saved;
 @Before public void setup(){c=RuntimeEnvironment.getApplication();store=new StackStore(c);StackState s=store.createSaved("Calendar");s.add(11);s.add(22);store.put(s);saved=s.id;store.assign(101,saved);store.assign(102,saved);TaskerReceiver.enable(c,true);}
 Intent command(){return new Intent(TaskerReceiver.ACTION).putExtra("position","2");}
 @Test public void instanceSelectionDoesNotChangeOtherCopiesOrDefault(){assertEquals(1,TaskerReceiver.select(c,command().putExtra("instance_id","101")).size());assertEquals(22,store.get(101).selected);assertEquals(11,store.get(102).selected);assertEquals(11,store.get(saved).selected);}
 @Test public void savedTargetSelectsEveryExistingCopy(){assertEquals(2,TaskerReceiver.select(c,command().putExtra("stack_id",saved)).size());assertEquals(22,store.get(101).selected);assertEquals(22,store.get(102).selected);}
 @Test public void disabledAndInvalidCommandsDoNothing(){
  TaskerReceiver.enable(c,false);assertTrue(TaskerReceiver.select(c,command().putExtra("instance_id",101)).isEmpty());TaskerReceiver.enable(c,true);
  assertTrue(TaskerReceiver.select(c,command()).isEmpty());
  assertTrue(TaskerReceiver.select(c,command().putExtra("instance_id",101).putExtra("stack_id",saved)).isEmpty());
  for(String position:new String[]{"0","-1","3","bad","2147483648"})assertTrue(TaskerReceiver.select(c,command().putExtra("instance_id",101).putExtra("position",position)).isEmpty());
  assertTrue(TaskerReceiver.select(c,command().putExtra("instance_id",999)).isEmpty());
  assertTrue(TaskerReceiver.select(c,command().putExtra("instance_id",101).setAction("other")).isEmpty());assertEquals(11,store.get(101).selected);
 }
}

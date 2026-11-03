package com.kevin.widgetstack;
import android.app.Application;
import android.content.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class)
public class VisibilityProfilesTest {
 Context c;StackStore store;VisibilityProfiles profiles;StackState a,b;
 @Before public void setup(){c=RuntimeEnvironment.getApplication();store=new StackStore(c);profiles=new VisibilityProfiles(c);a=store.createSaved("Work");a.add(11);a.add(22);a.add(33);store.put(a);store.assign(701,a.id);store.assign(702,a.id);b=store.createSaved("Other");b.add(44);b.add(55);store.put(b);}
 Set<String> only(int...ids){Set<String>s=new HashSet<>();for(int id:ids)s.add(String.valueOf(id));return s;}
 @Test public void activationFiltersAllCopiesWithoutDeletingBindingsOrSettings(){int id=profiles.create(a.id,"Focus");profiles.members(id,only(22));assertTrue(profiles.activate(a.id,id));for(int home:new int[]{701,702}){assertEquals(Arrays.asList(22),VisibilityProfiles.filtered(c,store.get(home)).children);assertEquals(22,store.get(home).selected);}assertEquals(Arrays.asList(11,22,33),store.get(a.id).children);profiles.activate(a.id,0);assertEquals(3,VisibilityProfiles.filtered(c,store.get(701)).children.size());}
 @Test public void profilesAreIndependentAndRejectCrossStackActivation(){int x=profiles.create(a.id,"Work"),y=profiles.create(b.id,"Work");profiles.members(x,only(11));profiles.members(y,only(55));profiles.activate(a.id,x);profiles.activate(b.id,y);assertFalse(profiles.activate(b.id,x));assertEquals(y,profiles.active(b.id));assertEquals(Arrays.asList(11),VisibilityProfiles.filtered(c,a).children);assertEquals(Arrays.asList(55),VisibilityProfiles.filtered(c,b).children);}
 @Test public void emptyProfileIsSafeAndReorderingPreservesMembership(){int id=profiles.create(a.id,"Empty");profiles.members(id,only());profiles.activate(a.id,id);assertTrue(VisibilityProfiles.filtered(c,a).children.isEmpty());assertEquals(-1,VisibilityProfiles.filtered(c,a).selected);assertNotNull(StackRenderer.render(c,store.get(701),0,false));profiles.members(id,only(11,33));a.move(2,0);store.put(a);assertEquals(Arrays.asList(33,11),VisibilityProfiles.filtered(c,store.get(a.id)).children);}
 @Test public void renameKeepsIdAndDeletingActiveProfileRestoresAll(){int id=profiles.create(a.id,"Old");profiles.members(id,only(22));profiles.activate(a.id,id);profiles.rename(id,"New");assertEquals("New",profiles.activeName(a.id));profiles.delete(id);assertEquals(0,profiles.active(a.id));assertEquals(3,VisibilityProfiles.filtered(c,a).children.size());assertTrue(profiles.create(a.id,"Next")>id);assertFalse(profiles.activate(a.id,id));}
 @Test public void taskerActivationRequiresOptInAndKeepsOldPositionProtocol(){int id=profiles.create(a.id,"Only last");profiles.members(id,only(33));Intent fire=new Intent(TaskerPlugin.FIRE).putExtra(TaskerPlugin.BUNDLE,TaskerPlugin.profileConfiguration(a.id,id));Intent command=TaskerPlugin.command(fire);assertNotNull(command);new TaskerReceiver().onReceive(c,command);assertEquals(0,profiles.active(a.id));TaskerReceiver.enable(c,true);new TaskerPluginReceiver().onReceive(c,fire);assertEquals(id,profiles.active(a.id));assertEquals(33,store.get(701).selected);assertNotNull(TaskerPlugin.command(new Intent(TaskerPlugin.FIRE).putExtra(TaskerPlugin.BUNDLE,TaskerPlugin.configuration("instance_id",701,1))));}
 @Test public void taskerPositionsSkipHiddenChildrenAndNewChildrenRequireMembership(){int id=profiles.create(a.id,"Two");profiles.members(id,only(11,33));profiles.activate(a.id,id);TaskerReceiver.enable(c,true);TaskerReceiver.select(c,new Intent(TaskerReceiver.ACTION).putExtra("instance_id",701).putExtra("position",2));assertEquals(33,store.get(701).selected);assertEquals(11,store.get(702).selected);a.add(66);store.put(a);assertEquals(Arrays.asList(11,33),VisibilityProfiles.filtered(c,store.get(a.id)).children);}
}

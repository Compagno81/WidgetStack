package com.kevin.widgetstack;

import android.app.Application;
import android.content.Context;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class)
public class SavedStackStoreTest {
 private Context c(){return RuntimeEnvironment.getApplication();}
 private StackStore store(){return new StackStore(c());}
 private StackState saved(String name){StackState s=store().createSaved(name);s.add(11);s.add(22);store().put(s);return s;}
 @Test public void createWithoutHomePlacementRetainsWidgetsAndAllControls(){
  StackState s=saved("Daily");s.railEdge=StackState.TOP;s.railWidth=64;s.railOpacity=42;s.showRail=false;s.animateSwitch=true;s.buttonsOnly=true;s.overlapBar=true;s.autoContract=true;s.contractedWidth=18;s.contractDelaySeconds=12;s.contractToCorner=true;s.cornerEnd=1;s.select(22);store().put(s);
  StackState r=store().get(s.id);assertTrue(s.id<=-2);assertEquals("Daily",r.name);assertEquals(Arrays.asList(11,22),r.children);assertEquals(22,r.selected);assertEquals(0,r.railEdge);assertEquals(64,r.railWidth);assertEquals(42,r.railOpacity);assertFalse(r.showRail);assertTrue(r.animateSwitch&&r.buttonsOnly&&r.overlapBar&&r.autoContract&&r.contractToCorner);assertEquals(18,r.contractedWidth);assertEquals(12,r.contractDelaySeconds);assertEquals(1,r.cornerEnd);assertTrue(store().placements(s.id).isEmpty());assertEquals(1,store().saved().size());
 }
 @Test public void legacyMigrationPreservesBindingsSettingsAndSelectionAndIsIdempotent(){
  StackState old=new StackState(701);old.name="Existing";old.add(11);old.add(22);old.select(22);old.autoContract=true;old.contractDelaySeconds=7;old.railEdge=StackState.LEFT;old.overlapBar=true;store().put(old);store().migrateLegacy();
  int id=store().definitionId(701);assertTrue(store().isSaved(id));assertEquals(Arrays.asList(11,22),store().get(id).children);assertEquals("Existing",store().get(id).name);assertEquals(22,store().get(701).selected);assertEquals(7,store().get(id).contractDelaySeconds);assertEquals(StackState.LEFT,store().get(id).railEdge);assertTrue(store().get(id).overlapBar);assertEquals(Arrays.asList(701),store().placements(id));assertFalse(c().getSharedPreferences("stacks",0).contains("stack_701"));store().migrateLegacy();assertEquals(id,store().definitionId(701));assertEquals(1,store().saved().size());assertEquals(2,store().all().size());
 }
 @Test public void twoCopiesShareConfigurationsButKeepIndependentSelections(){
  StackState s=saved("Shared");assertTrue(store().assign(701,s.id));assertTrue(store().assign(702,s.id));store().selectOnHome(701,22);s.railWidth=68;s.buttonsOnly=true;store().put(s);
  assertEquals(22,store().get(701).selected);assertEquals(11,store().get(702).selected);assertEquals(11,store().get(s.id).selected);assertEquals(68,store().get(701).railWidth);assertEquals(68,store().get(702).railWidth);assertTrue(store().get(702).buttonsOnly);
 }
 @Test public void removingHomeCopyKeepsSavedStackAndConfiguredChildIds(){StackState s=saved("Keep");store().assign(701,s.id);store().delete(701);assertNull(store().get(701));assertEquals(Arrays.asList(11,22),store().get(s.id).children);assertEquals(1,store().saved().size());}
 @Test public void reassignmentDoesNotMutateEitherSavedDefinitionOrAnotherCopy(){StackState a=saved("A"),b=saved("B");b.children.clear();b.add(33);store().put(b);store().assign(701,a.id);store().assign(702,a.id);store().assign(701,b.id);assertEquals(Arrays.asList(33),store().get(701).children);assertEquals(Arrays.asList(11,22),store().get(702).children);assertEquals(Arrays.asList(702),store().placements(a.id));assertEquals(Arrays.asList(701),store().placements(b.id));}
 @Test public void savedDeletionIsBlockedWhileAHomeCopyUsesIt(){StackState s=saved("In use");store().assign(701,s.id);try{store().delete(s.id);fail("Must retain bindings used by an existing copy");}catch(IllegalStateException expected){}assertNotNull(store().get(s.id));store().delete(701);store().delete(s.id);assertNull(store().get(s.id));}
 @Test public void removingSelectedChildNormalizesEachCopyAndDefaultSeparately(){StackState s=saved("Normalize");store().assign(701,s.id);store().assign(702,s.id);store().selectOnHome(701,22);s.remove(22);store().put(s);assertEquals(11,store().get(701).selected);assertEquals(11,store().get(702).selected);assertEquals(Arrays.asList(11),store().get(s.id).children);}
 @Test public void placementWritesUpdateSharedSettingsWithoutChangingStartingWidget(){StackState s=saved("Start");store().assign(701,s.id);StackState home=store().get(701);home.railOpacity=80;home.select(22);store().put(home);assertEquals(80,store().get(s.id).railOpacity);assertEquals(11,store().get(s.id).selected);assertEquals(22,store().get(701).selected);store().assign(702,s.id);assertEquals(11,store().get(702).selected);}
 @Test public void migrationCoexistsWithAlreadySavedDefinitionsAndSkipsMalformedLegacyData(){StackState a=saved("Already saved");StackState old=new StackState(701);old.name="Legacy";old.add(44);store().put(old);c().getSharedPreferences("stacks",0).edit().putString("stack_999","broken json").commit();store().migrateLegacy();assertEquals(2,store().saved().size());assertEquals(Arrays.asList(11,22),store().get(a.id).children);assertEquals(Arrays.asList(44),store().get(701).children);assertTrue(c().getSharedPreferences("stacks",0).contains("stack_999"));}
 @Test public void pruneOnlyRemovesMissingPlacements(){StackState s=saved("Saved");store().assign(701,s.id);store().assign(702,s.id);store().prunePlacements(new HashSet<>(Arrays.asList(702)));assertNull(store().get(701));assertNotNull(store().get(702));assertNotNull(store().get(s.id));}
 @Test public void invalidAssignmentNeverCreatesData(){assertFalse(store().assign(701,-2));StackState s=saved("Valid");assertFalse(store().assign(-1,s.id));assertEquals(1,store().all().size());}
 @Test public void perWidgetLogicalLayoutSizePersistsAndDefaultsToActualSize(){StackState s=saved("Scale");s.setLogicalSizePercent(11,125);s.setLogicalSizePercent(22,150);store().put(s);StackState copy=store().get(s.id);assertEquals(125,copy.logicalSizePercent(11));assertEquals(150,copy.logicalSizePercent(22));copy.add(33);assertEquals(100,copy.logicalSizePercent(33));copy.remove(22);store().put(copy);assertEquals(125,store().get(s.id).logicalSizePercent(11));assertEquals(100,store().get(s.id).logicalSizePercent(33));}
}

package com.kevin.widgetstack;
import org.junit.Test;import static org.junit.Assert.*;import java.util.Arrays;
public class StackStateTest {
 @Test public void independentStacks(){StackState a=new StackState(1),b=new StackState(2);a.add(100);b.add(200);assertEquals(100,a.selected);assertEquals(200,b.selected);}
 @Test public void reorderingPreservesSelectedIdentity(){StackState s=new StackState(1);s.add(10);s.add(20);s.select(20);s.move(1,0);assertEquals(Arrays.asList(20,10),s.children);assertEquals(20,s.selected);}
 @Test public void deletingSelectedChoosesNeighbour(){StackState s=new StackState(1);s.add(10);s.add(20);s.add(30);s.select(20);s.remove(20);assertEquals(30,s.selected);s.remove(30);assertEquals(10,s.selected);s.remove(10);assertEquals(-1,s.selected);}
 @Test public void invalidSelectionCannotEscapeStack(){StackState s=new StackState(1);s.add(10);s.select(99);assertEquals(10,s.selected);}
 @Test public void invalidMovesDoNotCorruptOrder(){StackState s=new StackState(1);s.add(10);s.add(20);s.move(-1,0);s.move(0,2);assertEquals(Arrays.asList(10,20),s.children);}
 @Test public void addingDuplicateDoesNotAllocateAnotherPage(){StackState s=new StackState(1);s.add(10);s.add(10);assertEquals(1,s.children.size());}
 @Test public void navigationStripDefaultsVisibleAndCanBeHidden(){StackState s=new StackState(1);assertTrue(s.showRail);s.showRail=false;assertFalse(s.showRail);}
 @Test public void previousAndNextWrapAndFollowReorderedWidgets(){StackState s=new StackState(1);s.add(10);s.add(20);s.add(30);s.step(-1);assertEquals(30,s.selected);s.step(1);assertEquals(10,s.selected);s.move(2,1);s.step(1);assertEquals(30,s.selected);}
 @Test public void navigationHandlesEmptyAndSingleWidget(){StackState s=new StackState(1);s.step(1);assertEquals(-1,s.selected);s.add(10);s.step(-1);s.step(1);assertEquals(10,s.selected);}
 @Test public void invalidAppearanceValuesAreClamped(){StackState s=new StackState(1);s.railEdge=99;s.railWidth=1;s.railOpacity=101;s.normalize();assertEquals(StackState.RIGHT,s.railEdge);assertEquals(8,s.railWidth);assertEquals(100,s.railOpacity);}
}

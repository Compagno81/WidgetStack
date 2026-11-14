package com.kevin.widgetstack;

import android.app.Application;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class)
public class RetainedNavigationTest {
 Context c;StackState s;
 @Before public void before(){
  c=RuntimeEnvironment.getApplication();RetainedWidgets.clear();ReflectionHelpers.setStaticField(StackHost.class,"instance",null);
  s=new StackState(900);s.add(11);s.add(22);
  for(int child:s.children){RemoteViews rv=new RemoteViews("android",android.R.layout.list_content);rv.setContentDescription(android.R.id.list,"Child "+child);StackHost.get(c).views.put(child,rv);}
 }
 RemoteViews parcel(RemoteViews rv){Parcel p=Parcel.obtain();rv.writeToParcel(p,0);p.setDataPosition(0);RemoteViews copy=RemoteViews.CREATOR.createFromParcel(p);p.recycle();return copy;}
 View mount()throws Exception{AppWidgetHostView host=new AppWidgetHostView(c);View root=parcel(RetainedWidgets.full(c,s)).apply(c,host);host.addView(root);RetainedWidgets.remember(c,s);return root;}
 RemoteViews next(int motion)throws Exception{RemoteViews rv=parcel(RetainedWidgets.full(c,s,motion));RetainedWidgets.remember(c,s);return rv;}
 ListView list(View root,int index){return ((ViewGroup)root.findViewById(R.id.retained_pages)).getChildAt(index).findViewById(android.R.id.list);}
 void layout(View root){root.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.EXACTLY));root.layout(0,0,400,300);}
 void fill(ListView list){String[] rows=new String[100];for(int i=0;i<rows.length;i++)rows[i]="Row "+i;list.setAdapter(new ArrayAdapter<>(c,android.R.layout.simple_list_item_1,rows));}
 @Test public void switchingKeepsBothActualListsTheirAdaptersAndScrollPositions()throws Exception{
  View root=mount();ListView first=list(root,0),second=list(root,1);fill(first);fill(second);layout(root);
  first.setSelectionFromTop(35,-7);layout(root);int firstRow=first.getFirstVisiblePosition();assertTrue(firstRow>=30);
  s.select(22);next(0).reapply(c,root);layout(root);
  second.setSelectionFromTop(55,-11);layout(root);int secondRow=second.getFirstVisiblePosition();assertTrue(secondRow>=50);
  Object firstAdapter=first.getAdapter(),secondAdapter=second.getAdapter();
  for(int i=0;i<20;i++){s.select(i%2==0?11:22);next(0).reapply(c,root);layout(root);assertSame(first,list(root,0));assertSame(second,list(root,1));assertSame(firstAdapter,first.getAdapter());assertSame(secondAdapter,second.getAdapter());}
  s.select(11);next(0).reapply(c,root);layout(root);assertEquals(firstRow,first.getFirstVisiblePosition());
  s.select(22);next(0).reapply(c,root);layout(root);assertEquals(secondRow,second.getFirstVisiblePosition());
  assertTrue(root.findViewById(R.id.next).isClickable());assertTrue(root.findViewById(R.id.previous).isClickable());
 }
 @Test public void expandingAndContractingBarNeverReplaceProviderViews()throws Exception{
  s.autoContract=true;s.contractToCorner=true;View root=mount();ListView first=list(root,0);fill(first);layout(root);first.setSelectionFromTop(40,0);layout(root);int row=first.getFirstVisiblePosition();
  for(int motion:new int[]{1,0,-1,0}){next(motion).reapply(c,root);layout(root);assertSame(first,list(root,0));assertEquals(row,first.getFirstVisiblePosition());}
 }
 @Test public void collectionNotificationsNeverTargetAnEarlierWidgetsMatchingId()throws Exception{
  for(int child:s.children){RemoteViews rv=StackHost.get(c).views.get(child);rv.setRemoteAdapter(android.R.id.list,new Intent().setComponent(new ComponentName("fixture","Rows")).setData(android.net.Uri.parse("fixture://rows/"+child)));}
  RetainedWidgets.remember(c,s);assertTrue(RetainedWidgets.canNotify(c,900,11,android.R.id.list));assertFalse(RetainedWidgets.canNotify(c,900,22,android.R.id.list));
 }
 @Test public void nativeCollectionsAndProviderInitialScrollCommandSurviveSwitches()throws Exception{
  for(int child:s.children){
   RemoteViews rv=new RemoteViews("android",android.R.layout.list_content);
   RemoteViews.RemoteCollectionItems.Builder rows=new RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(1);
   for(int n=0;n<100;n++){RemoteViews row=new RemoteViews("android",android.R.layout.simple_list_item_1);row.setTextViewText(android.R.id.text1,"Widget "+child+" row "+n);rows.addItem(n,row);}
   rv.setRemoteAdapter(android.R.id.list,rows.build());rv.setScrollPosition(android.R.id.list,0);StackHost.get(c).views.put(child,rv);
  }
  if(Build.VERSION.SDK_INT<35){
   assertThrows(UnsupportedOperationException.class,()->RetainedWidgets.full(c,s));
   View fallback=StackRenderer.renderForHome(c,s,0,0).apply(c,new AppWidgetHostView(c));
   ListView list=fallback.findViewById(android.R.id.list);assertNotNull(list.getAdapter());assertEquals(100,list.getCount());return;
  }
  View root=mount();layout(root);ListView first=list(root,0);assertNotNull("Native collection must be bound",first.getAdapter());assertEquals(100,first.getCount());Shadows.shadowOf(Looper.getMainLooper()).idle();first.setSelectionFromTop(45,0);layout(root);int position=first.getFirstVisiblePosition();assertTrue(position>40);
  s.select(22);next(0).reapply(c,root);layout(root);s.select(11);next(0).reapply(c,root);layout(root);
  assertSame(first,list(root,0));assertEquals(position,first.getFirstVisiblePosition());assertEquals(100,first.getAdapter().getCount());
 }
 @Test public void customSizeAndCornerArrowsRetainProviderAndTouchActions()throws Exception{
  s.cornerArrows=true;s.railEdge=StackState.TOP;s.setDimensions(11,600,500);s.setVisualScalePercent(11,75);
  View root=mount();ListView first=list(root,0);fill(first);layout(root);first.setSelectionFromTop(30,0);layout(root);int position=first.getFirstVisiblePosition();
  s.select(22);next(0).reapply(c,root);layout(root);s.select(11);next(0).reapply(c,root);layout(root);
  assertSame(first,list(root,0));assertEquals(position,first.getFirstVisiblePosition());
  assertEquals("←",((TextView)root.findViewById(R.id.previous)).getText().toString());assertTrue(root.findViewById(R.id.next).isClickable());
 }
 @Test @Config(sdk=35) public void responsiveBitmapCollectionsLoadAndRetainTheirRows()throws Exception{
  for(int child:s.children){
   java.util.Map<android.util.SizeF,RemoteViews> variants=new java.util.LinkedHashMap<>();
   for(int width:new int[]{200,400}){
    RemoteViews rv=new RemoteViews("android",android.R.layout.list_content);
    RemoteViews.RemoteCollectionItems.Builder rows=new RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(1);
    for(int n=0;n<50;n++){
     RemoteViews row=new RemoteViews(c.getPackageName(),R.layout.retention_fixture);
     android.graphics.Bitmap b=android.graphics.Bitmap.createBitmap(20,20,android.graphics.Bitmap.Config.ARGB_8888);b.eraseColor(child==11?android.graphics.Color.RED:android.graphics.Color.BLUE);
     row.setImageViewBitmap(android.R.id.icon,b);row.setTextViewText(android.R.id.text1,"Widget "+child+" row "+n);rows.addItem(n,row);
    }
    rv.setRemoteAdapter(android.R.id.list,rows.build());variants.put(new android.util.SizeF(width,300),rv);
   }
   StackHost.get(c).views.put(child,parcel(new RemoteViews(variants)));
  }
  View root=mount();layout(root);
  for(int index=0;index<2;index++){
   ListView list=list(root,index);assertNotNull(list.getAdapter());assertEquals(50,list.getCount());
   View row=list.getAdapter().getView(0,null,list);assertNotNull(((ImageView)row.findViewById(android.R.id.icon)).getDrawable());
   assertEquals("Widget "+(index==0?11:22)+" row 0",((TextView)row.findViewById(android.R.id.text1)).getText().toString());
  }
  s.select(22);next(0).reapply(c,root);layout(root);
 }
 @Test public void changingMembershipRequiresFullPublication(){RetainedWidgets.remember(c,s);s.add(33);assertFalse(RetainedWidgets.navigate(c,s,0));}
}

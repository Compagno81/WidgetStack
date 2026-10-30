package com.kevin.widgetstack;
import android.test.AndroidTestCase;import android.widget.RemoteViews;import android.graphics.Bitmap;import java.lang.reflect.Field;import java.util.*;
@SuppressWarnings("deprecation")
public class BridgeRegressionTest extends AndroidTestCase {
 public void testNestedTraversalTerminates() throws Exception {
  RemoteViews v=new RemoteViews(getContext().getPackageName(),R.layout.stack);
  RemoteViews child=new RemoteViews(getContext().getPackageName(),R.layout.indicator);child.setTextViewText(R.id.dot,"weather");v.addView(R.id.content,child);
  v.setImageViewBitmap(R.id.dot,Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888));
  assertTrue(RemoteViewsBridge.prepare(getContext(),v,1).isEmpty());
 }
 public void testInlineCollectionTraversalTerminates() throws Exception {
  RemoteViews v=new RemoteViews(getContext().getPackageName(),R.layout.stack);
  v.setRemoteAdapter(R.id.indicators,new RemoteViews.RemoteCollectionItems.Builder().addItem(1,new RemoteViews(getContext().getPackageName(),R.layout.indicator)).build());
  assertFalse(RemoteViewsBridge.prepare(getContext(),v,1).isEmpty());
 }
 public void testOwnerBackReferenceDoesNotRecurse() throws Exception {
  RemoteViews owner=new RemoteViews(getContext().getPackageName(),R.layout.stack);owner.setTextViewText(R.id.empty,"weather");
  // Represents a framework action's synthetic enclosing RemoteViews reference.
  Object backReference=new Object(){final RemoteViews this$0=owner;};
  Field f=RemoteViews.class.getDeclaredField("mActions");f.setAccessible(true);
  @SuppressWarnings("unchecked") List<Object> actions=(List<Object>)f.get(owner);actions.add(backReference);
  assertTrue(RemoteViewsBridge.prepare(getContext(),owner,1).isEmpty());
 }
}

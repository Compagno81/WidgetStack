package com.kevin.widgetstack;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.util.SizeF;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class,qualifiers="mdpi")
public class SavedStackFlowTest {
 private Context c(){return RuntimeEnvironment.getApplication();}
 private StackStore store(){return new StackStore(c());}
 @Before public void reset(){org.robolectric.util.ReflectionHelpers.setStaticField(StackHost.class,"instance",null);}
 @After public void clean(){for(int id:new int[]{701,702})BarController.cancel(id);}
 private AppWidgetProviderInfo provider(String name,String label) throws Exception {
  AppWidgetProviderInfo info=new AppWidgetProviderInfo();info.provider=new ComponentName(c(),name);info.label=label;info.widgetCategory=1;ActivityInfo activity=new ActivityInfo();activity.applicationInfo=c().getApplicationInfo();activity.packageName=c().getPackageName();activity.name=name;activity.nonLocalizedLabel=label;
  for(java.lang.reflect.Field f:AppWidgetProviderInfo.class.getDeclaredFields())if(f.getType()==ActivityInfo.class){f.setAccessible(true);f.set(info,activity);}
  shadowOf(AppWidgetManager.getInstance(c())).addInstalledProvider(info);shadowOf(AppWidgetManager.getInstance(c())).setAllowedToBindAppWidgets(true);return info;
 }
 private void bind(int id,AppWidgetProviderInfo p){assertTrue(AppWidgetManager.getInstance(c()).bindAppWidgetIdIfAllowed(id,p.provider));}
 private void home(int id,int w,int h) throws Exception {bind(id,provider(StackProvider.class.getName(),"Stack"));Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,w);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,w);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,h);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,h);AppWidgetManager.getInstance(c()).updateAppWidgetOptions(id,options);}
 private StackState saved() throws Exception {
  AppWidgetProviderInfo child=provider("com.kevin.widgetstack.TestProvider","Test widget");bind(11,child);bind(22,child);StackState s=store().createSaved("Daily stack");s.add(11);s.add(22);store().put(s);RemoteViews rv=new RemoteViews(c().getPackageName(),R.layout.stack_placeholder);rv.setTextViewText(R.id.empty,"Saved live content");StackHost.get(c()).views.put(11,rv);StackHost.get(c()).views.put(22,rv);return s;
 }
 private TextView text(View v,String value){if(v instanceof TextView&&value.contentEquals(((TextView)v).getText()))return(TextView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView r=text(((ViewGroup)v).getChildAt(i),value);if(r!=null)return r;}return null;}
 @Test public void savedEditorWorksBeforeAnyHomeWidgetAndBackKeepsTheDefinition() throws Exception {
  StackState s=saved();var controller=Robolectric.buildActivity(ConfigureActivity.class,SavedStacks.edit(c(),s.id)).setup();ConfigureActivity a=controller.get();assertFalse(a.isFinishing());assertEquals(s.id,a.stackId);assertNotNull(text(a.getWindow().getDecorView(),"Add widget"));assertNotNull(text(a.getWindow().getDecorView(),"Saved live content"));a.finish();controller.pause().stop().destroy();assertEquals(Arrays.asList(11,22),store().get(s.id).children);assertNotNull(AppWidgetManager.getInstance(c()).getAppWidgetInfo(11));
 }
 @Test public void addingProviderFromPickerWorksInAnUnplacedSavedStack() throws Exception {
  StackState s=store().createSaved("Prepared first");AppWidgetProviderInfo child=provider("com.kevin.widgetstack.NoSetupProvider","Prepared widget");ConfigureActivity a=Robolectric.buildActivity(ConfigureActivity.class,SavedStacks.edit(c(),s.id)).setup().get();shadowOf((AppWidgetHost)StackHost.get(c())).setAppWidgetIdToAllocate(33);a.onActivityResult(54,Activity.RESULT_OK,new Intent().putExtra("provider",child));if(a.pending>=0){bind(a.pending,child);a.onActivityResult(51,Activity.RESULT_OK,new Intent());}StackState result=store().get(s.id);assertEquals(1,result.children.size());assertNotNull(AppWidgetManager.getInstance(c()).getAppWidgetInfo(result.children.get(0)));assertEquals(-1,a.pending);assertTrue(store().placements(s.id).isEmpty());
 }
 @Test public void launcherChooserAssignsExistingBindingsAndReturnsTheHomeId() throws Exception {
  home(701,320,160);StackState s=saved();StackChooserActivity a=Robolectric.buildActivity(StackChooserActivity.class,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,701)).setup().get();assertNotNull(text(a.getWindow().getDecorView(),"Choose a saved stack"));a.choose(s.id);assertEquals(Activity.RESULT_OK,shadowOf(a).getResultCode());assertEquals(701,shadowOf(a).getResultIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1));assertEquals(s.id,store().definitionId(701));assertEquals(Arrays.asList(11,22),store().get(701).children);assertEquals(1,store().saved().size());
 }
 @Test public void cancelChooserLeavesSavedStacksAndHomeUnassigned() throws Exception {
  home(701,320,160);StackState s=saved();var controller=Robolectric.buildActivity(StackChooserActivity.class,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,701)).setup();StackChooserActivity a=controller.get();a.finish();controller.pause().stop().destroy();assertEquals(Activity.RESULT_CANCELED,shadowOf(a).getResultCode());assertNull(store().get(701));assertEquals(Arrays.asList(11,22),store().get(s.id).children);
 }
 @Test public void createFromChooserReturnsToTheSamePlacementAfterEditorDone() throws Exception {
  home(701,320,160);StackChooserActivity chooser=Robolectric.buildActivity(StackChooserActivity.class,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,701)).setup().get();text(chooser.getWindow().getDecorView(),"Create saved stack").performClick();Intent edit=shadowOf(chooser).getNextStartedActivityForResult().intent;int saved=edit.getIntExtra(SavedStacks.EXTRA_SAVED_ID,-1);assertTrue(store().isSaved(saved));ConfigureActivity editor=Robolectric.buildActivity(ConfigureActivity.class,edit).setup().get();text(editor.getWindow().getDecorView(),"Done").performClick();chooser.onActivityResult(61,shadowOf(editor).getResultCode(),shadowOf(editor).getResultIntent());assertEquals(saved,store().definitionId(701));assertEquals(Activity.RESULT_OK,shadowOf(chooser).getResultCode());
 }
 @Test public void pinCallbackReceivesFilledHomeIdAndUsesTheRequestedSavedStack() throws Exception {
  home(701,320,160);StackState s=saved();PendingIntent callback=SavedStacks.pinCallback(c(),s.id);callback.send(c(),0,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,701));shadowOf(Looper.getMainLooper()).idle();assertEquals(s.id,store().definitionId(701));assertEquals(Arrays.asList(11,22),store().get(701).children);
 }
 @Test public void twoCopiesNavigateIndependentlyThroughTheActualReceiver() throws Exception {
  home(701,320,160);home(702,240,200);StackState s=saved();assertTrue(SavedStacks.place(c(),701,s.id));assertTrue(SavedStacks.place(c(),702,s.id));new StackProvider().onReceive(c(),new Intent().setAction(StackProvider.SELECT).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,701).putExtra("direction",1));assertEquals(22,store().get(701).selected);assertEquals(11,store().get(702).selected);assertEquals(11,store().get(s.id).selected);s.railWidth=60;s.railOpacity=75;store().put(s);SavedStacks.changed(c(),s.id);assertEquals(60,store().get(701).railWidth);assertEquals(60,store().get(702).railWidth);assertEquals(22,store().get(701).selected);
 }
 @Test public void deletingEveryHomeCopyRetainsTheConfiguredRealWidgets() throws Exception {
  home(701,320,160);home(702,240,200);StackState s=saved();SavedStacks.place(c(),701,s.id);SavedStacks.place(c(),702,s.id);new StackProvider().onDeleted(c(),new int[]{701,702});assertNull(store().get(701));assertNull(store().get(702));assertEquals(Arrays.asList(11,22),store().get(s.id).children);assertNotNull(AppWidgetManager.getInstance(c()).getAppWidgetInfo(11));assertNotNull(AppWidgetManager.getInstance(c()).getAppWidgetInfo(22));
 }
 @Test public void sharedBindingReceivesResponsiveSizesFromEveryPlacement() throws Exception {
  home(701,320,160);home(702,240,200);StackState s=saved();SavedStacks.place(c(),701,s.id);SavedStacks.place(c(),702,s.id);Bundle options=AppWidgetManager.getInstance(c()).getAppWidgetOptions(11);assertEquals(204,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));assertEquals(284,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH));ArrayList<SizeF> sizes=options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);assertTrue(sizes.contains(new SizeF(284,160)));assertTrue(sizes.contains(new SizeF(204,200)));s.showRail=false;store().put(s);SavedStacks.changed(c(),s.id);options=AppWidgetManager.getInstance(c()).getAppWidgetOptions(11);assertEquals(240,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));assertEquals(320,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH));
 }
 @Test public void invalidHomeOrRemovedDefinitionCannotBeAssigned() throws Exception {StackState s=saved();assertFalse(SavedStacks.place(c(),999,s.id));home(701,320,160);store().delete(s.id);assertFalse(SavedStacks.place(c(),701,s.id));assertNull(store().get(701));}
 @Test public void unassignedHomeDoesNotCreateAnAccidentalSavedStack() throws Exception {home(701,320,160);new StackProvider().onUpdate(c(),AppWidgetManager.getInstance(c()),new int[]{701});assertTrue(store().saved().isEmpty());assertNull(store().get(701));assertEquals(StackChooserActivity.class.getName(),StackRenderer.editIntent(c(),701).getComponent().getClassName());}
 @Test public void unplacedPreviewShowsLiveContentAndObservesUpdatesWithoutActivatingIt() throws Exception {
  StackState s=saved();StackPreview p=new StackPreview(c(),s);var controller=Robolectric.buildActivity(Activity.class).create().start().resume();Activity a=controller.get();a.setContentView(p);controller.visible();shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(32));assertNotNull(text(p,"Saved live content"));assertFalse(StackHost.get(c()).previewListeners.isEmpty());
  RemoteViews updated=new RemoteViews(c().getPackageName(),R.layout.stack_placeholder);updated.setTextViewText(R.id.empty,"Refreshed saved content");org.robolectric.util.ReflectionHelpers.callInstanceMethod(StackHost.get(c()),"capture",org.robolectric.util.ReflectionHelpers.ClassParameter.from(int.class,11),org.robolectric.util.ReflectionHelpers.ClassParameter.from(RemoteViews.class,updated));shadowOf(Looper.getMainLooper()).idle();assertNotNull(text(p,"Refreshed saved content"));a.setContentView(new FrameLayout(a));assertTrue(StackHost.get(c()).previewListeners.isEmpty());assertTrue(store().placements(s.id).isEmpty());
 }
 @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
 @Test public void savedManagerAndChooserHaveNativeUsableLayouts() throws Exception {
  home(701,320,160);StackState s=saved();store().assign(701,s.id);StackState another=store().createSaved("Calendar and tasks");another.railEdge=StackState.BOTTOM;store().put(another);
  MainActivity main=Robolectric.buildActivity(MainActivity.class).setup().get();assertNotNull(text(main.getWindow().getDecorView(),"Create saved stack"));assertNotNull(text(main.getWindow().getDecorView(),"Daily stack"));capture(main.getWindow().getDecorView(),"saved-manager");
  StackChooserActivity chooser=Robolectric.buildActivity(StackChooserActivity.class,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,701).putExtra(StackChooserActivity.EXTRA_CHOOSE,true)).setup().get();assertNotNull(text(chooser.getWindow().getDecorView(),"Choose a saved stack"));capture(chooser.getWindow().getDecorView(),"saved-chooser");
 }
 private void capture(View decor,String name) throws Exception {decor.measure(View.MeasureSpec.makeMeasureSpec(400,1073741824),View.MeasureSpec.makeMeasureSpec(900,1073741824));decor.layout(0,0,400,900);android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(400,900,android.graphics.Bitmap.Config.ARGB_8888);decor.draw(new android.graphics.Canvas(bitmap));java.io.File file=new java.io.File("build/reports/"+name+"-api"+Build.VERSION.SDK_INT+".png");file.getParentFile().mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}}

}

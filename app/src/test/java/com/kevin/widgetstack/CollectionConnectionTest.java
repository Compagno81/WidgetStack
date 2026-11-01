package com.kevin.widgetstack;
import android.app.Application;
import android.content.*;
import android.os.*;
import android.widget.RemoteViewsService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class)
public class CollectionConnectionTest {
 public static class Remote {public int getCount(){return 40;}}
 private void set(Object target,String field,Object value)throws Exception{Field f=target.getClass().getDeclaredField(field);f.setAccessible(true);f.set(target,value);}
 @Test public void collectionConnectionCompletesWhileMainLooperIsPaused()throws Exception{
  CollectionProxyService service=Robolectric.buildService(CollectionProxyService.class).create().get();
  ExecutorService worker=Executors.newSingleThreadExecutor();
  try{
   RemoteViewsService.RemoteViewsFactory factory=service.onGetViewFactory(new Intent().putExtra("child",12));
   Field field=factory.getClass().getDeclaredField("handler");field.setAccessible(true);Handler callback=(Handler)field.get(factory);
   assertNotSame(Looper.getMainLooper(),callback.getLooper());
   assertEquals("Disconnected metadata must return immediately",0,(int)worker.submit(factory::getCount).get(1,TimeUnit.SECONDS));
   CountDownLatch connected=new CountDownLatch(1);
   callback.post(()->{try{set(factory,"factoryInterface",Remote.class);set(factory,"remote",new Remote());connected.countDown();}catch(Exception e){throw new RuntimeException(e);}});
   // Do not drain the main looper: this models publication waiting for metadata.
   assertTrue(connected.await(2,TimeUnit.SECONDS));assertEquals(40,(int)worker.submit(factory::getCount).get(1,TimeUnit.SECONDS));
  }finally{worker.shutdownNow();service.onDestroy();}
 }
 @Test public void providerReceivesChildIdAndOriginalIntentIsPreserved(){
  Context c=RuntimeEnvironment.getApplication();Intent original=new Intent("agenda").setComponent(new ComponentName("com.appgenix.bizcal","AgendaService")).setData(android.net.Uri.parse("agenda://calendar/123")).putExtra("calendar",7);
  Intent proxy=CollectionProxyService.intent(c,123,original,42);
  proxy.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,999); // outer launcher ID
  Intent forwarded=proxy.getParcelableExtra("original");
  assertEquals(123,forwarded.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,-1));
  assertFalse(original.hasExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID));
  assertEquals(original.getComponent(),forwarded.getComponent());assertEquals(original.getData(),forwarded.getData());assertEquals(7,forwarded.getIntExtra("calendar",0));
  original.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,999);
  assertEquals(123,CollectionProxyService.providerIntent(123,original).getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,-1));
 }
 @Test public void collectionServiceRunsOutsideHostProcess()throws Exception{
  Context c=RuntimeEnvironment.getApplication();android.content.pm.ServiceInfo info=c.getPackageManager().getServiceInfo(new ComponentName(c,CollectionProxyService.class),0);
  assertEquals(c.getPackageName()+":collections",info.processName);
 }
}

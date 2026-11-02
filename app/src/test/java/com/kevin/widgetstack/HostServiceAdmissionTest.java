package com.kevin.widgetstack;
import android.app.*;
import android.content.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},application=Application.class)
public class HostServiceAdmissionTest {
 public static class DeniedService extends HostService { @Override void promote(Notification n){throw new ForegroundServiceStartNotAllowedException("background restart denied");} }
 public static class PermissionDeniedService extends HostService { @Override void promote(Notification n){throw new SecurityException("foreground permission denied");} }
 @Test public void deniedPromotionDoesNotCrashOrRequestStickyRestart(){
  org.robolectric.android.controller.ServiceController<DeniedService> controller=Robolectric.buildService(DeniedService.class).create();DeniedService service=controller.get();
  assertFalse(service.foregroundReady);assertFalse(service.hostStarted);assertEquals(Service.START_NOT_STICKY,service.onStartCommand(null,0,1));assertFalse(service.hostStarted);assertTrue(StackDiagnostics.report(service).contains("host start denied"));controller.destroy();
 }
 @Test public void permissionDenialAlsoStopsCleanly(){org.robolectric.android.controller.ServiceController<PermissionDeniedService> controller=Robolectric.buildService(PermissionDeniedService.class).create();assertEquals(Service.START_NOT_STICKY,controller.get().onStartCommand(null,0,1));controller.destroy();}
 @Test public void permittedStartStillCreatesForegroundNotification(){org.robolectric.android.controller.ServiceController<HostService> controller=Robolectric.buildService(HostService.class).create();assertTrue(controller.get().foregroundReady);controller.destroy();}
}

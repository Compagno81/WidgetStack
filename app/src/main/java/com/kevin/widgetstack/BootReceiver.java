package com.kevin.widgetstack;
import android.content.*;
public final class BootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){if(!Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())&&!Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction()))return;new StackStore(c).migrateLegacy();HostService.ensure(c);for(StackState s:new StackStore(c).all())StackRenderer.update(c,s.id);}
}

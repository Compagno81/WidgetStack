package com.kevin.widgetstack;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public final class HostService extends Service {
    static void ensure(Context c) {
        if(new StackStore(c).all().isEmpty()) return;
        try { c.startForegroundService(new Intent(c,HostService.class)); }
        catch(RuntimeException e) { Log.w("WidgetStack","Open Widget Stack to resume live updates",e); }
    }
    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm=getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("host","Widget updates",NotificationManager.IMPORTANCE_LOW));
        Notification n=new Notification.Builder(this,"host").setSmallIcon(R.drawable.ic_stack)
            .setContentTitle("Widget stacks are active").setContentText("Keeping your home-screen widgets up to date")
            .setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true).setShowWhen(false).build();
        if(Build.VERSION.SDK_INT>=34)startForeground(41,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(41,n);
    }
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        new StackStore(this).migrateLegacy();
        if(new StackStore(this).all().isEmpty()){stopSelf();return START_NOT_STICKY;}
        StackHost.get(this).sync();
        for(StackState s:new StackStore(this).all()){StackRenderer.updateSizes(this,s.id);StackHost.get(this).schedule(s.id);}
        return START_STICKY;
    }
    @Override public void onDestroy(){StackHost.get(this).stop();super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}

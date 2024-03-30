package com.vectras.qemu;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;

import androidx.core.app.NotificationCompat;

import com.vectras.qemu.jni.StartVM;
import com.vectras.qemu.utils.FileUtils;
import com.vectras.vm.Fragment.HomeFragment;
import com.vectras.vm.MainActivity;
import com.vectras.vm.R;

public class MainService extends Service {

    private static final String TAG = "MainService";
    private static Notification mNotification;
    private static WifiLock mWifiLock;
    public static MainService service;
    private static WakeLock mWakeLock;
    public static boolean isRunning;
    private NotificationManager mNotificationManager;

    @Override
    public IBinder onBind(Intent arg0) {

        return null;
    }

    public static StartVM executor;
    private static NotificationCompat.Builder builder;

    public static final int notifID = 1000;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        final Bundle b = intent.getExtras();
        final int ui = b.getInt("ui", 0);

        if (action.equals(Config.ACTION_START)) {
            setUpAsForeground(Config.machinename + " VM Running");

            FileUtils.startLogging();

            scheduleTimer();

            Thread t = new Thread(new Runnable() {
                public void run() {

                    //XXX: wait till logging starts capturing
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Log.v(TAG, "Starting VM " + Config.machinename);

                    setupLocks();

                    if (ui == Config.UI_VNC) {

                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                MainActivity.startvnc();
                            }
                        }, 2000);
                    }

                    //Start vm
                    String res = executor.startvm();

                    //VM has exited
                    MainActivity.cleanup();

                }
            });
            t.setName("StartVM");
            t.start();


        }


        // Don't restart if killed
        return START_NOT_STICKY;
    }

    private void scheduleTimer() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                MainActivity.startTimer();
            }
        });
        t.start();
    }


    private void setUpAsForeground(String text) {
        isRunning = true;
        MainActivity.vmStarted = true;
        Class<?> clientClass = null;
        if (Config.ui != null) {
            if (Config.ui.equals("VNC")) {
                if (MainSettingsManager.getVncExternal(MainActivity.activity)) {
                    MainActivity.extVncLayout.setVisibility(View.VISIBLE);
                    MainActivity.appbar.setExpanded(true);
                }
                clientClass = MainVNCActivity.class;
            } else if (Config.ui.equals("SDL")) {
                clientClass = MainSDLActivity.class;
            } else {
                Log.e(TAG, "Unknown User Interface");
                return;
            }
        } else {
            // UIUtils.toastLong(service, "Machine UI is not set");
            //using VNC by default
            clientClass = MainVNCActivity.class;
        }
        Intent intent = new Intent(service.getApplicationContext(), clientClass);

        PendingIntent pi = PendingIntent.getActivity(service.getApplicationContext(), 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(Config.notificationChannelID, Config.notificationChannelName, NotificationManager.IMPORTANCE_NONE);
            NotificationManager notifService = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notifService.createNotificationChannel(chan);
            builder = new NotificationCompat.Builder(service, Config.notificationChannelID);

        } else
            builder = new NotificationCompat.Builder(service, "");
        mNotification = builder.setContentIntent(pi).setContentTitle(getString(R.string.app_name)).setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(service.getResources(), R.mipmap.ic_launcher)).build();
        mNotification.tickerText = text;
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;

        if (Build.VERSION.SDK_INT >= 34) {
            service.startForeground(notifID, mNotification, FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            service.startForeground(notifID, mNotification);
        }


    }

    public static void updateServiceNotification(String text) {
        if (builder != null) {
            builder.setContentText(text);
            mNotification = builder.build();
            // mNotification.tickerText = text ;

            NotificationManager mNotificationManager = (NotificationManager)
                    service.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            // mId allows you to update the notification later on.
            mNotificationManager.notify(notifID, mNotification);
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Creating Service");
        service = this;
    }

    private void setupLocks() {

        mWifiLock = ((WifiManager) service.getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, Config.wifiLockTag);
        mWifiLock.setReferenceCounted(false);

        PowerManager pm = (PowerManager) service.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Config.wakeLockTag);
        mWakeLock.setReferenceCounted(false);

        mNotificationManager = (NotificationManager) service.getApplicationContext().getSystemService(NOTIFICATION_SERVICE);

    }

    private static void releaseLocks() {

        if (mWifiLock != null && mWifiLock.isHeld()) {
            Log.d(TAG, "Release Wifi lock...");
            mWifiLock.release();
        }

        if (mWakeLock != null && mWakeLock.isHeld()) {
            Log.d(TAG, "Release Wake lock...");
            mWakeLock.release();
        }

    }

    public static void stopService() {

        Thread t = new Thread(new Runnable() {
            public void run() {
                releaseLocks();
                if (service != null) {
                    service.stopForeground(true);
                    service.stopSelf();
                    isRunning = false;
                }

            }
        });
        t.setName("StartVM");
        t.start();


    }

}

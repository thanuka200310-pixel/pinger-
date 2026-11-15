package com.hutch.pingerlite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KeepAliveService extends Service {
    private ScheduledExecutorService scheduler;
    public static boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        startForegroundServiceNotification();
        startTurboPinger();
    }

    private void startForegroundServiceNotification() {
        String channelId = "PingerChannel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Network Keep Alive", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Intent stopIntent = new Intent(this, KeepAliveService.class);
        stopIntent.setAction("STOP_PINGER");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 0);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle("Z Pinger TURBO Active")
                .setContentText("High-Speed Data Channel Locked (1.5s)")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .addAction(android.R.drawable.ic_media_pause, "STOP PINGER", stopPendingIntent)
                .build();

        startForeground(1, notification);
    }

    private void startTurboPinger() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        // හැම තත්පර 1.5කට (1500ms) සැරයක්ම High-Speed HTTP පැකට් එකක් යවනවා
        scheduler.scheduleWithFixedDelay(() -> {
            HttpURLConnection conn = null;
            try {
                // මේ Google 204 සර්වර් එකෙන් තමයි ටවර් එකේ High-Speed මෝඩ් එක ඇහැරවන්නේ
                URL url = new URL("http://connectivitycheck.gstatic.com/generate_204");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setConnectTimeout(1500);
                conn.setReadTimeout(1500);
                conn.getResponseCode();
            } catch (Exception e) {
            } finally {
                if (conn != null) conn.disconnect();
            }
        }, 0, 1500, TimeUnit.MILLISECONDS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP_PINGER".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        isRunning = true;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (scheduler != null) scheduler.shutdownNow();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

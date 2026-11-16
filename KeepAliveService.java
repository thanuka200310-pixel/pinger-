package com.hutch.pingerlite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;
import java.io.InputStream;
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
        startSmartTurboPinger();
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
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle("Z Pinger Smart-Turbo ⚡")
                .setContentText("Fast Speed + Battery/Charge Friendly (2.5s)")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .addAction(android.R.drawable.ic_media_pause, "STOP PINGER", stopPendingIntent)
                .build();

        startForeground(1, notification);
    }

    private void startSmartTurboPinger() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        // තත්පර 2.5කට (2500ms) සැරයක් එකම Open Connection එක පාවිච්චි කරනවා (බැටරි කන්නේ නෑ)
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                URL url = new URL("http://connectivitycheck.gstatic.com/generate_204");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.setRequestProperty("Connection", "keep-alive");
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                conn.getResponseCode();
                
                // disconnect() කරන්නේ නැතුව stream එක විතරක් වහනවා.
                // එතකොට ෆෝන් එකට අලුතින් කනෙක්ෂන් හදන්න මහන්සි වෙන්න ඕනේ නෑ!
                InputStream is = conn.getInputStream();
                if (is != null) is.close();
            } catch (Exception e) {
            }
        }, 0, 2500, TimeUnit.MILLISECONDS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP_PINGER".equals(intent.getAction())) {
            isRunning = false;
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

    public static class PingerTile extends TileService {
        @Override
        public void onStartListening() {
            super.onStartListening();
            updateTile();
        }

        @Override
        public void onClick() {
            super.onClick();
            Intent intent = new Intent(getApplicationContext(), KeepAliveService.class);
            if (KeepAliveService.isRunning) {
                getApplicationContext().stopService(intent);
                KeepAliveService.isRunning = false;
                Toast.makeText(getApplicationContext(), "Z Pinger OFF 🔴", Toast.LENGTH_SHORT).show();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getApplicationContext().startForegroundService(intent);
                } else {
                    getApplicationContext().startService(intent);
                }
                KeepAliveService.isRunning = true;
                Toast.makeText(getApplicationContext(), "Smart-Turbo ON ⚡", Toast.LENGTH_SHORT).show();
            }
            updateTile();
        }

        private void updateTile() {
            Tile tile = getQsTile();
            if (tile != null) {
                tile.setState(KeepAliveService.isRunning ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
                tile.setLabel(KeepAliveService.isRunning ? "Turbo ON" : "Z Pinger");
                tile.updateTile();
            }
        }
    }
}

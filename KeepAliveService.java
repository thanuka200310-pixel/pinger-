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
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
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
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle("Z Pinger TURBO Active 🚀")
                .setContentText("High-Speed 4G+/H+ Locked (Every 1s)")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .addAction(android.R.drawable.ic_media_pause, "STOP PINGER", stopPendingIntent)
                .build();

        startForeground(1, notification);
    }

    private void startTurboPinger() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        // හැම තත්පර 1කටම (1000ms) සැරයක් HTTP + DNS පැකට් යවලා ටවර් එක High-Speed තියාගන්නවා
        scheduler.scheduleWithFixedDelay(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://connectivitycheck.gstatic.com/generate_204");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setUseCaches(false);
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                conn.getResponseCode();
            } catch (Exception e) {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress("1.1.1.1", 53), 1000);
                    socket.close();
                } catch (Exception ex) {
                }
            } finally {
                if (conn != null) conn.disconnect();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
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

    // උඩ Notification Bar එකේ Icon එක වැඩ කරවන කොටස
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
                Toast.makeText(getApplicationContext(), "Z Pinger TURBO ON 🚀", Toast.LENGTH_SHORT).show();
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

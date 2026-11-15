package com.hutch.pingerlite;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#121212"));
        layout.setPadding(50, 50, 50, 50);

        statusText = new TextView(this);
        statusText.setTextSize(22);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 0, 0, 60);
        updateStatusUI();

        Button startBtn = new Button(this);
        startBtn.setText("START TURBO PINGER");
        startBtn.setTextSize(18);
        startBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, KeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            KeepAliveService.isRunning = true;
            updateStatusUI();
            Toast.makeText(MainActivity.this, "Turbo Pinger ON!", Toast.LENGTH_SHORT).show();
        });

        Button stopBtn = new Button(this);
        stopBtn.setText("STOP PINGER");
        stopBtn.setTextSize(18);
        stopBtn.setOnClickListener(v -> {
            stopService(new Intent(MainActivity.this, KeepAliveService.class));
            KeepAliveService.isRunning = false;
            updateStatusUI();
            Toast.makeText(MainActivity.this, "Pinger OFF!", Toast.LENGTH_SHORT).show();
        });

        layout.addView(statusText);
        layout.addView(startBtn);
        layout.addView(stopBtn);
        setContentView(layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusUI();
    }

    private void updateStatusUI() {
        if (KeepAliveService.isRunning) {
            statusText.setText("STATUS: TURBO ACTIVE 🚀\n(Pinging Every 1 Second)");
            statusText.setTextColor(Color.GREEN);
        } else {
            statusText.setText("STATUS: STOPPED 🔴");
            statusText.setTextColor(Color.RED);
        }
    }
}

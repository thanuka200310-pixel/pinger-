package com.hutch.pingerlite;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        Button startBtn = new Button(this);
        startBtn.setText("START Z PINGER LITE");
        startBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, KeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            Toast.makeText(MainActivity.this, "Pinger Started!", Toast.LENGTH_SHORT).show();
        });

        Button stopBtn = new Button(this);
        stopBtn.setText("STOP PINGER");
        stopBtn.setOnClickListener(v -> {
            stopService(new Intent(MainActivity.this, KeepAliveService.class));
            Toast.makeText(MainActivity.this, "Pinger Stopped!", Toast.LENGTH_SHORT).show();
        });

        layout.addView(startBtn);
        layout.addView(stopBtn);
        setContentView(layout);
    }
}

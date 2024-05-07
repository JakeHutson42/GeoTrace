package com.example.geotrace;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    // Define progress bar variables
    private static final int MAX_PROGRESS = 100;
    private static final int PROGRESS_DELAY = 30; // Delay in milliseconds between each progress update
    private ProgressBar progressBar;
    private int progress = 0;

    // Start activity and progress bar, start loading.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setMax(MAX_PROGRESS);

        startLoading();
    }


    // Initialise progress bar loading and increment loading points.
    // Check if the loading bar is at max progress and if not increment it,
    // else start the login activity.

    private void startLoading() {
        final Handler handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (progress <= MAX_PROGRESS) {
                    progressBar.setProgress(progress);
                    progress++;
                    sendEmptyMessageDelayed(0, PROGRESS_DELAY);
                } else {
                    // Progress completed, start the next activity
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };

        handler.sendEmptyMessageDelayed(0, PROGRESS_DELAY);
    }
}

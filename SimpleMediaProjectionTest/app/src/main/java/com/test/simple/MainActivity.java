package com.test.simple;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SimpleMediaProjection";
    private static final int REQUEST_MEDIA_PROJECTION = 1;

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    
    private Button buttonStart;
    private TextView textStatus;
    private SurfaceView surfaceView;
    
    private int screenDensity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        buttonStart = findViewById(R.id.buttonStart);
        textStatus = findViewById(R.id.textStatus);
        surfaceView = findViewById(R.id.surfaceView);

        // Get screen density
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenDensity = metrics.densityDpi;

        // Initialize MediaProjection manager
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // Set up button click
        buttonStart.setOnClickListener(v -> {
            if (virtualDisplay == null) {
                startScreenCapture();
            } else {
                stopScreenCapture();
            }
        });

        textStatus.setText("Ready to test MediaProjection");
        Log.d(TAG, "MainActivity created, screen density: " + screenDensity);
    }

    private void startScreenCapture() {
        textStatus.setText("Requesting MediaProjection permission...");
        Log.d(TAG, "Requesting MediaProjection permission");
        
        // Request MediaProjection permission
        Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                textStatus.setText("MediaProjection permission denied");
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "MediaProjection permission denied");
                return;
            }

            Log.d(TAG, "MediaProjection permission granted, starting capture");
            textStatus.setText("Permission granted, starting capture...");

            // Create MediaProjection
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            
            // Start screen mirroring to SurfaceView
            setUpVirtualDisplay();
        }
    }

    private void setUpVirtualDisplay() {
        Surface surface = surfaceView.getHolder().getSurface();
        
        int width = surfaceView.getWidth();
        int height = surfaceView.getHeight();
        
        Log.d(TAG, "Setting up VirtualDisplay: " + width + "x" + height + " (" + screenDensity + ")");
        
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            width, height, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface, null, null);

        textStatus.setText("Screen capture active! Check SurfaceView.");
        buttonStart.setText("Stop Capture");
        
        Toast.makeText(this, "Screen capture started!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "VirtualDisplay created successfully");
    }

    private void stopScreenCapture() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
            Log.d(TAG, "VirtualDisplay released");
        }
        
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
            Log.d(TAG, "MediaProjection stopped");
        }

        textStatus.setText("Screen capture stopped");
        buttonStart.setText("Start Capture");
        
        Toast.makeText(this, "Screen capture stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScreenCapture();
        Log.d(TAG, "MainActivity destroyed");
    }
}
package com.gestureai.gameautomation;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class OverlayService extends Service {
    private static final String TAG = "OverlayService";
    private static OverlayService instance;
    
    private WindowManager windowManager;
    private View overlayView;
    private boolean isOverlayVisible = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Log.d(TAG, "Overlay Service created");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Overlay Service started");
        showAutomationOverlay();
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    public static OverlayService getInstance() {
        return instance;
    }
    
    public void showAutomationOverlay() {
        if (isOverlayVisible) {
            return;
        }
        
        try {
            overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_controls, null);
            
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 0;
            params.y = 100;
            
            // Set up overlay controls
            setupOverlayControls();
            
            windowManager.addView(overlayView, params);
            isOverlayVisible = true;
            
            Log.d(TAG, "Automation overlay displayed");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to show overlay", e);
        }
    }
    
    private void setupOverlayControls() {
        if (overlayView == null) return;
        
        TextView statusText = overlayView.findViewById(R.id.tv_overlay_status);
        Button toggleButton = overlayView.findViewById(R.id.btn_toggle_automation);
        Button closeButton = overlayView.findViewById(R.id.btn_close_overlay);
        
        if (statusText != null) {
            statusText.setText("Automation Active");
        }
        
        if (toggleButton != null) {
            toggleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleAutomation();
                }
            });
        }
        
        if (closeButton != null) {
            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideOverlay();
                }
            });
        }
    }
    
    private void toggleAutomation() {
        GameAutomationEngine engine = GameAutomationEngine.getInstance();
        if (engine != null) {
            boolean isEnabled = engine.isAutomationEnabled();
            engine.setAutomationEnabled(!isEnabled);
            
            TextView statusText = overlayView.findViewById(R.id.tv_overlay_status);
            if (statusText != null) {
                statusText.setText(isEnabled ? "Automation Paused" : "Automation Active");
            }
            
            Log.d(TAG, "Automation toggled: " + !isEnabled);
        }
    }
    
    public void hideOverlay() {
        if (isOverlayVisible && overlayView != null) {
            try {
                windowManager.removeView(overlayView);
                isOverlayVisible = false;
                overlayView = null;
                Log.d(TAG, "Overlay hidden");
            } catch (Exception e) {
                Log.e(TAG, "Failed to hide overlay", e);
            }
        }
    }
    
    public void updateOverlayStatus(String status) {
        if (isOverlayVisible && overlayView != null) {
            TextView statusText = overlayView.findViewById(R.id.tv_overlay_status);
            if (statusText != null) {
                statusText.setText(status);
            }
        }
    }
    
    public boolean isOverlayVisible() {
        return isOverlayVisible;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        hideOverlay();
        instance = null;
        Log.d(TAG, "Overlay Service destroyed");
    }
}
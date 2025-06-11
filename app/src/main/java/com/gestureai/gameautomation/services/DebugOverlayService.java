package com.gestureai.gameautomation.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.managers.AIStackManager;

/**
 * Debug Overlay Service - Shows real-time AI performance metrics on screen
 */
public class DebugOverlayService extends Service {
    private static final String TAG = "DebugOverlayService";
    
    private WindowManager windowManager;
    private View overlayView;
    private TextView tvAIStatus;
    private TextView tvPerformanceMetrics;
    private TextView tvMemoryUsage;
    
    private GameStrategyAgent strategyAgent;
    private AIStackManager aiStackManager;
    
    private volatile boolean isOverlayVisible = false;
    private android.os.Handler updateHandler;
    private Runnable updateRunnable;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Initialize AI components for monitoring
        strategyAgent = new GameStrategyAgent(this);
        aiStackManager = AIStackManager.getInstance(this);
        
        createOverlayView();
        startMetricsUpdates();
        
        Log.d(TAG, "Debug Overlay Service created");
    }
    
    private void createOverlayView() {
        try {
            LayoutInflater inflater = LayoutInflater.from(this);
            overlayView = inflater.inflate(R.layout.debug_overlay, null);
            
            tvAIStatus = overlayView.findViewById(R.id.tv_ai_status);
            tvPerformanceMetrics = overlayView.findViewById(R.id.tv_performance_metrics);
            tvMemoryUsage = overlayView.findViewById(R.id.tv_memory_usage);
            
            // Set window parameters for overlay
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 10;
            params.y = 100;
            
            windowManager.addView(overlayView, params);
            isOverlayVisible = true;
            
            Log.d(TAG, "Debug overlay view created and displayed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating overlay view", e);
            createFallbackOverlay();
        }
    }
    
    private void createFallbackOverlay() {
        try {
            // Create simple text-based overlay if layout fails
            TextView fallbackView = new TextView(this);
            fallbackView.setText("AI Debug\nLoading...");
            fallbackView.setTextColor(android.graphics.Color.WHITE);
            fallbackView.setBackgroundColor(android.graphics.Color.argb(128, 0, 0, 0));
            fallbackView.setPadding(10, 10, 10, 10);
            
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 10;
            params.y = 100;
            
            windowManager.addView(fallbackView, params);
            overlayView = fallbackView;
            isOverlayVisible = true;
            
            Log.d(TAG, "Fallback debug overlay created");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to create fallback overlay", e);
        }
    }
    
    private void startMetricsUpdates() {
        updateHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateDebugMetrics();
                updateHandler.postDelayed(this, 1000); // Update every second
            }
        };
        updateHandler.post(updateRunnable);
    }
    
    private void updateDebugMetrics() {
        if (!isOverlayVisible || overlayView == null) return;
        
        try {
            StringBuilder debugInfo = new StringBuilder();
            
            // AI Status
            if (strategyAgent != null && strategyAgent.isActive()) {
                debugInfo.append("AI: Active\n");
                debugInfo.append("Success Rate: ").append(String.format("%.1f%%", strategyAgent.getSuccessRate() * 100)).append("\n");
                debugInfo.append("Actions: ").append(strategyAgent.getTotalActionsExecuted()).append("\n");
            } else {
                debugInfo.append("AI: Inactive\n");
            }
            
            // Memory Usage
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            debugInfo.append("Memory: ").append(usedMemory / 1024 / 1024).append("MB\n");
            
            // AI Stack Status
            if (aiStackManager != null) {
                debugInfo.append("ND4J: ").append(aiStackManager.isND4JEnabled() ? "ON" : "OFF").append("\n");
            }
            
            // Update overlay text
            if (overlayView instanceof TextView) {
                ((TextView) overlayView).setText(debugInfo.toString());
            } else if (tvAIStatus != null) {
                tvAIStatus.setText("AI Status: " + (strategyAgent != null && strategyAgent.isActive() ? "Active" : "Inactive"));
                if (tvPerformanceMetrics != null) {
                    tvPerformanceMetrics.setText("Success: " + String.format("%.1f%%", 
                        strategyAgent != null ? strategyAgent.getSuccessRate() * 100 : 0));
                }
                if (tvMemoryUsage != null) {
                    tvMemoryUsage.setText("Memory: " + (usedMemory / 1024 / 1024) + "MB");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating debug metrics", e);
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("SHOW_OVERLAY".equals(action)) {
                showOverlay();
            } else if ("HIDE_OVERLAY".equals(action)) {
                hideOverlay();
            }
        }
        return START_STICKY;
    }
    
    public void showOverlay() {
        if (!isOverlayVisible && overlayView != null) {
            try {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) overlayView.getLayoutParams();
                windowManager.addView(overlayView, params);
                isOverlayVisible = true;
                Log.d(TAG, "Debug overlay shown");
            } catch (Exception e) {
                Log.e(TAG, "Error showing overlay", e);
            }
        }
    }
    
    public void hideOverlay() {
        if (isOverlayVisible && overlayView != null) {
            try {
                windowManager.removeView(overlayView);
                isOverlayVisible = false;
                Log.d(TAG, "Debug overlay hidden");
            } catch (Exception e) {
                Log.e(TAG, "Error hiding overlay", e);
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Stop updates
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
        
        // Remove overlay
        if (isOverlayVisible && overlayView != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception e) {
                Log.e(TAG, "Error removing overlay on destroy", e);
            }
        }
        
        Log.d(TAG, "Debug Overlay Service destroyed");
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
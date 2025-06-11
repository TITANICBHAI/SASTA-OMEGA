package com.gestureai.gameautomation.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.*;
import android.os.IBinder;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.GameAutomationEngine;
import com.gestureai.gameautomation.ObjectDetectionEngine;
import com.gestureai.gameautomation.PerformanceTracker;
import com.gestureai.gameautomation.ai.ResourceMonitor;
import com.gestureai.gameautomation.ai.ZoneTracker;
import java.util.List;
import java.util.ArrayList;

public class DebugOverlayService extends Service {
    private static final String TAG = "DebugOverlayService";
    
    private WindowManager windowManager;
    private View overlayView;
    private boolean isOverlayVisible = false;
    
    // Debug visualization components
    private ImageView ivDebugCanvas;
    private TextView tvTouchCoordinates;
    private TextView tvPerformanceMetrics;
    private TextView tvAIStatus;
    private Button btnToggleDetection;
    private Button btnToggleZones;
    private Button btnTogglePerformance;
    
    // Debug data
    private List<TouchPoint> touchHistory;
    private List<ObjectDetectionEngine.DetectedObject> detectedObjects;
    private Bitmap debugCanvas;
    private Canvas debugCanvasDrawer;
    private Paint touchPaint;
    private Paint objectPaint;
    private Paint zonePaint;
    
    // Monitoring flags
    private boolean showObjectDetection = true;
    private boolean showZoneTracking = true;
    private boolean showPerformanceData = true;
    private boolean showTouchCoordinates = true;
    
    // Update components
    private android.os.Handler updateHandler;
    private Runnable updateRunnable;
    
    @Override
    public void onCreate() {
        super.onCreate();
        initializeDebugComponents();
        createOverlayView();
        setupUpdateLoop();
    }
    
    private void initializeDebugComponents() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        touchHistory = new ArrayList<>();
        detectedObjects = new ArrayList<>();
        
        // Initialize paint objects for drawing
        touchPaint = new Paint();
        touchPaint.setColor(Color.RED);
        touchPaint.setStyle(Paint.Style.FILL);
        touchPaint.setAntiAlias(true);
        
        objectPaint = new Paint();
        objectPaint.setColor(Color.GREEN);
        objectPaint.setStyle(Paint.Style.STROKE);
        objectPaint.setStrokeWidth(3);
        objectPaint.setAntiAlias(true);
        
        zonePaint = new Paint();
        zonePaint.setColor(Color.BLUE);
        zonePaint.setStyle(Paint.Style.STROKE);
        zonePaint.setStrokeWidth(2);
        zonePaint.setPathEffect(new DashPathEffect(new float[]{10, 5}, 0));
        
        // Create debug canvas
        createDebugCanvas();
    }
    
    private void createDebugCanvas() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        
        debugCanvas = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        debugCanvasDrawer = new Canvas(debugCanvas);
    }
    
    private void createOverlayView() {
        overlayView = LayoutInflater.from(this).inflate(R.layout.debug_overlay_layout, null);
        
        initializeOverlayViews();
        setupOverlayButtons();
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT);
        
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 0;
        
        windowManager.addView(overlayView, params);
        isOverlayVisible = true;
    }
    
    private void initializeOverlayViews() {
        ivDebugCanvas = overlayView.findViewById(R.id.iv_debug_canvas);
        tvTouchCoordinates = overlayView.findViewById(R.id.tv_touch_coordinates);
        tvPerformanceMetrics = overlayView.findViewById(R.id.tv_performance_metrics);
        tvAIStatus = overlayView.findViewById(R.id.tv_ai_status);
        btnToggleDetection = overlayView.findViewById(R.id.btn_toggle_detection);
        btnToggleZones = overlayView.findViewById(R.id.btn_toggle_zones);
        btnTogglePerformance = overlayView.findViewById(R.id.btn_toggle_performance);
    }
    
    private void setupOverlayButtons() {
        btnToggleDetection.setOnClickListener(v -> {
            showObjectDetection = !showObjectDetection;
            btnToggleDetection.setText(showObjectDetection ? "Hide Detection" : "Show Detection");
            updateDebugVisualization();
        });
        
        btnToggleZones.setOnClickListener(v -> {
            showZoneTracking = !showZoneTracking;
            btnToggleZones.setText(showZoneTracking ? "Hide Zones" : "Show Zones");
            updateDebugVisualization();
        });
        
        btnTogglePerformance.setOnClickListener(v -> {
            showPerformanceData = !showPerformanceData;
            btnTogglePerformance.setText(showPerformanceData ? "Hide Performance" : "Show Performance");
            tvPerformanceMetrics.setVisibility(showPerformanceData ? View.VISIBLE : View.GONE);
        });
        
        // Add close button
        Button btnClose = overlayView.findViewById(R.id.btn_close_debug);
        btnClose.setOnClickListener(v -> stopSelf());
    }
    
    private void setupUpdateLoop() {
        updateHandler = new android.os.Handler();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateDebugData();
                updateDebugVisualization();
                updateHandler.postDelayed(this, 100); // Update every 100ms
            }
        };
        updateHandler.post(updateRunnable);
    }
    
    private void updateDebugData() {
        // Get latest detection data
        GameAutomationEngine engine = GameAutomationEngine.getInstance();
        if (engine != null) {
            // Update detected objects
            ObjectDetectionEngine detector = engine.getObjectDetectionEngine();
            if (detector != null) {
                detectedObjects = detector.getLatestDetections();
            }
            
            // Update AI status
            updateAIStatus(engine);
        }
        
        // Update performance data
        if (showPerformanceData) {
            updatePerformanceMetrics();
        }
        
        // Limit touch history size
        if (touchHistory.size() > 50) {
            touchHistory.remove(0);
        }
    }
    
    private void updateAIStatus(GameAutomationEngine engine) {
        StringBuilder aiStatus = new StringBuilder();
        aiStatus.append("AI Status:\n");
        
        if (engine.getStrategyProcessor() != null) {
            aiStatus.append("Strategy: Active\n");
        } else {
            aiStatus.append("Strategy: Inactive\n");
        }
        
        if (engine.getObjectLabelerEngine() != null) {
            aiStatus.append("Labeler: Active\n");
        } else {
            aiStatus.append("Labeler: Inactive\n");
        }
        
        aiStatus.append("Automation: ").append(engine.isAutomationEnabled() ? "ON" : "OFF");
        
        tvAIStatus.setText(aiStatus.toString());
    }
    
    private void updatePerformanceMetrics() {
        PerformanceTracker tracker = PerformanceTracker.getInstance();
        ResourceMonitor monitor = ResourceMonitor.getInstance();
        
        if (tracker != null && monitor != null) {
            PerformanceTracker.PerformanceData perfData = tracker.getCurrentPerformance();
            ResourceMonitor.SystemResources sysRes = monitor.getCurrentResources();
            
            String perfText = String.format(
                "Performance:\nFPS: %.1f\nCPU: %.1f%%\nMemory: %.1f MB\nLatency: %.0f ms",
                perfData.currentFPS,
                sysRes.cpuUsage,
                sysRes.memoryUsage,
                perfData.touchLatency
            );
            
            tvPerformanceMetrics.setText(perfText);
        }
    }
    
    private void updateDebugVisualization() {
        // Clear debug canvas
        debugCanvasDrawer.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        
        // Draw touch history
        if (showTouchCoordinates) {
            drawTouchHistory();
        }
        
        // Draw object detection
        if (showObjectDetection) {
            drawDetectedObjects();
        }
        
        // Draw zone tracking
        if (showZoneTracking) {
            drawZoneTracking();
        }
        
        // Update canvas display
        ivDebugCanvas.setImageBitmap(debugCanvas);
    }
    
    private void drawTouchHistory() {
        for (int i = 0; i < touchHistory.size(); i++) {
            TouchPoint point = touchHistory.get(i);
            float alpha = (float) i / touchHistory.size(); // Fade older points
            
            Paint fadingPaint = new Paint(touchPaint);
            fadingPaint.setAlpha((int) (255 * alpha));
            
            debugCanvasDrawer.drawCircle(point.x, point.y, 10, fadingPaint);
            
            // Draw touch coordinates for latest point
            if (i == touchHistory.size() - 1) {
                String coordinates = String.format("(%d, %d)", (int) point.x, (int) point.y);
                tvTouchCoordinates.setText("Touch: " + coordinates);
            }
        }
    }
    
    private void drawDetectedObjects() {
        for (ObjectDetectionEngine.DetectedObject obj : detectedObjects) {
            // Draw bounding box
            Rect bounds = obj.getBoundingBox();
            debugCanvasDrawer.drawRect(bounds, objectPaint);
            
            // Draw label
            Paint textPaint = new Paint();
            textPaint.setColor(Color.GREEN);
            textPaint.setTextSize(24);
            textPaint.setAntiAlias(true);
            
            String label = String.format("%s (%.2f)", obj.getLabel(), obj.getConfidence());
            debugCanvasDrawer.drawText(label, bounds.left, bounds.top - 10, textPaint);
        }
    }
    
    private void drawZoneTracking() {
        ZoneTracker zoneTracker = ZoneTracker.getInstance();
        if (zoneTracker != null) {
            // Draw safe zone
            ZoneTracker.ZoneInfo safeZone = zoneTracker.getCurrentSafeZone();
            if (safeZone != null) {
                debugCanvasDrawer.drawCircle(
                    safeZone.centerX, safeZone.centerY, safeZone.radius, zonePaint);
                
                // Draw zone shrink direction
                Paint arrowPaint = new Paint(zonePaint);
                arrowPaint.setStrokeWidth(5);
                
                float arrowLength = 50;
                float arrowX = safeZone.centerX + arrowLength * (float) Math.cos(safeZone.shrinkDirection);
                float arrowY = safeZone.centerY + arrowLength * (float) Math.sin(safeZone.shrinkDirection);
                
                debugCanvasDrawer.drawLine(safeZone.centerX, safeZone.centerY, arrowX, arrowY, arrowPaint);
            }
            
            // Draw danger zones
            List<ZoneTracker.ZoneInfo> dangerZones = zoneTracker.getDangerZones();
            Paint dangerPaint = new Paint();
            dangerPaint.setColor(Color.RED);
            dangerPaint.setStyle(Paint.Style.STROKE);
            dangerPaint.setStrokeWidth(2);
            
            for (ZoneTracker.ZoneInfo zone : dangerZones) {
                debugCanvasDrawer.drawRect(zone.bounds, dangerPaint);
            }
        }
    }
    
    public void addTouchPoint(float x, float y) {
        touchHistory.add(new TouchPoint(x, y, System.currentTimeMillis()));
    }
    
    public void updateDetectedObjects(List<ObjectDetectionEngine.DetectedObject> objects) {
        this.detectedObjects = new ArrayList<>(objects);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if (updateHandler != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
        
        if (overlayView != null && isOverlayVisible) {
            windowManager.removeView(overlayView);
            isOverlayVisible = false;
        }
        
        if (debugCanvas != null && !debugCanvas.isRecycled()) {
            debugCanvas.recycle();
        }
    }
    
    // Touch point data class
    private static class TouchPoint {
        public float x, y;
        public long timestamp;
        
        public TouchPoint(float x, float y, long timestamp) {
            this.x = x;
            this.y = y;
            this.timestamp = timestamp;
        }
    }
}
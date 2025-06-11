package com.gestureai.gameautomation.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.GameAutomationEngine;
import com.gestureai.gameautomation.ObjectDetectionEngine;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import java.util.List;

/**
 * Screen monitoring fragment for real-time game state analysis
 * Displays live screen capture with object detection overlays
 */
public class ScreenMonitorFragment extends Fragment {
    
    private ImageView imageViewScreen;
    private TextView tvDetectedObjects;
    private TextView tvGameState;
    private Button btnStartMonitoring;
    private Button btnStopMonitoring;
    private Switch switchObjectDetection;
    private Switch switchGameAnalysis;
    
    private GameAutomationEngine automationEngine;
    private ObjectDetectionEngine detectionEngine;
    private GameStrategyAgent strategyAgent;
    
    private Handler updateHandler;
    private Runnable updateRunnable;
    private boolean isMonitoring = false;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_screen_monitor, container, false);
        
        initializeViews(view);
        setupListeners();
        initializeBackend();
        
        return view;
    }
    
    private void initializeViews(View view) {
        imageViewScreen = view.findViewById(R.id.imageview_screen);
        tvDetectedObjects = view.findViewById(R.id.tv_detected_objects);
        tvGameState = view.findViewById(R.id.tv_game_state);
        btnStartMonitoring = view.findViewById(R.id.btn_start_monitoring);
        btnStopMonitoring = view.findViewById(R.id.btn_stop_monitoring);
        switchObjectDetection = view.findViewById(R.id.switch_object_detection);
        switchGameAnalysis = view.findViewById(R.id.switch_game_analysis);
    }
    
    private void initializeBackend() {
        automationEngine = new GameAutomationEngine(getContext());
        detectionEngine = new ObjectDetectionEngine(getContext());
        strategyAgent = new GameStrategyAgent(getContext());
    }
    
    private void setupListeners() {
        btnStartMonitoring.setOnClickListener(v -> startScreenMonitoring());
        btnStopMonitoring.setOnClickListener(v -> stopScreenMonitoring());
    }
    
    private void startScreenMonitoring() {
        if (!isMonitoring) {
            isMonitoring = true;
            btnStartMonitoring.setEnabled(false);
            btnStopMonitoring.setEnabled(true);
            
            startRealTimeUpdates();
            android.widget.Toast.makeText(getContext(), "Screen monitoring started", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopScreenMonitoring() {
        if (isMonitoring) {
            isMonitoring = false;
            btnStartMonitoring.setEnabled(true);
            btnStopMonitoring.setEnabled(false);
            
            stopRealTimeUpdates();
            android.widget.Toast.makeText(getContext(), "Screen monitoring stopped", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startRealTimeUpdates() {
        updateHandler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isMonitoring) {
                    updateScreenCapture();
                    if (switchObjectDetection.isChecked()) {
                        updateObjectDetection();
                    }
                    if (switchGameAnalysis.isChecked()) {
                        updateGameAnalysis();
                    }
                }
                updateHandler.postDelayed(this, 500); // Update every 500ms
            }
        };
        updateHandler.post(updateRunnable);
    }
    
    private void stopRealTimeUpdates() {
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }
    
    private void updateScreenCapture() {
        Bitmap screenshot = automationEngine.captureScreen();
        if (screenshot != null) {
            imageViewScreen.setImageBitmap(screenshot);
        }
    }
    
    private void updateObjectDetection() {
        Bitmap currentScreen = automationEngine.captureScreen();
        if (currentScreen != null) {
            List<ObjectDetectionEngine.DetectedObject> objects = detectionEngine.detectObjects(currentScreen);
            
            StringBuilder objectText = new StringBuilder();
            objectText.append("Detected Objects (").append(objects.size()).append("):\n");
            
            for (ObjectDetectionEngine.DetectedObject obj : objects) {
                objectText.append("• ").append(obj.name)
                          .append(" (").append(String.format("%.1f%%", obj.confidence * 100))
                          .append(")\n");
            }
            
            tvDetectedObjects.setText(objectText.toString());
        }
    }
    
    private void updateGameAnalysis() {
        GameStrategyAgent.UniversalGameState gameState = analyzeCurrentGameState();
        
        StringBuilder stateText = new StringBuilder();
        stateText.append("Game Analysis:\n");
        stateText.append("Player Position: (").append(gameState.playerX).append(", ").append(gameState.playerY).append(")\n");
        stateText.append("Threat Level: ").append(String.format("%.1f%%", gameState.threatLevel * 100)).append("\n");
        stateText.append("Opportunity Level: ").append(String.format("%.1f%%", gameState.opportunityLevel * 100)).append("\n");
        stateText.append("Objects on Screen: ").append(gameState.objectCount).append("\n");
        stateText.append("Game Score: ").append(gameState.gameScore).append("\n");
        
        tvGameState.setText(stateText.toString());
    }
    
    private GameStrategyAgent.UniversalGameState analyzeCurrentGameState() {
        // Create game state from current screen analysis
        GameStrategyAgent.UniversalGameState state = new GameStrategyAgent.UniversalGameState();
        
        // Get screen dimensions
        state.screenWidth = getResources().getDisplayMetrics().widthPixels;
        state.screenHeight = getResources().getDisplayMetrics().heightPixels;
        
        // Analyze current screen for game elements
        Bitmap currentScreen = automationEngine.captureScreen();
        if (currentScreen != null) {
            // Extract game state information from screen
            state.playerX = state.screenWidth / 2; // Default center position
            state.playerY = state.screenHeight / 2;
            
            // Get object count from detection engine
            List<ObjectDetectionEngine.DetectedObject> objects = detectionEngine.detectObjects(currentScreen);
            state.objectCount = objects.size();
            
            // Calculate threat and opportunity levels based on detected objects
            float threatSum = 0;
            float opportunitySum = 0;
            
            for (ObjectDetectionEngine.DetectedObject obj : objects) {
                if (obj.name.toLowerCase().contains("enemy") || obj.name.toLowerCase().contains("danger")) {
                    threatSum += obj.confidence;
                } else if (obj.name.toLowerCase().contains("powerup") || obj.name.toLowerCase().contains("coin")) {
                    opportunitySum += obj.confidence;
                }
                
                // Set nearest object coordinates
                if (state.nearestObjectX == 0 && state.nearestObjectY == 0) {
                    state.nearestObjectX = obj.boundingBox.centerX();
                    state.nearestObjectY = obj.boundingBox.centerY();
                }
            }
            
            state.threatLevel = Math.min(1.0f, threatSum / Math.max(1, objects.size()));
            state.opportunityLevel = Math.min(1.0f, opportunitySum / Math.max(1, objects.size()));
        }
        
        // Set other state fields with reasonable defaults
        state.gameScore = 0; // Would need game-specific logic
        state.gameSpeed = 1.0f;
        state.timeInGame = System.currentTimeMillis() / 1000f;
        state.averageReward = 0.5f;
        state.consecutiveSuccess = 0;
        state.gameType = 0.5f; // Unknown game type
        state.difficultyLevel = 0.5f;
        state.powerUpActive = false;
        state.healthLevel = 1.0f;
        
        return state;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isMonitoring) {
            stopScreenMonitoring();
        }
    }
}
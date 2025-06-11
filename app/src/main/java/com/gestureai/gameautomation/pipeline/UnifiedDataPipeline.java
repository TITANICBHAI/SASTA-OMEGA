package com.gestureai.gameautomation.pipeline;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.gestureai.gameautomation.GameAction;
import com.gestureai.gameautomation.data.UniversalGameState;
import com.gestureai.gameautomation.data.GameContext;
import com.gestureai.gameautomation.DetectedObject;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.ObjectDetectionEngine;
import com.gestureai.gameautomation.services.ScreenCaptureService;
import com.gestureai.gameautomation.services.TouchAutomationService;
import com.gestureai.gameautomation.ObjectLabelerEngine;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unified data pipeline connecting all system components
 * Screen Capture → Object Detection → AI Strategy → Touch Execution
 */
public class UnifiedDataPipeline {
    private static final String TAG = "UnifiedDataPipeline";
    
    private Context context;
    private ScreenCaptureService screenCapture;
    private ObjectDetectionEngine objectDetection;
    private ObjectLabelerEngine objectLabeler;
    private GameStrategyAgent strategyAgent;
    private TouchAutomationService touchAutomation;
    
    private UniversalGameState currentGameState;
    private GameContext gameContext;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private PipelineMetrics metrics;
    
    // Pipeline configuration
    private int frameProcessingInterval = 100; // ms
    private float confidenceThreshold = 0.7f;
    private boolean enableLearning = true;
    
    public UnifiedDataPipeline(Context context) {
        this.context = context;
        this.currentGameState = new UniversalGameState();
        this.gameContext = new GameContext();
        this.metrics = new PipelineMetrics();
        
        initializeComponents();
    }
    
    /**
     * Initialize all pipeline components
     */
    private void initializeComponents() {
        try {
            // Initialize detection engine
            objectDetection = new ObjectDetectionEngine(context);
            
            // Initialize labeling engine
            objectLabeler = new ObjectLabelerEngine(context);
            
            // Initialize strategy agent
            strategyAgent = new GameStrategyAgent(context);
            
            Log.i(TAG, "Pipeline components initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize pipeline components", e);
        }
    }
    
    /**
     * Connect to running services
     */
    public void connectServices(ScreenCaptureService screenCapture, 
                               TouchAutomationService touchAutomation) {
        this.screenCapture = screenCapture;
        this.touchAutomation = touchAutomation;
        
        Log.i(TAG, "Services connected to pipeline");
    }
    
    /**
     * Start the unified data pipeline
     */
    public void startPipeline() {
        if (isRunning.get()) {
            Log.w(TAG, "Pipeline already running");
            return;
        }
        
        if (!validateComponents()) {
            Log.e(TAG, "Cannot start pipeline - missing components");
            return;
        }
        
        isRunning.set(true);
        
        // Start main processing loop
        CompletableFuture.runAsync(this::processingLoop);
        
        Log.i(TAG, "Unified data pipeline started");
    }
    
    /**
     * Stop the pipeline
     */
    public void stopPipeline() {
        isRunning.set(false);
        Log.i(TAG, "Pipeline stopped");
    }
    
    /**
     * Main processing loop - processes frames continuously
     */
    private void processingLoop() {
        while (isRunning.get()) {
            try {
                // Step 1: Capture screen frame
                Bitmap currentFrame = captureFrame();
                if (currentFrame == null) {
                    Thread.sleep(frameProcessingInterval);
                    continue;
                }
                
                // Step 2: Detect objects in frame
                List<DetectedObject> detectedObjects = detectObjects(currentFrame);
                
                // Step 3: Update game state
                updateGameState(detectedObjects, currentFrame);
                
                // Step 4: Generate strategy decision
                GameAction strategicAction = generateAction();
                
                // Step 5: Execute touch action
                executeAction(strategicAction);
                
                // Step 6: Learning feedback (if enabled)
                if (enableLearning) {
                    provideLearningFeedback(strategicAction);
                }
                
                // Update metrics
                metrics.incrementFramesProcessed();
                
                Thread.sleep(frameProcessingInterval);
                
            } catch (Exception e) {
                Log.e(TAG, "Error in processing loop", e);
                metrics.incrementErrors();
                
                try {
                    Thread.sleep(frameProcessingInterval * 2);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }
    }
    
    /**
     * Step 1: Capture current screen frame
     */
    private Bitmap captureFrame() {
        try {
            if (screenCapture != null) {
                return screenCapture.captureScreen();
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Failed to capture frame", e);
            return null;
        }
    }
    
    /**
     * Step 2: Detect objects in the captured frame
     */
    private List<DetectedObject> detectObjects(Bitmap frame) {
        try {
            return objectDetection.detectObjects(frame).get();
        } catch (Exception e) {
            Log.e(TAG, "Object detection failed", e);
            return List.of();
        }
    }
    
    /**
     * Step 3: Update game state based on detected objects
     */
    private void updateGameState(List<DetectedObject> objects, Bitmap frame) {
        try {
            currentGameState.objectCount = objects.size();
            currentGameState.threatLevel = calculateThreatLevel(objects);
            currentGameState.opportunityLevel = calculateOpportunityLevel(objects);
            updatePlayerPosition(objects, frame);
            updateHealthLevel(objects);
            currentGameState.setScreenDimensions(frame.getWidth(), frame.getHeight());
            currentGameState.timestamp = System.currentTimeMillis();
            gameContext.updateContext(currentGameState);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to update game state", e);
        }
    }
    
    /**
     * Step 4: Generate strategic action using AI
     */
    private GameAction generateAction() {
        try {
            return strategyAgent.analyzeGameContext(currentGameState);
        } catch (Exception e) {
            Log.e(TAG, "Strategy generation failed", e);
            return new GameAction("WAIT", 540, 960, 0.5f, "fallback");
        }
    }
    
    /**
     * Step 5: Execute the generated action
     */
    private void executeAction(GameAction action) {
        try {
            if (touchAutomation != null && action != null) {
                if (action.getConfidence() >= confidenceThreshold) {
                    touchAutomation.executeAction(action);
                    metrics.incrementActionsExecuted();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Action execution failed", e);
        }
    }
    
    /**
     * Step 6: Provide learning feedback to AI components
     */
    private void provideLearningFeedback(GameAction action) {
        try {
            boolean actionSuccess = evaluateActionSuccess(action);
            float reward = actionSuccess ? 1.0f : -0.1f;
            
            if (strategyAgent != null) {
                strategyAgent.updateStrategy(currentGameState, action, reward);
            }
            
            if (objectLabeler != null && actionSuccess) {
                objectLabeler.recordSuccessfulDetection(action.getObjectName());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Learning feedback failed", e);
        }
    }
    
    private float calculateThreatLevel(List<DetectedObject> objects) {
        float threat = 0f;
        for (DetectedObject obj : objects) {
            if (obj.getLabel().contains("enemy") || obj.getLabel().contains("danger")) {
                threat += obj.getConfidence() * 0.3f;
            }
        }
        return Math.min(1.0f, threat);
    }
    
    private float calculateOpportunityLevel(List<DetectedObject> objects) {
        float opportunity = 0f;
        for (DetectedObject obj : objects) {
            if (obj.getLabel().contains("reward") || obj.getLabel().contains("item")) {
                opportunity += obj.getConfidence() * 0.2f;
            }
        }
        return Math.min(1.0f, opportunity);
    }
    
    private void updatePlayerPosition(List<DetectedObject> objects, Bitmap frame) {
        boolean playerFound = false;
        for (DetectedObject obj : objects) {
            if (obj.getLabel().contains("player")) {
                currentGameState.playerX = obj.getX() + obj.getWidth() / 2f;
                currentGameState.playerY = obj.getY() + obj.getHeight() / 2f;
                playerFound = true;
                break;
            }
        }
        
        if (!playerFound) {
            currentGameState.playerX = frame.getWidth() / 2f;
            currentGameState.playerY = frame.getHeight() / 2f;
        }
    }
    
    private void updateHealthLevel(List<DetectedObject> objects) {
        for (DetectedObject obj : objects) {
            if (obj.getLabel().contains("health")) {
                currentGameState.healthLevel = obj.getConfidence();
                return;
            }
        }
    }
    
    private boolean evaluateActionSuccess(GameAction action) {
        try {
            Thread.sleep(200);
            float currentScore = currentGameState.gameScore;
            float previousScore = gameContext.gameState != null ? gameContext.gameState.gameScore : 0f;
            return currentScore >= previousScore || action.getConfidence() > 0.8f;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean validateComponents() {
        return objectDetection != null && strategyAgent != null && currentGameState != null;
    }
    
    public PipelineMetrics getMetrics() { return metrics; }
    public UniversalGameState getCurrentGameState() { return currentGameState; }
    public GameContext getGameContext() { return gameContext; }
    public boolean isRunning() { return isRunning.get(); }
    
    public void updateConfiguration(int frameInterval, float confidenceThresh, boolean learning) {
        this.frameProcessingInterval = frameInterval;
        this.confidenceThreshold = confidenceThresh;
        this.enableLearning = learning;
        Log.i(TAG, "Pipeline configuration updated");
    }
    
    public static class PipelineMetrics {
        private long framesProcessed = 0;
        private long actionsExecuted = 0;
        private long errors = 0;
        private long startTime = System.currentTimeMillis();
        
        public void incrementFramesProcessed() { framesProcessed++; }
        public void incrementActionsExecuted() { actionsExecuted++; }
        public void incrementErrors() { errors++; }
        
        public long getFramesProcessed() { return framesProcessed; }
        public long getActionsExecuted() { return actionsExecuted; }
        public long getErrors() { return errors; }
        public long getRuntime() { return System.currentTimeMillis() - startTime; }
        public float getFPS() { 
            long runtime = getRuntime();
            return runtime > 0 ? (framesProcessed * 1000f) / runtime : 0f;
        }
    }
}
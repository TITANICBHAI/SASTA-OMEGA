package com.gestureai.gameautomation.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.gestureai.gameautomation.GameAction;
import com.gestureai.gameautomation.data.UniversalGameState;
import com.gestureai.gameautomation.DetectedObject;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.ai.ObjectDetectionEngine;
import com.gestureai.gameautomation.services.ScreenCaptureService;
import com.gestureai.gameautomation.services.TouchAutomationService;
import com.gestureai.gameautomation.services.ServiceCommunicationProtocol;
import com.gestureai.gameautomation.pipeline.UnifiedDataPipeline;
import com.gestureai.gameautomation.ObjectLabelerEngine;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * System Integration Coordinator
 * Connects all components into a cohesive system with proper data flow
 */
public class SystemIntegrationCoordinator implements ServiceCommunicationProtocol.MessageListener {
    private static final String TAG = "SystemIntegrator";
    
    private static SystemIntegrationCoordinator instance;
    private Context context;
    
    // Core components
    private UnifiedDataPipeline dataPipeline;
    private ServiceCommunicationProtocol communicationProtocol;
    private GameStrategyAgent strategyAgent;
    private ObjectDetectionEngine objectDetection;
    private ObjectLabelerEngine objectLabeler;
    
    // Service references
    private ScreenCaptureService screenCapture;
    private TouchAutomationService touchAutomation;
    
    // System state with failure recovery
    private UniversalGameState currentGameState;
    private AtomicBoolean systemRunning = new AtomicBoolean(false);
    private ExecutorService executor;
    private SystemMetrics metrics;
    private volatile boolean systemHealthy = true;
    private int consecutiveFailures = 0;
    private static final int MAX_CONSECUTIVE_FAILURES = 5;
    private final Object coordinatorLock = new Object();
    
    private SystemIntegrationCoordinator(Context context) {
        this.context = context;
        this.executor = Executors.newFixedThreadPool(4); // Limit thread pool to prevent exhaustion
        this.metrics = new SystemMetrics();
        
        initializeSystem();
    }
    
    public static synchronized SystemIntegrationCoordinator getInstance(Context context) {
        if (instance == null) {
            instance = new SystemIntegrationCoordinator(context);
        }
        return instance;
    }
    
    /**
     * Initialize all system components
     */
    private void initializeSystem() {
        try {
            // Initialize communication protocol
            communicationProtocol = ServiceCommunicationProtocol.getInstance(context);
            communicationProtocol.addMessageListener(this);
            
            // Initialize data pipeline
            dataPipeline = new UnifiedDataPipeline(context);
            
            // Initialize AI components with proper context
            strategyAgent = GameStrategyAgent.getInstance(context);
            objectDetection = new ObjectDetectionEngine(context);
            objectLabeler = new ObjectLabelerEngine(context);
            
            // Initialize game state
            currentGameState = new UniversalGameState();
            
            Log.i(TAG, "System Integration Coordinator initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize system", e);
        }
    }
    
    /**
     * Start the integrated system
     */
    public void startSystem() {
        if (systemRunning.get()) {
            Log.w(TAG, "System already running");
            return;
        }
        
        try {
            // Register this coordinator with communication protocol
            communicationProtocol.registerService("SystemIntegrationCoordinator", 
                ServiceCommunicationProtocol.ServiceState.STARTING);
            
            // Start data pipeline if services are ready
            if (communicationProtocol.areAllServicesReady()) {
                startDataPipeline();
            }
            
            systemRunning.set(true);
            communicationProtocol.updateServiceState("SystemIntegrationCoordinator", 
                ServiceCommunicationProtocol.ServiceState.RUNNING);
            
            Log.i(TAG, "System started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start system", e);
            communicationProtocol.updateServiceState("SystemIntegrationCoordinator", 
                ServiceCommunicationProtocol.ServiceState.ERROR);
        }
    }
    
    /**
     * Stop the integrated system
     */
    public void stopSystem() {
        if (!systemRunning.get()) {
            return;
        }
        
        try {
            systemRunning.set(false);
            
            // Stop data pipeline
            if (dataPipeline != null) {
                dataPipeline.stopPipeline();
            }
            
            // Cleanup AI components
            if (strategyAgent != null) {
                strategyAgent.cleanup();
            }
            
            // Shutdown executor
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            communicationProtocol.updateServiceState("SystemIntegrationCoordinator", 
                ServiceCommunicationProtocol.ServiceState.STOPPED);
            
            Log.i(TAG, "System stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping system", e);
        }
    }
    
    /**
     * Register service instances for direct access
     */
    public void registerService(String serviceName, Object serviceInstance) {
        try {
            switch (serviceName) {
                case "ScreenCaptureService":
                    screenCapture = (ScreenCaptureService) serviceInstance;
                    break;
                case "TouchAutomationService":
                    touchAutomation = (TouchAutomationService) serviceInstance;
                    break;
            }
            
            // Connect to data pipeline when both critical services are available
            if (screenCapture != null && touchAutomation != null && dataPipeline != null) {
                dataPipeline.connectServices(screenCapture, touchAutomation);
                Log.i(TAG, "Services connected to data pipeline");
            }
            
            // Register with communication protocol
            communicationProtocol.registerServiceInstance(serviceName, serviceInstance);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to register service: " + serviceName, e);
        }
    }
    
    /**
     * Start the data pipeline
     */
    private void startDataPipeline() {
        try {
            if (dataPipeline != null && !dataPipeline.isRunning()) {
                dataPipeline.startPipeline();
                Log.i(TAG, "Data pipeline started");
                metrics.pipelineStartTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start data pipeline", e);
        }
    }
    
    /**
     * Process captured frame through the complete pipeline
     */
    public CompletableFuture<GameAction> processFrame(Bitmap frame) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                metrics.framesProcessed++;
                
                // Step 1: Object Detection
                List<DetectedObject> objects = objectDetection.detectObjects(frame).get();
                
                // Step 2: Update Game State
                updateGameState(objects, frame);
                
                // Step 3: Strategy Decision
                GameAction action = strategyAgent.analyzeGameContext(currentGameState);
                
                // Step 4: Broadcast updates
                communicationProtocol.broadcastObjectDetections(objects);
                communicationProtocol.broadcastGameStateUpdate(currentGameState);
                communicationProtocol.broadcastActionExecution(action);
                
                return action;
                
            } catch (Exception e) {
                Log.e(TAG, "Error processing frame", e);
                metrics.errors++;
                return new GameAction("WAIT", 540, 960, 0.1f, "error");
            }
        }, executor);
    }
    
    /**
     * Execute action through touch automation
     */
    public void executeAction(GameAction action) {
        try {
            if (touchAutomation != null && action != null) {
                touchAutomation.executeAction(action);
                metrics.actionsExecuted++;
                
                // Provide learning feedback
                provideLearningFeedback(action, true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute action", e);
            metrics.errors++;
            
            // Provide negative feedback
            if (action != null) {
                provideLearningFeedback(action, false);
            }
        }
    }
    
    /**
     * Update game state from detected objects
     */
    private void updateGameState(List<DetectedObject> objects, Bitmap frame) {
        try {
            // Update object count
            currentGameState.objectCount = objects.size();
            
            // Calculate threat and opportunity levels
            float threatLevel = 0f;
            float opportunityLevel = 0f;
            
            for (DetectedObject obj : objects) {
                String label = obj.getLabel().toLowerCase();
                float confidence = obj.getConfidence();
                
                if (label.contains("enemy") || label.contains("danger") || label.contains("threat")) {
                    threatLevel += confidence * 0.3f;
                } else if (label.contains("reward") || label.contains("coin") || label.contains("item")) {
                    opportunityLevel += confidence * 0.2f;
                } else if (label.contains("health") || label.contains("hp")) {
                    currentGameState.healthLevel = confidence;
                }
            }
            
            currentGameState.threatLevel = Math.min(1.0f, threatLevel);
            currentGameState.opportunityLevel = Math.min(1.0f, opportunityLevel);
            
            // Update screen dimensions
            currentGameState.setScreenDimensions(frame.getWidth(), frame.getHeight());
            
            // Update timestamp
            currentGameState.timestamp = System.currentTimeMillis();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to update game state", e);
        }
    }
    
    /**
     * Provide learning feedback to AI components
     */
    private void provideLearningFeedback(GameAction action, boolean success) {
        try {
            float reward = success ? 1.0f : -0.5f;
            
            // Update strategy agent
            if (strategyAgent != null) {
                strategyAgent.updateStrategy(currentGameState, action, reward);
            }
            
            // Update object labeler
            if (objectLabeler != null && success) {
                objectLabeler.recordSuccessfulDetection(action.getObjectName());
            }
            
            // Broadcast learning feedback
            communicationProtocol.broadcastLearningFeedback("SystemIntegrator", reward, success);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to provide learning feedback", e);
        }
    }
    
    /**
     * Handle messages from communication protocol
     */
    @Override
    public void onMessageReceived(String messageType, android.os.Bundle data) {
        try {
            switch (messageType) {
                case ServiceCommunicationProtocol.MSG_SERVICE_STATUS:
                    handleServiceStatusChange(data);
                    break;
                case ServiceCommunicationProtocol.MSG_SCREEN_CAPTURE_READY:
                    handleScreenCaptureReady(data);
                    break;
                case ServiceCommunicationProtocol.MSG_OBJECT_DETECTED:
                    handleObjectDetected(data);
                    break;
                case ServiceCommunicationProtocol.MSG_ACTION_EXECUTED:
                    handleActionExecuted(data);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling message: " + messageType, e);
        }
    }
    
    private void handleServiceStatusChange(android.os.Bundle data) {
        String serviceName = data.getString("service_name");
        String stateString = data.getString("service_state");
        
        Log.d(TAG, "Service status change: " + serviceName + " -> " + stateString);
        
        // Restart pipeline if all services become ready
        if (ServiceCommunicationProtocol.ServiceState.RUNNING.name().equals(stateString)) {
            if (communicationProtocol.areAllServicesReady() && systemRunning.get()) {
                startDataPipeline();
            }
        }
    }
    
    private void handleScreenCaptureReady(android.os.Bundle data) {
        Log.d(TAG, "Screen capture ready");
        metrics.screenCapturesReady++;
    }
    
    private void handleObjectDetected(android.os.Bundle data) {
        int objectCount = data.getInt("object_count", 0);
        Log.d(TAG, "Objects detected: " + objectCount);
        metrics.objectDetections++;
    }
    
    private void handleActionExecuted(android.os.Bundle data) {
        String actionType = data.getString("action_type");
        Log.d(TAG, "Action executed: " + actionType);
        metrics.actionsExecuted++;
    }
    
    /**
     * Get current system metrics
     */
    public SystemMetrics getSystemMetrics() {
        return metrics;
    }
    
    /**
     * Get current game state
     */
    public UniversalGameState getCurrentGameState() {
        return currentGameState;
    }
    
    /**
     * Get data pipeline instance
     */
    public UnifiedDataPipeline getDataPipeline() {
        return dataPipeline;
    }
    
    /**
     * Check if system is running
     */
    public boolean isSystemRunning() {
        return systemRunning.get();
    }
    
    /**
     * Get system status summary
     */
    public String getSystemStatus() {
        StringBuilder status = new StringBuilder();
        status.append("System Integration Status:\n");
        status.append("Running: ").append(systemRunning.get()).append("\n");
        status.append("Data Pipeline: ").append(dataPipeline != null && dataPipeline.isRunning()).append("\n");
        status.append("Services Ready: ").append(communicationProtocol.areAllServicesReady()).append("\n");
        status.append("Metrics: ").append(metrics.toString()).append("\n");
        return status.toString();
    }
    
    /**
     * Cleanup system resources
     */
    public void cleanup() {
        try {
            stopSystem();
            
            if (executor != null) {
                executor.shutdown();
            }
            
            if (communicationProtocol != null) {
                communicationProtocol.removeMessageListener(this);
            }
            
            Log.i(TAG, "System cleaned up");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
    
    /**
     * System metrics class
     */
    public static class SystemMetrics {
        public long framesProcessed = 0;
        public long actionsExecuted = 0;
        public long objectDetections = 0;
        public long screenCapturesReady = 0;
        public long errors = 0;
        public long pipelineStartTime = 0;
        
        public long getUptime() {
            return pipelineStartTime > 0 ? System.currentTimeMillis() - pipelineStartTime : 0;
        }
        
        public float getFrameRate() {
            long uptime = getUptime();
            return uptime > 0 ? (framesProcessed * 1000f) / uptime : 0f;
        }
        
        @Override
        public String toString() {
            return String.format("Frames=%d, Actions=%d, Detections=%d, Errors=%d, FPS=%.1f", 
                    framesProcessed, actionsExecuted, objectDetections, errors, getFrameRate());
        }
    }
}
package com.gestureai.gameautomation;

import android.content.Context;
import android.util.Log;
import com.gestureai.gameautomation.services.ServiceCommunicationProtocol;
import com.gestureai.gameautomation.messaging.EventBus;
import com.gestureai.gameautomation.synchronization.AIComponentSynchronizer;
import com.gestureai.gameautomation.ai.GameStrategyAgent;

/**
 * Core automation engine that coordinates all AI and service components
 * This was referenced in GameAutomationManager but was missing implementation
 */
public class GameAutomationEngine {
    
    private static final String TAG = "GameAutomationEngine";
    private static volatile GameAutomationEngine instance;
    
    private Context context;
    private volatile boolean isInitialized = false;
    private boolean automationEnabled = false;
    private boolean autoGameDetectionEnabled = false;
    private int reactionSpeed = 50;
    
    private ServiceCommunicationProtocol serviceProtocol;
    private EventBus eventBus;
    private AIComponentSynchronizer synchronizer;
    private GameStrategyAgent strategyAgent;
    
    private GameAutomationEngine() {
        Log.d(TAG, "GameAutomationEngine created");
    }
    
    public static GameAutomationEngine getInstance() {
        if (instance == null) {
            synchronized (GameAutomationEngine.class) {
                if (instance == null) {
                    instance = new GameAutomationEngine();
                }
            }
        }
        return instance;
    }
    
    public static void initialize(Context context) {
        getInstance().initialize(context);
    }
    
    public synchronized void initialize(Context context) {
        if (!isInitialized && this.context == null) {
            this.context = context.getApplicationContext();
            this.serviceProtocol = ServiceCommunicationProtocol.getInstance(context);
            this.eventBus = EventBus.getInstance();
            this.synchronizer = AIComponentSynchronizer.getInstance();
            this.strategyAgent = GameStrategyAgent.getInstance(context);
            this.isInitialized = true;
            
            Log.d(TAG, "GameAutomationEngine initialized");
        }
    }
    
    public void setAutomationEnabled(boolean enabled) {
        this.automationEnabled = enabled;
        
        if (enabled) {
            // Start critical services when automation is enabled
            if (serviceProtocol != null) {
                serviceProtocol.startAllCriticalServices();
            }
        }
        
        // Post status change event
        if (eventBus != null) {
            eventBus.post(new EventBus.ServiceStatusChangedEvent(TAG, "GameAutomationEngine", enabled, 
                enabled ? "Automation enabled" : "Automation disabled"));
        }
        
        Log.d(TAG, "Automation " + (enabled ? "enabled" : "disabled"));
    }
    
    public boolean isAutomationEnabled() {
        return automationEnabled;
    }
    
    public void setAutoGameDetection(boolean enabled) {
        this.autoGameDetectionEnabled = enabled;
        Log.d(TAG, "Auto game detection " + (enabled ? "enabled" : "disabled"));
    }
    
    public void startGameTypeDetection() {
        if (autoGameDetectionEnabled && strategyAgent != null) {
            // Start game type detection using strategy agent
            strategyAgent.startGameTypeDetection();
            Log.d(TAG, "Game type detection started");
        }
    }
    
    public void setReactionSpeed(int speed) {
        this.reactionSpeed = Math.max(0, Math.min(100, speed));
        Log.d(TAG, "Reaction speed set to: " + this.reactionSpeed + "%");
    }
    
    public int getReactionSpeed() {
        return reactionSpeed;
    }
    
    public boolean isAutoGameDetectionEnabled() {
        return autoGameDetectionEnabled;
    }
    
    public void cleanup() {
        if (automationEnabled) {
            setAutomationEnabled(false);
        }
        
        if (serviceProtocol != null) {
            serviceProtocol.stopAllServices();
        }
        
        if (strategyAgent != null) {
            strategyAgent.cleanup();
        }
        
        Log.d(TAG, "GameAutomationEngine cleaned up");
    }
    
    /**
     * Capture current screen - referenced by ObjectLabelerEngine
     */
    public Bitmap captureScreen() {
        try {
            if (serviceProtocol != null) {
                return serviceProtocol.captureScreen();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to capture screen", e);
        }
        return null;
    }
    
    /**
     * Get DQN agent instance - referenced by training pipeline
     */
    public com.gestureai.gameautomation.ai.DQNAgent getDQNAgent() {
        if (strategyAgent != null) {
            return strategyAgent.getDQNAgent();
        }
        return null;
    }
    
    /**
     * Get PPO agent instance - referenced by training pipeline
     */
    public com.gestureai.gameautomation.ai.PPOAgent getPPOAgent() {
        if (strategyAgent != null) {
            return strategyAgent.getPPOAgent();
        }
        return null;
    }
    
    /**
     * Get current game state for AI processing
     */
    public com.gestureai.gameautomation.data.UniversalGameState getCurrentGameState() {
        if (strategyAgent != null) {
            return strategyAgent.getCurrentGameState();
        }
        return new com.gestureai.gameautomation.data.UniversalGameState();
    }
    
    /**
     * Execute a game action through the touch automation service
     */
    public boolean executeAction(com.gestureai.gameautomation.GameAction action) {
        try {
            if (serviceProtocol != null) {
                return serviceProtocol.executeGameAction(action);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute action", e);
        }
        return false;
    }
    
    /**
     * Get object detection engine instance
     */
    public com.gestureai.gameautomation.ai.ObjectDetectionEngine getObjectDetectionEngine() {
        if (serviceProtocol != null) {
            return serviceProtocol.getObjectDetectionEngine();
        }
        return null;
    }
    
    /**
     * Check if automation is currently running
     */
    public boolean isRunning() {
        return automationEnabled && serviceProtocol != null && serviceProtocol.areServicesRunning();
    }
    
    /**
     * Get automation statistics
     */
    public java.util.Map<String, Object> getAutomationStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("automationEnabled", automationEnabled);
        stats.put("reactionSpeed", reactionSpeed);
        stats.put("autoGameDetection", autoGameDetectionEnabled);
        stats.put("servicesRunning", serviceProtocol != null ? serviceProtocol.areServicesRunning() : false);
        return stats;
    }
}
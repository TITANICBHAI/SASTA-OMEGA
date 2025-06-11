package com.gestureai.gameautomation.utils;

import android.util.Log;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.ai.DQNAgent;
import com.gestureai.gameautomation.ai.PPOAgent;
import com.gestureai.gameautomation.ai.AdaptiveDecisionMaker;
import com.gestureai.gameautomation.ObjectDetectionEngine;
import com.gestureai.gameautomation.services.ScreenCaptureService;
import com.gestureai.gameautomation.data.UniversalGameState;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Synchronizes AI component operations to prevent race conditions and ensure consistent decision making
 */
public class AIComponentSynchronizer {
    private static final String TAG = "AIComponentSynchronizer";
    private static volatile AIComponentSynchronizer instance;
    private static final Object lock = new Object();
    
    // Synchronization primitives
    private final ReentrantReadWriteLock gameStateLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock decisionLock = new ReentrantReadWriteLock();
    private final Object visionProcessingLock = new Object();
    
    // Component availability flags
    private AtomicBoolean visionProcessingActive = new AtomicBoolean(false);
    private AtomicBoolean decisionMakingActive = new AtomicBoolean(false);
    private AtomicBoolean learningActive = new AtomicBoolean(false);
    
    // Shared state
    private volatile UniversalGameState currentGameState;
    private volatile String lastDecision;
    private volatile long lastDecisionTime;
    private volatile boolean systemReady = false;
    
    // Component references
    private GameStrategyAgent strategyAgent;
    private DQNAgent dqnAgent;
    private PPOAgent ppoAgent;
    private AdaptiveDecisionMaker decisionMaker;
    private ObjectDetectionEngine visionEngine;
    
    public interface SynchronizationCallback {
        void onComponentsReady();
        void onDecisionReady(String decision, float confidence);
        void onGameStateUpdated(UniversalGameState gameState);
        void onSynchronizationError(String error);
    }
    
    private SynchronizationCallback callback;
    
    public static AIComponentSynchronizer getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new AIComponentSynchronizer();
                }
            }
        }
        return instance;
    }
    
    private AIComponentSynchronizer() {
        Log.d(TAG, "AI Component Synchronizer initialized");
    }
    
    public void setSynchronizationCallback(SynchronizationCallback callback) {
        this.callback = callback;
    }
    
    public void registerComponents(GameStrategyAgent strategy, DQNAgent dqn, PPOAgent ppo, 
                                 AdaptiveDecisionMaker decision, ObjectDetectionEngine vision) {
        this.strategyAgent = strategy;
        this.dqnAgent = dqn;
        this.ppoAgent = ppo;
        this.decisionMaker = decision;
        this.visionEngine = vision;
        
        systemReady = true;
        Log.d(TAG, "All AI components registered and synchronized");
        
        if (callback != null) {
            callback.onComponentsReady();
        }
    }
    
    /**
     * Synchronized vision processing pipeline
     */
    public void processFrameWithSynchronization(android.graphics.Bitmap frame) {
        if (!systemReady || visionProcessingActive.get()) {
            return;
        }
        
        new Thread(() -> {
            synchronized (visionProcessingLock) {
                visionProcessingActive.set(true);
                
                try {
                    // Step 1: Object detection
                    java.util.List<com.gestureai.gameautomation.models.DetectedObject> objects = 
                        visionEngine.detectObjects(frame);
                    
                    // Step 2: Update game state (thread-safe)
                    gameStateLock.writeLock().lock();
                    try {
                        if (currentGameState == null) {
                            currentGameState = new UniversalGameState();
                        }
                        currentGameState.updateDetectedObjects(objects);
                        currentGameState.setTimestamp(System.currentTimeMillis());
                        
                        Log.d(TAG, "Game state updated with " + objects.size() + " detected objects");
                        
                        if (callback != null) {
                            callback.onGameStateUpdated(currentGameState);
                        }
                    } finally {
                        gameStateLock.writeLock().unlock();
                    }
                    
                    // Step 3: Trigger decision making
                    triggerDecisionMaking();
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error in synchronized frame processing", e);
                    if (callback != null) {
                        callback.onSynchronizationError("Frame processing failed: " + e.getMessage());
                    }
                } finally {
                    visionProcessingActive.set(false);
                }
            }
        }).start();
    }
    
    /**
     * Synchronized decision making process
     */
    private void triggerDecisionMaking() {
        if (decisionMakingActive.get()) {
            Log.d(TAG, "Decision making already in progress, skipping");
            return;
        }
        
        new Thread(() -> {
            decisionLock.writeLock().lock();
            decisionMakingActive.set(true);
            
            try {
                // Get current game state (read-only)
                UniversalGameState gameState;
                gameStateLock.readLock().lock();
                try {
                    gameState = currentGameState;
                } finally {
                    gameStateLock.readLock().unlock();
                }
                
                if (gameState == null) {
                    Log.w(TAG, "No game state available for decision making");
                    return;
                }
                
                // Step 1: Strategy agent analysis
                String strategyDecision = null;
                float strategyConfidence = 0.0f;
                
                if (strategyAgent != null && strategyAgent.isActive()) {
                    strategyDecision = strategyAgent.analyzeGameState(gameState);
                    strategyConfidence = strategyAgent.getLastDecisionConfidence();
                }
                
                // Step 2: DQN agent decision
                String dqnDecision = null;
                float dqnConfidence = 0.0f;
                
                if (dqnAgent != null) {
                    float[] stateVector = gameState.toStateVector();
                    int actionIndex = dqnAgent.selectAction(stateVector);
                    dqnDecision = mapActionToString(actionIndex);
                    dqnConfidence = dqnAgent.getCurrentPerformance();
                }
                
                // Step 3: PPO agent decision
                String ppoDecision = null;
                float ppoConfidence = 0.0f;
                
                if (ppoAgent != null) {
                    float[] stateVector = gameState.toStateVector();
                    int actionIndex = ppoAgent.selectAction(stateVector);
                    ppoDecision = mapActionToString(actionIndex);
                    ppoConfidence = ppoAgent.getCurrentPerformance();
                }
                
                // Step 4: Adaptive decision maker combines all inputs
                String finalDecision = "none";
                float finalConfidence = 0.0f;
                
                if (decisionMaker != null) {
                    finalDecision = decisionMaker.makeFinalDecision(
                        strategyDecision, strategyConfidence,
                        dqnDecision, dqnConfidence,
                        ppoDecision, ppoConfidence
                    );
                    finalConfidence = decisionMaker.getLastDecisionConfidence();
                }
                
                // Store decision with timestamp
                lastDecision = finalDecision;
                lastDecisionTime = System.currentTimeMillis();
                
                Log.d(TAG, "Synchronized decision made: " + finalDecision + " (confidence: " + finalConfidence + ")");
                
                if (callback != null) {
                    callback.onDecisionReady(finalDecision, finalConfidence);
                }
                
                // Step 5: Trigger learning if enabled
                if (learningActive.get()) {
                    triggerLearning(gameState, finalDecision, finalConfidence);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in synchronized decision making", e);
                if (callback != null) {
                    callback.onSynchronizationError("Decision making failed: " + e.getMessage());
                }
            } finally {
                decisionMakingActive.set(false);
                decisionLock.writeLock().unlock();
            }
        }).start();
    }
    
    /**
     * Synchronized learning process
     */
    private void triggerLearning(UniversalGameState gameState, String decision, float confidence) {
        new Thread(() -> {
            try {
                // Calculate reward based on game state change
                float reward = calculateReward(gameState, decision);
                
                // Train DQN agent
                if (dqnAgent != null) {
                    float[] stateVector = gameState.toStateVector();
                    int actionIndex = mapStringToAction(decision);
                    dqnAgent.trainFromCustomData(stateVector, actionIndex, reward);
                }
                
                // Train PPO agent
                if (ppoAgent != null) {
                    float[] stateVector = gameState.toStateVector();
                    int actionIndex = mapStringToAction(decision);
                    ppoAgent.trainFromCustomData(stateVector, actionIndex, reward);
                }
                
                // Update adaptive decision maker
                if (decisionMaker != null) {
                    decisionMaker.updatePerformance(decision, reward);
                }
                
                Log.d(TAG, "Synchronized learning completed with reward: " + reward);
                
            } catch (Exception e) {
                Log.e(TAG, "Error in synchronized learning", e);
            }
        }).start();
    }
    
    // Control methods
    public void enableLearning(boolean enabled) {
        learningActive.set(enabled);
        Log.d(TAG, "Learning " + (enabled ? "enabled" : "disabled"));
    }
    
    public boolean waitForDecision(long timeoutMs) {
        long startTime = System.currentTimeMillis();
        
        while (decisionMakingActive.get() && (System.currentTimeMillis() - startTime) < timeoutMs) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return !decisionMakingActive.get();
    }
    
    public String getLastDecision() {
        return lastDecision;
    }
    
    public long getLastDecisionTime() {
        return lastDecisionTime;
    }
    
    public UniversalGameState getCurrentGameState() {
        gameStateLock.readLock().lock();
        try {
            return currentGameState;
        } finally {
            gameStateLock.readLock().unlock();
        }
    }
    
    public boolean isSystemReady() {
        return systemReady;
    }
    
    public boolean isProcessingActive() {
        return visionProcessingActive.get() || decisionMakingActive.get();
    }
    
    // Utility methods
    private String mapActionToString(int actionIndex) {
        switch (actionIndex) {
            case 0: return "tap";
            case 1: return "swipe_left";
            case 2: return "swipe_right";
            case 3: return "swipe_up";
            case 4: return "swipe_down";
            case 5: return "long_press";
            case 6: return "pinch";
            case 7: return "wait";
            default: return "none";
        }
    }
    
    private int mapStringToAction(String action) {
        switch (action.toLowerCase()) {
            case "tap": return 0;
            case "swipe_left": return 1;
            case "swipe_right": return 2;
            case "swipe_up": return 3;
            case "swipe_down": return 4;
            case "long_press": return 5;
            case "pinch": return 6;
            case "wait": return 7;
            default: return 7; // wait as default
        }
    }
    
    private float calculateReward(UniversalGameState gameState, String decision) {
        // Simple reward calculation based on game state changes
        // This should be customized based on specific game metrics
        
        float baseReward = 0.1f; // Small positive reward for any action
        
        // Bonus for successful object detection
        if (gameState.getDetectedObjects().size() > 0) {
            baseReward += 0.2f;
        }
        
        // Penalty for long decision times
        long decisionTime = System.currentTimeMillis() - gameState.getTimestamp();
        if (decisionTime > 1000) { // More than 1 second
            baseReward -= 0.1f;
        }
        
        return Math.max(0.0f, Math.min(1.0f, baseReward));
    }
    
    public void shutdown() {
        systemReady = false;
        visionProcessingActive.set(false);
        decisionMakingActive.set(false);
        learningActive.set(false);
        
        Log.d(TAG, "AI Component Synchronizer shut down");
    }
}
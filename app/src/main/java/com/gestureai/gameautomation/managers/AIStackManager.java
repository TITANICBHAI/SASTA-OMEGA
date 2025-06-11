package com.gestureai.gameautomation.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.gestureai.gameautomation.ai.AdaptiveDecisionMaker;
import com.gestureai.gameautomation.ai.GameStatePredictor;
import com.gestureai.gameautomation.DecisionEngine;
import com.gestureai.gameautomation.TensorFlowLiteHelper;

/**
 * Manages AI stack configuration - switches between lightweight and advanced AI
 */
public class AIStackManager {
    private static final String TAG = "AIStackManager";
    private static final String PREFS_NAME = "ai_stack_preferences";
    private static final String KEY_ND4J_ENABLED = "nd4j_enabled";

    private static AIStackManager instance;
    private Context context;
    private SharedPreferences preferences;

    // AI Components
    private boolean isND4JEnabled = false;
    private boolean isInitialized = false;

    // Advanced AI (ND4J-based)
    private AdaptiveDecisionMaker adaptiveDecisionMaker;
    private GameStatePredictor gameStatePredictor;
    private DecisionEngine decisionEngine;

    // Lightweight AI (TensorFlow Lite only)
    private TensorFlowLiteHelper tensorFlowLiteHelper;
    private MLModelManager mlModelManager;

    // Performance tracking
    private long initializationTime = 0;
    private float memoryUsage = 0.0f;

    public interface AIStackCallback {
        void onStackEnabled(boolean success, String message);
        void onStackDisabled(boolean success, String message);
        void onPerformanceUpdate(float memoryUsage, long processingTime);
    }

    private AIStackCallback callback;

    public static AIStackManager getInstance(Context context) {
        if (instance == null) {
            instance = new AIStackManager(context);
        }
        return instance;
    }

    public static AIStackManager getInstance() {
        return instance;
    }

    private AIStackManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load saved preference
        this.isND4JEnabled = preferences.getBoolean(KEY_ND4J_ENABLED, false);

        // Initialize lightweight components always
        initializeLightweightAI();

        Log.d(TAG, "AIStackManager initialized - ND4J enabled: " + isND4JEnabled);
    }

    public void setCallback(AIStackCallback callback) {
        this.callback = callback;
    }

    /**
     * Toggle ND4J advanced AI stack on/off
     */
    public void toggleND4JStack(boolean enable) {
        if (enable == isND4JEnabled) {
            Log.d(TAG, "ND4J stack already " + (enable ? "enabled" : "disabled"));
            return;
        }

        long startTime = System.currentTimeMillis();

        if (enable) {
            enableAdvancedAI();
        } else {
            disableAdvancedAI();
        }

        // Save preference
        preferences.edit().putBoolean(KEY_ND4J_ENABLED, enable).apply();
        this.isND4JEnabled = enable;

        initializationTime = System.currentTimeMillis() - startTime;
        updatePerformanceMetrics();

        Log.d(TAG, "ND4J stack " + (enable ? "enabled" : "disabled") +
              " in " + initializationTime + "ms");
    }

    /**
     * Initialize lightweight AI components (always available)
     */
    private void initializeLightweightAI() {
        try {
            tensorFlowLiteHelper = new TensorFlowLiteHelper(context);
            mlModelManager = new MLModelManager(context);
            mlModelManager.initialize();

            Log.d(TAG, "Lightweight AI initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize lightweight AI", e);
        }
    }

    /**
     * Enable advanced ND4J-based AI components
     */
    private void enableAdvancedAI() {
        try {
            Log.d(TAG, "Initializing advanced ND4J AI stack...");

            // Initialize ND4J-based components
            adaptiveDecisionMaker = new AdaptiveDecisionMaker();
            gameStatePredictor = new GameStatePredictor();
            decisionEngine = new DecisionEngine(context);

            // Verify initialization
            if (adaptiveDecisionMaker.isInitialized() &&
                gameStatePredictor.isInitialized() &&
                decisionEngine.isInitialized()) {

                isInitialized = true;
                Log.d(TAG, "Advanced AI stack enabled successfully");

                if (callback != null) {
                    callback.onStackEnabled(true, "Advanced AI enabled");
                }
            } else {
                throw new RuntimeException("Failed to initialize ND4J components");
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to enable advanced AI", e);
            isInitialized = false;

            if (callback != null) {
                callback.onStackEnabled(false, "Failed: " + e.getMessage());
            }
        }
    }

    /**
     * Disable advanced ND4J components and cleanup resources
     */
    private void disableAdvancedAI() {
        try {
            Log.d(TAG, "Disabling advanced ND4J AI stack...");

            // Cleanup ND4J components
            if (adaptiveDecisionMaker != null) {
                adaptiveDecisionMaker.cleanup();
                adaptiveDecisionMaker = null;
            }

            if (gameStatePredictor != null) {
                gameStatePredictor.cleanup();
                gameStatePredictor = null;
            }

            if (decisionEngine != null) {
                decisionEngine.cleanup();
                decisionEngine = null;
            }

            // Force garbage collection to free ND4J memory
            System.gc();

            isInitialized = false;
            Log.d(TAG, "Advanced AI stack disabled successfully");

            if (callback != null) {
                callback.onStackDisabled(true, "Advanced AI disabled");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error disabling advanced AI", e);

            if (callback != null) {
                callback.onStackDisabled(false, "Error: " + e.getMessage());
            }
        }
    }

    /**
     * Get optimal AI decision using available stack
     */
    public GameDecision makeDecision(float[] gameState, String gameType) {
        if (isND4JEnabled && isInitialized && adaptiveDecisionMaker != null) {
            // Use advanced ND4J-based decision making
            return adaptiveDecisionMaker.makeAdvancedDecision(gameState, gameType);
        } else {
            // Use lightweight TensorFlow Lite decision making
            return makeLightweightDecision(gameState, gameType);
        }
    }

    /**
     * Predict future game state using available stack
     */
    public float[] predictGameState(float[] currentState, int stepsAhead) {
        if (isND4JEnabled && isInitialized && gameStatePredictor != null) {
            // Use advanced LSTM-based prediction
            return gameStatePredictor.predictFutureState(currentState, stepsAhead);
        } else {
            // Use lightweight linear prediction
            return makeLightweightPrediction(currentState, stepsAhead);
        }
    }

    /**
     * Lightweight decision making using TensorFlow Lite
     */
    private GameDecision makeLightweightDecision(float[] gameState, String gameType) {
        try {
            if (tensorFlowLiteHelper != null) {
                TensorFlowLiteHelper.DetectionResult result =
                    tensorFlowLiteHelper.runInference("decision_model", gameState);

                return new GameDecision(
                    result.className,
                    result.confidence,
                    "lightweight"
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in lightweight decision making", e);
        }

        // Fallback to simple rule-based decision
        return new GameDecision("tap", 0.5f, "fallback");
    }

    /**
     * Lightweight prediction using simple extrapolation
     */
    private float[] makeLightweightPrediction(float[] currentState, int stepsAhead) {
        // Simple linear extrapolation for lightweight prediction
        float[] prediction = new float[currentState.length];

        for (int i = 0; i < currentState.length; i++) {
            // Basic momentum-based prediction
            float momentum = i > 0 ? (currentState[i] - currentState[i-1]) : 0;
            prediction[i] = currentState[i] + (momentum * stepsAhead);
        }

        return prediction;
    }

    /**
     * Update performance metrics
     */
    private void updatePerformanceMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        memoryUsage = usedMemory / (1024.0f * 1024.0f); // MB

        if (callback != null) {
            callback.onPerformanceUpdate(memoryUsage, initializationTime);
        }
    }

    /**
     * Get current AI stack status
     */
    public AIStackStatus getStatus() {
        return new AIStackStatus(
            isND4JEnabled,
            isInitialized,
            memoryUsage,
            initializationTime,
            getAvailableFeatures()
        );
    }

    /**
     * Get list of available AI features
     */
    private String[] getAvailableFeatures() {
        if (isND4JEnabled && isInitialized) {
            return new String[]{
                "Advanced Decision Making",
                "LSTM State Prediction",
                "Complex Strategy Planning",
                "Multi-Agent Coordination",
                "Advanced Pattern Learning"
            };
        } else {
            return new String[]{
                "Basic Decision Making",
                "Simple State Prediction",
                "Rule-Based Strategy",
                "Single-Agent Control"
            };
        }
    }

    public boolean isND4JEnabled() {
        return isND4JEnabled;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public float getMemoryUsage() {
        updatePerformanceMetrics();
        return memoryUsage;
    }

    /**
     * Cleanup all resources
     */
    public void cleanup() {
        disableAdvancedAI();

        if (tensorFlowLiteHelper != null) {
            tensorFlowLiteHelper.cleanup();
        }

        if (mlModelManager != null) {
            mlModelManager.cleanup();
        }

        instance = null;
    }

    /**
     * Data classes for AI decisions and status
     */
    public static class GameDecision {
        public String action;
        public float confidence;
        public String source;

        public GameDecision(String action, float confidence, String source) {
            this.action = action;
            this.confidence = confidence;
            this.source = source;
        }
    }

    public static class AIStackStatus {
        public boolean nd4jEnabled;
        public boolean initialized;
        public float memoryUsage;
        public long initializationTime;
        public String[] availableFeatures;

        public AIStackStatus(boolean nd4jEnabled, boolean initialized,
                           float memoryUsage, long initializationTime,
                           String[] availableFeatures) {
            this.nd4jEnabled = nd4jEnabled;
            this.initialized = initialized;
            this.memoryUsage = memoryUsage;
            this.initializationTime = initializationTime;
            this.availableFeatures = availableFeatures;
        }
    }

}
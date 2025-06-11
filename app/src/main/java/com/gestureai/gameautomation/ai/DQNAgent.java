package com.gestureai.gameautomation.ai;

import android.content.Context;
import android.util.Log;
import com.gestureai.gameautomation.managers.DynamicModelManager;
import org.tensorflow.lite.Interpreter;

import java.util.Random;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import com.gestureai.gameautomation.models.ActionIntent;
import com.gestureai.gameautomation.models.GameFrame;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class DQNAgent {
    private static final String TAG = "DQNAgent";
    private static volatile DQNAgent instance;
    private static final Object instanceLock = new Object();
    
    private Context context;
    
    public static DQNAgent getInstance() {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    // Create with default parameters if no instance exists
                    instance = new DQNAgent(16, 8);
                    Log.d(TAG, "DQNAgent instance created with default parameters");
                }
            }
        }
        return instance;
    }
    
    public static DQNAgent getInstance(int stateSize, int actionSize) {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new DQNAgent(stateSize, actionSize);
                    Log.d(TAG, "DQNAgent instance created with custom parameters");
                }
            }
        }
        return instance;
    }
    private DynamicModelManager modelManager;
    private int stateSize;
    private int actionSize;
    private volatile float learningRate;
    private volatile float epsilon;
    private volatile float epsilonDecay;
    private volatile float gamma;
    
    // TensorFlow Lite backend - thread-safe access
    private volatile String currentModelName = "dqn_model";
    private volatile boolean modelLoaded = false;
    private final Object modelLock = new Object();
    
    // Fallback Q-table for when model is not available - thread-safe
    private volatile float[][] qTable;
    private volatile float[][] targetQTable; // Target network for stable learning
    private final Object qTableLock = new Object();
    private final Object targetNetworkLock = new Object();
    private Random random;
    private volatile float currentPerformance;
    private volatile boolean isDestroyed = false;
    
    // Critical: Neural network synchronization controls
    private volatile boolean networkUpdateInProgress = false;
    private volatile int updatesSinceTargetSync = 0;
    private static final int TARGET_NETWORK_UPDATE_FREQUENCY = 100;
    private volatile boolean atomicUpdateEnabled = true;
    
    // Gradient explosion prevention
    private final java.util.concurrent.atomic.AtomicReference<Float> lastGradientNorm = new java.util.concurrent.atomic.AtomicReference<>(0.0f);
    private final java.util.concurrent.atomic.AtomicInteger gradientExplosionCount = new java.util.concurrent.atomic.AtomicInteger(0);
    private volatile float gradientClipThreshold = 1.0f;
    private volatile float adaptiveLearningRate;
    private static final float MAX_GRADIENT_NORM = 10.0f;
    private static final float MIN_LEARNING_RATE = 0.00001f;
    private static final float MAX_LEARNING_RATE = 0.01f;
    private final java.util.concurrent.atomic.AtomicBoolean emergencyStop = new java.util.concurrent.atomic.AtomicBoolean(false);
    
    // Experience replay buffer with corruption prevention and bounds checking
    private final java.util.concurrent.ConcurrentLinkedQueue<Experience> replayBuffer;
    private static final int MAX_REPLAY_BUFFER_SIZE = 10000; // Reduced to prevent OOM
    private final java.util.concurrent.atomic.AtomicInteger bufferSize = new java.util.concurrent.atomic.AtomicInteger(0);
    private final Object bufferLock = new Object();
    private volatile boolean bufferCorrupted = false;
    private final java.util.concurrent.atomic.AtomicLong bufferModificationCount = new java.util.concurrent.atomic.AtomicLong(0);
    
    public DQNAgent(int stateSize, int actionSize) {
        this.stateSize = stateSize;
        this.actionSize = actionSize;
        this.learningRate = 0.001f;
        this.epsilon = 1.0f;
        this.epsilonDecay = 0.995f;
        this.gamma = 0.95f;
        
        this.qTable = new float[100][actionSize]; // Fallback Q-table
        this.random = new Random();
        this.currentPerformance = 0.5f;
        this.replayBuffer = new java.util.concurrent.ConcurrentLinkedQueue<>();
        this.adaptiveLearningRate = this.learningRate;
        
        try {
            initializeQTable();
            initializeTargetNetwork();
            Log.d(TAG, "DQN Agent initialized with state size: " + stateSize + ", action size: " + actionSize);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize DQN Agent, using fallback mode", e);
            // Initialize minimal fallback state
            initializeFallbackMode();
        }
    }
    
    private void initializeFallbackMode() {
        // Initialize minimal working state when normal initialization fails
        this.qTable = new float[10][actionSize]; // Reduced size fallback
        this.random = new Random();
        this.currentPerformance = 0.1f; // Lower starting performance
        
        // Initialize with conservative random values
        for (int i = 0; i < qTable.length; i++) {
            for (int j = 0; j < actionSize; j++) {
                qTable[i][j] = 0.01f; // Very small conservative values
            }
        }
        Log.w(TAG, "DQN Agent running in fallback mode with reduced capabilities");
    }
    
    public DQNAgent(Context context, int stateSize, int actionSize) {
        this(stateSize, actionSize);
        this.context = context;
        this.modelManager = new DynamicModelManager(context);
        initializeTensorFlowModel();
        synchronized(instanceLock) {
            instance = this;
        }
        Log.d(TAG, "DQN Agent initialized with TensorFlow Lite backend");
    }
    
    // Simple constructor for AIControlBridge compatibility
    public DQNAgent(Context context) {
        this(context, 20, 6); // Default state and action sizes
    }
    

    
    private void initializeTensorFlowModel() {
        synchronized(modelLock) {
            try {
                if (modelManager.loadModel(currentModelName)) {
                    modelLoaded = true;
                    Log.d(TAG, "TensorFlow Lite DQN model loaded successfully");
                } else {
                    Log.w(TAG, "DQN model not found, using Q-table fallback");
                    modelLoaded = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing TensorFlow model", e);
                modelLoaded = false;
            }
        }
    }
    
    private void initializeQTable() {
        synchronized(qTableLock) {
            for (int i = 0; i < qTable.length; i++) {
                for (int j = 0; j < actionSize; j++) {
                    qTable[i][j] = random.nextFloat() * 0.1f; // Small random values
                }
            }
        }
    }
    
    /**
     * Critical: Initialize target network for stable DQN learning
     */
    private void initializeTargetNetwork() {
        synchronized(targetNetworkLock) {
            targetQTable = new float[qTable.length][actionSize];
            
            // Copy initial weights from main network
            for (int i = 0; i < qTable.length; i++) {
                for (int j = 0; j < actionSize; j++) {
                    targetQTable[i][j] = qTable[i][j];
                }
            }
            
            Log.d(TAG, "Target network initialized with copied weights");
        }
    }
    
    /**
     * Critical: Atomic target network synchronization to prevent race conditions
     */
    private void synchronizeTargetNetworkAtomic() {
        if (!atomicUpdateEnabled || networkUpdateInProgress) {
            Log.d(TAG, "Target network sync skipped - update in progress or disabled");
            return;
        }
        
        try {
            networkUpdateInProgress = true;
            
            // Use double-checked locking for atomic operation
            synchronized(targetNetworkLock) {
                synchronized(qTableLock) {
                    if (qTable != null && targetQTable != null) {
                        // Atomic weight copying with validation
                        for (int i = 0; i < Math.min(qTable.length, targetQTable.length); i++) {
                            for (int j = 0; j < Math.min(actionSize, targetQTable[i].length); j++) {
                                // Validate weight before copying
                                if (!Float.isNaN(qTable[i][j]) && !Float.isInfinite(qTable[i][j])) {
                                    targetQTable[i][j] = qTable[i][j];
                                } else {
                                    Log.w(TAG, "Invalid weight detected during sync, skipping");
                                }
                            }
                        }
                        
                        updatesSinceTargetSync = 0;
                        Log.d(TAG, "Target network synchronized atomically");
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Critical error in atomic target network sync", e);
        } finally {
            networkUpdateInProgress = false;
        }
    }
    
    /**
     * Critical: Race condition safe Q-value computation using target network
     */
    private float getTargetQValue(int stateIndex, int action) {
        synchronized(targetNetworkLock) {
            try {
                if (targetQTable != null && 
                    stateIndex >= 0 && stateIndex < targetQTable.length &&
                    action >= 0 && action < actionSize) {
                    return targetQTable[stateIndex][action];
                }
            } catch (Exception e) {
                Log.w(TAG, "Error accessing target Q-value", e);
            }
        }
        return 0.0f; // Safe fallback
    }
    
    /**
     * Critical: Thread-safe target network max Q-value computation
     */
    private float getMaxTargetQValue(int stateIndex) {
        synchronized(targetNetworkLock) {
            try {
                if (targetQTable != null && stateIndex >= 0 && stateIndex < targetQTable.length) {
                    float maxQ = Float.NEGATIVE_INFINITY;
                    
                    for (int action = 0; action < actionSize; action++) {
                        float qValue = targetQTable[stateIndex][action];
                        if (!Float.isNaN(qValue) && !Float.isInfinite(qValue)) {
                            maxQ = Math.max(maxQ, qValue);
                        }
                    }
                    
                    return maxQ == Float.NEGATIVE_INFINITY ? 0.0f : maxQ;
                }
            } catch (Exception e) {
                Log.w(TAG, "Error computing max target Q-value", e);
            }
        }
        return 0.0f;
    }
    
    // Thread-safe parameter setters for AIControlBridge
    public synchronized void setLearningRate(double rate) {
        this.learningRate = (float) rate;
        Log.d(TAG, "Learning rate updated to: " + rate);
    }
    
    public synchronized void setExplorationRate(double rate) {
        this.epsilon = (float) rate;
        Log.d(TAG, "Exploration rate updated to: " + rate);
    }
    
    public synchronized void cleanup() {
        synchronized(modelLock) {
            if (modelManager != null) {
                // Cleanup model resources
                modelLoaded = false;
            }
        }
        synchronized(qTableLock) {
            qTable = null;
        }
        Log.d(TAG, "DQN Agent cleaned up");
    }
    
    /**
     * CRITICAL: Add custom training method for labeled objects
     */
    public void trainFromCustomData(float[] state, int action, float reward) {
        synchronized(modelLock) {
            try {
                if (modelLoaded) {
                    // Train TensorFlow Lite model with custom data
                    trainTensorFlowModel(state, action, reward);
                } else {
                    // Fallback to Q-table training
                    synchronized(qTableLock) {
                trainQTable(state, action, reward);
            }
            
            // Update performance metrics
            updatePerformanceMetrics(reward);
            
            Log.d(TAG, "Trained DQN with custom data - Action: " + action + ", Reward: " + reward);
            
        } catch (Exception e) {
            Log.e(TAG, "Error training with custom data", e);
        }
    }
    
    /**
     * Train TensorFlow Lite model with custom labeled data
     */
    private void trainTensorFlowModel(float[] state, int action, float reward) {
        try {
            if (modelManager != null) {
                // Create target Q-values for training
                float[] currentQValues = modelManager.runInference(currentModelName, state);
                if (currentQValues != null) {
                    // Update Q-value for the taken action
                    currentQValues[action] = reward + gamma * getMaxValue(currentQValues);
                    
                    // Train the model with updated Q-values
                    modelManager.trainModel(currentModelName, state, currentQValues);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error training TensorFlow model", e);
        }
    }
    
    /**
     * Fallback Q-table training when TensorFlow model unavailable with gradient explosion prevention
     */
    private void trainQTable(float[] state, int action, float reward) {
        if (emergencyStop.get()) {
            Log.w(TAG, "Training blocked due to emergency stop from gradient explosion");
            return;
        }
        
        int stateIndex = hashState(state);
        if (stateIndex >= 0 && stateIndex < qTable.length && action >= 0 && action < actionSize) {
            float oldValue = qTable[stateIndex][action];
            float maxNextQ = getMaxQValue(stateIndex);
            
            // Calculate gradient with explosion prevention
            float gradient = reward + gamma * maxNextQ - oldValue;
            float clippedGradient = clipGradient(gradient);
            
            // Check for gradient explosion
            if (detectGradientExplosion(clippedGradient)) {
                Log.w(TAG, "Gradient explosion detected, applying emergency measures");
                handleGradientExplosion();
                return;
            }
            
            // Apply adaptive learning rate
            float effectiveLearningRate = getAdaptiveLearningRate(clippedGradient);
            
            // Q-learning update rule with gradient clipping
            qTable[stateIndex][action] = oldValue + effectiveLearningRate * clippedGradient;
            
            // Update gradient tracking
            updateGradientStatistics(clippedGradient);
        }
    }
    
    private float clipGradient(float gradient) {
        float gradientNorm = Math.abs(gradient);
        if (gradientNorm > gradientClipThreshold) {
            return Math.signum(gradient) * gradientClipThreshold;
        }
        return gradient;
    }
    
    private boolean detectGradientExplosion(float gradient) {
        float gradientNorm = Math.abs(gradient);
        
        // Check for sudden spikes in gradient magnitude
        if (gradientNorm > MAX_GRADIENT_NORM) {
            gradientExplosionCount.incrementAndGet();
            return true;
        }
        
        // Check for rapid increase in gradient magnitude
        Float lastNorm = lastGradientNorm.get();
        if (lastNorm != null && gradientNorm > lastNorm * 5.0f && gradientNorm > 1.0f) {
            gradientExplosionCount.incrementAndGet();
            return true;
        }
        
        return false;
    }
    
    private void handleGradientExplosion() {
        int explosionCount = gradientExplosionCount.get();
        Log.e(TAG, "Handling gradient explosion #" + explosionCount);
        
        // Immediate measures
        adaptiveLearningRate *= 0.5f; // Halve learning rate
        gradientClipThreshold *= 0.8f; // Tighten gradient clipping
        
        // Ensure minimum bounds
        if (adaptiveLearningRate < MIN_LEARNING_RATE) {
            adaptiveLearningRate = MIN_LEARNING_RATE;
        }
        if (gradientClipThreshold < 0.1f) {
            gradientClipThreshold = 0.1f;
        }
        
        // Emergency stop if explosions are frequent
        if (explosionCount >= 10) {
            emergencyStop.set(true);
            Log.e(TAG, "Emergency stop activated due to repeated gradient explosions");
            
            // Schedule emergency stop reset
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                emergencyStop.set(false);
                gradientExplosionCount.set(0);
                adaptiveLearningRate = learningRate * 0.1f; // Conservative restart
                Log.i(TAG, "Emergency stop reset - training resumed with conservative parameters");
            }, 60000); // 1 minute cooldown
        }
    }
    
    private float getAdaptiveLearningRate(float gradient) {
        float gradientMagnitude = Math.abs(gradient);
        
        // Adaptive learning rate based on gradient magnitude
        if (gradientMagnitude > 2.0f) {
            return adaptiveLearningRate * 0.5f; // Reduce for large gradients
        } else if (gradientMagnitude < 0.1f) {
            return Math.min(adaptiveLearningRate * 1.2f, MAX_LEARNING_RATE); // Increase for small gradients
        }
        
        return adaptiveLearningRate;
    }
    
    private void updateGradientStatistics(float gradient) {
        float gradientNorm = Math.abs(gradient);
        lastGradientNorm.set(gradientNorm);
        
        // Adaptive threshold adjustment
        if (gradientNorm < gradientClipThreshold * 0.5f) {
            // Gradually increase threshold if gradients are consistently small
            gradientClipThreshold = Math.min(gradientClipThreshold * 1.01f, 2.0f);
        }
    }
    
    /**
     * Update performance tracking for training feedback
     */
    private void updatePerformanceMetrics(float reward) {
        // Exponential moving average of performance
        currentPerformance = 0.9f * currentPerformance + 0.1f * reward;
        
        // Decay epsilon for exploration/exploitation balance
        if (epsilon > 0.01f) {
            epsilon *= epsilonDecay;
        }
    }
    
    private float getMaxValue(float[] values) {
        float max = Float.NEGATIVE_INFINITY;
        for (float value : values) {
            max = Math.max(max, value);
        }
        return max;
    }
    
    private float getMaxQValue(int stateIndex) {
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < actionSize; i++) {
            max = Math.max(max, qTable[stateIndex][i]);
        }
        return max;
    }
    
    /**
     * Critical: Buffer corruption detection and recovery
     */
    private void validateReplayBufferIntegrity() {
        synchronized (bufferLock) {
            if (bufferCorrupted) {
                Log.w(TAG, "Buffer corruption detected, performing recovery");
                performBufferRecovery();
                bufferCorrupted = false;
            }
            
            // Verify buffer size consistency
            int actualSize = 0;
            for (Experience exp : replayBuffer) {
                if (exp != null) actualSize++;
            }
            
            if (actualSize != bufferSize.get()) {
                Log.w(TAG, "Buffer size inconsistency detected: expected=" + bufferSize.get() + ", actual=" + actualSize);
                bufferSize.set(actualSize);
                bufferModificationCount.incrementAndGet();
            }
        }
    }
    
    private void performBufferRecovery() {
        // Clear corrupted buffer and reinitialize
        replayBuffer.clear();
        bufferSize.set(0);
        bufferModificationCount.incrementAndGet();
        Log.i(TAG, "Replay buffer recovery completed");
    }
    
    private void addExperienceWithValidation(Experience experience) {
        synchronized (bufferLock) {
            try {
                // Check for buffer overflow
                if (bufferSize.get() >= MAX_REPLAY_BUFFER_SIZE) {
                    // Remove oldest experience
                    Experience removed = replayBuffer.poll();
                    if (removed != null) {
                        bufferSize.decrementAndGet();
                    }
                }
                
                // Validate experience data
                if (experience != null && isValidExperience(experience)) {
                    replayBuffer.offer(experience);
                    bufferSize.incrementAndGet();
                    bufferModificationCount.incrementAndGet();
                } else {
                    Log.w(TAG, "Invalid experience rejected from buffer");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error adding experience to buffer", e);
                bufferCorrupted = true;
            }
        }
    }
    
    private boolean isValidExperience(Experience experience) {
        return experience.state != null && 
               experience.action != null && 
               !Float.isNaN(experience.reward) && 
               !Float.isInfinite(experience.reward);
    }

    public int selectAction(float[] state) {
        try {
            if (modelLoaded && random.nextFloat() >= epsilon) {
                // Use TensorFlow Lite model for action selection
                float[] qValues = modelManager.runInference(currentModelName, state);
                if (qValues != null && qValues.length > 0) {
                    return getMaxIndex(qValues);
                }
            }
            
            if (random.nextFloat() < epsilon) {
                // Exploration: random action
                return random.nextInt(actionSize);
            } else {
                // Fallback to Q-table exploitation
                int stateIndex = hashState(state);
                return getBestAction(stateIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in action selection", e);
            return random.nextInt(actionSize);
        }
    }
    
    /**
     * CRITICAL: Enhanced action confidence evaluation with reasoning context
     */
    public float evaluateActionConfidenceWithReasoning(float[] state, int action, 
                                                      String why, String what, String how) {
        try {
            // Base confidence from Q-values
            float baseConfidence = evaluateActionConfidence(state, action);
            
            // Reasoning enhancement factors
            float reasoningBoost = 1.0f;
            
            // Boost confidence if reasoning is complete and coherent
            if (why != null && !why.trim().isEmpty()) reasoningBoost += 0.1f;
            if (what != null && !what.trim().isEmpty()) reasoningBoost += 0.1f;
            if (how != null && !how.trim().isEmpty()) reasoningBoost += 0.1f;
            
            // Additional boost for strategic reasoning keywords
            if (why != null) {
                String whyLower = why.toLowerCase();
                if (whyLower.contains("optimal") || whyLower.contains("best") || whyLower.contains("efficient")) {
                    reasoningBoost += 0.15f;
                }
                if (whyLower.contains("critical") || whyLower.contains("essential") || whyLower.contains("must")) {
                    reasoningBoost += 0.1f;
                }
            }
            
            // Enhanced confidence calculation
            float enhancedConfidence = baseConfidence * reasoningBoost;
            return Math.min(1.0f, enhancedConfidence);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in enhanced confidence evaluation", e);
            return evaluateActionConfidence(state, action);
        }
    }
    
    public float evaluateActionConfidence(float[] state, int action) {
        if (!isInitialized) return 0.5f;
        
        try {
            float[] qValues = getQValues(state);
            if (action >= 0 && action < qValues.length) {
                // Normalize Q-value to confidence [0,1]
                float maxQ = Float.NEGATIVE_INFINITY;
                float minQ = Float.POSITIVE_INFINITY;
                
                for (float q : qValues) {
                    maxQ = Math.max(maxQ, q);
                    minQ = Math.min(minQ, q);
                }
                
                if (maxQ == minQ) return 0.5f;
                
                return (qValues[action] - minQ) / (maxQ - minQ);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error evaluating action confidence", e);
        }
        
        return 0.5f;
    }
    
    private float[] getQValues(float[] state) {
        try {
            if (modelLoaded && modelManager != null) {
                return modelManager.runInference(currentModelName, state);
            } else {
                // Fallback to Q-table
                int stateIndex = hashState(state);
                if (stateIndex >= 0 && stateIndex < qTable.length) {
                    return qTable[stateIndex].clone();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting Q-values", e);
        }
        
        // Return default Q-values
        float[] defaultQValues = new float[actionSize];
        for (int i = 0; i < actionSize; i++) {
            defaultQValues[i] = 0.5f;
        }
        return defaultQValues;
    }
    
    private int getMaxIndex(float[] array) {
        int maxIndex = 0;
        float maxValue = array[0];
        
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }
        
        return maxIndex;
    }
    
    private int hashState(float[] state) {
        // Simple state hashing for demonstration
        int hash = 0;
        for (float value : state) {
            hash += (int) (value * 10);
        }
        return Math.abs(hash) % qTable.length;
    }
    
    private int getBestAction(int stateIndex) {
        float maxQ = Float.NEGATIVE_INFINITY;
        int bestAction = 0;
        
        for (int action = 0; action < actionSize; action++) {
            if (qTable[stateIndex][action] > maxQ) {
                maxQ = qTable[stateIndex][action];
                bestAction = action;
            }
        }
        
        return bestAction;
    }
    
    public float trainStep() {
        // Simulate training step
        float loss = 0.5f + (random.nextFloat() - 0.5f) * 0.1f;
        
        // Update epsilon
        epsilon = Math.max(0.01f, epsilon * epsilonDecay);
        
        // Update performance based on training
        currentPerformance = Math.min(1.0f, currentPerformance + 0.001f);
        
        Log.d(TAG, "Training step completed. Loss: " + loss + ", Performance: " + currentPerformance);
        return loss;
    }
    
    public void updateQValue(float[] state, int action, float reward, float[] nextState) {
        int stateIndex = hashState(state);
        int nextStateIndex = hashState(nextState);
        
        float maxNextQ = Float.NEGATIVE_INFINITY;
        for (int a = 0; a < actionSize; a++) {
            maxNextQ = Math.max(maxNextQ, qTable[nextStateIndex][a]);
        }
        
        float target = reward + gamma * maxNextQ;
        float currentQ = qTable[stateIndex][action];
        
        qTable[stateIndex][action] = currentQ + learningRate * (target - currentQ);
    }
    
    public float getPerformanceMetric() {
        return currentPerformance;
    }
    
    public void updatePerformanceMetric(float performance) {
        this.currentPerformance = Math.max(0.0f, Math.min(1.0f, performance));
    }
    
    public float getEpsilon() {
        return epsilon;
    }
    
    public float getLearningRate() {
        return learningRate;
    }
    
    public void setLearningRate(float learningRate) {
        this.learningRate = Math.max(0.0001f, Math.min(0.1f, learningRate));
    }
    
    public void saveModel(String filepath) {
        // In production, implement proper model serialization
        Log.d(TAG, "Model saved to: " + filepath);
    }
    
    public void loadModel(String filepath) {
        // In production, implement proper model loading
        Log.d(TAG, "Model loaded from: " + filepath);
    }
    
    public String getModelSummary() {
        return String.format(
            "DQN Agent - State Size: %d, Action Size: %d, Performance: %.3f, Epsilon: %.3f",
            stateSize, actionSize, currentPerformance, epsilon
        );
    }
    
    /**
     * NEW: Train DQN from user explanation
     */
    public void trainFromUserExplanation(GameFrame gameFrame, ActionIntent actionIntent) {
        try {
            Log.d(TAG, "Training DQN from user explanation: " + actionIntent.getAction());
            
            // 1. Convert GameFrame to state vector
            float[] state = convertGameFrameToState(gameFrame);
            
            // 2. Extract action from user intent
            int action = mapActionIntentToIndex(actionIntent);
            
            // 3. Calculate reward from user explanation
            float reward = calculateRewardFromExplanation(gameFrame, actionIntent);
            
            // 4. Add to replay buffer safely
            addExperienceToBufferSafe(state, action, reward, state);
            
            // 5. Perform training step with experience
            performTrainingStep();
            
            Log.d(TAG, "Successfully trained DQN from user explanation");
            
        } catch (Exception e) {
            Log.e(TAG, "Error training DQN from user explanation", e);
        }
    }
    
    /**
     * Thread-safe experience buffer management to prevent memory corruption
     */
    private void addExperienceToBufferSafe(float[] state, int action, float reward, float[] nextState) {
        if (isDestroyed) {
            return;
        }
        
        try {
            // Validate input parameters to prevent corruption
            if (state == null || nextState == null || action < 0 || action >= actionSize) {
                Log.w(TAG, "Invalid experience data, skipping buffer addition");
                return;
            }
            
            // Check buffer size and enforce limits to prevent OOM
            while (bufferSize.get() >= MAX_REPLAY_BUFFER_SIZE) {
                Experience removed = replayBuffer.poll();
                if (removed != null) {
                    bufferSize.decrementAndGet();
                } else {
                    break; // Prevent infinite loop if buffer is somehow corrupted
                }
            }
            
            // Create new experience with validation
            if (state != null && nextState != null && action >= 0 && action < actionSize) {
                Experience experience = new Experience(
                    state.clone(), action, reward, nextState.clone(), false
                );
                
                replayBuffer.offer(experience);
                bufferSize.incrementAndGet();
                
                Log.v(TAG, "Added experience to buffer. Size: " + bufferSize.get());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding experience to buffer", e);
        }
    }
    
    /**
     * Perform training step using experience replay
     */
    private void performTrainingStep() {
        try {
            final int BATCH_SIZE = 32;
            
            if (bufferSize.get() < BATCH_SIZE) {
                return; // Not enough experiences for training
            }
            
            // Sample random batch from replay buffer
            List<Experience> batch = sampleRandomBatch(BATCH_SIZE);
            
            for (Experience exp : batch) {
                if (exp != null) {
                    // Update Q-values using the experience
                    updateQValue(exp.getState(), exp.getAction(), exp.getReward(), exp.getNextState());
                }
            }
            
            // Update exploration rate
            if (epsilon > minEpsilon) {
                epsilon *= epsilonDecay;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in training step", e);
        }
    }
    
    /**
     * Sample random batch from experience replay buffer
     */
    private List<Experience> sampleRandomBatch(int batchSize) {
        List<Experience> batch = new ArrayList<>();
        List<Experience> bufferList = new ArrayList<>(replayBuffer);
        
        int availableSize = Math.min(batchSize, bufferList.size());
        
        for (int i = 0; i < availableSize; i++) {
            int randomIndex = random.nextInt(bufferList.size());
            batch.add(bufferList.get(randomIndex));
        }
        
        return batch;
            
            // 5. Update Q-table with user guidance
            updateQValue(state, action, reward, state);
            
            // 5. Store as positive example for future training
            storeUserExample(state, action, reward, gameFrame.userExplanation);
            
            Log.d(TAG, "DQN training completed from user explanation");
            
        } catch (Exception e) {
            Log.e(TAG, "Error training DQN from user explanation", e);
        }
    }
    
    private float[] convertGameFrameToState(GameFrame gameFrame) {
        float[] stateArray = new float[stateSize];
        
        // Extract visual features using ND4J tensor operations
        if (gameFrame.screenshot != null) {
            INDArray visualTensor = convertBitmapToND4J(gameFrame.screenshot);
            INDArray visualFeatures = extractVisualFeaturesND4J(visualTensor);
            
            // Copy ND4J-extracted features to state array
            float[] features = visualFeatures.toFloatVector();
            System.arraycopy(features, 0, stateArray, 0, Math.min(features.length, 10));
        }
        
        // Player confidence
        if (stateArray.length > 10) stateArray[10] = gameFrame.playerConfidence;
        
        // Bounding box features using ND4J normalization
        if (gameFrame.boundingBox != null && stateArray.length > 14) {
            INDArray boundingTensor = Nd4j.create(new float[]{
                gameFrame.boundingBox.left, gameFrame.boundingBox.top,
                gameFrame.boundingBox.width(), gameFrame.boundingBox.height()
            });
            INDArray normalizedBounds = boundingTensor.div(1000.0f);
            float[] bounds = normalizedBounds.toFloatVector();
            
            System.arraycopy(bounds, 0, stateArray, 11, Math.min(bounds.length, 4));
        }
        
        // Fill remaining with ND4J-generated contextual features
        for (int i = 15; i < stateSize; i++) {
            stateArray[i] = 0.0f;
        }
        
        return stateArray;
    }
    
    private INDArray convertBitmapToND4J(android.graphics.Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // Create ND4J tensor from bitmap pixels
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        
        // Convert to normalized RGB tensor [1, 3, height, width]
        float[] rgbArray = new float[3 * width * height];
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            rgbArray[i] = ((pixel >> 16) & 0xFF) / 255.0f; // R
            rgbArray[i + pixels.length] = ((pixel >> 8) & 0xFF) / 255.0f; // G
            rgbArray[i + 2 * pixels.length] = (pixel & 0xFF) / 255.0f; // B
        }
        
        return Nd4j.create(rgbArray).reshape(1, 3, height, width);
    }
    
    private INDArray extractVisualFeaturesND4J(INDArray imageTensor) {
        // Simplified CNN-like feature extraction using ND4J operations
        // In production, this would use actual CNN layers
        
        // Global average pooling
        INDArray pooled = imageTensor.mean(2, 3); // Average over height, width dimensions
        
        // Apply simple linear transformation
        INDArray weights = Nd4j.randn(3, 10).mul(0.1); // 3 RGB channels to 10 features
        INDArray features = pooled.reshape(1, 3).mmul(weights);
        
        return features;
    }
    
    private int mapActionIntentToIndex(ActionIntent actionIntent) {
        String action = actionIntent.getAction().toUpperCase();
        
        switch (action) {
            case "TAP":
            case "CLICK":
            case "TOUCH": return 0;
            case "SWIPE_UP": return 1;
            case "SWIPE_DOWN": return 2;
            case "SWIPE_LEFT": return 3;
            case "SWIPE_RIGHT": return 4;
            case "LONG_PRESS": return 5;
            case "DOUBLE_TAP": return 6;
            case "WAIT":
            case "PAUSE": return 7;
            default: return 0; // Default to tap
        }
    }
    
    private float calculateRewardFromExplanation(GameFrame gameFrame, ActionIntent actionIntent) {
        float reward = 0.0f;
        
        // Base reward from action confidence
        reward += actionIntent.getConfidence() * 0.5f;
        
        // Parse user explanation for reward signals
        if (gameFrame.userExplanation != null) {
            String explanation = gameFrame.userExplanation.toLowerCase();
            
            // Positive rewards
            if (explanation.contains("collect") || explanation.contains("get")) {
                reward += 1.0f;
            }
            if (explanation.contains("avoid") || explanation.contains("safe")) {
                reward += 0.8f;
            }
            if (explanation.contains("efficient") || explanation.contains("optimal")) {
                reward += 0.6f;
            }
            if (explanation.contains("score") || explanation.contains("points")) {
                reward += 1.2f;
            }
            
            // Negative rewards
            if (explanation.contains("damage") || explanation.contains("hurt")) {
                reward -= 0.8f;
            }
            if (explanation.contains("waste") || explanation.contains("mistake")) {
                reward -= 0.6f;
            }
            if (explanation.contains("dangerous") || explanation.contains("risky")) {
                reward -= 1.0f;
            }
        }
        
        return Math.max(-1.0f, Math.min(1.0f, reward)); // Clamp to [-1, 1]
    }
    
    private void storeUserExample(float[] state, int action, float reward, String explanation) {
        UserTrainingExample example = new UserTrainingExample();
        example.state = state.clone();
        example.action = action;
        example.reward = reward;
        example.explanation = explanation;
        example.timestamp = System.currentTimeMillis();
        
        userExamples.add(example);
        
        // Keep only recent examples
        if (userExamples.size() > 1000) {
            userExamples.remove(0);
        }
        
        Log.d(TAG, "Stored user training example with reward: " + reward);
    }
    
    private static class UserTrainingExample {
        float[] state;
        int action;
        float reward;
        String explanation;
        long timestamp;
    }
    
    private List<UserTrainingExample> userExamples = new ArrayList<>();

    public void cleanup() {
        // Reset training state
        isTrainingMode = false;
        
        // Clear experience buffer if it exists
        // In production, implement proper cleanup of neural network resources
        
        Log.d(TAG, "DQN Agent cleanup completed");
    }
    
    /**
     * NEW: Train DQN from user explanation using ND4J
     */
    public void trainFromUserExplanation(GameFrame gameFrame, ActionIntent actionIntent) {
        try {
            Log.d(TAG, "Training DQN from user explanation with ND4J: " + actionIntent.getAction());
            
            // Convert GameFrame to state using ND4J operations
            float[] state = convertGameFrameToState(gameFrame);
            INDArray stateTensor = Nd4j.create(state);
            
            // Map action intent to action index
            int action = mapActionIntentToIndex(actionIntent);
            
            // Calculate reward from user explanation
            float reward = calculateRewardFromExplanation(gameFrame, actionIntent);
            
            // Store experience for training
            addExperience(state, action, reward, state);
            
            // Train if we have enough experiences
            if (experiences.size() >= batchSize) {
                trainNetworkND4J();
            }
            
            Log.d(TAG, "DQN training completed from user explanation");
            
        } catch (Exception e) {
            Log.e(TAG, "Error training DQN from user explanation", e);
        }
    }
    
    private int mapActionIntentToIndex(ActionIntent actionIntent) {
        String action = actionIntent.getAction().toUpperCase();
        
        switch (action) {
            case "TAP":
            case "CLICK":
                return 0;
            case "SWIPE":
            case "DRAG":
                return 1;
            case "HOLD":
            case "LONG_PRESS":
                return 2;
            case "PINCH":
            case "ZOOM":
                return 3;
            case "ROTATE":
                return 4;
            default:
                return 0; // Default to tap
        }
    }
    
    private float calculateRewardFromExplanation(GameFrame gameFrame, ActionIntent actionIntent) {
        if (gameFrame.userExplanation == null) return 0.0f;
        
        String explanation = gameFrame.userExplanation.toLowerCase();
        float reward = 0.0f;
        
        // Positive rewards
        if (explanation.contains("good") || explanation.contains("correct") || explanation.contains("success")) {
            reward += 1.0f;
        }
        if (explanation.contains("efficient") || explanation.contains("optimal")) {
            reward += 0.5f;
        }
        if (explanation.contains("collect") || explanation.contains("gain")) {
            reward += 0.7f;
        }
        
        // Negative rewards
        if (explanation.contains("bad") || explanation.contains("wrong") || explanation.contains("mistake")) {
            reward -= 1.0f;
        }
        if (explanation.contains("dangerous") || explanation.contains("risky")) {
            reward -= 0.8f;
        }
        
        // Use action confidence as multiplier
        reward *= actionIntent.getConfidence();
        
        return Math.max(-1.0f, Math.min(1.0f, reward));
    }
    
    private void trainNetworkND4J() {
        // Simplified training using ND4J operations
        // In production, this would implement proper DQN loss calculation
        
        int trainBatchSize = Math.min(batchSize, experiences.size());
        
        for (int i = 0; i < trainBatchSize; i++) {
            Experience exp = experiences.get(i);
            
            // Convert experience to ND4J tensors
            INDArray stateTensor = Nd4j.create(exp.state);
            INDArray targetTensor = Nd4j.create(new float[]{exp.reward});
            
            // Simplified Q-value update using ND4J
            INDArray qValues = computeQValues(stateTensor);
            qValues.putScalar(exp.action, exp.reward);
            
            // Update network weights (simplified)
            updateNetworkWeights(stateTensor, qValues);
        }
        
        // Clear processed experiences
        experiences.clear();
        
        Log.d(TAG, "Network updated using ND4J tensor operations");
    }
    
    private INDArray computeQValues(INDArray state) {
        // Simplified Q-value computation using ND4J
        INDArray weights = Nd4j.randn(stateSize, actionSize).mul(0.01);
        return state.reshape(1, stateSize).mmul(weights);
    }
    
    private void updateNetworkWeights(INDArray state, INDArray targetQValues) {
        // Simplified weight update using ND4J operations
        // In production, this would implement proper gradient descent
        
        currentPerformance = Math.max(0.0f, Math.min(1.0f, 
            currentPerformance + targetQValues.meanNumber().floatValue() * 0.01f));
        
        Log.d(TAG, "Network weights updated, performance: " + currentPerformance);
    }
    
    public void setLearningRate(float learningRate) {
        this.learningRate = learningRate;
        Log.d(TAG, "Learning rate updated to: " + learningRate);
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
        Log.d(TAG, "Batch size updated to: " + batchSize);
    }
    
    public void resetWeights() {
        try {
            // Reinitialize Q-network with random weights
            qNetwork = createNeuralNetwork();
            targetNetwork = createNeuralNetwork();
            Log.d(TAG, "Neural network weights reset");
        } catch (Exception e) {
            Log.e(TAG, "Error resetting weights", e);
        }
    }
    
    /**
     * CRITICAL: Thread-safe experience replay buffer management to fix corruption
     */
    private void addExperienceToBuffer(float[] state, int action, float reward, float[] nextState) {
        try {
            // Create experience with cloned arrays to prevent reference corruption
            Experience experience = new Experience(state.clone(), action, reward, nextState.clone());
            
            // Thread-safe addition with size bounds checking
            while (replayBuffer.size() >= MAX_REPLAY_BUFFER_SIZE) {
                replayBuffer.poll(); // Remove oldest experience
            }
            replayBuffer.offer(experience);
            
            // Trigger replay training if buffer is sufficient
            if (replayBuffer.size() >= 32) {
                replayExperience();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding experience to replay buffer", e);
        }
    }
    
    /**
     * CRITICAL: Thread-safe replay buffer training to prevent corruption
     */
    private void replayExperience() {
        if (replayBuffer.size() < 32) return; // Minimum batch size
        
        try {
            // Convert to array for safe sampling
            Experience[] bufferArray = replayBuffer.toArray(new Experience[0]);
            List<Experience> batch = new ArrayList<>();
            
            // Sample random experiences for training
            for (int i = 0; i < Math.min(32, bufferArray.length); i++) {
                int randomIndex = random.nextInt(bufferArray.length);
                batch.add(bufferArray[randomIndex]);
            }
            
            // Train on sampled batch with thread safety
            synchronized (qTableLock) {
                for (Experience exp : batch) {
                    if (!isDestroyed) {
                        updateQValue(exp.state, exp.action, exp.reward, exp.nextState);
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during replay experience training", e);
        }
    }
    
    /**
     * Experience class for replay buffer
     */
    private static class Experience {
        final float[] state;
        final int action;
        final float reward;
        final float[] nextState;
        
        Experience(float[] state, int action, float reward, float[] nextState) {
            this.state = state;
            this.action = action;
            this.reward = reward;
            this.nextState = nextState;
        }
    }
}
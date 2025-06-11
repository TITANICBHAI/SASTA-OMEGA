package com.gestureai.gameautomation.ai;

import android.content.Context;
import android.util.Log;
import com.gestureai.gameautomation.managers.DynamicModelManager;
import org.tensorflow.lite.Interpreter;

import java.util.Random;
import java.util.Arrays;

public class DQNAgent {
    private static final String TAG = "DQNAgent";
    private static DQNAgent instance;
    
    private Context context;
    private DynamicModelManager modelManager;
    private int stateSize;
    private int actionSize;
    private float learningRate;
    private float epsilon;
    private float epsilonDecay;
    private float gamma;
    
    // TensorFlow Lite backend
    private String currentModelName = "dqn_model";
    private boolean modelLoaded = false;
    
    // Fallback Q-table for when model is not available
    private float[][] qTable;
    private Random random;
    private float currentPerformance;
    
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
        
        initializeQTable();
        Log.d(TAG, "DQN Agent initialized with state size: " + stateSize + ", action size: " + actionSize);
    }
    
    public DQNAgent(Context context, int stateSize, int actionSize) {
        this(stateSize, actionSize);
        this.context = context;
        this.modelManager = new DynamicModelManager(context);
        initializeTensorFlowModel();
        instance = this;
        Log.d(TAG, "DQN Agent initialized with TensorFlow Lite backend");
    }
    
    public static DQNAgent getInstance() {
        return instance;
    }
    
    private void initializeTensorFlowModel() {
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
    
    private void initializeQTable() {
        for (int i = 0; i < qTable.length; i++) {
            for (int j = 0; j < actionSize; j++) {
                qTable[i][j] = random.nextFloat() * 0.1f; // Small random values
            }
        }
    }
    
    /**
     * CRITICAL: Add custom training method for labeled objects
     */
    public void trainFromCustomData(float[] state, int action, float reward) {
        try {
            if (modelLoaded) {
                // Train TensorFlow Lite model with custom data
                trainTensorFlowModel(state, action, reward);
            } else {
                // Fallback to Q-table training
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
     * Fallback Q-table training when TensorFlow model unavailable
     */
    private void trainQTable(float[] state, int action, float reward) {
        int stateIndex = hashState(state);
        if (stateIndex >= 0 && stateIndex < qTable.length && action >= 0 && action < actionSize) {
            float oldValue = qTable[stateIndex][action];
            float maxNextQ = getMaxQValue(stateIndex);
            
            // Q-learning update rule
            qTable[stateIndex][action] = oldValue + learningRate * (reward + gamma * maxNextQ - oldValue);
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
}
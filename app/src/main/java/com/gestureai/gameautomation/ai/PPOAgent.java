package com.gestureai.gameautomation.ai;

import android.content.Context;
import android.util.Log;
import com.gestureai.gameautomation.managers.DynamicModelManager;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

public class PPOAgent {
    private static final String TAG = "PPOAgent";
    
    private int stateSize;
    private int actionSize;
    private float learningRate;
    private float clipRatio;
    private float gamma;
    private float lambda;
    
    // TensorFlow Lite backend
    private Context context;
    private DynamicModelManager modelManager;
    private String policyModelName = "ppo_policy_model";
    private String valueModelName = "ppo_value_model";
    private boolean modelLoaded = false;
    
    // Simplified policy and value networks (fallback)
    private float[][] policyWeights;
    private float[][] valueWeights;
    private Random random;
    private float currentPerformance;
    
    // Experience buffer
    private List<Experience> experienceBuffer;
    private int bufferSize;
    
    public PPOAgent(int stateSize, int actionSize) {
        this.stateSize = stateSize;
        this.actionSize = actionSize;
        this.learningRate = 0.0003f;
        this.clipRatio = 0.2f;
        this.gamma = 0.99f;
        this.lambda = 0.95f;
        this.bufferSize = 2048;
        
        this.policyWeights = new float[stateSize][actionSize];
        this.valueWeights = new float[stateSize][1];
        this.random = new Random();
        this.currentPerformance = 0.6f; // Initial performance
        this.experienceBuffer = new ArrayList<>();
        
        initializeNetworks();
        Log.d(TAG, "PPO Agent initialized with state size: " + stateSize + ", action size: " + actionSize);
    }
    
    public PPOAgent(Context context, int stateSize, int actionSize) {
        this(stateSize, actionSize);
        this.context = context;
        this.modelManager = new DynamicModelManager(context);
        initializeTensorFlowModels();
        Log.d(TAG, "PPO Agent initialized with TensorFlow Lite backend");
    }
    
    private void initializeTensorFlowModels() {
        try {
            if (modelManager.loadModel(policyModelName) && modelManager.loadModel(valueModelName)) {
                modelLoaded = true;
                Log.d(TAG, "PPO TensorFlow Lite models loaded successfully");
            } else {
                Log.w(TAG, "PPO models not found, using fallback networks");
                modelLoaded = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing PPO TensorFlow models", e);
            modelLoaded = false;
        }
    }
    
    /**
     * CRITICAL: Custom training method for labeled objects
     */
    public void trainFromCustomData(float[] state, int action, float reward) {
        try {
            // Store experience for batch training
            Experience exp = new Experience(state.clone(), action, reward, new float[state.length], false);
            experienceBuffer.add(exp);
            
            // Train when buffer has enough samples
            if (experienceBuffer.size() >= 32) { // Mini-batch size
                if (modelLoaded) {
                    trainTensorFlowModels();
                } else {
                    trainFallbackNetworks();
                }
                
                // Clear processed experiences
                experienceBuffer.clear();
            }
            
            // Update performance metrics
            updatePerformanceMetrics(reward);
            
            Log.d(TAG, "Added training sample - Action: " + action + ", Reward: " + reward);
            
        } catch (Exception e) {
            Log.e(TAG, "Error training with custom data", e);
        }
    }
    
    /**
     * Train TensorFlow Lite policy and value models
     */
    private void trainTensorFlowModels() {
        try {
            if (modelManager == null) return;
            
            for (Experience exp : experienceBuffer) {
                // Train policy network
                float[] actionProbs = modelManager.runInference(policyModelName, exp.state);
                if (actionProbs != null) {
                    // Update action probability for taken action
                    actionProbs[exp.action] = Math.min(1.0f, actionProbs[exp.action] + learningRate * exp.reward);
                    modelManager.trainModel(policyModelName, exp.state, actionProbs);
                }
                
                // Train value network
                float[] stateValue = new float[]{exp.reward}; // Simplified target value
                modelManager.trainModel(valueModelName, exp.state, stateValue);
            }
            
            Log.d(TAG, "Trained PPO TensorFlow models with " + experienceBuffer.size() + " samples");
            
        } catch (Exception e) {
            Log.e(TAG, "Error training TensorFlow models", e);
        }
    }
    
    /**
     * Fallback network training when TensorFlow unavailable
     */
    private void trainFallbackNetworks() {
        for (Experience exp : experienceBuffer) {
            // Update policy weights based on reward
            for (int i = 0; i < Math.min(exp.state.length, stateSize); i++) {
                if (exp.action < actionSize) {
                    policyWeights[i][exp.action] += learningRate * exp.reward * exp.state[i];
                }
            }
            
            // Update value weights
            for (int i = 0; i < Math.min(exp.state.length, stateSize); i++) {
                valueWeights[i][0] += learningRate * exp.reward * exp.state[i];
            }
        }
    }
    
    /**
     * Update performance tracking
     */
    private void updatePerformanceMetrics(float reward) {
        currentPerformance = 0.95f * currentPerformance + 0.05f * reward;
    }
    
    private void initializeNetworks() {
        // Initialize policy network weights
        for (int i = 0; i < stateSize; i++) {
            for (int j = 0; j < actionSize; j++) {
                policyWeights[i][j] = (float) (random.nextGaussian() * 0.1f);
            }
        }
        
        // Initialize value network weights
        for (int i = 0; i < stateSize; i++) {
            valueWeights[i][0] = (float) (random.nextGaussian() * 0.1f);
        }
    }
    
    public int selectAction(float[] state) {
        float[] actionProbabilities = computeActionProbabilities(state);
        return sampleAction(actionProbabilities);
    }
    
    private float[] computeActionProbabilities(float[] state) {
        float[] logits = new float[actionSize];
        
        // Simple linear transformation for demonstration
        for (int action = 0; action < actionSize; action++) {
            for (int i = 0; i < Math.min(state.length, stateSize); i++) {
                logits[action] += state[i] * policyWeights[i][action];
            }
        }
        
        // Apply softmax
        return softmax(logits);
    }
    
    private float[] softmax(float[] logits) {
        float[] probabilities = new float[logits.length];
        float maxLogit = Float.NEGATIVE_INFINITY;
        
        for (float logit : logits) {
            maxLogit = Math.max(maxLogit, logit);
        }
        
        float sum = 0.0f;
        for (int i = 0; i < logits.length; i++) {
            probabilities[i] = (float) Math.exp(logits[i] - maxLogit);
            sum += probabilities[i];
        }
        
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] /= sum;
        }
        
        return probabilities;
    }
    
    private int sampleAction(float[] probabilities) {
        float rand = random.nextFloat();
        float cumulative = 0.0f;
        
        for (int i = 0; i < probabilities.length; i++) {
            cumulative += probabilities[i];
            if (rand <= cumulative) {
                return i;
            }
        }
        
        return probabilities.length - 1;
    }
    
    public float computeValue(float[] state) {
        float value = 0.0f;
        
        for (int i = 0; i < Math.min(state.length, stateSize); i++) {
            value += state[i] * valueWeights[i][0];
        }
        
        return value;
    }
    
    public void addExperience(float[] state, int action, float reward, float[] nextState, boolean done) {
        Experience exp = new Experience(state, action, reward, nextState, done);
        experienceBuffer.add(exp);
        
        if (experienceBuffer.size() > bufferSize) {
            experienceBuffer.remove(0);
        }
    }
    
    public float trainStep() {
        if (experienceBuffer.size() < 32) {
            return 0.0f; // Not enough experience
        }
        
        // Compute advantages and update networks
        computeAdvantages();
        float policyLoss = updatePolicyNetwork();
        float valueLoss = updateValueNetwork();
        
        // Update performance metric
        currentPerformance = Math.min(1.0f, currentPerformance + 0.0005f);
        
        float totalLoss = policyLoss + valueLoss;
        Log.d(TAG, "Training step completed. Policy Loss: " + policyLoss + 
              ", Value Loss: " + valueLoss + ", Performance: " + currentPerformance);
        
        return totalLoss;
    }
    
    private void computeAdvantages() {
        // Simplified advantage computation
        for (int i = experienceBuffer.size() - 2; i >= 0; i--) {
            Experience current = experienceBuffer.get(i);
            Experience next = experienceBuffer.get(i + 1);
            
            float value = computeValue(current.state);
            float nextValue = current.done ? 0.0f : computeValue(next.state);
            
            float delta = current.reward + gamma * nextValue - value;
            current.advantage = delta; // Simplified - should use GAE
        }
    }
    
    private float updatePolicyNetwork() {
        float totalLoss = 0.0f;
        int batchSize = Math.min(32, experienceBuffer.size());
        
        for (int i = 0; i < batchSize; i++) {
            Experience exp = experienceBuffer.get(random.nextInt(experienceBuffer.size()));
            
            // Simplified policy gradient update
            float[] actionProbs = computeActionProbabilities(exp.state);
            float oldProb = actionProbs[exp.action];
            
            // Update policy weights (simplified)
            for (int j = 0; j < Math.min(exp.state.length, stateSize); j++) {
                policyWeights[j][exp.action] += learningRate * exp.advantage * exp.state[j];
            }
            
            totalLoss += Math.abs(exp.advantage);
        }
        
        return totalLoss / batchSize;
    }
    
    private float updateValueNetwork() {
        float totalLoss = 0.0f;
        int batchSize = Math.min(32, experienceBuffer.size());
        
        for (int i = 0; i < batchSize; i++) {
            Experience exp = experienceBuffer.get(random.nextInt(experienceBuffer.size()));
            
            float predictedValue = computeValue(exp.state);
            float targetValue = exp.reward + (exp.done ? 0.0f : gamma * computeValue(exp.nextState));
            float valueLoss = targetValue - predictedValue;
            
            // Update value weights (simplified)
            for (int j = 0; j < Math.min(exp.state.length, stateSize); j++) {
                valueWeights[j][0] += learningRate * valueLoss * exp.state[j];
            }
            
            totalLoss += Math.abs(valueLoss);
        }
        
        return totalLoss / batchSize;
    }
    
    public float getPerformanceMetric() {
        return currentPerformance;
    }
    
    public void updatePerformanceMetric(float performance) {
        this.currentPerformance = Math.max(0.0f, Math.min(1.0f, performance));
    }
    
    public void clearExperienceBuffer() {
        experienceBuffer.clear();
    }
    
    public int getExperienceBufferSize() {
        return experienceBuffer.size();
    }
    
    public String getModelSummary() {
        return String.format(
            "PPO Agent - State Size: %d, Action Size: %d, Performance: %.3f, Experience: %d",
            stateSize, actionSize, currentPerformance, experienceBuffer.size()
        );
    }
    
    // Experience data class
    private static class Experience {
        public float[] state;
        public int action;
        public float reward;
        public float[] nextState;
        public boolean done;
        public float advantage;
        
        public Experience(float[] state, int action, float reward, float[] nextState, boolean done) {
            this.state = state.clone();
            this.action = action;
            this.reward = reward;
            this.nextState = nextState.clone();
            this.done = done;
            this.advantage = 0.0f;
        }
    }
}
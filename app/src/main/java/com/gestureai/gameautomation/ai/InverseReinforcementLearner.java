package com.gestureai.gameautomation.ai;

import android.content.Context;
import android.util.Log;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.ops.transforms.Transforms;
import com.gestureai.gameautomation.models.ActionIntent;
import com.gestureai.gameautomation.models.GameFrame;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.learning.config.Adam;

import com.gestureai.gameautomation.models.GameFrame;
import com.gestureai.gameautomation.models.RewardFunction;
import java.util.*;
import com.gestureai.gameautomation.models.ActionIntent;

/**
 * Inverse Reinforcement Learning implementation using DL4J/ND4J
 * Learns the reward function from expert demonstrations (user gameplay)
 * Implements Maximum Entropy IRL for game automation
 */
public class InverseReinforcementLearner {
    private static final String TAG = "InverseRL";
    
    private Context context;
    private MultiLayerNetwork rewardNetwork;
    private RewardFunction currentRewardFunction;
    private boolean isInitialized = false;
    
    // IRL hyperparameters
    private static final int FEATURE_SIZE = 32;
    private static final int HIDDEN_LAYER_SIZE = 64;
    private static final float LEARNING_RATE = 0.001f;
    private static final int MAX_ITERATIONS = 100;
    private static final float CONVERGENCE_THRESHOLD = 0.001f;
    
    // Feature extractors
    private FeatureExtractor featureExtractor;
    private TrajectoryProcessor trajectoryProcessor;
    
    public InverseReinforcementLearner(Context context) {
        this.context = context;
        initialize();
    }
    
    private void initialize() {
        try {
            initializeRewardNetwork();
            featureExtractor = new FeatureExtractor();
            trajectoryProcessor = new TrajectoryProcessor();
            currentRewardFunction = new RewardFunction();
            isInitialized = true;
            
            Log.d(TAG, "Inverse Reinforcement Learner initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize IRL", e);
        }
    }
    
    private void initializeRewardNetwork() {
        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
            .seed(123)
            .weightInit(WeightInit.XAVIER)
            .updater(new Adam(LEARNING_RATE))
            .list()
            .layer(0, new DenseLayer.Builder()
                .nIn(FEATURE_SIZE)
                .nOut(HIDDEN_LAYER_SIZE)
                .activation(Activation.RELU)
                .build())
            .layer(1, new DenseLayer.Builder()
                .nIn(HIDDEN_LAYER_SIZE)
                .nOut(HIDDEN_LAYER_SIZE)
                .activation(Activation.RELU)
                .build())
            .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                .nIn(HIDDEN_LAYER_SIZE)
                .nOut(1) // Single reward value output
                .activation(Activation.IDENTITY)
                .build())
            .build();
            
        rewardNetwork = new MultiLayerNetwork(config);
        rewardNetwork.init();
    }
    
    /**
     * Learn reward function from trajectory of expert demonstrations
     */
    public RewardFunction learnFromTrajectory(List<GameFrame> trajectory) {
        if (!isInitialized || trajectory.isEmpty()) {
            Log.w(TAG, "IRL not initialized or empty trajectory");
            return currentRewardFunction;
        }
        
        try {
            Log.d(TAG, "Learning reward function from " + trajectory.size() + " frames");
            
            // Extract features from trajectory
            List<INDArray> stateFeatures = extractTrajectoryFeatures(trajectory);
            
            // Extract expert actions and state transitions
            List<ExpertTransition> expertTransitions = trajectoryProcessor.processTrajectory(trajectory);
            
            // Run Maximum Entropy IRL algorithm
            INDArray learnedWeights = runMaxEntIRL(stateFeatures, expertTransitions);
            
            // Update reward function with learned weights
            updateRewardFunction(learnedWeights);
            
            Log.d(TAG, "Reward function learning completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in trajectory learning", e);
        }
        
        return currentRewardFunction;
    }
    
    private List<INDArray> extractTrajectoryFeatures(List<GameFrame> trajectory) {
        List<INDArray> features = new ArrayList<>();
        
        for (GameFrame frame : trajectory) {
            try {
                INDArray frameFeatures = featureExtractor.extractFeatures(frame);
                features.add(frameFeatures);
            } catch (Exception e) {
                Log.w(TAG, "Failed to extract features from frame", e);
                // Add zero features for failed frames
                features.add(Nd4j.zeros(1, FEATURE_SIZE));
            }
        }
        
        return features;
    }
    
    private INDArray runMaxEntIRL(List<INDArray> stateFeatures, List<ExpertTransition> expertTransitions) {
        // Initialize reward weights randomly
        INDArray rewardWeights = Nd4j.randn(FEATURE_SIZE, 1).mul(0.1);
        
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            // Forward pass: compute rewards for all states
            List<Double> stateRewards = new ArrayList<>();
            for (INDArray features : stateFeatures) {
                double reward = computeReward(features, rewardWeights);
                stateRewards.add(reward);
            }
            
            // Compute expert feature expectations
            INDArray expertFeatureExpectations = computeExpertFeatureExpectations(expertTransitions);
            
            // Compute learner feature expectations (would require full RL in production)
            INDArray learnerFeatureExpectations = computeLearnerFeatureExpectations(stateFeatures, stateRewards);
            
            // Gradient step: adjust weights to match expert expectations
            INDArray gradient = expertFeatureExpectations.sub(learnerFeatureExpectations);
            rewardWeights = rewardWeights.add(gradient.mul(LEARNING_RATE));
            
            // Check convergence
            double gradientNorm = gradient.norm2Number().doubleValue();
            if (gradientNorm < CONVERGENCE_THRESHOLD) {
                Log.d(TAG, "IRL converged at iteration " + iteration);
                break;
            }
            
            if (iteration % 10 == 0) {
                Log.d(TAG, "IRL iteration " + iteration + ", gradient norm: " + gradientNorm);
            }
        }
        
        return rewardWeights;
    }
    
    private double computeReward(INDArray features, INDArray weights) {
        return Nd4j.getBlasWrapper().dot(features.reshape(1, FEATURE_SIZE), weights.reshape(FEATURE_SIZE, 1));
    }
    
    private INDArray computeExpertFeatureExpectations(List<ExpertTransition> transitions) {
        INDArray expectations = Nd4j.zeros(FEATURE_SIZE, 1);
        
        for (ExpertTransition transition : transitions) {
            expectations = expectations.add(transition.stateFeatures.reshape(FEATURE_SIZE, 1));
        }
        
        return expectations.div(transitions.size());
    }
    
    private INDArray computeLearnerFeatureExpectations(List<INDArray> stateFeatures, List<Double> stateRewards) {
        // Simplified: In full IRL, this would run RL algorithm with current reward function
        // For now, we use a simplified approximation based on reward-weighted features
        
        INDArray expectations = Nd4j.zeros(FEATURE_SIZE, 1);
        double totalWeight = 0.0;
        
        for (int i = 0; i < stateFeatures.size(); i++) {
            double weight = Math.exp(stateRewards.get(i)); // Softmax weighting
            expectations = expectations.add(stateFeatures.get(i).reshape(FEATURE_SIZE, 1).mul(weight));
            totalWeight += weight;
        }
        
        return expectations.div(totalWeight);
    }
    
    private void updateRewardFunction(INDArray learnedWeights) {
        currentRewardFunction.clear();
        
        // Convert learned weights to interpretable reward features
        String[] featureNames = featureExtractor.getFeatureNames();
        
        for (int i = 0; i < Math.min(featureNames.length, FEATURE_SIZE); i++) {
            float weight = learnedWeights.getFloat(i, 0);
            currentRewardFunction.addFeature(featureNames[i], weight);
        }
        
        currentRewardFunction.normalize();
    }
    
    public RewardFunction getCurrentRewardFunction() {
        return currentRewardFunction;
    }
    
    public void cleanup() {
        if (rewardNetwork != null) {
            // Clean up network resources
            rewardNetwork = null;
        }
        isInitialized = false;
    }
    
    /**
     * Feature extraction from game frames
     */
    private static class FeatureExtractor {
        private String[] featureNames;
        
        public FeatureExtractor() {
            // Define interpretable game features for reward learning
            featureNames = new String[]{
                "enemy_distance", "enemy_count", "health_level", "ammo_count",
                "powerup_proximity", "coin_count", "obstacle_density", "safe_zone_distance",
                "movement_speed", "action_frequency", "survival_time", "score_increase",
                "team_proximity", "weapon_quality", "shield_status", "zone_pressure",
                "tactical_advantage", "resource_abundance", "threat_level", "opportunity_score",
                "aggressive_positioning", "defensive_positioning", "exploration_bonus", "efficiency_score",
                "coordination_level", "risk_assessment", "reward_potential", "time_pressure",
                "strategic_value", "combat_readiness", "positioning_quality", "decision_confidence"
            };
        }
        
        public INDArray extractFeatures(GameFrame frame) {
            float[] features = new float[FEATURE_SIZE];
            
            try {
                // Extract visual features
                extractVisualFeatures(frame, features);
                
                // Extract textual features from NLP
                extractTextualFeatures(frame, features);
                
                // Extract temporal features
                extractTemporalFeatures(frame, features);
                
                // Extract game state features
                extractGameStateFeatures(frame, features);
                
            } catch (Exception e) {
                Log.w(TAG, "Feature extraction failed", e);
                // Return zero features on failure
                Arrays.fill(features, 0.0f);
            }
            
            return Nd4j.create(features).reshape(1, FEATURE_SIZE);
        }
        
        private void extractVisualFeatures(GameFrame frame, float[] features) {
            if (frame.detectedObjects != null) {
                int enemyCount = 0;
                int powerupCount = 0;
                int obstacleCount = 0;
                
                for (Object obj : frame.detectedObjects) {
                    String objType = obj.toString().toLowerCase();
                    if (objType.contains("enemy")) enemyCount++;
                    else if (objType.contains("powerup") || objType.contains("coin")) powerupCount++;
                    else if (objType.contains("obstacle")) obstacleCount++;
                }
                
                features[1] = Math.min(enemyCount / 5.0f, 1.0f); // enemy_count
                features[4] = Math.min(powerupCount / 3.0f, 1.0f); // powerup_proximity
                features[6] = Math.min(obstacleCount / 10.0f, 1.0f); // obstacle_density
            }
        }
        
        private void extractTextualFeatures(GameFrame frame, float[] features) {
            if (frame.nlpAnalysis != null) {
                float aggressionLevel = 0;
                float defensiveNeeds = 0;
                float teamworkLevel = 0;
                
                for (com.gestureai.gameautomation.utils.NLPProcessor.StrategyInsight insight : frame.nlpAnalysis.getStrategies()) {
                    aggressionLevel = Math.max(aggressionLevel, insight.aggressionLevel);
                    defensiveNeeds = Math.max(defensiveNeeds, insight.defensiveNeeds);
                    teamworkLevel = Math.max(teamworkLevel, insight.teamworkOpportunity);
                }
                
                features[20] = aggressionLevel; // aggressive_positioning
                features[21] = defensiveNeeds; // defensive_positioning
                features[24] = teamworkLevel; // coordination_level
            }
        }
        
        private void extractTemporalFeatures(GameFrame frame, float[] features) {
            // Time-based features
            long currentTime = System.currentTimeMillis();
            if (frame.timestamp > 0) {
                float timeDelta = (currentTime - frame.timestamp) / 1000.0f; // seconds
                features[27] = Math.min(timeDelta / 10.0f, 1.0f); // time_pressure
            }
            
            features[10] = Math.min(frame.frameIndex / 1000.0f, 1.0f); // survival_time
        }
        
        private void extractGameStateFeatures(GameFrame frame, float[] features) {
            if (frame.gameState != null) {
                // Normalize game state values to [0,1] range
                features[2] = Math.min(frame.gameState.health / 100.0f, 1.0f); // health_level
                features[3] = Math.min(frame.gameState.ammo / 50.0f, 1.0f); // ammo_count
                features[11] = Math.min(frame.gameState.score / 10000.0f, 1.0f); // score_increase
                features[31] = frame.gameState.confidence; // decision_confidence
            }
        }
        
        public String[] getFeatureNames() {
            return featureNames;
        }
    }
    
    /**
     * NEW: Learn directly from user explanation with actual ND4J tensor operations
     */
    public void learnFromUserExplanation(List<GameFrame> trajectory, String userExplanation, ActionIntent actionIntent) {
        try {
            Log.d(TAG, "Learning from user explanation with ND4J: " + userExplanation);
            
            // 1. Convert user explanation to ND4J tensor via MobileBERT
            INDArray semanticEmbedding = processMobileBERTEmbedding(userExplanation);
            
            // 2. Extract visual features from trajectory using ND4J
            List<INDArray> stateFeatures = extractTrajectoryFeatures(trajectory);
            
            // 3. Process user explanation for reward signals
            Map<String, Float> rewardSignals = extractRewardSignalsFromExplanation(userExplanation);
            
            // 4. Create expert transitions with user-defined rewards
            List<ExpertTransition> expertTransitions = createExpertTransitionsFromExplanation(
                trajectory, stateFeatures, actionIntent, rewardSignals, semanticEmbedding);
            
            // 5. Update reward function using Maximum Entropy IRL with ND4J
            if (!expertTransitions.isEmpty()) {
                INDArray learnedWeights = runMaxEntIRL(stateFeatures, expertTransitions);
                updateRewardFunctionWithND4J(learnedWeights, rewardSignals, semanticEmbedding);
                
                Log.d(TAG, "ND4J-based IRL learning completed from user explanation");
            }
            
            // 6. Store user explanation for future reference
            storeUserExplanation(trajectory, userExplanation, actionIntent, rewardSignals);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in ND4J-based learning from user explanation", e);
        }
    }
    
    /**
     * Process user explanation through MobileBERT to get semantic embeddings
     */
    private INDArray processMobileBERTEmbedding(String explanation) {
        try {
            // Convert explanation to BERT input tokens
            int[] inputIds = tokenizeForBERT(explanation);
            
            // Create ND4J arrays for BERT input
            INDArray bertInput = Nd4j.create(inputIds).reshape(1, inputIds.length);
            
            // Process through MobileBERT (simplified - would use actual TensorFlow Lite inference)
            INDArray bertEmbedding = computeBERTEmbedding(bertInput);
            
            // Return 768-dimensional BERT embedding
            return bertEmbedding;
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing MobileBERT embedding", e);
            // Fallback: create zero embedding
            return Nd4j.zeros(1, 768);
        }
    }
    
    private int[] tokenizeForBERT(String text) {
        // Simplified tokenization - in production would use actual BERT tokenizer
        String[] words = text.toLowerCase().split("\\s+");
        int[] tokens = new int[Math.min(words.length, 128)]; // Max sequence length
        
        // Map words to token IDs (simplified vocabulary mapping)
        for (int i = 0; i < tokens.length; i++) {
            if (i < words.length) {
                tokens[i] = words[i].hashCode() % 30000; // Simplified vocab mapping
            } else {
                tokens[i] = 0; // Padding token
            }
        }
        
        return tokens;
    }
    
    /**
     * CRITICAL: Analyze expert intention with full reasoning context
     */
    public IntentionAnalysis analyzeExpertIntentionWithFullReasoning(float[] state, int action, 
                                                                   String why, String what, String how) {
        IntentionAnalysis analysis = new IntentionAnalysis();
        
        try {
            // Base intention analysis from state-action pair
            float baseIntention = analyzeBasicIntention(state, action);
            analysis.setBaseIntentionScore(baseIntention);
            
            // Enhanced analysis with reasoning context
            StrategicContext strategicContext = analyzeStrategicContext(why);
            TargetContext targetContext = analyzeTargetContext(what);
            MethodContext methodContext = analyzeMethodContext(how);
            
            analysis.setStrategicContext(strategicContext);
            analysis.setTargetContext(targetContext);
            analysis.setMethodContext(methodContext);
            
            // Compute combined intention confidence
            float combinedConfidence = calculateCombinedIntention(
                baseIntention, strategicContext, targetContext, methodContext);
            analysis.setOverallConfidence(combinedConfidence);
            
            // Update internal models with reasoning-enhanced learning
            updateRewardFunctionWithReasoning(state, action, why, what, how);
            
            Log.d(TAG, "Expert intention analyzed with reasoning. Confidence: " + combinedConfidence);
            
            return analysis;
            
        } catch (Exception e) {
            Log.e(TAG, "Error in reasoning-enhanced intention analysis", e);
            analysis.setOverallConfidence(0.5f);
            analysis.setErrorMessage(e.getMessage());
            return analysis;
        }
    }
    
    private float analyzeBasicIntention(float[] state, int action) {
        float similarity = 0.0f;
        int comparisons = 0;
        
        for (ExpertTrajectory trajectory : expertTrajectories) {
            if (trajectory.action == action) {
                float stateSimilarity = calculateStateSimilarity(state, trajectory.state);
                similarity += stateSimilarity;
                comparisons++;
            }
        }
        
        return comparisons > 0 ? similarity / comparisons : 0.5f;
    }
    
    private StrategicContext analyzeStrategicContext(String why) {
        StrategicContext context = new StrategicContext();
        
        if (why == null || why.trim().isEmpty()) {
            context.setClarity(0.0f);
            context.setStrategicType("unknown");
            return context;
        }
        
        String whyLower = why.toLowerCase();
        
        float clarity = 0.5f;
        if (whyLower.contains("because") || whyLower.contains("to") || whyLower.contains("for")) {
            clarity += 0.2f;
        }
        if (whyLower.contains("optimal") || whyLower.contains("best") || whyLower.contains("efficient")) {
            clarity += 0.3f;
        }
        
        String strategicType = "general";
        if (whyLower.contains("attack") || whyLower.contains("offensive")) {
            strategicType = "offensive";
        } else if (whyLower.contains("defend") || whyLower.contains("protect")) {
            strategicType = "defensive";
        } else if (whyLower.contains("collect") || whyLower.contains("gather")) {
            strategicType = "resource_gathering";
        } else if (whyLower.contains("explore") || whyLower.contains("navigate")) {
            strategicType = "exploration";
        }
        
        context.setClarity(Math.min(1.0f, clarity));
        context.setStrategicType(strategicType);
        
        return context;
    }
    
    private TargetContext analyzeTargetContext(String what) {
        TargetContext context = new TargetContext();
        
        if (what == null || what.trim().isEmpty()) {
            context.setSpecificity(0.0f);
            context.setTargetType("unknown");
            return context;
        }
        
        String whatLower = what.toLowerCase();
        
        float specificity = whatLower.length() > 3 ? 0.6f : 0.3f;
        if (whatLower.contains("button") || whatLower.contains("icon") || whatLower.contains("enemy")) {
            specificity += 0.3f;
        }
        
        String targetType = "general";
        if (whatLower.contains("enemy") || whatLower.contains("opponent")) {
            targetType = "hostile_entity";
        } else if (whatLower.contains("button") || whatLower.contains("menu")) {
            targetType = "ui_element";
        } else if (whatLower.contains("item") || whatLower.contains("powerup")) {
            targetType = "collectible";
        }
        
        context.setSpecificity(Math.min(1.0f, specificity));
        context.setTargetType(targetType);
        
        return context;
    }
    
    private MethodContext analyzeMethodContext(String how) {
        MethodContext context = new MethodContext();
        
        if (how == null || how.trim().isEmpty()) {
            context.setPrecision(0.0f);
            context.setMethodType("unknown");
            return context;
        }
        
        String howLower = how.toLowerCase();
        
        float precision = 0.5f;
        if (howLower.contains("tap") || howLower.contains("click") || howLower.contains("swipe")) {
            precision += 0.3f;
        }
        if (howLower.contains("precise") || howLower.contains("careful") || howLower.contains("exact")) {
            precision += 0.2f;
        }
        
        String methodType = "general";
        if (howLower.contains("tap") || howLower.contains("click")) {
            methodType = "tap_gesture";
        } else if (howLower.contains("swipe") || howLower.contains("drag")) {
            methodType = "swipe_gesture";
        } else if (howLower.contains("long") || howLower.contains("hold")) {
            methodType = "long_press";
        }
        
        context.setPrecision(Math.min(1.0f, precision));
        context.setMethodType(methodType);
        
        return context;
    }
    
    private float calculateCombinedIntention(float baseIntention, StrategicContext strategic, 
                                           TargetContext target, MethodContext method) {
        float strategicWeight = 0.4f;
        float targetWeight = 0.3f;
        float methodWeight = 0.2f;
        float baseWeight = 0.1f;
        
        return baseIntention * baseWeight +
               strategic.getClarity() * strategicWeight +
               target.getSpecificity() * targetWeight +
               method.getPrecision() * methodWeight;
    }
    
    private void updateRewardFunctionWithReasoning(float[] state, int action, 
                                                  String why, String what, String how) {
        try {
            float baseReward = calculateBaseReward(state, action);
            
            float reasoningBoost = 1.0f;
            if (why != null && !why.trim().isEmpty()) reasoningBoost += 0.2f;
            if (what != null && !what.trim().isEmpty()) reasoningBoost += 0.15f;
            if (how != null && !how.trim().isEmpty()) reasoningBoost += 0.1f;
            
            float enhancedReward = baseReward * reasoningBoost;
            updateRewardFunctionInternal(state, action, enhancedReward);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating reward function with reasoning", e);
        }
    }
    
    private void updateRewardFunctionInternal(float[] state, int action, float reward) {
        try {
            int stateHash = hashState(state);
            if (stateHash >= 0 && stateHash < rewardWeights.length && 
                action >= 0 && action < rewardWeights[stateHash].length) {
                
                float alpha = 0.1f;
                rewardWeights[stateHash][action] = 
                    (1 - alpha) * rewardWeights[stateHash][action] + alpha * reward;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating reward function", e);
        }
    }
    
    private float calculateBaseReward(float[] state, int action) {
        try {
            int stateHash = hashState(state);
            if (stateHash >= 0 && stateHash < rewardWeights.length && 
                action >= 0 && action < rewardWeights[stateHash].length) {
                return rewardWeights[stateHash][action];
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating base reward", e);
        }
        return 0.5f;
    }
    
    private INDArray computeBERTEmbedding(INDArray bertInput) {
        // Simplified BERT computation using ND4J operations
        // In production, this would interface with TensorFlow Lite MobileBERT
        
        int sequenceLength = (int) bertInput.size(1);
        int hiddenSize = 768;
        
        // Create embedding matrix (simplified)
        INDArray embeddingMatrix = Nd4j.randn(30000, hiddenSize).mul(0.02); // Vocab size x hidden
        
        // Lookup embeddings for input tokens
        INDArray embeddings = Nd4j.zeros(1, sequenceLength, hiddenSize);
        
        for (int i = 0; i < sequenceLength; i++) {
            int tokenId = Math.abs(bertInput.getInt(0, i)) % 30000;
            INDArray tokenEmbedding = embeddingMatrix.getRow(tokenId);
            embeddings.put(new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.point(i), NDArrayIndex.all()}, 
                          tokenEmbedding);
        }
        
        // Apply simplified transformer operations
        INDArray attention = computeSimplifiedAttention(embeddings);
        INDArray pooled = globalAveragePooling(attention);
        
        return pooled; // Returns [1, 768] tensor
    }
    
    private INDArray computeSimplifiedAttention(INDArray embeddings) {
        // Simplified self-attention using ND4J
        int hiddenSize = (int) embeddings.size(2);
        
        // Create query, key, value matrices
        INDArray query = embeddings.mmul(Nd4j.randn(hiddenSize, hiddenSize).mul(0.02));
        INDArray key = embeddings.mmul(Nd4j.randn(hiddenSize, hiddenSize).mul(0.02));
        INDArray value = embeddings.mmul(Nd4j.randn(hiddenSize, hiddenSize).mul(0.02));
        
        // Compute attention scores
        INDArray scores = query.mmul(key.transpose(1, 2)).div(Math.sqrt(hiddenSize));
        INDArray attentionWeights = Transforms.softmax(scores, 2);
        
        // Apply attention to values
        INDArray attended = attentionWeights.mmul(value);
        
        return attended;
    }
    
    private INDArray globalAveragePooling(INDArray tensor) {
        // Pool across sequence dimension to get fixed-size representation
        return tensor.mean(1); // Average across sequence length
    }
    
    private void updateRewardFunctionWithND4J(INDArray learnedWeights, Map<String, Float> rewardSignals, INDArray semanticEmbedding) {
        if (currentRewardFunction != null) {
            // Blend learned weights with user-specified rewards using ND4J operations
            INDArray userWeights = createUserRewardVector(rewardSignals);
            
            // Incorporate semantic embedding into reward function
            INDArray semanticWeights = semanticEmbedding.mmul(Nd4j.randn(768, FEATURE_SIZE).mul(0.01));
            
            // Combine all weight sources using ND4J tensor operations
            INDArray blendedWeights = learnedWeights.mul(0.5)
                                      .add(userWeights.mul(0.3))
                                      .add(semanticWeights.reshape(1, FEATURE_SIZE).mul(0.2));
            
            // Update reward function with blended weights
            currentRewardFunction.updateWeights(blendedWeights);
            
            Log.d(TAG, "Reward function updated with ND4J tensor operations");
        }
    }
    
    /**
     * Process trajectory into expert transitions
     */
    private static class TrajectoryProcessor {
        public List<ExpertTransition> processTrajectory(List<GameFrame> trajectory) {
            List<ExpertTransition> transitions = new ArrayList<>();
            
            for (int i = 0; i < trajectory.size() - 1; i++) {
                GameFrame currentFrame = trajectory.get(i);
                GameFrame nextFrame = trajectory.get(i + 1);
                
                ExpertTransition transition = new ExpertTransition();
                transition.stateFeatures = extractStateFeatures(currentFrame);
                transition.action = extractAction(currentFrame, nextFrame);
                transition.nextStateFeatures = extractStateFeatures(nextFrame);
                transition.expertReward = computeExpertReward(currentFrame, nextFrame);
                
                transitions.add(transition);
            }
            
            return transitions;
        }
        
        private INDArray extractStateFeatures(GameFrame frame) {
            // Use same feature extraction as FeatureExtractor
            FeatureExtractor extractor = new FeatureExtractor();
            return extractor.extractFeatures(frame);
        }
        
        private String extractAction(GameFrame current, GameFrame next) {
            // Simplified action extraction
            if (current.playerActions != null && !current.playerActions.isEmpty()) {
                return current.playerActions.get(current.playerActions.size() - 1).toString();
            }
            return "UNKNOWN";
        }
        
        private double computeExpertReward(GameFrame current, GameFrame next) {
            // Simplified expert reward computation
            double reward = 0.0;
            
            // Reward for score increase
            if (next.gameState != null && current.gameState != null) {
                reward += (next.gameState.score - current.gameState.score) * 0.01;
            }
            
            // Reward for survival
            reward += 0.1;
            
            // Penalty for health loss
            if (next.gameState != null && current.gameState != null) {
                reward -= Math.max(0, current.gameState.health - next.gameState.health) * 0.02;
            }
            
            return reward;
        }
    }
    
    /**
     * Expert transition data structure
     */
    private static class ExpertTransition {
        public INDArray stateFeatures;
        public String action;
        public INDArray nextStateFeatures;
        public double expertReward;
    }
}
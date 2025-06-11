package com.gestureai.gameautomation;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import java.util.*;

/**
 * Advanced AI decision coordination engine integrating all game automation components
 */
public class DecisionEngine {
    private static final String TAG = "DecisionEngine";

    private Context context;
    private MultiLayerNetwork decisionNetwork;
    private PlayerTracker playerTracker;
    private GameContextAnalyzer contextAnalyzer;
    private WeaponRecognizer weaponRecognizer;
    private TeamClassifier teamClassifier;
    private MinimapAnalyzer minimapAnalyzer;
    private GameTypeDetector gameTypeDetector;
    private PerformanceTracker performanceTracker;

    private DecisionState currentState;
    private Queue<DecisionHistory> decisionHistory;
    private Map<String, Float> actionWeights;
    private boolean isInitialized = false;

    public static class DecisionState {
        public GameContextAnalyzer.GameContext gameContext;
        public List<PlayerTracker.PlayerData> players;
        public WeaponRecognizer.WeaponDetectionResult weapons;
        public TeamClassifier.TeamAnalysisResult teamAnalysis;
        public MinimapAnalyzer.MinimapData minimapData;
        public GameTypeDetector.GameType gameType;
        public float overallThreat;
        public float opportunityScore;
        public long timestamp;

        public DecisionState() {
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class DecisionHistory {
        public DecisionState state;
        public GameAction actionTaken;
        public float reward;
        public boolean wasSuccessful;
        public long executionTime;

        public DecisionHistory(DecisionState state, GameAction action) {
            this.state = state;
            this.actionTaken = action;
            this.executionTime = System.currentTimeMillis();
        }
    }

    public static class DecisionResult {
        public GameAction primaryAction;
        public List<GameAction> alternativeActions;
        public float confidence;
        public String reasoning;
        public Map<String, Float> componentContributions;
        public DecisionPriority priority;

        public DecisionResult() {
            this.alternativeActions = new ArrayList<>();
            this.componentContributions = new HashMap<>();
        }
    }

    public enum DecisionPriority {
        EMERGENCY, HIGH, MEDIUM, LOW, BACKGROUND
    }

    public DecisionEngine(Context context, PlayerTracker playerTracker,
                          GameContextAnalyzer contextAnalyzer, WeaponRecognizer weaponRecognizer,
                          TeamClassifier teamClassifier, MinimapAnalyzer minimapAnalyzer,
                          GameTypeDetector gameTypeDetector, PerformanceTracker performanceTracker) {

        this.context = context;
        this.playerTracker = playerTracker;
        this.contextAnalyzer = contextAnalyzer;
        this.weaponRecognizer = weaponRecognizer;
        this.teamClassifier = teamClassifier;
        this.minimapAnalyzer = minimapAnalyzer;
        this.gameTypeDetector = gameTypeDetector;
        this.performanceTracker = performanceTracker;

        this.decisionHistory = new LinkedList<>();
        this.actionWeights = new HashMap<>();

        initialize();
    }

    private void initialize() {
        try {
            // Initialize decision neural network
            initializeDecisionNetwork();

            // Initialize action weights
            initializeActionWeights();

            isInitialized = true;
            Log.d(TAG, "Decision Engine initialized");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Decision Engine", e);
        }
    }

    private void initializeDecisionNetwork() {
        // Neural network for decision making
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .weightInit(WeightInit.XAVIER)
                .updater(new org.nd4j.linalg.learning.config.Adam(0.001))
                .list()
                .layer(new DenseLayer.Builder().nIn(50).nOut(128) // 50 input features
                        .activation(Activation.RELU).build())
                .layer(new DenseLayer.Builder().nIn(128).nOut(64)
                        .activation(Activation.RELU).build())
                .layer(new DenseLayer.Builder().nIn(64).nOut(32)
                        .activation(Activation.RELU).build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .nIn(32).nOut(15) // 15 possible action types
                        .activation(Activation.SOFTMAX).build())
                .build();

        decisionNetwork = new MultiLayerNetwork(conf);
        decisionNetwork.init();

        Log.d(TAG, "Decision network initialized");
    }

    private void initializeActionWeights() {
        // Base weights for different action types
        actionWeights.put("EMERGENCY_HEAL", 10.0f);
        actionWeights.put("EMERGENCY_RELOAD", 9.0f);
        actionWeights.put("ZONE_ROTATION", 8.0f);
        actionWeights.put("ENGAGE_ENEMY", 7.0f);
        actionWeights.put("TAKE_COVER", 6.0f);
        actionWeights.put("COLLECT_LOOT", 5.0f);
        actionWeights.put("REPOSITION", 4.0f);
        actionWeights.put("SCOUT_AREA", 3.0f);
        actionWeights.put("RELOAD", 2.0f);
        actionWeights.put("WAIT", 1.0f);
    }

    public DecisionResult makeDecision(Bitmap gameScreen) {
        DecisionResult result = new DecisionResult();

        if (!isInitialized || gameScreen == null) {
            return createEmergencyResult("System not initialized");
        }

        try {
            // Step 1: Gather comprehensive game state
            currentState = gatherGameState(gameScreen);

            // Step 2: Analyze critical situations
            DecisionPriority priority = assessSituationPriority(currentState);
            result.priority = priority;

            // Step 3: Generate action candidates
            List<GameAction> candidates = generateActionCandidates(currentState);

            // Step 4: Evaluate each candidate
            evaluateActionCandidates(candidates, currentState, result);

            // Step 5: Apply neural network decision making
            applyNeuralNetworkDecision(currentState, result);

            // Step 6: Apply experience-based adjustments
            applyExperienceAdjustments(result);

            // Step 7: Generate reasoning
            generateDecisionReasoning(result, currentState);

            // Step 8: Record decision for learning
            recordDecision(result);

            Log.d(TAG, "Decision made: " + result.primaryAction.getActionType() +
                    " (Priority: " + priority + ", Confidence: " + result.confidence + ")");

        } catch (Exception e) {
            Log.e(TAG, "Error making decision", e);
            return createEmergencyResult("Decision error: " + e.getMessage());
        }

        return result;
    }

    private DecisionState gatherGameState(Bitmap gameScreen) {
        DecisionState state = new DecisionState();

        try {
            // Gather data from all components
            List<ObjectDetectionEngine.DetectedObject> detectedObjects = new ArrayList<>(); // From object detection

            state.players = playerTracker.updateTracking(detectedObjects);
            state.gameContext = contextAnalyzer.analyzeGameScreen(gameScreen, detectedObjects);
            state.weapons = weaponRecognizer.analyzeWeapons(gameScreen);
            state.teamAnalysis = teamClassifier.classifyPlayers(gameScreen, state.players);
            state.minimapData = minimapAnalyzer.analyzeScreen(gameScreen);
            state.gameType = gameTypeDetector.detectGameType(gameScreen).gameType;

            // Calculate composite scores
            state.overallThreat = calculateOverallThreat(state);
            state.opportunityScore = calculateOpportunityScore(state);

        } catch (Exception e) {
            Log.w(TAG, "Error gathering game state", e);
        }

        return state;
    }

    private float calculateOverallThreat(DecisionState state) {
        float threat = 0f;

        // Threat from enemies
        if (state.teamAnalysis != null) {
            float enemyThreat = Math.min(1.0f, state.teamAnalysis.enemyCount * 0.2f);
            threat += enemyThreat * 0.4f;
        }

        // Health-based threat
        if (state.gameContext != null) {
            float healthPercent = state.gameContext.resourceLevels.getOrDefault("health", 100f) / 100f;
            threat += (1.0f - healthPercent) * 0.3f;
        }

        // Zone threat (battle royale)
        if (state.minimapData != null && state.minimapData.playerPosition != null) {
            if (!state.minimapData.playerPosition.isInSafeZone) {
                threat += 0.3f;
            }
        }

        return Math.min(1.0f, threat);
    }

    private float calculateOpportunityScore(DecisionState state) {
        float opportunity = 0f;

        // Loot opportunities
        if (state.gameContext != null) {
            opportunity += Math.min(0.3f, state.gameContext.availableWeapons.size() * 0.1f);
        }

        // Weak enemies
        if (state.players != null) {
            long weakEnemies = state.players.stream()
                    .filter(p -> "enemy".equals(p.teamStatus) && p.confidence < 0.5f)
                    .count();
            opportunity += Math.min(0.4f, weakEnemies * 0.1f);
        }

        // Safe positioning
        if (state.minimapData != null && state.minimapData.playerPosition != null) {
            if (state.minimapData.playerPosition.isInSafeZone) {
                opportunity += 0.3f;
            }
        }

        return Math.min(1.0f, opportunity);
    }

    private DecisionPriority assessSituationPriority(DecisionState state) {
        // Emergency situations
        if (state.gameContext != null) {
            float health = state.gameContext.resourceLevels.getOrDefault("health", 100f);
            if (health < 20f) return DecisionPriority.EMERGENCY;
        }

        if (state.minimapData != null && state.minimapData.currentZone != null) {
            if (!state.minimapData.playerPosition.isInSafeZone &&
                    state.minimapData.currentZone.timeRemaining < 30f) {
                return DecisionPriority.EMERGENCY;
            }
        }

        // High priority situations
        if (state.overallThreat > 0.7f) return DecisionPriority.HIGH;

        if (state.teamAnalysis != null && state.teamAnalysis.enemyCount > 2) {
            return DecisionPriority.HIGH;
        }

        // Medium priority
        if (state.overallThreat > 0.4f || state.opportunityScore > 0.6f) {
            return DecisionPriority.MEDIUM;
        }

        // Low priority
        if (state.opportunityScore > 0.3f) return DecisionPriority.LOW;

        return DecisionPriority.BACKGROUND;
    }

    private List<GameAction> generateActionCandidates(DecisionState state) {
        List<GameAction> candidates = new ArrayList<>();

        // Emergency actions
        if (state.gameContext != null) {
            float health = state.gameContext.resourceLevels.getOrDefault("health", 100f);
            if (health < 30f) {
                candidates.add(new GameAction("EMERGENCY_HEAL", 500, 500, 0.9f, "emergency"));
            }

            float ammo = state.gameContext.resourceLevels.getOrDefault("ammo", 30f);
            if (ammo < 5f && state.weapons != null && state.weapons.currentWeapon != null) {
                candidates.add(new GameAction("EMERGENCY_RELOAD", 500, 500, 0.9f, "emergency"));
            }
        }

        // Zone rotation (battle royale)
        if (state.minimapData != null && state.minimapData.playerPosition != null &&
                !state.minimapData.playerPosition.isInSafeZone) {
            int targetX = (int) (state.minimapData.currentZone.center[0] * 1080);
            int targetY = (int) (state.minimapData.currentZone.center[1] * 1920);
            candidates.add(new GameAction("ZONE_ROTATION", targetX, targetY, 0.8f, "survival"));
        }

        // Combat actions
        if (state.teamAnalysis != null) {
            List<TeamClassifier.PlayerClassification> enemies = teamClassifier.getEnemyPlayers(state.teamAnalysis);

            for (TeamClassifier.PlayerClassification enemy : enemies) {
                if (enemy.confidence > 0.6f) {
                    int enemyX = enemy.playerRegion.centerX();
                    int enemyY = enemy.playerRegion.centerY();

                    // Engagement action
                    candidates.add(new GameAction("ENGAGE_ENEMY", enemyX, enemyY,
                            enemy.confidence * 0.8f, "combat"));

                    // Cover action
                    int coverX = enemyX + (enemyX > 540 ? -200 : 200);
                    int coverY = enemyY;
                    candidates.add(new GameAction("TAKE_COVER", coverX, coverY, 0.7f, "tactical"));
                }
            }
        }

        // Utility actions
        candidates.add(new GameAction("SCOUT_AREA", 540, 400, 0.4f, "information"));
        candidates.add(new GameAction("REPOSITION", 540, 960, 0.3f, "positioning"));

        // Weapon management
        if (state.weapons != null && state.weapons.currentWeapon != null) {
            if (weaponRecognizer.shouldReload(state.weapons.currentWeapon)) {
                candidates.add(new GameAction("RELOAD", 540, 960, 0.6f, "weapon_management"));
            }
        }

        return candidates;
    }

    private void evaluateActionCandidates(List<GameAction> candidates, DecisionState state, DecisionResult result) {
        float bestScore = 0f;
        GameAction bestAction = null;

        for (GameAction candidate : candidates) {
            float score = evaluateAction(candidate, state);

            if (score > bestScore) {
                bestScore = score;
                bestAction = candidate;
            }

            // Add to alternatives if score is reasonable
            if (score > 0.3f && !candidate.equals(bestAction)) {
                result.alternativeActions.add(candidate);
            }
        }

        result.primaryAction = bestAction;
        result.confidence = bestScore;
    }

    private float evaluateAction(GameAction action, DecisionState state) {
        float score = 0f;

        // Base weight from action type
        String actionType = action.getActionType();
        score += actionWeights.getOrDefault(actionType, 1.0f) * 0.1f;

        // Context-specific scoring
        switch (actionType) {
            case "EMERGENCY_HEAL":
                float health = state.gameContext.resourceLevels.getOrDefault("health", 100f);
                score += (100f - health) / 100f * 2.0f;
                break;

            case "ZONE_ROTATION":
                if (state.minimapData != null && !state.minimapData.playerPosition.isInSafeZone) {
                    float urgency = minimapAnalyzer.calculateZoneRotationUrgency(state.minimapData);
                    score += urgency * 1.5f;
                }
                break;

            case "ENGAGE_ENEMY":
                if (state.weapons != null && state.weapons.currentWeapon != null) {
                    float weaponEffectiveness = weaponRecognizer.calculateWeaponEffectiveness(
                            state.weapons.currentWeapon, 300f); // Assume 300m distance
                    score += weaponEffectiveness * 1.0f;
                }
                break;

            case "TAKE_COVER":
                score += state.overallThreat * 0.8f;
                break;

            case "COLLECT_LOOT":
                score += state.opportunityScore * 0.6f;
                break;
        }

        // Performance history adjustment
        PerformanceTracker.PerformanceMetrics metrics = performanceTracker.calculateMetrics();
        Float successRate = metrics.actionSuccessRates.get(actionType);
        if (successRate != null) {
            score *= (0.5f + successRate * 0.5f); // Adjust based on historical success
        }

        return Math.max(0f, Math.min(2.0f, score));
    }

    private void applyNeuralNetworkDecision(DecisionState state, DecisionResult result) {
        try {
            // Convert game state to neural network input
            INDArray input = stateToNeuralInput(state);

            // Get network prediction
            INDArray output = decisionNetwork.output(input);

            // Apply neural network adjustment to confidence
            int bestActionIndex = Nd4j.argMax(output, 1).getInt(0);
            float networkConfidence = output.getFloat(bestActionIndex);

            // Blend with rule-based confidence
            result.confidence = (result.confidence * 0.7f) + (networkConfidence * 0.3f);

            result.componentContributions.put("neural_network", networkConfidence);

        } catch (Exception e) {
            Log.w(TAG, "Neural network decision failed", e);
        }
    }

    private INDArray stateToNeuralInput(DecisionState state) {
        float[] features = new float[50];
        int index = 0;

        // Game context features (10 values)
        if (state.gameContext != null) {
            features[index++] = state.gameContext.resourceLevels.getOrDefault("health", 100f) / 100f;
            features[index++] = state.gameContext.resourceLevels.getOrDefault("shield", 0f) / 100f;
            features[index++] = state.gameContext.resourceLevels.getOrDefault("ammo", 30f) / 100f;
            features[index++] = state.gameContext.playersAlive / 100f;
            features[index++] = state.gameContext.inSafeZone ? 1f : 0f;
            features[index++] = state.gameContext.timeToZoneCollapse / 300f; // Normalize to 5 minutes
            features[index++] = state.gameContext.availableWeapons.size() / 10f;
            features[index++] = state.gameContext.currentRisk.ordinal() / 4f;
            features[index++] = state.gameContext.canEngage ? 1f : 0f;
            features[index++] = state.gameType.ordinal() / 8f;
        } else {
            index += 10; // Skip if no context
        }

        // Player tracking features (10 values)
        if (state.players != null) {
            features[index++] = Math.min(1f, state.players.size() / 20f);

            long enemies = state.players.stream().filter(p -> "enemy".equals(p.teamStatus)).count();
            features[index++] = Math.min(1f, enemies / 10f);

            long teammates = state.players.stream().filter(p -> "teammate".equals(p.teamStatus)).count();
            features[index++] = Math.min(1f, teammates / 5f);

            // Average threat level
            float avgThreat = (float) state.players.stream()
                    .mapToDouble(p -> p.threatLevel)
                    .average().orElse(0.0);
            features[index++] = avgThreat;

            index += 6; // Reserved for future player features
        } else {
            index += 10;
        }

        // Team analysis features (10 values)
        if (state.teamAnalysis != null) {
            features[index++] = state.teamAnalysis.friendlyCount / 10f;
            features[index++] = state.teamAnalysis.enemyCount / 10f;
            features[index++] = state.teamAnalysis.neutralCount / 10f;
            features[index++] = state.teamAnalysis.overallConfidence;
            features[index++] = state.teamAnalysis.detectedMode.ordinal() / 4f;
            index += 5; // Reserved
        } else {
            index += 10;
        }

        // Weapon features (10 values)
        if (state.weapons != null && state.weapons.currentWeapon != null) {
            WeaponRecognizer.WeaponInfo weapon = state.weapons.currentWeapon;
            features[index++] = weapon.type.ordinal() / 9f;
            features[index++] = weapon.currentAmmo / 100f;
            features[index++] = weapon.damage / 100f;
            features[index++] = weapon.confidence;
            features[index++] = weapon.attachments.size() / 5f;
            index += 5; // Reserved
        } else {
            index += 10;
        }

        // Minimap features (10 values)
        if (state.minimapData != null) {
            if (state.minimapData.currentZone != null) {
                features[index++] = state.minimapData.currentZone.radius;
                features[index++] = state.minimapData.currentZone.timeRemaining / 300f;
                features[index++] = state.minimapData.currentZone.phase.ordinal() / 3f;
            } else {
                index += 3;
            }

            if (state.minimapData.playerPosition != null) {
                features[index++] = state.minimapData.playerPosition.isInSafeZone ? 1f : 0f;
                features[index++] = state.minimapData.playerPosition.distanceToZone;
            } else {
                index += 2;
            }

            features[index++] = state.minimapData.markers.size() / 20f;
            index += 4; // Reserved
        } else {
            index += 10;
        }

        return Nd4j.create(features).reshape(1, 50);
    }

    private void applyExperienceAdjustments(DecisionResult result) {
        // Adjust based on recent performance
        if (decisionHistory.size() > 10) {
            String actionType = result.primaryAction.getActionType();

            float recentSuccessRate = calculateRecentSuccessRate(actionType);
            result.confidence *= (0.5f + recentSuccessRate * 0.5f);

            result.componentContributions.put("experience", recentSuccessRate);
        }
    }

    private float calculateRecentSuccessRate(String actionType) {
        int total = 0;
        int successful = 0;

        for (DecisionHistory history : decisionHistory) {
            if (history.actionTaken.getActionType().equals(actionType)) {
                total++;
                if (history.wasSuccessful) successful++;
            }
        }

        return total > 0 ? (float) successful / total : 0.5f;
    }

    private void generateDecisionReasoning(DecisionResult result, DecisionState state) {
        StringBuilder reasoning = new StringBuilder();

        // Primary factors
        reasoning.append("Decision factors: ");

        if (result.priority == DecisionPriority.EMERGENCY) {
            reasoning.append("EMERGENCY situation detected. ");
        }

        if (state.overallThreat > 0.6f) {
            reasoning.append("High threat level (").append(String.format("%.1f", state.overallThreat)).append("). ");
        }

        if (state.opportunityScore > 0.6f) {
            reasoning.append("Good opportunities available (").append(String.format("%.1f", state.opportunityScore)).append("). ");
        }

        // Component contributions
        if (!result.componentContributions.isEmpty()) {
            reasoning.append("Component scores: ");
            for (Map.Entry<String, Float> entry : result.componentContributions.entrySet()) {
                reasoning.append(entry.getKey()).append("=").append(String.format("%.2f", entry.getValue())).append(" ");
            }
        }

        result.reasoning = reasoning.toString();
    }

    private void recordDecision(DecisionResult result) {
        if (currentState != null && result.primaryAction != null) {
            DecisionHistory history = new DecisionHistory(currentState, result.primaryAction);
            decisionHistory.offer(history);

            // Keep only recent history
            while (decisionHistory.size() > 100) {
                decisionHistory.poll();
            }
        }
    }

    public void recordDecisionOutcome(GameAction action, boolean success, float reward) {
        // Find the most recent decision with this action
        for (DecisionHistory history : decisionHistory) {
            if (history.actionTaken.equals(action) && history.reward == 0f) {
                history.wasSuccessful = success;
                history.reward = reward;

                // Train neural network with this outcome
                trainFromOutcome(history);
                break;
            }
        }
    }

    private void trainFromOutcome(DecisionHistory history) {
        try {
            INDArray input = stateToNeuralInput(history.state);

            // Create target based on outcome
            INDArray target = Nd4j.zeros(1, 15);
            int actionIndex = getActionIndex(history.actionTaken.getActionType());

            if (actionIndex >= 0) {
                float targetValue = history.wasSuccessful ?
                        Math.min(1.0f, 0.5f + history.reward) :
                        Math.max(0.0f, 0.5f - Math.abs(history.reward));

                target.putScalar(actionIndex, targetValue);

                // Train the network
                decisionNetwork.fit(input, target);
            }

        } catch (Exception e) {
            Log.w(TAG, "Training from outcome failed", e);
        }
    }

    private int getActionIndex(String actionType) {
        String[] actionTypes = {
                "EMERGENCY_HEAL", "EMERGENCY_RELOAD", "ZONE_ROTATION", "ENGAGE_ENEMY",
                "TAKE_COVER", "COLLECT_LOOT", "REPOSITION", "SCOUT_AREA", "RELOAD",
                "WAIT", "MOVE_LEFT", "MOVE_RIGHT", "JUMP", "SLIDE", "ACTIVATE_POWERUP"
        };

        for (int i = 0; i < actionTypes.length; i++) {
            if (actionTypes[i].equals(actionType)) {
                return i;
            }
        }

        return -1;
    }

    private DecisionResult createEmergencyResult(String reason) {
        DecisionResult result = new DecisionResult();
        result.primaryAction = new GameAction("WAIT", 540, 960, 0.1f, "emergency");
        result.confidence = 0.1f;
        result.reasoning = reason;
        result.priority = DecisionPriority.EMERGENCY;
        return result;
    }

    public PerformanceTracker.PerformanceMetrics getPerformanceMetrics() {
        return performanceTracker.calculateMetrics();
    }

    public List<DecisionHistory> getRecentDecisions(int count) {
        List<DecisionHistory> recent = new ArrayList<>();
        int size = decisionHistory.size();
        int start = Math.max(0, size - count);

        int i = 0;
        for (DecisionHistory history : decisionHistory) {
            if (i >= start) {
                recent.add(history);
            }
            i++;
        }

        return recent;
    }
}
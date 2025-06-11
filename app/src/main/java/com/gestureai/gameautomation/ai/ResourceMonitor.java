package com.gestureai.gameautomation.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.gestureai.gameautomation.GameAction;
import com.gestureai.gameautomation.DetectedObject;
import com.gestureai.gameautomation.PlayerTracker.PlayerData;
import com.gestureai.gameautomation.GameContextAnalyzer.GameContext;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Advanced Resource Monitor for tracking and managing game resources
 * Handles health, mana, ammo, stamina, currency, and consumables across different game types
 */
public class ResourceMonitor {
    private static final String TAG = "ResourceMonitor";

    private Context context;
    private MultiLayerNetwork resourcePredictionNetwork;
    private MultiLayerNetwork consumptionAnalysisNetwork;
    private MultiLayerNetwork optimizationNetwork;

    // Monitoring parameters
    private static final int RESOURCE_STATE_SIZE = 30;
    private static final int CONSUMPTION_STATE_SIZE = 25;
    private static final int OPTIMIZATION_STATE_SIZE = 20;
    private static final int ACTION_SIZE = 10;

    // Resource tracking
    private Map<String, ResourceData> trackedResources;
    private List<ResourceEvent> resourceHistory;
    private Map<String, Float> consumptionPatterns;
    private Map<String, Float> regenerationRates;

    // Thresholds and alerts
    private Map<String, Float> criticalThresholds;
    private Map<String, Float> warningThresholds;
    private List<ResourceAlert> activeAlerts;

    public enum ResourceType {
        HEALTH, MANA, STAMINA, AMMO, SHIELD, ENERGY,
        GOLD, EXPERIENCE, FOOD, MATERIALS, FUEL, OXYGEN
    }

    public enum ResourceAction {
        USE_HEALTH_POTION, USE_MANA_POTION, RELOAD_AMMO, REST_STAMINA,
        GATHER_RESOURCES, CONSERVE_ENERGY, EMERGENCY_HEAL, OPTIMIZE_USAGE,
        FIND_SUPPLIES, MANAGE_CONSUMPTION
    }

    public enum AlertLevel {
        CRITICAL, WARNING, INFO
    }

    public static class ResourceData {
        public ResourceType type;
        public float currentAmount;
        public float maxAmount;
        public float percentage;
        public float regenerationRate;
        public float consumptionRate;
        public boolean isCritical;
        public boolean isRegenerating;
        public long lastUpdated;
        public List<Float> history;

        public ResourceData(ResourceType type, float current, float max) {
            this.type = type;
            this.currentAmount = current;
            this.maxAmount = max;
            this.percentage = max > 0 ? current / max : 0;
            this.history = new ArrayList<>();
            this.lastUpdated = System.currentTimeMillis();
            this.regenerationRate = 0.0f;
            this.consumptionRate = 0.0f;
            this.isCritical = false;
            this.isRegenerating = false;
        }

        public void updateAmount(float newAmount) {
            if (history.size() >= 100) {
                history.remove(0);
            }
            history.add(currentAmount);

            float previousAmount = currentAmount;
            currentAmount = Math.max(0, Math.min(maxAmount, newAmount));
            percentage = maxAmount > 0 ? currentAmount / maxAmount : 0;

            // Calculate rates
            long timeDelta = System.currentTimeMillis() - lastUpdated;
            if (timeDelta > 0) {
                float amountDelta = currentAmount - previousAmount;
                float ratePerSecond = (amountDelta / timeDelta) * 1000;

                if (amountDelta > 0) {
                    regenerationRate = ratePerSecond;
                    consumptionRate = 0;
                    isRegenerating = true;
                } else if (amountDelta < 0) {
                    consumptionRate = Math.abs(ratePerSecond);
                    regenerationRate = 0;
                    isRegenerating = false;
                } else {
                    isRegenerating = false;
                }
            }

            lastUpdated = System.currentTimeMillis();
        }

        public float getProjectedAmount(long futureTimeMs) {
            float timeDeltaSeconds = (futureTimeMs - lastUpdated) / 1000.0f;

            if (isRegenerating) {
                return Math.min(maxAmount, currentAmount + (regenerationRate * timeDeltaSeconds));
            } else {
                return Math.max(0, currentAmount - (consumptionRate * timeDeltaSeconds));
            }
        }

        public boolean isHealthy() {
            return percentage > 0.7f;
        }

        public boolean isLow() {
            return percentage < 0.3f;
        }

        public boolean isCriticalLow() {
            return percentage < 0.15f;
        }
    }

    public static class ResourceEvent {
        public ResourceType resourceType;
        public String eventType; // "consumption", "regeneration", "collection", "usage"
        public float amountChanged;
        public float resultingAmount;
        public String context;
        public long timestamp;

        public ResourceEvent(ResourceType type, String event, float change, float result, String ctx) {
            this.resourceType = type;
            this.eventType = event;
            this.amountChanged = change;
            this.resultingAmount = result;
            this.context = ctx;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class ResourceAlert {
        public ResourceType resourceType;
        public AlertLevel level;
        public String message;
        public float threshold;
        public float currentValue;
        public boolean isActive;
        public long timestamp;

        public ResourceAlert(ResourceType type, AlertLevel level, String msg, float thresh, float current) {
            this.resourceType = type;
            this.level = level;
            this.message = msg;
            this.threshold = thresh;
            this.currentValue = current;
            this.isActive = true;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public ResourceMonitor(Context context) {
        this.context = context;
        this.trackedResources = new HashMap<>();
        this.resourceHistory = new ArrayList<>();
        this.consumptionPatterns = new HashMap<>();
        this.regenerationRates = new HashMap<>();
        this.criticalThresholds = new HashMap<>();
        this.warningThresholds = new HashMap<>();
        this.activeAlerts = new ArrayList<>();

        initializeNetworks();
        setupDefaultThresholds();
        Log.d(TAG, "Resource Monitor initialized");
    }

    private void initializeNetworks() {
        try {
            // Resource prediction network - predicts future resource levels
            MultiLayerConfiguration predictionConf = new NeuralNetConfiguration.Builder()
                    .seed(123)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new org.nd4j.linalg.learning.config.Adam(0.001))
                    .list()
                    .layer(new DenseLayer.Builder().nIn(RESOURCE_STATE_SIZE).nOut(128)
                            .activation(Activation.RELU).build())
                    .layer(new DenseLayer.Builder().nIn(128).nOut(64)
                            .activation(Activation.RELU).build())
                    .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                            .nIn(64).nOut(12) // Predict 12 resource types
                            .activation(Activation.SIGMOID).build())
                    .build();

            resourcePredictionNetwork = new MultiLayerNetwork(predictionConf);
            resourcePredictionNetwork.init();

            // Consumption analysis network - analyzes usage patterns
            MultiLayerConfiguration consumptionConf = new NeuralNetConfiguration.Builder()
                    .seed(456)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new org.nd4j.linalg.learning.config.Adam(0.0015))
                    .list()
                    .layer(new DenseLayer.Builder().nIn(CONSUMPTION_STATE_SIZE).nOut(100)
                            .activation(Activation.RELU).build())
                    .layer(new DenseLayer.Builder().nIn(100).nOut(50)
                            .activation(Activation.RELU).build())
                    .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                            .nIn(50).nOut(ACTION_SIZE)
                            .activation(Activation.SOFTMAX).build())
                    .build();

            consumptionAnalysisNetwork = new MultiLayerNetwork(consumptionConf);
            consumptionAnalysisNetwork.init();

            // Optimization network - suggests optimal resource management
            MultiLayerConfiguration optimizationConf = new NeuralNetConfiguration.Builder()
                    .seed(789)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new org.nd4j.linalg.learning.config.Adam(0.0012))
                    .list()
                    .layer(new DenseLayer.Builder().nIn(OPTIMIZATION_STATE_SIZE).nOut(80)
                            .activation(Activation.RELU).build())
                    .layer(new DenseLayer.Builder().nIn(80).nOut(40)
                            .activation(Activation.RELU).build())
                    .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                            .nIn(40).nOut(ACTION_SIZE)
                            .activation(Activation.SOFTMAX).build())
                    .build();

            optimizationNetwork = new MultiLayerNetwork(optimizationConf);
            optimizationNetwork.init();

            Log.d(TAG, "Resource monitoring neural networks initialized");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing neural networks", e);
        }
    }

    private void setupDefaultThresholds() {
        // Critical thresholds (urgent action needed)
        criticalThresholds.put("HEALTH", 0.15f);
        criticalThresholds.put("MANA", 0.10f);
        criticalThresholds.put("STAMINA", 0.20f);
        criticalThresholds.put("AMMO", 0.25f);
        criticalThresholds.put("SHIELD", 0.15f);
        criticalThresholds.put("ENERGY", 0.10f);
        criticalThresholds.put("FUEL", 0.20f);
        criticalThresholds.put("OXYGEN", 0.30f);

        // Warning thresholds (proactive management)
        warningThresholds.put("HEALTH", 0.40f);
        warningThresholds.put("MANA", 0.30f);
        warningThresholds.put("STAMINA", 0.50f);
        warningThresholds.put("AMMO", 0.50f);
        warningThresholds.put("SHIELD", 0.40f);
        warningThresholds.put("ENERGY", 0.30f);
        warningThresholds.put("FUEL", 0.40f);
        warningThresholds.put("OXYGEN", 0.60f);
    }

    public void updateResource(ResourceType type, float currentAmount, float maxAmount) {
        String key = type.name();
        ResourceData resource = trackedResources.get(key);

        if (resource == null) {
            resource = new ResourceData(type, currentAmount, maxAmount);
            trackedResources.put(key, resource);
        } else {
            float previousAmount = resource.currentAmount;
            resource.updateAmount(currentAmount);
            resource.maxAmount = maxAmount;

            // Record resource event
            if (previousAmount != currentAmount) {
                String eventType = currentAmount > previousAmount ? "regeneration" : "consumption";
                float change = Math.abs(currentAmount - previousAmount);
                ResourceEvent event = new ResourceEvent(type, eventType, change, currentAmount, "automatic_update");
                recordResourceEvent(event);
            }
        }

        // Check for alerts
        checkResourceAlerts(resource);

        // Update consumption patterns
        updateConsumptionPattern(type, resource);
    }

    public GameAction analyzeResourceNeeds(GameContext gameContext) {
        try {
            // Identify most critical resource need
            ResourceAction priorityAction = analyzeCriticalNeeds();

            // Predict future resource states
            Map<ResourceType, Float> predictions = predictFutureResources(30000); // 30 seconds ahead

            // Optimize resource management strategy
            ResourceAction optimizedAction = optimizeResourceStrategy(predictions, gameContext);

            // Select final action based on priority and optimization
            ResourceAction finalAction = selectFinalAction(priorityAction, optimizedAction);

            // Convert to game action
            GameAction action = createActionFromResourceStrategy(finalAction, gameContext);

            Log.d(TAG, String.format("Resource analysis: Priority=%s, Optimized=%s, Final=%s",
                    priorityAction.name(), optimizedAction.name(), finalAction.name()));

            return action;

        } catch (Exception e) {
            Log.e(TAG, "Error in resource analysis", e);
            return createFallbackAction();
        }
    }

    private ResourceAction analyzeCriticalNeeds() {
        try {
            float[] consumptionFeatures = createConsumptionFeatures();
            INDArray input = Nd4j.create(consumptionFeatures).reshape(1, CONSUMPTION_STATE_SIZE);
            INDArray output = consumptionAnalysisNetwork.output(input);

            int bestActionIndex = Nd4j.argMax(output, 1).getInt(0);
            ResourceAction[] actions = ResourceAction.values();

            if (bestActionIndex < actions.length) {
                return actions[bestActionIndex];
            } else {
                return selectCriticalActionFallback();
            }

        } catch (Exception e) {
            Log.w(TAG, "Error analyzing critical needs", e);
            return selectCriticalActionFallback();
        }
    }

    private Map<ResourceType, Float> predictFutureResources(long futureTimeMs) {
        Map<ResourceType, Float> predictions = new HashMap<>();

        try {
            float[] predictionFeatures = createPredictionFeatures();
            INDArray input = Nd4j.create(predictionFeatures).reshape(1, RESOURCE_STATE_SIZE);
            INDArray output = resourcePredictionNetwork.output(input);

            ResourceType[] types = ResourceType.values();
            for (int i = 0; i < Math.min(types.length, output.columns()); i++) {
                float predictedPercentage = output.getFloat(i);
                predictions.put(types[i], predictedPercentage);
            }

        } catch (Exception e) {
            Log.w(TAG, "Error predicting future resources", e);
            // Fallback to mathematical prediction
            for (ResourceData resource : trackedResources.values()) {
                float predicted = resource.getProjectedAmount(System.currentTimeMillis() + futureTimeMs);
                float percentage = resource.maxAmount > 0 ? predicted / resource.maxAmount : 0;
                predictions.put(resource.type, percentage);
            }
        }

        return predictions;
    }

    private ResourceAction optimizeResourceStrategy(Map<ResourceType, Float> predictions, GameContext gameContext) {
        try {
            float[] optimizationFeatures = createOptimizationFeatures(predictions, gameContext);
            INDArray input = Nd4j.create(optimizationFeatures).reshape(1, OPTIMIZATION_STATE_SIZE);
            INDArray output = optimizationNetwork.output(input);

            int bestActionIndex = Nd4j.argMax(output, 1).getInt(0);
            ResourceAction[] actions = ResourceAction.values();

            if (bestActionIndex < actions.length) {
                return actions[bestActionIndex];
            } else {
                return selectOptimizationFallback(predictions);
            }

        } catch (Exception e) {
            Log.w(TAG, "Error optimizing resource strategy", e);
            return selectOptimizationFallback(predictions);
        }
    }

    private float[] createConsumptionFeatures() {
        float[] features = new float[CONSUMPTION_STATE_SIZE];

        // Current resource levels (0-11)
        ResourceType[] types = ResourceType.values();
        for (int i = 0; i < Math.min(12, types.length); i++) {
            ResourceData resource = trackedResources.get(types[i].name());
            features[i] = resource != null ? resource.percentage : 0.0f;
        }

        // Consumption rates (12-17)
        features[12] = getConsumptionRate(ResourceType.HEALTH);
        features[13] = getConsumptionRate(ResourceType.MANA);
        features[14] = getConsumptionRate(ResourceType.STAMINA);
        features[15] = getConsumptionRate(ResourceType.AMMO);
        features[16] = getConsumptionRate(ResourceType.SHIELD);
        features[17] = getConsumptionRate(ResourceType.ENERGY);

        // Alert counts (18-20)
        features[18] = countAlertsByLevel(AlertLevel.CRITICAL) / 5.0f;
        features[19] = countAlertsByLevel(AlertLevel.WARNING) / 10.0f;
        features[20] = activeAlerts.size() / 15.0f;

        // Time factors (21-24)
        features[21] = (System.currentTimeMillis() % 60000) / 60000.0f; // Minute cycle
        features[22] = calculateOverallResourceHealth();
        features[23] = calculateResourceTrend();
        features[24] = calculateUrgencyFactor();

        return features;
    }

    private float[] createPredictionFeatures() {
        float[] features = new float[RESOURCE_STATE_SIZE];

        // Current states (0-11)
        ResourceType[] types = ResourceType.values();
        for (int i = 0; i < Math.min(12, types.length); i++) {
            ResourceData resource = trackedResources.get(types[i].name());
            features[i] = resource != null ? resource.percentage : 0.0f;
        }

        // Regeneration rates (12-17)
        features[12] = getRegenerationRate(ResourceType.HEALTH);
        features[13] = getRegenerationRate(ResourceType.MANA);
        features[14] = getRegenerationRate(ResourceType.STAMINA);
        features[15] = getRegenerationRate(ResourceType.SHIELD);
        features[16] = getRegenerationRate(ResourceType.ENERGY);
        features[17] = getRegenerationRate(ResourceType.FUEL);

        // Consumption rates (18-23)
        features[18] = getConsumptionRate(ResourceType.HEALTH);
        features[19] = getConsumptionRate(ResourceType.MANA);
        features[20] = getConsumptionRate(ResourceType.STAMINA);
        features[21] = getConsumptionRate(ResourceType.AMMO);
        features[22] = getConsumptionRate(ResourceType.ENERGY);
        features[23] = getConsumptionRate(ResourceType.FUEL);

        // Historical patterns (24-29)
        features[24] = calculateAverageConsumption(ResourceType.HEALTH);
        features[25] = calculateAverageConsumption(ResourceType.MANA);
        features[26] = calculateResourceVolatility();
        features[27] = calculateResourceEfficiency();
        features[28] = (System.currentTimeMillis() % 300000) / 300000.0f; // 5-minute cycle
        features[29] = calculatePredictionConfidence();

        return features;
    }

    private float[] createOptimizationFeatures(Map<ResourceType, Float> predictions, GameContext gameContext) {
        float[] features = new float[OPTIMIZATION_STATE_SIZE];

        // Current resource states (0-5)
        features[0] = getResourcePercentage(ResourceType.HEALTH);
        features[1] = getResourcePercentage(ResourceType.MANA);
        features[2] = getResourcePercentage(ResourceType.STAMINA);
        features[3] = getResourcePercentage(ResourceType.AMMO);
        features[4] = getResourcePercentage(ResourceType.SHIELD);
        features[5] = getResourcePercentage(ResourceType.ENERGY);

        // Predicted future states (6-11)
        features[6] = predictions.getOrDefault(ResourceType.HEALTH, 0.0f);
        features[7] = predictions.getOrDefault(ResourceType.MANA, 0.0f);
        features[8] = predictions.getOrDefault(ResourceType.STAMINA, 0.0f);
        features[9] = predictions.getOrDefault(ResourceType.AMMO, 0.0f);
        features[10] = predictions.getOrDefault(ResourceType.SHIELD, 0.0f);
        features[11] = predictions.getOrDefault(ResourceType.ENERGY, 0.0f);

        // Game context (12-17)
        features[12] = gameContext != null ? gameContext.currentRisk.ordinal() / 4.0f : 0.5f;
        features[13] = gameContext != null ? (gameContext.timeToZoneCollapse / 300.0f) : 0.0f;
        features[14] = gameContext != null ? (gameContext.playersAlive / 100.0f) : 0.5f;
        features[15] = gameContext != null ? (gameContext.resourceLevels.size() / 10.0f) : 0.0f;
        features[16] = calculateCombatIntensity();
        features[17] = calculateResourceDemand();

        // Optimization factors (18-19)
        features[18] = calculateOptimizationPotential();
        features[19] = calculateResourceBalance();

        return features;
    }

    private ResourceAction selectFinalAction(ResourceAction priority, ResourceAction optimized) {
        // Priority system: Critical needs override optimization

        // Check if any resource is in critical state
        for (ResourceData resource : trackedResources.values()) {
            if (resource.isCriticalLow()) {
                switch (resource.type) {
                    case HEALTH:
                        if (priority == ResourceAction.USE_HEALTH_POTION || priority == ResourceAction.EMERGENCY_HEAL) {
                            return priority;
                        }
                        break;
                    case MANA:
                        if (priority == ResourceAction.USE_MANA_POTION) {
                            return priority;
                        }
                        break;
                    case AMMO:
                        if (priority == ResourceAction.RELOAD_AMMO) {
                            return priority;
                        }
                        break;
                    case STAMINA:
                        if (priority == ResourceAction.REST_STAMINA) {
                            return priority;
                        }
                        break;
                }
            }
        }

        // If no critical needs, use optimized action
        return optimized;
    }

    private GameAction createActionFromResourceStrategy(ResourceAction strategy, GameContext gameContext) {
        switch (strategy) {
            case USE_HEALTH_POTION:
                return new GameAction("LONG_PRESS", 150, 1600, 0.9f, "health_potion");

            case USE_MANA_POTION:
                return new GameAction("LONG_PRESS", 250, 1600, 0.8f, "mana_potion");

            case RELOAD_AMMO:
                return new GameAction("LONG_PRESS", 540, 1600, 0.85f, "reload");

            case REST_STAMINA:
                return new GameAction("WAIT", 540, 960, 0.7f, "rest");

            case GATHER_RESOURCES:
                return new GameAction("SWIPE", 540, 960, 0.6f, "gather");

            case CONSERVE_ENERGY:
                return new GameAction("WAIT", 540, 960, 0.5f, "conserve");

            case EMERGENCY_HEAL:
                return new GameAction("DOUBLE_TAP", 150, 1600, 0.95f, "emergency_heal");

            case OPTIMIZE_USAGE:
                return new GameAction("TAP", 400, 1700, 0.6f, "optimize");

            case FIND_SUPPLIES:
                return new GameAction("SWIPE_UP", 540, 960, 0.7f, "search_supplies");

            case MANAGE_CONSUMPTION:
                return new GameAction("TAP", 800, 1700, 0.5f, "manage");

            default:
                return new GameAction("WAIT", 540, 960, 0.4f, "resource_wait");
        }
    }

    // Helper methods for calculations
    private float getConsumptionRate(ResourceType type) {
        ResourceData resource = trackedResources.get(type.name());
        return resource != null ? resource.consumptionRate / 10.0f : 0.0f; // Normalized
    }

    private float getRegenerationRate(ResourceType type) {
        ResourceData resource = trackedResources.get(type.name());
        return resource != null ? resource.regenerationRate / 10.0f : 0.0f; // Normalized
    }

    private float getResourcePercentage(ResourceType type) {
        ResourceData resource = trackedResources.get(type.name());
        return resource != null ? resource.percentage : 0.0f;
    }

    private int countAlertsByLevel(AlertLevel level) {
        int count = 0;
        for (ResourceAlert alert : activeAlerts) {
            if (alert.level == level && alert.isActive) {
                count++;
            }
        }
        return count;
    }

    private float calculateOverallResourceHealth() {
        if (trackedResources.isEmpty()) return 0.5f;

        float totalHealth = 0.0f;
        for (ResourceData resource : trackedResources.values()) {
            totalHealth += resource.percentage;
        }
        return totalHealth / trackedResources.size();
    }

    private float calculateResourceTrend() {
        float trend = 0.0f;
        int count = 0;

        for (ResourceData resource : trackedResources.values()) {
            if (resource.history.size() >= 2) {
                float recent = resource.currentAmount;
                float previous = resource.history.get(resource.history.size() - 1);
                trend += (recent - previous) / resource.maxAmount;
                count++;
            }
        }

        return count > 0 ? trend / count : 0.0f;
    }

    private float calculateUrgencyFactor() {
        float urgency = 0.0f;

        for (ResourceData resource : trackedResources.values()) {
            if (resource.isCriticalLow()) {
                urgency += 0.5f;
            } else if (resource.isLow()) {
                urgency += 0.2f;
            }
        }

        return Math.min(1.0f, urgency);
    }

    private float calculateAverageConsumption(ResourceType type) {
        String pattern = type.name() + "_consumption";
        return consumptionPatterns.getOrDefault(pattern, 0.0f);
    }

    private float calculateResourceVolatility() {
        float volatility = 0.0f;
        int count = 0;

        for (ResourceData resource : trackedResources.values()) {
            if (resource.history.size() >= 5) {
                float variance = 0.0f;
                float mean = 0.0f;

                // Calculate mean
                for (float value : resource.history) {
                    mean += value;
                }
                mean /= resource.history.size();

                // Calculate variance
                for (float value : resource.history) {
                    variance += Math.pow(value - mean, 2);
                }
                variance /= resource.history.size();

                volatility += Math.sqrt(variance) / resource.maxAmount;
                count++;
            }
        }

        return count > 0 ? volatility / count : 0.0f;
    }

    private float calculateResourceEfficiency() {
        // Simplified efficiency calculation
        return calculateOverallResourceHealth() * (1.0f - calculateResourceVolatility());
    }

    private float calculatePredictionConfidence() {
        // Based on data history and pattern stability
        float confidence = 0.5f; // Base confidence

        for (ResourceData resource : trackedResources.values()) {
            if (resource.history.size() >= 10) {
                confidence += 0.05f; // More data = higher confidence
            }
        }

        return Math.min(1.0f, confidence);
    }

    private float calculateCombatIntensity() {
        // Calculate based on resource consumption patterns
        float intensity = 0.0f;
        intensity += getConsumptionRate(ResourceType.HEALTH) * 2.0f;
        intensity += getConsumptionRate(ResourceType.AMMO) * 1.5f;
        intensity += getConsumptionRate(ResourceType.STAMINA) * 1.0f;
        return Math.min(1.0f, intensity);
    }

    private float calculateResourceDemand() {
        return Math.min(1.0f, countAlertsByLevel(AlertLevel.CRITICAL) * 0.3f +
                countAlertsByLevel(AlertLevel.WARNING) * 0.1f);
    }

    private float calculateOptimizationPotential() {
        float potential = 0.0f;

        for (ResourceData resource : trackedResources.values()) {
            if (resource.percentage > 0.8f && resource.consumptionRate < 0.1f) {
                potential += 0.2f; // Resource is abundant and not being used
            }
        }

        return Math.min(1.0f, potential);
    }

    private float calculateResourceBalance() {
        if (trackedResources.isEmpty()) return 0.5f;

        float maxPercentage = 0.0f;
        float minPercentage = 1.0f;

        for (ResourceData resource : trackedResources.values()) {
            maxPercentage = Math.max(maxPercentage, resource.percentage);
            minPercentage = Math.min(minPercentage, resource.percentage);
        }

        return 1.0f - (maxPercentage - minPercentage); // Higher balance = smaller difference
    }

    private ResourceAction selectCriticalActionFallback() {
        // Find most critical resource
        ResourceData mostCritical = null;
        float lowestPercentage = 1.0f;

        for (ResourceData resource : trackedResources.values()) {
            if (resource.percentage < lowestPercentage) {
                lowestPercentage = resource.percentage;
                mostCritical = resource;
            }
        }

        if (mostCritical != null) {
            switch (mostCritical.type) {
                case HEALTH:
                    return lowestPercentage < 0.2f ? ResourceAction.EMERGENCY_HEAL : ResourceAction.USE_HEALTH_POTION;
                case MANA:
                    return ResourceAction.USE_MANA_POTION;
                case AMMO:
                    return ResourceAction.RELOAD_AMMO;
                case STAMINA:
                    return ResourceAction.REST_STAMINA;
                default:
                    return ResourceAction.GATHER_RESOURCES;
            }
        }

        return ResourceAction.OPTIMIZE_USAGE;
    }

    private ResourceAction selectOptimizationFallback(Map<ResourceType, Float> predictions) {
        // Find resource that will be most problematic in the future
        ResourceType mostProblematic = null;
        float lowestPredicted = 1.0f;

        for (Map.Entry<ResourceType, Float> entry : predictions.entrySet()) {
            if (entry.getValue() < lowestPredicted) {
                lowestPredicted = entry.getValue();
                mostProblematic = entry.getKey();
            }
        }

        if (mostProblematic != null && lowestPredicted < 0.3f) {
            switch (mostProblematic) {
                case HEALTH:
                    return ResourceAction.USE_HEALTH_POTION;
                case MANA:
                    return ResourceAction.USE_MANA_POTION;
                case AMMO:
                    return ResourceAction.RELOAD_AMMO;
                case STAMINA:
                    return ResourceAction.REST_STAMINA;
                default:
                    return ResourceAction.GATHER_RESOURCES;
            }
        }

        return ResourceAction.OPTIMIZE_USAGE;
    }

    private GameAction createFallbackAction() {
        // Emergency fallback based on most critical resource
        ResourceData mostCritical = null;
        float lowestPercentage = 1.0f;

        for (ResourceData resource : trackedResources.values()) {
            if (resource.percentage < lowestPercentage) {
                lowestPercentage = resource.percentage;
                mostCritical = resource;
            }
        }

        if (mostCritical != null && lowestPercentage < 0.2f) {
            switch (mostCritical.type) {
                case HEALTH:
                    return new GameAction("LONG_PRESS", 150, 1600, 0.9f, "emergency_health");
                case AMMO:
                    return new GameAction("LONG_PRESS", 540, 1600, 0.8f, "emergency_reload");
                case MANA:
                    return new GameAction("LONG_PRESS", 250, 1600, 0.8f, "emergency_mana");
            }
        }

        return new GameAction("WAIT", 540, 960, 0.5f, "resource_fallback");
    }

    private void checkResourceAlerts(ResourceData resource) {
        String key = resource.type.name();
        float criticalThresh = criticalThresholds.getOrDefault(key, 0.15f);
        float warningThresh = warningThresholds.getOrDefault(key, 0.40f);

        // Remove old alerts for this resource
        activeAlerts.removeIf(alert -> alert.resourceType == resource.type);

        if (resource.percentage <= criticalThresh) {
            ResourceAlert alert = new ResourceAlert(
                    resource.type,
                    AlertLevel.CRITICAL,
                    String.format("%s critically low: %.1f%%", resource.type.name(), resource.percentage * 100),
                    criticalThresh,
                    resource.percentage
            );
            activeAlerts.add(alert);
            resource.isCritical = true;
        } else if (resource.percentage <= warningThresh) {
            ResourceAlert alert = new ResourceAlert(
                    resource.type,
                    AlertLevel.WARNING,
                    String.format("%s low: %.1f%%", resource.type.name(), resource.percentage * 100),
                    warningThresh,
                    resource.percentage
            );
            activeAlerts.add(alert);
            resource.isCritical = false;
        } else {
            resource.isCritical = false;
        }
    }

    private void updateConsumptionPattern(ResourceType type, ResourceData resource) {
        String consumptionKey = type.name() + "_consumption";
        String regenerationKey = type.name() + "_regeneration";

        // Update consumption pattern
        float currentConsumption = consumptionPatterns.getOrDefault(consumptionKey, 0.0f);
        float newConsumption = (currentConsumption * 0.9f) + (resource.consumptionRate * 0.1f); // Exponential moving average
        consumptionPatterns.put(consumptionKey, newConsumption);

        // Update regeneration rate
        float currentRegeneration = regenerationRates.getOrDefault(regenerationKey, 0.0f);
        float newRegeneration = (currentRegeneration * 0.9f) + (resource.regenerationRate * 0.1f);
        regenerationRates.put(regenerationKey, newRegeneration);
    }

    private void recordResourceEvent(ResourceEvent event) {
        resourceHistory.add(event);

        // Limit history size
        if (resourceHistory.size() > 1000) {
            resourceHistory.remove(0);
        }

        Log.d(TAG, String.format("Resource event: %s %s %.2f -> %.2f",
                event.resourceType.name(), event.eventType, event.amountChanged, event.resultingAmount));
    }

    // Public interface methods
    public Map<String, ResourceData> getTrackedResources() {
        return new HashMap<>(trackedResources);
    }

    public List<ResourceAlert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts);
    }

    public List<ResourceEvent> getResourceHistory() {
        return new ArrayList<>(resourceHistory);
    }

    public void setThreshold(ResourceType type, AlertLevel level, float threshold) {
        String key = type.name();
        if (level == AlertLevel.CRITICAL) {
            criticalThresholds.put(key, threshold);
        } else if (level == AlertLevel.WARNING) {
            warningThresholds.put(key, threshold);
        }
    }

    public float getResourceHealth() {
        return calculateOverallResourceHealth();
    }

    public Map<String, Float> getConsumptionPatterns() {
        return new HashMap<>(consumptionPatterns);
    }

    public void clearAlerts() {
        activeAlerts.clear();
    }



    public float getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        return (float) (used * 100.0 / max);
    }
    public float getCPUUsage() {
        try {
            // Read CPU stats from /proc/stat
            java.io.RandomAccessFile reader = new java.io.RandomAccessFile("/proc/stat", "r");
            String load = reader.readLine();
            reader.close();

            String[] toks = load.split(" +");
            long idle1 = Long.parseLong(toks[4]);
            long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            // Wait and read again
            try {
                Thread.sleep(360);
            } catch (Exception e) {}

            reader = new java.io.RandomAccessFile("/proc/stat", "r");
            load = reader.readLine();
            reader.close();

            toks = load.split(" +");
            long idle2 = Long.parseLong(toks[4]);
            long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            // Calculate CPU usage percentage
            return (float)(cpu2 - cpu1) * 100 / ((cpu2 + idle2) - (cpu1 + idle1));

        } catch (Exception e) {
            Log.e(TAG, "Error reading CPU usage", e);
            // Fallback: use ActivityManager for app-specific CPU
            android.app.ActivityManager am = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            return getCurrentAppCPUUsage(am);
        }
    }

    private float getCurrentAppCPUUsage(android.app.ActivityManager am) {
        try {
            // Get current process CPU usage
            android.os.Debug.MemoryInfo[] memInfos = am.getProcessMemoryInfo(new int[]{android.os.Process.myPid()});
            if (memInfos.length > 0) {
                // Approximate CPU usage based on memory pressure and active processing
                Runtime runtime = Runtime.getRuntime();
                long totalMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                float memoryUsage = (float)(totalMemory - freeMemory) / totalMemory;

                // Estimate CPU usage (this is an approximation)
                return Math.min(100f, memoryUsage * 60f);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting app CPU usage", e);
        }
        return 0f;
    }
    private long lastFrameTime = 0;
    private List<Long> frameTimes = new ArrayList<>();

    public int getCurrentFPS() {
        long currentTime = System.nanoTime();

        if (lastFrameTime != 0) {
            long frameTime = currentTime - lastFrameTime;
            frameTimes.add(frameTime);

            // Keep only last 60 frame times
            if (frameTimes.size() > 60) {
                frameTimes.remove(0);
            }

            // Calculate average FPS from actual frame times
            if (frameTimes.size() > 10) {
                long avgFrameTime = frameTimes.stream().mapToLong(Long::longValue).sum() / frameTimes.size();
                return (int)(1000000000.0f / avgFrameTime); // Convert nanoseconds to FPS as int
            }
        }

        lastFrameTime = currentTime;
        return 0; // No data yet
    }
    public int getNetworkLatency() {
        try {
            long startTime = System.currentTimeMillis();

            // Ping Google DNS
            java.net.InetAddress address = java.net.InetAddress.getByName("8.8.8.8");
            boolean reachable = address.isReachable(3000);

            long endTime = System.currentTimeMillis();

            if (reachable) {
                return (int)(endTime - startTime);
            } else {
                return -1; // Network unreachable
            }

        } catch (Exception e) {
            Log.e(TAG, "Error measuring network latency", e);
            return -1;
        }
    }
}
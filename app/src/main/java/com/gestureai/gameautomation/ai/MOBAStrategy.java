package com.gestureai.gameautomation.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.gestureai.gameautomation.GameAction;
import com.gestureai.gameautomation.DetectedObject;
import com.gestureai.gameautomation.PlayerTracker.PlayerData;
import com.gestureai.gameautomation.GameContextAnalyzer.GameContext;
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
 * Advanced MOBA Strategy for multiplayer online battle arena games
 * Handles complex decision making for lane management, team fights, objectives, and resource optimization
 */
public class MOBAStrategy {
    private static final String TAG = "MOBAStrategy";

    private Context context;
    private MultiLayerNetwork laneStrategyNetwork;
    private MultiLayerNetwork teamFightNetwork;
    private MultiLayerNetwork objectiveNetwork;
    private MultiLayerNetwork itemBuildNetwork;

    // Strategy parameters
    private static final int LANE_STATE_SIZE = 30;
    private static final int TEAMFIGHT_STATE_SIZE = 35;
    private static final int OBJECTIVE_STATE_SIZE = 25;
    private static final int ITEM_STATE_SIZE = 20;
    private static final int ACTION_SIZE = 15;
    private int reactionDelayMs = 250; // Default reaction time
    private float targetAccuracy = 0.8f; // Default accuracy target

    // Performance tracking
    private Map<String, Float> strategyPerformance;
    private List<TeamFightResult> teamFightHistory;
    private Map<String, Integer> objectiveResults;
    private float winRate = 0.0f;
    private int gamesPlayed = 0;

    public enum MOBAAction {
        LAST_HIT_MINION, HARASS_ENEMY, WARD_POSITION, GANK_LANE,
        RECALL_BASE, FARM_JUNGLE, ENGAGE_TEAMFIGHT, DISENGAGE_FIGHT,
        TAKE_OBJECTIVE, DEFEND_OBJECTIVE, ROAM_MAP, SPLIT_PUSH,
        BUY_ITEMS, USE_ABILITY, RETREAT_LANE
    }

    public enum GamePhase {
        EARLY_GAME, MID_GAME, LATE_GAME
    }

    public enum PlayerRole {
        CARRY, SUPPORT, TANK, ASSASSIN, MAGE, MARKSMAN
    }

    public static class MOBAState {
        // Player status
        public float[] playerPosition = new float[2];
        public int level;
        public int gold;
        public float healthPercent;
        public float manaPercent;
        public boolean[] abilitiesReady = new boolean[4];
        public List<String> items;
        public PlayerRole role;
        private int reactionDelayMs = 250; // Default reaction time
        private float targetAccuracy = 0.8f; // Default accuracy target

        // Lane information
        public String currentLane; // "top", "mid", "bot", "jungle"
        public int minionCount;
        public int enemyMinionCount;
        public List<PlayerData> laneEnemies;
        public List<PlayerData> teammates;
        public boolean laneWarded;
        public float lanePressure;

        // Team information
        public int teamKills;
        public int teamDeaths;
        public int enemyTeamKills;
        public int enemyTeamDeaths;
        public Map<String, Boolean> objectiveStatus; // dragon, baron, towers
        public float teamGoldAdvantage;

        // Game state
        public int gameTime; // in seconds
        public GamePhase phase;
        public boolean inTeamFight;
        public int enemiesNearby;
        public int alliesNearby;
        public float mapControl;

        // Vision and map awareness
        public int wardsPlaced;
        public int enemyWardsDetected;
        public boolean hasVision;
        public List<DetectedObject> visibleEnemies;

        public MOBAState() {
            this.items = new ArrayList<>();
            this.laneEnemies = new ArrayList<>();
            this.teammates = new ArrayList<>();
            this.objectiveStatus = new HashMap<>();
            this.visibleEnemies = new ArrayList<>();
            this.role = PlayerRole.CARRY;
            this.currentLane = "mid";
        }
    }

    public static class TeamFightResult {
        public boolean won;
        public int alliesAlive;
        public int enemiesAlive;
        public String initiatedBy;
        public String objective;
        public long timestamp;

        public TeamFightResult(boolean won, int allies, int enemies, String initiator, String obj) {
            this.won = won;
            this.alliesAlive = allies;
            this.enemiesAlive = enemies;
            this.initiatedBy = initiator;
            this.objective = obj;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public MOBAStrategy(Context context) {
        this.context = context;
        this.strategyPerformance = new HashMap<>();
        this.teamFightHistory = new ArrayList<>();
        this.objectiveResults = new HashMap<>();

        initializeNetworks();
        loadStrategyData();
        Log.d(TAG, "MOBA Strategy initialized");
    }

    private void initializeNetworks() {
        try {
            // Lane strategy network - for laning phase decisions
            MultiLayerConfiguration laneConf = new NeuralNetConfiguration.Builder()
                    .seed(123)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new org.nd4j.linalg.learning.config.Adam(0.001))
                    .list()
                    .layer(new DenseLayer.Builder().nIn(LANE_STATE_SIZE).nOut(128)
                            .activation(Activation.RELU).build())
                    .layer(new DenseLayer.Builder().nIn(128).nOut(64)
                            .activation(Activation.RELU).build())
                    .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                            .nIn(64).nOut(ACTION_SIZE)
                            .activation(Activation.SOFTMAX).build())
                    .build();

            laneStrategyNetwork = new MultiLayerNetwork(laneConf);
            if (laneStrategyNetwork != null) {
                try { 
                    laneStrategyNetwork.init(); 
                } catch (Exception e) { 
                    Log.w(TAG, "Failed to initialize lane strategy network", e); 
                    laneStrategyNetwork = null; 
                }
            }

            // Team fight network - for team fight decisions
            MultiLayerConfiguration teamFightConf = new NeuralNetConfiguration.Builder()
                    .seed(456)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new org.nd4j.linalg.learning.config.Adam(0.0015))
                    .list()
                    .layer(new DenseLayer.Builder().nIn(TEAMFIGHT_STATE_SIZE).nOut(150)
                            .activation(Activation.RELU).build())
                    .layer(new DenseLayer.Builder().nIn(150).nOut(75)
                            .activation(Activation.RELU).build())
                    .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                            .nIn(75).nOut(ACTION_SIZE)
                            .activation(Activation.SOFTMAX).build())
                    .build();

            teamFightNetwork = new MultiLayerNetwork(teamFightConf);
            if (teamFightNetwork != null) {
                try { 
                    teamFightNetwork.init(); 
                } catch (Exception e) { 
                    Log.w(TAG, "Failed to initialize team fight network", e); 
                    teamFightNetwork = null; 
                }
            }

            // Objective network - for dragon/baron/tower decisions
            MultiLayerConfiguration objectiveConf = new NeuralNetConfiguration.Builder()
                    .seed(789)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new org.nd4j.linalg.learning.config.Adam(0.0012))
                    .list()
                    .layer(new DenseLayer.Builder().nIn(OBJECTIVE_STATE_SIZE).nOut(100)
                            .activation(Activation.RELU).build())
                    .layer(new DenseLayer.Builder().nIn(100).nOut(50)
                            .activation(Activation.RELU).build())
                    .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                            .nIn(50).nOut(ACTION_SIZE)
                            .activation(Activation.SOFTMAX).build())
                    .build();

            objectiveNetwork = new MultiLayerNetwork(objectiveConf);
            if (objectiveNetwork != null) {
                try { 
                    objectiveNetwork.init(); 
                } catch (Exception e) { 
                    Log.w(TAG, "Failed to initialize objective network", e); 
                    objectiveNetwork = null; 
                }
            }

            // Item build network - for item purchase decisions
            MultiLayerConfiguration itemConf = new NeuralNetConfiguration.Builder()
                    .seed(101112)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new org.nd4j.linalg.learning.config.Adam(0.001))
                    .list()
                    .layer(new DenseLayer.Builder().nIn(ITEM_STATE_SIZE).nOut(80)
                            .activation(Activation.RELU).build())
                    .layer(new DenseLayer.Builder().nIn(80).nOut(40)
                            .activation(Activation.RELU).build())
                    .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                            .nIn(40).nOut(10) // 10 item categories
                            .activation(Activation.SOFTMAX).build())
                    .build();

            itemBuildNetwork = new MultiLayerNetwork(itemConf);
            if (itemBuildNetwork != null) {
                try { 
                    itemBuildNetwork.init(); 
                } catch (Exception e) { 
                    Log.w(TAG, "Failed to initialize item build network", e); 
                    itemBuildNetwork = null; 
                }
            }

            Log.d(TAG, "MOBA neural networks initialized");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing neural networks", e);
        }
    }

    public GameAction analyzeMOBAScenario(MOBAState state, GameContext gameContext) {
        try {
            // Determine current priority based on game state
            MOBAAction strategicAction;

            if (state.inTeamFight) {
                strategicAction = analyzeTeamFight(state);
            } else if (isObjectiveAvailable(state)) {
                strategicAction = analyzeObjective(state);
            } else {
                strategicAction = analyzeLaning(state);
            }

            // Convert strategic decision to game action
            GameAction action = createActionFromStrategy(strategicAction, state);

            // Log decision reasoning
            Log.d(TAG, String.format("MOBA Decision: Action=%s, Phase=%s, Lane=%s, Level=%d",
                    strategicAction.name(), state.phase.name(), state.currentLane, state.level));

            return action;

        } catch (Exception e) {
            Log.e(TAG, "Error in MOBA analysis", e);
            return createFallbackAction(state);
        }
    }

    private MOBAAction analyzeLaning(MOBAState state) {
        try {
            float[] laneFeatures = createLaneFeatures(state);
            INDArray input = Nd4j.create(laneFeatures).reshape(1, LANE_STATE_SIZE);
            INDArray output = laneStrategyNetwork.output(input);

            int bestActionIndex = Nd4j.argMax(output, 1).getInt(0);
            MOBAAction[] actions = MOBAAction.values();

            if (bestActionIndex < actions.length) {
                return actions[bestActionIndex];
            } else {
                return MOBAAction.LAST_HIT_MINION;
            }

        } catch (Exception e) {
            Log.w(TAG, "Error in lane analysis", e);
            return selectFallbackLaneAction(state);
        }
    }

    private MOBAAction analyzeTeamFight(MOBAState state) {
        try {
            float[] teamFightFeatures = createTeamFightFeatures(state);
            INDArray input = Nd4j.create(teamFightFeatures).reshape(1, TEAMFIGHT_STATE_SIZE);
            INDArray output = teamFightNetwork.output(input);

            int bestActionIndex = Nd4j.argMax(output, 1).getInt(0);
            MOBAAction[] actions = MOBAAction.values();

            if (bestActionIndex < actions.length) {
                return actions[bestActionIndex];
            } else {
                return MOBAAction.ENGAGE_TEAMFIGHT;
            }

        } catch (Exception e) {
            Log.w(TAG, "Error in team fight analysis", e);
            return selectFallbackTeamFightAction(state);
        }
    }

    private MOBAAction analyzeObjective(MOBAState state) {
        try {
            float[] objectiveFeatures = createObjectiveFeatures(state);
            INDArray input = Nd4j.create(objectiveFeatures).reshape(1, OBJECTIVE_STATE_SIZE);
            INDArray output = objectiveNetwork.output(input);

            int bestActionIndex = Nd4j.argMax(output, 1).getInt(0);
            MOBAAction[] actions = MOBAAction.values();

            if (bestActionIndex < actions.length) {
                return actions[bestActionIndex];
            } else {
                return MOBAAction.TAKE_OBJECTIVE;
            }

        } catch (Exception e) {
            Log.w(TAG, "Error in objective analysis", e);
            return selectFallbackObjectiveAction(state);
        }
    }

    private float[] createLaneFeatures(MOBAState state) {
        float[] features = new float[LANE_STATE_SIZE];

        // Player status (0-7)
        features[0] = state.level / 18.0f;
        features[1] = state.gold / 10000.0f;
        features[2] = state.healthPercent;
        features[3] = state.manaPercent;
        features[4] = countReadyAbilities(state.abilitiesReady) / 4.0f;
        features[5] = state.items.size() / 6.0f;
        features[6] = getRoleValue(state.role);
        features[7] = getLaneValue(state.currentLane);

        // Lane state (8-15)
        features[8] = state.minionCount / 10.0f;
        features[9] = state.enemyMinionCount / 10.0f;
        features[10] = state.laneEnemies.size() / 3.0f;
        features[11] = state.teammates.size() / 5.0f;
        features[12] = state.laneWarded ? 1.0f : 0.0f;
        features[13] = state.lanePressure;
        features[14] = state.enemiesNearby / 5.0f;
        features[15] = state.alliesNearby / 5.0f;

        // Game state (16-23)
        features[16] = state.gameTime / 3600.0f; // Normalized to 1 hour
        features[17] = getPhaseValue(state.phase);
        features[18] = state.teamKills / 50.0f;
        features[19] = state.teamDeaths / 50.0f;
        features[20] = state.enemyTeamKills / 50.0f;
        features[21] = state.enemyTeamDeaths / 50.0f;
        features[22] = state.teamGoldAdvantage / 10000.0f;
        features[23] = state.mapControl;

        // Vision and awareness (24-29)
        features[24] = state.wardsPlaced / 20.0f;
        features[25] = state.enemyWardsDetected / 20.0f;
        features[26] = state.hasVision ? 1.0f : 0.0f;
        features[27] = state.visibleEnemies.size() / 5.0f;
        features[28] = calculateLaneThreat(state);
        features[29] = winRate;

        return features;
    }

    private float[] createTeamFightFeatures(MOBAState state) {
        float[] features = new float[TEAMFIGHT_STATE_SIZE];

        // Include all lane features first
        float[] laneFeatures = createLaneFeatures(state);
        System.arraycopy(laneFeatures, 0, features, 0, LANE_STATE_SIZE);

        // Additional team fight specific features (30-34)
        features[30] = calculateTeamFightPower(state);
        features[31] = calculateEnemyTeamPower(state);
        features[32] = calculatePositionalAdvantage(state);
        features[33] = calculateUltimateReadiness(state);
        features[34] = calculateEngagementTiming(state);

        return features;
    }

    private float[] createObjectiveFeatures(MOBAState state) {
        float[] features = new float[OBJECTIVE_STATE_SIZE];

        // Core game state (0-14)
        features[0] = state.level / 18.0f;
        features[1] = state.teamKills / 50.0f;
        features[2] = state.teamDeaths / 50.0f;
        features[3] = state.teamGoldAdvantage / 10000.0f;
        features[4] = state.gameTime / 3600.0f;
        features[5] = getPhaseValue(state.phase);
        features[6] = state.alliesNearby / 5.0f;
        features[7] = state.enemiesNearby / 5.0f;
        features[8] = state.hasVision ? 1.0f : 0.0f;
        features[9] = calculateTeamFightPower(state);
        features[10] = calculateEnemyTeamPower(state);
        features[11] = state.mapControl;
        features[12] = getObjectiveValue("dragon", state);
        features[13] = getObjectiveValue("baron", state);
        features[14] = getObjectiveValue("tower", state);

        // Positioning and timing (15-24)
        features[15] = calculateObjectiveDistance(state, "dragon");
        features[16] = calculateObjectiveDistance(state, "baron");
        features[17] = calculateObjectiveRisk(state);
        features[18] = calculateObjectiveReward(state);
        features[19] = state.healthPercent;
        features[20] = state.manaPercent;
        features[21] = countReadyAbilities(state.abilitiesReady) / 4.0f;
        features[22] = calculateEnemyRotationTime(state);
        features[23] = calculateAllyRotationTime(state);
        features[24] = winRate;

        return features;
    }
    private int[] findNearestMinion(MOBAState state) {
        // Return default minion position if no specific logic is available
        // In a real implementation, this would scan the game screen for minions
        int defaultX = (int) (state.playerPosition[0] + 100); // 100 pixels ahead
        int defaultY = (int) state.playerPosition[1];
        return new int[]{defaultX, defaultY};
    }

    private GameAction createActionFromStrategy(MOBAAction strategy, MOBAState state) {
        switch (strategy) {
            case LAST_HIT_MINION:
                // Get minion coordinates first
                int[] minionPos = findNearestMinion(state);
                return new GameAction("TAP", minionPos[0], minionPos[1], 0.8f, "last_hit");
            case HARASS_ENEMY:
                if (!state.laneEnemies.isEmpty()) {
                    PlayerData target = state.laneEnemies.get(0);
                    return new GameAction("TAP", (int)target.position[0], (int)target.position[1], 0.7f, "harass");
                }
                return new GameAction("WAIT", 540, 960, 0.3f, "no_enemy");

            case WARD_POSITION:
                int[] wardPos = calculateWardPosition(state);
                return new GameAction("LONG_PRESS", wardPos[0], wardPos[1], 0.6f, "ward");

            case GANK_LANE:
                int[] gankPos = calculateGankPosition(state);
                return new GameAction("MOVE", gankPos[0], gankPos[1], 0.9f, "gank");

            case RECALL_BASE:
                return new GameAction("LONG_PRESS", 100, 100, 0.8f, "recall");

            case FARM_JUNGLE:
                int[] junglePos = calculateJunglePosition(state);
                return new GameAction("MOVE", junglePos[0], junglePos[1], 0.7f, "jungle");

            case ENGAGE_TEAMFIGHT:
                int[] engagePos = calculateEngagePosition(state);
                return new GameAction("TAP", engagePos[0], engagePos[1], 0.95f, "engage");

            case DISENGAGE_FIGHT:
                int[] retreatPos = calculateRetreatPosition(state);
                return new GameAction("MOVE", retreatPos[0], retreatPos[1], 0.9f, "disengage");

            case TAKE_OBJECTIVE:
                int[] objPos = calculateObjectivePosition(state);
                return new GameAction("MOVE", objPos[0], objPos[1], 0.85f, "objective");

            case DEFEND_OBJECTIVE:
                int[] defensePos = calculateDefensePosition(state);
                return new GameAction("MOVE", defensePos[0], defensePos[1], 0.8f, "defend");

            case ROAM_MAP:
                int[] roamPos = calculateRoamPosition(state);
                return new GameAction("MOVE", roamPos[0], roamPos[1], 0.7f, "roam");

            case SPLIT_PUSH:
                int[] splitPos = calculateSplitPushPosition(state);
                return new GameAction("MOVE", splitPos[0], splitPos[1], 0.8f, "split_push");

            case BUY_ITEMS:
                return new GameAction("TAP", 900, 1600, 0.6f, "shop");

            case USE_ABILITY:
                int[] abilityPos = calculateAbilityTarget(state);
                return new GameAction("TAP", abilityPos[0], abilityPos[1], 0.8f, "ability");

            default:
                return new GameAction("WAIT", 540, 960, 0.4f, "strategic_wait");
        }
    }

    // Helper methods for feature calculations
    private int countReadyAbilities(boolean[] abilities) {
        int count = 0;
        for (boolean ready : abilities) {
            if (ready) count++;
        }
        return count;
    }

    private float getRoleValue(PlayerRole role) {
        switch (role) {
            case CARRY: return 0.1f;
            case SUPPORT: return 0.2f;
            case TANK: return 0.3f;
            case ASSASSIN: return 0.4f;
            case MAGE: return 0.5f;
            case MARKSMAN: return 0.6f;
            default: return 0.3f;
        }
    }

    private float getLaneValue(String lane) {
        switch (lane.toLowerCase()) {
            case "top": return 0.2f;
            case "mid": return 0.4f;
            case "bot": return 0.6f;
            case "jungle": return 0.8f;
            default: return 0.4f;
        }
    }

    private float getPhaseValue(GamePhase phase) {
        switch (phase) {
            case EARLY_GAME: return 0.2f;
            case MID_GAME: return 0.5f;
            case LATE_GAME: return 0.8f;
            default: return 0.5f;
        }
    }

    private float calculateLaneThreat(MOBAState state) {
        float threat = 0.0f;
        threat += state.laneEnemies.size() * 0.3f;
        threat += (1.0f - state.healthPercent) * 0.3f;
        threat += (state.laneWarded ? 0.0f : 0.2f);
        threat += (state.enemiesNearby > state.alliesNearby) ? 0.2f : 0.0f;
        return Math.min(1.0f, threat);
    }

    private float calculateTeamFightPower(MOBAState state) {
        float power = 0.0f;
        power += state.level / 18.0f * 0.3f;
        power += state.healthPercent * 0.2f;
        power += state.manaPercent * 0.1f;
        power += (countReadyAbilities(state.abilitiesReady) / 4.0f) * 0.2f;
        power += (state.items.size() / 6.0f) * 0.2f;
        return Math.min(1.0f, power);
    }

    private float calculateEnemyTeamPower(MOBAState state) {
        // Simplified enemy power calculation based on visible information
        float estimatedPower = 0.5f; // Base assumption
        if (state.teamGoldAdvantage > 0) {
            estimatedPower -= state.teamGoldAdvantage / 20000.0f;
        } else {
            estimatedPower += Math.abs(state.teamGoldAdvantage) / 20000.0f;
        }
        return Math.max(0.1f, Math.min(1.0f, estimatedPower));
    }

    private float calculatePositionalAdvantage(MOBAState state) {
        float advantage = 0.5f;
        if (state.alliesNearby > state.enemiesNearby) advantage += 0.2f;
        if (state.hasVision) advantage += 0.1f;
        if (state.healthPercent > 0.7f) advantage += 0.1f;
        return Math.min(1.0f, advantage);
    }

    private float calculateUltimateReadiness(MOBAState state) {
        return state.abilitiesReady[3] ? 1.0f : 0.0f; // Assuming ultimate is index 3
    }

    private float calculateEngagementTiming(MOBAState state) {
        float timing = 0.5f;
        if (state.phase == GamePhase.LATE_GAME) timing += 0.2f;
        if (state.teamGoldAdvantage > 2000) timing += 0.2f;
        if (state.alliesNearby >= 4) timing += 0.1f;
        return Math.min(1.0f, timing);
    }

    private boolean isObjectiveAvailable(MOBAState state) {
        return state.gameTime > 300 && // After 5 minutes
                (state.alliesNearby >= 3 || state.phase == GamePhase.LATE_GAME);
    }

    private float getObjectiveValue(String objective, MOBAState state) {
        Boolean status = state.objectiveStatus.get(objective);
        return (status != null && status) ? 1.0f : 0.0f;
    }

    private float calculateObjectiveDistance(MOBAState state, String objective) {
        // Simplified distance calculation
        Map<String, float[]> objectivePositions = new HashMap<>();
        objectivePositions.put("dragon", new float[]{400, 1200});
        objectivePositions.put("baron", new float[]{400, 600});
        objectivePositions.put("tower", new float[]{state.playerPosition[0], state.playerPosition[1] - 200});

        float[] objPos = objectivePositions.getOrDefault(objective, new float[]{540, 960});
        float distance = (float)Math.sqrt(
                Math.pow(state.playerPosition[0] - objPos[0], 2) +
                        Math.pow(state.playerPosition[1] - objPos[1], 2)
        );
        return Math.min(1.0f, distance / 1000.0f);
    }

    private float calculateObjectiveRisk(MOBAState state) {
        float risk = 0.3f; // Base risk
        if (state.enemiesNearby > state.alliesNearby) risk += 0.4f;
        if (!state.hasVision) risk += 0.2f;
        if (state.healthPercent < 0.5f) risk += 0.1f;
        return Math.min(1.0f, risk);
    }

    private float calculateObjectiveReward(MOBAState state) {
        float reward = 0.5f; // Base reward
        if (state.phase == GamePhase.LATE_GAME) reward += 0.3f;
        if (state.teamGoldAdvantage > 0) reward += 0.1f;
        if (state.gameTime > 1200) reward += 0.1f; // After 20 minutes
        return Math.min(1.0f, reward);
    }

    private float calculateEnemyRotationTime(MOBAState state) {
        return 0.3f; // Simplified calculation
    }

    private float calculateAllyRotationTime(MOBAState state) {
        return Math.min(1.0f, state.alliesNearby / 5.0f);
    }



    private int[] calculateWardPosition(MOBAState state) {
        // Strategic ward positions based on lane
        Map<String, int[]> wardPositions = new HashMap<>();
        wardPositions.put("top", new int[]{300, 400});
        wardPositions.put("mid", new int[]{500, 500});
        wardPositions.put("bot", new int[]{700, 1400});
        wardPositions.put("jungle", new int[]{400, 800});

        return wardPositions.getOrDefault(state.currentLane, new int[]{540, 960});
    }

    private int[] calculateGankPosition(MOBAState state) {
        return new int[]{(int)state.playerPosition[0] + 100, (int)state.playerPosition[1]};
    }

    private int[] calculateJunglePosition(MOBAState state) {
        return new int[]{350, 800}; // Typical jungle position
    }

    private int[] calculateEngagePosition(MOBAState state) {
        if (!state.laneEnemies.isEmpty()) {
            PlayerData target = state.laneEnemies.get(0);
            return new int[]{(int)target.position[0], (int)target.position[1]};
        }
        return new int[]{540, 960};
    }

    private int[] calculateRetreatPosition(MOBAState state) {
        // Move toward base/safe position
        return new int[]{(int)state.playerPosition[0] - 200, (int)state.playerPosition[1] + 200};
    }

    private int[] calculateObjectivePosition(MOBAState state) {
        if (state.gameTime > 1200) { // Late game - Baron
            return new int[]{400, 600};
        } else { // Early/Mid game - Dragon
            return new int[]{400, 1200};
        }
    }

    private int[] calculateDefensePosition(MOBAState state) {
        return new int[]{(int)state.playerPosition[0], (int)state.playerPosition[1] - 100};
    }

    private int[] calculateRoamPosition(MOBAState state) {
        // Move to help other lanes
        if ("mid".equals(state.currentLane)) {
            return new int[]{400, 1400}; // Go bot
        } else {
            return new int[]{540, 960}; // Go mid
        }
    }

    private int[] calculateSplitPushPosition(MOBAState state) {
        return new int[]{200, 200}; // Push top lane
    }

    private int[] calculateAbilityTarget(MOBAState state) {
        if (!state.laneEnemies.isEmpty()) {
            PlayerData target = state.laneEnemies.get(0);
            return new int[]{(int)target.position[0], (int)target.position[1]};
        }
        return new int[]{540, 960};
    }

    // Fallback action methods
    private MOBAAction selectFallbackLaneAction(MOBAState state) {
        if (state.healthPercent < 0.3f) return MOBAAction.RECALL_BASE;
        if (state.minionCount > 5) return MOBAAction.LAST_HIT_MINION;
        if (!state.laneEnemies.isEmpty() && state.healthPercent > 0.6f) return MOBAAction.HARASS_ENEMY;
        return MOBAAction.FARM_JUNGLE;
    }

    private MOBAAction selectFallbackTeamFightAction(MOBAState state) {
        if (state.healthPercent < 0.4f) return MOBAAction.DISENGAGE_FIGHT;
        if (state.alliesNearby > state.enemiesNearby) return MOBAAction.ENGAGE_TEAMFIGHT;
        return MOBAAction.ROAM_MAP;
    }

    private MOBAAction selectFallbackObjectiveAction(MOBAState state) {
        if (state.alliesNearby >= 3) return MOBAAction.TAKE_OBJECTIVE;
        return MOBAAction.DEFEND_OBJECTIVE;
    }

    private GameAction createFallbackAction(MOBAState state) {
        if (state.healthPercent < 0.3f) {
            return new GameAction("LONG_PRESS", 100, 100, 0.8f, "emergency_recall");
        }
        return new GameAction("WAIT", 540, 960, 0.5f, "fallback_wait");
    }

    private void loadStrategyData() {
        // Initialize strategy performance tracking
        for (MOBAAction action : MOBAAction.values()) {
            strategyPerformance.put(action.name(), 0.5f);
        }

        // Initialize objective tracking
        objectiveResults.put("dragon_taken", 0);
        objectiveResults.put("baron_taken", 0);
        objectiveResults.put("towers_taken", 0);
        objectiveResults.put("teamfights_won", 0);
    }

    public void recordGameResult(boolean won, MOBAState finalState) {
        gamesPlayed++;

        if (won) {
            winRate = (winRate * (gamesPlayed - 1) + 1.0f) / gamesPlayed;
        } else {
            winRate = (winRate * (gamesPlayed - 1)) / gamesPlayed;
        }

        Log.d(TAG, String.format("Game result recorded: Won=%b, Win Rate=%.2f", won, winRate));
    }

    public void recordTeamFightResult(boolean won, int alliesAlive, int enemiesAlive, String initiator, String objective) {
        TeamFightResult result = new TeamFightResult(won, alliesAlive, enemiesAlive, initiator, objective);
        teamFightHistory.add(result);

        // Update objective tracking
        if (won && objective != null) {
            String key = objective.toLowerCase() + "_taken";
            objectiveResults.put(key, objectiveResults.getOrDefault(key, 0) + 1);
        }

        // Update team fight win rate
        String key = "teamfights_won";
        if (won) {
            objectiveResults.put(key, objectiveResults.getOrDefault(key, 0) + 1);
        }

        // Limit history size
        if (teamFightHistory.size() > 500) {
            teamFightHistory.remove(0);
        }

        Log.d(TAG, String.format("Team fight result: Won=%b, Allies=%d, Enemies=%d", won, alliesAlive, enemiesAlive));
    }

    public float getWinRate() {
        return winRate;
    }

    public Map<String, Float> getStrategyPerformance() {
        return new HashMap<>(strategyPerformance);
    }

    public Map<String, Integer> getObjectiveResults() {
        return new HashMap<>(objectiveResults);
    }

    public List<TeamFightResult> getTeamFightHistory() {
        return new ArrayList<>(teamFightHistory);
    }
    public void setTeamFightFocusEnabled(boolean enabled) {
        Log.d(TAG, "Team fight focus " + (enabled ? "enabled" : "disabled"));
    }

    public void setMapAwarenessEnabled(boolean enabled) {
        Log.d(TAG, "Map awareness " + (enabled ? "enabled" : "disabled"));
    }

    public void setStrategyType(String type) {
        Log.d(TAG, "Strategy type set to: " + type);
    }
    public void setAggressionLevel(float level) {
        Log.d(TAG, "Aggression level set to: " + level);
    }
    public void setReactionTime(int timeMs) {
        this.reactionDelayMs = timeMs;
        Log.d(TAG, "Reaction time set to: " + timeMs + "ms");
    }

    public void setAccuracyTarget(float target) {
        this.targetAccuracy = target;
        updateAimingParameters(target);
        Log.d(TAG, "Accuracy target set to: " + target);
    }

    private void updateAimingParameters(float accuracy) {
        // Modify neural network parameters based on accuracy target
        if (laneStrategyNetwork != null) {
            // Adjust learning rate based on target accuracy
            float learningRate = accuracy > 0.9f ? 0.0005f : 0.001f;
            // Network parameter updates would go here
        }
    }
    
}
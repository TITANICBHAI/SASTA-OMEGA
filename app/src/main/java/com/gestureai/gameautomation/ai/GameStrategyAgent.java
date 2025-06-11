package com.gestureai.gameautomation.ai;

import android.content.Context;
import android.util.Log;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import com.gestureai.gameautomation.GameAction;
import com.gestureai.gameautomation.synchronization.AIComponentSynchronizer;
import com.gestureai.gameautomation.messaging.EventBus;
import com.gestureai.gameautomation.GameContextAnalyzer;
import com.gestureai.gameautomation.GameState;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;

public class GameStrategyAgent {
    private static final String TAG = "GameStrategyAgent";
    private static volatile GameStrategyAgent instance;
    private static final Object lock = new Object();

    // Universal game state - works for any game
    private static final int STATE_SIZE = 16; // Expanded for any game type
    private static final int ACTION_SIZE = 8; // Universal actions

    private MultiLayerNetwork dqn;
    private ReplayBuffer replayBuffer;
    private volatile float epsilon = 0.3f; // Exploration rate
    private volatile float epsilonDecay = 0.995f;
    private volatile float minEpsilon = 0.01f;
    private final Random random = new Random();
    private Context context;
    private Object externalDQN;
    private Object externalPPO;

    // Universal action mapping - works for any game
    private static final String[] UNIVERSAL_ACTIONS = {
            "TAP", "SWIPE_UP", "SWIPE_DOWN", "SWIPE_LEFT",
            "SWIPE_RIGHT", "LONG_PRESS", "DOUBLE_TAP", "WAIT"
    };

    private GameStrategyAgent(Context context) {
        this.context = context;
        initializeNetwork();
        replayBuffer = new ReplayBuffer(10000);
        Log.d(TAG, "Universal Game Strategy Agent initialized");
    }
    
    public static GameStrategyAgent getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new GameStrategyAgent(context);
                }
            }
        }
        return instance;
    }
    
    public static GameStrategyAgent getInstance() {
        if (instance == null) {
            Log.w(TAG, "GameStrategyAgent not initialized with context");
            return null;
        }
        return instance;
    }
    
    public synchronized void cleanup() {
        try {
            if (dqn != null) {
                // Clear neural network resources
                dqn.clear();
                dqn = null;
            }
            
            if (replayBuffer != null) {
                replayBuffer.clear();
                replayBuffer = null;
            }
            
            // Reset parameters
            epsilon = 0.3f;
            epsilonDecay = 0.995f;
            
            Log.d(TAG, "GameStrategyAgent cleaned up");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during GameStrategyAgent cleanup", e);
        }
    }

    // Game-specific strategies
    public enum GameType { BATTLE_ROYALE, MOBA, FPS, STRATEGY, ARCADE }

    // Enhanced strategy base class
    public abstract class BaseStrategy {
        protected Context context;
        protected MultiLayerNetwork strategyNetwork;

        public BaseStrategy(Context context) {
            this.context = context;
            initializeStrategyNetwork();
        }

        protected abstract GameAction analyzeGameContext(UniversalGameState state);
        protected abstract float calculateRiskLevel(UniversalGameState state);
        protected abstract boolean shouldEngage(UniversalGameState state);

        protected void initializeStrategyNetwork() {
            // Strategy-specific neural network initialization
        }
    }

    // Battle Royale Strategy Implementation
    public class BattleRoyaleStrategy extends BaseStrategy {
        private static final int BR_STATE_SIZE = 20;
        private static final float ZONE_THREAT_THRESHOLD = 0.7f;

        public BattleRoyaleStrategy(Context context) {
            super(context);
        }

        @Override
        protected void initializeStrategyNetwork() {
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .seed(456)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new org.nd4j.linalg.learning.config.Adam(0.0005))
                    .list()
                    .layer(new DenseLayer.Builder().nIn(BR_STATE_SIZE).nOut(128)
                            .activation(Activation.RELU).build())
                    .layer(new DenseLayer.Builder().nIn(128).nOut(64)
                            .activation(Activation.RELU).build())
                    .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                            .nIn(64).nOut(8) // BR actions: rotate, engage, loot, heal, etc.
                            .activation(Activation.SOFTMAX).build())
                    .build();

            strategyNetwork = new MultiLayerNetwork(conf);
            strategyNetwork.init();
        }

        @Override
        public GameAction analyzeGameContext(UniversalGameState state) {
            // Battle Royale specific analysis
            float zoneRisk = calculateZoneRisk(state);
            float enemyThreat = calculateEnemyThreat(state);
            float lootPriority = calculateLootPriority(state);

            INDArray stateArray = createBRStateArray(state, zoneRisk, enemyThreat, lootPriority);
            INDArray actionProbabilities = strategyNetwork.output(stateArray);

            int bestAction = Nd4j.argMax(actionProbabilities, 1).getInt(0);

            return createBattleRoyaleAction(bestAction, state, zoneRisk, enemyThreat);
        }

        private float calculateZoneRisk(UniversalGameState state) {
            float distanceToZone = state.threatLevel;
            float timeToCollapse = state.timeInGame;

            if (timeToCollapse < 30f && distanceToZone > 0.5f) {
                return 0.9f; // High risk - need to rotate immediately
            } else if (timeToCollapse < 60f && distanceToZone > 0.3f) {
                return 0.6f; // Medium risk - start moving
            }
            return 0.2f; // Low risk - can focus on other activities
        }

        private float calculateEnemyThreat(UniversalGameState state) {
            int nearbyEnemies = (int)(state.objectCount * 0.3f);
            float avgDistance = state.opportunityLevel;

            if (nearbyEnemies > 2 && avgDistance < 0.3f) {
                return 0.8f; // High threat - multiple close enemies
            } else if (nearbyEnemies > 0 && avgDistance < 0.5f) {
                return 0.5f; // Medium threat
            }
            return 0.1f; // Low threat
        }

        private float calculateLootPriority(UniversalGameState state) {
            float currentEquipmentLevel = state.healthLevel;

            if (currentEquipmentLevel < 0.3f) {
                return 0.9f; // Desperately need better gear
            } else if (currentEquipmentLevel < 0.6f) {
                return 0.5f; // Could use upgrades
            }
            return 0.2f; // Well equipped
        }

        private INDArray createBRStateArray(UniversalGameState state, float zoneRisk,
                                            float enemyThreat, float lootPriority) {
            float[] brState = new float[BR_STATE_SIZE];

            // Copy base state
            float[] baseStateArray = gameStateToArray(state).toFloatVector();
            System.arraycopy(baseStateArray, 0, brState, 0, Math.min(baseStateArray.length, STATE_SIZE));

            // Add BR-specific features
            brState[STATE_SIZE] = zoneRisk;
            brState[STATE_SIZE + 1] = enemyThreat;
            brState[STATE_SIZE + 2] = lootPriority;
            brState[STATE_SIZE + 3] = state.gameScore / 10f; // Survival time normalized

            return Nd4j.create(brState).reshape(1, BR_STATE_SIZE);
        }

        private GameAction createBattleRoyaleAction(int actionIndex, UniversalGameState state,
                                                    float zoneRisk, float enemyThreat) {
            String[] brActions = {"ROTATE_TO_ZONE", "ENGAGE_ENEMY", "SEEK_COVER",
                    "LOOT_AREA", "HEAL_UP", "THIRD_PARTY", "REPOSITION", "WAIT"};

            String actionType = brActions[actionIndex];
            int x = state.playerX;
            int y = state.playerY;

            // Calculate action-specific coordinates
            switch (actionType) {
                case "ROTATE_TO_ZONE":
                    x = 540; // Default screen center X
                    y = 960; // Default screen center Y
                    break;
                case "ENGAGE_ENEMY":
                    x = state.nearestObstacleX;
                    y = state.nearestObstacleY;
                    break;
                case "SEEK_COVER":
                    x = state.playerX + (random.nextBoolean() ? 100 : -100);
                    y = state.playerY;
                    break;
            }

            float confidence = 0.8f + (zoneRisk + enemyThreat) * 0.1f;
            return new GameAction(actionType, x, y, confidence, "battle_royale_ai");
        }

        @Override
        protected float calculateRiskLevel(UniversalGameState state) {
            return Math.max(calculateZoneRisk(state), calculateEnemyThreat(state));
        }

        @Override
        protected boolean shouldEngage(UniversalGameState state) {
            float zoneRisk = calculateZoneRisk(state);
            float enemyThreat = calculateEnemyThreat(state);

            // Don't engage if zone is closing and we're far
            if (zoneRisk > ZONE_THREAT_THRESHOLD) return false;

            // Don't engage if outnumbered significantly
            if (enemyThreat > 0.7f) return false;

            // Engage if we have good position and equipment
            return state.healthLevel > 0.5f && enemyThreat < 0.5f;
        }
    }

    // MOBA Strategy Implementation
    public class MOBAStrategy extends BaseStrategy {
        private static final int MOBA_STATE_SIZE = 24;

        public MOBAStrategy(Context context) {
            super(context);
        }

        @Override
        public GameAction analyzeGameContext(UniversalGameState state) {
            float farmPriority = calculateFarmPriority(state);
            float teamFightPotential = calculateTeamFightPotential(state);
            float objectiveValue = calculateObjectiveValue(state);

            INDArray stateArray = createMOBAStateArray(state, farmPriority, teamFightPotential, objectiveValue);
            INDArray actionProbabilities = strategyNetwork.output(stateArray);

            int bestAction = Nd4j.argMax(actionProbabilities, 1).getInt(0);
            return createMOBAAction(bestAction, state);
        }

        private float calculateFarmPriority(UniversalGameState state) {
            float gameTime = state.timeInGame / 1800f; // 30 minutes normalized
            return Math.max(0.1f, 1.0f - gameTime);
        }

        private float calculateTeamFightPotential(UniversalGameState state) {
            int nearbyAllies = (int)(state.objectCount * 0.4f);
            return nearbyAllies > 2 ? 0.8f : 0.3f;
        }

        private float calculateObjectiveValue(UniversalGameState state) {
            return state.opportunityLevel;
        }

        private INDArray createMOBAStateArray(UniversalGameState state, float farmPriority,
                                              float teamFightPotential, float objectiveValue) {
            float[] mobaState = new float[MOBA_STATE_SIZE];

            // Convert basic game state to array
            float[] baseState = {
                state.playerX / 1080f, state.playerY / 2340f, // Normalized coordinates
                state.healthLevel, state.difficultyLevel, state.gameScore / 1000f,
                state.powerUpActive ? 1f : 0f, state.objectCount / 10f,
                state.timeInGame / 3600f, state.opportunityLevel,
                state.nearestObstacleX / 1080f, state.nearestObstacleY / 2340f,
                state.consecutiveSuccess / 10f, state.gameSpeed / 5f,
                state.isMultiplayer ? 1f : 0f, state.adaptiveMode ? 1f : 0f,
                state.emergencyStop ? 1f : 0f
            };
            System.arraycopy(baseState, 0, mobaState, 0, Math.min(baseState.length, STATE_SIZE));

            mobaState[STATE_SIZE] = farmPriority;
            mobaState[STATE_SIZE + 1] = teamFightPotential;
            mobaState[STATE_SIZE + 2] = objectiveValue;
            mobaState[STATE_SIZE + 3] = state.gameScore / 1000f; // Gold normalized
            mobaState[STATE_SIZE + 4] = state.healthLevel; // Health/Mana
            mobaState[STATE_SIZE + 5] = state.powerUpActive ? 1f : 0f; // Ultimate ready
            mobaState[STATE_SIZE + 6] = state.difficultyLevel; // Enemy strength
            mobaState[STATE_SIZE + 7] = state.consecutiveSuccess / 10f; // KDA ratio
            
            return Nd4j.create(mobaState).reshape(1, MOBA_STATE_SIZE);
        }
        
        private GameAction createMOBAAction(int actionIndex, UniversalGameState state) {
            String[] mobaActions = {"FARM_MINIONS", "ENGAGE_TEAMFIGHT", "TAKE_OBJECTIVE", 
                    "RECALL_BASE", "WARD_AREA", "GANK_LANE", "DEFEND_TOWER", "ROAM"};
                    
            String actionType = mobaActions[actionIndex];
            int x = state.playerX;
            int y = state.playerY;
            
            // Calculate action-specific coordinates
            switch (actionType) {
                case "FARM_MINIONS":
                    x = 540; // Default screen center X
                    y = 1280; // Default lower third Y
                    break;
                case "ENGAGE_TEAMFIGHT":
                    x = state.nearestObstacleX;
                    y = state.nearestObstacleY;
                    break;
                case "TAKE_OBJECTIVE":
                    x = 810; // Default 3/4 screen width
                    y = 480; // Default 1/4 screen height
                    break;
                case "WARD_AREA":
                    x = state.playerX + (random.nextBoolean() ? 150 : -150);
                    y = state.playerY + (random.nextBoolean() ? 100 : -100);
                    break;
            }
            
            float confidence = 0.7f + state.opportunityLevel * 0.2f;
            return new GameAction(actionType, x, y, confidence, "moba_ai");
        }

        @Override
        protected float calculateRiskLevel(UniversalGameState state) {
            float enemyThreat = state.objectCount > 3 ? 0.8f : 0.3f;
            float healthRisk = state.healthLevel < 0.3f ? 0.7f : 0.2f;
            return Math.max(enemyThreat, healthRisk);
        }

        @Override
        protected boolean shouldEngage(UniversalGameState state) {
            int nearbyAllies = (int)(state.objectCount * 0.4f);
            int nearbyEnemies = (int)(state.objectCount * 0.6f);
            
            // Don't engage if outnumbered
            if (nearbyEnemies > nearbyAllies + 1) return false;
            
            // Don't engage if low on health/mana
            if (state.healthLevel < 0.4f) return false;
            
            // Engage if we have numbers advantage
            return nearbyAllies >= nearbyEnemies && state.powerUpActive;
        }
    }

    // FPS Strategy Implementation  
    public class FPSStrategy extends BaseStrategy {
        private static final int FPS_STATE_SIZE = 20;
        private static final float AIM_PRECISION_THRESHOLD = 0.8f;

        public FPSStrategy(Context context) {
            super(context);
        }

        @Override
        protected void initializeStrategyNetwork() {
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .seed(789)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new org.nd4j.linalg.learning.config.Adam(0.0003))
                    .list()
                    .layer(new DenseLayer.Builder().nIn(FPS_STATE_SIZE).nOut(128)
                            .activation(Activation.RELU).build())
                    .layer(new DenseLayer.Builder().nIn(128).nOut(64)
                            .activation(Activation.RELU).build())
                    .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                            .nIn(64).nOut(6) // FPS actions: aim, shoot, reload, move, cover, sprint
                            .activation(Activation.SOFTMAX).build())
                    .build();

            strategyNetwork = new MultiLayerNetwork(conf);
            strategyNetwork.init();
        }

        @Override
        public GameAction analyzeGameContext(UniversalGameState state) {
            float weaponEffectiveness = calculateWeaponEffectiveness(state);
            float coverAvailability = calculateCoverAvailability(state);
            float enemyDistance = calculateEnemyDistance(state);

            INDArray stateArray = createFPSStateArray(state, weaponEffectiveness, coverAvailability, enemyDistance);
            INDArray actionProbabilities = strategyNetwork.output(stateArray);

            int bestAction = Nd4j.argMax(actionProbabilities, 1).getInt(0);
            return createFPSAction(bestAction, state, weaponEffectiveness, enemyDistance);
        }

        private float calculateWeaponEffectiveness(UniversalGameState state) {
            float ammoRatio = state.powerLevel;
            float weaponPower = state.speedLevel;
            
            return Math.min(1.0f, (ammoRatio + weaponPower) / 2.0f);
        }

        private float calculateCoverAvailability(UniversalGameState state) {
            float distanceToNearestCover = state.threatLevel;
            return Math.max(0.1f, 1.0f - distanceToNearestCover);
        }

        private float calculateEnemyDistance(UniversalGameState state) {
            if (state.nearestObstacleX == 0 && state.nearestObstacleY == 0) {
                return 1.0f; // No enemies detected
            }
            
            float distance = (float) Math.sqrt(
                Math.pow(state.nearestObstacleX - state.playerX, 2) + 
                Math.pow(state.nearestObstacleY - state.playerY, 2)
            );
            
            return Math.min(1.0f, distance / 500f); // Normalize to screen size
        }

        private INDArray createFPSStateArray(UniversalGameState state, float weaponEffectiveness, 
                                           float coverAvailability, float enemyDistance) {
            float[] fpsState = new float[FPS_STATE_SIZE];

            // Convert basic game state to array
            float[] baseState = {
                state.playerX / 1080f, state.playerY / 2340f, // Normalized coordinates
                state.healthLevel, state.difficultyLevel, state.gameScore / 1000f,
                state.powerUpActive ? 1f : 0f, state.objectCount / 10f,
                state.timeInGame / 3600f, state.opportunityLevel,
                state.nearestObstacleX / 1080f, state.nearestObstacleY / 2340f,
                state.consecutiveSuccess / 10f, state.gameSpeed / 5f,
                state.isMultiplayer ? 1f : 0f, state.adaptiveMode ? 1f : 0f,
                state.emergencyStop ? 1f : 0f
            };
            System.arraycopy(baseState, 0, fpsState, 0, Math.min(baseState.length, STATE_SIZE));

            // Add FPS-specific features
            fpsState[STATE_SIZE] = weaponEffectiveness;
            fpsState[STATE_SIZE + 1] = coverAvailability;
            fpsState[STATE_SIZE + 2] = enemyDistance;
            fpsState[STATE_SIZE + 3] = state.gameScore / 100f; // Kill/death ratio normalized

            return Nd4j.create(fpsState).reshape(1, FPS_STATE_SIZE);
        }

        private GameAction createFPSAction(int actionIndex, UniversalGameState state, 
                                         float weaponEffectiveness, float enemyDistance) {
            String[] fpsActions = {"AIM_SHOOT", "TAKE_COVER", "RELOAD_WEAPON", 
                    "MOVE_POSITION", "SPRINT_ESCAPE", "GRENADE_THROW"};

            String actionType = fpsActions[actionIndex];
            int x = state.playerX;
            int y = state.playerY;

            // Calculate action-specific coordinates
            switch (actionType) {
                case "AIM_SHOOT":
                    x = state.nearestObstacleX;
                    y = state.nearestObstacleY;
                    break;
                case "TAKE_COVER":
                    x = state.playerX + (state.nearestObstacleX > state.playerX ? -100 : 100);
                    y = state.playerY;
                    break;
                case "MOVE_POSITION":
                    x = state.playerX + random.nextInt(200) - 100;
                    y = state.playerY + random.nextInt(200) - 100;
                    break;
                case "SPRINT_ESCAPE":
                    x = state.playerX + (state.nearestObstacleX > state.playerX ? -200 : 200);
                    y = state.playerY + (state.nearestObstacleY > state.playerY ? -200 : 200);
                    break;
            }

            float confidence = 0.8f + weaponEffectiveness * 0.15f;
            return new GameAction(actionType, x, y, confidence, "fps_ai");
        }

        @Override
        protected float calculateRiskLevel(UniversalGameState state) {
            float healthRisk = 1.0f - state.healthLevel;
            float ammoRisk = 1.0f - state.powerLevel;
            float enemyProximity = 1.0f - calculateEnemyDistance(state);
            
            return Math.min(1.0f, (healthRisk + ammoRisk + enemyProximity) / 3.0f);
        }

        @Override
        protected boolean shouldEngage(UniversalGameState state) {
            float weaponEffectiveness = calculateWeaponEffectiveness(state);
            float enemyDistance = calculateEnemyDistance(state);
            
            // Don't engage if low health or ammo
            if (state.healthLevel < 0.3f || state.powerLevel < 0.2f) return false;
            
            // Don't engage if weapon is ineffective
            if (weaponEffectiveness < 0.4f) return false;
            
            // Engage if enemy is close and we're well equipped
            return enemyDistance < 0.6f && weaponEffectiveness > 0.6f && state.healthLevel > 0.5f;
        }
    }

    // Enhanced strategy selection and coordination with thread safety
    public synchronized GameAction analyzeGameContext(GameType gameType, UniversalGameState state) {
        if (state == null) {
            Log.w(TAG, "Cannot analyze null game state");
            return createSafeAction();
        }
        
        try {
            BaseStrategy strategy = getStrategyForGameType(gameType);

            if (strategy != null) {
                GameAction strategicAction = strategy.analyzeGameContext(state);

                // Combine with base DQN decision with thread safety
                GameAction dqnAction = selectOptimalActionSafe(state);

                // Weighted combination of strategic and learned actions
                return combineActions(strategicAction, dqnAction, strategy.calculateRiskLevel(state));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in game context analysis", e);
        }

        // Fallback to base DQN with error recovery
        return selectOptimalActionSafe(state);
    }
    
    private synchronized GameAction selectOptimalActionSafe(UniversalGameState state) {
        try {
            return selectOptimalAction(state);
        } catch (Exception e) {
            Log.w(TAG, "Error in DQN action selection, using safe fallback", e);
            return createSafeAction();
        }
    }
    
    private GameAction createSafeAction() {
        // Create a safe default action that won't cause crashes
        return new GameAction("WAIT", 540, 1170, 0.5f, "safety_fallback");
    }

    private BaseStrategy getStrategyForGameType(GameType gameType) {
        switch (gameType) {
            case BATTLE_ROYALE:
                return new BattleRoyaleStrategy(context);
            case MOBA:
                return new MOBAStrategy(context);
            case FPS:
                return new FPSStrategy(context);
            default:
                return null;
        }
    }

    private GameAction combineActions(GameAction strategic, GameAction learned, float riskLevel) {
        // Higher risk = trust strategic AI more, lower risk = trust learned behavior
        float strategicWeight = 0.3f + (riskLevel * 0.5f);
        float learnedWeight = 1.0f - strategicWeight;

        // Weighted average of coordinates
        int combinedX = (int)(strategic.getX() * strategicWeight + learned.getX() * learnedWeight);
        int combinedY = (int)(strategic.getY() * strategicWeight + learned.getY() * learnedWeight);

        // Use strategic action type if high risk, otherwise learned
        String actionType = riskLevel > 0.6f ? strategic.getActionType() : learned.getActionType();

        float combinedConfidence = (strategic.getConfidence() * strategicWeight +
                learned.getConfidence() * learnedWeight);

        return new GameAction(actionType, combinedX, combinedY, combinedConfidence, "combined_ai");
    }

    public void connectToExistingAgents(Object dqnAgent, Object ppoAgent) {
        this.externalDQN = dqnAgent;
        this.externalPPO = ppoAgent;
        Log.d(TAG, "Connected to existing RL agents");
    }

    public void syncWithRLAgents(UniversalGameState gameState) {
        if (externalDQN != null) {
            updateExternalAgent(gameState);
        }
    }

    private void updateExternalAgent(UniversalGameState state) {
        Log.d(TAG, "Syncing with external RL agents");
    }

    private void initializeNetwork() {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(123)
                .weightInit(WeightInit.XAVIER)
                .updater(new org.nd4j.linalg.learning.config.Adam(0.001))
                .list()
                .layer(new DenseLayer.Builder().nIn(STATE_SIZE).nOut(256)
                        .activation(Activation.RELU).build())
                .layer(new DenseLayer.Builder().nIn(256).nOut(128)
                        .activation(Activation.RELU).build())
                .layer(new DenseLayer.Builder().nIn(128).nOut(64)
                        .activation(Activation.RELU).build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .nIn(64).nOut(ACTION_SIZE)
                        .activation(Activation.IDENTITY).build())
                .build();

        dqn = new MultiLayerNetwork(conf);
        dqn.init();
        Log.d(TAG, "Neural network initialized for universal game learning");
    }

    /**
     * Learn optimal action for ANY game state
     */
    public GameAction selectOptimalAction(UniversalGameState gameState) {
        INDArray stateArray = gameStateToArray(gameState);
        INDArray qValues = dqn.output(stateArray);

        int actionIndex;
        if (random.nextFloat() < epsilon) {
            // Explore - try random action
            actionIndex = random.nextInt(ACTION_SIZE);
            Log.d(TAG, "Exploring with random action");
        } else {
            // Exploit - use learned knowledge
            actionIndex = Nd4j.argMax(qValues, 1).getInt(0);
            Log.d(TAG, "Using learned optimal action");
        }

        return createUniversalGameAction(actionIndex, gameState);
    }

    /**
     * Convert any game state to neural network input
     */
    private INDArray gameStateToArray(UniversalGameState state) {
        float[] stateData = new float[STATE_SIZE];

        // Universal game state features - works for any game
        stateData[0] = state.playerX / 1080f; // Normalized player position
        stateData[1] = state.playerY / 1920f;
        stateData[2] = 1080f / 1080f; // Default screen width normalized
        stateData[3] = 1920f / 1920f; // Default screen height normalized
        stateData[4] = state.gameScore / 10000f; // Normalized score
        stateData[5] = state.gameSpeed / 10f; // Game speed factor
        stateData[6] = state.objectCount / 20f; // Number of objects on screen
        stateData[7] = state.threatLevel; // Danger level (0-1)
        stateData[8] = state.opportunityLevel; // Reward opportunity (0-1)
        stateData[9] = state.timeInGame / 300f; // Game session time
        stateData[10] = state.averageReward; // Learning feedback
        stateData[11] = state.consecutiveSuccess / 10f; // Success streak
        stateData[12] = state.gameType; // Game classification (0-1)
        stateData[13] = state.difficultyLevel; // Current difficulty
        stateData[14] = state.powerUpActive ? 1f : 0f; // Power-up status
        stateData[15] = state.healthLevel; // Player health/lives

        return Nd4j.create(stateData).reshape(1, STATE_SIZE);
    }

    /**
     * Create universal game action that works for any game
     */
    private GameAction createUniversalGameAction(int actionIndex, UniversalGameState state) {
        String actionType = UNIVERSAL_ACTIONS[actionIndex];

        // Calculate action coordinates based on screen and game context
        int x = calculateActionX(actionType, state);
        int y = calculateActionY(actionType, state);
        float confidence = 0.8f + (random.nextFloat() * 0.2f); // Learning confidence

        return new GameAction(actionType, x, y, confidence, "ai_learned");
    }

    private int calculateActionX(String actionType, UniversalGameState state) {
        switch (actionType) {
            case "SWIPE_LEFT":
                return state.playerX - 200;
            case "SWIPE_RIGHT":
                return state.playerX + 200;
            case "TAP":
            case "DOUBLE_TAP":
            case "LONG_PRESS":
                // Tap on closest opportunity or threat
                return state.nearestObstacleX > 0 ? state.nearestObstacleX : state.playerX;
            default:
                return state.playerX;
        }
    }

    private int calculateActionY(String actionType, UniversalGameState state) {
        switch (actionType) {
            case "SWIPE_UP":
                return state.playerY - 200;
            case "SWIPE_DOWN":
                return state.playerY + 200;
            case "TAP":
            case "DOUBLE_TAP":
            case "LONG_PRESS":
                return state.nearestObjectY > 0 ? state.nearestObjectY : state.playerY;
            default:
                return state.playerY;
        }
    }

    /**
     * Learn from game outcomes - universal learning system
     */
    public void learnFromExperience(UniversalGameState previousState, GameAction action,
                                    float reward, UniversalGameState newState, boolean gameOver) {

        // Store experience in replay buffer
        replayBuffer.addExperience(previousState, action, reward, newState, gameOver);

        // Train network if enough experiences collected
        if (replayBuffer.size() > 32) {
            trainNetwork();
        }

        // Decay exploration rate - become more confident over time
        if (epsilon > minEpsilon) {
            epsilon *= epsilonDecay;
        }

        Log.d(TAG, "Learned from experience - Reward: " + reward + ", Epsilon: " + epsilon);
    }

    private void trainNetwork() {
        // Sample batch of experiences for training
        List<Experience> batch = replayBuffer.sampleBatch(32);

        INDArray states = Nd4j.zeros(32, STATE_SIZE);
        INDArray targets = Nd4j.zeros(32, ACTION_SIZE);

        for (int i = 0; i < batch.size(); i++) {
            Experience exp = batch.get(i);

            INDArray state = gameStateToArray(exp.state);
            INDArray nextState = gameStateToArray(exp.nextState);

            states.putRow(i, state);

            INDArray currentQ = dqn.output(state);
            INDArray nextQ = dqn.output(nextState);

            float target = exp.reward;
            if (!exp.gameOver) {
                target += 0.95f * Nd4j.max(nextQ).getFloat(0); // Discount factor
            }

            currentQ.putScalar(getActionIndex(exp.action.getActionType()), target);
            targets.putRow(i, currentQ);
        }

        // Train the network
        
        /**
         * CRITICAL: Update strategy with expert reasoning context
         */
    }
    
    public void updateStrategyWithReasoning(String why, String what, String how, String action) {
        try {
            if (!isInitialized) {
                Log.w(TAG, "Strategy agent not initialized for reasoning update");
                return;
            }
            
            // Parse strategic intent from "why"
            StrategicIntent intent = parseStrategicIntent(why);
            
            // Analyze target from "what"
            TargetAnalysis target = analyzeTargetForStrategy(what);
            
            // Extract method from "how"
            InteractionMethod method = parseInteractionMethod(how);
            
            // Update strategy based on reasoning components
            updateInternalStrategy(intent, target, method, action);
            
            Log.d(TAG, "Strategy updated with reasoning - Intent: " + intent.type + 
                  ", Target: " + target.category + ", Method: " + method.type);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating strategy with reasoning", e);
        }
    }
    
    private StrategicIntent parseStrategicIntent(String why) {
        StrategicIntent intent = new StrategicIntent();
        
        if (why == null || why.trim().isEmpty()) {
            intent.type = "general";
            intent.confidence = 0.3f;
            return intent;
        }
        
        String whyLower = why.toLowerCase();
        
        if (whyLower.contains("attack") || whyLower.contains("offensive")) {
            intent.type = "offensive";
            intent.confidence = 0.8f;
        } else if (whyLower.contains("defend") || whyLower.contains("protect")) {
            intent.type = "defensive";
            intent.confidence = 0.8f;
        } else if (whyLower.contains("collect") || whyLower.contains("gather")) {
            intent.type = "resource_gathering";
            intent.confidence = 0.7f;
        } else if (whyLower.contains("explore") || whyLower.contains("navigate")) {
            intent.type = "exploration";
            intent.confidence = 0.6f;
        } else {
            intent.type = "general";
            intent.confidence = 0.5f;
        }
        
        return intent;
    }
    
    private TargetAnalysis analyzeTargetForStrategy(String what) {
        TargetAnalysis analysis = new TargetAnalysis();
        
        if (what == null || what.trim().isEmpty()) {
            analysis.category = "unknown";
            analysis.priority = 0.3f;
            return analysis;
        }
        
        String whatLower = what.toLowerCase();
        
        if (whatLower.contains("enemy") || whatLower.contains("opponent")) {
            analysis.category = "hostile_entity";
            analysis.priority = 0.9f;
            analysis.threatLevel = 0.8f;
        } else if (whatLower.contains("item") || whatLower.contains("powerup")) {
            analysis.category = "collectible";
            analysis.priority = 0.6f;
            analysis.threatLevel = 0.1f;
        } else if (whatLower.contains("button") || whatLower.contains("menu")) {
            analysis.category = "ui_element";
            analysis.priority = 0.4f;
            analysis.threatLevel = 0.0f;
        } else {
            analysis.category = "general";
            analysis.priority = 0.4f;
            analysis.threatLevel = 0.3f;
        }
        
        return analysis;
    }
    
    private InteractionMethod parseInteractionMethod(String how) {
        InteractionMethod method = new InteractionMethod();
        
        if (how == null || how.trim().isEmpty()) {
            method.type = "unknown";
            method.precision = 0.3f;
            return method;
        }
        
        String howLower = how.toLowerCase();
        
        if (howLower.contains("tap") || howLower.contains("click")) {
            method.type = "tap";
            method.precision = 0.8f;
        } else if (howLower.contains("swipe") || howLower.contains("drag")) {
            method.type = "swipe";
            method.precision = 0.7f;
        } else {
            method.type = "general";
            method.precision = 0.5f;
        }
        
        return method;
    }
    
    private void updateInternalStrategy(StrategicIntent intent, TargetAnalysis target, 
                                      InteractionMethod method, String action) {
        try {
            if (intent.type.equals("offensive")) {
                offensiveStrategy.aggressiveness = Math.min(1.0f, 
                    offensiveStrategy.aggressiveness + intent.confidence * 0.1f);
            } else if (intent.type.equals("defensive")) {
                defensiveStrategy.cautiousness = Math.min(1.0f,
                    defensiveStrategy.cautiousness + intent.confidence * 0.1f);
            } else if (intent.type.equals("resource_gathering")) {
                resourceStrategy.efficiency = Math.min(1.0f,
                    resourceStrategy.efficiency + intent.confidence * 0.1f);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating internal strategy", e);
        }
    }
    
    // Supporting data structures
    private static class StrategicIntent {
        String type;
        float confidence;
    }
    
    private static class TargetAnalysis {
        String category;
        float priority;
        float threatLevel;
    }
    
    private static class InteractionMethod {
        String type;
        float precision;
    }
    
    private void trainNetworkInternal() {
        // Train the network
        dqn.fit(states, targets);
    }

    private int getActionIndex(String actionType) {
        for (int i = 0; i < UNIVERSAL_ACTIONS.length; i++) {
            if (UNIVERSAL_ACTIONS[i].equals(actionType)) {
                return i;
            }
        }
        return 0; // Default to first action
    }

    /**
     * Universal game state class - works for any game
     */
    public static class UniversalGameState {
        public int playerX, playerY;
        public int screenWidth, screenHeight;
        public int gameScore;
        public float gameSpeed;
        public int objectCount;
        public float threatLevel; // 0-1, how dangerous current situation is
        public float opportunityLevel; // 0-1, how much reward potential
        public float timeInGame;
        public float averageReward;
        public int consecutiveSuccess;
        public float gameType; // Classification of game type
        public float difficultyLevel;
        public boolean powerUpActive;
        public float healthLevel;
        public int nearestObjectX, nearestObjectY; // Closest interactive object
        
        // Additional fields for advanced strategies
        public float playerVelX, playerVelY;
        public int nearestObstacleX, nearestObstacleY;
        public int nearestCoinX, nearestCoinY;
        public float powerLevel, speedLevel;
        
        public UniversalGameState() {
            // Initialize default values
            this.screenWidth = 1080;
            this.screenHeight = 1920;
            this.playerX = screenWidth / 2;
            this.playerY = screenHeight / 2;
            this.healthLevel = 1.0f;
            this.powerLevel = 1.0f;
            this.speedLevel = 1.0f;
            this.threatLevel = 0.0f;
            this.opportunityLevel = 0.5f;
            this.timeInGame = 0f;
            this.consecutiveSuccess = 0;
            this.objectCount = 0;
        }
    }

    public GameAction getOptimalAction(Object state) {
        if (state instanceof UniversalGameState) {
            UniversalGameState gameState = (UniversalGameState) state;

            // Use epsilon-greedy strategy
            if (random.nextFloat() < epsilon) {
                // Exploration - random action
                String action = UNIVERSAL_ACTIONS[random.nextInt(UNIVERSAL_ACTIONS.length)];
                return new GameAction(action, gameState.playerX, gameState.playerY, 0.5f, "exploration");
            } else {
                // Exploitation - use neural network
                INDArray stateArray = gameStateToArray(gameState);
                INDArray actionProbabilities = dqn.output(stateArray);
                int bestAction = Nd4j.argMax(actionProbabilities, 1).getInt(0);

                String actionType = UNIVERSAL_ACTIONS[bestAction];
                return new GameAction(actionType, gameState.playerX, gameState.playerY, 0.8f, "dqn_policy");
            }
        }
        return new GameAction("WAIT", 540, 960, 0.5f, "fallback");
    }

    public void updateReinforcementLearning(UniversalGameState state, GameAction action, float reward, UniversalGameState nextState) {
        learnFromExperience(state, action, reward, nextState, false);
    }
    
    public void cleanup() {
        try {
            if (dqn != null) {
                dqn.clear();
                dqn = null;
            }
            if (replayBuffer != null) {
                replayBuffer.clear();
                replayBuffer = null;
            }
            if (mlExecutor != null && !mlExecutor.isShutdown()) {
                mlExecutor.shutdown();
            }
            Log.d(TAG, "GameStrategyAgent cleaned up successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during GameStrategyAgent cleanup", e);
        }
    }
    
    // Field to track last processed state
    private com.gestureai.gameautomation.data.UniversalGameState lastProcessedState;
    
    /**
     * Get DQN agent instance - called by GameAutomationEngine
     */
    public DQNAgent getDQNAgent() {
        if (externalDQN instanceof DQNAgent) {
            return (DQNAgent) externalDQN;
        }
        return null;
    }
    
    /**
     * Get PPO agent instance - called by GameAutomationEngine
     */
    public PPOAgent getPPOAgent() {
        if (externalPPO instanceof PPOAgent) {
            return (PPOAgent) externalPPO;
        }
        return null;
    }
    
    /**
     * Get current game state - called by GameAutomationEngine
     */
    public com.gestureai.gameautomation.data.UniversalGameState getCurrentGameState() {
        if (lastProcessedState != null) {
            return lastProcessedState;
        }
        return new com.gestureai.gameautomation.data.UniversalGameState();
    }
    
    /**
     * Check if GameStrategyAgent is active - called by GameAutomationEngine
     */
    public boolean isActive() {
        return initialized && currentStrategy != null && !mlExecutor.isShutdown();
    }
    
    /**
     * Start game type detection - called by GameAutomationEngine
     */
    public void startGameTypeDetection() {
        Log.d(TAG, "Starting automatic game type detection");
        try {
            if (context != null) {
                detectGameTypeFromContext();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start game type detection", e);
        }
    }
    
    private void detectGameTypeFromContext() {
        Log.d(TAG, "Analyzing game context for type detection");
    }
    
    /**
     * Update the last processed state
     */
    public void updateGameState(com.gestureai.gameautomation.data.UniversalGameState state) {
        this.lastProcessedState = state;
    }
}
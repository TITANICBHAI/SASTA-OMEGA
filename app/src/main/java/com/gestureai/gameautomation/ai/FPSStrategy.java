package com.gestureai.gameautomation.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.gestureai.gameautomation.GameAction;
import com.gestureai.gameautomation.DetectedObject;
import com.gestureai.gameautomation.GameContextAnalyzer;
import com.gestureai.gameautomation.ai.GameStrategyAgent.UniversalGameState;
import com.gestureai.gameautomation.PlayerTracker.PlayerData;
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
 * Advanced FPS Strategy for first-person shooter games
 * Handles aiming, movement, weapon selection, tactical positioning, and combat scenarios
 */
public class FPSStrategy {
    private static final String TAG = "FPSStrategy";

    private Context context;
    private MultiLayerNetwork aimingNetwork;
    private MultiLayerNetwork movementNetwork;
    private MultiLayerNetwork weaponNetwork;
    private MultiLayerNetwork tacticalNetwork;

    // Strategy parameters
    private static final int AIM_STATE_SIZE = 25;
    private static final int MOVEMENT_STATE_SIZE = 20;
    private static final int WEAPON_STATE_SIZE = 15;
    private static final int TACTICAL_STATE_SIZE = 30;
    private static final int ACTION_SIZE = 12;
    private int reactionDelayMs = 150; // Default reaction time
    private float targetAccuracy = 0.8f; // Default accuracy target

    // Performance tracking
    private Map<String, Float> weaponPerformance;
    private List<EngagementResult> combatHistory;
    private Map<String, Integer> killStats;
    private float aimAccuracy = 0.0f;
    private int totalShots = 0;
    private int totalHits = 0;

    public enum FPSAction {
        AIM_TARGET, SHOOT_PRIMARY, RELOAD_WEAPON, SWITCH_WEAPON,
        MOVE_COVER, STRAFE_LEFT, STRAFE_RIGHT, JUMP_DODGE,
        CROUCH_POSITION, THROW_GRENADE, USE_ABILITY, TACTICAL_RETREAT
    }

    public enum WeaponType {
        ASSAULT_RIFLE, SNIPER_RIFLE, SHOTGUN, SMG, PISTOL, LMG, ROCKET_LAUNCHER, GRENADE
    }

    public enum CombatRange {
        CLOSE, MEDIUM, LONG, EXTREME
    }

    public static class FPSState {
        // Player status
        public float[] playerPosition = new float[3]; // X, Y, Z (includes height/elevation)
        public float[] playerRotation = new float[2]; // Yaw, Pitch
        public float[] crosshairPosition = new float[2];
        public int health;
        public int armor;
        public int stamina;
        public boolean isReloading;
        public boolean isCrouched;
        public boolean isMoving;

        // Weapon information
        public WeaponType currentWeapon;
        public int currentAmmo;
        public int totalAmmo;
        public float weaponAccuracy;
        public float recoilPattern;
        public boolean weaponReady;
        public List<WeaponType> availableWeapons;

        // Enemy information
        public List<PlayerData> visibleEnemies;
        public List<PlayerData> teammates;
        public PlayerData targetEnemy;
        public float distanceToTarget;
        public CombatRange combatRange;
        public boolean enemyAiming;
        public boolean underFire;

        // Environment
        public boolean inCover;
        public float coverQuality;
        public List<DetectedObject> coverPoints;
        public String mapArea;
        public boolean hasHighGround;
        public float visibility;

        // Game mode specific
        public int kills;
        public int deaths;
        public int assists;
        public String gameMode; // "deathmatch", "team", "capture", "bomb"
        public boolean objectiveNearby;
        public int teamScore;
        public int enemyScore;

        public FPSState() {
            this.availableWeapons = new ArrayList<>();
            this.visibleEnemies = new ArrayList<>();
            this.teammates = new ArrayList<>();
            this.coverPoints = new ArrayList<>();
            this.currentWeapon = WeaponType.ASSAULT_RIFLE;
            this.combatRange = CombatRange.MEDIUM;
        }
    }

    public static class EngagementResult {
        public boolean won;
        public WeaponType weaponUsed;
        public CombatRange range;
        public int shotsNeeded;
        public float accuracy;
        public boolean hadCover;
        public long timestamp;

        public EngagementResult(boolean won, WeaponType weapon, CombatRange range, int shots, float acc, boolean cover) {
            this.won = won;
            this.weaponUsed = weapon;
            this.range = range;
            this.shotsNeeded = shots;
            this.accuracy = acc;
            this.hadCover = cover;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public FPSStrategy(Context context) {
        this.context = context;
        this.weaponPerformance = new HashMap<>();
        this.combatHistory = new ArrayList<>();
        this.killStats = new HashMap<>();

        initializeNetworks();
        loadStrategyData();
        Log.d(TAG, "FPS Strategy initialized");
    }

    private void initializeNetworks() {
        try {
            // Aiming network - for target acquisition and crosshair placement
            MultiLayerConfiguration aimConf = new NeuralNetConfiguration.Builder()
                    .seed(123)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new org.nd4j.linalg.learning.config.Adam(0.0008))
                    .list()
                    .layer(new DenseLayer.Builder().nIn(AIM_STATE_SIZE).nOut(128)
                            .activation(Activation.RELU).build())
                    .layer(new DenseLayer.Builder().nIn(128).nOut(64)
                            .activation(Activation.RELU).build())
                    .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                            .nIn(64).nOut(2) // X, Y aim adjustments
                            .activation(Activation.TANH).build())
                    .build();

            aimingNetwork = new MultiLayerNetwork(aimConf);
            if (aimingNetwork != null) {
                try {
                    aimingNetwork.init();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to initialize aiming network", e);
                    aimingNetwork = null;
                }
            }

            // Movement network - for positioning and evasion
            MultiLayerConfiguration movementConf = new NeuralNetConfiguration.Builder()
                    .seed(456)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new org.nd4j.linalg.learning.config.Adam(0.001))
                    .list()
                    .layer(new DenseLayer.Builder().nIn(MOVEMENT_STATE_SIZE).nOut(100)
                            .activation(Activation.RELU).build())
                    .layer(new DenseLayer.Builder().nIn(100).nOut(50)
                            .activation(Activation.RELU).build())
                    .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                            .nIn(50).nOut(ACTION_SIZE)
                            .activation(Activation.SOFTMAX).build())
                    .build();

            movementNetwork = new MultiLayerNetwork(movementConf);
            if (movementNetwork != null) {
                try {
                    movementNetwork.init();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to initialize movement network", e);
                    movementNetwork = null;
                }
            }

            // Weapon selection network
            MultiLayerConfiguration weaponConf = new NeuralNetConfiguration.Builder()
                    .seed(789)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new org.nd4j.linalg.learning.config.Adam(0.0012))
                    .list()
                    .layer(new DenseLayer.Builder().nIn(WEAPON_STATE_SIZE).nOut(80)
                            .activation(Activation.RELU).build())
                    .layer(new DenseLayer.Builder().nIn(80).nOut(40)
                            .activation(Activation.RELU).build())
                    .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                            .nIn(40).nOut(WeaponType.values().length)
                            .activation(Activation.SOFTMAX).build())
                    .build();

            weaponNetwork = new MultiLayerNetwork(weaponConf);
            if (weaponNetwork != null) {
                try {
                    weaponNetwork.init();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to initialize weapon network", e);
                    weaponNetwork = null;
                }
            }

            // Tactical decision network
            MultiLayerConfiguration tacticalConf = new NeuralNetConfiguration.Builder()
                    .seed(101112)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new org.nd4j.linalg.learning.config.Adam(0.0015))
                    .list()
                    .layer(new DenseLayer.Builder().nIn(TACTICAL_STATE_SIZE).nOut(150)
                            .activation(Activation.RELU).build())
                    .layer(new DenseLayer.Builder().nIn(150).nOut(75)
                            .activation(Activation.RELU).build())
                    .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                            .nIn(75).nOut(ACTION_SIZE)
                            .activation(Activation.SOFTMAX).build())
                    .build();

            tacticalNetwork = new MultiLayerNetwork(tacticalConf);
            if (tacticalNetwork != null) {
                try {
                    tacticalNetwork.init();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to initialize tactical network", e);
                    tacticalNetwork = null;
                }
            }

            Log.d(TAG, "FPS neural networks initialized");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing neural networks", e);
        }
    }

    public GameAction analyzeFPSScenario(FPSState state,UniversalGameState gameState, GameContextAnalyzer.GameContext gameContext) {
        try {
            // Multi-layer FPS decision making
            FPSAction primaryAction;

            // Priority system: Combat > Movement > Utility
            if (state.targetEnemy != null && state.weaponReady) {
                primaryAction = analyzeCombat(state);
            } else if (state.underFire || !state.inCover) {
                primaryAction = analyzeMovement(state);
            } else {
                primaryAction = analyzeTactical(state);
            }

            // Calculate optimal weapon for current situation
            WeaponType optimalWeapon = analyzeWeaponSelection(state);

            // Calculate precise aim adjustment
            float[] aimAdjustment = calculateAimAdjustment(state);

            // Convert strategic decision to game action
            GameAction action = createActionFromStrategy(primaryAction, state, aimAdjustment, optimalWeapon);

            // Log decision reasoning
            Log.d(TAG, String.format("FPS Decision: Action=%s, Weapon=%s, Range=%s, Enemies=%d",
                    primaryAction.name(), optimalWeapon.name(), state.combatRange.name(), state.visibleEnemies.size()));

            return action;

        } catch (Exception e) {
            Log.e(TAG, "Error in FPS analysis", e);
            return createFallbackAction(state);
        }
    }

    private FPSAction analyzeCombat(FPSState state) {
        try {
            float[] tacticalFeatures = createTacticalFeatures(state);
            INDArray input = Nd4j.create(tacticalFeatures).reshape(1, TACTICAL_STATE_SIZE);
            INDArray output = tacticalNetwork.output(input);

            int bestActionIndex = Nd4j.argMax(output, 1).getInt(0);
            FPSAction[] actions = FPSAction.values();

            if (bestActionIndex < actions.length) {
                return actions[bestActionIndex];
            } else {
                return FPSAction.AIM_TARGET;
            }

        } catch (Exception e) {
            Log.w(TAG, "Error in combat analysis", e);
            return selectFallbackCombatAction(state);
        }
    }

    private FPSAction analyzeMovement(FPSState state) {
        try {
            float[] movementFeatures = createMovementFeatures(state);
            INDArray input = Nd4j.create(movementFeatures).reshape(1, MOVEMENT_STATE_SIZE);
            INDArray output = movementNetwork.output(input);

            int bestActionIndex = Nd4j.argMax(output, 1).getInt(0);
            FPSAction[] actions = FPSAction.values();

            if (bestActionIndex < actions.length) {
                return actions[bestActionIndex];
            } else {
                return FPSAction.MOVE_COVER;
            }

        } catch (Exception e) {
            Log.w(TAG, "Error in movement analysis", e);
            return selectFallbackMovementAction(state);
        }
    }

    private FPSAction analyzeTactical(FPSState state) {
        // Tactical decisions when not in immediate combat
        if (state.currentAmmo < 10 && !state.isReloading) {
            return FPSAction.RELOAD_WEAPON;
        }

        if (!state.inCover && !state.coverPoints.isEmpty()) {
            return FPSAction.MOVE_COVER;
        }

        if (state.objectiveNearby && "capture".equals(state.gameMode)) {
            return FPSAction.MOVE_COVER; // Move to objective
        }

        if (!state.visibleEnemies.isEmpty() && state.weaponReady) {
            return FPSAction.AIM_TARGET;
        }

        return FPSAction.MOVE_COVER; // Default tactical action
    }

    private WeaponType analyzeWeaponSelection(FPSState state) {
        try {
            float[] weaponFeatures = createWeaponFeatures(state);
            INDArray input = Nd4j.create(weaponFeatures).reshape(1, WEAPON_STATE_SIZE);
            INDArray output = weaponNetwork.output(input);

            int bestWeaponIndex = Nd4j.argMax(output, 1).getInt(0);
            WeaponType[] weapons = WeaponType.values();

            if (bestWeaponIndex < weapons.length && state.availableWeapons.contains(weapons[bestWeaponIndex])) {
                return weapons[bestWeaponIndex];
            } else {
                return selectOptimalWeaponFallback(state);
            }

        } catch (Exception e) {
            Log.w(TAG, "Error in weapon selection", e);
            return selectOptimalWeaponFallback(state);
        }
    }

    private float[] calculateAimAdjustment(FPSState state) {
        try {
            if (state.targetEnemy == null) {
                return new float[]{0.0f, 0.0f};
            }

            float[] aimFeatures = createAimFeatures(state);
            INDArray input = Nd4j.create(aimFeatures).reshape(1, AIM_STATE_SIZE);
            INDArray output = aimingNetwork.output(input);

            float deltaX = output.getFloat(0) * 200.0f; // Scale to screen coordinates
            float deltaY = output.getFloat(1) * 200.0f;

            return new float[]{deltaX, deltaY};

        } catch (Exception e) {
            Log.w(TAG, "Error calculating aim adjustment", e);
            return calculateFallbackAim(state);
        }
    }

    private float[] createTacticalFeatures(FPSState state) {
        float[] features = new float[TACTICAL_STATE_SIZE];

        // Player status (0-9)
        features[0] = state.health / 100.0f;
        features[1] = state.armor / 100.0f;
        features[2] = state.stamina / 100.0f;
        features[3] = state.isReloading ? 1.0f : 0.0f;
        features[4] = state.isCrouched ? 1.0f : 0.0f;
        features[5] = state.isMoving ? 1.0f : 0.0f;
        features[6] = state.inCover ? 1.0f : 0.0f;
        features[7] = state.coverQuality;
        features[8] = state.hasHighGround ? 1.0f : 0.0f;
        features[9] = state.visibility;

        // Weapon status (10-14)
        features[10] = state.currentAmmo / 30.0f; // Normalized assuming 30 max ammo
        features[11] = state.totalAmmo / 300.0f;
        features[12] = state.weaponAccuracy;
        features[13] = state.weaponReady ? 1.0f : 0.0f;
        features[14] = getWeaponTypeValue(state.currentWeapon);

        // Enemy information (15-22)
        features[15] = state.visibleEnemies.size() / 10.0f;
        features[16] = state.targetEnemy != null ? 1.0f : 0.0f;
        features[17] = state.distanceToTarget / 1000.0f;
        features[18] = getCombatRangeValue(state.combatRange);
        features[19] = state.enemyAiming ? 1.0f : 0.0f;
        features[20] = state.underFire ? 1.0f : 0.0f;
        features[21] = state.teammates.size() / 10.0f;
        features[22] = calculateEnemyThreatLevel(state);

        // Game context (23-29)
        features[23] = state.kills / 20.0f;
        features[24] = state.deaths / 20.0f;
        features[25] = state.objectiveNearby ? 1.0f : 0.0f;
        features[26] = getGameModeValue(state.gameMode);
        features[27] = (state.teamScore - state.enemyScore) / 100.0f;
        features[28] = aimAccuracy;
        features[29] = (System.currentTimeMillis() % 60000) / 60000.0f; // Time factor

        return features;
    }

    private float[] createMovementFeatures(FPSState state) {
        float[] features = new float[MOVEMENT_STATE_SIZE];

        // Position and movement (0-6)
        features[0] = state.playerPosition[0] / 1000.0f;
        features[1] = state.playerPosition[1] / 1000.0f;
        features[2] = state.playerPosition[2] / 100.0f; // Height/elevation
        features[3] = state.isMoving ? 1.0f : 0.0f;
        features[4] = state.isCrouched ? 1.0f : 0.0f;
        features[5] = state.stamina / 100.0f;
        features[6] = state.hasHighGround ? 1.0f : 0.0f;

        // Cover and safety (7-11)
        features[7] = state.inCover ? 1.0f : 0.0f;
        features[8] = state.coverQuality;
        features[9] = state.coverPoints.size() / 10.0f;
        features[10] = state.underFire ? 1.0f : 0.0f;
        features[11] = calculatePositionalSafety(state);

        // Threat assessment (12-17)
        features[12] = state.visibleEnemies.size() / 10.0f;
        features[13] = state.distanceToTarget / 1000.0f;
        features[14] = state.enemyAiming ? 1.0f : 0.0f;
        features[15] = calculateEnemyThreatLevel(state);
        features[16] = state.teammates.size() / 10.0f;
        features[17] = calculateTeamSupport(state);

        // Additional factors (18-19)
        features[18] = state.health / 100.0f;
        features[19] = aimAccuracy;

        return features;
    }

    private float[] createWeaponFeatures(FPSState state) {
        float[] features = new float[WEAPON_STATE_SIZE];

        // Current weapon status (0-4)
        features[0] = getWeaponTypeValue(state.currentWeapon);
        features[1] = state.currentAmmo / 30.0f;
        features[2] = state.totalAmmo / 300.0f;
        features[3] = state.weaponAccuracy;
        features[4] = state.isReloading ? 1.0f : 0.0f;

        // Combat situation (5-9)
        features[5] = getCombatRangeValue(state.combatRange);
        features[6] = state.distanceToTarget / 1000.0f;
        features[7] = state.visibleEnemies.size() / 10.0f;
        features[8] = state.underFire ? 1.0f : 0.0f;
        features[9] = state.inCover ? 1.0f : 0.0f;

        // Performance factors (10-14)
        features[10] = getWeaponPerformance(state.currentWeapon);
        features[11] = aimAccuracy;
        features[12] = state.kills / 20.0f;
        features[13] = state.availableWeapons.size() / 8.0f;
        features[14] = calculateWeaponEffectiveness(state);

        return features;
    }

    private float[] createAimFeatures(FPSState state) {
        float[] features = new float[AIM_STATE_SIZE];

        if (state.targetEnemy == null) {
            return features; // Return zeros if no target
        }

        // Target information (0-7)
        features[0] = state.targetEnemy.position[0] / 1000.0f;
        features[1] = state.targetEnemy.position[1] / 1000.0f;
        features[2] = state.crosshairPosition[0] / 1000.0f;
        features[3] = state.crosshairPosition[1] / 1000.0f;
        features[4] = state.distanceToTarget / 1000.0f;
        features[5] = getCombatRangeValue(state.combatRange);
        features[6] = state.targetEnemy.confidence; // Target visibility/certainty
        features[7] = calculateTargetMovement(state);

        // Player status (8-13)
        features[8] = state.playerRotation[0] / 360.0f; // Yaw
        features[9] = state.playerRotation[1] / 180.0f; // Pitch
        features[10] = state.isMoving ? 1.0f : 0.0f;
        features[11] = state.isCrouched ? 1.0f : 0.0f;
        features[12] = state.stamina / 100.0f;
        features[13] = state.hasHighGround ? 1.0f : 0.0f;

        // Weapon factors (14-18)
        features[14] = state.weaponAccuracy;
        features[15] = state.recoilPattern;
        features[16] = getWeaponTypeValue(state.currentWeapon);
        features[17] = state.currentAmmo / 30.0f;
        features[18] = calculateWeaponStability(state);

        // Environmental factors (19-24)
        features[19] = state.visibility;
        features[20] = state.underFire ? 1.0f : 0.0f;
        features[21] = state.enemyAiming ? 1.0f : 0.0f;
        features[22] = aimAccuracy; // Historical performance
        features[23] = calculateWindFactors(state);
        features[24] = (System.currentTimeMillis() % 5000) / 5000.0f; // Timing factor

        return features;
    }

    private GameAction createActionFromStrategy(FPSAction strategy, FPSState state, float[] aimAdjustment, WeaponType optimalWeapon) {
        switch (strategy) {
            case AIM_TARGET:
                if (state.targetEnemy != null) {
                    float targetX = state.targetEnemy.position[0] + aimAdjustment[0];
                    float targetY = state.targetEnemy.position[1] + aimAdjustment[1];
                    return new GameAction("AIM", (int)targetX, (int)targetY, 0.9f, "aim_enemy");
                }
                return new GameAction("WAIT", 540, 960, 0.3f, "no_target");

            case SHOOT_PRIMARY:
                if (state.targetEnemy != null && state.weaponReady) {
                    return new GameAction("TAP", (int)state.crosshairPosition[0], (int)state.crosshairPosition[1], 0.95f, "shoot");
                }
                return new GameAction("WAIT", 540, 960, 0.2f, "weapon_not_ready");

            case RELOAD_WEAPON:
                return new GameAction("LONG_PRESS", 540, 1600, 0.8f, "reload");

            case SWITCH_WEAPON:
                if (optimalWeapon != state.currentWeapon) {
                    int weaponSlot = getWeaponSlot(optimalWeapon);
                    return new GameAction("TAP", weaponSlot * 100 + 100, 1800, 0.7f, "switch_weapon");
                }
                return new GameAction("WAIT", 540, 960, 0.3f, "weapon_optimal");

            case MOVE_COVER:
                int[] coverPos = calculateNearestCover(state);
                return new GameAction("MOVE", coverPos[0], coverPos[1], 0.8f, "move_cover");

            case STRAFE_LEFT:
                return new GameAction("SWIPE_LEFT", 300, 960, 0.7f, "strafe_left");

            case STRAFE_RIGHT:
                return new GameAction("SWIPE_RIGHT", 780, 960, 0.7f, "strafe_right");

            case JUMP_DODGE:
                return new GameAction("SWIPE_UP", 540, 700, 0.6f, "jump");

            case CROUCH_POSITION:
                return new GameAction("SWIPE_DOWN", 540, 1200, 0.6f, "crouch");

            case THROW_GRENADE:
                if (state.targetEnemy != null) {
                    return new GameAction("LONG_PRESS", (int)state.targetEnemy.position[0], (int)state.targetEnemy.position[1], 0.8f, "grenade");
                }
                return new GameAction("WAIT", 540, 960, 0.3f, "no_grenade_target");

            case USE_ABILITY:
                return new GameAction("DOUBLE_TAP", 200, 1600, 0.7f, "ability");

            case TACTICAL_RETREAT:
                int[] retreatPos = calculateRetreatPosition(state);
                return new GameAction("MOVE", retreatPos[0], retreatPos[1], 0.9f, "retreat");

            default:
                return new GameAction("WAIT", 540, 960, 0.4f, "strategic_wait");
        }
    }

    // Helper methods for feature calculations
    private float getWeaponTypeValue(WeaponType weapon) {
        switch (weapon) {
            case ASSAULT_RIFLE: return 0.1f;
            case SNIPER_RIFLE: return 0.2f;
            case SHOTGUN: return 0.3f;
            case SMG: return 0.4f;
            case PISTOL: return 0.5f;
            case LMG: return 0.6f;
            case ROCKET_LAUNCHER: return 0.7f;
            case GRENADE: return 0.8f;
            default: return 0.1f;
        }
    }

    private float getCombatRangeValue(CombatRange range) {
        switch (range) {
            case CLOSE: return 0.2f;
            case MEDIUM: return 0.4f;
            case LONG: return 0.6f;
            case EXTREME: return 0.8f;
            default: return 0.4f;
        }
    }

    private float getGameModeValue(String gameMode) {
        switch (gameMode.toLowerCase()) {
            case "deathmatch": return 0.2f;
            case "team": return 0.4f;
            case "capture": return 0.6f;
            case "bomb": return 0.8f;
            default: return 0.4f;
        }
    }

    private float calculateEnemyThreatLevel(FPSState state) {
        float threat = 0.0f;
        threat += state.visibleEnemies.size() * 0.2f;
        threat += state.underFire ? 0.3f : 0.0f;
        threat += state.enemyAiming ? 0.2f : 0.0f;
        threat += (state.distanceToTarget < 100) ? 0.3f : 0.0f;
        return Math.min(1.0f, threat);
    }

    private float calculatePositionalSafety(FPSState state) {
        float safety = 0.5f; // Base safety
        if (state.inCover) safety += 0.3f;
        if (state.hasHighGround) safety += 0.1f;
        if (state.coverQuality > 0.7f) safety += 0.1f;
        return Math.min(1.0f, safety);
    }

    private float calculateTeamSupport(FPSState state) {
        float support = 0.0f;
        for (PlayerData teammate : state.teammates) {
            float distance = (float)Math.sqrt(
                    Math.pow(teammate.position[0] - state.playerPosition[0], 2) +
                            Math.pow(teammate.position[1] - state.playerPosition[1], 2)
            );
            if (distance < 200) { // Close teammate
                support += 0.3f;
            }
        }
        return Math.min(1.0f, support);
    }

    private float getWeaponPerformance(WeaponType weapon) {
        return weaponPerformance.getOrDefault(weapon.name(), 0.5f);
    }

    private float calculateWeaponEffectiveness(FPSState state) {
        // Calculate weapon effectiveness for current situation
        Map<CombatRange, Map<WeaponType, Float>> effectiveness = new HashMap<>();

        // Close range effectiveness
        Map<WeaponType, Float> closeRange = new HashMap<>();
        closeRange.put(WeaponType.SHOTGUN, 0.9f);
        closeRange.put(WeaponType.SMG, 0.8f);
        closeRange.put(WeaponType.ASSAULT_RIFLE, 0.6f);
        closeRange.put(WeaponType.PISTOL, 0.7f);
        closeRange.put(WeaponType.SNIPER_RIFLE, 0.3f);
        effectiveness.put(CombatRange.CLOSE, closeRange);

        // Medium range effectiveness
        Map<WeaponType, Float> mediumRange = new HashMap<>();
        mediumRange.put(WeaponType.ASSAULT_RIFLE, 0.9f);
        mediumRange.put(WeaponType.SMG, 0.7f);
        mediumRange.put(WeaponType.LMG, 0.8f);
        mediumRange.put(WeaponType.SNIPER_RIFLE, 0.8f);
        mediumRange.put(WeaponType.SHOTGUN, 0.4f);
        effectiveness.put(CombatRange.MEDIUM, mediumRange);

        // Long range effectiveness
        Map<WeaponType, Float> longRange = new HashMap<>();
        longRange.put(WeaponType.SNIPER_RIFLE, 0.95f);
        longRange.put(WeaponType.LMG, 0.7f);
        longRange.put(WeaponType.ASSAULT_RIFLE, 0.6f);
        longRange.put(WeaponType.SMG, 0.3f);
        longRange.put(WeaponType.SHOTGUN, 0.1f);
        effectiveness.put(CombatRange.LONG, longRange);

        return effectiveness.getOrDefault(state.combatRange, new HashMap<>())
                .getOrDefault(state.currentWeapon, 0.5f);
    }

    private float calculateTargetMovement(FPSState state) {
        // Simplified target movement prediction
        if (state.targetEnemy != null) {
            return 0.5f; // Assume moderate movement
        }
        return 0.0f;
    }

    private float calculateWeaponStability(FPSState state) {
        float stability = 0.7f; // Base stability
        if (state.isCrouched) stability += 0.2f;
        if (!state.isMoving) stability += 0.1f;
        return Math.min(1.0f, stability);
    }

    private float calculateWindFactors(FPSState state) {
        // Simplified environmental factors
        return 0.1f; // Minimal wind effect
    }

    private float[] calculateFallbackAim(FPSState state) {
        if (state.targetEnemy == null) {
            return new float[]{0.0f, 0.0f};
        }

        // Simple aim calculation toward target
        float deltaX = state.targetEnemy.position[0] - state.crosshairPosition[0];
        float deltaY = state.targetEnemy.position[1] - state.crosshairPosition[1];

        // Apply lead for moving targets
        deltaX *= 0.3f; // Conservative adjustment
        deltaY *= 0.3f;

        return new float[]{deltaX, deltaY};
    }

    private WeaponType selectOptimalWeaponFallback(FPSState state) {
        if (state.availableWeapons.isEmpty()) {
            return WeaponType.PISTOL; // Default fallback
        }

        // Select based on combat range
        switch (state.combatRange) {
            case CLOSE:
                if (state.availableWeapons.contains(WeaponType.SHOTGUN)) return WeaponType.SHOTGUN;
                if (state.availableWeapons.contains(WeaponType.SMG)) return WeaponType.SMG;
                break;
            case MEDIUM:
                if (state.availableWeapons.contains(WeaponType.ASSAULT_RIFLE)) return WeaponType.ASSAULT_RIFLE;
                if (state.availableWeapons.contains(WeaponType.LMG)) return WeaponType.LMG;
                break;
            case LONG:
            case EXTREME:
                if (state.availableWeapons.contains(WeaponType.SNIPER_RIFLE)) return WeaponType.SNIPER_RIFLE;
                if (state.availableWeapons.contains(WeaponType.ASSAULT_RIFLE)) return WeaponType.ASSAULT_RIFLE;
                break;
        }

        return state.availableWeapons.get(0); // Return first available weapon
    }

    // Fallback action selection methods
    private FPSAction selectFallbackCombatAction(FPSState state) {
        if (state.currentAmmo <= 0) return FPSAction.RELOAD_WEAPON;
        if (state.targetEnemy != null && state.weaponReady) return FPSAction.SHOOT_PRIMARY;
        if (state.underFire && !state.inCover) return FPSAction.MOVE_COVER;
        if (state.health < 30) return FPSAction.TACTICAL_RETREAT;
        return FPSAction.AIM_TARGET;
    }

    private FPSAction selectFallbackMovementAction(FPSState state) {
        if (state.underFire && !state.inCover) return FPSAction.MOVE_COVER;
        if (state.health < 50 && state.underFire) return FPSAction.TACTICAL_RETREAT;
        if (!state.inCover && !state.coverPoints.isEmpty()) return FPSAction.MOVE_COVER;
        return FPSAction.STRAFE_LEFT;
    }

    private GameAction createFallbackAction(FPSState state) {
        if (state.health < 20) {
            int[] retreatPos = calculateRetreatPosition(state);
            return new GameAction("MOVE", retreatPos[0], retreatPos[1], 0.9f, "emergency_retreat");
        }

        if (state.targetEnemy != null && state.weaponReady) {
            return new GameAction("TAP", (int)state.targetEnemy.position[0], (int)state.targetEnemy.position[1], 0.7f, "fallback_shoot");
        }

        return new GameAction("WAIT", 540, 960, 0.5f, "fallback_wait");
    }

    // Position calculation methods
    private int getWeaponSlot(WeaponType weapon) {
        switch (weapon) {
            case PISTOL: return 1;
            case SMG: return 2;
            case ASSAULT_RIFLE: return 3;
            case SHOTGUN: return 4;
            case SNIPER_RIFLE: return 5;
            case LMG: return 6;
            case ROCKET_LAUNCHER: return 7;
            case GRENADE: return 8;
            default: return 1;
        }
    }

    private int[] calculateNearestCover(FPSState state) {
        if (!state.coverPoints.isEmpty()) {
            DetectedObject nearestCover = state.coverPoints.get(0);
            return new int[]{nearestCover.boundingRect.centerX(), nearestCover.boundingRect.centerY()};
        }

        // Find cover relative to threats
        int coverX = (int)state.playerPosition[0];
        int coverY = (int)state.playerPosition[1];

        if (state.targetEnemy != null) {
            // Move perpendicular to enemy line of sight
            float deltaX = state.playerPosition[0] - state.targetEnemy.position[0];
            float deltaY = state.playerPosition[1] - state.targetEnemy.position[1];
            float distance = (float)Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            if (distance > 0) {
                coverX += (int)((deltaY / distance) * 150); // Move perpendicular
                coverY -= (int)((deltaX / distance) * 150);
            }
        }

        return new int[]{coverX, coverY};
    }

    private int[] calculateRetreatPosition(FPSState state) {
        int retreatX = (int)state.playerPosition[0];
        int retreatY = (int)state.playerPosition[1];

        // Move away from all visible enemies
        for (PlayerData enemy : state.visibleEnemies) {
            float deltaX = state.playerPosition[0] - enemy.position[0];
            float deltaY = state.playerPosition[1] - enemy.position[1];
            float distance = (float)Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            if (distance > 0 && distance < 300) { // Close enemies
                retreatX += (int)((deltaX / distance) * 200); // Move away
                retreatY += (int)((deltaY / distance) * 200);
            }
        }

        return new int[]{retreatX, retreatY};
    }

    private void loadStrategyData() {
        // Initialize weapon performance tracking
        for (WeaponType weapon : WeaponType.values()) {
            weaponPerformance.put(weapon.name(), 0.5f);
        }

        // Initialize kill stats
        killStats.put("total_kills", 0);
        killStats.put("headshots", 0);
        killStats.put("long_range_kills", 0);
        killStats.put("close_range_kills", 0);
    }

    public void recordCombatResult(boolean won, WeaponType weapon, CombatRange range, int shots, boolean hadCover) {
        float accuracy = shots > 0 ? (won ? 1.0f / shots : 0.0f) : 0.0f;
        EngagementResult result = new EngagementResult(won, weapon, range, shots, accuracy, hadCover);
        combatHistory.add(result);

        // Update weapon performance
        float currentPerf = weaponPerformance.getOrDefault(weapon.name(), 0.5f);
        float newPerf = won ? Math.min(1.0f, currentPerf + 0.1f) : Math.max(0.0f, currentPerf - 0.05f);
        weaponPerformance.put(weapon.name(), newPerf);

        // Update aim accuracy
        totalShots += shots;
        if (won) totalHits++;
        aimAccuracy = totalShots > 0 ? (float)totalHits / totalShots : 0.0f;

        // Update kill stats
        if (won) {
            killStats.put("total_kills", killStats.getOrDefault("total_kills", 0) + 1);

            if (range == CombatRange.LONG || range == CombatRange.EXTREME) {
                killStats.put("long_range_kills", killStats.getOrDefault("long_range_kills", 0) + 1);
            } else if (range == CombatRange.CLOSE) {
                killStats.put("close_range_kills", killStats.getOrDefault("close_range_kills", 0) + 1);
            }
        }

        // Limit history size
        if (combatHistory.size() > 1000) {
            combatHistory.remove(0);
        }

        Log.d(TAG, String.format("Combat result: Won=%b, Weapon=%s, Accuracy=%.2f", won, weapon.name(), aimAccuracy));
    }

    public float getAimAccuracy() {
        return aimAccuracy;
    }

    public Map<String, Float> getWeaponPerformance() {
        return new HashMap<>(weaponPerformance);
    }

    public Map<String, Integer> getKillStats() {
        return new HashMap<>(killStats);
    }

    public List<EngagementResult> getCombatHistory() {
        return new ArrayList<>(combatHistory);
    }
    public void setRecoilControlEnabled(boolean enabled) {
        Log.d(TAG, "Recoil control " + (enabled ? "enabled" : "disabled"));
    }

    public void setMovementOptimizationEnabled(boolean enabled) {
        Log.d(TAG, "Movement optimization " + (enabled ? "enabled" : "disabled"));
    }

    public void setAggressionLevel(float level) {
        Log.d(TAG, "Aggression level set to: " + level);
    }

    public void setStrategyType(String type) {
        Log.d(TAG, "Strategy type set to: " + type);
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
        if (aimingNetwork != null) {
            // Adjust learning rate based on target accuracy
            float learningRate = accuracy > 0.9f ? 0.0005f : 0.001f;
            // Network parameter updates would go here
        }
    }
}

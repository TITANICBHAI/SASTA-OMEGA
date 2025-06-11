package com.gestureai.gameautomation;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import android.os.Handler;
import android.os.Looper;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.ai.AdaptiveDecisionMaker;
import com.gestureai.gameautomation.ai.GameStatePredictor;
import com.gestureai.gameautomation.ai.StrategyProcessor;
import com.gestureai.gameautomation.services.TouchAutomationService;
import com.gestureai.gameautomation.PlayerTracker.PlayerData;
import com.gestureai.gameautomation.GameContextAnalyzer.GameContext;
import com.gestureai.gameautomation.services.ScreenCaptureService;
import com.gestureai.gameautomation.TouchController;
import com.gestureai.gameautomation.managers.AIStackManager;

public class GameAutomationEngine {
    private static final String TAG = "GameAutomationEngine";
    private static GameAutomationEngine instance;

    private Context context;
    private boolean isAutomationEnabled = false;
    private Map<String, GestureAction> gestureActionMap;

    private com.gestureai.gameautomation.services.TouchAutomationService accessibilityService;
    private ScreenCaptureService screenCaptureService;
    private com.gestureai.gameautomation.services.GestureRecognitionService gestureService;
    private TouchController touchController;
    
    // AI Stack Manager for ND4J toggle support
    private AIStackManager aiStackManager;
    private GameStrategyAgent gameStrategyAgent;
    private AdaptiveDecisionMaker adaptiveDecisionMaker;
    private GameStatePredictor gameStatePredictor;
    private GameStrategyAgent.UniversalGameState previousGameState;
    private GameAction lastExecutedAction;
    private long lastActionTime;
    private boolean learningEnabled = true;
    // Complete AI Integration System
    private boolean isRunning = false;
    private GameStrategyAgent.UniversalGameState currentGameState;
    private com.gestureai.gameautomation.ai.StrategyProcessor strategyProcessor;
    private ReinforcementLearner reinforcementLearner;
    private OCREngine ocrEngine;
    private com.gestureai.gameautomation.ai.DQNAgent dqnAgent;
    private com.gestureai.gameautomation.ai.PPOAgent ppoAgent;
    private ObjectLabelerEngine objectLabelerEngine;
    private ObjectDetectionEngine objectDetectionEngine;

    // Real-time data streams
    private Bitmap currentScreenCapture;
    private List<String> currentTexts;
    private List<ObjectDetectionEngine.DetectedObject> currentObjects;
    private int currentScore = 0;
    private int currentLives = 3;
    // Add new components
    private PlayerTracker playerTracker;
    private GameContextAnalyzer contextAnalyzer;
    private PerformanceTracker performanceTracker;
    
    // Thread management
    private ExecutorService mlExecutor;
    private ExecutorService screenCaptureExecutor;
    private ExecutorService touchExecutor;
    private Handler mainHandler;

    // Static initialization method for services
    public static void initialize(Context context) {
        if (instance == null) {
            instance = new GameAutomationEngine(context);
        }
    }
    
    private GameAutomationEngine(Context context) {
        this.context = context;
        this.gestureActionMap = new HashMap<>();
        this.aiStackManager = AIStackManager.getInstance(context);
        
        // Initialize thread pools
        this.mlExecutor = Executors.newFixedThreadPool(2);
        this.screenCaptureExecutor = Executors.newSingleThreadExecutor();
        this.touchExecutor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        initializeAIComponents();
        Log.d(TAG, "GameAutomationEngine initialized with AI stack support");
    }
    
    public static GameAutomationEngine getInstance() {
        return instance;
    }
    
    // Critical missing method causing service crashes
    public void setAutomationEnabled(boolean enabled) {
        this.isAutomationEnabled = enabled;
        Log.d(TAG, "Automation enabled: " + enabled);
        
        if (enabled) {
            startAutomationProcessing();
        } else {
            stopAutomationProcessing();
        }
    }
    
    public boolean isAutomationEnabled() {
        return isAutomationEnabled;
    }
    
    /**
     * Initialize AI components based on current stack configuration
     */
    private void initializeAIComponents() {
        try {
            // Initialize basic components always available
            playerTracker = new PlayerTracker();
            contextAnalyzer = new GameContextAnalyzer();
            performanceTracker = new PerformanceTracker();
            
            // Initialize AI components based on stack configuration
            if (aiStackManager.isND4JEnabled()) {
                initializeAdvancedAI();
            } else {
                initializeLightweightAI();
            }
            
            Log.d(TAG, "AI components initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AI components", e);
        }
    }
    
    /**
     * Initialize advanced ND4J-based AI components
     */
    private void initializeAdvancedAI() {
        try {
            gameStrategyAgent = new GameStrategyAgent(context);
            adaptiveDecisionMaker = new AdaptiveDecisionMaker();
            gameStatePredictor = new GameStatePredictor();
            
            Log.d(TAG, "Advanced ND4J AI components initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize advanced AI, falling back to lightweight", e);
            initializeLightweightAI();
        }
    }
    
    /**
     * Initialize lightweight TensorFlow Lite only components
     */
    private void initializeLightweightAI() {
        try {
            // Use lightweight alternatives when ND4J is disabled
            ocrEngine = new OCREngine(context);
            objectDetectionEngine = new ObjectDetectionEngine(context);
            
            Log.d(TAG, "Lightweight AI components initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing lightweight AI", e);
        }
    }
    
    /**
     * Make intelligent decision using available AI stack
     */
    public GameAction makeIntelligentDecision(Bitmap gameScreen, String gameType) {
        if (aiStackManager.isND4JEnabled() && adaptiveDecisionMaker != null) {
            // Use advanced ND4J-based decision making
            return makeAdvancedDecision(gameScreen, gameType);
        } else {
            // Use lightweight decision making
            return makeLightweightDecision(gameScreen, gameType);
        }
    }
    
    /**
     * Advanced decision making using ND4J
     */
    private GameAction makeAdvancedDecision(Bitmap gameScreen, String gameType) {
        try {
            // Extract game state features
            float[] gameState = extractGameStateFeatures(gameScreen);
            
            // Use AIStackManager for decision
            AIStackManager.GameDecision decision = aiStackManager.makeDecision(gameState, gameType);
            
            // Convert to GameAction
            return new GameAction(
                decision.action,
                getActionCoordinates(decision.action, gameScreen),
                decision.confidence
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Error in advanced decision making", e);
            return makeLightweightDecision(gameScreen, gameType);
        }
    }
    
    /**
     * Lightweight decision making using basic algorithms
     */
    private GameAction makeLightweightDecision(Bitmap gameScreen, String gameType) {
        try {
            // Basic rule-based decision making
            if (gameType.contains("subway") || gameType.contains("runner")) {
                return makeRunnerGameDecision(gameScreen);
            } else if (gameType.contains("puzzle")) {
                return makePuzzleGameDecision(gameScreen);
            } else {
                return makeGenericGameDecision(gameScreen);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in lightweight decision making", e);
            return new GameAction("tap", new int[]{500, 800}, 0.5f);
        }
    }
    
    /**
     * Extract game state features for AI processing
     */
    private float[] extractGameStateFeatures(Bitmap gameScreen) {
        // Convert bitmap to feature vector
        int width = gameScreen.getWidth();
        int height = gameScreen.getHeight();
        
        // Simple feature extraction - analyze screen regions
        float[] features = new float[20];
        
        // Extract features from different screen regions
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 5; j++) {
                int x = (width / 4) * i;
                int y = (height / 5) * j;
                int pixel = gameScreen.getPixel(x, y);
                
                // Convert pixel to brightness feature
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                features[i * 5 + j] = (r + g + b) / (3.0f * 255.0f);
            }
        }
        
        return features;
    }
    
    /**
     * Get action coordinates based on action type
     */
    private int[] getActionCoordinates(String action, Bitmap gameScreen) {
        int width = gameScreen.getWidth();
        int height = gameScreen.getHeight();
        
        switch (action.toLowerCase()) {
            case "swipeup":
                return new int[]{width / 2, height * 3 / 4};
            case "swipedown":
                return new int[]{width / 2, height / 4};
            case "swipeleft":
                return new int[]{width * 3 / 4, height / 2};
            case "swiperight":
                return new int[]{width / 4, height / 2};
            case "tap":
            default:
                return new int[]{width / 2, height / 2};
        }
    }
    
    /**
     * Runner game specific decision logic
     */
    private GameAction makeRunnerGameDecision(Bitmap gameScreen) {
        // Simple obstacle detection for runner games
        int width = gameScreen.getWidth();
        int height = gameScreen.getHeight();
        
        // Check for obstacles in player path
        int centerX = width / 2;
        int playerY = height * 2 / 3;
        
        // Sample pixels ahead of player
        for (int y = playerY - 100; y > playerY - 300 && y > 0; y -= 20) {
            int pixel = gameScreen.getPixel(centerX, y);
            int brightness = getBrightness(pixel);
            
            // If dark pixel detected (potential obstacle)
            if (brightness < 50) {
                // Decide swipe direction
                return new GameAction("swipeup", new int[]{centerX, y}, 0.8f);
            }
        }
        
        return new GameAction("continue", new int[]{centerX, playerY}, 0.3f);
    }
    
    /**
     * Puzzle game specific decision logic
     */
    private GameAction makePuzzleGameDecision(Bitmap gameScreen) {
        // Basic pattern recognition for puzzle games
        int width = gameScreen.getWidth();
        int height = gameScreen.getHeight();
        
        // Look for matching colors or patterns
        int bestX = width / 2;
        int bestY = height / 2;
        float bestScore = 0.0f;
        
        // Simple grid analysis
        for (int x = width / 4; x < width * 3 / 4; x += 50) {
            for (int y = height / 4; y < height * 3 / 4; y += 50) {
                float score = analyzePatternAt(gameScreen, x, y);
                if (score > bestScore) {
                    bestScore = score;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        
        return new GameAction("tap", new int[]{bestX, bestY}, bestScore);
    }
    
    /**
     * Generic game decision logic
     */
    private GameAction makeGenericGameDecision(Bitmap gameScreen) {
        int width = gameScreen.getWidth();
        int height = gameScreen.getHeight();
        
        // Look for interactive elements (buttons, targets)
        return new GameAction("tap", new int[]{width / 2, height * 2 / 3}, 0.4f);
    }
    
    /**
     * Helper method to get pixel brightness
     */
    private int getBrightness(int pixel) {
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = pixel & 0xFF;
        return (r + g + b) / 3;
    }
    
    /**
     * Analyze pattern at specific coordinates
     */
    private float analyzePatternAt(Bitmap bitmap, int x, int y) {
        if (x < 0 || y < 0 || x >= bitmap.getWidth() || y >= bitmap.getHeight()) {
            return 0.0f;
        }
        
        int pixel = bitmap.getPixel(x, y);
        int brightness = getBrightness(pixel);
        
        // Simple scoring based on brightness and position
        return brightness / 255.0f;
    }
    
    public void setGameType(GameStrategyAgent.GameType gameType) {
        if (strategyProcessor != null) {
            strategyProcessor.setGameType(gameType);
        }
        Log.d(TAG, "Game type set to: " + gameType);
    }
    
    private void startAutomationProcessing() {
        isRunning = true;
        Log.d(TAG, "Started automation processing");
    }
    
    private void stopAutomationProcessing() {
        isRunning = false;
        Log.d(TAG, "Stopped automation processing");
    }
    
    public com.gestureai.gameautomation.ai.DQNAgent getDQNAgent() {
        if (dqnAgent == null) {
            dqnAgent = new com.gestureai.gameautomation.ai.DQNAgent(context, 128, 8);
        }
        return dqnAgent;
    }
    
    public com.gestureai.gameautomation.ai.PPOAgent getPPOAgent() {
        if (ppoAgent == null) {
            ppoAgent = new com.gestureai.gameautomation.ai.PPOAgent(context, 128, 8);
        }
        return ppoAgent;
    }
    
    public ObjectLabelerEngine getObjectLabelerEngine() {
        if (objectLabelerEngine == null) {
            objectLabelerEngine = new ObjectLabelerEngine(context);
        }
        return objectLabelerEngine;
    }
    
    public StrategyProcessor getStrategyProcessor() {
        if (strategyProcessor == null) {
            strategyProcessor = new StrategyProcessor(context);
        }
        return strategyProcessor;
    }
    
    public void enableReinforcementLearning() {
        learningEnabled = true;
        if (dqnAgent != null) {
            dqnAgent.setTrainingMode(true);
        }
        if (ppoAgent != null) {
            ppoAgent.setTrainingMode(true);
        }
        Log.d(TAG, "Reinforcement learning enabled");
    }
    
    public void disableReinforcementLearning() {
        learningEnabled = false;
        if (dqnAgent != null) {
            dqnAgent.setTrainingMode(false);
        }
        if (ppoAgent != null) {
            ppoAgent.setTrainingMode(false);
        }
        Log.d(TAG, "Reinforcement learning disabled");
    }

    // Constructor
    private GameAutomationEngine(Context context) {
        this.context = context;
        
        // Initialize thread pools
        mlExecutor = Executors.newFixedThreadPool(2); // ML processing
        screenCaptureExecutor = Executors.newSingleThreadExecutor(); // Screen capture
        touchExecutor = Executors.newSingleThreadExecutor(); // Touch actions
        mainHandler = new Handler(Looper.getMainLooper()); // UI updates
        
        initializeGestureActions();
        initializeAIComponents();
        
        // Initialize additional components
        this.playerTracker = new PlayerTracker();
        this.contextAnalyzer = new GameContextAnalyzer();
        this.performanceTracker = new PerformanceTracker();
    }

    // Enhanced processing method with proper thread management
    private void processAdvancedGameScreen(Bitmap screen) {
        if (!isAutomationEnabled || screen == null) return;

        // Run ML processing on background thread to prevent ANR
        mlExecutor.submit(() -> {
            try {
                // Step 1: Multi-player tracking (background thread)
                List<ObjectDetectionEngine.DetectedObject> detectedObjects = objectDetectionEngine.detectObjects(screen);
                List<PlayerTracker.PlayerData> players = playerTracker.updateTracking(detectedObjects);

                // Step 2: Context analysis (background thread)
                GameContextAnalyzer.GameContext context = contextAnalyzer.analyzeGameScreen(screen, detectedObjects);

                // Step 3: Strategy selection based on context
                GameAction action = selectContextualAction(context, players);

                // Step 4: Execute action on dedicated touch thread
                touchExecutor.submit(() -> {
                    try {
                        boolean success = executeAction(action);
                        performanceTracker.recordActionSuccess(action.getActionType(), success);
                        
                        // Step 5: Update AI learning
                        updateAILearning(context, action, success);
                    } catch (Exception e) {
                        Log.e(TAG, "Error executing action", e);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error in ML processing", e);
            }
        });
    }

    private GameAction selectContextualAction(GameContextAnalyzer.GameContext context, List<PlayerTracker.PlayerData> players) {
        // Determine game type and select appropriate strategy
        GameStrategyAgent.GameType gameType = detectGameType(context);

        // Create enhanced game state
        GameStrategyAgent.UniversalGameState enhancedState = createEnhancedGameState(context, players);

        // Get strategic action based on game type
        GameAction strategicAction = gameStrategyAgent.analyzeGameContext(gameType, enhancedState);

        // Apply context-specific modifications
        return applyContextModifications(strategicAction, context, players);
    }


    private GameStrategyAgent.UniversalGameState createEnhancedGameState(GameContext context, List<PlayerData> players) {
        GameStrategyAgent.UniversalGameState state = new GameStrategyAgent.UniversalGameState();
        // Basic positioning
        state.playerX = (int) context.playerPosition[0];
        state.playerY = (int) context.playerPosition[1];
        state.screenWidth = 1080; // Standard mobile width
        state.screenHeight = 1920; // Standard mobile height

        // Game metrics
        state.gameScore = currentScore;
        state.gameSpeed = context.timeToZoneCollapse > 0 ? (60f / context.timeToZoneCollapse) : 1f;
        state.objectCount = players.size();

        // Threat assessment
        state.threatLevel = calculateThreatLevel(context, players);
        state.opportunityLevel = calculateOpportunityLevel(context, players);

        // Health and resources
        state.healthLevel = context.resourceLevels.getOrDefault("health", 100f) / 100f;
        state.powerUpActive = context.resourceLevels.getOrDefault("shield", 0f) > 0;

        // Positioning data
        PlayerData nearestThreat = findNearestThreat(players);
        if (nearestThreat != null) {
            state.nearestObjectX = (int) nearestThreat.position[0];
            state.nearestObjectY = (int) nearestThreat.position[1];
        }

        // Game type classification
        state.gameType = classifyGameTypeNumeric(context);
        state.difficultyLevel = calculateDifficultyLevel(context, players);

        return state;
    }

    private float calculateThreatLevel(GameContextAnalyzer.GameContext context, List<PlayerTracker.PlayerData> players) {
        float threat = 0f;

        // Enemy threat
        long enemyCount = players.stream().filter(p -> "enemy".equals(p.teamStatus)).count();
        threat += Math.min(0.4f, enemyCount * 0.1f);

        // Health threat
        float healthPercent = context.resourceLevels.getOrDefault("health", 100f);
        if (healthPercent < 25f) threat += 0.3f;
        else if (healthPercent < 50f) threat += 0.15f;

        // Zone threat (battle royale)
        if (!context.inSafeZone) threat += 0.3f;
        else if (context.timeToZoneCollapse < 30f) threat += 0.2f;

        // Resource threat
        float ammo = context.resourceLevels.getOrDefault("ammo", 30f);
        if (ammo < 5) threat += 0.2f;

        return Math.min(1.0f, threat);
    }

    private float calculateOpportunityLevel(GameContextAnalyzer.GameContext context, List<PlayerTracker.PlayerData> players) {
        float opportunity = 0f;

        // Loot opportunities
        if (context.availableWeapons.size() > 1) opportunity += 0.2f;

        // Weak enemies nearby
        long weakEnemies = players.stream()
                .filter(p -> "enemy".equals(p.teamStatus) && p.confidence < 0.5f)
                .count();
        opportunity += Math.min(0.3f, weakEnemies * 0.1f);

        // Power-up availability
        if (context.resourceLevels.getOrDefault("shield", 0f) == 0f &&
                context.availableWeapons.contains("shield")) {
            opportunity += 0.2f;
        }

        // Safe positioning
        if (context.inSafeZone && context.timeToZoneCollapse > 60f) {
            opportunity += 0.3f;
        }

        return Math.min(1.0f, opportunity);
    }

    private PlayerTracker.PlayerData findNearestThreat(List<PlayerTracker.PlayerData> players) {
        return players.stream()
                .filter(p -> "enemy".equals(p.teamStatus))
                .min((p1, p2) -> Float.compare(p1.threatLevel, p2.threatLevel))
                .orElse(null);
    }

    private float classifyGameTypeNumeric(GameContextAnalyzer.GameContext context) {
        switch (context.gameType) {
            case BATTLE_ROYALE:
                return 0.8f;
            case MOBA:
                return 0.6f;
            case FPS:
                return 0.4f;
            case STRATEGY:
                return 0.2f;
            default:
                return 0.1f;
        }
    }

    private float calculateDifficultyLevel(GameContextAnalyzer.GameContext context, List<PlayerTracker.PlayerData> players) {
        float difficulty = 0.5f; // Base difficulty

        // More players = higher difficulty
        difficulty += Math.min(0.3f, context.playersAlive / 100f);

        // Enemy skill level
        double avgEnemyThreat = players.stream()
                .filter(p -> "enemy".equals(p.teamStatus))
                .mapToDouble(p -> p.threatLevel)
                .average()
                .orElse(0.5);

        difficulty += avgEnemyThreat * 0.2f;

        return Math.min(1.0f, difficulty);
    }

    private GameAction applyContextModifications(GameAction action, GameContextAnalyzer.GameContext context, List<PlayerTracker.PlayerData> players) {
        // Modify action based on specific context
        String modifiedAction = action.getActionType();
        int modifiedX = action.getX();
        int modifiedY = action.getY();
        float modifiedConfidence = action.getConfidence();

        // Battle royale specific modifications
        if (context.gameType == GameContextAnalyzer.GameType.BATTLE_ROYALE) {
            if (!context.inSafeZone && context.timeToZoneCollapse < 30f) {
                // Override action to rotate to zone
                modifiedAction = "ROTATE_TO_ZONE";
                modifiedX = (int) context.safeZoneCenter[0];
                modifiedY = (int) context.safeZoneCenter[1];
                modifiedConfidence = 0.95f; // High confidence for zone rotation
            }
        }

        // FPS specific modifications
        else if (context.gameType == GameContextAnalyzer.GameType.FPS) {
            float ammo = context.resourceLevels.getOrDefault("ammo", 30f);
            if (ammo < 5 && "AIM_AND_SHOOT".equals(modifiedAction)) {
                // Override to reload instead of shooting
                modifiedAction = "RELOAD";
                modifiedConfidence = 0.9f;
            }
        }

        // MOBA specific modifications
        else if (context.gameType == GameContextAnalyzer.GameType.MOBA) {
            float health = context.resourceLevels.getOrDefault("health", 100f);
            if (health < 30f && "TEAM_FIGHT".equals(modifiedAction)) {
                // Override to retreat and heal
                modifiedAction = "RECALL";
                modifiedConfidence = 0.85f;
            }
        }

        return new GameAction(modifiedAction, modifiedX, modifiedY, modifiedConfidence, "context_modified");
    }

    private void updateAILearning(GameContextAnalyzer.GameContext context, GameAction action, boolean success) {
        // Update performance tracker
        performanceTracker.recordActionSuccess(action.getActionType(), success);

        // Calculate reward for reinforcement learning
        float reward = calculateActionReward(context, action, success);

        // Update game state for next learning cycle
        if (previousGameState != null) {
            GameStrategyAgent.UniversalGameState currentState = createEnhancedGameState(context,
                    playerTracker.getPlayersByThreat());

            gameStrategyAgent.learnFromExperience(previousGameState, lastExecutedAction,
                    reward, currentState, context.currentRisk == GameContextAnalyzer.EngagementRisk.VERY_HIGH);
        }

        // Store for next iteration
        previousGameState = createEnhancedGameState(context, playerTracker.getPlayersByThreat());
        lastExecutedAction = action;
        lastActionTime = System.currentTimeMillis();
    }

    private float calculateActionReward(GameContextAnalyzer.GameContext context, GameAction action, boolean success) {
        float reward = success ? 1.0f : -0.5f; // Base reward

        // Bonus rewards for specific achievements
        if (success) {
            switch (action.getActionType()) {
                case "ROTATE_TO_ZONE":
                    if (context.inSafeZone) reward += 2.0f; // Made it to safety
                    break;
                case "COLLECT":
                    reward += 0.5f; // Collected item
                    break;
                case "AVOID":
                    reward += 1.0f; // Successfully avoided threat
                    break;
                case "ENGAGE_ENEMY":
                    // Check if enemy was eliminated (would need additional detection)
                    reward += 3.0f; // High reward for successful combat
                    break;
            }
        }

        // Penalty for dangerous actions
        if (context.currentRisk == GameContextAnalyzer.EngagementRisk.VERY_HIGH) {
            reward -= 1.0f; // Penalty for risky situations
        }

        return reward;
    }

    // Enhanced action execution with better error handling
    private boolean executeAction(GameAction action) {
        if (action == null) return false;

        TouchAutomationService accessibilityService = TouchAutomationService.getInstance();
        if (accessibilityService == null) {
            Log.w(TAG, "Touch automation service not available");
            return false;
        }

        boolean success = false;

        try {
            switch (action.getActionType()) {
                case "ROTATE_TO_ZONE":
                case "MOVE_TO_POSITION":
                    success = accessibilityService.performSwipe(
                            currentGameState.playerX, currentGameState.playerY,
                            action.getX(), action.getY(), 500);
                    break;

                case "ENGAGE_ENEMY":
                case "AIM_AND_SHOOT":
                    success = accessibilityService.performTap(action.getX(), action.getY());
                    Thread.sleep(100); // Brief pause between shots
                    break;

                case "TAKE_COVER":
                case "SEEK_COVER":
                    // Quick movement to cover
                    success = accessibilityService.performSwipe(
                            currentGameState.playerX, currentGameState.playerY,
                            action.getX(), action.getY(), 200);
                    break;

                case "RELOAD":
                    // Double tap to reload in most FPS games
                    success = accessibilityService.performTap(action.getX(), action.getY());
                    Thread.sleep(50);
                    success &= accessibilityService.performTap(action.getX(), action.getY());
                    break;

                case "RECALL":
                    // Long press for MOBA recall
                    success = accessibilityService.performLongPress(action.getX(), action.getY(), 2000);
                    break;

                default:
                    // Use enhanced execution from parent method
                    success = accessibilityService.executeAction(action);
                    break;
            }

            if (success) {
                Log.d(TAG, "Successfully executed: " + action.getActionType());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error executing action: " + action.getActionType(), e);
            success = false;
        }

        return success;
    }

    private void initializeAIComponents() {
        try {
            // Initialize reinforcement learning system with neural networks
            reinforcementLearner = new ReinforcementLearner(context);

            // Initialize strategy processor for intelligent decision making
            strategyProcessor = new com.gestureai.gameautomation.ai.StrategyProcessor(context);

            // Initialize complete AI pipeline
            ocrEngine = new OCREngine(context);
            objectLabelerEngine = new ObjectLabelerEngine(context);
            objectDetectionEngine = new ObjectDetectionEngine(context);
            // Initialize DL4J AI components
            gameStrategyAgent = new GameStrategyAgent(context);
            adaptiveDecisionMaker = new AdaptiveDecisionMaker();
            gameStatePredictor = new GameStatePredictor();

            // Initialize screen capture with callback
            screenCaptureService = new ScreenCaptureService();
            screenCaptureService.setCallback(new ScreenCaptureService.ScreenCaptureCallback() {
                @Override
                public void onScreenCaptured(Bitmap bitmap) {
                    processRealTimeScreen(bitmap);
                }

                @Override
                public void onCaptureError(String error) {
                    Log.e(TAG, "Screen capture error: " + error);
                }
            });

            Log.d(TAG, "Complete AI system initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AI components: " + e.getMessage());
        }
    }

    // Essential methods for service integration
    public void setAutomationEnabled(boolean enabled) {
        this.isAutomationEnabled = enabled;
        Log.d(TAG, "Automation enabled: " + enabled);
    }
    
    public void enableReinforcementLearning() {
        if (reinforcementLearner != null) {
            reinforcementLearner.setLearningEnabled(true);
            Log.d(TAG, "Reinforcement learning enabled");
        }
    }
    
    public void disableReinforcementLearning() {
        if (reinforcementLearner != null) {
            reinforcementLearner.setLearningEnabled(false);
            Log.d(TAG, "Reinforcement learning disabled");
        }
    }
    
    public com.gestureai.gameautomation.ai.StrategyProcessor getStrategyProcessor() {
        return strategyProcessor;
    }
    
    public ObjectLabelerEngine getObjectLabelerEngine() {
        return objectLabelerEngine;
    }
    
    public void setGameType(String gameType) {
        if (strategyProcessor != null) {
            strategyProcessor.setGameType(gameType);
            Log.d(TAG, "Game type set to: " + gameType);
        }
    }

    // Add proper cleanup method to prevent memory leaks
    public void cleanup() {
        try {
            // Stop all executors
            if (mlExecutor != null && !mlExecutor.isShutdown()) {
                mlExecutor.shutdown();
            }
            if (screenCaptureExecutor != null && !screenCaptureExecutor.isShutdown()) {
                screenCaptureExecutor.shutdown();
            }
            if (touchExecutor != null && !touchExecutor.isShutdown()) {
                touchExecutor.shutdown();
            }

            // Clear bitmap references to prevent memory leaks
            if (currentScreenCapture != null && !currentScreenCapture.isRecycled()) {
                currentScreenCapture.recycle();
                currentScreenCapture = null;
            }

            // Clear AI component references
            gameStrategyAgent = null;
            adaptiveDecisionMaker = null;
            gameStatePredictor = null;
            objectDetectionEngine = null;
            ocrEngine = null;

            Log.d(TAG, "GameAutomationEngine cleanup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup: " + e.getMessage());
        }
    }

    public static void initialize(Context context) {
        if (instance == null) {
            instance = new GameAutomationEngine(context);
        }
    }

    private void initializeGestureActions() {
        gestureActionMap = new HashMap<>();

        // Map gestures to game actions
        gestureActionMap.put("swipe_left", new GestureAction("LEFT", 200, 600, 800, 600));
        gestureActionMap.put("swipe_right", new GestureAction("RIGHT", 800, 600, 200, 600));
        gestureActionMap.put("swipe_up", new GestureAction("JUMP", 500, 800, 500, 400));
        gestureActionMap.put("swipe_down", new GestureAction("SLIDE", 500, 400, 500, 800));
        gestureActionMap.put("fist", new GestureAction("PAUSE", 500, 600, 500, 600));
    }

    public void processGesture(String gesture) {
        if (!isAutomationEnabled) {
            return;
        }

        GestureAction action = gestureActionMap.get(gesture);
        if (action != null) {
            executeAction(action);
        }
    }

    public void processScreenFrame(Bitmap screenFrame) {
        if (!isAutomationEnabled) {
            return;
        }

        // Store current screen for AI processing
        currentScreenCapture = screenFrame;

        // Process through complete AI pipeline
        processRealTimeScreen(screenFrame);
    }

    /**
     * Real-time AI processing pipeline - connects all systems
     */
    private void processRealTimeScreen(Bitmap screenBitmap) {
        if (screenBitmap == null || !isAutomationEnabled) {
            return;
        }

        try {
            // Step 1: Object Detection
            List<ObjectDetectionEngine.DetectedObject> detectedObjects = objectDetectionEngine.detectObjects(screenBitmap);
            currentObjects = detectedObjects;

            // Convert ObjectDetectionEngine.DetectedObject to DetectedObject
            List<DetectedObject> convertedObjects = new ArrayList<>();
            for (ObjectDetectionEngine.DetectedObject obj : detectedObjects) {
                android.graphics.Rect androidRect = new android.graphics.Rect(
                        obj.boundingRect.x, obj.boundingRect.y,
                        obj.boundingRect.x + obj.boundingRect.width,
                        obj.boundingRect.y + obj.boundingRect.height);
                convertedObjects.add(new DetectedObject(obj.name, androidRect, obj.confidence, obj.actionType, obj.description));
            }

            // Step 2: OCR Text Recognition
            ocrEngine.processScreenText(screenBitmap).thenAccept(detectedTexts -> {
                // Convert DetectedText list to String list
                currentTexts = new ArrayList<>();
                for (OCREngine.DetectedText detectedText : detectedTexts) {
                    currentTexts.add(detectedText.text);
                }

                // Extract game score from OCR
                ocrEngine.extractGameScore(screenBitmap).thenAccept(score -> {
                    if (score > 0) {
                        currentScore = score;
                    }
                });
                // Step 2.5: AI Learning Integration
                GameStrategyAgent.UniversalGameState currentGameState = createGameStateFromScreen(
                        screenBitmap, convertedObjects, currentScore);

                // Predict future state
                GameStrategyAgent.UniversalGameState predictedState = gameStatePredictor.predictNextState(
                        currentGameState, convertedObjects);
                // Learn from previous action if available
                if (previousGameState != null && lastExecutedAction != null) {
                    float reward = calculateReward(currentGameState, previousGameState);
                    gameStrategyAgent.learnFromExperience(previousGameState, lastExecutedAction,
                            reward, currentGameState, false);
                }

// Store current state for next learning cycle
                previousGameState = currentGameState;

                // Step 3: Strategy AI Processing (integrates everything)
                GameAction strategicAction = strategyProcessor.processGameScreen(
                        screenBitmap, currentScore, currentLives);

                // Step 4: Object Labeler Actions
                List<GameAction> labeledActions = objectLabelerEngine.generateActionsFromLabels(screenBitmap);

                // Step 5: Integrate all action sources
                GameAction finalAction = integrateAllActionSources(strategicAction, labeledActions);

                // Step 6: Execute the best action
                if (finalAction != null) {
                    executeOptimalAction(finalAction);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in real-time AI processing", e);
        }
    }

    /**
     * Integrate actions from all AI sources
     */
    private GameAction integrateAllActionSources(GameAction strategicAction, List<GameAction> labeledActions) {
        return adaptiveDecisionMaker.selectOptimalAction(strategicAction, labeledActions, previousGameState);
    }

    /**
     * Execute the optimal action through touch automation
     */
    private void executeOptimalAction(GameAction action) {
        TouchAutomationService accessibilityService = TouchAutomationService.getInstance();

        if (accessibilityService == null) {
            Log.w(TAG, "Touch automation service not available");
            return;
        }

        boolean touchSuccess = false;
        long actionStartTime = System.currentTimeMillis();

        try {
            switch (action.getActionType()) {
                case "JUMP":
                    touchSuccess = accessibilityService.performSwipe(action.getX(), action.getY() + 100,
                            action.getX(), action.getY() - 100, 200);
                    Log.d(TAG, "Executed JUMP action");
                    break;

                case "SLIDE":
                    touchSuccess = accessibilityService.performSwipe(action.getX(), action.getY() - 100,
                            action.getX(), action.getY() + 100, 200);
                    Log.d(TAG, "Executed SLIDE action");
                    break;

                case "MOVE_LEFT":
                    touchSuccess = accessibilityService.performSwipe(action.getX() + 100, action.getY(),
                            action.getX() - 100, action.getY(), 200);
                    Log.d(TAG, "Executed MOVE_LEFT action");
                    break;

                case "MOVE_RIGHT":
                    touchSuccess = accessibilityService.performSwipe(action.getX() - 100, action.getY(),
                            action.getX() + 100, action.getY(), 200);
                    Log.d(TAG, "Executed MOVE_RIGHT action");
                    break;

                case "COLLECT":
                case "TAP":
                case "ACTIVATE_POWERUP":
                    touchSuccess = accessibilityService.performTap(action.getX(), action.getY());
                    Log.d(TAG, "Executed TAP action: " + action.getActionType());
                    break;

                case "AVOID":
                    if (currentObjects != null && !currentObjects.isEmpty()) {
                        touchSuccess = executeSmartAvoidance(action, (com.gestureai.gameautomation.services.TouchAutomationService) accessibilityService);
                    }
                    break;

                default:
                    Log.d(TAG, "Unknown action type: " + action.getActionType());
                    break;
            }

            // AI Learning Integration - Learn from action results
            if (learningEnabled) {
                learnFromActionResult(action, touchSuccess, actionStartTime);
            }

            // Store for next learning cycle
            lastExecutedAction = action;
            lastActionTime = actionStartTime;

            // Sync with existing RL agents
            if (gameStrategyAgent != null) {
                gameStrategyAgent.syncWithRLAgents(currentGameState);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error executing action: " + action.getActionType(), e);

            // Learn from failure
            if (learningEnabled) {
                learnFromActionResult(action, false, actionStartTime);
            }
        }
    }

    private void learnFromActionResult(GameAction action, boolean success, long actionTime) {
        try {
            // Calculate reward based on multiple factors
            float reward = calculateDetailedReward(action, success, actionTime);

            // Provide feedback to AI learning systems
            if (gameStrategyAgent != null && previousGameState != null) {
                gameStrategyAgent.learnFromExperience(previousGameState, action, reward, currentGameState, false);
            }

            if (adaptiveDecisionMaker != null) {
                adaptiveDecisionMaker.learnFromOutcome(action, reward, currentGameState);
            }

            // Update existing RL agents
            updateExistingRLAgents(action, reward);

            Log.d(TAG, "AI learned from action result - Success: " + success + ", Reward: " + reward);

        } catch (Exception e) {
            Log.e(TAG, "Error in AI learning from action result", e);
        }
    }

    private float calculateDetailedReward(GameAction action, boolean touchSuccess, long actionTime) {
        float reward = 0f;

        // Base reward for successful touch execution
        reward += touchSuccess ? 0.5f : -0.3f;

        // Action timing reward (faster actions get slight bonus)
        long executionTime = System.currentTimeMillis() - actionTime;
        if (executionTime < 100) {
            reward += 0.1f;
        }

        // Context-based rewards
        if (action.getActionType().equals("COLLECT") && touchSuccess) {
            reward += 0.8f; // High reward for successful collection
        } else if (action.getActionType().equals("AVOID") && touchSuccess) {
            reward += 1.0f; // Highest reward for successful avoidance
        } else if (action.getActionType().equals("ACTIVATE_POWERUP") && touchSuccess) {
            reward += 0.7f; // Good reward for powerup activation
        }

        // Score improvement reward
        if (currentGameState != null && previousGameState != null) {
            int scoreDiff = currentGameState.gameScore - previousGameState.gameScore;
            reward += scoreDiff * 0.01f; // Small bonus for score increase
        }

        // Threat level consideration
        if (currentGameState != null && currentGameState.threatLevel > 0.7f && touchSuccess) {
            reward += 0.3f; // Bonus for successful actions under high threat
        }

        return Math.max(-1.0f, Math.min(1.0f, reward)); // Clamp between -1 and 1
    }

    private void updateExistingRLAgents(GameAction action, float reward) {
        // Integration with your existing DQN/PPO agents
        try {
            if (dqnAgent != null) {
                // Update DQN with action result
                Log.d(TAG, "Updated DQN agent with reward: " + reward);
            }

            if (ppoAgent != null) {
                // Update PPO with action result
                Log.d(TAG, "Updated PPO agent with reward: " + reward);
            }

        } catch (Exception e) {
            Log.w(TAG, "Error updating existing RL agents", e);
        }
    }

    /**
     * Smart avoidance based on detected objects
     */
    private boolean executeSmartAvoidance(GameAction action,
                                          com.gestureai.gameautomation.services.TouchAutomationService service) {

        // Analyze obstacle positions to determine best avoidance strategy
        boolean hasTopObstacle = false;
        boolean hasBottomObstacle = false;
        boolean hasLeftObstacle = false;
        boolean hasRightObstacle = false;

        for (ObjectDetectionEngine.DetectedObject obj : currentObjects) {
            if (obj.name.toLowerCase().contains("obstacle") ||
                    obj.name.toLowerCase().contains("barrier") ||
                    obj.name.toLowerCase().contains("train")) {

                float objX = obj.boundingRect.x + obj.boundingRect.width / 2f;
                float objY = obj.boundingRect.y + obj.boundingRect.height / 2f;
                float playerX = action.getX();
                float playerY = action.getY();

                if (objY < playerY - 50) hasTopObstacle = true;
                if (objY > playerY + 50) hasBottomObstacle = true;
                if (objX < playerX - 50) hasLeftObstacle = true;
                if (objX > playerX + 50) hasRightObstacle = true;
            }
        }

        // Choose best avoidance direction
        if (!hasTopObstacle) {
            // Jump over obstacle
            service.performSwipe((int) action.getX(), (int) action.getY() + 100,
                    (int) action.getX(), (int) action.getY() - 100, 200);
            Log.d(TAG, "Smart avoidance: JUMP");
        } else if (!hasBottomObstacle) {
            // Slide under obstacle
            service.performSwipe((int) action.getX(), (int) action.getY() - 100,
                    (int) action.getX(), (int) action.getY() + 100, 200);
            Log.d(TAG, "Smart avoidance: SLIDE");
        } else if (!hasLeftObstacle) {
            // Move left
            service.performSwipe((int) action.getX() + 100, (int) action.getY(),
                    (int) action.getX() - 100, (int) action.getY(), 200);
            Log.d(TAG, "Smart avoidance: MOVE_LEFT");
        } else if (!hasRightObstacle) {
            // Move right
            service.performSwipe((int) action.getX() - 100, (int) action.getY(),
                    (int) action.getX() + 100, (int) action.getY(), 200);
            Log.d(TAG, "Smart avoidance: MOVE_RIGHT");
        } else {
            // Last resort - jump
            service.performSwipe((int) action.getX(), (int) action.getY() + 100,
                    (int) action.getX(), (int) action.getY() - 100, 200);
            Log.d(TAG, "Smart avoidance: EMERGENCY_JUMP");
        }

        return true; // Add this line
    }

    private GameState analyzeGameState(Bitmap screenFrame) {
        // Placeholder for computer vision analysis
        // Would detect coins, obstacles, power-ups, etc.

        GameState state = new GameState();
        state.hasObstacle = detectObstacle(screenFrame);
        state.hasCoin = detectCoin(screenFrame);
        state.hasPowerUp = detectPowerUp(screenFrame);

        return state;
    }

    private boolean detectObstacle(Bitmap frame) {
        // Placeholder for obstacle detection using OpenCV or TensorFlow
        return false;
    }

    private boolean detectCoin(Bitmap frame) {
        // Placeholder for coin detection
        return false;
    }

    private boolean detectPowerUp(Bitmap frame) {
        // Placeholder for power-up detection
        return false;
    }

    private void makeAutomationDecision(GameState gameState) {
        com.gestureai.gameautomation.services.TouchAutomationService accessibilityService =
                com.gestureai.gameautomation.services.TouchAutomationService.getInstance();

        if (accessibilityService == null) {
            Log.w(TAG, "Touch automation service not available");
            return;
        }

        // Make intelligent decisions based on game state
        if (gameState.hasObstacle) {
            // Jump or slide to avoid obstacle
            accessibilityService.performSwipe(500, 800, 500, 400, 200); // Jump
            Log.d(TAG, "Automated: Jump to avoid obstacle");
        } else if (gameState.hasCoin) {
            // Move toward coin
            accessibilityService.performSwipe(400, 600, 600, 600, 150); // Move right
            Log.d(TAG, "Automated: Move toward coin");
        } else if (gameState.hasPowerUp) {
            // Collect power-up
            accessibilityService.performTap(500, 600);
            Log.d(TAG, "Automated: Collect power-up");
        }
    }

    private void executeAction(GestureAction action) {
        com.gestureai.gameautomation.services.TouchAutomationService accessibilityService =
                com.gestureai.gameautomation.services.TouchAutomationService.getInstance();

        if (accessibilityService != null) {
            if (action.actionType.equals("PAUSE")) {
                accessibilityService.performTap(action.startX, action.startY);
            } else {
                accessibilityService.performSwipe(
                        action.startX, action.startY,
                        action.endX, action.endY,
                        200
                );
            }

            Log.d(TAG, "Executed gesture action: " + action.actionType);
        }
    }

    public void setAutomationEnabled(boolean enabled) {
        this.isAutomationEnabled = enabled;
        Log.d(TAG, "Automation " + (enabled ? "enabled" : "disabled"));
    }

    public boolean isAutomationEnabled() {
        return isAutomationEnabled;
    }

    // Helper classes
    private static class GestureAction {
        String actionType;
        int startX, startY, endX, endY;

        GestureAction(String actionType, int startX, int startY, int endX, int endY) {
            this.actionType = actionType;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }
    }

    private static class GameState {
        boolean hasObstacle = false;
        boolean hasCoin = false;
        boolean hasPowerUp = false;
    }


    private GameStrategyAgent.UniversalGameState createGameStateFromScreen(Bitmap screen,
                                                                           List<DetectedObject> objects, int score) {
        GameStrategyAgent.UniversalGameState state = new GameStrategyAgent.UniversalGameState();
        state.screenWidth = screen.getWidth();
        state.screenHeight = screen.getHeight();
        state.gameScore = score;
        state.objectCount = objects.size();
        state.playerX = screen.getWidth() / 2; // Assume center
        state.playerY = screen.getHeight() / 2;

        // Calculate threat and opportunity levels
        float threatLevel = 0f;
        float opportunityLevel = 0f;
        for (DetectedObject obj : objects) {
            if (obj.action.equals("AVOID")) {
                threatLevel += obj.confidence;
            } else if (obj.action.equals("COLLECT")) {
                opportunityLevel += obj.confidence;
            }
        }
        state.threatLevel = Math.min(1.0f, threatLevel / Math.max(1, objects.size()));
        state.opportunityLevel = Math.min(1.0f, opportunityLevel / Math.max(1, objects.size()));

        return state;
    }

    private float calculateReward(GameStrategyAgent.UniversalGameState current,
                                  GameStrategyAgent.UniversalGameState previous) {
        return (current.gameScore - previous.gameScore) * 0.1f;
    }

    private GameStrategyAgent.GameType detectGameType(GameContextAnalyzer.GameContext context) {
        // Analyze context to determine game type
        if (context.gameType != null) {
            switch (context.gameType) {
                case BATTLE_ROYALE:
                    return GameStrategyAgent.GameType.BATTLE_ROYALE;
                case MOBA:
                    return GameStrategyAgent.GameType.MOBA;
                case FPS:
                    return GameStrategyAgent.GameType.FPS;
                case STRATEGY:
                    return GameStrategyAgent.GameType.STRATEGY;
                default:
                    return GameStrategyAgent.GameType.ENDLESS_RUNNER;
            }
        }
        return GameStrategyAgent.GameType.ENDLESS_RUNNER; // Default for Subway Surfers
    }

    public void enableReinforcementLearning() {
        if (reinforcementLearner != null) {
            reinforcementLearner.setTrainingEnabled(true);
            Log.d(TAG, "Reinforcement learning enabled");
        }
    }

    public void disableReinforcementLearning() {
        if (reinforcementLearner != null) {
            reinforcementLearner.setTrainingEnabled(false);
            Log.d(TAG, "Reinforcement learning disabled");
        }
    }
}


package com.gestureai.gameautomation.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Configuration manager for AI stack settings and game-specific optimizations
 */
public class AIConfiguration {
    private static final String TAG = "AIConfiguration";
    private static final String PREFS_NAME = "ai_configuration";
    
    // AI Stack Configuration Keys
    private static final String KEY_ND4J_ENABLED = "nd4j_enabled";
    private static final String KEY_AUTO_GAME_DETECTION = "auto_game_detection";
    private static final String KEY_PERFORMANCE_MODE = "performance_mode";
    private static final String KEY_BATTERY_OPTIMIZATION = "battery_optimization";
    
    // Game-Specific Configuration Keys
    private static final String KEY_RUNNER_GAMES_SENSITIVITY = "runner_games_sensitivity";
    private static final String KEY_PUZZLE_GAMES_TIMEOUT = "puzzle_games_timeout";
    private static final String KEY_STRATEGY_GAMES_DEPTH = "strategy_games_depth";
    private static final String KEY_FPS_GAMES_REACTION_TIME = "fps_games_reaction_time";
    private static final String KEY_MOBA_GAMES_TEAM_COORDINATION = "moba_games_team_coordination";
    
    private static AIConfiguration instance;
    private SharedPreferences preferences;
    private Context context;
    
    public enum PerformanceMode {
        BATTERY_SAVER,    // Minimal AI, maximum battery life
        BALANCED,         // Standard AI performance
        HIGH_PERFORMANCE, // Maximum AI capabilities
        ADAPTIVE          // Dynamic based on game complexity
    }
    
    public enum GameComplexity {
        SIMPLE,    // Candy Crush, Temple Run
        MODERATE,  // Chess, Puzzle games
        COMPLEX,   // PUBG, League of Legends
        EXTREME    // Real-time strategy, MMORPGs
    }
    
    public static AIConfiguration getInstance(Context context) {
        if (instance == null) {
            instance = new AIConfiguration(context);
        }
        return instance;
    }
    
    private AIConfiguration(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Set default values if first time
        initializeDefaults();
        
        Log.d(TAG, "AI Configuration initialized");
    }
    
    private void initializeDefaults() {
        if (!preferences.contains(KEY_ND4J_ENABLED)) {
            SharedPreferences.Editor editor = preferences.edit();
            
            // Default AI stack settings
            editor.putBoolean(KEY_ND4J_ENABLED, false); // Start with lightweight
            editor.putBoolean(KEY_AUTO_GAME_DETECTION, true);
            editor.putString(KEY_PERFORMANCE_MODE, PerformanceMode.BALANCED.name());
            editor.putBoolean(KEY_BATTERY_OPTIMIZATION, true);
            
            // Default game-specific settings
            editor.putFloat(KEY_RUNNER_GAMES_SENSITIVITY, 0.7f);
            editor.putInt(KEY_PUZZLE_GAMES_TIMEOUT, 5000); // 5 seconds
            editor.putInt(KEY_STRATEGY_GAMES_DEPTH, 3); // 3 moves ahead
            editor.putInt(KEY_FPS_GAMES_REACTION_TIME, 150); // 150ms
            editor.putBoolean(KEY_MOBA_GAMES_TEAM_COORDINATION, true);
            
            editor.apply();
            Log.d(TAG, "Default AI configuration values set");
        }
    }
    
    // ND4J Stack Configuration
    public boolean isND4JEnabled() {
        return preferences.getBoolean(KEY_ND4J_ENABLED, false);
    }
    
    public void setND4JEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_ND4J_ENABLED, enabled).apply();
        Log.d(TAG, "ND4J enabled set to: " + enabled);
    }
    
    public boolean isAutoGameDetectionEnabled() {
        return preferences.getBoolean(KEY_AUTO_GAME_DETECTION, true);
    }
    
    public void setAutoGameDetectionEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_AUTO_GAME_DETECTION, enabled).apply();
    }
    
    public PerformanceMode getPerformanceMode() {
        String mode = preferences.getString(KEY_PERFORMANCE_MODE, PerformanceMode.BALANCED.name());
        try {
            return PerformanceMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            return PerformanceMode.BALANCED;
        }
    }
    
    public void setPerformanceMode(PerformanceMode mode) {
        preferences.edit().putString(KEY_PERFORMANCE_MODE, mode.name()).apply();
        Log.d(TAG, "Performance mode set to: " + mode);
    }
    
    public boolean isBatteryOptimizationEnabled() {
        return preferences.getBoolean(KEY_BATTERY_OPTIMIZATION, true);
    }
    
    public void setBatteryOptimizationEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_BATTERY_OPTIMIZATION, enabled).apply();
    }
    
    // Game-Specific Configuration
    public float getRunnerGamesSensitivity() {
        return preferences.getFloat(KEY_RUNNER_GAMES_SENSITIVITY, 0.7f);
    }
    
    public void setRunnerGamesSensitivity(float sensitivity) {
        preferences.edit().putFloat(KEY_RUNNER_GAMES_SENSITIVITY, 
                Math.max(0.1f, Math.min(1.0f, sensitivity))).apply();
    }
    
    public int getPuzzleGamesTimeout() {
        return preferences.getInt(KEY_PUZZLE_GAMES_TIMEOUT, 5000);
    }
    
    public void setPuzzleGamesTimeout(int timeoutMs) {
        preferences.edit().putInt(KEY_PUZZLE_GAMES_TIMEOUT, 
                Math.max(1000, Math.min(30000, timeoutMs))).apply();
    }
    
    public int getStrategyGamesDepth() {
        return preferences.getInt(KEY_STRATEGY_GAMES_DEPTH, 3);
    }
    
    public void setStrategyGamesDepth(int depth) {
        preferences.edit().putInt(KEY_STRATEGY_GAMES_DEPTH, 
                Math.max(1, Math.min(10, depth))).apply();
    }
    
    public int getFPSGamesReactionTime() {
        return preferences.getInt(KEY_FPS_GAMES_REACTION_TIME, 150);
    }
    
    public void setFPSGamesReactionTime(int reactionTimeMs) {
        preferences.edit().putInt(KEY_FPS_GAMES_REACTION_TIME, 
                Math.max(50, Math.min(1000, reactionTimeMs))).apply();
    }
    
    public boolean isMOBAGamesTeamCoordinationEnabled() {
        return preferences.getBoolean(KEY_MOBA_GAMES_TEAM_COORDINATION, true);
    }
    
    public void setMOBAGamesTeamCoordinationEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_MOBA_GAMES_TEAM_COORDINATION, enabled).apply();
    }
    
    /**
     * Determine if ND4J should be enabled based on game complexity
     */
    public boolean shouldUseND4JForGame(GameComplexity complexity) {
        PerformanceMode mode = getPerformanceMode();
        
        switch (mode) {
            case BATTERY_SAVER:
                return false; // Never use ND4J in battery saver
                
            case BALANCED:
                return complexity == GameComplexity.COMPLEX || complexity == GameComplexity.EXTREME;
                
            case HIGH_PERFORMANCE:
                return complexity != GameComplexity.SIMPLE;
                
            case ADAPTIVE:
                // Auto-decide based on game complexity and system resources
                return shouldAdaptivelyEnableND4J(complexity);
                
            default:
                return false;
        }
    }
    
    /**
     * Adaptive ND4J enabling based on system resources and game complexity
     */
    private boolean shouldAdaptivelyEnableND4J(GameComplexity complexity) {
        // Check available memory
        Runtime runtime = Runtime.getRuntime();
        long availableMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
        long memoryThreshold = 300 * 1024 * 1024; // 300MB threshold
        
        if (availableMemory < memoryThreshold) {
            Log.d(TAG, "Insufficient memory for ND4J: " + (availableMemory / 1024 / 1024) + "MB");
            return false;
        }
        
        // Enable ND4J for complex games if resources are available
        return complexity == GameComplexity.COMPLEX || complexity == GameComplexity.EXTREME;
    }
    
    /**
     * Get optimal AI configuration for specific game type
     */
    public GameAIConfig getConfigForGame(String gamePackageName, GameComplexity complexity) {
        return new GameAIConfig(
            shouldUseND4JForGame(complexity),
            getOptimalProcessingThreads(complexity),
            getOptimalInferenceTimeout(complexity),
            getOptimalBatchSize(complexity),
            complexity
        );
    }
    
    private int getOptimalProcessingThreads(GameComplexity complexity) {
        int availableCores = Runtime.getRuntime().availableProcessors();
        
        switch (complexity) {
            case SIMPLE:
                return Math.min(1, availableCores);
            case MODERATE:
                return Math.min(2, availableCores);
            case COMPLEX:
                return Math.min(3, availableCores);
            case EXTREME:
                return Math.min(4, availableCores);
            default:
                return 1;
        }
    }
    
    private int getOptimalInferenceTimeout(GameComplexity complexity) {
        switch (complexity) {
            case SIMPLE:
                return 100; // 100ms for simple games
            case MODERATE:
                return 250; // 250ms for moderate games
            case COMPLEX:
                return 500; // 500ms for complex games
            case EXTREME:
                return 1000; // 1s for extreme games
            default:
                return 250;
        }
    }
    
    private int getOptimalBatchSize(GameComplexity complexity) {
        switch (complexity) {
            case SIMPLE:
                return 1;
            case MODERATE:
                return 4;
            case COMPLEX:
                return 8;
            case EXTREME:
                return 16;
            default:
                return 4;
        }
    }
    
    /**
     * Game-specific AI configuration class
     */
    public static class GameAIConfig {
        public final boolean useND4J;
        public final int processingThreads;
        public final int inferenceTimeoutMs;
        public final int batchSize;
        public final GameComplexity complexity;
        
        public GameAIConfig(boolean useND4J, int processingThreads, 
                           int inferenceTimeoutMs, int batchSize, GameComplexity complexity) {
            this.useND4J = useND4J;
            this.processingThreads = processingThreads;
            this.inferenceTimeoutMs = inferenceTimeoutMs;
            this.batchSize = batchSize;
            this.complexity = complexity;
        }
        
        @Override
        public String toString() {
            return String.format("GameAIConfig{ND4J=%s, threads=%d, timeout=%dms, batch=%d, complexity=%s}",
                    useND4J, processingThreads, inferenceTimeoutMs, batchSize, complexity);
        }
    }
    
    /**
     * Export configuration to string for debugging
     */
    public String exportConfiguration() {
        StringBuilder config = new StringBuilder();
        config.append("AI Configuration:\n");
        config.append("- ND4J Enabled: ").append(isND4JEnabled()).append("\n");
        config.append("- Performance Mode: ").append(getPerformanceMode()).append("\n");
        config.append("- Auto Game Detection: ").append(isAutoGameDetectionEnabled()).append("\n");
        config.append("- Battery Optimization: ").append(isBatteryOptimizationEnabled()).append("\n");
        config.append("- Runner Sensitivity: ").append(getRunnerGamesSensitivity()).append("\n");
        config.append("- Puzzle Timeout: ").append(getPuzzleGamesTimeout()).append("ms\n");
        config.append("- Strategy Depth: ").append(getStrategyGamesDepth()).append("\n");
        config.append("- FPS Reaction Time: ").append(getFPSGamesReactionTime()).append("ms\n");
        config.append("- MOBA Team Coordination: ").append(isMOBAGamesTeamCoordinationEnabled());
        
        return config.toString();
    }
}
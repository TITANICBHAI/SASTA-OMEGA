package com.gestureai.gameautomation;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import java.util.*;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

/**
 * Automatic game type detection using visual analysis and UI patterns
 */
public class GameTypeDetector {
    private static final String TAG = "GameTypeDetector";

    private Context context;
    private TensorFlowLiteHelper tfliteHelper;
    private OCREngine ocrEngine;
    private Map<GameType, GameTypeProfile> gameProfiles;
    private GameType lastDetectedType = GameType.UNKNOWN;
    private long lastDetectionTime = 0;
    private int detectionConfidenceCount = 0;

    public enum GameType {
        BATTLE_ROYALE, MOBA, FPS, STRATEGY, RACING, ARCADE, PUZZLE, RPG, UNKNOWN
    }

    public static class GameTypeProfile {
        public Set<String> uiKeywords;
        public Set<String> gameplayElements;
        public float[] typicalAspectRatios;
        public Map<String, Float> colorPatterns;
        public int minPlayers;
        public int maxPlayers;
        public boolean hasHealthBar;
        public boolean hasAmmoCounter;
        public boolean hasMinimap;
        public boolean hasInventory;

        public GameTypeProfile() {
            this.uiKeywords = new HashSet<>();
            this.gameplayElements = new HashSet<>();
            this.colorPatterns = new HashMap<>();
        }
    }

    public static class DetectionResult {
        public GameType gameType;
        public float confidence;
        public Map<String, Float> featureConfidences;
        public List<String> detectedElements;

        public DetectionResult(GameType type, float confidence) {
            this.gameType = type;
            this.confidence = confidence;
            this.featureConfidences = new HashMap<>();
            this.detectedElements = new ArrayList<>();
        }
    }

    public GameTypeDetector(Context context, TensorFlowLiteHelper tfliteHelper, OCREngine ocrEngine) {
        this.context = context;
        this.tfliteHelper = tfliteHelper;
        this.ocrEngine = ocrEngine;
        this.gameProfiles = new HashMap<>();

        initializeGameProfiles();
        Log.d(TAG, "Game Type Detector initialized");
    }

    private void initializeGameProfiles() {
        // Battle Royale Profile
        GameTypeProfile brProfile = new GameTypeProfile();
        brProfile.uiKeywords.addAll(Arrays.asList("alive", "zone", "circle", "storm", "players", "survived", "eliminated"));
        brProfile.gameplayElements.addAll(Arrays.asList("parachute", "loot", "weapons", "armor", "backpack", "scope"));
        brProfile.typicalAspectRatios = new float[]{16f/9f, 18f/9f, 19.5f/9f};
        brProfile.colorPatterns.put("blue_zone", 0.6f);
        brProfile.colorPatterns.put("orange_loot", 0.4f);
        brProfile.minPlayers = 50;
        brProfile.maxPlayers = 100;
        brProfile.hasHealthBar = true;
        brProfile.hasAmmoCounter = true;
        brProfile.hasMinimap = true;
        brProfile.hasInventory = true;
        gameProfiles.put(GameType.BATTLE_ROYALE, brProfile);

        // MOBA Profile
        GameTypeProfile mobaProfile = new GameTypeProfile();
        mobaProfile.uiKeywords.addAll(Arrays.asList("gold", "level", "minions", "towers", "dragon", "baron", "jungle"));
        mobaProfile.gameplayElements.addAll(Arrays.asList("abilities", "items", "creeps", "lanes", "base", "nexus"));
        mobaProfile.typicalAspectRatios = new float[]{16f/9f, 4f/3f};
        mobaProfile.colorPatterns.put("blue_team", 0.5f);
        mobaProfile.colorPatterns.put("red_team", 0.5f);
        mobaProfile.minPlayers = 6;
        mobaProfile.maxPlayers = 10;
        mobaProfile.hasHealthBar = true;
        mobaProfile.hasAmmoCounter = false;
        mobaProfile.hasMinimap = true;
        mobaProfile.hasInventory = true;
        gameProfiles.put(GameType.MOBA, mobaProfile);

        // FPS Profile
        GameTypeProfile fpsProfile = new GameTypeProfile();
        fpsProfile.uiKeywords.addAll(Arrays.asList("kills", "deaths", "headshot", "reload", "grenades", "tactical"));
        fpsProfile.gameplayElements.addAll(Arrays.asList("crosshair", "scope", "recoil", "spray", "aim", "cover"));
        fpsProfile.typicalAspectRatios = new float[]{16f/9f, 21f/9f};
        fpsProfile.colorPatterns.put("red_crosshair", 0.7f);
        fpsProfile.colorPatterns.put("green_crosshair", 0.3f);
        fpsProfile.minPlayers = 2;
        fpsProfile.maxPlayers = 64;
        fpsProfile.hasHealthBar = true;
        fpsProfile.hasAmmoCounter = true;
        fpsProfile.hasMinimap = false;
        fpsProfile.hasInventory = false;
        gameProfiles.put(GameType.FPS, fpsProfile);

        // Strategy Profile
        GameTypeProfile strategyProfile = new GameTypeProfile();
        strategyProfile.uiKeywords.addAll(Arrays.asList("resources", "buildings", "units", "research", "economy", "army"));
        strategyProfile.gameplayElements.addAll(Arrays.asList("base", "workers", "military", "technology", "expansion"));
        strategyProfile.typicalAspectRatios = new float[]{16f/9f, 16f/10f};
        strategyProfile.colorPatterns.put("resource_icons", 0.8f);
        strategyProfile.minPlayers = 1;
        strategyProfile.maxPlayers = 8;
        strategyProfile.hasHealthBar = false;
        strategyProfile.hasAmmoCounter = false;
        strategyProfile.hasMinimap = true;
        strategyProfile.hasInventory = false;
        gameProfiles.put(GameType.STRATEGY, strategyProfile);

        // Racing Profile
        GameTypeProfile racingProfile = new GameTypeProfile();
        racingProfile.uiKeywords.addAll(Arrays.asList("speed", "lap", "position", "time", "finish", "checkpoint"));
        racingProfile.gameplayElements.addAll(Arrays.asList("speedometer", "gear", "nitro", "boost", "track", "racing"));
        racingProfile.typicalAspectRatios = new float[]{16f/9f, 18f/9f};
        racingProfile.colorPatterns.put("speed_indicators", 0.6f);
        racingProfile.minPlayers = 1;
        racingProfile.maxPlayers = 20;
        racingProfile.hasHealthBar = false;
        racingProfile.hasAmmoCounter = false;
        racingProfile.hasMinimap = true;
        racingProfile.hasInventory = false;
        gameProfiles.put(GameType.RACING, racingProfile);

        // Arcade Profile (default for simple games)
        GameTypeProfile arcadeProfile = new GameTypeProfile();
        arcadeProfile.uiKeywords.addAll(Arrays.asList("score", "lives", "level", "points", "bonus", "power"));
        arcadeProfile.gameplayElements.addAll(Arrays.asList("coins", "gems", "stars", "collectibles", "obstacles"));
        arcadeProfile.typicalAspectRatios = new float[]{16f/9f, 9f/16f, 3f/4f};
        arcadeProfile.colorPatterns.put("bright_colors", 0.8f);
        arcadeProfile.minPlayers = 1;
        arcadeProfile.maxPlayers = 4;
        arcadeProfile.hasHealthBar = false;
        arcadeProfile.hasAmmoCounter = false;
        arcadeProfile.hasMinimap = false;
        arcadeProfile.hasInventory = false;
        gameProfiles.put(GameType.ARCADE, arcadeProfile);
    }

    public DetectionResult detectGameType(Bitmap gameScreen) {
        if (gameScreen == null) {
            return new DetectionResult(GameType.UNKNOWN, 0f);
        }

        try {
            // Multi-stage detection process
            DetectionResult textBasedResult = analyzeTextElements(gameScreen);
            DetectionResult visualResult = analyzeVisualElements(gameScreen);
            DetectionResult uiResult = analyzeUIStructure(gameScreen);
            DetectionResult mlResult = runMLDetection(gameScreen);

            // Combine all detection results
            DetectionResult finalResult = combineDetectionResults(
                    Arrays.asList(textBasedResult, visualResult, uiResult, mlResult));

            // Apply temporal consistency
            finalResult = applyTemporalFiltering(finalResult);

            Log.d(TAG, "Detected game type: " + finalResult.gameType +
                    " (confidence: " + finalResult.confidence + ")");

            return finalResult;

        } catch (Exception e) {
            Log.e(TAG, "Error detecting game type", e);
            return new DetectionResult(GameType.UNKNOWN, 0f);
        }
    }

    private DetectionResult analyzeTextElements(Bitmap screen) {
        Map<GameType, Float> typeScores = new HashMap<>();

        try {
            // Extract text from screen
            List<OCREngine.DetectedText> detectedTexts = ocrEngine.processScreenText(screen).get();

            for (OCREngine.DetectedText text : detectedTexts) {
                String textLower = text.text.toLowerCase();

                // Check against each game type profile
                for (Map.Entry<GameType, GameTypeProfile> entry : gameProfiles.entrySet()) {
                    GameType type = entry.getKey();
                    GameTypeProfile profile = entry.getValue();

                    float score = typeScores.getOrDefault(type, 0f);

                    // Check UI keywords
                    for (String keyword : profile.uiKeywords) {
                        if (textLower.contains(keyword)) {
                            score += text.confidence * 0.8f;
                        }
                    }

                    // Check gameplay elements
                    for (String element : profile.gameplayElements) {
                        if (textLower.contains(element)) {
                            score += text.confidence * 0.6f;
                        }
                    }

                    typeScores.put(type, score);
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Text analysis failed", e);
        }

        return getBestDetection(typeScores, "text_analysis");
    }

    private DetectionResult analyzeVisualElements(Bitmap screen) {
        Map<GameType, Float> typeScores = new HashMap<>();

        try {
            // Analyze color patterns using ND4J for fast processing
            INDArray screenArray = bitmapToNDArray(screen);

            for (Map.Entry<GameType, GameTypeProfile> entry : gameProfiles.entrySet()) {
                GameType type = entry.getKey();
                GameTypeProfile profile = entry.getValue();

                float score = 0f;

                // Analyze dominant colors
                Map<String, Float> colorScores = analyzeColorPatterns(screenArray, profile.colorPatterns);
                for (float colorScore : colorScores.values()) {
                    score += colorScore;
                }

                // Analyze aspect ratio
                float aspectRatio = (float) screen.getWidth() / screen.getHeight();
                for (float targetRatio : profile.typicalAspectRatios) {
                    if (Math.abs(aspectRatio - targetRatio) < 0.2f) {
                        score += 0.5f;
                        break;
                    }
                }

                typeScores.put(type, score);
            }

        } catch (Exception e) {
            Log.w(TAG, "Visual analysis failed", e);
        }

        return getBestDetection(typeScores, "visual_analysis");
    }

    private DetectionResult analyzeUIStructure(Bitmap screen) {
        Map<GameType, Float> typeScores = new HashMap<>();

        try {
            // Use TensorFlow Lite UI detection model
            List<TensorFlowLiteHelper.DetectionResult> uiElements =
                    tfliteHelper.runInference("ui_detector", screen);

            Set<String> detectedUIElements = new HashSet<>();
            for (TensorFlowLiteHelper.DetectionResult element : uiElements) {
                detectedUIElements.add(element.className);
            }

            // Score based on UI element presence
            for (Map.Entry<GameType, GameTypeProfile> entry : gameProfiles.entrySet()) {
                GameType type = entry.getKey();
                GameTypeProfile profile = entry.getValue();

                float score = 0f;

                if (profile.hasHealthBar && detectedUIElements.contains("health_bar")) {
                    score += 1.0f;
                } else if (!profile.hasHealthBar && !detectedUIElements.contains("health_bar")) {
                    score += 0.5f;
                }

                if (profile.hasAmmoCounter && detectedUIElements.contains("ammo_counter")) {
                    score += 1.0f;
                } else if (!profile.hasAmmoCounter && !detectedUIElements.contains("ammo_counter")) {
                    score += 0.5f;
                }

                if (profile.hasMinimap && detectedUIElements.contains("minimap")) {
                    score += 1.0f;
                } else if (!profile.hasMinimap && !detectedUIElements.contains("minimap")) {
                    score += 0.5f;
                }

                typeScores.put(type, score);
            }

        } catch (Exception e) {
            Log.w(TAG, "UI structure analysis failed", e);
        }

        return getBestDetection(typeScores, "ui_structure");
    }

    private DetectionResult runMLDetection(Bitmap screen) {
        Map<GameType, Float> typeScores = new HashMap<>();

        try {
            // Use game state classification model
            List<TensorFlowLiteHelper.DetectionResult> mlResults =
                    tfliteHelper.runInference("game_state_classifier", screen);

            // Map ML results to game types
            for (TensorFlowLiteHelper.DetectionResult result : mlResults) {
                GameType mappedType = mapMLResultToGameType(result.className);
                if (mappedType != GameType.UNKNOWN) {
                    typeScores.put(mappedType, result.confidence);
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "ML detection failed", e);
        }

        return getBestDetection(typeScores, "ml_detection");
    }

    private INDArray bitmapToNDArray(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        float[] rgbData = new float[width * height * 3];
        for (int i = 0; i < pixels.length; i++) {
            rgbData[i * 3] = ((pixels[i] >> 16) & 0xFF) / 255.0f;     // R
            rgbData[i * 3 + 1] = ((pixels[i] >> 8) & 0xFF) / 255.0f; // G
            rgbData[i * 3 + 2] = (pixels[i] & 0xFF) / 255.0f;        // B
        }

        return Nd4j.create(rgbData).reshape(height, width, 3);
    }

    private Map<String, Float> analyzeColorPatterns(INDArray screenArray, Map<String, Float> targetPatterns) {
        Map<String, Float> scores = new HashMap<>();

        // Calculate dominant colors
        INDArray meanColors = screenArray.mean(0, 1); // Average across height and width

        for (String patternName : targetPatterns.keySet()) {
            float score = calculateColorPatternMatch(meanColors, patternName);
            scores.put(patternName, score);
        }

        return scores;
    }

    private float calculateColorPatternMatch(INDArray colors, String patternName) {
        // Define target color patterns
        switch (patternName) {
            case "blue_zone":
                return Math.max(0, colors.getFloat(2) - colors.getFloat(0) - colors.getFloat(1)); // More blue
            case "orange_loot":
                return Math.max(0, colors.getFloat(0) + colors.getFloat(1) - colors.getFloat(2)); // Red + Green
            case "red_crosshair":
                return colors.getFloat(0) > 0.7f ? 1.0f : 0f; // High red component
            case "bright_colors":
                return (colors.getFloat(0) + colors.getFloat(1) + colors.getFloat(2)) / 3f; // Overall brightness
            default:
                return 0f;
        }
    }

    private GameType mapMLResultToGameType(String mlClass) {
        switch (mlClass.toLowerCase()) {
            case "battle_royale":
            case "pubg":
            case "fortnite":
                return GameType.BATTLE_ROYALE;
            case "moba":
            case "dota":
            case "lol":
                return GameType.MOBA;
            case "fps":
            case "shooter":
                return GameType.FPS;
            case "strategy":
            case "rts":
                return GameType.STRATEGY;
            case "racing":
                return GameType.RACING;
            case "arcade":
            case "casual":
                return GameType.ARCADE;
            default:
                return GameType.UNKNOWN;
        }
    }

    private DetectionResult getBestDetection(Map<GameType, Float> typeScores, String source) {
        GameType bestType = GameType.UNKNOWN;
        float bestScore = 0f;

        for (Map.Entry<GameType, Float> entry : typeScores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestType = entry.getKey();
            }
        }

        DetectionResult result = new DetectionResult(bestType, bestScore);
        result.featureConfidences.put(source, bestScore);

        return result;
    }

    private DetectionResult combineDetectionResults(List<DetectionResult> results) {
        Map<GameType, Float> combinedScores = new HashMap<>();
        Map<String, Float> allFeatureConfidences = new HashMap<>();

        // Weight different detection methods
        float[] weights = {0.3f, 0.25f, 0.25f, 0.2f}; // text, visual, ui, ml

        for (int i = 0; i < results.size() && i < weights.length; i++) {
            DetectionResult result = results.get(i);
            float weight = weights[i];

            float currentScore = combinedScores.getOrDefault(result.gameType, 0f);
            combinedScores.put(result.gameType, currentScore + (result.confidence * weight));

            allFeatureConfidences.putAll(result.featureConfidences);
        }

        // Find best combined result
        GameType bestType = GameType.UNKNOWN;
        float bestScore = 0f;

        for (Map.Entry<GameType, Float> entry : combinedScores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestType = entry.getKey();
            }
        }

        DetectionResult finalResult = new DetectionResult(bestType, bestScore);
        finalResult.featureConfidences = allFeatureConfidences;

        return finalResult;
    }

    private DetectionResult applyTemporalFiltering(DetectionResult currentResult) {
        long currentTime = System.currentTimeMillis();

        // If same type detected recently, increase confidence
        if (currentResult.gameType == lastDetectedType &&
                (currentTime - lastDetectionTime) < 5000) { // 5 seconds

            detectionConfidenceCount++;
            float temporalBonus = Math.min(0.3f, detectionConfidenceCount * 0.05f);
            currentResult.confidence = Math.min(1.0f, currentResult.confidence + temporalBonus);

        } else {
            detectionConfidenceCount = 1;
        }

        lastDetectedType = currentResult.gameType;
        lastDetectionTime = currentTime;

        return currentResult;
    }

    public GameType getLastDetectedType() {
        return lastDetectedType;
    }

    public boolean isConfidentDetection() {
        return detectionConfidenceCount >= 3; // 3 consecutive detections
    }

    public GameTypeProfile getGameProfile(GameType gameType) {
        return gameProfiles.get(gameType);
    }
    public GameTypeDetector(Context context) {
        this(context, null, null);
    }

    public String detectCurrentGame() {
        return lastDetectedType.name();
    }
}
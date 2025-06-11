package com.gestureai.gameautomation;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import java.util.*;

/**
 * Advanced game context analysis for strategic decision making
 */
public class GameContextAnalyzer {
    private static final String TAG = "GameContextAnalyzer";

    public enum GameType {
        BATTLE_ROYALE, MOBA, FPS, STRATEGY, ARCADE, UNKNOWN
    }

    public enum EngagementRisk {
        VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH
    }

    public static class GameContext {
        public GameType gameType;
        public float safeZoneRadius;
        public float[] safeZoneCenter;
        public float[] playerPosition;
        public int playersAlive;
        public int teamMatesAlive;
        public EngagementRisk currentRisk;
        public String gameMode;
        public int teamScore;
        public int enemyScore;
        public long gameTime;
        public String mapName;
        public boolean objectiveActive;
        public boolean inSafeZone;
        public float timeToZoneCollapse;
        public Map<String, Float> resourceLevels; // health, shield, ammo, etc.
        public List<String> availableWeapons;
        public String currentWeapon;
        public boolean canEngage;
        public float[] optimalRotationPath;

        public GameContext() {
            this.resourceLevels = new HashMap<>();
            this.availableWeapons = new ArrayList<>();
            this.safeZoneCenter = new float[2];
            this.playerPosition = new float[2];
            this.optimalRotationPath = new float[4]; // start_x, start_y, end_x, end_y
            this.gameMode = "unknown";
            this.gameTime = System.currentTimeMillis();
        }
    }

    private GameType detectedGameType = GameType.UNKNOWN;
    private OCREngine ocrEngine;
    private PlayerTracker playerTracker;

    public GameContextAnalyzer(OCREngine ocrEngine, PlayerTracker playerTracker) {
        this.ocrEngine = ocrEngine;
        this.playerTracker = playerTracker;
    }

    /**
     * Analyze complete game context from screen
     */
    public GameContext analyzeGameScreen(Bitmap screen, List<ObjectDetectionEngine.DetectedObject> detectedObjects) {
        GameContext context = new GameContext();

        // Detect game type if unknown
        if (detectedGameType == GameType.UNKNOWN) {
            detectedGameType = detectGameType(screen, detectedObjects);
        }
        context.gameType = detectedGameType;

        // Game-specific analysis
        switch (detectedGameType) {
            case BATTLE_ROYALE:
                analyzeBattleRoyaleContext(screen, context, detectedObjects);
                break;
            case MOBA:
                analyzeMOBAContext(screen, context, detectedObjects);
                break;
            case FPS:
                analyzeFPSContext(screen, context, detectedObjects);
                break;
            case STRATEGY:
                analyzeStrategyContext(screen, context, detectedObjects);
                break;
            default:
                analyzeGenericContext(screen, context, detectedObjects);
                break;
        }

        // Calculate engagement risk
        context.currentRisk = calculateEngagementRisk(context);

        // Determine optimal actions
        context.canEngage = shouldEngage(context);
        context.optimalRotationPath = calculateOptimalRotation(context);

        return context;
    }

    private GameType detectGameType(Bitmap screen, List<ObjectDetectionEngine.DetectedObject> objects) {
        // Analyze UI elements and game objects to determine type
        boolean hasZoneIndicator = false;
        boolean hasHealthBar = false;
        boolean hasMinimap = false;
        boolean hasWeaponUI = false;

        for (ObjectDetectionEngine.DetectedObject obj : objects) {
            String name = obj.name.toLowerCase();
            if (name.contains("zone") || name.contains("circle")) {
                hasZoneIndicator = true;
            }
            if (name.contains("health") || name.contains("hp")) {
                hasHealthBar = true;
            }
            if (name.contains("map") || name.contains("radar")) {
                hasMinimap = true;
            }
            if (name.contains("weapon") || name.contains("gun")) {
                hasWeaponUI = true;
            }
        }

        if (hasZoneIndicator && hasMinimap && hasWeaponUI) {
            return GameType.BATTLE_ROYALE;
        } else if (hasMinimap && hasHealthBar) {
            return GameType.MOBA;
        } else if (hasWeaponUI && hasHealthBar) {
            return GameType.FPS;
        }

        return GameType.ARCADE;
    }

    private void analyzeBattleRoyaleContext(Bitmap screen, GameContext context,
                                            List<ObjectDetectionEngine.DetectedObject> objects) {

        // Extract zone information from minimap
        analyzeZoneFromMinimap(screen, context);

        // Extract player count from UI
        context.playersAlive = extractPlayerCount(screen);

        // Extract resource levels
        context.resourceLevels.put("health", extractHealthLevel(screen));
        context.resourceLevels.put("shield", extractShieldLevel(screen));
        context.resourceLevels.put("ammo", extractAmmoCount(screen));

        // Analyze available weapons
        context.availableWeapons = extractAvailableWeapons(screen, objects);
        context.currentWeapon = extractCurrentWeapon(screen);

        // Determine player position relative to zone
        context.inSafeZone = isInSafeZone(context.playerPosition, context.safeZoneCenter, context.safeZoneRadius);

        Log.d(TAG, "Battle Royale Analysis - Players: " + context.playersAlive +
                ", In Zone: " + context.inSafeZone +
                ", Health: " + context.resourceLevels.get("health"));
    }

    private void analyzeMOBAContext(Bitmap screen, GameContext context,
                                    List<ObjectDetectionEngine.DetectedObject> objects) {

        // Extract team information
        context.teamMatesAlive = extractTeamMateCount(screen);

        // Extract resource levels (mana, health, etc.)
        context.resourceLevels.put("health", extractHealthLevel(screen));
        context.resourceLevels.put("mana", extractManaLevel(screen));

        // Analyze minion waves and objectives
        analyzeMinionsAndObjectives(screen, context, objects);

        Log.d(TAG, "MOBA Analysis - Teammates: " + context.teamMatesAlive +
                ", Health: " + context.resourceLevels.get("health"));
    }

    private void analyzeFPSContext(Bitmap screen, GameContext context,
                                   List<ObjectDetectionEngine.DetectedObject> objects) {

        // Extract weapon and ammo information
        context.currentWeapon = extractCurrentWeapon(screen);
        context.resourceLevels.put("ammo", extractAmmoCount(screen));
        context.resourceLevels.put("health", extractHealthLevel(screen));

        // Analyze enemy positions and cover
        analyzeEnemyPositions(screen, context, objects);

        Log.d(TAG, "FPS Analysis - Weapon: " + context.currentWeapon +
                ", Ammo: " + context.resourceLevels.get("ammo"));
    }

    private void analyzeStrategyContext(Bitmap screen, GameContext context,
                                        List<ObjectDetectionEngine.DetectedObject> objects) {

        // Extract resource counts (gold, units, etc.)
        context.resourceLevels.put("gold", extractGoldAmount(screen));
        context.resourceLevels.put("units", extractUnitCount(screen));

        Log.d(TAG, "Strategy Analysis - Gold: " + context.resourceLevels.get("gold"));
    }

    private void analyzeGenericContext(Bitmap screen, GameContext context,
                                       List<ObjectDetectionEngine.DetectedObject> objects) {

        // Basic analysis for unknown game types
        context.resourceLevels.put("score", extractScore(screen));
        context.playersAlive = playerTracker.getPlayerCount();

        Log.d(TAG, "Generic Analysis - Score: " + context.resourceLevels.get("score"));
    }

    private void analyzeZoneFromMinimap(Bitmap screen, GameContext context) {
        // Extract minimap region and analyze zone
        Rect minimapRegion = findMinimapRegion(screen);
        if (minimapRegion != null) {
            // Analyze white circle (safe zone) and blue circle (next zone)
            // This would use computer vision to detect circular patterns
            context.safeZoneRadius = 150f; // Placeholder
            context.safeZoneCenter[0] = minimapRegion.centerX();
            context.safeZoneCenter[1] = minimapRegion.centerY();
            context.timeToZoneCollapse = extractZoneTimer(screen);
        }
    }

    private int extractPlayerCount(Bitmap screen) {
        if (ocrEngine != null) {
            // Look for player count UI element (usually top-right)
            Rect playerCountRegion = new Rect(screen.getWidth() - 200, 0, screen.getWidth(), 100);
            String text = ocrEngine.extractTextFromRegion(screen, playerCountRegion);

            // Parse "45 alive" or similar format
            if (text.contains("alive")) {
                try {
                    String[] parts = text.split(" ");
                    return Integer.parseInt(parts[0]);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse player count: " + text);
                }
            }
        }
        return playerTracker.getPlayerCount(); // Fallback to tracked players
    }

    private float extractHealthLevel(Bitmap screen) {
        if (ocrEngine != null) {
            // Health usually in bottom-left corner
            Rect healthRegion = new Rect(0, screen.getHeight() - 200, 300, screen.getHeight());
            String text = ocrEngine.extractTextFromRegion(screen, healthRegion);

            // Parse "100/100" or "100%" format
            return parseHealthText(text);
        }
        return 100f; // Default full health
    }

    private float extractShieldLevel(Bitmap screen) {
        if (ocrEngine != null) {
            // Shield usually next to health
            Rect shieldRegion = new Rect(0, screen.getHeight() - 150, 300, screen.getHeight() - 50);
            String text = ocrEngine.extractTextFromRegion(screen, shieldRegion);
            return parseHealthText(text);
        }
        return 0f; // Default no shield
    }

    private float extractAmmoCount(Bitmap screen) {
        if (ocrEngine != null) {
            // Ammo usually bottom-right
            Rect ammoRegion = new Rect(screen.getWidth() - 200, screen.getHeight() - 150,
                    screen.getWidth(), screen.getHeight());
            String text = ocrEngine.extractTextFromRegion(screen, ammoRegion);

            // Parse "30/120" format
            return parseAmmoText(text);
        }
        return 30f; // Default ammo
    }

    private String extractCurrentWeapon(Bitmap screen) {
        if (ocrEngine != null) {
            // Weapon name usually bottom center
            Rect weaponRegion = new Rect(screen.getWidth()/2 - 100, screen.getHeight() - 100,
                    screen.getWidth()/2 + 100, screen.getHeight());
            return ocrEngine.extractTextFromRegion(screen, weaponRegion);
        }
        return "unknown";
    }

    private List<String> extractAvailableWeapons(Bitmap screen, List<ObjectDetectionEngine.DetectedObject> objects) {
        List<String> weapons = new ArrayList<>();
        for (ObjectDetectionEngine.DetectedObject obj : objects) {
            if (obj.name.toLowerCase().contains("weapon") ||
                    obj.name.toLowerCase().contains("gun") ||
                    obj.name.toLowerCase().contains("rifle")) {
                weapons.add(obj.name);
            }
        }
        return weapons;
    }

    private float parseHealthText(String text) {
        try {
            if (text.contains("/")) {
                String[] parts = text.split("/");
                return Float.parseFloat(parts[0].trim());
            } else if (text.contains("%")) {
                return Float.parseFloat(text.replace("%", "").trim());
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse health text: " + text);
        }
        return 100f;
    }

    private float parseAmmoText(String text) {
        try {
            if (text.contains("/")) {
                String[] parts = text.split("/");
                return Float.parseFloat(parts[0].trim());
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse ammo text: " + text);
        }
        return 30f;
    }

    private EngagementRisk calculateEngagementRisk(GameContext context) {
        float riskScore = 0f;

        // Health-based risk
        float healthPercent = context.resourceLevels.getOrDefault("health", 100f);
        if (healthPercent < 25) riskScore += 0.4f;
        else if (healthPercent < 50) riskScore += 0.2f;

        // Enemy count risk
        int enemyCount = playerTracker.getEnemyCount();
        riskScore += enemyCount * 0.15f;

        // Zone risk (for battle royales)
        if (!context.inSafeZone) riskScore += 0.3f;

        // Ammo risk
        float ammo = context.resourceLevels.getOrDefault("ammo", 30f);
        if (ammo < 10) riskScore += 0.2f;

        if (riskScore >= 0.8f) return EngagementRisk.VERY_HIGH;
        if (riskScore >= 0.6f) return EngagementRisk.HIGH;
        if (riskScore >= 0.4f) return EngagementRisk.MEDIUM;
        if (riskScore >= 0.2f) return EngagementRisk.LOW;
        return EngagementRisk.VERY_LOW;
    }

    private boolean shouldEngage(GameContext context) {
        // Don't engage if health too low
        if (context.resourceLevels.getOrDefault("health", 100f) < 30) return false;

        // Don't engage if outnumbered significantly
        if (playerTracker.getEnemyCount() > 3) return false;

        // Don't engage if out of zone in battle royale
        if (context.gameType == GameType.BATTLE_ROYALE && !context.inSafeZone) return false;

        // Don't engage if low on ammo
        if (context.resourceLevels.getOrDefault("ammo", 30f) < 5) return false;

        return context.currentRisk != EngagementRisk.VERY_HIGH;
    }

    private float[] calculateOptimalRotation(GameContext context) {
        float[] rotation = new float[4];

        if (context.gameType == GameType.BATTLE_ROYALE && !context.inSafeZone) {
            // Calculate path to safe zone
            rotation[0] = context.playerPosition[0];
            rotation[1] = context.playerPosition[1];
            rotation[2] = context.safeZoneCenter[0];
            rotation[3] = context.safeZoneCenter[1];
        } else {
            // Calculate path to optimal position (cover, high ground, etc.)
            rotation[0] = context.playerPosition[0];
            rotation[1] = context.playerPosition[1];
            rotation[2] = context.playerPosition[0] + 100; // Move right as default
            rotation[3] = context.playerPosition[1];
        }

        return rotation;
    }

    // Helper methods for specific game analysis
    private Rect findMinimapRegion(Bitmap screen) {
        // Usually top-right or top-left corner
        return new Rect(screen.getWidth() - 200, 0, screen.getWidth(), 200);
    }

    private float extractZoneTimer(Bitmap screen) {
        // Extract countdown timer for zone collapse
        return 60f; // Placeholder
    }

    private int extractTeamMateCount(Bitmap screen) {
        return playerTracker.getTeammateCount();
    }

    private float extractManaLevel(Bitmap screen) {
        // Similar to health extraction but for mana bar
        return 100f;
    }

    private void analyzeMinionsAndObjectives(Bitmap screen, GameContext context,
                                             List<ObjectDetectionEngine.DetectedObject> objects) {
        // Analyze MOBA-specific elements like minion waves, towers, jungle monsters
    }

    private void analyzeEnemyPositions(Bitmap screen, GameContext context,
                                       List<ObjectDetectionEngine.DetectedObject> objects) {
        // Analyze enemy positions relative to cover and tactical advantages
    }

    private float extractGoldAmount(Bitmap screen) {
        if (ocrEngine != null) {
            // Gold usually displayed in top UI
            Rect goldRegion = new Rect(0, 0, screen.getWidth(), 100);
            String text = ocrEngine.extractTextFromRegion(screen, goldRegion);
            // Parse gold amount from text
        }
        return 1000f; // Placeholder
    }

    private float extractUnitCount(Bitmap screen) {
        return playerTracker.getPlayerCount();
    }

    private float extractScore(Bitmap screen) {
        if (ocrEngine != null) {
            // Score usually at top center
            Rect scoreRegion = new Rect(screen.getWidth()/2 - 100, 0, screen.getWidth()/2 + 100, 100);
            String text = ocrEngine.extractTextFromRegion(screen, scoreRegion);
            try {
                return Float.parseFloat(text.replaceAll("[^0-9]", ""));
            } catch (Exception e) {
                Log.w(TAG, "Failed to parse score: " + text);
            }
        }
        return 0f;
    }

    private boolean isInSafeZone(float[] playerPos, float[] zoneCenter, float zoneRadius) {
        float distance = (float) Math.sqrt(
                Math.pow(playerPos[0] - zoneCenter[0], 2) +
                        Math.pow(playerPos[1] - zoneCenter[1], 2)
        );
        return distance <= zoneRadius;
    }
}
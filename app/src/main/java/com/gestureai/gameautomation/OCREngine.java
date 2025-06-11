package com.gestureai.gameautomation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.gestureai.gameautomation.utils.NLPProcessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import com.gestureai.gameautomation.ai.GameStrategyAgent;

/**
 * Advanced OCR Engine using Google ML Kit for text recognition
 * Connects to Strategy AI, NLP, and Object Detection systems
 */
public class OCREngine {
    private static final String TAG = "OCREngine";
    
    private Context context;
    private TextRecognizer textRecognizer;
    private GameStrategyAgent gameStrategyAgent;

    private boolean aiLearningEnabled = true;
    private NLPProcessor nlpProcessor;
    private boolean isInitialized = false;

    // ADD THIS TO OCREngine:
    public enum GameType { BATTLE_ROYALE, MOBA, FPS, STRATEGY, ARCADE }
    // Game-specific strategies
    public abstract class BaseStrategy {
        protected Context context;

        public BaseStrategy(Context context) {
            this.context = context;
        }

        protected abstract GameAction analyzeGameContext(Object state);
    }

    public GameAction analyzeGameContext(GameType type, Object state) {
        // Connect to your existing DQN/PPO agents
        if (gameStrategyAgent != null) {
            return gameStrategyAgent.getOptimalAction(state);
        }
        return new GameAction("WAIT", 540, 960, 0.5f, "fallback"); // Only when AI unavailable
    }
    // Dynamic text recognition methods
    public int extractPlayerCount(Bitmap screen) {
        if (!isInitialized) return -1;

        try {
            // Battle royale games show player count in top-right
            Rect playerCountRegion = new Rect(screen.getWidth() - 200, 0, screen.getWidth(), 100);
            String text = extractTextFromRegion(screen, playerCountRegion);

            // Parse formats like "45 alive", "Players: 23", "23/100"
            String numericText = text.replaceAll("[^0-9/]", "");
            if (numericText.contains("/")) {
                return Integer.parseInt(numericText.split("/")[0]);
            } else if (!numericText.isEmpty()) {
                return Integer.parseInt(numericText);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract player count", e);
        }
        return -1;
    }

    public int extractKillCount(Bitmap screen) {
        try {
            // Kill count usually in top-left or center-top
            Rect killRegion = new Rect(0, 0, screen.getWidth()/2, 150);
            String text = extractTextFromRegion(screen, killRegion);

            // Look for patterns like "Kills: 5", "5 eliminations", "K: 3"
            if (text.toLowerCase().contains("kill") || text.toLowerCase().contains("elim")) {
                String numericText = text.replaceAll("[^0-9]", "");
                if (!numericText.isEmpty()) {
                    return Integer.parseInt(numericText);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract kill count", e);
        }
        return 0;
    }

    public float extractHealthBar(Bitmap screen) {
        try {
            // Health usually bottom-left corner
            Rect healthRegion = new Rect(0, screen.getHeight() - 200, 300, screen.getHeight());
            String text = extractTextFromRegion(screen, healthRegion);

            // Parse formats like "100/100", "85%", "Health: 75"
            if (text.contains("/")) {
                String[] parts = text.split("/");
                return Float.parseFloat(parts[0].replaceAll("[^0-9.]", ""));
            } else if (text.contains("%")) {
                return Float.parseFloat(text.replaceAll("[^0-9.]", ""));
            } else {
                String numericText = text.replaceAll("[^0-9.]", "");
                if (!numericText.isEmpty()) {
                    return Float.parseFloat(numericText);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract health", e);
        }
        return 100f; // Default full health
    }

    public int extractAmmoCount(Bitmap screen) {
        try {
            // Ammo count usually bottom-right
            Rect ammoRegion = new Rect(screen.getWidth() - 200, screen.getHeight() - 150,
                    screen.getWidth(), screen.getHeight());
            String text = extractTextFromRegion(screen, ammoRegion);

            // Parse formats like "30/120", "Ammo: 45", "25 rounds"
            if (text.contains("/")) {
                String[] parts = text.split("/");
                return Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
            } else {
                String numericText = text.replaceAll("[^0-9]", "");
                if (!numericText.isEmpty()) {
                    return Integer.parseInt(numericText);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract ammo count", e);
        }
        return 0;
    }

    public Map<String, Float> extractMinimapData(Bitmap screen) {
        Map<String, Float> minimapData = new HashMap<>();

        try {
            // Minimap usually top-right corner
            Rect minimapRegion = new Rect(screen.getWidth() - 250, 0, screen.getWidth(), 250);

            // Extract zone timer for battle royales
            String text = extractTextFromRegion(screen, minimapRegion);

            // Look for timer patterns like "2:45", "Zone: 1:30", "Next zone in 0:45"
            Pattern timePattern = Pattern.compile("(\\d+):(\\d+)");
            Matcher matcher = timePattern.matcher(text);

            if (matcher.find()) {
                int minutes = Integer.parseInt(matcher.group(1));
                int seconds = Integer.parseInt(matcher.group(2));
                minimapData.put("zone_timer", minutes * 60f + seconds);
            }

            // Extract distance indicators
            Pattern distancePattern = Pattern.compile("(\\d+)m");
            matcher = distancePattern.matcher(text);
            if (matcher.find()) {
                minimapData.put("zone_distance", Float.parseFloat(matcher.group(1)));
            }

        } catch (Exception e) {
            Log.w(TAG, "Failed to extract minimap data", e);
        }

        return minimapData;
    }

    public String extractTextFromRegion(Bitmap screen, Rect region) {
        if (!isInitialized || screen == null || region == null) {
            return "";
        }

        try {
            // Crop bitmap to region
            Bitmap croppedBitmap = Bitmap.createBitmap(screen,
                    Math.max(0, region.left),
                    Math.max(0, region.top),
                    Math.min(region.width(), screen.getWidth() - region.left),
                    Math.min(region.height(), screen.getHeight() - region.top)
            );

            // Process with ML Kit
            InputImage image = InputImage.fromBitmap(croppedBitmap, 0);
            CompletableFuture<String> future = new CompletableFuture<>();

            textRecognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        StringBuilder extractedText = new StringBuilder();
                        for (Text.TextBlock block : visionText.getTextBlocks()) {
                            extractedText.append(block.getText()).append(" ");
                        }
                        future.complete(extractedText.toString().trim());
                    })
                    .addOnFailureListener(future::completeExceptionally);

            return future.get(2, TimeUnit.SECONDS); // 2 second timeout

        } catch (Exception e) {
            Log.w(TAG, "Failed to extract text from region", e);
            return "";
        }
    }
    // Battle Royale specific text extraction
    public BattleRoyaleData extractBattleRoyaleData(Bitmap screen) {
        BattleRoyaleData data = new BattleRoyaleData();

        data.playersAlive = extractPlayerCount(screen);
        data.kills = extractKillCount(screen);
        data.health = extractHealthBar(screen);
        data.shield = extractShieldLevel(screen);
        data.ammo = extractAmmoCount(screen);
        data.minimapData = extractMinimapData(screen);
        data.currentWeapon = extractCurrentWeapon(screen);

        return data;
    }

    private float extractShieldLevel(Bitmap screen) {
        try {
            // Shield usually near health bar
            Rect shieldRegion = new Rect(0, screen.getHeight() - 180, 300, screen.getHeight() - 80);
            String text = extractTextFromRegion(screen, shieldRegion);

            // Look for shield indicators
            if (text.toLowerCase().contains("shield") || text.toLowerCase().contains("armor")) {
                String numericText = text.replaceAll("[^0-9.]", "");
                if (!numericText.isEmpty()) {
                    return Float.parseFloat(numericText);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract shield level", e);
        }
        return 0f;
    }

    private String extractCurrentWeapon(Bitmap screen) {
        try {
            // Weapon name usually bottom center
            Rect weaponRegion = new Rect(screen.getWidth()/2 - 150, screen.getHeight() - 120,
                    screen.getWidth()/2 + 150, screen.getHeight() - 20);
            String text = extractTextFromRegion(screen, weaponRegion);

            // Common weapon patterns
            String[] weaponKeywords = {"rifle", "shotgun", "pistol", "sniper", "smg", "ak", "m4", "scar"};
            String lowerText = text.toLowerCase();

            for (String weapon : weaponKeywords) {
                if (lowerText.contains(weapon)) {
                    return weapon;
                }
            }

            return text.trim();
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract weapon name", e);
        }
        return "unknown";
    }

    // MOBA specific text extraction
    public MOBAData extractMOBAData(Bitmap screen) {
        MOBAData data = new MOBAData();

        data.gold = extractGoldAmount(screen);
        data.level = extractPlayerLevel(screen);
        data.health = extractHealthBar(screen);
        data.mana = extractManaLevel(screen);
        data.kills = extractKillCount(screen);
        data.deaths = extractDeathCount(screen);
        data.assists = extractAssistCount(screen);

        return data;
    }

    private int extractGoldAmount(Bitmap screen) {
        try {
            // Gold usually in top UI
            Rect goldRegion = new Rect(0, 0, screen.getWidth(), 120);
            String text = extractTextFromRegion(screen, goldRegion);

            // Look for gold indicators with large numbers
            Pattern goldPattern = Pattern.compile("(\\d{3,})"); // 3+ digits
            Matcher matcher = goldPattern.matcher(text);

            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract gold amount", e);
        }
        return 0;
    }

    private int extractPlayerLevel(Bitmap screen) {
        try {
            // Level usually near player portrait
            Rect levelRegion = new Rect(0, screen.getHeight() - 300, 200, screen.getHeight() - 100);
            String text = extractTextFromRegion(screen, levelRegion);

            Pattern levelPattern = Pattern.compile("(?i)level?\\s*(\\d+)|lv\\s*(\\d+)|(\\d+)");
            Matcher matcher = levelPattern.matcher(text);

            if (matcher.find()) {
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    if (matcher.group(i) != null) {
                        return Integer.parseInt(matcher.group(i));
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract player level", e);
        }
        return 1;
    }

    // Supporting data classes
    public static class BattleRoyaleData {
        public int playersAlive;
        public int kills;
        public float health;
        public float shield;
        public int ammo;
        public Map<String, Float> minimapData;
        public String currentWeapon;
    }

    public static class MOBAData {
        public int gold;
        public int level;
        public float health;
        public float mana;
        public int kills;
        public int deaths;
        public int assists;
    }
    
    // Game-specific text patterns for Subway Surfers and similar games
    private static final Map<String, String> GAME_TEXT_PATTERNS = new HashMap<>();
    static {
        // Score and progress indicators
        GAME_TEXT_PATTERNS.put("score", "\\d+");
        GAME_TEXT_PATTERNS.put("coins", "\\d+");
        GAME_TEXT_PATTERNS.put("multiplier", "x\\d+");
        GAME_TEXT_PATTERNS.put("level", "Level \\d+");
        
        // Power-up and item names
        GAME_TEXT_PATTERNS.put("jetpack", "(?i)jetpack|jet pack");
        GAME_TEXT_PATTERNS.put("magnet", "(?i)magnet|coin magnet");
        GAME_TEXT_PATTERNS.put("boost", "(?i)boost|speed boost");
        GAME_TEXT_PATTERNS.put("mystery", "(?i)mystery|mystery box");
        
        // UI elements
        GAME_TEXT_PATTERNS.put("pause", "(?i)pause");
        GAME_TEXT_PATTERNS.put("resume", "(?i)resume|continue");
        GAME_TEXT_PATTERNS.put("restart", "(?i)restart|try again");
        GAME_TEXT_PATTERNS.put("menu", "(?i)menu|main menu");
        
        // Game state indicators
        GAME_TEXT_PATTERNS.put("game_over", "(?i)game over|crashed");
        GAME_TEXT_PATTERNS.put("new_record", "(?i)new record|high score");
        GAME_TEXT_PATTERNS.put("mission", "(?i)mission|objective");
    }
    
    public interface OCRCallback {
        void onTextDetected(List<DetectedText> detectedTexts);
        void onOCRError(String error);
    }
    
    public static class DetectedText {
        public String text;
        public Rect boundingBox;
        public float confidence;
        public String category;
        public GameAction suggestedAction;
        
        public DetectedText(String text, Rect boundingBox, float confidence) {
            this.text = text;
            this.boundingBox = boundingBox;
            this.confidence = confidence;
            this.category = "unknown";
        }
    }
    
    public OCREngine(Context context) {
        this.context = context;
        initialize();
    }
    
    private void initialize() {
        try {
            // Initialize ML Kit Text Recognizer with Latin script support
            textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            
            // Initialize NLP processor for text interpretation
            nlpProcessor = new NLPProcessor(context);
            // Initialize AI learning
            gameStrategyAgent = new GameStrategyAgent(context);
            
            isInitialized = true;
            Log.d(TAG, "OCR Engine initialized with ML Kit");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize OCR Engine", e);
        }
    }
    
    /**
     * Process screen bitmap and extract actionable text
     */
    public CompletableFuture<List<DetectedText>> processScreenText(Bitmap screenBitmap) {
        CompletableFuture<List<DetectedText>> future = new CompletableFuture<>();
        
        if (!isInitialized || screenBitmap == null) {
            future.completeExceptionally(new RuntimeException("OCR Engine not initialized or invalid bitmap"));
            return future;
        }
        
        try {
            InputImage image = InputImage.fromBitmap(screenBitmap, 0);
            
            textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    List<DetectedText> detectedTexts = processVisionText(visionText);
                    future.complete(detectedTexts);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "OCR processing failed", e);
                    future.completeExceptionally(e);
                });
                
        } catch (Exception e) {
            Log.e(TAG, "Error creating InputImage", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    private List<DetectedText> processVisionText(Text visionText) {
        List<DetectedText> detectedTexts = new ArrayList<>();
        
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                for (Text.Element element : line.getElements()) {
                    String text = element.getText();
                    Rect boundingBox = element.getBoundingBox();
                    float confidence = element.getConfidence();
                    
                    if (boundingBox != null && confidence > 0.5f) {
                        DetectedText detectedText = new DetectedText(text, boundingBox, confidence);
                        
                        // Categorize and analyze text
                        categorizeText(detectedText);
                        generateActionFromText(detectedText);
                        
                        detectedTexts.add(detectedText);
                    }
                }
            }
        }
        
        Log.d(TAG, "Detected " + detectedTexts.size() + " text elements");
        return detectedTexts;
    }
    
    private void categorizeText(DetectedText detectedText) {
        String text = detectedText.text.toLowerCase().trim();
        
        // Check against known game patterns
        for (Map.Entry<String, String> pattern : GAME_TEXT_PATTERNS.entrySet()) {
            if (text.matches(pattern.getValue())) {
                detectedText.category = pattern.getKey();
                return;
            }
        }
        
        // Fallback categorization based on content
        if (text.matches("\\d+")) {
            detectedText.category = "number";
        } else if (text.matches("(?i)[a-z]+")) {
            detectedText.category = "word";
        } else if (text.contains("x") && text.matches(".*\\d+.*")) {
            detectedText.category = "multiplier";
        } else {
            detectedText.category = "misc";
        }
    }
    
    private void generateActionFromText(DetectedText detectedText) {
        try {
            String actionCommand = createNLPCommand(detectedText);
            
            if (actionCommand != null) {
                NLPProcessor.ActionIntent intent = nlpProcessor.processNaturalLanguageCommand(actionCommand);
                
                if (intent != null) {
                    detectedText.suggestedAction = new GameAction(
                        intent.getAction(),
                        detectedText.boundingBox.centerX(),
                        detectedText.boundingBox.centerY(),
                        intent.getConfidence() * detectedText.confidence,
                        "ocr_text_" + detectedText.category
                    );
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to generate action from text: " + detectedText.text, e);
        }
    }
    
    private String createNLPCommand(DetectedText detectedText) {
        switch (detectedText.category) {
            case "pause":
                return "pause the game";
            case "resume":
                return "resume the game";
            case "restart":
                return "restart the game";
            case "jetpack":
                return "activate jetpack powerup";
            case "magnet":
                return "activate coin magnet";
            case "boost":
                return "activate speed boost";
            case "mystery":
                return "open mystery box";
            case "score":
                return "check current score";
            case "coins":
                return "count available coins";
            case "game_over":
                return "handle game over state";
            case "new_record":
                return "celebrate new high score";
            default:
                return "tap on " + detectedText.text;
        }
    }
    
    /**
     * Process specific screen region for targeted OCR
     */
    public CompletableFuture<List<DetectedText>> processScreenRegion(Bitmap screenBitmap, Rect region) {
        if (screenBitmap == null || region == null) {
            CompletableFuture<List<DetectedText>> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Invalid bitmap or region"));
            return future;
        }
        
        try {
            // Crop bitmap to specified region
            Bitmap croppedBitmap = Bitmap.createBitmap(
                screenBitmap, 
                region.left, region.top, 
                region.width(), region.height()
            );
            
            return processScreenText(croppedBitmap);
            
        } catch (Exception e) {
            CompletableFuture<List<DetectedText>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Extract game score from screen
     */
    public CompletableFuture<Integer> extractGameScore(Bitmap screenBitmap) {
        return processScreenText(screenBitmap).thenApply(detectedTexts -> {
            for (DetectedText text : detectedTexts) {
                if ("score".equals(text.category) || "number".equals(text.category)) {
                    try {
                        // Extract numeric value
                        String numericText = text.text.replaceAll("[^0-9]", "");
                        if (!numericText.isEmpty()) {
                            return Integer.parseInt(numericText);
                        }
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Failed to parse score: " + text.text);
                    }
                }
            }
            return -1; // Score not found
        });
    }
    
    /**
     * Detect UI state from text elements
     */
    public CompletableFuture<GameUIState> detectUIState(Bitmap screenBitmap) {
        return processScreenText(screenBitmap).thenApply(detectedTexts -> {
            GameUIState uiState = new GameUIState();
            
            for (DetectedText text : detectedTexts) {
                switch (text.category) {
                    case "pause":
                        uiState.isPaused = true;
                        uiState.pauseButtonLocation = text.boundingBox;
                        break;
                    case "game_over":
                        uiState.isGameOver = true;
                        break;
                    case "new_record":
                        uiState.hasNewRecord = true;
                        break;
                    case "score":
                        try {
                            String numericText = text.text.replaceAll("[^0-9]", "");
                            if (!numericText.isEmpty()) {
                                uiState.currentScore = Integer.parseInt(numericText);
                            }
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Failed to parse score from UI");
                        }
                        break;
                }
            }
            
            return uiState;
        });
    }
    
    /**
     * Connect OCR results to Strategy AI system
     */
    public List<GameAction> generateStrategicActions(List<DetectedText> detectedTexts, int currentScore) {
        List<GameAction> actions = new ArrayList<>();
        
        for (DetectedText text : detectedTexts) {
            if (text.suggestedAction != null) {
                // Adjust action priority based on game state
                float priority = calculateActionPriority(text, currentScore);
                // Create new GameAction with adjusted priority instead of modifying existing one
                float adjustedPriority = calculateAdjustedPriority(priority, currentScore);
                GameAction adjustedAction = new GameAction(
                        text.suggestedAction.getActionType(),
                        text.suggestedAction.getX(),
                        text.suggestedAction.getY(),
                        text.suggestedAction.getConfidence(),
                        text.suggestedAction.getObjectName(),
                        adjustedPriority
                );
                text.suggestedAction = adjustedAction;

                actions.add(text.suggestedAction);
            }
        }
        
        // Sort actions by priority
        actions.sort((a, b) -> Float.compare(b.getPriority(), a.getPriority()));
        
        return actions;
    }
    
    private float calculateActionPriority(DetectedText text, int currentScore) {
        float basePriority = text.confidence;
        
        switch (text.category) {
            case "game_over":
                return 1.0f; // Highest priority - need to restart
            case "pause":
                return 0.9f; // High priority for UI control
            case "jetpack":
            case "magnet":
            case "boost":
                return 0.8f; // High priority for power-ups
            case "restart":
                return 0.7f; // Medium-high priority
            case "score":
            case "coins":
                return 0.3f; // Low priority - informational
            default:
                return basePriority;
        }
    }
    
    public static class GameUIState {
        public boolean isPaused = false;
        public boolean isGameOver = false;
        public boolean hasNewRecord = false;
        public int currentScore = -1;
        public Rect pauseButtonLocation = null;
        public List<Rect> powerUpLocations = new ArrayList<>();
    }
    
    public void cleanup() {
        if (textRecognizer != null) {
            textRecognizer.close();
        }
        isInitialized = false;
        Log.d(TAG, "OCR Engine cleaned up");
    }
    private float calculateAdjustedPriority(float basePriority, int currentScore) {
        // Adjust priority based on game score and context
        return Math.min(1.0f, basePriority * (1.0f + currentScore / 10000.0f));
    }
    private int extractDeathCount(Bitmap screen) {
        try {
            // Deaths usually shown in scoreboard/HUD area
            Rect deathRegion = new Rect(screen.getWidth()/2 - 100, 0, screen.getWidth()/2 + 100, 120);
            String text = extractTextFromRegion(screen, deathRegion);

            // Look for "Deaths: 3", "D: 2", "3/5/1" (K/D/A format)
            if (text.contains("/")) {
                String[] parts = text.split("/");
                if (parts.length >= 2) {
                    return Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
                }
            } else if (text.toLowerCase().contains("death")) {
                return Integer.parseInt(text.replaceAll("[^0-9]", ""));
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract death count", e);
        }
        return 0; // Only fallback when OCR fails
    }

    private int extractAssistCount(Bitmap screen) {
        try {
            Rect assistRegion = new Rect(screen.getWidth()/2 - 50, 0, screen.getWidth()/2 + 150, 120);
            String text = extractTextFromRegion(screen, assistRegion);

            // Look for "Assists: 5", "A: 3", "5/2/7" format
            if (text.contains("/")) {
                String[] parts = text.split("/");
                if (parts.length >= 3) {
                    return Integer.parseInt(parts[2].replaceAll("[^0-9]", ""));
                }
            } else if (text.toLowerCase().contains("assist")) {
                return Integer.parseInt(text.replaceAll("[^0-9]", ""));
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract assist count", e);
        }
        return 0;
    }
    public float extractZoneTimer(Bitmap screen) {
        Map<String, Float> minimapData = extractMinimapData(screen);
        return minimapData.getOrDefault("zone_timer", 0f);
    }

    public float extractManaLevel(Bitmap screen) {
        try {
            Rect manaRegion = new Rect(0, screen.getHeight() - 160, 300, screen.getHeight() - 60);
            String text = extractTextFromRegion(screen, manaRegion);

            if (text.toLowerCase().contains("mana") || text.toLowerCase().contains("mp")) {
                String numericText = text.replaceAll("[^0-9.]", "");
                if (!numericText.isEmpty()) {
                    return Float.parseFloat(numericText);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract mana level", e);
        }
        return 100f;
    }

    public float extractUnitCount(Bitmap screen) {
        try {
            Rect unitRegion = new Rect(0, 0, screen.getWidth(), 150);
            String text = extractTextFromRegion(screen, unitRegion);

            Pattern unitPattern = Pattern.compile("(\\d+)\\s*units?|(\\d+)\\s*army");
            Matcher matcher = unitPattern.matcher(text.toLowerCase());

            if (matcher.find()) {
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    if (matcher.group(i) != null) {
                        return Float.parseFloat(matcher.group(i));
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract unit count", e);
        }
        return 0f;
    }

    public float extractScore(Bitmap screen) {
        try {
            Rect scoreRegion = new Rect(0, 0, screen.getWidth(), 200);
            String text = extractTextFromRegion(screen, scoreRegion);

            Pattern scorePattern = Pattern.compile("(?i)score:?\\s*(\\d+)|(\\d{4,})");
            Matcher matcher = scorePattern.matcher(text);

            if (matcher.find()) {
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    if (matcher.group(i) != null) {
                        return Float.parseFloat(matcher.group(i));
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract score", e);
        }
        return 0f;
    }
}
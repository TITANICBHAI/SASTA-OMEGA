package com.gestureai.gameautomation.utils;

import android.content.Context;
import android.util.Log;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;

import com.gestureai.gameautomation.models.ActionIntent;
import com.gestureai.gameautomation.models.ActionTemplate;

// Using ActionIntent model class

public class NLPProcessor {
    private boolean nd4jNLPEnabled = true;
    private Map<String, INDArray> wordEmbeddings;
    private static final String TAG = "NLPProcessor";
    
    private Context context;
    private Map<String, String> actionMappings;
    private Map<String, List<String>> gameActionTemplates;
    private boolean isInitialized = false;
    
    // OpenNLP models
    private SentenceDetectorME sentenceDetector;
    private TokenizerME tokenizer;
    private POSTaggerME posTagger;
    private NameFinderME personFinder;
    private NameFinderME locationFinder;


    // Universal game action vocabulary
    private static final Map<String, String> GAME_ACTIONS = new HashMap<>();
    static {
        // Movement actions - universal across games
        GAME_ACTIONS.put("jump", "JUMP");
        GAME_ACTIONS.put("hop", "JUMP");
        GAME_ACTIONS.put("leap", "JUMP");
        GAME_ACTIONS.put("up", "JUMP");
        GAME_ACTIONS.put("rise", "JUMP");
        
        GAME_ACTIONS.put("slide", "SLIDE");
        GAME_ACTIONS.put("duck", "SLIDE");
        GAME_ACTIONS.put("down", "SLIDE");
        GAME_ACTIONS.put("crouch", "SLIDE");
        GAME_ACTIONS.put("lower", "SLIDE");
        
        GAME_ACTIONS.put("left", "MOVE_LEFT");
        GAME_ACTIONS.put("west", "MOVE_LEFT");
        GAME_ACTIONS.put("right", "MOVE_RIGHT");
        GAME_ACTIONS.put("east", "MOVE_RIGHT");
        
        // Touch actions - common in mobile games
        GAME_ACTIONS.put("tap", "TAP");
        GAME_ACTIONS.put("touch", "TAP");
        GAME_ACTIONS.put("press", "TAP");
        GAME_ACTIONS.put("click", "TAP");
        GAME_ACTIONS.put("hit", "TAP");
        
        GAME_ACTIONS.put("swipe", "SWIPE");
        GAME_ACTIONS.put("drag", "SWIPE");
        GAME_ACTIONS.put("flick", "SWIPE");
        GAME_ACTIONS.put("stroke", "SWIPE");
        
        // Collection actions - RPGs, platformers, runners
        GAME_ACTIONS.put("collect", "COLLECT");
        GAME_ACTIONS.put("grab", "COLLECT");
        GAME_ACTIONS.put("take", "COLLECT");
        GAME_ACTIONS.put("get", "COLLECT");
        GAME_ACTIONS.put("pickup", "COLLECT");
        GAME_ACTIONS.put("acquire", "COLLECT");
        GAME_ACTIONS.put("gather", "COLLECT");
        
        // Combat actions - action games, fighters
        GAME_ACTIONS.put("attack", "ATTACK");
        GAME_ACTIONS.put("fight", "ATTACK");
        GAME_ACTIONS.put("strike", "ATTACK");
        GAME_ACTIONS.put("hit", "ATTACK");
        GAME_ACTIONS.put("punch", "ATTACK");
        GAME_ACTIONS.put("kick", "ATTACK");
        
        GAME_ACTIONS.put("defend", "DEFEND");
        GAME_ACTIONS.put("block", "DEFEND");
        GAME_ACTIONS.put("guard", "DEFEND");
        GAME_ACTIONS.put("shield", "DEFEND");
        
        // Power-up actions - various genres
        GAME_ACTIONS.put("activate", "ACTIVATE_POWERUP");
        GAME_ACTIONS.put("use", "ACTIVATE_POWERUP");
        GAME_ACTIONS.put("trigger", "ACTIVATE_POWERUP");
        GAME_ACTIONS.put("enable", "ACTIVATE_POWERUP");
        GAME_ACTIONS.put("deploy", "ACTIVATE_POWERUP");
        
        // Avoidance actions - runners, platformers
        GAME_ACTIONS.put("avoid", "AVOID");
        GAME_ACTIONS.put("dodge", "AVOID");
        GAME_ACTIONS.put("evade", "AVOID");
        GAME_ACTIONS.put("escape", "AVOID");
        GAME_ACTIONS.put("flee", "AVOID");
        
        // Strategy actions - puzzle, strategy games
        GAME_ACTIONS.put("place", "PLACE");
        GAME_ACTIONS.put("put", "PLACE");
        GAME_ACTIONS.put("position", "PLACE");
        GAME_ACTIONS.put("set", "PLACE");
        
        GAME_ACTIONS.put("remove", "REMOVE");
        GAME_ACTIONS.put("delete", "REMOVE");
        GAME_ACTIONS.put("clear", "REMOVE");
        GAME_ACTIONS.put("eliminate", "REMOVE");
        
        GAME_ACTIONS.put("rotate", "ROTATE");
        GAME_ACTIONS.put("turn", "ROTATE");
        GAME_ACTIONS.put("spin", "ROTATE");
        GAME_ACTIONS.put("twist", "ROTATE");
        
        // Menu actions - universal
        GAME_ACTIONS.put("pause", "PAUSE");
        GAME_ACTIONS.put("stop", "PAUSE");
        GAME_ACTIONS.put("halt", "PAUSE");
        
        GAME_ACTIONS.put("resume", "RESUME");
        GAME_ACTIONS.put("continue", "RESUME");
        GAME_ACTIONS.put("start", "RESUME");
        
        GAME_ACTIONS.put("restart", "RESTART");
        GAME_ACTIONS.put("reset", "RESTART");
        GAME_ACTIONS.put("retry", "RESTART");
        
        GAME_ACTIONS.put("quit", "QUIT");
        GAME_ACTIONS.put("exit", "QUIT");
        GAME_ACTIONS.put("leave", "QUIT");
        GAME_ACTIONS.put("close", "QUIT");
        // Add to existing GAME_ACTIONS map
// Strategic concepts
        GAME_ACTIONS.put("flank", "FLANK_POSITION");
        GAME_ACTIONS.put("coordinate", "COORDINATE_TEAM");
        GAME_ACTIONS.put("pressure", "APPLY_PRESSURE");
        GAME_ACTIONS.put("control", "CONTROL_AREA");
        GAME_ACTIONS.put("breakthrough", "BREAKTHROUGH_POSITION");
        GAME_ACTIONS.put("disengage", "TACTICAL_RETREAT");

// Contextual modifiers
        GAME_ACTIONS.put("aggressive", "AGGRESSIVE_STANCE");
        GAME_ACTIONS.put("defensive", "DEFENSIVE_STANCE");
        GAME_ACTIONS.put("tactical", "TACTICAL_APPROACH");
        GAME_ACTIONS.put("coordinated", "TEAM_COORDINATION");
    }
    
    public NLPProcessor(Context context) {
        this.context = context;
        initialize();
    }
    
    private void initialize() {
        try {
            loadActionMappings();
            initializeOpenNLPModels();
            loadGameActionTemplates();
            // Initialize ND4J word embeddings
            if (nd4jNLPEnabled) {
                initializeWordEmbeddings();
            }
            isInitialized = true;
            Log.d(TAG, "Advanced NLP Processor with Apache OpenNLP initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize NLP Processor", e);
        }
    }
    
    private void loadActionMappings() {
        actionMappings = new HashMap<>();
        actionMappings.putAll(GAME_ACTIONS);
        
        // Load custom mappings from JSON if available
        try {
            String customMappings = loadAssetFile("custom_action_mappings.json");
            if (customMappings != null) {
                JSONObject json = new JSONObject(customMappings);
                JSONObject mappings = json.getJSONObject("action_mappings");

                Iterator<String> keys = mappings.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    actionMappings.put(key.toLowerCase(), mappings.getString(key));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Custom action mappings not found, using defaults");
        }
    }

    
    private void initializeOpenNLPModels() throws IOException {
        try {
            // Initialize sentence detector
            InputStream sentModelStream = context.getAssets().open("opennlp/en-sent.bin");
            SentenceModel sentenceModel = new SentenceModel(sentModelStream);
            sentenceDetector = new SentenceDetectorME(sentenceModel);
            sentModelStream.close();
            
            // Initialize tokenizer
            InputStream tokenModelStream = context.getAssets().open("opennlp/en-token.bin");
            TokenizerModel tokenizerModel = new TokenizerModel(tokenModelStream);
            tokenizer = new TokenizerME(tokenizerModel);
            tokenModelStream.close();
            
            // Initialize POS tagger
            InputStream posModelStream = context.getAssets().open("opennlp/en-pos-maxent.bin");
            POSModel posModel = new POSModel(posModelStream);
            posTagger = new POSTaggerME(posModel);
            posModelStream.close();
            
            // Initialize name finders
            InputStream personModelStream = context.getAssets().open("opennlp/en-ner-person.bin");
            TokenNameFinderModel personModel = new TokenNameFinderModel(personModelStream);
            personFinder = new NameFinderME(personModel);
            personModelStream.close();
            
            InputStream locationModelStream = context.getAssets().open("opennlp/en-ner-location.bin");
            TokenNameFinderModel locationModel = new TokenNameFinderModel(locationModelStream);
            locationFinder = new NameFinderME(locationModel);
            locationModelStream.close();
            
            Log.d(TAG, "All OpenNLP models loaded successfully");
            
        } catch (IOException e) {
            Log.w(TAG, "OpenNLP models not found, falling back to basic processing");
            // Continue without advanced NLP features
        }
    }
    
    private void loadGameActionTemplates() {
        gameActionTemplates = new HashMap<>();
        
        // Runner games (Subway Surfers, Temple Run, etc.)
        gameActionTemplates.put("runner", Arrays.asList(
            "when obstacle ahead then jump over it",
            "if train coming then slide under",
            "when coin visible then collect it",
            "if powerup available then activate it",
            "when barrier ahead then jump",
            "if moving train then duck down"
        ));
        
        // Puzzle games (Candy Crush, Match 3, etc.)
        gameActionTemplates.put("puzzle", Arrays.asList(
            "when matching pieces then tap to swap",
            "if special piece available then activate",
            "when combo possible then execute sequence",
            "if blocker present then clear it first",
            "when objective close then prioritize"
        ));
        
        // Action games (shooters, fighters, etc.)
        gameActionTemplates.put("action", Arrays.asList(
            "when enemy appears then attack",
            "if health low then defend or retreat",
            "when weapon available then pick up",
            "if enemy attacking then block or dodge",
            "when boss fight then use special attacks"
        ));
        
        // Strategy games (tower defense, RTS, etc.)
        gameActionTemplates.put("strategy", Arrays.asList(
            "when resource available then collect",
            "if enemy advancing then place defense",
            "when upgrade possible then improve units",
            "if territory empty then expand",
            "when enemy weak then launch attack"
        ));
        
        // RPG games (adventure, role-playing, etc.)
        gameActionTemplates.put("rpg", Arrays.asList(
            "when quest marker visible then follow",
            "if enemy encountered then engage or flee",
            "when loot available then collect",
            "if level up available then upgrade skills",
            "when NPC nearby then interact"
        ));
    }
    
    public ActionIntent processNaturalLanguageCommand(String command) {
        if (!isInitialized || command == null || command.trim().isEmpty()) {
            return null;
        }
        
        String cleanCommand = command.toLowerCase().trim();
        Log.d(TAG, "Processing advanced NLP command: " + cleanCommand);
        
        try {
            // Use OpenNLP for advanced processing if models are available
            if (sentenceDetector != null && tokenizer != null) {
                return processWithOpenNLP(cleanCommand);
            } else {
                // Fallback to basic processing
                return processBasicCommand(cleanCommand);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing NLP command", e);
            return processBasicCommand(cleanCommand);
        }
    }
    
    private ActionIntent processWithOpenNLP(String command) {
        // Detect sentences
        String[] sentences = sentenceDetector.sentDetect(command);
        
        for (String sentence : sentences) {
            // Tokenize sentence
            String[] tokens = tokenizer.tokenize(sentence);
            
            // Get part-of-speech tags
            String[] posTags = posTagger != null ? posTagger.tag(tokens) : new String[tokens.length];
            
            // Extract named entities
            Span[] personSpans = personFinder != null ? personFinder.find(tokens) : new Span[0];
            Span[] locationSpans = locationFinder != null ? locationFinder.find(tokens) : new Span[0];
            
            // Analyze semantic structure
            ActionIntent intent = analyzeSemanticStructure(tokens, posTags, personSpans, locationSpans, sentence);
            
            if (intent != null) {
                return intent;
            }
        }
        
        // Fallback to basic processing
        return processBasicCommand(command);
    }
    
    private ActionIntent analyzeSemanticStructure(String[] tokens, String[] posTags, 
                                                 Span[] personSpans, Span[] locationSpans, 
                                                 String originalSentence) {
        
        // Identify action verbs (VB, VBD, VBG, VBN, VBP, VBZ)
        List<String> actionVerbs = new ArrayList<>();
        List<String> objects = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        
        for (int i = 0; i < tokens.length && i < posTags.length; i++) {
            String token = tokens[i].toLowerCase();
            String pos = posTags[i];
            
            // Extract verbs as potential actions
            if (pos.startsWith("VB")) {
                actionVerbs.add(token);
            }
            
            // Extract nouns as potential objects
            if (pos.startsWith("NN")) {
                objects.add(token);
            }
            
            // Extract conditional words
            if (token.equals("if") || token.equals("when") || token.equals("while")) {
                conditions.add(token);
            }
        }
        
        // Map extracted verbs to game actions
        for (String verb : actionVerbs) {
            String action = actionMappings.get(verb);
            if (action != null) {
                Map<String, Object> params = extractAdvancedParameters(originalSentence, objects, conditions);
                float confidence = calculateAdvancedConfidence(verb, objects, conditions, originalSentence);
                return new ActionIntent(action, params, confidence);
            }
        }
        
        // Check for compound actions (e.g., "jump over obstacle")
        return analyzeCompoundActions(tokens, objects, originalSentence);
    }
    
    private ActionIntent analyzeCompoundActions(String[] tokens, List<String> objects, String sentence) {
        String tokenString = String.join(" ", tokens);
        Map<String, Object> params = new HashMap<>();
        
        // Pattern: action + direction/object
        if (tokenString.contains("jump") && (tokenString.contains("over") || tokenString.contains("up"))) {
            params.put("direction", "up");
            if (objects.contains("obstacle") || objects.contains("barrier")) {
                params.put("target", "obstacle");
            }
            return new ActionIntent("JUMP", params, 0.85f);
        }
        
        if (tokenString.contains("slide") && (tokenString.contains("under") || tokenString.contains("down"))) {
            params.put("direction", "down");
            if (objects.contains("train") || objects.contains("barrier")) {
                params.put("target", "train");
            }
            return new ActionIntent("SLIDE", params, 0.85f);
        }
        
        if (tokenString.contains("move") || tokenString.contains("go")) {
            if (tokenString.contains("left")) {
                params.put("direction", "left");
                return new ActionIntent("MOVE_LEFT", params, 0.8f);
            }
            if (tokenString.contains("right")) {
                params.put("direction", "right");
                return new ActionIntent("MOVE_RIGHT", params, 0.8f);
            }
        }
        
        // Pattern: collect + object
        if ((tokenString.contains("collect") || tokenString.contains("grab") || tokenString.contains("take")) &&
            (objects.contains("coin") || objects.contains("money") || objects.contains("point"))) {
            params.put("object_type", "collectible");
            return new ActionIntent("COLLECT", params, 0.9f);
        }
        
        // Pattern: avoid + object
        if ((tokenString.contains("avoid") || tokenString.contains("dodge")) &&
            (objects.contains("obstacle") || objects.contains("train") || objects.contains("barrier"))) {
            params.put("object_type", "obstacle");
            return new ActionIntent("AVOID", params, 0.88f);
        }
        
        return null;
    }
    
    private Map<String, Object> extractAdvancedParameters(String sentence, List<String> objects, List<String> conditions) {
        Map<String, Object> params = new HashMap<>();
        
        // Extract timing information
        if (sentence.contains("quickly") || sentence.contains("fast")) {
            params.put("speed", "fast");
        } else if (sentence.contains("slowly") || sentence.contains("careful")) {
            params.put("speed", "slow");
        }
        
        // Extract priority information
        if (sentence.contains("priority") || sentence.contains("first") || sentence.contains("urgent")) {
            params.put("priority", "high");
        }
        
        // Extract repetition information
        if (sentence.contains("repeat") || sentence.contains("continue") || sentence.contains("keep")) {
            params.put("repeat", true);
        }
        
        // Extract conditional information
        if (!conditions.isEmpty()) {
            params.put("conditional", true);
            params.put("condition_type", conditions.get(0));
        }
        
        // Extract object targets
        if (!objects.isEmpty()) {
            params.put("targets", objects);
        }
        
        return params;
    }
    
    private float calculateAdvancedConfidence(String verb, List<String> objects, 
                                            List<String> conditions, String sentence) {
        float baseConfidence = 0.7f;
        
        // Boost confidence based on context
        if (!objects.isEmpty()) {
            baseConfidence += 0.1f; // Has clear objects
        }
        
        if (!conditions.isEmpty()) {
            baseConfidence += 0.05f; // Has conditional structure
        }
        
        // Boost based on sentence completeness
        if (sentence.length() > 10) {
            baseConfidence += 0.05f; // More detailed command
        }
        
        // Check for game-specific context
        for (String gameType : gameActionTemplates.keySet()) {
            List<String> templates = gameActionTemplates.get(gameType);
            for (String template : templates) {
                if (calculateSimilarity(sentence.toLowerCase(), template.toLowerCase()) > 0.6) {
                    baseConfidence += 0.1f;
                    break;
                }
            }
        }
        
        return Math.min(1.0f, baseConfidence);
    }
    
    private ActionIntent processBasicCommand(String command) {
        // Try direct action mapping first
        for (Map.Entry<String, String> entry : actionMappings.entrySet()) {
            if (command.contains(entry.getKey())) {
                Map<String, Object> params = extractAdvancedParameters(command, new ArrayList<>(), new ArrayList<>());
                return new ActionIntent(entry.getValue(), params, 0.8f);
            }
        }
        
        // Try fuzzy matching as fallback
        return fuzzyMatch(command);
    }
    
    public List<ActionIntent> processObjectLabelCommand(String objectLabel, String actionLogic) {
        List<ActionIntent> intents = new ArrayList<>();
        
        try {
            // Try to parse as JSON first
            JSONObject json = new JSONObject(actionLogic);
            
            if (json.has("actions")) {
                JSONArray actions = json.getJSONArray("actions");
                for (int i = 0; i < actions.length(); i++) {
                    JSONObject action = actions.getJSONObject(i);
                    String actionType = action.getString("type");
                    
                    Map<String, Object> params = new HashMap<>();
                    if (action.has("parameters")) {
                        JSONObject parameters = action.getJSONObject("parameters");
                        Iterator<String> keys = parameters.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            params.put(key, parameters.get(key));
                        }
                    }
                    
                    float confidence = action.has("confidence") ? 
                        (float) action.getDouble("confidence") : 1.0f;
                    
                    intents.add(new ActionIntent(actionType, params, confidence));
                }
            }
        } catch (JSONException e) {
            // If not JSON, treat as natural language
            ActionIntent intent = processNaturalLanguageCommand(actionLogic);
            if (intent != null) {
                intent.getParameters().put("object_label", objectLabel);
                intents.add(intent);
            }
        }
        
        return intents;
    }
    
    private ActionIntent fuzzyMatch(String command) {
        String bestMatch = null;
        float bestScore = 0.0f;
        
        for (String action : actionMappings.keySet()) {
            float similarity = calculateSimilarity(command, action);
            if (similarity > bestScore && similarity > 0.6f) {
                bestScore = similarity;
                bestMatch = action;
            }
        }
        
        if (bestMatch != null) {
            Map<String, Object> params = extractAdvancedParameters(command, new ArrayList<>(), new ArrayList<>());
            return new ActionIntent(actionMappings.get(bestMatch), params, bestScore);
        }
        
        return null;
    }
    
    // Game-specific NLP processing methods
    public ActionIntent processGameSpecificCommand(String command, String gameType) {
        if (!isInitialized || command == null || gameType == null) {
            return null;
        }
        
        // Get game-specific templates
        List<String> templates = gameActionTemplates.get(gameType.toLowerCase());
        if (templates != null) {
            // Find best matching template
            String bestTemplate = null;
            float bestSimilarity = 0.0f;
            
            for (String template : templates) {
                float similarity = calculateSimilarity(command.toLowerCase(), template.toLowerCase());
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestTemplate = template;
                }
            }
            
            // If good match found, process with template context
            if (bestSimilarity > 0.7f && bestTemplate != null) {
                ActionIntent intent = processNaturalLanguageCommand(command);
                if (intent != null) {
                    intent.getParameters().put("game_type", gameType);
                    intent.getParameters().put("template_match", bestTemplate);
                    intent.getParameters().put("template_confidence", bestSimilarity);
                    return intent;
                }
            }
        }
        
        // Fallback to general processing
        return processNaturalLanguageCommand(command);
    }
    
    // Multi-language support for commands
    public ActionIntent processMultiLanguageCommand(String command, String language) {
        if (!isInitialized) {
            return null;
        }
        
        // For now, translate basic keywords to English
        String translatedCommand = translateBasicKeywords(command, language);
        return processNaturalLanguageCommand(translatedCommand);
    }
    
    private String translateBasicKeywords(String command, String language) {
        // Basic translation mapping for common gaming terms
        Map<String, Map<String, String>> translations = new HashMap<>();
        
        // Spanish translations
        Map<String, String> spanish = new HashMap<>();
        spanish.put("saltar", "jump");
        spanish.put("deslizar", "slide");
        spanish.put("izquierda", "left");
        spanish.put("derecha", "right");
        spanish.put("recoger", "collect");
        spanish.put("evitar", "avoid");
        translations.put("es", spanish);
        
        // French translations
        Map<String, String> french = new HashMap<>();
        french.put("sauter", "jump");
        french.put("glisser", "slide");
        french.put("gauche", "left");
        french.put("droite", "right");
        french.put("collecter", "collect");
        french.put("éviter", "avoid");
        translations.put("fr", french);
        
        // Apply translations
        Map<String, String> langMap = translations.get(language);
        if (langMap != null) {
            String translated = command;
            for (Map.Entry<String, String> entry : langMap.entrySet()) {
                translated = translated.replace(entry.getKey(), entry.getValue());
            }
            return translated;
        }
        
        return command; // Return original if no translation available
    }
    
    // Advanced context-aware processing
    public List<ActionIntent> processContextualCommands(List<String> commands, String gameContext) {
        List<ActionIntent> intents = new ArrayList<>();
        
        for (String command : commands) {
            ActionIntent intent = processGameSpecificCommand(command, gameContext);
            if (intent != null) {
                // Add sequence information for chained commands
                intent.getParameters().put("sequence_index", intents.size());
                intent.getParameters().put("sequence_total", commands.size());
                intents.add(intent);
            }
        }
        
        return intents;
    }
    
    // Cleanup resources
    public void cleanup() {
        if (personFinder != null) {
            personFinder.clearAdaptiveData();
        }
        if (locationFinder != null) {
            locationFinder.clearAdaptiveData();
        }
        
        isInitialized = false;
        Log.d(TAG, "NLP Processor cleaned up");
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }

    private float calculateSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) return 0.0f;
        if (str1.equals(str2)) return 1.0f;

        // Simple Levenshtein distance-based similarity
        int maxLen = Math.max(str1.length(), str2.length());
        if (maxLen == 0) return 1.0f;

        int distance = levenshteinDistance(str1, str2);
        return 1.0f - (float) distance / maxLen;
    }

    private int levenshteinDistance(String a, String b) {
        if (a.length() == 0) return b.length();
        if (b.length() == 0) return a.length();

        int[][] matrix = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) matrix[i][0] = i;
        for (int j = 0; j <= b.length(); j++) matrix[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                matrix[i][j] = Math.min(Math.min(
                                matrix[i - 1][j] + 1,
                                matrix[i][j - 1] + 1),
                        matrix[i - 1][j - 1] + cost);
            }
        }

        return matrix[a.length()][b.length()];
    }
    
    private Map<String, Object> extractParameters(String command) {
        Map<String, Object> parameters = new HashMap<>();
        
        // Extract coordinates if mentioned
        java.util.regex.Pattern coordPattern = java.util.regex.Pattern.compile("(?i).*at\\s*\\(?([0-9]+)[,\\s]+([0-9]+)\\)?.*");
        java.util.regex.Matcher coordMatcher = coordPattern.matcher(command);
        if (coordMatcher.find()) {
            parameters.put("x", Integer.parseInt(coordMatcher.group(1)));
            parameters.put("y", Integer.parseInt(coordMatcher.group(2)));
        }
        
        // Extract duration if mentioned
        java.util.regex.Pattern durationPattern = java.util.regex.Pattern.compile("(?i).*for\\s*([0-9]+)\\s*(ms|milliseconds|seconds?).*");
        java.util.regex.Matcher durationMatcher = durationPattern.matcher(command);
        if (durationMatcher.find()) {
            int duration = Integer.parseInt(durationMatcher.group(1));
            if (durationMatcher.group(2).startsWith("s")) {
                duration *= 1000; // Convert seconds to milliseconds
            }
            parameters.put("duration", duration);
        }
        
        // Extract priority if mentioned
        java.util.regex.Pattern priorityPattern = java.util.regex.Pattern.compile("(?i).*(high|medium|low)\\s*priority.*");
        java.util.regex.Matcher priorityMatcher = priorityPattern.matcher(command);
        if (priorityMatcher.find()) {
            parameters.put("priority", priorityMatcher.group(1));
        }
        
        return parameters;
    }

    private String loadAssetFile(String filename) {
        try {
            InputStream inputStream = context.getAssets().open(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line);
            }

            reader.close();
            inputStream.close();
            return content.toString();

        } catch (IOException e) {
            Log.w(TAG, "Asset file not found: " + filename);
            return null;
        }
    }
    
    // Internal ActionIntent class for NLP processing
    public static class ActionIntent {
        private String action;
        private Map<String, Object> parameters;
        private float confidence;
        
        public ActionIntent(String action, Map<String, Object> parameters, float confidence) {
            this.action = action;
            this.parameters = parameters != null ? parameters : new HashMap<>();
            this.confidence = confidence;
        }
        public List<String> getEntityTypes() {
            if (parameters.containsKey("entityTypes")) {
                Object entityTypes = parameters.get("entityTypes");
                if (entityTypes instanceof List) {
                    return (List<String>) entityTypes;
                }
            }
            return new ArrayList<>();
        }
        public String getAction() { return action; }
        public Map<String, Object> getParameters() { return parameters; }
        public float getConfidence() { return confidence; }
        
        public String getParameterAsString(String key) {
            Object value = parameters.get(key);
            return value != null ? value.toString() : null;
        }
        
        public Integer getParameterAsInt(String key) {
            Object value = parameters.get(key);
            if (value instanceof Integer) return (Integer) value;
            if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }
        
        public Float getParameterAsFloat(String key) {
            Object value = parameters.get(key);
            if (value instanceof Float) return (Float) value;
            if (value instanceof Double) return ((Double) value).floatValue();
            if (value instanceof String) {
                try {
                    return Float.parseFloat((String) value);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }
    }
    private void initializeWordEmbeddings() {
        try {
            wordEmbeddings = new HashMap<>();

            // Create simple word embeddings for game actions
            for (String word : GAME_ACTIONS.keySet()) {
                INDArray embedding = Nd4j.randn(1, 50); // 50-dimensional embeddings
                wordEmbeddings.put(word, embedding);
            }

            Log.d(TAG, "ND4J word embeddings initialized");
        } catch (Exception e) {
            Log.w(TAG, "Word embeddings initialization failed", e);
            nd4jNLPEnabled = false;
        }
    }

    private ActionIntent processWithSemanticSimilarity(String command) {
        if (!nd4jNLPEnabled || wordEmbeddings == null) {
            return processBasicCommand(command);
        }

        try {
            String[] words = command.split("\\s+");
            String bestMatch = null;
            double highestSimilarity = 0.0;

            for (String word : words) {
                INDArray wordVector = wordEmbeddings.get(word.toLowerCase());
                if (wordVector != null) {
                    for (Map.Entry<String, INDArray> entry : wordEmbeddings.entrySet()) {
                        // Calculate cosine similarity using ND4J
                        double similarity = Transforms.cosineSim(wordVector, entry.getValue());

                        if (similarity > highestSimilarity) {
                            highestSimilarity = similarity;
                            bestMatch = entry.getKey();
                        }
                    }
                }
            }

            if (bestMatch != null && highestSimilarity > 0.7f) {
                String action = GAME_ACTIONS.get(bestMatch);
                Map<String, Object> params = new HashMap<>();
                params.put("matched_word", bestMatch);
                params.put("original_command", command);
                return new ActionIntent(action, params, (float) highestSimilarity);
            }

        } catch (Exception e) {
            Log.w(TAG, "Semantic similarity processing failed", e);
        }

        return processBasicCommand(command);
    }
    /**
     * Process complete strategy explanation with Apache OpenNLP
     */
    public StrategyAnalysis processStrategyExplanation(ActionTemplate actionTemplate) {
        StrategyAnalysis analysis = new StrategyAnalysis();

        try {
            // Process strategy explanation
            String[] strategySentences = sentenceDetector.sentDetect(actionTemplate.strategyExplanation);
            String[] strategyTokens = tokenizer.tokenize(actionTemplate.strategyExplanation);
            String[] strategyPOS = posTagger.tag(strategyTokens);

            // Process decision reasoning
            String[] reasoningSentences = sentenceDetector.sentDetect(actionTemplate.decisionReasoning);
            String[] reasoningTokens = tokenizer.tokenize(actionTemplate.decisionReasoning);

            // Extract key strategic concepts
            analysis.strategicConcepts = extractStrategicConcepts(strategyTokens, strategyPOS);
            analysis.reasoningFactors = extractReasoningFactors(reasoningTokens);
            analysis.gameContextElements = extractGameContext(actionTemplate.gameContext);
            analysis.outcomeExpectations = extractOutcomeExpectations(actionTemplate.expectedOutcome);

            // Build strategy knowledge graph
            analysis.strategyGraph = buildStrategyKnowledgeGraph(analysis);

            Log.d(TAG, "Strategy analysis complete: " + analysis.strategicConcepts.size() + " concepts extracted");

        } catch (Exception e) {
            Log.e(TAG, "Error processing strategy explanation", e);
        }

        return analysis;
    }

    public static class StrategyAnalysis {
        public List<String> strategicConcepts;
        public List<String> reasoningFactors;
        public List<String> gameContextElements;
        public List<String> outcomeExpectations;
        public Map<String, List<String>> strategyGraph;
        public float complexityScore;
        public String strategyType;

        public StrategyAnalysis() {
            strategicConcepts = new ArrayList<>();
            reasoningFactors = new ArrayList<>();
            gameContextElements = new ArrayList<>();
            outcomeExpectations = new ArrayList<>();
            strategyGraph = new HashMap<>();
        }
    }

    private List<String> extractStrategicConcepts(String[] tokens, String[] posTags) {
        List<String> concepts = new ArrayList<>();

        // Extract nouns and verb phrases that indicate strategy
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].toLowerCase();
            String pos = posTags[i];

            // Strategic action verbs
            if (pos.startsWith("VB") && isStrategicVerb(token)) {
                concepts.add("action:" + token);
            }

            // Strategic nouns (position, advantage, threat, etc.)
            if (pos.startsWith("NN") && isStrategicNoun(token)) {
                concepts.add("concept:" + token);
            }

            // Strategic adjectives (tactical, aggressive, defensive)
            if (pos.startsWith("JJ") && isStrategicAdjective(token)) {
                concepts.add("modifier:" + token);
            }
        }

        return concepts;
    }
    private boolean isStrategicAdjective(String adjective) {
        String[] strategicAdjectives = {
                "tactical", "aggressive", "defensive", "strategic", "coordinated",
                "flanking", "covering", "supporting", "pressing", "controlling",
                "dominant", "vulnerable", "exposed", "protected", "advantageous"
        };
        return Arrays.asList(strategicAdjectives).contains(adjective);
    }

    private List<String> extractReasoningFactors(String[] tokens) {
        List<String> factors = new ArrayList<>();

        String[] reasoningKeywords = {
                "because", "since", "due", "considering", "given", "therefore",
                "consequently", "thus", "hence", "accordingly", "based", "analysis"
        };

        for (String token : tokens) {
            String lowerToken = token.toLowerCase();
            if (Arrays.asList(reasoningKeywords).contains(lowerToken)) {
                factors.add(lowerToken);
            }
        }

        return factors;
    }

    private List<String> extractGameContext(String gameContext) {
        List<String> contextElements = new ArrayList<>();

        if (gameContext != null && !gameContext.isEmpty()) {
            String[] tokens = gameContext.toLowerCase().split("\\s+");

            String[] contextKeywords = {
                    "enemy", "obstacle", "powerup", "coin", "barrier", "train",
                    "platform", "jump", "slide", "collect", "avoid", "timing"
            };

            for (String token : tokens) {
                if (Arrays.asList(contextKeywords).contains(token)) {
                    contextElements.add(token);
                }
            }
        }

        return contextElements;
    }

    private List<String> extractOutcomeExpectations(String expectedOutcome) {
        List<String> expectations = new ArrayList<>();

        if (expectedOutcome != null && !expectedOutcome.isEmpty()) {
            String[] tokens = expectedOutcome.toLowerCase().split("\\s+");

            String[] outcomeKeywords = {
                    "success", "failure", "score", "points", "survival", "completion",
                    "advancement", "collection", "avoidance", "elimination"
            };

            for (String token : tokens) {
                if (Arrays.asList(outcomeKeywords).contains(token)) {
                    expectations.add(token);
                }
            }
        }

        return expectations;
    }

    private boolean areRelated(String concept, String item) {
        // Simple semantic relatedness check
        String[] conceptWords = concept.split(":");
        String[] itemWords = item.split(":");

        if (conceptWords.length > 1 && itemWords.length > 1) {
            return conceptWords[1].contains(itemWords[1]) ||
                    itemWords[1].contains(conceptWords[1]);
        }

        return concept.toLowerCase().contains(item.toLowerCase()) ||
                item.toLowerCase().contains(concept.toLowerCase());
    }
    private boolean isStrategicVerb(String verb) {
        String[] strategicVerbs = {
                "engage", "flank", "retreat", "advance", "defend", "attack",
                "coordinate", "time", "focus", "eliminate", "control", "secure",
                "distract", "pressure", "maintain", "disrupt", "breakthrough"
        };
        return Arrays.asList(strategicVerbs).contains(verb);
    }

    private boolean isStrategicNoun(String noun) {
        String[] strategicNouns = {
                "advantage", "position", "cover", "threat", "opportunity", "timing",
                "coordination", "pressure", "control", "formation", "route", "angle",
                "visibility", "range", "height", "flank", "choke", "objective"
        };
        return Arrays.asList(strategicNouns).contains(noun);
    }

    private Map<String, List<String>> buildStrategyKnowledgeGraph(StrategyAnalysis analysis) {
        Map<String, List<String>> graph = new HashMap<>();

        // Connect strategic concepts with context and outcomes
        for (String concept : analysis.strategicConcepts) {
            List<String> connections = new ArrayList<>();

            // Link to relevant context elements
            for (String context : analysis.gameContextElements) {
                if (areRelated(concept, context)) {
                    connections.add("context:" + context);
                }
            }

            // Link to expected outcomes
            for (String outcome : analysis.outcomeExpectations) {
                if (areRelated(concept, outcome)) {
                    connections.add("outcome:" + outcome);
                }
            }

            graph.put(concept, connections);
        }

        return graph;
    }
    public List<String> extractTeamIndicators(String teamDescription) {
        List<String> indicators = new ArrayList<>();

        String[] keywords = {"blue", "red", "green", "yellow", "friendly", "enemy", "ally", "hostile",
                "teammate", "opponent", "neutral", "civilian", "squad", "team", "clan"};

        String lowerDesc = teamDescription.toLowerCase();

        for (String keyword : keywords) {
            if (lowerDesc.contains(keyword)) {
                indicators.add(keyword);
            }
        }

        return indicators;
    }
}
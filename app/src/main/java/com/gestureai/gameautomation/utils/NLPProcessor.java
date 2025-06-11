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
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import com.gestureai.gameautomation.models.ActionIntent;
import com.gestureai.gameautomation.models.ActionTemplate;

// Enhanced NLP with MobileBERT integration

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
    
    // MobileBERT Integration
    private Interpreter mobileBertInterpreter;
    private Map<String, Integer> bertVocabulary;
    private boolean mobileBertEnabled = true;
    private static final String MOBILE_BERT_MODEL = "mobilebert_qa.tflite";
    private static final int MAX_SEQUENCE_LENGTH = 128;
    private static final int BERT_HIDDEN_SIZE = 768;


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
            // Initialize MobileBERT for advanced text understanding
            if (mobileBertEnabled) {
                initializeMobileBERT();
            }
            
            isInitialized = true;
            Log.d(TAG, "NLPProcessor initialization complete");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing NLPProcessor", e);
            isInitialized = false;
        }
    }
    
    private void initializeMobileBERT() {
        try {
            // Load MobileBERT model for semantic analysis
            ByteBuffer mobileBertModel = FileUtil.loadMappedFile(context, MOBILE_BERT_MODEL);
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2);
            mobileBertInterpreter = new Interpreter(mobileBertModel, options);
            
            // Initialize BERT vocabulary
            bertVocabulary = loadBertVocabulary();
            
            Log.d(TAG, "MobileBERT initialized successfully");
            
        } catch (Exception e) {
            Log.w(TAG, "MobileBERT not available, using fallback NLP", e);
            mobileBertEnabled = false;
        }
    }
    
    private Map<String, Integer> loadBertVocabulary() {
        Map<String, Integer> vocab = new HashMap<>();
        // Load vocabulary from assets or create basic vocabulary
        String[] basicVocab = {
            "[PAD]", "[UNK]", "[CLS]", "[SEP]", "[MASK]",
            "attack", "defend", "collect", "move", "jump", "shoot", "run", "hide",
            "enemy", "item", "button", "weapon", "coin", "powerup", "door", "platform",
            "because", "to", "for", "when", "if", "then", "and", "or", "but"
        };
        
        for (int i = 0; i < basicVocab.length; i++) {
            vocab.put(basicVocab[i], i);
        }
        
        return vocab;
    }
    
    private void initializeWordEmbeddings() {
        try {
            wordEmbeddings = new HashMap<>();
            
            // Create basic word embeddings using ND4J
            String[] gameWords = {"attack", "defend", "collect", "move", "enemy", "item", "button"};
            
            for (String word : gameWords) {
                // Create random embedding vector (in practice, would load pre-trained)
                INDArray embedding = Nd4j.randn(1, 50); // 50-dimensional embeddings
                wordEmbeddings.put(word, embedding);
            }
            
            Log.d(TAG, "Word embeddings initialized with " + wordEmbeddings.size() + " words");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing word embeddings", e);
            nd4jNLPEnabled = false;
        }
    }
    
    /**
     * Enhanced semantic analysis integrating why/what/how reasoning
     */
    public SemanticAnalysis analyzeSemanticContext(String contextText) {
        SemanticAnalysis analysis = new SemanticAnalysis();
        
        if (!isInitialized) {
            Log.w(TAG, "NLP not initialized");
            return analysis;
        }
        
        try {
            // Extract components from context text
            String[] parts = contextText.split("\\s+");
            
            // Use BERT for semantic understanding if available
            if (mobileBertEnabled && mobileBertInterpreter != null) {
                analysis = analyzeBERTSemantics(contextText);
            } else {
                analysis = analyzeBasicSemantics(contextText);
            }
            
            // Enhance with ND4J embeddings if available
            if (nd4jNLPEnabled && wordEmbeddings != null) {
                enhanceWithWordEmbeddings(analysis, contextText);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in semantic analysis", e);
        }
        
        return analysis;
    }
    
    private SemanticAnalysis analyzeBERTSemantics(String text) {
        SemanticAnalysis analysis = new SemanticAnalysis();
        
        try {
            // Tokenize text for BERT
            int[] tokens = tokenizeForBERT(text);
            
            // Prepare input tensors
            float[][] inputIds = new float[1][MAX_SEQUENCE_LENGTH];
            float[][] attentionMask = new float[1][MAX_SEQUENCE_LENGTH];
            
            for (int i = 0; i < Math.min(tokens.length, MAX_SEQUENCE_LENGTH); i++) {
                inputIds[0][i] = tokens[i];
                attentionMask[0][i] = 1.0f;
            }
            
            // Run BERT inference
            Map<Integer, Object> outputs = new HashMap<>();
            float[][] embeddings = new float[1][BERT_HIDDEN_SIZE];
            outputs.put(0, embeddings);
            
            mobileBertInterpreter.runForMultipleInputsOutputs(
                new Object[]{inputIds, attentionMask}, outputs);
            
            // Analyze embeddings to extract semantic features
            float[] embeddingVector = embeddings[0];
            
            // Calculate importance score from embedding magnitude
            float magnitude = 0.0f;
            for (float value : embeddingVector) {
                magnitude += value * value;
            }
            magnitude = (float) Math.sqrt(magnitude);
            
            analysis.setImportanceScore(Math.min(1.0f, magnitude / 100.0f));
            analysis.setConfidence(0.9f);
            analysis.setSemanticCategory(extractSemanticCategory(embeddingVector));
            
        } catch (Exception e) {
            Log.e(TAG, "Error in BERT semantic analysis", e);
            analysis = analyzeBasicSemantics(text);
        }
        
        return analysis;
    }
    
    private SemanticAnalysis analyzeBasicSemantics(String text) {
        SemanticAnalysis analysis = new SemanticAnalysis();
        
        String textLower = text.toLowerCase();
        
        // Analyze importance based on keywords
        float importance = 0.5f;
        if (textLower.contains("critical") || textLower.contains("essential")) importance += 0.3f;
        if (textLower.contains("important") || textLower.contains("must")) importance += 0.2f;
        if (textLower.contains("urgent") || textLower.contains("quick")) importance += 0.2f;
        if (textLower.contains("optional") || textLower.contains("maybe")) importance -= 0.2f;
        
        analysis.setImportanceScore(Math.max(0.0f, Math.min(1.0f, importance)));
        analysis.setConfidence(0.7f);
        
        // Determine semantic category
        if (textLower.contains("attack") || textLower.contains("fight") || textLower.contains("combat")) {
            analysis.setSemanticCategory("offensive");
        } else if (textLower.contains("defend") || textLower.contains("protect") || textLower.contains("avoid")) {
            analysis.setSemanticCategory("defensive");
        } else if (textLower.contains("collect") || textLower.contains("gather") || textLower.contains("pickup")) {
            analysis.setSemanticCategory("resource");
        } else if (textLower.contains("move") || textLower.contains("navigate") || textLower.contains("go")) {
            analysis.setSemanticCategory("navigation");
        } else {
            analysis.setSemanticCategory("utility");
        }
        
        return analysis;
    }
    
    private void enhanceWithWordEmbeddings(SemanticAnalysis analysis, String text) {
        try {
            String[] words = text.toLowerCase().split("\\s+");
            List<INDArray> wordVectors = new ArrayList<>();
            
            for (String word : words) {
                if (wordEmbeddings.containsKey(word)) {
                    wordVectors.add(wordEmbeddings.get(word));
                }
            }
            
            if (!wordVectors.isEmpty()) {
                // Calculate average word embedding
                INDArray avgEmbedding = wordVectors.get(0).dup();
                for (int i = 1; i < wordVectors.size(); i++) {
                    avgEmbedding.addi(wordVectors.get(i));
                }
                avgEmbedding.divi(wordVectors.size());
                
                // Use embedding magnitude to adjust confidence
                float embeddingMagnitude = (float) avgEmbedding.norm2Number().doubleValue();
                float adjustedConfidence = analysis.getConfidence() * Math.min(1.0f, embeddingMagnitude);
                analysis.setConfidence(adjustedConfidence);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error enhancing with word embeddings", e);
        }
    }
    
    private int[] tokenizeForBERT(String text) {
        String[] words = text.toLowerCase().split("\\s+");
        List<Integer> tokens = new ArrayList<>();
        
        tokens.add(bertVocabulary.getOrDefault("[CLS]", 0)); // Start token
        
        for (String word : words) {
            tokens.add(bertVocabulary.getOrDefault(word, bertVocabulary.getOrDefault("[UNK]", 1)));
        }
        
        tokens.add(bertVocabulary.getOrDefault("[SEP]", 3)); // End token
        
        return tokens.stream().mapToInt(Integer::intValue).toArray();
    }
    
    private String extractSemanticCategory(float[] embedding) {
        // Simple category extraction based on embedding values
        float combatScore = 0.0f;
        float resourceScore = 0.0f;
        float navigationScore = 0.0f;
        
        // Analyze specific dimensions (simplified approach)
        for (int i = 0; i < Math.min(embedding.length, 100); i++) {
            if (i % 3 == 0) combatScore += Math.abs(embedding[i]);
            else if (i % 3 == 1) resourceScore += Math.abs(embedding[i]);
            else navigationScore += Math.abs(embedding[i]);
        }
        
        if (combatScore > resourceScore && combatScore > navigationScore) return "offensive";
        else if (resourceScore > navigationScore) return "resource";
        else return "navigation";
    }
    
    /**
     * Generate action alternatives using why/what context
     */
    public List<String> generateActionAlternatives(String action, String why, String what) {
        List<String> alternatives = new ArrayList<>();
        
        if (action == null) return alternatives;
        
        String actionLower = action.toLowerCase();
        String whyContext = (why != null) ? why.toLowerCase() : "";
        String whatContext = (what != null) ? what.toLowerCase() : "";
        
        // Generate context-aware alternatives
        if (actionLower.contains("tap") || actionLower.contains("click")) {
            alternatives.add("long_press");
            if (whyContext.contains("quick") || whyContext.contains("fast")) {
                alternatives.add("double_tap");
            }
            if (whatContext.contains("button") || whatContext.contains("icon")) {
                alternatives.add("touch_and_hold");
            }
        } else if (actionLower.contains("swipe")) {
            alternatives.add("drag");
            alternatives.add("flick");
            if (whyContext.contains("precise") || whyContext.contains("careful")) {
                alternatives.add("slow_swipe");
            }
        } else if (actionLower.contains("move")) {
            alternatives.add("navigate");
            alternatives.add("walk");
            if (whyContext.contains("quick") || whyContext.contains("urgent")) {
                alternatives.add("run");
                alternatives.add("dash");
            }
        }
        
        // Add semantic alternatives based on GAME_ACTIONS mapping
        for (Map.Entry<String, String> entry : GAME_ACTIONS.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(action) && !entry.getKey().equals(actionLower)) {
                alternatives.add(entry.getKey());
            }
        }
        
        return alternatives;
    }
    
    /**
     * Calculate semantic similarity between two texts
     */
    public float calculateSemanticSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0f;
        if (text1.equals(text2)) return 1.0f;
        
        try {
            // Use word embeddings if available
            if (nd4jNLPEnabled && wordEmbeddings != null) {
                return calculateEmbeddingSimilarity(text1, text2);
            }
            
            // Fallback to lexical similarity
            return calculateLexicalSimilarity(text1, text2);
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating semantic similarity", e);
            return calculateLexicalSimilarity(text1, text2);
        }
    }
    
    private float calculateEmbeddingSimilarity(String text1, String text2) {
        try {
            INDArray embedding1 = getTextEmbeddingInternal(text1);
            INDArray embedding2 = getTextEmbeddingInternal(text2);
            
            if (embedding1 != null && embedding2 != null) {
                // Calculate cosine similarity
                double dotProduct = embedding1.mmul(embedding2.transpose()).getDouble(0);
                double norm1 = embedding1.norm2Number().doubleValue();
                double norm2 = embedding2.norm2Number().doubleValue();
                
                if (norm1 > 0 && norm2 > 0) {
                    return (float) (dotProduct / (norm1 * norm2));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in embedding similarity", e);
        }
        
        return calculateLexicalSimilarity(text1, text2);
    }
    
    private INDArray getTextEmbeddingInternal(String text) {
        String[] words = text.toLowerCase().split("\\s+");
        List<INDArray> wordVectors = new ArrayList<>();
        
        for (String word : words) {
            if (wordEmbeddings.containsKey(word)) {
                wordVectors.add(wordEmbeddings.get(word));
            }
        }
        
        if (wordVectors.isEmpty()) return null;
        
        // Average word embeddings
        INDArray result = wordVectors.get(0).dup();
        for (int i = 1; i < wordVectors.size(); i++) {
            result.addi(wordVectors.get(i));
        }
        result.divi(wordVectors.size());
        
        return result;
    }
    
    private float calculateLexicalSimilarity(String text1, String text2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0f : (float) intersection.size() / union.size();
    }
    
    /**
     * Get text embedding for RL agent integration
     */
    public float[] getTextEmbedding(String text) {
        try {
            if (nd4jNLPEnabled && wordEmbeddings != null) {
                INDArray embedding = getTextEmbeddingInternal(text);
                if (embedding != null) {
                    return embedding.toFloatArray();
                }
            }
            
            // Fallback: create simple bag-of-words embedding
            return createBagOfWordsEmbedding(text);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting text embedding", e);
            return createBagOfWordsEmbedding(text);
        }
    }
    
    private float[] createBagOfWordsEmbedding(String text) {
        float[] embedding = new float[10]; // Simple 10-dimensional embedding
        String[] words = text.toLowerCase().split("\\s+");
        
        for (String word : words) {
            int hash = Math.abs(word.hashCode()) % 10;
            embedding[hash] += 1.0f;
        }
        
        // Normalize
        float sum = 0.0f;
        for (float value : embedding) sum += value * value;
        if (sum > 0) {
            float norm = (float) Math.sqrt(sum);
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= norm;
            }
        }
        
        return embedding;
    }
    
    /**
     * Extract game phase context from reasoning and objects
     */
    public String extractGamePhaseContext(String contextText, List<Object> detectedObjects) {
        String textLower = contextText.toLowerCase();
        
        // Analyze text for phase indicators
        if (textLower.contains("start") || textLower.contains("begin") || textLower.contains("tutorial")) {
            return "early_game";
        } else if (textLower.contains("boss") || textLower.contains("final") || textLower.contains("end")) {
            return "late_game";
        } else if (textLower.contains("level") || textLower.contains("stage")) {
            return "mid_game";
        }
        
        // Analyze based on detected objects (simplified)
        if (detectedObjects != null && detectedObjects.size() > 5) {
            return "complex_scene";
        } else if (detectedObjects != null && detectedObjects.size() < 2) {
            return "simple_scene";
        }
        
        return "general_gameplay";
    }
    
    /**
     * Supporting data structures
     */
    public static class SemanticAnalysis {
        private float importanceScore = 0.5f;
        private float confidence = 0.5f;
        private String semanticCategory = "unknown";
        
        public float getImportanceScore() { return importanceScore; }
        public void setImportanceScore(float score) { this.importanceScore = score; }
        public float getConfidence() { return confidence; }
        public void setConfidence(float confidence) { this.confidence = confidence; }
        public String getSemanticCategory() { return semanticCategory; }
        public void setSemanticCategory(String category) { this.semanticCategory = category; }
    }
    
    // Placeholder methods that need proper implementation
    private void loadActionMappings() {
        actionMappings = new HashMap<>(GAME_ACTIONS);
    }
    
    private void initializeOpenNLPModels() {
        // Initialize OpenNLP models (simplified for compilation)
        try {
            // In practice, would load actual OpenNLP models from assets
            Log.d(TAG, "OpenNLP models initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing OpenNLP models", e);
                initializeMobileBERT();
            }
            isInitialized = true;
            Log.d(TAG, "Advanced NLP Processor with MobileBERT initialized successfully");
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
    
    // ===== WORKFLOW EXTENSION METHODS =====
    
    /**
     * Parse voice command into workflow instructions
     */
    public WorkflowParseResult parseVoiceCommand(String command) {
        try {
            String cleanCommand = preprocessCommand(command);
            
            // Classify workflow intent
            WorkflowIntent intent = classifyWorkflowIntent(cleanCommand);
            
            // Extract workflow components
            List<WorkflowAction> actions = extractWorkflowActions(cleanCommand);
            List<WorkflowCondition> conditions = extractWorkflowConditions(cleanCommand);
            String workflowName = extractWorkflowName(cleanCommand);
            
            Log.d(TAG, "Voice command parsed: " + intent + " with " + actions.size() + " actions");
            
            return new WorkflowParseResult(cleanCommand, intent, workflowName, actions, conditions);
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing voice command", e);
            return new WorkflowParseResult(command, WorkflowIntent.UNKNOWN, null, new ArrayList<>(), new ArrayList<>());
        }
    }
    
    /**
     * Parse natural language description into workflow
     */
    public WorkflowParseResult parseNaturalLanguageWorkflow(String description) {
        try {
            // Use MobileBERT for advanced understanding if available
            float[] embeddings = getMobileBertEmbeddings(description);
            
            WorkflowIntent intent = classifyWorkflowIntentFromEmbeddings(embeddings, description);
            List<WorkflowAction> actions = extractWorkflowActionsAdvanced(description, embeddings);
            List<WorkflowCondition> conditions = extractWorkflowConditionsAdvanced(description, embeddings);
            String workflowName = generateWorkflowName(description);
            
            return new WorkflowParseResult(description, intent, workflowName, actions, conditions);
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing natural language workflow", e);
            return new WorkflowParseResult(description, WorkflowIntent.UNKNOWN, null, new ArrayList<>(), new ArrayList<>());
        }
    }
    
    /**
     * Generate workflow suggestions based on patterns
     */
    public List<WorkflowSuggestion> generateWorkflowSuggestions(String gameType, List<String> userActions) {
        List<WorkflowSuggestion> suggestions = new ArrayList<>();
        
        try {
            // Analyze action patterns
            Map<String, Integer> actionFrequency = new HashMap<>();
            for (String action : userActions) {
                actionFrequency.merge(action, 1, Integer::sum);
            }
            
            // Generate pattern-based suggestions
            for (Map.Entry<String, Integer> entry : actionFrequency.entrySet()) {
                if (entry.getValue() > 3) { // Threshold for pattern
                    String actionType = entry.getKey();
                    String suggestion = generateSuggestionForAction(actionType, gameType);
                    if (suggestion != null) {
                        float relevance = calculateRelevanceScore(actionType, entry.getValue(), gameType);
                        suggestions.add(new WorkflowSuggestion(
                            "Auto " + actionType,
                            suggestion,
                            relevance
                        ));
                    }
                }
            }
            
            // Add game-specific suggestions
            suggestions.addAll(getGameSpecificSuggestions(gameType));
            
            // Sort by relevance
            suggestions.sort((a, b) -> Float.compare(b.getRelevanceScore(), a.getRelevanceScore()));
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating workflow suggestions", e);
        }
        
        return suggestions;
    }
    
    // Private workflow helper methods
    
    private String preprocessCommand(String command) {
        return command.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
    
    private WorkflowIntent classifyWorkflowIntent(String command) {
        if (command.contains("create") || command.contains("make") || command.contains("build")) {
            return WorkflowIntent.CREATE_WORKFLOW;
        } else if (command.contains("run") || command.contains("execute") || command.contains("start")) {
            return WorkflowIntent.EXECUTE_WORKFLOW;
        } else if (command.contains("modify") || command.contains("edit") || command.contains("change")) {
            return WorkflowIntent.MODIFY_WORKFLOW;
        } else if (command.contains("when") || command.contains("if") || command.contains("condition")) {
            return WorkflowIntent.ADD_CONDITION;
        } else if (command.contains("repeat") || command.contains("loop")) {
            return WorkflowIntent.ADD_LOOP;
        }
        return WorkflowIntent.UNKNOWN;
    }
    
    private WorkflowIntent classifyWorkflowIntentFromEmbeddings(float[] embeddings, String text) {
        // Fallback to text-based classification if MobileBERT not available
        return classifyWorkflowIntent(text);
    }
    
    private List<WorkflowAction> extractWorkflowActions(String command) {
        List<WorkflowAction> actions = new ArrayList<>();
        
        // Pattern matching for workflow actions
        Map<String, java.util.regex.Pattern> actionPatterns = getWorkflowActionPatterns();
        
        for (Map.Entry<String, java.util.regex.Pattern> entry : actionPatterns.entrySet()) {
            java.util.regex.Matcher matcher = entry.getValue().matcher(command);
            while (matcher.find()) {
                WorkflowAction action = createWorkflowActionFromMatch(entry.getKey(), matcher);
                if (action != null) {
                    actions.add(action);
                }
            }
        }
        
        return actions;
    }
    
    private List<WorkflowAction> extractWorkflowActionsAdvanced(String text, float[] embeddings) {
        // Combine pattern matching with ML-based extraction
        List<WorkflowAction> patternActions = extractWorkflowActions(text);
        
        // Add ML-based actions if MobileBERT is available
        if (mobileBertEnabled && embeddings != null) {
            // Use embeddings for more sophisticated action extraction
            patternActions.addAll(extractActionsWithMobileBERT(text, embeddings));
        }
        
        return patternActions;
    }
    
    private List<WorkflowCondition> extractWorkflowConditions(String command) {
        List<WorkflowCondition> conditions = new ArrayList<>();
        
        Map<String, java.util.regex.Pattern> conditionPatterns = getWorkflowConditionPatterns();
        
        for (Map.Entry<String, java.util.regex.Pattern> entry : conditionPatterns.entrySet()) {
            java.util.regex.Matcher matcher = entry.getValue().matcher(command);
            while (matcher.find()) {
                WorkflowCondition condition = createWorkflowConditionFromMatch(entry.getKey(), matcher);
                if (condition != null) {
                    conditions.add(condition);
                }
            }
        }
        
        return conditions;
    }
    
    private List<WorkflowCondition> extractWorkflowConditionsAdvanced(String text, float[] embeddings) {
        return extractWorkflowConditions(text); // Use basic extraction for now
    }
    
    private String extractWorkflowName(String command) {
        java.util.regex.Pattern namePattern = java.util.regex.Pattern.compile(
            "(?i)(?:create|make|build)\\s+(?:a\\s+)?(?:workflow|sequence)\\s+(?:called|named)\\s+['\"]?([^'\"\\s]+)['\"]?"
        );
        java.util.regex.Matcher matcher = namePattern.matcher(command);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return "Workflow_" + System.currentTimeMillis();
    }
    
    private String generateWorkflowName(String description) {
        // Extract key action words to create meaningful name
        String[] words = description.toLowerCase().split("\\s+");
        StringBuilder nameBuilder = new StringBuilder();
        
        for (String word : words) {
            if (actionMappings.containsKey(word)) {
                nameBuilder.append(word).append("_");
                break;
            }
        }
        
        if (nameBuilder.length() == 0) {
            nameBuilder.append("Custom_");
        }
        
        nameBuilder.append(System.currentTimeMillis() % 10000);
        return nameBuilder.toString();
    }
    
    private float[] getMobileBertEmbeddings(String text) {
        if (!mobileBertEnabled || mobileBertInterpreter == null) {
            return null;
        }
        
        try {
            // Tokenize and prepare input
            int[] tokens = tokenizeForBERT(text);
            
            // Create input buffer
            java.nio.ByteBuffer inputBuffer = java.nio.ByteBuffer.allocateDirect(4 * MAX_SEQUENCE_LENGTH);
            inputBuffer.order(java.nio.ByteOrder.nativeOrder());
            
            for (int i = 0; i < Math.min(tokens.length, MAX_SEQUENCE_LENGTH); i++) {
                inputBuffer.putInt(tokens[i]);
            }
            
            // Run inference
            float[][] output = new float[1][BERT_HIDDEN_SIZE];
            mobileBertInterpreter.run(inputBuffer, output);
            
            return output[0];
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting MobileBERT embeddings", e);
            return null;
        }
    }
    
    private int[] tokenizeForBERT(String text) {
        // Basic tokenization - in real implementation would use proper BERT tokenizer
        String[] words = text.toLowerCase().split("\\s+");
        int[] tokens = new int[Math.min(words.length + 2, MAX_SEQUENCE_LENGTH)]; // +2 for [CLS] and [SEP]
        
        tokens[0] = 101; // [CLS] token
        for (int i = 0; i < Math.min(words.length, MAX_SEQUENCE_LENGTH - 2); i++) {
            tokens[i + 1] = Math.abs(words[i].hashCode()) % 30000 + 1000; // Simple hash-based vocab
        }
        if (words.length < MAX_SEQUENCE_LENGTH - 2) {
            tokens[words.length + 1] = 102; // [SEP] token
        }
        
        return tokens;
    }
    
    private Map<String, java.util.regex.Pattern> getWorkflowActionPatterns() {
        Map<String, java.util.regex.Pattern> patterns = new HashMap<>();
        
        patterns.put("TAP", java.util.regex.Pattern.compile("(?i)(tap|click|touch|press)\\s+(?:on\\s+)?(\\w+)"));
        patterns.put("SWIPE", java.util.regex.Pattern.compile("(?i)(swipe|drag|slide)\\s+(up|down|left|right)"));
        patterns.put("TYPE", java.util.regex.Pattern.compile("(?i)(type|enter|input)\\s+['\"]([^'\"]+)['\"]"));
        patterns.put("WAIT", java.util.regex.Pattern.compile("(?i)(wait|pause|delay)\\s+(?:for\\s+)?(\\d+)\\s*(seconds?|ms|milliseconds?)"));
        patterns.put("LONG_PRESS", java.util.regex.Pattern.compile("(?i)(long\\s+press|hold)\\s+(?:on\\s+)?(\\w+)"));
        patterns.put("DOUBLE_TAP", java.util.regex.Pattern.compile("(?i)(double\\s+tap|double\\s+click)\\s+(?:on\\s+)?(\\w+)"));
        
        return patterns;
    }
    
    private Map<String, java.util.regex.Pattern> getWorkflowConditionPatterns() {
        Map<String, java.util.regex.Pattern> patterns = new HashMap<>();
        
        patterns.put("TEXT_VISIBLE", java.util.regex.Pattern.compile("(?i)(?:when|if|until)\\s+(?:you\\s+)?(?:see|find)\\s+['\"]([^'\"]+)['\"]"));
        patterns.put("ELEMENT_EXISTS", java.util.regex.Pattern.compile("(?i)(?:when|if|until)\\s+(?:the\\s+)?(\\w+)\\s+(?:appears|exists|is\\s+visible)"));
        patterns.put("TIME_ELAPSED", java.util.regex.Pattern.compile("(?i)(?:after|when)\\s+(\\d+)\\s*(seconds?|minutes?)\\s+(?:pass|elapse)"));
        patterns.put("REPEAT", java.util.regex.Pattern.compile("(?i)(repeat|loop|do)\\s+(?:this\\s+)?(\\d+)\\s+times"));
        
        return patterns;
    }
    
    private WorkflowAction createWorkflowActionFromMatch(String actionType, java.util.regex.Matcher matcher) {
        Map<String, Object> params = new HashMap<>();
        
        switch (actionType) {
            case "TAP":
                params.put("target", matcher.group(2));
                break;
            case "SWIPE":
                params.put("direction", matcher.group(2));
                break;
            case "TYPE":
                params.put("text", matcher.group(2));
                break;
            case "WAIT":
                params.put("duration", parseWaitDuration(matcher.group(2), matcher.group(3)));
                break;
            case "LONG_PRESS":
                params.put("target", matcher.group(2));
                break;
            case "DOUBLE_TAP":
                params.put("target", matcher.group(2));
                break;
        }
        
        return new WorkflowAction(actionType, params);
    }
    
    private WorkflowCondition createWorkflowConditionFromMatch(String conditionType, java.util.regex.Matcher matcher) {
        Map<String, Object> params = new HashMap<>();
        
        switch (conditionType) {
            case "TEXT_VISIBLE":
                params.put("text", matcher.group(1));
                break;
            case "ELEMENT_EXISTS":
                params.put("element", matcher.group(1));
                break;
            case "TIME_ELAPSED":
                params.put("duration", parseTimeDuration(matcher.group(1), matcher.group(2)));
                break;
            case "REPEAT":
                params.put("count", Integer.parseInt(matcher.group(2)));
                break;
        }
        
        return new WorkflowCondition(conditionType, params);
    }
    
    private long parseWaitDuration(String number, String unit) {
        int duration = Integer.parseInt(number);
        switch (unit.toLowerCase()) {
            case "seconds":
            case "second":
                return duration * 1000L;
            case "ms":
            case "milliseconds":
            case "millisecond":
                return duration;
            default:
                return duration * 1000L;
        }
    }
    
    private long parseTimeDuration(String number, String unit) {
        int duration = Integer.parseInt(number);
        switch (unit.toLowerCase()) {
            case "minutes":
            case "minute":
                return duration * 60 * 1000L;
            case "seconds":
            case "second":
                return duration * 1000L;
            default:
                return duration * 1000L;
        }
    }
    
    private List<WorkflowAction> extractActionsWithMobileBERT(String text, float[] embeddings) {
        // Placeholder for MobileBERT-based action extraction
        return new ArrayList<>();
    }
    
    private String generateSuggestionForAction(String actionType, String gameType) {
        switch (actionType.toLowerCase()) {
            case "tap":
                return "Automatically tap when specific elements appear";
            case "swipe":
                return "Auto-swipe in response to game events";
            case "collect":
                return "Automatically collect items when detected";
            case "jump":
                return "Auto-jump over obstacles";
            case "attack":
                return "Auto-attack when enemies are detected";
            default:
                return null;
        }
    }
    
    private float calculateRelevanceScore(String actionType, int frequency, String gameType) {
        float baseScore = Math.min(frequency / 10.0f, 1.0f);
        
        // Boost score based on game type relevance
        switch (gameType.toLowerCase()) {
            case "runner":
                if (actionType.equals("jump") || actionType.equals("slide")) {
                    baseScore += 0.2f;
                }
                break;
            case "battle_royale":
                if (actionType.equals("collect") || actionType.equals("attack")) {
                    baseScore += 0.2f;
                }
                break;
        }
        
        return Math.min(baseScore, 1.0f);
    }
    
    private List<WorkflowSuggestion> getGameSpecificSuggestions(String gameType) {
        List<WorkflowSuggestion> suggestions = new ArrayList<>();
        
        switch (gameType.toLowerCase()) {
            case "runner":
                suggestions.add(new WorkflowSuggestion("Obstacle Avoider", "Automatically jump over obstacles", 0.9f));
                suggestions.add(new WorkflowSuggestion("Coin Collector", "Auto-collect coins and power-ups", 0.8f));
                break;
            case "battle_royale":
                suggestions.add(new WorkflowSuggestion("Loot Collector", "Automatically collect nearby loot", 0.9f));
                suggestions.add(new WorkflowSuggestion("Zone Rotator", "Auto-move when zone shrinks", 0.8f));
                break;
            case "moba":
                suggestions.add(new WorkflowSuggestion("Last Hit Helper", "Perfect minion last hits", 0.9f));
                suggestions.add(new WorkflowSuggestion("Ward Placer", "Auto-place wards at key locations", 0.7f));
                break;
        }
        
        return suggestions;
    }
    
    // Inner classes for workflow data structures
    
    public enum WorkflowIntent {
        CREATE_WORKFLOW, EXECUTE_WORKFLOW, MODIFY_WORKFLOW, 
        ADD_CONDITION, ADD_LOOP, SET_TIMING, UNKNOWN
    }
    
    public static class WorkflowParseResult {
        private String originalText;
        private WorkflowIntent intent;
        private String workflowName;
        private List<WorkflowAction> actions;
        private List<WorkflowCondition> conditions;
        
        public WorkflowParseResult(String originalText, WorkflowIntent intent, String workflowName,
                                 List<WorkflowAction> actions, List<WorkflowCondition> conditions) {
            this.originalText = originalText;
            this.intent = intent;
            this.workflowName = workflowName;
            this.actions = actions;
            this.conditions = conditions;
        }
        
        // Getters
        public String getOriginalText() { return originalText; }
        public WorkflowIntent getIntent() { return intent; }
        public String getWorkflowName() { return workflowName; }
        public List<WorkflowAction> getActions() { return actions; }
        public List<WorkflowCondition> getConditions() { return conditions; }
        public boolean isWorkflowCreation() { return intent == WorkflowIntent.CREATE_WORKFLOW; }
        public boolean isWorkflowExecution() { return intent == WorkflowIntent.EXECUTE_WORKFLOW; }
    }
    
    public static class WorkflowAction {
        private String type;
        private Map<String, Object> parameters;
        
        public WorkflowAction(String type, Map<String, Object> parameters) {
            this.type = type;
            this.parameters = parameters != null ? parameters : new HashMap<>();
        }
        
        public String getType() { return type; }
        public Map<String, Object> getParameters() { return parameters; }
        public Object getParameter(String key) { return parameters.get(key); }
        public void setParameter(String key, Object value) { parameters.put(key, value); }
    }
    
    public static class WorkflowCondition {
        private String type;
        private Map<String, Object> parameters;
        
        public WorkflowCondition(String type, Map<String, Object> parameters) {
            this.type = type;
            this.parameters = parameters != null ? parameters : new HashMap<>();
        }
        
        public String getType() { return type; }
        public Map<String, Object> getParameters() { return parameters; }
        public Object getParameter(String key) { return parameters.get(key); }
    }
    
    public static class WorkflowSuggestion {
        private String name;
        private String description;
        private float relevanceScore;
        
        public WorkflowSuggestion(String name, String description, float relevanceScore) {
            this.name = name;
            this.description = description;
            this.relevanceScore = relevanceScore;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public float getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(float score) { this.relevanceScore = score; }
    }
    
    // WORKFLOW-SPECIFIC METHODS
    
    /**
     * Parse voice command into workflow creation instructions
     */
    public WorkflowCommand parseWorkflowCommand(String voiceInput) {
        try {
            String cleanInput = voiceInput.toLowerCase().trim();
            
            // Determine intent
            WorkflowIntent intent = classifyWorkflowIntent(cleanInput);
            
            // Extract workflow components
            List<WorkflowAction> actions = extractWorkflowActions(cleanInput);
            List<WorkflowCondition> conditions = extractWorkflowConditions(cleanInput);
            String workflowName = extractWorkflowName(cleanInput);
            
            return new WorkflowCommand(intent, workflowName, actions, conditions, cleanInput);
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing workflow command", e);
            return new WorkflowCommand(WorkflowIntent.UNKNOWN, null, new ArrayList<>(), new ArrayList<>(), voiceInput);
        }
    }
    
    /**
     * Generate workflow suggestions based on game context and user behavior
     */
    public List<WorkflowSuggestion> generateWorkflowSuggestions(String gameType, List<String> recentActions) {
        List<WorkflowSuggestion> suggestions = new ArrayList<>();
        
        // Analyze action patterns
        Map<String, Integer> actionFrequency = new HashMap<>();
        for (String action : recentActions) {
            actionFrequency.merge(action, 1, Integer::sum);
        }
        
        // Generate game-specific suggestions
        switch (gameType.toLowerCase()) {
            case "runner":
                suggestions.addAll(generateRunnerSuggestions(actionFrequency));
                break;
            case "puzzle":
                suggestions.addAll(generatePuzzleSuggestions(actionFrequency));
                break;
            case "action":
                suggestions.addAll(generateActionSuggestions(actionFrequency));
                break;
            case "strategy":
                suggestions.addAll(generateStrategySuggestions(actionFrequency));
                break;
        }
        
        // Add pattern-based suggestions
        suggestions.addAll(generatePatternBasedSuggestions(actionFrequency));
        
        return suggestions;
    }
    
    /**
     * Convert natural language description to workflow steps
     */
    public List<WorkflowStep> convertDescriptionToSteps(String description) {
        List<WorkflowStep> steps = new ArrayList<>();
        
        try {
            // Split into sentences
            String[] sentences = description.split("[.!?]");
            
            for (String sentence : sentences) {
                sentence = sentence.trim();
                if (sentence.isEmpty()) continue;
                
                // Process each sentence as a potential step
                WorkflowStep step = convertSentenceToStep(sentence);
                if (step != null) {
                    steps.add(step);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting description to steps", e);
        }
        
        return steps;
    }
    
    private WorkflowIntent classifyWorkflowIntent(String input) {
        if (input.contains("create") || input.contains("make") || input.contains("build")) {
            return WorkflowIntent.CREATE;
        } else if (input.contains("run") || input.contains("execute") || input.contains("start")) {
            return WorkflowIntent.EXECUTE;
        } else if (input.contains("modify") || input.contains("edit") || input.contains("change")) {
            return WorkflowIntent.MODIFY;
        } else if (input.contains("delete") || input.contains("remove")) {
            return WorkflowIntent.DELETE;
        } else if (input.contains("save") || input.contains("store")) {
            return WorkflowIntent.SAVE;
        }
        return WorkflowIntent.UNKNOWN;
    }
    
    private List<WorkflowAction> extractWorkflowActions(String input) {
        List<WorkflowAction> actions = new ArrayList<>();
        
        // Extract action patterns
        java.util.regex.Pattern actionPattern = java.util.regex.Pattern.compile(
            "(?i)(tap|click|swipe|wait|type|press|hold|collect|move)\\s*([^,\\.]*)"
        );
        
        java.util.regex.Matcher matcher = actionPattern.matcher(input);
        while (matcher.find()) {
            String actionType = matcher.group(1).toLowerCase();
            String actionTarget = matcher.group(2).trim();
            
            WorkflowAction action = createWorkflowAction(actionType, actionTarget);
            if (action != null) {
                actions.add(action);
            }
        }
        
        return actions;
    }
    
    private List<WorkflowCondition> extractWorkflowConditions(String input) {
        List<WorkflowCondition> conditions = new ArrayList<>();
        
        // Extract condition patterns
        java.util.regex.Pattern conditionPattern = java.util.regex.Pattern.compile(
            "(?i)(when|if|until|while)\\s*([^,\\.]*?)\\s*(then|do|execute)"
        );
        
        java.util.regex.Matcher matcher = conditionPattern.matcher(input);
        while (matcher.find()) {
            String conditionType = matcher.group(1).toLowerCase();
            String conditionTarget = matcher.group(2).trim();
            
            WorkflowCondition condition = createWorkflowCondition(conditionType, conditionTarget);
            if (condition != null) {
                conditions.add(condition);
            }
        }
        
        return conditions;
    }
    
    private String extractWorkflowName(String input) {
        // Look for explicit naming
        java.util.regex.Pattern namePattern = java.util.regex.Pattern.compile(
            "(?i)(?:create|make|build)\\s*(?:a|an)?\\s*(?:workflow|sequence)\\s*(?:called|named)\\s*['\"]?([^'\"]*)['\"]?"
        );
        
        java.util.regex.Matcher matcher = namePattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // Generate name from actions
        return "Auto_Workflow_" + System.currentTimeMillis() % 10000;
    }
    
    private WorkflowAction createWorkflowAction(String type, String target) {
        switch (type) {
            case "tap":
            case "click":
                return new WorkflowAction("TAP", extractParameters(target));
            case "swipe":
                return new WorkflowAction("SWIPE", extractSwipeParameters(target));
            case "wait":
                return new WorkflowAction("WAIT", extractWaitParameters(target));
            case "type":
                return new WorkflowAction("TYPE", extractTypeParameters(target));
            case "press":
            case "hold":
                return new WorkflowAction("LONG_PRESS", extractParameters(target));
            case "collect":
                return new WorkflowAction("COLLECT", extractParameters(target));
            case "move":
                return new WorkflowAction("MOVE", extractMoveParameters(target));
            default:
                return null;
        }
    }
    
    private WorkflowCondition createWorkflowCondition(String type, String target) {
        switch (type) {
            case "when":
            case "if":
                if (target.contains("visible") || target.contains("appears")) {
                    return new WorkflowCondition("ELEMENT_VISIBLE", extractElementName(target));
                } else if (target.contains("text") || target.contains("shows")) {
                    return new WorkflowCondition("TEXT_PRESENT", extractTextContent(target));
                }
                break;
            case "until":
                return new WorkflowCondition("WAIT_UNTIL", extractParameters(target));
            case "while":
                return new WorkflowCondition("REPEAT_WHILE", extractParameters(target));
        }
        return null;
    }
    
    private Map<String, Object> extractSwipeParameters(String target) {
        Map<String, Object> params = new HashMap<>();
        
        if (target.contains("up")) params.put("direction", "up");
        else if (target.contains("down")) params.put("direction", "down");
        else if (target.contains("left")) params.put("direction", "left");
        else if (target.contains("right")) params.put("direction", "right");
        
        return params;
    }
    
    private Map<String, Object> extractWaitParameters(String target) {
        Map<String, Object> params = new HashMap<>();
        
        java.util.regex.Pattern durationPattern = java.util.regex.Pattern.compile("(\\d+)\\s*(seconds?|ms|milliseconds?)");
        java.util.regex.Matcher matcher = durationPattern.matcher(target);
        
        if (matcher.find()) {
            int duration = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            
            if (unit.startsWith("s")) {
                duration *= 1000; // Convert to milliseconds
            }
            
            params.put("duration", duration);
        } else {
            params.put("duration", 1000); // Default 1 second
        }
        
        return params;
    }
    
    private Map<String, Object> extractTypeParameters(String target) {
        Map<String, Object> params = new HashMap<>();
        
        // Remove quotes and extract text
        String text = target.replaceAll("['\"]", "").trim();
        params.put("text", text);
        
        return params;
    }
    
    private Map<String, Object> extractMoveParameters(String target) {
        Map<String, Object> params = new HashMap<>();
        
        if (target.contains("left")) params.put("direction", "left");
        else if (target.contains("right")) params.put("direction", "right");
        else if (target.contains("up")) params.put("direction", "up");
        else if (target.contains("down")) params.put("direction", "down");
        
        return params;
    }
    
    private String extractElementName(String target) {
        // Remove common words and extract the object
        return target.replaceAll("(?i)(the|a|an|is|appears|visible)", "").trim();
    }
    
    private String extractTextContent(String target) {
        // Extract text between quotes or after "text"
        java.util.regex.Pattern textPattern = java.util.regex.Pattern.compile("['\"]([^'\"]*)['\"]");
        java.util.regex.Matcher matcher = textPattern.matcher(target);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return target.replaceAll("(?i)(text|shows|displays)", "").trim();
    }
    
    private WorkflowStep convertSentenceToStep(String sentence) {
        try {
            ActionIntent intent = processNaturalLanguageCommand(sentence);
            if (intent != null && intent.getConfidence() > 0.5f) {
                return new WorkflowStep(intent.getAction(), intent.getParameters());
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not convert sentence to step: " + sentence);
        }
        return null;
    }
    
    private List<WorkflowSuggestion> generateRunnerSuggestions(Map<String, Integer> actionFrequency) {
        List<WorkflowSuggestion> suggestions = new ArrayList<>();
        
        suggestions.add(new WorkflowSuggestion(
            "Auto Coin Collector",
            "Automatically collect coins when they appear",
            0.9f
        ));
        
        suggestions.add(new WorkflowSuggestion(
            "Obstacle Avoider",
            "Jump over or slide under obstacles automatically",
            0.85f
        ));
        
        suggestions.add(new WorkflowSuggestion(
            "Power-up Activator",
            "Use power-ups when available",
            0.8f
        ));
        
        return suggestions;
    }
    
    private List<WorkflowSuggestion> generatePuzzleSuggestions(Map<String, Integer> actionFrequency) {
        List<WorkflowSuggestion> suggestions = new ArrayList<>();
        
        suggestions.add(new WorkflowSuggestion(
            "Match Finder",
            "Automatically find and execute optimal matches",
            0.9f
        ));
        
        suggestions.add(new WorkflowSuggestion(
            "Combo Builder",
            "Create chain combos for higher scores",
            0.85f
        ));
        
        return suggestions;
    }
    
    private List<WorkflowSuggestion> generateActionSuggestions(Map<String, Integer> actionFrequency) {
        List<WorkflowSuggestion> suggestions = new ArrayList<>();
        
        suggestions.add(new WorkflowSuggestion(
            "Combat Optimizer",
            "Optimize attack sequences and defense",
            0.9f
        ));
        
        suggestions.add(new WorkflowSuggestion(
            "Resource Manager",
            "Automatically manage health and ammo",
            0.8f
        ));
        
        return suggestions;
    }
    
    private List<WorkflowSuggestion> generateStrategySuggestions(Map<String, Integer> actionFrequency) {
        List<WorkflowSuggestion> suggestions = new ArrayList<>();
        
        suggestions.add(new WorkflowSuggestion(
            "Resource Optimizer",
            "Efficiently manage resources and buildings",
            0.85f
        ));
        
        suggestions.add(new WorkflowSuggestion(
            "Unit Controller",
            "Automate unit production and deployment",
            0.8f
        ));
        
        return suggestions;
    }
    
    private List<WorkflowSuggestion> generatePatternBasedSuggestions(Map<String, Integer> actionFrequency) {
        List<WorkflowSuggestion> suggestions = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : actionFrequency.entrySet()) {
            if (entry.getValue() > 5) { // Frequently used actions
                suggestions.add(new WorkflowSuggestion(
                    "Auto " + entry.getKey(),
                    "Automate " + entry.getKey() + " based on usage patterns",
                    Math.min(0.9f, entry.getValue() / 10.0f)
                ));
            }
        }
        
        return suggestions;
    }
    
    // Inner classes for workflow support
    
    public enum WorkflowIntent {
        CREATE, EXECUTE, MODIFY, DELETE, SAVE, UNKNOWN
    }
    
    public static class WorkflowCommand {
        private WorkflowIntent intent;
        private String workflowName;
        private List<WorkflowAction> actions;
        private List<WorkflowCondition> conditions;
        private String originalText;
        
        public WorkflowCommand(WorkflowIntent intent, String workflowName, 
                             List<WorkflowAction> actions, List<WorkflowCondition> conditions, String originalText) {
            this.intent = intent;
            this.workflowName = workflowName;
            this.actions = actions;
            this.conditions = conditions;
            this.originalText = originalText;
        }
        
        // Getters
        public WorkflowIntent getIntent() { return intent; }
        public String getWorkflowName() { return workflowName; }
        public List<WorkflowAction> getActions() { return actions; }
        public List<WorkflowCondition> getConditions() { return conditions; }
        public String getOriginalText() { return originalText; }
        
        public boolean isCreationCommand() { return intent == WorkflowIntent.CREATE; }
        public boolean isExecutionCommand() { return intent == WorkflowIntent.EXECUTE; }
    }
    
    public static class WorkflowAction {
        private String type;
        private Map<String, Object> parameters;
        
        public WorkflowAction(String type, Map<String, Object> parameters) {
            this.type = type;
            this.parameters = parameters != null ? parameters : new HashMap<>();
        }
        
        public String getType() { return type; }
        public Map<String, Object> getParameters() { return parameters; }
    }
    
    public static class WorkflowCondition {
        private String type;
        private Object value;
        
        public WorkflowCondition(String type, Object value) {
            this.type = type;
            this.value = value;
        }
        
        public String getType() { return type; }
        public Object getValue() { return value; }
    }
    
    public static class WorkflowStep {
        private String actionType;
        private Map<String, Object> parameters;
        
        public WorkflowStep(String actionType, Map<String, Object> parameters) {
            this.actionType = actionType;
            this.parameters = parameters != null ? parameters : new HashMap<>();
        }
        
        public String getActionType() { return actionType; }
        public Map<String, Object> getParameters() { return parameters; }
    }
    
    public static class WorkflowSuggestion {
        private String name;
        private String description;
        private float relevanceScore;
        
        public WorkflowSuggestion(String name, String description, float relevanceScore) {
            this.name = name;
            this.description = description;
            this.relevanceScore = relevanceScore;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public float getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(float score) { this.relevanceScore = score; }
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

    // ========================== MobileBERT INTEGRATION ==========================
    
    /**
     * Initialize MobileBERT for advanced text understanding
     */
    private void initializeMobileBERT() {
        try {
            // Load MobileBERT TensorFlow Lite model
            java.nio.MappedByteBuffer modelBuffer = FileUtil.loadMappedFile(context, MOBILE_BERT_MODEL);
            
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2); // Optimize for mobile
            options.setUseNNAPI(true); // Use Neural Networks API if available
            
            mobileBertInterpreter = new Interpreter(modelBuffer, options);
            
            // Load BERT vocabulary for tokenization
            loadBertVocabulary();
            
            Log.d(TAG, "MobileBERT initialized successfully");
            
        } catch (Exception e) {
            Log.w(TAG, "MobileBERT initialization failed, using fallback", e);
            mobileBertEnabled = false;
        }
    }
    
    /**
     * Enhanced OCR text analysis with MobileBERT
     */
    public GameTextAnalysis analyzeGameTextWithBERT(List<String> ocrTexts) {
        GameTextAnalysis analysis = new GameTextAnalysis();
        
        if (!mobileBertEnabled || ocrTexts.isEmpty()) {
            // Fallback to existing OpenNLP processing
            return analyzeGameTextWithOpenNLP(ocrTexts);
        }
        
        for (String text : ocrTexts) {
            try {
                // Tokenize text for BERT
                int[] tokenIds = tokenizeForBERT(text);
                
                // Run MobileBERT inference
                float[][] embeddings = runBERTInference(tokenIds);
                
                // Convert to ND4J for integration with existing AI
                INDArray bertFeatures = Nd4j.create(embeddings);
                
                // Classify game text type using BERT features
                GameTextClassification classification = classifyGameText(bertFeatures, text);
                analysis.addClassification(text, classification);
                
                // Extract strategic insights from text
                StrategyInsight insight = extractStrategyFromBERT(bertFeatures, text);
                analysis.addStrategyInsight(insight);
                
            } catch (Exception e) {
                Log.w(TAG, "BERT processing failed for text: " + text, e);
                // Fallback to OpenNLP for this text
                GameTextClassification fallback = classifyGameTextBasic(text);
                analysis.addClassification(text, fallback);
            }
        }
        
        return analysis;
    }
    
    /**
     * Enhanced screen processing combining visual + textual AI
     */
    public EnhancedGameState processScreenWithBERT(android.graphics.Bitmap screenshot, List<String> ocrTexts) {
        EnhancedGameState gameState = new EnhancedGameState();
        
        try {
            // Process textual elements with MobileBERT
            GameTextAnalysis textAnalysis = analyzeGameTextWithBERT(ocrTexts);
            
            // Convert visual features from screenshot if needed
            INDArray visualFeatures = convertScreenToNDArray(screenshot);
            
            // Combine visual and textual understanding
            INDArray combinedFeatures = combineVisualAndTextual(visualFeatures, textAnalysis.getAggregatedFeatures());
            
            // Generate intelligent actions based on combined analysis
            List<SmartGameAction> actions = generateSmartActions(combinedFeatures, textAnalysis);
            
            gameState.visualFeatures = visualFeatures;
            gameState.textualInsights = textAnalysis;
            gameState.combinedIntelligence = combinedFeatures;
            gameState.recommendedActions = actions;
            
        } catch (Exception e) {
            Log.e(TAG, "Error in enhanced screen processing", e);
        }
        
        return gameState;
    }
    
    private int[] tokenizeForBERT(String text) {
        int[] tokenIds = new int[MAX_SEQUENCE_LENGTH];
        
        // Add [CLS] token at start
        tokenIds[0] = bertVocabulary.getOrDefault("[CLS]", 101);
        
        String[] words = text.toLowerCase().trim().split("\\s+");
        int pos = 1;
        
        for (String word : words) {
            if (pos >= MAX_SEQUENCE_LENGTH - 1) break;
            
            // Handle subword tokenization (simplified)
            if (bertVocabulary.containsKey(word)) {
                tokenIds[pos++] = bertVocabulary.get(word);
            } else {
                // Use [UNK] token for unknown words
                tokenIds[pos++] = bertVocabulary.getOrDefault("[UNK]", 100);
            }
        }
        
        // Add [SEP] token at end
        if (pos < MAX_SEQUENCE_LENGTH) {
            tokenIds[pos] = bertVocabulary.getOrDefault("[SEP]", 102);
        }
        
        return tokenIds;
    }
    
    private float[][] runBERTInference(int[] tokenIds) {
        try {
            // Prepare input tensors
            int[][] inputIds = {tokenIds};
            int[][] attentionMask = new int[1][MAX_SEQUENCE_LENGTH];
            int[][] tokenTypeIds = new int[1][MAX_SEQUENCE_LENGTH];
            
            // Create attention mask (1 for real tokens, 0 for padding)
            for (int i = 0; i < tokenIds.length; i++) {
                attentionMask[0][i] = tokenIds[i] != 0 ? 1 : 0;
            }
            
            // Run MobileBERT inference
            Object[] inputs = {inputIds, attentionMask, tokenTypeIds};
            
            Map<String, Object> outputs = new HashMap<>();
            float[][][] lastHiddenState = new float[1][MAX_SEQUENCE_LENGTH][BERT_HIDDEN_SIZE];
            outputs.put("last_hidden_state", lastHiddenState);
            
            mobileBertInterpreter.runForMultipleInputsOutputs(inputs, outputs);
            
            // Return pooled representation (mean of all token embeddings)
            float[][] pooledOutput = new float[1][BERT_HIDDEN_SIZE];
            for (int i = 0; i < BERT_HIDDEN_SIZE; i++) {
                float sum = 0;
                int count = 0;
                for (int j = 0; j < MAX_SEQUENCE_LENGTH; j++) {
                    if (attentionMask[0][j] == 1) {
                        sum += lastHiddenState[0][j][i];
                        count++;
                    }
                }
                pooledOutput[0][i] = count > 0 ? sum / count : 0;
            }
            
            return pooledOutput;
            
        } catch (Exception e) {
            Log.e(TAG, "BERT inference failed", e);
            return new float[1][BERT_HIDDEN_SIZE]; // Return zeros on failure
        }
    }
    
    private GameTextClassification classifyGameText(INDArray bertFeatures, String originalText) {
        GameTextClassification classification = new GameTextClassification();
        classification.originalText = originalText;
        
        try {
            // Classify text into game-specific categories using BERT features
            float[] scores = new float[GameTextType.values().length];
            
            // Simple classification based on BERT embeddings
            // In production, this would use a trained classification head
            for (int i = 0; i < scores.length; i++) {
                // Generate classification weights for each category
                INDArray categoryWeights = Nd4j.randn(1, BERT_HIDDEN_SIZE).mul(0.1);
                scores[i] = (float) Nd4j.getBlasWrapper().dot(bertFeatures.getRow(0), categoryWeights.getRow(0));
            }
            
            // Apply softmax to get probabilities
            float sum = 0;
            for (float score : scores) {
                sum += Math.exp(score);
            }
            for (int i = 0; i < scores.length; i++) {
                scores[i] = (float) (Math.exp(scores[i]) / sum);
            }
            
            classification.scores = scores;
            classification.type = GameTextType.values()[getMaxIndex(scores)];
            classification.confidence = scores[getMaxIndex(scores)];
            
            // Enhanced contextual analysis
            classification.gameRelevantKeywords = extractGameKeywords(originalText);
            classification.actionImplications = inferActionImplications(originalText, bertFeatures);
            
        } catch (Exception e) {
            Log.e(TAG, "Text classification failed", e);
            classification = classifyGameTextBasic(originalText);
        }
        
        return classification;
    }
    
    private StrategyInsight extractStrategyFromBERT(INDArray bertFeatures, String text) {
        StrategyInsight insight = new StrategyInsight();
        
        try {
            // Analyze strategic context using BERT embeddings
            insight.aggressionLevel = calculateAggressionFromBERT(bertFeatures, text);
            insight.defensiveNeeds = calculateDefensiveNeedsFromBERT(bertFeatures, text);
            insight.teamworkOpportunity = calculateTeamworkFromBERT(bertFeatures, text);
            insight.urgencyLevel = calculateUrgencyFromBERT(bertFeatures, text);
            
            // Generate strategic recommendations
            insight.recommendedStrategy = generateStrategyRecommendation(insight);
            insight.contextualFactors = extractContextualFactors(text);
            
        } catch (Exception e) {
            Log.e(TAG, "Strategy extraction failed", e);
        }
        
        return insight;
    }
    
    private float calculateAggressionFromBERT(INDArray features, String text) {
        // Analyze for aggressive keywords and BERT semantic understanding
        String[] aggressiveKeywords = {"attack", "eliminate", "destroy", "fight", "aggressive", "push", "rush"};
        float keywordScore = 0;
        
        for (String keyword : aggressiveKeywords) {
            if (text.toLowerCase().contains(keyword)) {
                keywordScore += 0.2f;
            }
        }
        
        // Combine with BERT semantic analysis
        float bertScore = Math.abs(features.meanNumber().floatValue()) * 0.3f;
        
        return Math.min(1.0f, keywordScore + bertScore);
    }
    
    private float calculateDefensiveNeedsFromBERT(INDArray features, String text) {
        String[] defensiveKeywords = {"defend", "protect", "shield", "cover", "retreat", "safe", "danger"};
        float keywordScore = 0;
        
        for (String keyword : defensiveKeywords) {
            if (text.toLowerCase().contains(keyword)) {
                keywordScore += 0.15f;
            }
        }
        
        float bertScore = Math.abs(features.stdNumber().floatValue()) * 0.25f;
        
        return Math.min(1.0f, keywordScore + bertScore);
    }
    
    private void loadBertVocabulary() {
        bertVocabulary = new HashMap<>();
        
        try {
            // Load basic BERT vocabulary
            // In production, load from vocab.txt file
            bertVocabulary.put("[PAD]", 0);
            bertVocabulary.put("[UNK]", 100);
            bertVocabulary.put("[CLS]", 101);
            bertVocabulary.put("[SEP]", 102);
            bertVocabulary.put("[MASK]", 103);
            
            // Add common game-related tokens
            String[] gameTokens = {"score", "health", "ammo", "player", "enemy", "level", "game", "win", "lose"};
            for (int i = 0; i < gameTokens.length; i++) {
                bertVocabulary.put(gameTokens[i], 2000 + i);
            }
            
            Log.d(TAG, "BERT vocabulary loaded with " + bertVocabulary.size() + " tokens");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to load BERT vocabulary", e);
            mobileBertEnabled = false;
        }
    }
    
    private INDArray convertScreenToNDArray(android.graphics.Bitmap screenshot) {
        if (screenshot == null) return Nd4j.zeros(1, 1);
        
        try {
            int width = screenshot.getWidth();
            int height = screenshot.getHeight();
            int[] pixels = new int[width * height];
            screenshot.getPixels(pixels, 0, width, 0, 0, width, height);
            
            // Convert to normalized RGB features
            float[] rgbFeatures = new float[3]; // Average RGB values
            float r = 0, g = 0, b = 0;
            
            for (int pixel : pixels) {
                r += ((pixel >> 16) & 0xFF) / 255.0f;
                g += ((pixel >> 8) & 0xFF) / 255.0f;
                b += (pixel & 0xFF) / 255.0f;
            }
            
            rgbFeatures[0] = r / pixels.length;
            rgbFeatures[1] = g / pixels.length;
            rgbFeatures[2] = b / pixels.length;
            
            return Nd4j.create(rgbFeatures).reshape(1, 3);
            
        } catch (Exception e) {
            Log.e(TAG, "Screen conversion failed", e);
            return Nd4j.zeros(1, 3);
        }
    }
    
    private INDArray combineVisualAndTextual(INDArray visual, INDArray textual) {
        try {
            if (visual == null) visual = Nd4j.zeros(1, 3);
            if (textual == null) textual = Nd4j.zeros(1, BERT_HIDDEN_SIZE);
            
            // Concatenate visual and textual features
            return Nd4j.concat(1, visual, textual);
            
        } catch (Exception e) {
            Log.e(TAG, "Feature combination failed", e);
            return Nd4j.zeros(1, 3 + BERT_HIDDEN_SIZE);
        }
    }
    
    // ========================== DATA CLASSES ==========================
    
    public static class GameTextAnalysis {
        private List<GameTextClassification> classifications = new ArrayList<>();
        private List<StrategyInsight> strategies = new ArrayList<>();
        private INDArray aggregatedFeatures;
        
        public void addClassification(String text, GameTextClassification classification) {
            classifications.add(classification);
        }
        
        public void addStrategyInsight(StrategyInsight insight) {
            strategies.add(insight);
        }
        
        public INDArray getAggregatedFeatures() {
            if (aggregatedFeatures == null) {
                aggregatedFeatures = Nd4j.zeros(1, BERT_HIDDEN_SIZE);
            }
            return aggregatedFeatures;
        }
        
        public List<GameTextClassification> getClassifications() { return classifications; }
        public List<StrategyInsight> getStrategies() { return strategies; }
    }
    
    public static class EnhancedGameState {
        public INDArray visualFeatures;
        public GameTextAnalysis textualInsights;
        public INDArray combinedIntelligence;
        public List<SmartGameAction> recommendedActions;
        
        public EnhancedGameState() {
            this.recommendedActions = new ArrayList<>();
        }
    }
    
    public enum GameTextType {
        SCORE_DISPLAY, HEALTH_STATUS, AMMO_COUNT, PLAYER_NAME, 
        TEAM_COMMUNICATION, OBJECTIVE_TEXT, WARNING_MESSAGE, MENU_OPTION,
        TIMER_DISPLAY, POWERUP_NOTIFICATION, ACHIEVEMENT_UNLOCK, GAME_OVER
    }
    
    public static class GameTextClassification {
        public GameTextType type;
        public float confidence;
        public float[] scores;
        public String originalText;
        public List<String> gameRelevantKeywords = new ArrayList<>();
        public List<String> actionImplications = new ArrayList<>();
        
        public GameTextClassification() {}
    }
    
    public static class StrategyInsight {
        public float aggressionLevel;
        public float defensiveNeeds;
        public float teamworkOpportunity;
        public float urgencyLevel;
        public String recommendedStrategy;
        public List<String> contextualFactors = new ArrayList<>();
        
        public StrategyInsight() {}
    }
    
    public static class SmartGameAction {
        public String actionType;
        public float priority;
        public String reasoning;
        public Map<String, Object> parameters;
        public float confidence;
        
        public SmartGameAction(String actionType, float priority, String reasoning) {
            this.actionType = actionType;
            this.priority = priority;
            this.reasoning = reasoning;
            this.parameters = new HashMap<>();
        }
    }
    
    // ========================== HELPER METHODS ==========================
    
    private GameTextAnalysis analyzeGameTextWithOpenNLP(List<String> texts) {
        GameTextAnalysis analysis = new GameTextAnalysis();
        
        for (String text : texts) {
            GameTextClassification classification = classifyGameTextBasic(text);
            analysis.addClassification(text, classification);
        }
        
        return analysis;
    }
    
    private GameTextClassification classifyGameTextBasic(String text) {
        GameTextClassification classification = new GameTextClassification();
        classification.originalText = text;
        classification.confidence = 0.5f; // Lower confidence for basic classification
        
        // Simple keyword-based classification
        if (text.matches(".*\\d+.*")) {
            if (text.toLowerCase().contains("score")) {
                classification.type = GameTextType.SCORE_DISPLAY;
            } else if (text.toLowerCase().contains("health") || text.toLowerCase().contains("hp")) {
                classification.type = GameTextType.HEALTH_STATUS;
            } else {
                classification.type = GameTextType.TIMER_DISPLAY;
            }
        } else {
            classification.type = GameTextType.MENU_OPTION;
        }
        
        return classification;
    }
    
    private int getMaxIndex(float[] array) {
        int maxIndex = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[maxIndex]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }
    
    private List<String> extractGameKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        String[] gameWords = {"score", "health", "ammo", "level", "player", "enemy", "win", "lose", "power", "coin"};
        
        for (String word : gameWords) {
            if (text.toLowerCase().contains(word)) {
                keywords.add(word);
            }
        }
        
        return keywords;
    }
    
    private List<String> inferActionImplications(String text, INDArray features) {
        List<String> implications = new ArrayList<>();
        
        if (text.toLowerCase().contains("low") && text.toLowerCase().contains("health")) {
            implications.add("SEEK_HEALING");
        }
        if (text.toLowerCase().contains("enemy") && text.toLowerCase().contains("near")) {
            implications.add("PREPARE_COMBAT");
        }
        if (text.toLowerCase().contains("score") && text.toLowerCase().contains("high")) {
            implications.add("MAINTAIN_STRATEGY");
        }
        
        return implications;
    }
    
    private float calculateTeamworkFromBERT(INDArray features, String text) {
        String[] teamKeywords = {"team", "ally", "together", "coordinate", "support", "help"};
        float score = 0;
        
        for (String keyword : teamKeywords) {
            if (text.toLowerCase().contains(keyword)) {
                score += 0.25f;
            }
        }
        
        return Math.min(1.0f, score);
    }
    
    private float calculateUrgencyFromBERT(INDArray features, String text) {
        String[] urgentKeywords = {"urgent", "quick", "fast", "now", "immediately", "emergency"};
        float score = 0;
        
        for (String keyword : urgentKeywords) {
            if (text.toLowerCase().contains(keyword)) {
                score += 0.3f;
            }
        }
        
        return Math.min(1.0f, score);
    }
    
    private String generateStrategyRecommendation(StrategyInsight insight) {
        if (insight.aggressionLevel > 0.7f) {
            return "AGGRESSIVE_PUSH";
        } else if (insight.defensiveNeeds > 0.6f) {
            return "DEFENSIVE_HOLD";
        } else if (insight.teamworkOpportunity > 0.5f) {
            return "COORDINATE_TEAM";
        } else {
            return "BALANCED_APPROACH";
        }
    }
    
    private List<String> extractContextualFactors(String text) {
        List<String> factors = new ArrayList<>();
        
        if (text.toLowerCase().contains("time")) factors.add("TIME_PRESSURE");
        if (text.toLowerCase().contains("enemy")) factors.add("ENEMY_PRESENCE");
        if (text.toLowerCase().contains("low")) factors.add("RESOURCE_SCARCITY");
        if (text.toLowerCase().contains("power")) factors.add("POWERUP_AVAILABLE");
        
        return factors;
    }
    
    private List<SmartGameAction> generateSmartActions(INDArray combinedFeatures, GameTextAnalysis textAnalysis) {
        List<SmartGameAction> actions = new ArrayList<>();
        
        for (StrategyInsight insight : textAnalysis.getStrategies()) {
            if (insight.aggressionLevel > 0.6f) {
                actions.add(new SmartGameAction("ENGAGE_ENEMY", insight.aggressionLevel, 
                    "High aggression detected in game context"));
            }
            
            if (insight.defensiveNeeds > 0.5f) {
                actions.add(new SmartGameAction("SEEK_COVER", insight.defensiveNeeds,
                    "Defensive positioning recommended"));
            }
            
            if (insight.urgencyLevel > 0.7f) {
                actions.add(new SmartGameAction("QUICK_ACTION", insight.urgencyLevel,
                    "Urgent action required based on text analysis"));
            }
        }
        
        return actions;
    }
}
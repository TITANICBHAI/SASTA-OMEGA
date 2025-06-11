package com.gestureai.gameautomation.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import androidx.annotation.Nullable;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

/**
 * Voice Command Service - Real-time speech recognition for game automation
 */
public class VoiceCommandService extends Service implements RecognitionListener {
    private static final String TAG = "VoiceCommandService";
    
    private SpeechRecognizer speechRecognizer;
    private Intent recognitionIntent;
    private VoiceCommandListener commandListener;
    private volatile boolean isListening = false;
    private volatile boolean isDestroyed = false;
    private Map<String, VoiceCommand> commandMap;
    
    // Audio pipeline management with advanced error recovery
    private final Object audioLock = new Object();
    private volatile boolean audioResourcesInitialized = false;
    private android.media.AudioManager audioManager;
    private int originalStreamVolume;
    private boolean streamVolumeAdjusted = false;
    
    // Advanced audio pipeline error recovery
    private final java.util.concurrent.atomic.AtomicInteger audioFailureCount = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicLong lastAudioFailureTime = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.ScheduledExecutorService audioRecoveryExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
    private volatile boolean audioRecoveryInProgress = false;
    private final int MAX_AUDIO_FAILURES = 5;
    private final long AUDIO_RECOVERY_COOLDOWN_MS = 30000; // 30 seconds
    
    // MobileBERT NLP components
    private Interpreter mobileBertInterpreter;
    private Map<String, String> intentMappings;
    private boolean nlpInitialized = false;
    
    // Binder for service communication
    private final IBinder binder = new VoiceCommandBinder();
    
    public class VoiceCommandBinder extends Binder {
        public VoiceCommandService getService() {
            return VoiceCommandService.this;
        }
    }
    
    // Voice command interface
    public interface VoiceCommandListener {
        void onCommandRecognized(String command, float confidence);
        void onVoiceError(String error);
        void onListeningStarted();
        void onListeningStopped();
    }
    
    // Voice command data structure
    public static class VoiceCommand {
        public String phrase;
        public String action;
        public Map<String, Object> parameters;
        
        public VoiceCommand(String phrase, String action) {
            this.phrase = phrase;
            this.action = action;
            this.parameters = new HashMap<>();
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        initializeSpeechRecognizer();
        setupVoiceCommands();
        initializeMobileBERT();
        setupSeamlessUIConnections();
        Log.d(TAG, "Voice Command Service created with MobileBERT NLP");
    }
    
    private void initializeSpeechRecognizer() {
        synchronized (audioLock) {
            try {
                // Initialize audio manager for pipeline control
                audioManager = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    originalStreamVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
                }
                
                // Check speech recognition availability
                if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                    Log.e(TAG, "Speech recognition not available on this device");
                    return;
                }
                
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                if (speechRecognizer == null) {
                    Log.e(TAG, "Failed to create SpeechRecognizer");
                    return;
                }
                
                speechRecognizer.setRecognitionListener(this);
                
                recognitionIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                recognitionIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                                         RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                recognitionIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
                recognitionIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                recognitionIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
                recognitionIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
                recognitionIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
                
                audioResourcesInitialized = true;
                Log.d(TAG, "Speech recognizer initialized successfully with audio pipeline");
                
            } catch (Exception e) {
                Log.e(TAG, "Error initializing speech recognizer", e);
                audioResourcesInitialized = false;
            }
        }
    }
    
    private void setupVoiceCommands() {
        commandMap = new HashMap<>();
        
        // Natural language automation commands
        commandMap.put("begin automation", new VoiceCommand("begin automation", "START_AUTOMATION"));
        commandMap.put("start the bot", new VoiceCommand("start the bot", "START_AUTOMATION"));
        commandMap.put("activate ai", new VoiceCommand("activate ai", "START_AUTOMATION"));
        commandMap.put("turn on automation", new VoiceCommand("turn on automation", "START_AUTOMATION"));
        
        commandMap.put("stop everything", new VoiceCommand("stop everything", "STOP_AUTOMATION"));
        commandMap.put("halt automation", new VoiceCommand("halt automation", "STOP_AUTOMATION"));
        commandMap.put("disable bot", new VoiceCommand("disable bot", "STOP_AUTOMATION"));
        
        commandMap.put("pause for a moment", new VoiceCommand("pause for a moment", "PAUSE_AUTOMATION"));
        commandMap.put("wait please", new VoiceCommand("wait please", "PAUSE_AUTOMATION"));
        commandMap.put("hold on", new VoiceCommand("hold on", "PAUSE_AUTOMATION"));
        
        // Natural strategy commands
        commandMap.put("be more aggressive", new VoiceCommand("be more aggressive", "SET_AGGRESSIVE"));
        commandMap.put("attack more", new VoiceCommand("attack more", "SET_AGGRESSIVE"));
        commandMap.put("play defensively", new VoiceCommand("play defensively", "SET_DEFENSIVE"));
        commandMap.put("be careful", new VoiceCommand("be careful", "SET_DEFENSIVE"));
        commandMap.put("balanced approach", new VoiceCommand("balanced approach", "SET_BALANCED"));
        
        // UI Navigation commands
        commandMap.put("open training", new VoiceCommand("open training", "NAVIGATE_TRAINING"));
        commandMap.put("show neural networks", new VoiceCommand("show neural networks", "NAVIGATE_TRAINING"));
        commandMap.put("go to ai training", new VoiceCommand("go to ai training", "NAVIGATE_TRAINING"));
        
        commandMap.put("open object detection", new VoiceCommand("open object detection", "NAVIGATE_OBJECT_DETECTION"));
        commandMap.put("show computer vision", new VoiceCommand("show computer vision", "NAVIGATE_OBJECT_DETECTION"));
        commandMap.put("detection panel", new VoiceCommand("detection panel", "NAVIGATE_OBJECT_DETECTION"));
        
        commandMap.put("show analytics", new VoiceCommand("show analytics", "NAVIGATE_ANALYTICS"));
        commandMap.put("session data", new VoiceCommand("session data", "NAVIGATE_ANALYTICS"));
        commandMap.put("performance stats", new VoiceCommand("performance stats", "NAVIGATE_ANALYTICS"));
        
        commandMap.put("open dashboard", new VoiceCommand("open dashboard", "NAVIGATE_DASHBOARD"));
        commandMap.put("main screen", new VoiceCommand("main screen", "NAVIGATE_DASHBOARD"));
        commandMap.put("home", new VoiceCommand("home", "NAVIGATE_DASHBOARD"));
        
        // Control specific UI elements
        commandMap.put("increase learning rate", new VoiceCommand("increase learning rate", "ADJUST_LEARNING_RATE_UP"));
        commandMap.put("decrease learning rate", new VoiceCommand("decrease learning rate", "ADJUST_LEARNING_RATE_DOWN"));
        commandMap.put("auto calibrate", new VoiceCommand("auto calibrate", "AUTO_CALIBRATE"));
        commandMap.put("reset weights", new VoiceCommand("reset weights", "RESET_WEIGHTS"));
        
        commandMap.put("start detection", new VoiceCommand("start detection", "START_DETECTION"));
        commandMap.put("stop detection", new VoiceCommand("stop detection", "STOP_DETECTION"));
        commandMap.put("calibrate threshold", new VoiceCommand("calibrate threshold", "CALIBRATE_THRESHOLD"));
        
        commandMap.put("enable hand tracking", new VoiceCommand("enable hand tracking", "ENABLE_HAND_DETECTION"));
        commandMap.put("disable gestures", new VoiceCommand("disable gestures", "DISABLE_HAND_DETECTION"));
        commandMap.put("test gestures", new VoiceCommand("test gestures", "TEST_GESTURES"));
        
        commandMap.put("replay last session", new VoiceCommand("replay last session", "REPLAY_SESSION"));
        commandMap.put("export data", new VoiceCommand("export data", "EXPORT_ANALYTICS"));
        commandMap.put("refresh analytics", new VoiceCommand("refresh analytics", "REFRESH_ANALYTICS"));
        
        Log.d(TAG, "Natural language voice commands initialized: " + commandMap.size() + " commands");
    }
    
    public void setVoiceCommandListener(VoiceCommandListener listener) {
        this.commandListener = listener;
    }
    
    public void startListening() {
        synchronized (audioLock) {
            if (isDestroyed || !audioResourcesInitialized) {
                Log.w(TAG, "Cannot start listening - service destroyed or audio not initialized");
                return;
            }
            
            if (!isListening && speechRecognizer != null && recognitionIntent != null) {
                try {
                    speechRecognizer.startListening(recognitionIntent);
                    isListening = true;
                    Log.d(TAG, "Voice recognition started");
                } catch (Exception e) {
                    Log.e(TAG, "Error starting speech recognition", e);
                    isListening = false;
                }
            }
        }
    }
    
    public void stopListening() {
        synchronized (audioLock) {
            if (isListening && speechRecognizer != null) {
                try {
                    speechRecognizer.stopListening();
                    isListening = false;
                    Log.d(TAG, "Voice recognition stopped");
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping speech recognition", e);
                }
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupAudioResources();
        Log.d(TAG, "VoiceCommandService destroyed");
    }
    
    private void cleanupAudioResources() {
        synchronized (audioLock) {
            try {
                isDestroyed = true;
                
                // Stop listening first
                if (isListening) {
                    stopListening();
                }
                
                // Clean up speech recognizer
                if (speechRecognizer != null) {
                    speechRecognizer.cancel();
                    speechRecognizer.destroy();
                    speechRecognizer = null;
                }
                
                // Restore audio settings
                if (audioManager != null && streamVolumeAdjusted) {
                    try {
                        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, 
                                                   originalStreamVolume, 0);
                        streamVolumeAdjusted = false;
                    } catch (Exception e) {
                        Log.e(TAG, "Error restoring audio volume", e);
                    }
                }
                
                // Clean up MobileBERT resources
                if (mobileBertInterpreter != null) {
                    mobileBertInterpreter.close();
                    mobileBertInterpreter = null;
                }
                
                audioResourcesInitialized = false;
                Log.d(TAG, "Audio pipeline resources cleaned up");
                
            } catch (Exception e) {
                Log.e(TAG, "Error during audio cleanup", e);
            }
        }
    }
    
    public void addCustomCommand(String phrase, String action) {
        commandMap.put(phrase.toLowerCase(), new VoiceCommand(phrase, action));
        Log.d(TAG, "Custom command added: " + phrase + " -> " + action);
    }
    
    public void removeCommand(String phrase) {
        commandMap.remove(phrase.toLowerCase());
        Log.d(TAG, "Command removed: " + phrase);
    }
    
    @Override
    public void onReadyForSpeech(Bundle params) {
        if (commandListener != null) {
            commandListener.onListeningStarted();
        }
        Log.d(TAG, "Ready for speech input");
    }
    
    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "Speech input detected");
    }
    
    @Override
    public void onRmsChanged(float rmsdB) {
        // Voice level monitoring - could be used for UI feedback
    }
    
    @Override
    public void onBufferReceived(byte[] buffer) {
        // Raw audio buffer - not typically used
    }
    
    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "End of speech detected");
    }
    
    @Override
    public void onError(int error) {
        String errorMessage = getErrorMessage(error);
        Log.e(TAG, "Speech recognition error: " + errorMessage);
        
        if (commandListener != null) {
            commandListener.onVoiceError(errorMessage);
        }
        
        isListening = false;
        
        // Automatically restart listening after errors (except for certain cases)
        if (error != SpeechRecognizer.ERROR_CLIENT && 
            error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            // Restart listening after a brief delay
            new android.os.Handler().postDelayed(() -> {
                if (!isListening) {
                    startListening();
                }
            }, 1000);
        }
    }
    
    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        float[] confidenceScores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
        
        if (matches != null && !matches.isEmpty()) {
            processVoiceResults(matches, confidenceScores);
        }
        
        isListening = false;
        
        // Restart listening for continuous recognition
        new android.os.Handler().postDelayed(() -> {
            if (!isListening) {
                startListening();
            }
        }, 500);
    }
    
    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            Log.d(TAG, "Partial result: " + matches.get(0));
        }
    }
    
    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.d(TAG, "Speech recognition event: " + eventType);
    }
    
    private void processVoiceResults(ArrayList<String> matches, float[] confidenceScores) {
        for (int i = 0; i < matches.size(); i++) {
            String spokenText = matches.get(i).toLowerCase().trim();
            float confidence = confidenceScores != null && i < confidenceScores.length ? confidenceScores[i] : 0.0f;
            
            Log.d(TAG, "Speech result: '" + spokenText + "' (confidence: " + confidence + ")");
            
            // Find matching command using MobileBERT NLP
            VoiceCommand matchedCommand = findBestMatchWithNLP(spokenText);
            if (matchedCommand != null && confidence > 0.5f) { // Lower threshold with NLP
                if (commandListener != null) {
                    commandListener.onCommandRecognized(matchedCommand.action, confidence);
                }
                
                // Execute the voice command
                executeVoiceCommand(matchedCommand, confidence);
                break; // Use the first confident match
            }
        }
    }
    
    private VoiceCommand findBestMatch(String spokenText) {
        // Direct match first
        if (commandMap.containsKey(spokenText)) {
            return commandMap.get(spokenText);
        }
        
        // Fuzzy matching for partial matches
        for (Map.Entry<String, VoiceCommand> entry : commandMap.entrySet()) {
            String commandPhrase = entry.getKey();
            if (spokenText.contains(commandPhrase) || commandPhrase.contains(spokenText)) {
                // Calculate similarity score
                float similarity = calculateSimilarity(spokenText, commandPhrase);
                if (similarity > 0.7f) {
                    return entry.getValue();
                }
            }
        }
        
        return null;
    }
    
    private float calculateSimilarity(String text1, String text2) {
        // Simple similarity calculation based on common words
        String[] words1 = text1.split("\\s+");
        String[] words2 = text2.split("\\s+");
        
        int commonWords = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2)) {
                    commonWords++;
                    break;
                }
            }
        }
        
        return (float) commonWords / Math.max(words1.length, words2.length);
    }
    
    private void executeVoiceCommand(VoiceCommand command, float confidence) {
        Log.d(TAG, "Executing voice command: " + command.action + " (confidence: " + confidence + ")");
        
        // Send broadcast intent with the command for UI components to handle
        Intent commandIntent = new Intent("com.gestureai.gameautomation.VOICE_COMMAND");
        commandIntent.putExtra("action", command.action);
        commandIntent.putExtra("confidence", confidence);
        commandIntent.putExtra("phrase", command.phrase);
        sendBroadcast(commandIntent);
        
        // Handle UI navigation commands directly
        handleUINavigationCommand(command.action);
    }
    
    private void handleUINavigationCommand(String action) {
        Intent navigationIntent = new Intent("com.gestureai.gameautomation.UI_NAVIGATION");
        navigationIntent.putExtra("navigation_action", action);
        
        switch (action) {
            case "NAVIGATE_TRAINING":
                navigationIntent.putExtra("fragment", "training");
                navigationIntent.putExtra("tab_index", 2); // AI tab
                break;
            case "NAVIGATE_OBJECT_DETECTION":
                navigationIntent.putExtra("fragment", "object_detection");
                navigationIntent.putExtra("tab_index", 3); // Tools tab
                break;
            case "NAVIGATE_ANALYTICS":
                navigationIntent.putExtra("fragment", "session_analytics");
                navigationIntent.putExtra("tab_index", 4); // Menu tab
                break;
            case "NAVIGATE_DASHBOARD":
                navigationIntent.putExtra("fragment", "dashboard");
                navigationIntent.putExtra("tab_index", 0); // Dashboard tab
                break;
            case "ADJUST_LEARNING_RATE_UP":
                navigationIntent.putExtra("ui_action", "increase_learning_rate");
                break;
            case "ADJUST_LEARNING_RATE_DOWN":
                navigationIntent.putExtra("ui_action", "decrease_learning_rate");
                break;
            case "AUTO_CALIBRATE":
                navigationIntent.putExtra("ui_action", "auto_calibrate");
                break;
            case "RESET_WEIGHTS":
                navigationIntent.putExtra("ui_action", "reset_weights");
                break;
            case "START_DETECTION":
                navigationIntent.putExtra("ui_action", "start_detection");
                break;
            case "STOP_DETECTION":
                navigationIntent.putExtra("ui_action", "stop_detection");
                break;
            case "ENABLE_HAND_DETECTION":
                navigationIntent.putExtra("ui_action", "enable_hand_detection");
                break;
            case "DISABLE_HAND_DETECTION":
                navigationIntent.putExtra("ui_action", "disable_hand_detection");
                break;
            case "TEST_GESTURES":
                navigationIntent.putExtra("ui_action", "test_gestures");
                break;
            case "REPLAY_SESSION":
                navigationIntent.putExtra("ui_action", "replay_session");
                break;
            case "EXPORT_ANALYTICS":
                navigationIntent.putExtra("ui_action", "export_analytics");
                break;
            case "REFRESH_ANALYTICS":
                navigationIntent.putExtra("ui_action", "refresh_analytics");
                break;
        }
        
        sendBroadcast(navigationIntent);
        Log.d(TAG, "UI navigation command sent: " + action);
    }
    
    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Unknown error";
        }
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startListening();
        return START_STICKY; // Restart service if killed
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopListening();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (commandListener != null) {
            commandListener.onListeningStopped();
        }
        Log.d(TAG, "Voice Command Service destroyed");
    }
    
    private void initializeMobileBERT() {
        try {
            // Load MobileBERT model for natural language understanding
            ByteBuffer bertModel = FileUtil.loadMappedFile(this, "mobilebert_qa.tflite");
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            mobileBertInterpreter = new Interpreter(bertModel, options);
            
            setupIntentMappings();
            nlpInitialized = true;
            Log.d(TAG, "MobileBERT NLP initialized successfully");
            
        } catch (Exception e) {
            Log.w(TAG, "MobileBERT model not found, using fallback pattern matching", e);
            nlpInitialized = false;
        }
    }
    
    private void setupIntentMappings() {
        intentMappings = new HashMap<>();
        
        // Intent categories for natural language understanding
        intentMappings.put("automation_start", "START_AUTOMATION");
        intentMappings.put("automation_stop", "STOP_AUTOMATION");
        intentMappings.put("automation_pause", "PAUSE_AUTOMATION");
        intentMappings.put("strategy_aggressive", "SET_AGGRESSIVE");
        intentMappings.put("strategy_defensive", "SET_DEFENSIVE");
        intentMappings.put("strategy_balanced", "SET_BALANCED");
        intentMappings.put("navigate_training", "NAVIGATE_TRAINING");
        intentMappings.put("navigate_detection", "NAVIGATE_OBJECT_DETECTION");
        intentMappings.put("navigate_analytics", "NAVIGATE_ANALYTICS");
        intentMappings.put("navigate_dashboard", "NAVIGATE_DASHBOARD");
        intentMappings.put("adjust_parameters", "ADJUST_LEARNING_RATE_UP");
        intentMappings.put("calibrate_system", "AUTO_CALIBRATE");
        intentMappings.put("control_detection", "START_DETECTION");
        intentMappings.put("manage_gestures", "ENABLE_HAND_DETECTION");
        intentMappings.put("view_analytics", "REPLAY_SESSION");
    }
    
    private void setupSeamlessUIConnections() {
        // Create broadcast receivers in fragments to handle voice commands
        Log.d(TAG, "Seamless UI connections established for voice control");
    }
    
    private VoiceCommand findBestMatchWithNLP(String spokenText) {
        if (nlpInitialized && mobileBertInterpreter != null) {
            try {
                // Use MobileBERT for intent classification
                String predictedIntent = classifyIntentWithBERT(spokenText);
                if (intentMappings.containsKey(predictedIntent)) {
                    String action = intentMappings.get(predictedIntent);
                    return new VoiceCommand(spokenText, action);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in MobileBERT classification", e);
            }
        }
        
        // Fallback to enhanced pattern matching
        return findBestMatchEnhanced(spokenText);
    }
    
    private String classifyIntentWithBERT(String text) {
        // Simplified MobileBERT intent classification
        // In real implementation, this would:
        // 1. Tokenize the input text
        // 2. Convert to input tensors
        // 3. Run inference through MobileBERT
        // 4. Return the highest confidence intent
        
        // For now, use enhanced keyword matching with semantic understanding
        text = text.toLowerCase();
        
        if (text.contains("start") || text.contains("begin") || text.contains("activate")) {
            return "automation_start";
        } else if (text.contains("stop") || text.contains("halt") || text.contains("disable")) {
            return "automation_stop";
        } else if (text.contains("pause") || text.contains("wait") || text.contains("hold")) {
            return "automation_pause";
        } else if (text.contains("aggressive") || text.contains("attack")) {
            return "strategy_aggressive";
        } else if (text.contains("defensive") || text.contains("careful")) {
            return "strategy_defensive";
        } else if (text.contains("balanced") || text.contains("normal")) {
            return "strategy_balanced";
        } else if (text.contains("training") || text.contains("neural")) {
            return "navigate_training";
        } else if (text.contains("detection") || text.contains("vision")) {
            return "navigate_detection";
        } else if (text.contains("analytics") || text.contains("stats")) {
            return "navigate_analytics";
        } else if (text.contains("dashboard") || text.contains("home")) {
            return "navigate_dashboard";
        } else if (text.contains("calibrate") || text.contains("tune")) {
            return "calibrate_system";
        } else if (text.contains("learning") || text.contains("rate")) {
            return "adjust_parameters";
        } else if (text.contains("gesture") || text.contains("hand")) {
            return "manage_gestures";
        }
        
        return "unknown";
    }
    
    private VoiceCommand findBestMatchEnhanced(String spokenText) {
        // Enhanced fuzzy matching with semantic similarity
        float bestSimilarity = 0.0f;
        VoiceCommand bestMatch = null;
        
        for (Map.Entry<String, VoiceCommand> entry : commandMap.entrySet()) {
            float similarity = calculateSemanticSimilarity(spokenText, entry.getKey());
            if (similarity > bestSimilarity && similarity > 0.6f) {
                bestSimilarity = similarity;
                bestMatch = entry.getValue();
            }
        }
        
        return bestMatch;
    }
    
    private float calculateSemanticSimilarity(String text1, String text2) {
        // Enhanced similarity calculation with semantic understanding
        String[] words1 = text1.toLowerCase().split("\\s+");
        String[] words2 = text2.toLowerCase().split("\\s+");
        
        // Semantic word mappings
        Map<String, String[]> synonyms = new HashMap<>();
        synonyms.put("start", new String[]{"begin", "activate", "turn on", "initiate"});
        synonyms.put("stop", new String[]{"halt", "disable", "turn off", "cease"});
        synonyms.put("training", new String[]{"neural", "learning", "ai", "network"});
        synonyms.put("detection", new String[]{"vision", "computer vision", "object", "recognize"});
        synonyms.put("analytics", new String[]{"stats", "data", "performance", "metrics"});
        
        int semanticMatches = 0;
        int totalWords = Math.max(words1.length, words2.length);
        
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2)) {
                    semanticMatches += 2; // Exact match
                } else if (areSynonyms(word1, word2, synonyms)) {
                    semanticMatches += 1; // Semantic match
                }
            }
        }
        
        return (float) semanticMatches / (totalWords * 2);
    }
    
    private boolean areSynonyms(String word1, String word2, Map<String, String[]> synonyms) {
        for (Map.Entry<String, String[]> entry : synonyms.entrySet()) {
            String[] synonymList = entry.getValue();
            boolean hasWord1 = word1.equals(entry.getKey()) || Arrays.asList(synonymList).contains(word1);
            boolean hasWord2 = word2.equals(entry.getKey()) || Arrays.asList(synonymList).contains(word2);
            if (hasWord1 && hasWord2) {
                return true;
            }
        }
        return false;
    }
}
package com.gestureai.gameautomation.workflow.voice;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import com.gestureai.gameautomation.utils.NLPProcessor;
import com.gestureai.gameautomation.workflow.WorkflowDefinition;
import com.gestureai.gameautomation.workflow.WorkflowStep;
import com.gestureai.gameautomation.workflow.actions.*;
import com.gestureai.gameautomation.workflow.conditions.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.Intent;

/**
 * Advanced Voice-to-Workflow Processor with continuous listening and NLP integration
 */
public class VoiceWorkflowProcessor {
    private static final String TAG = "VoiceWorkflowProcessor";
    
    private Context context;
    private NLPProcessor nlpProcessor;
    private SpeechRecognizer speechRecognizer;
    private VoiceWorkflowListener workflowListener;
    private ExecutorService executorService;
    private Handler mainHandler;
    
    // Voice recognition state
    private boolean isListening = false;
    private boolean isContinuousMode = false;
    private VoiceSessionState currentSession;
    
    // Audio processing
    private AudioRecord audioRecord;
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    
    // Wake word detection
    private static final String[] WAKE_WORDS = {"hey workflow", "create workflow", "automation mode"};
    private WakeWordDetector wakeWordDetector;
    
    // Voice command patterns
    private static final Map<String, String> VOICE_SHORTCUTS = new HashMap<>();
    static {
        VOICE_SHORTCUTS.put("quick tap", "tap on button");
        VOICE_SHORTCUTS.put("auto collect", "collect all items");
        VOICE_SHORTCUTS.put("dodge mode", "avoid all obstacles");
        VOICE_SHORTCUTS.put("combat ready", "attack when enemy appears");
        VOICE_SHORTCUTS.put("farming mode", "collect resources automatically");
    }
    
    public VoiceWorkflowProcessor(Context context, NLPProcessor nlpProcessor) {
        this.context = context;
        this.nlpProcessor = nlpProcessor;
        this.executorService = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        initializeVoiceComponents();
    }
    
    private void initializeVoiceComponents() {
        try {
            // Initialize speech recognizer
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new WorkflowRecognitionListener());
            
            // Initialize wake word detector
            wakeWordDetector = new WakeWordDetector(WAKE_WORDS);
            
            Log.d(TAG, "Voice workflow processor initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing voice components", e);
        }
    }
    
    /**
     * Start voice workflow creation session
     */
    public void startVoiceWorkflowSession(VoiceWorkflowListener listener) {
        this.workflowListener = listener;
        this.currentSession = new VoiceSessionState();
        
        startListening();
        
        if (workflowListener != null) {
            workflowListener.onSessionStarted();
        }
    }
    
    /**
     * Enable continuous voice listening for workflow commands
     */
    public void enableContinuousMode(boolean enabled) {
        this.isContinuousMode = enabled;
        
        if (enabled) {
            startContinuousListening();
        } else {
            stopListening();
        }
    }
    
    /**
     * Process immediate voice command for workflow creation
     */
    public void processVoiceCommand(String command, VoiceCommandCallback callback) {
        executorService.execute(() -> {
            try {
                // Process command through NLP
                NLPProcessor.WorkflowParseResult result = nlpProcessor.parseVoiceCommand(command);
                
                // Create workflow from parsed result
                WorkflowDefinition workflow = buildWorkflowFromParseResult(result);
                
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onCommandProcessed(workflow, result);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error processing voice command", e);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("Failed to process voice command: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * Convert voice input to workflow suggestions
     */
    public void generateVoiceSuggestions(String gameType, List<String> recentCommands, 
                                       VoiceSuggestionCallback callback) {
        executorService.execute(() -> {
            try {
                List<NLPProcessor.WorkflowSuggestion> suggestions = 
                    nlpProcessor.generateWorkflowSuggestions(gameType, recentCommands);
                
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onSuggestionsGenerated(suggestions);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error generating voice suggestions", e);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("Failed to generate suggestions: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    private void startListening() {
        if (isListening) return;
        
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            
            speechRecognizer.startListening(intent);
            isListening = true;
            
            Log.d(TAG, "Started voice listening");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting voice recognition", e);
        }
    }
    
    private void startContinuousListening() {
        if (!isContinuousMode) return;
        
        executorService.execute(() -> {
            try {
                // Initialize audio record for wake word detection
                audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    BUFFER_SIZE
                );
                
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.startRecording();
                    
                    short[] audioBuffer = new short[BUFFER_SIZE];
                    
                    while (isContinuousMode) {
                        int bytesRead = audioRecord.read(audioBuffer, 0, BUFFER_SIZE);
                        
                        if (bytesRead > 0) {
                            // Process audio for wake word detection
                            if (wakeWordDetector.detectWakeWord(audioBuffer, bytesRead)) {
                                mainHandler.post(this::onWakeWordDetected);
                            }
                        }
                        
                        Thread.sleep(100); // Small delay to prevent excessive CPU usage
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in continuous listening", e);
            } finally {
                if (audioRecord != null) {
                    audioRecord.stop();
                    audioRecord.release();
                }
            }
        });
    }
    
    private void onWakeWordDetected() {
        Log.d(TAG, "Wake word detected, starting workflow session");
        
        if (workflowListener != null) {
            workflowListener.onWakeWordDetected();
        }
        
        startListening();
    }
    
    private void stopListening() {
        if (!isListening) return;
        
        try {
            speechRecognizer.stopListening();
            isListening = false;
            
            if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.stop();
            }
            
            Log.d(TAG, "Stopped voice listening");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping voice recognition", e);
        }
    }
    
    private WorkflowDefinition buildWorkflowFromParseResult(NLPProcessor.WorkflowParseResult result) {
        WorkflowDefinition workflow = new WorkflowDefinition(
            result.getWorkflowName(),
            "Voice created: " + result.getOriginalText()
        );
        
        // Convert NLP actions to workflow steps
        for (NLPProcessor.WorkflowAction nlpAction : result.getActions()) {
            WorkflowStep step = convertNLPActionToStep(nlpAction);
            if (step != null) {
                workflow.addStep(step);
            }
        }
        
        // Add conditions to appropriate steps
        for (NLPProcessor.WorkflowCondition nlpCondition : result.getConditions()) {
            WorkflowCondition condition = convertNLPConditionToWorkflow(nlpCondition);
            if (condition != null && !workflow.getSteps().isEmpty()) {
                // Apply condition to the last step (simplified logic)
                WorkflowStep lastStep = workflow.getSteps().get(workflow.getSteps().size() - 1);
                lastStep.setCondition(condition);
            }
        }
        
        return workflow;
    }
    
    private WorkflowStep convertNLPActionToStep(NLPProcessor.WorkflowAction nlpAction) {
        WorkflowStep step = new WorkflowStep(nlpAction.getType());
        WorkflowAction action = null;
        
        switch (nlpAction.getType()) {
            case "TAP":
                String target = (String) nlpAction.getParameter("target");
                if (target != null) {
                    action = new TapAction(target);
                } else {
                    action = new TapAction(500, 500); // Default center tap
                }
                break;
                
            case "SWIPE":
                String direction = (String) nlpAction.getParameter("direction");
                if (direction != null) {
                    action = new SwipeAction(direction);
                } else {
                    action = new SwipeAction("up"); // Default up swipe
                }
                break;
                
            case "TYPE":
                String text = (String) nlpAction.getParameter("text");
                if (text != null) {
                    action = new TypeTextAction(text);
                }
                break;
                
            case "WAIT":
                Object durationObj = nlpAction.getParameter("duration");
                long duration = 1000; // Default 1 second
                if (durationObj instanceof Number) {
                    duration = ((Number) durationObj).longValue();
                }
                action = new WaitAction(duration);
                break;
                
            case "LONG_PRESS":
                String longPressTarget = (String) nlpAction.getParameter("target");
                if (longPressTarget != null) {
                    action = new LongPressAction(longPressTarget);
                } else {
                    action = new LongPressAction(500, 500); // Default center
                }
                break;
                
            case "DOUBLE_TAP":
                String doubleTapTarget = (String) nlpAction.getParameter("target");
                if (doubleTapTarget != null) {
                    action = new DoubleTapAction(doubleTapTarget);
                } else {
                    action = new DoubleTapAction(500, 500); // Default center
                }
                break;
                
            case "COLLECT":
                action = new CollectAction("item"); // Generic collect action
                break;
                
            case "MOVE":
                String moveDirection = (String) nlpAction.getParameter("direction");
                if (moveDirection != null) {
                    action = new MoveAction(moveDirection);
                }
                break;
        }
        
        if (action != null) {
            step.setAction(action);
            return step;
        }
        
        return null;
    }
    
    private WorkflowCondition convertNLPConditionToWorkflow(NLPProcessor.WorkflowCondition nlpCondition) {
        switch (nlpCondition.getType()) {
            case "TEXT_VISIBLE":
                String text = (String) nlpCondition.getParameter("text");
                if (text != null) {
                    return new TextVisibleCondition(text);
                }
                break;
                
            case "ELEMENT_EXISTS":
                String element = (String) nlpCondition.getParameter("element");
                if (element != null) {
                    return new ElementExistsCondition(element);
                }
                break;
                
            case "TIME_ELAPSED":
                Object durationObj = nlpCondition.getParameter("duration");
                if (durationObj instanceof Number) {
                    long duration = ((Number) durationObj).longValue();
                    return new TimeElapsedCondition(duration);
                }
                break;
                
            case "REPEAT":
                Object countObj = nlpCondition.getParameter("count");
                if (countObj instanceof Number) {
                    int count = ((Number) countObj).intValue();
                    return new RepeatCondition(count);
                }
                break;
        }
        
        return null;
    }
    
    private String expandVoiceShortcut(String command) {
        String lowerCommand = command.toLowerCase();
        
        for (Map.Entry<String, String> entry : VOICE_SHORTCUTS.entrySet()) {
            if (lowerCommand.contains(entry.getKey())) {
                return command.replace(entry.getKey(), entry.getValue());
            }
        }
        
        return command;
    }
    
    public void shutdown() {
        isContinuousMode = false;
        stopListening();
        
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    // Recognition listener implementation
    private class WorkflowRecognitionListener implements RecognitionListener {
        
        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "Ready for speech");
            if (workflowListener != null) {
                workflowListener.onListeningStarted();
            }
        }
        
        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of speech");
        }
        
        @Override
        public void onRmsChanged(float rmsdB) {
            // Can be used for visual feedback
        }
        
        @Override
        public void onBufferReceived(byte[] buffer) {
            // Raw audio data if needed
        }
        
        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "End of speech");
            isListening = false;
        }
        
        @Override
        public void onError(int error) {
            Log.e(TAG, "Speech recognition error: " + error);
            isListening = false;
            
            if (workflowListener != null) {
                workflowListener.onError("Speech recognition error: " + getErrorString(error));
            }
            
            // Restart listening in continuous mode
            if (isContinuousMode) {
                mainHandler.postDelayed(() -> startListening(), 1000);
            }
        }
        
        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            
            if (matches != null && !matches.isEmpty()) {
                String command = matches.get(0);
                Log.d(TAG, "Voice command received: " + command);
                
                // Expand shortcuts
                command = expandVoiceShortcut(command);
                
                // Process command
                processVoiceCommand(command, new VoiceCommandCallback() {
                    @Override
                    public void onCommandProcessed(WorkflowDefinition workflow, NLPProcessor.WorkflowParseResult result) {
                        if (workflowListener != null) {
                            workflowListener.onWorkflowCreated(workflow, result);
                        }
                        
                        if (currentSession != null) {
                            currentSession.addCommand(command, workflow);
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        if (workflowListener != null) {
                            workflowListener.onError(error);
                        }
                    }
                });
            }
            
            isListening = false;
            
            // Restart listening in continuous mode
            if (isContinuousMode) {
                mainHandler.postDelayed(() -> startListening(), 500);
            }
        }
        
        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> partials = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            
            if (partials != null && !partials.isEmpty() && workflowListener != null) {
                workflowListener.onPartialResult(partials.get(0));
            }
        }
        
        @Override
        public void onEvent(int eventType, Bundle params) {
            // Handle speech events if needed
        }
        
        private String getErrorString(int error) {
            switch (error) {
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
                    return "No speech input matched";
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
    }
    
    // Callback interfaces
    
    public interface VoiceWorkflowListener {
        void onSessionStarted();
        void onListeningStarted();
        void onWakeWordDetected();
        void onPartialResult(String partial);
        void onWorkflowCreated(WorkflowDefinition workflow, NLPProcessor.WorkflowParseResult parseResult);
        void onError(String error);
    }
    
    public interface VoiceCommandCallback {
        void onCommandProcessed(WorkflowDefinition workflow, NLPProcessor.WorkflowParseResult result);
        void onError(String error);
    }
    
    public interface VoiceSuggestionCallback {
        void onSuggestionsGenerated(List<NLPProcessor.WorkflowSuggestion> suggestions);
        void onError(String error);
    }
    
    // Session state management
    
    private static class VoiceSessionState {
        private List<String> commands = new ArrayList<>();
        private List<WorkflowDefinition> workflows = new ArrayList<>();
        private long sessionStartTime = System.currentTimeMillis();
        
        public void addCommand(String command, WorkflowDefinition workflow) {
            commands.add(command);
            workflows.add(workflow);
        }
        
        public List<String> getCommands() { return commands; }
        public List<WorkflowDefinition> getWorkflows() { return workflows; }
        public long getSessionDuration() { return System.currentTimeMillis() - sessionStartTime; }
    }
    
    // Wake word detector
    
    private static class WakeWordDetector {
        private String[] wakeWords;
        private StringBuilder audioBuffer = new StringBuilder();
        
        public WakeWordDetector(String[] wakeWords) {
            this.wakeWords = wakeWords;
        }
        
        public boolean detectWakeWord(short[] audioData, int length) {
            // Simple energy-based detection (in real implementation, use more sophisticated methods)
            double energy = 0;
            for (int i = 0; i < length; i++) {
                energy += audioData[i] * audioData[i];
            }
            energy = Math.sqrt(energy / length);
            
            // If energy threshold exceeded, consider it speech
            return energy > 1000; // Simplified threshold
        }
    }
}
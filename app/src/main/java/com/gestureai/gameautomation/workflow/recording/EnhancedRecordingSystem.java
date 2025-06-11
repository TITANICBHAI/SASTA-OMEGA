package com.gestureai.gameautomation.workflow.recording;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.gestureai.gameautomation.accessibility.GameAutomationAccessibilityService;
import com.gestureai.gameautomation.core.detection.GameObjectDetectionEngine;
import com.gestureai.gameautomation.utils.NLPProcessor;
import com.gestureai.gameautomation.workflow.WorkflowDefinition;
import com.gestureai.gameautomation.workflow.WorkflowStep;
import com.gestureai.gameautomation.workflow.actions.*;
import com.gestureai.gameautomation.workflow.conditions.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enhanced recording system with NLP-powered intelligent recording modes
 */
public class EnhancedRecordingSystem {
    private static final String TAG = "EnhancedRecordingSystem";
    
    private Context context;
    private NLPProcessor nlpProcessor;
    private GameObjectDetectionEngine detectionEngine;
    private GameAutomationAccessibilityService accessibilityService;
    private ExecutorService executorService;
    private Handler mainHandler;
    
    // Recording state
    private boolean isRecording = false;
    private RecordingMode currentMode = RecordingMode.STANDARD;
    private String currentSessionName;
    private long recordingStartTime;
    private EnhancedRecordingListener listener;
    
    // Recording data
    private Queue<RecordedAction> actionQueue;
    private List<RecordedSequence> completedSequences;
    private Map<String, Object> contextData;
    private GameStateTracker stateTracker;
    
    // NLP-powered features
    private SemanticActionGrouper actionGrouper;
    private ContextualConditionGenerator conditionGenerator;
    private IntelligentTimingAnalyzer timingAnalyzer;
    private PatternRecognitionEngine patternEngine;
    
    // Recording modes
    public enum RecordingMode {
        STANDARD,           // Basic action recording
        SEMANTIC,           // NLP-enhanced with semantic understanding
        CONTEXTUAL,         // Context-aware with game state tracking
        INTELLIGENT,        // Full AI-powered analysis and optimization
        VOICE_GUIDED        // Voice commands during recording
    }
    
    public interface EnhancedRecordingListener {
        void onRecordingStarted(String sessionName, RecordingMode mode);
        void onRecordingStopped(RecordedSequence sequence);
        void onActionRecorded(RecordedAction action);
        void onPatternDetected(ActionPattern pattern);
        void onSuggestionGenerated(RecordingSuggestion suggestion);
        void onContextChange(GameContext context);
        void onError(String error);
    }
    
    public EnhancedRecordingSystem(Context context, NLPProcessor nlpProcessor, 
                                 GameObjectDetectionEngine detectionEngine) {
        this.context = context;
        this.nlpProcessor = nlpProcessor;
        this.detectionEngine = detectionEngine;
        this.executorService = Executors.newFixedThreadPool(3);
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        initializeRecordingComponents();
    }
    
    private void initializeRecordingComponents() {
        actionQueue = new ConcurrentLinkedQueue<>();
        completedSequences = new ArrayList<>();
        contextData = new HashMap<>();
        
        stateTracker = new GameStateTracker();
        actionGrouper = new SemanticActionGrouper(nlpProcessor);
        conditionGenerator = new ContextualConditionGenerator(nlpProcessor);
        timingAnalyzer = new IntelligentTimingAnalyzer();
        patternEngine = new PatternRecognitionEngine();
        
        Log.d(TAG, "Enhanced recording system initialized");
    }
    
    /**
     * Start recording with specified mode
     */
    public void startRecording(String sessionName, RecordingMode mode) {
        if (isRecording) {
            Log.w(TAG, "Recording already in progress");
            return;
        }
        
        try {
            this.currentSessionName = sessionName;
            this.currentMode = mode;
            this.recordingStartTime = System.currentTimeMillis();
            this.isRecording = true;
            
            // Clear previous data
            actionQueue.clear();
            contextData.clear();
            
            // Initialize mode-specific components
            initializeModeSpecificFeatures(mode);
            
            // Start recording processes
            startAccessibilityRecording();
            startGameStateTracking();
            
            if (mode == RecordingMode.VOICE_GUIDED) {
                startVoiceGuidedRecording();
            }
            
            if (listener != null) {
                listener.onRecordingStarted(sessionName, mode);
            }
            
            Log.d(TAG, "Started recording: " + sessionName + " in mode: " + mode);
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording", e);
            if (listener != null) {
                listener.onError("Failed to start recording: " + e.getMessage());
            }
        }
    }
    
    /**
     * Stop recording and process results
     */
    public RecordedSequence stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "No recording in progress");
            return null;
        }
        
        try {
            isRecording = false;
            long recordingDuration = System.currentTimeMillis() - recordingStartTime;
            
            // Process recorded actions
            List<RecordedAction> actions = new ArrayList<>(actionQueue);
            
            // Apply mode-specific processing
            actions = applyModeSpecificProcessing(actions, currentMode);
            
            // Create recorded sequence
            RecordedSequence sequence = new RecordedSequence(
                currentSessionName,
                actions,
                recordingStartTime,
                recordingDuration,
                currentMode,
                new HashMap<>(contextData)
            );
            
            // Enhance sequence with NLP analysis
            enhanceSequenceWithNLP(sequence);
            
            // Store completed sequence
            completedSequences.add(sequence);
            
            if (listener != null) {
                listener.onRecordingStopped(sequence);
            }
            
            Log.d(TAG, "Stopped recording: " + actions.size() + " actions in " + recordingDuration + "ms");
            
            return sequence;
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
            if (listener != null) {
                listener.onError("Failed to stop recording: " + e.getMessage());
            }
            return null;
        }
    }
    
    /**
     * Record accessibility event
     */
    public void recordAccessibilityEvent(AccessibilityEvent event) {
        if (!isRecording) return;
        
        executorService.execute(() -> {
            try {
                RecordedAction action = createActionFromAccessibilityEvent(event);
                if (action != null) {
                    // Enrich action with context
                    enrichActionWithContext(action);
                    
                    actionQueue.offer(action);
                    
                    // Real-time analysis
                    performRealTimeAnalysis(action);
                    
                    if (listener != null) {
                        mainHandler.post(() -> listener.onActionRecorded(action));
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error recording accessibility event", e);
            }
        });
    }
    
    /**
     * Record touch action
     */
    public void recordTouchAction(String actionType, float x, float y, long timestamp) {
        if (!isRecording) return;
        
        executorService.execute(() -> {
            try {
                RecordedAction action = new RecordedAction(
                    actionType, x, y, timestamp, getCurrentGameContext()
                );
                
                enrichActionWithContext(action);
                actionQueue.offer(action);
                
                performRealTimeAnalysis(action);
                
                if (listener != null) {
                    mainHandler.post(() -> listener.onActionRecorded(action));
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error recording touch action", e);
            }
        });
    }
    
    /**
     * Add voice annotation to current recording
     */
    public void addVoiceAnnotation(String annotation) {
        if (!isRecording || nlpProcessor == null) return;
        
        executorService.execute(() -> {
            try {
                // Parse voice annotation using NLP
                NLPProcessor.WorkflowParseResult parsed = nlpProcessor.parseVoiceCommand(annotation);
                
                // Create annotation action
                RecordedAction annotationAction = new RecordedAction(
                    "VOICE_ANNOTATION",
                    0, 0,
                    System.currentTimeMillis(),
                    getCurrentGameContext()
                );
                
                annotationAction.addMetadata("annotation", annotation);
                annotationAction.addMetadata("nlp_result", parsed);
                
                actionQueue.offer(annotationAction);
                
                if (listener != null) {
                    mainHandler.post(() -> listener.onActionRecorded(annotationAction));
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error adding voice annotation", e);
            }
        });
    }
    
    /**
     * Generate workflow from recorded sequence
     */
    public WorkflowDefinition generateWorkflowFromSequence(RecordedSequence sequence) {
        try {
            WorkflowDefinition workflow = new WorkflowDefinition(
                sequence.getName() + "_Workflow",
                "Generated from recording session: " + sequence.getName()
            );
            
            List<RecordedAction> actions = sequence.getActions();
            
            // Group related actions
            List<ActionGroup> groups = actionGrouper.groupActions(actions);
            
            // Convert groups to workflow steps
            for (ActionGroup group : groups) {
                WorkflowStep step = createStepFromActionGroup(group);
                if (step != null) {
                    // Add intelligent conditions
                    WorkflowCondition condition = conditionGenerator.generateCondition(group, sequence);
                    if (condition != null) {
                        step.setCondition(condition);
                    }
                    
                    // Set intelligent timing
                    long waitTime = timingAnalyzer.calculateOptimalWaitTime(group, actions);
                    if (waitTime > 0) {
                        step.setWaitTime(waitTime);
                    }
                    
                    workflow.addStep(step);
                }
            }
            
            Log.d(TAG, "Generated workflow with " + workflow.getSteps().size() + " steps");
            return workflow;
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating workflow from sequence", e);
            return null;
        }
    }
    
    /**
     * Get recording suggestions based on current sequence
     */
    public List<RecordingSuggestion> getRecordingSuggestions() {
        List<RecordingSuggestion> suggestions = new ArrayList<>();
        
        try {
            if (!isRecording || actionQueue.isEmpty()) {
                return suggestions;
            }
            
            List<RecordedAction> currentActions = new ArrayList<>(actionQueue);
            
            // Pattern-based suggestions
            suggestions.addAll(patternEngine.generateSuggestions(currentActions));
            
            // NLP-based suggestions
            if (nlpProcessor != null) {
                List<String> actionTypes = new ArrayList<>();
                for (RecordedAction action : currentActions) {
                    actionTypes.add(action.getType());
                }
                
                List<NLPProcessor.WorkflowSuggestion> nlpSuggestions = 
                    nlpProcessor.generateWorkflowSuggestions("generic", actionTypes);
                
                for (NLPProcessor.WorkflowSuggestion nlpSugg : nlpSuggestions) {
                    suggestions.add(new RecordingSuggestion(
                        nlpSugg.getName(),
                        nlpSugg.getDescription(),
                        RecordingSuggestion.SuggestionType.OPTIMIZATION,
                        nlpSugg.getRelevanceScore()
                    ));
                }
            }
            
            // Context-based suggestions
            suggestions.addAll(generateContextBasedSuggestions(currentActions));
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating recording suggestions", e);
        }
        
        return suggestions;
    }
    
    private void initializeModeSpecificFeatures(RecordingMode mode) {
        switch (mode) {
            case SEMANTIC:
                actionGrouper.enableSemanticGrouping(true);
                break;
            case CONTEXTUAL:
                stateTracker.enableAdvancedTracking(true);
                conditionGenerator.enableContextualGeneration(true);
                break;
            case INTELLIGENT:
                actionGrouper.enableSemanticGrouping(true);
                stateTracker.enableAdvancedTracking(true);
                conditionGenerator.enableContextualGeneration(true);
                timingAnalyzer.enableIntelligentTiming(true);
                patternEngine.enableRealTimeDetection(true);
                break;
            case VOICE_GUIDED:
                // Voice guidance features enabled separately
                break;
        }
    }
    
    private List<RecordedAction> applyModeSpecificProcessing(List<RecordedAction> actions, RecordingMode mode) {
        switch (mode) {
            case SEMANTIC:
                return actionGrouper.optimizeActionSequence(actions);
            case CONTEXTUAL:
                return enhanceActionsWithContext(actions);
            case INTELLIGENT:
                actions = actionGrouper.optimizeActionSequence(actions);
                actions = enhanceActionsWithContext(actions);
                actions = timingAnalyzer.optimizeActionTiming(actions);
                return actions;
            default:
                return actions;
        }
    }
    
    private void enhanceSequenceWithNLP(RecordedSequence sequence) {
        if (nlpProcessor == null) return;
        
        try {
            // Analyze sequence patterns
            List<ActionPattern> patterns = patternEngine.analyzeSequence(sequence);
            sequence.setDetectedPatterns(patterns);
            
            // Generate natural language description
            String description = generateSequenceDescription(sequence);
            sequence.setNaturalLanguageDescription(description);
            
            // Add optimization suggestions
            List<RecordingSuggestion> suggestions = generateOptimizationSuggestions(sequence);
            sequence.setOptimizationSuggestions(suggestions);
            
        } catch (Exception e) {
            Log.e(TAG, "Error enhancing sequence with NLP", e);
        }
    }
    
    private RecordedAction createActionFromAccessibilityEvent(AccessibilityEvent event) {
        try {
            String actionType = mapAccessibilityEventToActionType(event);
            if (actionType == null) return null;
            
            AccessibilityNodeInfo source = event.getSource();
            if (source == null) return null;
            
            android.graphics.Rect bounds = new android.graphics.Rect();
            source.getBoundsInScreen(bounds);
            
            float x = bounds.centerX();
            float y = bounds.centerY();
            
            RecordedAction action = new RecordedAction(
                actionType, x, y, event.getEventTime(), getCurrentGameContext()
            );
            
            // Add accessibility-specific metadata
            action.addMetadata("package_name", event.getPackageName());
            action.addMetadata("class_name", event.getClassName());
            action.addMetadata("content_description", event.getContentDescription());
            action.addMetadata("text", event.getText());
            
            return action;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating action from accessibility event", e);
            return null;
        }
    }
    
    private String mapAccessibilityEventToActionType(AccessibilityEvent event) {
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                return "TAP";
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                return "LONG_PRESS";
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                return "SWIPE";
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                return "TYPE";
            default:
                return null;
        }
    }
    
    private void enrichActionWithContext(RecordedAction action) {
        try {
            // Add game state context
            GameContext context = getCurrentGameContext();
            action.setGameContext(context);
            
            // Add screen content information if available
            if (detectionEngine != null) {
                List<String> detectedObjects = detectionEngine.getVisibleObjectLabels();
                action.addMetadata("detected_objects", detectedObjects);
            }
            
            // Add timing context
            if (!actionQueue.isEmpty()) {
                RecordedAction lastAction = ((LinkedList<RecordedAction>) actionQueue).peekLast();
                if (lastAction != null) {
                    long timeDiff = action.getTimestamp() - lastAction.getTimestamp();
                    action.addMetadata("time_since_last_action", timeDiff);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error enriching action with context", e);
        }
    }
    
    private void performRealTimeAnalysis(RecordedAction action) {
        try {
            // Detect patterns in real-time
            if (patternEngine.isRealTimeDetectionEnabled()) {
                List<RecordedAction> recentActions = getRecentActions(10);
                ActionPattern pattern = patternEngine.detectPattern(recentActions);
                
                if (pattern != null && listener != null) {
                    mainHandler.post(() -> listener.onPatternDetected(pattern));
                }
            }
            
            // Generate suggestions
            if (actionQueue.size() % 5 == 0) { // Every 5 actions
                List<RecordingSuggestion> suggestions = getRecordingSuggestions();
                if (!suggestions.isEmpty() && listener != null) {
                    mainHandler.post(() -> listener.onSuggestionGenerated(suggestions.get(0)));
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in real-time analysis", e);
        }
    }
    
    private GameContext getCurrentGameContext() {
        return stateTracker.getCurrentContext();
    }
    
    private List<RecordedAction> getRecentActions(int count) {
        List<RecordedAction> recent = new ArrayList<>();
        Iterator<RecordedAction> iterator = ((LinkedList<RecordedAction>) actionQueue).descendingIterator();
        
        int added = 0;
        while (iterator.hasNext() && added < count) {
            recent.add(0, iterator.next()); // Add to beginning to maintain order
            added++;
        }
        
        return recent;
    }
    
    private WorkflowStep createStepFromActionGroup(ActionGroup group) {
        try {
            RecordedAction primaryAction = group.getPrimaryAction();
            WorkflowStep step = new WorkflowStep(primaryAction.getType());
            
            // Create appropriate action based on type
            WorkflowAction action = createWorkflowActionFromRecorded(primaryAction);
            if (action != null) {
                step.setAction(action);
            }
            
            return step;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating step from action group", e);
            return null;
        }
    }
    
    private WorkflowAction createWorkflowActionFromRecorded(RecordedAction recorded) {
        switch (recorded.getType()) {
            case "TAP":
                return new TapAction((int)recorded.getX(), (int)recorded.getY());
            case "SWIPE":
                // Determine swipe direction from metadata or context
                String direction = (String) recorded.getMetadata().getOrDefault("direction", "up");
                return new SwipeAction(direction);
            case "TYPE":
                String text = (String) recorded.getMetadata().getOrDefault("text", "");
                return new TypeTextAction(text);
            case "LONG_PRESS":
                return new LongPressAction((int)recorded.getX(), (int)recorded.getY());
            case "DOUBLE_TAP":
                return new DoubleTapAction((int)recorded.getX(), (int)recorded.getY());
            default:
                return null;
        }
    }
    
    private List<RecordedAction> enhanceActionsWithContext(List<RecordedAction> actions) {
        // Add contextual information to actions
        for (RecordedAction action : actions) {
            // This could involve analyzing game state, screen content, etc.
            enrichActionWithContext(action);
        }
        return actions;
    }
    
    private String generateSequenceDescription(RecordedSequence sequence) {
        StringBuilder description = new StringBuilder();
        
        List<RecordedAction> actions = sequence.getActions();
        Map<String, Integer> actionCounts = new HashMap<>();
        
        // Count action types
        for (RecordedAction action : actions) {
            actionCounts.merge(action.getType(), 1, Integer::sum);
        }
        
        // Generate description
        description.append("Recorded sequence containing ");
        for (Map.Entry<String, Integer> entry : actionCounts.entrySet()) {
            description.append(entry.getValue()).append(" ").append(entry.getKey().toLowerCase()).append(" actions, ");
        }
        
        if (description.length() > 2) {
            description.setLength(description.length() - 2); // Remove trailing comma
        }
        
        return description.toString();
    }
    
    private List<RecordingSuggestion> generateOptimizationSuggestions(RecordedSequence sequence) {
        List<RecordingSuggestion> suggestions = new ArrayList<>();
        
        // Analyze for optimization opportunities
        List<RecordedAction> actions = sequence.getActions();
        
        // Suggest consolidation of similar actions
        if (hasConsecutiveSimilarActions(actions)) {
            suggestions.add(new RecordingSuggestion(
                "Consolidate Similar Actions",
                "Multiple similar actions detected that could be combined",
                RecordingSuggestion.SuggestionType.OPTIMIZATION,
                0.8f
            ));
        }
        
        // Suggest timing optimization
        if (hasSuboptimalTiming(actions)) {
            suggestions.add(new RecordingSuggestion(
                "Optimize Timing",
                "Action timing could be improved for better reliability",
                RecordingSuggestion.SuggestionType.TIMING,
                0.7f
            ));
        }
        
        return suggestions;
    }
    
    private List<RecordingSuggestion> generateContextBasedSuggestions(List<RecordedAction> actions) {
        List<RecordingSuggestion> suggestions = new ArrayList<>();
        
        // Analyze current context and actions to suggest improvements
        GameContext context = getCurrentGameContext();
        
        if (context != null) {
            // Game-specific suggestions based on context
            switch (context.getGameType()) {
                case "action":
                    if (hasFrequentTaps(actions)) {
                        suggestions.add(new RecordingSuggestion(
                            "Auto-Attack Pattern",
                            "Consider creating an auto-attack sequence",
                            RecordingSuggestion.SuggestionType.PATTERN,
                            0.9f
                        ));
                    }
                    break;
                case "puzzle":
                    if (hasRepeatedSequences(actions)) {
                        suggestions.add(new RecordingSuggestion(
                            "Pattern Recognition",
                            "Repeated move pattern detected",
                            RecordingSuggestion.SuggestionType.PATTERN,
                            0.8f
                        ));
                    }
                    break;
            }
        }
        
        return suggestions;
    }
    
    private boolean hasConsecutiveSimilarActions(List<RecordedAction> actions) {
        for (int i = 0; i < actions.size() - 1; i++) {
            RecordedAction current = actions.get(i);
            RecordedAction next = actions.get(i + 1);
            
            if (current.getType().equals(next.getType()) &&
                Math.abs(current.getX() - next.getX()) < 50 &&
                Math.abs(current.getY() - next.getY()) < 50) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasSuboptimalTiming(List<RecordedAction> actions) {
        for (int i = 0; i < actions.size() - 1; i++) {
            long timeDiff = actions.get(i + 1).getTimestamp() - actions.get(i).getTimestamp();
            if (timeDiff > 5000) { // More than 5 seconds between actions
                return true;
            }
        }
        return false;
    }
    
    private boolean hasFrequentTaps(List<RecordedAction> actions) {
        int tapCount = 0;
        for (RecordedAction action : actions) {
            if ("TAP".equals(action.getType())) {
                tapCount++;
            }
        }
        return tapCount > actions.size() * 0.7; // More than 70% taps
    }
    
    private boolean hasRepeatedSequences(List<RecordedAction> actions) {
        // Simple pattern detection for repeated sequences
        if (actions.size() < 6) return false;
        
        // Check for repeated 3-action sequences
        for (int i = 0; i <= actions.size() - 6; i++) {
            boolean isRepeated = true;
            for (int j = 0; j < 3; j++) {
                if (!actions.get(i + j).getType().equals(actions.get(i + j + 3).getType())) {
                    isRepeated = false;
                    break;
                }
            }
            if (isRepeated) return true;
        }
        
        return false;
    }
    
    private void startAccessibilityRecording() {
        // Register with accessibility service for events
        if (accessibilityService != null) {
            accessibilityService.setRecordingMode(true);
        }
    }
    
    private void startGameStateTracking() {
        stateTracker.startTracking();
    }
    
    private void startVoiceGuidedRecording() {
        // Initialize voice guidance features
        // This would integrate with the VoiceWorkflowProcessor
    }
    
    // Public interface methods
    
    public void setListener(EnhancedRecordingListener listener) {
        this.listener = listener;
    }
    
    public void setAccessibilityService(GameAutomationAccessibilityService service) {
        this.accessibilityService = service;
    }
    
    public boolean isRecording() {
        return isRecording;
    }
    
    public RecordingMode getCurrentMode() {
        return currentMode;
    }
    
    public List<RecordedSequence> getCompletedSequences() {
        return new ArrayList<>(completedSequences);
    }
    
    public void clearCompletedSequences() {
        completedSequences.clear();
    }
    
    public void shutdown() {
        isRecording = false;
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        if (stateTracker != null) {
            stateTracker.stopTracking();
        }
    }
}
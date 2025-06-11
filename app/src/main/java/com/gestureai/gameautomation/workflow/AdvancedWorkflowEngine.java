package com.gestureai.gameautomation.workflow;

import android.content.Context;
import android.util.Log;
import com.gestureai.gameautomation.workflow.actions.*;
import com.gestureai.gameautomation.workflow.conditions.*;
import com.gestureai.gameautomation.workflow.recording.ActionRecorder;
import com.gestureai.gameautomation.workflow.voice.VoiceWorkflowProcessor;
import com.gestureai.gameautomation.utils.NLPProcessor;
import com.gestureai.gameautomation.utils.TensorFlowLiteHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

/**
 * Advanced Workflow Engine with NLP, Voice Commands, Recording Mode, and Visual Builder
 */
public class AdvancedWorkflowEngine extends WorkflowEngine {
    private static final String TAG = "AdvancedWorkflowEngine";
    private static volatile AdvancedWorkflowEngine instance;
    
    private ActionRecorder actionRecorder;
    private VoiceWorkflowProcessor voiceProcessor;
    private NLPProcessor nlpProcessor;
    private WorkflowBuilder visualBuilder;
    
    // Workflow storage - Thread-safe collections
    private final Map<String, WorkflowTemplate> workflowTemplates;
    private final Map<String, RecordedSequence> recordedSequences;
    private WorkflowConfigManager configManager;
    
    // Voice and NLP state - Thread-safe
    private volatile boolean voiceListeningEnabled = false;
    private volatile boolean recordingMode = false;
    private volatile String currentRecordingSession;
    private volatile boolean isDestroyed = false;
    private final Object creationLock = new Object();
    
    public static AdvancedWorkflowEngine getInstance(Context context) {
        if (instance == null) {
            synchronized (AdvancedWorkflowEngine.class) {
                if (instance == null) {
                    instance = new AdvancedWorkflowEngine(context);
                }
            }
        }
        return instance;
    }
    
    private AdvancedWorkflowEngine(Context context) {
        super();
        this.context = context.getApplicationContext();
        
        // Initialize thread-safe collections
        this.workflowTemplates = new ConcurrentHashMap<>();
        this.recordedSequences = new ConcurrentHashMap<>();
        
        initializeAdvancedComponents();
        loadWorkflowTemplates();
        
        Log.d(TAG, "AdvancedWorkflowEngine initialized");
    }
    
    private void initializeAdvancedComponents() {
        try {
            // Initialize NLP processor with MobileBERT
            nlpProcessor = new NLPProcessor(context);
            
            // Initialize voice processor
            voiceProcessor = new VoiceWorkflowProcessor(context, nlpProcessor);
            
            // Initialize action recorder
            actionRecorder = new ActionRecorder(context);
            
            // Initialize visual workflow builder
            visualBuilder = new WorkflowBuilder(context);
            
            // Initialize configuration manager
            configManager = new WorkflowConfigManager(context);
            
            // Storage already initialized in constructor to prevent race conditions
            
            Log.d(TAG, "Advanced workflow components initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing advanced components", e);
        }
    }
    
    /**
     * 1. CODE-BASED WORKFLOWS: Create workflow from Java class definition
     */
    public void createCodeBasedWorkflow(Class<? extends CodeBasedWorkflow> workflowClass) {
        try {
            CodeBasedWorkflow workflow = workflowClass.newInstance();
            WorkflowDefinition definition = workflow.buildWorkflow();
            
            // Register the workflow
            registerWorkflow(definition.getName(), definition);
            
            Log.d(TAG, "Code-based workflow created: " + definition.getName());
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating code-based workflow", e);
        }
    }
    
    /**
     * 2. CONFIGURATION FILES: Load workflow from JSON/XML
     */
    public void loadWorkflowFromConfig(String configPath, ConfigType type) {
        try {
            String content = loadConfigFile(configPath);
            WorkflowDefinition workflow;
            
            switch (type) {
                case JSON:
                    workflow = parseWorkflowFromJson(new JSONObject(content));
                    break;
                case XML:
                    workflow = parseWorkflowFromXml(content);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported config type: " + type);
            }
            
            registerWorkflow(workflow.getName(), workflow);
            Log.d(TAG, "Workflow loaded from config: " + workflow.getName());
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading workflow from config", e);
        }
    }
    
    /**
     * 3. VISUAL WORKFLOW BUILDER: Create workflow through drag-and-drop UI
     */
    public WorkflowBuilder getVisualBuilder() {
        return visualBuilder;
    }
    
    public void createVisualWorkflow(String name, List<VisualWorkflowNode> nodes) {
        try {
            WorkflowDefinition workflow = visualBuilder.buildWorkflowFromNodes(name, nodes);
            registerWorkflow(name, workflow);
            
            Log.d(TAG, "Visual workflow created: " + name);
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating visual workflow", e);
        }
    }
    
    /**
     * 4. VOICE COMMANDS: Create workflow from voice input
     */
    public void enableVoiceWorkflowCreation(boolean enabled) {
        voiceListeningEnabled = enabled;
        
        if (enabled) {
            voiceProcessor.startListening(new VoiceWorkflowProcessor.VoiceCallback() {
                @Override
                public void onVoiceCommand(String command) {
                    processVoiceWorkflowCommand(command);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Voice command error: " + error);
                }
            });
        } else {
            voiceProcessor.stopListening();
        }
        
        Log.d(TAG, "Voice workflow creation " + (enabled ? "enabled" : "disabled"));
    }
    
    private void processVoiceWorkflowCommand(String command) {
        try {
            // Use NLP to parse voice command into workflow steps
            WorkflowNLPProcessor.ParsedCommand parsed = nlpProcessor.parseVoiceCommand(command);
            
            if (parsed.isWorkflowCreation()) {
                WorkflowDefinition workflow = nlpProcessor.buildWorkflowFromNLP(parsed);
                registerWorkflow(parsed.getWorkflowName(), workflow);
                
                Log.d(TAG, "Workflow created from voice: " + parsed.getWorkflowName());
            } else if (parsed.isWorkflowExecution()) {
                executeWorkflow(parsed.getWorkflowName());
                Log.d(TAG, "Executing workflow from voice: " + parsed.getWorkflowName());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing voice command", e);
        }
    }
    
    /**
     * 5. RECORDING MODE: Record user actions and convert to workflow
     */
    public void startRecordingMode(String sessionName) {
        if (recordingMode) {
            stopRecordingMode();
        }
        
        recordingMode = true;
        currentRecordingSession = sessionName;
        
        actionRecorder.startRecording(sessionName, new ActionRecorder.RecordingCallback() {
            @Override
            public void onActionRecorded(RecordedAction action) {
                Log.d(TAG, "Action recorded: " + action.getType());
            }
            
            @Override
            public void onRecordingComplete(List<RecordedAction> actions) {
                processRecordedActions(sessionName, actions);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Recording error: " + error);
                recordingMode = false;
            }
        });
        
        Log.d(TAG, "Recording mode started: " + sessionName);
    }
    
    public void stopRecordingMode() {
        if (recordingMode) {
            actionRecorder.stopRecording();
            recordingMode = false;
            currentRecordingSession = null;
            Log.d(TAG, "Recording mode stopped");
        }
    }
    
    private void processRecordedActions(String sessionName, List<RecordedAction> actions) {
        try {
            // Use NLP to add conditions and logic to recorded actions
            WorkflowDefinition workflow = nlpProcessor.enhanceRecordedActions(sessionName, actions);
            
            // Store as recorded sequence
            RecordedSequence sequence = new RecordedSequence(sessionName, actions, workflow);
            recordedSequences.put(sessionName, sequence);
            
            // Register as executable workflow
            registerWorkflow(sessionName, workflow);
            
            Log.d(TAG, "Recorded sequence processed: " + sessionName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing recorded actions", e);
        }
    }
    
    /**
     * NLP-ENHANCED WORKFLOW CREATION
     */
    public void createWorkflowFromNaturalLanguage(String description) {
        try {
            WorkflowNLPProcessor.ParsedCommand parsed = nlpProcessor.parseNaturalLanguage(description);
            WorkflowDefinition workflow = nlpProcessor.buildWorkflowFromNLP(parsed);
            
            registerWorkflow(parsed.getWorkflowName(), workflow);
            
            Log.d(TAG, "Workflow created from NLP: " + parsed.getWorkflowName());
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating workflow from NLP", e);
        }
    }
    
    /**
     * WORKFLOW TEMPLATE SYSTEM
     */
    public void createWorkflowTemplate(String name, WorkflowDefinition workflow, Map<String, String> parameters) {
        WorkflowTemplate template = new WorkflowTemplate(name, workflow, parameters);
        workflowTemplates.put(name, template);
        
        // Save to persistent storage
        configManager.saveWorkflowTemplate(template);
        
        Log.d(TAG, "Workflow template created: " + name);
    }
    
    public WorkflowDefinition instantiateTemplate(String templateName, Map<String, String> values) {
        WorkflowTemplate template = workflowTemplates.get(templateName);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateName);
        }
        
        return template.instantiate(values);
    }
    
    /**
     * WORKFLOW SHARING AND EXPORT
     */
    public String exportWorkflowToJson(String workflowName) {
        WorkflowDefinition workflow = getWorkflow(workflowName);
        if (workflow == null) {
            throw new IllegalArgumentException("Workflow not found: " + workflowName);
        }
        
        return configManager.exportWorkflowToJson(workflow);
    }
    
    public void importWorkflowFromJson(String jsonContent) {
        try {
            WorkflowDefinition workflow = configManager.importWorkflowFromJson(jsonContent);
            registerWorkflow(workflow.getName(), workflow);
            
            Log.d(TAG, "Workflow imported: " + workflow.getName());
            
        } catch (Exception e) {
            Log.e(TAG, "Error importing workflow", e);
        }
    }
    
    /**
     * INTELLIGENT WORKFLOW SUGGESTIONS
     */
    public List<WorkflowSuggestion> getSuggestedWorkflows(String gameType, String userBehavior) {
        try {
            return nlpProcessor.generateWorkflowSuggestions(gameType, userBehavior, recordedSequences.values());
        } catch (Exception e) {
            Log.e(TAG, "Error generating workflow suggestions", e);
            return new ArrayList<>();
        }
    }
    
    // Helper methods
    private void loadWorkflowTemplates() {
        try {
            Map<String, WorkflowTemplate> templates = configManager.loadWorkflowTemplates();
            workflowTemplates.putAll(templates);
            
            Log.d(TAG, "Loaded " + templates.size() + " workflow templates");
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading workflow templates", e);
        }
    }
    
    private String loadConfigFile(String path) throws IOException {
        StringBuilder content = new StringBuilder();
        try (InputStream is = context.getAssets().open(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    private WorkflowDefinition parseWorkflowFromXml(String xmlContent) {
        // XML parsing implementation
        // This would use Android's XML parser to create WorkflowDefinition
        throw new UnsupportedOperationException("XML parsing not yet implemented");
    }
    
    private void registerWorkflow(String name, WorkflowDefinition workflow) {
        workflows.put(name, workflow);
        configManager.saveWorkflow(workflow);
    }
    
    private WorkflowDefinition getWorkflow(String name) {
        return workflows.get(name);
    }
    
    // Configuration types
    public enum ConfigType {
        JSON, XML
    }
    
    // Getters for UI integration
    public boolean isVoiceListeningEnabled() { return voiceListeningEnabled; }
    public boolean isRecordingMode() { return recordingMode; }
    public String getCurrentRecordingSession() { return currentRecordingSession; }
    public Map<String, WorkflowTemplate> getWorkflowTemplates() { return new HashMap<>(workflowTemplates); }
    public Map<String, RecordedSequence> getRecordedSequences() { return new HashMap<>(recordedSequences); }
    
    @Override
    public void shutdown() {
        super.shutdown();
        
        if (voiceProcessor != null) {
            voiceProcessor.shutdown();
        }
        if (actionRecorder != null) {
            actionRecorder.shutdown();
        }
        if (nlpProcessor != null) {
            nlpProcessor.shutdown();
        }
    }
    
    /**
     * Critical: Cleanup method to prevent memory leaks and resource corruption
     * Addresses failures identified in comprehensive analysis
     */
    public synchronized void cleanup() {
        synchronized (creationLock) {
            if (isDestroyed) {
                return;
            }
            
            isDestroyed = true;
            recordingMode = false;
            voiceListeningEnabled = false;
            
            try {
                // Stop recording if active
                if (actionRecorder != null) {
                    actionRecorder.stopRecording();
                    actionRecorder = null;
                }
                
                // Cleanup voice processor to prevent handler leaks
                if (voiceProcessor != null) {
                    voiceProcessor.cleanup();
                    voiceProcessor = null;
                }
                
                // Cleanup NLP processor and model resources
                if (nlpProcessor != null) {
                    nlpProcessor.shutdown();
                    nlpProcessor = null;
                }
                
                // Cleanup visual builder resources
                if (visualBuilder != null) {
                    visualBuilder.cleanup();
                    visualBuilder = null;
                }
                
                // Clear template storage to prevent memory retention
                if (workflowTemplates != null) {
                    workflowTemplates.clear();
                }
                
                // Clear recorded sequences
                if (recordedSequences != null) {
                    recordedSequences.clear();
                }
                
                // Cleanup configuration manager
                if (configManager != null) {
                    configManager.cleanup();
                    configManager = null;
                }
                
                // Clear session reference
                currentRecordingSession = null;
                
                Log.d(TAG, "AdvancedWorkflowEngine cleanup completed");
                
            } catch (Exception e) {
                Log.e(TAG, "Error during AdvancedWorkflowEngine cleanup", e);
            }
        }
    }
    
    /**
     * Static cleanup for singleton instance
     */
    public static synchronized void destroyInstance() {
        if (instance != null) {
            instance.cleanup();
            instance = null;
        }
    }
}
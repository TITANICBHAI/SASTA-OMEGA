package com.gestureai.gameautomation.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.systems.EnhancedExpertDemonstrationSystem;
import com.gestureai.gameautomation.workflow.recording.EnhancedRecordingSystem;
import com.gestureai.gameautomation.adapters.FrameAnalysisAdapter;
import com.gestureai.gameautomation.models.ReasoningDataStructures.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Expert Demonstration Activity with Multi-Frame Selection and Reasoning Input
 */
public class ExpertDemonstrationActivity extends AppCompatActivity 
    implements EnhancedExpertDemonstrationSystem.DemonstrationListener {
    
    private static final String TAG = "ExpertDemoActivity";
    
    // UI Components
    private Button startSessionBtn, stopSessionBtn, addFrameBtn, completeSessionBtn;
    private EditText sessionNameInput, gameContextInput;
    private EditText whyInput, whatInput, howInput, actionInput;
    private CheckBox optionalCheckbox;
    private SeekBar criticalitySeekBar;
    private RecyclerView framesRecyclerView;
    private TextView sessionStatusText, aiConfidenceText;
    private ImageView currentFrameView;
    
    // System Components
    private EnhancedExpertDemonstrationSystem demonstrationSystem;
    private FrameAnalysisAdapter frameAdapter;
    private List<EnhancedExpertDemonstrationSystem.AnalyzedFrame> analyzedFrames;
    
    // Session State
    private String currentSessionId;
    private boolean isSessionActive = false;
    private int selectedFrameIndex = -1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expert_demonstration);
        
        initializeViews();
        initializeSystem();
        setupListeners();
        setupRecyclerView();
    }
    
    private void initializeViews() {
        // Session controls
        startSessionBtn = findViewById(R.id.btn_start_session);
        stopSessionBtn = findViewById(R.id.btn_stop_session);
        addFrameBtn = findViewById(R.id.btn_add_frame);
        completeSessionBtn = findViewById(R.id.btn_complete_session);
        
        // Input fields
        sessionNameInput = findViewById(R.id.et_session_name);
        gameContextInput = findViewById(R.id.et_game_context);
        whyInput = findViewById(R.id.et_why);
        whatInput = findViewById(R.id.et_what);
        howInput = findViewById(R.id.et_how);
        actionInput = findViewById(R.id.et_action);
        optionalCheckbox = findViewById(R.id.cb_optional);
        criticalitySeekBar = findViewById(R.id.seekbar_criticality);
        
        // Display components
        framesRecyclerView = findViewById(R.id.rv_frames);
        sessionStatusText = findViewById(R.id.tv_session_status);
        aiConfidenceText = findViewById(R.id.tv_ai_confidence);
        currentFrameView = findViewById(R.id.iv_current_frame);
        
        // Initial UI state
        updateUIState(false);
    }
    
    private void initializeSystem() {
        try {
            demonstrationSystem = new EnhancedExpertDemonstrationSystem(this);
            demonstrationSystem.setDemonstrationListener(this);
            analyzedFrames = new ArrayList<>();
            
            Log.d(TAG, "Expert demonstration system initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing demonstration system", e);
            Toast.makeText(this, "Failed to initialize system: " + e.getMessage(), 
                          Toast.LENGTH_LONG).show();
        }
    }
    
    private void setupListeners() {
        startSessionBtn.setOnClickListener(v -> startSession());
        stopSessionBtn.setOnClickListener(v -> stopSession());
        addFrameBtn.setOnClickListener(v -> addFrame());
        completeSessionBtn.setOnClickListener(v -> completeSession());
        
        // Reasoning input listeners
        findViewById(R.id.btn_apply_reasoning).setOnClickListener(v -> applyReasoning());
        findViewById(R.id.btn_clear_reasoning).setOnClickListener(v -> clearReasoningInputs());
        
        // Criticality seekbar
        criticalitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float criticality = progress / 100.0f;
                updateCriticalityDisplay(criticality);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    private void setupRecyclerView() {
        frameAdapter = new FrameAnalysisAdapter(analyzedFrames, this::onFrameSelected);
        framesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        framesRecyclerView.setAdapter(frameAdapter);
    }
    
    private void startSession() {
        String sessionName = sessionNameInput.getText().toString().trim();
        String gameContext = gameContextInput.getText().toString().trim();
        
        if (sessionName.isEmpty()) {
            Toast.makeText(this, "Please enter session name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            currentSessionId = "demo_" + System.currentTimeMillis();
            demonstrationSystem.startDemonstrationSession(currentSessionId, gameContext);
            
            isSessionActive = true;
            analyzedFrames.clear();
            frameAdapter.notifyDataSetChanged();
            
            updateUIState(true);
            sessionStatusText.setText("Session Active: " + sessionName);
            
            Toast.makeText(this, "Session started: " + sessionName, Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting session", e);
            Toast.makeText(this, "Failed to start session: " + e.getMessage(), 
                          Toast.LENGTH_LONG).show();
        }
    }
    
    private void stopSession() {
        if (!isSessionActive) return;
        
        try {
            isSessionActive = false;
            updateUIState(false);
            sessionStatusText.setText("Session Stopped");
            
            Toast.makeText(this, "Session stopped", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping session", e);
        }
    }
    
    private void addFrame() {
        if (!isSessionActive) {
            Toast.makeText(this, "No active session", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Capture current screen frame
            Bitmap capturedFrame = captureCurrentScreen();
            if (capturedFrame != null) {
                demonstrationSystem.addFrameToSession(currentSessionId, capturedFrame);
                Toast.makeText(this, "Frame added for analysis", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to capture frame", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding frame", e);
            Toast.makeText(this, "Error adding frame: " + e.getMessage(), 
                          Toast.LENGTH_LONG).show();
        }
    }
    
    private void applyReasoning() {
        if (selectedFrameIndex == -1) {
            Toast.makeText(this, "Please select a frame first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String why = whyInput.getText().toString().trim();
        String what = whatInput.getText().toString().trim();
        String how = howInput.getText().toString().trim();
        String action = actionInput.getText().toString().trim();
        boolean isOptional = optionalCheckbox.isChecked();
        
        if (why.isEmpty() || action.isEmpty()) {
            Toast.makeText(this, "Why and Action fields are required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            demonstrationSystem.defineFrameReasoning(
                currentSessionId, selectedFrameIndex, why, what, how, action, isOptional);
            
            Toast.makeText(this, "Reasoning applied to frame " + selectedFrameIndex, 
                          Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error applying reasoning", e);
            Toast.makeText(this, "Error applying reasoning: " + e.getMessage(), 
                          Toast.LENGTH_LONG).show();
        }
    }
    
    private void completeSession() {
        if (!isSessionActive) {
            Toast.makeText(this, "No active session", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            demonstrationSystem.completeSession(currentSessionId);
            
            isSessionActive = false;
            updateUIState(false);
            sessionStatusText.setText("Session Completed");
            
            Toast.makeText(this, "Session completed and workflows generated", 
                          Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error completing session", e);
            Toast.makeText(this, "Error completing session: " + e.getMessage(), 
                          Toast.LENGTH_LONG).show();
        }
    }
    
    private void onFrameSelected(int position) {
        selectedFrameIndex = position;
        
        if (position < analyzedFrames.size()) {
            EnhancedExpertDemonstrationSystem.AnalyzedFrame frame = analyzedFrames.get(position);
            
            // Display frame
            currentFrameView.setImageBitmap(frame.getOriginalFrame());
            
            // Load existing reasoning if any
            EnhancedExpertDemonstrationSystem.ExpertReasoning reasoning = frame.getReasoning();
            if (reasoning.getWhy() != null) whyInput.setText(reasoning.getWhy());
            if (reasoning.getWhat() != null) whatInput.setText(reasoning.getWhat());
            if (reasoning.getHow() != null) howInput.setText(reasoning.getHow());
            if (reasoning.getAction() != null) actionInput.setText(reasoning.getAction());
            optionalCheckbox.setChecked(reasoning.isOptional());
            criticalitySeekBar.setProgress((int)(reasoning.getCriticalityScore() * 100));
            
            // Update AI confidence display
            updateAIConfidenceDisplay(frame.confidence);
            
            Toast.makeText(this, "Selected frame " + position, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateUIState(boolean sessionActive) {
        startSessionBtn.setEnabled(!sessionActive);
        stopSessionBtn.setEnabled(sessionActive);
        addFrameBtn.setEnabled(sessionActive);
        completeSessionBtn.setEnabled(sessionActive);
        
        sessionNameInput.setEnabled(!sessionActive);
        gameContextInput.setEnabled(!sessionActive);
        
        // Reasoning inputs enabled when frame is selected
        boolean reasoningEnabled = sessionActive && selectedFrameIndex != -1;
        whyInput.setEnabled(reasoningEnabled);
        whatInput.setEnabled(reasoningEnabled);
        howInput.setEnabled(reasoningEnabled);
        actionInput.setEnabled(reasoningEnabled);
        optionalCheckbox.setEnabled(reasoningEnabled);
        criticalitySeekBar.setEnabled(reasoningEnabled);
        findViewById(R.id.btn_apply_reasoning).setEnabled(reasoningEnabled);
    }
    
    private void updateCriticalityDisplay(float criticality) {
        TextView criticalityText = findViewById(R.id.tv_criticality_value);
        String level;
        if (criticality > 0.8f) level = "CRITICAL";
        else if (criticality > 0.6f) level = "HIGH";
        else if (criticality > 0.4f) level = "MEDIUM";
        else level = "LOW";
        
        criticalityText.setText(String.format("%.1f (%s)", criticality, level));
    }
    
    private void updateAIConfidenceDisplay(EnhancedExpertDemonstrationSystem.ConfidenceMetrics confidence) {
        confidence.calculateOverallConfidence();
        String confidenceText = String.format(
            "AI Confidence:\nVision: %.1f%%\nNLP: %.1f%%\nRL: %.1f%%\nIRL: %.1f%%\nOverall: %.1f%%",
            confidence.getVisionConfidence() * 100,
            confidence.getNlpConfidence() * 100,
            confidence.getRlConfidence() * 100,
            confidence.getIrlConfidence() * 100,
            confidence.getOverallConfidence() * 100
        );
        aiConfidenceText.setText(confidenceText);
    }
    
    private void clearReasoningInputs() {
        whyInput.setText("");
        whatInput.setText("");
        howInput.setText("");
        actionInput.setText("");
        optionalCheckbox.setChecked(false);
        criticalitySeekBar.setProgress(50);
    }
    
    private Bitmap captureCurrentScreen() {
        try {
            // Request screen capture from accessibility service or screen capture service
            Intent captureIntent = new Intent("com.gestureai.CAPTURE_SCREEN");
            sendBroadcast(captureIntent);
            
            // For demo purposes, return a placeholder
            // In production, this would get the actual captured frame
            return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
            
        } catch (Exception e) {
            Log.e(TAG, "Error capturing screen", e);
            return null;
        }
    }
    
    // Demonstration Listener Implementation
    @Override
    public void onSessionStarted(String sessionName, String gameContext) {
        runOnUiThread(() -> {
            sessionStatusText.setText("Session Started: " + sessionName);
            Toast.makeText(this, "AI analysis initialized", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onFrameAdded(EnhancedExpertDemonstrationSystem.AnalyzedFrame frame, int totalFrames) {
        runOnUiThread(() -> {
            analyzedFrames.add(frame);
            frameAdapter.notifyItemInserted(analyzedFrames.size() - 1);
            
            sessionStatusText.setText("Frames: " + totalFrames + " | AI Analysis: Processing");
        });
    }
    
    @Override
    public void onFrameAnalyzed(EnhancedExpertDemonstrationSystem.AnalyzedFrame frame, 
                               SemanticAnalysisResult analysis) {
        runOnUiThread(() -> {
            // Update frame in adapter
            int frameIndex = analyzedFrames.indexOf(frame);
            if (frameIndex != -1) {
                frameAdapter.notifyItemChanged(frameIndex);
            }
            
            sessionStatusText.setText("Frames: " + analyzedFrames.size() + " | AI Analysis: Complete");
        });
    }
    
    @Override
    public void onReasoningExtracted(EnhancedExpertDemonstrationSystem.AnalyzedFrame frame, 
                                   EnhancedExpertDemonstrationSystem.ExpertReasoning reasoning) {
        runOnUiThread(() -> {
            Toast.makeText(this, "AI enhanced reasoning for frame", Toast.LENGTH_SHORT).show();
            
            // Update confidence display if this is the selected frame
            if (selectedFrameIndex != -1 && 
                selectedFrameIndex < analyzedFrames.size() && 
                analyzedFrames.get(selectedFrameIndex) == frame) {
                updateAIConfidenceDisplay(frame.confidence);
            }
        });
    }
    
    @Override
    public void onWorkflowGenerated(com.gestureai.gameautomation.workflow.WorkflowDefinition workflow) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Workflow generated: " + workflow.getName(), 
                          Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    public void onSequenceCompleted(DemonstrationSequence sequence) {}
    @Override
    public void onLearningProgress(LearningMetrics metrics) {}
    @Override
    public void onPatternDiscovered(ActionPattern pattern) {}
    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (demonstrationSystem != null) {
            demonstrationSystem.shutdown();
        }
    }
}
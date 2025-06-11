package com.gestureai.gameautomation.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.GameAutomationEngine;
import com.gestureai.gameautomation.utils.NLPProcessor;
import com.gestureai.gameautomation.ai.InverseReinforcementLearner;
import com.gestureai.gameautomation.ai.ExplanationEngine;
import com.gestureai.gameautomation.models.DecisionExplanation;
import com.gestureai.gameautomation.models.GameFrame;
import com.gestureai.gameautomation.models.RewardFunction;
import android.util.Log;
import java.util.*;

/**
 * Advanced AI Explainer Fragment
 * Combines NLP + Visual Frame Analysis + Deep IRL for decision explanation
 * Shows WHY the AI made specific decisions based on multimodal analysis
 */
public class AIExplainerFragment extends Fragment {
    private static final String TAG = "AIExplainerFragment";
    
    // UI Components
    private TextView tvCurrentDecision;
    private TextView tvConfidenceScore;
    private TextView tvRewardFunction;
    private ImageView ivCurrentFrame;
    private RecyclerView rvExplanations;
    private Button btnStartExplaining;
    private Button btnStopExplaining;
    private Button btnExportModel;
    private ProgressBar pbProcessing;
    private Switch swRealTimeExplanation;
    private SeekBar seekExplanationDepth;
    
    // AI Components
    private GameAutomationEngine automationEngine;
    private NLPProcessor nlpProcessor;
    private InverseReinforcementLearner irlLearner;
    private ExplanationEngine explanationEngine;
    private ExplanationAdapter explanationAdapter;
    
    // Data Collection
    private List<DecisionExplanation> explanations = new ArrayList<>();
    private List<GameFrame> collectedFrames = new ArrayList<>();
    private boolean isExplaining = false;
    private int currentFrameIndex = 0;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai_explainer, container, false);
        
        initializeViews(view);
        initializeAIComponents();
        setupListeners();
        setupRecyclerView();
        
        return view;
    }
    
    private void initializeViews(View view) {
        tvCurrentDecision = view.findViewById(R.id.tv_current_decision);
        tvConfidenceScore = view.findViewById(R.id.tv_confidence_score);
        tvRewardFunction = view.findViewById(R.id.tv_reward_function);
        ivCurrentFrame = view.findViewById(R.id.iv_current_frame);
        rvExplanations = view.findViewById(R.id.rv_explanations);
        btnStartExplaining = view.findViewById(R.id.btn_start_explaining);
        btnStopExplaining = view.findViewById(R.id.btn_stop_explaining);
        btnExportModel = view.findViewById(R.id.btn_export_model);
        pbProcessing = view.findViewById(R.id.pb_processing);
        swRealTimeExplanation = view.findViewById(R.id.sw_realtime_explanation);
        seekExplanationDepth = view.findViewById(R.id.seek_explanation_depth);
    }
    
    private void initializeAIComponents() {
        try {
            automationEngine = GameAutomationEngine.getInstance();
            nlpProcessor = new NLPProcessor(getContext());
            irlLearner = new InverseReinforcementLearner(getContext());
            explanationEngine = new ExplanationEngine(getContext());
            
            Log.d(TAG, "AI Explainer components initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize AI components", e);
        }
    }
    
    private void setupListeners() {
        btnStartExplaining.setOnClickListener(v -> startExplaining());
        btnStopExplaining.setOnClickListener(v -> stopExplaining());
        btnExportModel.setOnClickListener(v -> exportLearnedModel());
        
        swRealTimeExplanation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !isExplaining) {
                startRealTimeExplanation();
            } else if (!isChecked) {
                stopRealTimeExplanation();
            }
        });
        
        seekExplanationDepth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (explanationEngine != null) {
                    explanationEngine.setExplanationDepth(progress / 100.0f);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    private void setupRecyclerView() {
        explanationAdapter = new ExplanationAdapter(explanations);
        rvExplanations.setLayoutManager(new LinearLayoutManager(getContext()));
        rvExplanations.setAdapter(explanationAdapter);
    }
    
    private void startExplaining() {
        isExplaining = true;
        btnStartExplaining.setEnabled(false);
        btnStopExplaining.setEnabled(true);
        pbProcessing.setVisibility(View.VISIBLE);
        
        // Start collecting frame data and explanations
        startFrameCollection();
        updateUI();
    }
    
    private void stopExplaining() {
        isExplaining = false;
        btnStartExplaining.setEnabled(true);
        btnStopExplaining.setEnabled(false);
        pbProcessing.setVisibility(View.GONE);
        
        // Process collected data through IRL
        processCollectedFrames();
        updateUI();
    }
    
    private void startFrameCollection() {
        new Thread(() -> {
            while (isExplaining) {
                try {
                    // Capture current game frame
                    GameFrame frame = captureCurrentGameFrame();
                    if (frame != null) {
                        collectedFrames.add(frame);
                        
                        // Generate real-time explanation
                        DecisionExplanation explanation = generateFrameExplanation(frame);
                        explanations.add(explanation);
                        
                        getActivity().runOnUiThread(() -> {
                            updateCurrentFrameDisplay(frame);
                            updateExplanationsList();
                        });
                    }
                    
                    Thread.sleep(100); // 10 FPS collection rate
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in frame collection", e);
                }
            }
        }).start();
    }
    
    private GameFrame captureCurrentGameFrame() {
        try {
            GameFrame frame = new GameFrame();
            frame.timestamp = System.currentTimeMillis();
            frame.frameIndex = currentFrameIndex++;
            
            // Capture visual data
            if (automationEngine != null) {
                frame.screenshot = automationEngine.getCurrentScreenCapture();
                frame.detectedObjects = automationEngine.getCurrentDetectedObjects();
            }
            
            // Capture NLP data from OCR
            if (nlpProcessor != null && frame.screenshot != null) {
                frame.ocrTexts = extractOCRFromFrame(frame.screenshot);
                frame.nlpAnalysis = nlpProcessor.analyzeGameTextWithBERT(frame.ocrTexts);
            }
            
            // Capture game state
            frame.gameState = automationEngine.getCurrentGameState();
            frame.playerActions = automationEngine.getRecentActions();
            
            return frame;
            
        } catch (Exception e) {
            Log.e(TAG, "Error capturing game frame", e);
            return null;
        }
    }
    
    private DecisionExplanation generateFrameExplanation(GameFrame frame) {
        DecisionExplanation explanation = new DecisionExplanation();
        explanation.timestamp = frame.timestamp;
        explanation.frameIndex = frame.frameIndex;
        
        try {
            // Analyze current decision using multimodal AI
            if (explanationEngine != null) {
                explanation = explanationEngine.explainDecision(frame);
            }
            
            // Extract reasoning from NLP analysis
            if (frame.nlpAnalysis != null) {
                explanation.textualContext = extractTextualReasoning(frame.nlpAnalysis);
            }
            
            // Analyze visual features that influenced decision
            explanation.visualFeatures = analyzeVisualInfluence(frame);
            
            // Calculate confidence and reasoning
            explanation.confidence = calculateDecisionConfidence(frame);
            explanation.reasoning = generateReasoning(frame, explanation);
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating explanation", e);
            explanation.reasoning = "Error in explanation generation";
            explanation.confidence = 0.0f;
        }
        
        return explanation;
    }
    
    private void processCollectedFrames() {
        if (collectedFrames.isEmpty()) return;
        
        new Thread(() -> {
            try {
                getActivity().runOnUiThread(() -> pbProcessing.setVisibility(View.VISIBLE));
                
                // Run Inverse Reinforcement Learning on collected data
                RewardFunction learnedReward = irlLearner.learnFromTrajectory(collectedFrames);
                
                // Update explanations with learned reward function insights
                for (DecisionExplanation explanation : explanations) {
                    explanation.rewardAnalysis = analyzeRewardInfluence(explanation, learnedReward);
                }
                
                getActivity().runOnUiThread(() -> {
                    updateRewardFunctionDisplay(learnedReward);
                    explanationAdapter.notifyDataSetChanged();
                    pbProcessing.setVisibility(View.GONE);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error processing collected frames", e);
                getActivity().runOnUiThread(() -> pbProcessing.setVisibility(View.GONE));
            }
        }).start();
    }
    
    private List<String> extractOCRFromFrame(Bitmap screenshot) {
        // Integration with existing OCR system
        List<String> ocrTexts = new ArrayList<>();
        try {
            // Use existing OCR engine from automation system
            if (automationEngine != null) {
                ocrTexts = automationEngine.extractTextFromScreen(screenshot);
            }
        } catch (Exception e) {
            Log.e(TAG, "OCR extraction failed", e);
        }
        return ocrTexts;
    }
    
    private String extractTextualReasoning(NLPProcessor.GameTextAnalysis nlpAnalysis) {
        StringBuilder reasoning = new StringBuilder();
        
        // Analyze strategy insights
        for (NLPProcessor.StrategyInsight insight : nlpAnalysis.getStrategies()) {
            if (insight.aggressionLevel > 0.6f) {
                reasoning.append("Aggressive context detected. ");
            }
            if (insight.defensiveNeeds > 0.5f) {
                reasoning.append("Defensive positioning needed. ");
            }
            if (insight.teamworkOpportunity > 0.4f) {
                reasoning.append("Team coordination opportunity. ");
            }
        }
        
        // Analyze text classifications
        for (NLPProcessor.GameTextClassification classification : nlpAnalysis.getClassifications()) {
            if (classification.confidence > 0.7f) {
                reasoning.append("Detected ").append(classification.type).append(". ");
            }
        }
        
        return reasoning.toString();
    }
    
    private Map<String, Float> analyzeVisualInfluence(GameFrame frame) {
        Map<String, Float> features = new HashMap<>();
        
        if (frame.detectedObjects != null) {
            float enemyInfluence = 0;
            float powerupInfluence = 0;
            float obstacleInfluence = 0;
            
            for (Object obj : frame.detectedObjects) {
                // Simplified object analysis
                String objType = obj.toString().toLowerCase();
                if (objType.contains("enemy")) {
                    enemyInfluence += 0.3f;
                } else if (objType.contains("powerup") || objType.contains("coin")) {
                    powerupInfluence += 0.2f;
                } else if (objType.contains("obstacle")) {
                    obstacleInfluence += 0.1f;
                }
            }
            
            features.put("enemy_influence", Math.min(1.0f, enemyInfluence));
            features.put("powerup_influence", Math.min(1.0f, powerupInfluence));
            features.put("obstacle_influence", Math.min(1.0f, obstacleInfluence));
        }
        
        return features;
    }
    
    private float calculateDecisionConfidence(GameFrame frame) {
        float confidence = 0.5f; // Base confidence
        
        // Increase confidence based on clear visual and textual cues
        if (frame.nlpAnalysis != null) {
            for (NLPProcessor.GameTextClassification classification : frame.nlpAnalysis.getClassifications()) {
                confidence += classification.confidence * 0.2f;
            }
        }
        
        if (frame.detectedObjects != null && !frame.detectedObjects.isEmpty()) {
            confidence += 0.2f; // Visual clarity bonus
        }
        
        return Math.min(1.0f, confidence);
    }
    
    private String generateReasoning(GameFrame frame, DecisionExplanation explanation) {
        StringBuilder reasoning = new StringBuilder();
        
        reasoning.append("Frame ").append(frame.frameIndex).append(": ");
        
        if (!explanation.textualContext.isEmpty()) {
            reasoning.append("Text analysis: ").append(explanation.textualContext).append(" ");
        }
        
        if (!explanation.visualFeatures.isEmpty()) {
            reasoning.append("Visual cues: ");
            for (Map.Entry<String, Float> feature : explanation.visualFeatures.entrySet()) {
                if (feature.getValue() > 0.3f) {
                    reasoning.append(feature.getKey()).append(" (").append(String.format("%.1f", feature.getValue())).append(") ");
                }
            }
        }
        
        reasoning.append("Confidence: ").append(String.format("%.1f%%", explanation.confidence * 100));
        
        return reasoning.toString();
    }
    
    private String analyzeRewardInfluence(DecisionExplanation explanation, RewardFunction rewardFunction) {
        if (rewardFunction == null) return "No reward analysis available";
        
        StringBuilder analysis = new StringBuilder();
        analysis.append("Learned reward factors: ");
        
        Map<String, Float> rewards = rewardFunction.getFeatureWeights();
        for (Map.Entry<String, Float> reward : rewards.entrySet()) {
            if (Math.abs(reward.getValue()) > 0.1f) {
                analysis.append(reward.getKey()).append(" (").append(String.format("%.2f", reward.getValue())).append(") ");
            }
        }
        
        return analysis.toString();
    }
    
    private void updateCurrentFrameDisplay(GameFrame frame) {
        if (frame.screenshot != null) {
            ivCurrentFrame.setImageBitmap(frame.screenshot);
        }
        
        tvCurrentDecision.setText("Processing frame " + frame.frameIndex);
    }
    
    private void updateExplanationsList() {
        explanationAdapter.notifyItemInserted(explanations.size() - 1);
        rvExplanations.scrollToPosition(explanations.size() - 1);
    }
    
    private void updateRewardFunctionDisplay(RewardFunction rewardFunction) {
        if (rewardFunction != null) {
            StringBuilder display = new StringBuilder("Learned Reward Function:\n");
            Map<String, Float> weights = rewardFunction.getFeatureWeights();
            
            for (Map.Entry<String, Float> weight : weights.entrySet()) {
                display.append(weight.getKey()).append(": ").append(String.format("%.3f", weight.getValue())).append("\n");
            }
            
            tvRewardFunction.setText(display.toString());
        }
    }
    
    private void startRealTimeExplanation() {
        // Enable real-time explanation mode
        if (explanationEngine != null) {
            explanationEngine.setRealTimeMode(true);
        }
    }
    
    private void stopRealTimeExplanation() {
        if (explanationEngine != null) {
            explanationEngine.setRealTimeMode(false);
        }
    }
    
    private void exportLearnedModel() {
        new Thread(() -> {
            try {
                getActivity().runOnUiThread(() -> pbProcessing.setVisibility(View.VISIBLE));
                
                // Export learned reward function and explanations to JSON
                String exportData = createExportData();
                
                // Save to file system
                String filename = "ai_explanation_model_" + System.currentTimeMillis() + ".json";
                saveExportData(filename, exportData);
                
                getActivity().runOnUiThread(() -> {
                    pbProcessing.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Model exported to: " + filename, Toast.LENGTH_LONG).show();
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Export failed", e);
                getActivity().runOnUiThread(() -> {
                    pbProcessing.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    private String createExportData() {
        // Create comprehensive export of learned AI behavior
        org.json.JSONObject exportJson = new org.json.JSONObject();
        
        try {
            // Export learned reward function
            if (irlLearner != null) {
                RewardFunction reward = irlLearner.getCurrentRewardFunction();
                if (reward != null) {
                    exportJson.put("reward_function", reward.toJSON());
                }
            }
            
            // Export decision explanations
            org.json.JSONArray explanationsArray = new org.json.JSONArray();
            for (DecisionExplanation explanation : explanations) {
                explanationsArray.put(explanation.toJSON());
            }
            exportJson.put("explanations", explanationsArray);
            
            // Export frame statistics
            org.json.JSONObject stats = new org.json.JSONObject();
            stats.put("total_frames", collectedFrames.size());
            stats.put("total_explanations", explanations.size());
            stats.put("collection_period", System.currentTimeMillis());
            exportJson.put("statistics", stats);
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating export data", e);
        }
        
        return exportJson.toString();
    }
    
    private void saveExportData(String filename, String data) {
        try {
            java.io.FileOutputStream fos = getContext().openFileOutput(filename, getContext().MODE_PRIVATE);
            fos.write(data.getBytes());
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Error saving export data", e);
            throw new RuntimeException("Failed to save export data", e);
        }
    }
    
    private void updateUI() {
        if (isExplaining) {
            tvCurrentDecision.setText("Collecting and analyzing frames...");
            tvConfidenceScore.setText("Confidence: Calculating...");
        } else {
            tvCurrentDecision.setText("Analysis complete");
            if (!explanations.isEmpty()) {
                float avgConfidence = (float) explanations.stream()
                    .mapToDouble(e -> e.confidence)
                    .average()
                    .orElse(0.0);
                tvConfidenceScore.setText("Average Confidence: " + String.format("%.1f%%", avgConfidence * 100));
            }
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopExplaining();
        if (explanationEngine != null) {
            explanationEngine.cleanup();
        }
    }
}
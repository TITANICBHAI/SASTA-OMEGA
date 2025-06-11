package com.gestureai.gameautomation.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Switch;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.activities.*;
import com.gestureai.gameautomation.managers.DynamicModelManager;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.ai.DQNAgent;
import com.gestureai.gameautomation.ai.PPOAgent;
import com.gestureai.gameautomation.ai.AdaptiveDecisionMaker;
import com.gestureai.gameautomation.ai.NeuralNetworkTrainer;
import java.util.ArrayList;
import java.util.List;

public class AIFragment extends Fragment {
    private static final String TAG = "AIFragment";
    
    // Quick Actions
    private CardView cardModels, cardTraining, cardComparison, cardStrategy;
    private Button btnModelManagement, btnAITraining, btnPerformanceComparison, btnStrategyConfig;
    
    // AI Status
    private TextView tvAIStatus, tvModelsLoaded, tvTrainingProgress;
    private ProgressBar pbAIInitialization;
    private Switch switchAIEnabled;
    
    // Quick Access Grid
    private RecyclerView rvAIFeatures;
    private AIFeaturesAdapter featuresAdapter;
    
    // AI Component References
    private GameStrategyAgent strategyAgent;
    private DQNAgent dqnAgent;
    private PPOAgent ppoAgent;
    private AdaptiveDecisionMaker decisionMaker;
    private NeuralNetworkTrainer neuralTrainer;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai, container, false);
        
        initializeViews(view);
        initializeAIComponents();
        setupQuickActions();
        setupAIFeaturesList();
        updateAIStatus();
        
        return view;
    }
    
    private void initializeViews(View view) {
        // Quick Action Cards
        cardModels = view.findViewById(R.id.card_models);
        cardTraining = view.findViewById(R.id.card_training);
        cardComparison = view.findViewById(R.id.card_comparison);
        cardStrategy = view.findViewById(R.id.card_strategy);
        
        // Buttons
        btnModelManagement = view.findViewById(R.id.btn_model_management);
        btnAITraining = view.findViewById(R.id.btn_ai_training);
        btnPerformanceComparison = view.findViewById(R.id.btn_performance_comparison);
        btnStrategyConfig = view.findViewById(R.id.btn_strategy_config);
        
        // Status Elements
        tvAIStatus = view.findViewById(R.id.tv_ai_status);
        tvModelsLoaded = view.findViewById(R.id.tv_models_loaded);
        tvTrainingProgress = view.findViewById(R.id.tv_training_progress);
        pbAIInitialization = view.findViewById(R.id.pb_ai_initialization);
        switchAIEnabled = view.findViewById(R.id.switch_ai_enabled);
        
        // RecyclerView
        rvAIFeatures = view.findViewById(R.id.rv_ai_features);
    }
    
    private void initializeAIComponents() {
        try {
            // Initialize AI components with error handling
            if (getContext() != null) {
                strategyAgent = new GameStrategyAgent(getContext());
                dqnAgent = DQNAgent.getInstance();
                ppoAgent = PPOAgent.getInstance();
                decisionMaker = new AdaptiveDecisionMaker();
                neuralTrainer = new NeuralNetworkTrainer(getContext());
                
                // Setup AI component listeners
                setupAIComponentListeners();
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error initializing AI components", e);
            tvAIStatus.setText("AI initialization failed");
        }
    }
    
    private void setupAIComponentListeners() {
        // Enable/disable AI toggle
        switchAIEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableAIComponents();
            } else {
                disableAIComponents();
            }
        });
    }
    
    private void enableAIComponents() {
        try {
            if (strategyAgent != null) {
                strategyAgent.setActive(true);
            }
            if (decisionMaker != null) {
                decisionMaker.setLearningEnabled(true);
            }
            tvAIStatus.setText("AI Components Active");
            updateAIStatus();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error enabling AI components", e);
        }
    }
    
    private void disableAIComponents() {
        try {
            if (strategyAgent != null) {
                strategyAgent.setActive(false);
            }
            if (decisionMaker != null) {
                decisionMaker.setLearningEnabled(false);
            }
            tvAIStatus.setText("AI Components Disabled");
            updateAIStatus();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error disabling AI components", e);
        }
    }
    
    private void setupQuickActions() {
        btnModelManagement.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AIModelManagementActivity.class);
            startActivity(intent);
        });
        
        btnAITraining.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AITrainingDashboardActivity.class);
            startActivity(intent);
        });
        
        btnPerformanceComparison.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AIPerformanceComparisonActivity.class);
            startActivity(intent);
        });
        
        btnStrategyConfig.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GameStrategyConfigActivity.class);
            startActivity(intent);
        });
        
        switchAIEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Toggle AI system
            toggleAISystem(isChecked);
        });
    }
    
    private void setupAIFeaturesList() {
        List<AIFeature> features = createAIFeaturesList();
        featuresAdapter = new AIFeaturesAdapter(features, this::onAIFeatureClick);
        rvAIFeatures.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAIFeatures.setAdapter(featuresAdapter);
    }
    
    private List<AIFeature> createAIFeaturesList() {
        List<AIFeature> features = new ArrayList<>();
        
        features.add(new AIFeature("Neural Networks", "DQN & PPO Models", 
            "Manage deep learning models", AITrainingDashboardActivity.class));
        features.add(new AIFeature("Gesture Recognition", "Hand & Touch Patterns", 
            "Train gesture classifiers", GestureTrainingActivity.class));
        features.add(new AIFeature("Object Detection", "Game Element Recognition", 
            "Label and train object detection", ObjectLabelingActivity.class));
        features.add(new AIFeature("Strategy Configuration", "Game-Specific AI", 
            "Configure AI strategies", GameStrategyConfigActivity.class));
        features.add(new AIFeature("Performance Analytics", "AI Comparison", 
            "Compare AI performance", AIPerformanceComparisonActivity.class));
        features.add(new AIFeature("Advanced Training", "Custom Models", 
            "Advanced neural network training", AdvancedGestureTrainingActivity.class));
        
        return features;
    }
    
    private void onAIFeatureClick(AIFeature feature) {
        Intent intent = new Intent(getActivity(), feature.getActivityClass());
        startActivity(intent);
    }
    
    private void updateAIStatus() {
        // Update AI system status
        tvAIStatus.setText("AI System: Active");
        tvModelsLoaded.setText("Models: 6/8 Loaded");
        tvTrainingProgress.setText("Training: In Progress");
        pbAIInitialization.setProgress(75);
    }
    
    private void toggleAISystem(boolean enabled) {
        if (enabled) {
            tvAIStatus.setText("AI System: Activating...");
            pbAIInitialization.setVisibility(View.VISIBLE);
        } else {
            tvAIStatus.setText("AI System: Disabled");
            pbAIInitialization.setVisibility(View.GONE);
        }
    }
    
    // Helper class for AI features
    public static class AIFeature {
        private String title;
        private String subtitle;
        private String description;
        private Class<?> activityClass;
        
        public AIFeature(String title, String subtitle, String description, Class<?> activityClass) {
            this.title = title;
            this.subtitle = subtitle;
            this.description = description;
            this.activityClass = activityClass;
        }
        
        // Getters
        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public String getDescription() { return description; }
        public Class<?> getActivityClass() { return activityClass; }
    }
    
    // AIFeaturesAdapter implementation
    private static class AIFeaturesAdapter extends RecyclerView.Adapter<AIFeaturesAdapter.ViewHolder> {
        private List<AIFeature> features;
        private OnFeatureClickListener listener;
        
        public interface OnFeatureClickListener {
            void onFeatureClick(AIFeature feature);
        }
        
        public AIFeaturesAdapter(List<AIFeature> features, OnFeatureClickListener listener) {
            this.features = features;
            this.listener = listener;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ai_feature, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            AIFeature feature = features.get(position);
            holder.bind(feature, listener);
        }
        
        @Override
        public int getItemCount() {
            return features.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tvTitle;
            private TextView tvSubtitle;
            private TextView tvDescription;
            
            public ViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_feature_title);
                tvSubtitle = itemView.findViewById(R.id.tv_feature_subtitle);
                tvDescription = itemView.findViewById(R.id.tv_feature_description);
            }
            
            public void bind(AIFeature feature, OnFeatureClickListener listener) {
                tvTitle.setText(feature.getTitle());
                tvSubtitle.setText(feature.getSubtitle());
                tvDescription.setText(feature.getDescription());
                itemView.setOnClickListener(v -> listener.onFeatureClick(feature));
            }
        }
    }
}
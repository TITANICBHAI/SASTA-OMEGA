package com.gestureai.gameautomation.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.List;
import java.util.ArrayList;

import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.managers.AITrainingControlManager;
import com.gestureai.gameautomation.managers.AITrainingControlManager.TrainingConfiguration;
import com.gestureai.gameautomation.managers.AITrainingControlManager.TrainingSession;
import com.gestureai.gameautomation.managers.AITrainingControlManager.TrainingComparison;
import com.gestureai.gameautomation.managers.AITrainingControlManager.TrainingProgressListener;

public class AITrainingDashboardActivity extends AppCompatActivity implements TrainingProgressListener {
    private static final String TAG = "AITrainingDashboard";
    
    private AITrainingControlManager trainingManager;
    
    // UI Components
    private Button btnStartDQN, btnStopDQN, btnStartPPO, btnStopPPO, btnStartStrategy, btnStopStrategy;
    private Button btnStartAllTraining, btnStopAllTraining;
    private ProgressBar pbDQNProgress, pbPPOProgress, pbStrategyProgress;
    private TextView tvDQNStatus, tvPPOStatus, tvStrategyStatus;
    private TextView tvDQNLoss, tvPPOLoss, tvStrategyLoss;
    private TextView tvDQNEpisodes, tvPPOEpisodes, tvStrategyEpisodes;
    private TextView tvTrainingComparison, tvBestPerforming;
    private SeekBar sbMaxEpisodes, sbLearningRate;
    private TextView tvMaxEpisodesValue, tvLearningRateValue;
    private RecyclerView rvTrainingHistory;
    private Switch swAutoSaveCheckpoints;
    
    private TrainingHistoryAdapter historyAdapter;
    private List<TrainingSession> trainingHistory;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_training_dashboard);
        
        initializeViews();
        initializeTrainingManager();
        setupEventListeners();
        updateUI();
        
        Log.d(TAG, "AI Training Dashboard initialized");
    }
    
    private void initializeViews() {
        // Training control buttons
        btnStartDQN = findViewById(R.id.btn_start_dqn);
        btnStopDQN = findViewById(R.id.btn_stop_dqn);
        btnStartPPO = findViewById(R.id.btn_start_ppo);
        btnStopPPO = findViewById(R.id.btn_stop_ppo);
        btnStartStrategy = findViewById(R.id.btn_start_strategy);
        btnStopStrategy = findViewById(R.id.btn_stop_strategy);
        btnStartAllTraining = findViewById(R.id.btn_start_all_training);
        btnStopAllTraining = findViewById(R.id.btn_stop_all_training);
        
        // Progress indicators
        pbDQNProgress = findViewById(R.id.pb_dqn_progress);
        pbPPOProgress = findViewById(R.id.pb_ppo_progress);
        pbStrategyProgress = findViewById(R.id.pb_strategy_progress);
        
        // Status displays
        tvDQNStatus = findViewById(R.id.tv_dqn_status);
        tvPPOStatus = findViewById(R.id.tv_ppo_status);
        tvStrategyStatus = findViewById(R.id.tv_strategy_status);
        
        // Performance metrics
        tvDQNLoss = findViewById(R.id.tv_dqn_loss);
        tvPPOLoss = findViewById(R.id.tv_ppo_loss);
        tvStrategyLoss = findViewById(R.id.tv_strategy_loss);
        tvDQNEpisodes = findViewById(R.id.tv_dqn_episodes);
        tvPPOEpisodes = findViewById(R.id.tv_ppo_episodes);
        tvStrategyEpisodes = findViewById(R.id.tv_strategy_episodes);
        
        // Comparison displays
        tvTrainingComparison = findViewById(R.id.tv_training_comparison);
        tvBestPerforming = findViewById(R.id.tv_best_performing);
        
        // Configuration controls
        sbMaxEpisodes = findViewById(R.id.sb_max_episodes);
        sbLearningRate = findViewById(R.id.sb_learning_rate);
        tvMaxEpisodesValue = findViewById(R.id.tv_max_episodes_value);
        tvLearningRateValue = findViewById(R.id.tv_learning_rate_value);
        swAutoSaveCheckpoints = findViewById(R.id.sw_auto_save_checkpoints);
        
        // Training history
        rvTrainingHistory = findViewById(R.id.rv_training_history);
        trainingHistory = new ArrayList<>();
        historyAdapter = new TrainingHistoryAdapter(trainingHistory);
        rvTrainingHistory.setLayoutManager(new LinearLayoutManager(this));
        rvTrainingHistory.setAdapter(historyAdapter);
        
        // Set initial values
        sbMaxEpisodes.setMax(2000);
        sbMaxEpisodes.setProgress(1000);
        sbLearningRate.setMax(100);
        sbLearningRate.setProgress(10); // 0.001 * 10 = 0.01
        updateConfigurationValues();
    }
    
    private void initializeTrainingManager() {
        trainingManager = AITrainingControlManager.getInstance(this);
        trainingManager.addTrainingProgressListener(this);
    }
    
    private void setupEventListeners() {
        // DQN controls
        btnStartDQN.setOnClickListener(v -> startDQNTraining());
        btnStopDQN.setOnClickListener(v -> trainingManager.stopDQNTraining());
        
        // PPO controls
        btnStartPPO.setOnClickListener(v -> startPPOTraining());
        btnStopPPO.setOnClickListener(v -> trainingManager.stopPPOTraining());
        
        // Strategy controls
        btnStartStrategy.setOnClickListener(v -> startStrategyTraining());
        btnStopStrategy.setOnClickListener(v -> trainingManager.stopStrategyTraining());
        
        // Combined controls
        btnStartAllTraining.setOnClickListener(v -> startAllTraining());
        btnStopAllTraining.setOnClickListener(v -> trainingManager.stopAllTraining());
        
        // Configuration listeners
        sbMaxEpisodes.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateConfigurationValues();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        sbLearningRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateConfigurationValues();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    private void updateConfigurationValues() {
        int maxEpisodes = sbMaxEpisodes.getProgress();
        float learningRate = sbLearningRate.getProgress() * 0.0001f; // 0.0001 to 0.01
        
        tvMaxEpisodesValue.setText(String.valueOf(maxEpisodes));
        tvLearningRateValue.setText(String.format("%.4f", learningRate));
    }
    
    private TrainingConfiguration createTrainingConfiguration() {
        int maxEpisodes = sbMaxEpisodes.getProgress();
        float learningRate = sbLearningRate.getProgress() * 0.0001f;
        
        TrainingConfiguration config = new TrainingConfiguration(maxEpisodes, learningRate);
        config.saveCheckpoints = swAutoSaveCheckpoints.isChecked();
        config.checkpointInterval = 100;
        config.trainingDelayMs = 10;
        
        return config;
    }
    
    private void startDQNTraining() {
        TrainingConfiguration config = createTrainingConfiguration();
        trainingManager.startDQNTraining(config);
    }
    
    private void startPPOTraining() {
        TrainingConfiguration config = createTrainingConfiguration();
        trainingManager.startPPOTraining(config);
    }
    
    private void startStrategyTraining() {
        TrainingConfiguration config = createTrainingConfiguration();
        trainingManager.startStrategyTraining(config);
    }
    
    private void startAllTraining() {
        TrainingConfiguration config = createTrainingConfiguration();
        trainingManager.startAllTraining(config);
    }
    
    private void updateUI() {
        runOnUiThread(() -> {
            // Update button states
            btnStartDQN.setEnabled(!trainingManager.isDQNTraining());
            btnStopDQN.setEnabled(trainingManager.isDQNTraining());
            btnStartPPO.setEnabled(!trainingManager.isPPOTraining());
            btnStopPPO.setEnabled(trainingManager.isPPOTraining());
            btnStartStrategy.setEnabled(!trainingManager.isStrategyTraining());
            btnStopStrategy.setEnabled(trainingManager.isStrategyTraining());
            
            btnStartAllTraining.setEnabled(!trainingManager.isAnyTraining());
            btnStopAllTraining.setEnabled(trainingManager.isAnyTraining());
            
            // Update status displays
            updateAgentStatus("DQN", tvDQNStatus, trainingManager.isDQNTraining());
            updateAgentStatus("PPO", tvPPOStatus, trainingManager.isPPOTraining());
            updateAgentStatus("Strategy", tvStrategyStatus, trainingManager.isStrategyTraining());
            
            // Update session information
            updateSessionInfo();
            
            // Update comparison
            updateTrainingComparison();
        });
    }
    
    private void updateAgentStatus(String agentType, TextView statusView, boolean isTraining) {
        if (isTraining) {
            statusView.setText(agentType + " Training: ACTIVE");
            statusView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            statusView.setText(agentType + " Training: STOPPED");
            statusView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }
    
    private void updateSessionInfo() {
        TrainingSession dqnSession = trainingManager.getDQNSession();
        TrainingSession ppoSession = trainingManager.getPPOSession();
        TrainingSession strategySession = trainingManager.getStrategySession();
        
        if (dqnSession != null) {
            tvDQNLoss.setText(String.format("Loss: %.4f", dqnSession.averageLoss));
            tvDQNEpisodes.setText(String.format("Episodes: %d/%d", 
                dqnSession.episodesCompleted, dqnSession.config.maxEpisodes));
            pbDQNProgress.setProgress((int)(dqnSession.getProgress() * 100));
        }
        
        if (ppoSession != null) {
            tvPPOLoss.setText(String.format("Loss: %.4f", ppoSession.averageLoss));
            tvPPOEpisodes.setText(String.format("Episodes: %d/%d", 
                ppoSession.episodesCompleted, ppoSession.config.maxEpisodes));
            pbPPOProgress.setProgress((int)(ppoSession.getProgress() * 100));
        }
        
        if (strategySession != null) {
            tvStrategyLoss.setText(String.format("Loss: %.4f", strategySession.averageLoss));
            tvStrategyEpisodes.setText(String.format("Episodes: %d/%d", 
                strategySession.episodesCompleted, strategySession.config.maxEpisodes));
            pbStrategyProgress.setProgress((int)(strategySession.getProgress() * 100));
        }
    }
    
    private void updateTrainingComparison() {
        TrainingComparison comparison = trainingManager.getTrainingComparison();
        
        StringBuilder comparisonText = new StringBuilder();
        comparisonText.append("Training Comparison:\n");
        
        if (comparison.dqnMetrics != null) {
            comparisonText.append(String.format("DQN: %.4f loss, %d episodes\n", 
                comparison.dqnMetrics.averageLoss, comparison.dqnMetrics.episodesCompleted));
        }
        
        if (comparison.ppoMetrics != null) {
            comparisonText.append(String.format("PPO: %.4f loss, %d episodes\n", 
                comparison.ppoMetrics.averageLoss, comparison.ppoMetrics.episodesCompleted));
        }
        
        if (comparison.strategyMetrics != null) {
            comparisonText.append(String.format("Strategy: %.4f loss, %d episodes\n", 
                comparison.strategyMetrics.averageLoss, comparison.strategyMetrics.episodesCompleted));
        }
        
        tvTrainingComparison.setText(comparisonText.toString());
        
        // Best performing agent
        var bestAgent = comparison.getBestPerforming();
        if (bestAgent != null) {
            tvBestPerforming.setText(String.format("Best Performing: %s (%.4f loss)", 
                bestAgent.agentType, bestAgent.averageLoss));
        }
    }
    
    // TrainingProgressListener implementation
    @Override
    public void onTrainingStarted(String agentType, TrainingConfiguration config) {
        Log.d(TAG, "Training started: " + agentType);
        updateUI();
        showToast(agentType + " training started");
    }
    
    @Override
    public void onTrainingProgress(String agentType, TrainingSession session) {
        Log.d(TAG, String.format("Training progress: %s - Episode %d, Loss: %.4f", 
            agentType, session.episodesCompleted, session.currentLoss));
        updateUI();
    }
    
    @Override
    public void onTrainingCompleted(String agentType, TrainingSession session) {
        Log.d(TAG, "Training completed: " + agentType);
        trainingHistory.add(session);
        historyAdapter.notifyDataSetChanged();
        updateUI();
        showToast(agentType + " training completed");
    }
    
    @Override
    public void onTrainingInterrupted(String agentType) {
        Log.d(TAG, "Training interrupted: " + agentType);
        updateUI();
        showToast(agentType + " training interrupted");
    }
    
    @Override
    public void onTrainingError(String agentType, String error) {
        Log.e(TAG, "Training error: " + agentType + " - " + error);
        updateUI();
        showToast("Training error: " + agentType + " - " + error);
    }
    
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (trainingManager != null) {
            trainingManager.removeTrainingProgressListener(this);
        }
    }
    
    // Training History Adapter
    private static class TrainingHistoryAdapter extends RecyclerView.Adapter<TrainingHistoryAdapter.ViewHolder> {
        private final List<TrainingSession> sessions;
        
        public TrainingHistoryAdapter(List<TrainingSession> sessions) {
            this.sessions = sessions;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_training_session, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TrainingSession session = sessions.get(position);
            holder.bind(session);
        }
        
        @Override
        public int getItemCount() {
            return sessions.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvAgentType;
            private final TextView tvEpisodes;
            private final TextView tvFinalLoss;
            private final TextView tvDuration;
            
            public ViewHolder(View itemView) {
                super(itemView);
                tvAgentType = itemView.findViewById(R.id.tv_agent_type);
                tvEpisodes = itemView.findViewById(R.id.tv_episodes);
                tvFinalLoss = itemView.findViewById(R.id.tv_final_loss);
                tvDuration = itemView.findViewById(R.id.tv_duration);
            }
            
            public void bind(TrainingSession session) {
                tvAgentType.setText(session.agentType);
                tvEpisodes.setText(String.valueOf(session.episodesCompleted));
                tvFinalLoss.setText(String.format("%.4f", session.averageLoss));
                tvDuration.setText(formatDuration(session.getTrainingDuration()));
            }
            
            private String formatDuration(long durationMs) {
                long seconds = durationMs / 1000;
                long minutes = seconds / 60;
                seconds = seconds % 60;
                return String.format("%dm %ds", minutes, seconds);
            }
        }
    }
}
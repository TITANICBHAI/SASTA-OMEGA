package com.gestureai.gameautomation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.GameAutomationManager;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.ai.DQNAgent;
import com.gestureai.gameautomation.ai.PPOAgent;
import com.gestureai.gameautomation.ai.AdaptiveDecisionMaker;
import com.gestureai.gameautomation.services.TouchAutomationService;
import com.gestureai.gameautomation.services.ScreenCaptureService;
import com.gestureai.gameautomation.services.GestureRecognitionService;
import com.gestureai.gameautomation.utils.ServiceStartupCoordinator;
import com.gestureai.gameautomation.database.DatabaseIntegrationManager;

public class AutoPlayFragment extends Fragment {
    
    private TextView tvAutomationStatus;
    private Button btnStartAutomation;
    private Button btnStopAutomation;
    private Spinner spinnerStrategy;
    private SeekBar seekBarReactionSpeed;
    private TextView tvReactionSpeedValue;
    private TextView tvActionsCount;
    private TextView tvSuccessRate;
    
    private GameAutomationManager automationManager;
    private CheckBox cbAutoGameDetection;
    private Spinner spinnerGameType;
    private ProgressBar pbModelLoading;
    private TextView tvGameTypeDetected;
    
    // AI Component Integration
    private GameStrategyAgent strategyAgent;
    private DQNAgent dqnAgent;
    private PPOAgent ppoAgent;
    private AdaptiveDecisionMaker decisionMaker;
    private ServiceStartupCoordinator serviceCoordinator;
    private DatabaseIntegrationManager databaseManager;
    
    private boolean isAutomationRunning = false;
    
    // Critical: Background task management to prevent memory leaks
    private java.util.concurrent.ExecutorService backgroundExecutor;
    private final java.util.concurrent.atomic.AtomicBoolean isDestroyed = new java.util.concurrent.atomic.AtomicBoolean(false);
    private final java.util.List<java.util.concurrent.Future<?>> activeTasks = new java.util.concurrent.CopyOnWriteArrayList<>();
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_autoplay, container, false);
        
        initializeViews(view);
        initializeAIComponents();
        setupListeners();
        setupSpinner();
        updateUI();
        
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Critical: Set destroyed flag to stop all background operations
        isDestroyed.set(true);
        
        // Cancel all active background tasks to prevent memory leaks
        cancelAllBackgroundTasks();
        
        // Shutdown background executor
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
            try {
                if (!backgroundExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    backgroundExecutor.shutdownNow();
                    android.util.Log.w("AutoPlayFragment", "Background executor force shutdown");
                }
            } catch (InterruptedException e) {
                backgroundExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Clear view references to prevent memory leaks
        tvAutomationStatus = null;
        btnStartAutomation = null;
        btnStopAutomation = null;
        spinnerStrategy = null;
        seekBarReactionSpeed = null;
        tvReactionSpeedValue = null;
        tvActionsCount = null;
        tvSuccessRate = null;
        cbAutoGameDetection = null;
        spinnerGameType = null;
        pbModelLoading = null;
        tvGameTypeDetected = null;
        
        // Clear automation manager reference
        if (automationManager != null) {
            automationManager.cleanup();
            automationManager = null;
        }
        
        // Clean up AI components
        if (databaseManager != null) {
            databaseManager.stopRealTimeMonitoring();
            databaseManager = null;
        }
        
        // Clear AI component references
        strategyAgent = null;
        dqnAgent = null;
        ppoAgent = null;
        decisionMaker = null;
        serviceCoordinator = null;
    }
    
    /**
     * Critical: Cancel all background tasks to prevent memory leaks
     */
    private void cancelAllBackgroundTasks() {
        for (java.util.concurrent.Future<?> task : activeTasks) {
            if (task != null && !task.isDone()) {
                task.cancel(true);
            }
        }
        activeTasks.clear();
        android.util.Log.d("AutoPlayFragment", "All background tasks cancelled");
    }

    private void initializeViews(View view) {
        if (view == null) {
            throw new IllegalStateException("View is null during initialization");
        }
        
        try {
            tvAutomationStatus = view.findViewById(R.id.tv_automation_status);
            btnStartAutomation = view.findViewById(R.id.btn_start_automation);
            btnStopAutomation = view.findViewById(R.id.btn_stop_automation);
            spinnerStrategy = view.findViewById(R.id.spinner_strategy);
            seekBarReactionSpeed = view.findViewById(R.id.seek_bar_reaction_speed);
            tvReactionSpeedValue = view.findViewById(R.id.tv_reaction_speed_value);
            tvActionsCount = view.findViewById(R.id.tv_actions_count);
            tvSuccessRate = view.findViewById(R.id.tv_success_rate);
            cbAutoGameDetection = view.findViewById(R.id.cb_auto_game_detection);
            spinnerGameType = view.findViewById(R.id.spinner_game_type);
            pbModelLoading = view.findViewById(R.id.pb_model_loading);
            tvGameTypeDetected = view.findViewById(R.id.tv_game_type_detected);
            
            // Validate critical views
            if (tvAutomationStatus == null || btnStartAutomation == null || btnStopAutomation == null) {
                throw new IllegalStateException("Critical views not found in layout");
            }
            
        } catch (Exception e) {
            android.util.Log.e("AutoPlayFragment", "Error initializing views", e);
            // Set fallback state
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Show error state to user
                    if (tvAutomationStatus != null) {
                        tvAutomationStatus.setText("UI initialization failed");
                    }
                });
            }
        }

        seekBarReactionSpeed = view.findViewById(R.id.seekbar_reaction_speed);
        tvReactionSpeedValue = view.findViewById(R.id.tv_reaction_speed_value);
        tvActionsCount = view.findViewById(R.id.tv_actions_count);
        tvSuccessRate = view.findViewById(R.id.tv_success_rate);

        automationManager = GameAutomationManager.getInstance(getContext());
    }
    
    private void setupListeners() {
        btnStartAutomation.setOnClickListener(v -> {
            if (automationManager != null) {
                // Get selected strategy from spinner
                String selectedStrategy = spinnerStrategy.getSelectedItem().toString();
                automationManager.setStrategy(selectedStrategy);
                
                // Get reaction speed from seekbar
                int reactionSpeed = seekBarReactionSpeed.getProgress();
                automationManager.setReactionSpeed(reactionSpeed);
                
                // Start automation with real backend connection
                automationManager.startAutomation();
                updateUI();
            }
        });
        
        btnStopAutomation.setOnClickListener(v -> {
            if (automationManager != null) {
                automationManager.stopAutomation();
                updateUI();
            }
        });
        
        seekBarReactionSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvReactionSpeedValue.setText(progress + "%");
                if (automationManager != null && fromUser) {
                    automationManager.setReactionSpeed(progress);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Add game type selection listener
        if (spinnerGameType != null) {
            spinnerGameType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                    String gameType = parent.getItemAtPosition(position).toString();
                    if (automationManager != null) {
                        automationManager.setGameType(gameType);
                        if ("Auto-Detect".equals(gameType)) {
                            automationManager.enableAutoGameDetection(true);
                        } else {
                            automationManager.enableAutoGameDetection(false);
                        }
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        // Add auto detection checkbox listener
        if (cbAutoGameDetection != null) {
            cbAutoGameDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (automationManager != null) {
                    automationManager.enableAutoGameDetection(isChecked);
                    spinnerGameType.setEnabled(!isChecked);
                    if (isChecked) {
                        pbModelLoading.setVisibility(android.view.View.VISIBLE);
                        automationManager.startGameTypeDetection();
                    } else {
                        pbModelLoading.setVisibility(android.view.View.GONE);
                    }
                }
            });
        }
    }

    private void setupSpinner() {
        // Game type selection
        String[] gameTypes = {"Auto-Detect", "Battle Royale", "MOBA", "FPS", "Arcade", "RPG"};
        ArrayAdapter<String> gameAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, gameTypes);
        gameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGameType.setAdapter(gameAdapter);

        // Strategy selection
        String[] strategies = {"Adaptive AI", "Aggressive", "Defensive", "Balanced", "Custom"};
        ArrayAdapter<String> strategyAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, strategies);
        strategyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStrategy.setAdapter(strategyAdapter);
    }
    
    private void updateUI() {
        if (automationManager != null) {
            boolean isRunning = automationManager.isRunning();
            tvAutomationStatus.setText(isRunning ? R.string.status_active : R.string.status_inactive);
            btnStartAutomation.setEnabled(!isRunning);
            btnStopAutomation.setEnabled(isRunning);
        }
    }
    
    private void startAutomationWithAI() {
        if (isAutomationRunning) {
            android.util.Log.w("AutoPlayFragment", "Automation already running");
            return;
        }
        
        tvAutomationStatus.setText("Starting automation...");
        
        // Start services with coordination
        if (serviceCoordinator != null) {
            serviceCoordinator.startServicesWithCoordination(new ServiceStartupCoordinator.ServiceReadyCallback() {
                @Override
                public void onAllServicesReady() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // Enable AI components
                            enableAIComponents();
                            
                            // Start automation manager
                            if (automationManager == null) {
                                automationManager = new GameAutomationManager(getContext());
                            }
                            automationManager.startAutomation();
                            
                            // Update UI
                            isAutomationRunning = true;
                            tvAutomationStatus.setText("Automation Active - AI Enabled");
                            btnStartAutomation.setEnabled(false);
                            btnStopAutomation.setEnabled(true);
                            
                            // Start real-time monitoring
                            if (databaseManager != null) {
                                databaseManager.startRealTimeMonitoring();
                            }
                            
                            // Start new session tracking
                            startNewSession();
                            
                            android.util.Log.d("AutoPlayFragment", "Automation started with AI integration");
                        });
                    }
                }
                
                @Override
                public void onServiceFailed(String serviceName, String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            tvAutomationStatus.setText("Service failed: " + serviceName);
                            android.util.Log.e("AutoPlayFragment", "Service startup failed: " + serviceName + " - " + error);
                        });
                    }
                }
            });
        } else {
            tvAutomationStatus.setText("Service coordinator not available");
        }
    }
    
    private void stopAutomationWithAI() {
        if (!isAutomationRunning) {
            android.util.Log.w("AutoPlayFragment", "Automation not running");
            return;
        }
        
        tvAutomationStatus.setText("Stopping automation...");
        
        try {
            // Disable AI components
            disableAIComponents();
            
            // Stop automation manager
            if (automationManager != null) {
                automationManager.stopAutomation();
            }
            
            // Stop real-time monitoring
            if (databaseManager != null) {
                databaseManager.stopRealTimeMonitoring();
            }
            
            // End current session
            endCurrentSession();
            
            // Update UI
            isAutomationRunning = false;
            tvAutomationStatus.setText("Automation Stopped");
            btnStartAutomation.setEnabled(true);
            btnStopAutomation.setEnabled(false);
            
            android.util.Log.d("AutoPlayFragment", "Automation stopped");
            
        } catch (Exception e) {
            android.util.Log.e("AutoPlayFragment", "Error stopping automation", e);
            tvAutomationStatus.setText("Error stopping automation");
        }
    }
    
    private void enableAIComponents() {
        try {
            if (strategyAgent != null) {
                strategyAgent.setActive(true);
            }
            if (decisionMaker != null) {
                decisionMaker.setLearningEnabled(true);
            }
            
            // Configure AI based on selected strategy
            String selectedStrategy = spinnerStrategy.getSelectedItem().toString();
            configureAIStrategy(selectedStrategy);
            
        } catch (Exception e) {
            android.util.Log.e("AutoPlayFragment", "Error enabling AI components", e);
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
        } catch (Exception e) {
            android.util.Log.e("AutoPlayFragment", "Error disabling AI components", e);
        }
    }
    
    private void configureAIStrategy(String strategy) {
        try {
            if (strategyAgent != null) {
                switch (strategy) {
                    case "Aggressive":
                        strategyAgent.setAggressiveness(0.8f);
                        break;
                    case "Defensive":
                        strategyAgent.setAggressiveness(0.3f);
                        break;
                    case "Balanced":
                        strategyAgent.setAggressiveness(0.5f);
                        break;
                    case "Adaptive AI":
                        // Let AI decide based on game state
                        if (decisionMaker != null) {
                            decisionMaker.enableAdaptiveStrategy(true);
                        }
                        break;
                }
            }
        } catch (Exception e) {
            android.util.Log.e("AutoPlayFragment", "Error configuring AI strategy", e);
        }
    }
    
    private void startNewSession() {
        try {
            if (databaseManager != null) {
                // Create new session data
                com.gestureai.gameautomation.data.SessionData newSession = 
                    new com.gestureai.gameautomation.data.SessionData();
                newSession.setGameType(spinnerGameType.getSelectedItem().toString());
                newSession.setTimestamp(System.currentTimeMillis());
                newSession.setSessionDuration(0);
                newSession.setTotalActions(0);
                newSession.setSuccessRate(0.0f);
                newSession.setAverageReactionTime(0.0f);
                
                // Save to database
                databaseManager.saveSessionData(newSession);
            }
        } catch (Exception e) {
            android.util.Log.e("AutoPlayFragment", "Error starting new session", e);
        }
    }
    
    private void endCurrentSession() {
        try {
            if (databaseManager != null) {
                // Get latest session and update with final metrics
                databaseManager.getLatestSession().thenAccept(session -> {
                    if (session != null) {
                        // Update session with performance data
                        if (strategyAgent != null) {
                            session.setSuccessRate(strategyAgent.getCurrentSuccessRate());
                        }
                        session.setSessionDuration(System.currentTimeMillis() - session.getTimestamp());
                        
                        // Save updated session
                        databaseManager.saveSessionData(session);
                    }
                });
            }
        } catch (Exception e) {
            android.util.Log.e("AutoPlayFragment", "Error ending session", e);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }
}
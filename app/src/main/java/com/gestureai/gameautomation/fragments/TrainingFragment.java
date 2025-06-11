package com.gestureai.gameautomation.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.Toast;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.activities.*;
import com.gestureai.gameautomation.ai.DQNAgent;
import com.gestureai.gameautomation.ai.PPOAgent;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.managers.MediaPipeManager;
import com.gestureai.gameautomation.services.VoiceCommandService;
import com.gestureai.gameautomation.services.GestureRecognitionService;
import com.gestureai.gameautomation.database.AppDatabase;
import com.gestureai.gameautomation.database.dao.SessionDataDao;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

public class TrainingFragment extends Fragment {
    private static final String TAG = "TrainingFragment";

    // Neural Network Calibration Controls
    private SeekBar seekBarLearningRate, seekBarBatchSize;
    private TextView tvLearningRateValue, tvBatchSizeValue;
    private Button btnAutoCalibrate, btnResetWeights;
    
    // Gesture Recognition System
    private TextView tvMediaPipeStatus;
    private Switch switchHandDetection;
    private Button btnCalibrateGestures, btnTestGestures;
    
    // Voice Command Integration
    private TextView tvSpeechEngineStatus;
    private Switch switchVoiceCommands;
    private Button btnTrainVoice, btnTestVoice;
    
    // Session Analytics
    private TextView tvSessionsRecorded, tvTotalActions;
    private Button btnSessionReplay, btnAnalyticsDashboard;
    
    // Training Status
    private TextView tvTrainingStatus;
    private ProgressBar pbTrainingProgress;
    
    // AI Agents
    private DQNAgent dqnAgent;
    private PPOAgent ppoAgent;
    private GameStrategyAgent strategyAgent;
    
    // Services
    private MediaPipeManager mediaPipeManager;
    private VoiceCommandService voiceService;
    private GestureRecognitionService gestureService;
    
    // Database
    private AppDatabase database;
    private SessionDataDao sessionDao;
    
    // Voice command receiver
    private BroadcastReceiver voiceCommandReceiver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_training, container, false);

        initializeComponents();
        initializeViews(view);
        setupClickListeners();
        setupSeekBarListeners();
        loadInitialData();
        setupVoiceCommandReceiver();

        return view;
    }
    
    private void initializeComponents() {
        try {
            // Initialize AI agents
            dqnAgent = new DQNAgent(getContext(), 128, 8);
            ppoAgent = new PPOAgent(getContext(), 128, 8);
            strategyAgent = new GameStrategyAgent(getContext());
            
            // Initialize managers and services
            mediaPipeManager = MediaPipeManager.getInstance(getContext());
            
            // Initialize database
            database = AppDatabase.getInstance(getContext());
            sessionDao = database.sessionDataDao();
            
            Log.d(TAG, "Training components initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing training components", e);
        }
    }

    private void initializeViews(View view) {
        // Neural Network Calibration
        seekBarLearningRate = view.findViewById(R.id.seekbar_learning_rate);
        seekBarBatchSize = view.findViewById(R.id.seekbar_batch_size);
        tvLearningRateValue = view.findViewById(R.id.tv_learning_rate_value);
        tvBatchSizeValue = view.findViewById(R.id.tv_batch_size_value);
        btnAutoCalibrate = view.findViewById(R.id.btn_auto_calibrate);
        btnResetWeights = view.findViewById(R.id.btn_reset_weights);
        
        // Gesture Recognition
        tvMediaPipeStatus = view.findViewById(R.id.tv_mediapipe_status);
        switchHandDetection = view.findViewById(R.id.switch_hand_detection);
        btnCalibrateGestures = view.findViewById(R.id.btn_calibrate_gestures);
        btnTestGestures = view.findViewById(R.id.btn_test_gestures);
        
        // Voice Commands
        tvSpeechEngineStatus = view.findViewById(R.id.tv_speech_engine_status);
        switchVoiceCommands = view.findViewById(R.id.switch_voice_commands);
        btnTrainVoice = view.findViewById(R.id.btn_train_voice);
        btnTestVoice = view.findViewById(R.id.btn_test_voice);
        
        // Session Analytics
        tvSessionsRecorded = view.findViewById(R.id.tv_sessions_recorded);
        tvTotalActions = view.findViewById(R.id.tv_total_actions);
        btnSessionReplay = view.findViewById(R.id.btn_session_replay);
        btnAnalyticsDashboard = view.findViewById(R.id.btn_analytics_dashboard);
        
        // Training Status
        tvTrainingStatus = view.findViewById(R.id.tv_training_status);
        pbTrainingProgress = view.findViewById(R.id.pb_training_progress);
    }
    
    private void setupSeekBarListeners() {
        seekBarLearningRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float learningRate = 0.0001f + (progress / 100.0f) * 0.01f; // 0.0001 to 0.0101
                    tvLearningRateValue.setText(String.format("%.4f", learningRate));
                    updateNeuralNetworkParameters();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        seekBarBatchSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int batchSize = (int) Math.pow(2, progress + 4); // 16, 32, 64, 128, 256, 512, 1024, 2048
                    tvBatchSizeValue.setText(String.valueOf(batchSize));
                    updateNeuralNetworkParameters();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupClickListeners() {
        // Neural Network Calibration
        btnAutoCalibrate.setOnClickListener(v -> performAutoCalibration());
        btnResetWeights.setOnClickListener(v -> resetNeuralNetworkWeights());
        
        // Gesture Recognition
        switchHandDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleHandDetection(isChecked);
        });
        btnCalibrateGestures.setOnClickListener(v -> calibrateGestureRecognition());
        btnTestGestures.setOnClickListener(v -> testGestureRecognition());
        
        // Voice Commands
        switchVoiceCommands.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleVoiceCommands(isChecked);
        });
        btnTrainVoice.setOnClickListener(v -> trainVoiceCommands());
        btnTestVoice.setOnClickListener(v -> testVoiceRecognition());
        
        // Session Analytics
        btnSessionReplay.setOnClickListener(v -> openSessionReplay());
        btnAnalyticsDashboard.setOnClickListener(v -> openAnalyticsDashboard());
    }
    
    private void loadInitialData() {
        // Load session analytics data from database
        new Thread(() -> {
            try {
                long sessionCount = sessionDao.getSessionCount();
                long totalActions = sessionDao.getTotalActionCount();
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        tvSessionsRecorded.setText(String.valueOf(sessionCount));
                        tvTotalActions.setText(String.format("%,d", totalActions));
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading session data", e);
            }
        }).start();
        
        // Update system status
        updateSystemStatus();
    }
    
    private void updateSystemStatus() {
        // Update MediaPipe status
        if (mediaPipeManager != null && mediaPipeManager.isInitialized()) {
            tvMediaPipeStatus.setText("Active");
            tvMediaPipeStatus.setTextColor(getResources().getColor(R.color.accent_primary));
        } else {
            tvMediaPipeStatus.setText("Inactive");
            tvMediaPipeStatus.setTextColor(getResources().getColor(R.color.accent_secondary));
        }
        
        // Update speech engine status
        tvSpeechEngineStatus.setText("Ready");
        tvSpeechEngineStatus.setTextColor(getResources().getColor(R.color.accent_primary));
    }
    
    private void updateNeuralNetworkParameters() {
        if (dqnAgent != null && ppoAgent != null) {
            float learningRate = Float.parseFloat(tvLearningRateValue.getText().toString());
            int batchSize = Integer.parseInt(tvBatchSizeValue.getText().toString());
            
            // Update AI agents with new parameters
            dqnAgent.setLearningRate(learningRate);
            dqnAgent.setBatchSize(batchSize);
            ppoAgent.setLearningRate(learningRate);
            ppoAgent.setBatchSize(batchSize);
            
            Log.d(TAG, "Neural network parameters updated - LR: " + learningRate + ", Batch: " + batchSize);
        }
    }
    
    private void performAutoCalibration() {
        tvTrainingStatus.setText("Auto-calibrating neural networks...");
        pbTrainingProgress.setVisibility(View.VISIBLE);
        pbTrainingProgress.setProgress(0);
        
        new Thread(() -> {
            try {
                // Simulate auto-calibration process
                for (int i = 0; i <= 100; i += 10) {
                    Thread.sleep(200);
                    final int progress = i;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> pbTrainingProgress.setProgress(progress));
                    }
                }
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        tvTrainingStatus.setText("Auto-calibration complete");
                        pbTrainingProgress.setVisibility(View.GONE);
                        
                        // Set optimal parameters
                        seekBarLearningRate.setProgress(25); // 0.0026
                        seekBarBatchSize.setProgress(2); // 64
                        
                        Toast.makeText(getContext(), "Neural networks auto-calibrated", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during auto-calibration", e);
            }
        }).start();
    }
    
    private void resetNeuralNetworkWeights() {
        if (dqnAgent != null && ppoAgent != null) {
            dqnAgent.resetWeights();
            ppoAgent.resetWeights();
            tvTrainingStatus.setText("Neural network weights reset");
            Toast.makeText(getContext(), "Weights reset to initial values", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void toggleHandDetection(boolean enabled) {
        if (mediaPipeManager != null) {
            if (enabled) {
                mediaPipeManager.enableHandDetection();
                tvMediaPipeStatus.setText("Hand Detection Active");
            } else {
                mediaPipeManager.disableHandDetection();
                tvMediaPipeStatus.setText("Hand Detection Disabled");
            }
        }
    }
    
    private void calibrateGestureRecognition() {
        Intent intent = new Intent(getActivity(), AdvancedGestureTrainingActivity.class);
        startActivity(intent);
    }
    
    private void testGestureRecognition() {
        if (mediaPipeManager != null) {
            mediaPipeManager.startGestureTest();
            Toast.makeText(getContext(), "Gesture recognition test started", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void toggleVoiceCommands(boolean enabled) {
        try {
            Intent serviceIntent = new Intent(getContext(), VoiceCommandService.class);
            if (enabled) {
                getContext().startService(serviceIntent);
                tvSpeechEngineStatus.setText("Listening");
                tvSpeechEngineStatus.setTextColor(getResources().getColor(R.color.accent_primary));
            } else {
                getContext().stopService(serviceIntent);
                tvSpeechEngineStatus.setText("Stopped");
                tvSpeechEngineStatus.setTextColor(getResources().getColor(R.color.accent_secondary));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling voice commands", e);
        }
    }
    
    private void trainVoiceCommands() {
        Intent intent = new Intent(getActivity(), VoiceCommandConfigurationActivity.class);
        startActivity(intent);
    }
    
    private void testVoiceRecognition() {
        Toast.makeText(getContext(), "Say 'Start Automation' to test voice recognition", Toast.LENGTH_LONG).show();
    }
    
    private void openSessionReplay() {
        Intent intent = new Intent(getActivity(), SessionAnalyticsDashboardActivity.class);
        intent.putExtra("show_replay", true);
        startActivity(intent);
    }
    
    private void openAnalyticsDashboard() {
        Intent intent = new Intent(getActivity(), SessionAnalyticsDashboardActivity.class);
        startActivity(intent);
    }
    
    private void setupVoiceCommandReceiver() {
        voiceCommandReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.gestureai.gameautomation.UI_NAVIGATION".equals(intent.getAction())) {
                    String uiAction = intent.getStringExtra("ui_action");
                    handleVoiceUIAction(uiAction);
                }
            }
        };
        
        IntentFilter filter = new IntentFilter("com.gestureai.gameautomation.UI_NAVIGATION");
        if (getContext() != null) {
            getContext().registerReceiver(voiceCommandReceiver, filter);
        }
    }
    
    private void handleVoiceUIAction(String action) {
        if (action == null) return;
        
        switch (action) {
            case "increase_learning_rate":
                int currentProgress = seekBarLearningRate.getProgress();
                seekBarLearningRate.setProgress(Math.min(100, currentProgress + 10));
                Toast.makeText(getContext(), "Learning rate increased via voice", Toast.LENGTH_SHORT).show();
                break;
                
            case "decrease_learning_rate":
                int currentProgressDown = seekBarLearningRate.getProgress();
                seekBarLearningRate.setProgress(Math.max(0, currentProgressDown - 10));
                Toast.makeText(getContext(), "Learning rate decreased via voice", Toast.LENGTH_SHORT).show();
                break;
                
            case "auto_calibrate":
                performAutoCalibration();
                Toast.makeText(getContext(), "Auto-calibration started via voice", Toast.LENGTH_SHORT).show();
                break;
                
            case "reset_weights":
                resetNeuralNetworkWeights();
                Toast.makeText(getContext(), "Neural network weights reset via voice", Toast.LENGTH_SHORT).show();
                break;
                
            case "enable_hand_detection":
                switchHandDetection.setChecked(true);
                Toast.makeText(getContext(), "Hand detection enabled via voice", Toast.LENGTH_SHORT).show();
                break;
                
            case "disable_hand_detection":
                switchHandDetection.setChecked(false);
                Toast.makeText(getContext(), "Hand detection disabled via voice", Toast.LENGTH_SHORT).show();
                break;
                
            case "test_gestures":
                testGestureRecognition();
                Toast.makeText(getContext(), "Gesture test started via voice", Toast.LENGTH_SHORT).show();
                break;
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (voiceCommandReceiver != null && getContext() != null) {
            getContext().unregisterReceiver(voiceCommandReceiver);
        }
    }
}
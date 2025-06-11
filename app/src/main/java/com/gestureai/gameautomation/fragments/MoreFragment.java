package com.gestureai.gameautomation.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.activities.*;
import com.gestureai.gameautomation.MainActivity;

public class MoreFragment extends Fragment {

    private Button btnGameStrategyConfig;
    private Button btnSettings;
    private Button btnAdvancedSettings;
    private Button btnScreenMonitor;
    private Button btnGestureController;
    private Button btnObjectLabeling;
    private Button btnNeuralNetworkTraining;
    private Button btnVoiceCommandConfig;
    private Button btnAdvancedDebugging;
    private Button btnAITrainingDashboard;
    private Button btnComprehensiveAnalytics;
    private Button btnGestureTraining;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        initializeViews(view);
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        btnGameStrategyConfig = view.findViewById(R.id.btn_game_strategy_config);
        btnSettings = view.findViewById(R.id.btn_settings);
        btnAdvancedSettings = view.findViewById(R.id.btn_advanced_settings);
        btnScreenMonitor = view.findViewById(R.id.btn_screen_monitor);
        btnGestureController = view.findViewById(R.id.btn_gesture_controller);
        btnObjectLabeling = view.findViewById(R.id.btn_object_labeling);
        btnNeuralNetworkTraining = view.findViewById(R.id.btn_neural_network_training);
        btnVoiceCommandConfig = view.findViewById(R.id.btn_voice_command_config);
        btnAdvancedDebugging = view.findViewById(R.id.btn_advanced_debugging);
        btnAITrainingDashboard = view.findViewById(R.id.btn_ai_training_dashboard);
        btnComprehensiveAnalytics = view.findViewById(R.id.btn_comprehensive_analytics);
        btnGestureTraining = view.findViewById(R.id.btn_gesture_training);
    }

    private void setupClickListeners() {
        btnGameStrategyConfig.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GameStrategyConfigActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });

        btnAdvancedSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AdvancedSettingsActivity.class);
            startActivity(intent);
        });

        btnScreenMonitor.setOnClickListener(v -> {
            // Switch to screen monitor fragment in main activity
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToFragment(new ScreenMonitorFragment());
            }
        });

        btnGestureController.setOnClickListener(v -> {
            // Switch to gesture controller fragment in main activity
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToFragment(new GestureControllerFragment());
            }
        });

        btnObjectLabeling.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ObjectLabelingActivity.class);
            startActivity(intent);
        });

        btnNeuralNetworkTraining.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NeuralNetworkTrainingActivity.class);
            startActivity(intent);
        });

        btnVoiceCommandConfig.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), VoiceCommandConfigurationActivity.class);
            startActivity(intent);
        });

        btnAdvancedDebugging.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AdvancedDebuggingToolsActivity.class);
            startActivity(intent);
        });

        btnAITrainingDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AITrainingDashboardActivity.class);
            startActivity(intent);
        });

        btnComprehensiveAnalytics.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ComprehensiveAnalyticsActivity.class);
            startActivity(intent);
        });

        btnGestureTraining.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GestureTrainingActivity.class);
            startActivity(intent);
        });

        // Add new dashboard access points
        Button btnPerformanceDashboard = view.findViewById(R.id.btn_performance_dashboard);
        Button btnTrainingProgress = view.findViewById(R.id.btn_training_progress);
        Button btnGameConfiguration = view.findViewById(R.id.btn_game_configuration);
        Button btnGestureSequence = view.findViewById(R.id.btn_gesture_sequence);
        Button btnSessionAnalytics = view.findViewById(R.id.btn_session_analytics);
        Button btnAIComparison = view.findViewById(R.id.btn_ai_comparison);
        Button btnDebugOverlay = view.findViewById(R.id.btn_debug_overlay);

        btnPerformanceDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RealTimePerformanceDashboardActivity.class);
            startActivity(intent);
        });

        btnTrainingProgress.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TrainingProgressVisualizerActivity.class);
            startActivity(intent);
        });

        btnGameConfiguration.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GameConfigurationWizardActivity.class);
            startActivity(intent);
        });

        btnGestureSequence.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GestureSequenceBuilderActivity.class);
            startActivity(intent);
        });

        btnSessionAnalytics.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SessionAnalyticsDashboardActivity.class);
            startActivity(intent);
        });

        btnAIComparison.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AIPerformanceComparisonActivity.class);
            startActivity(intent);
        });

        btnDebugOverlay.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), DebugOverlayService.class);
            getContext().startService(intent);
            Toast.makeText(getContext(), "Debug overlay started", Toast.LENGTH_SHORT).show();
        });
    }
}
package com.gestureai.gameautomation.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.services.TouchAutomationService;
import com.gestureai.gameautomation.services.ScreenCaptureService;
import com.gestureai.gameautomation.services.GestureRecognitionService;
import com.gestureai.gameautomation.activities.SettingsActivity;
import com.gestureai.gameautomation.managers.AIStackManager;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.ai.DQNAgent;
import com.gestureai.gameautomation.ai.PPOAgent;
import com.gestureai.gameautomation.ai.PerformanceTracker;
import com.gestureai.gameautomation.MainActivity;

public class DashboardFragment extends Fragment {

    private TextView tvAiServiceStatus;
    private TextView tvAccessibilityStatus;
    private TextView tvScreenCaptureStatus;
    private TextView tvSuccessRate;
    private TextView tvReactionTime;
    private TextView tvTotalActions;
    private TextView tvSessionTime;
    private Button btnStartAutomation;
    private Button btnOpenSettings;
    
    private Handler updateHandler;
    private Runnable updateRunnable;
    private boolean isResumed = false;
    
    // AI Component References for real-time monitoring
    private GameStrategyAgent strategyAgent;
    private DQNAgent dqnAgent;
    private PPOAgent ppoAgent;
    private PerformanceTracker performanceTracker;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        
        initializeViews(view);
        initializeAIComponents();
        setupListeners();
        startStatusUpdates();
        
        return view;
    }

    private void initializeViews(View view) {
        tvAiServiceStatus = view.findViewById(R.id.tv_ai_service_status);
        tvAccessibilityStatus = view.findViewById(R.id.tv_accessibility_status);
        tvScreenCaptureStatus = view.findViewById(R.id.tv_screen_capture_status);
        tvSuccessRate = view.findViewById(R.id.tv_success_rate);
        tvReactionTime = view.findViewById(R.id.tv_reaction_time);
        tvTotalActions = view.findViewById(R.id.tv_total_actions);
        tvSessionTime = view.findViewById(R.id.tv_session_time);
        btnStartAutomation = view.findViewById(R.id.btn_start_automation);
        btnOpenSettings = view.findViewById(R.id.btn_open_settings);
    }
    
    private void initializeAIComponents() {
        try {
            if (getContext() != null) {
                // Get AI component instances for monitoring
                strategyAgent = new GameStrategyAgent(getContext());
                dqnAgent = DQNAgent.getInstance();
                ppoAgent = PPOAgent.getInstance();
                performanceTracker = PerformanceTracker.getInstance(getContext());
            }
        } catch (Exception e) {
            android.util.Log.e("DashboardFragment", "Error initializing AI components for monitoring", e);
        }
    }

    private void setupListeners() {
        btnStartAutomation.setOnClickListener(v -> {
            // Switch to AutoPlay fragment
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToFragment(1); // AutoPlay is position 1
            }
        });

        btnOpenSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void startStatusUpdates() {
        updateHandler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isResumed) {
                    updateServiceStatuses();
                    updatePerformanceMetrics();
                    updateHandler.postDelayed(this, 2000); // Update every 2 seconds
                }
            }
        };
        updateHandler.post(updateRunnable);
    }
    
    private void updateServiceStatuses() {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            // Update AI Service Status
            if (strategyAgent != null && strategyAgent.isActive()) {
                tvAiServiceStatus.setText("AI Agent: Active");
            } else {
                tvAiServiceStatus.setText("AI Agent: Inactive");
            }
            
            // Update Accessibility Service Status
            TouchAutomationService touchService = TouchAutomationService.getInstanceSafe();
            if (touchService != null) {
                tvAccessibilityStatus.setText("Accessibility: Connected");
            } else {
                tvAccessibilityStatus.setText("Accessibility: Disconnected");
            }
            
            // Update Screen Capture Status
            ScreenCaptureService screenService = ScreenCaptureService.getInstanceSafe();
            if (screenService != null) {
                tvScreenCaptureStatus.setText("Screen Capture: Active");
            } else {
                tvScreenCaptureStatus.setText("Screen Capture: Inactive");
            }
        });
    }
    
    private void updatePerformanceMetrics() {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            try {
                // Update Success Rate from AI performance
                if (performanceTracker != null) {
                    float successRate = performanceTracker.getOverallSuccessRate();
                    tvSuccessRate.setText(String.format("%.1f%%", successRate * 100));
                    
                    float avgReactionTime = performanceTracker.getAverageReactionTime();
                    tvReactionTime.setText(String.format("%.0f ms", avgReactionTime));
                    
                    int totalActions = performanceTracker.getTotalActionsPerformed();
                    tvTotalActions.setText(String.valueOf(totalActions));
                    
                    long sessionTime = performanceTracker.getCurrentSessionDuration();
                    tvSessionTime.setText(formatSessionTime(sessionTime));
                } else {
                    // Fallback values when performance tracker not available
                    tvSuccessRate.setText("--");
                    tvReactionTime.setText("--");
                    tvTotalActions.setText("0");
                    tvSessionTime.setText("00:00");
                }
            } catch (Exception e) {
                android.util.Log.e("DashboardFragment", "Error updating performance metrics", e);
            }
        });
    }
    
    private String formatSessionTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
        } else {
            return String.format("%02d:%02d", minutes, seconds % 60);
        }
    }
    
    private void continueUpdateLoop() {
        updateHandler.postDelayed(updateRunnable, 2000);
                    updateSystemStatus();
                    updatePerformanceMetrics();
                }
                updateHandler.postDelayed(this, 2000); // Update every 2 seconds
            }
        };
        updateHandler.post(updateRunnable);
    }

    private void updateSystemStatus() {
        try {
            // Check AI Stack Manager status
            boolean aiStackReady = AIStackManager.getInstance().isInitialized();
            tvAiServiceStatus.setText(aiStackReady ? R.string.status_active : R.string.status_inactive);
            tvAiServiceStatus.setTextColor(getResources().getColor(
                aiStackReady ? R.color.status_ready : R.color.status_inactive, null));

            // Check Accessibility Service status
            TouchAutomationService accessibilityService = TouchAutomationService.getInstance();
            boolean accessibilityReady = accessibilityService != null;
            tvAccessibilityStatus.setText(accessibilityReady ? R.string.status_active : R.string.status_inactive);
            tvAccessibilityStatus.setTextColor(getResources().getColor(
                accessibilityReady ? R.color.status_ready : R.color.status_inactive, null));

            // Screen capture status (simplified check)
            tvScreenCaptureStatus.setText(R.string.status_inactive);
            tvScreenCaptureStatus.setTextColor(getResources().getColor(R.color.status_inactive, null));

        } catch (Exception e) {
            // Handle any exceptions during status updates
        }
    }

    private void updatePerformanceMetrics() {
        try {
            // Update with sample data for now - in real implementation, 
            // these would come from PerformanceTracker
            tvSuccessRate.setText("92.5%");
            tvReactionTime.setText("145ms");
            tvTotalActions.setText("1,247");
            tvSessionTime.setText("23m 15s");
            
        } catch (Exception e) {
            // Handle any exceptions during metrics updates
            tvSuccessRate.setText("--");
            tvReactionTime.setText("--");
            tvTotalActions.setText("--");
            tvSessionTime.setText("--");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isResumed = true;
        updateSystemStatus();
        updatePerformanceMetrics();
    }

    @Override
    public void onPause() {
        super.onPause();
        isResumed = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }
}
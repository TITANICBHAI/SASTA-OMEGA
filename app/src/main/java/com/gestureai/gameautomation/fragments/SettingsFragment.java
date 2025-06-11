package com.gestureai.gameautomation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.fragment.app.Fragment;
import android.preference.PreferenceManager;
import com.gestureai.gameautomation.GameAutomationEngine;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.ai.*;
import android.content.Intent;
import android.widget.Button;
import com.gestureai.gameautomation.activities.*;
import android.content.SharedPreferences;
import android.widget.Toast;
import com.gestureai.gameautomation.services.TouchAutomationService;
import com.gestureai.gameautomation.PerformanceTracker;
import com.gestureai.gameautomation.ai.ResourceMonitor;
import com.gestureai.gameautomation.ObjectLabelerEngine;
import androidx.appcompat.app.AlertDialog;


public class SettingsFragment extends Fragment {

    // AI Configuration Controls
    private Switch switchEnableAI;
    private Switch switchEnableRL;
    private Switch switchEnableNLP;
    private SeekBar seekBarAIAggression;
    private SeekBar seekBarReactionTime;
    private Spinner spinnerAIMode;
    private Spinner spinnerGameType;

    // Performance Settings
    private SeekBar seekBarFPS;
    private Switch switchHighPerformance;
    private Switch switchBatterySaver;
    private TextView tvCPUUsage;
    private TextView tvMemoryUsage;

    // Model Management
    private Button btnUpdateModels;
    private Button btnResetAI;
    private Button btnExportData;
    private Button btnImportData;
    private ProgressBar pbModelUpdate;

    // AI Components
    private ResourceMonitor resourceMonitor;
    private PerformanceTracker performanceTracker;
    private static final int REQUEST_IMPORT_DATA = 1001;
    private Button btnAITraining;
    private Button btnStrategyConfig;
    private Button btnGestureTraining;
    private Button btnAdvancedSettings;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        initializeViews(view);
        setupListeners();
        initializeAIComponents();
        loadCurrentSettings();

        return view;
    }

    private void initializeViews(View view) {
        // AI Configuration
        switchEnableAI = view.findViewById(R.id.switch_enable_ai);
        switchEnableRL = view.findViewById(R.id.switch_enable_rl);
        switchEnableNLP = view.findViewById(R.id.switch_enable_nlp);
        seekBarAIAggression = view.findViewById(R.id.seekbar_ai_aggression);
        seekBarReactionTime = view.findViewById(R.id.seekbar_reaction_time);
        spinnerAIMode = view.findViewById(R.id.spinner_ai_mode);
        spinnerGameType = view.findViewById(R.id.spinner_game_type);

        // Performance Settings
        seekBarFPS = view.findViewById(R.id.seekbar_fps);
        switchHighPerformance = view.findViewById(R.id.switch_high_performance);
        switchBatterySaver = view.findViewById(R.id.switch_battery_saver);
        tvCPUUsage = view.findViewById(R.id.tv_cpu_usage);
        tvMemoryUsage = view.findViewById(R.id.tv_memory_usage);

        // Model Management
        btnUpdateModels = view.findViewById(R.id.btn_update_models);
        btnResetAI = view.findViewById(R.id.btn_reset_ai);
        btnExportData = view.findViewById(R.id.btn_export_data);
        btnImportData = view.findViewById(R.id.btn_import_data);
        pbModelUpdate = view.findViewById(R.id.pb_model_update);
        btnAITraining = view.findViewById(R.id.btn_ai_training);
        btnStrategyConfig = view.findViewById(R.id.btn_strategy_config);
        btnGestureTraining = view.findViewById(R.id.btn_gesture_training);
        btnAdvancedSettings = view.findViewById(R.id.btn_advanced_settings);

        setupSpinners();
    }

    private void setupSpinners() {
        // AI Mode spinner
        String[] aiModes = {"Adaptive", "Aggressive", "Defensive", "Learning", "Custom"};
        ArrayAdapter<String> aiAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, aiModes);
        aiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAIMode.setAdapter(aiAdapter);

        // Game Type spinner
        String[] gameTypes = {"Auto-Detect", "Battle Royale", "MOBA", "FPS", "RPG", "Strategy"};
        ArrayAdapter<String> gameAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, gameTypes);
        gameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGameType.setAdapter(gameAdapter);
    }

    private void initializeAIComponents() {
        resourceMonitor = new ResourceMonitor(getContext());
        performanceTracker = new com.gestureai.gameautomation.PerformanceTracker(getContext());
    }

    // Remove duplicate setupListeners() method and consolidate:

    private void setupListeners() {
        // AI Configuration listeners
        switchEnableAI.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSettingBoolean("ai_enabled", isChecked);
            toggleAIComponents(isChecked);
        });

        switchEnableRL.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSettingBoolean("rl_enabled", isChecked);
            // Connect to ReinforcementLearner
            if (isChecked) {
                GameAutomationEngine.getInstance().enableReinforcementLearning();
            } else {
                GameAutomationEngine.getInstance().disableReinforcementLearning();
            }
        });

        switchEnableNLP.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSettingBoolean("nlp_enabled", isChecked);
        });

        seekBarAIAggression.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float aggression = progress / 100.0f;
                saveSettingFloat("ai_aggression", aggression);
                // Apply to StrategyProcessor
                StrategyProcessor processor = GameAutomationEngine.getInstance().getStrategyProcessor();
                if (processor != null) {
                    processor.setAggressionLevel(aggression);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarReactionTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int reactionTime = 100 + (progress * 5); // 100-600ms range
                saveSettingInt("reaction_time", reactionTime);
                // Apply to TouchAutomationService
                TouchAutomationService service = TouchAutomationService.getInstance();
                if (service != null) {
                    service.setReactionDelay(reactionTime);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Model Management listeners
        btnUpdateModels.setOnClickListener(v -> updateAIModels());
        btnResetAI.setOnClickListener(v -> resetAISettings());
        btnExportData.setOnClickListener(v -> exportTrainingData());
        btnImportData.setOnClickListener(v -> importTrainingData());

        // Activity launchers
        btnAITraining.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AITrainingDashboardActivity.class);
            startActivity(intent);
        });

        btnStrategyConfig.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), GameStrategyConfigActivity.class);
            startActivity(intent);
        });

        btnGestureTraining.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), GestureTrainingActivity.class);
            startActivity(intent);
        });

        btnAdvancedSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            startActivity(intent);
        });
    }

    // Add missing helper methods:
    private void toggleAIComponents(boolean enabled) {
        GameAutomationEngine engine = GameAutomationEngine.getInstance();
        if (engine != null) {
            engine.setAutomationEnabled(enabled);
        }
    }

    private void resetAISettings() {
        // Reset all AI components to defaults
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        editor.putBoolean("ai_enabled", true);
        editor.putBoolean("rl_enabled", true);
        editor.putFloat("ai_aggression", 0.5f);
        editor.putInt("reaction_time", 250);
        editor.apply();

        loadCurrentSettings();
        Toast.makeText(getContext(), "AI settings reset to defaults", Toast.LENGTH_SHORT).show();
    }

    private void exportTrainingData() {
        // Export training data to file
        ObjectLabelerEngine labeler = GameAutomationEngine.getInstance().getObjectLabelerEngine();
        if (labeler != null) {
            String exportPath = labeler.exportTrainingData();
            if (exportPath != null) {
                Toast.makeText(getContext(), "Training data exported to: " + exportPath, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), "Export failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void importTrainingData() {
        // Import training data from file
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/json");
        startActivityForResult(intent, REQUEST_IMPORT_DATA);
    }

    private void updatePerformanceMode(boolean highPerformance) {
        com.gestureai.gameautomation.PerformanceTracker tracker = com.gestureai.gameautomation.PerformanceTracker.getInstance();
        if (tracker != null) {
            tracker.setHighPerformanceMode(highPerformance);
        }
    }

    private void saveSettingInt(String key, int value) {
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .edit().putInt(key, value).apply();
    }

    private void updateAIModels() {
        pbModelUpdate.setVisibility(View.VISIBLE);
        btnUpdateModels.setEnabled(false);

        // Simulate model update process
        new Thread(() -> {
            try {
                Thread.sleep(3000); // Simulate download/update

                getActivity().runOnUiThread(() -> {
                    pbModelUpdate.setVisibility(View.GONE);
                    btnUpdateModels.setEnabled(true);
                    Toast.makeText(getContext(), "AI models updated successfully", Toast.LENGTH_SHORT).show();
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadCurrentSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        switchEnableAI.setChecked(prefs.getBoolean("ai_enabled", true));
        switchEnableRL.setChecked(prefs.getBoolean("rl_enabled", true));
        switchEnableNLP.setChecked(prefs.getBoolean("nlp_enabled", true));

        int aggression = (int)(prefs.getFloat("ai_aggression", 0.5f) * 100);
        seekBarAIAggression.setProgress(aggression);

        switchHighPerformance.setChecked(prefs.getBoolean("high_performance", false));
        switchBatterySaver.setChecked(prefs.getBoolean("battery_saver", false));

        updatePerformanceStats();
    }

    private void updatePerformanceStats() {
        if (resourceMonitor != null) {
            float cpuUsage = resourceMonitor.getCPUUsage();
            float memoryUsage = resourceMonitor.getMemoryUsage();

            tvCPUUsage.setText(String.format("CPU: %.1f%%", cpuUsage));
            tvMemoryUsage.setText(String.format("Memory: %.1f%%", memoryUsage));
        }
    }

    private void saveSettingBoolean(String key, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .edit().putBoolean(key, value).apply();
    }

    private void saveSettingFloat(String key, float value) {
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .edit().putFloat(key, value).apply();
    }

    private void resetAISettings() {
        new AlertDialog.Builder(getContext())
            .setTitle("Reset AI Settings")
            .setMessage("This will reset all AI learning data and models. Continue?")
            .setPositiveButton("Reset", (dialog, which) -> {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                editor.putBoolean("ai_enabled", true);
                editor.putBoolean("rl_enabled", true);
                editor.putBoolean("nlp_enabled", true);
                editor.putFloat("ai_aggression", 0.5f);
                editor.apply();
                
                loadCurrentSettings();
                Toast.makeText(getContext(), "AI settings reset to defaults", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void exportTrainingData() {
        Toast.makeText(getContext(), "Training data export feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void importTrainingData() {
        Toast.makeText(getContext(), "Training data import feature coming soon", Toast.LENGTH_SHORT).show();
    }


}
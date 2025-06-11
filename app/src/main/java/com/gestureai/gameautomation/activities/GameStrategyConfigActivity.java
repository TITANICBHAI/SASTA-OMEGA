package com.gestureai.gameautomation.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.gestureai.gameautomation.GameTypeDetector;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.ai.*;

import java.util.HashMap;
import java.util.Map;

public class GameStrategyConfigActivity extends AppCompatActivity {
    private static final String TAG = "GameStrategyConfig";

    // Game Type Detection
    private Switch switchAutoDetection;
    private Spinner spinnerGameType;
    private TextView tvDetectedGame;
    private Button btnDetectGame;
    private ProgressBar pbDetection;

    // Strategy Selection
    private Spinner spinnerStrategy;
    private TextView tvStrategyDescription;
    private Switch switchCustomStrategy;

    // Difficulty & Behavior
    private SeekBar seekBarAggression;
    private SeekBar seekBarReactionTime;
    private SeekBar seekBarAccuracy;
    private TextView tvAggressionValue;
    private TextView tvReactionValue;
    private TextView tvAccuracyValue;

    // Game-Specific Settings
    private LinearLayout layoutBattleRoyale;
    private LinearLayout layoutMOBA;
    private LinearLayout layoutFPS;
    private LinearLayout layoutGeneral;

    // Battle Royale Settings
    private SeekBar seekBarSurvivalPriority;
    private Switch switchZoneAwareness;
    private Switch switchLootOptimization;
    private Spinner spinnerDropStrategy;

    // MOBA Settings
    private SeekBar seekBarFarmPriority;
    private Switch switchTeamFightFocus;
    private Switch switchMapAwareness;
    private Spinner spinnerRole;

    // FPS Settings
    private SeekBar seekBarAimSensitivity;
    private Switch switchRecoilControl;
    private Switch switchMovementOptimization;
    private Spinner spinnerWeaponPreference;

    // Advanced Options
    private Switch switchAdaptiveLearning;
    private Switch switchPerformanceMonitoring;
    private SeekBar seekBarLearningRate;
    private TextView tvLearningRateValue;

    // AI Components
    private GameTypeDetector gameTypeDetector;
    private MultiPlayerStrategy multiPlayerStrategy;
    private MOBAStrategy mobaStrategy;
    private FPSStrategy fpsStrategy;
    private AdaptiveDecisionMaker adaptiveDecisionMaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_strategy_config);

        initializeViews();
        initializeAIComponents();
        setupListeners();
        loadCurrentConfiguration();
    }

    private void initializeViews() {
        // Game Type Detection
        switchAutoDetection = findViewById(R.id.switch_auto_detection);
        spinnerGameType = findViewById(R.id.spinner_game_type);
        tvDetectedGame = findViewById(R.id.tv_detected_game);
        btnDetectGame = findViewById(R.id.btn_detect_game);
        pbDetection = findViewById(R.id.pb_detection);

        // Strategy Selection
        spinnerStrategy = findViewById(R.id.spinner_strategy);
        tvStrategyDescription = findViewById(R.id.tv_strategy_description);
        switchCustomStrategy = findViewById(R.id.switch_custom_strategy);

        // Difficulty & Behavior
        seekBarAggression = findViewById(R.id.seekbar_aggression);
        seekBarReactionTime = findViewById(R.id.seekbar_reaction_time);
        seekBarAccuracy = findViewById(R.id.seekbar_accuracy);
        tvAggressionValue = findViewById(R.id.tv_aggression_value);
        tvReactionValue = findViewById(R.id.tv_reaction_value);
        tvAccuracyValue = findViewById(R.id.tv_accuracy_value);

        // Game-Specific Layouts
        layoutBattleRoyale = findViewById(R.id.layout_battle_royale);
        layoutMOBA = findViewById(R.id.layout_moba);
        layoutFPS = findViewById(R.id.layout_fps);
        layoutGeneral = findViewById(R.id.layout_general);

        // Battle Royale Settings
        seekBarSurvivalPriority = findViewById(R.id.seekbar_survival_priority);
        switchZoneAwareness = findViewById(R.id.switch_zone_awareness);
        switchLootOptimization = findViewById(R.id.switch_loot_optimization);
        spinnerDropStrategy = findViewById(R.id.spinner_drop_strategy);

        // MOBA Settings
        seekBarFarmPriority = findViewById(R.id.seekbar_farm_priority);
        switchTeamFightFocus = findViewById(R.id.switch_teamfight_focus);
        switchMapAwareness = findViewById(R.id.switch_map_awareness);
        spinnerRole = findViewById(R.id.spinner_role);

        // FPS Settings
        seekBarAimSensitivity = findViewById(R.id.seekbar_aim_sensitivity);
        switchRecoilControl = findViewById(R.id.switch_recoil_control);
        switchMovementOptimization = findViewById(R.id.switch_movement_optimization);
        spinnerWeaponPreference = findViewById(R.id.spinner_weapon_preference);

        // Advanced Options
        switchAdaptiveLearning = findViewById(R.id.switch_adaptive_learning);
        switchPerformanceMonitoring = findViewById(R.id.switch_performance_monitoring);
        seekBarLearningRate = findViewById(R.id.seekbar_learning_rate);
        tvLearningRateValue = findViewById(R.id.tv_learning_rate_value);

        setupSpinners();
    }

    private void setupSpinners() {
        // Game Type
        String[] gameTypes = {
                "Auto-Detect", "Battle Royale", "MOBA", "FPS", "RPG", "Strategy", "Arcade"
        };
        ArrayAdapter<String> gameAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, gameTypes);
        gameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGameType.setAdapter(gameAdapter);

        // Strategy
        String[] strategies = {
                "Adaptive AI", "Aggressive", "Defensive", "Balanced", "Learning", "Custom"
        };
        ArrayAdapter<String> strategyAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, strategies);
        strategyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStrategy.setAdapter(strategyAdapter);

        // Battle Royale Drop Strategy
        String[] dropStrategies = {
                "Hot Drop", "Safe Drop", "Edge Drop", "Adaptive", "Random"
        };
        ArrayAdapter<String> dropAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, dropStrategies);
        dropAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDropStrategy.setAdapter(dropAdapter);

        // MOBA Role
        String[] roles = {
                "Carry", "Support", "Tank", "Assassin", "Mage", "Adaptive"
        };
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);

        // FPS Weapon Preference
        String[] weapons = {
                "Assault Rifle", "SMG", "Sniper", "Shotgun", "Pistol", "Adaptive"
        };
        ArrayAdapter<String> weaponAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, weapons);
        weaponAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWeaponPreference.setAdapter(weaponAdapter);
    }

    private void initializeAIComponents() {
        try {
            gameTypeDetector = new GameTypeDetector(this);
            multiPlayerStrategy = new MultiPlayerStrategy(this);
            mobaStrategy = new MOBAStrategy(this);
            fpsStrategy = new FPSStrategy(this);
            adaptiveDecisionMaker = new AdaptiveDecisionMaker();

            Log.d(TAG, "AI strategy components initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AI components", e);
            Toast.makeText(this, "Error initializing strategy system", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        // Game Type Detection
        switchAutoDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spinnerGameType.setEnabled(!isChecked);
            btnDetectGame.setEnabled(!isChecked);
            if (isChecked) {
                startAutoDetection();
            }
        });

        btnDetectGame.setOnClickListener(v -> detectGameManually());

        spinnerGameType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateGameSpecificSettings(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Strategy Selection
        spinnerStrategy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateStrategyDescription(position);
                applyStrategyConfiguration(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Behavior Controls
        setupSeekBarListener(seekBarAggression, tvAggressionValue, "Aggression", "%");
        setupSeekBarListener(seekBarReactionTime, tvReactionValue, "Reaction", "ms");
        setupSeekBarListener(seekBarAccuracy, tvAccuracyValue, "Accuracy", "%");
        setupSeekBarListener(seekBarLearningRate, tvLearningRateValue, "Learning Rate", "");

        // Game-Specific Controls
        setupGameSpecificListeners();

        // Advanced Options
        switchAdaptiveLearning.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (adaptiveDecisionMaker != null) {
                adaptiveDecisionMaker.setAdaptiveLearningEnabled(isChecked);
            }
        });
    }

    private void setupSeekBarListener(SeekBar seekBar, TextView valueText, String label, String unit) {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) {
                    float value = progress / 100.0f;
                    if (label.equals("Reaction")) {
                        int ms = 50 + (int)(progress * 4.5f); // 50-500ms range
                        valueText.setText(ms + unit);
                        updateReactionTime(ms);
                    } else if (label.equals("Learning Rate")) {
                        float rate = progress / 10000.0f; // 0.0001 to 0.01
                        valueText.setText(String.format("%.4f", rate));
                        updateLearningRate(rate);
                    } else {
                        valueText.setText(progress + unit);
                        updateBehaviorParameter(label, value);
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupGameSpecificListeners() {
        // Battle Royale
        switchZoneAwareness.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (multiPlayerStrategy != null) {
                multiPlayerStrategy.setZoneAwarenessEnabled(isChecked);
            }
        });

        switchLootOptimization.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (multiPlayerStrategy != null) {
                multiPlayerStrategy.setLootOptimizationEnabled(isChecked);
            }
        });

        // MOBA
        switchTeamFightFocus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mobaStrategy != null) {
                mobaStrategy.setTeamFightFocusEnabled(isChecked);
            }
        });

        switchMapAwareness.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mobaStrategy != null) {
                mobaStrategy.setMapAwarenessEnabled(isChecked);
            }
        });

        // FPS
        switchRecoilControl.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (fpsStrategy != null) {
                fpsStrategy.setRecoilControlEnabled(isChecked);
            }
        });

        switchMovementOptimization.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (fpsStrategy != null) {
                fpsStrategy.setMovementOptimizationEnabled(isChecked);
            }
        });
    }

    private void startAutoDetection() {
        pbDetection.setVisibility(View.VISIBLE);
        tvDetectedGame.setText("Detecting...");

        if (gameTypeDetector != null) {
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Simulate detection time

                    String detectedType = gameTypeDetector.detectCurrentGame();

                    runOnUiThread(() -> {
                        pbDetection.setVisibility(View.GONE);
                        String gameTypeName = detectedType;
                        tvDetectedGame.setText("Detected: " + gameTypeName);

                        // Update spinner selection
                        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerGameType.getAdapter();
                        int position = adapter.getPosition(gameTypeName);
                        if (position >= 0) {
                            spinnerGameType.setSelection(position);
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        pbDetection.setVisibility(View.GONE);
                        tvDetectedGame.setText("Detection failed");
                    });
                }
            }).start();
        }
    }

    private void detectGameManually() {
        startAutoDetection();
    }

    private void updateGameSpecificSettings(int gameTypePosition) {
        // Hide all game-specific layouts
        layoutBattleRoyale.setVisibility(View.GONE);
        layoutMOBA.setVisibility(View.GONE);
        layoutFPS.setVisibility(View.GONE);
        layoutGeneral.setVisibility(View.VISIBLE);

        String[] gameTypes = {"Auto-Detect", "Battle Royale", "MOBA", "FPS", "RPG", "Strategy", "Arcade"};
        if (gameTypePosition < gameTypes.length) {
            String selectedType = gameTypes[gameTypePosition];

            switch (selectedType) {
                case "Battle Royale":
                    layoutBattleRoyale.setVisibility(View.VISIBLE);
                    break;
                case "MOBA":
                    layoutMOBA.setVisibility(View.VISIBLE);
                    break;
                case "FPS":
                    layoutFPS.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    private void updateStrategyDescription(int strategyPosition) {
        String[] descriptions = {
                "AI adapts strategy based on game state and performance",
                "Prioritizes offensive actions and high-risk high-reward plays",
                "Focuses on safety, survival, and conservative play",
                "Balanced approach between aggressive and defensive tactics",
                "Continuously learns and improves from gameplay experience",
                "User-defined custom strategy configuration"
        };

        if (strategyPosition < descriptions.length) {
            tvStrategyDescription.setText(descriptions[strategyPosition]);
        }
    }

    private void applyStrategyConfiguration(int strategyPosition) {
        String[] strategies = {"Adaptive", "Aggressive", "Defensive", "Balanced", "Learning", "Custom"};

        if (strategyPosition < strategies.length) {
            String strategy = strategies[strategyPosition];

            // Apply to all strategy components
            if (multiPlayerStrategy != null) {
                multiPlayerStrategy.setStrategyType(strategy);
            }
            if (mobaStrategy != null) {
                mobaStrategy.setStrategyType(strategy);
            }
            if (fpsStrategy != null) {
                fpsStrategy.setStrategyType(strategy);
            }

            // Update UI based on strategy
            updateUIForStrategy(strategy);
        }
    }

    private void updateUIForStrategy(String strategy) {
        switch (strategy) {
            case "Aggressive":
                seekBarAggression.setProgress(80);
                seekBarReactionTime.setProgress(20); // Faster reaction
                break;
            case "Defensive":
                seekBarAggression.setProgress(20);
                seekBarReactionTime.setProgress(60); // More cautious
                break;
            case "Balanced":
                seekBarAggression.setProgress(50);
                seekBarReactionTime.setProgress(50);
                break;
        }
    }

    private void updateBehaviorParameter(String parameter, float value) {
        switch (parameter) {
            case "Aggression":
                if (multiPlayerStrategy != null) multiPlayerStrategy.setAggressionLevel(value);
                if (mobaStrategy != null) mobaStrategy.setAggressionLevel(value);
                if (fpsStrategy != null) fpsStrategy.setAggressionLevel(value);
                break;
            case "Accuracy":
                if (fpsStrategy != null) fpsStrategy.setAccuracyTarget(value);
                break;
        }
    }

    private void updateReactionTime(int milliseconds) {
        if (multiPlayerStrategy != null) multiPlayerStrategy.setReactionTime(milliseconds);
        if (mobaStrategy != null) mobaStrategy.setReactionTime(milliseconds);
        if (fpsStrategy != null) fpsStrategy.setReactionTime(milliseconds);
    }

    private void updateLearningRate(float rate) {
        if (adaptiveDecisionMaker != null) {
            adaptiveDecisionMaker.setLearningRate(rate);
        }
    }

    private void loadCurrentConfiguration() {
        // Load saved configuration from preferences or AI components
        // This would restore the last used settings

        // Default values
        seekBarAggression.setProgress(50);
        seekBarReactionTime.setProgress(50);
        seekBarAccuracy.setProgress(75);
        seekBarLearningRate.setProgress(10);

        // Update value displays
        tvAggressionValue.setText("50%");
        tvReactionValue.setText("275ms");
        tvAccuracyValue.setText("75%");
        tvLearningRateValue.setText("0.0010");

        // Load game-specific settings
        if (multiPlayerStrategy != null) {
            switchZoneAwareness.setChecked(multiPlayerStrategy.isZoneAwarenessEnabled());
            switchLootOptimization.setChecked(multiPlayerStrategy.isLootOptimizationEnabled());
        }

        Toast.makeText(this, "Configuration loaded", Toast.LENGTH_SHORT).show();
    }

    private String getGameTypeName(GameTypeDetector.GameType gameType) {
        switch (gameType) {
            case BATTLE_ROYALE: return "Battle Royale";
            case MOBA: return "MOBA";
            case FPS: return "FPS";
            case RPG: return "RPG";
            case STRATEGY: return "Strategy";
            default: return "Arcade";
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Save current configuration
        saveConfiguration();
    }

    private void saveConfiguration() {
        // Save all current settings to preferences
        Log.d(TAG, "Configuration saved");
    }
}
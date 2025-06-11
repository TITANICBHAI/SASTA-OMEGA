package com.gestureai.gameautomation.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.GameTypeDetector;
import java.util.HashMap;
import java.util.Map;

/**
 * Game-specific strategy configuration interface
 * Allows per-game automation settings, strategy parameter tuning,
 * custom action sequences, and game element detection zones
 */
public class GameSpecificStrategyConfigActivity extends AppCompatActivity {
    private static final String TAG = "GameStrategyConfig";
    
    private Spinner spinnerGameType;
    private SeekBar seekBarAggression, seekBarCaution, seekBarLearningRate;
    private EditText editCustomActions, editDetectionZones;
    private Button btnSaveConfig, btnTestStrategy, btnResetDefaults;
    private TextView tvAggressionValue, tvCautionValue, tvLearningValue;
    
    private GameStrategyAgent strategyAgent;
    private GameTypeDetector gameTypeDetector;
    private Map<String, GameConfig> gameConfigs;
    private String currentGameType = "subway_surfers";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_strategy_config);
        
        initializeViews();
        initializeGameConfigs();
        setupListeners();
        loadCurrentConfig();
        
        strategyAgent = new GameStrategyAgent(this);
        gameTypeDetector = new GameTypeDetector(this);
        
        Log.d(TAG, "Game-specific strategy configuration initialized");
    }
    
    private void initializeViews() {
        spinnerGameType = findViewById(R.id.spinner_game_type);
        seekBarAggression = findViewById(R.id.seekbar_aggression);
        seekBarCaution = findViewById(R.id.seekbar_caution);
        seekBarLearningRate = findViewById(R.id.seekbar_learning_rate);
        editCustomActions = findViewById(R.id.edit_custom_actions);
        editDetectionZones = findViewById(R.id.edit_detection_zones);
        btnSaveConfig = findViewById(R.id.btn_save_config);
        btnTestStrategy = findViewById(R.id.btn_test_strategy);
        btnResetDefaults = findViewById(R.id.btn_reset_defaults);
        tvAggressionValue = findViewById(R.id.tv_aggression_value);
        tvCautionValue = findViewById(R.id.tv_caution_value);
        tvLearningValue = findViewById(R.id.tv_learning_value);
    }
    
    private void initializeGameConfigs() {
        gameConfigs = new HashMap<>();
        
        // Subway Surfers Configuration
        GameConfig subwaySurfers = new GameConfig();
        subwaySurfers.gameType = "subway_surfers";
        subwaySurfers.aggression = 0.7f;
        subwaySurfers.caution = 0.6f;
        subwaySurfers.learningRate = 0.01f;
        subwaySurfers.customActions = "SWIPE_LEFT,SWIPE_RIGHT,SWIPE_UP,SWIPE_DOWN,JUMP,ROLL";
        subwaySurfers.detectionZones = "TRAIN_TOP:0.3,0.1,0.4,0.3|OBSTACLES:0.2,0.4,0.6,0.8|COINS:0.0,0.0,1.0,1.0";
        gameConfigs.put("subway_surfers", subwaySurfers);
        
        // PUBG Mobile Configuration
        GameConfig pubgMobile = new GameConfig();
        pubgMobile.gameType = "pubg_mobile";
        pubgMobile.aggression = 0.5f;
        pubgMobile.caution = 0.8f;
        pubgMobile.learningRate = 0.005f;
        pubgMobile.customActions = "AIM,SHOOT,MOVE,CROUCH,PRONE,RELOAD,GRENADE,HEAL";
        pubgMobile.detectionZones = "ENEMIES:0.0,0.0,1.0,0.7|LOOT:0.0,0.0,1.0,1.0|ZONE:0.8,0.0,1.0,0.2|MINIMAP:0.8,0.0,1.0,0.2";
        gameConfigs.put("pubg_mobile", pubgMobile);
        
        // Mobile Legends Configuration
        GameConfig mobileLegends = new GameConfig();
        mobileLegends.gameType = "mobile_legends";
        mobileLegends.aggression = 0.6f;
        mobileLegends.caution = 0.7f;
        mobileLegends.learningRate = 0.008f;
        mobileLegends.customActions = "SKILL_1,SKILL_2,SKILL_3,ULTIMATE,ATTACK,RECALL,WARD,JUNGLE";
        mobileLegends.detectionZones = "ENEMIES:0.0,0.0,1.0,0.8|MINIONS:0.2,0.4,0.8,0.8|JUNGLE:0.0,0.0,1.0,0.4|MINIMAP:0.8,0.0,1.0,0.25";
        gameConfigs.put("mobile_legends", mobileLegends);
        
        // Free Fire Configuration
        GameConfig freeFire = new GameConfig();
        freeFire.gameType = "free_fire";
        freeFire.aggression = 0.8f;
        freeFire.caution = 0.4f;
        freeFire.learningRate = 0.012f;
        freeFire.customActions = "SHOOT,AIM,JUMP,SLIDE,GLOO_WALL,HEAL,GRENADE,VEHICLE";
        freeFire.detectionZones = "ENEMIES:0.0,0.0,1.0,0.7|LOOT:0.0,0.0,1.0,1.0|SAFE_ZONE:0.0,0.0,1.0,1.0|VEHICLES:0.0,0.0,1.0,0.6";
        gameConfigs.put("free_fire", freeFire);
    }
    
    private void setupListeners() {
        // Game type selection
        String[] gameTypes = {"Subway Surfers", "PUBG Mobile", "Mobile Legends", "Free Fire"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, gameTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGameType.setAdapter(adapter);
        
        spinnerGameType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                String[] gameKeys = {"subway_surfers", "pubg_mobile", "mobile_legends", "free_fire"};
                currentGameType = gameKeys[position];
                loadGameConfig(currentGameType);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // Seekbar listeners with value display
        seekBarAggression.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 100f;
                tvAggressionValue.setText(String.format("%.2f", value));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        seekBarCaution.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 100f;
                tvCautionValue.setText(String.format("%.2f", value));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        seekBarLearningRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 1000f; // 0.000 to 0.100
                tvLearningValue.setText(String.format("%.3f", value));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Button listeners
        btnSaveConfig.setOnClickListener(v -> saveCurrentConfig());
        btnTestStrategy.setOnClickListener(v -> testStrategy());
        btnResetDefaults.setOnClickListener(v -> resetToDefaults());
    }
    
    private void loadCurrentConfig() {
        loadGameConfig(currentGameType);
    }
    
    private void loadGameConfig(String gameType) {
        GameConfig config = gameConfigs.get(gameType);
        if (config != null) {
            seekBarAggression.setProgress((int)(config.aggression * 100));
            seekBarCaution.setProgress((int)(config.caution * 100));
            seekBarLearningRate.setProgress((int)(config.learningRate * 1000));
            editCustomActions.setText(config.customActions);
            editDetectionZones.setText(config.detectionZones);
            
            tvAggressionValue.setText(String.format("%.2f", config.aggression));
            tvCautionValue.setText(String.format("%.2f", config.caution));
            tvLearningValue.setText(String.format("%.3f", config.learningRate));
            
            Log.d(TAG, "Loaded configuration for: " + gameType);
        }
    }
    
    private void saveCurrentConfig() {
        try {
            GameConfig config = gameConfigs.get(currentGameType);
            if (config == null) {
                config = new GameConfig();
                config.gameType = currentGameType;
                gameConfigs.put(currentGameType, config);
            }
            
            config.aggression = seekBarAggression.getProgress() / 100f;
            config.caution = seekBarCaution.getProgress() / 100f;
            config.learningRate = seekBarLearningRate.getProgress() / 1000f;
            config.customActions = editCustomActions.getText().toString();
            config.detectionZones = editDetectionZones.getText().toString();
            
            // Apply configuration to strategy agent
            if (strategyAgent != null) {
                strategyAgent.setGameConfiguration(config.gameType, config.aggression, 
                    config.caution, config.learningRate);
                strategyAgent.setCustomActions(config.customActions.split(","));
                strategyAgent.setDetectionZones(parseDetectionZones(config.detectionZones));
            }
            
            Toast.makeText(this, "Configuration saved for " + currentGameType, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Configuration saved for: " + currentGameType);
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving configuration", e);
            Toast.makeText(this, "Error saving configuration: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void testStrategy() {
        try {
            saveCurrentConfig();
            
            if (strategyAgent != null) {
                // Create a test game state
                GameStrategyAgent.UniversalGameState testState = new GameStrategyAgent.UniversalGameState();
                testState.gameType = getGameTypeFloat(currentGameType);
                testState.threatLevel = 0.5f;
                testState.opportunityLevel = 0.7f;
                testState.playerX = 540; // Center of 1080p screen
                testState.playerY = 960;
                testState.screenWidth = 1080;
                testState.screenHeight = 1920;
                
                // Test strategy decision
                com.gestureai.gameautomation.GameAction testAction = strategyAgent.makeDecision(testState);
                
                String message = String.format("Test Strategy Result:\nAction: %s\nCoordinates: (%d, %d)\nConfidence: %.2f",
                    testAction.getActionType(), testAction.getX(), testAction.getY(), testAction.getConfidence());
                
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                Log.d(TAG, "Strategy test completed: " + message);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error testing strategy", e);
            Toast.makeText(this, "Error testing strategy: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void resetToDefaults() {
        initializeGameConfigs();
        loadGameConfig(currentGameType);
        Toast.makeText(this, "Reset to default configuration", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Configuration reset to defaults for: " + currentGameType);
    }
    
    private Map<String, float[]> parseDetectionZones(String zonesString) {
        Map<String, float[]> zones = new HashMap<>();
        
        try {
            String[] zoneEntries = zonesString.split("\\|");
            for (String entry : zoneEntries) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    String zoneName = parts[0];
                    String[] coords = parts[1].split(",");
                    if (coords.length == 4) {
                        float[] zone = new float[4];
                        for (int i = 0; i < 4; i++) {
                            zone[i] = Float.parseFloat(coords[i]);
                        }
                        zones.put(zoneName, zone);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing detection zones", e);
        }
        
        return zones;
    }
    
    private float getGameTypeFloat(String gameType) {
        switch (gameType) {
            case "subway_surfers": return 0.1f;
            case "pubg_mobile": return 0.3f;
            case "mobile_legends": return 0.5f;
            case "free_fire": return 0.7f;
            default: return 0.0f;
        }
    }
    
    // Configuration data class
    private static class GameConfig {
        String gameType;
        float aggression;
        float caution;
        float learningRate;
        String customActions;
        String detectionZones;
    }
}
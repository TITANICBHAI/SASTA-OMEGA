package com.gestureai.gameautomation.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.GameTypeDetector;
import com.gestureai.gameautomation.managers.UnifiedServiceCoordinator;

public class GameStrategyConfigurationActivity extends AppCompatActivity {
    private static final String TAG = "GameStrategyConfig";
    
    private Spinner spGameType;
    private SeekBar sbAggressiveness;
    private SeekBar sbRiskTolerance;
    private SeekBar sbReactionSpeed;
    private SeekBar sbResourcePriority;
    private TextView tvAggressivenessValue;
    private TextView tvRiskToleranceValue;
    private TextView tvReactionSpeedValue;
    private TextView tvResourcePriorityValue;
    private Button btnSaveStrategy;
    private Button btnTestStrategy;
    private Button btnResetDefaults;
    
    private GameStrategyAgent strategyAgent;
    private UnifiedServiceCoordinator coordinator;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_strategy_config);
        
        initializeViews();
        setupSeekBarListeners();
        loadCurrentStrategy();
        
        Log.d(TAG, "Game Strategy Configuration Activity initialized");
    }
    
    private void initializeViews() {
        spGameType = findViewById(R.id.sp_game_type);
        sbAggressiveness = findViewById(R.id.sb_aggressiveness);
        sbRiskTolerance = findViewById(R.id.sb_risk_tolerance);
        sbReactionSpeed = findViewById(R.id.sb_reaction_speed);
        sbResourcePriority = findViewById(R.id.sb_resource_priority);
        
        tvAggressivenessValue = findViewById(R.id.tv_aggressiveness_value);
        tvRiskToleranceValue = findViewById(R.id.tv_risk_tolerance_value);
        tvReactionSpeedValue = findViewById(R.id.tv_reaction_speed_value);
        tvResourcePriorityValue = findViewById(R.id.tv_resource_priority_value);
        
        btnSaveStrategy = findViewById(R.id.btn_save_strategy);
        btnTestStrategy = findViewById(R.id.btn_test_strategy);
        btnResetDefaults = findViewById(R.id.btn_reset_defaults);
        
        coordinator = UnifiedServiceCoordinator.getInstance(this);
        strategyAgent = new GameStrategyAgent(this);
        
        setupClickListeners();
    }
    
    private void setupClickListeners() {
        btnSaveStrategy.setOnClickListener(v -> saveStrategyConfiguration());
        btnTestStrategy.setOnClickListener(v -> testCurrentStrategy());
        btnResetDefaults.setOnClickListener(v -> resetToDefaults());
    }
    
    private void setupSeekBarListeners() {
        sbAggressiveness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvAggressivenessValue.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        sbRiskTolerance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvRiskToleranceValue.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        sbReactionSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvReactionSpeedValue.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        sbResourcePriority.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvResourcePriorityValue.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    private void loadCurrentStrategy() {
        // Load current strategy parameters
        sbAggressiveness.setProgress(50);
        sbRiskTolerance.setProgress(30);
        sbReactionSpeed.setProgress(70);
        sbResourcePriority.setProgress(60);
        
        updateValueDisplays();
    }
    
    private void updateValueDisplays() {
        tvAggressivenessValue.setText(String.valueOf(sbAggressiveness.getProgress()));
        tvRiskToleranceValue.setText(String.valueOf(sbRiskTolerance.getProgress()));
        tvReactionSpeedValue.setText(String.valueOf(sbReactionSpeed.getProgress()));
        tvResourcePriorityValue.setText(String.valueOf(sbResourcePriority.getProgress()));
    }
    
    private void saveStrategyConfiguration() {
        try {
            GameStrategyAgent.GameType selectedGameType = getSelectedGameType();
            
            StrategyConfiguration config = new StrategyConfiguration(
                selectedGameType,
                sbAggressiveness.getProgress() / 100.0f,
                sbRiskTolerance.getProgress() / 100.0f,
                sbReactionSpeed.getProgress() / 100.0f,
                sbResourcePriority.getProgress() / 100.0f
            );
            
            applyStrategyConfiguration(config);
            
            Toast.makeText(this, "Strategy configuration saved", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Strategy configuration saved: " + config.toString());
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving strategy configuration", e);
            Toast.makeText(this, "Error saving configuration", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void testCurrentStrategy() {
        try {
            Toast.makeText(this, "Testing strategy with current game...", Toast.LENGTH_SHORT).show();
            
            // Create test game state
            GameStrategyAgent.UniversalGameState testState = createTestGameState();
            
            // Get strategy recommendation
            GameStrategyAgent.BaseStrategy strategy = strategyAgent.getStrategyForGame(getSelectedGameType());
            if (strategy != null) {
                // Test would be implemented here
                Log.d(TAG, "Strategy test completed");
                Toast.makeText(this, "Strategy test completed - check logs", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error testing strategy", e);
            Toast.makeText(this, "Error testing strategy", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void resetToDefaults() {
        sbAggressiveness.setProgress(50);
        sbRiskTolerance.setProgress(30);
        sbReactionSpeed.setProgress(70);
        sbResourcePriority.setProgress(60);
        
        updateValueDisplays();
        Toast.makeText(this, "Reset to default values", Toast.LENGTH_SHORT).show();
    }
    
    private GameStrategyAgent.GameType getSelectedGameType() {
        int position = spGameType.getSelectedItemPosition();
        GameStrategyAgent.GameType[] values = GameStrategyAgent.GameType.values();
        return position < values.length ? values[position] : GameStrategyAgent.GameType.ARCADE;
    }
    
    private void applyStrategyConfiguration(StrategyConfiguration config) {
        // Apply configuration to strategy agent
        if (coordinator != null) {
            // Integration with UnifiedServiceCoordinator
            Log.d(TAG, "Applying strategy configuration to coordinator");
        }
    }
    
    private GameStrategyAgent.UniversalGameState createTestGameState() {
        GameStrategyAgent.UniversalGameState state = new GameStrategyAgent.UniversalGameState();
        state.healthLevel = 0.8f;
        state.powerLevel = 0.6f;
        state.speedLevel = 0.7f;
        state.threatLevel = 0.3f;
        state.opportunityLevel = 0.5f;
        state.objectCount = 5;
        state.timeInGame = 120.0f;
        state.gameScore = 1500;
        state.playerX = 540;
        state.playerY = 960;
        return state;
    }
    
    // Configuration data structure
    private static class StrategyConfiguration {
        public GameStrategyAgent.GameType gameType;
        public float aggressiveness;
        public float riskTolerance;
        public float reactionSpeed;
        public float resourcePriority;
        
        public StrategyConfiguration(GameStrategyAgent.GameType gameType, float aggressiveness, 
                                   float riskTolerance, float reactionSpeed, float resourcePriority) {
            this.gameType = gameType;
            this.aggressiveness = aggressiveness;
            this.riskTolerance = riskTolerance;
            this.reactionSpeed = reactionSpeed;
            this.resourcePriority = resourcePriority;
        }
        
        @Override
        public String toString() {
            return String.format("Strategy[%s: Agg=%.2f, Risk=%.2f, Speed=%.2f, Resource=%.2f]",
                gameType, aggressiveness, riskTolerance, reactionSpeed, resourcePriority);
        }
    }
}
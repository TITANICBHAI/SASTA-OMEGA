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
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_autoplay, container, false);
        
        initializeViews(view);
        setupListeners();
        setupSpinner();
        updateUI();
        
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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
    }

    private void initializeViews(View view) {
        tvAutomationStatus = view.findViewById(R.id.tv_automation_status);
        btnStartAutomation = view.findViewById(R.id.btn_start_automation);
        btnStopAutomation = view.findViewById(R.id.btn_stop_automation);
        spinnerStrategy = view.findViewById(R.id.spinner_strategy);

        // ADD THESE MISSING ELEMENTS:
        spinnerGameType = view.findViewById(R.id.spinner_game_type);
        cbAutoGameDetection = view.findViewById(R.id.cb_auto_game_detection);
        pbModelLoading = view.findViewById(R.id.pb_model_loading);
        tvGameTypeDetected = view.findViewById(R.id.tv_game_type_detected);

        seekBarReactionSpeed = view.findViewById(R.id.seekbar_reaction_speed);
        tvReactionSpeedValue = view.findViewById(R.id.tv_reaction_speed_value);
        tvActionsCount = view.findViewById(R.id.tv_actions_count);
        tvSuccessRate = view.findViewById(R.id.tv_success_rate);
        // Add after line 54:
        if (pbModelLoading == null) {
            // Element not in layout yet - add to fragment_autoplay.xml
        }
        if (tvGameTypeDetected == null) {
            // Element not in layout yet - add to fragment_autoplay.xml
        }

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
        // MISSING UI ELEMENTS (line 84):

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
    
    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }
}
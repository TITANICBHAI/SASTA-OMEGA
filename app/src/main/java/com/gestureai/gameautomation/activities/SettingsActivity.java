package com.gestureai.gameautomation.activities;

import android.os.Bundle;
import android.widget.Switch;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.gestureai.gameautomation.R;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    
    private Switch swAutoMode;
    private Switch swVibration;
    private Switch swSoundFeedback;
    private SeekBar sbSensitivity;
    private SeekBar sbReactionSpeed;
    private TextView tvSensitivityValue;
    private TextView tvReactionSpeedValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        initializeViews();
        setupListeners();
        loadSettings();
    }

    private void initializeViews() {
        swAutoMode = findViewById(R.id.sw_auto_mode);
        swVibration = findViewById(R.id.sw_vibration);
        swSoundFeedback = findViewById(R.id.sw_sound_feedback);
        sbSensitivity = findViewById(R.id.sb_sensitivity);
        sbReactionSpeed = findViewById(R.id.sb_reaction_speed);
        tvSensitivityValue = findViewById(R.id.tv_sensitivity_value);
        tvReactionSpeedValue = findViewById(R.id.tv_reaction_speed_value);
    }

    private void setupListeners() {
        sbSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSensitivityValue.setText(progress + "%");
                saveSettings();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbReactionSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvReactionSpeedValue.setText(progress + "ms");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void loadSettings() {
        // Load saved settings from SharedPreferences
        // Default values for now
        swAutoMode.setChecked(true);
        swVibration.setChecked(true);
        swSoundFeedback.setChecked(false);
        sbSensitivity.setProgress(75);
        sbReactionSpeed.setProgress(100);
        tvSensitivityValue.setText("75%");
        tvReactionSpeedValue.setText("100ms");
    }

    private void loadSettings() {
        android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        
        swAutoMode.setChecked(prefs.getBoolean("auto_mode", true));
        swVibration.setChecked(prefs.getBoolean("vibration", true));
        swSoundFeedback.setChecked(prefs.getBoolean("sound_feedback", true));
        
        int sensitivity = prefs.getInt("sensitivity", 50);
        int reactionSpeed = prefs.getInt("reaction_speed", 70);
        
        sbSensitivity.setProgress(sensitivity);
        sbReactionSpeed.setProgress(reactionSpeed);
        tvSensitivityValue.setText(sensitivity + "%");
        tvReactionSpeedValue.setText(reactionSpeed + "%");
    }

    private void saveSettings() {
        android.content.SharedPreferences.Editor editor = 
            android.preference.PreferenceManager.getDefaultSharedPreferences(this).edit();
        
        editor.putBoolean("auto_mode", swAutoMode.isChecked());
        editor.putBoolean("vibration", swVibration.isChecked());
        editor.putBoolean("sound_feedback", swSoundFeedback.isChecked());
        editor.putInt("sensitivity", sbSensitivity.getProgress());
        editor.putInt("reaction_speed", sbReactionSpeed.getProgress());
        
        editor.apply();
    }
}
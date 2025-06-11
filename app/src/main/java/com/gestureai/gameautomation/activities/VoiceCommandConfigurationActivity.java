package com.gestureai.gameautomation.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.services.VoiceCommandService;
import com.gestureai.gameautomation.adapters.VoiceCommandAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * Voice Command Configuration Activity - User interface for voice command setup
 */
public class VoiceCommandConfigurationActivity extends AppCompatActivity {
    private static final String TAG = "VoiceCommandConfig";
    
    private Switch switchVoiceEnabled;
    private SeekBar seekBarSensitivity;
    private TextView tvSensitivityValue;
    private Spinner spinnerLanguage;
    private Button btnTestVoice;
    private Button btnAddCommand;
    private RecyclerView recyclerViewCommands;
    
    private VoiceCommandAdapter commandAdapter;
    private List<VoiceCommandService.VoiceCommand> voiceCommands;
    private VoiceCommandService voiceService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_command_configuration);
        
        initializeViews();
        setupListeners();
        loadCurrentSettings();
        setupRecyclerView();
    }
    
    private void initializeViews() {
        switchVoiceEnabled = findViewById(R.id.switch_voice_enabled);
        seekBarSensitivity = findViewById(R.id.seekbar_sensitivity);
        tvSensitivityValue = findViewById(R.id.tv_sensitivity_value);
        spinnerLanguage = findViewById(R.id.spinner_language);
        btnTestVoice = findViewById(R.id.btn_test_voice);
        btnAddCommand = findViewById(R.id.btn_add_command);
        recyclerViewCommands = findViewById(R.id.recyclerview_commands);
        
        // Setup language spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.voice_languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);
    }
    
    private void setupListeners() {
        switchVoiceEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableVoiceCommands(isChecked);
        });
        
        seekBarSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float sensitivity = progress / 100.0f;
                tvSensitivityValue.setText(String.format("%.2f", sensitivity));
                if (voiceService != null) {
                    voiceService.setSensitivity(sensitivity);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        btnTestVoice.setOnClickListener(v -> testVoiceRecognition());
        btnAddCommand.setOnClickListener(v -> addNewVoiceCommand());
    }
    
    private void loadCurrentSettings() {
        // Load voice command settings from preferences
        android.content.SharedPreferences prefs = getSharedPreferences("voice_config", MODE_PRIVATE);
        
        boolean voiceEnabled = prefs.getBoolean("voice_enabled", false);
        switchVoiceEnabled.setChecked(voiceEnabled);
        
        float sensitivity = prefs.getFloat("voice_sensitivity", 0.7f);
        seekBarSensitivity.setProgress((int)(sensitivity * 100));
        tvSensitivityValue.setText(String.format("%.2f", sensitivity));
        
        String language = prefs.getString("voice_language", "en-US");
        setLanguageSpinner(language);
        
        loadVoiceCommands();
    }
    
    private void setupRecyclerView() {
        voiceCommands = new ArrayList<>();
        commandAdapter = new VoiceCommandAdapter(voiceCommands, this::onCommandEdit, this::onCommandDelete);
        recyclerViewCommands.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCommands.setAdapter(commandAdapter);
    }
    
    private void loadVoiceCommands() {
        // Load predefined voice commands
        voiceCommands.clear();
        voiceCommands.add(new VoiceCommandService.VoiceCommand("start automation", "START_AUTOMATION"));
        voiceCommands.add(new VoiceCommandService.VoiceCommand("stop automation", "STOP_AUTOMATION"));
        voiceCommands.add(new VoiceCommandService.VoiceCommand("take screenshot", "TAKE_SCREENSHOT"));
        voiceCommands.add(new VoiceCommandService.VoiceCommand("switch to game", "SWITCH_TO_GAME"));
        voiceCommands.add(new VoiceCommandService.VoiceCommand("emergency stop", "EMERGENCY_STOP"));
        
        if (commandAdapter != null) {
            commandAdapter.notifyDataSetChanged();
        }
    }
    
    private void enableVoiceCommands(boolean enabled) {
        // Save preference
        android.content.SharedPreferences prefs = getSharedPreferences("voice_config", MODE_PRIVATE);
        prefs.edit().putBoolean("voice_enabled", enabled).apply();
        
        // Enable/disable voice service
        if (enabled) {
            startVoiceService();
        } else {
            stopVoiceService();
        }
        
        // Update UI state
        seekBarSensitivity.setEnabled(enabled);
        spinnerLanguage.setEnabled(enabled);
        btnTestVoice.setEnabled(enabled);
        btnAddCommand.setEnabled(enabled);
    }
    
    private void startVoiceService() {
        try {
            android.content.Intent intent = new android.content.Intent(this, VoiceCommandService.class);
            startService(intent);
            Toast.makeText(this, "Voice commands enabled", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to start voice service", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopVoiceService() {
        try {
            android.content.Intent intent = new android.content.Intent(this, VoiceCommandService.class);
            stopService(intent);
            Toast.makeText(this, "Voice commands disabled", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to stop voice service", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void testVoiceRecognition() {
        Toast.makeText(this, "Say a command to test voice recognition", Toast.LENGTH_LONG).show();
        // Implementation for voice testing would go here
    }
    
    private void addNewVoiceCommand() {
        // Show dialog to add new voice command
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Add Voice Command");
        
        // Create input fields
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        
        final android.widget.EditText phraseInput = new android.widget.EditText(this);
        phraseInput.setHint("Voice phrase (e.g., 'start game')");
        layout.addView(phraseInput);
        
        final android.widget.EditText actionInput = new android.widget.EditText(this);
        actionInput.setHint("Action (e.g., 'START_GAME')");
        layout.addView(actionInput);
        
        builder.setView(layout);
        
        builder.setPositiveButton("Add", (dialog, which) -> {
            String phrase = phraseInput.getText().toString().trim();
            String action = actionInput.getText().toString().trim();
            
            if (!phrase.isEmpty() && !action.isEmpty()) {
                VoiceCommandService.VoiceCommand newCommand = new VoiceCommandService.VoiceCommand(phrase, action);
                voiceCommands.add(newCommand);
                commandAdapter.notifyItemInserted(voiceCommands.size() - 1);
                Toast.makeText(this, "Voice command added", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void onCommandEdit(int position) {
        VoiceCommandService.VoiceCommand command = voiceCommands.get(position);
        // Show edit dialog similar to add dialog
        Toast.makeText(this, "Edit: " + command.phrase, Toast.LENGTH_SHORT).show();
    }
    
    private void onCommandDelete(int position) {
        voiceCommands.remove(position);
        commandAdapter.notifyItemRemoved(position);
        Toast.makeText(this, "Voice command deleted", Toast.LENGTH_SHORT).show();
    }
    
    private void setLanguageSpinner(String language) {
        ArrayAdapter adapter = (ArrayAdapter) spinnerLanguage.getAdapter();
        int position = adapter.getPosition(language);
        if (position >= 0) {
            spinnerLanguage.setSelection(position);
        }
    }
}
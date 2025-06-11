package com.gestureai.gameautomation.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.services.VoiceCommandService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Voice command configuration interface
 * Voice command mapping, custom phrase recording, sensitivity settings,
 * and command conflict resolution
 */
public class VoiceCommandConfigurationActivity extends AppCompatActivity {
    private static final String TAG = "VoiceCommandConfig";
    private static final int REQUEST_AUDIO_PERMISSION = 1001;
    
    private Button btnStartRecording, btnStopRecording, btnSaveCommand, btnTestRecognition;
    private EditText editCommandPhrase, editActionMapping;
    private SeekBar seekBarSensitivity, seekBarVolumeThreshold;
    private TextView tvSensitivityValue, tvVolumeValue, tvRecordingStatus;
    private RecyclerView recyclerViewCommands;
    private ProgressBar progressBarRecording;
    private Switch switchEnableVoice;
    
    private SpeechRecognizer speechRecognizer;
    private MediaRecorder mediaRecorder;
    private VoiceCommandAdapter commandAdapter;
    private List<VoiceCommand> savedCommands;
    private boolean isRecording = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_command_configuration);
        
        initializeViews();
        setupRecyclerView();
        setupListeners();
        initializeDefaultCommands();
        requestAudioPermission();
        
        Log.d(TAG, "Voice command configuration initialized");
    }
    
    private void initializeViews() {
        btnStartRecording = findViewById(R.id.btn_start_recording);
        btnStopRecording = findViewById(R.id.btn_stop_recording);
        btnSaveCommand = findViewById(R.id.btn_save_command);
        btnTestRecognition = findViewById(R.id.btn_test_recognition);
        editCommandPhrase = findViewById(R.id.edit_command_phrase);
        editActionMapping = findViewById(R.id.edit_action_mapping);
        seekBarSensitivity = findViewById(R.id.seekbar_sensitivity);
        seekBarVolumeThreshold = findViewById(R.id.seekbar_volume_threshold);
        tvSensitivityValue = findViewById(R.id.tv_sensitivity_value);
        tvVolumeValue = findViewById(R.id.tv_volume_value);
        tvRecordingStatus = findViewById(R.id.tv_recording_status);
        recyclerViewCommands = findViewById(R.id.recycler_view_commands);
        progressBarRecording = findViewById(R.id.progress_bar_recording);
        switchEnableVoice = findViewById(R.id.switch_enable_voice);
        
        // Initial state
        btnStopRecording.setEnabled(false);
        btnSaveCommand.setEnabled(false);
    }
    
    private void setupRecyclerView() {
        savedCommands = new ArrayList<>();
        commandAdapter = new VoiceCommandAdapter(savedCommands);
        recyclerViewCommands.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCommands.setAdapter(commandAdapter);
    }
    
    private void setupListeners() {
        btnStartRecording.setOnClickListener(v -> startVoiceRecording());
        btnStopRecording.setOnClickListener(v -> stopVoiceRecording());
        btnSaveCommand.setOnClickListener(v -> saveVoiceCommand());
        btnTestRecognition.setOnClickListener(v -> testVoiceRecognition());
        
        switchEnableVoice.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableVoiceCommands(isChecked);
        });
        
        // Sensitivity seekbar
        seekBarSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float sensitivity = progress / 100f;
                tvSensitivityValue.setText(String.format("%.2f", sensitivity));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Volume threshold seekbar
        seekBarVolumeThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume = progress / 100f;
                tvVolumeValue.setText(String.format("%.2f", volume));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    private void initializeDefaultCommands() {
        // Add default voice commands for game automation
        savedCommands.add(new VoiceCommand("start automation", "START_AUTOMATION", 0.8f));
        savedCommands.add(new VoiceCommand("stop automation", "STOP_AUTOMATION", 0.8f));
        savedCommands.add(new VoiceCommand("pause game", "PAUSE_GAME", 0.7f));
        savedCommands.add(new VoiceCommand("resume game", "RESUME_GAME", 0.7f));
        savedCommands.add(new VoiceCommand("swipe left", "SWIPE_LEFT", 0.6f));
        savedCommands.add(new VoiceCommand("swipe right", "SWIPE_RIGHT", 0.6f));
        savedCommands.add(new VoiceCommand("jump", "JUMP", 0.5f));
        savedCommands.add(new VoiceCommand("emergency stop", "EMERGENCY_STOP", 0.9f));
        
        commandAdapter.notifyDataSetChanged();
        Log.d(TAG, "Initialized default voice commands");
    }
    
    private void requestAudioPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
        } else {
            initializeSpeechRecognizer();
        }
    }
    
    private void initializeSpeechRecognizer() {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new VoiceRecognitionListener());
            
            btnTestRecognition.setEnabled(true);
            Log.d(TAG, "Speech recognizer initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing speech recognizer", e);
            Toast.makeText(this, "Error initializing voice recognition", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startVoiceRecording() {
        if (isRecording) return;
        
        try {
            android.content.Intent intent = new android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            
            speechRecognizer.startListening(intent);
            isRecording = true;
            
            btnStartRecording.setEnabled(false);
            btnStopRecording.setEnabled(true);
            progressBarRecording.setVisibility(android.view.View.VISIBLE);
            tvRecordingStatus.setText("Listening... Say your command phrase");
            tvRecordingStatus.setTextColor(getColor(android.R.color.holo_red_light));
            
            Log.d(TAG, "Started voice recording");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting voice recording", e);
            Toast.makeText(this, "Error starting voice recording", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopVoiceRecording() {
        if (!isRecording) return;
        
        try {
            speechRecognizer.stopListening();
            isRecording = false;
            
            btnStartRecording.setEnabled(true);
            btnStopRecording.setEnabled(false);
            progressBarRecording.setVisibility(android.view.View.GONE);
            
            Log.d(TAG, "Stopped voice recording");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping voice recording", e);
        }
    }
    
    private void saveVoiceCommand() {
        String phrase = editCommandPhrase.getText().toString().trim();
        String action = editActionMapping.getText().toString().trim();
        
        if (phrase.isEmpty() || action.isEmpty()) {
            Toast.makeText(this, "Please enter both phrase and action", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check for duplicate phrases
        for (VoiceCommand existing : savedCommands) {
            if (existing.phrase.equalsIgnoreCase(phrase)) {
                Toast.makeText(this, "Command phrase already exists", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        try {
            float sensitivity = seekBarSensitivity.getProgress() / 100f;
            VoiceCommand newCommand = new VoiceCommand(phrase, action, sensitivity);
            
            savedCommands.add(newCommand);
            commandAdapter.notifyDataSetChanged();
            
            // Register with voice command service
            VoiceCommandService.addVoiceCommand(phrase, action, sensitivity);
            
            editCommandPhrase.setText("");
            editActionMapping.setText("");
            btnSaveCommand.setEnabled(false);
            
            tvRecordingStatus.setText("Voice command saved: " + phrase);
            Toast.makeText(this, "Command saved: " + phrase, Toast.LENGTH_SHORT).show();
            
            Log.d(TAG, "Saved voice command: " + phrase + " -> " + action);
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving voice command", e);
            Toast.makeText(this, "Error saving command: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void testVoiceRecognition() {
        if (speechRecognizer == null) {
            Toast.makeText(this, "Voice recognition not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            android.content.Intent intent = new android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
            
            speechRecognizer.startListening(intent);
            
            tvRecordingStatus.setText("Testing voice recognition... Say something");
            progressBarRecording.setVisibility(android.view.View.VISIBLE);
            
            Log.d(TAG, "Started voice recognition test");
            
        } catch (Exception e) {
            Log.e(TAG, "Error testing voice recognition", e);
            Toast.makeText(this, "Error testing voice recognition", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void enableVoiceCommands(boolean enabled) {
        try {
            if (enabled) {
                // Start voice command service
                android.content.Intent serviceIntent = new android.content.Intent(this, VoiceCommandService.class);
                startService(serviceIntent);
                
                // Configure service with current settings
                float sensitivity = seekBarSensitivity.getProgress() / 100f;
                float volumeThreshold = seekBarVolumeThreshold.getProgress() / 100f;
                
                VoiceCommandService.configureSettings(sensitivity, volumeThreshold);
                
                Toast.makeText(this, "Voice commands enabled", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Voice commands enabled with sensitivity: " + sensitivity);
                
            } else {
                // Stop voice command service
                android.content.Intent serviceIntent = new android.content.Intent(this, VoiceCommandService.class);
                stopService(serviceIntent);
                
                Toast.makeText(this, "Voice commands disabled", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Voice commands disabled");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error toggling voice commands", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeSpeechRecognizer();
            } else {
                Toast.makeText(this, "Audio permission required for voice commands", Toast.LENGTH_LONG).show();
                btnTestRecognition.setEnabled(false);
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing media recorder", e);
            }
        }
        
        Log.d(TAG, "Voice command configuration destroyed");
    }
    
    // Voice recognition listener
    private class VoiceRecognitionListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
            tvRecordingStatus.setText("Ready for speech - speak now");
        }
        
        @Override
        public void onBeginningOfSpeech() {
            tvRecordingStatus.setText("Listening...");
        }
        
        @Override
        public void onRmsChanged(float rmsdB) {
            // Update volume indicator if needed
        }
        
        @Override
        public void onBufferReceived(byte[] buffer) {}
        
        @Override
        public void onEndOfSpeech() {
            tvRecordingStatus.setText("Processing speech...");
            progressBarRecording.setVisibility(android.view.View.GONE);
        }
        
        @Override
        public void onError(int error) {
            String errorMessage = "Speech recognition error: " + error;
            tvRecordingStatus.setText(errorMessage);
            tvRecordingStatus.setTextColor(getColor(android.R.color.holo_red_light));
            progressBarRecording.setVisibility(android.view.View.GONE);
            Log.e(TAG, errorMessage);
        }
        
        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            
            if (matches != null && !matches.isEmpty()) {
                String recognizedText = matches.get(0);
                editCommandPhrase.setText(recognizedText);
                btnSaveCommand.setEnabled(true);
                
                tvRecordingStatus.setText("Recognized: " + recognizedText);
                tvRecordingStatus.setTextColor(getColor(android.R.color.holo_green_light));
                
                Log.d(TAG, "Voice recognition result: " + recognizedText);
            }
        }
        
        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                tvRecordingStatus.setText("Partial: " + matches.get(0));
            }
        }
        
        @Override
        public void onEvent(int eventType, Bundle params) {}
    }
    
    // Voice command data class
    private static class VoiceCommand {
        String phrase;
        String action;
        float sensitivity;
        long createdAt;
        
        public VoiceCommand(String phrase, String action, float sensitivity) {
            this.phrase = phrase;
            this.action = action;
            this.sensitivity = sensitivity;
            this.createdAt = System.currentTimeMillis();
        }
    }
    
    // RecyclerView adapter for voice commands
    private class VoiceCommandAdapter extends RecyclerView.Adapter<VoiceCommandAdapter.CommandViewHolder> {
        private List<VoiceCommand> commands;
        
        public VoiceCommandAdapter(List<VoiceCommand> commands) {
            this.commands = commands;
        }
        
        @Override
        public CommandViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            return new CommandViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(CommandViewHolder holder, int position) {
            VoiceCommand command = commands.get(position);
            holder.textPhrase.setText(command.phrase);
            holder.textAction.setText(String.format("%s (sensitivity: %.2f)", command.action, command.sensitivity));
        }
        
        @Override
        public int getItemCount() {
            return commands.size();
        }
        
        class CommandViewHolder extends RecyclerView.ViewHolder {
            TextView textPhrase, textAction;
            
            public CommandViewHolder(android.view.View itemView) {
                super(itemView);
                textPhrase = itemView.findViewById(android.R.id.text1);
                textAction = itemView.findViewById(android.R.id.text2);
                
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        VoiceCommand command = commands.get(position);
                        editCommandPhrase.setText(command.phrase);
                        editActionMapping.setText(command.action);
                        seekBarSensitivity.setProgress((int)(command.sensitivity * 100));
                        Toast.makeText(VoiceCommandConfigurationActivity.this, 
                            "Loaded command: " + command.phrase, Toast.LENGTH_SHORT).show();
                    }
                });
                
                itemView.setOnLongClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        commands.remove(position);
                        notifyItemRemoved(position);
                        Toast.makeText(VoiceCommandConfigurationActivity.this, 
                            "Command deleted", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    return false;
                });
            }
        }
    }
}
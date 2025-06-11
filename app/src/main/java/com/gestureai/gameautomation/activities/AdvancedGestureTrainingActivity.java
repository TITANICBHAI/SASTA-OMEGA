package com.gestureai.gameautomation.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.managers.MediaPipeManager;
import com.gestureai.gameautomation.models.GestureResult;
import com.gestureai.gameautomation.models.HandLandmarks;
import java.util.ArrayList;
import java.util.List;

/**
 * Advanced gesture training UI with comprehensive gesture customization
 * Custom gesture recording/playback, sequence chaining, sensitivity adjustment,
 * and gesture conflict resolution
 */
public class AdvancedGestureTrainingActivity extends AppCompatActivity {
    private static final String TAG = "AdvancedGestureTraining";
    
    private Button btnStartRecording, btnStopRecording, btnPlayback, btnSaveGesture, btnClearGestures;
    private EditText editGestureName;
    private SeekBar seekBarSensitivity, seekBarChainDelay;
    private TextView tvSensitivityValue, tvChainDelayValue, tvRecordingStatus;
    private RecyclerView recyclerViewGestures;
    private ProgressBar progressBarTraining;
    
    private MediaPipeManager mediaPipeManager;
    private List<HandLandmarks> recordedGesture;
    private List<CustomGesture> savedGestures;
    private GestureAdapter gestureAdapter;
    private boolean isRecording = false;
    private Handler recordingHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_gesture_training);
        
        initializeViews();
        setupRecyclerView();
        setupListeners();
        initializeGestureRecording();
        
        savedGestures = new ArrayList<>();
        recordedGesture = new ArrayList<>();
        recordingHandler = new Handler(Looper.getMainLooper());
        
        Log.d(TAG, "Advanced gesture training interface initialized");
    }
    
    private void initializeViews() {
        btnStartRecording = findViewById(R.id.btn_start_recording);
        btnStopRecording = findViewById(R.id.btn_stop_recording);
        btnPlayback = findViewById(R.id.btn_playback);
        btnSaveGesture = findViewById(R.id.btn_save_gesture);
        btnClearGestures = findViewById(R.id.btn_clear_gestures);
        editGestureName = findViewById(R.id.edit_gesture_name);
        seekBarSensitivity = findViewById(R.id.seekbar_sensitivity);
        seekBarChainDelay = findViewById(R.id.seekbar_chain_delay);
        tvSensitivityValue = findViewById(R.id.tv_sensitivity_value);
        tvChainDelayValue = findViewById(R.id.tv_chain_delay_value);
        tvRecordingStatus = findViewById(R.id.tv_recording_status);
        recyclerViewGestures = findViewById(R.id.recycler_view_gestures);
        progressBarTraining = findViewById(R.id.progress_bar_training);
        
        // Initial state
        btnStopRecording.setEnabled(false);
        btnPlayback.setEnabled(false);
        btnSaveGesture.setEnabled(false);
    }
    
    private void setupRecyclerView() {
        gestureAdapter = new GestureAdapter(savedGestures);
        recyclerViewGestures.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewGestures.setAdapter(gestureAdapter);
    }
    
    private void setupListeners() {
        btnStartRecording.setOnClickListener(v -> startGestureRecording());
        btnStopRecording.setOnClickListener(v -> stopGestureRecording());
        btnPlayback.setOnClickListener(v -> playbackGesture());
        btnSaveGesture.setOnClickListener(v -> saveCurrentGesture());
        btnClearGestures.setOnClickListener(v -> clearAllGestures());
        
        // Sensitivity seekbar
        seekBarSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float sensitivity = progress / 100f;
                tvSensitivityValue.setText(String.format("%.2f", sensitivity));
                if (mediaPipeManager != null) {
                    mediaPipeManager.setGestureSensitivity(sensitivity);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Chain delay seekbar
        seekBarChainDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int delayMs = progress * 10; // 0-1000ms
                tvChainDelayValue.setText(String.format("%d ms", delayMs));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    private void initializeGestureRecording() {
        try {
            mediaPipeManager = new MediaPipeManager(this);
            mediaPipeManager.setGestureCallback(this::onGestureDetected);
            Log.d(TAG, "MediaPipe manager initialized for gesture recording");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing gesture recording", e);
            Toast.makeText(this, "Error initializing camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startGestureRecording() {
        if (isRecording) return;
        
        recordedGesture.clear();
        isRecording = true;
        
        btnStartRecording.setEnabled(false);
        btnStopRecording.setEnabled(true);
        btnPlayback.setEnabled(false);
        btnSaveGesture.setEnabled(false);
        
        tvRecordingStatus.setText("Recording... Perform your gesture");
        tvRecordingStatus.setTextColor(getColor(android.R.color.holo_red_light));
        
        if (mediaPipeManager != null) {
            mediaPipeManager.startGestureRecording();
        }
        
        Log.d(TAG, "Started gesture recording");
    }
    
    private void stopGestureRecording() {
        if (!isRecording) return;
        
        isRecording = false;
        
        btnStartRecording.setEnabled(true);
        btnStopRecording.setEnabled(false);
        btnPlayback.setEnabled(!recordedGesture.isEmpty());
        btnSaveGesture.setEnabled(!recordedGesture.isEmpty());
        
        tvRecordingStatus.setText(String.format("Recording complete. %d landmarks captured", recordedGesture.size()));
        tvRecordingStatus.setTextColor(getColor(android.R.color.holo_green_light));
        
        if (mediaPipeManager != null) {
            mediaPipeManager.stopGestureRecording();
        }
        
        Log.d(TAG, "Stopped gesture recording - " + recordedGesture.size() + " landmarks captured");
    }
    
    private void playbackGesture() {
        if (recordedGesture.isEmpty()) return;
        
        progressBarTraining.setVisibility(android.view.View.VISIBLE);
        tvRecordingStatus.setText("Playing back recorded gesture...");
        
        // Simulate playback by iterating through recorded landmarks
        new Thread(() -> {
            for (int i = 0; i < recordedGesture.size(); i++) {
                final int progress = (i * 100) / recordedGesture.size();
                final HandLandmarks landmarks = recordedGesture.get(i);
                
                runOnUiThread(() -> {
                    progressBarTraining.setProgress(progress);
                    // Could visualize landmarks here if needed
                });
                
                try {
                    Thread.sleep(33); // ~30 FPS playback
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            runOnUiThread(() -> {
                progressBarTraining.setVisibility(android.view.View.GONE);
                tvRecordingStatus.setText("Playback complete");
            });
        }).start();
        
        Log.d(TAG, "Playing back gesture with " + recordedGesture.size() + " landmarks");
    }
    
    private void saveCurrentGesture() {
        String gestureName = editGestureName.getText().toString().trim();
        
        if (gestureName.isEmpty()) {
            Toast.makeText(this, "Please enter a gesture name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (recordedGesture.isEmpty()) {
            Toast.makeText(this, "No gesture recorded", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check for duplicate names
        for (CustomGesture existing : savedGestures) {
            if (existing.name.equals(gestureName)) {
                Toast.makeText(this, "Gesture name already exists", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        try {
            CustomGesture newGesture = new CustomGesture();
            newGesture.name = gestureName;
            newGesture.landmarks = new ArrayList<>(recordedGesture);
            newGesture.sensitivity = seekBarSensitivity.getProgress() / 100f;
            newGesture.chainDelay = seekBarChainDelay.getProgress() * 10;
            newGesture.createdAt = System.currentTimeMillis();
            
            savedGestures.add(newGesture);
            gestureAdapter.notifyDataSetChanged();
            
            // Train the gesture recognition system
            if (mediaPipeManager != null) {
                mediaPipeManager.addCustomGesture(gestureName, recordedGesture, newGesture.sensitivity);
            }
            
            editGestureName.setText("");
            recordedGesture.clear();
            btnPlayback.setEnabled(false);
            btnSaveGesture.setEnabled(false);
            
            tvRecordingStatus.setText("Gesture saved and trained: " + gestureName);
            Toast.makeText(this, "Gesture saved: " + gestureName, Toast.LENGTH_SHORT).show();
            
            Log.d(TAG, "Saved custom gesture: " + gestureName + " with " + newGesture.landmarks.size() + " landmarks");
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving gesture", e);
            Toast.makeText(this, "Error saving gesture: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void clearAllGestures() {
        savedGestures.clear();
        gestureAdapter.notifyDataSetChanged();
        recordedGesture.clear();
        
        btnPlayback.setEnabled(false);
        btnSaveGesture.setEnabled(false);
        
        if (mediaPipeManager != null) {
            mediaPipeManager.clearCustomGestures();
        }
        
        tvRecordingStatus.setText("All gestures cleared");
        Toast.makeText(this, "All gestures cleared", Toast.LENGTH_SHORT).show();
        
        Log.d(TAG, "Cleared all custom gestures");
    }
    
    private void onGestureDetected(GestureResult result) {
        if (isRecording && result.getHandLandmarks() != null) {
            recordedGesture.add(result.getHandLandmarks());
            
            runOnUiThread(() -> {
                tvRecordingStatus.setText(String.format("Recording... %d landmarks captured", recordedGesture.size()));
            });
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (mediaPipeManager != null) {
            mediaPipeManager.cleanup();
        }
        
        if (recordingHandler != null) {
            recordingHandler.removeCallbacksAndMessages(null);
        }
        
        Log.d(TAG, "Advanced gesture training destroyed");
    }
    
    // Custom gesture data class
    private static class CustomGesture {
        String name;
        List<HandLandmarks> landmarks;
        float sensitivity;
        int chainDelay;
        long createdAt;
    }
    
    // RecyclerView adapter for saved gestures
    private class GestureAdapter extends RecyclerView.Adapter<GestureAdapter.GestureViewHolder> {
        private List<CustomGesture> gestures;
        
        public GestureAdapter(List<CustomGesture> gestures) {
            this.gestures = gestures;
        }
        
        @Override
        public GestureViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            return new GestureViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(GestureViewHolder holder, int position) {
            CustomGesture gesture = gestures.get(position);
            holder.textName.setText(gesture.name);
            holder.textDetails.setText(String.format("%d landmarks, %.2f sensitivity, %dms delay", 
                gesture.landmarks.size(), gesture.sensitivity, gesture.chainDelay));
        }
        
        @Override
        public int getItemCount() {
            return gestures.size();
        }
        
        class GestureViewHolder extends RecyclerView.ViewHolder {
            TextView textName, textDetails;
            
            public GestureViewHolder(android.view.View itemView) {
                super(itemView);
                textName = itemView.findViewById(android.R.id.text1);
                textDetails = itemView.findViewById(android.R.id.text2);
                
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        CustomGesture gesture = gestures.get(position);
                        editGestureName.setText(gesture.name);
                        seekBarSensitivity.setProgress((int)(gesture.sensitivity * 100));
                        seekBarChainDelay.setProgress(gesture.chainDelay / 10);
                        Toast.makeText(AdvancedGestureTrainingActivity.this, 
                            "Loaded gesture: " + gesture.name, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}
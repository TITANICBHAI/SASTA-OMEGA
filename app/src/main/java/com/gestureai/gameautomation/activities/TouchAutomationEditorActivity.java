package com.gestureai.gameautomation.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import android.view.View;
import android.view.MotionEvent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.content.Context;
import android.util.AttributeSet;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.managers.TouchExecutionManager;
import com.gestureai.gameautomation.GameAction;
import com.gestureai.gameautomation.GestureAction;

import java.util.List;
import java.util.ArrayList;

/**
 * Touch Automation Editor Interface
 * Provides user controls for touch sequence recording, playbook, gesture customization, and timing controls
 */
public class TouchAutomationEditorActivity extends Activity {
    private static final String TAG = "TouchAutomationEditor";
    
    // UI Components
    private TouchSequenceCanvas touchCanvas;
    private Button btnStartRecording;
    private Button btnStopRecording;
    private Button btnPlaySequence;
    private Button btnSaveSequence;
    private Button btnLoadSequence;
    private Button btnClearSequence;
    private SeekBar seekActionTiming;
    private SeekBar seekGesturePrecision;
    private TextView tvTimingValue;
    private TextView tvPrecisionValue;
    private TextView tvSequenceInfo;
    private RecyclerView rvActionSequence;
    private Spinner spinnerGestureType;
    private Switch switchLoopSequence;
    private Switch switchAdaptiveTiming;
    
    // Backend Components
    private TouchExecutionManager touchManager;
    private List<GameAction> recordedSequence;
    private boolean isRecording = false;
    private long recordingStartTime = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_gesture_sequence_builder);
            
            initializeComponents();
            setupTouchCanvas();
            setupGestureControls();
            
            Log.d(TAG, "Touch Automation Editor initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Touch Automation Editor", e);
            finish();
        }
    }
    
    private void initializeComponents() {
        // Initialize UI components
        touchCanvas = findViewById(R.id.touch_canvas);
        btnStartRecording = findViewById(R.id.btn_start_recording);
        btnStopRecording = findViewById(R.id.btn_stop_recording);
        btnPlaySequence = findViewById(R.id.btn_play_sequence);
        btnSaveSequence = findViewById(R.id.btn_save_sequence);
        btnLoadSequence = findViewById(R.id.btn_load_sequence);
        btnClearSequence = findViewById(R.id.btn_clear_sequence);
        seekActionTiming = findViewById(R.id.seek_action_timing);
        seekGesturePrecision = findViewById(R.id.seek_gesture_precision);
        tvTimingValue = findViewById(R.id.tv_timing_value);
        tvPrecisionValue = findViewById(R.id.tv_precision_value);
        tvSequenceInfo = findViewById(R.id.tv_sequence_info);
        rvActionSequence = findViewById(R.id.rv_action_sequence);
        spinnerGestureType = findViewById(R.id.spinner_gesture_type);
        switchLoopSequence = findViewById(R.id.switch_loop_sequence);
        switchAdaptiveTiming = findViewById(R.id.switch_adaptive_timing);
        
        // Initialize backend components
        touchManager = new TouchExecutionManager(this);
        recordedSequence = new ArrayList<>();
        
        // Setup listeners
        setupListeners();
        setupSequenceDisplay();
    }
    
    private void setupListeners() {
        btnStartRecording.setOnClickListener(this::startRecording);
        btnStopRecording.setOnClickListener(this::stopRecording);
        btnPlaySequence.setOnClickListener(this::playSequence);
        btnSaveSequence.setOnClickListener(this::saveSequence);
        btnLoadSequence.setOnClickListener(this::loadSequence);
        btnClearSequence.setOnClickListener(this::clearSequence);
        
        // Action timing control (50ms to 2000ms)
        seekActionTiming.setMax(1950);
        seekActionTiming.setProgress(200); // Default 250ms
        seekActionTiming.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int timing = 50 + progress;
                tvTimingValue.setText(timing + "ms");
                if (fromUser) updateActionTiming(timing);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Gesture precision control (1 to 100)
        seekGesturePrecision.setMax(99);
        seekGesturePrecision.setProgress(49); // Default 50%
        seekGesturePrecision.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int precision = progress + 1;
                tvPrecisionValue.setText(precision + "%");
                if (fromUser) updateGesturePrecision(precision);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    private void setupTouchCanvas() {
        if (touchCanvas == null) {
            // Create fallback canvas if not found in layout
            touchCanvas = new TouchSequenceCanvas(this);
            // Add to layout programmatically if needed
        }
        
        touchCanvas.setOnTouchListener((v, event) -> {
            if (isRecording) {
                recordTouchEvent(event);
            }
            return true;
        });
    }
    
    private void setupGestureControls() {
        // Setup gesture type spinner
        String[] gestureTypes = {"TAP", "SWIPE_UP", "SWIPE_DOWN", "SWIPE_LEFT", 
                                "SWIPE_RIGHT", "LONG_PRESS", "DOUBLE_TAP", "PINCH", "ZOOM"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, gestureTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGestureType.setAdapter(adapter);
    }
    
    private void setupSequenceDisplay() {
        rvActionSequence.setLayoutManager(new LinearLayoutManager(this));
        // Setup adapter for displaying recorded sequence
        updateSequenceInfo();
    }
    
    private void startRecording(View view) {
        try {
            isRecording = true;
            recordingStartTime = System.currentTimeMillis();
            recordedSequence.clear();
            
            btnStartRecording.setEnabled(false);
            btnStopRecording.setEnabled(true);
            
            tvSequenceInfo.setText("Recording... Touch the canvas to record gestures");
            touchCanvas.clearCanvas();
            
            Log.d(TAG, "Touch sequence recording started");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording", e);
        }
    }
    
    private void stopRecording(View view) {
        try {
            isRecording = false;
            
            btnStartRecording.setEnabled(true);
            btnStopRecording.setEnabled(false);
            
            updateSequenceInfo();
            
            Log.d(TAG, "Touch sequence recording stopped. Recorded " + recordedSequence.size() + " actions");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
        }
    }
    
    private void recordTouchEvent(MotionEvent event) {
        try {
            long currentTime = System.currentTimeMillis();
            long relativeTime = currentTime - recordingStartTime;
            
            int action = event.getAction();
            float x = event.getX();
            float y = event.getY();
            
            String actionType = "TAP";
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    actionType = "TAP";
                    break;
                case MotionEvent.ACTION_MOVE:
                    actionType = "SWIPE";
                    break;
                case MotionEvent.ACTION_UP:
                    actionType = "RELEASE";
                    break;
            }
            
            GameAction gameAction = new GameAction(actionType, (int)x, (int)y, 1.0f, "recorded");
            gameAction.setTimestamp(relativeTime);
            
            recordedSequence.add(gameAction);
            touchCanvas.addTouchPoint(x, y, actionType);
            
            updateSequenceInfo();
            
        } catch (Exception e) {
            Log.e(TAG, "Error recording touch event", e);
        }
    }
    
    private void playSequence(View view) {
        try {
            if (recordedSequence.isEmpty()) {
                Toast.makeText(this, "No sequence recorded", Toast.LENGTH_SHORT).show();
                return;
            }
            
            tvSequenceInfo.setText("Playing sequence...");
            
            // Execute recorded sequence
            new Thread(() -> {
                try {
                    boolean loopEnabled = switchLoopSequence.isChecked();
                    
                    do {
                        for (GameAction action : recordedSequence) {
                            touchManager.executeAction(action);
                            
                            // Wait for action timing
                            int timing = 50 + seekActionTiming.getProgress();
                            Thread.sleep(timing);
                        }
                    } while (loopEnabled);
                    
                    runOnUiThread(() -> {
                        tvSequenceInfo.setText("Sequence playback complete");
                    });
                    
                } catch (InterruptedException e) {
                    runOnUiThread(() -> {
                        tvSequenceInfo.setText("Playback interrupted");
                    });
                }
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "Error playing sequence", e);
            tvSequenceInfo.setText("Error playing sequence");
        }
    }
    
    private void saveSequence(View view) {
        try {
            String sequenceName = "sequence_" + System.currentTimeMillis();
            boolean saved = touchManager.saveSequence(sequenceName, recordedSequence);
            
            if (saved) {
                Toast.makeText(this, "Sequence saved: " + sequenceName, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save sequence", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving sequence", e);
            Toast.makeText(this, "Error saving sequence", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadSequence(View view) {
        try {
            List<String> savedSequences = touchManager.getSavedSequences();
            
            if (savedSequences.isEmpty()) {
                Toast.makeText(this, "No saved sequences found", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Show selection dialog
            String[] sequenceArray = savedSequences.toArray(new String[0]);
            new android.app.AlertDialog.Builder(this)
                .setTitle("Load Sequence")
                .setItems(sequenceArray, (dialog, which) -> {
                    String selectedSequence = sequenceArray[which];
                    List<GameAction> loadedSequence = touchManager.loadSequence(selectedSequence);
                    
                    if (loadedSequence != null) {
                        recordedSequence = loadedSequence;
                        updateSequenceInfo();
                        touchCanvas.displaySequence(recordedSequence);
                        Toast.makeText(this, "Sequence loaded: " + selectedSequence, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to load sequence", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
                
        } catch (Exception e) {
            Log.e(TAG, "Error loading sequence", e);
            Toast.makeText(this, "Error loading sequence", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void clearSequence(View view) {
        recordedSequence.clear();
        touchCanvas.clearCanvas();
        updateSequenceInfo();
        Toast.makeText(this, "Sequence cleared", Toast.LENGTH_SHORT).show();
    }
    
    private void updateActionTiming(int timing) {
        Log.d(TAG, "Action timing updated: " + timing + "ms");
        touchManager.setActionTiming(timing);
    }
    
    private void updateGesturePrecision(int precision) {
        Log.d(TAG, "Gesture precision updated: " + precision + "%");
        touchManager.setGesturePrecision(precision / 100.0f);
    }
    
    private void updateSequenceInfo() {
        int actionCount = recordedSequence.size();
        long totalDuration = recordedSequence.isEmpty() ? 0 : 
            recordedSequence.get(recordedSequence.size() - 1).getTimestamp();
        
        tvSequenceInfo.setText(String.format("Sequence: %d actions, Duration: %dms", 
            actionCount, totalDuration));
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Touch Automation Editor destroyed");
    }
    
    // Custom Canvas for visualizing touch sequences
    public static class TouchSequenceCanvas extends View {
        private Paint paint;
        private List<TouchPoint> touchPoints;
        
        public TouchSequenceCanvas(Context context) {
            super(context);
            init();
        }
        
        public TouchSequenceCanvas(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }
        
        private void init() {
            paint = new Paint();
            paint.setAntiAlias(true);
            touchPoints = new ArrayList<>();
        }
        
        public void addTouchPoint(float x, float y, String actionType) {
            touchPoints.add(new TouchPoint(x, y, actionType));
            invalidate();
        }
        
        public void clearCanvas() {
            touchPoints.clear();
            invalidate();
        }
        
        public void displaySequence(List<GameAction> sequence) {
            touchPoints.clear();
            for (GameAction action : sequence) {
                touchPoints.add(new TouchPoint(action.getX(), action.getY(), action.getActionType()));
            }
            invalidate();
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            for (int i = 0; i < touchPoints.size(); i++) {
                TouchPoint point = touchPoints.get(i);
                
                // Set color based on action type
                switch (point.actionType) {
                    case "TAP":
                        paint.setColor(Color.BLUE);
                        break;
                    case "SWIPE":
                        paint.setColor(Color.GREEN);
                        break;
                    case "LONG_PRESS":
                        paint.setColor(Color.RED);
                        break;
                    default:
                        paint.setColor(Color.GRAY);
                        break;
                }
                
                canvas.drawCircle(point.x, point.y, 10, paint);
                
                // Draw line to next point for swipe visualization
                if (i < touchPoints.size() - 1 && point.actionType.equals("SWIPE")) {
                    TouchPoint nextPoint = touchPoints.get(i + 1);
                    paint.setStrokeWidth(3);
                    canvas.drawLine(point.x, point.y, nextPoint.x, nextPoint.y, paint);
                }
            }
        }
        
        private static class TouchPoint {
            float x, y;
            String actionType;
            
            TouchPoint(float x, float y, String actionType) {
                this.x = x;
                this.y = y;
                this.actionType = actionType;
            }
        }
    }
}
package com.gestureai.gameautomation.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.managers.MLModelManager;
import com.gestureai.gameautomation.GestureRecognitionService;
import com.gestureai.gameautomation.GestureRecognitionService.GestureTrainingData;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class GestureTrainingActivity extends AppCompatActivity {
    private static final String TAG = "GestureTraining";
    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    // Camera Components
    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private ImageAnalysis imageAnalysis;

    // Training Controls
    private Button btnStartRecording;
    private Button btnStopRecording;
    private Button btnSaveGesture;
    private Button btnTestGesture;
    private EditText etGestureName;
    private EditText etGestureCommand;
    private Spinner spinnerGestureType;
    private Switch switchRealTimeDetection;

    // Training Progress
    private ProgressBar pbTrainingProgress;
    private TextView tvSamplesCollected;
    private TextView tvTrainingStatus;
    private TextView tvAccuracy;
    private ListView lvTrainedGestures;

    // Gesture Visualization
    private ImageView ivGestureVisualization;
    private TextView tvGestureConfidence;
    private TextView tvDetectedGesture;

    // Advanced Settings
    private SeekBar seekBarSensitivity;
    private SeekBar seekBarMinConfidence;
    private TextView tvSensitivityValue;
    private TextView tvMinConfidenceValue;
    private Switch switchHandTracking;
    private Switch switchPoseDetection;

    // AI Components
    private GestureRecognitionService gestureService;
    private MLModelManager modelManager;

    // Training Data
    private List<GestureTrainingData> trainingData;
    private TrainedGestureAdapter gestureAdapter;
    private boolean isRecording = false;
    private boolean isRealTimeEnabled = false;
    private String currentGestureName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture_training);

        initializeViews();
        initializeAIComponents();
        setupListeners();
        checkCameraPermission();
    }

    private void initializeViews() {
        // Camera
        previewView = findViewById(R.id.preview_view);

        // Training Controls
        btnStartRecording = findViewById(R.id.btn_start_recording);
        btnStopRecording = findViewById(R.id.btn_stop_recording);
        btnSaveGesture = findViewById(R.id.btn_save_gesture);
        btnTestGesture = findViewById(R.id.btn_test_gesture);
        etGestureName = findViewById(R.id.et_gesture_name);
        etGestureCommand = findViewById(R.id.et_gesture_command);
        spinnerGestureType = findViewById(R.id.spinner_gesture_type);
        switchRealTimeDetection = findViewById(R.id.switch_realtime_detection);

        // Training Progress
        pbTrainingProgress = findViewById(R.id.pb_training_progress);
        tvSamplesCollected = findViewById(R.id.tv_samples_collected);
        tvTrainingStatus = findViewById(R.id.tv_training_status);
        tvAccuracy = findViewById(R.id.tv_accuracy);
        lvTrainedGestures = findViewById(R.id.lv_trained_gestures);

        // Gesture Visualization
        ivGestureVisualization = findViewById(R.id.iv_gesture_visualization);
        tvGestureConfidence = findViewById(R.id.tv_gesture_confidence);
        tvDetectedGesture = findViewById(R.id.tv_detected_gesture);

        // Advanced Settings
        seekBarSensitivity = findViewById(R.id.seekbar_sensitivity);
        seekBarMinConfidence = findViewById(R.id.seekbar_min_confidence);
        tvSensitivityValue = findViewById(R.id.tv_sensitivity_value);
        tvMinConfidenceValue = findViewById(R.id.tv_min_confidence_value);
        switchHandTracking = findViewById(R.id.switch_hand_tracking);
        switchPoseDetection = findViewById(R.id.switch_pose_detection);

        setupSpinners();
        setupTrainedGesturesList();
        updateUI();
    }

    private void setupSpinners() {
        String[] gestureTypes = {
                "Hand Gesture", "Head Movement", "Body Pose", "Facial Expression",
                "Eye Movement", "Custom Combination"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, gestureTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGestureType.setAdapter(adapter);
    }

    private void setupTrainedGesturesList() {
        trainingData = new ArrayList<>();
        gestureAdapter = new TrainedGestureAdapter(this, trainingData);
        lvTrainedGestures.setAdapter(gestureAdapter);

        // Load existing trained gestures
        loadTrainedGestures();
    }

    private void initializeAIComponents() {
        try {
            gestureService = new GestureRecognitionService();
            modelManager = new MLModelManager(this);

            Log.d(TAG, "AI components initialized for gesture training");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AI components", e);
            Toast.makeText(this, "Error initializing gesture recognition system", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        // Training Controls
        btnStartRecording.setOnClickListener(v -> startGestureRecording());
        btnStopRecording.setOnClickListener(v -> stopGestureRecording());
        btnSaveGesture.setOnClickListener(v -> saveTrainedGesture());
        btnTestGesture.setOnClickListener(v -> testGestureRecognition());

        switchRealTimeDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isRealTimeEnabled = isChecked;
            toggleRealTimeDetection(isChecked);
        });

        // Advanced Settings
        seekBarSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float sensitivity = progress / 100.0f;
                    tvSensitivityValue.setText(String.format("%.2f", sensitivity));
                    if (gestureService != null) {
                        gestureService.setSensitivity(sensitivity);
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarMinConfidence.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float confidence = progress / 100.0f;
                    tvMinConfidenceValue.setText(String.format("%.2f", confidence));
                    if (gestureService != null) {
                        gestureService.setMinConfidence(confidence);
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        switchHandTracking.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (gestureService != null) {
                gestureService.setHandTrackingEnabled(isChecked);
            }
        });

        switchPoseDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (gestureService != null) {
                gestureService.setPoseDetectionEnabled(isChecked);
            }
        });

        // Gesture List Actions
        lvTrainedGestures.setOnItemClickListener((parent, view, position, id) -> {
            editTrainedGesture(position);
        });

        lvTrainedGestures.setOnItemLongClickListener((parent, view, position, id) -> {
            deleteTrainedGesture(position);
            return true;
        });
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission required for gesture training",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Error starting camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        // Unbind all use cases before rebinding
        cameraProvider.unbindAll();

        // Preview use case
        preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image analysis for gesture recognition
        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new android.util.Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::analyzeImage);

        // Select back camera
        CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

        try {
            // Bind use cases to camera
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera use cases", e);
        }
    }

    private void analyzeImage(@NonNull ImageProxy image) {
        if (!isRealTimeEnabled && !isRecording) {
            image.close();
            return;
        }

        // Convert ImageProxy to Bitmap for processing
        Bitmap bitmap = imageProxyToBitmap(image);

        if (gestureService != null && bitmap != null) {
            // Perform gesture recognition
            GestureRecognitionService.GestureResult result = gestureService.recognizeGesture(bitmap);

            runOnUiThread(() -> {
                if (result != null) {
                    updateGestureVisualization(result, bitmap);

                    if (isRecording) {
                        recordGestureSample(result, bitmap);
                    }
                }
            });
        }

        image.close();
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        // Convert ImageProxy to Bitmap
        // This is a simplified implementation
        try {
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            if (planes.length > 0) {
                java.nio.ByteBuffer buffer = planes[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);

                // Create a simple bitmap (in real implementation, you'd handle YUV conversion)
                return Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error converting image", e);
        }
        return null;
    }

    private void startGestureRecording() {
        String gestureName = etGestureName.getText().toString().trim();
        if (gestureName.isEmpty()) {
            Toast.makeText(this, "Please enter a gesture name", Toast.LENGTH_SHORT).show();
            return;
        }

        currentGestureName = gestureName;
        isRecording = true;

        btnStartRecording.setEnabled(false);
        btnStopRecording.setEnabled(true);
        btnSaveGesture.setEnabled(false);

        tvTrainingStatus.setText("Recording gesture: " + gestureName);
        tvSamplesCollected.setText("Samples: 0");

        // Start collecting training samples
        if (gestureService != null) {
            gestureService.startTrainingSession(gestureName);
        }

        Toast.makeText(this, "Recording started. Perform the gesture multiple times",
                Toast.LENGTH_LONG).show();
    }

    private void stopGestureRecording() {
        isRecording = false;

        btnStartRecording.setEnabled(true);
        btnStopRecording.setEnabled(false);
        btnSaveGesture.setEnabled(true);

        tvTrainingStatus.setText("Recording stopped");

        if (gestureService != null) {
            int sampleCount = gestureService.getTrainingSampleCount(currentGestureName); // Add gesture name parameter
            tvSamplesCollected.setText("Samples: " + sampleCount);

            if (sampleCount < 10) {
                Toast.makeText(this, "Warning: Less than 10 samples collected. Consider recording more for better accuracy",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void recordGestureSample(GestureRecognitionService.GestureResult result, Bitmap frame) {
        if (gestureService != null && isRecording) {
            gestureService.addTrainingSample(currentGestureName, frame); // Use 'frame' parameter

            int sampleCount = gestureService.getTrainingSampleCount(currentGestureName); // Add gesture name
            tvSamplesCollected.setText("Samples: " + sampleCount);

            // Update progress bar
            int progress = Math.min(100, sampleCount * 5); // 20 samples = 100%
            pbTrainingProgress.setProgress(progress);
        }
    }

    private void saveTrainedGesture() {
        if (gestureService == null) return;

        String gestureName = etGestureName.getText().toString().trim();
        String gestureCommand = etGestureCommand.getText().toString().trim();
        String gestureType = spinnerGestureType.getSelectedItem().toString();

        if (gestureName.isEmpty()) {
            Toast.makeText(this, "Please enter a gesture name", Toast.LENGTH_SHORT).show();
            return;
        }

        pbTrainingProgress.setVisibility(View.VISIBLE);
        tvTrainingStatus.setText("Training model...");

        // Train the gesture model in background
        new Thread(() -> {
            try {
                boolean success = gestureService.trainGestureModel(gestureName, gestureCommand, gestureType);

                runOnUiThread(() -> {
                    pbTrainingProgress.setVisibility(View.GONE);

                    if (success) {
                        float accuracy = gestureService.getModelAccuracy(gestureName);
                        tvAccuracy.setText(String.format("Accuracy: %.1f%%", accuracy * 100));
                        tvTrainingStatus.setText("Training completed successfully");

                        // Add to trained gestures list
                        addToTrainedGesturesList(gestureName, gestureCommand, gestureType, accuracy);

                        // Clear input fields
                        etGestureName.setText("");
                        etGestureCommand.setText("");

                        Toast.makeText(this, "Gesture trained successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        tvTrainingStatus.setText("Training failed");
                        Toast.makeText(this, "Training failed. Please try again with more samples",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    pbTrainingProgress.setVisibility(View.GONE);
                    tvTrainingStatus.setText("Training error");
                    Toast.makeText(this, "Training error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void testGestureRecognition() {
        if (gestureService == null) return;

        isRealTimeEnabled = true;
        switchRealTimeDetection.setChecked(true);

        tvTrainingStatus.setText("Testing gesture recognition - perform gestures in front of camera");
        Toast.makeText(this, "Perform trained gestures to test recognition", Toast.LENGTH_LONG).show();
    }

    private void toggleRealTimeDetection(boolean enabled) {
        if (enabled) {
            tvTrainingStatus.setText("Real-time detection enabled");
        } else {
            tvTrainingStatus.setText("Real-time detection disabled");
            tvDetectedGesture.setText("No gesture detected");
            tvGestureConfidence.setText("Confidence: 0%");
        }
    }

    private void updateGestureVisualization(GestureRecognitionService.GestureResult result, Bitmap frame) {
        if (result != null) {
            tvDetectedGesture.setText("Gesture: " + result.gestureName);
            tvGestureConfidence.setText(String.format("Confidence: %.1f%%", result.confidence * 100));

            // Draw gesture landmarks on frame
            Bitmap visualizationBitmap = drawGestureLandmarks(frame, result);
            ivGestureVisualization.setImageBitmap(visualizationBitmap);
        }
    }

    private Bitmap drawGestureLandmarks(Bitmap frame, GestureRecognitionService.GestureResult result) {
        Bitmap mutableBitmap = frame.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(5f);
        paint.setStyle(Paint.Style.STROKE);

        // Draw gesture landmarks if available
        if (result.landmarks != null) {
            for (GestureRecognitionService.Landmark landmark : result.landmarks) {
                canvas.drawCircle(landmark.x, landmark.y, 8f, paint);
            }
        }

        return mutableBitmap;
    }

    private void addToTrainedGesturesList(String name, String command, String type, float accuracy) {
        GestureRecognitionService.GestureTrainingData data = new GestureRecognitionService.GestureTrainingData(name, command, type);
        data.accuracy = accuracy;
        data.sampleCount = gestureService.getTrainingSampleCount(name); // Add name parameter
        trainingData.add(data);
        gestureAdapter.notifyDataSetChanged();
    }

    private void loadTrainedGestures() {
        if (gestureService != null) {
            List<String> trainedGestures = gestureService.getTrainedGestures();

            trainingData.clear();
            for (String gestureName : trainedGestures) {
                GestureRecognitionService.GestureTrainingData data = new GestureRecognitionService.GestureTrainingData(gestureName, "", "Hand Gesture");
                data.accuracy = gestureService.getModelAccuracy(gestureName);
                data.sampleCount = gestureService.getSampleCount(gestureName); // Use gestureName, not currentGestureName
                trainingData.add(data);
            }

            gestureAdapter.notifyDataSetChanged();
        }
    }

    private void editTrainedGesture(int position) {
        GestureTrainingData data = trainingData.get(position);
        etGestureName.setText(data.name);
        etGestureCommand.setText(data.command);

        // Set gesture type in spinner with null check
        if (spinnerGestureType.getAdapter() != null) {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerGestureType.getAdapter();
            int typePosition = adapter.getPosition(data.type);
            if (typePosition >= 0) {
                spinnerGestureType.setSelection(typePosition);
            }
        }
    }

    private void deleteTrainedGesture(int position) {
        GestureTrainingData data = trainingData.get(position);

        if (gestureService != null) {
            boolean success = gestureService.deleteTrainedGesture(data.name);
            if (success) {
                trainingData.remove(position);
                gestureAdapter.notifyDataSetChanged();
                Toast.makeText(this, "Gesture deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to delete gesture", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateUI() {
        btnStopRecording.setEnabled(false);
        btnSaveGesture.setEnabled(false);
        pbTrainingProgress.setVisibility(View.GONE);

        // Set default values
        seekBarSensitivity.setProgress(50);
        seekBarMinConfidence.setProgress(70);
        tvSensitivityValue.setText("0.50");
        tvMinConfidenceValue.setText("0.70");

        tvTrainingStatus.setText("Ready for gesture training");
        tvSamplesCollected.setText("Samples: 0");
        tvAccuracy.setText("Accuracy: 0%");
        tvDetectedGesture.setText("No gesture detected");
        tvGestureConfidence.setText("Confidence: 0%");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    // Data classes
    public static class GestureTrainingData {
        public String name;
        public String command;
        public String type;
        public float accuracy;
        public int sampleCount;
    }


    private static class TrainedGestureAdapter extends BaseAdapter {
        private Context context;
        private List<GestureTrainingData> data;

        private TrainedGestureAdapter(Context context, java.util.List<GestureTrainingData> trainingData) {
            this.context = context;
            this.data = data;
        }

        @Override
        public int getCount() { return data.size(); }

        @Override
        public Object getItem(int position) { return data.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Simple text view for now
            TextView textView = new TextView(context);
            GestureTrainingData item = data.get(position);
            textView.setText(item.name + " - " + String.format("%.1f%%", item.accuracy * 100));
            return textView;
        }
    }
}
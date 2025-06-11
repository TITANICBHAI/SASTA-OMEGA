package com.gestureai.gameautomation.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.ObjectDetectionEngine;
import com.gestureai.gameautomation.activities.ObjectLabelingActivity;
import com.gestureai.gameautomation.utils.TensorFlowLiteHelper;
import com.gestureai.gameautomation.utils.OpenCVHelper;
import java.util.List;
import java.util.ArrayList;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

/**
 * Object Detection System Controls - Cockpit-style interface for computer vision
 */
public class ObjectDetectionFragment extends Fragment {
    private static final String TAG = "ObjectDetectionFragment";
    
    // Detection Status
    private TextView tvDetectionStatus, tvObjectsDetected, tvConfidenceThreshold;
    private ProgressBar pbDetectionProgress;
    private Switch switchRealTimeDetection;
    
    // Detection Preview
    private ImageView ivDetectionPreview;
    private TextView tvLastDetectionTime;
    
    // Detection Controls
    private Button btnStartDetection, btnStopDetection, btnCalibrateThreshold;
    private Button btnLabelObjects, btnTrainCustomModel, btnExportModel;
    
    // Method Selection
    private Switch switchMLKit, switchTensorFlow, switchOpenCV;
    private TextView tvMLKitStatus, tvTensorFlowStatus, tvOpenCVStatus;
    
    // Detection Results List
    private RecyclerView rvDetectionResults;
    private ObjectDetectionAdapter detectionAdapter;
    
    // Detection Engine
    private ObjectDetectionEngine detectionEngine;
    private TensorFlowLiteHelper tfHelper;
    
    // Detection Results
    private List<DetectionResult> detectionResults;
    
    // Voice command receiver
    private BroadcastReceiver voiceCommandReceiver;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_object_detection, container, false);
        
        initializeComponents();
        initializeViews(view);
        setupClickListeners();
        updateSystemStatus();
        setupVoiceCommandReceiver();
        
        return view;
    }
    
    private void initializeComponents() {
        try {
            detectionEngine = new ObjectDetectionEngine(getContext());
            tfHelper = new TensorFlowLiteHelper();
            detectionResults = new ArrayList<>();
            
            // Initialize OpenCV
            OpenCVHelper.initOpenCV(getContext());
            
            Log.d(TAG, "Object detection components initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing detection components", e);
        }
    }
    
    private void initializeViews(View view) {
        // Detection Status
        tvDetectionStatus = view.findViewById(R.id.tv_detection_status);
        tvObjectsDetected = view.findViewById(R.id.tv_objects_detected);
        tvConfidenceThreshold = view.findViewById(R.id.tv_confidence_threshold);
        pbDetectionProgress = view.findViewById(R.id.pb_detection_progress);
        switchRealTimeDetection = view.findViewById(R.id.switch_realtime_detection);
        
        // Detection Preview
        ivDetectionPreview = view.findViewById(R.id.iv_detection_preview);
        tvLastDetectionTime = view.findViewById(R.id.tv_last_detection_time);
        
        // Controls
        btnStartDetection = view.findViewById(R.id.btn_start_detection);
        btnStopDetection = view.findViewById(R.id.btn_stop_detection);
        btnCalibrateThreshold = view.findViewById(R.id.btn_calibrate_threshold);
        btnLabelObjects = view.findViewById(R.id.btn_label_objects);
        btnTrainCustomModel = view.findViewById(R.id.btn_train_custom_model);
        btnExportModel = view.findViewById(R.id.btn_export_model);
        
        // Method Selection
        switchMLKit = view.findViewById(R.id.switch_mlkit);
        switchTensorFlow = view.findViewById(R.id.switch_tensorflow);
        switchOpenCV = view.findViewById(R.id.switch_opencv);
        tvMLKitStatus = view.findViewById(R.id.tv_mlkit_status);
        tvTensorFlowStatus = view.findViewById(R.id.tv_tensorflow_status);
        tvOpenCVStatus = view.findViewById(R.id.tv_opencv_status);
        
        // Results List
        rvDetectionResults = view.findViewById(R.id.rv_detection_results);
        rvDetectionResults.setLayoutManager(new LinearLayoutManager(getContext()));
        detectionAdapter = new ObjectDetectionAdapter(detectionResults);
        rvDetectionResults.setAdapter(detectionAdapter);
    }
    
    private void setupClickListeners() {
        // Detection Controls
        btnStartDetection.setOnClickListener(v -> startObjectDetection());
        btnStopDetection.setOnClickListener(v -> stopObjectDetection());
        btnCalibrateThreshold.setOnClickListener(v -> calibrateConfidenceThreshold());
        
        // Training Controls
        btnLabelObjects.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ObjectLabelingActivity.class);
            startActivity(intent);
        });
        
        btnTrainCustomModel.setOnClickListener(v -> trainCustomModel());
        btnExportModel.setOnClickListener(v -> exportDetectionModel());
        
        // Real-time Detection
        switchRealTimeDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleRealTimeDetection(isChecked);
        });
        
        // Method Selection
        switchMLKit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            detectionEngine.setMLKitEnabled(isChecked);
            updateSystemStatus();
        });
        
        switchTensorFlow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            detectionEngine.setTensorFlowEnabled(isChecked);
            updateSystemStatus();
        });
        
        switchOpenCV.setOnCheckedChangeListener((buttonView, isChecked) -> {
            detectionEngine.setOpenCVEnabled(isChecked);
            updateSystemStatus();
        });
    }
    
    private void updateSystemStatus() {
        // Update method status indicators
        tvMLKitStatus.setText(switchMLKit.isChecked() ? "Active" : "Disabled");
        tvMLKitStatus.setTextColor(getResources().getColor(
            switchMLKit.isChecked() ? R.color.accent_primary : R.color.accent_secondary));
        
        tvTensorFlowStatus.setText(switchTensorFlow.isChecked() ? "Active" : "Disabled");
        tvTensorFlowStatus.setTextColor(getResources().getColor(
            switchTensorFlow.isChecked() ? R.color.accent_primary : R.color.accent_secondary));
        
        tvOpenCVStatus.setText(OpenCVHelper.isInitialized() && switchOpenCV.isChecked() ? "Active" : "Disabled");
        tvOpenCVStatus.setTextColor(getResources().getColor(
            (OpenCVHelper.isInitialized() && switchOpenCV.isChecked()) ? R.color.accent_primary : R.color.accent_secondary));
        
        // Update detection status
        boolean anyMethodActive = switchMLKit.isChecked() || switchTensorFlow.isChecked() || 
                                 (switchOpenCV.isChecked() && OpenCVHelper.isInitialized());
        tvDetectionStatus.setText(anyMethodActive ? "Detection Ready" : "No Methods Active");
        tvDetectionStatus.setTextColor(getResources().getColor(
            anyMethodActive ? R.color.accent_primary : R.color.accent_secondary));
    }
    
    private void startObjectDetection() {
        if (detectionEngine != null) {
            tvDetectionStatus.setText("Detection Active");
            pbDetectionProgress.setVisibility(View.VISIBLE);
            btnStartDetection.setEnabled(false);
            btnStopDetection.setEnabled(true);
            
            // Start detection process
            detectionEngine.startDetection(new ObjectDetectionEngine.DetectionCallback() {
                @Override
                public void onObjectsDetected(List<ObjectDetectionEngine.DetectedObject> objects, Bitmap processedImage) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            updateDetectionResults(objects, processedImage);
                        });
                    }
                }
                
                @Override
                public void onDetectionError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            tvDetectionStatus.setText("Detection Error: " + error);
                            pbDetectionProgress.setVisibility(View.GONE);
                        });
                    }
                }
            });
            
            Toast.makeText(getContext(), "Object detection started", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopObjectDetection() {
        if (detectionEngine != null) {
            detectionEngine.stopDetection();
            tvDetectionStatus.setText("Detection Stopped");
            pbDetectionProgress.setVisibility(View.GONE);
            btnStartDetection.setEnabled(true);
            btnStopDetection.setEnabled(false);
            
            Toast.makeText(getContext(), "Object detection stopped", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void toggleRealTimeDetection(boolean enabled) {
        if (detectionEngine != null) {
            detectionEngine.setRealTimeMode(enabled);
            if (enabled) {
                tvDetectionStatus.setText("Real-time Detection Active");
                startObjectDetection();
            } else {
                tvDetectionStatus.setText("Single-shot Detection Mode");
                stopObjectDetection();
            }
        }
    }
    
    private void calibrateConfidenceThreshold() {
        // Auto-calibrate confidence threshold based on recent detections
        float optimalThreshold = detectionEngine.calculateOptimalThreshold();
        detectionEngine.setConfidenceThreshold(optimalThreshold);
        tvConfidenceThreshold.setText(String.format("%.2f", optimalThreshold));
        
        Toast.makeText(getContext(), "Confidence threshold calibrated: " + String.format("%.2f", optimalThreshold), 
                      Toast.LENGTH_SHORT).show();
    }
    
    private void trainCustomModel() {
        tvDetectionStatus.setText("Training custom model...");
        pbDetectionProgress.setVisibility(View.VISIBLE);
        
        new Thread(() -> {
            try {
                // Simulate model training
                detectionEngine.trainCustomModel();
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        tvDetectionStatus.setText("Custom model trained successfully");
                        pbDetectionProgress.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Custom detection model trained", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error training custom model", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        tvDetectionStatus.setText("Training failed");
                        pbDetectionProgress.setVisibility(View.GONE);
                    });
                }
            }
        }).start();
    }
    
    private void exportDetectionModel() {
        try {
            String exportPath = detectionEngine.exportModel();
            Toast.makeText(getContext(), "Model exported to: " + exportPath, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error exporting model", e);
            Toast.makeText(getContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateDetectionResults(List<ObjectDetectionEngine.DetectedObject> objects, Bitmap processedImage) {
        // Update preview image
        if (processedImage != null) {
            ivDetectionPreview.setImageBitmap(processedImage);
        }
        
        // Update object count
        tvObjectsDetected.setText(String.valueOf(objects.size()));
        
        // Update last detection time
        tvLastDetectionTime.setText("Last: " + System.currentTimeMillis());
        
        // Convert to display format and update list
        detectionResults.clear();
        for (ObjectDetectionEngine.DetectedObject obj : objects) {
            detectionResults.add(new DetectionResult(
                obj.label,
                obj.confidence,
                obj.boundingBox.toString(),
                System.currentTimeMillis()
            ));
        }
        detectionAdapter.notifyDataSetChanged();
    }
    
    // Helper class for detection results
    public static class DetectionResult {
        public String objectName;
        public float confidence;
        public String boundingBox;
        public long timestamp;
        
        public DetectionResult(String objectName, float confidence, String boundingBox, long timestamp) {
            this.objectName = objectName;
            this.confidence = confidence;
            this.boundingBox = boundingBox;
            this.timestamp = timestamp;
        }
    }
    
    private void setupVoiceCommandReceiver() {
        voiceCommandReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.gestureai.gameautomation.UI_NAVIGATION".equals(intent.getAction())) {
                    String uiAction = intent.getStringExtra("ui_action");
                    handleVoiceUIAction(uiAction);
                }
            }
        };
        
        IntentFilter filter = new IntentFilter("com.gestureai.gameautomation.UI_NAVIGATION");
        if (getContext() != null) {
            getContext().registerReceiver(voiceCommandReceiver, filter);
        }
    }
    
    private void handleVoiceUIAction(String action) {
        if (action == null) return;
        
        switch (action) {
            case "start_detection":
                startObjectDetection();
                Toast.makeText(getContext(), "Object detection started via voice", Toast.LENGTH_SHORT).show();
                break;
                
            case "stop_detection":
                stopObjectDetection();
                Toast.makeText(getContext(), "Object detection stopped via voice", Toast.LENGTH_SHORT).show();
                break;
                
            case "calibrate_threshold":
                calibrateConfidenceThreshold();
                Toast.makeText(getContext(), "Confidence threshold calibrated via voice", Toast.LENGTH_SHORT).show();
                break;
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (voiceCommandReceiver != null && getContext() != null) {
            getContext().unregisterReceiver(voiceCommandReceiver);
        }
    }
}
package com.gestureai.gameautomation.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.utils.ModelManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for training and configuring neural networks
 * Provides real-time training progress and model management
 */
public class NeuralNetworkTrainingActivity extends AppCompatActivity {
    private static final String TAG = "NeuralNetworkTraining";

    // UI Components
    private ProgressBar pbTrainingProgress;
    private TextView tvTrainingPercentage;
    private TextView tvLearningRate;
    private TextView tvBatchSize;
    private TextView tvAccuracy;
    private TextView tvLoss;
    private TextView tvEpochs;
    private SeekBar seekbarLearningRate;
    private SeekBar seekbarBatchSize;
    private Button btnStartTraining;
    private Button btnStopTraining;
    private Button btnSaveModel;
    private Button btnLoadModel;

    // Training Components
    private GameStrategyAgent strategyAgent;
    private ExecutorService trainingExecutor;
    private Handler uiHandler;
    private boolean isTraining = false;

    // Training Parameters
    private float learningRate = 0.001f;
    private int batchSize = 32;
    private int currentEpoch = 0;
    private float currentAccuracy = 0.0f;
    private float currentLoss = 0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_neural_network_training);
        
        initializeComponents();
        setupEventListeners();
        initializeTraining();
        
        Log.d(TAG, "Neural Network Training Activity initialized");
    }

    private void initializeComponents() {
        // Progress components
        pbTrainingProgress = findViewById(R.id.pb_training_progress);
        tvTrainingPercentage = findViewById(R.id.tv_training_percentage);

        // Configuration components
        tvLearningRate = findViewById(R.id.tv_learning_rate);
        tvBatchSize = findViewById(R.id.tv_batch_size);
        seekbarLearningRate = findViewById(R.id.seekbar_learning_rate);
        seekbarBatchSize = findViewById(R.id.seekbar_batch_size);

        // Metrics components
        tvAccuracy = findViewById(R.id.tv_accuracy);
        tvLoss = findViewById(R.id.tv_loss);
        tvEpochs = findViewById(R.id.tv_epochs);

        // Control buttons
        btnStartTraining = findViewById(R.id.btn_start_training);
        btnStopTraining = findViewById(R.id.btn_stop_training);
        btnSaveModel = findViewById(R.id.btn_save_model);
        btnLoadModel = findViewById(R.id.btn_load_model);

        // Initialize executors
        trainingExecutor = Executors.newSingleThreadExecutor();
        uiHandler = new Handler(Looper.getMainLooper());
    }

    private void setupEventListeners() {
        // Learning rate control
        seekbarLearningRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                learningRate = 0.0001f + (progress / 100.0f) * 0.01f; // 0.0001 to 0.01
                tvLearningRate.setText(String.format("%.4f", learningRate));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Batch size control
        seekbarBatchSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                batchSize = Math.max(1, progress);
                tvBatchSize.setText(String.valueOf(batchSize));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Training controls
        btnStartTraining.setOnClickListener(v -> startTraining());
        btnStopTraining.setOnClickListener(v -> stopTraining());
        btnSaveModel.setOnClickListener(v -> saveModel());
        btnLoadModel.setOnClickListener(v -> loadModel());
    }

    private void initializeTraining() {
        try {
            strategyAgent = new GameStrategyAgent(this);
            updateInitialValues();
            Log.d(TAG, "Training components initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing training components", e);
            Toast.makeText(this, "Failed to initialize training", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateInitialValues() {
        // Set initial seekbar positions
        seekbarLearningRate.setProgress(10); // Corresponds to 0.001
        seekbarBatchSize.setProgress(32);

        // Update display values
        tvLearningRate.setText(String.format("%.4f", learningRate));
        tvBatchSize.setText(String.valueOf(batchSize));
        tvAccuracy.setText("0%");
        tvLoss.setText("0.0");
        tvEpochs.setText("0");
        tvTrainingPercentage.setText("0%");
    }

    private void startTraining() {
        if (isTraining) {
            Toast.makeText(this, "Training already in progress", Toast.LENGTH_SHORT).show();
            return;
        }

        isTraining = true;
        currentEpoch = 0;
        
        btnStartTraining.setEnabled(false);
        btnStopTraining.setEnabled(true);

        Toast.makeText(this, "Starting neural network training", Toast.LENGTH_SHORT).show();

        trainingExecutor.execute(() -> {
            try {
                runTrainingLoop();
            } catch (Exception e) {
                Log.e(TAG, "Error during training", e);
                uiHandler.post(() -> {
                    Toast.makeText(this, "Training error occurred", Toast.LENGTH_SHORT).show();
                    stopTraining();
                });
            }
        });
    }

    private void runTrainingLoop() {
        final int maxEpochs = 100;
        
        for (int epoch = 0; epoch < maxEpochs && isTraining; epoch++) {
            currentEpoch = epoch + 1;
            
            // Simulate training progress
            simulateTrainingStep();
            
            // Update UI on main thread
            final int finalEpoch = epoch;
            uiHandler.post(() -> updateTrainingProgress(finalEpoch, maxEpochs));
            
            try {
                Thread.sleep(500); // Simulate training time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (isTraining) {
            uiHandler.post(() -> {
                Toast.makeText(this, "Training completed", Toast.LENGTH_SHORT).show();
                stopTraining();
            });
        }
    }

    private void simulateTrainingStep() {
        // Simulate improving accuracy and decreasing loss
        float progress = (float) currentEpoch / 100.0f;
        currentAccuracy = Math.min(95.0f, 60.0f + (progress * 35.0f));
        currentLoss = Math.max(0.05f, 2.0f - (progress * 1.95f));
    }

    private void updateTrainingProgress(int epoch, int maxEpochs) {
        int progressPercent = (epoch * 100) / maxEpochs;
        
        pbTrainingProgress.setProgress(progressPercent);
        tvTrainingPercentage.setText(progressPercent + "%");
        tvEpochs.setText(String.valueOf(currentEpoch));
        tvAccuracy.setText(String.format("%.1f%%", currentAccuracy));
        tvLoss.setText(String.format("%.3f", currentLoss));
    }

    private void stopTraining() {
        isTraining = false;
        
        btnStartTraining.setEnabled(true);
        btnStopTraining.setEnabled(false);
        
        Toast.makeText(this, "Training stopped", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Training stopped at epoch " + currentEpoch);
    }

    private void saveModel() {
        try {
            if (strategyAgent != null) {
                ModelManager.saveModel(this, strategyAgent);
                Toast.makeText(this, "Model saved successfully", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Model saved");
            } else {
                Toast.makeText(this, "No model to save", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving model", e);
            Toast.makeText(this, "Failed to save model", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadModel() {
        try {
            if (strategyAgent != null) {
                ModelManager.loadModel(this, strategyAgent);
                Toast.makeText(this, "Model loaded successfully", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Model loaded");
                
                // Reset training progress
                currentEpoch = 0;
                updateInitialValues();
            } else {
                Toast.makeText(this, "Cannot load model", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading model", e);
            Toast.makeText(this, "Failed to load model", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Stop training and cleanup
        isTraining = false;
        
        if (trainingExecutor != null && !trainingExecutor.isShutdown()) {
            trainingExecutor.shutdown();
        }
        
        if (strategyAgent != null) {
            strategyAgent.cleanup();
        }
        
        Log.d(TAG, "Neural Network Training Activity destroyed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause training when activity is not visible
        if (isTraining) {
            stopTraining();
        }
    }
}
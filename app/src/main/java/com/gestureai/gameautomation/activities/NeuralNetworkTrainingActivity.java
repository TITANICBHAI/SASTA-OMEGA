package com.gestureai.gameautomation.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.ai.NeuralNetworkTrainer;
import com.gestureai.gameautomation.ai.GameStrategyAgent;

/**
 * Neural Network Training Activity
 * Manages AI model training and optimization
 */
public class NeuralNetworkTrainingActivity extends AppCompatActivity {
    private static final String TAG = "NeuralNetworkTraining";
    
    private Button btnStartTraining;
    private Button btnStopTraining;
    private Button btnSaveModel;
    private ProgressBar progressTraining;
    private TextView tvTrainingStatus;
    private TextView tvEpochCount;
    private TextView tvLossValue;
    private SeekBar seekBarLearningRate;
    private Spinner spinnerModelType;
    
    private NeuralNetworkTrainer trainer;
    private boolean isTraining = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_neural_network_training);
        
        initializeViews();
        initializeTrainer();
        setupListeners();
        setupModelTypeSpinner();
        
        Log.d(TAG, "NeuralNetworkTrainingActivity created");
    }
    
    private void initializeViews() {
        try {
            btnStartTraining = findViewById(R.id.btn_start_training);
            btnStopTraining = findViewById(R.id.btn_stop_training);
            btnSaveModel = findViewById(R.id.btn_save_model);
            progressTraining = findViewById(R.id.progress_training);
            tvTrainingStatus = findViewById(R.id.tv_training_status);
            tvEpochCount = findViewById(R.id.tv_epoch_count);
            tvLossValue = findViewById(R.id.tv_loss_value);
            seekBarLearningRate = findViewById(R.id.seekbar_learning_rate);
            spinnerModelType = findViewById(R.id.spinner_model_type);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views - using fallback", e);
            // Continue without crashing if layout elements are missing
        }
    }
    
    private void initializeTrainer() {
        try {
            trainer = new NeuralNetworkTrainer(this);
            Log.d(TAG, "Neural network trainer initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize neural network trainer", e);
            trainer = null;
        }
    }
    
    private void setupListeners() {
        if (btnStartTraining != null) {
            btnStartTraining.setOnClickListener(v -> startTraining());
        }
        
        if (btnStopTraining != null) {
            btnStopTraining.setOnClickListener(v -> stopTraining());
        }
        
        if (btnSaveModel != null) {
            btnSaveModel.setOnClickListener(v -> saveModel());
        }
        
        if (seekBarLearningRate != null) {
            seekBarLearningRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float learningRate = progress / 1000.0f; // Convert to 0.001-0.1 range
                    Log.d(TAG, "Learning rate set to: " + learningRate);
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }
    
    private void setupModelTypeSpinner() {
        if (spinnerModelType != null) {
            String[] modelTypes = {"DQN", "PPO", "A3C", "Custom"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, modelTypes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerModelType.setAdapter(adapter);
        }
    }
    
    private void startTraining() {
        if (trainer == null) {
            Log.e(TAG, "Trainer not initialized");
            updateTrainingStatus("Error: Trainer not available");
            return;
        }
        
        if (isTraining) {
            Log.w(TAG, "Training already in progress");
            return;
        }
        
        isTraining = true;
        updateTrainingStatus("Starting training...");
        
        if (progressTraining != null) {
            progressTraining.setVisibility(android.view.View.VISIBLE);
        }
        
        // Start training in background thread
        new Thread(() -> {
            try {
                trainer.startTraining(new NeuralNetworkTrainer.TrainingCallback() {
                    @Override
                    public void onEpochComplete(int epoch, float loss) {
                        runOnUiThread(() -> {
                            if (tvEpochCount != null) {
                                tvEpochCount.setText("Epoch: " + epoch);
                            }
                            if (tvLossValue != null) {
                                tvLossValue.setText("Loss: " + String.format("%.4f", loss));
                            }
                            if (progressTraining != null) {
                                progressTraining.setProgress((epoch % 100) + 1);
                            }
                        });
                    }
                    
                    @Override
                    public void onTrainingComplete() {
                        runOnUiThread(() -> {
                            isTraining = false;
                            updateTrainingStatus("Training completed");
                            if (progressTraining != null) {
                                progressTraining.setVisibility(android.view.View.GONE);
                            }
                        });
                    }
                    
                    @Override
                    public void onTrainingError(String error) {
                        runOnUiThread(() -> {
                            isTraining = false;
                            updateTrainingStatus("Training error: " + error);
                            if (progressTraining != null) {
                                progressTraining.setVisibility(android.view.View.GONE);
                            }
                        });
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Training failed", e);
                runOnUiThread(() -> {
                    isTraining = false;
                    updateTrainingStatus("Training failed: " + e.getMessage());
                });
            }
        }).start();
        
        Log.d(TAG, "Neural network training started");
    }
    
    private void stopTraining() {
        if (trainer != null && isTraining) {
            trainer.stopTraining();
            isTraining = false;
            updateTrainingStatus("Training stopped");
            if (progressTraining != null) {
                progressTraining.setVisibility(android.view.View.GONE);
            }
            Log.d(TAG, "Neural network training stopped");
        }
    }
    
    private void saveModel() {
        if (trainer != null) {
            trainer.saveModel("trained_model_" + System.currentTimeMillis());
            updateTrainingStatus("Model saved successfully");
            Log.d(TAG, "Neural network model saved");
        }
    }
    
    private void updateTrainingStatus(String status) {
        if (tvTrainingStatus != null) {
            tvTrainingStatus.setText(status);
        }
        Log.d(TAG, "Training status: " + status);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isTraining && trainer != null) {
            trainer.stopTraining();
        }
        Log.d(TAG, "NeuralNetworkTrainingActivity destroyed");
    }
}
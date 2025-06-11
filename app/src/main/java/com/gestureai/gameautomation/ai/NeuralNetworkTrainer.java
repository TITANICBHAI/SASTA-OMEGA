package com.gestureai.gameautomation.ai;

import android.content.Context;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NeuralNetworkTrainer {
    private static final String TAG = "NeuralNetworkTrainer";
    
    private Context context;
    private ExecutorService trainingExecutor;
    private volatile boolean isTraining = false;
    private volatile float learningRate = 0.001f;
    private volatile int batchSize = 32;
    private volatile int epochs = 100;
    private final List<TrainingListener> listeners;
    private final Object trainingLock = new Object();
    
    public interface TrainingListener {
        void onTrainingStarted();
        void onEpochCompleted(int epoch, float loss, float accuracy);
        void onTrainingCompleted(float finalLoss, float finalAccuracy);
        void onTrainingFailed(Exception error);
    }
    
    public interface TrainingCallback {
        void onEpochComplete(int epoch, float loss);
        void onTrainingComplete();
        void onTrainingError(String error);
    }
    
    public static class TrainingData {
        public float[] inputs;
        public float[] outputs;
        public String label;
        public long timestamp;
        
        public TrainingData(float[] inputs, float[] outputs, String label) {
            this.inputs = inputs;
            this.outputs = outputs;
            this.label = label;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public NeuralNetworkTrainer(Context context) {
        this.context = context;
        this.trainingExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "NeuralNetworkTraining");
            t.setDaemon(false);
            return t;
        });
        this.listeners = new ArrayList<>();
        Log.d(TAG, "NeuralNetworkTrainer initialized");
    }
    
    public void cleanup() {
        synchronized (trainingLock) {
            if (trainingExecutor != null && !trainingExecutor.isShutdown()) {
                trainingExecutor.shutdown();
                try {
                    if (!trainingExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        trainingExecutor.shutdownNow();
                        Log.w(TAG, "Training executor forced shutdown");
                    }
                } catch (InterruptedException e) {
                    trainingExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                    Log.w(TAG, "Training executor shutdown interrupted");
                }
            }
            
            synchronized (listeners) {
                listeners.clear();
            }
            
            isTraining = false;
            Log.d(TAG, "NeuralNetworkTrainer cleaned up");
        }
    }
    
    public void addTrainingListener(TrainingListener listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
                Log.d(TAG, "TrainingListener added");
            }
        }
    }
    
    public void removeTrainingListener(TrainingListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
            Log.d(TAG, "TrainingListener removed");
        }
    }
    
    public void setLearningRate(float rate) {
        this.learningRate = Math.max(0.0001f, Math.min(0.1f, rate));
        Log.d(TAG, "Learning rate set to: " + learningRate);
    }
    
    public void setBatchSize(int size) {
        this.batchSize = Math.max(1, Math.min(128, size));
        Log.d(TAG, "Batch size set to: " + batchSize);
    }
    
    public void setEpochs(int epochCount) {
        this.epochs = Math.max(1, Math.min(1000, epochCount));
        Log.d(TAG, "Epochs set to: " + epochs);
    }
    
    public void trainNetwork(List<TrainingData> trainingData, String modelType) {
        synchronized (trainingLock) {
            if (isTraining) {
                Log.w(TAG, "Training already in progress");
                return;
            }
            
            if (trainingData == null || trainingData.isEmpty()) {
                Log.e(TAG, "No training data provided");
                notifyTrainingFailed(new IllegalArgumentException("No training data"));
                return;
            }
            
            isTraining = true;
        }
        
        trainingExecutor.execute(() -> {
            try {
                performTraining(trainingData, modelType);
            } catch (Exception e) {
                Log.e(TAG, "Training failed", e);
                notifyTrainingFailed(e);
            } finally {
                synchronized (trainingLock) {
                    isTraining = false;
                }
            }
        });
    }
    
    private void performTraining(List<TrainingData> trainingData, String modelType) {
        isTraining = true;
        notifyTrainingStarted();
        
        Log.d(TAG, "Starting training with " + trainingData.size() + " samples for model: " + modelType);
        
        try {
            // Initialize network based on model type
            SimpleNeuralNetwork network = createNetworkForType(modelType);
            
            float bestLoss = Float.MAX_VALUE;
            float finalAccuracy = 0f;
            
            for (int epoch = 0; epoch < epochs; epoch++) {
                float epochLoss = 0f;
                float epochAccuracy = 0f;
                int correctPredictions = 0;
                
                // Shuffle training data for each epoch
                List<TrainingData> shuffledData = new ArrayList<>(trainingData);
                java.util.Collections.shuffle(shuffledData);
                
                // Process data in batches
                for (int i = 0; i < shuffledData.size(); i += batchSize) {
                    int endIndex = Math.min(i + batchSize, shuffledData.size());
                    List<TrainingData> batch = shuffledData.subList(i, endIndex);
                    
                    // Forward pass and backpropagation for batch
                    float batchLoss = trainBatch(network, batch);
                    epochLoss += batchLoss;
                    
                    // Calculate accuracy for this batch
                    for (TrainingData data : batch) {
                        float[] prediction = network.predict(data.inputs);
                        if (isCorrectPrediction(prediction, data.outputs)) {
                            correctPredictions++;
                        }
                    }
                }
                
                epochLoss /= (float) Math.ceil((double) shuffledData.size() / batchSize);
                epochAccuracy = (float) correctPredictions / shuffledData.size();
                
                if (epochLoss < bestLoss) {
                    bestLoss = epochLoss;
                    finalAccuracy = epochAccuracy;
                }
                
                // Notify epoch completion
                notifyEpochCompleted(epoch + 1, epochLoss, epochAccuracy);
                
                Log.d(TAG, String.format("Epoch %d/%d - Loss: %.4f, Accuracy: %.2f%%", 
                      epoch + 1, epochs, epochLoss, epochAccuracy * 100));
                
                // Early stopping if loss is very low
                if (epochLoss < 0.001f) {
                    Log.d(TAG, "Early stopping - loss threshold reached");
                    break;
                }
            }
            
            notifyTrainingCompleted(bestLoss, finalAccuracy);
            Log.d(TAG, "Training completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during training", e);
            notifyTrainingFailed(e);
        } finally {
            isTraining = false;
        }
    }
    
    private SimpleNeuralNetwork createNetworkForType(String modelType) {
        switch (modelType.toUpperCase()) {
            case "GESTURE_RECOGNITION":
                return new SimpleNeuralNetwork(new int[]{20, 64, 32, 10});
            case "OBJECT_DETECTION":
                return new SimpleNeuralNetwork(new int[]{100, 128, 64, 20});
            case "STRATEGY_PLANNING":
                return new SimpleNeuralNetwork(new int[]{50, 128, 64, 32, 8});
            case "REWARD_PREDICTION":
                return new SimpleNeuralNetwork(new int[]{30, 64, 32, 1});
            default:
                Log.w(TAG, "Unknown model type: " + modelType + ", using default");
                return new SimpleNeuralNetwork(new int[]{10, 32, 16, 5});
        }
    }
    
    private float trainBatch(SimpleNeuralNetwork network, List<TrainingData> batch) {
        float totalLoss = 0f;
        
        for (TrainingData data : batch) {
            // Forward pass
            float[] prediction = network.predict(data.inputs);
            
            // Calculate loss (Mean Squared Error)
            float loss = 0f;
            for (int i = 0; i < prediction.length && i < data.outputs.length; i++) {
                float error = prediction[i] - data.outputs[i];
                loss += error * error;
            }
            loss /= prediction.length;
            totalLoss += loss;
            
            // Backward pass (simplified gradient descent)
            network.updateWeights(data.inputs, data.outputs, learningRate);
        }
        
        return totalLoss / batch.size();
    }
    
    private boolean isCorrectPrediction(float[] prediction, float[] target) {
        if (prediction.length != target.length) return false;
        
        // For classification, check if highest prediction matches highest target
        int predIndex = getMaxIndex(prediction);
        int targetIndex = getMaxIndex(target);
        
        return predIndex == targetIndex;
    }
    
    private int getMaxIndex(float[] array) {
        int maxIndex = 0;
        float maxValue = array[0];
        
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }
        
        return maxIndex;
    }
    
    private void notifyTrainingStarted() {
        for (TrainingListener listener : listeners) {
            listener.onTrainingStarted();
        }
    }
    
    private void notifyEpochCompleted(int epoch, float loss, float accuracy) {
        for (TrainingListener listener : listeners) {
            listener.onEpochCompleted(epoch, loss, accuracy);
        }
    }
    
    private void notifyTrainingCompleted(float finalLoss, float finalAccuracy) {
        for (TrainingListener listener : listeners) {
            listener.onTrainingCompleted(finalLoss, finalAccuracy);
        }
    }
    
    private void notifyTrainingFailed(Exception error) {
        for (TrainingListener listener : listeners) {
            listener.onTrainingFailed(error);
        }
    }
    
    public boolean isTraining() {
        return isTraining;
    }
    
    public float getLearningRate() {
        return learningRate;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public int getEpochs() {
        return epochs;
    }
    
    public void startTraining(TrainingCallback callback) {
        if (isTraining) {
            Log.w(TAG, "Training already in progress");
            callback.onTrainingError("Training already in progress");
            return;
        }
        
        trainingExecutor.execute(() -> {
            try {
                isTraining = true;
                
                // Simulate training process
                for (int epoch = 0; epoch < epochs && isTraining; epoch++) {
                    // Simulate epoch processing
                    Thread.sleep(100); // Simulate computation time
                    
                    float simulatedLoss = 1.0f - (epoch / (float) epochs) + (float)(Math.random() * 0.1);
                    callback.onEpochComplete(epoch + 1, simulatedLoss);
                    
                    // Check if training should stop
                    if (!isTraining) {
                        callback.onTrainingError("Training stopped by user");
                        return;
                    }
                }
                
                isTraining = false;
                callback.onTrainingComplete();
                Log.d(TAG, "Training completed successfully");
                
            } catch (Exception e) {
                isTraining = false;
                Log.e(TAG, "Training failed", e);
                callback.onTrainingError(e.getMessage());
            }
        });
    }
    
    public void saveModel(String modelName) {
        Log.d(TAG, "Saving model: " + modelName);
        // Model saving logic would go here
        // For now, just log the save operation
    }

    public void stopTraining() {
        if (isTraining) {
            isTraining = false;
            Log.d(TAG, "Training stop requested");
        }
    }
    
    public void cleanup() {
        stopTraining();
        if (trainingExecutor != null && !trainingExecutor.isShutdown()) {
            trainingExecutor.shutdown();
        }
        listeners.clear();
        Log.d(TAG, "NeuralNetworkTrainer cleaned up");
    }
}
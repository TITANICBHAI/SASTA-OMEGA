package com.gestureai.gameautomation.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages TensorFlow Lite model loading and inference
 * Provides thread-safe model management and fallback mechanisms
 */
public class TensorFlowLiteManager {
    private static final String TAG = "TensorFlowLiteManager";
    private static volatile TensorFlowLiteManager instance;
    
    private final ConcurrentHashMap<String, Interpreter> loadedModels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> modelStatus = new ConcurrentHashMap<>();
    private Context applicationContext;
    private boolean initializationFailed = false;
    
    public static TensorFlowLiteManager getInstance() {
        if (instance == null) {
            synchronized (TensorFlowLiteManager.class) {
                if (instance == null) {
                    instance = new TensorFlowLiteManager();
                }
            }
        }
        return instance;
    }
    
    public void initialize(Context context) {
        this.applicationContext = context.getApplicationContext();
        Log.d(TAG, "TensorFlow Lite Manager initialized");
    }
    
    /**
     * Load model from assets with comprehensive error handling
     */
    public boolean loadModel(String modelName, String assetPath) {
        if (initializationFailed) {
            Log.w(TAG, "TensorFlow Lite initialization failed, cannot load models");
            return false;
        }
        
        try {
            // Check if model already loaded
            if (loadedModels.containsKey(modelName) && modelStatus.getOrDefault(modelName, false)) {
                Log.d(TAG, "Model " + modelName + " already loaded");
                return true;
            }
            
            // Load model from assets
            MappedByteBuffer modelBuffer = loadModelFile(assetPath);
            if (modelBuffer == null) {
                Log.w(TAG, "Model file not found: " + assetPath);
                return false;
            }
            
            // Create interpreter with options
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2); // Conservative thread count
            options.setUseNNAPI(false); // Disable NNAPI for compatibility
            
            Interpreter interpreter = new Interpreter(modelBuffer, options);
            
            // Verify model can be used
            if (verifyModel(interpreter)) {
                loadedModels.put(modelName, interpreter);
                modelStatus.put(modelName, true);
                Log.i(TAG, "Successfully loaded model: " + modelName);
                return true;
            } else {
                interpreter.close();
                Log.w(TAG, "Model verification failed: " + modelName);
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to load model " + modelName, e);
            modelStatus.put(modelName, false);
            
            // Mark initialization as failed if this is a critical model
            if (modelName.contains("dqn") || modelName.contains("ppo")) {
                initializationFailed = true;
            }
            
            return false;
        }
    }
    
    /**
     * Load model file from assets
     */
    private MappedByteBuffer loadModelFile(String assetPath) {
        if (applicationContext == null) {
            Log.e(TAG, "Application context not initialized");
            return null;
        }
        
        try {
            AssetManager assetManager = applicationContext.getAssets();
            FileInputStream fileInputStream = new FileInputStream(assetManager.openFd(assetPath).getFileDescriptor());
            FileChannel fileChannel = fileInputStream.getChannel();
            long startOffset = assetManager.openFd(assetPath).getStartOffset();
            long declaredLength = assetManager.openFd(assetPath).getDeclaredLength();
            
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            
        } catch (IOException e) {
            Log.w(TAG, "Could not load model file: " + assetPath + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Verify model works correctly
     */
    private boolean verifyModel(Interpreter interpreter) {
        try {
            // Get input/output tensor info
            int[] inputShape = interpreter.getInputTensor(0).shape();
            int[] outputShape = interpreter.getOutputTensor(0).shape();
            
            Log.d(TAG, "Model input shape: " + java.util.Arrays.toString(inputShape));
            Log.d(TAG, "Model output shape: " + java.util.Arrays.toString(outputShape));
            
            // Create test input
            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * getShapeSize(inputShape));
            inputBuffer.order(ByteOrder.nativeOrder());
            
            // Fill with dummy data
            for (int i = 0; i < getShapeSize(inputShape); i++) {
                inputBuffer.putFloat(0.1f);
            }
            
            // Create output buffer
            ByteBuffer outputBuffer = ByteBuffer.allocateDirect(4 * getShapeSize(outputShape));
            outputBuffer.order(ByteOrder.nativeOrder());
            
            // Run test inference
            interpreter.run(inputBuffer, outputBuffer);
            
            Log.d(TAG, "Model verification successful");
            return true;
            
        } catch (Exception e) {
            Log.w(TAG, "Model verification failed", e);
            return false;
        }
    }
    
    /**
     * Calculate total size of tensor shape
     */
    private int getShapeSize(int[] shape) {
        int size = 1;
        for (int dim : shape) {
            size *= dim;
        }
        return size;
    }
    
    /**
     * Run inference on loaded model
     */
    public float[] runInference(String modelName, float[] inputData) {
        Interpreter interpreter = loadedModels.get(modelName);
        if (interpreter == null || !modelStatus.getOrDefault(modelName, false)) {
            Log.w(TAG, "Model not available: " + modelName);
            return null;
        }
        
        try {
            // Prepare input buffer
            int[] inputShape = interpreter.getInputTensor(0).shape();
            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * inputData.length);
            inputBuffer.order(ByteOrder.nativeOrder());
            
            for (float value : inputData) {
                inputBuffer.putFloat(value);
            }
            
            // Prepare output buffer
            int[] outputShape = interpreter.getOutputTensor(0).shape();
            ByteBuffer outputBuffer = ByteBuffer.allocateDirect(4 * getShapeSize(outputShape));
            outputBuffer.order(ByteOrder.nativeOrder());
            
            // Run inference
            interpreter.run(inputBuffer, outputBuffer);
            
            // Extract results
            outputBuffer.rewind();
            float[] results = new float[getShapeSize(outputShape)];
            for (int i = 0; i < results.length; i++) {
                results[i] = outputBuffer.getFloat();
            }
            
            return results;
            
        } catch (Exception e) {
            Log.e(TAG, "Inference failed for model: " + modelName, e);
            return null;
        }
    }
    
    /**
     * Check if model is loaded and ready
     */
    public boolean isModelReady(String modelName) {
        return loadedModels.containsKey(modelName) && modelStatus.getOrDefault(modelName, false);
    }
    
    /**
     * Unload specific model
     */
    public void unloadModel(String modelName) {
        Interpreter interpreter = loadedModels.remove(modelName);
        if (interpreter != null) {
            try {
                interpreter.close();
                modelStatus.put(modelName, false);
                Log.d(TAG, "Unloaded model: " + modelName);
            } catch (Exception e) {
                Log.w(TAG, "Error unloading model: " + modelName, e);
            }
        }
    }
    
    /**
     * Cleanup all loaded models
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up TensorFlow Lite models");
        
        for (Interpreter interpreter : loadedModels.values()) {
            try {
                interpreter.close();
            } catch (Exception e) {
                Log.w(TAG, "Error closing interpreter", e);
            }
        }
        
        loadedModels.clear();
        modelStatus.clear();
        initializationFailed = false;
    }
    
    /**
     * Get status summary
     */
    public String getStatusSummary() {
        StringBuilder status = new StringBuilder();
        status.append("TensorFlow Lite Manager Status:\n");
        status.append("Initialization Failed: ").append(initializationFailed).append("\n");
        status.append("Loaded Models: ").append(loadedModels.size()).append("\n");
        
        for (String modelName : loadedModels.keySet()) {
            boolean ready = modelStatus.getOrDefault(modelName, false);
            status.append("  ").append(modelName).append(": ").append(ready ? "Ready" : "Error").append("\n");
        }
        
        return status.toString();
    }
}
package com.gestureai.gameautomation.managers;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.gestureai.gameautomation.models.GestureResult;
import com.gestureai.gameautomation.models.HandLandmarks;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MLModelManager {
    private static final String TAG = "MLModelManager";
    private static final String HAND_LANDMARK_MODEL_FILE = "hand_landmark_full.tflite";
    
    private final Context context;
    private Interpreter gestureInterpreter;
    private boolean isInitialized = false;
    private Map<String, Interpreter> customModels = new HashMap<>();
    private Map<String, String> availableModels = new HashMap<>();
    private Set<String> selectedModels = new HashSet<>();
    
    // Gesture types supported by the model
    private static final String[] GESTURE_TYPES = {
        "swipeUp", "swipeDown", "swipeLeft", "swipeRight",
        "tap", "doubleTap", "longPress", "pinch", "spread",
        "fist", "openHand", "thumbsUp", "peace", "none"
    };

    public MLModelManager(Context context) {
        this.context = context;
        scanAvailableModels(); // Scan files
        loadSelectedModels();  // Load all by default
    }
    
    public void initialize() throws IOException {
        try {
            // Check network connectivity for online model downloads
            if (isNetworkAvailable()) {
                Log.d(TAG, "Network available - attempting online model initialization");
            } else {
                Log.w(TAG, "No network - using offline models only");
            }
            
            // Load TensorFlow Lite model with fallback
            MappedByteBuffer modelBuffer = loadModelFileWithFallback();
            if (modelBuffer != null) {
                gestureInterpreter = new Interpreter(modelBuffer);
            } else {
                Log.e(TAG, "No gesture model available - initialization failed");
                throw new IOException("Gesture model not available");
            }
            
            isInitialized = true;
            Log.d(TAG, "ML Model Manager initialized successfully");
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize ML Model Manager", e);
            throw e;
        }
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }
    
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(HAND_LANDMARK_MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    
    public GestureResult classifyGesture(HandLandmarks landmarks) {
        if (!isInitialized) {
            Log.w(TAG, "ML Model not initialized");
            return new GestureResult("none", 0.0f, landmarks.getCenterPoint());
        }
        
        if (landmarks == null || landmarks.getLandmarks().isEmpty()) {
            return new GestureResult("none", 0.0f, new android.graphics.Point(0, 0));
        }
        
        try {
            // Prepare input tensor from landmarks
            float[][] input = prepareLandmarksInput(landmarks);
            
            // Run inference
            float[][] output = new float[1][GESTURE_TYPES.length];
            gestureInterpreter.run(input, output);
            
            // Find the gesture with highest confidence
            int maxIndex = 0;
            float maxConfidence = output[0][0];
            
            for (int i = 1; i < output[0].length; i++) {
                if (output[0][i] > maxConfidence) {
                    maxConfidence = output[0][i];
                    maxIndex = i;
                }
            }
            
            String gestureType = GESTURE_TYPES[maxIndex];
            
            Log.d(TAG, String.format("Classified gesture: %s (%.2f confidence)", 
                                   gestureType, maxConfidence));
            
            return new GestureResult(gestureType, maxConfidence, landmarks.getCenterPoint());
            
        } catch (Exception e) {
            Log.e(TAG, "Error during gesture classification", e);
            return new GestureResult("none", 0.0f, landmarks.getCenterPoint());
        }
    }
    
    private float[][] prepareLandmarksInput(HandLandmarks landmarks) {
        // Convert hand landmarks to model input format
        // Assuming model expects flattened 21x3 landmarks (x, y, z)
        float[][] input = new float[1][63]; // 21 landmarks * 3 coordinates
        
        int index = 0;
        for (HandLandmarks.Landmark landmark : landmarks.getLandmarks()) {
            if (index < 21) { // Ensure we don't exceed expected landmark count
                input[0][index * 3] = landmark.x;
                input[0][index * 3 + 1] = landmark.y;
                input[0][index * 3 + 2] = landmark.z;
                index++;
            }
        }
        
        // Normalize coordinates
        normalizeInput(input[0]);
        
        return input;
    }
    
    private void normalizeInput(float[] input) {
        // Simple normalization - could be improved with proper scaling
        for (int i = 0; i < input.length; i++) {
            input[i] = input[i] / 640.0f; // Assuming 640px reference size
        }
    }
    
    public void cleanup() {
        if (gestureInterpreter != null) {
            gestureInterpreter.close();
            gestureInterpreter = null;
        }
        isInitialized = false;
        Log.d(TAG, "ML Model Manager cleaned up");
    }
    
    // Support for multiple game-specific classifiers
    public GestureResult classifyForGame(HandLandmarks landmarks, String gamePackage) {
        // Basic implementation - could be extended for game-specific models
        GestureResult result = classifyGesture(landmarks);
        
        // Apply game-specific adjustments
        if (gamePackage != null) {
            result = adjustForGameContext(result, gamePackage);
        }
        
        return result;
    }
    
    private GestureResult adjustForGameContext(GestureResult result, String gamePackage) {
        // Game-specific gesture mapping and confidence adjustments
        Map<String, Float> confidenceBoosts = getGameSpecificBoosts(gamePackage);
        
        Float boost = confidenceBoosts.get(result.getType());
        if (boost != null) {
            float adjustedConfidence = Math.min(1.0f, result.getConfidence() * boost);
            return new GestureResult(result.getType(), adjustedConfidence, result.getCenterPoint());
        }
        
        return result;
    }
    
    private Map<String, Float> getGameSpecificBoosts(String gamePackage) {
        Map<String, Float> boosts = new HashMap<>();
        
        if (gamePackage.contains("subway") || gamePackage.contains("runner")) {
            // Boost swipe gestures for endless runner games
            boosts.put("swipeUp", 1.2f);
            boosts.put("swipeDown", 1.2f);
            boosts.put("swipeLeft", 1.1f);
            boosts.put("swipeRight", 1.1f);
        } else if (gamePackage.contains("puzzle")) {
            // Boost tap gestures for puzzle games
            boosts.put("tap", 1.3f);
            boosts.put("doubleTap", 1.2f);
        }
        
        return boosts;
    }
    public String exportModel() {
        // Export current trained model to external storage
        String exportPath = context.getExternalFilesDir("exports") + "/trained_model.tflite";
        // Implementation for saving model
        return exportPath;
    }
    // In MLModelManager, add this method:
    // Replace scanForCustomModels() with this:
    public Map<String, String> scanAvailableModels() {
        File modelsDir = new File(context.getExternalFilesDir(null), "models");
        if (!modelsDir.exists()) modelsDir.mkdirs();

        File[] tfliteFiles = modelsDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".tflite"));

        availableModels.clear();
        if (tfliteFiles != null) {
            for (File model : tfliteFiles) {
                availableModels.put(model.getName(), model.getAbsolutePath());
                selectedModels.add(model.getName()); // All selected by default
            }
        }
        Log.d(TAG, "Found " + availableModels.size() + " .tflite models");
        return availableModels;
    }

    public void loadSelectedModels() {
        for (String modelName : selectedModels) {
            if (!customModels.containsKey(modelName)) {
                loadCustomModel(modelName, availableModels.get(modelName));
            }
        }
    }
    private void loadCustomModel(String modelName, String modelPath) {
        try {
            Interpreter customInterpreter = new Interpreter(new File(modelPath));
            customModels.put(modelName, customInterpreter);
            Log.d(TAG, "Loaded custom model: " + modelName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load " + modelName, e);
        }
    }
    public void setSelectedModels(Set<String> selected) {
        selectedModels = selected;
        // Unload deselected models
        customModels.entrySet().removeIf(entry -> {
            if (!selected.contains(entry.getKey())) {
                entry.getValue().close();
                return true;
            }
            return false;
        });
    }

}
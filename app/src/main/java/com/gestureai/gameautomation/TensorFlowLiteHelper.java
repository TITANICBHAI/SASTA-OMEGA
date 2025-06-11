package com.gestureai.gameautomation;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.gpu.GpuDelegateFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TensorFlow Lite model management and inference helper
 */
public class TensorFlowLiteHelper {
    private static final String TAG = "TensorFlowLiteHelper";

    private Context context;
    private Map<String, Interpreter> loadedModels;
    private Map<String, ModelConfig> modelConfigs;
    private GpuDelegate gpuDelegate;
    private boolean useGPU = true;

    public static class ModelConfig {
        public String modelPath;
        public int inputWidth;
        public int inputHeight;
        public int inputChannels;
        public int outputSize;
        public String[] outputLabels;
        public float confidenceThreshold;

        public ModelConfig(String path, int width, int height, int channels,
                           int outputSize, String[] labels, float threshold) {
            this.modelPath = path;
            this.inputWidth = width;
            this.inputHeight = height;
            this.inputChannels = channels;
            this.outputSize = outputSize;
            this.outputLabels = labels;
            this.confidenceThreshold = threshold;
        }
    }

    public static class DetectionResult {
        public String className;
        public float confidence;
        public float[] boundingBox; // [x, y, width, height] normalized 0-1
        public int classIndex;

        public DetectionResult(String className, float confidence, float[] boundingBox, int classIndex) {
            this.className = className;
            this.confidence = confidence;
            this.boundingBox = boundingBox;
            this.classIndex = classIndex;
        }
    }

    public TensorFlowLiteHelper(Context context) {
        this.context = context;
        this.loadedModels = new HashMap<>();
        this.modelConfigs = new HashMap<>();

        if (useGPU) {
            initializeGPUDelegate();
        }

        // Register default game-specific models
        registerDefaultModels();

        Log.d(TAG, "TensorFlow Lite Helper initialized");
    }

    private void initializeGPUDelegate() {
        try {
            // Check GPU compatibility first
            CompatibilityList compatList = new CompatibilityList();

            if (compatList.isDelegateSupportedOnThisDevice()) {
                // Use GpuDelegateFactory.Options for TF Lite 2.13.0+
                GpuDelegateFactory.Options delegateOptions = compatList.getBestOptionsForThisDevice();
                gpuDelegate = new GpuDelegate(delegateOptions);
                Log.d(TAG, "GPU delegate initialized for TensorFlow Lite");
            } else {
                Log.w(TAG, "GPU delegate not supported on this device, using CPU");
                useGPU = false;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to initialize GPU delegate, using CPU", e);
            useGPU = false;
        }
    }
    private void registerDefaultModels() {
        // Battle Royale Player Detection Model
        registerModel("player_detector", new ModelConfig(
                "models/player_detection.tflite",
                416, 416, 3, // YOLOv5 input format
                25200, // 3 scales × 3 anchors × (5 + 20 classes) = detection outputs
                new String[]{"player", "enemy", "teammate", "loot", "vehicle", "building"},
                0.5f
        ));

        // Weapon Classification Model
        registerModel("weapon_classifier", new ModelConfig(
                "models/weapon_classification.tflite",
                224, 224, 3, // MobileNet input format
                10, // Number of weapon classes
                new String[]{"rifle", "shotgun", "pistol", "sniper", "smg", "lmg", "melee", "grenade", "shield", "medkit"},
                0.7f
        ));

        // Game UI Element Detection
        registerModel("ui_detector", new ModelConfig(
                "models/ui_detection.tflite",
                320, 320, 3,
                15, // UI element classes
                new String[]{"health_bar", "ammo_counter", "minimap", "crosshair", "button", "menu", "score", "timer"},
                0.6f
        ));

        // Minimap Analysis Model
        registerModel("minimap_analyzer", new ModelConfig(
                "models/minimap_analysis.tflite",
                128, 128, 3,
                8, // Minimap features
                new String[]{"safe_zone", "storm", "enemy_marker", "teammate_marker", "loot", "vehicle", "building", "objective"},
                0.4f
        ));

        // Game State Classification
        registerModel("game_state_classifier", new ModelConfig(
                "models/game_state.tflite",
                299, 299, 3, // Inception input format
                6, // Game states
                new String[]{"menu", "lobby", "playing", "inventory", "map", "game_over"},
                0.8f
        ));
    }

    public void registerModel(String modelName, ModelConfig config) {
        modelConfigs.put(modelName, config);
        Log.d(TAG, "Registered model: " + modelName);
    }

    public boolean loadModel(String modelName) {
        if (loadedModels.containsKey(modelName)) {
            Log.d(TAG, "Model already loaded: " + modelName);
            return true;
        }

        ModelConfig config = modelConfigs.get(modelName);
        if (config == null) {
            Log.e(TAG, "Model config not found: " + modelName);
            return false;
        }

        try {
            MappedByteBuffer modelBuffer = loadModelFile(config.modelPath);

            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4); // Use 4 CPU threads

            if (useGPU && gpuDelegate != null) {
                options.addDelegate(gpuDelegate);
            }

            Interpreter interpreter = new Interpreter(modelBuffer, options);
            loadedModels.put(modelName, interpreter);

            Log.d(TAG, "Successfully loaded model: " + modelName);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to load model: " + modelName, e);
            return false;
        }
    }

    private MappedByteBuffer loadModelFile(String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public List<DetectionResult> runInference(String modelName, Bitmap inputBitmap) {
        List<DetectionResult> results = new ArrayList<>();

        if (!loadedModels.containsKey(modelName)) {
            if (!loadModel(modelName)) {
                Log.e(TAG, "Cannot run inference - model not loaded: " + modelName);
                return results;
            }
        }

        Interpreter interpreter = loadedModels.get(modelName);
        ModelConfig config = modelConfigs.get(modelName);

        try {
            // Prepare input
            ByteBuffer inputBuffer = preprocessImage(inputBitmap, config);

            // Prepare output based on model type
            Object output = prepareOutput(config);

            // Run inference
            long startTime = System.currentTimeMillis();
            interpreter.run(inputBuffer, output);
            long inferenceTime = System.currentTimeMillis() - startTime;

            // Process output based on model type
            results = processOutput(output, config, inferenceTime);

            Log.d(TAG, "Inference completed for " + modelName + " in " + inferenceTime + "ms");

        } catch (Exception e) {
            Log.e(TAG, "Inference failed for model: " + modelName, e);
        }

        return results;
    }

    private ByteBuffer preprocessImage(Bitmap bitmap, ModelConfig config) {
        // Resize bitmap to model input size
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap,
                config.inputWidth, config.inputHeight, true);

        // Create ByteBuffer for model input
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(
                config.inputWidth * config.inputHeight * config.inputChannels * 4); // 4 bytes per float
        inputBuffer.order(ByteOrder.nativeOrder());

        // Convert bitmap to normalized float values
        int[] pixels = new int[config.inputWidth * config.inputHeight];
        resizedBitmap.getPixels(pixels, 0, config.inputWidth, 0, 0,
                config.inputWidth, config.inputHeight);

        for (int pixel : pixels) {
            // Extract RGB values and normalize to [0, 1]
            inputBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f); // Red
            inputBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);  // Green
            inputBuffer.putFloat((pixel & 0xFF) / 255.0f);         // Blue
        }

        return inputBuffer;
    }

    private Object prepareOutput(ModelConfig config) {
        if (config.modelPath.contains("detection")) {
            // Object detection models output [batch, detections, 6] (x, y, w, h, conf, class)
            return new float[1][config.outputSize][6];
        } else {
            // Classification models output [batch, classes]
            return new float[1][config.outputSize];
        }
    }

    private List<DetectionResult> processOutput(Object output, ModelConfig config, long inferenceTime) {
        List<DetectionResult> results = new ArrayList<>();

        if (output instanceof float[][][]) {
            // Object detection output
            float[][][] detectionOutput = (float[][][]) output;

            for (int i = 0; i < detectionOutput[0].length; i++) {
                float confidence = detectionOutput[0][i][4];

                if (confidence > config.confidenceThreshold) {
                    float x = detectionOutput[0][i][0];
                    float y = detectionOutput[0][i][1];
                    float w = detectionOutput[0][i][2];
                    float h = detectionOutput[0][i][3];
                    int classIndex = (int) detectionOutput[0][i][5];

                    if (classIndex < config.outputLabels.length) {
                        String className = config.outputLabels[classIndex];
                        float[] boundingBox = {x, y, w, h};

                        results.add(new DetectionResult(className, confidence, boundingBox, classIndex));
                    }
                }
            }

        } else if (output instanceof float[][]) {
            // Classification output
            float[][] classificationOutput = (float[][]) output;

            // Find top predictions
            for (int i = 0; i < classificationOutput[0].length; i++) {
                float confidence = classificationOutput[0][i];

                if (confidence > config.confidenceThreshold) {
                    String className = i < config.outputLabels.length ?
                            config.outputLabels[i] : "unknown_" + i;

                    results.add(new DetectionResult(className, confidence, new float[]{0, 0, 1, 1}, i));
                }
            }
        }

        return results;
    }

    public boolean isModelLoaded(String modelName) {
        return loadedModels.containsKey(modelName);
    }

    public void unloadModel(String modelName) {
        Interpreter interpreter = loadedModels.remove(modelName);
        if (interpreter != null) {
            interpreter.close();
            Log.d(TAG, "Unloaded model: " + modelName);
        }
    }

    public void unloadAllModels() {
        for (Map.Entry<String, Interpreter> entry : loadedModels.entrySet()) {
            entry.getValue().close();
        }
        loadedModels.clear();

        if (gpuDelegate != null) {
            gpuDelegate.close();
        }

        Log.d(TAG, "All models unloaded");
    }

    public List<String> getLoadedModels() {
        return new ArrayList<>(loadedModels.keySet());
    }

    public List<String> getAvailableModels() {
        return new ArrayList<>(modelConfigs.keySet());
    }

    public ModelConfig getModelConfig(String modelName) {
        return modelConfigs.get(modelName);
    }

    /**
     * Batch inference for multiple images
     */
    public Map<String, List<DetectionResult>> runBatchInference(String modelName, List<Bitmap> images) {
        Map<String, List<DetectionResult>> batchResults = new HashMap<>();

        for (int i = 0; i < images.size(); i++) {
            List<DetectionResult> results = runInference(modelName, images.get(i));
            batchResults.put("image_" + i, results);
        }

        return batchResults;
    }

    /**
     * Performance benchmarking
     */
    public long benchmarkModel(String modelName, Bitmap testImage, int iterations) {
        if (!loadModel(modelName)) {
            return -1;
        }

        long totalTime = 0;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.currentTimeMillis();
            runInference(modelName, testImage);
            totalTime += System.currentTimeMillis() - startTime;
        }

        long averageTime = totalTime / iterations;
        Log.d(TAG, "Benchmark " + modelName + ": " + averageTime + "ms average over " + iterations + " iterations");

        return averageTime;
    }
}
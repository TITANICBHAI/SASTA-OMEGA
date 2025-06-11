package com.gestureai.gameautomation.utils;

import android.content.Context;
import android.util.Log;

import com.gestureai.gameautomation.ai.GameStrategyAgent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Utility class for managing neural network models
 */
public class ModelManager {
    private static final String TAG = "ModelManager";
    private static final String MODELS_DIR = "models";

    public static void saveModel(Context context, GameStrategyAgent agent) throws Exception {
        File modelsDir = new File(context.getFilesDir(), MODELS_DIR);
        if (!modelsDir.exists()) {
            modelsDir.mkdirs();
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        File modelFile = new File(modelsDir, "strategy_agent_" + timestamp + ".model");

        // In a real implementation, this would serialize the DL4J model
        // For now, we'll save basic configuration
        ModelData modelData = new ModelData();
        modelData.timestamp = System.currentTimeMillis();
        modelData.modelType = "GameStrategyAgent";
        modelData.version = "1.0";

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(modelFile))) {
            oos.writeObject(modelData);
        }

        Log.d(TAG, "Model saved to: " + modelFile.getAbsolutePath());
    }

    public static void loadModel(Context context, GameStrategyAgent agent) throws Exception {
        File modelsDir = new File(context.getFilesDir(), MODELS_DIR);
        if (!modelsDir.exists()) {
            throw new Exception("No models directory found");
        }

        // Find the most recent model file
        File[] modelFiles = modelsDir.listFiles((dir, name) -> name.endsWith(".model"));
        if (modelFiles == null || modelFiles.length == 0) {
            throw new Exception("No model files found");
        }

        File latestModel = modelFiles[0];
        for (File file : modelFiles) {
            if (file.lastModified() > latestModel.lastModified()) {
                latestModel = file;
            }
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(latestModel))) {
            ModelData modelData = (ModelData) ois.readObject();
            Log.d(TAG, "Loaded model: " + modelData.modelType + " v" + modelData.version);
        }

        Log.d(TAG, "Model loaded from: " + latestModel.getAbsolutePath());
    }

    public static boolean hasExistingModel(Context context) {
        File modelsDir = new File(context.getFilesDir(), MODELS_DIR);
        if (!modelsDir.exists()) {
            return false;
        }

        File[] modelFiles = modelsDir.listFiles((dir, name) -> name.endsWith(".model"));
        return modelFiles != null && modelFiles.length > 0;
    }

    public static void deleteAllModels(Context context) {
        File modelsDir = new File(context.getFilesDir(), MODELS_DIR);
        if (!modelsDir.exists()) {
            return;
        }

        File[] modelFiles = modelsDir.listFiles();
        if (modelFiles != null) {
            for (File file : modelFiles) {
                if (file.delete()) {
                    Log.d(TAG, "Deleted model: " + file.getName());
                }
            }
        }
    }

    private static class ModelData implements java.io.Serializable {
        long timestamp;
        String modelType;
        String version;
    }
}
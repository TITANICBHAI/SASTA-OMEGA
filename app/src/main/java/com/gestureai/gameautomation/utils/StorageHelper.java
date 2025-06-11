package com.gestureai.gameautomation.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.gestureai.gameautomation.GameObjectTemplate;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Utility class for managing storage of training data and models
 */
public class StorageHelper {
    private static final String TAG = "StorageHelper";
    private static final String LABELED_OBJECTS_DIR = "labeled_objects";
    private static final String DATASETS_DIR = "datasets";
    private static final String MODELS_DIR = "models";

    public static void saveLabeledObject(Context context, GameObjectTemplate template) {
        try {
            File dir = new File(context.getFilesDir(), LABELED_OBJECTS_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Save metadata as JSON
            JSONObject json = new JSONObject();
            json.put("name", template.getName());
            json.put("timestamp", template.getTimestamp());
            json.put("boundingBox", boundingBoxToJson(template.getBoundingBox()));

            String filename = "object_" + template.getTimestamp() + ".json";
            File file = new File(dir, filename);
            
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json.toString());
            }

            // Save screenshot if available
            if (template.getScreenshot() != null) {
                saveScreenshot(context, template.getScreenshot(), template.getTimestamp());
            }

            Log.d(TAG, "Saved labeled object: " + template.getName());
        } catch (Exception e) {
            Log.e(TAG, "Error saving labeled object", e);
        }
    }

    public static List<GameObjectTemplate> loadLabeledObjects(Context context) {
        List<GameObjectTemplate> objects = new ArrayList<>();
        
        try {
            File dir = new File(context.getFilesDir(), LABELED_OBJECTS_DIR);
            if (!dir.exists()) {
                return objects;
            }

            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    GameObjectTemplate template = loadLabeledObject(file);
                    if (template != null) {
                        objects.add(template);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading labeled objects", e);
        }

        return objects;
    }

    private static GameObjectTemplate loadLabeledObject(File file) {
        try {
            StringBuilder content = new StringBuilder();
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    content.append(scanner.nextLine());
                }
            }

            JSONObject json = new JSONObject(content.toString());
            GameObjectTemplate template = new GameObjectTemplate();
            template.setName(json.getString("name"));
            template.setTimestamp(json.getLong("timestamp"));
            
            if (json.has("boundingBox")) {
                template.setBoundingBox(boundingBoxFromJson(json.getJSONObject("boundingBox")));
            }

            return template;
        } catch (Exception e) {
            Log.e(TAG, "Error loading labeled object from file: " + file.getName(), e);
            return null;
        }
    }

    public static void exportDataset(Context context, List<GameObjectTemplate> objects) throws IOException {
        File exportDir = new File(context.getExternalFilesDir(null), DATASETS_DIR);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        String filename = "dataset_export_" + System.currentTimeMillis() + ".json";
        File exportFile = new File(exportDir, filename);

        JSONArray datasetArray = new JSONArray();
        for (GameObjectTemplate template : objects) {
            JSONObject objJson = new JSONObject();
            try {
                objJson.put("name", template.getName());
                objJson.put("timestamp", template.getTimestamp());
                if (template.getBoundingBox() != null) {
                    objJson.put("boundingBox", boundingBoxToJson(template.getBoundingBox()));
                }
                datasetArray.put(objJson);
            } catch (Exception e) {
                Log.e(TAG, "Error adding object to dataset export", e);
            }
        }

        try (FileWriter writer = new FileWriter(exportFile)) {
            writer.write(datasetArray.toString(2));
        }

        Log.d(TAG, "Dataset exported to: " + exportFile.getAbsolutePath());
    }

    public static List<GameObjectTemplate> importDataset(Context context) throws IOException {
        // Placeholder implementation - in real app, would show file picker
        File importDir = new File(context.getExternalFilesDir(null), DATASETS_DIR);
        if (!importDir.exists()) {
            return new ArrayList<>();
        }

        File[] files = importDir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }

        // Import the most recent file
        File latestFile = files[0];
        for (File file : files) {
            if (file.lastModified() > latestFile.lastModified()) {
                latestFile = file;
            }
        }

        return importDatasetFromFile(latestFile);
    }

    private static List<GameObjectTemplate> importDatasetFromFile(File file) {
        List<GameObjectTemplate> objects = new ArrayList<>();
        
        try {
            StringBuilder content = new StringBuilder();
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    content.append(scanner.nextLine());
                }
            }

            JSONArray datasetArray = new JSONArray(content.toString());
            for (int i = 0; i < datasetArray.length(); i++) {
                JSONObject objJson = datasetArray.getJSONObject(i);
                
                GameObjectTemplate template = new GameObjectTemplate();
                template.setName(objJson.getString("name"));
                template.setTimestamp(objJson.getLong("timestamp"));
                
                if (objJson.has("boundingBox")) {
                    template.setBoundingBox(boundingBoxFromJson(objJson.getJSONObject("boundingBox")));
                }
                
                objects.add(template);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error importing dataset from file: " + file.getName(), e);
        }

        return objects;
    }

    private static void saveScreenshot(Context context, Bitmap screenshot, long timestamp) {
        try {
            File dir = new File(context.getFilesDir(), "screenshots");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filename = "screenshot_" + timestamp + ".png";
            File file = new File(dir, filename);

            try (FileOutputStream out = new FileOutputStream(file)) {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving screenshot", e);
        }
    }

    private static JSONObject boundingBoxToJson(android.graphics.RectF boundingBox) throws Exception {
        if (boundingBox == null) return null;
        
        JSONObject json = new JSONObject();
        json.put("left", boundingBox.left);
        json.put("top", boundingBox.top);
        json.put("right", boundingBox.right);
        json.put("bottom", boundingBox.bottom);
        return json;
    }

    private static android.graphics.RectF boundingBoxFromJson(JSONObject json) throws Exception {
        if (json == null) return null;
        
        float left = (float) json.getDouble("left");
        float top = (float) json.getDouble("top");
        float right = (float) json.getDouble("right");
        float bottom = (float) json.getDouble("bottom");
        
        return new android.graphics.RectF(left, top, right, bottom);
    }

    public static void saveModel(Context context, String modelName, byte[] modelData) {
        try {
            File dir = new File(context.getFilesDir(), MODELS_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File modelFile = new File(dir, modelName + ".model");
            try (FileOutputStream out = new FileOutputStream(modelFile)) {
                out.write(modelData);
            }

            Log.d(TAG, "Model saved: " + modelName);
        } catch (Exception e) {
            Log.e(TAG, "Error saving model: " + modelName, e);
        }
    }

    public static byte[] loadModel(Context context, String modelName) {
        try {
            File modelFile = new File(new File(context.getFilesDir(), MODELS_DIR), modelName + ".model");
            if (!modelFile.exists()) {
                return null;
            }

            try (FileInputStream in = new FileInputStream(modelFile)) {
                byte[] data = new byte[(int) modelFile.length()];
                in.read(data);
                return data;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading model: " + modelName, e);
            return null;
        }
    }

    public static void clearCache(Context context) {
        try {
            File cacheDir = context.getCacheDir();
            deleteDirectory(cacheDir);
            Log.d(TAG, "Cache cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing cache", e);
        }
    }

    private static void deleteDirectory(File dir) {
        if (dir != null && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
    }
}
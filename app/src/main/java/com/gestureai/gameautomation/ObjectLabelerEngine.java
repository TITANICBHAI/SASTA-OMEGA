package com.gestureai.gameautomation;

import ndroid.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.gestureai.gameautomation.fragments.GestureLabelerFragment.LabeledObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Engine for object labeling and training dataset creation
 * Connects UI components to backend ML training systems
 */
public class ObjectLabelerEngine {
    private static final String TAG = "ObjectLabelerEngine";
    
    private Context context;
    private ExecutorService executorService;
    private Handler mainHandler;
    private List<LabeledObject> trainingDataset;
    private GameAutomationEngine automationEngine;
    
    // Callbacks for UI communication
    public interface CaptureCallback {
        void onCaptureComplete(Bitmap screenshot);
        void onCaptureError(String error);
    }
    
    public interface SaveCallback {
        void onSaveComplete(int savedCount);
        void onSaveError(String error);
    }
    
    public interface ExportCallback {
        void onExportComplete(String filePath);
        void onExportError(String error);
    }
    
    public interface ImportCallback {
        void onImportComplete(List<LabeledObject> importedObjects);
        void onImportError(String error);
    }
    
    public ObjectLabelerEngine(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.trainingDataset = new ArrayList<>();
        this.automationEngine = new GameAutomationEngine(context);
        
        Log.d(TAG, "ObjectLabelerEngine initialized");
    }
    
    /**
     * Capture screen for object labeling
     */
    public void captureScreenForLabeling(CaptureCallback callback) {
        executorService.execute(() -> {
            try {
                // Connect to GameAutomationEngine for screen capture
                Bitmap screenshot = automationEngine.captureScreen();
                
                if (screenshot != null) {
                    mainHandler.post(() -> callback.onCaptureComplete(screenshot));
                } else {
                    mainHandler.post(() -> callback.onCaptureError("Failed to capture screen"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Screen capture error", e);
                mainHandler.post(() -> callback.onCaptureError(e.getMessage()));
            }
        });
    }
    
    /**
     * Save labeled objects to training dataset
     */
    public void saveLabeledObjects(List<LabeledObject> labeledObjects, SaveCallback callback) {
        executorService.execute(() -> {
            try {
                // Add objects to training dataset
                trainingDataset.addAll(labeledObjects);
                
                // Save to persistent storage
                saveToInternalStorage(labeledObjects);
                
                // Connect to ML training pipeline
                trainObjectDetectionModel(labeledObjects);
                
                mainHandler.post(() -> callback.onSaveComplete(labeledObjects.size()));
                
            } catch (Exception e) {
                Log.e(TAG, "Save error", e);
                mainHandler.post(() -> callback.onSaveError(e.getMessage()));
            }
        });
    }
    
    /**
     * Export training dataset for external use
     */
    public void exportTrainingDataset(ExportCallback callback) {
        executorService.execute(() -> {
            try {
                File exportFile = new File(context.getExternalFilesDir(null), "training_dataset.json");
                
                // Convert dataset to JSON format
                String jsonData = convertDatasetToJson(trainingDataset);
                
                // Write to file
                FileOutputStream fos = new FileOutputStream(exportFile);
                fos.write(jsonData.getBytes());
                fos.close();
                
                mainHandler.post(() -> callback.onExportComplete(exportFile.getAbsolutePath()));
                
            } catch (IOException e) {
                Log.e(TAG, "Export error", e);
                mainHandler.post(() -> callback.onExportError(e.getMessage()));
            }
        });
    }
    
    /**
     * CRITICAL: Complete backend integration with AI training pipeline
     */
    private void trainObjectDetectionModel(List<LabeledObject> newObjects) {
        try {
            // 1. Connect to DQN Agent for training
            GameAutomationEngine engine = GameAutomationEngine.getInstance();
            if (engine != null) {
                com.gestureai.gameautomation.ai.DQNAgent dqnAgent = engine.getDQNAgent();
                com.gestureai.gameautomation.ai.PPOAgent ppoAgent = engine.getPPOAgent();
                
                // 2. Convert labeled objects to training data
                for (LabeledObject obj : newObjects) {
                    float[] stateVector = convertObjectToStateVector(obj);
                    int actionLabel = mapObjectToAction(obj.getActionType());
                    
                    // 3. Train DQN with new object data
                    if (dqnAgent != null) {
                        dqnAgent.trainFromCustomData(stateVector, actionLabel, 1.0f);
                    }
                    
                    // 4. Train PPO with new object data
                    if (ppoAgent != null) {
                        ppoAgent.trainFromCustomData(stateVector, actionLabel, 1.0f);
                    }
                }
                
                // 5. Update TensorFlow Lite models
                updateTensorFlowModels(newObjects);
                
                // 6. Update OpenCV templates
                updateOpenCVTemplates(newObjects);
                
                Log.d(TAG, "Successfully integrated " + newObjects.size() + " objects into AI training pipeline");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in AI training pipeline integration", e);
        }
    }
    
    /**
     * Convert labeled object to neural network state vector
     */
    private float[] convertObjectToStateVector(LabeledObject obj) {
        float[] stateVector = new float[128]; // Match DQN/PPO input size
        
        // Extract features from object
        Rect bounds = obj.getBoundingBox();
        
        // Position features (normalized 0-1)
        stateVector[0] = bounds.left / 1080.0f;   // x position
        stateVector[1] = bounds.top / 1920.0f;    // y position
        stateVector[2] = bounds.width() / 1080.0f; // width
        stateVector[3] = bounds.height() / 1920.0f; // height
        
        // Object type features (one-hot encoding)
        String objectType = obj.getLabel();
        if ("coin".equals(objectType)) stateVector[4] = 1.0f;
        else if ("obstacle".equals(objectType)) stateVector[5] = 1.0f;
        else if ("powerup".equals(objectType)) stateVector[6] = 1.0f;
        else if ("enemy".equals(objectType)) stateVector[7] = 1.0f;
        else if ("collectible".equals(objectType)) stateVector[8] = 1.0f;
        
        // Action type features
        String actionType = obj.getActionType();
        if ("tap".equals(actionType)) stateVector[9] = 1.0f;
        else if ("swipe_left".equals(actionType)) stateVector[10] = 1.0f;
        else if ("swipe_right".equals(actionType)) stateVector[11] = 1.0f;
        else if ("avoid".equals(actionType)) stateVector[12] = 1.0f;
        
        // Confidence score
        stateVector[13] = obj.getConfidence();
        
        // Fill remaining with context features
        for (int i = 14; i < 128; i++) {
            stateVector[i] = 0.0f; // Initialize to zero
        }
        
        return stateVector;
    }
    
    /**
     * Map object action type to neural network action index
     */
    private int mapObjectToAction(String actionType) {
        switch (actionType.toLowerCase()) {
            case "tap": return 0;
            case "swipe_up": return 1;
            case "swipe_down": return 2;
            case "swipe_left": return 3;
            case "swipe_right": return 4;
            case "long_press": return 5;
            case "double_tap": return 6;
            case "avoid": return 7;
            default: return 0; // Default to tap
        }
    }
    
    /**
     * Update TensorFlow Lite models with new training data
     */
    private void updateTensorFlowModels(List<LabeledObject> newObjects) {
        try {
            TensorFlowLiteHelper tfHelper = new TensorFlowLiteHelper(context);
            
            // Create training data in TFLite format
            for (LabeledObject obj : newObjects) {
                // Extract image patch for object detection model
                Bitmap objectPatch = extractObjectPatch(obj);
                if (objectPatch != null) {
                    // Add to TensorFlow Lite training dataset
                    tfHelper.addTrainingExample(objectPatch, obj.getLabel(), obj.getConfidence());
                }
            }
            
            // Trigger model retraining
            tfHelper.retrainModel("custom_object_detection");
            
            Log.d(TAG, "Updated TensorFlow Lite models with " + newObjects.size() + " new objects");
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating TensorFlow models", e);
        }
    }
    
    /**
     * Update OpenCV template matching with new objects
     */
    private void updateOpenCVTemplates(List<LabeledObject> newObjects) {
        try {
            // Get OpenCV helper
            com.gestureai.gameautomation.utils.OpenCVHelper openCVHelper = 
                new com.gestureai.gameautomation.utils.OpenCVHelper(context);
            
            for (LabeledObject obj : newObjects) {
                // Extract template from labeled object
                Bitmap template = extractObjectPatch(obj);
                if (template != null) {
                    // Add template to OpenCV matcher
                    openCVHelper.addTemplate(obj.getLabel(), template, obj.getConfidence());
                }
            }
            
            Log.d(TAG, "Updated OpenCV templates with " + newObjects.size() + " new objects");
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating OpenCV templates", e);
        }
    }
    
    /**
     * Extract object image patch from screenshot
     */
    private Bitmap extractObjectPatch(LabeledObject obj) {
        try {
            Bitmap screenshot = obj.getSourceBitmap();
            if (screenshot != null) {
                Rect bounds = obj.getBoundingBox();
                
                // Ensure bounds are within screenshot
                int left = Math.max(0, bounds.left);
                int top = Math.max(0, bounds.top);
                int right = Math.min(screenshot.getWidth(), bounds.right);
                int bottom = Math.min(screenshot.getHeight(), bounds.bottom);
                
                if (right > left && bottom > top) {
                    return Bitmap.createBitmap(screenshot, left, top, right - left, bottom - top);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting object patch", e);
        }
        return null;
    }
    
    /**
     * Import existing training dataset
     */
    public void importTrainingDataset(ImportCallback callback) {
        executorService.execute(() -> {
            try {
                File importFile = new File(context.getExternalFilesDir(null), "training_dataset.json");
                
                if (!importFile.exists()) {
                    mainHandler.post(() -> callback.onImportError("No existing dataset found"));
                    return;
                }
                
                // Load and parse dataset
                List<LabeledObject> importedObjects = parseDatasetFromFile(importFile);
                
                mainHandler.post(() -> callback.onImportComplete(importedObjects));
                
            } catch (Exception e) {
                Log.e(TAG, "Import error", e);
                mainHandler.post(() -> callback.onImportError(e.getMessage()));
            }
        });
    }
    
    /**
     * Create bounding box overlay on image
     */
    public Bitmap drawBoundingBoxes(Bitmap originalImage, List<LabeledObject> objects) {
        Bitmap resultBitmap = originalImage.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(resultBitmap);
        
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4.0f);
        
        for (LabeledObject obj : objects) {
            // Use different colors for different object types
            paint.setColor(getColorForObject(obj.name));
            
            // Draw bounding box
            canvas.drawRect(obj.boundingBox, paint);
            
            // Draw label text
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(24);
            canvas.drawText(obj.name, obj.boundingBox.left, obj.boundingBox.top - 10, paint);
            paint.setStyle(Paint.Style.STROKE);
        }
        
        return resultBitmap;
    }
    
    /**
     * Validate labeled object for training
     */
    public boolean validateLabeledObject(LabeledObject object) {
        // Check if bounding box is valid
        if (object.boundingBox.width() < 10 || object.boundingBox.height() < 10) {
            return false;
        }
        
        // Check if object name is valid
        if (object.name == null || object.name.trim().isEmpty()) {
            return false;
        }
        
        // Check confidence threshold
        if (object.confidence < 0.1f) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Get training statistics
     */
    public TrainingStats getTrainingStats() {
        TrainingStats stats = new TrainingStats();
        stats.totalObjects = trainingDataset.size();
        stats.uniqueClasses = countUniqueClasses();
        stats.averageConfidence = calculateAverageConfidence();
        stats.datasetQuality = assessDatasetQuality();
        
        return stats;
    }
    
    // Private helper methods
    
    private void saveToInternalStorage(List<LabeledObject> objects) {
        // Save to app's internal storage for persistence
        File internalFile = new File(context.getFilesDir(), "labeled_objects.dat");
        
        try {
            FileOutputStream fos = new FileOutputStream(internalFile, true); // Append mode
            
            for (LabeledObject obj : objects) {
                String objectData = obj.name + "," + 
                                 obj.boundingBox.left + "," + obj.boundingBox.top + "," +
                                 obj.boundingBox.right + "," + obj.boundingBox.bottom + "," +
                                 obj.confidence + "\n";
                fos.write(objectData.getBytes());
            }
            
            fos.close();
            Log.d(TAG, "Saved " + objects.size() + " objects to internal storage");
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to save to internal storage", e);
        }
    }
    
    private void trainObjectDetectionModel(List<LabeledObject> newObjects) {
        // Connect to ObjectDetectionEngine for model training
        ObjectDetectionEngine detectionEngine = new ObjectDetectionEngine(context);
        
        // Convert labeled objects to training format
        for (LabeledObject obj : newObjects) {
            detectionEngine.addTrainingExample(obj.name, obj.boundingBox, obj.confidence);
        }
        
        // Trigger model retraining
        detectionEngine.retrainModel();
        
        Log.d(TAG, "Triggered model retraining with " + newObjects.size() + " new examples");
    }
    
    private String convertDatasetToJson(List<LabeledObject> dataset) {
        StringBuilder json = new StringBuilder();
        json.append("{\"dataset\": [");
        
        for (int i = 0; i < dataset.size(); i++) {
            LabeledObject obj = dataset.get(i);
            json.append("{");
            json.append("\"name\": \"").append(obj.name).append("\",");
            json.append("\"bbox\": [")
                .append(obj.boundingBox.left).append(",")
                .append(obj.boundingBox.top).append(",")
                .append(obj.boundingBox.right).append(",")
                .append(obj.boundingBox.bottom).append("],");
            json.append("\"confidence\": ").append(obj.confidence);
            json.append("}");
            
            if (i < dataset.size() - 1) {
                json.append(",");
            }
        }
        
        json.append("]}");
        return json.toString();
    }
    
    private List<LabeledObject> parseDatasetFromFile(File file) {
        // Placeholder for JSON parsing - would use actual JSON library in production
        List<LabeledObject> objects = new ArrayList<>();
        
        // For now, return empty list - would implement actual parsing
        Log.d(TAG, "Dataset parsing not fully implemented");
        
        return objects;
    }
    
    private int getColorForObject(String objectName) {
        // Return different colors for different object types
        switch (objectName.toLowerCase()) {
            case "enemy": return 0xFFFF0000; // Red
            case "weapon": return 0xFF00FF00; // Green
            case "powerup": return 0xFF0000FF; // Blue
            case "obstacle": return 0xFFFFFF00; // Yellow
            default: return 0xFFFF00FF; // Magenta
        }
    }
    
    private int countUniqueClasses() {
        return (int) trainingDataset.stream()
                .map(obj -> obj.name)
                .distinct()
                .count();
    }
    
    private float calculateAverageConfidence() {
        if (trainingDataset.isEmpty()) return 0.0f;
        
        float sum = 0.0f;
        for (LabeledObject obj : trainingDataset) {
            sum += obj.confidence;
        }
        
        return sum / trainingDataset.size();
    }
    
    private float assessDatasetQuality() {
        if (trainingDataset.isEmpty()) return 0.0f;
        
        // Simple quality assessment based on:
        // - Number of examples per class
        // - Average confidence
        // - Bounding box size distribution
        
        int uniqueClasses = countUniqueClasses();
        float avgConfidence = calculateAverageConfidence();
        float examplesPerClass = (float) trainingDataset.size() / Math.max(1, uniqueClasses);
        
        // Quality score between 0-1
        float qualityScore = 0.0f;
        qualityScore += Math.min(1.0f, examplesPerClass / 50.0f) * 0.4f; // Examples per class
        qualityScore += avgConfidence * 0.3f; // Confidence
        qualityScore += Math.min(1.0f, uniqueClasses / 10.0f) * 0.3f; // Class diversity
        
        return qualityScore;
    }
    
    /**
     * Training statistics data class
     */
    public static class TrainingStats {
        public int totalObjects;
        public int uniqueClasses;
        public float averageConfidence;
        public float datasetQuality;
    }
    
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
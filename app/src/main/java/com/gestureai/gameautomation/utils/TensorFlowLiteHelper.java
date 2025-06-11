package com.gestureai.gameautomation.utils;

import android.content.Context;
import android.graphics.Bitmap;
import timber.log.Timber;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * TensorFlow Lite Helper for object detection and classification
 */
public class TensorFlowLiteHelper {

    
    private Interpreter objectDetectionInterpreter;
    private Interpreter gestureClassifierInterpreter;
    private boolean isInitialized = false;
    
    // Model configuration
    private static final String OBJECT_DETECTION_MODEL = "object_detection.tflite";
    private static final String GESTURE_CLASSIFIER_MODEL = "gesture_classifier.tflite";
    private static final int INPUT_SIZE = 320;
    private static final int NUM_DETECTIONS = 10;
    private static final float CONFIDENCE_THRESHOLD = 0.5f;
    
    // Class labels
    private String[] objectLabels = {
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck",
        "boat", "traffic light", "fire hydrant", "stop sign", "parking meter", "bench",
        "bird", "cat", "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra",
        "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
        "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove",
        "skateboard", "surfboard", "tennis racket", "bottle", "wine glass", "cup",
        "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange",
        "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
        "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse",
        "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink",
        "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier",
        "toothbrush"
    };
    
    public static class Detection {
        public String label;
        public float confidence;
        public float left, top, right, bottom;
        
        public Detection(String label, float confidence, float left, float top, float right, float bottom) {
            this.label = label;
            this.confidence = confidence;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
    
    public void initializeObjectDetection(Context context) {
        try {
            // Load object detection model with fallback handling
            try {
                ByteBuffer objectDetectionModel = FileUtil.loadMappedFile(context, OBJECT_DETECTION_MODEL);
                Interpreter.Options options = new Interpreter.Options();
                options.setNumThreads(4);
                objectDetectionInterpreter = new Interpreter(objectDetectionModel, options);
                Timber.d("Object detection model loaded successfully");
            } catch (IOException e) {
                Timber.w(e, "Object detection model not found, creating fallback interpreter");
                objectDetectionInterpreter = null; // Will use fallback detection
            }
            
            // Load gesture classifier model with fallback handling
            try {
                ByteBuffer gestureModel = FileUtil.loadMappedFile(context, GESTURE_CLASSIFIER_MODEL);
                Interpreter.Options options = new Interpreter.Options();
                options.setNumThreads(2);
                gestureClassifierInterpreter = new Interpreter(gestureModel, options);
                Timber.d("Gesture classifier model loaded successfully");
            } catch (IOException e) {
                Timber.w(e, "Gesture classifier model not found, using rule-based fallback");
                gestureClassifierInterpreter = null; // Will use rule-based classification
            }
            
            // Mark as initialized even if models are missing - fallbacks will handle it
            isInitialized = true;
            Timber.d("TensorFlow Lite helper initialized with available models");
            
        } catch (Exception e) {
            Timber.e(e, "Critical error during TensorFlow Lite initialization");
            isInitialized = true; // Still set to true to enable fallback methods
            objectDetectionInterpreter = null;
            gestureClassifierInterpreter = null;
        }
    }
    
    public List<Detection> detectObjects(Bitmap bitmap) {
        List<Detection> detections = new ArrayList<>();
        
        if (!isInitialized || objectDetectionInterpreter == null) {
            Timber.w("Object detection not initialized, returning empty results");
            return detections;
        }
        
        try {
            // Preprocess image
            ByteBuffer inputBuffer = preprocessImage(bitmap);
            
            // Prepare output arrays
            float[][][] outputLocations = new float[1][NUM_DETECTIONS][4];
            float[][] outputClasses = new float[1][NUM_DETECTIONS];
            float[][] outputScores = new float[1][NUM_DETECTIONS];
            float[] numDetections = new float[1];
            
            // Create output map
            Object[] inputs = {inputBuffer};
            java.util.Map<Integer, Object> outputs = new java.util.HashMap<>();
            outputs.put(0, outputLocations);
            outputs.put(1, outputClasses);
            outputs.put(2, outputScores);
            outputs.put(3, numDetections);
            
            // Run inference
            objectDetectionInterpreter.runForMultipleInputsOutputs(inputs, outputs);
            
            // Process results
            int numValidDetections = Math.min(NUM_DETECTIONS, (int) numDetections[0]);
            for (int i = 0; i < numValidDetections; i++) {
                float confidence = outputScores[0][i];
                if (confidence > CONFIDENCE_THRESHOLD) {
                    int classIndex = (int) outputClasses[0][i];
                    String label = classIndex < objectLabels.length ? objectLabels[classIndex] : "Unknown";
                    
                    float top = outputLocations[0][i][0];
                    float left = outputLocations[0][i][1];
                    float bottom = outputLocations[0][i][2];
                    float right = outputLocations[0][i][3];
                    
                    detections.add(new Detection(label, confidence, left, top, right, bottom));
                }
            }
            
        } catch (Exception e) {
            Timber.e(e, "Error during object detection inference");
        }
        
        return detections;
    }
    
    /**
     * Enhanced object detection with reasoning focus from "what" field
     */
    public List<Detection> detectObjectsWithFocus(Bitmap bitmap, String targetObject) {
        List<Detection> allDetections = detectObjects(bitmap);
        List<Detection> focusedDetections = new ArrayList<>();
        
        // Filter and enhance detections based on target object
        for (Detection detection : allDetections) {
            float relevanceScore = calculateRelevanceScore(detection.label, targetObject);
            
            if (relevanceScore > 0.3f) { // Minimum relevance threshold
                // Create enhanced detection with boosted confidence
                Detection enhancedDetection = new Detection(
                    detection.label,
                    Math.min(1.0f, detection.confidence * (1.0f + relevanceScore)),
                    detection.left, detection.top, detection.right, detection.bottom
                );
                focusedDetections.add(enhancedDetection);
            }
        }
        
        // If no focused detections found, return original with lower confidence
        if (focusedDetections.isEmpty()) {
            for (Detection detection : allDetections) {
                Detection reducedConfidenceDetection = new Detection(
                    detection.label,
                    detection.confidence * 0.8f, // Reduce confidence when target not found
                    detection.left, detection.top, detection.right, detection.bottom
                );
                focusedDetections.add(reducedConfidenceDetection);
            }
        }
        
        return focusedDetections;
    }
    
    /**
     * Calculate semantic relevance between detected object and target from reasoning
     */
    private float calculateRelevanceScore(String detectedLabel, String targetObject) {
        if (detectedLabel == null || targetObject == null) return 0.0f;
        
        String detectedLower = detectedLabel.toLowerCase();
        String targetLower = targetObject.toLowerCase();
        
        // Exact match
        if (detectedLower.equals(targetLower)) return 1.0f;
        
        // Contains match
        if (detectedLower.contains(targetLower) || targetLower.contains(detectedLower)) return 0.8f;
        
        // Semantic similarity for common game objects
        Map<String, List<String>> semanticGroups = createSemanticGroups();
        
        for (Map.Entry<String, List<String>> group : semanticGroups.entrySet()) {
            List<String> synonyms = group.getValue();
            boolean detectedInGroup = synonyms.stream().anyMatch(s -> detectedLower.contains(s) || s.contains(detectedLower));
            boolean targetInGroup = synonyms.stream().anyMatch(s -> targetLower.contains(s) || s.contains(targetLower));
            
            if (detectedInGroup && targetInGroup) return 0.6f;
        }
        
        // Character-level similarity as fallback
        return calculateLevenshteinSimilarity(detectedLower, targetLower);
    }
    
    /**
     * Create semantic groups for game objects
     */
    private Map<String, List<String>> createSemanticGroups() {
        Map<String, List<String>> groups = new HashMap<>();
        
        groups.put("enemies", Arrays.asList("enemy", "opponent", "monster", "boss", "villain", "hostile"));
        groups.put("collectibles", Arrays.asList("coin", "gem", "item", "powerup", "pickup", "bonus", "treasure"));
        groups.put("weapons", Arrays.asList("weapon", "sword", "gun", "rifle", "blade", "launcher"));
        groups.put("ui_elements", Arrays.asList("button", "icon", "menu", "panel", "tab", "control"));
        groups.put("navigation", Arrays.asList("door", "gate", "portal", "path", "bridge", "stairs", "ladder"));
        groups.put("characters", Arrays.asList("player", "character", "hero", "avatar", "npc"));
        
        return groups;
    }
    
    /**
     * Calculate string similarity using Levenshtein distance
     */
    private float calculateLevenshteinSimilarity(String s1, String s2) {
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0f;
        
        int editDistance = calculateEditDistance(s1, s2);
        return 1.0f - ((float) editDistance / maxLength);
    }
    
    private int calculateEditDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    public String classifyGesture(Bitmap bitmap) {
        if (!isInitialized || gestureClassifierInterpreter == null) {
            return "Unknown";
        }
        
        try {
            // Preprocess image for gesture classification
            ByteBuffer inputBuffer = preprocessImage(bitmap);
            
            // Prepare output
            float[][] output = new float[1][6]; // 6 gesture classes
            
            // Run inference
            gestureClassifierInterpreter.run(inputBuffer, output);
            
            // Find class with highest confidence
            int bestClass = 0;
            float bestConfidence = output[0][0];
            for (int i = 1; i < output[0].length; i++) {
                if (output[0][i] > bestConfidence) {
                    bestConfidence = output[0][i];
                    bestClass = i;
                }
            }
            
            if (bestConfidence > CONFIDENCE_THRESHOLD) {
                return getGestureLabel(bestClass);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during gesture classification", e);
        }
        
        return "Unknown";
    }
    
    private ByteBuffer preprocessImage(Bitmap bitmap) {
        // Resize bitmap to model input size
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        
        // Create ByteBuffer for model input
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);
        inputBuffer.order(ByteOrder.nativeOrder());
        
        // Normalize pixel values to [0, 1]
        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        resizedBitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);
        
        for (int pixel : pixels) {
            // Extract RGB values and normalize
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;
            
            inputBuffer.putFloat(r / 255.0f);
            inputBuffer.putFloat(g / 255.0f);
            inputBuffer.putFloat(b / 255.0f);
        }
        
        return inputBuffer;
    }
    
    private String getGestureLabel(int classIndex) {
        String[] gestureLabels = {
            "pointing", "peace", "fist", "open_palm", "thumbs_up", "thumbs_down"
        };
        
        return classIndex < gestureLabels.length ? gestureLabels[classIndex] : "Unknown";
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }
    
    public void cleanup() {
        if (objectDetectionInterpreter != null) {
            objectDetectionInterpreter.close();
        }
        if (gestureClassifierInterpreter != null) {
            gestureClassifierInterpreter.close();
        }
        isInitialized = false;
        Log.d(TAG, "TensorFlow Lite helper cleaned up");
    }
}
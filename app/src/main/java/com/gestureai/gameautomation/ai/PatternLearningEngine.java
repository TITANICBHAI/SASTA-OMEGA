package com.gestureai.gameautomation.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Advanced pattern learning engine for gesture recognition and adaptation
 * Connects to the gesture training UI components
 */
public class PatternLearningEngine {
    private static final String TAG = "PatternLearningEngine";

    private Context context;
    private ExecutorService executorService;
    private Handler mainHandler;
    private volatile boolean isTraining = false;
    private volatile boolean isDestroyed = false;

    // Pattern storage with bounds checking and corruption prevention
    private Map<String, List<TrainingExample>> learnedPatterns;
    private final Object trainingDataLock = new Object();
    private List<TrainingExample> trainingData;
    private GestureClassifier classifier;

    // Critical: Feature extraction corruption prevention
    private volatile boolean featureExtractionCorrupted = false;
    private volatile int consecutiveExtractionFailures = 0;
    private volatile float lastValidAccuracy = 0.0f;
    private volatile boolean convergenceDetected = false;
    private final Object featureLock = new Object();

    // Memory management constants
    private static final int MAX_TRAINING_EXAMPLES = 10000;
    private static final int MAX_PATTERNS_PER_GESTURE = 1000;

    // Callbacks for UI communication
    public interface TrainingCallback {
        void onProgress(float progress);
        void onComplete(float accuracy);
        void onError(String error);
    }

    public interface ExportCallback {
        void onSuccess(String filePath);
        void onError(String error);
    }

    public interface RecognitionCallback {
        void onGestureRecognized(String gestureName, float confidence);
        void onNoGestureFound();
    }

    public PatternLearningEngine(Context context) {
        this.context = context;
        this.executorService = Executors.newFixedThreadPool(2); // Fixed pool to prevent unlimited threads
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.learnedPatterns = new ConcurrentHashMap<>(); // Thread-safe
        this.trainingData = new ArrayList<>();
        this.classifier = new GestureClassifier();

        Log.d(TAG, "PatternLearningEngine initialized with bounded resources");
    }

    /**
     * Critical: Cleanup resources to prevent memory leaks and thread accumulation
     */
    public void cleanup() {
        synchronized (trainingDataLock) {
            isDestroyed = true;
            isTraining = false;

            // Cancel all running tasks
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdownNow();
                try {
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        Log.w(TAG, "ExecutorService did not terminate gracefully");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.w(TAG, "Interrupted while waiting for executor shutdown");
                }
                executorService = null;
            }

            // Clear training data to free memory
            if (trainingData != null) {
                trainingData.clear();
                trainingData = null;
            }

            // Clear learned patterns
            if (learnedPatterns != null) {
                learnedPatterns.clear();
                learnedPatterns = null;
            }

            // Clear handler references
            if (mainHandler != null) {
                mainHandler.removeCallbacksAndMessages(null);
                mainHandler = null;
            }

            // Clear classifier
            if (classifier != null) {
                classifier.cleanup();
                classifier = null;
            }

            Log.d(TAG, "PatternLearningEngine cleanup completed");
        }
    }

    /**
     * Train gesture model with collected data
     */
    public void trainGestureModel(TrainingCallback callback) {
        if (isDestroyed) {
            callback.onError("PatternLearningEngine has been destroyed");
            return;
        }

        if (isTraining) {
            callback.onError("Training already in progress");
            return;
        }

        synchronized (trainingDataLock) {
            if (trainingData.isEmpty()) {
                callback.onError("No training data available");
                return;
            }

            // Bounds checking to prevent memory explosion
            if (trainingData.size() > MAX_TRAINING_EXAMPLES) {
                Log.w(TAG, "Training data exceeds maximum size, trimming to prevent memory issues");
                trainingData = trainingData.subList(trainingData.size() - MAX_TRAINING_EXAMPLES, trainingData.size());
            }
        }

        isTraining = true;

        executorService.execute(() -> {
            try {
                Log.d(TAG, "Starting gesture model training with " + trainingData.size() + " examples");

                // Phase 1: Data preprocessing with memory safety
                mainHandler.post(() -> callback.onProgress(0.1f));
                List<ProcessedPattern> processedData = preprocessTrainingDataSafe();

                if (processedData == null || processedData.isEmpty()) {
                    throw new RuntimeException("No valid training data after preprocessing");
                }

                // Phase 2: Feature extraction with bounds checking
                mainHandler.post(() -> callback.onProgress(0.3f));
                List<FeatureVector> features = extractFeaturesSafe(processedData);

                // Clear processed data to free memory
                processedData.clear();

                // Phase 3: Model training
                mainHandler.post(() -> callback.onProgress(0.5f));
                float accuracy = trainClassificationModel(features);

                // Phase 4: Validation
                mainHandler.post(() -> callback.onProgress(0.8f));
                float validationAccuracy = validateModel(features);

                // Phase 5: Model optimization
                mainHandler.post(() -> callback.onProgress(0.9f));
                optimizeModel();

                // Complete
                isTraining = false;
                mainHandler.post(() -> callback.onComplete(validationAccuracy));

                Log.d(TAG, "Training completed with accuracy: " + validationAccuracy);

            } catch (Exception e) {
                Log.e(TAG, "Training error", e);
                isTraining = false;
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Add gesture training example with memory bounds checking
     */
    public void addGestureExample(String gestureName, List<Point> gesturePoints, long timestamp) {
        if (isDestroyed || gesturePoints == null || gestureName == null) {
            return;
        }

        synchronized (trainingDataLock) {
            // Prevent memory explosion by enforcing bounds
            if (trainingData.size() >= MAX_TRAINING_EXAMPLES) {
                Log.w(TAG, "Training data limit reached, removing oldest example");
                trainingData.remove(0);
            }

            // Limit patterns per gesture type to prevent memory explosion
            long currentGestureCount = trainingData.stream()
                    .filter(ex -> gestureName.equals(ex.gestureName))
                    .count();

            if (currentGestureCount >= MAX_PATTERNS_PER_GESTURE) {
                // Remove oldest example of this gesture type
                trainingData.removeIf(ex -> gestureName.equals(ex.gestureName) &&
                        ex.timestamp < System.currentTimeMillis() - 3600000); // Older than 1 hour
            }

            TrainingExample example = new TrainingExample();
            example.gestureName = gestureName;
            example.points = new ArrayList<>(gesturePoints);
            example.timestamp = timestamp;
            example.duration = calculateGestureDuration(gesturePoints);

            trainingData.add(example);

            Log.d(TAG, "Added gesture example: " + gestureName + " with " + gesturePoints.size() + " points (total: " + trainingData.size() + ")");
        }
    }

    /**
     * Recognize gesture from input points
     */
    public void recognizeGesture(List<Point> gesturePoints, RecognitionCallback callback) {
        executorService.execute(() -> {
            try {
                // Preprocess input gesture
                ProcessedPattern pattern = preprocessGesture(gesturePoints);

                // Extract features
                FeatureVector features = extractFeatures(pattern);

                // Classify gesture
                ClassificationResult result = classifier.classify(features);

                if (result.confidence > 0.6f) {
                    mainHandler.post(() -> callback.onGestureRecognized(result.gestureName, result.confidence));
                } else {
                    mainHandler.post(() -> callback.onNoGestureFound());
                }

            } catch (Exception e) {
                Log.e(TAG, "Recognition error", e);
                mainHandler.post(() -> callback.onNoGestureFound());
            }
        });
    }

    /**
     * Export gesture data for external use
     */
    public void exportGestureData(ExportCallback callback) {
        executorService.execute(() -> {
            try {
                File exportFile = new File(context.getExternalFilesDir(null), "gesture_patterns.dat");

                StringBuilder data = new StringBuilder();
                data.append("# Gesture Pattern Export\n");
                data.append("# Format: GestureName,PointCount,Points...\n\n");

                for (TrainingExample example : trainingData) {
                    data.append(example.gestureName).append(",");
                    data.append(example.points.size()).append(",");

                    for (Point point : example.points) {
                        data.append(point.x).append(",").append(point.y).append(",");
                    }
                    data.append("\n");
                }

                FileOutputStream fos = new FileOutputStream(exportFile);
                fos.write(data.toString().getBytes());
                fos.close();

                mainHandler.post(() -> callback.onSuccess(exportFile.getAbsolutePath()));

            } catch (IOException e) {
                Log.e(TAG, "Export error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Get learning statistics
     */
    public LearningStats getLearningStats() {
        LearningStats stats = new LearningStats();
        stats.totalExamples = trainingData.size();
        stats.uniqueGestures = countUniqueGestures();
        stats.averageAccuracy = classifier.getAverageAccuracy();
        stats.isTraining = isTraining;

        return stats;
    }

    // Private helper methods

    private List<ProcessedPattern> preprocessTrainingData() {
        List<ProcessedPattern> processed = new ArrayList<>();

        for (TrainingExample example : trainingData) {
            ProcessedPattern pattern = preprocessGesture(example.points);
            pattern.gestureName = example.gestureName;
            processed.add(pattern);
        }

        return processed;
    }

    private ProcessedPattern preprocessGesture(List<Point> points) {
        if (featureExtractionCorrupted) {
            Log.w(TAG, "Feature extraction corrupted, attempting recovery");
            return createFallbackPattern(points);
        }

        try {
            synchronized (featureLock) {
                ProcessedPattern pattern = new ProcessedPattern();

                // Critical: Validate input data before processing
                if (points == null || points.isEmpty()) {
                    consecutiveExtractionFailures++;
                    Log.w(TAG, "Invalid gesture points detected");
                    return createFallbackPattern(points);
                }

                // Normalize gesture with corruption detection
                pattern.normalizedPoints = normalizeGestureWithValidation(points);
                if (pattern.normalizedPoints == null) {
                    consecutiveExtractionFailures++;
                    return createFallbackPattern(points);
                }

                // Calculate features with bounds checking
                pattern.velocities = calculateVelocitiesWithValidation(points);
                pattern.curvatures = calculateCurvaturesWithValidation(points);
                pattern.boundingBox = calculateBoundingBoxWithValidation(points);

                // Validate extracted features
                if (!validateFeatureIntegrity(pattern)) {
                    consecutiveExtractionFailures++;
                    Log.w(TAG, "Feature integrity validation failed");

                    if (consecutiveExtractionFailures > 5) {
                        featureExtractionCorrupted = true;
                        Log.e(TAG, "Feature extraction corrupted, switching to recovery mode");
                        return createFallbackPattern(points);
                    }
                }

                // Reset failure count on success
                consecutiveExtractionFailures = 0;
                return pattern;
            }

        } catch (Exception e) {
            Log.e(TAG, "Critical error in feature extraction", e);
            consecutiveExtractionFailures++;

            if (consecutiveExtractionFailures > 3) {
                featureExtractionCorrupted = true;
                Log.e(TAG, "Feature extraction system corrupted");
            }

            return createFallbackPattern(points);
        }
    }

    private ProcessedPattern createFallbackPattern(List<Point> points) {
        ProcessedPattern pattern = new ProcessedPattern();

        try {
            // Safe fallback processing
            if (points != null && !points.isEmpty()) {
                pattern.normalizedPoints = new ArrayList<>(points);
                pattern.velocities = new float[Math.max(0, points.size() - 1)];
                pattern.curvatures = new float[Math.max(0, points.size() - 2)];
                pattern.boundingBox = new int[]{0, 0, 100, 100}; // Default bounding box
            } else {
                pattern.normalizedPoints = new ArrayList<>();
                pattern.velocities = new float[0];
                pattern.curvatures = new float[0];
                pattern.boundingBox = new int[]{0, 0, 1, 1};
            }

            Log.d(TAG, "Created fallback pattern successfully");

        } catch (Exception e) {
            Log.e(TAG, "Critical failure in fallback pattern creation", e);
            // Absolute minimal fallback
            pattern.normalizedPoints = new ArrayList<>();
            pattern.velocities = new float[0];
            pattern.curvatures = new float[0];
            pattern.boundingBox = new int[]{0, 0, 1, 1};
        }

        return pattern;
    }

    private boolean validateFeatureIntegrity(ProcessedPattern pattern) {
        try {
            // Validate normalized points
            if (pattern.normalizedPoints == null) return false;

            for (Point p : pattern.normalizedPoints) {
                if (p == null || p.x < -10000 || p.x > 10000 || p.y < -10000 || p.y > 10000) {
                    Log.w(TAG, "Invalid normalized point detected");
                    return false;
                }
            }

            // Validate velocities
            if (pattern.velocities != null) {
                for (float v : pattern.velocities) {
                    if (Float.isNaN(v) || Float.isInfinite(v) || Math.abs(v) > 10000) {
                        Log.w(TAG, "Invalid velocity detected");
                        return false;
                    }
                }
            }

            // Validate curvatures
            if (pattern.curvatures != null) {
                for (float c : pattern.curvatures) {
                    if (Float.isNaN(c) || Float.isInfinite(c) || Math.abs(c) > 100) {
                        Log.w(TAG, "Invalid curvature detected");
                        return false;
                    }
                }
            }

            // Validate bounding box
            if (pattern.boundingBox != null && pattern.boundingBox.length >= 4) {
                for (int bound : pattern.boundingBox) {
                    if (bound < -10000 || bound > 10000) {
                        Log.w(TAG, "Invalid bounding box detected");
                        return false;
                    }
                }
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error validating feature integrity", e);
            return false;
        }
    }

    private List<Point> normalizeGestureWithValidation(List<Point> points) {
        try {
            if (points == null || points.isEmpty()) return null;

            // Find bounding box with validation
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

            for (Point p : points) {
                if (p == null) continue;
                minX = Math.min(minX, p.x);
                maxX = Math.max(maxX, p.x);
                minY = Math.min(minY, p.y);
                maxY = Math.max(maxY, p.y);
            }

            // Validate bounds
            if (minX == Integer.MAX_VALUE || maxX - minX <= 0 || maxY - minY <= 0) {
                Log.w(TAG, "Invalid gesture bounds");
                return null;
            }

            // Normalize with bounds checking
            List<Point> normalized = new ArrayList<>();
            int width = Math.max(1, maxX - minX);
            int height = Math.max(1, maxY - minY);

            for (Point p : points) {
                if (p == null) continue;

                int normalizedX = Math.round(100.0f * (p.x - minX) / width);
                int normalizedY = Math.round(100.0f * (p.y - minY) / height);

                // Clamp to valid range
                normalizedX = Math.max(0, Math.min(100, normalizedX));
                normalizedY = Math.max(0, Math.min(100, normalizedY));

                normalized.add(new Point(normalizedX, normalizedY));
            }

            return normalized;

        } catch (Exception e) {
            Log.e(TAG, "Error in gesture normalization", e);
            return null;
        }
    }

    private float[] calculateVelocitiesWithValidation(List<Point> points) {
        try {
            if (points == null || points.size() < 2) return new float[0];

            float[] velocities = new float[points.size() - 1];

            for (int i = 0; i < points.size() - 1; i++) {
                Point p1 = points.get(i);
                Point p2 = points.get(i + 1);

                if (p1 == null || p2 == null) {
                    velocities[i] = 0.0f;
                    continue;
                }

                float dx = p2.x - p1.x;
                float dy = p2.y - p1.y;
                float velocity = (float) Math.sqrt(dx * dx + dy * dy);

                // Bound velocity to prevent corruption
                velocities[i] = Math.max(0.0f, Math.min(1000.0f, velocity));
            }

            return velocities;

        } catch (Exception e) {
            Log.e(TAG, "Error calculating velocities", e);
            return new float[0];
        }
    }

    private float[] calculateCurvaturesWithValidation(List<Point> points) {
        try {
            if (points == null || points.size() < 3) return new float[0];

            float[] curvatures = new float[points.size() - 2];

            for (int i = 1; i < points.size() - 1; i++) {
                Point p1 = points.get(i - 1);
                Point p2 = points.get(i);
                Point p3 = points.get(i + 1);

                if (p1 == null || p2 == null || p3 == null) {
                    curvatures[i - 1] = 0.0f;
                    continue;
                }

                // Calculate angle change (simplified curvature)
                float angle1 = (float) Math.atan2(p2.y - p1.y, p2.x - p1.x);
                float angle2 = (float) Math.atan2(p3.y - p2.y, p3.x - p2.x);
                float curvature = Math.abs(angle2 - angle1);

                // Normalize and bound curvature
                if (curvature > Math.PI) curvature = 2 * (float) Math.PI - curvature;
                curvatures[i - 1] = Math.max(0.0f, Math.min((float) Math.PI, curvature));
            }

            return curvatures;

        } catch (Exception e) {
            Log.e(TAG, "Error calculating curvatures", e);
            return new float[0];
        }
    }

    private int[] calculateBoundingBoxWithValidation(List<Point> points) {
        try {
            if (points == null || points.isEmpty()) return new int[]{0, 0, 1, 1};

            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

            for (Point p : points) {
                if (p == null) continue;
                minX = Math.min(minX, p.x);
                maxX = Math.max(maxX, p.x);
                minY = Math.min(minY, p.y);
                maxY = Math.max(maxY, p.y);
            }

            // Validate and return bounded box
            if (minX == Integer.MAX_VALUE) {
                return new int[]{0, 0, 1, 1};
            }

            return new int[]{
                    Math.max(-10000, Math.min(10000, minX)),
                    Math.max(-10000, Math.min(10000, minY)),
                    Math.max(-10000, Math.min(10000, maxX)),
                    Math.max(-10000, Math.min(10000, maxY))
            };

        } catch (Exception e) {
            Log.e(TAG, "Error calculating bounding box", e);
            return new int[]{0, 0, 1, 1};
        }
    }

    /**
     * Memory-safe preprocessing to prevent memory explosion during training
     */
    private List<ProcessedPattern> preprocessTrainingDataSafe() {
        List<ProcessedPattern> processedData = new ArrayList<>();

        synchronized (trainingDataLock) {
            try {
                // Process in smaller batches to prevent memory spikes
                final int BATCH_SIZE = 100;

                for (int i = 0; i < trainingData.size(); i += BATCH_SIZE) {
                    int endIndex = Math.min(i + BATCH_SIZE, trainingData.size());
                    List<TrainingExample> batch = trainingData.subList(i, endIndex);

                    for (TrainingExample example : batch) {
                        if (example != null && example.points != null && !example.points.isEmpty()) {
                            try {
                                ProcessedPattern pattern = preprocessGesture(example.points);
                                if (pattern != null && validateFeatureIntegrity(pattern)) {
                                    processedData.add(pattern);
                                }
                            } catch (OutOfMemoryError e) {
                                Log.e(TAG, "OOM during preprocessing, cleaning up and continuing");
                                System.gc();
                                break;
                            } catch (Exception e) {
                                Log.w(TAG, "Error preprocessing training example, skipping", e);
                            }
                        }
                    }

                    // Force garbage collection between batches
                    if (i % (BATCH_SIZE * 5) == 0) {
                        System.gc();
                        Thread.yield();
                    }
                }

                Log.d(TAG, "Successfully preprocessed " + processedData.size() + " training examples");

            } catch (Exception e) {
                Log.e(TAG, "Error in safe preprocessing", e);
            }
        }

        return processedData;
    }

    /**
     * Memory-safe feature extraction with bounds checking
     */
    private List<FeatureVector> extractFeaturesSafe(List<ProcessedPattern> processedData) {
        List<FeatureVector> features = new ArrayList<>();

        try {
            final int MAX_FEATURES = 1000; // Limit to prevent memory explosion

            for (int i = 0; i < Math.min(processedData.size(), MAX_FEATURES); i++) {
                ProcessedPattern pattern = processedData.get(i);

                if (pattern != null) {
                    try {
                        FeatureVector feature = extractFeatures(pattern);
                        if (feature != null && validateFeatureVector(feature)) {
                            features.add(feature);
                        }
                    } catch (OutOfMemoryError e) {
                        Log.e(TAG, "OOM during feature extraction, stopping at " + features.size() + " features");
                        System.gc();
                        break;
                    } catch (Exception e) {
                        Log.w(TAG, "Error extracting features from pattern, skipping", e);
                    }
                }

                // Periodic cleanup
                if (i % 50 == 0) {
                    System.gc();
                }
            }

            Log.d(TAG, "Extracted " + features.size() + " feature vectors safely");

        } catch (Exception e) {
            Log.e(TAG, "Error in safe feature extraction", e);
        }

        return features;
    }

    /**
     * Validate feature vector to prevent corruption
     */
    private boolean validateFeatureVector(FeatureVector feature) {
        try {
            if (feature == null || feature.values == null) return false;

            for (float value : feature.values) {
                if (Float.isNaN(value) || Float.isInfinite(value)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private List<Point> normalizeGesture(List<Point> points) {
        if (points.isEmpty()) return new ArrayList<>();

        // Find bounding box
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (Point p : points) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }

        int width = maxX - minX;
        int height = maxY - minY;

        if (width == 0 || height == 0) return points;

        // Normalize to 100x100 square
        List<Point> normalized = new ArrayList<>();
        for (Point p : points) {
            int newX = (p.x - minX) * 100 / width;
            int newY = (p.y - minY) * 100 / height;
            normalized.add(new Point(newX, newY));
        }

        return normalized;
    }

    private List<Float> calculateVelocities(List<Point> points) {
        List<Float> velocities = new ArrayList<>();

        for (int i = 1; i < points.size(); i++) {
            Point p1 = points.get(i - 1);
            Point p2 = points.get(i);

            double dx = p2.x - p1.x;
            double dy = p2.y - p1.y;
            float velocity = (float) Math.sqrt(dx * dx + dy * dy);

            velocities.add(velocity);
        }

        return velocities;
    }

    private List<Float> calculateCurvatures(List<Point> points) {
        List<Float> curvatures = new ArrayList<>();

        for (int i = 1; i < points.size() - 1; i++) {
            Point p1 = points.get(i - 1);
            Point p2 = points.get(i);
            Point p3 = points.get(i + 1);

            // Calculate angle between vectors
            double v1x = p2.x - p1.x;
            double v1y = p2.y - p1.y;
            double v2x = p3.x - p2.x;
            double v2y = p3.y - p2.y;

            double dot = v1x * v2x + v1y * v2y;
            double cross = v1x * v2y - v1y * v2x;

            float curvature = (float) Math.atan2(cross, dot);
            curvatures.add(curvature);
        }

        return curvatures;
    }

    private android.graphics.Rect calculateBoundingBox(List<Point> points) {
        if (points.isEmpty()) return new android.graphics.Rect();

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (Point p : points) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }

        return new android.graphics.Rect(minX, minY, maxX, maxY);
    }

    private List<FeatureVector> extractFeatures(List<ProcessedPattern> patterns) {
        List<FeatureVector> features = new ArrayList<>();

        for (ProcessedPattern pattern : patterns) {
            FeatureVector vector = extractFeatures(pattern);
            features.add(vector);
        }

        return features;
    }

    private FeatureVector extractFeatures(ProcessedPattern pattern) {
        FeatureVector vector = new FeatureVector();
        vector.gestureName = pattern.gestureName;

        // Basic geometric features
        vector.features = new float[20];

        // Bounding box features
        vector.features[0] = pattern.boundingBox.width();
        vector.features[1] = pattern.boundingBox.height();
        vector.features[2] = (float) pattern.boundingBox.width() / pattern.boundingBox.height();

        // Velocity features
        if (!pattern.velocities.isEmpty()) {
            float avgVelocity = 0;
            float maxVelocity = 0;
            for (float v : pattern.velocities) {
                avgVelocity += v;
                maxVelocity = Math.max(maxVelocity, v);
            }
            vector.features[3] = avgVelocity / pattern.velocities.size();
            vector.features[4] = maxVelocity;
        }

        // Curvature features
        if (!pattern.curvatures.isEmpty()) {
            float avgCurvature = 0;
            float maxCurvature = 0;
            for (float c : pattern.curvatures) {
                avgCurvature += Math.abs(c);
                maxCurvature = Math.max(maxCurvature, Math.abs(c));
            }
            vector.features[5] = avgCurvature / pattern.curvatures.size();
            vector.features[6] = maxCurvature;
        }

        // Point count and density
        vector.features[7] = pattern.normalizedPoints.size();
        vector.features[8] = pattern.normalizedPoints.size() / (float) (vector.features[0] * vector.features[1] + 1);

        return vector;
    }

    private float trainClassificationModel(List<FeatureVector> features) {
        // Simple k-NN classifier implementation
        classifier.train(features);

        // Calculate training accuracy
        int correct = 0;
        for (FeatureVector vector : features) {
            ClassificationResult result = classifier.classify(vector);
            if (result.gestureName.equals(vector.gestureName)) {
                correct++;
            }
        }

        return (float) correct / features.size();
    }

    private float validateModel(List<FeatureVector> features) {
        // Use leave-one-out cross-validation
        int correct = 0;

        for (int i = 0; i < features.size(); i++) {
            // Create training set without current example
            List<FeatureVector> trainSet = new ArrayList<>();
            for (int j = 0; j < features.size(); j++) {
                if (j != i) {
                    trainSet.add(features.get(j));
                }
            }

            // Train on reduced set
            GestureClassifier tempClassifier = new GestureClassifier();
            tempClassifier.train(trainSet);

            // Test on held-out example
            ClassificationResult result = tempClassifier.classify(features.get(i));
            if (result.gestureName.equals(features.get(i).gestureName)) {
                correct++;
            }
        }

        return features.isEmpty() ? 0f : (float) correct / features.size();
    }

    private void optimizeModel() {
        // Model optimization techniques
        classifier.optimize();
        Log.d(TAG, "Model optimization completed");
    }

    private long calculateGestureDuration(List<Point> points) {
        // Estimate duration based on point count (assuming 60fps capture)
        return points.size() * 16; // ~16ms per frame at 60fps
    }

    private int countUniqueGestures() {
        java.util.Set<String> uniqueGestures = new java.util.HashSet<>();
        for (TrainingExample example : trainingData) {
            uniqueGestures.add(example.gestureName);
        }
        return uniqueGestures.size();
    }

    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        if (trainingData != null) {
            trainingData.clear();
        }

        if (learnedPatterns != null) {
            learnedPatterns.clear();
        }

        Log.d(TAG, "PatternLearningEngine cleaned up");
    }

    // Data classes
    public static class TrainingExample {
        public String gestureName;
        public List<Point> points;
        public long timestamp;
        public long duration;
    }

    public static class ProcessedPattern {
        public String gestureName;
        public List<Point> normalizedPoints;
        public List<Float> velocities;
        public List<Float> curvatures;
        public android.graphics.Rect boundingBox;
    }

    public static class FeatureVector {
        public String gestureName;
        public float[] features;
    }

    public static class ClassificationResult {
        public String gestureName;
        public float confidence;

        public ClassificationResult(String name, float conf) {
            this.gestureName = name;
            this.confidence = conf;
        }
    }

    public static class LearningStats {
        public int totalExamples;
        public int uniqueGestures;
        public float averageAccuracy;
        public boolean isTraining;
    }

    // Simple k-NN classifier
    private static class GestureClassifier {
        private List<FeatureVector> trainingVectors;
        private float averageAccuracy = 0f;

        public GestureClassifier() {
            this.trainingVectors = new ArrayList<>();
        }

        public void train(List<FeatureVector> vectors) {
            this.trainingVectors = new ArrayList<>(vectors);
        }

        public ClassificationResult classify(FeatureVector input) {
            if (trainingVectors.isEmpty()) {
                return new ClassificationResult("unknown", 0f);
            }

            float minDistance = Float.MAX_VALUE;
            String bestMatch = "unknown";

            for (FeatureVector vector : trainingVectors) {
                float distance = calculateDistance(input.features, vector.features);
                if (distance < minDistance) {
                    minDistance = distance;
                    bestMatch = vector.gestureName;
                }
            }

            // Convert distance to confidence (higher distance = lower confidence)
            float confidence = Math.max(0f, 1f - (minDistance / 100f));
            return new ClassificationResult(bestMatch, confidence);
        }

        private float calculateDistance(float[] features1, float[] features2) {
            if (features1.length != features2.length) return Float.MAX_VALUE;

            float sum = 0f;
            for (int i = 0; i < features1.length; i++) {
                float diff = features1[i] - features2[i];
                sum += diff * diff;
            }
            return (float) Math.sqrt(sum);
        }

        public float getAverageAccuracy() {
            return averageAccuracy;
        }

        public void optimize() {
            // Simple optimization - normalize feature vectors
            for (FeatureVector vector : trainingVectors) {
                normalizeFeatures(vector.features);
            }
        }

        private void normalizeFeatures(float[] features) {
            float sum = 0f;
            for (float f : features) {
                sum += f * f;
            }
            float norm = (float) Math.sqrt(sum);
            if (norm > 0) {
                for (int i = 0; i < features.length; i++) {
                    features[i] /= norm;
                }
            }
        }
    }
}

/**
 * Clean up resources to prevent memory leaks
 */
public void cleanup() {
    isDestroyed = true;
    isTraining = false;

    if (executorService != null && !executorService.isShutdown()) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    synchronized (trainingDataLock) {
        if (trainingData != null) {
            trainingData.clear();
            trainingData = null;
        }
        if (learnedPatterns != null) {
            learnedPatterns.clear();
            learnedPatterns = null;
        }
    }

    if (mainHandler != null) {
        mainHandler.removeCallbacksAndMessages(null);
        mainHandler = null;
    }

    if (classifier != null) {
        classifier = null;
    }

    Log.d(TAG, "PatternLearningEngine cleaned up with proper memory management");
}
}
        }

/**
 * Add training example with bounds checking
 */
public void addTrainingExample(TrainingExample example) {
    if (isDestroyed) return;

    synchronized (trainingDataLock) {
        if (trainingData.size() >= MAX_TRAINING_EXAMPLES) {
            // Remove oldest example to maintain bounds
            trainingData.remove(0);
            Log.d(TAG, "Removed oldest training example to maintain memory bounds");
        }
        trainingData.add(example);
    }
}

/**
 * Clear training data to free memory
 */
public void clearTrainingData() {
    synchronized (trainingDataLock) {
        if (trainingData != null) {
            trainingData.clear();
            Log.d(TAG, "Training data cleared");
        }
    }
}

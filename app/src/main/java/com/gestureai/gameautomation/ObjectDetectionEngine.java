package com.gestureai.gameautomation;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.gestureai.gameautomation.ai.ZoneTracker;
import android.graphics.PointF;
import org.opencv.core.Core;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import com.gestureai.gameautomation.GameAction;
import com.gestureai.gameautomation.OCREngine;
import com.gestureai.gameautomation.TensorFlowLiteHelper;
import com.gestureai.gameautomation.utils.OpenCVHelper;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Rect;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.DetectedObject;
import com.gestureai.gameautomation.PlayerTracker.PlayerData;
import com.gestureai.gameautomation.ObjectLabelerEngine;
import com.gestureai.gameautomation.GameObjectTemplate;
import com.gestureai.gameautomation.PlayerTracker.PlayerData;
import com.gestureai.gameautomation.ai.PatternLearningEngine;
import com.gestureai.gameautomation.ai.ResourceMonitor;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.indexing.NDArrayIndex;
import com.gestureai.gameautomation.ai.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.tensorflow.lite.Interpreter;

public class ObjectDetectionEngine {
    private static final String TAG = "ObjectDetectionEngine";
    
    // OpenCV static initialization
    static {
        try {
            if (!org.opencv.android.OpenCVLoaderCallback.SUCCESS) {
                // Try static initialization first
                System.loadLibrary("opencv_java4");
                Log.d(TAG, "OpenCV loaded via static linking");
            }
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "OpenCV static loading failed, will use fallback: " + e.getMessage());
        }
    }

    private Context context;
    private NLPProcessor nlpProcessor;
    private Map<String, GameObjectTemplate> objectTemplates;
    private boolean isInitialized = false;

    // AI Strategy Components
    private GameStrategyAgent strategyAgent;
    private PatternLearningEngine patternLearner;
    private GameStrategyAgent.UniversalGameState currentGameState;
    private boolean rlIntegrationEnabled = true;

    // TensorFlow Lite Integration (single declaration)
    private Interpreter tfliteInterpreter;
    private ByteBuffer inputBuffer;
    private float[][][] tfliteOutput;

    // AI Component Integration
    private PlayerTracker playerTracker;
    private GameContextAnalyzer contextAnalyzer;
    private GameTypeDetector gameTypeDetector;
    private WeaponRecognizer weaponRecognizer;
    private TeamClassifier teamClassifier;
    private MinimapAnalyzer minimapAnalyzer;
    private MultiPlayerStrategy multiPlayerStrategy;
    private MOBAStrategy mobaStrategy;
    private FPSStrategy fpsStrategy;
    private ResourceMonitor resourceMonitor;
    private ZoneTracker zoneTracker;
    // Add this field declaration with other private fields
    // Use ObjectLabelerEngine's TrainingExample instead of creating a new one
    private List<ObjectLabelerEngine.TrainingExample> trainingExamples;

    // ND4J support
    private boolean nd4jEnabled = true;

    public void loadCustomTFLiteModel(String modelPath) {
        try {
            // Check if model file exists in assets
            if (!modelFileExists(modelPath)) {
                Log.w(TAG, "Model file not found: " + modelPath + ". Using fallback detection.");
                return;
            }
            
            tfliteInterpreter = new Interpreter(loadModelFile(modelPath));
            inputBuffer = ByteBuffer.allocateDirect(224 * 224 * 3 * 4); // RGBA
            inputBuffer.order(ByteOrder.nativeOrder());
            tfliteOutput = new float[1][100][6]; // max 100 detections, 6 values each
            Log.d(TAG, "TensorFlow Lite model loaded: " + modelPath);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load TFLite model: " + e.getMessage() + ". Using fallback detection.");
            tfliteInterpreter = null; // Ensure null for fallback detection
        }
    }

    private boolean modelFileExists(String modelPath) {
        try {
            InputStream is = context.getAssets().open(modelPath);
            is.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private java.nio.MappedByteBuffer loadModelFile(String modelPath) throws IOException {
        try (InputStream is = context.getAssets().open(modelPath);
             java.io.FileInputStream fis = new java.io.FileInputStream(is.getFD())) {
            java.nio.channels.FileChannel fileChannel = fis.getChannel();
            long startOffset = is.available();
            long declaredLength = is.available();
            return fileChannel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    public List<DetectedObject> detectWithTFLite(Bitmap frame) {
        List<DetectedObject> results = new ArrayList<>();
        if (tfliteInterpreter == null) {
            // Use fallback OpenCV-based detection when TFLite model is unavailable
            return detectWithOpenCVFallback(frame);
        }

        try {
            // Preprocess bitmap for TFLite
            Bitmap resized = Bitmap.createScaledBitmap(frame, 224, 224, true);
            inputBuffer.rewind();

            // Convert to ByteBuffer
            for (int y = 0; y < 224; y++) {
                for (int x = 0; x < 224; x++) {
                    int pixel = resized.getPixel(x, y);
                    inputBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f); // R
                    inputBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);  // G
                    inputBuffer.putFloat((pixel & 0xFF) / 255.0f);         // B
                }
            }

            // Run inference
            tfliteInterpreter.run(inputBuffer, tfliteOutput);

            // Parse results
            for (int i = 0; i < tfliteOutput[0].length; i++) {
                float confidence = tfliteOutput[0][i][4];
                if (confidence > 0.5f) {
                    int x = (int) (tfliteOutput[0][i][0] * frame.getWidth());
                    int y = (int) (tfliteOutput[0][i][1] * frame.getHeight());
                    int w = (int) (tfliteOutput[0][i][2] * frame.getWidth());
                    int h = (int) (tfliteOutput[0][i][3] * frame.getHeight());

                    org.opencv.core.Rect rect = new org.opencv.core.Rect(x, y, w, h);
                    String className = getClassNameFromIndex((int) tfliteOutput[0][i][5]);

                    results.add(new DetectedObject(className,
                            convertOpenCVToAndroidRect(rect),
                            confidence,
                            getActionForClass(className),
                            "TFLite detection"));
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "TFLite inference failed", e);
        }

        return results;
    }

    // Fallback detection using OpenCV when TensorFlow Lite model isn't available
    private List<DetectedObject> detectWithOpenCVFallback(Bitmap frame) {
        List<DetectedObject> results = new ArrayList<>();
        
        try {
            Mat mat = new Mat();
            Utils.bitmapToMat(frame, mat);
            
            // Use basic OpenCV feature detection for common game objects
            detectGameCoins(mat, results);
            detectGameObstacles(mat, results);
            detectGamePowerups(mat, results);
            
        } catch (Exception e) {
            Log.e(TAG, "OpenCV fallback detection failed", e);
        }
        
        return results;
    }

    private void detectGameCoins(Mat frame, List<DetectedObject> results) {
        // Basic color-based detection for yellow/gold coins
        Mat hsv = new Mat();
        Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_BGR2HSV);
        
        Mat mask = new Mat();
        Core.inRange(hsv, new Scalar(20, 100, 100), new Scalar(30, 255, 255), mask);
        
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            if (rect.area() > 50 && rect.area() < 2000) { // Reasonable coin size
                results.add(new DetectedObject("coin", 
                    convertOpenCVToAndroidRect(rect), 
                    0.7f, 
                    "collect", 
                    "OpenCV fallback"));
            }
        }
    }

    private void detectGameObstacles(Mat frame, List<DetectedObject> results) {
        // Edge detection for obstacles
        Mat gray = new Mat();
        Mat edges = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(gray, edges, 50, 150);
        
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            if (rect.area() > 1000) { // Large objects likely obstacles
                results.add(new DetectedObject("obstacle", 
                    convertOpenCVToAndroidRect(rect), 
                    0.6f, 
                    "avoid", 
                    "OpenCV fallback"));
            }
        }
    }

    private void detectGamePowerups(Mat frame, List<DetectedObject> results) {
        // Bright color detection for power-ups
        Mat hsv = new Mat();
        Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_BGR2HSV);
        
        // Detect bright/saturated colors
        Mat mask = new Mat();
        Core.inRange(hsv, new Scalar(0, 150, 150), new Scalar(180, 255, 255), mask);
        
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            if (rect.area() > 100 && rect.area() < 1000) {
                results.add(new DetectedObject("powerup", 
                    convertOpenCVToAndroidRect(rect), 
                    0.5f, 
                    "collect", 
                    "OpenCV fallback"));
            }
        }
    }

    public PlayerData[] trackMultiplePlayers(List<DetectedObject> players) {
        List<PlayerData> tracked = new ArrayList<>();

        for (DetectedObject player : players) {
            if (player.name.contains("player") || player.name.contains("character")) {
                PlayerData data = new PlayerData();
                data.playerId = generatePlayerId(player);
                data.position = new float[]{
                        player.boundingRect.x + player.boundingRect.width / 2f,
                        player.boundingRect.y + player.boundingRect.height / 2f
                };
                data.boundingBox = convertToAndroidRect(player.boundingRect);
                data.confidence = player.confidence;
                data.teamStatus = classifyPlayerTeam(player);
                data.lastSeen = System.currentTimeMillis();
                tracked.add(data);
            }
        }

        return tracked.toArray(new PlayerData[0]);
    }


    public String classifyPlayerTeam(DetectedObject player) {
        // Use ND4J for fast color analysis
        try {
            if (nd4jEnabled) {
                // Extract dominant colors from player region
                INDArray colorData = extractPlayerColors(player);

                // Common team color patterns
                INDArray blueTeam = Nd4j.create(new float[]{0.2f, 0.4f, 0.8f}); // Blue
                INDArray redTeam = Nd4j.create(new float[]{0.8f, 0.2f, 0.2f});  // Red
                INDArray greenTeam = Nd4j.create(new float[]{0.2f, 0.8f, 0.2f}); // Green

                // Calculate color distances
                double blueDistance = colorData.distance2(blueTeam);
                double redDistance = colorData.distance2(redTeam);
                double greenDistance = colorData.distance2(greenTeam);

                if (blueDistance < 0.3) return "teammate";
                if (redDistance < 0.3) return "enemy";
                if (greenDistance < 0.3) return "neutral";
            }
        } catch (Exception e) {
            Log.w(TAG, "Team classification failed", e);
        }

        return "unknown";
    }

    // Subway Surfers specific object detection thresholds//
    private static final float COIN_CONFIDENCE_THRESHOLD = 0.7f;
    private static final float OBSTACLE_CONFIDENCE_THRESHOLD = 0.8f;
    private static final float POWERUP_CONFIDENCE_THRESHOLD = 0.75f;

    public ObjectDetectionEngine(Context context) {
        this.context = context;
        this.nlpProcessor = new NLPProcessor(context);
        this.objectTemplates = new HashMap<>();
        initialize();
    }

    private void initialize() {
        try {
            loadDefaultTemplates();
            loadCustomTemplates();

            // Initialize AI learning components
            strategyAgent = new GameStrategyAgent(context);
            patternLearner = new PatternLearningEngine(context);
            currentGameState = new GameStrategyAgent.UniversalGameState();

            // Initialize all AI components
            playerTracker = new PlayerTracker(context);
            contextAnalyzer = new GameContextAnalyzer(new OCREngine(context), playerTracker);
            gameTypeDetector = new GameTypeDetector(context);
            weaponRecognizer = new WeaponRecognizer(context, new TensorFlowLiteHelper(context), new OCREngine(context));
            teamClassifier = new TeamClassifier(context, new TensorFlowLiteHelper(context));
            minimapAnalyzer = new MinimapAnalyzer(context, new TensorFlowLiteHelper(context));
            multiPlayerStrategy = new MultiPlayerStrategy(context);
            mobaStrategy = new MOBAStrategy(context);
            fpsStrategy = new FPSStrategy(context);
            resourceMonitor = new ResourceMonitor(context);
            zoneTracker = new ZoneTracker(context);
            // ADD THIS LINE HERE:
            this.trainingExamples = new ArrayList<>();

            if (rlIntegrationEnabled) {
                Log.d(TAG, "RL integration enabled for ObjectDetectionEngine");
            }

            isInitialized = true;
            Log.d(TAG, "Object Detection Engine with full AI integration initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Object Detection Engine", e);
        }
    }

    private void loadDefaultTemplates() {
        // Default Subway Surfers object templates
        addObjectTemplate("coin", new GameObjectTemplate(
                "coin",
                "COLLECT",
                "Collect coins for points",
                new int[]{20, 100, 100}, // HSV lower bound (yellow)
                new int[]{30, 255, 255}, // HSV upper bound
                15, 25, // Min/max size
                COIN_CONFIDENCE_THRESHOLD
        ));

        addObjectTemplate("train", new GameObjectTemplate(
                "train",
                "AVOID",
                "Jump or slide to avoid trains",
                new int[]{0, 0, 0}, // Dark objects
                new int[]{180, 50, 50},
                100, 300, // Larger objects
                OBSTACLE_CONFIDENCE_THRESHOLD
        ));

        addObjectTemplate("barrier", new GameObjectTemplate(
                "barrier",
                "AVOID",
                "Jump over barriers",
                new int[]{0, 0, 100}, // Light colored barriers
                new int[]{180, 50, 200},
                50, 150,
                OBSTACLE_CONFIDENCE_THRESHOLD
        ));

        addObjectTemplate("jetpack", new GameObjectTemplate(
                "jetpack",
                "ACTIVATE_POWERUP",
                "Activate jetpack power-up",
                new int[]{100, 150, 150}, // Blue/metallic
                new int[]{120, 255, 255},
                20, 40,
                POWERUP_CONFIDENCE_THRESHOLD
        ));

        addObjectTemplate("magnet", new GameObjectTemplate(
                "magnet",
                "ACTIVATE_POWERUP",
                "Activate coin magnet",
                new int[]{160, 100, 100}, // Red/pink
                new int[]{180, 255, 255},
                15, 30,
                POWERUP_CONFIDENCE_THRESHOLD
        ));
    }

    private void loadCustomTemplates() {
        try {
            String customTemplatesJson = loadAssetFile("custom_object_templates.json");
            if (customTemplatesJson != null) {
                JSONObject json = new JSONObject(customTemplatesJson);
                JSONArray templates = json.getJSONArray("templates");

                for (int i = 0; i < templates.length(); i++) {
                    JSONObject template = templates.getJSONObject(i);
                    GameObjectTemplate objectTemplate = parseTemplateFromJson(template);
                    if (objectTemplate != null) {
                        addObjectTemplate(objectTemplate.name, objectTemplate);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Custom templates not found or invalid, using defaults only");
        }
    }
    private String loadAssetFile(String filename) {
        try {
            InputStream inputStream = context.getAssets().open(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }

    private GameObjectTemplate parseTemplateFromJson(JSONObject json) throws JSONException {
        String name = json.getString("name");
        String defaultAction = json.getString("default_action");
        String description = json.getString("description");


        JSONArray lowerBound = json.getJSONArray("hsv_lower");
        JSONArray upperBound = json.getJSONArray("hsv_upper");

        int[] lower = new int[]{lowerBound.getInt(0), lowerBound.getInt(1), lowerBound.getInt(2)};
        int[] upper = new int[]{upperBound.getInt(0), upperBound.getInt(1), upperBound.getInt(2)};

        int minSize = json.getInt("min_size");
        int maxSize = json.getInt("max_size");
        float confidence = (float) json.getDouble("confidence_threshold");

        return new GameObjectTemplate(name, defaultAction, description, lower, upper, minSize, maxSize, confidence);
    }


    public void addObjectTemplate(String name, GameObjectTemplate template) {
        objectTemplates.put(name, template);
        Log.d(TAG, "Added object template: " + name);
    }

    public List<DetectedObject> detectObjects(Bitmap gameScreen) {
        List<DetectedObject> detectedObjects = new ArrayList<>();
        try {
            Mat screenMat = new Mat();
            Utils.bitmapToMat(gameScreen, screenMat);

            // Convert to HSV for better color detection
            Mat hsvMat = new Mat();
            Imgproc.cvtColor(screenMat, hsvMat, Imgproc.COLOR_RGB2HSV);

            // Step 1: Traditional object detection
            for (GameObjectTemplate template : objectTemplates.values()) {
                List<DetectedObject> objects = detectObjectType(hsvMat, template);
                detectedObjects.addAll(objects);
            }

            // Step 2: AI-Enhanced Pattern Learning
            List<DetectedObject> learnedObjects = patternLearner.detectLearnedPatterns(gameScreen);
            detectedObjects.addAll(learnedObjects);

            // Step 3: Update game state for AI learning
            updateGameStateFromDetection(detectedObjects, gameScreen);

            // Step 4: AI-Enhanced Object Labeling
            enhanceObjectLabeling(gameScreen, detectedObjects);

            // Step 5: Learn from detection results
            learnFromDetectionResults(detectedObjects);

            Log.d(TAG, "Detected " + detectedObjects.size() + " objects (AI-enhanced)");

        } catch (Exception e) {
            Log.e(TAG, "Error during AI-enhanced object detection", e);
        }

        return detectedObjects;
    }


    private List<DetectedObject> detectObjectType(Mat hsvImage, GameObjectTemplate template) {
        List<DetectedObject> objects = new ArrayList<>();

        try {
            Mat mask = new Mat();
            org.opencv.core.Scalar lowerBound = new org.opencv.core.Scalar(
                    template.hsvLower[0], template.hsvLower[1], template.hsvLower[2]);
            org.opencv.core.Scalar upperBound = new org.opencv.core.Scalar(
                    template.hsvUpper[0], template.hsvUpper[1], template.hsvUpper[2]);

            // Create mask for target color range
            org.opencv.core.Core.inRange(hsvImage, lowerBound, upperBound, mask);

            // Find contours
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // Process each contour
            for (MatOfPoint contour : contours) {
                org.opencv.core.Rect boundingRect = Imgproc.boundingRect(contour);

                // Filter by size
                int size = Math.max(boundingRect.width, boundingRect.height);
                if (size >= template.minSize && size <= template.maxSize) {

                    // Calculate confidence based on size, position, and color match
                    float confidence = calculateConfidence(boundingRect, template, hsvImage);

                    if (confidence >= template.confidenceThreshold) {
                        DetectedObject detectedObject = new DetectedObject(
                                template.name,
                                boundingRect,
                                confidence,
                                template.defaultAction,
                                template.description
                        );

                        objects.add(detectedObject);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error detecting " + template.name, e);
        }

        return objects;
    }

    private float calculateConfidence(org.opencv.core.Rect boundingRect, GameObjectTemplate template, Mat hsvImage) {
        if (nd4jEnabled) {
            return calculateConfidenceND4J(boundingRect, template, hsvImage);
        }
        // Fallback to original method
        return calculateConfidenceOriginal(boundingRect, template, hsvImage);
    }

    private float calculateConfidenceND4J(org.opencv.core.Rect boundingRect, GameObjectTemplate template, Mat hsvImage) {
        try {
            // Extract ROI using OpenCV to ND4J conversion
            Mat roi = new Mat(hsvImage, boundingRect);

            // Convert to ND4J for vectorized operations
            int[] roiData = new int[roi.rows() * roi.cols() * 3];
            roi.get(0, 0, roiData);

            INDArray roiArray = Nd4j.createFromArray(roiData)
                    .reshape(roi.rows(), roi.cols(), 3);

            // Vectorized confidence calculations
            INDArray confidenceComponents = Nd4j.zeros(5);

            // 1. Size confidence
            int size = Math.max(boundingRect.width, boundingRect.height);
            int expectedSize = (template.minSize + template.maxSize) / 2;
            float sizeRatio = Math.min(size, expectedSize) / (float) Math.max(size, expectedSize);
            confidenceComponents.putScalar(0, 0.3f + 0.4f * sizeRatio);

            // 2. Color match confidence using ND4J operations
            INDArray targetColor = Nd4j.create(new float[]{
                    (template.hsvLower[0] + template.hsvUpper[0]) / 2f,
                    (template.hsvLower[1] + template.hsvUpper[1]) / 2f,
                    (template.hsvLower[2] + template.hsvUpper[2]) / 2f
            });

            // Calculate mean color of ROI
            INDArray meanColor = roiArray.mean(0, 1);
            double colorDistance = meanColor.distance2(targetColor);
            float colorMatch = Math.max(0, 1.0f - (float) (colorDistance / 100.0));
            confidenceComponents.putScalar(1, 0.4f * colorMatch);

            // 3. Position confidence (objects more likely in certain screen areas)
            float centerX = boundingRect.x + boundingRect.width / 2f;
            float centerY = boundingRect.y + boundingRect.height / 2f;
            float positionWeight = calculatePositionWeight(centerX, centerY, template.name);
            confidenceComponents.putScalar(2, 0.1f * positionWeight);

            // 4. Shape confidence
            float aspectRatio = (float) boundingRect.width / boundingRect.height;
            float shapeScore = calculateShapeScore(aspectRatio, template.name);
            confidenceComponents.putScalar(3, 0.1f * shapeScore);

            // 5. Temporal consistency (if tracking previous detections)
            float temporalScore = calculateTemporalConsistency(boundingRect, template.name);
            confidenceComponents.putScalar(4, 0.1f * temporalScore);

            // Sum all confidence components
            return confidenceComponents.sumNumber().floatValue();

        } catch (Exception e) {
            Log.w(TAG, "ND4J confidence calculation failed, using fallback", e);
            return calculateConfidenceOriginal(boundingRect, template, hsvImage);
        }
    }

    // Enhanced helper methods
    private float calculatePositionWeight(float x, float y, String objectType) {
        // Different objects appear in different screen regions
        switch (objectType) {
            case "coin":
                return 1.0f; // Coins can appear anywhere
            case "train":
                return y > 600 ? 1.0f : 0.5f; // Trains usually in lower part
            case "barrier":
                return y > 500 ? 1.0f : 0.3f; // Barriers on ground
            case "jetpack":
            case "magnet":
                return y < 800 ? 1.0f : 0.4f; // Power-ups usually higher up
            default:
                return 0.7f;
        }
    }

    private float calculateShapeScore(float aspectRatio, String objectType) {
        switch (objectType) {
            case "coin":
                return Math.abs(aspectRatio - 1.0f) < 0.3f ? 1.0f : 0.5f; // Coins are circular
            case "train":
                return aspectRatio > 1.5f ? 1.0f : 0.3f; // Trains are wide
            case "barrier":
                return aspectRatio < 0.8f ? 1.0f : 0.4f; // Barriers are tall
            default:
                return 0.7f;
        }
    }

    private float calculateTemporalConsistency(Rect currentRect, String objectType) {
        // Check if similar object was detected in nearby position recently
        // This would use a temporal buffer of previous detections
        return 0.8f; // Placeholder for temporal tracking
    }

    private INDArray extractPlayerColors(DetectedObject player) {
        // Extract dominant RGB colors from player bounding box
        // This would analyze the actual pixel data
        return Nd4j.create(new float[]{0.5f, 0.5f, 0.5f}); // Placeholder
    }

    // Supporting classes
//    public PlayerData[] trackMultiplePlayers(List<DetectedObject> players) {
//        List<PlayerData> tracked = new ArrayList<>();
//
//        for (DetectedObject player : players) {
//            if (player.name.contains("player") || player.name.contains("character")) {
//                PlayerData data = new PlayerData(
//                        generatePlayerId(player),
//                        convertToAndroidRect(player.boundingRect),
//                        player.confidence
//                );
//                data.teamStatus = classifyPlayerTeam(player);
//                tracked.add(data);
//            }
//        }
//        return tracked.toArray(new PlayerData[0]);
//    }

    private int generatePlayerId(DetectedObject player) {
        return player.hashCode();
    }

    private android.graphics.Rect convertToAndroidRect(org.opencv.core.Rect cvRect) {
        return new android.graphics.Rect(
                cvRect.x, cvRect.y,
                cvRect.x + cvRect.width,
                cvRect.y + cvRect.height
        );
    }

    public List<GameAction> processDetectedObjects(List<DetectedObject> detectedObjects) {
        List<GameAction> actions = new ArrayList<>();

        // Sort objects by priority (obstacles first, then power-ups, then coins)
        detectedObjects.sort((o1, o2) -> {
            int priority1 = getObjectPriority(o1.name);
            int priority2 = getObjectPriority(o2.name);
            return Integer.compare(priority2, priority1); // Higher priority first
        });

        for (DetectedObject object : detectedObjects) {
            GameAction action = createActionForObject(object);
            if (action != null) {
                actions.add(action);
            }
        }

        return actions;
    }

    private int getObjectPriority(String objectName) {
        switch (objectName.toLowerCase()) {
            case "train":
            case "barrier":
            case "obstacle":
                return 10; // Highest priority - must avoid
            case "jetpack":
            case "magnet":
            case "powerup":
                return 5; // Medium priority - beneficial to collect
            case "coin":
                return 1; // Low priority - collect if safe
            default:
                return 0;
        }
    }

    private GameAction createActionForObject(DetectedObject object) {
        int centerX = object.boundingRect.centerX();
        int centerY = object.boundingRect.centerY();
        
        return new GameAction(
            object.action,
            centerX,
            centerY,
            object.confidence,
            "object_detection"
        );
    }

    private float calculateConfidenceOriginal(org.opencv.core.Rect boundingRect, GameObjectTemplate template, Mat hsvImage) {
        float confidence = 0.5f;
        
        // Size confidence
        int size = Math.max(boundingRect.width, boundingRect.height);
        int expectedSize = (template.minSize + template.maxSize) / 2;
        float sizeRatio = Math.min(size, expectedSize) / (float) Math.max(size, expectedSize);
        confidence += 0.3f * sizeRatio;
        
        // Position confidence
        float centerX = boundingRect.x + boundingRect.width / 2f;
        float centerY = boundingRect.y + boundingRect.height / 2f;
        confidence += 0.2f * calculatePositionWeight(centerX, centerY, template.name);
        
        return Math.min(1.0f, confidence);
    }

    private void updateGameStateFromDetection(List<DetectedObject> detectedObjects, Bitmap gameScreen) {
        if (currentGameState == null) return;
        
        currentGameState.screenWidth = gameScreen.getWidth();
        currentGameState.screenHeight = gameScreen.getHeight();
        currentGameState.objectCount = detectedObjects.size();
        
        // Calculate threat and opportunity levels
        float threatLevel = 0f;
        float opportunityLevel = 0f;
        
        for (DetectedObject obj : detectedObjects) {
            if (obj.action.equals("AVOID")) {
                threatLevel += obj.confidence;
            } else if (obj.action.equals("COLLECT") || obj.action.equals("ACTIVATE_POWERUP")) {
                opportunityLevel += obj.confidence;
            }
        }
        
        currentGameState.threatLevel = Math.min(1.0f, threatLevel / Math.max(1, detectedObjects.size()));
        currentGameState.opportunityLevel = Math.min(1.0f, opportunityLevel / Math.max(1, detectedObjects.size()));
    }

    private void enhanceObjectLabeling(Bitmap gameScreen, List<DetectedObject> detectedObjects) {
        try {
            // Use AI to improve object classification
            if (strategyAgent != null) {
                for (DetectedObject obj : detectedObjects) {
                    // Enhance confidence based on game context
                    float enhancedConfidence = strategyAgent.enhanceObjectConfidence(obj, currentGameState);
                    obj.confidence = Math.max(obj.confidence, enhancedConfidence);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Object labeling enhancement failed", e);
        }
    }

    private void learnFromDetectionResults(List<DetectedObject> detectedObjects) {
        if (patternLearner != null && rlIntegrationEnabled) {
            patternLearner.learnFromDetections(detectedObjects);
        }
    }

    private String getClassNameFromIndex(int classIndex) {
        String[] classNames = {"coin", "train", "barrier", "jetpack", "magnet", "obstacle", "powerup", "unknown"};
        return classIndex >= 0 && classIndex < classNames.length ? classNames[classIndex] : "unknown";
    }

    private String getActionForClass(String className) {
        switch (className.toLowerCase()) {
            case "coin":
                return "COLLECT";
            case "train":
            case "barrier":
            case "obstacle":
                return "AVOID";
            case "jetpack":
            case "magnet":
            case "powerup":
                return "ACTIVATE_POWERUP";
            default:
                return "WAIT";
        }
    }

    private android.graphics.Rect convertOpenCVToAndroidRect(org.opencv.core.Rect cvRect) {
        return new android.graphics.Rect(
            cvRect.x, cvRect.y,
            cvRect.x + cvRect.width,
            cvRect.y + cvRect.height
        );
    }

    private java.nio.ByteBuffer loadModelFile(String modelPath) throws IOException {
        InputStream inputStream = context.getAssets().open(modelPath);
        byte[] modelBytes = new byte[inputStream.available()];
        inputStream.read(modelBytes);
        inputStream.close();
        
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(modelBytes.length);
        buffer.put(modelBytes);
        return buffer;
    }

    public void addCustomObjectFromScreenshot(String objectName, String actionLogic) {
        try {
            // Create training example for the pattern learner
            ObjectLabelerEngine.TrainingExample example = new ObjectLabelerEngine.TrainingExample();
            example.objectName = objectName;
            example.actionType = actionLogic;
            example.timestamp = System.currentTimeMillis();
            
            trainingExamples.add(example);
            Log.d(TAG, "Added custom object training example: " + objectName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add custom object", e);
        }
    }
}

        hsvUpper[0] = Math.min(179, hsvMax[0] + 10);
        hsvUpper[1] = Math.min(255, hsvMax[1] + 50);
        hsvUpper[2] = Math.min(255, hsvMax[2] + 50);
    }

    private void updateGameStateFromDetection(List<DetectedObject> detectedObjects, Bitmap gameScreen) {
        // Update current game state for AI learning
        currentGameState.screenWidth = gameScreen.getWidth();
        currentGameState.screenHeight = gameScreen.getHeight();
        currentGameState.detectedObjects = detectedObjects;
        currentGameState.timestamp = System.currentTimeMillis();
    }

    private void enhanceObjectLabeling(Bitmap gameScreen, List<DetectedObject> detectedObjects) {
        // AI-enhanced object labeling
        for (DetectedObject obj : detectedObjects) {
            try {
                // Learn from this detection for future improvements
                patternLearner.learnObjectPattern(obj, gameScreen);
            } catch (Exception e) {
                Log.w(TAG, "Error in pattern learning", e);
            }
        }
    }

    private void learnFromDetectionResults(List<DetectedObject> detectedObjects) {
        if (rlIntegrationEnabled && gameStrategyAgent != null) {
            try {
                // Send detection results to RL agent for learning
                gameStrategyAgent.updateGameState(currentGameState);
            } catch (Exception e) {
                Log.w(TAG, "Error updating RL agent", e);
            }
        }
    }

    // Inner classes at the end of ObjectDetectionEngine.java
    public static class DetectedObject {
        public String name;
        public Rect boundingRect;
        public float confidence;
        public String actionType;
        public String description;

        public DetectedObject(String name, Rect boundingRect, float confidence, String actionType, String description) {
            this.name = name;
            this.boundingRect = boundingRect;
            this.confidence = confidence;
            this.actionType = actionType;
            this.description = description;
        }
    }

    /**
     * AI-Enhanced Object Labeling - learns and improves over time
     */
    private void enhanceObjectLabeling(Bitmap gameScreen, List<DetectedObject> objects) {
        if (strategyAgent == null || objects.isEmpty()) return;

        try {
            // Get AI suggestion for optimal action
            GameAction suggestedAction = strategyAgent.selectOptimalAction(currentGameState);

            // Enhance object confidence based on AI learning
            for (DetectedObject obj : objects) {
                if (isObjectRelevantToAction(obj, suggestedAction)) {
                    // AI considers this object important - boost confidence
                    float originalConfidence = obj.confidence;
                    obj.confidence = Math.min(1.0f, obj.confidence * 1.3f);
                    Log.d(TAG, "AI boosted " + obj.name + " confidence: " +
                            originalConfidence + " → " + obj.confidence);
                }

                // Learn new object patterns
                if (obj.confidence > 0.9f) {
                    patternLearner.learnObjectPattern(obj, gameScreen);
                }
            }

            // Adapt object templates based on AI feedback
            adaptTemplatesFromAI(objects, suggestedAction);

        } catch (Exception e) {
            Log.e(TAG, "Error in AI-enhanced labeling", e);
        }
    }

    private boolean isObjectRelevantToAction(DetectedObject obj, GameAction action) {
        if (action == null) return false;

        // Calculate distance between object and action target
        // Instead of boundingRect.centerX() and boundingRect.centerY()
        float centerX = obj.boundingRect.x + (obj.boundingRect.width / 2.0f);
        float centerY = obj.boundingRect.y + (obj.boundingRect.height / 2.0f);

        // Objects within 200 pixels of action target are considered relevant
        return distance < 200;
    }

    private void updateGameStateFromDetection(List<DetectedObject> objects, Bitmap gameScreen) {
        currentGameState.screenWidth = gameScreen.getWidth();
        currentGameState.screenHeight = gameScreen.getHeight();
        currentGameState.objectCount = objects.size();

        // Calculate threat and opportunity levels from detected objects
        float threatLevel = 0f;
        float opportunityLevel = 0f;

        for (DetectedObject obj : objects) {
            if (obj.action.equals("AVOID")) {
                threatLevel += obj.confidence;
            } else if (obj.action.equals("COLLECT") || obj.action.equals("ACTIVATE_POWERUP")) {
                opportunityLevel += obj.confidence;
            }
        }

        currentGameState.threatLevel = Math.min(1.0f, threatLevel / objects.size());
        currentGameState.opportunityLevel = Math.min(1.0f, opportunityLevel / objects.size());
        currentGameState.timeInGame = System.currentTimeMillis() / 1000f;
    }

    private void learnFromDetectionResults(List<DetectedObject> objects) {
        // Provide feedback to AI based on detection success
        float detectionQuality = 0f;
        for (DetectedObject obj : objects) {
            detectionQuality += obj.confidence;
        }

        if (!objects.isEmpty()) {
            detectionQuality /= objects.size();

            // Create dummy previous state for learning (in real implementation,
            // you'd store the actual previous state)
            GameStrategyAgent.UniversalGameState previousState = new GameStrategyAgent.UniversalGameState();
            GameAction dummyAction = new GameAction("DETECT", 0, 0, detectionQuality, "detection");

            // Learn from detection quality as reward
            strategyAgent.learnFromExperience(previousState, dummyAction,
                    detectionQuality, currentGameState, false);
        }
    }

    private void adaptTemplatesFromAI(List<DetectedObject> objects, GameAction suggestedAction) {
        // Adapt object detection templates based on AI learning
        for (DetectedObject obj : objects) {
            GameObjectTemplate template = objectTemplates.get(obj.name);
            if (template != null && obj.confidence > 0.8f) {
                // AI successfully identified this object - slightly relax thresholds
                template.confidenceThreshold = Math.max(0.5f, template.confidenceThreshold * 0.98f);
            }
        }
    }

    private float calculateConfidenceOriginal(org.opencv.core.Rect boundingRect, GameObjectTemplate template, Mat hsvImage) {
        float confidence = 0.0f;

        // Size confidence
        int size = Math.max(boundingRect.width, boundingRect.height);
        int expectedSize = (template.minSize + template.maxSize) / 2;
        float sizeRatio = Math.min(size, expectedSize) / (float) Math.max(size, expectedSize);
        confidence += 0.5f + 0.3f * sizeRatio;

        // Position confidence
        float centerX = boundingRect.x + boundingRect.width / 2f;
        float imageWidth = hsvImage.cols();
        float centerRatio = 1.0f - Math.abs(centerX - imageWidth / 2f) / (imageWidth / 2f);
        confidence += 0.1f * centerRatio;

        // Basic color match confidence
        confidence += 0.1f;

        return Math.min(1.0f, confidence);
    }

    private void updateGameStateFromDetection(List<DetectedObject> detectedObjects, Bitmap gameScreen) {
        // Update current game state for AI learning
        currentGameState.screenWidth = gameScreen.getWidth();
        currentGameState.screenHeight = gameScreen.getHeight();
        currentGameState.detectedObjects = detectedObjects;
        currentGameState.timestamp = System.currentTimeMillis();
    }

    private void enhanceObjectLabeling(Bitmap gameScreen, List<DetectedObject> detectedObjects) {
        // AI-enhanced object labeling
        for (DetectedObject obj : detectedObjects) {
            try {
                // Learn from this detection for future improvements
                patternLearner.learnObjectPattern(obj, gameScreen);
            } catch (Exception e) {
                Log.w(TAG, "Error in pattern learning", e);
            }
        }
    }

    private void learnFromDetectionResults(List<DetectedObject> detectedObjects) {
        if (rlIntegrationEnabled && gameStrategyAgent != null) {
            try {
                // Send detection results to RL agent for learning
                gameStrategyAgent.updateGameState(currentGameState);
            } catch (Exception e) {
                Log.w(TAG, "Error updating RL agent", e);
            }
        }
    }

    public static class ActionTemplate {
        public String actionType;
        public String condition;
        public String nlpDescription;
        public String strategyExplanation; // Detailed strategy explanation
        public String decisionReasoning; // Why this action was chosen
        public String gameContext; // What game situation this applies to
        public String expectedOutcome; // What should happen after this action
        public Map<String, String> parameters;
        public float priority;
        public boolean isConditional;

        public ActionTemplate() {
            this.parameters = new HashMap<>();
            this.priority = 0.5f;
            this.isConditional = false;
            this.strategyExplanation = "";
            this.decisionReasoning = "";
            this.gameContext = "";
            this.expectedOutcome = "";
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("actionType", actionType);
            json.put("condition", condition);
            json.put("nlpDescription", nlpDescription);
            json.put("strategyExplanation", strategyExplanation);
            json.put("decisionReasoning", decisionReasoning);
            json.put("gameContext", gameContext);
            json.put("expectedOutcome", expectedOutcome);
            json.put("priority", priority);
            return json;
        }
    }

    private void initializeComponents() {
        try {
            // Existing components
            strategyProcessor = new StrategyProcessor(context);
            nlpProcessor = new NLPProcessor(context);
            ocrEngine = new OCREngine(context);
            objectDetectionEngine = new ObjectDetectionEngine(context);

            // Initialize RL agents
            dqnAgent = new DQNAgent(55);
            ppoAgent = new PPOAgent(55);

            // Initialize all AI components
            playerTracker = new PlayerTracker(context);
            contextAnalyzer = new GameContextAnalyzer(context);
            gameTypeDetector = new GameTypeDetector(context);
            weaponRecognizer = new WeaponRecognizer(context);
            teamClassifier = new TeamClassifier(context);
            minimapAnalyzer = new MinimapAnalyzer(context);
            multiPlayerStrategy = new MultiPlayerStrategy(context);
            mobaStrategy = new MOBAStrategy(context);
            fpsStrategy = new FPSStrategy(context);
            resourceMonitor = new ResourceMonitor(context);
            zoneTracker = new ZoneTracker(context);

            Log.d(TAG, "All AI components initialized for labeling integration");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing AI components", e);
        }
    }

    /**
     * Create ActionTemplate with detailed strategy explanation
     */
    public ActionTemplate createStrategyActionTemplate(String actionType, String strategy,
                                                       String reasoning, String context,
                                                       String outcome) {
        ActionTemplate template = new ActionTemplate();
        template.actionType = actionType;
        template.strategyExplanation = strategy;
        template.decisionReasoning = reasoning;
        template.gameContext = context;
        template.expectedOutcome = outcome;

        // Process with NLP for enhanced understanding
        if (nlpProcessor != null) {
            NLPProcessor.ActionIntent intent = nlpProcessor.processNaturalLanguageCommand(strategy);
            if (intent != null) {
                template.nlpDescription = intent.getAction();
                template.priority = intent.getConfidence();
            }
        }

        return template;
    }

    /**
     * Integrate labeled data with all AI components for training
     */
    public void integrateWithAllAIComponents(CustomLabeledObject labeledObject) {
        try {
            Bitmap objectImage = extractObjectRegion(labeledObject);

            // Player tracking integration
            if (labeledObject.category.contains("player") && playerTracker != null) {
                PlayerData playerData = new PlayerData();
                playerData.position = new float[]{
                        labeledObject.boundingBox.centerX(),
                        labeledObject.boundingBox.centerY()
                };
                playerData.confidence = labeledObject.confidence;
                playerData.teamStatus = labeledObject.metadata.getOrDefault("team", "unknown").toString();

                playerTracker.addTrainingData(playerData, labeledObject.actionTemplate);
                Log.d(TAG, "Player tracking data added: " + labeledObject.name);
            }

            // Weapon recognition integration
            if (labeledObject.category.contains("weapon") && weaponRecognizer != null) {
                WeaponRecognizer.WeaponData weaponData = new WeaponRecognizer.WeaponData();
                weaponData.weaponType = labeledObject.name;
                weaponData.region = labeledObject.boundingBox;
                weaponData.confidence = labeledObject.confidence;

                weaponRecognizer.addTrainingExample(objectImage, weaponData, labeledObject.actionTemplate);
                Log.d(TAG, "Weapon recognition data added: " + labeledObject.name);
            }

            // Team classification integration
            if (labeledObject.category.contains("team") && teamClassifier != null) {
                String teamLabel = labeledObject.metadata.getOrDefault("team_type", "neutral").toString();
                teamClassifier.addTrainingData(objectImage, teamLabel, labeledObject.actionTemplate);
                Log.d(TAG, "Team classification data added: " + teamLabel);
            }

            // Game type detection integration
            if (gameTypeDetector != null) {
                String gameType = labeledObject.metadata.getOrDefault("game_type", "unknown").toString();
                gameTypeDetector.addTrainingData(currentSession.originalImage, gameType, labeledObject);
                Log.d(TAG, "Game type detection data added: " + gameType);
            }

            // Minimap analysis integration
            if (labeledObject.category.contains("minimap") && minimapAnalyzer != null) {
                MinimapAnalyzer.MinimapData minimapData = new MinimapAnalyzer.MinimapData();
                minimapData.bounds = labeledObject.boundingBox;
                minimapData.elements = parseMinimapElements(labeledObject.metadata);

                minimapAnalyzer.addTrainingData(objectImage, minimapData, labeledObject.actionTemplate);
                Log.d(TAG, "Minimap analysis data added");
            }

            // Strategy components integration
            integrateWithStrategyComponents(labeledObject);

        } catch (Exception e) {
            Log.e(TAG, "Error integrating with AI components", e);
        }
    }

    private void integrateWithStrategyComponents(CustomLabeledObject labeledObject) {
        String gameType = labeledObject.metadata.getOrDefault("game_type", "unknown").toString();

        // Battle Royale strategy integration
        if (gameType.contains("battle_royale") && multiPlayerStrategy != null) {
            MultiPlayerStrategy.BattleRoyaleState brState = createBRStateFromLabel(labeledObject);
            multiPlayerStrategy.recordEngagementResult(
                    labeledObject.actionTemplate.expectedOutcome.contains("success"),
                    labeledObject.confidence,
                    Float.parseFloat(labeledObject.metadata.getOrDefault("zone_risk", "0.5").toString()),
                    labeledObject.actionTemplate.actionType
            );
        }

        // MOBA strategy integration
        if (gameType.contains("moba") && mobaStrategy != null) {
            MOBAStrategy.MOBAState mobaState = createMOBAStateFromLabel(labeledObject);
            mobaStrategy.recordGameResult(
                    labeledObject.actionTemplate.expectedOutcome.contains("win"),
                    mobaState
            );
        }

        // FPS strategy integration
        if (gameType.contains("fps") && fpsStrategy != null) {
            FPSStrategy.WeaponType weapon = parseWeaponType(labeledObject.metadata);
            FPSStrategy.CombatRange range = parseCombatRange(labeledObject.metadata);
            fpsStrategy.recordCombatResult(
                    labeledObject.actionTemplate.expectedOutcome.contains("kill"),
                    weapon,
                    range,
                    Integer.parseInt(labeledObject.metadata.getOrDefault("shots_fired", "1").toString()),
                    labeledObject.metadata.containsKey("had_cover")
            );
        }
    }

    // Battle Royale Strategy Examples
    ActionTemplate brEngageTemplate = createStrategyActionTemplate(
            "ENGAGE_ENEMY",
            "Aggressive engagement strategy: Attack when enemy is isolated and we have positional advantage. Use cover-to-cover approach to minimize exposure while maintaining pressure.",
            "Enemy is alone, low on health, and we have high ground advantage. Our weapon has superior range and we have full health/shield.",
            "Battle Royale mid-game with 45 players remaining. Enemy rotating late to zone and caught in open ground.",
            "Eliminate enemy player, loot their items, maintain position for third-party opportunities."
    );

    // MOBA Strategy Examples
    ActionTemplate mobaGankTemplate = createStrategyActionTemplate(
            "GANK_LANE",
            "Coordinated gank strategy: Time ability usage with teammate engage. Focus enemy carry, use crowd control chain, secure kill before enemy support arrives.",
            "Enemy ADC is overextended, no vision on jungle, our abilities are ready, teammate has engage potential.",
            "MOBA bot lane at 8 minutes, enemy pushed to our tower, jungler has ultimate ready.",
            "Secure kill on enemy carry, gain gold advantage, control dragon objective."
    );

    // FPS Strategy Examples
    ActionTemplate fpsFlankTemplate = createStrategyActionTemplate(
            "FLANK_POSITION",
            "Flanking maneuver: Use alternate route to attack from unexpected angle. Coordinate timing with team distraction, target enemy sniper first.",
            "Enemies focused on main choke point, alternate path is clear, we have smoke grenades for cover.",
            "FPS team deathmatch, enemies camping high ground position, team needs breakthrough.",
            "Disrupt enemy formation, eliminate key targets, open path for team advance."
    );

    public CustomLabeledObject createCustomLabeledObjectWithStrategy(String name, String category,
                                                                     List<PointF> polygonPoints,
                                                                     String actionLogic,
                                                                     String strategy,
                                                                     String reasoning,
                                                                     String context,
                                                                     String outcome) {
        CustomLabeledObject labeledObject = createCustomLabeledObject(name, category, polygonPoints, actionLogic);

        // Enhanced action template with strategy
        ActionTemplate strategyTemplate = createStrategyActionTemplate(
                labeledObject.actionTemplate.actionType,
                strategy,
                reasoning,
                context,
                outcome
        );

        labeledObject.actionTemplate = strategyTemplate;

        // Integrate with all AI components
        integrateWithAllAIComponents(labeledObject);

        return labeledObject;
    }

    // Create detailed strategy-based labels
    CustomLabeledObject enemyPlayer = objectLabeler.createCustomLabeledObjectWithStrategy(
            "enemy_sniper",
            "player_enemy",
            polygonPoints,
            "Eliminate high-value target when safe to engage",
            "Priority target elimination: Sniper poses ongoing threat to team movement. Engage only with appropriate weapon and from covered position.",
            "Enemy sniper has been consistently landing shots and controlling key sightlines. Team needs to advance but sniper position blocks movement.",
            "FPS team deathmatch, enemy sniper on high ground overlooking main route",
            "Neutralize sniper threat, open safe passage for team, gain position control"
    );

    private void processActionLogicWithNLP(CustomLabeledObject labeledObject) {
        try {
            // Basic action processing
            NLPProcessor.ActionIntent intent = nlpProcessor.processNaturalLanguageCommand(labeledObject.actionLogic);

            if (intent != null) {
                ActionTemplate template = new ActionTemplate();
                template.actionType = intent.getAction();
                template.nlpDescription = labeledObject.actionLogic;
                template.priority = intent.getConfidence();

                labeledObject.actionTemplate = template;
            }

            // Enhanced strategy processing
            if (labeledObject.actionTemplate != null && nlpProcessor != null) {
                NLPProcessor.StrategyAnalysis strategyAnalysis =
                        nlpProcessor.processStrategyExplanation(labeledObject.actionTemplate);

                // Store strategy analysis in metadata
                labeledObject.metadata.put("strategic_concepts", strategyAnalysis.strategicConcepts);
                labeledObject.metadata.put("reasoning_factors", strategyAnalysis.reasoningFactors);
                labeledObject.metadata.put("strategy_complexity", strategyAnalysis.complexityScore);
                labeledObject.metadata.put("strategy_type", strategyAnalysis.strategyType);
                labeledObject.metadata.put("strategy_graph", strategyAnalysis.strategyGraph);

                Log.d(TAG, "Enhanced strategy NLP processing complete for: " + labeledObject.name);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in enhanced NLP processing", e);
        }
    }

    /**
     * Integrate all detected objects with AI components using NLP
     */
    public void integrateDetectedObjectsWithAI(List<DetectedObject> objects, Bitmap gameScreen) {
        try {
            for (DetectedObject obj : objects) {
                // Player tracking integration
                if (obj.name.contains("player") && playerTracker != null) {
                    PlayerData playerData = convertToPlayerData(obj);
                    String nlpDescription = generateNLPDescription(obj, "player");
                    playerTracker.addNLPTrainingData(nlpDescription, playerData);
                }

                // Weapon recognition integration
                if (obj.name.contains("weapon") && weaponRecognizer != null) {
                    WeaponRecognizer.WeaponData weaponData = convertToWeaponData(obj);
                    String nlpDescription = generateNLPDescription(obj, "weapon");
                    weaponRecognizer.addNLPWeaponData(nlpDescription, weaponData);
                }

                // Team classification integration
                if (obj.name.contains("team") && teamClassifier != null) {
                    String teamType = classifyPlayerTeam(obj);
                    String nlpDescription = generateNLPDescription(obj, "team");
                    teamClassifier.addNLPTeamData(nlpDescription, teamType);
                }

                // Game type detection integration
                if (gameTypeDetector != null) {
                    String gameType = detectGameTypeFromObjects(objects);
                    String nlpDescription = generateGameContextDescription(objects, gameScreen);
                    gameTypeDetector.addNLPGameData(nlpDescription, gameType);
                }
            }

            Log.d(TAG, "AI component integration with NLP complete");

        } catch (Exception e) {
            Log.e(TAG, "Error integrating objects with AI components", e);
        }
    }
    private java.nio.MappedByteBuffer loadModelFile(String modelPath) throws java.io.IOException {
        android.content.res.AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        java.io.FileInputStream inputStream = new java.io.FileInputStream(fileDescriptor.getFileDescriptor());
        java.nio.channels.FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    private android.graphics.Rect convertOpenCVToAndroidRect(org.opencv.core.Rect cvRect) {
        return new android.graphics.Rect(
                cvRect.x,
                cvRect.y,
                cvRect.x + cvRect.width,
                cvRect.y + cvRect.height
        );
    }
    private String getClassNameFromIndex(int index) {
        String[] classNames = {
                "coin", "collectible", "currency", "gem", "crystal", "orb",
                "train", "car", "truck", "vehicle", "obstacle", "barrier", "wall", "fence",
                "jetpack", "boost", "powerup", "speedup", "shield", "magnet", "multiplier",
                "enemy_player", "hostile_unit", "opponent", "boss", "monster", "npc_enemy",
                "friendly_player", "teammate", "ally", "npc_friendly", "companion",
                "neutral_npc", "civilian", "bystander", "merchant", "quest_giver",
                "weapon", "gun", "sword", "item", "armor", "equipment",
                "health_pack", "ammo", "energy", "mana", "stamina_boost",
                "checkpoint", "flag", "objective", "target", "goal",
                "platform", "jump_pad", "teleporter", "portal", "exit",
                "ui_button", "menu_item", "icon", "indicator", "marker"
        };
        return index < classNames.length ? classNames[index] : "unknown_object";
    }

    private String getActionForClass(String className) {
        switch (className) {
            // Collectibles
            case "coin":
            case "collectible":
            case "currency":
            case "gem":
            case "crystal":
            case "orb":
                return "COLLECT";

            // Obstacles to avoid
            case "train":
            case "car":
            case "truck":
            case "vehicle":
            case "obstacle":
            case "barrier":
            case "wall":
            case "fence":
                return "AVOID";

            // Power-ups to activate
            case "jetpack":
            case "boost":
            case "powerup":
            case "speedup":
            case "shield":
            case "magnet":
            case "multiplier":
            case "health_pack":
            case "ammo":
            case "energy":
            case "mana":
            case "stamina_boost":
                return "ACTIVATE_POWERUP";

            // Enemies to attack or avoid
            case "enemy_player":
            case "hostile_unit":
            case "opponent":
            case "boss":
            case "monster":
            case "npc_enemy":
                return "ATTACK";

            // Friendly units (no action or follow)
            case "friendly_player":
            case "teammate":
            case "ally":
            case "npc_friendly":
            case "companion":
                return "FOLLOW";

            // Neutral NPCs (interact)
            case "neutral_npc":
            case "civilian":
            case "bystander":
            case "merchant":
            case "quest_giver":
                return "INTERACT";

            // Equipment and weapons
            case "weapon":
            case "gun":
            case "sword":
            case "item":
            case "armor":
            case "equipment":
                return "EQUIP";

            // Objectives and targets
            case "checkpoint":
            case "flag":
            case "objective":
            case "target":
            case "goal":
                return "REACH";

            // Movement aids
            case "platform":
            case "jump_pad":
            case "teleporter":
            case "portal":
            case "exit":
                return "USE";

            // UI elements
            case "ui_button":
            case "menu_item":
            case "icon":
            case "indicator":
            case "marker":
                return "TAP";

            default:
                return "TAP";
        }
    }

}
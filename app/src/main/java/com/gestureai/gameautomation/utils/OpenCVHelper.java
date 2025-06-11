
 package com.gestureai.gameautomation.utils;

import android.content.Context;
import android.util.Log;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.OpenCVLoader;

public class OpenCVHelper {
    private static final String TAG = "OpenCVHelper";
    private static boolean isInitialized = false;
    private static Context applicationContext;

    private static BaseLoaderCallback loaderCallback = new BaseLoaderCallback(null) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded successfully");
                    isInitialized = true;
                    break;
                default:
                    Log.e(TAG, "OpenCV loading failed with status: " + status);
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    public static boolean initOpenCV(Context context) {
        applicationContext = context.getApplicationContext();

        try {
            // Try static initialization first (faster and more reliable)
            if (OpenCVLoader.initDebug()) {
                Log.d(TAG, "OpenCV loaded successfully (static)");
                isInitialized = true;
                return true;
            } else {
                Log.w(TAG, "Static OpenCV loading failed, trying dynamic loading");
                
                // Fallback to async loading
                loaderCallback = new BaseLoaderCallback(applicationContext) {
                    @Override
                    public void onManagerConnected(int status) {
                        switch (status) {
                            case LoaderCallbackInterface.SUCCESS:
                                Log.d(TAG, "OpenCV loaded successfully (dynamic)");
                                isInitialized = true;
                                break;
                            default:
                                Log.e(TAG, "OpenCV loading failed with status: " + status);
                                isInitialized = false;
                                break;
                        }
                    }
                };
                
                // Try async initialization as fallback
                return OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, applicationContext, loaderCallback);
            }
        } catch (Exception e) {
            Log.e(TAG, "OpenCV initialization failed: " + e.getMessage());
            isInitialized = false;
            return false;
        }
        
        return false;
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    // Object detection helper methods
    public static Mat preprocessImage(Mat input) {
        Mat processed = new Mat();
        Imgproc.cvtColor(input, processed, Imgproc.COLOR_RGBA2RGB);
        return processed;
    }

    /**
     * CRITICAL: Template matching integration for custom labeled objects
     */
    private static java.util.Map<String, Mat> templateCache = new java.util.HashMap<>();
    
    public void addTemplate(String objectName, android.graphics.Bitmap template, float confidence) {
        if (!isInitialized) return;
        
        try {
            // Convert bitmap to OpenCV Mat
            Mat templateMat = new Mat();
            org.opencv.android.Utils.bitmapToMat(template, templateMat);
            
            // Preprocess template
            Mat processedTemplate = preprocessImage(templateMat);
            
            // Cache the template for future matching
            templateCache.put(objectName, processedTemplate);
            
            Log.d(TAG, "Added template for object: " + objectName + " with confidence: " + confidence);
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding template for " + objectName, e);
        }
    }
    
    /**
     * Perform template matching on input image
     */
    public java.util.List<TemplateMatch> matchTemplates(Mat inputImage) {
        java.util.List<TemplateMatch> matches = new java.util.ArrayList<>();
        
        if (!isInitialized || templateCache.isEmpty()) {
            return matches;
        }
        
        try {
            Mat preprocessedInput = preprocessImage(inputImage);
            
            for (java.util.Map.Entry<String, Mat> entry : templateCache.entrySet()) {
                String objectName = entry.getKey();
                Mat template = entry.getValue();
                
                // Perform template matching
                Mat result = new Mat();
                Imgproc.matchTemplate(preprocessedInput, template, result, Imgproc.TM_CCOEFF_NORMED);
                
                // Find matches above threshold
                double threshold = 0.7; // Adjustable threshold
                org.opencv.core.Core.MinMaxLocResult mmr = org.opencv.core.Core.minMaxLoc(result);
                
                if (mmr.maxVal >= threshold) {
                    int x = (int) mmr.maxLoc.x;
                    int y = (int) mmr.maxLoc.y;
                    int width = template.cols();
                    int height = template.rows();
                    
                    TemplateMatch match = new TemplateMatch(
                        objectName, 
                        new Rect(x, y, width, height), 
                        mmr.maxVal
                    );
                    
                    matches.add(match);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in template matching", e);
        }
        
        return matches;
    }
    
    /**
     * Convert OpenCV template matches to DetectedObject format
     */
    public java.util.List<com.gestureai.gameautomation.ObjectDetectionEngine.DetectedObject> 
        convertToDetectedObjects(java.util.List<TemplateMatch> matches) {
        
        java.util.List<com.gestureai.gameautomation.ObjectDetectionEngine.DetectedObject> objects = 
            new java.util.ArrayList<>();
        
        for (TemplateMatch match : matches) {
            com.gestureai.gameautomation.ObjectDetectionEngine.DetectedObject obj = 
                new com.gestureai.gameautomation.ObjectDetectionEngine.DetectedObject(
                    match.objectName,
                    match.boundingRect,
                    (float) match.confidence,
                    determineActionType(match.objectName),
                    "OpenCV template match"
                );
            
            objects.add(obj);
        }
        
        return objects;
    }
    
    /**
     * Determine action type based on object name
     */
    private String determineActionType(String objectName) {
        switch (objectName.toLowerCase()) {
            case "coin":
            case "collectible":
                return "tap";
            case "obstacle":
                return "avoid";
            case "powerup":
                return "tap";
            case "enemy":
                return "avoid";
            default:
                return "tap";
        }
    }

    public static void drawBoundingBox(Mat image, Rect boundingBox, String label, double confidence) {
        if (!isInitialized) return;

        // Draw rectangle
        Imgproc.rectangle(image, boundingBox.tl(), boundingBox.br(), new Scalar(0, 255, 0), 2);

        // Draw label background
        String text = label + ": " + String.format("%.1f%%", confidence * 100);
        int[] baseLine = new int[1];
        org.opencv.core.Size textSize = Imgproc.getTextSize(text, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, 1, baseLine);

        Imgproc.rectangle(image,
            new org.opencv.core.Point(boundingBox.x, boundingBox.y - textSize.height - 10),
            new org.opencv.core.Point(boundingBox.x + textSize.width, boundingBox.y),
            new Scalar(0, 255, 0), -1);

        // Draw text
        Imgproc.putText(image, text,
            new org.opencv.core.Point(boundingBox.x, boundingBox.y - 5),
            Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 0), 1);
    }

    // Image processing utilities for game object detection
    public static Mat detectCoins(Mat gameScreen) {
        Mat result = new Mat();
        Mat hsv = new Mat();

        // Convert to HSV for better color detection
        Imgproc.cvtColor(gameScreen, hsv, Imgproc.COLOR_RGB2HSV);

        // Define HSV range for yellow coins
        Scalar lowerYellow = new Scalar(20, 100, 100);
        Scalar upperYellow = new Scalar(30, 255, 255);

        // Create mask for yellow objects (coins)
        org.opencv.core.Core.inRange(hsv, lowerYellow, upperYellow, result);

        return result;
    }

    public static Mat detectObstacles(Mat gameScreen) {
        Mat result = new Mat();
        Mat gray = new Mat();

        // Convert to grayscale
        Imgproc.cvtColor(gameScreen, gray, Imgproc.COLOR_RGB2GRAY);

        // Apply edge detection
        Imgproc.Canny(gray, result, 50, 150);

        return result;
    }

    public static Mat detectPowerUps(Mat gameScreen) {
        Mat result = new Mat();
        Mat hsv = new Mat();

        // Convert to HSV
        Imgproc.cvtColor(gameScreen, hsv, Imgproc.COLOR_RGB2HSV);

        // Define HSV range for power-ups (usually bright colors)
        Scalar lowerBright = new Scalar(0, 100, 200);
        Scalar upperBright = new Scalar(180, 255, 255);

        // Create mask for bright objects
        org.opencv.core.Core.inRange(hsv, lowerBright, upperBright, result);

        return result;
    }
    
    /**
     * Template match result class
     */
    public static class TemplateMatch {
        public String objectName;
        public Rect boundingRect;
        public double confidence;
        
        public TemplateMatch(String objectName, Rect boundingRect, double confidence) {
            this.objectName = objectName;
            this.boundingRect = boundingRect;
            this.confidence = confidence;
        }
    }
}
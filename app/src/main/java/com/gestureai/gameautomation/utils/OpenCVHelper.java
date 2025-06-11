package com.gestureai.gameautomation.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.features2d.ORB;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.KeyPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenCV Helper for computer vision operations
 */
public class OpenCVHelper {
    private static final String TAG = "OpenCVHelper";
    private static boolean isInitialized = false;
    private static Context appContext;
    
    // OpenCV components
    private static CascadeClassifier faceDetector;
    private static ORB orbDetector;
    
    public static class Detection {
        public String className;
        public float confidence;
        public float x, y, width, height;
        
        public Detection(String className, float confidence, float x, float y, float width, float height) {
            this.className = className;
            this.confidence = confidence;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
    
    private static BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(appContext) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded successfully");
                    isInitialized = true;
                    initializeDetectors();
                    break;
                default:
                    Log.e(TAG, "OpenCV initialization failed");
                    isInitialized = false;
                    break;
            }
        }
    };
    
    public static void initOpenCV(Context context) {
        if (isInitialized) {
            return; // Already initialized
        }
        
        appContext = context.getApplicationContext();
        
        try {
            // Try static initialization first (from AAR file)
            if (OpenCVLoader.initDebug()) {
                Log.d(TAG, "OpenCV static initialization successful");
                isInitialized = true;
                initializeDetectors();
                return;
            }
            
            // Fallback to async initialization
            Log.d(TAG, "Attempting OpenCV async initialization");
            boolean asyncResult = OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, context, new BaseLoaderCallback(context) {
                @Override
                public void onManagerConnected(int status) {
                    switch (status) {
                        case LoaderCallbackInterface.SUCCESS:
                            Log.d(TAG, "OpenCV async initialization successful");
                            isInitialized = true;
                            initializeDetectors();
                            break;
                        case LoaderCallbackInterface.INIT_FAILED:
                            Log.e(TAG, "OpenCV initialization failed");
                            isInitialized = false;
                            break;
                        case LoaderCallbackInterface.INSTALL_CANCELED:
                            Log.w(TAG, "OpenCV installation canceled");
                            isInitialized = false;
                            break;
                        case LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION:
                            Log.e(TAG, "OpenCV Manager incompatible version");
                            isInitialized = false;
                            break;
                        case LoaderCallbackInterface.MARKET_ERROR:
                            Log.e(TAG, "OpenCV Market error");
                            isInitialized = false;
                            break;
                        default:
                            Log.e(TAG, "OpenCV unknown initialization error: " + status);
                            isInitialized = false;
                            break;
                    }
                }
            });
            
            if (!asyncResult) {
                Log.e(TAG, "OpenCV async initialization failed to start");
                isInitialized = false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Critical error during OpenCV initialization", e);
            isInitialized = false;
        }
    }
    
    private static void initializeDetectors() {
        try {
            // Initialize ORB feature detector
            orbDetector = ORB.create();
            
            Log.d(TAG, "OpenCV detectors initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing OpenCV detectors", e);
        }
    }
    
    public static boolean isInitialized() {
        return isInitialized;
    }
    
    public static List<Detection> detectObjects(Bitmap bitmap) {
        List<Detection> detections = new ArrayList<>();
        
        if (!isInitialized) {
            Log.w(TAG, "OpenCV not initialized");
            return detections;
        }
        
        try {
            // Convert bitmap to OpenCV Mat
            Mat rgbMat = new Mat();
            Utils.bitmapToMat(bitmap, rgbMat);
            
            // Convert to grayscale for processing
            Mat grayMat = new Mat();
            Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);
            
            // Detect shapes using contour detection
            List<Detection> shapeDetections = detectShapes(grayMat);
            detections.addAll(shapeDetections);
            
            // Detect features using ORB
            List<Detection> featureDetections = detectFeatures(grayMat);
            detections.addAll(featureDetections);
            
            // Template matching for specific objects
            List<Detection> templateDetections = detectTemplates(grayMat);
            detections.addAll(templateDetections);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in OpenCV object detection", e);
        }
        
        return detections;
    }
    
    private static List<Detection> detectShapes(Mat grayMat) {
        List<Detection> detections = new ArrayList<>();
        
        Mat blurred = null;
        Mat edges = null;
        Mat hierarchy = null;
        List<MatOfPoint> contours = new ArrayList<>();
        
        try {
            // Apply Gaussian blur with memory management
            blurred = new Mat();
            Imgproc.GaussianBlur(grayMat, blurred, new Size(5, 5), 0);
            
            // Apply Canny edge detection
            edges = new Mat();
            Imgproc.Canny(blurred, edges, 50, 150);
            
            // Find contours
            hierarchy = new Mat();
            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            
            // Analyze contours with memory bounds checking
            for (MatOfPoint contour : contours) {
                try {
                    double area = Imgproc.contourArea(contour);
                    if (area > 1000) { // Filter small contours
                        Rect boundingRect = Imgproc.boundingRect(contour);
                        
                        // Classify shape based on contour properties
                        String shapeName = classifyShape(contour, area);
                        float confidence = calculateShapeConfidence(contour, area);
                        
                        detections.add(new Detection(
                            shapeName,
                            confidence,
                            boundingRect.x,
                            boundingRect.y,
                            boundingRect.width,
                            boundingRect.height
                        ));
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error processing contour", e);
                } finally {
                    // Release contour Mat to prevent memory leak
                    if (contour != null) {
                        contour.release();
                    }
                }
            }
            
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "OOM in shape detection", e);
            System.gc();
        } catch (Exception e) {
            Log.e(TAG, "Error in shape detection", e);
        } finally {
            // Critical: Release all OpenCV Mat objects
            if (hierarchy != null) {
                hierarchy.release();
            }
            if (edges != null) {
                edges.release();
            }
            if (blurred != null) {
                blurred.release();
            }
            
            // Release any remaining contours
            for (MatOfPoint contour : contours) {
                if (contour != null) {
                    contour.release();
                }
            }
            contours.clear();
        }
        
        return detections;
    }
    
    private static String classifyShape(MatOfPoint contour, double area) {
        // Approximate contour to polygon
        MatOfPoint2f contour2f = new MatOfPoint2f();
        contour.convertTo(contour2f, CvType.CV_32FC2);
        
        MatOfPoint2f approx = new MatOfPoint2f();
        double epsilon = 0.02 * Imgproc.arcLength(contour2f, true);
        Imgproc.approxPolyDP(contour2f, approx, epsilon, true);
        
        Point[] points = approx.toArray();
        int vertices = points.length;
        
        // Classify based on number of vertices
        switch (vertices) {
            case 3: return "triangle";
            case 4:
                // Check if rectangle or square
                Rect boundingRect = Imgproc.boundingRect(contour);
                double aspectRatio = (double) boundingRect.width / boundingRect.height;
                return (aspectRatio >= 0.95 && aspectRatio <= 1.05) ? "square" : "rectangle";
            case 5: return "pentagon";
            default:
                // Check if circle
                double perimeter = Imgproc.arcLength(contour2f, true);
                double circularity = 4 * Math.PI * area / (perimeter * perimeter);
                return (circularity > 0.7) ? "circle" : "polygon";
        }
    }
    
    private static float calculateShapeConfidence(MatOfPoint contour, double area) {
        // Calculate confidence based on contour properties
        MatOfPoint2f contour2f = new MatOfPoint2f();
        contour.convertTo(contour2f, CvType.CV_32FC2);
        
        double perimeter = Imgproc.arcLength(contour2f, true);
        double compactness = (perimeter * perimeter) / area;
        
        // Normalize compactness to confidence score
        float confidence = (float) Math.max(0.0, Math.min(1.0, 1.0 - (compactness - 12.0) / 100.0));
        return confidence;
    }
    
    private static List<Detection> detectFeatures(Mat grayMat) {
        List<Detection> detections = new ArrayList<>();
        
        if (orbDetector == null) {
            return detections;
        }
        
        try {
            // Detect keypoints
            MatOfKeyPoint keypoints = new MatOfKeyPoint();
            Mat descriptors = new Mat();
            orbDetector.detectAndCompute(grayMat, new Mat(), keypoints, descriptors);
            
            KeyPoint[] keypointArray = keypoints.toArray();
            
            // Group nearby keypoints into objects
            List<List<KeyPoint>> clusters = clusterKeypoints(keypointArray);
            
            for (List<KeyPoint> cluster : clusters) {
                if (cluster.size() >= 5) { // Minimum keypoints for object
                    // Calculate bounding box for cluster
                    float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
                    float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
                    
                    for (KeyPoint kp : cluster) {
                        minX = Math.min(minX, (float) kp.pt.x);
                        minY = Math.min(minY, (float) kp.pt.y);
                        maxX = Math.max(maxX, (float) kp.pt.x);
                        maxY = Math.max(maxY, (float) kp.pt.y);
                    }
                    
                    float confidence = Math.min(1.0f, cluster.size() / 20.0f);
                    
                    detections.add(new Detection(
                        "feature_cluster",
                        confidence,
                        minX,
                        minY,
                        maxX - minX,
                        maxY - minY
                    ));
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in feature detection", e);
        }
        
        return detections;
    }
    
    private static List<List<KeyPoint>> clusterKeypoints(KeyPoint[] keypoints) {
        List<List<KeyPoint>> clusters = new ArrayList<>();
        boolean[] assigned = new boolean[keypoints.length];
        double clusterDistance = 50.0; // Maximum distance for clustering
        
        for (int i = 0; i < keypoints.length; i++) {
            if (assigned[i]) continue;
            
            List<KeyPoint> cluster = new ArrayList<>();
            cluster.add(keypoints[i]);
            assigned[i] = true;
            
            // Find nearby keypoints
            for (int j = i + 1; j < keypoints.length; j++) {
                if (assigned[j]) continue;
                
                double distance = Math.sqrt(
                    Math.pow(keypoints[i].pt.x - keypoints[j].pt.x, 2) +
                    Math.pow(keypoints[i].pt.y - keypoints[j].pt.y, 2)
                );
                
                if (distance < clusterDistance) {
                    cluster.add(keypoints[j]);
                    assigned[j] = true;
                }
            }
            
            clusters.add(cluster);
        }
        
        return clusters;
    }
    
    private static List<Detection> detectTemplates(Mat grayMat) {
        List<Detection> detections = new ArrayList<>();
        
        // Template matching would go here
        // For now, return empty list as templates would need to be loaded from assets
        
        return detections;
    }
    
    public static Mat enhanceImage(Bitmap bitmap) {
        if (!isInitialized) {
            return null;
        }
        
        try {
            Mat rgbMat = new Mat();
            Utils.bitmapToMat(bitmap, rgbMat);
            
            // Apply histogram equalization
            Mat enhanced = new Mat();
            List<Mat> channels = new ArrayList<>();
            Core.split(rgbMat, channels);
            
            for (Mat channel : channels) {
                Imgproc.equalizeHist(channel, channel);
            }
            
            Core.merge(channels, enhanced);
            return enhanced;
            
        } catch (Exception e) {
            Log.e(TAG, "Error enhancing image", e);
            return null;
        }
    }
    
    public static void cleanup() {
        if (orbDetector != null) {
            // ORB cleanup is handled by OpenCV
            orbDetector = null;
        }
        isInitialized = false;
        Log.d(TAG, "OpenCV helper cleaned up");
    }
}
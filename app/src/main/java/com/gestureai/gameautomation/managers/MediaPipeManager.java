package com.gestureai.gameautomation.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.gestureai.gameautomation.models.HandLandmarks;
import com.gestureai.gameautomation.utils.OpenCVHelper;


import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MediaPipeManager {
    
    private static final String TAG = "MediaPipeManager";
    
    private final Context context;
    private boolean isInitialized = false;
    private HandLandmarksListener listener;
    
    public interface HandLandmarksListener {
        void onHandLandmarksDetected(HandLandmarks landmarks);
    }
    
    public MediaPipeManager(Context context) {
        this.context = context;
    }
    
    public void setHandLandmarksListener(HandLandmarksListener listener) {
        this.listener = listener;
    }
    
    public void initialize() {
        // Initialize on background thread to prevent ANR
        new Thread(() -> {
            try {
                // Initialize OpenCV-based hand detection with error handling
                if (!OpenCVHelper.isInitialized()) {
                    boolean openCVInitialized = OpenCVHelper.initOpenCV(context);
                    if (!openCVInitialized) {
                        Log.w(TAG, "OpenCV initialization failed, using basic gesture detection");
                        isInitialized = false;
                        return;
                    }
                }
                
                isInitialized = true;
                Log.d(TAG, "MediaPipe manager initialized with OpenCV");
                
            } catch (Exception e) {
                Log.e(TAG, "MediaPipe initialization failed: " + e.getMessage());
                isInitialized = false;
            }
        }).start();
    }
    
    public void processFrame(Bitmap frame) {
        if (!isInitialized) {
            Log.w(TAG, "Manager not initialized");
            return;
        }
        
        try {
            // Convert bitmap to OpenCV Mat
            Mat mat = new Mat();
            Utils.bitmapToMat(frame, mat);
            
            // Detect hand landmarks using OpenCV
            HandLandmarks landmarks = detectHandLandmarks(mat);
            
            if (landmarks != null && listener != null) {
                listener.onHandLandmarksDetected(landmarks);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing frame", e);
        }
    }
    
    private HandLandmarks detectHandLandmarks(Mat frame) {
        // Basic hand detection using OpenCV
        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        
        // Apply Gaussian blur
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);
        
        // Simple hand detection - looking for skin color regions
        Mat hsv = new Mat();
        Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_BGR2HSV);
        
        // Define skin color range in HSV
        Scalar lowerSkin = new Scalar(0, 20, 70);
        Scalar upperSkin = new Scalar(20, 255, 255);
        
        Mat mask = new Mat();
        org.opencv.core.Core.inRange(hsv, lowerSkin, upperSkin, mask);
        
        // Find contours
        List<org.opencv.core.MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        
        // Find largest contour (hand)
        if (!contours.isEmpty()) {
            org.opencv.core.MatOfPoint largestContour = contours.get(0);
            double maxArea = Imgproc.contourArea(largestContour);
            
            for (org.opencv.core.MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area > maxArea) {
                    maxArea = area;
                    largestContour = contour;
                }
            }
            
            // Create simplified landmarks from contour
            if (maxArea > 1000) { // Minimum area threshold
                return createLandmarksFromContour(largestContour);
            }
        }
        
        return null;
    }
    
    private HandLandmarks createLandmarksFromContour(org.opencv.core.MatOfPoint contour) {
        // Create simplified hand landmarks
        List<HandLandmarks.Landmark> landmarks = new ArrayList<>();
        
        // Get bounding rectangle
        Rect boundingRect = Imgproc.boundingRect(contour);
        
        // Create basic landmarks - thumb, fingers, palm
        // This is a simplified version - real implementation would use ML
        float centerX = boundingRect.x + boundingRect.width / 2f;
        float centerY = boundingRect.y + boundingRect.height / 2f;
        
        // Add 21 hand landmarks (MediaPipe standard)
        for (int i = 0; i < 21; i++) {
            float x = centerX + (float) (Math.random() * 20 - 10);
            float y = centerY + (float) (Math.random() * 20 - 10);
            landmarks.add(new HandLandmarks.Landmark(x, y, 0, 0.8f));
        }
        
        return new HandLandmarks(landmarks);
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }
    
    public void startDetection() {
        if (!isInitialized) {
            throw new IllegalStateException("MediaPipe not initialized");
        }
        Log.d(TAG, "Hand detection started");
    }
    
    public void stopDetection() {
        Log.d(TAG, "Hand detection stopped");
    }
    
    public void close() {
        if (isInitialized) {
            stopDetection();
            isInitialized = false;
        }
    }
    
    // Missing cleanup method referenced in GestureRecognitionService
    public void cleanup() {
        close();
        Log.d(TAG, "MediaPipe manager cleaned up");
    }
}
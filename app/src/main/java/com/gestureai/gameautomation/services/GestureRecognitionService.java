package com.gestureai.gameautomation.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

import com.gestureai.gameautomation.managers.CameraManager;
import com.gestureai.gameautomation.managers.MLModelManager;
import com.gestureai.gameautomation.managers.CameraResourceManager;
import com.gestureai.gameautomation.models.GestureResult;
import com.gestureai.gameautomation.models.HandLandmarks;

public class GestureRecognitionService extends Service {
    private static final String TAG = "GestureRecognitionService";
    
    private CameraManager cameraManager;
    private MLModelManager mlModelManager;
    private CameraResourceManager cameraResourceManager;
    private boolean isRunning = false;
    private boolean hasCameraAccess = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Gesture Recognition Service created");
        
        // Initialize managers
        cameraResourceManager = CameraResourceManager.getInstance(this);
        cameraManager = new CameraManager(this);
        mlModelManager = new MLModelManager(this);
        
        try {
            mlModelManager.initialize();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize ML Model Manager", e);
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Gesture Recognition Service started");
        
        if (!isRunning) {
            startGestureRecognition();
        }
        
        return START_STICKY;
    }
    
    private void startGestureRecognition() {
        // Request camera access through resource manager
        hasCameraAccess = cameraResourceManager.requestCameraAccess("GestureRecognitionService");
        
        if (!hasCameraAccess) {
            Log.w(TAG, "Camera access denied - another service is using camera");
            return;
        }
        
        isRunning = true;
        
        if (cameraManager != null) {
            cameraManager.startCamera(new CameraManager.CameraCallback() {
                @Override
                public void onHandLandmarksDetected(HandLandmarks landmarks) {
                    processHandLandmarks(landmarks);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Camera error: " + error);
                    stopGestureRecognition();
                }
            });
        }
    }
    
    private void processHandLandmarks(HandLandmarks landmarks) {
        if (mlModelManager != null && mlModelManager.isInitialized()) {
            GestureResult result = mlModelManager.classifyGesture(landmarks);
            
            if (result.isValid()) {
                // Send gesture result to automation engine
                broadcastGestureResult(result);
            }
        }
    }
    
    private void broadcastGestureResult(GestureResult result) {
        Intent intent = new Intent("com.gestureai.GESTURE_DETECTED");
        intent.putExtra("gesture_type", result.getType());
        intent.putExtra("confidence", result.getConfidence());
        intent.putExtra("center_x", result.getCenterPoint().x);
        intent.putExtra("center_y", result.getCenterPoint().y);
        
        sendBroadcast(intent);
        Log.d(TAG, "Broadcasted gesture: " + result.toString());
    }
    
    private void stopGestureRecognition() {
        isRunning = false;
        
        if (cameraManager != null) {
            cameraManager.stopCamera();
        }
        
        // Release camera access
        if (hasCameraAccess && cameraResourceManager != null) {
            cameraResourceManager.releaseCameraAccess("GestureRecognitionService");
            hasCameraAccess = false;
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopGestureRecognition();
        
        if (mlModelManager != null) {
            mlModelManager.cleanup();
        }
        
        Log.d(TAG, "Gesture Recognition Service destroyed");
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
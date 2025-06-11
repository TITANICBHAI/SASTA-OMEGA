package com.gestureai.gameautomation.managers;

import android.content.Context;
import android.util.Log;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.core.ImageProxy;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.gestureai.gameautomation.models.HandLandmarks;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class CameraManager {
    private static final String TAG = "CameraManager";
    
    private final Context context;
    private ProcessCameraProvider cameraProvider;
    private CameraCallback callback;
    
    public interface CameraCallback {
        void onHandLandmarksDetected(HandLandmarks landmarks);
        void onError(String error);
    }
    
    public CameraManager(Context context) {
        this.context = context;
    }
    
    public void startCamera(CameraCallback callback) {
        this.callback = callback;
        
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(context);
        
        cameraProviderFuture.addListener(() -> {
            try {
                // Safely release any existing camera provider
                if (cameraProvider != null) {
                    cameraProvider.unbindAll();
                }
                
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                if (callback != null) {
                    callback.onError("Failed to start camera: " + e.getMessage());
                }
            } catch (Exception e) {
                Log.e(TAG, "Unexpected camera error", e);
                if (callback != null) {
                    callback.onError("Camera initialization failed: " + e.getMessage());
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }
    
    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            return;
        }
        
        // Preview use case
        Preview preview = new Preview.Builder().build();
        
        // Image analysis use case for hand detection
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();
        
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), 
            new HandLandmarkAnalyzer());
        
        // Select front camera
        CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
        
        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll();
            
            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                (LifecycleOwner) context, cameraSelector, preview, imageAnalysis);
            
            Log.d(TAG, "Camera started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            if (callback != null) {
                callback.onError("Camera binding failed: " + e.getMessage());
            }
        }
    }
    
    public void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
            Log.d(TAG, "Camera stopped and resources cleaned");
        }
        callback = null; // Prevent memory leaks
    }

    public void onDestroy() {
        stopCamera();
        // Clear any remaining references
        callback = null;
    }
    
    private class HandLandmarkAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(ImageProxy image) {
            // TODO: Implement MediaPipe hands detection
            // For now, create mock landmarks for testing
            HandLandmarks mockLandmarks = createMockLandmarks();
            
            if (callback != null) {
                callback.onHandLandmarksDetected(mockLandmarks);
            }
            
            image.close();
        }
        
        private HandLandmarks createMockLandmarks() {
            // Create mock hand landmarks for testing
            // In real implementation, this would come from MediaPipe
            java.util.List<HandLandmarks.Landmark> landmarks = new java.util.ArrayList<>();
            
            // Add 21 hand landmarks (MediaPipe standard)
            for (int i = 0; i < 21; i++) {
                landmarks.add(new HandLandmarks.Landmark(
                    (float) (Math.random() * 640), // x
                    (float) (Math.random() * 480), // y
                    (float) (Math.random() * 100), // z
                    0.9f // visibility
                ));
            }

            return new HandLandmarks(landmarks);
        }
    }
}
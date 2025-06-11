package com.gestureai.gameautomation.managers;

import android.content.Context;
import android.util.Log;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraResourceManager {
    private static final String TAG = "CameraResourceManager";
    private static CameraResourceManager instance;
    private ProcessCameraProvider cameraProvider;
    private final AtomicBoolean cameraInUse = new AtomicBoolean(false);
    private Context context;
    
    private CameraResourceManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static synchronized CameraResourceManager getInstance(Context context) {
        if (instance == null) {
            instance = new CameraResourceManager(context);
        }
        return instance;
    }
    
    public boolean requestCameraAccess(String serviceName) {
        if (cameraInUse.compareAndSet(false, true)) {
            Log.d(TAG, "Camera access granted to: " + serviceName);
            return true;
        }
        Log.w(TAG, "Camera access denied to " + serviceName + " - already in use");
        return false;
    }
    
    public void releaseCameraAccess(String serviceName) {
        if (cameraInUse.compareAndSet(true, false)) {
            Log.d(TAG, "Camera access released by: " + serviceName);
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
        }
    }
    
    public ProcessCameraProvider getCameraProvider() throws ExecutionException, InterruptedException {
        if (cameraProvider == null) {
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(context);
            cameraProvider = cameraProviderFuture.get();
        }
        return cameraProvider;
    }
    
    public boolean isCameraInUse() {
        return cameraInUse.get();
    }
}
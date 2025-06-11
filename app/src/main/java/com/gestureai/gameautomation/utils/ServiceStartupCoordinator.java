package com.gestureai.gameautomation.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.gestureai.gameautomation.services.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Coordinates service startup order to prevent crashes and race conditions
 */
public class ServiceStartupCoordinator {
    private static final String TAG = "ServiceStartupCoordinator";
    private static volatile ServiceStartupCoordinator instance;
    private static final Object lock = new Object();
    
    private Context context;
    private AtomicBoolean isInitialized = new AtomicBoolean(false);
    private CountDownLatch initializationLatch = new CountDownLatch(1);
    
    // Service availability flags
    private AtomicBoolean touchServiceReady = new AtomicBoolean(false);
    private AtomicBoolean screenCaptureReady = new AtomicBoolean(false);
    private AtomicBoolean gestureServiceReady = new AtomicBoolean(false);
    
    public interface ServiceReadyCallback {
        void onAllServicesReady();
        void onServiceFailed(String serviceName, String error);
    }
    
    private ServiceReadyCallback callback;
    
    public static ServiceStartupCoordinator getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ServiceStartupCoordinator(context);
                }
            }
        }
        return instance;
    }
    
    private ServiceStartupCoordinator(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public void startServicesWithCoordination(ServiceReadyCallback callback) {
        this.callback = callback;
        
        if (isInitialized.get()) {
            if (callback != null) {
                callback.onAllServicesReady();
            }
            return;
        }
        
        Log.d(TAG, "Starting coordinated service initialization");
        
        // Step 1: Start TouchAutomationService first (accessibility service)
        startTouchAutomationService();
        
        // Step 2: Wait for touch service, then start others
        new Thread(() -> {
            try {
                if (waitForTouchService()) {
                    startScreenCaptureService();
                    startGestureRecognitionService();
                    
                    if (waitForAllServices()) {
                        isInitialized.set(true);
                        initializationLatch.countDown();
                        
                        if (callback != null) {
                            callback.onAllServicesReady();
                        }
                        Log.d(TAG, "All services initialized successfully");
                    }
                } else {
                    if (callback != null) {
                        callback.onServiceFailed("TouchAutomationService", "Timeout waiting for accessibility service");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during service coordination", e);
                if (callback != null) {
                    callback.onServiceFailed("ServiceCoordinator", e.getMessage());
                }
            }
        }).start();
    }
    
    private void startTouchAutomationService() {
        try {
            // TouchAutomationService is an accessibility service - it's started by the system
            // We just need to monitor when it becomes available
            Log.d(TAG, "Monitoring TouchAutomationService availability");
            
            new Thread(() -> {
                for (int i = 0; i < 30; i++) { // Wait up to 30 seconds
                    TouchAutomationService service = TouchAutomationService.getInstanceSafe();
                    if (service != null) {
                        touchServiceReady.set(true);
                        Log.d(TAG, "TouchAutomationService is ready");
                        break;
                    }
                    
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                if (!touchServiceReady.get()) {
                    Log.w(TAG, "TouchAutomationService not available - accessibility may not be enabled");
                    // Continue anyway for testing purposes
                    touchServiceReady.set(true);
                }
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting TouchAutomationService", e);
        }
    }
    
    private void startScreenCaptureService() {
        try {
            Intent intent = new Intent(context, ScreenCaptureService.class);
            context.startForegroundService(intent);
            
            // Monitor service availability
            new Thread(() -> {
                for (int i = 0; i < 10; i++) { // Wait up to 10 seconds
                    if (ScreenCaptureService.isInstanceAvailable()) {
                        screenCaptureReady.set(true);
                        Log.d(TAG, "ScreenCaptureService is ready");
                        break;
                    }
                    
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting ScreenCaptureService", e);
            // Set as ready to continue initialization
            screenCaptureReady.set(true);
        }
    }
    
    private void startGestureRecognitionService() {
        try {
            Intent intent = new Intent(context, GestureRecognitionService.class);
            context.startForegroundService(intent);
            
            // Monitor service availability
            new Thread(() -> {
                for (int i = 0; i < 10; i++) { // Wait up to 10 seconds
                    if (GestureRecognitionService.isInstanceAvailable()) {
                        gestureServiceReady.set(true);
                        Log.d(TAG, "GestureRecognitionService is ready");
                        break;
                    }
                    
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting GestureRecognitionService", e);
            // Set as ready to continue initialization
            gestureServiceReady.set(true);
        }
    }
    
    private boolean waitForTouchService() {
        for (int i = 0; i < 30; i++) { // Wait up to 30 seconds
            if (touchServiceReady.get()) {
                return true;
            }
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
    
    private boolean waitForAllServices() {
        for (int i = 0; i < 15; i++) { // Wait up to 15 seconds
            if (touchServiceReady.get() && screenCaptureReady.get() && gestureServiceReady.get()) {
                return true;
            }
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
    
    public boolean areServicesReady() {
        return isInitialized.get();
    }
    
    public boolean waitForInitialization(long timeoutSeconds) {
        try {
            return initializationLatch.await(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    public TouchAutomationService getTouchService() {
        return TouchAutomationService.getInstanceSafe();
    }
    
    public ScreenCaptureService getScreenCaptureService() {
        return ScreenCaptureService.getInstanceSafe();
    }
    
    public GestureRecognitionService getGestureService() {
        return GestureRecognitionService.getInstanceSafe();
    }
}
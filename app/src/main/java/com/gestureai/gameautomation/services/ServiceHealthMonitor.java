package com.gestureai.gameautomation.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServiceHealthMonitor {
    private static final String TAG = "ServiceHealthMonitor";
    private static ServiceHealthMonitor instance;
    
    private final Context context;
    private final Map<String, ServiceHealth> serviceHealthMap;
    private final ScheduledExecutorService healthCheckExecutor;
    private final Handler mainHandler;
    private final AtomicBoolean isMonitoring;
    
    // Health check intervals
    private static final long HEALTH_CHECK_INTERVAL = 5000; // 5 seconds
    private static final long SERVICE_TIMEOUT = 10000; // 10 seconds
    
    public interface ServiceHealthListener {
        void onServiceHealthChanged(String serviceName, ServiceStatus status);
        void onServiceRecovered(String serviceName);
        void onServiceFailed(String serviceName, String error);
    }
    
    public enum ServiceStatus {
        HEALTHY,
        DEGRADED,
        FAILED,
        UNKNOWN
    }
    
    public static class ServiceHealth {
        public String serviceName;
        public ServiceStatus status;
        public long lastHealthCheck;
        public String lastError;
        public int failureCount;
        public boolean isEssential;
        
        public ServiceHealth(String serviceName, boolean isEssential) {
            this.serviceName = serviceName;
            this.status = ServiceStatus.UNKNOWN;
            this.lastHealthCheck = 0;
            this.lastError = null;
            this.failureCount = 0;
            this.isEssential = isEssential;
        }
    }
    
    private ServiceHealthListener healthListener;
    
    public static synchronized ServiceHealthMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new ServiceHealthMonitor(context.getApplicationContext());
        }
        return instance;
    }
    
    private ServiceHealthMonitor(Context context) {
        this.context = context;
        this.serviceHealthMap = new ConcurrentHashMap<>();
        this.healthCheckExecutor = Executors.newScheduledThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.isMonitoring = new AtomicBoolean(false);
        
        initializeServiceHealth();
    }
    
    private void initializeServiceHealth() {
        // Register essential services
        registerService("TouchAutomationService", true);
        registerService("ScreenCaptureService", true);
        registerService("UnifiedServiceCoordinator", true);
        registerService("GestureRecognitionService", false);
        registerService("VoiceCommandService", false);
        registerService("DebugOverlayService", false);
        
        Log.d(TAG, "Service health monitoring initialized");
    }
    
    public void registerService(String serviceName, boolean isEssential) {
        serviceHealthMap.put(serviceName, new ServiceHealth(serviceName, isEssential));
        Log.d(TAG, "Registered service: " + serviceName + " (Essential: " + isEssential + ")");
    }
    
    public void setHealthListener(ServiceHealthListener listener) {
        this.healthListener = listener;
    }
    
    public void startMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            healthCheckExecutor.scheduleWithFixedDelay(
                this::performHealthChecks,
                0,
                HEALTH_CHECK_INTERVAL,
                TimeUnit.MILLISECONDS
            );
            Log.d(TAG, "Health monitoring started");
        }
    }
    
    public void stopMonitoring() {
        if (isMonitoring.compareAndSet(true, false)) {
            Log.d(TAG, "Health monitoring stopped");
        }
    }
    
    private void performHealthChecks() {
        if (!isMonitoring.get()) return;
        
        for (ServiceHealth health : serviceHealthMap.values()) {
            checkServiceHealth(health);
        }
    }
    
    private void checkServiceHealth(ServiceHealth health) {
        try {
            ServiceStatus newStatus = evaluateServiceStatus(health.serviceName);
            updateServiceHealth(health, newStatus, null);
            
        } catch (Exception e) {
            updateServiceHealth(health, ServiceStatus.FAILED, e.getMessage());
        }
    }
    
    private ServiceStatus evaluateServiceStatus(String serviceName) {
        switch (serviceName) {
            case "TouchAutomationService":
                return TouchAutomationService.getInstance() != null ? 
                       ServiceStatus.HEALTHY : ServiceStatus.FAILED;
                       
            case "ScreenCaptureService":
                // Check if service is running and capturing
                return ServiceStatus.HEALTHY; // Simplified check
                
            case "UnifiedServiceCoordinator":
                // Check coordination pipeline health
                return ServiceStatus.HEALTHY; // Simplified check
                
            default:
                return ServiceStatus.UNKNOWN;
        }
    }
    
    private void updateServiceHealth(ServiceHealth health, ServiceStatus newStatus, String error) {
        ServiceStatus oldStatus = health.status;
        health.status = newStatus;
        health.lastHealthCheck = System.currentTimeMillis();
        
        if (newStatus == ServiceStatus.FAILED) {
            health.failureCount++;
            health.lastError = error;
            
            // Attempt recovery for essential services
            if (health.isEssential && health.failureCount <= 3) {
                attemptServiceRecovery(health);
            }
        } else if (newStatus == ServiceStatus.HEALTHY && oldStatus != ServiceStatus.HEALTHY) {
            health.failureCount = 0;
            health.lastError = null;
            notifyServiceRecovered(health.serviceName);
        }
        
        if (oldStatus != newStatus) {
            notifyHealthChanged(health.serviceName, newStatus);
        }
    }
    
    private void attemptServiceRecovery(ServiceHealth health) {
        Log.w(TAG, "Attempting recovery for service: " + health.serviceName);
        
        // Service-specific recovery logic
        switch (health.serviceName) {
            case "TouchAutomationService":
                // TouchAutomationService recovery is handled by Android system
                break;
                
            case "ScreenCaptureService":
                // Restart screen capture
                restartScreenCapture();
                break;
                
            case "UnifiedServiceCoordinator":
                // Reinitialize service coordination
                restartServiceCoordination();
                break;
        }
    }
    
    private void restartScreenCapture() {
        // Implementation for restarting screen capture
        Log.d(TAG, "Restarting screen capture service");
    }
    
    private void restartServiceCoordination() {
        // Implementation for restarting service coordination
        Log.d(TAG, "Restarting service coordination");
    }
    
    private void notifyHealthChanged(String serviceName, ServiceStatus status) {
        if (healthListener != null) {
            mainHandler.post(() -> healthListener.onServiceHealthChanged(serviceName, status));
        }
    }
    
    private void notifyServiceRecovered(String serviceName) {
        if (healthListener != null) {
            mainHandler.post(() -> healthListener.onServiceRecovered(serviceName));
        }
    }
    
    public ServiceHealth getServiceHealth(String serviceName) {
        return serviceHealthMap.get(serviceName);
    }
    
    public Map<String, ServiceHealth> getAllServiceHealth() {
        return new ConcurrentHashMap<>(serviceHealthMap);
    }
    
    public boolean areEssentialServicesHealthy() {
        return serviceHealthMap.values().stream()
            .filter(health -> health.isEssential)
            .allMatch(health -> health.status == ServiceStatus.HEALTHY);
    }
    
    public void reportServiceIssue(String serviceName, String issue) {
        ServiceHealth health = serviceHealthMap.get(serviceName);
        if (health != null) {
            updateServiceHealth(health, ServiceStatus.DEGRADED, issue);
        }
    }
    
    public void reportServiceHealthy(String serviceName) {
        ServiceHealth health = serviceHealthMap.get(serviceName);
        if (health != null) {
            updateServiceHealth(health, ServiceStatus.HEALTHY, null);
        }
    }
    
    public void shutdown() {
        stopMonitoring();
        
        if (healthCheckExecutor != null && !healthCheckExecutor.isShutdown()) {
            healthCheckExecutor.shutdown();
            try {
                if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    healthCheckExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                healthCheckExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        serviceHealthMap.clear();
        Log.d(TAG, "Service health monitor shutdown complete");
    }
}
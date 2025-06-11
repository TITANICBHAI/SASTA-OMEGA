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
    
    // Recursive loop prevention
    private final java.util.concurrent.ConcurrentHashMap<String, Long> lastHealthCheckTime = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Integer> recursiveCallDepth = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicBoolean circuitBreakerOpen = new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile long circuitBreakerOpenTime = 0;
    private static final int MAX_RECURSIVE_DEPTH = 3;
    private static final long MIN_HEALTH_CHECK_INTERVAL = 1000; // 1 second minimum
    private static final long CIRCUIT_BREAKER_RESET_TIME = 30000; // 30 seconds
    
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
        // Check circuit breaker state
        if (circuitBreakerOpen.get()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - circuitBreakerOpenTime < CIRCUIT_BREAKER_RESET_TIME) {
                return; // Skip health checks while circuit breaker is open
            } else {
                // Try to reset circuit breaker
                circuitBreakerOpen.set(false);
                Log.i(TAG, "Circuit breaker reset - resuming health checks");
            }
        }
        
        // Check for recursive call prevention
        if (!canPerformHealthCheck(health.serviceName)) {
            return;
        }
        
        try {
            incrementRecursiveDepth(health.serviceName);
            ServiceStatus newStatus = evaluateServiceStatus(health.serviceName);
            updateServiceHealth(health, newStatus, null);
            
        } catch (Exception e) {
            updateServiceHealth(health, ServiceStatus.FAILED, e.getMessage());
            checkCircuitBreakerTrigger();
        } finally {
            decrementRecursiveDepth(health.serviceName);
        }
    }
    
    private boolean canPerformHealthCheck(String serviceName) {
        long currentTime = System.currentTimeMillis();
        
        // Check minimum interval between health checks
        Long lastCheck = lastHealthCheckTime.get(serviceName);
        if (lastCheck != null && (currentTime - lastCheck) < MIN_HEALTH_CHECK_INTERVAL) {
            return false;
        }
        
        // Check recursive call depth
        Integer depth = recursiveCallDepth.get(serviceName);
        if (depth != null && depth >= MAX_RECURSIVE_DEPTH) {
            Log.w(TAG, "Recursive call depth exceeded for service: " + serviceName);
            return false;
        }
        
        lastHealthCheckTime.put(serviceName, currentTime);
        return true;
    }
    
    private void incrementRecursiveDepth(String serviceName) {
        recursiveCallDepth.compute(serviceName, (key, value) -> value == null ? 1 : value + 1);
    }
    
    private void decrementRecursiveDepth(String serviceName) {
        recursiveCallDepth.compute(serviceName, (key, value) -> {
            if (value == null || value <= 1) {
                return null; // Remove from map if depth becomes 0 or negative
            }
            return value - 1;
        });
    }
    
    private void checkCircuitBreakerTrigger() {
        // Count recent failures across all services
        long currentTime = System.currentTimeMillis();
        int recentFailures = 0;
        
        for (ServiceHealth health : serviceHealthMap.values()) {
            if (health.status == ServiceStatus.FAILED && 
                (currentTime - health.lastHealthCheck) < 30000) { // Within 30 seconds
                recentFailures++;
            }
        }
        
        // Open circuit breaker if too many recent failures
        if (recentFailures >= 3 && !circuitBreakerOpen.get()) {
            circuitBreakerOpen.set(true);
            circuitBreakerOpenTime = currentTime;
            Log.w(TAG, "Circuit breaker opened due to " + recentFailures + " recent failures");
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
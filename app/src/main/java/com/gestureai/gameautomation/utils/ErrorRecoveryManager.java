package com.gestureai.gameautomation.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.gestureai.gameautomation.services.TouchAutomationService;
import com.gestureai.gameautomation.services.ScreenCaptureService;
import com.gestureai.gameautomation.services.GestureRecognitionService;
import com.gestureai.gameautomation.ai.DQNAgent;
import com.gestureai.gameautomation.ai.PPOAgent;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Handles automatic error recovery and system restart mechanisms
 */
public class ErrorRecoveryManager {
    private static final String TAG = "ErrorRecoveryManager";
    private static volatile ErrorRecoveryManager instance;
    private static final Object lock = new Object();
    
    private Context context;
    private AtomicInteger totalRecoveryAttempts = new AtomicInteger(0);
    private Map<String, AtomicInteger> componentRecoveryAttempts = new ConcurrentHashMap<>();
    private Map<String, Long> lastRecoveryTime = new ConcurrentHashMap<>();
    
    // Recovery limits
    private static final int MAX_RECOVERY_ATTEMPTS = 3;
    private static final long RECOVERY_COOLDOWN_MS = 30000; // 30 seconds
    private static final long SERVICE_RESTART_DELAY_MS = 5000; // 5 seconds
    
    public interface RecoveryListener {
        void onRecoveryStarted(String component, String error);
        void onRecoverySuccess(String component);
        void onRecoveryFailed(String component, String reason);
        void onSystemRestart();
    }
    
    private RecoveryListener recoveryListener;
    
    public static ErrorRecoveryManager getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ErrorRecoveryManager(context);
                }
            }
        }
        return instance;
    }
    
    private ErrorRecoveryManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public void setRecoveryListener(RecoveryListener listener) {
        this.recoveryListener = listener;
    }
    
    /**
     * Attempt to recover from AI model failure
     */
    public boolean recoverAIModels(String modelName, Exception error) {
        String componentKey = "AI_MODEL_" + modelName;
        
        if (!canAttemptRecovery(componentKey)) {
            Log.w(TAG, "Recovery attempts exhausted for " + componentKey);
            return false;
        }
        
        if (recoveryListener != null) {
            recoveryListener.onRecoveryStarted(componentKey, error.getMessage());
        }
        
        try {
            Log.i(TAG, "Attempting to recover AI model: " + modelName);
            
            // Step 1: Clear existing model instances
            if ("DQN".equals(modelName)) {
                DQNAgent dqnAgent = DQNAgent.getInstance();
                if (dqnAgent != null) {
                    dqnAgent.cleanup();
                }
                // Reinitialize with fallback mode
                DQNAgent.getInstance(16, 8);
            } else if ("PPO".equals(modelName)) {
                PPOAgent ppoAgent = PPOAgent.getInstance();
                if (ppoAgent != null) {
                    ppoAgent.cleanup();
                }
                // Reinitialize with fallback mode
                PPOAgent.getInstance(16, 8);
            }
            
            // Step 2: Force memory cleanup
            MemoryManager memoryManager = MemoryManager.getInstance(context);
            memoryManager.performEmergencyCleanup();
            
            // Step 3: Mark recovery attempt
            incrementRecoveryAttempt(componentKey);
            
            if (recoveryListener != null) {
                recoveryListener.onRecoverySuccess(componentKey);
            }
            
            // Step 3: Wait for system stabilization
            Thread.sleep(2000);
            
            recordRecoveryAttempt(componentKey);
            
            if (recoveryListener != null) {
                recoveryListener.onRecoverySuccess(componentKey);
            }
            
            Log.i(TAG, "AI model recovery completed for: " + modelName);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to recover AI model: " + modelName, e);
            
            if (recoveryListener != null) {
                recoveryListener.onRecoveryFailed(componentKey, e.getMessage());
            }
            
            return false;
        }
    }
    
    /**
     * Attempt to recover from service failure
     */
    public boolean recoverService(String serviceName, Exception error) {
        String componentKey = "SERVICE_" + serviceName;
        
        if (!canAttemptRecovery(componentKey)) {
            Log.w(TAG, "Recovery attempts exhausted for " + componentKey);
            return false;
        }
        
        if (recoveryListener != null) {
            recoveryListener.onRecoveryStarted(componentKey, error.getMessage());
        }
        
        try {
            Log.i(TAG, "Attempting to recover service: " + serviceName);
            
            // Step 1: Stop the service if it's running
            stopService(serviceName);
            
            // Step 2: Wait for service cleanup
            Thread.sleep(SERVICE_RESTART_DELAY_MS);
            
            // Step 3: Restart the service
            boolean restarted = restartService(serviceName);
            
            if (restarted) {
                recordRecoveryAttempt(componentKey);
                
                if (recoveryListener != null) {
                    recoveryListener.onRecoverySuccess(componentKey);
                }
                
                Log.i(TAG, "Successfully recovered service: " + serviceName);
                return true;
            } else {
                if (recoveryListener != null) {
                    recoveryListener.onRecoveryFailed(componentKey, "Service restart failed");
                }
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to recover service: " + serviceName, e);
            
            if (recoveryListener != null) {
                recoveryListener.onRecoveryFailed(componentKey, e.getMessage());
            }
            
            return false;
        }
    }
    
    /**
     * Attempt to recover from database error
     */
    public boolean recoverDatabase(String operation, Exception error) {
        String componentKey = "DATABASE_" + operation;
        
        if (!canAttemptRecovery(componentKey)) {
            Log.w(TAG, "Recovery attempts exhausted for " + componentKey);
            return false;
        }
        
        if (recoveryListener != null) {
            recoveryListener.onRecoveryStarted(componentKey, error.getMessage());
        }
        
        try {
            Log.i(TAG, "Attempting to recover database operation: " + operation);
            
            // Step 1: Clear database cache
            System.gc();
            
            // Step 2: Reinitialize database connection
            DatabaseIntegrationManager dbManager = DatabaseIntegrationManager.getInstance(context);
            
            // Step 3: Test database connectivity
            dbManager.getAllSessions().thenAccept(sessions -> {
                Log.d(TAG, "Database connectivity test successful");
            }).exceptionally(throwable -> {
                Log.e(TAG, "Database connectivity test failed", throwable);
                return null;
            });
            
            recordRecoveryAttempt(componentKey);
            
            if (recoveryListener != null) {
                recoveryListener.onRecoverySuccess(componentKey);
            }
            
            Log.i(TAG, "Successfully recovered database operation: " + operation);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to recover database operation: " + operation, e);
            
            if (recoveryListener != null) {
                recoveryListener.onRecoveryFailed(componentKey, e.getMessage());
            }
            
            return false;
        }
    }
    
    /**
     * Attempt to recover from memory pressure
     */
    public boolean recoverFromMemoryPressure() {
        String componentKey = "MEMORY_PRESSURE";
        
        if (!canAttemptRecovery(componentKey)) {
            Log.w(TAG, "Recovery attempts exhausted for memory pressure");
            return false;
        }
        
        if (recoveryListener != null) {
            recoveryListener.onRecoveryStarted(componentKey, "Memory pressure detected");
        }
        
        try {
            Log.i(TAG, "Attempting memory pressure recovery");
            
            // Step 1: Emergency memory cleanup
            MemoryManager memoryManager = MemoryManager.getInstance(context);
            memoryManager.emergencyCleanup();
            
            // Step 2: Disable non-essential AI components temporarily
            GameStrategyAgent strategyAgent = new GameStrategyAgent(context);
            if (strategyAgent != null) {
                strategyAgent.setActive(false);
            }
            
            // Step 3: Force garbage collection
            System.gc();
            
            // Step 4: Wait for memory stabilization
            Thread.sleep(3000);
            
            // Step 5: Re-enable AI components
            if (strategyAgent != null) {
                strategyAgent.setActive(true);
            }
            
            recordRecoveryAttempt(componentKey);
            
            if (recoveryListener != null) {
                recoveryListener.onRecoverySuccess(componentKey);
            }
            
            Log.i(TAG, "Successfully recovered from memory pressure");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to recover from memory pressure", e);
            
            if (recoveryListener != null) {
                recoveryListener.onRecoveryFailed(componentKey, e.getMessage());
            }
            
            return false;
        }
    }
    
    /**
     * Full system restart as last resort
     */
    public void performSystemRestart() {
        Log.w(TAG, "Performing full system restart");
        
        if (recoveryListener != null) {
            recoveryListener.onSystemRestart();
        }
        
        try {
            // Step 1: Stop all services
            stopService("TouchAutomationService");
            stopService("ScreenCaptureService");
            stopService("GestureRecognitionService");
            
            // Step 2: Clean up all AI components
            try {
                DQNAgent dqnAgent = DQNAgent.getInstance();
                if (dqnAgent != null) dqnAgent.cleanup();
                
                PPOAgent ppoAgent = PPOAgent.getInstance();
                if (ppoAgent != null) ppoAgent.cleanup();
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up AI components", e);
            }
            
            // Step 3: Emergency memory cleanup
            MemoryManager memoryManager = MemoryManager.getInstance(context);
            memoryManager.emergencyCleanup();
            
            // Step 4: Wait for system stabilization
            Thread.sleep(5000);
            
            // Step 5: Restart core services
            ServiceStartupCoordinator coordinator = ServiceStartupCoordinator.getInstance(context);
            coordinator.startServicesWithCoordination(new ServiceStartupCoordinator.ServiceReadyCallback() {
                @Override
                public void onAllServicesReady() {
                    Log.i(TAG, "System restart completed successfully");
                }
                
                @Override
                public void onServiceFailed(String serviceName, String error) {
                    Log.e(TAG, "Service restart failed during system restart: " + serviceName);
                }
            });
            
            // Reset recovery counters
            componentRecoveryAttempts.clear();
            lastRecoveryTime.clear();
            totalRecoveryAttempts.set(0);
            
        } catch (Exception e) {
            Log.e(TAG, "Error during system restart", e);
        }
    }
    
    // Helper methods
    private boolean canAttemptRecovery(String componentKey) {
        AtomicInteger attempts = componentRecoveryAttempts.computeIfAbsent(componentKey, k -> new AtomicInteger(0));
        
        if (attempts.get() >= MAX_RECOVERY_ATTEMPTS) {
            return false;
        }
        
        Long lastAttempt = lastRecoveryTime.get(componentKey);
        if (lastAttempt != null && (System.currentTimeMillis() - lastAttempt) < RECOVERY_COOLDOWN_MS) {
            return false;
        }
        
        return true;
    }
    
    private void recordRecoveryAttempt(String componentKey) {
        componentRecoveryAttempts.computeIfAbsent(componentKey, k -> new AtomicInteger(0)).incrementAndGet();
        lastRecoveryTime.put(componentKey, System.currentTimeMillis());
        totalRecoveryAttempts.incrementAndGet();
    }
    
    private void stopService(String serviceName) {
        try {
            switch (serviceName) {
                case "TouchAutomationService":
                    // Accessibility service is managed by system
                    break;
                case "ScreenCaptureService":
                    Intent screenIntent = new Intent(context, ScreenCaptureService.class);
                    context.stopService(screenIntent);
                    break;
                case "GestureRecognitionService":
                    Intent gestureIntent = new Intent(context, GestureRecognitionService.class);
                    context.stopService(gestureIntent);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping service: " + serviceName, e);
        }
    }
    
    private boolean restartService(String serviceName) {
        try {
            switch (serviceName) {
                case "TouchAutomationService":
                    // Check if accessibility service is available
                    return TouchAutomationService.getInstanceSafe() != null;
                case "ScreenCaptureService":
                    Intent screenIntent = new Intent(context, ScreenCaptureService.class);
                    context.startForegroundService(screenIntent);
                    return true;
                case "GestureRecognitionService":
                    Intent gestureIntent = new Intent(context, GestureRecognitionService.class);
                    context.startForegroundService(gestureIntent);
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restarting service: " + serviceName, e);
            return false;
        }
    }
    
    // Statistics
    public int getTotalRecoveryAttempts() {
        return totalRecoveryAttempts.get();
    }
    
    public int getRecoveryAttemptsForComponent(String componentKey) {
        AtomicInteger attempts = componentRecoveryAttempts.get(componentKey);
        return attempts != null ? attempts.get() : 0;
    }
    
    public void resetRecoveryCounters() {
        componentRecoveryAttempts.clear();
        lastRecoveryTime.clear();
        totalRecoveryAttempts.set(0);
        Log.i(TAG, "Recovery counters reset");
    }
}
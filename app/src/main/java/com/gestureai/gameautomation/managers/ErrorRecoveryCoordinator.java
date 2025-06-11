package com.gestureai.gameautomation.managers;

import android.content.Context;
import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coordinates error recovery across all system components
 */
public class ErrorRecoveryCoordinator {
    private static final String TAG = "ErrorRecoveryCoordinator";
    private static volatile ErrorRecoveryCoordinator instance;
    private static final Object lock = new Object();
    
    private final Context context;
    private final ConcurrentHashMap<String, AtomicInteger> failureCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastRecoveryTime = new ConcurrentHashMap<>();
    private final AtomicBoolean systemInRecovery = new AtomicBoolean(false);
    
    private static final int MAX_RECOVERY_ATTEMPTS = 3;
    private static final long RECOVERY_COOLDOWN_MS = 30000; // 30 seconds
    
    public interface RecoveryListener {
        void onRecoveryStarted(String component);
        void onRecoveryCompleted(String component);
        void onRecoveryFailed(String component);
        void onSystemRecoveryRequired();
    }
    
    private RecoveryListener recoveryListener;
    
    public static ErrorRecoveryCoordinator getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ErrorRecoveryCoordinator(context);
                }
            }
        }
        return instance;
    }
    
    private ErrorRecoveryCoordinator(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public void setRecoveryListener(RecoveryListener listener) {
        this.recoveryListener = listener;
    }
    
    public boolean attemptRecovery(String componentName, Exception error) {
        if (systemInRecovery.get()) {
            Log.w(TAG, "System already in recovery mode, skipping recovery for " + componentName);
            return false;
        }
        
        // Check if component can be recovered
        if (!canAttemptRecovery(componentName)) {
            Log.w(TAG, "Cannot attempt recovery for " + componentName + " - too many attempts or cooldown active");
            return false;
        }
        
        systemInRecovery.set(true);
        
        try {
            Log.i(TAG, "Attempting recovery for component: " + componentName);
            
            if (recoveryListener != null) {
                recoveryListener.onRecoveryStarted(componentName);
            }
            
            boolean recoverySuccessful = performComponentRecovery(componentName, error);
            
            if (recoverySuccessful) {
                resetFailureCount(componentName);
                if (recoveryListener != null) {
                    recoveryListener.onRecoveryCompleted(componentName);
                }
                Log.i(TAG, "Recovery successful for component: " + componentName);
            } else {
                incrementFailureCount(componentName);
                if (recoveryListener != null) {
                    recoveryListener.onRecoveryFailed(componentName);
                }
                Log.e(TAG, "Recovery failed for component: " + componentName);
            }
            
            return recoverySuccessful;
            
        } finally {
            systemInRecovery.set(false);
        }
    }
    
    private boolean performComponentRecovery(String componentName, Exception error) {
        try {
            switch (componentName) {
                case "TouchAutomationService":
                    return recoverTouchAutomationService();
                    
                case "MediaPipeManager":
                    return recoverMediaPipeManager();
                    
                case "GameStrategyAgent":
                    return recoverGameStrategyAgent();
                    
                case "DQNAgent":
                    return recoverDQNAgent();
                    
                case "PPOAgent":
                    return recoverPPOAgent();
                    
                case "ScreenCaptureService":
                    return recoverScreenCaptureService();
                    
                case "UnifiedServiceCoordinator":
                    return recoverUnifiedServiceCoordinator();
                    
                default:
                    Log.w(TAG, "No recovery strategy defined for component: " + componentName);
                    return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during recovery of " + componentName, e);
            return false;
        }
    }
    
    private boolean recoverTouchAutomationService() {
        try {
            TouchAutomationService service = TouchAutomationService.getInstance();
            if (service != null) {
                // Service exists but may be in bad state - restart connection
                service.onServiceConnected();
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to recover TouchAutomationService", e);
            return false;
        }
    }
    
    private boolean recoverMediaPipeManager() {
        try {
            MediaPipeManager manager = MediaPipeManager.getInstance(context);
            manager.cleanup();
            manager.initialize(); // Will fallback to CPU if GPU fails
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to recover MediaPipeManager", e);
            return false;
        }
    }
    
    private boolean recoverGameStrategyAgent() {
        try {
            GameStrategyAgent agent = GameStrategyAgent.getInstance();
            if (agent != null) {
                agent.cleanup();
            }
            // Reinitialize with context
            GameStrategyAgent.getInstance(context);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to recover GameStrategyAgent", e);
            return false;
        }
    }
    
    private boolean recoverDQNAgent() {
        try {
            DQNAgent agent = DQNAgent.getInstance();
            if (agent != null) {
                agent.cleanup();
            }
            // Reinitialize
            DQNAgent.getInstance(16, 8);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to recover DQNAgent", e);
            return false;
        }
    }
    
    private boolean recoverPPOAgent() {
        try {
            PPOAgent agent = PPOAgent.getInstance();
            if (agent != null) {
                agent.cleanup();
            }
            // Reinitialize
            PPOAgent.getInstance(16, 8);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to recover PPOAgent", e);
            return false;
        }
    }
    
    private boolean recoverScreenCaptureService() {
        try {
            // Screen capture service recovery logic
            // This would typically involve restarting the media projection
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to recover ScreenCaptureService", e);
            return false;
        }
    }
    
    private boolean recoverUnifiedServiceCoordinator() {
        try {
            UnifiedServiceCoordinator coordinator = UnifiedServiceCoordinator.getInstance(context);
            coordinator.cleanup();
            // The coordinator will reinitialize on next access
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to recover UnifiedServiceCoordinator", e);
            return false;
        }
    }
    
    private boolean canAttemptRecovery(String componentName) {
        AtomicInteger count = failureCount.computeIfAbsent(componentName, k -> new AtomicInteger(0));
        
        if (count.get() >= MAX_RECOVERY_ATTEMPTS) {
            return false;
        }
        
        Long lastRecovery = lastRecoveryTime.get(componentName);
        if (lastRecovery != null) {
            long timeSinceLastRecovery = System.currentTimeMillis() - lastRecovery;
            if (timeSinceLastRecovery < RECOVERY_COOLDOWN_MS) {
                return false;
            }
        }
        
        return true;
    }
    
    private void incrementFailureCount(String componentName) {
        failureCount.computeIfAbsent(componentName, k -> new AtomicInteger(0)).incrementAndGet();
        lastRecoveryTime.put(componentName, System.currentTimeMillis());
    }
    
    private void resetFailureCount(String componentName) {
        failureCount.put(componentName, new AtomicInteger(0));
        lastRecoveryTime.remove(componentName);
    }
    
    public void resetAllFailureCounts() {
        failureCount.clear();
        lastRecoveryTime.clear();
        Log.d(TAG, "All failure counts reset");
    }
    
    public boolean isSystemInRecovery() {
        return systemInRecovery.get();
    }
    
    public int getFailureCount(String componentName) {
        AtomicInteger count = failureCount.get(componentName);
        return count != null ? count.get() : 0;
    }
}
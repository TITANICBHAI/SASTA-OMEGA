package com.gestureai.gameautomation.utils;

import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe singleton pattern implementation for all managers
 * Provides consistent initialization and lifecycle management
 */
public abstract class ThreadSafeManager {
    private static final String TAG = "ThreadSafeManager";
    
    // Global instance tracking for all managers
    private static final ConcurrentHashMap<Class<?>, Object> instances = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();
    
    // Instance state tracking
    protected final AtomicBoolean isInitialized = new AtomicBoolean(false);
    protected final AtomicBoolean isDestroyed = new AtomicBoolean(false);
    
    @SuppressWarnings("unchecked")
    protected static <T extends ThreadSafeManager> T getInstance(Class<T> clazz, InstanceFactory<T> factory) {
        T instance = (T) instances.get(clazz);
        
        if (instance == null) {
            ReentrantReadWriteLock lock = locks.computeIfAbsent(clazz, k -> new ReentrantReadWriteLock());
            lock.writeLock().lock();
            try {
                // Double-check pattern
                instance = (T) instances.get(clazz);
                if (instance == null) {
                    instance = factory.create();
                    if (instance != null) {
                        instances.put(clazz, instance);
                        Log.d(TAG, "Created new instance of " + clazz.getSimpleName());
                    }
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        return instance;
    }
    
    /**
     * Get existing instance without creating new one
     */
    @SuppressWarnings("unchecked")
    protected static <T extends ThreadSafeManager> T getExistingInstance(Class<T> clazz) {
        ReentrantReadWriteLock lock = locks.get(clazz);
        if (lock != null) {
            lock.readLock().lock();
            try {
                return (T) instances.get(clazz);
            } finally {
                lock.readLock().unlock();
            }
        }
        return (T) instances.get(clazz);
    }
    
    /**
     * Initialize the manager - must be implemented by subclasses
     */
    protected abstract boolean initializeInternal();
    
    /**
     * Cleanup the manager - must be implemented by subclasses
     */
    protected abstract void cleanupInternal();
    
    /**
     * Thread-safe initialization
     */
    public final boolean initialize() {
        if (isDestroyed.get()) {
            Log.w(TAG, "Cannot initialize destroyed manager: " + getClass().getSimpleName());
            return false;
        }
        
        if (isInitialized.compareAndSet(false, true)) {
            try {
                boolean result = initializeInternal();
                if (!result) {
                    isInitialized.set(false);
                    Log.e(TAG, "Failed to initialize " + getClass().getSimpleName());
                }
                return result;
            } catch (Exception e) {
                isInitialized.set(false);
                Log.e(TAG, "Exception during initialization of " + getClass().getSimpleName(), e);
                return false;
            }
        }
        
        return isInitialized.get();
    }
    
    /**
     * Thread-safe cleanup and removal from global registry
     */
    public final void destroy() {
        if (isDestroyed.compareAndSet(false, true)) {
            try {
                cleanupInternal();
                
                // Remove from global registry with proper synchronization
                Class<?> clazz = getClass();
                Object removed = instances.remove(clazz);
                
                ReentrantReadWriteLock lock = locks.remove(clazz);
                if (lock != null) {
                    // Ensure no threads are waiting and properly release lock resources
                    try {
                        if (lock.writeLock().tryLock(1, java.util.concurrent.TimeUnit.SECONDS)) {
                            lock.writeLock().unlock();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        Log.w(TAG, "Interrupted while waiting for lock during cleanup");
                    }
                }
                
                Log.d(TAG, "Destroyed and removed " + clazz.getSimpleName() + " (was present: " + (removed != null) + ")");
                
            } catch (Exception e) {
                Log.e(TAG, "Exception during cleanup of " + getClass().getSimpleName(), e);
            } finally {
                isInitialized.set(false);
            }
        }
    }
    
    /**
     * Emergency cleanup for all global instances - use only in critical memory situations
     */
    public static void emergencyCleanupAll() {
        Log.w(TAG, "Performing emergency cleanup of all ThreadSafeManager instances");
        
        try {
            // Force cleanup of all instances
            for (Object instance : instances.values()) {
                if (instance instanceof ThreadSafeManager) {
                    try {
                        ((ThreadSafeManager) instance).destroy();
                    } catch (Exception e) {
                        Log.e(TAG, "Error during emergency cleanup of " + instance.getClass().getSimpleName(), e);
                    }
                }
            }
            
            // Clear all global collections
            instances.clear();
            locks.clear();
            
            // Force garbage collection
            System.gc();
            
            Log.d(TAG, "Emergency cleanup completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Critical error during emergency cleanup", e);
        }
    }
    
    /**
     * Check if manager is properly initialized and not destroyed
     */
    public final boolean isReady() {
        return isInitialized.get() && !isDestroyed.get();
    }
    
    /**
     * Factory interface for creating instances
     */
    public interface InstanceFactory<T> {
        T create();
    }
    
    /**
     * Get all active manager instances for debugging
     */
    public static String getActiveManagers() {
        StringBuilder sb = new StringBuilder("Active Managers: ");
        for (Class<?> clazz : instances.keySet()) {
            sb.append(clazz.getSimpleName()).append(", ");
        }
        return sb.toString();
    }
    
    /**
     * Emergency cleanup of all managers
     */
    public static void destroyAllManagers() {
        Log.w(TAG, "Emergency cleanup of all managers");
        for (Object instance : instances.values()) {
            if (instance instanceof ThreadSafeManager) {
                ((ThreadSafeManager) instance).destroy();
            }
        }
        instances.clear();
        locks.clear();
    }
}
package com.gestureai.gameautomation.synchronization;

import android.util.Log;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;

/**
 * Synchronization manager for AI components to prevent race conditions
 * and ensure thread-safe access to shared neural network resources
 */
public class AIComponentSynchronizer {
    
    private static final String TAG = "AIComponentSynchronizer";
    private static volatile AIComponentSynchronizer instance;
    
    // Locks for different AI components
    private final ReadWriteLock neuralNetworkLock = new ReentrantReadWriteLock();
    private final ReadWriteLock trainingDataLock = new ReentrantReadWriteLock();
    private final ReadWriteLock gameStateLock = new ReentrantReadWriteLock();
    
    // Component state tracking
    private final Map<String, ComponentState> componentStates = new ConcurrentHashMap<>();
    private final AtomicReference<String> activeAIComponent = new AtomicReference<>("NONE");
    
    // Training coordination
    private volatile boolean isTrainingInProgress = false;
    private volatile String trainingComponent = null;
    
    private AIComponentSynchronizer() {
        Log.d(TAG, "AIComponentSynchronizer initialized");
    }
    
    public static AIComponentSynchronizer getInstance() {
        if (instance == null) {
            synchronized (AIComponentSynchronizer.class) {
                if (instance == null) {
                    instance = new AIComponentSynchronizer();
                }
            }
        }
        return instance;
    }
    
    /**
     * Acquire read lock for neural network access
     */
    public void acquireNeuralNetworkReadLock(String componentName) {
        Log.d(TAG, componentName + " acquiring neural network read lock");
        neuralNetworkLock.readLock().lock();
        updateComponentState(componentName, ComponentState.NEURAL_NETWORK_READ);
    }
    
    /**
     * Release read lock for neural network access
     */
    public void releaseNeuralNetworkReadLock(String componentName) {
        neuralNetworkLock.readLock().unlock();
        updateComponentState(componentName, ComponentState.IDLE);
        Log.d(TAG, componentName + " released neural network read lock");
    }
    
    /**
     * Acquire write lock for neural network modification
     */
    public boolean acquireNeuralNetworkWriteLock(String componentName, long timeoutMs) {
        Log.d(TAG, componentName + " attempting to acquire neural network write lock");
        try {
            if (neuralNetworkLock.writeLock().tryLock(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                updateComponentState(componentName, ComponentState.NEURAL_NETWORK_WRITE);
                activeAIComponent.set(componentName);
                Log.d(TAG, componentName + " acquired neural network write lock");
                return true;
            } else {
                Log.w(TAG, componentName + " failed to acquire neural network write lock within timeout");
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, componentName + " interrupted while waiting for neural network write lock", e);
            return false;
        }
    }
    
    /**
     * Release write lock for neural network modification
     */
    public void releaseNeuralNetworkWriteLock(String componentName) {
        neuralNetworkLock.writeLock().unlock();
        updateComponentState(componentName, ComponentState.IDLE);
        activeAIComponent.set("NONE");
        Log.d(TAG, componentName + " released neural network write lock");
    }
    
    /**
     * Begin coordinated training session
     */
    public synchronized boolean beginTrainingSession(String componentName) {
        if (isTrainingInProgress) {
            Log.w(TAG, componentName + " cannot start training - " + trainingComponent + " is already training");
            return false;
        }
        
        isTrainingInProgress = true;
        trainingComponent = componentName;
        updateComponentState(componentName, ComponentState.TRAINING);
        
        Log.d(TAG, componentName + " started training session");
        return true;
    }
    
    /**
     * End coordinated training session
     */
    public synchronized void endTrainingSession(String componentName) {
        if (trainingComponent != null && trainingComponent.equals(componentName)) {
            isTrainingInProgress = false;
            trainingComponent = null;
            updateComponentState(componentName, ComponentState.IDLE);
            Log.d(TAG, componentName + " ended training session");
        }
    }
    
    /**
     * Check if training is in progress
     */
    public boolean isTrainingInProgress() {
        return isTrainingInProgress;
    }
    
    /**
     * Get currently training component
     */
    public String getTrainingComponent() {
        return trainingComponent;
    }
    
    /**
     * Get current active AI component
     */
    public String getActiveAIComponent() {
        return activeAIComponent.get();
    }
    
    /**
     * Check if component can safely access neural network
     */
    public synchronized boolean canAccessNeuralNetwork(String componentName) {
        if (!isTrainingInProgress) {
            return true;
        }
        return trainingComponent != null && trainingComponent.equals(componentName);
    }
    
    private void updateComponentState(String componentName, ComponentState state) {
        componentStates.put(componentName, state);
    }
    
    public enum ComponentState {
        IDLE,
        NEURAL_NETWORK_READ,
        NEURAL_NETWORK_WRITE,
        TRAINING
    }
}
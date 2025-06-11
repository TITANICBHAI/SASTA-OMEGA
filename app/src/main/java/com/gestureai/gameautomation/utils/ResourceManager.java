package com.gestureai.gameautomation.utils;

import android.content.Context;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.ArrayList;

public class ResourceManager {
    private static final String TAG = "ResourceManager";
    private static ResourceManager instance;
    
    // Thread pool management
    private final ExecutorService aiProcessingPool;
    private final ExecutorService imageProcessingPool;
    private final ScheduledExecutorService scheduledPool;
    private final ExecutorService generalPool;
    
    // Resource tracking
    private final Map<String, WeakReference<Object>> managedResources;
    private final AtomicBoolean isShuttingDown;
    private final Context context;
    
    // Memory management
    private long lastMemoryCheck;
    private static final long MEMORY_CHECK_INTERVAL = 30000; // 30 seconds
    private static final double MEMORY_THRESHOLD = 0.85; // 85% memory usage
    
    public static synchronized ResourceManager getInstance(Context context) {
        if (instance == null) {
            instance = new ResourceManager(context.getApplicationContext());
        }
        return instance;
    }
    
    private ResourceManager(Context context) {
        this.context = context;
        this.managedResources = new ConcurrentHashMap<>();
        this.isShuttingDown = new AtomicBoolean(false);
        
        // Initialize thread pools with proper sizing
        int coreCount = Runtime.getRuntime().availableProcessors();
        
        this.aiProcessingPool = Executors.newFixedThreadPool(
            Math.max(2, coreCount / 2), 
            r -> {
                Thread t = new Thread(r, "AI-Processing-" + System.currentTimeMillis());
                t.setPriority(Thread.NORM_PRIORITY + 1);
                return t;
            }
        );
        
        this.imageProcessingPool = Executors.newFixedThreadPool(
            Math.max(1, coreCount / 4),
            r -> {
                Thread t = new Thread(r, "Image-Processing-" + System.currentTimeMillis());
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        );
        
        this.scheduledPool = Executors.newScheduledThreadPool(
            2,
            r -> {
                Thread t = new Thread(r, "Scheduled-" + System.currentTimeMillis());
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            }
        );
        
        this.generalPool = Executors.newCachedThreadPool(
            r -> {
                Thread t = new Thread(r, "General-" + System.currentTimeMillis());
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        );
        
        startMemoryMonitoring();
        Log.d(TAG, "ResourceManager initialized with " + coreCount + " cores");
    }
    
    // Thread pool accessors
    public ExecutorService getAIProcessingPool() {
        checkShutdown();
        return aiProcessingPool;
    }
    
    public ExecutorService getImageProcessingPool() {
        checkShutdown();
        return imageProcessingPool;
    }
    
    public ScheduledExecutorService getScheduledPool() {
        checkShutdown();
        return scheduledPool;
    }
    
    public ExecutorService getGeneralPool() {
        checkShutdown();
        return generalPool;
    }
    
    // Resource registration and tracking
    public void registerResource(String key, Object resource) {
        if (!isShuttingDown.get()) {
            managedResources.put(key, new WeakReference<>(resource));
            Log.v(TAG, "Registered resource: " + key);
        }
    }
    
    public void unregisterResource(String key) {
        WeakReference<Object> ref = managedResources.remove(key);
        if (ref != null) {
            Object resource = ref.get();
            if (resource != null) {
                cleanupResource(resource);
            }
            Log.v(TAG, "Unregistered resource: " + key);
        }
    }
    
    private void cleanupResource(Object resource) {
        try {
            if (resource instanceof AutoCloseable) {
                ((AutoCloseable) resource).close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error cleaning up resource", e);
        }
    }
    
    // Memory management
    private void startMemoryMonitoring() {
        scheduledPool.scheduleWithFixedDelay(
            this::checkMemoryUsage,
            MEMORY_CHECK_INTERVAL,
            MEMORY_CHECK_INTERVAL,
            TimeUnit.MILLISECONDS
        );
    }
    
    private void checkMemoryUsage() {
        if (isShuttingDown.get()) return;
        
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsage = (double) usedMemory / totalMemory;
        
        if (memoryUsage > MEMORY_THRESHOLD) {
            Log.w(TAG, "High memory usage detected: " + String.format("%.1f%%", memoryUsage * 100));
            performMemoryCleanup();
        }
        
        lastMemoryCheck = System.currentTimeMillis();
    }
    
    private void performMemoryCleanup() {
        Log.d(TAG, "Performing memory cleanup");
        
        // Clean up weak references
        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, WeakReference<Object>> entry : managedResources.entrySet()) {
            if (entry.getValue().get() == null) {
                keysToRemove.add(entry.getKey());
            }
        }
        
        for (String key : keysToRemove) {
            managedResources.remove(key);
        }
        
        // Suggest garbage collection
        System.gc();
        
        Log.d(TAG, "Memory cleanup completed. Removed " + keysToRemove.size() + " dead references");
    }
    
    // Thread pool monitoring
    public ThreadPoolStatus getThreadPoolStatus() {
        ThreadPoolStatus status = new ThreadPoolStatus();
        
        if (aiProcessingPool instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) aiProcessingPool;
            status.aiPoolActive = tpe.getActiveCount();
            status.aiPoolQueue = tpe.getQueue().size();
        }
        
        if (imageProcessingPool instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) imageProcessingPool;
            status.imagePoolActive = tpe.getActiveCount();
            status.imagePoolQueue = tpe.getQueue().size();
        }
        
        if (generalPool instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) generalPool;
            status.generalPoolActive = tpe.getActiveCount();
            status.generalPoolQueue = tpe.getQueue().size();
        }
        
        return status;
    }
    
    public static class ThreadPoolStatus {
        public int aiPoolActive = 0;
        public int aiPoolQueue = 0;
        public int imagePoolActive = 0;
        public int imagePoolQueue = 0;
        public int generalPoolActive = 0;
        public int generalPoolQueue = 0;
        
        @Override
        public String toString() {
            return String.format(
                "AI: %d/%d, Image: %d/%d, General: %d/%d",
                aiPoolActive, aiPoolQueue,
                imagePoolActive, imagePoolQueue,
                generalPoolActive, generalPoolQueue
            );
        }
    }
    
    // Memory status
    public MemoryStatus getMemoryStatus() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;
        
        MemoryStatus status = new MemoryStatus();
        status.totalMemory = totalMemory;
        status.freeMemory = freeMemory;
        status.usedMemory = usedMemory;
        status.maxMemory = maxMemory;
        status.usagePercentage = (double) usedMemory / totalMemory;
        status.managedResourceCount = managedResources.size();
        
        return status;
    }
    
    public static class MemoryStatus {
        public long totalMemory;
        public long freeMemory;
        public long usedMemory;
        public long maxMemory;
        public double usagePercentage;
        public int managedResourceCount;
        
        @Override
        public String toString() {
            return String.format(
                "Memory: %.1f%% (%d MB / %d MB), Resources: %d",
                usagePercentage * 100,
                usedMemory / (1024 * 1024),
                totalMemory / (1024 * 1024),
                managedResourceCount
            );
        }
    }
    
    // Shutdown management
    private void checkShutdown() {
        if (isShuttingDown.get()) {
            throw new IllegalStateException("ResourceManager is shutting down");
        }
    }
    
    public void shutdown() {
        if (isShuttingDown.compareAndSet(false, true)) {
            Log.d(TAG, "ResourceManager shutdown initiated");
            
            // Cleanup all managed resources
            for (Map.Entry<String, WeakReference<Object>> entry : managedResources.entrySet()) {
                Object resource = entry.getValue().get();
                if (resource != null) {
                    cleanupResource(resource);
                }
            }
            managedResources.clear();
            
            // Shutdown thread pools gracefully
            shutdownThreadPool(aiProcessingPool, "AI Processing");
            shutdownThreadPool(imageProcessingPool, "Image Processing");
            shutdownThreadPool(generalPool, "General");
            shutdownThreadPool(scheduledPool, "Scheduled");
            
            Log.d(TAG, "ResourceManager shutdown completed");
        }
    }
    
    private void shutdownThreadPool(ExecutorService pool, String name) {
        try {
            pool.shutdown();
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                Log.w(TAG, name + " pool did not terminate gracefully, forcing shutdown");
                pool.shutdownNow();
                if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
                    Log.e(TAG, name + " pool did not terminate after force shutdown");
                }
            } else {
                Log.d(TAG, name + " pool terminated gracefully");
            }
        } catch (InterruptedException e) {
            Log.w(TAG, name + " pool shutdown interrupted", e);
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Utility methods
    public void forceMemoryCleanup() {
        performMemoryCleanup();
    }
    
    public boolean isHealthy() {
        MemoryStatus memory = getMemoryStatus();
        ThreadPoolStatus pools = getThreadPoolStatus();
        
        return memory.usagePercentage < MEMORY_THRESHOLD && 
               !isShuttingDown.get() &&
               pools.aiPoolQueue < 50 && 
               pools.imagePoolQueue < 20;
    }
}
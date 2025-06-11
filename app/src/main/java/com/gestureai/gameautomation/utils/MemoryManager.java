package com.gestureai.gameautomation.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;

/**
 * Manages memory allocation and cleanup for AI components and computer vision operations
 */
public class MemoryManager {
    private static final String TAG = "MemoryManager";
    private static volatile MemoryManager instance;
    private static final Object lock = new Object();
    
    private Context context;
    private ActivityManager activityManager;
    private AtomicLong totalAllocatedMemory = new AtomicLong(0);
    private AtomicLong nd4jMemoryUsage = new AtomicLong(0);
    private AtomicLong bitmapMemoryUsage = new AtomicLong(0);
    
    // Memory tracking
    private Map<String, WeakReference<INDArray>> nd4jArrays = new ConcurrentHashMap<>();
    private Map<String, WeakReference<Bitmap>> bitmaps = new ConcurrentHashMap<>();
    
    // Memory thresholds
    private static final long MAX_ND4J_MEMORY = 256 * 1024 * 1024; // 256MB
    private static final long MAX_BITMAP_MEMORY = 128 * 1024 * 1024; // 128MB
    private static final float MEMORY_WARNING_THRESHOLD = 0.8f;
    private static final float MEMORY_CRITICAL_THRESHOLD = 0.95f;
    
    public interface MemoryWarningListener {
        void onMemoryWarning(String component, long currentUsage, long maxUsage);
        void onMemoryCritical(String component, long currentUsage, long maxUsage);
        void onOutOfMemory(String component);
    }
    
    private MemoryWarningListener memoryListener;
    
    public static MemoryManager getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new MemoryManager(context);
                }
            }
        }
        return instance;
    }
    
    private MemoryManager(Context context) {
        this.context = context.getApplicationContext();
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        startMemoryMonitoring();
    }
    
    public void setMemoryWarningListener(MemoryWarningListener listener) {
        this.memoryListener = listener;
    }
    
    // ND4J Memory Management
    public INDArray createND4JArray(String identifier, int... shape) {
        try {
            // Check memory before allocation
            long estimatedSize = calculateArraySize(shape) * 4; // 4 bytes per float
            
            if (nd4jMemoryUsage.get() + estimatedSize > MAX_ND4J_MEMORY) {
                Log.w(TAG, "ND4J memory limit would be exceeded, cleaning up old arrays");
                cleanupND4JArrays(false);
                
                if (nd4jMemoryUsage.get() + estimatedSize > MAX_ND4J_MEMORY) {
                    Log.e(TAG, "Cannot allocate ND4J array: " + identifier + " - performing emergency cleanup");
                    cleanupND4JArrays(true); // Force cleanup
                    System.gc();
                    if (memoryListener != null) {
                        memoryListener.onOutOfMemory("ND4J");
                    }
                    // Create smaller fallback array instead of returning null
                    Log.w(TAG, "Creating reduced-size fallback array for: " + identifier);
                    int[] fallbackShape = new int[shape.length];
                    for (int i = 0; i < shape.length; i++) {
                        fallbackShape[i] = Math.max(1, shape[i] / 2); // Half the size
                    }
                    INDArray fallbackArray = Nd4j.create(fallbackShape);
                    long fallbackSize = calculateArraySize(fallbackShape) * 4;
                    nd4jArrays.put(identifier + "_fallback", new WeakReference<>(fallbackArray));
                    nd4jMemoryUsage.addAndGet(fallbackSize);
                    totalAllocatedMemory.addAndGet(fallbackSize);
                    return fallbackArray;
                }
            }
            
            INDArray array = Nd4j.create(shape);
            nd4jArrays.put(identifier, new WeakReference<>(array));
            nd4jMemoryUsage.addAndGet(estimatedSize);
            totalAllocatedMemory.addAndGet(estimatedSize);
            
            Log.d(TAG, "Created ND4J array: " + identifier + ", size: " + estimatedSize);
            checkMemoryThresholds("ND4J", nd4jMemoryUsage.get(), MAX_ND4J_MEMORY);
            
            return array;
        } catch (Exception e) {
            Log.e(TAG, "Error creating ND4J array: " + identifier, e);
            if (memoryListener != null) {
                memoryListener.onOutOfMemory("ND4J");
            }
            Log.e(TAG, "Failed to create ND4J array - attempting recovery", e);
            cleanupND4JArrays(true);
            System.gc();
            return null; // Return null for graceful failure handling
        }
    }
    
    public void releaseND4JArray(String identifier) {
        WeakReference<INDArray> ref = nd4jArrays.remove(identifier);
        if (ref != null) {
            INDArray array = ref.get();
            if (array != null) {
                try {
                    long arraySize = array.length() * 4; // 4 bytes per float
                    array.close();
                    nd4jMemoryUsage.addAndGet(-arraySize);
                    totalAllocatedMemory.addAndGet(-arraySize);
                    Log.d(TAG, "Released ND4J array: " + identifier + ", freed: " + arraySize);
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing ND4J array: " + identifier, e);
                }
            }
        }
    }
    
    // Bitmap Memory Management
    public Bitmap createBitmap(String identifier, int width, int height, Bitmap.Config config) {
        try {
            long estimatedSize = width * height * getBytesPerPixel(config);
            
            if (bitmapMemoryUsage.get() + estimatedSize > MAX_BITMAP_MEMORY) {
                Log.w(TAG, "Bitmap memory limit would be exceeded, cleaning up old bitmaps");
                cleanupBitmaps(false);
                
                if (bitmapMemoryUsage.get() + estimatedSize > MAX_BITMAP_MEMORY) {
                    Log.e(TAG, "Cannot allocate bitmap: " + identifier + " - performing emergency cleanup");
                    cleanupBitmaps(true);
                    System.gc();
                    if (memoryListener != null) {
                        memoryListener.onOutOfMemory("Bitmap");
                    }
                    // Create smaller fallback bitmap instead of throwing OutOfMemoryError
                    int fallbackWidth = Math.max(1, width / 2);
                    int fallbackHeight = Math.max(1, height / 2);
                    Log.w(TAG, "Creating reduced-size fallback bitmap for: " + identifier);
                    Bitmap fallbackBitmap = Bitmap.createBitmap(fallbackWidth, fallbackHeight, config);
                    bitmaps.put(identifier + "_fallback", new WeakReference<>(fallbackBitmap));
                    return fallbackBitmap;
                }
            }
            
            Bitmap bitmap = Bitmap.createBitmap(width, height, config);
            bitmaps.put(identifier, new WeakReference<>(bitmap));
            bitmapMemoryUsage.addAndGet(estimatedSize);
            totalAllocatedMemory.addAndGet(estimatedSize);
            
            Log.d(TAG, "Created bitmap: " + identifier + ", size: " + estimatedSize);
            checkMemoryThresholds("Bitmap", bitmapMemoryUsage.get(), MAX_BITMAP_MEMORY);
            
            return bitmap;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "OutOfMemoryError creating bitmap: " + identifier, e);
            // Emergency recovery instead of crash
            emergencyCleanup();
            if (memoryListener != null) {
                memoryListener.onOutOfMemory("Bitmap");
            }
            // Return minimal fallback bitmap instead of crashing
            try {
                Bitmap fallbackBitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.RGB_565);
                bitmaps.put(identifier + "_emergency", new WeakReference<>(fallbackBitmap));
                Log.w(TAG, "Created emergency fallback bitmap for: " + identifier);
                return fallbackBitmap;
            } catch (Exception fallbackException) {
                Log.e(TAG, "Emergency fallback also failed", fallbackException);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating bitmap: " + identifier, e);
            if (memoryListener != null) {
                memoryListener.onOutOfMemory("Bitmap");
            }
            return null; // Return null instead of throwing RuntimeException
        }
    }
    
    public void releaseBitmap(String identifier) {
        WeakReference<Bitmap> ref = bitmaps.remove(identifier);
        if (ref != null) {
            Bitmap bitmap = ref.get();
            if (bitmap != null && !bitmap.isRecycled()) {
                try {
                    long bitmapSize = bitmap.getByteCount();
                    bitmap.recycle();
                    bitmapMemoryUsage.addAndGet(-bitmapSize);
                    totalAllocatedMemory.addAndGet(-bitmapSize);
                    Log.d(TAG, "Released bitmap: " + identifier + ", freed: " + bitmapSize);
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing bitmap: " + identifier, e);
                }
            }
        }
    }
    
    /**
     * Emergency cleanup method for critical memory situations
     */
    public void performEmergencyCleanup() {
        Log.w(TAG, "Performing emergency memory cleanup");
        
        try {
            // Force cleanup of all tracked arrays
            cleanupND4JArrays(true);
            cleanupBitmaps(true);
            
            // Force garbage collection
            System.gc();
            System.runFinalization();
            System.gc();
            
            // Reset memory counters
            nd4jMemoryUsage.set(0);
            bitmapMemoryUsage.set(0);
            totalAllocatedMemory.set(0);
            
            // Clear tracking maps
            nd4jArrays.clear();
            bitmaps.clear();
            
            Log.i(TAG, "Emergency cleanup completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during emergency cleanup", e);
        }
    }
    
    /**
     * Legacy emergency memory cleanup to prevent crashes - critical recovery mechanism
     */
    private void emergencyMemoryCleanup() {
        Log.w(TAG, "Starting emergency memory cleanup");
        
        try {
            // Force cleanup of all weak references
            cleanupND4JArrays(true);
            cleanupBitmaps(true);
            
            // Clear memory tracking counters
            nd4jMemoryUsage.set(0);
            bitmapMemoryUsage.set(0);
            totalAllocatedMemory.set(0);
            
            // Aggressive garbage collection
            System.gc();
            Thread.yield();
            System.gc();
            
            Log.w(TAG, "Emergency memory cleanup completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during emergency memory cleanup", e);
        }
    }

    // Memory Monitoring
    private void startMemoryMonitoring() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000); // Check every 10 seconds
                    
                    // Clean up weak references
                    cleanupND4JArrays(true);
                    cleanupBitmaps(true);
                    
                    // Check system memory
                    checkSystemMemory();
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in memory monitoring", e);
                }
            }
        }).start();
    }
    
    private void cleanupND4JArrays(boolean onlyWeakReferences) {
        for (Map.Entry<String, WeakReference<INDArray>> entry : nd4jArrays.entrySet()) {
            WeakReference<INDArray> ref = entry.getValue();
            INDArray array = ref.get();
            
            if (array == null) {
                // Weak reference was garbage collected
                nd4jArrays.remove(entry.getKey());
            } else if (!onlyWeakReferences) {
                // Force cleanup for memory pressure
                try {
                    long arraySize = array.length() * 4;
                    array.close();
                    nd4jMemoryUsage.addAndGet(-arraySize);
                    totalAllocatedMemory.addAndGet(-arraySize);
                    nd4jArrays.remove(entry.getKey());
                    Log.d(TAG, "Force cleaned ND4J array: " + entry.getKey());
                } catch (Exception e) {
                    Log.e(TAG, "Error force cleaning ND4J array", e);
                }
            }
        }
    }
    
    private void cleanupBitmaps(boolean onlyWeakReferences) {
        for (Map.Entry<String, WeakReference<Bitmap>> entry : bitmaps.entrySet()) {
            WeakReference<Bitmap> ref = entry.getValue();
            Bitmap bitmap = ref.get();
            
            if (bitmap == null || bitmap.isRecycled()) {
                // Weak reference was garbage collected or bitmap recycled
                bitmaps.remove(entry.getKey());
            } else if (!onlyWeakReferences) {
                // Force cleanup for memory pressure
                try {
                    long bitmapSize = bitmap.getByteCount();
                    bitmap.recycle();
                    bitmapMemoryUsage.addAndGet(-bitmapSize);
                    totalAllocatedMemory.addAndGet(-bitmapSize);
                    bitmaps.remove(entry.getKey());
                    Log.d(TAG, "Force cleaned bitmap: " + entry.getKey());
                } catch (Exception e) {
                    Log.e(TAG, "Error force cleaning bitmap", e);
                }
            }
        }
    }
    
    private void checkMemoryThresholds(String component, long currentUsage, long maxUsage) {
        float usagePercent = (float) currentUsage / maxUsage;
        
        if (usagePercent >= MEMORY_CRITICAL_THRESHOLD) {
            Log.w(TAG, component + " memory critical: " + usagePercent * 100 + "%");
            if (memoryListener != null) {
                memoryListener.onMemoryCritical(component, currentUsage, maxUsage);
            }
        } else if (usagePercent >= MEMORY_WARNING_THRESHOLD) {
            Log.w(TAG, component + " memory warning: " + usagePercent * 100 + "%");
            if (memoryListener != null) {
                memoryListener.onMemoryWarning(component, currentUsage, maxUsage);
            }
        }
    }
    
    private void checkSystemMemory() {
        if (activityManager != null) {
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memInfo);
            
            float availablePercent = (float) memInfo.availMem / memInfo.totalMem;
            
            if (availablePercent < 0.1f) { // Less than 10% available
                Log.w(TAG, "System memory critically low: " + (availablePercent * 100) + "% available");
                
                // Force cleanup of all managed memory
                cleanupND4JArrays(false);
                cleanupBitmaps(false);
                
                // Force garbage collection
                System.gc();
                
                if (memoryListener != null) {
                    memoryListener.onMemoryCritical("System", memInfo.availMem, memInfo.totalMem);
                }
            }
        }
    }
    
    // Emergency cleanup
    public synchronized void emergencyCleanup() {
        Log.w(TAG, "Performing emergency memory cleanup");
        
        cleanupND4JArrays(false);
        cleanupBitmaps(false);
        
        // Clear ND4J workspace if needed
        try {
            Nd4j.getWorkspaceManager().destroyAllWorkspacesForCurrentThread();
        } catch (Exception e) {
            Log.e(TAG, "Error clearing ND4J workspaces", e);
        }
        
        System.gc();
        
        Log.i(TAG, "Emergency cleanup completed. Memory usage reset.");
    }
    
    // Utility methods
    private long calculateArraySize(int... shape) {
        long size = 1;
        for (int dim : shape) {
            size *= dim;
        }
        return size;
    }
    
    private int getBytesPerPixel(Bitmap.Config config) {
        switch (config) {
            case ARGB_8888: return 4;
            case RGB_565: return 2;
            case ARGB_4444: return 2;
            case ALPHA_8: return 1;
            default: return 4;
        }
    }
    
    // Memory statistics
    public long getTotalAllocatedMemory() {
        return totalAllocatedMemory.get();
    }
    
    public long getND4JMemoryUsage() {
        return nd4jMemoryUsage.get();
    }
    
    public long getBitmapMemoryUsage() {
        return bitmapMemoryUsage.get();
    }
    
    public float getMemoryUsagePercent() {
        if (activityManager != null) {
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memInfo);
            return (float) totalAllocatedMemory.get() / memInfo.totalMem;
        }
        return 0.0f;
    }
}
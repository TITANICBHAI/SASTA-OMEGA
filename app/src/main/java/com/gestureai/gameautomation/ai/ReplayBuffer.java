package com.gestureai.gameautomation.ai;

import com.gestureai.gameautomation.GameAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.ref.WeakReference;

public class ReplayBuffer {
    private final Experience[] circularBuffer; // Fixed-size circular buffer to prevent fragmentation
    private final int maxSize;
    private final AtomicInteger writeIndex = new AtomicInteger(0);
    private final AtomicInteger currentSize = new AtomicInteger(0);
    private final Random random;
    
    // Memory management and fragmentation prevention
    private final AtomicLong totalMemoryUsed = new AtomicLong(0);
    private final AtomicInteger compactionCount = new AtomicInteger(0);
    private volatile long lastCompactionTime = 0;
    private static final long COMPACTION_INTERVAL = 300000; // 5 minutes
    private static final long MAX_MEMORY_PER_EXPERIENCE = 1024; // 1KB estimate
    private static final double FRAGMENTATION_THRESHOLD = 0.7;
    
    // Memory pool for experience objects to reduce allocation overhead
    private final java.util.concurrent.ConcurrentLinkedQueue<Experience> experiencePool = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final AtomicInteger poolSize = new AtomicInteger(0);
    private static final int MAX_POOL_SIZE = 1000;

    public ReplayBuffer(int maxSize) {
        this.maxSize = maxSize;
        this.circularBuffer = new Experience[maxSize]; // Pre-allocated array
        this.random = new Random();
        
        // Pre-populate experience pool
        initializeExperiencePool();
    }

    private void initializeExperiencePool() {
        // Pre-create experience objects to reduce GC pressure
        for (int i = 0; i < Math.min(MAX_POOL_SIZE, maxSize / 4); i++) {
            experiencePool.offer(new Experience(null, null, 0.0f, null, false));
            poolSize.incrementAndGet();
        }
    }
    
    private Experience getPooledExperience() {
        Experience pooled = experiencePool.poll();
        if (pooled != null) {
            poolSize.decrementAndGet();
            return pooled;
        }
        return new Experience(null, null, 0.0f, null, false);
    }
    
    private void returnToPool(Experience experience) {
        if (poolSize.get() < MAX_POOL_SIZE && experience != null) {
            // Clear references to prevent memory leaks
            experience.state = null;
            experience.action = null;
            experience.nextState = null;
            experience.reward = 0.0f;
            experience.gameOver = false;
            
            experiencePool.offer(experience);
            poolSize.incrementAndGet();
        }
    }

    public void addExperience(GameStrategyAgent.UniversalGameState state, GameAction action, 
                             float reward, GameStrategyAgent.UniversalGameState nextState, boolean gameOver) {
        
        // Check for memory pressure before adding
        if (shouldPerformMemoryCompaction()) {
            performMemoryCompaction();
        }
        
        // Get experience object from pool or create new
        Experience experience = getPooledExperience();
        experience.state = state;
        experience.action = action;
        experience.reward = reward;
        experience.nextState = nextState;
        experience.gameOver = gameOver;
        
        // Use circular buffer to prevent array shifts and fragmentation
        int index = writeIndex.getAndIncrement() % maxSize;
        
        // Return old experience to pool if overwriting
        Experience oldExperience = circularBuffer[index];
        if (oldExperience != null) {
            returnToPool(oldExperience);
            totalMemoryUsed.addAndGet(-MAX_MEMORY_PER_EXPERIENCE);
        } else {
            // Only increment size if we're not overwriting
            currentSize.set(Math.min(currentSize.get() + 1, maxSize));
        }
        
        circularBuffer[index] = experience;
        totalMemoryUsed.addAndGet(MAX_MEMORY_PER_EXPERIENCE);
    }

    private boolean shouldPerformMemoryCompaction() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastCompactionTime) > COMPACTION_INTERVAL ||
               (totalMemoryUsed.get() > maxSize * MAX_MEMORY_PER_EXPERIENCE * FRAGMENTATION_THRESHOLD);
    }
    
    private void performMemoryCompaction() {
        synchronized (this) {
            long startTime = System.currentTimeMillis();
            int compactedCount = 0;
            
            // Compact the circular buffer by removing null references
            for (int i = 0; i < maxSize; i++) {
                if (circularBuffer[i] != null) {
                    // Check if experience is still valid
                    Experience exp = circularBuffer[i];
                    if (exp.state == null && exp.action == null) {
                        returnToPool(exp);
                        circularBuffer[i] = null;
                        compactedCount++;
                    }
                }
            }
            
            lastCompactionTime = startTime;
            compactionCount.incrementAndGet();
            
            // Force garbage collection if significant compaction occurred
            if (compactedCount > maxSize * 0.1) {
                System.gc();
            }
            
            android.util.Log.d("ReplayBuffer", "Memory compaction completed: " + compactedCount + 
                              " experiences compacted in " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

    public List<Experience> sampleBatch(int batchSize) {
        List<Experience> batch = new ArrayList<>(batchSize);
        int actualSize = currentSize.get();
        
        if (actualSize == 0) {
            return batch;
        }
        
        int samplesToTake = Math.min(batchSize, actualSize);
        
        // Use reservoir sampling for better distribution
        for (int i = 0; i < samplesToTake; i++) {
            Experience sample = getValidExperience();
            if (sample != null) {
                batch.add(sample);
            }
        }
        
        return batch;
    }
    
    private Experience getValidExperience() {
        int attempts = 0;
        int maxAttempts = Math.min(currentSize.get() * 2, 100);
        
        while (attempts < maxAttempts) {
            int randomIndex = random.nextInt(maxSize);
            Experience exp = circularBuffer[randomIndex];
            
            if (exp != null && exp.state != null && exp.action != null) {
                return exp;
            }
            attempts++;
        }
        
        // Fallback: linear search for any valid experience
        for (int i = 0; i < maxSize; i++) {
            Experience exp = circularBuffer[i];
            if (exp != null && exp.state != null && exp.action != null) {
                return exp;
            }
        }
        
        return null;
    }

    public int size() {
        return currentSize.get();
    }
    
    public long getMemoryUsage() {
        return totalMemoryUsed.get();
    }
    
    public int getPoolSize() {
        return poolSize.get();
    }
    
    public int getCompactionCount() {
        return compactionCount.get();
    }

    public void clear() {
        synchronized (this) {
            // Return all experiences to pool
            for (int i = 0; i < maxSize; i++) {
                if (circularBuffer[i] != null) {
                    returnToPool(circularBuffer[i]);
                    circularBuffer[i] = null;
                }
            }
            
            currentSize.set(0);
            writeIndex.set(0);
            totalMemoryUsed.set(0);
            lastCompactionTime = 0;
        }
    }
}
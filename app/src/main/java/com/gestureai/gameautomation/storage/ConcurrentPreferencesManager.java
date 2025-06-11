package com.gestureai.gameautomation.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * High-concurrency SharedPreferences manager with corruption detection and recovery
 */
public class ConcurrentPreferencesManager {
    private static final String TAG = "ConcurrentPrefsManager";
    
    private final Context context;
    private final String preferencesName;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true); // Fair lock
    private volatile SharedPreferences preferences;
    
    // Corruption detection and recovery
    private final AtomicBoolean corruptionDetected = new AtomicBoolean(false);
    private final AtomicInteger corruptionCount = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Object> backupCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> keyWriteTimestamps = new ConcurrentHashMap<>();
    
    // Transaction management
    private final ThreadLocal<TransactionState> activeTransaction = new ThreadLocal<>();
    private final AtomicInteger transactionIdCounter = new AtomicInteger(0);
    
    // Constants
    private static final int MAX_CORRUPTION_COUNT = 3;
    private static final long CORRUPTION_RESET_TIME = 300000; // 5 minutes
    private static final String BACKUP_SUFFIX = "_backup";
    private static final String CHECKSUM_KEY = "_integrity_checksum";
    
    private static class TransactionState {
        final int transactionId;
        final ConcurrentHashMap<String, Object> pendingWrites;
        final Set<String> pendingRemovals;
        boolean isActive;
        
        TransactionState(int id) {
            this.transactionId = id;
            this.pendingWrites = new ConcurrentHashMap<>();
            this.pendingRemovals = ConcurrentHashMap.newKeySet();
            this.isActive = true;
        }
    }
    
    public ConcurrentPreferencesManager(@NonNull Context context, @NonNull String preferencesName) {
        this.context = context.getApplicationContext();
        this.preferencesName = preferencesName;
        initializePreferences();
    }
    
    private void initializePreferences() {
        lock.writeLock().lock();
        try {
            preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
            validateIntegrity();
            createBackup();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing preferences", e);
            handleCorruption(e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void validateIntegrity() {
        try {
            String currentChecksum = calculateChecksum();
            String storedChecksum = preferences.getString(CHECKSUM_KEY, null);
            
            if (storedChecksum != null && !currentChecksum.equals(storedChecksum)) {
                Log.w(TAG, "Preferences integrity check failed - possible corruption");
                corruptionDetected.set(true);
                handleCorruption(new IllegalStateException("Checksum mismatch"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error validating preferences integrity", e);
            handleCorruption(e);
        }
    }
    
    private String calculateChecksum() {
        try {
            StringBuilder data = new StringBuilder();
            Map<String, ?> allPrefs = preferences.getAll();
            
            for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
                if (!CHECKSUM_KEY.equals(entry.getKey())) {
                    data.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
                }
            }
            
            return String.valueOf(data.toString().hashCode());
        } catch (Exception e) {
            Log.e(TAG, "Error calculating checksum", e);
            return "0";
        }
    }
    
    private void createBackup() {
        try {
            Map<String, ?> allPrefs = preferences.getAll();
            backupCache.clear();
            backupCache.putAll((Map<String, Object>) allPrefs);
            
            File backupFile = new File(context.getFilesDir(), preferencesName + BACKUP_SUFFIX);
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(backupFile))) {
                oos.writeObject(allPrefs);
            }
            
            Log.d(TAG, "Created backup with " + backupCache.size() + " entries");
        } catch (Exception e) {
            Log.e(TAG, "Error creating backup", e);
        }
    }
    
    private void handleCorruption(Exception cause) {
        int count = corruptionCount.incrementAndGet();
        Log.e(TAG, "Handling corruption #" + count, cause);
        
        if (count >= MAX_CORRUPTION_COUNT) {
            Log.e(TAG, "Maximum corruption count reached, attempting recovery");
            attemptRecovery();
        }
        
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            corruptionCount.set(0);
            corruptionDetected.set(false);
            Log.i(TAG, "Corruption count reset");
        }, CORRUPTION_RESET_TIME);
    }
    
    private void attemptRecovery() {
        lock.writeLock().lock();
        try {
            Log.i(TAG, "Attempting preferences recovery");
            
            if (restoreFromFileBackup()) {
                Log.i(TAG, "Recovery successful from file backup");
                return;
            }
            
            if (restoreFromMemoryBackup()) {
                Log.i(TAG, "Recovery successful from memory backup");
                return;
            }
            
            Log.w(TAG, "All recovery attempts failed, clearing preferences");
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            commitSafely(editor);
            
        } catch (Exception e) {
            Log.e(TAG, "Critical error during recovery", e);
        } finally {
            corruptionDetected.set(false);
            lock.writeLock().unlock();
        }
    }
    
    private boolean restoreFromFileBackup() {
        try {
            File backupFile = new File(context.getFilesDir(), preferencesName + BACKUP_SUFFIX);
            if (!backupFile.exists()) {
                return false;
            }
            
            Map<String, Object> backupData;
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(backupFile))) {
                backupData = (Map<String, Object>) ois.readObject();
            }
            
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            
            for (Map.Entry<String, Object> entry : backupData.entrySet()) {
                putValue(editor, entry.getKey(), entry.getValue());
            }
            
            return commitSafely(editor);
            
        } catch (Exception e) {
            Log.e(TAG, "Error restoring from file backup", e);
            return false;
        }
    }
    
    private boolean restoreFromMemoryBackup() {
        try {
            if (backupCache.isEmpty()) {
                return false;
            }
            
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            
            for (Map.Entry<String, Object> entry : backupCache.entrySet()) {
                putValue(editor, entry.getKey(), entry.getValue());
            }
            
            return commitSafely(editor);
            
        } catch (Exception e) {
            Log.e(TAG, "Error restoring from memory backup", e);
            return false;
        }
    }
    
    private void putValue(SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Set) {
            editor.putStringSet(key, (Set<String>) value);
        }
    }
    
    private boolean commitSafely(SharedPreferences.Editor editor) {
        try {
            boolean success = editor.commit();
            if (success) {
                String newChecksum = calculateChecksum();
                editor.putString(CHECKSUM_KEY, newChecksum);
                editor.commit();
            }
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error during safe commit", e);
            return false;
        }
    }
    
    public Transaction beginTransaction() {
        int transactionId = transactionIdCounter.incrementAndGet();
        TransactionState state = new TransactionState(transactionId);
        activeTransaction.set(state);
        
        Log.d(TAG, "Started transaction " + transactionId);
        return new Transaction(state);
    }
    
    public class Transaction {
        private final TransactionState state;
        
        Transaction(TransactionState state) {
            this.state = state;
        }
        
        public Transaction putString(String key, String value) {
            if (state.isActive) {
                state.pendingWrites.put(key, value);
                keyWriteTimestamps.put(key, System.currentTimeMillis());
            }
            return this;
        }
        
        public Transaction putInt(String key, int value) {
            if (state.isActive) {
                state.pendingWrites.put(key, value);
                keyWriteTimestamps.put(key, System.currentTimeMillis());
            }
            return this;
        }
        
        public Transaction putBoolean(String key, boolean value) {
            if (state.isActive) {
                state.pendingWrites.put(key, value);
                keyWriteTimestamps.put(key, System.currentTimeMillis());
            }
            return this;
        }
        
        public Transaction remove(String key) {
            if (state.isActive) {
                state.pendingRemovals.add(key);
                state.pendingWrites.remove(key);
            }
            return this;
        }
        
        public boolean commit() {
            if (!state.isActive) {
                Log.w(TAG, "Attempting to commit inactive transaction " + state.transactionId);
                return false;
            }
            
            lock.writeLock().lock();
            try {
                if (corruptionDetected.get()) {
                    Log.w(TAG, "Skipping commit due to detected corruption");
                    return false;
                }
                
                SharedPreferences.Editor editor = preferences.edit();
                
                for (String key : state.pendingRemovals) {
                    editor.remove(key);
                    backupCache.remove(key);
                }
                
                for (Map.Entry<String, Object> entry : state.pendingWrites.entrySet()) {
                    putValue(editor, entry.getKey(), entry.getValue());
                    backupCache.put(entry.getKey(), entry.getValue());
                }
                
                boolean success = commitSafely(editor);
                if (success) {
                    Log.d(TAG, "Transaction " + state.transactionId + " committed successfully");
                } else {
                    Log.e(TAG, "Transaction " + state.transactionId + " commit failed");
                }
                
                return success;
                
            } catch (Exception e) {
                Log.e(TAG, "Error committing transaction " + state.transactionId, e);
                handleCorruption(e);
                return false;
            } finally {
                state.isActive = false;
                activeTransaction.remove();
                lock.writeLock().unlock();
            }
        }
        
        public void rollback() {
            state.isActive = false;
            activeTransaction.remove();
            Log.d(TAG, "Transaction " + state.transactionId + " rolled back");
        }
    }
    
    public String getString(String key, String defaultValue) {
        lock.readLock().lock();
        try {
            if (corruptionDetected.get()) {
                return (String) backupCache.getOrDefault(key, defaultValue);
            }
            return preferences.getString(key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Error reading string for key: " + key, e);
            handleCorruption(e);
            return (String) backupCache.getOrDefault(key, defaultValue);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public int getInt(String key, int defaultValue) {
        lock.readLock().lock();
        try {
            if (corruptionDetected.get()) {
                Object value = backupCache.get(key);
                return value instanceof Integer ? (Integer) value : defaultValue;
            }
            return preferences.getInt(key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Error reading int for key: " + key, e);
            handleCorruption(e);
            Object value = backupCache.get(key);
            return value instanceof Integer ? (Integer) value : defaultValue;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        lock.readLock().lock();
        try {
            if (corruptionDetected.get()) {
                Object value = backupCache.get(key);
                return value instanceof Boolean ? (Boolean) value : defaultValue;
            }
            return preferences.getBoolean(key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Error reading boolean for key: " + key, e);
            handleCorruption(e);
            Object value = backupCache.get(key);
            return value instanceof Boolean ? (Boolean) value : defaultValue;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public boolean contains(String key) {
        lock.readLock().lock();
        try {
            if (corruptionDetected.get()) {
                return backupCache.containsKey(key);
            }
            return preferences.contains(key);
        } catch (Exception e) {
            Log.e(TAG, "Error checking key existence: " + key, e);
            handleCorruption(e);
            return backupCache.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void shutdown() {
        lock.writeLock().lock();
        try {
            createBackup();
            activeTransaction.remove();
            Log.d(TAG, "ConcurrentPreferencesManager shutdown completed");
        } finally {
            lock.writeLock().unlock();
        }
    }
}
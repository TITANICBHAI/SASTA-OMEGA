package com.gestureai.gameautomation.database;

import android.content.Context;
import android.util.Log;
import androidx.room.Room;
import com.gestureai.gameautomation.data.SessionData;
import com.gestureai.gameautomation.data.UniversalGameState;
import com.gestureai.gameautomation.data.GameContext;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;

/**
 * Manages database operations and provides real-time data to UI components
 */
public class DatabaseIntegrationManager {
    private static final String TAG = "DatabaseIntegrationManager";
    private static volatile DatabaseIntegrationManager instance;
    private static final Object lock = new Object();
    
    private Context context;
    private AppDatabase database;
    private GestureDatabase gestureDatabase;
    private ExecutorService executorService;
    
    // Transaction coordination and deadlock prevention
    private final java.util.concurrent.locks.ReentrantReadWriteLock crossDbLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
    private final Object transactionLock = new Object();
    private final java.util.concurrent.atomic.AtomicBoolean transactionInProgress = new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile long lastTransactionTime = 0;
    private static final long TRANSACTION_TIMEOUT_MS = 5000;
    
    // Real-time data callbacks
    public interface DataUpdateListener {
        void onSessionDataUpdated(List<SessionData> sessions);
        void onPerformanceMetricsUpdated(float successRate, float avgReactionTime, int totalActions);
        void onGameStateUpdated(UniversalGameState gameState);
        void onDatabaseError(String error);
    }
    
    private DataUpdateListener dataListener;
    
    public static DatabaseIntegrationManager getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new DatabaseIntegrationManager(context);
                }
            }
        }
        return instance;
    }
    
    private DatabaseIntegrationManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newFixedThreadPool(2);
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        try {
            database = AppDatabase.getInstance(context);
            gestureDatabase = GestureDatabase.getInstance(context);
            
            if (database == null || gestureDatabase == null) {
                throw new RuntimeException("Failed to initialize one or both databases");
            }
            
            Log.d(TAG, "Both databases initialized successfully with coordination");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize databases", e);
            if (dataListener != null) {
                dataListener.onDatabaseError("Database initialization failed: " + e.getMessage());
            }
        }
    }
    
    /**
     * Execute cross-database transaction with deadlock prevention
     */
    public <T> CompletableFuture<T> executeCoordinatedTransaction(CoordinatedTransactionCallback<T> callback) {
        return CompletableFuture.supplyAsync(() -> {
            // Acquire write lock for cross-database coordination
            crossDbLock.writeLock().lock();
            try {
                synchronized (transactionLock) {
                    // Check for existing transaction timeout
                    long currentTime = System.currentTimeMillis();
                    if (transactionInProgress.get() && (currentTime - lastTransactionTime > TRANSACTION_TIMEOUT_MS)) {
                        Log.w(TAG, "Previous transaction timed out, forcing cleanup");
                        transactionInProgress.set(false);
                    }
                    
                    if (transactionInProgress.get()) {
                        throw new RuntimeException("Transaction already in progress, preventing deadlock");
                    }
                    
                    transactionInProgress.set(true);
                    lastTransactionTime = currentTime;
                }
                
                try {
                    return callback.execute(database, gestureDatabase);
                } finally {
                    synchronized (transactionLock) {
                        transactionInProgress.set(false);
                        lastTransactionTime = 0;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in coordinated transaction", e);
                throw new RuntimeException("Transaction failed", e);
            } finally {
                crossDbLock.writeLock().unlock();
            }
        }, executorService);
    }
    
    /**
     * Interface for cross-database transactions
     */
    public interface CoordinatedTransactionCallback<T> {
        T execute(AppDatabase appDb, GestureDatabase gestureDb) throws Exception;
    }
    
    /**
     * Save session data with cross-database coordination
     */
    public CompletableFuture<Void> saveSessionDataCoordinated(SessionData sessionData) {
        return executeCoordinatedTransaction((appDb, gestureDb) -> {
            try {
                // Begin transaction on both databases
                appDb.runInTransaction(() -> {
                    appDb.sessionDataDao().insert(sessionData);
                });
                
                gestureDb.runInTransaction(() -> {
                    // Update related gesture data if needed
                    if (sessionData.gestureCount > 0) {
                        // Sync gesture statistics
                        Log.d(TAG, "Syncing gesture data for session: " + sessionData.id);
                    }
                });
                
                Log.d(TAG, "Session data saved with cross-database coordination");
                
                // Notify UI of updated data
                if (dataListener != null) {
                    List<SessionData> allSessions = appDb.sessionDataDao().getAllSessions();
                    dataListener.onSessionDataUpdated(allSessions);
                }
                
                return null;
            } catch (Exception e) {
                Log.e(TAG, "Error in coordinated session save", e);
                throw e;
            }
        });
    }
    
    public void setDataUpdateListener(DataUpdateListener listener) {
        this.dataListener = listener;
    }
    
    // Session Data Operations
    public CompletableFuture<Void> saveSessionData(SessionData sessionData) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (database != null) {
                    database.sessionDataDao().insert(sessionData);
                    Log.d(TAG, "Session data saved successfully");
                    
                    // Notify UI of updated data
                    if (dataListener != null) {
                        List<SessionData> allSessions = database.sessionDataDao().getAllSessions();
                        dataListener.onSessionDataUpdated(allSessions);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving session data", e);
                if (dataListener != null) {
                    dataListener.onDatabaseError("Failed to save session: " + e.getMessage());
                }
            }
        }, executorService);
    }
    
    public CompletableFuture<List<SessionData>> getAllSessions() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (database != null) {
                    return database.sessionDataDao().getAllSessions();
                }
                return List.of();
            } catch (Exception e) {
                Log.e(TAG, "Error retrieving sessions", e);
                return List.of();
            }
        }, executorService);
    }
    
    public CompletableFuture<List<SessionData>> getSessionsByGameType(String gameType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (database != null) {
                    return database.sessionDataDao().getSessionsByGameType(gameType);
                }
                return List.of();
            } catch (Exception e) {
                Log.e(TAG, "Error retrieving sessions by game type", e);
                return List.of();
            }
        }, executorService);
    }
    
    public CompletableFuture<SessionData> getLatestSession() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (database != null) {
                    List<SessionData> sessions = database.sessionDataDao().getLatestSessions(1);
                    return sessions.isEmpty() ? null : sessions.get(0);
                }
                return null;
            } catch (Exception e) {
                Log.e(TAG, "Error retrieving latest session", e);
                return null;
            }
        }, executorService);
    }
    
    // Performance Analytics
    public CompletableFuture<Float> calculateOverallSuccessRate() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (database != null) {
                    List<SessionData> sessions = database.sessionDataDao().getAllSessions();
                    if (sessions.isEmpty()) return 0.0f;
                    
                    float totalSuccessRate = 0;
                    for (SessionData session : sessions) {
                        totalSuccessRate += session.getSuccessRate();
                    }
                    
                    float result = totalSuccessRate / sessions.size();
                    
                    // Notify UI of performance metrics
                    if (dataListener != null) {
                        calculateAndNotifyPerformanceMetrics();
                    }
                    
                    return result;
                }
                return 0.0f;
            } catch (Exception e) {
                Log.e(TAG, "Error calculating success rate", e);
                return 0.0f;
            }
        }, executorService);
    }
    
    private void calculateAndNotifyPerformanceMetrics() {
        executorService.execute(() -> {
            try {
                if (database != null) {
                    List<SessionData> sessions = database.sessionDataDao().getAllSessions();
                    
                    if (!sessions.isEmpty()) {
                        float avgSuccessRate = 0;
                        float avgReactionTime = 0;
                        int totalActions = 0;
                        
                        for (SessionData session : sessions) {
                            avgSuccessRate += session.getSuccessRate();
                            avgReactionTime += session.getAverageReactionTime();
                            totalActions += session.getTotalActions();
                        }
                        
                        avgSuccessRate /= sessions.size();
                        avgReactionTime /= sessions.size();
                        
                        if (dataListener != null) {
                            dataListener.onPerformanceMetricsUpdated(avgSuccessRate, avgReactionTime, totalActions);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating performance metrics", e);
            }
        });
    }
    
    // Game State Operations
    public CompletableFuture<Void> updateGameState(UniversalGameState gameState) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Store game state in current session
                SessionData currentSession = getCurrentSession();
                if (currentSession != null) {
                    currentSession.setGameType(gameState.getCurrentGameType());
                    database.sessionDataDao().update(currentSession);
                    
                    // Notify UI of game state update
                    if (dataListener != null) {
                        dataListener.onGameStateUpdated(gameState);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating game state", e);
            }
        }, executorService);
    }
    
    private SessionData getCurrentSession() {
        try {
            if (database != null) {
                List<SessionData> sessions = database.sessionDataDao().getLatestSessions(1);
                return sessions.isEmpty() ? null : sessions.get(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting current session", e);
        }
        return null;
    }
    
    // Real-time Data Monitoring
    public void startRealTimeMonitoring() {
        executorService.execute(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // Update every 5 seconds
                    
                    if (dataListener != null && database != null) {
                        // Get latest session data
                        List<SessionData> sessions = database.sessionDataDao().getLatestSessions(10);
                        dataListener.onSessionDataUpdated(sessions);
                        
                        // Calculate and notify performance metrics
                        calculateAndNotifyPerformanceMetrics();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in real-time monitoring", e);
                }
            }
        });
    }
    
    public void stopRealTimeMonitoring() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    // Analytics Queries for Dashboard
    public CompletableFuture<List<SessionData>> getSessionsInDateRange(long startTime, long endTime) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (database != null) {
                    return database.sessionDataDao().getSessionsInDateRange(startTime, endTime);
                }
                return List.of();
            } catch (Exception e) {
                Log.e(TAG, "Error retrieving sessions in date range", e);
                return List.of();
            }
        }, executorService);
    }
    
    public CompletableFuture<Float> getSuccessRateForGameType(String gameType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (database != null) {
                    List<SessionData> sessions = database.sessionDataDao().getSessionsByGameType(gameType);
                    if (sessions.isEmpty()) return 0.0f;
                    
                    float totalSuccessRate = 0;
                    for (SessionData session : sessions) {
                        totalSuccessRate += session.getSuccessRate();
                    }
                    return totalSuccessRate / sessions.size();
                }
                return 0.0f;
            } catch (Exception e) {
                Log.e(TAG, "Error calculating success rate for game type", e);
                return 0.0f;
            }
        }, executorService);
    }
    
    // Export functionality for analytics
    public CompletableFuture<String> exportSessionDataAsJson() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (database != null) {
                    List<SessionData> sessions = database.sessionDataDao().getAllSessions();
                    // Convert to JSON format for export
                    StringBuilder json = new StringBuilder();
                    json.append("[");
                    for (int i = 0; i < sessions.size(); i++) {
                        SessionData session = sessions.get(i);
                        json.append("{");
                        json.append("\"sessionId\":").append(session.getSessionId()).append(",");
                        json.append("\"gameType\":\"").append(session.getGameType()).append("\",");
                        json.append("\"successRate\":").append(session.getSuccessRate()).append(",");
                        json.append("\"totalActions\":").append(session.getTotalActions()).append(",");
                        json.append("\"sessionDuration\":").append(session.getSessionDuration());
                        json.append("}");
                        if (i < sessions.size() - 1) json.append(",");
                    }
                    json.append("]");
                    return json.toString();
                }
                return "[]";
            } catch (Exception e) {
                Log.e(TAG, "Error exporting session data", e);
                return "[]";
            }
        }, executorService);
    }
}
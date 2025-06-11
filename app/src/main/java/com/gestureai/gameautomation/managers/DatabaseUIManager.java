package com.gestureai.gameautomation.managers;

import android.content.Context;
import android.util.Log;
import com.gestureai.gameautomation.database.GestureDatabase;
import com.gestureai.gameautomation.database.entities.GestureData;
import com.gestureai.gameautomation.database.entities.SessionData;
import com.gestureai.gameautomation.database.entities.TrainingData;
import com.gestureai.gameautomation.database.dao.GestureDataDao;
import com.gestureai.gameautomation.database.dao.SessionDataDao;
import com.gestureai.gameautomation.database.dao.TrainingDataDao;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseUIManager {
    private static final String TAG = "DatabaseUIManager";
    private static DatabaseUIManager instance;
    
    private Context context;
    private GestureDatabase database;
    private ExecutorService databaseExecutor;
    private DatabaseOperationListener listener;
    
    public interface DatabaseOperationListener {
        void onDataLoaded(String dataType, List<?> data);
        void onDataSaved(String dataType, boolean success);
        void onDataDeleted(String dataType, boolean success);
        void onDatabaseError(String operation, String error);
    }
    
    private DatabaseUIManager(Context context) {
        this.context = context.getApplicationContext();
        this.databaseExecutor = Executors.newSingleThreadExecutor();
        initializeDatabase();
    }
    
    public static synchronized DatabaseUIManager getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseUIManager(context);
        }
        return instance;
    }
    
    private void initializeDatabase() {
        try {
            database = GestureDatabase.getDatabase(context);
            Log.d(TAG, "Database initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize database", e);
            notifyDatabaseError("initialization", e.getMessage());
        }
    }
    
    public void setDatabaseOperationListener(DatabaseOperationListener listener) {
        this.listener = listener;
    }
    
    // Gesture Data Operations
    public void loadAllGestureData() {
        databaseExecutor.execute(() -> {
            try {
                if (database != null) {
                    GestureDataDao dao = database.gestureDataDao();
                    List<GestureData> gestureData = dao.getAllGestureData();
                    notifyDataLoaded("GestureData", gestureData);
                    Log.d(TAG, "Loaded " + gestureData.size() + " gesture data records");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading gesture data", e);
                notifyDatabaseError("loadGestureData", e.getMessage());
            }
        });
    }
    
    public void saveGestureData(GestureData gestureData) {
        databaseExecutor.execute(() -> {
            try {
                if (database != null) {
                    GestureDataDao dao = database.gestureDataDao();
                    dao.insertGestureData(gestureData);
                    notifyDataSaved("GestureData", true);
                    Log.d(TAG, "Gesture data saved successfully");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving gesture data", e);
                notifyDataSaved("GestureData", false);
                notifyDatabaseError("saveGestureData", e.getMessage());
            }
        });
    }
    
    public void deleteGestureData(int gestureId) {
        databaseExecutor.execute(() -> {
            try {
                if (database != null) {
                    GestureDataDao dao = database.gestureDataDao();
                    dao.deleteGestureData(gestureId);
                    notifyDataDeleted("GestureData", true);
                    Log.d(TAG, "Gesture data deleted successfully");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting gesture data", e);
                notifyDataDeleted("GestureData", false);
                notifyDatabaseError("deleteGestureData", e.getMessage());
            }
        });
    }
    
    // Session Data Operations
    public void loadAllSessionData() {
        databaseExecutor.execute(() -> {
            try {
                if (database != null) {
                    SessionDataDao dao = database.sessionDataDao();
                    List<SessionData> sessionData = dao.getAllSessions();
                    notifyDataLoaded("SessionData", sessionData);
                    Log.d(TAG, "Loaded " + sessionData.size() + " session data records");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading session data", e);
                notifyDatabaseError("loadSessionData", e.getMessage());
            }
        });
    }
    
    public void saveSessionData(SessionData sessionData) {
        databaseExecutor.execute(() -> {
            try {
                if (database != null) {
                    SessionDataDao dao = database.sessionDataDao();
                    dao.insertSession(sessionData);
                    notifyDataSaved("SessionData", true);
                    Log.d(TAG, "Session data saved successfully");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving session data", e);
                notifyDataSaved("SessionData", false);
                notifyDatabaseError("saveSessionData", e.getMessage());
            }
        });
    }
    
    // Training Data Operations
    public void loadAllTrainingData() {
        databaseExecutor.execute(() -> {
            try {
                if (database != null) {
                    TrainingDataDao dao = database.trainingDataDao();
                    List<TrainingData> trainingData = dao.getAllTrainingData();
                    notifyDataLoaded("TrainingData", trainingData);
                    Log.d(TAG, "Loaded " + trainingData.size() + " training data records");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading training data", e);
                notifyDatabaseError("loadTrainingData", e.getMessage());
            }
        });
    }
    
    public void saveTrainingData(TrainingData trainingData) {
        databaseExecutor.execute(() -> {
            try {
                if (database != null) {
                    TrainingDataDao dao = database.trainingDataDao();
                    dao.insertTrainingData(trainingData);
                    notifyDataSaved("TrainingData", true);
                    Log.d(TAG, "Training data saved successfully");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving training data", e);
                notifyDataSaved("TrainingData", false);
                notifyDatabaseError("saveTrainingData", e.getMessage());
            }
        });
    }
    
    // Analytics Operations
    public void getGestureAnalytics() {
        databaseExecutor.execute(() -> {
            try {
                if (database != null) {
                    GestureDataDao dao = database.gestureDataDao();
                    // Get gesture statistics
                    int totalGestures = dao.getGestureCount();
                    List<String> mostUsedGestures = dao.getMostUsedGestures(5);
                    
                    Log.d(TAG, "Analytics: Total gestures=" + totalGestures + 
                          ", Most used=" + mostUsedGestures.size());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting gesture analytics", e);
                notifyDatabaseError("getAnalytics", e.getMessage());
            }
        });
    }
    
    public void clearAllData() {
        databaseExecutor.execute(() -> {
            try {
                if (database != null) {
                    database.clearAllTables();
                    Log.d(TAG, "All database data cleared");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error clearing database data", e);
                notifyDatabaseError("clearData", e.getMessage());
            }
        });
    }
    
    private void notifyDataLoaded(String dataType, List<?> data) {
        if (listener != null) {
            listener.onDataLoaded(dataType, data);
        }
    }
    
    private void notifyDataSaved(String dataType, boolean success) {
        if (listener != null) {
            listener.onDataSaved(dataType, success);
        }
    }
    
    private void notifyDataDeleted(String dataType, boolean success) {
        if (listener != null) {
            listener.onDataDeleted(dataType, success);
        }
    }
    
    private void notifyDatabaseError(String operation, String error) {
        if (listener != null) {
            listener.onDatabaseError(operation, error);
        }
    }
    
    public void cleanup() {
        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
        }
    }
}
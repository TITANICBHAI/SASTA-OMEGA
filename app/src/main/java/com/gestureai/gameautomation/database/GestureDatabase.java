package com.gestureai.gameautomation.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.content.Context;
import android.util.Log;

@Database(
    entities = {GestureData.class},
    version = 3,
    exportSchema = false
)
public abstract class GestureDatabase extends RoomDatabase {
    private static final String TAG = "GestureDatabase";
    
    public abstract GestureDao gestureDao();

    private static volatile GestureDatabase INSTANCE;

    // Migration from version 1 to 2 (removed SessionData conflicts)
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            try {
                // Add model configuration table for dynamic TensorFlow models
                database.execSQL("CREATE TABLE IF NOT EXISTS model_config (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "model_name TEXT NOT NULL, " +
                    "model_path TEXT NOT NULL, " +
                    "model_type TEXT NOT NULL, " +
                    "is_active INTEGER NOT NULL DEFAULT 0, " +
                    "created_at INTEGER NOT NULL, " +
                    "UNIQUE(model_name))");
                
                // Add service state table for communication protocol
                database.execSQL("CREATE TABLE IF NOT EXISTS service_state (" +
                    "service_id TEXT PRIMARY KEY NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "last_message TEXT, " +
                    "last_update INTEGER NOT NULL)");
                
                // Add performance metrics columns to session_data
                database.execSQL("ALTER TABLE session_data ADD COLUMN model_accuracy REAL DEFAULT 0.0");
                database.execSQL("ALTER TABLE session_data ADD COLUMN inference_time REAL DEFAULT 0.0");
                database.execSQL("ALTER TABLE session_data ADD COLUMN model_name TEXT DEFAULT 'default'");
                
                Log.d(TAG, "Database migrated from version 1 to 2 with TensorFlow model support");
            } catch (Exception e) {
                Log.e(TAG, "Migration error: " + e.getMessage());
                throw e;
            }
        }
    };
    
    // Migration from version 2 to 3 (remove SessionData dependency)
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            try {
                // Remove SessionData table references to prevent conflicts with AppDatabase
                database.execSQL("DROP TABLE IF EXISTS session_data");
                database.execSQL("DROP TABLE IF EXISTS sessions");
                
                // Ensure only gesture-specific tables remain
                database.execSQL("CREATE INDEX IF NOT EXISTS index_gesture_data_timestamp ON gesture_data (timestamp)");
                database.execSQL("CREATE INDEX IF NOT EXISTS index_gesture_data_gesture_type ON gesture_data (gesture_type)");
                
                Log.d(TAG, "Database migrated from version 2 to 3 - SessionData conflicts resolved");
            } catch (Exception e) {
                Log.e(TAG, "Migration 2->3 error: " + e.getMessage());
                throw e;
            }
        }
    };

    public static GestureDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (GestureDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                GestureDatabase.class, "gesture_database")
                                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                                .addCallback(new DatabaseCallback())
                                .fallbackToDestructiveMigration() // Only as last resort
                                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE) // Better performance
                                .build();
                        Log.d(TAG, "Database initialized successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Database initialization failed: " + e.getMessage());
                        // Return null to indicate failure - caller should handle this
                        return null;
                    }
                }
            }
        }
        return INSTANCE;
    }
    
    // Missing getInstance method referenced in AnalyticsActivity
    public static GestureDatabase getInstance(Context context) {
        return getDatabase(context);
    }
    
    /**
     * Database recovery mechanism
     */
    public static void recoverDatabase(Context context) {
        try {
            if (INSTANCE != null) {
                INSTANCE.close();
                INSTANCE = null;
            }
            
            // Clear corrupted database and recreate
            context.deleteDatabase("gesture_database");
            Log.d(TAG, "Database recovered - recreating from scratch");
            
            getDatabase(context);
        } catch (Exception e) {
            Log.e(TAG, "Database recovery failed", e);
        }
    }
    
    /**
     * Database callback for initialization and error handling
     */
    private static class DatabaseCallback extends RoomDatabase.Callback {
        @Override
        public void onCreate(SupportSQLiteDatabase db) {
            super.onCreate(db);
            Log.d(TAG, "Database created successfully");
            
            // Initialize default model configurations
            try {
                db.execSQL("INSERT OR IGNORE INTO model_config (model_name, model_path, model_type, is_active, created_at) VALUES " +
                    "('dqn_model', 'models/dqn_model.tflite', 'DQN', 1, " + System.currentTimeMillis() + "), " +
                    "('object_detection', 'models/object_detection.tflite', 'DETECTION', 1, " + System.currentTimeMillis() + "), " +
                    "('gesture_classifier', 'models/gesture_classifier.tflite', 'CLASSIFICATION', 1, " + System.currentTimeMillis() + ")");
                
                Log.d(TAG, "Default model configurations inserted");
            } catch (Exception e) {
                Log.w(TAG, "Could not insert default configurations: " + e.getMessage());
            }
        }
        
        @Override
        public void onOpen(SupportSQLiteDatabase db) {
            super.onOpen(db);
            Log.d(TAG, "Database opened successfully");
        }
    }
}
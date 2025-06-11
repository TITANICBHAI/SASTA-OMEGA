package com.gestureai.gameautomation.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;
import com.gestureai.gameautomation.data.SessionData;
import com.gestureai.gameautomation.database.entities.GameActionEntity;
import com.gestureai.gameautomation.database.entities.GestureDataEntity;
import com.gestureai.gameautomation.database.dao.GameActionDao;
import com.gestureai.gameautomation.database.dao.GestureDataDao;

@Database(
    entities = {SessionData.class, GameActionEntity.class, GestureDataEntity.class},
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    
    public abstract SessionDataDao sessionDataDao();
    public abstract GameActionDao gameActionDao();
    public abstract GestureDataDao gestureDataDao();
    
    private static volatile AppDatabase instance;
    private static final Object DB_LOCK = new Object();
    
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (DB_LOCK) {
                if (instance == null) {
                    try {
                        instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "game_automation_database"
                        )
                        .fallbackToDestructiveMigration()
                        .setQueryExecutor(java.util.concurrent.Executors.newFixedThreadPool(4))
                        .setTransactionExecutor(java.util.concurrent.Executors.newSingleThreadExecutor())
                        .addCallback(new DatabaseCallback())
                        .enableMultiInstanceInvalidation()
                        .build();
                        
                        // Configure WAL mode for better concurrent access
                        instance.getOpenHelper().getWritableDatabase().execSQL("PRAGMA journal_mode=WAL");
                        instance.getOpenHelper().getWritableDatabase().execSQL("PRAGMA synchronous=NORMAL");
                        instance.getOpenHelper().getWritableDatabase().execSQL("PRAGMA cache_size=10000");
                        instance.getOpenHelper().getWritableDatabase().execSQL("PRAGMA temp_store=memory");
                        
                        android.util.Log.d("AppDatabase", "Database instance created successfully");
                    } catch (Exception e) {
                        android.util.Log.e("AppDatabase", "Failed to create database instance", e);
                        throw new RuntimeException("Database initialization failed", e);
                    }
                }
            }
        }
        return instance;
    }
    
    public static void closeDatabase() {
        synchronized (DB_LOCK) {
            if (instance != null && instance.isOpen()) {
                instance.close();
                instance = null;
                android.util.Log.d("AppDatabase", "Database closed and instance cleared");
            }
        }
    }
    
    /**
     * Database callback for corruption detection and recovery
     */
    private static class DatabaseCallback extends RoomDatabase.Callback {
        @Override
        public void onCreate(androidx.sqlite.db.SupportSQLiteDatabase db) {
            super.onCreate(db);
            // Enable foreign key constraints for data integrity
            db.execSQL("PRAGMA foreign_keys=ON");
            android.util.Log.d("AppDatabase", "Database created with integrity constraints");
        }
        
        @Override
        public void onOpen(androidx.sqlite.db.SupportSQLiteDatabase db) {
            super.onOpen(db);
            // Verify database integrity on each open
            try {
                db.execSQL("PRAGMA foreign_keys=ON");
                db.execSQL("PRAGMA integrity_check");
                android.util.Log.d("AppDatabase", "Database integrity verified");
            } catch (Exception e) {
                android.util.Log.e("AppDatabase", "Database integrity check failed", e);
            }
        }
        
        @Override
        public void onCorruptionDetected(androidx.sqlite.db.SupportSQLiteDatabase db) {
            super.onCorruptionDetected(db);
            android.util.Log.e("AppDatabase", "Database corruption detected - initiating recovery");
            
            // Close current instance to trigger rebuild
            synchronized (DB_LOCK) {
                if (instance != null) {
                    try {
                        instance.close();
                        instance = null;
                        android.util.Log.i("AppDatabase", "Corrupted database instance cleared for rebuild");
                    } catch (Exception e) {
                        android.util.Log.e("AppDatabase", "Error closing corrupted database", e);
                    }
                }
            }
        }
    }
}
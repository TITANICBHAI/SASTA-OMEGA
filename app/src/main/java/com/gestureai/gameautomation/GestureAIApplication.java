package com.gestureai.gameautomation;

import android.app.Application;
import android.util.Log;

import com.gestureai.gameautomation.database.AppDatabase;
import com.gestureai.gameautomation.managers.AIStackManager;
import com.gestureai.gameautomation.config.AIConfiguration;

/**
 * Application class to initialize critical components and prevent database crashes
 */
public class GestureAIApplication extends Application {
    private static final String TAG = "GestureAIApplication";
    
    private static GestureAIApplication instance;
    private AppDatabase database;
    private AIStackManager aiStackManager;
    private AIConfiguration aiConfiguration;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        initializeDatabase();
        initializeAIComponents();
        initializeGameAutomationEngine();
        
        Log.d(TAG, "GestureAI Application initialized successfully");
    }
    
    /**
     * Initialize Room database - CRITICAL to prevent analytics crashes
     */
    private void initializeDatabase() {
        try {
            database = AppDatabase.getInstance(this);
            Log.d(TAG, "Database initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize database", e);
        }
    }
    
    /**
     * Initialize AI components for global access
     */
    private void initializeAIComponents() {
        try {
            aiStackManager = AIStackManager.getInstance(this);
            aiConfiguration = AIConfiguration.getInstance(this);
            
            Log.d(TAG, "AI components initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize AI components", e);
        }
    }
    
    /**
     * Initialize GameAutomationEngine singleton
     */
    private void initializeGameAutomationEngine() {
        try {
            GameAutomationEngine.initialize(this);
            Log.d(TAG, "GameAutomationEngine initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize GameAutomationEngine", e);
        }
    }
    
    public static GestureAIApplication getInstance() {
        return instance;
    }
    
    public AppDatabase getDatabase() {
        return database;
    }
    
    public AIStackManager getAIStackManager() {
        return aiStackManager;
    }
    
    public AIConfiguration getAIConfiguration() {
        return aiConfiguration;
    }
}
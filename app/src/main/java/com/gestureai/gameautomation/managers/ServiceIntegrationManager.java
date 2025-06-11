package com.gestureai.gameautomation.managers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.gestureai.gameautomation.services.*;
import com.gestureai.gameautomation.activities.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Service Integration Manager
 * Connects all disconnected services and activities to the main application flow
 */
public class ServiceIntegrationManager {
    private static final String TAG = "ServiceIntegrationMgr";
    private static ServiceIntegrationManager instance;
    
    private Context context;
    private Map<String, Boolean> serviceStates;
    private Map<String, Class<?>> activityRegistry;
    
    private ServiceIntegrationManager(Context context) {
        this.context = context.getApplicationContext();
        this.serviceStates = new HashMap<>();
        this.activityRegistry = new HashMap<>();
        
        initializeServiceStates();
        registerActivities();
        
        Log.d(TAG, "Service Integration Manager initialized");
    }
    
    public static synchronized ServiceIntegrationManager getInstance(Context context) {
        if (instance == null) {
            instance = new ServiceIntegrationManager(context);
        }
        return instance;
    }
    
    private void initializeServiceStates() {
        serviceStates.put("TouchAutomationService", false);
        serviceStates.put("GestureRecognitionService", false);
        serviceStates.put("ScreenCaptureService", false);
        serviceStates.put("OverlayService", false);
        serviceStates.put("VoiceCommandService", false);
        serviceStates.put("DebugOverlayService", false);
    }
    
    private void registerActivities() {
        // Register all previously disconnected activities
        activityRegistry.put("object_labeling", ObjectLabelingActivity.class);
        activityRegistry.put("object_labeling_training", ObjectLabelingTrainingActivity.class);
        activityRegistry.put("comprehensive_analytics", ComprehensiveAnalyticsActivity.class);
        activityRegistry.put("real_time_analytics", RealTimeAnalyticsDashboardActivity.class);
        activityRegistry.put("game_strategy_config", GameSpecificStrategyConfigActivity.class);
        activityRegistry.put("advanced_gesture_training", AdvancedGestureTrainingActivity.class);
        activityRegistry.put("voice_command_config", VoiceCommandConfigurationActivity.class);
        activityRegistry.put("advanced_debug_tools", AdvancedDebugToolsActivity.class);
        activityRegistry.put("ai_training_dashboard", AITrainingDashboardActivity.class);
        activityRegistry.put("performance_monitoring", PerformanceMonitoringDashboardActivity.class);
        activityRegistry.put("session_analytics", SessionAnalyticsDashboardActivity.class);
        activityRegistry.put("gesture_sequence_builder", GestureSequenceBuilderActivity.class);
        activityRegistry.put("strategy_configuration_wizard", StrategyConfigurationWizardActivity.class);
        activityRegistry.put("training_progress_visualizer", TrainingProgressVisualizerActivity.class);
        activityRegistry.put("settings", SettingsActivity.class);
        activityRegistry.put("analytics", AnalyticsActivity.class);
        
        Log.d(TAG, "Registered " + activityRegistry.size() + " activities");
    }
    
    /**
     * Start a service with proper integration
     */
    public boolean startService(String serviceName) {
        try {
            Intent serviceIntent = null;
            
            switch (serviceName) {
                case "TouchAutomationService":
                    serviceIntent = new Intent(context, TouchAutomationService.class);
                    break;
                case "GestureRecognitionService":
                    serviceIntent = new Intent(context, GestureRecognitionService.class);
                    break;
                case "ScreenCaptureService":
                    serviceIntent = new Intent(context, ScreenCaptureService.class);
                    break;
                case "OverlayService":
                    serviceIntent = new Intent(context, OverlayService.class);
                    break;
                case "VoiceCommandService":
                    serviceIntent = new Intent(context, VoiceCommandService.class);
                    break;
                case "DebugOverlayService":
                    serviceIntent = new Intent(context, DebugOverlayService.class);
                    break;
            }
            
            if (serviceIntent != null) {
                context.startService(serviceIntent);
                serviceStates.put(serviceName, true);
                Log.d(TAG, "Started service: " + serviceName);
                return true;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting service: " + serviceName, e);
        }
        
        return false;
    }
    
    /**
     * Stop a service
     */
    public boolean stopService(String serviceName) {
        try {
            Intent serviceIntent = null;
            
            switch (serviceName) {
                case "TouchAutomationService":
                    serviceIntent = new Intent(context, TouchAutomationService.class);
                    break;
                case "GestureRecognitionService":
                    serviceIntent = new Intent(context, GestureRecognitionService.class);
                    break;
                case "ScreenCaptureService":
                    serviceIntent = new Intent(context, ScreenCaptureService.class);
                    break;
                case "OverlayService":
                    serviceIntent = new Intent(context, OverlayService.class);
                    break;
                case "VoiceCommandService":
                    serviceIntent = new Intent(context, VoiceCommandService.class);
                    break;
                case "DebugOverlayService":
                    serviceIntent = new Intent(context, DebugOverlayService.class);
                    break;
            }
            
            if (serviceIntent != null) {
                context.stopService(serviceIntent);
                serviceStates.put(serviceName, false);
                Log.d(TAG, "Stopped service: " + serviceName);
                return true;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping service: " + serviceName, e);
        }
        
        return false;
    }
    
    /**
     * Launch an activity with proper integration
     */
    public boolean launchActivity(Context activityContext, String activityKey) {
        try {
            Class<?> activityClass = activityRegistry.get(activityKey);
            
            if (activityClass != null) {
                Intent intent = new Intent(activityContext, activityClass);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activityContext.startActivity(intent);
                
                Log.d(TAG, "Launched activity: " + activityKey);
                return true;
            } else {
                Log.w(TAG, "Activity not found: " + activityKey);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching activity: " + activityKey, e);
        }
        
        return false;
    }
    
    /**
     * Start automation with all required services
     */
    public void startFullAutomation() {
        try {
            // Start core automation services
            startService("TouchAutomationService");
            startService("GestureRecognitionService");
            startService("ScreenCaptureService");
            startService("OverlayService");
            
            Log.d(TAG, "Full automation started");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting full automation", e);
        }
    }
    
    /**
     * Stop all automation services
     */
    public void stopAllAutomation() {
        try {
            for (String serviceName : serviceStates.keySet()) {
                if (serviceStates.get(serviceName)) {
                    stopService(serviceName);
                }
            }
            
            Log.d(TAG, "All automation stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping automation", e);
        }
    }
    
    /**
     * Get service status
     */
    public boolean isServiceRunning(String serviceName) {
        return serviceStates.getOrDefault(serviceName, false);
    }
    
    /**
     * Get all registered activities
     */
    public Map<String, Class<?>> getRegisteredActivities() {
        return new HashMap<>(activityRegistry);
    }
    
    /**
     * Check if all core services are ready
     */
    public boolean areAllServicesReady() {
        return serviceStates.getOrDefault("TouchAutomationService", false) &&
               serviceStates.getOrDefault("GestureRecognitionService", false) &&
               serviceStates.getOrDefault("ScreenCaptureService", false);
    }
    
    /**
     * Get service integration status report
     */
    public String getStatusReport() {
        StringBuilder report = new StringBuilder();
        report.append("Service Integration Status:\n");
        
        for (Map.Entry<String, Boolean> entry : serviceStates.entrySet()) {
            report.append("- ").append(entry.getKey()).append(": ")
                  .append(entry.getValue() ? "RUNNING" : "STOPPED").append("\n");
        }
        
        report.append("Registered Activities: ").append(activityRegistry.size()).append("\n");
        report.append("Core Services Ready: ").append(areAllServicesReady());
        
        return report.toString();
    }
}
package com.gestureai.gameautomation.managers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.gestureai.gameautomation.services.GestureRecognitionService;
import com.gestureai.gameautomation.services.ScreenCaptureService;
import com.gestureai.gameautomation.services.TouchAutomationService;
import com.gestureai.gameautomation.services.VoiceCommandService;
import java.util.HashMap;
import java.util.Map;

public class ServiceConnectionManager {
    private static final String TAG = "ServiceConnectionManager";
    private static ServiceConnectionManager instance;
    
    private Context context;
    private Map<String, Boolean> serviceStates;
    private ServiceConnectionListener listener;
    
    public interface ServiceConnectionListener {
        void onServiceConnected(String serviceName);
        void onServiceDisconnected(String serviceName);
        void onServiceError(String serviceName, String error);
    }
    
    private ServiceConnectionManager(Context context) {
        this.context = context;
        this.serviceStates = new HashMap<>();
        initializeServiceStates();
    }
    
    public static synchronized ServiceConnectionManager getInstance(Context context) {
        if (instance == null) {
            instance = new ServiceConnectionManager(context.getApplicationContext());
        }
        return instance;
    }
    
    private void initializeServiceStates() {
        serviceStates.put("GestureRecognitionService", false);
        serviceStates.put("ScreenCaptureService", false);
        serviceStates.put("TouchAutomationService", false);
        serviceStates.put("VoiceCommandService", false);
    }
    
    public void setServiceConnectionListener(ServiceConnectionListener listener) {
        this.listener = listener;
    }
    
    public boolean startGestureRecognitionService() {
        try {
            Intent intent = new Intent(context, GestureRecognitionService.class);
            context.startForegroundService(intent);
            updateServiceState("GestureRecognitionService", true);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start GestureRecognitionService", e);
            notifyServiceError("GestureRecognitionService", e.getMessage());
            return false;
        }
    }
    
    public boolean startScreenCaptureService() {
        try {
            Intent intent = new Intent(context, ScreenCaptureService.class);
            context.startForegroundService(intent);
            updateServiceState("ScreenCaptureService", true);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start ScreenCaptureService", e);
            notifyServiceError("ScreenCaptureService", e.getMessage());
            return false;
        }
    }
    
    public boolean startVoiceCommandService() {
        try {
            Intent intent = new Intent(context, VoiceCommandService.class);
            context.startForegroundService(intent);
            updateServiceState("VoiceCommandService", true);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start VoiceCommandService", e);
            notifyServiceError("VoiceCommandService", e.getMessage());
            return false;
        }
    }
    
    public void stopAllServices() {
        try {
            context.stopService(new Intent(context, GestureRecognitionService.class));
            context.stopService(new Intent(context, ScreenCaptureService.class));
            context.stopService(new Intent(context, VoiceCommandService.class));
            
            // Reset all service states
            for (String serviceName : serviceStates.keySet()) {
                updateServiceState(serviceName, false);
            }
            
            Log.d(TAG, "All services stopped successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping services", e);
        }
    }
    
    private void updateServiceState(String serviceName, boolean isConnected) {
        serviceStates.put(serviceName, isConnected);
        
        if (listener != null) {
            if (isConnected) {
                listener.onServiceConnected(serviceName);
            } else {
                listener.onServiceDisconnected(serviceName);
            }
        }
        
        Log.d(TAG, serviceName + " state: " + (isConnected ? "Connected" : "Disconnected"));
    }
    
    private void notifyServiceError(String serviceName, String error) {
        if (listener != null) {
            listener.onServiceError(serviceName, error);
        }
    }
    
    public boolean isServiceConnected(String serviceName) {
        return serviceStates.getOrDefault(serviceName, false);
    }
    
    public Map<String, Boolean> getAllServiceStates() {
        return new HashMap<>(serviceStates);
    }
    
    public boolean areAllCriticalServicesConnected() {
        return isServiceConnected("GestureRecognitionService") && 
               isServiceConnected("TouchAutomationService");
    }
}
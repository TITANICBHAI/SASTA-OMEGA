package com.gestureai.gameautomation.services;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service Communication Protocol
 * Manages inter-service communication, state sharing, and coordination
 */
public class ServiceCommunicationProtocol {
    private static final String TAG = "ServiceCommProtocol";
    private static ServiceCommunicationProtocol instance;
    
    private Context context;
    private Map<String, ServiceState> serviceStates;
    private Map<String, List<ServiceListener>> serviceListeners;
    private Map<String, ServiceConnection> serviceConnections;
    private Map<String, Object> serviceInstances;
    private AtomicBoolean isInitialized;
    private Handler mainHandler;
    private AtomicBoolean protocolActive;
    
    // Data pipeline integration
    private com.gestureai.gameautomation.pipeline.UnifiedDataPipeline dataPipeline;
    
    // Service identifiers
    public static final String TOUCH_AUTOMATION_SERVICE = "TouchAutomationService";
    public static final String GESTURE_RECOGNITION_SERVICE = "GestureRecognitionService";
    public static final String SCREEN_CAPTURE_SERVICE = "ScreenCaptureService";
    public static final String DEBUG_OVERLAY_SERVICE = "DebugOverlayService";
    public static final String VOICE_COMMAND_SERVICE = "VoiceCommandService";
    
    private ServiceCommunicationProtocol(Context context) {
        this.context = context.getApplicationContext();
        this.serviceStates = new ConcurrentHashMap<>();
        this.serviceListeners = new ConcurrentHashMap<>();
        this.serviceConnections = new HashMap<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.protocolActive = new AtomicBoolean(false);
        
        initializeServiceStates();
    }
    
    public static synchronized ServiceCommunicationProtocol getInstance(Context context) {
        if (instance == null) {
            instance = new ServiceCommunicationProtocol(context);
        }
        return instance;
    }
    
    private void initializeServiceStates() {
        serviceStates.put(TOUCH_AUTOMATION_SERVICE, new ServiceState(TOUCH_AUTOMATION_SERVICE));
        serviceStates.put(GESTURE_RECOGNITION_SERVICE, new ServiceState(GESTURE_RECOGNITION_SERVICE));
        serviceStates.put(SCREEN_CAPTURE_SERVICE, new ServiceState(SCREEN_CAPTURE_SERVICE));
        serviceStates.put(DEBUG_OVERLAY_SERVICE, new ServiceState(DEBUG_OVERLAY_SERVICE));
        serviceStates.put(VOICE_COMMAND_SERVICE, new ServiceState(VOICE_COMMAND_SERVICE));
        
        for (String serviceId : serviceStates.keySet()) {
            serviceListeners.put(serviceId, new ArrayList<>());
        }
        
        Log.d(TAG, "Service communication protocol initialized");
    }
    
    /**
     * Start the communication protocol
     */
    public void startProtocol() {
        if (protocolActive.get()) {
            Log.w(TAG, "Protocol already active");
            return;
        }
        
        protocolActive.set(true);
        bindToServices();
        startHealthMonitoring();
        
        Log.d(TAG, "Service communication protocol started");
    }
    
    /**
     * Stop the communication protocol
     */
    public void stopProtocol() {
        if (!protocolActive.get()) {
            return;
        }
        
        protocolActive.set(false);
        unbindFromServices();
        
        Log.d(TAG, "Service communication protocol stopped");
    }
    
    private void bindToServices() {
        for (String serviceId : serviceStates.keySet()) {
            bindToService(serviceId);
        }
    }
    
    private void bindToService(String serviceId) {
        try {
            Intent intent = getServiceIntent(serviceId);
            if (intent != null) {
                ServiceConnection connection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        updateServiceState(serviceId, ServiceStatus.CONNECTED, "Service connected");
                        Log.d(TAG, "Connected to service: " + serviceId);
                    }
                    
                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        updateServiceState(serviceId, ServiceStatus.DISCONNECTED, "Service disconnected");
                        Log.w(TAG, "Disconnected from service: " + serviceId);
                        
                        // Attempt reconnection
                        scheduleReconnection(serviceId);
                    }
                };
                
                serviceConnections.put(serviceId, connection);
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error binding to service: " + serviceId, e);
            updateServiceState(serviceId, ServiceStatus.ERROR, "Binding failed: " + e.getMessage());
        }
    }
    
    private Intent getServiceIntent(String serviceId) {
        switch (serviceId) {
            case TOUCH_AUTOMATION_SERVICE:
                return new Intent(context, TouchAutomationService.class);
            case GESTURE_RECOGNITION_SERVICE:
                return new Intent(context, GestureRecognitionService.class);
            case SCREEN_CAPTURE_SERVICE:
                return new Intent(context, ScreenCaptureService.class);
            case DEBUG_OVERLAY_SERVICE:
                return new Intent(context, DebugOverlayService.class);
            case VOICE_COMMAND_SERVICE:
                return new Intent(context, VoiceCommandService.class);
            default:
                return null;
        }
    }
    
    private void scheduleReconnection(String serviceId) {
        mainHandler.postDelayed(() -> {
            if (protocolActive.get()) {
                Log.d(TAG, "Attempting to reconnect to service: " + serviceId);
                bindToService(serviceId);
            }
        }, 5000); // Retry after 5 seconds
    }
    
    private void unbindFromServices() {
        for (Map.Entry<String, ServiceConnection> entry : serviceConnections.entrySet()) {
            try {
                context.unbindService(entry.getValue());
                updateServiceState(entry.getKey(), ServiceStatus.STOPPED, "Service unbound");
            } catch (Exception e) {
                Log.w(TAG, "Error unbinding service: " + entry.getKey(), e);
            }
        }
        serviceConnections.clear();
    }
    
    /**
     * Update service state and notify listeners
     */
    public void updateServiceState(String serviceId, ServiceStatus status, String message) {
        ServiceState state = serviceStates.get(serviceId);
        if (state != null) {
            state.setStatus(status);
            state.setLastMessage(message);
            state.setLastUpdate(System.currentTimeMillis());
            
            notifyServiceListeners(serviceId, state);
        }
    }
    
    /**
     * Broadcast message to all services
     */
    public void broadcastMessage(String messageType, String data) {
        for (String serviceId : serviceStates.keySet()) {
            sendMessageToService(serviceId, messageType, data);
        }
    }
    
    /**
     * Send message to specific service
     */
    public void sendMessageToService(String serviceId, String messageType, String data) {
        try {
            Intent intent = new Intent(messageType);
            intent.putExtra("data", data);
            intent.putExtra("sender", "ServiceCommunicationProtocol");
            intent.putExtra("timestamp", System.currentTimeMillis());
            
            context.sendBroadcast(intent);
            
            Log.d(TAG, "Message sent to " + serviceId + ": " + messageType);
        } catch (Exception e) {
            Log.e(TAG, "Error sending message to service: " + serviceId, e);
        }
    }
    
    /**
     * Register listener for service state changes
     */
    public void registerServiceListener(String serviceId, ServiceListener listener) {
        List<ServiceListener> listeners = serviceListeners.get(serviceId);
        if (listeners != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Unregister service listener
     */
    public void unregisterServiceListener(String serviceId, ServiceListener listener) {
        List<ServiceListener> listeners = serviceListeners.get(serviceId);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }
    
    private void notifyServiceListeners(String serviceId, ServiceState state) {
        List<ServiceListener> listeners = serviceListeners.get(serviceId);
        if (listeners != null) {
            for (ServiceListener listener : listeners) {
                try {
                    listener.onServiceStateChanged(serviceId, state);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying listener", e);
                }
            }
        }
    }
    
    private void startHealthMonitoring() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (protocolActive.get()) {
                    performHealthCheck();
                    mainHandler.postDelayed(this, 10000); // Check every 10 seconds
                }
            }
        });
    }
    
    private void performHealthCheck() {
        for (Map.Entry<String, ServiceState> entry : serviceStates.entrySet()) {
            String serviceId = entry.getKey();
            ServiceState state = entry.getValue();
            
            long timeSinceLastUpdate = System.currentTimeMillis() - state.getLastUpdate();
            
            if (timeSinceLastUpdate > 30000 && state.getStatus() == ServiceStatus.CONNECTED) {
                // Service hasn't updated in 30 seconds, mark as unresponsive
                updateServiceState(serviceId, ServiceStatus.UNRESPONSIVE, "No updates for " + (timeSinceLastUpdate / 1000) + " seconds");
            }
        }
    }
    
    /**
     * Get current service state
     */
    public ServiceState getServiceState(String serviceId) {
        return serviceStates.get(serviceId);
    }
    
    /**
     * Get all service states
     */
    public Map<String, ServiceState> getAllServiceStates() {
        return new HashMap<>(serviceStates);
    }
    
    /**
     * Check if all services are healthy
     */
    public boolean areAllServicesHealthy() {
        for (ServiceState state : serviceStates.values()) {
            if (state.getStatus() != ServiceStatus.CONNECTED) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Service state class
     */
    public static class ServiceState {
        private String serviceId;
        private ServiceStatus status;
        private String lastMessage;
        private long lastUpdate;
        
        public ServiceState(String serviceId) {
            this.serviceId = serviceId;
            this.status = ServiceStatus.DISCONNECTED;
            this.lastMessage = "Initialized";
            this.lastUpdate = System.currentTimeMillis();
        }
        
        // Getters and setters
        public String getServiceId() { return serviceId; }
        public ServiceStatus getStatus() { return status; }
        public String getLastMessage() { return lastMessage; }
        public long getLastUpdate() { return lastUpdate; }
        
        public void setStatus(ServiceStatus status) { this.status = status; }
        public void setLastMessage(String message) { this.lastMessage = message; }
        public void setLastUpdate(long timestamp) { this.lastUpdate = timestamp; }
    }
    
    /**
     * Service status enumeration
     */
    public enum ServiceStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        UNRESPONSIVE,
        ERROR,
        STOPPED
    }
    
    /**
     * Service listener interface
     */
    public interface ServiceListener {
        void onServiceStateChanged(String serviceId, ServiceState state);
    }
}
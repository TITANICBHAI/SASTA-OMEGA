package com.gestureai.gameautomation.managers;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
import android.util.Log;
import com.gestureai.gameautomation.utils.ThreadSafeManager;
import com.gestureai.gameautomation.services.TouchAutomationService;
import com.gestureai.gameautomation.services.GestureRecognitionService;
import com.gestureai.gameautomation.services.ScreenCaptureService;
import com.gestureai.gameautomation.services.VoiceCommandService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServiceConnectionManager extends ThreadSafeManager {
    private static final String TAG = "ServiceConnectionManager";
    
    private Context context;
    private final Map<String, ServiceConnection> connections = new HashMap<>();
    private final Map<String, AtomicBoolean> connectionStates = new HashMap<>();
    
    public static ServiceConnectionManager getInstance(Context context) {
        return getInstance(ServiceConnectionManager.class, () -> new ServiceConnectionManager(context));
    }
    
    private ServiceConnectionManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    @Override
    protected boolean initializeInternal() {
        try {
            Log.d(TAG, "Initializing service connections");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize service connections", e);
            return false;
        }
    }
    
    @Override
    protected void cleanupInternal() {
        try {
            disconnectAllServices();
            connections.clear();
            connectionStates.clear();
            Log.d(TAG, "Service connections cleaned up");
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
    
    /**
     * Connect to TouchAutomationService - Accessibility services use static instance access
     */
    public boolean connectTouchService() {
        try {
            // Accessibility services cannot be bound like regular services
            // Check if the service is available through static instance
            TouchAutomationService instance = TouchAutomationService.getInstanceSafe();
            boolean isAvailable = instance != null && instance.isServiceReady();
            connectionStates.put("TouchAutomationService", new AtomicBoolean(isAvailable));
            Log.d(TAG, "TouchAutomationService availability checked: " + isAvailable);
            return isAvailable;
        } catch (Exception e) {
            Log.e(TAG, "Error checking TouchAutomationService availability", e);
            connectionStates.put("TouchAutomationService", new AtomicBoolean(false));
            return false;
        }
    }
    
    /**
     * Connect to GestureRecognitionService
     */
    public boolean connectGestureService() {
        return connectService("GestureRecognitionService", GestureRecognitionService.class,
            new GestureServiceConnection());
    }
    
    /**
     * Generic service connection method
     */
    private boolean connectService(String serviceName, Class<?> serviceClass, ServiceConnection connection) {
        try {
            if (isServiceConnected(serviceName)) {
                Log.d(TAG, serviceName + " already connected");
                return true;
            }
            
            Intent intent = new Intent(context, serviceClass);
            boolean bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
            
            if (bound) {
                connections.put(serviceName, connection);
                connectionStates.put(serviceName, new AtomicBoolean(false));
                Log.d(TAG, "Binding " + serviceName + ": " + bound);
            }
            
            return bound;
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect " + serviceName, e);
            return false;
        }
    }
    
    /**
     * Check if service is connected
     */
    public boolean isServiceConnected(String serviceName) {
        AtomicBoolean state = connectionStates.get(serviceName);
        return state != null && state.get();
    }
    
    /**
     * Disconnect specific service
     */
    public void disconnectService(String serviceName) {
        try {
            ServiceConnection connection = connections.get(serviceName);
            if (connection != null) {
                context.unbindService(connection);
                connections.remove(serviceName);
                connectionStates.remove(serviceName);
                Log.d(TAG, "Disconnected " + serviceName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error disconnecting " + serviceName, e);
        }
    }
    
    /**
     * Disconnect all services
     */
    public void disconnectAllServices() {
        for (String serviceName : connections.keySet()) {
            disconnectService(serviceName);
        }
    }
    
    /**
     * Touch service connection
     */
    private class TouchServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            connectionStates.get("TouchAutomationService").set(true);
            Log.d(TAG, "TouchAutomationService connected");
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            connectionStates.get("TouchAutomationService").set(false);
            Log.d(TAG, "TouchAutomationService disconnected");
        }
    }
    
    /**
     * Gesture service connection
     */
    private class GestureServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            connectionStates.get("GestureRecognitionService").set(true);
            Log.d(TAG, "GestureRecognitionService connected");
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            connectionStates.get("GestureRecognitionService").set(false);
            Log.d(TAG, "GestureRecognitionService disconnected");
        }
    }
}
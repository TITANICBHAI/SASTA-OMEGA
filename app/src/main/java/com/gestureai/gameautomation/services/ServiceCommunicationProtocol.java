package com.gestureai.gameautomation.services;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import com.gestureai.gameautomation.messaging.EventBus;

/**
 * Centralized service communication protocol for coordinating
 * between TouchAutomationService, ScreenCaptureService, and other components
 */
public class ServiceCommunicationProtocol {
    
    private static final String TAG = "ServiceCommunicationProtocol";
    private static volatile ServiceCommunicationProtocol instance;
    
    private Context context;
    private EventBus eventBus;
    private Handler mainHandler;
    
    // Service status tracking
    private Map<String, ServiceStatus> serviceStatuses;
    private List<ServiceStatusListener> statusListeners;
    
    // Service connections
    private Map<String, ServiceConnection> serviceConnections;
    
    // Message ordering and duplicate detection with corruption prevention
    private final java.util.concurrent.ConcurrentLinkedQueue<ServiceMessage> messageQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final java.util.concurrent.atomic.AtomicLong messageSequence = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.ConcurrentHashMap<String, Long> lastMessageIds = new java.util.concurrent.ConcurrentHashMap<>();
    private final Object messageLock = new Object();
    private volatile boolean messageProcessingActive = false;
    
    // Service discovery with ClassNotFoundException protection
    private final java.util.concurrent.ConcurrentHashMap<String, Class<?>> validServiceClasses = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger serviceDiscoveryFailures = new java.util.concurrent.atomic.AtomicInteger(0);
    private volatile boolean emergencyMode = false;
    
    private ServiceCommunicationProtocol(Context context) {
        this.context = context.getApplicationContext();
        this.eventBus = EventBus.getInstance();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.serviceStatuses = new ConcurrentHashMap<>();
        this.statusListeners = new ArrayList<>();
        this.serviceConnections = new ConcurrentHashMap<>();
        
        initializeServiceTracking();
        Log.d(TAG, "ServiceCommunicationProtocol initialized");
    }
    
    public static ServiceCommunicationProtocol getInstance(Context context) {
        if (instance == null) {
            synchronized (ServiceCommunicationProtocol.class) {
                if (instance == null) {
                    instance = new ServiceCommunicationProtocol(context);
                }
            }
        }
        return instance;
    }
    
    private void initializeServiceTracking() {
        // Initialize status for all critical services
        serviceStatuses.put("TouchAutomationService", new ServiceStatus("TouchAutomationService", false));
        serviceStatuses.put("ScreenCaptureService", new ServiceStatus("ScreenCaptureService", false));
        serviceStatuses.put("GameAutomationAccessibilityService", new ServiceStatus("GameAutomationAccessibilityService", false));
        serviceStatuses.put("VoiceCommandService", new ServiceStatus("VoiceCommandService", false));
        serviceStatuses.put("ServiceIntegrationManager", new ServiceStatus("ServiceIntegrationManager", false));
        
        // Initialize valid service classes to prevent ClassNotFoundException
        initializeValidServiceClasses();
        
        // Subscribe to service events
        eventBus.subscribe(EventBus.ServiceStatusChangedEvent.class, this::handleServiceStatusChanged);
    }
    
    /**
     * Critical: Initialize valid service classes to prevent ClassNotFoundException
     */
    private void initializeValidServiceClasses() {
        try {
            validServiceClasses.put("TouchAutomationService", TouchAutomationService.class);
            validServiceClasses.put("ScreenCaptureService", ScreenCaptureService.class);
            validServiceClasses.put("GameAutomationAccessibilityService", GameAutomationAccessibilityService.class);
            validServiceClasses.put("VoiceCommandService", VoiceCommandService.class);
            Log.d(TAG, "Valid service classes initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing service classes", e);
            emergencyMode = true;
            serviceDiscoveryFailures.incrementAndGet();
        }
    }
    
    /**
     * Safe service discovery with ClassNotFoundException protection
     */
    public boolean isServiceClassValid(String serviceName) {
        if (emergencyMode) {
            Log.w(TAG, "Emergency mode active - service validation bypassed");
            return false;
        }
        
        Class<?> serviceClass = validServiceClasses.get(serviceName);
        if (serviceClass == null) {
            serviceDiscoveryFailures.incrementAndGet();
            Log.w(TAG, "Service class not found for: " + serviceName);
            
            // Enter emergency mode if too many failures
            if (serviceDiscoveryFailures.get() >= 5) {
                emergencyMode = true;
                Log.e(TAG, "Emergency mode activated due to repeated service discovery failures");
            }
            return false;
        }
        return true;
    }
    
    /**
     * Start a service and track its status
     */
    public boolean startService(String serviceName, Class<?> serviceClass) {
        try {
            Intent serviceIntent = new Intent(context, serviceClass);
            
            // Start the service
            ComponentName component = context.startService(serviceIntent);
            
            if (component != null) {
                updateServiceStatus(serviceName, true, "Service started successfully");
                Log.d(TAG, "Started service: " + serviceName);
                return true;
            } else {
                updateServiceStatus(serviceName, false, "Failed to start service");
                Log.e(TAG, "Failed to start service: " + serviceName);
                return false;
            }
        } catch (Exception e) {
            updateServiceStatus(serviceName, false, "Exception starting service: " + e.getMessage());
            Log.e(TAG, "Exception starting service " + serviceName, e);
            return false;
        }
    }
    
    /**
     * Stop a service and update status
     */
    public boolean stopService(String serviceName, Class<?> serviceClass) {
        try {
            Intent serviceIntent = new Intent(context, serviceClass);
            boolean stopped = context.stopService(serviceIntent);
            
            updateServiceStatus(serviceName, false, stopped ? "Service stopped" : "Service stop failed");
            Log.d(TAG, "Stopped service: " + serviceName + " (success: " + stopped + ")");
            return stopped;
        } catch (Exception e) {
            Log.e(TAG, "Exception stopping service " + serviceName, e);
            return false;
        }
    }
    
    /**
     * Bind to a service for direct communication
     */
    public boolean bindService(String serviceName, Class<?> serviceClass, ServiceConnection connection) {
        try {
            Intent serviceIntent = new Intent(context, serviceClass);
            boolean bound = context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
            
            if (bound) {
                serviceConnections.put(serviceName, connection);
                Log.d(TAG, "Bound to service: " + serviceName);
            } else {
                Log.e(TAG, "Failed to bind to service: " + serviceName);
            }
            
            return bound;
        } catch (Exception e) {
            Log.e(TAG, "Exception binding to service " + serviceName, e);
            return false;
        }
    }
    
    /**
     * Unbind from a service
     */
    public void unbindService(String serviceName) {
        ServiceConnection connection = serviceConnections.remove(serviceName);
        if (connection != null) {
            try {
                context.unbindService(connection);
                Log.d(TAG, "Unbound from service: " + serviceName);
            } catch (Exception e) {
                Log.e(TAG, "Exception unbinding from service " + serviceName, e);
            }
        }
    }
    
    /**
     * Update service status and notify listeners
     */
    public void updateServiceStatus(String serviceName, boolean isRunning, String statusMessage) {
        ServiceStatus status = serviceStatuses.get(serviceName);
        if (status != null) {
            status.isRunning = isRunning;
            status.statusMessage = statusMessage;
            status.lastUpdate = System.currentTimeMillis();
        } else {
            serviceStatuses.put(serviceName, new ServiceStatus(serviceName, isRunning, statusMessage));
        }
        
        // Notify listeners on main thread
        mainHandler.post(() -> {
            for (ServiceStatusListener listener : statusListeners) {
                listener.onServiceStatusChanged(serviceName, isRunning, statusMessage);
            }
        });
        
        // Post event to event bus
        eventBus.post(new EventBus.ServiceStatusChangedEvent(TAG, serviceName, isRunning, statusMessage));
        
        Log.d(TAG, "Service status updated: " + serviceName + " - " + (isRunning ? "RUNNING" : "STOPPED") + " - " + statusMessage);
    }
    
    /**
     * Get status of a specific service
     */
    public ServiceStatus getServiceStatus(String serviceName) {
        return serviceStatuses.get(serviceName);
    }
    
    /**
     * Check if a service is running
     */
    public boolean isServiceRunning(String serviceName) {
        ServiceStatus status = serviceStatuses.get(serviceName);
        return status != null && status.isRunning;
    }
    
    /**
     * Get all service statuses
     */
    public Map<String, ServiceStatus> getAllServiceStatuses() {
        return new ConcurrentHashMap<>(serviceStatuses);
    }
    
    /**
     * Check if all critical services are running
     */
    public boolean areAllCriticalServicesRunning() {
        String[] criticalServices = {
            "TouchAutomationService",
            "ScreenCaptureService",
            "GameAutomationAccessibilityService"
        };
        
        for (String serviceName : criticalServices) {
            if (!isServiceRunning(serviceName)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Add a service status listener
     */
    public void addServiceStatusListener(ServiceStatusListener listener) {
        if (!statusListeners.contains(listener)) {
            statusListeners.add(listener);
        }
    }
    
    /**
     * Remove a service status listener
     */
    public void removeServiceStatusListener(ServiceStatusListener listener) {
        statusListeners.remove(listener);
    }
    
    /**
     * Handle service status change events from event bus
     */
    private void handleServiceStatusChanged(EventBus.ServiceStatusChangedEvent event) {
        // Additional processing for service status changes if needed
        Log.d(TAG, "Received service status change event: " + event.serviceName + " - " + event.isRunning);
    }
    
    // Message structure for ordered communication
    public static class ServiceMessage {
        public final long id;
        public final String senderId;
        public final String receiverId;
        public final String messageType;
        public final Object payload;
        public final long timestamp;
        public final long sequenceNumber;
        
        public ServiceMessage(String senderId, String receiverId, String messageType, Object payload, long sequenceNumber) {
            this.id = System.nanoTime();
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.messageType = messageType;
            this.payload = payload;
            this.timestamp = System.currentTimeMillis();
            this.sequenceNumber = sequenceNumber;
        }
        
        public String getMessageKey() {
            return senderId + "_" + receiverId + "_" + messageType;
        }
    }
    
    /**
     * Send ordered message with duplicate detection
     */
    public boolean sendOrderedMessage(String senderId, String receiverId, String messageType, Object payload) {
        synchronized (messageLock) {
            try {
                long sequenceNum = messageSequence.incrementAndGet();
                ServiceMessage message = new ServiceMessage(senderId, receiverId, messageType, payload, sequenceNum);
                String messageKey = message.getMessageKey();
                
                // Check for duplicate message
                Long lastMessageId = lastMessageIds.get(messageKey);
                if (lastMessageId != null && Math.abs(message.timestamp - lastMessageId) < 100) {
                    Log.w(TAG, "Duplicate message detected and dropped: " + messageKey);
                    return false;
                }
                
                // Add to queue for ordered processing
                messageQueue.offer(message);
                lastMessageIds.put(messageKey, message.timestamp);
                
                // Start message processing if not active
                if (!messageProcessingActive) {
                    processMessageQueue();
                }
                
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error sending ordered message", e);
                return false;
            }
        }
    }
    
    /**
     * Process message queue in order
     */
    private void processMessageQueue() {
        messageProcessingActive = true;
        
        // Process on background thread to avoid blocking
        new Thread(() -> {
            try {
                ServiceMessage message;
                while ((message = messageQueue.poll()) != null) {
                    processServiceMessage(message);
                    
                    // Small delay to maintain order
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing message queue", e);
            } finally {
                synchronized (messageLock) {
                    messageProcessingActive = false;
                }
            }
        }).start();
    }
    
    /**
     * Process individual service message
     */
    private void processServiceMessage(ServiceMessage message) {
        try {
            Log.d(TAG, "Processing message: " + message.messageType + " from " + message.senderId + " to " + message.receiverId);
            
            // Route message based on type
            switch (message.messageType) {
                case "SERVICE_START":
                    handleServiceStartMessage(message);
                    break;
                case "SERVICE_STOP":
                    handleServiceStopMessage(message);
                    break;
                case "SERVICE_STATUS":
                    handleServiceStatusMessage(message);
                    break;
                case "AI_COMMAND":
                    handleAICommandMessage(message);
                    break;
                case "COORDINATION_REQUEST":
                    handleCoordinationRequest(message);
                    break;
                default:
                    Log.w(TAG, "Unknown message type: " + message.messageType);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing service message", e);
        }
    }
    
    private void handleServiceStartMessage(ServiceMessage message) {
        String serviceName = (String) message.payload;
        Log.d(TAG, "Processing service start request for: " + serviceName);
        // Implementation for service start coordination
    }
    
    private void handleServiceStopMessage(ServiceMessage message) {
        String serviceName = (String) message.payload;
        Log.d(TAG, "Processing service stop request for: " + serviceName);
        // Implementation for service stop coordination
    }
    
    private void handleServiceStatusMessage(ServiceMessage message) {
        // Handle service status updates
        if (message.payload instanceof ServiceStatus) {
            ServiceStatus status = (ServiceStatus) message.payload;
            updateServiceStatus(status.serviceName, status.isRunning, status.statusMessage);
        }
    }
    
    private void handleAICommandMessage(ServiceMessage message) {
        Log.d(TAG, "Processing AI command message from: " + message.senderId);
        // Route AI commands to appropriate services
    }
    
    private void handleCoordinationRequest(ServiceMessage message) {
        Log.d(TAG, "Processing coordination request from: " + message.senderId);
        // Handle service coordination requests
    }

    /**
     * Start all critical services in proper order
     */
    public void startAllCriticalServices() {
        Log.d(TAG, "Starting all critical services...");
        
        // Start services in dependency order
        try {
            startService("ServiceIntegrationManager", 
                Class.forName("com.gestureai.gameautomation.managers.ServiceIntegrationManager"));
            
            Thread.sleep(500); // Allow initialization
            
            startService("ScreenCaptureService", 
                Class.forName("com.gestureai.gameautomation.services.ScreenCaptureService"));
            
            Thread.sleep(500);
            
            // Note: Accessibility services must be enabled manually by user
            Log.d(TAG, "TouchAutomationService and GameAutomationAccessibilityService must be enabled manually in Settings");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting critical services", e);
        }
    }
    
    /**
     * Get predefined service class mappings to avoid ClassNotFoundException
     */
    private Map<String, String> getServiceClassMappings() {
        Map<String, String> mappings = new HashMap<>();
        
        // Core services
        mappings.put("TouchAutomationService", "com.gestureai.gameautomation.services.TouchAutomationService");
        mappings.put("GameAutomationAccessibilityService", "com.gestureai.gameautomation.services.GameAutomationAccessibilityService");
        mappings.put("ScreenCaptureService", "com.gestureai.gameautomation.services.ScreenCaptureService");
        mappings.put("MediaProjectionService", "com.gestureai.gameautomation.services.MediaProjectionService");
        mappings.put("GestureRecognitionService", "com.gestureai.gameautomation.services.GestureRecognitionService");
        mappings.put("VoiceCommandService", "com.gestureai.gameautomation.services.VoiceCommandService");
        mappings.put("FloatingOverlayService", "com.gestureai.gameautomation.services.FloatingOverlayService");
        mappings.put("SystemIntegrationService", "com.gestureai.gameautomation.services.SystemIntegrationService");
        mappings.put("AIInferenceService", "com.gestureai.gameautomation.services.AIInferenceService");
        mappings.put("ReinforcementLearningService", "com.gestureai.gameautomation.services.ReinforcementLearningService");
        mappings.put("ModelTrainingService", "com.gestureai.gameautomation.services.ModelTrainingService");
        
        // Managers
        mappings.put("AIModelLoadingManager", "com.gestureai.gameautomation.managers.AIModelLoadingManager");
        mappings.put("GameStateManager", "com.gestureai.gameautomation.managers.GameStateManager");
        mappings.put("PermissionManager", "com.gestureai.gameautomation.managers.PermissionManager");
        mappings.put("ResourceManager", "com.gestureai.gameautomation.managers.ResourceManager");
        mappings.put("ConfigurationManager", "com.gestureai.gameautomation.managers.ConfigurationManager");
        mappings.put("DatabaseManager", "com.gestureai.gameautomation.managers.DatabaseManager");
        
        return mappings;
    }
    
    /**
     * Stop all services
     */
    public void stopAllServices() {
        Log.d(TAG, "Stopping all services...");
        
        // Use predefined service mappings to avoid ClassNotFoundException
        Map<String, String> serviceClassMappings = getServiceClassMappings();
        
        for (String serviceName : serviceStatuses.keySet()) {
            String fullClassName = serviceClassMappings.get(serviceName);
            if (fullClassName != null) {
                try {
                    Class<?> serviceClass = Class.forName(fullClassName);
                    stopService(serviceName, serviceClass);
                } catch (ClassNotFoundException e) {
                    Log.w(TAG, "Service class not found: " + fullClassName + " for " + serviceName);
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping service: " + serviceName, e);
                }
            } else {
                Log.w(TAG, "No class mapping found for service: " + serviceName);
            }
        }
        
        // Unbind from all services
        for (String serviceName : new ArrayList<>(serviceConnections.keySet())) {
            unbindService(serviceName);
        }
    }
    
    public interface ServiceStatusListener {
        void onServiceStatusChanged(String serviceName, boolean isRunning, String statusMessage);
    }
    
    public static class ServiceStatus {
        public String serviceName;
        public boolean isRunning;
        public String statusMessage;
        public long lastUpdate;
        
        public ServiceStatus(String serviceName, boolean isRunning) {
            this(serviceName, isRunning, "");
        }
        
        public ServiceStatus(String serviceName, boolean isRunning, String statusMessage) {
            this.serviceName = serviceName;
            this.isRunning = isRunning;
            this.statusMessage = statusMessage;
            this.lastUpdate = System.currentTimeMillis();
        }
    }
    
    /**
     * Capture screen through ScreenCaptureService - called by GameAutomationEngine
     */
    public android.graphics.Bitmap captureScreen() {
        try {
            ScreenCaptureService screenCapture = ScreenCaptureService.getInstance();
            if (screenCapture != null) {
                return screenCapture.captureCurrentScreen();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to capture screen through service protocol", e);
        }
        return null;
    }
    
    /**
     * Execute game action through TouchAutomationService - called by GameAutomationEngine
     */
    public boolean executeGameAction(com.gestureai.gameautomation.GameAction action) {
        try {
            TouchAutomationService touchService = TouchAutomationService.getInstance();
            if (touchService != null) {
                return touchService.executeGameAction(action);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute game action through service protocol", e);
        }
        return false;
    }
    
    /**
     * Get object detection engine instance - called by GameAutomationEngine
     */
    public com.gestureai.gameautomation.ai.ObjectDetectionEngine getObjectDetectionEngine() {
        try {
            // Return singleton instance if available
            return com.gestureai.gameautomation.ai.ObjectDetectionEngine.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get object detection engine", e);
        }
        return null;
    }
    
    /**
     * Check if services are running - called by GameAutomationEngine
     */
    public boolean areServicesRunning() {
        return areAllCriticalServicesRunning();
    }
    
    /**
     * Cleanup resources and reset singleton instance to prevent memory leaks
     */
    public static void cleanup() {
        synchronized (ServiceCommunicationProtocol.class) {
            if (instance != null) {
                try {
                    // Stop all services
                    instance.stopAllServices();
                    
                    // Clear all collections
                    if (instance.serviceStatuses != null) {
                        instance.serviceStatuses.clear();
                    }
                    if (instance.statusListeners != null) {
                        instance.statusListeners.clear();
                    }
                    if (instance.serviceConnections != null) {
                        instance.serviceConnections.clear();
                    }
                    
                    // Clear handler messages
                    if (instance.mainHandler != null) {
                        instance.mainHandler.removeCallbacksAndMessages(null);
                        instance.mainHandler = null;
                    }
                    
                    // Clear event bus reference
                    instance.eventBus = null;
                    instance.context = null;
                    
                    Log.d(TAG, "ServiceCommunicationProtocol cleaned up successfully");
                } catch (Exception e) {
                    Log.e(TAG, "Error during ServiceCommunicationProtocol cleanup", e);
                } finally {
                    // Reset singleton instance
                    instance = null;
                }
            }
        }
    }
}
package com.gestureai.gameautomation.pipeline;

import android.content.Context;
import android.util.Log;

import com.gestureai.gameautomation.managers.SystemIntegrationCoordinator;
import com.gestureai.gameautomation.services.ServiceCommunicationProtocol;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.ai.ObjectDetectionEngine;
import com.gestureai.gameautomation.ObjectLabelerEngine;
import com.gestureai.gameautomation.services.ScreenCaptureService;
import com.gestureai.gameautomation.services.TouchAutomationService;
import com.gestureai.gameautomation.services.GestureRecognitionService;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Component Integration Manager
 * Ensures all system components are properly connected and communicating
 */
public class ComponentIntegrationManager {
    private static final String TAG = "ComponentIntegration";
    
    private static ComponentIntegrationManager instance;
    private Context context;
    
    // Integration state
    private AtomicBoolean isIntegrated = new AtomicBoolean(false);
    private AtomicBoolean initializationComplete = new AtomicBoolean(false);
    
    // Core system components
    private SystemIntegrationCoordinator systemCoordinator;
    private ServiceCommunicationProtocol communicationProtocol;
    private UnifiedDataPipeline dataPipeline;
    
    // AI components
    private GameStrategyAgent strategyAgent;
    private ObjectDetectionEngine objectDetection;
    private ObjectLabelerEngine objectLabeler;
    
    // Service components
    private ScreenCaptureService screenCapture;
    private TouchAutomationService touchAutomation;
    private GestureRecognitionService gestureService;
    
    private ComponentIntegrationManager(Context context) {
        this.context = context;
        initializeComponents();
    }
    
    public static synchronized ComponentIntegrationManager getInstance(Context context) {
        if (instance == null) {
            instance = new ComponentIntegrationManager(context);
        }
        return instance;
    }
    
    /**
     * Initialize all components for integration
     */
    private void initializeComponents() {
        try {
            Log.i(TAG, "Initializing component integration");
            
            // Initialize system coordinator
            systemCoordinator = SystemIntegrationCoordinator.getInstance(context);
            
            // Get communication protocol
            communicationProtocol = ServiceCommunicationProtocol.getInstance(context);
            
            // Initialize AI components
            strategyAgent = new GameStrategyAgent();
            objectDetection = new ObjectDetectionEngine(context);
            objectLabeler = new ObjectLabelerEngine(context);
            
            initializationComplete.set(true);
            Log.i(TAG, "Component initialization complete");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize components", e);
            initializationComplete.set(false);
        }
    }
    
    /**
     * Register service instances and integrate them
     */
    public void registerService(String serviceName, Object serviceInstance) {
        try {
            Log.d(TAG, "Registering service: " + serviceName);
            
            // Register with system coordinator
            systemCoordinator.registerService(serviceName, serviceInstance);
            
            // Store service references
            switch (serviceName) {
                case "ScreenCaptureService":
                    screenCapture = (ScreenCaptureService) serviceInstance;
                    break;
                case "TouchAutomationService":
                    touchAutomation = (TouchAutomationService) serviceInstance;
                    break;
                case "GestureRecognitionService":
                    gestureService = (GestureRecognitionService) serviceInstance;
                    break;
            }
            
            // Check if integration is complete
            checkIntegrationStatus();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to register service: " + serviceName, e);
        }
    }
    
    /**
     * Start the integrated system
     */
    public boolean startIntegratedSystem() {
        if (!initializationComplete.get()) {
            Log.e(TAG, "Cannot start system - initialization not complete");
            return false;
        }
        
        if (!areAllComponentsReady()) {
            Log.e(TAG, "Cannot start system - not all components ready");
            return false;
        }
        
        try {
            // Start system coordinator
            systemCoordinator.startSystem();
            
            // Mark as integrated
            isIntegrated.set(true);
            
            Log.i(TAG, "Integrated system started successfully");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start integrated system", e);
            return false;
        }
    }
    
    /**
     * Stop the integrated system
     */
    public void stopIntegratedSystem() {
        try {
            if (systemCoordinator != null) {
                systemCoordinator.stopSystem();
            }
            
            isIntegrated.set(false);
            Log.i(TAG, "Integrated system stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping integrated system", e);
        }
    }
    
    /**
     * Check if all components are ready for integration
     */
    private boolean areAllComponentsReady() {
        return screenCapture != null && 
               touchAutomation != null && 
               gestureService != null &&
               strategyAgent != null &&
               objectDetection != null &&
               objectLabeler != null;
    }
    
    /**
     * Check integration status and auto-start if ready
     */
    private void checkIntegrationStatus() {
        if (areAllComponentsReady() && !isIntegrated.get()) {
            Log.i(TAG, "All components ready - starting integration");
            startIntegratedSystem();
        }
    }
    
    /**
     * Get integration status
     */
    public boolean isSystemIntegrated() {
        return isIntegrated.get() && initializationComplete.get();
    }
    
    /**
     * Get system coordinator
     */
    public SystemIntegrationCoordinator getSystemCoordinator() {
        return systemCoordinator;
    }
    
    /**
     * Get data pipeline from system coordinator
     */
    public UnifiedDataPipeline getDataPipeline() {
        return systemCoordinator != null ? systemCoordinator.getDataPipeline() : null;
    }
    
    /**
     * Get communication protocol
     */
    public ServiceCommunicationProtocol getCommunicationProtocol() {
        return communicationProtocol;
    }
    
    /**
     * Get integration status summary
     */
    public String getIntegrationStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Component Integration Status:\n");
        status.append("Initialized: ").append(initializationComplete.get()).append("\n");
        status.append("Integrated: ").append(isIntegrated.get()).append("\n");
        status.append("Screen Capture: ").append(screenCapture != null).append("\n");
        status.append("Touch Automation: ").append(touchAutomation != null).append("\n");
        status.append("Gesture Service: ").append(gestureService != null).append("\n");
        status.append("Strategy Agent: ").append(strategyAgent != null).append("\n");
        status.append("Object Detection: ").append(objectDetection != null).append("\n");
        status.append("Object Labeler: ").append(objectLabeler != null).append("\n");
        
        if (systemCoordinator != null) {
            status.append("\nSystem Status: ").append(systemCoordinator.getSystemStatus());
        }
        
        return status.toString();
    }
    
    /**
     * Force integration refresh
     */
    public void refreshIntegration() {
        try {
            Log.i(TAG, "Refreshing component integration");
            
            if (isIntegrated.get()) {
                stopIntegratedSystem();
            }
            
            checkIntegrationStatus();
            
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing integration", e);
        }
    }
    
    /**
     * Cleanup integration resources
     */
    public void cleanup() {
        try {
            stopIntegratedSystem();
            
            if (systemCoordinator != null) {
                systemCoordinator.cleanup();
            }
            
            // Clear service references
            screenCapture = null;
            touchAutomation = null;
            gestureService = null;
            strategyAgent = null;
            objectDetection = null;
            objectLabeler = null;
            
            isIntegrated.set(false);
            initializationComplete.set(false);
            
            Log.i(TAG, "Component integration cleaned up");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
}
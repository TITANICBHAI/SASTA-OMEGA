package com.gestureai.gameautomation.managers;

import android.content.Context;
import android.util.Log;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coordinates proper service initialization order to prevent dependency failures
 */
public class ServiceInitializationCoordinator {
    private static final String TAG = "ServiceInitCoordinator";
    private static volatile ServiceInitializationCoordinator instance;
    private static final Object lock = new Object();
    
    private final Context context;
    private final ExecutorService initExecutor;
    private final AtomicBoolean isInitializing = new AtomicBoolean(false);
    private final AtomicInteger initializationPhase = new AtomicInteger(0);
    
    public enum InitializationPhase {
        NOT_STARTED(0),
        DATABASE_INIT(1),
        RESOURCE_MANAGERS(2),
        AI_MODEL_LOADING(3),
        COMMUNICATION_PROTOCOL(4),
        INDIVIDUAL_SERVICES(5),
        UI_COMPONENTS(6),
        COMPLETE(7);
        
        private final int value;
        InitializationPhase(int value) { this.value = value; }
        public int getValue() { return value; }
    }
    
    public interface InitializationListener {
        void onPhaseStarted(InitializationPhase phase);
        void onPhaseCompleted(InitializationPhase phase);
        void onInitializationComplete();
        void onInitializationFailed(InitializationPhase phase, Exception error);
    }
    
    private InitializationListener listener;
    
    public static ServiceInitializationCoordinator getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ServiceInitializationCoordinator(context);
                }
            }
        }
        return instance;
    }
    
    private ServiceInitializationCoordinator(Context context) {
        this.context = context.getApplicationContext();
        this.initExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ServiceInitialization");
            t.setPriority(Thread.NORM_PRIORITY + 1);
            return t;
        });
    }
    
    public void setInitializationListener(InitializationListener listener) {
        this.listener = listener;
    }
    
    public CompletableFuture<Boolean> initializeAllServices() {
        if (isInitializing.get()) {
            Log.w(TAG, "Initialization already in progress");
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            isInitializing.set(true);
            initializationPhase.set(0);
            
            try {
                // Phase 1: Database initialization
                if (!initializePhase(InitializationPhase.DATABASE_INIT, this::initializeDatabase)) {
                    return false;
                }
                
                // Phase 2: Resource managers
                if (!initializePhase(InitializationPhase.RESOURCE_MANAGERS, this::initializeResourceManagers)) {
                    return false;
                }
                
                // Phase 3: AI model loading
                if (!initializePhase(InitializationPhase.AI_MODEL_LOADING, this::initializeAIModels)) {
                    return false;
                }
                
                // Phase 4: Communication protocol
                if (!initializePhase(InitializationPhase.COMMUNICATION_PROTOCOL, this::initializeCommunicationProtocol)) {
                    return false;
                }
                
                // Phase 5: Individual services
                if (!initializePhase(InitializationPhase.INDIVIDUAL_SERVICES, this::initializeIndividualServices)) {
                    return false;
                }
                
                // Phase 6: UI components
                if (!initializePhase(InitializationPhase.UI_COMPONENTS, this::initializeUIComponents)) {
                    return false;
                }
                
                initializationPhase.set(InitializationPhase.COMPLETE.getValue());
                if (listener != null) {
                    listener.onInitializationComplete();
                }
                
                Log.i(TAG, "All services initialized successfully");
                return true;
                
            } catch (Exception e) {
                Log.e(TAG, "Initialization failed", e);
                if (listener != null) {
                    InitializationPhase currentPhase = getCurrentPhase();
                    listener.onInitializationFailed(currentPhase, e);
                }
                return false;
            } finally {
                isInitializing.set(false);
            }
        }, initExecutor);
    }
    
    private boolean initializePhase(InitializationPhase phase, Runnable initAction) {
        try {
            initializationPhase.set(phase.getValue());
            if (listener != null) {
                listener.onPhaseStarted(phase);
            }
            
            Log.d(TAG, "Starting initialization phase: " + phase.name());
            initAction.run();
            
            if (listener != null) {
                listener.onPhaseCompleted(phase);
            }
            
            Log.d(TAG, "Completed initialization phase: " + phase.name());
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Phase " + phase.name() + " failed", e);
            if (listener != null) {
                listener.onInitializationFailed(phase, e);
            }
            return false;
        }
    }
    
    private void initializeDatabase() {
        Log.d(TAG, "Initializing database systems");
        // Database initialization - handled by AppDatabase.getDatabase()
        Thread.sleep(500); // Simulated initialization time
    }
    
    private void initializeResourceManagers() {
        Log.d(TAG, "Initializing resource managers");
        
        // Initialize memory manager
        MemoryManager.getInstance(context);
        
        // Initialize resource manager  
        ResourceManager.getInstance(context);
        
        Thread.sleep(300);
    }
    
    private void initializeAIModels() {
        Log.d(TAG, "Initializing AI models");
        
        // Initialize AI model loading manager
        AIModelLoadingManager modelManager = AIModelLoadingManager.getInstance(context);
        modelManager.loadAllModelsAsync();
        
        Thread.sleep(1000); // AI models take longer to load
    }
    
    private void initializeCommunicationProtocol() {
        Log.d(TAG, "Initializing communication protocol");
        
        // Initialize service communication
        ServiceCommunicationProtocol.getInstance(context);
        
        Thread.sleep(200);
    }
    
    private void initializeIndividualServices() {
        Log.d(TAG, "Initializing individual services");
        
        // Initialize core services in order
        UnifiedServiceCoordinator.getInstance(context);
        SystemIntegrationCoordinator.getInstance(context);
        
        // Initialize MediaPipe manager with fallback
        MediaPipeManager.getInstance(context).initialize();
        
        Thread.sleep(800);
    }
    
    private void initializeUIComponents() {
        Log.d(TAG, "Initializing UI components");
        // UI components typically initialize themselves
        Thread.sleep(100);
    }
    
    private InitializationPhase getCurrentPhase() {
        int currentValue = initializationPhase.get();
        for (InitializationPhase phase : InitializationPhase.values()) {
            if (phase.getValue() == currentValue) {
                return phase;
            }
        }
        return InitializationPhase.NOT_STARTED;
    }
    
    public boolean isInitialized() {
        return initializationPhase.get() == InitializationPhase.COMPLETE.getValue();
    }
    
    public InitializationPhase getCurrentInitializationPhase() {
        return getCurrentPhase();
    }
    
    public void cleanup() {
        try {
            if (initExecutor != null && !initExecutor.isShutdown()) {
                initExecutor.shutdown();
                if (!initExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    initExecutor.shutdownNow();
                }
            }
            
            isInitializing.set(false);
            initializationPhase.set(0);
            
            Log.d(TAG, "Service initialization coordinator cleaned up");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
}
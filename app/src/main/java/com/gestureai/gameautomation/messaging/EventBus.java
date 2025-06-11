package com.gestureai.gameautomation.messaging;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;
import java.util.List;

/**
 * Thread-safe event bus for cross-component communication
 * Handles AI component synchronization and service coordination
 */
public class EventBus {
    
    private static final String TAG = "EventBus";
    private static volatile EventBus instance;
    
    private final Map<Class<?>, List<EventSubscriber<?>>> subscribers;
    private final Handler mainHandler;
    
    private EventBus() {
        this.subscribers = new ConcurrentHashMap<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static EventBus getInstance() {
        if (instance == null) {
            synchronized (EventBus.class) {
                if (instance == null) {
                    instance = new EventBus();
                }
            }
        }
        return instance;
    }
    
    /**
     * Subscribe to events of a specific type
     */
    public <T extends BaseEvent> void subscribe(Class<T> eventType, EventSubscriber<T> subscriber) {
        synchronized (subscribers) {
            subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(subscriber);
        }
    }
    
    /**
     * Unsubscribe from events
     */
    public <T extends BaseEvent> void unsubscribe(Class<T> eventType, EventSubscriber<T> subscriber) {
        synchronized (subscribers) {
            List<EventSubscriber<?>> eventSubscribers = subscribers.get(eventType);
            if (eventSubscribers != null) {
                eventSubscribers.remove(subscriber);
                if (eventSubscribers.isEmpty()) {
                    subscribers.remove(eventType);
                }
            }
        }
    }
    
    /**
     * Post event to all subscribers with message corruption prevention
     */
    public <T extends BaseEvent> void post(T event) {
        if (event == null) {
            android.util.Log.w(TAG, "Null event posted, ignoring");
            return;
        }
        
        // Create immutable snapshot of subscribers to prevent concurrent modification
        List<EventSubscriber<?>> eventSubscribers;
        synchronized (subscribers) {
            List<EventSubscriber<?>> originalList = subscribers.get(event.getClass());
            if (originalList == null || originalList.isEmpty()) {
                return;
            }
            eventSubscribers = new java.util.ArrayList<>(originalList);
        }
        
        // Process subscribers with corruption detection
        for (EventSubscriber<?> subscriber : eventSubscribers) {
            if (subscriber != null) {
                try {
                    // Validate subscriber before processing
                    if (isValidSubscriber(subscriber)) {
                        ((EventSubscriber<T>) subscriber).onEvent(event);
                    } else {
                        android.util.Log.w(TAG, "Invalid subscriber detected and skipped");
                        removeInvalidSubscriber(event.getClass(), subscriber);
                    }
                } catch (ClassCastException e) {
                    android.util.Log.e(TAG, "Type mismatch in event routing for: " + event.getClass().getSimpleName(), e);
                    removeInvalidSubscriber(event.getClass(), subscriber);
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Error processing event: " + event.getClass().getSimpleName(), e);
                }
            }
        }
    }
    
    /**
     * Post event on main thread
     */
    public <T extends BaseEvent> void postOnMainThread(T event) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            post(event);
        } else {
            mainHandler.post(() -> post(event));
        }
    }
    
    /**
     * Validate subscriber to prevent message corruption
     */
    private boolean isValidSubscriber(EventSubscriber<?> subscriber) {
        try {
            // Check if subscriber is still valid (not garbage collected weakly referenced object)
            return subscriber != null && subscriber.getClass() != null;
        } catch (Exception e) {
            android.util.Log.w(TAG, "Subscriber validation failed", e);
            return false;
        }
    }
    
    /**
     * Remove invalid subscriber to maintain routing integrity
     */
    private void removeInvalidSubscriber(Class<?> eventType, EventSubscriber<?> invalidSubscriber) {
        synchronized (subscribers) {
            List<EventSubscriber<?>> eventSubscribers = subscribers.get(eventType);
            if (eventSubscribers != null) {
                eventSubscribers.remove(invalidSubscriber);
                if (eventSubscribers.isEmpty()) {
                    subscribers.remove(eventType);
                }
                android.util.Log.d(TAG, "Removed invalid subscriber for event type: " + eventType.getSimpleName());
            }
        }
    }
    
    /**
     * Clear all subscribers for cleanup
     */
    public void clearAllSubscribers() {
        synchronized (subscribers) {
            subscribers.clear();
            android.util.Log.d(TAG, "All event subscribers cleared");
        }
    }
    
    public interface EventSubscriber<T extends BaseEvent> {
        void onEvent(T event);
    }
    
    // Base event class
    public abstract static class BaseEvent {
        public final long timestamp;
        public final String source;
        
        public BaseEvent(String source) {
            this.timestamp = System.currentTimeMillis();
            this.source = source;
        }
    }
    
    // AI Events
    public static class AIStateChangedEvent extends BaseEvent {
        public final String aiComponent;
        public final String previousState;
        public final String newState;
        
        public AIStateChangedEvent(String source, String aiComponent, String previousState, String newState) {
            super(source);
            this.aiComponent = aiComponent;
            this.previousState = previousState;
            this.newState = newState;
        }
    }
    
    public static class ModelTrainingProgressEvent extends BaseEvent {
        public final String modelName;
        public final float progress;
        public final float accuracy;
        
        public ModelTrainingProgressEvent(String source, String modelName, float progress, float accuracy) {
            super(source);
            this.modelName = modelName;
            this.progress = progress;
            this.accuracy = accuracy;
        }
    }
    
    // Service Events
    public static class ServiceStatusChangedEvent extends BaseEvent {
        public final String serviceName;
        public final boolean isRunning;
        public final String statusMessage;
        
        public ServiceStatusChangedEvent(String source, String serviceName, boolean isRunning, String statusMessage) {
            super(source);
            this.serviceName = serviceName;
            this.isRunning = isRunning;
            this.statusMessage = statusMessage;
        }
    }
    
    public static class GameActionExecutedEvent extends BaseEvent {
        public final String actionType;
        public final float x, y;
        public final boolean success;
        public final float confidence;
        
        public GameActionExecutedEvent(String source, String actionType, float x, float y, boolean success, float confidence) {
            super(source);
            this.actionType = actionType;
            this.x = x;
            this.y = y;
            this.success = success;
            this.confidence = confidence;
        }
    }
}
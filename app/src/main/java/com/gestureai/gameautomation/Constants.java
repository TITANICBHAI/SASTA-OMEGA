package com.gestureai.gameautomation;

public final class Constants {
    // Request codes for permissions and activities
    public static final int REQUEST_CODE_ACCESSIBILITY = 1001;
    public static final int REQUEST_CODE_OVERLAY = 1002;
    public static final int REQUEST_CODE_CAMERA = 1003;
    public static final int REQUEST_CODE_AUDIO = 1004;
    public static final int REQUEST_CODE_STORAGE = 1005;
    
    // Gesture detection constants
    public static final float DEFAULT_CONFIDENCE_THRESHOLD = 0.7f;
    public static final long DEFAULT_GESTURE_DEBOUNCE_TIME = 200L; // milliseconds
    
    // Touch action timing
    public static final long TAP_DURATION = 50L;
    public static final long LONG_PRESS_DURATION = 1000L;
    public static final long SWIPE_DURATION = 300L;
    
    // Screen dimensions (default for 1080p)
    public static final int SCREEN_WIDTH = 1080;
    public static final int SCREEN_HEIGHT = 1920;
    // Touch coordinates for game actions
    public static final int JUMP_START_X = SCREEN_WIDTH / 2;
    public static final int JUMP_START_Y = SCREEN_HEIGHT - 200;
    public static final int JUMP_END_X = SCREEN_WIDTH / 2;
    public static final int JUMP_END_Y = SCREEN_HEIGHT - 600;

    public static final int SLIDE_START_X = SCREEN_WIDTH / 2;
    public static final int SLIDE_START_Y = SCREEN_HEIGHT - 400;
    public static final int SLIDE_END_X = SCREEN_WIDTH / 2;
    public static final int SLIDE_END_Y = SCREEN_HEIGHT - 200;

    public static final int MOVE_LEFT_START_X = SCREEN_WIDTH / 2;
    public static final int MOVE_LEFT_START_Y = SCREEN_HEIGHT / 2;
    public static final int MOVE_LEFT_END_X = SCREEN_WIDTH / 4;
    public static final int MOVE_LEFT_END_Y = SCREEN_HEIGHT / 2;

    public static final int MOVE_RIGHT_START_X = SCREEN_WIDTH / 2;
    public static final int MOVE_RIGHT_START_Y = SCREEN_HEIGHT / 2;
    public static final int MOVE_RIGHT_END_X = (SCREEN_WIDTH * 3) / 4;
    public static final int MOVE_RIGHT_END_Y = SCREEN_HEIGHT / 2;

    public static final int SCREEN_WIDTH_REFERENCE = SCREEN_WIDTH;
    public static final int SCREEN_HEIGHT_REFERENCE = SCREEN_HEIGHT;
    public static GameAction createSwipe(int startX, int startY, int endX, int endY, long duration) {
        return new GameAction("SWIPE", startX, startY, 1.0f,
                String.format("endX=%d,endY=%d,duration=%d", endX, endY, duration));
    }


    public static GameAction createTap(int x, int y) {
        return new GameAction("TAP", x, y, 1.0f, "");
    }
    // Game-specific constants
    public static final String SUBWAY_SURFERS_PACKAGE = "com.kiloo.subwaysurf";
    public static final String TEMPLE_RUN_PACKAGE = "com.imangi.templerun";
    
    // Service notification IDs
    public static final int GESTURE_SERVICE_NOTIFICATION_ID = 1001;
    public static final int OVERLAY_SERVICE_NOTIFICATION_ID = 1002;
    public static final int SCREEN_CAPTURE_NOTIFICATION_ID = 1003;
    
    // Performance monitoring
    public static final int PERFORMANCE_MONITORING_INTERVAL = 1000; // milliseconds
    public static final int MAX_FPS_TARGET = 30;
    
    // Database constants
    public static final String DATABASE_NAME = "gesture_automation.db";
    public static final int DATABASE_VERSION = 1;
    
    // Asset file names
    public static final String GESTURE_MODEL_FILE = "gesture_classifier.tflite";
    public static final String OPENNLP_MODELS_DIR = "opennlp";
    
    // Error messages
    public static final String ERROR_ACCESSIBILITY_NOT_ENABLED = "Accessibility service not enabled";
    public static final String ERROR_OVERLAY_PERMISSION_DENIED = "Overlay permission denied";
    public static final String ERROR_CAMERA_PERMISSION_DENIED = "Camera permission denied";
    public static final String ERROR_INITIALIZATION_FAILED = "Failed to initialize service";
    
    // Private constructor to prevent instantiation
    private Constants() {
        throw new AssertionError("Constants class cannot be instantiated");
    }
}
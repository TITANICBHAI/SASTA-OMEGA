
package com.gestureai.gameautomation;

public class GameConstants {
    // Gesture Types
    public static final String GESTURE_SWIPE_LEFT = "swipe_left";
    public static final String GESTURE_SWIPE_RIGHT = "swipe_right";
    public static final String GESTURE_SWIPE_UP = "swipe_up";
    public static final String GESTURE_SWIPE_DOWN = "swipe_down";
    public static final String GESTURE_TAP = "tap";
    public static final String GESTURE_DOUBLE_TAP = "double_tap";
    public static final String GESTURE_LONG_PRESS = "long_press";
    public static final String GESTURE_PINCH = "pinch";
    public static final String GESTURE_SPREAD = "spread";

    // Action Types
    public static final String ACTION_MOVE_LEFT = "move_left";
    public static final String ACTION_MOVE_RIGHT = "move_right";
    public static final String ACTION_JUMP = "jump";
    public static final String ACTION_SLIDE = "slide";
    public static final String ACTION_COLLECT = "collect";
    public static final String ACTION_BOOST = "boost";
    public static final String ACTION_PAUSE = "pause";
    public static final String ACTION_AVOID = "avoid";

    // Game Object Types
    public static final int OBJECT_COIN = 0;
    public static final int OBJECT_OBSTACLE = 1;
    public static final int OBJECT_POWER_UP = 2;
    public static final int OBJECT_TRAIN = 3;
    public static final int OBJECT_TUNNEL = 4;
    public static final int OBJECT_BARRIER = 5;

    // Lane Positions
    public static final int LANE_LEFT = 0;
    public static final int LANE_CENTER = 1;
    public static final int LANE_RIGHT = 2;

    // Timing Constants
    public static final long GESTURE_DEBOUNCE_TIME = 200;
    public static final long ACTION_DELAY = 50;
    public static final long SCREEN_CAPTURE_INTERVAL = 33; // ~30 FPS

    // Confidence Thresholds
    public static final float MIN_GESTURE_CONFIDENCE = 0.7f;
    public static final float MIN_OBJECT_CONFIDENCE = 0.6f;
    public static final float HIGH_CONFIDENCE_THRESHOLD = 0.9f;

    // Screen Regions (for 1080x1920 screen)
    public static final int GAME_AREA_TOP = 200;
    public static final int GAME_AREA_BOTTOM = 1700;
    public static final int GAME_AREA_LEFT = 50;
    public static final int GAME_AREA_RIGHT = 1030;

    // Touch Coordinates
    public static final int SWIPE_LEFT_X = 200;
    public static final int SWIPE_RIGHT_X = 880;
    public static final int SWIPE_CENTER_X = 540;
    public static final int SWIPE_Y_START = 1400;
    public static final int SWIPE_Y_END = 600;

    // Game Specific Settings
    public static final String SUBWAY_SURFERS_PACKAGE = "com.kiloo.subwaysurf";
    public static final String FREE_FIRE_PACKAGE = "com.dts.freefireth";
    public static final String PUBG_PACKAGE = "com.tencent.ig";
    public static final String COD_PACKAGE = "com.activision.callofduty.shooter";
}

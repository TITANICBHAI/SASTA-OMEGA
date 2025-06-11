package com.gestureai.gameautomation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.*;

/**
 * Advanced minimap analysis for battle royale zone tracking and spatial awareness
 */
public class MinimapAnalyzer {
    private static final String TAG = "MinimapAnalyzer";

    private Context context;
    private TensorFlowLiteHelper tfliteHelper;
    private boolean isInitialized = false;

    public static class MinimapData {
        public Rect minimapRegion;
        public ZoneInfo currentZone;
        public ZoneInfo nextZone;
        public PlayerPosition playerPosition;
        public List<MarkerInfo> markers;
        public float mapScale;
        public float mapRotation;
        public long lastUpdateTime;

        public MinimapData() {
            this.markers = new ArrayList<>();
            this.lastUpdateTime = System.currentTimeMillis();
        }
    }

    public static class ZoneInfo {
        public float[] center; // [x, y] normalized coordinates
        public float radius; // normalized radius
        public float timeRemaining; // seconds until zone closes
        public ZonePhase phase;
        public float damagePerSecond;

        public ZoneInfo() {
            this.center = new float[2];
        }
    }

    public static class PlayerPosition {
        public float[] coordinates; // [x, y] on minimap
        public float[] worldCoordinates; // [x, y] in game world
        public float facing; // direction player is facing (0-360 degrees)
        public boolean isInSafeZone;
        public float distanceToZone;

        public PlayerPosition() {
            this.coordinates = new float[2];
            this.worldCoordinates = new float[2];
        }
    }

    public static class MarkerInfo {
        public MarkerType type;
        public float[] position; // [x, y] normalized
        public String label;
        public float confidence;
        public boolean isInteractive;

        public MarkerInfo(MarkerType type, float[] position, float confidence) {
            this.type = type;
            this.position = position;
            this.confidence = confidence;
        }
    }

    public enum ZonePhase {
        PREPARATION, SHRINKING, STATIC, FINAL_CIRCLE
    }

    public enum MarkerType {
        TEAMMATE, ENEMY, LOOT, VEHICLE, OBJECTIVE, BUILDING, LANDMARK
    }

    public MinimapAnalyzer(Context context, TensorFlowLiteHelper tfliteHelper) {
        this.context = context;
        this.tfliteHelper = tfliteHelper;
        initialize();
    }

    private void initialize() {
        try {
            // Load minimap analysis model
            tfliteHelper.loadModel("minimap_analyzer");
            isInitialized = true;
            Log.d(TAG, "Minimap Analyzer initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Minimap Analyzer", e);
        }
    }

    public MinimapData analyzeScreen(Bitmap gameScreen) {
        MinimapData data = new MinimapData();

        if (!isInitialized || gameScreen == null) {
            return data;
        }

        try {
            // Step 1: Locate minimap region
            data.minimapRegion = findMinimapRegion(gameScreen);

            if (data.minimapRegion == null) {
                Log.w(TAG, "Minimap not found in screen");
                return data;
            }

            // Step 2: Extract minimap image
            Bitmap minimapBitmap = extractMinimapBitmap(gameScreen, data.minimapRegion);

            // Step 3: Analyze zones
            analyzeZones(minimapBitmap, data);

            // Step 4: Find player position
            analyzePlayerPosition(minimapBitmap, data);

            // Step 5: Detect markers and POIs
            analyzeMarkers(minimapBitmap, data);

            // Step 6: Calculate spatial relationships
            calculateSpatialData(data);

            Log.d(TAG, "Minimap analysis complete - Zone phase: " +
                    (data.currentZone != null ? data.currentZone.phase : "unknown"));

        } catch (Exception e) {
            Log.e(TAG, "Error analyzing minimap", e);
        }

        return data;
    }

    private Rect findMinimapRegion(Bitmap screen) {
        // Common minimap locations in mobile games
        int width = screen.getWidth();
        int height = screen.getHeight();

        // Top-right corner (most common)
        Rect topRight = new Rect(width - 250, 0, width, 250);
        if (isMinimapRegion(screen, topRight)) {
            return topRight;
        }

        // Top-left corner
        Rect topLeft = new Rect(0, 0, 250, 250);
        if (isMinimapRegion(screen, topLeft)) {
            return topLeft;
        }

        // Bottom-right corner
        Rect bottomRight = new Rect(width - 200, height - 200, width, height);
        if (isMinimapRegion(screen, bottomRight)) {
            return bottomRight;
        }

        // Use ML detection as fallback
        return findMinimapWithML(screen);
    }

    private boolean isMinimapRegion(Bitmap screen, Rect region) {
        try {
            // Extract region
            Bitmap regionBitmap = Bitmap.createBitmap(screen,
                    region.left, region.top, region.width(), region.height());

            // Check for circular patterns (common in minimaps)
            Mat regionMat = new Mat();
            Utils.bitmapToMat(regionBitmap, regionMat);

            // Convert to grayscale
            Mat grayMat = new Mat();
            Imgproc.cvtColor(regionMat, grayMat, Imgproc.COLOR_RGB2GRAY);

            // Detect circles using HoughCircles
            Mat circles = new Mat();
            Imgproc.HoughCircles(grayMat, circles, Imgproc.HOUGH_GRADIENT, 1,
                    grayMat.rows()/8f, 200, 100, 30, 120);

            // If we found circles, likely a minimap
            return circles.cols() > 0;

        } catch (Exception e) {
            Log.w(TAG, "Error checking minimap region", e);
            return false;
        }
    }

    private Rect findMinimapWithML(Bitmap screen) {
        try {
            List<TensorFlowLiteHelper.DetectionResult> results =
                    tfliteHelper.runInference("ui_detector", screen);

            for (TensorFlowLiteHelper.DetectionResult result : results) {
                if ("minimap".equals(result.className) && result.confidence > 0.7f) {
                    float[] box = result.boundingBox;
                    return new Rect(
                            (int)(box[0] * screen.getWidth()),
                            (int)(box[1] * screen.getHeight()),
                            (int)((box[0] + box[2]) * screen.getWidth()),
                            (int)((box[1] + box[3]) * screen.getHeight())
                    );
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "ML minimap detection failed", e);
        }

        return null;
    }

    private Bitmap extractMinimapBitmap(Bitmap screen, Rect region) {
        return Bitmap.createBitmap(screen,
                region.left, region.top, region.width(), region.height());
    }

    private void analyzeZones(Bitmap minimap, MinimapData data) {
        try {
            // Use ML model for zone detection
            List<TensorFlowLiteHelper.DetectionResult> zoneResults =
                    tfliteHelper.runInference("minimap_analyzer", minimap);

            // Convert to OpenCV for advanced processing
            Mat minimapMat = new Mat();
            Utils.bitmapToMat(minimap, minimapMat);

            // Detect current safe zone (white circle)
            data.currentZone = detectSafeZone(minimapMat, zoneResults);

            // Detect next zone (blue circle)
            data.nextZone = detectNextZone(minimapMat, zoneResults);

            // Determine zone phase
            determineZonePhase(data);

        } catch (Exception e) {
            Log.w(TAG, "Zone analysis failed", e);
        }
    }

    private ZoneInfo detectSafeZone(Mat minimap, List<TensorFlowLiteHelper.DetectionResult> mlResults) {
        ZoneInfo zone = new ZoneInfo();

        // First try ML detection
        for (TensorFlowLiteHelper.DetectionResult result : mlResults) {
            if ("safe_zone".equals(result.className)) {
                float[] box = result.boundingBox;
                zone.center[0] = box[0] + box[2] / 2f;
                zone.center[1] = box[1] + box[3] / 2f;
                zone.radius = Math.max(box[2], box[3]) / 2f;
                return zone;
            }
        }

        // Fallback to color-based detection
        return detectZoneByColor(minimap, new Scalar(200, 200, 200), new Scalar(255, 255, 255));
    }

    private ZoneInfo detectNextZone(Mat minimap, List<TensorFlowLiteHelper.DetectionResult> mlResults) {
        ZoneInfo zone = new ZoneInfo();

        // ML detection first
        for (TensorFlowLiteHelper.DetectionResult result : mlResults) {
            if ("storm".equals(result.className) || "next_zone".equals(result.className)) {
                float[] box = result.boundingBox;
                zone.center[0] = box[0] + box[2] / 2f;
                zone.center[1] = box[1] + box[3] / 2f;
                zone.radius = Math.max(box[2], box[3]) / 2f;
                return zone;
            }
        }

        // Detect blue circle for next zone
        return detectZoneByColor(minimap, new Scalar(100, 150, 200), new Scalar(150, 200, 255));
    }

    private ZoneInfo detectZoneByColor(Mat minimap, Scalar lowerBound, Scalar upperBound) {
        ZoneInfo zone = new ZoneInfo();

        try {
            // Create mask for target color
            Mat mask = new Mat();
            Core.inRange(minimap, lowerBound, upperBound, mask);

            // Find contours
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // Find largest circular contour
            double maxArea = 0;
            MatOfPoint bestContour = null;

            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area > maxArea) {
                    maxArea = area;
                    bestContour = contour;
                }
            }

            if (bestContour != null) {
                // Get bounding circle
                Point center = new Point();
                float[] radius = new float[1];
                Imgproc.minEnclosingCircle(new MatOfPoint2f(bestContour.toArray()), center, radius);

                zone.center[0] = (float)(center.x / minimap.width());
                zone.center[1] = (float)(center.y / minimap.height());
                zone.radius = radius[0] / Math.max(minimap.width(), minimap.height());
            }

        } catch (Exception e) {
            Log.w(TAG, "Color-based zone detection failed", e);
        }

        return zone;
    }

    private void determineZonePhase(MinimapData data) {
        if (data.currentZone == null) {
            return;
        }

        // Determine phase based on zone size and timing
        if (data.currentZone.radius > 0.8f) {
            data.currentZone.phase = ZonePhase.PREPARATION;
            data.currentZone.timeRemaining = 120f; // Typical preparation time
        } else if (data.currentZone.radius > 0.2f) {
            data.currentZone.phase = ZonePhase.SHRINKING;
            data.currentZone.timeRemaining = 180f; // Typical shrinking time
        } else if (data.currentZone.radius > 0.1f) {
            data.currentZone.phase = ZonePhase.STATIC;
            data.currentZone.timeRemaining = 60f;
        } else {
            data.currentZone.phase = ZonePhase.FINAL_CIRCLE;
            data.currentZone.timeRemaining = 30f;
        }

        // Set damage based on phase
        switch (data.currentZone.phase) {
            case PREPARATION:
                data.currentZone.damagePerSecond = 1f;
                break;
            case SHRINKING:
                data.currentZone.damagePerSecond = 2f;
                break;
            case STATIC:
                data.currentZone.damagePerSecond = 5f;
                break;
            case FINAL_CIRCLE:
                data.currentZone.damagePerSecond = 10f;
                break;
        }
    }

    private void analyzePlayerPosition(Bitmap minimap, MinimapData data) {
        try {
            data.playerPosition = new PlayerPosition();

            // Look for player marker (usually center or distinct icon)
            Point playerPos = findPlayerMarker(minimap);

            if (playerPos != null) {
                data.playerPosition.coordinates[0] = (float)(playerPos.x / minimap.getWidth());
                data.playerPosition.coordinates[1] = (float)(playerPos.y / minimap.getHeight());

                // Convert to world coordinates (simplified)
                data.playerPosition.worldCoordinates[0] = data.playerPosition.coordinates[0] * 8000; // Typical map size
                data.playerPosition.worldCoordinates[1] = data.playerPosition.coordinates[1] * 8000;

                // Check if in safe zone
                if (data.currentZone != null) {
                    float dx = data.playerPosition.coordinates[0] - data.currentZone.center[0];
                    float dy = data.playerPosition.coordinates[1] - data.currentZone.center[1];
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);

                    data.playerPosition.isInSafeZone = distance <= data.currentZone.radius;
                    data.playerPosition.distanceToZone = Math.max(0, distance - data.currentZone.radius);
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Player position analysis failed", e);
        }
    }

    private Point findPlayerMarker(Bitmap minimap) {
        try {
            Mat minimapMat = new Mat();
            Utils.bitmapToMat(minimap, minimapMat);

            // Player is often at center of minimap
            Point center = new Point(minimapMat.width() / 2.0, minimapMat.height() / 2.0);

            // Look for arrow or triangle indicating player direction
            Mat mask = new Mat();
            Scalar lowerBound = new Scalar(0, 100, 0);   // Green player marker
            Scalar upperBound = new Scalar(100, 255, 100);
            Core.inRange(minimapMat, lowerBound, upperBound, mask);

            // Find brightest spot (likely player marker)
            Core.MinMaxLocResult result = Core.minMaxLoc(mask);

            return result.maxLoc;

        } catch (Exception e) {
            Log.w(TAG, "Player marker detection failed", e);
            return null;
        }
    }

    private void analyzeMarkers(Bitmap minimap, MinimapData data) {
        try {
            // Use ML model to detect various markers
            List<TensorFlowLiteHelper.DetectionResult> markerResults =
                    tfliteHelper.runInference("minimap_analyzer", minimap);

            for (TensorFlowLiteHelper.DetectionResult result : markerResults) {
                MarkerType type = mapResultToMarkerType(result.className);
                if (type != null) {
                    float[] position = {
                            result.boundingBox[0] + result.boundingBox[2] / 2f,
                            result.boundingBox[1] + result.boundingBox[3] / 2f
                    };

                    MarkerInfo marker = new MarkerInfo(type, position, result.confidence);
                    marker.label = result.className;
                    data.markers.add(marker);
                }
            }

            // Additional color-based marker detection
            detectColorBasedMarkers(minimap, data);

        } catch (Exception e) {
            Log.w(TAG, "Marker analysis failed", e);
        }
    }

    private MarkerType mapResultToMarkerType(String className) {
        switch (className.toLowerCase()) {
            case "teammate_marker":
                return MarkerType.TEAMMATE;
            case "enemy_marker":
                return MarkerType.ENEMY;
            case "loot":
                return MarkerType.LOOT;
            case "vehicle":
                return MarkerType.VEHICLE;
            case "objective":
                return MarkerType.OBJECTIVE;
            case "building":
                return MarkerType.BUILDING;
            default:
                return null;
        }
    }

    private void detectColorBasedMarkers(Bitmap minimap, MinimapData data) {
        Mat minimapMat = new Mat();
        Utils.bitmapToMat(minimap, minimapMat);

        // Detect blue teammate markers
        detectMarkersByColor(minimapMat, new Scalar(0, 0, 150), new Scalar(100, 100, 255),
                MarkerType.TEAMMATE, data);

        // Detect red enemy markers
        detectMarkersByColor(minimapMat, new Scalar(150, 0, 0), new Scalar(255, 100, 100),
                MarkerType.ENEMY, data);

        // Detect yellow/orange loot markers
        detectMarkersByColor(minimapMat, new Scalar(150, 150, 0), new Scalar(255, 255, 100),
                MarkerType.LOOT, data);
    }

    private void detectMarkersByColor(Mat minimap, Scalar lower, Scalar upper,
                                      MarkerType type, MinimapData data) {
        try {
            Mat mask = new Mat();
            Core.inRange(minimap, lower, upper, mask);

            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area > 5 && area < 200) { // Filter by size
                    // Use OpenCV Rect explicitly
                    org.opencv.core.Rect boundingRect = Imgproc.boundingRect(contour);
                    float[] position = {
                            (boundingRect.x + boundingRect.width / 2f) / minimap.width(),
                            (boundingRect.y + boundingRect.height / 2f) / minimap.height()
                    };

                    MarkerInfo marker = new MarkerInfo(type, position, 0.8f);
                    data.markers.add(marker);
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Color-based marker detection failed", e);
        }
    }

    private void calculateSpatialData(MinimapData data) {
        if (data.playerPosition == null) return;

        // Calculate distances to markers
        for (MarkerInfo marker : data.markers) {
            float dx = marker.position[0] - data.playerPosition.coordinates[0];
            float dy = marker.position[1] - data.playerPosition.coordinates[1];
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            // Store distance as confidence adjustment
            marker.confidence *= Math.max(0.1f, 1.0f - distance);
        }

        // Calculate map scale based on zone size
        if (data.currentZone != null) {
            data.mapScale = 1.0f / data.currentZone.radius;
        }

        // Calculate optimal rotation path if needed
        if (data.currentZone != null && !data.playerPosition.isInSafeZone) {
            calculateOptimalPath(data);
        }
    }

    private void calculateOptimalPath(MinimapData data) {
        // Find safest path to zone considering obstacles and enemies
        float targetX = data.currentZone.center[0];
        float targetY = data.currentZone.center[1];

        // Simple direct path for now (could be enhanced with pathfinding)
        float dx = targetX - data.playerPosition.coordinates[0];
        float dy = targetY - data.playerPosition.coordinates[1];

        data.playerPosition.facing = (float) Math.toDegrees(Math.atan2(dy, dx));
    }

    public float calculateZoneRotationUrgency(MinimapData data) {
        if (data.currentZone == null || data.playerPosition == null) {
            return 0f;
        }

        if (data.playerPosition.isInSafeZone) {
            return 0.1f; // Low urgency if already in zone
        }

        // Calculate urgency based on distance and time
        float distanceUrgency = Math.min(1.0f, data.playerPosition.distanceToZone * 2f);
        float timeUrgency = Math.min(1.0f, 60f / data.currentZone.timeRemaining);

        return Math.max(distanceUrgency, timeUrgency);
    }

    public List<MarkerInfo> getNearbyMarkers(MinimapData data, float radius) {
        List<MarkerInfo> nearby = new ArrayList<>();

        if (data.playerPosition == null) return nearby;

        for (MarkerInfo marker : data.markers) {
            float dx = marker.position[0] - data.playerPosition.coordinates[0];
            float dy = marker.position[1] - data.playerPosition.coordinates[1];
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance <= radius) {
                nearby.add(marker);
            }
        }

        return nearby;
    }

}
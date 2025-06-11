package com.gestureai.gameautomation.models;

import android.graphics.Point;
import java.util.ArrayList;
import java.util.List;

public class HandLandmarks {
    private final List<Landmark> landmarks;
    private final Point centerPoint;

    public HandLandmarks(List<Landmark> landmarks) {
        this.landmarks = landmarks != null ? landmarks : new ArrayList<>();
        this.centerPoint = calculateCenterPoint();
    }

    public List<Landmark> getLandmarks() {
        return landmarks;
    }

    public Point getCenterPoint() {
        return centerPoint;
    }

    private Point calculateCenterPoint() {
        if (landmarks.isEmpty()) {
            return new Point(0, 0);
        }

        float sumX = 0, sumY = 0;
        for (Landmark landmark : landmarks) {
            sumX += landmark.x;
            sumY += landmark.y;
        }

        return new Point((int)(sumX / landmarks.size()), (int)(sumY / landmarks.size()));
    }

    public static class Landmark {
        public final float x;
        public final float y;
        public final float z;
        public final float visibility;

        public Landmark(float x, float y, float z, float visibility) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.visibility = visibility;
        }

        public Landmark(float x, float y, float z) {
            this(x, y, z, 1.0f);
        }
    }
}
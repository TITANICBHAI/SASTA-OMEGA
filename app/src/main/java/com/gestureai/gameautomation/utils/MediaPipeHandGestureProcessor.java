package com.gestureai.gameautomation.utils;

import static com.gestureai.gameautomation.utils.OpenCVHelper.isInitialized;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import java.io.IOException;
import java.util.List;


@androidx.camera.core.ExperimentalGetImage
public class MediaPipeHandGestureProcessor {
    private static final String TAG = "MediaPipeHandGesture";

    private HandLandmarker handLandmarker;
    private Context context;
    private boolean isInitialized = false;

    public interface GestureCallback {
        void onGestureDetected(String gestureType, float confidence);
        void onHandLandmarksDetected(List<NormalizedLandmark> landmarks);
    }

    private GestureCallback gestureCallback;

    public MediaPipeHandGestureProcessor(Context context) {
        this.context = context;
        initializeHandLandmarker();
    }

    private void initializeHandLandmarker() {
        try {
            BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath("mediapipe/hand_landmarker.task")
                .build();

            HandLandmarkerOptions options = HandLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setNumHands(2)
                    .setMinHandDetectionConfidence(0.5f)
                    .setMinHandPresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .setRunningMode(com.google.mediapipe.tasks.vision.core.RunningMode.LIVE_STREAM)
                    .setResultListener(this::processHandLandmarkerResult)
                    .build();

            handLandmarker = HandLandmarker.createFromOptions(context, options);
            isInitialized = true;
            Log.d(TAG, "MediaPipe Hand Landmarker initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize MediaPipe Hand Landmarker", e);
            isInitialized = false;
        }
    }

    public void setGestureCallback(GestureCallback callback) {
        this.gestureCallback = callback;
    }

    public void processFrame(ImageProxy imageProxy) {
        if (!isInitialized || handLandmarker == null) {
            return;
        }

        try {
            // Convert ImageProxy to Bitmap
            Bitmap bitmap = ImageUtils.imageProxyToBitmap(imageProxy);
            if (bitmap == null) {
                return;
            }

            // Create MPImage from bitmap
            MPImage mpImage = new BitmapImageBuilder(bitmap).build();

            // Process with MediaPipe
            long timestampMs = System.currentTimeMillis();
            handLandmarker.detectAsync(mpImage, timestampMs);

        } catch (Exception e) {
            Log.e(TAG, "Error processing frame with MediaPipe", e);
        }
    }

    private void processHandLandmarkerResult(HandLandmarkerResult result, MPImage input) {
        if (result.landmarks().isEmpty()) {
            return;
        }

        // Process hand landmarks and detect gestures
        for (int i = 0; i < result.landmarks().size(); i++) {
            List<NormalizedLandmark> landmarks = result.landmarks().get(i);

            // Analyze hand pose for gesture recognition
            String gestureType = analyzeHandGesture(landmarks);
            float confidence = calculateGestureConfidence(landmarks, gestureType);

            if (gestureCallback != null) {
                gestureCallback.onHandLandmarksDetected(landmarks);
                if (gestureType != null && confidence > 0.7f) {
                    gestureCallback.onGestureDetected(gestureType, confidence);
                }
            }
        }
    }

    private String analyzeHandGesture(List<NormalizedLandmark> landmarks) {
        if (landmarks.size() < 21) {
            return null;
        }

        // Get key landmarks for gesture analysis
        NormalizedLandmark wrist = landmarks.get(0);
        NormalizedLandmark thumbTip = landmarks.get(4);
        NormalizedLandmark indexTip = landmarks.get(8);
        NormalizedLandmark middleTip = landmarks.get(12);
        NormalizedLandmark ringTip = landmarks.get(16);
        NormalizedLandmark pinkyTip = landmarks.get(20);

        NormalizedLandmark indexMcp = landmarks.get(5);
        NormalizedLandmark middleMcp = landmarks.get(9);
        NormalizedLandmark ringMcp = landmarks.get(13);
        NormalizedLandmark pinkyMcp = landmarks.get(17);

        // Analyze finger positions for common gestures
        boolean isThumbUp = thumbTip.y() < wrist.y();
        boolean isIndexUp = indexTip.y() < indexMcp.y();
        boolean isMiddleUp = middleTip.y() < middleMcp.y();
        boolean isRingUp = ringTip.y() < ringMcp.y();
        boolean isPinkyUp = pinkyTip.y() < pinkyMcp.y();

        // Count extended fingers
        int extendedFingers = 0;
        if (isThumbUp) extendedFingers++;
        if (isIndexUp) extendedFingers++;
        if (isMiddleUp) extendedFingers++;
        if (isRingUp) extendedFingers++;
        if (isPinkyUp) extendedFingers++;

        // Gesture classification
        if (extendedFingers == 0) {
            return "fist";
        } else if (extendedFingers == 5) {
            return "open_hand";
        } else if (extendedFingers == 1 && isIndexUp) {
            return "point";
        } else if (extendedFingers == 2 && isIndexUp && isMiddleUp) {
            return "peace";
        } else if (extendedFingers == 1 && isThumbUp) {
            return "thumbs_up";
        }

        // Detect swipe gestures based on hand movement
        return detectSwipeGesture(landmarks);
    }

    private String detectSwipeGesture(List<NormalizedLandmark> landmarks) {
        NormalizedLandmark wrist = landmarks.get(0);
        NormalizedLandmark indexTip = landmarks.get(8);

        float deltaX = indexTip.x() - wrist.x();
        float deltaY = indexTip.y() - wrist.y();

        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            if (deltaX > 0.1f) {
                return "swipe_right";
            } else if (deltaX < -0.1f) {
                return "swipe_left";
            }
        } else {
            if (deltaY > 0.1f) {
                return "swipe_down";
            } else if (deltaY < -0.1f) {
                return "swipe_up";
            }
        }
        return null;
    }

    private float calculateGestureConfidence(List<NormalizedLandmark> landmarks, String gestureType) {
        if (gestureType == null || landmarks.isEmpty()) {
            return 0.0f;
        }

        // Calculate confidence based on hand landmark visibility and consistency
        float totalVisibility = 0.0f;
        for (NormalizedLandmark landmark : landmarks) {
            totalVisibility += landmark.visibility().orElse(0.0f);
        }  // Add this closing brace


        float averageVisibility = totalVisibility / landmarks.size();

        // Adjust confidence based on gesture type stability
        float gestureConfidence = 0.8f; // Base confidence

        switch (gestureType) {
            case "fist":
            case "open_hand":
                gestureConfidence = 0.9f;
                break;
            case "point":
            case "peace":
            case "thumbs_up":
                gestureConfidence = 0.85f;
                break;
            case "swipe_left":
            case "swipe_right":
            case "swipe_up":
            case "swipe_down":
                gestureConfidence = 0.75f;
                break;
        }

        return gestureConfidence * averageVisibility;
    }

    public void release() {
            if (handLandmarker != null) {
                handLandmarker.close();
                handLandmarker = null;
            }
                isInitialized = false;
                Log.d(TAG, "MediaPipe Hand Landmarker released");
            }

            public boolean isInitialized () {
                return isInitialized;
            }
        }
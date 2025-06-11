package com.gestureai.gameautomation.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.google.mediapipe.framework.MediaPipeException;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.framework.PacketGetter;
import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.components.PermissionHelper;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.glutil.EglManager;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * MediaPipe Manager - Hand tracking and gesture recognition using MediaPipe
 */
public class MediaPipeManager {
    private static final String TAG = "MediaPipeManager";
    private static MediaPipeManager instance;
    
    // MediaPipe configuration
    private static final String BINARY_GRAPH_NAME = "hand_tracking_mobile_gpu.binarypb";
    private static final String INPUT_VIDEO_STREAM_NAME = "input_video";
    private static final String OUTPUT_VIDEO_STREAM_NAME = "output_video";
    private static final String OUTPUT_LANDMARKS_STREAM_NAME = "hand_landmarks";
    
    private Context context;
    private FrameProcessor processor;
    private EglManager eglManager;
    private volatile boolean isInitialized = false;
    private volatile boolean handDetectionEnabled = true;
    private GestureRecognitionListener gestureListener;
    
    // GPU context switching and pipeline reconfiguration fixes
    private final Object gpuContextLock = new Object();
    private volatile boolean pipelineReconfiguring = false;
    private volatile boolean gpuContextActive = false;
    private volatile long lastPipelineConfig = 0;
    private static final long PIPELINE_COOLDOWN_MS = 1000;
    private final java.util.concurrent.atomic.AtomicBoolean resourceCleanupInProgress = new java.util.concurrent.atomic.AtomicBoolean(false);
    
    // Hand landmark data
    private List<HandLandmark> currentLandmarks;
    private Map<String, GesturePattern> gesturePatterns;
    
    // Gesture recognition interface
    public interface GestureRecognitionListener {
        void onHandDetected(List<HandLandmark> landmarks);
        void onGestureRecognized(String gestureName, float confidence);
        void onHandLost();
        void onError(String error);
    }
    
    // Hand landmark structure
    public static class HandLandmark {
        public float x, y, z;
        public int landmarkIndex;
        public float visibility;
        
        public HandLandmark(int index, float x, float y, float z, float visibility) {
            this.landmarkIndex = index;
            this.x = x;
            this.y = y;
            this.z = z;
            this.visibility = visibility;
        }
    }
    
    // Gesture pattern for recognition
    public static class GesturePattern {
        public String name;
        public Map<Integer, float[]> keyLandmarks; // landmark index -> [x, y, z] relative positions
        public float confidenceThreshold;
        
        public GesturePattern(String name, float confidenceThreshold) {
            this.name = name;
            this.confidenceThreshold = confidenceThreshold;
            this.keyLandmarks = new HashMap<>();
        }
    }
    
    private MediaPipeManager(Context context) {
        this.context = context.getApplicationContext();
        this.currentLandmarks = new ArrayList<>();
        this.gesturePatterns = new HashMap<>();
        setupGesturePatterns();
    }
    
    public static synchronized MediaPipeManager getInstance(Context context) {
        if (instance == null) {
            instance = new MediaPipeManager(context);
        }
        return instance;
    }
    
    public void initialize() {
        synchronized (gpuContextLock) {
            if (isInitialized) {
                return;
            }
            
            if (pipelineReconfiguring) {
                Log.w(TAG, "Pipeline reconfiguration in progress, delaying initialization");
                return;
            }
            
            try {
                pipelineReconfiguring = true;
                
                // Check cooldown period to prevent rapid reconfigurations
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastPipelineConfig < PIPELINE_COOLDOWN_MS) {
                    Log.w(TAG, "Pipeline cooldown active, delaying initialization");
                    return;
                }
                
                // Initialize MediaPipe Android Asset Util
                AndroidAssetUtil.initializeNativeAssetManager(context);
                
                // Initialize EGL context with proper cleanup
                initializeGPUContext();
                
                // Setup MediaPipe processor
                initializeMediaPipeProcessor();
                
                isInitialized = true;
                lastPipelineConfig = currentTime;
                Log.d(TAG, "MediaPipe initialized successfully with GPU context management");
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize MediaPipe", e);
                cleanupResources();
                throw new RuntimeException("MediaPipe initialization failed", e);
            } finally {
                pipelineReconfiguring = false;
            }
        }
    }
    
    private void initializeGPUContext() throws Exception {
        try {
            if (eglManager != null && gpuContextActive) {
                Log.d(TAG, "GPU context already active, reusing existing context");
                return;
            }
            
            // Clean up any existing context first
            if (eglManager != null) {
                cleanupGPUContext();
            }
            
            eglManager = new EglManager(null);
            gpuContextActive = true;
            Log.d(TAG, "GPU context initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize GPU context", e);
            gpuContextActive = false;
            throw e;
        }
    }
    
    private void cleanupGPUContext() {
        try {
            if (eglManager != null && gpuContextActive) {
                eglManager.release();
                eglManager = null;
                gpuContextActive = false;
                Log.d(TAG, "GPU context cleaned up");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up GPU context", e);
        }
    }
    
    private void initializeMediaPipeProcessor() throws Exception {
        try {
            // Create processor with error handling
            processor = new FrameProcessor(
                context,
                eglManager.getNativeContext(),
                BINARY_GRAPH_NAME,
                INPUT_VIDEO_STREAM_NAME,
                OUTPUT_VIDEO_STREAM_NAME
            );
            
            // Add packet callback for landmarks
            processor.addPacketCallback(
                OUTPUT_LANDMARKS_STREAM_NAME,
                (packet) -> processLandmarkPacket(packet)
            );
            
            Log.d(TAG, "MediaPipe processor initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize MediaPipe processor", e);
            throw e;
        }
    }
            
            // Initialize EGL context with proper context sharing
            eglManager = new EglManager(null);
            
            // Verify EGL context is valid before proceeding
            if (eglManager.getNativeContext() == 0) {
                throw new MediaPipeException("Failed to create EGL context");
            }
            
            // Set up the processor with error handling
            processor = new FrameProcessor(
                context,
                eglManager.getNativeContext(),
                BINARY_GRAPH_NAME,
                INPUT_VIDEO_STREAM_NAME,
                OUTPUT_VIDEO_STREAM_NAME
            );
            
            // Add packet callback for hand landmarks with error handling
            processor.addPacketCallback(OUTPUT_LANDMARKS_STREAM_NAME, this::processHandLandmarks);
            
            isInitialized = true;
            Log.d(TAG, "MediaPipe Manager initialized successfully");
            
        } catch (MediaPipeException e) {
            Log.e(TAG, "MediaPipe initialization failed", e);
            cleanup();
            isInitialized = false;
            
            // Fallback to CPU-only mode
            initializeCPUFallback();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during MediaPipe initialization", e);
            cleanup();
            isInitialized = false;
        }
    }
    
    private void initializeCPUFallback() {
        try {
            Log.i(TAG, "Initializing MediaPipe in CPU-only mode");
            
            // Use CPU-only binary graph
            processor = new FrameProcessor(
                context,
                0, // No EGL context for CPU mode
                "hand_tracking_mobile_cpu.binarypb",
                INPUT_VIDEO_STREAM_NAME,
                OUTPUT_VIDEO_STREAM_NAME
            );
            
            processor.addPacketCallback(OUTPUT_LANDMARKS_STREAM_NAME, this::processHandLandmarks);
            isInitialized = true;
            Log.d(TAG, "MediaPipe CPU fallback initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "CPU fallback initialization failed", e);
            isInitialized = false;
        }
    }
    
    public void cleanup() {
        try {
            if (processor != null) {
                processor.close();
                processor = null;
            }
            
            if (eglManager != null) {
                eglManager.release();
                eglManager = null;
            }
            
            currentLandmarks.clear();
            isInitialized = false;
            Log.d(TAG, "MediaPipe Manager cleaned up");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during MediaPipe cleanup", e);
        }
    }
    
    private void setupGesturePatterns() {
        // Define basic gesture patterns
        
        // Pointing gesture (index finger extended)
        GesturePattern pointing = new GesturePattern("pointing", 0.8f);
        pointing.keyLandmarks.put(8, new float[]{0.0f, -0.3f, 0.0f}); // Index tip
        pointing.keyLandmarks.put(6, new float[]{0.0f, -0.1f, 0.0f}); // Index middle
        gesturePatterns.put("pointing", pointing);
        
        // Peace sign (index and middle finger extended)
        GesturePattern peace = new GesturePattern("peace", 0.7f);
        peace.keyLandmarks.put(8, new float[]{-0.1f, -0.3f, 0.0f}); // Index tip
        peace.keyLandmarks.put(12, new float[]{0.1f, -0.3f, 0.0f}); // Middle tip
        gesturePatterns.put("peace", peace);
        
        // Fist (all fingers closed)
        GesturePattern fist = new GesturePattern("fist", 0.8f);
        fist.keyLandmarks.put(8, new float[]{0.0f, 0.1f, 0.0f}); // Index tip below base
        fist.keyLandmarks.put(12, new float[]{0.0f, 0.1f, 0.0f}); // Middle tip below base
        gesturePatterns.put("fist", fist);
        
        // Open palm (all fingers extended)
        GesturePattern openPalm = new GesturePattern("open_palm", 0.7f);
        openPalm.keyLandmarks.put(8, new float[]{0.0f, -0.25f, 0.0f}); // Index tip
        openPalm.keyLandmarks.put(12, new float[]{0.0f, -0.3f, 0.0f}); // Middle tip
        openPalm.keyLandmarks.put(16, new float[]{0.0f, -0.25f, 0.0f}); // Ring tip
        openPalm.keyLandmarks.put(20, new float[]{0.0f, -0.2f, 0.0f}); // Pinky tip
        gesturePatterns.put("open_palm", openPalm);
        
        // Thumbs up
        GesturePattern thumbsUp = new GesturePattern("thumbs_up", 0.8f);
        thumbsUp.keyLandmarks.put(4, new float[]{0.0f, -0.3f, 0.0f}); // Thumb tip
        thumbsUp.keyLandmarks.put(8, new float[]{0.0f, 0.1f, 0.0f}); // Index folded
        gesturePatterns.put("thumbs_up", thumbsUp);
        
        Log.d(TAG, "Gesture patterns initialized: " + gesturePatterns.size() + " patterns");
    }
    
    public void setGestureRecognitionListener(GestureRecognitionListener listener) {
        this.gestureListener = listener;
    }
    
    public void enableHandDetection() {
        handDetectionEnabled = true;
        Log.d(TAG, "Hand detection enabled");
    }
    
    public void disableHandDetection() {
        handDetectionEnabled = false;
        Log.d(TAG, "Hand detection disabled");
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }
    
    public void processFrame(Bitmap inputBitmap) {
        if (!isInitialized || !handDetectionEnabled || processor == null) {
            return;
        }
        
        try {
            // Convert bitmap to MediaPipe format and process
            processor.onNewFrame(inputBitmap, System.currentTimeMillis() * 1000);
        } catch (Exception e) {
            Log.e(TAG, "Error processing frame", e);
            if (gestureListener != null) {
                gestureListener.onError("Frame processing error: " + e.getMessage());
            }
        }
    }
    
    private void processHandLandmarks(Packet packet) {
        try {
            // Extract hand landmarks from packet
            // This is a simplified version - actual implementation would use MediaPipe's
            // landmark parsing utilities
            
            if (!packet.isEmpty()) {
                // Parse landmarks (simplified)
                currentLandmarks.clear();
                
                // In real implementation, you would extract the actual landmark data
                // For now, we'll simulate with some basic landmarks
                for (int i = 0; i < 21; i++) { // 21 hand landmarks
                    currentLandmarks.add(new HandLandmark(i, 
                        (float) Math.random(), 
                        (float) Math.random(), 
                        (float) Math.random(), 
                        0.9f));
                }
                
                if (gestureListener != null) {
                    gestureListener.onHandDetected(currentLandmarks);
                }
                
                // Perform gesture recognition
                recognizeGesture(currentLandmarks);
                
            } else {
                // No hand detected
                currentLandmarks.clear();
                if (gestureListener != null) {
                    gestureListener.onHandLost();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing hand landmarks", e);
            if (gestureListener != null) {
                gestureListener.onError("Landmark processing error: " + e.getMessage());
            }
        }
    }
    
    private void recognizeGesture(List<HandLandmark> landmarks) {
        if (landmarks.size() < 21) {
            return; // Need all 21 landmarks for gesture recognition
        }
        
        float bestConfidence = 0.0f;
        String bestGesture = null;
        
        // Test each gesture pattern
        for (Map.Entry<String, GesturePattern> entry : gesturePatterns.entrySet()) {
            GesturePattern pattern = entry.getValue();
            float confidence = calculateGestureConfidence(landmarks, pattern);
            
            if (confidence > pattern.confidenceThreshold && confidence > bestConfidence) {
                bestConfidence = confidence;
                bestGesture = pattern.name;
            }
        }
        
        if (bestGesture != null && gestureListener != null) {
            gestureListener.onGestureRecognized(bestGesture, bestConfidence);
            Log.d(TAG, "Gesture recognized: " + bestGesture + " (confidence: " + bestConfidence + ")");
        }
    }
    
    private float calculateGestureConfidence(List<HandLandmark> landmarks, GesturePattern pattern) {
        if (landmarks.size() < 21) {
            return 0.0f;
        }
        
        float totalScore = 0.0f;
        int validComparisons = 0;
        
        // Get wrist position as reference point (landmark 0)
        HandLandmark wrist = landmarks.get(0);
        
        for (Map.Entry<Integer, float[]> entry : pattern.keyLandmarks.entrySet()) {
            int landmarkIndex = entry.getKey();
            float[] expectedRelativePos = entry.getValue();
            
            if (landmarkIndex < landmarks.size()) {
                HandLandmark landmark = landmarks.get(landmarkIndex);
                
                // Calculate relative position from wrist
                float relativeX = landmark.x - wrist.x;
                float relativeY = landmark.y - wrist.y;
                float relativeZ = landmark.z - wrist.z;
                
                // Calculate similarity to expected pattern
                float distanceX = Math.abs(relativeX - expectedRelativePos[0]);
                float distanceY = Math.abs(relativeY - expectedRelativePos[1]);
                float distanceZ = Math.abs(relativeZ - expectedRelativePos[2]);
                
                float distance = (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ);
                float similarity = Math.max(0.0f, 1.0f - distance); // Convert distance to similarity
                
                totalScore += similarity;
                validComparisons++;
            }
        }
        
        return validComparisons > 0 ? totalScore / validComparisons : 0.0f;
    }
    
    public void startGestureTest() {
        if (gestureListener != null) {
            // Simulate gesture detection for testing
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    
                    // Simulate pointing gesture
                    List<HandLandmark> testLandmarks = generateTestLandmarks("pointing");
                    gestureListener.onHandDetected(testLandmarks);
                    gestureListener.onGestureRecognized("pointing", 0.95f);
                    
                    Thread.sleep(2000);
                    
                    // Simulate peace gesture
                    testLandmarks = generateTestLandmarks("peace");
                    gestureListener.onHandDetected(testLandmarks);
                    gestureListener.onGestureRecognized("peace", 0.87f);
                    
                    Thread.sleep(2000);
                    
                    // Simulate hand lost
                    gestureListener.onHandLost();
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
    
    private List<HandLandmark> generateTestLandmarks(String gestureType) {
        List<HandLandmark> landmarks = new ArrayList<>();
        
        // Generate 21 hand landmarks based on gesture type
        for (int i = 0; i < 21; i++) {
            float x = 0.5f + (float) (Math.random() - 0.5) * 0.2f;
            float y = 0.5f + (float) (Math.random() - 0.5) * 0.2f;
            float z = 0.0f + (float) (Math.random() - 0.5) * 0.1f;
            
            landmarks.add(new HandLandmark(i, x, y, z, 0.9f));
        }
        
        return landmarks;
    }
    
    public void addCustomGesture(String name, Map<Integer, float[]> keyLandmarks, float threshold) {
        GesturePattern customPattern = new GesturePattern(name, threshold);
        customPattern.keyLandmarks.putAll(keyLandmarks);
        gesturePatterns.put(name, customPattern);
        Log.d(TAG, "Custom gesture added: " + name);
    }
    
    public void removeGesture(String name) {
        gesturePatterns.remove(name);
        Log.d(TAG, "Gesture removed: " + name);
    }
    
    public List<String> getAvailableGestures() {
        return new ArrayList<>(gesturePatterns.keySet());
    }
    
    public void cleanup() {
        if (processor != null) {
            processor.close();
        }
        if (eglManager != null) {
            eglManager.release();
        }
        currentLandmarks.clear();
        isInitialized = false;
        Log.d(TAG, "MediaPipe Manager cleaned up");
    }
}
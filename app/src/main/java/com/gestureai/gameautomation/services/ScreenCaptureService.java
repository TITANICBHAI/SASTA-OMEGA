package com.gestureai.gameautomation.services;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.gestureai.gameautomation.MainActivity;
import com.gestureai.gameautomation.R;
import java.nio.ByteBuffer;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.ai.PatternLearningEngine;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.api.ndarray.INDArray;

public class ScreenCaptureService extends Service {
    private static final String TAG = "ScreenCaptureService";
    private static volatile ScreenCaptureService instance;
    private static final Object instanceLock = new Object();
    private static final String CHANNEL_ID = "ScreenCaptureChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    private GameStrategyAgent aiProcessor;
    private PatternLearningEngine patternLearner;
    private boolean nd4jProcessingEnabled = true;
    private boolean aiProcessingEnabled = true;
    private static final int CAPTURE_INTERVAL = 100; // 10 FPS
    
    // Real-time learning components
    private com.gestureai.gameautomation.ai.DQNAgent dqnAgent;
    private com.gestureai.gameautomation.ai.PPOAgent ppoAgent;
    private com.gestureai.gameautomation.ObjectDetectionEngine objectDetector;
    private Bitmap previousFrame;
    private long lastActionTime = 0;
    private int previousScore = 0;
    private float[] previousGameState;
    private boolean learningEnabled = true;
    
    public interface ScreenCaptureCallback {
        void onScreenCaptured(Bitmap bitmap);
        void onCaptureError(String error);
    }
    
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler captureHandler;
    private ScreenCaptureCallback callback;
    private boolean isCapturing = false;
    
    // MediaProjection lifecycle management with strict validation
    private volatile boolean projectionActive = false;
    private volatile long projectionStartTime = 0;
    private static final long PROJECTION_TIMEOUT_MS = 300000; // 5 minutes
    private final Object projectionLock = new Object();
    private volatile boolean serviceDestroyed = false;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable projectionTimeoutRunnable;
    
    // Frame buffer management with memory limits
    private static final int MAX_FRAME_BUFFER_SIZE = 10;
    private final java.util.concurrent.ConcurrentLinkedQueue<Bitmap> frameBuffer = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final java.util.concurrent.atomic.AtomicInteger frameBufferSize = new java.util.concurrent.atomic.AtomicInteger(0);
    private volatile boolean memoryPressureDetected = false;
    
    // Screen dimensions
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;
    
    // Thread-safe singleton methods
    public static ScreenCaptureService getInstance() {
        return instance;
    }
    
    public static ScreenCaptureService getInstanceSafe() {
        synchronized (instanceLock) {
            if (instance == null) {
                Log.w(TAG, "ScreenCaptureService instance not yet created");
                return null;
            }
            return instance;
        }
    }
    
    public static boolean isInstanceAvailable() {
        synchronized (instanceLock) {
            return instance != null;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (instanceLock) {
            instance = this;
        }
        createNotificationChannel();
        
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        captureHandler = new Handler(Looper.getMainLooper());
        
        // Initialize AI processing with error handling
        try {
            aiProcessor = new GameStrategyAgent(getApplicationContext());
            patternLearner = new PatternLearningEngine(getApplicationContext());
            
            // Initialize RL agents for real-time learning
            dqnAgent = new com.gestureai.gameautomation.ai.DQNAgent(getApplicationContext(), 128, 8);
            ppoAgent = new com.gestureai.gameautomation.ai.PPOAgent(getApplicationContext(), 128, 8);
            objectDetector = new com.gestureai.gameautomation.ObjectDetectionEngine(getApplicationContext());
            
            Log.d(TAG, "Real-time learning components initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize AI components: " + e.getMessage());
        }
        
        initializeScreenMetrics();
        Log.d(TAG, "Screen Capture Service created");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Captures screen for game automation");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start as foreground service to prevent Android 8+ crashes
        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY; // Restart if killed
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Game Automation Active")
            .setContentText("Screen capture running")
            .setSmallIcon(R.drawable.ic_gesture)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }
    
    private void initializeScreenMetrics() {
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;
        
        Log.d(TAG, "Screen metrics: " + screenWidth + "x" + screenHeight + " @ " + screenDensity + "dpi");
    }
    
    public void setCallback(ScreenCaptureCallback callback) {
        this.callback = callback;
    }
    
    public boolean startCapture(Intent mediaProjectionData) {
        synchronized (projectionLock) {
            if (serviceDestroyed) {
                Log.w(TAG, "Service destroyed, cannot start capture");
                return false;
            }
            
            if (isCapturing) {
                Log.w(TAG, "Screen capture already in progress");
                return true;
            }
            
            try {
                // Validate projection timeout
                if (projectionActive && (System.currentTimeMillis() - projectionStartTime) > PROJECTION_TIMEOUT_MS) {
                    Log.w(TAG, "Previous projection timed out, cleaning up");
                    stopCaptureInternal();
                }
                
                // Create MediaProjection with lifecycle validation
                mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, mediaProjectionData);
                
                if (mediaProjection == null) {
                    Log.e(TAG, "Failed to create MediaProjection");
                    return false;
                }
                
                // Set up projection timeout monitoring
                projectionStartTime = System.currentTimeMillis();
                projectionActive = true;
                
                projectionTimeoutRunnable = () -> {
                    Log.w(TAG, "MediaProjection timeout reached, stopping capture");
                    stopCapture();
                };
                timeoutHandler.postDelayed(projectionTimeoutRunnable, PROJECTION_TIMEOUT_MS);
                
                // Add callback for projection termination
                mediaProjection.registerCallback(new MediaProjection.Callback() {
                    @Override
                    public void onStop() {
                        Log.d(TAG, "MediaProjection stopped by system");
                        stopCaptureInternal();
                    }
                }, null);
                
                setupImageReader();
                createVirtualDisplay();
                isCapturing = true;
                
                Log.d(TAG, "Screen capture started successfully with lifecycle management");
                return true;
                
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception starting screen capture", e);
                projectionActive = false;
                return false;
            } catch (Exception e) {
                Log.e(TAG, "Error starting screen capture", e);
                projectionActive = false;
                return false;
            }
        }
    }
    
    private void processScreenWithRealTimeLearning(Bitmap bitmap) {
        if (!learningEnabled || bitmap == null) return;
        
        try {
            // Real-time AI processing
            if (aiProcessingEnabled && aiProcessor != null) {
                // Process with GameStrategyAgent
                aiProcessor.processGameState(bitmap);
            }
            
            // DQN learning
            if (dqnAgent != null && previousFrame != null) {
                float[] currentState = extractGameState(bitmap);
                if (previousGameState != null) {
                    float reward = calculateReward(bitmap, previousFrame);
                    dqnAgent.learn(previousGameState, 0, reward, currentState, false);
                }
                previousGameState = currentState;
            }
            
            // PPO learning
            if (ppoAgent != null && previousFrame != null) {
                float[] gameState = extractGameState(bitmap);
                ppoAgent.processExperience(gameState);
            }
            
            // Object detection learning
            if (objectDetector != null) {
                objectDetector.detectObjects(bitmap);
            }
            
            previousFrame = bitmap.copy(bitmap.getConfig(), false);
        } catch (Exception e) {
            Log.e(TAG, "Error in real-time learning", e);
        }
    }
    
    private void processScreenWithAI(Bitmap bitmap) {
        if (!aiProcessingEnabled || bitmap == null) return;
        
        try {
            // ND4J processing if enabled
            if (nd4jProcessingEnabled) {
                INDArray imageArray = bitmapToINDArray(bitmap);
                if (imageArray != null) {
                    // Process with neural networks
                    patternLearner.analyzePattern(imageArray);
                }
            }
            
            // Pattern learning
            if (patternLearner != null) {
                patternLearner.processFrame(bitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in AI processing", e);
        }
    }
    
    private float[] extractGameState(Bitmap bitmap) {
        // Extract 128-dimensional game state from bitmap
        float[] state = new float[128];
        try {
            // Simple feature extraction - convert bitmap to normalized features
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            // Sample pixels at regular intervals
            int sampleStep = Math.max(1, width / 16); // 16x8 grid = 128 features
            int index = 0;
            
            for (int y = 0; y < height && index < 128; y += height / 8) {
                for (int x = 0; x < width && index < 128; x += sampleStep) {
                    int pixel = bitmap.getPixel(x, y);
                    // Convert to grayscale and normalize
                    int gray = (((pixel >> 16) & 0xFF) + ((pixel >> 8) & 0xFF) + (pixel & 0xFF)) / 3;
                    state[index++] = gray / 255.0f;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting game state", e);
        }
        return state;
    }
    
    private float calculateReward(Bitmap current, Bitmap previous) {
        // Simple reward calculation based on visual changes
        try {
            // Compare histograms or key regions
            return 0.1f; // Placeholder - implement game-specific reward logic
        } catch (Exception e) {
            Log.e(TAG, "Error calculating reward", e);
            return 0.0f;
        }
    }
    
    private INDArray bitmapToINDArray(Bitmap bitmap) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float[][][] imageArray = new float[1][height][width];
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = bitmap.getPixel(x, y);
                    int gray = (((pixel >> 16) & 0xFF) + ((pixel >> 8) & 0xFF) + (pixel & 0xFF)) / 3;
                    imageArray[0][y][x] = gray / 255.0f;
                }
            }
            
            return Nd4j.create(imageArray);
        } catch (Exception e) {
            Log.e(TAG, "Error converting bitmap to INDArray", e);
            return null;
        }
    }
    
    // Critical missing method that UnifiedServiceCoordinator expects
    public Bitmap captureScreen() {
        if (!isCapturing || imageReader == null) {
            Log.w(TAG, "Screen capture not active");
            return null;
        }
        
        try {
            Image image = imageReader.acquireLatestImage();
            if (image != null) {
                Bitmap bitmap = imageToBitmap(image);
                image.close();
                
                // Process with AI if enabled
                if (aiProcessingEnabled && bitmap != null) {
                    processScreenWithAI(bitmap);
                }
                
                return bitmap;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error capturing screen", e);
        }
        
        return null;
    }
    
    public void stopCapture() {
        synchronized (projectionLock) {
            if (!isCapturing && !projectionActive) {
                Log.w(TAG, "Screen capture not active");
                return;
            }
            
            stopScreenCaptureInternal();
        }
    }
    
    private void stopScreenCaptureInternal() {
        try {
            isCapturing = false;
            projectionActive = false;
            
            // Clear frame buffer to prevent memory leaks
            clearFrameBuffer();
            
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
            
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
            
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
            
            Log.d(TAG, "Screen capture stopped successfully with cleanup");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping screen capture", e);
        }
    }
    
    private void clearFrameBuffer() {
        try {
            while (!frameBuffer.isEmpty()) {
                Bitmap frame = frameBuffer.poll();
                if (frame != null && !frame.isRecycled()) {
                    frame.recycle();
                }
            }
            frameBufferSize.set(0);
            memoryPressureDetected = false;
            Log.d(TAG, "Frame buffer cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing frame buffer", e);
        }
    }
    
    private void addToFrameBuffer(Bitmap bitmap) {
        if (bitmap == null) return;
        
        try {
            // Check memory pressure
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsage = (double) usedMemory / maxMemory;
            
            if (memoryUsage > 0.8) {
                memoryPressureDetected = true;
                // Drop older frames under memory pressure
                while (frameBufferSize.get() > MAX_FRAME_BUFFER_SIZE / 2) {
                    Bitmap oldFrame = frameBuffer.poll();
                    if (oldFrame != null && !oldFrame.isRecycled()) {
                        oldFrame.recycle();
                    }
                    frameBufferSize.decrementAndGet();
                }
                Log.w(TAG, "Memory pressure detected, dropping frames");
            }
            
            // Add new frame if buffer not full
            if (frameBufferSize.get() < MAX_FRAME_BUFFER_SIZE) {
                frameBuffer.offer(bitmap.copy(bitmap.getConfig(), false));
                frameBufferSize.incrementAndGet();
            } else {
                // Remove oldest frame and add new one
                Bitmap oldFrame = frameBuffer.poll();
                if (oldFrame != null && !oldFrame.isRecycled()) {
                    oldFrame.recycle();
                }
                frameBuffer.offer(bitmap.copy(bitmap.getConfig(), false));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error managing frame buffer", e);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCapture();
        
        // Cleanup MediaProjection resources
        try {
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up MediaProjection resources", e);
        }
        
        // Cleanup AI components
        try {
            if (aiProcessor != null) {
                aiProcessor.cleanup();
            }
            if (patternLearner != null) {
                patternLearner.cleanup();
            }
            if (dqnAgent != null) {
                dqnAgent.cleanup();
            }
            if (ppoAgent != null) {
                ppoAgent.cleanup();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up AI components", e);
        }
        
        instance = null;
        Log.d(TAG, "ScreenCaptureService destroyed");
    }
    
    @Nullable
    // Service Binder for client communication
    public class ScreenCaptureBinder extends android.os.Binder {
        public ScreenCaptureService getService() {
            return ScreenCaptureService.this;
        }
        
        public boolean startCapture(int resultCode, Intent data, ScreenCaptureCallback callback) {
            return ScreenCaptureService.this.startCapture(resultCode, data, callback);
        }
        
        public void stopCapture() {
            ScreenCaptureService.this.stopCapture();
        }
        
        public boolean isCapturing() {
            return ScreenCaptureService.this.isCapturing;
        }
        
        public Bitmap captureScreen() {
            return ScreenCaptureService.this.captureScreen();
        }
    }
    
    private final IBinder binder = new ScreenCaptureBinder();
    
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service bound to client");
        return binder;
    }
    
    public static ScreenCaptureService getInstance() {
        return instance;
    }
    
    private void setupImageReader() {
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                try {
                    Image image = reader.acquireLatestImage();
                    if (image != null && callback != null) {
                        Bitmap bitmap = imageToBitmap(image);
                        if (bitmap != null) {
                            // Real-time learning from screen capture
                            processScreenWithRealTimeLearning(bitmap);
                            callback.onScreenCaptured(bitmap);
                        }
                        image.close();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing captured image", e);
                    if (callback != null) {
                        callback.onCaptureError(e.getMessage());
                    }
                }
            }
        }, captureHandler);
    }
    
    private void createVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.getSurface(),
            null, null
        );
    }
    
    private Bitmap imageToBitmap(Image image) {
        try {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * screenWidth;
            
            Bitmap bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            );
            bitmap.copyPixelsFromBuffer(buffer);
            
            if (rowPadding == 0) {
                return bitmap;
            } else {
                return Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting image to bitmap", e);
            return null;
        }
    }
    
    private void processScreenWithAI(Bitmap bitmap) {
        if (aiProcessor == null || patternLearner == null) return;
        
        try {
            // Convert bitmap to AI-readable format
            INDArray imageArray = bitmapToNDArray(bitmap);
            
            // Process with strategy agent
            GameStrategyAgent.UniversalGameState gameState = extractGameState(bitmap);
            if (gameState != null) {
                aiProcessor.analyzeGameContext(gameState);
            }
            
            // Learn patterns
            patternLearner.processFrame(bitmap);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in AI processing", e);
        }
    }
    
    private INDArray bitmapToNDArray(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        
        float[] normalizedPixels = new float[pixels.length * 3]; // RGB
        for (int i = 0; i < pixels.length; i++) {
            normalizedPixels[i * 3] = ((pixels[i] >> 16) & 0xFF) / 255.0f; // R
            normalizedPixels[i * 3 + 1] = ((pixels[i] >> 8) & 0xFF) / 255.0f; // G
            normalizedPixels[i * 3 + 2] = (pixels[i] & 0xFF) / 255.0f; // B
        }
        
        return Nd4j.create(normalizedPixels).reshape(1, 3, height, width);
    }
    
    private GameStrategyAgent.UniversalGameState extractGameState(Bitmap bitmap) {
        // Basic game state extraction from screen
        GameStrategyAgent.UniversalGameState state = new GameStrategyAgent.UniversalGameState();
        state.screenWidth = bitmap.getWidth();
        state.screenHeight = bitmap.getHeight();
        state.playerX = state.screenWidth / 2;
        state.playerY = state.screenHeight / 2;
        state.gameType = 0.5f; // Default game type
        return state;
    }
    
    public void stopCapture() {
        isCapturing = false;
        
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        
        Log.d(TAG, "Screen capture stopped");
    }
    
    @Override
    public void onDestroy() {
        stopCapture();
        
        // Cleanup AI components to prevent memory leaks
        if (aiProcessor != null) {
            try {
                aiProcessor = null;
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up AI processor", e);
            }
        }
        
        if (patternLearner != null) {
            try {
                patternLearner = null;
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up pattern learner", e);
            }
        }
        
        if (captureHandler != null) {
            captureHandler.removeCallbacksAndMessages(null);
        }
        
        super.onDestroy();
        Log.d(TAG, "Screen Capture Service destroyed");
    }
    
    private void scheduleContinuousCapture() {
        if (isCapturing && captureHandler != null) {
            captureHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isCapturing) {
                        scheduleContinuousCapture();
                    }
                }
            }, 16); // ~60 FPS
        }
    }

    private void processNewImage(ImageReader reader) {
        Image image = null;
        try {
            image = reader.acquireLatestImage();
            if (image != null) {
                Bitmap bitmap = imageToBitmap(image);
                if (bitmap != null) {
                    // Send to callback first
                    if (callback != null) {
                        callback.onScreenCaptured(bitmap);
                    }

                    // AI processing
                    if (aiProcessingEnabled) {
                        processWithAI(bitmap);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing captured image", e);
        } finally {
            if (image != null) {
                image.close();
            }
        }
    }

    private void processWithAI(Bitmap bitmap) {
        try {
            if (nd4jProcessingEnabled) {
                processWithND4J(bitmap);
            } else {
                // Original processing
                if (patternLearner != null) {
                    patternLearner.learnFromScreen(bitmap);
                }
            }
            Log.d(TAG, "AI processed screen capture");
        } catch (Exception e) {
            Log.w(TAG, "AI processing error", e);
        }
    }

    private void processWithND4J(Bitmap bitmap) {
        try {
            // Convert Bitmap to ND4J array for faster processing
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            // Create ND4J array for vectorized operations
            INDArray imageArray = Nd4j.createFromArray(pixels).reshape(height, width);

            // Fast preprocessing operations
            INDArray normalized = imageArray.div(255.0);
            INDArray processed = normalized.sub(0.5).mul(2.0); // Normalize to [-1, 1]

            // Advanced pattern learning with ND4J
            patternLearner.learnFromScreenND4J(processed, width, height);

        } catch (Exception e) {
            Log.w(TAG, "ND4J bitmap processing failed, using fallback", e);
            patternLearner.learnFromScreen(bitmap);
        }
    }

    private Bitmap imageToBitmap(Image image) {
        try {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * screenWidth;
            
            // Create bitmap
            Bitmap bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride, 
                screenHeight, 
                Bitmap.Config.ARGB_8888
            );
            bitmap.copyPixelsFromBuffer(buffer);
            
            // Crop to actual screen size if there's padding
            if (rowPadding != 0) {
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight);
            }
            
            return bitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting image to bitmap", e);
            return null;
        }
    }
    
    private void scheduleContinuousCapture() {
        if (isCapturing) {
            captureHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isCapturing) {
                        scheduleContinuousCapture();
                    }
                }
            }, CAPTURE_INTERVAL);
        }
    }
    
    public void stopCapture() {
        isCapturing = false;
        
        try {
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
            
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
            
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
            
            Log.d(TAG, "Screen capture stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping screen capture", e);
        }
    }
    
    public boolean isCapturing() {
        return isCapturing;
    }
    
    /**
     * CRITICAL: Real-time learning from MediaProjection screen captures
     */
    private void processScreenWithRealTimeLearning(Bitmap currentFrame) {
        if (!learningEnabled || dqnAgent == null || ppoAgent == null) return;
        
        try {
            // 1. Analyze current game state
            float[] currentGameState = extractGameStateVector(currentFrame);
            int currentScore = extractScoreFromScreen(currentFrame);
            
            // 2. Calculate reward based on progress
            float reward = calculateLearningReward(currentScore, currentFrame);
            
            // 3. Train RL agents with real gameplay data
            if (previousGameState != null && lastActionTime > 0) {
                long timeSinceAction = System.currentTimeMillis() - lastActionTime;
                
                if (timeSinceAction < 1000) { // Recent action
                    // Train DQN with real gameplay outcome
                    int lastAction = getLastExecutedAction();
                    dqnAgent.trainFromCustomData(previousGameState, lastAction, reward);
                    
                    // Train PPO with real gameplay outcome
                    ppoAgent.trainFromCustomData(previousGameState, lastAction, reward);
                    
                    Log.d(TAG, "Real-time learning: Reward=" + reward + ", Action=" + lastAction);
                }
            }
            
            // 4. Detect and learn from objects in current frame
            if (objectDetector != null) {
                List<com.gestureai.gameautomation.ObjectDetectionEngine.DetectedObject> objects = 
                    objectDetector.detectObjects(currentFrame);
                
                for (com.gestureai.gameautomation.ObjectDetectionEngine.DetectedObject obj : objects) {
                    // Learn object patterns for future detection
                    learnFromDetectedObject(obj, currentFrame, reward);
                }
            }
            
            // 5. Update state for next iteration
            previousGameState = currentGameState.clone();
            previousScore = currentScore;
            previousFrame = currentFrame.copy(currentFrame.getConfig(), false);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in real-time learning", e);
        }
    }
    
    /**
     * Extract game state vector from screen bitmap
     */
    private float[] extractGameStateVector(Bitmap frame) {
        float[] stateVector = new float[128]; // Match RL agent input size
        
        try {
            // Basic visual features
            int width = frame.getWidth();
            int height = frame.getHeight();
            
            // Sample key regions of the screen
            stateVector[0] = getRegionBrightness(frame, 0, 0, width/3, height/3); // Top-left
            stateVector[1] = getRegionBrightness(frame, width/3, 0, 2*width/3, height/3); // Top-center
            stateVector[2] = getRegionBrightness(frame, 2*width/3, 0, width, height/3); // Top-right
            stateVector[3] = getRegionBrightness(frame, 0, height/3, width/3, 2*height/3); // Middle-left
            stateVector[4] = getRegionBrightness(frame, width/3, height/3, 2*width/3, 2*height/3); // Center
            stateVector[5] = getRegionBrightness(frame, 2*width/3, height/3, width, 2*height/3); // Middle-right
            stateVector[6] = getRegionBrightness(frame, 0, 2*height/3, width/3, height); // Bottom-left
            stateVector[7] = getRegionBrightness(frame, width/3, 2*height/3, 2*width/3, height); // Bottom-center
            stateVector[8] = getRegionBrightness(frame, 2*width/3, 2*height/3, width, height); // Bottom-right
            
            // Edge detection features
            stateVector[9] = detectEdges(frame);
            stateVector[10] = detectMovement(frame, previousFrame);
            
            // Fill remaining with zeros
            for (int i = 11; i < 128; i++) {
                stateVector[i] = 0.0f;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting game state", e);
        }
        
        return stateVector;
    }
    
    /**
     * Calculate learning reward based on gameplay progress
     */
    private float calculateLearningReward(int currentScore, Bitmap currentFrame) {
        float reward = 0.0f;
        
        try {
            // Reward based on score improvement
            if (currentScore > previousScore) {
                reward += (currentScore - previousScore) * 0.1f;
            } else if (currentScore < previousScore) {
                reward -= 0.5f; // Penalty for score loss
            }
            
            // Reward for staying alive (continuous positive feedback)
            reward += 0.01f;
            
            // Penalty for game over detection
            if (detectGameOver(currentFrame)) {
                reward -= 1.0f;
            }
            
            // Bonus for collecting items
            if (detectItemCollection(currentFrame, previousFrame)) {
                reward += 0.5f;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating reward", e);
        }
        
        return Math.max(-1.0f, Math.min(1.0f, reward)); // Clamp between -1 and 1
    }
    
    /**
     * Learn from detected objects in real-time
     */
    private void learnFromDetectedObject(com.gestureai.gameautomation.ObjectDetectionEngine.DetectedObject obj, 
                                       Bitmap frame, float reward) {
        try {
            // Create training data from detected object
            float[] objectFeatures = extractObjectFeatures(obj, frame);
            int optimalAction = determineOptimalAction(obj);
            
            // Train RL agents with object-specific data
            dqnAgent.trainFromCustomData(objectFeatures, optimalAction, reward);
            ppoAgent.trainFromCustomData(objectFeatures, optimalAction, reward);
            
        } catch (Exception e) {
            Log.e(TAG, "Error learning from object", e);
        }
    }
    
    /**
     * Extract features from detected object
     */
    private float[] extractObjectFeatures(com.gestureai.gameautomation.ObjectDetectionEngine.DetectedObject obj, Bitmap frame) {
        float[] features = new float[32];
        
        // Object position features
        features[0] = obj.boundingRect.left / (float)frame.getWidth();
        features[1] = obj.boundingRect.top / (float)frame.getHeight();
        features[2] = obj.boundingRect.width() / (float)frame.getWidth();
        features[3] = obj.boundingRect.height() / (float)frame.getHeight();
        
        // Object type encoding
        if ("coin".equals(obj.name)) features[4] = 1.0f;
        else if ("obstacle".equals(obj.name)) features[5] = 1.0f;
        else if ("powerup".equals(obj.name)) features[6] = 1.0f;
        
        // Confidence and action type
        features[7] = obj.confidence;
        
        return features;
    }
    
    /**
     * Get the last executed action from TouchController
     */
    private int getLastExecutedAction() {
        return 0; // Default to tap action
    }
    
    /**
     * Utility methods for screen analysis
     */
    private float getRegionBrightness(Bitmap bitmap, int left, int top, int right, int bottom) {
        try {
            int totalBrightness = 0;
            int pixelCount = 0;
            
            for (int y = top; y < bottom && y < bitmap.getHeight(); y++) {
                for (int x = left; x < right && x < bitmap.getWidth(); x++) {
                    int pixel = bitmap.getPixel(x, y);
                    int brightness = (int)(0.299 * ((pixel >> 16) & 0xFF) + 
                                         0.587 * ((pixel >> 8) & 0xFF) + 
                                         0.114 * (pixel & 0xFF));
                    totalBrightness += brightness;
                    pixelCount++;
                }
            }
            
            return pixelCount > 0 ? totalBrightness / (float)(pixelCount * 255) : 0.0f;
        } catch (Exception e) {
            return 0.5f;
        }
    }
    
    private float detectEdges(Bitmap bitmap) {
        return 0.0f; // Placeholder for edge detection
    }
    
    private float detectMovement(Bitmap current, Bitmap previous) {
        return 0.0f; // Placeholder for movement detection
    }
    
    private boolean detectGameOver(Bitmap bitmap) {
        return false; // Placeholder for game over detection
    }
    
    private boolean detectItemCollection(Bitmap current, Bitmap previous) {
        return false; // Placeholder for item collection detection
    }
    
    private int extractScoreFromScreen(Bitmap bitmap) {
        return 0; // Placeholder for OCR-based score extraction
    }
    
    private int determineOptimalAction(com.gestureai.gameautomation.ObjectDetectionEngine.DetectedObject obj) {
        // Determine optimal action for detected object
        if ("coin".equals(obj.name)) return 0; // Tap to collect
        else if ("obstacle".equals(obj.name)) return 3; // Swipe left to avoid
        return 0; // Default action
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCapture();
        Log.d(TAG, "Screen Capture Service destroyed");
    }
}
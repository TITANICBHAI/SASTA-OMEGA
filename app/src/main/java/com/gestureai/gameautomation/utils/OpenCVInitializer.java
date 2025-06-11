package com.gestureai.gameautomation.utils;

import android.content.Context;
import android.util.Log;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

public class OpenCVInitializer {
    private static final String TAG = "OpenCVInitializer";
    private static volatile boolean isInitialized = false;
    private static volatile boolean isInitializing = false;
    private static Context appContext;
    private static final Object initLock = new Object();
    
    public interface OpenCVInitCallback {
        void onInitializationSuccess();
        void onInitializationFailed();
    }
    
    private static BaseLoaderCallback loaderCallback = new BaseLoaderCallback(null) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    synchronized (initLock) {
                        Log.d(TAG, "OpenCV loaded successfully");
                        isInitialized = true;
                        isInitializing = false;
                        if (callback != null) {
                            callback.onInitializationSuccess();
                        }
                    }
                    break;
                default:
                    synchronized (initLock) {
                        super.onManagerConnected(status);
                        Log.e(TAG, "OpenCV loading failed");
                        isInitializing = false;
                        if (callback != null) {
                            callback.onInitializationFailed();
                        }
                    }
                    break;
            }
        }
    };
    
    private static OpenCVInitCallback callback;
    
    public static void initialize(Context context, OpenCVInitCallback initCallback) {
        synchronized (initLock) {
            // Prevent multiple simultaneous initialization attempts
            if (isInitialized) {
                if (initCallback != null) {
                    initCallback.onInitializationSuccess();
                }
                return;
            }
            
            if (isInitializing) {
                Log.d(TAG, "OpenCV initialization already in progress");
                return;
            }
            
            isInitializing = true;
            appContext = context.getApplicationContext();
            callback = initCallback;
            
            if (!OpenCVLoader.initDebug()) {
                Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, appContext, loaderCallback);
            } else {
                Log.d(TAG, "OpenCV library found inside package. Using it!");
                loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        }
    }
    
    public static boolean isOpenCVInitialized() {
        return isInitialized;
    }
    
    public static void onResume() {
        if (!isInitialized && appContext != null) {
            initialize(appContext, callback);
        }
    }
}
package com.gestureai.gameautomation.bridge;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.gestureai.gameautomation.services.TouchAutomationService;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.managers.AIStackManager;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Bridge between React frontend and Android native backend
 */
public class ReactNativeBridge {
    private static final String TAG = "ReactNativeBridge";
    
    private Context context;
    private WebView webView;
    private TouchAutomationService touchService;
    private GameStrategyAgent strategyAgent;
    private AIStackManager aiStackManager;
    
    public ReactNativeBridge(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        this.touchService = TouchAutomationService.getInstanceSafe();
        this.strategyAgent = new GameStrategyAgent(context);
        this.aiStackManager = AIStackManager.getInstance(context);
        
        // Add this bridge to WebView
        webView.addJavascriptInterface(this, "AndroidBridge");
        Log.d(TAG, "React Native Bridge initialized");
    }
    
    @JavascriptInterface
    public String getSystemStatus() {
        try {
            JSONObject status = new JSONObject();
            status.put("touchService", touchService != null && touchService.isServiceReady());
            status.put("aiAgent", strategyAgent != null && strategyAgent.isActive());
            status.put("aiStackEnabled", aiStackManager.isND4JEnabled());
            status.put("timestamp", System.currentTimeMillis());
            
            Log.d(TAG, "System status requested: " + status.toString());
            return status.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error getting system status", e);
            return "{\"error\":\"Failed to get system status\"}";
        }
    }
    
    @JavascriptInterface
    public boolean startAutomation() {
        try {
            if (touchService != null && touchService.isServiceReady()) {
                // Start automation logic
                Log.d(TAG, "Starting automation from React frontend");
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error starting automation", e);
            return false;
        }
    }
    
    @JavascriptInterface
    public boolean stopAutomation() {
        try {
            if (touchService != null) {
                // Stop automation logic
                Log.d(TAG, "Stopping automation from React frontend");
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error stopping automation", e);
            return false;
        }
    }
    
    @JavascriptInterface
    public boolean performTouch(float x, float y) {
        try {
            if (touchService != null && touchService.isServiceReady()) {
                return touchService.executeTouchAction("TAP", x, y);
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error performing touch", e);
            return false;
        }
    }
    
    @JavascriptInterface
    public String getAIMetrics() {
        try {
            JSONObject metrics = new JSONObject();
            
            if (strategyAgent != null) {
                metrics.put("successRate", strategyAgent.getSuccessRate());
                metrics.put("totalActions", strategyAgent.getTotalActionsExecuted());
                metrics.put("averageReactionTime", strategyAgent.getAverageReactionTime());
            }
            
            metrics.put("memoryUsage", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            metrics.put("timestamp", System.currentTimeMillis());
            
            return metrics.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error getting AI metrics", e);
            return "{\"error\":\"Failed to get AI metrics\"}";
        }
    }
    
    public void cleanup() {
        this.context = null;
        this.webView = null;
        this.touchService = null;
        this.strategyAgent = null;
        this.aiStackManager = null;
        Log.d(TAG, "React Native Bridge cleaned up");
    }
}
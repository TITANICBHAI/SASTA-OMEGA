package com.gestureai.gameautomation.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.bridge.ReactNativeBridge;

/**
 * Activity that hosts React frontend components with native Android AI backend integration
 */
public class ReactIntegrationActivity extends AppCompatActivity {
    private static final String TAG = "ReactIntegrationActivity";
    
    private WebView webView;
    private ReactNativeBridge bridge;
    
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_react_integration);
        
        initializeWebView();
        setupBridge();
        loadReactApp();
    }
    
    private void initializeWebView() {
        webView = findViewById(R.id.webview_react);
        
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(false);
        webSettings.setDefaultTextEncodingName("utf-8");
        
        // Enable debugging in development
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "React app loaded successfully");
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(TAG, "WebView error: " + description);
                loadFallbackHTML();
            }
        });
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "React app loaded successfully");
                
                // Initialize React-Native bridge
                initializeBridgeInReact();
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(TAG, "WebView error: " + description);
            }
        });
    }
    
    private void setupBridge() {
        bridge = new ReactNativeBridge(this, webView);
        webView.addJavascriptInterface(bridge, "NativeAndroid");
        Log.d(TAG, "React-Native bridge initialized");
    }
    
    private void loadReactApp() {
        // In development, load from local server
        String reactUrl = "http://localhost:5173"; // Vite dev server default port
        
        // In production, load from assets
        // String reactUrl = "file:///android_asset/react/index.html";
        
        webView.loadUrl(reactUrl);
        Log.d(TAG, "Loading React app from: " + reactUrl);
    }
    
    private void initializeBridgeInReact() {
        // Inject JavaScript to initialize the bridge on React side
        String bridgeInitScript = 
            "window.handleNativeEvent = function(event, data) {" +
            "  if (window.ReactNativeWebView) {" +
            "    window.ReactNativeWebView.handleNativeEvent(event, data);" +
            "  } else {" +
            "    console.log('Native event:', event, data);" +
            "  }" +
            "};" +
            "window.NativeAndroid = window.NativeAndroid || {};" +
            "console.log('React-Native bridge initialized');";
        
        webView.evaluateJavascript(bridgeInitScript, null);
    }
    
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
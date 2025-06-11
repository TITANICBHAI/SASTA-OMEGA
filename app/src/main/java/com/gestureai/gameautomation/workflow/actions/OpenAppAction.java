package com.gestureai.gameautomation.workflow.actions;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import java.util.Map;

/**
 * Open App Action - Launches specified app by package name
 */
public class OpenAppAction implements WorkflowAction {
    private static final String TAG = "OpenAppAction";
    
    private String packageName;
    
    public OpenAppAction(String packageName) {
        this.packageName = packageName;
    }
    
    @Override
    public void execute(Context context, Map<String, Object> variables) throws Exception {
        Log.d(TAG, "Opening app: " + packageName);
        
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(packageName);
        
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            
            // Wait a moment for app to launch
            Thread.sleep(2000);
        } else {
            throw new Exception("App not found: " + packageName);
        }
    }
    
    @Override
    public String getDescription() {
        return "Open app: " + packageName;
    }
    
    @Override
    public boolean isValid() {
        return packageName != null && !packageName.isEmpty();
    }
    
    // Getters and setters
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
}
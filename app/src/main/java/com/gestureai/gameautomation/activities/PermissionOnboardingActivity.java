package com.gestureai.gameautomation.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.gestureai.gameautomation.MainActivity;
import com.gestureai.gameautomation.R;

/**
 * Onboarding activity for requesting necessary permissions
 * Set as launcher activity in AndroidManifest.xml
 */
public class PermissionOnboardingActivity extends AppCompatActivity {
    
    private static final int REQUEST_ACCESSIBILITY = 1001;
    private static final int REQUEST_OVERLAY = 1002;
    
    private Button btnAccessibility;
    private Button btnOverlay;
    private Button btnContinue;
    private TextView tvStatus;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_onboarding);
        
        initializeViews();
        setupListeners();
        checkPermissions();
    }
    
    private void initializeViews() {
        btnAccessibility = findViewById(R.id.btn_enable_accessibility);
        btnOverlay = findViewById(R.id.btn_enable_overlay);
        btnContinue = findViewById(R.id.btn_continue);
        tvStatus = findViewById(R.id.tv_permission_status);
    }
    
    private void setupListeners() {
        btnAccessibility.setOnClickListener(v -> requestAccessibilityPermission());
        btnOverlay.setOnClickListener(v -> requestOverlayPermission());
        btnContinue.setOnClickListener(v -> continueToMainApp());
    }
    
    private void requestAccessibilityPermission() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivityForResult(intent, REQUEST_ACCESSIBILITY);
    }
    
    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_OVERLAY);
    }
    
    private void continueToMainApp() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void checkPermissions() {
        boolean accessibilityEnabled = isAccessibilityServiceEnabled();
        boolean overlayEnabled = Settings.canDrawOverlays(this);
        
        btnAccessibility.setEnabled(!accessibilityEnabled);
        btnOverlay.setEnabled(!overlayEnabled);
        btnContinue.setEnabled(accessibilityEnabled && overlayEnabled);
        
        if (accessibilityEnabled && overlayEnabled) {
            tvStatus.setText("All permissions granted! Ready to continue.");
        } else {
            tvStatus.setText("Please grant the required permissions to continue.");
        }
    }
    
    private boolean isAccessibilityServiceEnabled() {
        // Simple check - in production would verify specific service
        return false; // Force user to enable manually
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        checkPermissions();
    }
}
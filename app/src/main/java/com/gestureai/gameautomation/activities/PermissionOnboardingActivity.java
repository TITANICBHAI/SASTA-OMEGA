package com.gestureai.gameautomation.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.MainActivity;

public class PermissionOnboardingActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSIONS = 1001;
    private static final int REQUEST_OVERLAY_PERMISSION = 1002;
    private static final int REQUEST_ACCESSIBILITY = 1003;
    private static final int REQUEST_SCREEN_CAPTURE = 1004;
    
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.VIBRATE
    };
    
    private TextView tvPermissionStatus;
    private Button btnGrantBasicPermissions;
    private Button btnGrantOverlayPermission;
    private Button btnOpenAccessibilitySettings;
    private Button btnRequestScreenCapture;
    private Button btnContinueToApp;
    
    private boolean basicPermissionsGranted = false;
    private boolean overlayPermissionGranted = false;
    private boolean accessibilityEnabled = false;
    private boolean screenCapturePermissionGranted = false;
    private MediaProjectionManager mediaProjectionManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_onboarding);
        
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        
        initializeViews();
        checkAllPermissions();
        updateUI();
    }
    
    private void initializeViews() {
        tvPermissionStatus = findViewById(R.id.tv_permission_status);
        btnGrantBasicPermissions = findViewById(R.id.btn_grant_basic_permissions);
        btnGrantOverlayPermission = findViewById(R.id.btn_grant_overlay_permission);
        btnOpenAccessibilitySettings = findViewById(R.id.btn_open_accessibility_settings);
        btnRequestScreenCapture = findViewById(R.id.btn_request_screen_capture);
        btnContinueToApp = findViewById(R.id.btn_continue_to_app);
        
        btnGrantBasicPermissions.setOnClickListener(v -> requestBasicPermissions());
        btnGrantOverlayPermission.setOnClickListener(v -> requestOverlayPermission());
        btnOpenAccessibilitySettings.setOnClickListener(v -> openAccessibilitySettings());
        btnRequestScreenCapture.setOnClickListener(v -> requestScreenCapturePermission());
        btnContinueToApp.setOnClickListener(v -> continueToMainApp());
    }
    
    private void checkAllPermissions() {
        basicPermissionsGranted = hasAllBasicPermissions();
        overlayPermissionGranted = hasOverlayPermission();
        accessibilityEnabled = isAccessibilityServiceEnabled();
    }
    
    private boolean hasAllBasicPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    private boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }
    
    private boolean isAccessibilityServiceEnabled() {
        // Check if TouchAutomationService is enabled
        String serviceName = getPackageName() + "/com.gestureai.gameautomation.services.TouchAutomationService";
        String enabledServices = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabledServices != null && enabledServices.contains(serviceName);
    }
    
    private void updateUI() {
        StringBuilder statusText = new StringBuilder();
        statusText.append("Permission Status:\n\n");
        
        statusText.append("Basic Permissions: ");
        statusText.append(basicPermissionsGranted ? "✓ Granted" : "✗ Required");
        statusText.append("\n");
        
        statusText.append("Overlay Permission: ");
        statusText.append(overlayPermissionGranted ? "✓ Granted" : "✗ Required");
        statusText.append("\n");
        
        statusText.append("Accessibility Service: ");
        statusText.append(accessibilityEnabled ? "✓ Enabled" : "✗ Required");
        statusText.append("\n");
        
        tvPermissionStatus.setText(statusText.toString());
        
        btnGrantBasicPermissions.setEnabled(!basicPermissionsGranted);
        btnGrantOverlayPermission.setEnabled(!overlayPermissionGranted);
        btnOpenAccessibilitySettings.setEnabled(!accessibilityEnabled);
        
        btnContinueToApp.setEnabled(basicPermissionsGranted && overlayPermissionGranted && accessibilityEnabled);
    }
    
    private void requestBasicPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS);
    }
    
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        }
    }
    
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivityForResult(intent, REQUEST_ACCESSIBILITY);
        Toast.makeText(this, "Please enable 'GestureAI Touch Automation' in Accessibility settings", Toast.LENGTH_LONG).show();
    }
    
    private void requestScreenCapturePermission() {
        if (mediaProjectionManager != null) {
            Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
            startActivityForResult(captureIntent, REQUEST_SCREEN_CAPTURE);
        } else {
            Toast.makeText(this, "Screen capture not available on this device", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void continueToMainApp() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_PERMISSIONS) {
            checkAllPermissions();
            updateUI();
            
            if (basicPermissionsGranted) {
                Toast.makeText(this, "Basic permissions granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Some permissions were denied. The app may not work properly.", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION || requestCode == REQUEST_ACCESSIBILITY) {
            checkAllPermissions();
            updateUI();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        checkAllPermissions();
        updateUI();
    }
}
package com.gestureai.gameautomation.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.utils.ServiceStartupCoordinator;
import com.gestureai.gameautomation.utils.ErrorRecoveryManager;
import com.gestureai.gameautomation.ai.DQNAgent;
import com.gestureai.gameautomation.ai.PPOAgent;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.database.DatabaseIntegrationManager;

/**
 * Guided setup wizard for first-time users
 */
public class SetupWizardActivity extends AppCompatActivity {
    private static final String TAG = "SetupWizardActivity";
    
    private ViewPager2 viewPager;
    private Button btnNext, btnPrevious, btnFinish;
    private ProgressBar progressSetup;
    private TextView tvCurrentStep, tvStepDescription;
    
    // Setup components
    private ServiceStartupCoordinator serviceCoordinator;
    private ErrorRecoveryManager errorRecovery;
    private DatabaseIntegrationManager databaseManager;
    
    // Setup progress tracking
    private boolean permissionsGranted = false;
    private boolean servicesInitialized = false;
    private boolean aiComponentsReady = false;
    private boolean databaseInitialized = false;
    
    private int currentStep = 0;
    private final int totalSteps = 5;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_wizard);
        
        initializeViews();
        initializeComponents();
        setupListeners();
        startSetupProcess();
    }
    
    private void initializeViews() {
        viewPager = findViewById(R.id.viewpager_setup);
        btnNext = findViewById(R.id.btn_next);
        btnPrevious = findViewById(R.id.btn_previous);
        btnFinish = findViewById(R.id.btn_finish);
        progressSetup = findViewById(R.id.progress_setup);
        tvCurrentStep = findViewById(R.id.tv_current_step);
        tvStepDescription = findViewById(R.id.tv_step_description);
        
        // Initially hide finish button
        btnFinish.setVisibility(View.GONE);
        btnPrevious.setEnabled(false);
    }
    
    private void initializeComponents() {
        try {
            serviceCoordinator = ServiceStartupCoordinator.getInstance(this);
            errorRecovery = ErrorRecoveryManager.getInstance(this);
            databaseManager = DatabaseIntegrationManager.getInstance(this);
            
            setupErrorRecoveryListener();
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error initializing setup components", e);
            showError("Setup initialization failed: " + e.getMessage());
        }
    }
    
    private void setupErrorRecoveryListener() {
        errorRecovery.setRecoveryListener(new ErrorRecoveryManager.RecoveryListener() {
            @Override
            public void onRecoveryStarted(String component, String error) {
                runOnUiThread(() -> {
                    tvStepDescription.setText("Recovering " + component + "...");
                    progressSetup.setIndeterminate(true);
                });
            }
            
            @Override
            public void onRecoverySuccess(String component) {
                runOnUiThread(() -> {
                    tvStepDescription.setText(component + " recovered successfully");
                    progressSetup.setIndeterminate(false);
                });
            }
            
            @Override
            public void onRecoveryFailed(String component, String reason) {
                runOnUiThread(() -> {
                    showError("Recovery failed for " + component + ": " + reason);
                });
            }
            
            @Override
            public void onSystemRestart() {
                runOnUiThread(() -> {
                    tvStepDescription.setText("Performing system restart...");
                    progressSetup.setIndeterminate(true);
                });
            }
        });
    }
    
    private void setupListeners() {
        btnNext.setOnClickListener(v -> nextStep());
        btnPrevious.setOnClickListener(v -> previousStep());
        btnFinish.setOnClickListener(v -> finishSetup());
    }
    
    private void startSetupProcess() {
        updateStepDisplay();
        executeCurrentStep();
    }
    
    private void nextStep() {
        if (currentStep < totalSteps - 1) {
            currentStep++;
            updateStepDisplay();
            executeCurrentStep();
        }
    }
    
    private void previousStep() {
        if (currentStep > 0) {
            currentStep--;
            updateStepDisplay();
            executeCurrentStep();
        }
    }
    
    private void updateStepDisplay() {
        tvCurrentStep.setText("Step " + (currentStep + 1) + " of " + totalSteps);
        progressSetup.setProgress((int) ((currentStep + 1) / (float) totalSteps * 100));
        
        btnPrevious.setEnabled(currentStep > 0);
        
        if (currentStep == totalSteps - 1) {
            btnNext.setVisibility(View.GONE);
            btnFinish.setVisibility(View.VISIBLE);
        } else {
            btnNext.setVisibility(View.VISIBLE);
            btnFinish.setVisibility(View.GONE);
        }
    }
    
    private void executeCurrentStep() {
        switch (currentStep) {
            case 0:
                stepWelcome();
                break;
            case 1:
                stepPermissions();
                break;
            case 2:
                stepServices();
                break;
            case 3:
                stepAIComponents();
                break;
            case 4:
                stepComplete();
                break;
        }
    }
    
    private void stepWelcome() {
        tvStepDescription.setText("Welcome to GestureAI Game Automation! This wizard will guide you through the setup process.");
        btnNext.setEnabled(true);
    }
    
    private void stepPermissions() {
        tvStepDescription.setText("Checking required permissions...");
        btnNext.setEnabled(false);
        
        new Thread(() -> {
            try {
                // Check accessibility permission
                boolean accessibilityEnabled = isAccessibilityServiceEnabled();
                
                // Check overlay permission
                boolean overlayEnabled = android.provider.Settings.canDrawOverlays(this);
                
                // Check other permissions
                boolean cameraEnabled = checkSelfPermission(android.Manifest.permission.CAMERA) == 
                    android.content.pm.PackageManager.PERMISSION_GRANTED;
                
                boolean storageEnabled = checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == 
                    android.content.pm.PackageManager.PERMISSION_GRANTED;
                
                permissionsGranted = accessibilityEnabled && overlayEnabled && cameraEnabled && storageEnabled;
                
                runOnUiThread(() -> {
                    if (permissionsGranted) {
                        tvStepDescription.setText("All permissions granted ✓");
                        btnNext.setEnabled(true);
                    } else {
                        tvStepDescription.setText("Some permissions are missing. Please enable them in Settings.");
                        
                        // Provide button to open settings
                        Button btnOpenSettings = findViewById(R.id.btn_open_settings);
                        if (btnOpenSettings != null) {
                            btnOpenSettings.setVisibility(View.VISIBLE);
                            btnOpenSettings.setOnClickListener(v -> openPermissionSettings());
                        }
                    }
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showError("Error checking permissions: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void stepServices() {
        tvStepDescription.setText("Initializing system services...");
        btnNext.setEnabled(false);
        progressSetup.setIndeterminate(true);
        
        serviceCoordinator.startServicesWithCoordination(new ServiceStartupCoordinator.ServiceReadyCallback() {
            @Override
            public void onAllServicesReady() {
                servicesInitialized = true;
                runOnUiThread(() -> {
                    tvStepDescription.setText("All services initialized successfully ✓");
                    progressSetup.setIndeterminate(false);
                    btnNext.setEnabled(true);
                });
            }
            
            @Override
            public void onServiceFailed(String serviceName, String error) {
                runOnUiThread(() -> {
                    tvStepDescription.setText("Service initialization failed: " + serviceName);
                    progressSetup.setIndeterminate(false);
                    
                    // Attempt recovery
                    errorRecovery.recoverService(serviceName, new Exception(error));
                });
            }
        });
    }
    
    private void stepAIComponents() {
        tvStepDescription.setText("Initializing AI components...");
        btnNext.setEnabled(false);
        progressSetup.setIndeterminate(true);
        
        new Thread(() -> {
            try {
                // Initialize DQN Agent
                updateStepDescription("Initializing DQN Agent...");
                DQNAgent dqnAgent = DQNAgent.getInstance(16, 8);
                
                // Initialize PPO Agent
                updateStepDescription("Initializing PPO Agent...");
                PPOAgent ppoAgent = PPOAgent.getInstance(16, 8);
                
                // Initialize Strategy Agent
                updateStepDescription("Initializing Game Strategy Agent...");
                GameStrategyAgent strategyAgent = new GameStrategyAgent(this);
                
                // Initialize Database
                updateStepDescription("Initializing database...");
                databaseManager.getAllSessions().thenAccept(sessions -> {
                    databaseInitialized = true;
                    android.util.Log.d(TAG, "Database initialized with " + sessions.size() + " existing sessions");
                });
                
                // Wait a moment for all components to stabilize
                Thread.sleep(3000);
                
                aiComponentsReady = true;
                
                runOnUiThread(() -> {
                    tvStepDescription.setText("AI components initialized successfully ✓");
                    progressSetup.setIndeterminate(false);
                    btnNext.setEnabled(true);
                });
                
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error initializing AI components", e);
                
                runOnUiThread(() -> {
                    tvStepDescription.setText("AI initialization failed - attempting recovery...");
                    
                    // Attempt AI recovery
                    boolean recovered = errorRecovery.recoverAIModels("ALL", e);
                    if (recovered) {
                        aiComponentsReady = true;
                        tvStepDescription.setText("AI components recovered successfully ✓");
                        progressSetup.setIndeterminate(false);
                        btnNext.setEnabled(true);
                    } else {
                        showError("AI initialization failed: " + e.getMessage());
                    }
                });
            }
        }).start();
    }
    
    private void stepComplete() {
        tvStepDescription.setText("Setup completed successfully! You can now use GestureAI Game Automation.");
        
        // Show setup summary
        TextView tvSummary = findViewById(R.id.tv_setup_summary);
        if (tvSummary != null) {
            StringBuilder summary = new StringBuilder();
            summary.append("Setup Summary:\n");
            summary.append("✓ Permissions: ").append(permissionsGranted ? "Granted" : "Incomplete").append("\n");
            summary.append("✓ Services: ").append(servicesInitialized ? "Initialized" : "Failed").append("\n");
            summary.append("✓ AI Components: ").append(aiComponentsReady ? "Ready" : "Failed").append("\n");
            summary.append("✓ Database: ").append(databaseInitialized ? "Connected" : "Failed").append("\n");
            
            tvSummary.setText(summary.toString());
            tvSummary.setVisibility(View.VISIBLE);
        }
        
        btnFinish.setEnabled(true);
    }
    
    private void finishSetup() {
        // Save setup completion
        getSharedPreferences("setup", MODE_PRIVATE)
            .edit()
            .putBoolean("setup_completed", true)
            .putLong("setup_timestamp", System.currentTimeMillis())
            .apply();
        
        // Return to main activity
        Intent intent = new Intent(this, com.gestureai.gameautomation.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    
    // Helper methods
    private void updateStepDescription(String description) {
        runOnUiThread(() -> tvStepDescription.setText(description));
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        android.util.Log.e(TAG, message);
    }
    
    private boolean isAccessibilityServiceEnabled() {
        try {
            return com.gestureai.gameautomation.services.TouchAutomationService.getInstanceSafe() != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void openPermissionSettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error opening permission settings", e);
        }
    }
    
    public static boolean isSetupCompleted(android.content.Context context) {
        return context.getSharedPreferences("setup", MODE_PRIVATE)
            .getBoolean("setup_completed", false);
    }
}
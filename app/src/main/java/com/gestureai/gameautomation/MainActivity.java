package com.gestureai.gameautomation;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.gestureai.gameautomation.fragments.SettingsFragment;
import androidx.fragment.app.FragmentTransaction;
import com.gestureai.gameautomation.fragments.AnalyticsFragment;
import com.gestureai.gameautomation.fragments.AutoPlayFragment;
import com.gestureai.gameautomation.fragments.GestureControllerFragment;
import com.gestureai.gameautomation.fragments.ScreenMonitorFragment;
import com.gestureai.gameautomation.fragments.TrainingFragment;
import com.gestureai.gameautomation.fragments.DebugFragment;
import com.gestureai.gameautomation.fragments.MoreFragment;
import com.gestureai.gameautomation.fragments.PerformanceFragment;
import com.gestureai.gameautomation.services.TouchAutomationService;
import com.gestureai.gameautomation.utils.NLPProcessor;
import com.gestureai.gameautomation.utils.OpenCVHelper;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.ai.AdaptiveDecisionMaker;
import com.gestureai.gameautomation.ai.GameStatePredictor;
import com.gestureai.gameautomation.ai.PatternLearningEngine;
import com.gestureai.gameautomation.ai.*;
import com.gestureai.gameautomation.managers.ServiceConnectionManager;
import com.gestureai.gameautomation.managers.FragmentNavigationManager;
import com.gestureai.gameautomation.managers.AIModelLoadingManager;
import com.gestureai.gameautomation.managers.DatabaseUIManager;
import com.gestureai.gameautomation.managers.ServiceIntegrationManager;
import com.gestureai.gameautomation.managers.AIStackManager;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.appcompat.widget.SwitchCompat;
import java.io.InputStream;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_ACCESSIBILITY_PERMISSION = 1001;
    private static final int REQUEST_OVERLAY_PERMISSION = 1002;
    private static final int REQUEST_SCREENSHOT_SELECTION = 1003;
    
    private ObjectDetectionEngine detectionEngine;
    private NLPProcessor nlpProcessor;
    private TouchAutomationService automationService;
    private GameStrategyAgent gameStrategyAgent;
    private AdaptiveDecisionMaker adaptiveDecisionMaker;
    private GameStatePredictor gameStatePredictor;
    private PatternLearningEngine patternLearningEngine;
    private ReinforcementLearner reinforcementLearner;
    private boolean aiSystemInitialized = false;
    // UI Components
    private Button btnStartAutomation;
    private Button btnStopAutomation;
    private Button btnTrainObjects;
    private Button btnSettings;
    private Button btnAnalytics;
    private TextView tvStatus;
    private ImageView ivScreenshot;
    private GameTypeDetector gameTypeDetector;
    private MultiPlayerStrategy multiPlayerStrategy;
    private MOBAStrategy mobaStrategy;
    private FPSStrategy fpsStrategy;
    private CheckBox cbAutoGameDetection;
    private ProgressBar pbModelLoading;
    private TextView tvGameTypeDetected;
    private TextView tvModelStatus;
    private Button btnLoadModel;
    private BottomNavigationView bottomNavigationView;
    private Fragment currentFragment;
    
    // ND4J AI Toggle Components
    private SwitchCompat switchND4J;
    private TextView tvAIStatus;

    // Backend Integration Managers
    private ServiceConnectionManager serviceConnectionManager;
    private FragmentNavigationManager fragmentNavigationManager;
    private AIModelLoadingManager aiModelLoadingManager;
    private DatabaseUIManager databaseUIManager;
    private ServiceIntegrationManager serviceIntegrationManager;
    private AIStackManager aiStackManager;
    
    private boolean isAutomationActive = false;
    private boolean permissionsGranted = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeUI();
        initializeBackendManagers();
        initializeComponents();
        checkPermissions();
        setupBottomNavigation();
        setupManagerListeners();

        Log.d(TAG, "MainActivity created with comprehensive backend integration");
    }
    
    private void initializeBackendManagers() {
        try {
            // Initialize service connection manager
            serviceConnectionManager = ServiceConnectionManager.getInstance(this);
            
            // Initialize fragment navigation manager
            fragmentNavigationManager = new FragmentNavigationManager(this);
            
            // Initialize AI model loading manager
            aiModelLoadingManager = AIModelLoadingManager.getInstance(this);
            
            // Initialize database UI manager
            databaseUIManager = DatabaseUIManager.getInstance(this);
            
            // Initialize service integration manager
            serviceIntegrationManager = ServiceIntegrationManager.getInstance(this);
            
            // Initialize AI stack manager with ND4J toggle
            aiStackManager = AIStackManager.getInstance(this);
            setupAIStackCallback();
            
            Log.d(TAG, "All backend managers initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing backend managers", e);
        }
    }
    
    private void setupManagerListeners() {
        // Service connection listener
        serviceConnectionManager.setServiceConnectionListener(new ServiceConnectionManager.ServiceConnectionListener() {
            @Override
            public void onServiceConnected(String serviceName) {
                runOnUiThread(() -> {
                    tvStatus.setText("Service Connected: " + serviceName);
                    fragmentNavigationManager.onServiceStateChanged(serviceName, true);
                });
            }
            
            @Override
            public void onServiceDisconnected(String serviceName) {
                runOnUiThread(() -> {
                    tvStatus.setText("Service Disconnected: " + serviceName);
                    fragmentNavigationManager.onServiceStateChanged(serviceName, false);
                });
            }
            
            @Override
            public void onServiceError(String serviceName, String error) {
                runOnUiThread(() -> {
                    tvStatus.setText("Service Error: " + serviceName + " - " + error);
                });
            }
        });
        
        // AI model loading listener
        aiModelLoadingManager.setModelLoadingListener(new AIModelLoadingManager.ModelLoadingListener() {
            @Override
            public void onModelLoadingStarted(String modelName) {
                runOnUiThread(() -> {
                    pbModelLoading.setVisibility(android.view.View.VISIBLE);
                    tvModelStatus.setText("Loading: " + modelName);
                });
            }
            
            @Override
            public void onModelLoadingProgress(String modelName, int progress) {
                runOnUiThread(() -> {
                    pbModelLoading.setProgress(progress);
                });
            }
            
            @Override
            public void onModelLoadingCompleted(String modelName) {
                runOnUiThread(() -> {
                    tvModelStatus.setText("Loaded: " + modelName);
                    if (aiModelLoadingManager.areAllModelsLoaded()) {
                        pbModelLoading.setVisibility(android.view.View.GONE);
                        tvModelStatus.setText("All AI Models Loaded");
                    }
                });
            }
            
            @Override
            public void onModelLoadingFailed(String modelName, String error) {
                runOnUiThread(() -> {
                    tvModelStatus.setText("Failed: " + modelName + " - " + error);
                });
            }
        });
        
        // Database operation listener
        databaseUIManager.setDatabaseOperationListener(new DatabaseUIManager.DatabaseOperationListener() {
            @Override
            public void onDataLoaded(String dataType, List<?> data) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Data loaded: " + dataType + ", count: " + data.size());
                    fragmentNavigationManager.refreshAllFragments();
                });
            }
            
            @Override
            public void onDataSaved(String dataType, boolean success) {
                runOnUiThread(() -> {
                    String message = success ? "Data saved: " + dataType : "Failed to save: " + dataType;
                    Log.d(TAG, message);
                });
            }
            
            @Override
            public void onDataDeleted(String dataType, boolean success) {
                runOnUiThread(() -> {
                    String message = success ? "Data deleted: " + dataType : "Failed to delete: " + dataType;
                    Log.d(TAG, message);
                });
            }
            
            @Override
            public void onDatabaseError(String operation, String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Database error in " + operation + ": " + error);
                });
            }
        });
    }
    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_autoplay) {
                selectedFragment = new AutoPlayFragment();
            } else if (item.getItemId() == R.id.nav_gesture_controller) {
                selectedFragment = new GestureControllerFragment();
            } else if (item.getItemId() == R.id.nav_screen_monitor) {
                selectedFragment = new ScreenMonitorFragment();
            } else if (item.getItemId() == R.id.nav_training) {
                selectedFragment = new TrainingFragment();
            } else if (item.getItemId() == R.id.nav_analytics) {
                selectedFragment = new AnalyticsFragment();
            } else if (item.getItemId() == R.id.nav_performance) {
                selectedFragment = new PerformanceFragment();
            } else if (item.getItemId() == R.id.nav_debug) {
                selectedFragment = new DebugFragment();
            } else if (item.getItemId() == R.id.nav_more) {
                selectedFragment = new MoreFragment();
            }

            if (selectedFragment != null) {
                replaceFragment(selectedFragment);
            }
            return true;
        });

        // Load default fragment
        replaceFragment(new AutoPlayFragment());
    }
    private void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
        currentFragment = fragment;
    }

    // Public method for fragments to switch to other fragments
    public void switchToFragment(Fragment fragment) {
        replaceFragment(fragment);
    }

    
    private void initializeUI() {
        btnStartAutomation = findViewById(R.id.btn_start_automation);
        btnStopAutomation = findViewById(R.id.btn_stop_automation);
        btnTrainObjects = findViewById(R.id.btn_train_objects);
        btnSettings = findViewById(R.id.btn_settings);
        btnAnalytics = findViewById(R.id.btn_analytics);
        tvStatus = findViewById(R.id.tv_status);
        ivScreenshot = findViewById(R.id.iv_screenshot);
        
        // Initialize ND4J toggle components
        switchND4J = findViewById(R.id.switch_nd4j);
        tvAIStatus = findViewById(R.id.tv_ai_status);
        
        // Set click listeners
        btnStartAutomation.setOnClickListener(this::startAutomation);
        btnStopAutomation.setOnClickListener(this::stopAutomation);
        btnTrainObjects.setOnClickListener(this::openTrainingMode);
        btnSettings.setOnClickListener(this::openSettings);
        btnAnalytics.setOnClickListener(this::openAnalytics);
        
        // Setup ND4J toggle listener
        switchND4J.setOnCheckedChangeListener(this::onND4JToggleChanged);
        
        // Initially disable automation buttons
        btnStartAutomation.setEnabled(false);
        btnStopAutomation.setEnabled(false);

        
        updateStatus("Initializing...");
    }
    
    private void initializeComponents() {
        try {
            // Initialize OpenCV
            OpenCVHelper.initOpenCV(this);
            // Initialize AI learning system
            initializeAISystem();
            // Initialize NLP processor
            nlpProcessor = new NLPProcessor(this);
            
            // Initialize object detection engine
            detectionEngine = new ObjectDetectionEngine(this);
            
            updateStatus("Components initialized");
            Log.d(TAG, "All components initialized successfully");
            
            // Load sample Subway Surfers screenshots for training
            loadSampleScreenshots();
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing components", e);
            updateStatus("Initialization failed: " + e.getMessage());
        }
    }
    private void initializeAISystem() {
        try {
            Log.d(TAG, "Initializing complete AI learning system...");

            // Initialize DL4J components
            gameStrategyAgent = new GameStrategyAgent(this);
            adaptiveDecisionMaker = new AdaptiveDecisionMaker();
            gameStatePredictor = new GameStatePredictor();
            patternLearningEngine = new PatternLearningEngine(this);

            // Initialize reinforcement learning with DL4J integration
            reinforcementLearner = new ReinforcementLearner(this);

            // Connect all AI systems
            GameAutomationEngine.initialize(this);

            aiSystemInitialized = true;
            Log.d(TAG, "Complete AI learning system initialized successfully");
            updateStatus("AI learning system ready");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing AI system", e);
            updateStatus("AI system initialization failed: " + e.getMessage());
            aiSystemInitialized = false;
        }
    }
    private void loadSampleScreenshots() {
        // This method will load the Subway Surfers screenshots from assets
        // for object detection training
        try {
            String[] screenshots = getAssets().list("subway_surfers_frames");
            if (screenshots != null && screenshots.length > 0) {
                Log.d(TAG, "Found " + screenshots.length + " training screenshots");
                updateStatus("Ready for training with " + screenshots.length + " screenshots");
                
                // Load first screenshot as preview
                InputStream inputStream = getAssets().open("subway_surfers_frames/" + screenshots[0]);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                ivScreenshot.setImageBitmap(bitmap);
                inputStream.close();
                
                // Demonstrate object detection on the first frame
                demonstrateDetection(bitmap);
            }
        } catch (Exception e) {
            Log.w(TAG, "No sample screenshots found in assets", e);
            updateStatus("Ready - No training data loaded");
        }
    }

    private void demonstrateDetection(Bitmap screenshot) {
        if (detectionEngine != null) {
            List<ObjectDetectionEngine.DetectedObject> objects = detectionEngine.detectObjects(screenshot);
            Log.d(TAG, "Detected " + objects.size() + " objects in sample screenshot");

            for (ObjectDetectionEngine.DetectedObject obj : objects) {
                float centerX = obj.boundingRect.x + (obj.boundingRect.width / 2.0f);
                float centerY = obj.boundingRect.y + (obj.boundingRect.height / 2.0f);
                Log.d(TAG, "Detected " + obj.name + " at (" +
                        centerX + ", " + centerY +
                        ") confidence: " + obj.confidence);
            }


            if (!objects.isEmpty()) {
                updateStatus("Sample detection: Found " + objects.size() + " objects");
            }
        }
    }
    
    private void checkPermissions() {
        boolean accessibilityEnabled = isAccessibilityServiceEnabled();
        boolean overlayPermission = Settings.canDrawOverlays(this);
        
        permissionsGranted = accessibilityEnabled && overlayPermission;
        
        if (permissionsGranted) {
            btnStartAutomation.setEnabled(true);
            updateStatus("All permissions granted - Ready to start");
        } else {
            updateStatus("Permissions required");
            requestMissingPermissions(accessibilityEnabled, overlayPermission);
        }
    }
    
    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/com.gestureai.gameautomation.services.TouchAutomationService";
        String enabledServices = Settings.Secure.getString(
            getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        
        return enabledServices != null && enabledServices.contains(service);
    }
    
    private void requestMissingPermissions(boolean accessibilityEnabled, boolean overlayPermission) {
        if (!accessibilityEnabled) {
            Toast.makeText(this, "Please enable accessibility service for game automation", 
                Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivityForResult(intent, REQUEST_ACCESSIBILITY_PERMISSION);
        } else if (!overlayPermission) {
            Toast.makeText(this, "Please grant overlay permission for automation controls", 
                Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        }
    }
    
    private void startAutomation(View view) {
        if (!permissionsGranted) {
            checkPermissions();
            return;
        }
        
        try {
            updateStatus("Starting automation for Subway Surfers...");
            
            // Start the touch automation service
            Intent touchIntent = new Intent(this, com.gestureai.gameautomation.services.TouchAutomationService.class);
            startService(touchIntent);
            
            // Start the overlay controls service
            Intent overlayIntent = new Intent(this, OverlayService.class);
            startService(overlayIntent);
            
            // Start gesture recognition service
            Intent gestureIntent = new Intent(this, com.gestureai.gameautomation.services.GestureRecognitionService.class);
            gestureIntent.setAction("start");
            startService(gestureIntent);
            
            isAutomationActive = true;
            btnStartAutomation.setEnabled(false);
            btnStopAutomation.setEnabled(true);
            
            updateStatus("Automation active - Waiting for Subway Surfers");
            Toast.makeText(this, "Automation started! Open Subway Surfers to begin.", 
                Toast.LENGTH_LONG).show();
            
            Log.d(TAG, "Automation started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting automation", e);
            updateStatus("Failed to start automation: " + e.getMessage());
            Toast.makeText(this, "Failed to start automation", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopAutomation(View view) {
        try {
            updateStatus("Stopping automation...");
            
            // Stop all automation services
            stopService(new Intent(this, com.gestureai.gameautomation.services.TouchAutomationService.class));
            stopService(new Intent(this, OverlayService.class));
            stopService(new Intent(this, com.gestureai.gameautomation.services.GestureRecognitionService.class));
            stopService(new Intent(this, com.gestureai.gameautomation.services.VoiceCommandService.class));
            
            isAutomationActive = false;
            btnStartAutomation.setEnabled(true);
            btnStopAutomation.setEnabled(false);
            
            updateStatus("Automation stopped");
            Toast.makeText(this, "Automation stopped", Toast.LENGTH_SHORT).show();
            
            Log.d(TAG, "Automation stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping automation", e);
            updateStatus("Error stopping automation");
        }
    }
    
    private void openTrainingMode(View view) {
        try {
            updateStatus("Opening training mode...");
            Intent intent = new Intent(this, com.gestureai.gameautomation.activities.ObjectLabelingActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening training mode", e);
            Toast.makeText(this, "Error opening training mode", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openSettings(View view) {
        updateStatus("Opening settings...");
        Intent intent = new Intent(this, com.gestureai.gameautomation.activities.SettingsActivity.class);
        startActivity(intent);
    }
    
    private void openAnalytics(View view) {
        updateStatus("Opening analytics...");
        Intent intent = new Intent(this, com.gestureai.gameautomation.activities.AnalyticsActivity.class);
        startActivity(intent);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, com.gestureai.gameautomation.activities.SettingsActivity.class));
            return true;
        } else if (id == R.id.action_analytics) {
            startActivity(new Intent(this, com.gestureai.gameautomation.activities.AnalyticsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        switch (requestCode) {
            case REQUEST_ACCESSIBILITY_PERMISSION:
            case REQUEST_OVERLAY_PERMISSION:
                checkPermissions();
                break;
                
            case REQUEST_SCREENSHOT_SELECTION:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    handleSelectedScreenshot(data.getData());
                }
                break;
        }
    }
    
    private void handleSelectedScreenshot(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            if (bitmap != null) {
                ivScreenshot.setImageBitmap(bitmap);
                
                // Demonstrate training with NLP commands
                demonstrateTraining(bitmap);
                
                updateStatus("Screenshot loaded - Ready for training");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading selected screenshot", e);
            Toast.makeText(this, "Error loading screenshot", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void demonstrateTraining(Bitmap screenshot) {
        // Demonstrate how to add custom objects with natural language
        if (detectionEngine != null && nlpProcessor != null) {
            
            // Example: Add a custom coin object with NLP action logic
            android.graphics.Rect coinBoundingBox = new android.graphics.Rect(100, 200, 150, 250);
            String actionLogic = "When you see this coin, collect it by tapping";

            org.opencv.core.Rect coinOpenCVRect = new org.opencv.core.Rect(
                    coinBoundingBox.left, coinBoundingBox.top,
                    coinBoundingBox.width(), coinBoundingBox.height());
            
            // Example: Add obstacle with conditional logic
            android.graphics.Rect obstacleBoundingBox = new android.graphics.Rect(300, 400, 400, 500);
            String obstacleLogic = "If obstacle ahead then jump over it";

            org.opencv.core.Rect obstacleOpenCVRect = new org.opencv.core.Rect(obstacleBoundingBox.left, obstacleBoundingBox.top,
                    obstacleBoundingBox.width(), obstacleBoundingBox.height());
            
            Log.d(TAG, "Demonstrated training with custom objects");
            
            // Test NLP processing
            NLPProcessor.ActionIntent intent = nlpProcessor.processNaturalLanguageCommand(
                "jump over the barrier");
            if (intent != null) {
                Log.d(TAG, "NLP parsed command: " + intent.getAction() + 
                    " with confidence: " + intent.getConfidence());
            }
        }
    }
    
    private void updateStatus(String status) {
        runOnUiThread(() -> {
            tvStatus.setText(status);
            Log.d(TAG, "Status: " + status);
        });
    }
    
    /**
     * Switch to fragment - Critical method for MoreFragment navigation
     * Fixes crash when accessing Screen Monitor or Gesture Controller from More tab
     */
    public void switchToFragment(Fragment fragment) {
        try {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
            
            // Update current fragment reference
            currentFragment = fragment;
            
            // Update bottom navigation if needed
            updateBottomNavigationForFragment(fragment);
            
            Log.d(TAG, "Switched to fragment: " + fragment.getClass().getSimpleName());
            
        } catch (Exception e) {
            Log.e(TAG, "Error switching to fragment", e);
            Toast.makeText(this, "Error loading screen", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateBottomNavigationForFragment(Fragment fragment) {
        if (bottomNavigationView == null) return;
        
        // Update bottom navigation selection based on fragment type
        if (fragment instanceof ScreenMonitorFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_screen_monitor);
        } else if (fragment instanceof GestureControllerFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_gesture_controller);
        } else if (fragment instanceof AnalyticsFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_analytics);
        } else if (fragment instanceof AutoPlayFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_autoplay);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isAutomationActive) {
            stopAutomation(null);
        }
    }
    private void setupFragmentNavigation() {
        // Add bottom navigation or tab layout
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        // REPLACE this switch statement:
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_autoplay) {
                selectedFragment = new AutoPlayFragment();
            } else if (item.getItemId() == R.id.nav_analytics) {
                selectedFragment = new AnalyticsFragment();
            } else if (item.getItemId() == R.id.nav_gesture_controller) {
                selectedFragment = new GestureControllerFragment();
            } else if (item.getItemId() == R.id.nav_screen_monitor) {
                selectedFragment = new ScreenMonitorFragment();
            } else if (item.getItemId() == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                replaceFragment(selectedFragment);
            }
            return true;
        });
    }
    
    /**
     * Setup AI Stack Manager callback for ND4J toggle feedback
     */
    private void setupAIStackCallback() {
        aiStackManager.setCallback(new AIStackManager.AIStackCallback() {
            @Override
            public void onStackEnabled(boolean success, String message) {
                runOnUiThread(() -> {
                    if (success) {
                        tvAIStatus.setText("AI: Advanced Mode (ND4J)");
                        tvAIStatus.setBackgroundColor(getColor(android.R.color.holo_green_dark));
                        updateStatus("Advanced AI enabled - Full capabilities active");
                    } else {
                        tvAIStatus.setText("AI: Failed to enable ND4J");
                        tvAIStatus.setBackgroundColor(getColor(android.R.color.holo_red_dark));
                        switchND4J.setChecked(false);
                        updateStatus("Advanced AI failed: " + message);
                    }
                });
            }
            
            @Override
            public void onStackDisabled(boolean success, String message) {
                runOnUiThread(() -> {
                    if (success) {
                        tvAIStatus.setText("AI: Lightweight Mode");
                        tvAIStatus.setBackgroundColor(getColor(android.R.color.holo_blue_dark));
                        updateStatus("Switched to lightweight AI - Battery optimized");
                    } else {
                        tvAIStatus.setText("AI: Error during disable");
                        tvAIStatus.setBackgroundColor(getColor(android.R.color.holo_orange_dark));
                        updateStatus("AI disable error: " + message);
                    }
                });
            }
            
            @Override
            public void onPerformanceUpdate(float memoryUsage, long processingTime) {
                runOnUiThread(() -> {
                    String memoryInfo = String.format("Memory: %.1f MB", memoryUsage);
                    Log.d(TAG, "AI Performance - " + memoryInfo + ", Init: " + processingTime + "ms");
                });
            }
        });
        
        // Set initial switch state based on saved preference
        switchND4J.setChecked(aiStackManager.isND4JEnabled());
        updateAIStatusDisplay();
    }
    
    /**
     * Handle ND4J toggle switch changes
     */
    private void onND4JToggleChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
        if (!buttonView.isPressed()) return; // Ignore programmatic changes
        
        Log.d(TAG, "ND4J toggle changed to: " + isChecked);
        
        // Show loading state
        tvAIStatus.setText("AI: " + (isChecked ? "Enabling..." : "Disabling..."));
        tvAIStatus.setBackgroundColor(getColor(android.R.color.darker_gray));
        
        // Disable switch during transition
        switchND4J.setEnabled(false);
        
        // Toggle AI stack in background thread
        new Thread(() -> {
            aiStackManager.toggleND4JStack(isChecked);
            
            // Re-enable switch after transition
            runOnUiThread(() -> {
                switchND4J.setEnabled(true);
            });
        }).start();
    }
    
    /**
     * Update AI status display based on current state
     */
    private void updateAIStatusDisplay() {
        AIStackManager.AIStackStatus status = aiStackManager.getStatus();
        
        if (status.nd4jEnabled && status.initialized) {
            tvAIStatus.setText("AI: Advanced Mode (ND4J)");
            tvAIStatus.setBackgroundColor(getColor(android.R.color.holo_green_dark));
        } else if (status.nd4jEnabled && !status.initialized) {
            tvAIStatus.setText("AI: ND4J Loading...");
            tvAIStatus.setBackgroundColor(getColor(android.R.color.darker_gray));
        } else {
            tvAIStatus.setText("AI: Lightweight Mode");
            tvAIStatus.setBackgroundColor(getColor(android.R.color.holo_blue_dark));
        }
        
        Log.d(TAG, "AI Status: " + String.join(", ", status.availableFeatures));
    }

}
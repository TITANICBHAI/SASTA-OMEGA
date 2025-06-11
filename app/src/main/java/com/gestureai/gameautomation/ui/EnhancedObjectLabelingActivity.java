package com.gestureai.gameautomation.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.GameObjectTemplate;
import com.gestureai.gameautomation.services.ScreenCaptureService;
import com.gestureai.gameautomation.utils.StorageHelper;
import com.gestureai.gameautomation.fragments.GestureLabelerFragment.LabeledObject;
import com.gestureai.gameautomation.GameAutomationEngine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Enhanced Object Labeling Activity with hierarchical classification support
 * Supports custom categories, types, states, and context labeling
 */
public class EnhancedObjectLabelingActivity extends AppCompatActivity {
    private static final String TAG = "EnhancedObjectLabeling";
    private static final int REQUEST_MEDIA_PROJECTION = 1;

    // UI Components
    private ImageView ivScreenshot;
    private EditText etObjectName;
    private AutoCompleteTextView actvCategory;
    private AutoCompleteTextView actvType;
    private AutoCompleteTextView actvState;
    private AutoCompleteTextView actvContext;
    private EditText etUserExplanation;
    private TextView tvLabelsCount;
    private TextView tvHierarchyPreview;
    private Button btnCapture;
    private Button btnSaveLabel;
    private Button btnExportDataset;
    private Button btnImportDataset;
    private Button btnClearAll;
    private View drawingOverlay;

    // Data Management
    private Bitmap currentScreenshot;
    private List<LabeledObject> labeledObjects;
    private int totalLabels = 0;
    private RectF currentBoundingBox;
    private Paint boundingBoxPaint;
    
    // Autocomplete Suggestions
    private Set<String> categoryHistory;
    private Set<String> typeHistory;
    private Set<String> stateHistory;
    private Set<String> contextHistory;
    
    // Backend Integration
    private GameAutomationEngine automationEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enhanced_object_labeling);
        
        initializeViews();
        initializeData();
        initializePaint();
        setupEventListeners();
        setupAutoComplete();
        loadExistingData();
        
        Log.d(TAG, "Enhanced Object Labeling Activity initialized");
    }

    private void initializeViews() {
        ivScreenshot = findViewById(R.id.iv_screenshot);
        etObjectName = findViewById(R.id.et_object_name);
        actvCategory = findViewById(R.id.actv_category);
        actvType = findViewById(R.id.actv_type);
        actvState = findViewById(R.id.actv_state);
        actvContext = findViewById(R.id.actv_context);
        etUserExplanation = findViewById(R.id.et_user_explanation);
        tvLabelsCount = findViewById(R.id.tv_labels_count);
        tvHierarchyPreview = findViewById(R.id.tv_hierarchy_preview);
        btnCapture = findViewById(R.id.btn_capture);
        btnSaveLabel = findViewById(R.id.btn_save_label);
        btnExportDataset = findViewById(R.id.btn_export_dataset);
        btnImportDataset = findViewById(R.id.btn_import_dataset);
        btnClearAll = findViewById(R.id.btn_clear_all);
        drawingOverlay = findViewById(R.id.drawing_overlay);
    }

    private void initializeData() {
        labeledObjects = new ArrayList<>();
        categoryHistory = new HashSet<>();
        typeHistory = new HashSet<>();
        stateHistory = new HashSet<>();
        contextHistory = new HashSet<>();
        automationEngine = GameAutomationEngine.getInstance();
        
        // Add common defaults
        initializeDefaultSuggestions();
    }

    private void initializeDefaultSuggestions() {
        // Categories
        categoryHistory.add("weapon");
        categoryHistory.add("enemy");
        categoryHistory.add("item");
        categoryHistory.add("ui");
        categoryHistory.add("vehicle");
        categoryHistory.add("environment");
        categoryHistory.add("consumable");
        categoryHistory.add("equipment");
        
        // Types
        typeHistory.add("assault_rifle");
        typeHistory.add("sniper");
        typeHistory.add("shotgun");
        typeHistory.add("pistol");
        typeHistory.add("player");
        typeHistory.add("button");
        typeHistory.add("health");
        typeHistory.add("ammo");
        
        // States
        stateHistory.add("equipped");
        stateHistory.add("dropped");
        stateHistory.add("available");
        stateHistory.add("damaged");
        stateHistory.add("active");
        stateHistory.add("inactive");
        stateHistory.add("moving");
        stateHistory.add("stationary");
        
        // Context
        contextHistory.add("ground");
        contextHistory.add("inventory");
        contextHistory.add("hand");
        contextHistory.add("building");
        contextHistory.add("open_area");
        contextHistory.add("cover");
        contextHistory.add("vehicle");
        contextHistory.add("water");
    }

    private void setupAutoComplete() {
        // Category autocomplete
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(categoryHistory));
        actvCategory.setAdapter(categoryAdapter);
        actvCategory.setThreshold(1);
        
        // Type autocomplete
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(typeHistory));
        actvType.setAdapter(typeAdapter);
        actvType.setThreshold(1);
        
        // State autocomplete
        ArrayAdapter<String> stateAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(stateHistory));
        actvState.setAdapter(stateAdapter);
        actvState.setThreshold(1);
        
        // Context autocomplete
        ArrayAdapter<String> contextAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(contextHistory));
        actvContext.setAdapter(contextAdapter);
        actvContext.setThreshold(1);
    }

    private void initializePaint() {
        boundingBoxPaint = new Paint();
        boundingBoxPaint.setColor(0xFF00FF00);
        boundingBoxPaint.setStyle(Paint.Style.STROKE);
        boundingBoxPaint.setStrokeWidth(3.0f);
    }

    private void setupEventListeners() {
        btnCapture.setOnClickListener(v -> captureScreen());
        btnSaveLabel.setOnClickListener(v -> saveCurrentLabel());
        btnExportDataset.setOnClickListener(v -> exportTrainingDataset());
        btnImportDataset.setOnClickListener(v -> importTrainingDataset());
        btnClearAll.setOnClickListener(v -> clearAllLabels());
        
        // Real-time hierarchy preview
        TextWatcher hierarchyWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateHierarchyPreview();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        };
        
        etObjectName.addTextChangedListener(hierarchyWatcher);
        actvCategory.addTextChangedListener(hierarchyWatcher);
        actvType.addTextChangedListener(hierarchyWatcher);
        actvState.addTextChangedListener(hierarchyWatcher);
        actvContext.addTextChangedListener(hierarchyWatcher);
        
        // Touch handling for bounding box drawing
        ivScreenshot.setOnTouchListener((v, event) -> handleImageTouch(event));
    }

    private void updateHierarchyPreview() {
        String name = etObjectName.getText().toString().trim();
        String category = actvCategory.getText().toString().trim();
        String type = actvType.getText().toString().trim();
        String state = actvState.getText().toString().trim();
        String context = actvContext.getText().toString().trim();
        
        if (name.isEmpty()) {
            tvHierarchyPreview.setText("Enter object name to see hierarchy");
            return;
        }
        
        // Build hierarchical classification
        StringBuilder hierarchy = new StringBuilder();
        if (!category.isEmpty()) hierarchy.append(category).append(" → ");
        if (!type.isEmpty()) hierarchy.append(type).append(" → ");
        hierarchy.append(name);
        if (!state.isEmpty()) hierarchy.append(" (").append(state).append(")");
        if (!context.isEmpty()) hierarchy.append(" @ ").append(context);
        
        tvHierarchyPreview.setText(hierarchy.toString());
    }

    private boolean handleImageTouch(android.view.MotionEvent event) {
        // Implementation for bounding box drawing
        switch (event.getAction()) {
            case android.view.MotionEvent.ACTION_DOWN:
                currentBoundingBox = new RectF(event.getX(), event.getY(), event.getX(), event.getY());
                return true;
                
            case android.view.MotionEvent.ACTION_MOVE:
                if (currentBoundingBox != null) {
                    currentBoundingBox.right = event.getX();
                    currentBoundingBox.bottom = event.getY();
                    // Redraw with current bounding box
                    drawBoundingBoxPreview();
                }
                return true;
                
            case android.view.MotionEvent.ACTION_UP:
                if (currentBoundingBox != null && !isValidBoundingBox()) {
                    Toast.makeText(this, "Bounding box too small. Try again.", Toast.LENGTH_SHORT).show();
                    currentBoundingBox = null;
                }
                return true;
        }
        return false;
    }

    private boolean isValidBoundingBox() {
        return currentBoundingBox != null &&
               Math.abs(currentBoundingBox.width()) > 20 &&
               Math.abs(currentBoundingBox.height()) > 20;
    }

    private void drawBoundingBoxPreview() {
        if (currentScreenshot == null || currentBoundingBox == null) return;
        
        Bitmap preview = currentScreenshot.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(preview);
        canvas.drawRect(currentBoundingBox, boundingBoxPaint);
        ivScreenshot.setImageBitmap(preview);
    }

    private void captureScreen() {
        Intent captureIntent = ((MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE))
                .createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION);
    }

    private void saveCurrentLabel() {
        if (!isValidBoundingBox()) {
            Toast.makeText(this, "Please draw a bounding box first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String name = etObjectName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter object name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String category = actvCategory.getText().toString().trim();
        String type = actvType.getText().toString().trim();
        String state = actvState.getText().toString().trim();
        String context = actvContext.getText().toString().trim();
        
        // Add to history for autocomplete
        if (!category.isEmpty()) categoryHistory.add(category);
        if (!type.isEmpty()) typeHistory.add(type);
        if (!state.isEmpty()) stateHistory.add(state);
        if (!context.isEmpty()) contextHistory.add(context);
        
        // Create labeled object
        android.graphics.Rect rect = new android.graphics.Rect(
                (int) currentBoundingBox.left,
                (int) currentBoundingBox.top,
                (int) currentBoundingBox.right,
                (int) currentBoundingBox.bottom
        );
        
        LabeledObject labeledObject = new LabeledObject(name, category, type, state, context, rect);
        labeledObjects.add(labeledObject);
        totalLabels++;
        
        // Clear inputs
        etObjectName.setText("");
        actvCategory.setText("");
        actvType.setText("");
        actvState.setText("");
        actvContext.setText("");
        currentBoundingBox = null;
        
        // Update UI
        updateStatistics();
        refreshAutoCompleteAdapters();
        
        Toast.makeText(this, "Labeled: " + labeledObject.getFullClassification(), Toast.LENGTH_SHORT).show();
        
        Log.d(TAG, "Saved object: " + labeledObject.getFullClassification());
    }

    private void refreshAutoCompleteAdapters() {
        ((ArrayAdapter<?>) actvCategory.getAdapter()).notifyDataSetChanged();
        ((ArrayAdapter<?>) actvType.getAdapter()).notifyDataSetChanged();
        ((ArrayAdapter<?>) actvState.getAdapter()).notifyDataSetChanged();
        ((ArrayAdapter<?>) actvContext.getAdapter()).notifyDataSetChanged();
    }

    private void updateStatistics() {
        tvLabelsCount.setText("Labels: " + totalLabels);
    }

    private void loadExistingData() {
        try {
            List<GameObjectTemplate> existing = StorageHelper.loadLabeledObjects(this);
            if (existing != null) {
                // Convert to new format
                for (GameObjectTemplate template : existing) {
                    android.graphics.Rect rect = new android.graphics.Rect(0, 0, 100, 100); // Placeholder
                    LabeledObject obj = new LabeledObject(
                            template.getName(),
                            template.getCategory(),
                            template.getType(),
                            template.getState(),
                            template.getContext(),
                            rect
                    );
                    labeledObjects.add(obj);
                }
                totalLabels = labeledObjects.size();
                updateStatistics();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading existing data", e);
        }
    }

    private void exportTrainingDataset() {
        // Implementation for exporting hierarchical dataset
        Toast.makeText(this, "Exporting " + totalLabels + " hierarchical labels...", Toast.LENGTH_SHORT).show();
    }

    private void importTrainingDataset() {
        // Implementation for importing hierarchical dataset
        Toast.makeText(this, "Import functionality", Toast.LENGTH_SHORT).show();
    }

    private void clearAllLabels() {
        labeledObjects.clear();
        totalLabels = 0;
        currentBoundingBox = null;
        updateStatistics();
        Toast.makeText(this, "All labels cleared", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentScreenshot != null && !currentScreenshot.isRecycled()) {
            currentScreenshot.recycle();
        }
        Log.d(TAG, "Enhanced Object Labeling Activity destroyed");
    }
}
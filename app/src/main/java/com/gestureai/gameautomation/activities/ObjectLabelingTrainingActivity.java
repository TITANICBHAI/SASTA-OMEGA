package com.gestureai.gameautomation.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.data.GameObjectTemplate;
import com.gestureai.gameautomation.services.ScreenCaptureService;
import com.gestureai.gameautomation.utils.StorageHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for training custom object detection through manual labeling
 * Allows users to capture screenshots and label game objects for ML training
 */
public class ObjectLabelingTrainingActivity extends AppCompatActivity {
    private static final String TAG = "ObjectLabelingTraining";
    private static final int REQUEST_MEDIA_PROJECTION = 1;

    private ImageView ivScreenshot;
    private EditText etObjectLabel;
    private TextView tvLabelsCount;
    private TextView tvObjectTypes;
    private Button btnCapture;
    private Button btnSaveLabel;
    private Button btnExportDataset;
    private Button btnImportDataset;
    private View drawingOverlay;

    private Bitmap currentScreenshot;
    private List<GameObjectTemplate> labeledObjects;
    private int totalLabels = 0;
    private RectF currentBoundingBox;
    private Paint boundingBoxPaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_labeling_training);
        
        initializeViews();
        initializePaint();
        setupEventListeners();
        loadExistingData();
        
        Log.d(TAG, "Object Labeling Training Activity initialized");
    }

    private void initializeViews() {
        ivScreenshot = findViewById(R.id.iv_screenshot);
        etObjectLabel = findViewById(R.id.et_object_label);
        tvLabelsCount = findViewById(R.id.tv_labels_count);
        tvObjectTypes = findViewById(R.id.tv_object_types);
        btnCapture = findViewById(R.id.btn_capture_screenshot);
        btnSaveLabel = findViewById(R.id.btn_save_label);
        btnExportDataset = findViewById(R.id.btn_export_dataset);
        btnImportDataset = findViewById(R.id.btn_import_dataset);
        drawingOverlay = findViewById(R.id.drawing_overlay);

        labeledObjects = new ArrayList<>();
    }

    private void initializePaint() {
        boundingBoxPaint = new Paint();
        boundingBoxPaint.setColor(getColor(R.color.accent_primary));
        boundingBoxPaint.setStyle(Paint.Style.STROKE);
        boundingBoxPaint.setStrokeWidth(4f);
    }

    private void setupEventListeners() {
        btnCapture.setOnClickListener(v -> requestScreenCapture());
        btnSaveLabel.setOnClickListener(v -> saveLabeledObject());
        btnExportDataset.setOnClickListener(v -> exportDataset());
        btnImportDataset.setOnClickListener(v -> importDataset());

        // Touch handling for bounding box creation
        drawingOverlay.setOnTouchListener((v, event) -> {
            handleTouchForBoundingBox(event);
            return true;
        });
    }

    private void requestScreenCapture() {
        MediaProjectionManager projectionManager = 
            (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        
        Intent captureIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK) {
            startScreenCapture(data);
        }
    }

    private void startScreenCapture(Intent data) {
        try {
            // Start screen capture service
            Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
            serviceIntent.putExtra("capture_intent", data);
            startService(serviceIntent);
            
            // Simulate screenshot capture (in real implementation, get from service)
            simulateScreenshotCapture();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting screen capture", e);
            Toast.makeText(this, "Failed to capture screen", Toast.LENGTH_SHORT).show();
        }
    }

    private void simulateScreenshotCapture() {
        // Create a sample screenshot for demonstration
        Bitmap sampleBitmap = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(sampleBitmap);
        canvas.drawColor(getColor(R.color.background_secondary));
        
        currentScreenshot = sampleBitmap;
        ivScreenshot.setImageBitmap(currentScreenshot);
        
        Toast.makeText(this, "Screenshot captured", Toast.LENGTH_SHORT).show();
    }

    private void handleTouchForBoundingBox(android.view.MotionEvent event) {
        if (currentScreenshot == null) {
            Toast.makeText(this, "Capture screenshot first", Toast.LENGTH_SHORT).show();
            return;
        }

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case android.view.MotionEvent.ACTION_DOWN:
                // Start bounding box
                currentBoundingBox = new RectF(x, y, x, y);
                break;
                
            case android.view.MotionEvent.ACTION_MOVE:
                // Update bounding box
                if (currentBoundingBox != null) {
                    currentBoundingBox.right = x;
                    currentBoundingBox.bottom = y;
                    drawBoundingBox();
                }
                break;
                
            case android.view.MotionEvent.ACTION_UP:
                // Finalize bounding box
                if (currentBoundingBox != null) {
                    normalizeRectangle();
                    Toast.makeText(this, "Bounding box created", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void drawBoundingBox() {
        if (currentBoundingBox == null || currentScreenshot == null) return;

        Bitmap overlayBitmap = currentScreenshot.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(overlayBitmap);
        canvas.drawRect(currentBoundingBox, boundingBoxPaint);
        
        ivScreenshot.setImageBitmap(overlayBitmap);
    }

    private void normalizeRectangle() {
        if (currentBoundingBox == null) return;

        float left = Math.min(currentBoundingBox.left, currentBoundingBox.right);
        float top = Math.min(currentBoundingBox.top, currentBoundingBox.bottom);
        float right = Math.max(currentBoundingBox.left, currentBoundingBox.right);
        float bottom = Math.max(currentBoundingBox.top, currentBoundingBox.bottom);

        currentBoundingBox.set(left, top, right, bottom);
    }

    private void saveLabeledObject() {
        String label = etObjectLabel.getText().toString().trim();
        
        if (label.isEmpty()) {
            Toast.makeText(this, "Enter object label", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentBoundingBox == null || currentScreenshot == null) {
            Toast.makeText(this, "Create bounding box first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create game object template
        GameObjectTemplate template = new GameObjectTemplate();
        template.setName(label);
        template.setBoundingBox(currentBoundingBox);
        template.setScreenshot(currentScreenshot);
        template.setTimestamp(System.currentTimeMillis());

        labeledObjects.add(template);
        totalLabels++;

        // Save to storage
        StorageHelper.saveLabeledObject(this, template);

        updateStatistics();
        clearCurrentLabel();
        
        Toast.makeText(this, "Object labeled successfully", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Saved labeled object: " + label);
    }

    private void exportDataset() {
        try {
            StorageHelper.exportDataset(this, labeledObjects);
            Toast.makeText(this, "Dataset exported successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error exporting dataset", e);
            Toast.makeText(this, "Failed to export dataset", Toast.LENGTH_SHORT).show();
        }
    }

    private void importDataset() {
        try {
            List<GameObjectTemplate> imported = StorageHelper.importDataset(this);
            if (imported != null) {
                labeledObjects.addAll(imported);
                totalLabels += imported.size();
                updateStatistics();
                Toast.makeText(this, "Dataset imported successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error importing dataset", e);
            Toast.makeText(this, "Failed to import dataset", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadExistingData() {
        try {
            List<GameObjectTemplate> existing = StorageHelper.loadLabeledObjects(this);
            if (existing != null) {
                labeledObjects.addAll(existing);
                totalLabels = labeledObjects.size();
                updateStatistics();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading existing data", e);
        }
    }

    private void updateStatistics() {
        tvLabelsCount.setText(String.valueOf(totalLabels));
        
        // Count unique object types
        long uniqueTypes = labeledObjects.stream()
            .map(GameObjectTemplate::getName)
            .distinct()
            .count();
        
        tvObjectTypes.setText(String.valueOf(uniqueTypes));
    }

    private void clearCurrentLabel() {
        etObjectLabel.setText("");
        currentBoundingBox = null;
        
        if (currentScreenshot != null) {
            ivScreenshot.setImageBitmap(currentScreenshot);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up resources
        if (currentScreenshot != null && !currentScreenshot.isRecycled()) {
            currentScreenshot.recycle();
        }
        
        Log.d(TAG, "Object Labeling Training Activity destroyed");
    }
}
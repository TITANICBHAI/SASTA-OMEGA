package com.gestureai.gameautomation.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.ObjectLabelerEngine;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ObjectLabelingActivity extends AppCompatActivity {
    private static final String TAG = "ObjectLabelingActivity";
    private static final int REQUEST_IMAGE_SELECT = 1001;

    private ImageView ivImage;
    private EditText etObjectName;
    private Spinner spinnerObjectType;
    private Spinner spinnerAction;
    private Button btnAddLabel;
    private Button btnSaveLabels;
    private Button btnLoadImage;
    private Button btnExportData;
    private Button btnImportData;
    private TextView tvLabelCount;
    private ListView lvLabeledObjects;

    private Bitmap currentImage;
    private ObjectLabelerEngine labelerEngine;
    private List<LabeledObject> labeledObjects;
    private boolean isDrawingBox = false;
    private float startX, startY, endX, endY;
    private Paint boxPaint;
    private LabeledObjectAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_labeling);

        initializeViews();
        initializeComponents();
        setupListeners();
        loadSampleImage();
    }

    private void initializeViews() {
        ivImage = findViewById(R.id.iv_image);
        etObjectName = findViewById(R.id.et_object_name);
        spinnerObjectType = findViewById(R.id.spinner_object_type);
        spinnerAction = findViewById(R.id.spinner_action);
        btnAddLabel = findViewById(R.id.btn_add_label);
        btnSaveLabels = findViewById(R.id.btn_save_labels);
        btnLoadImage = findViewById(R.id.btn_load_image);
        btnExportData = findViewById(R.id.btn_export_data);
        btnImportData = findViewById(R.id.btn_import_data);
        tvLabelCount = findViewById(R.id.tv_label_count);
        lvLabeledObjects = findViewById(R.id.lv_labeled_objects);

        // Setup spinners
        setupSpinners();

        // Setup paint for drawing boxes
        boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(5f);

        labeledObjects = new ArrayList<>();
        adapter = new LabeledObjectAdapter(this, labeledObjects);
        lvLabeledObjects.setAdapter(adapter);
    }

    private void setupSpinners() {
        // Object types
        String[] objectTypes = {
                "Player", "Enemy", "Coin", "Power-up", "Obstacle",
                "Weapon", "Health", "Barrier", "Button", "Text", "Custom"
        };
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, objectTypes);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerObjectType.setAdapter(typeAdapter);

        // Actions
        String[] actions = {
                "Tap", "Swipe Up", "Swipe Down", "Swipe Left", "Swipe Right",
                "Long Press", "Double Tap", "Avoid", "Collect", "Track", "Ignore"
        };
        ArrayAdapter<String> actionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, actions);
        actionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAction.setAdapter(actionAdapter);
    }

    private void initializeComponents() {
        labelerEngine = new ObjectLabelerEngine(this);
    }

    private void setupListeners() {
        btnLoadImage.setOnClickListener(v -> loadImageFromGallery());
        btnAddLabel.setOnClickListener(v -> addCurrentLabel());
        btnSaveLabels.setOnClickListener(v -> saveLabelsToFile());
        btnExportData.setOnClickListener(v -> exportTrainingData());
        btnImportData.setOnClickListener(v -> importTrainingData());

        // Touch listener for drawing bounding boxes
        ivImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleImageTouch(event);
            }
        });

        // List item click to edit/delete
        lvLabeledObjects.setOnItemClickListener((parent, view, position, id) -> {
            editLabeledObject(position);
        });

        lvLabeledObjects.setOnItemLongClickListener((parent, view, position, id) -> {
            deleteLabeledObject(position);
            return true;
        });
    }

    private boolean handleImageTouch(MotionEvent event) {
        if (currentImage == null) return false;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDrawingBox = true;
                startX = x;
                startY = y;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDrawingBox) {
                    endX = x;
                    endY = y;
                    drawBoundingBox();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (isDrawingBox) {
                    endX = x;
                    endY = y;
                    isDrawingBox = false;
                    drawBoundingBox();

                    // Enable add button if we have a box
                    btnAddLabel.setEnabled(true);
                }
                return true;
        }

        return false;
    }

    private void drawBoundingBox() {
        if (currentImage == null) return;

        Bitmap displayBitmap = currentImage.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(displayBitmap);

        // Draw existing boxes
        for (LabeledObject obj : labeledObjects) {
            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            canvas.drawRect(obj.boundingBox, paint);

            // Draw label text
            Paint textPaint = new Paint();
            textPaint.setColor(Color.GREEN);
            textPaint.setTextSize(30f);
            canvas.drawText(obj.name, obj.boundingBox.left, obj.boundingBox.top - 10, textPaint);
        }

        // Draw current box being drawn
        if (isDrawingBox || (startX != 0 && startY != 0 && endX != 0 && endY != 0)) {
            float left = Math.min(startX, endX);
            float top = Math.min(startY, endY);
            float right = Math.max(startX, endX);
            float bottom = Math.max(startY, endY);

            canvas.drawRect(left, top, right, bottom, boxPaint);
        }

        ivImage.setImageBitmap(displayBitmap);
    }

    private void addCurrentLabel() {
        String name = etObjectName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter object name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startX == 0 && startY == 0 && endX == 0 && endY == 0) {
            Toast.makeText(this, "Please draw a bounding box first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create labeled object
        LabeledObject obj = new LabeledObject();
        obj.name = name;
        obj.type = spinnerObjectType.getSelectedItem().toString();
        obj.action = spinnerAction.getSelectedItem().toString();
        obj.boundingBox = new Rect(
                (int) Math.min(startX, endX),
                (int) Math.min(startY, endY),
                (int) Math.max(startX, endX),
                (int) Math.max(startY, endY)
        );

        labeledObjects.add(obj);
        adapter.notifyDataSetChanged();

        // Update label count
        tvLabelCount.setText("Labels: " + labeledObjects.size());

        // Clear inputs
        etObjectName.setText("");
        startX = startY = endX = endY = 0;
        btnAddLabel.setEnabled(false);

        // Redraw image
        drawBoundingBox();

        // Add to labeler engine for training
        if (labelerEngine != null) {
            labelerEngine.addTrainingExample(currentImage, obj.name, obj.boundingBox, obj.action);
        }

        Toast.makeText(this, "Label added successfully", Toast.LENGTH_SHORT).show();
    }

    private void loadImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_SELECT);
    }

    private void loadSampleImage() {
        try {
            // Load sample image from assets if available
            Bitmap sample = BitmapFactory.decodeStream(getAssets().open("sample_game_screen.png"));
            if (sample != null) {
                currentImage = sample;
                ivImage.setImageBitmap(currentImage);
            }
        } catch (IOException e) {
            Log.d(TAG, "No sample image found in assets");
        }
    }

    private void saveLabelsToFile() {
        if (labeledObjects.isEmpty()) {
            Toast.makeText(this, "No labels to save", Toast.LENGTH_SHORT).show();
            return;
        }

        if (labelerEngine != null) {
            boolean success = labelerEngine.saveTrainingData();
            if (success) {
                Toast.makeText(this, "Training data saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save training data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void exportTrainingData() {
        if (labelerEngine != null) {
            String exportPath = labelerEngine.exportTrainingData();
            if (exportPath != null) {
                Toast.makeText(this, "Data exported to: " + exportPath, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void importTrainingData() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/json");
        startActivityForResult(intent, 1002);
    }

    private void editLabeledObject(int position) {
        LabeledObject obj = labeledObjects.get(position);
        etObjectName.setText(obj.name);

        // Set spinners
        ArrayAdapter<String> typeAdapter = (ArrayAdapter<String>) spinnerObjectType.getAdapter();
        int typePos = typeAdapter.getPosition(obj.type);
        if (typePos >= 0) spinnerObjectType.setSelection(typePos);

        ArrayAdapter<String> actionAdapter = (ArrayAdapter<String>) spinnerAction.getAdapter();
        int actionPos = actionAdapter.getPosition(obj.action);
        if (actionPos >= 0) spinnerAction.setSelection(actionPos);

        // Remove from list (will be re-added when user clicks add)
        labeledObjects.remove(position);
        adapter.notifyDataSetChanged();
        tvLabelCount.setText("Labels: " + labeledObjects.size());

        // Set bounding box for editing
        startX = obj.boundingBox.left;
        startY = obj.boundingBox.top;
        endX = obj.boundingBox.right;
        endY = obj.boundingBox.bottom;

        btnAddLabel.setEnabled(true);
        drawBoundingBox();
    }

    private void deleteLabeledObject(int position) {
        labeledObjects.remove(position);
        adapter.notifyDataSetChanged();
        tvLabelCount.setText("Labels: " + labeledObjects.size());
        drawBoundingBox();
        Toast.makeText(this, "Label deleted", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_SELECT && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                currentImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                ivImage.setImageBitmap(currentImage);

                // Clear existing labels
                labeledObjects.clear();
                adapter.notifyDataSetChanged();
                tvLabelCount.setText("Labels: 0");

                Toast.makeText(this, "Image loaded. Start drawing bounding boxes!", Toast.LENGTH_LONG).show();

            } catch (IOException e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 1002 && resultCode == RESULT_OK && data != null) {
            // Import training data
            Uri dataUri = data.getData();
            if (labelerEngine != null) {
                boolean success = labelerEngine.importTrainingData(dataUri.getPath());
                if (success) {
                    Toast.makeText(this, "Training data imported successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to import training data", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Data class for labeled objects
    public static class LabeledObject {
        public String name;
        public String type;
        public String action;
        public Rect boundingBox;
    }

    // Adapter for labeled objects list
    private static class LabeledObjectAdapter extends ArrayAdapter<LabeledObject> {
        public LabeledObjectAdapter(Activity context, List<LabeledObject> objects) {
            super(context, android.R.layout.simple_list_item_2, objects);
        }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            LabeledObject obj = getItem(position);
            if (obj != null) {
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                text1.setText(obj.name + " (" + obj.type + ")");
                text2.setText("Action: " + obj.action + " | Box: " +
                        obj.boundingBox.width() + "x" + obj.boundingBox.height());
            }

            return view;
        }
    }
}
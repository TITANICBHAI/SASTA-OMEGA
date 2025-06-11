package com.gestureai.gameautomation.fragments;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.ObjectLabelerEngine;
import com.gestureai.gameautomation.ObjectDetectionEngine;
import java.util.ArrayList;
import java.util.List;

public class GestureLabelerFragment extends Fragment {
    
    // Object labeling interface components
    private ImageView imageViewLabeling;
    private EditText etObjectName;
    private Button btnLoadImage;
    private Button btnSaveLabels;
    private Button btnExportDataset;
    private Button btnImportDataset;
    private RecyclerView rvLabeledObjects;
    
    // Bounding box drawing components
    private Paint boundingBoxPaint;
    private List<LabeledObject> labeledObjects;
    private LabeledObject currentObject;
    private boolean isDrawingBox = false;
    private float startX, startY, endX, endY;
    
    // Backend integration
    private ObjectLabelerEngine labelerEngine;
    private ObjectDetectionEngine detectionEngine;
    
    // Data structures
    private static class LabeledObject {
        public String name;
        public Rect boundingBox;
        public float confidence;
        
        public LabeledObject(String name, Rect box, float confidence) {
            this.name = name;
            this.boundingBox = box;
            this.confidence = confidence;
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gesture_labeler, container, false);
        
        initializeViews(view);
        setupListeners();
        setupBoundingBoxDrawing();
        initializeBackend();
        
        return view;
    }
    
    private void initializeViews(View view) {
        imageViewLabeling = view.findViewById(R.id.imageview_labeling);
        etObjectName = view.findViewById(R.id.et_object_name);
        btnLoadImage = view.findViewById(R.id.btn_load_image);
        btnSaveLabels = view.findViewById(R.id.btn_save_labels);
        btnExportDataset = view.findViewById(R.id.btn_export_dataset);
        btnImportDataset = view.findViewById(R.id.btn_import_dataset);
        rvLabeledObjects = view.findViewById(R.id.rv_labeled_objects);
        
        labeledObjects = new ArrayList<>();
        
        // Initialize bounding box paint
        boundingBoxPaint = new Paint();
        boundingBoxPaint.setColor(Color.RED);
        boundingBoxPaint.setStyle(Paint.Style.STROKE);
        boundingBoxPaint.setStrokeWidth(4.0f);
    }

    private void initializeBackend() {
        labelerEngine = new ObjectLabelerEngine(getContext());
        detectionEngine = new ObjectDetectionEngine(getContext());
    }
    
    private void setupListeners() {
        btnLoadImage.setOnClickListener(v -> loadImageForLabeling());
        btnSaveLabels.setOnClickListener(v -> saveLabeledObjects());
        btnExportDataset.setOnClickListener(v -> exportTrainingDataset());
        btnImportDataset.setOnClickListener(v -> importExistingDataset());
    }

    private void setupBoundingBoxDrawing() {
        imageViewLabeling.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    startY = event.getY();
                    isDrawingBox = true;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (isDrawingBox) {
                        endX = event.getX();
                        endY = event.getY();
                        drawBoundingBox();
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    if (isDrawingBox) {
                        endX = event.getX();
                        endY = event.getY();
                        finalizeBoundingBox();
                        isDrawingBox = false;
                    }
                    return true;
            }
            return false;
        });
    }

    private void drawBoundingBox() {
        // Create overlay canvas for real-time drawing
        Bitmap overlay = Bitmap.createBitmap(imageViewLabeling.getWidth(), imageViewLabeling.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);
        
        // Draw current bounding box
        canvas.drawRect(startX, startY, endX, endY, boundingBoxPaint);
        
        // Draw existing boxes
        for (LabeledObject obj : labeledObjects) {
            canvas.drawRect(obj.boundingBox, boundingBoxPaint);
        }
        
        imageViewLabeling.setImageBitmap(overlay);
    }

    private void finalizeBoundingBox() {
        String objectName = etObjectName.getText().toString().trim();
        if (objectName.isEmpty()) {
            Toast.makeText(getContext(), "Please enter object name", Toast.LENGTH_SHORT).show();
            return;
        }

        Rect boundingBox = new Rect((int)startX, (int)startY, (int)endX, (int)endY);
        LabeledObject newObject = new LabeledObject(objectName, boundingBox, 1.0f);
        labeledObjects.add(newObject);
        
        // Clear object name for next labeling
        etObjectName.setText("");
        
        Toast.makeText(getContext(), "Object labeled: " + objectName, Toast.LENGTH_SHORT).show();
    }

    private void loadImageForLabeling() {
        // Connect to GameAutomationEngine for screen capture
        if (labelerEngine != null) {
            labelerEngine.captureScreenForLabeling(new ObjectLabelerEngine.CaptureCallback() {
                @Override
                public void onCaptureComplete(Bitmap screenshot) {
                    getActivity().runOnUiThread(() -> {
                        imageViewLabeling.setImageBitmap(screenshot);
                        labeledObjects.clear(); // Reset for new image
                    });
                }

                @Override
                public void onCaptureError(String error) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Screen capture failed: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void saveLabeledObjects() {
        if (labeledObjects.isEmpty()) {
            Toast.makeText(getContext(), "No objects to save", Toast.LENGTH_SHORT).show();
            return;
        }

        if (labelerEngine != null) {
            labelerEngine.saveLabeledObjects(labeledObjects, new ObjectLabelerEngine.SaveCallback() {
                @Override
                public void onSaveComplete(int savedCount) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), savedCount + " objects saved successfully", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onSaveError(String error) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Save failed: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void exportTrainingDataset() {
        if (labelerEngine != null) {
            labelerEngine.exportTrainingDataset(new ObjectLabelerEngine.ExportCallback() {
                @Override
                public void onExportComplete(String filePath) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Dataset exported to: " + filePath, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onExportError(String error) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Export failed: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void importExistingDataset() {
        if (labelerEngine != null) {
            labelerEngine.importTrainingDataset(new ObjectLabelerEngine.ImportCallback() {
                @Override
                public void onImportComplete(List<LabeledObject> importedObjects) {
                    getActivity().runOnUiThread(() -> {
                        labeledObjects.addAll(importedObjects);
                        Toast.makeText(getContext(), importedObjects.size() + " objects imported", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onImportError(String error) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Import failed: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
                startRecording();
            } else {
                stopRecording();
            }
        });
        
        btnSaveGesture.setOnClickListener(v -> {
            saveCurrentGesture();
        });
        
        gestureRecordingArea.setOnTouchListener((v, event) -> {
            if (isRecording) {
                // Handle touch events for gesture recording
                // This would capture the gesture path
                return true;
            }
            return false;
        });
    }
    
    private void setupRecyclerView() {
        gesturesAdapter = new SavedGesturesAdapter(savedGestures);
        rvSavedGestures.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSavedGestures.setAdapter(gesturesAdapter);
    }
    
    private void startRecording() {
        isRecording = true;
        btnRecordGesture.setText("Stop Recording");
        btnSaveGesture.setEnabled(false);
        gestureRecordingArea.setBackgroundColor(0xFFFFE0E0); // Light red background
    }
    
    private void stopRecording() {
        isRecording = false;
        btnRecordGesture.setText(R.string.record);
        btnSaveGesture.setEnabled(true);
        gestureRecordingArea.setBackgroundColor(0xFFF0F0F0); // Original background
    }
    
    private void saveCurrentGesture() {
        String gestureLabel = etGestureLabel.getText().toString().trim();
        if (!gestureLabel.isEmpty()) {
            savedGestures.add(gestureLabel);
            gesturesAdapter.notifyItemInserted(savedGestures.size() - 1);
            etGestureLabel.setText("");
            btnSaveGesture.setEnabled(false);
        }
    }
    
    // Simple adapter for saved gestures list
    private static class SavedGesturesAdapter extends RecyclerView.Adapter<SavedGesturesAdapter.ViewHolder> {
        private List<String> gestures;
        
        public SavedGesturesAdapter(List<String> gestures) {
            this.gestures = gestures;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.textView.setText(gestures.get(position));
        }
        
        @Override
        public int getItemCount() {
            return gestures.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView textView;
            
            ViewHolder(View view) {
                super(view);
                textView = view.findViewById(android.R.id.text1);
            }
        }
    }
}
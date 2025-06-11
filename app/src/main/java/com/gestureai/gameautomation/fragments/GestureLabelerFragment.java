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
    
    // Gesture recording components
    private Button btnRecordGesture;
    private Button btnSaveGesture;
    private EditText etGestureLabel;
    private View gestureRecordingArea;
    private RecyclerView rvSavedGestures;
    
    // Bounding box drawing components
    private Paint boundingBoxPaint;
    private List<LabeledObject> labeledObjects;
    private LabeledObject currentObject;
    private boolean isDrawingBox = false;
    private float startX, startY, endX, endY;
    
    // Gesture recording state
    private boolean isRecording = false;
    private List<String> savedGestures;
    private SavedGesturesAdapter gesturesAdapter;
    
    // Backend integration
    private ObjectLabelerEngine labelerEngine;
    private ObjectDetectionEngine detectionEngine;
    
    // Enhanced data structures with hierarchical classification
    public static class LabeledObject {
        public String name;
        public String category;
        public String type;
        public String state;
        public String context;
        public Rect boundingBox;
        public float confidence;
        
        // Enhanced constructor
        public LabeledObject(String name, String category, String type, String state, 
                           String context, Rect boundingBox) {
            this.name = name;
            this.category = category != null ? category : "unknown";
            this.type = type != null ? type : "default";
            this.state = state != null ? state : "normal";
            this.context = context != null ? context : "general";
            this.boundingBox = boundingBox;
            this.confidence = 1.0f;
        }
        
        // Backward compatibility constructor
        public LabeledObject(String name, Rect boundingBox) {
            this(name, "unknown", "default", "normal", "general", boundingBox);
        }
        
        // Hierarchical classification methods
        public String getFullClassification() {
            return category + "_" + type + "_" + name + "_" + state + "_" + context;
        }
        
        public boolean matchesCategory(String categoryPattern) {
            return category.toLowerCase().contains(categoryPattern.toLowerCase());
        }
        
        public boolean matchesHierarchy(String categoryPattern, String typePattern) {
            return matchesCategory(categoryPattern) && 
                   type.toLowerCase().contains(typePattern.toLowerCase());
        }
        
        public String getLabel() { return name; }
        public String getCategory() { return category; }
        public String getType() { return type; }
        public String getState() { return state; }
        public String getContext() { return context; }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gesture_labeler, container, false);
        
        initializeViews(view);
        initializeData();
        setupClickListeners();
        setupRecyclerView();
        setupGestureRecording();
        
        return view;
    }
    
    private void initializeViews(View view) {
        imageViewLabeling = view.findViewById(R.id.iv_labeling_image);
        etObjectName = view.findViewById(R.id.et_object_name);
        btnLoadImage = view.findViewById(R.id.btn_load_image);
        btnSaveLabels = view.findViewById(R.id.btn_save_labels);
        btnExportDataset = view.findViewById(R.id.btn_export_dataset);
        btnImportDataset = view.findViewById(R.id.btn_import_dataset);
        rvLabeledObjects = view.findViewById(R.id.rv_labeled_objects);
        
        btnRecordGesture = view.findViewById(R.id.btn_record_gesture);
        btnSaveGesture = view.findViewById(R.id.btn_save_gesture);
        etGestureLabel = view.findViewById(R.id.et_gesture_label);
        gestureRecordingArea = view.findViewById(R.id.gesture_recording_area);
        rvSavedGestures = view.findViewById(R.id.rv_saved_gestures);
    }
    
    private void initializeData() {
        labeledObjects = new ArrayList<>();
        savedGestures = new ArrayList<>();
        
        // Initialize paint for bounding boxes
        boundingBoxPaint = new Paint();
        boundingBoxPaint.setColor(Color.RED);
        boundingBoxPaint.setStyle(Paint.Style.STROKE);
        boundingBoxPaint.setStrokeWidth(3);
        
        // Initialize engines
        if (getContext() != null) {
            labelerEngine = new ObjectLabelerEngine(getContext());
            detectionEngine = new ObjectDetectionEngine(getContext());
        }
    }
    
    private void setupClickListeners() {
        if (btnLoadImage != null) {
            btnLoadImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadImageForLabeling();
                }
            });
        }
        
        if (btnSaveLabels != null) {
            btnSaveLabels.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveCurrentLabels();
                }
            });
        }
        
        if (btnExportDataset != null) {
            btnExportDataset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exportTrainingDataset();
                }
            });
        }
        
        if (btnImportDataset != null) {
            btnImportDataset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    importExistingDataset();
                }
            });
        }
        
        if (imageViewLabeling != null) {
            imageViewLabeling.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return handleImageTouch(event);
                }
            });
        }
    }
    
    private boolean handleImageTouch(MotionEvent event) {
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
                    // Redraw bounding box preview
                    invalidateImageView();
                }
                return true;
                
            case MotionEvent.ACTION_UP:
                if (isDrawingBox) {
                    endX = event.getX();
                    endY = event.getY();
                    isDrawingBox = false;
                    
                    // Create bounding box
                    String objectName = etObjectName.getText().toString().trim();
                    if (!objectName.isEmpty()) {
                        createBoundingBox(objectName);
                    }
                }
                return true;
        }
        return false;
    }
    
    private void createBoundingBox(String objectName) {
        int left = (int) Math.min(startX, endX);
        int top = (int) Math.min(startY, endY);
        int right = (int) Math.max(startX, endX);
        int bottom = (int) Math.max(startY, endY);
        
        Rect boundingBox = new Rect(left, top, right, bottom);
        
        // Create enhanced labeled object with hierarchical classification
        // For now, use default values - UI can be enhanced to capture these
        LabeledObject labeledObject = new LabeledObject(objectName, "unknown", "default", "normal", "general", boundingBox);
        labeledObjects.add(labeledObject);
        
        // Clear the input field
        etObjectName.setText("");
        
        Toast.makeText(getContext(), "Object labeled: " + labeledObject.getFullClassification(), Toast.LENGTH_SHORT).show();
    }
    
    private void invalidateImageView() {
        if (imageViewLabeling != null) {
            imageViewLabeling.invalidate();
        }
    }
    
    private void loadImageForLabeling() {
        // This would typically open a file picker or capture from screen
        Toast.makeText(getContext(), "Image loading functionality", Toast.LENGTH_SHORT).show();
    }
    
    private void saveCurrentLabels() {
        if (labelerEngine != null && !labeledObjects.isEmpty()) {
            labelerEngine.saveTrainingLabels(labeledObjects, new ObjectLabelerEngine.SaveCallback() {
                @Override
                public void onSaveComplete() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), "Labels saved successfully", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onSaveError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), "Save failed: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        }
    }

    private void exportTrainingDataset() {
        if (labelerEngine != null) {
            labelerEngine.exportTrainingDataset(new ObjectLabelerEngine.ExportCallback() {
                @Override
                public void onExportComplete(String filePath) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), "Dataset exported to: " + filePath, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }

                @Override
                public void onExportError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), "Export failed: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        }
    }

    private void importExistingDataset() {
        if (labelerEngine != null) {
            labelerEngine.importTrainingDataset(new ObjectLabelerEngine.ImportCallback() {
                @Override
                public void onImportComplete(List<LabeledObject> importedObjects) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                labeledObjects.addAll(importedObjects);
                                Toast.makeText(getContext(), importedObjects.size() + " objects imported", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onImportError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), "Import failed: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        }
    }
    
    private void setupGestureRecording() {
        if (btnRecordGesture != null) {
            btnRecordGesture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isRecording) {
                        startRecording();
                    } else {
                        stopRecording();
                    }
                }
            });
        }
        
        if (btnSaveGesture != null) {
            btnSaveGesture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveCurrentGesture();
                }
            });
        }
        
        if (gestureRecordingArea != null) {
            gestureRecordingArea.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (isRecording) {
                        // Handle touch events for gesture recording
                        return true;
                    }
                    return false;
                }
            });
        }
    }
    
    private void setupRecyclerView() {
        if (rvSavedGestures != null) {
            gesturesAdapter = new SavedGesturesAdapter(savedGestures);
            rvSavedGestures.setLayoutManager(new LinearLayoutManager(getContext()));
            rvSavedGestures.setAdapter(gesturesAdapter);
        }
    }
    
    private void startRecording() {
        isRecording = true;
        if (btnRecordGesture != null) {
            btnRecordGesture.setText("Stop Recording");
        }
        if (btnSaveGesture != null) {
            btnSaveGesture.setEnabled(false);
        }
        if (gestureRecordingArea != null) {
            gestureRecordingArea.setBackgroundColor(0xFFFFE0E0); // Light red background
        }
    }
    
    private void stopRecording() {
        isRecording = false;
        if (btnRecordGesture != null) {
            btnRecordGesture.setText("Record Gesture");
        }
        if (btnSaveGesture != null) {
            btnSaveGesture.setEnabled(true);
        }
        if (gestureRecordingArea != null) {
            gestureRecordingArea.setBackgroundColor(0xFFF0F0F0); // Original background
        }
    }
    
    private void saveCurrentGesture() {
        if (etGestureLabel != null) {
            String gestureLabel = etGestureLabel.getText().toString().trim();
            if (!gestureLabel.isEmpty()) {
                savedGestures.add(gestureLabel);
                if (gesturesAdapter != null) {
                    gesturesAdapter.notifyItemInserted(savedGestures.size() - 1);
                }
                etGestureLabel.setText("");
                if (btnSaveGesture != null) {
                    btnSaveGesture.setEnabled(false);
                }
            }
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
            holder.bind(gestures.get(position));
        }
        
        @Override
        public int getItemCount() {
            return gestures.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View itemView) {
                super(itemView);
            }
            
            void bind(String gesture) {
                ((android.widget.TextView) itemView).setText(gesture);
            }
        }
    }
}
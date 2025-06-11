package com.gestureai.gameautomation.activities;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.GestureAction;
import com.gestureai.gameautomation.services.TouchAutomationService;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class GestureSequenceBuilderActivity extends AppCompatActivity {
    private static final String TAG = "GestureSequenceBuilder";
    
    // Gesture sequence components
    private RecyclerView rvGestureSequence;
    private GestureSequenceAdapter sequenceAdapter;
    private List<GestureSequenceItem> gestureSequence;
    
    // Gesture library
    private RecyclerView rvGestureLibrary;
    private GestureLibraryAdapter libraryAdapter;
    private List<GestureTemplate> gestureLibrary;
    
    // Controls
    private Button btnAddGesture;
    private Button btnPlaySequence;
    private Button btnSaveSequence;
    private Button btnLoadSequence;
    private Button btnClearSequence;
    private EditText etSequenceName;
    private TextView tvSequenceDuration;
    
    // Timing controls
    private SeekBar sbGlobalDelay;
    private TextView tvGlobalDelay;
    private Switch swLoopSequence;
    private EditText etLoopCount;
    
    // Touch automation service
    private TouchAutomationService touchService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture_sequence_builder);
        
        initializeViews();
        setupGestureSequence();
        setupGestureLibrary();
        setupControls();
    }
    
    private void initializeViews() {
        // Gesture sequence
        rvGestureSequence = findViewById(R.id.rv_gesture_sequence);
        rvGestureSequence.setLayoutManager(new LinearLayoutManager(this));
        
        // Gesture library
        rvGestureLibrary = findViewById(R.id.rv_gesture_library);
        rvGestureLibrary.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        
        // Controls
        btnAddGesture = findViewById(R.id.btn_add_gesture);
        btnPlaySequence = findViewById(R.id.btn_play_sequence);
        btnSaveSequence = findViewById(R.id.btn_save_sequence);
        btnLoadSequence = findViewById(R.id.btn_load_sequence);
        btnClearSequence = findViewById(R.id.btn_clear_sequence);
        etSequenceName = findViewById(R.id.et_sequence_name);
        tvSequenceDuration = findViewById(R.id.tv_sequence_duration);
        
        // Timing controls
        sbGlobalDelay = findViewById(R.id.sb_global_delay);
        tvGlobalDelay = findViewById(R.id.tv_global_delay);
        swLoopSequence = findViewById(R.id.sw_loop_sequence);
        etLoopCount = findViewById(R.id.et_loop_count);
        
        setupButtonListeners();
        setupTimingControls();
    }
    
    private void setupButtonListeners() {
        btnAddGesture.setOnClickListener(v -> showGestureSelector());
        btnPlaySequence.setOnClickListener(v -> playGestureSequence());
        btnSaveSequence.setOnClickListener(v -> saveGestureSequence());
        btnLoadSequence.setOnClickListener(v -> loadGestureSequence());
        btnClearSequence.setOnClickListener(v -> clearGestureSequence());
    }
    
    private void setupTimingControls() {
        sbGlobalDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int delay = progress * 10; // 0-1000ms
                tvGlobalDelay.setText(delay + "ms");
                updateSequenceDuration();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        swLoopSequence.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etLoopCount.setEnabled(isChecked);
            updateSequenceDuration();
        });
    }
    
    private void setupGestureSequence() {
        gestureSequence = new ArrayList<>();
        sequenceAdapter = new GestureSequenceAdapter(gestureSequence);
        rvGestureSequence.setAdapter(sequenceAdapter);
        
        // Enable drag and drop for reordering
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new GestureSequenceItemTouchCallback());
        itemTouchHelper.attachToRecyclerView(rvGestureSequence);
    }
    
    private void setupGestureLibrary() {
        gestureLibrary = createGestureLibrary();
        libraryAdapter = new GestureLibraryAdapter(gestureLibrary);
        rvGestureLibrary.setAdapter(libraryAdapter);
    }
    
    private void setupControls() {
        touchService = TouchAutomationService.getInstance();
        etSequenceName.setText("Custom Sequence " + System.currentTimeMillis() % 1000);
        updateSequenceDuration();
    }
    
    private List<GestureTemplate> createGestureLibrary() {
        List<GestureTemplate> library = new ArrayList<>();
        
        library.add(new GestureTemplate("Tap", "TAP", R.drawable.ic_gesture_tap));
        library.add(new GestureTemplate("Swipe Up", "SWIPE_UP", R.drawable.ic_gesture_swipe_up));
        library.add(new GestureTemplate("Swipe Down", "SWIPE_DOWN", R.drawable.ic_gesture_swipe_down));
        library.add(new GestureTemplate("Swipe Left", "SWIPE_LEFT", R.drawable.ic_gesture_swipe_left));
        library.add(new GestureTemplate("Swipe Right", "SWIPE_RIGHT", R.drawable.ic_gesture_swipe_right));
        library.add(new GestureTemplate("Long Press", "LONG_PRESS", R.drawable.ic_gesture_long_press));
        library.add(new GestureTemplate("Double Tap", "DOUBLE_TAP", R.drawable.ic_gesture_double_tap));
        library.add(new GestureTemplate("Pinch", "PINCH", R.drawable.ic_gesture_pinch));
        library.add(new GestureTemplate("Wait", "WAIT", R.drawable.ic_gesture_wait));
        
        return library;
    }
    
    private void showGestureSelector() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Gesture Type");
        
        String[] gestureNames = gestureLibrary.stream()
            .map(template -> template.name)
            .toArray(String[]::new);
        
        builder.setItems(gestureNames, (dialog, which) -> {
            GestureTemplate template = gestureLibrary.get(which);
            showGestureConfigDialog(template);
        });
        
        builder.show();
    }
    
    private void showGestureConfigDialog(GestureTemplate template) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Configure " + template.name);
        
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_gesture_config, null);
        builder.setView(dialogView);
        
        EditText etX = dialogView.findViewById(R.id.et_x_coordinate);
        EditText etY = dialogView.findViewById(R.id.et_y_coordinate);
        EditText etDuration = dialogView.findViewById(R.id.et_duration);
        EditText etDelay = dialogView.findViewById(R.id.et_delay);
        
        // Set default values
        etX.setText("540"); // Center X
        etY.setText("960"); // Center Y
        etDuration.setText("300");
        etDelay.setText("500");
        
        builder.setPositiveButton("Add", (dialog, which) -> {
            try {
                int x = Integer.parseInt(etX.getText().toString());
                int y = Integer.parseInt(etY.getText().toString());
                int duration = Integer.parseInt(etDuration.getText().toString());
                int delay = Integer.parseInt(etDelay.getText().toString());
                
                GestureSequenceItem item = new GestureSequenceItem(
                    template.type, template.name, x, y, duration, delay);
                
                gestureSequence.add(item);
                sequenceAdapter.notifyItemInserted(gestureSequence.size() - 1);
                updateSequenceDuration();
                
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid input values", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void playGestureSequence() {
        if (gestureSequence.isEmpty()) {
            Toast.makeText(this, "Sequence is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (touchService == null) {
            Toast.makeText(this, "Touch service not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnPlaySequence.setEnabled(false);
        btnPlaySequence.setText("Playing...");
        
        new Thread(() -> {
            try {
                int loopCount = swLoopSequence.isChecked() ? 
                    Integer.parseInt(etLoopCount.getText().toString()) : 1;
                
                for (int loop = 0; loop < loopCount; loop++) {
                    for (GestureSequenceItem item : gestureSequence) {
                        executeGestureItem(item);
                        
                        // Add global delay
                        int globalDelay = sbGlobalDelay.getProgress() * 10;
                        Thread.sleep(item.delay + globalDelay);
                    }
                }
                
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error playing sequence", e);
            }
            
            runOnUiThread(() -> {
                btnPlaySequence.setEnabled(true);
                btnPlaySequence.setText("Play Sequence");
                Toast.makeText(this, "Sequence completed", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
    
    private void executeGestureItem(GestureSequenceItem item) {
        switch (item.type) {
            case "TAP":
                touchService.performTap(item.x, item.y);
                break;
            case "SWIPE_UP":
                touchService.performSwipe(item.x, item.y, item.x, item.y - 200, item.duration);
                break;
            case "SWIPE_DOWN":
                touchService.performSwipe(item.x, item.y, item.x, item.y + 200, item.duration);
                break;
            case "SWIPE_LEFT":
                touchService.performSwipe(item.x, item.y, item.x - 200, item.y, item.duration);
                break;
            case "SWIPE_RIGHT":
                touchService.performSwipe(item.x, item.y, item.x + 200, item.y, item.duration);
                break;
            case "LONG_PRESS":
                touchService.executeLongPress(item.x, item.y, item.duration);
                break;
            case "DOUBLE_TAP":
                touchService.executeDoubleTap(item.x, item.y);
                break;
            case "WAIT":
                // Wait is handled by delay
                break;
        }
    }
    
    private void saveGestureSequence() {
        String sequenceName = etSequenceName.getText().toString().trim();
        if (sequenceName.isEmpty()) {
            Toast.makeText(this, "Please enter a sequence name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Save to shared preferences
        android.content.SharedPreferences prefs = getSharedPreferences("gesture_sequences", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        
        String sequenceJson = serializeSequence(gestureSequence);
        editor.putString("sequence_" + sequenceName, sequenceJson);
        editor.apply();
        
        Toast.makeText(this, "Sequence saved: " + sequenceName, Toast.LENGTH_SHORT).show();
    }
    
    private void loadGestureSequence() {
        android.content.SharedPreferences prefs = getSharedPreferences("gesture_sequences", MODE_PRIVATE);
        java.util.Map<String, ?> sequences = prefs.getAll();
        
        if (sequences.isEmpty()) {
            Toast.makeText(this, "No saved sequences found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] sequenceNames = sequences.keySet().stream()
            .map(key -> key.replace("sequence_", ""))
            .toArray(String[]::new);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Load Sequence");
        builder.setItems(sequenceNames, (dialog, which) -> {
            String selectedName = sequenceNames[which];
            String sequenceJson = prefs.getString("sequence_" + selectedName, "");
            
            List<GestureSequenceItem> loadedSequence = deserializeSequence(sequenceJson);
            if (!loadedSequence.isEmpty()) {
                gestureSequence.clear();
                gestureSequence.addAll(loadedSequence);
                sequenceAdapter.notifyDataSetChanged();
                etSequenceName.setText(selectedName);
                updateSequenceDuration();
                
                Toast.makeText(this, "Sequence loaded: " + selectedName, Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.show();
    }
    
    private void clearGestureSequence() {
        gestureSequence.clear();
        sequenceAdapter.notifyDataSetChanged();
        updateSequenceDuration();
        Toast.makeText(this, "Sequence cleared", Toast.LENGTH_SHORT).show();
    }
    
    private void updateSequenceDuration() {
        long totalDuration = 0;
        int globalDelay = sbGlobalDelay.getProgress() * 10;
        
        for (GestureSequenceItem item : gestureSequence) {
            totalDuration += item.duration + item.delay + globalDelay;
        }
        
        if (swLoopSequence.isChecked()) {
            try {
                int loopCount = Integer.parseInt(etLoopCount.getText().toString());
                totalDuration *= loopCount;
            } catch (NumberFormatException e) {
                // Use single loop if invalid
            }
        }
        
        tvSequenceDuration.setText(String.format("Duration: %.1fs", totalDuration / 1000.0));
    }
    
    private String serializeSequence(List<GestureSequenceItem> sequence) {
        // Simple JSON serialization
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < sequence.size(); i++) {
            GestureSequenceItem item = sequence.get(i);
            json.append(String.format(
                "{\"type\":\"%s\",\"name\":\"%s\",\"x\":%d,\"y\":%d,\"duration\":%d,\"delay\":%d}",
                item.type, item.name, item.x, item.y, item.duration, item.delay));
            if (i < sequence.size() - 1) json.append(",");
        }
        json.append("]");
        return json.toString();
    }
    
    private List<GestureSequenceItem> deserializeSequence(String json) {
        // Simple JSON deserialization - in production use proper JSON library
        List<GestureSequenceItem> sequence = new ArrayList<>();
        // For now, return empty list - implement proper JSON parsing as needed
        return sequence;
    }
    
    // Data classes
    public static class GestureTemplate {
        public String name;
        public String type;
        public int iconRes;
        
        public GestureTemplate(String name, String type, int iconRes) {
            this.name = name;
            this.type = type;
            this.iconRes = iconRes;
        }
    }
    
    public static class GestureSequenceItem {
        public String type;
        public String name;
        public int x, y;
        public int duration;
        public int delay;
        
        public GestureSequenceItem(String type, String name, int x, int y, int duration, int delay) {
            this.type = type;
            this.name = name;
            this.x = x;
            this.y = y;
            this.duration = duration;
            this.delay = delay;
        }
    }
    
    // RecyclerView adapters
    private class GestureSequenceAdapter extends RecyclerView.Adapter<GestureSequenceAdapter.ViewHolder> {
        private List<GestureSequenceItem> data;
        
        public GestureSequenceAdapter(List<GestureSequenceItem> data) {
            this.data = data;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gesture_sequence, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            GestureSequenceItem item = data.get(position);
            
            holder.tvGestureName.setText(item.name);
            holder.tvCoordinates.setText(String.format("(%d, %d)", item.x, item.y));
            holder.tvDuration.setText(item.duration + "ms");
            holder.tvDelay.setText(item.delay + "ms");
            
            holder.btnRemove.setOnClickListener(v -> {
                data.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, data.size());
                updateSequenceDuration();
            });
        }
        
        @Override
        public int getItemCount() {
            return data.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvGestureName, tvCoordinates, tvDuration, tvDelay;
            Button btnRemove;
            
            public ViewHolder(android.view.View itemView) {
                super(itemView);
                tvGestureName = itemView.findViewById(R.id.tv_gesture_name);
                tvCoordinates = itemView.findViewById(R.id.tv_coordinates);
                tvDuration = itemView.findViewById(R.id.tv_duration);
                tvDelay = itemView.findViewById(R.id.tv_delay);
                btnRemove = itemView.findViewById(R.id.btn_remove);
            }
        }
    }
    
    private class GestureLibraryAdapter extends RecyclerView.Adapter<GestureLibraryAdapter.ViewHolder> {
        private List<GestureTemplate> data;
        
        public GestureLibraryAdapter(List<GestureTemplate> data) {
            this.data = data;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gesture_template, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            GestureTemplate template = data.get(position);
            
            holder.tvGestureName.setText(template.name);
            holder.ivGestureIcon.setImageResource(template.iconRes);
            
            holder.itemView.setOnClickListener(v -> showGestureConfigDialog(template));
        }
        
        @Override
        public int getItemCount() {
            return data.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvGestureName;
            ImageView ivGestureIcon;
            
            public ViewHolder(android.view.View itemView) {
                super(itemView);
                tvGestureName = itemView.findViewById(R.id.tv_gesture_name);
                ivGestureIcon = itemView.findViewById(R.id.iv_gesture_icon);
            }
        }
    }
    
    // ItemTouchHelper for drag and drop
    private class GestureSequenceItemTouchCallback extends ItemTouchHelper.SimpleCallback {
        
        public GestureSequenceItemTouchCallback() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }
        
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, 
                             RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();
            
            Collections.swap(gestureSequence, fromPosition, toPosition);
            sequenceAdapter.notifyItemMoved(fromPosition, toPosition);
            
            return true;
        }
        
        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            gestureSequence.remove(position);
            sequenceAdapter.notifyItemRemoved(position);
            updateSequenceDuration();
        }
    }
}
package com.gestureai.gameautomation.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.activities.*;
import java.util.ArrayList;
import java.util.List;

public class ToolsFragment extends Fragment {
    private static final String TAG = "ToolsFragment";
    
    private RecyclerView rvToolsGrid;
    private ToolsAdapter toolsAdapter;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tools, container, false);
        
        initializeViews(view);
        setupToolsGrid();
        
        return view;
    }
    
    private void initializeViews(View view) {
        rvToolsGrid = view.findViewById(R.id.rv_tools_grid);
        rvToolsGrid.setLayoutManager(new GridLayoutManager(getContext(), 2));
    }
    
    private void setupToolsGrid() {
        List<ToolItem> tools = createToolsList();
        toolsAdapter = new ToolsAdapter(tools, this::onToolClick);
        rvToolsGrid.setAdapter(toolsAdapter);
    }
    
    private List<ToolItem> createToolsList() {
        List<ToolItem> tools = new ArrayList<>();
        
        tools.add(new ToolItem("Object Labeling", "Train object detection", 
            ObjectLabelingActivity.class));
        tools.add(new ToolItem("Gesture Training", "Train gesture recognition", 
            GestureTrainingActivity.class));
        tools.add(new ToolItem("Debug Tools", "Advanced debugging", 
            AdvancedDebugToolsActivity.class));
        tools.add(new ToolItem("Performance Monitor", "System performance", 
            PerformanceMonitoringDashboardActivity.class));
        tools.add(new ToolItem("AI Training", "Neural network training", 
            AITrainingDashboardActivity.class));
        tools.add(new ToolItem("Strategy Config", "Game strategy setup", 
            GameStrategyConfigActivity.class));
        tools.add(new ToolItem("Voice Commands", "Voice control setup", 
            VoiceCommandConfigurationActivity.class));
        tools.add(new ToolItem("Session Analytics", "Session analysis", 
            SessionAnalyticsDashboardActivity.class));
        
        return tools;
    }
    
    private void onToolClick(ToolItem tool) {
        Intent intent = new Intent(getActivity(), tool.getActivityClass());
        startActivity(intent);
    }
    
    // Helper classes
    public static class ToolItem {
        private String title;
        private String description;
        private Class<?> activityClass;
        
        public ToolItem(String title, String description, Class<?> activityClass) {
            this.title = title;
            this.description = description;
            this.activityClass = activityClass;
        }
        
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public Class<?> getActivityClass() { return activityClass; }
    }
    
    // Simple adapter for tools grid
    private static class ToolsAdapter extends RecyclerView.Adapter<ToolsAdapter.ViewHolder> {
        private List<ToolItem> tools;
        private OnToolClickListener listener;
        
        public interface OnToolClickListener {
            void onToolClick(ToolItem tool);
        }
        
        public ToolsAdapter(List<ToolItem> tools, OnToolClickListener listener) {
            this.tools = tools;
            this.listener = listener;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tool_card, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ToolItem tool = tools.get(position);
            holder.bind(tool, listener);
        }
        
        @Override
        public int getItemCount() {
            return tools.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            private CardView cardView;
            private Button btnTool;
            
            public ViewHolder(View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.card_tool);
                btnTool = itemView.findViewById(R.id.btn_tool);
            }
            
            public void bind(ToolItem tool, OnToolClickListener listener) {
                btnTool.setText(tool.getTitle());
                cardView.setOnClickListener(v -> listener.onToolClick(tool));
            }
        }
    }
}
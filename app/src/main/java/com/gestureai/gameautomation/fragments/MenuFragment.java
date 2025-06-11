package com.gestureai.gameautomation.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.activities.*;
import java.util.ArrayList;
import java.util.List;

public class MenuFragment extends Fragment {
    private static final String TAG = "MenuFragment";
    
    private RecyclerView rvMenuItems;
    private MenuAdapter menuAdapter;
    private TextView tvAppVersion;
    private Button btnSettings;
    private Button btnAbout;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        
        initializeViews(view);
        setupMenuItems();
        setupButtonListeners();
        
        return view;
    }
    
    private void initializeViews(View view) {
        rvMenuItems = view.findViewById(R.id.rv_menu_items);
        tvAppVersion = view.findViewById(R.id.tv_app_version);
        btnSettings = view.findViewById(R.id.btn_settings);
        btnAbout = view.findViewById(R.id.btn_about);
        
        rvMenuItems.setLayoutManager(new LinearLayoutManager(getContext()));
        tvAppVersion.setText("Version 1.0.0");
    }
    
    private void setupMenuItems() {
        List<MenuItem> menuItems = createMenuItems();
        menuAdapter = new MenuAdapter(menuItems, this::onMenuItemClick);
        rvMenuItems.setAdapter(menuAdapter);
    }
    
    private List<MenuItem> createMenuItems() {
        List<MenuItem> items = new ArrayList<>();
        
        items.add(new MenuItem("Performance Monitoring", "Real-time system metrics", 
            PerformanceMonitoringDashboardActivity.class));
        items.add(new MenuItem("AI Model Management", "Manage neural networks", 
            AIModelManagementActivity.class));
        items.add(new MenuItem("Training Visualizer", "View training progress", 
            TrainingProgressVisualizerActivity.class));
        items.add(new MenuItem("Strategy Wizard", "Configure game strategies", 
            StrategyConfigurationWizardActivity.class));
        items.add(new MenuItem("Gesture Sequences", "Build gesture patterns", 
            GestureSequenceBuilderActivity.class));
        items.add(new MenuItem("AI Performance Compare", "Compare AI agents", 
            AIPerformanceComparisonActivity.class));
        items.add(new MenuItem("Real-time Analytics", "Live analytics dashboard", 
            RealTimeAnalyticsDashboardActivity.class));
        items.add(new MenuItem("Game Configuration", "Setup game profiles", 
            GameConfigurationWizardActivity.class));
        
        return items;
    }
    
    private void setupButtonListeners() {
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });
        
        btnAbout.setOnClickListener(v -> {
            // Show about dialog or navigate to about activity
            showAboutDialog();
        });
    }
    
    private void onMenuItemClick(MenuItem item) {
        Intent intent = new Intent(getActivity(), item.getActivityClass());
        startActivity(intent);
    }
    
    private void showAboutDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Gesture AI Game Automation")
               .setMessage("Advanced AI-powered mobile game automation system\n\nVersion: 1.0.0\nBuild: Production")
               .setPositiveButton("OK", null)
               .show();
    }
    
    // Helper classes
    public static class MenuItem {
        private String title;
        private String description;
        private Class<?> activityClass;
        
        public MenuItem(String title, String description, Class<?> activityClass) {
            this.title = title;
            this.description = description;
            this.activityClass = activityClass;
        }
        
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public Class<?> getActivityClass() { return activityClass; }
    }
    
    // Menu adapter
    private static class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {
        private List<MenuItem> menuItems;
        private OnMenuItemClickListener listener;
        
        public interface OnMenuItemClickListener {
            void onMenuItemClick(MenuItem item);
        }
        
        public MenuAdapter(List<MenuItem> menuItems, OnMenuItemClickListener listener) {
            this.menuItems = menuItems;
            this.listener = listener;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu_entry, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            MenuItem item = menuItems.get(position);
            holder.bind(item, listener);
        }
        
        @Override
        public int getItemCount() {
            return menuItems.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tvTitle;
            private TextView tvDescription;
            
            public ViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_menu_title);
                tvDescription = itemView.findViewById(R.id.tv_menu_description);
            }
            
            public void bind(MenuItem item, OnMenuItemClickListener listener) {
                tvTitle.setText(item.getTitle());
                tvDescription.setText(item.getDescription());
                itemView.setOnClickListener(v -> listener.onMenuItemClick(item));
            }
        }
    }
}
package com.gestureai.gameautomation.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.activities.AdvancedDebuggingToolsActivity;

public class DebugFragment extends Fragment {

    private Button btnAdvancedDebugTools;
    private CheckBox cbDebugMode;
    private CheckBox cbShowCoordinates;
    private CheckBox cbShowDetectionBoxes;
    private CheckBox cbShowFpsCounter;
    private TextView tvDebugStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_debug, container, false);

        initializeViews(view);
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        btnAdvancedDebugTools = view.findViewById(R.id.btn_advanced_debug_tools);
        cbDebugMode = view.findViewById(R.id.cb_debug_mode);
        cbShowCoordinates = view.findViewById(R.id.cb_show_coordinates);
        cbShowDetectionBoxes = view.findViewById(R.id.cb_show_detection_boxes);
        cbShowFpsCounter = view.findViewById(R.id.cb_show_fps_counter);
        tvDebugStatus = view.findViewById(R.id.tv_debug_status);
    }

    private void setupClickListeners() {
        btnAdvancedDebugTools.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AdvancedDebuggingToolsActivity.class);
            startActivity(intent);
        });

        cbDebugMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateDebugStatus();
        });

        cbShowCoordinates.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateDebugStatus();
        });

        cbShowDetectionBoxes.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateDebugStatus();
        });

        cbShowFpsCounter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateDebugStatus();
        });
    }

    private void updateDebugStatus() {
        boolean anyDebugEnabled = cbDebugMode.isChecked() || cbShowCoordinates.isChecked() 
                                || cbShowDetectionBoxes.isChecked() || cbShowFpsCounter.isChecked();
        
        if (anyDebugEnabled) {
            tvDebugStatus.setText("Debug mode active");
            tvDebugStatus.setTextColor(getResources().getColor(R.color.status_active));
        } else {
            tvDebugStatus.setText("Debug mode inactive");
            tvDebugStatus.setTextColor(getResources().getColor(R.color.status_inactive));
        }
    }
}
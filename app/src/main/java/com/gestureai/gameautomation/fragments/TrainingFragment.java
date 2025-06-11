package com.gestureai.gameautomation.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.activities.ObjectLabelingActivity;
import com.gestureai.gameautomation.activities.AITrainingDashboardActivity;

public class TrainingFragment extends Fragment {

    private Button btnObjectLabeling;
    private Button btnGestureTraining;
    private Button btnNeuralNetworkTraining;
    private Button btnVoiceCommandConfig;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_training, container, false);

        initializeViews(view);
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        btnObjectLabeling = view.findViewById(R.id.btn_object_labeling);
        btnGestureTraining = view.findViewById(R.id.btn_gesture_training);
        btnNeuralNetworkTraining = view.findViewById(R.id.btn_neural_network_training);
        btnVoiceCommandConfig = view.findViewById(R.id.btn_voice_command_config);
    }

    private void setupClickListeners() {
        btnObjectLabeling.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ObjectLabelingActivity.class);
            startActivity(intent);
        });

        btnGestureTraining.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AdvancedGestureTrainingActivity.class);
            startActivity(intent);
        });

        btnNeuralNetworkTraining.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NeuralNetworkTrainingActivity.class);
            startActivity(intent);
        });

        btnVoiceCommandConfig.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), VoiceCommandConfigurationActivity.class);
            startActivity(intent);
        });
    }
}
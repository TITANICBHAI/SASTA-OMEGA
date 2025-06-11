package com.gestureai.gameautomation.activities;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.GameTypeDetector;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.ObjectDetectionEngine;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class GameConfigurationWizardActivity extends AppCompatActivity {
    private static final String TAG = "GameConfigWizard";
    
    private ViewPager2 vpWizardSteps;
    private Button btnNext;
    private Button btnPrevious;
    private Button btnFinish;
    private TextView tvStepIndicator;
    
    private WizardPagerAdapter pagerAdapter;
    private GameProfile currentProfile;
    private GameTypeDetector gameTypeDetector;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_configuration_wizard);
        
        initializeViews();
        setupWizardFlow();
        initializeComponents();
    }
    
    private void initializeViews() {
        vpWizardSteps = findViewById(R.id.vp_wizard_steps);
        btnNext = findViewById(R.id.btn_next);
        btnPrevious = findViewById(R.id.btn_previous);
        btnFinish = findViewById(R.id.btn_finish);
        tvStepIndicator = findViewById(R.id.tv_step_indicator);
        
        setupButtonListeners();
    }
    
    private void setupButtonListeners() {
        btnNext.setOnClickListener(v -> nextStep());
        btnPrevious.setOnClickListener(v -> previousStep());
        btnFinish.setOnClickListener(v -> finishConfiguration());
    }
    
    private void setupWizardFlow() {
        currentProfile = new GameProfile();
        pagerAdapter = new WizardPagerAdapter(this);
        vpWizardSteps.setAdapter(pagerAdapter);
        
        vpWizardSteps.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateStepIndicator(position);
                updateNavigationButtons(position);
            }
        });
        
        updateStepIndicator(0);
        updateNavigationButtons(0);
    }
    
    private void initializeComponents() {
        gameTypeDetector = new GameTypeDetector(this);
    }
    
    private void nextStep() {
        int currentItem = vpWizardSteps.getCurrentItem();
        if (currentItem < pagerAdapter.getItemCount() - 1) {
            vpWizardSteps.setCurrentItem(currentItem + 1);
        }
    }
    
    private void previousStep() {
        int currentItem = vpWizardSteps.getCurrentItem();
        if (currentItem > 0) {
            vpWizardSteps.setCurrentItem(currentItem - 1);
        }
    }
    
    private void updateStepIndicator(int position) {
        tvStepIndicator.setText(String.format("Step %d of %d", 
            position + 1, pagerAdapter.getItemCount()));
    }
    
    private void updateNavigationButtons(int position) {
        btnPrevious.setEnabled(position > 0);
        btnNext.setEnabled(position < pagerAdapter.getItemCount() - 1);
        btnFinish.setVisibility(position == pagerAdapter.getItemCount() - 1 ? 
            android.view.View.VISIBLE : android.view.View.GONE);
    }
    
    private void finishConfiguration() {
        // Save game profile
        saveGameProfile(currentProfile);
        
        Toast.makeText(this, "Game profile created successfully!", Toast.LENGTH_LONG).show();
        finish();
    }
    
    private void saveGameProfile(GameProfile profile) {
        // Save to shared preferences or database
        android.content.SharedPreferences prefs = getSharedPreferences("game_profiles", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        
        editor.putString("profile_" + profile.name, profile.toJson());
        editor.apply();
        
        android.util.Log.d(TAG, "Game profile saved: " + profile.name);
    }
    
    // Game profile data class
    public static class GameProfile {
        public String name;
        public String gameType;
        public String packageName;
        public Map<String, Float> objectMappings;
        public Map<String, String> actionMappings;
        public Map<String, Float> strategySettings;
        public List<String> customGestures;
        
        public GameProfile() {
            objectMappings = new HashMap<>();
            actionMappings = new HashMap<>();
            strategySettings = new HashMap<>();
            customGestures = new ArrayList<>();
        }
        
        public String toJson() {
            // Simple JSON serialization
            return String.format(
                "{\"name\":\"%s\",\"gameType\":\"%s\",\"packageName\":\"%s\"}",
                name, gameType, packageName);
        }
    }
    
    // Wizard step fragments
    public static class GameSelectionFragment extends Fragment {
        private Spinner spinnerGameType;
        private EditText etGameName;
        private EditText etPackageName;
        private Button btnDetectGame;
        
        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, 
                                            android.view.ViewGroup container, 
                                            Bundle savedInstanceState) {
            android.view.View view = inflater.inflate(R.layout.fragment_game_selection, container, false);
            
            initializeViews(view);
            setupGameDetection();
            
            return view;
        }
        
        private void initializeViews(android.view.View view) {
            spinnerGameType = view.findViewById(R.id.spinner_game_type);
            etGameName = view.findViewById(R.id.et_game_name);
            etPackageName = view.findViewById(R.id.et_package_name);
            btnDetectGame = view.findViewById(R.id.btn_detect_game);
            
            setupGameTypeSpinner();
            btnDetectGame.setOnClickListener(v -> detectCurrentGame());
        }
        
        private void setupGameTypeSpinner() {
            String[] gameTypes = {"Battle Royale", "MOBA", "FPS", "Strategy", "Arcade", "Custom"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, gameTypes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerGameType.setAdapter(adapter);
        }
        
        private void detectCurrentGame() {
            // Auto-detect running game
            GameConfigurationWizardActivity activity = (GameConfigurationWizardActivity) getActivity();
            if (activity != null && activity.gameTypeDetector != null) {
                String detectedGame = activity.gameTypeDetector.detectCurrentGame();
                if (detectedGame != null) {
                    etGameName.setText(detectedGame);
                    Toast.makeText(getContext(), "Game detected: " + detectedGame, Toast.LENGTH_SHORT).show();
                }
            }
        }
        
        public void saveToProfile(GameProfile profile) {
            profile.name = etGameName.getText().toString();
            profile.gameType = spinnerGameType.getSelectedItem().toString();
            profile.packageName = etPackageName.getText().toString();
        }
    }
    
    public static class ObjectMappingFragment extends Fragment {
        private RecyclerView rvObjectMappings;
        private Button btnAddMapping;
        private ObjectMappingAdapter adapter;
        private List<ObjectMapping> objectMappings;
        
        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, 
                                            android.view.ViewGroup container, 
                                            Bundle savedInstanceState) {
            android.view.View view = inflater.inflate(R.layout.fragment_object_mapping, container, false);
            
            initializeViews(view);
            setupObjectMappings();
            
            return view;
        }
        
        private void initializeViews(android.view.View view) {
            rvObjectMappings = view.findViewById(R.id.rv_object_mappings);
            btnAddMapping = view.findViewById(R.id.btn_add_mapping);
            
            rvObjectMappings.setLayoutManager(new LinearLayoutManager(getContext()));
            
            objectMappings = new ArrayList<>();
            adapter = new ObjectMappingAdapter(objectMappings);
            rvObjectMappings.setAdapter(adapter);
            
            btnAddMapping.setOnClickListener(v -> addNewMapping());
        }
        
        private void setupObjectMappings() {
            // Add default mappings based on game type
            objectMappings.add(new ObjectMapping("Enemy", "High Priority", 0.9f));
            objectMappings.add(new ObjectMapping("Coin", "Medium Priority", 0.6f));
            objectMappings.add(new ObjectMapping("Obstacle", "Avoid", 0.8f));
            objectMappings.add(new ObjectMapping("Power-up", "Collect", 0.7f));
            
            adapter.notifyDataSetChanged();
        }
        
        private void addNewMapping() {
            objectMappings.add(new ObjectMapping("New Object", "Custom Action", 0.5f));
            adapter.notifyItemInserted(objectMappings.size() - 1);
        }
        
        public void saveToProfile(GameProfile profile) {
            for (ObjectMapping mapping : objectMappings) {
                profile.objectMappings.put(mapping.objectName, mapping.priority);
                profile.actionMappings.put(mapping.objectName, mapping.action);
            }
        }
        
        // Object mapping data class
        public static class ObjectMapping {
            public String objectName;
            public String action;
            public float priority;
            
            public ObjectMapping(String objectName, String action, float priority) {
                this.objectName = objectName;
                this.action = action;
                this.priority = priority;
            }
        }
        
        // Object mapping adapter
        private class ObjectMappingAdapter extends RecyclerView.Adapter<ObjectMappingAdapter.ViewHolder> {
            private List<ObjectMapping> data;
            
            public ObjectMappingAdapter(List<ObjectMapping> data) {
                this.data = data;
            }
            
            @Override
            public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_object_mapping, parent, false);
                return new ViewHolder(view);
            }
            
            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                ObjectMapping mapping = data.get(position);
                
                holder.etObjectName.setText(mapping.objectName);
                holder.etAction.setText(mapping.action);
                holder.sbPriority.setProgress((int) (mapping.priority * 100));
            }
            
            @Override
            public int getItemCount() {
                return data.size();
            }
            
            class ViewHolder extends RecyclerView.ViewHolder {
                EditText etObjectName, etAction;
                SeekBar sbPriority;
                
                public ViewHolder(android.view.View itemView) {
                    super(itemView);
                    etObjectName = itemView.findViewById(R.id.et_object_name);
                    etAction = itemView.findViewById(R.id.et_action);
                    sbPriority = itemView.findViewById(R.id.sb_priority);
                }
            }
        }
    }
    
    public static class StrategyConfigFragment extends Fragment {
        private SeekBar sbAggression;
        private SeekBar sbReactionTime;
        private SeekBar sbRiskTolerance;
        private Switch swEnableAI;
        private Switch swEnableLearning;
        
        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, 
                                            android.view.ViewGroup container, 
                                            Bundle savedInstanceState) {
            android.view.View view = inflater.inflate(R.layout.fragment_strategy_config, container, false);
            
            initializeViews(view);
            setupDefaultValues();
            
            return view;
        }
        
        private void initializeViews(android.view.View view) {
            sbAggression = view.findViewById(R.id.sb_aggression);
            sbReactionTime = view.findViewById(R.id.sb_reaction_time);
            sbRiskTolerance = view.findViewById(R.id.sb_risk_tolerance);
            swEnableAI = view.findViewById(R.id.sw_enable_ai);
            swEnableLearning = view.findViewById(R.id.sw_enable_learning);
        }
        
        private void setupDefaultValues() {
            sbAggression.setProgress(50);
            sbReactionTime.setProgress(25);
            sbRiskTolerance.setProgress(30);
            swEnableAI.setChecked(true);
            swEnableLearning.setChecked(true);
        }
        
        public void saveToProfile(GameProfile profile) {
            profile.strategySettings.put("aggression", sbAggression.getProgress() / 100.0f);
            profile.strategySettings.put("reactionTime", sbReactionTime.getProgress() / 100.0f);
            profile.strategySettings.put("riskTolerance", sbRiskTolerance.getProgress() / 100.0f);
            profile.strategySettings.put("enableAI", swEnableAI.isChecked() ? 1.0f : 0.0f);
            profile.strategySettings.put("enableLearning", swEnableLearning.isChecked() ? 1.0f : 0.0f);
        }
    }
    
    // Wizard pager adapter
    private class WizardPagerAdapter extends FragmentStateAdapter {
        
        public WizardPagerAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }
        
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new GameSelectionFragment();
                case 1:
                    return new ObjectMappingFragment();
                case 2:
                    return new StrategyConfigFragment();
                default:
                    return new GameSelectionFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
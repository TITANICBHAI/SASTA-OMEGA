package com.gestureai.gameautomation.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.systems.AdaptiveWorkflowOrchestrationSystem;
import com.gestureai.gameautomation.workflow.WorkflowDefinition;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptive Workflow Orchestration System Activity - UI for advanced workflow management
 */
public class AWOSActivity extends AppCompatActivity {
    private static final String TAG = "AWOSActivity";
    
    private TextView tvSystemStatus;
    private TextView tvActiveWorkflows;
    private Button btnCreateWorkflow;
    private Button btnStartOrchestration;
    private Button btnStopOrchestration;
    private RecyclerView recyclerViewWorkflows;
    
    private AdaptiveWorkflowOrchestrationSystem awosSystem;
    private List<WorkflowDefinition> workflowList;
    private WorkflowAdapter workflowAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_awos);
        
        initializeViews();
        initializeAWOS();
        setupListeners();
        loadWorkflows();
    }
    
    private void initializeViews() {
        tvSystemStatus = findViewById(R.id.tv_system_status);
        tvActiveWorkflows = findViewById(R.id.tv_active_workflows);
        btnCreateWorkflow = findViewById(R.id.btn_create_workflow);
        btnStartOrchestration = findViewById(R.id.btn_start_orchestration);
        btnStopOrchestration = findViewById(R.id.btn_stop_orchestration);
        recyclerViewWorkflows = findViewById(R.id.recyclerview_workflows);
        
        recyclerViewWorkflows.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void initializeAWOS() {
        try {
            awosSystem = AdaptiveWorkflowOrchestrationSystem.getInstance(this);
            updateSystemStatus();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error initializing AWOS", e);
            tvSystemStatus.setText("System Status: Error - " + e.getMessage());
        }
    }
    
    private void setupListeners() {
        btnCreateWorkflow.setOnClickListener(v -> createNewWorkflow());
        btnStartOrchestration.setOnClickListener(v -> startOrchestration());
        btnStopOrchestration.setOnClickListener(v -> stopOrchestration());
    }
    
    private void loadWorkflows() {
        workflowList = new ArrayList<>();
        
        // Load sample workflows for demonstration
        addSampleWorkflows();
        
        workflowAdapter = new WorkflowAdapter(workflowList, this::onWorkflowEdit, this::onWorkflowDelete);
        recyclerViewWorkflows.setAdapter(workflowAdapter);
        
        updateActiveWorkflowsCount();
    }
    
    private void addSampleWorkflows() {
        // Battle Royale workflow
        WorkflowDefinition battleRoyaleFlow = new WorkflowDefinition();
        battleRoyaleFlow.setName("Battle Royale Strategy");
        battleRoyaleFlow.setDescription("Automated strategy for battle royale games");
        battleRoyaleFlow.setGameType("BATTLE_ROYALE");
        workflowList.add(battleRoyaleFlow);
        
        // MOBA workflow
        WorkflowDefinition mobaFlow = new WorkflowDefinition();
        mobaFlow.setName("MOBA Lane Management");
        mobaFlow.setDescription("Automated lane farming and team fight coordination");
        mobaFlow.setGameType("MOBA");
        workflowList.add(mobaFlow);
        
        // FPS workflow
        WorkflowDefinition fpsFlow = new WorkflowDefinition();
        fpsFlow.setName("FPS Tactical Positioning");
        fpsFlow.setDescription("Automated aiming and movement optimization");
        fpsFlow.setGameType("FPS");
        workflowList.add(fpsFlow);
    }
    
    private void updateSystemStatus() {
        if (awosSystem != null) {
            boolean isActive = awosSystem.isOrchestrationActive();
            tvSystemStatus.setText("System Status: " + (isActive ? "Active" : "Inactive"));
            
            btnStartOrchestration.setEnabled(!isActive);
            btnStopOrchestration.setEnabled(isActive);
        }
    }
    
    private void updateActiveWorkflowsCount() {
        int activeCount = 0;
        if (awosSystem != null) {
            activeCount = awosSystem.getActiveWorkflowCount();
        }
        tvActiveWorkflows.setText("Active Workflows: " + activeCount);
    }
    
    private void createNewWorkflow() {
        // Show workflow creation dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Create New Workflow");
        
        // Create input fields
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        
        final android.widget.EditText nameInput = new android.widget.EditText(this);
        nameInput.setHint("Workflow name");
        layout.addView(nameInput);
        
        final android.widget.EditText descInput = new android.widget.EditText(this);
        descInput.setHint("Description");
        layout.addView(descInput);
        
        final android.widget.Spinner gameTypeSpinner = new android.widget.Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"BATTLE_ROYALE", "MOBA", "FPS", "STRATEGY", "PUZZLE", "RUNNER"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gameTypeSpinner.setAdapter(adapter);
        layout.addView(gameTypeSpinner);
        
        builder.setView(layout);
        
        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String description = descInput.getText().toString().trim();
            String gameType = gameTypeSpinner.getSelectedItem().toString();
            
            if (!name.isEmpty()) {
                WorkflowDefinition newWorkflow = new WorkflowDefinition();
                newWorkflow.setName(name);
                newWorkflow.setDescription(description);
                newWorkflow.setGameType(gameType);
                
                workflowList.add(newWorkflow);
                workflowAdapter.notifyItemInserted(workflowList.size() - 1);
                
                if (awosSystem != null) {
                    awosSystem.registerWorkflow(newWorkflow);
                }
                
                Toast.makeText(this, "Workflow created: " + name, Toast.LENGTH_SHORT).show();
                updateActiveWorkflowsCount();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void startOrchestration() {
        if (awosSystem != null) {
            try {
                awosSystem.startOrchestration();
                updateSystemStatus();
                updateActiveWorkflowsCount();
                Toast.makeText(this, "Workflow orchestration started", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to start orchestration: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void stopOrchestration() {
        if (awosSystem != null) {
            try {
                awosSystem.stopOrchestration();
                updateSystemStatus();
                updateActiveWorkflowsCount();
                Toast.makeText(this, "Workflow orchestration stopped", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to stop orchestration: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void onWorkflowEdit(int position) {
        WorkflowDefinition workflow = workflowList.get(position);
        Toast.makeText(this, "Edit workflow: " + workflow.getName(), Toast.LENGTH_SHORT).show();
        // Implementation for workflow editing would go here
    }
    
    private void onWorkflowDelete(int position) {
        WorkflowDefinition workflow = workflowList.get(position);
        
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Workflow")
                .setMessage("Are you sure you want to delete '" + workflow.getName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    workflowList.remove(position);
                    workflowAdapter.notifyItemRemoved(position);
                    
                    if (awosSystem != null) {
                        awosSystem.unregisterWorkflow(workflow);
                    }
                    
                    Toast.makeText(this, "Workflow deleted", Toast.LENGTH_SHORT).show();
                    updateActiveWorkflowsCount();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateSystemStatus();
        updateActiveWorkflowsCount();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (awosSystem != null) {
            awosSystem.cleanup();
        }
    }
    
    // Simple adapter for workflow list
    private static class WorkflowAdapter extends RecyclerView.Adapter<WorkflowAdapter.WorkflowViewHolder> {
        private List<WorkflowDefinition> workflows;
        private OnWorkflowClickListener editListener;
        private OnWorkflowClickListener deleteListener;
        
        interface OnWorkflowClickListener {
            void onWorkflowClick(int position);
        }
        
        public WorkflowAdapter(List<WorkflowDefinition> workflows, 
                              OnWorkflowClickListener editListener,
                              OnWorkflowClickListener deleteListener) {
            this.workflows = workflows;
            this.editListener = editListener;
            this.deleteListener = deleteListener;
        }
        
        @Override
        public WorkflowViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new WorkflowViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(WorkflowViewHolder holder, int position) {
            WorkflowDefinition workflow = workflows.get(position);
            holder.bind(workflow, position);
        }
        
        @Override
        public int getItemCount() {
            return workflows.size();
        }
        
        class WorkflowViewHolder extends RecyclerView.ViewHolder {
            private TextView tvName;
            private TextView tvDescription;
            
            public WorkflowViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(android.R.id.text1);
                tvDescription = itemView.findViewById(android.R.id.text2);
            }
            
            public void bind(WorkflowDefinition workflow, int position) {
                tvName.setText(workflow.getName() + " (" + workflow.getGameType() + ")");
                tvDescription.setText(workflow.getDescription());
                
                itemView.setOnClickListener(v -> {
                    if (editListener != null) {
                        editListener.onWorkflowClick(position);
                    }
                });
                
                itemView.setOnLongClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onWorkflowClick(position);
                    }
                    return true;
                });
            }
        }
    }
}
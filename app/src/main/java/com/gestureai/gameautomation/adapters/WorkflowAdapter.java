package com.gestureai.gameautomation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.workflow.WorkflowDefinition;
import java.util.List;

public class WorkflowAdapter extends RecyclerView.Adapter<WorkflowAdapter.WorkflowViewHolder> {
    
    private List<WorkflowDefinition> workflows;
    private OnWorkflowClickListener clickListener;
    
    public interface OnWorkflowClickListener {
        void onWorkflowClick(WorkflowDefinition workflow);
    }
    
    public WorkflowAdapter(List<WorkflowDefinition> workflows, OnWorkflowClickListener listener) {
        this.workflows = workflows;
        this.clickListener = listener;
    }
    
    @Override
    public WorkflowViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_workflow, parent, false);
        return new WorkflowViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(WorkflowViewHolder holder, int position) {
        WorkflowDefinition workflow = workflows.get(position);
        
        holder.workflowName.setText(workflow.getName());
        holder.workflowDescription.setText(workflow.getDescription());
        holder.stepCount.setText(workflow.getSteps().size() + " steps");
        
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onWorkflowClick(workflow);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return workflows.size();
    }
    
    static class WorkflowViewHolder extends RecyclerView.ViewHolder {
        TextView workflowName;
        TextView workflowDescription;
        TextView stepCount;
        
        WorkflowViewHolder(View itemView) {
            super(itemView);
            workflowName = itemView.findViewById(R.id.tv_workflow_name);
            workflowDescription = itemView.findViewById(R.id.tv_workflow_description);
            stepCount = itemView.findViewById(R.id.tv_step_count);
        }
    }
}
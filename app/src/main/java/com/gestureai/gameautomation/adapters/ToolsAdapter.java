package com.gestureai.gameautomation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.fragments.ToolsFragment;
import java.util.List;

public class ToolsAdapter extends RecyclerView.Adapter<ToolsAdapter.ViewHolder> {
    
    private List<ToolsFragment.ToolItem> tools;
    private OnToolClickListener clickListener;
    
    public interface OnToolClickListener {
        void onToolClick(ToolsFragment.ToolItem tool);
    }
    
    public ToolsAdapter(List<ToolsFragment.ToolItem> tools, OnToolClickListener listener) {
        this.tools = tools;
        this.clickListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tool, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ToolsFragment.ToolItem tool = tools.get(position);
        
        holder.tvIcon.setText(tool.getIcon());
        holder.tvTitle.setText(tool.getTitle());
        holder.tvDescription.setText(tool.getDescription());
        
        holder.cardView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onToolClick(tool);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return tools.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvIcon;
        TextView tvTitle;
        TextView tvDescription;
        
        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_tool);
            tvIcon = itemView.findViewById(R.id.tv_tool_icon);
            tvTitle = itemView.findViewById(R.id.tv_tool_title);
            tvDescription = itemView.findViewById(R.id.tv_tool_description);
        }
    }
}
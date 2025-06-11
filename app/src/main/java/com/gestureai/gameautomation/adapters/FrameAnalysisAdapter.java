package com.gestureai.gameautomation.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.systems.EnhancedExpertDemonstrationSystem;
import java.util.List;

public class FrameAnalysisAdapter extends RecyclerView.Adapter<FrameAnalysisAdapter.FrameViewHolder> {
    
    private List<EnhancedExpertDemonstrationSystem.AnalyzedFrame> frames;
    private OnFrameClickListener clickListener;
    private int selectedPosition = -1;
    
    public interface OnFrameClickListener {
        void onFrameClick(int position);
    }
    
    public FrameAnalysisAdapter(List<EnhancedExpertDemonstrationSystem.AnalyzedFrame> frames, 
                               OnFrameClickListener listener) {
        this.frames = frames;
        this.clickListener = listener;
    }
    
    @Override
    public FrameViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_frame_analysis, parent, false);
        return new FrameViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(FrameViewHolder holder, int position) {
        EnhancedExpertDemonstrationSystem.AnalyzedFrame frame = frames.get(position);
        
        // Display frame thumbnail
        holder.frameImage.setImageBitmap(frame.getOriginalFrame());
        
        // Frame number
        holder.frameNumber.setText("Frame " + (position + 1));
        
        // Reasoning status
        EnhancedExpertDemonstrationSystem.ExpertReasoning reasoning = frame.getReasoning();
        boolean hasReasoning = reasoning.getWhy() != null && !reasoning.getWhy().isEmpty();
        holder.reasoningStatus.setText(hasReasoning ? "✓ Reasoning" : "○ No Reasoning");
        holder.reasoningStatus.setTextColor(hasReasoning ? Color.GREEN : Color.GRAY);
        
        // AI confidence
        frame.confidence.calculateOverallConfidence();
        float confidence = frame.confidence.getOverallConfidence();
        holder.confidenceText.setText(String.format("AI: %.1f%%", confidence * 100));
        
        // Selection highlight
        holder.itemView.setBackgroundColor(
            position == selectedPosition ? Color.LTGRAY : Color.TRANSPARENT);
        
        // Click listener
        holder.itemView.setOnClickListener(v -> {
            selectedPosition = position;
            notifyDataSetChanged();
            if (clickListener != null) {
                clickListener.onFrameClick(position);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return frames.size();
    }
    
    static class FrameViewHolder extends RecyclerView.ViewHolder {
        ImageView frameImage;
        TextView frameNumber;
        TextView reasoningStatus;
        TextView confidenceText;
        
        FrameViewHolder(View itemView) {
            super(itemView);
            frameImage = itemView.findViewById(R.id.iv_frame_thumbnail);
            frameNumber = itemView.findViewById(R.id.tv_frame_number);
            reasoningStatus = itemView.findViewById(R.id.tv_reasoning_status);
            confidenceText = itemView.findViewById(R.id.tv_confidence);
        }
    }
}
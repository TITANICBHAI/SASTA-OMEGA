package com.gestureai.gameautomation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.fragments.AIFragment;
import java.util.List;

public class AIFeaturesAdapter extends RecyclerView.Adapter<AIFeaturesAdapter.ViewHolder> {
    
    private List<AIFragment.AIFeature> features;
    private OnFeatureClickListener clickListener;
    
    public interface OnFeatureClickListener {
        void onFeatureClick(AIFragment.AIFeature feature);
    }
    
    public AIFeaturesAdapter(List<AIFragment.AIFeature> features, OnFeatureClickListener listener) {
        this.features = features;
        this.clickListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ai_feature, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AIFragment.AIFeature feature = features.get(position);
        
        holder.tvTitle.setText(feature.getTitle());
        holder.tvSubtitle.setText(feature.getSubtitle());
        holder.tvDescription.setText(feature.getDescription());
        
        holder.cardView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onFeatureClick(feature);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return features.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvTitle;
        TextView tvSubtitle;
        TextView tvDescription;
        
        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_feature);
            tvTitle = itemView.findViewById(R.id.tv_feature_title);
            tvSubtitle = itemView.findViewById(R.id.tv_feature_subtitle);
            tvDescription = itemView.findViewById(R.id.tv_feature_description);
        }
    }
}
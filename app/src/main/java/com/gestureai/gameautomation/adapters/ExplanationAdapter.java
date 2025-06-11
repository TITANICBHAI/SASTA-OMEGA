package com.gestureai.gameautomation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.models.DecisionExplanation;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying AI decision explanations
 */
public class ExplanationAdapter extends RecyclerView.Adapter<ExplanationAdapter.ExplanationViewHolder> {
    
    private List<DecisionExplanation> explanations;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    
    public ExplanationAdapter(List<DecisionExplanation> explanations) {
        this.explanations = explanations;
    }
    
    @NonNull
    @Override
    public ExplanationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_explanation, parent, false);
        return new ExplanationViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ExplanationViewHolder holder, int position) {
        DecisionExplanation explanation = explanations.get(position);
        holder.bind(explanation);
    }
    
    @Override
    public int getItemCount() {
        return explanations.size();
    }
    
    class ExplanationViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTimestamp;
        private TextView tvDecision;
        private TextView tvConfidence;
        private TextView tvReasoning;
        private TextView tvKeyFactors;
        private TextView tvInfluences;
        private ProgressBar pbConfidence;
        
        public ExplanationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvDecision = itemView.findViewById(R.id.tv_decision);
            tvConfidence = itemView.findViewById(R.id.tv_confidence);
            tvReasoning = itemView.findViewById(R.id.tv_reasoning);
            tvKeyFactors = itemView.findViewById(R.id.tv_key_factors);
            tvInfluences = itemView.findViewById(R.id.tv_influences);
            pbConfidence = itemView.findViewById(R.id.pb_confidence);
        }
        
        public void bind(DecisionExplanation explanation) {
            // Format timestamp
            String timestamp = timeFormat.format(new Date(explanation.timestamp));
            tvTimestamp.setText("Frame " + explanation.frameIndex + " - " + timestamp);
            
            // Display decision
            tvDecision.setText(explanation.decision);
            
            // Display confidence
            float confidencePercent = explanation.confidence * 100;
            tvConfidence.setText(String.format("%.1f%%", confidencePercent));
            pbConfidence.setProgress((int) confidencePercent);
            
            // Display reasoning (truncated if too long)
            String reasoning = explanation.reasoning;
            if (reasoning.length() > 200) {
                reasoning = reasoning.substring(0, 200) + "...";
            }
            tvReasoning.setText(reasoning);
            
            // Display key factors
            StringBuilder factorsText = new StringBuilder();
            for (int i = 0; i < Math.min(3, explanation.keyFactors.size()); i++) {
                factorsText.append("• ").append(explanation.keyFactors.get(i)).append("\n");
            }
            tvKeyFactors.setText(factorsText.toString());
            
            // Display top influences
            String topInfluence = explanation.getTopInfluencingFactor();
            tvInfluences.setText("Primary: " + topInfluence);
        }
    }
}
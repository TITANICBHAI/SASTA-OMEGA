package com.gestureai.gameautomation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.services.VoiceCommandService;
import java.util.List;

public class VoiceCommandAdapter extends RecyclerView.Adapter<VoiceCommandAdapter.VoiceCommandViewHolder> {
    
    private List<VoiceCommandService.VoiceCommand> commands;
    private OnCommandClickListener onEditListener;
    private OnCommandClickListener onDeleteListener;
    
    public interface OnCommandClickListener {
        void onCommandClick(int position);
    }
    
    public VoiceCommandAdapter(List<VoiceCommandService.VoiceCommand> commands, 
                              OnCommandClickListener onEditListener,
                              OnCommandClickListener onDeleteListener) {
        this.commands = commands;
        this.onEditListener = onEditListener;
        this.onDeleteListener = onDeleteListener;
    }
    
    @NonNull
    @Override
    public VoiceCommandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_voice_command, parent, false);
        return new VoiceCommandViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull VoiceCommandViewHolder holder, int position) {
        VoiceCommandService.VoiceCommand command = commands.get(position);
        holder.bind(command, position);
    }
    
    @Override
    public int getItemCount() {
        return commands.size();
    }
    
    class VoiceCommandViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPhrase;
        private TextView tvAction;
        private Button btnEdit;
        private Button btnDelete;
        
        public VoiceCommandViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPhrase = itemView.findViewById(R.id.tv_phrase);
            tvAction = itemView.findViewById(R.id.tv_action);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
        
        public void bind(VoiceCommandService.VoiceCommand command, int position) {
            tvPhrase.setText(command.phrase);
            tvAction.setText(command.action);
            
            btnEdit.setOnClickListener(v -> {
                if (onEditListener != null) {
                    onEditListener.onCommandClick(position);
                }
            });
            
            btnDelete.setOnClickListener(v -> {
                if (onDeleteListener != null) {
                    onDeleteListener.onCommandClick(position);
                }
            });
        }
    }
}
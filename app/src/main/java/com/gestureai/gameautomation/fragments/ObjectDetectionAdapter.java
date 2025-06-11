package com.gestureai.gameautomation.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import com.gestureai.gameautomation.ObjectDetectionEngine.DetectedObject;

public class ObjectDetectionAdapter extends RecyclerView.Adapter<ObjectDetectionAdapter.ViewHolder> {
    private List<DetectedObject> objects = new ArrayList<>();

    public void updateObjects(List<DetectedObject> newObjects) {
        objects.clear();
        objects.addAll(newObjects);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DetectedObject obj = objects.get(position);
        holder.name.setText(obj.name);
        holder.details.setText("Confidence: " + String.format("%.1f%%", obj.confidence * 100));
    }

    @Override
    public int getItemCount() {
        return objects.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, details;

        ViewHolder(View view) {
            super(view);
            name = view.findViewById(android.R.id.text1);
            details = view.findViewById(android.R.id.text2);
        }
    }
}
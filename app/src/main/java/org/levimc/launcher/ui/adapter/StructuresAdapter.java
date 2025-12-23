package org.levimc.launcher.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.content.StructureExtractor.StructureInfo;

import java.util.ArrayList;
import java.util.List;

public class StructuresAdapter extends RecyclerView.Adapter<StructuresAdapter.StructureViewHolder> {

    private List<StructureInfo> structures = new ArrayList<>();
    private OnStructureExportListener listener;

    public interface OnStructureExportListener {
        void onExportStructure(StructureInfo structure);
    }

    public void setOnStructureExportListener(OnStructureExportListener listener) {
        this.listener = listener;
    }

    public void setStructures(List<StructureInfo> structures) {
        this.structures = structures != null ? structures : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StructureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_structure, parent, false);
        return new StructureViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StructureViewHolder holder, int position) {
        StructureInfo structure = structures.get(position);
        holder.bind(structure, listener);
    }

    @Override
    public int getItemCount() {
        return structures.size();
    }

    static class StructureViewHolder extends RecyclerView.ViewHolder {
        TextView structureName;
        TextView structureSize;
        Button exportButton;

        StructureViewHolder(View itemView) {
            super(itemView);
            structureName = itemView.findViewById(R.id.structure_name);
            structureSize = itemView.findViewById(R.id.structure_size);
            exportButton = itemView.findViewById(R.id.export_button);
        }

        void bind(StructureInfo structure, OnStructureExportListener listener) {
            structureName.setText(structure.getName());
            structureSize.setText(structure.getFormattedSize());
            
            exportButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onExportStructure(structure);
                }
            });
        }
    }
}

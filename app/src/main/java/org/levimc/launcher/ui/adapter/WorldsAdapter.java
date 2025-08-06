package org.levimc.launcher.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.content.WorldItem;

import java.util.ArrayList;
import java.util.List;

public class WorldsAdapter extends RecyclerView.Adapter<WorldsAdapter.WorldViewHolder> {

    private List<WorldItem> worlds = new ArrayList<>();
    private OnWorldActionListener onWorldActionListener;

    public interface OnWorldActionListener {
        void onWorldExport(WorldItem world);
        void onWorldDelete(WorldItem world);
        void onWorldBackup(WorldItem world);
    }

    public WorldsAdapter() {
    }

    public void setOnWorldActionListener(OnWorldActionListener listener) {
        this.onWorldActionListener = listener;
    }

    public void updateWorlds(List<WorldItem> worlds) {
        this.worlds = worlds != null ? worlds : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WorldViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_world, parent, false);
        return new WorldViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorldViewHolder holder, int position) {
        WorldItem world = worlds.get(position);
        
        holder.worldName.setText(world.getWorldName());
        holder.worldSize.setText(holder.itemView.getContext().getString(R.string.world_size, world.getFormattedSize()));
        holder.worldLastPlayed.setText(holder.itemView.getContext().getString(R.string.world_last_played, world.getFormattedLastModified()));
        holder.worldDescription.setText(world.getDescription());

        holder.exportButton.setOnClickListener(v -> {
            if (onWorldActionListener != null) {
                onWorldActionListener.onWorldExport(world);
            }
        });

        holder.backupButton.setOnClickListener(v -> {
            if (onWorldActionListener != null) {
                onWorldActionListener.onWorldBackup(world);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (onWorldActionListener != null) {
                onWorldActionListener.onWorldDelete(world);
            }
        });
    }

    @Override
    public int getItemCount() {
        return worlds.size();
    }

    static class WorldViewHolder extends RecyclerView.ViewHolder {
        TextView worldName;
        TextView worldSize;
        TextView worldLastPlayed;
        TextView worldDescription;
        ImageButton exportButton;
        ImageButton backupButton;
        ImageButton deleteButton;

        public WorldViewHolder(@NonNull View itemView) {
            super(itemView);
            worldName = itemView.findViewById(R.id.world_name);
            worldSize = itemView.findViewById(R.id.world_size);
            worldLastPlayed = itemView.findViewById(R.id.world_last_played);
            worldDescription = itemView.findViewById(R.id.world_description);
            exportButton = itemView.findViewById(R.id.world_export_button);
            backupButton = itemView.findViewById(R.id.world_backup_button);
            deleteButton = itemView.findViewById(R.id.world_delete_button);
        }
    }
}
package org.levimc.launcher.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.content.ResourcePackItem;

import java.util.ArrayList;
import java.util.List;

public class ResourcePacksAdapter extends RecyclerView.Adapter<ResourcePacksAdapter.ResourcePackViewHolder> {

    private List<ResourcePackItem> resourcePacks = new ArrayList<>();
    private OnResourcePackActionListener onResourcePackActionListener;

    public interface OnResourcePackActionListener {
        void onResourcePackDelete(ResourcePackItem pack);
    }

    public ResourcePacksAdapter() {
    }

    public void setOnResourcePackActionListener(OnResourcePackActionListener listener) {
        this.onResourcePackActionListener = listener;
    }

    public void updateResourcePacks(List<ResourcePackItem> resourcePacks) {
        this.resourcePacks = resourcePacks != null ? resourcePacks : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ResourcePackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resource_pack, parent, false);
        return new ResourcePackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResourcePackViewHolder holder, int position) {
        ResourcePackItem pack = resourcePacks.get(position);
        
        holder.packName.setText(pack.getPackName());
        holder.packDescription.setText(pack.getDescription());
        holder.packSize.setText("Size: " + pack.getFormattedSize());

        holder.deleteButton.setOnClickListener(v -> {
            if (onResourcePackActionListener != null) {
                onResourcePackActionListener.onResourcePackDelete(pack);
            }
        });
    }

    @Override
    public int getItemCount() {
        return resourcePacks.size();
    }

    static class ResourcePackViewHolder extends RecyclerView.ViewHolder {
        TextView packName;
        TextView packDescription;
        TextView packSize;
        Button deleteButton;

        public ResourcePackViewHolder(@NonNull View itemView) {
            super(itemView);
            packName = itemView.findViewById(R.id.pack_name);
            packDescription = itemView.findViewById(R.id.pack_description);
            packSize = itemView.findViewById(R.id.pack_size);
            deleteButton = itemView.findViewById(R.id.pack_delete_button);
        }
    }
}
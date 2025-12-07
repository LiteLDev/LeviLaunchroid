package org.levimc.launcher.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.model.InbuiltMod;
import org.levimc.launcher.ui.animation.DynamicAnim;

import java.util.ArrayList;
import java.util.List;

public class AddedInbuiltModsAdapter extends RecyclerView.Adapter<AddedInbuiltModsAdapter.ViewHolder> {

    private List<InbuiltMod> mods = new ArrayList<>();
    private OnRemoveClickListener onRemoveClickListener;

    public interface OnRemoveClickListener {
        void onRemoveClick(InbuiltMod mod);
    }

    public void setOnRemoveClickListener(OnRemoveClickListener listener) {
        this.onRemoveClickListener = listener;
    }

    public void updateMods(List<InbuiltMod> mods) {
        this.mods = mods;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inbuilt_mod_added, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InbuiltMod mod = mods.get(position);
        holder.name.setText(mod.getName());
        holder.description.setText(mod.getDescription());

        holder.removeButton.setOnClickListener(v -> {
            if (onRemoveClickListener != null) {
                onRemoveClickListener.onRemoveClick(mod);
            }
        });
        DynamicAnim.applyPressScale(holder.removeButton);
    }

    @Override
    public int getItemCount() {
        return mods.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, description;
        ImageButton removeButton;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.inbuilt_mod_name);
            description = itemView.findViewById(R.id.inbuilt_mod_desc);
            removeButton = itemView.findViewById(R.id.remove_inbuilt_button);
        }
    }
}

package org.levimc.launcher.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.model.InbuiltMod;
import org.levimc.launcher.ui.animation.DynamicAnim;

import java.util.ArrayList;
import java.util.List;

public class InbuiltModsListAdapter extends RecyclerView.Adapter<InbuiltModsListAdapter.ViewHolder> {

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
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inbuilt_mod_list, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InbuiltMod mod = mods.get(position);
        Context context = holder.itemView.getContext();
        holder.name.setText(mod.getName());

        int iconRes = org.levimc.launcher.ui.util.InbuiltModConfigHelper.getModIcon(mod.getId());
        holder.icon.setImageResource(iconRes);
        holder.icon.setImageTintList(null);
        holder.icon.setBackgroundTintList(null);

        holder.settingsButton.setOnClickListener(v -> org.levimc.launcher.ui.util.InbuiltModConfigHelper.showConfigDialog(context, mod, modId -> {
            org.levimc.launcher.core.mods.inbuilt.overlay.InbuiltOverlayManager overlayManager = org.levimc.launcher.core.mods.inbuilt.overlay.InbuiltOverlayManager.getInstance();
            if (overlayManager != null) {
                overlayManager.applyConfigurationChanges(modId);
            }
        }));
        DynamicAnim.applyPressScale(holder.settingsButton);

        holder.removeButton.setOnClickListener(v -> {
            if (onRemoveClickListener != null) {
                onRemoveClickListener.onRemoveClick(mod);
            }
        });
        DynamicAnim.applyPressScale(holder.removeButton);

        org.levimc.launcher.util.PersonalizationManager pm = new org.levimc.launcher.util.PersonalizationManager(context);
        pm.applyGlassToView(holder.itemView);
        pm.applyAccentToView(holder.itemView, context);
    }



    @Override
    public int getItemCount() {
        return mods.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        ImageButton settingsButton, removeButton;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.inbuilt_mod_icon);
            name = itemView.findViewById(R.id.inbuilt_mod_name);
            settingsButton = itemView.findViewById(R.id.inbuilt_mod_settings);
            removeButton = itemView.findViewById(R.id.remove_inbuilt_button);
        }
    }
}

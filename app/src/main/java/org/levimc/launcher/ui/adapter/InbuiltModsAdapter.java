package org.levimc.launcher.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class InbuiltModsAdapter extends RecyclerView.Adapter<InbuiltModsAdapter.ViewHolder> {

    private List<InbuiltMod> mods = new ArrayList<>();
    private OnAddClickListener onAddClickListener;
    private boolean isModMenuEnabled = false;

    public interface OnAddClickListener {
        void onAddClick(InbuiltMod mod);
    }

    public void setOnAddClickListener(OnAddClickListener listener) {
        this.onAddClickListener = listener;
    }

    public void updateMods(List<InbuiltMod> mods) {
        this.mods = mods;
        notifyDataSetChanged();
    }

    public void setModMenuEnabled(boolean enabled) {
        this.isModMenuEnabled = enabled;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inbuilt_mod, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InbuiltMod mod = mods.get(position);
        Context context = holder.itemView.getContext();
        holder.name.setText(mod.getName());
        holder.description.setText(mod.getDescription());

        int iconRes = org.levimc.launcher.ui.util.InbuiltModConfigHelper.getModIcon(mod.getId());
        holder.icon.setImageResource(iconRes);
        holder.icon.setImageTintList(null); // Clear any tint
        holder.icon.setBackgroundTintList(null); // Clear background tint
        holder.icon.setBackground(null); // Force clear background

        holder.settingsButton.setOnClickListener(v -> org.levimc.launcher.ui.util.InbuiltModConfigHelper.showConfigDialog(context, mod, null));
        DynamicAnim.applyPressScale(holder.settingsButton);

        holder.addButton.setOnClickListener(v -> {
            if (onAddClickListener != null) {
                onAddClickListener.onAddClick(mod);
            }
        });
        holder.addButton.setVisibility(isModMenuEnabled ? View.GONE : View.VISIBLE);
        DynamicAnim.applyPressScale(holder.addButton);

        org.levimc.launcher.util.PersonalizationManager pm = new org.levimc.launcher.util.PersonalizationManager(context);
        pm.applyGlassToView(holder.itemView);
    }



    @Override
    public int getItemCount() {
        return mods.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, description;
        ImageButton settingsButton;
        Button addButton;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.inbuilt_mod_icon);
            name = itemView.findViewById(R.id.inbuilt_mod_name);
            description = itemView.findViewById(R.id.inbuilt_mod_description);
            settingsButton = itemView.findViewById(R.id.inbuilt_mod_settings);
            addButton = itemView.findViewById(R.id.inbuilt_mod_add_button);
        }
    }
}

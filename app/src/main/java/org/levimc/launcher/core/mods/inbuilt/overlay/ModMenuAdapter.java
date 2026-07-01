package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.UnifiedMod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModMenuAdapter extends RecyclerView.Adapter<ModMenuAdapter.ViewHolder> {

    private List<UnifiedMod> mods = new ArrayList<>();
    private final Map<String, Boolean> toggleStates = new HashMap<>();
    private OnModActionListener listener;

    public interface OnModActionListener {
        void onToggle(UnifiedMod mod, boolean enabled);
        void onConfig(UnifiedMod mod);
    }

    public void setOnModActionListener(OnModActionListener listener) {
        this.listener = listener;
    }

    public void updateMods(List<UnifiedMod> mods) {
        this.mods = new ArrayList<>(mods);
        for (UnifiedMod mod : mods) {
            toggleStates.put(mod.getId(), mod.isEnabled());
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_mod_menu_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UnifiedMod mod = mods.get(position);

        holder.name.setText(mod.getName());

        if (mod.getSource() == UnifiedMod.Source.INBUILT) {
            holder.icon.setImageResource(ModIconHelper.getModIcon(mod.getId()));
            holder.icon.setImageTintList(null);
            holder.icon.setColorFilter(null);
        } else {
            holder.icon.setImageResource(R.drawable.ic_modules);
            holder.icon.setImageTintList(null);
            holder.icon.setColorFilter(null);
        }

        boolean isEnabled = toggleStates.getOrDefault(mod.getId(), false);
        updateStatusView(holder, isEnabled);

        View.OnClickListener toggleClick = v -> {
            boolean newState = !toggleStates.getOrDefault(mod.getId(), false);
            toggleStates.put(mod.getId(), newState);
            updateStatusView(holder, newState);
            if (listener != null) {
                listener.onToggle(mod, newState);
            }
        };

        holder.itemView.setOnClickListener(toggleClick);
        holder.statusText.setOnClickListener(toggleClick);
        holder.icon.setOnClickListener(toggleClick);

        if (mod.hasConfig()) {
            holder.configBtn.setVisibility(View.VISIBLE);
            holder.configBtn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConfig(mod);
                }
            });
        } else {
            holder.configBtn.setVisibility(View.GONE);
        }

        updateCardState(holder, isEnabled);
    }

    private void updateStatusView(ViewHolder holder, boolean enabled) {
        int accent = 0xFF4AE0A0;

        if (enabled) {
            holder.statusText.setText(R.string.mod_status_enabled);
            holder.statusText.setTextColor(accent);
            holder.statusText.setBackgroundResource(R.drawable.bg_mod_status_enabled);
            holder.statusText.getBackground().setTint(android.graphics.Color.argb(40, 
                android.graphics.Color.red(accent), 
                android.graphics.Color.green(accent), 
                android.graphics.Color.blue(accent)));
        } else {
            holder.statusText.setText(R.string.mod_status_disabled);
            holder.statusText.setTextColor(0xFF888888);
            holder.statusText.setBackgroundResource(R.drawable.bg_mod_status_disabled);
            holder.statusText.getBackground().setTintList(null);
        }
        updateCardState(holder, enabled);
    }

    private void updateCardState(ViewHolder holder, boolean enabled) {
        holder.itemView.setAlpha(enabled ? 1f : 0.7f);
        holder.icon.setAlpha(enabled ? 1f : 0.5f);
        
        if (holder.itemView instanceof androidx.cardview.widget.CardView) {
            androidx.cardview.widget.CardView cv = (androidx.cardview.widget.CardView) holder.itemView;
            org.levimc.launcher.util.PersonalizationManager pm = new org.levimc.launcher.util.PersonalizationManager(cv.getContext());
            
            if (enabled) {
                cv.setCardBackgroundColor(0xFF242424);
                cv.setCardElevation(6f);
            } else {
                if (pm.hasBackgroundImage()) {
                    cv.setCardBackgroundColor(android.graphics.Color.argb(50, 25, 25, 25));
                } else {
                    cv.setCardBackgroundColor(0xFF1A1A1A);
                }
                cv.setCardElevation(2f);
            }
        }
    }


    @Override
    public int getItemCount() {
        return mods.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        TextView statusText;
        ImageButton configBtn;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.mod_card_icon);
            name = itemView.findViewById(R.id.mod_card_name);
            statusText = itemView.findViewById(R.id.mod_card_status);
            configBtn = itemView.findViewById(R.id.mod_card_config);
        }
    }
}

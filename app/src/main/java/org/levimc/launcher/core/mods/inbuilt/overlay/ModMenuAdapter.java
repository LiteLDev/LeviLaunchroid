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
import org.levimc.launcher.core.mods.inbuilt.model.InbuiltMod;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModMenuAdapter extends RecyclerView.Adapter<ModMenuAdapter.ViewHolder> {

    private List<InbuiltMod> mods = new ArrayList<>();
    private final Map<String, Boolean> toggleStates = new HashMap<>();
    private OnModActionListener listener;

    public interface OnModActionListener {
        void onToggle(InbuiltMod mod, boolean enabled);
        void onConfig(InbuiltMod mod);
    }

    public void setOnModActionListener(OnModActionListener listener) {
        this.listener = listener;
    }

    public void updateMods(List<InbuiltMod> mods) {
        this.mods = new ArrayList<>(mods);
        InbuiltOverlayManager manager = InbuiltOverlayManager.getInstance();
        if (manager != null) {
            for (InbuiltMod mod : mods) {
                boolean isActive = manager.isModActive(mod.getId());
                toggleStates.put(mod.getId(), isActive);
            }
        }
        notifyDataSetChanged();
    }

    public boolean isModEnabled(String modId) {
        return toggleStates.getOrDefault(modId, false);
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
        InbuiltMod mod = mods.get(position);

        holder.name.setText(mod.getName());
        holder.icon.setImageResource(getModIcon(mod.getId()));
        holder.icon.setImageTintList(null);
        holder.icon.setColorFilter(null);

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

        holder.configBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConfig(mod);
            }
        });

        updateCardState(holder, isEnabled);
    }

    private void updateStatusView(ViewHolder holder, boolean enabled) {
        if (enabled) {
            holder.statusText.setText(R.string.mod_status_enabled);
            holder.statusText.setTextColor(0xFF4AE0A0);
            holder.statusText.setBackgroundResource(R.drawable.bg_mod_status_enabled);
        } else {
            holder.statusText.setText(R.string.mod_status_disabled);
            holder.statusText.setTextColor(0xFF888888);
            holder.statusText.setBackgroundResource(R.drawable.bg_mod_status_disabled);
        }
        updateCardState(holder, enabled);
    }

    private void updateCardState(ViewHolder holder, boolean enabled) {
        holder.itemView.setAlpha(enabled ? 1f : 0.6f);
        holder.icon.setAlpha(enabled ? 1f : 0.5f);
    }

    private int getModIcon(String modId) {
        return switch (modId) {
            case ModIds.QUICK_DROP -> R.drawable.ic_quick_drop;
            case ModIds.CAMERA_PERSPECTIVE -> R.drawable.ic_camera;
            case ModIds.TOGGLE_HUD -> R.drawable.ic_hud;
            case ModIds.AUTO_SPRINT -> R.drawable.ic_sprint_disabled;
            case ModIds.CHICK_PET -> R.drawable.chick_idle_1;
            case ModIds.ZOOM -> R.drawable.ic_zoom_disabled;
            case ModIds.FPS_DISPLAY -> R.drawable.ic_fps;
            case ModIds.CPS_DISPLAY -> R.drawable.ic_cps;
            case ModIds.SNAPLOOK -> R.drawable.ic_snaplook_disabled;
            default -> R.drawable.ic_settings;
        };
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

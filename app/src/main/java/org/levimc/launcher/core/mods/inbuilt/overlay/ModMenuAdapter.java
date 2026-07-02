package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
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
import java.util.Set;

public class ModMenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_GROUP = 0;
    private static final int VIEW_TYPE_MOD = 1;

    private final List<MenuItem> items = new ArrayList<>();
    private final Map<String, Boolean> toggleStates = new HashMap<>();
    private final Map<String, Boolean> favoriteStates = new HashMap<>();
    private OnModActionListener listener;

    public interface OnModActionListener {
        void onToggle(UnifiedMod mod, boolean enabled);
        void onConfig(UnifiedMod mod);
        void onFavoriteChanged(UnifiedMod mod, boolean favorite);
    }

    public void setOnModActionListener(OnModActionListener listener) {
        this.listener = listener;
    }

    public void updateMods(List<UnifiedMod> mods, Set<String> favoriteKeys) {
        items.clear();
        String lastGroupId = null;
        for (UnifiedMod mod : mods) {
            if (!mod.getGroupId().equals(lastGroupId)) {
                items.add(MenuItem.group(mod.getGroupId(), mod.getGroupName()));
                lastGroupId = mod.getGroupId();
            }
            items.add(MenuItem.mod(mod));
            toggleStates.put(mod.getId(), mod.isEnabled());
            favoriteStates.put(mod.getFavoriteKey(),
                favoriteKeys != null && favoriteKeys.contains(mod.getFavoriteKey()));
        }
        notifyDataSetChanged();
    }

    public boolean isGroupHeader(int position) {
        return position >= 0 && position < items.size() && items.get(position).isGroup();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_GROUP) {
            TextView title = new TextView(parent.getContext());
            float density = parent.getResources().getDisplayMetrics().density;
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (34 * density));
            params.setMargins((int) (8 * density), (int) (6 * density),
                (int) (8 * density), 0);
            title.setLayoutParams(params);
            title.setGravity(Gravity.CENTER_VERTICAL);
            title.setPadding((int) (6 * density), 0, (int) (6 * density), 0);
            title.setTextColor(0xFF4AE0A0);
            title.setTextSize(11);
            title.setTypeface(null, Typeface.BOLD);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            return new GroupViewHolder(title);
        }

        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_mod_menu_card, parent, false);
        return new ModViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MenuItem item = items.get(position);
        if (item.isGroup()) {
            ((GroupViewHolder) holder).title.setText(item.groupName);
            return;
        }

        ModViewHolder modHolder = (ModViewHolder) holder;
        UnifiedMod mod = item.mod;

        modHolder.name.setText(mod.getName());

        if (mod.getSource() == UnifiedMod.Source.INBUILT) {
            modHolder.icon.setImageResource(ModIconHelper.getModIcon(mod.getId()));
            modHolder.icon.setImageTintList(null);
            modHolder.icon.setColorFilter(null);
        } else {
            modHolder.icon.setImageResource(R.drawable.ic_modules);
            modHolder.icon.setImageTintList(null);
            modHolder.icon.setColorFilter(null);
        }

        boolean isEnabled = toggleStates.getOrDefault(mod.getId(), false);
        updateStatusView(modHolder, isEnabled);
        updateFavoriteView(modHolder, favoriteStates.getOrDefault(mod.getFavoriteKey(), false));

        View.OnClickListener toggleClick = v -> {
            boolean newState = !toggleStates.getOrDefault(mod.getId(), false);
            toggleStates.put(mod.getId(), newState);
            mod.setEnabled(newState);
            updateStatusView(modHolder, newState);
            if (listener != null) {
                listener.onToggle(mod, newState);
            }
        };

        modHolder.itemView.setOnClickListener(toggleClick);
        modHolder.statusText.setOnClickListener(toggleClick);
        modHolder.icon.setOnClickListener(toggleClick);

        modHolder.favoriteBtn.setOnClickListener(v -> {
            boolean favorite = !favoriteStates.getOrDefault(mod.getFavoriteKey(), false);
            favoriteStates.put(mod.getFavoriteKey(), favorite);
            updateFavoriteView(modHolder, favorite);
            if (listener != null) {
                listener.onFavoriteChanged(mod, favorite);
            }
        });

        if (mod.hasConfig()) {
            modHolder.configBtn.setVisibility(View.VISIBLE);
            modHolder.configBtn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConfig(mod);
                }
            });
        } else {
            modHolder.configBtn.setVisibility(View.GONE);
        }

        updateCardState(modHolder, isEnabled);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isGroup() ? VIEW_TYPE_GROUP : VIEW_TYPE_MOD;
    }

    private void updateFavoriteView(ModViewHolder holder, boolean favorite) {
        int color = favorite ? 0xFFFFC107 : 0xFFA8B0B8;
        holder.favoriteBtn.setImageTintList(ColorStateList.valueOf(color));
        holder.favoriteBtn.setAlpha(favorite ? 1f : 0.9f);
        holder.favoriteBtn.setContentDescription(holder.favoriteBtn.getContext().getString(
            favorite ? R.string.mod_menu_unfavorite : R.string.mod_menu_favorite));
    }

    private void updateStatusView(ModViewHolder holder, boolean enabled) {
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
            holder.statusText.setTextColor(0xFFB4BBC3);
            holder.statusText.setBackgroundResource(R.drawable.bg_mod_status_disabled);
            holder.statusText.getBackground().setTintList(null);
        }
        updateCardState(holder, enabled);
    }

    private void updateCardState(ModViewHolder holder, boolean enabled) {
        holder.itemView.setAlpha(1f);
        holder.icon.setAlpha(enabled ? 1f : 0.8f);
        holder.name.setTextColor(enabled ? 0xFFF1F4F6 : 0xFFD6DCE2);
        
        if (holder.itemView instanceof androidx.cardview.widget.CardView) {
            androidx.cardview.widget.CardView cv = (androidx.cardview.widget.CardView) holder.itemView;
            
            if (enabled) {
                cv.setCardBackgroundColor(0xFF28302D);
                cv.setCardElevation(6f);
            } else {
                cv.setCardBackgroundColor(0xFF24282C);
                cv.setCardElevation(2f);
            }
        }
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    private static class MenuItem {
        final String groupId;
        final String groupName;
        final UnifiedMod mod;

        private MenuItem(String groupId, String groupName, UnifiedMod mod) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.mod = mod;
        }

        static MenuItem group(String groupId, String groupName) {
            return new MenuItem(groupId, groupName, null);
        }

        static MenuItem mod(UnifiedMod mod) {
            return new MenuItem(null, null, mod);
        }

        boolean isGroup() {
            return mod == null;
        }
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView title;

        GroupViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView;
        }
    }

    static class ModViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        TextView statusText;
        ImageButton favoriteBtn;
        ImageButton configBtn;

        ModViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.mod_card_icon);
            name = itemView.findViewById(R.id.mod_card_name);
            statusText = itemView.findViewById(R.id.mod_card_status);
            favoriteBtn = itemView.findViewById(R.id.mod_card_favorite);
            configBtn = itemView.findViewById(R.id.mod_card_config);
        }
    }
}

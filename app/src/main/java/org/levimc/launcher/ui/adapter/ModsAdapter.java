package org.levimc.launcher.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModsAdapter extends RecyclerView.Adapter<ModsAdapter.ModViewHolder> {

    private List<Mod> mods = new ArrayList<>();
    private OnDeleteListener onDeleteListener;
    private OnModEnableChangeListener onModEnableChangeListener;
    private OnModReorderListener onModReorderListener;

    public interface OnDeleteListener {
        void onDelete(Mod mod, int position);
    }

    public interface OnModEnableChangeListener {
        void onEnableChanged(Mod mod, boolean enabled);
    }

    public interface OnModReorderListener {
        void onModsReordered(List<Mod> reorderedMods);
    }

    public ModsAdapter(List<Mod> mods) {
        this.mods = mods;
    }

    public void setOnDeleteListener(OnDeleteListener listener) {
        this.onDeleteListener = listener;
    }

    public void setOnModEnableChangeListener(OnModEnableChangeListener l) {
        this.onModEnableChangeListener = l;
    }

    public void setOnModReorderListener(OnModReorderListener listener) {
        this.onModReorderListener = listener;
    }

    @NonNull
    @Override
    public ModViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mod, parent, false);
        return new ModViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ModViewHolder holder, int position) {
        Mod mod = mods.get(position);
        holder.name.setText(mod.getDisplayName());

        if (holder.orderText != null) {
            holder.orderText.setText(holder.itemView.getContext().getString(R.string.mod_load_order, position + 1));
            holder.orderText.setVisibility(View.VISIBLE);
        }

        holder.switchBtn.setOnCheckedChangeListener(null);
        holder.switchBtn.setChecked(mod.isEnabled());

        holder.switchBtn.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked == mod.isEnabled()) return;

            mod.setEnabled(isChecked);

            if (onModEnableChangeListener != null) {
                onModEnableChangeListener.onEnableChanged(mod, isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mods != null ? mods.size() : 0;
    }

    public Mod getItem(int pos) {
        return mods.get(pos);
    }

    public void removeAt(int pos) {
        mods.remove(pos);
        notifyItemRemoved(pos);
    }

    public void updateMods(List<Mod> list) {
        this.mods = list;
        notifyDataSetChanged();
    }

    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mods, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mods, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);

        if (onModReorderListener != null) {
            onModReorderListener.onModsReordered(new ArrayList<>(mods));
        }
    }

    static class ModViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView orderText;
        Switch switchBtn;
        ImageView dragHandle;

        public ModViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.mod_name);
            orderText = itemView.findViewById(R.id.mod_order);
            switchBtn = itemView.findViewById(R.id.mod_switch);
            dragHandle = itemView.findViewById(R.id.drag_handle);
        }
    }
}
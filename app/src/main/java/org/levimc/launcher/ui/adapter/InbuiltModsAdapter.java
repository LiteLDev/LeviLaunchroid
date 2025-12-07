package org.levimc.launcher.ui.adapter;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.model.InbuiltMod;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;
import org.levimc.launcher.ui.animation.DynamicAnim;

import java.util.ArrayList;
import java.util.List;

public class InbuiltModsAdapter extends RecyclerView.Adapter<InbuiltModsAdapter.ViewHolder> {

    private List<InbuiltMod> mods = new ArrayList<>();
    private OnAddClickListener onAddClickListener;

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

        if (mod.getId().equals(ModIds.AUTO_SPRINT)) {
            holder.configContainer.setVisibility(View.VISIBLE);
            String[] options = {context.getString(R.string.autosprint_key_ctrl), context.getString(R.string.autosprint_key_shift)};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_item_inbuilt, options);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_inbuilt);
            holder.configSpinner.setAdapter(adapter);

            InbuiltModManager manager = InbuiltModManager.getInstance(context);
            int currentKey = manager.getAutoSprintKey();
            holder.configSpinner.setSelection(currentKey == KeyEvent.KEYCODE_SHIFT_LEFT ? 1 : 0);

            holder.configSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    int key = pos == 1 ? KeyEvent.KEYCODE_SHIFT_LEFT : KeyEvent.KEYCODE_CTRL_LEFT;
                    manager.setAutoSprintKey(key);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        } else {
            holder.configContainer.setVisibility(View.GONE);
        }

        holder.addButton.setOnClickListener(v -> {
            if (onAddClickListener != null) {
                onAddClickListener.onAddClick(mod);
            }
        });
        DynamicAnim.applyPressScale(holder.addButton);
    }

    @Override
    public int getItemCount() {
        return mods.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, description;
        Button addButton;
        LinearLayout configContainer;
        Spinner configSpinner;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.inbuilt_mod_name);
            description = itemView.findViewById(R.id.inbuilt_mod_description);
            addButton = itemView.findViewById(R.id.inbuilt_mod_add_button);
            configContainer = itemView.findViewById(R.id.config_container);
            configSpinner = itemView.findViewById(R.id.config_spinner);
        }
    }
}
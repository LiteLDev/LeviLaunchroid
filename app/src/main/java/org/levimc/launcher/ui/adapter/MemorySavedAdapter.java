package org.levimc.launcher.ui.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.memoryeditor.FreezeManager;
import org.levimc.launcher.core.mods.memoryeditor.MemoryAddress;
import org.levimc.launcher.core.mods.memoryeditor.MemoryEditorOverlay;
import java.util.ArrayList;
import java.util.List;

public class MemorySavedAdapter extends RecyclerView.Adapter<MemorySavedAdapter.ViewHolder> {
    private List<MemoryAddress> items = new ArrayList<>();
    private OnItemActionListener listener;
    private MemoryEditorOverlay overlay;

    public interface OnItemActionListener {
        void onDelete(int position);
        void onFreeze(MemoryAddress address, boolean frozen);
        void onUpdate(int position, MemoryAddress address);
        void onOverlayConfig(int position, MemoryAddress address);
    }

    public void setOnItemActionListener(OnItemActionListener listener) {
        this.listener = listener;
    }

    public void setOverlay(MemoryEditorOverlay overlay) {
        this.overlay = overlay;
    }

    public void updateItems(List<MemoryAddress> items) {
        this.items = new ArrayList<>(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_memory_saved, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MemoryAddress item = items.get(position);
        String label = item.getLabel();
        holder.labelText.setText(label.isEmpty() ? item.getType().getName() : label);
        holder.addressText.setText(item.getAddressHex());
        holder.valueEdit.removeTextChangedListener(holder.watcher);
        holder.valueEdit.setText(item.readValue());
        holder.watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String val = s.toString();
                if (!val.isEmpty()) {
                    item.writeValue(val);
                    if (item.isFrozen()) {
                        item.setFrozenValue(val);
                    }
                    if (listener != null) {
                        listener.onUpdate(holder.getAdapterPosition(), item);
                    }
                }
            }
        };
        holder.valueEdit.addTextChangedListener(holder.watcher);
        holder.valueEdit.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN && overlay != null) {
                overlay.requestFocusForEdit(holder.valueEdit);
            }
            return false;
        });
        holder.valueEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (overlay != null) {
                overlay.dismissKeyboardFromEdit(holder.valueEdit);
            }
            holder.valueEdit.clearFocus();
            return true;
        });
        holder.btnFreeze.setColorFilter(item.isFrozen() ? 0xFF00FF88 : 0xFF888888);
        holder.btnFreeze.setOnClickListener(v -> {
            boolean newState = !item.isFrozen();
            item.setFrozen(newState);
            if (newState) {
                item.setFrozenValue(holder.valueEdit.getText().toString());
                FreezeManager.getInstance().addFrozenAddress(item);
            } else {
                FreezeManager.getInstance().removeFrozenAddress(item);
            }
            holder.btnFreeze.setColorFilter(newState ? 0xFF00FF88 : 0xFF888888);
            if (listener != null) listener.onFreeze(item, newState);
        });
        holder.btnOverlay.setColorFilter(item.isOverlayEnabled() ? 0xFF00FF88 : 0xFF888888);
        holder.btnOverlay.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos >= 0 && listener != null) {
                if (item.isOverlayEnabled()) {
                    long addrValue = item.getAddress();
                    item.setOverlayEnabled(false);
                    item.setOverlayToggleable(false);
                    item.setOverlayOriginalValue("");
                    item.setOverlayNewValue("");
                    item.setOverlayName("");
                    listener.onUpdate(pos, item);
                    holder.btnOverlay.setColorFilter(0xFF888888);
                    org.levimc.launcher.core.mods.inbuilt.overlay.InbuiltOverlayManager mgr = 
                        org.levimc.launcher.core.mods.inbuilt.overlay.InbuiltOverlayManager.getInstance();
                    if (mgr != null) {
                        mgr.removeMemoryOverlay(addrValue);
                    }
                } else {
                    listener.onOverlayConfig(pos, item);
                }
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos >= 0 && listener != null) {
                listener.onDelete(pos);
            }
        });
    }

    public void notifyOverlayStateChanged(int position) {
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView labelText, addressText;
        EditText valueEdit;
        ImageButton btnFreeze, btnOverlay, btnDelete;
        TextWatcher watcher;

        ViewHolder(View v) {
            super(v);
            labelText = v.findViewById(R.id.label_text);
            addressText = v.findViewById(R.id.address_text);
            valueEdit = v.findViewById(R.id.value_edit);
            btnFreeze = v.findViewById(R.id.btn_freeze);
            btnOverlay = v.findViewById(R.id.btn_overlay);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}

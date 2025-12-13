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

public class MemoryResultAdapter extends RecyclerView.Adapter<MemoryResultAdapter.ViewHolder> {
    private List<MemoryAddress> items = new ArrayList<>();
    private OnItemActionListener listener;
    private MemoryEditorOverlay overlay;

    public interface OnItemActionListener {
        void onSave(MemoryAddress address);
        void onFreeze(MemoryAddress address, boolean frozen);
    }

    public void setOnItemActionListener(OnItemActionListener listener) {
        this.listener = listener;
    }

    public void setOverlay(MemoryEditorOverlay overlay) {
        this.overlay = overlay;
    }

    public void updateItems(List<MemoryAddress> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_memory_result, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MemoryAddress item = items.get(position);
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
        holder.btnSave.setOnClickListener(v -> {
            if (listener != null) listener.onSave(item);
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
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView addressText;
        EditText valueEdit;
        ImageButton btnSave, btnFreeze;
        TextWatcher watcher;

        ViewHolder(View v) {
            super(v);
            addressText = v.findViewById(R.id.address_text);
            valueEdit = v.findViewById(R.id.value_edit);
            btnSave = v.findViewById(R.id.btn_save);
            btnFreeze = v.findViewById(R.id.btn_freeze);
        }
    }
}

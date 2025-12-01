package org.levimc.launcher.ui.adapter;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.content.WorldEditor.WorldProperty;
import org.levimc.launcher.core.content.nbt.NbtTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorldPropertiesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_PROPERTY = 1;

    private final List<Object> items = new ArrayList<>();
    private OnPropertyChangedListener listener;

    public interface OnPropertyChangedListener {
        void onPropertyChanged(WorldProperty property, Object newValue);
    }

    public void setOnPropertyChangedListener(OnPropertyChangedListener listener) {
        this.listener = listener;
    }

    public void setProperties(Map<String, List<WorldProperty>> grouped) {
        items.clear();
        for (Map.Entry<String, List<WorldProperty>> entry : grouped.entrySet()) {
            items.add(new HeaderItem(entry.getKey()));
            items.addAll(entry.getValue());
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof HeaderItem) return TYPE_HEADER;
        return TYPE_PROPERTY;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_property_header, parent, false));
        }
        return new PropertyViewHolder(inflater.inflate(R.layout.item_world_property, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        
        if (holder instanceof HeaderViewHolder headerHolder) {
            headerHolder.bind((HeaderItem) item);
        } else if (holder instanceof PropertyViewHolder propHolder) {
            propHolder.bind((WorldProperty) item, listener);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static class HeaderItem {
        final String title;
        HeaderItem(String title) { this.title = title; }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;

        HeaderViewHolder(View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.header_text);
        }

        void bind(HeaderItem item) {
            headerText.setText(item.title);
        }
    }

    static class PropertyViewHolder extends RecyclerView.ViewHolder {
        TextView propertyName;
        TextView propertyType;
        EditText propertyValue;
        SwitchCompat propertySwitch;

        PropertyViewHolder(View itemView) {
            super(itemView);
            propertyName = itemView.findViewById(R.id.property_name);
            propertyType = itemView.findViewById(R.id.property_type);
            propertyValue = itemView.findViewById(R.id.property_value);
            propertySwitch = itemView.findViewById(R.id.property_switch);
        }

        void bind(WorldProperty property, OnPropertyChangedListener listener) {
            propertyName.setText(property.getDisplayName());
            propertyType.setText(property.getTypeString());

            propertySwitch.setOnCheckedChangeListener(null);
            propertyValue.setOnEditorActionListener(null);
            propertyValue.setOnFocusChangeListener(null);

            if (property.isBoolean()) {
                propertyValue.setVisibility(View.GONE);
                propertySwitch.setVisibility(View.VISIBLE);
                propertySwitch.setChecked(property.getTag().getByte() == 1);
                
                propertySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (listener != null) {
                        listener.onPropertyChanged(property, isChecked ? "1" : "0");
                    }
                });
            } else {
                propertySwitch.setVisibility(View.GONE);
                propertyValue.setVisibility(View.VISIBLE);
                propertyValue.setText(property.getValueString());

                int inputType = getInputType(property.getType());
                propertyValue.setInputType(inputType | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                propertyValue.setOnEditorActionListener((v, actionId, event) -> {
                    if (listener != null) {
                        String newValue = propertyValue.getText().toString();
                        if (!newValue.equals(property.getValueString())) {
                            listener.onPropertyChanged(property, newValue);
                        }
                    }
                    hideKeyboard(v);
                    propertyValue.clearFocus();
                    return true;
                });

                propertyValue.setOnFocusChangeListener((v, hasFocus) -> {
                    if (!hasFocus && listener != null) {
                        String newValue = propertyValue.getText().toString();
                        if (!newValue.equals(property.getValueString())) {
                            listener.onPropertyChanged(property, newValue);
                        }
                    }
                });
            }
        }

        private int getInputType(byte nbtType) {
            return switch (nbtType) {
                case NbtTag.TAG_BYTE, NbtTag.TAG_SHORT, NbtTag.TAG_INT, NbtTag.TAG_LONG ->
                    InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
                case NbtTag.TAG_FLOAT, NbtTag.TAG_DOUBLE ->
                    InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED;
                default -> InputType.TYPE_CLASS_TEXT;
            };
        }

        private void hideKeyboard(View view) {
            Context context = view.getContext();
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
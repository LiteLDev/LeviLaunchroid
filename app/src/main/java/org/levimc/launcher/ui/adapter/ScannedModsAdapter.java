package org.levimc.launcher.ui.adapter;

import android.content.res.ColorStateList;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.ui.animation.DynamicAnim;

import org.levimc.launcher.util.StorageAccess;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ScannedModsAdapter extends RecyclerView.Adapter<ScannedModsAdapter.ViewHolder> {
    public interface OnAddClickListener {
        void onClick(StorageAccess.Document file);
    }

    private final List<StorageAccess.Document> files;
    private final Set<String> addedPaths = new HashSet<>();
    private final OnAddClickListener listener;
    private String importingPath;

    public ScannedModsAdapter(List<StorageAccess.Document> files, OnAddClickListener listener) {
        this.files = files;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scanned_mod, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StorageAccess.Document file = files.get(position);
        String path = file.uri.toString();
        String lowerName = file.name.toLowerCase(Locale.ROOT);
        int type = lowerName.endsWith(".levipack")
                ? R.string.scan_downloads_levipack
                : R.string.scan_downloads_native_library;

        holder.name.setText(file.name);
        holder.details.setText(holder.itemView.getContext().getString(
                R.string.scan_downloads_file_details,
                holder.itemView.getContext().getString(type),
                Formatter.formatShortFileSize(holder.itemView.getContext(), file.size)));
        holder.add.setOnClickListener(null);

        if (addedPaths.contains(path)) {
            setButtonState(holder.add, R.string.scan_downloads_added, false, false);
        } else if (path.equals(importingPath)) {
            setButtonState(holder.add, R.string.scan_downloads_adding, false, false);
        } else {
            setButtonState(holder.add, R.string.scan_downloads_add, importingPath == null, true);
            holder.add.setOnClickListener(v -> listener.onClick(file));
        }
    }

    private void setButtonState(Button button, int text, boolean enabled, boolean primary) {
        button.setText(text);
        button.setEnabled(enabled);
        int background = ContextCompat.getColor(button.getContext(),
                primary ? R.color.primary : R.color.surface_variant);
        int textColor = ContextCompat.getColor(button.getContext(),
                primary ? R.color.on_primary : R.color.text_secondary);
        button.setBackgroundTintList(ColorStateList.valueOf(background));
        button.setTextColor(textColor);
    }

    public void setImporting(StorageAccess.Document file) {
        importingPath = file.uri.toString();
        notifyDataSetChanged();
    }

    public void setAdded(StorageAccess.Document file) {
        addedPaths.add(file.uri.toString());
        importingPath = null;
        notifyDataSetChanged();
    }

    public void setIdle(StorageAccess.Document file) {
        if (file.uri.toString().equals(importingPath)) {
            importingPath = null;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView details;
        final Button add;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.scanned_mod_name);
            details = itemView.findViewById(R.id.scanned_mod_details);
            add = itemView.findViewById(R.id.scanned_mod_add);
            DynamicAnim.applyPressScale(add);
        }
    }
}

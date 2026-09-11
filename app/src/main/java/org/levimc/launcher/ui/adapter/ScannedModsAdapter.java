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

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ScannedModsAdapter extends RecyclerView.Adapter<ScannedModsAdapter.ViewHolder> {
    public interface OnAddClickListener {
        void onClick(File file);
    }

    private final List<File> files;
    private final Set<String> addedPaths = new HashSet<>();
    private final OnAddClickListener listener;
    private String importingPath;

    public ScannedModsAdapter(List<File> files, OnAddClickListener listener) {
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
        File file = files.get(position);
        String path = file.getAbsolutePath();
        String lowerName = file.getName().toLowerCase(Locale.ROOT);
        int type = lowerName.endsWith(".levipack")
                ? R.string.scan_downloads_levipack
                : R.string.scan_downloads_native_library;

        holder.name.setText(file.getName());
        holder.details.setText(holder.itemView.getContext().getString(
                R.string.scan_downloads_file_details,
                holder.itemView.getContext().getString(type),
                Formatter.formatShortFileSize(holder.itemView.getContext(), file.length())));
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

    public void setImporting(File file) {
        importingPath = file.getAbsolutePath();
        notifyDataSetChanged();
    }

    public void setAdded(File file) {
        addedPaths.add(file.getAbsolutePath());
        importingPath = null;
        notifyDataSetChanged();
    }

    public void setIdle(File file) {
        if (file.getAbsolutePath().equals(importingPath)) {
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

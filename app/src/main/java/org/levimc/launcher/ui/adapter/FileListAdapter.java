package org.levimc.launcher.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.curseforge.models.ContentFile;

import java.util.ArrayList;
import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {

    private List<ContentFile> files = new ArrayList<>();
    private final OnFileClickListener listener;
    private final int accentColor;

    public interface OnFileClickListener {
        void onDownloadClick(ContentFile file);
    }

    public FileListAdapter(OnFileClickListener listener, int accentColor) {
        this.listener = listener;
        this.accentColor = accentColor;
    }

    public void setFiles(List<ContentFile> files) {
        this.files = files;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContentFile file = files.get(position);
        holder.bind(file, listener, accentColor);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView date;
        TextView version;
        View btnDownload;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.file_name);
            date = itemView.findViewById(R.id.file_date);
            version = itemView.findViewById(R.id.file_version);
            btnDownload = itemView.findViewById(R.id.btn_download);
        }

        void bind(final ContentFile file, final OnFileClickListener listener, int accentColor) {
            name.setText(file.displayName != null ? file.displayName : file.fileName);
            date.setText(file.fileDate != null && file.fileDate.length() >= 10 ? file.fileDate.substring(0, 10) : "");
            
            if (file.gameVersions != null && !file.gameVersions.isEmpty()) {
                String v = file.gameVersions.get(0);
                for (String ver : file.gameVersions) {
                    if (ver.matches(".*\\d+\\.\\d+.*")) {
                        v = ver;
                        break;
                    }
                }
                version.setText(v);
            } else {
                version.setText("");
            }
            
            if (btnDownload instanceof com.google.android.material.button.MaterialButton) {
                com.google.android.material.button.MaterialButton btn = (com.google.android.material.button.MaterialButton) btnDownload;
                btn.setTextColor(accentColor);
                btn.setIconTint(android.content.res.ColorStateList.valueOf(accentColor));
            }

            btnDownload.setOnClickListener(v -> listener.onDownloadClick(file));
        }
    }
}

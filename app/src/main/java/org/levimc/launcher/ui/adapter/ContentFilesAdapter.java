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

public class ContentFilesAdapter extends RecyclerView.Adapter<ContentFilesAdapter.ViewHolder> {

    private List<ContentFile> files = new ArrayList<>();
    private final OnFileClickListener listener;

    public interface OnFileClickListener {
        void onDownloadClick(ContentFile file);
    }

    public ContentFilesAdapter(OnFileClickListener listener) {
        this.listener = listener;
    }

    public void setFiles(List<ContentFile> files) {
        this.files = files;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_simple, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContentFile file = files.get(position);
        holder.bind(file, listener);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView date;
        TextView version;
        ImageButton btnDownload;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.file_name);
            date = itemView.findViewById(R.id.file_date);
            version = itemView.findViewById(R.id.file_version);
            btnDownload = itemView.findViewById(R.id.btn_download);
        }

        void bind(final ContentFile file, final OnFileClickListener listener) {
            name.setText(file.displayName != null ? file.displayName : file.fileName);
            date.setText(file.fileDate != null ? file.fileDate.substring(0, Math.min(10, file.fileDate.length())) : "");
            
            if (file.gameVersions != null && !file.gameVersions.isEmpty()) {
                // Find a version that looks like a version number if possible
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

            btnDownload.setOnClickListener(v -> listener.onDownloadClick(file));
        }
    }
}

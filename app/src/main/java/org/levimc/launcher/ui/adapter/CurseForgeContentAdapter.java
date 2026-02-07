package org.levimc.launcher.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import org.levimc.launcher.R;
import org.levimc.launcher.core.curseforge.models.Content;

import java.util.ArrayList;
import java.util.List;

public class CurseForgeContentAdapter extends RecyclerView.Adapter<CurseForgeContentAdapter.ViewHolder> {

    private List<Content> contents = new ArrayList<>();
    private final OnContentClickListener listener;

    public interface OnContentClickListener {
        void onContentClick(Content content);
    }

    public CurseForgeContentAdapter(OnContentClickListener listener) {
        this.listener = listener;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_curseforge_content, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Content content = contents.get(position);
        holder.bind(content, listener);
    }

    @Override
    public int getItemCount() {
        return contents.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView description;
        TextView author;
        TextView metadata;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.mod_icon);
            title = itemView.findViewById(R.id.mod_title);
            description = itemView.findViewById(R.id.mod_description);
            author = itemView.findViewById(R.id.mod_author);
            metadata = itemView.findViewById(R.id.mod_metadata);
        }

        void bind(final Content content, final OnContentClickListener listener) {
            title.setText(content.name);
            description.setText(content.summary);
            
            if (content.authors != null && !content.authors.isEmpty()) {
                author.setText("by " + content.authors.get(0).name);
            } else {
                author.setText("");
            }

            StringBuilder meta = new StringBuilder();
            if (content.downloadCount > 1000000) {
                meta.append(String.format("%.1fM Downloads", content.downloadCount / 1000000.0));
            } else if (content.downloadCount > 1000) {
                meta.append(String.format("%.1fK Downloads", content.downloadCount / 1000.0));
            } else {
                meta.append(content.downloadCount).append(" Downloads");
            }

            if (content.dateModified != null) {
                meta.append(" • Updated ").append(content.dateModified.substring(0, Math.min(10, content.dateModified.length())));
            }

            if (content.categories != null && !content.categories.isEmpty()) {
                 meta.append(" • ").append(content.categories.get(0).name);
            }
            
            if (metadata != null) {
                metadata.setText(meta.toString());
            }

            String iconUrl = null;
            if (content.logo != null) {
                iconUrl = content.logo.thumbnailUrl;
            }

            if (iconUrl != null) {
                Glide.with(itemView.getContext())
                        .load(iconUrl)
                        .transform(new RoundedCorners(16))
                        .placeholder(R.drawable.ic_minecraft_cube)
                        .error(R.drawable.ic_minecraft_cube)
                        .into(icon);
            } else {
                icon.setImageResource(R.drawable.ic_minecraft_cube);
            }

            itemView.setOnClickListener(v -> listener.onContentClick(content));
        }
    }
}
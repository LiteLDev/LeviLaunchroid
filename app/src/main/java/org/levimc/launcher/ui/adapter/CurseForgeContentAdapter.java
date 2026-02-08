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

public class CurseForgeContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Content> contents = new ArrayList<>();
    private final OnContentClickListener listener;

    public interface OnContentClickListener {
        void onContentClick(Content content);
    }

    public interface OnPageChangeListener {
        void onNextPage();
        void onPrevPage();
    }

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_FOOTER = 1;

    private OnPageChangeListener pageChangeListener;
    private int currentPage = 1;
    private int totalPages = 1;


    public CurseForgeContentAdapter(OnContentClickListener listener, OnPageChangeListener pageChangeListener) {
        this.listener = listener;
        this.pageChangeListener = pageChangeListener;
    }


    public void setContents(List<Content> contents, int currentPage, int totalPages) {
        this.contents = contents;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FOOTER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_curseforge_footer, parent, false);
            return new FooterViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_curseforge_content, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolder) {
            Content content = contents.get(position);
            ((ViewHolder) holder).bind(content, listener);
        } else if (holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).bind(currentPage, totalPages, pageChangeListener);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return (position == contents.size()) ? VIEW_TYPE_FOOTER : VIEW_TYPE_ITEM;
    }


    @Override
    public int getItemCount() {
        return contents.isEmpty() ? 0 : contents.size() + 1; // +1 for footer
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

    static class FooterViewHolder extends RecyclerView.ViewHolder {
        android.widget.Button btnPrev;
        android.widget.Button btnNext;
        TextView tvPageInfo;

        FooterViewHolder(@NonNull View itemView) {
            super(itemView);
            btnPrev = itemView.findViewById(R.id.btn_prev_page);
            btnNext = itemView.findViewById(R.id.btn_next_page);
            tvPageInfo = itemView.findViewById(R.id.tv_page_info);
        }

        void bind(int currentPage, int totalPages, final OnPageChangeListener listener) {
            tvPageInfo.setText("Page " + currentPage + " of " + (totalPages > 0 ? totalPages : "?"));
            
            btnPrev.setEnabled(currentPage > 1);
            btnPrev.setAlpha(currentPage > 1 ? 1.0f : 0.5f);
            
            btnNext.setEnabled(totalPages == 0 || currentPage < totalPages);
            btnNext.setAlpha((totalPages == 0 || currentPage < totalPages) ? 1.0f : 0.5f);
            
            btnPrev.setOnClickListener(v -> {
                if (listener != null) listener.onPrevPage();
            });
            
            btnNext.setOnClickListener(v -> {
                if (listener != null) listener.onNextPage();
            });
        }
    }
}
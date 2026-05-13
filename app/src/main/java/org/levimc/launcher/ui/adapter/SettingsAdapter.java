package org.levimc.launcher.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.ContainerViewHolder> {

    public interface ContentBuilder {
        void build(LinearLayout container);
    }

    private final ContentBuilder builder;

    public SettingsAdapter(ContentBuilder builder) {
        this.builder = builder;
    }

    @NonNull
    @Override
    public ContainerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_settings_container, parent, false);
        return new ContainerViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ContainerViewHolder holder, int position) {
        if (builder != null) {
            holder.container.removeAllViews();
            builder.build(holder.container);
        }
        android.content.Context ctx = holder.itemView.getContext();
        org.levimc.launcher.util.PersonalizationManager pm = new org.levimc.launcher.util.PersonalizationManager(ctx);
        pm.applyAccentToView(holder.itemView, ctx);
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    static class ContainerViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;
        public ContainerViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.settings_items);
        }
    }
}
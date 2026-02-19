package org.levimc.launcher.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.content.ServerItem;

import java.util.List;

public class ServersAdapter extends RecyclerView.Adapter<ServersAdapter.ViewHolder> {
    private List<ServerItem> servers;
    private final OnServerClickListener listener;

    public interface OnServerClickListener {
        void onDeleteClick(ServerItem server);
    }

    public ServersAdapter(List<ServerItem> servers, OnServerClickListener listener) {
        this.servers = servers;
        this.listener = listener;
    }

    public void updateData(List<ServerItem> newServers) {
        this.servers = newServers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_server, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServerItem server = servers.get(position);
        holder.nameText.setText(server.name);
        holder.ipText.setText(server.ip + ":" + server.port);

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(server);
            }
        });
    }

    @Override
    public int getItemCount() {
        return servers == null ? 0 : servers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView ipText;
        Button deleteButton;

        ViewHolder(View view) {
            super(view);
            nameText = view.findViewById(R.id.server_name);
            ipText = view.findViewById(R.id.server_ip);
            deleteButton = view.findViewById(R.id.server_delete_button);
        }
    }
}

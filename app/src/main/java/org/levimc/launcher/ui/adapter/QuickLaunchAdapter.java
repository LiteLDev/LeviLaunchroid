package org.levimc.launcher.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.ui.animation.DynamicAnim;

import java.util.ArrayList;
import java.util.List;

public class QuickLaunchAdapter extends RecyclerView.Adapter<QuickLaunchAdapter.ViewHolder> {

    public enum ActionType {
        HOW_TO_PLAY,
        SERVERS_TAB,
        PROFILE_SCREEN,
        STORE_HOME,
        MINECOIN_OFFERS,
        MARKETPLACE_PASS,
        CONNECT_SERVER,
        ADD_SERVER,
        REALM_INVITE,
        LOAD_WORLD,
        SLASH_COMMAND,
        CUSTOM_URI
    }

    public static class QuickLaunchItem {
        public final String title;
        public final String description;
        public final ActionType actionType;

        public QuickLaunchItem(String title, String description, ActionType actionType) {
            this.title = title;
            this.description = description;
            this.actionType = actionType;
        }
    }

    public interface OnActionClickListener {
        void onActionClick(ActionType actionType);
    }

    private List<QuickLaunchItem> items = new ArrayList<>();
    private OnActionClickListener listener;

    public void setOnActionClickListener(OnActionClickListener listener) {
        this.listener = listener;
    }

    public void updateItems(List<QuickLaunchItem> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quick_launch, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuickLaunchItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView descriptionText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.title_text);
            descriptionText = itemView.findViewById(R.id.description_text);
            
            DynamicAnim.applyPressScale(itemView);
        }

        void bind(QuickLaunchItem item) {
            titleText.setText(item.title);
            descriptionText.setText(item.description);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActionClick(item.actionType);
                }
            });
        }
    }
}

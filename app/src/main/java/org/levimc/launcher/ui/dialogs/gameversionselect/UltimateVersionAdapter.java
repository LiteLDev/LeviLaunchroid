package org.levimc.launcher.ui.dialogs.gameversionselect;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.versions.GameVersion;

import java.util.ArrayList;
import java.util.List;

public class UltimateVersionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_BIG_GROUP = 0;
    private static final int TYPE_VER_GROUP = 1;
    private static final int TYPE_ITEM = 2;

    private final List<Object> flatItems = new ArrayList<>();
    private final Context context;
    private OnVersionSelectListener listener;

    public interface OnVersionSelectListener {
        void onVersionSelected(GameVersion version);
    }

    public interface OnVersionLongClickListener {
        void onVersionLongClicked(GameVersion version);
    }

    public void setOnVersionSelectListener(OnVersionSelectListener l) {
        this.listener = l;
    }

    private OnVersionLongClickListener longClickListener;

    public void setOnVersionLongClickListener(OnVersionLongClickListener l) {
        this.longClickListener = l;
    }

    public UltimateVersionAdapter(Context context, List<BigGroup> bigGroups) {
        this.context = context;
        for (BigGroup group : bigGroups) {
            flatItems.add(group.groupTitleResId);
            for (VersionGroup vg : group.versionGroups) {
                flatItems.add(vg.versionCode);
                flatItems.addAll(vg.versions);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Object item = flatItems.get(position);
        if (item instanceof Integer) return TYPE_BIG_GROUP;
        if (item instanceof String) return TYPE_VER_GROUP;
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_BIG_GROUP) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_version_big_group, parent, false);
            return new BigGroupVH(v);
        } else if (viewType == TYPE_VER_GROUP) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_version_group_title, parent, false);
            return new VerGroupVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_version, parent, false);
            return new ItemVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = flatItems.get(position);
        int viewType = getItemViewType(position);
        if (viewType == TYPE_BIG_GROUP) {
            int resId = (int) item;
            ((BigGroupVH) holder).title.setText(context.getString(resId));
        } else if (viewType == TYPE_VER_GROUP) {
            ((VerGroupVH) holder).title.setText((String) item);
        } else {
            GameVersion gv = (GameVersion) item;
            ((ItemVH) holder).bind(gv, listener, longClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return flatItems.size();
    }

    static class BigGroupVH extends RecyclerView.ViewHolder {
        TextView title;

        BigGroupVH(View v) {
            super(v);
            title = v.findViewById(R.id.tv_big_group_title);
        }
    }

    static class VerGroupVH extends RecyclerView.ViewHolder {
        TextView title;

        VerGroupVH(View v) {
            super(v);
            title = v.findViewById(R.id.tv_version_code_group);
        }
    }

    static class ItemVH extends RecyclerView.ViewHolder {
        TextView tv;
        LinearLayout parentLayout;
        View btnRename;

        ItemVH(View v) {
            super(v);
            tv = v.findViewById(R.id.tv_version_name_item);
            parentLayout = v.findViewById(R.id.linear_parent);
            btnRename = v.findViewById(R.id.btn_rename);
        }

        public void bind(GameVersion v, OnVersionSelectListener listener, OnVersionLongClickListener longClickListener) {
            StringBuilder sb = new StringBuilder();
            sb.append(v.displayName);
            if (v.isInstalled && v.packageName != null) {
                sb.append(" ").append(v.packageName);
            }
            tv.setText(sb.toString());
            parentLayout.setOnClickListener(_v -> {
                if (listener != null) listener.onVersionSelected(v);
            });

            if (!v.isInstalled) {
                if (btnRename != null) {
                    btnRename.setVisibility(View.VISIBLE);
                    btnRename.setOnClickListener(_v -> {
                        if (longClickListener != null) longClickListener.onVersionLongClicked(v);
                    });
                    org.levimc.launcher.ui.animation.DynamicAnim.applyPressScale(btnRename);
                }
                parentLayout.setOnLongClickListener(_v -> {
                    if (longClickListener != null) longClickListener.onVersionLongClicked(v);
                    return true;
                });
            } else {
                if (btnRename != null) btnRename.setVisibility(View.GONE);
                parentLayout.setOnLongClickListener(null);
            }
        }
    }
}
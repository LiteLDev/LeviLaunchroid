package org.levimc.launcher.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.auth.MsftAccountStore;
import org.levimc.launcher.util.AccountTextUtils;
import org.levimc.launcher.util.PersonalizationManager;

import java.util.ArrayList;
import java.util.List;

public class AccountsAdapter extends RecyclerView.Adapter<AccountsAdapter.AccountViewHolder> {

    public interface OnAccountActionListener {
        void onSetActive(MsftAccountStore.MsftAccount account);
        void onDelete(MsftAccountStore.MsftAccount account);
    }

    private final List<MsftAccountStore.MsftAccount> accounts = new ArrayList<>();
    private OnAccountActionListener listener;

    public void setOnAccountActionListener(OnAccountActionListener listener) {
        this.listener = listener;
    }

    public void updateAccounts(List<MsftAccountStore.MsftAccount> list) {
        accounts.clear();
        if (list != null) accounts.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_msft_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        MsftAccountStore.MsftAccount a = accounts.get(position);
        String titleText = AccountTextUtils.titleOrUnknown(a);
        holder.title.setText(titleText);

        String sub = AccountTextUtils.subtitle(a);
        holder.subtitle.setText(sub);

        if (a.active) {
            holder.activeBadge.setVisibility(View.VISIBLE);
            holder.btnUse.setEnabled(false);
            holder.btnUse.setText(R.string.ms_activated);
            holder.btnUse.setAlpha(0.6f);
        } else {
            holder.activeBadge.setVisibility(View.GONE);
            holder.btnUse.setEnabled(true);
            holder.btnUse.setText(R.string.ms_set_active);
            holder.btnUse.setAlpha(1.0f);
        }

        holder.btnUse.setOnClickListener(v -> {
            if (listener != null) listener.onSetActive(a);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(a);
        });

        new PersonalizationManager(holder.itemView.getContext())
                .applyAccentToView(holder.itemView, holder.itemView.getContext());
        applyButtonAccent(holder);
    }

    private void applyButtonAccent(@NonNull AccountViewHolder holder) {
        Context context = holder.itemView.getContext();
        int accent = new PersonalizationManager(context).getAccentColor();
        if (accent == 0) {
            accent = ContextCompat.getColor(context, R.color.primary);
        }
        ColorStateList tint = ColorStateList.valueOf(accent);
        holder.btnUse.setBackgroundTintList(tint);
        holder.btnUse.setTextColor(Color.WHITE);
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    static class AccountViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView subtitle;
        TextView activeBadge;
        Button btnUse;
        ImageButton btnDelete;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.account_title);
            subtitle = itemView.findViewById(R.id.account_subtitle);
            activeBadge = itemView.findViewById(R.id.active_badge);
            btnUse = itemView.findViewById(R.id.btn_use);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}

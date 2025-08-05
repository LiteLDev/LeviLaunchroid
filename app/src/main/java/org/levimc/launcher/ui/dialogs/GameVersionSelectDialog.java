package org.levimc.launcher.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.core.versions.VersionManager;
import org.levimc.launcher.ui.dialogs.gameversionselect.BigGroup;
import org.levimc.launcher.ui.dialogs.gameversionselect.UltimateVersionAdapter;

import java.util.List;

public class GameVersionSelectDialog extends Dialog {
    public interface OnVersionSelectListener {
        void onVersionSelected(GameVersion version);
    }

    private OnVersionSelectListener listener;
    private List<BigGroup> bigGroups;

    public GameVersionSelectDialog(@NonNull Context ctx, List<BigGroup> bigGroups) {
        super(ctx);
        this.bigGroups = bigGroups;
    }

    public void setOnVersionSelectListener(OnVersionSelectListener l) {
        this.listener = l;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_game_version_select);
        RecyclerView recyclerView = findViewById(R.id.recycler_versions);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        UltimateVersionAdapter adapter = new UltimateVersionAdapter(getContext(), bigGroups);
        adapter.setOnVersionSelectListener(v -> {
            if (listener != null) listener.onVersionSelected(v);
            dismiss();
        });

        adapter.setOnVersionLongClickListener(this::showRenameDialog);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        recyclerView.setAdapter(adapter);
    }

    private void showRenameDialog(GameVersion version) {
        if (version.isInstalled) {
            Toast.makeText(getContext(), getContext().getString(R.string.cannot_rename_installed), Toast.LENGTH_SHORT).show();
            return;
        }

        VersionRenameDialog renameDialog = new VersionRenameDialog(getContext())
                .setVersion(version)
                .setCallback(new VersionRenameDialog.Callback() {
                    @Override
                    public void onRenameClicked(String newName) {
                        VersionManager versionManager = VersionManager.get(getContext());
                        versionManager.renameCustomVersion(version, newName, new VersionManager.OnRenameVersionCallback() {
                            @Override
                            public void onRenameCompleted(boolean success) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (success) {
                                        Toast.makeText(getContext(), getContext().getString(R.string.rename_success), Toast.LENGTH_SHORT).show();
                                        dismiss();
                                    }
                                });
                            }

                            @Override
                            public void onRenameFailed(Exception e) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    Toast.makeText(getContext(), getContext().getString(R.string.rename_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                                });
                            }
                        });
                    }

                    @Override
                    public void onCancelled() {
                    }
                });
        renameDialog.show();
    }
}
package org.levimc.launcher.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import org.levimc.launcher.R;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;

public class PermissionsHandler {

    public interface PermissionResultCallback {
        void onPermissionGranted(PermissionType type);

        void onPermissionDenied(PermissionType type, boolean permanentlyDenied);
    }

    public enum PermissionType {
        OVERLAY, UNKNOWN_SOURCES
    }

    private static volatile PermissionsHandler instance;
    private Activity activity;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private PermissionResultCallback callback;
    private PermissionType pendingType = null;

    private PermissionsHandler() {
    }

    public static PermissionsHandler getInstance() {
        if (instance == null) {
            synchronized (PermissionsHandler.class) {
                if (instance == null) {
                    instance = new PermissionsHandler();
                }
            }
        }
        return instance;
    }

    public void setActivity(Activity activity, ActivityResultLauncher<Intent> resultLauncher) {
        this.activity = activity;
        this.activityResultLauncher = resultLauncher;
    }

    public boolean hasPermission(PermissionType type) {
        if (activity == null) return false;
        switch (type) {
            case OVERLAY:
                return Settings.canDrawOverlays(activity);
            case UNKNOWN_SOURCES:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return activity.getPackageManager().canRequestPackageInstalls();
                }
                return true;
        }
        return false;
    }

    public void requestPermission(PermissionType type, PermissionResultCallback callback) {
        this.callback = callback;
        this.pendingType = type;
        if (activity == null)
            throw new IllegalStateException("Activity not set. Call setActivity() first.");
        if (hasPermission(type)) {
            runOnUiThread(() -> {
                if (callback != null) callback.onPermissionGranted(type);
            });
            return;
        }
        switch (type) {
            case OVERLAY:
                requestOverlayPermission();
                break;
            case UNKNOWN_SOURCES:
                requestUnknownSourcesPermission();
                break;
        }
    }

    private void requestOverlayPermission() {
        if (!hasPermission(PermissionType.OVERLAY)) {
            runOnUiThread(() -> {
                if (!Settings.canDrawOverlays(activity)) {
                    new CustomAlertDialog(activity)
                            .setTitleText(activity.getString(R.string.overlay_permission_message))
                            .setNegativeButton(activity.getString(R.string.cancel), (v) -> {
                                if (callback != null)
                                    callback.onPermissionDenied(PermissionType.OVERLAY, false);
                            })
                            .setPositiveButton(activity.getString(R.string.grant_permission), (v) -> {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName()));
                                if (activityResultLauncher != null)
                                    activityResultLauncher.launch(intent);
                            })
                            .show();
                } else {
                    if (callback != null) callback.onPermissionGranted(PermissionType.OVERLAY);
                }
            });
        }
    }

    private void requestUnknownSourcesPermission() {
        if (!hasPermission(PermissionType.UNKNOWN_SOURCES)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runOnUiThread(() -> {
                    new CustomAlertDialog(activity)
                            .setTitleText(activity.getString(R.string.unknown_sources_permission_title))
                            .setMessage(activity.getString(R.string.unknown_sources_permission_message))
                            .setPositiveButton(activity.getString(R.string.grant_permission), (v) -> {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:" + activity.getPackageName()));
                                if (activityResultLauncher != null)
                                    activityResultLauncher.launch(intent);
                            })
                            .setNegativeButton(activity.getString(R.string.cancel), (v) -> {
                                if (callback != null)
                                    callback.onPermissionDenied(PermissionType.UNKNOWN_SOURCES, false);
                            })
                            .show();
                });
            }
        }
    }

    public void onActivityResult(int resultCode, Intent data) {
        if (pendingType == null) return;
        PermissionType type = pendingType;
        pendingType = null;

        if (hasPermission(type)) {
            runOnUiThread(() -> {
                if (callback != null)
                    callback.onPermissionGranted(type);
            });
        } else {
            runOnUiThread(() -> {
                if (callback != null)
                    callback.onPermissionDenied(type, false);
                if (type == PermissionType.UNKNOWN_SOURCES) {
                    Toast.makeText(activity, activity.getString(R.string.allow_unknown_sources), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void runOnUiThread(Runnable r) {
        if (activity != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (activity.isDestroyed() || activity.isFinishing()) return;
            }
            activity.runOnUiThread(r);
        }
    }
}
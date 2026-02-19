package org.levimc.launcher.core.mods;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.provider.OpenableColumns;
import android.util.Log;

import org.levimc.launcher.R;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.core.versions.VersionManager;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;
import org.levimc.launcher.ui.views.MainViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class FileHandler {
    private static final String TAG = "FileHandler";
    private final Context context;
    private final MainViewModel modManager;
    private String targetPath;

    public interface FileOperationCallback {
        void onSuccess(int processedFiles);

        void onError(String errorMessage);

        void onProgressUpdate(int progress);
    }

    public FileHandler(Context context, MainViewModel modManager, VersionManager version) {
        this.context = context;
        this.modManager = modManager;

        GameVersion currentVersion = version.getSelectedVersion();
        if (currentVersion != null && currentVersion.modsDir != null) {
            this.targetPath = currentVersion.modsDir.getAbsolutePath();
        } else {
            this.targetPath = null;
        }
    }

    public void processIncomingFilesWithConfirmation(Intent intent, FileOperationCallback callback, boolean isButtonClick) {
        List<Uri> fileUris = extractFileUris(intent);

        List<Uri> soUris = new ArrayList<>();
        for (Uri uri : fileUris) {
            String fileName = resolveFileName(uri);
            if (fileName != null && fileName.endsWith(".so")) {
                soUris.add(uri);
            }
        }

        if (soUris.isEmpty()) {
            if (isButtonClick) {
                new CustomAlertDialog(context)
                        .setTitleText(context.getString(R.string.invalid_mod_file))
                        .setTitleColor(Color.RED)
                        .setMessage(context.getString(R.string.invalid_mod_file_reason))
                        .setUseBorderedBackground(true)
                        .setBlurBackground(true)
                        .setPositiveButton((context.getString(R.string.confirm)), (d) -> {})
                        .show();
            }
            return;
        }

        new CustomAlertDialog(context)
                .setTitleText(context.getString(R.string.import_confirmation_title))
                .setMessage(context.getString(R.string.import_confirmation_message, soUris.size()))
                .setPositiveButton((context.getString(R.string.confirm)), (d) -> handleFilesWithOverwriteCheck(soUris, callback))
                .setNegativeButton(context.getString(R.string.cancel), (d) -> {
                    if (callback != null)
                        callback.onError(context.getString(R.string.user_cancelled));
                })
                .show();
    }

    private void handleFilesWithOverwriteCheck(List<Uri> fileUris, FileOperationCallback callback) {
        new Thread(() -> {
            int processed = 0;
            int total = fileUris.size();

            for (Uri uri : fileUris) {
                String fileName = resolveFileName(uri);
                if (fileName == null || !fileName.endsWith(".so")) continue;

                if (targetPath == null) {
                    return;
                }
                File targetDir = new File(targetPath);
                if (!createDirectoryIfNeeded(targetDir)) continue;

                File destinationFile = new File(targetDir, fileName);
                if (destinationFile.exists()) {
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    final boolean[] userChoice = new boolean[1];
                    final boolean[] decisionMade = new boolean[]{false};

                    mainHandler.post(() -> {
                        new CustomAlertDialog(context)
                                .setTitleText(context.getString(R.string.overwrite_file_title))
                                .setMessage(context.getString(R.string.overwrite_file_message, fileName))
                                .setPositiveButton(context.getString(R.string.overwrite), (dlg) -> {
                                    userChoice[0] = true;
                                    decisionMade[0] = true;
                                    synchronized (this) {
                                        this.notify();
                                    }
                                })
                                .setNegativeButton(context.getString(R.string.skip), (dlg) -> {
                                    userChoice[0] = false;
                                    decisionMade[0] = true;
                                    synchronized (this) {
                                        this.notify();
                                    }
                                }).show();
                    });


                    synchronized (this) {
                        try {
                            while (!decisionMade[0]) {
                                this.wait();
                            }
                        } catch (InterruptedException e) {
                            Log.e(TAG, "用户选择中断", e);
                        }
                    }

                    if (!userChoice[0]) {
                        processed++;
                        if (callback != null) {
                            int finalProcessed = processed;
                            new Handler(Looper.getMainLooper()).post(() -> {
                                callback.onProgressUpdate((finalProcessed * 100) / total);
                            });
                        }
                        continue;
                    }
                }

                try {
                    if (copyFileWithStream(uri, destinationFile)) {
                        processed++;
                        if (callback != null) {
                            int finalProcessed = processed;
                            new Handler(Looper.getMainLooper()).post(() -> {
                                callback.onProgressUpdate((finalProcessed * 100) / total);
                            });
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "导入失败: " + fileName, e);
                }
            }

            Handler mainHandler = new Handler(Looper.getMainLooper());
            int finalProcessedCount = processed;
            mainHandler.post(() -> {
                modManager.refreshMods();
                if (callback != null)
                    callback.onSuccess(finalProcessedCount);
            });

        }).start();
    }

    private List<Uri> extractFileUris(Intent intent) {
        List<Uri> uris = new ArrayList<>();
        if (intent == null) return uris;

        ClipData clipData = intent.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                uris.add(clipData.getItemAt(i).getUri());
            }
        } else if (intent.getData() != null) {
            uris.add(intent.getData());
        }
        return uris;
    }

    private String resolveFileName(Uri uri) {
        String defaultName = "unknown_" + System.currentTimeMillis() + ".so";
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0)
                        return cursor.getString(nameIndex);
                }
            } finally {
                cursor.close();
            }
        }
        return defaultName;
    }

    private boolean createDirectoryIfNeeded(File dir) {
        return dir.exists() || dir.mkdirs();
    }

    private boolean copyFileWithStream(Uri source, File destination) throws IOException {
        try (InputStream in = context.getContentResolver().openInputStream(source);
             OutputStream out = new FileOutputStream(destination)) {

            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return true;
        }
    }
}
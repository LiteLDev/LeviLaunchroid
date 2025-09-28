package com.mojang.minecraftpe;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.StatFs;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import com.mojang.minecraftpe.utils.FileHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

/**
 * 13.08.2022
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class WorldRecovery {
    private ContentResolver mContentResolver;
    private Context mContext;
    private int mTotalFilesToCopy = 0;
    private long mTotalBytesRequired = 0;

    public WorldRecovery(Context context, ContentResolver contentResolver) {
        this.mContext = null;
        this.mContentResolver = null;
        this.mContext = context;
        this.mContentResolver = contentResolver;
    }

    private static native void nativeComplete();

    private static native void nativeError(String error, long bytesRequired, long bytesAvailable);

    private static native void nativeUpdate(String status, int filesTotal, int filesCompleted, long bytesTotal, long bytesCompleted);

    public String migrateFolderContents(String srcURIString, String destFolderString) {
        final DocumentFile fromTreeUri = DocumentFile.fromTreeUri(this.mContext, Uri.parse(srcURIString));
        if (fromTreeUri == null) {
            return "Could not resolve URI to a DocumentFile tree: " + srcURIString;
        } else if (!fromTreeUri.isDirectory()) {
            return "Root file of URI is not a directory: " + srcURIString;
        } else {
            final File file = new File(destFolderString);
            if (!file.isDirectory()) {
                return "Destination folder does not exist: " + destFolderString;
            }
            String[] list = file.list();
            Objects.requireNonNull(list);
            if (list.length != 0) {
                return "Destination folder is not empty: " + destFolderString;
            }
            new Thread(() -> doMigrateFolderContents(fromTreeUri, file)).start();
            return "";
        }
    }

    public void doMigrateFolderContents(DocumentFile root, @NonNull File destFolder) {
        ArrayList<DocumentFile> arrayList = new ArrayList<>();
        mTotalFilesToCopy = 0;
        long bytesAvailable = 0;
        mTotalBytesRequired = 0L;
        generateCopyFilesRecursively(arrayList, root);
        long availableBytes = new StatFs(destFolder.getAbsolutePath()).getAvailableBytes();
        long bytesTotal = mTotalBytesRequired;
        if (bytesTotal >= availableBytes) {
            nativeError("Insufficient space", bytesTotal, availableBytes);
            return;
        }
        String path = root.getUri().getPath();
        String tmpDirPath = destFolder + "_temp";
        File tmpDir = new File(tmpDirPath);
        Iterator<DocumentFile> it = arrayList.iterator();
        long bytesCompleted = 0;
        int filesCompleted = 0;
        while (it.hasNext()) {
            DocumentFile next = it.next();
            String dir = tmpDirPath + next.getUri().getPath().substring(path.length());
            if (next.isDirectory()) {
                File file2 = new File(dir);
                if (!file2.isDirectory()) {
                    if (!file2.mkdirs()) {
                        nativeError("Could not create directory: " + dir, bytesAvailable, bytesAvailable);
                        return;
                    }
                } else {
                }
            } else {
                String status = "Copying: " + dir;
                filesCompleted++;
                nativeUpdate(status, mTotalFilesToCopy, filesCompleted, mTotalBytesRequired, bytesCompleted);
                try {
                    FileHelper.writeToFile(new File(dir), mContentResolver.openInputStream(next.getUri()));
                } catch (IOException e) {
                    nativeError(e.getMessage(), 0L, 0L);
                    return;
                }
            }
        }
        if (destFolder.delete()) {
            if (tmpDir.renameTo(destFolder)) {
                nativeComplete();
                return;
            } else if (destFolder.mkdir()) {
                nativeError("Could not replace destination directory: " + destFolder.getAbsolutePath(), 0L, 0L);
                return;
            } else {
                nativeError("Could not recreate destination directory after failed replace: " + destFolder.getAbsolutePath(), 0L, 0L);
                return;
            }
        }
        nativeError("Could not delete empty destination directory: " + destFolder.getAbsolutePath(), 0L, 0L);
    }

    private void generateCopyFilesRecursively(ArrayList<DocumentFile> result, @NonNull DocumentFile root) {
        for (DocumentFile documentFile : root.listFiles()) {
            result.add(documentFile);
            if (documentFile.isDirectory()) {
                generateCopyFilesRecursively(result, documentFile);
            } else {
                mTotalBytesRequired += documentFile.length();
                mTotalFilesToCopy++;
            }
        }
    }
}

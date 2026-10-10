package org.levimc.launcher.util;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class StorageAccess {
    private StorageAccess() {
    }

    public static final class Document {
        public final Uri uri;
        public final String name;
        public final long size;

        public Document(Uri uri, String name, long size) {
            this.uri = uri;
            this.name = name;
            this.size = Math.max(0L, size);
        }
    }

    public static Intent downloadsPicker(boolean multiple) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                Uri.parse("content://com.android.providers.downloads.documents/root/downloads"));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        return intent;
    }

    public static List<Uri> selectedUris(Intent data) {
        Set<Uri> uris = new LinkedHashSet<>();
        if (data != null) {
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    if (uri != null) uris.add(uri);
                }
            }
            if (data.getData() != null) uris.add(data.getData());
        }
        return new ArrayList<>(uris);
    }

    public static void retainReadPermission(Context context, Intent data, Uri uri) {
        if (data == null || uri == null) return;
        int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
        if (flags == 0 || (data.getFlags() & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) == 0) return;
        try {
            context.getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (SecurityException ignored) {
        }
    }

    public static Document readDocument(Context context, Uri uri) throws IOException {
        String name = uri.getLastPathSegment();
        long size = 0L;
        try (Cursor cursor = context.getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) throw new IOException("Cannot read selected file");
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (nameIndex >= 0) name = cursor.getString(nameIndex);
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex);
        } catch (RuntimeException error) {
            throw new IOException("Cannot read selected file", error);
        }
        if (name == null || name.isEmpty()) throw new IOException("Selected file has no name");
        return new Document(uri, name, size);
    }

}

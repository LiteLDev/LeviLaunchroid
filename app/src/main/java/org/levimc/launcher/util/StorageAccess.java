package org.levimc.launcher.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class StorageAccess {
    private StorageAccess() {
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

}

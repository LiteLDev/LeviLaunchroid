package com.mojang.minecraftpe.python;

import android.content.res.AssetManager;
import android.util.Log;
import androidx.annotation.NonNull;
import com.mojang.minecraftpe.utils.FileHelper;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class PythonPackageLoader {
    private final AssetManager assetManager;
    private final File destination;

    public PythonPackageLoader(AssetManager assetManager2, File file) {
        this.assetManager = assetManager2;
        this.destination = new File(file + "/python");
    }

    private boolean shouldUnpack() throws Throwable {
        File file = new File(this.destination.getAbsolutePath() + "/python-tracker.txt");
        String[] list = this.assetManager.list("python-tracker.txt");
        if (!file.exists() || list == null || list.length == 0) {
            return true;
        }
        return !new BufferedReader(new FileReader(file)).readLine().equals(new BufferedReader(new InputStreamReader(this.assetManager.open(list[0]), StandardCharsets.UTF_8)).readLine());
    }

    private void copy(InputStream inputStream, @NonNull File file) throws Throwable {
        File parentFile = file.getParentFile();
        if (parentFile != null) {
            createDirectory(parentFile);
            FileHelper.writeToFile(file, inputStream);
        }
    }

    public void unpack() {
        try {
            if (createDirectory(this.destination) != CreateDirectory.Exists || shouldUnpack()) {
                unpackAssetPrefix("python", this.destination);
                return;
            }
        } catch (Throwable th) {
            Log.e("PythonPackageLoader", th.toString());
        }
    }

    private CreateDirectory createDirectory(@NonNull File file) throws Throwable {
        if (file.exists()) {
            return CreateDirectory.Exists;
        }
        if (file.mkdirs()) {
            return CreateDirectory.Created;
        }
        throw new IOException("Failed to mkdir " + file.getAbsolutePath());
    }

    private void delete(@NonNull File file) {
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            if (listFiles != null) {
                for (File f : listFiles) {
                    delete(f);
                }
            }
        }
        file.delete();
    }

    private void traverse(@NonNull File file) {
        if (file.exists()) {
            File[] listFiles = file.listFiles();
            if (listFiles != null) {
                for (File f : listFiles) {
                    if (f.isDirectory()) {
                        traverse(f);
                    } else {
                    }
                }
            }
        }
    }

    private void unpackAssetPrefix(String path, @NonNull File file) throws Throwable {
        delete(file);
        String[] list = assetManager.list(path);
        if (list == null || list.length == 0) {
            throw new IOException("No assets at prefix " + path);
        }
        for (String fileName : list) {
            unpackAssetPath(path + '/' + fileName, path.length(), file);
        }
    }

    private void unpackAssetPath(String path, int i, File file) throws Throwable {
        String[] list = assetManager.list(String.valueOf(path));
        if (list == null) {
            throw new IOException("Unable to list assets at path " + path + '/');
        } else if (list.length == 0) {
            copy(assetManager.open(path), new File(file.getAbsolutePath() + '/' + path.substring(i)));
        } else {
            for (String fileName : list) {
                unpackAssetPath(path + '/' + fileName, i, file);
            }
        }
    }

    public enum CreateDirectory {
        Created,
        Exists
    }
}

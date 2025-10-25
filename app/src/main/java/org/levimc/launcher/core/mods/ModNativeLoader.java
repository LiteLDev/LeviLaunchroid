package org.levimc.launcher.core.mods;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ModNativeLoader {
    private static final String TAG = "ModNativeLoader";

    static { System.loadLibrary("dlopenglobal"); }
    public static native long dlopenGlobal(String path);
    public static native void initNativeLibraryDirAndModsDir(String nativeLibDir, String modsDir);

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public static void loadEnabledSoMods(ModManager modManager, File cacheDir) {
        List<Mod> mods = modManager.getMods();
        for (Mod mod : mods) {
            if (!mod.isEnabled()) continue;
            File src = new File(modManager.getCurrentVersion().modsDir, mod.getFileName());
            File dir = new File(cacheDir, "mods");
            if (!dir.exists()) dir.mkdirs();
            File dst = new File(dir, mod.getFileName());
            try {
                copyFile(src, dst);
                dlopenGlobal(dst.getAbsolutePath());
                Log.i(TAG, "Loaded so: " + dst.getName());
            } catch (IOException | UnsatisfiedLinkError e) {
                Log.e(TAG, "Can't load " + src.getName() + ": " + e.getMessage());
            }
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }
}

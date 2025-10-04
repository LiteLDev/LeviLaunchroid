package com.mojang.minecraftpe;

import androidx.annotation.Nullable;
import org.jetbrains.annotations.Contract;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class CrashManager {
    public String getCrashUploadURI() {
        return "http://localhost:1234/";
    }

    public String getExceptionUploadURI() {
        return "http://localhost:1234/";
    }

    @Nullable
    @Contract(pure = true)
    private String uploadCrashFile(String filePath, String sessionID, String sentryParametersJSON) {
        return null;
    }
}

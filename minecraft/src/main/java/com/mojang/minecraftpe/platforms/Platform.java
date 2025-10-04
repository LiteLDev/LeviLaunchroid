package com.mojang.minecraftpe.platforms;

import android.view.View;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public abstract class Platform {
    @NotNull
    @Contract("_ -> new")
    public static Platform createPlatform(boolean initEventHandler) {
        return new Platform21(initEventHandler);
    }

    public abstract String getABIS();

    public abstract void onAppStart(View view);

    public abstract void onViewFocusChanged(boolean z);

    public abstract void onVolumePressed();
}
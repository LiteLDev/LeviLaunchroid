package org.levimc.launcher.core.minecraft.mcpelauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import androidx.preference.PreferenceManager;

import com.google.android.material.color.DynamicColors;

import org.levimc.launcher.core.minecraft.pesdk.PESdk;

public class Application extends android.app.Application {
    public static Context context;
    public static SharedPreferences preferences;
    public static PESdk mPESdk;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        mPESdk = new PESdk(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        DynamicColors.applyToActivitiesIfAvailable(this);
    }

    public static Context getContext() {
        if (context == null) {
            context = new Application();
        }
        return context;
    }

    public AssetManager getAssets() {
        return mPESdk.getMinecraftInfo().getAssets();
    }
}

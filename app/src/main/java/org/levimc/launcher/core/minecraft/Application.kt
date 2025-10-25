package org.levimc.launcher.core.minecraft

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import org.levimc.launcher.core.mods.ModNativeLoader
import org.levimc.launcher.settings.FeatureSettings

class LauncherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        FeatureSettings.init(applicationContext)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        ModNativeLoader.initNativeLibraryDirAndModsDir(applicationInfo.nativeLibraryDir, "/data/data/org.levimc.launcher/cache/mods")
    }

    companion object {
        @JvmStatic
        lateinit var context: Context
            private set

        @JvmStatic
        lateinit var preferences: SharedPreferences
            private set
    }
}
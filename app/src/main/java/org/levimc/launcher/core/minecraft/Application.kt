package org.levimc.launcher.core.minecraft

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import org.levimc.launcher.settings.FeatureSettings
import java.io.File

class LauncherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        FeatureSettings.init(applicationContext)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        try {
            System.loadLibrary("levi_init")
            val modsDir = File(cacheDir, "mods")
            if (!modsDir.exists()) modsDir.mkdirs()
            Log.d("LauncherApplication", "Mods path: ${modsDir.absolutePath}")
            nativeSetupRuntime(modsDir.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    external fun nativeSetupRuntime(modsPath: String)

    companion object {
        @JvmStatic
        lateinit var context: Context
            private set

        @JvmStatic
        lateinit var preferences: SharedPreferences
            private set
    }
}
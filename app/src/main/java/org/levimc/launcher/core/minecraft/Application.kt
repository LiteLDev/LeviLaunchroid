package org.levimc.launcher.core.minecraft

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.levimc.launcher.core.crash.CrashReporter
import org.levimc.launcher.settings.FeatureSettings
import org.levimc.launcher.ui.dialogs.LogcatOverlayManager

class LauncherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        FeatureSettings.init(applicationContext)
        CrashReporter.init(this)
        if (Application.getProcessName().endsWith(":crash")) return

        LogcatOverlayManager.init(this)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
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

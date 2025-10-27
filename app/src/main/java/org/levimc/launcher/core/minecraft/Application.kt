package org.levimc.launcher.core.minecraft

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import androidx.preference.PreferenceManager
import org.levimc.launcher.BuildConfig
import org.levimc.launcher.settings.FeatureSettings
import xcrash.ICrashCallback
import xcrash.XCrash
import java.io.File
import android.content.Intent
import org.levimc.launcher.ui.activities.CrashActivity
import org.levimc.launcher.ui.views.LogcatOverlayManager

class LauncherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        FeatureSettings.init(applicationContext)
        LogcatOverlayManager.init(this)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        val callback: ICrashCallback = ICrashCallback { logPath, emergency ->
            try {
                val i = Intent(applicationContext, CrashActivity::class.java).apply {
                    putExtra("LOG_PATH", logPath)
                    putExtra("EMERGENCY", emergency)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                applicationContext.startActivity(i)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        XCrash.init(this, XCrash.InitParameters().apply {
            setAppVersion(BuildConfig.VERSION_NAME)
            setLogDir(File( Environment.getExternalStorageDirectory(), "games/org.levimc/crash_logs").absolutePath)
            setNativeCallback(callback)
            setJavaCallback(callback)
            setAnrCallback(callback)
            setJavaRethrow(false)
            setNativeRethrow(false)
            setAnrRethrow(false)
        })

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
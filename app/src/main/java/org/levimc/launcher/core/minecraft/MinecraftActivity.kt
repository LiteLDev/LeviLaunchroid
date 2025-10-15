package org.levimc.launcher.core.minecraft

import android.content.Intent
import android.content.res.AssetManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.mojang.minecraftpe.MainActivity
import org.levimc.launcher.core.versions.GameVersion
import org.levimc.launcher.settings.FeatureSettings
import java.io.File

class MinecraftActivity : MainActivity() {

    private lateinit var gameManager: GamePackageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            val versionDir = intent.getStringExtra("MC_PATH")
            val versionCode = intent.getStringExtra("MINECRAFT_VERSION") ?: ""
            val versionDirName = intent.getStringExtra("MINECRAFT_VERSION_DIR") ?: ""
            val isInstalled = intent.getBooleanExtra("IS_INSTALLED", false)
            val isIsolated = FeatureSettings.getInstance().isVersionIsolationEnabled()

            val version = if (isIsolated && !versionDir.isNullOrEmpty()) {
                GameVersion(
                    versionDirName,
                    versionCode,
                    versionCode,
                    File(versionDir),
                    isInstalled,
                    MinecraftLauncher.MC_PACKAGE_NAME,
                    ""
                )
            } else if (!versionCode.isNullOrEmpty()) {
                GameVersion(
                    versionDirName,
                    versionCode,
                    versionCode,
                    File(versionDir ?: ""),
                    true,
                    MinecraftLauncher.MC_PACKAGE_NAME,
                    ""
                )
            } else {
                null
            }

            gameManager = GamePackageManager.getInstance(applicationContext, version)

            try {
                System.loadLibrary("preloader")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load preloader: ${e.message}")
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load game: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        super.onCreate(savedInstanceState)
        MinecraftActivityState.onCreated(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        MinecraftActivityState.onResumed()
    }

    override fun onPause() {
        MinecraftActivityState.onPaused()
        super.onPause()
    }

    override fun onDestroy() {
        MinecraftActivityState.onDestroyed()
        super.onDestroy()

        val intent = Intent(applicationContext, org.levimc.launcher.ui.activities.MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)

        finishAndRemoveTask()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    override fun getAssets(): AssetManager {
        return if (::gameManager.isInitialized) {
            gameManager.getAssets()
        } else {
            super.getAssets()
        }
    }

    override fun getFilesDir(): File {
        val mcPath = intent.getStringExtra("MC_PATH")
        val isVersionIsolationEnabled = FeatureSettings.getInstance().isVersionIsolationEnabled()

        return if (isVersionIsolationEnabled && !mcPath.isNullOrEmpty()) {
            val filesDir = File(mcPath, "games/com.mojang")
            if (!filesDir.exists()) {
                filesDir.mkdirs()
            }
            filesDir
        } else {
            super.getFilesDir()
        }
    }

    override fun getDataDir(): File {
        val mcPath = intent.getStringExtra("MC_PATH")
        val isVersionIsolationEnabled = FeatureSettings.getInstance().isVersionIsolationEnabled()

        return if (isVersionIsolationEnabled && !mcPath.isNullOrEmpty()) {
            val dataDir = File(mcPath)
            if (!dataDir.exists()) {
                dataDir.mkdirs()
            }
            dataDir
        } else {
            super.getDataDir()
        }
    }

    override fun getExternalFilesDir(type: String?): File? {
        val mcPath = intent.getStringExtra("MC_PATH")
        val isVersionIsolationEnabled = FeatureSettings.getInstance().isVersionIsolationEnabled()

        return if (isVersionIsolationEnabled && !mcPath.isNullOrEmpty()) {
            val externalDir = if (type != null) {
                File(mcPath, "games/com.mojang/$type")
            } else {
                File(mcPath, "games/com.mojang")
            }
            if (!externalDir.exists()) {
                externalDir.mkdirs()
            }
            externalDir
        } else {
            super.getExternalFilesDir(type)
        }
    }

    override fun getDatabasePath(name: String): File {
        val mcPath = intent.getStringExtra("MC_PATH")
        val isVersionIsolationEnabled = FeatureSettings.getInstance().isVersionIsolationEnabled()

        return if (isVersionIsolationEnabled && !mcPath.isNullOrEmpty()) {
            val dbDir = File(mcPath, "databases")
            if (!dbDir.exists()) {
                dbDir.mkdirs()
            }
            File(dbDir, name)
        } else {
            super.getDatabasePath(name)
        }
    }

    override fun getCacheDir(): File {
        val mcPath = intent.getStringExtra("MC_PATH")
        val isVersionIsolationEnabled = FeatureSettings.getInstance().isVersionIsolationEnabled()

        return if (isVersionIsolationEnabled && !mcPath.isNullOrEmpty()) {
            val cacheDir = File(mcPath, "cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            cacheDir
        } else {
            super.getCacheDir()
        }
    }

    companion object {
        private const val TAG = "MinecraftActivity"
    }
}
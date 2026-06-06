package org.levimc.launcher.core.minecraft

import android.content.Intent
import android.content.res.AssetManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import com.mojang.minecraftpe.MainActivity
import org.levimc.launcher.core.mods.inbuilt.overlay.InbuiltOverlayManager
import org.levimc.launcher.core.versions.GameVersion
import org.levimc.launcher.settings.FeatureSettings
import java.io.File

class MinecraftActivity : MainActivity() {

    private lateinit var gameManager: GamePackageManager
    private var overlayManager: InbuiltOverlayManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            val versionDir = intent.getStringExtra("MC_PATH")
            val versionCode = intent.getStringExtra("MINECRAFT_VERSION") ?: ""
            val versionDirName = intent.getStringExtra("MINECRAFT_VERSION_DIR") ?: ""
            val isInstalled = intent.getBooleanExtra("IS_INSTALLED", false)

            val version = if (!versionDir.isNullOrEmpty()) {
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
                    null,
                    isInstalled,
                    MinecraftLauncher.MC_PACKAGE_NAME,
                    ""
                )
            } else {
                null
            }

            gameManager = GamePackageManager.getInstance(applicationContext, version)

            try {
                System.loadLibrary("preloader")
            } catch (e: Exception) {}

            if (!gameManager.loadLibrary("minecraftpe")) {
                throw RuntimeException("Failed to load libminecraftpe.so")
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load game: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        super.onCreate(savedInstanceState)
        
        val launchVertically = intent.getBooleanExtra("LAUNCH_VERTICALLY", false)
        if (launchVertically) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        
        org.levimc.launcher.preloader.PreloaderInput.setActivity(this)
        MinecraftActivityState.onCreated(this)
        getSharedPreferences("LauncherPrefs", MODE_PRIVATE)
            .edit()
            .putBoolean("game_verified", true)
            .apply()
    }

    private fun startInbuiltModServices() {
        overlayManager = InbuiltOverlayManager(this)
        overlayManager?.showEnabledOverlays()
    }

    private fun stopInbuiltModServices() {
        overlayManager?.hideAllOverlays()
        overlayManager = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        MinecraftActivityState.onResumed()

        if (overlayManager == null) {
            startInbuiltModServices()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val unicodeChar = event.unicodeChar
            if (unicodeChar != 0) {
                if (org.levimc.launcher.preloader.PreloaderInput.onKeyChar(unicodeChar)) {
                    return true
                }
            }
            if (org.levimc.launcher.preloader.PreloaderInput.onKeyDown(event.keyCode)) {
                return true
            }
        }

        overlayManager?.let { manager ->
            if (manager.handleKeyEvent(event.keyCode, event.action)) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val actionIndex = event.actionIndex
        if (org.levimc.launcher.preloader.PreloaderInput.onTouch(
                event.actionMasked,
                event.getPointerId(actionIndex),
                event.getX(actionIndex),
                event.getY(actionIndex)
            )) {
            return true
        }

        overlayManager?.handleTouchEvent(event)

        if (org.levimc.launcher.core.mods.inbuilt.overlay.VirtualCursorMod.isActive()) {
            org.levimc.launcher.core.mods.inbuilt.overlay.VirtualCursorMod.processTouchEvent(event, this)
            return true
        }

        return super.dispatchTouchEvent(event)
    }

    fun dispatchGenericMotionEventToGame(event: MotionEvent): Boolean {
        return super.dispatchGenericMotionEvent(event)
    }

    fun dispatchTouchEventToGame(event: MotionEvent): Boolean {
        return super.dispatchTouchEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_BUTTON_PRESS ||
            event.action == MotionEvent.ACTION_BUTTON_RELEASE) {
            overlayManager?.handleMouseEvent(event)
        }

        if (event.action == MotionEvent.ACTION_SCROLL) {
            val vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            if (vScroll != 0f) {
                overlayManager?.let { manager ->
                    if (manager.handleScrollEvent(vScroll)) {
                        return true
                    }
                }
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun onPause() {
        MinecraftActivityState.onPaused()
        super.onPause()
    }

    override fun onDestroy() {
        org.levimc.launcher.preloader.PreloaderInput.clearActivity()
        MinecraftActivityState.onDestroyed()
        stopInbuiltModServices()
        super.onDestroy()

        if (isFinishing) {
            val intent = Intent(applicationContext, org.levimc.launcher.ui.activities.MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)

            finishAndRemoveTask()
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    override fun getAssets(): AssetManager {
        return if (::gameManager.isInitialized) {
            gameManager.getAssets()
        } else {
            super.getAssets()
        }
    }

    override fun getFilesDir(): File {
        val mcPath = intent?.getStringExtra("MC_PATH")
        val isIsolated = intent?.getBooleanExtra("VERSION_ISOLATION", false) ?: false

        return if (isIsolated && !mcPath.isNullOrEmpty()) {
            val filesDir = File(mcPath, "games/com.mojang")
            if (!filesDir.exists()) {
                filesDir.mkdirs()
            }
            filesDir
        } else {
            super.getFilesDir()
        }
    }

    override fun tick() {
        super.tick()
        overlayManager?.tick()
    }

    override fun getDataDir(): File {
        val mcPath = intent?.getStringExtra("MC_PATH")
        val isIsolated = intent?.getBooleanExtra("VERSION_ISOLATION", false) ?: false

        return if (isIsolated && !mcPath.isNullOrEmpty()) {
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
        val mcPath = intent?.getStringExtra("MC_PATH")
        val isIsolated = intent?.getBooleanExtra("VERSION_ISOLATION", false) ?: false

        return if (isIsolated && !mcPath.isNullOrEmpty()) {
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
        val mcPath = intent?.getStringExtra("MC_PATH")
        val isIsolated = intent?.getBooleanExtra("VERSION_ISOLATION", false) ?: false

        return if (isIsolated && !mcPath.isNullOrEmpty()) {
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
        val mcPath = intent?.getStringExtra("MC_PATH")
        val isIsolated = intent?.getBooleanExtra("VERSION_ISOLATION", false) ?: false

        return if (isIsolated && !mcPath.isNullOrEmpty()) {
            val cacheDir = File(mcPath, "cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            cacheDir
        } else {
            super.getCacheDir()
        }
    }

    fun showSoftKeyboard() {
        runOnUiThread {
            val inputMethodManager = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            val view = window.decorView.findFocus() ?: window.decorView
            view.requestFocus()
            inputMethodManager.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            inputMethodManager.toggleSoftInput(android.view.inputmethod.InputMethodManager.SHOW_FORCED, 0)
        }
    }

    fun hideSoftKeyboard() {
        runOnUiThread {
            val inputMethodManager = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            val view = window.decorView.findFocus() ?: window.decorView
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}

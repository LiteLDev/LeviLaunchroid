package org.levimc.launcher.core.minecraft

import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import com.mojang.minecraftpe.MainActivity
import org.levimc.launcher.core.crash.CrashReporter
import org.levimc.launcher.core.mods.ModManager
import org.levimc.launcher.core.mods.inbuilt.overlay.InbuiltOverlayManager
import java.io.File

class MinecraftActivity : MainActivity() {

    private lateinit var gameManager: GamePackageManager
    private lateinit var trace: LaunchTrace
    private var overlayManager: InbuiltOverlayManager? = null
    private var normalExitPrepared = false
    private var normalExitRestartScheduled = false
    private var gameRuntimeStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        trace = LaunchTrace.ensure(intent)
        trace.mark("MinecraftActivity onCreate entered")
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(resolveLaunchBackgroundColor()))

        if (savedInstanceState != null) {
            trace.mark("MinecraftActivity finishing restored instance")
            gameRuntimeStarted = true
            super.onCreate(null)
            finish()
            return
        }

        try {
            val preparedRuntime = MinecraftLaunchSession.getPreparedRuntime()
                ?: MinecraftRuntimePreparer.prepare(applicationContext, intent)
            gameManager = preparedRuntime.gameManager
            trace.mark("Prepared runtime consumed")
        } catch (throwable: Throwable) {
            trace.error("MinecraftActivity prepare failed", formatLaunchFailure(throwable))
            returnToLauncherAfterLaunchFailure()
            return
        }
        trace.mark("Native mod enable started")
        ModManager.enableLoadedMods()
        trace.mark("Native mod enable finished")
        trace.mark("Mojang MainActivity super.onCreate starting")
        try {
            gameRuntimeStarted = true
            super.onCreate(savedInstanceState)
        } catch (throwable: Throwable) {
            trace.error("Mojang MainActivity super.onCreate failed", formatLaunchFailure(throwable))
            returnToLauncherAfterLaunchFailure()
            return
        }
        trace.mark("Mojang MainActivity super.onCreate finished")
        
        val launchVertically = intent.getBooleanExtra("LAUNCH_VERTICALLY", false)
        if (launchVertically) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        
        org.levimc.launcher.preloader.PreloaderInput.setActivity(this)
        MinecraftActivityState.onCreated(this)
        trace.mark("MinecraftActivity onCreate finished")
    }

    private fun returnToLauncherAfterLaunchFailure() {
        gameRuntimeStarted = false
        MinecraftLaunchSession.clear()
        MinecraftProcessRestarter.restartLauncherAfterMinecraftExit(this)
        finish()
    }

    private fun formatLaunchFailure(throwable: Throwable): String {
        return throwable.message ?: throwable.javaClass.simpleName
    }

    private fun resolveLaunchBackgroundColor(): Int {
        val typedValue = android.util.TypedValue()
        return if (theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)) {
            typedValue.data
        } else {
            Color.BLACK
        }
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
        setIntent(intent)
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing) {
            normalExitPrepared = false
            normalExitRestartScheduled = false
        }
        MinecraftActivityState.onResumed(this)

        if (overlayManager == null) {
            startInbuiltModServices()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val unicodeChar = event.unicodeChar
        if (event.action == KeyEvent.ACTION_UP) {
            if (org.levimc.launcher.preloader.PreloaderInput.onKeyEvent(event.keyCode, unicodeChar, false)) {
                return true
            }
        }

        if (event.action == KeyEvent.ACTION_DOWN) {
            if (org.levimc.launcher.preloader.PreloaderInput.onKeyEvent(event.keyCode, unicodeChar, true)) {
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
        if (event.actionMasked == MotionEvent.ACTION_BUTTON_PRESS ||
            event.actionMasked == MotionEvent.ACTION_BUTTON_RELEASE) {
            
            val isDown = event.actionMasked == MotionEvent.ACTION_BUTTON_PRESS
            if (org.levimc.launcher.preloader.PreloaderInput.onMouse(event.actionButton, isDown)) {
                return true
            }
            
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
        val shouldRestartAfterNormalExit = shouldRestartAfterNormalExit()
        if (shouldRestartAfterNormalExit) {
            ModManager.disableAndUnloadLoadedMods()
            prepareNormalExitCleanup()
            scheduleNormalExitProcessRestart()
        }
        MinecraftActivityState.onPaused(this)
        super.onPause()
    }

    override fun onDestroy() {
        ModManager.disableAndUnloadLoadedMods()

        val shouldPrepareNormalExit = shouldRestartAfterNormalExit()
        if (shouldPrepareNormalExit) {
            prepareNormalExitCleanup()
        }

        org.levimc.launcher.preloader.PreloaderInput.clearActivity()
        MinecraftActivityState.onDestroyed(this)
        MinecraftLaunchSession.clear()
        stopInbuiltModServices()

        try {
            super.onDestroy()
        } finally {
            if (shouldPrepareNormalExit) {
                scheduleNormalExitProcessRestart()
            }
        }
    }

    private fun shouldRestartAfterNormalExit(): Boolean {
        return gameRuntimeStarted && isFinishing && !CrashReporter.isHandlingCrash()
    }

    private fun prepareNormalExitCleanup() {
        if (normalExitPrepared) return
        normalExitPrepared = true
    }

    private fun scheduleNormalExitProcessRestart() {
        if (normalExitRestartScheduled) return
        normalExitRestartScheduled = true

        MinecraftProcessRestarter.restartLauncherAfterMinecraftExit(this)
    }

    override fun getAssets(): AssetManager {
        return if (::gameManager.isInitialized) {
            gameManager.getAssets()
        } else {
            super.getAssets()
        }
    }

    override fun getFilesDir(): File {
        return resolveStorageDir(MinecraftLauncher.EXTRA_STORAGE_FILES_DIR, super.getFilesDir())
    }

    override fun tick() {
        super.tick()
        overlayManager?.tick()
    }

    override fun getDataDir(): File {
        return resolveStorageDir(MinecraftLauncher.EXTRA_STORAGE_DATA_DIR, super.getDataDir())
    }

    override fun getExternalFilesDir(type: String?): File? {
        val baseDir = resolveStorageDir(
            MinecraftLauncher.EXTRA_STORAGE_EXTERNAL_FILES_DIR,
            super.getExternalFilesDir(null)
        )
        return if (type.isNullOrEmpty()) {
            baseDir
        } else {
            File(baseDir, type).also { it.mkdirs() }
        }
    }

    override fun getInternalStoragePath(): String {
        return getFilesDir().absolutePath
    }

    override fun getExternalStoragePath(): String {
        return (getExternalFilesDir(null) ?: getFilesDir()).absolutePath
    }

    private fun resolveStorageDir(extraName: String, fallback: File?): File {
        val path = intent?.getStringExtra(extraName)
        val dir = if (!path.isNullOrEmpty()) File(path) else fallback ?: super.getFilesDir()
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    override fun getDatabasePath(name: String): File {
        val dbDir = File(getDataDir(), "databases")
        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }
        return File(dbDir, name)
    }

    override fun getCacheDir(): File {
        return resolveStorageDir(MinecraftLauncher.EXTRA_STORAGE_CACHE_DIR, super.getCacheDir())
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

package org.levimc.launcher.core.minecraft

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import com.mojang.minecraftpe.MainActivity
import org.levimc.launcher.core.crash.CrashReporter
import org.levimc.launcher.core.mods.ModManager
import org.levimc.launcher.core.mods.inbuilt.overlay.InbuiltOverlayManager
import org.levimc.launcher.preloader.PreloaderInput
import java.io.File

class MinecraftActivity : MainActivity() {

    private lateinit var gameManager: GamePackageManager
    private lateinit var trace: LaunchTrace
    private var overlayManager: InbuiltOverlayManager? = null
    private var normalExitPrepared = false
    private var normalExitRestartScheduled = false
    private var gameRuntimeStarted = false
    private var preloaderTextInput: PreloaderTextInput? = null
    private var previousInputFocus: View? = null

    private class PreloaderTextInput(context: Context) : AppCompatEditText(context) {
        override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
            val target = super.onCreateInputConnection(outAttrs) ?: return null
            return object : InputConnectionWrapper(target, true) {
                override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                    if (!text.isNullOrEmpty() && PreloaderInput.onTextInput(text)) {
                        super.commitText(text, newCursorPosition)
                        return true
                    }
                    return super.commitText(text, newCursorPosition)
                }

                override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                    if (beforeLength > 0 && dispatchBackspace()) {
                        return true
                    }
                    return super.deleteSurroundingText(beforeLength, afterLength)
                }

                override fun deleteSurroundingTextInCodePoints(
                    beforeLength: Int,
                    afterLength: Int
                ): Boolean {
                    if (beforeLength > 0 && dispatchBackspace()) {
                        return true
                    }
                    return super.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
                }

                private fun dispatchBackspace(): Boolean {
                    val downConsumed = PreloaderInput.onKeyEvent(
                        KeyEvent.KEYCODE_DEL,
                        0,
                        true
                    )
                    val upConsumed = PreloaderInput.onKeyEvent(
                        KeyEvent.KEYCODE_DEL,
                        0,
                        false
                    )
                    return downConsumed || upConsumed
                }
            }
        }
    }

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
        
        initializePreloaderTextInput()
        PreloaderInput.setActivity(this)
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

        preloaderTextInput = null
        previousInputFocus = null
        PreloaderInput.clearActivity()
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

    private fun initializePreloaderTextInput() {
        val input = PreloaderTextInput(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isEmojiCompatEnabled = false
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_DONE or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                EditorInfo.IME_FLAG_NO_FULLSCREEN or
                EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.TRANSPARENT)
            isCursorVisible = false
            alpha = 0f
            visibility = View.GONE
        }
        findViewById<ViewGroup>(android.R.id.content).addView(
            input,
            ViewGroup.LayoutParams(1, 1)
        )
        preloaderTextInput = input
    }

    fun showSoftKeyboard() {
        runOnUiThread {
            val input = preloaderTextInput ?: return@runOnUiThread
            previousInputFocus = currentFocus?.takeUnless { it === input }
            input.visibility = View.VISIBLE
            input.setText("")
            input.requestFocus()
            input.setSelection(0)

            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.restartInput(input)
            if (!inputMethodManager.showSoftInput(
                    input,
                    InputMethodManager.SHOW_IMPLICIT
                )
            ) {
                inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            }
        }
    }

    fun hideSoftKeyboard() {
        runOnUiThread {
            val input = preloaderTextInput ?: return@runOnUiThread
            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(input.windowToken, 0)
            input.clearFocus()
            input.visibility = View.GONE

            previousInputFocus
                ?.takeIf { it.isAttachedToWindow && it.visibility == View.VISIBLE }
                ?.requestFocus()
            previousInputFocus = null
        }
    }
}

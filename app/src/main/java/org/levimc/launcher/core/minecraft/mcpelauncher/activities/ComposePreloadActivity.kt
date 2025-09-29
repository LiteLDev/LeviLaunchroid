package org.levimc.launcher.core.minecraft.mcpelauncher.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.Toast
import androidx.activity.ComponentActivity
import org.levimc.launcher.core.minecraft.mcpelauncher.Application
import org.levimc.launcher.core.minecraft.pesdk.PESdk
import org.levimc.launcher.core.minecraft.pesdk.PreloadException
import org.levimc.launcher.core.minecraft.pesdk.Preloader
import org.levimc.launcher.util.Logger
import java.util.concurrent.atomic.AtomicBoolean


/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

class ComposePreloadActivity : ComponentActivity() {
    private companion object {
        const val MSG_START_MINECRAFT = 1
        const val MSG_ERROR = 3
        const val MAX_RETRY_COUNT = 2
    }

    private var retryCount = 0
    private val isPreloading = AtomicBoolean(false)

    private val preloadUIHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_START_MINECRAFT -> {
                    try {
                        if (Application.context == null) {
                            Application.context = applicationContext
                        }

                        val intent = Intent(this@ComposePreloadActivity, MinecraftActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.putExtras(getIntent())

                        startActivity(intent)
                        finish()
                    } catch (e: Exception) {
                        Logger.get().error("Failed to start Minecraft activity: ${e.message}", e)
                        handleLaunchError(e)
                    }
                }
                MSG_ERROR -> {
                    val exception = msg.obj as PreloadException
                    Logger.get().error("Preload failed: ${exception.message}", exception)
                    handleLaunchError(exception)
                }
            }
        }
    }

    private fun handleLaunchError(e: Exception) {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++
            Logger.get().info("Retrying launch (attempt $retryCount of $MAX_RETRY_COUNT)")
            System.gc()

            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing) {
                    Toast.makeText(this, "Retrying launch...", Toast.LENGTH_SHORT).show()
                    startPreload()
                }
            }, 1000)
        } else {
            Toast.makeText(this, "Failed to launch Minecraft after multiple attempts", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Application.context = applicationContext.applicationContext

        startPreload()
    }

    private fun startPreload() {
        try {
            if (Application.mPESdk == null || Application.mPESdk?.getMinecraftInfo() == null) {
                Application.mPESdk = PESdk(Application.context)
            }

            if (!isPreloading.getAndSet(true)) {
                PreloadThread().start()
            }
        } catch (e: Exception) {
            Logger.get().error("Failed to initialize PESdk: ${e.message}", e)
            handleLaunchError(e)
        }
    }

    private inner class PreloadThread : Thread() {
        override fun run() {
            try {
                val preloader = Preloader(
                    Application.mPESdk,
                    null,
                    object : Preloader.PreloadListener() {
                        override fun onFinish(bundle: Bundle) {
                            val message = Message()
                            message.what = MSG_START_MINECRAFT
                            message.data = bundle
                            preloadUIHandler.sendMessage(message)
                        }
                    })
                preloader.preload(Application.context)
            } catch (e: PreloadException) {
                val message = Message()
                message.what = MSG_ERROR
                message.obj = e
                preloadUIHandler.sendMessage(message)
            } finally {
                isPreloading.set(false)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPreloading.set(false)
    }
}
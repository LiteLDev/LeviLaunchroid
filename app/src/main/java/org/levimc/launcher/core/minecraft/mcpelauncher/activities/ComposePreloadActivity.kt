package org.levimc.launcher.core.minecraft.mcpelauncher.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.activity.ComponentActivity
import org.levimc.launcher.core.minecraft.mcpelauncher.Application
import org.levimc.launcher.core.minecraft.pesdk.PESdk
import org.levimc.launcher.core.minecraft.pesdk.PreloadException
import org.levimc.launcher.core.minecraft.pesdk.Preloader
import org.levimc.launcher.util.Logger


/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

class ComposePreloadActivity : ComponentActivity() {
    private companion object {
        const val MSG_START_MINECRAFT = 1
        const val MSG_ERROR = 3
    }

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
                        finish()
                    }
                }
                MSG_ERROR -> {
                    val exception = msg.obj as PreloadException
                    Logger.get().error("Preload failed: ${exception.message}", exception)
                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Application.context = applicationContext.applicationContext

        if (Application.mPESdk == null) {
            Application.mPESdk = PESdk(Application.context)
        }

        PreloadThread().start()
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
            }
        }
    }
}
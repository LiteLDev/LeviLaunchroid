package org.levimc.launcher.core.minecraft.mcpelauncher.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.activity.ComponentActivity
import org.levimc.launcher.core.minecraft.mcpelauncher.Application
import org.levimc.launcher.core.minecraft.mcpelauncher.data.Preferences
import org.levimc.launcher.core.minecraft.pesdk.PreloadException
import org.levimc.launcher.core.minecraft.pesdk.Preloader


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
                    val intent = Intent(this@ComposePreloadActivity, MinecraftActivity::class.java)
                    intent.putExtras(msg.data)
                    startActivity(intent)
                    finish()
                }

                MSG_ERROR -> {
                    val preloadException = msg.obj as PreloadException
                    Preferences.openGameFailed = preloadException.toString()
                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                preloader.preload(this@ComposePreloadActivity)
            } catch (e: PreloadException) {
                val message = Message()
                message.what = MSG_ERROR
                message.obj = e
                preloadUIHandler.sendMessage(message)
            }
        }
    }
}
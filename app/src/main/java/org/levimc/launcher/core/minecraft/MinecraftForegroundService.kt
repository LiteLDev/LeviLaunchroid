package org.levimc.launcher.core.minecraft

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.net.wifi.WifiManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.levimc.launcher.R
import org.levimc.launcher.settings.FeatureSettings

class MinecraftForegroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!FeatureSettings.getInstance().isForegroundServiceEnabled()) {
            stopSelf()
            return START_NOT_STICKY
        }

        acquireBackgroundLocks()
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseBackgroundLocks()
        super.onDestroy()
    }

    private fun acquireBackgroundLocks() {
        if (wakeLock?.isHeld != true) {
            val powerManager = getSystemService(PowerManager::class.java)
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "$packageName:MinecraftForeground"
            )?.apply {
                setReferenceCounted(false)
                acquire()
            }
        }

        if (wifiLock?.isHeld != true) {
            val wifiManager = applicationContext.getSystemService(WifiManager::class.java)
            wifiLock = wifiManager?.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "$packageName:MinecraftForegroundWifi"
            )?.apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    private fun releaseBackgroundLocks() {
        try {
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (_: Throwable) {
        } finally {
            wifiLock = null
        }

        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Throwable) {
        } finally {
            wakeLock = null
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.foreground_service_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val gameIntent = Intent(this, MinecraftActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            gameIntent,
            pendingIntentFlags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_leaf_logo_mono)
            .setContentTitle(getString(R.string.foreground_service_notification_title))
            .setContentText(getString(R.string.foreground_service_notification_text))
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    companion object {
        private const val TAG = "MinecraftForeground"
        private const val CHANNEL_ID = "minecraft_foreground_service"
        private const val NOTIFICATION_ID = 0x4D43

        @JvmStatic
        fun startIfEnabled(context: Context) {
            if (!FeatureSettings.getInstance().isForegroundServiceEnabled()) return
            try {
                ContextCompat.startForegroundService(
                    context.applicationContext,
                    Intent(context.applicationContext, MinecraftForegroundService::class.java)
                )
            } catch (throwable: Throwable) {
                Log.w(TAG, "Unable to start Minecraft foreground service", throwable)
            }
        }

        @JvmStatic
        fun stop(context: Context) {
            try {
                context.applicationContext.stopService(
                    Intent(context.applicationContext, MinecraftForegroundService::class.java)
                )
            } catch (throwable: Throwable) {
                Log.w(TAG, "Unable to stop Minecraft foreground service", throwable)
            }
        }
    }
}

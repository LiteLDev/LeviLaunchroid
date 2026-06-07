package org.levimc.launcher.core.crash

import android.app.ActivityManager
import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import androidx.annotation.RequiresApi
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.levimc.launcher.BuildConfig
import org.levimc.launcher.settings.FeatureSettings
import org.levimc.launcher.ui.activities.CrashActivity
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

object CrashReporter {
    private const val PREFS_NAME = "crash_reporter"
    private const val KEY_PENDING_LOG_PATH = "pending_log_path"
    private const val KEY_PENDING_SUMMARY = "pending_summary"
    private const val KEY_PENDING_CRASH_TYPE = "pending_crash_type"
    private const val KEY_PENDING_EMERGENCY = "pending_emergency"
    private const val KEY_HANDLED_JAVA_CRASH_TIMESTAMP = "handled_java_crash_timestamp"
    private const val KEY_HANDLED_EXIT_TIMESTAMP = "handled_exit_timestamp"
    private const val KEY_RECOVERY_ARMED_TIMESTAMP = "recovery_armed_timestamp"
    private const val KEY_RECOVERY_ARMED_REASON = "recovery_armed_reason"
    private const val MAX_EXIT_TRACE_LENGTH = 80_000
    private const val MAX_EXIT_TRACE_BYTES = 160_000
    private const val MAX_CRASHLYTICS_VALUE_LENGTH = 1024
    private const val JAVA_CRASH_EXIT_DEDUP_WINDOW_MS = 5 * 60 * 1000L
    private const val CRASH_ACTIVITY_DELAY_MS = 1500L
    private const val RECOVERY_ALARM_DELAY_MS = 1800L
    private const val RECOVERY_ALARM_HEARTBEAT_MS = 900L
    private const val RECOVERY_REQUEST_CODE = 0x1EAF
    private const val EXTRA_LOG_PATH = "LOG_PATH"
    private const val EXTRA_SUMMARY = "SUMMARY"
    private const val EXTRA_CRASH_TYPE = "CRASH_TYPE"
    private const val EXTRA_LEGACY_EMERGENCY = "EMERGENCY"
    private const val CRASH_TYPE_JAVA_KOTLIN = "JAVA_KOTLIN"
    private const val CRASH_TYPE_NATIVE = "NATIVE_CRASH"
    private const val CRASH_TYPE_ANR = "ANR"

    @Volatile
    private var installed = false

    @Volatile
    private var handlingCrash = false

    private val recoveryHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var recoveryArmed = false

    private var foregroundActivityCount = 0

    private var recoveryHeartbeatContext: Context? = null

    private val recoveryHeartbeat = object : Runnable {
        override fun run() {
            val context = recoveryHeartbeatContext
            if (!recoveryArmed || context == null) return
            scheduleRecoveryAlarm(context, pendingRecoveryReason(context))
            recoveryHandler.postDelayed(this, RECOVERY_ALARM_HEARTBEAT_MS)
        }
    }

    @JvmStatic
    fun init(application: Application) {
        if (installed) return
        synchronized(this) {
            if (installed) return
            installed = true
            val appContext = application.applicationContext
            configureCrashlytics(appContext)
            capturePreviousProcessExit(appContext)
            if (isCrashProcess()) return
            installJavaCrashHandler(appContext)
            registerCrashActivityLauncher(application)
        }
    }

    @JvmStatic
    fun isHandlingCrash(): Boolean {
        return handlingCrash
    }

    @JvmStatic
    fun hasPendingCrash(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val logPath = prefs.getString(KEY_PENDING_LOG_PATH, null)
        val summary = pendingSummary(prefs)
        return !logPath.isNullOrBlank() || !summary.isNullOrBlank()
    }

    @JvmStatic
    fun consumePendingCrashIntent(context: Context): Intent? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val logPath = prefs.getString(KEY_PENDING_LOG_PATH, null)
        val summary = pendingSummary(prefs)
        val crashType = prefs.getString(KEY_PENDING_CRASH_TYPE, null)
        if (logPath.isNullOrBlank() && summary.isNullOrBlank()) return null

        clearPendingCrash(context)
        return buildCrashIntent(context, logPath, summary, crashType)
    }

    @JvmStatic
    fun refreshPendingCrashFromPreviousExit(context: Context) {
        capturePreviousProcessExit(context.applicationContext)
    }

    @JvmStatic
    fun pendingCrashIntent(context: Context): Intent? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val logPath = prefs.getString(KEY_PENDING_LOG_PATH, null)
        val summary = pendingSummary(prefs)
        val crashType = prefs.getString(KEY_PENDING_CRASH_TYPE, null)
        if (logPath.isNullOrBlank() && summary.isNullOrBlank()) return null
        return buildCrashIntent(context, logPath, summary, crashType)
    }

    @JvmStatic
    fun disarmRecovery(context: Context) {
        val appContext = context.applicationContext
        recoveryArmed = false
        recoveryHeartbeatContext = null
        recoveryHandler.removeCallbacks(recoveryHeartbeat)
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_RECOVERY_ARMED_TIMESTAMP)
            .remove(KEY_RECOVERY_ARMED_REASON)
            .commit()
        cancelRecoveryAlarm(appContext)
    }

    @JvmStatic
    fun sendUnsentReports() {
        if (!isCrashlyticsEnabled()) return
        try {
            FirebaseCrashlytics.getInstance().sendUnsentReports()
        } catch (_: Throwable) {
        }
    }

    @JvmStatic
    fun refreshCrashlyticsCollection(context: Context) {
        configureCrashlytics(context.applicationContext)
    }

    @JvmStatic
    fun clearPendingCrash(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PENDING_LOG_PATH)
            .remove(KEY_PENDING_SUMMARY)
            .remove(KEY_PENDING_CRASH_TYPE)
            .remove(KEY_PENDING_EMERGENCY)
            .commit()
    }

    private fun configureCrashlytics(context: Context) {
        if (!ensureFirebaseInitialized(context)) return
        try {
            FirebaseCrashlytics.getInstance().apply {
                val uploadEnabled = isCrashUploadEnabled()
                setCrashlyticsCollectionEnabled(uploadEnabled)
                if (!uploadEnabled) {
                    deleteUnsentReports()
                    return
                }
                setCustomKey("version_name", BuildConfig.VERSION_NAME)
                setCustomKey("version_code", BuildConfig.VERSION_CODE)
                setCustomKey("is_beta", BuildConfig.IS_BETA)
                setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            }
        } catch (_: Throwable) {
        }
    }

    private fun isCrashlyticsEnabled(): Boolean {
        return isCrashlyticsAvailable() && isCrashUploadEnabled()
    }

    private fun isCrashlyticsAvailable(): Boolean {
        return BuildConfig.BUILD_TYPE == "debug" ||
            BuildConfig.BUILD_TYPE == "release" ||
            BuildConfig.BUILD_TYPE == "beta"
    }

    private fun isCrashUploadEnabled(): Boolean {
        return try {
            FeatureSettings.getInstance().isCrashUploadEnabled
        } catch (_: Throwable) {
            true
        }
    }

    private fun isCrashProcess(): Boolean {
        return Application.getProcessName().endsWith(":crash")
    }

    private fun ensureFirebaseInitialized(context: Context): Boolean {
        if (!isCrashlyticsAvailable()) return false
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (_: Throwable) {
            false
        }
    }

    private fun installJavaCrashHandler(context: Context) {
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            var logPath: String? = null
            val summary = throwable.toString()

            try {
                handlingCrash = true
                val logFile = writeJavaCrashLog(appContext, thread, throwable)
                logPath = logFile.absolutePath
                savePendingCrash(appContext, logPath, summary, CRASH_TYPE_JAVA_KOTLIN)
                markJavaCrashHandled(appContext)
                logJavaCrashToCrashlytics(
                    thread = thread,
                    throwable = throwable,
                    logPath = logPath,
                    recordAsNonFatal = previousHandler == null
                )
                sendUnsentReports()
                disarmRecovery(appContext)
                showCrashActivity(appContext, logPath, summary, CRASH_TYPE_JAVA_KOTLIN)
                waitForCrashActivityLaunch()
            } catch (handlerError: Throwable) {
                recordHandlerError(handlerError)
            } finally {
                delegateOrTerminate(previousHandler, thread, throwable)
            }
        }
    }

    private fun buildCrashIntent(
        context: Context,
        logPath: String?,
        summary: String?,
        crashType: String?
    ): Intent {
        return Intent(context, CrashActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(EXTRA_LOG_PATH, logPath)
            putExtra(EXTRA_SUMMARY, summary)
            putExtra(EXTRA_CRASH_TYPE, crashType)
            putExtra(EXTRA_LEGACY_EMERGENCY, summary)
        }
    }

    private fun showCrashActivity(
        context: Context,
        logPath: String?,
        summary: String?,
        crashType: String?
    ) {
        context.startActivity(buildCrashIntent(context, logPath, summary, crashType))
    }

    private fun waitForCrashActivityLaunch() {
        try {
            Thread.sleep(CRASH_ACTIVITY_DELAY_MS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun registerCrashActivityLauncher(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                launchPendingCrashIfNeeded(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                if (activity is CrashActivity) return
                foregroundActivityCount++
                armRecovery(activity.applicationContext, "FOREGROUND")
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                if (activity is CrashActivity) return
                foregroundActivityCount = (foregroundActivityCount - 1).coerceAtLeast(0)
                if (foregroundActivityCount == 0 && !handlingCrash && !hasPendingCrash(activity)) {
                    disarmRecovery(activity.applicationContext)
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun armRecovery(context: Context, reason: String) {
        if (isCrashProcess()) return

        val appContext = context.applicationContext
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_RECOVERY_ARMED_TIMESTAMP, System.currentTimeMillis())
            .putString(KEY_RECOVERY_ARMED_REASON, reason)
            .commit()

        recoveryArmed = true
        recoveryHeartbeatContext = appContext
        scheduleRecoveryAlarm(appContext, reason)
        recoveryHandler.removeCallbacks(recoveryHeartbeat)
        recoveryHandler.postDelayed(recoveryHeartbeat, RECOVERY_ALARM_HEARTBEAT_MS)
    }

    private fun pendingRecoveryReason(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RECOVERY_ARMED_REASON, "FOREGROUND") ?: "FOREGROUND"
    }

    private fun scheduleRecoveryAlarm(context: Context, reason: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = SystemClock.elapsedRealtime() + RECOVERY_ALARM_DELAY_MS
        val intent = buildRecoveryIntent(context, reason)
        val pendingIntent = recoveryPendingIntent(context, intent)
        try {
            alarmManager.cancel(pendingIntent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (_: Throwable) {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun cancelRecoveryAlarm(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        try {
            alarmManager.cancel(recoveryPendingIntent(context, buildRecoveryIntent(context, "CANCEL")))
        } catch (_: Throwable) {
        }
    }

    private fun recoveryPendingIntent(context: Context, intent: Intent): PendingIntent {
        return PendingIntent.getActivity(
            context,
            RECOVERY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag()
        )
    }

    private fun buildRecoveryIntent(context: Context, reason: String): Intent {
        return buildCrashIntent(
            context = context,
            logPath = null,
            summary = "Recovering after process exit: $reason",
            crashType = reason
        ).apply {
            putExtra(EXTRA_LEGACY_EMERGENCY, "Recovering after process exit: $reason")
        }
    }

    private fun pendingIntentImmutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }

    private fun launchPendingCrashIfNeeded(activity: Activity) {
        if (activity is CrashActivity) return

        val crashIntent = consumePendingCrashIntent(activity)
        if (crashIntent == null) return

        sendUnsentReports()
        activity.startActivity(crashIntent)
        activity.finish()
    }

    private fun logJavaCrashToCrashlytics(
        thread: Thread,
        throwable: Throwable,
        logPath: String?,
        recordAsNonFatal: Boolean
    ) {
        if (!isCrashlyticsEnabled()) return
        try {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("last_crash_type", CRASH_TYPE_JAVA_KOTLIN)
                setCustomKey("last_crash_thread", trimCrashlyticsValue(thread.name))
                setCustomKey("last_crash_summary", trimCrashlyticsValue(throwable.toString()))
                if (!logPath.isNullOrBlank()) {
                    setCustomKey("local_crash_log", logPath)
                    log("Local crash log: $logPath")
                }
                log("Captured Java/Kotlin crash on thread ${thread.name}: $throwable")
                if (recordAsNonFatal) {
                    recordException(throwable)
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun recordHandlerError(handlerError: Throwable) {
        if (!isCrashlyticsEnabled()) return
        try {
            FirebaseCrashlytics.getInstance().recordException(handlerError)
        } catch (_: Throwable) {
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun logPreviousProcessExitToCrashlytics(
        reason: String,
        logPath: String,
        exitInfo: ApplicationExitInfo
    ) {
        if (!isCrashlyticsEnabled()) return
        try {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("last_crash_type", reason)
                setCustomKey("last_process_exit_reason", reason)
                setCustomKey("last_process_exit_log", logPath)
                setCustomKey("last_process_exit_pid", exitInfo.pid)
                setCustomKey("last_process_exit_process", trimCrashlyticsValue(exitInfo.processName))
                setCustomKey("last_process_exit_description", trimCrashlyticsValue(exitInfo.description ?: ""))
                log("Detected previous process exit: $reason")
                recordException(PreviousProcessExitException(reason, exitInfo.description))
            }
        } catch (_: Throwable) {
        }
    }

    private fun delegateOrTerminate(
        previousHandler: Thread.UncaughtExceptionHandler?,
        thread: Thread,
        throwable: Throwable
    ): Nothing {
        if (previousHandler != null) {
            try {
                previousHandler.uncaughtException(thread, throwable)
            } catch (_: Throwable) {
            }
        }
        terminateProcess()
    }

    private fun terminateProcess(): Nothing {
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }

    private fun writeJavaCrashLog(context: Context, thread: Thread, throwable: Throwable): File {
        val file = newCrashLogFile(context, "java")
        file.writeText(buildString {
            appendHeader("Java crash")
            append("Thread: ").append(thread.name).append('\n').append('\n')
            append(stackTraceToString(throwable))
        })
        return file
    }

    private fun savePendingCrash(
        context: Context,
        logPath: String,
        summary: String?,
        crashType: String?
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PENDING_LOG_PATH, logPath)
            .putString(KEY_PENDING_SUMMARY, summary)
            .putString(KEY_PENDING_CRASH_TYPE, crashType)
            .putString(KEY_PENDING_EMERGENCY, summary)
            .commit()
    }

    private fun markJavaCrashHandled(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_HANDLED_JAVA_CRASH_TIMESTAMP, System.currentTimeMillis())
            .commit()
    }

    private fun capturePreviousProcessExit(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        capturePreviousProcessExitApi30(context)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun capturePreviousProcessExitApi30(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val handledTimestamp = prefs.getLong(KEY_HANDLED_EXIT_TIMESTAMP, 0L)
        val activityManager = context.getSystemService(ActivityManager::class.java) ?: return
        val exitInfo = activityManager
            .getHistoricalProcessExitReasons(context.packageName, 0, 8)
            .firstOrNull { isReportableExitReason(it.reason) && it.timestamp > handledTimestamp }
            ?: return

        if (isAlreadyHandledJavaCrash(prefs, exitInfo)) {
            prefs.edit()
                .putLong(KEY_HANDLED_EXIT_TIMESTAMP, exitInfo.timestamp)
                .commit()
            return
        }

        prefs.edit()
            .putLong(KEY_HANDLED_EXIT_TIMESTAMP, exitInfo.timestamp)
            .commit()

        val reason = reasonName(exitInfo.reason)
        if (hasConcretePendingCrash(prefs, reason)) return

        val logFile = writeProcessExitLog(context, exitInfo)
        savePendingCrash(context, logFile.absolutePath, buildProcessExitSummary(reason, exitInfo), reason)
        logPreviousProcessExitToCrashlytics(reason, logFile.absolutePath, exitInfo)
    }

    private fun hasConcretePendingCrash(prefs: SharedPreferences, reason: String): Boolean {
        val logPath = prefs.getString(KEY_PENDING_LOG_PATH, null)
        val crashType = prefs.getString(KEY_PENDING_CRASH_TYPE, null)
        return !logPath.isNullOrBlank() && crashType == reason
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun isAlreadyHandledJavaCrash(
        prefs: android.content.SharedPreferences,
        exitInfo: ApplicationExitInfo
    ): Boolean {
        if (exitInfo.reason != ApplicationExitInfo.REASON_CRASH) return false

        val handledJavaCrashTimestamp = prefs.getLong(KEY_HANDLED_JAVA_CRASH_TIMESTAMP, 0L)
        if (handledJavaCrashTimestamp <= 0L) return false

        return kotlin.math.abs(exitInfo.timestamp - handledJavaCrashTimestamp) <=
            JAVA_CRASH_EXIT_DEDUP_WINDOW_MS
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun writeProcessExitLog(context: Context, exitInfo: ApplicationExitInfo): File {
        val file = newCrashLogFile(context, reasonName(exitInfo.reason).lowercase(Locale.US))
        file.writeText(buildString {
            appendHeader("Previous process exit")
            append("Reason: ").append(reasonName(exitInfo.reason)).append('\n')
            append("Importance: ").append(exitInfo.importance).append('\n')
            append("PID: ").append(exitInfo.pid).append('\n')
            append("Process name: ").append(exitInfo.processName).append('\n')
            append("Timestamp: ").append(formatTimestamp(exitInfo.timestamp)).append('\n')
            append("Description: ").append(exitInfo.description ?: "").append("\n\n")
            val trace = readExitTrace(exitInfo)
            if (!trace.isNullOrBlank()) {
                append(trace)
            } else {
                append("No local trace is available.\n")
            }
        })
        return file
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun isReportableExitReason(reason: Int): Boolean {
        return reason == ApplicationExitInfo.REASON_CRASH ||
            reason == ApplicationExitInfo.REASON_CRASH_NATIVE ||
            reason == ApplicationExitInfo.REASON_ANR
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun reasonName(reason: Int): String {
        return when (reason) {
            ApplicationExitInfo.REASON_CRASH -> "JAVA_CRASH"
            ApplicationExitInfo.REASON_CRASH_NATIVE -> CRASH_TYPE_NATIVE
            ApplicationExitInfo.REASON_ANR -> CRASH_TYPE_ANR
            else -> "PROCESS_EXIT_$reason"
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun buildProcessExitSummary(reason: String, exitInfo: ApplicationExitInfo): String {
        val description = exitInfo.description?.takeIf { it.isNotBlank() }
        return if (description.isNullOrBlank()) {
            reason
        } else {
            "$reason: $description"
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun readExitTrace(exitInfo: ApplicationExitInfo): String? {
        return try {
            exitInfo.traceInputStream?.use { input ->
                val bytes = readLimitedBytes(input, MAX_EXIT_TRACE_BYTES)
                sanitizeTraceBytes(bytes)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun readLimitedBytes(input: InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream(limit.coerceAtMost(8192))
        val buffer = ByteArray(4096)
        var remaining = limit
        while (remaining > 0) {
            val count = input.read(buffer, 0, buffer.size.coerceAtMost(remaining))
            if (count <= 0) break
            output.write(buffer, 0, count)
            remaining -= count
        }
        return output.toByteArray()
    }

    private fun sanitizeTraceBytes(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null

        val builder = StringBuilder(bytes.size.coerceAtMost(MAX_EXIT_TRACE_LENGTH))
        var previousWasBreak = false
        var previousWasSpace = false

        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            when {
                value == 0x0A || value == 0x0D -> {
                    if (!previousWasBreak) builder.append('\n')
                    previousWasBreak = true
                    previousWasSpace = false
                }
                value == 0x09 -> {
                    if (!previousWasSpace) builder.append(' ')
                    previousWasBreak = false
                    previousWasSpace = true
                }
                value in 0x20..0x7E -> {
                    builder.append(value.toChar())
                    previousWasBreak = false
                    previousWasSpace = value == 0x20
                }
                else -> {
                    if (!previousWasBreak) builder.append('\n')
                    previousWasBreak = true
                    previousWasSpace = false
                }
            }

            if (builder.length >= MAX_EXIT_TRACE_LENGTH) {
                builder.append("\n\n... [truncated] ...\n")
                break
            }
        }

        val text = builder.toString()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && hasUsefulTraceText(it) }
            .distinct()
            .joinToString("\n")

        return text.ifBlank { null }
    }

    private fun hasUsefulTraceText(line: String): Boolean {
        if (line.length < 3) return false
        return line.any { it.isLetterOrDigit() } &&
            !line.all { it == '?' || it == '"' || it == '\'' || it == '/' || it == '-' }
    }

    private fun StringBuilder.appendHeader(title: String) {
        append(title).append('\n')
        append("Time: ").append(formatTimestamp(System.currentTimeMillis())).append('\n')
        append("Version: ").append(BuildConfig.VERSION_NAME)
            .append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
        append("Build type: ").append(BuildConfig.BUILD_TYPE).append('\n')
        append("Is beta: ").append(BuildConfig.IS_BETA).append('\n')
        append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
        append("Android: ").append(Build.VERSION.RELEASE)
            .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n\n")
    }

    private fun stackTraceToString(throwable: Throwable): String {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

    private fun newCrashLogFile(context: Context, prefix: String): File {
        val dir = crashLogDir(context)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return File(dir, "${prefix}_crash_$timestamp.log")
    }

    private fun crashLogDir(context: Context): File {
        val primary = File(Environment.getExternalStorageDirectory(), "games/org.levimc/crash_logs")
        if (ensureDir(primary)) return primary

        val externalRoot = context.getExternalFilesDir(null)
        if (externalRoot != null) {
            val external = File(externalRoot, "crash_logs")
            if (ensureDir(external)) return external
        }

        val fallback = File(context.filesDir, "crash_logs")
        ensureDir(fallback)
        return fallback
    }

    private fun pendingSummary(prefs: SharedPreferences): String? {
        return prefs.getString(KEY_PENDING_SUMMARY, null)
            ?: prefs.getString(KEY_PENDING_EMERGENCY, null)
    }

    private fun trimCrashlyticsValue(value: String): String {
        return if (value.length > MAX_CRASHLYTICS_VALUE_LENGTH) {
            value.take(MAX_CRASHLYTICS_VALUE_LENGTH)
        } else {
            value
        }
    }

    private class PreviousProcessExitException(
        reason: String,
        description: String?
    ) : RuntimeException(
        if (description.isNullOrBlank()) {
            "Previous process exited because of $reason"
        } else {
            "Previous process exited because of $reason: $description"
        }
    )

    private fun ensureDir(dir: File): Boolean {
        return try {
            if (dir.exists()) dir.isDirectory else dir.mkdirs()
        } catch (_: Throwable) {
            false
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date(timestamp))
    }
}

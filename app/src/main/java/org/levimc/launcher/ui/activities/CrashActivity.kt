package org.levimc.launcher.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.levimc.launcher.R
import java.io.File
import androidx.core.content.FileProvider
import java.util.concurrent.Executors

class CrashActivity : AppCompatActivity() {
    
    private var executor = Executors.newSingleThreadExecutor()
    private var logPath: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_crash)

        logPath = intent.getStringExtra("LOG_PATH")
        val emergency = intent.getStringExtra("EMERGENCY")

        val tvTitle = findViewById<TextView>(R.id.crash_title)
        val tvDetails = findViewById<TextView>(R.id.crash_details)
        val btnBack = findViewById<Button>(R.id.btn_back_to_main)
        val btnShare = findViewById<Button>(R.id.btn_share_log)

        tvTitle.text = getString(R.string.crash_title)

        tvDetails.text = buildString {
            append(getString(R.string.crash_log_file_label)).append("\n")
            append(logPath ?: "").append("\n\n")
            append("Loading crash details...")
        }

        btnBack.setOnClickListener {
            navigateToMain()
        }
        
        btnShare.setOnClickListener {
            shareLogFile()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            loadCrashDetails(tvDetails, emergency)
        }, 100)
    }
    
    private fun loadCrashDetails(tvDetails: TextView, emergency: String?) {
        if (isFinishing || isDestroyed) return
        
        executor.execute {
            val detailsText = buildString {
                if (!logPath.isNullOrEmpty()) {
                    append(getString(R.string.crash_log_file_label)).append("\n")
                    append(logPath).append("\n\n")
                    try {
                        val f = File(logPath!!)
                        if (f.exists() && f.isFile) {
                            val content = f.readText()
                            val maxLength = 50000
                            if (content.length > maxLength) {
                                append(content.substring(0, maxLength))
                                append("\n\n... [truncated, full log in file] ...")
                            } else {
                                append(content)
                            }
                        } else {
                            append(getString(R.string.crash_log_file_missing)).append("\n")
                        }
                    } catch (e: Exception) {
                        append(getString(R.string.crash_read_failed, e.message)).append("\n")
                    }
                }
                if (!emergency.isNullOrEmpty()) {
                    append("\n")
                    append(getString(R.string.crash_emergency_label)).append("\n")
                    append(emergency)
                }
                if (isEmpty()) {
                    append(getString(R.string.crash_no_details))
                }
            }
            
            if (!isFinishing && !isDestroyed) {
                runOnUiThread {
                    tvDetails.text = detailsText
                }
            }
        }
    }
    
    private fun navigateToMain() {
        try {
            val intent = Intent(this, SplashActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            try {
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            } catch (ignored: Exception) {
                finish()
            }
        }
    }
    
    private fun shareLogFile() {
        val path = logPath ?: return
        try {
            val logFile = File(path)
            if (logFile.exists()) {
                val logFileUri = FileProvider.getUriForFile(
                    this, 
                    packageName + ".fileprovider", 
                    logFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, logFileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, logFile.name))
            }
        } catch (e: Exception) {
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            executor.shutdownNow()
        } catch (ignored: Exception) {}
    }
}
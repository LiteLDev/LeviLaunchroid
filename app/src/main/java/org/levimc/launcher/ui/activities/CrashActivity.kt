package org.levimc.launcher.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import org.levimc.launcher.R
import java.io.File

class CrashActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)

        val tvTitle = findViewById<TextView>(R.id.crash_title)
        val tvDetails = findViewById<TextView>(R.id.crash_details)
        val btnBack = findViewById<Button>(R.id.btn_back_to_main)

        tvTitle.text = getString(R.string.crash_title)

        val logPath = intent.getStringExtra("LOG_PATH")
        val emergency = intent.getStringExtra("EMERGENCY")
        val detailsText = buildString {
            if (!logPath.isNullOrEmpty()) {
                append(getString(R.string.crash_log_file_label)).append("\n")
                append(logPath).append("\n\n")
                try {
                    val f = File(logPath)
                    if (f.exists() && f.isFile) {
                        val content = f.readText()
                        append(content)
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
        tvDetails.text = detailsText

        btnBack.setOnClickListener {
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
                } catch (ignored: Exception) {}
            }
        }
    }
}
/*
 * Copyright (C) 2018-2022 Тимашков Иван
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.microsoft.xal.browser

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.microsoft.xal.logging.XalLogger



/**
 * 13.08.2022
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
class IntentHandler : AppCompatActivity() {
    companion object {
        private const val TAG = "IntentHandler"
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "onCreate() New intent received.")

        val callbackData = intent.data
        Log.i(TAG, "Received callback URL: $callbackData")

        if (callbackData != null) {
            val browserIntent = Intent(this, BrowserLaunchActivity::class.java)
            browserIntent.data = callbackData
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(browserIntent)
        } else {
            Log.w(TAG, "No callback data received")
            val mainIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (mainIntent != null) {
                startActivity(mainIntent)
            }
        }

        finish()
    }
}

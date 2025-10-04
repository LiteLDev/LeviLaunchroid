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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.microsoft.xal.browser.ShowUrlType.Companion.fromInt


/**
 * 13.08.2022
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
class BrowserLaunchActivity : AppCompatActivity() {
    private var mLaunchParameters: BrowserLaunchParameters? = null
    private var mOperationId: Long = 0
    private var mCustomTabsInProgress = false
    private var mSharedBrowserUsed = false
    private var mBrowserInfo: String? = null
    private var mExternalBrowserLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extras = intent.extras
        if (!checkNativeCodeLoaded()) {
            Log.e(TAG, "onCreate() Called while XAL not loaded. Dropping flow and starting app's main activity.")
            startActivity(
                applicationContext.packageManager.getLaunchIntentForPackage(
                    applicationContext.packageName
                )
            )
            finish()
        } else if (savedInstanceState != null) {
            Log.e(TAG, "onCreate() Recreating with saved state.")
            mOperationId = savedInstanceState.getLong(OPERATION_ID_STATE_KEY)
            mCustomTabsInProgress = savedInstanceState.getBoolean(CUSTOM_TABS_IN_PROGRESS_STATE_KEY)
            mSharedBrowserUsed = savedInstanceState.getBoolean(SHARED_BROWSER_USED_STATE_KEY)
            mBrowserInfo = savedInstanceState.getString(BROWSER_INFO_STATE_KEY)
            mExternalBrowserLaunched = savedInstanceState.getBoolean("EXTERNAL_BROWSER_LAUNCHED", false)
        } else if (extras != null) {
            Log.e(TAG, "onCreate() Created with intent args. Starting auth session.")
            mOperationId = extras.getLong(OPERATION_ID, 0L)
            val parameters = BrowserLaunchParameters.parameters(extras)
            mLaunchParameters = parameters
            if (parameters == null || mOperationId == 0L) {
                Log.e(TAG, "onCreate() Found invalid args, failing operation.")
                finishOperation(WebResult.FAIL, null);
            }
        } else if (intent.data != null) {
            Log.i(TAG, "onCreate() Created with callback URL from authentication: ${intent.data}")
            Log.w(TAG, "onCreate() No operation ID for callback, restarting main activity")

            getSharedPreferences("xal_prefs", MODE_PRIVATE)
                .edit()
                .putString("last_auth_result", intent.data.toString())
                .putLong("auth_timestamp", System.currentTimeMillis())
                .apply()

            val mainIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (mainIntent != null) {
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(mainIntent)
            }
            finish()
        } else {
            Log.e(TAG, "onCreate() Unexpectedly created, reason unknown. Finishing with failure.")
            setResult(RESULT_FAILED)
            finishOperation(WebResult.FAIL, null)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume()");
        val customTabsInProgress = mCustomTabsInProgress
        val browserLaunchParameters = mLaunchParameters

        if (!customTabsInProgress && browserLaunchParameters != null) {
            Log.i(TAG, "onResume() Resumed with launch parameters. Starting auth session.")
            mLaunchParameters = null
            startAuthSession(browserLaunchParameters)
            return
        }

        if (customTabsInProgress) {
            mCustomTabsInProgress = false
            val data = intent.data
            if (data != null) {
                Log.i(TAG, "onResume() Resumed with intent data. Finishing operation successfully.")
                finishOperation(WebResult.SUCCESS, data.toString())
                return
            }
            Log.w(TAG, "onResume() Resumed with no intent data. Canceling operation.")
            finishOperation(WebResult.CANCEL, null)
            return
        }

        if (mSharedBrowserUsed && mOperationId != 0L) {
            Log.i(TAG, "onResume() User returned from external browser, assuming authentication completed")
            val data = intent.data
            if (data != null) {
                Log.i(TAG, "onResume() Received callback URL: ${data}")
                finishOperation(WebResult.SUCCESS, data.toString())
            } else {
                Log.i(TAG, "onResume() No callback URL, but assuming success")
                finishOperation(WebResult.SUCCESS, "https://login.live.com/oauth20_desktop.srf?code=success")
            }
            return
        }

        Log.w(TAG, "onResume() No action to take.")
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        Log.i(TAG, "onSaveInstanceState() Preserving state.")
        bundle.putLong(OPERATION_ID_STATE_KEY, mOperationId)
        bundle.putBoolean(CUSTOM_TABS_IN_PROGRESS_STATE_KEY, mCustomTabsInProgress)
        bundle.putBoolean(SHARED_BROWSER_USED_STATE_KEY, mSharedBrowserUsed)
        bundle.putString(BROWSER_INFO_STATE_KEY, mBrowserInfo)
        bundle.putBoolean("EXTERNAL_BROWSER_LAUNCHED", mExternalBrowserLaunched)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.i(TAG, "onNewIntent() Received intent.")
        setIntent(intent)

        val callbackData = intent.data
        Log.i(TAG, "onNewIntent() Intent data: $callbackData, operationId: $mOperationId")

        if (callbackData != null) {
            Log.i(TAG, "onNewIntent() Processing callback URL: $callbackData")
            if (mOperationId != 0L) {
                finishOperation(WebResult.SUCCESS, callbackData.toString())
            } else {
                Log.w(TAG, "onNewIntent() No operation ID, cannot finish operation")
                val mainIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (mainIntent != null) {
                    startActivity(mainIntent)
                }
                finish()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Log.e(TAG, "onActivityResult() Result received.")
        if (requestCode == WEB_KIT_WEB_VIEW_REQUEST) {
            when (resultCode) {
                RESULT_OK -> {
                    val response = intent?.extras?.getString(WebKitWebViewController.RESPONSE_KEY, "")
                    if (response.isNullOrEmpty()) {
                        Log.e(TAG, "onActivityResult() Invalid final URL received from web view.")
                        finishOperation(WebResult.FAIL, null)
                    } else {
                        getSharedPreferences("xal_prefs", MODE_PRIVATE)
                            .edit()
                            .putInt("webview_crash_count", 0)
                            .apply()
                        finishOperation(WebResult.SUCCESS, response)
                        return
                    }
                }

                RESULT_CANCELED -> {
                    finishOperation(WebResult.CANCEL, null)
                    return
                }

                RESULT_UNRECOGNIZED -> {
                    Log.w(TAG, "onActivityResult() Unrecognized result code received from web view: $resultCode")
                    finishOperation(WebResult.FAIL, null)
                }

                else -> {
                    val prefs = getSharedPreferences("xal_prefs", MODE_PRIVATE)
                    val crashCount = prefs.getInt("webview_crash_count", 0)
                    prefs.edit().putInt("webview_crash_count", crashCount + 1).apply()
                    Log.e(TAG, "WebView failed with result code: $resultCode, crash count: ${crashCount + 1}")
                    finishOperation(WebResult.FAIL, null)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "onDestroy()")
        if (!isFinishing || mOperationId == 0L) {
            return
        }
        Log.w(TAG, "onDestroy() Activity is finishing with operation in progress, canceling.")
        finishOperation(WebResult.CANCEL, null)
    }

    private fun startAuthSession(browserLaunchParameters: BrowserLaunchParameters) {
        Log.i(TAG, "startAuthSession() Using external browser strategy due to WebView compatibility issues.")

        if (browserLaunchParameters.showType == ShowUrlType.CookieRemoval ||
            browserLaunchParameters.showType == ShowUrlType.CookieRemovalSkipIfSharedCredentials) {
            Log.i(TAG, "startAuthSession() Cookie removal requested, completing immediately.")
            finishOperation(WebResult.SUCCESS, browserLaunchParameters.endUrl)
            return
        }

        startExternalBrowser(browserLaunchParameters.startUrl, browserLaunchParameters.endUrl)
    }

    private fun startCustomTabsInBrowser(
        packageName: String,
        startUrl: String,
        endUrl: String,
        showUrlType: ShowUrlType,
    ) {
        if (showUrlType === ShowUrlType.CookieRemovalSkipIfSharedCredentials) {
            finishOperation(WebResult.SUCCESS, endUrl)
            return
        }
        mCustomTabsInProgress = true
        mSharedBrowserUsed = true

        val builder = CustomTabsIntent.Builder()
        builder.setShowTitle(true)

        val build = builder.build()
        build.intent.setData(Uri.parse(startUrl))
        build.intent.setPackage(packageName)

        startActivity(build.intent)
    }

    private fun startWebView(
        startUrl: String,
        endUrl: String,
        showUrlType: ShowUrlType,
        requestHeaderKeys: Array<String>,
        requestHeaderValues: Array<String>,
    ) {
        mSharedBrowserUsed = false

        val prefs = getSharedPreferences("xal_prefs", MODE_PRIVATE)
        val webViewCrashCount = prefs.getInt("webview_crash_count", 0)

        if (webViewCrashCount >= 3) {
            Log.w(TAG, "WebView has crashed $webViewCrashCount times, using external browser")
            startExternalBrowser(startUrl, endUrl)
            return
        }

        val bundle = Bundle()
        bundle.putString(START_URL, startUrl)
        bundle.putString(END_URL, endUrl)
        bundle.putSerializable(SHOW_TYPE, showUrlType)
        bundle.putStringArray(REQUEST_HEADER_KEYS, requestHeaderKeys)
        bundle.putStringArray(REQUEST_HEADER_VALUES, requestHeaderValues)

        try {
            val intent = Intent(applicationContext, WebKitWebViewController::class.java)
            intent.putExtras(bundle)
            startActivityForResult(intent, WEB_KIT_WEB_VIEW_REQUEST)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WebView, trying fallback browser", e)
            // Increment crash count
            prefs.edit().putInt("webview_crash_count", webViewCrashCount + 1).apply()
            startExternalBrowser(startUrl, endUrl)
        }
    }

    private fun startExternalBrowser(startUrl: String, endUrl: String) {
        try {
            Log.i(TAG, "Starting external browser for authentication")

            val customTabsIntent = androidx.browser.customtabs.CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(false)
                .build()

            try {
                customTabsIntent.launchUrl(this, Uri.parse(startUrl))
                mSharedBrowserUsed = true
                mExternalBrowserLaunched = true
                mCustomTabsInProgress = true
                Log.i(TAG, "Launched Chrome Custom Tabs successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Custom Tabs failed, falling back to default browser", e)
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(startUrl))
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(browserIntent)
                mSharedBrowserUsed = true
                mExternalBrowserLaunched = true
                Log.i(TAG, "Launched default browser successfully")
            }

            Log.i(TAG, "Waiting for authentication callback...")

        } catch (browserException: Exception) {
            Log.e(TAG, "Failed to start external browser", browserException)
            finishOperation(WebResult.FAIL, null)
        }
    }

    private fun finishOperation(webResult: WebResult, finalUrl: String?) {
        val operationId = mOperationId
        mOperationId = 0L
        finish()
        if (operationId == 0L) {
            Log.e(TAG, "finishOperation() No operation ID to complete.");
            return
        }
        when (XalWebResult.mWebResult[webResult.ordinal]) {
            1 -> urlOperationSucceeded(operationId, finalUrl, mSharedBrowserUsed, mBrowserInfo)
            2 -> urlOperationCanceled(operationId, mSharedBrowserUsed, mBrowserInfo)
            3 -> urlOperationFailed(operationId, mSharedBrowserUsed, mBrowserInfo)
            else -> return
        }
    }

    private fun checkNativeCodeLoaded(): Boolean {
        return try {
            checkIsLoaded()
            true
        } catch (unused: UnsatisfiedLinkError) {
            false
        }
    }

    companion object {
        const val END_URL = "END_URL"
        const val IN_PROC_BROWSER = "IN_PROC_BROWSER"
        const val OPERATION_ID = "OPERATION_ID"
        const val REQUEST_HEADER_KEYS = "REQUEST_HEADER_KEYS"
        const val REQUEST_HEADER_VALUES = "REQUEST_HEADER_VALUES"
        const val RESULT_FAILED = 8052
        const val RESULT_UNRECOGNIZED = 8054
        const val SHOW_TYPE = "SHOW_TYPE"
        const val START_URL = "START_URL"
        const val WEB_KIT_WEB_VIEW_REQUEST = 8053
        private const val BROWSER_INFO_STATE_KEY = "BROWSER_INFO_STATE"
        private const val CUSTOM_TABS_IN_PROGRESS_STATE_KEY = "CUSTOM_TABS_IN_PROGRESS_STATE"
        private const val OPERATION_ID_STATE_KEY = "OPERATION_ID_STATE"
        private const val SHARED_BROWSER_USED_STATE_KEY = "SHARED_BROWSER_USED_STATE"
        private const val TAG = "BrowserLaunchActivity"

        @JvmStatic
        private external fun checkIsLoaded()

        @JvmStatic
        private external fun urlOperationCanceled(
            operationId: Long,
            sharedBrowserUsed: Boolean,
            browserInfo: String?
        )

        @JvmStatic
        private external fun urlOperationFailed(
            operationId: Long,
            sharedBrowserUsed: Boolean,
            browserInfo: String?
        )

        @JvmStatic
        private external fun urlOperationSucceeded(
            operationId: Long,
            finalUrl: String?,
            sharedBrowserUsed: Boolean,
            browserInfo: String?
        )

        @JvmStatic
        fun showUrl(
            operationId: Long,
            context: Context,
            startUrl: String,
            endUrl: String,
            showTypeInt: Int,
            useInProcBrowser: Boolean,
            j: Long
        ) {
            showUrl(operationId, context, startUrl, endUrl, showTypeInt, emptyArray(), emptyArray(), false)
        }

        @JvmStatic
        fun showUrl(
            operationId: Long,
            context: Context,
            startUrl: String,
            endUrl: String,
            showTypeInt: Int,
            requestHeaderKeys: Array<String?>,
            requestHeaderValues: Array<String?>,
            useInProcBrowser: Boolean,
            j2: Long
        ) {
            showUrl(
                operationId,
                context,
                startUrl,
                endUrl,
                showTypeInt,
                requestHeaderKeys,
                requestHeaderValues,
                true
            )
        }

        @JvmStatic
        fun showUrl(
            operationId: Long,
            context: Context,
            startUrl: String,
            endUrl: String,
            showTypeInt: Int,
            requestHeaderKeys: Array<String?>,
            requestHeaderValues: Array<String?>,
            useInProcBrowser: Boolean
        ) {
            if (startUrl.isNotEmpty() && endUrl.isNotEmpty()) {
                Log.i(TAG, "JNI call received.");
                val fromInt = fromInt(showTypeInt)
                if (fromInt == null) {
                    Log.e(TAG, "Unrecognized show type received: $showTypeInt");
                    urlOperationFailed(operationId, false, null)
                    return
                }
                if (requestHeaderKeys.size != requestHeaderValues.size) {
                    Log.e(TAG, "requestHeaderKeys different length than requestHeaderValues.");
                    urlOperationFailed(operationId, false, null)
                    return
                }
                val bundle = Bundle()
                bundle.putLong(OPERATION_ID, operationId)
                bundle.putString(START_URL, startUrl)
                bundle.putString(END_URL, endUrl)
                bundle.putSerializable(SHOW_TYPE, fromInt)
                bundle.putStringArray(REQUEST_HEADER_KEYS, requestHeaderKeys)
                bundle.putStringArray(REQUEST_HEADER_VALUES, requestHeaderValues)
                bundle.putBoolean(IN_PROC_BROWSER, useInProcBrowser)

                val intent = Intent(context, BrowserLaunchActivity::class.java)
                intent.putExtras(bundle)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
            Log.e(TAG, "Received invalid start or end URL.");
            urlOperationFailed(operationId, false, null)
        }
    }
}

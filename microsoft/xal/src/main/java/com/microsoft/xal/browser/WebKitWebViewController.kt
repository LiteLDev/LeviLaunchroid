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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

/**
 * 13.08.2022
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
class WebKitWebViewController : AppCompatActivity() {
    private var mWebView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras
        if (extras == null) {
            Log.e(TAG, "onCreate() Called with no extras.")
            setResult(RESULT_FAILED)
            finish()
            return
        }
        val url = extras.getString(START_URL, "")
        val endUrl = extras.getString(END_URL, "")
        if (url.isEmpty() || endUrl.isEmpty()) {
            Log.e(TAG, "onCreate() Received invalid start or end URL.");
            setResult(RESULT_FAILED)
            finish()
            return
        }
        val requestHeaderKeys = extras.getStringArray(REQUEST_HEADER_KEYS) ?: emptyArray()
        val requestHeaderValues = extras.getStringArray(REQUEST_HEADER_VALUES) ?: emptyArray()
        if (requestHeaderKeys.size != requestHeaderValues.size) {
            Log.e(TAG, "onCreate() Received request header and key arrays of different lengths.");
            setResult(RESULT_FAILED)
            finish()
            return
        }

        when (extras[SHOW_TYPE] as? ShowUrlType) {
            ShowUrlType.CookieRemoval, ShowUrlType.CookieRemovalSkipIfSharedCredentials -> {
                Log.i(TAG, "onCreate() WebView invoked for cookie removal. Deleting cookies and finishing.")
                if (requestHeaderKeys.isNotEmpty()) {
                    Log.w(TAG, "onCreate() WebView invoked for cookie removal with requestHeaders.")
                }
                deleteCookies("login.live.com", true)
                deleteCookies("account.live.com", true)
                deleteCookies("live.com", true)
                deleteCookies("xboxlive.com", true)
                deleteCookies("sisu.xboxlive.com", true)

                val intent = Intent()
                intent.putExtra(RESPONSE_KEY, endUrl)
                setResult(RESULT_OK, intent)
                finish()
                return
            }

            else -> {}
        }

        val hashMap = HashMap<String, String>(requestHeaderKeys.size)
        for (i in requestHeaderKeys.indices) {
            val str2 = requestHeaderKeys[i]
            val str = requestHeaderValues[i]
            if (str2.isNullOrEmpty() || str.isNullOrEmpty()) {
                Log.e(TAG, "onCreate() Received null or empty request field.")
                setResult(RESULT_FAILED)
                finish()
                return
            }
            hashMap[requestHeaderKeys[i]] = requestHeaderValues[i]
        }

        try {
            WebView.setWebContentsDebuggingEnabled(false)

            val webView = WebView(this@WebKitWebViewController)

            setContentView(android.widget.ProgressBar(this))

            val settings = webView.settings
            settings.javaScriptEnabled = true
            settings.mixedContentMode = MIXED_CONTENT_COMPATIBILITY_MODE
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.allowFileAccess = false
            settings.allowContentAccess = false

            webView.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)

            webView.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(webView: WebView, i: Int) {
                    setProgress(i * 100)
                    if (i == 100) {
                        runOnUiThread {
                            try {
                                setContentView(webView)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to set WebView content", e)
                            }
                        }
                    }
                }
            }

            webView.webViewClient = XalWebViewClient(this@WebKitWebViewController, endUrl)

            android.os.Handler(mainLooper).postDelayed({
                try {
                    webView.loadUrl(url, hashMap)
                    mWebView = webView
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load URL in WebView", e)
                    setResult(RESULT_FAILED)
                    finish()
                }
            }, 200)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create WebView", e)
            setResult(RESULT_FAILED)
            finish()
        }
    }

    private fun deleteCookies(domain: String, useHttps: Boolean) {
        val cookieManager = CookieManager.getInstance()
        val url = (if (useHttps) {
            "https://"
        } else {
            "http://"
        }) + domain
        cookieManager.getCookie(url)?.let { cookie ->
            val isDeleted = false
            val split = cookie.split(";".toRegex()).dropLastWhile {
                it.isEmpty()
            }.toTypedArray()
            for (str2 in split) {
                val trim = str2.split("=".toRegex()).dropLastWhile {
                    it.isEmpty()
                }.toTypedArray()[0].trim()
                var str3 = "$trim=;"
                if (trim.startsWith("__Secure-")) {
                    str3 = str3 + "Secure;Domain=" + domain + ";Path=/"
                }
                cookieManager.setCookie(
                    url,
                    if (trim.startsWith("__Host-")) {
                        str3 + "Secure;Path=/"
                    } else {
                        str3 + "Domain=" + domain + ";Path=/"
                    }
                )
            }
            if (isDeleted) {
                println("deleteCookies() Deleted cookies for $domain");
            } else {
                println("deleteCookies() Found no cookies for $domain");
            }
        }
        cookieManager.flush()
    }

    override fun onDestroy() {
        try {
            mWebView?.let { webView ->
                webView.clearHistory()
                webView.clearCache(true)
                webView.loadUrl("about:blank")
                webView.onPause()
                webView.removeAllViews()
                webView.destroyDrawingCache()
                webView.destroy()
            }
            mWebView = null
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying WebView", e)
        }
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        try {
            mWebView?.onPause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing WebView", e)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            mWebView?.onResume()
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming WebView", e)
        }
    }

    companion object {
        private const val TAG = "WebKitWebViewController"
        const val END_URL = "END_URL"
        const val REQUEST_HEADER_KEYS = "REQUEST_HEADER_KEYS"
        const val REQUEST_HEADER_VALUES = "REQUEST_HEADER_VALUES"
        const val RESPONSE_KEY = "RESPONSE"
        const val RESULT_FAILED = 8052
        const val SHOW_TYPE = "SHOW_TYPE"
        const val START_URL = "START_URL"
    }
}

package com.microsoft.xal.browser

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import com.microsoft.xal.browser.WebKitWebViewController.Companion.RESPONSE_KEY
import java.io.IOException

class XalWebViewClient(
    private val mActivity: Activity,
    private val mUrl: String,
) : WebViewClient() {
    override fun onPageFinished(webView: WebView, url: String) {
        try {
            super.onPageFinished(webView, url)
            webView.requestFocus(130)
            webView.sendAccessibilityEvent(8)
            webView.evaluateJavascript(
                "if (typeof window.__xal__performAccessibilityFocus === \"function\") { window.__xal__performAccessibilityFocus(); }",
                null
            )
        } catch (e: Exception) {
            Log.e("XalWebViewClient", "Error in onPageFinished", e)
        }
    }

    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        Log.e("XalWebViewClient", "WebView error: $errorCode - $description for URL: $failingUrl")
        super.onReceivedError(view, errorCode, description, failingUrl)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
        try {
            super.onPageStarted(view, url, favicon)
            Log.d("XalWebViewClient", "Page started loading: $url")
        } catch (e: Exception) {
            Log.e("XalWebViewClient", "Error in onPageStarted", e)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(webView: WebView, url: String): Boolean {
        try {
            if (!url.startsWith(mUrl, 0)) {
                return false
            }
            val intent = Intent()
            intent.putExtra(RESPONSE_KEY, url)
            mActivity.setResult(RESULT_OK, intent)
            mActivity.finish()
            return true
        } catch (e: Exception) {
            Log.e("XalWebViewClient", "Error in shouldOverrideUrlLoading", e)
            return false
        }
    }

    override fun shouldInterceptRequest(
        webView: WebView,
        webResourceRequest: WebResourceRequest
    ): WebResourceResponse? {
        try {
            val uri = webResourceRequest.url.toString()

            if (uri.contains("favicon.ico") || uri.contains("AppLogos")) {
                Thread {
                    try {
                        webView.loadUrl(uri)
                    } catch (e: Exception) {
                        Log.e("XalWebViewClient", "Error loading favicon/AppLogos", e)
                    }
                }
            }
            if (uri.contains(".css") && uri.contains("splash")) {
                Thread {
                    try {
                        webView.loadUrl(uri)
                    } catch (e: Exception) {
                        Log.e("XalWebViewClient", "Error loading splash CSS", e)
                    }
                }
            }

            if (uri.contains("signin_options")) {
                return webResponseFromAssets("resources/ic_manage_accounts.png")
            }
            if (uri.contains("images-eds") && uri.contains("xbox")) {
                println("User Finished Login")
            }
            return if (!uri.contains("microsoft_logo") &&
                !uri.contains("AppLogos") &&
                !uri.contains("applogos") &&
                !uri.contains("xboxlivelogo") &&
                !uri.contains("logo") &&
                !uri.contains("14_298176657f8069ea5220")
            ) {
                if (!uri.contains("AppBackgrounds") && !uri.contains("appbackgrounds") && !uri.contains("73_b46031e02b69c55b4305")) {
                    if (uri.contains("minecraft") && (uri.contains(".png") || uri.contains(".jpg"))) {
                        webResponseFromAssets("resources/bg32.png")
                    } else super.shouldInterceptRequest(webView, webResourceRequest)
                } else webResponseFromAssets("resources/bg32.png")
            } else webResponseFromAssets("resources/title.png")
        } catch (e: Exception) {
            Log.e("XalWebViewClient", "Error in shouldInterceptRequest", e)
            return super.shouldInterceptRequest(webView, webResourceRequest)
        }
    }

    private fun webResponseFromAssets(resName: String): WebResourceResponse? {
        return try {
            WebResourceResponse("text/css", "UTF-8", mActivity.assets.open(resName))
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}

package com.microsoft.xal.browser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.microsoft.xal.logging.XalLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="https://github.com/dreamguxiang">dreamguxiang</a>
 */

public class WebKitWebViewController extends Activity {
    public static final String END_URL = "END_URL";
    public static final String REQUEST_HEADER_KEYS = "REQUEST_HEADER_KEYS";
    public static final String REQUEST_HEADER_VALUES = "REQUEST_HEADER_VALUES";
    public static final String RESPONSE_KEY = "RESPONSE";
    public static final int RESULT_FAILED = 8054;
    public static final String SHOW_TYPE = "SHOW_TYPE";
    public static final String START_URL = "START_URL";

    private final XalLogger mLogger = new XalLogger("WebKitWebViewController");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogger.Important("onCreate()");

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            failAndFinish("No extras provided");
            return;
        }

        String startUrl = extras.getString(START_URL, "");
        final String endUrl = extras.getString(END_URL, "");
        if (startUrl.isEmpty() || endUrl.isEmpty()) {
            failAndFinish("Invalid start or end URL");
            return;
        }

        String[] headerKeys = extras.getStringArray(REQUEST_HEADER_KEYS);
        String[] headerValues = extras.getStringArray(REQUEST_HEADER_VALUES);

        if (headerKeys == null || headerValues == null || headerKeys.length != headerValues.length) {
            failAndFinish("Request header key/value arrays have different lengths");
            return;
        }

        BrowserLaunchActivity.ShowUrlType showType =
                (BrowserLaunchActivity.ShowUrlType) extras.getSerializable(SHOW_TYPE);

        if (showType == BrowserLaunchActivity.ShowUrlType.CookieRemoval_DEPRECATED
                || showType == BrowserLaunchActivity.ShowUrlType.CookieRemovalSkipIfSharedCredentials) {
            mLogger.Important("Cookie removal requested. Deleting cookies...");
            for (String domain : new String[]{
                    "login.live.com", "account.live.com", "live.com",
                    "xboxlive.com", "sisu.xboxlive.com", "microsoft.com", "account.microsoft.com"}) {
                deleteCookies(domain, true);
            }

            Intent intent = new Intent();
            intent.putExtra(RESPONSE_KEY, endUrl);
            setResult(Activity.RESULT_OK, intent);
            finish();
            return;
        }

        Map<String, String> headers = new HashMap<>();
        for (int i = 0; i < headerKeys.length; i++) {
            String key = headerKeys[i];
            String value = headerValues[i];
            if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
                failAndFinish("Received null or empty request header");
                return;
            }
            headers.put(key, value);
        }

        WebView webView = new WebView(this);
        setContentView(webView);
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                WebKitWebViewController.this.setProgress(newProgress * 100);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.requestFocus(WebView.FOCUS_DOWN);
                view.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED);
                view.evaluateJavascript(
                        "if (typeof window.__xal__performAccessibilityFocus === 'function') { window.__xal__performAccessibilityFocus(); }",
                        null);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!url.startsWith(endUrl)) {
                    return false;
                }
                mLogger.Important("Found end URL, finishing flow.");
                Intent intent = new Intent();
                intent.putExtra(RESPONSE_KEY, url);
                setResult(Activity.RESULT_OK, intent);
                finish();
                return true;
            }
        });

        webView.loadUrl(startUrl, headers);
    }

    private void failAndFinish(String message) {
        mLogger.Error("onCreate() " + message);
        mLogger.Flush();
        setResult(RESULT_FAILED);
        finish();
    }

    private void deleteCookies(String domain, boolean httpsOnly) {
        CookieManager cookieManager = CookieManager.getInstance();
        String base = (httpsOnly ? "https://" : "http://") + domain;
        String cookie = cookieManager.getCookie(base);
        boolean hadCookies = false;

        if (cookie != null) {
            String[] pairs = cookie.split(";");
            for (String pair : pairs) {
                String name = pair.split("=")[0].trim();
                if (name.isEmpty()) continue;

                String cookieStr;
                if (name.startsWith("__Host-")) {
                    cookieStr = name + "=;Secure;Path=/";
                } else if (name.startsWith("__Secure-")) {
                    cookieStr = name + "=;Secure;Domain=" + domain + ";Path=/";
                } else {
                    cookieStr = name + "=;Domain=" + domain + ";Path=/";
                }
                cookieManager.setCookie(base, cookieStr);
            }
            hadCookies = pairs.length > 0;
        }

        if (hadCookies) {
            mLogger.Information("Deleted cookies for " + domain);
        } else {
            mLogger.Information("No cookies found for " + domain);
        }
        cookieManager.flush();
    }
}
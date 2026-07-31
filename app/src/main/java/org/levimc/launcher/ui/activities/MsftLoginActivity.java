package org.levimc.launcher.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.levimc.launcher.R;
import org.levimc.launcher.core.auth.MsftAuthManager;
import org.levimc.launcher.util.CryptoUtils;

public class MsftLoginActivity extends AppCompatActivity {

    private static final String TAG = "MsftLoginActivity";

    private WebView webView;
    private TextView statusText;

    private String state;
    private volatile boolean redirectHandled = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_msft_login);

        webView = findViewById(R.id.msft_login_webview);
        statusText = findViewById(R.id.msft_login_status);

        statusText.setText(R.string.ms_login_starting);

        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setBackgroundColor(Color.BLACK);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                String redirectUri = MsftAuthManager.getAppConfig().getRedirectUri();
                if (url != null && url.startsWith(redirectUri)) {
                    if (redirectHandled) return true;
                    redirectHandled = true;
                    handleRedirect(url);
                    return true;
                }
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request != null && request.getUrl() != null ? request.getUrl().toString() : null;
                String redirectUri = MsftAuthManager.getAppConfig().getRedirectUri();
                if (url != null && url.startsWith(redirectUri)) {
                    if (redirectHandled) return true;
                    redirectHandled = true;
                    handleRedirect(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                Toast.makeText(MsftLoginActivity.this, "SSL error: " + error.toString(), Toast.LENGTH_LONG).show();
            }
        });

        state = CryptoUtils.randomString(32);

        String url = MsftAuthManager.buildAuthorizeUrl(state);
        webView.loadUrl(url);
    }

    private void handleRedirect(String url) {
        try {
            Uri uri = Uri.parse(url);
            String code = uri.getQueryParameter("code");
            if (code == null) {
                Toast.makeText(this, R.string.ms_login_failed, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            Intent data = new Intent();
            data.putExtra("ms_auth_code", code);
            setResult(RESULT_OK, data);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, R.string.ms_login_failed, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
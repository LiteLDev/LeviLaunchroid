package org.levimc.launcher.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.levimc.launcher.R;
import org.levimc.launcher.core.auth.MsftAccountStore;
import org.levimc.launcher.core.auth.MsftAuthManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import coelho.msftauth.api.oauth20.OAuth20Authorize;
import coelho.msftauth.api.oauth20.OAuth20Desktop;
import coelho.msftauth.api.oauth20.OAuth20Token;
import coelho.msftauth.api.oauth20.OAuth20TokenRequestByCode;
import coelho.msftauth.api.oauth20.OAuth20Util;
import coelho.msftauth.api.xbox.XboxDevice;
import coelho.msftauth.api.xbox.XboxDeviceAuthRequest;
import coelho.msftauth.api.xbox.XboxDeviceKey;
import coelho.msftauth.api.xbox.XboxDeviceToken;
import coelho.msftauth.api.xbox.XboxTitleAuthRequest;
import coelho.msftauth.api.xbox.XboxTitleToken;
import coelho.msftauth.api.xbox.XboxToken;
import coelho.msftauth.api.xbox.XboxUserAuthRequest;
import coelho.msftauth.api.xbox.XboxXSTSAuthRequest;
import coelho.msftauth.api.APIEncoding;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MsftLoginActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "0000000048183522";
    private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL offline_access";
    private static final String TAG = "MsftLoginActivity";

    private WebView webView;
    private TextView statusText;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String codeVerifier;
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
                if (url != null && url.startsWith(OAuth20Util.REDIRECT_URI)) {
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
                if (url != null && url.startsWith(OAuth20Util.REDIRECT_URI)) {
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

        codeVerifier = org.levimc.launcher.util.CryptoUtils.generateCodeVerifier();
        state = org.levimc.launcher.util.CryptoUtils.randomString(32);

        String url = MsftAuthManager.buildAuthorizeUrl(CLIENT_ID, SCOPE, org.levimc.launcher.util.CryptoUtils.urlSafeBase64(org.levimc.launcher.util.CryptoUtils.sha256(codeVerifier)), state);
        webView.loadUrl(url);
    }

    private void handleRedirect(String url) {
        try {
            OAuth20Desktop desktop = new OAuth20Desktop(url);
            String code = desktop.getCode();
            if (code == null) {
                Toast.makeText(this, R.string.ms_login_failed, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            Intent data = new Intent();
            data.putExtra("ms_auth_code", code);
            data.putExtra("ms_code_verifier", codeVerifier);
            setResult(RESULT_OK, data);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, R.string.ms_login_failed, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
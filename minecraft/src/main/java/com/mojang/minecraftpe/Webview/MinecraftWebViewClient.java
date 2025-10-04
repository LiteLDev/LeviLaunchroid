package com.mojang.minecraftpe.Webview;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.RequiresApi;
import com.mojang.minecraftpe.MainActivity;
import org.jetbrains.annotations.NotNull;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
class MinecraftWebViewClient extends WebViewClient {
    private final MinecraftWebview mView;

    public MinecraftWebViewClient(MinecraftWebview view) {
        mView = view;
    }

    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        System.out.println("Started loading " + url);
        super.onPageStarted(view, url, favicon);
    }

    public void onPageFinished(WebView view, String url) {
        System.out.println("Finished loading " + url);
        super.onPageFinished(view, url);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onReceivedError(WebView view, @NotNull WebResourceRequest request, @NotNull WebResourceError error) {
        System.out.printf("Error %s loading url %s%n", error.getDescription().toString(), request.getUrl().toString());
        mView.nativeOnWebError(error.getErrorCode(), error.getDescription().toString());
        super.onReceivedError(view, request, error);
    }

    public boolean shouldOverrideUrlLoading(@NotNull WebView view, @NotNull WebResourceRequest request) {
        Uri newUrl = request.getUrl();
        Uri oldUrl = Uri.parse(view.getUrl());
        if (!request.hasGesture() || oldUrl.getHost().equals(newUrl.getHost())) {
            return super.shouldOverrideUrlLoading(view, request);
        }
        MainActivity.mInstance.launchUri(request.getUrl().toString());
        return true;
    }
}

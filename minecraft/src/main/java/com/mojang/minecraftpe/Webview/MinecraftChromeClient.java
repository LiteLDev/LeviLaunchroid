package com.mojang.minecraftpe.Webview;

import android.webkit.WebChromeClient;
import android.webkit.WebView;
import com.mojang.minecraftpe.MainActivity;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
class MinecraftChromeClient extends WebChromeClient {
    public MinecraftWebview mView;

    public MinecraftChromeClient(MinecraftWebview view) {
        mView = view;
    }

    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        MainActivity.mInstance.runOnUiThread(() -> mView._injectApi());
    }
}

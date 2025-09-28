package com.mojang.minecraftpe.Webview;

import android.webkit.JavascriptInterface;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
class WebviewHostInterface {
    private final MinecraftWebview mView;

    public WebviewHostInterface(MinecraftWebview view) {
        mView = view;
    }

    @JavascriptInterface
    public void sendToHost(String data) {
        System.out.println("SendToHost " + data);
        mView.nativeSendToHost(data);
    }

    @JavascriptInterface
    public void dismiss() {
        System.out.println("dismiss");
        mView.nativeDismiss();
    }
}

package com.microsoft.xal.browser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.microsoft.xal.logging.XalLogger;

/**
 * @author <a href="https://github.com/dreamguxiang">dreamguxiang</a>
 */

public class IntentHandler extends Activity {
    private final XalLogger m_logger = new XalLogger("IntentHandler");

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.m_logger.Important("onCreate() New intent received.");
        this.m_logger.Flush();
        Intent intent = new Intent(this, (Class<?>) BrowserLaunchActivity.class);
        intent.setData(getIntent().getData());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
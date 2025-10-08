package com.microsoft.xal.browser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.browser.customtabs.CustomTabsIntent;

import com.microsoft.xal.logging.XalLogger;

/**
 * @author <a href="https://github.com/dreamguxiang">dreamguxiang</a>
 */

public class BrowserLaunchActivity extends Activity {

    private static final String BROWSER_INFO_STATE_KEY = "BROWSER_INFO_STATE";
    private static final String CUSTOM_TABS_IN_PROGRESS_STATE_KEY = "CUSTOM_TABS_IN_PROGRESS_STATE";
    public static final String END_URL = "END_URL";
    public static final String IN_PROC_BROWSER = "IN_PROC_BROWSER";
    public static final String OPERATION_ID = "OPERATION_ID";
    private static final String OPERATION_ID_STATE_KEY = "OPERATION_ID_STATE";
    public static final String REQUEST_HEADER_KEYS = "REQUEST_HEADER_KEYS";
    public static final String REQUEST_HEADER_VALUES = "REQUEST_HEADER_VALUES";
    public static final int RESULT_FAILED = 8052;
    private static final String SHARED_BROWSER_USED_STATE_KEY = "SHARED_BROWSER_USED_STATE";
    public static final String SHOW_TYPE = "SHOW_TYPE";
    public static final String START_URL = "START_URL";
    public static final int WEB_KIT_WEB_VIEW_REQUEST = 8053;

    private final XalLogger mLogger = new XalLogger("BrowserLaunchActivity");
    private BrowserLaunchParameters mLaunchParameters = null;
    private long mOperationId = 0;
    private boolean mCustomTabsInProgress = false;
    private boolean mSharedBrowserUsed = false;
    private String mBrowserInfo = null;

    private enum WebResult {
        SUCCESS, FAIL, CANCEL
    }

    private static native void checkIsLoaded();
    private static native void urlOperationCanceled(long id, boolean shared, String info);
    private static native void urlOperationFailed(long id, boolean shared, String info);
    private static native void urlOperationSucceeded(long id, String url, boolean shared, String info);

    public enum ShowUrlType {
        Normal,
        CookieRemoval_DEPRECATED,
        CookieRemovalSkipIfSharedCredentials,
        NonAuthFlow;

        public static ShowUrlType fromInt(int i) {
            XalLogger log = new XalLogger("BrowserLaunchActivity.ShowUrlType");
            try {
                switch (i) {
                    case 0:
                        return Normal;
                    case 1:
                        log.Warning("Encountered unexpected show type, mapped to deprecated CookieRemoval_DEPRECATED");
                        return CookieRemoval_DEPRECATED;
                    case 2:
                        return CookieRemovalSkipIfSharedCredentials;
                    case 3:
                        return NonAuthFlow;
                    default:
                        log.Warning("Unexpected show type int value: " + i);
                        return null;
                }
            } finally {
                log.close();
            }
        }

        @Override
        public String toString() {
            switch (this) {
                case Normal:
                    return "Normal";
                case CookieRemoval_DEPRECATED:
                    return "CookieRemoval_DEPRECATED";
                case CookieRemovalSkipIfSharedCredentials:
                    return "CookieRemovalSkipIfSharedCredentials";
                case NonAuthFlow:
                    return "NonAuthFlow";
                default:
                    return "Unknown";
            }
        }
    }

    private static class BrowserLaunchParameters {
        public final String StartUrl;
        public final String EndUrl;
        public final String[] RequestHeaderKeys;
        public final String[] RequestHeaderValues;
        public final ShowUrlType ShowType;
        public final boolean UseInProcBrowser;

        public static BrowserLaunchParameters FromArgs(Bundle bundle) {
            String startUrl = bundle.getString(START_URL);
            String endUrl = bundle.getString(END_URL);
            String[] headerKeys = bundle.getStringArray(REQUEST_HEADER_KEYS);
            String[] headerValues = bundle.getStringArray(REQUEST_HEADER_VALUES);
            ShowUrlType showType = (ShowUrlType) bundle.getSerializable(SHOW_TYPE);
            boolean inProc = bundle.getBoolean(IN_PROC_BROWSER);

            if (startUrl == null || endUrl == null || headerKeys == null ||
                    headerValues == null || headerKeys.length != headerValues.length) {
                return null;
            }

            return new BrowserLaunchParameters(startUrl, endUrl, headerKeys, headerValues, showType, inProc);
        }

        private BrowserLaunchParameters(
                String start, String end, String[] keys,
                String[] values, ShowUrlType type, boolean useInProc) {

            XalLogger log = new XalLogger("BrowserLaunchParameters");
            try {
                this.StartUrl = start;
                this.EndUrl = end;
                this.RequestHeaderKeys = keys;
                this.RequestHeaderValues = values;
                this.ShowType = type;

                boolean forceInProc = useInProc;

                if (type == ShowUrlType.NonAuthFlow) {
                    log.Important("Forcing inProc browser for NonAuthFlow");
                    forceInProc = true;
                } else if (keys.length > 0) {
                    log.Important("Forcing inProc browser due to request headers");
                    forceInProc = true;
                }

                this.UseInProcBrowser = forceInProc;
            } finally {
                log.close();
            }
        }
    }

    public static void showUrl(long opId, Context context, String start, String end,
                               int showTypeInt, String[] headerKeys, String[] headerValues, boolean inProc) {
        XalLogger log = new XalLogger("BrowserLaunchActivity.showUrl()");
        try {
            log.Important("JNI call received.");

            if (start == null || end == null || start.isEmpty() || end.isEmpty()) {
                log.Error("Invalid start or end URL.");
                urlOperationFailed(opId, false, null);
                return;
            }

            ShowUrlType type = ShowUrlType.fromInt(showTypeInt);
            if (type == null) {
                log.Error("Unrecognized show type: " + showTypeInt);
                urlOperationFailed(opId, false, null);
                return;
            }

            if (headerKeys.length != headerValues.length) {
                log.Error("Header key/value length mismatch.");
                urlOperationFailed(opId, false, null);
                return;
            }

            Intent i = new Intent(context, BrowserLaunchActivity.class);
            Bundle b = new Bundle();
            b.putLong(OPERATION_ID, opId);
            b.putString(START_URL, start);
            b.putString(END_URL, end);
            b.putSerializable(SHOW_TYPE, type);
            b.putStringArray(REQUEST_HEADER_KEYS, headerKeys);
            b.putStringArray(REQUEST_HEADER_VALUES, headerValues);
            b.putBoolean(IN_PROC_BROWSER, inProc);

            i.putExtras(b);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        } finally {
            log.close();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogger.Important("onCreate()");

        if (!checkNativeCodeLoaded()) {
            mLogger.Warning("Native code not loaded, restarting main app...");
            startActivity(getApplicationContext()
                    .getPackageManager()
                    .getLaunchIntentForPackage(getPackageName()));
            finish();
            return;
        }

        if (savedInstanceState != null) {
            mLogger.Important("Restoring saved state");
            mOperationId = savedInstanceState.getLong(OPERATION_ID_STATE_KEY);
            mCustomTabsInProgress = savedInstanceState.getBoolean(CUSTOM_TABS_IN_PROGRESS_STATE_KEY);
            mSharedBrowserUsed = savedInstanceState.getBoolean(SHARED_BROWSER_USED_STATE_KEY);
            mBrowserInfo = savedInstanceState.getString(BROWSER_INFO_STATE_KEY);
            return;
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mOperationId = extras.getLong(OPERATION_ID, 0L);
            mLaunchParameters = BrowserLaunchParameters.FromArgs(extras);
            if (mLaunchParameters == null || mOperationId == 0) {
                mLogger.Error("Invalid args, failing operation");
                finishOperation(WebResult.FAIL, null);
                return;
            }
        } else if (getIntent().getData() != null) {
            mLogger.Error("Unexpected intent data, failing.");
            finishOperation(WebResult.FAIL, null);
        } else {
            mLogger.Error("Unexpected intent, failing.");
            finishOperation(WebResult.FAIL, null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLogger.Important("onResume()");

        if (!mCustomTabsInProgress && mLaunchParameters != null) {
            BrowserLaunchParameters params = mLaunchParameters;
            mLaunchParameters = null;
            startAuthSession(params);
        } else if (mCustomTabsInProgress) {
            mCustomTabsInProgress = false;
            Uri data = getIntent().getData();
            if (data != null) {
                finishOperation(WebResult.SUCCESS, data.toString());
            } else {
                finishOperation(WebResult.CANCEL, null);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        mLogger.Important("Saving instance state");
        out.putLong(OPERATION_ID_STATE_KEY, mOperationId);
        out.putBoolean(CUSTOM_TABS_IN_PROGRESS_STATE_KEY, mCustomTabsInProgress);
        out.putBoolean(SHARED_BROWSER_USED_STATE_KEY, mSharedBrowserUsed);
        out.putString(BROWSER_INFO_STATE_KEY, mBrowserInfo);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mLogger.Important("onNewIntent()");
        setIntent(intent);
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        mLogger.Important("onActivityResult()");
        if (reqCode == WEB_KIT_WEB_VIEW_REQUEST) {
            if (resCode == RESULT_OK && data != null) {
                String resultUrl = data.getStringExtra(WebKitWebViewController.RESPONSE_KEY);
                if (resultUrl != null && !resultUrl.isEmpty()) {
                    finishOperation(WebResult.SUCCESS, resultUrl);
                    return;
                }
            } else if (resCode == RESULT_CANCELED) {
                finishOperation(WebResult.CANCEL, null);
                return;
            }
            finishOperation(WebResult.FAIL, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing() && mOperationId != 0) {
            finishOperation(WebResult.CANCEL, null);
        }
    }

    private void startAuthSession(BrowserLaunchParameters params) {
        BrowserSelectionResult selection = BrowserSelector.selectBrowser(getApplicationContext(), params.UseInProcBrowser);
        mBrowserInfo = selection.toString();

        if (selection.packageName() == null) {
            startWebView(params.StartUrl, params.EndUrl, params.ShowType, params.RequestHeaderKeys, params.RequestHeaderValues);
        } else {
            startCustomTabsInBrowser(selection.packageName(), params.StartUrl, params.EndUrl, params.ShowType);
        }
    }

    private void startCustomTabsInBrowser(String pkg, String startUrl, String endUrl, ShowUrlType showType) {
        if (showType == ShowUrlType.CookieRemovalSkipIfSharedCredentials) {
            finishOperation(WebResult.SUCCESS, endUrl);
            return;
        }

        mCustomTabsInProgress = true;
        mSharedBrowserUsed = true;

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setShowTitle(true);
        CustomTabsIntent tabs = builder.build();
        tabs.intent.setPackage(pkg);
        tabs.intent.setData(Uri.parse(startUrl));
        startActivity(tabs.intent);
    }

    private void startWebView(String start, String end, ShowUrlType type, String[] keys, String[] values) {
        mSharedBrowserUsed = false;
        Intent intent = new Intent(getApplicationContext(), WebKitWebViewController.class);
        Bundle b = new Bundle();
        b.putString(START_URL, start);
        b.putString(END_URL, end);
        b.putSerializable(SHOW_TYPE, type);
        b.putStringArray(REQUEST_HEADER_KEYS, keys);
        b.putStringArray(REQUEST_HEADER_VALUES, values);
        intent.putExtras(b);
        startActivityForResult(intent, WEB_KIT_WEB_VIEW_REQUEST);
    }

    private void finishOperation(WebResult result, String finalUrl) {
        long opId = mOperationId;
        mOperationId = 0;
        finish();

        if (opId == 0) {
            mLogger.Error("No operation ID to complete.");
            return;
        }

        switch (result) {
            case SUCCESS:
                urlOperationSucceeded(opId, finalUrl, mSharedBrowserUsed, mBrowserInfo);
                break;
            case CANCEL:
                urlOperationCanceled(opId, mSharedBrowserUsed, mBrowserInfo);
                break;
            case FAIL:
                urlOperationFailed(opId, mSharedBrowserUsed, mBrowserInfo);
                break;
        }
    }

    private boolean checkNativeCodeLoaded() {
        try {
            checkIsLoaded();
            return true;
        } catch (UnsatisfiedLinkError e) {
            mLogger.Error("Native code not loaded");
            return false;
        }
    }
}
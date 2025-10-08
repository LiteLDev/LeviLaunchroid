package com.microsoft.xal.browser;

import java.util.Locale;

/**
 * @author <a href="https://github.com/dreamguxiang">dreamguxiang</a>
 */

public class BrowserSelectionResult {
    private final BrowserInfo m_defaultBrowserInfo;
    private final String m_notes;
    private final boolean m_useCustomTabs;

    static class BrowserInfo {
        public final String packageName;
        public final int versionCode;
        public final String versionName;

        BrowserInfo(String str, int i, String str2) {
            this.packageName = str;
            this.versionCode = i;
            this.versionName = str2;
        }
    }

    BrowserSelectionResult(BrowserInfo browserInfo, String str, boolean z) {
        this.m_defaultBrowserInfo = browserInfo;
        this.m_notes = str;
        this.m_useCustomTabs = z;
    }

    public String toString() {
        Locale locale = Locale.US;
        Object[] objArr = new Object[4];
        objArr[0] = this.m_useCustomTabs ? "CT" : "WK";
        objArr[1] = this.m_defaultBrowserInfo.packageName;
        objArr[2] = this.m_notes;
        objArr[3] = this.m_defaultBrowserInfo.versionName;
        return String.format(locale, "%s-%s-%s::%s", objArr);
    }

    public String packageName() {
        if (this.m_useCustomTabs) {
            return this.m_defaultBrowserInfo.packageName;
        }
        return null;
    }
}

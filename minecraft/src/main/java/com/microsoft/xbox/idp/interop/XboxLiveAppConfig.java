package com.microsoft.xbox.idp.interop;


public class XboxLiveAppConfig {
    private final long id = create();

    private static native long create();

    private static native void delete(long j);

    private static native String getEnvironment(long j);

    private static native int getOverrideTitleId(long j);

    private static native String getSandbox(long j);

    private static native String getScid(long j);

    private static native int getTitleId(long j);

    public int getTitleId() {
        return getTitleId(id);
    }

    public int getOverrideTitleId() {
        return getOverrideTitleId(id);
    }

    public String getScid() {
        return getScid(id);
    }

    public String getEnvironment() {
        return getEnvironment(id);
    }

    public String getSandbox() {
        return getSandbox(id);
    }

    public void finalize() throws Throwable {
        super.finalize();
        delete(id);
    }
}

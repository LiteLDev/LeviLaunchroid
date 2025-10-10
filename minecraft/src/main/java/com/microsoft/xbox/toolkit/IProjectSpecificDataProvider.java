package com.microsoft.xbox.toolkit;


public interface IProjectSpecificDataProvider {
    boolean getAllowExplicitContent();

    String getAutoSuggestdDataSource();

    String getCombinedContentRating();

    String getConnectedLocale();

    String getConnectedLocale(boolean z);

    String getContentRestrictions();

    String getCurrentSandboxID();

    boolean getInitializeComplete();

    boolean getIsForXboxOne();

    boolean getIsFreeAccount();

    boolean getIsXboxMusicSupported();

    String getLegalLocale();

    String getMembershipLevel();

    String getPrivileges();

    void setPrivileges(String str);

    String getRegion();

    String getSCDRpsTicket();

    void setSCDRpsTicket(String str);

    String getVersionCheckUrl();

    int getVersionCode();

    String getWindowsLiveClientId();

    String getXuidString();

    void setXuidString(String str);

    boolean isDeviceLocaleKnown();

    void resetModels(boolean z);
}

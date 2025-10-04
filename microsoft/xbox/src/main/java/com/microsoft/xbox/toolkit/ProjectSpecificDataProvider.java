package com.microsoft.xbox.toolkit;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class ProjectSpecificDataProvider implements IProjectSpecificDataProvider {
    private static final ProjectSpecificDataProvider instance = new ProjectSpecificDataProvider();
    private IProjectSpecificDataProvider provider;

    public static ProjectSpecificDataProvider getInstance() {
        return instance;
    }

    private void checkProvider() {
    }

    public void setProvider(IProjectSpecificDataProvider iProjectSpecificDataProvider) {
        this.provider = iProjectSpecificDataProvider;
    }

    public String getLegalLocale() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getLegalLocale();
        }
        return null;
    }

    public String getCombinedContentRating() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getCombinedContentRating();
        }
        return null;
    }

    public String getMembershipLevel() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getMembershipLevel();
        }
        return null;
    }

    public String getXuidString() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getXuidString();
        }
        return null;
    }

    public void setXuidString(String str) {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            iProjectSpecificDataProvider.setXuidString(str);
        }
    }

    public String getSCDRpsTicket() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getSCDRpsTicket();
        }
        return null;
    }

    public void setSCDRpsTicket(String str) {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            iProjectSpecificDataProvider.setSCDRpsTicket(str);
        }
    }

    public String getPrivileges() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        return iProjectSpecificDataProvider != null ? iProjectSpecificDataProvider.getPrivileges() : "";
    }

    public void setPrivileges(String str) {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            iProjectSpecificDataProvider.setPrivileges(str);
        }
    }

    public boolean getAllowExplicitContent() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getAllowExplicitContent();
        }
        return false;
    }

    public String getAutoSuggestdDataSource() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getAutoSuggestdDataSource();
        }
        return null;
    }

    public boolean getInitializeComplete() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getInitializeComplete();
        }
        return false;
    }

    public boolean getIsFreeAccount() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getIsFreeAccount();
        }
        return true;
    }

    public boolean getIsXboxMusicSupported() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getIsXboxMusicSupported();
        }
        return false;
    }

    public String getWindowsLiveClientId() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getWindowsLiveClientId();
        }
        return null;
    }

    public String getVersionCheckUrl() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getVersionCheckUrl();
        }
        return null;
    }

    public void resetModels(boolean z) {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            iProjectSpecificDataProvider.resetModels(z);
        }
    }

    public boolean getIsForXboxOne() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getIsForXboxOne();
        }
        return false;
    }

    public String getCurrentSandboxID() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getCurrentSandboxID();
        }
        return null;
    }

    public boolean isDeviceLocaleKnown() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.isDeviceLocaleKnown();
        }
        return true;
    }

    public String getConnectedLocale() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getConnectedLocale();
        }
        return null;
    }

    public String getConnectedLocale(boolean z) {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getConnectedLocale(z);
        }
        return null;
    }

    public int getVersionCode() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getVersionCode();
        }
        return 0;
    }

    public String getContentRestrictions() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getContentRestrictions();
        }
        return null;
    }

    public String getRegion() {
        checkProvider();
        IProjectSpecificDataProvider iProjectSpecificDataProvider = this.provider;
        if (iProjectSpecificDataProvider != null) {
            return iProjectSpecificDataProvider.getRegion();
        }
        return null;
    }
}

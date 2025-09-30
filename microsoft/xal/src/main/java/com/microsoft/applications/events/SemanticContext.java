package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

class SemanticContext implements ISemanticContext {
    private final long m_nativeISemanticContextPtr;

    private native void nativeClearExperimentIds(long j);

    private native void nativeSetAppExperimentETag(long j, String str);

    private native void nativeSetCommonField(long j, String str, EventProperty eventProperty);

    private native void nativeSetCommonFieldString(long j, String str, String str2);

    private native void nativeSetCustomField(long j, String str, EventProperty eventProperty);

    private native void nativeSetCustomFieldString(long j, String str, String str2);

    private native void nativeSetEventExperimentIds(long j, String str, String str2);

    private native void nativeSetNetworkCost(long j, int i);

    private native void nativeSetNetworkType(long j, int i);

    private native void nativeSetTicket(long j, int i, String str);

    private native void nativeSetUserId(long j, String str, int i);

    long getNativeSemanticContextPtr() {
        return this.m_nativeISemanticContextPtr;
    }

    SemanticContext(long j) {
        this.m_nativeISemanticContextPtr = j;
    }

    @Override
    public void setAppEnv(String str) {
        setCommonField(Constants.COMMONFIELDS_APP_ENV, str);
    }

    @Override
    public void setAppId(String str) {
        setCommonField(Constants.COMMONFIELDS_APP_ID, str);
    }

    @Override
    public void setAppName(String str) {
        setCommonField(Constants.COMMONFIELDS_APP_NAME, str);
    }

    @Override
    public void setAppVersion(String str) {
        setCommonField(Constants.COMMONFIELDS_APP_VERSION, str);
    }

    @Override
    public void setAppLanguage(String str) {
        setCommonField(Constants.COMMONFIELDS_APP_LANGUAGE, str);
    }

    @Override
    public void setAppExperimentIds(String str) {
        setCommonField(Constants.COMMONFIELDS_APP_EXPERIMENTIDS, str);
    }

    @Override
    public void setAppExperimentETag(String str) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("appExperimentETag is null or empty");
        }
        nativeSetAppExperimentETag(this.m_nativeISemanticContextPtr, str);
    }

    @Override
    public void setAppExperimentImpressionId(String str) {
        setCommonField(Constants.SESSION_IMPRESSION_ID, str);
    }

    @Override
    public void setEventExperimentIds(String str, String str2) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("eventName is null or empty");
        }
        if (str2 == null || str2.trim().isEmpty()) {
            throw new IllegalArgumentException("experimentIds is null or empty");
        }
        nativeSetEventExperimentIds(this.m_nativeISemanticContextPtr, str, str2);
    }

    @Override
    public void clearExperimentIds() {
        nativeClearExperimentIds(this.m_nativeISemanticContextPtr);
    }

    @Override
    public void setDeviceId(String str) {
        setCommonField(Constants.COMMONFIELDS_DEVICE_ID, str);
    }

    @Override
    public void setDeviceMake(String str) {
        setCommonField(Constants.COMMONFIELDS_DEVICE_MAKE, str);
    }

    @Override
    public void setDeviceModel(String str) {
        setCommonField(Constants.COMMONFIELDS_DEVICE_MODEL, str);
    }

    @Override
    public void setDeviceClass(String str) {
        setCommonField(Constants.COMMONFIELDS_DEVICE_CLASS, str);
    }

    @Override
    public void setNetworkCost(NetworkCost networkCost) {
        if (networkCost == null) {
            throw new IllegalArgumentException("networkCost is null");
        }
        nativeSetNetworkCost(this.m_nativeISemanticContextPtr, networkCost.getValue());
    }

    @Override
    public void setNetworkProvider(String str) {
        setCommonField(Constants.COMMONFIELDS_NETWORK_PROVIDER, str);
    }

    @Override
    public void SetNetworkType(NetworkType networkType) {
        if (networkType == null) {
            throw new IllegalArgumentException("networkType is null");
        }
        nativeSetNetworkType(this.m_nativeISemanticContextPtr, networkType.getValue());
    }

    @Override
    public void setOsName(String str) {
        setCommonField("e", str);
    }

    @Override
    public void setOsVersion(String str) {
        setCommonField(Constants.COMMONFIELDS_OS_VERSION, str);
    }

    @Override
    public void setOsBuild(String str) {
        setCommonField(Constants.COMMONFIELDS_OS_BUILD, str);
    }

    @Override
    public void setUserId(String str) {
        setUserId(str, PiiKind.Identity);
    }

    @Override
    public void setUserId(String str, PiiKind piiKind) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("userId is null or empty");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind_Identity is null");
        }
        nativeSetUserId(this.m_nativeISemanticContextPtr, str, piiKind.getValue());
    }

    @Override
    public void setUserMsaId(String str) {
        setCommonField(Constants.COMMONFIELDS_USER_MSAID, str);
    }

    @Override
    public void setUserANID(String str) {
        setCommonField(Constants.COMMONFIELDS_USER_ANID, str);
    }

    @Override
    public void setUserAdvertisingId(String str) {
        setCommonField("e", str);
    }

    @Override
    public void setUserLanguage(String str) {
        setCommonField("e", str);
    }

    @Override
    public void setUserTimeZone(String str) {
        setCommonField(Constants.COMMONFIELDS_USER_TIMEZONE, str);
    }

    @Override
    public void setCommercialId(String str) {
        setCommonField(Constants.COMMONFIELDS_COMMERCIAL_ID, str);
    }

    @Override
    public void setCommonField(String str, String str2) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        if (str2 == null) {
            throw new IllegalArgumentException("value is null");
        }
        nativeSetCommonFieldString(this.m_nativeISemanticContextPtr, str, str2);
    }

    @Override
    public void setCommonField(String str, EventProperty eventProperty) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        if (eventProperty == null) {
            throw new IllegalArgumentException("value is null");
        }
        nativeSetCommonField(this.m_nativeISemanticContextPtr, str, eventProperty);
    }

    @Override
    public void setCustomField(String str, String str2) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        if (str2 == null) {
            throw new IllegalArgumentException("value is null");
        }
        nativeSetCustomFieldString(this.m_nativeISemanticContextPtr, str, str2);
    }

    @Override
    public void setCustomField(String str, EventProperty eventProperty) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        if (eventProperty == null) {
            throw new IllegalArgumentException("value is null");
        }
        nativeSetCustomField(this.m_nativeISemanticContextPtr, str, eventProperty);
    }

    @Override
    public void setTicket(TicketType ticketType, String str) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("ticketValue is null or empty");
        }
        if (ticketType == null) {
            throw new IllegalArgumentException("ticketType is null");
        }
        nativeSetTicket(this.m_nativeISemanticContextPtr, ticketType.getValue(), str);
    }
}

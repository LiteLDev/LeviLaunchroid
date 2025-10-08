package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public interface ISemanticContext {
    void SetNetworkType(NetworkType networkType);

    void clearExperimentIds();

    void setAppEnv(String str);

    void setAppExperimentETag(String str);

    void setAppExperimentIds(String str);

    void setAppExperimentImpressionId(String str);

    void setAppId(String str);

    void setAppLanguage(String str);

    void setAppName(String str);

    void setAppVersion(String str);

    void setCommercialId(String str);

    void setCommonField(String str, EventProperty eventProperty);

    void setCommonField(String str, String str2);

    void setCustomField(String str, EventProperty eventProperty);

    void setCustomField(String str, String str2);

    void setDeviceClass(String str);

    void setDeviceId(String str);

    void setDeviceMake(String str);

    void setDeviceModel(String str);

    void setEventExperimentIds(String str, String str2);

    void setNetworkCost(NetworkCost networkCost);

    void setNetworkProvider(String str);

    void setOsBuild(String str);

    void setOsName(String str);

    void setOsVersion(String str);

    void setTicket(TicketType ticketType, String str);

    void setUserANID(String str);

    void setUserAdvertisingId(String str);

    void setUserId(String str);

    void setUserId(String str, PiiKind piiKind);

    void setUserLanguage(String str);

    void setUserMsaId(String str);

    void setUserTimeZone(String str);
}

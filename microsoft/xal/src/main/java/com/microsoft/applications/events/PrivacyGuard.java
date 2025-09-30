package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class PrivacyGuard {
    public static native boolean isEnabled();

    public static native boolean isInitialized();

    private static native void nativeAddIgnoredConcern(String str, String str2, int i);

    private static native boolean nativeAppendCommonDataContext(String str, String str2, Object[] objArr, Object[] objArr2, Object[] objArr3, Object[] objArr4, Object[] objArr5, Object[] objArr6);

    private static native boolean nativeInitializePrivacyGuard(long j, String str, String str2, String str3, boolean z, boolean z2, String str4, String str5, Object[] objArr, Object[] objArr2, Object[] objArr3, Object[] objArr4, Object[] objArr5, Object[] objArr6);

    private static native boolean nativeInitializePrivacyGuardWithoutCommonDataContext(long j, String str, String str2, String str3, boolean z, boolean z2);

    public static native boolean setEnabled(boolean z);

    public static native boolean uninitialize();

    public static boolean initialize(PrivacyGuardInitConfig privacyGuardInitConfig) {
        if (privacyGuardInitConfig == null) {
            throw new IllegalArgumentException("initConfig cannot be null");
        }
        if (privacyGuardInitConfig.LoggerInstance == null) {
            throw new IllegalArgumentException("loggerInstance cannot be null in initConfig.");
        }
        if (privacyGuardInitConfig.DataContext != null) {
            return nativeInitializePrivacyGuard(privacyGuardInitConfig.LoggerInstance.getNativeILoggerPtr(), privacyGuardInitConfig.NotificationEventName, privacyGuardInitConfig.SemanticContextNotificationEventName, privacyGuardInitConfig.SummaryEventName, privacyGuardInitConfig.UseEventFieldPrefix, privacyGuardInitConfig.ScanForUrls, privacyGuardInitConfig.DataContext.domainName, privacyGuardInitConfig.DataContext.machineName, privacyGuardInitConfig.DataContext.userNames.toArray(), privacyGuardInitConfig.DataContext.userAliases.toArray(), privacyGuardInitConfig.DataContext.ipAddresses.toArray(), privacyGuardInitConfig.DataContext.languageIdentifiers.toArray(), privacyGuardInitConfig.DataContext.machineIds.toArray(), privacyGuardInitConfig.DataContext.outOfScopeIdentifiers.toArray());
        }
        return nativeInitializePrivacyGuardWithoutCommonDataContext(privacyGuardInitConfig.LoggerInstance.getNativeILoggerPtr(), privacyGuardInitConfig.NotificationEventName, privacyGuardInitConfig.SemanticContextNotificationEventName, privacyGuardInitConfig.SummaryEventName, privacyGuardInitConfig.UseEventFieldPrefix, privacyGuardInitConfig.ScanForUrls);
    }

    public static boolean appendCommonDataContext(CommonDataContext commonDataContext) {
        if (commonDataContext == null) {
            throw new IllegalArgumentException("Passed Common Data Context is null");
        }
        return nativeAppendCommonDataContext(commonDataContext.domainName, commonDataContext.machineName, commonDataContext.userNames.toArray(), commonDataContext.userAliases.toArray(), commonDataContext.ipAddresses.toArray(), commonDataContext.languageIdentifiers.toArray(), commonDataContext.machineIds.toArray(), commonDataContext.outOfScopeIdentifiers.toArray());
    }

    public static void addIgnoredConcern(String str, String str2, DataConcernType dataConcernType) {
        nativeAddIgnoredConcern(str, str2, dataConcernType.getValue());
    }
}
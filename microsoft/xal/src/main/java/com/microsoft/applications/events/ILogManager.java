package com.microsoft.applications.events;

import java.util.Date;
import java.util.UUID;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public interface ILogManager extends AutoCloseable {
    void addEventListener(DebugEventType debugEventType, DebugEventListener debugEventListener);

    void disableViewer();

    Status flush();

    void flushAndTeardown();

    String getCurrentEndpoint();

    ILogConfiguration getLogConfigurationCopy();

    LogSessionData getLogSessionData();

    ILogger getLogger(String str, String str2, String str3);

    ISemanticContext getSemanticContext();

    String getTransmitProfileName();

    boolean initializeDiagnosticDataViewer(String str, String str2);

    boolean isViewerEnabled();

    Status loadTransmitProfiles(String str);

    Status pauseTransmission();

    boolean registerPrivacyGuard();

    void removeEventListener(DebugEventType debugEventType, DebugEventListener debugEventListener);

    Status resetTransmitProfiles();

    Status resumeTransmission();

    Status setContext(String str, double d, PiiKind piiKind);

    Status setContext(String str, int i, PiiKind piiKind);

    Status setContext(String str, long j, PiiKind piiKind);

    Status setContext(String str, String str2, PiiKind piiKind);

    Status setContext(String str, Date date, PiiKind piiKind);

    Status setContext(String str, UUID uuid, PiiKind piiKind);

    Status setContext(String str, boolean z, PiiKind piiKind);

    void setLevelFilter(int i, int[] iArr);

    Status setTransmitProfile(TransmitProfile transmitProfile);

    Status setTransmitProfile(String str);

    boolean unregisterPrivacyGuard();

    Status uploadNow();
}

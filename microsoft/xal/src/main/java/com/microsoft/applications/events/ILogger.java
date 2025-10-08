package com.microsoft.applications.events;

import java.util.Date;
import java.util.UUID;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public interface ILogger extends AutoCloseable {
    void SetContext(String str, EventProperty eventProperty);

    long getNativeILoggerPtr();

    ISemanticContext getSemanticContext();

    void logAggregatedMetric(AggregatedMetricData aggregatedMetricData, EventProperties eventProperties);

    void logAggregatedMetric(String str, long j, long j2, EventProperties eventProperties);

    void logAppLifecycle(AppLifecycleState appLifecycleState, EventProperties eventProperties);

    void logEvent(EventProperties eventProperties);

    void logEvent(String str);

    void logFailure(String str, String str2, EventProperties eventProperties);

    void logFailure(String str, String str2, String str3, String str4, EventProperties eventProperties);

    void logPageAction(PageActionData pageActionData, EventProperties eventProperties);

    void logPageAction(String str, ActionType actionType, EventProperties eventProperties);

    void logPageView(String str, String str2, EventProperties eventProperties);

    void logPageView(String str, String str2, String str3, String str4, String str5, EventProperties eventProperties);

    void logSampledMetric(String str, double d, String str2, EventProperties eventProperties);

    void logSampledMetric(String str, double d, String str2, String str3, String str4, String str5, EventProperties eventProperties);

    void logSession(SessionState sessionState, EventProperties eventProperties);

    void logTrace(TraceLevel traceLevel, String str, EventProperties eventProperties);

    void logUserState(UserState userState, long j, EventProperties eventProperties);

    void setContext(String str, double d);

    void setContext(String str, double d, PiiKind piiKind);

    void setContext(String str, int i);

    void setContext(String str, int i, PiiKind piiKind);

    void setContext(String str, long j);

    void setContext(String str, long j, PiiKind piiKind);

    void setContext(String str, String str2);

    void setContext(String str, String str2, PiiKind piiKind);

    void setContext(String str, Date date);

    void setContext(String str, Date date, PiiKind piiKind);

    void setContext(String str, UUID uuid);

    void setContext(String str, UUID uuid, PiiKind piiKind);

    void setContext(String str, boolean z);

    void setContext(String str, boolean z, PiiKind piiKind);

    void setLevel(DiagnosticLevel diagnosticLevel);

    void setParentContext(ISemanticContext iSemanticContext);
}

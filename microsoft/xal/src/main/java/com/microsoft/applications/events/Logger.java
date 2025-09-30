package com.microsoft.applications.events;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

class Logger implements ILogger {
    private long m_nativePtr;

    private native long nativeGetSemanticContext(long j);

    private native void nativeLogAggregatedMetric(long j, String str, long j2, long j3, String str2, String str3, int i, int i2, double d, long j4, long j5, Object[] objArr, Object[] objArr2);

    private native void nativeLogAggregatedMetricData(long j, String str, long j2, long j3, String str2, String str3, String str4, String str5, int[] iArr, double[] dArr, long[] jArr, long[] jArr2, String str6, String str7, int i, int i2, double d, long j4, long j5, Object[] objArr, Object[] objArr2);

    private native void nativeLogAppLifecycle(long j, int i, String str, String str2, int i2, int i3, double d, long j2, long j3, Object[] objArr, Object[] objArr2);

    private native void nativeLogEventName(long j, String str);

    private native void nativeLogEventProperties(long j, String str, String str2, int i, int i2, double d, long j2, long j3, Object[] objArr, Object[] objArr2);

    private native void nativeLogFailure(long j, String str, String str2, String str3, String str4, int i, int i2, double d, long j2, long j3, Object[] objArr, Object[] objArr2);

    private native void nativeLogFailureWithCategoryId(long j, String str, String str2, String str3, String str4, String str5, String str6, int i, int i2, double d, long j2, long j3, Object[] objArr, Object[] objArr2);

    private native void nativeLogPageAction(long j, String str, int i, String str2, String str3, int i2, int i3, double d, long j2, long j3, Object[] objArr, Object[] objArr2);

    private native void nativeLogPageActionData(long j, String str, int i, int i2, int i3, String str2, String str3, String str4, String str5, String str6, short s, String str7, String str8, String str9, int i4, int i5, double d, long j2, long j3, Object[] objArr, Object[] objArr2);

    private native void nativeLogPageView(long j, String str, String str2, String str3, String str4, int i, int i2, double d, long j2, long j3, Object[] objArr, Object[] objArr2);

    private native void nativeLogPageViewWithUri(long j, String str, String str2, String str3, String str4, String str5, String str6, String str7, int i, int i2, double d, long j2, long j3, Object[] objArr, Object[] objArr2);

    private native void nativeLogSampledMetric(long j, String str, double d, String str2, String str3, String str4, int i, int i2, double d2, long j2, long j3, Object[] objArr, Object[] objArr2);

    private native void nativeLogSampledMetricWithObjectId(long j, String str, double d, String str2, String str3, String str4, String str5, String str6, String str7, int i, int i2, double d2, long j2, long j3, Object[] objArr, Object[] objArr2);

    private native void nativeLogSession(long j, int i, String str, String str2, int i2, int i3, double d, long j2, long j3, Object[] objArr, Object[] objArr2);

    private native void nativeLogTrace(long j, int i, String str, String str2, String str3, int i2, int i3, double d, long j2, long j3, Object[] objArr, Object[] objArr2);

    private native void nativeLogUserState(long j, int i, long j2, String str, String str2, int i2, int i3, double d, long j3, long j4, Object[] objArr, Object[] objArr2);

    private static native void nativeSetContextBoolValue(long j, String str, boolean z, int i);

    private static native void nativeSetContextDoubleValue(long j, String str, double d, int i);

    private native void nativeSetContextEventProperty(long j, String str, EventProperty eventProperty);

    private static native void nativeSetContextGuidValue(long j, String str, String str2, int i);

    private static native void nativeSetContextIntValue(long j, String str, int i, int i2);

    private static native void nativeSetContextLongValue(long j, String str, long j2, int i);

    private static native void nativeSetContextStringValue(long j, String str, String str2, int i);

    private static native void nativeSetContextTimeTicksValue(long j, String str, long j2, int i);

    private native void nativeSetLevel(long j, int i);

    private native void nativeSetParentContext(long j, long j2);

    Logger(long j) {
        this.m_nativePtr = j;
        LogManager.registerLogger(this);
    }

    @Override
    public ISemanticContext getSemanticContext() {
        return new SemanticContext(nativeGetSemanticContext(this.m_nativePtr));
    }

    @Override
    public void setContext(String str, String str2, PiiKind piiKind) {
        if (str == null || !Utils.validatePropertyName(str)) {
            throw new IllegalArgumentException("name is null or invalid");
        }
        if (str2 == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        nativeSetContextStringValue(this.m_nativePtr, str, str2, piiKind.getValue());
    }

    @Override  
    public void setContext(String str, String str2) {
        setContext(str, str2, PiiKind.None);
    }

    @Override  
    public void setContext(String str, double d, PiiKind piiKind) {
        if (str == null || !Utils.validatePropertyName(str)) {
            throw new IllegalArgumentException("name is null or invalid");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        nativeSetContextDoubleValue(this.m_nativePtr, str, d, piiKind.getValue());
    }

    @Override  
    public void setContext(String str, double d) {
        setContext(str, d, PiiKind.None);
    }

    @Override  
    public void setContext(String str, long j, PiiKind piiKind) {
        if (str == null || !Utils.validatePropertyName(str)) {
            throw new IllegalArgumentException("name is null or invalid");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        nativeSetContextLongValue(this.m_nativePtr, str, j, piiKind.getValue());
    }

    @Override  
    public void setContext(String str, long j) {
        setContext(str, j, PiiKind.None);
    }

    @Override  
    public void setContext(String str, int i, PiiKind piiKind) {
        if (str == null || !Utils.validatePropertyName(str)) {
            throw new IllegalArgumentException("name is null or invalid");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        nativeSetContextIntValue(this.m_nativePtr, str, i, piiKind.getValue());
    }

    @Override  
    public void setContext(String str, int i) {
        setContext(str, i, PiiKind.None);
    }

    @Override  
    public void setContext(String str, boolean z, PiiKind piiKind) {
        if (str == null || !Utils.validatePropertyName(str)) {
            throw new IllegalArgumentException("name is null or invalid");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        nativeSetContextBoolValue(this.m_nativePtr, str, z, piiKind.getValue());
    }

    @Override  
    public void setContext(String str, boolean z) {
        setContext(str, z, PiiKind.None);
    }

    private void setContext(String str, TimeTicks timeTicks, PiiKind piiKind) {
        if (str == null || !Utils.validatePropertyName(str)) {
            throw new IllegalArgumentException("name is null or invalid");
        }
        if (timeTicks == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        nativeSetContextTimeTicksValue(this.m_nativePtr, str, timeTicks.getTicks(), piiKind.getValue());
    }

    @Override  
    public void setContext(String str, Date date, PiiKind piiKind) {
        setContext(str, new TimeTicks(date), piiKind);
    }

    @Override  
    public void setContext(String str, Date date) {
        setContext(str, date, PiiKind.None);
    }

    @Override  
    public void setContext(String str, UUID uuid, PiiKind piiKind) {
        if (str == null || !Utils.validatePropertyName(str)) {
            throw new IllegalArgumentException("name is null or invalid");
        }
        if (uuid == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        nativeSetContextGuidValue(this.m_nativePtr, str, uuid.toString(), piiKind.getValue());
    }

    @Override  
    public void setContext(String str, UUID uuid) {
        setContext(str, uuid, PiiKind.None);
    }

    @Override  
    public void SetContext(String str, EventProperty eventProperty) {
        if (str == null || !Utils.validatePropertyName(str)) {
            throw new IllegalArgumentException("name is null or invalid");
        }
        if (eventProperty == null) {
            throw new IllegalArgumentException("prop is null");
        }
        nativeSetContextEventProperty(this.m_nativePtr, str, eventProperty);
    }

    @Override  
    public void setParentContext(ISemanticContext iSemanticContext) {
        if (iSemanticContext == null) {
            throw new IllegalArgumentException("context is null");
        }
        nativeSetParentContext(this.m_nativePtr, ((SemanticContext) iSemanticContext).getNativeSemanticContextPtr());
    }

    @Override  
    public void logAppLifecycle(AppLifecycleState appLifecycleState, EventProperties eventProperties) {
        if (appLifecycleState == null) {
            throw new IllegalArgumentException("state is null");
        }
        if (eventProperties == null) {
            throw new IllegalArgumentException("properties is null");
        }
        String name = eventProperties.getName();
        String type = eventProperties.getType();
        EventLatency latency = eventProperties.getLatency();
        EventPersistence persistence = eventProperties.getPersistence();
        double popSample = eventProperties.getPopSample();
        long policyBitFlags = eventProperties.getPolicyBitFlags();
        long timestamp = eventProperties.getTimestamp();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Map.Entry<String, EventProperty> entry : eventProperties.getProperties().entrySet()) {
            arrayList.add(entry.getKey());
            arrayList2.add(entry.getValue());
        }
        nativeLogAppLifecycle(this.m_nativePtr, appLifecycleState.getValue(), name, type, latency.getValue(), persistence.getValue(), popSample, policyBitFlags, timestamp, arrayList.toArray(), arrayList2.toArray());
    }

    @Override  
    public void logSession(SessionState sessionState, EventProperties eventProperties) {
        if (sessionState == null) {
            throw new IllegalArgumentException("state is null");
        }
        if (eventProperties == null) {
            throw new IllegalArgumentException("properties is null");
        }
        String name = eventProperties.getName();
        String type = eventProperties.getType();
        EventLatency latency = eventProperties.getLatency();
        EventPersistence persistence = eventProperties.getPersistence();
        double popSample = eventProperties.getPopSample();
        long policyBitFlags = eventProperties.getPolicyBitFlags();
        long timestamp = eventProperties.getTimestamp();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Map.Entry<String, EventProperty> entry : eventProperties.getProperties().entrySet()) {
            arrayList.add(entry.getKey());
            arrayList2.add(entry.getValue());
        }
        nativeLogSession(this.m_nativePtr, sessionState.getValue(), name, type, latency.getValue(), persistence.getValue(), popSample, policyBitFlags, timestamp, arrayList.toArray(), arrayList2.toArray());
    }

    @Override  
    public void logEvent(String str) {
        if (str == null || !Utils.validateEventName(str)) {
            throw new IllegalArgumentException("name is null or invalid");
        }
        nativeLogEventName(this.m_nativePtr, str);
    }

    @Override  
    public void logEvent(EventProperties eventProperties) {
        if (eventProperties == null) {
            throw new IllegalArgumentException("properties is null");
        }
        String name = eventProperties.getName();
        String type = eventProperties.getType();
        EventLatency latency = eventProperties.getLatency();
        EventPersistence persistence = eventProperties.getPersistence();
        double popSample = eventProperties.getPopSample();
        long policyBitFlags = eventProperties.getPolicyBitFlags();
        long timestamp = eventProperties.getTimestamp();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Map.Entry<String, EventProperty> entry : eventProperties.getProperties().entrySet()) {
            arrayList.add(entry.getKey());
            arrayList2.add(entry.getValue());
        }
        nativeLogEventProperties(this.m_nativePtr, name, type, latency.getValue(), persistence.getValue(), popSample, policyBitFlags, timestamp, arrayList.toArray(), arrayList2.toArray());
    }

    @Override  
    public void logFailure(String str, String str2, EventProperties eventProperties) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("signature is null or empty");
        }
        if (str2 == null || str2.trim().isEmpty()) {
            throw new IllegalArgumentException("detail is null or empty");
        }
        if (eventProperties == null) {
            throw new IllegalArgumentException("properties is null");
        }
        String name = eventProperties.getName();
        String type = eventProperties.getType();
        EventLatency latency = eventProperties.getLatency();
        EventPersistence persistence = eventProperties.getPersistence();
        double popSample = eventProperties.getPopSample();
        long policyBitFlags = eventProperties.getPolicyBitFlags();
        long timestamp = eventProperties.getTimestamp();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Map.Entry<String, EventProperty> entry : eventProperties.getProperties().entrySet()) {
            arrayList.add(entry.getKey());
            arrayList2.add(entry.getValue());
        }
        nativeLogFailure(this.m_nativePtr, str, str2, name, type, latency.getValue(), persistence.getValue(), popSample, policyBitFlags, timestamp, arrayList.toArray(), arrayList2.toArray());
    }

    @Override  
    public void logFailure(String str, String str2, String str3, String str4, EventProperties eventProperties) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("signature is null or empty");
        }
        if (str2 == null || str2.trim().isEmpty()) {
            throw new IllegalArgumentException("detail is null or empty");
        }
        if (eventProperties == null) {
            throw new IllegalArgumentException("properties is null");
        }
        String name = eventProperties.getName();
        String type = eventProperties.getType();
        EventLatency latency = eventProperties.getLatency();
        EventPersistence persistence = eventProperties.getPersistence();
        double popSample = eventProperties.getPopSample();
        long policyBitFlags = eventProperties.getPolicyBitFlags();
        long timestamp = eventProperties.getTimestamp();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Map.Entry<String, EventProperty> entry : eventProperties.getProperties().entrySet()) {
            arrayList.add(entry.getKey());
            arrayList2.add(entry.getValue());
        }
        nativeLogFailureWithCategoryId(this.m_nativePtr, str, str2, str3, str4, name, type, latency.getValue(), persistence.getValue(), popSample, policyBitFlags, timestamp, arrayList.toArray(), arrayList2.toArray());
    }

    @Override  
    public void logPageView(String str, String str2, EventProperties eventProperties) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("id is null or empty");
        }
        if (str2 == null || str2.trim().isEmpty()) {
            throw new IllegalArgumentException("pageName is null or empty");
        }
        if (eventProperties == null) {
            throw new IllegalArgumentException("properties is null");
        }
        String name = eventProperties.getName();
        String type = eventProperties.getType();
        EventLatency latency = eventProperties.getLatency();
        EventPersistence persistence = eventProperties.getPersistence();
        double popSample = eventProperties.getPopSample();
        long policyBitFlags = eventProperties.getPolicyBitFlags();
        long timestamp = eventProperties.getTimestamp();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Map.Entry<String, EventProperty> entry : eventProperties.getProperties().entrySet()) {
            arrayList.add(entry.getKey());
            arrayList2.add(entry.getValue());
        }
        nativeLogPageView(this.m_nativePtr, str, str2, name, type, latency.getValue(), persistence.getValue(), popSample, policyBitFlags, timestamp, arrayList.toArray(), arrayList2.toArray());
    }

    @Override  
    public void logPageView(String str, String str2, String str3, String str4, String str5, EventProperties eventProperties) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("id is null or empty");
        }
        if (str2 == null || str2.trim().isEmpty()) {
            throw new IllegalArgumentException("pageName is null or empty");
        }
        if (str3 == null || str3.trim().isEmpty()) {
            throw new IllegalArgumentException("category is null or empty");
        }
        if (str4 == null || str4.trim().isEmpty()) {
            throw new IllegalArgumentException("uri is null or empty");
        }
        if (str5 == null || str5.trim().isEmpty()) {
            throw new IllegalArgumentException("referrerUri is null or empty");
        }
        if (eventProperties == null) {
            throw new IllegalArgumentException("properties is null");
        }
        String name = eventProperties.getName();
        String type = eventProperties.getType();
        EventLatency latency = eventProperties.getLatency();
        EventPersistence persistence = eventProperties.getPersistence();
        double popSample = eventProperties.getPopSample();
        long policyBitFlags = eventProperties.getPolicyBitFlags();
        long timestamp = eventProperties.getTimestamp();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Map.Entry<String, EventProperty> entry : eventProperties.getProperties().entrySet()) {
            arrayList.add(entry.getKey());
            arrayList2.add(entry.getValue());
        }
        nativeLogPageViewWithUri(this.m_nativePtr, str, str2, str3, str4, str5, name, type, latency.getValue(), persistence.getValue(), popSample, policyBitFlags, timestamp, arrayList.toArray(), arrayList2.toArray());
    }

    @Override  
    public void logPageAction(String str, ActionType actionType, EventProperties eventProperties) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("pageViewId is null or empty");
        }
        if (actionType == null) {
            throw new IllegalArgumentException("actionType is null");
        }
        if (eventProperties == null) {
            throw new IllegalArgumentException("properties is null");
        }
        String name = eventProperties.getName();
        String type = eventProperties.getType();
        EventLatency latency = eventProperties.getLatency();
        EventPersistence persistence = eventProperties.getPersistence();
        double popSample = eventProperties.getPopSample();
        long policyBitFlags = eventProperties.getPolicyBitFlags();
        long timestamp = eventProperties.getTimestamp();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Map.Entry<String, EventProperty> entry : eventProperties.getProperties().entrySet()) {
            arrayList.add(entry.getKey());
            arrayList2.add(entry.getValue());
        }
        nativeLogPageAction(this.m_nativePtr, str, actionType.getValue(), name, type, latency.getValue(), persistence.getValue(), popSample, policyBitFlags, timestamp, arrayList.toArray(), arrayList2.toArray());
    }

    @Override  
    public void logPageAction(PageActionData pageActionData, EventProperties eventProperties) {
        if (pageActionData == null) {
            throw new IllegalArgumentException("pageActionData is null");
        }
        if (eventProperties == null) {
            throw new IllegalArgumentException("properties is null");
        }
        String name = eventProperties.getName();
        String type = eventProperties.getType();
        EventLatency latency = eventProperties.getLatency();
        EventPersistence persistence = eventProperties.getPersistence();
        double popSample = eventProperties.getPopSample();
        long policyBitFlags = eventProperties.getPolicyBitFlags();
        long timestamp = eventProperties.getTimestamp();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Map.Entry<String, EventProperty> entry : eventProperties.getProperties().entrySet()) {
            arrayList.add(entry.getKey());
            arrayList2.add(entry.getValue());
        }
        nativeLogPageActionData(this.m_nativePtr, pageActionData.pageViewId, pageActionData.actionType.getValue(), pageActionData.rawActionType.getValue(), pageActionData.inputDeviceType.getValue(), pageActionData.targetItemId, pageActionData.targetItemDataSourceName, pageActionData.targetItemDataSourceCategory, pageActionData.targetItemDataSourceCollection, pageActionData.targetItemLayoutContainer, pageActionData.targetItemLayoutRank, pageActionData.destinationUri, name, type, latency.getValue(), persistence.getValue(), popSample, policyBitFlags, timestamp, arrayList.toArray(), arrayList2.toArray());
    }

    @Override  
    public void logSampledMetric(String str, double d, String str2, EventProperties eventProperties) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        if (str2 == null || str2.trim().isEmpty()) {
            throw new IllegalArgumentException("units is null");
        }
        if (eventProperties == null) {
            throw new IllegalArgumentException("properties is null");
        }
        String name = eventProperties.getName();
        String type = eventProperties.getType();
        EventLatency latency = eventProperties.getLatency();
        EventPersistence persistence = eventProperties.getPersistence();
        double popSample = eventProperties.getPopSample();
        long policyBitFlags = eventProperties.getPolicyBitFlags();
        long timestamp = eventProperties.getTimestamp();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Map.Entry<String, EventProperty> entry : eventProperties.getProperties().entrySet()) {
            arrayList.add(entry.getKey());
            arrayList2.add(entry.getValue());
        }
        nativeLogSampledMetric(this.m_nativePtr, str, d, str2, name, type, latency.getValue(), persistence.getValue(), popSample, policyBitFlags, timestamp, arrayList.toArray(), arrayList2.toArray());
    }

    @Override  
    public void logSampledMetric(String str, double d, String str2, String str3, String str4, String str5, EventProperties eventProperties) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        if (str2 == null || str2.trim().isEmpty()) {
            throw new IllegalArgumentException("units is null");
        }
        if (str3 == null || str3.trim().isEmpty()) {
            throw new IllegalArgumentException("instanceName is null or empty");
        }
        if (str4 == null || str4.trim().isEmpty()) {
            throw new IllegalArgumentException("objectClass is null or empty");
        }
        if (str5 == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("objectId is null or empty");
        }
        if (eventProperties == null) {
            throw new IllegalArgumentException("properties is null");
        }
        String name = eventProperties.getName();
        String type = eventProperties.getType();
        EventLatency latency = eventProperties.getLatency();
        EventPersistence persistence = eventProperties.getPersistence();
        double popSample = eventProperties.getPopSample();
        long policyBitFlags = eventProperties.getPolicyBitFlags();
        long timestamp = eventProperties.getTimestamp();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Map.Entry<String, EventProperty> entry : eventProperties.getProperties().entrySet()) {
            arrayList.add(entry.getKey());
            arrayList2.add(entry.getValue());
        }
        nativeLogSampledMetricWithObjectId(this.m_nativePtr, str, d, str2, str3, str4, str5, name, type, latency.getValue(), persistence.getValue(), popSample, policyBitFlags, timestamp, arrayList.toArray(), arrayList2.toArray());
    }

    @Override  
    public void logAggregatedMetric(String str, long j, long j2, EventProperties eventProperties) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        if (eventProperties == null) {
            throw new IllegalArgumentException("properties is null");
        }
        String name = eventProperties.getName();
        String type = eventProperties.getType();
        EventLatency latency = eventProperties.getLatency();
        EventPersistence persistence = eventProperties.getPersistence();
        double popSample = eventProperties.getPopSample();
        long policyBitFlags = eventProperties.getPolicyBitFlags();
        long timestamp = eventProperties.getTimestamp();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Map.Entry<String, EventProperty> entry : eventProperties.getProperties().entrySet()) {
            arrayList.add(entry.getKey());
            arrayList2.add(entry.getValue());
        }
        nativeLogAggregatedMetric(this.m_nativePtr, str, j, j2, name, type, latency.getValue(), persistence.getValue(), popSample, policyBitFlags, timestamp, arrayList.toArray(), arrayList2.toArray());
    }

    @Override  
    public void logAggregatedMetric(AggregatedMetricData aggregatedMetricData, EventProperties eventProperties) {
        if (aggregatedMetricData == null) {
            throw new IllegalArgumentException("metricData is null");
        }
        if (eventProperties == null) {
            throw new IllegalArgumentException("properties is null");
        }
        String name = eventProperties.getName();
        String type = eventProperties.getType();
        EventLatency latency = eventProperties.getLatency();
        EventPersistence persistence = eventProperties.getPersistence();
        double popSample = eventProperties.getPopSample();
        long policyBitFlags = eventProperties.getPolicyBitFlags();
        long timestamp = eventProperties.getTimestamp();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Map.Entry<String, EventProperty> entry : eventProperties.getProperties().entrySet()) {
            arrayList.add(entry.getKey());
            arrayList2.add(entry.getValue());
        }
        int[] iArr = new int[aggregatedMetricData.aggregates.size()];
        double[] dArr = new double[aggregatedMetricData.aggregates.size()];
        int i = 0;
        int i2 = 0;
        for (Map.Entry<AggregateType, Double> entry2 : aggregatedMetricData.aggregates.entrySet()) {
            iArr[i2] = entry2.getKey().getValue();
            dArr[i2] = entry2.getValue().doubleValue();
            i2++;
        }
        long[] jArr = new long[aggregatedMetricData.buckets.size()];
        long[] jArr2 = new long[aggregatedMetricData.buckets.size()];
        for (Map.Entry<Long, Long> entry3 : aggregatedMetricData.buckets.entrySet()) {
            jArr[i] = entry3.getKey().longValue();
            jArr2[i] = entry3.getValue().longValue();
            i++;
        }
        nativeLogAggregatedMetricData(this.m_nativePtr, aggregatedMetricData.name, aggregatedMetricData.duration, aggregatedMetricData.count, aggregatedMetricData.units, aggregatedMetricData.instanceName, aggregatedMetricData.objectClass, aggregatedMetricData.objectId, iArr, dArr, jArr, jArr2, name, type, latency.getValue(), persistence.getValue(), popSample, policyBitFlags, timestamp, arrayList.toArray(), arrayList2.toArray());
    }

    @Override  
    public void logTrace(TraceLevel traceLevel, String str, EventProperties eventProperties) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("message is null or empty");
        }
        if (traceLevel == null) {
            throw new IllegalArgumentException("level is null");
        }
        String name = eventProperties.getName();
        String type = eventProperties.getType();
        EventLatency latency = eventProperties.getLatency();
        EventPersistence persistence = eventProperties.getPersistence();
        double popSample = eventProperties.getPopSample();
        long policyBitFlags = eventProperties.getPolicyBitFlags();
        long timestamp = eventProperties.getTimestamp();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Map.Entry<String, EventProperty> entry : eventProperties.getProperties().entrySet()) {
            arrayList.add(entry.getKey());
            arrayList2.add(entry.getValue());
        }
        nativeLogTrace(this.m_nativePtr, traceLevel.getValue(), str, name, type, latency.getValue(), persistence.getValue(), popSample, policyBitFlags, timestamp, arrayList.toArray(), arrayList2.toArray());
    }

    @Override  
    public void logUserState(UserState userState, long j, EventProperties eventProperties) {
        if (userState == null) {
            throw new IllegalArgumentException("state is null");
        }
        String name = eventProperties.getName();
        String type = eventProperties.getType();
        EventLatency latency = eventProperties.getLatency();
        EventPersistence persistence = eventProperties.getPersistence();
        double popSample = eventProperties.getPopSample();
        long policyBitFlags = eventProperties.getPolicyBitFlags();
        long timestamp = eventProperties.getTimestamp();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Map.Entry<String, EventProperty> entry : eventProperties.getProperties().entrySet()) {
            arrayList.add(entry.getKey());
            arrayList2.add(entry.getValue());
        }
        nativeLogUserState(this.m_nativePtr, userState.getValue(), j, name, type, latency.getValue(), persistence.getValue(), popSample, policyBitFlags, timestamp, arrayList.toArray(), arrayList2.toArray());
    }

    @Override  
    public void setLevel(DiagnosticLevel diagnosticLevel) {
        nativeSetLevel(this.m_nativePtr, diagnosticLevel.getValue());
    }

    @Override // java.lang.AutoCloseable
    public void close() {
        LogManager.removeLogger(this);
        clearNative();
    }

    @Override  
    public long getNativeILoggerPtr() {
        return this.m_nativePtr;
    }

    public synchronized void clearNative() {
        this.m_nativePtr = 0L;
    }
}

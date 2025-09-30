package com.microsoft.applications.events;

import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class EventProperties {
    private static final String DEFAULT_EVENT_NAME = "undefined";
    private EventPropertiesStorage mStorage;

    public EventProperties(String str, DiagnosticLevel diagnosticLevel) {
        this.mStorage = new EventPropertiesStorage();
        if (!setName(str)) {
            throw new IllegalArgumentException("name is invalid");
        }
        setLevel(diagnosticLevel);
    }

    public EventProperties(String str) {
        this(str, DiagnosticLevel.DIAG_LEVEL_OPTIONAL);
    }

    public EventProperties(EventProperties eventProperties) {
        if (eventProperties == null) {
            throw new IllegalArgumentException("copy is null");
        }
        this.mStorage = new EventPropertiesStorage(eventProperties.mStorage);
    }

    public EventProperties(String str, Map<String, EventProperty> map) {
        this(str);
        addProperties(map);
    }

    public void addProperties(Map<String, EventProperty> map) {
        this.mStorage.addProperties(map);
    }

    public void setProperties(Map<String, EventProperty> map) {
        this.mStorage.setProperties(map);
    }

    public boolean setName(String str) {
        if (str == null || !Utils.validateEventName(str)) {
            return false;
        }
        this.mStorage.eventName = str;
        return true;
    }

    public String getName() {
        return this.mStorage.eventName;
    }

    public boolean setType(String str) {
        if (str == null || !Utils.validateEventName(str)) {
            return false;
        }
        this.mStorage.eventType = str;
        return true;
    }

    public String getType() {
        return this.mStorage.eventType;
    }

    public void setTimestamp(long j) {
        this.mStorage.timestampInMillis = j;
    }

    public long getTimestamp() {
        return this.mStorage.timestampInMillis;
    }

    public void setPriority(EventPriority eventPriority) {
        this.mStorage.eventLatency = EventLatency.getEnum(eventPriority.getValue());
        if (eventPriority.getValue() >= EventPriority.High.getValue()) {
            this.mStorage.eventLatency = EventLatency.RealTime;
            this.mStorage.eventPersistence = EventPersistence.Critical;
            return;
        }
        if (eventPriority.getValue() >= EventPriority.Low.getValue()) {
            this.mStorage.eventLatency = EventLatency.Normal;
            this.mStorage.eventPersistence = EventPersistence.Normal;
        }
    }

    public EventPriority getPriority() {
        return EventPriority.getEnum(this.mStorage.eventLatency.getValue());
    }

    public void setLatency(EventLatency eventLatency) {
        this.mStorage.eventLatency = eventLatency;
    }

    public EventLatency getLatency() {
        return this.mStorage.eventLatency;
    }

    public void setPersistence(EventPersistence eventPersistence) {
        this.mStorage.eventPersistence = eventPersistence;
    }

    public EventPersistence getPersistence() {
        return this.mStorage.eventPersistence;
    }

    public void setPopSample(double d) {
        this.mStorage.eventPopSample = d;
    }

    public double getPopSample() {
        return this.mStorage.eventPopSample;
    }

    public void setPolicyBitFlags(long j) {
        this.mStorage.eventPolicyBitflags = j;
    }

    public long getPolicyBitFlags() {
        return this.mStorage.eventPolicyBitflags;
    }

    public void setLevel(DiagnosticLevel diagnosticLevel) {
        setProperty(Constants.COMMONFIELDS_EVENT_LEVEL, diagnosticLevel.getValue());
    }

    public void setPrivacyTags(PrivacyDiagnosticTag privacyDiagnosticTag) {
        if (privacyDiagnosticTag == null) {
            throw new IllegalArgumentException("tag is null");
        }
        setPrivacyTags(EnumSet.of(privacyDiagnosticTag));
    }

    public void setPrivacyTags(EnumSet<PrivacyDiagnosticTag> enumSet) {
        if (enumSet == null) {
            throw new IllegalArgumentException("tags is null");
        }
        Iterator it = enumSet.iterator();
        long value = 0;
        while (it.hasNext()) {
            value |= ((PrivacyDiagnosticTag) it.next()).getValue();
        }
        if (value == 0) {
            throw new IllegalArgumentException("EnumSet of tags is empty");
        }
    }

    public void setPrivacyMetadata(PrivacyDiagnosticTag privacyDiagnosticTag, DiagnosticLevel diagnosticLevel) {
        if (privacyDiagnosticTag == null) {
            throw new IllegalArgumentException("tag is null");
        }
        setPrivacyMetadata(EnumSet.of(privacyDiagnosticTag), diagnosticLevel);
    }

    public void setPrivacyMetadata(EnumSet<PrivacyDiagnosticTag> enumSet, DiagnosticLevel diagnosticLevel) {
        setLevel(diagnosticLevel);
        setPrivacyTags(enumSet);
    }

    public void setProperty(String str, EventProperty eventProperty) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        if (!Utils.validatePropertyName(str)) {
            throw new IllegalArgumentException("name:" + str + " is invalid.");
        }
        if (eventProperty == null) {
            throw new IllegalArgumentException("prop is null");
        }
        this.mStorage.properties.put(str, eventProperty);
    }

    public void setProperty(String str, String str2) {
        setProperty(str, str2, PiiKind.None);
    }

    public void setProperty(String str, String str2, PiiKind piiKind) {
        setProperty(str, str2, piiKind, DataCategory.PartC);
    }

    public void setProperty(String str, String str2, PiiKind piiKind, DataCategory dataCategory) {
        setProperty(str, new EventProperty(str2, piiKind, dataCategory));
    }

    public void setProperty(String str, double d) {
        setProperty(str, d, PiiKind.None);
    }

    public void setProperty(String str, double d, PiiKind piiKind) {
        setProperty(str, d, piiKind, DataCategory.PartC);
    }

    void setProperty(String str, double d, PiiKind piiKind, DataCategory dataCategory) {
        setProperty(str, new EventProperty(d, piiKind, dataCategory));
    }

    public void setProperty(String str, int i) {
        setProperty(str, i, PiiKind.None);
    }

    public void setProperty(String str, int i, PiiKind piiKind) {
        setProperty(str, i, piiKind, DataCategory.PartC);
    }

    void setProperty(String str, int i, PiiKind piiKind, DataCategory dataCategory) {
        setProperty(str, new EventProperty(i, piiKind, dataCategory));
    }

    public void setProperty(String str, long j) {
        setProperty(str, j, PiiKind.None);
    }

    public void setProperty(String str, long j, PiiKind piiKind) {
        setProperty(str, j, piiKind, DataCategory.PartC);
    }

    void setProperty(String str, long j, PiiKind piiKind, DataCategory dataCategory) {
        setProperty(str, new EventProperty(j, piiKind, dataCategory));
    }

    public void setProperty(String str, boolean z) {
        setProperty(str, z, PiiKind.None);
    }

    public void setProperty(String str, boolean z, PiiKind piiKind) {
        setProperty(str, z, piiKind, DataCategory.PartC);
    }

    void setProperty(String str, boolean z, PiiKind piiKind, DataCategory dataCategory) {
        setProperty(str, new EventProperty(z, piiKind, dataCategory));
    }

    public void setProperty(String str, Date date) {
        setProperty(str, date, PiiKind.None);
    }

    public void setProperty(String str, Date date, PiiKind piiKind) {
        setProperty(str, date, piiKind, DataCategory.PartC);
    }

    void setProperty(String str, Date date, PiiKind piiKind, DataCategory dataCategory) {
        setProperty(str, new EventProperty(date, piiKind, dataCategory));
    }

    public void setProperty(String str, UUID uuid) {
        setProperty(str, uuid, PiiKind.None);
    }

    public void setProperty(String str, UUID uuid, PiiKind piiKind) {
        setProperty(str, uuid, piiKind, DataCategory.PartC);
    }

    void setProperty(String str, UUID uuid, PiiKind piiKind, DataCategory dataCategory) {
        setProperty(str, new EventProperty(uuid, piiKind, dataCategory));
    }

    public void setProperty(String str, String[] strArr) {
        setProperty(str, strArr, PiiKind.None);
    }

    public void setProperty(String str, String[] strArr, PiiKind piiKind) {
        setProperty(str, strArr, piiKind, DataCategory.PartC);
    }

    void setProperty(String str, String[] strArr, PiiKind piiKind, DataCategory dataCategory) {
        setProperty(str, new EventProperty(strArr, piiKind, dataCategory));
    }

    public void setProperty(String str, UUID[] uuidArr) {
        setProperty(str, uuidArr, PiiKind.None);
    }

    public void setProperty(String str, UUID[] uuidArr, PiiKind piiKind) {
        setProperty(str, uuidArr, piiKind, DataCategory.PartC);
    }

    void setProperty(String str, UUID[] uuidArr, PiiKind piiKind, DataCategory dataCategory) {
        setProperty(str, new EventProperty(uuidArr, piiKind, dataCategory));
    }

    public void setProperty(String str, double[] dArr) {
        setProperty(str, dArr, PiiKind.None);
    }

    public void setProperty(String str, double[] dArr, PiiKind piiKind) {
        setProperty(str, dArr, piiKind, DataCategory.PartC);
    }

    void setProperty(String str, double[] dArr, PiiKind piiKind, DataCategory dataCategory) {
        setProperty(str, new EventProperty(dArr, piiKind, dataCategory));
    }

    public void setProperty(String str, long[] jArr) {
        setProperty(str, jArr, PiiKind.None);
    }

    public void setProperty(String str, long[] jArr, PiiKind piiKind) {
        setProperty(str, jArr, piiKind, DataCategory.PartC);
    }

    void setProperty(String str, long[] jArr, PiiKind piiKind, DataCategory dataCategory) {
        setProperty(str, new EventProperty(jArr, piiKind, dataCategory));
    }

    public Map<String, EventProperty> getProperties() {
        return getProperties(DataCategory.PartC);
    }

    public Map<String, EventProperty> getProperties(DataCategory dataCategory) {
        if (dataCategory == DataCategory.PartC) {
            return this.mStorage.properties;
        }
        return this.mStorage.propertiesPartB;
    }

    boolean erase(String str) {
        return erase(str, DataCategory.PartC);
    }

    boolean erase(String str, DataCategory dataCategory) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("key is null or empty");
        }
        if (dataCategory != null) {
            return (dataCategory == DataCategory.PartC ? this.mStorage.properties : this.mStorage.propertiesPartB).remove(str) == null;
        }
        throw new IllegalArgumentException("category is null");
    }
}

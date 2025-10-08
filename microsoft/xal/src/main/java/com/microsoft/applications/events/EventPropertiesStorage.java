package com.microsoft.applications.events;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

class EventPropertiesStorage {
    EventLatency eventLatency;
    String eventName;
    EventPersistence eventPersistence;
    long eventPolicyBitflags;
    double eventPopSample;
    String eventType;
    Map<String, EventProperty> properties;
    Map<String, EventProperty> propertiesPartB;
    long timestampInMillis;

    EventPropertiesStorage() {
        this.eventName = "";
        this.eventLatency = EventLatency.Normal;
        this.eventPersistence = EventPersistence.Normal;
        this.eventPopSample = 100.0d;
        this.eventPolicyBitflags = 0L;
        this.timestampInMillis = 0L;
        this.properties = new HashMap();
        this.propertiesPartB = new HashMap();
    }

    EventPropertiesStorage(EventPropertiesStorage eventPropertiesStorage) {
        if (eventPropertiesStorage == null) {
            throw new IllegalArgumentException("other is null");
        }
        this.eventName = eventPropertiesStorage.eventName;
        this.eventType = eventPropertiesStorage.eventType;
        this.eventLatency = eventPropertiesStorage.eventLatency;
        this.eventPersistence = eventPropertiesStorage.eventPersistence;
        this.eventPopSample = eventPropertiesStorage.eventPopSample;
        this.eventPolicyBitflags = eventPropertiesStorage.eventPolicyBitflags;
        this.timestampInMillis = eventPropertiesStorage.timestampInMillis;
        this.properties = eventPropertiesStorage.properties;
        this.propertiesPartB = eventPropertiesStorage.propertiesPartB;
    }

    void addProperties(Map<String, EventProperty> map) {
        if (map == null) {
            throw new IllegalArgumentException("properties is null");
        }
        this.properties.putAll(map);
    }

    void setProperties(Map<String, EventProperty> map) {
        if (map == null) {
            throw new IllegalArgumentException("properties is null");
        }
        this.properties = map;
    }
}

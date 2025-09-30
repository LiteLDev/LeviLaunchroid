package com.microsoft.applications.events;

import java.util.Date;
import java.util.UUID;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class EventProperty {
    private DataCategory m_category;
    private EventPropertyValue m_eventPropertyValue;
    private PiiKind m_piiKind;

    public PiiKind getPiiKind() {
        return this.m_piiKind;
    }

    int getPiiKindValue() {
        return this.m_piiKind.getValue();
    }

    int getDataCategoryValue() {
        return this.m_category.getValue();
    }

    DataCategory getDataCategory() {
        return this.m_category;
    }

    public EventPropertyValue getEventPropertyValue() {
        return this.m_eventPropertyValue;
    }

    public EventProperty(String str) {
        this(str, PiiKind.None);
    }

    public EventProperty(String str, PiiKind piiKind) {
        this(str, piiKind, DataCategory.PartC);
    }

    public EventProperty(String str, PiiKind piiKind, DataCategory dataCategory) {
        this.m_category = DataCategory.PartC;
        if (str == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        if (dataCategory == null) {
            throw new IllegalArgumentException("category is null");
        }
        this.m_piiKind = piiKind;
        this.m_category = dataCategory;
        this.m_eventPropertyValue = new EventPropertyStringValue(str);
    }

    public EventProperty(int i) {
        this(i, PiiKind.None);
    }

    public EventProperty(int i, PiiKind piiKind) {
        this(i, piiKind, DataCategory.PartC);
    }

    public EventProperty(int i, PiiKind piiKind, DataCategory dataCategory) {
    }

    public EventProperty(long j) {
        this(j, PiiKind.None);
    }

    public EventProperty(long j, PiiKind piiKind) {
        this(j, piiKind, DataCategory.PartC);
    }

    public EventProperty(long j, PiiKind piiKind, DataCategory dataCategory) {
        this.m_category = DataCategory.PartC;
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        if (dataCategory == null) {
            throw new IllegalArgumentException("category is null");
        }
        this.m_piiKind = piiKind;
        this.m_category = dataCategory;
        this.m_eventPropertyValue = new EventPropertyLongValue(j);
    }

    public EventProperty(double d) {
        this(d, PiiKind.None);
    }

    public EventProperty(double d, PiiKind piiKind) {
        this(d, piiKind, DataCategory.PartC);
    }

    public EventProperty(double d, PiiKind piiKind, DataCategory dataCategory) {
        this.m_category = DataCategory.PartC;
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        if (dataCategory == null) {
            throw new IllegalArgumentException("category is null");
        }
        this.m_piiKind = piiKind;
        this.m_category = dataCategory;
        this.m_eventPropertyValue = new EventPropertyDoubleValue(d);
    }

    public EventProperty(boolean z) {
        this(z, PiiKind.None);
    }

    public EventProperty(boolean z, PiiKind piiKind) {
        this(z, piiKind, DataCategory.PartC);
    }

    public EventProperty(boolean z, PiiKind piiKind, DataCategory dataCategory) {
        this.m_category = DataCategory.PartC;
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        if (dataCategory == null) {
            throw new IllegalArgumentException("category is null");
        }
        this.m_piiKind = piiKind;
        this.m_category = dataCategory;
        this.m_eventPropertyValue = new EventPropertyBooleanValue(z);
    }

    public EventProperty(Date date) {
        this(date, PiiKind.None);
    }

    public EventProperty(Date date, PiiKind piiKind) {
        this(date, piiKind, DataCategory.PartC);
    }

    public EventProperty(Date date, PiiKind piiKind, DataCategory dataCategory) {
        this(new TimeTicks(date), piiKind, dataCategory);
    }

    private EventProperty(TimeTicks timeTicks, PiiKind piiKind, DataCategory dataCategory) {
        this.m_category = DataCategory.PartC;
        if (timeTicks == null) {
            throw new IllegalArgumentException("value is null or empty");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        if (dataCategory == null) {
            throw new IllegalArgumentException("category is null");
        }
        this.m_piiKind = piiKind;
        this.m_category = dataCategory;
        this.m_eventPropertyValue = new EventPropertyTimeTicksValue(timeTicks.getTicks());
    }

    public EventProperty(UUID uuid) {
        this(uuid, PiiKind.None);
    }

    public EventProperty(UUID uuid, PiiKind piiKind) {
        this(uuid, piiKind, DataCategory.PartC);
    }

    public EventProperty(UUID uuid, PiiKind piiKind, DataCategory dataCategory) {
        this.m_category = DataCategory.PartC;
        if (uuid == null) {
            throw new IllegalArgumentException("value is null or empty");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        if (dataCategory == null) {
            throw new IllegalArgumentException("category is null");
        }
        this.m_piiKind = piiKind;
        this.m_category = dataCategory;
        this.m_eventPropertyValue = new EventPropertyGuidValue(uuid);
    }

    public EventProperty(String[] strArr) {
        this(strArr, PiiKind.None);
    }

    public EventProperty(String[] strArr, PiiKind piiKind) {
        this(strArr, piiKind, DataCategory.PartC);
    }

    public EventProperty(String[] strArr, PiiKind piiKind, DataCategory dataCategory) {
        this.m_category = DataCategory.PartC;
        if (strArr == null || strArr.length == 0) {
            throw new IllegalArgumentException("value is null or empty");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        if (dataCategory == null) {
            throw new IllegalArgumentException("category is null");
        }
        this.m_piiKind = piiKind;
        this.m_category = dataCategory;
        this.m_eventPropertyValue = new EventPropertyStringArrayValue(strArr);
    }

    public EventProperty(long[] jArr) {
        this(jArr, PiiKind.None);
    }

    public EventProperty(long[] jArr, PiiKind piiKind) {
        this(jArr, piiKind, DataCategory.PartC);
    }

    public EventProperty(long[] jArr, PiiKind piiKind, DataCategory dataCategory) {
        this.m_category = DataCategory.PartC;
        if (jArr == null || jArr.length == 0) {
            throw new IllegalArgumentException("value is null or empty");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        if (dataCategory == null) {
            throw new IllegalArgumentException("category is null");
        }
        this.m_piiKind = piiKind;
        this.m_category = dataCategory;
        this.m_eventPropertyValue = new EventPropertyLongArrayValue(jArr);
    }

    public EventProperty(double[] dArr) {
        this(dArr, PiiKind.None);
    }

    public EventProperty(double[] dArr, PiiKind piiKind) {
        this(dArr, piiKind, DataCategory.PartC);
    }

    public EventProperty(double[] dArr, PiiKind piiKind, DataCategory dataCategory) {
        this.m_category = DataCategory.PartC;
        if (dArr == null || dArr.length == 0) {
            throw new IllegalArgumentException("value is null or empty");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        if (dataCategory == null) {
            throw new IllegalArgumentException("category is null");
        }
        this.m_piiKind = piiKind;
        this.m_category = dataCategory;
        this.m_eventPropertyValue = new EventPropertyDoubleArrayValue(dArr);
    }

    public EventProperty(UUID[] uuidArr) {
        this(uuidArr, PiiKind.None);
    }

    public EventProperty(UUID[] uuidArr, PiiKind piiKind) {
        this(uuidArr, piiKind, DataCategory.PartC);
    }

    public EventProperty(UUID[] uuidArr, PiiKind piiKind, DataCategory dataCategory) {
        this.m_category = DataCategory.PartC;
        if (uuidArr == null || uuidArr.length == 0) {
            throw new IllegalArgumentException("value is null or empty");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        if (dataCategory == null) {
            throw new IllegalArgumentException("category is null");
        }
        this.m_piiKind = piiKind;
        this.m_category = dataCategory;
        this.m_eventPropertyValue = new EventPropertyGuidArrayValue(uuidArr);
    }
}

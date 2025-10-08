package com.microsoft.applications.events;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class AggregatedMetricData {
    public long count;
    public long duration;
    public String name;
    public String units = "";
    public String instanceName = "";
    public String objectClass = "";
    public String objectId = "";
    public Map<AggregateType, Double> aggregates = new HashMap();
    public Map<Long, Long> buckets = new HashMap();

    public AggregatedMetricData(String str, long j, long j2) {
        this.name = str;
        this.duration = j;
        this.count = j2;
    }
}

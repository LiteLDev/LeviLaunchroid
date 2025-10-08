package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class ByTenant {
    public long count;
    public String tenantToken;

    public ByTenant(String str, Long l) {
        this.tenantToken = str;
        this.count = l.longValue();
    }
}

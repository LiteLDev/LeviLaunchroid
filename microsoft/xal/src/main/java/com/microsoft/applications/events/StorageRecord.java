package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class StorageRecord {
    public static final int EventLatency_Normal = 1;
    public static final int EventLatency_RealTime = 3;
    public static final int EventLatency_Unspecified = -1;
    public static final int EventPersistence_Normal = 1;
    public byte[] blob;
    public long id;
    public int latency;
    public int persistence;
    public long reservedUntil;
    public int retryCount;
    public String tenantToken;
    public long timestamp;

    public StorageRecord(long j, String str, int i, int i2, long j2, int i3, long j3, byte[] bArr) {
        this.id = j;
        this.tenantToken = str;
        this.latency = i;
        this.persistence = i2;
        this.timestamp = j2;
        this.retryCount = i3;
        this.reservedUntil = j3;
        this.blob = bArr;
    }
}

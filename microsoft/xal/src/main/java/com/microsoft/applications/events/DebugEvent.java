package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class DebugEvent {
    Object data;
    long param1;
    long param2;
    public long seq;
    long size;
    public long ts;
    public DebugEventType type;

    public DebugEvent(long j, long j2, long j3, long j4, long j5, Object obj, long j6) {
        this.seq = 0L;
        this.ts = 0L;
        this.type = DebugEventType.EVT_UNKNOWN;
        this.param1 = 0L;
        this.param2 = 0L;
        this.data = null;
        this.size = 0L;
        this.seq = j;
        this.ts = j2;
        if (DebugEventType.eventMap.containsKey(Long.valueOf(j3))) {
            this.type = DebugEventType.eventMap.get(Long.valueOf(j3));
        } else {
            this.type = DebugEventType.EVT_UNKNOWN;
        }
        this.param1 = j4;
        this.param2 = j5;
        this.data = obj;
        this.size = j6;
    }
}

package com.microsoft.applications.events;

import java.util.TreeMap;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum DebugEventType {
    EVT_LOG_EVENT(16777216),
    EVT_LOG_LIFECYCLE(16777217),
    EVT_LOG_FAILURE(16777218),
    EVT_LOG_PAGEVIEW(16777220),
    EVT_LOG_PAGEACTION(16777221),
    EVT_LOG_SAMPLEMETR(16777222),
    EVT_LOG_AGGRMETR(16777223),
    EVT_LOG_TRACE(16777224),
    EVT_LOG_USERSTATE(16777225),
    EVT_LOG_SESSION(16777226),
    EVT_ADDED(16781312),
    EVT_CACHED(33554432),
    EVT_DROPPED(50331648),
    EVT_FILTERED(50331649),
    EVT_SENT(67108864),
    EVT_SENDING(67108864),
    EVT_SEND_FAILED(67108865),
    EVT_SEND_RETRY(67108866),
    EVT_SEND_RETRY_DROPPED(67108867),
    EVT_SEND_SKIP_UTC_REGISTRATION(67108868),
    EVT_REJECTED(83886080),
    EVT_HTTP_STATE(150994944),
    EVT_CONN_FAILURE(167772160),
    EVT_HTTP_FAILURE(167772161),
    EVT_COMPRESS_FAILED(167772162),
    EVT_UNKNOWN_HOST(167772163),
    EVT_HTTP_ERROR(184549376),
    EVT_HTTP_OK(201326592),
    EVT_NET_CHANGED(218103808),
    EVT_STORAGE_FULL(234881024),
    EVT_STORAGE_FAILED(234881025),
    EVT_TICKET_EXPIRED(251658240),
    EVT_UNKNOWN(3735928559L);

    static final TreeMap<Long, DebugEventType> eventMap = new TreeMap<>();
    private long value;

    static {
        for (DebugEventType debugEventType : values()) {
            eventMap.put(Long.valueOf(debugEventType.value()), debugEventType);
        }
    }

    DebugEventType(long j) {
        this.value = j;
    }

    public long value() {
        return this.value;
    }
}

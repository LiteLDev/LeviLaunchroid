package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public final class EventPriority {
    private static final /* synthetic */ EventPriority[] $VALUES;
    public static final EventPriority High;
    public static final EventPriority Immediate;
    public static final EventPriority Low;
    public static final EventPriority MAX;
    public static final EventPriority MIN;
    public static final EventPriority Normal;
    public static final EventPriority Off;
    public static final EventPriority Unspecified;
    private final int m_value;

    public static EventPriority[] values() {
        return (EventPriority[]) $VALUES.clone();
    }

    static {
        EventPriority eventPriority = new EventPriority("Unspecified", 0, -1);
        Unspecified = eventPriority;
        EventPriority eventPriority2 = new EventPriority("Off", 1, 0);
        Off = eventPriority2;
        EventPriority eventPriority3 = new EventPriority("Low", 2, 1);
        Low = eventPriority3;
        EventPriority eventPriority4 = new EventPriority("MIN", 3, eventPriority3.getValue());
        MIN = eventPriority4;
        EventPriority eventPriority5 = new EventPriority("Normal", 4, 2);
        Normal = eventPriority5;
        EventPriority eventPriority6 = new EventPriority("High", 5, 3);
        High = eventPriority6;
        EventPriority eventPriority7 = new EventPriority("Immediate", 6, 4);
        Immediate = eventPriority7;
        EventPriority eventPriority8 = new EventPriority("MAX", 7, eventPriority7.getValue());
        MAX = eventPriority8;
        $VALUES = new EventPriority[]{eventPriority, eventPriority2, eventPriority3, eventPriority4, eventPriority5, eventPriority6, eventPriority7, eventPriority8};
    }

    private EventPriority(String str, int i, int i2) {
        this.m_value = i2;
    }

    int getValue() {
        return this.m_value;
    }

    static EventPriority getEnum(int i) {
        if (i == -1) {
            return Unspecified;
        }
        if (i == 0) {
            return Off;
        }
        if (i == 1) {
            return Low;
        }
        if (i == 2) {
            return Normal;
        }
        if (i == 3) {
            return High;
        }
        if (i == 4) {
            return Immediate;
        }
        throw new IllegalArgumentException("Unsupported value: " + i);
    }
}

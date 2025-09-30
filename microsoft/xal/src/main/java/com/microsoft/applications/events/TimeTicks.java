package com.microsoft.applications.events;

import java.util.Date;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

class TimeTicks {
    private final long ticks;

    public long getTicks() {
        return ticks;
    }

    public TimeTicks(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        this.ticks = (date.getTime() * 10_000) + 621_355_968_000_000_000L;
    }
}
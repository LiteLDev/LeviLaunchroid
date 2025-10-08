package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum Status {
    EFAIL(-1),
    SUCCESS(0),
    EPERM(1),
    EALREADY(103),
    ENOSYS(40),
    ENOTSUP(129);

    private final int m_value;

    Status(int i) {
        this.m_value = i;
    }

    static Status getEnum(int i) {
        if (i == -1) {
            return EFAIL;
        }
        if (i == 0) {
            return SUCCESS;
        }
        if (i == 1) {
            return EPERM;
        }
        if (i == 40) {
            return ENOSYS;
        }
        if (i == 103) {
            return EALREADY;
        }
        if (i == 129) {
            return ENOTSUP;
        }
        throw new IllegalArgumentException("Unsupported value: " + i);
    }

    class StatusValues {
        static final int VALUE_EALREADY = 103;
        static final int VALUE_EFAIL = -1;
        static final int VALUE_ENOSYS = 40;
        static final int VALUE_ENOTSUP = 129;
        static final int VALUE_EPERM = 1;
        static final int VALUE_SUCCESS = 0;

        StatusValues() {
        }
    }
}

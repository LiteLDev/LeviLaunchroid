package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum TicketType {
    MSA_Device(0),
    MSA_User(1),
    XAuth_Device(2),
    XAuth_User(3),
    AAD(4),
    AAD_User(5),
    AAD_JWT(6);

    private final int m_value;

    TicketType(int i) {
        this.m_value = i;
    }

    int getValue() {
        return this.m_value;
    }
}
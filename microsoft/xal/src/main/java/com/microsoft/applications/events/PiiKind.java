package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum PiiKind {
    None(0),
    DistinguishedName(1),
    GenericData(2),
    IPv4Address(3),
    IPv6Address(4),
    MailSubject(5),
    PhoneNumber(6),
    QueryString(7),
    SipAddress(8),
    SmtpAddress(9),
    Identity(10),
    Uri(11),
    Fqdn(12),
    IPv4AddressLegacy(13),
    CustomerContentKind_GenericData(32);

    private final int m_value;

    PiiKind(int i) {
        this.m_value = i;
    }

    public int getValue() {
        return this.m_value;
    }
}

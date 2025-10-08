package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum RawActionType {
    Unspecified(0),
    Unknown(1),
    Other(2),
    LButtonDoubleClick(11),
    LButtonDown(12),
    LButtonUp(13),
    MButtonDoubleClick(14),
    MButtonDown(15),
    MButtonUp(16),
    MouseHover(17),
    MouseWheel(18),
    MouseMove(20),
    RButtonDoubleClick(22),
    RButtonDown(23),
    RButtonUp(24),
    TouchTap(50),
    TouchDoubleTap(51),
    TouchLongPress(52),
    TouchScroll(53),
    TouchPan(54),
    TouchFlick(55),
    TouchPinch(56),
    TouchZoom(57),
    TouchRotate(58),
    KeyboardPress(100),
    KeyboardEnter(101);

    private final int m_value;

    RawActionType(int i) {
        this.m_value = i;
    }

    int getValue() {
        return this.m_value;
    }
}

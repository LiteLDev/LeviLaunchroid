package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum InputDeviceType {
    Unspecified(0),
    Unknown(1),
    Other(2),
    Mouse(3),
    Keyboard(4),
    Touch(5),
    Stylus(6),
    Microphone(7),
    Kinect(8),
    Camera(9);

    private final int m_value;

    InputDeviceType(int i) {
        this.m_value = i;
    }

    int getValue() {
        return this.m_value;
    }
}

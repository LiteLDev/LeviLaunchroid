package org.levimc.pojavcontrols;

import androidx.annotation.Keep;

@Keep
public class ControlJoystickData extends ControlData {
    public boolean forwardLock;
    public boolean absolute;

    public ControlJoystickData() {
        super("Joystick", new int[]{}, "${margin}", "${bottom} - ${height}", 120, 120);
        isHideable = true;
    }

    public ControlJoystickData(ControlJoystickData source) {
        super(source);
        forwardLock = source.forwardLock;
        absolute = source.absolute;
        isHideable = true;
    }
}

package org.levimc.pojavcontrols;

import androidx.annotation.Keep;

import java.util.ArrayList;

@Keep
public class ControlDrawerData {
    public enum Orientation { DOWN, LEFT, UP, RIGHT, FREE }

    public ArrayList<ControlData> buttonProperties;
    public ControlData properties;
    public Orientation orientation;

    public ControlDrawerData() {
        buttonProperties = new ArrayList<>();
        properties = new ControlData("Drawer", new int[]{}, "0.5 * ${screen_width}",
                "0.5 * ${screen_height}", 50, 50);
        orientation = Orientation.LEFT;
    }

    public ControlDrawerData(ControlDrawerData source) {
        buttonProperties = new ArrayList<>();
        if (source.buttonProperties != null) {
            for (ControlData button : source.buttonProperties) buttonProperties.add(new ControlData(button));
        }
        properties = source.properties == null ? new ControlData() : new ControlData(source.properties);
        orientation = source.orientation == null ? Orientation.LEFT : source.orientation;
    }

    public void normalize() {
        if (buttonProperties == null) buttonProperties = new ArrayList<>();
        if (properties == null) properties = new ControlData();
        if (orientation == null) orientation = Orientation.LEFT;
        properties.normalize();
        for (ControlData button : buttonProperties) button.normalize();
    }
}

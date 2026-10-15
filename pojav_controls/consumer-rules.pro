-keep class org.levimc.pojavcontrols.CustomControls {
    !static !transient <fields>;
    <init>();
}

-keep class org.levimc.pojavcontrols.ControlData {
    !static !transient <fields>;
    <init>();
}

-keep class org.levimc.pojavcontrols.ControlDrawerData {
    !static !transient <fields>;
    <init>();
}

-keep class org.levimc.pojavcontrols.ControlJoystickData {
    !static !transient <fields>;
    <init>();
}

-keep enum org.levimc.pojavcontrols.ControlDrawerData$Orientation { *; }

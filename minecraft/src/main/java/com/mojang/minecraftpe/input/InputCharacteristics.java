package com.mojang.minecraftpe.input;

import android.view.InputDevice;
import androidx.core.view.InputDeviceCompat;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class InputCharacteristics {
    private static final int DUALSENSE_DEVICE_ID = 3302;
    private static final int SONY_VENDOR_ID = 1356;
    private static final int XBOX_VENDOR_ID = 1118;
    private static final int XBOX_PRODUCT_ID = 736;
    private static final int SOURCE_GAMEPAD_MASK = InputDeviceCompat.SOURCE_GAMEPAD;

    private static final String[] XBOX_KEYLAYOUT_PATHS = {
            "/system/usr/keylayout/Vendor_045e_Product_02e0.kl",
            "/data/system/devices/keylayout/Vendor_045e_Product_02e0.kl"
    };

    public static boolean allControllersHaveDoubleTriggers() {
        for (int deviceId : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(deviceId);
            if (isValidGameController(device)) {
                boolean supportsAnalogTriggers = checkAnalogTriggersSupport(device);
                if (!supportsAnalogTriggers) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isCreteController(int deviceId) {
        InputDevice device = InputDevice.getDevice(deviceId);
        if (!isValidGameController(device)) {
            return false;
        }

        if (device.getProductId() == XBOX_PRODUCT_ID && device.getVendorId() == XBOX_VENDOR_ID) {
            return !hasXboxKeylayoutFiles();
        }
        return false;
    }

    public static boolean supportsAnalogTriggers(int deviceId) {
        InputDevice device = InputDevice.getDevice(deviceId);
        return device != null && supportsAnalogTriggers(device);
    }

    private static boolean supportsAnalogTriggers(@NotNull InputDevice inputDevice) {
        boolean hasLeftTrigger = inputDevice.getMotionRange(17) != null || inputDevice.getMotionRange(23) != null;
        boolean hasRightTrigger = inputDevice.getMotionRange(18) != null || inputDevice.getMotionRange(22) != null;
        return hasLeftTrigger && hasRightTrigger;
    }

    public static boolean isXboxController(int deviceId) {
        InputDevice device = InputDevice.getDevice(deviceId);
        return device != null &&
                (device.getSources() & SOURCE_GAMEPAD_MASK) == 1025 &&
                device.getVendorId() == XBOX_VENDOR_ID;
    }

    public static boolean isPlaystationController(int deviceId) {
        InputDevice device = InputDevice.getDevice(deviceId);
        return device != null &&
                (device.getSources() & SOURCE_GAMEPAD_MASK) == 1025 &&
                device.getVendorId() == SONY_VENDOR_ID;
    }

    public static boolean isDualsenseController(int deviceId) {
        InputDevice device = InputDevice.getDevice(deviceId);
        return device != null &&
                (device.getSources() & SOURCE_GAMEPAD_MASK) == 1025 &&
                device.getVendorId() == SONY_VENDOR_ID &&
                device.getProductId() == DUALSENSE_DEVICE_ID;
    }

    private static boolean isValidGameController(InputDevice device) {
        return device != null &&
                !device.isVirtual() &&
                device.getControllerNumber() > 0 &&
                (device.getSources() & SOURCE_GAMEPAD_MASK) != 0;
    }

    private static boolean checkAnalogTriggersSupport(@NotNull InputDevice device) {
        boolean[] hasKeys = device.hasKeys(102, 103, 104, 105);
        boolean supportsAnalogTriggers = hasKeys.length == 4;

        for (boolean hasKey : hasKeys) {
            if (!hasKey) {
                supportsAnalogTriggers = false;
                break;
            }
        }

        if (!supportsAnalogTriggers && hasKeys[0] && hasKeys[1]) {
            supportsAnalogTriggers = supportsAnalogTriggers(device);
        }

        return supportsAnalogTriggers && !device.getName().contains("EI-GP20");
    }

    private static boolean hasXboxKeylayoutFiles() {
        for (String path : XBOX_KEYLAYOUT_PATHS) {
            if (new File(path).exists()) {
                return true;
            }
        }
        return false;
    }
}
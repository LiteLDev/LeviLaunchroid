package com.mojang.minecraftpe.input;

import android.content.Context;
import android.hardware.input.InputManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class JellyBeanDeviceManager extends InputDeviceManager implements InputManager.InputDeviceListener {
    private static final String INPUT_SERVICE = "input";

    private final InputManager inputManager;

    native void onInputDeviceAddedNative(int deviceId);
    native void onInputDeviceChangedNative(int deviceId);
    native void onInputDeviceRemovedNative(int deviceId);
    native void setControllerDetailsNative(int deviceId, boolean isCreteController, boolean supportsAnalogTriggers);
    native void setDoubleTriggersSupportedNative(boolean supported);
    native void setFoundDualsenseControllerNative(boolean found);
    native void setFoundPlaystationControllerNative(boolean found);
    native void setFoundXboxControllerNative(boolean found);

    public JellyBeanDeviceManager(@NotNull Context context) {
        this.inputManager = (InputManager) context.getSystemService(INPUT_SERVICE);
    }

    @Override
    public void register() {
        int[] deviceIds = this.inputManager.getInputDeviceIds();
        this.inputManager.registerInputDeviceListener(this, null);

        updateDoubleTriggersSupport();
        updateAllControllerDetails(deviceIds);
        checkForXboxAndPlaystationController();
    }

    @Override
    public void unregister() {
        this.inputManager.unregisterInputDeviceListener(this);
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        onInputDeviceAddedNative(deviceId);
        updateDoubleTriggersSupport();
        updateControllerDetails(deviceId);
        updateControllerTypeFlags(deviceId);
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        onInputDeviceChangedNative(deviceId);
        updateDoubleTriggersSupport();
        updateControllerDetails(deviceId);
        checkForXboxAndPlaystationController();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        onInputDeviceRemovedNative(deviceId);
        updateDoubleTriggersSupport();
        updateControllerDetails(deviceId);
        checkForXboxAndPlaystationController();
    }

    public void checkForXboxAndPlaystationController() {
        boolean foundXbox = false;
        boolean foundPlaystation = false;
        boolean foundDualsense = false;

        for (int deviceId : this.inputManager.getInputDeviceIds()) {
            foundXbox = foundXbox || InputCharacteristics.isXboxController(deviceId);
            foundPlaystation = foundPlaystation || InputCharacteristics.isPlaystationController(deviceId);
            foundDualsense = foundDualsense || InputCharacteristics.isDualsenseController(deviceId);

            if (foundXbox && foundPlaystation) {
                break;
            }
        }

        setFoundXboxControllerNative(foundXbox);
        setFoundPlaystationControllerNative(foundPlaystation);
        setFoundDualsenseControllerNative(foundDualsense);
    }

    private void updateDoubleTriggersSupport() {
        setDoubleTriggersSupportedNative(InputCharacteristics.allControllersHaveDoubleTriggers());
    }

    private void updateControllerDetails(int deviceId) {
        boolean isCrete = InputCharacteristics.isCreteController(deviceId);
        boolean supportsAnalog = InputCharacteristics.supportsAnalogTriggers(deviceId);
        setControllerDetailsNative(deviceId, isCrete, supportsAnalog);
    }

    private void updateAllControllerDetails(int @NotNull [] deviceIds) {
        for (int deviceId : deviceIds) {
            updateControllerDetails(deviceId);
        }
    }

    private void updateControllerTypeFlags(int deviceId) {
        if (InputCharacteristics.isXboxController(deviceId)) {
            setFoundXboxControllerNative(true);
        } else if (InputCharacteristics.isPlaystationController(deviceId)) {
            setFoundPlaystationControllerNative(true);
            if (InputCharacteristics.isDualsenseController(deviceId)) {
                setFoundDualsenseControllerNative(true);
            }
        }
    }
}
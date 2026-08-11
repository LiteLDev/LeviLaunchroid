package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.view.MotionEvent;

import org.levimc.launcher.core.mods.inbuilt.ExternalModBridge;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;
import org.levimc.launcher.core.mods.inbuilt.nativemod.PojavControlsMod;
import org.levimc.pojavcontrols.PojavControls;
import org.levimc.pojavcontrols.PojavControlsHost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InbuiltOverlayManager {
    public interface HudEditorSelectionListener {
        void onHudEditorSelectionChanged(int currentSizeDp);
    }

    private static volatile InbuiltOverlayManager instance;
    private final Activity activity;
    private final List<BaseOverlayButton> overlays = new ArrayList<>();
    private final Map<String, Boolean> modActiveStates = new HashMap<>();
    private final Map<String, BaseOverlayButton> modOverlayMap = new HashMap<>();
    private final Map<String, ExternalButtonOverlay> externalButtonOverlayMap = new HashMap<>();
    private final Map<String, Integer> modPositionMap = new HashMap<>();
    private ChickPetOverlay chickPetOverlay;
    private ZoomOverlay zoomOverlay;
    private SnaplookOverlay snaplookOverlay;
    private GyroOverlay gyroOverlay;
    private FpsDisplayOverlay fpsDisplayOverlay;
    private CpsDisplayOverlay cpsDisplayOverlay;
    private ModMenuButton modMenuButton;
    private HudOverlay hudOverlay;
    private BaseOverlayButton selectedHudEditorOverlay;
    private String selectedDisplayModId;
    private HudEditorSelectionListener hudEditorSelectionListener;
    private boolean hudEditorMode = false;
    private int baseY = 150;
    private static final int SPACING = 70;
    private static final int START_X = 50;
    private long lastVisibilityStateHash = Long.MIN_VALUE;

    public InbuiltOverlayManager(Activity activity) {
        this.activity = activity;
        instance = this;
    }

    public static InbuiltOverlayManager getInstance() {
        return instance;
    }

    public void showEnabledOverlays() {
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        if (!manager.isModMenuEnabled()) return;

        if (hudOverlay == null) {
            hudOverlay = new HudOverlay(activity);
        }
        hudOverlay.show();

        int nextY = baseY;

        modActiveStates.put(ModIds.QUICK_DROP, false);
        modActiveStates.put(ModIds.CAMERA_PERSPECTIVE, false);
        modActiveStates.put(ModIds.TOGGLE_HUD, false);
        modActiveStates.put(ModIds.AUTO_SPRINT, false);
        modActiveStates.put(ModIds.CHICK_PET, false);
        modActiveStates.put(ModIds.ZOOM, false);
        modActiveStates.put(ModIds.FPS_DISPLAY, false);
        modActiveStates.put(ModIds.CPS_DISPLAY, false);
        modActiveStates.put(ModIds.SNAPLOOK, false);
        modActiveStates.put(ModIds.VIRTUAL_CURSOR, false);
        modActiveStates.put(ModIds.GYRO, false);
        modActiveStates.put(ModIds.POJAV_CONTROLS, false);

        modPositionMap.put(ModIds.QUICK_DROP, nextY + SPACING);
        modPositionMap.put(ModIds.CAMERA_PERSPECTIVE, nextY + SPACING * 2);
        modPositionMap.put(ModIds.TOGGLE_HUD, nextY + SPACING * 3);
        modPositionMap.put(ModIds.AUTO_SPRINT, nextY + SPACING * 4);
        modPositionMap.put(ModIds.ZOOM, nextY + SPACING * 5);
        modPositionMap.put(ModIds.FPS_DISPLAY, nextY + SPACING * 6);
        modPositionMap.put(ModIds.CPS_DISPLAY, nextY + SPACING * 7);
        modPositionMap.put(ModIds.SNAPLOOK, nextY + SPACING * 8);
        modPositionMap.put(ModIds.VIRTUAL_CURSOR, nextY + SPACING * 9);
        modPositionMap.put(ModIds.GYRO, nextY + SPACING * 10);

        if (zoomOverlay == null) {
            zoomOverlay = new ZoomOverlay(activity);
            zoomOverlay.initializeForKeyboard();
        }

        if (snaplookOverlay == null) {
            snaplookOverlay = new SnaplookOverlay(activity);
            snaplookOverlay.initializeForKeyboard();
        }

        restorePersistedInbuiltModState(manager, ModIds.QUICK_DROP);
        restorePersistedInbuiltModState(manager, ModIds.CAMERA_PERSPECTIVE);
        restorePersistedInbuiltModState(manager, ModIds.TOGGLE_HUD);
        restorePersistedInbuiltModState(manager, ModIds.AUTO_SPRINT);
        restorePersistedInbuiltModState(manager, ModIds.CHICK_PET);
        restorePersistedInbuiltModState(manager, ModIds.ZOOM);
        restorePersistedInbuiltModState(manager, ModIds.FPS_DISPLAY);
        restorePersistedInbuiltModState(manager, ModIds.CPS_DISPLAY);
        restorePersistedInbuiltModState(manager, ModIds.SNAPLOOK);
        restorePersistedInbuiltModState(manager, ModIds.VIRTUAL_CURSOR);
        restorePersistedInbuiltModState(manager, ModIds.GYRO);
        restorePersistedInbuiltModState(manager, ModIds.POJAV_CONTROLS);

        modMenuButton = new ModMenuButton(activity);
        modMenuButton.show(START_X, nextY);
        refreshExternalButtons();
    }

    private void restorePersistedInbuiltModState(InbuiltModManager manager, String modId) {
        if (manager.resolveInbuiltModEnabled(modId, false)) {
            handleModToggle(modId, true);
        }
    }

    public void handleModToggle(String modId, boolean enabled) {
        boolean wasEnabled = modActiveStates.getOrDefault(modId, false);
        modActiveStates.put(modId, enabled);
        
        if (enabled && !wasEnabled) {
            showModOverlay(modId);
        } else if (!enabled && wasEnabled) {
            hideModOverlay(modId);
        }
    }

    private void showModOverlay(String modId) {
        if (modOverlayMap.containsKey(modId)) {
            return;
        }

        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        
        android.util.DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int centerX = metrics.widthPixels / 2 - (int)(26 * metrics.density);
        int centerY = metrics.heightPixels / 2 - (int)(26 * metrics.density);

        int savedX = manager.getOverlayPositionX(modId, centerX);
        int savedY = manager.getOverlayPositionY(modId, centerY);

        switch (modId) {
            case ModIds.QUICK_DROP:
                QuickDropOverlay quickDrop = new QuickDropOverlay(activity);
                quickDrop.show(savedX, savedY);
                overlays.add(quickDrop);
                modOverlayMap.put(modId, quickDrop);
                break;
            case ModIds.CAMERA_PERSPECTIVE:
                CameraPerspectiveOverlay camera = new CameraPerspectiveOverlay(activity);
                camera.show(savedX, savedY);
                overlays.add(camera);
                modOverlayMap.put(modId, camera);
                break;
            case ModIds.TOGGLE_HUD:
                ToggleHudOverlay hud = new ToggleHudOverlay(activity);
                hud.show(savedX, savedY);
                overlays.add(hud);
                modOverlayMap.put(modId, hud);
                break;
            case ModIds.AUTO_SPRINT:
                AutoSprintOverlay sprint = new AutoSprintOverlay(activity, manager.getAutoSprintKeybind());
                sprint.show(savedX, savedY);
                overlays.add(sprint);
                modOverlayMap.put(modId, sprint);
                break;
            case ModIds.CHICK_PET:
                if (chickPetOverlay == null) {
                    chickPetOverlay = new ChickPetOverlay(activity);
                    chickPetOverlay.show();
                }
                break;
            case ModIds.ZOOM:
                if (zoomOverlay == null) {
                    zoomOverlay = new ZoomOverlay(activity);
                }
                zoomOverlay.show(savedX, savedY);
                overlays.add(zoomOverlay);
                modOverlayMap.put(modId, zoomOverlay);
                break;
            case ModIds.FPS_DISPLAY:
                if (fpsDisplayOverlay == null) {
                    fpsDisplayOverlay = new FpsDisplayOverlay(activity);
                    fpsDisplayOverlay.show(savedX, savedY);
                }
                break;
            case ModIds.CPS_DISPLAY:
                if (cpsDisplayOverlay == null) {
                    cpsDisplayOverlay = new CpsDisplayOverlay(activity);
                    cpsDisplayOverlay.show(savedX, savedY);
                }
                break;
            case ModIds.SNAPLOOK:
                if (snaplookOverlay == null) {
                    snaplookOverlay = new SnaplookOverlay(activity);
                }
                snaplookOverlay.show(savedX, savedY);
                overlays.add(snaplookOverlay);
                modOverlayMap.put(modId, snaplookOverlay);
                break;
            case ModIds.VIRTUAL_CURSOR:
                VirtualCursorOverlay cursorOverlay = new VirtualCursorOverlay(activity);
                cursorOverlay.show(savedX, savedY);
                overlays.add(cursorOverlay);
                modOverlayMap.put(modId, cursorOverlay);
                break;
            case ModIds.GYRO:
                if (gyroOverlay == null) {
                    gyroOverlay = new GyroOverlay(activity);
                }
                gyroOverlay.show(savedX, savedY);
                overlays.add(gyroOverlay);
                modOverlayMap.put(modId, gyroOverlay);
                break;
            case ModIds.POJAV_CONTROLS:
                if (activity instanceof PojavControlsHost && PojavControlsMod.setEnabled(true)) {
                    PojavControls.setEnabled(activity, (PojavControlsHost) activity, true);
                }
                break;
        }
    }

    private void hideModOverlay(String modId) {
        if (modId.equals(ModIds.POJAV_CONTROLS)) {
            PojavControls.setEnabled(activity,
                    activity instanceof PojavControlsHost ? (PojavControlsHost) activity : null,
                    false);
            PojavControlsMod.setEnabled(false);
            return;
        }
        if (modId.equals(ModIds.CHICK_PET)) {
            if (chickPetOverlay != null) {
                chickPetOverlay.hide();
                chickPetOverlay = null;
            }
            return;
        }
        
        if (modId.equals(ModIds.ZOOM)) {
            if (zoomOverlay != null) {
                if (zoomOverlay == selectedHudEditorOverlay) {
                    selectHudEditorOverlay(null);
                }
                zoomOverlay.hide();
                overlays.remove(zoomOverlay);
                modOverlayMap.remove(modId);
            }
            return;
        }

        if (modId.equals(ModIds.FPS_DISPLAY)) {
            if (fpsDisplayOverlay != null) {
                fpsDisplayOverlay.hide();
                fpsDisplayOverlay = null;
            }
            return;
        }

        if (modId.equals(ModIds.CPS_DISPLAY)) {
            if (cpsDisplayOverlay != null) {
                cpsDisplayOverlay.hide();
                cpsDisplayOverlay = null;
            }
            return;
        }

        if (modId.equals(ModIds.SNAPLOOK)) {
            if (snaplookOverlay != null) {
                if (snaplookOverlay == selectedHudEditorOverlay) {
                    selectHudEditorOverlay(null);
                }
                snaplookOverlay.hide();
                overlays.remove(snaplookOverlay);
                modOverlayMap.remove(modId);
            }
            return;
        }

        if (modId.equals(ModIds.GYRO)) {
            if (gyroOverlay != null) {
                if (gyroOverlay == selectedHudEditorOverlay) {
                    selectHudEditorOverlay(null);
                }
                gyroOverlay.hide();
                overlays.remove(gyroOverlay);
                modOverlayMap.remove(modId);
            }
            return;
        }
        
        BaseOverlayButton overlay = modOverlayMap.get(modId);
        if (overlay != null) {
            if (overlay == selectedHudEditorOverlay) {
                selectHudEditorOverlay(null);
            }
            overlay.hide();
            overlays.remove(overlay);
            modOverlayMap.remove(modId);
        }
    }

    public void handleExternalModuleToggle(String moduleId, boolean enabled) {
        if (enabled) {
            showExternalButtonsForModule(moduleId);
        } else {
            hideExternalButtonsForModule(moduleId);
        }
    }

    private void refreshExternalButtons() {
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        java.util.Set<String> enabledModules = new java.util.HashSet<>();
        int extCount = ExternalModBridge.getExternalModCount();
        for (int i = 0; i < extCount; i++) {
            try {
                org.json.JSONObject obj = new org.json.JSONObject(ExternalModBridge.getExternalModInfo(i));
                String moduleId = obj.optString("module_id", "");
                if (moduleId.isEmpty()) continue;

                boolean nativeEnabled = obj.optBoolean("enabled", false);
                boolean enabled = manager.resolveExternalModuleEnabled(moduleId, nativeEnabled);
                if (enabled != nativeEnabled) {
                    ExternalModBridge.toggleExternalMod(moduleId, enabled);
                }
                if (enabled) {
                    enabledModules.add(moduleId);
                }
            } catch (Exception ignored) {}
        }

        for (String moduleId : enabledModules) {
            showExternalButtonsForModule(moduleId);
        }
        java.util.List<ExternalButtonOverlay> stale = new java.util.ArrayList<>();
        for (ExternalButtonOverlay overlay : externalButtonOverlayMap.values()) {
            if (!enabledModules.contains(overlay.getModuleId())) {
                stale.add(overlay);
            }
        }
        for (ExternalButtonOverlay overlay : stale) {
            if (overlay == selectedHudEditorOverlay) {
                selectHudEditorOverlay(null);
            }
            overlay.hide();
            overlays.remove(overlay);
            externalButtonOverlayMap.remove(overlay.getButtonId());
            modOverlayMap.remove(overlay.getModId());
        }
    }

    private void showExternalButtonsForModule(String moduleId) {
        if (moduleId == null || moduleId.isEmpty()) return;

        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        android.util.DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int centerX = metrics.widthPixels / 2 - (int)(26 * metrics.density);
        int centerY = metrics.heightPixels / 2 - (int)(26 * metrics.density);

        int buttonCount = ExternalModBridge.getExternalButtonCount();
        for (int i = 0; i < buttonCount; i++) {
            ExternalModBridge.ExternalButton button = ExternalModBridge.getExternalButton(i);
            if (button == null || !moduleId.equals(button.moduleId)) continue;
            if (!button.defaultVisible || !button.moduleEnabled) continue;
            if (externalButtonOverlayMap.containsKey(button.buttonId)) continue;

            int savedX = manager.getOverlayPositionX(button.positionKey(), centerX);
            int savedY = manager.getOverlayPositionY(button.positionKey(), centerY);
            ExternalButtonOverlay overlay = new ExternalButtonOverlay(activity, button);
            overlay.show(savedX, savedY);
            overlays.add(overlay);
            externalButtonOverlayMap.put(button.buttonId, overlay);
            modOverlayMap.put(button.positionKey(), overlay);
        }
    }

    private void hideExternalButtonsForModule(String moduleId) {
        java.util.List<ExternalButtonOverlay> toHide = new java.util.ArrayList<>();
        for (ExternalButtonOverlay overlay : externalButtonOverlayMap.values()) {
            if (moduleId.equals(overlay.getModuleId())) {
                toHide.add(overlay);
            }
        }
        for (ExternalButtonOverlay overlay : toHide) {
            if (overlay == selectedHudEditorOverlay) {
                selectHudEditorOverlay(null);
            }
            overlay.hide();
            overlays.remove(overlay);
            externalButtonOverlayMap.remove(overlay.getButtonId());
            modOverlayMap.remove(overlay.getModId());
        }
    }

    public boolean isModActive(String modId) {
        return modActiveStates.getOrDefault(modId, false);
    }


    public void hideAllOverlays() {
        PojavControls.setEnabled(activity,
                activity instanceof PojavControlsHost ? (PojavControlsHost) activity : null,
                false);
        PojavControlsMod.setEnabled(false);
        selectHudEditorOverlay(null);
        for (BaseOverlayButton overlay : overlays) {
            overlay.hide();
        }
        overlays.clear();
        modOverlayMap.clear();
        externalButtonOverlayMap.clear();
        modActiveStates.clear();
        modPositionMap.clear();
        if (chickPetOverlay != null) {
            chickPetOverlay.hide();
            chickPetOverlay = null;
        }
        if (zoomOverlay != null) {
            zoomOverlay.hide();
            zoomOverlay = null;
        }
        if (fpsDisplayOverlay != null) {
            fpsDisplayOverlay.hide();
            fpsDisplayOverlay = null;
        }
        if (cpsDisplayOverlay != null) {
            cpsDisplayOverlay.hide();
            cpsDisplayOverlay = null;
        }
        if (snaplookOverlay != null) {
            snaplookOverlay.hide();
            snaplookOverlay = null;
        }
        if (gyroOverlay != null) {
            gyroOverlay.hide();
            gyroOverlay = null;
        }
        if (modMenuButton != null) {
            modMenuButton.hide();
            modMenuButton = null;
        }
        if (hudOverlay != null) {
            hudOverlay.hide();
            hudOverlay = null;
        }
        instance = null;
    }

    public boolean handleKeyEvent(int keyCode, int action) {
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        
        boolean zoomEnabled = modActiveStates.getOrDefault(ModIds.ZOOM, false);
        
        int zoomKeybind = manager.getZoomKeybind();
        if (zoomEnabled && keyCode == zoomKeybind) {
            if (zoomOverlay != null) {
                if (action == android.view.KeyEvent.ACTION_DOWN) {
                    zoomOverlay.onKeyDown();
                    return true;
                } else if (action == android.view.KeyEvent.ACTION_UP) {
                    zoomOverlay.onKeyUp();
                    return true;
                }
            }
        }

        boolean snaplookEnabled = modActiveStates.getOrDefault(ModIds.SNAPLOOK, false);

        if (snaplookEnabled && keyCode == android.view.KeyEvent.KEYCODE_X) {
            if (snaplookOverlay != null) {
                if (action == android.view.KeyEvent.ACTION_DOWN) {
                    snaplookOverlay.onKeyDown();
                    return true;
                } else if (action == android.view.KeyEvent.ACTION_UP) {
                    snaplookOverlay.onKeyUp();
                    return true;
                }
            }
        }

        return false;
    }

    public boolean handleScrollEvent(float scrollDelta) {
        for (ExternalButtonOverlay overlay : externalButtonOverlayMap.values()) {
            if (overlay.onScroll(scrollDelta)) {
                return true;
            }
        }
        if (zoomOverlay != null && zoomOverlay.isZooming()) {
            zoomOverlay.onScroll(scrollDelta);
            return true;
        }
        return false;
    }

    public boolean handleTouchEvent(MotionEvent event) {
        if (cpsDisplayOverlay != null) {
            return cpsDisplayOverlay.handleTouchEvent(event);
        }
        return false;
    }

    public boolean handleMouseEvent(MotionEvent event) {
        if (cpsDisplayOverlay != null) {
            return cpsDisplayOverlay.handleMouseEvent(event);
        }
        return false;
    }

    public void applyConfigurationChanges(String modId) {
        BaseOverlayButton overlay = modOverlayMap.get(modId);
        if (overlay != null) {
            overlay.applyConfigurationChanges();
        }

        if (modId.equals(ModIds.ZOOM) && zoomOverlay != null) {
            zoomOverlay.applyConfigurationChanges();
        }
        if (modId.equals(ModIds.SNAPLOOK) && snaplookOverlay != null) {
            snaplookOverlay.applyConfigurationChanges();
        }
        if (modId.equals(ModIds.GYRO) && gyroOverlay != null) {
            gyroOverlay.applyConfigurationChanges();
        }
        if (modId.equals(ModIds.FPS_DISPLAY) && fpsDisplayOverlay != null) {
            fpsDisplayOverlay.applyConfigurationChanges();
        }
        if (modId.equals(ModIds.CPS_DISPLAY) && cpsDisplayOverlay != null) {
            cpsDisplayOverlay.applyConfigurationChanges();
        }
    }

    public void setHudEditorMode(boolean active) {
        hudEditorMode = active;
        for (BaseOverlayButton overlay : overlays) {
            overlay.setHudEditorMode(active);
        }
        if (fpsDisplayOverlay != null) {
            fpsDisplayOverlay.setHudEditorMode(active);
        }
        if (cpsDisplayOverlay != null) {
            cpsDisplayOverlay.setHudEditorMode(active);
        }
        if (hudOverlay != null) {
            hudOverlay.setHudEditorMode(active);
        }

        if (modMenuButton != null) {
            if (active) {
                modMenuButton.setVisibility(android.view.View.GONE);
            } else {
                modMenuButton.setVisibility(android.view.View.VISIBLE);
                int savedX = InbuiltModManager.getInstance(activity).getOverlayPositionX(ModIds.MOD_MENU, START_X);
                int savedY = InbuiltModManager.getInstance(activity).getOverlayPositionY(ModIds.MOD_MENU, baseY);
                modMenuButton.show(savedX, savedY);
            }
        }

        if (active) {
            selectFirstHudEditorOverlay();
        } else {
            selectHudEditorOverlay(null);
        }
    }

    public void setHudEditorSelectionListener(HudEditorSelectionListener listener) {
        hudEditorSelectionListener = listener;
        if (listener != null) {
            listener.onHudEditorSelectionChanged(getSelectedHudEditorButtonSize());
        }
    }

    public void selectHudEditorOverlay(BaseOverlayButton overlay) {
        if (!hudEditorMode && overlay != null) {
            return;
        }
        selectedDisplayModId = null;
        if (selectedHudEditorOverlay == overlay) {
            notifySelectionListener();
            return;
        }
        if (selectedHudEditorOverlay != null) {
            selectedHudEditorOverlay.setHudEditorSelected(false);
        }
        selectedHudEditorOverlay = overlay;
        if (selectedHudEditorOverlay != null) {
            selectedHudEditorOverlay.setHudEditorSelected(true);
        }
        notifySelectionListener();
    }

    public void selectHudEditorDisplay(String modId) {
        if (!hudEditorMode || modId == null) return;
        if (selectedHudEditorOverlay != null) {
            selectedHudEditorOverlay.setHudEditorSelected(false);
            selectedHudEditorOverlay = null;
        }
        selectedDisplayModId = modId;
        notifySelectionListener();
    }

    private void notifySelectionListener() {
        if (hudEditorSelectionListener != null) {
            hudEditorSelectionListener.onHudEditorSelectionChanged(getSelectedHudEditorButtonSize());
        }
    }

    public int getSelectedHudEditorButtonSize() {
        if (selectedHudEditorOverlay != null) {
            return selectedHudEditorOverlay.getCurrentButtonSizeDp();
        }
        if (selectedDisplayModId != null) {
            return InbuiltModManager.getInstance(activity).getOverlayButtonSize(selectedDisplayModId);
        }
        return 0;
    }

    public void setSelectedHudEditorButtonSize(int sizeDp) {
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        if (selectedHudEditorOverlay != null) {
            manager.setOverlayButtonSize(selectedHudEditorOverlay.getOverlayConfigKey(), sizeDp);
            selectedHudEditorOverlay.applyConfigurationChanges();
        } else if (selectedDisplayModId != null) {
            manager.setOverlayButtonSize(selectedDisplayModId, sizeDp);
            if (selectedDisplayModId.equals(ModIds.FPS_DISPLAY) && fpsDisplayOverlay != null) {
                fpsDisplayOverlay.applyConfigurationChanges();
            } else if (selectedDisplayModId.equals(ModIds.CPS_DISPLAY) && cpsDisplayOverlay != null) {
                cpsDisplayOverlay.applyConfigurationChanges();
            }
        }
    }

    private void selectFirstHudEditorOverlay() {
        if (selectedHudEditorOverlay != null) {
            selectedHudEditorOverlay.setHudEditorSelected(true);
            notifySelectionListener();
            return;
        }
        if (selectedDisplayModId != null) {
            notifySelectionListener();
            return;
        }
        if (!overlays.isEmpty()) {
            selectHudEditorOverlay(overlays.get(0));
        } else {
            selectHudEditorOverlay(null);
        }
    }

    public void resetAllPositionsToCenter() {
        android.util.DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int centerX = metrics.widthPixels / 2 - (int)(26 * metrics.density);
        int centerY = metrics.heightPixels / 2 - (int)(26 * metrics.density);

        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        
        for (java.util.Map.Entry<String, BaseOverlayButton> entry : modOverlayMap.entrySet()) {
            manager.setOverlayPosition(entry.getKey(), centerX, centerY);
            entry.getValue().updatePosition(centerX, centerY);
        }
        
        if (fpsDisplayOverlay != null) {
            manager.setOverlayPosition(ModIds.FPS_DISPLAY, centerX, centerY);
            fpsDisplayOverlay.updatePosition(centerX, centerY);
        }
        if (cpsDisplayOverlay != null) {
            manager.setOverlayPosition(ModIds.CPS_DISPLAY, centerX, centerY);
            cpsDisplayOverlay.updatePosition(centerX, centerY);
        }
        
        org.levimc.launcher.core.mods.inbuilt.ExternalModBridge.DrawCommand[] cmds = org.levimc.launcher.core.mods.inbuilt.ExternalModBridge.getDrawCommands();
        if (cmds != null) {
            java.util.Set<String> processed = new java.util.HashSet<>();
            for (org.levimc.launcher.core.mods.inbuilt.ExternalModBridge.DrawCommand cmd : cmds) {
                if (cmd.moduleId != null && !processed.contains(cmd.moduleId)) {
                    processed.add(cmd.moduleId);
                    org.levimc.launcher.core.mods.inbuilt.ExternalModBridge.setExternalModConfig(cmd.moduleId, "hudPosX", String.valueOf(centerX));
                    org.levimc.launcher.core.mods.inbuilt.ExternalModBridge.setExternalModConfig(cmd.moduleId, "hudPosY", String.valueOf(centerY));
                }
            }
        }
    }

    public void tick() {
        for (BaseOverlayButton overlay : overlays) {
            overlay.tick();
        }

        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        boolean isPauseOnly = manager.isPauseMenuOnly();
        boolean forceGlobalModMenu = org.levimc.launcher.preloader.PreloaderInput.shouldForceGlobalModMenu();
        boolean isPauseOpen = org.levimc.launcher.preloader.PreloaderInput.isPauseMenuOpen();
        boolean isHudScreenOpen = org.levimc.launcher.preloader.PreloaderInput.isHudScreenOpen();
        boolean isShowingMenu = org.levimc.launcher.preloader.PreloaderInput.isShowingMenu();
        boolean showGameOverlays = forceGlobalModMenu || (isHudScreenOpen && !isShowingMenu);
        boolean inbuiltVisible = hudEditorMode || showGameOverlays;
        long stateHash = manager.getOverlayVisibilityRevision();
        stateHash = 31L * stateHash + (isPauseOnly ? 1L : 0L);
        stateHash = 31L * stateHash + (forceGlobalModMenu ? 1L : 0L);
        stateHash = 31L * stateHash + (isPauseOpen ? 1L : 0L);
        stateHash = 31L * stateHash + (isHudScreenOpen ? 1L : 0L);
        stateHash = 31L * stateHash + (isShowingMenu ? 1L : 0L);
        stateHash = 31L * stateHash + (hudEditorMode ? 1L : 0L);
        stateHash = 31L * stateHash + overlays.size();
        for (BaseOverlayButton overlay : overlays) {
            stateHash = 31L * stateHash + System.identityHashCode(overlay);
        }
        stateHash = 31L * stateHash + (fpsDisplayOverlay == null ? 0L : System.identityHashCode(fpsDisplayOverlay));
        stateHash = 31L * stateHash + (cpsDisplayOverlay == null ? 0L : System.identityHashCode(cpsDisplayOverlay));
        stateHash = 31L * stateHash + (modMenuButton == null ? 0L : System.identityHashCode(modMenuButton));
        stateHash = 31L * stateHash + (hudOverlay == null ? 0L : System.identityHashCode(hudOverlay));
        if (stateHash == lastVisibilityStateHash) return;
        lastVisibilityStateHash = stateHash;

        activity.runOnUiThread(() -> {
            if (modMenuButton != null) {
                int visibility = !isPauseOnly || forceGlobalModMenu || isPauseOpen
                        ? android.view.View.VISIBLE
                        : android.view.View.GONE;
                modMenuButton.setVisibility(visibility);
                if (visibility == android.view.View.GONE && modMenuButton.isMenuShowing()) {
                    modMenuButton.hideMenu();
                }
            }

            if (hudOverlay != null) {
                int visibility = hudOverlay.isHudEditorMode() || showGameOverlays
                        ? android.view.View.VISIBLE
                        : android.view.View.GONE;
                if (hudOverlay.getVisibility() != visibility) {
                    hudOverlay.setVisibility(visibility);
                }
            }

            for (BaseOverlayButton overlay : overlays) {
                if (overlay.overlayView != null) {
                    int visibility = inbuiltVisible || manager.isOverlayShowEverywhere(overlay.getOverlayConfigKey())
                            ? android.view.View.VISIBLE
                            : android.view.View.GONE;
                    if (overlay.overlayView.getVisibility() != visibility) {
                        overlay.overlayView.setVisibility(visibility);
                    }
                }
            }

            if (fpsDisplayOverlay != null) {
                int visibility = inbuiltVisible || manager.isOverlayShowEverywhere(ModIds.FPS_DISPLAY)
                        ? android.view.View.VISIBLE
                        : android.view.View.GONE;
                fpsDisplayOverlay.setVisibility(visibility);
            }

            if (cpsDisplayOverlay != null) {
                int visibility = inbuiltVisible || manager.isOverlayShowEverywhere(ModIds.CPS_DISPLAY)
                        ? android.view.View.VISIBLE
                        : android.view.View.GONE;
                cpsDisplayOverlay.setVisibility(visibility);
            }
        });
    }
}

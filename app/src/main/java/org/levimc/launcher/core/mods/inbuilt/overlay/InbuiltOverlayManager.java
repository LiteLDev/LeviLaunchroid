package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;

import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;
import org.levimc.launcher.core.mods.memoryeditor.MemoryAddress;
import org.levimc.launcher.core.mods.memoryeditor.MemoryEditorButton;
import org.levimc.launcher.core.mods.memoryeditor.MemoryOverlayButton;
import org.levimc.launcher.core.mods.memoryeditor.SavedAddressManager;
import org.levimc.launcher.settings.FeatureSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InbuiltOverlayManager {
    private static volatile InbuiltOverlayManager instance;
    private final Activity activity;
    private final List<BaseOverlayButton> overlays = new ArrayList<>();
    private final List<MemoryOverlayButton> memoryOverlays = new ArrayList<>();
    private final Map<String, Boolean> modActiveStates = new HashMap<>();
    private final Map<String, BaseOverlayButton> modOverlayMap = new HashMap<>();
    private final Map<String, Integer> modPositionMap = new HashMap<>();
    private MemoryEditorButton memoryEditorButton;
    private ChickPetOverlay chickPetOverlay;
    private ZoomOverlay zoomOverlay;
    private SnaplookOverlay snaplookOverlay;
    private FpsDisplayOverlay fpsDisplayOverlay;
    private CpsDisplayOverlay cpsDisplayOverlay;
    private ModMenuButton modMenuButton;
    private int baseY = 150;
    private static final int SPACING = 70;
    private static final int START_X = 50;
    private boolean isModMenuMode = false;

    public InbuiltOverlayManager(Activity activity) {
        this.activity = activity;
        instance = this;
    }

    public static InbuiltOverlayManager getInstance() {
        return instance;
    }

    public void showEnabledOverlays() {
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        int nextY = baseY;

        isModMenuMode = manager.isModMenuEnabled();

        if (isModMenuMode) {
            nextY = showModMenuMode(manager, nextY);
        } else {
            nextY = showIndividualOverlays(manager, nextY);
        }

        if (FeatureSettings.getInstance().isMemoryEditorEnabled()) {
            memoryEditorButton = new MemoryEditorButton(activity);
            memoryEditorButton.show(START_X, nextY);
            nextY += SPACING;
        }

        List<MemoryAddress> overlayAddresses = SavedAddressManager.getInstance(activity).getOverlayEnabledAddresses();
        for (MemoryAddress addr : overlayAddresses) {
            MemoryOverlayButton overlayBtn = new MemoryOverlayButton(activity, addr);
            overlayBtn.show(START_X, nextY);
            memoryOverlays.add(overlayBtn);
            nextY += SPACING;
        }
    }

    private int showModMenuMode(InbuiltModManager manager, int nextY) {
        modActiveStates.put(ModIds.QUICK_DROP, false);
        modActiveStates.put(ModIds.CAMERA_PERSPECTIVE, false);
        modActiveStates.put(ModIds.TOGGLE_HUD, false);
        modActiveStates.put(ModIds.AUTO_SPRINT, false);
        modActiveStates.put(ModIds.CHICK_PET, false);
        modActiveStates.put(ModIds.ZOOM, false);
        modActiveStates.put(ModIds.FPS_DISPLAY, false);
        modActiveStates.put(ModIds.CPS_DISPLAY, false);
        modActiveStates.put(ModIds.SNAPLOOK, false);

        modPositionMap.put(ModIds.QUICK_DROP, nextY + SPACING);
        modPositionMap.put(ModIds.CAMERA_PERSPECTIVE, nextY + SPACING * 2);
        modPositionMap.put(ModIds.TOGGLE_HUD, nextY + SPACING * 3);
        modPositionMap.put(ModIds.AUTO_SPRINT, nextY + SPACING * 4);
        modPositionMap.put(ModIds.ZOOM, nextY + SPACING * 5);
        modPositionMap.put(ModIds.FPS_DISPLAY, nextY + SPACING * 6);
        modPositionMap.put(ModIds.CPS_DISPLAY, nextY + SPACING * 7);
        modPositionMap.put(ModIds.SNAPLOOK, nextY + SPACING * 8);

        if (zoomOverlay == null) {
            zoomOverlay = new ZoomOverlay(activity);
            zoomOverlay.initializeForKeyboard();
        }

        if (snaplookOverlay == null) {
            snaplookOverlay = new SnaplookOverlay(activity);
            snaplookOverlay.initializeForKeyboard();
        }

        modMenuButton = new ModMenuButton(activity);
        modMenuButton.show(START_X, nextY);
        return nextY + SPACING;
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

        int posY = modPositionMap.getOrDefault(modId, baseY + SPACING);
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);

        switch (modId) {
            case ModIds.QUICK_DROP:
                QuickDropOverlay quickDrop = new QuickDropOverlay(activity);
                quickDrop.show(START_X, posY);
                overlays.add(quickDrop);
                modOverlayMap.put(modId, quickDrop);
                break;
            case ModIds.CAMERA_PERSPECTIVE:
                CameraPerspectiveOverlay camera = new CameraPerspectiveOverlay(activity);
                camera.show(START_X, posY);
                overlays.add(camera);
                modOverlayMap.put(modId, camera);
                break;
            case ModIds.TOGGLE_HUD:
                ToggleHudOverlay hud = new ToggleHudOverlay(activity);
                hud.show(START_X, posY);
                overlays.add(hud);
                modOverlayMap.put(modId, hud);
                break;
            case ModIds.AUTO_SPRINT:
                AutoSprintOverlay sprint = new AutoSprintOverlay(activity, manager.getAutoSprintKey());
                sprint.show(START_X, posY);
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
                zoomOverlay.show(START_X, posY);
                overlays.add(zoomOverlay);
                modOverlayMap.put(modId, zoomOverlay);
                break;
            case ModIds.FPS_DISPLAY:
                if (fpsDisplayOverlay == null) {
                    fpsDisplayOverlay = new FpsDisplayOverlay(activity);
                    fpsDisplayOverlay.show(START_X, posY);
                }
                break;
            case ModIds.CPS_DISPLAY:
                if (cpsDisplayOverlay == null) {
                    cpsDisplayOverlay = new CpsDisplayOverlay(activity);
                    cpsDisplayOverlay.show(START_X, posY);
                }
                break;
            case ModIds.SNAPLOOK:
                if (snaplookOverlay == null) {
                    snaplookOverlay = new SnaplookOverlay(activity);
                }
                snaplookOverlay.show(START_X, posY);
                overlays.add(snaplookOverlay);
                modOverlayMap.put(modId, snaplookOverlay);
                break;
        }
    }

    private void hideModOverlay(String modId) {
        if (modId.equals(ModIds.CHICK_PET)) {
            if (chickPetOverlay != null) {
                chickPetOverlay.hide();
                chickPetOverlay = null;
            }
            return;
        }
        
        if (modId.equals(ModIds.ZOOM)) {
            if (zoomOverlay != null) {
                zoomOverlay.hide();
                overlays.remove(zoomOverlay);
                modOverlayMap.remove(modId);

                if (!isModMenuMode) {
                    zoomOverlay = null;
                }
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
                snaplookOverlay.hide();
                overlays.remove(snaplookOverlay);
                modOverlayMap.remove(modId);

                if (!isModMenuMode) {
                    snaplookOverlay = null;
                }
            }
            return;
        }
        
        BaseOverlayButton overlay = modOverlayMap.get(modId);
        if (overlay != null) {
            overlay.hide();
            overlays.remove(overlay);
            modOverlayMap.remove(modId);
        }
    }

    public boolean isModActive(String modId) {
        return modActiveStates.getOrDefault(modId, false);
    }

    private int showIndividualOverlays(InbuiltModManager manager, int nextY) {
        if (manager.isModAdded(ModIds.QUICK_DROP)) {
            int x = manager.getOverlayPositionX(ModIds.QUICK_DROP, START_X);
            int y = manager.getOverlayPositionY(ModIds.QUICK_DROP, nextY);
            QuickDropOverlay overlay = new QuickDropOverlay(activity);
            overlay.show(x, y);
            overlays.add(overlay);
            nextY += SPACING;
        }
        if (manager.isModAdded(ModIds.CAMERA_PERSPECTIVE)) {
            int x = manager.getOverlayPositionX(ModIds.CAMERA_PERSPECTIVE, START_X);
            int y = manager.getOverlayPositionY(ModIds.CAMERA_PERSPECTIVE, nextY);
            CameraPerspectiveOverlay overlay = new CameraPerspectiveOverlay(activity);
            overlay.show(x, y);
            overlays.add(overlay);
            nextY += SPACING;
        }
        if (manager.isModAdded(ModIds.TOGGLE_HUD)) {
            int x = manager.getOverlayPositionX(ModIds.TOGGLE_HUD, START_X);
            int y = manager.getOverlayPositionY(ModIds.TOGGLE_HUD, nextY);
            ToggleHudOverlay overlay = new ToggleHudOverlay(activity);
            overlay.show(x, y);
            overlays.add(overlay);
            nextY += SPACING;
        }
        if (manager.isModAdded(ModIds.AUTO_SPRINT)) {
            int x = manager.getOverlayPositionX(ModIds.AUTO_SPRINT, START_X);
            int y = manager.getOverlayPositionY(ModIds.AUTO_SPRINT, nextY);
            AutoSprintOverlay overlay = new AutoSprintOverlay(activity, manager.getAutoSprintKey());
            overlay.show(x, y);
            overlays.add(overlay);
            nextY += SPACING;
        }

        if (manager.isModAdded(ModIds.CHICK_PET)) {
            chickPetOverlay = new ChickPetOverlay(activity);
            chickPetOverlay.show();
        }

        if (manager.isModAdded(ModIds.ZOOM)) {
            int x = manager.getOverlayPositionX(ModIds.ZOOM, START_X);
            int y = manager.getOverlayPositionY(ModIds.ZOOM, nextY);
            zoomOverlay = new ZoomOverlay(activity);
            zoomOverlay.show(x, y);
            overlays.add(zoomOverlay);
            modOverlayMap.put(ModIds.ZOOM, zoomOverlay);
            nextY += SPACING;
        }

        if (manager.isModAdded(ModIds.FPS_DISPLAY)) {
            int x = manager.getOverlayPositionX(ModIds.FPS_DISPLAY, START_X);
            int y = manager.getOverlayPositionY(ModIds.FPS_DISPLAY, nextY);
            fpsDisplayOverlay = new FpsDisplayOverlay(activity);
            fpsDisplayOverlay.show(x, y);
            nextY += SPACING;
        }

        if (manager.isModAdded(ModIds.CPS_DISPLAY)) {
            int x = manager.getOverlayPositionX(ModIds.CPS_DISPLAY, START_X);
            int y = manager.getOverlayPositionY(ModIds.CPS_DISPLAY, nextY);
            cpsDisplayOverlay = new CpsDisplayOverlay(activity);
            cpsDisplayOverlay.show(x, y);
            nextY += SPACING;
        }

        if (manager.isModAdded(ModIds.SNAPLOOK)) {
            int x = manager.getOverlayPositionX(ModIds.SNAPLOOK, START_X);
            int y = manager.getOverlayPositionY(ModIds.SNAPLOOK, nextY);
            snaplookOverlay = new SnaplookOverlay(activity);
            snaplookOverlay.show(x, y);
            overlays.add(snaplookOverlay);
            modOverlayMap.put(ModIds.SNAPLOOK, snaplookOverlay);
            nextY += SPACING;
        }
        return nextY;
    }

    public void addMemoryOverlay(MemoryAddress address) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        for (MemoryOverlayButton existing : memoryOverlays) {
            if (existing.getMemoryAddress().getAddress() == address.getAddress()) {
                return;
            }
        }
        int posY = baseY + (overlays.size() + memoryOverlays.size() + 1) * SPACING;
        MemoryOverlayButton overlayBtn = new MemoryOverlayButton(activity, address);
        overlayBtn.show(START_X, posY);
        memoryOverlays.add(overlayBtn);
    }

    public void removeMemoryOverlay(long addressValue) {
        MemoryOverlayButton toRemove = null;
        for (MemoryOverlayButton btn : memoryOverlays) {
            if (btn.getMemoryAddress().getAddress() == addressValue) {
                toRemove = btn;
                break;
            }
        }
        if (toRemove != null) {
            toRemove.hide();
            memoryOverlays.remove(toRemove);
        }
    }

    public void hideAllOverlays() {
        for (BaseOverlayButton overlay : overlays) {
            overlay.hide();
        }
        overlays.clear();
        modOverlayMap.clear();
        for (MemoryOverlayButton memOverlay : memoryOverlays) {
            memOverlay.hide();
        }
        memoryOverlays.clear();
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
        if (modMenuButton != null) {
            modMenuButton.hide();
            modMenuButton = null;
        }
        if (memoryEditorButton != null) {
            if (memoryEditorButton.getEditorOverlay() != null) {
                memoryEditorButton.getEditorOverlay().hide();
            }
            memoryEditorButton.hide();
            memoryEditorButton = null;
        }
        instance = null;
    }

    public boolean handleKeyEvent(int keyCode, int action) {
        boolean zoomEnabled = isModMenuMode 
            ? modActiveStates.getOrDefault(ModIds.ZOOM, false)
            : InbuiltModManager.getInstance(activity).isModAdded(ModIds.ZOOM);
        
        if (zoomEnabled && keyCode == android.view.KeyEvent.KEYCODE_C) {
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

        boolean snaplookEnabled = isModMenuMode
            ? modActiveStates.getOrDefault(ModIds.SNAPLOOK, false)
            : InbuiltModManager.getInstance(activity).isModAdded(ModIds.SNAPLOOK);

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
        if (zoomOverlay != null && zoomOverlay.isZooming()) {
            zoomOverlay.onScroll(scrollDelta);
            return true;
        }
        return false;
    }

    public ZoomOverlay getZoomOverlay() {
        return zoomOverlay;
    }

    public SnaplookOverlay getSnaplookOverlay() {
        return snaplookOverlay;
    }

}

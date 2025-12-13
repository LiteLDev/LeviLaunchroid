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
import java.util.List;

public class InbuiltOverlayManager {
    private static volatile InbuiltOverlayManager instance;
    private final Activity activity;
    private final List<BaseOverlayButton> overlays = new ArrayList<>();
    private final List<MemoryOverlayButton> memoryOverlays = new ArrayList<>();
    private MemoryEditorButton memoryEditorButton;
    private int nextY = 150;
    private static final int SPACING = 70;
    private static final int START_X = 50;

    public InbuiltOverlayManager(Activity activity) {
        this.activity = activity;
        instance = this;
    }

    public static InbuiltOverlayManager getInstance() {
        return instance;
    }

    public void showEnabledOverlays() {
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        nextY = 150;

        if (manager.isModAdded(ModIds.QUICK_DROP)) {
            QuickDropOverlay overlay = new QuickDropOverlay(activity);
            overlay.show(START_X, nextY);
            overlays.add(overlay);
            nextY += SPACING;
        }
        if (manager.isModAdded(ModIds.CAMERA_PERSPECTIVE)) {
            CameraPerspectiveOverlay overlay = new CameraPerspectiveOverlay(activity);
            overlay.show(START_X, nextY);
            overlays.add(overlay);
            nextY += SPACING;
        }
        if (manager.isModAdded(ModIds.TOGGLE_HUD)) {
            ToggleHudOverlay overlay = new ToggleHudOverlay(activity);
            overlay.show(START_X, nextY);
            overlays.add(overlay);
            nextY += SPACING;
        }
        if (manager.isModAdded(ModIds.AUTO_SPRINT)) {
            AutoSprintOverlay overlay = new AutoSprintOverlay(activity, manager.getAutoSprintKey());
            overlay.show(START_X, nextY);
            overlays.add(overlay);
            nextY += SPACING;
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

    public void addMemoryOverlay(MemoryAddress address) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        for (MemoryOverlayButton existing : memoryOverlays) {
            if (existing.getMemoryAddress().getAddress() == address.getAddress()) {
                return;
            }
        }
        MemoryOverlayButton overlayBtn = new MemoryOverlayButton(activity, address);
        overlayBtn.show(START_X, nextY);
        memoryOverlays.add(overlayBtn);
        nextY += SPACING;
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
        for (MemoryOverlayButton memOverlay : memoryOverlays) {
            memOverlay.hide();
        }
        memoryOverlays.clear();
        if (memoryEditorButton != null) {
            if (memoryEditorButton.getEditorOverlay() != null) {
                memoryEditorButton.getEditorOverlay().hide();
            }
            memoryEditorButton.hide();
            memoryEditorButton = null;
        }
        instance = null;
    }
}
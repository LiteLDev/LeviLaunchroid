package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;

import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;

import java.util.ArrayList;
import java.util.List;

public class InbuiltOverlayManager {
    private final Activity activity;
    private final List<BaseOverlayButton> overlays = new ArrayList<>();

    public InbuiltOverlayManager(Activity activity) {
        this.activity = activity;
    }

    public void showEnabledOverlays() {
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        int x = 50;
        int y = 150;
        int spacing = 70;

        if (manager.isModAdded(ModIds.QUICK_DROP)) {
            QuickDropOverlay overlay = new QuickDropOverlay(activity);
            overlay.show(x, y);
            overlays.add(overlay);
            y += spacing;
        }
        if (manager.isModAdded(ModIds.CAMERA_PERSPECTIVE)) {
            CameraPerspectiveOverlay overlay = new CameraPerspectiveOverlay(activity);
            overlay.show(x, y);
            overlays.add(overlay);
            y += spacing;
        }
        if (manager.isModAdded(ModIds.TOGGLE_HUD)) {
            ToggleHudOverlay overlay = new ToggleHudOverlay(activity);
            overlay.show(x, y);
            overlays.add(overlay);
            y += spacing;
        }
        if (manager.isModAdded(ModIds.AUTO_SPRINT)) {
            AutoSprintOverlay overlay = new AutoSprintOverlay(activity, manager.getAutoSprintKey());
            overlay.show(x, y);
            overlays.add(overlay);
        }
    }

    public void hideAllOverlays() {
        for (BaseOverlayButton overlay : overlays) {
            overlay.hide();
        }
        overlays.clear();
    }
}
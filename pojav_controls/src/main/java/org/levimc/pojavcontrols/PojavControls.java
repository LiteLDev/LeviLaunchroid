package org.levimc.pojavcontrols;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

public final class PojavControls {
    public static final String ACTION_PROFILE_CHANGED = "org.levimc.pojavcontrols.PROFILE_CHANGED";

    private static WeakReference<Activity> attachedActivity = new WeakReference<>(null);
    private static PojavControlOverlay overlay;
    private static WeakReference<Activity> editorActivity = new WeakReference<>(null);
    private static PojavControlsEditorView editor;

    private PojavControls() {}

    public static synchronized void setEnabled(Activity activity, PojavControlsHost host, boolean enabled) {
        if (enabled) attach(activity, host);
        else detach();
    }

    public static synchronized boolean isEnabled() {
        return overlay != null && overlay.isAttachedToWindow();
    }

    public static void launchEditor(Activity activity) {
        activity.runOnUiThread(() -> showEditor(activity));
    }

    public static synchronized boolean closeEditor() {
        if (editor == null) return false;
        editor.close();
        return true;
    }

    public static synchronized boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        return editor != null && editor.handleActivityResult(requestCode, resultCode, data);
    }

    public static synchronized boolean ownsTouchInput() {
        View target = editor != null && editor.isAttachedToWindow() ? editor : overlay;
        return target != null && target.isAttachedToWindow() && target.getVisibility() == View.VISIBLE;
    }

    public static synchronized void reload() {
        if (overlay != null) overlay.reloadProfile();
    }

    private static void attach(Activity activity, PojavControlsHost host) {
        Activity current = attachedActivity.get();
        if (overlay != null && current == activity && overlay.isAttachedToWindow()) {
            overlay.reloadProfile();
            overlay.bringToFront();
            return;
        }
        detach();
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) return;
        overlay = new PojavControlOverlay(activity, host);
        ((ViewGroup) content).addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.bringToFront();
        attachedActivity = new WeakReference<>(activity);
    }

    private static synchronized void showEditor(Activity activity) {
        if (editor != null) {
            if (editorActivity.get() == activity && editor.isAttachedToWindow()) {
                editor.bringToFront();
                return;
            }
            finishEditor();
        }
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) return;
        if (overlay != null) {
            overlay.releaseAll();
            overlay.setVisibility(View.GONE);
        }
        editor = new PojavControlsEditorView(activity, PojavControls::finishEditor);
        ((ViewGroup) content).addView(editor, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        editor.bringToFront();
        editorActivity = new WeakReference<>(activity);
    }

    private static synchronized void finishEditor() {
        if (editor != null && editor.getParent() instanceof ViewGroup) {
            ((ViewGroup) editor.getParent()).removeView(editor);
        }
        editor = null;
        editorActivity.clear();
        if (overlay != null) {
            overlay.reloadProfile();
            overlay.setVisibility(View.VISIBLE);
            overlay.bringToFront();
        }
    }

    private static void detach() {
        if (editor != null) editor.close();
        if (overlay != null) {
            overlay.releaseAll();
            if (overlay.getParent() instanceof ViewGroup) ((ViewGroup) overlay.getParent()).removeView(overlay);
            overlay.dispose();
        }
        overlay = null;
        attachedActivity.clear();
    }
}

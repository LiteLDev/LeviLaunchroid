package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

final class OverlayBounds {
    static final class Position {
        final int x;
        final int y;

        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private OverlayBounds() {
    }

    static Position clampPosition(Activity activity, View view, int x, int y) {
        return clampPosition(activity, x, y, resolveViewWidth(view), resolveViewHeight(view));
    }

    static Position clampPosition(Activity activity, int x, int y, int width, int height) {
        int parentWidth = resolveParentWidth(activity);
        int parentHeight = resolveParentHeight(activity);
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        int maxX = Math.max(0, parentWidth - safeWidth);
        int maxY = Math.max(0, parentHeight - safeHeight);
        return new Position(clamp(x, 0, maxX), clamp(y, 0, maxY));
    }

    private static int resolveViewWidth(View view) {
        if (view == null) {
            return 1;
        }
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null && params.width > 0) {
            return params.width;
        }
        if (params != null && params.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            measureWrapContent(view);
            if (view.getMeasuredWidth() > 0) {
                return view.getMeasuredWidth();
            }
        }
        if (view.getWidth() > 0) {
            return view.getWidth();
        }
        if (view.getMeasuredWidth() <= 0) {
            measureWrapContent(view);
        }
        return Math.max(1, view.getMeasuredWidth());
    }

    private static int resolveViewHeight(View view) {
        if (view == null) {
            return 1;
        }
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null && params.height > 0) {
            return params.height;
        }
        if (params != null && params.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            measureWrapContent(view);
            if (view.getMeasuredHeight() > 0) {
                return view.getMeasuredHeight();
            }
        }
        if (view.getHeight() > 0) {
            return view.getHeight();
        }
        if (view.getMeasuredHeight() <= 0) {
            measureWrapContent(view);
        }
        return Math.max(1, view.getMeasuredHeight());
    }

    private static void measureWrapContent(View view) {
        view.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
    }

    private static int resolveParentWidth(Activity activity) {
        View view = resolveContentView(activity);
        if (view != null && view.getWidth() > 0) {
            return view.getWidth();
        }
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        return Math.max(1, metrics.widthPixels);
    }

    private static int resolveParentHeight(Activity activity) {
        View view = resolveContentView(activity);
        if (view != null && view.getHeight() > 0) {
            return view.getHeight();
        }
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        return Math.max(1, metrics.heightPixels);
    }

    private static View resolveContentView(Activity activity) {
        if (activity == null) {
            return null;
        }
        View content = activity.findViewById(android.R.id.content);
        if (content != null) {
            return content;
        }
        return activity.getWindow() == null ? null : activity.getWindow().getDecorView();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

package org.levimc.pojavcontrols;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

final class ControlEditorCanvas extends ViewGroup {
    private static final int NO_GUIDE = Integer.MIN_VALUE;

    interface EditListener {
        void edit(EditorTarget target);
    }

    static final class EditorTarget {
        static final int BUTTON = 0;
        static final int JOYSTICK = 1;
        static final int DRAWER = 2;
        static final int DRAWER_BUTTON = 3;

        final int type;
        final ControlData data;
        final ControlJoystickData joystick;
        final ControlDrawerData drawer;
        final Runnable deleteAction;
        final Runnable cloneAction;

        EditorTarget(int type, ControlData data, ControlJoystickData joystick, ControlDrawerData drawer,
                     Runnable deleteAction, Runnable cloneAction) {
            this.type = type;
            this.data = data;
            this.joystick = joystick;
            this.drawer = drawer;
            this.deleteAction = deleteAction;
            this.cloneAction = cloneAction;
        }
    }

    private final EditListener listener;
    private final Map<EditorItemView, EditorTarget> targets = new HashMap<>();
    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private CustomControls profile;
    private int guideX = NO_GUIDE;
    private int guideY = NO_GUIDE;

    ControlEditorCanvas(Context context, EditListener listener) {
        super(context);
        this.listener = listener;
        setClipChildren(false);
        setBackgroundColor(0x66101316);
        float density = getResources().getDisplayMetrics().density;
        guidePaint.setColor(0xFF4AE0A0);
        guidePaint.setStrokeWidth(Math.max(2f, density));
    }

    void setProfile(CustomControls profile) {
        this.profile = profile;
        rebuild();
    }

    void rebuild() {
        removeAllViews();
        targets.clear();
        if (profile == null) return;
        for (ControlData data : new ArrayList<>(profile.mControlDataList)) {
            addTarget(new EditorTarget(EditorTarget.BUTTON, data, null, null,
                    () -> { profile.mControlDataList.remove(data); rebuild(); },
                    () -> { profile.mControlDataList.add(new ControlData(data)); rebuild(); }));
        }
        for (ControlJoystickData data : new ArrayList<>(profile.mJoystickDataList)) {
            addTarget(new EditorTarget(EditorTarget.JOYSTICK, data, data, null,
                    () -> { profile.mJoystickDataList.remove(data); rebuild(); },
                    () -> { profile.mJoystickDataList.add(new ControlJoystickData(data)); rebuild(); }));
        }
        for (ControlDrawerData drawer : new ArrayList<>(profile.mDrawerDataList)) {
            addTarget(new EditorTarget(EditorTarget.DRAWER, drawer.properties, null, drawer,
                    () -> { profile.mDrawerDataList.remove(drawer); rebuild(); },
                    () -> { profile.mDrawerDataList.add(new ControlDrawerData(drawer)); rebuild(); }));
            for (ControlData data : new ArrayList<>(drawer.buttonProperties)) {
                addTarget(new EditorTarget(EditorTarget.DRAWER_BUTTON, data, null, drawer,
                        () -> { drawer.buttonProperties.remove(data); rebuild(); },
                        () -> { drawer.buttonProperties.add(new ControlData(data)); rebuild(); }));
            }
        }
        requestLayout();
    }

    private void addTarget(EditorTarget target) {
        EditorItemView view = new EditorItemView(getContext(), target);
        targets.put(view, target);
        addView(view);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < getChildCount(); i++) {
            ControlData data = targets.get((EditorItemView) getChildAt(i)).data;
            int childWidth = Math.max(1, Math.round(data.width * density));
            int childHeight = Math.max(1, Math.round(data.height * density));
            getChildAt(i).measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        for (int i = 0; i < getChildCount(); i++) {
            EditorItemView child = (EditorItemView) getChildAt(i);
            ControlData data = targets.get(child).data;
            int x = evaluate(data.dynamicX, data, width, height, true);
            int y = evaluate(data.dynamicY, data, width, height, false);
            x = Math.max(0, Math.min(x, width - child.getMeasuredWidth()));
            y = Math.max(0, Math.min(y, height - child.getMeasuredHeight()));
            child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (guideX != NO_GUIDE) canvas.drawLine(guideX, 0, guideX, getHeight(), guidePaint);
        if (guideY != NO_GUIDE) canvas.drawLine(0, guideY, getWidth(), guideY, guidePaint);
    }

    private SnapPosition snap(EditorItemView moving, int requestedX, int requestedY) {
        int maxX = Math.max(0, getWidth() - moving.getWidth());
        int maxY = Math.max(0, getHeight() - moving.getHeight());
        int x = Math.max(0, Math.min(requestedX, maxX));
        int y = Math.max(0, Math.min(requestedY, maxY));
        int threshold = Math.round(8 * getResources().getDisplayMetrics().density);
        int gap = Math.round(8 * getResources().getDisplayMetrics().density);
        AxisSnap xSnap = new AxisSnap(x, threshold);
        AxisSnap ySnap = new AxisSnap(y, threshold);

        xSnap.offer(0, 0);
        xSnap.offer(maxX, getWidth());
        xSnap.offer(maxX / 2, getWidth() / 2);
        ySnap.offer(0, 0);
        ySnap.offer(maxY, getHeight());
        ySnap.offer(maxY / 2, getHeight() / 2);

        for (int i = 0; i < getChildCount(); i++) {
            View other = getChildAt(i);
            if (other == moving) continue;
            int otherCenterX = other.getLeft() + other.getWidth() / 2;
            int otherCenterY = other.getTop() + other.getHeight() / 2;
            xSnap.offer(other.getLeft(), other.getLeft());
            xSnap.offer(other.getRight() - moving.getWidth(), other.getRight());
            xSnap.offer(otherCenterX - moving.getWidth() / 2, otherCenterX);
            xSnap.offer(other.getLeft() - moving.getWidth() - gap, other.getLeft() - gap / 2);
            xSnap.offer(other.getRight() + gap, other.getRight() + gap / 2);
            ySnap.offer(other.getTop(), other.getTop());
            ySnap.offer(other.getBottom() - moving.getHeight(), other.getBottom());
            ySnap.offer(otherCenterY - moving.getHeight() / 2, otherCenterY);
            ySnap.offer(other.getTop() - moving.getHeight() - gap, other.getTop() - gap / 2);
            ySnap.offer(other.getBottom() + gap, other.getBottom() + gap / 2);
        }

        return new SnapPosition(
                Math.max(0, Math.min(xSnap.value, maxX)),
                Math.max(0, Math.min(ySnap.value, maxY)),
                xSnap.guide,
                ySnap.guide);
    }

    private void setGuides(int x, int y) {
        if (guideX == x && guideY == y) return;
        guideX = x;
        guideY = y;
        invalidate();
    }

    private static final class AxisSnap {
        private final int requested;
        private final int threshold;
        private int distance;
        int value;
        int guide = NO_GUIDE;

        AxisSnap(int requested, int threshold) {
            this.requested = requested;
            this.threshold = threshold;
            this.value = requested;
            this.distance = threshold + 1;
        }

        void offer(int candidate, int candidateGuide) {
            int candidateDistance = Math.abs(requested - candidate);
            if (candidateDistance <= threshold && candidateDistance < distance) {
                value = candidate;
                guide = candidateGuide;
                distance = candidateDistance;
            }
        }
    }

    private static final class SnapPosition {
        final int x;
        final int y;
        final int guideX;
        final int guideY;

        SnapPosition(int x, int y, int guideX, int guideY) {
            this.x = x;
            this.y = y;
            this.guideX = guideX;
            this.guideY = guideY;
        }
    }

    private int evaluate(String expression, ControlData data, int screenWidth, int screenHeight, boolean xAxis) {
        float density = getResources().getDisplayMetrics().density;
        float childWidth = data.width * density;
        float childHeight = data.height * density;
        HashMap<String, Float> values = new HashMap<>();
        values.put("top", 0f);
        values.put("left", 0f);
        values.put("right", screenWidth - childWidth);
        values.put("bottom", screenHeight - childHeight);
        values.put("width", childWidth);
        values.put("height", childHeight);
        values.put("screen_width", (float) screenWidth);
        values.put("screen_height", (float) screenHeight);
        values.put("margin", 8f * density);
        values.put("preferred_scale", 100f);
        return Math.round(ExpressionEvaluator.evaluate(expression, values,
                xAxis ? (screenWidth - childWidth) / 2f : (screenHeight - childHeight) / 2f));
    }

    private final class EditorItemView extends AppCompatTextView {
        private final EditorTarget target;
        private float startRawX;
        private float startRawY;
        private float startTranslationX;
        private float startTranslationY;
        private boolean moved;

        EditorItemView(Context context, EditorTarget target) {
            super(context);
            this.target = target;
            setText(target.data.name);
            setTextColor(Color.WHITE);
            setTextSize(12);
            setGravity(Gravity.CENTER);
            setPadding(4, 4, 4, 4);
            applyStyle();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startRawX = event.getRawX();
                    startRawY = event.getRawY();
                    startTranslationX = getTranslationX();
                    startTranslationY = getTranslationY();
                    moved = false;
                    bringToFront();
                    setGuides(NO_GUIDE, NO_GUIDE);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - startRawX;
                    float dy = event.getRawY() - startRawY;
                    if (Math.hypot(dx, dy) > 6 * getResources().getDisplayMetrics().density) moved = true;
                    SnapPosition position = snap(this,
                            Math.round(getLeft() + startTranslationX + dx),
                            Math.round(getTop() + startTranslationY + dy));
                    setTranslationX(position.x - getLeft());
                    setTranslationY(position.y - getTop());
                    setGuides(position.guideX, position.guideY);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (moved) {
                        int x = Math.max(0, Math.min(Math.round(getLeft() + getTranslationX()),
                                getWidth() == 0 ? 0 : ControlEditorCanvas.this.getWidth() - getWidth()));
                        int y = Math.max(0, Math.min(Math.round(getTop() + getTranslationY()),
                                getHeight() == 0 ? 0 : ControlEditorCanvas.this.getHeight() - getHeight()));
                        target.data.dynamicX = Integer.toString(x);
                        target.data.dynamicY = Integer.toString(y);
                        setTranslationX(0f);
                        setTranslationY(0f);
                        requestLayout();
                    } else listener.edit(target);
                    setGuides(NO_GUIDE, NO_GUIDE);
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    setTranslationX(0f);
                    setTranslationY(0f);
                    setGuides(NO_GUIDE, NO_GUIDE);
                    return true;
                default:
                    return true;
            }
        }

        private void applyStyle() {
            GradientDrawable background = new GradientDrawable();
            int fill = target.type == EditorTarget.JOYSTICK ? 0x6639A0ED : target.data.bgColor;
            background.setColor(fill);
            float density = getResources().getDisplayMetrics().density;
            float radius = target.type == EditorTarget.JOYSTICK ? target.data.width * density / 2f
                    : Math.min(target.data.width, target.data.height) * density * target.data.cornerRadius / 200f;
            background.setCornerRadius(radius);
            background.setStroke(Math.max(2, Math.round(Math.max(1f, target.data.strokeWidth) * density)),
                    0xFF4AE0A0);
            setBackground(background);
            setAlpha(Math.max(0.25f, target.data.opacity));
        }
    }
}

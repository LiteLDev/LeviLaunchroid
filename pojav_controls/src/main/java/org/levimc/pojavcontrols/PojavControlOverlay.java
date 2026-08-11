package org.levimc.pojavcontrols;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class PojavControlOverlay extends ViewGroup {
    private final Activity activity;
    private final PojavControlsHost host;
    private final ControlRepository repository;
    private final RuntimeSurface runtimeSurface;
    private final VirtualMouseCursor cursorView;
    private final ArrayList<RuntimeButton> buttons = new ArrayList<>();
    private final ArrayList<RuntimeJoystick> joysticks = new ArrayList<>();
    private final ArrayList<DrawerRuntime> drawers = new ArrayList<>();
    private final Map<View, DrawerPlacement> drawerPlacements = new HashMap<>();
    private CustomControls profile;
    private boolean controlsVisible = true;
    private boolean virtualMouse;
    private float virtualCursorX = Float.NaN;
    private float virtualCursorY = Float.NaN;
    private boolean virtualTouchDown;
    private long virtualTouchDownAt;
    private boolean receiverRegistered;

    private final BroadcastReceiver profileReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reloadProfile();
        }
    };

    PojavControlOverlay(Activity activity, PojavControlsHost host) {
        super(activity);
        this.activity = activity;
        this.host = host;
        repository = new ControlRepository(activity);
        runtimeSurface = new RuntimeSurface(activity);
        cursorView = new VirtualMouseCursor(activity);
        setClipChildren(false);
        setClipToPadding(false);
        setMotionEventSplittingEnabled(true);
        setClickable(true);
        setFocusable(false);
        registerProfileReceiver();
        reloadProfile();
    }

    void reloadProfile() {
        releaseAll();
        removeAllViews();
        buttons.clear();
        joysticks.clear();
        drawers.clear();
        drawerPlacements.clear();
        profile = repository.loadActive();
        addView(runtimeSurface);
        for (ControlData data : profile.mControlDataList) addRuntimeButton(data);
        for (ControlJoystickData data : profile.mJoystickDataList) {
            RuntimeJoystick joystick = new RuntimeJoystick(getContext(), data, host);
            joysticks.add(joystick);
            addView(joystick);
        }
        for (ControlDrawerData data : profile.mDrawerDataList) addDrawer(data);
        addView(cursorView);
        cursorView.setVisibility(virtualMouse ? VISIBLE : GONE);
        updateVirtualMouseButtons();
        requestLayout();
        invalidate();
    }

    void releaseAll() {
        for (RuntimeButton button : buttons) button.release();
        for (RuntimeJoystick joystick : joysticks) joystick.release();
        runtimeSurface.release();
        releaseVirtualTouch();
    }

    void dispose() {
        releaseAll();
        if (receiverRegistered) {
            try {
                activity.unregisterReceiver(profileReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            receiverRegistered = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child == runtimeSurface) {
                child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                continue;
            }
            if (child == cursorView) {
                int cursorSize = Math.round(36 * density);
                child.measure(MeasureSpec.makeMeasureSpec(cursorSize, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(cursorSize, MeasureSpec.EXACTLY));
                continue;
            }
            ControlData data = dataFor(child);
            int childWidth = Math.max(1, Math.round(data.width * density));
            int childHeight = Math.max(1, Math.round(data.height * density));
            child.measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child == runtimeSurface) {
                child.layout(0, 0, width, height);
                continue;
            }
            if (child == cursorView) {
                if (Float.isNaN(virtualCursorX) || Float.isNaN(virtualCursorY)) {
                    virtualCursorX = width / 2f;
                    virtualCursorY = height / 2f;
                }
                int x = Math.round(virtualCursorX);
                int y = Math.round(virtualCursorY);
                child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
                continue;
            }
            if (drawerPlacements.containsKey(child)) continue;
            ControlData data = dataFor(child);
            int x = evaluatePosition(data.dynamicX, data, width, height, true);
            int y = evaluatePosition(data.dynamicY, data, width, height, false);
            x = Math.max(0, Math.min(x, width - child.getMeasuredWidth()));
            y = Math.max(0, Math.min(y, height - child.getMeasuredHeight()));
            child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
        }
        for (Map.Entry<View, DrawerPlacement> entry : drawerPlacements.entrySet()) {
            layoutDrawerChild(entry.getKey(), entry.getValue(), width, height);
        }
        updateVisibility();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        updateVisibility();
        super.dispatchDraw(canvas);
        postInvalidateDelayed(250);
    }

    private float cameraSensitivity() {
        int height = getResources().getDisplayMetrics().heightPixels;
        return height > 0 ? 1.4f * 1080f / height : 1.4f;
    }

    private void setVirtualMouse(boolean enabled) {
        if (virtualMouse == enabled) return;
        runtimeSurface.release();
        releaseVirtualTouch();
        virtualMouse = enabled;
        if (enabled) {
            if (Float.isNaN(virtualCursorX) || Float.isNaN(virtualCursorY)) {
                virtualCursorX = getWidth() / 2f;
                virtualCursorY = getHeight() / 2f;
            }
            clampVirtualCursor();
        }
        cursorView.setVisibility(enabled ? VISIBLE : GONE);
        updateVirtualMouseButtons();
        requestLayout();
    }

    private void moveVirtualCursor(float deltaX, float deltaY) {
        if (Float.isNaN(virtualCursorX) || Float.isNaN(virtualCursorY)) {
            virtualCursorX = getWidth() / 2f;
            virtualCursorY = getHeight() / 2f;
        }
        virtualCursorX += deltaX;
        virtualCursorY += deltaY;
        clampVirtualCursor();
        int x = Math.round(virtualCursorX);
        int y = Math.round(virtualCursorY);
        cursorView.layout(x, y, x + cursorView.getMeasuredWidth(), y + cursorView.getMeasuredHeight());
        cursorView.invalidate();
    }

    private void clampVirtualCursor() {
        virtualCursorX = Math.max(0f, Math.min(virtualCursorX, Math.max(0, getWidth() - 1)));
        virtualCursorY = Math.max(0f, Math.min(virtualCursorY, Math.max(0, getHeight() - 1)));
    }

    private void updateVirtualMouseButtons() {
        for (RuntimeButton button : buttons) button.setVirtualMouseState(virtualMouse);
    }

    private final class RuntimeSurface extends View {
        private int cameraPointer = -1;
        private float cameraX;
        private float cameraY;
        private float cameraDownX;
        private float cameraDownY;
        private long cameraDownAt;
        private boolean cameraMoved;

        RuntimeSurface(Context context) {
            super(context);
            setClickable(true);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            int actionIndex = event.getActionIndex();
            if (!virtualMouse) {
                host.pojavSendTouch(event);
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) release();
                return true;
            }
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                if (cameraPointer == -1) {
                    cameraPointer = event.getPointerId(actionIndex);
                    cameraX = event.getX(actionIndex);
                    cameraY = event.getY(actionIndex);
                    cameraDownX = cameraX;
                    cameraDownY = cameraY;
                    cameraDownAt = SystemClock.uptimeMillis();
                    cameraMoved = false;
                }
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE && cameraPointer != -1) {
                int pointerIndex = event.findPointerIndex(cameraPointer);
                if (pointerIndex >= 0) {
                    float x = event.getX(pointerIndex);
                    float y = event.getY(pointerIndex);
                    float downDeltaX = x - cameraDownX;
                    float downDeltaY = y - cameraDownY;
                    float threshold = 9f * getResources().getDisplayMetrics().density;
                    if (downDeltaX * downDeltaX + downDeltaY * downDeltaY > threshold * threshold) {
                        cameraMoved = true;
                    }
                    moveVirtualCursor(x - cameraX, y - cameraY);
                    cameraX = x;
                    cameraY = y;
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL ||
                    (action == MotionEvent.ACTION_POINTER_UP &&
                            event.getPointerId(actionIndex) == cameraPointer)) {
                if (action != MotionEvent.ACTION_CANCEL && !cameraMoved) {
                    long elapsed = SystemClock.uptimeMillis() - cameraDownAt;
                    if (elapsed < 200L) sendVirtualTouchTap(event.getEventTime());
                }
                release();
                return true;
            }
            return true;
        }

        void release() {
            cameraPointer = -1;
            cameraMoved = false;
        }
    }

    private void sendVirtualTouchTap(long eventTime) {
        sendVirtualTouch(MotionEvent.ACTION_DOWN, eventTime, eventTime);
        sendVirtualTouch(MotionEvent.ACTION_UP, eventTime, eventTime + 10L);
    }

    private void setVirtualTouchPressed(boolean down) {
        long now = SystemClock.uptimeMillis();
        if (down) {
            if (virtualTouchDown) return;
            virtualTouchDown = true;
            virtualTouchDownAt = now;
            sendVirtualTouch(MotionEvent.ACTION_DOWN, now, now);
        } else {
            releaseVirtualTouch();
        }
    }

    private void releaseVirtualTouch() {
        if (!virtualTouchDown) return;
        long now = SystemClock.uptimeMillis();
        sendVirtualTouch(MotionEvent.ACTION_UP, virtualTouchDownAt, now);
        virtualTouchDown = false;
    }

    private void sendVirtualTouch(int action, long downTime, long eventTime) {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action,
                virtualCursorX, virtualCursorY, 0);
        event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
        host.pojavSendTouch(event);
        event.recycle();
    }

    private void addRuntimeButton(ControlData data) {
        RuntimeButton button = new RuntimeButton(getContext(), data, host, this::handleSpecialAction);
        buttons.add(button);
        addView(button);
    }

    private void addDrawer(ControlDrawerData data) {
        RuntimeButton pull = new RuntimeButton(getContext(), data.properties, host, this::handleSpecialAction);
        DrawerRuntime runtime = new DrawerRuntime(data, pull);
        pull.setOnClickListener(view -> {
            runtime.open = !runtime.open;
            updateVisibility();
        });
        buttons.add(pull);
        addView(pull);
        for (int i = 0; i < data.buttonProperties.size(); i++) {
            RuntimeButton button = new RuntimeButton(getContext(), data.buttonProperties.get(i), host,
                    this::handleSpecialAction);
            runtime.children.add(button);
            buttons.add(button);
            drawerPlacements.put(button, new DrawerPlacement(runtime, i));
            addView(button);
        }
        drawers.add(runtime);
    }

    private void handleSpecialAction(int code, boolean down) {
        if (code == ControlData.SPECIALBTN_KEYBOARD && down) host.pojavShowKeyboard();
        else if (code == ControlData.SPECIALBTN_TOGGLECTRL && down) {
            controlsVisible = !controlsVisible;
            updateVisibility();
        } else if (code == ControlData.SPECIALBTN_VIRTUALMOUSE && down) setVirtualMouse(!virtualMouse);
        else if (code == ControlData.SPECIALBTN_MOUSEPRI && virtualMouse) {
            setVirtualTouchPressed(down);
        }
        else if (code == ControlData.SPECIALBTN_MOUSESEC && virtualMouse) {
            setVirtualTouchPressed(down);
        }
        else if (code == ControlData.SPECIALBTN_MOUSEPRI) host.pojavSendMouseButton(MotionEvent.BUTTON_PRIMARY, down);
        else if (code == ControlData.SPECIALBTN_MOUSESEC) host.pojavSendMouseButton(MotionEvent.BUTTON_SECONDARY, down);
        else if (code == ControlData.SPECIALBTN_MOUSEMID) host.pojavSendMouseButton(MotionEvent.BUTTON_TERTIARY, down);
        else if (code == ControlData.SPECIALBTN_SCROLLUP && !down) host.pojavSendScroll(1f);
        else if (code == ControlData.SPECIALBTN_SCROLLDOWN && !down) host.pojavSendScroll(-1f);
        else if (code == ControlData.SPECIALBTN_MENU && down) {
            host.pojavSendKey(KeyMapper.toBedrock(KeyMapper.GLFW_KEY_ESCAPE), true);
            host.pojavSendKey(KeyMapper.toBedrock(KeyMapper.GLFW_KEY_ESCAPE), false);
        }
    }

    private void updateVisibility() {
        boolean menu = host.pojavIsMenuOpen();
        for (RuntimeButton button : buttons) {
            boolean specialToggle = button.data.keycodes[0] == ControlData.SPECIALBTN_TOGGLECTRL;
            boolean visible = specialToggle || (controlsVisible && button.isVisibleForMode(menu));
            DrawerPlacement placement = drawerPlacements.get(button);
            if (placement != null) visible &= placement.runtime.open;
            button.setVisibility(visible ? VISIBLE : GONE);
        }
        for (RuntimeJoystick joystick : joysticks) {
            boolean visible = controlsVisible && joystick.isVisibleForMode(menu);
            joystick.setVisibility(visible ? VISIBLE : GONE);
            if (!visible) joystick.release();
        }
    }

    private void layoutDrawerChild(View child, DrawerPlacement placement, int screenWidth, int screenHeight) {
        DrawerRuntime runtime = placement.runtime;
        View pull = runtime.pull;
        int gap = Math.round(8 * getResources().getDisplayMetrics().density);
        int step = placement.index + 1;
        int x = pull.getLeft();
        int y = pull.getTop();
        switch (runtime.data.orientation) {
            case LEFT -> x -= step * (child.getMeasuredWidth() + gap);
            case RIGHT -> x += step * (child.getMeasuredWidth() + gap);
            case UP -> y -= step * (child.getMeasuredHeight() + gap);
            case DOWN -> y += step * (child.getMeasuredHeight() + gap);
            case FREE -> {
                ControlData data = dataFor(child);
                x = evaluatePosition(data.dynamicX, data, screenWidth, screenHeight, true);
                y = evaluatePosition(data.dynamicY, data, screenWidth, screenHeight, false);
            }
        }
        x = Math.max(0, Math.min(x, screenWidth - child.getMeasuredWidth()));
        y = Math.max(0, Math.min(y, screenHeight - child.getMeasuredHeight()));
        child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
    }

    private int evaluatePosition(String expression, ControlData data, int screenWidth, int screenHeight, boolean xAxis) {
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

    private ControlData dataFor(View child) {
        if (child instanceof RuntimeButton) return ((RuntimeButton) child).data;
        return ((RuntimeJoystick) child).data;
    }

    private void registerProfileReceiver() {
        IntentFilter filter = new IntentFilter(PojavControls.ACTION_PROFILE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) activity.registerReceiver(profileReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else activity.registerReceiver(profileReceiver, filter);
        receiverRegistered = true;
    }

    private static final class DrawerRuntime {
        final ControlDrawerData data;
        final RuntimeButton pull;
        final List<RuntimeButton> children = new ArrayList<>();
        boolean open;

        DrawerRuntime(ControlDrawerData data, RuntimeButton pull) {
            this.data = data;
            this.pull = pull;
        }
    }

    private static final class DrawerPlacement {
        final DrawerRuntime runtime;
        final int index;

        DrawerPlacement(DrawerRuntime runtime, int index) {
            this.runtime = runtime;
            this.index = index;
        }
    }

    private interface SpecialActionHandler {
        void handle(int code, boolean down);
    }

    private static final class VirtualMouseCursor extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path pointer = new Path();

        VirtualMouseCursor(Context context) {
            super(context);
            fill.setColor(Color.WHITE);
            fill.setStyle(Paint.Style.FILL);
            outline.setColor(Color.BLACK);
            outline.setStyle(Paint.Style.STROKE);
            outline.setStrokeWidth(2f * getResources().getDisplayMetrics().density);
            outline.setStrokeJoin(Paint.Join.ROUND);
            setClickable(false);
            setFocusable(false);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float density = getResources().getDisplayMetrics().density;
            pointer.reset();
            pointer.moveTo(2f * density, 2f * density);
            pointer.lineTo(2f * density, 29f * density);
            pointer.lineTo(9f * density, 22f * density);
            pointer.lineTo(15f * density, 34f * density);
            pointer.lineTo(21f * density, 31f * density);
            pointer.lineTo(15f * density, 19f * density);
            pointer.lineTo(26f * density, 18f * density);
            pointer.close();
            canvas.drawPath(pointer, fill);
            canvas.drawPath(pointer, outline);
        }
    }

    private static final class RuntimeButton extends TextView {
        final ControlData data;
        private final PojavControlsHost host;
        private final SpecialActionHandler specialHandler;
        private boolean pressed;
        private boolean toggled;
        private boolean outside;
        private boolean rawPassThrough;
        private boolean virtualMouseButton;
        private boolean virtualMouseActive;
        private float passThroughX;
        private float passThroughY;

        RuntimeButton(Context context, ControlData data, PojavControlsHost host,
                      SpecialActionHandler specialHandler) {
            super(context);
            this.data = data;
            this.host = host;
            this.specialHandler = specialHandler;
            setText(data.name);
            setGravity(Gravity.CENTER);
            setTextColor(Color.WHITE);
            setTextSize(13);
            setPadding(4, 4, 4, 4);
            setClickable(true);
            virtualMouseButton = data.primaryKeycode() == ControlData.SPECIALBTN_VIRTUALMOUSE;
            applyStyle();
        }

        boolean isVisibleForMode(boolean menu) {
            return menu ? data.displayInMenu : data.displayInGame;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                outside = false;
                passThroughX = event.getX();
                passThroughY = event.getY();
                rawPassThrough = data.passThruEnabled && host.pojavIsMenuOpen();
                if (rawPassThrough) sendTouchToGame(event);
                if (!data.isToggle || virtualMouseButton) press(true);
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                if (data.passThruEnabled) {
                    float x = event.getX();
                    float y = event.getY();
                    if (rawPassThrough) sendTouchToGame(event);
                    else {
                        float sensitivity = cameraSensitivity();
                        host.pojavSendLookDelta((x - passThroughX) * sensitivity,
                                (y - passThroughY) * sensitivity);
                    }
                    passThroughX = x;
                    passThroughY = y;
                }
                boolean nowOutside = event.getX() < 0 || event.getY() < 0 ||
                        event.getX() > getWidth() || event.getY() > getHeight();
                if (data.isSwipeable && nowOutside != outside &&
                        (!data.isToggle || virtualMouseButton)) press(!nowOutside);
                outside = nowOutside;
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                if (rawPassThrough) sendTouchToGame(event);
                if (data.isToggle && !virtualMouseButton && !outside) {
                    toggled = !toggled;
                    press(toggled);
                } else if (!data.isToggle || virtualMouseButton) press(false);
                if (!outside) performClick();
                outside = false;
                rawPassThrough = false;
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                if (rawPassThrough) sendTouchToGame(event);
                if (!data.isToggle || virtualMouseButton) press(false);
                outside = false;
                rawPassThrough = false;
                return true;
            }
            return true;
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        void release() {
            if (pressed) send(false);
            pressed = false;
            toggled = false;
            refreshState();
            setScaleX(1f);
            setScaleY(1f);
            rawPassThrough = false;
        }

        void setVirtualMouseState(boolean active) {
            if (!virtualMouseButton) return;
            virtualMouseActive = active;
            setText(data.name + (active ? " ON" : " OFF"));
            setTextColor(active ? 0xFF65F0B2 : Color.WHITE);
            refreshState();
        }

        private float cameraSensitivity() {
            int height = getResources().getDisplayMetrics().heightPixels;
            return height > 0 ? 1.4f * 1080f / height : 1.4f;
        }

        private void sendTouchToGame(MotionEvent event) {
            MotionEvent copy = MotionEvent.obtain(event);
            copy.offsetLocation(getLeft(), getTop());
            host.pojavSendTouch(copy);
            copy.recycle();
        }

        private void press(boolean down) {
            if (pressed == down) return;
            pressed = down;
            send(down);
            refreshState();
            setScaleX(down ? 0.94f : 1f);
            setScaleY(down ? 0.94f : 1f);
        }

        private void refreshState() {
            setActivated(pressed || virtualMouseActive);
        }

        private void send(boolean down) {
            int code = data.primaryKeycode();
            if (code < 0) specialHandler.handle(code, down);
            else if (code != KeyMapper.GLFW_KEY_UNKNOWN) {
                int bedrockCode = KeyMapper.toBedrock(code);
                if (bedrockCode != KeyMapper.GLFW_KEY_UNKNOWN) host.pojavSendKey(bedrockCode, down);
            }
        }

        private void applyStyle() {
            GradientDrawable background = new GradientDrawable();
            background.setColor(data.bgColor);
            float radius = Math.min(data.width, data.height) * getResources().getDisplayMetrics().density
                    * data.cornerRadius / 200f;
            background.setCornerRadius(radius);
            if (data.strokeWidth > 0f) {
                background.setStroke(Math.round(data.strokeWidth * getResources().getDisplayMetrics().density),
                        data.strokeColor);
            }
            setBackground(background);
            setAlpha(data.opacity);
        }
    }

    private static final class RuntimeJoystick extends View {
        final ControlJoystickData data;
        private final PojavControlsHost host;
        private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float centerX;
        private float centerY;
        private float knobX;
        private float knobY;
        private int pointer = -1;
        private int direction;
        private boolean forwardLocked;

        RuntimeJoystick(Context context, ControlJoystickData data, PojavControlsHost host) {
            super(context);
            this.data = data;
            this.host = host;
            basePaint.setColor(data.bgColor);
            basePaint.setStyle(Paint.Style.FILL);
            knobPaint.setColor(data.strokeColor);
            knobPaint.setAlpha(180);
            setAlpha(data.opacity);
        }

        boolean isVisibleForMode(boolean menu) {
            return menu ? data.displayInMenu : data.displayInGame;
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            centerX = knobX = w / 2f;
            centerY = knobY = h / 2f;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float radius = Math.min(getWidth(), getHeight()) * 0.47f;
            canvas.drawCircle(centerX, centerY, radius, basePaint);
            canvas.drawCircle(knobX, knobY, radius * 0.42f, knobPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            int actionIndex = event.getActionIndex();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                if (pointer == -1) {
                    pointer = event.getPointerId(actionIndex);
                    if (data.absolute) {
                        centerX = event.getX(actionIndex);
                        centerY = event.getY(actionIndex);
                    }
                    update(event.getX(actionIndex), event.getY(actionIndex));
                }
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE && pointer != -1) {
                int index = event.findPointerIndex(pointer);
                if (index >= 0) update(event.getX(index), event.getY(index));
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP ||
                    (action == MotionEvent.ACTION_POINTER_UP && event.getPointerId(actionIndex) == pointer)) {
                release();
                return true;
            }
            return true;
        }

        void release() {
            setDirection(0);
            setForwardLocked(false);
            pointer = -1;
            centerX = knobX = getWidth() / 2f;
            centerY = knobY = getHeight() / 2f;
            invalidate();
        }

        private void update(float x, float y) {
            float max = Math.min(getWidth(), getHeight()) * 0.38f;
            float dx = x - centerX;
            float dy = y - centerY;
            float distance = (float) Math.hypot(dx, dy);
            float factor = distance > max ? max / distance : 1f;
            knobX = centerX + dx * factor;
            knobY = centerY + dy * factor;
            float strength = max == 0 ? 0 : Math.min(1f, distance / max);
            if (strength < 0.28f) setDirection(0);
            else {
                double angle = Math.atan2(-dy, dx);
                int octant = (int) Math.round(angle / (Math.PI / 4));
                if (octant < 0) octant += 8;
                setDirection(octant + 1);
            }
            setForwardLocked(data.forwardLock && strength > 0.9f && direction == 3);
            invalidate();
        }

        private void setDirection(int next) {
            if (direction == next) return;
            sendDirection(direction, false);
            direction = next;
            sendDirection(direction, true);
        }

        private void sendDirection(int value, boolean down) {
            switch (value) {
                case 1 -> send(KeyMapper.GLFW_KEY_D, down);
                case 2 -> { send(KeyMapper.GLFW_KEY_D, down); send(KeyMapper.GLFW_KEY_W, down); }
                case 3 -> send(KeyMapper.GLFW_KEY_W, down);
                case 4 -> { send(KeyMapper.GLFW_KEY_W, down); send(KeyMapper.GLFW_KEY_A, down); }
                case 5 -> send(KeyMapper.GLFW_KEY_A, down);
                case 6 -> { send(KeyMapper.GLFW_KEY_A, down); send(KeyMapper.GLFW_KEY_S, down); }
                case 7 -> send(KeyMapper.GLFW_KEY_S, down);
                case 8 -> { send(KeyMapper.GLFW_KEY_S, down); send(KeyMapper.GLFW_KEY_D, down); }
            }
        }

        private void setForwardLocked(boolean value) {
            if (forwardLocked == value) return;
            forwardLocked = value;
            send(KeyMapper.GLFW_KEY_LEFT_CONTROL, value);
        }

        private void send(int glfw, boolean down) {
            host.pojavSendKey(KeyMapper.toBedrock(glfw), down);
        }
    }
}

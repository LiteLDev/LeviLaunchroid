package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.levimc.launcher.core.mods.inbuilt.ExternalModBridge;
import org.levimc.launcher.core.mods.inbuilt.ExternalModBridge.DrawCommand;

public class HudOverlay extends View {
    private boolean isShowing = false;
    private final Paint paint = new Paint();

    public HudOverlay(Activity activity) {
        super(activity);
        paint.setAntiAlias(true);
        setWillNotDraw(false);
        setElevation(100f);
    }

    private final android.view.Choreographer.FrameCallback frameCallback = new android.view.Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (isShowing) {
                invalidate();
                android.view.Choreographer.getInstance().postFrameCallback(this);
            }
        }
    };

    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

    public void show() {
        if (isShowing) return;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isShowing) return;
                
                android.os.IBinder token = ((Activity) getContext()).getWindow().getDecorView().getWindowToken();
                if (token == null) {
                    handler.postDelayed(this, 100);
                    return;
                }

                WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT
                );
                wmParams.token = token;
                wmParams.gravity = android.view.Gravity.TOP | android.view.Gravity.START;

                try {
                    WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                    windowManager.addView(HudOverlay.this, wmParams);
                    isShowing = true;
                    android.view.Choreographer.getInstance().postFrameCallback(frameCallback);
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        ViewGroup rootView = ((Activity) getContext()).findViewById(android.R.id.content);
                        if (rootView != null) {
                            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                            );
                            rootView.addView(HudOverlay.this, params);
                            isShowing = true;
                            android.view.Choreographer.getInstance().postFrameCallback(frameCallback);
                        }
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }
            }
        }, 100);
    }

    public void hide() {
        if (!isShowing) return;
        try {
            WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            windowManager.removeView(this);
        } catch (Exception e) {
            try {
                ViewGroup rootView = ((Activity) getContext()).findViewById(android.R.id.content);
                if (rootView != null) {
                    rootView.removeView(this);
                }
            } catch (Exception e2) {}
        }
        isShowing = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isShowing) return;

        DrawCommand[] cmds = ExternalModBridge.getDrawCommands();
        if (cmds != null) {
            for (DrawCommand cmd : cmds) {
                paint.setColor(cmd.color);
                
                if (cmd.type == DrawCommand.TYPE_TEXT) {
                    paint.setTextSize(cmd.size);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setShadowLayer(3f, 1f, 1f, 0xFF000000);
                    if (cmd.text != null) {
                        canvas.drawText(cmd.text, cmd.x, cmd.y, paint);
                    }
                    paint.clearShadowLayer();
                } else if (cmd.type == DrawCommand.TYPE_RECT) {
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRect(cmd.x, cmd.y, cmd.x + cmd.w, cmd.y + cmd.h, paint);
                } else if (cmd.type == DrawCommand.TYPE_LINE) {
                    paint.setStrokeWidth(cmd.size);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawLine(cmd.x, cmd.y, cmd.x + cmd.w, cmd.y + cmd.h, paint);
                }
            }
        }
    }
}

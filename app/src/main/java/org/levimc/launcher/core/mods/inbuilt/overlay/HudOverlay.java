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

                wmParams = new WindowManager.LayoutParams(
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

    private boolean isHudEditorMode = false;
    private WindowManager.LayoutParams wmParams;
    
    private String draggingModule = null;
    private float dragOffsetX = 0;
    private float dragOffsetY = 0;

    public void setHudEditorMode(boolean active) {
        isHudEditorMode = active;
        if (wmParams != null && isShowing) {
            if (active) {
                wmParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            } else {
                wmParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            }
            try {
                WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                windowManager.updateViewLayout(this, wmParams);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        invalidate();
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (!isHudEditorMode) return false;

        switch (event.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
                DrawCommand[] cmds = ExternalModBridge.getDrawCommands();
                if (cmds != null) {
                    for (DrawCommand cmd : cmds) {
                        if (cmd.moduleId != null) {
                            if (cmd.type == DrawCommand.TYPE_LINE || cmd.type == DrawCommand.TYPE_CIRCLE_FILLED || cmd.type == DrawCommand.TYPE_TRIANGLE_FILLED) {
                                continue;
                            }
                            if (cmd.type == DrawCommand.TYPE_TEXT) {
                                paint.setTextSize(cmd.size);
                            }
                            float w = cmd.w > 0 ? cmd.w : (cmd.text != null ? paint.measureText(cmd.text) : 100);
                            float h = cmd.h > 0 ? cmd.h : (cmd.size > 0 ? cmd.size : 30);
                            if (event.getRawX() >= cmd.x && event.getRawX() <= cmd.x + w &&
                                event.getRawY() >= cmd.y && event.getRawY() <= cmd.y + h) {
                                draggingModule = cmd.moduleId;
                                
                                float minX = Float.MAX_VALUE;
                                float minY = Float.MAX_VALUE;
                                for (DrawCommand c : cmds) {
                                    if (draggingModule.equals(c.moduleId)) {
                                        if (c.type == DrawCommand.TYPE_LINE || c.type == DrawCommand.TYPE_CIRCLE_FILLED || c.type == DrawCommand.TYPE_TRIANGLE_FILLED) {
                                            continue;
                                        }
                                        if (c.x < minX) minX = c.x;
                                        if (c.y < minY) minY = c.y;
                                    }
                                }
                                
                                dragOffsetX = event.getRawX() - minX;
                                dragOffsetY = event.getRawY() - minY;
                                return true;
                            }
                        }
                    }
                }
                break;
            case android.view.MotionEvent.ACTION_MOVE:
                if (draggingModule != null) {
                    float newX = event.getRawX() - dragOffsetX;
                    float newY = event.getRawY() - dragOffsetY;
                    ExternalModBridge.setExternalModConfig(draggingModule, "hudPosX", String.valueOf((int)newX));
                    ExternalModBridge.setExternalModConfig(draggingModule, "hudPosY", String.valueOf((int)newY));
                    return true;
                }
                break;
            case android.view.MotionEvent.ACTION_UP:
            case android.view.MotionEvent.ACTION_CANCEL:
                draggingModule = null;
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isShowing) return;

        if (isHudEditorMode) {
            canvas.drawColor(0x88000000);
        }

        DrawCommand[] cmds = ExternalModBridge.getDrawCommands();
        if (cmds != null) {
            for (DrawCommand cmd : cmds) {
                paint.setColor(cmd.color);
                
                if (cmd.type == DrawCommand.TYPE_TEXT) {
                    paint.setTextSize(cmd.size);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setShadowLayer(3f, 1f, 1f, 0xFF000000);
                    if (cmd.text != null) {
                        if (cmd.w > 0 && cmd.h > 0) {
                            paint.setTextAlign(Paint.Align.CENTER);
                            float textY = cmd.y + (cmd.h / 2f) - ((paint.descent() + paint.ascent()) / 2f);
                            canvas.drawText(cmd.text, cmd.x + (cmd.w / 2f), textY, paint);
                        } else {
                            paint.setTextAlign(Paint.Align.LEFT);
                            canvas.drawText(cmd.text, cmd.x, cmd.y - paint.ascent(), paint);
                        }
                    }
                    paint.clearShadowLayer();
                } else if (cmd.type == DrawCommand.TYPE_RECT) {
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawRect(cmd.x, cmd.y, cmd.x + cmd.w, cmd.y + cmd.h, paint);
                } else if (cmd.type == DrawCommand.TYPE_RECT_FILLED) {
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRect(cmd.x, cmd.y, cmd.x + cmd.w, cmd.y + cmd.h, paint);
                } else if (cmd.type == DrawCommand.TYPE_LINE) {
                    paint.setStrokeWidth(cmd.size);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawLine(cmd.x, cmd.y, cmd.x + cmd.w, cmd.y + cmd.h, paint);
                } else if (cmd.type == DrawCommand.TYPE_CIRCLE_FILLED) {
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(cmd.x, cmd.y, cmd.size, paint);
                } else if (cmd.type == DrawCommand.TYPE_TRIANGLE_FILLED) {
                    paint.setStyle(Paint.Style.FILL);
                    android.graphics.Path path = new android.graphics.Path();
                    path.moveTo(cmd.x, cmd.y);
                    path.lineTo(cmd.w, cmd.h);
                    path.lineTo(cmd.x3, cmd.y3);
                    path.close();
                    canvas.drawPath(path, paint);
                }

                if (isHudEditorMode && cmd.moduleId != null) {
                    if (cmd.type == DrawCommand.TYPE_LINE || cmd.type == DrawCommand.TYPE_CIRCLE_FILLED || cmd.type == DrawCommand.TYPE_TRIANGLE_FILLED) {
                        continue;
                    }
                    if (cmd.type == DrawCommand.TYPE_TEXT) {
                        paint.setTextSize(cmd.size);
                    }
                    float w = cmd.w > 0 ? cmd.w : (cmd.text != null ? paint.measureText(cmd.text) : 100);
                    float h = cmd.h > 0 ? cmd.h : (cmd.size > 0 ? cmd.size : 30);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2f);
                    paint.setColor(0xFF4AE0A0);
                    canvas.drawRect(cmd.x - 2, cmd.y - 2, cmd.x + w + 2, cmd.y + h + 2, paint);
                }
            }
        }
    }
}

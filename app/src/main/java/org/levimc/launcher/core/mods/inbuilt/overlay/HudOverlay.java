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
    private final java.util.Map<String, android.graphics.Typeface> typefaceCache = new java.util.HashMap<>();

    private android.graphics.Typeface getFont(String fontId) {
        if (fontId == null || fontId.isEmpty()) return null;
        if (typefaceCache.containsKey(fontId)) return typefaceCache.get(fontId);
        
        byte[] fontBytes = ExternalModBridge.nativeGetRegisteredFontBytes(fontId);
        if (fontBytes != null && fontBytes.length > 0) {
            try {
                java.io.File tempFile = java.io.File.createTempFile("font_" + fontId, ".ttf", getContext().getCacheDir());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
                fos.write(fontBytes);
                fos.close();
                android.graphics.Typeface tf = android.graphics.Typeface.createFromFile(tempFile);
                typefaceCache.put(fontId, tf);
                return tf;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        typefaceCache.put(fontId, null);
        return null;
    }

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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 100);
    }

    public void hide() {
        if (!isShowing) return;
        try {
            ViewGroup rootView = ((Activity) getContext()).findViewById(android.R.id.content);
            if (rootView != null) {
                rootView.removeView(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        isShowing = false;
    }

    private boolean isHudEditorMode = false;
    private WindowManager.LayoutParams wmParams;
    
    private String draggingModule = null;
    private float dragOffsetX = 0f;
    private float dragOffsetY = 0f;

    private java.util.Map<String, Boolean> hiddenInHudCache = new java.util.HashMap<>();

    private boolean isHiddenInHudEditor(String moduleId) {
        if (moduleId == null) return false;
        if (hiddenInHudCache.containsKey(moduleId)) {
            return hiddenInHudCache.get(moduleId);
        }
        boolean hidden = false;
        int extCount = ExternalModBridge.getExternalModCount();
        for (int i = 0; i < extCount; i++) {
            String json = ExternalModBridge.getExternalModInfo(i);
            try {
                org.json.JSONObject obj = new org.json.JSONObject(json);
                if (moduleId.equals(obj.optString("module_id", ""))) {
                    hidden = obj.optBoolean("hide_in_hud_editor", false);
                    break;
                }
            } catch (Exception e) {}
        }
        hiddenInHudCache.put(moduleId, hidden);
        return hidden;
    }

    public void setHudEditorMode(boolean active) {
        isHudEditorMode = active;
        hiddenInHudCache.clear();
        invalidate();
    }

    public boolean isHudEditorMode() {
        return isHudEditorMode;
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
                            if (isHiddenInHudEditor(cmd.moduleId)) {
                                continue;
                            }
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
        return true;
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
                if (isHudEditorMode && isHiddenInHudEditor(cmd.moduleId)) continue;
                paint.setColor(cmd.color);
                
                float drawX = cmd.x;
                if (drawX <= -19000f) drawX = (getWidth() / 2f) + (drawX + 20000f);
                else if (drawX <= -9000f) drawX = getWidth() + (drawX + 10000f);
                
                float drawY = cmd.y;
                if (drawY <= -19000f) drawY = (getHeight() / 2f) + (drawY + 20000f);
                else if (drawY <= -9000f) drawY = getHeight() + (drawY + 10000f);

                if (isHudEditorMode && dragOffsetX != 0 && cmd.moduleId != null && cmd.moduleId.equals(draggingModule)) {
                    drawX += dragOffsetX;
                    drawY += dragOffsetY;
                }

                if (cmd.type == DrawCommand.TYPE_TEXT) {
                    android.graphics.Typeface tf = getFont(cmd.fontId);
                    if (tf != null) {
                        paint.setTypeface(tf);
                    } else {
                        paint.setTypeface(android.graphics.Typeface.DEFAULT);
                    }
                    paint.setTextSize(cmd.size);
                    paint.setStyle(Paint.Style.FILL);
                    
                    String txt = cmd.text;
                    if (txt != null && txt.contains("{DISPLAY_SIZE}")) {
                        txt = txt.replace("{DISPLAY_SIZE}", getWidth() + "x" + getHeight());
                    }
                    
                    int bgColor = Float.floatToRawIntBits(cmd.x3);
                    if (bgColor != 0 && txt != null && !txt.isEmpty()) {
                        float textWidth = paint.measureText(txt);
                        float paddingX = 4f;
                        float left = drawX;
                        if (cmd.w == -1f) left = drawX - textWidth;
                        else if (cmd.w == -2f) left = drawX - textWidth / 2f;

                        float top = drawY - cmd.size * 0.9f; 
                        float right = left + textWidth;
                        float bottom = drawY + (cmd.size + 4f - cmd.size * 0.9f); 
                        
                        int oldColor = paint.getColor();
                        paint.setColor(bgColor);
                        canvas.drawRect(left - paddingX, top, right + paddingX, bottom, paint);
                        paint.setColor(oldColor);
                    }

                    if (txt != null) {
                        paint.setShadowLayer(3f, 1f, 1f, 0xFF000000);
                        if (cmd.w > 0 && cmd.h > 0) {
                            paint.setTextAlign(Paint.Align.CENTER);
                            float textY = drawY + (cmd.h / 2f) - ((paint.descent() + paint.ascent()) / 2f);
                            canvas.drawText(txt, drawX + (cmd.w / 2f), textY, paint);
                        } else {
                            if (cmd.w == 0f) paint.setTextAlign(Paint.Align.LEFT);
                            else if (cmd.w == -1f) paint.setTextAlign(Paint.Align.RIGHT);
                            else if (cmd.w == -2f) paint.setTextAlign(Paint.Align.CENTER);
                            
                            canvas.drawText(txt, drawX, drawY, paint);
                        }
                        paint.clearShadowLayer();
                    }
                } else if (cmd.type == DrawCommand.TYPE_RECT) {
                    paint.setStyle(Paint.Style.STROKE);
                    if (cmd.x3 > 0) {
                        canvas.drawRoundRect(drawX, drawY, drawX + cmd.w, drawY + cmd.h, cmd.x3, cmd.x3, paint);
                    } else {
                        canvas.drawRect(drawX, drawY, drawX + cmd.w, drawY + cmd.h, paint);
                    }
                } else if (cmd.type == DrawCommand.TYPE_RECT_FILLED) {
                    paint.setStyle(Paint.Style.FILL);
                    if (cmd.x3 > 0) {
                        canvas.drawRoundRect(drawX, drawY, drawX + cmd.w, drawY + cmd.h, cmd.x3, cmd.x3, paint);
                    } else {
                        canvas.drawRect(drawX, drawY, drawX + cmd.w, drawY + cmd.h, paint);
                    }
                } else if (cmd.type == DrawCommand.TYPE_LINE) {
                    paint.setStrokeWidth(cmd.size);
                    paint.setStyle(Paint.Style.STROKE);
                    float endX = cmd.x + cmd.w;
                    if (endX <= -19000f) endX = (getWidth() / 2f) + (endX + 20000f);
                    else if (endX <= -9000f) endX = getWidth() + (endX + 10000f);
                    float endY = cmd.y + cmd.h;
                    if (endY <= -19000f) endY = (getHeight() / 2f) + (endY + 20000f);
                    else if (endY <= -9000f) endY = getHeight() + (endY + 10000f);
                    canvas.drawLine(drawX, drawY, endX, endY, paint);
                } else if (cmd.type == DrawCommand.TYPE_CIRCLE_FILLED) {
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(drawX, drawY, cmd.size, paint);
                } else if (cmd.type == DrawCommand.TYPE_TRIANGLE_FILLED) {
                    paint.setStyle(Paint.Style.FILL);
                    android.graphics.Path path = new android.graphics.Path();
                    path.moveTo(drawX, drawY);
                    
                    float pt2X = cmd.w;
                    if (pt2X <= -19000f) pt2X = (getWidth() / 2f) + (pt2X + 20000f);
                    else if (pt2X <= -9000f) pt2X = getWidth() + (pt2X + 10000f);
                    float pt2Y = cmd.h;
                    if (pt2Y <= -19000f) pt2Y = (getHeight() / 2f) + (pt2Y + 20000f);
                    else if (pt2Y <= -9000f) pt2Y = getHeight() + (pt2Y + 10000f);
                    path.lineTo(pt2X, pt2Y);
                    
                    float pt3X = cmd.x3;
                    if (pt3X <= -19000f) pt3X = (getWidth() / 2f) + (pt3X + 20000f);
                    else if (pt3X <= -9000f) pt3X = getWidth() + (pt3X + 10000f);
                    float pt3Y = cmd.y3;
                    if (pt3Y <= -19000f) pt3Y = (getHeight() / 2f) + (pt3Y + 20000f);
                    else if (pt3Y <= -9000f) pt3Y = getHeight() + (pt3Y + 10000f);
                    path.lineTo(pt3X, pt3Y);
                    
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

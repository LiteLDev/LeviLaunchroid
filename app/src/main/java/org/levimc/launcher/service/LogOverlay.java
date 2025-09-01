package org.levimc.launcher.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import org.levimc.launcher.R;
import org.levimc.launcher.service.imgui.GIViewWrapper;
import org.levimc.launcher.service.imgui.NativeMethods;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogOverlay {
    private static LogOverlay instance;
    private final Context context;
    private final int APP_PID;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static View touchView;
    private static GIViewWrapper drawView;
    private static WindowManager winMrg;

    public static synchronized LogOverlay getInstance(Context context) {
        if (instance == null) instance = new LogOverlay(context);
        return instance;
    }

    private LogOverlay(Context ctx) {
        context = ctx.getApplicationContext();
        APP_PID = android.os.Process.myPid();
        touchView = new View(context);
        drawView = new GIViewWrapper(context);
        winMrg = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        initOverlay();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initOverlay() {
        WindowManager.LayoutParams touchParams = GetLayoutParams(false);
        touchView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            switch (action) {
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_UP:
                    NativeMethods.MotionEventClick(action != MotionEvent.ACTION_UP,
                            motionEvent.getRawX(), motionEvent.getRawY());
                    break;
                default:
                    break;
            }
            return false;
        });

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] rect = NativeMethods.GetWindowRect().split("\\|");
                    touchParams.x = Integer.parseInt(rect[0]);
                    touchParams.y = Integer.parseInt(rect[1]);
                    touchParams.width = Integer.parseInt(rect[2]);
                    touchParams.height = Integer.parseInt(rect[3]);
                    if (touchView.getParent() != null) {
                        winMrg.updateViewLayout(touchView, touchParams);
                    }
                } catch (Exception ignored) {
                }
                handler.postDelayed(this, 20);
            }
        }, 1000);

        startLogging();
    }

    private static WindowManager.LayoutParams GetLayoutParams(boolean isDrawLayout) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        if (isDrawLayout) {
            params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }

        params.gravity = Gravity.LEFT | Gravity.TOP;
        params.format = PixelFormat.RGBA_8888;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        params.x = params.y = 0;
        params.width = params.height = WindowManager.LayoutParams.MATCH_PARENT;

        return params;
    }

    public void show() {
        NativeMethods.SetOpen(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(context)) {
            Toast.makeText(context,
                    context.getString(R.string.overlay_permission_denied_message),
                    Toast.LENGTH_LONG).show();

            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return;
        }
        
        if (touchView.getParent() == null) {
            WindowManager.LayoutParams touchParams = GetLayoutParams(false);
            winMrg.addView(touchView, touchParams);
            winMrg.addView(drawView, GetLayoutParams(true));
        }
    }

    public void hide() {
        try {
            NativeMethods.SetOpen(false);
            if (touchView.getParent() != null) {
                winMrg.removeView(drawView);
                winMrg.removeView(touchView);
            }
        } catch (Exception ignored) {
        }
    }

    private void startLogging() {
        executor.execute(() -> {
            try {
                Process process = Runtime.getRuntime().exec("logcat --pid=" + APP_PID);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("LeviLogger")) {
                        String[] parts = line.split("\\s+", 6);
                        if (parts.length >= 6) {
                            String timestamp = parts[0] + " " + parts[1];
                            String level = parts[4];
                            String tag = parts[5].split(":")[0];
                            String message = line.substring(line.indexOf(tag) + tag.length() + 1).trim();

                            final String formattedLog =
                                    String.format("%s %s %s", timestamp, level, message);

                            mainHandler.post(() -> NativeMethods.AddLog(formattedLog + "\n"));
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }
}
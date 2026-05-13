package org.levimc.launcher.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import org.levimc.launcher.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PersonalizationManager {
    private static final String PREFS_NAME = "personalization_prefs";
    private static final String KEY_ACCENT_COLOR = "accent_color";
    private static final String KEY_BG_IMAGE_PATH = "bg_image_path";
    private static final String KEY_BASE_MODE = "base_mode";

    public static final int BASE_MODE_NONE = 0;
    public static final int BASE_MODE_FOLLOW_THEME = 1;
    public static final int BASE_MODE_CUSTOM = 2;
    private static final String KEY_BASE_CUSTOM_COLOR = "base_custom_color";

    private static int sChangeGeneration = 0;

    private final SharedPreferences prefs;
    private final Context context;

    private static final int GLASS_ALPHA_DARK = 50;
    private static final int GLASS_R_DARK = 25;
    private static final int GLASS_G_DARK = 25;
    private static final int GLASS_B_DARK = 25;

    private static final int GLASS_ALPHA_LIGHT = 50;
    private static final int GLASS_R_LIGHT = 255;
    private static final int GLASS_G_LIGHT = 255;
    private static final int GLASS_B_LIGHT = 255;

    public PersonalizationManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getAccentColor() {
        return prefs.getInt(KEY_ACCENT_COLOR, PRESET_COLORS[0]);
    }

    public void setAccentColor(int color) {
        prefs.edit().putInt(KEY_ACCENT_COLOR, color).apply();
        sChangeGeneration++;
    }

    public boolean hasCustomAccent() {
        return prefs.contains(KEY_ACCENT_COLOR) && prefs.getInt(KEY_ACCENT_COLOR, 0) != 0;
    }

    public void clearAccentColor() {
        prefs.edit().remove(KEY_ACCENT_COLOR).apply();
        sChangeGeneration++;
    }

    public String getBackgroundImagePath() {
        return prefs.getString(KEY_BG_IMAGE_PATH, null);
    }

    public boolean hasBackgroundImage() {
        String path = getBackgroundImagePath();
        if (path == null) return false;
        return new File(path).exists();
    }

    public void setBackgroundImage(Uri sourceUri, Context activityContext) {
        try {
            File destDir = new File(context.getFilesDir(), "personalization");
            if (!destDir.exists()) destDir.mkdirs();
            File destFile = new File(destDir, "background.jpg");

            InputStream is = activityContext.getContentResolver().openInputStream(sourceUri);
            if (is == null) return;

            Bitmap original = BitmapFactory.decodeStream(is);
            is.close();
            if (original == null) return;

            int maxDim = 2048;
            float scale = Math.min((float) maxDim / original.getWidth(), (float) maxDim / original.getHeight());
            if (scale < 1f) {
                Bitmap scaled = Bitmap.createScaledBitmap(original,
                        (int) (original.getWidth() * scale),
                        (int) (original.getHeight() * scale), true);
                original.recycle();
                original = scaled;
            }

            FileOutputStream fos = new FileOutputStream(destFile);
            original.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.close();
            original.recycle();

            prefs.edit().putString(KEY_BG_IMAGE_PATH, destFile.getAbsolutePath()).apply();
            sChangeGeneration++;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearBackgroundImage() {
        String path = getBackgroundImagePath();
        if (path != null) {
            File f = new File(path);
            if (f.exists()) f.delete();
        }
        prefs.edit().remove(KEY_BG_IMAGE_PATH).apply();
        sChangeGeneration++;
    }

    public Bitmap loadBackgroundBitmap() {
        String path = getBackgroundImagePath();
        if (path == null) return null;
        File f = new File(path);
        if (!f.exists()) return null;
        try {
            return BitmapFactory.decodeFile(path);
        } catch (Exception e) {
            return null;
        }
    }

    public int getBaseMode() {
        return prefs.getInt(KEY_BASE_MODE, BASE_MODE_NONE);
    }

    public void setBaseMode(int mode) {
        prefs.edit().putInt(KEY_BASE_MODE, mode).apply();
        sChangeGeneration++;
    }

    public int getBaseCustomColor() {
        return prefs.getInt(KEY_BASE_CUSTOM_COLOR, Color.BLACK);
    }

    public void setBaseCustomColor(int color) {
        prefs.edit().putInt(KEY_BASE_CUSTOM_COLOR, color).apply();
        sChangeGeneration++;
    }

    public static int getChangeGeneration() {
        return sChangeGeneration;
    }

    public void applyToActivity(Activity activity) {
        if (activity instanceof org.levimc.launcher.ui.activities.SplashActivity) return;

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;

        int accent = getAccentColor();
        boolean hasBg = hasBackgroundImage();

        if (hasBg) {
            applyBackgroundImage(activity, rootView);
        }

        if (accent != 0) {
            applyAccentColorRecursive(rootView, accent, activity);
            applyNavBarAccent(activity, accent);
        }
    }

    private void applyNavBarAccent(Activity activity, int accent) {
        TextView appName = activity.findViewById(R.id.nav_app_name);
        if (appName != null) {
            applySubtleWhiteGradient(appName, accent, 0.35f, false);
        }

        View signInBtn = activity.findViewById(R.id.nav_sign_in_button);
        if (signInBtn instanceof Button) {
            Button btn = (Button) signInBtn;
            btn.setBackgroundTintList(ColorStateList.valueOf(accent));
            btn.setTextColor(Color.WHITE);
        }
    }

    public void applySubtleWhiteGradient(TextView textView, int accentColor, float whiteRatio, boolean whiteOnLeft) {
        textView.post(() -> {
            String text = textView.getText().toString();
            float textWidth = textView.getPaint().measureText(text);
            if (textWidth <= 0) textWidth = 1f;

            int blendedWhite = blendColors(accentColor, Color.WHITE, whiteRatio);

            int leftColor = whiteOnLeft ? blendedWhite : accentColor;
            int rightColor = whiteOnLeft ? accentColor : blendedWhite;

            Shader shader = new LinearGradient(
                    0, 0, textWidth, 0,
                    new int[]{leftColor, rightColor},
                    new float[]{0f, 1f},
                    Shader.TileMode.CLAMP
            );
            textView.getPaint().setShader(shader);
            textView.invalidate();
        });
    }

    private int blendColors(int color1, int color2, float ratio) {
        float inverseRatio = 1f - ratio;
        int r = (int) (Color.red(color1) * inverseRatio + Color.red(color2) * ratio);
        int g = (int) (Color.green(color1) * inverseRatio + Color.green(color2) * ratio);
        int b = (int) (Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio);
        return Color.rgb(r, g, b);
    }

    private void applyBackgroundImage(Activity activity, ViewGroup rootView) {
        Bitmap bmp = loadBackgroundBitmap();
        if (bmp == null) return;

        ImageView bgView = rootView.findViewWithTag("personalization_bg");
        if (bgView == null) {
            bgView = new ImageView(activity);
            bgView.setTag("personalization_bg");
            bgView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            bgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            rootView.addView(bgView, 0);
        }
        bgView.setImageBitmap(bmp);

        int baseMode = getBaseMode();
        View overlayView = rootView.findViewWithTag("personalization_overlay");
        if (overlayView == null) {
            overlayView = new View(activity);
            overlayView.setTag("personalization_overlay");
            overlayView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            int bgIndex = rootView.indexOfChild(bgView);
            rootView.addView(overlayView, bgIndex + 1);
        }

        boolean isDark = isDarkMode(activity);
        int accentForOverlay = getAccentColor();
        switch (baseMode) {
            case BASE_MODE_FOLLOW_THEME:
                if (accentForOverlay != 0) {
                    overlayView.setBackgroundColor(Color.argb(140, Color.red(accentForOverlay), Color.green(accentForOverlay), Color.blue(accentForOverlay)));
                } else {
                    overlayView.setBackgroundColor(isDark ? Color.argb(140, 0, 0, 0) : Color.argb(120, 255, 255, 255));
                }
                overlayView.setVisibility(View.VISIBLE);
                break;
            case BASE_MODE_CUSTOM:
                int customColor = getBaseCustomColor();
                overlayView.setBackgroundColor(Color.argb(140, Color.red(customColor), Color.green(customColor), Color.blue(customColor)));
                overlayView.setVisibility(View.VISIBLE);
                break;
            default:
                overlayView.setBackgroundColor(isDark ? Color.argb(100, 0, 0, 0) : Color.argb(80, 255, 255, 255));
                overlayView.setVisibility(View.VISIBLE);
                break;
        }

        makeChildBackgroundsTranslucent(rootView, activity);
    }

    private int getGlassColor(boolean isDark) {
        if (isDark) {
            return Color.argb(GLASS_ALPHA_DARK, GLASS_R_DARK, GLASS_G_DARK, GLASS_B_DARK);
        } else {
            return Color.argb(GLASS_ALPHA_LIGHT, GLASS_R_LIGHT, GLASS_G_LIGHT, GLASS_B_LIGHT);
        }
    }

    private void makeChildBackgroundsTranslucent(ViewGroup parent, Activity activity) {
        int bgColor = ContextCompat.getColor(activity, R.color.background);
        int surfColor = ContextCompat.getColor(activity, R.color.surface);
        int surfVariantColor = ContextCompat.getColor(activity, R.color.surface_variant);
        boolean isDark = isDarkMode(activity);
        int glassColor = getGlassColor(isDark);

        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if ("personalization_bg".equals(child.getTag()) || "personalization_overlay".equals(child.getTag())) {
                continue;
            }

            if (child instanceof CardView) {
                CardView cv = (CardView) child;
                int cardColor = cv.getCardBackgroundColor().getDefaultColor();
                if (cardColor == surfColor || cardColor == bgColor || cardColor == surfVariantColor
                        || isNearBlack(cardColor) || isNearWhite(cardColor)) {
                    cv.setCardBackgroundColor(glassColor);
                }
                makeChildBackgroundsTranslucent((ViewGroup) child, activity);
                continue;
            }

            Drawable bg = child.getBackground();
            if (bg instanceof ColorDrawable) {
                int color = ((ColorDrawable) bg).getColor();
                if (color == bgColor || isNearBlack(color)) {
                    child.setBackgroundColor(Color.TRANSPARENT);
                } else if (color == surfColor || color == surfVariantColor || isNearWhite(color)) {
                    child.setBackgroundColor(glassColor);
                }
            } else if (bg instanceof GradientDrawable) {
                GradientDrawable gd = (GradientDrawable) bg;
                try {
                    if (gd.getColor() != null) {
                        int gdColor = gd.getColor().getDefaultColor();
                        if (gdColor == surfColor || gdColor == bgColor || gdColor == surfVariantColor
                                || isNearBlack(gdColor) || isNearWhite(gdColor)) {
                            GradientDrawable newGd = new GradientDrawable();
                            newGd.setCornerRadius(dpToPx(activity, 12));
                            newGd.setColor(glassColor);
                            child.setBackground(newGd);
                        }
                    }
                } catch (Exception ignored) {}
            } else if (bg instanceof StateListDrawable) {
                StateListDrawable sld = (StateListDrawable) bg;
                boolean modified = false;
                for (int si = 0; si < sld.getStateCount(); si++) {
                    Drawable stateDrawable = sld.getStateDrawable(si);
                    if (stateDrawable instanceof GradientDrawable) {
                        GradientDrawable sgd = (GradientDrawable) stateDrawable;
                        try {
                            if (sgd.getColor() != null) {
                                int sgdColor = sgd.getColor().getDefaultColor();
                                if (sgdColor == surfColor || sgdColor == bgColor || sgdColor == surfVariantColor
                                        || isNearBlack(sgdColor) || isNearWhite(sgdColor)) {
                                    sgd.setColor(glassColor);
                                    modified = true;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (modified) {
                    sld.invalidateSelf();
                }
            }

            if (child instanceof ViewGroup) {
                makeChildBackgroundsTranslucent((ViewGroup) child, activity);
            }
        }
    }

    private boolean isNearBlack(int color) {
        return Color.red(color) < 20 && Color.green(color) < 20 && Color.blue(color) < 20 && Color.alpha(color) > 200;
    }

    private boolean isNearWhite(int color) {
        return Color.red(color) > 235 && Color.green(color) > 235 && Color.blue(color) > 235 && Color.alpha(color) > 200;
    }

    public void applyAccentColorRecursive(View view, int accentColor, Activity activity) {
        applyAccentColorRecursive(view, accentColor, (Context) activity);
    }

    public void applyAccentColorRecursive(View view, int accentColor, Context ctx) {
        int defaultPrimary = ContextCompat.getColor(ctx, R.color.primary);
        int defaultSecondary = ContextCompat.getColor(ctx, R.color.secondary);
        int defaultTertiary = ContextCompat.getColor(ctx, R.color.tertiary);
        int defaultAccentText = ContextCompat.getColor(ctx, R.color.accent_text);

        if (view instanceof com.google.android.material.switchmaterial.SwitchMaterial) {
            com.google.android.material.switchmaterial.SwitchMaterial sw =
                    (com.google.android.material.switchmaterial.SwitchMaterial) view;
            try {
                int[][] states = {{android.R.attr.state_checked}, {}};
                sw.setThumbTintList(new ColorStateList(states, new int[]{accentColor, 0xFFAAAAAA}));
                int trackChecked = Color.argb(100, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));
                sw.setTrackTintList(new ColorStateList(states, new int[]{trackChecked, 0xFF555555}));
            } catch (Exception ignored) {}
        }

        if (view instanceof Switch && !(view instanceof com.google.android.material.switchmaterial.SwitchMaterial)) {
            Switch sw = (Switch) view;
            try {
                int[][] states = {{android.R.attr.state_checked}, {}};
                sw.setThumbTintList(new ColorStateList(states, new int[]{accentColor, 0xFFAAAAAA}));
                int trackChecked = Color.argb(100, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));
                sw.setTrackTintList(new ColorStateList(states, new int[]{trackChecked, 0xFF555555}));
            } catch (Exception ignored) {}
        }

        if (view instanceof androidx.appcompat.widget.SwitchCompat && !(view instanceof com.google.android.material.switchmaterial.SwitchMaterial)) {
            androidx.appcompat.widget.SwitchCompat sw = (androidx.appcompat.widget.SwitchCompat) view;
            try {
                int[][] states = {{android.R.attr.state_checked}, {}};
                sw.setThumbTintList(new ColorStateList(states, new int[]{accentColor, 0xFFAAAAAA}));
                int trackChecked = Color.argb(100, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));
                sw.setTrackTintList(new ColorStateList(states, new int[]{trackChecked, 0xFF555555}));
            } catch (Exception ignored) {}
        }

        if (view instanceof SeekBar) {
            SeekBar sb = (SeekBar) view;
            try {
                sb.setProgressTintList(ColorStateList.valueOf(accentColor));
                sb.setThumbTintList(ColorStateList.valueOf(accentColor));
            } catch (Exception ignored) {}
        }

        if (view instanceof Button) {
            Button btn = (Button) view;
            try {
                if (btn.getBackgroundTintList() != null) {
                    int currentTint = btn.getBackgroundTintList().getDefaultColor();
                    if (currentTint == defaultPrimary || currentTint == defaultSecondary || currentTint == defaultTertiary) {
                        btn.setBackgroundTintList(ColorStateList.valueOf(accentColor));
                        btn.setTextColor(Color.WHITE);
                    }
                }
            } catch (Exception ignored) {}

            if (btn.getCurrentTextColor() == defaultPrimary || btn.getCurrentTextColor() == defaultAccentText) {
                btn.setTextColor(accentColor);
            }

            Drawable bg = btn.getBackground();
            if (bg instanceof GradientDrawable) {
                GradientDrawable gd = (GradientDrawable) bg;
                try {
                    if (gd.getColor() != null) {
                        int gdColor = gd.getColor().getDefaultColor();
                        if (gdColor == defaultPrimary || gdColor == defaultSecondary || gdColor == defaultTertiary) {
                            gd.setColor(accentColor);
                            btn.setTextColor(Color.WHITE);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        if (view instanceof TextView && !(view instanceof Button)) {
            TextView tv = (TextView) view;
            int textColor = tv.getCurrentTextColor();

            if (textColor == defaultPrimary || textColor == defaultAccentText) {
                tv.setTextColor(accentColor);
            }

            try {
                if (tv.getCompoundDrawableTintList() != null) {
                    int tintColor = tv.getCompoundDrawableTintList().getDefaultColor();
                    if (tintColor == defaultPrimary) {
                        tv.setCompoundDrawableTintList(ColorStateList.valueOf(accentColor));
                    }
                }
            } catch (Exception ignored) {}
        }

        if (view instanceof ImageView && !(view instanceof Button)) {
            ImageView iv = (ImageView) view;
            if (iv.getImageTintList() != null) {
                int tint = iv.getImageTintList().getDefaultColor();
                if (tint == defaultPrimary) {
                    iv.setImageTintList(ColorStateList.valueOf(accentColor));
                }
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyAccentColorRecursive(group.getChildAt(i), accentColor, ctx);
            }
        }
    }

    public void applyAccentToView(View view, Context context) {
        int accent = getAccentColor();
        if (accent == 0) return;
        applyAccentColorRecursive(view, accent, context);
    }

    private boolean isDarkMode(Activity activity) {
        return isDarkMode((Context) activity);
    }

    public boolean isDarkMode(Context ctx) {
        int nightModeFlags = ctx.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    public int getEffectiveSurfaceColor() {
        if (hasBackgroundImage()) {
            return getGlassColor(isDarkMode(context));
        }
        return ContextCompat.getColor(context, R.color.surface);
    }

    public void applyGlassToView(View view) {
        if (!hasBackgroundImage()) return;
        boolean isDark = isDarkMode(context);
        int glassColor = getGlassColor(isDark);
        int surfColor = ContextCompat.getColor(context, R.color.surface);
        int bgColor = ContextCompat.getColor(context, R.color.background);
        int surfVariantColor = ContextCompat.getColor(context, R.color.surface_variant);

        Drawable bg = view.getBackground();
        if (bg instanceof GradientDrawable) {
            GradientDrawable gd = (GradientDrawable) bg;
            try {
                if (gd.getColor() != null) {
                    int gdColor = gd.getColor().getDefaultColor();
                    if (gdColor == surfColor || gdColor == bgColor || gdColor == surfVariantColor
                            || isNearBlack(gdColor) || isNearWhite(gdColor)) {
                        gd.setColor(glassColor);
                    }
                }
            } catch (Exception ignored) {}
        } else if (bg instanceof StateListDrawable) {
            StateListDrawable sld = (StateListDrawable) bg;
            for (int si = 0; si < sld.getStateCount(); si++) {
                Drawable stateDrawable = sld.getStateDrawable(si);
                if (stateDrawable instanceof GradientDrawable) {
                    GradientDrawable sgd = (GradientDrawable) stateDrawable;
                    try {
                        if (sgd.getColor() != null) {
                            int sgdColor = sgd.getColor().getDefaultColor();
                            if (sgdColor == surfColor || sgdColor == bgColor || sgdColor == surfVariantColor
                                    || isNearBlack(sgdColor) || isNearWhite(sgdColor)) {
                                sgd.setColor(glassColor);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
            sld.invalidateSelf();
        } else if (view instanceof CardView) {
            CardView cv = (CardView) view;
            int cardColor = cv.getCardBackgroundColor().getDefaultColor();
            if (cardColor == surfColor || cardColor == bgColor || cardColor == surfVariantColor
                    || isNearBlack(cardColor) || isNearWhite(cardColor)) {
                cv.setCardBackgroundColor(glassColor);
            }
        }
    }

    private float dpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    public static final int[] PRESET_COLORS = {
            0xFF26A69A, 0xFF42A5F5, 0xFF5C6BC0,
            0xFFAB47BC, 0xFFEC407A, 0xFFEF5350,
            0xFFFF7043, 0xFFFFA726, 0xFFFFCA28,
            0xFF66BB6A, 0xFF26C6DA, 0xFF29B6F6,
            0xFF7E57C2, 0xFFE91E63, 0xFFF44336
    };

    public static final int[] MORE_COLORS = {
            0xFFFF9800, 0xFFF57C00, 0xFFE65100,
            0xFFFF5722, 0xFFBF360C, 0xFFD32F2F,
            0xFFC62828, 0xFFAD1457, 0xFF880E4F,
            0xFFE91E63, 0xFFF06292, 0xFFCE93D8,
            0xFF9C27B0, 0xFF7B1FA2, 0xFF4A148C,
            0xFF5C6BC0, 0xFF3F51B5, 0xFF283593,
            0xFF1A237E, 0xFF1565C0, 0xFF0D47A1,
            0xFF0277BD, 0xFF00838F, 0xFF006064,
            0xFF00796B, 0xFF00695C, 0xFF004D40,
            0xFF2E7D32, 0xFF1B5E20, 0xFF33691E,
            0xFF827717, 0xFF9E9E9E, 0xFF757575,
            0xFF616161, 0xFF455A64, 0xFF37474F,
            0xFF4AE0A0, 0xFFCDDC39, 0xFF8BC34A
    };
}

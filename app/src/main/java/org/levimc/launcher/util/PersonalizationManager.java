package org.levimc.launcher.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageDecoder;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import org.levimc.launcher.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class PersonalizationManager {
    private static final String PREFS_NAME = "personalization_prefs";
    private static final String KEY_ACCENT_COLOR = "accent_color";
    private static final String KEY_BG_IMAGE_PATH = "bg_image_path";
    private static final String KEY_BG_MEDIA_TYPE = "bg_media_type";
    private static final String KEY_BG_POSTER_PATH = "bg_poster_path";
    private static final String KEY_BG_IMAGE_BLUR = "bg_image_blur";
    private static final String KEY_BG_IMAGE_BRIGHTNESS = "bg_image_brightness";

    public static final int BG_BLUR_MIN = 0;
    public static final int BG_BLUR_MAX = 25;
    public static final int BG_BRIGHTNESS_MIN = 1;
    public static final int BG_BRIGHTNESS_MAX = 150;
    public static final int BG_BRIGHTNESS_DEFAULT = 100;

    private static final AtomicInteger sChangeGeneration = new AtomicInteger();
    private static final AtomicInteger sBackgroundEffectGeneration = new AtomicInteger();

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
        String type = prefs.getString(KEY_BG_MEDIA_TYPE, "image");
        if (!"image".equals(type) && !"animation".equals(type)) {
            String previous = prefs.getString(KEY_BG_IMAGE_PATH, null);
            String poster = prefs.getString(KEY_BG_POSTER_PATH, null);
            SharedPreferences.Editor editor = prefs.edit();
            if (poster != null && new File(poster).exists()) {
                editor.putString(KEY_BG_IMAGE_PATH, poster).putString(KEY_BG_MEDIA_TYPE, "image");
            } else {
                editor.remove(KEY_BG_IMAGE_PATH).remove(KEY_BG_POSTER_PATH).remove(KEY_BG_MEDIA_TYPE);
            }
            editor.apply();
            if (previous != null && !previous.equals(poster)) deleteBackgroundFile(previous);
            sChangeGeneration.incrementAndGet();
        }
    }

    public int getAccentColor() {
        return prefs.getInt(KEY_ACCENT_COLOR, PRESET_COLORS[0]);
    }

    public void setAccentColor(int color) {
        prefs.edit().putInt(KEY_ACCENT_COLOR, color).apply();
        sChangeGeneration.incrementAndGet();
    }

    public boolean hasCustomAccent() {
        return prefs.contains(KEY_ACCENT_COLOR) && prefs.getInt(KEY_ACCENT_COLOR, 0) != 0;
    }

    public void clearAccentColor() {
        prefs.edit().remove(KEY_ACCENT_COLOR).apply();
        sChangeGeneration.incrementAndGet();
    }

    public String getBackgroundImagePath() {
        return prefs.getString(KEY_BG_IMAGE_PATH, null);
    }

    public boolean hasBackgroundImage() {
        String path = getBackgroundImagePath();
        if (path == null) return false;
        return new File(path).exists();
    }

    public int getBackgroundImageBlur() {
        return clamp(prefs.getInt(KEY_BG_IMAGE_BLUR, BG_BLUR_MIN), BG_BLUR_MIN, BG_BLUR_MAX);
    }

    public void setBackgroundImageBlur(int blurRadius) {
        int clamped = clamp(blurRadius, BG_BLUR_MIN, BG_BLUR_MAX);
        if (getBackgroundImageBlur() == clamped) return;
        prefs.edit().putInt(KEY_BG_IMAGE_BLUR, clamped).apply();
        sBackgroundEffectGeneration.incrementAndGet();
    }

    public int getBackgroundImageBrightness() {
        return clamp(prefs.getInt(KEY_BG_IMAGE_BRIGHTNESS, BG_BRIGHTNESS_DEFAULT),
                BG_BRIGHTNESS_MIN, BG_BRIGHTNESS_MAX);
    }

    public void setBackgroundImageBrightness(int brightnessPercent) {
        int clamped = clamp(brightnessPercent, BG_BRIGHTNESS_MIN, BG_BRIGHTNESS_MAX);
        if (getBackgroundImageBrightness() == clamped) return;
        prefs.edit().putInt(KEY_BG_IMAGE_BRIGHTNESS, clamped).apply();
        sBackgroundEffectGeneration.incrementAndGet();
    }

    public boolean hasAnimatedBackground() {
        return hasBackgroundImage() && "animation".equals(prefs.getString(KEY_BG_MEDIA_TYPE, "image"));
    }

    public boolean canBlurBackground() {
        return !hasAnimatedBackground() || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    public boolean setBackgroundImage(Uri sourceUri, Context activityContext) {
        File media = null;
        File poster = null;
        Bitmap bitmap = null;
        boolean installed = false;
        try {
            if (LauncherBackgroundController.isMinecraftBlocked()) return false;
            File directory = new File(context.getFilesDir(), "personalization");
            if (!directory.exists() && !directory.mkdirs()) return false;
            String name = "background_" + UUID.randomUUID();
            media = new File(directory, name + ".media");
            poster = new File(directory, name + ".jpg");
            try (InputStream input = activityContext.getContentResolver().openInputStream(sourceUri);
                 FileOutputStream output = new FileOutputStream(media)) {
                if (input == null) return false;
                byte[] buffer = new byte[65536];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    ensureImportActive();
                    output.write(buffer, 0, count);
                }
            }
            ensureImportActive();
            boolean[] animated = {false};
            bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(media),
                    (decoder, info, source) -> {
                        animated[0] = info.isAnimated();
                        int width = info.getSize().getWidth();
                        int height = info.getSize().getHeight();
                        float scale = Math.min(1f, (animated[0] ? 720f : 2048f)
                                / Math.max(width, height));
                        decoder.setTargetSize(Math.max(1, Math.round(width * scale)),
                                Math.max(1, Math.round(height * scale)));
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    });
            String type = animated[0] ? "animation" : "image";
            ensureImportActive();
            try (FileOutputStream output = new FileOutputStream(poster)) {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)) return false;
            }
            ensureImportActive();
            String previousMedia = getBackgroundImagePath();
            String previousPoster = prefs.getString(KEY_BG_POSTER_PATH, null);
            File selected = "image".equals(type) ? poster : media;
            prefs.edit().putString(KEY_BG_IMAGE_PATH, selected.getAbsolutePath())
                    .putString(KEY_BG_POSTER_PATH, poster.getAbsolutePath())
                    .putString(KEY_BG_MEDIA_TYPE, type).apply();
            installed = true;
            sChangeGeneration.incrementAndGet();
            if ("image".equals(type)) media.delete();
            deleteBackgroundFile(previousMedia);
            deleteBackgroundFile(previousPoster);
            return true;
        } catch (Exception | OutOfMemoryError e) {
            return false;
        } finally {
            if (bitmap != null) bitmap.recycle();
            if (!installed) {
                if (media != null) media.delete();
                if (poster != null) poster.delete();
            }
        }
    }

    private static void ensureImportActive() throws IOException {
        if (Thread.currentThread().isInterrupted() || LauncherBackgroundController.isMinecraftBlocked()) {
            throw new IOException("Background import cancelled");
        }
    }

    private void deleteBackgroundFile(String path) {
        if (path == null) return;
        File file = new File(path);
        File directory = new File(context.getFilesDir(), "personalization");
        if (directory.equals(file.getParentFile())) file.delete();
    }

    public void clearBackgroundImage() {
        LauncherBackgroundController.backgroundCleared();
        String path = getBackgroundImagePath();
        String poster = prefs.getString(KEY_BG_POSTER_PATH, null);
        prefs.edit().remove(KEY_BG_IMAGE_PATH).remove(KEY_BG_POSTER_PATH)
                .remove(KEY_BG_MEDIA_TYPE).apply();
        deleteBackgroundFile(path);
        deleteBackgroundFile(poster);
        sChangeGeneration.incrementAndGet();
    }

    public Bitmap loadBackgroundBitmap() {
        String path = prefs.getString(KEY_BG_POSTER_PATH, getBackgroundImagePath());
        if (path == null || !new File(path).exists()) return null;
        try {
            return BitmapFactory.decodeFile(path);
        } catch (Exception e) {
            return null;
        }
    }



    public static int getChangeGeneration() {
        return sChangeGeneration.get();
    }

    public static int getBackgroundEffectGeneration() {
        return sBackgroundEffectGeneration.get();
    }

    public void applyToActivity(Activity activity) {
        if (activity instanceof org.levimc.launcher.ui.activities.SplashActivity) return;

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;

        int accent = getAccentColor();
        boolean hasBg = hasBackgroundImage();

        if (hasBg && !LauncherBackgroundController.isMinecraftBlocked()
                && !(activity instanceof org.levimc.launcher.core.minecraft.MinecraftLoadingActivity)
                && !(activity instanceof org.levimc.launcher.core.minecraft.LauncherRestartActivity)
                && !(activity instanceof org.levimc.launcher.core.minecraft.MinecraftActivity)) {
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
            applySolidAccentText(appName, accent);
        }

        View signInBtn = activity.findViewById(R.id.nav_sign_in_button);
        if (signInBtn instanceof Button) {
            Button btn = (Button) signInBtn;
            btn.setBackgroundTintList(ColorStateList.valueOf(accent));
            btn.setTextColor(Color.WHITE);
        }
    }

    public void applySolidAccentText(TextView textView, int accentColor) {
        textView.getPaint().setShader(null);
        textView.setTextColor(accentColor);
        textView.invalidate();
    }

    private void applyBackgroundImage(Activity activity, ViewGroup rootView) {
        View background = rootView.findViewWithTag("personalization_bg");
        boolean animated = hasAnimatedBackground();
        if (background != null && (animated != (background instanceof FrameLayout))) {
            rootView.removeView(background);
            background = null;
        }
        ImageView bgView;
        if (animated) {
            FrameLayout holder;
            if (background == null) {
                holder = new FrameLayout(activity);
                holder.setTag("personalization_bg");
                rootView.addView(holder, 0, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                bgView = new ImageView(activity);
                bgView.setTag("personalization_poster");
                bgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                holder.addView(bgView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                FrameLayout media = new FrameLayout(activity);
                media.setTag("personalization_media");
                media.setClickable(false);
                media.setFocusable(false);
                holder.addView(media, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                background = holder;
            } else {
                bgView = ((FrameLayout) background).findViewWithTag("personalization_poster");
            }
        } else {
            if (background == null) {
                bgView = new ImageView(activity);
                bgView.setTag("personalization_bg");
                bgView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                bgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                rootView.addView(bgView, 0);
                background = bgView;
            } else bgView = (ImageView) background;
        }
        if (!applyBackgroundImageToView(bgView)) return;

        View overlayView = rootView.findViewWithTag("personalization_overlay");
        if (overlayView == null) {
            overlayView = new View(activity);
            overlayView.setTag("personalization_overlay");
            overlayView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            int bgIndex = rootView.indexOfChild(background);
            rootView.addView(overlayView, bgIndex + 1);
        }

        boolean isDark = isDarkMode(activity);
        overlayView.setBackgroundColor(isDark ? Color.argb(100, 0, 0, 0) : Color.argb(80, 255, 255, 255));
        overlayView.setVisibility(View.VISIBLE);

        makeChildBackgroundsTranslucent(rootView, activity);
    }

    public void refreshBackgroundEffects(Activity activity) {
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;
        ImageView bgView = findBackgroundPoster(rootView);
        if (bgView != null) refreshBackgroundImageView(bgView);
        LauncherBackgroundController.refreshEffects();
    }

    public void refreshBackgroundColorEffects(Activity activity) {
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;
        ImageView bgView = findBackgroundPoster(rootView);
        if (bgView != null) applyBackgroundImageEffects(bgView);
        LauncherBackgroundController.refreshEffects();
    }

    private ImageView findBackgroundPoster(ViewGroup rootView) {
        View background = rootView.findViewWithTag("personalization_bg");
        if (background instanceof ImageView) return (ImageView) background;
        return rootView.findViewWithTag("personalization_poster");
    }

    public boolean supportsRealtimeBackgroundBlur() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    public boolean applyBackgroundImageToView(ImageView bgView) {
        if (hasAnimatedBackground() && LauncherBackgroundController.isMinecraftBlocked()) return false;
        String path = prefs.getString(KEY_BG_POSTER_PATH, getBackgroundImagePath());
        int blur = Build.VERSION.SDK_INT < Build.VERSION_CODES.S && canBlurBackground()
                ? getBackgroundImageBlur() : 0;
        String contentKey = path + ":" + blur;
        if (contentKey.equals(bgView.getTag(R.id.bg_image_preview)) && bgView.getDrawable() != null) {
            applyBackgroundImageEffects(bgView);
            return true;
        }
        Bitmap bmp = loadBackgroundBitmap();
        if (bmp == null) return false;

        Bitmap displayBitmap = bmp;
        if (blur > 0) {
            Bitmap blurredBitmap = createBlurredBitmap(bmp, blur);
            if (blurredBitmap != null) {
                displayBitmap = blurredBitmap;
                if (displayBitmap != bmp) {
                    bmp.recycle();
                }
            }
        }

        bgView.setImageBitmap(displayBitmap);
        bgView.setTag(R.id.bg_image_preview, contentKey);
        applyBackgroundImageEffects(bgView);
        return true;
    }

    public void refreshBackgroundImageView(ImageView bgView) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            applyBackgroundImageToView(bgView);
        } else {
            applyBackgroundImageEffects(bgView);
        }
    }

    public void applyBackgroundImageEffects(ImageView bgView) {
        int blurRadius = getBackgroundImageBlur();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (blurRadius > 0) {
                bgView.setRenderEffect(RenderEffect.createBlurEffect(
                        blurRadius, blurRadius, Shader.TileMode.CLAMP));
            } else {
                bgView.setRenderEffect(null);
            }
        }

        float brightness = getBackgroundImageBrightness() / 100f;
        if (Math.abs(brightness - 1f) < 0.001f) {
            bgView.clearColorFilter();
            return;
        }

        ColorMatrix matrix = new ColorMatrix(new float[]{
                brightness, 0, 0, 0, 0,
                0, brightness, 0, 0, 0,
                0, 0, brightness, 0, 0,
                0, 0, 0, 1, 0
        });
        bgView.setColorFilter(new ColorMatrixColorFilter(matrix));
    }

    private Bitmap createBlurredBitmap(Bitmap source, int radius) {
        if (source == null || radius <= 0) return source;
        try {
            int maxDim = 720;
            int width = source.getWidth();
            int height = source.getHeight();
            float scale = Math.min(1f, (float) maxDim / Math.max(width, height));
            int scaledWidth = Math.max(1, Math.round(width * scale));
            int scaledHeight = Math.max(1, Math.round(height * scale));
            Bitmap working = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
                    .copy(Bitmap.Config.ARGB_8888, true);

            int w = working.getWidth();
            int h = working.getHeight();
            int[] pixels = new int[w * h];
            int[] temp = new int[w * h];
            int[] output = new int[w * h];
            working.getPixels(pixels, 0, w, 0, 0, w, h);

            int windowSize = radius * 2 + 1;
            for (int y = 0; y < h; y++) {
                int row = y * w;
                int a = 0;
                int r = 0;
                int g = 0;
                int b = 0;
                for (int i = -radius; i <= radius; i++) {
                    int color = pixels[row + clamp(i, 0, w - 1)];
                    a += Color.alpha(color);
                    r += Color.red(color);
                    g += Color.green(color);
                    b += Color.blue(color);
                }
                for (int x = 0; x < w; x++) {
                    temp[row + x] = Color.argb(a / windowSize, r / windowSize, g / windowSize, b / windowSize);

                    int removeColor = pixels[row + clamp(x - radius, 0, w - 1)];
                    int addColor = pixels[row + clamp(x + radius + 1, 0, w - 1)];
                    a += Color.alpha(addColor) - Color.alpha(removeColor);
                    r += Color.red(addColor) - Color.red(removeColor);
                    g += Color.green(addColor) - Color.green(removeColor);
                    b += Color.blue(addColor) - Color.blue(removeColor);
                }
            }

            for (int x = 0; x < w; x++) {
                int a = 0;
                int r = 0;
                int g = 0;
                int b = 0;
                for (int i = -radius; i <= radius; i++) {
                    int color = temp[clamp(i, 0, h - 1) * w + x];
                    a += Color.alpha(color);
                    r += Color.red(color);
                    g += Color.green(color);
                    b += Color.blue(color);
                }
                for (int y = 0; y < h; y++) {
                    output[y * w + x] = Color.argb(a / windowSize, r / windowSize, g / windowSize, b / windowSize);

                    int removeColor = temp[clamp(y - radius, 0, h - 1) * w + x];
                    int addColor = temp[clamp(y + radius + 1, 0, h - 1) * w + x];
                    a += Color.alpha(addColor) - Color.alpha(removeColor);
                    r += Color.red(addColor) - Color.red(removeColor);
                    g += Color.green(addColor) - Color.green(removeColor);
                    b += Color.blue(addColor) - Color.blue(removeColor);
                }
            }

            working.setPixels(output, 0, w, 0, 0, w, h);
            return working;
        } catch (Exception e) {
            return null;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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

        if (view instanceof ProgressBar) {
            ProgressBar pb = (ProgressBar) view;
            try {
                pb.setProgressTintList(ColorStateList.valueOf(accentColor));
                pb.setProgressBackgroundTintList(ColorStateList.valueOf(Color.argb(42, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))));
                pb.setIndeterminateTintList(ColorStateList.valueOf(accentColor));
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

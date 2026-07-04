package org.levimc.launcher.ui.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

import org.levimc.launcher.R;
import org.levimc.launcher.databinding.ActivitySplashBinding;
import org.levimc.launcher.preloader.PreloaderSignatureRulesManager;
import org.levimc.launcher.util.LauncherStorage;
import org.levimc.launcher.util.PersonalizationManager;

import java.util.Locale;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity {

    @Override
    protected boolean shouldSkipNavBar() { return true; }

    private ActivitySplashBinding binding;
    private boolean navigated = false;
    private ValueAnimator orbitAnimator;
    private ObjectAnimator progressAnimator;
    private ValueAnimator glowPulseAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        syncSystemLocale();
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applySplashTheme();

        binding.imgLeaf.setAlpha(0f);
        binding.imgLeaf.setScaleX(0.8f);
        binding.imgLeaf.setScaleY(0.8f);
        binding.logoGlow.setAlpha(0f);
        binding.logoGlow.setScaleX(0.5f);
        binding.logoGlow.setScaleY(0.5f);
        binding.orbitRing.setAlpha(0f);
        binding.orbitDot.setAlpha(0f);
        binding.tvAppName.setAlpha(0f);
        binding.tvAppName.setTranslationY(20f);
        binding.loadingBar.setAlpha(0f);
        binding.loadingBar.setProgress(0);
        binding.tvPreparing.setAlpha(0f);

        warmUpStorageAndVersions();

        binding.getRoot().post(this::startSplashSequence);
    }

    private void warmUpStorageAndVersions() {
        Context appContext = getApplicationContext();
        new Thread(() -> {
            LauncherStorage.ensureNoMedia(appContext);
        }, "launcher-storage-warmup").start();
        PreloaderSignatureRulesManager.refreshOnLauncherStart(appContext);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelAnimators();
    }

    private void cancelAnimators() {
        if (orbitAnimator != null) orbitAnimator.cancel();
        if (progressAnimator != null) progressAnimator.cancel();
        if (glowPulseAnimator != null) glowPulseAnimator.cancel();
    }

    private void startSplashSequence() {
        binding.logoGlow.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setInterpolator(new LinearOutSlowInInterpolator())
            .start();

        binding.imgLeaf.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setStartDelay(200)
            .setInterpolator(new LinearOutSlowInInterpolator())
            .start();

        binding.orbitRing.post(() -> {
            float centerX = binding.orbitRing.getX() + binding.orbitRing.getWidth() / 2f;
            float centerY = binding.orbitRing.getY() + binding.orbitRing.getHeight() / 2f;
            float radius = binding.orbitRing.getWidth() / 2f - binding.orbitDot.getWidth() / 2f;
            
            double radians = Math.toRadians(-90);
            float x = (float) (centerX + radius * Math.cos(radians) - binding.orbitDot.getWidth() / 2f);
            float y = (float) (centerY + radius * Math.sin(radians) - binding.orbitDot.getHeight() / 2f);
            
            binding.orbitDot.setX(x);
            binding.orbitDot.setY(y);
        });

        binding.orbitRing.animate()
            .alpha(1f)
            .setDuration(400)
            .setStartDelay(400)
            .start();

        binding.orbitDot.animate()
            .alpha(1f)
            .setDuration(400)
            .setStartDelay(400)
            .withEndAction(this::startOrbitAnimation)
            .start();

        binding.tvAppName.postDelayed(() -> {
            binding.tvAppName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(new LinearOutSlowInInterpolator())
                .start();
        }, 500);

        binding.loadingBar.postDelayed(() -> {
            binding.loadingBar.animate().alpha(1f).setDuration(300).start();
            binding.tvPreparing.animate().alpha(0.7f).setDuration(300).start();
            startLoadingAnimation();
        }, 700);

        binding.logoGlow.postDelayed(this::startGlowPulse, 600);
    }

    private void applySplashTheme() {
        int accent = resolveAccentColor();
        binding.tvAppName.setTextColor(accent);
        binding.logoGlow.setBackground(createRadialGlow(accent));
        binding.orbitRing.setBackground(createOrbitRing(accent));
        binding.orbitDot.setBackground(createOrbitDot(accent));
        binding.loadingBar.setProgressTintList(ColorStateList.valueOf(accent));
        binding.loadingBar.setProgressBackgroundTintList(ColorStateList.valueOf(withAlpha(accent, 42)));
        binding.loadingBar.setIndeterminateTintList(ColorStateList.valueOf(accent));
        binding.tvPreparing.setTextColor(blendColors(
            getColor(R.color.text_secondary),
            accent,
            isDarkMode() ? 0.24f : 0.18f
        ));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.loadingBar.getProgressDrawable().setColorFilter(null);
        }
    }

    private int resolveAccentColor() {
        int accent = new PersonalizationManager(this).getAccentColor();
        return accent != 0 ? accent : getColor(R.color.primary);
    }

    private GradientDrawable createRadialGlow(int accent) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        drawable.setGradientRadius(dp(90));
        drawable.setColors(new int[]{
            withAlpha(accent, isDarkMode() ? 42 : 30),
            withAlpha(accent, isDarkMode() ? 18 : 14),
            Color.TRANSPARENT
        });
        return drawable;
    }

    private GradientDrawable createOrbitRing(int accent) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(Color.TRANSPARENT);
        drawable.setStroke(dp(1), withAlpha(accent, isDarkMode() ? 52 : 70));
        return drawable;
    }

    private GradientDrawable createOrbitDot(int accent) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        drawable.setGradientRadius(dp(8));
        drawable.setColors(new int[]{
            blendColors(accent, Color.WHITE, 0.28f),
            withAlpha(accent, 190),
            withAlpha(accent, 0)
        });
        return drawable;
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(
            Math.max(0, Math.min(alpha, 255)),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        );
    }

    private int blendColors(int from, int to, float ratio) {
        float boundedRatio = Math.max(0f, Math.min(ratio, 1f));
        float inverse = 1f - boundedRatio;
        return Color.rgb(
            (int) (Color.red(from) * inverse + Color.red(to) * boundedRatio),
            (int) (Color.green(from) * inverse + Color.green(to) * boundedRatio),
            (int) (Color.blue(from) * inverse + Color.blue(to) * boundedRatio)
        );
    }

    private boolean isDarkMode() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    private int dp(float value) {
        return Math.max(1, Math.round(value * getResources().getDisplayMetrics().density));
    }

    private void startOrbitAnimation() {
        View dot = binding.orbitDot;
        View ring = binding.orbitRing;
        
        float centerX = ring.getX() + ring.getWidth() / 2f;
        float centerY = ring.getY() + ring.getHeight() / 2f;
        float radius = ring.getWidth() / 2f - dot.getWidth() / 2f;

        orbitAnimator = ValueAnimator.ofFloat(0f, 360f);
        orbitAnimator.setDuration(3000);
        orbitAnimator.setRepeatCount(ValueAnimator.INFINITE);
        orbitAnimator.setInterpolator(new LinearInterpolator());
        orbitAnimator.addUpdateListener(animation -> {
            float angle = (float) animation.getAnimatedValue();
            double radians = Math.toRadians(angle - 90);
            float x = (float) (centerX + radius * Math.cos(radians) - dot.getWidth() / 2f);
            float y = (float) (centerY + radius * Math.sin(radians) - dot.getHeight() / 2f);
            dot.setX(x);
            dot.setY(y);
        });
        orbitAnimator.start();
    }

    private void startGlowPulse() {
        glowPulseAnimator = ValueAnimator.ofFloat(1f, 1.1f, 1f);
        glowPulseAnimator.setDuration(2000);
        glowPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        glowPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        glowPulseAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            binding.logoGlow.setScaleX(scale);
            binding.logoGlow.setScaleY(scale);
        });
        glowPulseAnimator.start();
    }

    private void startLoadingAnimation() {
        progressAnimator = ObjectAnimator.ofInt(binding.loadingBar, "progress", 0, 100);
        progressAnimator.setDuration(2200);
        progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                navigateToMain();
            }
        });
        progressAnimator.start();
    }

    private void navigateToMain() {
        if (navigated) return;
        navigated = true;

        binding.getRoot().animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction(() -> {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            })
            .start();
    }

    private void syncSystemLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                android.app.LocaleManager localeManager = getSystemService(android.app.LocaleManager.class);
                if (localeManager != null) {
                    android.os.LocaleList appLocales = localeManager.getApplicationLocales();
                    android.os.LocaleList systemLocales = localeManager.getSystemLocales();
                    if (!appLocales.isEmpty()) {
                        localeManager.setApplicationLocales(android.os.LocaleList.getEmptyLocaleList());
                    }
                    if (!systemLocales.isEmpty()) {
                        Locale.setDefault(systemLocales.get(0));
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}

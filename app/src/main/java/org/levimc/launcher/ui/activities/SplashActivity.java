package org.levimc.launcher.ui.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

import org.levimc.launcher.databinding.ActivitySplashBinding;

import java.util.Locale;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity {

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

        createNoMediaFile();

        binding.getRoot().post(this::startSplashSequence);
    }

    private void createNoMediaFile() {
        try {
            java.io.File baseDir = new java.io.File(
                android.os.Environment.getExternalStorageDirectory(), 
                "games/org.levimc"
            );
            if (!baseDir.exists()) baseDir.mkdirs();
            java.io.File noMediaFile = new java.io.File(baseDir, ".nomedia");
            if (!noMediaFile.exists()) noMediaFile.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            applyTextGradient(binding.tvAppName);
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

    private void applyTextGradient(TextView textView) {
        String text = textView.getText().toString();
        float textWidth = textView.getPaint().measureText(text);
        int green = Color.parseColor("#2ECC71");
        int cyan = Color.parseColor("#00D9FF");
        Shader shader = new LinearGradient(
            0, 0, Math.max(1f, textWidth), 0,
            new int[]{green, cyan},
            new float[]{0f, 1f},
            Shader.TileMode.CLAMP
        );
        textView.getPaint().setShader(shader);
        textView.invalidate();
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

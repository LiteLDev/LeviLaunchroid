package org.levimc.launcher.ui.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.animation.ValueAnimator;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import android.graphics.Color;
import android.view.animation.OvershootInterpolator;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import org.levimc.launcher.databinding.ActivitySplashBinding;
import org.levimc.launcher.ui.animation.DynamicAnim;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity {

    private ActivitySplashBinding binding;
    private boolean navigated = false;
    private ViewPropertyAnimator leafAnimator;
    private ViewPropertyAnimator nameAnimator;
    private ViewPropertyAnimator nameAppearAnimator;
    private ValueAnimator textStyleAnimator;
    private CharSequence fullAppNameText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        View root = binding.getRoot();

        fullAppNameText = binding.tvAppName.getText();
        binding.tvAppName.setText("");
        binding.tvAppName.setAlpha(0f);
        applyInitialTextStyle(binding.tvAppName, fullAppNameText);
        
        createNoMediaFile();
        
        binding.getRoot().post(() ->
                startLeafAnimation(() -> startAppNameAnimation(this::navigateToMain))
        );
    }
    
    private void createNoMediaFile() {
        try {
            java.io.File baseDir = new java.io.File(android.os.Environment.getExternalStorageDirectory(), "games/org.levimc");
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }
            java.io.File noMediaFile = new java.io.File(baseDir, ".nomedia");
            if (!noMediaFile.exists()) {
                noMediaFile.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (leafAnimator != null) {
            try { leafAnimator.cancel(); } catch (Exception ignored) {}
        }
        if (nameAnimator != null) {
            try { nameAnimator.cancel(); } catch (Exception ignored) {}
        }
        if (nameAppearAnimator != null) {
            try { nameAppearAnimator.cancel(); } catch (Exception ignored) {}
        }
        if (textStyleAnimator != null) {
            try { textStyleAnimator.cancel(); } catch (Exception ignored) {}
        }
    }

    private void startLeafAnimation(Runnable onEnd) {
        float dy = -getResources().getDisplayMetrics().density * 120f;
        if (leafAnimator != null) {
            try { leafAnimator.cancel(); } catch (Exception ignored) {}
        }
        try { binding.imgLeaf.animate().cancel(); } catch (Exception ignored) {}
        binding.imgLeaf.setTranslationY(dy);
        leafAnimator = binding.imgLeaf.animate()
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(new LinearOutSlowInInterpolator());
        if (onEnd != null) {
            leafAnimator.withEndAction(onEnd);
        }
        leafAnimator.start();
    }

    private void applyInitialTextStyle(TextView textView, CharSequence displayText) {
        String s = displayText == null ? "" : displayText.toString();
        float textWidth = textView.getPaint().measureText(s);
        int start = Color.parseColor("#F1C40F");
        int end = Color.parseColor("#2ECC71");
        int[] colors = new int[]{start, end};
        float[] positions = new float[]{0f, 1f};
        Shader shader = new LinearGradient(0, 0, Math.max(1f, textWidth), 0,
                colors, positions, Shader.TileMode.CLAMP);
        textView.getPaint().setShader(shader);
        textView.getPaint().setShadowLayer(10f, 0f, 0f, Color.parseColor("#803ED256"));
    }

    private void startAppNameAnimation(Runnable onEnd) {
        binding.tvAppName.setAlpha(1f);
        binding.tvAppName.setLetterSpacing(0f);
        binding.tvAppName.setScaleX(1f);
        binding.tvAppName.setScaleY(1f);
        
        CharSequence full = fullAppNameText;
        binding.tvAppName.setText("");
        int len = full != null ? full.length() : 0;
        int perCharMs = 60;
        int duration = Math.max(400, Math.min(1200, len * perCharMs));
        ValueAnimator typewriter = ValueAnimator.ofInt(0, Math.max(0, len));
        typewriter.setDuration(duration);
        typewriter.setInterpolator(new AccelerateDecelerateInterpolator());
        typewriter.addUpdateListener(a -> {
            int n = (int) a.getAnimatedValue();
            if (full != null) {
                binding.tvAppName.setText(full.subSequence(0, n));
            }
        });
        typewriter.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                startTitleFinalPolish(onEnd);
            }
        });
        textStyleAnimator = typewriter;
        typewriter.start();
    }

    private void startTitleFinalPolish(Runnable onEnd) {
        binding.tvAppName.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(240)
                .setInterpolator(new OvershootInterpolator())
                .withEndAction(() -> binding.tvAppName.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(160)
                        .start())
                .start();

        ValueAnimator glow = ValueAnimator.ofFloat(0f, 1f, 0f);
        glow.setDuration(520);
        glow.setInterpolator(new AccelerateDecelerateInterpolator());
        glow.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            float radius = 10f * t;
            binding.tvAppName.getPaint().setShadowLayer(radius, 0f, 0f, Color.parseColor("#804E7CF9"));
            float spacing = 0.04f * (t <= 0.5f ? (t / 0.5f) : ((1f - t) / 0.5f));
            binding.tvAppName.setLetterSpacing(spacing);
            binding.tvAppName.invalidate();
        });
        glow.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onEnd != null) onEnd.run();
            }
        });
        textStyleAnimator = glow;
        glow.start();
    }

    private void navigateToMain() {
        if (navigated) return;
        navigated = true;
        Intent newIntent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(newIntent);
        finish();
    }
}
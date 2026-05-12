package org.levimc.launcher.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.levimc.launcher.R;
import org.levimc.launcher.core.auth.MsftAccountStore;
import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.util.ThemeManager;

import java.util.Locale;

public class BaseActivity extends AppCompatActivity {
    private int appliedThemeGeneration = -1;
    private boolean navBarInjected = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String languageCode = prefs.getString("language", Locale.getDefault().toLanguageTag());
        Locale locale = Locale.forLanguageTag(languageCode);
        Locale.setDefault(locale);
        Resources res = newBase.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        Context localizedContext = newBase.createConfigurationContext(config);
        super.attachBaseContext(localizedContext);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager themeManager = new ThemeManager(this);
        themeManager.applyTheme();
        appliedThemeGeneration = ThemeManager.getThemeChangeGeneration();
        super.onCreate(savedInstanceState);
        hideSystemUI();
    }

    @Override
    public void setContentView(int layoutResID) {
        View contentView = LayoutInflater.from(this).inflate(layoutResID, null);
        wrapWithNavBar(contentView);
    }

    @Override
    public void setContentView(View view) {
        wrapWithNavBar(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        wrapWithNavBar(view);
    }

    private void wrapWithNavBar(View contentView) {
        if (shouldSkipNavBar()) {
            super.setContentView(contentView);
            return;
        }

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        View navBar = LayoutInflater.from(this).inflate(R.layout.nav_bar, wrapper, false);
        wrapper.addView(navBar);

        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        contentView.setLayoutParams(contentParams);
        wrapper.addView(contentView);

        super.setContentView(wrapper);
        navBarInjected = true;
        setupBaseNavBar();
    }

    protected boolean shouldSkipNavBar() {
        return false;
    }

    private void setupBaseNavBar() {
        int[] tabIds = {
            R.id.nav_tab_launch, R.id.nav_tab_import, R.id.nav_tab_instances,
            R.id.nav_tab_about, R.id.nav_tab_settings
        };
        for (int id : tabIds) {
            TextView tab = findViewById(id);
            if (tab == null) continue;
            tab.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
            tab.setTypeface(tab.getTypeface(), android.graphics.Typeface.NORMAL);
        }

        findViewById(R.id.nav_tab_launch).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.nav_tab_import).setOnClickListener(v -> {});
        findViewById(R.id.nav_tab_instances).setOnClickListener(v -> {
            if (!(this instanceof InstancesActivity)) {
                startActivity(new Intent(this, InstancesActivity.class));
                finish();
            }
        });
        findViewById(R.id.nav_tab_about).setOnClickListener(v -> {
            if (!(this instanceof AboutActivity)) {
                startActivity(new Intent(this, AboutActivity.class));
                finish();
            }
        });
        findViewById(R.id.nav_tab_settings).setOnClickListener(v -> {
            if (!(this instanceof SettingsActivity)) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
            }
        });

        refreshNavAccountUI();
    }

    private void refreshNavAccountUI() {
        if (!navBarInjected) return;
        java.util.List<MsftAccountStore.MsftAccount> list = MsftAccountStore.list(this);
        MsftAccountStore.MsftAccount active = null;
        for (MsftAccountStore.MsftAccount a : list) if (a.active) { active = a; break; }
        View signIn = findViewById(R.id.nav_sign_in_button);
        View avatarContainer = findViewById(R.id.nav_account_avatar_container);
        if (active == null) {
            if (signIn != null) signIn.setVisibility(View.VISIBLE);
            if (avatarContainer != null) avatarContainer.setVisibility(View.GONE);
        } else {
            if (signIn != null) signIn.setVisibility(View.GONE);
            if (avatarContainer != null) avatarContainer.setVisibility(View.VISIBLE);
        }
    }

    protected void setActiveNavTab(int activeTabId) {
        if (!navBarInjected) return;
        int[] tabIds = {
            R.id.nav_tab_launch, R.id.nav_tab_import, R.id.nav_tab_instances,
            R.id.nav_tab_about, R.id.nav_tab_settings
        };
        for (int id : tabIds) {
            TextView tab = findViewById(id);
            if (tab == null) continue;
            if (id == activeTabId) {
                tab.setTextColor(getResources().getColor(R.color.on_surface, getTheme()));
                tab.setTypeface(tab.getTypeface(), android.graphics.Typeface.BOLD);
            } else {
                tab.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
                tab.setTypeface(tab.getTypeface(), android.graphics.Typeface.NORMAL);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int currentGen = ThemeManager.getThemeChangeGeneration();
        if (appliedThemeGeneration != currentGen) {
            appliedThemeGeneration = currentGen;
            recreate();
            return;
        }
        getDelegate().applyDayNight();
        hideSystemUI();
        refreshNavAccountUI();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().applyDayNight();
        hideSystemUI();
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        super.startActivity(intent, options);
        overridePendingTransition(0, 0);
    }

    @Override
    public void finishAfterTransition() {
        super.finishAfterTransition();
        overridePendingTransition(0, 0);
    }
}
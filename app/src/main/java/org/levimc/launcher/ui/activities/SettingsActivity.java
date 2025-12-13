package org.levimc.launcher.ui.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.levimc.launcher.R;
import org.levimc.launcher.settings.FeatureSettings;
import org.levimc.launcher.ui.adapter.SettingsAdapter;
import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.ui.dialogs.LogcatOverlayManager;
import org.levimc.launcher.util.GithubReleaseUpdater;
import org.levimc.launcher.util.LanguageManager;
import org.levimc.launcher.util.PermissionsHandler;
import org.levimc.launcher.util.ThemeManager;

public class SettingsActivity extends BaseActivity {

    private LinearLayout settingsItemsContainer;
    private RecyclerView settingsRecyclerView;
    private PermissionsHandler permissionsHandler;
    private ActivityResultLauncher<Intent> permissionResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        DynamicAnim.applyPressScaleRecursively(findViewById(android.R.id.content));

        ImageButton backButton = findViewById(R.id.back_button);
        if (backButton != null) backButton.setOnClickListener(v -> finish());

        settingsRecyclerView = findViewById(R.id.settings_recycler);
        settingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        permissionsHandler = PermissionsHandler.getInstance();
        permissionResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (permissionsHandler != null) {
                        permissionsHandler.onActivityResult(result.getResultCode(), result.getData());
                    }
                }
        );
        permissionsHandler.setActivity(this, permissionResultLauncher);

        settingsRecyclerView.setAdapter(new SettingsAdapter(container -> {
            settingsItemsContainer = container;

            ThemeManager themeManager = new ThemeManager(this);
            LanguageManager languageManager = new LanguageManager(this);
            FeatureSettings fs = FeatureSettings.getInstance();
            addThemeSelectorItem(themeManager);
            addLanguageSelectorItem(languageManager);
            addSwitchItem(getString(R.string.memory_editor_enable), fs.isMemoryEditorEnabled(), (btn, checked) -> {
                fs.setMemoryEditorEnabled(checked);
            });
            addSwitchItem(getString(R.string.version_isolation), fs.isVersionIsolationEnabled(), (btn, checked) -> fs.setVersionIsolationEnabled(checked));
            addSwitchItem(getString(R.string.launcher_managed_mc_login), fs.isLauncherManagedMcLoginEnabled(), (btn, checked) -> fs.setLauncherManagedMcLoginEnabled(checked));
            addSwitchItem(getString(R.string.show_logcat_overlay), fs.isLogcatOverlayEnabled(), (btn, checked) -> {
                fs.setLogcatOverlayEnabled(checked);
                try {
                    LogcatOverlayManager mgr = LogcatOverlayManager.getInstance();
                    if (mgr != null) mgr.refreshVisibility();
                } catch (Throwable ignored) {}
            });

            try {
                String localVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                addActionButton(
                        getString(R.string.version_prefix) + localVersion,
                        getString(R.string.check_update),
                        v -> new GithubReleaseUpdater(this, "LiteLDev", "LeviLaunchroid", permissionResultLauncher).checkUpdate()
                );
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }));

        settingsRecyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(settingsRecyclerView));
    }

    private void addSwitchItem(String label, boolean defChecked, Switch.OnCheckedChangeListener listener) {
        View ll = LayoutInflater.from(this).inflate(R.layout.item_settings_switch, settingsItemsContainer, false);
        ((TextView) ll.findViewById(R.id.tv_title)).setText(label);
        Switch sw = ll.findViewById(R.id.switch_value);
        sw.setChecked(defChecked);
        if (listener != null) sw.setOnCheckedChangeListener(listener);
        settingsItemsContainer.addView(ll);
    }

    private Spinner addSpinnerItem(String label, String[] options, int defaultIdx) {
        View ll = LayoutInflater.from(this).inflate(R.layout.item_settings_spinner, settingsItemsContainer, false);
        ((TextView) ll.findViewById(R.id.tv_title)).setText(label);
        Spinner spinner = ll.findViewById(R.id.spinner_value);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, options);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setPopupBackgroundResource(R.drawable.bg_popup_menu_rounded);
        DynamicAnim.applyPressScale(spinner);
        spinner.setSelection(defaultIdx);
        settingsItemsContainer.addView(ll);
        return spinner;
    }

    private void addActionButton(String label, String buttonText, View.OnClickListener listener) {
        View ll = LayoutInflater.from(this).inflate(R.layout.item_settings_button, settingsItemsContainer, false);
        ((TextView) ll.findViewById(R.id.tv_title)).setText(label);
        Button btn = ll.findViewById(R.id.btn_action);
        btn.setText(buttonText);
        btn.setOnClickListener(listener);
        settingsItemsContainer.addView(ll);
    }

    private void addThemeSelectorItem(ThemeManager themeManager) {
        String[] themeOptions = {
                getString(R.string.theme_follow_system),
                getString(R.string.theme_light),
                getString(R.string.theme_dark)
        };
        int currentMode = themeManager.getCurrentMode();
        Spinner spinner = addSpinnerItem(getString(R.string.theme_title), themeOptions, currentMode);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                themeManager.setThemeMode(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void addLanguageSelectorItem(LanguageManager languageManager) {
        String[] languageOptions = {
                getString(R.string.english),
                getString(R.string.chinese),
                getString(R.string.russian)
        };
        String currentCode = languageManager.getCurrentLanguage();
        int defaultIdx = switch (currentCode) {
            case "zh", "zh-CN" -> 1;
            case "ru" -> 2;
            default -> 0; 
        };
        Spinner spinner = addSpinnerItem(getString(R.string.language), languageOptions, defaultIdx);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String code = switch (position) {
                    case 1 -> "zh-CN";
                    case 2 -> "ru";
                    default -> "en";
                };
                if (!code.equals(languageManager.getCurrentLanguage())) {
                    languageManager.setAppLanguage(code);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}
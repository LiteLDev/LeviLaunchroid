package org.levimc.launcher.ui.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.levimc.launcher.R;
import org.levimc.launcher.settings.FeatureSettings;
import org.levimc.launcher.ui.adapter.SettingsAdapter;
import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;
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
    private int updateButtonTapCount = 0;
    private long lastUpdateButtonTapTime = 0;
    private static final int EASTER_EGG_TAP_COUNT = 3;
    private static final long TAP_TIMEOUT_MS = 2000;

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
            addMemoryEditorSwitchItem(fs);
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
                        v -> handleUpdateButtonClick()
                );
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }));

        settingsRecyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(settingsRecyclerView));
    }

    private void handleUpdateButtonClick() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastUpdateButtonTapTime > TAP_TIMEOUT_MS) {
            updateButtonTapCount = 0;
        }
        
        updateButtonTapCount++;
        lastUpdateButtonTapTime = currentTime;
        
        if (updateButtonTapCount >= EASTER_EGG_TAP_COUNT) {
            updateButtonTapCount = 0;
            triggerEasterEgg();
        } else {
            new GithubReleaseUpdater(this, "LiteLDev", "LeviLaunchroid", permissionResultLauncher).checkUpdate();
        }
    }

    private void triggerEasterEgg() {
        try {
            String encoded = "aHR0cHM6Ly95b3V0dS5iZS9GdHV0TEE2M0NwOD9zaT1CSExEWHZLOTZPZ1A0NUI4";
            String url = new String(Base64.decode(encoded, Base64.DEFAULT));
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addSwitchItem(String label, boolean defChecked, SwitchMaterial.OnCheckedChangeListener listener) {
        View ll = LayoutInflater.from(this).inflate(R.layout.item_settings_switch, settingsItemsContainer, false);
        ((TextView) ll.findViewById(R.id.tv_title)).setText(label);
        SwitchMaterial sw = ll.findViewById(R.id.switch_value);
        sw.setChecked(defChecked);
        if (listener != null) sw.setOnCheckedChangeListener(listener);
        settingsItemsContainer.addView(ll);
    }

    private void addMemoryEditorSwitchItem(FeatureSettings fs) {
        View ll = LayoutInflater.from(this).inflate(R.layout.item_settings_switch, settingsItemsContainer, false);
        ((TextView) ll.findViewById(R.id.tv_title)).setText(getString(R.string.memory_editor_enable));
        SwitchMaterial sw = ll.findViewById(R.id.switch_value);
        sw.setChecked(fs.isMemoryEditorEnabled());
        
        sw.setOnCheckedChangeListener((btn, checked) -> {
            if (checked && !fs.isMemoryEditorEnabled()) {
                sw.setChecked(false);
                showMemoryEditorWarningDialog(sw, fs);
            } else if (!checked) {
                fs.setMemoryEditorEnabled(false);
            }
        });
        settingsItemsContainer.addView(ll);
    }

    private void showMemoryEditorWarningDialog(SwitchMaterial sw, FeatureSettings fs) {
        CustomAlertDialog dialog = new CustomAlertDialog(this);
        final int[] countdown = {5};
        final String confirmText = getString(R.string.confirm);
        
        dialog.setTitleText(getString(R.string.memory_editor_warning_title))
              .setMessage(getString(R.string.memory_editor_warning_message))
              .setUseBorderedBackground(true)
              .setBlurBackground(true)
              .setPositiveButton(confirmText + " (" + countdown[0] + ")", null)
              .setNegativeButton(getString(R.string.cancel), null);
        
        dialog.show();
        
        Button positiveBtn = dialog.getPositiveButton();
        if (positiveBtn != null) {
            positiveBtn.setEnabled(false);
            positiveBtn.setAlpha(0.5f);
        }
        
        CountDownTimer timer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdown[0] = (int) (millisUntilFinished / 1000) + 1;
                if (positiveBtn != null) {
                    positiveBtn.setText(confirmText + " (" + countdown[0] + ")");
                }
            }

            @Override
            public void onFinish() {
                if (positiveBtn != null) {
                    positiveBtn.setText(confirmText);
                    positiveBtn.setEnabled(true);
                    positiveBtn.setAlpha(1.0f);
                    positiveBtn.setOnClickListener(v -> {
                        fs.setMemoryEditorEnabled(true);
                        sw.setChecked(true);
                        dialog.dismiss();
                    });
                }
            }
        };
        timer.start();
        
        dialog.setOnDismissListener(d -> timer.cancel());
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
                getString(R.string.russian),
                getString(R.string.indonesian)
        };
        String currentCode = languageManager.getCurrentLanguage();
        int defaultIdx = switch (currentCode) {
            case "zh", "zh-CN" -> 1;
            case "ru" -> 2;
            case "idn" -> 3;
            default -> 0; 
        };
        Spinner spinner = addSpinnerItem(getString(R.string.language), languageOptions, defaultIdx);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String code = switch (position) {
                    case 1 -> "zh-CN";
                    case 2 -> "ru";
                    case 3 -> "idn";
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
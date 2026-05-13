package org.levimc.launcher.ui.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.levimc.launcher.R;
import org.levimc.launcher.settings.FeatureSettings;
import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.ui.dialogs.LogcatOverlayManager;
import org.levimc.launcher.util.GithubReleaseUpdater;
import org.levimc.launcher.util.LanguageManager;
import org.levimc.launcher.util.PermissionsHandler;
import org.levimc.launcher.util.PersonalizationManager;
import org.levimc.launcher.util.ThemeManager;

public class SettingsActivity extends BaseActivity {

    private PermissionsHandler permissionsHandler;
    private ActivityResultLauncher<Intent> permissionResultLauncher;
    private ActivityResultLauncher<Intent> bgImagePickerLauncher;
    private int updateButtonTapCount = 0;
    private long lastUpdateButtonTapTime = 0;
    private static final int EASTER_EGG_TAP_COUNT = 3;
    private static final long TAP_TIMEOUT_MS = 2000;

    private TextView tabBasic;
    private TextView tabPersonalize;
    private TextView tabUpdates;
    private TextView tabAbout;

    private View sectionBasic;
    private View sectionPersonalize;
    private View sectionUpdates;
    private View sectionAbout;

    private PersonalizationManager personalizationManager;
    private LinearLayout colorGridContainer;
    private LinearLayout moreColorsContainer;
    private TextView bgImageStatus;
    private ImageView bgImagePreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        DynamicAnim.applyPressScaleRecursively(findViewById(android.R.id.content));

        setupNavBar();

        personalizationManager = new PersonalizationManager(this);

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

        bgImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            personalizationManager.setBackgroundImage(uri, this);
                            updateBgImageUI();
                            recreate();
                        }
                    }
                }
        );

        initTabs();
        setupBasicSection();
        setupPersonalizeSection();
        setupUpdatesSection();
        setupAboutSection();

        selectTab(tabBasic);
    }

    private void initTabs() {
        tabBasic = findViewById(R.id.tab_basic);
        tabPersonalize = findViewById(R.id.tab_personalize);
        tabUpdates = findViewById(R.id.tab_updates);
        tabAbout = findViewById(R.id.tab_about);

        sectionBasic = findViewById(R.id.section_basic);
        sectionPersonalize = findViewById(R.id.section_personalize);
        sectionUpdates = findViewById(R.id.section_updates);
        sectionAbout = findViewById(R.id.section_about);

        tabBasic.setOnClickListener(v -> selectTab(tabBasic));
        tabPersonalize.setOnClickListener(v -> selectTab(tabPersonalize));
        tabUpdates.setOnClickListener(v -> selectTab(tabUpdates));
        tabAbout.setOnClickListener(v -> selectTab(tabAbout));
    }

    private void selectTab(TextView selectedTab) {
        TextView[] tabs = {tabBasic, tabPersonalize, tabUpdates, tabAbout};
        View[] sections = {sectionBasic, sectionPersonalize, sectionUpdates, sectionAbout};

        int accent = personalizationManager.getAccentColor();

        for (int i = 0; i < tabs.length; i++) {
            boolean isSelected = tabs[i] == selectedTab;

            if (isSelected) {
                if (accent != 0) {
                    android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                    gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                    gd.setColor(accent);
                    gd.setCornerRadius(16 * getResources().getDisplayMetrics().density);
                    tabs[i].setBackground(gd);
                } else {
                    tabs[i].setBackgroundResource(R.drawable.bg_tab_selected);
                }
                tabs[i].setTextColor(Color.WHITE);
                tabs[i].setTextSize(13);
            } else {
                tabs[i].setBackgroundResource(R.drawable.bg_tab_unselected);
                tabs[i].setTextColor(getColor(R.color.text_secondary));
            }

            if (isSelected) {
                sections[i].setVisibility(View.VISIBLE);
                sections[i].setAlpha(0f);
                sections[i].animate().alpha(1f).setDuration(200).start();
            } else {
                sections[i].setVisibility(View.GONE);
            }
        }
    }

    private void setupBasicSection() {
        LanguageManager languageManager = new LanguageManager(this);
        FeatureSettings fs = FeatureSettings.getInstance();

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

        TextView languageCurrent = findViewById(R.id.language_current);
        languageCurrent.setText(languageOptions[defaultIdx]);

        Spinner languageSpinner = findViewById(R.id.language_spinner);
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, languageOptions);
        langAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        languageSpinner.setAdapter(langAdapter);
        languageSpinner.setPopupBackgroundResource(R.drawable.bg_popup_menu_rounded);
        languageSpinner.setSelection(defaultIdx);
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
                languageCurrent.setText(languageOptions[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        SwitchMaterial switchLogcat = findViewById(R.id.switch_logcat);
        switchLogcat.setChecked(fs.isLogcatOverlayEnabled());
        switchLogcat.setOnCheckedChangeListener((btn, checked) -> {
            fs.setLogcatOverlayEnabled(checked);
            try {
                LogcatOverlayManager mgr = LogcatOverlayManager.getInstance();
                if (mgr != null) mgr.refreshVisibility();
            } catch (Throwable ignored) {}
        });

        SwitchMaterial switchManagedLogin = findViewById(R.id.switch_managed_login);
        switchManagedLogin.setChecked(fs.isLauncherManagedMcLoginEnabled());
        switchManagedLogin.setOnCheckedChangeListener((btn, checked) -> fs.setLauncherManagedMcLoginEnabled(checked));
    }

    private void setupPersonalizeSection() {
        ThemeManager themeManager = new ThemeManager(this);

        String[] themeOptions = {
                getString(R.string.theme_follow_system),
                getString(R.string.theme_light),
                getString(R.string.theme_dark)
        };

        Spinner themeSpinner = findViewById(R.id.theme_spinner);
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, themeOptions);
        themeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        themeSpinner.setAdapter(themeAdapter);
        themeSpinner.setPopupBackgroundResource(R.drawable.bg_popup_menu_rounded);
        themeSpinner.setSelection(themeManager.getCurrentMode());
        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                themeManager.setThemeMode(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        setupColorPicker();
        setupBackgroundImagePicker();
        setupBaseMode();
    }

    private void setupColorPicker() {
        colorGridContainer = findViewById(R.id.color_preset_grid);
        moreColorsContainer = findViewById(R.id.color_more_grid);

        if (colorGridContainer == null || moreColorsContainer == null) return;

        int currentAccent = personalizationManager.getAccentColor();

        buildColorGrid(colorGridContainer, PersonalizationManager.PRESET_COLORS, currentAccent);
        buildColorGrid(moreColorsContainer, PersonalizationManager.MORE_COLORS, currentAccent);
    }

    private void buildColorGrid(LinearLayout container, int[] colors, int selectedColor) {
        container.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        int circleSize = (int) (32 * density);
        int margin = (int) (4 * density);
        int checkSize = (int) (14 * density);

        int columns = 15;
        int index = 0;
        while (index < colors.length) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            for (int col = 0; col < columns && index < colors.length; col++, index++) {
                int color = colors[index];

                FrameLayout wrapper = new FrameLayout(this);
                LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(circleSize, circleSize);
                wrapParams.setMargins(margin, margin, margin, margin);
                wrapper.setLayoutParams(wrapParams);

                View circle = new View(this);
                circle.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                GradientDrawable circleDrawable = new GradientDrawable();
                circleDrawable.setShape(GradientDrawable.OVAL);
                circleDrawable.setColor(color);
                if (color == selectedColor) {
                    circleDrawable.setStroke((int) (2 * density), Color.WHITE);
                }
                circle.setBackground(circleDrawable);
                wrapper.addView(circle);

                if (color == selectedColor) {
                    ImageView check = new ImageView(this);
                    FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(checkSize, checkSize);
                    checkParams.gravity = Gravity.CENTER;
                    check.setLayoutParams(checkParams);
                    check.setImageResource(R.drawable.ic_check);
                    check.setColorFilter(Color.WHITE);
                    wrapper.addView(check);
                }

                wrapper.setClickable(true);
                wrapper.setFocusable(true);
                final int finalColor = color;
                wrapper.setOnClickListener(v -> {
                    personalizationManager.setAccentColor(finalColor);
                    recreate();
                });
                DynamicAnim.applyPressScale(wrapper);

                row.addView(wrapper);
            }

            container.addView(row);
        }

        if (selectedColor != 0) {
            LinearLayout resetRow = new LinearLayout(this);
            resetRow.setOrientation(LinearLayout.HORIZONTAL);
            resetRow.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams resetRowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            resetRowParams.topMargin = (int) (8 * density);
            resetRow.setLayoutParams(resetRowParams);

            boolean alreadyHasReset = container.getTag() != null && container.getTag().equals("has_reset");
            if (!alreadyHasReset && container == moreColorsContainer) {
                FrameLayout resetWrapper = new FrameLayout(this);
                LinearLayout.LayoutParams rwParams = new LinearLayout.LayoutParams(circleSize, circleSize);
                rwParams.setMargins(margin, margin, margin, margin);
                resetWrapper.setLayoutParams(rwParams);

                View resetCircle = new View(this);
                resetCircle.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                GradientDrawable resetDrawable = new GradientDrawable();
                resetDrawable.setShape(GradientDrawable.OVAL);
                boolean isDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                resetDrawable.setColor(isDark ? Color.argb(80, 255, 255, 255) : Color.argb(80, 0, 0, 0));
                resetDrawable.setStroke((int) (1 * density), isDark ? Color.argb(100, 255, 255, 255) : Color.argb(100, 0, 0, 0));
                resetCircle.setBackground(resetDrawable);
                resetWrapper.addView(resetCircle);

                ImageView resetIcon = new ImageView(this);
                FrameLayout.LayoutParams riParams = new FrameLayout.LayoutParams(checkSize, checkSize);
                riParams.gravity = Gravity.CENTER;
                resetIcon.setLayoutParams(riParams);
                resetIcon.setImageResource(R.drawable.ic_close);
                resetIcon.setColorFilter(isDark ? Color.WHITE : Color.BLACK);
                resetWrapper.addView(resetIcon);

                resetWrapper.setClickable(true);
                resetWrapper.setFocusable(true);
                resetWrapper.setOnClickListener(v -> {
                    personalizationManager.clearAccentColor();
                    recreate();
                });
                DynamicAnim.applyPressScale(resetWrapper);

                resetRow.addView(resetWrapper);
                container.addView(resetRow);
                container.setTag("has_reset");
            }
        }
    }

    private void setupBackgroundImagePicker() {
        bgImageStatus = findViewById(R.id.bg_image_status);
        bgImagePreview = findViewById(R.id.bg_image_preview);
        Button btnSelectImage = findViewById(R.id.btn_select_bg_image);
        Button btnClearImage = findViewById(R.id.btn_clear_bg_image);

        if (btnSelectImage == null) return;

        updateBgImageUI();

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            bgImagePickerLauncher.launch(intent);
        });

        if (btnClearImage != null) {
            btnClearImage.setOnClickListener(v -> {
                personalizationManager.clearBackgroundImage();
                updateBgImageUI();
                recreate();
            });
        }
    }

    private void updateBgImageUI() {
        if (bgImageStatus == null) return;
        if (personalizationManager.hasBackgroundImage()) {
            bgImageStatus.setText(R.string.bg_image_selected);
            if (bgImagePreview != null) {
                android.graphics.Bitmap bmp = personalizationManager.loadBackgroundBitmap();
                if (bmp != null) {
                    bgImagePreview.setImageBitmap(bmp);
                    bgImagePreview.setVisibility(View.VISIBLE);
                }
            }
            View btnClear = findViewById(R.id.btn_clear_bg_image);
            if (btnClear != null) btnClear.setVisibility(View.VISIBLE);
        } else {
            bgImageStatus.setText(R.string.bg_image_none);
            if (bgImagePreview != null) {
                bgImagePreview.setImageDrawable(null);
                bgImagePreview.setVisibility(View.GONE);
            }
            View btnClear = findViewById(R.id.btn_clear_bg_image);
            if (btnClear != null) btnClear.setVisibility(View.GONE);
        }
    }

    private void setupBaseMode() {
        LinearLayout baseModeContainer = findViewById(R.id.base_mode_container);
        if (baseModeContainer == null) return;

        TextView btnNone = findViewById(R.id.base_mode_none);
        TextView btnFollow = findViewById(R.id.base_mode_follow);
        TextView btnCustom = findViewById(R.id.base_mode_custom);

        if (btnNone == null || btnFollow == null || btnCustom == null) return;

        int currentMode = personalizationManager.getBaseMode();
        updateBaseModeButtons(btnNone, btnFollow, btnCustom, currentMode);

        btnNone.setOnClickListener(v -> {
            personalizationManager.setBaseMode(PersonalizationManager.BASE_MODE_NONE);
            updateBaseModeButtons(btnNone, btnFollow, btnCustom, PersonalizationManager.BASE_MODE_NONE);
        });
        btnFollow.setOnClickListener(v -> {
            personalizationManager.setBaseMode(PersonalizationManager.BASE_MODE_FOLLOW_THEME);
            updateBaseModeButtons(btnNone, btnFollow, btnCustom, PersonalizationManager.BASE_MODE_FOLLOW_THEME);
        });
        btnCustom.setOnClickListener(v -> {
            personalizationManager.setBaseMode(PersonalizationManager.BASE_MODE_CUSTOM);
            updateBaseModeButtons(btnNone, btnFollow, btnCustom, PersonalizationManager.BASE_MODE_CUSTOM);
        });
    }

    private void updateBaseModeButtons(TextView btnNone, TextView btnFollow, TextView btnCustom, int selectedMode) {
        TextView[] buttons = {btnNone, btnFollow, btnCustom};
        int[] modes = {PersonalizationManager.BASE_MODE_NONE, PersonalizationManager.BASE_MODE_FOLLOW_THEME, PersonalizationManager.BASE_MODE_CUSTOM};

        int accent = personalizationManager.getAccentColor();

        for (int i = 0; i < buttons.length; i++) {
            boolean isSelected = modes[i] == selectedMode;

            if (isSelected) {
                if (accent != 0) {
                    GradientDrawable gd = new GradientDrawable();
                    gd.setShape(GradientDrawable.RECTANGLE);
                    gd.setColor(accent);
                    gd.setCornerRadius(16 * getResources().getDisplayMetrics().density);
                    buttons[i].setBackground(gd);
                } else {
                    buttons[i].setBackgroundResource(R.drawable.bg_tab_selected);
                }
                buttons[i].setTextColor(Color.WHITE);
            } else {
                buttons[i].setBackgroundResource(R.drawable.bg_tab_unselected);
                buttons[i].setTextColor(getColor(R.color.text_secondary));
            }
        }
    }

    private void setupUpdatesSection() {
        try {
            String localVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            TextView versionText = findViewById(R.id.version_text);
            versionText.setText(getString(R.string.version_prefix) + localVersion);
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        Button btnCheckUpdate = findViewById(R.id.btn_check_update);
        btnCheckUpdate.setOnClickListener(v -> handleUpdateButtonClick());
    }

    private void setupAboutSection() {
        findViewById(R.id.settings_btn_github).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/LiteLDev/LeviLaunchroid"))));

        findViewById(R.id.settings_btn_discord).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/jsnzw4ueAt"))));
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

    private void setupNavBar() {
        setActiveNavTab(R.id.nav_tab_settings);
        findViewById(R.id.nav_tab_settings).setOnClickListener(v -> {});
    }
}
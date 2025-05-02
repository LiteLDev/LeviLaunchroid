package org.levimc.launcher.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import org.levimc.launcher.R;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.core.versions.VersionManager;
import org.levimc.launcher.databinding.ActivityMainBinding;

import org.levimc.launcher.service.LogOverlay;
import org.levimc.launcher.settings.FeatureSettings;
import org.levimc.launcher.ui.dialogs.GameVersionSelectDialog;
import org.levimc.launcher.ui.dialogs.SettingsDialog;
import org.levimc.launcher.ui.dialogs.gameversionselect.BigGroup;
import org.levimc.launcher.ui.dialogs.gameversionselect.VersionUtil;
import org.levimc.launcher.ui.views.MainViewModel;
import org.levimc.launcher.ui.views.MainViewModelFactory;
import org.levimc.launcher.util.ApkImportManager;
import org.levimc.launcher.core.minecraft.MinecraftLauncher;
import org.levimc.launcher.core.mods.FileHandler;
import org.levimc.launcher.core.mods.Mod;
import org.levimc.launcher.ui.animation.AnimationHelper;
import org.levimc.launcher.util.LanguageManager;
import org.levimc.launcher.util.Logger;
import org.levimc.launcher.util.PermissionsHandler;
import org.levimc.launcher.util.ResourcepackHandler;
import org.levimc.launcher.util.ThemeManager;
import org.levimc.launcher.util.UIHelper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity  {

    static {
        System.loadLibrary("leviutils");
    }


    private ActivityMainBinding binding;
    private MinecraftLauncher minecraftLauncher;
    private LanguageManager languageManager;
    private ThemeManager themeManager;
    private PermissionsHandler permissionsHandler;
    private FileHandler fileHandler;
    private ApkImportManager apkImportManager;
    private MainViewModel viewModel;
    private VersionManager versionManager;
    private GameVersion currentVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        languageManager = new LanguageManager(this);
        languageManager.applySavedLanguage();
        apkImportManager = new ApkImportManager(this, viewModel);
        //themeManager = new ThemeManager(this);
        //themeManager.applyTheme();

        versionManager = VersionManager.get(this);
        versionManager.loadAllVersions(); // Ensure versions are loaded
        currentVersion = versionManager.getSelectedVersion(); // May be null

        // Initialize UI and other components that don't depend on currentVersion
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up view model
        viewModel = new ViewModelProvider(
                this,
                new MainViewModelFactory(getApplication())
        ).get(MainViewModel.class);
        viewModel.getModsLiveData().observe(this, this::updateModsUI);

        UIHelper.setTransparentNavigationBar(this);

        minecraftLauncher = new MinecraftLauncher(this, getClassLoader());

        initListeners();
        AnimationHelper.prepareInitialStates(binding);
        AnimationHelper.runInitializationSequence(binding);

        // binding.themeSwitch.setChecked(themeManager.isDarkMode());

        permissionsHandler = PermissionsHandler.getInstance();
        permissionsHandler.setActivity(this);

        permissionsHandler.requestPermission(PermissionsHandler.PermissionType.STORAGE, new PermissionsHandler.PermissionResultCallback() {
            @Override
            public void onPermissionGranted(PermissionsHandler.PermissionType type) {
                if (type == PermissionsHandler.PermissionType.STORAGE) {
                    viewModel.refreshMods();
                }
            }
            @Override
            public void onPermissionDenied(PermissionsHandler.PermissionType type, boolean permanentlyDenied) {
                if (type == PermissionsHandler.PermissionType.STORAGE) {
                    Toast.makeText(MainActivity.this, R.string.storage_permission_not_granted, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });

        fileHandler = new FileHandler(this, viewModel, versionManager); // handles null version

        // Update UI with version info
        setTextMinecraftVersion();

        // Only set current version in view model if we have one
        if (currentVersion != null) {
            viewModel.setCurrentVersion(currentVersion);
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        ResourcepackHandler resourcepackHandler = new ResourcepackHandler(
                this,
                minecraftLauncher,
                executorService,
                binding.progressLoader,
                binding.launchButton
        );
        resourcepackHandler.checkIntentForResourcepack();
        handleIncomingFiles();
        initSettings();
    }

    private void initSettings() {
        if (FeatureSettings.getInstance().isDebugLogDialogEnabled()) {
            LogOverlay.getInstance(this).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        permissionsHandler.onActivityResult(requestCode, resultCode, data);
        apkImportManager.handleActivityResult(requestCode, resultCode, data);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @SuppressLint({"ClickableViewAccessibility", "UnsafeIntentLaunch"})
    private void initListeners() {
        binding.launchButton.setOnClickListener(v -> {
            binding.launchButton.setEnabled(false);
            binding.progressLoader.setVisibility(View.VISIBLE);
            new Thread(() -> {
                new MinecraftLauncher(this, getClassLoader())
                        .launch(getIntent(), versionManager.getSelectedVersion());
                runOnUiThread(() -> {
                    binding.progressLoader.setVisibility(View.GONE);
                    binding.launchButton.setEnabled(true);
                });
            }).start();
        });

        binding.launchButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setInterpolator(new OvershootInterpolator()).setDuration(200).start();
            }
            return false;
        });

        binding.languageButton.setOnClickListener(v -> languageManager.showLanguageMenu(v));

        binding.selectVersionButton.setOnClickListener(v -> {
            versionManager.loadAllVersions();

            List<GameVersion> installedList = versionManager.getInstalledVersions();
            List<GameVersion> customList = versionManager.getCustomVersions();

            List<BigGroup> bigGroups = VersionUtil.buildBigGroups(installedList, customList);

            GameVersionSelectDialog dialog = new GameVersionSelectDialog(this, bigGroups);
            dialog.setOnVersionSelectListener(version -> {
                currentVersion = version;
                versionManager.selectVersion(version);
                viewModel.setCurrentVersion(version);
                setTextMinecraftVersion();
            });
            dialog.show();
        });

        binding.importApkButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/vnd.android.package-archive");
            startActivityForResult(intent, 1004);
        });

        FeatureSettings.init(getApplicationContext());

        binding.settingsButton.setOnClickListener(v -> showSettingsDialog());
        //binding.themeSwitch.setOnCheckedChangeListener((button, checked) -> themeManager.toggleTheme(checked));
    }

    private void showSettingsDialog() {
        FeatureSettings fs = FeatureSettings.getInstance();
        SettingsDialog dlg = new SettingsDialog(this);
        Switch swEnable = dlg.addSwitchItem(getString(R.string.enable_debug_log), fs.isDebugLogDialogEnabled());
        swEnable.setOnCheckedChangeListener((btn, checked) -> fs.setDebugLogDialogEnabled(checked));

        dlg.setOnCancelListener((View.OnClickListener) v -> dlg.dismiss());
        dlg.setOnConfirmListener(v -> dlg.dismiss());

        dlg.show();
    }

    private void setTextMinecraftVersion() {
        if (currentVersion != null) {
            binding.textMinecraftVersion.setText(currentVersion.displayName);
        } else {
            binding.textMinecraftVersion.setText(getString(R.string.not_found_version));
        }
    }

    private void handleIncomingFiles() {
        fileHandler.processIncomingFilesWithConfirmation(getIntent(), new FileHandler.FileOperationCallback() {
            @Override
            public void onSuccess(int processedFiles) {
                if (processedFiles > 0) {
                    UIHelper.showToast(MainActivity.this,
                            getString(R.string.files_processed, processedFiles));
                }
            }

            @Override
            public void onError(String errorMessage) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.error)
                        .setMessage(errorMessage)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }

            @Override
            public void onProgressUpdate(int progress) {
                binding.progressLoader.setProgress(progress);
            }
        });
    }
    private void updateModsUI(List<Mod> mods) {
        binding.modContent.removeAllViews();
        if (mods == null || mods.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No Mods Found");
            binding.modContent.addView(tv);
            return;
        }
        String formattedTitle = getString(R.string.mods_title, mods.size());
        binding.modsTitleText.setText(formattedTitle);
        for (Mod mod : mods) {
            binding.modContent.addView(createModView(mod));
        }
    }

    private View createModView(Mod mod) {
        LayoutInflater inflater = LayoutInflater.from(this);

        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.item_mod, null);
        TextView name = view.findViewById(R.id.mod_name);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switchBtn = view.findViewById(R.id.mod_switch);

        name.setText(mod.getDisplayName());
        switchBtn.setChecked(mod.isEnabled());

        switchBtn.setOnCheckedChangeListener((btn, isChecked) -> {
            viewModel.setModEnabled(mod.getFileName(), isChecked);
        });
        return view;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
package org.levimc.launcher.ui.activities;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.minecraft.MinecraftLauncher;
import org.levimc.launcher.core.mods.FileHandler;
import org.levimc.launcher.core.mods.Mod;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.core.versions.VersionManager;
import org.levimc.launcher.databinding.ActivityMainBinding;
import org.levimc.launcher.service.LogOverlay;
import org.levimc.launcher.settings.FeatureSettings;
import org.levimc.launcher.ui.adapter.ModsAdapter;
import org.levimc.launcher.ui.animation.AnimationHelper;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;
import org.levimc.launcher.ui.dialogs.GameVersionSelectDialog;
import org.levimc.launcher.ui.dialogs.SettingsDialog;
import org.levimc.launcher.ui.dialogs.gameversionselect.BigGroup;
import org.levimc.launcher.ui.dialogs.gameversionselect.VersionUtil;
import org.levimc.launcher.ui.views.MainViewModel;
import org.levimc.launcher.ui.views.MainViewModelFactory;
import org.levimc.launcher.util.ApkImportManager;
import org.levimc.launcher.util.GithubReleaseUpdater;
import org.levimc.launcher.util.LanguageManager;
import org.levimc.launcher.util.PermissionsHandler;
import org.levimc.launcher.util.ResourcepackHandler;
import org.levimc.launcher.util.ThemeManager;
import org.levimc.launcher.util.UIHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity {

    static {
        System.loadLibrary("leviutils");
    }

    private ActivityMainBinding binding;
    private MinecraftLauncher minecraftLauncher;
    private LanguageManager languageManager;
    private PermissionsHandler permissionsHandler;
    private FileHandler fileHandler;
    private ApkImportManager apkImportManager;
    private MainViewModel viewModel;
    private VersionManager versionManager;
    private ActivityResultLauncher<Intent> permissionResultLauncher;
    private ActivityResultLauncher<Intent> apkImportResultLauncher;
    private ActivityResultLauncher<Intent> soImportResultLauncher;
    private ModsAdapter modsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupManagersAndHandlers();

        AnimationHelper.prepareInitialStates(binding);
        AnimationHelper.runInitializationSequence(binding);

        setTextMinecraftVersion();
        updateViewModelVersion();


        checkResourcepack();
        handleIncomingFiles();
        initSettingsUi();

        new GithubReleaseUpdater(this, "LiteLDev", "LeviLaunchroid", permissionResultLauncher).checkUpdateOnLaunch();

        repairNeededVersions();

        requestBasicPermissions();

        showEulaIfNeeded();

        initModsRecycler();
    }

    private void setupManagersAndHandlers() {
        languageManager = new LanguageManager(this);
        languageManager.applySavedLanguage();

        viewModel = new ViewModelProvider(this, new MainViewModelFactory(getApplication())).get(MainViewModel.class);
        viewModel.getModsLiveData().observe(this, this::updateModsUI);

        versionManager = VersionManager.get(this);
        versionManager.loadAllVersions();

        apkImportManager = new ApkImportManager(this, viewModel);
        minecraftLauncher = new MinecraftLauncher(this, getClassLoader());
        fileHandler = new FileHandler(this, viewModel, versionManager);

        permissionResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (permissionsHandler != null)
                        permissionsHandler.onActivityResult(result.getResultCode(), result.getData());
                }
        );

        apkImportResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (apkImportManager != null)
                        apkImportManager.handleActivityResult(result.getResultCode(), result.getData());
                }
        );

        soImportResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && fileHandler != null) {
                        fileHandler.processIncomingFilesWithConfirmation(result.getData(), new FileHandler.FileOperationCallback() {
                            @Override
                            public void onSuccess(int processedFiles) {
                                UIHelper.showToast(MainActivity.this, getString(R.string.files_processed, processedFiles));
                            }

                            @Override
                            public void onError(String errorMessage) {
                            }

                            @Override
                            public void onProgressUpdate(int progress) {
                                if (binding != null) binding.progressLoader.setProgress(progress);
                            }
                        }, true);
                    }
                }
        );

        permissionsHandler = PermissionsHandler.getInstance();
        permissionsHandler.setActivity(this, permissionResultLauncher);

        initListeners();
    }

    private void initModsRecycler() {
        modsAdapter = new ModsAdapter(new ArrayList<>());
        binding.modsRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.modsRecycler.setAdapter(modsAdapter);
        modsAdapter.setOnModEnableChangeListener((mod, enabled) -> {
            if (viewModel != null) viewModel.setModEnabled(mod.getFileName(), enabled);
        });

        modsAdapter.setOnModReorderListener(reorderedMods -> {
            if (viewModel != null) {
                viewModel.reorderMods(reorderedMods);
                runOnUiThread(() ->
                    Toast.makeText(this, R.string.mod_reordered, Toast.LENGTH_SHORT).show()
                );
            }
        });

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                modsAdapter.moveItem(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                Mod mod = modsAdapter.getItem(pos);
                new CustomAlertDialog(MainActivity.this)
                        .setTitleText(getString(R.string.dialog_title_delete_mod))
                        .setMessage(getString(R.string.dialog_message_delete_mod))
                        .setPositiveButton(getString(R.string.dialog_positive_delete), v -> {
                            viewModel.removeMod(mod);
                            modsAdapter.removeAt(pos);
                        })
                        .setNegativeButton(getString(R.string.dialog_negative_cancel), v -> {
                            modsAdapter.notifyItemChanged(pos);
                        })
                        .show();
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(binding.modsRecycler);

        viewModel.getModsLiveData().observe(this, this::updateModsUI);
    }

    private void updateViewModelVersion() {
        GameVersion selectedVersion = versionManager.getSelectedVersion();
        if (selectedVersion != null) {
            viewModel.setCurrentVersion(selectedVersion);
        }
    }

    private void checkResourcepack() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        new ResourcepackHandler(
                this,
                minecraftLauncher,
                executorService,
                binding.progressLoader,
                binding.launchButton
        ).checkIntentForResourcepack();
    }

    private void repairNeededVersions() {
        for (GameVersion version : versionManager.getCustomVersions()) {
            if (version.needsRepair) {
                VersionManager.attemptRepairLibs(this, version);
            }
        }
    }

    private void requestBasicPermissions() {
        permissionsHandler.requestPermission(PermissionsHandler.PermissionType.STORAGE,
                new PermissionsHandler.PermissionResultCallback() {
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
                }
        );
    }

    private void showEulaIfNeeded() {
        SharedPreferences prefs = getSharedPreferences("LauncherPrefs", MODE_PRIVATE);
        if (!prefs.getBoolean("eula_accepted", false)) {
            showEulaDialog();
        }
    }

    private void showEulaDialog() {
        CustomAlertDialog dia = new CustomAlertDialog(this)
                .setTitleText(getString(R.string.eula_title))
                .setMessage(getString(R.string.eula_message))
                .setPositiveButton(getString(R.string.eula_agree), v -> {
                    getSharedPreferences("LauncherPrefs", MODE_PRIVATE)
                            .edit().putBoolean("eula_accepted", true).apply();
                })
                .setNegativeButton(getString(R.string.eula_exit), v -> finishAffinity());
        dia.setCancelable(false);
        dia.show();
    }

    private void updateAbiLabel() {
        if (binding == null) return;
        TextView abiLabel = binding.abiLabel;
        String abiList = (versionManager.getSelectedVersion() != null) ? versionManager.getSelectedVersion().abiList : null;
        String abiToShow = "unknown";
        if (!TextUtils.isEmpty(abiList) && !"unknown".equals(abiList)) {
            abiToShow = abiList.split("\\n")[0].trim();
        }
        abiLabel.setText(abiToShow);
        int bgRes = switch (abiToShow) {
            case "arm64-v8a" -> R.drawable.bg_abi_arm64_v8a;
            case "armeabi-v7a" -> R.drawable.bg_abi_armeabi_v7a;
            case "x86" -> R.drawable.bg_abi_x86;
            case "x86_64" -> R.drawable.bg_abi_x86_64;
            default -> R.drawable.bg_abi_default;
        };
        abiLabel.setBackgroundResource(bgRes);
    }

    private void initSettingsUi() {
        FeatureSettings fs = FeatureSettings.getInstance();
        if (fs.isDebugLogDialogEnabled()) {
            LogOverlay.getInstance(this).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionsHandler != null) {
            permissionsHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @SuppressLint({"ClickableViewAccessibility", "UnsafeIntentLaunch"})
    private void initListeners() {
        binding.launchButton.setOnClickListener(v -> launchGame());
        binding.launchButton.setOnTouchListener(this::animateLaunchButton);
        binding.languageButton.setOnClickListener(v -> {
            if (languageManager != null) languageManager.showLanguageMenu(v);
        });
        binding.selectVersionButton.setOnClickListener(v -> showVersionSelectDialog());
        binding.importApkButton.setOnClickListener(v -> startFilePicker("application/vnd.android.package-archive", apkImportResultLauncher));
        binding.addModButton.setOnClickListener(v -> startFilePicker("*/*", soImportResultLauncher));
        binding.settingsButton.setOnClickListener(v -> showSettingsSafely());
        binding.githubIcon.setOnClickListener(v -> openGithub());
        binding.deleteVersionButton.setOnClickListener(v -> showDeleteVersionDialog());
        FeatureSettings.init(getApplicationContext());
    }

    private void launchGame() {
        binding.launchButton.setEnabled(false);
        binding.progressLoader.setVisibility(View.VISIBLE);
        new Thread(() -> {
            GameVersion version = versionManager != null ? versionManager.getSelectedVersion() : null;
            minecraftLauncher.launch(getIntent(), version);
            runOnUiThread(() -> {
                binding.progressLoader.setVisibility(View.GONE);
                binding.launchButton.setEnabled(true);
            });
        }).start();
    }

    private boolean animateLaunchButton(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            v.animate().scaleX(1f).scaleY(1f).setInterpolator(new OvershootInterpolator()).setDuration(200).start();
        }
        return false;
    }

    private void showVersionSelectDialog() {
        if (versionManager == null) return;
        versionManager.loadAllVersions();
        List<BigGroup> bigGroups = VersionUtil.buildBigGroups(
                versionManager.getInstalledVersions(),
                versionManager.getCustomVersions()
        );
        GameVersionSelectDialog dialog = new GameVersionSelectDialog(this, bigGroups);
        dialog.setOnVersionSelectListener(version -> {
            versionManager.selectVersion(version);
            viewModel.setCurrentVersion(version);
            setTextMinecraftVersion();
        });
        dialog.show();
    }

    private void startFilePicker(String type, ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(type);
        launcher.launch(intent);
    }

    private void showSettingsSafely() {
        try {
            showSettingsDialog();
        } catch (PackageManager.NameNotFoundException e) {
            //Toast.makeText(this, R.string.error_load_setting, Toast.LENGTH_SHORT).show();
        }
    }

    private void openGithub() {
        Uri uri = Uri.parse("https://github.com/LiteLDev/LeviLaunchroid");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.error_no_browser, Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteVersionDialog() {
        new CustomAlertDialog(this)
                .setTitleText(getString(R.string.dialog_title_delete_version))
                .setMessage(getString(R.string.dialog_message_delete_version))
                .setPositiveButton(getString(R.string.dialog_positive_delete), v2 -> {
                    VersionManager.get(this).deleteCustomVersion(versionManager.getSelectedVersion(),
                            new VersionManager.OnDeleteVersionCallback() {
                                @Override
                                public void onDeleteCompleted(boolean success) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(MainActivity.this, getString(R.string.toast_delete_success), Toast.LENGTH_SHORT).show();
                                        viewModel.setCurrentVersion(versionManager.getSelectedVersion());
                                        setTextMinecraftVersion();
                                    });
                                }

                                @Override
                                public void onDeleteFailed(Exception e) {
                                    runOnUiThread(() -> Toast.makeText(MainActivity.this, getString(R.string.toast_delete_failed, e.getMessage()), Toast.LENGTH_SHORT).show());
                                }
                            });
                })
                .setNegativeButton(getString(R.string.dialog_negative_cancel), null)
                .show();
    }

    private void showSettingsDialog() throws PackageManager.NameNotFoundException {
        FeatureSettings fs = FeatureSettings.getInstance();
        ThemeManager themeManager = new ThemeManager(this);
        SettingsDialog dlg = new SettingsDialog(this);

        dlg.addThemeSelectorItem(themeManager);
        dlg.addSwitchItem(
                getString(R.string.enable_debug_log),
                fs.isDebugLogDialogEnabled(),
                (btn, check) -> fs.setDebugLogDialogEnabled(check)
        );
        dlg.addSwitchItem(
                getString(R.string.version_isolation),
                fs.isVersionIsolationEnabled(),
                (btn, check) -> fs.setVersionIsolationEnabled(check)
        );
        String localVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        dlg.addActionButton(
                getString(R.string.version_prefix) + localVersion,
                getString(R.string.check_update),
                v -> new GithubReleaseUpdater(
                        this,
                        "LiteLDev",
                        "LeviLaunchroid",
                        permissionResultLauncher
                ).checkUpdate()
        );
        dlg.show();
    }

    public void setTextMinecraftVersion() {
        if (binding == null) return;
        String display = versionManager.getSelectedVersion() != null ?
                versionManager.getSelectedVersion().displayName : getString(R.string.not_found_version);
        binding.textMinecraftVersion.setText(TextUtils.isEmpty(display) ? getString(R.string.not_found_version) : display);
        updateAbiLabel();
    }

    private void handleIncomingFiles() {
        if (fileHandler == null) return;
        fileHandler.processIncomingFilesWithConfirmation(getIntent(), new FileHandler.FileOperationCallback() {
            @Override
            public void onSuccess(int processedFiles) {
                if (processedFiles > 0)
                    UIHelper.showToast(MainActivity.this, getString(R.string.files_processed, processedFiles));
            }

            @Override
            public void onError(String errorMessage) {
            }

            @Override
            public void onProgressUpdate(int progress) {
                if (binding != null) binding.progressLoader.setProgress(progress);
            }
        }, false);
    }

    private void updateModsUI(List<Mod> mods) {
        modsAdapter.updateMods(mods != null ? mods : new ArrayList<>());
        if (binding == null) return;
        int modCount = (mods != null) ? mods.size() : 0;
        binding.modsTitleText.setText(getString(R.string.mods_title, modCount));
        if (modCount == 0) {
            TextView tv = new TextView(this);
            tv.setText(R.string.no_mods_found);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
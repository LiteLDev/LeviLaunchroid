package org.levimc.launcher.ui.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.levimc.launcher.R;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.core.versions.VersionManager;
import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;
import org.levimc.launcher.ui.dialogs.InstallProgressDialog;
import org.levimc.launcher.util.InstanceBackupManager;

public class InstanceSettingsActivity extends BaseActivity {
    private static final int REQUEST_BACKUP_STORAGE = 4201;

    private GameVersion version;
    private VersionManager versionManager;
    private InstanceBackupManager backupManager;
    private InstallProgressDialog backupProgressDialog;
    private Button backupButton;

    private TextView tabGeneral, tabLaunchOptions, tabManagement;
    private View sectionGeneral, sectionLaunchOptions, sectionManagement;

    private EditText editName;
    private SwitchMaterial switchIsolation;
    private SwitchMaterial switchLaunchVertically;
    private String originalDisplayName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance_settings);

        DynamicAnim.applyPressScaleRecursively(findViewById(android.R.id.content));

        setupNavBar();

        versionManager = VersionManager.get(this);
        backupManager = new InstanceBackupManager(this);

        version = getIntent().getParcelableExtra("version");
        if (version == null) {
            finish();
            return;
        }

        initViews();
        populateData();
        selectTab(tabGeneral);
    }

    private void initViews() {
        tabGeneral = findViewById(R.id.tab_general);
        tabLaunchOptions = findViewById(R.id.tab_launch_options);
        tabManagement = findViewById(R.id.tab_management);

        sectionGeneral = findViewById(R.id.section_general);
        sectionLaunchOptions = findViewById(R.id.section_launch_options);
        sectionManagement = findViewById(R.id.section_management);

        editName = findViewById(R.id.edit_instance_name);
        switchIsolation = findViewById(R.id.switch_version_isolation);
        switchLaunchVertically = findViewById(R.id.switch_launch_vertically);

        tabGeneral.setOnClickListener(v -> selectTab(tabGeneral));
        tabLaunchOptions.setOnClickListener(v -> selectTab(tabLaunchOptions));
        tabManagement.setOnClickListener(v -> selectTab(tabManagement));

        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.btn_ok).setOnClickListener(v -> saveAndFinish());

        Button btnDelete = findViewById(R.id.btn_delete_instance);
        backupButton = findViewById(R.id.btn_backup_instance);
        if (backupButton != null) {
            backupButton.setOnClickListener(v -> confirmBackup());
        }
        if (version.isInstalled) {
            btnDelete.setEnabled(false);
            btnDelete.setAlpha(0.4f);
        } else {
            btnDelete.setOnClickListener(v -> confirmDelete());
        }
    }

    private void populateData() {
        TextView instanceInfo = findViewById(R.id.instance_info);
        String type = version.isInstalled ? getString(R.string.tag_installed) : getString(R.string.tag_custom);
        String info = "Game Version: " + (version.versionCode != null ? version.versionCode : "—")
                + " · Name: " + (version.directoryName != null ? version.directoryName : "—")
                + " · " + type;
        instanceInfo.setText(info);

        String currentName = version.versionCode != null ? version.versionCode : "";
        if (version.displayName != null && !version.displayName.isEmpty()) {
            String dn = version.displayName;
            int parenIdx = dn.lastIndexOf(" (");
            if (parenIdx > 0) {
                currentName = dn.substring(0, parenIdx);
            } else {
                currentName = dn;
            }
        }
        originalDisplayName = currentName.trim();
        editName.setText(currentName);

        switchIsolation.setChecked(version.versionIsolation);
        switchLaunchVertically.setChecked(version.launchVertically);
    }

    private void selectTab(TextView selectedTab) {
        TextView[] tabs = {tabGeneral, tabLaunchOptions, tabManagement};
        View[] sections = {sectionGeneral, sectionLaunchOptions, sectionManagement};

        org.levimc.launcher.util.PersonalizationManager pm = new org.levimc.launcher.util.PersonalizationManager(this);
        int accent = pm.getAccentColor();

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
                tabs[i].setTextColor(android.graphics.Color.WHITE);
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

    private void saveAndFinish() {
        String newName = editName.getText().toString().trim();

        versionManager.setInstanceVersionIsolation(version, switchIsolation.isChecked());
        versionManager.setInstanceLaunchVertically(version, switchLaunchVertically.isChecked());

        if (!newName.isEmpty() && !newName.equals(originalDisplayName)) {
            View btnOk = findViewById(R.id.btn_ok);
            if (btnOk != null) {
                btnOk.setEnabled(false);
                btnOk.setAlpha(0.5f);
            }
            versionManager.renameVersion(version, newName, new VersionManager.OnRenameVersionCallback() {
                @Override
                public void onRenameCompleted(boolean success) {
                    runOnUiThread(() -> {
                        setResult(RESULT_OK);
                        finish();
                    });
                }

                @Override
                public void onRenameFailed(Exception e) {
                    runOnUiThread(() -> {
                        if (btnOk != null) {
                            btnOk.setEnabled(true);
                            btnOk.setAlpha(1.0f);
                        }
                        Toast.makeText(InstanceSettingsActivity.this,
                                getString(R.string.rename_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void confirmDelete() {
        new CustomAlertDialog(this)
                .setTitleText(getString(R.string.instance_delete_confirm_title))
                .setMessage(getString(R.string.instance_delete_confirm_msg))
                .setPositiveButton(getString(R.string.delete), v -> {
                    versionManager.deleteCustomVersion(version, new VersionManager.OnDeleteVersionCallback() {
                        @Override
                        public void onDeleteCompleted(boolean success) {
                            runOnUiThread(() -> {
                                setResult(RESULT_OK);
                                finish();
                            });
                        }

                        @Override
                        public void onDeleteFailed(Exception e) {
                            runOnUiThread(() -> Toast.makeText(InstanceSettingsActivity.this,
                                    getString(R.string.toast_delete_failed, e.getMessage()), Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void confirmBackup() {
        new CustomAlertDialog(this)
                .setTitleText(getString(R.string.instance_backup_title))
                .setMessage(getString(R.string.instance_backup_confirm_message))
                .setPositiveButton(getString(R.string.backup), v -> startBackupWithPermissionCheck())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void startBackupWithPermissionCheck() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_BACKUP_STORAGE);
            return;
        }
        startBackup();
    }

    private void startBackup() {
        if (backupButton != null) {
            backupButton.setEnabled(false);
            backupButton.setAlpha(0.55f);
        }
        backupProgressDialog = new InstallProgressDialog(this);
        backupProgressDialog.setTitleText(getString(R.string.instance_backup_title));
        backupProgressDialog.setStatusText(getString(R.string.instance_backup_in_progress));
        backupProgressDialog.setProgress(0);
        backupProgressDialog.show();

        backupManager.backup(version, new InstanceBackupManager.BackupCallback() {
            @Override
            public void onStarted() {
                if (backupProgressDialog != null) {
                    backupProgressDialog.setProgress(0);
                    backupProgressDialog.setStatusText(getString(R.string.instance_backup_in_progress));
                }
            }

            @Override
            public void onProgress(int progress) {
                if (backupProgressDialog != null) {
                    backupProgressDialog.setProgress(progress);
                }
            }

            @Override
            public void onSuccess(String displayPath) {
                finishBackupProgress();
                new CustomAlertDialog(InstanceSettingsActivity.this)
                        .setTitleText(getString(R.string.instance_backup_success_title))
                        .setMessage(getString(R.string.instance_backup_success_message, displayPath))
                        .setPositiveButton(getString(R.string.confirm), null)
                        .show();
            }

            @Override
            public void onError(String message) {
                finishBackupProgress();
                new CustomAlertDialog(InstanceSettingsActivity.this)
                        .setTitleText(getString(R.string.instance_backup_failed_title))
                        .setMessage(getString(R.string.instance_backup_failed_message, message))
                        .setPositiveButton(getString(R.string.confirm), null)
                        .show();
            }
        });
    }

    private void finishBackupProgress() {
        if (backupProgressDialog != null && backupProgressDialog.isShowing()) {
            backupProgressDialog.dismiss();
        }
        backupProgressDialog = null;
        if (backupButton != null) {
            backupButton.setEnabled(true);
            backupButton.setAlpha(1f);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BACKUP_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBackup();
            } else {
                Toast.makeText(this, R.string.storage_permission_not_granted, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupNavBar() {
        setActiveNavTab(R.id.nav_tab_instances);
    }
}

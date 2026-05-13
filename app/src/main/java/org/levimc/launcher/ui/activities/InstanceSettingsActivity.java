package org.levimc.launcher.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.levimc.launcher.R;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.core.versions.VersionManager;
import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;

public class InstanceSettingsActivity extends BaseActivity {

    private GameVersion version;
    private VersionManager versionManager;

    private TextView tabGeneral, tabLaunchOptions, tabManagement;
    private View sectionGeneral, sectionLaunchOptions, sectionManagement;

    private EditText editName;
    private SwitchMaterial switchIsolation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance_settings);

        DynamicAnim.applyPressScaleRecursively(findViewById(android.R.id.content));

        setupNavBar();

        versionManager = VersionManager.get(this);

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

        tabGeneral.setOnClickListener(v -> selectTab(tabGeneral));
        tabLaunchOptions.setOnClickListener(v -> selectTab(tabLaunchOptions));
        tabManagement.setOnClickListener(v -> selectTab(tabManagement));

        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.btn_ok).setOnClickListener(v -> saveAndFinish());

        Button btnDelete = findViewById(R.id.btn_delete_instance);
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
        editName.setText(currentName);

        switchIsolation.setChecked(version.versionIsolation);
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

        if (!newName.isEmpty() && !version.isInstalled) {
            versionManager.renameCustomVersion(version, newName, new VersionManager.OnRenameVersionCallback() {
                @Override
                public void onRenameCompleted(boolean success) {}

                @Override
                public void onRenameFailed(Exception e) {
                    runOnUiThread(() -> Toast.makeText(InstanceSettingsActivity.this,
                            "Rename failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        }

        versionManager.setInstanceVersionIsolation(version, switchIsolation.isChecked());

        setResult(RESULT_OK);
        finish();
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
                                    "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void setupNavBar() {
        setActiveNavTab(R.id.nav_tab_instances);
    }
}

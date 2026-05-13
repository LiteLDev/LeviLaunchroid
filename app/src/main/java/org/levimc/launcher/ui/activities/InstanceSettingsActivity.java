package org.levimc.launcher.ui.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.levimc.launcher.R;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.core.versions.VersionManager;
import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class InstanceSettingsActivity extends BaseActivity {

    private GameVersion version;
    private VersionManager versionManager;

    private TextView tabGeneral, tabLaunchOptions, tabManagement;
    private View sectionGeneral, sectionLaunchOptions, sectionManagement;

    private EditText editName;
    private ImageView iconPreview;
    private SwitchMaterial switchIsolation;
    private boolean pendingIsolation;
    private String pendingName;
    private Bitmap pendingIcon;
    private boolean clearIcon;

    private ActivityResultLauncher<Intent> iconPickerLauncher;

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

        iconPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                InputStream is = getContentResolver().openInputStream(uri);
                                pendingIcon = BitmapFactory.decodeStream(is);
                                clearIcon = false;
                                if (is != null) is.close();
                                if (pendingIcon != null) {
                                    iconPreview.setImageBitmap(pendingIcon);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
        );

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
        iconPreview = findViewById(R.id.instance_icon_preview);
        switchIsolation = findViewById(R.id.switch_version_isolation);

        tabGeneral.setOnClickListener(v -> selectTab(tabGeneral));
        tabLaunchOptions.setOnClickListener(v -> selectTab(tabLaunchOptions));
        tabManagement.setOnClickListener(v -> selectTab(tabManagement));

        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.btn_ok).setOnClickListener(v -> saveAndFinish());

        Button btnClearIcon = findViewById(R.id.btn_clear_icon);
        btnClearIcon.setOnClickListener(v -> {
            clearIcon = true;
            pendingIcon = null;
            iconPreview.setImageResource(R.drawable.ic_minecraft);
        });

        iconPreview.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            iconPickerLauncher.launch(intent);
        });

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
        pendingName = currentName;

        pendingIsolation = version.versionIsolation;
        switchIsolation.setChecked(pendingIsolation);

        File logoFile = getInstanceLogoFile();
        if (logoFile != null && logoFile.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(logoFile.getAbsolutePath());
            if (bmp != null) {
                iconPreview.setImageBitmap(bmp);
            }
        }
    }

    private void selectTab(TextView selectedTab) {
        TextView[] tabs = {tabGeneral, tabLaunchOptions, tabManagement};
        View[] sections = {sectionGeneral, sectionLaunchOptions, sectionManagement};

        for (int i = 0; i < tabs.length; i++) {
            boolean isSelected = tabs[i] == selectedTab;
            tabs[i].setBackgroundResource(isSelected ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
            tabs[i].setTextColor(getColor(isSelected ? R.color.on_primary : R.color.text_secondary));

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

        if (pendingIcon != null && !clearIcon) {
            saveIconToFile(pendingIcon);
        } else if (clearIcon) {
            File logoFile = getInstanceLogoFile();
            if (logoFile != null && logoFile.exists()) {
                logoFile.delete();
            }
        }

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

    private File getInstanceLogoFile() {
        if (version.versionDir == null) return null;
        return new File(version.versionDir, "LargeLogo.png");
    }

    private void saveIconToFile(Bitmap bitmap) {
        File logoFile = getInstanceLogoFile();
        if (logoFile == null) return;
        try (FileOutputStream fos = new FileOutputStream(logoFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception ignored) {}
    }

    private void setupNavBar() {
        setActiveNavTab(R.id.nav_tab_instances);
    }
}

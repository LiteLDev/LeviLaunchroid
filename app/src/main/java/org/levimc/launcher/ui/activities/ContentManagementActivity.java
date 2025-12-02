package org.levimc.launcher.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.levimc.launcher.R;
import org.levimc.launcher.core.content.ContentManager;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.core.versions.VersionManager;
import org.levimc.launcher.databinding.ActivityContentManagementBinding;
import org.levimc.launcher.settings.FeatureSettings;
import org.levimc.launcher.ui.animation.DynamicAnim;

import java.io.File;

public class ContentManagementActivity extends BaseActivity {
    
    private static final String PREFS_NAME = "content_management";
    private static final String KEY_STORAGE_TYPE = "storage_type";
    
    private ActivityContentManagementBinding binding;
    private ContentManager contentManager;
    private VersionManager versionManager;
    private FeatureSettings.StorageType currentStorageType = FeatureSettings.StorageType.VERSION_ISOLATION;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContentManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        DynamicAnim.applyPressScaleRecursively(binding.getRoot());

        initializeManagers();
        setupUI();
        loadCurrentVersion();
    }

    private void initializeManagers() {
        contentManager = ContentManager.getInstance(this);
        versionManager = VersionManager.get(this);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        loadStorageType();
    }

    private void loadStorageType() {
        String savedType = prefs.getString(KEY_STORAGE_TYPE, "VERSION_ISOLATION");
        currentStorageType = FeatureSettings.StorageType.valueOf(savedType);
    }

    private void saveStorageType() {
        prefs.edit().putString(KEY_STORAGE_TYPE, currentStorageType.name()).apply();
    }

    private void setupUI() {
        binding.backButton.setOnClickListener(v -> finish());
        
        setupStorageSpinner();
        setupCategoryButtons();
    }

    private void setupStorageSpinner() {
        String[] storageOptions = {
            getString(R.string.storage_internal),
            getString(R.string.storage_external),
            getString(R.string.storage_version_isolation)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, storageOptions);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        binding.storageTypeSpinner.setAdapter(adapter);
        binding.storageTypeSpinner.setPopupBackgroundResource(R.drawable.bg_popup_menu_rounded);
        DynamicAnim.applyPressScale(binding.storageTypeSpinner);

        int currentSelection = switch (currentStorageType) {
            case INTERNAL -> 0;
            case EXTERNAL -> 1;
            case VERSION_ISOLATION -> 2;
        };
        binding.storageTypeSpinner.setSelection(currentSelection);

        binding.storageTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                FeatureSettings.StorageType newType = switch (position) {
                    case 0 -> FeatureSettings.StorageType.INTERNAL;
                    case 1 -> FeatureSettings.StorageType.EXTERNAL;
                    case 2 -> FeatureSettings.StorageType.VERSION_ISOLATION;
                    default -> FeatureSettings.StorageType.VERSION_ISOLATION;
                };

                if (newType != currentStorageType) {
                    currentStorageType = newType;
                    saveStorageType();
                    updateStorageDirectories();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupCategoryButtons() {
        binding.worldsButton.setOnClickListener(v -> openContentList(ContentListActivity.TYPE_WORLDS));
        binding.skinPacksButton.setOnClickListener(v -> openContentList(ContentListActivity.TYPE_SKIN_PACKS));
        binding.resourcePacksButton.setOnClickListener(v -> openContentList(ContentListActivity.TYPE_RESOURCE_PACKS));
        binding.behaviorPacksButton.setOnClickListener(v -> openContentList(ContentListActivity.TYPE_BEHAVIOR_PACKS));
    }

    private void openContentList(int contentType) {
        Intent intent = new Intent(this, ContentListActivity.class);
        intent.putExtra(ContentListActivity.EXTRA_CONTENT_TYPE, contentType);
        
        if (contentType == ContentListActivity.TYPE_WORLDS) {
            File worldsDir = getWorldsDirectory();
            if (worldsDir != null) {
                intent.putExtra(ContentListActivity.EXTRA_WORLDS_DIRECTORY, worldsDir.getAbsolutePath());
            }
        }
        
        startActivity(intent);
    }

    private File getWorldsDirectory() {
        GameVersion currentVersion = versionManager.getSelectedVersion();
        if (currentVersion == null) return null;

        switch (currentStorageType) {
            case VERSION_ISOLATION:
                if (currentVersion.versionDir != null) {
                    File gameDataDir = new File(currentVersion.versionDir, "games/com.mojang");
                    return new File(gameDataDir, "minecraftWorlds");
                }
                break;
            case EXTERNAL:
                File externalDir = getExternalFilesDir(null);
                if (externalDir != null) {
                    File gameDataDir = new File(externalDir, "games/com.mojang");
                    return new File(gameDataDir, "minecraftWorlds");
                }
                break;
            case INTERNAL:
                File internalDir = new File(getDataDir(), "games/com.mojang");
                return new File(internalDir, "minecraftWorlds");
        }
        return null;
    }

    private void loadCurrentVersion() {
        GameVersion currentVersion = versionManager.getSelectedVersion();
        if (currentVersion != null) {
            binding.versionText.setText(currentVersion.displayName);
            updateStorageDirectories();
        } else {
            binding.versionText.setText(getString(R.string.not_found_version));
            Toast.makeText(this, "No version selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStorageDirectories() {
        GameVersion currentVersion = versionManager.getSelectedVersion();
        if (currentVersion == null) return;

        File worldsDir;
        File resourcePacksDir;
        File behaviorPacksDir;
        File skinPacksDir;

        switch (currentStorageType) {
            case VERSION_ISOLATION:
                if (currentVersion.versionDir != null) {
                    File gameDataDir = new File(currentVersion.versionDir, "games/com.mojang");
                    worldsDir = new File(gameDataDir, "minecraftWorlds");
                    resourcePacksDir = new File(gameDataDir, "resource_packs");
                    behaviorPacksDir = new File(gameDataDir, "behavior_packs");
                    skinPacksDir = new File(gameDataDir, "skin_packs");
                } else {
                    worldsDir = null;
                    resourcePacksDir = null;
                    behaviorPacksDir = null;
                    skinPacksDir = null;
                }
                break;

            case EXTERNAL:
                File externalDir = getExternalFilesDir(null);
                if (externalDir != null) {
                    File gameDataDir = new File(externalDir, "games/com.mojang");
                    worldsDir = new File(gameDataDir, "minecraftWorlds");
                    resourcePacksDir = new File(gameDataDir, "resource_packs");
                    behaviorPacksDir = new File(gameDataDir, "behavior_packs");
                    skinPacksDir = new File(gameDataDir, "skin_packs");
                } else {
                    worldsDir = null;
                    resourcePacksDir = null;
                    behaviorPacksDir = null;
                    skinPacksDir = null;
                }
                break;

            case INTERNAL:
                File internalDir = new File(getDataDir(), "games/com.mojang");
                worldsDir = new File(internalDir, "minecraftWorlds");
                resourcePacksDir = new File(internalDir, "resource_packs");
                behaviorPacksDir = new File(internalDir, "behavior_packs");
                skinPacksDir = new File(internalDir, "skin_packs");
                break;

            default:
                worldsDir = null;
                resourcePacksDir = null;
                behaviorPacksDir = null;
                skinPacksDir = null;
                break;
        }

        contentManager.setStorageDirectories(worldsDir, resourcePacksDir, behaviorPacksDir, skinPacksDir);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStorageDirectories();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

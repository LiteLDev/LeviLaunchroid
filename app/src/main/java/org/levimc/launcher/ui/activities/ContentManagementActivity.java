package org.levimc.launcher.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;

import org.levimc.launcher.R;
import org.levimc.launcher.core.content.ContentManager;
import org.levimc.launcher.core.content.ResourcePackItem;
import org.levimc.launcher.core.content.ResourcePackManager;
import org.levimc.launcher.core.content.WorldItem;
import org.levimc.launcher.core.content.WorldManager;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.core.versions.VersionManager;
import org.levimc.launcher.databinding.ActivityContentManagementBinding;
import org.levimc.launcher.settings.FeatureSettings;
import org.levimc.launcher.ui.adapter.ResourcePacksAdapter;
import org.levimc.launcher.ui.adapter.WorldsAdapter;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;
import org.levimc.launcher.ui.animation.DynamicAnim;

import java.io.File;

public class ContentManagementActivity extends BaseActivity {
    
    private static final String PREFS_NAME = "content_management";
    private static final String KEY_STORAGE_TYPE = "storage_type";
    
    private ActivityContentManagementBinding binding;
    private ContentManager contentManager;
    private VersionManager versionManager;
    
    private WorldsAdapter worldsAdapter;
    private ResourcePacksAdapter resourcePacksAdapter;
    private ResourcePacksAdapter behaviorPacksAdapter;
    
    private ActivityResultLauncher<Intent> worldImportLauncher;
    private ActivityResultLauncher<Intent> worldExportLauncher;
    private ActivityResultLauncher<Intent> packImportLauncher;
    
    private WorldItem pendingExportWorld;
    private FeatureSettings.StorageType currentStorageType = FeatureSettings.StorageType.VERSION_ISOLATION;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContentManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 全页面统一按压缩放反馈
        DynamicAnim.applyPressScaleRecursively(binding.getRoot());

        initializeManagers();
        setupActivityResultLaunchers();
        setupUI();
        setupObservers();
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

    private void setupActivityResultLaunchers() {
        worldImportLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        importWorld(uri);
                    }
                }
            }
        );

        worldExportLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && pendingExportWorld != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        exportWorld(pendingExportWorld, uri);
                    }
                }
                pendingExportWorld = null;
            }
        );

        packImportLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        importResourcePack(uri);
                    }
                }
            }
        );
    }

    private void setupUI() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.worlds_title)));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.resource_packs_title)));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.behavior_packs_title)));

        setupWorldsRecyclerView();
        setupResourcePacksRecyclerView();
        setupBehaviorPacksRecyclerView();
        setupStorageSpinner();

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTabContent(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        binding.importWorldButton.setOnClickListener(v -> startWorldImport());
        binding.importPackButton.setOnClickListener(v -> startPackImport());
        DynamicAnim.applyPressScale(binding.importWorldButton);
        DynamicAnim.applyPressScale(binding.importPackButton);

        binding.backButton.setOnClickListener(v -> finish());
        DynamicAnim.applyPressScale(binding.backButton);

        showTabContent(0);
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

    private void setupWorldsRecyclerView() {
        worldsAdapter = new WorldsAdapter();
        worldsAdapter.setOnWorldActionListener(new WorldsAdapter.OnWorldActionListener() {
            @Override
            public void onWorldExport(WorldItem world) {
                startWorldExport(world);
            }

            @Override
            public void onWorldDelete(WorldItem world) {
                showDeleteWorldDialog(world);
            }

            @Override
            public void onWorldBackup(WorldItem world) {
                backupWorld(world);
            }
        });

        binding.worldsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.worldsRecyclerView.setAdapter(worldsAdapter);
        binding.worldsRecyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(binding.worldsRecyclerView));
    }

    private void setupResourcePacksRecyclerView() {
        resourcePacksAdapter = new ResourcePacksAdapter();
        resourcePacksAdapter.setOnResourcePackActionListener(new ResourcePacksAdapter.OnResourcePackActionListener() {
            @Override
            public void onResourcePackDelete(ResourcePackItem pack) {
                showDeleteResourcePackDialog(pack);
            }
        });

        binding.resourcePacksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.resourcePacksRecyclerView.setAdapter(resourcePacksAdapter);
        binding.resourcePacksRecyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(binding.resourcePacksRecyclerView));
    }

    private void setupBehaviorPacksRecyclerView() {
        behaviorPacksAdapter = new ResourcePacksAdapter();
        behaviorPacksAdapter.setOnResourcePackActionListener(new ResourcePacksAdapter.OnResourcePackActionListener() {
            @Override
            public void onResourcePackDelete(ResourcePackItem pack) {
                showDeleteResourcePackDialog(pack);
            }
        });

        binding.behaviorPacksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.behaviorPacksRecyclerView.setAdapter(behaviorPacksAdapter);
        binding.behaviorPacksRecyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(binding.behaviorPacksRecyclerView));
    }

    private void setupObservers() {
        contentManager.getWorldsLiveData().observe(this, worlds -> {
            worldsAdapter.updateWorlds(worlds);
            updateWorldsCount(worlds != null ? worlds.size() : 0);
        });

        contentManager.getResourcePacksLiveData().observe(this, resourcePacks -> {
            resourcePacksAdapter.updateResourcePacks(resourcePacks);
            updateResourcePacksCount(resourcePacks != null ? resourcePacks.size() : 0);
        });

        contentManager.getBehaviorPacksLiveData().observe(this, behaviorPacks -> {
            behaviorPacksAdapter.updateResourcePacks(behaviorPacks);
            updateBehaviorPacksCount(behaviorPacks != null ? behaviorPacks.size() : 0);
        });

        contentManager.getStatusLiveData().observe(this, status -> {
            if (status != null && !status.isEmpty()) {
                binding.statusText.setText(status);
            }
        });
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

        switch (currentStorageType) {
            case VERSION_ISOLATION:
                if (currentVersion.versionDir != null) {
                    File gameDataDir = new File(currentVersion.versionDir, "games/com.mojang");
                    worldsDir = new File(gameDataDir, "minecraftWorlds");
                    resourcePacksDir = new File(gameDataDir, "resource_packs");
                    behaviorPacksDir = new File(gameDataDir, "behavior_packs");
                } else {
                    worldsDir = null;
                    resourcePacksDir = null;
                    behaviorPacksDir = null;
                }
                break;

            case EXTERNAL:
                File externalDir = getExternalFilesDir(null);
                if (externalDir != null) {
                    File gameDataDir = new File(externalDir, "games/com.mojang");
                    worldsDir = new File(gameDataDir, "minecraftWorlds");
                    resourcePacksDir = new File(gameDataDir, "resource_packs");
                    behaviorPacksDir = new File(gameDataDir, "behavior_packs");
                } else {
                    worldsDir = null;
                    resourcePacksDir = null;
                    behaviorPacksDir = null;
                }
                break;

            case INTERNAL:
                File internalDir = new File(getDataDir(), "games/com.mojang");
                worldsDir = new File(internalDir, "minecraftWorlds");
                resourcePacksDir = new File(internalDir, "resource_packs");
                behaviorPacksDir = new File(internalDir, "behavior_packs");
                break;

            default:
                worldsDir = null;
                resourcePacksDir = null;
                behaviorPacksDir = null;
                break;
        }

        contentManager.setStorageDirectories(worldsDir, resourcePacksDir, behaviorPacksDir);
    }

    private void showTabContent(int position) {
        binding.worldsRecyclerView.setVisibility(android.view.View.GONE);
        binding.resourcePacksRecyclerView.setVisibility(android.view.View.GONE);
        binding.behaviorPacksRecyclerView.setVisibility(android.view.View.GONE);

        switch (position) {
            case 0: // Worlds
                binding.worldsRecyclerView.setVisibility(android.view.View.VISIBLE);
                binding.importWorldButton.setVisibility(android.view.View.VISIBLE);
                binding.importPackButton.setVisibility(android.view.View.GONE);
                DynamicAnim.staggerRecyclerChildren(binding.worldsRecyclerView);
                break;
            case 1: // Resource Packs
                binding.resourcePacksRecyclerView.setVisibility(android.view.View.VISIBLE);
                binding.importWorldButton.setVisibility(android.view.View.GONE);
                binding.importPackButton.setVisibility(android.view.View.VISIBLE);
                binding.importPackButton.setText(getString(R.string.import_resource_pack));
                DynamicAnim.staggerRecyclerChildren(binding.resourcePacksRecyclerView);
                break;
            case 2: // Behavior Packs
                binding.behaviorPacksRecyclerView.setVisibility(android.view.View.VISIBLE);
                binding.importWorldButton.setVisibility(android.view.View.GONE);
                binding.importPackButton.setVisibility(android.view.View.VISIBLE);
                binding.importPackButton.setText(getString(R.string.import_behavior_pack));
                DynamicAnim.staggerRecyclerChildren(binding.behaviorPacksRecyclerView);
                break;
        }
    }

    private void updateWorldsCount(int count) {
        TabLayout.Tab tab = binding.tabLayout.getTabAt(0);
        if (tab != null) {
            tab.setText(getString(R.string.worlds_title) + " (" + count + ")");
        }
    }

    private void updateResourcePacksCount(int count) {
        TabLayout.Tab tab = binding.tabLayout.getTabAt(1);
        if (tab != null) {
            tab.setText(getString(R.string.resource_packs_title) + " (" + count + ")");
        }
    }

    private void updateBehaviorPacksCount(int count) {
        TabLayout.Tab tab = binding.tabLayout.getTabAt(2);
        if (tab != null) {
            tab.setText(getString(R.string.behavior_packs_title) + " (" + count + ")");
        }
    }

    private void startWorldImport() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/octet-stream"});
        worldImportLauncher.launch(Intent.createChooser(intent, getString(R.string.import_world)));
    }

    private void startWorldExport(WorldItem world) {
        pendingExportWorld = world;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, world.getName() + ".mcworld");
        worldExportLauncher.launch(intent);
    }

    private void startPackImport() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/octet-stream"});
        packImportLauncher.launch(Intent.createChooser(intent, getString(R.string.import_resource_pack)));
    }

    private void importWorld(Uri uri) {
        contentManager.importWorld(uri, new WorldManager.WorldOperationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(ContentManagementActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ContentManagementActivity.this, error, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onProgress(int progress) {
            }
        });
    }

    private void exportWorld(WorldItem world, Uri uri) {
        contentManager.exportWorld(world, uri, new WorldManager.WorldOperationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(ContentManagementActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ContentManagementActivity.this, error, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onProgress(int progress) {
            }
        });
    }

    private void importResourcePack(Uri uri) {
        contentManager.importResourcePack(uri, new ResourcePackManager.PackOperationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(ContentManagementActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ContentManagementActivity.this, error, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onProgress(int progress) {
            }
        });
    }



    private void showDeleteWorldDialog(WorldItem world) {
        new CustomAlertDialog(this)
                .setTitleText(getString(R.string.delete_world))
                .setMessage(getString(R.string.confirm_delete_world))
                .setPositiveButton(getString(R.string.dialog_positive_delete), (dialog) -> deleteWorld(world))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showDeleteResourcePackDialog(ResourcePackItem pack) {
        new CustomAlertDialog(this)
                .setTitleText(getString(R.string.delete_resource_pack))
                .setMessage(getString(R.string.confirm_delete_resource_pack))
                .setPositiveButton(getString(R.string.dialog_positive_delete), (dialog) -> deleteResourcePack(pack))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }



    private void deleteWorld(WorldItem world) {
        contentManager.deleteWorld(world, new WorldManager.WorldOperationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(ContentManagementActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ContentManagementActivity.this, error, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onProgress(int progress) {}
        });
    }

    private void backupWorld(WorldItem world) {
        contentManager.backupWorld(world, new WorldManager.WorldOperationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(ContentManagementActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ContentManagementActivity.this, error, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onProgress(int progress) {}
        });
    }



    private void deleteResourcePack(ResourcePackItem pack) {
        contentManager.deleteResourcePack(pack, new ResourcePackManager.PackOperationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(ContentManagementActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ContentManagementActivity.this, error, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onProgress(int progress) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (contentManager != null) {
            contentManager.shutdown();
        }
    }
}
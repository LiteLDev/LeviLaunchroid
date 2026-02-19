package org.levimc.launcher.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.levimc.launcher.R;
import org.levimc.launcher.core.content.ContentManager;
import org.levimc.launcher.core.content.ResourcePackItem;
import org.levimc.launcher.core.content.ResourcePackManager;
import org.levimc.launcher.core.content.ServerItem;
import org.levimc.launcher.core.content.StructureExtractor;
import org.levimc.launcher.core.content.WorldItem;
import org.levimc.launcher.core.content.WorldManager;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.core.versions.VersionManager;
import org.levimc.launcher.databinding.ActivityContentListBinding;
import org.levimc.launcher.settings.FeatureSettings;
import org.levimc.launcher.ui.adapter.ResourcePacksAdapter;
import org.levimc.launcher.ui.adapter.StructuresAdapter;
import org.levimc.launcher.ui.adapter.WorldsAdapter;
import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;

import android.provider.MediaStore;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.OutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ContentListActivity extends BaseActivity {

    public static final String EXTRA_CONTENT_TYPE = "content_type";
    public static final String EXTRA_WORLDS_DIRECTORY = "worlds_directory";
    public static final String EXTRA_CURRENT_STORAGE_TYPE = "current_storage_type";
    public static final int TYPE_WORLDS = 0;
    public static final int TYPE_SKIN_PACKS = 1;
    public static final int TYPE_RESOURCE_PACKS = 2;
    public static final int TYPE_BEHAVIOR_PACKS = 3;
    public static final int TYPE_SCREENSHOTS = 4;
    public static final int TYPE_SERVERS = 5;

    private ActivityContentListBinding binding;
    private ContentManager contentManager;
    private VersionManager versionManager;
    private int contentType;
    private File worldsDirectory;
    private FeatureSettings.StorageType currentStorageType;

    private WorldsAdapter worldsAdapter;
    private ResourcePacksAdapter packsAdapter;
    private org.levimc.launcher.ui.adapter.ScreenshotsAdapter screenshotsAdapter;
    private org.levimc.launcher.ui.adapter.ServersAdapter serversAdapter;

    private ActivityResultLauncher<Intent> importLauncher;
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> customFlatWorldLauncher;
    private ActivityResultLauncher<Intent> structureExportLauncher;
    private WorldItem pendingExportWorld;
    private WorldItem pendingStructureExportWorld;
    private StructureExtractor.StructureInfo pendingStructureInfo;
    private StructureExtractor structureExtractor;

    private List<WorldItem> allWorlds = new ArrayList<>();
    private List<ResourcePackItem> allPacks = new ArrayList<>();
    private List<ServerItem> allServers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContentListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        DynamicAnim.applyPressScaleRecursively(binding.getRoot());

        contentType = getIntent().getIntExtra(EXTRA_CONTENT_TYPE, TYPE_WORLDS);
        contentManager = ContentManager.getInstance(this);
        versionManager = VersionManager.get(this);
        
        String storageTypeStr = getIntent().getStringExtra(EXTRA_CURRENT_STORAGE_TYPE);
        if (storageTypeStr != null) {
            currentStorageType = FeatureSettings.StorageType.valueOf(storageTypeStr);
        } else {
            SharedPreferences prefs = getSharedPreferences("content_management", MODE_PRIVATE);
            String savedType = prefs.getString("storage_type", "INTERNAL");
            currentStorageType = FeatureSettings.StorageType.valueOf(savedType);
        }

        setupActivityResultLaunchers();
        setupUI();
        setupObservers();
        loadContent();
    }

    private void setupActivityResultLaunchers() {
        importLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        handleImport(uri);
                    }
                }
            }
        );

        exportLauncher = registerForActivityResult(
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

        customFlatWorldLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadContent();
                }
            }
        );

        structureExportLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && pendingStructureExportWorld != null && pendingStructureInfo != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        exportStructureToFile(pendingStructureExportWorld, pendingStructureInfo, uri);
                    }
                }
                pendingStructureExportWorld = null;
                pendingStructureInfo = null;
            }
        );

        structureExtractor = new StructureExtractor(this);
    }

    private void setupUI() {
        binding.backButton.setOnClickListener(v -> finish());

        String worldsPath = getIntent().getStringExtra(EXTRA_WORLDS_DIRECTORY);
        if (worldsPath != null) {
            worldsDirectory = new File(worldsPath);
        }

        switch (contentType) {
            case TYPE_WORLDS:
                binding.titleText.setText(getString(R.string.worlds_title));
                binding.importButton.setText(getString(R.string.import_world));
                binding.customFlatButton.setVisibility(View.VISIBLE);
                setupWorldsRecyclerView();
                break;
            case TYPE_SKIN_PACKS:
                binding.titleText.setText(getString(R.string.skin_packs_title));
                binding.importButton.setText(getString(R.string.import_skin_pack));
                setupPacksRecyclerView();
                break;
            case TYPE_RESOURCE_PACKS:
                binding.titleText.setText(getString(R.string.resource_packs_title));
                binding.importButton.setText(getString(R.string.import_resource_pack));
                setupPacksRecyclerView();
                break;
            case TYPE_BEHAVIOR_PACKS:
                binding.titleText.setText(getString(R.string.behavior_packs_title));
                binding.importButton.setText(getString(R.string.import_behavior_pack));
                setupPacksRecyclerView();
                break;
            case TYPE_SCREENSHOTS:
                binding.titleText.setText(getString(R.string.screenshots_category));
                binding.importButton.setVisibility(View.GONE);
                binding.searchEditText.setVisibility(View.GONE);
                setupScreenshotsRecyclerView();
                break;
            case TYPE_SERVERS:
                binding.titleText.setText(getString(R.string.servers_category));
                binding.importButton.setText(getString(R.string.quick_launch_add_server));
                binding.importButton.setVisibility(View.VISIBLE);
                binding.searchEditText.setVisibility(View.VISIBLE);
                setupServersRecyclerView();
                break;
        }

        binding.importButton.setOnClickListener(v -> {
            if (contentType == TYPE_SERVERS) {
                showAddServerDialog();
            } else {
                startImport();
            }
        });
        binding.customFlatButton.setOnClickListener(v -> openCustomFlatWorld());

        setupSearchFilter();
    }

    private void setupSearchFilter() {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterContent(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterContent(String query) {
        String lowerQuery = query.toLowerCase().trim();

        if (contentType == TYPE_WORLDS) {
            if (lowerQuery.isEmpty()) {
                worldsAdapter.updateWorlds(allWorlds);
            } else {
                List<WorldItem> filtered = allWorlds.stream()
                    .filter(world -> world.getWorldName().toLowerCase().contains(lowerQuery))
                    .collect(Collectors.toList());
                worldsAdapter.updateWorlds(filtered);
            }
        } else if (contentType == TYPE_SERVERS) {
            if (lowerQuery.isEmpty()) {
                serversAdapter.updateData(allServers);
            } else {
                List<org.levimc.launcher.core.content.ServerItem> filtered = allServers.stream()
                    .filter(server -> server.name.toLowerCase().contains(lowerQuery) || 
                                     server.ip.toLowerCase().contains(lowerQuery))
                    .collect(Collectors.toList());
                serversAdapter.updateData(filtered);
            }
        } else {
            if (lowerQuery.isEmpty()) {
                packsAdapter.updateResourcePacks(allPacks);
            } else {
                List<ResourcePackItem> filtered = allPacks.stream()
                    .filter(pack -> pack.getPackName().toLowerCase().contains(lowerQuery))
                    .collect(Collectors.toList());
                packsAdapter.updateResourcePacks(filtered);
            }
        }
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

            @Override
            public void onWorldEdit(WorldItem world) {
                openWorldEditor(world);
            }

            @Override
            public void onWorldExtractStructures(WorldItem world) {
                showExtractStructuresDialog(world);
            }

            @Override
            public void onWorldTransfer(WorldItem world) {
                showTransferWorldDialog(world);
            }
        });

        binding.contentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.contentRecyclerView.setAdapter(worldsAdapter);
        binding.contentRecyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(binding.contentRecyclerView));
    }

    private void openWorldEditor(WorldItem world) {
        File worldFile = world.getFile();
        if (worldFile == null || !worldFile.exists()) {
            Toast.makeText(this, "World directory not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, WorldEditorActivity.class);
        intent.putExtra(WorldEditorActivity.EXTRA_WORLD_PATH, worldFile.getAbsolutePath());
        intent.putExtra(WorldEditorActivity.EXTRA_WORLD_NAME, world.getWorldName());
        startActivity(intent);
    }

    private void setupPacksRecyclerView() {
        packsAdapter = new ResourcePacksAdapter();
        packsAdapter.setOnResourcePackActionListener(new ResourcePacksAdapter.OnResourcePackActionListener() {
            @Override
            public void onResourcePackDelete(ResourcePackItem pack) {
                showDeletePackDialog(pack);
            }

            @Override
            public void onResourcePackTransfer(ResourcePackItem pack) {
                showTransferPackDialog(pack);
            }
        });

        binding.contentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.contentRecyclerView.setAdapter(packsAdapter);
        binding.contentRecyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(binding.contentRecyclerView));
    }

    private void setupScreenshotsRecyclerView() {
        screenshotsAdapter = new org.levimc.launcher.ui.adapter.ScreenshotsAdapter(new ArrayList(), new org.levimc.launcher.ui.adapter.ScreenshotsAdapter.OnScreenshotClickListener() {
            @Override
            public void onDeleteClick(org.levimc.launcher.core.content.ScreenshotItem screenshot) {
                showDeleteScreenshotDialog(screenshot);
            }

            @Override
            public void onSaveClick(org.levimc.launcher.core.content.ScreenshotItem screenshot) {
                saveScreenshotToGallery(screenshot);
            }
        });
        binding.contentRecyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
        binding.contentRecyclerView.setAdapter(screenshotsAdapter);
        binding.contentRecyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(binding.contentRecyclerView));
    }

    private void setupServersRecyclerView() {
        serversAdapter = new org.levimc.launcher.ui.adapter.ServersAdapter(new ArrayList<>(), server -> showDeleteServerDialog(server));
        binding.contentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.contentRecyclerView.setAdapter(serversAdapter);
        binding.contentRecyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(binding.contentRecyclerView));
    }

    private void setupObservers() {
        switch (contentType) {
            case TYPE_WORLDS:
                contentManager.getWorldsLiveData().observe(this, worlds -> {
                    allWorlds = worlds != null ? worlds : new ArrayList<>();
                    if (worldsAdapter != null) {
                        filterContent(binding.searchEditText.getText().toString());
                    }
                    showLoading(false);
                });
                break;
            case TYPE_SKIN_PACKS:
                contentManager.getSkinPacksLiveData().observe(this, packs -> {
                    allPacks = packs != null ? packs : new ArrayList<>();
                    if (packsAdapter != null) {
                        filterContent(binding.searchEditText.getText().toString());
                    }
                    showLoading(false);
                });
                break;
            case TYPE_RESOURCE_PACKS:
                contentManager.getResourcePacksLiveData().observe(this, packs -> {
                    allPacks = packs != null ? packs : new ArrayList<>();
                    if (packsAdapter != null) {
                        filterContent(binding.searchEditText.getText().toString());
                    }
                    showLoading(false);
                });
                break;
            case TYPE_BEHAVIOR_PACKS:
                contentManager.getBehaviorPacksLiveData().observe(this, packs -> {
                    allPacks = packs != null ? packs : new ArrayList<>();
                    if (packsAdapter != null) {
                        filterContent(binding.searchEditText.getText().toString());
                    }
                    showLoading(false);
                });
                break;
            case TYPE_SCREENSHOTS:
                contentManager.getScreenshotsLiveData().observe(this, screenshots -> {
                    if (screenshotsAdapter != null) {
                        screenshotsAdapter.updateData(screenshots != null ? screenshots : new ArrayList<>());
                    }
                    showLoading(false);
                });
                break;
            case TYPE_SERVERS:
                contentManager.getServersLiveData().observe(this, servers -> {
                    allServers = servers != null ? servers : new ArrayList<>();
                    if (serversAdapter != null) {
                        filterContent(binding.searchEditText.getText().toString());
                    }
                    showLoading(false);
                });
                break;
        }
    }

    private void loadContent() {
        showLoading(true);
        switch (contentType) {
            case TYPE_WORLDS:
                contentManager.refreshWorlds();
                break;
            case TYPE_SKIN_PACKS:
                contentManager.refreshSkinPacks();
                break;
            case TYPE_RESOURCE_PACKS:
                contentManager.refreshResourcePacks();
                break;
            case TYPE_BEHAVIOR_PACKS:
                contentManager.refreshBehaviorPacks();
                break;
            case TYPE_SCREENSHOTS:
                contentManager.refreshScreenshots();
                break;
            case TYPE_SERVERS:
                contentManager.refreshServers();
                break;
        }
    }

    private void showLoading(boolean show) {
        binding.loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void startImport() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/octet-stream"});
        importLauncher.launch(Intent.createChooser(intent, getString(R.string.import_world)));
    }

    private void handleImport(Uri uri) {
        if (contentType == TYPE_WORLDS) {
            contentManager.importWorld(uri, new WorldManager.WorldOperationCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> Toast.makeText(ContentListActivity.this, message, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(ContentListActivity.this, error, Toast.LENGTH_LONG).show());
                }

                @Override
                public void onProgress(int progress) {}
            });
        } else {
            contentManager.importResourcePack(uri, new ResourcePackManager.PackOperationCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> Toast.makeText(ContentListActivity.this, message, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(ContentListActivity.this, error, Toast.LENGTH_LONG).show());
                }

                @Override
                public void onProgress(int progress) {}
            });
        }
    }

    private void startWorldExport(WorldItem world) {
        pendingExportWorld = world;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, world.getName() + ".mcworld");
        exportLauncher.launch(intent);
    }

    private void exportWorld(WorldItem world, Uri uri) {
        contentManager.exportWorld(world, uri, new WorldManager.WorldOperationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(ContentListActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ContentListActivity.this, error, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onProgress(int progress) {}
        });
    }

    private void backupWorld(WorldItem world) {
        contentManager.backupWorld(world, new WorldManager.WorldOperationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(ContentListActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ContentListActivity.this, error, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onProgress(int progress) {}
        });
    }

    private void showDeleteWorldDialog(WorldItem world) {
        new CustomAlertDialog(this)
            .setTitleText(getString(R.string.delete_world))
            .setMessage(getString(R.string.confirm_delete_world))
            .setPositiveButton(getString(R.string.dialog_positive_delete), v -> deleteWorld(world))
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

    private void deleteWorld(WorldItem world) {
        contentManager.deleteWorld(world, new WorldManager.WorldOperationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(ContentListActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ContentListActivity.this, error, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onProgress(int progress) {}
        });
    }

    private void showDeletePackDialog(ResourcePackItem pack) {
        int titleResId;
        int messageResId;

        if (contentType == TYPE_BEHAVIOR_PACKS) {
            titleResId = R.string.delete_behavior_pack;
            messageResId = R.string.confirm_delete_behavior_pack;
        } else if (contentType == TYPE_SKIN_PACKS) {
            titleResId = R.string.delete_skin_pack;
            messageResId = R.string.confirm_delete_skin_pack;
        } else {
            titleResId = R.string.delete_resource_pack;
            messageResId = R.string.confirm_delete_resource_pack;
        }

        new CustomAlertDialog(this)
            .setTitleText(getString(titleResId))
            .setMessage(getString(messageResId))
            .setPositiveButton(getString(R.string.dialog_positive_delete), v -> deletePack(pack))
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

    private void deletePack(ResourcePackItem pack) {
        contentManager.deleteResourcePack(pack, new ResourcePackManager.PackOperationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(ContentListActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ContentListActivity.this, error, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onProgress(int progress) {}
        });
    }

    private void saveScreenshotToGallery(org.levimc.launcher.core.content.ScreenshotItem screenshot) {
        showLoading(true);
        new Thread(() -> {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(screenshot.file.getAbsolutePath());
                if (bitmap != null) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, screenshot.name + "_" + System.currentTimeMillis() + ".jpg");
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);

                    Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                        }
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(ContentListActivity.this, R.string.saved_to_gallery, Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(ContentListActivity.this, "Save failed", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void showDeleteScreenshotDialog(org.levimc.launcher.core.content.ScreenshotItem screenshot) {
        new CustomAlertDialog(this)
            .setTitleText(getString(R.string.delete))
            .setMessage("Are you sure you want to delete this screenshot?")
            .setPositiveButton(getString(R.string.dialog_positive_delete), v -> deleteScreenshot(screenshot))
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

    private void showAddServerDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_server, null);
        EditText serverNameEdit = dialogView.findViewById(R.id.server_name_edit);
        EditText serverIpEdit = dialogView.findViewById(R.id.server_ip_edit);
        EditText serverPortEdit = dialogView.findViewById(R.id.server_port_edit);
        serverPortEdit.setText("19132");

        new CustomAlertDialog(this)
                .setTitleText(getString(R.string.quick_launch_add_server))
                .setCustomView(dialogView)
                .setPositiveButton(getString(R.string.add), v -> {
                    String name = serverNameEdit.getText().toString().trim();
                    String ip = serverIpEdit.getText().toString().trim();
                    String portStr = serverPortEdit.getText().toString().trim();

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(ip)) {
                        Toast.makeText(this, R.string.server_details_required, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int port = 19132;
                    if (!TextUtils.isEmpty(portStr)) {
                        try {
                            port = Integer.parseInt(portStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, R.string.invalid_port, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    addServer(new org.levimc.launcher.core.content.ServerItem(name, ip, port));
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void addServer(org.levimc.launcher.core.content.ServerItem server) {
        contentManager.addServer(server, new ContentManager.ContentOperationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(ContentListActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ContentListActivity.this, error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void deleteScreenshot(org.levimc.launcher.core.content.ScreenshotItem screenshot) {
        contentManager.deleteScreenshot(screenshot, new ContentManager.ContentOperationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(ContentListActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ContentListActivity.this, error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void showDeleteServerDialog(org.levimc.launcher.core.content.ServerItem server) {
        new CustomAlertDialog(this)
            .setTitleText(getString(R.string.delete))
            .setMessage("Are you sure you want to delete this server?")
            .setPositiveButton(getString(R.string.dialog_positive_delete), v -> deleteServer(server))
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

    private void deleteServer(org.levimc.launcher.core.content.ServerItem server) {
        contentManager.deleteServer(server, new ContentManager.ContentOperationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(ContentListActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ContentListActivity.this, error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void openCustomFlatWorld() {
        if (worldsDirectory == null || !worldsDirectory.exists()) {
            Toast.makeText(this, "Worlds directory not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, CustomFlatWorldActivity.class);
        intent.putExtra(CustomFlatWorldActivity.EXTRA_WORLDS_DIRECTORY, worldsDirectory.getAbsolutePath());
        customFlatWorldLauncher.launch(intent);
    }

    private void showExtractStructuresDialog(WorldItem world) {
        File worldFile = world.getFile();
        if (worldFile == null || !worldFile.exists()) {
            Toast.makeText(this, "World directory not found", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.loadingOverlay.setVisibility(View.VISIBLE);

        structureExtractor.loadStructures(worldFile, new StructureExtractor.StructureListCallback() {
            @Override
            public void onComplete(List<StructureExtractor.StructureInfo> structures) {
                runOnUiThread(() -> {
                    binding.loadingOverlay.setVisibility(View.GONE);
                    if (structures.isEmpty()) {
                        showNoStructuresFoundDialog();
                    } else {
                        showStructureSelectionDialog(world, structures);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(ContentListActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showNoStructuresFoundDialog() {
        new CustomAlertDialog(this)
            .setTitleText(getString(R.string.no_structures_found_title))
            .setMessage(getString(R.string.no_structures_found_message))
            .setPositiveButton(getString(R.string.dialog_positive_ok), null)
            .show();
    }

    private void showStructureSelectionDialog(WorldItem world, List<StructureExtractor.StructureInfo> structures) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_structure_list, null);
        
        TextView structureCount = dialogView.findViewById(R.id.structure_count);
        RecyclerView recyclerView = dialogView.findViewById(R.id.structures_recycler_view);
        
        structureCount.setText(getString(R.string.structures_found_count, structures.size()));
        
        StructuresAdapter adapter = new StructuresAdapter();
        adapter.setStructures(structures);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.structures_found_title)
            .setView(dialogView)
            .setNegativeButton(R.string.cancel, null)
            .create();
        
        adapter.setOnStructureExportListener(structure -> {
            dialog.dismiss();
            startStructureExport(world, structure);
        });
        
        dialog.show();
    }

    private void startStructureExport(WorldItem world, StructureExtractor.StructureInfo structure) {
        pendingStructureExportWorld = world;
        pendingStructureInfo = structure;
        
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, structure.getFileName());
        structureExportLauncher.launch(intent);
    }

    private void exportStructureToFile(WorldItem world, StructureExtractor.StructureInfo structure, Uri uri) {
        binding.loadingOverlay.setVisibility(View.VISIBLE);

        structureExtractor.exportSingleStructure(structure, uri, new StructureExtractor.ExtractionCallback() {

            @Override
            public void onComplete(int extractedCount, String outputPath) {
                runOnUiThread(() -> {
                    binding.loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(ContentListActivity.this,
                            getString(R.string.structure_exported, structure.getName()),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(ContentListActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showTransferWorldDialog(WorldItem world) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_transfer_content, null);
        RadioGroup radioGroup = dialogView.findViewById(R.id.storage_radio_group);
        RadioButton radioInternal = dialogView.findViewById(R.id.radio_internal);
        RadioButton radioExternal = dialogView.findViewById(R.id.radio_external);
        RadioButton radioVersionIsolation = dialogView.findViewById(R.id.radio_version_isolation);

        switch (currentStorageType) {
            case INTERNAL -> radioInternal.setEnabled(false);
            case EXTERNAL -> radioExternal.setEnabled(false);
            case VERSION_ISOLATION -> radioVersionIsolation.setEnabled(false);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.transfer_content)
            .setView(dialogView)
            .setPositiveButton(R.string.transfer, (d, which) -> {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                FeatureSettings.StorageType targetType = null;
                
                if (selectedId == R.id.radio_internal) {
                    targetType = FeatureSettings.StorageType.INTERNAL;
                } else if (selectedId == R.id.radio_external) {
                    targetType = FeatureSettings.StorageType.EXTERNAL;
                } else if (selectedId == R.id.radio_version_isolation) {
                    targetType = FeatureSettings.StorageType.VERSION_ISOLATION;
                }

                if (targetType != null && targetType != currentStorageType) {
                    transferWorld(world, targetType);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
        
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.accent_text, getTheme()));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.accent_text, getTheme()));
    }

    private void showTransferPackDialog(ResourcePackItem pack) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_transfer_content, null);
        RadioGroup radioGroup = dialogView.findViewById(R.id.storage_radio_group);
        RadioButton radioInternal = dialogView.findViewById(R.id.radio_internal);
        RadioButton radioExternal = dialogView.findViewById(R.id.radio_external);
        RadioButton radioVersionIsolation = dialogView.findViewById(R.id.radio_version_isolation);

        switch (currentStorageType) {
            case INTERNAL -> radioInternal.setEnabled(false);
            case EXTERNAL -> radioExternal.setEnabled(false);
            case VERSION_ISOLATION -> radioVersionIsolation.setEnabled(false);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.transfer_content)
            .setView(dialogView)
            .setPositiveButton(R.string.transfer, (d, which) -> {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                FeatureSettings.StorageType targetType = null;
                
                if (selectedId == R.id.radio_internal) {
                    targetType = FeatureSettings.StorageType.INTERNAL;
                } else if (selectedId == R.id.radio_external) {
                    targetType = FeatureSettings.StorageType.EXTERNAL;
                } else if (selectedId == R.id.radio_version_isolation) {
                    targetType = FeatureSettings.StorageType.VERSION_ISOLATION;
                }

                if (targetType != null && targetType != currentStorageType) {
                    transferPack(pack, targetType);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
        
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.accent_text, getTheme()));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.accent_text, getTheme()));
    }

    private void transferWorld(WorldItem world, FeatureSettings.StorageType targetType) {
        File targetDir = getWorldsDirectoryForType(targetType);
        if (targetDir == null) {
            Toast.makeText(this, getString(R.string.transfer_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        contentManager.transferWorld(world, targetDir, new WorldManager.WorldOperationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ContentListActivity.this, getString(R.string.transfer_success), Toast.LENGTH_SHORT).show();
                    loadContent();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ContentListActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onProgress(int progress) {}
        });
    }

    private void transferPack(ResourcePackItem pack, FeatureSettings.StorageType targetType) {
        File targetDir = getPackDirectoryForType(targetType, pack.isBehaviorPack() ? "behavior_packs" : 
                (contentType == TYPE_SKIN_PACKS ? "skin_packs" : "resource_packs"));
        if (targetDir == null) {
            Toast.makeText(this, getString(R.string.transfer_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        contentManager.transferResourcePack(pack, targetDir, new ResourcePackManager.PackOperationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ContentListActivity.this, getString(R.string.transfer_success), Toast.LENGTH_SHORT).show();
                    loadContent();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ContentListActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onProgress(int progress) {}
        });
    }

    private File getWorldsDirectoryForType(FeatureSettings.StorageType storageType) {
        GameVersion currentVersion = versionManager.getSelectedVersion();
        
        switch (storageType) {
            case VERSION_ISOLATION:
                if (currentVersion != null && currentVersion.versionDir != null) {
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

    private File getPackDirectoryForType(FeatureSettings.StorageType storageType, String packType) {
        GameVersion currentVersion = versionManager.getSelectedVersion();
        
        switch (storageType) {
            case VERSION_ISOLATION:
                if (currentVersion != null && currentVersion.versionDir != null) {
                    File gameDataDir = new File(currentVersion.versionDir, "games/com.mojang");
                    return new File(gameDataDir, packType);
                }
                break;
            case EXTERNAL:
                File externalDir = getExternalFilesDir(null);
                if (externalDir != null) {
                    File gameDataDir = new File(externalDir, "games/com.mojang");
                    return new File(gameDataDir, packType);
                }
                break;
            case INTERNAL:
                File internalDir = new File(getDataDir(), "games/com.mojang");
                return new File(internalDir, packType);
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadContent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (structureExtractor != null) {
            structureExtractor.shutdown();
        }
    }
}

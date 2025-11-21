package org.levimc.launcher.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.levimc.launcher.R;
import org.levimc.launcher.core.content.ContentManager;
import org.levimc.launcher.core.content.ResourcePackItem;
import org.levimc.launcher.core.content.ResourcePackManager;
import org.levimc.launcher.core.content.WorldItem;
import org.levimc.launcher.core.content.WorldManager;
import org.levimc.launcher.databinding.ActivityContentListBinding;
import org.levimc.launcher.ui.adapter.ResourcePacksAdapter;
import org.levimc.launcher.ui.adapter.WorldsAdapter;
import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;

public class ContentListActivity extends BaseActivity {

    public static final String EXTRA_CONTENT_TYPE = "content_type";
    public static final int TYPE_WORLDS = 0;
    public static final int TYPE_SKIN_PACKS = 1;
    public static final int TYPE_RESOURCE_PACKS = 2;
    public static final int TYPE_BEHAVIOR_PACKS = 3;

    private ActivityContentListBinding binding;
    private ContentManager contentManager;
    private int contentType;

    private WorldsAdapter worldsAdapter;
    private ResourcePacksAdapter packsAdapter;

    private ActivityResultLauncher<Intent> importLauncher;
    private ActivityResultLauncher<Intent> exportLauncher;
    private WorldItem pendingExportWorld;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContentListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        DynamicAnim.applyPressScaleRecursively(binding.getRoot());

        contentType = getIntent().getIntExtra(EXTRA_CONTENT_TYPE, TYPE_WORLDS);
        contentManager = ContentManager.getInstance(this);

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
    }

    private void setupUI() {
        binding.backButton.setOnClickListener(v -> finish());

        switch (contentType) {
            case TYPE_WORLDS:
                binding.titleText.setText(getString(R.string.worlds_title));
                binding.importButton.setText(getString(R.string.import_world));
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
        }

        binding.importButton.setOnClickListener(v -> startImport());
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

        binding.contentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.contentRecyclerView.setAdapter(worldsAdapter);
        binding.contentRecyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(binding.contentRecyclerView));
    }

    private void setupPacksRecyclerView() {
        packsAdapter = new ResourcePacksAdapter();
        packsAdapter.setOnResourcePackActionListener(pack -> showDeletePackDialog(pack));

        binding.contentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.contentRecyclerView.setAdapter(packsAdapter);
        binding.contentRecyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(binding.contentRecyclerView));
    }

    private void setupObservers() {
        switch (contentType) {
            case TYPE_WORLDS:
                contentManager.getWorldsLiveData().observe(this, worlds -> {
                    if (worldsAdapter != null) {
                        worldsAdapter.updateWorlds(worlds);
                    }
                });
                break;
            case TYPE_SKIN_PACKS:
                contentManager.getSkinPacksLiveData().observe(this, packs -> {
                    if (packsAdapter != null) {
                        packsAdapter.updateResourcePacks(packs);
                    }
                });
                break;
            case TYPE_RESOURCE_PACKS:
                contentManager.getResourcePacksLiveData().observe(this, packs -> {
                    if (packsAdapter != null) {
                        packsAdapter.updateResourcePacks(packs);
                    }
                });
                break;
            case TYPE_BEHAVIOR_PACKS:
                contentManager.getBehaviorPacksLiveData().observe(this, packs -> {
                    if (packsAdapter != null) {
                        packsAdapter.updateResourcePacks(packs);
                    }
                });
                break;
        }
    }

    private void loadContent() {
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
        }
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
        new CustomAlertDialog(this)
            .setTitleText(getString(R.string.delete_resource_pack))
            .setMessage(getString(R.string.confirm_delete_resource_pack))
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
}

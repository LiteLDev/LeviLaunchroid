package org.levimc.launcher.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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
import org.levimc.launcher.core.content.StructureExtractor;
import org.levimc.launcher.core.content.WorldItem;
import org.levimc.launcher.core.content.WorldManager;
import org.levimc.launcher.databinding.ActivityContentListBinding;
import org.levimc.launcher.ui.adapter.ResourcePacksAdapter;
import org.levimc.launcher.ui.adapter.StructuresAdapter;
import org.levimc.launcher.ui.adapter.WorldsAdapter;
import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ContentListActivity extends BaseActivity {

    public static final String EXTRA_CONTENT_TYPE = "content_type";
    public static final String EXTRA_WORLDS_DIRECTORY = "worlds_directory";
    public static final int TYPE_WORLDS = 0;
    public static final int TYPE_SKIN_PACKS = 1;
    public static final int TYPE_RESOURCE_PACKS = 2;
    public static final int TYPE_BEHAVIOR_PACKS = 3;

    private ActivityContentListBinding binding;
    private ContentManager contentManager;
    private int contentType;
    private File worldsDirectory;

    private WorldsAdapter worldsAdapter;
    private ResourcePacksAdapter packsAdapter;

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
        }

        binding.importButton.setOnClickListener(v -> startImport());
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
        packsAdapter.setOnResourcePackActionListener(pack -> showDeletePackDialog(pack));

        binding.contentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.contentRecyclerView.setAdapter(packsAdapter);
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

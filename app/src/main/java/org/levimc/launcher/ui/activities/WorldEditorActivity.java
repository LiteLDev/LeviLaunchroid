package org.levimc.launcher.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import org.levimc.launcher.R;
import org.levimc.launcher.core.content.WorldEditor;
import org.levimc.launcher.core.content.WorldEditor.WorldProperty;
import org.levimc.launcher.databinding.ActivityWorldEditorBinding;
import org.levimc.launcher.ui.adapter.WorldPropertiesAdapter;
import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorldEditorActivity extends BaseActivity {

    public static final String EXTRA_WORLD_PATH = "world_path";
    public static final String EXTRA_WORLD_NAME = "world_name";

    private ActivityWorldEditorBinding binding;
    private WorldEditor worldEditor;
    private WorldPropertiesAdapter adapter;
    private ExecutorService executor;
    
    private List<WorldProperty> allProperties = new ArrayList<>();
    private boolean hasUnsavedChanges = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWorldEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        DynamicAnim.applyPressScaleRecursively(binding.getRoot());

        executor = Executors.newSingleThreadExecutor();

        String worldPath = getIntent().getStringExtra(EXTRA_WORLD_PATH);
        String worldName = getIntent().getStringExtra(EXTRA_WORLD_NAME);

        if (worldPath == null) {
            Toast.makeText(this, "Invalid world path", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        worldEditor = new WorldEditor(new File(worldPath));
        
        setupUI(worldName);
        loadWorldData();
    }

    private void setupUI(String worldName) {
        binding.backButton.setOnClickListener(v -> onBackPressed());
        binding.titleText.setText(worldName != null ? worldName : getString(R.string.edit_world));
        
        binding.saveButton.setOnClickListener(v -> saveChanges());
        binding.saveButton.setEnabled(false);

        adapter = new WorldPropertiesAdapter();
        adapter.setOnPropertyChangedListener((property, newValue) -> {
            worldEditor.updateLevelDatProperty(property.getPath(), newValue);
            hasUnsavedChanges = true;
            binding.saveButton.setEnabled(true);
        });

        binding.propertiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.propertiesRecyclerView.setAdapter(adapter);
    }

    private void loadWorldData() {
        binding.loadingProgress.setVisibility(View.VISIBLE);
        binding.propertiesRecyclerView.setVisibility(View.GONE);
        binding.emptyText.setVisibility(View.GONE);

        executor.execute(() -> {
            try {
                if (worldEditor.hasLevelDat()) {
                    worldEditor.loadLevelDat();
                }
                
                runOnUiThread(this::loadLevelDatProperties);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    binding.loadingProgress.setVisibility(View.GONE);
                    binding.emptyText.setVisibility(View.VISIBLE);
                    binding.emptyText.setText(getString(R.string.error_loading_world) + ": " + e.getMessage());
                });
            }
        });
    }

    private void loadLevelDatProperties() {
        binding.loadingProgress.setVisibility(View.GONE);
        
        if (!worldEditor.isLevelDatLoaded()) {
            binding.emptyText.setVisibility(View.VISIBLE);
            binding.emptyText.setText(R.string.level_dat_not_found);
            binding.propertiesRecyclerView.setVisibility(View.GONE);
            return;
        }

        allProperties = worldEditor.getLevelDatProperties();
        
        if (allProperties.isEmpty()) {
            binding.emptyText.setVisibility(View.VISIBLE);
            binding.emptyText.setText(R.string.no_editable_properties);
            binding.propertiesRecyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyText.setVisibility(View.GONE);
            binding.propertiesRecyclerView.setVisibility(View.VISIBLE);

            Map<String, List<WorldProperty>> grouped = groupByCategory(allProperties);
            adapter.setProperties(grouped);
        }
    }

    private Map<String, List<WorldProperty>> groupByCategory(List<WorldProperty> properties) {
        Map<String, List<WorldProperty>> grouped = new LinkedHashMap<>();
        
        for (WorldProperty prop : properties) {
            String category = prop.getCategory();
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(prop);
        }
        
        return grouped;
    }

    private void saveChanges() {
        if (!hasUnsavedChanges) return;

        binding.loadingProgress.setVisibility(View.VISIBLE);
        binding.saveButton.setEnabled(false);

        executor.execute(() -> {
            try {
                worldEditor.saveLevelDat();
                
                runOnUiThread(() -> {
                    binding.loadingProgress.setVisibility(View.GONE);
                    hasUnsavedChanges = false;
                    Toast.makeText(this, R.string.world_saved, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    binding.loadingProgress.setVisibility(View.GONE);
                    binding.saveButton.setEnabled(true);
                    Toast.makeText(this, getString(R.string.save_failed) + ": " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges) {
            new CustomAlertDialog(this)
                .setTitleText(getString(R.string.unsaved_changes))
                .setMessage(getString(R.string.discard_changes_message))
                .setPositiveButton(getString(R.string.discard), v -> super.onBackPressed())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}

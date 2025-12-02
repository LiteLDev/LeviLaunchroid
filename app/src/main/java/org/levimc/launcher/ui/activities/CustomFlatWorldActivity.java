package org.levimc.launcher.ui.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.content.FlatWorldGenerator;
import org.levimc.launcher.core.content.FlatWorldGenerator.BlockLayer;
import org.levimc.launcher.databinding.ActivityCustomFlatWorldBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomFlatWorldActivity extends BaseActivity {

    public static final String EXTRA_WORLDS_DIRECTORY = "worlds_directory";

    private ActivityCustomFlatWorldBinding binding;
    private LayersAdapter layersAdapter;
    private List<BlockLayer> layers;
    private File worldsDirectory;
    private ExecutorService executor;

    private static final String[] BIOME_NAMES = {
            "Plains", "Desert", "Mountains", "Forest", "Taiga", "Swamp", "River", "Nether Wastes",
            "The End", "Frozen Ocean", "Frozen River", "Snowy Plains", "Snowy Mountains", "Mushroom Fields",
            "Beach", "Jungle", "Sparse Jungle", "Deep Ocean", "Stony Shore", "Snowy Beach",
            "Birch Forest", "Dark Forest", "Snowy Taiga", "Old Growth Pine Taiga", "Windswept Forest",
            "Savanna", "Savanna Plateau", "Badlands", "Wooded Badlands", "Warm Ocean"
    };

    private static final int[] BIOME_IDS = {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 21, 23, 24, 25, 26,
            27, 29, 30, 32, 34, 35, 36, 37, 38, 44
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomFlatWorldBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        executor = Executors.newSingleThreadExecutor();

        String worldsPath = getIntent().getStringExtra(EXTRA_WORLDS_DIRECTORY);
        if (worldsPath == null) {
            Toast.makeText(this, "Invalid worlds directory", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        worldsDirectory = new File(worldsPath);

        layers = new ArrayList<>(FlatWorldGenerator.getDefaultLayers());

        setupUI();
    }

    private void setupUI() {
        binding.backButton.setOnClickListener(v -> finish());

        ArrayAdapter<String> biomeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, BIOME_NAMES);
        biomeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.biomeSpinner.setAdapter(biomeAdapter);

        String[] gameModes = {"Survival", "Creative", "Adventure"};
        ArrayAdapter<String> gameModeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, gameModes);
        gameModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.gameModeSpinner.setAdapter(gameModeAdapter);
        binding.gameModeSpinner.setSelection(1);

        layersAdapter = new LayersAdapter();
        binding.layersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.layersRecyclerView.setAdapter(layersAdapter);

        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();
                Collections.swap(layers, from, to);
                layersAdapter.notifyItemMoved(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                layers.remove(pos);
                layersAdapter.notifyItemRemoved(pos);
            }
        });
        touchHelper.attachToRecyclerView(binding.layersRecyclerView);

        binding.addLayerButton.setOnClickListener(v -> {
            layers.add(new BlockLayer("minecraft:stone", 1));
            layersAdapter.notifyItemInserted(layers.size() - 1);
            binding.layersRecyclerView.scrollToPosition(layers.size() - 1);
        });

        binding.createWorldButton.setOnClickListener(v -> createWorld());
    }

    private void saveAllLayerValues() {
        for (int i = 0; i < binding.layersRecyclerView.getChildCount(); i++) {
            View child = binding.layersRecyclerView.getChildAt(i);
            RecyclerView.ViewHolder holder = binding.layersRecyclerView.getChildViewHolder(child);
            if (holder != null) {
                int position = holder.getAdapterPosition();
                if (position >= 0 && position < layers.size()) {
                    EditText countEdit = child.findViewById(R.id.count_edit);
                    EditText blockEdit = child.findViewById(R.id.block_edit);
                    if (countEdit != null) {
                        try {
                            int count = Integer.parseInt(countEdit.getText().toString());
                            layers.get(position).count = Math.max(1, Math.min(count, 256));
                        } catch (NumberFormatException e) {
                            layers.get(position).count = 1;
                        }
                    }
                    if (blockEdit != null) {
                        String blockName = blockEdit.getText().toString().trim();
                        if (!blockName.isEmpty()) {
                            layers.get(position).blockName = blockName;
                        }
                    }
                }
            }
        }
    }


    private void createWorld() {
        binding.getRoot().clearFocus();

        String worldName = binding.worldNameEdit.getText().toString().trim();
        if (worldName.isEmpty()) {
            Toast.makeText(this, R.string.enter_world_name, Toast.LENGTH_SHORT).show();
            return;
        }

        if (layers.isEmpty()) {
            Toast.makeText(this, R.string.add_at_least_one_layer, Toast.LENGTH_SHORT).show();
            return;
        }

        saveAllLayerValues();

        binding.createWorldButton.setEnabled(false);
        binding.loadingProgress.setVisibility(View.VISIBLE);

        int biomeIndex = binding.biomeSpinner.getSelectedItemPosition();
        int biomeId = biomeIndex < BIOME_IDS.length ? BIOME_IDS[biomeIndex] : 1;
        int gameMode = binding.gameModeSpinner.getSelectedItemPosition();

        List<BlockLayer> layersCopy = new ArrayList<>();
        for (BlockLayer layer : layers) {
            layersCopy.add(new BlockLayer(layer.blockName, layer.count));
        }

        executor.execute(() -> {
            try {
                FlatWorldGenerator.generateFlatWorld(worldsDirectory, worldName, layersCopy, biomeId, gameMode);

                runOnUiThread(() -> {
                    binding.loadingProgress.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.world_created_successfully, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    binding.loadingProgress.setVisibility(View.GONE);
                    binding.createWorldButton.setEnabled(true);
                    Toast.makeText(this, getString(R.string.failed_to_create_world) + ": " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    private class LayersAdapter extends RecyclerView.Adapter<LayersAdapter.LayerViewHolder> {

        @NonNull
        @Override
        public LayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_flat_layer, parent, false);
            return new LayerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LayerViewHolder holder, int position) {
            BlockLayer layer = layers.get(position);
            holder.bind(layer, position);
        }

        @Override
        public int getItemCount() {
            return layers.size();
        }

        class LayerViewHolder extends RecyclerView.ViewHolder {
            EditText blockEdit;
            EditText countEdit;
            TextView layerNumber;
            private boolean isBinding = false;

            LayerViewHolder(View itemView) {
                super(itemView);
                blockEdit = itemView.findViewById(R.id.block_edit);
                countEdit = itemView.findViewById(R.id.count_edit);
                layerNumber = itemView.findViewById(R.id.layer_number);

                blockEdit.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        if (isBinding) return;
                        int adapterPos = getBindingAdapterPosition();
                        if (adapterPos >= 0 && adapterPos < layers.size()) {
                            String blockName = s.toString().trim();
                            if (!blockName.isEmpty()) {
                                layers.get(adapterPos).blockName = blockName;
                            }
                        }
                    }
                });

                countEdit.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        if (isBinding) return;
                        int adapterPos = getBindingAdapterPosition();
                        if (adapterPos >= 0 && adapterPos < layers.size()) {
                            try {
                                int count = Integer.parseInt(s.toString());
                                layers.get(adapterPos).count = Math.max(1, Math.min(count, 256));
                            } catch (NumberFormatException e) {
                            }
                        }
                    }
                });
            }

            void bind(BlockLayer layer, int position) {
                isBinding = true;
                layerNumber.setText(String.valueOf(position + 1));
                blockEdit.setText(layer.blockName);
                countEdit.setText(String.valueOf(layer.count));
                isBinding = false;
            }
        }
    }
}

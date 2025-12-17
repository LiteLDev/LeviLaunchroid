package org.levimc.launcher.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.core.view.ViewCompat;
import androidx.core.app.ActivityOptionsCompat;
import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.FileHandler;
import org.levimc.launcher.core.mods.Mod;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.model.InbuiltMod;
import org.levimc.launcher.core.versions.VersionManager;
import org.levimc.launcher.ui.adapter.InbuiltModsListAdapter;
import org.levimc.launcher.ui.adapter.ModsAdapter;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;
import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.ui.views.MainViewModel;
import org.levimc.launcher.ui.views.MainViewModelFactory;
import java.util.ArrayList;
import java.util.List;

public class ModsFullscreenActivity extends BaseActivity {

    private RecyclerView modsRecycler;
    private RecyclerView inbuiltModsRecycler;
    private ModsAdapter modsAdapter;
    private InbuiltModsListAdapter inbuiltModsAdapter;
    private MainViewModel viewModel;
    private TextView totalModsCount;
    private TextView enabledModsCount;
    private TextView inbuiltModsHeader;
    private TextView externalModsHeader;
    private ActivityResultLauncher<Intent> pickModLauncher;
    private FileHandler fileHandler;
    private InbuiltModManager inbuiltModManager;
    private int lastModsCount = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mods_fullscreen);

        View root = findViewById(android.R.id.content);
        if (root != null) {
            DynamicAnim.applyPressScaleRecursively(root);
        }

        inbuiltModManager = InbuiltModManager.getInstance(this);
        setupViews();
        setupViewModel();
        setupRecyclerView();
        setupInbuiltModsRecycler();
        fileHandler = new FileHandler(this, viewModel, VersionManager.get(this));
        
        pickModLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        fileHandler.processIncomingFilesWithConfirmation(result.getData(), new FileHandler.FileOperationCallback() {
                            @Override
                            public void onSuccess(int processedFiles) {
                                Toast.makeText(ModsFullscreenActivity.this, getString(R.string.files_processed, processedFiles), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String errorMessage) {
                            }

                            @Override
                            public void onProgressUpdate(int progress) {
                            }
                        }, true);
                    }
                }
        );
    }

    private void setupViews() {
        ImageButton closeButton = findViewById(R.id.close_fullscreen_button);
        closeButton.setOnClickListener(v -> finish());
        DynamicAnim.applyPressScale(closeButton);

        Button addModButton = findViewById(R.id.add_mod_fullscreen_button);
        addModButton.setOnClickListener(v -> {
            startFilePicker();
        });
        DynamicAnim.applyPressScale(addModButton);

        Button inbuiltModsButton = findViewById(R.id.inbuilt_mods_button);
        inbuiltModsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, InbuiltModsActivity.class);
            startActivity(intent);
        });
        DynamicAnim.applyPressScale(inbuiltModsButton);

        totalModsCount = findViewById(R.id.total_mods_count);
        enabledModsCount = findViewById(R.id.enabled_mods_count);
        inbuiltModsHeader = findViewById(R.id.inbuilt_mods_header);
        externalModsHeader = findViewById(R.id.external_mods_header);
    }
    
    private void startFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        pickModLauncher.launch(intent);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this, new MainViewModelFactory(getApplication())).get(MainViewModel.class);

        viewModel.getModsLiveData().observe(this, this::updateModsUI);
    }

    private void setupRecyclerView() {
        modsRecycler = findViewById(R.id.mods_recycler_fullscreen);
        modsAdapter = new ModsAdapter(new ArrayList<>());
        modsRecycler.setLayoutManager(new LinearLayoutManager(this));
        modsRecycler.setAdapter(modsAdapter);
        if (modsRecycler.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) modsRecycler.getItemAnimator()).setSupportsChangeAnimations(false);
        }

        modsRecycler.post(() -> DynamicAnim.staggerRecyclerChildren(modsRecycler));

        modsAdapter.setOnModClickListener((mod, position, sharedView) -> {
            Intent intent = new Intent(this, ModDetailActivity.class);
            intent.putExtra("mod_filename", mod.getFileName());
            intent.putExtra("mod_position", position);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this,
                    sharedView,
                    ViewCompat.getTransitionName(sharedView)
            );
            startActivity(intent, options.toBundle());
        });

        modsAdapter.setOnModEnableChangeListener((mod, enabled) -> {
            if (viewModel != null) {
                viewModel.setModEnabled(mod.getFileName(), enabled);
                updateModsCount(); 
            }
        });
        
        modsAdapter.setOnModReorderListener(reorderedMods -> {
            if (viewModel != null) {
                viewModel.reorderMods(reorderedMods);
                Toast.makeText(this, R.string.mod_reordered, Toast.LENGTH_SHORT).show();
            }
        });
        
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                modsAdapter.moveItem(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                Mod mod = modsAdapter.getItem(pos);
                new CustomAlertDialog(ModsFullscreenActivity.this)
                        .setTitleText(getString(R.string.dialog_title_delete_mod))
                        .setMessage(getString(R.string.dialog_message_delete_mod))
                        .setPositiveButton(getString(R.string.dialog_positive_delete), v -> {
                            viewModel.removeMod(mod);
                            modsAdapter.removeAt(pos);
                            updateModsCount();
                        })
                        .setNegativeButton(getString(R.string.dialog_negative_cancel), v -> {
                            modsAdapter.notifyItemChanged(pos);
                        })
                        .show();
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(modsRecycler);
    }

    private void setupInbuiltModsRecycler() {
        inbuiltModsRecycler = findViewById(R.id.inbuilt_mods_recycler);
        inbuiltModsAdapter = new InbuiltModsListAdapter();
        inbuiltModsRecycler.setLayoutManager(new LinearLayoutManager(this));
        inbuiltModsRecycler.setAdapter(inbuiltModsAdapter);
        
        inbuiltModsAdapter.setOnRemoveClickListener(mod -> {
            inbuiltModManager.removeMod(mod.getId());
            Toast.makeText(this, getString(R.string.inbuilt_mod_removed, mod.getName()), Toast.LENGTH_SHORT).show();
            refreshInbuiltMods();
            updateModsCount();
        });
        
        refreshInbuiltMods();
    }

    private void refreshInbuiltMods() {
        List<InbuiltMod> addedMods = inbuiltModManager.getAddedMods(this);
        inbuiltModsAdapter.updateMods(addedMods);
        boolean hasInbuilt = !addedMods.isEmpty();
        inbuiltModsRecycler.setVisibility(hasInbuilt ? View.VISIBLE : View.GONE);
        if (inbuiltModsHeader != null) {
            inbuiltModsHeader.setVisibility(hasInbuilt ? View.VISIBLE : View.GONE);
        }
        updateExternalModsHeader();
    }
    
    private void updateExternalModsHeader() {
        List<Mod> mods = viewModel.getModsLiveData().getValue();
        List<InbuiltMod> inbuiltMods = inbuiltModManager.getAddedMods(this);
        boolean hasExternal = mods != null && !mods.isEmpty();
        boolean hasInbuilt = !inbuiltMods.isEmpty();
        if (externalModsHeader != null) {
            externalModsHeader.setVisibility(hasExternal && hasInbuilt ? View.VISIBLE : View.GONE);
        }
    }

    private void updateModsUI(List<Mod> mods) {
        if (modsAdapter != null) {
            modsAdapter.updateMods(mods);
            updateModsCount();
            updateExternalModsHeader();
            if (modsRecycler != null) {
                int count = (mods != null) ? mods.size() : 0;
                if (lastModsCount == -1 || count != lastModsCount) {
                    modsRecycler.post(() -> DynamicAnim.staggerRecyclerChildren(modsRecycler));
                }
                lastModsCount = count;
            }
        }
    }

    private void updateModsCount() {
        List<Mod> mods = viewModel.getModsLiveData().getValue();
        InbuiltModManager inbuiltManager = InbuiltModManager.getInstance(this);
        List<InbuiltMod> inbuiltMods = inbuiltManager.getAddedMods(this);
        
        int total = (mods != null ? mods.size() : 0) + inbuiltMods.size();
        int enabled = inbuiltMods.size();
        
        if (mods != null) {
            for (Mod mod : mods) {
                if (mod.isEnabled()) {
                    enabled++;
                }
            }
        }

        totalModsCount.setText(String.valueOf(total));
        enabledModsCount.setText(String.valueOf(enabled));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshMods();
        }
        refreshInbuiltMods();
        updateModsCount();
    }
}

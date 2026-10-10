package org.levimc.launcher.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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
import androidx.core.widget.NestedScrollView;
import android.graphics.Canvas;
import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.FileHandler;
import org.levimc.launcher.core.mods.Mod;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.versions.VersionManager;
import org.levimc.launcher.ui.adapter.ModsAdapter;
import org.levimc.launcher.ui.adapter.ScannedModsAdapter;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;
import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.ui.views.MainViewModel;
import org.levimc.launcher.ui.views.MainViewModelFactory;
import org.levimc.launcher.util.StorageAccess;
import org.levimc.launcher.util.PersonalizationManager;
import java.util.ArrayList;
import java.util.List;

public class ModsFullscreenActivity extends BaseActivity {

    private RecyclerView modsRecycler;
    private ModsAdapter modsAdapter;
    private MainViewModel viewModel;
    private TextView totalModsCount;
    private TextView enabledModsCount;
    private ActivityResultLauncher<Intent> pickModLauncher;
    private ActivityResultLauncher<Intent> scanDownloadsLauncher;
    private FileHandler fileHandler;
    private InbuiltModManager inbuiltModManager;
    private Button scanModsButton;
    private boolean scanInProgress;
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
        fileHandler = new FileHandler(this, viewModel, VersionManager.get(this));

        scanDownloadsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        scanDownloads(result.getData());
                    }
                });

        pickModLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        for (Uri uri : StorageAccess.selectedUris(result.getData())) {
                            StorageAccess.retainReadPermission(this, result.getData(), uri);
                        }
                        fileHandler.processIncomingFilesWithConfirmation(
                                result.getData(), createImportCallback(), true);
                    }
                }
        );
    }

    private void setupViews() {
        Button addModButton = findViewById(R.id.add_mod_fullscreen_button);
        addModButton.setVisibility(View.VISIBLE);
        addModButton.setOnClickListener(v -> {
            startFilePicker();
        });
        DynamicAnim.applyPressScale(addModButton);

        scanModsButton = findViewById(R.id.scan_mods_button);
        scanModsButton.setEnabled(!scanInProgress);
        scanModsButton.setText(scanInProgress
                ? R.string.scan_downloads_scanning
                : R.string.scan_downloads);
        scanModsButton.setOnClickListener(v -> requestDownloadsScan());
        DynamicAnim.applyPressScale(scanModsButton);

        Button modMenuButton = findViewById(R.id.mod_menu_button);
        boolean isMenuEnabled = inbuiltModManager.isModMenuEnabled();
        modMenuButton.setText(getString(R.string.mod_menu) + ": " + (isMenuEnabled ? "ON" : "OFF"));
        modMenuButton.setOnClickListener(v -> {
            boolean current = inbuiltModManager.isModMenuEnabled();
            inbuiltModManager.setModMenuEnabled(!current);
            modMenuButton.setText(getString(R.string.mod_menu) + ": " + (!current ? "ON" : "OFF"));
            Toast.makeText(this, !current ? R.string.mod_menu_enabled : R.string.mod_menu_disabled, Toast.LENGTH_SHORT).show();
        });
        DynamicAnim.applyPressScale(modMenuButton);

        Button externalModsButton = findViewById(R.id.external_mods_button);
        externalModsButton.setOnClickListener(v -> startActivity(
                new Intent(this, ExternalModsActivity.class),
                ActivityOptionsCompat.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out).toBundle()));
        DynamicAnim.applyPressScale(externalModsButton);

        totalModsCount = findViewById(R.id.total_mods_count);
        enabledModsCount = findViewById(R.id.enabled_mods_count);

        PersonalizationManager personalizationManager = new PersonalizationManager(this);
        View root = findViewById(android.R.id.content);
        if (root != null) {
            personalizationManager.applyAccentToView(root, this);
        }
    }

    private void startFilePicker() {
        pickModLauncher.launch(StorageAccess.downloadsPicker(true));
    }

    private void requestDownloadsScan() {
        if (scanInProgress) return;
        scanDownloadsLauncher.launch(StorageAccess.downloadsPicker(true));
    }

    private void scanDownloads(Intent data) {
        scanInProgress = true;
        updateScanButton();
        List<Uri> uris = StorageAccess.selectedUris(data);
        for (Uri uri : uris) StorageAccess.retainReadPermission(this, data, uri);
        new Thread(() -> {
            List<StorageAccess.Document> files = new ArrayList<>();
            boolean failed = false;
            for (Uri uri : uris) {
                try {
                    StorageAccess.Document document = StorageAccess.readDocument(this, uri);
                    String name = document.name.toLowerCase(java.util.Locale.ROOT);
                    if (name.endsWith(".levipack") || name.endsWith(".so")) files.add(document);
                } catch (Exception error) {
                    failed = true;
                }
            }
            boolean scanFailed = failed;
            runOnUiThread(() -> {
                scanInProgress = false;
                if (isFinishing() || isDestroyed()) return;
                updateScanButton();
                if (scanFailed) {
                    Toast.makeText(this, R.string.scan_downloads_failed, Toast.LENGTH_LONG).show();
                }
                if (!files.isEmpty()) showScannedMods(files);
                else if (!scanFailed) Toast.makeText(this, R.string.scan_downloads_none_found, Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void showScannedMods(List<StorageAccess.Document> files) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_scanned_mods, null);
        RecyclerView results = content.findViewById(R.id.scan_results_recycler);
        results.setLayoutManager(new LinearLayoutManager(this));

        ScannedModsAdapter[] adapterHolder = new ScannedModsAdapter[1];
        adapterHolder[0] = new ScannedModsAdapter(files,
                file -> addScannedMod(file, adapterHolder[0]));
        results.setAdapter(adapterHolder[0]);

        float density = getResources().getDisplayMetrics().density;
        ViewGroup.LayoutParams params = results.getLayoutParams();
        int desiredHeight = (int) (Math.min(files.size(), 4) * 72 * density);
        int availableHeight = getResources().getDisplayMetrics().heightPixels - (int) (190 * density);
        params.height = Math.min(desiredHeight, Math.max((int) (72 * density), availableHeight));
        results.setLayoutParams(params);

        new CustomAlertDialog(this)
                .setTitleText(getString(R.string.scan_downloads_results_title))
                .setCustomView(content)
                .setNegativeButton(getString(R.string.close), null)
                .setUseBorderedBackground(true)
                .setBlurBackground(true)
                .show();
    }

    private void addScannedMod(StorageAccess.Document file, ScannedModsAdapter adapter) {
        adapter.setImporting(file);
        fileHandler.processScannedFile(file.uri, new FileHandler.FileOperationCallback() {
            @Override
            public void onSuccess(int processedFiles) {
                if (isFinishing() || isDestroyed()) return;
                adapter.setAdded(file);
                Toast.makeText(ModsFullscreenActivity.this,
                        getString(R.string.scan_downloads_added_message, file.name),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;
                adapter.setIdle(file);
                Toast.makeText(ModsFullscreenActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgressUpdate(int progress) {
            }
        });
    }

    private void updateScanButton() {
        if (scanModsButton == null) return;
        scanModsButton.setEnabled(!scanInProgress);
        scanModsButton.setText(scanInProgress
                ? R.string.scan_downloads_scanning
                : R.string.scan_downloads);
    }

    private FileHandler.FileOperationCallback createImportCallback() {
        return new FileHandler.FileOperationCallback() {
            @Override
            public void onSuccess(int processedFiles) {
                Toast.makeText(ModsFullscreenActivity.this,
                        getString(R.string.files_processed, processedFiles), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ModsFullscreenActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgressUpdate(int progress) {
            }
        };
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this, new MainViewModelFactory(getApplication())).get(MainViewModel.class);

        org.levimc.launcher.core.versions.GameVersion selectedVersion = VersionManager.get(this).getSelectedVersion();
        if (selectedVersion != null) {
            viewModel.setCurrentVersion(selectedVersion);
        }

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
            intent.putExtra("mod_filename", mod.getId());
            intent.putExtra("mod_position", position);
            startActivity(intent);
        });

        modsAdapter.setOnModEnableChangeListener((mod, enabled) -> {
            if (viewModel != null) {
                viewModel.setModEnabled(mod.getId(), enabled);
                updateModsCount(); 
            }
        });
        
        modsAdapter.setOnModReorderListener(reorderedMods -> {
            if (viewModel != null) {
                viewModel.reorderMods(reorderedMods);
                Toast.makeText(this, R.string.mod_reordered, Toast.LENGTH_SHORT).show();
            }
        });
        
        NestedScrollView nestedScrollView = findViewById(R.id.nested_scroll_view);
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {
            private long lastScrollTime = 0;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                modsAdapter.moveItem(fromPosition, toPosition);
                return true;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                modsAdapter.commitReorder();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive && nestedScrollView != null) {
                    long now = System.currentTimeMillis();
                    if (now - lastScrollTime > 16) {
                        int[] location = new int[2];
                        viewHolder.itemView.getLocationOnScreen(location);
                        int y = location[1];

                        nestedScrollView.getLocationOnScreen(location);
                        int svY = location[1];
                        int svHeight = nestedScrollView.getHeight();

                        int scrollZone = (int) (80 * recyclerView.getResources().getDisplayMetrics().density);
                        int scrollAmount = 0;
                        int maxScrollSpeed = 15;

                        if (y < svY + scrollZone) {
                            float ratio = 1.0f - Math.max(0, y - svY) / (float) scrollZone;
                            scrollAmount = (int) (-maxScrollSpeed * ratio);
                        } else if (y + viewHolder.itemView.getHeight() > svY + svHeight - scrollZone) {
                            float ratio = 1.0f - Math.max(0, svY + svHeight - (y + viewHolder.itemView.getHeight())) / (float) scrollZone;
                            scrollAmount = (int) (maxScrollSpeed * ratio);
                        }

                        if (scrollAmount != 0) {
                            nestedScrollView.scrollBy(0, scrollAmount);
                            lastScrollTime = now;
                            recyclerView.invalidate();
                        }
                    }
                }
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
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(modsRecycler);
        modsAdapter.setItemTouchHelper(itemTouchHelper);
    }

    private void updateModsUI(List<Mod> mods) {
        if (modsAdapter != null) {
            modsAdapter.updateMods(mods);
            updateModsCount();
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
        
        int total = (mods != null ? mods.size() : 0);
        int enabled = 0;
        
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
        setupViews();
        if (viewModel != null) {
            viewModel.refreshMods();
        }
        updateModsCount();
    }

}

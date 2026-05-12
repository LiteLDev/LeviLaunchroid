package org.levimc.launcher.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.core.versions.VersionManager;
import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.util.ApkImportManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstancesActivity extends BaseActivity {

    private static final int FILTER_ALL = 0;
    private static final int FILTER_CUSTOM = 1;

    private VersionManager versionManager;
    private RecyclerView recyclerView;
    private InstanceCardAdapter adapter;
    private TextView filterAll, filterCustom;
    private TextView instanceCountBadge;
    private EditText searchInput;
    private int currentFilter = FILTER_ALL;
    private List<GameVersion> allVersions = new ArrayList<>();

    private ApkImportManager apkImportManager;
    private ActivityResultLauncher<Intent> apkImportResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instances);
        setupNavBar();

        versionManager = VersionManager.get(this);
        versionManager.loadAllVersions();

        apkImportManager = new ApkImportManager(this, null);
        apkImportResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (apkImportManager != null)
                        apkImportManager.handleActivityResult(result.getResultCode(), result.getData());
                }
        );

        recyclerView = findViewById(R.id.instances_recycler);
        filterAll = findViewById(R.id.filter_all);
        filterCustom = findViewById(R.id.filter_custom);
        instanceCountBadge = findViewById(R.id.instance_count_badge);
        searchInput = findViewById(R.id.search_input);

        int spanCount = calculateSpanCount();
        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        recyclerView.setLayoutManager(layoutManager);

        int spacing = (int) (10 * getResources().getDisplayMetrics().density);
        recyclerView.addItemDecoration(new GridSpacingDecoration(spanCount, spacing));

        loadVersions();

        GameVersion selectedVersion = versionManager.getSelectedVersion();
        adapter = new InstanceCardAdapter(allVersions, selectedVersion);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(version -> {
            versionManager.selectVersion(version);
            adapter.setSelectedVersion(version);
            adapter.notifyDataSetChanged();
        });

        setupFilterTabs();
        setupSearch();
        setupImportButton();
        updateCount();

        DynamicAnim.applyPressScaleRecursively(findViewById(android.R.id.content));
        recyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(recyclerView));
    }

    private int calculateSpanCount() {
        float displayWidth = getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density;
        float cardMinWidth = 240f;
        float padding = 40f;
        int count = (int) ((displayWidth - padding) / cardMinWidth);
        return Math.max(2, count);
    }

    private void loadVersions() {
        allVersions.clear();
        List<GameVersion> installed = versionManager.getInstalledVersions();
        List<GameVersion> custom = versionManager.getCustomVersions();
        if (installed != null) allVersions.addAll(installed);
        if (custom != null) allVersions.addAll(custom);

        Collections.sort(allVersions, (a, b) -> {
            String va = a.versionCode != null ? a.versionCode : "";
            String vb = b.versionCode != null ? b.versionCode : "";
            return vb.compareTo(va);
        });
    }

    private void setupFilterTabs() {
        updateFilterUI();

        filterAll.setOnClickListener(v -> {
            currentFilter = FILTER_ALL;
            updateFilterUI();
            applyFilters();
        });
        filterCustom.setOnClickListener(v -> {
            currentFilter = FILTER_CUSTOM;
            updateFilterUI();
            applyFilters();
        });
    }

    private void updateFilterUI() {
        TextView[] tabs = {filterAll, filterCustom};
        for (int i = 0; i < tabs.length; i++) {
            boolean selected = (i == currentFilter);
            tabs[i].setSelected(selected);
            if (selected) {
                tabs[i].setTextColor(getResources().getColor(R.color.on_primary, getTheme()));
                tabs[i].setTypeface(tabs[i].getTypeface(), android.graphics.Typeface.BOLD);
            } else {
                tabs[i].setTextColor(getResources().getColor(R.color.on_surface, getTheme()));
                tabs[i].setTypeface(tabs[i].getTypeface(), android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupImportButton() {
        TextView btnImport = findViewById(R.id.btn_import_apk);
        if (btnImport == null) return;
        btnImport.setSelected(true);
        btnImport.setOnClickListener(v -> startApkFilePicker());
    }

    private void startApkFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"application/vnd.android.package-archive", "application/octet-stream", "application/zip"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        apkImportResultLauncher.launch(intent);
    }

    private void applyFilters() {
        String query = searchInput.getText().toString().trim().toLowerCase();
        List<GameVersion> filtered = new ArrayList<>();

        for (GameVersion v : allVersions) {
            if (currentFilter == FILTER_CUSTOM && v.isInstalled) continue;

            if (!query.isEmpty()) {
                String name = v.displayName != null ? v.displayName.toLowerCase() : "";
                String code = v.versionCode != null ? v.versionCode.toLowerCase() : "";
                String dir = v.directoryName != null ? v.directoryName.toLowerCase() : "";
                if (!name.contains(query) && !code.contains(query) && !dir.contains(query)) {
                    continue;
                }
            }

            filtered.add(v);
        }

        adapter.updateData(filtered);
        updateCount();
    }

    private void updateCount() {
        if (instanceCountBadge != null && adapter != null) {
            instanceCountBadge.setText(String.valueOf(adapter.getItemCount()));
        }
    }

    private void setupNavBar() {
        setActiveNavTab(R.id.nav_tab_instances);
        findViewById(R.id.nav_tab_instances).setOnClickListener(v -> {});
    }

    @Override
    protected void onResume() {
        super.onResume();
        versionManager.loadAllVersions();
        loadVersions();
        applyFilters();
    }

    private static class GridSpacingDecoration extends RecyclerView.ItemDecoration {
        private final int spanCount;
        private final int spacing;

        GridSpacingDecoration(int spanCount, int spacing) {
            this.spanCount = spanCount;
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;
            outRect.left = spacing - column * spacing / spanCount;
            outRect.right = (column + 1) * spacing / spanCount;
            if (position >= spanCount) {
                outRect.top = spacing;
            }
        }
    }

    private static class InstanceCardAdapter extends RecyclerView.Adapter<InstanceCardAdapter.VH> {
        private List<GameVersion> versions;
        private GameVersion selectedVersion;
        private OnItemClickListener listener;

        interface OnItemClickListener {
            void onClick(GameVersion version);
        }

        void setOnItemClickListener(OnItemClickListener l) {
            this.listener = l;
        }

        InstanceCardAdapter(List<GameVersion> versions, GameVersion selected) {
            this.versions = new ArrayList<>(versions);
            this.selectedVersion = selected;
        }

        void setSelectedVersion(GameVersion v) {
            this.selectedVersion = v;
        }

        void updateData(List<GameVersion> newVersions) {
            this.versions = new ArrayList<>(newVersions);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_instance_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            GameVersion v = versions.get(position);
            boolean isSelected = selectedVersion != null
                    && selectedVersion.directoryName != null
                    && selectedVersion.directoryName.equals(v.directoryName);

            holder.itemView.setActivated(isSelected);

            holder.versionCode.setText(v.versionCode != null ? v.versionCode : v.directoryName);

            if (v.isInstalled) {
                holder.typeTag.setText(R.string.tag_installed);
                holder.typeTag.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.primary, holder.itemView.getContext().getTheme()));
                holder.typeTag.setBackgroundResource(R.drawable.bg_release_tag);
            } else {
                holder.typeTag.setText(R.string.tag_custom);
                holder.typeTag.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_secondary, holder.itemView.getContext().getTheme()));
                holder.typeTag.setBackgroundResource(R.drawable.bg_preview_tag);
            }
            holder.typeTag.setVisibility(View.VISIBLE);

            String displayLabel;
            if (v.displayName != null && !v.displayName.isEmpty()) {
                displayLabel = v.displayName;
            } else {
                displayLabel = holder.itemView.getContext().getString(R.string.vanilla_prefix, v.versionCode != null ? v.versionCode : "");
            }
            holder.displayName.setText(displayLabel);

            holder.settingsIcon.setOnClickListener(iv -> {});

            holder.itemView.setOnClickListener(iv -> {
                if (listener != null) listener.onClick(v);
            });

            DynamicAnim.applyPressScale(holder.itemView);
        }

        @Override
        public int getItemCount() {
            return versions.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView versionCode, typeTag, displayName;
            ImageView settingsIcon;

            VH(View v) {
                super(v);
                versionCode = v.findViewById(R.id.card_version_code);
                typeTag = v.findViewById(R.id.card_type_tag);
                displayName = v.findViewById(R.id.card_display_name);
                settingsIcon = v.findViewById(R.id.card_settings_icon);
            }
        }
    }
}

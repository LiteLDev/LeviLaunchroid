package org.levimc.launcher.ui.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.levimc.launcher.R;
import org.levimc.launcher.core.curseforge.CurseForgeClient;
import org.levimc.launcher.core.curseforge.models.Content;
import org.levimc.launcher.core.curseforge.models.ContentSearchResponse;
import org.levimc.launcher.ui.adapter.CurseForgeContentAdapter;
import org.levimc.launcher.util.PersonalizationManager;
import org.levimc.launcher.util.UIHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CurseForgeActivity extends BaseActivity {

    private static final int PAGE_SIZE = 20;

    private EditText searchBox;
    private RadioGroup categoryGroup;
    private MaterialButton btnSort;
    private RecyclerView recyclerView;
    private ProgressBar loadingProgress;
    private TextView emptyMessage;
    private CurseForgeContentAdapter adapter;
    private CurseForgeClient client;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int currentPage = 1;
    private int totalPages = 1;
    private int requestSequence;
    private SortOption currentSort;

    private static class Category {
        final String name;
        final int id;

        Category(String name, int id) {
            this.name = name;
            this.id = id;
        }
    }

    private static class SortOption {
        final String name;
        final String field;
        final String order;

        SortOption(String name, String field, String order) {
            this.name = name;
            this.field = field;
            this.order = order;
        }
    }

    private final List<Category> categories = new ArrayList<>();
    private final List<SortOption> sortOptions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_curseforge);
        client = CurseForgeClient.getInstance();
        setupData();
        initViews();
        loadContent();
    }

    private void setupData() {
        categories.add(new Category("All", 0));
        categories.add(new Category("Addons", 4984));
        categories.add(new Category("Maps", 6913));
        categories.add(new Category("Skins", 6925));
        categories.add(new Category("Textures", 6929));
        categories.add(new Category("Scripts", 6940));

        sortOptions.add(new SortOption("Popular", CurseForgeClient.SORT_POPULARITY, "desc"));
        sortOptions.add(new SortOption("Downloads", CurseForgeClient.SORT_TOTAL_DOWNLOADS, "desc"));
        sortOptions.add(new SortOption("Recently updated", CurseForgeClient.SORT_LAST_UPDATED, "desc"));
        sortOptions.add(new SortOption("Name", CurseForgeClient.SORT_NAME, "asc"));
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    private void initViews() {
        searchBox = findViewById(R.id.search_box);
        categoryGroup = findViewById(R.id.category_group);
        btnSort = findViewById(R.id.btn_sort);
        recyclerView = findViewById(R.id.mods_recycler);
        loadingProgress = findViewById(R.id.loading_progress);
        emptyMessage = findViewById(R.id.empty_message);

        adapter = new CurseForgeContentAdapter(this::onContentClick, new CurseForgeContentAdapter.OnPageChangeListener() {
            @Override
            public void onNextPage() {
                if (currentPage < totalPages) {
                    currentPage++;
                    loadContent();
                }
            }

            @Override
            public void onPrevPage() {
                if (currentPage > 1) {
                    currentPage--;
                    loadContent();
                }
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(null);
        recyclerView.setAdapter(adapter);

        searchBox.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentPage = 1;
                loadContent();
                UIHelper.hideKeyboard(this);
                searchBox.clearFocus();
                return true;
            }
            return false;
        });

        PersonalizationManager personalization = new PersonalizationManager(this);
        int accentColor = personalization.hasCustomAccent()
                ? personalization.getAccentColor()
                : ContextCompat.getColor(this, R.color.primary);
        int onSurface = ContextCompat.getColor(this, R.color.on_surface);
        int surfaceVariant = ContextCompat.getColor(this, R.color.surface_variant);
        int textOnAccent = Color.luminance(accentColor) > 0.42f ? Color.BLACK : Color.WHITE;

        ColorStateList chipText = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{textOnAccent, onSurface});

        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            RadioButton chip = new RadioButton(this);
            chip.setId(View.generateViewId());
            chip.setTag(category);
            chip.setText(category.name);
            chip.setTextColor(chipText);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            chip.setAllCaps(false);
            chip.setButtonDrawable(android.R.color.transparent);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setMinHeight(dp(32));
            chip.setMinimumWidth(0);
            chip.setPadding(dp(12), dp(6), dp(12), dp(6));

            GradientDrawable checked = new GradientDrawable();
            checked.setCornerRadius(dp(12));
            checked.setColor(accentColor);
            GradientDrawable unchecked = new GradientDrawable();
            unchecked.setCornerRadius(dp(12));
            unchecked.setColor(surfaceVariant);
            StateListDrawable state = new StateListDrawable();
            state.addState(new int[]{android.R.attr.state_checked}, checked);
            state.addState(new int[]{}, unchecked);
            chip.setBackground(state);

            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
            params.setMarginEnd(dp(7));
            categoryGroup.addView(chip, params);
            if (i == 0) chip.setChecked(true);
        }

        categoryGroup.setOnCheckedChangeListener((group, checkedId) -> {
            currentPage = 1;
            loadContent();
        });

        currentSort = sortOptions.get(0);
        btnSort.setText(currentSort.name);
        btnSort.setOnClickListener(view -> {
            PopupMenu menu = new PopupMenu(this, view);
            for (int i = 0; i < sortOptions.size(); i++) {
                menu.getMenu().add(0, i, i, sortOptions.get(i).name);
            }
            menu.setOnMenuItemClickListener(item -> {
                currentSort = sortOptions.get(item.getItemId());
                btnSort.setText(currentSort.name);
                currentPage = 1;
                loadContent();
                return true;
            });
            menu.show();
        });
    }

    private void loadContent() {
        String query = searchBox.getText().toString().trim();
        int checkedId = categoryGroup.getCheckedRadioButtonId();
        Category category = null;
        if (checkedId != -1) {
            View selected = categoryGroup.findViewById(checkedId);
            if (selected != null) category = (Category) selected.getTag();
        }

        SortOption sort = currentSort;
        int request = ++requestSequence;
        int requestedPage = currentPage;
        loadingProgress.setVisibility(View.VISIBLE);
        emptyMessage.setVisibility(View.GONE);
        recyclerView.setVisibility(adapter.getItemCount() > 0 ? View.VISIBLE : View.INVISIBLE);

        client.searchContent(query, category != null ? category.id : 0, "",
                (requestedPage - 1) * PAGE_SIZE, PAGE_SIZE,
                sort != null ? sort.field : CurseForgeClient.SORT_POPULARITY,
                sort != null ? sort.order : "desc",
                new CurseForgeClient.CurseForgeCallback<ContentSearchResponse>() {
                    @Override
                    public void onSuccess(ContentSearchResponse result) {
                        handler.post(() -> {
                            if (isFinishing() || isDestroyed() || request != requestSequence) return;
                            loadingProgress.setVisibility(View.GONE);
                            if (result != null && result.data != null) {
                                if (result.pagination != null && result.pagination.totalCount > 0) {
                                    totalPages = Math.max(1, (int) Math.ceil((double) result.pagination.totalCount / PAGE_SIZE));
                                } else {
                                    totalPages = result.data.size() < PAGE_SIZE ? requestedPage : requestedPage + 1;
                                }
                                adapter.setContents(result.data, requestedPage, totalPages);
                                boolean empty = result.data.isEmpty();
                                emptyMessage.setText(R.string.no_mods_found);
                                emptyMessage.setVisibility(empty ? View.VISIBLE : View.GONE);
                                recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                                if (!empty) recyclerView.scrollToPosition(0);
                            } else {
                                adapter.setContents(Collections.emptyList(), 1, 1);
                                recyclerView.setVisibility(View.GONE);
                                emptyMessage.setVisibility(View.VISIBLE);
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable error) {
                        handler.post(() -> {
                            if (isFinishing() || isDestroyed() || request != requestSequence) return;
                            loadingProgress.setVisibility(View.GONE);
                            if (adapter.getItemCount() == 0) {
                                recyclerView.setVisibility(View.GONE);
                                emptyMessage.setText(getString(R.string.error_message_format, error.getMessage()));
                                emptyMessage.setVisibility(View.VISIBLE);
                            } else {
                                recyclerView.setVisibility(View.VISIBLE);
                                android.widget.Toast.makeText(CurseForgeActivity.this,
                                        getString(R.string.error_message_format, error.getMessage()),
                                        android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
    }

    private void onContentClick(Content content) {
        Intent intent = new Intent(this, ContentDetailsActivity.class);
        intent.putExtra(ContentDetailsActivity.EXTRA_CONTENT, content);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        requestSequence++;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}

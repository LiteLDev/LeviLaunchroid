package org.levimc.launcher.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.PopupMenu;
import android.view.ViewGroup;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import androidx.core.content.ContextCompat;
import org.levimc.launcher.util.PersonalizationManager;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.curseforge.CurseForgeClient;
import org.levimc.launcher.core.curseforge.models.Content;
import org.levimc.launcher.core.curseforge.models.ContentSearchResponse;
import org.levimc.launcher.ui.adapter.CurseForgeContentAdapter;
import org.levimc.launcher.util.UIHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CurseForgeActivity extends BaseActivity {

    private EditText searchBox;
    private RadioGroup categoryGroup;
    private com.google.android.material.button.MaterialButton btnSort;
    private SortOption currentSort;
    private RecyclerView recyclerView;
    private ProgressBar loadingProgress;
    private CurseForgeContentAdapter adapter;
    private CurseForgeClient client;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int currentPage = 1;
    private int totalPages = 1;
    private static final int PAGE_SIZE = 20;


    private static class Category {
        String name;
        int id;
        Category(String name, int id) { this.name = name; this.id = id; }
        @Override public String toString() { return name; }
    }
    
    private final List<Category> categories = new ArrayList<>();

    private static class SortOption {
        String name;
        String field;
        String order;
        SortOption(String name, String field, String order) { 
            this.name = name; 
            this.field = field; 
            this.order = order;
        }
        @Override public String toString() { return name; }
    }
    
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
        categories.add(new Category("All Categories", 0));
        categories.add(new Category("Addons", 4984));
        categories.add(new Category("Maps", 6913));
        categories.add(new Category("Skins", 6925));
        categories.add(new Category("Texture Packs", 6929));
        categories.add(new Category("Scripts", 6940));

        sortOptions.add(new SortOption("Relevancy", CurseForgeClient.SORT_POPULARITY, "desc"));
        sortOptions.add(new SortOption("Total Downloads", CurseForgeClient.SORT_TOTAL_DOWNLOADS, "desc"));
        sortOptions.add(new SortOption("Last Updated", CurseForgeClient.SORT_LAST_UPDATED, "desc"));
        sortOptions.add(new SortOption("Name", CurseForgeClient.SORT_NAME, "asc"));
    }

    private void initViews() {
        searchBox = findViewById(R.id.search_box);
        categoryGroup = findViewById(R.id.category_group);
        btnSort = findViewById(R.id.btn_sort);
        recyclerView = findViewById(R.id.mods_recycler);
        loadingProgress = findViewById(R.id.loading_progress);

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
        recyclerView.setAdapter(adapter);


        searchBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentPage = 1;
                loadContent();
                UIHelper.hideKeyboard(this);
                return true;
            }

            return false;
        });

        PersonalizationManager pm = new PersonalizationManager(this);
        int accentColor = pm.hasCustomAccent() ? pm.getAccentColor() : ContextCompat.getColor(this, R.color.primary);
        int onSurface = ContextCompat.getColor(this, R.color.on_surface);

        ColorStateList textColors = new ColorStateList(
            new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{android.R.attr.state_selected},
                new int[]{}
            },
            new int[]{
                Color.WHITE,
                Color.WHITE,
                onSurface
            }
        );

        float radiusPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, getResources().getDisplayMetrics());

        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            RadioButton rb = new RadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(cat.name);
            rb.setTextColor(textColors);
            rb.setTextSize(14f);
            rb.setButtonDrawable(android.R.color.transparent);
            
            GradientDrawable checkedBg = new GradientDrawable();
            checkedBg.setShape(GradientDrawable.RECTANGLE);
            checkedBg.setCornerRadius(radiusPx);
            checkedBg.setColor(accentColor);
            
            GradientDrawable uncheckedBg = new GradientDrawable();
            uncheckedBg.setShape(GradientDrawable.RECTANGLE);
            uncheckedBg.setCornerRadius(radiusPx);
            uncheckedBg.setColor(Color.parseColor("#1A888888"));
            
            StateListDrawable sld = new StateListDrawable();
            sld.addState(new int[]{android.R.attr.state_checked}, checkedBg);
            sld.addState(new int[]{android.R.attr.state_selected}, checkedBg);
            sld.addState(new int[]{}, uncheckedBg);
            
            rb.setBackground(sld);
            rb.setPadding(40, 20, 40, 20);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 16, 0);
            categoryGroup.addView(rb, params);
            if (i == 0) rb.setChecked(true);
            rb.setTag(cat);
        }

        categoryGroup.setOnCheckedChangeListener((group, checkedId) -> {
            currentPage = 1;
            loadContent();
        });

        currentSort = sortOptions.get(0);
        btnSort.setText("Sort: " + currentSort.name);
        btnSort.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            for (int i = 0; i < sortOptions.size(); i++) {
                popup.getMenu().add(0, i, i, sortOptions.get(i).name);
            }
            popup.setOnMenuItemClickListener(item -> {
                currentSort = sortOptions.get(item.getItemId());
                btnSort.setText("Sort: " + currentSort.name);
                currentPage = 1;
                loadContent();
                return true;
            });
            popup.show();
        });
    }
    
    private void loadContent() {
        String query = searchBox.getText().toString();
        
        Category category = null;
        int checkedId = categoryGroup.getCheckedRadioButtonId();
        if (checkedId != -1) {
            category = (Category) findViewById(checkedId).getTag();
        }
        
        SortOption sort = currentSort;
        
        loadingProgress.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        int index = (currentPage - 1) * PAGE_SIZE;

        client.searchContent(query, category != null ? category.id : 0, "", index, PAGE_SIZE, sort != null ? sort.field : CurseForgeClient.SORT_POPULARITY, sort != null ? sort.order : "desc", new CurseForgeClient.CurseForgeCallback<ContentSearchResponse>() {
            @Override
            public void onSuccess(ContentSearchResponse result) {
                handler.post(() -> {
                    loadingProgress.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    if (result != null && result.data != null) {

                        if (result.pagination != null && result.pagination.totalCount > 0) {
                             totalPages = (int) Math.ceil((double) result.pagination.totalCount / PAGE_SIZE);
                        } else {
                            if (result.data.size() < PAGE_SIZE) {
                                totalPages = currentPage;
                            } else {
                                totalPages = currentPage + 1;
                            }
                        }
                        
                        adapter.setContents(result.data, currentPage, totalPages);
                        
                        if (result.data.isEmpty()) {
                            Toast.makeText(CurseForgeActivity.this, R.string.no_mods_found, Toast.LENGTH_SHORT).show();
                        }
                        recyclerView.scrollToPosition(0);
                    } else {
                        adapter.setContents(Collections.emptyList(), 1, 1);
                    }

                });
            }


            @Override
            public void onError(Throwable t) {
                handler.post(() -> {
                    loadingProgress.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    Toast.makeText(CurseForgeActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void onContentClick(Content content) {
        Intent intent = new Intent(this, ContentDetailsActivity.class);
        intent.putExtra(ContentDetailsActivity.EXTRA_CONTENT, content);
        startActivity(intent);
    }
}

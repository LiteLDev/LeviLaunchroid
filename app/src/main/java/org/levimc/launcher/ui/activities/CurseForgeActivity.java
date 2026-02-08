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
import android.widget.Toast;

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
    private Spinner spinnerCategory;
    private Spinner spinnerSort;
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
        spinnerCategory = findViewById(R.id.spinner_category);
        spinnerSort = findViewById(R.id.spinner_sort);
        recyclerView = findViewById(R.id.mods_recycler);
        loadingProgress = findViewById(R.id.loading_progress);
        View backButton = findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> finish());

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

        ArrayAdapter<Category> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
        
        ArrayAdapter<SortOption> sortAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sortOptions);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);
        
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentPage = 1;
                loadContent();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        
        spinnerCategory.setOnItemSelectedListener(listener);
        spinnerSort.setOnItemSelectedListener(listener);
    }
    
    private void loadContent() {
        String query = searchBox.getText().toString();
        Category category = (Category) spinnerCategory.getSelectedItem();
        SortOption sort = (SortOption) spinnerSort.getSelectedItem();
        
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

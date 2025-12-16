package org.levimc.launcher.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.model.InbuiltMod;
import org.levimc.launcher.ui.adapter.InbuiltModsAdapter;
import org.levimc.launcher.ui.animation.DynamicAnim;

import java.util.List;

public class InbuiltModsActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private InbuiltModsAdapter adapter;
    private InbuiltModManager modManager;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbuilt_mods);

        View root = findViewById(android.R.id.content);
        if (root != null) {
            DynamicAnim.applyPressScaleRecursively(root);
        }

        modManager = InbuiltModManager.getInstance(this);
        setupViews();
        loadMods();
    }

    private void setupViews() {
        ImageButton closeButton = findViewById(R.id.close_inbuilt_button);
        closeButton.setOnClickListener(v -> finish());
        DynamicAnim.applyPressScale(closeButton);

        recyclerView = findViewById(R.id.inbuilt_mods_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        
        emptyText = findViewById(R.id.empty_inbuilt_text);

        adapter = new InbuiltModsAdapter();
        adapter.setOnAddClickListener(mod -> {
            modManager.addMod(mod.getId());
            Toast.makeText(this, getString(R.string.inbuilt_mod_added, mod.getName()), Toast.LENGTH_SHORT).show();
            loadMods();
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadMods() {
        List<InbuiltMod> mods = modManager.getAvailableMods(this);
        adapter.updateMods(mods);
        emptyText.setVisibility(mods.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(mods.isEmpty() ? View.GONE : View.VISIBLE);
        recyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(recyclerView));
    }
}

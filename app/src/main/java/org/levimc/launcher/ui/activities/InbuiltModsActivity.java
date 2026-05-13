package org.levimc.launcher.ui.activities;

import android.os.Bundle;
import android.view.View;

import android.widget.Switch;
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
    private Switch modMenuToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbuilt_mods);

        modManager = InbuiltModManager.getInstance(this);
        setupViews();
        
        View root = findViewById(android.R.id.content);
        if (root != null) {
            DynamicAnim.applyPressScaleRecursively(root);
        }
        
        loadMods();
    }

    private void setupViews() {
        recyclerView = findViewById(R.id.inbuilt_mods_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        emptyText = findViewById(R.id.empty_inbuilt_text);
        modMenuToggle = findViewById(R.id.mod_menu_toggle);
        modMenuToggle.setChecked(modManager.isModMenuEnabled());
        
        modMenuToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            modManager.setModMenuEnabled(isChecked);
            loadMods();
            Toast.makeText(this, 
                isChecked ? R.string.mod_menu_enabled : R.string.mod_menu_disabled, 
                Toast.LENGTH_SHORT).show();
        });

        adapter = new InbuiltModsAdapter();
        adapter.setOnAddClickListener(mod -> {
            modManager.addMod(mod.getId());
            Toast.makeText(this, getString(R.string.inbuilt_mod_added, mod.getName()), Toast.LENGTH_SHORT).show();
            loadMods();
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadMods() {
        boolean modMenuEnabled = modManager.isModMenuEnabled();
        List<InbuiltMod> mods = modMenuEnabled ? modManager.getAllMods(this) : modManager.getAvailableMods(this);
        adapter.setModMenuEnabled(modMenuEnabled);
        adapter.updateMods(mods);
        
        boolean isEmpty = mods.isEmpty();
        
        emptyText.setVisibility(isEmpty && !modMenuEnabled ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        
        if (!isEmpty) {
            recyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(recyclerView));
        }
    }

}

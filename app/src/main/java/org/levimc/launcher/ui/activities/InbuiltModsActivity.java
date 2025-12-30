package org.levimc.launcher.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
    private View modMenuOverlay;

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
        
        // Remove press animation from mod menu overlay and its children
        if (modMenuOverlay != null) {
            modMenuOverlay.setOnTouchListener(null);
            clearPressAnimationRecursively(modMenuOverlay);
        }
        
        loadMods();
    }

    private void setupViews() {
        ImageButton closeButton = findViewById(R.id.close_inbuilt_button);
        closeButton.setOnClickListener(v -> finish());
        DynamicAnim.applyPressScale(closeButton);

        recyclerView = findViewById(R.id.inbuilt_mods_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        
        emptyText = findViewById(R.id.empty_inbuilt_text);
        modMenuOverlay = findViewById(R.id.mod_menu_overlay);

        // Mod Menu Toggle
        modMenuToggle = findViewById(R.id.mod_menu_toggle);
        modMenuToggle.setChecked(modManager.isModMenuEnabled());
        updateModMenuOverlay(modManager.isModMenuEnabled());
        
        modMenuToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            modManager.setModMenuEnabled(isChecked);
            updateModMenuOverlay(isChecked);
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

    private void updateModMenuOverlay(boolean modMenuEnabled) {
        if (modMenuOverlay != null) {
            modMenuOverlay.setVisibility(modMenuEnabled ? View.VISIBLE : View.GONE);
        }
    }

    private void loadMods() {
        List<InbuiltMod> mods = modManager.getAvailableMods(this);
        adapter.updateMods(mods);
        
        boolean isEmpty = mods.isEmpty();
        boolean modMenuEnabled = modManager.isModMenuEnabled();
        
        emptyText.setVisibility(isEmpty && !modMenuEnabled ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        
        if (!isEmpty) {
            recyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(recyclerView));
        }
    }
    
    private void clearPressAnimationRecursively(View view) {
        view.setOnTouchListener(null);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                clearPressAnimationRecursively(group.getChildAt(i));
            }
        }
    }
}

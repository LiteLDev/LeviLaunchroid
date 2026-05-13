package org.levimc.launcher.ui.activities;

import android.os.Bundle;
import android.widget.Button;

import androidx.core.view.ViewCompat;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.Mod;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;
import org.levimc.launcher.ui.views.MainViewModel;
import org.levimc.launcher.ui.views.MainViewModelFactory;
import org.levimc.launcher.ui.animation.DynamicAnim;

public class ModDetailActivity extends BaseActivity {

    private MainViewModel viewModel;
    private Mod currentMod;
    private int modPosition;
    private String modFilenameArg;
    private TextView modNameText;
    private TextView modFilenameText;
    private TextView modOrderText;
    private Switch modSwitch;
    private View headerContainer;
    private View infoContainer;
    private View actionsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mod_detail);

        View root = findViewById(R.id.mod_detail_root);
        if (root != null) {
            DynamicAnim.applyPressScaleRecursively(root);
        }

        if (getIntent().hasExtra("mod_filename") && getIntent().hasExtra("mod_position")) {
            modFilenameArg = getIntent().getStringExtra("mod_filename");
            modPosition = getIntent().getIntExtra("mod_position", -1);
            
            setupViewModel();
            setupViews();
            runEnterAnimations();
            
            loadModDetails(modFilenameArg);
        } else {
            Toast.makeText(this, R.string.error_loading_mod, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this, new MainViewModelFactory(getApplication())).get(MainViewModel.class);
    }

    private void setupViews() {
        modNameText = findViewById(R.id.mod_name_detail);
        modFilenameText = findViewById(R.id.mod_filename_detail);
        modOrderText = findViewById(R.id.mod_order_detail);
        modSwitch = findViewById(R.id.mod_switch_detail);
        headerContainer = findViewById(R.id.mod_detail_title);
        infoContainer = findViewById(R.id.mod_detail_info_container);
        actionsContainer = findViewById(R.id.mod_detail_actions_container);

        if (modFilenameArg != null && headerContainer != null) {
            ViewCompat.setTransitionName(headerContainer, "mod_card_" + modFilenameArg);
        }

        Button deleteButton = findViewById(R.id.delete_mod_button);
        deleteButton.setOnClickListener(v -> confirmDeleteMod());
        DynamicAnim.applyPressScale(deleteButton);

        modSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentMod != null && isChecked != currentMod.isEnabled()) {
                currentMod.setEnabled(isChecked);
                viewModel.setModEnabled(currentMod.getId(), isChecked);
                Toast.makeText(this, isChecked ? R.string.mod_enabled : R.string.mod_disabled, Toast.LENGTH_SHORT).show();
            }
        });
        DynamicAnim.applyPressScale(modSwitch);
    }

    private void loadModDetails(String modFilename) {
        if (viewModel != null) {
            viewModel.getModsLiveData().observe(this, mods -> {
                if (mods != null) {
                    for (Mod mod : mods) {
                        if (mod.getId().equals(modFilename)) {
                            currentMod = mod;
                            updateModUI(mod);
                            break;
                        }
                    }
                }
            });
            
            viewModel.refreshMods();
        }
    }

    private void updateModUI(Mod mod) {
        if (mod != null) {
            modNameText.setText(mod.getDisplayName());
            modFilenameText.setText(getString(R.string.mod_filename_format, mod.getFileName()));
            modOrderText.setText(getString(R.string.mod_load_order, modPosition + 1));
            modSwitch.setChecked(mod.isEnabled());
        }
    }

    private void runEnterAnimations() {
        float density = getResources().getDisplayMetrics().density;
        float dy = 16f * density;

        View[] cards = new View[]{headerContainer, infoContainer, actionsContainer};
        for (int i = 0; i < cards.length; i++) {
            View card = cards[i];
            if (card == null) continue;
            card.setAlpha(0f);
            card.setTranslationY(dy);
            final int delay = 100 + i * 80;
            card.postDelayed(() -> {
                DynamicAnim.springAlphaTo(card, 1f).start();
                DynamicAnim.springTranslationYTo(card, 0f).start();
            }, delay);
        }

        // 不对 mod 名称做入场动画，保持静态
    }

    private void confirmDeleteMod() {
        if (currentMod != null) {
            new CustomAlertDialog(this)
                    .setTitleText(getString(R.string.dialog_title_delete_mod))
                    .setMessage(getString(R.string.dialog_message_delete_mod))
                    .setPositiveButton(getString(R.string.dialog_positive_delete), v -> {
                        viewModel.removeMod(currentMod);
                        Toast.makeText(this, R.string.delete_mod, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton(getString(R.string.dialog_negative_cancel), null)
                    .show();
        }
    }

    // Exit animation is now handled uniformly in BaseActivity.finish()
}

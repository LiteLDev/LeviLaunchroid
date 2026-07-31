package org.levimc.launcher.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.ImageView;

import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.Mod;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;
import org.levimc.launcher.ui.views.MainViewModel;
import org.levimc.launcher.ui.views.MainViewModelFactory;
import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.util.PersonalizationManager;

import java.io.File;

public class ModDetailActivity extends BaseActivity {

    private MainViewModel viewModel;
    private Mod currentMod;
    private int modPosition;
    private String modFilenameArg;
    private TextView modNameText;
    private TextView modAuthorText;
    private TextView modVersionText;
    private TextView modDescriptionText;
    private TextView modStatusText;
    private ImageView modIconView;
    private View iconContainer;
    private MetaRow versionRow;
    private MetaRow minecraftVersionsRow;
    private MetaRow configFilesRow;
    private Switch modSwitch;
    private Button configureButton;
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
        modAuthorText = findViewById(R.id.mod_author_detail);
        modVersionText = findViewById(R.id.mod_version_detail);
        modDescriptionText = findViewById(R.id.mod_description_detail);
        modStatusText = findViewById(R.id.mod_status_text);
        modIconView = findViewById(R.id.mod_icon_detail);
        iconContainer = findViewById(R.id.mod_icon_container);
        versionRow = MetaRow.bind(findViewById(R.id.mod_version_row));
        minecraftVersionsRow = MetaRow.bind(findViewById(R.id.mod_minecraft_versions_row));
        configFilesRow = MetaRow.bind(findViewById(R.id.mod_config_files_row));
        modSwitch = findViewById(R.id.mod_switch_detail);
        headerContainer = findViewById(R.id.mod_detail_title);
        View heroContainer = findViewById(R.id.mod_detail_header_container);
        infoContainer = findViewById(R.id.mod_detail_info_container);
        actionsContainer = findViewById(R.id.mod_detail_actions_container);


        applyPersonalization(heroContainer);

        Button deleteButton = findViewById(R.id.delete_mod_button);
        deleteButton.setOnClickListener(v -> confirmDeleteMod());
        DynamicAnim.applyPressScale(deleteButton);

        configureButton = findViewById(R.id.configure_mod_button);
        configureButton.setOnClickListener(v -> openModConfig());
        DynamicAnim.applyPressScale(configureButton);

        modSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentMod != null && isChecked != currentMod.isEnabled()) {
                currentMod.setEnabled(isChecked);
                viewModel.setModEnabled(currentMod.getId(), isChecked);
                if (modStatusText != null) {
                    modStatusText.setText(isChecked ? R.string.mod_status_enabled : R.string.mod_status_disabled);
                }
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
            String author = fallbackText(mod.getAuthor(), getString(R.string.mod_unknown_author));
            String version = fallbackText(mod.getVersion(), getString(R.string.mod_unknown_version));

            modNameText.setText(mod.getDisplayName());
            modAuthorText.setText(getString(R.string.mod_author_byline, author));
            modVersionText.setText(getString(R.string.mod_version_chip, version));
            modStatusText.setText(mod.isEnabled() ? R.string.mod_status_enabled : R.string.mod_status_disabled);
            modSwitch.setChecked(mod.isEnabled());

            String description = trimToNull(mod.getDescription());
            if (description == null) {
                modDescriptionText.setVisibility(View.GONE);
            } else {
                modDescriptionText.setVisibility(View.VISIBLE);
                modDescriptionText.setText(description);
            }

            versionRow.set(R.string.mod_version_label, version);
            minecraftVersionsRow.set(R.string.mod_minecraft_versions_label, formatMinecraftVersions(mod));
            configFilesRow.set(R.string.mod_config_files_label, formatConfigFiles(mod));
            loadModIcon(mod);

            if (configureButton != null) {
                configureButton.setVisibility(mod.hasEditableConfig() ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void applyPersonalization(@Nullable View heroContainer) {
        PersonalizationManager personalizationManager = new PersonalizationManager(this);
        View root = findViewById(android.R.id.content);
        if (root != null) {
            personalizationManager.applyAccentToView(root, this);
        }
        View[] themedSurfaces = new View[]{heroContainer, infoContainer, actionsContainer, iconContainer};
        for (View surface : themedSurfaces) {
            if (surface != null) {
                personalizationManager.applyGlassToView(surface);
            }
        }
    }

    private void loadModIcon(Mod mod) {
        if (modIconView == null) {
            return;
        }

        String iconPath = trimToNull(mod.getIconPath());
        if (iconPath == null) {
            modIconView.setImageResource(R.drawable.ic_plugin);
            return;
        }

        File iconFile = new File(iconPath);
        if (!iconFile.isFile()) {
            modIconView.setImageResource(R.drawable.ic_plugin);
            return;
        }

        int radiusPx = Math.round(12f * getResources().getDisplayMetrics().density);
        Glide.with(this)
                .load(iconFile)
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(radiusPx)))
                .placeholder(R.drawable.ic_plugin)
                .error(R.drawable.ic_plugin)
                .into(modIconView);
    }

    private String formatMinecraftVersions(Mod mod) {
        if (mod.getMinecraftVersions().isEmpty()) {
            return getString(R.string.mod_all_versions);
        }
        return String.join(", ", mod.getMinecraftVersions());
    }

    private String formatConfigFiles(Mod mod) {
        if (!mod.hasEditableConfig() || mod.getConfigFileCount() <= 0) {
            return getString(R.string.mod_no_config_files);
        }
        return getResources().getQuantityString(
                R.plurals.mod_config_files_count,
                mod.getConfigFileCount(),
                mod.getConfigFileCount()
        );
    }

    private String fallbackText(String value, String fallback) {
        String text = trimToNull(value);
        return text == null ? fallback : text;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void openModConfig() {
        if (currentMod == null || !currentMod.hasEditableConfig()) {
            return;
        }
        Intent intent = new Intent(this, ModConfigActivity.class);
        intent.putExtra(ModConfigActivity.EXTRA_MOD_NAME, currentMod.getDisplayName());
        intent.putExtra(ModConfigActivity.EXTRA_MOD_ROOT, currentMod.getModRootPath());
        startActivity(intent);
    }

    private void runEnterAnimations() {
        float density = getResources().getDisplayMetrics().density;
        float dy = 16f * density;
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

        View heroContainer = findViewById(R.id.mod_detail_header_container);
        View[] cards = new View[]{headerContainer, heroContainer, infoContainer, actionsContainer};
        for (int i = 0; i < cards.length; i++) {
            View card = cards[i];
            if (card == null) continue;
            card.setAlpha(0f);
            card.setTranslationY(dy);
            final int delay = 100 + i * 80;
            handler.postDelayed(() -> {
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

    private static final class MetaRow {
        private final TextView labelView;
        private final TextView valueView;

        private MetaRow(TextView labelView, TextView valueView) {
            this.labelView = labelView;
            this.valueView = valueView;
        }

        static MetaRow bind(View rowView) {
            return new MetaRow(
                    rowView.findViewById(R.id.meta_label),
                    rowView.findViewById(R.id.meta_value)
            );
        }

        void set(int labelResId, String value) {
            labelView.setText(labelResId);
            valueView.setText(value);
        }
    }
}

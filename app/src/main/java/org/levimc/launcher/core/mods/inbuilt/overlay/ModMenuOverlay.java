package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.ExternalModuleProvider;
import org.levimc.launcher.core.mods.inbuilt.InbuiltModuleProvider;
import org.levimc.launcher.core.mods.inbuilt.UnifiedMod;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ModMenuOverlay {
    private enum ModuleFilter {
        ALL,
        FAVORITES,
        ENABLED,
        INBUILT,
        EXTERNAL
    }

    private final Activity activity;
    private View overlayView;
    private WindowManager windowManager;
    private WindowManager.LayoutParams wmParams;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isShowing = false;
    
    private RecyclerView modsRecycler;
    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable searchRunnable = this::applyFilters;
    private ModMenuAdapter adapter;
    private EditText searchInput;
    private ImageButton clearSearchBtn;
    private TextView navModules, navSettings, navHudEditor;
    private ImageButton compactNavModules, compactNavSettings, compactNavHudEditor;
    private TextView filterAll, filterFavorites, filterEnabled, filterInbuilt, filterExternal;
    private TextView moduleCountText, emptyStateText;
    private TextView compactFilterSelector, compactModuleCount;
    private View settingsContainer;
    private View modulesContainer;
    private View emptyState;
    private View menuContainer;
    private View modMenuSidebar;
    private View modMenuLogo;
    private View filterBar;
    private View compactFilterBar;
    private Switch notificationsSwitch;
    private Switch pauseMenuOnlySwitch;
    private Switch compactModeSwitch;
    private SeekBar modMenuOpacitySeekBar;
    private TextView modMenuOpacityText;
    private SeekBar modMenuButtonOpacitySeekBar;
    private TextView modMenuButtonOpacityText;
    private SeekBar hudButtonSizeSeekBar;
    private TextView hudButtonSizeText;
    private boolean updatingHudButtonSize = false;
    private boolean compactMode = false;
    private GridLayoutManager modsLayoutManager;
    
    private List<UnifiedMod> allMods = new ArrayList<>();
    private List<UnifiedMod> filteredMods = new ArrayList<>();
    private final Set<String> favoriteKeys = new HashSet<>();
    private ModuleFilter activeFilter = ModuleFilter.ALL;
    
    private ModMenuCallback callback;
    private ModNotificationManager notificationManager;
    
    private void crossfade(View view) {
        view.setAlpha(0f);
        view.setTranslationX(30f);
        view.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(250)
            .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
            .start();
    }

    private void animateMenuEnter(final View menuContainer) {
        menuContainer.setAlpha(0f);
        menuContainer.setScaleX(0.85f);
        menuContainer.setScaleY(0.85f);
        
        menuContainer.post(() -> {
            menuContainer.setPivotX(menuContainer.getWidth() / 2f);
            menuContainer.setPivotY(menuContainer.getHeight() / 2f);
            
            int opacity = InbuiltModManager.getInstance(activity).getModMenuOpacity();
            float targetAlpha = opacity / 100f;
            
            menuContainer.animate()
                .alpha(targetAlpha)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(220)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
                .withLayer()
                .start();
        });
    }
    
    private void animateMenuExit(final View menuContainer, Runnable onEnd) {
        menuContainer.setPivotX(menuContainer.getWidth() / 2f);
        menuContainer.setPivotY(menuContainer.getHeight() / 2f);
        
        menuContainer.animate()
            .alpha(0f)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(180)
            .setInterpolator(new android.view.animation.AccelerateInterpolator(1.5f))
            .withLayer()
            .setListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    menuContainer.animate().setListener(null);
                    onEnd.run();
                }
            })
            .start();
    }

    private int getAccentColor() {
        return 0xFF4AE0A0;
    }
    
    public interface ModMenuCallback {
        void onModToggled(String modId, boolean enabled);
        void onButtonOpacityChanged(int opacity);
    }
    
    public ModMenuOverlay(Activity activity) {
        this.activity = activity;
        this.windowManager = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
        this.notificationManager = new ModNotificationManager(activity);
    }
    
    public void setCallback(ModMenuCallback callback) {
        this.callback = callback;
    }
    
    public void show() {
        if (isShowing) {
            refreshMods();
            return;
        }
        showInternal();
    }
    
    private void showInternal() {
        if (isShowing || activity.isFinishing() || activity.isDestroyed()) return;
        
        try {
            overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_mod_menu, null);
            
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            overlayView.setSystemUiVisibility(uiOptions);
            
            overlayView.setOnSystemUiVisibilityChangeListener(visibility -> {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    if (overlayView != null) {
                        overlayView.setSystemUiVisibility(uiOptions);
                    }
                }
            });
            
            setupViews();
            loadMods();
            
            wmParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT
            );
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                wmParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            wmParams.gravity = Gravity.CENTER;
            wmParams.token = activity.getWindow().getDecorView().getWindowToken();
            
            windowManager.addView(overlayView, wmParams);
            isShowing = true;
            
            overlayView.setAlpha(0f);
            overlayView.animate().alpha(1f).setDuration(220).start();
            
            View menuContainer = overlayView.findViewById(R.id.mod_menu_container);
            if (menuContainer != null) {
                animateMenuEnter(menuContainer);
            }
        } catch (Exception e) {
            showFallback();
        }
    }
    
    private void showFallback() {
        if (isShowing) return;
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;
        
        overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_mod_menu, null);
        setupViews();
        loadMods();
        
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        rootView.addView(overlayView, params);
        isShowing = true;
        wmParams = null;
        
        overlayView.setAlpha(0f);
        overlayView.animate().alpha(1f).setDuration(220).start();
        
        View menuContainer = overlayView.findViewById(R.id.mod_menu_container);
        if (menuContainer != null) {
            animateMenuEnter(menuContainer);
        }
    }
    
    private void setupViews() {
        menuContainer = overlayView.findViewById(R.id.mod_menu_container);
        modMenuSidebar = overlayView.findViewById(R.id.mod_menu_sidebar);
        modMenuLogo = overlayView.findViewById(R.id.mod_menu_logo);
        filterBar = overlayView.findViewById(R.id.filter_bar);
        compactFilterBar = overlayView.findViewById(R.id.compact_filter_bar);
        compactFilterSelector = overlayView.findViewById(R.id.compact_filter_selector);
        compactModuleCount = overlayView.findViewById(R.id.compact_module_count);
        ImageButton closeBtn = overlayView.findViewById(R.id.btn_close_menu);
        searchInput = overlayView.findViewById(R.id.search_input);
        clearSearchBtn = overlayView.findViewById(R.id.btn_clear_search);
        modsRecycler = overlayView.findViewById(R.id.mods_grid_recycler);
        navModules = overlayView.findViewById(R.id.nav_modules);
        navSettings = overlayView.findViewById(R.id.nav_settings);
        navHudEditor = overlayView.findViewById(R.id.nav_hud_editor);
        compactNavModules = overlayView.findViewById(R.id.nav_modules_compact);
        compactNavSettings = overlayView.findViewById(R.id.nav_settings_compact);
        compactNavHudEditor = overlayView.findViewById(R.id.nav_hud_editor_compact);
        filterAll = overlayView.findViewById(R.id.filter_all);
        filterFavorites = overlayView.findViewById(R.id.filter_favorites);
        filterEnabled = overlayView.findViewById(R.id.filter_enabled);
        filterInbuilt = overlayView.findViewById(R.id.filter_inbuilt);
        filterExternal = overlayView.findViewById(R.id.filter_external);
        moduleCountText = overlayView.findViewById(R.id.module_count_text);
        settingsContainer = overlayView.findViewById(R.id.settings_container);
        modulesContainer = overlayView.findViewById(R.id.modules_container);
        emptyState = overlayView.findViewById(R.id.empty_state);
        emptyStateText = overlayView.findViewById(R.id.empty_state_text);
        notificationsSwitch = overlayView.findViewById(R.id.switch_notifications);
        pauseMenuOnlySwitch = overlayView.findViewById(R.id.switch_pause_menu_only);
        compactModeSwitch = overlayView.findViewById(R.id.switch_compact_mod_menu);

        View hudEditorTools = overlayView.findViewById(R.id.hud_editor_tools);
        View btnHudSave = overlayView.findViewById(R.id.btn_hud_save);
        View btnHudCancel = overlayView.findViewById(R.id.btn_hud_cancel);
        View modMenuContainer = overlayView.findViewById(R.id.mod_menu_container);
        hudButtonSizeSeekBar = overlayView.findViewById(R.id.seekbar_hud_button_size);
        hudButtonSizeText = overlayView.findViewById(R.id.text_hud_button_size);

        if (hudButtonSizeSeekBar != null) {
            hudButtonSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser || updatingHudButtonSize) return;
                    InbuiltOverlayManager manager = InbuiltOverlayManager.getInstance();
                    if (manager != null) {
                        manager.setSelectedHudEditorButtonSize(progress);
                    }
                    updateHudButtonSizeText(progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            updateHudEditorSizeControls(0);
        }

        if (navHudEditor != null) {
            navHudEditor.setOnClickListener(v -> enterHudEditorMode(modMenuContainer, hudEditorTools));
        }
        if (compactNavHudEditor != null) {
            compactNavHudEditor.setOnClickListener(v -> enterHudEditorMode(modMenuContainer, hudEditorTools));
        }
        
        if (btnHudSave != null) {
            btnHudSave.setOnClickListener(v -> {
                exitHudEditorMode(modMenuContainer, hudEditorTools);
            });
        }
        
        View btnHudReset = overlayView.findViewById(R.id.btn_hud_reset);
        if (btnHudReset != null) {
            btnHudReset.setOnClickListener(v -> {
                InbuiltOverlayManager.getInstance().resetAllPositionsToCenter();
            });
        }

        if (btnHudCancel != null) {
            btnHudCancel.setOnClickListener(v -> {
                exitHudEditorMode(modMenuContainer, hudEditorTools);
            });
        }
        
        // Close on background tap
        overlayView.setOnClickListener(v -> {
            // Only hide if not in HUD editor mode
            if (hudEditorTools == null || hudEditorTools.getVisibility() != View.VISIBLE) {
                hide();
            }
        });
        menuContainer.setOnClickListener(v -> {}); // Consume clicks
        if (hudEditorTools != null) {
            hudEditorTools.setOnClickListener(v -> {}); // Consume clicks
        }
        
        closeBtn.setOnClickListener(v -> hide());
        
        // Search functionality
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacks(searchRunnable);
                searchHandler.postDelayed(searchRunnable, 60L);
                clearSearchBtn.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        clearSearchBtn.setOnClickListener(v -> {
            searchInput.setText("");
            clearSearchBtn.setVisibility(View.GONE);
        });
        setupFilterButtons();
        setupCompactFilter();
        
        View btnBackToModules = overlayView.findViewById(R.id.btn_back_to_modules);
        if (btnBackToModules != null) {
            btnBackToModules.setOnClickListener(v -> showModulesSection());
        }
        
        // Navigation
        navModules.setOnClickListener(v -> showModulesSection());
        navSettings.setOnClickListener(v -> showSettingsSection());
        if (compactNavModules != null) compactNavModules.setOnClickListener(v -> showModulesSection());
        if (compactNavSettings != null) compactNavSettings.setOnClickListener(v -> showSettingsSection());

        // Settings
        InbuiltModManager modManager = InbuiltModManager.getInstance(activity);
        compactMode = modManager.isModMenuCompact();
        notificationsSwitch.setChecked(modManager.isNotificationsEnabled());
        notificationsSwitch.setOnCheckedChangeListener((btn, checked) -> {
            modManager.setNotificationsEnabled(checked);
        });

        if (pauseMenuOnlySwitch != null) {
            pauseMenuOnlySwitch.setChecked(modManager.isPauseMenuOnly());
            pauseMenuOnlySwitch.setOnCheckedChangeListener((btn, checked) -> {
                modManager.setPauseMenuOnly(checked);
            });
        }

        if (compactModeSwitch != null) {
            compactModeSwitch.setChecked(compactMode);
            compactModeSwitch.setOnCheckedChangeListener((btn, checked) -> {
                modManager.setModMenuCompact(checked);
                setCompactMode(checked);
            });
        }

        modMenuOpacitySeekBar = overlayView.findViewById(R.id.seekbar_mod_menu_opacity);
        modMenuOpacityText = overlayView.findViewById(R.id.text_mod_menu_opacity);
        int currentMenuOpacity = modManager.getModMenuOpacity();
        modMenuOpacitySeekBar.setProgress(currentMenuOpacity);
        modMenuOpacityText.setText(activity.getString(R.string.mod_menu_percent_value, currentMenuOpacity));
        modMenuOpacitySeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    modMenuOpacityText.setText(activity.getString(R.string.mod_menu_percent_value, progress));
                    modManager.setModMenuOpacity(progress);
                    applyMenuOpacity();
                }
            }
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });

        modMenuButtonOpacitySeekBar = overlayView.findViewById(R.id.seekbar_mod_menu_button_opacity);
        modMenuButtonOpacityText = overlayView.findViewById(R.id.text_mod_menu_button_opacity);
        int currentButtonOpacity = modManager.getModMenuButtonOpacity();
        modMenuButtonOpacitySeekBar.setProgress(currentButtonOpacity);
        modMenuButtonOpacityText.setText(activity.getString(R.string.mod_menu_percent_value, currentButtonOpacity));
        modMenuButtonOpacitySeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    modMenuButtonOpacityText.setText(activity.getString(R.string.mod_menu_percent_value, progress));
                    modManager.setModMenuButtonOpacity(progress);
                    if (callback != null) {
                        callback.onButtonOpacityChanged(progress);
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });
        
        applyMenuOpacity();
        
        adapter = new ModMenuAdapter();
        adapter.setCompactMode(compactMode);
        modsLayoutManager = new GridLayoutManager(activity, compactMode ? 1 : 4);
        modsLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter != null && adapter.isGroupHeader(position)
                    ? modsLayoutManager.getSpanCount()
                    : 1;
            }
        });
        modsRecycler.setLayoutManager(modsLayoutManager);
        modsRecycler.setItemAnimator(null);
        modsRecycler.setHasFixedSize(true);
        adapter.setOnModActionListener(new ModMenuAdapter.OnModActionListener() {
            @Override
            public void onToggle(UnifiedMod mod, boolean enabled) {
                mod.applyEnabled(enabled);
                InbuiltModManager modManager = InbuiltModManager.getInstance(activity);
                if (enabled && modManager.isNotificationsEnabled()) {
                    notificationManager.show(mod.getName(), mod.getStableKey());
                }
                if (callback != null) {
                    callback.onModToggled(mod.getStableKey(), enabled);
                }
                if (activeFilter == ModuleFilter.ENABLED) {
                    applyFilters();
                }
            }
            @Override
            public void onConfig(UnifiedMod mod) {
                showConfigSection(mod);
            }
            @Override
            public void onFavoriteChanged(UnifiedMod mod, boolean favorite) {
                InbuiltModManager.getInstance(activity).setModFavorite(mod.getStableKey(), favorite);
                if (favorite) {
                    favoriteKeys.add(mod.getStableKey());
                } else {
                    favoriteKeys.remove(mod.getStableKey());
                }
                if (activeFilter == ModuleFilter.FAVORITES) {
                    applyFilters();
                }
            }
        });
        modsRecycler.setAdapter(adapter);
        applyCompactModeLayout(compactMode);
        
        showModulesSection();
    }
    
    private void showModulesSection() {
        updateNavigationItem(navModules, compactNavModules, true);
        updateNavigationItem(navSettings, compactNavSettings, false);
        updateNavigationItem(navHudEditor, compactNavHudEditor, false);
        
        if (modulesContainer.getVisibility() != View.VISIBLE) {
            modulesContainer.setVisibility(View.VISIBLE);
            crossfade(modulesContainer);
        }
        settingsContainer.setVisibility(View.GONE);
        
        if (overlayView != null) {
            View modConfigContainer = overlayView.findViewById(R.id.mod_config_container);
            View searchContainer = overlayView.findViewById(R.id.search_container);
            View configHeader = overlayView.findViewById(R.id.config_header);
            if (modConfigContainer != null) modConfigContainer.setVisibility(View.GONE);
            if (searchContainer != null) searchContainer.setVisibility(View.VISIBLE);
            if (configHeader != null) configHeader.setVisibility(View.GONE);
            updateFilterBarVisibility();
        }
    }
    
    private void showSettingsSection() {
        updateNavigationItem(navSettings, compactNavSettings, true);
        updateNavigationItem(navModules, compactNavModules, false);
        updateNavigationItem(navHudEditor, compactNavHudEditor, false);
        
        modulesContainer.setVisibility(View.GONE);
        if (settingsContainer.getVisibility() != View.VISIBLE) {
            settingsContainer.setVisibility(View.VISIBLE);
            crossfade(settingsContainer);
        }
        
        if (overlayView != null) {
            View modConfigContainer = overlayView.findViewById(R.id.mod_config_container);
            View searchContainer = overlayView.findViewById(R.id.search_container);
            View configHeader = overlayView.findViewById(R.id.config_header);
            if (modConfigContainer != null) modConfigContainer.setVisibility(View.GONE);
            if (searchContainer != null) searchContainer.setVisibility(View.VISIBLE);
            if (configHeader != null) configHeader.setVisibility(View.GONE);
            if (filterBar != null) filterBar.setVisibility(View.GONE);
            if (compactFilterBar != null) compactFilterBar.setVisibility(View.GONE);
        }
    }
    
    private void showConfigSection(UnifiedMod mod) {
        if (mod.openCustomConfig()) {
            hide();
            return;
        }
        updateNavigationItem(navModules, compactNavModules, false);
        updateNavigationItem(navSettings, compactNavSettings, false);
        updateNavigationItem(navHudEditor, compactNavHudEditor, false);
        
        modulesContainer.setVisibility(View.GONE);
        settingsContainer.setVisibility(View.GONE);
        
        if (overlayView != null) {
            View modConfigContainer = overlayView.findViewById(R.id.mod_config_container);
            View searchContainer = overlayView.findViewById(R.id.search_container);
            View configHeader = overlayView.findViewById(R.id.config_header);
            ViewGroup modConfigContent = overlayView.findViewById(R.id.mod_config_content);
            TextView configTitle = overlayView.findViewById(R.id.config_title);
            
            if (modConfigContainer != null) {
                modConfigContainer.setVisibility(View.VISIBLE);
                crossfade(modConfigContainer);
            }
            if (searchContainer != null) searchContainer.setVisibility(View.GONE);
            if (configHeader != null) configHeader.setVisibility(View.VISIBLE);
            if (filterBar != null) filterBar.setVisibility(View.GONE);
            if (compactFilterBar != null) compactFilterBar.setVisibility(View.GONE);
            if (configTitle != null) configTitle.setText(mod.getName());
            
            if (modConfigContent != null) {
                ModConfigView.render(activity, modConfigContent, mod, compactMode, () -> {
                    InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
                    if (overlayManager != null) {
                        overlayManager.applyConfigurationChanges(mod.getId());
                    }
                });
            }
        }
    }

    private void enterHudEditorMode(View modMenuContainer, View hudEditorTools) {
        updateNavigationItem(navModules, compactNavModules, false);
        updateNavigationItem(navSettings, compactNavSettings, false);
        updateNavigationItem(navHudEditor, compactNavHudEditor, true);

        if (modMenuContainer != null) {
            modMenuContainer.setVisibility(View.GONE);
        }
        if (hudEditorTools != null) {
            hudEditorTools.setVisibility(View.VISIBLE);
            crossfade(hudEditorTools);
        }
        if (overlayView != null) {
            overlayView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            overlayView.setClickable(false);
        }
        if (wmParams != null && windowManager != null) {
            wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            wmParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            windowManager.updateViewLayout(overlayView, wmParams);
        }
        InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
        if (overlayManager != null) {
            overlayManager.setHudEditorSelectionListener(this::updateHudEditorSizeControls);
            overlayManager.setHudEditorMode(true);
        }
    }

    private void exitHudEditorMode(View modMenuContainer, View hudEditorTools) {
        if (hudEditorTools != null) {
            hudEditorTools.setVisibility(View.GONE);
        }
        if (overlayView != null) {
            overlayView.setClickable(true);
        }
        if (wmParams != null && windowManager != null) {
            wmParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            wmParams.height = WindowManager.LayoutParams.MATCH_PARENT;
            wmParams.gravity = Gravity.CENTER;
            windowManager.updateViewLayout(overlayView, wmParams);
        }
        if (modMenuContainer != null) {
            modMenuContainer.setVisibility(View.VISIBLE);
            animateMenuEnter(modMenuContainer);
        }
        InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
        if (overlayManager != null) {
            overlayManager.setHudEditorMode(false);
            overlayManager.setHudEditorSelectionListener(null);
        }
        showModulesSection();
    }

    private void updateHudEditorSizeControls(int sizeDp) {
        if (hudButtonSizeSeekBar == null) return;
        boolean hasSelection = sizeDp > 0;
        hudButtonSizeSeekBar.setEnabled(hasSelection);
        if (!hasSelection) {
            updateHudButtonSizeText(0);
            return;
        }
        updatingHudButtonSize = true;
        hudButtonSizeSeekBar.setProgress(sizeDp);
        updatingHudButtonSize = false;
        updateHudButtonSizeText(sizeDp);
    }

    private void updateHudButtonSizeText(int size) {
        if (hudButtonSizeText == null) return;
        if (size <= 0) {
            hudButtonSizeText.setText(activity.getString(R.string.overlay_button_size));
        } else {
            hudButtonSizeText.setText(activity.getString(R.string.overlay_button_size_value, size));
        }
    }

    private void setupCompactFilter() {
        if (compactFilterSelector == null) return;
        compactFilterSelector.setOnClickListener(this::showCompactFilterMenu);
        updateCompactFilterSelector();
    }

    private void showCompactFilterMenu(View anchor) {
        PopupMenu popup = new PopupMenu(activity, anchor);
        popup.getMenu().add(0, 100, 0, R.string.filter_all);
        popup.getMenu().add(0, 101, 1, R.string.mod_menu_favorites);
        popup.getMenu().add(0, 102, 2, R.string.mod_menu_filter_enabled);
        popup.getMenu().add(0, 103, 3, R.string.mod_menu_filter_inbuilt);
        popup.getMenu().add(0, 104, 4, R.string.mod_menu_filter_external);
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 101:
                    setModuleFilter(ModuleFilter.FAVORITES);
                    break;
                case 102:
                    setModuleFilter(ModuleFilter.ENABLED);
                    break;
                case 103:
                    setModuleFilter(ModuleFilter.INBUILT);
                    break;
                case 104:
                    setModuleFilter(ModuleFilter.EXTERNAL);
                    break;
                case 100:
                default:
                    setModuleFilter(ModuleFilter.ALL);
                    break;
            }
            return true;
        });
        popup.show();
    }

    private void setCompactMode(boolean compact) {
        compactMode = compact;
        if (modsLayoutManager != null) {
            modsLayoutManager.setSpanCount(compact ? 1 : 4);
        }
        if (adapter != null) {
            adapter.setCompactMode(compact);
            adapter.updateMods(filteredMods, favoriteKeys);
        }
        applyCompactModeLayout(compact);
        updateFilterBarVisibility();
        updateFilterButtons();
        updateModuleCount();
    }

    private void applyCompactModeLayout(boolean compact) {
        if (menuContainer == null) return;
        ViewGroup.LayoutParams rawParams = menuContainer.getLayoutParams();
        if (rawParams instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) rawParams;
            if (compact) {
                int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
                int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
                int availableWidth = Math.max(1, screenWidth - dp(36));
                int desiredWidth = Math.min(dp(392), Math.round(screenWidth * 0.40f));
                int minimumWidth = Math.min(dp(312), availableWidth);
                params.width = Math.min(availableWidth, Math.max(minimumWidth, desiredWidth));
                int availableHeight = Math.max(1, screenHeight - dp(24));
                params.height = Math.min(dp(560), availableHeight);
                params.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
                params.setMargins(dp(18), dp(12), 0, dp(12));
                params.setMarginStart(dp(18));
                params.setMarginEnd(0);
            } else {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                params.gravity = Gravity.CENTER;
                params.setMargins(dp(32), dp(20), dp(32), dp(20));
                params.setMarginStart(dp(32));
                params.setMarginEnd(dp(32));
            }
            menuContainer.setLayoutParams(params);
        }

        if (modMenuSidebar != null) {
            ViewGroup.LayoutParams params = modMenuSidebar.getLayoutParams();
            params.width = dp(compact ? 62 : 150);
            modMenuSidebar.setLayoutParams(params);
            if (modMenuSidebar instanceof android.widget.LinearLayout) {
                ((android.widget.LinearLayout) modMenuSidebar).setGravity(compact ? Gravity.TOP | Gravity.CENTER_HORIZONTAL : Gravity.NO_GRAVITY);
            }
            modMenuSidebar.setPadding(0, dp(14), 0, dp(14));
        }
        if (modMenuLogo != null) modMenuLogo.setVisibility(compact ? View.GONE : View.VISIBLE);

        setNavigationMode(navModules, compactNavModules, R.string.mod_menu_modules, compact);
        setNavigationMode(navHudEditor, compactNavHudEditor, R.string.mod_menu_hud_editor, compact);
        setNavigationMode(navSettings, compactNavSettings, R.string.settings, compact);

        updateNavigationItem(navModules, compactNavModules, modulesContainer != null && modulesContainer.getVisibility() == View.VISIBLE);
        updateNavigationItem(navSettings, compactNavSettings, settingsContainer != null && settingsContainer.getVisibility() == View.VISIBLE);
        updateNavigationItem(navHudEditor, compactNavHudEditor, false);

        if (modsRecycler != null) {
            int padding = dp(compact ? 4 : 14);
            modsRecycler.setPadding(padding, padding, padding, padding);
        }
        menuContainer.requestLayout();
    }

    private void setNavigationMode(TextView fullView, ImageButton compactView, int textRes, boolean compact) {
        if (fullView != null) {
            fullView.setVisibility(compact ? View.GONE : View.VISIBLE);
            if (!compact) {
                fullView.setText(textRes);
                fullView.setGravity(Gravity.CENTER_VERTICAL);
                fullView.setIncludeFontPadding(true);
                fullView.setPadding(dp(16), 0, dp(12), 0);
                fullView.setCompoundDrawablePadding(dp(8));
                ViewGroup.LayoutParams rawParams = fullView.getLayoutParams();
                if (rawParams instanceof android.widget.LinearLayout.LayoutParams) {
                    android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) rawParams;
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    params.height = dp(44);
                    params.gravity = Gravity.NO_GRAVITY;
                    params.setMargins(0, 0, 0, 0);
                    fullView.setLayoutParams(params);
                }
            }
        }
        if (compactView != null) compactView.setVisibility(compact ? View.VISIBLE : View.GONE);
    }

    private void updateFilterBarVisibility() {
        boolean modulesVisible = modulesContainer != null && modulesContainer.getVisibility() == View.VISIBLE;
        if (filterBar != null) {
            filterBar.setVisibility(modulesVisible && !compactMode ? View.VISIBLE : View.GONE);
        }
        if (compactFilterBar != null) {
            compactFilterBar.setVisibility(modulesVisible && compactMode ? View.VISIBLE : View.GONE);
        }
    }

    private void updateCompactFilterSelector() {
        if (compactFilterSelector == null) return;
        compactFilterSelector.setText(getFilterLabelRes(activeFilter));
        compactFilterSelector.setTextColor(getAccentColor());
    }

    private int getFilterLabelRes(ModuleFilter filter) {
        switch (filter) {
            case FAVORITES:
                return R.string.mod_menu_favorites;
            case ENABLED:
                return R.string.mod_menu_filter_enabled;
            case INBUILT:
                return R.string.mod_menu_filter_inbuilt;
            case EXTERNAL:
                return R.string.mod_menu_filter_external;
            case ALL:
            default:
                return R.string.filter_all;
        }
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private void setupFilterButtons() {
        if (filterAll != null) {
            filterAll.setOnClickListener(v -> setModuleFilter(ModuleFilter.ALL));
        }
        if (filterFavorites != null) {
            filterFavorites.setOnClickListener(v -> setModuleFilter(ModuleFilter.FAVORITES));
        }
        if (filterEnabled != null) {
            filterEnabled.setOnClickListener(v -> setModuleFilter(ModuleFilter.ENABLED));
        }
        if (filterInbuilt != null) {
            filterInbuilt.setOnClickListener(v -> setModuleFilter(ModuleFilter.INBUILT));
        }
        if (filterExternal != null) {
            filterExternal.setOnClickListener(v -> setModuleFilter(ModuleFilter.EXTERNAL));
        }
        updateFilterButtons();
    }

    private void setModuleFilter(ModuleFilter filter) {
        activeFilter = filter;
        updateFilterButtons();
        applyFilters();
    }

    private void applyFilters() {
        filteredMods.clear();
        String query = searchInput != null
            ? searchInput.getText().toString().trim().toLowerCase(Locale.ROOT)
            : "";

        Map<String, GroupedMods> groupedMatches = new LinkedHashMap<>();
        for (UnifiedMod mod : allMods) {
            if (matchesActiveFilter(mod) && matchesQuery(mod, query)) {
                GroupedMods group = groupedMatches.computeIfAbsent(
                    mod.getGroupId(), ignored -> new GroupedMods());
                group.add(mod, isFavorite(mod));
            }
        }
        for (GroupedMods group : groupedMatches.values()) {
            group.appendTo(filteredMods);
        }

        if (adapter != null) {
            adapter.updateMods(filteredMods, favoriteKeys);
        }
        updateEmptyState();
        updateModuleCount();
    }

    private boolean matchesActiveFilter(UnifiedMod mod) {
        switch (activeFilter) {
            case FAVORITES:
                return isFavorite(mod);
            case ENABLED:
                return mod.isEnabled();
            case INBUILT:
                return mod.getSource() == UnifiedMod.Source.INBUILT;
            case EXTERNAL:
                return mod.getSource() == UnifiedMod.Source.EXTERNAL;
            case ALL:
            default:
                return true;
        }
    }

    private boolean matchesQuery(UnifiedMod mod, String query) {
        if (query.isEmpty()) return true;
        String searchText = (
            safeString(mod.getName()) + " " +
            safeString(mod.getDescription()) + " " +
            safeString(mod.getId()) + " " +
            safeString(mod.getModId()) + " " +
            safeString(mod.getGroupName()) + " " +
            safeString(mod.getGroupId())
        ).toLowerCase(Locale.ROOT);
        return searchText.contains(query);
    }

    private boolean isFavorite(UnifiedMod mod) {
        return favoriteKeys.contains(mod.getStableKey());
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private void updateFilterButtons() {
        updateFilterButton(filterAll, activeFilter == ModuleFilter.ALL);
        updateFilterButton(filterFavorites, activeFilter == ModuleFilter.FAVORITES);
        updateFilterButton(filterEnabled, activeFilter == ModuleFilter.ENABLED);
        updateFilterButton(filterInbuilt, activeFilter == ModuleFilter.INBUILT);
        updateFilterButton(filterExternal, activeFilter == ModuleFilter.EXTERNAL);
        updateCompactFilterSelector();
    }

    private void updateFilterButton(TextView view, boolean selected) {
        if (view == null) return;
        view.setTextColor(selected ? getAccentColor() : 0xFFA8B0B8);
        view.setAlpha(1f);
        Drawable background = view.getBackground();
        if (background != null) {
            background.mutate().setTint(selected ? 0x334AE0A0 : 0xFF24282C);
        }
    }

    private void updateNavigationItem(TextView fullView, ImageButton compactView, boolean selected) {
        int color = selected ? getAccentColor() : 0xFFA8B0B8;
        if (fullView != null) {
            fullView.setTextColor(color);
            fullView.setAlpha(selected ? 1f : 0.82f);
            fullView.setCompoundDrawableTintList(ColorStateList.valueOf(color));
            TypedValue typedValue = new TypedValue();
            if (activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)) {
                fullView.setBackgroundResource(typedValue.resourceId);
            }
        }
        if (compactView != null) {
            compactView.setAlpha(selected ? 1f : 0.88f);
            compactView.setImageTintList(ColorStateList.valueOf(color));
            if (selected) {
                compactView.setBackgroundResource(R.drawable.bg_mod_menu_nav_compact_selected);
            } else {
                TypedValue typedValue = new TypedValue();
                if (activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, typedValue, true)) {
                    compactView.setBackgroundResource(typedValue.resourceId);
                } else {
                    compactView.setBackground(null);
                }
            }
        }
    }

    private void updateModuleCount() {
        String value = activity.getString(
            R.string.mod_menu_module_count,
            filteredMods.size(),
            allMods.size());
        if (moduleCountText != null) {
            moduleCountText.setText(value);
        }
        if (compactModuleCount != null) {
            compactModuleCount.setText(value);
        }
    }

    private void loadMods() {
        allMods.clear();

        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        favoriteKeys.clear();
        favoriteKeys.addAll(manager.getFavoriteModKeys());
        allMods.addAll(InbuiltModuleProvider.load(activity));
        allMods.addAll(ExternalModuleProvider.load(activity));

        applyFilters();
    }
    
    private void filterMods(String query) {
        applyFilters();
    }
    
    private void updateEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(filteredMods.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (emptyStateText != null) {
            String query = searchInput != null ? searchInput.getText().toString().trim() : "";
            if (!query.isEmpty()) {
                emptyStateText.setText(R.string.mod_menu_no_matches);
            } else if (activeFilter == ModuleFilter.FAVORITES) {
                emptyStateText.setText(R.string.mod_menu_no_favorites);
            } else {
                emptyStateText.setText(R.string.mod_menu_no_mods);
            }
        }
    }
    
    public void refreshMods() {
        loadMods();
    }

    private void applyMenuOpacity() {
        if (overlayView != null) {
            View menuContainer = overlayView.findViewById(R.id.mod_menu_container);
            if (menuContainer != null) {
                int opacity = InbuiltModManager.getInstance(activity).getModMenuOpacity();
                menuContainer.setAlpha(opacity / 100f);
            }
        }
    }
    
    public void hide() {
        if (!isShowing || overlayView == null) return;

        InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
        if (overlayManager != null) {
            overlayManager.setHudEditorMode(false);
            overlayManager.setHudEditorSelectionListener(null);
        }
        
        Runnable performHide = () -> {
            handler.post(() -> {
                try {
                    if (wmParams != null && windowManager != null) {
                        windowManager.removeView(overlayView);
                    } else {
                        ViewGroup rootView = activity.findViewById(android.R.id.content);
                        if (rootView != null) {
                            rootView.removeView(overlayView);
                        }
                    }
                } catch (Exception ignored) {}
                overlayView = null;
                isShowing = false;
            });
        };
        
        View menuContainer = overlayView.findViewById(R.id.mod_menu_container);
        if (menuContainer != null) {
            animateMenuExit(menuContainer, performHide);
        } else {
            performHide.run();
        }
        overlayView.animate().alpha(0f).setDuration(180).start();
    }
    
    public boolean isShowing() {
        return isShowing;
    }

    private static class GroupedMods {
        private final List<UnifiedMod> favorites = new ArrayList<>();
        private final List<UnifiedMod> others = new ArrayList<>();

        void add(UnifiedMod mod, boolean favorite) {
            if (favorite) {
                favorites.add(mod);
            } else {
                others.add(mod);
            }
        }

        void appendTo(List<UnifiedMod> target) {
            sort(favorites);
            sort(others);
            target.addAll(favorites);
            target.addAll(others);
        }

        private void sort(List<UnifiedMod> mods) {
            mods.sort((left, right) -> left.getName().compareToIgnoreCase(right.getName()));
        }
    }
}

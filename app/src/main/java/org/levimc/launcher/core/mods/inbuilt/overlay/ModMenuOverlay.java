package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.graphics.PixelFormat;
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
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.model.InbuiltMod;

import java.util.ArrayList;
import java.util.List;

public class ModMenuOverlay {
    private final Activity activity;
    private View overlayView;
    private WindowManager windowManager;
    private WindowManager.LayoutParams wmParams;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isShowing = false;
    
    private RecyclerView modsRecycler;
    private ModMenuAdapter adapter;
    private EditText searchInput;
    private ImageButton clearSearchBtn;
    private TextView navModules, navSettings;
    private View settingsContainer;
    private View modulesContainer;
    private View emptyState;
    private Switch notificationsSwitch;
    private SeekBar modMenuOpacitySeekBar;
    private TextView modMenuOpacityText;
    private SeekBar modMenuButtonOpacitySeekBar;
    private TextView modMenuButtonOpacityText;
    
    private List<InbuiltMod> allMods = new ArrayList<>();
    private List<InbuiltMod> filteredMods = new ArrayList<>();
    
    private ModMenuCallback callback;
    private ModNotificationManager notificationManager;
    
    public interface ModMenuCallback {
        void onModToggled(String modId, boolean enabled);
        void onModConfigRequested(InbuiltMod mod);
    }

    public interface ModMenuButtonCallback extends ModMenuCallback {
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
            // If already showing, just refresh the mods
            refreshMods();
            return;
        }
        handler.postDelayed(this::showInternal, 100);
    }
    
    private void showInternal() {
        if (isShowing || activity.isFinishing() || activity.isDestroyed()) return;
        
        try {
            overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_mod_menu, null);
            setupViews();
            loadMods();
            
            wmParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            );
            wmParams.gravity = Gravity.CENTER;
            wmParams.token = activity.getWindow().getDecorView().getWindowToken();
            
            windowManager.addView(overlayView, wmParams);
            isShowing = true;
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
    }
    
    private void setupViews() {
        View menuContainer = overlayView.findViewById(R.id.mod_menu_container);
        ImageButton closeBtn = overlayView.findViewById(R.id.btn_close_menu);
        searchInput = overlayView.findViewById(R.id.search_input);
        clearSearchBtn = overlayView.findViewById(R.id.btn_clear_search);
        modsRecycler = overlayView.findViewById(R.id.mods_grid_recycler);
        navModules = overlayView.findViewById(R.id.nav_modules);
        navSettings = overlayView.findViewById(R.id.nav_settings);
        settingsContainer = overlayView.findViewById(R.id.settings_container);
        modulesContainer = overlayView.findViewById(R.id.modules_container);
        emptyState = overlayView.findViewById(R.id.empty_state);
        notificationsSwitch = overlayView.findViewById(R.id.switch_notifications);
        
        // Close on background tap
        overlayView.setOnClickListener(v -> hide());
        menuContainer.setOnClickListener(v -> {}); // Consume clicks
        
        closeBtn.setOnClickListener(v -> hide());
        
        // Search functionality
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMods(s.toString());
                clearSearchBtn.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        clearSearchBtn.setOnClickListener(v -> {
            searchInput.setText("");
            clearSearchBtn.setVisibility(View.GONE);
        });
        
        // Navigation
        navModules.setOnClickListener(v -> showModulesSection());
        navSettings.setOnClickListener(v -> showSettingsSection());

        // Settings
        InbuiltModManager modManager = InbuiltModManager.getInstance(activity);
        notificationsSwitch.setChecked(modManager.isNotificationsEnabled());
        notificationsSwitch.setOnCheckedChangeListener((btn, checked) -> {
            modManager.setNotificationsEnabled(checked);
        });

        modMenuOpacitySeekBar = overlayView.findViewById(R.id.seekbar_mod_menu_opacity);
        modMenuOpacityText = overlayView.findViewById(R.id.text_mod_menu_opacity);
        int currentMenuOpacity = modManager.getModMenuOpacity();
        modMenuOpacitySeekBar.setProgress(currentMenuOpacity);
        modMenuOpacityText.setText(currentMenuOpacity + "%");
        modMenuOpacitySeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    modMenuOpacityText.setText(progress + "%");
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
        modMenuButtonOpacityText.setText(currentButtonOpacity + "%");
        modMenuButtonOpacitySeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    modMenuButtonOpacityText.setText(progress + "%");
                    modManager.setModMenuButtonOpacity(progress);
                    if (callback != null && callback instanceof ModMenuButtonCallback) {
                        ((ModMenuButtonCallback) callback).onButtonOpacityChanged(progress);
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });
        
        applyMenuOpacity();
        
        // Setup RecyclerView
        modsRecycler.setLayoutManager(new GridLayoutManager(activity, 3));
        adapter = new ModMenuAdapter();
        adapter.setOnModActionListener(new ModMenuAdapter.OnModActionListener() {
            @Override
            public void onToggle(InbuiltMod mod, boolean enabled) {
                InbuiltOverlayManager manager = InbuiltOverlayManager.getInstance();
                if (manager != null) {
                    manager.handleModToggle(mod.getId(), enabled);
                }
                if (enabled && InbuiltModManager.getInstance(activity).isNotificationsEnabled()) {
                    notificationManager.show(mod.getName(), mod.getId());
                }
                if (callback != null) {
                    callback.onModToggled(mod.getId(), enabled);
                }
            }
            @Override
            public void onConfig(InbuiltMod mod) {
                if (callback != null) {
                    callback.onModConfigRequested(mod);
                }
            }
        });
        modsRecycler.setAdapter(adapter);
        
        showModulesSection();
    }
    
    private void showModulesSection() {
        navModules.setTextColor(0xFF4AE0A0);
        navModules.setAlpha(1f);
        navSettings.setTextColor(0xFFAAAAAA);
        navSettings.setAlpha(0.6f);
        modulesContainer.setVisibility(View.VISIBLE);
        settingsContainer.setVisibility(View.GONE);
    }
    
    private void showSettingsSection() {
        navSettings.setTextColor(0xFF4AE0A0);
        navSettings.setAlpha(1f);
        navModules.setTextColor(0xFFAAAAAA);
        navModules.setAlpha(0.6f);
        modulesContainer.setVisibility(View.GONE);
        settingsContainer.setVisibility(View.VISIBLE);
    }
    
    private void loadMods() {
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        allMods = manager.getAllMods(activity);
        filteredMods = new ArrayList<>(allMods);
        adapter.updateMods(filteredMods);
        updateEmptyState();
    }
    
    private void filterMods(String query) {
        filteredMods.clear();
        if (query.isEmpty()) {
            filteredMods.addAll(allMods);
        } else {
            String lowerQuery = query.toLowerCase();
            for (InbuiltMod mod : allMods) {
                if (mod.getName().toLowerCase().contains(lowerQuery)) {
                    filteredMods.add(mod);
                }
            }
        }
        adapter.updateMods(filteredMods);
        updateEmptyState();
    }
    
    private void updateEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(filteredMods.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
    
    public void refreshMods() {
        loadMods();
        String currentQuery = searchInput != null ? searchInput.getText().toString() : "";
        if (!currentQuery.isEmpty()) {
            filterMods(currentQuery);
        }
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
    }
    
    public boolean isShowing() {
        return isShowing;
    }
}

package org.levimc.launcher.core.mods.memoryeditor;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.levimc.launcher.R;
import org.levimc.launcher.ui.adapter.MemoryResultAdapter;
import org.levimc.launcher.ui.adapter.MemorySavedAdapter;
import android.app.AlertDialog;
import android.widget.CheckBox;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MemoryEditorOverlay {
    private final Activity activity;
    private View overlayView;
    private WindowManager windowManager;
    private WindowManager.LayoutParams wmParams;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isShowing = false;
    private float initialX, initialY, initialTouchX, initialTouchY;
    private boolean isDragging = false;
    private boolean isResizing = false;
    private int initialWidth, initialHeight;
    private static final float DRAG_THRESHOLD = 10f;
    private int minWidth, minHeight, maxWidth, maxHeight;

    private View editorContainer;
    private Spinner spinnerType, spinnerCondition;
    private EditText inputValue;
    private Button btnSearch, btnFilter, btnReset;
    private Button btnTabSearch, btnTabSaved;
    private TextView resultCount;
    private RecyclerView resultsRecycler;
    private MemoryResultAdapter resultAdapter;
    private MemorySavedAdapter savedAdapter;
    private boolean showingSaved = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MemoryEditorOverlay(Activity activity) {
        this.activity = activity;
        this.windowManager = (WindowManager) activity.getSystemService(android.content.Context.WINDOW_SERVICE);
        float density = activity.getResources().getDisplayMetrics().density;
        minWidth = (int)(220 * density);
        minHeight = (int)(150 * density);
        maxWidth = (int)(800 * density);
        maxHeight = (int)(700 * density);
    }

    public void show() {
        if (isShowing) return;
        handler.postDelayed(this::showInternal, 300);
    }

    private void showInternal() {
        if (isShowing || activity.isFinishing() || activity.isDestroyed()) return;
        try {
            MemorySearchEngine.getInstance().init();
            FreezeManager.getInstance().start();
            overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_memory_editor, null);
            setupViews();
            wmParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            );
            wmParams.gravity = Gravity.TOP | Gravity.START;
            wmParams.x = 50;
            wmParams.y = 100;
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
        MemorySearchEngine.getInstance().init();
        FreezeManager.getInstance().start();
        overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_memory_editor, null);
        setupViews();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.leftMargin = 50;
        params.topMargin = 100;
        rootView.addView(overlayView, params);
        isShowing = true;
        wmParams = null;
    }

    private void setupViews() {
        editorContainer = overlayView.findViewById(R.id.editor_container);
        spinnerType = overlayView.findViewById(R.id.spinner_type);
        spinnerCondition = overlayView.findViewById(R.id.spinner_condition);
        inputValue = overlayView.findViewById(R.id.input_value);
        btnSearch = overlayView.findViewById(R.id.btn_search);
        btnFilter = overlayView.findViewById(R.id.btn_filter);
        btnReset = overlayView.findViewById(R.id.btn_reset);
        btnTabSearch = overlayView.findViewById(R.id.btn_tab_search);
        btnTabSaved = overlayView.findViewById(R.id.btn_tab_saved);
        resultCount = overlayView.findViewById(R.id.result_count);
        resultsRecycler = overlayView.findViewById(R.id.results_recycler);
        ImageButton btnClose = overlayView.findViewById(R.id.btn_close);
        View header = overlayView.findViewById(R.id.editor_header);
        View resizeHandle = overlayView.findViewById(R.id.resize_handle);

        String[] types = {"Byte", "Word", "Dword", "Qword", "Float", "Double", "XOR", "Auto"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item_memory, types);
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_memory);
        spinnerType.setAdapter(typeAdapter);
        spinnerType.setSelection(4);

        String[] conditions = {"=", "≠", ">", "<", "≥", "≤"};
        ArrayAdapter<String> condAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item_memory, conditions);
        condAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_memory);
        spinnerCondition.setAdapter(condAdapter);

        resultsRecycler.setLayoutManager(new LinearLayoutManager(activity));
        resultAdapter = new MemoryResultAdapter();
        savedAdapter = new MemorySavedAdapter();
        resultsRecycler.setAdapter(resultAdapter);

        resultAdapter.setOnItemActionListener(new MemoryResultAdapter.OnItemActionListener() {
            @Override
            public void onSave(MemoryAddress address) {
                SavedAddressManager.getInstance(activity).addAddress(address);
            }
            @Override
            public void onFreeze(MemoryAddress address, boolean frozen) {}
            @Override
            public void onOverlayConfig(MemoryAddress address) {
                List<MemoryAddress> saved = SavedAddressManager.getInstance(activity).getSavedAddresses();
                int position = -1;
                for (int i = 0; i < saved.size(); i++) {
                    if (saved.get(i).getAddress() == address.getAddress()) {
                        position = i;
                        break;
                    }
                }
                if (position >= 0) {
                    showOverlayConfigDialog(position, saved.get(position));
                }
            }
        });

        savedAdapter.setOnItemActionListener(new MemorySavedAdapter.OnItemActionListener() {
            @Override
            public void onDelete(int position) {
                SavedAddressManager.getInstance(activity).removeAddress(position);
                refreshSavedList();
            }
            @Override
            public void onFreeze(MemoryAddress address, boolean frozen) {}
            @Override
            public void onUpdate(int position, MemoryAddress address) {
                SavedAddressManager.getInstance(activity).updateAddress(position, address);
            }
            @Override
            public void onOverlayConfig(int position, MemoryAddress address) {
                showOverlayConfigDialog(position, address);
            }
        });

        btnSearch.setOnClickListener(v -> performSearch());
        btnFilter.setOnClickListener(v -> performFilter());
        btnReset.setOnClickListener(v -> {
            MemorySearchEngine.getInstance().clearResults();
            updateResultCount();
            resultAdapter.updateItems(java.util.Collections.emptyList());
        });

        btnTabSearch.setOnClickListener(v -> {
            showingSaved = false;
            btnTabSearch.setTextColor(0xFF00FF88);
            btnTabSaved.setTextColor(0xFF888888);
            resultsRecycler.setAdapter(resultAdapter);
            refreshSearchResults();
        });

        btnTabSaved.setOnClickListener(v -> {
            showingSaved = true;
            btnTabSaved.setTextColor(0xFF00FF88);
            btnTabSearch.setTextColor(0xFF888888);
            resultsRecycler.setAdapter(savedAdapter);
            refreshSavedList();
        });

        btnClose.setOnClickListener(v -> hideOverlayOnly());

        header.setOnTouchListener(this::handleDrag);
        if (resizeHandle != null) {
            resizeHandle.setOnTouchListener(this::handleResize);
        }

        inputValue.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setFocusable(true);
            }
            return false;
        });
        inputValue.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showKeyboard(inputValue);
            }
        });
        inputValue.setOnEditorActionListener((v, actionId, event) -> {
            hideKeyboard();
            return true;
        });

        resultAdapter.setOverlay(this);
        savedAdapter.setOverlay(this);

        updateResultCount();
        refreshSearchResults();
    }

    public void requestFocusForEdit(EditText editText) {
        setFocusable(true);
        editText.requestFocus();
        showKeyboard(editText);
    }

    public void dismissKeyboardFromEdit(EditText editText) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
        setFocusable(false);
    }

    private void setFocusable(boolean focusable) {
        if (wmParams == null) return;
        if (focusable) {
            wmParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        } else {
            wmParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        try {
            windowManager.updateViewLayout(overlayView, wmParams);
        } catch (Exception ignored) {}
    }

    private void showKeyboard(EditText editText) {
        handler.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 100);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && inputValue != null) {
            imm.hideSoftInputFromWindow(inputValue.getWindowToken(), 0);
        }
        setFocusable(false);
    }

    private void performSearch() {
        String value = inputValue.getText().toString().trim();
        if (value.isEmpty()) return;
        hideKeyboard();
        int typePos = spinnerType.getSelectedItemPosition();
        ValueType type = ValueType.fromId(typePos);
        MemorySearchEngine engine = MemorySearchEngine.getInstance();
        engine.setValueType(type);
        if (type == ValueType.XOR) {
            engine.setXorMode(true);
            engine.setXorKey(0x12345678);
        } else {
            engine.setXorMode(false);
        }
        resultCount.setText("Searching...");
        btnSearch.setEnabled(false);
        executor.execute(() -> {
            engine.search(value);
            handler.post(() -> {
                btnSearch.setEnabled(true);
                updateResultCount();
                refreshSearchResults();
            });
        });
    }

    private void performFilter() {
        String value = inputValue.getText().toString().trim();
        if (value.isEmpty()) return;
        int currentResults = MemorySearchEngine.getInstance().getResultCount();
        if (currentResults == 0) {
            resultCount.setText("Search first!");
            return;
        }
        hideKeyboard();
        int condPos = spinnerCondition.getSelectedItemPosition();
        SearchCondition condition = SearchCondition.fromId(condPos);
        resultCount.setText("Filtering " + currentResults + "...");
        btnFilter.setEnabled(false);
        executor.execute(() -> {
            MemorySearchEngine.getInstance().filter(value, condition);
            handler.post(() -> {
                btnFilter.setEnabled(true);
                updateResultCount();
                refreshSearchResults();
            });
        });
    }

    private void updateResultCount() {
        int count = MemorySearchEngine.getInstance().getResultCount();
        resultCount.setText(count + " results");
    }

    private void refreshSearchResults() {
        List<MemoryAddress> results = MemorySearchEngine.getInstance().getResults(0, 100);
        resultAdapter.updateItems(results);
    }

    private void refreshSavedList() {
        List<MemoryAddress> saved = SavedAddressManager.getInstance(activity).getSavedAddresses();
        savedAdapter.updateItems(saved);
    }

    private boolean handleResize(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                initialWidth = editorContainer.getWidth();
                initialHeight = editorContainer.getHeight();
                isResizing = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (isResizing) {
                    float dx = event.getRawX() - initialTouchX;
                    float dy = event.getRawY() - initialTouchY;
                    int newWidth = Math.max(minWidth, Math.min(maxWidth, (int)(initialWidth + dx)));
                    int newHeight = Math.max(minHeight, Math.min(maxHeight, (int)(initialHeight + dy)));
                    ViewGroup.LayoutParams lp = editorContainer.getLayoutParams();
                    lp.width = newWidth;
                    lp.height = newHeight;
                    editorContainer.setLayoutParams(lp);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isResizing = false;
                return true;
        }
        return false;
    }

    private boolean handleDrag(View v, MotionEvent event) {
        if (wmParams == null) return handleDragFallback(v, event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initialX = wmParams.x;
                initialY = wmParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                isDragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
                if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
                    isDragging = true;
                }
                if (isDragging && windowManager != null && overlayView != null) {
                    wmParams.x = (int) (initialX + dx);
                    wmParams.y = (int) (initialY + dy);
                    windowManager.updateViewLayout(overlayView, wmParams);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                return true;
        }
        return false;
    }

    private boolean handleDragFallback(View v, MotionEvent event) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) overlayView.getLayoutParams();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initialX = params.leftMargin;
                initialY = params.topMargin;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                isDragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
                if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
                    isDragging = true;
                }
                if (isDragging) {
                    params.leftMargin = (int) (initialX + dx);
                    params.topMargin = (int) (initialY + dy);
                    overlayView.setLayoutParams(params);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                return true;
        }
        return false;
    }

    private void hideOverlayOnly() {
        if (!isShowing || overlayView == null) return;
        hideKeyboard();
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

    public void hide() {
        if (!isShowing || overlayView == null) return;
        hideKeyboard();
        handler.post(() -> {
            try {
                FreezeManager.getInstance().stop();
                MemorySearchEngine.getInstance().close();
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

    private void showOverlayConfigDialog(int position, MemoryAddress address) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_memory_overlay_config, null);
        EditText editName = dialogView.findViewById(R.id.edit_name);
        EditText editOriginal = dialogView.findViewById(R.id.edit_original_value);
        EditText editNew = dialogView.findViewById(R.id.edit_new_value);
        CheckBox checkToggleable = dialogView.findViewById(R.id.checkbox_toggleable);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        String currentValue = address.readValue();
        editName.setText(address.getOverlayName());
        editOriginal.setText(address.isOverlayEnabled() ? address.getOverlayOriginalValue() : currentValue);
        editNew.setText(address.getOverlayNewValue());
        checkToggleable.setChecked(address.isOverlayToggleable());

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.MemoryEditorDialogTheme);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> {
            if (address.isOverlayEnabled()) {
                long addrValue = address.getAddress();
                address.setOverlayEnabled(false);
                address.setOverlayToggleable(false);
                address.setOverlayOriginalValue("");
                address.setOverlayNewValue("");
                address.setOverlayName("");
                SavedAddressManager.getInstance(activity).updateAddress(position, address);
                savedAdapter.notifyOverlayStateChanged(position);
                org.levimc.launcher.core.mods.inbuilt.overlay.InbuiltOverlayManager mgr = 
                    org.levimc.launcher.core.mods.inbuilt.overlay.InbuiltOverlayManager.getInstance();
                if (mgr != null) {
                    mgr.removeMemoryOverlay(addrValue);
                }
            }
            dialog.dismiss();
        });

        btnSave.setOnClickListener(v -> {
            String nameVal = editName.getText().toString().trim();
            String originalVal = editOriginal.getText().toString().trim();
            String newVal = editNew.getText().toString().trim();
            if (newVal.isEmpty()) {
                editNew.setError(activity.getString(R.string.memory_overlay_new_value_hint));
                return;
            }
            boolean wasEnabled = address.isOverlayEnabled();
            address.setOverlayEnabled(true);
            address.setOverlayToggleable(checkToggleable.isChecked());
            address.setOverlayOriginalValue(originalVal);
            address.setOverlayNewValue(newVal);
            address.setOverlayName(nameVal);
            SavedAddressManager.getInstance(activity).updateAddress(position, address);
            savedAdapter.notifyOverlayStateChanged(position);
            if (!wasEnabled) {
                org.levimc.launcher.core.mods.inbuilt.overlay.InbuiltOverlayManager mgr = 
                    org.levimc.launcher.core.mods.inbuilt.overlay.InbuiltOverlayManager.getInstance();
                if (mgr != null) {
                    mgr.addMemoryOverlay(address);
                }
            }
            dialog.dismiss();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            float density = activity.getResources().getDisplayMetrics().density;
            int widthPx = (int) (400 * density);
            dialog.getWindow().setLayout(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}

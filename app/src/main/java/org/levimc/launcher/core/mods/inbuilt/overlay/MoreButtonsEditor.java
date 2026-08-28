package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.manager.MoreButtonsManager;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;
import org.levimc.launcher.core.mods.inbuilt.model.MoreButtonConfig;
import org.levimc.pojavcontrols.KeyMapper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MoreButtonsEditor {
    private static final int REQUEST_SVG = 0x4D42;
    private static final int ACCENT = 0xFF4AE0A0;
    private static EditorHost editor;
    private static WeakReference<Activity> editorActivity = new WeakReference<>(null);
    private static WeakReference<EditPage> pendingSession;
    private static boolean pendingPressedSvg;

    private MoreButtonsEditor() {}

    public static void show(Activity activity) {
        activity.runOnUiThread(() -> {
            if (editor != null && editorActivity.get() == activity && editor.isAttachedToWindow()) {
                editor.bringToFront();
                applyImmersive(activity);
                return;
            }
            closeImmediately();
            View content = activity.findViewById(android.R.id.content);
            if (!(content instanceof ViewGroup)) return;
            editor = new EditorHost(activity);
            ((ViewGroup) content).addView(editor, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            editorActivity = new WeakReference<>(activity);
            editor.bringToFront();
            InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
            if (overlayManager != null) overlayManager.setMoreButtonsEditorOpen(true);
            applyImmersive(activity);
        });
    }

    public static void onResume() {
        Activity activity = editorActivity.get();
        if (editor != null && activity != null) applyImmersive(activity);
    }

    public static boolean closeEditor() {
        if (editor == null) return false;
        editor.handleBack();
        return true;
    }

    public static boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_SVG) return false;
        Activity activity = editorActivity.get();
        if (activity != null) applyImmersive(activity);
        EditPage session = pendingSession == null ? null : pendingSession.get();
        pendingSession = null;
        if (session == null || resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return true;
        }
        session.acceptSvg(data.getData(), pendingPressedSvg);
        return true;
    }

    private static void closeImmediately() {
        if (editor != null) editor.dispose();
        if (editor != null && editor.getParent() instanceof ViewGroup) {
            ((ViewGroup) editor.getParent()).removeView(editor);
        }
        editor = null;
        editorActivity.clear();
        pendingSession = null;
        InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
        if (overlayManager != null) overlayManager.setMoreButtonsEditorOpen(false);
    }

    private static void applyImmersive(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private static final class EditorHost extends FrameLayout {
        private final Activity activity;
        private final MoreButtonsManager manager;
        private Runnable backAction;
        private EditPage currentEdit;

        EditorHost(Activity activity) {
            super(activity);
            this.activity = activity;
            this.manager = MoreButtonsManager.getInstance(activity);
            setBackgroundColor(0xFF111315);
            setClickable(true);
            setFocusable(true);
            setFocusableInTouchMode(true);
            setOnSystemUiVisibilityChangeListener(visibility -> postDelayed(() -> applyImmersive(activity), 120));
            showMain();
        }

        void handleBack() {
            if (backAction != null) backAction.run();
            else close();
        }

        void close() {
            closeImmediately();
            applyImmersive(activity);
        }

        void dispose() {
            if (currentEdit != null) currentEdit.dispose();
            currentEdit = null;
        }

        void showMain() {
            if (currentEdit != null) currentEdit.dispose();
            currentEdit = null;
            backAction = this::close;
            float d = density(activity);
            LinearLayout root = screenRoot(activity);
            root.addView(toolbar(activity.getString(R.string.inbuilt_mod_more_buttons), activity.getString(R.string.close), this::close));

            ScrollView scroll = new ScrollView(activity);
            LinearLayout content = vertical(activity, 16);
            TextView intro = text(activity,
                    activity.getString(R.string.more_buttons_intro),
                    13, 0xFFD6DADF);
            content.addView(intro);

            LinearLayout actions = horizontal(activity);
            Button add = actionButton(activity, activity.getString(R.string.more_buttons_add_button));
            Button tutorial = actionButton(activity, activity.getString(R.string.tutorial));
            actions.addView(add, weighted());
            LinearLayout.LayoutParams tutorialParams = weighted();
            tutorialParams.leftMargin = Math.round(8 * d);
            actions.addView(tutorial, tutorialParams);
            content.addView(actions, topMargin(12, d));

            add.setOnClickListener(v -> {
                if (manager.getButtons().size() >= MoreButtonsManager.MAX_BUTTONS) {
                    Toast.makeText(activity, activity.getString(R.string.more_buttons_limit_reached, MoreButtonsManager.MAX_BUTTONS), Toast.LENGTH_SHORT).show();
                    return;
                }
                MoreButtonConfig fresh = new MoreButtonConfig();
                fresh.name = activity.getString(R.string.more_buttons_default_name);
                showEdit(fresh, true);
            });
            tutorial.setOnClickListener(v -> showTutorial());

            List<MoreButtonConfig> buttons = manager.getButtons();
            if (buttons.isEmpty()) {
                TextView empty = text(activity, activity.getString(R.string.more_buttons_empty), 13, 0xFF9FA7AE);
                empty.setGravity(Gravity.CENTER);
                content.addView(empty, topMargin(24, d));
            } else {
                InbuiltModManager inbuilt = InbuiltModManager.getInstance(activity);
                for (MoreButtonConfig cfg : buttons) {
                    LinearLayout card = vertical(activity, 10);
                    card.setBackgroundColor(0xFF24282C);

                    LinearLayout titleRow = horizontal(activity);
                    TextView title = text(activity, cfg.name, 15, Color.WHITE);
                    title.setTypeface(null, android.graphics.Typeface.BOLD);
                    titleRow.addView(title, weighted());
                    Switch visible = switchView(activity, activity.getString(R.string.visible), cfg.visible);
                    titleRow.addView(visible);
                    card.addView(titleRow);

                    TextView detail = text(activity,
                            activity.getString(R.string.more_buttons_summary_format,
                                    keyName(activity, cfg.keyCode),
                                    activity.getString(cfg.toggle ? R.string.more_buttons_mode_toggle : R.string.more_buttons_mode_momentary),
                                    cfg.iconScale),
                            12, 0xFFA8B0B8);
                    card.addView(detail);

                    LinearLayout row = horizontal(activity);
                    Button edit = actionButton(activity, activity.getString(R.string.edit));
                    Button delete = actionButton(activity, activity.getString(R.string.delete));
                    row.addView(edit, weighted());
                    LinearLayout.LayoutParams deleteParams = weighted();
                    deleteParams.leftMargin = Math.round(8 * d);
                    row.addView(delete, deleteParams);
                    card.addView(row, topMargin(6, d));

                    visible.setOnCheckedChangeListener((buttonView, checked) -> {
                        MoreButtonConfig updated = cfg.copy();
                        updated.visible = checked;
                        manager.saveButton(updated);
                        refreshOverlays();
                    });
                    edit.setOnClickListener(v -> showEdit(cfg.copy(), false));
                    boolean[] confirmDelete = new boolean[]{false};
                    delete.setOnClickListener(v -> {
                        if (!confirmDelete[0]) {
                            confirmDelete[0] = true;
                            delete.setText(R.string.more_buttons_tap_again_delete);
                            delete.postDelayed(() -> {
                                confirmDelete[0] = false;
                                delete.setText(R.string.delete);
                            }, 2500);
                            return;
                        }
                        manager.deleteButton(cfg.id);
                        inbuilt.clearOverlaySettings(cfg.overlayKey());
                        refreshOverlays();
                        showMain();
                    });
                    content.addView(card, cardMargin(d));
                }
            }

            scroll.addView(content);
            root.addView(scroll, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
            replace(root);
        }

        void showEdit(MoreButtonConfig config, boolean isNew) {
            if (currentEdit != null && currentEdit.working != config) currentEdit.dispose();
            currentEdit = new EditPage(this, config, isNew);
            showEditPage(currentEdit);
        }

        void showEditPage(EditPage page) {
            currentEdit = page;
            backAction = this::showMain;
            replace(page.build());
        }

        void showKeyPicker(EditPage page) {
            page.syncName();
            backAction = () -> showEditPage(page);
            float d = density(activity);
            LinearLayout root = screenRoot(activity);
            root.addView(toolbar(activity.getString(R.string.more_buttons_choose_key), activity.getString(R.string.back), () -> showEditPage(page)));

            LinearLayout content = vertical(activity, 12);
            TextView help = text(activity,
                    activity.getString(R.string.more_buttons_choose_one_key_help),
                    12, 0xFFA8B0B8);
            content.addView(help);

            EditText search = new EditText(activity);
            search.setHint(R.string.more_buttons_search_keys);
            search.setSingleLine(true);
            search.setTextColor(Color.WHITE);
            search.setHintTextColor(0xFF7F8992);
            content.addView(search);

            List<KeyMapper.Entry> all = new ArrayList<>();
            for (KeyMapper.Entry entry : KeyMapper.entries()) {
                if (MoreButtonConfig.isSupportedMapping(entry.glfwCode)) all.add(entry);
            }
            ArrayList<KeyMapper.Entry> filtered = new ArrayList<>(all);
            ArrayList<String> names = new ArrayList<>();
            for (KeyMapper.Entry entry : filtered) names.add(keyName(activity, entry.glfwCode));
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                    android.R.layout.simple_list_item_1, names);
            ListView list = new ListView(activity);
            list.setAdapter(adapter);
            list.setDividerHeight(1);
            list.setOnItemClickListener((parent, view, position, id) -> {
                if (position < 0 || position >= filtered.size()) return;
                page.working.keyCode = filtered.get(position).glfwCode;
                showEditPage(page);
            });
            search.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                    filtered.clear();
                    names.clear();
                    for (KeyMapper.Entry entry : all) {
                        String displayName = keyName(activity, entry.glfwCode);
                        if (query.isEmpty() || displayName.toLowerCase(Locale.ROOT).contains(query)) {
                            filtered.add(entry);
                            names.add(displayName);
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
            content.addView(list, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

            Button hardware = actionButton(activity, activity.getString(R.string.more_buttons_press_hardware_key));
            content.addView(hardware, topMargin(8, d));
            hardware.setOnClickListener(v -> showHardwareCapture(page));

            root.addView(content, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
            replace(root);
        }

        void showHardwareCapture(EditPage page) {
            backAction = () -> showKeyPicker(page);
            LinearLayout root = screenRoot(activity);
            root.addView(toolbar(activity.getString(R.string.more_buttons_press_one_key), activity.getString(R.string.back), () -> showKeyPicker(page)));
            LinearLayout body = vertical(activity, 16);
            TextView message = text(activity,
                    activity.getString(R.string.more_buttons_hardware_key_help), 15, Color.WHITE);
            message.setGravity(Gravity.CENTER);
            body.setGravity(Gravity.CENTER);
            body.addView(message);
            root.addView(body, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
            root.setFocusableInTouchMode(true);
            root.requestFocus();
            root.setOnKeyListener((v, androidKeyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
                if (androidKeyCode == KeyEvent.KEYCODE_BACK) {
                    showKeyPicker(page);
                    return true;
                }
                int glfw = KeyMapper.fromAndroidKeyCode(androidKeyCode);
                if (!KeyMapper.isKeyboardKey(glfw)) {
                    Toast.makeText(activity, R.string.more_buttons_key_unsupported, Toast.LENGTH_SHORT).show();
                    return true;
                }
                page.working.keyCode = glfw;
                showEditPage(page);
                return true;
            });
            replace(root);
            root.requestFocus();
        }

        void showTutorial() {
            backAction = this::showMain;
            LinearLayout root = screenRoot(activity);
            root.addView(toolbar(activity.getString(R.string.more_buttons_tutorial_title), activity.getString(R.string.back), this::showMain));
            ScrollView scroll = new ScrollView(activity);
            LinearLayout content = vertical(activity, 16);
            content.addView(text(activity,
                    activity.getString(R.string.more_buttons_tutorial_body),
                    13, 0xFFD6DADF));

            Button svgRepo = actionButton(activity, activity.getString(R.string.more_buttons_open_svg_repo));
            Button theSvg = actionButton(activity, activity.getString(R.string.more_buttons_open_the_svg));
            content.addView(svgRepo, topMargin(12, density(activity)));
            content.addView(theSvg, topMargin(8, density(activity)));
            svgRepo.setOnClickListener(v -> openUrl(activity, "https://www.svgrepo.com/"));
            theSvg.setOnClickListener(v -> openUrl(activity, "https://thesvg.org/"));
            scroll.addView(content);
            root.addView(scroll, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
            replace(root);
        }

        private LinearLayout toolbar(String titleText, String backText, Runnable action) {
            float d = density(activity);
            LinearLayout bar = horizontal(activity);
            bar.setPadding(Math.round(8 * d), Math.round(5 * d), Math.round(8 * d), Math.round(5 * d));
            bar.setBackgroundColor(0xFF202428);
            TextView title = text(activity, titleText, 18, Color.WHITE);
            title.setGravity(Gravity.CENTER_VERTICAL);
            bar.addView(title, weighted());
            Button back = actionButton(activity, backText);
            back.setOnClickListener(v -> action.run());
            bar.addView(back, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            return bar;
        }

        private void replace(View view) {
            removeAllViews();
            addView(view, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            requestFocus();
            bringToFront();
            applyImmersive(activity);
        }
    }

    private static final class EditPage {
        private final EditorHost host;
        private final Activity activity;
        private final MoreButtonsManager manager;
        private final InbuiltModManager inbuilt;
        private final MoreButtonConfig working;
        private final boolean isNew;
        private ImageView preview;
        private Bitmap previewBitmap;
        private TextView normalFile;
        private TextView pressedFile;
        private Button keyButton;
        private EditText nameInput;
        private boolean previewPressed;
        private int buttonSize;
        private int opacity;
        private boolean locked;
        private boolean showEverywhere;

        EditPage(EditorHost host, MoreButtonConfig working, boolean isNew) {
            this.host = host;
            this.activity = host.activity;
            this.manager = MoreButtonsManager.getInstance(activity);
            this.inbuilt = InbuiltModManager.getInstance(activity);
            this.working = working;
            this.isNew = isNew;
            buttonSize = inbuilt.getOverlayButtonSize(working.overlayKey());
            opacity = inbuilt.getOverlayOpacity(working.overlayKey());
            locked = inbuilt.isOverlayLocked(working.overlayKey());
            showEverywhere = inbuilt.isOverlayShowEverywhere(working.overlayKey());
        }

        View build() {
            float d = density(activity);
            LinearLayout root = screenRoot(activity);
            root.addView(host.toolbar(isNew ? activity.getString(R.string.more_buttons_add_button) : activity.getString(R.string.more_buttons_edit_title, working.name),
                    activity.getString(R.string.back), host::showMain));

            ScrollView scroll = new ScrollView(activity);
            LinearLayout content = vertical(activity, 14);
            content.setGravity(Gravity.CENTER_HORIZONTAL);

            preview = new ImageView(activity);
            int previewPx = Math.round(170 * d);
            preview.setLayoutParams(new LinearLayout.LayoutParams(previewPx, previewPx));
            content.addView(preview);

            Button previewState = actionButton(activity, activity.getString(previewPressed ? R.string.more_buttons_preview_normal_state : R.string.more_buttons_preview_pressed_state));
            content.addView(previewState, centeredMargin(8, d));
            previewState.setOnClickListener(v -> {
                previewPressed = !previewPressed;
                previewState.setText(previewPressed ? R.string.more_buttons_preview_normal_state : R.string.more_buttons_preview_pressed_state);
                updatePreview();
            });

            content.addView(sectionLabel(activity, activity.getString(R.string.more_buttons_name), d));
            nameInput = new EditText(activity);
            nameInput.setHint(R.string.more_buttons_button_name_hint);
            nameInput.setText(working.name);
            nameInput.setTextColor(Color.WHITE);
            nameInput.setHintTextColor(0xFF7F8992);
            content.addView(nameInput);

            content.addView(sectionLabel(activity, activity.getString(R.string.more_buttons_key_mapping), d));
            keyButton = actionButton(activity, keyName(activity, working.keyCode));
            content.addView(keyButton);
            keyButton.setOnClickListener(v -> host.showKeyPicker(this));
            content.addView(text(activity,
                    activity.getString(R.string.more_buttons_one_key_only),
                    11, 0xFF9FA7AE));

            Switch toggle = switchView(activity, activity.getString(R.string.more_buttons_toggle_button), working.toggle);
            content.addView(toggle, topMargin(10, d));
            content.addView(text(activity,
                    activity.getString(R.string.more_buttons_toggle_help),
                    11, 0xFF9FA7AE));
            toggle.setOnCheckedChangeListener((buttonView, checked) -> {
                working.toggle = checked;
            });

            content.addView(sectionLabel(activity, activity.getString(R.string.more_buttons_normal_icon), d));
            Button normalPick = actionButton(activity, activity.getString(R.string.more_buttons_choose_svg));
            normalFile = text(activity, svgName(activity, working.normalSvgName), 12, 0xFFA8B0B8);
            content.addView(normalPick);
            content.addView(normalFile);
            Switch normalColors = switchView(activity, activity.getString(R.string.more_buttons_keep_original_colors), working.keepNormalColors);
            content.addView(normalColors);
            content.addView(text(activity,
                    activity.getString(R.string.more_buttons_normal_color_help),
                    11, 0xFF9FA7AE));
            normalPick.setOnClickListener(v -> pickSvg(false));
            normalColors.setOnCheckedChangeListener((b, checked) -> {
                working.keepNormalColors = checked;
                updatePreview();
            });

            content.addView(sectionLabel(activity, activity.getString(R.string.more_buttons_pressed_icon), d));
            Button pressedPick = actionButton(activity, activity.getString(R.string.more_buttons_choose_pressed_svg_optional));
            pressedFile = text(activity, svgName(activity, working.pressedSvgName), 12, 0xFFA8B0B8);
            content.addView(pressedPick);
            content.addView(pressedFile);
            Switch pressedColors = switchView(activity, activity.getString(R.string.more_buttons_keep_pressed_colors), working.keepPressedColors);
            content.addView(pressedColors);
            content.addView(text(activity,
                    activity.getString(R.string.more_buttons_pressed_color_help),
                    11, 0xFF9FA7AE));
            pressedPick.setOnClickListener(v -> pickSvg(true));
            pressedColors.setOnCheckedChangeListener((b, checked) -> {
                working.keepPressedColors = checked;
                updatePreview();
            });

            addSlider(content, activity.getString(R.string.more_buttons_icon_size), 20, 80, working.iconScale, "%", value -> {
                working.iconScale = value;
                updatePreview();
            }, d);
            addSlider(content, activity.getString(R.string.more_buttons_icon_x_offset), -32, 32, working.iconOffsetX, " " + activity.getString(R.string.more_buttons_unit_px), value -> {
                working.iconOffsetX = value;
                updatePreview();
            }, d);
            addSlider(content, activity.getString(R.string.more_buttons_icon_y_offset), -32, 32, working.iconOffsetY, " " + activity.getString(R.string.more_buttons_unit_px), value -> {
                working.iconOffsetY = value;
                updatePreview();
            }, d);
            addSlider(content, activity.getString(R.string.more_buttons_button_size), 24, 100, buttonSize, " " + activity.getString(R.string.more_buttons_unit_dp), value -> buttonSize = value, d);
            addSlider(content, activity.getString(R.string.more_buttons_opacity), 0, 100, opacity, "%", value -> opacity = value, d);

            Switch lock = switchView(activity, activity.getString(R.string.more_buttons_lock_position), locked);
            Switch everywhere = switchView(activity, activity.getString(R.string.more_buttons_show_outside_hud), showEverywhere);
            content.addView(lock, topMargin(8, d));
            content.addView(everywhere);
            lock.setOnCheckedChangeListener((b, checked) -> locked = checked);
            everywhere.setOnCheckedChangeListener((b, checked) -> showEverywhere = checked);

            content.addView(text(activity,
                    activity.getString(R.string.more_buttons_position_help),
                    11, 0xFF9FA7AE), topMargin(6, d));

            LinearLayout actions = horizontal(activity);
            Button cancel = actionButton(activity, activity.getString(R.string.cancel));
            Button save = actionButton(activity, activity.getString(R.string.save));
            actions.addView(cancel, weighted());
            LinearLayout.LayoutParams saveParams = weighted();
            saveParams.leftMargin = Math.round(8 * d);
            actions.addView(save, saveParams);
            content.addView(actions, topMargin(14, d));
            cancel.setOnClickListener(v -> host.showMain());
            save.setOnClickListener(v -> save());

            scroll.addView(content);
            root.addView(scroll, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
            root.post(this::updatePreview);
            return root;
        }

        void syncName() {
            if (nameInput != null) working.name = nameInput.getText().toString();
        }

        void acceptSvg(Uri uri, boolean pressed) {
            try {
                int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                try { activity.getContentResolver().takePersistableUriPermission(uri, flags); } catch (Exception ignored) {}
                byte[] bytes = readLimited(activity, uri, MoreButtonsManager.MAX_SVG_BYTES);
                String svg = new String(bytes, StandardCharsets.UTF_8);
                if (!MoreButtonsManager.looksLikeSvg(svg)) {
                    Toast.makeText(activity, R.string.more_buttons_invalid_svg, Toast.LENGTH_LONG).show();
                    return;
                }
                String name = displayName(activity, uri);
                if (pressed) {
                    working.pressedSvg = svg;
                    working.pressedSvgName = name;
                    if (pressedFile != null) pressedFile.setText(name);
                } else {
                    working.normalSvg = svg;
                    working.normalSvgName = name;
                    if (normalFile != null) normalFile.setText(name);
                }
                updatePreview();
                applyImmersive(activity);
            } catch (Exception e) {
                if (e instanceof IllegalArgumentException) {
                    Toast.makeText(activity, R.string.more_buttons_svg_too_large, Toast.LENGTH_LONG).show();
                } else if (e instanceof IllegalStateException) {
                    Toast.makeText(activity, R.string.more_buttons_cannot_open_file, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, R.string.more_buttons_svg_read_failed, Toast.LENGTH_LONG).show();
                }
            }
        }

        void dispose() {
            if (previewBitmap != null && !previewBitmap.isRecycled()) previewBitmap.recycle();
            previewBitmap = null;
        }

        private void pickSvg(boolean pressed) {
            syncName();
            pendingSession = new WeakReference<>(this);
            pendingPressedSvg = pressed;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/svg+xml");
            intent.putExtra(Intent.EXTRA_MIME_TYPES,
                    new String[]{"image/svg+xml", "text/xml", "application/xml", "text/plain"});
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            activity.startActivityForResult(intent, REQUEST_SVG);
        }

        private void save() {
            syncName();
            String cleanName = working.name == null ? "" : working.name.trim();
            if (cleanName.isEmpty()) {
                Toast.makeText(activity, R.string.more_buttons_enter_name, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!MoreButtonConfig.isSupportedMapping(working.keyCode)) {
                Toast.makeText(activity, R.string.more_buttons_choose_valid_key, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!MoreButtonsManager.looksLikeSvg(working.normalSvg)) {
                Toast.makeText(activity, R.string.more_buttons_choose_normal_svg, Toast.LENGTH_SHORT).show();
                return;
            }
            working.name = cleanName;
            if (!manager.saveButton(working)) {
                Toast.makeText(activity, R.string.more_buttons_save_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            inbuilt.setOverlayButtonSize(working.overlayKey(), buttonSize);
            inbuilt.setOverlayOpacity(working.overlayKey(), opacity);
            inbuilt.setOverlayLocked(working.overlayKey(), locked);
            inbuilt.setOverlayShowEverywhere(working.overlayKey(), showEverywhere);
            refreshOverlays();
            host.showMain();
        }

        private void updatePreview() {
            if (preview == null) return;
            Bitmap next = MoreButtonOverlay.createButtonBitmap(activity, working, previewPressed);
            Bitmap previous = previewBitmap;
            previewBitmap = next;
            preview.setImageBitmap(next);
            if (previous != null && previous != next && !previous.isRecycled()) previous.recycle();
        }
    }

    private interface IntConsumer {
        void accept(int value);
    }

    private static void addSlider(LinearLayout root, String label, int min, int max, int current,
                                  String suffix, IntConsumer consumer, float density) {
        root.addView(sectionLabel(root.getContext(), label, density));
        TextView value = valueText(root.getContext(), current + suffix);
        root.addView(value);
        SeekBar seek = new SeekBar(root.getContext());
        seek.setMin(min);
        seek.setMax(max);
        seek.setProgress(Math.max(min, Math.min(max, current)));
        root.addView(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                value.setText(progress + suffix);
                consumer.accept(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private static void refreshOverlays() {
        InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
        if (overlayManager != null && overlayManager.isModActive(ModIds.MORE_BUTTONS)) {
            overlayManager.refreshMoreButtons();
        }
    }

    private static LinearLayout screenRoot(Activity activity) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF111315);
        root.setClickable(true);
        root.setFocusable(true);
        return root;
    }

    private static LinearLayout vertical(android.content.Context context, int paddingDp) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(paddingDp * density(context));
        layout.setPadding(padding, padding, padding, padding);
        return layout;
    }

    private static LinearLayout horizontal(android.content.Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private static Button actionButton(android.content.Context context, String label) {
        Button button = new Button(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        return button;
    }

    private static Switch switchView(android.content.Context context, String label, boolean checked) {
        Switch view = new Switch(context);
        view.setText(label);
        view.setTextColor(0xFFD6DADF);
        view.setChecked(checked);
        return view;
    }

    private static TextView text(android.content.Context context, String value, float size, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private static TextView valueText(android.content.Context context, String value) {
        TextView view = text(context, value, 12, ACCENT);
        view.setGravity(Gravity.END);
        return view;
    }

    private static TextView sectionLabel(android.content.Context context, String value, float density) {
        TextView label = text(context, value, 12, 0xFFB8C0C8);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = Math.round(10 * density);
        label.setLayoutParams(params);
        return label;
    }

    private static LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private static LinearLayout.LayoutParams topMargin(int dp, float density) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = Math.round(dp * density);
        return params;
    }

    private static LinearLayout.LayoutParams centeredMargin(int dp, float density) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.topMargin = Math.round(dp * density);
        return params;
    }

    private static LinearLayout.LayoutParams cardMargin(float density) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = Math.round(8 * density);
        return params;
    }

    private static float density(android.content.Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    private static String keyName(android.content.Context context, int glfwCode) {
        return KeyMapper.isKeyboardKey(glfwCode) ? KeyMapper.nameOf(glfwCode) : context.getString(R.string.more_buttons_choose_key);
    }

    private static String svgName(android.content.Context context, String name) {
        return name == null || name.trim().isEmpty() ? context.getString(R.string.more_buttons_no_svg_selected) : name;
    }

    private static void openUrl(Activity activity, String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(activity, url, Toast.LENGTH_LONG).show();
        }
    }

    private static byte[] readLimited(Activity activity, Uri uri, int limit) throws Exception {
        try (InputStream input = activity.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) throw new IllegalStateException();
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > limit) throw new IllegalArgumentException();
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static String displayName(Activity activity, Uri uri) {
        try (Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String value = cursor.getString(index);
                    if (value != null && !value.isEmpty()) return value;
                }
            }
        } catch (Exception ignored) {
        }
        String last = uri.getLastPathSegment();
        return last == null ? "icon.svg" : last;
    }
}

package org.levimc.pojavcontrols;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PojavControlsEditorActivity extends AppCompatActivity {
    private static final int REQUEST_IMPORT = 4101;
    private static final int REQUEST_EXPORT = 4102;

    private ControlRepository repository;
    private CustomControls profile;
    private String profileName;
    private ControlEditorCanvas canvas;
    private Spinner profileSpinner;
    private boolean profileSpinnerBusy;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        repository = new ControlRepository(this);
        profileName = repository.activeName();
        profile = repository.load(profileName);
        setContentView(buildContent());
        reloadProfileSpinner(profileName);
    }

    @Override
    protected void onPause() {
        saveCurrent(false);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        saveCurrent(false);
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (requestCode == REQUEST_IMPORT) {
                String requested = uri.getLastPathSegment();
                if (requested != null && requested.endsWith(".json")) requested = requested.substring(0, requested.length() - 5);
                try (InputStream input = getContentResolver().openInputStream(uri)) {
                    if (input == null) throw new IllegalStateException();
                    profileName = repository.importProfile(requested, input);
                }
                repository.setActive(profileName);
                profile = repository.load(profileName);
                canvas.setProfile(profile);
                reloadProfileSpinner(profileName);
                notifyProfileChanged();
                Toast.makeText(this, R.string.pojav_controls_imported, Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQUEST_EXPORT) {
                saveCurrent(false);
                try (OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
                    if (output == null) throw new IllegalStateException();
                    repository.exportProfile(profileName, output);
                }
            }
        } catch (Exception exception) {
            Toast.makeText(this, R.string.pojav_controls_invalid, Toast.LENGTH_LONG).show();
        }
    }

    private View buildContent() {
        float density = getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF101316);

        HorizontalScrollView toolbarScroll = new HorizontalScrollView(this);
        toolbarScroll.setHorizontalScrollBarEnabled(false);
        toolbarScroll.setFillViewport(true);
        toolbarScroll.setBackgroundColor(0xFF202428);
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(Math.round(8 * density), Math.round(4 * density),
                Math.round(8 * density), Math.round(4 * density));

        TextView title = new TextView(this);
        title.setText(R.string.pojav_controls_editor);
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.addView(title, new LinearLayout.LayoutParams(Math.round(160 * density),
                Math.round(48 * density)));

        profileSpinner = new Spinner(this);
        toolbar.addView(profileSpinner, new LinearLayout.LayoutParams(Math.round(180 * density),
                Math.round(48 * density)));
        profileSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (profileSpinnerBusy) return;
                String selected = String.valueOf(parent.getItemAtPosition(position));
                if (!selected.equals(profileName)) {
                    saveCurrent(false);
                    profileName = selected;
                    repository.setActive(selected);
                    profile = repository.load(selected);
                    canvas.setProfile(profile);
                    notifyProfileChanged();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        toolbar.addView(toolbarButton(R.string.pojav_controls_profiles, view -> showProfilesDialog()));
        toolbar.addView(toolbarButton(R.string.pojav_controls_add, view -> showAddDialog()));
        toolbar.addView(toolbarButton(R.string.pojav_controls_save, view -> saveCurrent(true)));
        toolbar.addView(toolbarButton(R.string.pojav_controls_import, view -> startImport()));
        toolbar.addView(toolbarButton(R.string.pojav_controls_export, view -> startExport()));
        toolbar.addView(toolbarButton(R.string.pojav_controls_close, view -> {
            saveCurrent(false);
            finish();
        }));

        toolbarScroll.addView(toolbar, new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT, Math.round(56 * density)));
        root.addView(toolbarScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.round(56 * density)));

        canvas = new ControlEditorCanvas(this, this::showProperties);
        canvas.setProfile(profile);
        root.addView(canvas, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private Button toolbarButton(int text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        return button;
    }

    private void showAddDialog() {
        String[] entries = new String[]{getString(R.string.pojav_controls_button),
                getString(R.string.pojav_controls_joystick), getString(R.string.pojav_controls_drawer)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.pojav_controls_add)
                .setItems(entries, (dialog, which) -> {
                    if (which == 0) profile.mControlDataList.add(new ControlData());
                    else if (which == 1) profile.mJoystickDataList.add(new ControlJoystickData());
                    else {
                        ControlDrawerData drawer = new ControlDrawerData();
                        drawer.buttonProperties.add(new ControlData("Button", new int[]{KeyMapper.GLFW_KEY_SPACE},
                                "0.5 * ${screen_width}", "0.5 * ${screen_height}", 50, 50));
                        profile.mDrawerDataList.add(drawer);
                    }
                    canvas.rebuild();
                })
                .show();
    }

    private void showProfilesDialog() {
        List<String> profiles = repository.listProfiles();
        String[] actions = new String[]{getString(R.string.pojav_controls_new_profile),
                getString(R.string.pojav_controls_delete)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.pojav_controls_profiles)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) showNewProfileDialog();
                    else if (profiles.size() > 1) showDeleteProfileDialog(profiles);
                })
                .show();
    }

    private void showNewProfileDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.pojav_controls_profile_name);
        input.setSingleLine(true);
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setPadding(padding, 0, padding, 0);
        wrapper.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(this)
                .setTitle(R.string.pojav_controls_new_profile)
                .setView(wrapper)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String name = ControlRepository.sanitizeName(input.getText().toString());
                    if (name.isEmpty()) return;
                    saveCurrent(false);
                    profileName = repository.duplicate(profileName, name);
                    repository.setActive(profileName);
                    profile = repository.load(profileName);
                    canvas.setProfile(profile);
                    reloadProfileSpinner(profileName);
                    notifyProfileChanged();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showDeleteProfileDialog(List<String> profiles) {
        ArrayList<String> deletable = new ArrayList<>(profiles);
        deletable.remove("default");
        new AlertDialog.Builder(this)
                .setTitle(R.string.pojav_controls_delete)
                .setItems(deletable.toArray(new String[0]), (dialog, which) -> {
                    String selected = deletable.get(which);
                    repository.delete(selected);
                    if (selected.equals(profileName)) {
                        profileName = repository.activeName();
                        profile = repository.load(profileName);
                        canvas.setProfile(profile);
                    }
                    reloadProfileSpinner(profileName);
                    notifyProfileChanged();
                })
                .show();
    }

    private void showProperties(ControlEditorCanvas.EditorTarget target) {
        float density = getResources().getDisplayMetrics().density;
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(16 * density);
        form.setPadding(padding, padding, padding, padding);

        EditText name = field(form, R.string.pojav_controls_name, target.data.name);
        Button mapping = new Button(this);
        mapping.setText(mappingText(target.data.keycodes));
        mapping.setAllCaps(false);
        int[] selectedCodes = Arrays.copyOf(target.data.keycodes, 1);
        if (target.type == ControlEditorCanvas.EditorTarget.BUTTON ||
                target.type == ControlEditorCanvas.EditorTarget.DRAWER_BUTTON) {
            addLabel(form, R.string.pojav_controls_mapping);
            form.addView(mapping);
            mapping.setOnClickListener(view -> showMappingDialog(selectedCodes, mapping));
        }

        EditText x = field(form, R.string.pojav_controls_position_x, target.data.dynamicX);
        EditText y = field(form, R.string.pojav_controls_position_y, target.data.dynamicY);
        EditText width = field(form, R.string.pojav_controls_width, Float.toString(target.data.width));
        EditText height = field(form, R.string.pojav_controls_height, Float.toString(target.data.height));
        EditText opacity = field(form, R.string.pojav_controls_opacity,
                Integer.toString(Math.round(target.data.opacity * 100f)));
        EditText background = field(form, R.string.pojav_controls_background,
                String.format("#%08X", target.data.bgColor));
        EditText stroke = field(form, R.string.pojav_controls_stroke,
                String.format("#%08X", target.data.strokeColor));
        EditText strokeWidth = field(form, R.string.pojav_controls_stroke_width,
                Float.toString(target.data.strokeWidth));
        EditText radius = field(form, R.string.pojav_controls_corner_radius,
                Float.toString(target.data.cornerRadius));
        CheckBox toggle = check(form, R.string.pojav_controls_toggle, target.data.isToggle);
        CheckBox swipeable = check(form, R.string.pojav_controls_swipeable, target.data.isSwipeable);
        CheckBox passThrough = check(form, R.string.pojav_controls_pass_through, target.data.passThruEnabled);
        CheckBox inGame = check(form, R.string.pojav_controls_in_game, target.data.displayInGame);
        CheckBox inMenu = check(form, R.string.pojav_controls_in_menu, target.data.displayInMenu);

        CheckBox joystickAbsolute = null;
        CheckBox forwardLock = null;
        if (target.joystick != null) {
            joystickAbsolute = check(form, R.string.pojav_controls_joystick_absolute, target.joystick.absolute);
            forwardLock = check(form, R.string.pojav_controls_forward_lock, target.joystick.forwardLock);
        }

        Spinner orientation = null;
        if (target.type == ControlEditorCanvas.EditorTarget.DRAWER) {
            addLabel(form, R.string.pojav_controls_orientation);
            orientation = new Spinner(this);
            ArrayAdapter<ControlDrawerData.Orientation> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, ControlDrawerData.Orientation.values());
            orientation.setAdapter(adapter);
            orientation.setSelection(target.drawer.orientation.ordinal());
            form.addView(orientation);
            Button addDrawerButton = new Button(this);
            addDrawerButton.setText(R.string.pojav_controls_button);
            addDrawerButton.setOnClickListener(view -> {
                target.drawer.buttonProperties.add(new ControlData());
                canvas.rebuild();
            });
            form.addView(addDrawerButton);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button clone = new Button(this);
        clone.setText(R.string.pojav_controls_clone);
        Button delete = new Button(this);
        delete.setText(R.string.pojav_controls_delete);
        actions.addView(clone, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        actions.addView(delete, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        form.addView(actions);
        scroll.addView(form);

        CheckBox finalJoystickAbsolute = joystickAbsolute;
        CheckBox finalForwardLock = forwardLock;
        Spinner finalOrientation = orientation;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.pojav_controls_properties)
                .setView(scroll)
                .setPositiveButton(android.R.string.ok, (ignored, which) -> {
                    target.data.name = name.getText().toString().trim();
                    target.data.keycodes = Arrays.copyOf(selectedCodes, 1);
                    target.data.dynamicX = x.getText().toString().trim();
                    target.data.dynamicY = y.getText().toString().trim();
                    target.data.width = number(width, target.data.width);
                    target.data.height = number(height, target.data.height);
                    target.data.opacity = number(opacity, target.data.opacity * 100f) / 100f;
                    target.data.bgColor = color(background, target.data.bgColor);
                    target.data.strokeColor = color(stroke, target.data.strokeColor);
                    target.data.strokeWidth = number(strokeWidth, target.data.strokeWidth);
                    target.data.cornerRadius = number(radius, target.data.cornerRadius);
                    target.data.isToggle = toggle.isChecked();
                    target.data.isSwipeable = swipeable.isChecked();
                    target.data.passThruEnabled = passThrough.isChecked();
                    target.data.displayInGame = inGame.isChecked();
                    target.data.displayInMenu = inMenu.isChecked();
                    if (target.joystick != null) {
                        target.joystick.absolute = finalJoystickAbsolute.isChecked();
                        target.joystick.forwardLock = finalForwardLock.isChecked();
                    }
                    if (finalOrientation != null) {
                        target.drawer.orientation = (ControlDrawerData.Orientation) finalOrientation.getSelectedItem();
                    }
                    target.data.normalize();
                    canvas.rebuild();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        clone.setOnClickListener(view -> {
            target.cloneAction.run();
            dialog.dismiss();
        });
        delete.setOnClickListener(view -> {
            target.deleteAction.run();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showMappingDialog(int[] selectedCodes, Button mapping) {
        List<KeyMapper.Entry> entries = KeyMapper.entries();
        String[] names = new String[entries.size()];
        int selected = 0;
        for (int i = 0; i < entries.size(); i++) {
            names[i] = entries.get(i).name;
            if (selectedCodes.length > 0 && selectedCodes[0] == entries.get(i).glfwCode) selected = i;
        }
        int[] selectedIndex = new int[]{selected};
        new AlertDialog.Builder(this)
                .setTitle(R.string.pojav_controls_mapping)
                .setSingleChoiceItems(names, selected, (dialog, which) -> selectedIndex[0] = which)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    selectedCodes[0] = entries.get(selectedIndex[0]).glfwCode;
                    mapping.setText(mappingText(selectedCodes));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String mappingText(int[] keycodes) {
        int code = keycodes == null || keycodes.length == 0
                ? KeyMapper.GLFW_KEY_UNKNOWN : keycodes[0];
        return code == KeyMapper.GLFW_KEY_UNKNOWN
                ? getString(R.string.pojav_controls_mapping) : KeyMapper.nameOf(code);
    }

    private EditText field(LinearLayout form, int label, String value) {
        addLabel(form, label);
        EditText input = new EditText(this);
        input.setText(value == null ? "" : value);
        input.setSingleLine(true);
        form.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return input;
    }

    private void addLabel(LinearLayout form, int label) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(0xFFB8C0C8);
        view.setTextSize(12);
        view.setPadding(0, Math.round(8 * getResources().getDisplayMetrics().density), 0, 0);
        form.addView(view);
    }

    private CheckBox check(LinearLayout form, int label, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setChecked(checked);
        form.addView(box);
        return box;
    }

    private float number(EditText input, float fallback) {
        try {
            return Float.parseFloat(input.getText().toString().trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int color(EditText input, int fallback) {
        String value = input.getText().toString().trim();
        try {
            if (!value.startsWith("#")) value = "#" + value;
            if (value.length() == 9) return (int) Long.parseLong(value.substring(1), 16);
            return Color.parseColor(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void saveCurrent(boolean toast) {
        if (repository == null || profile == null || profileName == null) return;
        repository.save(profileName, profile);
        repository.setActive(profileName);
        notifyProfileChanged();
        if (toast) Toast.makeText(this, R.string.pojav_controls_saved, Toast.LENGTH_SHORT).show();
    }

    private void reloadProfileSpinner(String selected) {
        profileSpinnerBusy = true;
        List<String> profiles = repository.listProfiles();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, profiles);
        profileSpinner.setAdapter(adapter);
        int index = profiles.indexOf(selected);
        profileSpinner.setSelection(Math.max(0, index));
        profileSpinnerBusy = false;
    }

    private void startImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQUEST_IMPORT);
    }

    private void startExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, profileName + ".json");
        startActivityForResult(intent, REQUEST_EXPORT);
    }

    private void notifyProfileChanged() {
        Intent intent = new Intent(PojavControls.ACTION_PROFILE_CHANGED);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }
}
